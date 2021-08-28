/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.AccountQuery;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.beans.SaleBean;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.Done;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.InventoryItem;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductInstanceQuery;
import com.smilecoms.commons.sca.PurchaseUnitCreditRequest;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAErr;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.Sale;
import com.smilecoms.commons.sca.SaleLine;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StDone;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StUnitCreditSpecificationLookupVerbosity;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.sca.UnitCreditSpecificationQuery;
import com.smilecoms.commons.sca.beans.UnitCreditBean;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.sra.helpers.SRAException;
import com.smilecoms.sra.helpers.SRAUtil;
import com.smilecoms.sra.helpers.Token;
import com.smilecoms.sra.helpers.paymentgateway.PaymentGatewayManager;
import com.smilecoms.sra.helpers.paymentgateway.GatewayCodes;
import com.smilecoms.sra.helpers.paymentgateway.IPGWTransactionData;
import com.smilecoms.sra.helpers.paymentgateway.PaymentGatewayManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Web Service
 *
 * @author paul
 */
@Path("sales")
public class SalesResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(SalesResource.class);
    private final String PAYMENT_METHOD_PAYMENT_GATEWAY = "Payment Gateway";
    private final String PAYMENT_METHOD_AIRTIME = "Airtime";
    @Context
    private javax.servlet.http.HttpServletRequest request;

    @Path("{identifier}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SaleResource getSale(@PathParam("identifier") String identifier, @QueryParam("by") String by) {
        start(request);
        try {
            if (by == null || by.isEmpty() || by.equalsIgnoreCase("saleId")) {
                return SaleResource.getSaleResourceBySaleId(Integer.valueOf(identifier));
            } else if (by.equalsIgnoreCase("saleLineId")) {
                return SaleResource.getSaleResourceBySaleLineId(Integer.valueOf(identifier));
            } else {
                throw new Exception("Invalid sale lookup type -- " + by);
            }
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public SaleResource processSale(SaleResource sale) {
        start(request);
        try {
            checkPermissions(Permissions.MAKE_SALE);
            return new SaleResource(SaleBean.processSale(sale.getSale()));
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }

    }

    /**
     * 
     * @param sale
     * @param walletProvider
     * @param recipientPhoneNumber
     * @return 
     */
    @Path("airtime")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public SaleResource processSaleAirtime(SaleResource sale, @QueryParam("walletProvider") String walletProvider,
            @QueryParam("recipientPhoneNumber") String recipientPhoneNumber) {
        start(request);
        log.debug("inside processSaleAirtime ");

        SaleResource saleDataResp = null;
        try {
            if (sale.getSale() != null) {
                Sale scaSale = convertSaleBeanToScaSale(sale.getSale());
                
                CustomerQuery cq = new CustomerQuery();
                cq.setCustomerId(sale.getSale().getRecipientCustomerId());
                cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                cq.setProductInstanceResultLimit(1);
                
                Customer customer = SCAWrapper.getAdminInstance().getCustomer(cq);
                double totalAmount = 0.0d;
                double transactionAmount = sale.getSale().getSaleTotalCentsIncl() * 100d;
                String gatewayCode = sale.getSale().getPaymentGatewayCode();

                if (sale.getSale().getSaleLines() != null && !sale.getSale().getSaleLines().isEmpty()) {

                    List<SaleLine> tmpSaleLines = new ArrayList<>();
                    tmpSaleLines.addAll(sale.getSale().getSaleLines());
                    sale.getSale().getSaleLines().clear();//reset
                    int cnt = 0;
                    for (SaleLine line : tmpSaleLines) {
                        if (line.getLineTotalCentsIncl() >= 25d) {//Minimum allowed amount (major unit) per transaction set by Diamond Bank
                            line.setLineNumber(cnt);
                            line.setLineTotalCentsIncl(line.getLineTotalCentsIncl() * 100);
                            line.getInventoryItem().setItemNumber("AIR1004");
                            line.getInventoryItem().setSerialNumber("AIRTIME");
                            line.getInventoryItem().setDescription("Smile Airtime purchased through Clearing Bureau");
                            Double quantity = line.getLineTotalCentsIncl();
                            line.setQuantity(quantity.longValue() / 100);
                            totalAmount += line.getLineTotalCentsIncl();
                            sale.getSale().getSaleLines().add(line);
                            cnt++;
                        }
                    }
                }

                Account account = getAccount(sale.getSale().getRecipientAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
                if (totalAmount != transactionAmount) {
                    transactionAmount = totalAmount;
                }
                for (ServiceInstance si : account.getServiceInstances()) {
                    int organisationId = getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN).getOrganisationId();
                    if (organisationId > 0) {
                        sale.getSale().setRecipientOrganisationId(organisationId);
                        break;
                    }
                }
                scaSale.setSaleTotalCentsIncl(transactionAmount);
                scaSale.setRecipientCustomerId(customer.getCustomerId());
                scaSale.setPaymentMethod(PAYMENT_METHOD_PAYMENT_GATEWAY);
                scaSale.setRecipientPhoneNumber(customer.getAlternativeContact1());
                scaSale.setSaleLocation("");
                scaSale.setRecipientName(customer.getFirstName());
                String channelProp = "MySmileX3Channel";
                if(walletProvider != null && ! walletProvider.isEmpty()){
                    channelProp = "MySmileX3"+walletProvider.trim()+"Channel";
                }
                scaSale.setChannel(BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", channelProp));
                scaSale.setWarehouseId("");
                scaSale.setPromotionCode("");
                scaSale.setTaxExempt(false);
                scaSale.setPaymentGatewayCode(gatewayCode);
                scaSale.setLandingURL("/PaymentGateway.action?processBankTransaction");

                Sale saleData = SCAWrapper.getAdminInstance().generateQuote(scaSale);
                saleData.setPaymentGatewayCode(gatewayCode);
                long accId = Long.parseLong(BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "AccountId"));
                saleData.setSalesPersonAccountId(accId);
                saleData.setSalesPersonCustomerId(1);
                saleData.setPurchaseOrderData("");
                saleData.setExtraInfo("");
                saleData.setPaymentMethod(PAYMENT_METHOD_PAYMENT_GATEWAY);

                Sale processedSale = SCAWrapper.getAdminInstance().processSale(saleData);
                PaymentGatewayManager pgm = null;
                for (GatewayCodes gc : GatewayCodes.values()) {
                    if (gc.getGatewayCode().equals(gatewayCode)) {
                        pgm = PaymentGatewayManagerFactory.createPaymentGatewayManager(gc);
                        log.debug("Gateway code [{}], uses class [{}]", gatewayCode, pgm.getClass().getCanonicalName());
                        break;
                    }
                }

                IPGWTransactionData initialisedTransaction = null;

                try {
                    // For Yo Payments, phoneNumber is wallet linked mobile bumber
                    String phoneNumber = recipientPhoneNumber == null ? customer.getAlternativeContact1() : recipientPhoneNumber;
                    processedSale.setRecipientPhoneNumber(phoneNumber);
                    initialisedTransaction = pgm.startTransaction(processedSale, "AIRTIME");
                } catch (SRAException ex) {
                    log.warn("Error: ", ex);
                    throw processError(ex);
                }

                IPGWTransactionData PGWTransactionData = initialisedTransaction;
                saleDataResp = new SaleResource(new SaleBean(processedSale));
                saleDataResp.getSale().setPaymentTransactionData(PGWTransactionData.getGatewayURLData());
                saleDataResp.getSale().setPaymentGatewayURL(PGWTransactionData.getPaymentGatewayPostURL());
            }
        } catch (Exception ex) {
            if(ex.getMessage().contains("POS-0044")){
                throw processError(SRAUtil.getMesssage("sra.error.duplicate.sale"), "BUSINESS", "SRA-0021", Response.Status.BAD_REQUEST);
            } else {
                throw processError("Server error", "BUSINESS", "SRA-0006", Response.Status.INTERNAL_SERVER_ERROR);
            }
        } finally {
            end();
        }
        
        createSelfcareSaleEvent(saleDataResp.getSale().getSaleId(), Token.getRequestsToken());
        return saleDataResp;
    }
    
    private Account getAccount(long accountNo, StAccountLookupVerbosity verbosity){
        AccountQuery aq = new AccountQuery();
        aq.setAccountId(accountNo);
        aq.setVerbosity(verbosity);
        return SCAWrapper.getAdminInstance().getAccount(aq);
    }
    
    private ProductInstance getProductInstance(int productInstanceId,StProductInstanceLookupVerbosity verbosity){
        ProductInstanceQuery q = new ProductInstanceQuery();
        q.setProductInstanceId(productInstanceId);
        q.setVerbosity(verbosity);
        return SCAWrapper.getAdminInstance().getProductInstance(q);
    }
    
    /**
     * API to buy bundles.
     * @param reqBody
     * @param gatewayCode payment gateway code.
     * @param recipientPhoneNumber recipient phone number.
     * @param walletProvider 
     * @param extraUccId unit credit specification id of extra bundle. 
     * @return 
     */
    @Path("bundle")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Object provisionUnitCreditViaPaymentGateway(PurchaseUnitCreditRequest reqBody,
            @QueryParam("gatewayCode") String gatewayCode, 
            @QueryParam("recipientPhoneNumber") String recipientPhoneNumber,
            @QueryParam("walletProvider") String walletProvider,
            @QueryParam("extraUccId") Integer extraUccId) {
        start(request);
        log.debug("inside provisionUnitCreditViaPaymentGateway");
        
        SaleResource saleDataResp = null;
        
       String gateway = "Airtime".equalsIgnoreCase(reqBody.getPaymentMethod()) ? "Wallet": gatewayCode;
        
        if (gateway == null || gateway.isEmpty()) {
            throw processError(new Exception("gateway code not provided"));
        }
        if(reqBody.getAccountId() < 0 ){
            throw processError(new Exception("invalid account id"));
        }
        
        Account account = SCAWrapper.getAdminInstance().getAccount(reqBody.getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        int customerId = account.getServiceInstances().get(0).getCustomerId();
        
        try {
            CustomerQuery cq = new CustomerQuery();
            cq.setCustomerId(customerId);
            cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            cq.setProductInstanceResultLimit(1);
            Customer customer = SCAWrapper.getAdminInstance().getCustomer(cq);
            
            if("Wallet".equalsIgnoreCase(gateway)){
                return provisionUnitCreditUsingAirtime(reqBody, customer, extraUccId);
            }
            
            UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
            if (reqBody.getUnitCreditSpecificationId() > 0) {
                q.setUnitCreditSpecificationId(reqBody.getUnitCreditSpecificationId());
            }
            if (reqBody.getItemNumber() != null && !reqBody.getItemNumber().isEmpty()) {
                q.setItemNumber(reqBody.getItemNumber());
            }
            
            q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
            UnitCreditSpecification ucs = SCAWrapper.getAdminInstance().getUnitCreditSpecifications(q).getUnitCreditSpecifications().get(0);
            boolean isvalidBuy = isUCSValidToBuy(ucs, reqBody.getAccountId());
            log.debug("isvalidBuy returned [{}]",isvalidBuy);
            if(!isvalidBuy) {
                throw processError("Bundle not allowed", "BUSINESS", "SRA-0763", Response.Status.BAD_REQUEST);
            }
            String mySmileUCDiscount = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "MySmileDiscountPercent");
            
            if (mySmileUCDiscount != null && mySmileUCDiscount.length() > 0) {
                double discountPercOff = Double.parseDouble(mySmileUCDiscount);
                // Apply the discount
                ucs.setPriceInCents(ucs.getPriceInCents() * (1 - discountPercOff / 100d));
                
            }
            
            if (!gatewayExist(gateway)) {
                throw processError(new Exception("Invalid gateway code"));
            }
            log.debug("Going to use SCA integration gateway for unit credit provisioning");

            //setUnitCreditSpecification(ucs);
            Sale sale = new Sale();
            
            sale.setSaleTotalCentsIncl(ucs.getPriceInCents());
            sale.setRecipientCustomerId(customerId);
            sale.setRecipientAccountId(reqBody.getAccountId());
            sale.setPaymentMethod(PAYMENT_METHOD_PAYMENT_GATEWAY);
            
            sale.setRecipientPhoneNumber(customer.getAlternativeContact1());
            /*if (reqBody.getProductInstanceId() > 0) {
                int organisationId = UserSpecificCachedDataHelper.getProductInstance(reqBody.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN).getOrganisationId();
                sale.setRecipientOrganisationId(organisationId);
            }*/
            sale.setSaleLocation("");
            sale.setRecipientName(customer.getFirstName());
            String channelProp = "MySmileX3Channel";
            if(walletProvider != null && ! walletProvider.isEmpty()){
                channelProp = "MySmileX3"+walletProvider.trim()+"Channel";
            }
            log.debug("channelProp is [{}]",channelProp);
            sale.setChannel(BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", channelProp));
            sale.setWarehouseId("");
            sale.setPromotionCode("");
            sale.setTaxExempt(false);
            sale.setPaymentGatewayCode(gatewayCode);
            sale.setLandingURL("/PaymentGateway.action?processBankTransaction");
            
            if (sale.getSaleLines() != null && !sale.getSaleLines().isEmpty()) {
                sale.getSaleLines().clear();//cleanup   
            }
            
            SaleLine line = new SaleLine();
            line.setLineNumber(1);
            line.setQuantity(1);
            line.setLineTotalCentsIncl(ucs.getPriceInCents());
            
            InventoryItem ii = new InventoryItem();
            ii.setItemNumber(ucs.getItemNumber());
            ii.setSerialNumber("");
            line.setInventoryItem(ii);
            sale.getSaleLines().add(line);
            
            //Add Upsize bundles to sale
            if(extraUccId != null && extraUccId > 0){
                
                UnitCreditSpecification extraUCSpec = getUnitCreditSpecification(extraUccId);
                if(extraUCSpec == null || !isSubUCSValid(ucs, extraUCSpec)){
                    throw processError("Invalid Sub Bundle", "BUSINESS", "SRA-0523", Response.Status.BAD_REQUEST);
                }
                addExtraSaleLine(reqBody, sale, extraUCSpec);
            }
            Sale saleData = SCAWrapper.getAdminInstance().generateQuote(sale);
            
            saleData.setPaymentGatewayCode(gatewayCode);
            long accId = Long.parseLong(BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "AccountId"));
            saleData.setSalesPersonAccountId(accId);
            saleData.setSalesPersonCustomerId(1); // Make sale as admin
            saleData.setPurchaseOrderData("");
            saleData.setExtraInfo("");
            saleData.setPaymentMethod(PAYMENT_METHOD_PAYMENT_GATEWAY);
            
            Sale processedSale = SCAWrapper.getAdminInstance().processSale(saleData);
            
            PaymentGatewayManager pgm = null;
            for (GatewayCodes gc : GatewayCodes.values()) {
                if (gc.getGatewayCode().equals(gatewayCode)) {
                    pgm = PaymentGatewayManagerFactory.createPaymentGatewayManager(gc);
                    log.debug("Gateway code [{}], uses class [{}]", gatewayCode, pgm.getClass().getCanonicalName());
                    break;
                }
            }
            
            IPGWTransactionData initialisedTransaction;
            try {
                String phoneNumber = recipientPhoneNumber == null ? customer.getAlternativeContact1() : recipientPhoneNumber;
                if(phoneNumber != null && phoneNumber.startsWith("+")){
                    phoneNumber = phoneNumber.substring(1, phoneNumber.length());
                    log.debug("received recipient phone number :"+phoneNumber);
                }
                processedSale.setRecipientPhoneNumber(phoneNumber);// Used for YoUganda payments
                initialisedTransaction = pgm.startTransaction(processedSale, ucs.getName());
            } catch (Exception ex) {
                log.error("error while startTransaction",ex);
                throw processError(new Exception("payment.gateway.manager.initialisation.failure"));
            }
            IPGWTransactionData PGWTransactionData = initialisedTransaction;
            saleDataResp = new SaleResource(new SaleBean(processedSale));
            saleDataResp.getSale().setPaymentTransactionData(PGWTransactionData.getGatewayURLData());
            saleDataResp.getSale().setPaymentGatewayURL(PGWTransactionData.getPaymentGatewayPostURL());
        } catch (SCABusinessError ex) {
            log.error("error in provisionUnitCreditViaPaymentGateway", ex);
            if(ex.getMessage().contains("POS-0044")){
                throw processError(SRAUtil.getMesssage("sra.error.duplicate.sale"), "BUSINESS", "SRA-0021", Response.Status.BAD_REQUEST);
            } else {
                throw processError("Server error", "BUSINESS", "SRA-0006", Response.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (SRAException ex){
           log.error("SRAException :",ex);
           throw ex;
        }
        catch (Exception e) {
            log.error("error in provisionUnitCreditViaPaymentGateway", e);
            throw processError("Server error", "BUSINESS", "SRA-0046", Response.Status.INTERNAL_SERVER_ERROR);
        }
        
        createSelfcareSaleEvent(saleDataResp.getSale().getSaleId(), Token.getRequestsToken());
        
        return saleDataResp;
    }
    
    private Sale convertSaleBeanToScaSale(SaleBean sale){
        Sale scaSale = new Sale();
        scaSale.setPaymentMethod(sale.getPaymentMethod());
        scaSale.setSalesPersonCustomerId(sale.getSalesPersonCustomerId());
        scaSale.setRecipientCustomerId(sale.getRecipientCustomerId());
        scaSale.setRecipientAccountId(sale.getRecipientAccountId());
        scaSale.setSalesPersonAccountId(sale.getSalesPersonAccountId());
        scaSale.setRecipientOrganisationId(sale.getRecipientOrganisationId());
        scaSale.setAmountTenderedCents(sale.getAmountTenderedCents());
        scaSale.setTillId(sale.getTillId());
        scaSale.setSaleTotalCentsIncl(sale.getSaleTotalCentsIncl());
        scaSale.setChannel(sale.getChannel());
        scaSale.setWarehouseId(sale.getWarehouseId());
        scaSale.setPromotionCode(sale.getPromotionCode());
        scaSale.setPaymentTransactionData(sale.getPaymentTransactionData());
        scaSale.setPurchaseOrderData(sale.getPurchaseOrderData());
        scaSale.setCreditAccountNumber(sale.getCreditAccountNumber());
        scaSale.setTaxExempt(sale.isTaxExempt());
        scaSale.setTenderedCurrency(sale.getTenderedCurrency());
        scaSale.setExtraInfo(sale.getExtraInfo());
        scaSale.setWithholdingTaxRate(sale.getWithholdingTaxRate());
        scaSale.getSaleLines().addAll(sale.getSaleLines());
        scaSale.setCallbackURL(sale.getCallbackURL());
        scaSale.setPaymentGatewayCode(sale.getPaymentGatewayCode());
        scaSale.setLandingURL(sale.getLandingURL());
        return scaSale;
    }
    
    private boolean gatewayExist(String gatewayCode) {
        try {
            String subProperty = BaseUtils.getSubProperty("env.pgw." + gatewayCode.toLowerCase() + ".config", "GatewayCode");
            return true;
        } catch (Exception x) {
            log.warn("No payment gateway configuration corresponding to GatewayCode: {}", gatewayCode);
        }
        return false;
    }

    private boolean UCHasExtraUpsizeConfig(UnitCreditSpecification ucs) {
        String val = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "ExtraUpsizeSpecIds");
        return !(val == null || val.length() <= 0);
    }
    
    private Object provisionUnitCreditUsingAirtime(PurchaseUnitCreditRequest reqBody, Customer customer, Integer extraUccId) {
        log.debug("buying databundle using airtime");
        if (reqBody.getPaidByAccountId() == 0) {
            reqBody.setPaidByAccountId(reqBody.getAccountId());
        }

        UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
        if (reqBody.getUnitCreditSpecificationId() > 0) {
            q.setUnitCreditSpecificationId(reqBody.getUnitCreditSpecificationId());
        }
        if (reqBody.getItemNumber() != null && !reqBody.getItemNumber().isEmpty()) {
            q.setItemNumber(reqBody.getItemNumber());
        }
        q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
        UnitCreditSpecification ucs = SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(q).getUnitCreditSpecifications().get(0);
        boolean isvalidBuy = isUCSValidToBuy(ucs, reqBody.getAccountId());
        log.debug("isvalidBuy returned [{}]",isvalidBuy);
            if(!isvalidBuy) {
                throw processError("Bundle not allowed", "BUSINESS", "SRA-0763", Response.Status.BAD_REQUEST);
        }
        String mySmileUCDiscount = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "MySmileDiscountPercent");
        if (mySmileUCDiscount != null && mySmileUCDiscount.length() > 0) {
            double discountPercOff = Double.parseDouble(mySmileUCDiscount);
            // Apply the discount
            ucs.setPriceInCents(ucs.getPriceInCents() * (1 - discountPercOff / 100d));

        }
        boolean isUCSAllowedForSale = isUCSAllowedForCustomer(ucs.getPurchaseRoles());
        if (!isUCSAllowedForSale) {
            log.error("unit credit id [{}] is not allowed for customer id [{}]",ucs.getItemNumber(),customer.getCustomerId());
            throw processError(new Exception("unit credit not allowed"));
        }
        Sale scaSale = null;
        
        try {
            boolean provisionUpsize = true;

            reqBody.setNumberToPurchase(1);
            reqBody.setItemNumber(ucs.getItemNumber());
            reqBody.setPaymentMethod(PAYMENT_METHOD_AIRTIME);
            reqBody.setCreditAccountNumber("");
            reqBody.setChannel(BaseUtils.getSubProperty("env.mysmile.airtime.config", "MySmileX3Channel"));

            if (!provisionUpsize) {
                try {
                    SCAWrapper.getUserSpecificInstance().purchaseUnitCredit(reqBody);
                } catch (SCAErr ex) {
                    if (ex.getErrorCode().equalsIgnoreCase("BM-0001") || ex.getErrorCode().equalsIgnoreCase("BM-0007") || ex.getErrorCode().equalsIgnoreCase("POS-0057")) {

                        Account account = UserSpecificCachedDataHelper.getAccount(reqBody.getPaidByAccountId(), StAccountLookupVerbosity.ACCOUNT);
                        //CREDIT_BARRED = 1; // e.g. receiving me2u transfers, topping up etc
                        //DEBIT_BARRED = 2; // e.g. sending me2u transfers, buying bundles etc
                        //UNIT_CREDIT_CHARGE_BARRED = 4; // e.g. in bundle usage
                        //MONETARY_CHARGE_BARRED = 8; // e.g. out of bundle usage (does not stop rate group 9000 charges
                        if (account.getStatus() == 14 || account.getStatus() == 223) {
                            log.error("Account status does not allow operation.account status : [{}]",account.getStatus());
                            throw processError(new Exception("Account status does not allow operation"));
                        }
                        log.error("You do not have sufficient SmileAirtime to acccount number [{}]",account.getAccountId());
                        throw processError(SRAUtil.getMesssage("sra.error.sale.insufficient.airtime"), "BUSINESS", "SRA-0021", Response.Status.BAD_REQUEST);
                    }
                    log.error("throwing exception :",ex);
                    throw ex;
                }

            } else {
                Sale sale = new Sale();

                sale.setSaleTotalCentsIncl(ucs.getPriceInCents());
                sale.setRecipientCustomerId(customer.getCustomerId());
                sale.setPaymentMethod(PAYMENT_METHOD_AIRTIME);
                sale.setRecipientPhoneNumber(customer.getAlternativeContact1());

                if (reqBody.getProductInstanceId() > 0) {
                    int organisationId = UserSpecificCachedDataHelper.getProductInstance(reqBody.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN).getOrganisationId();
                    sale.setRecipientOrganisationId(organisationId);

                }

                sale.setSaleLocation("");
                sale.setRecipientAccountId(reqBody.getAccountId());
                sale.setRecipientName(customer.getFirstName());
                sale.setChannel(BaseUtils.getSubProperty("env.mysmile.airtime.config", "MySmileX3Channel"));
                sale.setWarehouseId("");
                sale.setPromotionCode("");
                sale.setTaxExempt(false);
                sale.setSalesPersonAccountId(reqBody.getPaidByAccountId());
                sale.setSalesPersonCustomerId(1); // Make sale as admin
                sale.setPurchaseOrderData("");
                sale.setExtraInfo("");
                sale.setChangeCents(0);
                sale.setAmountTenderedCents(0);
                sale.setPaymentTransactionData("");
                sale.setWithholdingTaxRate(0);
                sale.setCreditAccountNumber("");

                SaleLine line = new SaleLine();
                line.setLineNumber(1);
                line.setQuantity(1);
                line.setLineTotalCentsIncl(ucs.getPriceInCents());
                line.setProvisioningData("ProductInstanceId=" + reqBody.getProductInstanceId() + "\r\nDaysGapBetweenStart=" + reqBody.getDaysGapBetweenStart());

                InventoryItem ii = new InventoryItem();
                ii.setItemNumber(ucs.getItemNumber());
                ii.setSerialNumber("");
                line.setInventoryItem(ii);
                sale.getSaleLines().add(line);

                //Add additional bundles to sale
                if (extraUccId != null && extraUccId > 0) {
                    UnitCreditSpecification extraUCSpec = getUnitCreditSpecification(extraUccId);
                    if(extraUCSpec == null || ! isSubUCSValid(ucs, extraUCSpec)){
                        throw processError("Invalid Sub Bundle", "BUSINESS", "SRA-0523", Response.Status.BAD_REQUEST);
                    }
                    addExtraSaleLine(reqBody, sale, extraUCSpec);
                }
                sale.setSaleTotalCentsIncl(sale.getSaleTotalCentsIncl());
                try {
                    scaSale = SCAWrapper.getUserSpecificInstance().processSale(sale);
                } catch (SCAErr ex) {
                    if (ex.getErrorCode().equalsIgnoreCase("BM-0001") || ex.getErrorCode().equalsIgnoreCase("BM-0007") || ex.getErrorCode().equalsIgnoreCase("POS-0057")) {

                        Account account = UserSpecificCachedDataHelper.getAccount(reqBody.getPaidByAccountId(), StAccountLookupVerbosity.ACCOUNT);
                        if (account.getStatus() == 14 || account.getStatus() == 223) {
                            log.error("Account status does not allow operation.account status : [{}]",account.getStatus());
                            throw processError(new Exception("Account status does not allow operation"));
                        
                        }
                        log.error("You do not have sufficient SmileAirtime to acccount number [{}]",account.getAccountId());
                        throw processError(SRAUtil.getMesssage("sra.error.sale.insufficient.airtime"), "BUSINESS", "SRA-0021", Response.Status.BAD_REQUEST);
                    }
                    throw ex;
                }

            }

        } catch (SCABusinessError e) {
            throw e;
        }
        
        if (scaSale != null){
            createSelfcareSaleEvent(scaSale.getSaleId(), Token.getRequestsToken());
        }
        Done done = new Done();
        done.setDone(StDone.TRUE);
        return done;
    }

    private boolean isUCSAllowedForCustomer(String purchaseRoles) {
        StringTokenizer stValues = new StringTokenizer(purchaseRoles, "\r\n");
        while (stValues.hasMoreTokens()) {
            String role = stValues.nextToken().trim();
            if("Customer".equalsIgnoreCase(role)){
                return true;
            }
        }
        return false;
    }

    private void createSelfcareSaleEvent(int saleId, Token token) {
        if (token.getVersion() == 2.0) {
            Event eventData = new Event();
            eventData.setEventType("SRA");
            eventData.setEventSubType("Selfcare_sale");
            eventData.setEventKey(String.valueOf(saleId));
            eventData.setEventData(String.valueOf(token.getCustomerId()));
            SCAWrapper.getAdminInstance().createEvent(eventData);
        }
    }

    private boolean isSubUCSValid(UnitCreditSpecification ucs, UnitCreditSpecification extraUcs) {
        log.debug("called isSubUCSValid");
         String allowedUCSpecIds = Utils.getValueFromCRDelimitedAVPString(extraUcs.getConfiguration(), "CanBeSoldWhenSpecIDExist"); 
         log.debug("allowedUCSpecIds is [{}]",allowedUCSpecIds);
         log.debug("getUnitCreditSpecificationId is [{}]", ucs.getUnitCreditSpecificationId());
         String[] upSizeIDs = allowedUCSpecIds.split(",");
         for(String upsizeid : upSizeIDs){
             if(upsizeid.equals(String.valueOf(ucs.getUnitCreditSpecificationId()))){
                 log.debug("returning [{}]",true);
                 return true;
             }
         }
         return false;
    }
    
    private UnitCreditSpecification getUnitCreditSpecification(int ucid){
        log.debug("called getUnitCreditSpecification");
        UnitCreditSpecificationQuery ucsq = new UnitCreditSpecificationQuery();
        ucsq.setUnitCreditSpecificationId(ucid);
        ucsq.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
        UnitCreditSpecification upsizeUCSpec = SCAWrapper.getAdminInstance().getUnitCreditSpecifications(ucsq).getUnitCreditSpecifications().get(0);
        return upsizeUCSpec;
    }

    private void addExtraSaleLine(PurchaseUnitCreditRequest reqBody, Sale sale, UnitCreditSpecification extraUCSpec) {
        log.debug("called addExtraSaleLine");
        if (extraUCSpec == null) {
            throw processError("Invalid Sub Bundle", "BUSINESS", "SRA-0523", Response.Status.BAD_REQUEST);
        }
        String mySmileUpsizeUCDiscount = Utils.getValueFromCRDelimitedAVPString(extraUCSpec.getConfiguration(), "MySmileDiscountPercent");
        if (mySmileUpsizeUCDiscount != null && mySmileUpsizeUCDiscount.length() > 0) {
            double upsizeDiscountPercOff = Double.parseDouble(mySmileUpsizeUCDiscount);
            extraUCSpec.setPriceInCents(extraUCSpec.getPriceInCents() * (1 - upsizeDiscountPercOff / 100d));
        }
        SaleLine line = new SaleLine();
        line.setLineNumber(2);
        line.setQuantity(1);
        line.setLineTotalCentsIncl(extraUCSpec.getPriceInCents());
        line.setProvisioningData("ProductInstanceId=" + reqBody.getProductInstanceId() + "\r\nDaysGapBetweenStart=" + reqBody.getDaysGapBetweenStart());

        InventoryItem ii = new InventoryItem();
        ii.setItemNumber(extraUCSpec.getItemNumber());
        ii.setSerialNumber("");
        line.setInventoryItem(ii);
        sale.getSaleLines().add(line);

        sale.setSaleTotalCentsIncl(sale.getSaleTotalCentsIncl() + extraUCSpec.getPriceInCents());
    }

    private boolean isUCSValidToBuy(UnitCreditSpecification ucs, long accountId) {
        log.debug("inside isUCSValidToBuy");
        String canBeSoldWhenSpecIDExist = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "CanBeSoldWhenSpecIDExist");
        if(canBeSoldWhenSpecIDExist == null || canBeSoldWhenSpecIDExist.trim().isEmpty()){
            return true;
        }
        String[] specIDs = canBeSoldWhenSpecIDExist.split(",");
        List<Integer> specIdList = new ArrayList<>();
        for(String str : specIDs){
            specIdList.add(Integer.valueOf(str.trim()));
        }
        AccountResource ar = new AccountResource(accountId);
        for(UnitCreditBean ucb : ar.getAccount().getUnitCredits()){
           if(specIdList.contains(ucb.getUnitCreditSpecificationId())){
               log.debug("[{}] account have [{}]",accountId,ucb.getUnitCreditSpecificationId() );
               return true;
           }
        }
        log.debug("{} can be onnly sold if account have {}",ucs.getUnitCreditSpecificationId(), specIDs);
        return false;
    }

}
