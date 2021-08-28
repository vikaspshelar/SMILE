/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Organisation;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StOrganisationLookupVerbosity;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StUnitCreditSpecificationLookupVerbosity;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.sca.UnitCreditSpecificationList;
import com.smilecoms.commons.sca.UnitCreditSpecificationQuery;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.pos.Sale;
import com.smilecoms.xml.schema.pos.SaleLine;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class CreditAccountNumberHelper {

    private static final Map<Long, String> caNumCache = new ConcurrentHashMap<>();
    private static long caNumCacheLastRefreshed = 0;
    private static long ucCacheLastRefreshed = 0;
    private static UnitCreditSpecificationList ucCache = null;

    private static final Logger log = LoggerFactory.getLogger(CreditAccountNumberHelper.class);

    private static String getCreditAccountNumberForAccountId(long accountId) throws Exception {

        if (System.currentTimeMillis() - caNumCacheLastRefreshed > 300000) {
            caNumCache.clear();
            caNumCacheLastRefreshed = System.currentTimeMillis();
        }

        String can = caNumCache.get(accountId);

        if (can == null) {
            ServiceInstanceQuery q = new ServiceInstanceQuery();
            q.setAccountId(accountId);
            q.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(q);
            ProductInstance pi = SCAWrapper.getAdminInstance().getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN);
            Organisation o = SCAWrapper.getAdminInstance().getOrganisation(pi.getOrganisationId(), StOrganisationLookupVerbosity.MAIN);
            can = o.getCreditAccountNumber();
            if (can.isEmpty()) {
                throw new Exception("Account has no associated credit account number -- " + accountId);
            }
            caNumCache.put(accountId, can);
        }

        log.debug("Account id [{}] has credit account number [{}]", accountId, can);
        return can;
    }

    static void populateCreditAccountNumberForSale(Sale xmlSale) throws Exception {
        String creditAccountNumber;
        if (xmlSale.getSalesPersonAccountId() > 0 && (xmlSale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_PAYMENT_GATEWAY)
                || xmlSale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_CLEARING_BUREAU))) {
            // Caters for payment gateway or clearing bureau scenario where the payment gateway code or clearing bureau sale request tells us the clearing bureau account number
            creditAccountNumber = getCreditAccountNumberForAccountId(xmlSale.getSalesPersonAccountId());
            xmlSale.setCreditAccountNumber(creditAccountNumber);
            return;
        }

        if (xmlSale.getPaymentMethod().equals(POSManager.PAYMENT_METHOD_DELIVERY_SERVICE)) {
            if (xmlSale.getCreditAccountNumber() != null && !xmlSale.getCreditAccountNumber().isEmpty()) {
                return;
            }
            // Caters for delivery service 
            creditAccountNumber = BaseUtils.getProperty("env.pos.delivery.credit.account.number");
            xmlSale.setCreditAccountNumber(creditAccountNumber);
            return;
        }

        // Check if the sale has Staff bundles only. If so use the staff credit account code
        if (System.currentTimeMillis() - ucCacheLastRefreshed > 300000 || ucCache == null) {
            ucCacheLastRefreshed = System.currentTimeMillis();
            UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
            q.setUnitCreditSpecificationId(-1);
            q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN);
            ucCache = SCAWrapper.getAdminInstance().getUnitCreditSpecifications(q);
        }

        boolean foundStaff = false;
        boolean foundvatFree = false;
        boolean foundvatIncl = false;
        boolean foundOther = false;

        for (SaleLine line : xmlSale.getSaleLines()) {
            boolean foundUCSpec = false;
            for (UnitCreditSpecification spec : ucCache.getUnitCreditSpecifications()) {
                if (spec.getItemNumber().equals(line.getInventoryItem().getItemNumber())) {
                    log.debug("Item [{}] is a unit credit", line.getInventoryItem().getItemNumber());
                    String staff = Utils.getValueFromCRDelimitedAVPString(spec.getConfiguration(), "Staff");
                    String reporting = Utils.getValueFromCRDelimitedAVPString(spec.getConfiguration(), "Reporting");
                    if (staff != null && staff.equals("true")) {
                        foundStaff = true;
                    } else {
                        foundOther = true;
                    }
                    if (reporting != null && reporting.contains("NoVAT")) {
                        foundvatFree = true;
                    } else {
                        foundvatIncl = true;
                    }
                    foundUCSpec = true;
                    break;
                }
            }
            if (!foundUCSpec) {
                foundOther = true;
                log.debug("Item [{}] is not a unit credit", line.getInventoryItem().getItemNumber());
            }
        }

        if (!foundOther && foundStaff) {
            // Throw in some VAT checking for good measure
            if (foundvatFree && !foundvatIncl && !xmlSale.isTaxExempt()) {
                throw new Exception("Sale must be tax exempt for staff bundles");
            } else if (foundvatIncl && !foundvatFree && xmlSale.isTaxExempt()) {
                throw new Exception("Sale must not be tax exempt for staff bundles");
            } else if ((foundvatIncl && foundvatFree)) {
                throw new Exception("Cannot mix staff bundle VAT treatment");
            }
            xmlSale.setCreditAccountNumber(X3Helper.props.getProperty("StaffLoanCustomerCode"));
            log.debug("Sale only contains staff bundles so credit account number is [{}]", xmlSale.getCreditAccountNumber());
            return;
        }

        throw new Exception("Unable to determine the credit account number for the sale");
    }

}
