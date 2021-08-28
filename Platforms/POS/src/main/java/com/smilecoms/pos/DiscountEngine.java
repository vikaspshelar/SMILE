/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Javassist;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.pos.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class DiscountEngine {

    private static final Logger log = LoggerFactory.getLogger(DiscountEngine.class);

    void doDiscounting(EntityManager em, Sale sale) throws Exception {

        log.debug("In DiscountEngine doDiscounting. Sale has [{}] lines to run discounting on", sale.getSaleLines().size());

        String origPromoCode = sale.getPromotionCode();
        sale.setPromotionCode(sale.getPromotionCode().replace("Request Approval: ", ""));

        double passedInTotal = sale.getSaleTotalCentsIncl();

        List<SaleLine> consolidatedLines = getConsolidatedLines(sale.getSaleLines());

        sale.getSaleLines().clear();
        sale.getSaleLines().addAll(consolidatedLines);
        // set values to zero as they will be recalculated
        sale.setSaleTotalDiscountOnExclCents(0);
        sale.setSaleTotalDiscountOnInclCents(0);
        sale.setSaleTotalCentsExcl(0);
        sale.setSaleTotalCentsIncl(0);
        sale.setSaleTotalTaxCents(0);

        for (SaleLine saleLine : sale.getSaleLines()) {

            logSaleLine(saleLine, "Line Info Prior to Tiering/Discounting");

            double itemTaxRateForStandardTaxableCustomers; // The tax rate of this item if its sold to a vattable customer
            double salesTaxRateOfItemWithinContextOfSale; // The tax rate of this item considering whether the cutomer is tax exempt or not
            if (saleLine.getInventoryItem().getPriceInCentsIncl() == 0) {
                itemTaxRateForStandardTaxableCustomers = 1;
            } else {
                log.debug("##DUTY: sale line item [{}], incl [{}], excl [{}], rate is [{}/{}]",
                        new Object[]{saleLine.getInventoryItem().getItemNumber(), saleLine.getInventoryItem().getPriceInCentsIncl(), saleLine.getInventoryItem().getPriceInCentsExcl(),
                            (saleLine.getInventoryItem().getPriceInCentsIncl() - saleLine.getInventoryItem().getPriceInCentsExcl()), saleLine.getInventoryItem().getPriceInCentsExcl()});
                itemTaxRateForStandardTaxableCustomers = 1 + (saleLine.getInventoryItem().getPriceInCentsIncl() - saleLine.getInventoryItem().getPriceInCentsExcl()) / saleLine.getInventoryItem().getPriceInCentsExcl();
            }

            List<String> lstVatExemptItems = BaseUtils.getPropertyAsList("env.pos.vat.exempt.items");

            if (lstVatExemptItems != null && lstVatExemptItems.contains(saleLine.getInventoryItem().getItemNumber())) {
                salesTaxRateOfItemWithinContextOfSale = 1;
            }

            if (sale.isTaxExempt()) {
                salesTaxRateOfItemWithinContextOfSale = 1;
            } else {
                salesTaxRateOfItemWithinContextOfSale = itemTaxRateForStandardTaxableCustomers;
            }

            /*If youre a KIT and tax exempt... dont adjust the rate you have as it might be contain duty tax*/
            if (salesTaxRateOfItemWithinContextOfSale == 1
                    && (BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0) > 0)
                    && saleLine.getInventoryItem().getPriceInCentsIncl() != 0
                    && saleLine.getInventoryItem().getItemNumber().startsWith("KIT")) {
                salesTaxRateOfItemWithinContextOfSale = 1 + (saleLine.getInventoryItem().getPriceInCentsIncl() - saleLine.getInventoryItem().getPriceInCentsExcl()) / saleLine.getInventoryItem().getPriceInCentsExcl();
            }

            /*If youre NOT a KIT and tax exempt... then adjust the rate to be based on duty tax rate*/
            if (salesTaxRateOfItemWithinContextOfSale == 1
                    && (BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0) > 0)
                    && saleLine.getInventoryItem().getPriceInCentsIncl() != 0
                    && !saleLine.getInventoryItem().getItemNumber().startsWith("KIT")) {
                log.debug("#Item is NOT a KIT and is tax exempt going to calculate excise duty rate: sale line item [{}], incl [{}], excl [{}], rate is [{}/{}]",
                        new Object[]{saleLine.getInventoryItem().getItemNumber(), saleLine.getInventoryItem().getPriceInCentsIncl(), saleLine.getInventoryItem().getPriceInCentsExcl(),
                            (saleLine.getInventoryItem().getPriceInCentsIncl() - saleLine.getInventoryItem().getPriceInCentsExcl()), saleLine.getInventoryItem().getPriceInCentsExcl()});

                salesTaxRateOfItemWithinContextOfSale = 1 + (BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0) / 100);
            }

            log.debug("Standard item Tax rate for item is [{}]. Within the context of this sale, the tax rate is [{}]", itemTaxRateForStandardTaxableCustomers, salesTaxRateOfItemWithinContextOfSale);

            DiscountData discData;
            try {
                discData = getDiscountData(sale, saleLine, false, em);
                doAuthorisationCode(em, sale, saleLine, false);
            } catch (Exception e) {
                log.warn("Error: ", e);
                if (e.getCause() != null) {
                    log.warn("Error: ", e.getCause());
                }
                throw new Exception("Error calculating discounting/tiering -- " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            }

            applyDiscountingIntoLine(sale, saleLine, discData, salesTaxRateOfItemWithinContextOfSale);

            double subLineTotalCentsIncl = 0;
            double subLineTotalCentsExcl = 0;
            double subLineTotalDiscountCentsIncl = 0;
            double subLineTotalDiscountCentsExcl = 0;
            double subLineTotalItemPriceCentsIncl = 0;
            double subLineTotalItemPriceCentsExcl = 0;

            if (!saleLine.getSubSaleLines().isEmpty()) {

                log.debug("This is a kitted item");

                if (discData.hasReductions()) {
                    log.debug("The price of a kit has changed. Applying change to its sub items accordingly");
                    for (SaleLine subLine : saleLine.getSubSaleLines()) {
                        log.debug("Putting discount data and sub item tiered prices into kits sub line for item number [{}]", subLine.getInventoryItem().getItemNumber());
                        if ((BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0) > 0)
                                && !subLine.getInventoryItem().getItemNumber().startsWith("KIT")
                                && subLine.getInventoryItem().getPriceInCentsExcl() > 0
                                && subLine.getInventoryItem().getPriceInCentsIncl() != 0) {//saleLine.getInventoryItem().getPriceInCentsIncl() != 0
                            log.debug("This is a KIT we derive the rate at component level instead of parent line");
                            double tmpItemTaxRateForStandardTaxableCustomers = salesTaxRateOfItemWithinContextOfSale;
                            salesTaxRateOfItemWithinContextOfSale = 1 + (subLine.getInventoryItem().getPriceInCentsIncl() - subLine.getInventoryItem().getPriceInCentsExcl()) / subLine.getInventoryItem().getPriceInCentsExcl();
                            if (sale.isTaxExempt()) {
                                salesTaxRateOfItemWithinContextOfSale = 1;
                                if (subLine.getInventoryItem().getSerialNumber().equals("BUNDLE")) {
                                    salesTaxRateOfItemWithinContextOfSale = 1 + (BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0) / 100);
                                }
                            }
                            log.debug("Derived tax rate for this item is [{}], parent tax rate is [{}]", salesTaxRateOfItemWithinContextOfSale, tmpItemTaxRateForStandardTaxableCustomers);
                        }
                        applyDiscountingIntoLine(sale, subLine, discData, salesTaxRateOfItemWithinContextOfSale);
                        subLineTotalCentsIncl += subLine.getLineTotalCentsIncl();
                    }
                } else {
                    log.debug("The price of a kit has not changed. Calling tiered pricing and discounting engine on each sub item");
                    for (SaleLine subLine : saleLine.getSubSaleLines()) {
                        try {
                            discData = getDiscountData(sale, subLine, true, em);
                            doAuthorisationCode(em, sale, saleLine, true);
                        } catch (Exception e) {
                            throw new Exception("Error calculating discounting/tiering -- " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                        }
                        log.debug("Putting discount data and sub item tiered prices into kits sub line for item number [{}]", subLine.getInventoryItem().getItemNumber());
                        if ((BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0) > 0)
                                && !subLine.getInventoryItem().getItemNumber().startsWith("KIT")// Exclude parent
                                && subLine.getInventoryItem().getPriceInCentsExcl() > 0
                                && subLine.getInventoryItem().getPriceInCentsIncl() != 0) {
                            log.debug("This is a kit we derive the rate at component level instead of parent line");
                            double tmpItemTaxRateForStandardTaxableCustomers = salesTaxRateOfItemWithinContextOfSale;
                            salesTaxRateOfItemWithinContextOfSale = 1 + (subLine.getInventoryItem().getPriceInCentsIncl() - subLine.getInventoryItem().getPriceInCentsExcl()) / subLine.getInventoryItem().getPriceInCentsExcl();
                            if (sale.isTaxExempt()) {
                                salesTaxRateOfItemWithinContextOfSale = 1;
                                if (subLine.getInventoryItem().getSerialNumber().equals("BUNDLE")) {
                                    salesTaxRateOfItemWithinContextOfSale = 1 + (BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0) / 100);
                                }
                            }
                            log.debug("Derived tax rate for this item is [{}], parent tax rate is [{}]", salesTaxRateOfItemWithinContextOfSale, tmpItemTaxRateForStandardTaxableCustomers);
                        }
                        applyDiscountingIntoLine(sale, subLine, discData, salesTaxRateOfItemWithinContextOfSale);
                        subLineTotalCentsIncl += subLine.getLineTotalCentsIncl();
                        subLineTotalCentsExcl += subLine.getLineTotalCentsExcl();
                        subLineTotalDiscountCentsIncl += subLine.getLineTotalDiscountOnInclCents();
                        subLineTotalDiscountCentsExcl += subLine.getLineTotalDiscountOnExclCents();
                        subLineTotalItemPriceCentsIncl += subLine.getInventoryItem().getPriceInCentsIncl();
                        subLineTotalItemPriceCentsExcl += subLine.getInventoryItem().getPriceInCentsExcl();
                    }
                    log.debug("The Kits main price will now be adjusted according to the sub lines changes");
                    saleLine.setLineTotalCentsIncl(subLineTotalCentsIncl);
                    saleLine.setLineTotalCentsExcl(subLineTotalCentsExcl);
                    saleLine.setLineTotalDiscountOnInclCents(subLineTotalDiscountCentsIncl);
                    saleLine.setLineTotalDiscountOnExclCents(subLineTotalDiscountCentsExcl);
                    saleLine.getInventoryItem().setPriceInCentsIncl(subLineTotalItemPriceCentsIncl);
                    saleLine.getInventoryItem().setPriceInCentsExcl(subLineTotalItemPriceCentsExcl);
                    logSaleLine(saleLine, "Subsequent to Kit Line Tiering/Discounting");
                }
            }

            // Calculate running totals from per line values. VAT, discounts etc are all summed up from the detailed lines
            sale.setSaleTotalDiscountOnExclCents(sale.getSaleTotalDiscountOnExclCents() + saleLine.getLineTotalDiscountOnExclCents());
            sale.setSaleTotalDiscountOnInclCents(sale.getSaleTotalDiscountOnInclCents() + saleLine.getLineTotalDiscountOnInclCents());
            sale.setSaleTotalCentsExcl(sale.getSaleTotalCentsExcl() + saleLine.getLineTotalCentsExcl());
            sale.setSaleTotalCentsIncl(sale.getSaleTotalCentsIncl() + saleLine.getLineTotalCentsIncl());
            sale.setSaleTotalTaxCents(sale.getSaleTotalTaxCents() + (saleLine.getLineTotalCentsIncl() - saleLine.getLineTotalCentsExcl()));

        } // end looping through sales lines

        if (passedInTotal > 0 && (Math.abs(passedInTotal - sale.getSaleTotalCentsIncl()) >= 1)) {// compensate for rounding of the passed in value
            throw new Exception("The discounting engines calculated total does not match what was quoted");
        }

        if (BaseUtils.getBooleanProperty("env.pos.withholding.tax.on.gross")) {
            sale.setWithholdingTaxCents(sale.getSaleTotalCentsExcl() * sale.getWithholdingTaxRate() / 100d);
        } else {
            sale.setWithholdingTaxCents(sale.getSaleTotalCentsIncl() * sale.getWithholdingTaxRate() / 100d);
        }
        sale.setTotalLessWithholdingTaxCents(sale.getSaleTotalCentsIncl() - sale.getWithholdingTaxCents());

        if (log.isDebugEnabled()) {
            log.debug("After all tiered pricing and discounting:");
            log.debug("SaleTotalCentsExcl [{}]", sale.getSaleTotalCentsExcl());
            log.debug("SaleTotalCentsIncl [{}]", sale.getSaleTotalCentsIncl());
            log.debug("SaleTotalDiscountOnExclCents [{}]", sale.getSaleTotalDiscountOnExclCents());
            log.debug("SaleTotalDiscountOnInclCents [{}]", sale.getSaleTotalDiscountOnInclCents());
            log.debug("SaleTotalTaxCents [{}]", sale.getSaleTotalTaxCents());
            log.debug("WithholdingTaxCents [{}]", sale.getWithholdingTaxCents());
            log.debug("TotalLessWithholdingTaxCents [{}]", sale.getTotalLessWithholdingTaxCents());
        }

        sale.setPromotionCode(origPromoCode);
    }

    /**
     * Remove duplicate items
     */
    private List<SaleLine> getConsolidatedLines(List<SaleLine> saleLines) {
        log.debug("Consolidating sales lines");
        List<SaleLine> consolidatedSaleLines = new ArrayList<>(saleLines.size());
        Set<String> uniqueItemsInConsolidatedSaleLines = new HashSet<>(saleLines.size());
        int lineCnt = 0;
        for (SaleLine saleLine : saleLines) {
            log.debug("Looking at line with serial [{}] and quantity [{}]", saleLine.getInventoryItem().getSerialNumber(), saleLine.getQuantity());
            boolean lineExists = false;
            if (saleLine.getSubSaleLines().isEmpty()
                    && !POSManager.isAirtime(saleLine.getInventoryItem())
                    && !POSManager.isUnitCredit(saleLine.getInventoryItem())
                    && uniqueItemsInConsolidatedSaleLines.contains(saleLine.getInventoryItem().getItemNumber() + "_" + saleLine.getInventoryItem().getSerialNumber())) {
                for (SaleLine consolidatedLine : consolidatedSaleLines) {
                    // We do not want to consolidate itrems that have sub items as we may need to get serial numbers for each and every sub item
                    if (isSameItem(saleLine.getInventoryItem(), consolidatedLine.getInventoryItem())) {
                        consolidatedLine.setQuantity(consolidatedLine.getQuantity() + saleLine.getQuantity());
                        log.debug("Duplicate item found in sale. New quantity is now [{}]", consolidatedLine.getQuantity());
                        lineExists = true;
                    }
                }
            }
            if (!lineExists && saleLine.getQuantity() > 0) {
                lineCnt++;
                saleLine.setLineNumber(lineCnt);
                consolidatedSaleLines.add(saleLine);
                uniqueItemsInConsolidatedSaleLines.add(saleLine.getInventoryItem().getItemNumber() + "_" + saleLine.getInventoryItem().getSerialNumber());
                log.debug("Added line with serial [{}] and quantity [{}] to consolidated list", saleLine.getInventoryItem().getSerialNumber(), saleLine.getQuantity());
            } else {
                log.debug("Line is not being added into consolidated lines");
            }
        }
        log.debug("Consolidated sale has [{}] lines", consolidatedSaleLines.size());
        return consolidatedSaleLines;
    }

    private boolean isSameItem(InventoryItem i1, InventoryItem i2) {
        return (i1.getItemNumber().equals(i2.getItemNumber()) && i1.getSerialNumber().equals(i2.getSerialNumber()));
    }

    private DiscountData getDiscountData(Sale sale, SaleLine saleLine, boolean isSubItem, EntityManager em) throws Exception {
        String volumeCode = BaseUtils.getProperty("env.pos.discount.code");
        log.debug("Calling runtime compiled code to get the discount and tiered pricing reduction...");
        DiscountData res = (DiscountData) Javassist.runCode(new Class[]{this.getClass(), java.util.Calendar.class}, volumeCode, sale, saleLine, BaseUtils.getDoubleProperty("env.exchange.rate"), isSubItem, em);
        log.debug("Discount/Tiering code returned tiered lowering of [{}]% and discount of [{}]%", res.getTieredPricingPercentageOff(), res.getDiscountPercentageOff());
        return res;
    }

    private void doAuthorisationCode(EntityManager em, Sale sale, SaleLine saleLine, boolean isSubItem) throws Exception {
        String authorisationCode = BaseUtils.getProperty("env.pos.item.authorisation.code", "");

        if (authorisationCode == null || authorisationCode.isEmpty()) {
            log.debug("Authorisation code is not supplied - no customer authorisation is required.");
            return;
        } else {
            log.debug("Calling runtime compiled code to check if customer is allowed to purchase a specific item.");
            Javassist.runCode(new Class[]{this.getClass(), java.util.Calendar.class}, authorisationCode, em, sale, saleLine, BaseUtils.getDoubleProperty("env.exchange.rate"), isSubItem);
            log.debug("Customer [{}]% is allowed to  purchase item [{}]%", sale.getRecipientCustomerId(), saleLine.getInventoryItem().getItemNumber());
        }
        return;
    }

    private double getCentsRoundedForPOS(double val) {
        return Utils.round(val, BaseUtils.getIntProperty("env.pos.decimal.places.on.majorcurrency", X3Field.getX3DecimalPlaces()) - 2); // Subtract 2 as we are rounding cents
    }

    private void applyDiscountingIntoLine(Sale sale, SaleLine saleLine, DiscountData discData, double taxRate) {    	
        log.debug("Tax rate to use for this item [{}] is: [{}]", saleLine.getInventoryItem().getItemNumber(), taxRate);
        // Get per item after tax price - this is what X3 eventually gets
        double preDiscountPostTieringPerItemPriceAtSalesTaxRate = getCentsRoundedForPOS(saleLine.getInventoryItem().getPriceInCentsExcl() * (1 - discData.getTieredPricingPercentageOff() / 100d) * taxRate);
        log.debug("Item price at the tax rate of the sale [{}]. That is the price X3 will eventually be given", preDiscountPostTieringPerItemPriceAtSalesTaxRate);

        // PCB - Hack to fix X3 not accepting more than X decimal places. We must work with the same decimal places as X3
        
        saleLine.getInventoryItem().setPriceInCentsIncl(getCentsRoundedForPOS(saleLine.getInventoryItem().getPriceInCentsIncl() * (1 - discData.getTieredPricingPercentageOff() / 100d)));
        saleLine.getInventoryItem().setPriceInCentsExcl(getCentsRoundedForPOS(saleLine.getInventoryItem().getPriceInCentsExcl() * (1 - discData.getTieredPricingPercentageOff() / 100d)));
        
        //HBT-11528 - modify the airtime commission computation for TZ ICP (Selcom ) 
        if(saleLine.getInventoryItem().getSerialNumber().equals("AIRTIME") && sale.getRecipientOrganisationId() == 148 && sale.getRecipientAccountId() == 1208000054) {                
                Double selcomDiscount = 8.625731d;                
        	saleLine.setLineTotalCentsExcl(saleLine.getQuantity() * saleLine.getInventoryItem().getPriceInCentsExcl() * (1 - selcomDiscount / 100d));
                saleLine.setLineTotalCentsIncl(saleLine.getQuantity() * preDiscountPostTieringPerItemPriceAtSalesTaxRate * (1 - selcomDiscount / 100d));
	        saleLine.setLineTotalDiscountOnExclCents(saleLine.getQuantity() * saleLine.getInventoryItem().getPriceInCentsExcl() - saleLine.getLineTotalCentsExcl());
                saleLine.setLineTotalDiscountOnInclCents(saleLine.getQuantity() * preDiscountPostTieringPerItemPriceAtSalesTaxRate - saleLine.getLineTotalCentsIncl());
        } else {
            saleLine.setLineTotalCentsExcl(saleLine.getQuantity() * saleLine.getInventoryItem().getPriceInCentsExcl() * (1 - discData.getDiscountPercentageOff() / 100d));
            saleLine.setLineTotalCentsIncl(saleLine.getQuantity() * preDiscountPostTieringPerItemPriceAtSalesTaxRate * (1 - discData.getDiscountPercentageOff() / 100d));
            saleLine.setLineTotalDiscountOnExclCents(saleLine.getQuantity() * saleLine.getInventoryItem().getPriceInCentsExcl() - saleLine.getLineTotalCentsExcl());
            saleLine.setLineTotalDiscountOnInclCents(saleLine.getQuantity() * preDiscountPostTieringPerItemPriceAtSalesTaxRate - saleLine.getLineTotalCentsIncl());
        }
        
        logSaleLine(saleLine, "Sale Line Info After Pricing"); 
    }

    private void logSaleLine(SaleLine saleLine, String msg) {
        if (log.isDebugEnabled()) {
            log.debug(msg + ". Item [{}] with quantity [{}] and Serial [{}]", new Object[]{saleLine.getInventoryItem().getItemNumber(), saleLine.getQuantity(), saleLine.getInventoryItem().getSerialNumber()});
            log.debug("Item PriceInCentsExcl [{}]", saleLine.getInventoryItem().getPriceInCentsExcl());
            log.debug("Item PriceInCentsIncl [{}]", saleLine.getInventoryItem().getPriceInCentsIncl());
            log.debug("SaleLineCentsExcl [{}]", saleLine.getLineTotalCentsExcl());
            log.debug("SaleLineCentsIncl [{}]", saleLine.getLineTotalCentsIncl());
            log.debug("SaleLineDiscountOnExclCents [{}]", saleLine.getLineTotalDiscountOnExclCents());
            log.debug("SaleLineDiscountOnInclCents [{}]", saleLine.getLineTotalDiscountOnInclCents());
        }
    }

    public com.smilecoms.pos.DiscountData getDynamicDiscountDataTZ(com.smilecoms.xml.schema.pos.Sale sale, com.smilecoms.xml.schema.pos.SaleLine saleLine, double exRt, boolean isSubItem, javax.persistence.EntityManager em) throws Exception {
        String itemNumber = saleLine.getInventoryItem().getItemNumber();
        String serialNumber = saleLine.getInventoryItem().getSerialNumber();
        boolean isAirtime = serialNumber.equals("AIRTIME");
        boolean isRouter = itemNumber.startsWith("ROU700");
        String promoCode = sale.getPromotionCode();
        String paymentMethod = sale.getPaymentMethod();
        String ccNumber = sale.getCreditAccountNumber();
        int salesPersonId = sale.getSalesPersonCustomerId();
        long recipientAccountId = sale.getRecipientAccountId();
        boolean inQuote = (paymentMethod == null || paymentMethod.isEmpty());
        Double fullDisc = new Double(saleLine.getInventoryItem().getPriceInCentsIncl() * saleLine.getQuantity());
        String channel = sale.getChannel();
        com.smilecoms.pos.DiscountData dd = new com.smilecoms.pos.DiscountData();
        int orgId = sale.getRecipientOrganisationId();
        java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");

        if (paymentMethod == null) {
            paymentMethod = "";
        }
        if (ccNumber == null) {
            ccNumber = "";
        }

        javax.persistence.Query q = null;
        java.util.List lstResults = null;

        if (saleLine.getProvisioningData() != null && saleLine.getProvisioningData().contains("ParentUCI=")) {

            String parentUCI = com.smilecoms.commons.util.Utils.getValueFromCRDelimitedAVPString(saleLine.getProvisioningData(), "ParentUCI");

            q = em.createNativeQuery("select count(*) from unit_credit_instance where purchase_date >  '2019/05/15' and info like concat('%ParentUCI=',?,  '%');");

            q.setParameter(1, parentUCI);

            lstResults = q.getResultList();

            Long counter1 = (Long) lstResults.get(0);
            if (counter1.longValue() > 0) {
                throw new Exception("ERROR:  Kyookya bonus bundle for parent UCI " + parentUCI + " has already been redeemed.");
            }
        }

        if (sale.getSalesPersonAccountId() != 1208000054l && sale.getSalesPersonAccountId() != 1512000177l && itemNumber.equals("BUN1005")) {

            java.util.List roles = new java.util.ArrayList();
            roles.add("Administrator");
            com.smilecoms.commons.sca.CallersRequestContext crc = new com.smilecoms.commons.sca.CallersRequestContext("admin", "0.0.0.0", 1, roles);
            com.smilecoms.commons.sca.SCAWrapper.getUserSpecificInstance().setThreadsRequestContext(crc);
            com.smilecoms.commons.sca.Customer cust = com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper.getCustomer(sale.getRecipientCustomerId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER);
            int roleCnt = cust.getSecurityGroups().size();
            boolean found = false;
            for (int i = 0; i < roleCnt; i++) {
                String role = (String) cust.getSecurityGroups().get(i);
                if (role.equals("Promo3")) {
                    dd.setDiscountPercentageOff(50d);
                    dd.setTieredPricingPercentageOff(0d);
                    return dd;
                }
            }

        }

        if (itemNumber.startsWith("BUNST") || itemNumber.startsWith("BUNTV")) {
            if (!paymentMethod.equals("") && !paymentMethod.equals("Credit Account")) {
                throw new Exception("Staff bundles must be paid by credit account");
            }
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.startsWith("BUNTN")) {
            if (!paymentMethod.equals("") && !paymentMethod.equals("Credit Account")) {
                throw new Exception("Staff bundles must be paid by credit account");
            }
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BUN3010") && orgId == 23) {
            // Bollore pay 69 000 for 10GB 1 month validity bundle
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(13.75d);
            return dd;
        }

        if (itemNumber.equals("BUN3016") && orgId == 7) {
            // Halliburton pay 102 400 for 16GB 1 month validity bundle
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(17.4193548d);
            return dd;
        }

        if (itemNumber.equals("BUN3050") && orgId == 103) {
            // Halliburton pay 337 500 for 50GB 1 month validity bundle
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(10.0d);
            return dd;
        }

        if (itemNumber.startsWith("BUNP")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (!inQuote && !paymentMethod.equals("Staff") && !paymentMethod.equals("Credit Note") && !paymentMethod.equals("Loan") && promoCode.isEmpty()) {
            for (int i = 0; i < sale.getSaleLines().size(); i++) {
                com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                if (otherLine.getInventoryItem().getItemNumber().equals("MIF6003")) {
                    throw new Exception("Cannot sell MIFI standalone");
                }
            }
        }

        // 100% discount on routers and sims and dongles for gifts and give aways
        if (promoCode.equalsIgnoreCase("TestingDiscount")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        //100% tiered price drop for static IP testing
        if (itemNumber.startsWith("BUNIP") && promoCode.equals("100% Discount for static IP test")) {
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(100d);
            return dd;
        }

        // Price unlimited premium at price of essential
        if (itemNumber.equals("BUNU3021") && promoCode.equals("UnlimitedPremium at Essential Price")) {
            dd.setDiscountPercentageOff(45.9574468d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Temp unlimited discounts
        if (itemNumber.startsWith("BUNU") && promoCode.equals("Unlimited Discount 9.50%")) {
            dd.setDiscountPercentageOff(9.5d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // Temp unlimited discounts
        if (itemNumber.startsWith("BUNU") && promoCode.equals("Unlimited Discount 10.7%")) {
            dd.setDiscountPercentageOff(10.7d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // Temp unlimited discounts
        if (itemNumber.startsWith("BUNU") && promoCode.equals("Unlimited Discount 6.8%")) {
            dd.setDiscountPercentageOff(6.8d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Lahmeyer Consulting Engineers pay 800k for 2Mbps BE as approved by Ahmed/Tom 21/7/2015
        if (itemNumber.equals("BENTBE020") && orgId == 2813) {
            double discPercent = (saleLine.getInventoryItem().getPriceInCentsIncl() - 80000000d) * 100d / saleLine.getInventoryItem().getPriceInCentsIncl();
            dd.setDiscountPercentageOff(discPercent);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BENTBE010") && orgId == 4422) {
            dd.setDiscountPercentageOff(29.3785311d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BENTOS100") || itemNumber.equals("BENTOS200") || itemNumber.equals("BENTOS050")
                || itemNumber.equals("BENTMB005") || itemNumber.equals("BENTMB010") || itemNumber.equals("BENTMB020")
                || itemNumber.equals("BENTBE005") || itemNumber.equals("BENTBE010") || itemNumber.equals("BENTBE020")
                || itemNumber.equals("BENTFB013") || itemNumber.equals("BENTFB024") || itemNumber.equals("BENTFB035")
                || itemNumber.equals("BENTFB046") || itemNumber.equals("BUNU3002") || itemNumber.equals("BUNU3006")
                || itemNumber.equals("BUNU3016") || itemNumber.equals("BUNU3021")) {
            if (saleLine.getQuantity() >= 24) {
                dd.setDiscountPercentageOff(10d);
            } else if (saleLine.getQuantity() >= 12) {
                dd.setDiscountPercentageOff(5d);
            }
        }
        // Percentage based discounts for enterprise
        if (itemNumber.startsWith("BENT") && promoCode.contains("% Discount on Corporate Access")) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + discPercent);
            dd.setTieredPricingPercentageOff(0d);
        }

        if (dd.getDiscountPercentageOff() > 0.0d) {
            return dd;
        }

        // Percentage based discounts for ICPs HBT-6775
        if ((itemNumber.equals("KIT1155") || itemNumber.equals("KIT1157") || itemNumber.equals("KIT1159") || itemNumber.equals("KIT1152") || itemNumber.equals("KIT1141")) && promoCode.contains("% Discount on ICP Kits")) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(discPercent);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.startsWith("PNUM") && promoCode.contains("% Discount on Special Numbers")) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(discPercent);
            dd.setTieredPricingPercentageOff(0d);
        }

        if (!itemNumber.startsWith("BUN") && saleLine.getInventoryItem().getPriceInCentsIncl() == 0d && !paymentMethod.isEmpty() && !paymentMethod.equals("Credit Note")) {
            throw new Exception("A sale with a zero item price is not allowed!");
        }
        if (paymentMethod.equals("Loan") && !promoCode.equalsIgnoreCase("HotSpot")) {
            throw new Exception("Loan Payment methods not allowed at this point!");
        }
        if (!paymentMethod.isEmpty() && !paymentMethod.equals("Loan") && promoCode.equalsIgnoreCase("HotSpot")) {
            throw new Exception("Hotspots must be loaned");
        }
        if (!inQuote && !ccNumber.equals("CSI001") && (promoCode.equalsIgnoreCase("Education") || promoCode.equalsIgnoreCase("Community"))) {
            throw new Exception("CSI Promotions must be on the CSI credit account");
        }

        if (!isSubItem && !paymentMethod.isEmpty() && !paymentMethod.equals("Staff") && !paymentMethod.equals("Loan") && !paymentMethod.equals("Credit Note") && itemNumber.startsWith("SIM") && promoCode.isEmpty()) {
            throw new Exception("A standalone SIM can only be sold using a SIMSwap promotion code. SIMs sold with SIMSwap promotion codes can only be used in a SIM Swap");
        }

        String staffOnlyKits = "KIT1207 KIT1249 KIT1271 KIT1272 KIT1109 KIT1121 KIT1147 KIT1148 KIT1149 KIT1150 KIT1109 KIT1121 KIT1160 KIT1161 KIT1162";

        if ((staffOnlyKits.indexOf(itemNumber) != -1) && !paymentMethod.equals("Staff") && !inQuote) {
            throw new Exception("Selected Kit is exclusive to Smile Staff");
        } else { // Enfore the serial number for each kit as per HBT-7171
            String serialsForKIT1160 = "357943060840006";
            String serialsForKIT1161 = "357749/06/339409/3 357749/06/339415/0 357749/06/339418/4 354223061262911 354223061262978 354223061515780";
            String serialsForKIT1162 = "352564/07/174300/8 359523/06/896509/2 352564/07/177264/0 352564/07/177266/8";

            if (itemNumber.equals("KIT1160") && (serialsForKIT1160.indexOf(serialNumber) == -1)) {
                throw new Exception("Wromg serial number selected for " + itemNumber);
            } else if (itemNumber.equals("KIT1161") && (serialsForKIT1161.indexOf(serialNumber) == -1)) {
                throw new Exception("Wromg serial number selected for " + itemNumber);
            } else if (itemNumber.equals("KIT1162") && (serialsForKIT1162.indexOf(serialNumber) == -1)) {
                throw new Exception("Wromg serial number selected for " + itemNumber);
            }
        }

        // 100% discount for invoices generated in SCA for bundles
        if (!isAirtime && promoCode.equalsIgnoreCase("UCGiveAway")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // 50% discount promo code
        if (!isAirtime && promoCode.equalsIgnoreCase("HalfOffDiscount")) {
            dd.setDiscountPercentageOff(50d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // 100% discount on routers and sims for educational institutions
        if (!isAirtime && promoCode.equalsIgnoreCase("Education")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims for communities 
        if (!isAirtime && promoCode.equalsIgnoreCase("Community")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims for brand ambassadors
        if (!isAirtime && promoCode.equalsIgnoreCase("BrandAmbassador")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims and dongles for gifts and give aways
        if (!isAirtime && promoCode.equalsIgnoreCase("GiveAways")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Percentage based volume breaks
        if (!isAirtime && promoCode.contains("% Volume Break")) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(discPercent);
            return dd;
        }

        // 50% discount on routers and sims and dongles for Staff
        if (!isAirtime && promoCode.equalsIgnoreCase("Staff 50% Discount")) {
            dd.setDiscountPercentageOff(50d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (promoCode.equals("15% Discount on starter KITs")
                && (itemNumber.equals("KIT1131") || itemNumber.equals("KIT1128") || itemNumber.equals("KIT1132"))) {
            dd.setDiscountPercentageOff(15d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Percentage based discounts
        if (!isAirtime && promoCode.contains("% Discount")) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(discPercent);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BENTRC001") && promoCode.equals("13% Discount on Co-location")) {
            dd.setDiscountPercentageOff(13d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BENTRC001") && promoCode.equals("13% Discount on Co-location")) {
            dd.setDiscountPercentageOff(13d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Airtime bulk discounting for third parties
        if (isAirtime) {
            // E-Fulusi Africa (T) Limited - 8% Discount
            if (recipientAccountId == 1304000210) {
                dd.setDiscountPercentageOff(8d);
                dd.setTieredPricingPercentageOff(0d);
                return dd;
            }

            // Maxcom - 8% Discount
            if (recipientAccountId == 1304000198) {
                dd.setDiscountPercentageOff(8d);
                dd.setTieredPricingPercentageOff(0d);
                return dd;
            }

            // Selcom - 8% Discount            
            if (recipientAccountId == 1208000054) {
                
                //Calculation of Selcom was revised to 8.625731 to allow for difference in contract
                //as per HBT-11528
                
               // dd.setDiscountPercentageOff(8d);
                dd.setDiscountPercentageOff(8.625731d);
                dd.setTieredPricingPercentageOff(0d);
                return dd;
            }
            
            
            // Ezeemoney- 8% Discount
            if (recipientAccountId == 1503000453) {
                dd.setDiscountPercentageOff(8d);
                dd.setTieredPricingPercentageOff(0d);
                return dd;
            }
        }
        // British Gas pay 295 000 Incl for routers (only apply as a standalone now when part of KIT (e.g KIT1141)) 
        if (!isSubItem && isRouter && orgId == 103) {
            double discPercent = (saleLine.getInventoryItem().getPriceInCentsIncl() - 29500000d) * 100d / saleLine.getInventoryItem().getPriceInCentsIncl();
            dd.setDiscountPercentageOff(discPercent);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (orgId == 1396 || orgId == 1415 || orgId == 1863 || orgId == 1448 || orgId == 1450 || orgId == 2748) {
            if (!itemNumber.startsWith("BUN") && !paymentMethod.isEmpty() && !channel.startsWith("26P")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P");
            }

            if (isAirtime && (recipientAccountId == 1308000540 || recipientAccountId == 1309000348 || recipientAccountId == 1310000525 || recipientAccountId == 1310000572 || recipientAccountId == 1310000564 || recipientAccountId == 1405000689)) {
                // Orange, Bluestar, Capricorn, The Hub, Mobistock, Ezeemoney - 8% Discount on airtime
                dd.setDiscountPercentageOff(8d);
                dd.setTieredPricingPercentageOff(0d);
                return dd;
            }
            if (!isAirtime) {
                // Orange, Bluestar, Capricorn, The Hub Get 10% volume break
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(10d);
                return dd;
            }
        }

        String simKits = "KIT1110 KIT1111 KIT1112 KIT1117 KIT1127 KIT1119 ";
        String deviceKits = "KIT1128 KIT1129 KIT1102 KIT1108 KIT1101 KIT1105 KIT1100 KIT1115 KIT1114 KIT1103 KIT1106 KIT1104 KIT1109 KIT1113 KIT1107 KIT1120 ";

        if (orgId == 4154 // Cel Solutions               
                || orgId == 4869 // Maruti Office Supplies
                || orgId == 1870 // Freedom Electronics
                || orgId == 4899 // Complus
                || orgId == 5449 // Laut
                || orgId == 1374 // Smart banking solutions
                ) {
            // New ICP Contract
            if (!itemNumber.startsWith("BUN") && !paymentMethod.isEmpty() && !channel.startsWith("26P")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P");
            }
            int simKitCount = 0;
            int deviceCount = 0;

            for (int i = 0; i < sale.getSaleLines().size(); i++) {
                com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                String it = otherLine.getInventoryItem().getItemNumber();
                if (simKits.indexOf(it) != -1) {
                    simKitCount += otherLine.getQuantity();
                } else if (deviceKits.indexOf(it) != -1) {
                    deviceCount += otherLine.getQuantity();
                }
            }

            if (simKits.indexOf(itemNumber) != -1) {
                if ((orgId == 5449 && new java.util.Date().before(dateFormat.parse("2015-09-01")))) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                } else if (simKitCount >= 6 && simKitCount <= 14) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(8d);
                } else if (simKitCount >= 15) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                }
            }

            if (deviceKits.indexOf(itemNumber) != -1) {
                if ((orgId == 5449 && new java.util.Date().before(dateFormat.parse("2015-09-01")))) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                } else if (deviceCount >= 6 && deviceCount <= 14) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(8d);
                } else if (deviceCount >= 15) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                }
            }

            if (isAirtime) {
                double discTier1 = 1d / (1d / 0.05d + 1d);
                double discTier2 = 1d / (1d / 0.055d + 1d);
                double discTier3 = 1d / (1d / 0.06d + 1d);

                double tier1Start = 1000000d / (1d - discTier1);
                double tier2Start = 3000000d / (1d - discTier2);
                double tier3Start = 6000000d / (1d - discTier3);

                long airtimeUnits = saleLine.getQuantity();
                if (airtimeUnits >= tier1Start && airtimeUnits < tier2Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier1 * 100d);
                } else if (airtimeUnits >= tier2Start && airtimeUnits < tier3Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier2 * 100d);
                } else if (airtimeUnits >= tier3Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier3 * 100d);
                }
            }

        }

        if (orgId == 148 // Selcom
                || orgId == 6367 // F.M Pharmacy 
                || orgId == 6371 // Solani & Sons 
                || orgId == 6369 // Access Computer Limited
                || orgId == 6375 // Better Results Limited
                || orgId == 6373 // Cafe Barrista 
                || orgId == 6965 // Airtime Suppliers
                || orgId == 6573 // Fern Company Limited
                || orgId == 7105 // Safety Net Africa Company Limited
                || orgId == 7163 // Daus Internet and Computer Labs
                || orgId == 7253 // BHATT TELECOM LIMITED
                || orgId == 7509 // IRENE KAHEMELE
                || orgId == 8455 // Green Telecom
                ) {
            // Newest contract requested in HBT-3659
            if (!itemNumber.startsWith("BUN") && !paymentMethod.isEmpty() && !channel.startsWith("26P")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P");
            }

            if (simKits.indexOf(itemNumber) != -1) {
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(83.3333333333d);
            }

            if (isAirtime) {
                double discTier1 = 1d / (1d / 0.06d + 1d);
                double tier1Start = 5000000d / (1d - discTier1);

                long airtimeUnits = saleLine.getQuantity();
                if (airtimeUnits >= tier1Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier1 * 100d);
                }
            }
        }

        return dd;
    }

    public void doAuthorisationCode(javax.persistence.EntityManager em, com.smilecoms.xml.schema.pos.Sale sale, com.smilecoms.xml.schema.pos.SaleLine saleLine, double exRt, boolean isSubItem) throws Exception {

        int campaignId = 73;
        String itemNumber = saleLine.getInventoryItem().getItemNumber();
        javax.persistence.Query q = null;
        java.util.List lstResults = null;
        String paymentMethod = sale.getPaymentMethod();
        boolean isSubmitForPricing = (paymentMethod == null || paymentMethod.isEmpty());

        if (itemNumber.equalsIgnoreCase("KIT1375")) {

            // Kit should be sold by only retail staff at the Smile shops.
            String rolesAllowed = " 'ShopSales' "; //"'ShopSales'";
            q = em.createNativeQuery("select count(*) From security_group_membership where customer_profile_id=? "
                    + " and security_group_name in (" + rolesAllowed + ")");

            q.setParameter(1, Integer.valueOf(sale.getSalesPersonCustomerId()));

            lstResults = q.getResultList();

            Long numRoles = (Long) lstResults.get(0);

            if (numRoles.longValue() == 0) { // Sales person not allowed to sell this kit.
                throw new Exception("Your are not allowed to sell this KIT");
            }

        }

        if (itemNumber.equalsIgnoreCase("KIT1333")) {

            if (sale.getPurchaseOrderData() == null || sale.getPurchaseOrderData().isEmpty()) {
                throw new Exception("Returned Device Serial# must be supplied for KIT1333, please try again.");
            }

            //Check if is a device was sold to this customer and is out of warranty - caters for ICP provisioning
            q = em.createNativeQuery("select count(*) from sale_row SR  join sale S on (SR.serial_number = ? and S.sale_id = SR.sale_id "
                    + " and S.sale_date_time < now() -  interval 12 month) where "
                    + " not (item_number like 'SIM%' or item_number like 'KIT%' or item_number like 'BUN%' or item_number like 'BENT%');");

            q.setParameter(1, sale.getPurchaseOrderData());

            lstResults = q.getResultList();

            Long counter1 = (Long) lstResults.get(0);
            if (counter1.longValue() <= 0) {
                throw new Exception("Please confirm that serial number '" + sale.getPurchaseOrderData() + "' was sold to this customer and is also out of Warranty.");
            }

            q = em.createNativeQuery(" select count(*) from sale S where S.purchase_order_data = ? and status != 'RV';");
            q.setParameter(1, sale.getPurchaseOrderData());

            lstResults = q.getResultList();

            Long counter2 = (Long) lstResults.get(0);
            if (counter2.longValue() > 0) {
                throw new Exception("Please confirm that serial number '" + sale.getPurchaseOrderData() + "' was not replaced before?");
            }

            q = em.createNativeQuery("select count(*) as enrolments from service_instance SI join campaign_run CR on (SI.product_instance_id = CR.product_instance_id "
                    + "     and SI.customer_profile_id=? and CR.campaign_id=? and account_id=?)");

            q.setParameter(1, Integer.valueOf(sale.getRecipientCustomerId()));
            q.setParameter(2, Integer.valueOf(campaignId));
            q.setParameter(3, Long.valueOf(sale.getRecipientAccountId()));

            lstResults = q.getResultList();

            Long numEnrolments = (Long) lstResults.get(0);

            // java.math.BigDecimal numEnrolments = (java.math.BigDecimal) row[0];
            if (numEnrolments.longValue() > 0) {// Product has been a member of this campaign
                return;
            } else {
                throw new Exception("Customer with id " + sale.getRecipientCustomerId() + " is not allowed to purchase KIT1333.");
            }
        }

        if (itemNumber.equalsIgnoreCase("KIT1348") || itemNumber.equalsIgnoreCase("KIT1349") || itemNumber.equalsIgnoreCase("KIT1370")) {

            if (sale.getPurchaseOrderData() == null || sale.getPurchaseOrderData().isEmpty()) {
                throw new Exception("Returned Device Serial# must be supplied for KIT1337, please try again.");
            }

            // Kit should be sold by only retail staff at the Smile shops.
            String rolesAllowed = " 'ShopSales' "; //"'ShopSales'";
            q = em.createNativeQuery("select count(*) From security_group_membership where customer_profile_id=? "
                    + " and security_group_name in (" + rolesAllowed + ")");

            q.setParameter(1, Integer.valueOf(sale.getSalesPersonCustomerId()));

            lstResults = q.getResultList();

            Long numRoles = (Long) lstResults.get(0);

            if (numRoles.longValue() == 0) { // Sales person not allowed to sell this kit.
                throw new Exception("Your are not allowed to sell this KIT");
            }

            // Allow sale of kit if the customer account number was selected when making sale
            if (sale.getRecipientAccountId() <= 0) {
                throw new Exception("Customer account number not supplied.");
            }

            // SEP to validate that the serial of the return faulty device is a MiFi serial number.
            // SEP to validate that the serial of the return faulty device is within warranty
            q = em.createNativeQuery("select count(*) from sale_row SR  join sale S on (SR.serial_number = ? and S.sale_id = SR.sale_id "
                    + "and S.sale_date_time < now() -  interval 12 month) "
                    + "where item_number like 'MIF%'");

            q.setParameter(1, sale.getPurchaseOrderData());

            lstResults = q.getResultList();

            Long counter3 = (Long) lstResults.get(0);
            if (counter3.longValue() <= 0) {
                throw new Exception("Please confirm that serial number '" + sale.getPurchaseOrderData() + "' is a MIFI and is out of warranty.");
            }

            boolean refurbishedSerialFound = false;

            String serialNumber = "";
            for (int i = 0; i < saleLine.getSubSaleLines().size(); i++) {

                com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) saleLine.getSubSaleLines().get(i);

                if (com.smilecoms.commons.base.BaseUtils.getPropertyAsList("env.allowed.refurbished.devices.for.replacement").contains(otherLine.getInventoryItem().getSerialNumber())) {
                    refurbishedSerialFound = true;
                    serialNumber = otherLine.getInventoryItem().getSerialNumber();
                    break;
                }
            }

            //SEP to validate that serial number entered on submit for pricing page is on the whitelisted list of refurbished devices serial numbers to ensure sales are made from the refurbished available stock
            if (!refurbishedSerialFound && !isSubmitForPricing) {
                throw new Exception("Device serial number [" + serialNumber + "] is not on the whitelisted list of refurbished devices serial numbers.");
            }

            q = em.createNativeQuery(" select count(*) from sale S where S.purchase_order_data = ? and status != 'RV';");
            q.setParameter(1, sale.getPurchaseOrderData());

            lstResults = q.getResultList();

            Long counter2 = (Long) lstResults.get(0);
            if (counter2.longValue() > 0) {
                throw new Exception("Please confirm that serial number '" + sale.getPurchaseOrderData() + "' was not replaced before?");
            }
        }

        if (itemNumber.equalsIgnoreCase("KIT1347") || itemNumber.equalsIgnoreCase("KIT1369")) {

            if (sale.getPurchaseOrderData() == null || sale.getPurchaseOrderData().isEmpty()) {
                throw new Exception("Returned Device Serial# must be supplied for KIT1338, please try again.");
            }

            // Kit should be sold by only retail staff at the Smile shops.
            String rolesAllowed = " 'ShopSales' "; //"'ShopSales'";
            q = em.createNativeQuery("select count(*) From security_group_membership where customer_profile_id=? "
                    + " and security_group_name in (" + rolesAllowed + ");");

            q.setParameter(1, Integer.valueOf(sale.getSalesPersonCustomerId()));

            lstResults = q.getResultList();

            Long numRoles = (Long) lstResults.get(0);

            if (numRoles.longValue() == 0) { // Sales person not allowed to sell this kit.
                throw new Exception("Your are not allowed to sell this KIT");
            }

            // Allow sale of kit if the customer account number was selected when making sale
            if (sale.getRecipientAccountId() <= 0) {
                throw new Exception("Customer account number not supplied.");
            }

            // SEP to validate that the serial of the return faulty device is a MiFi serial number.
            // SEP to validate that the serial of the return faulty device is within warranty
            q = em.createNativeQuery("select count(*) from sale_row SR  join sale S on (SR.serial_number = ? and S.sale_id = SR.sale_id "
                    + " and S.sale_date_time >= now() -  interval 12 month) "
                    + " where item_number like 'MIF%';");

            q.setParameter(1, sale.getPurchaseOrderData());

            lstResults = q.getResultList();

            Long counter1 = (Long) lstResults.get(0);
            if (counter1.longValue() <= 0) {
                throw new Exception("Please confirm that serial number '" + sale.getPurchaseOrderData() + "' is a MIFI and is within warranty.");
            }

            boolean refurbishedSerialFound = false;
            String serialNumber = "";
            for (int i = 0; i < saleLine.getSubSaleLines().size(); i++) {

                com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) saleLine.getSubSaleLines().get(i);

                if (com.smilecoms.commons.base.BaseUtils.getPropertyAsList("env.allowed.refurbished.devices.for.replacement").contains(otherLine.getInventoryItem().getSerialNumber())) {
                    refurbishedSerialFound = true;
                    serialNumber = otherLine.getInventoryItem().getSerialNumber();
                    break;
                }
            }

            //SEP to validate that serial number entered on submit for pricing page is on the whitelisted list of refurbished devices serial numbers to ensure sales are made from the refurbished available stock
            if (!refurbishedSerialFound && !isSubmitForPricing) {
                throw new Exception("Device serial number [" + serialNumber + "] is not on the whitelisted list of refurbished devices serial numbers.");
            }

            q = em.createNativeQuery(" select count(*) from sale S where S.purchase_order_data = ? and status != 'RV';");
            q.setParameter(1, sale.getPurchaseOrderData());

            lstResults = q.getResultList();

            Long counter2 = (Long) lstResults.get(0);
            if (counter2.longValue() > 0) {
                throw new Exception("Please confirm that serial number '" + sale.getPurchaseOrderData() + "' was not replaced before?");
            }

        }

        // Kit should be only sold to low utalization sites.
        if (itemNumber.equalsIgnoreCase("KIT1379")) {

            java.util.List allowedSalesPersonsForLowUtilizationSites = new java.util.ArrayList();

            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(8641));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(14339));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(14339));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(40108));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(40108));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(40108));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(454903));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(305687));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(109363));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(253133));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(446707));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(222967));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(253577));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(308499));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(187839));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(251195));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(481947));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(470969));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(372697));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(546137));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(546137));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(241191));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(241225));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(411617));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(240991));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(241083));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(241177));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(374247));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(131317));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(241889));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(408553));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(223009));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(519659));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(122831));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(520947));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(520947));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(327563));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(166297));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(523047));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(233319));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(131369));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(131369));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(601585));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(22794));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(516587));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(15892));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(15892));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(23096));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(220427));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(251847));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(22802));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(22751));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(62025));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(398669));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(241603));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(241603));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(241603));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(369135));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(22769));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(241185));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(473895));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(473895));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(498133));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(124649));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(409669));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(5195));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(226497));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(226497));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(103017));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(103017));
            allowedSalesPersonsForLowUtilizationSites.add(java.lang.Integer.valueOf(498117));

            q = em.createNativeQuery("select count(product_instance_id) from product_instance where customer_profile_id=?");
            q.setParameter(1, java.lang.Integer.valueOf(sale.getRecipientCustomerId()));
            lstResults = q.getResultList();
            Long numProduct = (Long) lstResults.get(0);
            if (numProduct.longValue() > 0) { // This is not a new customer.
                throw new Exception("Customer not allowed to purchase this KIT ");
            }

            if (!allowedSalesPersonsForLowUtilizationSites.contains(java.lang.Integer.valueOf(sale.getSalesPersonCustomerId()))) {
                throw new Exception("Sales person " + sale.getSalesPersonCustomerId() + " is not allowed to sell this device.");
            }

            boolean refurbishedSerialFound = false;
            String serialNumber = "";
            for (int i = 0; i < saleLine.getSubSaleLines().size(); i++) {
                com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) saleLine.getSubSaleLines().get(i);

                if (com.smilecoms.commons.base.BaseUtils.getPropertyAsList("env.allowed.refurbished.devices.for.lowutilizationsites").contains(otherLine.getInventoryItem().getSerialNumber())) {
                    refurbishedSerialFound = true;
                    serialNumber = otherLine.getInventoryItem().getSerialNumber();
                    break;
                }
            }
        }

    }

// Super Dealer and ICP config for new model
// This is used by SEP and POS to know which Orgs are Super Dealers and ICPs
// <Org Id> ACC: <Commission account> DISC: <Discount %> TYPE: <SD or ICP> <Name>
// Kaduna ICPs treated as Super Dealers
// 6179 ACC: 1507002203 DISC: 8% TYPE: ICP Mo Concepts Co. LTD
// 9631 ACC: 1704006564 DISC: 8% TYPE: ICP Crysolyte Networks
// 7163 ACC: 1601005343 DISC: 8% TYPE: ICP Bloom Associates
// 9649 ACC: 1704009598 DISC: 8% TYPE: ICP NM MARGI NIG LTD
// 6929 ACC: 1602002373 DISC: 8% TYPE: ICP Micromanna Ltd.
// 2917 ACC: 1402000211 DISC: 8% TYPE: SD Yfree Solutions
// 2652 ACC: 1507002419 DISC: 8% TYPE: FRA Ditrezi Enterprises
// 5479 ACC: 1503002012 DISC: 8% TYPE: SD ImageAddiction
// 6555 ACC: 1509005251 DISC: 8% TYPE: SD Veemobile services
// 6643 ACC: 1510002993 DISC: 8% TYPE: SD Dahrah Global Limited
// 6993 ACC: 1512006591 DISC: 8% TYPE: SD Kima-Maxi Nigeria Limited
// 6997 ACC: 1512006592 DISC: 8% TYPE: SD Value Plus Service & Industries L
// 7009 ACC: 1512006586 DISC: 8% TYPE: SD Detoyibo Enterprises
// 7021 ACC: 1512006589 DISC: 8% TYPE: SD Toluque Nigeria Limited
// 7045 ACC: 1512007980 DISC: 8% TYPE: SD Payit Consulting Limited
// 7417 ACC: 1602003768 DISC: 8% TYPE: SD Synergy Mobile Business Solutions Limited
// 7431 ACC: 1602004047 DISC: 8% TYPE: SD Dpro Project Limited
// 7843 ACC: 1604007384 DISC: 8% TYPE: SD SAANET GLOBAL SERVICES LIMITED
// 8283 ACC: 1705003619 DISC: 8% TYPE: SD Digital Development Hub
// 9539 ACC: 1703001193 DISC: 8% TYPE: SD Intergrity Communications
// 9783 ACC: 1705015937 DISC: 8% TYPE: SD 23RD Century Technologies LTD
// 9815 ACC: 1706008776 DISC: 8% TYPE: SD GENIALLINKS SERVICES
// 9825 ACC: 1706008774 DISC: 8% TYPE: SD TIMBOY VENTURES
// 9831 ACC: 1706012063 DISC: 8% TYPE: SD Universal Asset Commercial Boker
// 9941 ACC: 1707002065 DISC: 8% TYPE: SD Manjub Limited
// 10025 ACC: 1707008135 DISC: 8% TYPE: SD Olans Communications
// 10075 ACC: 1708003269 DISC: 8% TYPE: SD Ultimate Aslan Limited
// Franchises
// 8909 ACC: 1609001970 DISC: 7% TYPE: FRA Code2take Ventures
// 6633 ACC: 1511001587 DISC: 11% TYPE: SD Lyds Louisa Intergrated Company Limited
// 8623 ACC: 1607009915 DISC: 11% TYPE: SD ZEPH ASSOCIATES NIGERIA LIMITED
// 8641 ACC: 1608000342 DISC: 11% TYPE: SD Idems First Network Limited
// 8639 ACC: 1607011274 DISC: 11% TYPE: SD Gabros Divine Technologies Limited
// 8649 ACC: 1608000265 DISC: 11% TYPE: SD CANT STOP NIGERIA LIMITED
// 9109 ACC: 1610001302 DISC: 11% TYPE: SD Concise Services Limited
// 2643 ACC: 1312000182 DISC: 11% TYPE: SD Timjat Global Resources Ltd
// 9615 ACC: 1704001876 DISC: 11% TYPE: SD Digital Development Hub Consultancy Services Ltd
// 9647 ACC: 1704009231 DISC: 11% TYPE: SD YFree SD
// 10085 ACC: 1708004284 DISC: 8% TYPE: ICP Global Communication and Digital services
// 10077 ACC: 1708003338 DISC: 8% TYPE: ICP Onigx Investment Limited
// 10079 ACC: 1708003426 DISC: 8% TYPE: ICP LTC network limited
// 10059 ACC: 1708001482 DISC: 8% TYPE: ICP Glitteringway enterprise
// 10009 ACC: 1707007946 DISC: 8% TYPE: ICP Asrad Multi-links Business and logistic limited
// 10001 ACC: 1707006592 DISC: 8% TYPE: ICP BlueBreed Enterprise
// 10025 ACC: 1707008135 DISC: 8% TYPE: ICP Olans Communication
// 10041 ACC: 1707010667 DISC: 8% TYPE: ICP Base 15 Concept Nigeria Limited
// 9571 ACC: 1707010662 DISC: 8% TYPE: ICP Cyberdyne Integrated Limited
// 7431 ACC: 1602004047 DISC: 8% TYPE: ICP Dpro Project
// 9885 ACC: 1707001965 DISC: 8% TYPE: ICP Happymood enterprise
// 9975 ACC: 1707005327 DISC: 8% TYPE: ICP Godsaves devices Tech Ltd
// 9945 ACC: 1707008942 DISC: 8% TYPE: ICP Aumak-mikash Ltd
// 9999 ACC: 1707007126 DISC: 8% TYPE: ICP Aje Global Multiservices
// 9963 ACC: 1707004296 DISC: 8% TYPE: ICP FEM9 TOUCH LIMITED
// 9973 ACC: 1707005483 DISC: 8% TYPE: ICP Joelink Global Concept communication Ltd
// 9967 ACC: 1707005338 DISC: 8% TYPE: ICP Goldenworths West Africa LTD
// 5488 ACC: 1501003415 DISC: 8% TYPE: ICP Blessing Computers
// 5519 ACC: 1502000278 DISC: 8% TYPE: ICP Burj Khalifah Nigeria Limited
// 2652 ACC: 1612003434 DISC: 8% TYPE: ICP Ditrezi Enterprises
// 2917 ACC: 1402000211 DISC: 8% TYPE: ICP Yfree Solutions
// 9947 ACC: 1707003031 DISC: 8% TYPE: ICP DANDOLAS NIGERIA LIMITED
// 9941 ACC: 1707002065 DISC: 8% TYPE: ICP Manjub Limited
// 9939 ACC: 1707002036 DISC: 8% TYPE: ICP Tecboom Digital Ltd
// 9933 ACC: 1707001131 DISC: 8% TYPE: ICP Da-Costa F.A and Company
// 9929 ACC: 1707000869 DISC: 8% TYPE: ICP Jb Mwonyi Company Limited
// 9883 ACC: 1706014165 DISC: 8% TYPE: ICP Oroborkso Ventures Nigeria Limited
// 9875 ACC: 1706014128 DISC: 8% TYPE: ICP Chrisoduwa Global Ventures
// 9887 ACC: 1706014329 DISC: 8% TYPE: ICP Multitronics Global Enterprise
// 9861 ACC: 1706014118 DISC: 8% TYPE: ICP DRUCICARE NIG LTD
// 9881 ACC: 1706014150 DISC: 8% TYPE: ICP Beejao Infosolutions Limited
// 9877 ACC: 1706014141 DISC: 8% TYPE: ICP Sam Fransco Resources
// 9873 ACC: 1706014121 DISC: 8% TYPE: ICP GLORIOUS TELECOMM
// 9867 ACC: 1707000906 DISC: 8% TYPE: ICP AMIN AUTO LTD
// 9931 ACC: 1707000923 DISC: 8% TYPE: ICP Boxtech Consult
// 9649 ACC: 1704009598 DISC: 8% TYPE: ICP NM MARGI NIG LTD
// 9897 ACC: 1707000781 DISC: 8% TYPE: ICP Brownnys-Connect Nigeria Limited
// 9835 ACC: 1706010115 DISC: 8% TYPE: ICP K-CEE DEVICES LIMITED
// 9859 ACC: 1706014143 DISC: 8% TYPE: ICP Budddybod Enterprises
// 9863 ACC: 1706013579 DISC: 8% TYPE: ICP GOLDENPEECOMPUTERS TECHNOLOGIES
// 9831 ACC: 1706012063 DISC: 8% TYPE: ICP Universal Asset Commercial Boker
// 9847 ACC: 1706011240 DISC: 8% TYPE: ICP Fundripples Limited
// 9841 ACC: 1706011245 DISC: 8% TYPE: ICP Sharptowers Limited
// 9549 ACC: 1703001162 DISC: 8% TYPE: ICP Payconnect Networks Ltd
// 9827 ACC: 1706009025 DISC: 8% TYPE: ICP Leverage Options
// 9793 ACC: 1706008830 DISC: 8% TYPE: ICP Sunsole Global Investment Ltd
// 9801 ACC: 1706008832 DISC: 8% TYPE: ICP Godmic Nigeria LTD
// 9825 ACC: 1706008774 DISC: 8% TYPE: ICP Timboy Ventures
// 9815 ACC: 1706008776 DISC: 8% TYPE: ICP Geniallinks Services
// 9783 ACC: 1705015937 DISC: 8% TYPE: ICP 23RD Century Technologies LTD
// 9755 ACC: 1705010786 DISC: 8% TYPE: ICP Abiola Gold Resources
// 9753 ACC: 1705010787 DISC: 8% TYPE: ICP Hetoch Technology
// 9747 ACC: 1705009480 DISC: 8% TYPE: ICP Mac Onella
// 9711 ACC: 1705006481 DISC: 8% TYPE: ICP Finet Communications Ltd
// 9723 ACC: 1705007543 DISC: 8% TYPE: ICP Rezio Limited
// 6179 ACC: 1507002203 DISC: 8% TYPE: ICP MO Concepts co. Ltd
// 8283 ACC: 1705003619 DISC: 8% TYPE: ICP Digital Development Hub
// 5985 ACC: 1506002422 DISC: 8% TYPE: ICP Capricorn Digital
// 9657 ACC: 1704011368 DISC: 8% TYPE: ICP Spacesignal Ltd
// 7219 ACC: 1601008476 DISC: 8% TYPE: ICP TYJPEE Global Service
// 5598 ACC: 1502005940 DISC: 8% TYPE: ICP P N B Bytes Resources
// 9637 ACC: 1704007239 DISC: 8% TYPE: ICP Network Enterprises PH
// 9625 ACC: 1704006509 DISC: 8% TYPE: ICP Global Lightcrown Ventures
// 9631 ACC: 1704006564 DISC: 8% TYPE: ICP CRYSOLYTE NETWORKS
// 3143 ACC: 1402000481 DISC: 8% TYPE: ICP Koams Solutions
// 3675 ACC: 1406000144 DISC: 8% TYPE: ICP Trend Worlwide Ltd
// 3677 ACC: 1406000147 DISC: 8% TYPE: ICP Bankytech Comm Ventures
// 5500 ACC: 1501003476 DISC: 8% TYPE: ICP Spectrum Innovation Technologies
// 3412 ACC: 1406000145 DISC: 8% TYPE: ICP Galant Communications Ltd
// 2918 ACC: 1402000215 DISC: 8% TYPE: ICP Aghayedo Brother Enterprises
// 4222 ACC: 1408000539 DISC: 8% TYPE: ICP Barcutt Investment Ltd
// 5493 ACC: 1501003451 DISC: 8% TYPE: ICP Pinacle Wireless Ltd
// 5506 ACC: 1501004187 DISC: 8% TYPE: ICP Dot.Com
// 5713 ACC: 1504002740 DISC: 8% TYPE: ICP Recharge Hub
// 5715 ACC: 1504002778 DISC: 8% TYPE: ICP Suremobile Networks
// 4938 ACC: 1408001017 DISC: 8% TYPE: ICP Global Rolling Technologies
// 5833 ACC: 1505001221 DISC: 8% TYPE: ICP F.F.Tech Communication
// 6581 ACC: 1509006920 DISC: 8% TYPE: ICP ING Systems
// 6615 ACC: 1510001332 DISC: 8% TYPE: ICP Genie Infotech
// 6437 ACC: 1508005507 DISC: 8% TYPE: ICP New Sonshyne
// 7845 ACC: 1604007573 DISC: 8% TYPE: ICP Banc Solutions
// 8051 ACC: 1605005403 DISC: 8% TYPE: ICP Rulesixmobile
// 8185 ACC: 1606000715 DISC: 8% TYPE: ICP PayQuick
// 8191 ACC: 1606001008 DISC: 8% TYPE: ICP Kemasho
// 8107 ACC: 1606008018 DISC: 8% TYPE: ICP Cordiant Technology
// 2888 ACC: 1402000629 DISC: 8% TYPE: ICP Pheg Marathon
// 6417 ACC: 1509000345 DISC: 8% TYPE: ICP Simcoe & Dalhousie
// 5921 ACC: 1505005109 DISC: 8% TYPE: ICP Oakmead Realtors
// 6307 ACC: 1508001986 DISC: 8% TYPE: ICP Brightgrab
// 5567 ACC: 1502003116 DISC: 8% TYPE: ICP Acmesynergy
// 5565 ACC: 1502002826 DISC: 8% TYPE: ICP Broad Projects
// 6523 ACC: 1509003263 DISC: 8% TYPE: ICP The Koptim Company
// 8361 ACC: 1606007627 DISC: 8% TYPE: ICP SmartDrive & Systems Limited
// 5680 ACC: 1504000784 DISC: 8% TYPE: ICP Trebkann Nig. Ltd
// 6311 ACC: 1508001134 DISC: 8% TYPE: ICP Donval Platinum
// 5074 ACC: 1410002018 DISC: 8% TYPE: ICP Globasure Technologies
// 6923 ACC: 1512002524 DISC: 8% TYPE: ICP LeeBroad Ventures
// 8511 ACC: 1607008247 DISC: 8% TYPE: ICP Nartel Ventures
// 5504 ACC: 1501004137 DISC: 8% TYPE: ICP Belle-Vista
// 5489 ACC: 1501003418 DISC: 8% TYPE: ICP Wanaga Nig Ltd
// 3413 ACC: 1406000156 DISC: 8% TYPE: ICP Wireles Link Integrated
// 4679 ACC: 1408000945 DISC: 8% TYPE: ICP Ideal Hospitality
// 6271 ACC: 1507005163 DISC: 8% TYPE: ICP Avensa Global 
// 6149 ACC: 1507001087 DISC: 8% TYPE: ICP Bailord Ltd
// 5837 ACC: 1505001228 DISC: 8% TYPE: ICP Cheroxy Nig Ltd
// 6013 ACC: 1506002217 DISC: 8% TYPE: ICP Viclyd Enterprise
// 5925 ACC: 1505005013 DISC: 8% TYPE: ICP Foiim Technology
// 6155 ACC: 1507001275 DISC: 8% TYPE: ICP Sweet Planet
// 6407 ACC: 1508004268 DISC: 8% TYPE: ICP Lumron Enterprise
// 6299 ACC: 1508000989 DISC: 8% TYPE: ICP Best Baxany
// 6827 ACC: 1511003671 DISC: 8% TYPE: ICP Top Coat Limited
// 6623 ACC: 1510001977 DISC: 8% TYPE: ICP Sovereign Gold
// 6607 ACC: 1510000847 DISC: 8% TYPE: ICP Kollash Business and Technologies
// 6547 ACC: 1509005008 DISC: 8% TYPE: ICP KVC E-Mart
// 6733 ACC: 1510006947 DISC: 8% TYPE: ICP Isladex Communications
// 6735 ACC: 1510007028 DISC: 8% TYPE: ICP Avro Group
// 6823 ACC: 1511003342 DISC: 8% TYPE: ICP Omovo Tv
// 6821 ACC: 1511003293 DISC: 8% TYPE: ICP Fibrepath Limited 
// 7149 ACC: 1506000291 DISC: 8% TYPE: ICP SDMA Networks Limited  
// 7145 ACC: 1601004566 DISC: 8% TYPE: ICP Morgenall Global Limited
// 7347 ACC: 1602001116 DISC: 8% TYPE: ICP Amarcelo Nig Ltd
// 7477 ACC: 1602006358 DISC: 8% TYPE: ICP ALTS Consult Services Limited
// 7851 ACC: 1604007898 DISC: 8% TYPE: ICP Next BT Nig Ltd
// 7791 ACC: 1604001542 DISC: 8% TYPE: ICP Divine Anchor Nig Ltd
// 7821 ACC: 1604005277 DISC: 8% TYPE: ICP Grinotech Nig Ltd
// 8215 ACC: 1606002044 DISC: 8% TYPE: ICP I-Cell Mobile
// 5490 ACC: 1501003431 DISC: 8% TYPE: ICP Primadas Investment Nig Ltd
// 5729 ACC: 1504003622 DISC: 8% TYPE: ICP OLIVE INTEGRATED SERVICES
// 5482 ACC: 1501003738 DISC: 8% TYPE: ICP ZIONELI LIMITED
// 5289 ACC: 1412000818 DISC: 8% TYPE: ICP KORENET TECHNOLOGIES LTD
// 5529 ACC: 1502000810 DISC: 8% TYPE: ICP Arctuals Concepts Limited
// 5473 ACC: 1501003735 DISC: 8% TYPE: ICP AZMAN CONCEPTS LIMITED
// 5487 ACC: 1501003736 DISC: 8% TYPE: ICP NORTH EAST GOLD
// 5474 ACC: 1501003737 DISC: 8% TYPE: ICP InfoTek Perspective Services Limited
// 5823 ACC: 1505000924 DISC: 8% TYPE: ICP MERC-OATON TECHNOLOGIES LIMITED
// 5555 ACC: 1502003979 DISC: 8% TYPE: ICP COMTRANET IT SOLUTIONS LTD
// 6747 ACC: 1510007607 DISC: 8% TYPE: ICP First Regional Solutions Limited
// 7295 ACC: 1601010317 DISC: 8% TYPE: ICP Commonwealth Technologies Ltd
// 5839 ACC: 1505001367 DISC: 8% TYPE: ICP PRISTINE PANACHE LIMITED
// 6555 ACC: 1509005251 DISC: 8% TYPE: ICP Veemobile services
// 5479 ACC: 1503002012 DISC: 8% TYPE: ICP ImageAddiction
// 6805 ACC: 1511002557 DISC: 8% TYPE: ICP Global Quid Ltd
// 6461 ACC: 1509001625 DISC: 8% TYPE: ICP Febby communication Nigeria limited
// 6423 ACC: 1508005012 DISC: 8% TYPE: ICP Wonder Worth Global Investment Ltd
// 5162 ACC: 1411000821 DISC: 8% TYPE: ICP Sainana Resources
// 5292 ACC: 1412000819 DISC: 8% TYPE: ICP Skyraph Intergrated Servicesu Limited
// 6541 ACC: 1509004144 DISC: 8% TYPE: ICP Mohim Security Services and Consultancy Ltd
// 7727 ACC: 1603010674 DISC: 8% TYPE: ICP NEOCORTEX INTERNATIONAL LTD
// 5577 ACC: 1502004789 DISC: 8% TYPE: ICP Jotech Computer Co. Ltd.
// 6369 ACC: 1508002662 DISC: 8% TYPE: ICP MCEDWARDS ENT.LTD
// 6599 ACC: 1510000393 DISC: 8% TYPE: ICP Phones and Computers One-Stop Center Limited
// 5472 ACC: 1501003213 DISC: 8% TYPE: ICP Onis Digital Ventures
// 7197 ACC: 1601007501 DISC: 8% TYPE: ICP Skenyo Global Services Limited
// 7771 ACC: 1604000388 DISC: 8% TYPE: ICP Zillion Technocom
// 6685 ACC: 1510004241 DISC: 8% TYPE: ICP Soofine Print Limited
// 6519 ACC: 1509003097 DISC: 8% TYPE: ICP  Rakameris Ventures  
// 5279 ACC: 1412000433 DISC: 8% TYPE: ICP TOBOWY NIGERIA LIMITED
// 5215 ACC: 1411001937 DISC: 8% TYPE: ICP CHRISTY-GEORGE
// 5056 ACC: 1411001346 DISC: 8% TYPE: ICP Everyday Group
// 5444 ACC: 1501002271 DISC: 8% TYPE: ICP Pearl Inter Facet Resources
// 5446 ACC: 1501002273 DISC: 8% TYPE: ICP Central Dynamics Ltd
// 5483 ACC: 1501003505 DISC: 8% TYPE: ICP 3CEE TECHNOLOGIES
// 5445 ACC: 1501002272 DISC: 8% TYPE: ICP Emmrose Technologies Ltd
// 5450 ACC: 1501002270 DISC: 8% TYPE: ICP SACEN PROJECTS LIMITED
// 5060 ACC: 1501003504 DISC: 8% TYPE: ICP Integral Com (NIG) Ltd
// 5449 ACC: 1501003506 DISC: 8% TYPE: ICP Sayrex phone communication
// 5059 ACC: 1503001021 DISC: 8% TYPE: ICP Callus Miller Company Limited
// 5062 ACC: 1506003787 DISC: 8% TYPE: ICP Chuxel Computers
// 6181 ACC: 1507002247 DISC: 8% TYPE: ICP SHELL EAST STAFF INVESTMENT CO-OP SOCIETY LMT
// 6419 ACC: 1508004778 DISC: 8% TYPE: ICP SWING-HIGH COMMUNICATIONS LIMITED
// 6459 ACC: 1509001373 DISC: 8% TYPE: ICP CHARLYN NETWORKS LTD
// 6659 ACC: 1510003308 DISC: 8% TYPE: ICP YOMEEZ ELECTROWORLD LTD
// 7167 ACC: 1601005651 DISC: 8% TYPE: ICP DAKECH DYNAMIC SERVICES NIG LTD
// 7159 ACC: 1601005251 DISC: 8% TYPE: ICP MULTI RESOURCES AND INDUSTRIAL SERVICES NIG LTD
// 6769 ACC: 1602003001 DISC: 8% TYPE: ICP G-PAY INSTANT SOLUTIONS
// 7453 ACC: 1602005395 DISC: 8% TYPE: ICP Harry Concern Nigeria Enterprises
// 8819 ACC: 1608006679 DISC: 8% TYPE: ICP PINAAR UNIVERSAL LTD
// 6787 ACC: 1511003555 DISC: 8% TYPE: ICP Digital Direct
// 6819 ACC: 1511003971 DISC: 8% TYPE: ICP Xtraone Family Minstrel Ventures
// 6917 ACC: 1512002151 DISC: 8% TYPE: ICP Moben-D Excel Limited
// 6785 ACC: 1511003588 DISC: 8% TYPE: ICP Nofot Gardma Limited
// 5889 ACC: 1505003402 DISC: 8% TYPE: ICP Optimum Greeland Limited
// 6537 ACC: 1509003925 DISC: 8% TYPE: ICP D.S. Computer - Tech
// 6653 ACC: 1511001589 DISC: 8% TYPE: ICP Ambuilt works Ltd
// 6625 ACC: 1511001586 DISC: 8% TYPE: ICP Surfspot Communications Limited
// 6081 ACC: 1506005174 DISC: 8% TYPE: ICP Mesizanee Concept Limited.
// 5929 ACC: 1506000291 DISC: 8% TYPE: ICP SDMA Networks Limited
// 8903 ACC: 1609001070 DISC: 8% TYPE: ICP Emma Paradise Multilinks Limited
// 7089 ACC: 1601001377 DISC: 8% TYPE: ICP Madeeyak Nigeria Limited
// 8949 ACC: 1609002135 DISC: 8% TYPE: ICP Trevari
// 6771 ACC: 1511001721 DISC: 8% TYPE: ICP Easy Talil Enterprise
// 8983 ACC: 1609005821 DISC: 8% TYPE: ICP WATERGATE TELECOMMUNICATION
// 6657 ACC: 1511000486 DISC: 8% TYPE: ICP Rich Technologies Limited	
// 6745 ACC: 1511001590 DISC: 8% TYPE: ICP Joeche Computer Warehouse Limited	
// 6175 ACC: 1507001839 DISC: 8% TYPE: ICP Great Bakis Ventures Limited
// 9151 ACC: 1610006349 DISC: 8% TYPE: ICP Red and White Global Concept Ltd
// 8969 ACC: 1610010473 DISC: 8% TYPE: ICP Edge Baseline Solution
// 9173 ACC: 1610010961 DISC: 8% TYPE: ICP Riggsco Percific concept limited
// 9197 ACC: 1611000665 DISC: 8% TYPE: ICP Enbelo Company Limited
// 9221 ACC: 1611005581 DISC: 8% TYPE: ICP Ritech Network Enterprises
// 2630 ACC: 1310000420 DISC: 8% TYPE: ICP NETPOINT COMMUNICATIONS
// 2653 ACC: 1401000533 DISC: 8% TYPE: ICP ZACFRED SYSTEMS SOLUTIONS LTD
// 5539 ACC: 1502001765 DISC: 8% TYPE: ICP GLOBAL HANDSET LTD ( I WILL DISTRIBUTE ENT)
// 9039 ACC: 1609008911 DISC: 8% TYPE: ICP Hadiel Calidad Limited
// 7429 ACC: 1602004034 DISC: 8% TYPE: ICP Idea Konsult
// 6779 ACC: 1511001375 DISC: 8% TYPE: ICP The Gizmo world And Solution Limited
// 6179 ACC: 1507002203 DISC: 8% TYPE: ICP Mo Concepts Co. LTD
// 6861 ACC: 1507004510 DISC: 8% TYPE: ICP General B.Y. Supermarket
// 5631 ACC: 1503003181 DISC: 8% TYPE: ICP Ages Accurate Speed LTD
// 5534 ACC: 1502000967 DISC: 8% TYPE: ICP Yield Systems Limited
// 6779 ACC: 1511001375 DISC: 8% TYPE: ICP The Gizmo world And Solution Limited
// 7493 ACC: 1611006408 DISC: 8% TYPE: ICP Ski Current Service Limited
// 6289 ACC: 1508000009 DISC: 8% TYPE: ICP Final Logic
// 7491 ACC: 1602009235 DISC: 8% TYPE: ICP Emir Label
// 1361 ACC: 1503004497 DISC: 8% TYPE: ICP Onilu prime
// 7217 ACC: 1601008727 DISC: 8% TYPE: ICP REMGLORY VENTURES NIGERIA
// 8139 ACC: 1605007749 DISC: 8% TYPE: ICP Texmaco Ventures Nigeria Limited
// 7209 ACC: 1602004152 DISC: 8% TYPE: ICP U-MAT SUCCESS VENTURES
// 7697 ACC: 1603009927 DISC: 8% TYPE: ICP TUCAS GLOBAL RESOURCES
// 7215 ACC: 1601008729 DISC: 8% TYPE: ICP TOKAZ CONCEPT
// 9091 ACC: 1609010883 DISC: 8% TYPE: ICP Enatel Communications
// 7381 ACC: 1602002402 DISC: 8% TYPE: ICP POYEN NOMOVO NIG LTD
// 9255 ACC: 1611006221 DISC: 8% TYPE: ICP Brown Street Limited
// 9271 ACC: 1611008529 DISC: 8% TYPE: ICP Raz Resources
// 6565 ACC: 1509006328 DISC: 8% TYPE: ICP EMSI Enterprises Limited
// 5127 ACC: 1411000823 DISC: 8% TYPE: ICP Content Oasis limited
// 9293 ACC: 1612001954 DISC: 8% TYPE: ICP DA MORIS NIGERIA ENTERPRISES
// 5479 ACC: 1612008149 DISC: 8% TYPE: ICP ImageAddiction
// 9363 ACC: 1701000464 DISC: 8% TYPE: ICP Chuvexton Technovation Enterprises
// 9349 ACC: 1612007905 DISC: 8% TYPE: ICP Bama Global Concept Limited
// 9397 ACC: 1701003701 DISC: 8% TYPE: ICP Gold Vicky Communications Ltd
// 9405 ACC: 1701004853 DISC: 8% TYPE: ICP Double A Wireless
// 9413 ACC: 1701006201 DISC: 8% TYPE: ICP Darlin Ronkus Phones Ventures
// 9415 ACC: 1701006832 DISC: 8% TYPE: ICP Emmanuel Nelson Mobile Vetures
// 9451 ACC: 1701011176 DISC: 8% TYPE: ICP Temperature Limited
// 9473 ACC: 1702000782 DISC: 8% TYPE: ICP Leads Hotel Integrated Services
// 9489 ACC: 1702001605 DISC: 8% TYPE: ICP Enhanced Payment Solutions
// 9463 ACC: 1702002043 DISC: 8% TYPE: ICP UNIQUE EVERMORE GLOBAL VENTURES
// 9511 ACC: 1702003409 DISC: 8% TYPE: ICP NPNG GLOBAL VENTURES
// 9521 ACC: 1702004999 DISC: 8% TYPE: ICP Demasworld Resource limited
// 9483 ACC: 1702001841 DISC: 8% TYPE: ICP Homekonnekt Limited
// 9543 ACC: 1702006254 DISC: 8% TYPE: ICP Padillax Technology Limited
// 9547 ACC: 1703000627 DISC: 8% TYPE: ICP Ficoven Investments Limited
// 9559 ACC: 1703002062 DISC: 8% TYPE: ICP Blu Edition International Limited
// 9553 ACC: 1703001554 DISC: 8% TYPE: ICP Joshfat Global Investment Ltd
// 9539 ACC: 1703001193 DISC: 8% TYPE: ICP Integrity Communications
// 9575 ACC: 1703005182 DISC: 8% TYPE: ICP Fabisek Investment Limited
// 9577 ACC: 1703005043 DISC: 8% TYPE: ICP Ironwill Business Services
// 9589 ACC: 1703006673 DISC: 8% TYPE: ICP E-Gate Technology Consults Ltd. ID
// 9601 ACC: 1703008371 DISC: 8% TYPE: ICP Chinwaks Global Nig Ltd
// 9595 ACC: 1703007903 DISC: 8% TYPE: ICP Liricom Nig Ltd
// 9605 ACC: 1703008748 DISC: 8% TYPE: ICP Peritus technologies limited
// 9599 ACC: 1703008749 DISC: 8% TYPE: ICP Golden eagles technology
// 9477 ACC: 1702001112 DISC: 8% TYPE: ICP Tribal Marks Media Productions Limited
// 9613 ACC: 1704001833 DISC: 8% TYPE: ICP Jegkobah Integrate Service Ltd
// 9629 ACC: 1704006245 DISC: 8% TYPE: ICP Diksolyn Concept Limited
// 9649 ACC: 1704009598 DISC: 8% TYPE: ICP NM Margi Nig Ltd
// 9695 ACC: 1705005014 DISC: 8% TYPE: ICP Easycomm global connection
// 9693 ACC: 1705005016 DISC: 8% TYPE: ICP Matuluko and son
// 8283 ACC: 1705003619 DISC: 8% TYPE: ICP Digital Development Hub
// 9797 ACC: 1706003164 DISC: 8% TYPE: ICP Cida-woods Limited
// 11175 ACC: 1802006011 DISC: 8% TYPE: ICP Ammid Adex Telecom
    public com.smilecoms.pos.DiscountData getDynamicDiscountDataNGNew999(com.smilecoms.xml.schema.pos.Sale sale, com.smilecoms.xml.schema.pos.SaleLine saleLine, double exRt, boolean isSubItem) throws Exception {
        String itemNumber = saleLine.getInventoryItem().getItemNumber();
        String serialNumber = saleLine.getInventoryItem().getSerialNumber();
        boolean isAirtime = serialNumber.equals("AIRTIME");
        boolean isBundle = itemNumber.startsWith("BUN");
        boolean isKitBundle = itemNumber.startsWith("BUNK");
        boolean isNonKitBundle = (isBundle && !isKitBundle);
        String promoCode = sale.getPromotionCode();
        String paymentMethod = sale.getPaymentMethod();
        String ccNumber = sale.getCreditAccountNumber();
        int salesPersonId = sale.getSalesPersonCustomerId();
        long recipientAccountId = sale.getRecipientAccountId();
        boolean inQuote = (paymentMethod == null || paymentMethod.isEmpty());
        com.smilecoms.pos.DiscountData dd = new com.smilecoms.pos.DiscountData();
        String channel = sale.getChannel();
        String warehouse = sale.getWarehouseId();
        boolean isKit = itemNumber.startsWith("KIT");
        int orgId = sale.getRecipientOrganisationId();

        if (recipientAccountId == 1410001298) {
            dd.setDiscountPercentageOff(8d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.startsWith("MWE003") && !(orgId > 0)) {

            throw new Exception("Product '" + itemNumber + "' can only be sold to corporate customers.");
        }

        if (paymentMethod == null) {
            paymentMethod = "";
        }
        if (ccNumber == null) {
            ccNumber = "";
        }

        if (salesPersonId == 28381 && ((isBundle && !isKitBundle) || isAirtime)) {
            throw new Exception("You are barred from selling airtime and bundles. You can only sell kits");
        }

        // New super dealer sales process
        // The list of physical items that can be sold to Super Dealers for their discount of 11%
        String SDItems = "SIM8000 SIM8001 SIM8002 SIM8003 SIM8004 KIT1255 KIT1256 KIT1257 KIT1294 KIT1301 KIT1303 KIT1287 KIT1283 KIT1285 KIT1326 KIT1328 KIT1330 KIT1345 ";

        //New ICP contract for Kaduna ICPs
        // HBT-6748 and HBT-7376 - New Franchise Model
        if (!itemNumber.startsWith("BUN") && (orgId == 8909 // Code2take Ventures
                )) {

            // New ICP Contract for Kaduna ICPs
            if (!paymentMethod.isEmpty() && !channel.startsWith("26P") && !channel.startsWith("27P") && !itemNumber.startsWith("BUN")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }

            if (SDItems.indexOf(itemNumber) != -1) {
                // This is one of the items in SDItems list
                int cnt = 0;
                // This logic counts how many kit items there are in the sale so we know if they have met their mimium of 500 in order to get the discount
                for (int i = 0; i < sale.getSaleLines().size(); i++) {
                    com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                    String it = otherLine.getInventoryItem().getItemNumber();
                    if (SDItems.indexOf(it) != -1) {
                        cnt += otherLine.getQuantity();
                    }
                }

                if (cnt >= 50 || promoCode.equalsIgnoreCase("Franchise Discount")) {
                    dd.setDiscountPercentageOff(7d);
                    dd.setTieredPricingPercentageOff(0d);
                    return dd;
                }
            }

            if (isAirtime) {
                double discTier1 = 1d / (1d / 0.05d + 1d);
                double tier1Start = 2000000d / (1d - discTier1);

                long airtimeUnits = saleLine.getQuantity();
                if (airtimeUnits >= tier1Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier1 * 100d);
                    return dd;
                }
            }

            if (itemNumber.startsWith("KIT")) {
                // Dont allow the special Super Dealer KITs to be sold to anyone else
                throw new Exception("One or more of the items in the sale cannot be sold to Super Dealers");
            }
        }

        // HBT-7239: Kaduna does not have a SD at the moment , all ICP in the region needs to be set up like SD on set up but get 8% discount.
        if (!itemNumber.startsWith("BUN") && (orgId == 6643 // Dahrah Global Limited
                || orgId == 6555 // Veemobile Services
                || orgId == 6997 // Value plus service & Industries Ltd
                || orgId == 7021 // Toluque Nigeria Limited
                || orgId == 6993 // Kima-Maxi Nigeria Limited
                || orgId == 7045 // Payit consulting Limited
                || orgId == 7009 // Detoyibo Enterprises
                || orgId == 2652 // Ditrezi Enterprises
                || orgId == 7843 // SAANET GLOBAL SERVICES LIMITED
                || orgId == 5479 // ImageAddiction
                || orgId == 9539 // Integrity Communications
                || orgId == 8283 // Digital Development Hub
                || orgId == 7417 // Synergy Mobile Business Solutions Limited
                || orgId == 9783 // 23RD Century Technologies LTD	
                || orgId == 9825 // Timboy Ventures
                || orgId == 9815 // Geniallinks Services
                || orgId == 9831 // Universal Asset Commercial Boker
                || orgId == 9941 // Manjub Limited
                || orgId == 10025 //Olans Communication
                || orgId == 7431 // Dpro Project
                || orgId == 10075 // Ultimate Aslan Limited
                )) {
            // New ICP Contract for Kaduna ICPs
            if (!paymentMethod.isEmpty() && !channel.startsWith("26P") && !channel.startsWith("27P") && !itemNumber.startsWith("BUN")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }

            if (SDItems.indexOf(itemNumber) != -1) {
                // This is one of the items in SDItems list
                int cnt = 0;
                // This logic counts how many kit items there are in the sale so we know if they have met their mimium of 500 in order to get the discount
                for (int i = 0; i < sale.getSaleLines().size(); i++) {
                    com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                    String it = otherLine.getInventoryItem().getItemNumber();
                    if (SDItems.indexOf(it) != -1) {
                        cnt += otherLine.getQuantity();
                    }
                }

                if (cnt >= 500 || promoCode.equalsIgnoreCase("ICP Device Discount")) {
                    dd.setDiscountPercentageOff(8d);
                    dd.setTieredPricingPercentageOff(0d);
                    return dd;
                }
            }

            if (isAirtime) {
                double discAllTiers = 1d / (1d / 0.07d + 1d);
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(discAllTiers * 100d);
                return dd;
            }

            if (itemNumber.startsWith("KIT")) {
                // Dont allow the special Super Dealer KITs to be sold to anyone else
                throw new Exception("One or more of the items in the sale cannot be sold to Super Dealers");
            }

        }

        if (itemNumber.equals("KIT1336") && orgId != 2917 && promoCode.equalsIgnoreCase("Super Dealer Discount")) {
            throw new Exception("That kit (KIT1336) is only for YFree.");
        }

        if (promoCode.equalsIgnoreCase("Super Dealer Discount") && orgId == 2917 && itemNumber.equalsIgnoreCase("KIT1336")) {
            dd.setDiscountPercentageOff(1.232d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // New super dealer sales process (Super Dealers)
        if (orgId == 8623 // ZEPH ASSOCIATES NIGERIA LIMITED
                || orgId == 8641 // Data Partners Limited
                || orgId == 8639 // Gabros Divine Technologies Limited
                || orgId == 8649 // CANT STOP NIGERIA LIMITED
                || orgId == 9109 // Concise Services Limited
                || orgId == 2643 // Timjat Global Resources Ltd
                || orgId == 9615 // Digital Development Hub Consultancy Services Ltd
                || orgId == 9647 // YFree SD
                || orgId == 6633 // Lyds Louisa Intergrated Company Limited
                || orgId == 2917 // Yfree Solutions
                ) {
            // The item is being sold to a Super Dealer Organisation
            if (!paymentMethod.isEmpty() && !channel.startsWith("26P") && !channel.startsWith("27P") && !itemNumber.startsWith("BUN")) {
                // Dont allow the wrong channel to be used when selling to Super Dealers
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }

            if (isAirtime && saleLine.getQuantity() >= 5000000) {
                // This is airtime of over 5M
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(9.09090909d); // 9.0909% discount is the same as giving 10% extra airtime
                return dd;
            } else if (SDItems.indexOf(itemNumber) != -1) {
                // This is one of the items in SDItems list
                int cnt = 0;
                // This logic counts how many kit items there are in the sale so we know if they have met their mimium of 500 in order to get the discount
                for (int i = 0; i < sale.getSaleLines().size(); i++) {
                    com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                    String it = otherLine.getInventoryItem().getItemNumber();
                    if (SDItems.indexOf(it) != -1) {
                        cnt += otherLine.getQuantity();
                    }
                }

                if (cnt >= 500 || promoCode.equalsIgnoreCase("Super Dealer Discount")) {
                    // The sale does have over 500 devices in it
                    dd.setDiscountPercentageOff(11d);
                    dd.setTieredPricingPercentageOff(0d);
                    return dd;
                }
            } else if (itemNumber.startsWith("KIT")) {
                throw new Exception("One or more of the items in the sale cannot be sold to Super Dealers");
            }
        } else if (itemNumber.startsWith("KIT") && SDItems.indexOf(itemNumber) != -1) {
            // Dont allow the special Super Dealer KITs to be sold to anyone else
            throw new Exception("One or more of the items in the sale can only be sold to Super Dealers");
        }

        if (itemNumber.startsWith("BUNST") || itemNumber.startsWith("BUNTV")) {
            if (!paymentMethod.equals("") && !paymentMethod.equals("Credit Account")) {
                throw new Exception("Staff bundles must be paid by credit account");
            }
            // 100% discount for Staff bundles
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.startsWith("BUNP") && saleLine.getInventoryItem().getPriceInCentsIncl() > 0d) {
            if (!paymentMethod.equals("") && !paymentMethod.equals("Cash") && !itemNumber.equals("BUNPG0000")) {
                throw new Exception("Gift bundles must be paid by cash");
            }
            // 100% discount for Promotional bundles
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // HBT-7231 - Creation of Promo Bundle for Winback Campaign
        if (itemNumber.equals("BUNDC4003")) {
            // 23.89649924% discount as per Mike's comment on HBT-7231 to bring the price down to NGN1000
            dd.setDiscountPercentageOff(27.5204754656809d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // HBT-7178 = Promo's on Retail Products
        if (itemNumber.equals("KIT1261")) {
            dd.setDiscountPercentageOff(30d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("KIT1304")) {
            dd.setDiscountPercentageOff(45d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        if (itemNumber.equals("KIT1236")) {
            dd.setDiscountPercentageOff(60d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BUN1020") && (paymentMethod.equals("Credit Account") || paymentMethod.isEmpty()) && orgId == 8465) {
            // 5% discount for NCC on 20GB Bundle as per HBT-6414
            dd.setDiscountPercentageOff(5d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // HBT-6815 
        if ((orgId == 3199) && (itemNumber.equals("BENTOS200") || itemNumber.equals("BUN1200")) && promoCode.equals("4.76% off 200GB Anytime Sharing Bundle")) {
            dd.setDiscountPercentageOff(4.76d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

// HBT-7104
        if ((orgId == 8557 || orgId == 6091) && (itemNumber.equals("BENTOS200") || itemNumber.equals("BUN1200")) && promoCode.equals("5% off 200GB Anytime Sharing Bundle")) {
            dd.setDiscountPercentageOff(5.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Ensure that sales of non corporate GBR items are not open for more than 7 days
        boolean isCorpGBR = itemNumber.startsWith("BENT");
        if ((isAirtime || isKit) && !isCorpGBR && sale.getExpiryDate() != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sale.getSaleDate().toGregorianCalendar().getTime());
            cal.add(java.util.Calendar.DATE, 7);
            if (sale.getExpiryDate().toGregorianCalendar().getTime().after(cal.getTime())) {
                throw new Exception("Only Corporate GBR can be on an invoice open for more than 7 days");
            }
        }

        // 100% discount on routers and sims and dongles testing purposes when testing new products, kits etc
        if (promoCode.equalsIgnoreCase("TestingDiscount")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BENTBE010") || itemNumber.equals("BENTBE020") || itemNumber.equals("BENTBE030")) {
            if (saleLine.getQuantity() >= 12) {
                // 10% discount when buying enterprise best effort 12 months in advance
                dd.setDiscountPercentageOff(10d);
            } else if (saleLine.getQuantity() >= 6) {
                // 10% discount when buying enterprise best effort 6 months in advance
                dd.setDiscountPercentageOff(5d);
            }
            if (itemNumber.equals("BENTBE010") || itemNumber.equals("BENTBE020") || itemNumber.equals("BENTBE030")) {
                if (promoCode.equalsIgnoreCase("5% Discount on Corporate Access")) {
                    // Additional 5% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 5d);
                } else if (promoCode.equalsIgnoreCase("10% Discount on Corporate Access")) {
                    // Additional 10% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 10d);
                } else if (promoCode.equalsIgnoreCase("15% Discount on Corporate Access")) {
                    // Additional 15% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 15d);
                } else if (promoCode.equalsIgnoreCase("16.5% Discount on Corporate Access")) {
                    // Additional 16.5% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 16.5d);
                } else if (promoCode.equalsIgnoreCase("30% Discount on Corporate Access")) {
                    // Additional 30% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 30d);
                } else if (promoCode.equalsIgnoreCase("33% Discount on Corporate Access")) {
                    // Additional 33% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 33d);
                }
            }
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.startsWith("ROU")) {
            for (int i = 0; i < sale.getSaleLines().size(); i++) {
                com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                String it = otherLine.getInventoryItem().getItemNumber();
                if (otherLine.getQuantity() >= 6 && (it.equals("BENTBE010") || it.equals("BENTBE020") || it.equals("BENTBE030"))) {
                    // Get 100% off the router when buying 6 months enterprise best effort up front
                    dd.setDiscountPercentageOff(100d);
                    return dd;
                }
            }
        }

        if (itemNumber.equals("KIT1217") && orgId != 3700) {
            throw new Exception("That kit is only for Diamond Bank");
        }

        if (!itemNumber.equals("KIT1293") && !itemNumber.equals("KIT1287") && !itemNumber.equals("KIT1289") && !itemNumber.equals("KIT1288") && !itemNumber.equals("KIT1290") && !itemNumber.startsWith("BAT") && !itemNumber.startsWith("SIM") && !itemNumber.startsWith("BUN") && saleLine.getInventoryItem().getPriceInCentsIncl() == 0d && !paymentMethod.isEmpty() && !paymentMethod.equals("Credit Note") && sale.getSalesPersonCustomerId() != 1) {
            throw new Exception("A sale with a zero item price is not allowed!");
        }

        if (!isSubItem && !paymentMethod.isEmpty() && !paymentMethod.equals("Staff") && !paymentMethod.equals("Loan") && !paymentMethod.equals("Credit Note") && itemNumber.startsWith("SIM") && promoCode.isEmpty() && !promoCode.contains("100% Volume Break")) {
            throw new Exception("A standalone SIM can only be sold using a SIMSwap promotion code. SIMs sold with SIMSwap promotion codes can only be used in a SIM Swap");
        }

        if (!paymentMethod.isEmpty() && !paymentMethod.equals("Loan") && promoCode.equalsIgnoreCase("HotSpot")) {
            throw new Exception("Hotspots must be loaned");
        }
        if (itemNumber.equals("KIT1207") && !paymentMethod.equals("Staff") && !inQuote) {
            throw new Exception("Selected Kit is exclusive to Smile Staff");
        }
        if (!inQuote && !ccNumber.equals("CSI003") && (promoCode.equalsIgnoreCase("Education") || promoCode.equalsIgnoreCase("Community"))) {
            throw new Exception("CSI Promotions must be on the CSI credit account. Add CSI003 as the credit account number for this Organisation and select Credit Account as the payment method");
        }

        // 100% discount on routers and sims for educational institutions
        if (!isAirtime && promoCode.equalsIgnoreCase("Education")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims for brand ambassadors
        if (!isAirtime && promoCode.equalsIgnoreCase("BrandAmbassador")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on sims for faulty sim
        if (itemNumber.startsWith("SIM") && promoCode.equalsIgnoreCase("FreeSIMSwap")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims for communities 
        if (!isAirtime && promoCode.equalsIgnoreCase("Community")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims and dongles for gifts and give aways
        if (!isAirtime && promoCode.equalsIgnoreCase("UCGiveAway")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on ICP tools of trade event
        if (!isAirtime && promoCode.equalsIgnoreCase("ICPToolOfTrade")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Discounts when doing returns/replacements of ATEL
        if (!isSubItem && (itemNumber.equals("MIF6003") || itemNumber.equals("MIF6006")) && promoCode.contains("Pays 30%")) {
            dd.setDiscountPercentageOff(70d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        if (!isSubItem && (itemNumber.equals("MIF6003") || itemNumber.equals("MIF6006")) && promoCode.contains("Pays 50%")) {
            dd.setDiscountPercentageOff(50d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        if (!isSubItem && (itemNumber.equals("MIF6003") || itemNumber.equals("MIF6006")) && promoCode.contains("Pays 70%")) {
            dd.setDiscountPercentageOff(30d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // 100% discount on static IP
        if (itemNumber.startsWith("BUNIP") && promoCode.equalsIgnoreCase("100% discount on Static IP")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Percentage based volume breaks
        if (!isAirtime && promoCode.contains("% Volume Break") && !isNonKitBundle) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(discPercent);
            return dd;
        }

        // Percentage based discounts
        if (!isAirtime && promoCode.contains("% Discount") && !isNonKitBundle) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(discPercent);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Temp for airtime commissions
        if (isAirtime && promoCode.equals("100% Off Airtime")) {
            dd.setDiscountPercentageOff(100.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (orgId == 2889 // Asbam Global Resources
                || orgId == 2893 // Lightning Networks
                || orgId == 2894 // Green Heritage                
                ) {
            // Old ICP Contract
            if (!paymentMethod.isEmpty() && !channel.startsWith("21E") && !channel.startsWith("25I") && !channel.startsWith("27I") && !itemNumber.startsWith("BUN")) {
                throw new Exception("Sales to ICPs must use channel starting with 21E or 25I or 27I");
            }
            if (isAirtime) {
                // ICPs get 8% Discount
                dd.setDiscountPercentageOff(8d);
                dd.setTieredPricingPercentageOff(0d);
                return dd;
            }
            if (isKit) {
                // ICPs Get 10% off kits
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(10d);
                return dd;
            }
        }

        if (itemNumber.equals("BUNIC1005")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        String simKits = "KIT1208 KIT1209 KIT1210";
        String deviceKits = "KIT1238 KIT1239 KIT1200 KIT1201 KIT1202 KIT1203 KIT1204 KIT1205 KIT1206 KIT1211 KIT1212 KIT1213 KIT1214 KIT1215 KIT1218 KIT1219 KIT1220 KIT1222 KIT1223 KIT1221 KIT1224 KIT1227 KIT1228 KIT1229 KIT1231 KIT1232 KIT1240 KIT1241 KIT1242 KIT1244 KIT1245 KIT1246 KIT1251 KIT1252 KIT1267 KIT1263 KIT1261 KIT1265 KIT1279 KIT1280 KIT1266 KIT1286 KIT1281 KIT1284 KIT1282 KIT1274 KIT1272 KIT1270 KIT1291 KIT1259 KIT1299 KIT1300 KIT1302 KIT1305 KIT1304";

        // New ICP contract
        if (!itemNumber.startsWith("BUN") && (orgId == 3670 //Adibba Online Shopping LTD
                || orgId == 2918 //Aghayedo Brothers Enterprises
                || orgId == 3677 //Bankytech Communications Ventures
                || orgId == 4222 //Barcutt Investment Limited
                || orgId == 3915 //BIGDATA TECHNOLOGIES LTD
                || orgId == 2121 //C-Soka Limited
                || orgId == 3412 //Galant Communications LTD
                || orgId == 4938 //Global Rolling
                || orgId == 4679 //Ideal Hospitality Solutions Limited
                || orgId == 3143 //Koam Computer Solution
                || orgId == 2630 //Netpoint Communications
                || orgId == 2123 //Novas Systems Limited
                || orgId == 2888 //Pheg Marathon Ventures
                || orgId == 3165 //Pine Height Global Resources ltd
                || orgId == 2643 //Timjat Global Resources Ltd
                || orgId == 3675 //Trend World Wide Ltd
                || orgId == 3413 //Wireless Link Integrated
                || orgId == 2917 //Yfree Solutions Limited
                || orgId == 2653 //Zacfred Systems Solutions Ltd
                || orgId == 5026 //Top on Top Success Global Limited
                || orgId == 5040 // Rafael stores
                || orgId == 4201 // Abetech
                || orgId == 5074 // Globasure Technologies
                || orgId == 2122 // Best
                || orgId == 5117 // VConnect
                || orgId == 5210 // Phone Hut
                || orgId == 5127 // Content Oasis
                || orgId == 5162 // Sainana Resources
                || orgId == 5165 // Hasino Communication
                || orgId == 5215 // Christy George
                || orgId == 5056 // Everyday group
                || orgId == 5242 // Telxtra
                || orgId == 5293 // Cedar network
                || orgId == 5289 // Korenet
                || orgId == 5292 // Skyraph
                || orgId == 5279 // Tobowy
                || orgId == 5450 // Sacen Projects
                || orgId == 5446 // Central Dynamics
                || orgId == 5444 // Pearl Facet
                || orgId == 5445 // Emmrose
                || orgId == 5447 // Einfotech
                || orgId == 5452 // Kevicomms
                || orgId == 5448 // Bel comms
                || orgId == 5472 // Onis Digital
                || orgId == 285 // Sabaoth
                || orgId == 5498 // Biz Ad Solution
                || orgId == 5493 // Pinnacle Wireless
                || orgId == 5499 // GG Communications
                || orgId == 5501 // Tim Sholtz Nigeria Limited
                || orgId == 5491 // Baytown Communications
                || orgId == 5500 // Spectrum Innovation Technologies
                || orgId == 5489 // Wanaga Nig Ltd
                || orgId == 5490 // Primadas Investment Nig. Ltd
                || orgId == 5478 // Bayunfa Ventures Nig.Ltd
                || orgId == 5487 // North East Gold
                || orgId == 5473 // Azman Concepts Limited
                || orgId == 5474 // Infotek Perspective Service Limited
                || orgId == 5449 // Sayrex
                || orgId == 5483 // 3CEE
                || orgId == 5060 // Integral
                || orgId == 5480 // Samace
                || orgId == 5482 // Zioneli
                || orgId == 5504 // Belle vista
                || orgId == 5506 // Dot.com ventures
                || orgId == 5519 // Burj Khalifah Nigeria Limited
                || orgId == 5521 // Datacounter International Ventures
                || orgId == 5534 // Yield Systems
                || orgId == 5529 // Arctuals Concepts Limited
                || orgId == 5530 // Emax Solutions Ventures Ltd
                || orgId == 5539 //  I Will Distribute
                || orgId == 5565 // Broad Prject Nigeria
                || orgId == 5566 // Heirs Multi Concept
                || orgId == 5567 // ACMESYNERGY
                || orgId == 5555 // COMTRANET IT SOLUTIONS LTD
                || orgId == 5577 // JOTECH COMPUTER
                || orgId == 5599 // CHARLOTTE GLOBAL CONCEPTS
                || orgId == 5059 // Callus Miller
                || orgId == 5479 // ImageAddiction
                || orgId == 5631 // Ages Accurate Speed Ltd
                || orgId == 5632 // Kestrel tours
                || orgId == 1361 // ONILU PRIME
                || orgId == 5680 // Trebkann
                || orgId == 5713 // Recharge Hub Services 
                || orgId == 5715 // Suremobile Network Ltd
                || orgId == 5729 // Olive Integrated Services Limited
                || orgId == 5769 // Centri-net Technology Nig. Ltd
                || orgId == 5823 // Merc-Oaton
                || orgId == 5839 // Pristine Panache
                || orgId == 5833 // FF Tech
                || orgId == 5837 // Cheroxy
                || orgId == 5889 // Optimum Greenland
                || orgId == 5925 // Foimm Technology Enterprise
                || orgId == 5921 // Oakmead Realtors
                || orgId == 5927 // First Sopeq Integrated Nig Ltd
                || orgId == 5929 // SDMA Networks
                || orgId == 6013 // Viclyd Nig Ent
                || orgId == 5062 // CHUXEL COMPUTERS
                || orgId == 6019 // BROADLAND GLOBAL RESOURCES LIMITED
                || orgId == 6051 // Reart Risk and Safety Management Limited
                || orgId == 6081 // Mesizanee Concept Limited
                || orgId == 6149 // Bailord Limited
                || orgId == 6155 // Sweetplanet Worldwide Enterprise
                || orgId == 6181 // SHELL EAST STAFF INVESTMENT CO-OP
                || orgId == 6179 // Mo Concepts Co
                || orgId == 6175 // Great Bakis Ventures 
                || orgId == 6177 // Pose Technologies Ltd
                || orgId == 6211 // Shetuham Intl Ltd
                || orgId == 6271 // Avensa Global solutions
                || orgId == 6283 // Derak Multi Ventures Nigeria Limited
                || orgId == 6311 // Donval Platinum Investment
                || orgId == 6299 // Best Baxany Global Services Limited
                || orgId == 6307 // BrightGrab Integrated Services
                || orgId == 6289 // Final Logic Limited
                || orgId == 6339 // Top up Afric
                || orgId == 6369 // Mcedwards Ent.Ltd
                || orgId == 6407 // Lumron Enterprises
                || orgId == 6419 // SWING-HIGH COMMUNICATIONS LIMITED
                || orgId == 6437 // New Sonshyne Systems Limited
                || orgId == 6417 // Simcoe & Dalhousie Nigeria Limited
                || orgId == 6423 // Wonder Global Investment Ltd.Organisation Id 
                || orgId == 6459 // CHARLYN NETWORKS LTD
                || orgId == 6461 // Febby Communication
                || orgId == 6519 // Rakameris Ventures
                || orgId == 6523 // The Koptim Company
                || orgId == 6537 // D.S. Computer-Tech
                || orgId == 6547 // KVC E-Mart
                || orgId == 6541 // Security Services and Consultancy
                || orgId == 6557 // Globe Du Bellissima LTD
                || orgId == 6561 // Kamzi Global Enterprise
                || orgId == 6581 // ING Systems
                || orgId == 6583 // Codex Plus
                || orgId == 6599 // Phones and Computers One- Stop Center Limited
                || orgId == 6617 // KTO ENT
                || orgId == 6615 // GENIE INFOTECH LTD
                || orgId == 6607 // Kollash Business and Technologies Limited
                || orgId == 6623 // Sovereign Gold Service
                || orgId == 6631 // Abhala Ben International Ventures 
                || orgId == 6629 // J.E.A Global Ventures
                || orgId == 6641 // Jenshu Int'l Services Ltd
                || orgId == 6659 // YOMEEZ ELECTROWORLD LTD
                || orgId == 6685 // Soofine Print Limited
                || orgId == 6687 // Lindski
                || orgId == 6689 // Frotkom Nigeria Limited.
                || orgId == 6735 // Avro Group
                || orgId == 6733 // Isladex Communications
                || orgId == 6657 // Rich Technologies Limited
                || orgId == 6745 // Joeche Computer Warehouse
                || orgId == 6653 // Ambuilt Works Limited 
                || orgId == 6719 // Le Charm International Limited
                || orgId == 2918 // Aghayedo Brothers Enterprises
                || orgId == 6565 // EMSI Enterprise
                || orgId == 6761 // hephyOceans Integrated Services Limited
                || orgId == 6665 // Oliver1010 Communications Concept Limited
                || orgId == 6625 // Surfspot Communications Limited
                || orgId == 6633 // Lydslouisa Integrated Company Limited
                || orgId == 6747 // First Regional Solutions Limited. Id
                || orgId == 6779 // The Gizmo world And Solution Limited
                || orgId == 6771 // Easy Talil Enterprise
                || orgId == 6785 // Nofot Gardma Limited
                || orgId == 6823 // Omovo Group
                || orgId == 6821 // Fibre Path
                || orgId == 6787 // Digital Direct
                || orgId == 6819 // Xtraone Family Minstrel Ventures
                || orgId == 6827 // Topcoat Limited
                || orgId == 6861 // General B Y Supermarket
                || orgId == 6805 // Global Quid Ltd
                || orgId == 6923 // LeeBroadVentures
                || orgId == 6917 // Moben-D' Excel Limited
                || orgId == 7011 // SMB Shanono Global Venture Ltd
                || orgId == 6995 // Melendless Ltd
                || orgId == 7057 // Sonite communication
                || orgId == 7089 // Madeeyak
                || orgId == 7073 // Integrated Communication Engineering 
                || orgId == 6949 // caso communication
                || orgId == 7141 // Syrah Communication Limited
                || orgId == 7145 // Morgen Global Limited
                || orgId == 7149 // SDMA Limited Surulere
                || orgId == 7157 // Good Venture Integrated Concepts Limited
                || orgId == 7167 // DAKESH DYNAMIC SERVICES
                || orgId == 7197 // Skenyo Global Services Limited
                || orgId == 7217 // REMGLORY VENTURES NIGERIA LTD
                || orgId == 7209 // U-MAT SUCCESS VENTURES
                || orgId == 7215 // TOKAZ CONCEPT
                || orgId == 7229 // Russo Link
                || orgId == 7207 // Gofulene Integrated Service Ltd
                || orgId == 7227 // Laidera Consulting Ltd
                || orgId == 289 // Olamind Technology
                || orgId == 7273 // ABBAH TECH CONNECTIONS LTD
                || orgId == 7295 // Commonwealth Technologies Ltd
                || orgId == 7293 // Vantage Options Nigeria Ltd
                || orgId == 7297 // B-Jayc Global Resources Limited
                || orgId == 7291 // SDMA Network Badagry
                || orgId == 7347 // Amarcelo Limited
                || orgId == 7159 // MULTI RESOURCES AND INDUSTRIAL SERVICES
                || orgId == 7381 // POYEN NOMOVO NIG LTD
                || orgId == 6769 // G-PAY INSTANT SOLUTIONS
                || orgId == 7073 // Intergrated Communication Engineering
                || orgId == 7431 // Dpro Project Limited
                || orgId == 7417 // Synergy Mobile Business Solutions Limited
                || orgId == 7429 // IDEA KONSULT LIMITED
                || orgId == 7453 // Harry Concern Nigeria Enterprises
                || orgId == 7399 // Dream Nation Alliance (MPCS) Limited
                || orgId == 7329 // Contec Global Infotech Ltd
                || orgId == 7477 // ALTS Service Consult Limited
                || orgId == 7493 // Ski Current Services Limited
                || orgId == 7491 // Emir Label Entertainment Limited
                || orgId == 7559 // Mercury Resources And Investment Company Ltd
                || orgId == 7575 // Micro Link Telecoms Limited
                || orgId == 7237 // Star Gaming Limited
                || orgId == 7727 // Neocortex International Ltd. ID
                || orgId == 7697 // UCAS GLOBAL RESOURCES
                || orgId == 7771 // Zillion Technocom
                || orgId == 7791 // Divine Anchor
                || orgId == 7821 // Grinotech limited
                || orgId == 7845 // Banc Solutions Ltd
                || orgId == 7851 // Next Bt Ltd
                || orgId == 7881 // Uzogy Integrated Technical Service
                || orgId == 7919 // Anne colt intergrated services ltd
                || orgId == 7887 // Fashamu Bookshop and Business Centre
                || orgId == 7921 // TIDAKE GLOBAL LIMITED
                || orgId == 8051 // RULESIXMOBILE LTD
                || orgId == 8139 // Texmaco Ventures Nigeria Limited
                || orgId == 8185 // Payquic World Wide Ltd
                || orgId == 8191 // Kemasho Ventures Ltd
                || orgId == 8215 // I CELL MOBILE
                || orgId == 8279 // Lemor Global Services
                || orgId == 8361 // SmartDrive & Systems Limited
                || orgId == 8107 // Cordiant Technology Services
                || orgId == 8511 // Nartel Ventures
                || orgId == 8615 // Flux Integrated Services Ltd
                || orgId == 8903 // Emma Paradise Multilinks Limited
                || orgId == 9049 // Northsnow Enterprises
                || orgId == 9039 // Hadiel Calidad Limited
                || orgId == 9091 // Enatel Communications
                || orgId == 9629 // Diksolyn Concept Limited
                || orgId == 9625 // Global Lightcrown Ventures	
                || orgId == 9637 // Network Enterprises PH
                || orgId == 5985 // ICP Capricorn Digital
                || orgId == 8283 // Digital Development Hub
                || orgId == 10059 // Glitteringway enterprise
                || orgId == 10009 // Asrad Multi-links Business and logistic limited
                || orgId == 10001 // BlueBreed Enterprise
                || orgId == 10025 // Olans Communication
                || orgId == 7431 // Dpro Project
                || orgId == 9885 // Happymood enterprise
                || orgId == 9975 // Godsaves devices Tech Ltd
                || orgId == 9945 // Aumak-mikash Ltd
                || orgId == 9999 // Aje Global Multiservices
                || orgId == 9963 // FEM9 TOUCH LIMITED
                || orgId == 9973 // Joelink Global Concept communication Ltd
                || orgId == 9967 // Goldenworths West Africa LTD
                || orgId == 5488 // Blessing Computers
                || orgId == 5519 // Burj Khalifah Nigeria Limited
                || orgId == 2652 // Ditrezi Enterprises
                || orgId == 9947 // DANDOLAS NIGERIA LIMITED
                || orgId == 9941 // Manjub Limited
                || orgId == 9939 // Tecboom Digital Ltd
                || orgId == 9933 // Da-Costa F.A and Company
                || orgId == 9929 // Jb Mwonyi Company Limited
                || orgId == 9883 // Oroborkso Ventures Nigeria Limited
                || orgId == 9875 // Chrisoduwa Global Ventures
                || orgId == 9887 // Multitronics Global Enterprise
                || orgId == 9861 // DRUCICARE NIG LTD
                || orgId == 9881 // Beejao Infosolutions Limited
                || orgId == 9877 // Sam Fransco Resources
                || orgId == 9873 // GLORIOUS TELECOMM
                || orgId == 9867 // AMIN AUTO LTD
                || orgId == 9931 // Boxtech Consult
                || orgId == 9649 // NM MARGI NIG LTD
                || orgId == 9897 // Brownnys-Connect Nigeria Limited
                || orgId == 9835 // K-CEE DEVICES LIMITED
                || orgId == 9859 // Budddybod Enterprises
                || orgId == 9863 // GOLDENPEECOMPUTERS TECHNOLOGIES
                || orgId == 9831 // Universal Asset Commercial Boker
                || orgId == 9847 // Fundripples Limited
                || orgId == 9841 // Sharptowers Limited
                || orgId == 9549 // Payconnect Networks Ltd
                || orgId == 9827 // Leverage Options
                || orgId == 9793 // Sunsole Global Investment Ltd
                || orgId == 9801 // Godmic Nigeria LTD
                || orgId == 9825 // Timboy Ventures
                || orgId == 9815 // Geniallinks Services
                || orgId == 9783 // 23RD Century Technologies LTD
                || orgId == 9755 // Abiola Gold Resources
                || orgId == 9753 // Hetoch Technology
                || orgId == 9747 // Mac Onella
                || orgId == 9711 // Finet Communications Ltd
                || orgId == 9723 // Rezio Limited
                || orgId == 10041 // Base 15 Concept Nigeria Limited
                || orgId == 9571 // Cyberdyne Integrated Limited
                || orgId == 10079 // LTC network limited
                || orgId == 10077 // Onigx Investment Limited
                || orgId == 10085 // Global Communication and Digital services
                || orgId == 11175 // Ammid Adex Telecom
                )) {
            // New ICP Contract
            if (!paymentMethod.isEmpty() && !channel.startsWith("26P") && !channel.startsWith("27P") && !itemNumber.startsWith("BUN")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }
            int simKitCount = 0;
            int deviceCount = 0;

            for (int i = 0; i < sale.getSaleLines().size(); i++) {
                com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                String it = otherLine.getInventoryItem().getItemNumber();
                if (simKits.indexOf(it) != -1) {
                    simKitCount += otherLine.getQuantity();
                } else if (deviceKits.indexOf(it) != -1) {
                    deviceCount += otherLine.getQuantity();
                }
            }

            if (simKits.indexOf(itemNumber) != -1) {
                if (simKitCount >= 6 && simKitCount <= 14) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(8d);
                } else if (simKitCount >= 15 && simKitCount <= 50) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                } else if (simKitCount >= 51 && simKitCount <= 100) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(12d);
                } else if (simKitCount >= 101) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(15d);
                }
            }

            if (deviceKits.indexOf(itemNumber) != -1) {
                if (deviceCount >= 6 && deviceCount <= 14) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(8d);
                } else if (deviceCount >= 15 && deviceCount <= 50) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                } else if (deviceCount >= 51 && deviceCount <= 100) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(12d);
                } else if (deviceCount >= 101) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(15d);
                }
            }

            if (isAirtime) {
                double discTier1 = 1d / (1d / 0.05d + 1d);
                double discTier2 = 1d / (1d / 0.055d + 1d);
                double discTier3 = 1d / (1d / 0.06d + 1d);
                double discTier4 = 1d / (1d / 0.07d + 1d);

                double tier1Start = 100000d / (1d - discTier1);
                double tier2Start = 250000d / (1d - discTier2);
                double tier3Start = 500000d / (1d - discTier3);
                double tier4Start = 1000000d / (1d - discTier4);

                long airtimeUnits = saleLine.getQuantity();
                if (airtimeUnits >= tier1Start && airtimeUnits < tier2Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier1 * 100d);
                } else if (airtimeUnits >= tier2Start && airtimeUnits < tier3Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier2 * 100d);
                } else if (airtimeUnits >= tier3Start && airtimeUnits < tier4Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier3 * 100d);
                } else if (airtimeUnits >= tier4Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier4 * 100d);
                }
            }

        }

// New ICP contract for Kaduna ICPs
        if (!itemNumber.startsWith("BUN") && (orgId == 9631 // Crysolyte Networks
                || orgId == 6179 // MO Concepts co. Ltd
                || orgId == 7163 // Bloom Associates
                || orgId == 9649 // NM MARGI NIG LTD
                || orgId == 6929 // Micromanna Limited
                // || orgId == 2652 //Ditrezi
                )) {
            // New ICP Contract for Kaduna ICPs
            if (!paymentMethod.isEmpty() && !channel.startsWith("26P") && !channel.startsWith("27P") && !itemNumber.startsWith("BUN")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }

            if (deviceKits.indexOf(itemNumber) != -1 || simKits.indexOf(itemNumber) != -1) {
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(8d);
            }

            if (isAirtime) {
                double discAllTiers = 1d / (1d / 0.07d + 1d);
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(discAllTiers * 100d);
            }

        }

        if ((orgId == 5074) || (orgId == 28514) // WHT Testing Telecom
                ) {
            // Newest contract requested in HBT-3659
            if (!itemNumber.startsWith("BUN") && !paymentMethod.isEmpty() && !channel.startsWith("26P")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P");
            }

            if (isAirtime) {
                double discTier1 = 1d / (1d / 0.06d + 1d);
                double tier1Start = 5000000d / (1d - discTier1);

                long airtimeUnits = saleLine.getQuantity();
                if (airtimeUnits >= tier1Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier1 * 100d);
                }
            }
            return dd;
        }

        // TPGW Partner Airtime Discount Setup
        if ((orgId == 6409 // Onecard
                || orgId == 7329 // Contec
                || orgId == 9469 // CitiServe
                || orgId == 9483 // HomeKonnekt Limited
                || orgId == 7511 // Cellulant Nigeria Limited
                || orgId == 9603 // Zinternet Nig Ltd
                || orgId == 9549 // Payconnect Networks Ltd 
                || orgId == 10617 // TRUSTVAS LIMITED	
                ) && saleLine.getQuantity() >= 500000 && isAirtime) {

            if (!paymentMethod.isEmpty() && !channel.startsWith("26P") && !channel.startsWith("27P")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }
            double discTier3 = 1d / (1d / 0.08d + 1d);
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(discTier3 * 100d);
        }

        return dd;
    }

    // Super Dealer and ICP config for new model
// This is used by SEP and POS to know which Orgs are Super Dealers and ICPs
// <Org Id> ACC: <Commission account> DISC: <Discount %> TYPE: <SD or ICP> <Name>
// Franchises
// 10077 ACC: 1708003338 DISC: 7% TYPE: FRA Onigx Investment ltd
// 9723 ACC: 1705007543 DISC: 7% TYPE: FRA Rezio Limited
// 5059 ACC: 1503001021 DISC: 7% TYPE: FRA Callus Miller Company Limited
// 8909 ACC: 1708012011 DISC: 7% TYPE: FRA Code2take Ventures
// 2888 ACC: 1709004332 DISC: 7% TYPE: FRA Pheg Marathon Ventures
// Kaduna ICPs treated as Super Dealers
// 6179 ACC: 1507002203 DISC: 8% TYPE: FRA Mo Concepts Co. LTD
// 9631 ACC: 1704006564 DISC: 8% TYPE: FRA Crysolyte Networks
// 7163 ACC: 1601005343 DISC: 8% TYPE: FRA Bloom Associates
// 9649 ACC: 1704009598 DISC: 8% TYPE: FRA NM MARGI NIG LTD
// 6929 ACC: 1602002373 DISC: 8% TYPE: FRA Micromanna Ltd.
// 2652 ACC: 1612003434 DISC: 8% TYPE: FRA Ditrezi Enterprises
// 5479 ACC: 1503002012 DISC: 8% TYPE: FRA ImageAddiction
// 6555 ACC: 1509005251 DISC: 8% TYPE: FRA Veemobile services
// 6643 ACC: 1510002993 DISC: 8% TYPE: FRA Dahrah Global Limited
// 6993 ACC: 1512006591 DISC: 8% TYPE: FRA Kima-Maxi Nigeria Limited
// 6997 ACC: 1512006592 DISC: 8% TYPE: FRA Value Plus Service & Industries L
// 7009 ACC: 1512006586 DISC: 8% TYPE: FRA Detoyibo Enterprises
// 7021 ACC: 1512006589 DISC: 8% TYPE: FRA Toluque Nigeria Limited
// 7045 ACC: 1512007980 DISC: 8% TYPE: FRA Payit Consulting Limited
// 7417 ACC: 1602003768 DISC: 8% TYPE: FRA Synergy Mobile Business Solutions Limited
// 7431 ACC: 1602004047 DISC: 8% TYPE: FRA Dpro Project Limited
// 7843 ACC: 1604007384 DISC: 8% TYPE: FRA SAANET GLOBAL SERVICES LIMITED
// 8283 ACC: 1705003619 DISC: 8% TYPE: FRA Digital Development Hub
// 9539 ACC: 1703001193 DISC: 8% TYPE: FRA Intergrity Communications
// 9783 ACC: 1705015937 DISC: 8% TYPE: FRA 23RD Century Technologies LTD
// 9815 ACC: 1706008776 DISC: 8% TYPE: FRA GENIALLINKS SERVICES
// 9825 ACC: 1706008774 DISC: 8% TYPE: FRA TIMBOY VENTURES
// 9831 ACC: 1706012063 DISC: 8% TYPE: FRA Universal Asset Commercial Boker
// 9941 ACC: 1707002065 DISC: 8% TYPE: FRA Manjub Limited
// 10025 ACC: 1707008135 DISC: 8% TYPE: FRA Olans Communications
// 10075 ACC: 1708003269 DISC: 8% TYPE: FRA Ultimate Aslan Limited
// 10019 ACC: 1708002311 DISC: 8% TYPE: FRA White house Computers Ltd
// 10013 ACC: 1708002310 DISC: 8% TYPE: FRA Speedy IDeas Synergy Nig Ltd
// 10181 ACC: 1708012163 DISC: 8% TYPE: FRA ALAJIM Limited
// 10123 ACC: 1708007593 DISC: 8% TYPE: FRA MJ and partners power Electric
// 10325 ACC: 1709003981 DISC: 8% TYPE: FRA Beyond Integrated Services
// 10467 ACC: 1709009459 DISC: 8% TYPE: ICP ABSON Bussiness World LTD
// 7057 ACC: 1512008310 DISC: 11% TYPE: SD Sonite Communications Limited
// 6633 ACC: 1511001587 DISC: 10% TYPE: SD Lyds Louisa Intergrated Company Limited
// 8623 ACC: 1607009915 DISC: 10% TYPE: SD ZEPH ASSOCIATES NIGERIA LIMITED
// 8641 ACC: 1608000342 DISC: 10% TYPE: SD Idems First Network Limited
// 8639 ACC: 1607011274 DISC: 10% TYPE: SD Gabros Divine Technologies Limited
// 8649 ACC: 1608000265 DISC: 10% TYPE: SD CANT STOP NIGERIA LIMITED
// 8649 ACC: 1710003339 DISC: 10% TYPE: SD  Golden Rule Communications Ltd
// 8649 ACC: 1710003342 DISC: 10% TYPE: SD  Golden Rule Communications Ltd
// 9109 ACC: 1610001302 DISC: 10% TYPE: SD Concise Services Limited
// 2643 ACC: 1312000182 DISC: 10% TYPE: SD Timjat Global Resources Ltd
// 9615 ACC: 1704001876 DISC: 10% TYPE: SD Digital Development Hub Consultancy Services Ltd
// 9647 ACC: 1704009231 DISC: 10% TYPE: SD YFree SD
// 10839 ACC: 1712002385 DISC: 10% TYPE: SD  Lustre Communications ltd
// 3 ACC: 1711010802 DISC: 8% TYPE: ICP Helpline Telecoms Nig. LTD
// 10303 ACC: 1709008032 DISC: 8% TYPE: ICP PC Corner Services Limited
// 10221 ACC: 1709001037 DISC: 8% TYPE: ICP Profast Integrated Services Limited
// 10215 ACC: 1709000966 DISC: 8% TYPE: ICP Rahmatob Computer Concept
// 10219 ACC: 1709001017 DISC: 8% TYPE: ICP Kastech Technologies
// 10179 ACC: 1708011903 DISC: 8% TYPE: ICP Popsicles Enterprises 
// 10109 ACC: 1708006078 DISC: 8% TYPE: ICP Ahijo Ventures limited
// 10163 ACC: 1708011094 DISC: 8% TYPE: ICP Option 360 Communications
// 10159 ACC: 1708010209 DISC: 8% TYPE: ICP Poatech Enterprises Nigeria Ltd
// 10151 ACC: 1708009500 DISC: 8% TYPE: ICP TICEE CONNECT
// 10125 ACC: 1708007797 DISC: 8% TYPE: ICP Hona Ara Nigeria Ltd
// 10115 ACC: 1708006791 DISC: 8% TYPE: ICP Dolamif Nigeria Limited
// 10085 ACC: 1708004284 DISC: 8% TYPE: ICP Global Communication and Digital services
// 10077 ACC: 1708003338 DISC: 8% TYPE: ICP Onigx Investment Limited
// 10079 ACC: 1708003426 DISC: 8% TYPE: ICP LTC network limited
// 10059 ACC: 1708001482 DISC: 8% TYPE: ICP Glitteringway enterprise
// 10009 ACC: 1707007946 DISC: 8% TYPE: ICP Asrad Multi-links Business and logistic limited
// 10001 ACC: 1707006592 DISC: 8% TYPE: ICP BlueBreed Enterprise
// 10025 ACC: 1707008135 DISC: 8% TYPE: ICP Olans Communication
// 10041 ACC: 1707010667 DISC: 8% TYPE: ICP Base 15 Concept Nigeria Limited
// 10171 ACC: 1708011045 DISC: 8% TYPE: ICP Pesuzok Nig Ltd
// 10429 ACC: 1709009699 DISC: 8% TYPE: ICP PC GABNET TECHNOLOGY
// 10427 ACC: 1709009697 DISC: 8% TYPE: ICP LATEETOY NIGERIA LIMITED
// 7921 ACC: 1605003822 DISC: 8% TYPE: ICP Tidake global limited
// 9609 ACC: 1710001286 DISC: 8% TYPE: ICP Lexton Energy
// 9731 ACC: 1708010168 DISC: 8% TYPE: ICP Belledave Nigeria Limited
// 9571 ACC: 1707010662 DISC: 8% TYPE: ICP Cyberdyne Integrated Limited
// 7431 ACC: 1602004047 DISC: 8% TYPE: ICP Dpro Project
// 9885 ACC: 1707001965 DISC: 8% TYPE: ICP Happymood enterprise
// 9975 ACC: 1707005327 DISC: 8% TYPE: ICP Godsaves devices Tech Ltd
// 9945 ACC: 1707008942 DISC: 8% TYPE: ICP Aumak-mikash Ltd
// 9999 ACC: 1707007126 DISC: 8% TYPE: ICP Aje Global Multiservices
// 9963 ACC: 1707004296 DISC: 8% TYPE: ICP FEM9 TOUCH LIMITED
// 9973 ACC: 1707005483 DISC: 8% TYPE: ICP Joelink Global Concept communication Ltd
// 9967 ACC: 1707005338 DISC: 8% TYPE: ICP Goldenworths West Africa LTD
// 5488 ACC: 1501003415 DISC: 8% TYPE: ICP Blessing Computers
// 5519 ACC: 1502000278 DISC: 8% TYPE: ICP Burj Khalifah Nigeria Limited
// 2652 ACC: 1612003434 DISC: 8% TYPE: ICP Ditrezi Enterprises
// 9947 ACC: 1707003031 DISC: 8% TYPE: ICP DANDOLAS NIGERIA LIMITED
// 9941 ACC: 1707002065 DISC: 8% TYPE: ICP Manjub Limited
// 9939 ACC: 1707002036 DISC: 8% TYPE: ICP Tecboom Digital Ltd
// 9933 ACC: 1707001131 DISC: 8% TYPE: ICP Da-Costa F.A and Company
// 9929 ACC: 1707000869 DISC: 8% TYPE: ICP Jb Mwonyi Company Limited
// 9883 ACC: 1706014165 DISC: 8% TYPE: ICP Oroborkso Ventures Nigeria Limited
// 9875 ACC: 1706014128 DISC: 8% TYPE: ICP Chrisoduwa Global Ventures
// 9887 ACC: 1706014329 DISC: 8% TYPE: ICP Multitronics Global Enterprise
// 9861 ACC: 1706014118 DISC: 8% TYPE: ICP DRUCICARE NIG LTD
// 9881 ACC: 1706014150 DISC: 8% TYPE: ICP Beejao Infosolutions Limited
// 9877 ACC: 1706014141 DISC: 8% TYPE: ICP Sam Fransco Resources
// 9873 ACC: 1706014121 DISC: 8% TYPE: ICP GLORIOUS TELECOMM
// 9867 ACC: 1707000906 DISC: 8% TYPE: ICP AMIN AUTO LTD
// 9931 ACC: 1707000923 DISC: 8% TYPE: ICP Boxtech Consult
// 9649 ACC: 1704009598 DISC: 8% TYPE: ICP NM MARGI NIG LTD
// 9897 ACC: 1707000781 DISC: 8% TYPE: ICP Brownnys-Connect Nigeria Limited
// 9835 ACC: 1706010115 DISC: 8% TYPE: ICP K-CEE DEVICES LIMITED
// 9859 ACC: 1706014143 DISC: 8% TYPE: ICP Budddybod Enterprises
// 9863 ACC: 1706013579 DISC: 8% TYPE: ICP GOLDENPEECOMPUTERS TECHNOLOGIES
// 9831 ACC: 1706012063 DISC: 8% TYPE: ICP Universal Asset Commercial Boker
// 9847 ACC: 1706011240 DISC: 8% TYPE: ICP Fundripples Limited
// 9841 ACC: 1706011245 DISC: 8% TYPE: ICP Sharptowers Limited
// 9549 ACC: 1703001162 DISC: 8% TYPE: ICP Payconnect Networks Ltd
// 9827 ACC: 1706009025 DISC: 8% TYPE: ICP Leverage Options
// 9793 ACC: 1706008830 DISC: 8% TYPE: ICP Sunsole Global Investment Ltd
// 9801 ACC: 1706008832 DISC: 8% TYPE: ICP Godmic Nigeria LTD
// 9825 ACC: 1706008774 DISC: 8% TYPE: ICP Timboy Ventures
// 9815 ACC: 1706008776 DISC: 8% TYPE: ICP Geniallinks Services
// 9783 ACC: 1705015937 DISC: 8% TYPE: ICP 23RD Century Technologies LTD
// 9755 ACC: 1705010786 DISC: 8% TYPE: ICP Abiola Gold Resources
// 9753 ACC: 1705010787 DISC: 8% TYPE: ICP Hetoch Technology
// 9747 ACC: 1705009480 DISC: 8% TYPE: ICP Mac Onella
// 9711 ACC: 1705006481 DISC: 8% TYPE: ICP Finet Communications Ltd
// 9723 ACC: 1705007543 DISC: 8% TYPE: ICP Rezio Limited
// 6179 ACC: 1507002203 DISC: 8% TYPE: ICP MO Concepts co. Ltd
// 8283 ACC: 1705003619 DISC: 8% TYPE: ICP Digital Development Hub
// 5985 ACC: 1506002422 DISC: 8% TYPE: ICP Capricorn Digital
// 9657 ACC: 1704011368 DISC: 8% TYPE: ICP Spacesignal Ltd
// 7219 ACC: 1601008476 DISC: 8% TYPE: ICP TYJPEE Global Service
// 5598 ACC: 1502005940 DISC: 8% TYPE: ICP P N B Bytes Resources
// 9637 ACC: 1704007239 DISC: 8% TYPE: ICP Network Enterprises PH
// 9625 ACC: 1704006509 DISC: 8% TYPE: ICP Global Lightcrown Ventures
// 9631 ACC: 1704006564 DISC: 8% TYPE: ICP CRYSOLYTE NETWORKS
// 3143 ACC: 1402000481 DISC: 8% TYPE: ICP Koams Solutions
// 3675 ACC: 1406000144 DISC: 8% TYPE: ICP Trend Worlwide Ltd
// 3677 ACC: 1406000147 DISC: 8% TYPE: ICP Bankytech Comm Ventures
// 5500 ACC: 1501003476 DISC: 8% TYPE: ICP Spectrum Innovation Technologies
// 3412 ACC: 1406000145 DISC: 8% TYPE: ICP Galant Communications Ltd
// 2918 ACC: 1402000215 DISC: 8% TYPE: ICP Aghayedo Brother Enterprises
// 4222 ACC: 1408000539 DISC: 8% TYPE: ICP Barcutt Investment Ltd
// 5493 ACC: 1501003451 DISC: 8% TYPE: ICP Pinacle Wireless Ltd
// 5506 ACC: 1501004187 DISC: 8% TYPE: ICP Dot.Com
// 5713 ACC: 1504002740 DISC: 8% TYPE: ICP Recharge Hub
// 5715 ACC: 1504002778 DISC: 8% TYPE: ICP Suremobile Networks
// 4938 ACC: 1408001017 DISC: 8% TYPE: ICP Global Rolling Technologies
// 5833 ACC: 1505001221 DISC: 8% TYPE: ICP F.F.Tech Communication
// 6581 ACC: 1509006920 DISC: 8% TYPE: ICP ING Systems
// 6615 ACC: 1510001332 DISC: 8% TYPE: ICP Genie Infotech
// 6437 ACC: 1508005507 DISC: 8% TYPE: ICP New Sonshyne
// 7845 ACC: 1604007573 DISC: 8% TYPE: ICP Banc Solutions
// 8051 ACC: 1605005403 DISC: 8% TYPE: ICP Rulesixmobile
// 8185 ACC: 1606000715 DISC: 8% TYPE: ICP PayQuick
// 8191 ACC: 1606001008 DISC: 8% TYPE: ICP Kemasho
// 8107 ACC: 1606008018 DISC: 8% TYPE: ICP Cordiant Technology
// 2917 ACC: 1402000211 DISC: 8% TYPE: ICP Yfree Solutions
// 2888 ACC: 1402000629 DISC: 8% TYPE: ICP Pheg Marathon
// 6417 ACC: 1509000345 DISC: 8% TYPE: ICP Simcoe & Dalhousie
// 5921 ACC: 1505005109 DISC: 8% TYPE: ICP Oakmead Realtors
// 6307 ACC: 1508001986 DISC: 8% TYPE: ICP Brightgrab
// 5567 ACC: 1502003116 DISC: 8% TYPE: ICP Acmesynergy
// 5565 ACC: 1502002826 DISC: 8% TYPE: ICP Broad Projects
// 6523 ACC: 1509003263 DISC: 8% TYPE: ICP The Koptim Company
// 8361 ACC: 1606007627 DISC: 8% TYPE: ICP SmartDrive & Systems Limited
// 5680 ACC: 1504000784 DISC: 8% TYPE: ICP Trebkann Nig. Ltd
// 6311 ACC: 1508001134 DISC: 8% TYPE: ICP Donval Platinum
// 5074 ACC: 1410002018 DISC: 8% TYPE: ICP Globasure Technologies
// 6923 ACC: 1512002524 DISC: 8% TYPE: ICP LeeBroad Ventures
// 8511 ACC: 1607008247 DISC: 8% TYPE: ICP Nartel Ventures
// 5504 ACC: 1501004137 DISC: 8% TYPE: ICP Belle-Vista
// 5489 ACC: 1501003418 DISC: 8% TYPE: ICP Wanaga Nig Ltd
// 3413 ACC: 1406000156 DISC: 8% TYPE: ICP Wireles Link Integrated
// 4679 ACC: 1408000945 DISC: 8% TYPE: ICP Ideal Hospitality
// 6271 ACC: 1507005163 DISC: 8% TYPE: ICP Avensa Global 
// 6149 ACC: 1507001087 DISC: 8% TYPE: ICP Bailord Ltd
// 5837 ACC: 1505001228 DISC: 8% TYPE: ICP Cheroxy Nig Ltd
// 6013 ACC: 1506002217 DISC: 8% TYPE: ICP Viclyd Enterprise
// 5925 ACC: 1505005013 DISC: 8% TYPE: ICP Foiim Technology
// 6155 ACC: 1507001275 DISC: 8% TYPE: ICP Sweet Planet
// 6407 ACC: 1508004268 DISC: 8% TYPE: ICP Lumron Enterprise
// 6299 ACC: 1508000989 DISC: 8% TYPE: ICP Best Baxany
// 6827 ACC: 1511003671 DISC: 8% TYPE: ICP Top Coat Limited
// 6623 ACC: 1510001977 DISC: 8% TYPE: ICP Sovereign Gold
// 6607 ACC: 1510000847 DISC: 8% TYPE: ICP Kollash Business and Technologies
// 6547 ACC: 1509005008 DISC: 8% TYPE: ICP KVC E-Mart
// 6733 ACC: 1510006947 DISC: 8% TYPE: ICP Isladex Communications
// 6735 ACC: 1510007028 DISC: 8% TYPE: ICP Avro Group
// 6823 ACC: 1511003342 DISC: 8% TYPE: ICP Omovo Tv
// 6821 ACC: 1511003293 DISC: 8% TYPE: ICP Fibrepath Limited 
// 7149 ACC: 1506000291 DISC: 8% TYPE: ICP SDMA Networks Limited  
// 7145 ACC: 1601004566 DISC: 8% TYPE: ICP Morgenall Global Limited
// 7347 ACC: 1602001116 DISC: 8% TYPE: ICP Amarcelo Nig Ltd
// 7477 ACC: 1602006358 DISC: 8% TYPE: ICP ALTS Consult Services Limited
// 7851 ACC: 1604007898 DISC: 8% TYPE: ICP Next BT Nig Ltd
// 7791 ACC: 1604001542 DISC: 8% TYPE: ICP Divine Anchor Nig Ltd
// 7821 ACC: 1604005277 DISC: 8% TYPE: ICP Grinotech Nig Ltd
// 8215 ACC: 1606002044 DISC: 8% TYPE: ICP I-Cell Mobile
// 5490 ACC: 1501003431 DISC: 8% TYPE: ICP Primadas Investment Nig Ltd
// 5729 ACC: 1504003622 DISC: 8% TYPE: ICP OLIVE INTEGRATED SERVICES
// 5482 ACC: 1501003738 DISC: 8% TYPE: ICP ZIONELI LIMITED
// 5289 ACC: 1412000818 DISC: 8% TYPE: ICP KORENET TECHNOLOGIES LTD
// 5529 ACC: 1502000810 DISC: 8% TYPE: ICP Arctuals Concepts Limited
// 5473 ACC: 1501003735 DISC: 8% TYPE: ICP AZMAN CONCEPTS LIMITED
// 5487 ACC: 1501003736 DISC: 8% TYPE: ICP NORTH EAST GOLD
// 5474 ACC: 1501003737 DISC: 8% TYPE: ICP InfoTek Perspective Services Limited
// 5823 ACC: 1505000924 DISC: 8% TYPE: ICP MERC-OATON TECHNOLOGIES LIMITED
// 5555 ACC: 1502003979 DISC: 8% TYPE: ICP COMTRANET IT SOLUTIONS LTD
// 6747 ACC: 1510007607 DISC: 8% TYPE: ICP First Regional Solutions Limited
// 7295 ACC: 1601010317 DISC: 8% TYPE: ICP Commonwealth Technologies Ltd
// 5839 ACC: 1505001367 DISC: 8% TYPE: ICP PRISTINE PANACHE LIMITED
// 6555 ACC: 1509005251 DISC: 8% TYPE: ICP Veemobile services
// 5479 ACC: 1503002012 DISC: 8% TYPE: ICP ImageAddiction
// 6805 ACC: 1511002557 DISC: 8% TYPE: ICP Global Quid Ltd
// 6461 ACC: 1509001625 DISC: 8% TYPE: ICP Febby communication Nigeria limited
// 6423 ACC: 1508005012 DISC: 8% TYPE: ICP Wonder Worth Global Investment Ltd
// 5162 ACC: 1411000821 DISC: 8% TYPE: ICP Sainana Resources
// 5292 ACC: 1412000819 DISC: 8% TYPE: ICP Skyraph Intergrated Servicesu Limited
// 6541 ACC: 1509004144 DISC: 8% TYPE: ICP Mohim Security Services and Consultancy Ltd
// 7727 ACC: 1603010674 DISC: 8% TYPE: ICP NEOCORTEX INTERNATIONAL LTD
// 5577 ACC: 1502004789 DISC: 8% TYPE: ICP Jotech Computer Co. Ltd.
// 6369 ACC: 1508002662 DISC: 8% TYPE: ICP MCEDWARDS ENT.LTD
// 6599 ACC: 1510000393 DISC: 8% TYPE: ICP Phones and Computers One-Stop Center Limited
// 5472 ACC: 1501003213 DISC: 8% TYPE: ICP Onis Digital Ventures
// 7197 ACC: 1601007501 DISC: 8% TYPE: ICP Skenyo Global Services Limited
// 7771 ACC: 1604000388 DISC: 8% TYPE: ICP Zillion Technocom
// 6685 ACC: 1510004241 DISC: 8% TYPE: ICP Soofine Print Limited
// 6519 ACC: 1509003097 DISC: 8% TYPE: ICP  Rakameris Ventures  
// 5279 ACC: 1412000433 DISC: 8% TYPE: ICP TOBOWY NIGERIA LIMITED
// 5215 ACC: 1411001937 DISC: 8% TYPE: ICP CHRISTY-GEORGE
// 5056 ACC: 1411001346 DISC: 8% TYPE: ICP Everyday Group
// 5444 ACC: 1501002271 DISC: 8% TYPE: ICP Pearl Inter Facet Resources
// 5446 ACC: 1501002273 DISC: 8% TYPE: ICP Central Dynamics Ltd
// 5483 ACC: 1501003505 DISC: 8% TYPE: ICP 3CEE TECHNOLOGIES
// 5445 ACC: 1501002272 DISC: 8% TYPE: ICP Emmrose Technologies Ltd
// 5450 ACC: 1501002270 DISC: 8% TYPE: ICP SACEN PROJECTS LIMITED
// 5060 ACC: 1501003504 DISC: 8% TYPE: ICP Integral Com (NIG) Ltd
// 5449 ACC: 1501003506 DISC: 8% TYPE: ICP Sayrex phone communication
// 5059 ACC: 1503001021 DISC: 8% TYPE: ICP Callus Miller Company Limited
// 5062 ACC: 1506003787 DISC: 8% TYPE: ICP Chuxel Computers
// 6181 ACC: 1507002247 DISC: 8% TYPE: ICP SHELL EAST STAFF INVESTMENT CO-OP SOCIETY LMT
// 6419 ACC: 1508004778 DISC: 8% TYPE: ICP SWING-HIGH COMMUNICATIONS LIMITED
// 6459 ACC: 1509001373 DISC: 8% TYPE: ICP CHARLYN NETWORKS LTD
// 6659 ACC: 1510003308 DISC: 8% TYPE: ICP YOMEEZ ELECTROWORLD LTD
// 7167 ACC: 1601005651 DISC: 8% TYPE: ICP DAKECH DYNAMIC SERVICES NIG LTD
// 7159 ACC: 1601005251 DISC: 8% TYPE: ICP MULTI RESOURCES AND INDUSTRIAL SERVICES NIG LTD
// 6769 ACC: 1602003001 DISC: 8% TYPE: ICP G-PAY INSTANT SOLUTIONS
// 7453 ACC: 1602005395 DISC: 8% TYPE: ICP Harry Concern Nigeria Enterprises
// 8819 ACC: 1608006679 DISC: 8% TYPE: ICP PINAAR UNIVERSAL LTD
// 6787 ACC: 1511003555 DISC: 8% TYPE: ICP Digital Direct
// 6819 ACC: 1511003971 DISC: 8% TYPE: ICP Xtraone Family Minstrel Ventures
// 6917 ACC: 1512002151 DISC: 8% TYPE: ICP Moben-D Excel Limited
// 6785 ACC: 1511003588 DISC: 8% TYPE: ICP Nofot Gardma Limited
// 5889 ACC: 1505003402 DISC: 8% TYPE: ICP Optimum Greeland Limited
// 6537 ACC: 1509003925 DISC: 8% TYPE: ICP D.S. Computer - Tech
// 6653 ACC: 1511001589 DISC: 8% TYPE: ICP Ambuilt works Ltd
// 6625 ACC: 1511001586 DISC: 8% TYPE: ICP Surfspot Communications Limited
// 6081 ACC: 1506005174 DISC: 8% TYPE: ICP Mesizanee Concept Limited.
// 5929 ACC: 1506000291 DISC: 8% TYPE: ICP SDMA Networks Limited
// 8903 ACC: 1609001070 DISC: 8% TYPE: ICP Emma Paradise Multilinks Limited
// 7089 ACC: 1601001377 DISC: 8% TYPE: ICP Madeeyak Nigeria Limited
// 8949 ACC: 1609002135 DISC: 8% TYPE: ICP Trevari
// 6771 ACC: 1511001721 DISC: 8% TYPE: ICP Easy Talil Enterprise
// 8983 ACC: 1609005821 DISC: 8% TYPE: ICP WATERGATE TELECOMMUNICATION
// 6657 ACC: 1511000486 DISC: 8% TYPE: ICP Rich Technologies Limited	
// 6745 ACC: 1511001590 DISC: 8% TYPE: ICP Joeche Computer Warehouse Limited	
// 6175 ACC: 1507001839 DISC: 8% TYPE: ICP Great Bakis Ventures Limited
// 9151 ACC: 1610006349 DISC: 8% TYPE: ICP Red and White Global Concept Ltd
// 8969 ACC: 1610010473 DISC: 8% TYPE: ICP Edge Baseline Solution
// 9173 ACC: 1610010961 DISC: 8% TYPE: ICP Riggsco Percific concept limited
// 9197 ACC: 1611000665 DISC: 8% TYPE: ICP Enbelo Company Limited
// 9221 ACC: 1611005581 DISC: 8% TYPE: ICP Ritech Network Enterprises
// 2630 ACC: 1310000420 DISC: 8% TYPE: ICP NETPOINT COMMUNICATIONS
// 2653 ACC: 1401000533 DISC: 8% TYPE: ICP ZACFRED SYSTEMS SOLUTIONS LTD
// 5539 ACC: 1502001765 DISC: 8% TYPE: ICP GLOBAL HANDSET LTD ( I WILL DISTRIBUTE ENT)
// 9039 ACC: 1609008911 DISC: 8% TYPE: ICP Hadiel Calidad Limited
// 7429 ACC: 1602004034 DISC: 8% TYPE: ICP Idea Konsult
// 6779 ACC: 1511001375 DISC: 8% TYPE: ICP The Gizmo world And Solution Limited
// 6179 ACC: 1507002203 DISC: 8% TYPE: ICP Mo Concepts Co. LTD
// 6861 ACC: 1507004510 DISC: 8% TYPE: ICP General B.Y. Supermarket
// 5631 ACC: 1503003181 DISC: 8% TYPE: ICP Ages Accurate Speed LTD
// 5534 ACC: 1502000967 DISC: 8% TYPE: ICP Yield Systems Limited
// 6779 ACC: 1511001375 DISC: 8% TYPE: ICP The Gizmo world And Solution Limited
// 7493 ACC: 1611006408 DISC: 8% TYPE: ICP Ski Current Service Limited
// 6289 ACC: 1508000009 DISC: 8% TYPE: ICP Final Logic
// 7491 ACC: 1602009235 DISC: 8% TYPE: ICP Emir Label
// 1361 ACC: 1503004497 DISC: 8% TYPE: ICP Onilu prime
// 7217 ACC: 1601008727 DISC: 8% TYPE: ICP REMGLORY VENTURES NIGERIA
// 8139 ACC: 1605007749 DISC: 8% TYPE: ICP Texmaco Ventures Nigeria Limited
// 7209 ACC: 1602004152 DISC: 8% TYPE: ICP U-MAT SUCCESS VENTURES
// 7697 ACC: 1603009927 DISC: 8% TYPE: ICP TUCAS GLOBAL RESOURCES
// 7215 ACC: 1601008729 DISC: 8% TYPE: ICP TOKAZ CONCEPT
// 9091 ACC: 1609010883 DISC: 8% TYPE: ICP Enatel Communications
// 7381 ACC: 1602002402 DISC: 8% TYPE: ICP POYEN NOMOVO NIG LTD
// 9255 ACC: 1611006221 DISC: 8% TYPE: ICP Brown Street Limited
// 9271 ACC: 1611008529 DISC: 8% TYPE: ICP Raz Resources
// 6565 ACC: 1509006328 DISC: 8% TYPE: ICP EMSI Enterprises Limited
// 5127 ACC: 1411000823 DISC: 8% TYPE: ICP Content Oasis limited
// 9293 ACC: 1612001954 DISC: 8% TYPE: ICP DA MORIS NIGERIA ENTERPRISES
// 5479 ACC: 1612008149 DISC: 8% TYPE: ICP ImageAddiction
// 9363 ACC: 1701000464 DISC: 8% TYPE: ICP Chuvexton Technovation Enterprises
// 9349 ACC: 1612007905 DISC: 8% TYPE: ICP Bama Global Concept Limited
// 9397 ACC: 1701003701 DISC: 8% TYPE: ICP Gold Vicky Communications Ltd
// 9405 ACC: 1701004853 DISC: 8% TYPE: ICP Double A Wireless
// 9413 ACC: 1701006201 DISC: 8% TYPE: ICP Darlin Ronkus Phones Ventures
// 9415 ACC: 1701006832 DISC: 8% TYPE: ICP Emmanuel Nelson Mobile Vetures
// 9451 ACC: 1701011176 DISC: 8% TYPE: ICP Temperature Limited
// 9473 ACC: 1702000782 DISC: 8% TYPE: ICP Leads Hotel Integrated Services
// 8909 ACC: 1609001970 DISC: 8% TYPE: ICP Code2take Ventures
// 9489 ACC: 1702001605 DISC: 8% TYPE: ICP Enhanced Payment Solutions
// 9463 ACC: 1702002043 DISC: 8% TYPE: ICP UNIQUE EVERMORE GLOBAL VENTURES
// 9511 ACC: 1702003409 DISC: 8% TYPE: ICP NPNG GLOBAL VENTURES
// 9521 ACC: 1702004999 DISC: 8% TYPE: ICP Demasworld Resource limited
// 9483 ACC: 1702001841 DISC: 8% TYPE: ICP Homekonnekt Limited
// 9543 ACC: 1702006254 DISC: 8% TYPE: ICP Padillax Technology Limited
// 9547 ACC: 1703000627 DISC: 8% TYPE: ICP Ficoven Investments Limited
// 9559 ACC: 1703002062 DISC: 8% TYPE: ICP Blu Edition International Limited
// 9553 ACC: 1703001554 DISC: 8% TYPE: ICP Joshfat Global Investment Ltd
// 9539 ACC: 1703001193 DISC: 8% TYPE: ICP Integrity Communications
// 9575 ACC: 1703005182 DISC: 8% TYPE: ICP Fabisek Investment Limited
// 9577 ACC: 1703005043 DISC: 8% TYPE: ICP Ironwill Business Services
// 9589 ACC: 1703006673 DISC: 8% TYPE: ICP E-Gate Technology Consults Ltd. ID
// 9601 ACC: 1703008371 DISC: 8% TYPE: ICP Chinwaks Global Nig Ltd
// 9595 ACC: 1703007903 DISC: 8% TYPE: ICP Liricom Nig Ltd
// 9605 ACC: 1703008748 DISC: 8% TYPE: ICP Peritus technologies limited
// 9599 ACC: 1703008749 DISC: 8% TYPE: ICP Golden eagles technology
// 9477 ACC: 1702001112 DISC: 8% TYPE: ICP Tribal Marks Media Productions Limited
// 9613 ACC: 1704001833 DISC: 8% TYPE: ICP Jegkobah Integrate Service Ltd
// 9629 ACC: 1704006245 DISC: 8% TYPE: ICP Diksolyn Concept Limited
// 9649 ACC: 1704009598 DISC: 8% TYPE: ICP NM Margi Nig Ltd
// 9695 ACC: 1705005014 DISC: 8% TYPE: ICP Easycomm global connection
// 9693 ACC: 1705005016 DISC: 8% TYPE: ICP Matuluko and son
// 8283 ACC: 1705003619 DISC: 8% TYPE: ICP Digital Development Hub
// 9797 ACC: 1706003164 DISC: 8% TYPE: ICP Cida-woods Limited
// 8909 ACC: 1609001970 DISC: 8% TYPE: ICP Code2take Ventures
// 10571 ACC: 1710006894 DISC: 8% TYPE: ICP  Pristine Stitches Enterprise 
// 10251 ACC: 1709001469 DISC: 8% TYPE: ICP Otomab and Sons Nigeria Limited
// 10323 ACC: 1709003979 DISC: 8% TYPE: ICP OPKISE GLOBAL CONCEPTS
// 10553 ACC: 1710004088 DISC: 8% TYPE: ICP Olive-Frank Global Limited
// 10577 ACC: 1710006214 DISC: 8% TYPE: ICP  GreenTag Investment Limited 
// 10639 ACC: 1710010736 DISC: 8% TYPE: ICP  Grandieu Limited
// 10657 ACC: 1711000649 DISC: 8% TYPE: ICP  ELVESTA VENTURES
// 10545 ACC: 1710003975 DISC: 8% TYPE: ICP  Adubi-Gold Nig Enterprises
// 10649 ACC: 1710012391 DISC: 8% TYPE: ICP UJE AND GROM CONCEPTS
// 10701 ACC: 1711003243 DISC: 8% TYPE: ICP Caiviks Technologies
// 10037 ACC: 1707009337 DISC: 8% TYPE: ICP Bizhub solution
// 10753 ACC: 1711002150 DISC: 8% TYPE: ICP HORLMIL CONTINENTAL SERVICES
// 10755 ACC: 1711007991 DISC: 8% TYPE: ICP EIUCOM RECHARGE HUB CONCEPTS
// 10251 ACC: 1709001469 DISC: 8% TYPE: ICP  Otomab and Sons Nigeria Limited
// 10905 ACC: 1712007920 DISC: 8% TYPE: ICP JATRAB VENTURES
// 10991 ACC: 1801003694 DISC: 8% TYPE: ICP GodfreyBrown LTD
// 11001 ACC: 1801004932 DISC: 8% TYPE: ICP Okay Fones Limited
// 10915 ACC: 1712009452 DISC: 8% TYPE: ICP Embehie Enterprises
// 10923 ACC: 1712011398 DISC: 8% TYPE: ICP Worthy Realtors
// 10925 ACC: 1712011403 DISC: 8% TYPE: ICP Holad best International
// 10955 ACC: 1701003491 DISC: 8% TYPE: ICP Natural colour press
// 10913 ACC: 1712009241 DISC: 8% TYPE: ICP Sambrix International Limited
// 10979 ACC: 1801002030 DISC: 8% TYPE: ICP Is-Hami Construction Limited
// 11075 ACC: 1801010261 DISC: 8% TYPE: ICP Adedayo Adedeji
// 11039 ACC: 1801007930 DISC: 8% TYPE: ICP Shounix Global Venture
// 11077 ACC: 1801010566 DISC: 8% TYPE: ICP CECON24 Systems Limited
// 11091 ACC: 1801013136 DISC: 8% TYPE: ICP Correspondence Limited ID
// 11055 ACC: 1802001867 DISC: 8% TYPE: ICP Evabest communications
// 11127 ACC: 1802001564 DISC: 8% TYPE: ICP Jidekaiji Global Services
// 11105 ACC: 1802000052 DISC: 8% TYPE: ICP Daveloret Concepts
// 11119 ACC: 1802002079 DISC: 8% TYPE: ICP Just Comestics Ltd
// 11143 ACC: 1802003245 DISC: 8% TYPE: ICP Bona Multi Solutions LTD
// 11145 ACC: 1802003248 DISC: 8% TYPE: ICP DirimzSammy EnterprisE
// 11143 ACC: 1802003245 DISC: 8% TYPE: ICP Bona Multi Solutions LTD
// 11089 ACC: 1801012269 DISC: 8% TYPE: ICP  Minsendwell Limited
// 11175 ACC: 1802006011 DISC: 8% TYPE: ICP Ammid Adex Telecom
// 11479 ACC: 1804010489 DISC: 8% TYPE: ICP Pointek Technology Limited
// 11487 ACC: 1804010870 DISC: 8% TYPE: ICP DMANSOCOTEX GLOBAL ENT
// 9947 ACC: 1707003031 DISC: 8% TYPE: ICP  DANDOLAS NIGERIA LIMITED
// 6423 ACC: 1508005012 DISC: 8% TYPE: ICP  Wonder Worth Global Investment Ltd
// 11679 ACC: 1806000361 DISC: 8% TYPE: ICP CLICK NETWORKS ENTERPRISES
// 11677 ACC: 1806000362 DISC: 8% TYPE: ICP LADEHBAK VENTURES
// 11727 ACC: 1806002927 DISC: 8% TYPE: ICP Kazra Global Services LTD
// 11785 ACC: 1806007561 DISC: 8% TYPE: ICP Comm-spot Concepts
// 11701 ACC: 1806001261 DISC: 8% TYPE: ICP ADE OMO ADE DOUBLE CROWN SOLUTIONS
// 3693 ACC: 1806009668 DISC: 8% TYPE: ICP Island Computer College
// 11837 ACC: 1806010698 DISC: 8% TYPE: ICP  Borsh Ventures
// 11577 ACC: 1806001027 DISC: 8% TYPE: ICP Coscharlom Intl Associates Ltd
// 11603 ACC: 1806000570 DISC: 8% TYPE: ICP INNOFLEX CYBER ENT
// 11847 ACC: 1807002293 DISC: 8% TYPE: ICP Abbegold ICT Solutions
// 10203 ACC: 1807005928 DISC: 8% TYPE: ICP Binet Resources
// 11951 ACC: 1807008185 DISC: 8% TYPE: ICP Rebras Tech Limited
// 12041 ACC: 1808001709 DISC: 8% TYPE: ICP Rachma Concept
// 12045 ACC: 1808002541 DISC: 8% TYPE: ICP MR. Abusufiyan Sa'ad
// 11849 ACC: 1807000802 DISC: 8% TYPE: ICP Repsunny Integrated Services Ltd
// 12053 ACC: 1808003233 DISC: 8% TYPE: ICP Darphilz Links Ventures Limited
// 12073 ACC: 1808004284 DISC: 8% TYPE: ICP V-dile Solutions
// 12109 ACC: 1808008325 DISC: 8% TYPE: ICP Arktronics Travels & Tours Limited
// 12141 ACC: 1701000420 DISC: 8% TYPE: ICP NANO NG MULTI SOLUTIONS LTD
// 11847 ACC: 1807002293 DISC: 8% TYPE: ICP Abbegold ICT Solutions
    public com.smilecoms.pos.DiscountData getDynamicDiscountDataNGNew(com.smilecoms.xml.schema.pos.Sale sale, com.smilecoms.xml.schema.pos.SaleLine saleLine, double exRt, boolean isSubItem) throws Exception {
        String itemNumber = saleLine.getInventoryItem().getItemNumber();
        String serialNumber = saleLine.getInventoryItem().getSerialNumber();
        boolean isAirtime = serialNumber.equals("AIRTIME");
        boolean isBundle = itemNumber.startsWith("BUN");
        boolean isKitBundle = itemNumber.startsWith("BUNK");
        boolean isNonKitBundle = (isBundle && !isKitBundle);
        String promoCode = sale.getPromotionCode();
        String paymentMethod = sale.getPaymentMethod();
        String ccNumber = sale.getCreditAccountNumber();
        int salesPersonId = sale.getSalesPersonCustomerId();
        long recipientAccountId = sale.getRecipientAccountId();
        boolean inQuote = (paymentMethod == null || paymentMethod.isEmpty());
        com.smilecoms.pos.DiscountData dd = new com.smilecoms.pos.DiscountData();
        String channel = sale.getChannel();
        String warehouse = sale.getWarehouseId();
        boolean isKit = itemNumber.startsWith("KIT");
        int orgId = sale.getRecipientOrganisationId();
        if (paymentMethod == null) {
            paymentMethod = "";
        }
        if (ccNumber == null) {
            ccNumber = "";
        }

        if (itemNumber.equalsIgnoreCase("KIT1324") && !(salesPersonId == 241231
                || salesPersonId == 452987
                || salesPersonId == 70537
                || salesPersonId == 251919
                || salesPersonId == 239261
                || salesPersonId == 472857
                || salesPersonId == 23076
                || salesPersonId == 114937
                || salesPersonId == 241603
                || salesPersonId == 70547
                || salesPersonId == 22802)) {
            throw new Exception("KIT1324 is not available for selling by sales person " + salesPersonId);
        }

// if(itemNumber.equalsIgnoreCase("KIT1341") ||
// 	itemNumber.equalsIgnoreCase("KIT1342") 
        // ) {
        //    throw new Exception("Kit '" + itemNumber +  "' is temporarily not available for selling.");	
// }
        if (itemNumber.equalsIgnoreCase("KIT1334") && !(salesPersonId == 2)) {
            throw new Exception("KIT1334 is not available for selling by sales person " + salesPersonId);
        }

//if((itemNumber.equalsIgnoreCase("KIT1357") ||  itemNumber.equalsIgnoreCase("KIT1358")) && !(salesPersonId == 12356)) {
        //     throw new Exception("KIT1358 or KIT1357 is not available for selling by sales person " + salesPersonId);	
//}
        if (itemNumber.equalsIgnoreCase("KIT1340") && !(salesPersonId == 2)) {
            throw new Exception("KIT1340 is not available for selling by sales person " + salesPersonId);
        }

        if (itemNumber.equalsIgnoreCase("KIT1349") || itemNumber.equalsIgnoreCase("KIT1350")) {
            throw new Exception("KIT no longer available for selling.");
        }

// Olumide make KIT1374 available to Customer ID = 12356 (Adedokun Olagunju)
//if(itemNumber.equalsIgnoreCase("KIT1374") && !(salesPersonId == 12356)) {
        //     throw new Exception("KIT1374 is not available for selling by sales person " + salesPersonId);	
//}
// Olumide -  Make KIT1373 available to Customer ID = 2(Tunde)
//if(itemNumber.equalsIgnoreCase("KIT1373") && !(salesPersonId == 2)) {
        //     throw new Exception("KIT1373 is not available for selling by sales person " + salesPersonId);	
//}
// Olumide -  Make KIT1401 available to Customer ID = 12356 (Adedokun Olagunju) 
//if(itemNumber.equalsIgnoreCase("KIT1401") && !(salesPersonId == 12356)) {
//     throw new Exception("KIT1401 is not available for selling by sales person " + salesPersonId);	
//}
        if (itemNumber.equalsIgnoreCase("KIT1351") && !(salesPersonId == 2)) {
            // throw new Exception("KIT1351 is not available for selling by sales person " + salesPersonId);	
        }

// To restrict KIT1359 to Tunde Baale only
        if (itemNumber.equalsIgnoreCase("KIT1359") && !(salesPersonId == 2)) {
            // throw new Exception("KIT1359 is not available for selling by sales person " + salesPersonId);	
        }

// To restrict BENTDI020 to 143507,119483 (requested by Caro
//if(itemNumber.equalsIgnoreCase("BENTDI020") && !(salesPersonId == 143507 || salesPersonId == 119483)) {
        //   throw new Exception("BENTDI020 is not available for selling by sales person " + salesPersonId);	
//}
// To restrict KIT1380 to Tunde Baale only
//if(itemNumber.equalsIgnoreCase("KIT1380") && !(salesPersonId == 2)) {
        // throw new Exception("KIT1380 is not available for selling by sales person " + salesPersonId);	
//}
// Testing for upsize in SEP-  Restrict BUNL0001 available to Customer ID = 2 (Tunde Baale) only.
        if (itemNumber.equalsIgnoreCase("BUNL0001") && !(salesPersonId == 2)) {
            throw new Exception("BUNL0001-1GB Upsize is not available for selling by sales person " + salesPersonId);
        }

// Olumide ,Don't allow these KITS to be sold
        if (itemNumber.equalsIgnoreCase("KIT1355")
                || itemNumber.equalsIgnoreCase("KIT1356")
                || itemNumber.equalsIgnoreCase("KIT1357")
                || itemNumber.equalsIgnoreCase("KIT1358")
                || itemNumber.equalsIgnoreCase("KIT1365")
                || itemNumber.equalsIgnoreCase("KIT1366")
                || itemNumber.equalsIgnoreCase("KIT1371")
                || itemNumber.equalsIgnoreCase("KIT1372")
                || itemNumber.equalsIgnoreCase("KIT1373")
                || itemNumber.equalsIgnoreCase("KIT1374")
                || itemNumber.equalsIgnoreCase("KIT1390")
                || itemNumber.equalsIgnoreCase("KIT1391")
                || itemNumber.equalsIgnoreCase("KIT1401")
                || itemNumber.equalsIgnoreCase("KIT1378")
                || itemNumber.equalsIgnoreCase("KIT1398")
                || itemNumber.equalsIgnoreCase("KIT1399")
                || itemNumber.equalsIgnoreCase("KIT1400")
                || itemNumber.equalsIgnoreCase("KIT1344")
                || itemNumber.equalsIgnoreCase("KIT1345")
                || itemNumber.equalsIgnoreCase("KIT1351")
                || itemNumber.equalsIgnoreCase("KIT1352")
                || itemNumber.equalsIgnoreCase("KIT1363")
                || itemNumber.equalsIgnoreCase("KIT1364")
                || itemNumber.equalsIgnoreCase("KIT1376")
                || itemNumber.equalsIgnoreCase("KIT1388")
                || itemNumber.equalsIgnoreCase("KIT1389")
                || itemNumber.equalsIgnoreCase("KIT1392")
                || itemNumber.equalsIgnoreCase("KIT1393")
                || itemNumber.equalsIgnoreCase("KIT1402")
                || itemNumber.equalsIgnoreCase("KIT1403")
                || itemNumber.equalsIgnoreCase("KIT1404")) {
            throw new Exception("KIT not available for sale");
        }

// Testing refurbished on low utilisation,restricted KIT1379 to Emmanuel offem only
//if(itemNumber.equalsIgnoreCase("KIT1379") && !(salesPersonId == 70537)) {
        // throw new Exception("KIT1379 is not available for selling by sales person " + salesPersonId);	
//}
        if (itemNumber.equalsIgnoreCase("KIT1353") && !(salesPersonId == 2)) {
            throw new Exception("KIT1353 is not available for selling by sales person " + salesPersonId);
        }

//if( ( itemNumber.equalsIgnoreCase("KIT1336") || itemNumber.equalsIgnoreCase("KIT1337")) && !(salesPersonId == 2)) {
        //  throw new Exception("KIT1336/KIT1337 is not available for selling by sales person " + salesPersonId);	
//}
        if (salesPersonId == 28381 && ((isBundle && !isKitBundle) || isAirtime)) {
            throw new Exception("You are barred from selling airtime and bundles. You can only sell kits");
        }

        if (promoCode.equalsIgnoreCase("Super Dealer Discount") && (itemNumber.equalsIgnoreCase("KIT1357") || itemNumber.equalsIgnoreCase("KIT1358") || itemNumber.equalsIgnoreCase("KIT1374") || itemNumber.equalsIgnoreCase("KIT1401"))) {
            dd.setDiscountPercentageOff(10d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // New super dealer sales process-these Items can be sold to SD only.
        // The list of physical items that can be sold to Super Dealers for their discount of 11%
        String SDItems = "SIM8000 SIM8001 SIM8002 SIM8003 SIM8004 KIT1255 KIT1256 KIT1257 KIT1294 KIT1301 KIT1303 KIT1328 KIT1329 KIT1332 KIT1339 KIT1346 KIT1352 KIT1360 KIT1411 KIT1415 KIT1419 KIT1423";

        // HBT-6748 and HBT-7376 - New Franchise Model
        if (!itemNumber.startsWith("BUN") && (orgId == 8909 // Code2take Ventures - On hold
                || orgId == 2888 // Pheg Marathon Ventures
                || orgId == 10077 // Onigx Investment ltd
                || orgId == 9723 // Rezio Limited
                || orgId == 5059 // Callus Miller Company Limited
                )) {

            // New ICP Contract for Kaduna ICPs
            if (!paymentMethod.isEmpty() && !paymentMethod.equalsIgnoreCase("Credit Note") && !channel.startsWith("26P") && !channel.startsWith("27P") && !itemNumber.startsWith("BUN")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }

            if (SDItems.indexOf(itemNumber) != -1) {
                // This is one of the items in SDItems list
                int cnt = 0;
                // This logic counts how many kit items there are in the sale so we know if they have met their mimium of 500 in order to get the discount
                for (int i = 0; i < sale.getSaleLines().size(); i++) {
                    com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                    String it = otherLine.getInventoryItem().getItemNumber();
                    if (SDItems.indexOf(it) != -1) {
                        cnt += otherLine.getQuantity();
                    }
                }

                if (cnt >= 50 || promoCode.equalsIgnoreCase("Franchise Discount")) {
                    dd.setDiscountPercentageOff(7d);
                    dd.setTieredPricingPercentageOff(0d);
                    return dd;
                }
            }

            // Special discount for Callus Miller Company Limited
            if (isAirtime && orgId == 5059) {
                if (saleLine.getQuantity() >= 1000000) {
                    // This is airtime of over 1M
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(9.09090909d); // 9.0909% discount is the same as giving 10% extra airtime
                    return dd;
                }
            }

            if (isAirtime) {
                double discTier1 = 1d / (1d / 0.1d + 1d);
                double tier1Start = 2000000d / (1d - discTier1);

                long airtimeUnits = saleLine.getQuantity();
                if (airtimeUnits >= tier1Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier1 * 100d);
                    return dd;
                }
            }

            if (itemNumber.startsWith("KIT")) {
                // Dont allow the special Super Dealer KITs to be sold to anyone else
                throw new Exception("One or more of the items in the sale cannot be sold to Super Dealers");
            }
        }

        //New ICP contract for Kaduna ICPs
        // HBT-7239: Kaduna does not have a SD at the moment , all ICP in the region needs to be set up like SD on set up but get 8% discount.
        if (!itemNumber.startsWith("BUN") && (orgId == 6643 // Dahrah Global Limited
                || orgId == 6555 // Veemobile Services
                || orgId == 6997 // Value plus service & Industries Ltd
                || orgId == 7021 // Toluque Nigeria Limited
                || orgId == 6993 // Kima-Maxi Nigeria Limited
                || orgId == 7045 // Payit consulting Limited
                || orgId == 7009 // Detoyibo Enterprises
                || orgId == 2652 // Ditrezi Enterprises
                || orgId == 7843 // SAANET GLOBAL SERVICES LIMITED
                || orgId == 5479 // ImageAddiction
                || orgId == 9539 // Integrity Communications
                || orgId == 8283 // Digital Development Hub
                || orgId == 7417 // Synergy Mobile Business Solutions Limited
                || orgId == 9783 // 23RD Century Technologies LTD	
                || orgId == 9825 // Timboy Ventures
                || orgId == 9815 // Geniallinks Services
                || orgId == 9831 // Universal Asset Commercial Boker
                || orgId == 9941 // Manjub Limited
                || orgId == 10025 // Olans Communication
                || orgId == 7431 // Dpro Project
                || orgId == 10075 // Ultimate Aslan Limited
                || orgId == 6179 // Mo Concepts Co. LTD
                || orgId == 9631 // Crysolyte Networks
                || orgId == 7163 // Bloom Associates
                || orgId == 9649 // NM MARGI NIG LTD
                || orgId == 6929 // Micromanna Ltd.
                || orgId == 10019 // White house Computers Ltd
                || orgId == 10013 // Speedy IDeas Synergy Nig Ltd
                || orgId == 10181 // ALAJIM Limited
                || orgId == 10123 // MJ and partners power Electric
                || orgId == 10325 // Beyond Integrated Services
                || orgId == 10467 // ABSON Bussiness World LTD
                )) {
            // New ICP Contract for Kaduna ICPs
            if (!paymentMethod.isEmpty() && !paymentMethod.equalsIgnoreCase("Credit Note") && !channel.startsWith("26P") && !channel.startsWith("27P") && !itemNumber.startsWith("BUN")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }

            if (SDItems.indexOf(itemNumber) != -1) {
                // This is one of the items in SDItems list
                int cnt = 0;
                // This logic counts how many kit items there are in the sale so we know if they have met their mimium of 500 in order to get the discount
                for (int i = 0; i < sale.getSaleLines().size(); i++) {
                    com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                    String it = otherLine.getInventoryItem().getItemNumber();
                    if (SDItems.indexOf(it) != -1) {
                        cnt += otherLine.getQuantity();
                    }
                }

                if (cnt >= 500 || promoCode.equalsIgnoreCase("ICP Device Discount")) {

                    dd.setDiscountPercentageOff(8d);
                    dd.setTieredPricingPercentageOff(0d);
                    return dd;
                }
            }

            if (isAirtime) {
                double discAllTiers = 1d / (1d / 0.07d + 1d);
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(discAllTiers * 100d);
                return dd;
            }

            if (itemNumber.startsWith("KIT")) {
                // Dont allow the special Super Dealer KITs to be sold to anyone else
                throw new Exception("One or more of the items in the sale cannot be sold to Super Dealers");
            }

        }

        if ((itemNumber.equalsIgnoreCase("KIT1343")) && !(salesPersonId == 12356 || salesPersonId == 4304 || salesPersonId == 386833)) {
            throw new Exception("KIT1343 - Permission denied for " + salesPersonId);
        }

        if (itemNumber.equals("KIT1343") && orgId != 9647 && promoCode.equalsIgnoreCase("Super Dealer Discount")) {
            throw new Exception("That kit (KIT1343) is only for YFree.");
        }

        if (promoCode.equalsIgnoreCase("Super Dealer Discount") && orgId == 9647 && itemNumber.equalsIgnoreCase("KIT1343")) {
            dd.setDiscountPercentageOff(1.232d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // New super dealer sales process (Super Dealers)
        if (orgId == 8623 // ZEPH ASSOCIATES NIGERIA LIMITED
                || orgId == 8641 // Data Partners Limited
                || orgId == 8639 // Gabros Divine Technologies Limited
                || orgId == 8649 // CANT STOP NIGERIA LIMITED GOlden Rule
                || orgId == 9109 // Concise Services Limited
                || orgId == 2643 // Timjat Global Resources Ltd
                || orgId == 9615 // Digital Development Hub Consultancy Services Ltd
                || orgId == 9647 // YFree SD
                || orgId == 6633 // Lyds Louisa Intergrated Company Limited
                || orgId == 7057 // Sonite Communications Limited
                || orgId == 10839 // Lustre Communications ltd
                ) {
            // The item is being sold to a Super Dealer Organisation
            if (!paymentMethod.isEmpty() && !paymentMethod.equalsIgnoreCase("Credit Note") && !channel.startsWith("26P") && !channel.startsWith("27P") && !itemNumber.startsWith("BUN")) {
                // Dont allow the wrong channel to be used when selling to Super Dealers
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }

            if (isAirtime && saleLine.getQuantity() >= 3000000) {
                // This is airtime of over 5M
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(9.09090909d); // 9.0909% discount is the same as giving 10% extra airtime
                return dd;
            } else if (SDItems.indexOf(itemNumber) != -1) {
                // This is one of the items in SDItems list
                int cnt = 0;
                // This logic counts how many kit items there are in the sale so we know if they have met their mimium of 500 in order to get the discount
                for (int i = 0; i < sale.getSaleLines().size(); i++) {
                    com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                    String it = otherLine.getInventoryItem().getItemNumber();
                    if (SDItems.indexOf(it) != -1) {
                        cnt += otherLine.getQuantity();
                    }
                }

                if (cnt >= 500 || promoCode.equalsIgnoreCase("Super Dealer Discount")) {

                    if (orgId == 7057 && itemNumber.equalsIgnoreCase("KIT1332")) {
                        dd.setDiscountPercentageOff(19.379845d);
                        dd.setTieredPricingPercentageOff(0d);
                        return dd;
                    }

                    // The sale does have over 500 devices in it
                    dd.setDiscountPercentageOff(10d);
                    dd.setTieredPricingPercentageOff(0d);
                    return dd;
                }
            } else if (itemNumber.startsWith("KIT")) {
                throw new Exception("One or more of the items in the sale cannot be sold to Super Dealers");
            }
        } else if (itemNumber.startsWith("KIT") && SDItems.indexOf(itemNumber) != -1) {
            // Dont allow the special Super Dealer KITs to be sold to anyone else
            throw new Exception("One or more of the items in the sale can only be sold to Super Dealers");
        }

        if (itemNumber.startsWith("BUNST") || itemNumber.startsWith("BUNTV")) {
            if (!paymentMethod.equals("") && !paymentMethod.equals("Credit Account")) {
                throw new Exception("Staff bundles must be paid by credit account");
            }
            // 100% discount for Staff bundles
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.startsWith("BUNP") && saleLine.getInventoryItem().getPriceInCentsIncl() > 0d) {
            if (!paymentMethod.equals("") && !paymentMethod.equals("Cash") && !itemNumber.equals("BUNPG0000")) {
                throw new Exception("Gift bundles must be paid by cash");
            }
            // 100% discount for Promotional bundles
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // HBT-7518
        if (itemNumber.equals("BUNDC3007")) {
            dd.setDiscountPercentageOff(50.00d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

//HBT 8148
        if (itemNumber.equals("BUNDC3005")) {
            dd.setDiscountPercentageOff(37.5d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BUNDC3001")) {
            dd.setDiscountPercentageOff(50.00d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BUNDC3003")) {
            dd.setDiscountPercentageOff(66.66666667d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BUNDC3010")) {
            dd.setDiscountPercentageOff(44.44444444d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // HBT-7231 - Creation of Promo Bundle for Winback Campaign
        if (itemNumber.equals("BUNDC4003")) {
            // 27.5204754656809d % discount as per Mike's comment on HBT-7231 to bring the price down to NGN1000
            dd.setDiscountPercentageOff(27.5204754656809d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // HBT-7386 - Creation of Promo Bundle for Winback Campaign 2
        if (itemNumber.equals("BUNDC4007")) {
            // 68.9373d% discount as per Mike's comment on HBT-7231 to bring the price down to NGN1000
            dd.setDiscountPercentageOff(68.9373d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BUN1020") && (paymentMethod.equals("Credit Account") || paymentMethod.isEmpty()) && orgId == 8465) {
            // 5% discount for NCC on 20GB Bundle as per HBT-6414
            dd.setDiscountPercentageOff(5d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // HBT-6815 
        if ((orgId == 3199) && (itemNumber.equals("BENTOS200") || itemNumber.equals("BUN1200")) && promoCode.equals("4.76% off 200GB Anytime Sharing Bundle")) {
            dd.setDiscountPercentageOff(4.76d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

// HBT-7104
        if ((orgId == 8557 || orgId == 6091) && (itemNumber.equals("BENTOS200") || itemNumber.equals("BUN1200")) && promoCode.equals("5% off 200GB Anytime Sharing Bundle")) {
            dd.setDiscountPercentageOff(5.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Ensure that sales of non corporate GBR items are not open for more than 7 days
        boolean isCorpGBR = itemNumber.startsWith("BENT");
        if ((isAirtime || isKit) && !isCorpGBR && sale.getExpiryDate() != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sale.getSaleDate().toGregorianCalendar().getTime());
            cal.add(java.util.Calendar.DATE, 7);
            if (sale.getExpiryDate().toGregorianCalendar().getTime().after(cal.getTime())) {
                throw new Exception("Only Corporate GBR can be on an invoice open for more than 7 days");
            }
        }

        // 100% discount on routers and sims and dongles testing purposes when testing new products, kits etc
        if (promoCode.equalsIgnoreCase("TestingDiscount")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BENTBE010") || itemNumber.equals("BENTBE020") || itemNumber.equals("BENTBE030")) {
            if (saleLine.getQuantity() >= 12) {
                // 10% discount when buying enterprise best effort 12 months in advance
                dd.setDiscountPercentageOff(10d);
            } else if (saleLine.getQuantity() >= 6) {
                // 10% discount when buying enterprise best effort 6 months in advance
                dd.setDiscountPercentageOff(5d);
            }
            if (itemNumber.equals("BENTBE010") || itemNumber.equals("BENTBE020") || itemNumber.equals("BENTBE030")) {
                if (promoCode.equalsIgnoreCase("5% Discount on Corporate Access")) {
                    // Additional 5% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 5d);
                } else if (promoCode.equalsIgnoreCase("10% Discount on Corporate Access")) {
                    // Additional 10% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 10d);
                } else if (promoCode.equalsIgnoreCase("15% Discount on Corporate Access")) {
                    // Additional 15% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 15d);
                } else if (promoCode.equalsIgnoreCase("16.5% Discount on Corporate Access")) {
                    // Additional 16.5% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 16.5d);
                } else if (promoCode.equalsIgnoreCase("30% Discount on Corporate Access")) {
                    // Additional 30% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 30d);
                } else if (promoCode.equalsIgnoreCase("33% Discount on Corporate Access")) {
                    // Additional 33% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 33d);
                }
            }
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.startsWith("ROU")) {
            for (int i = 0; i < sale.getSaleLines().size(); i++) {
                com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                String it = otherLine.getInventoryItem().getItemNumber();
                if (otherLine.getQuantity() >= 6 && (it.equals("BENTBE010") || it.equals("BENTBE020") || it.equals("BENTBE030"))) {
                    // Get 100% off the router when buying 6 months enterprise best effort up front
                    dd.setDiscountPercentageOff(100d);
                    return dd;
                }
            }
        }

        if (itemNumber.equals("KIT1217") && orgId != 3700) {
            throw new Exception("That kit is only for Diamond Bank");
        }

        if (!itemNumber.equals("KIT1293") && !itemNumber.equals("KIT1287") && !itemNumber.equals("KIT1289") && !itemNumber.equals("KIT1288") && !itemNumber.equals("KIT1290") && !itemNumber.startsWith("BAT") && !itemNumber.startsWith("SIM") && !itemNumber.startsWith("BUN") && saleLine.getInventoryItem().getPriceInCentsIncl() == 0d && !paymentMethod.isEmpty() && !paymentMethod.equals("Credit Note") && sale.getSalesPersonCustomerId() != 1) {
            throw new Exception("A sale with a zero item price is not allowed!");
        }

        if (!isSubItem && !paymentMethod.isEmpty() && !paymentMethod.equals("Staff") && !paymentMethod.equals("Loan") && !paymentMethod.equals("Credit Note") && itemNumber.startsWith("SIM") && promoCode.isEmpty() && !promoCode.contains("100% Volume Break")) {
            throw new Exception("A standalone SIM can only be sold using a SIMSwap promotion code. SIMs sold with SIMSwap promotion codes can only be used in a SIM Swap");
        }

        if (!paymentMethod.isEmpty() && !paymentMethod.equals("Loan") && promoCode.equalsIgnoreCase("HotSpot")) {
            throw new Exception("Hotspots must be loaned");
        }
        if (itemNumber.equals("KIT1207") && !paymentMethod.equals("Staff") && !inQuote) {
            throw new Exception("Selected Kit is exclusive to Smile Staff");
        }
        if (!inQuote && !ccNumber.equals("CSI003") && (promoCode.equalsIgnoreCase("Education") || promoCode.equalsIgnoreCase("Community"))) {
            throw new Exception("CSI Promotions must be on the CSI credit account. Add CSI003 as the credit account number for this Organisation and select Credit Account as the payment method");
        }

        // 100% discount on routers and sims for educational institutions
        if (!isAirtime && promoCode.equalsIgnoreCase("Education")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims for brand ambassadors
        if (!isAirtime && promoCode.equalsIgnoreCase("BrandAmbassador")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on sims for faulty sim
        if (itemNumber.startsWith("SIM") && promoCode.equalsIgnoreCase("FreeSIMSwap")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims for communities 
        if (!isAirtime && promoCode.equalsIgnoreCase("Community")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims and dongles for gifts and give aways
        if (!isAirtime && promoCode.equalsIgnoreCase("UCGiveAway")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on ICP tools of trade event
        if (!isAirtime && promoCode.equalsIgnoreCase("ICPToolOfTrade")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Discounts when doing returns/replacements of ATEL
        if (!isSubItem && (itemNumber.equals("MIF6003") || itemNumber.equals("MIF6006")) && promoCode.contains("Pays 30%")) {
            dd.setDiscountPercentageOff(70d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        if (!isSubItem && (itemNumber.equals("MIF6003") || itemNumber.equals("MIF6006")) && promoCode.contains("Pays 50%")) {
            dd.setDiscountPercentageOff(50d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        if (!isSubItem && (itemNumber.equals("MIF6003") || itemNumber.equals("MIF6006")) && promoCode.contains("Pays 70%")) {
            dd.setDiscountPercentageOff(30d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // 100% discount on static IP
        if (itemNumber.startsWith("BUNIP") && promoCode.equalsIgnoreCase("100% discount on Static IP")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Percentage based volume breaks
        if (!isAirtime && promoCode.contains("% Volume Break") && !isNonKitBundle) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(discPercent);
            return dd;
        }

        // Percentage based discounts
        if (!isAirtime && promoCode.contains("% Discount") && !isNonKitBundle) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(discPercent);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Temp for airtime commissions
        if (isAirtime && promoCode.equals("100% Off Airtime")) {
            dd.setDiscountPercentageOff(100.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (isAirtime && promoCode.equals("5% Off Airtime")) {
            dd.setDiscountPercentageOff(5.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (isAirtime && promoCode.equals("10% Off Airtime")) {
            dd.setDiscountPercentageOff(10.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (isAirtime && promoCode.equals("15% Off Airtime")) {
            dd.setDiscountPercentageOff(15.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (isBundle && promoCode.equals("5% Off Data Bundle")) {
            dd.setDiscountPercentageOff(5.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (isBundle && promoCode.equals("10% Off Data Bundle")) {
            dd.setDiscountPercentageOff(10.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (isBundle && promoCode.equals("15% Off Data Bundle")) {
            dd.setDiscountPercentageOff(15.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (orgId == 2889 // Asbam Global Resources
                || orgId == 2893 // Lightning Networks
                || orgId == 2894 // Green Heritage                
                ) {
            // Old ICP Contract
            if (!paymentMethod.isEmpty() && !channel.startsWith("21E") && !channel.startsWith("25I") && !channel.startsWith("27I") && !itemNumber.startsWith("BUN")) {
                throw new Exception("Sales to ICPs must use channel starting with 21E or 25I or 27I");
            }
            if (isAirtime) {
                // ICPs get 8% Discount
                dd.setDiscountPercentageOff(8d);
                dd.setTieredPricingPercentageOff(0d);
                return dd;
            }
            if (isKit) {
                // ICPs Get 10% off kits
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(10d);
                return dd;
            }
        }

        if (itemNumber.equals("BUNIC1005")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        String simKits = "KIT1208 KIT1209 KIT1210";
        String deviceKits = "KIT1238 KIT1239 KIT1200 KIT1201 KIT1202 KIT1203 KIT1204 KIT1205 KIT1206 KIT1211 KIT1212 KIT1213 KIT1214 KIT1215 KIT1218 KIT1219 KIT1220 KIT1222 KIT1223 KIT1221 KIT1224 KIT1227 KIT1228 KIT1229 KIT1231 KIT1232 KIT1240 KIT1241 KIT1242 KIT1244 KIT1245 KIT1246 KIT1251 KIT1252 KIT1267 KIT1263 KIT1261 KIT1265 KIT1279 KIT1280 KIT1266 KIT1286 KIT1281 KIT1284 KIT1282 KIT1274 KIT1272 KIT1270 KIT1291 KIT1259 KIT1299 KIT1300 KIT1302 KIT1305 KIT1304";

        // New ICP contract
        if (!itemNumber.startsWith("BUN") && (orgId == 3670 //Adibba Online Shopping LTD
                || orgId == 2918 //Aghayedo Brothers Enterprises
                || orgId == 3677 //Bankytech Communications Ventures
                || orgId == 4222 //Barcutt Investment Limited
                || orgId == 3915 //BIGDATA TECHNOLOGIES LTD
                || orgId == 2121 //C-Soka Limited
                || orgId == 3412 //Galant Communications LTD
                || orgId == 4938 //Global Rolling
                || orgId == 4679 //Ideal Hospitality Solutions Limited
                || orgId == 3143 //Koam Computer Solution
                || orgId == 2630 //Netpoint Communications
                || orgId == 2123 //Novas Systems Limited
                || orgId == 2888 //Pheg Marathon Ventures
                || orgId == 3165 //Pine Height Global Resources ltd
                || orgId == 2643 //Timjat Global Resources Ltd
                || orgId == 3675 //Trend World Wide Ltd
                || orgId == 3413 //Wireless Link Integrated
                || orgId == 2917 //Yfree Solutions Limited
                || orgId == 2653 //Zacfred Systems Solutions Ltd
                || orgId == 5026 //Top on Top Success Global Limited
                || orgId == 5040 // Rafael stores
                || orgId == 4201 // Abetech
                || orgId == 5074 // Globasure Technologies
                || orgId == 2122 // Best
                || orgId == 5117 // VConnect
                || orgId == 5210 // Phone Hut
                || orgId == 5127 // Content Oasis
                || orgId == 5162 // Sainana Resources
                || orgId == 5165 // Hasino Communication
                || orgId == 5215 // Christy George
                || orgId == 5056 // Everyday group
                || orgId == 5242 // Telxtra
                || orgId == 5293 // Cedar network
                || orgId == 5289 // Korenet
                || orgId == 5292 // Skyraph
                || orgId == 5279 // Tobowy
                || orgId == 5450 // Sacen Projects
                || orgId == 5446 // Central Dynamics
                || orgId == 5444 // Pearl Facet
                || orgId == 5445 // Emmrose
                || orgId == 5447 // Einfotech
                || orgId == 5452 // Kevicomms
                || orgId == 5448 // Bel comms
                || orgId == 5472 // Onis Digital
                || orgId == 285 // Sabaoth
                || orgId == 5498 // Biz Ad Solution
                || orgId == 5493 // Pinnacle Wireless
                || orgId == 5499 // GG Communications
                || orgId == 5501 // Tim Sholtz Nigeria Limited
                || orgId == 5491 // Baytown Communications
                || orgId == 5500 // Spectrum Innovation Technologies
                || orgId == 5489 // Wanaga Nig Ltd
                || orgId == 5490 // Primadas Investment Nig. Ltd
                || orgId == 5478 // Bayunfa Ventures Nig.Ltd
                || orgId == 5487 // North East Gold
                || orgId == 5473 // Azman Concepts Limited
                || orgId == 5474 // Infotek Perspective Service Limited
                || orgId == 5449 // Sayrex
                || orgId == 5483 // 3CEE
                || orgId == 5060 // Integral
                || orgId == 5480 // Samace
                || orgId == 5482 // Zioneli
                || orgId == 5504 // Belle vista
                || orgId == 5506 // Dot.com ventures
                || orgId == 5519 // Burj Khalifah Nigeria Limited
                || orgId == 5521 // Datacounter International Ventures
                || orgId == 5534 // Yield Systems
                || orgId == 5529 // Arctuals Concepts Limited
                || orgId == 5530 // Emax Solutions Ventures Ltd
                || orgId == 5539 //  I Will Distribute
                || orgId == 5565 // Broad Prject Nigeria
                || orgId == 5566 // Heirs Multi Concept
                || orgId == 5567 // ACMESYNERGY
                || orgId == 5555 // COMTRANET IT SOLUTIONS LTD
                || orgId == 5577 // JOTECH COMPUTER
                || orgId == 5599 // CHARLOTTE GLOBAL CONCEPTS
                || orgId == 5059 // Callus Miller
                || orgId == 5479 // ImageAddiction
                || orgId == 5631 // Ages Accurate Speed Ltd
                || orgId == 5632 // Kestrel tours
                || orgId == 1361 // ONILU PRIME
                || orgId == 5680 // Trebkann
                || orgId == 5713 // Recharge Hub Services 
                || orgId == 5715 // Suremobile Network Ltd
                || orgId == 5729 // Olive Integrated Services Limited
                || orgId == 5769 // Centri-net Technology Nig. Ltd
                || orgId == 5823 // Merc-Oaton
                || orgId == 5839 // Pristine Panache
                || orgId == 5833 // FF Tech
                || orgId == 5837 // Cheroxy
                || orgId == 5889 // Optimum Greenland
                || orgId == 5925 // Foimm Technology Enterprise
                || orgId == 5921 // Oakmead Realtors
                || orgId == 5927 // First Sopeq Integrated Nig Ltd
                || orgId == 5929 // SDMA Networks
                || orgId == 6013 // Viclyd Nig Ent
                || orgId == 5062 // CHUXEL COMPUTERS
                || orgId == 6019 // BROADLAND GLOBAL RESOURCES LIMITED
                || orgId == 6051 // Reart Risk and Safety Management Limited
                || orgId == 6081 // Mesizanee Concept Limited
                || orgId == 6149 // Bailord Limited
                || orgId == 6155 // Sweetplanet Worldwide Enterprise
                || orgId == 6181 // SHELL EAST STAFF INVESTMENT CO-OP
                || orgId == 6179 // Mo Concepts Co
                || orgId == 6175 // Great Bakis Ventures 
                || orgId == 6177 // Pose Technologies Ltd
                || orgId == 6211 // Shetuham Intl Ltd
                || orgId == 6271 // Avensa Global solutions
                || orgId == 6283 // Derak Multi Ventures Nigeria Limited
                || orgId == 6311 // Donval Platinum Investment
                || orgId == 6299 // Best Baxany Global Services Limited
                || orgId == 6307 // BrightGrab Integrated Services
                || orgId == 6289 // Final Logic Limited
                || orgId == 6339 // Top up Afric
                || orgId == 6369 // Mcedwards Ent.Ltd
                || orgId == 6407 // Lumron Enterprises
                || orgId == 6419 // SWING-HIGH COMMUNICATIONS LIMITED
                || orgId == 6437 // New Sonshyne Systems Limited
                || orgId == 6417 // Simcoe & Dalhousie Nigeria Limited
                || orgId == 6423 // Wonder Global Investment Ltd.Organisation Id 
                || orgId == 6459 // CHARLYN NETWORKS LTD
                || orgId == 6461 // Febby Communication
                || orgId == 6519 // Rakameris Ventures
                || orgId == 6523 // The Koptim Company
                || orgId == 6537 // D.S. Computer-Tech
                || orgId == 6547 // KVC E-Mart
                || orgId == 6541 // Security Services and Consultancy
                || orgId == 6557 // Globe Du Bellissima LTD
                || orgId == 6561 // Kamzi Global Enterprise
                || orgId == 6581 // ING Systems
                || orgId == 6583 // Codex Plus
                || orgId == 6599 // Phones and Computers One- Stop Center Limited
                || orgId == 6617 // KTO ENT
                || orgId == 6615 // GENIE INFOTECH LTD
                || orgId == 6607 // Kollash Business and Technologies Limited
                || orgId == 6623 // Sovereign Gold Service
                || orgId == 6631 // Abhala Ben International Ventures 
                || orgId == 6629 // J.E.A Global Ventures
                || orgId == 6641 // Jenshu Int'l Services Ltd
                || orgId == 6659 // YOMEEZ ELECTROWORLD LTD
                || orgId == 6685 // Soofine Print Limited
                || orgId == 6687 // Lindski
                || orgId == 6689 // Frotkom Nigeria Limited.
                || orgId == 6735 // Avro Group
                || orgId == 6733 // Isladex Communications
                || orgId == 6657 // Rich Technologies Limited
                || orgId == 6745 // Joeche Computer Warehouse
                || orgId == 6653 // Ambuilt Works Limited 
                || orgId == 6719 // Le Charm International Limited
                || orgId == 2918 // Aghayedo Brothers Enterprises
                || orgId == 6565 // EMSI Enterprise
                || orgId == 6761 // hephyOceans Integrated Services Limited
                || orgId == 6665 // Oliver1010 Communications Concept Limited
                || orgId == 6625 // Surfspot Communications Limited
                || orgId == 6633 // Lydslouisa Integrated Company Limited
                || orgId == 6747 // First Regional Solutions Limited. Id
                || orgId == 6779 // The Gizmo world And Solution Limited
                || orgId == 6771 // Easy Talil Enterprise
                || orgId == 6785 // Nofot Gardma Limited
                || orgId == 6823 // Omovo Group
                || orgId == 6821 // Fibre Path
                || orgId == 6787 // Digital Direct
                || orgId == 6819 // Xtraone Family Minstrel Ventures
                || orgId == 6827 // Topcoat Limited
                || orgId == 6861 // General B Y Supermarket
                || orgId == 6805 // Global Quid Ltd
                || orgId == 6923 // LeeBroadVentures
                || orgId == 6917 // Moben-D' Excel Limited
                || orgId == 7011 // SMB Shanono Global Venture Ltd
                || orgId == 6995 // Melendless Ltd
                || orgId == 7057 // Sonite communication
                || orgId == 7089 // Madeeyak
                || orgId == 7073 // Integrated Communication Engineering 
                || orgId == 6949 // caso communication
                || orgId == 7141 // Syrah Communication Limited
                || orgId == 7145 // Morgen Global Limited
                || orgId == 7149 // SDMA Limited Surulere
                || orgId == 7157 // Good Venture Integrated Concepts Limited
                || orgId == 7167 // DAKESH DYNAMIC SERVICES
                || orgId == 7197 // Skenyo Global Services Limited
                || orgId == 7217 // REMGLORY VENTURES NIGERIA LTD
                || orgId == 7209 // U-MAT SUCCESS VENTURES
                || orgId == 7215 // TOKAZ CONCEPT
                || orgId == 7229 // Russo Link
                || orgId == 7207 // Gofulene Integrated Service Ltd
                || orgId == 7227 // Laidera Consulting Ltd
                || orgId == 289 // Olamind Technology
                || orgId == 7273 // ABBAH TECH CONNECTIONS LTD
                || orgId == 7295 // Commonwealth Technologies Ltd
                || orgId == 7293 // Vantage Options Nigeria Ltd
                || orgId == 7297 // B-Jayc Global Resources Limited
                || orgId == 7291 // SDMA Network Badagry
                || orgId == 7347 // Amarcelo Limited
                || orgId == 7159 // MULTI RESOURCES AND INDUSTRIAL SERVICES
                || orgId == 7381 // POYEN NOMOVO NIG LTD
                || orgId == 6769 // G-PAY INSTANT SOLUTIONS
                || orgId == 7073 // Intergrated Communication Engineering
                || orgId == 7431 // Dpro Project Limited
                || orgId == 7417 // Synergy Mobile Business Solutions Limited
                || orgId == 7429 // IDEA KONSULT LIMITED
                || orgId == 7453 // Harry Concern Nigeria Enterprises
                || orgId == 7399 // Dream Nation Alliance (MPCS) Limited
                || orgId == 7329 // Contec Global Infotech Ltd
                || orgId == 7477 // ALTS Service Consult Limited
                || orgId == 7493 // Ski Current Services Limited
                || orgId == 7491 // Emir Label Entertainment Limited
                || orgId == 7559 // Mercury Resources And Investment Company Ltd
                || orgId == 7575 // Micro Link Telecoms Limited
                || orgId == 7237 // Star Gaming Limited
                || orgId == 7727 // Neocortex International Ltd. ID
                || orgId == 7697 // UCAS GLOBAL RESOURCES
                || orgId == 7771 // Zillion Technocom
                || orgId == 7791 // Divine Anchor
                || orgId == 7821 // Grinotech limited
                || orgId == 7845 // Banc Solutions Ltd
                || orgId == 7851 // Next Bt Ltd
                || orgId == 7881 // Uzogy Integrated Technical Service
                || orgId == 7919 // Anne colt intergrated services ltd
                || orgId == 7887 // Fashamu Bookshop and Business Centre
                || orgId == 7921 // TIDAKE GLOBAL LIMITED
                || orgId == 8051 // RULESIXMOBILE LTD
                || orgId == 8139 // Texmaco Ventures Nigeria Limited
                || orgId == 8185 // Payquic World Wide Ltd
                || orgId == 8191 // Kemasho Ventures Ltd
                || orgId == 8215 // I CELL MOBILE
                || orgId == 8279 // Lemor Global Services
                || orgId == 8361 // SmartDrive & Systems Limited
                || orgId == 8107 // Cordiant Technology Services
                || orgId == 8511 // Nartel Ventures
                || orgId == 8615 // Flux Integrated Services Ltd
                || orgId == 8903 // Emma Paradise Multilinks Limited
                || orgId == 8909 // CODE2TAKE VENTURES LIMITED
                || orgId == 9049 // Northsnow Enterprises
                || orgId == 9039 // Hadiel Calidad Limited
                || orgId == 9091 // Enatel Communications
                || orgId == 9629 // Diksolyn Concept Limited
                || orgId == 9625 // Global Lightcrown Ventures	
                || orgId == 9637 // Network Enterprises PH
                || orgId == 5985 // ICP Capricorn Digital
                || orgId == 8283 // Digital Development Hub
                || orgId == 10059 // Glitteringway enterprise
                || orgId == 10009 // Asrad Multi-links Business and logistic limited
                || orgId == 10001 // BlueBreed Enterprise
                || orgId == 10025 // Olans Communication
                || orgId == 7431 // Dpro Project
                || orgId == 9885 // Happymood enterprise
                || orgId == 9975 // Godsaves devices Tech Ltd
                || orgId == 9945 // Aumak-mikash Ltd
                || orgId == 9999 // Aje Global Multiservices
                || orgId == 9963 // FEM9 TOUCH LIMITED
                || orgId == 9973 // Joelink Global Concept communication Ltd
                || orgId == 9967 // Goldenworths West Africa LTD
                || orgId == 5488 // Blessing Computers
                || orgId == 5519 // Burj Khalifah Nigeria Limited
                || orgId == 2652 // Ditrezi Enterprises
                || orgId == 9947 // DANDOLAS NIGERIA LIMITED
                || orgId == 9941 // Manjub Limited
                || orgId == 9939 // Tecboom Digital Ltd
                || orgId == 9933 // Da-Costa F.A and Company
                || orgId == 9929 // Jb Mwonyi Company Limited
                || orgId == 9883 // Oroborkso Ventures Nigeria Limited
                || orgId == 9875 // Chrisoduwa Global Ventures
                || orgId == 9887 // Multitronics Global Enterprise
                || orgId == 9861 // DRUCICARE NIG LTD
                || orgId == 9881 // Beejao Infosolutions Limited
                || orgId == 9877 // Sam Fransco Resources
                || orgId == 9873 // GLORIOUS TELECOMM
                || orgId == 9867 // AMIN AUTO LTD
                || orgId == 9931 // Boxtech Consult
                || orgId == 9649 // NM MARGI NIG LTD
                || orgId == 9897 // Brownnys-Connect Nigeria Limited
                || orgId == 9835 // K-CEE DEVICES LIMITED
                || orgId == 9859 // Budddybod Enterprises
                || orgId == 9863 // GOLDENPEECOMPUTERS TECHNOLOGIES
                || orgId == 9831 // Universal Asset Commercial Boker
                || orgId == 9847 // Fundripples Limited
                || orgId == 9841 // Sharptowers Limited
                || orgId == 9549 // Payconnect Networks Ltd
                || orgId == 9827 // Leverage Options
                || orgId == 9793 // Sunsole Global Investment Ltd
                || orgId == 9801 // Godmic Nigeria LTD
                || orgId == 9825 // Timboy Ventures
                || orgId == 9815 // Geniallinks Services
                || orgId == 9783 // 23RD Century Technologies LTD
                || orgId == 9755 // Abiola Gold Resources
                || orgId == 9753 // Hetoch Technology
                || orgId == 9747 // Mac Onella
                || orgId == 9711 // Finet Communications Ltd
                || orgId == 9723 // Rezio Limited
                || orgId == 10041 // Base 15 Concept Nigeria Limited
                || orgId == 9571 // Cyberdyne Integrated Limited
                || orgId == 10079 // LTC network limited
                || orgId == 10077 // Onigx Investment Limited
                || orgId == 10085 // Global Communication and Digital services
                || orgId == 10115 // Dolamif Nigeria Limited
                || orgId == 10151 // TICEE CONNECT
                || orgId == 10159 // Poatech Enterprises Nigeria Ltd
                || orgId == 10171 // Pesuzok Nig Ltd
                || orgId == 9731 // Belledave Nigeria Limited
                || orgId == 10163 // Option 360 Communications
                || orgId == 10179 // Popsicles Enterprises 
                || orgId == 10109 // Ahijo Ventures limited
                || orgId == 10215 // Rahmatob Computer Concept
                || orgId == 10219 // Kastech Technologies
                || orgId == 8909 // Code2take Ventures
                || orgId == 10221 // Profast Integrated Services Limited
                || orgId == 10323 // OPKISE GLOBAL CONCEPTS
                || orgId == 10303 // PC Corner Services Limited
                || orgId == 10429 // GABNET TECHNOLOGY
                || orgId == 10427 // LATEETOY NIGERIA LIMITED            
                || orgId == 9609 // Lexton Energy
                || orgId == 10553 // Olive-Frank Global Limited
                || orgId == 673419 // GreenTag Investment Limited 
                || orgId == 10571 // Pristine Stitches Enterprise 
                || orgId == 10639 // Grandieu Limited
                || orgId == 10657 // ELVESTA VENTURES
                || orgId == 10545 //Adubi-Gold Nig Enterprises
                || orgId == 10649 // UJE AND GROM CONCEPTS
                || orgId == 10701 // Caiviks Technologies
                || orgId == 10037 // Bizhub solution
                || orgId == 10753 // HORLMIL CONTINENTAL SERVICES
                || orgId == 10755 // EIUCOM RECHARGE HUB CONCEPTS
                || orgId == 3 // Helpline Telecoms Nig. LTD
                || orgId == 10251 // Otomab and Sons Nigeria Limited
                || orgId == 10905 // JATRAB VENTURES
                || orgId == 10991 // GodfreyBrown LTD
                || orgId == 11001 // Okay Fones Limited
                || orgId == 10915 // Embehie Enterprises
                || orgId == 10923 // Worthy Realtors
                || orgId == 10925 // Holad best International
                || orgId == 10955 // Natural colour press	
                || orgId == 10913 // Sambrix International Limited
                || orgId == 10979 // Is-Hami Construction Limited
                || orgId == 11075 // Adedayo Adedeji
                || orgId == 11039 // Shounix Global Venture
                || orgId == 11077 // CECON24 Systems Limited
                || orgId == 11091 // Correspondence Limited ID
                || orgId == 11055 // Evabest communications
                || orgId == 11127 // Jidekaiji Global Services
                || orgId == 11105 // Daveloret Concepts
                || orgId == 11119 // Just Comestics Ltd
                || orgId == 11143 // Bona Multi Solutions LTD
                || orgId == 11145 // DirimzSammy EnterprisE
                || orgId == 11143 // Bona Multi Solutions LTD
                || orgId == 11089 // Minsendwell Limited
                || orgId == 11175 // Ammid Adex Telecom
                || orgId == 11479 // Pointek Technology Limited
                || orgId == 9947 // DANDOLAS NIGERIA LIMITED
                || orgId == 6423 // Wonder Worth Global Investment Ltd
                || orgId == 11487 // DMANSOCOTEX GLOBAL ENT
                || orgId == 11679 //  CLICK NETWORKS ENTERPRISES
                || orgId == 11677 // LADEHBAK VENTURES
                || orgId == 11727 //Kazra Global Services LTD
                || orgId == 11785 //Comm-spot Concepts
                || orgId == 11701 //ADE OMO ADE DOUBLE CROWN SOLUTIONS
                || orgId == 3693 //Island Computer College
                || orgId == 11837 // Borsh Ventures
                || orgId == 11577 // Coscharlom Intl Associates Ltd
                || orgId == 11603 // INNOFLEX CYBER ENT
                || orgId == 11847 // Abbegold ICT Solutions
                || orgId == 10203 // Binet Resources
                || orgId == 11951 // Rebras Tech Limited
                || orgId == 12041 // Rachma Concept
                || orgId == 12045 // MR. Abusufiyan Sa'ad
                || orgId == 11849 // Repsunny Integrated Services Ltd
                || orgId == 12053 // Darphilz Links Ventures Limited
                || orgId == 12073 // V-dile Solutions
                || orgId == 12109 // Arktronics Travels and Tours Limited
                || orgId == 12141 // NANO NG MULTI SOLUTIONS LTD
                || orgId == 11847 // Abbegold ICT Solutions
                )) {
            // New ICP Contract
            if (!paymentMethod.isEmpty() && !paymentMethod.equalsIgnoreCase("Credit Note") && !channel.startsWith("26P") && !channel.startsWith("27P") && !itemNumber.startsWith("BUN")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }
            int simKitCount = 0;
            int deviceCount = 0;

            for (int i = 0; i < sale.getSaleLines().size(); i++) {
                com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                String it = otherLine.getInventoryItem().getItemNumber();
                if (simKits.indexOf(it) != -1) {
                    simKitCount += otherLine.getQuantity();
                } else if (deviceKits.indexOf(it) != -1) {
                    deviceCount += otherLine.getQuantity();
                }
            }

            if (simKits.indexOf(itemNumber) != -1) {
                if (simKitCount >= 6 && simKitCount <= 14) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(8d);
                } else if (simKitCount >= 15 && simKitCount <= 50) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                } else if (simKitCount >= 51 && simKitCount <= 100) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(12d);
                } else if (simKitCount >= 101) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(15d);
                }
            }

            if (deviceKits.indexOf(itemNumber) != -1) {
                if (deviceCount >= 6 && deviceCount <= 14) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(8d);
                } else if (deviceCount >= 15 && deviceCount <= 50) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                } else if (deviceCount >= 51 && deviceCount <= 100) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(12d);
                } else if (deviceCount >= 101) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(15d);
                }
            }

            if (isAirtime) {
                double discTier1 = 1d / (1d / 0.05d + 1d);
                double discTier2 = 1d / (1d / 0.055d + 1d);
                double discTier3 = 1d / (1d / 0.06d + 1d);
                double discTier4 = 1d / (1d / 0.07d + 1d);

                double tier1Start = 100000d / (1d - discTier1);
                double tier2Start = 250000d / (1d - discTier2);
                double tier3Start = 500000d / (1d - discTier3);
                double tier4Start = 1000000d / (1d - discTier4);

                long airtimeUnits = saleLine.getQuantity();
                if (airtimeUnits >= tier1Start && airtimeUnits < tier2Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier1 * 100d);
                } else if (airtimeUnits >= tier2Start && airtimeUnits < tier3Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier2 * 100d);
                } else if (airtimeUnits >= tier3Start && airtimeUnits < tier4Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier3 * 100d);
                } else if (airtimeUnits >= tier4Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier4 * 100d);
                }
            }

        }

        // TPGW Partner Airtime Discount Setup
        if ((orgId == 6409 // Onecard
                || orgId == 7329 // Contec
                || orgId == 9469 // CitiServe
                || orgId == 9483 // HomeKonnekt Limited
                || orgId == 7511 // Cellulant Nigeria Limited
                || orgId == 9603 // Zinternet Nig Ltd
                || orgId == 9549 // Payconnect Networks Ltd 
                || orgId == 8649 // Golden Rule Communications Limited	
                || orgId == 10617 // TRUSTVAS LIMITED
                || orgId == 5985 // ICP Capricorn Digital
                || orgId == 11609 // ITEX INTEGRATED SERVICES LIMITED
                ) && saleLine.getQuantity() >= 500000 && isAirtime) {

            if (!paymentMethod.isEmpty() && !paymentMethod.equalsIgnoreCase("Credit Note") && !channel.startsWith("26P") && !channel.startsWith("27P")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }
            double discTier3 = 1d / (1d / 0.08d + 1d);
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(discTier3 * 100d);
        }

        return dd;
    }

    public com.smilecoms.pos.DiscountData getDynamicDiscountDataUG(com.smilecoms.xml.schema.pos.Sale sale, com.smilecoms.xml.schema.pos.SaleLine saleLine, double exRate, boolean isSubItem) throws Exception {
        String itemNumber = saleLine.getInventoryItem().getItemNumber();
        String serialNumber = saleLine.getInventoryItem().getSerialNumber();
        boolean isAirtime = serialNumber.equals("AIRTIME");
        boolean isKit = itemNumber.startsWith("KIT");
        String promoCode = sale.getPromotionCode();
        String paymentMethod = sale.getPaymentMethod();
        String ccNumber = sale.getCreditAccountNumber();
        int salesPersonId = sale.getSalesPersonCustomerId();
        long recipientAccountId = sale.getRecipientAccountId();
        boolean inQuote = (paymentMethod == null || paymentMethod.isEmpty());
        String channel = sale.getChannel();
        com.smilecoms.pos.DiscountData dd = new com.smilecoms.pos.DiscountData();
        int orgId = sale.getRecipientOrganisationId();
        java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
        if (paymentMethod == null) {
            paymentMethod = "";
        }
        if (ccNumber == null) {
            ccNumber = "";
        }

// if((itemNumber.equalsIgnoreCase("KIT9057") || itemNumber.equalsIgnoreCase("KIT9058")) && !(salesPersonId == 17042)) {
        //       throw new Exception("KIT9058/KIT9057 is not available for selling by sales person " + salesPersonId );	
// }
        if (itemNumber.startsWith("BUNTN")) {
            if (!paymentMethod.equals("") && !paymentMethod.equals("Credit Account")) {
                throw new Exception("Staff bundles must be paid by credit account");
            }
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (!itemNumber.startsWith("BUN") && !itemNumber.startsWith("BENTBET") && saleLine.getInventoryItem().getPriceInCentsIncl() == 0d && !paymentMethod.isEmpty() && !paymentMethod.equals("Credit Note")) {
            throw new Exception("A sale with a zero item price is not allowed!");
        }

        if (itemNumber.startsWith("BUNST")) {
            if (!paymentMethod.equals("") && !paymentMethod.equals("Credit Account")) {
                throw new Exception("Staff bundles must be paid by credit account");
            }
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.startsWith("BUNP")) {
            if (!paymentMethod.equals("") && !paymentMethod.equals("Cash")) {
                throw new Exception("Gift bundles must be paid by cash");
            }
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (!isSubItem && !paymentMethod.isEmpty() && !paymentMethod.equals("Staff") && !paymentMethod.equals("Loan") && !paymentMethod.equals("Credit Note") && itemNumber.startsWith("SIM") && promoCode.isEmpty()) {
            throw new Exception("A standalone SIM can only be sold using a SIMSwap promotion code. SIMs sold with SIMSwap promotion codes can only be used in a SIM Swap");
        }

        // 100% discount on routers and sims and dongles for testing
        if (promoCode.equalsIgnoreCase("TestingDiscount")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BENTBE005") || itemNumber.equals("BENTBE010") || itemNumber.equals("BENTBE020")) {
            if (saleLine.getQuantity() >= 24) {
                dd.setDiscountPercentageOff(10d);
            } else if (saleLine.getQuantity() >= 12) {
                dd.setDiscountPercentageOff(5d);
            }
        }
        // Percentage based discounts for enterprise
        if (itemNumber.startsWith("BENT") && promoCode.contains("% Discount on Corporate Access")) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + discPercent);
            dd.setTieredPricingPercentageOff(0d);
        }

        if (dd.getDiscountPercentageOff() > 0.0d) {
            return dd;
        }

        // Hotspots are allowed loans
        if (paymentMethod.equals("Loan")) {
            if (promoCode.equalsIgnoreCase("HotSpot")) {
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(0d);
                return dd;
            }
            throw new Exception("Loan Payment methods not allowed at this point!");
        }

        if (!paymentMethod.isEmpty() && !paymentMethod.equals("Loan") && promoCode.equalsIgnoreCase("HotSpot")) {
            throw new Exception("Hotspots must be loaned");
        }
        if (!inQuote && !ccNumber.equals("CSI002") && (promoCode.equalsIgnoreCase("Education") || promoCode.equalsIgnoreCase("Community"))) {
            throw new Exception("CSI Promotions must be on the CSI credit account");
        }
        // 100% discount for invoices generated in SCA for bundles
        if (!isAirtime && promoCode.equalsIgnoreCase("UCGiveAway")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on sims for faulty sim
        if (itemNumber.startsWith("SIM") && promoCode.equalsIgnoreCase("FreeSIMSwap")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims for educational institutions
        if (!isAirtime && promoCode.equalsIgnoreCase("Education")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims for brand ambassadors
        if (!isAirtime && promoCode.equalsIgnoreCase("BrandAmbassador")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims for communities 
        if (!isAirtime && promoCode.equalsIgnoreCase("Community")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims and dongles for gifts and give aways
        if (!isAirtime && promoCode.equalsIgnoreCase("GiveAways")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Percentage based volume breaks
        if (!isAirtime && promoCode.contains("% Volume Break")) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(discPercent);
            return dd;
        }

        // Percentage based discounts
        if (!isAirtime && promoCode.contains("% Discount")) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(discPercent);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Airtime bulk discounting for third parties
        if (isAirtime) {
            // AVS - 8.0% Discount
            if (recipientAccountId == 1209000010) {
                dd.setDiscountPercentageOff(8.0d);
                dd.setTieredPricingPercentageOff(0d);
                return dd;
            }
            // Cellulant - 6.0% Discount
            if (recipientAccountId == 1805000164 && saleLine.getQuantity() >= 9000000) {
                dd.setDiscountPercentageOff(6.0d);
                dd.setTieredPricingPercentageOff(0d);
                return dd;
            }
        }

        if (orgId == 1579
                || orgId == 5
                || orgId == 4738
                || orgId == 4867
                || orgId == 4877
                || orgId == 4893
                || orgId == 5566
                || orgId == 5573) {

            if (!paymentMethod.isEmpty() && !channel.startsWith("26P")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P");
            }

            if (isAirtime) {
                dd.setDiscountPercentageOff(8d);
                dd.setTieredPricingPercentageOff(0d);
                return dd;
            }
            if (isKit) {
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(10d);
                return dd;
            }
        }

        if (itemNumber.equals("BUNIC1005")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.indexOf("KIT9058") != -1) {
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(24.39d);
        } else if (itemNumber.indexOf("KIT9060") != -1) {
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(24.39d);
        } else if (itemNumber.indexOf("KIT9055") != -1) {
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(14.04d);
        } else if (itemNumber.indexOf("KIT9048") != -1) {
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(10d);
        } else if (itemNumber.indexOf("KIT9063") != -1 || itemNumber.indexOf("KIT9062") != -1 || itemNumber.indexOf("KIT9064") != -1) {
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(24.3902d);
        } else if (itemNumber.indexOf("KIT9061") != -1) {
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(14.0401d);
        } else if (itemNumber.indexOf("KIT9065") != -1) {
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(10d);
        }

        String simKits = "KIT9011 KIT9012 KIT9013 KIT9017 KIT9018 KIT9019 KIT9028 ";
        String deviceKits = "KIT9029 KIT9003 KIT9007 KIT9010 KIT9002 KIT9016 KIT9001 KIT9006 KIT9000 KIT9005 KIT9015 KIT9004 KIT9008 KIT9009 KIT9014 KIT9020 KIT9021 KIT9023 KIT9027 KIT9022 KIT9024 KIT9035 KIT9025 KIT9042 KIT9043 KIT9045 KIT9046 KIT9047  KIT9048 KIT9049 KIT9050 KIT9051 KIT9052 KIT9057 KIT9058 KIT9055 KIT9054 KIT9056 KIT9053 KIT9047";

        if ((itemNumber.equalsIgnoreCase("KIT9061")
                || itemNumber.equalsIgnoreCase("KIT9062")
                || itemNumber.equalsIgnoreCase("KIT9063")
                || itemNumber.equalsIgnoreCase("KIT9064")
                || itemNumber.equalsIgnoreCase("KIT9065")
                || itemNumber.equalsIgnoreCase("KIT9066")
                || itemNumber.equalsIgnoreCase("KIT9067")
                || itemNumber.equalsIgnoreCase("KIT9068")) && salesPersonId != 7) {
            throw new Exception("KIT is not available for selling by sales person " + salesPersonId);
        }

        // For ICP Commission Structure Version 2: 
        if (orgId == 11283 //Airtel Communications 
                || orgId == 135262 //Equinox Communication Centre Limited 
                || orgId == 14217 //Musamoh (U) Limited 
                || orgId == 14509 //Media Tele Centre Communications Ltd  
                || orgId == 14779 //Skylim Limited 
                || orgId == 14049 // Mindset Solutions Limited 
                || orgId == 14153 //Mega Networks Limited 
                || orgId == 14231 //Zulu Enterprises (U) Limited 
                ) {

            if (itemNumber.indexOf("KIT9058") != -1) {
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(24.39d);
            } else if (itemNumber.indexOf("KIT9060") != -1) {
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(24.39d);
            } else if (itemNumber.indexOf("KIT9055") != -1) {
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(14.04d);
            } else if (itemNumber.indexOf("KIT9048") != -1) {
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(10d);
            }

            //For Airtime??
            if (isAirtime) {
                double discTier1 = 1d / (1d / 0.05d + 1d);
                double discTier2 = 1d / (1d / 0.055d + 1d);
                double discTier3 = 1d / (1d / 0.06d + 1d);

                double tier1Start = 1500000d / (1d - discTier1);
                double tier2Start = 4500000d / (1d - discTier2);
                double tier3Start = 9000000d / (1d - discTier3);

                long airtimeUnits = saleLine.getQuantity();
                if (airtimeUnits >= tier1Start && airtimeUnits < tier2Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier1 * 100d);
                } else if (airtimeUnits >= tier2Start && airtimeUnits < tier3Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier2 * 100d);
                } else if (airtimeUnits >= tier3Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier3 * 100d);
                }
            }

            return dd;
        }

        if (orgId == 4896 // Fono Zone
                || orgId == 4869 // Maruti Office Supplies
                || orgId == 5569 // Kapish
                || orgId == 7992 // Aletheia
                || orgId == 328 // MEC
                || orgId == 9641 // JuiceNet
                || (orgId == 826 && recipientAccountId == 1503000404) // VAS Garage
                || orgId == 10229 // Padmac
                || orgId == 9885 // Nimar
                || orgId == 10543 // Manyana
                || orgId == 9765 // Wadi
                || orgId == 10229 // Padmac
                || orgId == 10939 // Trend systems East Africa Limited
                || orgId == 11297 // Ahuriire (Uganda) Ltd
                || orgId == 11283 // Airtel Communications Ltd
                || orgId == 11309 // Brandman
                || orgId == 11377 // Pelsat Investments
                || orgId == 11387 // Ninena Limited
                || orgId == 11365 // Dondolo
                || orgId == 11417 // Masaka Digital Connections
                || orgId == 11789 // Mama Bears Services
                || orgId == 9441 // Fonexpress Uganda Limited
                || orgId == 11911 // Regiline International Limited
                || orgId == 11923 // Deno Telecom Limited
                || orgId == 11581 // Geses Uganda
                || orgId == 12145 // Hotel Aribus
                || orgId == 12239 // Pebuu Limited
                || orgId == 11511 // Novoplat Ltd
                || orgId == 11939 // Homeland Solutions Limited
                || orgId == 12443 // TWI Distribution ltd
                || orgId == 12523 // Noclinks
                || orgId == 12465 // Liquid Silk
                || orgId == 13843 // True African (U) Limited
                || orgId == 12615 // Wireless Engeneering Based Technologies
                || orgId == 12641 // Interswitch
                || orgId == 12835 // Desires Choice
                || orgId == 13039 // Odyssey Communications (U) Limited
                || orgId == 12851 // Red Core Interactive Limited
                || orgId == 13251 // Payline
                || orgId == 13249 // Light Investments and Contractors Limited
                || orgId == 12701 // One Global
                || orgId == 13473 // ChapChap
                || orgId == 13603 // Prepay Nation LLC
                || orgId == 13587 // SwipeIT
                || orgId == 13699 // Spurion
                || orgId == 7487 // Mr. Garget(1)
                || orgId == 13777 // TRNItel Limited
                || orgId == 13959 // V4 Communications Ltd
                || orgId == 13947 // Pegasus Technologies Ltd
                || orgId == 14023 // Star Telecommunications Kabale
                || orgId == 13973 // Local Links Limited
                || orgId == 14049 // Mindset Solutions Limited
                || orgId == 14045 // Agiza Uganda Limited
                || orgId == 14047 // Tannex Limited
                || orgId == 14153 // Mega Networks Limited
                || orgId == 14075 // Africa Tours and Travel
                || orgId == 14163 // A.K. Communications Limited
                || orgId == 14051 // Sri Krishna Chattanya Mission Ltd
                || orgId == 14201 // Vintech Phone Service Limited
                || orgId == 14217 // Musamoh (U) Limited
                || orgId == 14199 // Pacific Computers 
                || orgId == 14211 // Icon Marks Limited 
                || orgId == 14227 // Shreenath Store Limited 
                || orgId == 14231 // Zulu Enterprises (U) Limited
                || orgId == 14225 // Ocean One Enterprises
                || orgId == 14253 // Phone Director Enterprise
                || orgId == 14401 // Kigs Electronic Center
                || orgId == 14405 // Rehim Networks Limited
                || orgId == 14483 // Mutyo and Sons Investments Limited
                || orgId == 14779 // Skylim Limited
                || orgId == 14909 // DSJ Multiple Ventures Limited
                || orgId == 14907 // Deluxe Investments Limited 
                || orgId == 14783 // Equinox Communication Centre Limited
                || orgId == 15225 // Bunyonyi Distributors Limited
                || orgId == 9359 //  Comfort Trade Uganda Limited
                || orgId == 9367 // ICP  testing
                ) {
            // New ICP Contract
            if (!paymentMethod.isEmpty() && !channel.startsWith("26P")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P");
            }

            int simKitCount = 0;
            int deviceCount = 0;

            for (int i = 0; i < sale.getSaleLines().size(); i++) {
                com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                String it = otherLine.getInventoryItem().getItemNumber();
                if (simKits.indexOf(it) != -1) {
                    simKitCount += otherLine.getQuantity();
                } else if (deviceKits.indexOf(it) != -1) {
                    deviceCount += otherLine.getQuantity();
                }
            }

            boolean inFirst3Months = false;
            if ((orgId == 13699 && new java.util.Date().before(dateFormat.parse("2017-09-06")))
                    || (orgId == 13603 && new java.util.Date().before(dateFormat.parse("2017-07-04")))
                    || (orgId == 13587 && new java.util.Date().before(dateFormat.parse("2017-07-24")))) {
                inFirst3Months = true;
            }

            if (simKits.indexOf(itemNumber) != -1) {
                if (inFirst3Months == true) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                } else if (simKitCount >= 6 && simKitCount <= 14) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(8d);
                } else if (simKitCount >= 15) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                }
            }

            if (deviceKits.indexOf(itemNumber) != -1) {
                if (inFirst3Months == true) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                } else if (deviceCount >= 6 && deviceCount <= 14) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(8d);
                } else if (deviceCount >= 15) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                }
            }

            if (isAirtime) {
                double discTier1 = 1d / (1d / 0.05d + 1d);
                double discTier2 = 1d / (1d / 0.055d + 1d);
                double discTier3 = 1d / (1d / 0.06d + 1d);

                double tier1Start = 1500000d / (1d - discTier1);
                double tier2Start = 4500000d / (1d - discTier2);
                double tier3Start = 9000000d / (1d - discTier3);

                long airtimeUnits = saleLine.getQuantity();
                if (airtimeUnits >= tier1Start && airtimeUnits < tier2Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier1 * 100d);
                } else if (airtimeUnits >= tier2Start && airtimeUnits < tier3Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier2 * 100d);
                } else if (airtimeUnits >= tier3Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier3 * 100d);
                }
            }

        }

        if ((orgId == 9663 || orgId == 9713 || (orgId == 826 && recipientAccountId == 1502000471)) && saleLine.getQuantity() >= 9000000 && isAirtime) { // EzeeMoney, MCash, VAS Garage

            if (!paymentMethod.isEmpty() && !channel.startsWith("26P")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P");
            }
            double discTier3 = 1d / (1d / 0.08d + 1d);
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(discTier3 * 100d);
        }

        return dd;
    }

    // Super Dealer and ICP config for new model
// This is used by SEP and POS to know which Orgs are Super Dealers and ICPs
// <Org Id> ACC: <Commission account> DISC: <Discount %> TYPE: <SD or ICP> <Name>
// Franchises
// 10077 ACC: 1708003338 DISC: 7% TYPE: FRA Onigx Investment ltd
// 9723 ACC: 1705007543 DISC: 7% TYPE: FRA Rezio Limited
// 5059 ACC: 1503001021 DISC: 7% TYPE: FRA Callus Miller Company Limited
// 8909 ACC: 1708012011 DISC: 7% TYPE: FRA Code2take Ventures
// 2888 ACC: 1709004332 DISC: 7% TYPE: FRA Pheg Marathon Ventures
// Kaduna ICPs (used to be treated as Super Dealers)
// 6179 ACC: 1507002203 DISC: 8% TYPE: ICP Mo Concepts Co. LTD
// 9631 ACC: 1704006564 DISC: 8% TYPE: ICP Crysolyte Networks
// 7163 ACC: 1601005343 DISC: 8% TYPE: ICP Bloom Associates
// 9649 ACC: 1704009598 DISC: 8% TYPE: ICP NM MARGI NIG LTD
// 6929 ACC: 1602002373 DISC: 8% TYPE: ICP Micromanna Ltd.
// 2652 ACC: 1612003434 DISC: 8% TYPE: ICP Ditrezi Enterprises
// 5479 ACC: 1503002012 DISC: 8% TYPE: ICP ImageAddiction
// 6555 ACC: 1509005251 DISC: 8% TYPE: ICP Veemobile services
// 6643 ACC: 1510002993 DISC: 8% TYPE: ICP Dahrah Global Limited
// 6993 ACC: 1512006591 DISC: 8% TYPE: ICP Kima-Maxi Nigeria Limited
// 6997 ACC: 1512006592 DISC: 8% TYPE: ICP Value Plus Service & Industries L
// 7009 ACC: 1512006586 DISC: 8% TYPE: ICP Detoyibo Enterprises
// 7021 ACC: 1512006589 DISC: 8% TYPE: ICP Toluque Nigeria Limited
// 7045 ACC: 1512007980 DISC: 8% TYPE: ICP Payit Consulting Limited
// 7417 ACC: 1602003768 DISC: 8% TYPE: ICP Synergy Mobile Business Solutions Limited
// 7431 ACC: 1602004047 DISC: 8% TYPE: ICP Dpro Project Limited
// 7843 ACC: 1604007384 DISC: 8% TYPE: ICP SAANET GLOBAL SERVICES LIMITED
// 8283 ACC: 1705003619 DISC: 8% TYPE: ICP Digital Development Hub
// 9539 ACC: 1703001193 DISC: 8% TYPE: ICP Intergrity Communications
// 9783 ACC: 1705015937 DISC: 8% TYPE: ICP 23RD Century Technologies LTD
// 9815 ACC: 1706008776 DISC: 8% TYPE: ICP GENIALLINKS SERVICES
// 9825 ACC: 1706008774 DISC: 8% TYPE: ICP TIMBOY VENTURES
// 9831 ACC: 1706012063 DISC: 8% TYPE: ICP Universal Asset Commercial Boker
// 9941 ACC: 1707002065 DISC: 8% TYPE: ICP Manjub Limited
// 10025 ACC: 1707008135 DISC: 8% TYPE: ICP Olans Communications
// 10075 ACC: 1708003269 DISC: 8% TYPE: ICP Ultimate Aslan Limited
// 10019 ACC: 1708002311 DISC: 8% TYPE: ICP White house Computers Ltd
// 10013 ACC: 1708002310 DISC: 8% TYPE: ICP Speedy IDeas Synergy Nig Ltd
// 10181 ACC: 1708012163 DISC: 8% TYPE: ICP ALAJIM Limited
// 10123 ACC: 1708007593 DISC: 8% TYPE: ICP MJ and partners power Electric
// 10325 ACC: 1709003981 DISC: 8% TYPE: ICP Beyond Integrated Services
// 10467 ACC: 1709009459 DISC: 8% TYPE: ICP ABSON Bussiness World LTD
// 7057 ACC: 1512008310 DISC: 11% TYPE: SD Sonite Communications Limited
// 6633 ACC: 1511001587 DISC: 10% TYPE: SD Lyds Louisa Intergrated Company Limited
// 8623 ACC: 1607009915 DISC: 10% TYPE: SD ZEPH ASSOCIATES NIGERIA LIMITED
// 8641 ACC: 1608000342 DISC: 10% TYPE: SD Idems First Network Limited
// 8639 ACC: 1607011274 DISC: 10% TYPE: SD Gabros Divine Technologies Limited
// 8649 ACC: 1608000265 DISC: 10% TYPE: SD CANT STOP NIGERIA LIMITED
// 8649 ACC: 1710003339 DISC: 10% TYPE: SD  Golden Rule Communications Ltd
// 8649 ACC: 1710003342 DISC: 10% TYPE: SD  Golden Rule Communications Ltd
// 9109 ACC: 1610001302 DISC: 10% TYPE: SD Concise Services Limited
// 2643 ACC: 1312000182 DISC: 10% TYPE: SD Timjat Global Resources Ltd
// 9615 ACC: 1704001876 DISC: 10% TYPE: SD Digital Development Hub Consultancy Services Ltd
// 9647 ACC: 1704009231 DISC: 10% TYPE: SD YFree SD
// 10839 ACC: 1712002385 DISC: 10% TYPE: SD  Lustre Communications ltd
// 12509 ACC: 1811003908 DISC: 10% TYPE: SD Communication Facilitators Ltd
// 11091 ACC: 1811002217 DISC: 10% TYPE: SD Correspondence Limited
// 9801 ACC: 1811011721 DISC: 10% TYPE: SD Godmic Nigeria Limited
// 3 ACC: 1711010802 DISC: 8% TYPE: ICP Helpline Telecoms Nig. LTD
// 10303 ACC: 1709008032 DISC: 8% TYPE: ICP PC Corner Services Limited
// 10221 ACC: 1709001037 DISC: 8% TYPE: ICP Profast Integrated Services Limited
// 10215 ACC: 1709000966 DISC: 8% TYPE: ICP Rahmatob Computer Concept
// 10219 ACC: 1709001017 DISC: 8% TYPE: ICP Kastech Technologies
// 10179 ACC: 1708011903 DISC: 8% TYPE: ICP Popsicles Enterprises 
// 10109 ACC: 1708006078 DISC: 8% TYPE: ICP Ahijo Ventures limited
// 10163 ACC: 1708011094 DISC: 8% TYPE: ICP Option 360 Communications
// 10159 ACC: 1708010209 DISC: 8% TYPE: ICP Poatech Enterprises Nigeria Ltd
// 10151 ACC: 1708009500 DISC: 8% TYPE: ICP TICEE CONNECT
// 10125 ACC: 1708007797 DISC: 8% TYPE: ICP Hona Ara Nigeria Ltd
// 10115 ACC: 1708006791 DISC: 8% TYPE: ICP Dolamif Nigeria Limited
// 10085 ACC: 1708004284 DISC: 8% TYPE: ICP Global Communication and Digital services
// 10077 ACC: 1708003338 DISC: 8% TYPE: ICP Onigx Investment Limited
// 10079 ACC: 1708003426 DISC: 8% TYPE: ICP LTC network limited
// 10059 ACC: 1708001482 DISC: 8% TYPE: ICP Glitteringway enterprise
// 10009 ACC: 1707007946 DISC: 8% TYPE: ICP Asrad Multi-links Business and logistic limited
// 10001 ACC: 1707006592 DISC: 8% TYPE: ICP BlueBreed Enterprise
// 10025 ACC: 1707008135 DISC: 8% TYPE: ICP Olans Communication
// 10041 ACC: 1707010667 DISC: 8% TYPE: ICP Base 15 Concept Nigeria Limited
// 10171 ACC: 1708011045 DISC: 8% TYPE: ICP Pesuzok Nig Ltd
// 10429 ACC: 1709009699 DISC: 8% TYPE: ICP PC GABNET TECHNOLOGY
// 10427 ACC: 1709009697 DISC: 8% TYPE: ICP LATEETOY NIGERIA LIMITED
// 7921 ACC: 1605003822 DISC: 8% TYPE: ICP Tidake global limited
// 9609 ACC: 1710001286 DISC: 8% TYPE: ICP Lexton Energy
// 9731 ACC: 1708010168 DISC: 8% TYPE: ICP Belledave Nigeria Limited
// 9571 ACC: 1707010662 DISC: 8% TYPE: ICP Cyberdyne Integrated Limited
// 7431 ACC: 1602004047 DISC: 8% TYPE: ICP Dpro Project
// 9885 ACC: 1707001965 DISC: 8% TYPE: ICP Happymood enterprise
// 9975 ACC: 1707005327 DISC: 8% TYPE: ICP Godsaves devices Tech Ltd
// 9945 ACC: 1707008942 DISC: 8% TYPE: ICP Aumak-mikash Ltd
// 9999 ACC: 1707007126 DISC: 8% TYPE: ICP Aje Global Multiservices
// 9963 ACC: 1707004296 DISC: 8% TYPE: ICP FEM9 TOUCH LIMITED
// 9973 ACC: 1707005483 DISC: 8% TYPE: ICP Joelink Global Concept communication Ltd
// 9967 ACC: 1707005338 DISC: 8% TYPE: ICP Goldenworths West Africa LTD
// 5488 ACC: 1501003415 DISC: 8% TYPE: ICP Blessing Computers
// 5519 ACC: 1502000278 DISC: 8% TYPE: ICP Burj Khalifah Nigeria Limited
// 2652 ACC: 1612003434 DISC: 8% TYPE: ICP Ditrezi Enterprises
// 9947 ACC: 1707003031 DISC: 8% TYPE: ICP DANDOLAS NIGERIA LIMITED
// 9941 ACC: 1707002065 DISC: 8% TYPE: ICP Manjub Limited
// 9939 ACC: 1707002036 DISC: 8% TYPE: ICP Tecboom Digital Ltd
// 9933 ACC: 1707001131 DISC: 8% TYPE: ICP Da-Costa F.A and Company
// 9929 ACC: 1707000869 DISC: 8% TYPE: ICP Jb Mwonyi Company Limited
// 9883 ACC: 1706014165 DISC: 8% TYPE: ICP Oroborkso Ventures Nigeria Limited
// 9875 ACC: 1706014128 DISC: 8% TYPE: ICP Chrisoduwa Global Ventures
// 9887 ACC: 1706014329 DISC: 8% TYPE: ICP Multitronics Global Enterprise
// 9861 ACC: 1706014118 DISC: 8% TYPE: ICP DRUCICARE NIG LTD
// 9881 ACC: 1706014150 DISC: 8% TYPE: ICP Beejao Infosolutions Limited
// 9877 ACC: 1706014141 DISC: 8% TYPE: ICP Sam Fransco Resources
// 9873 ACC: 1706014121 DISC: 8% TYPE: ICP GLORIOUS TELECOMM
// 9867 ACC: 1707000906 DISC: 8% TYPE: ICP AMIN AUTO LTD
// 9931 ACC: 1707000923 DISC: 8% TYPE: ICP Boxtech Consult
// 9649 ACC: 1704009598 DISC: 8% TYPE: ICP NM MARGI NIG LTD
// 9897 ACC: 1707000781 DISC: 8% TYPE: ICP Brownnys-Connect Nigeria Limited
// 9835 ACC: 1706010115 DISC: 8% TYPE: ICP K-CEE DEVICES LIMITED
// 9859 ACC: 1706014143 DISC: 8% TYPE: ICP Budddybod Enterprises
// 9863 ACC: 1706013579 DISC: 8% TYPE: ICP GOLDENPEECOMPUTERS TECHNOLOGIES
// 9831 ACC: 1706012063 DISC: 8% TYPE: ICP Universal Asset Commercial Boker
// 9847 ACC: 1706011240 DISC: 8% TYPE: ICP Fundripples Limited
// 9841 ACC: 1706011245 DISC: 8% TYPE: ICP Sharptowers Limited
// 9549 ACC: 1703001162 DISC: 8% TYPE: ICP Payconnect Networks Ltd
// 9827 ACC: 1706009025 DISC: 8% TYPE: ICP Leverage Options
// 9793 ACC: 1706008830 DISC: 8% TYPE: ICP Sunsole Global Investment Ltd
// 9801 ACC: 1706008832 DISC: 8% TYPE: ICP Godmic Nigeria LTD
// 9825 ACC: 1706008774 DISC: 8% TYPE: ICP Timboy Ventures
// 9815 ACC: 1706008776 DISC: 8% TYPE: ICP Geniallinks Services
// 9783 ACC: 1705015937 DISC: 8% TYPE: ICP 23RD Century Technologies LTD
// 9755 ACC: 1705010786 DISC: 8% TYPE: ICP Abiola Gold Resources
// 9753 ACC: 1705010787 DISC: 8% TYPE: ICP Hetoch Technology
// 9747 ACC: 1705009480 DISC: 8% TYPE: ICP Mac Onella
// 9711 ACC: 1705006481 DISC: 8% TYPE: ICP Finet Communications Ltd
// 9723 ACC: 1705007543 DISC: 8% TYPE: ICP Rezio Limited
// 6179 ACC: 1507002203 DISC: 8% TYPE: ICP MO Concepts co. Ltd
// 8283 ACC: 1705003619 DISC: 8% TYPE: ICP Digital Development Hub
// 5985 ACC: 1506002422 DISC: 8% TYPE: ICP Capricorn Digital
// 9657 ACC: 1704011368 DISC: 8% TYPE: ICP Spacesignal Ltd
// 7219 ACC: 1601008476 DISC: 8% TYPE: ICP TYJPEE Global Service
// 5598 ACC: 1502005940 DISC: 8% TYPE: ICP P N B Bytes Resources
// 9637 ACC: 1704007239 DISC: 8% TYPE: ICP Network Enterprises PH
// 9625 ACC: 1704006509 DISC: 8% TYPE: ICP Global Lightcrown Ventures
// 9631 ACC: 1704006564 DISC: 8% TYPE: ICP CRYSOLYTE NETWORKS
// 3143 ACC: 1402000481 DISC: 8% TYPE: ICP Koams Solutions
// 3675 ACC: 1406000144 DISC: 8% TYPE: ICP Trend Worlwide Ltd
// 3677 ACC: 1406000147 DISC: 8% TYPE: ICP Bankytech Comm Ventures
// 5500 ACC: 1501003476 DISC: 8% TYPE: ICP Spectrum Innovation Technologies
// 3412 ACC: 1406000145 DISC: 8% TYPE: ICP Galant Communications Ltd
// 2918 ACC: 1402000215 DISC: 8% TYPE: ICP Aghayedo Brother Enterprises
// 4222 ACC: 1408000539 DISC: 8% TYPE: ICP Barcutt Investment Ltd
// 5493 ACC: 1501003451 DISC: 8% TYPE: ICP Pinacle Wireless Ltd
// 5506 ACC: 1501004187 DISC: 8% TYPE: ICP Dot.Com
// 5713 ACC: 1504002740 DISC: 8% TYPE: ICP Recharge Hub
// 5715 ACC: 1504002778 DISC: 8% TYPE: ICP Suremobile Networks
// 4938 ACC: 1408001017 DISC: 8% TYPE: ICP Global Rolling Technologies
// 5833 ACC: 1505001221 DISC: 8% TYPE: ICP F.F.Tech Communication
// 6581 ACC: 1509006920 DISC: 8% TYPE: ICP ING Systems
// 6615 ACC: 1510001332 DISC: 8% TYPE: ICP Genie Infotech
// 6437 ACC: 1508005507 DISC: 8% TYPE: ICP New Sonshyne
// 7845 ACC: 1604007573 DISC: 8% TYPE: ICP Banc Solutions
// 8051 ACC: 1605005403 DISC: 8% TYPE: ICP Rulesixmobile
// 8185 ACC: 1606000715 DISC: 8% TYPE: ICP PayQuick
// 8191 ACC: 1606001008 DISC: 8% TYPE: ICP Kemasho
// 8107 ACC: 1606008018 DISC: 8% TYPE: ICP Cordiant Technology
// 2917 ACC: 1402000211 DISC: 8% TYPE: ICP Yfree Solutions
// 2888 ACC: 1402000629 DISC: 8% TYPE: ICP Pheg Marathon
// 6417 ACC: 1509000345 DISC: 8% TYPE: ICP Simcoe & Dalhousie
// 5921 ACC: 1505005109 DISC: 8% TYPE: ICP Oakmead Realtors
// 6307 ACC: 1508001986 DISC: 8% TYPE: ICP Brightgrab
// 5567 ACC: 1502003116 DISC: 8% TYPE: ICP Acmesynergy
// 5565 ACC: 1502002826 DISC: 8% TYPE: ICP Broad Projects
// 6523 ACC: 1509003263 DISC: 8% TYPE: ICP The Koptim Company
// 8361 ACC: 1606007627 DISC: 8% TYPE: ICP SmartDrive & Systems Limited
// 5680 ACC: 1504000784 DISC: 8% TYPE: ICP Trebkann Nig. Ltd
// 6311 ACC: 1508001134 DISC: 8% TYPE: ICP Donval Platinum
// 5074 ACC: 1410002018 DISC: 8% TYPE: ICP Globasure Technologies
// 6923 ACC: 1512002524 DISC: 8% TYPE: ICP LeeBroad Ventures
// 8511 ACC: 1607008247 DISC: 8% TYPE: ICP Nartel Ventures
// 5504 ACC: 1501004137 DISC: 8% TYPE: ICP Belle-Vista
// 5489 ACC: 1501003418 DISC: 8% TYPE: ICP Wanaga Nig Ltd
// 3413 ACC: 1406000156 DISC: 8% TYPE: ICP Wireles Link Integrated
// 4679 ACC: 1408000945 DISC: 8% TYPE: ICP Ideal Hospitality
// 6271 ACC: 1507005163 DISC: 8% TYPE: ICP Avensa Global 
// 6149 ACC: 1507001087 DISC: 8% TYPE: ICP Bailord Ltd
// 5837 ACC: 1505001228 DISC: 8% TYPE: ICP Cheroxy Nig Ltd
// 6013 ACC: 1506002217 DISC: 8% TYPE: ICP Viclyd Enterprise
// 5925 ACC: 1505005013 DISC: 8% TYPE: ICP Foiim Technology
// 6155 ACC: 1507001275 DISC: 8% TYPE: ICP Sweet Planet
// 6407 ACC: 1508004268 DISC: 8% TYPE: ICP Lumron Enterprise
// 6299 ACC: 1508000989 DISC: 8% TYPE: ICP Best Baxany
// 6827 ACC: 1511003671 DISC: 8% TYPE: ICP Top Coat Limited
// 6623 ACC: 1510001977 DISC: 8% TYPE: ICP Sovereign Gold
// 6607 ACC: 1510000847 DISC: 8% TYPE: ICP Kollash Business and Technologies
// 6547 ACC: 1509005008 DISC: 8% TYPE: ICP KVC E-Mart
// 6733 ACC: 1510006947 DISC: 8% TYPE: ICP Isladex Communications
// 6735 ACC: 1510007028 DISC: 8% TYPE: ICP Avro Group
// 6823 ACC: 1511003342 DISC: 8% TYPE: ICP Omovo Tv
// 6821 ACC: 1511003293 DISC: 8% TYPE: ICP Fibrepath Limited 
// 7149 ACC: 1506000291 DISC: 8% TYPE: ICP SDMA Networks Limited  
// 7145 ACC: 1601004566 DISC: 8% TYPE: ICP Morgenall Global Limited
// 7347 ACC: 1602001116 DISC: 8% TYPE: ICP Amarcelo Nig Ltd
// 7477 ACC: 1602006358 DISC: 8% TYPE: ICP ALTS Consult Services Limited
// 7851 ACC: 1604007898 DISC: 8% TYPE: ICP Next BT Nig Ltd
// 7791 ACC: 1604001542 DISC: 8% TYPE: ICP Divine Anchor Nig Ltd
// 7821 ACC: 1604005277 DISC: 8% TYPE: ICP Grinotech Nig Ltd
// 8215 ACC: 1606002044 DISC: 8% TYPE: ICP I-Cell Mobile
// 5490 ACC: 1501003431 DISC: 8% TYPE: ICP Primadas Investment Nig Ltd
// 5729 ACC: 1504003622 DISC: 8% TYPE: ICP OLIVE INTEGRATED SERVICES
// 5482 ACC: 1501003738 DISC: 8% TYPE: ICP ZIONELI LIMITED
// 5289 ACC: 1412000818 DISC: 8% TYPE: ICP KORENET TECHNOLOGIES LTD
// 5529 ACC: 1502000810 DISC: 8% TYPE: ICP Arctuals Concepts Limited
// 5473 ACC: 1501003735 DISC: 8% TYPE: ICP AZMAN CONCEPTS LIMITED
// 5487 ACC: 1501003736 DISC: 8% TYPE: ICP NORTH EAST GOLD
// 5474 ACC: 1501003737 DISC: 8% TYPE: ICP InfoTek Perspective Services Limited
// 5823 ACC: 1505000924 DISC: 8% TYPE: ICP MERC-OATON TECHNOLOGIES LIMITED
// 5555 ACC: 1502003979 DISC: 8% TYPE: ICP COMTRANET IT SOLUTIONS LTD
// 6747 ACC: 1510007607 DISC: 8% TYPE: ICP First Regional Solutions Limited
// 7295 ACC: 1601010317 DISC: 8% TYPE: ICP Commonwealth Technologies Ltd
// 5839 ACC: 1505001367 DISC: 8% TYPE: ICP PRISTINE PANACHE LIMITED
// 6555 ACC: 1509005251 DISC: 8% TYPE: ICP Veemobile services
// 5479 ACC: 1503002012 DISC: 8% TYPE: ICP ImageAddiction
// 6805 ACC: 1511002557 DISC: 8% TYPE: ICP Global Quid Ltd
// 6461 ACC: 1509001625 DISC: 8% TYPE: ICP Febby communication Nigeria limited
// 6423 ACC: 1508005012 DISC: 8% TYPE: ICP Wonder Worth Global Investment Ltd
// 5162 ACC: 1411000821 DISC: 8% TYPE: ICP Sainana Resources
// 5292 ACC: 1412000819 DISC: 8% TYPE: ICP Skyraph Intergrated Servicesu Limited
// 6541 ACC: 1509004144 DISC: 8% TYPE: ICP Mohim Security Services and Consultancy Ltd
// 7727 ACC: 1603010674 DISC: 8% TYPE: ICP NEOCORTEX INTERNATIONAL LTD
// 5577 ACC: 1502004789 DISC: 8% TYPE: ICP Jotech Computer Co. Ltd.
// 6369 ACC: 1508002662 DISC: 8% TYPE: ICP MCEDWARDS ENT.LTD
// 6599 ACC: 1510000393 DISC: 8% TYPE: ICP Phones and Computers One-Stop Center Limited
// 5472 ACC: 1501003213 DISC: 8% TYPE: ICP Onis Digital Ventures
// 7197 ACC: 1601007501 DISC: 8% TYPE: ICP Skenyo Global Services Limited
// 7771 ACC: 1604000388 DISC: 8% TYPE: ICP Zillion Technocom
// 6685 ACC: 1510004241 DISC: 8% TYPE: ICP Soofine Print Limited
// 6519 ACC: 1509003097 DISC: 8% TYPE: ICP  Rakameris Ventures  
// 5279 ACC: 1412000433 DISC: 8% TYPE: ICP TOBOWY NIGERIA LIMITED
// 5215 ACC: 1411001937 DISC: 8% TYPE: ICP CHRISTY-GEORGE
// 5056 ACC: 1411001346 DISC: 8% TYPE: ICP Everyday Group
// 5444 ACC: 1501002271 DISC: 8% TYPE: ICP Pearl Inter Facet Resources
// 5446 ACC: 1501002273 DISC: 8% TYPE: ICP Central Dynamics Ltd
// 5483 ACC: 1501003505 DISC: 8% TYPE: ICP 3CEE TECHNOLOGIES
// 5445 ACC: 1501002272 DISC: 8% TYPE: ICP Emmrose Technologies Ltd
// 5450 ACC: 1501002270 DISC: 8% TYPE: ICP SACEN PROJECTS LIMITED
// 5060 ACC: 1501003504 DISC: 8% TYPE: ICP Integral Com (NIG) Ltd
// 5449 ACC: 1501003506 DISC: 8% TYPE: ICP Sayrex phone communication
// 5059 ACC: 1503001021 DISC: 8% TYPE: ICP Callus Miller Company Limited
// 5062 ACC: 1506003787 DISC: 8% TYPE: ICP Chuxel Computers
// 6181 ACC: 1507002247 DISC: 8% TYPE: ICP SHELL EAST STAFF INVESTMENT CO-OP SOCIETY LMT
// 6419 ACC: 1508004778 DISC: 8% TYPE: ICP SWING-HIGH COMMUNICATIONS LIMITED
// 6459 ACC: 1509001373 DISC: 8% TYPE: ICP CHARLYN NETWORKS LTD
// 6659 ACC: 1510003308 DISC: 8% TYPE: ICP YOMEEZ ELECTROWORLD LTD
// 7167 ACC: 1601005651 DISC: 8% TYPE: ICP DAKECH DYNAMIC SERVICES NIG LTD
// 7159 ACC: 1601005251 DISC: 8% TYPE: ICP MULTI RESOURCES AND INDUSTRIAL SERVICES NIG LTD
// 6769 ACC: 1602003001 DISC: 8% TYPE: ICP G-PAY INSTANT SOLUTIONS
// 7453 ACC: 1602005395 DISC: 8% TYPE: ICP Harry Concern Nigeria Enterprises
// 8819 ACC: 1608006679 DISC: 8% TYPE: ICP PINAAR UNIVERSAL LTD
// 6787 ACC: 1511003555 DISC: 8% TYPE: ICP Digital Direct
// 6819 ACC: 1511003971 DISC: 8% TYPE: ICP Xtraone Family Minstrel Ventures
// 6917 ACC: 1512002151 DISC: 8% TYPE: ICP Moben-D Excel Limited
// 6785 ACC: 1511003588 DISC: 8% TYPE: ICP Nofot Gardma Limited
// 5889 ACC: 1505003402 DISC: 8% TYPE: ICP Optimum Greeland Limited
// 6537 ACC: 1509003925 DISC: 8% TYPE: ICP D.S. Computer - Tech
// 6653 ACC: 1511001589 DISC: 8% TYPE: ICP Ambuilt works Ltd
// 6625 ACC: 1511001586 DISC: 8% TYPE: ICP Surfspot Communications Limited
// 6081 ACC: 1506005174 DISC: 8% TYPE: ICP Mesizanee Concept Limited.
// 5929 ACC: 1506000291 DISC: 8% TYPE: ICP SDMA Networks Limited
// 8903 ACC: 1609001070 DISC: 8% TYPE: ICP Emma Paradise Multilinks Limited
// 7089 ACC: 1601001377 DISC: 8% TYPE: ICP Madeeyak Nigeria Limited
// 8949 ACC: 1609002135 DISC: 8% TYPE: ICP Trevari
// 6771 ACC: 1511001721 DISC: 8% TYPE: ICP Easy Talil Enterprise
// 8983 ACC: 1609005821 DISC: 8% TYPE: ICP WATERGATE TELECOMMUNICATION
// 6657 ACC: 1511000486 DISC: 8% TYPE: ICP Rich Technologies Limited	
// 6745 ACC: 1511001590 DISC: 8% TYPE: ICP Joeche Computer Warehouse Limited	
// 6175 ACC: 1507001839 DISC: 8% TYPE: ICP Great Bakis Ventures Limited
// 9151 ACC: 1610006349 DISC: 8% TYPE: ICP Red and White Global Concept Ltd
// 8969 ACC: 1610010473 DISC: 8% TYPE: ICP Edge Baseline Solution
// 9173 ACC: 1610010961 DISC: 8% TYPE: ICP Riggsco Percific concept limited
// 9197 ACC: 1611000665 DISC: 8% TYPE: ICP Enbelo Company Limited
// 9221 ACC: 1611005581 DISC: 8% TYPE: ICP Ritech Network Enterprises
// 2630 ACC: 1310000420 DISC: 8% TYPE: ICP NETPOINT COMMUNICATIONS
// 2653 ACC: 1401000533 DISC: 8% TYPE: ICP ZACFRED SYSTEMS SOLUTIONS LTD
// 5539 ACC: 1502001765 DISC: 8% TYPE: ICP GLOBAL HANDSET LTD ( I WILL DISTRIBUTE ENT)
// 9039 ACC: 1609008911 DISC: 8% TYPE: ICP Hadiel Calidad Limited
// 7429 ACC: 1602004034 DISC: 8% TYPE: ICP Idea Konsult
// 6779 ACC: 1511001375 DISC: 8% TYPE: ICP The Gizmo world And Solution Limited
// 6179 ACC: 1507002203 DISC: 8% TYPE: ICP Mo Concepts Co. LTD
// 6861 ACC: 1507004510 DISC: 8% TYPE: ICP General B.Y. Supermarket
// 5631 ACC: 1503003181 DISC: 8% TYPE: ICP Ages Accurate Speed LTD
// 5534 ACC: 1502000967 DISC: 8% TYPE: ICP Yield Systems Limited
// 6779 ACC: 1511001375 DISC: 8% TYPE: ICP The Gizmo world And Solution Limited
// 7493 ACC: 1611006408 DISC: 8% TYPE: ICP Ski Current Service Limited
// 6289 ACC: 1508000009 DISC: 8% TYPE: ICP Final Logic
// 7491 ACC: 1602009235 DISC: 8% TYPE: ICP Emir Label
// 1361 ACC: 1503004497 DISC: 8% TYPE: ICP Onilu prime
// 7217 ACC: 1601008727 DISC: 8% TYPE: ICP REMGLORY VENTURES NIGERIA
// 8139 ACC: 1605007749 DISC: 8% TYPE: ICP Texmaco Ventures Nigeria Limited
// 7209 ACC: 1602004152 DISC: 8% TYPE: ICP U-MAT SUCCESS VENTURES
// 7697 ACC: 1603009927 DISC: 8% TYPE: ICP TUCAS GLOBAL RESOURCES
// 7215 ACC: 1601008729 DISC: 8% TYPE: ICP TOKAZ CONCEPT
// 9091 ACC: 1609010883 DISC: 8% TYPE: ICP Enatel Communications
// 7381 ACC: 1602002402 DISC: 8% TYPE: ICP POYEN NOMOVO NIG LTD
// 9255 ACC: 1611006221 DISC: 8% TYPE: ICP Brown Street Limited
// 9271 ACC: 1611008529 DISC: 8% TYPE: ICP Raz Resources
// 6565 ACC: 1509006328 DISC: 8% TYPE: ICP EMSI Enterprises Limited
// 5127 ACC: 1411000823 DISC: 8% TYPE: ICP Content Oasis limited
// 9293 ACC: 1612001954 DISC: 8% TYPE: ICP DA MORIS NIGERIA ENTERPRISES
// 5479 ACC: 1612008149 DISC: 8% TYPE: ICP ImageAddiction
// 9363 ACC: 1701000464 DISC: 8% TYPE: ICP Chuvexton Technovation Enterprises
// 9349 ACC: 1612007905 DISC: 8% TYPE: ICP Bama Global Concept Limited
// 9397 ACC: 1701003701 DISC: 8% TYPE: ICP Gold Vicky Communications Ltd
// 9405 ACC: 1701004853 DISC: 8% TYPE: ICP Double A Wireless
// 9413 ACC: 1701006201 DISC: 8% TYPE: ICP Darlin Ronkus Phones Ventures
// 9415 ACC: 1701006832 DISC: 8% TYPE: ICP Emmanuel Nelson Mobile Vetures
// 9451 ACC: 1701011176 DISC: 8% TYPE: ICP Temperature Limited
// 9473 ACC: 1702000782 DISC: 8% TYPE: ICP Leads Hotel Integrated Services
// 8909 ACC: 1609001970 DISC: 8% TYPE: ICP Code2take Ventures
// 9489 ACC: 1702001605 DISC: 8% TYPE: ICP Enhanced Payment Solutions
// 9463 ACC: 1702002043 DISC: 8% TYPE: ICP UNIQUE EVERMORE GLOBAL VENTURES
// 9511 ACC: 1702003409 DISC: 8% TYPE: ICP NPNG GLOBAL VENTURES
// 9521 ACC: 1702004999 DISC: 8% TYPE: ICP Demasworld Resource limited
// 9483 ACC: 1702001841 DISC: 8% TYPE: ICP Homekonnekt Limited
// 9543 ACC: 1702006254 DISC: 8% TYPE: ICP Padillax Technology Limited
// 9547 ACC: 1703000627 DISC: 8% TYPE: ICP Ficoven Investments Limited
// 9559 ACC: 1703002062 DISC: 8% TYPE: ICP Blu Edition International Limited
// 9553 ACC: 1703001554 DISC: 8% TYPE: ICP Joshfat Global Investment Ltd
// 9539 ACC: 1703001193 DISC: 8% TYPE: ICP Integrity Communications
// 9575 ACC: 1703005182 DISC: 8% TYPE: ICP Fabisek Investment Limited
// 9577 ACC: 1703005043 DISC: 8% TYPE: ICP Ironwill Business Services
// 9589 ACC: 1703006673 DISC: 8% TYPE: ICP E-Gate Technology Consults Ltd. ID
// 9601 ACC: 1703008371 DISC: 8% TYPE: ICP Chinwaks Global Nig Ltd
// 9595 ACC: 1703007903 DISC: 8% TYPE: ICP Liricom Nig Ltd
// 9605 ACC: 1703008748 DISC: 8% TYPE: ICP Peritus technologies limited
// 9599 ACC: 1703008749 DISC: 8% TYPE: ICP Golden eagles technology
// 9477 ACC: 1702001112 DISC: 8% TYPE: ICP Tribal Marks Media Productions Limited
// 9613 ACC: 1704001833 DISC: 8% TYPE: ICP Jegkobah Integrate Service Ltd
// 9629 ACC: 1704006245 DISC: 8% TYPE: ICP Diksolyn Concept Limited
// 9649 ACC: 1704009598 DISC: 8% TYPE: ICP NM Margi Nig Ltd
// 9695 ACC: 1705005014 DISC: 8% TYPE: ICP Easycomm global connection
// 9693 ACC: 1705005016 DISC: 8% TYPE: ICP Matuluko and son
// 8283 ACC: 1705003619 DISC: 8% TYPE: ICP Digital Development Hub
// 9797 ACC: 1706003164 DISC: 8% TYPE: ICP Cida-woods Limited
// 8909 ACC: 1609001970 DISC: 8% TYPE: ICP Code2take Ventures
// 10571 ACC: 1710006894 DISC: 8% TYPE: ICP  Pristine Stitches Enterprise 
// 10251 ACC: 1709001469 DISC: 8% TYPE: ICP Otomab and Sons Nigeria Limited
// 10323 ACC: 1709003979 DISC: 8% TYPE: ICP OPKISE GLOBAL CONCEPTS
// 10553 ACC: 1710004088 DISC: 8% TYPE: ICP Olive-Frank Global Limited
// 10577 ACC: 1710006214 DISC: 8% TYPE: ICP  GreenTag Investment Limited 
// 10639 ACC: 1710010736 DISC: 8% TYPE: ICP  Grandieu Limited
// 10657 ACC: 1711000649 DISC: 8% TYPE: ICP  ELVESTA VENTURES
// 10545 ACC: 1710003975 DISC: 8% TYPE: ICP  Adubi-Gold Nig Enterprises
// 10649 ACC: 1710012391 DISC: 8% TYPE: ICP UJE AND GROM CONCEPTS
// 10701 ACC: 1711003243 DISC: 8% TYPE: ICP Caiviks Technologies
// 10037 ACC: 1707009337 DISC: 8% TYPE: ICP Bizhub solution
// 10753 ACC: 1711002150 DISC: 8% TYPE: ICP HORLMIL CONTINENTAL SERVICES
// 10755 ACC: 1711007991 DISC: 8% TYPE: ICP EIUCOM RECHARGE HUB CONCEPTS
// 10251 ACC: 1709001469 DISC: 8% TYPE: ICP  Otomab and Sons Nigeria Limited
// 10905 ACC: 1712007920 DISC: 8% TYPE: ICP JATRAB VENTURES
// 10991 ACC: 1801003694 DISC: 8% TYPE: ICP GodfreyBrown LTD
// 11001 ACC: 1801004932 DISC: 8% TYPE: ICP Okay Fones Limited
// 10915 ACC: 1712009452 DISC: 8% TYPE: ICP Embehie Enterprises
// 10923 ACC: 1712011398 DISC: 8% TYPE: ICP Worthy Realtors
// 10925 ACC: 1712011403 DISC: 8% TYPE: ICP Holad best International
// 10955 ACC: 1701003491 DISC: 8% TYPE: ICP Natural colour press
// 10913 ACC: 1712009241 DISC: 8% TYPE: ICP Sambrix International Limited
// 10979 ACC: 1801002030 DISC: 8% TYPE: ICP Is-Hami Construction Limited
// 11075 ACC: 1801010261 DISC: 8% TYPE: ICP Adedayo Adedeji
// 11039 ACC: 1801007930 DISC: 8% TYPE: ICP Shounix Global Venture
// 11077 ACC: 1801010566 DISC: 8% TYPE: ICP CECON24 Systems Limited
// 11091 ACC: 1801013136 DISC: 8% TYPE: ICP Correspondence Limited ID
// 11055 ACC: 1802001867 DISC: 8% TYPE: ICP Evabest communications
// 11127 ACC: 1802001564 DISC: 8% TYPE: ICP Jidekaiji Global Services
// 11105 ACC: 1802000052 DISC: 8% TYPE: ICP Daveloret Concepts
// 11119 ACC: 1802002079 DISC: 8% TYPE: ICP Just Comestics Ltd
// 11143 ACC: 1802003245 DISC: 8% TYPE: ICP Bona Multi Solutions LTD
// 11145 ACC: 1802003248 DISC: 8% TYPE: ICP DirimzSammy EnterprisE
// 11143 ACC: 1802003245 DISC: 8% TYPE: ICP Bona Multi Solutions LTD
// 11089 ACC: 1801012269 DISC: 8% TYPE: ICP  Minsendwell Limited
// 11175 ACC: 1802006011 DISC: 8% TYPE: ICP Ammid Adex Telecom
// 11479 ACC: 1804010489 DISC: 8% TYPE: ICP Pointek Technology Limited
// 11487 ACC: 1804010870 DISC: 8% TYPE: ICP DMANSOCOTEX GLOBAL ENT
// 9947 ACC: 1707003031 DISC: 8% TYPE: ICP  DANDOLAS NIGERIA LIMITED
// 6423 ACC: 1508005012 DISC: 8% TYPE: ICP  Wonder Worth Global Investment Ltd
// 11679 ACC: 1806000361 DISC: 8% TYPE: ICP CLICK NETWORKS ENTERPRISES
// 11677 ACC: 1806000362 DISC: 8% TYPE: ICP LADEHBAK VENTURES
// 11727 ACC: 1806002927 DISC: 8% TYPE: ICP Kazra Global Services LTD
// 11785 ACC: 1806007561 DISC: 8% TYPE: ICP Comm-spot Concepts
// 11701 ACC: 1806001261 DISC: 8% TYPE: ICP ADE OMO ADE DOUBLE CROWN SOLUTIONS
// 3693 ACC: 1806009668 DISC: 8% TYPE: ICP Island Computer College
// 11837 ACC: 1806010698 DISC: 8% TYPE: ICP  Borsh Ventures
// 11577 ACC: 1806001027 DISC: 8% TYPE: ICP Coscharlom Intl Associates Ltd
// 11603 ACC: 1806000570 DISC: 8% TYPE: ICP INNOFLEX CYBER ENT
// 11847 ACC: 1807002293 DISC: 8% TYPE: ICP Abbegold ICT Solutions
// 10203 ACC: 1807005928 DISC: 8% TYPE: ICP Binet Resources
// 11951 ACC: 1807008185 DISC: 8% TYPE: ICP Rebras Tech Limited
// 12041 ACC: 1808001709 DISC: 8% TYPE: ICP Rachma Concept
// 12045 ACC: 1808002541 DISC: 8% TYPE: ICP MR. Abusufiyan Sa'ad
// 11849 ACC: 1807000802 DISC: 8% TYPE: ICP Repsunny Integrated Services Ltd
// 12053 ACC: 1808003233 DISC: 8% TYPE: ICP Darphilz Links Ventures Limited
// 12073 ACC: 1808004284 DISC: 8% TYPE: ICP V-dile Solutions
// 12109 ACC: 1808008325 DISC: 8% TYPE: ICP Arktronics Travels & Tours Limited
// 12141 ACC: 1701000420 DISC: 8% TYPE: ICP NANO NG MULTI SOLUTIONS LTD
// 11847 ACC: 1807002293 DISC: 8% TYPE: ICP Abbegold ICT Solutions
// 12191 ACC: 1809000541 DISC: 8% TYPE: ICP WAMBAI SHAAGI ATTORNEY
// 12193 ACC: 1809000731 DISC: 8% TYPE: ICP Toch Technologies Company
// 12183 ACC: 1809000427 DISC: 8% TYPE: ICP Soije Limited
// 12243 ACC: 1809004138 DISC: 8% TYPE: ICP Everest Technologies Limited
// 12245 ACC: 1809004139 DISC: 8% TYPE: ICP Toch Esmak Technology
// 12247 ACC: 1809004141 DISC: 8% TYPE: ICP Toch Libaaxa
// 12249 ACC: 1809004142 DISC: 8% TYPE: ICP Toch Bablaw Enterprises
// 12225 ACC: 1809002621 DISC: 8% TYPE: ICP HatlonSystems
// 12295 ACC: 1809007909 DISC: 8% TYPE: ICP ASSOTEL
// 12311 ACC: 1809009795 DISC: 8% TYPE: ICP Techlift Nigeria Limited
// 12327 ACC: 1809012114 DISC: 8% TYPE: ICP Pectra Tech Computers Ltd
// 12403 ACC: 1810010443 DISC: 8% TYPE: ICP Optimal Device Technology
// 12495 ACC: 1811002709 DISC: 8% TYPE: ICP Mimshackog Intl Ltd
// 12539 ACC: 1811006274 DISC: 8% TYPE: ICP ANDMARS NIGERIA LTD
// 12531 ACC: 1811005492 DISC: 8% TYPE: ICP Intermesh Company
// 12555 ACC: 1811008650 DISC: 8% TYPE: ICP Glomos Academy
// 9801 ACC: 1811011721 DISC: 8% TYPE: ICP Godmic Nigeria Limited
// 12635 ACC: 1812002878 DISC: 8% TYPE: ICP Helaz Global Solutions ltd
// 12675 ACC: 1812006454 DISC: 8% TYPE: ICP Farfromusual Limited
    public com.smilecoms.pos.DiscountData getDynamicDiscountDataNGNew(com.smilecoms.xml.schema.pos.Sale sale, com.smilecoms.xml.schema.pos.SaleLine saleLine, double exRt, boolean isSubItem, javax.persistence.EntityManager em) throws Exception {
        String itemNumber = saleLine.getInventoryItem().getItemNumber();
        String serialNumber = saleLine.getInventoryItem().getSerialNumber();
        boolean isAirtime = serialNumber.equals("AIRTIME");
        boolean isBundle = itemNumber.startsWith("BUN");
        boolean isKitBundle = itemNumber.startsWith("BUNK");
        boolean isNonKitBundle = (isBundle && !isKitBundle);
        String promoCode = sale.getPromotionCode();
        String paymentMethod = sale.getPaymentMethod();
        String ccNumber = sale.getCreditAccountNumber();
        int salesPersonId = sale.getSalesPersonCustomerId();
        long recipientAccountId = sale.getRecipientAccountId();
        boolean inQuote = (paymentMethod == null || paymentMethod.isEmpty());
        com.smilecoms.pos.DiscountData dd = new com.smilecoms.pos.DiscountData();
        String channel = sale.getChannel();
        String warehouse = sale.getWarehouseId();
        boolean isKit = itemNumber.startsWith("KIT");
        int orgId = sale.getRecipientOrganisationId();
        if (paymentMethod == null) {
            paymentMethod = "";
        }
        if (ccNumber == null) {
            ccNumber = "";
        }

        java.text.DateFormat dateTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (itemNumber.equalsIgnoreCase("KIT1324") && !(salesPersonId == 241231
                || salesPersonId == 452987
                || salesPersonId == 70537
                || salesPersonId == 251919
                || salesPersonId == 239261
                || salesPersonId == 472857
                || salesPersonId == 23076
                || salesPersonId == 114937
                || salesPersonId == 241603
                || salesPersonId == 70547
                || salesPersonId == 22802)) {
            throw new Exception("KIT1324 is not available for selling by sales person " + salesPersonId);
        }

// TundeAndAd
//if((itemNumber.equalsIgnoreCase("KIT1437") || itemNumber.equalsIgnoreCase("KIT1438") || itemNumber.equalsIgnoreCase("KIT1440") || itemNumber.equalsIgnoreCase("KIT1447")) 
        //                 && !(salesPersonId == 2 || salesPersonId == 305)) {
        //            throw new Exception("KIT is not available for selling by sales person " + salesPersonId);	
        //  }
//KIT1441-KIT1444 to Customer ID = 143507 (Dokun)
        if ((itemNumber.equalsIgnoreCase("KIT1441") || itemNumber.equalsIgnoreCase("KIT1442") || itemNumber.equalsIgnoreCase("KIT1443") || itemNumber.equalsIgnoreCase("KIT1444")) && !(salesPersonId == 143507 || salesPersonId == 178141 || salesPersonId == 119483 || salesPersonId == 25837 || salesPersonId == 480835 || salesPersonId == 114937)) {
            throw new Exception("KIT is not available for selling by sales person " + salesPersonId);
        }

// if(itemNumber.equalsIgnoreCase("KIT1341") ||
// 	itemNumber.equalsIgnoreCase("KIT1342") 
        // ) {
        //    throw new Exception("Kit '" + itemNumber +  "' is temporarily not available for selling.");	
// }
        if (itemNumber.equalsIgnoreCase("KIT1334") && !(salesPersonId == 2)) {
            throw new Exception("KIT1334 is not available for selling by sales person " + salesPersonId);
        }

//if((itemNumber.equalsIgnoreCase("KIT1357") ||  itemNumber.equalsIgnoreCase("KIT1358")) && !(salesPersonId == 12356)) {
        //     throw new Exception("KIT1358 or KIT1357 is not available for selling by sales person " + salesPersonId);	
//}
        //if((itemNumber.equalsIgnoreCase("KIT1433") || itemNumber.equalsIgnoreCase("KIT1434")) && !(salesPersonId == 2)) {
        //   throw new Exception("KIT1433 or KIT1434 is not available for selling by sales person " + salesPersonId);	
//}
        if (itemNumber.equalsIgnoreCase("KIT1435") && !(salesPersonId == 2)) {
            throw new Exception("KIT1435 is not available for selling by sales person " + salesPersonId);
        }

        if (itemNumber.equalsIgnoreCase("KIT1340") && !(salesPersonId == 2)) {
            throw new Exception("KIT1340 is not available for selling by sales person " + salesPersonId);
        }

        if (itemNumber.equalsIgnoreCase("KIT1349") || itemNumber.equalsIgnoreCase("KIT1350")) {
            throw new Exception("KIT no longer available for selling.");
        }

// Olumide make KIT1374 available to Customer ID = 12356 (Adedokun Olagunju)
//if(itemNumber.equalsIgnoreCase("KIT1374") && !(salesPersonId == 12356)) {
        //     throw new Exception("KIT1374 is not available for selling by sales person " + salesPersonId);	
//}
// Olumide -  Make KIT1373 available to Customer ID = 2(Tunde)
//if(itemNumber.equalsIgnoreCase("KIT1373") && !(salesPersonId == 2)) {
        //     throw new Exception("KIT1373 is not available for selling by sales person " + salesPersonId);	
//}
// Olumide -  Make KIT1401 available to Customer ID = 12356 (Adedokun Olagunju) 
//if(itemNumber.equalsIgnoreCase("KIT1401") && !(salesPersonId == 12356)) {
//     throw new Exception("KIT1401 is not available for selling by sales person " + salesPersonId);	
//}
        if (itemNumber.equalsIgnoreCase("KIT1351") && !(salesPersonId == 2)) {
            // throw new Exception("KIT1351 is not available for selling by sales person " + salesPersonId);	
        }

// To restrict KIT1359 to Tunde Baale only
        if (itemNumber.equalsIgnoreCase("KIT1359") && !(salesPersonId == 2)) {
            // throw new Exception("KIT1359 is not available for selling by sales person " + salesPersonId);	
        }

// To restrict BENTDI020 to 143507,119483 (requested by Caro
//if(itemNumber.equalsIgnoreCase("BENTDI020") && !(salesPersonId == 143507 || salesPersonId == 119483)) {
        //   throw new Exception("BENTDI020 is not available for selling by sales person " + salesPersonId);	
//}
// To restrict KIT1380 to Tunde Baale only
//if(itemNumber.equalsIgnoreCase("KIT1380") && !(salesPersonId == 2)) {
        // throw new Exception("KIT1380 is not available for selling by sales person " + salesPersonId);	
//}
// Testing for upsize in SEP-  Restrict BUNL0001 available to Customer ID = 2 (Tunde Baale) only.
        if (itemNumber.equalsIgnoreCase("BUNL0001") && !(salesPersonId == 2)) {
            throw new Exception("BUNL0001-1GB Upsize is not available for selling by sales person " + salesPersonId);
        }

// Allow all customer or salepeople to buy or sell the product after the specified time otherwise Restrict KITs purchase to Tunde Baale only
//itemNumber.equalsIgnoreCase("KIT1390") || 
// itemNumber.equalsIgnoreCase("KIT1398") || 
        if ((itemNumber.equalsIgnoreCase("KIT1390")
                || itemNumber.equalsIgnoreCase("KIT1398"))
                && (new java.util.Date().before(dateTimeFormat.parse("2018-12-28 00:00:00"))
                || new java.util.Date().after(dateTimeFormat.parse("2018-12-30 23:59:59")))) {
            throw new Exception("KIT is not available for selling.");
        }

        if ((itemNumber.equalsIgnoreCase("KIT1355")
                || itemNumber.equalsIgnoreCase("KIT1391")
                || itemNumber.equalsIgnoreCase("KIT1372")
                || itemNumber.equalsIgnoreCase("KIT1373")
                || itemNumber.equalsIgnoreCase("KIT1399")
                || itemNumber.equalsIgnoreCase("KIT1400")
                || itemNumber.equalsIgnoreCase("KIT1356")
                || itemNumber.equalsIgnoreCase("KIT1365")
                || itemNumber.equalsIgnoreCase("KIT1366")
                || itemNumber.equalsIgnoreCase("KIT1371")) && new java.util.Date().after(dateTimeFormat.parse("2018-11-24 00:00:00"))) {
            // if (!(salesPersonId == 2)) {
            throw new Exception("KIT is not available for selling.");

            //}
        }

// Testing refurbished on low utilisation,restricted KIT1379 to Emmanuel offem only
//if(itemNumber.equalsIgnoreCase("KIT1379") && !(salesPersonId == 70537)) {
        // throw new Exception("KIT1379 is not available for selling by sales person " + salesPersonId);	
//}
        if (itemNumber.equalsIgnoreCase("KIT1353") && !(salesPersonId == 2)) {
            throw new Exception("KIT1353 is not available for selling by sales person " + salesPersonId);
        }

//if( ( itemNumber.equalsIgnoreCase("KIT1426") || itemNumber.equalsIgnoreCase("KIT1427") || itemNumber.equalsIgnoreCase("KIT1428")) && !(salesPersonId == 2)) {
//     throw new Exception("KIT is not available for selling by sales person " + salesPersonId);	
//}
        if ((itemNumber.equalsIgnoreCase("KIT1429") || itemNumber.equalsIgnoreCase("KIT1430")) && !(salesPersonId == 2)) {
            throw new Exception("KIT1429/KIT1430 is not available for selling by sales person " + salesPersonId);
        }

        if (salesPersonId == 28381 && ((isBundle && !isKitBundle) || isAirtime)) {
            throw new Exception("You are barred from selling airtime and bundles. You can only sell kits");
        }

        if (promoCode.equalsIgnoreCase("Super Dealer Discount") && (itemNumber.equalsIgnoreCase("KIT1357") || itemNumber.equalsIgnoreCase("KIT1358") || itemNumber.equalsIgnoreCase("KIT1374") || itemNumber.equalsIgnoreCase("KIT1401") || itemNumber.equalsIgnoreCase("KIT1432"))) {
            dd.setDiscountPercentageOff(10d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // New super dealer sales process-these Items can be sold to SD only.
        // The list of physical items that can be sold to Super Dealers for their discount
        //String SDItems = "SIM8000 SIM8001 SIM8002 SIM8003 SIM8004 KIT1255 KIT1256 KIT1257 KIT1294 KIT1301 KIT1303 KIT1328 KIT1329 KIT1332 KIT1339 KIT1346 KIT1352 KIT1360 KIT1411";
        String kitRows = "<env.pos.superdealer.kits.items>";
        String[] rowsAsArray = kitRows.split("_newrow_");
        String retailKits = "";
        for (int i = 0; i < rowsAsArray.length; i++) {
            String row = rowsAsArray[i];
            String[] rowAsArray = row.split("\\|");//Get all columns
            retailKits = retailKits + " " + rowAsArray[0].replaceAll("_pipe_", "|");//Only interested on the first column
        }

        String SDItems = "SIM8000 SIM8001 SIM8002 SIM8003 SIM8004 " + retailKits;

        // HBT-6748 and HBT-7376 - New Franchise Model
        if (!itemNumber.startsWith("BUN") && (orgId == 8909 // Code2take Ventures - On hold
                || orgId == 2888 // Pheg Marathon Ventures
                || orgId == 10077 // Onigx Investment ltd
                || orgId == 9723 // Rezio Limited
                || orgId == 5059 // Callus Miller Company Limited
                )) {

            // New ICP Contract for Kaduna ICPs
            if (!paymentMethod.isEmpty() && !paymentMethod.equalsIgnoreCase("Credit Note") && !channel.startsWith("26P") && !channel.startsWith("27P") && !itemNumber.startsWith("BUN")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }

            if (SDItems.indexOf(itemNumber) != -1) {
                // This is one of the items in SDItems list
                int cnt = 0;
                // This logic counts how many kit items there are in the sale so we know if they have met their mimium of 500 in order to get the discount
                for (int i = 0; i < sale.getSaleLines().size(); i++) {
                    com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                    String it = otherLine.getInventoryItem().getItemNumber();
                    if (SDItems.indexOf(it) != -1) {
                        cnt += otherLine.getQuantity();
                    }
                }

                if (cnt >= 50 || promoCode.equalsIgnoreCase("Franchise Discount")) {
                    dd.setDiscountPercentageOff(7d);
                    dd.setTieredPricingPercentageOff(0d);
                    return dd;
                }
            }

            // Special discount for Callus Miller Company Limited
            if (isAirtime && orgId == 5059) {
                if (saleLine.getQuantity() >= 1000000) {
                    // This is airtime of over 1M
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(9.09090909d); // 9.0909% discount is the same as giving 10% extra airtime
                    return dd;
                }
            }

            if (isAirtime) {
                double discTier1 = 1d / (1d / 0.1d + 1d);
                double tier1Start = 2000000d / (1d - discTier1);

                long airtimeUnits = saleLine.getQuantity();
                if (airtimeUnits >= tier1Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier1 * 100d);
                    return dd;
                }
            }

            if (itemNumber.startsWith("KIT")) {
                // Dont allow the special Super Dealer KITs to be sold to anyone else
                throw new Exception("One or more of the items in the sale cannot be sold to Super Dealers");
            }
        }

        //New ICP contract for Kaduna ICPs
        // HBT-7239: Kaduna does not have a SD at the moment , all ICP in the region needs to be set up like SD on set up but get 8% discount.
        if (!itemNumber.startsWith("BUN") && (orgId == 6643 // Dahrah Global Limited
                || orgId == 6555 // Veemobile Services
                || orgId == 6997 // Value plus service & Industries Ltd
                || orgId == 7021 // Toluque Nigeria Limited
                || orgId == 6993 // Kima-Maxi Nigeria Limited
                || orgId == 7045 // Payit consulting Limited
                || orgId == 7009 // Detoyibo Enterprises
                || orgId == 2652 // Ditrezi Enterprises
                || orgId == 7843 // SAANET GLOBAL SERVICES LIMITED
                || orgId == 5479 // ImageAddiction
                || orgId == 9539 // Integrity Communications
                || orgId == 8283 // Digital Development Hub
                || orgId == 7417 // Synergy Mobile Business Solutions Limited
                || orgId == 9783 // 23RD Century Technologies LTD	
                || orgId == 9825 // Timboy Ventures
                || orgId == 9815 // Geniallinks Services
                || orgId == 9831 // Universal Asset Commercial Boker
                || orgId == 9941 // Manjub Limited
                || orgId == 10025 // Olans Communication
                || orgId == 7431 // Dpro Project
                || orgId == 10075 // Ultimate Aslan Limited
                || orgId == 6179 // Mo Concepts Co. LTD
                || orgId == 9631 // Crysolyte Networks
                || orgId == 7163 // Bloom Associates
                || orgId == 9649 // NM MARGI NIG LTD
                || orgId == 6929 // Micromanna Ltd.
                || orgId == 10019 // White house Computers Ltd
                || orgId == 10013 // Speedy IDeas Synergy Nig Ltd
                || orgId == 10181 // ALAJIM Limited
                || orgId == 10123 // MJ and partners power Electric
                || orgId == 10325 // Beyond Integrated Services
                || orgId == 10467 // ABSON Bussiness World LTD
                )) {
            // New ICP Contract for Kaduna ICPs
            if (!paymentMethod.isEmpty() && !paymentMethod.equalsIgnoreCase("Credit Note") && !channel.startsWith("26P") && !channel.startsWith("27P") && !itemNumber.startsWith("BUN")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }

            if (SDItems.indexOf(itemNumber) != -1) {
                // This is one of the items in SDItems list
                int cnt = 0;
                // This logic counts how many kit items there are in the sale so we know if they have met their mimium of 500 in order to get the discount
                for (int i = 0; i < sale.getSaleLines().size(); i++) {
                    com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                    String it = otherLine.getInventoryItem().getItemNumber();
                    if (SDItems.indexOf(it) != -1) {
                        cnt += otherLine.getQuantity();
                    }
                }

                if (cnt >= 500 || promoCode.equalsIgnoreCase("ICP Device Discount")) {

                    dd.setDiscountPercentageOff(8d);
                    dd.setTieredPricingPercentageOff(0d);
                    return dd;
                }
            }

            if (isAirtime) {
                double discAllTiers = 1d / (1d / 0.07d + 1d);
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(discAllTiers * 100d);
                return dd;
            }

            if (itemNumber.startsWith("KIT")) {
                // Dont allow the special Super Dealer KITs to be sold to anyone else
                throw new Exception("One or more of the items in the sale cannot be sold to Super Dealers");
            }

        }

        if ((itemNumber.equalsIgnoreCase("KIT1343")) && !(salesPersonId == 12356 || salesPersonId == 4304 || salesPersonId == 386833)) {
            throw new Exception("KIT1343 - Permission denied for " + salesPersonId);
        }

        if (itemNumber.equals("KIT1343") && orgId != 9647 && promoCode.equalsIgnoreCase("Super Dealer Discount")) {
            throw new Exception("That kit (KIT1343) is only for YFree.");
        }

        if (promoCode.equalsIgnoreCase("Super Dealer Discount") && orgId == 9647 && itemNumber.equalsIgnoreCase("KIT1343")) {
            dd.setDiscountPercentageOff(1.232d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // New super dealer sales process (Super Dealers)
        if (orgId == 8623 // ZEPH ASSOCIATES NIGERIA LIMITED
                || orgId == 8641 // Data Partners Limited
                || orgId == 8639 // Gabros Divine Technologies Limited
                || orgId == 8649 // CANT STOP NIGERIA LIMITED GOlden Rule
                || orgId == 9109 // Concise Services Limited
                || orgId == 2643 // Timjat Global Resources Ltd
                || orgId == 9615 // Digital Development Hub Consultancy Services Ltd
                || orgId == 9647 // YFree SD
                || orgId == 6633 // Lyds Louisa Intergrated Company Limited
                || orgId == 7057 // Sonite Communications Limited
                || orgId == 10839 // Lustre Communications ltd
                || orgId == 12509 // Communication Facilitators Ltd
                || orgId == 11091 // Correspondence Limited
                || orgId == 9801 // Godmic Nigeria Limited
                ) {
            // The item is being sold to a Super Dealer Organisation
            if (!paymentMethod.isEmpty() && !paymentMethod.equalsIgnoreCase("Credit Note") && !channel.startsWith("26P") && !channel.startsWith("27P") && !itemNumber.startsWith("BUN")) {
                // Dont allow the wrong channel to be used when selling to Super Dealers
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }

            if (isAirtime && saleLine.getQuantity() >= 2000000) {
                // This is airtime of over 5M
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(9.09090909d); // 9.0909% discount is the same as giving 10% extra airtime
                return dd;
            } else if (SDItems.indexOf(itemNumber) != -1) {
                // This is one of the items in SDItems list
                int cnt = 0;
                // This logic counts how many kit items there are in the sale so we know if they have met their mimium of 500 in order to get the discount
                for (int i = 0; i < sale.getSaleLines().size(); i++) {
                    com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                    String it = otherLine.getInventoryItem().getItemNumber();
                    if (SDItems.indexOf(it) != -1) {
                        cnt += otherLine.getQuantity();
                    }
                }

                if (cnt >= 500 || promoCode.equalsIgnoreCase("Super Dealer Discount")) {

                    if (orgId == 7057 && itemNumber.equalsIgnoreCase("KIT1332")) {
                        dd.setDiscountPercentageOff(19.379845d);
                        dd.setTieredPricingPercentageOff(0d);
                        return dd;
                    }

                    // The sale does have over 500 devices in it
                    dd.setDiscountPercentageOff(10d);
                    dd.setTieredPricingPercentageOff(0d);
                    return dd;
                }
            } else if (itemNumber.startsWith("KIT")) {
                throw new Exception("One or more of the items in the sale cannot be sold to Super Dealers");
            }
        } else if (itemNumber.startsWith("KIT") && SDItems.indexOf(itemNumber) != -1) {
            // Dont allow the special Super Dealer KITs to be sold to anyone else
            throw new Exception("One or more of the items in the sale can only be sold to Super Dealers");
        }

        if (itemNumber.startsWith("BUNST") || itemNumber.startsWith("BUNTV")) {
            if (!paymentMethod.equals("") && !paymentMethod.equals("Credit Account")) {
                throw new Exception("Staff bundles must be paid by credit account");
            }
            // 100% discount for Staff bundles
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.startsWith("BUNP") && saleLine.getInventoryItem().getPriceInCentsIncl() > 0d) {
            if (!paymentMethod.equals("") && !paymentMethod.equals("Cash") && !itemNumber.equals("BUNPG0000")) {
                throw new Exception("Gift bundles must be paid by cash");
            }
            // 100% discount for Promotional bundles
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // HBT-7518
        if (itemNumber.equals("BUNDC3007")) {
            dd.setDiscountPercentageOff(50.00d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

//HBT 8148
        if (itemNumber.equals("BUNDC3005")) {
            dd.setDiscountPercentageOff(37.5d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BUNDC3001")) {
            dd.setDiscountPercentageOff(50.00d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BUNDC3003")) {
            dd.setDiscountPercentageOff(66.66666667d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BUNDC3010")) {
            dd.setDiscountPercentageOff(44.44444444d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // HBT-7231 - Creation of Promo Bundle for Winback Campaign
        if (itemNumber.equals("BUNDC4003")) {
            // 27.5204754656809d % discount as per Mike's comment on HBT-7231 to bring the price down to NGN1000
            dd.setDiscountPercentageOff(27.5204754656809d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // HBT-7386 - Creation of Promo Bundle for Winback Campaign 2
        if (itemNumber.equals("BUNDC4007")) {
            // 68.9373d% discount as per Mike's comment on HBT-7231 to bring the price down to NGN1000
            dd.setDiscountPercentageOff(68.9373d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BUN1020") && (paymentMethod.equals("Credit Account") || paymentMethod.isEmpty()) && orgId == 8465) {
            // 5% discount for NCC on 20GB Bundle as per HBT-6414
            dd.setDiscountPercentageOff(5d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // HBT-6815 
        if ((orgId == 3199) && (itemNumber.equals("BENTOS200") || itemNumber.equals("BUN1200")) && promoCode.equals("4.76% off 200GB Anytime Sharing Bundle")) {
            dd.setDiscountPercentageOff(4.76d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

// HBT-7104
        if ((orgId == 8557 || orgId == 6091) && (itemNumber.equals("BENTOS200") || itemNumber.equals("BUN1200")) && promoCode.equals("5% off 200GB Anytime Sharing Bundle")) {
            dd.setDiscountPercentageOff(5.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Ensure that sales of non corporate GBR items are not open for more than 7 days
        boolean isCorpGBR = itemNumber.startsWith("BENT");
        if ((isAirtime || isKit) && !isCorpGBR && sale.getExpiryDate() != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sale.getSaleDate().toGregorianCalendar().getTime());
            cal.add(java.util.Calendar.DATE, 7);
            if (sale.getExpiryDate().toGregorianCalendar().getTime().after(cal.getTime())) {
                throw new Exception("Only Corporate GBR can be on an invoice open for more than 7 days");
            }
        }

        // 100% discount on routers and sims and dongles testing purposes when testing new products, kits etc
        if (promoCode.equalsIgnoreCase("TestingDiscount")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BENTBE010") || itemNumber.equals("BENTBE020") || itemNumber.equals("BENTBE030")) {
            if (saleLine.getQuantity() >= 12) {
                // 10% discount when buying enterprise best effort 12 months in advance
                dd.setDiscountPercentageOff(10d);
            } else if (saleLine.getQuantity() >= 6) {
                // 10% discount when buying enterprise best effort 6 months in advance
                dd.setDiscountPercentageOff(5d);
            }
            if (itemNumber.equals("BENTBE010") || itemNumber.equals("BENTBE020") || itemNumber.equals("BENTBE030")) {
                if (promoCode.equalsIgnoreCase("5% Discount on Corporate Access")) {
                    // Additional 5% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 5d);
                } else if (promoCode.equalsIgnoreCase("10% Discount on Corporate Access")) {
                    // Additional 10% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 10d);
                } else if (promoCode.equalsIgnoreCase("15% Discount on Corporate Access")) {
                    // Additional 15% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 15d);
                } else if (promoCode.equalsIgnoreCase("16.5% Discount on Corporate Access")) {
                    // Additional 16.5% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 16.5d);
                } else if (promoCode.equalsIgnoreCase("30% Discount on Corporate Access")) {
                    // Additional 30% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 30d);
                } else if (promoCode.equalsIgnoreCase("33% Discount on Corporate Access")) {
                    // Additional 33% off enterprise best effort if specified by the sales person
                    dd.setDiscountPercentageOff(dd.getDiscountPercentageOff() + 33d);
                }
            }
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.startsWith("ROU")) {
            for (int i = 0; i < sale.getSaleLines().size(); i++) {
                com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                String it = otherLine.getInventoryItem().getItemNumber();
                if (otherLine.getQuantity() >= 6 && (it.equals("BENTBE010") || it.equals("BENTBE020") || it.equals("BENTBE030"))) {
                    // Get 100% off the router when buying 6 months enterprise best effort up front
                    dd.setDiscountPercentageOff(100d);
                    return dd;
                }
            }
        }

        if (itemNumber.equals("KIT1217") && orgId != 3700) {
            throw new Exception("That kit is only for Diamond Bank");
        }

        if (!itemNumber.equals("KIT1293") && !itemNumber.equals("KIT1287") && !itemNumber.equals("KIT1289") && !itemNumber.equals("KIT1288") && !itemNumber.equals("KIT1290") && !itemNumber.startsWith("BAT") && !itemNumber.startsWith("SIM") && !itemNumber.startsWith("BUN") && saleLine.getInventoryItem().getPriceInCentsIncl() == 0d && !paymentMethod.isEmpty() && !paymentMethod.equals("Credit Note") && sale.getSalesPersonCustomerId() != 1) {
            throw new Exception("A sale with a zero item price is not allowed!");
        }

        if (!isSubItem && !paymentMethod.isEmpty() && !paymentMethod.equals("Staff") && !paymentMethod.equals("Loan") && !paymentMethod.equals("Credit Note") && itemNumber.startsWith("SIM") && promoCode.isEmpty() && !promoCode.contains("100% Volume Break")) {
            throw new Exception("A standalone SIM can only be sold using a SIMSwap promotion code. SIMs sold with SIMSwap promotion codes can only be used in a SIM Swap");
        }

        if (!paymentMethod.isEmpty() && !paymentMethod.equals("Loan") && promoCode.equalsIgnoreCase("HotSpot")) {
            throw new Exception("Hotspots must be loaned");
        }
        if (itemNumber.equals("KIT1207") && !paymentMethod.equals("Staff") && !inQuote) {
            throw new Exception("Selected Kit is exclusive to Smile Staff");
        }
        if (!inQuote && !ccNumber.equals("CSI003") && (promoCode.equalsIgnoreCase("Education") || promoCode.equalsIgnoreCase("Community"))) {
            throw new Exception("CSI Promotions must be on the CSI credit account. Add CSI003 as the credit account number for this Organisation and select Credit Account as the payment method");
        }

        // 100% discount on routers and sims for educational institutions
        if (!isAirtime && promoCode.equalsIgnoreCase("Education")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims for brand ambassadors
        if (!isAirtime && promoCode.equalsIgnoreCase("BrandAmbassador")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on sims for faulty sim
        if (itemNumber.startsWith("SIM") && promoCode.equalsIgnoreCase("FreeSIMSwap")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims for communities 
        if (!isAirtime && promoCode.equalsIgnoreCase("Community")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on routers and sims and dongles for gifts and give aways
        if (!isAirtime && promoCode.equalsIgnoreCase("UCGiveAway")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        // 100% discount on ICP tools of trade event
        if (!isAirtime && promoCode.equalsIgnoreCase("ICPToolOfTrade")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Discounts when doing returns/replacements of ATEL
        if (!isSubItem && (itemNumber.equals("MIF6003") || itemNumber.equals("MIF6006")) && promoCode.contains("Pays 30%")) {
            dd.setDiscountPercentageOff(70d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        if (!isSubItem && (itemNumber.equals("MIF6003") || itemNumber.equals("MIF6006")) && promoCode.contains("Pays 50%")) {
            dd.setDiscountPercentageOff(50d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        if (!isSubItem && (itemNumber.equals("MIF6003") || itemNumber.equals("MIF6006")) && promoCode.contains("Pays 70%")) {
            dd.setDiscountPercentageOff(30d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // 100% discount on static IP
        if (itemNumber.startsWith("BUNIP") && promoCode.equalsIgnoreCase("100% discount on Static IP")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Percentage based volume breaks
        if (!isAirtime && promoCode.contains("% Volume Break") && !isNonKitBundle) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(discPercent);
            return dd;
        }

        // Percentage based discounts
        if (!isAirtime && promoCode.contains("% Discount") && !isNonKitBundle) {
            String disc = promoCode.split("\\%")[0].trim();
            double discPercent = Double.parseDouble(disc);
            dd.setDiscountPercentageOff(discPercent);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        // Temp for airtime commissions
        if (isAirtime && promoCode.equals("100% Off Airtime")) {
            dd.setDiscountPercentageOff(100.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (isAirtime && promoCode.equals("5% Off Airtime")) {
            dd.setDiscountPercentageOff(5.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (isAirtime && promoCode.equals("10% Off Airtime")) {
            dd.setDiscountPercentageOff(10.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (isAirtime && promoCode.equals("15% Off Airtime")) {
            dd.setDiscountPercentageOff(15.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (isBundle && promoCode.equals("5% Off Data Bundle")) {
            dd.setDiscountPercentageOff(5.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (isBundle && promoCode.equals("10% Off Data Bundle")) {
            dd.setDiscountPercentageOff(10.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (isBundle && promoCode.equals("15% Off Data Bundle")) {
            dd.setDiscountPercentageOff(15.0d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (orgId == 2889 // Asbam Global Resources
                || orgId == 2893 // Lightning Networks
                || orgId == 2894 // Green Heritage                
                ) {
            // Old ICP Contract
            if (!paymentMethod.isEmpty() && !channel.startsWith("21E") && !channel.startsWith("25I") && !channel.startsWith("27I") && !itemNumber.startsWith("BUN")) {
                throw new Exception("Sales to ICPs must use channel starting with 21E or 25I or 27I");
            }
            if (isAirtime) {
                // ICPs get 8% Discount
                dd.setDiscountPercentageOff(8d);
                dd.setTieredPricingPercentageOff(0d);
                return dd;
            }
            if (isKit) {
                // ICPs Get 10% off kits
                dd.setDiscountPercentageOff(0d);
                dd.setTieredPricingPercentageOff(10d);
                return dd;
            }
        }

        if (itemNumber.equals("BUNIC1005")) {
            dd.setDiscountPercentageOff(100d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        //MNG-63 : Container combo bundle with discounts BUNC1101 and BUNC1500
        if (itemNumber.equals("BUNC1101")) {
            dd.setDiscountPercentageOff(47.36842105263160d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        if (itemNumber.equals("BUNC1500")) {
            dd.setDiscountPercentageOff(55.55555555555560d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

//MNG-66 : Container combo bundles with discounts
        if (itemNumber.equals("BUNC1107")) {
            dd.setDiscountPercentageOff(2.534113060428850d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        if (itemNumber.equals("BUNC1001")) {
            dd.setDiscountPercentageOff(22.11838006230530d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }
        if (itemNumber.equals("BUNC2015")) {
            dd.setDiscountPercentageOff(0.09990009990010d);
            dd.setTieredPricingPercentageOff(0d);
            return dd;
        }

        String simKits = "KIT1208 KIT1209 KIT1210";
        String deviceKits = "KIT1238 KIT1239 KIT1200 KIT1201 KIT1202 KIT1203 KIT1204 KIT1205 KIT1206 KIT1211 KIT1212 KIT1213 KIT1214 KIT1215 KIT1218 KIT1219 KIT1220 KIT1222 KIT1223 KIT1221 KIT1224 KIT1227 KIT1228 KIT1229 KIT1231 KIT1232 KIT1240 KIT1241 KIT1242 KIT1244 KIT1245 KIT1246 KIT1251 KIT1252 KIT1267 KIT1263 KIT1261 KIT1265 KIT1279 KIT1280 KIT1266 KIT1286 KIT1281 KIT1284 KIT1282 KIT1274 KIT1272 KIT1270 KIT1291 KIT1259 KIT1299 KIT1300 KIT1302 KIT1305 KIT1304";

        // New ICP contract
        if (1 == 2 // Disable ICP tiering model as per HBT-9041 
                && !itemNumber.startsWith("BUN") && (orgId == 3670 //Adibba Online Shopping LTD
                || orgId == 2918 //Aghayedo Brothers Enterprises
                || orgId == 3677 //Bankytech Communications Ventures
                || orgId == 4222 //Barcutt Investment Limited
                || orgId == 3915 //BIGDATA TECHNOLOGIES LTD
                || orgId == 2121 //C-Soka Limited
                || orgId == 3412 //Galant Communications LTD
                || orgId == 4938 //Global Rolling
                || orgId == 4679 //Ideal Hospitality Solutions Limited
                || orgId == 3143 //Koam Computer Solution
                || orgId == 2630 //Netpoint Communications
                || orgId == 2123 //Novas Systems Limited
                || orgId == 2888 //Pheg Marathon Ventures
                || orgId == 3165 //Pine Height Global Resources ltd
                || orgId == 2643 //Timjat Global Resources Ltd
                || orgId == 3675 //Trend World Wide Ltd
                || orgId == 3413 //Wireless Link Integrated
                || orgId == 2917 //Yfree Solutions Limited
                || orgId == 2653 //Zacfred Systems Solutions Ltd
                || orgId == 5026 //Top on Top Success Global Limited
                || orgId == 5040 // Rafael stores
                || orgId == 4201 // Abetech
                || orgId == 5074 // Globasure Technologies
                || orgId == 2122 // Best
                || orgId == 5117 // VConnect
                || orgId == 5210 // Phone Hut
                || orgId == 5127 // Content Oasis
                || orgId == 5162 // Sainana Resources
                || orgId == 5165 // Hasino Communication
                || orgId == 5215 // Christy George
                || orgId == 5056 // Everyday group
                || orgId == 5242 // Telxtra
                || orgId == 5293 // Cedar network
                || orgId == 5289 // Korenet
                || orgId == 5292 // Skyraph
                || orgId == 5279 // Tobowy
                || orgId == 5450 // Sacen Projects
                || orgId == 5446 // Central Dynamics
                || orgId == 5444 // Pearl Facet
                || orgId == 5445 // Emmrose
                || orgId == 5447 // Einfotech
                || orgId == 5452 // Kevicomms
                || orgId == 5448 // Bel comms
                || orgId == 5472 // Onis Digital
                || orgId == 285 // Sabaoth
                || orgId == 5498 // Biz Ad Solution
                || orgId == 5493 // Pinnacle Wireless
                || orgId == 5499 // GG Communications
                || orgId == 5501 // Tim Sholtz Nigeria Limited
                || orgId == 5491 // Baytown Communications
                || orgId == 5500 // Spectrum Innovation Technologies
                || orgId == 5489 // Wanaga Nig Ltd
                || orgId == 5490 // Primadas Investment Nig. Ltd
                || orgId == 5478 // Bayunfa Ventures Nig.Ltd
                || orgId == 5487 // North East Gold
                || orgId == 5473 // Azman Concepts Limited
                || orgId == 5474 // Infotek Perspective Service Limited
                || orgId == 5449 // Sayrex
                || orgId == 5483 // 3CEE
                || orgId == 5060 // Integral
                || orgId == 5480 // Samace
                || orgId == 5482 // Zioneli
                || orgId == 5504 // Belle vista
                || orgId == 5506 // Dot.com ventures
                || orgId == 5519 // Burj Khalifah Nigeria Limited
                || orgId == 5521 // Datacounter International Ventures
                || orgId == 5534 // Yield Systems
                || orgId == 5529 // Arctuals Concepts Limited
                || orgId == 5530 // Emax Solutions Ventures Ltd
                || orgId == 5539 //  I Will Distribute
                || orgId == 5565 // Broad Prject Nigeria
                || orgId == 5566 // Heirs Multi Concept
                || orgId == 5567 // ACMESYNERGY
                || orgId == 5555 // COMTRANET IT SOLUTIONS LTD
                || orgId == 5577 // JOTECH COMPUTER
                || orgId == 5599 // CHARLOTTE GLOBAL CONCEPTS
                || orgId == 5059 // Callus Miller
                || orgId == 5479 // ImageAddiction
                || orgId == 5631 // Ages Accurate Speed Ltd
                || orgId == 5632 // Kestrel tours
                || orgId == 1361 // ONILU PRIME
                || orgId == 5680 // Trebkann
                || orgId == 5713 // Recharge Hub Services 
                || orgId == 5715 // Suremobile Network Ltd
                || orgId == 5729 // Olive Integrated Services Limited
                || orgId == 5769 // Centri-net Technology Nig. Ltd
                || orgId == 5823 // Merc-Oaton
                || orgId == 5839 // Pristine Panache
                || orgId == 5833 // FF Tech
                || orgId == 5837 // Cheroxy
                || orgId == 5889 // Optimum Greenland
                || orgId == 5925 // Foimm Technology Enterprise
                || orgId == 5921 // Oakmead Realtors
                || orgId == 5927 // First Sopeq Integrated Nig Ltd
                || orgId == 5929 // SDMA Networks
                || orgId == 6013 // Viclyd Nig Ent
                || orgId == 5062 // CHUXEL COMPUTERS
                || orgId == 6019 // BROADLAND GLOBAL RESOURCES LIMITED
                || orgId == 6051 // Reart Risk and Safety Management Limited
                || orgId == 6081 // Mesizanee Concept Limited
                || orgId == 6149 // Bailord Limited
                || orgId == 6155 // Sweetplanet Worldwide Enterprise
                || orgId == 6181 // SHELL EAST STAFF INVESTMENT CO-OP
                || orgId == 6179 // Mo Concepts Co
                || orgId == 6175 // Great Bakis Ventures 
                || orgId == 6177 // Pose Technologies Ltd
                || orgId == 6211 // Shetuham Intl Ltd
                || orgId == 6271 // Avensa Global solutions
                || orgId == 6283 // Derak Multi Ventures Nigeria Limited
                || orgId == 6311 // Donval Platinum Investment
                || orgId == 6299 // Best Baxany Global Services Limited
                || orgId == 6307 // BrightGrab Integrated Services
                || orgId == 6289 // Final Logic Limited
                || orgId == 6339 // Top up Afric
                || orgId == 6369 // Mcedwards Ent.Ltd
                || orgId == 6407 // Lumron Enterprises
                || orgId == 6419 // SWING-HIGH COMMUNICATIONS LIMITED
                || orgId == 6437 // New Sonshyne Systems Limited
                || orgId == 6417 // Simcoe & Dalhousie Nigeria Limited
                || orgId == 6423 // Wonder Global Investment Ltd.Organisation Id 
                || orgId == 6459 // CHARLYN NETWORKS LTD
                || orgId == 6461 // Febby Communication
                || orgId == 6519 // Rakameris Ventures
                || orgId == 6523 // The Koptim Company
                || orgId == 6537 // D.S. Computer-Tech
                || orgId == 6547 // KVC E-Mart
                || orgId == 6541 // Security Services and Consultancy
                || orgId == 6557 // Globe Du Bellissima LTD
                || orgId == 6561 // Kamzi Global Enterprise
                || orgId == 6581 // ING Systems
                || orgId == 6583 // Codex Plus
                || orgId == 6599 // Phones and Computers One- Stop Center Limited
                || orgId == 6617 // KTO ENT
                || orgId == 6615 // GENIE INFOTECH LTD
                || orgId == 6607 // Kollash Business and Technologies Limited
                || orgId == 6623 // Sovereign Gold Service
                || orgId == 6631 // Abhala Ben International Ventures 
                || orgId == 6629 // J.E.A Global Ventures
                || orgId == 6641 // Jenshu Int'l Services Ltd
                || orgId == 6659 // YOMEEZ ELECTROWORLD LTD
                || orgId == 6685 // Soofine Print Limited
                || orgId == 6687 // Lindski
                || orgId == 6689 // Frotkom Nigeria Limited.
                || orgId == 6735 // Avro Group
                || orgId == 6733 // Isladex Communications
                || orgId == 6657 // Rich Technologies Limited
                || orgId == 6745 // Joeche Computer Warehouse
                || orgId == 6653 // Ambuilt Works Limited 
                || orgId == 6719 // Le Charm International Limited
                || orgId == 2918 // Aghayedo Brothers Enterprises
                || orgId == 6565 // EMSI Enterprise
                || orgId == 6761 // hephyOceans Integrated Services Limited
                || orgId == 6665 // Oliver1010 Communications Concept Limited
                || orgId == 6625 // Surfspot Communications Limited
                || orgId == 6633 // Lydslouisa Integrated Company Limited
                || orgId == 6747 // First Regional Solutions Limited. Id
                || orgId == 6779 // The Gizmo world And Solution Limited
                || orgId == 6771 // Easy Talil Enterprise
                || orgId == 6785 // Nofot Gardma Limited
                || orgId == 6823 // Omovo Group
                || orgId == 6821 // Fibre Path
                || orgId == 6787 // Digital Direct
                || orgId == 6819 // Xtraone Family Minstrel Ventures
                || orgId == 6827 // Topcoat Limited
                || orgId == 6861 // General B Y Supermarket
                || orgId == 6805 // Global Quid Ltd
                || orgId == 6923 // LeeBroadVentures
                || orgId == 6917 // Moben-D' Excel Limited
                || orgId == 7011 // SMB Shanono Global Venture Ltd
                || orgId == 6995 // Melendless Ltd
                || orgId == 7057 // Sonite communication
                || orgId == 7089 // Madeeyak
                || orgId == 7073 // Integrated Communication Engineering 
                || orgId == 6949 // caso communication
                || orgId == 7141 // Syrah Communication Limited
                || orgId == 7145 // Morgen Global Limited
                || orgId == 7149 // SDMA Limited Surulere
                || orgId == 7157 // Good Venture Integrated Concepts Limited
                || orgId == 7167 // DAKESH DYNAMIC SERVICES
                || orgId == 7197 // Skenyo Global Services Limited
                || orgId == 7217 // REMGLORY VENTURES NIGERIA LTD
                || orgId == 7209 // U-MAT SUCCESS VENTURES
                || orgId == 7215 // TOKAZ CONCEPT
                || orgId == 7229 // Russo Link
                || orgId == 7207 // Gofulene Integrated Service Ltd
                || orgId == 7227 // Laidera Consulting Ltd
                || orgId == 289 // Olamind Technology
                || orgId == 7273 // ABBAH TECH CONNECTIONS LTD
                || orgId == 7295 // Commonwealth Technologies Ltd
                || orgId == 7293 // Vantage Options Nigeria Ltd
                || orgId == 7297 // B-Jayc Global Resources Limited
                || orgId == 7291 // SDMA Network Badagry
                || orgId == 7347 // Amarcelo Limited
                || orgId == 7159 // MULTI RESOURCES AND INDUSTRIAL SERVICES
                || orgId == 7381 // POYEN NOMOVO NIG LTD
                || orgId == 6769 // G-PAY INSTANT SOLUTIONS
                || orgId == 7073 // Intergrated Communication Engineering
                || orgId == 7431 // Dpro Project Limited
                || orgId == 7417 // Synergy Mobile Business Solutions Limited
                || orgId == 7429 // IDEA KONSULT LIMITED
                || orgId == 7453 // Harry Concern Nigeria Enterprises
                || orgId == 7399 // Dream Nation Alliance (MPCS) Limited
                || orgId == 7329 // Contec Global Infotech Ltd
                || orgId == 7477 // ALTS Service Consult Limited
                || orgId == 7493 // Ski Current Services Limited
                || orgId == 7491 // Emir Label Entertainment Limited
                || orgId == 7559 // Mercury Resources And Investment Company Ltd
                || orgId == 7575 // Micro Link Telecoms Limited
                || orgId == 7237 // Star Gaming Limited
                || orgId == 7727 // Neocortex International Ltd. ID
                || orgId == 7697 // UCAS GLOBAL RESOURCES
                || orgId == 7771 // Zillion Technocom
                || orgId == 7791 // Divine Anchor
                || orgId == 7821 // Grinotech limited
                || orgId == 7845 // Banc Solutions Ltd
                || orgId == 7851 // Next Bt Ltd
                || orgId == 7881 // Uzogy Integrated Technical Service
                || orgId == 7919 // Anne colt intergrated services ltd
                || orgId == 7887 // Fashamu Bookshop and Business Centre
                || orgId == 7921 // TIDAKE GLOBAL LIMITED
                || orgId == 8051 // RULESIXMOBILE LTD
                || orgId == 8139 // Texmaco Ventures Nigeria Limited
                || orgId == 8185 // Payquic World Wide Ltd
                || orgId == 8191 // Kemasho Ventures Ltd
                || orgId == 8215 // I CELL MOBILE
                || orgId == 8279 // Lemor Global Services
                || orgId == 8361 // SmartDrive & Systems Limited
                || orgId == 8107 // Cordiant Technology Services
                || orgId == 8511 // Nartel Ventures
                || orgId == 8615 // Flux Integrated Services Ltd
                || orgId == 8903 // Emma Paradise Multilinks Limited
                || orgId == 8909 // CODE2TAKE VENTURES LIMITED
                || orgId == 9049 // Northsnow Enterprises
                || orgId == 9039 // Hadiel Calidad Limited
                || orgId == 9091 // Enatel Communications
                || orgId == 9629 // Diksolyn Concept Limited
                || orgId == 9625 // Global Lightcrown Ventures	
                || orgId == 9637 // Network Enterprises PH
                || orgId == 5985 // ICP Capricorn Digital
                || orgId == 8283 // Digital Development Hub
                || orgId == 10059 // Glitteringway enterprise
                || orgId == 10009 // Asrad Multi-links Business and logistic limited
                || orgId == 10001 // BlueBreed Enterprise
                || orgId == 10025 // Olans Communication
                || orgId == 7431 // Dpro Project
                || orgId == 9885 // Happymood enterprise
                || orgId == 9975 // Godsaves devices Tech Ltd
                || orgId == 9945 // Aumak-mikash Ltd
                || orgId == 9999 // Aje Global Multiservices
                || orgId == 9963 // FEM9 TOUCH LIMITED
                || orgId == 9973 // Joelink Global Concept communication Ltd
                || orgId == 9967 // Goldenworths West Africa LTD
                || orgId == 5488 // Blessing Computers
                || orgId == 5519 // Burj Khalifah Nigeria Limited
                || orgId == 2652 // Ditrezi Enterprises
                || orgId == 9947 // DANDOLAS NIGERIA LIMITED
                || orgId == 9941 // Manjub Limited
                || orgId == 9939 // Tecboom Digital Ltd
                || orgId == 9933 // Da-Costa F.A and Company
                || orgId == 9929 // Jb Mwonyi Company Limited
                || orgId == 9883 // Oroborkso Ventures Nigeria Limited
                || orgId == 9875 // Chrisoduwa Global Ventures
                || orgId == 9887 // Multitronics Global Enterprise
                || orgId == 9861 // DRUCICARE NIG LTD
                || orgId == 9881 // Beejao Infosolutions Limited
                || orgId == 9877 // Sam Fransco Resources
                || orgId == 9873 // GLORIOUS TELECOMM
                || orgId == 9867 // AMIN AUTO LTD
                || orgId == 9931 // Boxtech Consult
                || orgId == 9649 // NM MARGI NIG LTD
                || orgId == 9897 // Brownnys-Connect Nigeria Limited
                || orgId == 9835 // K-CEE DEVICES LIMITED
                || orgId == 9859 // Budddybod Enterprises
                || orgId == 9863 // GOLDENPEECOMPUTERS TECHNOLOGIES
                || orgId == 9831 // Universal Asset Commercial Boker
                || orgId == 9847 // Fundripples Limited
                || orgId == 9841 // Sharptowers Limited
                || orgId == 9549 // Payconnect Networks Ltd
                || orgId == 9827 // Leverage Options
                || orgId == 9793 // Sunsole Global Investment Ltd
                || orgId == 9801 // Godmic Nigeria LTD
                || orgId == 9825 // Timboy Ventures
                || orgId == 9815 // Geniallinks Services
                || orgId == 9783 // 23RD Century Technologies LTD
                || orgId == 9755 // Abiola Gold Resources
                || orgId == 9753 // Hetoch Technology
                || orgId == 9747 // Mac Onella
                || orgId == 9711 // Finet Communications Ltd
                || orgId == 9723 // Rezio Limited
                || orgId == 10041 // Base 15 Concept Nigeria Limited
                || orgId == 9571 // Cyberdyne Integrated Limited
                || orgId == 10079 // LTC network limited
                || orgId == 10077 // Onigx Investment Limited
                || orgId == 10085 // Global Communication and Digital services
                || orgId == 10115 // Dolamif Nigeria Limited
                || orgId == 10151 // TICEE CONNECT
                || orgId == 10159 // Poatech Enterprises Nigeria Ltd
                || orgId == 10171 // Pesuzok Nig Ltd
                || orgId == 9731 // Belledave Nigeria Limited
                || orgId == 10163 // Option 360 Communications
                || orgId == 10179 // Popsicles Enterprises 
                || orgId == 10109 // Ahijo Ventures limited
                || orgId == 10215 // Rahmatob Computer Concept
                || orgId == 10219 // Kastech Technologies
                || orgId == 8909 // Code2take Ventures
                || orgId == 10221 // Profast Integrated Services Limited
                || orgId == 10323 // OPKISE GLOBAL CONCEPTS
                || orgId == 10303 // PC Corner Services Limited
                || orgId == 10429 // GABNET TECHNOLOGY
                || orgId == 10427 // LATEETOY NIGERIA LIMITED            
                || orgId == 9609 // Lexton Energy
                || orgId == 10553 // Olive-Frank Global Limited
                || orgId == 673419 // GreenTag Investment Limited 
                || orgId == 10571 // Pristine Stitches Enterprise 
                || orgId == 10639 // Grandieu Limited
                || orgId == 10657 // ELVESTA VENTURES
                || orgId == 10545 //Adubi-Gold Nig Enterprises
                || orgId == 10649 // UJE AND GROM CONCEPTS
                || orgId == 10701 // Caiviks Technologies
                || orgId == 10037 // Bizhub solution
                || orgId == 10753 // HORLMIL CONTINENTAL SERVICES
                || orgId == 10755 // EIUCOM RECHARGE HUB CONCEPTS
                || orgId == 3 // Helpline Telecoms Nig. LTD
                || orgId == 10251 // Otomab and Sons Nigeria Limited
                || orgId == 10905 // JATRAB VENTURES
                || orgId == 10991 // GodfreyBrown LTD
                || orgId == 11001 // Okay Fones Limited
                || orgId == 10915 // Embehie Enterprises
                || orgId == 10923 // Worthy Realtors
                || orgId == 10925 // Holad best International
                || orgId == 10955 // Natural colour press	
                || orgId == 10913 // Sambrix International Limited
                || orgId == 10979 // Is-Hami Construction Limited
                || orgId == 11075 // Adedayo Adedeji
                || orgId == 11039 // Shounix Global Venture
                || orgId == 11077 // CECON24 Systems Limited
                || orgId == 11091 // Correspondence Limited ID
                || orgId == 11055 // Evabest communications
                || orgId == 11127 // Jidekaiji Global Services
                || orgId == 11105 // Daveloret Concepts
                || orgId == 11119 // Just Comestics Ltd
                || orgId == 11143 // Bona Multi Solutions LTD
                || orgId == 11145 // DirimzSammy EnterprisE
                || orgId == 11143 // Bona Multi Solutions LTD
                || orgId == 11089 // Minsendwell Limited
                || orgId == 11175 // Ammid Adex Telecom
                || orgId == 11479 // Pointek Technology Limited
                || orgId == 9947 // DANDOLAS NIGERIA LIMITED
                || orgId == 6423 // Wonder Worth Global Investment Ltd
                || orgId == 11487 // DMANSOCOTEX GLOBAL ENT
                || orgId == 11679 //  CLICK NETWORKS ENTERPRISES
                || orgId == 11677 // LADEHBAK VENTURES
                || orgId == 11727 //Kazra Global Services LTD
                || orgId == 11785 //Comm-spot Concepts
                || orgId == 11701 //ADE OMO ADE DOUBLE CROWN SOLUTIONS
                || orgId == 3693 //Island Computer College
                || orgId == 11837 // Borsh Ventures
                || orgId == 11577 // Coscharlom Intl Associates Ltd
                || orgId == 11603 // INNOFLEX CYBER ENT
                || orgId == 11847 // Abbegold ICT Solutions
                || orgId == 10203 // Binet Resources
                || orgId == 11951 // Rebras Tech Limited
                || orgId == 12041 // Rachma Concept
                || orgId == 12045 // MR. Abusufiyan Sa'ad
                || orgId == 11849 // Repsunny Integrated Services Ltd
                || orgId == 12053 // Darphilz Links Ventures Limited
                || orgId == 12073 // V-dile Solutions
                || orgId == 12109 // Arktronics Travels and Tours Limited
                || orgId == 12141 // NANO NG MULTI SOLUTIONS LTD
                || orgId == 11847 // Abbegold ICT Solutions
                || orgId == 12191 // AMBAI SHA'AGI ATTORNEY
                || orgId == 12193 // Toch Technologies Company
                || orgId == 12183 // ICP Soije Limited
                || orgId == 12243 // Everest Technologies Limited
                || orgId == 12245 // Toch Esmak Technology
                || orgId == 12247 // Toch Libaaxa
                || orgId == 12249 // Toch Bablaw Enterprises
                || orgId == 12225 // HatlonSystems
                || orgId == 12295 // ASSOTEL
                || orgId == 12311 // Techlift Nigeria Limited
                || orgId == 12327 // Pectra Tech Computers Ltd
                || orgId == 12403 // ICP Optimal Device Technology
                || orgId == 12495 // ICP Mimshackog Intl Ltd
                || orgId == 12509 // Communication Facilitators Ltd
                || orgId == 12539 // ANDMARS NIGERIA LTD
                || orgId == 12531 // Intermesh Company
                || orgId == 12555 // Glomos Academy
                || orgId == 9801 // Godmic Nigeria Limited
                || orgId == 12635 // Helaz Global Solutions ltd
                )) {
            // New ICP Contract
            if (!paymentMethod.isEmpty() && !paymentMethod.equalsIgnoreCase("Credit Note") && !channel.startsWith("26P") && !channel.startsWith("27P") && !itemNumber.startsWith("BUN")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }
            int simKitCount = 0;
            int deviceCount = 0;

            for (int i = 0; i < sale.getSaleLines().size(); i++) {
                com.smilecoms.xml.schema.pos.SaleLine otherLine = (com.smilecoms.xml.schema.pos.SaleLine) sale.getSaleLines().get(i);
                String it = otherLine.getInventoryItem().getItemNumber();
                if (simKits.indexOf(it) != -1) {
                    simKitCount += otherLine.getQuantity();
                } else if (deviceKits.indexOf(it) != -1) {
                    deviceCount += otherLine.getQuantity();
                }
            }

            if (simKits.indexOf(itemNumber) != -1) {
                if (simKitCount >= 6 && simKitCount <= 14) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(8d);
                } else if (simKitCount >= 15 && simKitCount <= 50) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                } else if (simKitCount >= 51 && simKitCount <= 100) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(12d);
                } else if (simKitCount >= 101) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(15d);
                }
            }

            if (deviceKits.indexOf(itemNumber) != -1) {
                if (deviceCount >= 6 && deviceCount <= 14) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(8d);
                } else if (deviceCount >= 15 && deviceCount <= 50) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(10d);
                } else if (deviceCount >= 51 && deviceCount <= 100) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(12d);
                } else if (deviceCount >= 101) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(15d);
                }
            }

            if (isAirtime) {
                double discTier1 = 1d / (1d / 0.05d + 1d);
                double discTier2 = 1d / (1d / 0.055d + 1d);
                double discTier3 = 1d / (1d / 0.06d + 1d);
                double discTier4 = 1d / (1d / 0.07d + 1d);

                double tier1Start = 100000d / (1d - discTier1);
                double tier2Start = 250000d / (1d - discTier2);
                double tier3Start = 500000d / (1d - discTier3);
                double tier4Start = 1000000d / (1d - discTier4);

                long airtimeUnits = saleLine.getQuantity();
                if (airtimeUnits >= tier1Start && airtimeUnits < tier2Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier1 * 100d);
                } else if (airtimeUnits >= tier2Start && airtimeUnits < tier3Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier2 * 100d);
                } else if (airtimeUnits >= tier3Start && airtimeUnits < tier4Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier3 * 100d);
                } else if (airtimeUnits >= tier4Start) {
                    dd.setDiscountPercentageOff(0d);
                    dd.setTieredPricingPercentageOff(discTier4 * 100d);
                }
            }

        }

        // TPGW Partner Airtime Discount Setup
        if ((orgId == 6409 // Onecard
                || orgId == 7329 // Contec
                || orgId == 9469 // CitiServe
                || orgId == 9483 // HomeKonnekt Limited
                || orgId == 7511 // Cellulant Nigeria Limited
                || orgId == 9603 // Zinternet Nig Ltd
                || orgId == 9549 // Payconnect Networks Ltd 
                || orgId == 8649 // Golden Rule Communications Limited	
                || orgId == 10617 // TRUSTVAS LIMITED
                || orgId == 5985 // ICP Capricorn Digital
                || orgId == 11609 // ITEX INTEGRATED SERVICES LIMITED
                || orgId == 6339 //TOPUP Africa Ltd
                ) && saleLine.getQuantity() >= 500000 && isAirtime) {

            if (!paymentMethod.isEmpty() && !paymentMethod.equalsIgnoreCase("Credit Note") && !channel.startsWith("26P") && !channel.startsWith("27P")) {
                throw new Exception("Sales to ICPs must use channel starting with 26P or 27P");
            }
            double discTier3 = 1d / (1d / 0.08d + 1d);
            dd.setDiscountPercentageOff(0d);
            dd.setTieredPricingPercentageOff(discTier3 * 100d);
        }

        return dd;
    }

}
