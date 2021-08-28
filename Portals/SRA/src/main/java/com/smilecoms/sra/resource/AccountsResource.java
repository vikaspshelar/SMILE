/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.localisation.PDFUtils;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.AccountHistory;
import com.smilecoms.commons.sca.AccountHistoryQuery;
import com.smilecoms.commons.sca.AccountQuery;
import com.smilecoms.commons.sca.BalanceTransferData;
import com.smilecoms.commons.sca.Done;
import com.smilecoms.commons.sca.PrepaidStrip;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.Reservation;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAErr;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.Sale;
import com.smilecoms.commons.sca.SaleLine;
import com.smilecoms.commons.sca.SalesList;
import com.smilecoms.commons.sca.SalesQuery;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.SplitUnitCreditData;
import com.smilecoms.commons.sca.StAccountHistoryVerbosity;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StDone;
import com.smilecoms.commons.sca.StSaleLookupVerbosity;
import com.smilecoms.commons.sca.TransactionRecord;
import com.smilecoms.commons.sca.UnitCreditInstance;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.sra.model.ShareUnitCreditRequest;
import com.smilecoms.sra.model.AirtimeShareRequest;
import com.smilecoms.sra.model.AccountHistoryBean;
import com.smilecoms.commons.sca.beans.AccountBean;
import com.smilecoms.commons.sca.beans.CustomerBean;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.beans.ProductBean;
import com.smilecoms.commons.sca.beans.ServiceBean;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.commons.stripes.InsufficientPrivilegesException;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.sra.helpers.RequestParser;
import com.smilecoms.sra.helpers.SRAUtil;
import com.smilecoms.sra.helpers.Token;
import com.smilecoms.sra.model.AccountProduct;
import com.smilecoms.sra.model.ShareAirtimeResponse;
import com.smilecoms.sra.model.ShareUnitCreditResponse;
import com.smilecoms.sra.model.TransactionRecordBean;
import com.smilecoms.sra.model.VoucherRedeemResp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Web Service
 *
 * @author paul
 */
@Path("accounts")
public class AccountsResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(AccountsResource.class);
    @Context
    private javax.servlet.http.HttpServletRequest request;

    /**
     * Get account details for given account ID
     * @param accountId
     * @return 
     */
    @Path("{accountId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AccountResource getAccount(@PathParam("accountId") long accountId) {
        start(request);

        try {
            checkPermissions(Permissions.VIEW_ACCOUNT);
            return new AccountResource(accountId);
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }

    /**
     * Get details of Account with history for a given account id.
     * @param accountId customers account id
     * @param dateFrom starting date
     * @param dateTo end date
     * @param resultLimit number of records
     * @return 
     */
    @Path("{accountId}/history")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AccountResource getAccountWithHistory(@PathParam("accountId") long accountId, @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo, @QueryParam("resultLimit") int resultLimit) {
        start(request);
        try {
            if (resultLimit <= 0) {
                resultLimit = 200;
            }
            return AccountResource.getAccountResourceWithHistory(accountId, Utils.getStringAsDate(dateFrom, "yyyyMMdd", Utils.getPastDate(Calendar.MONTH, 1)), Utils.getEndOfDay(Utils.getStringAsDate(dateTo, "yyyyMMdd", new Date())), resultLimit);
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }
    
    @Path("{accountId}/allhistory")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AccountHistoryBean getAccountAllHistory(@PathParam("accountId") long accountId,
            @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo, 
            @QueryParam("recordType") String type, @QueryParam("resultLimit") int resultLimit,
            @QueryParam("offset") int offset) {
        start(request);
        try {
            if (resultLimit <= 0) {
                resultLimit = 300;
            }
            AccountHistoryQuery ahq = new AccountHistoryQuery();
            ahq.setAccountId(accountId);
            Date startDate = Utils.getStringAsDate(dateFrom, "yyyyMMdd", Utils.getPastDate(Calendar.MONTH, 1));
            Date endDate   =   Utils.getEndOfDay(Utils.getStringAsDate(dateTo, "yyyyMMdd", new Date()));
            ahq.setDateFrom(Utils.getDateAsXMLGregorianCalendar(startDate));
            ahq.setDateTo(Utils.getDateAsXMLGregorianCalendar(endDate));
            ahq.setResultLimit(resultLimit);
            ahq.setOffset(offset);
            if(type != null && !type.isEmpty()){
                String trType = getTrType(type);
                ahq.setTransactionType(trType);
            }
            ahq.setVerbosity(StAccountHistoryVerbosity.RECORDS);
            AccountHistory ah = SCAWrapper.getUserSpecificInstance().getAccountHistory(ahq);
            AccountHistoryBean ahb = convertAccountHistory(ah, type);
            return ahb;
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }
    
    @Path("{accountId}/downloadhistory")
    @GET
    @Produces("application/pdf")
    public Response downAccountAllHistory(@PathParam("accountId") long accountId,
            @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo, 
            @QueryParam("recordType") String type, @QueryParam("resultLimit") int resultLimit) {
        start(request);
        log.debug("downAccountAllHistory call time [{}]", SRAUtil.getCurrentDateTime());
        try {
            if (resultLimit <= 0) {
                resultLimit = 1000;
            }
             
            AccountHistoryQuery ahq = new AccountHistoryQuery();
            ahq.setAccountId(accountId);
            Date startDate = Utils.getStringAsDate(dateFrom, "yyyyMMdd", Utils.getPastDate(Calendar.MONTH, 1));
            Date endDate = Utils.getEndOfDay(Utils.getStringAsDate(dateTo, "yyyyMMdd", new Date()));
            ahq.setDateFrom(Utils.getDateAsXMLGregorianCalendar(startDate));
            ahq.setDateTo(Utils.getDateAsXMLGregorianCalendar(endDate));
            ahq.setResultLimit(resultLimit);
            if(type != null && !type.isEmpty()){
                String trType = getTrType(type);
                log.debug("trType is : "+trType);
                ahq.setTransactionType(trType);
            }
            ahq.setVerbosity(StAccountHistoryVerbosity.RECORDS);
            AccountHistory ah = SCAWrapper.getUserSpecificInstance().getAccountHistory(ahq);
            String xmlHistory = getXmlHistory(ah, type);
            byte[] pdf = generateHistoryPdf(xmlHistory);
            String filename = "smile-accounthistory-"+dateFrom+"-"+dateTo+".pdf";
            log.debug("time before writing response [{}]", SRAUtil.getCurrentDateTime());
            ResponseBuilder response = Response.ok((Object) pdf);           
            response.header("Content-Disposition",
                "attachment; filename=\"" + filename + "\"");
            log.debug("time before return of downAccountAllHistory [{}]", SRAUtil.getCurrentDateTime());
            return response.build();
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }
    
    /**
     * 
     * @param accountId
     * @param verbosityString
     * @return 
     */
    @Path("{accountId}/products")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<AccountProduct> getAccountProduct(@PathParam("accountId") int accountId, @QueryParam("verbosity") String verbosityString) {
        start(request);
        try {
            log.debug(("inside getAccountProduct"));
            Token callersToken = Token.getRequestsToken();
            int customerId = callersToken.getCustomerId();
            List<AccountProduct> productList = new ArrayList<>();
            CustomerResource customerR = CustomerResource.getCustomerResourceById(customerId, StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
            CustomerBean customer = customerR.getCustomer();
            List<ProductBean> products = customer.getProducts();
            String iccId = null;
            ProductBean product = null;
            for (ProductBean pb : products) {
                for (ServiceBean sb : pb.getServices()) { 
                    if(sb.getAccountId() != accountId){
                        continue;
                    }
                    product = pb;
                    iccId = pb.getProductICCID();
                    break;
                }
            }
            log.debug("iccId is : "+iccId);
            if(iccId == null){
                log.debug("No device available for account [{}]", accountId);
                return productList;
            }
            
            if(product == null){
                log.debug("No products founds for account [{}]",accountId);
                return productList;
            }
            
            SalesQuery sq = new SalesQuery();
            sq.setSerialNumber(iccId);
            sq.setVerbosity(StSaleLookupVerbosity.SALE_LINES);
            //sq.setRecipientCustomerId(customerId);
            
            SalesList sl = SCAWrapper.getAdminInstance().getSales(sq);
            if(sl == null || sl.getSales() == null || sl.getSales().isEmpty()){
                log.debug("No sale done for iccId [{}] and customer customerId [{}]",iccId, customerId);
                return productList;
            }
            for(Sale sale : sl.getSales()){
                if(!"PD".equals(sale.getStatus()) || !"ST".equals(sale.getStatus()) || !"QT".equals(sale.getStatus())){
                    continue;
                }
                List<SaleLine> saleLines = sale.getSaleLines();
                for(SaleLine sline : saleLines){
                    addSaleDetailToProductList(productList, sale, sline, product);
                    
                    for(SaleLine saleSubLine : sline.getSubSaleLines()){
                        addSaleDetailToProductList(productList, sale, saleSubLine,product);
                    }
                }
            }
            return productList;
            
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }
    
 
    /**
     * 
     * @param accountId
     * @param friendlyName
     * @return 
     */
    @Path("{accountId}/alias")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Done updateProductAlias(@PathParam("accountId") int accountId, @QueryParam("friendlyName") String friendlyName) {
        start(request);
        if(friendlyName == null|| friendlyName.trim().isEmpty()){
            throw processError("Alias not provided","BUSINESS","SRA-00693", Response.Status.BAD_REQUEST);
        }
        Done done = new Done();
        done.setDone(StDone.FALSE);
        try {
            log.debug(("inside getAccountProduct"));
            Token callersToken = Token.getRequestsToken();
            int customerId = callersToken.getCustomerId();
            CustomerResource customerR = CustomerResource.getCustomerResourceById(customerId, StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
            CustomerBean customer = customerR.getCustomer();
            List<ProductBean> products = customer.getProducts();
            ProductBean product = null;
            for (ProductBean pb : products) {
                for (ServiceBean sb : pb.getServices()) { 
                    if(sb.getAccountId() != accountId){
                        continue;
                    }
                    product = pb;
                    String nowfriendlyName = product.getFriendlyName();
                    if(nowfriendlyName != null && nowfriendlyName.equals(friendlyName)){
                        return new Done();
                    }
                    ProductOrder pd = new ProductOrder();
                    pd.setAction(StAction.UPDATE);
                    pd.setCustomerId(pb.getCustomerId());
                    pd.setProductInstanceId(pb.getProductInstanceId());
                    log.debug("product instance id :"+pb.getProductInstanceId());
                    pd.setOrganisationId(pb.getOrganisationId());
                    pd.setSegment(pb.getSegment());                   
                    pd.setFriendlyName(friendlyName);
                    done = SCAWrapper.getUserSpecificInstance().processOrder(pd);
                    break;
                }
            }
            
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
        return done;
    }
    
    private void addSaleDetailToProductList(List<AccountProduct> accProList,Sale sale, SaleLine sline, ProductBean product){
        AccountProduct accPro =  new AccountProduct();
        if(sline.getInventoryItem().getItemNumber().startsWith("BUN") ||
               sline.getInventoryItem().getItemNumber().startsWith("KIT") ){
            return;
        }
        accPro.setDeviceName(sline.getInventoryItem().getItemNumber());
        accPro.setDescription(sline.getInventoryItem().getDescription());
        accPro.setPurchageDate(SRAUtil.convertTime(Utils.getJavaDate(sale.getSaleDate()).getTime()));
        accPro.setSerialNumber(sline.getInventoryItem().getSerialNumber());
        accPro.setActivationDate(SRAUtil.convertTime(product.getFirstActivityDate()));
        accPro.setLastUsedDate(SRAUtil.convertTime(product.getLastActivityDate()));
        accPro.setSmileNumber(product.getProductPhoneNumber());
        accProList.add(accPro);
    }
    private AccountHistoryBean convertAccountHistory(AccountHistory ah, String recordtype){
        AccountHistoryBean ahb = new AccountHistoryBean();
        List<TransactionRecordBean> trbList = new ArrayList<>();
        for(TransactionRecord tr : ah.getTransactionRecords()) {
            
            if(recordtype != null && recordtype.equals("airtime.purchase")){
                if(tr.getTransactionType().contains("m2u")){
                    continue;
                }
            }
            TransactionRecordBean trb =  new TransactionRecordBean();
            trb.setAccountBalanceRemainingInCents(tr.getAccountBalanceRemainingInCents());
            trb.setAccountId(tr.getAccountId());
            trb.setStartDate(tr.getStartDate().toXMLFormat());
            trb.setEndDate(tr.getEndDate().toString());
            trb.setChargingDetail(tr.getChargingDetail());
            trb.setDescription(tr.getDescription());
            if((tr.getTransactionType().contains("txtype.uc.split") || 
                    tr.getTransactionType().contains("txtype.tfr"))
                    && !tr.getDestination().isEmpty()){
                trb.setDestination(SRAUtil.getSmileVoiceNumber(Long.valueOf(tr.getDestination())));
            } else {
                trb.setDestination(tr.getDestination());
            }
            
            trb.setExtTxId(tr.getExtTxId());
            trb.setInfo(tr.getInfo());
            trb.setIpAddress(tr.getIPAddress());
            trb.setLocation(tr.getLocation());
            trb.setServiceInstanceId(tr.getServiceInstanceId());
            trb.setServiceInstanceIdentifier(tr.getServiceInstanceIdentifier());
            trb.setSource(tr.getSource());
            trb.setStatus(tr.getStatus());
            trb.setTermCode(tr.getTermCode());
            trb.setTotalUnits(tr.getTotalUnits());
            trb.setTransactionRecordId(tr.getTransactionRecordId());
            trb.setTransactionType(tr.getTransactionType());
            trb.setUnitCreditBaselineUnits(tr.getUnitCreditBaselineUnits());
            trb.setUnitCreditUnits(tr.getUnitCreditUnits());
            double amountInCents = getAmountInCents(tr);
            trb.setAmountInCents(amountInCents);
            trbList.add(trb);
        }
        
        ahb.setTransactionRecords(trbList);
        ahb.setResultsReturned(trbList.size());
        
        return ahb;
    }
    
    private String getTrType(String type){
        if(type == null || type.isEmpty()){
            return null;
        }
        if("uc".equalsIgnoreCase(type)) {
            return "txtype.uc.purchase";
        } else if("airtime.share".equalsIgnoreCase(type)){
            return "txtype.tfr.%.m2u";
        } else if("airtime.purchase".equalsIgnoreCase(type)){
            return "txtype.tfr";
        }
        return type;
    }

    /**
     * Get account details with summary. 
     * @param accountId
     * @param dateFrom
     * @param dateTo
     * @return 
     */
    @Path("{accountId}/summary")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AccountResource getAccountWithSummary(@PathParam("accountId") long accountId, @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
        start(request);
        try {
            return AccountResource.getAccountResourceWithSummary(accountId, Utils.getStringAsDate(dateFrom, "yyyyMMdd", Utils.getPastDate(Calendar.MONTH, 1)), Utils.getEndOfDay(Utils.getStringAsDate(dateTo, "yyyyMMdd", new Date())));
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }
    
    /**
     * 
     * @param accountId
     * @param dateFrom
     * @param dateTo
     * @param resultLimit
     * @return 
     */
    @Path("{accountId}/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AccountResource getAccountWithHistoryAndSummary(@PathParam("accountId") long accountId, @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo, @QueryParam("resultLimit") int resultLimit) {
        start(request);
        try {
            return AccountResource.getAccountResourceWithHistoryAndSummary(accountId, Utils.getStringAsDate(dateFrom, "yyyyMMdd", Utils.getPastDate(Calendar.MONTH, 1)), Utils.getEndOfDay(Utils.getStringAsDate(dateTo, "yyyyMMdd", new Date())), resultLimit);
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }

    /**
     * Update customer account
     * @param accountId
     * @param account
     * @return 
     */
    @PUT 
    @Path("{accountId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AccountResource modifyAccount(@PathParam("accountId") int accountId, AccountResource account) {
        start(request);
        try {
            account.getAccount().setAccountId(accountId);
            return new AccountResource(AccountBean.modifyAccount(account.getAccount()));
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }
    
    /**
     * Adding Items to the account
     * @param accountId
     * @param formParams
     * @param uriInfo
     * @return 
     */
    @POST
    @Path("{accountId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AccountResource addThingsToAccount(@PathParam("accountId") long accountId, MultivaluedMap<String, String> formParams, @Context UriInfo uriInfo) {
        start(request);
        try {
            RequestParser parser = new RequestParser(formParams, uriInfo);
            switch (parser.getMethod()) {
                case "balanceTransfer":
                    if (parser.getParamAsLong("toAccountId", 0) > 0) {
                        return new AccountResource(AccountBean.doBalanceTransfer(accountId, parser.getParamAsLong("toAccountId", 0), parser.getParamAsDouble("amount") * 100));
                    } else if (parser.getParamAsString("toPhoneNumber") != null) {
                        return new AccountResource(AccountBean.doBalanceTransfer(accountId, parser.getParamAsString("toPhoneNumber"), parser.getParamAsDouble("amount") * 100));
                    } else {
                        throw new Exception("Insufficient data for balance transfer. Must specify to account or to phone number");
                    }
                    
                case "provisionUnitCredit":
                    return new AccountResource(AccountBean.provisionUnitCredit(accountId,
                            parser.getParamAsInt("unitCreditSpecificationId"),
                            parser.getParamAsInt("productInstanceId"),
                            parser.getParamAsInt("numberToPurchase", 1),
                            parser.getParamAsInt("daysGapBetweenStart", 0)));
                case "redeemVoucher":
                    return new AccountResource(AccountBean.redeemVoucher(0, accountId, parser.getParamAsString("voucherCode")));
                default:
                    throw new Exception("Invalid method type -- " + parser.getMethod());
            }

        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }
    
    /**
     * API to redeem pre-paid voucher strip
     * @param accountId Account ID of customer
     * @param voucherCode pre-paid strip voucher code
     * @return Account details
     */
    @POST
    @Path("{accountId}/redeem/{voucherCode}")
    @Produces(MediaType.APPLICATION_JSON)
    public AccountResource redeemVoucher(@PathParam("accountId") long accountId, @PathParam("voucherCode") String voucherCode) {
        start(request);
        try {
            return new AccountResource(AccountBean.redeemVoucher(accountId, accountId, voucherCode));
        } catch (Exception ex) {
            if(ex.getMessage().contains("Invalid strip PIN")){
                throw processError(SRAUtil.getMesssage("sra.error.invalid.voucher.pin"),"BUSINESS", "SRA-0013",Response.Status.BAD_REQUEST);
            } else {
                throw processError(SRAUtil.getMesssage("sra.error.redeem.voucher.error"),"BUSINESS", "SRA-0011",Response.Status.BAD_REQUEST);
            }
        } finally {
            end();
        }
    }
    
    @GET
    @Path("{accountId}/voucher/{voucherCode}")
    @Produces(MediaType.APPLICATION_JSON)
    public PrepaidStrip getVoucher(@PathParam("accountId") long accountId, @PathParam("voucherCode") String voucherCode) {
        start(request);
        try {
            PrepaidStrip prepaidStrip = AccountBean.getVoucher(voucherCode, accountId);
            return prepaidStrip;
        } catch (Exception ex) {
            log.error("error in get voucher ", ex);
            throw processError(SRAUtil.getMesssage("sra.error.invalid.voucher.pin"),"BUSINESS", "SRA-0013",Response.Status.BAD_REQUEST);
            
        } finally {
            end();
        }
    }
    
    /**
     * API to redeem pre-paid voucher strip
     * @param accountId Account ID of customer
     * @param voucherCode pre-paid strip voucher code
     * @return Account details
     */
    @POST
    @Path("{accountId}/redeemvoucher/{voucherCode}")
    @Produces(MediaType.APPLICATION_JSON)
    public VoucherRedeemResp voucherRedeem(@PathParam("accountId") long accountId, @PathParam("voucherCode") String voucherCode) {
        start(request);
        try {
            AccountResource ar =  new AccountResource(AccountBean.redeemVoucher(accountId, accountId, voucherCode));
            log.debug("redeem of voucher [{}] is successful",voucherCode);
            PrepaidStrip strip = AccountBean.getVoucher(voucherCode, accountId);
            int ucID = strip.getUnitCreditSpecificationId();
            log.debug("voucher uccid is : [{}]",ucID);
                        
            VoucherRedeemResp resp = new VoucherRedeemResp();
            
            resp.setPrepaidStripId(strip.getPrepaidStripId());
            resp.setUnitCreditSpecificationId(strip.getUnitCreditSpecificationId());
            resp.setStatus(strip.getStatus());
            resp.setValueInCents(strip.getValueInCents());
            if(ucID > 0){
                UnitCreditSpecification ucs = NonUserSpecificCachedDataHelper.getUnitCreditSpecification(ucID);
                resp.setUnitCreditName(ucs.getName());
                resp.setUnits(ucs.getUnits());
                resp.setUsableDays(ucs.getUsableDays());
            }
            return resp;
            
            
        } catch (Exception ex) {
            log.error("error in voucherRedeem",ex);
            if(ex.getMessage().contains("Invalid strip PIN") || ex.getMessage().contains("No money")){
                throw processError(SRAUtil.getMesssage("sra.error.invalid.voucher.pin"),"BUSINESS", "SRA-0013",Response.Status.BAD_REQUEST);
            } else {
                throw processError(SRAUtil.getMesssage("sra.error.redeem.voucher.error"),"BUSINESS", "SRA-0011",Response.Status.BAD_REQUEST);
            }
        } finally {
            end();
        }
    }
    
    /**
     * API to share data bundle to multiple accounts. 
     * @param request
     * @return 
     */
    @POST
    @Path("sharedata")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ShareUnitCreditResponse shareAcountData(ShareUnitCreditRequest request){
        log.debug("inside shareAcountData");
        Token callersToken = Token.getRequestsToken();
        int userId = callersToken.getCustomerId();
        log.debug("user in token is "+userId);
        
        if(request.getSourceAccountId() <= 0){
            throw processError(new Exception("invalid source account id"));
        }
        
        if(request.getTargetAccounts() == null || request.getTargetAccounts().isEmpty()){
            throw processError(new Exception("target accounts not provided"));
        }
        
        if (request.getTargetAccounts().size() > BaseUtils.getIntProperty("env.split.unitcredit.max.target.accounts", 20)) {
            throw processError(new Exception("Max target account allowed is exceeded"));
        }
        if(isSystemAccountsFound(request.getTargetAccounts())){
            throw new InsufficientPrivilegesException("system accounts cannot be credited");
        }
        
        //Customer customer = getCustomerByUserName(userName);
        Account sourceAcc = SCAWrapper.getUserSpecificInstance().getAccount(request.getSourceAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        int sourceCustId = sourceAcc.getServiceInstances().get(0).getCustomerId();
        boolean isValidTargetAcc = validateTargetAccForShare(userId, request.getTargetAccounts());
        if(sourceCustId != userId || !isValidTargetAcc){
            throw processError(SRAUtil.getMesssage("sra.error.share.invalid.target.acount"), "BUSINESS", "SRA-0030", Response.Status.BAD_REQUEST);
        }
        
        boolean isSharable =  isSharable(request.getUnitCreditInstanceId());
        if(!isSharable){
            throw processError(new Exception("bundle can't be shared"));
        }
        
        SplitUnitCreditData data = new SplitUnitCreditData();
        data.setUnitCreditInstanceId(request.getUnitCreditInstanceId());
        data.setUnits(request.getUnits());
        boolean splitFailure = false;
        StringBuilder errorCollections = new StringBuilder("[");
        for (Long accId : request.getTargetAccounts()) {
            log.debug("Target account is [{}]", accId);
            if (String.valueOf(accId).length() != 10) {
                        continue;
            }
            data.setTargetAccountId(accId);
            try {
                SCAWrapper.getUserSpecificInstance().splitUnitCredit(data);
            } catch (Exception ex) {
                log.warn("Failed to do split on account: ", ex);
                splitFailure = true;
                errorCollections.append(accId).append(",");
            }
        }
        String partialFailMsg = null;
        if (splitFailure) {
            String errorColl = errorCollections.toString();
            errorColl = errorColl.substring(0, errorColl.lastIndexOf(","));
            errorColl = errorColl + "]";
            partialFailMsg = "Split completed, the following accounts failed :"+errorColl;
        }
        
        AccountQuery aq = new AccountQuery();
        aq.setAccountId(request.getSourceAccountId());
        aq.setVerbosity(StAccountLookupVerbosity.ACCOUNT_UNITCREDITS);
        Account acc = SCAWrapper.getAdminInstance().getAccount(aq);
        ShareUnitCreditResponse resp = new ShareUnitCreditResponse(acc);   
        
        if(partialFailMsg != null){
            resp.setExtraInfo(partialFailMsg);
        }
        return resp;
    }
    
    /**
     * API to share airtime to multiple accounts. 
     * @param request
     * @return 
     */
    @POST
    @Path("shareairtime")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ShareAirtimeResponse shareAcountAirtime(AirtimeShareRequest request){
        log.debug("inside shareAcountAirtime");
        Token callersToken = Token.getRequestsToken();
        int userId = callersToken.getCustomerId();
        log.debug("userId in token is "+userId);
        if(request.getSourceAccountId() <= 0){
            throw processError(new Exception("invalid source account id"));
        }
        
        if(request.getTargetAccountId() <= 0){
            throw processError(new Exception("invalid target account id"));
        }
        
        if(request.getSourceAccountId() == request.getTargetAccountId()){
            throw processError(new Exception("source and target accounts should not be same"));
        }
        
        if(request.getAmountInCents() <= 0.00){
            throw processError(new Exception("invalid amount"));
        }
        
        if (!BaseUtils.getBooleanProperty("env.scp.me2u.enabled", true)) {
            throw new InsufficientPrivilegesException("Me2U transfers is currently disabled");
        }
        
        if(request.getTargetAccountId() < 1100000000 || request.getSourceAccountId() < 1100000000) {
            throw new InsufficientPrivilegesException("invalid accounts for airtime transfer");
        }

        Account targetAcc = SCAWrapper.getAdminInstance().getAccount(request.getTargetAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        Account sourceAcc = SCAWrapper.getUserSpecificInstance().getAccount(request.getSourceAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        for (ServiceInstance si : sourceAcc.getServiceInstances()) {
            if (si.getServiceSpecificationId() == 15 || si.getServiceSpecificationId() >= 1000) {
                throw new InsufficientPrivilegesException("Special accounts cannot do transfers");
            }
        }
        
        int sourceCustId = sourceAcc.getServiceInstances().get(0).getCustomerId();
        int targetCustId = targetAcc.getServiceInstances().get(0).getCustomerId();
        
        if(sourceCustId != userId || targetCustId != userId){
            throw processError(SRAUtil.getMesssage("sra.error.share.invalid.target.acount"), "BUSINESS", "SRA-0030", Response.Status.BAD_REQUEST);
        }
        
        BalanceTransferData data = new BalanceTransferData();
        data.setSourceAccountId(request.getSourceAccountId());
        data.setTargetAccountId(request.getTargetAccountId());
        data.setAmountInCents(request.getAmountInCents());
        data.setSCAContext(null);
        try {
                SCAWrapper.getUserSpecificInstance().transferBalance(data);
        } catch (SCAErr e) {
            if (e.getErrorCode().equalsIgnoreCase("BM-0001") || e.getErrorCode().equalsIgnoreCase("BM-0007")) {
                throw processError(SRAUtil.getMesssage("sra.error.insufficient.airtime"), "BUSINESS", "SRA-0019", Response.Status.BAD_REQUEST);
            } else if (e.getErrorCode().equalsIgnoreCase("BM-0002")) {
                throw processError("invalid account to do transfer", "BUSINESS", "SRA-0020", Response.Status.BAD_REQUEST);
            } else if (e.getErrorCode().equalsIgnoreCase("BM-0003")) {
                throw processError("invalid request", "BUSINESS", "SRA-0020", Response.Status.BAD_REQUEST);
            }  else {
                throw processError("balance transfer request failed", "BUSINESS", "SRA-0021", Response.Status.BAD_REQUEST);
            }
        }
        
        AccountQuery aq = new AccountQuery();
        aq.setAccountId(request.getSourceAccountId());
        aq.setVerbosity(StAccountLookupVerbosity.ACCOUNT_UNITCREDITS);
        Account acc = SCAWrapper.getAdminInstance().getAccount(aq);
        return new ShareAirtimeResponse(acc);
    }

    private boolean isSystemAccountsFound(List<Long> targetAccounts) {
        if(targetAccounts == null || targetAccounts.isEmpty()){
            return false;
        }
        for(Long acc: targetAccounts){
            if(acc < 1100000000){
                return true;
            }
        }
        return false;
    }

    private boolean isSharable(int unitCreditInstanceId) {
        AccountQuery aq = new AccountQuery();
        aq.setUnitCreditInstanceId(unitCreditInstanceId);
        aq.setVerbosity(StAccountLookupVerbosity.ACCOUNT_UNITCREDITS);
        Account acc = SCAWrapper.getAdminInstance().getAccount(aq);
        int ucsId = -1;
        for (UnitCreditInstance uci : acc.getUnitCreditInstances()) {
            if (uci.getUnitCreditInstanceId() == unitCreditInstanceId) {
                ucsId = uci.getUnitCreditSpecificationId();
                break;
            }
        }
        if (ucsId <= 0){
            return false;
        }
        
        UnitCreditSpecification ucs = SCAWrapper.getAdminInstance().getUnitCreditSpecification(ucsId);
        if(ucs == null){
            return false;
        }
        
        return (ucs.getConfiguration().contains("AllowSplitting=true"));
    }

    private boolean validateTargetAccForShare(int userId, List<Long> targetAccounts) {
        
        for(Long acc : targetAccounts){
            Account account = SCAWrapper.getUserSpecificInstance().getAccount(acc, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
            int customerId = account.getServiceInstances().get(0).getCustomerId();
            if(customerId != userId){
                return false;
            }
        }
        return true;
    }
    
    private String getXmlHistory(AccountHistory ahb, String type) {
        StringBuilder xml = new StringBuilder();
        xml.append("<History>");
        log.debug("type of record is "+type);
        for(TransactionRecord trb : ahb.getTransactionRecords()){
            
            log.debug("TransactionRecord type is :"+trb.getTransactionType());
            String trType = getTrType(type);
            log.debug("trType is : "+trType);
            if(trType != null && !type.equals("airtime.share") && !trb.getTransactionType().contains(trType)){
                continue;
            }
            if("txtype.tfr".equals(trType) && trb.getTransactionType().endsWith("m2u")){
                continue;
            }
            xml.append("<Transaction>");
            xml.append("<Source>").append(Utils.getPhoneNumberFromSIPURI(trb.getSource())).append("</Source>");
            xml.append("<Destination>").append(Utils.getPhoneNumberFromSIPURI(trb.getDestination())).append("</Destination>");
            xml.append("<Value>").append(trb.getAmountInCents()/100.00).append("</Value>");
            xml.append("<TotalUnits>").append(getTotalUnits(trb)).append("</TotalUnits>");
            //xml.append("<BundleUnits>").append(trb.getTotalUnits()).append("</BundleUnits>");
            xml.append("<Balance>").append(trb.getAccountBalanceRemainingInCents() / 100.00).append("</Balance>");
            xml.append("<Type>").append(getTransactionType(trb.getTransactionType())).append("</Type>");
            xml.append("<Description>").append(trb.getDescription().replaceAll("&", "and")).append("</Description>");
            xml.append("<Date>").append(getFormatedDate(trb)).append("</Date>");
            xml.append("</Transaction>");
        }
        xml.append("</History>");
        log.debug("xml history is [{}]",xml.toString());
        return xml.toString();
    }

    private byte[] generateHistoryPdf(String xmlHistory) throws Exception {
        return PDFUtils.generateLocalisedPDF("account.history.download.pdf.xslt", xmlHistory, LocalisationHelper.getDefaultLocale(), getClass().getClassLoader());
        
    }

    private String getTotalUnits(TransactionRecord trb) {
        if("Data".startsWith(trb.getDescription())){
            return Utils.displayVolumeAsString(trb.getUnitCreditUnits(), "Byte");
        } else if("Voice".startsWith(trb.getDescription())){
            return Utils.displayVolumeAsString(trb.getTotalUnits(), "Sec");
        } else if("SMS".startsWith(trb.getDescription())){
            return String.valueOf(trb.getTotalUnits());
        } else if("Split".startsWith(trb.getDescription())){
            return Utils.displayVolumeAsString(trb.getUnitCreditUnits(), "Byte");
        } else {
            if(trb.getTotalUnits() > 0){
                return String.valueOf(trb.getTotalUnits());
            }
            return String.valueOf(trb.getTotalUnits());
        }
    }

    private String getTransactionType(String transactionType) {
        return SRAUtil.getMesssage("event."+transactionType);
    }

    private String getFormatedDate(TransactionRecord trb) {
        StringBuilder sb = new StringBuilder();
        String startTime = Utils.formatDateLong(trb.getStartDate().toGregorianCalendar().getTime());
        String endTime = Utils.formatDateLong(trb.getEndDate().toGregorianCalendar().getTime());
        sb.append(startTime).append(" - ").append(endTime);
        return sb.toString();
    }

    private double getAmountInCents(TransactionRecord tr) {
        log.debug("called getAmountInCents");
        if(! tr.getTransactionType().startsWith("txtype.uc.purchase")){
            return tr.getAmountInCents();
        }
        // get price from sale for UC purchases
        String saleRowId = Utils.getValueFromCRDelimitedAVPString(tr.getInfo(), "SaleLineId").trim();
        log.debug("saleRowId is {}",saleRowId);
        if(saleRowId == null || saleRowId.isEmpty()){
            return tr.getAmountInCents();
        }
        SalesQuery sq = new SalesQuery();
        sq.setSaleLineId(Integer.valueOf(saleRowId));
        sq.setVerbosity(StSaleLookupVerbosity.SALE_LINES);
        SalesList slist = SCAWrapper.getAdminInstance().getSales(sq);
        for(Sale sale : slist.getSales()){
            for(SaleLine sl : sale.getSaleLines()){
                log.debug("sl.getLineId() == "+sl.getLineId());
                if(sl.getLineId().equals(Integer.valueOf(saleRowId))){
                    return sl.getLineTotalCentsIncl();
                }
            }
        }
        return tr.getAmountInCents();
    }
    
    
    /**
     * Update customer account status
     * @param account
     * @return 
     */
    @PUT 
    @Path("/modify/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object modifyAccountStatus(Account account) {
        start(request);
        
        try {
            if (!BaseUtils.getPropertyAsSet("global.account.allowed.statuses.scp").contains(String.valueOf(account.getStatus()))) {
                log.warn("Bad account status [" + String.valueOf(account.getStatus()) + "]" );
                throw processError(new Exception("Bad account status [" + String.valueOf(account.getStatus()) + "]" ));
            }
            
            Account acc = SCAWrapper.getUserSpecificInstance().getAccount(account.getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS);
            
            if (!BaseUtils.getProperty("env.immutable.account.statuses.scp", "").isEmpty()) {
                if (BaseUtils.getPropertyAsSet("env.immutable.account.statuses.scp").contains(String.valueOf(acc.getStatus()))) {
                    log.warn("Account status override not allowed");
                    throw processError(new Exception("Account status override not allowed - " + String.valueOf(acc.getStatus())));
                }
            }
            

            for (Reservation res : acc.getReservations()) {
                if (res.getDescription().equals("SMS") && acc.getStatus() == 14) {
                    throw processError(new Exception("Account status cannot be changed while it has reservations"));                    
                } else if (res.getDescription().equals("Voice") && acc.getStatus() == 14) {
                    throw processError(new Exception("Account status cannot be changed while it has reservations"));                    
                }/* else if (res.getDescription().equals("Data") && getAccount().getStatus() == 14) {//Need to be tested; one might only be able to change this if not browsing at all
                    localiseErrorAndAddToGlobalErrors("scp.account.status.allusage.selected.withactive.reservation");
                    return retrieveAccount();
                }*/
            }
            SCAWrapper.getUserSpecificInstance().modifyAccount(account);
            
            return account;            
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }
    
    
     /**
     * Update customer account status
     * @param portal [scp/sep]
     * 
     * @return 
     */
    /*@GET
    @Path("/get/{portal}/allowedstatuses")
    @Produces(MediaType.APPLICATION_JSON)
    public Object modifyAccountStatus(@PathParam("portal") String portal) {
        start(request);
        
        HashMap<String,String> statusesMap = new HashMap<>();
        
        try {
            List<String> allowedStatuses = BaseUtils.getPropertyAsList("global.account.allowed.statuses." + portal);
            
            
            
            for(String accStatus: allowedStatuses) {
                statusesMap.putIfAbsent(accStatus, );
                    
            }
            
            
            return new JSONObject(statusesMap);            
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }
*/
}
