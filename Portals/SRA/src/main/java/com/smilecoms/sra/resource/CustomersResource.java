/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.AccountQuery;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.CustomerRole;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.TTIssueQuery;
import com.smilecoms.commons.sca.beans.AccountBean;
import com.smilecoms.commons.sca.beans.CustomerBean;
import com.smilecoms.commons.sca.beans.ProductBean;
import com.smilecoms.commons.sca.beans.SaleBean;
import com.smilecoms.commons.sca.beans.ServiceBean;
import com.smilecoms.commons.sca.beans.UnitCreditBean;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.sra.model.AccountsResponse;
import com.smilecoms.sra.model.AppAccount;
import com.smilecoms.sra.model.ChangePasswordRequest;
import com.smilecoms.sra.model.KYCDetail;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.sra.helpers.Done;
import com.smilecoms.sra.helpers.RequestParser;
import com.smilecoms.sra.helpers.Token;

import com.currencyfair.onesignal.OneSignal;
import com.currencyfair.onesignal.model.notification.CreateNotificationResponse;
import com.currencyfair.onesignal.model.notification.NotificationRequest;
import com.smilecoms.commons.sca.Photograph;
import com.smilecoms.selfcare.AppNotificationReq;
import com.smilecoms.selfcare.NotificationMessage;
import com.smilecoms.selfcare.SelfcareService;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cxf.jaxrs.ext.multipart.*; 

/**
 *
 * @author lesiba
 */
@Path("customers")
public class CustomersResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(CustomersResource.class);
    @Context
    private javax.servlet.http.HttpServletRequest request;

    @Path("{identifier}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CustomerResource getCustomer(@PathParam("identifier") String identifier, @QueryParam("by") String by, @QueryParam("verbosity") String verbosityString) {
        start(request);
        try {
            StCustomerLookupVerbosity verbosity;
            if (verbosityString != null) {
                verbosity = StCustomerLookupVerbosity.fromValue(verbosityString);
            } else {
                verbosity = StCustomerLookupVerbosity.CUSTOMER;
            }
            if (by == null || by.isEmpty() || by.equalsIgnoreCase("id")) {
                return CustomerResource.getCustomerResourceById(Integer.valueOf(identifier), verbosity);
            } else if (by.equalsIgnoreCase("username")) {
                return CustomerResource.getCustomerResourceByUserName(identifier, verbosity);
            } else if (by.equalsIgnoreCase("email")) {
                return CustomerResource.getCustomerResourceByEmail(identifier, verbosity);
            } else if (by.equalsIgnoreCase("smilevoice")) {
                Map<String,String> customerDetail = getCustomerIdBySmileVoiceNumber(identifier);
                int customerId = Integer.valueOf(customerDetail.get("customer_id"));
                return CustomerResource.getCustomerResourceById(customerId, verbosity);
            } else {
                throw new Exception("Invalid customer lookup type -- " + by);
            }
        } catch (Exception ex) {
            throw processError("Customer not found","BUSINESS","SRA-00013", Response.Status.NOT_FOUND);
        } finally {
            end();
        }
    }

    @POST
    @Path("{identifier}/passwordResetLink")
    @Produces(MediaType.APPLICATION_JSON)
    public Done sendPasswordResetLink(@PathParam("identifier") String identifier, MultivaluedMap<String, String> formParams, @Context UriInfo uriInfo) {
        start(request);
        try {
            CustomerBean.sendPasswordResetLink(identifier);
            return new Done();
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }

    @GET
    @Path("/{identifier}/organisations/")
    @Produces(MediaType.APPLICATION_JSON)
    public OrganisationResource[] getCustomersOrganisations(@PathParam("identifier") String identifier, @QueryParam("by") String by) {
        start(request);
        try {
            List<OrganisationResource> orList = new ArrayList<>();
            CustomerResource customerR;
            if (by == null || by.isEmpty() || by.equalsIgnoreCase("id")) {
                customerR = CustomerResource.getCustomerResourceById(Integer.valueOf(identifier), StCustomerLookupVerbosity.CUSTOMER);
            } else if (by.equalsIgnoreCase("username")) {
                customerR = CustomerResource.getCustomerResourceByUserName(identifier, StCustomerLookupVerbosity.CUSTOMER);
            } else if (by.equalsIgnoreCase("email")) {
                customerR = CustomerResource.getCustomerResourceByEmail(identifier, StCustomerLookupVerbosity.CUSTOMER);
            } else {
                throw new Exception("Invalid customer lookup type -- " + by);
            }
            CustomerBean customer = customerR.getCustomer();
            for (CustomerRole cr : customer.getCustomerRoles()) {
                orList.add(new OrganisationResource(cr.getOrganisationId()));
            }
            return orList.toArray(new OrganisationResource[orList.size()]);
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }

    @GET
    @Path("/{identifier}/tickets/")
    @Produces(MediaType.APPLICATION_JSON)
    public TTIssueResource getCustomersTickets(@PathParam("identifier") String identifier, @QueryParam("by") String by, @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo, @QueryParam("resultLimit") int resultLimit) {
        start(request);
        try {
            CustomerResource customerR;
            if (by == null || by.isEmpty() || by.equalsIgnoreCase("id")) {
                customerR = CustomerResource.getCustomerResourceById(Integer.valueOf(identifier), StCustomerLookupVerbosity.CUSTOMER);
            } else if (by.equalsIgnoreCase("username")) {
                customerR = CustomerResource.getCustomerResourceByUserName(identifier, StCustomerLookupVerbosity.CUSTOMER);
            } else if (by.equalsIgnoreCase("email")) {
                customerR = CustomerResource.getCustomerResourceByEmail(identifier, StCustomerLookupVerbosity.CUSTOMER);
            } else {
                throw new Exception("Invalid customer lookup type -- " + by);
            }
            CustomerBean customer = customerR.getCustomer();
            TTIssueQuery query = new TTIssueQuery();
            query.setIssueID("");
            query.setIncidentChannel("");
            query.setCreatedDateFrom(Utils.getDateAsXMLGregorianCalendar(Utils.getStringAsDate(dateFrom, "yyyyMMdd", Utils.getPastDate(Calendar.MONTH, BaseUtils.getIntProperty("env.jira.months.number.filter.by")))));
            query.setCreatedDateTo(Utils.getDateAsXMLGregorianCalendar(Utils.getEndOfDay(Utils.getStringAsDate(dateTo, "yyyyMMdd", new Date()))));
            query.setCustomerId(Integer.toString(customer.getCustomerId()));
            if (resultLimit <= 0) {
                resultLimit = 10;
            }
            query.setResultLimit(resultLimit);
            return new TTIssueResource(query);
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }

    @GET
    @Path("/{customerId}/sales/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SaleResource> getCustomersSales(@PathParam("customerId") int customerId, @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
        start(request);
        try {
            List<SaleResource> sales = new ArrayList<>();
            for (SaleBean sb : SaleBean.getSalesByCustomerProfileId(customerId, Utils.getStringAsDate(dateFrom, "yyyyMMdd", Utils.getPastDate(Calendar.MONTH, 1)), Utils.getEndOfDay(Utils.getStringAsDate(dateTo, "yyyyMMdd", new Date())))) {
                sales.add(new SaleResource(sb));
            }
            return sales;
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }

    @PUT
    @Path("/optout/")
    public Response optOut(@QueryParam("email") String email, @QueryParam("value") int value) {
        start(request);
        try {
            if(email == null || email.trim().isEmpty()){
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Token callersToken = Token.getRequestsToken();
            int tokenCustomerId = callersToken.getCustomerId();
            CustomerQuery custQuery = new CustomerQuery();
            custQuery.setEmailAddress(email);
            custQuery.setResultLimit(1);
            custQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            Customer c = SCAWrapper.getAdminInstance().getCustomer(custQuery);
            if(c.getCustomerId() != tokenCustomerId){
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            int currentOptInLevel = c.getOptInLevel();
            log.debug("Current Opt in level is [{}]", currentOptInLevel);
            int newOptInLevel = 0;
            if(value > 7) {
                newOptInLevel = 7;
            } else if (value < 0){
                newOptInLevel = 0;
            } else {
                newOptInLevel = value; 
            }

            c.setOptInLevel(newOptInLevel);
            log.debug("Setting customers opt in level to [{}]", newOptInLevel);
            SCAWrapper.getAdminInstance().modifyCustomer(c);

            return Response.status(Response.Status.OK).entity(new Done())
                    .build();
            /*return Response.status(Response.Status.TEMPORARY_REDIRECT)
                    .header(HttpHeaders.LOCATION, BaseUtils.getProperty("env.sra.campaign.redirect.ok.optout", "https://smilecoms.com?ok=true"))
                    .build();*/
        } catch (Exception ex) {
            processError(ex);
        } finally {
            end();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @PUT
    @Path("/{customerId}/password")
    @Produces(MediaType.APPLICATION_JSON)
    public Done changeCustomersPassword(@PathParam("customerId") int customerId, MultivaluedMap<String, String> formParams, @Context UriInfo uriInfo) {
        start(request);
        try {
            checkPermissions(Permissions.EDIT_CUSTOMER_PASSWORD);
            RequestParser parser = new RequestParser(formParams, uriInfo);
            CustomerBean.changeCustomerPassword(customerId, parser.getParamAsString("newPassword"));
            return new Done();
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }
    
    @PUT
    @Path("/{customerId}/nin/{nin}")
    @Produces(MediaType.APPLICATION_JSON)
    public CustomerResource updateNIN(@PathParam("customerId") int customerId, @PathParam("nin") String nin, 
            @QueryParam("mobileno") String mobileno, @QueryParam("email") String email){
        start(request);
        log.debug("called updateNIN");
        try {
            Token callersToken = Token.getRequestsToken();
            if(callersToken.getCustomerId() != customerId){
                throw new Exception("Invalid credential to change password");
            }
            Customer customer = SCAWrapper.getUserSpecificInstance().getCustomer(customerId, StCustomerLookupVerbosity.CUSTOMER);
            log.debug("found a customer in db");
            customer.setNationalIdentityNumber(nin);
            SCAWrapper.getAdminInstance().modifyCustomer(customer);
            log.debug("updated customer NIN");
            CustomerResource cr = new CustomerResource(new CustomerBean(customer));
            return cr;
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CustomerResource addCustomer(CustomerResource cust) {
        start(request);
        try {
            // Temp till Menno fixes
            cust.getCustomer().setIdentityNumberType("passport");

            checkPermissions(Permissions.ADD_CUSTOMER);
            return new CustomerResource(CustomerBean.addCustomer(cust.getCustomer()));
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }

    }

    @GET
    @Path("/{customerId}/accounts/")
    @Produces(MediaType.APPLICATION_JSON)
    public AccountResource[] getCustomersAccounts(@PathParam("customerId") int customerId) {
        start(request);
        try {
            CustomerResource customerR = CustomerResource.getCustomerResourceById(customerId, StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
            CustomerBean customer = customerR.getCustomer();
            List<ProductBean> products = customer.getProducts();
            Set<Long> uniqueAccounts = new HashSet<>();

            for (ProductBean pb : products) {
                for (ServiceBean sb : pb.getServices()) {
                    uniqueAccounts.add(sb.getAccountId());
                }
            }

            List<AccountResource> accList = new ArrayList<>();
            for (long accountId : uniqueAccounts) {
                accList.add(new AccountResource(accountId));
            }
            return accList.toArray(new AccountResource[accList.size()]);

        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }
    
    @GET
    @Path("/{customerId}/smilevoiceaccounts")
    @Produces(MediaType.APPLICATION_JSON)
    public AccountsResponse getCustomerSmileVoiceAccounts(@PathParam("customerId") int customerId) {
        start(request);
        
        AccountsResponse resp = new AccountsResponse();
        try {
            CustomerResource customerR = CustomerResource.getCustomerResourceById(customerId, StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
            CustomerBean customer = customerR.getCustomer();
            List<ProductBean> products = customer.getProducts();
            Set<Long> uniqueAccounts = new HashSet<>();

            Map<Long,String> acFriendlyNameMap = new HashMap<>();
            for (ProductBean pb : products) {
                for (ServiceBean sb : pb.getServices()) {
                    uniqueAccounts.add(sb.getAccountId());
                    if(pb.getFriendlyName() != null || ! pb.getFriendlyName().isEmpty()){
                       acFriendlyNameMap.put(sb.getAccountId(), pb.getFriendlyName()); 
                    }
                }
            }
            log.debug("acFriendlyNameMap = "+acFriendlyNameMap);
            for (long accountId : uniqueAccounts) {
                String smileVoice = getSmileVoiceNumber(accountId);
                if(null == smileVoice || smileVoice.equals("NA")){
                    continue;
                }
                AccountResource accRes = new AccountResource(accountId);                
                AppAccount account = new AppAccount();
                account.setAccount(accRes.getAccount());
                account.setSmileVoiceNo(smileVoice);
                account.setUnitCreditConfig(getUnitCreditConfig(accRes.getAccount()));
                account.setFriendlyName(acFriendlyNameMap.get(accRes.getAccount().getAccountId()));
                log.debug("setting friendly name [{}] to account [{}]",acFriendlyNameMap.get(accRes.getAccount().getAccountId()),accRes.getAccount().getAccountId() );
                resp.getAccounts().add(account);
            }
            resp.setNotifications(getCustomersUnreadNotifications(customerId));
            return resp;
        } catch (Exception ex) {
            log.error("exception in getCustomerSmileVoiceAccounts",ex);
            if(ex.getMessage().contains("IM-0003")){
                throw processError("Customer not found","BUSINESS","SRA-00013", Response.Status.NOT_FOUND);
            } else {
                throw processError("Server error","BUSINESS","SRA-00019", Response.Status.INTERNAL_SERVER_ERROR);
            }
        } finally {
            end();
        }
        
    }
    
    @GET
    @Path("/smilevoiceaccounts")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,String> getCustomerSmileVoiceAccounts(@QueryParam("smileVoiceNo") String smileVoiceNumber) {
        start(request);
        try {
            if(smileVoiceNumber == null || smileVoiceNumber.isEmpty()){
                throw processError("invalid smile voice number","BUSINESS","SRA-00213", Response.Status.NOT_FOUND);
            }
            Map<String,String> customerAccountMap = getCustomerIdBySmileVoiceNumber(smileVoiceNumber);
            if(customerAccountMap == null || customerAccountMap.get("customer_id") == null){
                throw processError("invalid smile voice number","BUSINESS","SRA-00214", Response.Status.NOT_FOUND);
            }
            if(customerAccountMap.get("account_id") == null){
                throw processError(new Exception("account number not found"));
            }
            return customerAccountMap;
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
        
    }
    
    /**
     * get customer id against smile voice number
     * @param smileNo smile voice number
     * @return customer id against smile voice number.returns -1 when customer not found.
     */
    private Map<String,String> getCustomerIdBySmileVoiceNumber(String smileNo) {
      
        int customerId = -1;
        Map<String, String> resp = new HashMap<>();
        ServiceInstanceQuery siq = new ServiceInstanceQuery();
        siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
        siq.setIdentifierType("END_USER_SIP_URI");
        siq.setIdentifier(Utils.getPublicIdentityForPhoneNumber(smileNo));
        try {
            ServiceInstanceList sil = SCAWrapper.getAdminInstance().getServiceInstances(siq);
            if (sil == null || sil.getServiceInstances() == null || sil.getServiceInstances().isEmpty()) {
                return null;
            }
            customerId = sil.getServiceInstances().get(0).getCustomerId();
            resp.put("customer_id", String.valueOf(sil.getServiceInstances().get(0).getCustomerId()));
            resp.put("account_id", String.valueOf(sil.getServiceInstances().get(0).getAccountId()));
        } catch (Exception ex) {
            log.error("customer id not found for smile voice number [{}]", smileNo);
        }
        log.debug("customer id for smiel number [{}] is [{}]",smileNo,customerId);
        return resp;       
    }
    
    private String getSmileVoiceNumber(long accountNo){
        log.debug("checking getSmileVoiceNumber for account "+accountNo);
        AccountQuery aq = new AccountQuery();
        String smileNumber = null;
        aq.setAccountId(accountNo);
        aq.setVerbosity(StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        Account acc = SCAWrapper.getAdminInstance().getAccount(aq);
        log.debug("account service instances count is "+acc.getServiceInstances().size());
        for (ServiceInstance si : acc.getServiceInstances()) {
            if(100 != si.getServiceSpecificationId()){
                continue;
            }
            smileNumber =  UserSpecificCachedDataHelper.getServiceInstancePhoneNumber(si.getServiceInstanceId());
        }
        log.debug("smile number for account "+accountNo+" is "+smileNumber);
        return smileNumber;
    }
    
    
    @PUT
    @Path("/{customerId}/changepassword")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Done changeMyPassword(@PathParam("customerId") int customerId, ChangePasswordRequest req) {
        start(request);
        try {
            Token callersToken = Token.getRequestsToken();
            if(callersToken.getCustomerId() != customerId){
                throw new Exception("Invalid credential to change password");
            }
            if(! req.getNewPassword().equals(req.getConfirmPassword())){
                throw new Exception("Confirm password didn't match with new password");
            }
            CustomerBean.changeCustomerPassword(customerId, req.getNewPassword());
            return new Done();
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }
    
    /**
     * API to add or update selfcare app id
     * @param req
     * @return 
     */
    @POST
    @Path("/appnotification")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Done addOrUpdateNotificationApp(AppNotificationReq req){        
        SelfcareService service = new SelfcareService();       
        boolean result = service.AddOrUpdateAppNotification(req);
        if(result != true){
            throw processError("Failed to update notification","BUSINESS","SRA-0378",Response.Status.BAD_REQUEST);
        }
        return new Done();
    }
    
    /**
     * API to send push notification to a customer selfcare app.
     * @param customerId
     * @param notificationRequest
     * @return 
     */
    @POST
    @Path("/{customerId}/pushnotification")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CreateNotificationResponse pushNonification(@PathParam("customerId") int customerId, NotificationRequest notificationRequest){
        String appAuthKey = BaseUtils.getSubProperty("env.app.push.notification", "APIAuthKey");
        return OneSignal.createNotification(appAuthKey, notificationRequest);
    }
    
    /**
     * API for getting all the in App notifications for a customer.
     * @param customerId
     * @return 
     */
    @GET
    @Path("/{customerId}/notification")
    @Produces(MediaType.APPLICATION_JSON)
    public List<NotificationMessage> getInAppNonification(@PathParam("customerId") int customerId){
        SelfcareService service = new SelfcareService();
        return service.getNotificationMessage(customerId);
    }
    
    /**
     * API for getting all the in App notifications for a customer.
     * @param customerId
     * @param message
     * @return 
     */
    @PUT
    @Path("/{customerId}/notification")
    @Produces(MediaType.APPLICATION_JSON)
    public Done updateAppNonification(@PathParam("customerId") int customerId, NotificationMessage message){
        SelfcareService service = new SelfcareService();
        message.setCustomerId(customerId);
        boolean result = service.updateNotificationMessage(message);
        if(!result){
            throw processError("Failed to update notification messaage","BUSINESS","SRA-0379",Response.Status.BAD_REQUEST);
        }
        return new Done();
    }

    private List<Map<String, String>> getUnitCreditConfig(AccountBean account) {
        List<Map<String, String>> uccList = new ArrayList<>();
        for(UnitCreditBean ucb : account.getUnitCredits()){
            Map<String, String> config = new HashMap<>();
            config.put("ID", String.valueOf(ucb.getUnitCreditSpecificationId()));
            config.put("WhiteListRatingGroupRegex", ucb.getConfigProperty("WhiteListRatingGroupRegex"));
            config.put("AllowSplitting", ucb.getConfigProperty("AllowSplitting"));
            uccList.add(config);
        }
        return uccList;
    }

    private int getCustomersUnreadNotifications(int customerId) {
        SelfcareService service = new SelfcareService();
        List<NotificationMessage> notifications =  service.getNotificationMessage(customerId);
        List<NotificationMessage> unreadNotifications = notifications.stream().filter(message -> message.getStatus() == NotificationMessage.Status.UR).collect(Collectors.toList());
        return (unreadNotifications == null || unreadNotifications.isEmpty()) ? 0 : unreadNotifications.size();
    }
    
    /**
     * 
     * @param customerId
     * @param attachment
     * @return
     * @throws Exception 
     */
    @POST
    @Path("/{customerId}/uploadprofilephoto")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Done handleUpload(@PathParam("customerId") int customerId,
            @Multipart("file") Attachment attachment) throws Exception {

        log.info("called handleUpload");
        String filename = attachment.getContentDisposition().getParameter("filename");
        InputStream in = attachment.getObject(InputStream.class);
        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(customerId);
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);

        Customer tmpCust = SCAWrapper.getUserSpecificInstance().getCustomer(q);

        if (tmpCust == null) {
            throw processError("Invalid customer ID", "BUSINESS", "SRA-0479", Response.Status.BAD_REQUEST);
        }
        tmpCust.getCustomerPhotographs().removeIf(photo -> photo.getPhotoType().equalsIgnoreCase("profilephoto"));
        
        String fileExtension = filename.split("\\.")[1];
        log.debug("fileExtension is [{}]", fileExtension);

        byte[] bytes = IOUtils.toByteArray(in);
        File tmpFile = Utils.createTempFile(Utils.getUUID() + "." + fileExtension, bytes);
        Photograph photo = new Photograph();
        photo.setPhotoGuid(tmpFile.getName());
        photo.setPhotoType("profilephoto");
        photo.setData(Utils.encodeBase64(Utils.getDataFromTempFile(tmpFile.getName())));
        log.debug("PhotoGuid is "+photo.getPhotoGuid());
        tmpCust.getCustomerPhotographs().add(photo);
        try {
            SCAWrapper.getAdminInstance().modifyCustomer(tmpCust);
        } catch(Exception ex){
            log.error("error in modifyCustomer:",ex);
        }
        log.debug("finished handleUpload");
        return new Done();  
    }
    

    /**
     * 
     * @param customerId
     * @return 
     */
    @GET
    @Path("/{customerId}/downloadprofilephoto")
    @Produces("image/jpg")
    public Response handleDownload(@PathParam("customerId") int customerId) {

        Photograph photo = getCustomerProfilePhoto(customerId);
        if(photo == null){
            throw processError("profile photo not found","BUSINESS","SRA-0383",Response.Status.NOT_FOUND);
        }
        try {
        String fileName = photo.getPhotoGuid();
        log.debug("handleDownload fileName is [{}]",fileName);
        byte[] fileData = Utils.decodeBase64(photo.getData());
        File tmpFile = Utils.createTempFile(fileName, fileData);
        ResponseBuilder response = Response.ok((Object) tmpFile);
        response.header("Content-Disposition", "attachment;filename=" + fileName);
        return response.build();
        } catch(Exception ex){
            log.error("handleDownload error :",ex);
            ResponseBuilder response = Response.serverError();
            return response.build();
        }
        
    }
    
    /**
     * 
     * @param customerId
     * @return 
     */
    @GET
    @Path("/{customerId}/kycdetail")
    @Produces(MediaType.APPLICATION_JSON)
    public KYCDetail getKycDetail(@PathParam("customerId") int customerId) {
        start(request);
        
        Customer customer = UserSpecificCachedDataHelper.getCustomer(customerId, StCustomerLookupVerbosity.CUSTOMER_PHOTO_ADDRESS_MANDATEKYCFIELD);
        if(customer.getMandatoryKYCFields() == null){
            throw processError("KYC details not found","BUSINESS","SRA-0451",Response.Status.NOT_FOUND);
        }
        customer.getMandatoryKYCFields().getCustomerId();
        KYCDetail kycDetail = new KYCDetail();
        kycDetail.setCustomerId(customer.getMandatoryKYCFields().getCustomerId());
        kycDetail.setDobVerified(customer.getMandatoryKYCFields().getDobVerified());
        kycDetail.setEmailVerified(customer.getMandatoryKYCFields().getEmailVerified());
        kycDetail.setFacialPitureVerified(customer.getMandatoryKYCFields().getFacialPitureVerified());
        kycDetail.setFingerPrintVerified(customer.getMandatoryKYCFields().getFingerPrintVerified());
        kycDetail.setGenderVerified(customer.getMandatoryKYCFields().getGenderVerified());
        kycDetail.setMobileVerified(customer.getMandatoryKYCFields().getMobileVerified());
        kycDetail.setNameVerified(customer.getMandatoryKYCFields().getNameVerified());
        kycDetail.setNationalityVerified(customer.getMandatoryKYCFields().getNationalityVerified());
        kycDetail.setPhysicalAddressVerified(customer.getMandatoryKYCFields().getPhysicalAddressVerified());
        kycDetail.setTitleVerified(customer.getMandatoryKYCFields().getTitleVerified());
        kycDetail.setValidIdCardVerified(customer.getMandatoryKYCFields().getValidIdCardVerified());
        return kycDetail;
    }
    
    private Photograph getCustomerProfilePhoto(int customerId){
        Customer customer = UserSpecificCachedDataHelper.getCustomer(customerId, StCustomerLookupVerbosity.CUSTOMER_PHOTO);
        for(Photograph photo : customer.getCustomerPhotographs()){
            if("profilephoto".equals(photo.getPhotoType())){
                return photo;
            }
        }
        return null;
    }
}
