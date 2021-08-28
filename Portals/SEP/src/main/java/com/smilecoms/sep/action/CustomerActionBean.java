/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * CustomerActionBean
 *
 */
package com.smilecoms.sep.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.*;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.sca.direct.im.NiraPasswordChange;
import com.smilecoms.commons.sca.direct.im.NiraPasswordChangeReply;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.IMAPUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.sep.helpers.OptionTransfer;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.sep.helpers.RestServices;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpSession;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.DontValidate;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.direct.im.DocumentUniquenessQuery;
import com.smilecoms.commons.sca.direct.im.NidaVerifyQuery;
import com.smilecoms.commons.sca.direct.im.NidaVerifyReply;
import com.smilecoms.commons.sca.direct.im.VerifyForeignerQuery;
import com.smilecoms.commons.sca.direct.im.VerifyForeignerReply;
import com.smilecoms.commons.sca.direct.im.VerifyNinQuery;
import com.smilecoms.commons.sca.direct.im.VerifyNinReply;
import com.smilecoms.commons.sca.direct.im.VerifyNinResponseList;
import com.smilecoms.commons.util.HashUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.selfcare.SelfcareService;
import com.smilecoms.sep.helpers.NinAccountData;
import com.smilecoms.sep.helpers.ug.UCCHelper;
import com.smilecoms.sep.helpers.ug.ValidateRefugeeRequest;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.GregorianCalendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.StringTokenizer;
import javax.sql.rowset.serial.SerialBlob;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author PCB
 */
public class CustomerActionBean extends SmileActionBean {

    private static final String EMAIL_PROGRESS_SESSION_KEY = "email.percent.progress";
    private static final String SMS_PROGRESS_SESSION_KEY = "sms.percent.progress";
    private static final String MNP_REQUEST_PROCESSING_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String MNP_REQUEST_PROCESSING_STATUS_DONE = "DONE";
    
    
    @DontValidate
    public Resolution retrieveCustomer() {

        checkPermissions(Permissions.VIEW_CUSTOMER);
        if (getCustomerQuery() == null && getCustomer() != null) {
            setCustomerQuery(new CustomerQuery());
            getCustomerQuery().setCustomerId(getCustomer().getCustomerId());
            getCustomerQuery().setResultLimit(1);

        }

        if (getCustomerQuery() == null) {
            return showSearchCustomer();
        }

        getCustomerQuery().setProductInstanceResultLimit(BaseUtils.getIntProperty("env.sep.pagesize", 50));

        if (getCustomerQuery().getProductInstanceOffset() == null) {
            getCustomerQuery().setProductInstanceOffset(0);
        }

        try {
            if (getCustomerQuery().getCustomerId() != null && getCustomerQuery().getCustomerId() > 0) {
                setCustomer(UserSpecificCachedDataHelper.getCustomer(getCustomerQuery().getCustomerId(),
                        StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP,
                        getCustomerQuery().getProductInstanceOffset(),
                        getCustomerQuery().getProductInstanceResultLimit()));
            } else {
                getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP);
                setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
            }
        } catch (SCABusinessError sbe) {

            if (getCustomerQuery().getAlternativeContact() != null && !getCustomerQuery().getAlternativeContact().isEmpty()) {
                // Search by Smile Phone number
                ServiceInstanceQuery siq = new ServiceInstanceQuery();
                siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                siq.setIdentifierType("END_USER_SIP_URI");
                siq.setIdentifier(Utils.getPublicIdentityForPhoneNumber(getCustomerQuery().getAlternativeContact()));
                ServiceInstanceList sil = SCAWrapper.getUserSpecificInstance().getServiceInstances(siq);
                if (sil.getNumberOfServiceInstances() > 0) {
                    setCustomer(UserSpecificCachedDataHelper.getCustomer(sil.getServiceInstances().get(0).getCustomerId(),
                            StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP,
                            getCustomerQuery().getProductInstanceOffset(),
                            getCustomerQuery().getProductInstanceResultLimit()));
                } else {
                    localiseErrorAndAddToGlobalErrors("no.records.found");
                    return showSearchCustomer();
                }
            } else {
                localiseErrorAndAddToGlobalErrors("no.records.found");
                return showSearchCustomer();
            }

        }
        getCustomer().setDateOfBirth(Utils.addSlashesToDate(getCustomer().getDateOfBirth()));
        
        if(BaseUtils.getProperty("env.country.name").trim().equalsIgnoreCase("Tanzania")) {
            HashMap<String,String> iccidStatuses = new HashMap<String,String>();
            
            for(ProductInstance product : getCustomer().getProductInstances()) {
                String iccid = product.getPhysicalId();                
                String simVerificationStatus = getSimVerificationStatus(iccid);  
                
                if(!simVerificationStatus.equalsIgnoreCase("NoStatus")) {
                    iccidStatuses.put(iccid, simVerificationStatus);                
                }
            }
            setSimStatus(iccidStatuses);
        }
        
 /*       if(BaseUtils.getProperty("env.country.name").trim().equalsIgnoreCase("Nigeria") && (getCustomer().getCustomerRoles()!=null && getCustomer().getCustomerRoles().size()>0)) {
            HashMap<String,String> iccidUsers = new HashMap<String,String>();
            
            for(ProductInstance product : getCustomer().getProductInstances()) {
                String iccid = product.getPhysicalId();                
                String simUser = getIccidUser(iccid, getCustomer().getCustomerRoles().get(0).getOrganisationId());  
                
                iccidUsers.put(iccid, simUser);
            }
            setSimUser(iccidUsers);
        }
*/
        if (!getCustomer().getPassportExpiryDate().equalsIgnoreCase("")) {
            getCustomer().setPassportExpiryDate(Utils.addSlashesToDate(getCustomer().getPassportExpiryDate()));
        }

        if (!getCustomer().getVisaExpiryDate().equalsIgnoreCase("")) {
            getCustomer().setVisaExpiryDate(Utils.addSlashesToDate(getCustomer().getVisaExpiryDate()));
        }
        
        return getDDForwardResolution("/customer/view_customer.jsp");
    }
    
    public String getVerifier() {
        EventQuery evq = new EventQuery();
        
        evq.setEventKey(String.valueOf(getCustomer().getCustomerId()));
        evq.setEventType("IM");
        evq.setEventSubType("KYCChange");
        evq.setResultLimit(4);
        
        String log="";
        EventList eventsData = SCAWrapper.getUserSpecificInstance().getEvents(evq);
        
        
        for(Event ev: eventsData.getEvents()) {
            String[] evData = ev.getEventData().split("\\|");
            
            if(log.length()>0) {
                log += "<br>- " + Utils.formatDateLong(Utils.getXMLGregorianCalendarAsDate(ev.getDate(), new Date()))  + " Verification by Username: " + evData[2];
            } else {
                log = "- " + Utils.formatDateLong(Utils.getXMLGregorianCalendarAsDate(ev.getDate(), new Date())) + " Verification by Username: " + evData[2];
            }
            
        }        
        
        return log;
    }
    
    public String getSimVerificationStatus(String iccid) {        
        PreparedStatement ps = null;
        Connection conn = null;
        String status = "NoStatus";

        try {            
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            String query = "select tcra_response from additional_sim_webservice_info where customer_iccid =? limit 1";
            ps = conn.prepareStatement(query);
            ps.setString(1, iccid);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {                
                status = rs.getString("tcra_response");
                break;
            }             
            ps.close();
            conn.close();
            
        } catch (Exception ex) {
            log.error("Error occured getting status: " + ex);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    log.error("Error closing the prepared statement " + ex);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.error("Error closing the connection " + ex);
                }
            }
        }
        return status;
    }

    public Resolution previousProductInstancePage() {
        return retrieveCustomer();
    }

    public Resolution nextProductInstancePage() {
        return retrieveCustomer();
    }
    
    private boolean verifyAction = false;

    public boolean isVerifyAction() {
        return verifyAction;
    }

    public void setVerifyAction(boolean verifyAction) {
        this.verifyAction = verifyAction;
    }

    private List<PhoneNumberRange> numberRangesToPort;

    public List<PhoneNumberRange> getNumberRangesToPort() {
        return numberRangesToPort;
    }

    public void setNumberRangesToPort(List<PhoneNumberRange> numberRangesToPort) {
        this.numberRangesToPort = numberRangesToPort;
    }

    private List<String> availableNumbers;

    public List<String> getAvailableNumbers() {
        return availableNumbers;
    }

    public void setAvailableNumbers(List<String> availableNumbers) {
        this.availableNumbers = availableNumbers;
    }

    public List<String> getAllowedOptInLevels() {
        return BaseUtils.getPropertyAsList("global.opt.in.levels");
    }
    private List<String> fulfilmentItemsAllowed;

    public List<String> getFulfilmentItemsAllowed() {
        return fulfilmentItemsAllowed;
    }

    public void setFulfilmentItemsAllowed(List<String> fulfilmentItemsAllowed) {
        this.fulfilmentItemsAllowed = fulfilmentItemsAllowed;
    }
    private List<String> staffMembersAllowed;

    public List<String> getStaffMembersAllowed() {
        return staffMembersAllowed;
    }

    public void setStaffMembersAllowed(List<String> staffMembersAllowed) {
        this.staffMembersAllowed = staffMembersAllowed;
    }

    public Resolution showAddCustomerWizard() {
        checkPermissions(Permissions.ADD_CUSTOMER);
        setBiometricKyc(true);
        return getDDForwardResolution("/customer/add_customer_select_customer_type.jsp");
    }

    boolean biometricKyc;

    public boolean isBiometricKyc() {
        return biometricKyc;
    }

    public void setBiometricKyc(boolean biometricKyc) {
        this.biometricKyc = biometricKyc;
    }
    

    public Resolution showAddCustomerSearchAndConvertSaleslead() {
        if(getCustomer().getClassification().isEmpty()) {
            localiseErrorAndAddToGlobalErrors("Error", "Please supply customer type.");
            return showAddCustomerWizard();
        }
                
        if (BaseUtils.getBooleanProperty("env.customer.verify.with.nida", false)
                && isBiometricKyc() && !"diplomat".equalsIgnoreCase(getCustomer().getClassification())) { // Tanzania - get basic details from NIDA first
            log.debug("Insdide showAddCustomerSearchAndConvertSaleslead");
            if (getCustomer().getCustomerId() > 0) {
                if ("minor".equalsIgnoreCase(getCustomer().getClassification())) {
                    return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
                } else if ("company".equalsIgnoreCase(getCustomer().getClassification())) {
                    return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
                } else if ("foreigner".equalsIgnoreCase(getCustomer().getClassification())) {
                    String idNumberType = getCustomer().getIdentityNumberType();
                    log.debug("idNumberType******" + idNumberType);
                    if ("passport".equalsIgnoreCase(idNumberType)) {
                        getCustomerWithPhoto();
                        getCustomer().setClassification("foreigner");
                        setVerifyExistingCustomerWithNIDAOrImmigration(true);
                        return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
                    } else {
                        getCustomerWithPhoto();
                        getCustomer().setClassification("foreigner");
                        setVerifyExistingCustomerWithNIDAOrImmigration(true);
                        return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
                    }
                } else {
                    getCustomerWithPhoto();
                    setVerifyExistingCustomerWithNIDA(true);
                    return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
                }
            }

            if ("foreigner".equalsIgnoreCase(getCustomer().getClassification())) {
                
                setVerifyExistingCustomerImmigration(true);
                return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
            }
            return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
        } else {
            return getDDForwardResolution("/customer/add_customer_search_and_convert_saleslead.jsp");
        }
    }

    public Resolution showAddCustomerSkipSalesleadSearch() {
        return getDDForwardResolution("/customer/add_customer_capture_basic_details.jsp");
    }

    public Resolution showAddCustomerBackIntoSearchAndConvertSaleslead() {
        return getDDForwardResolution("/customer/add_customer_search_and_convert_saleslead.jsp");
    }

    public Resolution showAddCustomerBasicDetails() {

        return getDDForwardResolution("/customer/add_customer_capture_basic_details.jsp");

    }

    public Resolution verifyExistingCustomerWithNIDA() {

        setCustomerQuery(new CustomerQuery());
        getCustomerQuery().setCustomerId(getCustomer().getCustomerId());
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));

        // Check if customer has fingerprints.
        Photograph fingerPrint = null;
        for (Photograph photo : getCustomer().getCustomerPhotographs()) {
            if (photo.getPhotoType().equalsIgnoreCase("fingerprints")) {
                fingerPrint = photo;
            }
        }
        //Clear all photos, and only add the fingerprint
        getCustomer().getCustomerPhotographs().clear();

        if (fingerPrint != null) {
            getCustomer().getCustomerPhotographs().add(fingerPrint);
            getPhotographs().addAll(getCustomer().getCustomerPhotographs());
            writeCustomerPhotographsDataToFile(getPhotographs());
        }

        setVerifyExistingCustomerWithNIDA(true);

        return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");

    }

    public boolean verifyExistingCustomerWithNIDA = false;

    public boolean isVerifyExistingCustomerWithNIDA() {
        return verifyExistingCustomerWithNIDA;
    }

    public void setVerifyExistingCustomerWithNIDA(boolean nida) {
        this.verifyExistingCustomerWithNIDA = nida;
    }

    public boolean verifyExistingCustomerWithNIDAOrImmigration = false;

    public boolean isVerifyExistingCustomerWithNIDAOrImmigration() {
        return verifyExistingCustomerWithNIDAOrImmigration;
    }

    public void setVerifyExistingCustomerWithNIDAOrImmigration(boolean nida) {
        this.verifyExistingCustomerWithNIDAOrImmigration = nida;
    }

    public boolean verifyExistingCustomerImmigration = false;

    public boolean isVerifyExistingCustomerImmigration() {
        return verifyExistingCustomerImmigration;
    }

    public void setVerifyExistingCustomerImmigration(boolean nida) {
        this.verifyExistingCustomerImmigration = nida;
    }

    public Resolution captureDiplomatDocuments() {
        try {
            if (getCustomer() != null) {
                checkPermissions(Permissions.EDIT_CUSTOMER_PHOTOS);
                setCustomerQuery(new CustomerQuery());
                getCustomerQuery().setCustomerId(getCustomer().getCustomerId());
                getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);
                setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
                getCustomer().setClassification("diplomat");
                if (getCustomer().getSCAContext().getObviscated() != null && getCustomer().getSCAContext().getObviscated().equals("ob")) {
                    if (getIsDeliveryPerson() || getIsIndirectChannelPartner()) {
                        localiseErrorAndAddToGlobalErrors("not.pending.kyc");
                        return showSearchCustomer();
                    } else {
                        if (!isAllowed(BaseUtils.getProperty("env.staff.roles.to.be.obviscated"))) {
                            setPageMessage("not.your.customer");
                            return retrieveCustomer();
                        }
                    }
                }
                getPhotographs().addAll(getCustomer().getCustomerPhotographs());
                writeCustomerPhotographsDataToFile(getPhotographs());

            } else if (getOrganisation() != null) {
                checkPermissions(Permissions.EDIT_ORGANISATION_PHOTOS);
                setOrganisationQuery(new OrganisationQuery());
                getOrganisationQuery().setOrganisationId(getOrganisation().getOrganisationId());
                getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN_PHOTO);

                setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
                getPhotographs().addAll(getOrganisation().getOrganisationPhotographs());

                writeCustomerPhotographsDataToFile(getPhotographs());
            }

        } catch (SCABusinessError e) {
        }
        return getDDForwardResolution("/photograph/edit_photographs.jsp");
    }

    public Resolution getCustomersNidaDetailsAndUpdateCustomer() {

        String idNumberType = getCustomer().getIdentityNumberType();
        try {
            //Save the fingerprint that was used to verify with Nida
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
            Photograph fingerPrint = new Photograph();
            //fingerPrint.setData(Utils.decodeBase64(Utils.getDataFromTempFile(fingerPrint.getPhotoGuid())));
            fingerPrint.setPhotoType("fingerprints");
            fingerPrint.setPhotoGuid(getCustomer().getCustomerPhotographs().get(0).getPhotoGuid());

            setCustomerQuery(new CustomerQuery());
            getCustomerQuery().setCustomerId(getCustomer().getCustomerId());
            getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO_ADDRESS);
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
            getPhotographs().addAll(getCustomer().getCustomerPhotographs());

            String allowedNidaKycIdTypes = BaseUtils.getSubProperty("env.nida.config", "AllowedIdTypesRegex");

            if (idNumberType == null) {
                idNumberType = getCustomer().getIdentityNumberType();
            }

            if (!idNumberType.isEmpty()) {
                log.debug("IDTYPE " + idNumberType);
                if (!Utils.matchesWithPatternCache(idNumberType, allowedNidaKycIdTypes)) {
                    throw new Exception("Id type not enabled for eKyc verification -- customer ["
                            + getCustomer().getCustomerId() + "] is using id type [" + idNumberType + "].");
                }
            }

            //Check if this fingerprint has been used before?
            DocumentUniquenessQuery documentUniquenessQuery = new DocumentUniquenessQuery();
            documentUniquenessQuery.setDocumentHash(HashUtils.md5(Utils.getDataFromTempFile(fingerPrint.getPhotoGuid())));
            //An error will be thrown if document has been used before.
            SCAWrapper.getUserSpecificInstance().checkDocumentUniqueness_Direct(documentUniquenessQuery);

            NidaVerifyQuery request = new NidaVerifyQuery();

            request.setIdentityNumber(getCustomer().getIdentityNumber());

            request.setFingerprintB64(Utils.getDataFromTempFile(fingerPrint.getPhotoGuid()));
            request.setVerifiedBy(getUser());
            request.setEntityId(Integer.toString(getCustomer().getCustomerId()));
            request.setEntityType("UPDATECUSTOMER");

            NidaVerifyReply nidaResponse = SCAWrapper.getUserSpecificInstance().verifyCustomerWithNIDA_Direct(request);
            if (nidaResponse.getCode() != null && nidaResponse.getCode().equalsIgnoreCase(BaseUtils.getProperty("env.defaced.status.code"))) {
                return getDDForwardResolution("/customer/alternative_customer_verification.jsp");
            }

            //getCustomer().getKYCStatus();
            // setCustomer(new Customer());
            getCustomer().setFirstName(nidaResponse.getFirstName());
            getCustomer().setMiddleName(nidaResponse.getMiddleName());
            getCustomer().setLastName(nidaResponse.getLastName());
            getCustomer().setKYCStatus("V"); // N = NIDA Verified

            getCustomer().setGender(nidaResponse.getGender());
            getCustomer().setDateOfBirth(nidaResponse.getDateOfBirth());
            getCustomer().setAlternativeContact1(nidaResponse.getPhoneNumber());

            //Physical address
            int physicalAddressId = 0;
            int postalAddressId = 0;
            for (Address curAddr : getCustomer().getAddresses()) {
                if (curAddr.getType() != null && curAddr.getType().equalsIgnoreCase("Physical Address")) {
                    physicalAddressId = curAddr.getAddressId();
                }
                if (curAddr.getType() != null && curAddr.getType().equalsIgnoreCase("Postal Address")) {
                    postalAddressId = curAddr.getAddressId();
                }
            }
            log.warn("NIDA Verifying, current physical address id is [{}], current postal address is [{}]", physicalAddressId, postalAddressId);
            Address addrPhy = new Address();
            addrPhy.setCustomerId(getCustomer().getCustomerId());
            addrPhy.setAddressId(physicalAddressId);
            addrPhy.setType("Physical Address");
            addrPhy.setLine1(nidaResponse.getResidentHouseNo());
            addrPhy.setLine2(nidaResponse.getResidentStreet() + "," + nidaResponse.getResidentWard()
                    + "," + nidaResponse.getResidentVillage());
            addrPhy.setZone(nidaResponse.getResidentWard());
            addrPhy.setTown(nidaResponse.getResidentDistrict());
            addrPhy.setCode(nidaResponse.getResidentPostCode());
            addrPhy.setState(nidaResponse.getResidentRegion());
            addrPhy.setCountry(BaseUtils.getProperty("env.country.name"));
            // LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), ""));
            Address addrPost = new Address();
            addrPost.setCustomerId(getCustomer().getCustomerId());
            addrPost.setAddressId(postalAddressId);
            addrPost.setType("Postal Address");
            addrPost.setLine1(nidaResponse.getResidentPostalAddress());
            addrPost.setCode(nidaResponse.getResidentPostCode());
            addrPost.setCountry(BaseUtils.getProperty("env.country.name"));
            addrPost.setZone(nidaResponse.getResidentWard());

            byte[] fileData = Utils.decodeBase64(nidaResponse.getPhoto());
            String fileExtension = "jpg";
            String photoGuid = Utils.getUUID() + "." + fileExtension;
            File tmpFile = Utils.createTempFile(photoGuid, fileData);
            Photograph photo = new Photograph();
            photo.setPhotoGuid(photoGuid);
            photo.setPhotoType("photo");
            // photo.setData(nidaResponse.getPhoto()); //Base64 photo data
            log.debug("Wrote tmp file to [{}] of size [{}]", tmpFile.getAbsolutePath(), fileData.length);

            //Set photograph and fingerprints
            Photograph curPhoto;
            Iterator<Photograph> itrPhotos = getCustomer().getCustomerPhotographs().iterator();
            while (itrPhotos.hasNext()) {
                curPhoto = itrPhotos.next();
                if (curPhoto.getPhotoType().equalsIgnoreCase("photo") || curPhoto.getPhotoType().equalsIgnoreCase("fingerprints")) {
                    itrPhotos.remove();
                    // break;
                }
            }
            //Add the photo  from NIDA
            getCustomer().getCustomerPhotographs().add(photo);
            //Add fingerprints
            getCustomer().getCustomerPhotographs().add(fingerPrint);

            setCustomerPhotographsData(getCustomer().getCustomerPhotographs());

            log.error("Customer has [{}] photos", getCustomer().getCustomerPhotographs().size());

            SCAWrapper.getUserSpecificInstance().modifyCustomer(getCustomer());
            // UPdate address details here;
            // Physical
            if (addrPhy.getLine2() == null || addrPhy.getLine2().isEmpty()) {
                addrPhy.setLine2(" ");
            }
            if (addrPhy.getTown() == null || addrPhy.getTown().isEmpty()) {
                addrPhy.setTown(" ");
            }
            if (addrPhy.getState() == null || addrPhy.getState().isEmpty()) {
                addrPhy.setState(" ");
            }

            if (addrPost.getLine2() == null || addrPost.getLine2().isEmpty()) {
                addrPost.setLine2(" ");
            }

            if (addrPost.getTown() == null || addrPost.getTown().isEmpty()) {
                addrPost.setTown(" ");
            }

            if (addrPost.getState() == null || addrPost.getState().isEmpty()) {
                addrPost.setState(" ");
            }

            if (addrPhy.getLine1() != null && !addrPhy.getLine1().isEmpty()) {
                if (addrPhy.getAddressId() != 0) {
                    log.warn("Going to modify physical address with id [{}]", addrPhy.getAddressId());
                    SCAWrapper.getUserSpecificInstance().modifyAddress(addrPhy);
                } else {//Add as 
                    log.warn("Going to add new physical address for customer [{}]", addrPhy.getCustomerId());
                    SCAWrapper.getUserSpecificInstance().addAddress(addrPhy);
                }
            }
            //Postal
            if (addrPost.getLine1() != null && !addrPost.getLine1().isEmpty()) {
                if (addrPost.getAddressId() != 0) {
                    log.warn("Going to modify postal address with id [{}]", addrPost.getAddressId());
                    SCAWrapper.getUserSpecificInstance().modifyAddress(addrPost);
                } else {
                    log.warn("Going to add new postal address for customer [{}]", addrPost.getCustomerId());
                    // getCustomer().getAddresses().add(addrPost);
                    SCAWrapper.getUserSpecificInstance().addAddress(addrPost);
                }
            }
            // http://jira.smilecoms.com/browse/HBT-9643
            //Now cheeck how many products does the customer have - if they have 1 product and NIDA was veried successfully, also update their 
            // SIM service to verified.
            setCustomerQuery(new CustomerQuery());
            getCustomerQuery().setCustomerId(getCustomer().getCustomerId());
            getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP);
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
            // -----------------------------------------------------------------------------------------------------
            if (getCustomer().getProductInstances().size() == 1) { //As requested by Pamela, in this scenario, also set the SIM service for this product as verified
                //See URL: http://jira.smilecoms.com/browse/HBT-9643
                ProductInstance pi = getCustomer().getProductInstances().get(0);
                //setProductOrder(new ProductOrder());
                //Find SIM service
                ServiceInstance si = null;
                for (ProductServiceInstanceMapping m : getCustomer().getProductInstances().get(0).getProductServiceInstanceMappings()) {
                    if (m.getServiceInstance().getServiceSpecificationId() == 1) { //SIM Found
                        si = m.getServiceInstance();
                        break;
                    }
                }

                if (si != null) { //SIM found 
                    populateAVPDetailsIntoServiceInstanceAVPs(si);
                    setProductOrder(new ProductOrder());
                    getProductOrder().setAction(StAction.NONE);
                    getProductOrder().setOrganisationId(pi.getOrganisationId());
                    getProductOrder().setCustomerId(pi.getCustomerId());
                    getProductOrder().setProductInstanceId(si.getProductInstanceId());
                    getProductOrder().getServiceInstanceOrders().add(new ServiceInstanceOrder());
                    getProductOrder().getServiceInstanceOrders().get(0).setAction(StAction.UPDATE);
                    getProductOrder().getServiceInstanceOrders().get(0).setServiceInstance(new ServiceInstance());
                    getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setServiceInstanceId(si.getServiceInstanceId());
                    getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setServiceSpecificationId(si.getServiceSpecificationId());
                    getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setCustomerId(si.getCustomerId());
                    getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setStatus(si.getStatus());
                    getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setAccountId(si.getAccountId());

                    AVP status = new AVP();
                    status.setAttribute("KYCStatus");
                    status.setValue(StringEscapeUtils.unescapeHtml("Complete"));
                    getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs().add(status);

                    AVP method = new AVP();
                    method.setAttribute("KYCVerifyingMethod");
                    method.setValue(StringEscapeUtils.unescapeHtml("NIDA Pilot"));
                    getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs().add(method);

                    AVP trans = new AVP();
                    trans.setAttribute("NIDATransactionId");
                    trans.setValue(nidaResponse.getNIDATransactionId());
                    getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs().add(trans);

                    AVP date = new AVP();
                    date.setAttribute("NIDAVerifiedOnDate");
                    date.setValue(sdf.format(new Date()));
                    getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs().add(date);

                    formatAVPsForSendingToSCA(getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs(), getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getServiceSpecificationId(), true);
                    log.warn("Size of APVs before calling processOder is: [{}]", getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs().size());
                    SCAWrapper.getUserSpecificInstance().processOrder(getProductOrder());
                } else { //SIM was not found
                    log.error("SIM service was  not found under product instance id [{}]", getCustomer().getProductInstances().get(0).getProductInstanceId());
                }
            }

            if (getCustomer() != null) {
                setPageMessage("customer.updated.successfully");
            }
        } catch (SCABusinessError ex) {
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            // localiseErrorAndAddToGlobalErrors("nida.validation.failed", ex.getMessage());
            setVerifyExistingCustomerWithNIDA(true);
            throw new RuntimeException(ex);
        }

        return retrieveCustomer();
    }

    private void formatAVPsForSendingToSCA(List<AVP> avPs, int serviceSpecificationId, boolean populatePhotoData) {
        if (avPs == null) {
            return;
        }
        for (AVP avp : avPs) {
            if (avp != null && avp.getAttribute() != null) {

                // log.error("Now doing: Avp name[{}]", avp.getAttribute());
                AVP avpConfig = getAVPConfig(avp.getAttribute(), serviceSpecificationId);
                if (avpConfig != null) {
                    avp.setInputType(avpConfig.getInputType());
                    if (avpConfig.getInputType().equals("photo") && populatePhotoData) {
                        String guid = avp.getValue();
                        log.debug("The value of the GUID for attribute [{}] is [{}]", avp.getAttribute(), guid);
                        try {
                            avp.setValue(guid + "|" + Utils.encodeBase64(Utils.getDataFromTempFile(guid)));
                        } catch (Exception ex) {
                            log.warn("Error", ex);
                        }
                    } else if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                        avp.setValue(Utils.getPublicIdentityForPhoneNumber(avp.getValue()));
                    } else if (avp.getAttribute().equalsIgnoreCase("NAIUsername")) {
                        avp.setValue(Utils.makeNAIIdentityFromUsername(avp.getValue()));
                    } else if (avp.getAttribute().equalsIgnoreCase("NAIPassword")) {
                        try {
                            avp.setValue(Utils.hashPasswordWithComplexityCheck(avp.getValue()));
                        } catch (Exception ex) {
                            avp.setValue(Utils.oneWayHash(avp.getValue()));
                        }
                    } else {
                        avp.setValue(StringEscapeUtils.unescapeHtml(avp.getValue()));
                    }
                }

            }
        }
    }

    private AVP getAVPConfig(String attributeName, int serviceSpecificationId) {
        ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(serviceSpecificationId);
        for (AVP ssAVP : ss.getAVPs()) {
            if (attributeName.equals(ssAVP.getAttribute())) {
                return ssAVP;
            }
        }
        return null;
    }

    private Resolution showMinorCustomerDetails(String nin) {
        log.debug("called showMinorCustomerDetails. parameters [{}]", nin);
        if (nin == null || nin.trim().equals("")) {
            return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
        }
        CustomerQuery requestObject = new CustomerQuery();
        requestObject.setIdentityNumber(nin);
        requestObject.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
        Customer parent = SCAWrapper.getUserSpecificInstance().getCustomer(requestObject);

        if (parent == null || !"nationalid".equals(parent.getIdentityNumberType())) {
            log.debug("parentl is not found or id type is not nationalid.");
            return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
        }
        getCustomer().setKYCStatus("V"); // Nida verified
        getCustomer().setIdentityNumberType("parentnin");
        getCustomer().setIdentityNumber(nin);

        for (Address add : parent.getAddresses()) {
            getCustomer().getAddresses().add(add);
        }
        return getDDForwardResolution("/customer/add_customer_capture_basic_details.jsp");
    }

    private Resolution showCompanyCustomerDetails(String nin) {
        log.debug("called showCompanyCustomerDetails. repransative [{}]", nin);
        if (nin == null || nin.trim().equals("")) {
            return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
        }
        CustomerQuery requestObject = new CustomerQuery();
        requestObject.setIdentityNumber(nin);
        requestObject.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
        Customer representative = SCAWrapper.getUserSpecificInstance().getCustomer(requestObject);

        if (representative == null || !"nationalid".equals(representative.getIdentityNumberType())) {
            return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
        }
        getCustomer().setKYCStatus("V"); // Nida verified
        getCustomer().setIdentityNumberType("representativenin");
        getCustomer().setIdentityNumber(nin);
        //getCustomer().setDateOfBirth(representative.getDateOfBirth());

        for (Address add : representative.getAddresses()) {
            getCustomer().getAddresses().add(add);
        }
        return getDDForwardResolution("/customer/add_customer_capture_basic_details.jsp");
    }

    private Resolution showForeignerDetailsFromImmigration(String passport) {
        log.debug("called showForeignerDetailsFromImmigration. repransative [{}]", passport);
        if (passport == null || passport.trim().equals("")) {
            localiseErrorAndAddToGlobalErrors("error", "Passport Number not supplied.");
            return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
        }
        try {
            byte[] fingerPrintData = Utils.getDataFromTempFile(getCustomer().getCustomerPhotographs().get(0).getPhotoGuid());

            DocumentUniquenessQuery documentUniquenessQuery = new DocumentUniquenessQuery();
            documentUniquenessQuery.setDocumentHash(HashUtils.md5(fingerPrintData));
            //An error will be thrown if document has been used before.
            SCAWrapper.getUserSpecificInstance().checkDocumentUniqueness_Direct(documentUniquenessQuery);
            log.debug("Customer passpport countyCode [{}]", getCustomer());
            VerifyForeignerQuery verifyForeignerQuery = new VerifyForeignerQuery();
            verifyForeignerQuery.setDocumentNo(passport);
            verifyForeignerQuery.setCountryCode(getCustomer().getCountryCode());
            verifyForeignerQuery.setFingerprintB64(fingerPrintData);
            verifyForeignerQuery.setVerifiedBy(getUser());
            verifyForeignerQuery.setEntityId(Integer.toString(getCustomer().getCustomerId()));
            verifyForeignerQuery.setEntityType("NEWCUSTOMER");

            VerifyForeignerReply verifyForeignerReply = SCAWrapper.getUserSpecificInstance().verifyForeignerCustomer_Direct(verifyForeignerQuery);

            if (verifyForeignerReply.getStatus().equalsIgnoreCase("0")) {
                SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat formatter3 = new SimpleDateFormat("yyyy/MM/dd");
                SimpleDateFormat formatter4 = new SimpleDateFormat("yyyy/MM/dd");
                Date dateOfBirth = formatter1.parse(verifyForeignerReply.getDateOfBirth());
                Date passportExpiryDate = formatter2.parse(verifyForeignerReply.getPassportExpiryDate());
                getCustomer().setFirstName(verifyForeignerReply.getGivenName());
                getCustomer().setMiddleName("");
                getCustomer().setLastName(verifyForeignerReply.getSurname());

                getCustomer().setGender(verifyForeignerReply.getSex());
                getCustomer().setDateOfBirth(formatter3.format(dateOfBirth));
                getCustomer().setKYCStatus("V");                
                getCustomer().setIdentityNumberType("passport");
                getCustomer().setPassportExpiryDate(formatter4.format(passportExpiryDate));
                getCustomer().setClassification("foreigner");
                getCustomer().setNationality(iso3CountryCodeToIso2CountryCode(verifyForeignerReply.getNationality()));

                Address addrPhy = new Address();
                addrPhy.setType("Physical Address");
                addrPhy.setLine1(verifyForeignerReply.getResidentRegion());
                addrPhy.setLine2(verifyForeignerReply.getResidentRegion() + "," + verifyForeignerReply.getResidentWard()
                        + "," + verifyForeignerReply.getResidentDistrict());
                addrPhy.setZone(verifyForeignerReply.getResidentWard());
                addrPhy.setTown(verifyForeignerReply.getResidentDistrict());
                addrPhy.setCode("");
                addrPhy.setState(verifyForeignerReply.getResidentRegion());
                addrPhy.setCountry(BaseUtils.getProperty("env.country.name"));
                // LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), ""));
                Address addrPost = new Address();
                addrPost.setType("Postal Address");
                addrPost.setLine1("");
                addrPost.setLine2(verifyForeignerReply.getResidentWard());
                addrPost.setCode("");
                addrPost.setCountry(BaseUtils.getProperty("env.country.name"));
                addrPost.setZone(verifyForeignerReply.getResidentWard());

                getCustomer().getAddresses().add(addrPhy);
                if (addrPost.getLine1() != null && !addrPost.getLine1().isEmpty()) {
                    getCustomer().getAddresses().add(addrPost);
                }

                byte[] fileData = Utils.decodeBase64(verifyForeignerReply.getPhoto());
                String fileExtension = "jpg";
                File tmpFile = Utils.createTempFile(Utils.getUUID() + "." + fileExtension, fileData);
                Photograph photo = new Photograph();
                photo.setPhotoGuid(tmpFile.getName());
                photo.setPhotoType("photo");

//                getCustomer().setAlternativeContact1("");
                getCustomer().getCustomerPhotographs().add(photo);
                getCustomer().setClassification("foreigner");

                log.debug("Wrote tmp file to [{}] of size [{}]", tmpFile.getAbsolutePath(), fileData.length);

//                getCustomer().getCustomerPhotographs().add(photo);
            } else {
                if (verifyForeignerReply.getMessage() != null) {
                    setVerifyExistingCustomerImmigration(true);
                    localiseErrorAndAddToGlobalErrors(verifyForeignerReply.getMessage());
                    return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
                } else {
                    setVerifyExistingCustomerImmigration(true);
                    localiseErrorAndAddToGlobalErrors("Please provide the valid document.");
                    return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
                }
            }
            setVerifyExistingCustomerImmigration(true);
            return getDDForwardResolution("/customer/add_customer_capture_basic_details.jsp");

        } catch (Exception ex) {
            log.error("Error:", ex);
            throw new RuntimeException(ex);
        }
    }
    
    String nimcTrackingId="";

    public java.lang.String getNimcTrackingId() {
        return nimcTrackingId;
    }

    public void setNimcTrackingId(java.lang.String nimcTrackingId) {
        this.nimcTrackingId = nimcTrackingId;
    }

    
    public Resolution getCustomersNidaDetailsAndShowAddCustomerBasicDetails() {
        setNimcTrackingId("");
        try {
           
            String nin = getCustomer().getIdentityNumber();
            
            CustomerQuery q = new CustomerQuery();
            q.setNationalIdentityNumber(nin.trim());
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            q.setResultLimit(1);
            
            CustomerList cust = SCAWrapper.getUserSpecificInstance().getCustomers(q);
            
            if(cust.getCustomers().size()>0) {
                localiseErrorAndAddToGlobalErrors("Error", "Cannot create profile. A profile with NIN " + nin + " already exists.");
                setCustomer(new Customer());
                return showAddCustomerWizard();
            }
            
            if ("minor".equalsIgnoreCase(getCustomer().getClassification())) {
                return showMinorCustomerDetails(nin);
            }
            if ("company".equalsIgnoreCase(getCustomer().getClassification())) {
                return showCompanyCustomerDetails(nin);
            }
            String idNumberType = getCustomer().getIdentityNumberType();
            if ("foreigner".equalsIgnoreCase(getCustomer().getClassification())) {
                if ("passport".equalsIgnoreCase(idNumberType)) {
                    log.debug("Customer Id Number Type " + idNumberType);
                    return showForeignerDetailsFromImmigration(nin);
                }
            }

            if (BaseUtils.getBooleanProperty("env.customer.verify.with.nin", false)) {
                log.debug("Customer Id Number Type " + idNumberType);
                String errorMessage="";
                
                if(getCustomer().getIdentityNumber().trim().isEmpty() || 
                    getCustomer().getIdentityNumber().trim().length()!=11 ||
                    !StringUtils.isNumeric(getCustomer().getIdentityNumber().trim())) {
                    errorMessage+="- ID/NIN needs to be 11 digits.<br/>";
                }
                
                if(errorMessage.trim().length()>0) {
                    setBiometricKyc(true);
                    localiseErrorAndAddToGlobalErrors("validation.errors", "<br/>" +errorMessage);
                    return showAddCustomerWizard();
                }
                
                VerifyNinQuery verifyNinQuery = new VerifyNinQuery();
                verifyNinQuery.setNin(getCustomer().getIdentityNumber());

//                if (BaseUtils.getBooleanProperty("env.nimc.use.fingerprint", true)) {
//                    verifyNinQuery.setFingerStringInBase64(fingerPrintData);
//                } else {
//                    verifyNinQuery.setFingerStringInBase64(null);
//                }
                verifyNinQuery.setFirstName(getCustomer().getFirstName());
                verifyNinQuery.setLastName(getCustomer().getLastName());
                verifyNinQuery.setSurname(getCustomer().getMiddleName());
                verifyNinQuery.setVerifiedBy(getUser());
                verifyNinQuery.setEntityId(Integer.toString(getCustomer().getCustomerId()));
                verifyNinQuery.setEntityType("NEWCUSTOMER");
                VerifyNinResponseList verifyNinResponseList= new VerifyNinResponseList();
                
                try {
                        verifyNinResponseList= SCAWrapper.getUserSpecificInstance().verifyNinCustomer_Direct(verifyNinQuery);
                } catch (Exception e) {
                    setBiometricKyc(true);
                    errorMessage = e.getMessage().substring(e.getMessage().indexOf("with description (")+18);
                    localiseErrorAndAddToGlobalErrors("validation.errors", "<br/>NIN verification returned:  " +errorMessage);
                    return showAddCustomerWizard();
                }
                
                VerifyNinReply ninResponse = verifyNinResponseList.getVerifyNinReplys().get(0);
                
                if(ninResponse.getTrackingId().startsWith("BV")) {
                    localiseErrorAndAddToGlobalErrors("Error", "<br>- Client Regulator reference indicate a BVN registration. [" + ninResponse.getTrackingId() + "]<br>- Customer still needs to register directly with NIMC");
                    return showAddCustomerWizard();
                }
                
                setNimcTrackingId(ninResponse.getTrackingId());
                
                log.warn("NIN RESPONSE is {}", ninResponse.toString());
                // setCustomer(new Customer());
                SimpleDateFormat sdf1 = new SimpleDateFormat("dd-MM-yyyy");
                Date dob = sdf1.parse(ninResponse.getBirthdate());
                sdf1.applyPattern("yyyy/MM/dd");

                getCustomer().setTitle(ninResponse.getTitle().trim());
                getCustomer().setFirstName(ninResponse.getFirstname().trim());
                getCustomer().setMiddleName(ninResponse.getMiddlename().trim());
                getCustomer().setLastName(ninResponse.getSurname().trim());

                getCustomer().setGender(ninResponse.getGender().toUpperCase());
                getCustomer().setDateOfBirth(sdf1.format(dob));
                getCustomer().setKYCStatus("V"); // Nida verified
                getCustomer().setIdentityNumberType("nationalid");
                getCustomer().setNationalIdentityNumber(ninResponse.getNin().trim());
                getCustomer().setIsNinVerified("Y");                
                Address addrPhy = new Address();
                addrPhy.setType("Physical Address");
                addrPhy.setLine1(ninResponse.getResidenceAdressLine1().trim());
                addrPhy.setLine2((ninResponse.getResidenceAdressLine2() != null && !ninResponse.getResidenceAdressLine2().isEmpty()) ? ninResponse.getResidenceAdressLine2() : "NA");
                addrPhy.setZone(ninResponse.getResidenceLga().trim());
                addrPhy.setTown(ninResponse.getResidenceTown().trim());
                addrPhy.setCode(ninResponse.getResidencePostalcode().trim());
                addrPhy.setState(ninResponse.getResidenceState().trim());                
                addrPhy.setCountry(BaseUtils.getProperty("env.country.name"));
                // LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), ""));
//                Address addrPost = new Address();
//                addrPost.setType("Postal Address");
//                addrPost.setLine1(ninResponse.getResidentPostalAddress());
//                addrPost.setLine2(ninResponse.getResidentPostalAddress());
//                addrPost.setCode(ninResponse.getResidentPostCode());
//                addrPost.setCountry(BaseUtils.getProperty("env.country.name"));
//                addrPost.setZone(ninResponse.getResidentWard());

//                if (addrPost.getLine1() != null && !addrPost.getLine1().isEmpty()) {
//                    getCustomer().getAddresses().add(addrPost);
//                } else {
//                    addrPhy.setLine1("NA");
//                }
                getCustomer().getAddresses().add(addrPhy);

                byte[] fileData = Utils.decodeBase64(ninResponse.getPhoto());
                String fileExtension = "jpg";
                File tmpFile = Utils.createTempFile(Utils.getUUID() + "." + fileExtension, fileData);
                Photograph photo = new Photograph();
                photo.setPhotoGuid(tmpFile.getName());
                photo.setPhotoType("photo");
                if ("nationalid".equalsIgnoreCase(getCustomer().getClassification())) {
                    getCustomer().setClassification("foreigner");
                }

                getCustomer().setAlternativeContact1(ninResponse.getTelephoneno());

                getCustomer().getCustomerPhotographs().add(photo);
                getPhotographs().addAll(getCustomer().getCustomerPhotographs());              
                return getDDForwardResolution("/customer/add_customer_capture_basic_details.jsp");
            } else {

                log.debug("Customer Id Number Type " + idNumberType);
                // Retrieve the fingerprint data here
                byte[] fingerPrintData = Utils.getDataFromTempFile(getCustomer().getCustomerPhotographs().get(0).getPhotoGuid());

                //Check if fingerprint has been used before?
                DocumentUniquenessQuery documentUniquenessQuery = new DocumentUniquenessQuery();
                documentUniquenessQuery.setDocumentHash(HashUtils.md5(fingerPrintData));
                //An error will be thrown if document has been used before.
                SCAWrapper.getUserSpecificInstance().checkDocumentUniqueness_Direct(documentUniquenessQuery);

                // HashUtils.md5(photo.getData())
                NidaVerifyQuery request = new NidaVerifyQuery();

                request.setIdentityNumber(nin);
                request.setFingerprintB64(fingerPrintData);

                request.setVerifiedBy(getUser());
                request.setEntityId(Integer.toString(getCustomer().getCustomerId()));
                request.setEntityType("NEWCUSTOMER");

                NidaVerifyReply nidaResponse = SCAWrapper.getUserSpecificInstance().verifyCustomerWithNIDA_Direct(request);

                if (nidaResponse.getCode() != null && nidaResponse.getCode().equalsIgnoreCase(BaseUtils.getProperty("env.defaced.status.code"))) {
                    return getDDForwardResolution("/customer/alternative_customer_verification.jsp");
                }

                // setCustomer(new Customer());
                getCustomer().setFirstName(nidaResponse.getFirstName());
                getCustomer().setMiddleName(nidaResponse.getMiddleName());
                getCustomer().setLastName(nidaResponse.getLastName());

                getCustomer().setGender(nidaResponse.getGender());
                getCustomer().setDateOfBirth(nidaResponse.getDateOfBirth());
                getCustomer().setKYCStatus("V"); // Nida verified
                getCustomer().setIdentityNumberType("nationalid");

                Address addrPhy = new Address();
                addrPhy.setType("Physical Address");
                addrPhy.setLine1(nidaResponse.getResidentHouseNo());
                addrPhy.setLine2(nidaResponse.getResidentStreet() + "," + nidaResponse.getResidentWard()
                        + "," + nidaResponse.getResidentVillage());
                addrPhy.setZone(nidaResponse.getResidentWard());
                addrPhy.setTown(nidaResponse.getResidentDistrict());
                addrPhy.setCode(nidaResponse.getResidentPostCode());
                addrPhy.setState(nidaResponse.getResidentRegion());
                addrPhy.setCountry(BaseUtils.getProperty("env.country.name"));
                // LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), ""));
                Address addrPost = new Address();
                addrPost.setType("Postal Address");
                addrPost.setLine1(nidaResponse.getResidentPostalAddress());
                addrPost.setLine2(nidaResponse.getResidentPostalAddress());
                addrPost.setCode(nidaResponse.getResidentPostCode());
                addrPost.setCountry(BaseUtils.getProperty("env.country.name"));
                addrPost.setZone(nidaResponse.getResidentWard());

                if (addrPost.getLine1() != null && !addrPost.getLine1().isEmpty()) {
                    getCustomer().getAddresses().add(addrPost);
                } else {
                    addrPhy.setLine1("NA");
                }
                getCustomer().getAddresses().add(addrPhy);

                byte[] fileData = Utils.decodeBase64(nidaResponse.getPhoto());
                String fileExtension = "jpg";
                File tmpFile = Utils.createTempFile(Utils.getUUID() + "." + fileExtension, fileData);
                Photograph photo = new Photograph();
                photo.setPhotoGuid(tmpFile.getName());
                photo.setPhotoType("photo");
                if ("nationalid".equalsIgnoreCase(getCustomer().getClassification())) {
                    getCustomer().setClassification("foreigner");
                }

                getCustomer().setAlternativeContact1(nidaResponse.getPhoneNumber());

                log.debug("Wrote tmp file to [{}] of size [{}]", tmpFile.getAbsolutePath(), fileData.length);
                
                getCustomer().getCustomerPhotographs().add(photo);
               
                return getDDForwardResolution("/customer/add_customer_capture_basic_details.jsp");
            }

        } catch (Exception ex) {
            log.error("Error:", ex);
            throw new RuntimeException(ex);
        }
        // return  getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp"); 
    }

    public Resolution showAddCustomerBackIntoBasicDetails() {
        return getDDForwardResolution("/customer/add_customer_capture_basic_details.jsp");
    }

    public Resolution showAddOrganisationBackIntoBasicDetails() {
        return getDDForwardResolution("/organisation/add_organisation_capture_basic_details.jsp");
    }

    public Resolution showSetCustomerUsernamePasswordNext() {
        return getDDForwardResolution("/customer/add_customer_set_username_password.jsp");
    }

    public Resolution showSetCustomerUsernamePasswordBack() {
        return getDDForwardResolution("/customer/add_customer_set_username_password.jsp");
    }

    private boolean isCustomerMinor(String dob) {
        log.debug("call isCustomerMinor. Parameters [{}]", dob);
        boolean isMinor = false;
        try {
            int currentAge = getAge(dob);
            return currentAge < 18;
        } catch (ParseException ex) {
            isMinor = false;
        }
        return isMinor;
    }

    private int getAge(String ageStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        Date d = sdf.parse(ageStr);
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int date = c.get(Calendar.DATE);
        LocalDate l1 = LocalDate.of(year, month, date);
        LocalDate now = LocalDate.now();
        Period diff = Period.between(l1, now);
        return diff.getYears();
    }

    public Resolution captureCustomerScannedDocuments() {
        if (!hasPermissions(Permissions.ADD_CUSTOMER_PHOTOS)) {
            return showAddCustomerManageCustomerAddresses();
        }
        if (getCustomer().getClassification().equals("minor") && !isCustomerMinor(getCustomer().getDateOfBirth())) {
            localiseErrorAndAddToGlobalErrors("Customer is not minor");
            
            if(BaseUtils.getBooleanProperty("env.new.cutomer.nimc.consent.required", false)) {
                return getDDForwardResolution("/customer/add_customer_select_customer_type.jsp");
            } else {
                return getDDForwardResolution("/customer/add_customer_capture_basic_details.jsp");
            }
        }
        
        
        
        if(BaseUtils.getBooleanProperty("env.new.cutomer.nimc.consent.required", false) && getCustomer().getClassification().equalsIgnoreCase("customer")) {
            
            
            if(getPhotographs().size()>0) {
            
                try {
                    byte[] consentData = Utils.getDataFromTempFile(getPhotographs().get(0).getPhotoGuid());
                    String fileExt = getPhotographs().get(0).getPhotoGuid().substring(getPhotographs().get(0).getPhotoGuid().indexOf(".") + 1);
                    File tmpConsentFile = Utils.createTempFile(Utils.getUUID() + "." + fileExt, consentData);
                    Photograph consentForm = new Photograph();
                    consentForm.setPhotoGuid(tmpConsentFile.getName());
                    consentForm.setPhotoType("consentform");

                    getCustomer().getCustomerPhotographs().add(consentForm);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    localiseErrorAndAddToGlobalErrors("Error", "Something went wrong attaching consent form. Please retry");
                    return getDDForwardResolution("/customer/add_customer_select_customer_type.jsp");
                }
                    
            }
         
            if(getPhotographs().size()<1) {
                localiseErrorAndAddToGlobalErrors("Error", "Consent form not supplied");
                return getDDForwardResolution("/customer/add_customer_select_customer_type.jsp");
            }
        } 
        
        return getDDForwardResolution("/customer/add_customer_capture_scanned_documents.jsp");
    }

    public Resolution captureOrganisationScannedDocuments() {
        return getDDForwardResolution("/organisation/add_organisation_capture_scanned_documents.jsp");
    }

    public Resolution captureOrganisationScannedDocumentsBack() {
        return getDDForwardResolution("/organisation/add_organisation_capture_scanned_documents.jsp");
    }

    public Resolution captureCustomerScannedDocumentsBack() {
        if (!hasPermissions(Permissions.ADD_CUSTOMER_PHOTOS)) {
            return showAddCustomerBackIntoBasicDetails();
        }
        return getDDForwardResolution("/customer/add_customer_capture_scanned_documents.jsp");
    }

    public Resolution showAddCustomerWizardSummary() {
        return getDDForwardResolution("/customer/add_customer_summary.jsp");
    }

    public Resolution showAddOrganisationWizardSummary() {
        return getDDForwardResolution("/organisation/add_organisation_summary.jsp");
    }

    public Resolution showAddCustomerManageCustomerAddresses() {
        return getDDForwardResolution("/customer/add_customer_manage_customer_addresses.jsp");
    }

    public Resolution showAddCustomerManageCustomerAddressesSummaryBack() {
        return getDDForwardResolution("/customer/add_customer_manage_customer_addresses.jsp");
    }

    public Resolution showAddOrganisationManageOrganisationAddressesSummaryBack() {

        return getDDForwardResolution("/organisation/add_organisation_manage_organisation_addresses.jsp");
    }

    public Resolution showAddOrganisationManageOrganisationAddresses() {
        return getDDForwardResolution("/organisation/add_organisation_manage_organisation_addresses.jsp");
    }
    
    public Resolution processSaleKycProductActivation() {
        checkPermissions(Permissions.REACTIVATE_SERVICE_INSTANCE);
        
        if(getCustomer().getNationalIdentityNumber()==null || getCustomer().getNationalIdentityNumber().isEmpty()) {
            localiseErrorAndAddToGlobalErrors("error", "Customer profile does not have a NIN.");
            return retrieveKycProducts();
        }
        
        setVerifyAction(true);
        return getDDForwardResolution("/customer/customer_smile_nimc_compare.jsp");
    }
    
    public Resolution showNinAccounts() {
        checkPermissions(Permissions.VIEW_CUSTOMER);
        checkPermissions(Permissions.VIEW_ACCOUNT);
        return getDDForwardResolution("/customer/customer_view_nin_accounts.jsp");
    }
  
    public Resolution retrieveNinAccounts() {
        checkPermissions(Permissions.VIEW_CUSTOMER);
        checkPermissions(Permissions.VIEW_ACCOUNT);
        
        setCustomerQuery(new CustomerQuery());
        getCustomerQuery().setNationalIdentityNumber(getCustomer().getNationalIdentityNumber());
        getCustomerQuery().setResultLimit(50);
        
        if (getCustomerQuery() == null) {
            return showNinAccounts();
        }

        getCustomerQuery().setProductInstanceResultLimit(BaseUtils.getIntProperty("env.sep.pagesize", 200));

        if (getCustomerQuery().getProductInstanceOffset() == null) {
            getCustomerQuery().setProductInstanceOffset(0);
        }

        try {
                getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
                CustomerList custList = SCAWrapper.getUserSpecificInstance().getCustomers(getCustomerQuery());
                
                if(custList.getCustomers().isEmpty()) {
                    localiseErrorAndAddToGlobalErrors("no.records.found");
                    return showNinAccounts();
                }                
                int numberOfAccounts=0;
                long custAccount=0;
                for(Customer cust: custList.getCustomers()) {                    
                    for(ProductInstance pi: cust.getProductInstances()) {
                        for(ProductServiceInstanceMapping sim: pi.getProductServiceInstanceMappings()) {
                            if(!sim.getServiceInstance().getStatus().equals("DE")) {
                                
                                if(custAccount != sim.getServiceInstance().getAccountId()) {
                                    NinAccountData ninAccount = new NinAccountData();
                                    ninAccount.setAccountId(sim.getServiceInstance().getAccountId());
                                    ninAccount.setCustomerId(sim.getServiceInstance().getCustomerId());
                                    ninAccount.setStatus(sim.getServiceInstance().getStatus());
                                    ninAccount.setNin(cust.getNationalIdentityNumber());
                                    ninAccount.setAccountOrg(pi.getOrganisationId());
                                    if(pi.getOrganisationId()>0) {
                                        ninAccount.setAccountType("Business");
                                    } else {
                                        ninAccount.setAccountType("Personal");
                                    }
                                    custAccount=sim.getServiceInstance().getAccountId();
                                    ninAccountData.add(ninAccount);
                                }
                            }
                        }
                    }
                }
                setNinAccountData(ninAccountData);
        } catch (SCABusinessError sbe) {
                localiseErrorAndAddToGlobalErrors("no.records.found");
                return showNinAccounts();
        }
        
    /*    if(BaseUtils.getProperty("env.country.name").trim().equalsIgnoreCase("Tanzania")) {
            HashMap<String,String> iccidStatuses = new HashMap<String,String>();
            
            for(ProductInstance product : getCustomer().getProductInstances()) {
                String iccid = product.getPhysicalId();                
                String simVerificationStatus = getSimVerificationStatus(iccid);  
                
                if(!simVerificationStatus.equalsIgnoreCase("NoStatus")) {
                    iccidStatuses.put(iccid, simVerificationStatus);                
                }
            }
            setSimStatus(iccidStatuses);
        }
        */
        
 /*       if(BaseUtils.getProperty("env.country.name").trim().equalsIgnoreCase("Nigeria") && (getCustomer().getCustomerRoles()!=null && getCustomer().getCustomerRoles().size()>0)) {
            HashMap<String,String> iccidUsers = new HashMap<String,String>();
            
            for(ProductInstance product : getCustomer().getProductInstances()) {
                String iccid = product.getPhysicalId();                
                String simUser = getIccidUser(iccid, getCustomer().getCustomerRoles().get(0).getOrganisationId());  
                
                iccidUsers.put(iccid, simUser);
            }
            setSimUser(iccidUsers);
        }
*/
        return getDDForwardResolution("/customer/customer_view_nin_accounts.jsp");
    }
    

    public Resolution showCompareCustomerRegulatorData() {
        return getDDForwardResolution("/customer/customer_smile_nimc_compare.jsp");
    }

    public Resolution checkIfUsernameExists() {
        checkPermissions(Permissions.VIEW_CUSTOMER);
        boolean customerExists = false;

        clearValidationErrors(); // In case we got here from the customer id screen and no id was captured

        try {
            getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            setCustomerList(SCAWrapper.getUserSpecificInstance().getCustomers(getCustomerQuery()));

            if (getCustomerList().getNumberOfCustomers() > 0) {
                customerExists = true;
            }

        } catch (SCABusinessError e) {
            showSearchCustomer();
            throw e;
        }
        return new StreamingResolution("text", String.valueOf(customerExists));
    }

    @DefaultHandler
    public Resolution showSearchCustomer() {
        HttpSession session = getContext().getRequest().getSession();               
        session.removeAttribute("fromSale");
        session.removeAttribute("sessionSale");
        session.removeAttribute("sessionProduct");
        return getDDForwardResolution("/customer/search_customer.jsp");
    }

    private String imsi;

    public void setIMSI(String imsi) {
        this.imsi = imsi;
    }

    public java.lang.String getIMSI() {
        return imsi;
    }

    @DontValidate()
    public Resolution searchCustomer() {
        checkPermissions(Permissions.VIEW_CUSTOMER);

        clearValidationErrors(); // In case we got here from the customer id screen and no id was captured
        try {
            getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            if (getServiceInstanceQuery() != null) {
                getCustomerQuery().setAlternativeContact(getServiceInstanceQuery().getIdentifier()); // Search for phone number in alternative contacts as well as for a Smile number
            }
            setCustomerList(SCAWrapper.getUserSpecificInstance().getCustomers(getCustomerQuery()));
            if (getServiceInstanceQuery() != null && getCustomerList().getNumberOfCustomers() < getCustomerQuery().getResultLimit()
                    && !getServiceInstanceQuery().getIdentifier().isEmpty()) {
                // Search by Phone number
                getServiceInstanceQuery().setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                getServiceInstanceQuery().setIdentifierType("END_USER_SIP_URI");
                getServiceInstanceQuery().setIdentifier(Utils.getPublicIdentityForPhoneNumber(getServiceInstanceQuery().getIdentifier()));
                setServiceInstanceList(SCAWrapper.getUserSpecificInstance().getServiceInstances(getServiceInstanceQuery()));
                for (ServiceInstance si : getServiceInstanceList().getServiceInstances()) {
                    if (si.getCustomerId() > 0 && !isCustomerIdInList(si.getCustomerId(), getCustomerList().getCustomers())) {
                        getCustomerList().getCustomers().add(SCAWrapper.getUserSpecificInstance().getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
                    }
                }
            }
            if (getCustomerList().getNumberOfCustomers() < getCustomerQuery().getResultLimit()
                    && imsi != null && !imsi.isEmpty()) {
                // Search by IMSI
                ServiceInstanceQuery q = new ServiceInstanceQuery();
                q.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                q.setIdentifierType("END_USER_PRIVATE");
                q.setIdentifier(Utils.makePrivateIdentityFromIMSI(imsi));
                setServiceInstanceList(SCAWrapper.getUserSpecificInstance().getServiceInstances(q));
                for (ServiceInstance si : getServiceInstanceList().getServiceInstances()) {
                    if (si.getCustomerId() > 0 && !isCustomerIdInList(si.getCustomerId(), getCustomerList().getCustomers())) {
                        getCustomerList().getCustomers().add(SCAWrapper.getUserSpecificInstance().getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
                    }
                }
            }

            getCustomerList().setNumberOfCustomers(getCustomerList().getCustomers().size());

            if (getCustomerList().getNumberOfCustomers() < getCustomerQuery().getResultLimit()
                    && !getIMSSubscriptionQuery().getIntegratedCircuitCardIdentifier().isEmpty()) {
                // Search by ICCID by getting the SIM public and private ID's and finding all mapped SI's
                try {
                    getIMSSubscriptionQuery().setVerbosity(StIMSSubscriptionLookupVerbosity.IMSU_IMPI_IMPU);
                    IMSSubscription sub = SCAWrapper.getUserSpecificInstance().getIMSSubscription(getIMSSubscriptionQuery());

                    ServiceInstanceQuery siq = new ServiceInstanceQuery();
                    siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);

                    for (IMSPrivateIdentity impi : sub.getIMSPrivateIdentities()) {
                        siq.setIdentifier(impi.getIdentity());
                        siq.setIdentifierType("END_USER_PRIVATE");
                        ServiceInstanceList siList = SCAWrapper.getUserSpecificInstance().getServiceInstances(siq);
                        for (ServiceInstance si : siList.getServiceInstances()) {
                            if (si.getCustomerId() > 0 && !isCustomerIdInList(si.getCustomerId(), getCustomerList().getCustomers())) {
                                getCustomerList().getCustomers().add(SCAWrapper.getUserSpecificInstance().getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
                            }
                        }
                        for (ImplicitIMSPublicIdentitySet set : impi.getImplicitIMSPublicIdentitySets()) {
                            for (IMSNestedIdentityAssociation association : set.getAssociatedIMSPublicIdentities()) {
                                if (association.getIMSPublicIdentity().getIdentity() != null && !association.getIMSPublicIdentity().getIdentity().isEmpty()) {
                                    siq.setIdentifier(association.getIMSPublicIdentity().getIdentity());
                                    siq.setIdentifierType("END_USER_SIP_URI");
                                    siList = SCAWrapper.getUserSpecificInstance().getServiceInstances(siq);
                                    for (ServiceInstance si : siList.getServiceInstances()) {
                                        if (si.getCustomerId() > 0 && !isCustomerIdInList(si.getCustomerId(), getCustomerList().getCustomers())) {
                                            getCustomerList().getCustomers().add(SCAWrapper.getUserSpecificInstance().getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
                                        }
                                    }
                                }
                            }
                        }
                    }

                } catch (SCABusinessError be) {
                    log.debug("No Subscription data found for ICCID [{}]", getIMSSubscriptionQuery().getIntegratedCircuitCardIdentifier());
                }
            }

            getCustomerList().setNumberOfCustomers(getCustomerList().getCustomers().size());
            if (getCustomerList().getNumberOfCustomers() <= 0) {
                localiseErrorAndAddToGlobalErrors("no.records.found");
            }
        } catch (SCABusinessError e) {
            showSearchCustomer();
            throw e;
        }
        return getDDForwardResolution("/customer/search_customer.jsp");
    }

    private boolean isCustomerIdInList(int id, List<Customer> customerList) {
        for (Customer c : customerList) {
            if (c.getCustomerId() == id) {
                return true;
            }
        }
        return false;
    }
    private Map<Integer, String> AccountManagerCustomerList;

    public void setAccountManagerCustomerList(Map<Integer, String> AccountManagerCustomerList) {
        this.AccountManagerCustomerList = AccountManagerCustomerList;
    }

    public Map<Integer, String> getAccountManagerCustomerList() {
        return AccountManagerCustomerList;
    }

    public Resolution showTrackSession() {
        checkPermissions(Permissions.TRACK_CUSTOMER);
        String ssoid = getCustomer().getSSOIdentity().toLowerCase();
        CacheHelper.putInRemoteCache("RequestTrack_" + ssoid, getUser(), 120);
        CacheHelper.removeFromRemoteCache("Track_" + ssoid);
        return getDDForwardResolution("/customer/track_customer.jsp?trackSession=true&ssoid=" + ssoid);
    }

    public Resolution allowTracking() {
        enableTracking();
        setPageMessage("tracking.enabled");
        return getDDForwardResolution("/index.jsp");
    }

    public Resolution showChangePassword() {
        return getDDForwardResolution("/customer/change_loggedin_password.jsp");
    }

    public Resolution changePassword() {
        checkCSRF();
        if (!getParameter("password1").equals(getParameter("password2"))) {
            localiseErrorAndAddToGlobalErrors("passwords.dont.match");
            return showChangePassword();
        }
        String newPasswd = getParameter("password1");
        String origPasswd = getParameter("currpassword");

        AuthenticationQuery q = new AuthenticationQuery();
        q.setSSOIdentity(getUser());
        q.setSSOEncryptedPassword(Codec.stringToEncryptedHexString(origPasswd));

        boolean correctPassword = false;
        try {
            AuthenticationResult authResult = SCAWrapper.getAdminInstance().authenticate(q);
            correctPassword = authResult.getDone().equals(StDone.TRUE);
        } catch (Exception e) {
        }
        if (!correctPassword) {
            localiseErrorAndAddToGlobalErrors("invalid.login.credentials");
            return showChangePassword();
        }

        CustomerQuery customerQuery = new CustomerQuery();
        customerQuery.setCustomerId(getUserCustomerIdFromSession());
        customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        Customer cust = SCAWrapper.getAdminInstance().getCustomer(customerQuery);
        try {
            cust.setSSODigest(Utils.hashPasswordWithComplexityCheck(newPasswd));
        } catch (Exception ex) {
            localiseErrorAndAddToGlobalErrors("password.too.simple");
            return showChangePassword();
        }
        log.debug("Allowing password reset");
        SCAWrapper.getAdminInstance().modifyCustomer(cust);
        setPageMessage("password.reset");
        return getDDForwardResolution("/index.jsp");
    }

    public Resolution showChangeAccountManagerCustomer() {
        checkPermissions(Permissions.EDIT_CUSTOMER_ACCOUNT_MANAGER);
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));

        Customer loggedInCustomer = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER);
        setAccountManagerCustomerList(new HashMap<Integer, String>());

        for (CustomerRole role : loggedInCustomer.getCustomerRoles()) {
            Organisation org = UserSpecificCachedDataHelper.getOrganisation(role.getOrganisationId(), StOrganisationLookupVerbosity.MAIN_ROLES);
            for (CustomerRole roleInOrg : org.getCustomerRoles()) {
                getAccountManagerCustomerList().put(roleInOrg.getCustomerId(), roleInOrg.getCustomerName());
            }
        }

        return getDDForwardResolution("/customer/change_account_manager.jsp");
    }

    public Resolution changeAccountManagerCustomer() {
        checkPermissions(Permissions.EDIT_CUSTOMER_ACCOUNT_MANAGER);
        CustomerQuery q = new CustomerQuery();

        q.setCustomerId(getCustomer().getCustomerId());
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);

        Customer tmpCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(q);

        tmpCustomer.setAccountManagerCustomerProfileId(getCustomer().getAccountManagerCustomerProfileId());
        tmpCustomer.setVersion(getCustomer().getVersion());

        SCAWrapper.getUserSpecificInstance().modifyCustomer(tmpCustomer);

        return retrieveCustomer();
    }

    private List<Photograph> setCustomerPhotographsData(List<Photograph> customerPhotoGraphs) {
        for (Photograph p : customerPhotoGraphs) {
            if (p != null) {
                try {
                    p.setData(Utils.encodeBase64(Utils.getDataFromTempFile(p.getPhotoGuid())));
                } catch (Exception ex) {
                    log.warn("Error", ex);
                }
            }
        }
        return customerPhotoGraphs;
    }

    private void writeCustomerPhotographsDataToFile(List<Photograph> customerPhotoGraphs) {

        for (Photograph p : customerPhotoGraphs) {
            String fileName = p.getPhotoGuid();
            try {
                Utils.createTempFile(fileName, Utils.decodeBase64(p.getData()));
            } catch (Exception e) {
                log.warn("Error in writeCustomerPhotographsDataToFile", e);
            }
        }
    }

    private boolean comingFromMakeSale = false;

    public boolean isComingFromMakeSale() {
        return comingFromMakeSale;
    }

    /**
     * This fuction will verify the refugee with UCC and create the customer if
     * verification is successful.
     *
     * @return
     */
    public Resolution verifyCustomerWithUCC() {
        checkPermissions(Permissions.ADD_CUSTOMER);
        checkCSRF();
        log.debug("In verifyCustomerWithUCC");

        boolean verified = verifyRefugeeWithUCC();

        if (verified) {
            setPageMessage("customer.created.confirmation");
            getCustomer().setKYCStatus("V");
        } else {
            localiseErrorAndAddToGlobalErrors("invalid.refugee.credentials");
            return getDDForwardResolution("/customer/add_customer_capture_basic_details.jsp");
        }
        SCAWrapper.getUserSpecificInstance().addCustomer(getCustomer());
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
        log.debug("Finished verifyCustomerWithUCC");
        return showNextStepAfterAddingCustomer();
    }

    private boolean verifyRefugeeWithUCC() {
        ValidateRefugeeRequest req = new ValidateRefugeeRequest();
        req.setIndividualId(getCustomer().getIdentityNumber());
        req.setSex(getSexForUCC(getCustomer().getGender()));
        req.setYearOfBirth(getYearOfBirth(getCustomer().getDateOfBirth()));

        byte[] fingerPrintData = null;
        String fingerprintBase64 = null;
        try {
            for (Photograph p : getCustomer().getCustomerPhotographs()) {
                if (p.getPhotoType().startsWith("fingerprint")) {
                    fingerPrintData = Utils.getDataFromTempFile(p.getPhotoGuid());
                    fingerprintBase64 = new String(Base64.encodeBase64(fingerPrintData), "UTF-8");
                    break;
                }
            }
        } catch (Exception ex) {
            log.error("error while fingerprint conversion");
        }
        if (fingerPrintData == null) {
            return false;
        }
        req.setFingerprint(fingerprintBase64);
        return UCCHelper.validateRefugee(req);
    }

    public Resolution addCustomerWizard() {
        checkPermissions(Permissions.ADD_CUSTOMER);
        checkCSRF();
        try {
            log.debug("In addCustomerWizard");
            getCustomer().getSecurityGroups().clear(); // Dont allow hacker to override
            getCustomer().getSecurityGroups().add("Customer");
            getCustomer().setDateOfBirth(getCustomer().getDateOfBirth().replace("/", ""));
            getCustomer().setPassportExpiryDate(getCustomer().getPassportExpiryDate().replace("/", ""));
            getCustomer().setVisaExpiryDate(getCustomer().getVisaExpiryDate().replace("/", ""));
            // Keep the photos before we submit the request to create a customer profile ...
            setCustomerPhotographsData(getCustomer().getCustomerPhotographs());
            log.debug("Calling addCustomer on SCA");
            if (BaseUtils.getBooleanProperty("env.customer.verify.with.nimc", false)) {
                getCustomer().setNationalIdentityNumber(getCustomer().getIdentityNumber());
            }
            getCustomer().setCustomerId(SCAWrapper.getUserSpecificInstance().addCustomer(getCustomer()).getInteger());

            log.debug("Called addCustomer on SCA");

            // Nira stuff
            if (BaseUtils.getBooleanProperty("env.customer.verify.with.nira", false)
                    && getCustomer().getNationality().equalsIgnoreCase(BaseUtils.getProperty("env.locale.country.for.language.en"))
                    && isAllowed(BaseUtils.getProperty("env.sep.roles.allowed.to.verify.customers.with.nira", ""))) {

                // Check if customer was verified in case.
                Customer newCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER);

                if (newCustomer.getKYCStatus() != null && newCustomer.getKYCStatus().equals("V")) {
                    setPageMessage("customer.created.and.nira.verified");
                } else {
                    localiseErrorAndAddToGlobalErrors("customer.not.nira.verified");
                }

            } else {
                setPageMessage("customer.created.confirmation");
            }

            //Attach a sticky note to this customer if converted from sales lead
            String issueId = (getTTIssue().getID() == null ? "" : getTTIssue().getID());
            if (!issueId.isEmpty()) {
                try {
                    NewStickyNote nsn = new NewStickyNote();
                    nsn.setEntityType("Customer");
                    nsn.setCreatedBy(getUser());
                    nsn.setEntityId(getCustomer().getCustomerId());
                    nsn.setTypeName("SalesLeadCustomer");

                    NewStickyNoteField nsnf = new NewStickyNoteField();
                    nsnf.setFieldData(getTTIssue().getID());
                    nsnf.setFieldName("SalesLeadIssueId");
                    nsnf.setFieldType("L");
                    nsnf.setDocumentData("");
                    nsn.getFields().add(nsnf);

                    SCAWrapper.getUserSpecificInstance().addStickyNote(nsn);
                    setPageMessage("customer.created.confirmation.stickynote.attached");
                } catch (Exception ex) {
                    setPageMessage("customer.created.confirmation.nostickynote.attached");
                }
            }

        } catch (SCABusinessError e) {
            log.debug("Got SCABusinessError in AddCustomer", e);
            throw e;
        }

        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
        if (!getCustomer().getClassification().equals("customer")) {
            setPageMessage("pending.customer.created.confirmation");
            return retrieveCustomer();
        }
        log.debug("Finished addCustomerWizard");
        return showNextStepAfterAddingCustomer();
    }

    public Resolution showNextStepAfterAddingCustomer() {
        return getDDForwardResolution("/customer/next_steps_after_adding.jsp");
    }

    public Resolution addOrganisationWizard() {
        checkPermissions(Permissions.ADD_ORGANISATION);
        checkCSRF();
        setCustomerPhotographsData(getOrganisation().getOrganisationPhotographs());
        getOrganisation().setOrganisationId(SCAWrapper.getUserSpecificInstance().addOrganisation(getOrganisation()).getInteger());
        setPageMessage("organisation.created.confirmation");
        return retrieveOrganisation();
    }

    public Resolution manageAddresses() {
        if (getCustomerQuery() != null) {
            checkPermissions(Permissions.EDIT_CUSTOMER_ADDRESS);
            getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
        }
        if (getOrganisationQuery() != null) {
            checkPermissions(Permissions.EDIT_ORGANISATION_ADDRESS);
            getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN);
            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
        }
        return getDDForwardResolution("/address/manage_addresses.jsp");
    }

    public Resolution manageContracts() {

        clearValidationErrors();

        if (getCustomerQuery() != null) {
            setContractQuery(new ContractQuery());
            getContractQuery().setCustomerId(getCustomer().getCustomerId());
            setContractList(SCAWrapper.getUserSpecificInstance().getContracts(getContractQuery()));
            setCustomer(UserSpecificCachedDataHelper.getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
        }

        if (getOrganisationQuery() != null) {
            setContractQuery(new ContractQuery());
            getContractQuery().setOrganisationId(getOrganisation().getOrganisationId());
            setContractList(SCAWrapper.getUserSpecificInstance().getContracts(getContractQuery()));
            setOrganisation(UserSpecificCachedDataHelper.getOrganisation(getOrganisation().getOrganisationId(), StOrganisationLookupVerbosity.MAIN));
        }

        if (getContractList() == null || getContractList().getNumberOfContracts() <= 0) {
            setPageMessage("no.contract.exists");
        }

        return getDDForwardResolution("/contract/manage_contracts.jsp");
    }

    public Resolution showAddContract() {
        checkEditContractPermissions();

        if (getOrganisationQuery() != null) {
            getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN_ROLES);
            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
        }

        return getDDForwardResolution("/contract/add_contract.jsp");
    }

    public Resolution showEditContract() {
        checkEditContractPermissions();

        setContractQuery(new ContractQuery());
        getContractQuery().setContractId(getContract().getContractId());
        setContract(SCAWrapper.getUserSpecificInstance().getContracts(getContractQuery()).getContracts().get(0));
        getPhotographs().addAll(getContract().getContractDocuments());

        fulfilmentItemsAllowed = new ArrayList();
        getFulfilmentItemsAllowed().addAll(Utils.getListFromCRDelimitedString(getContract().getFulfilmentItemsAllowed()));

        staffMembersAllowed = new ArrayList();
        getStaffMembersAllowed().addAll(Utils.getListFromCRDelimitedString(getContract().getStaffMembersAllowed()));

        if (getContract().getOrganisationId() != null) {
            setOrganisationQuery(new OrganisationQuery());
            getOrganisationQuery().setOrganisationId(getContract().getOrganisationId());
            getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN_ROLES);
            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
        }
        writeCustomerPhotographsDataToFile(getPhotographs());

        return getDDForwardResolution("/contract/edit_contract.jsp");
    }

    @DontValidate()
    public Resolution editContract() {
        checkEditContractPermissions();
        clearValidationErrors();

        try {
            if (!doContractValidationForEdit(getContract())) {
                getContract().getContractDocuments().clear();
                getContract().getContractDocuments().addAll(setCustomerPhotographsData(getPhotographs()));

                if (log.isDebugEnabled()) {
                    log.debug("Number of photographs is [{}]", getContract().getContractDocuments().size());
                }

                boolean errorEncountered = false;

                for (Photograph photo : getContract().getContractDocuments()) {

                    if (photo.getData() == null || photo.getPhotoGuid() == null) { // A photo with no data was encountered. Raise exception ...
                        localiseErrorAndAddToGlobalErrors("no.file.selected");
                        errorEncountered = true;
                    }

                    if (photo.getPhotoType() == null || photo.getPhotoType().trim().length() <= 0) { // A photo with no type selected was encountered. Raise error ...
                        localiseErrorAndAddToGlobalErrors("no.file.type.selected");
                        errorEncountered = true;
                    }

                    if (errorEncountered) {
                        if (getOrganisationQuery() != null) {
                            getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN_ROLES);
                            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
                        }
                        return getDDForwardResolution("/contract/edit_contract.jsp");
                    }

                    log.debug("Photo GUID [{}] Photo Type [{}] Data length [{}]", new Object[]{photo.getPhotoGuid(), photo.getPhotoType(), photo.getData().length()});
                }

                if (getFulfilmentItemsAllowed() != null) {
                    getContract().setFulfilmentItemsAllowed(Utils.makeCRDelimitedStringFromList(getFulfilmentItemsAllowed()));
                } else {
                    getContract().setFulfilmentItemsAllowed(""); // Nothing selected so make it blank.
                }

                if (getStaffMembersAllowed() != null) {
                    getContract().setStaffMembersAllowed(Utils.makeCRDelimitedStringFromList(getStaffMembersAllowed()));
                } else {
                    getContract().setStaffMembersAllowed(""); // Nothing selected so make it blank.
                }

                Done done = SCAWrapper.getUserSpecificInstance().modifyContract(getContract());
                if (done.getDone().equals(StDone.TRUE)) {
                    setPageMessage("contract.edit.confirmed");
                } else {
                    setPageMessage("contract.edit.failed");
                }
            } else {
                if (getOrganisationQuery() != null) {
                    getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN_ROLES);
                    setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
                }
                return getDDForwardResolution("/contract/edit_contract.jsp");
            }
        } catch (SCABusinessError e) {
            log.error("SCA Error while adding contract - ", e.getMessage());
            throw e;
        }

        return manageContracts();
    }

    @DontValidate()
    public Resolution addContract() {
        checkCSRF();
        checkEditContractPermissions();

        clearValidationErrors();

        try {

            if (!doContractValidation(getContract())) {

                getContract().getContractDocuments().clear();
                getContract().getContractDocuments().addAll(setCustomerPhotographsData(getPhotographs()));
                getPhotographs().addAll(getContract().getContractDocuments());

                writeCustomerPhotographsDataToFile(getPhotographs());

                if (log.isDebugEnabled()) {
                    log.debug("Number of photographs is [{}]", getContract().getContractDocuments().size());
                }

                if (getFulfilmentItemsAllowed() != null) {
                    getContract().setFulfilmentItemsAllowed(Utils.makeCRDelimitedStringFromList(getFulfilmentItemsAllowed()));
                } else {
                    getContract().setFulfilmentItemsAllowed("");
                }

                if (getStaffMembersAllowed() != null) {
                    getContract().setStaffMembersAllowed(Utils.makeCRDelimitedStringFromList(getStaffMembersAllowed()));
                } else {
                    getContract().setStaffMembersAllowed("");
                }
                getContract().setCreatedByCustomerId(getUserCustomerIdFromSession());
                SCAInteger contractId = SCAWrapper.getUserSpecificInstance().addContract(getContract());
                if (contractId != null) {
                    setPageMessage("contract.added.confirmed", contractId.getInteger());
                }
            } else {
                if (getOrganisationQuery() != null) {
                    getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN_ROLES);
                    setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
                }
                return getDDForwardResolution("/contract/add_contract.jsp");
            }

        } catch (SCABusinessError e) {
            log.error("SCA Error while adding contract - ", e.getMessage());
            throw e;
        }

        return manageContracts();
    }

    private boolean doContractValidation(Contract cr) {

        boolean isError = false;

        // Check if end date is before or after
        if (cr.getContractStartDateTime().compare(cr.getContractEndDateTime()) >= 0) {
            localiseErrorAndAddToGlobalErrors("contract.end.date.after.start.date");
            isError = true;
        }

        return isError;
    }

    private boolean doContractValidationForEdit(Contract cr) {

        boolean isError = false;

        // Check if end date is before or after
        if (cr.getContractStartDateTime().compare(cr.getContractEndDateTime()) >= 0) {
            localiseErrorAndAddToGlobalErrors("contract.end.date.after.start.date");
            isError = true;
        }

        return isError;
    }

    public Resolution deleteContract() {
        checkCSRF();
        checkEditContractPermissions();

        if (notConfirmed()) {
            return confirm();
        }
        SCAInteger contractId = new SCAInteger();
        contractId.setInteger(getContract().getContractId());

        SCAWrapper.getUserSpecificInstance().deleteContract(contractId);
        if (getCustomer() != null || getOrganisation() != null) {
            setPageMessage("contract.deleted.confirmation");
        }
        return manageContracts();
    }

    public Resolution viewContract() {

        ContractQuery q = new ContractQuery();
        q.setContractId(getContract().getContractId());
        setContract(SCAWrapper.getUserSpecificInstance().getContracts(q).getContracts().get(0));
        fulfilmentItemsAllowed = new ArrayList();
        fulfilmentItemsAllowed.addAll(Utils.getListFromCRDelimitedString(getContract().getFulfilmentItemsAllowed()));

        staffMembersAllowed = new ArrayList();
        staffMembersAllowed.addAll(Utils.getListFromCRDelimitedString(getContract().getStaffMembersAllowed()));

        if (getContract().getOrganisationId() != null) {
            setOrganisationQuery(new OrganisationQuery());
            getOrganisationQuery().setOrganisationId(getContract().getOrganisationId());
            getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN_ROLES);
            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
        }
        writeCustomerPhotographsDataToFile(getContract().getContractDocuments()); //So they can be diplayed on the view
        return getDDForwardResolution("/contract/view_contract.jsp");
    }

    public void checkEditContractPermissions() {
        if (getCustomerQuery() != null) {
            checkPermissions(Permissions.EDIT_CUSTOMER_CONTRACT);
        }
        if (getOrganisationQuery() != null) {
            checkPermissions(Permissions.EDIT_ORGANISATION_CONTRACT);
        }
    }

    public Resolution deleteAddress() {
        checkCSRF();
        checkAddressPermissions();

        if (notConfirmed()) {
            return confirm();
        }
        SCAWrapper.getUserSpecificInstance().deleteAddress(getAddress());
        if (getCustomer() != null || getOrganisation() != null) {
            setPageMessage("address.deleted.confirmation");
        }

        return manageAddresses();
    }

    public Resolution addAddress() {
        checkCSRF();
        checkAddressPermissions();
        try {
            SCAWrapper.getUserSpecificInstance().addAddress(getAddress());

            if (getCustomer() != null || getOrganisation() != null) {
                setPageMessage("address.added.confirmed");
            }
        } catch (SCABusinessError e) {
            localiseErrorAndAddToGlobalErrors("duplicate.address.types");
            manageAddresses();
        }
        return manageAddresses();
    }

    private List<ServiceInstance> customersServiceInstances;

    public List<ServiceInstance> getCustomersServiceInstances() {
        return customersServiceInstances;
    }

    private List<ServiceInstance> organisationServiceInstances;

    public List<ServiceInstance> getOrganisationServiceInstances() {
        return organisationServiceInstances;
    }

    public Resolution showCreateOrganisationPortInRequest() {
        checkPermissions(Permissions.CREATE_PORTING_ORDER);

        getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN_ROLES);
        getOrganisationQuery().setRolesResultLimit(1000);
        getOrganisationQuery().setRolesOffset(0);

        try {// getOrganisation().getO
            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
        } catch (SCABusinessError sbe) {
            localiseErrorAndAddToGlobalErrors("no.records.found");
            return showSearchOrganisation();
        }

        if (getProductInstanceQuery() == null) {
            setProductInstanceQuery(new ProductInstanceQuery());
        }
        getProductInstanceQuery().setResultLimit(BaseUtils.getIntProperty("env.sep.pagesize", 50));

        if (getProductInstanceQuery().getOffset() == null) {
            getProductInstanceQuery().setOffset(0);
        }
        getProductInstanceQuery().setOrganisationId(getOrganisation().getOrganisationId());
        getProductInstanceQuery().setVerbosity(StProductInstanceLookupVerbosity.MAIN_SVC);
        setProductInstanceList(SCAWrapper.getUserSpecificInstance().getProductInstances(getProductInstanceQuery()));

        organisationServiceInstances = new ArrayList<>();

        if (getPortInEvent() == null) {
            setPortInEvent(new PortInEvent());
        }
        //Set the PortRequestFormId to a UUID without dashes and limit to 32 characters long.
        if (BaseUtils.getBooleanProperty("env.mnp.generate.port.request.form.id", false)) {
            if (getPortInEvent().getPortRequestFormId() == null) {
                getPortInEvent().setPortRequestFormId(Utils.getUUID().replace("-", "").substring(0, 32).toUpperCase());
            }
        } else {
            getPortInEvent().setPortRequestFormId(null);
        }

        for (ProductInstance pi : getProductInstanceList().getProductInstances()) {
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = m.getServiceInstance();
                organisationServiceInstances.add(si);
            }
        }
        /* if(getPortInEvent() == null) {
            setPortInEvent(new PortInEvent());
            getPortInEvent().setPortingDate(Utils.getDateAsXMLGregorianCalendar(new Date()));
        }*/
        return getDDForwardResolution("/organisation/create_port_in_request.jsp");
    }

    public Resolution showCreateCustomerPortInRequest() {
        checkPermissions(Permissions.CREATE_PORTING_ORDER);

        if (getOrganisation() != null) {
            setOrganisationQuery(new OrganisationQuery());
            getOrganisationQuery().setOrganisationId(getOrganisation().getOrganisationId());
            return showCreateOrganisationPortInRequest();
        }

        customersServiceInstances = new ArrayList<>();
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES));
        getCustomer().setDateOfBirth(Utils.addSlashesToDate(getCustomer().getDateOfBirth()));
        // All MNP communications uses the friendly phone number - i.e. local phone number prefixed with the direct dial code (env.e164.direct.dial.code)
        getCustomer().setAlternativeContact1(Utils.getFriendlyPhoneNumber(getCustomer().getAlternativeContact1()));

        if (getPortInEvent() == null) {
            setPortInEvent(new PortInEvent());
        }
        //Set the PortRequestFormId to a UUID without dashes and limit to 32 characters long.
        if (BaseUtils.getBooleanProperty("env.mnp.generate.port.request.form.id", false)) {
            if (getPortInEvent().getPortRequestFormId() == null) {
                getPortInEvent().setPortRequestFormId(Utils.getUUID().replace("-", "").substring(0, 32).toUpperCase());
            }
        } else {
            getPortInEvent().setPortRequestFormId(null);
        }

        // To ensure we only show the service instances that belong to this customer and not the ones that belong to an organisation.    
        for (ProductInstance pi : getCustomer().getProductInstances()) {
            //Check if this is as standalone product?
            if (pi.getOrganisationId() == 0) {
                for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                    ServiceInstance si = m.getServiceInstance();
                    if (si.getCustomerId() == getCustomer().getCustomerId()) {
                        customersServiceInstances.add(si);
                    }
                }
            }
        }
        /* if(getPortInEvent() == null) {
            setPortInEvent(new PortInEvent());
            getPortInEvent().setPortingDate(Utils.getDateAsXMLGregorianCalendar(new Date()));
        }*/
        return getDDForwardResolution("/customer/create_port_in_request.jsp");
    }

    public Resolution showCreateRingFenceRequest() {
        checkPermissions(Permissions.DO_RING_FENCE);

        customersServiceInstances = new ArrayList<>();
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES));
        getCustomer().setDateOfBirth(Utils.addSlashesToDate(getCustomer().getDateOfBirth()));
        // All MNP communications uses the friendly phone number - i.e. local phone number prefixed with the direct dial code (env.e164.direct.dial.code)
        getCustomer().setAlternativeContact1(Utils.getFriendlyPhoneNumber(getCustomer().getAlternativeContact1()));

        return getDDForwardResolution("/customer/create_ring_fence_request.jsp");
    }

    public Resolution createPortInRequest() {
        checkPermissions(Permissions.CREATE_PORTING_ORDER);
        try {
            RoutingInfoList curRoutingInfoList = new RoutingInfoList();
            if (getPortInEvent().getRoutingInfoList() == null) {
                localiseErrorAndAddToGlobalErrors("porting.order.routinginfolist.not.supplied");
                return showCreateCustomerPortInRequest();
            } else {

                log.debug("Number of phone numbers to be ring fenced is: {}", getPortInEvent().getRingFenceNumberList().size());
                // Set the ring fencing indicator.
                if (getPortInEvent().getRingFenceNumberList() != null && getPortInEvent().getRingFenceNumberList().size() > 0) {
                    getPortInEvent().setRingFenceIndicator("Y");
                } else {
                    getPortInEvent().setRingFenceIndicator("N"); // No Ring-fencing required.
                }

                // Validation for Photographs.
                if (getPortInEvent().getRingFenceIndicator().equals("Y")) { // Must make sure the document/s are attached.
                    if (getPhotographs().isEmpty()) {
                        // No photos supplied.
                        localiseErrorAndAddToGlobalErrors("port.request.forms.not.supplied");
                        return showCreateCustomerPortInRequest();
                    }
                } else if (BaseUtils.getBooleanProperty("env.mnp.port.request.form.mandatory", true)
                        && !BaseUtils.getBooleanProperty("env.mnp.include.ringfencing.on.new.portorder", false)) {
                    // Port Request Forms are Mandatory
                    if (getPhotographs().isEmpty()) {
                        // No photos supplied.
                        localiseErrorAndAddToGlobalErrors("port.request.forms.not.supplied");
                        return showCreateCustomerPortInRequest();
                    }
                }

                getPortInEvent().getPortRequestForms().clear();
                getPortInEvent().getPortRequestForms().addAll(setCustomerPhotographsData(getPhotographs()));
                // getPhotographs().addAll(getPortInEvent().getPortRequestForms());
                writeCustomerPhotographsDataToFile(getPhotographs());
                log.debug("Port request has [{}] documents to attach.", getPortInEvent().getPortRequestForms().size());
                // Set the ranges for single number portations.
                for (RoutingInfo routingInfo : getPortInEvent().getRoutingInfoList().getRoutingInfo()) {
                    if (routingInfo.isSelectedForPortIn()) {
                        if (routingInfo.getPhoneNumberRange().getPhoneNumberStart() == null
                                || routingInfo.getPhoneNumberRange().getPhoneNumberStart().trim().isEmpty()) {
                            localiseErrorAndAddToGlobalErrors("porting.order.no.phone.number.not.supplied", String.valueOf(routingInfo.getServiceInstanceId()));
                            return showCreateCustomerPortInRequest();
                        }
                        routingInfo.getPhoneNumberRange().setPhoneNumberEnd(routingInfo.getPhoneNumberRange().getPhoneNumberStart());
                        routingInfo.setRoutingNumber(Utils.getPropertyValueFromList("env.mnp.config", "SmileNetworkRoutingNumber"));
                        curRoutingInfoList.getRoutingInfo().add(routingInfo);
                    }
                }
            }

            //Add the ranges to be ported, if any specified.
            RoutingInfo tRoutingInfo = null;
            if (getNumberRangesToPort() != null) {
                for (PhoneNumberRange phoneRange : getNumberRangesToPort()) {
                    if (phoneRange.getPhoneNumberEnd().equalsIgnoreCase(phoneRange.getPhoneNumberStart())) {
                        localiseErrorAndAddToGlobalErrors("porting.order.invalid.number.range", phoneRange.getPhoneNumberStart() + " - " + phoneRange.getPhoneNumberEnd());
                        return showCreateCustomerPortInRequest();
                    }

                    tRoutingInfo = new RoutingInfo();
                    tRoutingInfo.setServiceInstanceId(-1); // To indicate that this is a range and not an individual number.
                    tRoutingInfo.setPhoneNumberRange(phoneRange);
                    tRoutingInfo.setRoutingNumber(Utils.getPropertyValueFromList("env.mnp.config", "SmileNetworkRoutingNumber"));
                    curRoutingInfoList.getRoutingInfo().add(tRoutingInfo);
                }
            }

            /* Do some validation here ...*/
            if (curRoutingInfoList.getRoutingInfo().size() < 1) {
                localiseErrorAndAddToGlobalErrors("porting.order.no.service.selected");
                return showCreateCustomerPortInRequest();
            }

            // Retrieve the customer associated with this porting
            if (getPortInEvent().getCustomerType().equalsIgnoreCase("individual")) {
                CustomerQuery q = new CustomerQuery();
                q.setCustomerId(getPortInEvent().getCustomerProfileId());
                q.setResultLimit(1);
                q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);

                setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(q));

                getPortInEvent().setCustomerFirstName(getCustomer().getFirstName());
                getPortInEvent().setCustomerLastName(getCustomer().getLastName());
                getPortInEvent().setRoutingInfoList(curRoutingInfoList);
                getPortInEvent().setGender(getCustomer().getGender());
                getPortInEvent().setPortingDirection("IN");

            } else if (getPortInEvent().getCustomerType().equalsIgnoreCase("corporate")) {
                getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN_ROLES);
                getOrganisationQuery().setRolesResultLimit(1000);
                getOrganisationQuery().setRolesOffset(0);

                setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
                getPortInEvent().setOrganisationId(getOrganisation().getOrganisationId());
                getPortInEvent().setOrganisationName(getOrganisation().getOrganisationName());
                getPortInEvent().setOrganisationNumber(getOrganisation().getCompanyNumber());
                getPortInEvent().setOrganisationTaxNumber(getOrganisation().getTaxNumber());
                getPortInEvent().setPortingDirection("IN");

            } else {

            }

            getPortInEvent().setRecipientId(Utils.getPropertyValueFromList("env.mnp.config", "SmileMNPParticipatingId"));

            setPortInEvent(SCAWrapper.getUserSpecificInstance().handlePortInEvent(getPortInEvent()));

            if (getPortInEvent().getProcessingStatus().equals(MNP_REQUEST_PROCESSING_STATUS_IN_PROGRESS)) {
                // Port Order was successfully placed with the clearing house.
                setPageMessage("customer.portin.request.successfull", getPortInEvent().getPortingOrderId());
                // Navigate to the view port screen.

                setPortOrdersQuery(new PortOrdersQuery());
                getPortOrdersQuery().setPortingOrderId(getPortInEvent().getPortingOrderId());
                return searchPortOrder();

            } else { // Portin request failed - show the validation errors.
                for (String validationError : getPortInEvent().getValidationErrors()) {
                    localiseErrorAndAddToGlobalErrors("npc.validation.error", validationError);
                }
                return showCreateCustomerPortInRequest();
            }
        } catch (SCABusinessError e) {
            log.error("Error while trying to create a customer port-in request - [{}] - [{}] - [{}]" + e.getErrorCode(), e.getErrorDesc(), e.getMessage());
            setPageMessage("customer.portin.request.failed");
        }

        // return getDDForwardResolution("/customer/view_customer.jsp");
        return retrieveCustomer();

    }

    public Resolution createRingFenceRequest() {
        checkPermissions(Permissions.DO_RING_FENCE); // Will check the return permissions here ...
        try {

            for (RoutingInfo routingInfo : getPortInEvent().getRoutingInfoList().getRoutingInfo()) {
                routingInfo.setRoutingNumber(Utils.getPropertyValueFromList("env.mnp.config", "SmileNetworkRoutingNumber"));
            }
            // Retrieve the customer associated with this porting
            CustomerQuery q = new CustomerQuery();
            q.setCustomerId(getPortInEvent().getCustomerProfileId());
            q.setResultLimit(1);
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(q));

            getPortInEvent().setCustomerFirstName(getCustomer().getFirstName());
            getPortInEvent().setCustomerLastName(getCustomer().getLastName());
            getPortInEvent().setMessageType(StMNPMessageTypes.SMILE_RING_FENCE_REQUEST);
            getPortInEvent().setGender(getCustomer().getGender());
            getPortInEvent().setPortingDirection("IN");
            setPortInEvent(SCAWrapper.getUserSpecificInstance().handlePortInEvent(getPortInEvent()));

            if (getPortInEvent().getProcessingStatus().equals(MNP_REQUEST_PROCESSING_STATUS_IN_PROGRESS)) {
                // Port Order was successfully placed with the clearing house.
                setPageMessage("ring.fence.request.created", getPortInEvent().getPortingOrderId());
                // Navigate to the view port screen.

                setPortOrdersQuery(new PortOrdersQuery());
                getPortOrdersQuery().setPortingOrderId(getPortInEvent().getPortingOrderId());
                return searchPortOrder();

            } else { // Portin request failed - show the validation errors.

                for (String validationError : getPortInEvent().getValidationErrors()) {
                    localiseErrorAndAddToGlobalErrors("npc.validation.error", validationError);
                }
                //setPortInEvent(currentEvent);
                return showCreateRingFenceRequest();
            }
        } catch (SCABusinessError e) {
            log.error("Error while trying to return ported number to range holder - [{}] - [{}] - [{}]" + e.getErrorCode(), e.getErrorDesc(), e.getMessage());
            throw e;
        }
    }

    public Resolution returnPortedNumberToRangeHolder() {
        checkPermissions(Permissions.DO_NUMBER_RETURN);

        try {
            for (RoutingInfo routingInfo : getPortInEvent().getRoutingInfoList().getRoutingInfo()) {
                routingInfo.setRoutingNumber(Utils.getPropertyValueFromList("env.mnp.config", "SmileNetworkRoutingNumber"));
            }

            // Retrieve the customer associated with this porting
            CustomerQuery q = new CustomerQuery();
            q.setCustomerId(getPortInEvent().getCustomerProfileId());
            q.setResultLimit(1);
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(q));

            getPortInEvent().setCustomerFirstName(getCustomer().getFirstName());
            getPortInEvent().setCustomerLastName(getCustomer().getLastName());
            getPortInEvent().setMessageType(StMNPMessageTypes.SMILE_REQUEST_NUMBER_RETURN);
            getPortInEvent().setGender(getCustomer().getGender());
            getPortInEvent().setPortingDirection("IN");

            setPortInEvent(SCAWrapper.getUserSpecificInstance().handlePortInEvent(getPortInEvent()));

            if (getPortInEvent().getProcessingStatus().equals(MNP_REQUEST_PROCESSING_STATUS_IN_PROGRESS)) {
                // Port Order was successfully placed with the clearing house.
                setPageMessage("port.order.return.successfull", getPortInEvent().getPortingOrderId());
                // Navigate to the view port screen.
                // Sample - 40000000002106
                setPortOrdersQuery(new PortOrdersQuery());
                getPortOrdersQuery().setPortingOrderId(getPortInEvent().getPortingOrderId());
                return searchPortOrder();

            } else { // Portin request failed - show the validation errors.
                for (String validationError : getPortInEvent().getValidationErrors()) {
                    localiseErrorAndAddToGlobalErrors("npc.validation.error", validationError);
                }
                return searchPortOrder();
            }
        } catch (SCABusinessError e) {
            log.error("Error while trying to return ported number to range holder - [{}] - [{}] - [{}]" + e.getErrorCode(), e.getErrorDesc(), e.getMessage());
            throw e;
        }
    }

    public Resolution cancelReturnOfPortedNumberToRangeHolder() {
        checkPermissions(Permissions.DO_NUMBER_RETURN);
        try {

            for (RoutingInfo routingInfo : getPortInEvent().getRoutingInfoList().getRoutingInfo()) {
                routingInfo.setRoutingNumber(Utils.getPropertyValueFromList("env.mnp.config", "SmileNetworkRoutingNumber"));
            }

            // Retrieve the customer associated with this porting
            CustomerQuery q = new CustomerQuery();
            q.setCustomerId(getPortInEvent().getCustomerProfileId());
            q.setResultLimit(1);
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(q));

            getPortInEvent().setCustomerFirstName(getCustomer().getFirstName());
            getPortInEvent().setCustomerLastName(getCustomer().getLastName());
            getPortInEvent().setMessageType(StMNPMessageTypes.SMILE_CANCEL_NUMBER_RETURN);
            getPortInEvent().setGender(getCustomer().getGender());
            getPortInEvent().setPortingDirection("IN");

            setPortInEvent(SCAWrapper.getUserSpecificInstance().handlePortInEvent(getPortInEvent()));

            if (getPortInEvent().getProcessingStatus().equals(MNP_REQUEST_PROCESSING_STATUS_DONE)) {
                // Port Order was successfully placed with the clearing house.
                setPageMessage("port.order.return.cancelled", getPortInEvent().getPortingOrderId());
                // Navigate to the view port screen.

                setPortOrdersQuery(new PortOrdersQuery());
                getPortOrdersQuery().setPortingOrderId(getPortInEvent().getPortingOrderId());
                return searchPortOrder();

            } else { // Portin request failed - show the validation errors.
                for (String validationError : getPortInEvent().getValidationErrors()) {
                    localiseErrorAndAddToGlobalErrors("npc.validation.error", validationError);
                }
                return showCreateCustomerPortInRequest();
            }
        } catch (SCABusinessError e) {
            log.error("Error while trying to return ported number to range holder - [{}] - [{}] - [{}]" + e.getErrorCode(), e.getErrorDesc(), e.getMessage());
            setPageMessage("porting.return.to.block.holder.failed");
        }

        setPortOrdersQuery(new PortOrdersQuery());
        getPortOrdersQuery().setPortingOrderId(getPortInEvent().getPortingOrderId());
        return searchPortOrder();
    }

    public Resolution showSearchPortOrder() {
        return getDDForwardResolution("/mnp/search_port_order.jsp");
    }

    public Resolution searchPortOrder() {
        try {
            if (getCustomer() != null) {
                if (getPortOrdersQuery() == null) {
                    setPortOrdersQuery(new PortOrdersQuery());
                }
                getPortOrdersQuery().setCustomerProfileId(getCustomer().getCustomerId());
            }

            setPortOrdersList(SCAWrapper.getUserSpecificInstance().getPortOrders(getPortOrdersQuery()));
            if (getPortOrdersList().getNumberOfPortationEvents() <= 0) {
                localiseErrorAndAddToGlobalErrors("no.records.found");
                return getDDForwardResolution("/mnp/search_port_order.jsp");
            }

            // getPortInEvent().getRoutingInfoList().getRoutingInfo();
            // getPortInEvent().getRoutingInfoList().getRoutingInfo().get(0).getPhoneNumberRange().getPhoneNumberStart()
            if (getPortOrdersList().getPortInEvents().size() == 1) {

                for (RoutingInfo routingInfo : getPortOrdersList().getPortInEvents().get(0).getRoutingInfoList().getRoutingInfo()) {
                    if (routingInfo.getServiceInstanceId() > 0) {
                        ServiceInstanceQuery q = new ServiceInstanceQuery();
                        q.setServiceInstanceId(routingInfo.getServiceInstanceId());
                        q.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                        ServiceInstance si = SCAWrapper.getUserSpecificInstance().getServiceInstance(q);
                        routingInfo.setServiceInstanceStatus(si.getStatus());
                    }
                }
                setPortInEvent(getPortOrdersList().getPortInEvents().get(0)); // Set the port order to view ...

                writeCustomerPhotographsDataToFile(getPortInEvent().getPortRequestForms()); //So they can be diplayed on the view

                return getDDForwardResolution("/mnp/view_port_order.jsp");
            } else {
                return getDDForwardResolution("/mnp/search_port_order.jsp");
            }

        } catch (SCABusinessError e) {
            showSearchCustomer();
            throw e;
        }
    }

    public Resolution showEmergencyRestore() {
        checkPermissions(Permissions.DO_EMERGENCY_RESTORE);
        // Diaplay a list of port orders that can be emergency restored...
        // checkPermissions(Permissions.ADD_ORGANISATION);
        try {
            setPortOrdersQuery(new PortOrdersQuery());

            if (getPortInEvent() == null) {
                getPortOrdersQuery().setCustomerProfileId(getCustomer().getCustomerId());
                getPortOrdersQuery().setPortingDirection("OUT");
                getPortOrdersQuery().setPortingState("NP_DONE");
                getPortOrdersQuery().setProcessingState(MNP_REQUEST_PROCESSING_STATUS_DONE);
            } else {
                // Pull out a specific port order for emergency restore
                getPortOrdersQuery().setPortingOrderId(getPortInEvent().getPortingOrderId());
                getPortOrdersQuery().setPortingDirection("OUT");
            }

            setPortOrdersList(SCAWrapper.getUserSpecificInstance().getPortOrders(getPortOrdersQuery()));
            if (getPortOrdersList().getNumberOfPortationEvents() <= 0) {
                localiseErrorAndAddToGlobalErrors("no.ported.out.orders.to.restore");
                return retrieveCustomer();
            }

            if (getPortOrdersList().getPortInEvents().size() == 1) {
                setPortInEvent(getPortOrdersList().getPortInEvents().get(0)); // Set the port order to view ...
                getPortInEvent().setMessageType(StMNPMessageTypes.SMILE_REQUEST_EMERGENCY_RESTORE_ID);

                return getDDForwardResolution("/mnp/view_port_order.jsp");
            } else { // To view the list of port orders and select one to 
                for (PortInEvent portInEvent : getPortOrdersList().getPortInEvents()) {
                    portInEvent.setMessageType(StMNPMessageTypes.SMILE_REQUEST_EMERGENCY_RESTORE_ID);
                }
                return getDDForwardResolution("/mnp/search_port_order.jsp");
            }
        } catch (SCABusinessError e) {
            retrieveCustomer();
            throw e;
        }
    }

    public Resolution requestEmergencyRestoreId() {
        checkAddressPermissions(); // Will check the return permissions here ...
        try {

            // getPortInEvent().setMessageType(StMNPMessageTypes.SMILE_REQUEST_EMERGENCY_RESTORE_ID);
            getPortInEvent().setPortingDirection("OUT");
            getPortInEvent().setIsEmergencyRestore("Y");

            setPortInEvent(SCAWrapper.getUserSpecificInstance().handlePortInEvent(getPortInEvent()));

            if (getPortInEvent().getProcessingStatus().equals(MNP_REQUEST_PROCESSING_STATUS_IN_PROGRESS)) {
                // Port Order was successfully placed with the clearing house.
                setPageMessage("port.order.emergency.restore.request.submitted", getPortInEvent().getPortingOrderId());
                // Navigate to the view port screen.

                setPortOrdersQuery(new PortOrdersQuery());
                getPortOrdersQuery().setPortingOrderId(getPortInEvent().getPortingOrderId());
                return searchPortOrder();

            } else { // Portin request failed - show the validation errors.

                for (String validationError : getPortInEvent().getValidationErrors()) {
                    localiseErrorAndAddToGlobalErrors("npc.validation.error", validationError);
                }

                return showEmergencyRestore();
            }
        } catch (SCABusinessError e) {
            log.error("Error while trying to return ported number to range holder - [{}] - [{}] - [{}]" + e.getErrorCode(), e.getErrorDesc(), e.getMessage());
            throw e;
        }
    }

    public Resolution showAddAddresses() {
        checkAddressPermissions();
        return getDDForwardResolution("/address/add_addresses.jsp");
    }

    public Resolution updateAddress() {
        checkAddressPermissions();
        try {
            SCAWrapper.getUserSpecificInstance().modifyAddress(getAddress());
            if (getCustomer() != null || getOrganisation() != null) {
                setPageMessage("address.updated.confirmation");
            }
        } catch (SCABusinessError e) {
            showEditAddress();
            throw e;
        }
        return manageAddresses();
    }

    public Resolution showEditAddress() {
        checkAddressPermissions();
        return getDDForwardResolution("/address/edit_address.jsp");
    }

    public void checkAddressPermissions() {
        if (getCustomer() != null) {
            checkPermissions(Permissions.EDIT_CUSTOMER_ADDRESS);
        }
        if (getOrganisation() != null) {
            checkPermissions(Permissions.EDIT_ORGANISATION_ADDRESS);
        }
    }

    public Resolution editCustomer() {
        checkPermissions(Permissions.EDIT_CUSTOMER_DATA);
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
        getCustomer().setDateOfBirth(Utils.addSlashesToDate(getCustomer().getDateOfBirth()));
        if (!getCustomer().getPassportExpiryDate().isEmpty()) {
            getCustomer().setPassportExpiryDate(Utils.addSlashesToDate(getCustomer().getPassportExpiryDate()));
        }

        if (!getCustomer().getVisaExpiryDate().isEmpty()) {
            getCustomer().setVisaExpiryDate(Utils.addSlashesToDate(getCustomer().getVisaExpiryDate()));
        }

        // Check for Nira status on Local customers here
        if (BaseUtils.getBooleanProperty("env.customer.verify.with.nira", false)
                && getCustomer().getNationality().equalsIgnoreCase(BaseUtils.getProperty("env.locale.country.for.language.en"))
                && isAllowed(BaseUtils.getProperty("env.sep.roles.allowed.to.verify.customers.with.nira", ""))) {
            // As resquested by UG.
            getCustomer().setFirstName("");
            getCustomer().setMiddleName("");
            getCustomer().setLastName("");
            getCustomer().setDateOfBirth("");
            getCustomer().setIdentityNumber("");
            getCustomer().setCardNumber("");
            setDisplayCardNumberField(true);

            localiseErrorAndAddToGlobalErrors("customer.nira.verify.required");

        } else {
            setDisplayCardNumberField(false);
        }

        return getDDForwardResolution("/customer/edit_customer.jsp");
    }

    public Resolution editCustomerNIN() {
        checkPermissions(Permissions.EDIT_CUSTOMER_NATIONAL_ID_NUMBER);
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomer(SCAWrapper.getAdminInstance().getCustomer(getCustomerQuery()));
        getCustomer().setDateOfBirth(Utils.addSlashesToDate(getCustomer().getDateOfBirth()));
        if (!getCustomer().getPassportExpiryDate().isEmpty()) {
            getCustomer().setPassportExpiryDate(Utils.addSlashesToDate(getCustomer().getPassportExpiryDate()));
        }

        if (!getCustomer().getVisaExpiryDate().isEmpty()) {
            getCustomer().setVisaExpiryDate(Utils.addSlashesToDate(getCustomer().getVisaExpiryDate()));
        }

        // Check for Nira status on Local customers here
        if (BaseUtils.getBooleanProperty("env.customer.verify.with.nira", false)
                && getCustomer().getNationality().equalsIgnoreCase(BaseUtils.getProperty("env.locale.country.for.language.en"))
                && isAllowed(BaseUtils.getProperty("env.sep.roles.allowed.to.verify.customers.with.nira", ""))) {
            // As resquested by UG.
            getCustomer().setFirstName("");
            getCustomer().setMiddleName("");
            getCustomer().setLastName("");
            getCustomer().setDateOfBirth("");
            getCustomer().setIdentityNumber("");
            getCustomer().setCardNumber("");
            setDisplayCardNumberField(true);

            localiseErrorAndAddToGlobalErrors("customer.nira.verify.required");

        } else {
            setDisplayCardNumberField(false);
        }

        return getDDForwardResolution("/customer/edit_customer_nin.jsp");
    }

    public boolean displayCardNumberField = false;

    public boolean isDisplayCardNumberField() {
        return displayCardNumberField;
    }

    public void setDisplayCardNumberField(boolean displayCardNumberField) {
        this.displayCardNumberField = displayCardNumberField;
    }

    public Resolution showPendingKYC() {
        checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_VERIFIED);
        setCustomerQuery(new CustomerQuery());
        getCustomerQuery().setKYCStatus("P");
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);
        getCustomerQuery().setResultLimit(BaseUtils.getIntProperty("env.sep.kyc.validate.rows", 10));
        setCustomerList(SCAWrapper.getUserSpecificInstance().getCustomers(getCustomerQuery()));
        lastModifiedBy = new HashMap<>();
        List<Customer> custlist = getCustomerList().getCustomers();
        Collections.sort(custlist, (Customer o1, Customer o2) -> o1.getCreatedDateTime().compare(o2.getCreatedDateTime()));
        for (Customer c : custlist) {
            List<Photograph> justPhotos = new ArrayList<>();
            for (Photograph p : c.getCustomerPhotographs()) {
                if (!p.getPhotoType().equals("fingerprints")) {
                    String fileName = p.getPhotoGuid();
                    try {
                        Utils.createTempFile(fileName, Utils.decodeBase64(p.getData()));
                        justPhotos.add(p);
                    } catch (Exception e) {
                        log.warn("Error in writeCustomerPhotographsDataToFile", e);
                    }
                }
            }
            c.getCustomerPhotographs().clear();
            c.getCustomerPhotographs().addAll(justPhotos);
            lastModifiedBy.put(c.getCustomerId(), getLastModifiedBySalesPerson(c).getCustomerId());
        }

        return getDDForwardResolution("/customer/kyc_verification.jsp");
    }

    private Map<Integer, Integer> lastModifiedBy;

    public Map<Integer, Integer> getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(Map<Integer, Integer> lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    private List<Integer> KYCVerified;

    private List<Integer> KYCUnverified;

    public List<Integer> getKYCVerified() {
        return KYCVerified;
    }

    public void setKYCVerified(List<Integer> KYCVerified) {
        this.KYCVerified = KYCVerified;
    }

    public List<Integer> getKYCUnverified() {
        return KYCUnverified;
    }

    public void setKYCUnverified(List<Integer> KYCUnverified) {
        this.KYCUnverified = KYCUnverified;
    }

    private Map<Integer, String> unverifiedReason;

    public Map<Integer, java.lang.String> getUnverifiedReason() {
        if (unverifiedReason == null) {
            unverifiedReason = new HashMap<>();
        }
        return unverifiedReason;
    }

    public void setUnverifiedReason(Map<Integer, java.lang.String> unverifiedReason) {
        this.unverifiedReason = unverifiedReason;
    }

    private Customer getLastModifiedBySalesPerson(Customer cust) {
        // Get the last sales person to modify the customer
        EventQuery eq = new EventQuery();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -7);
        eq.setDateFrom(Utils.getDateAsXMLGregorianCalendar(cal.getTime()));
        eq.setDateTo(Utils.getDateAsXMLGregorianCalendar(new Date()));
        eq.setEventKey(String.valueOf(cust.getCustomerId()));
        eq.setEventType("IM");
        eq.setEventSubType("modifyCustomer");
        eq.setResultLimit(1000);
        EventList events = SCAWrapper.getUserSpecificInstance().getEvents(eq);
        log.debug("Number of events is [{}]", events.getNumberOfEvents());
        Customer salesPerson = null;
        for (Event e : events.getEvents()) {
            try {
                String userName = Utils.getBetween(events.getEvents().get(events.getEvents().size() - 1).getEventData(), "<OriginatingIdentity>", "</OriginatingIdentity>");
                if (userName.equals("admin")) {
                    continue;
                }
                CustomerQuery q = new CustomerQuery();
                q.setSSOIdentity(userName);
                q.setResultLimit(1);
                q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                Customer sp = SCAWrapper.getUserSpecificInstance().getCustomer(q);
                if (!sp.getEmailAddress().equals(cust.getEmailAddress())) {
                    log.debug("Customer was modified by email address [{}]", sp.getEmailAddress());
                    salesPerson = sp;
                }
            } catch (Exception ex) {
                log.warn("Error getting modifier from [{}]", e.getEventData());
            }
        }
        if (salesPerson == null) {
            salesPerson = UserSpecificCachedDataHelper.getCustomer(cust.getCreatedByCustomerProfileId(), StCustomerLookupVerbosity.CUSTOMER);
        }
        return salesPerson;
    }

    public Resolution updateKYCStatus() {
        checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_VERIFIED);
        if (getKYCVerified() != null) {
            for (int custId : getKYCVerified()) {
                log.debug("Setting KYC status to V on cust id [{}]", custId);
                Customer c = UserSpecificCachedDataHelper.getCustomer(custId, StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
                if (c.getKYCStatus().equals("P")) {
                    c.setKYCStatus("V");
                    SCAWrapper.getUserSpecificInstance().modifyCustomer(c);
                    createTicket("KYC Data Accepted", "KYC Verified", "KYC data on customer id " + c.getCustomerId() + " has been updated to KYCVerified.", c, true);
                }
            }
        }
        if (getKYCUnverified() != null && unverifiedReason != null) {
            Customer loggedIn = SCAWrapper.getUserSpecificInstance().getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER);
            for (int custId : getKYCUnverified()) {
                log.debug("Setting KYC status to U on cust id [{}]", custId);
                Customer c = UserSpecificCachedDataHelper.getCustomer(custId, StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
                if (c.getKYCStatus().equals("P")) {
                    Customer salesPerson = getLastModifiedBySalesPerson(c);
                    String reason = unverifiedReason.get(c.getCustomerId());
                    try {
                        if (salesPerson != null) {
                            Event eventData = new Event();
                            eventData.setEventType("KYC");
                            eventData.setEventSubType("U");
                            eventData.setEventKey(String.valueOf(c.getCustomerId()));
                            eventData.setEventData(salesPerson.getCustomerId() + "|" + loggedIn.getCustomerId() + "|" + reason);
                            SCAWrapper.getAdminInstance().createEvent(eventData);
                            IMAPUtils.sendEmail(loggedIn.getEmailAddress(), salesPerson.getEmailAddress(), "KYC Data Rejected!", "KYC data on customer id " + c.getCustomerId() + " must be recaptured. Reason: " + reason);
                            createTicket("KYC Data Rejected", "KYC Rejected!", "KYC data on customer id " + c.getCustomerId() + " must be recaptured. Reason: " + reason, c, false);
                        }

                    } catch (Exception ex) {
                        log.warn("Error sending KYC rejection email", ex);
                    }

                    c.setKYCStatus("U");
                    SCAWrapper.getUserSpecificInstance().modifyCustomer(c);

                }
            }
        }
        setPageMessage("kyc.status.updated");
        return showPendingKYC();
    }

    public Resolution updateCustomerNIN() {
        checkCSRF();
        checkPermissions(Permissions.EDIT_CUSTOMER_NATIONAL_ID_NUMBER);

        getCustomer().setDateOfBirth(Utils.getDateWithoutSlashes(getCustomer().getDateOfBirth()));
        getCustomer().setPassportExpiryDate(Utils.getDateWithoutSlashes(getCustomer().getPassportExpiryDate()));
        getCustomer().setVisaExpiryDate(Utils.getDateWithoutSlashes(getCustomer().getVisaExpiryDate()));
        getCustomer().getSecurityGroups().clear();

        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(getCustomer().getCustomerId());
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_CUSTOMERNINDATA);
        Customer tmpCustomer = SCAWrapper.getAdminInstance().getCustomer(q);

        if (isComingFromMakeSale() && BaseUtils.getBooleanProperty("env.customer.verify.with.nira", false)
                && isAllowed(BaseUtils.getProperty("env.sep.roles.allowed.to.verify.customers.with.nira", ""))) {
            tmpCustomer.getOutstandingTermsAndConditions().clear();
            tmpCustomer.getCustomerRoles().clear();
            // Nira is only allowed to modify 5 fields.
            /*  
                    NIN: CM930121003EGE
                    Document ID: 000092564
                    SURNAME: Tipiyai
                    Given Names: Johnson
                    OtherNames:
                    Date Of Birth: 01/01/1993 
             */
            tmpCustomer.setIdentityNumber(getCustomer().getIdentityNumber());
            tmpCustomer.setCardNumber(getCustomer().getCardNumber());
            tmpCustomer.setLastName(getCustomer().getLastName());
            tmpCustomer.setMiddleName(getCustomer().getMiddleName());
            tmpCustomer.setDateOfBirth(getCustomer().getDateOfBirth());

            SCAWrapper.getUserSpecificInstance().modifyCustomer(tmpCustomer);

            setPageMessage("customer.nira.verify.successful");
            ForwardResolution forward = (ForwardResolution) getDDForwardResolution(SalesActionBean.class, "collectCustomerRoleAndAccountDataForSale");
            // forward.addParameter("customerQuery", getCustomerQuery());
            forward.addParameter("customer.customerId", getCustomer().getCustomerId());
            return forward;
        } else {
            log.warn("Photo entered1");
            if (tmpCustomer.getOptInLevel() != getCustomer().getOptInLevel()) {
                checkPermissions(Permissions.EDIT_CUSTOMER_OPT_IN_LEVEL);
            }

            if (!tmpCustomer.getEmailAddress().equals(getCustomer().getEmailAddress())) {
                checkPermissions(Permissions.EDIT_CUSTOMER_EMAIL_ADDRESS);
                for (String permission : tmpCustomer.getSecurityGroups()) {
                    if (!permission.equals("Customer") && !permission.startsWith("Promo")) {
                        checkPermissions(Permissions.EDIT_SEP_USER_EMAIL_ADDRESS);
                    }
                }
            }

            if (!tmpCustomer.getWarehouseId().equals(getCustomer().getWarehouseId())) {
                checkPermissions(Permissions.EDIT_CUSTOMER_WAREHOUSE_ID);
            }
            if (getCustomer().getKYCStatus() != null) {

                if (getCustomer().getKYCStatus().equals("V") && !tmpCustomer.getKYCStatus().equals("V")) {
                    checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_VERIFIED);
                }
                if (!getCustomer().getKYCStatus().equals("V") && tmpCustomer.getKYCStatus().equals("V")) {
                    checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_FROM_VERIFIED);
                }
                if (getCustomer().getKYCStatus().equals("P") && !tmpCustomer.getKYCStatus().equals("P")) {
                    checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_PENDING);
                }

            }
            if (getCustomer().getNationality() == null) {
                getCustomer().setNationality(tmpCustomer.getNationality());
            }
            if (getCustomer().getIdentityNumberType() == null) {
                getCustomer().setIdentityNumberType(tmpCustomer.getIdentityNumberType());
            }
            log.warn("Photo entered2");
            getCustomer().setCustomerStatus(tmpCustomer.getCustomerStatus());
            getCustomer().getSecurityGroups().addAll(tmpCustomer.getSecurityGroups());
            getCustomer().getOutstandingTermsAndConditions().clear();
            getCustomer().getOutstandingTermsAndConditions().addAll(tmpCustomer.getOutstandingTermsAndConditions());
            getCustomer().getCustomerRoles().clear();
            getCustomer().getCustomerRoles().addAll(tmpCustomer.getCustomerRoles());
            getCustomer().setSSODigest(tmpCustomer.getSSODigest());
            getCustomer().setSSOIdentity(tmpCustomer.getSSOIdentity());
            getCustomer().setAccountManagerCustomerProfileId(tmpCustomer.getAccountManagerCustomerProfileId());
            getCustomer().setIsNinVerified(tmpCustomer.getIsNinVerified());

            if (BaseUtils.getBooleanProperty("env.customer.verify.with.nin.updatecustoemrnin", false)) {
                if (getCustomer().getNationalIdentityNumber() != null && !getCustomer().getNationalIdentityNumber().isEmpty()) {
                    try {
                        CustomerQuery query = new CustomerQuery();
                        query.setCustomerId(getCustomer().getCustomerId());
                        query.setResultLimit(1);
                        query.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);
                        Customer customer = SCAWrapper.getAdminInstance().getCustomer(query);

                        // Check if customer has fingerprints.
                        if (customer.getIsNinVerified().equalsIgnoreCase("N") || customer.getIsNinVerified().equalsIgnoreCase("P")) {
                            byte[] fingerPrintData = null;
                            log.warn("Photo Entered");
                            for (Photograph photo : customer.getCustomerPhotographs()) {
                                if (photo.getPhotoType().equalsIgnoreCase("fingerprints")) {
                                    String fileName = photo.getPhotoGuid();
                                    try {
                                        Utils.createTempFile(fileName, Utils.decodeBase64(photo.getData()));
                                        fingerPrintData = Utils.getDataFromTempFile(photo.getPhotoGuid());
                                        log.warn("Photo " + fingerPrintData);
                                    } catch (Exception e) {
                                        log.warn("Error in writeCustomerPhotographsDataToFile", e);
                                    }
                                }
                            }
                            VerifyNinQuery verifyNinQuery = new VerifyNinQuery();
                            verifyNinQuery.setNin(getCustomer().getNationalIdentityNumber());
                            verifyNinQuery.setFirstName(getCustomer().getFirstName());
                            verifyNinQuery.setLastName(getCustomer().getLastName());
                            verifyNinQuery.setSurname(getCustomer().getMiddleName());
                            verifyNinQuery.setVerifiedBy(getUser());
                            verifyNinQuery.setEntityId(Integer.toString(getCustomer().getCustomerId()));
                            verifyNinQuery.setEntityType("UPDATECUSTOMER");
                            CustomerNinData customerNinData = new CustomerNinData();
                            if (fingerPrintData != null) {
                                if (BaseUtils.getBooleanProperty("env.nimc.use.fingerprint.updatecustoemrnin", false)) {
                                    verifyNinQuery.setFingerStringInBase64(fingerPrintData);
                                    customerNinData.setNinVerificationType("NIN & Fingerprint");
                                } else {
                                    verifyNinQuery.setFingerStringInBase64(null);
                                    customerNinData.setNinVerificationType("NIN only");
                                }
                            }
                            VerifyNinResponseList verifyNinResponseList = SCAWrapper.getAdminInstance().verifyNinCustomer_Direct(verifyNinQuery);
                            if (verifyNinResponseList.getReturnMessage().equalsIgnoreCase(BaseUtils.getProperty("env.nin.status.message"))) {
                                VerifyNinReply verifyNinReply = verifyNinResponseList.getVerifyNinReplys().get(0);
                                log.debug("NIN [{}]", verifyNinReply.getNin());
                                SimpleDateFormat sdfo1 = new SimpleDateFormat("dd-MM-yyyy");
                                SimpleDateFormat sdfo2 = new SimpleDateFormat("yyyyMMdd");
                                Date d1 = sdfo1.parse(verifyNinReply.getBirthdate());
                                Date d2 = sdfo2.parse(customer.getDateOfBirth());
                                if (verifyNinReply.getFirstname().equalsIgnoreCase(customer.getFirstName()) && verifyNinReply.getSurname().equalsIgnoreCase(customer.getLastName()) && d1.equals(d2)) {
                                    if (BaseUtils.getBooleanProperty("env.nimc.use.fingerprint.updatecustoemrnin", false)) {
                                        getCustomer().setIsNinVerified("Y");
                                    } else {
                                        getCustomer().setIsNinVerified("P");
                                    }
                                } else {
                                    getCustomer().setIsNinVerified("M");
                                }
                                customerNinData.setCustomerProfileId(getCustomer().getCustomerId());
                                customerNinData.setNinVerificationTrackingId(verifyNinResponseList.getVerifyNinReplys().get(0).getTrackingId());
                                Date date = Calendar.getInstance().getTime();
                                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                                String createdDate = dateFormat.format(date);
                                customerNinData.setNinVerifiedDate(createdDate);
                                customerNinData.setNinResponseStatus(verifyNinResponseList.getReturnMessage());
                                if (tmpCustomer.getNationalIdentityNumber() == null || tmpCustomer.getNationalIdentityNumber().isEmpty()) {
                                    customerNinData.setNinCollectionDate(createdDate);
                                } else if (tmpCustomer.getCustomerNinData() != null) {
                                    if ((tmpCustomer.getCustomerNinData().getNinCollectionDate() != null && !tmpCustomer.getCustomerNinData().getNinCollectionDate().isEmpty())) {
                                        customerNinData.setNinCollectionDate(tmpCustomer.getCustomerNinData().getNinCollectionDate());
                                    }
                                } else {
                                    customerNinData.setNinCollectionDate("");
                                }
                                getCustomer().setCustomerNinData(customerNinData);
                            } else {
                                getCustomer().setIsNinVerified("N");
                                customerNinData.setCustomerProfileId(getCustomer().getCustomerId());
                                customerNinData.setNinVerificationTrackingId(verifyNinResponseList.getReturnMessage());
                                Date date = Calendar.getInstance().getTime();
                                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                                String createdDate = dateFormat.format(date);
                                customerNinData.setNinVerifiedDate(createdDate);
                                customerNinData.setNinResponseStatus(verifyNinResponseList.getReturnMessage());
                                if (tmpCustomer.getNationalIdentityNumber() == null || tmpCustomer.getNationalIdentityNumber().isEmpty()) {
                                    customerNinData.setNinCollectionDate(createdDate);
                                } else if (tmpCustomer.getCustomerNinData() != null) {
                                    if ((tmpCustomer.getCustomerNinData().getNinCollectionDate() != null && !tmpCustomer.getCustomerNinData().getNinCollectionDate().isEmpty())) {
                                        customerNinData.setNinCollectionDate(tmpCustomer.getCustomerNinData().getNinCollectionDate());
                                    }
                                } else {
                                    customerNinData.setNinCollectionDate("");
                                }
                                getCustomer().setCustomerNinData(customerNinData);
                            }
                        }
                    } catch (Exception e) {
                        CustomerNinData customerNinData = new CustomerNinData();
                        customerNinData.setCustomerProfileId(getCustomer().getCustomerId());
                        Date date = Calendar.getInstance().getTime();
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                        String createdDate = dateFormat.format(date);
                        customerNinData.setNinVerifiedDate(createdDate);
                        customerNinData.setNinResponseStatus("Error");
                        if (tmpCustomer.getNationalIdentityNumber() == null || tmpCustomer.getNationalIdentityNumber().isEmpty()) {
                            customerNinData.setNinCollectionDate(createdDate);
                            customerNinData.setNinVerificationTrackingId("");
                        } else if (tmpCustomer.getCustomerNinData() != null) {
                            if ((tmpCustomer.getCustomerNinData().getNinCollectionDate() != null && !tmpCustomer.getCustomerNinData().getNinCollectionDate().isEmpty())) {
                                customerNinData.setNinCollectionDate(tmpCustomer.getCustomerNinData().getNinCollectionDate());
                                customerNinData.setNinVerificationTrackingId(tmpCustomer.getCustomerNinData().getNinVerificationTrackingId());
                            }
                        } else {
                            customerNinData.setNinVerificationTrackingId("");
                            customerNinData.setNinCollectionDate("");
                        }
                        getCustomer().setCustomerNinData(customerNinData);
                        
                        String err="";
                        if(e.getMessage().toString().toLowerCase().contains("with description")) {
                            err = e.getMessage().toString().substring(e.getMessage().toString().toLowerCase().indexOf("description"));
                        } else {
                            err = e.getMessage().toString().substring(e.getMessage().toString().toLowerCase().indexOf("desc"));
                        }
                        localiseErrorAndAddToGlobalErrors("nimc.general.error.message", err);
                        log.debug("NIN Verification in updateCustomer() [{}]", e);
                    }
                }
            }
            log.debug("Before Updating NIN [{}]");
            SCAWrapper.getAdminInstance().modifyCustomer(getCustomer());
            if (getCustomer() != null) {
                setPageMessage("customer.updated.successfully");
            }

            return retrieveCustomer();
        }

    }

    public Resolution updateCustomer() {
        checkCSRF();
        checkPermissions(Permissions.EDIT_CUSTOMER_DATA);
        // Set the permissions to what they are already

        getCustomer().setDateOfBirth(Utils.getDateWithoutSlashes(getCustomer().getDateOfBirth()));
        getCustomer().setPassportExpiryDate(Utils.getDateWithoutSlashes(getCustomer().getPassportExpiryDate()));
        getCustomer().setVisaExpiryDate(Utils.getDateWithoutSlashes(getCustomer().getVisaExpiryDate()));
        getCustomer().getSecurityGroups().clear();
        getCustomer().getCustomerSellers().clear();

        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(getCustomer().getCustomerId());
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        Customer tmpCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(q);

        if (BaseUtils.getBooleanProperty("env.customer.national.id.mandatory", false)) {
            //If updating National ID Number (NIN), Check if the logged in user is allowed to update NIN
            if (tmpCustomer.getNationalIdentityNumber() != null) {  //If DB NIN is NOT null, compare with updating one            
                if (!tmpCustomer.getNationalIdentityNumber().equalsIgnoreCase(getCustomer().getNationalIdentityNumber())) { //Compare if different from the new update
                    checkPermissions(Permissions.EDIT_CUSTOMER_NATIONAL_ID_NUMBER);
                }
            } else { //If DB NIN is NULL
                if (getCustomer().getNationalIdentityNumber() != null && getCustomer().getNationalIdentityNumber().trim().length() > 0) { //Updating with NewNIN                   
                    checkPermissions(Permissions.EDIT_CUSTOMER_NATIONAL_ID_NUMBER);
                }
            }
        }

        if (isComingFromMakeSale() && BaseUtils.getBooleanProperty("env.customer.verify.with.nira", false)
                && isAllowed(BaseUtils.getProperty("env.sep.roles.allowed.to.verify.customers.with.nira", ""))) {
            tmpCustomer.getOutstandingTermsAndConditions().clear();
            tmpCustomer.getCustomerRoles().clear();
            // Nira is only allowed to modify 5 fields.
            /*  
                    NIN: CM930121003EGE
                    Document ID: 000092564
                    SURNAME: Tipiyai
                    Given Names: Johnson
                    OtherNames:
                    Date Of Birth: 01/01/1993 
             */
            tmpCustomer.setIdentityNumber(getCustomer().getIdentityNumber());
            tmpCustomer.setCardNumber(getCustomer().getCardNumber());
            tmpCustomer.setLastName(getCustomer().getLastName());
            tmpCustomer.setMiddleName(getCustomer().getMiddleName());
            tmpCustomer.setDateOfBirth(getCustomer().getDateOfBirth());

            SCAWrapper.getUserSpecificInstance().modifyCustomer(tmpCustomer);

            setPageMessage("customer.nira.verify.successful");
            ForwardResolution forward = (ForwardResolution) getDDForwardResolution(SalesActionBean.class, "collectCustomerRoleAndAccountDataForSale");
            // forward.addParameter("customerQuery", getCustomerQuery());
            forward.addParameter("customer.customerId", getCustomer().getCustomerId());
            return forward;
        } else {

            if (tmpCustomer.getOptInLevel() != getCustomer().getOptInLevel()) {
                checkPermissions(Permissions.EDIT_CUSTOMER_OPT_IN_LEVEL);
            }

            if (!tmpCustomer.getEmailAddress().equals(getCustomer().getEmailAddress())) {
                checkPermissions(Permissions.EDIT_CUSTOMER_EMAIL_ADDRESS);
                for (String permission : tmpCustomer.getSecurityGroups()) {
                    if (!permission.equals("Customer") && !permission.startsWith("Promo")) {
                        checkPermissions(Permissions.EDIT_SEP_USER_EMAIL_ADDRESS);
                    }
                }
            }

            if (!tmpCustomer.getWarehouseId().equals(getCustomer().getWarehouseId())) {
                checkPermissions(Permissions.EDIT_CUSTOMER_WAREHOUSE_ID);
            }
            if (getCustomer().getKYCStatus() != null) {

                if (getCustomer().getKYCStatus().equals("V") && !tmpCustomer.getKYCStatus().equals("V")) {
                    checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_VERIFIED);
                }
                if (!getCustomer().getKYCStatus().equals("V") && tmpCustomer.getKYCStatus().equals("V")) {
                    checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_FROM_VERIFIED);
                }
                if (getCustomer().getKYCStatus().equals("P") && !tmpCustomer.getKYCStatus().equals("P")) {
                    checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_PENDING);
                }

            }
            if (getCustomer().getNationality() == null) {
                getCustomer().setNationality(tmpCustomer.getNationality());
            }
            if (getCustomer().getIdentityNumberType() == null) {
                getCustomer().setIdentityNumberType(tmpCustomer.getIdentityNumberType());
            }

            getCustomer().setCustomerStatus(tmpCustomer.getCustomerStatus());
            getCustomer().getSecurityGroups().addAll(tmpCustomer.getSecurityGroups());
            getCustomer().getCustomerSellers().addAll(tmpCustomer.getCustomerSellers());
            getCustomer().getOutstandingTermsAndConditions().clear();
            getCustomer().getOutstandingTermsAndConditions().addAll(tmpCustomer.getOutstandingTermsAndConditions());
            getCustomer().getCustomerRoles().clear();
            getCustomer().getCustomerRoles().addAll(tmpCustomer.getCustomerRoles());
            getCustomer().setSSODigest(tmpCustomer.getSSODigest());
            getCustomer().setAccountManagerCustomerProfileId(tmpCustomer.getAccountManagerCustomerProfileId());
            getCustomer().setIsNinVerified(tmpCustomer.getIsNinVerified());

            if (BaseUtils.getBooleanProperty("env.customer.verify.with.nin.updatecustomer", false)) {
                if (getCustomer().getNationalIdentityNumber() != null && !getCustomer().getNationalIdentityNumber().isEmpty()) {
                    if (!getCustomer().getNationalIdentityNumber().equalsIgnoreCase(tmpCustomer.getNationalIdentityNumber())) {
                        try {
                            CustomerQuery query = new CustomerQuery();
                            query.setCustomerId(getCustomer().getCustomerId());
                            query.setResultLimit(1);
                            query.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);
                            Customer customer = SCAWrapper.getAdminInstance().getCustomer(query);

                            // Check if customer has fingerprints.
                            if (customer.getIsNinVerified().equalsIgnoreCase("N")) {
                                byte[] fingerPrintData = null;
                                log.warn("Photo Entered");
                                for (Photograph photo : customer.getCustomerPhotographs()) {
                                    if (photo.getPhotoType().equalsIgnoreCase("fingerprints")) {
                                        String fileName = photo.getPhotoGuid();
                                        try {
                                            Utils.createTempFile(fileName, Utils.decodeBase64(photo.getData()));
                                            fingerPrintData = Utils.getDataFromTempFile(photo.getPhotoGuid());
                                            log.warn("Photo " + fingerPrintData);
                                        } catch (Exception e) {
                                            log.warn("Error in writeCustomerPhotographsDataToFile", e);
                                        }
                                    }
                                }
                                VerifyNinQuery verifyNinQuery = new VerifyNinQuery();
                                verifyNinQuery.setNin(getCustomer().getNationalIdentityNumber());
                                verifyNinQuery.setFirstName(getCustomer().getFirstName());
                                verifyNinQuery.setLastName(getCustomer().getLastName());
                                verifyNinQuery.setSurname(getCustomer().getMiddleName());
                                verifyNinQuery.setVerifiedBy(getUser());
                                verifyNinQuery.setEntityId(Integer.toString(getCustomer().getCustomerId()));
                                verifyNinQuery.setEntityType("UPDATECUSTOMER");
                                
                                CustomerNinData customerNinData = new CustomerNinData();
                                if (fingerPrintData != null) {
                                    if (BaseUtils.getBooleanProperty("env.nimc.use.fingerprint.updatecustomer", true)) {
                                        verifyNinQuery.setFingerStringInBase64(fingerPrintData);
                                        customerNinData.setNinVerificationType("NIN & Fingerprint");
                                    } else {
                                        verifyNinQuery.setFingerStringInBase64(null);
                                        customerNinData.setNinVerificationType("NIN only");
                                    }
                                }
                                VerifyNinResponseList verifyNinResponseList = SCAWrapper.getUserSpecificInstance().verifyNinCustomer_Direct(verifyNinQuery);
                                if (verifyNinResponseList.getReturnMessage().equalsIgnoreCase(BaseUtils.getProperty("env.nin.status.message"))) {
                                    log.debug("NIN [{}]", verifyNinResponseList.getVerifyNinReplys().get(0).getNin());
                                    if (fingerPrintData != null) {
                                        getCustomer().setIsNinVerified("Y");
                                    } else {
                                        getCustomer().setIsNinVerified("P");
                                    }
                                    customerNinData.setCustomerProfileId(getCustomer().getCustomerId());
                                    customerNinData.setNinVerificationTrackingId(verifyNinResponseList.getVerifyNinReplys().get(0).getTrackingId());
                                    Date date = Calendar.getInstance().getTime();
                                    DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
                                    String createdDate = dateFormat.format(date);
                                    customerNinData.setNinVerifiedDate(createdDate);
                                    getCustomer().setCustomerNinData(customerNinData);
                                } else {
                                    getCustomer().setIsNinVerified("N");
                                    customerNinData.setCustomerProfileId(getCustomer().getCustomerId());
                                    customerNinData.setNinVerificationTrackingId(verifyNinResponseList.getReturnMessage());
                                    Date date = Calendar.getInstance().getTime();
                                    DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
                                    String createdDate = dateFormat.format(date);
                                    customerNinData.setNinVerifiedDate(createdDate);
                                    getCustomer().setCustomerNinData(customerNinData);
                                }
                            }
                        } catch (Exception e) {
                            String err="";
                            if(e.getMessage().toString().toLowerCase().contains("with description")) {
                                err = e.getMessage().toString().substring(e.getMessage().toString().toLowerCase().indexOf("description"));
                            } else {
                                err = e.getMessage().toString().substring(e.getMessage().toString().toLowerCase().indexOf("desc"));
                            }
                            localiseErrorAndAddToGlobalErrors("nimc.general.error.message", err);
                            log.debug("NIN Verification in updateCustomer() [{}]", e);
                        }
                    }
                }
            }

            SCAWrapper.getUserSpecificInstance().modifyCustomer(getCustomer());
            if (getCustomer() != null) {
                setPageMessage("customer.updated.successfully");
            }

            return retrieveCustomer();
        }
    }

    /**
     * Retrieve note types applicable to this entity
     *
     * @return Resolution The Stripes resolution object to tell Stripes where to
     * go next
     */
    public Resolution retrieveNoteTypes() {
        checkPermissions(Permissions.STICKY_NOTES);
        // Need to pass the notes page the info about this entity so it knows where to attach the note
        setStickyNoteEntityIdentifier(new StickyNoteEntityIdentifier());
        getStickyNoteEntityIdentifier().setEntityId(getCustomer().getCustomerId());
        getStickyNoteEntityIdentifier().setEntityType("Customer");
        setStickyNoteTypeList(SCAWrapper.getUserSpecificInstance().getStickyNoteTypeList(makeSCAString("Customer")));
        return getDDForwardResolution("/note/view_note_types.jsp");
    }

    /**
     * Retrieve notes attached to this entity
     *
     * @return Resolution The Stripes resolution object to tell Stripes where to
     * go next
     */
    public Resolution retrieveNotes() {
        checkPermissions(Permissions.STICKY_NOTES);
        // Need to pass the notes page the info about this entity so it knows where to attach the note
        setStickyNoteEntityIdentifier(new StickyNoteEntityIdentifier());
        getStickyNoteEntityIdentifier().setEntityId(getCustomer().getCustomerId());
        getStickyNoteEntityIdentifier().setEntityType("Customer");
        setStickyNoteList((StickyNoteList) SCAWrapper.getUserSpecificInstance().getEntitiesStickyNotes(getStickyNoteEntityIdentifier()));
        return getDDForwardResolution("/note/view_notes.jsp");
    }

    public Resolution showUpdateCustomerPermission() {
        checkPermissions(Permissions.EDIT_CUSTOMER_PERMISSIONS);
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
        return getDDForwardResolution("/customer/edit_customer_permission.jsp");
    }

    List<Organisation> organisationSellers = new ArrayList<Organisation>();

    public List<Organisation> getOrganisationSellers() {
        return organisationSellers;
    }

    public void setOrganisationSellers(List<Organisation> organisationSellers) {
        this.organisationSellers = organisationSellers;
    }

    List<Organisation> availableOrganisationSellers = new ArrayList<Organisation>();

    public List<Organisation> getAvailableOrganisationSellers() {
        return availableOrganisationSellers;
    }

    public void setAvailableOrganisationSellers(List<Organisation> availableOrganisationSellers) {
        this.availableOrganisationSellers = availableOrganisationSellers;
    }

    List<Customer> customerSellers = new ArrayList<Customer>();
    List<Customer> availableSellers = new ArrayList<Customer>();

    public List<Customer> getCustomerSellers() {
        return customerSellers;
    }

    public void setCustomerSellers(List<Customer> customerSellers) {
        this.customerSellers = customerSellers;
    }

    public List<Customer> getAvailableSellers() {
        return availableSellers;
    }

    public void setAvailableSellers(List<Customer> availableSellers) {
        this.availableSellers = availableSellers;
    }

    public Resolution showUpdateCustomerSellers() {
        checkPermissions(Permissions.EDIT_CUSTOMER_PERMISSIONS);
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));

        List<Integer> custSellers = getCustomer().getCustomerSellers();
        for (int seller : custSellers) {

            CustomerQuery q = new CustomerQuery();
            q.setCustomerId(seller);
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            q.setResultLimit(1);
            Customer retSeller = SCAWrapper.getAdminInstance().getCustomer(q);
            customerSellers.add(retSeller);
        }

        List<String> custRoles = getCustomer().getSecurityGroups();

        for (String icpRole : BaseUtils.getPropertyAsList("env.icp.allowed.roles")) {
            if (custRoles.contains(icpRole)) {
                //We need to get allAvailable SDPartners that can sell to ICPs
                /*
                CustomerQuery icpQuery = new CustomerQuery();
                icpQuery.setSecurityGroupType("SD");
                icpQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER); 
                icpQuery.setResultLimit(30);*/

                setAvailableSellers(retrieveAvailableSellers("SD"));
                for (Customer customerSeller : customerSellers) {
                    for (Customer avbSeller : availableSellers) {
                        if (customerSeller.getCustomerId() == avbSeller.getCustomerId()) {
                            availableSellers.remove(avbSeller);
                            break;
                        }
                    }
                }

                log.warn("NUMBER OF SDs: ", getAvailableSellers().size());
                return getDDForwardResolution("/customer/edit_customer_sellers.jsp");
            }
        }

        for (String superDealerRole : BaseUtils.getPropertyAsList("env.superdealer.allowed.roles")) {
            if (custRoles.contains(superDealerRole)) {
                /*CustomerQuery q = new CustomerQuery();
                q.setSecurityGroupType("MD");
                q.setResultLimit(50);
                q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);               
                setCustomerList(SCAWrapper.getUserSpecificInstance().getCustomers(q));                
                availableSellers.addAll(getCustomers());*/
                setAvailableSellers(retrieveAvailableSellers("MD"));
                for (Customer customerSeller : customerSellers) {
                    for (Customer avbSeller : availableSellers) {
                        if (customerSeller.getCustomerId() == avbSeller.getCustomerId()) {
                            availableSellers.remove(avbSeller);
                            break;
                        }
                    }
                }
                return getDDForwardResolution("/customer/edit_customer_sellers.jsp");
            }
        }
        localiseErrorAndAddToGlobalErrors("cannot.restrict.profile.to.seller");

        return getDDForwardResolution("/index.jsp");
    }

    public Resolution showUpdateOrganisationSellers() {
        checkPermissions(Permissions.EDIT_ORGANISATION_PERMISSIONS);

        try {
            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisation().getOrganisationId(), StOrganisationLookupVerbosity.MAIN));

        } catch (SCABusinessError e) {
            retrieveOrganisation();
            throw e;
        }

        List<Integer> orgSellers = getOrganisation().getOrganisationSellers();

        for (int seller : orgSellers) {
            OrganisationQuery q = new OrganisationQuery();
            q.setOrganisationId(seller);
            q.setVerbosity(StOrganisationLookupVerbosity.MAIN);
            q.setResultLimit(1);

            try {
                log.warn("Looking for OrganisationSeller with ID: {}", seller);
                Organisation retSeller = SCAWrapper.getAdminInstance().getOrganisation(q);

                log.warn("Organization Received: {}", retSeller.getOrganisationName());
                organisationSellers.add(retSeller);
            } catch (Exception e) {
                e.printStackTrace();
                log.warn("Error GETTING ORG: {}", e.getMessage());
            }
        }
        return getDDForwardResolution("/organisation/edit_organisation_sellers.jsp");
    }

    public Resolution showUpdateCustomerPassword() {
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
        return getDDForwardResolution("/customer/edit_customer_password.jsp");
    }

    public Resolution updateCustomerPassword() {
        checkCSRF();
        checkPermissions(Permissions.EDIT_CUSTOMER_PASSWORD);
        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(getCustomer().getCustomerId());
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomerQuery(q);
        Customer tmpCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(q);
        try {
            tmpCustomer.setSSODigest(Utils.hashPasswordWithComplexityCheck(getCustomer().getSSODigest()));
        } catch (Exception ex) {
            localiseErrorAndAddToGlobalErrors("password.too.simple");
            return showUpdateCustomerPassword();
        }
        tmpCustomer.setVersion(getCustomer().getVersion());
        SCAWrapper.getUserSpecificInstance().modifyCustomer(tmpCustomer);
        setPageMessage("customer.updated.successfully");
        return retrieveCustomer();
    }

    public Resolution sendPasswordResetLink() {
        checkCSRF();
        checkPermissions(Permissions.SEND_PASSWORD_RESET_LINK);
        SSOPasswordResetLinkData reset = new SSOPasswordResetLinkData();
        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(getCustomer().getCustomerId());
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomerQuery(q);
        Customer tmpCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(q);
        reset.setIdentifier(tmpCustomer.getSSOIdentity());
        log.debug("Sending reset link for [{}]", reset.getIdentifier());
        SCAWrapper.getUserSpecificInstance().sendSSOPasswordResetLink(reset);
        setPageMessage("reset.link.sent.to", tmpCustomer.getEmailAddress());
        return retrieveCustomer();
    }

    public Resolution retrieveTroubleTickets() {
        checkPermissions(Permissions.TROUBLE_TICKETING);
        return getDDForwardResolution("/TroubleTicket.action");
    }
    private OptionTransfer optionTransfer;

    public OptionTransfer getOptionTransfer() {
        return optionTransfer;
    }

    public void setOptionTransfer(OptionTransfer optionTransfer) {
        this.optionTransfer = optionTransfer;
    }

    public Resolution updateCustomerPermission() {
        checkCSRF();
        checkPermissions(Permissions.EDIT_CUSTOMER_PERMISSIONS);
        /* Retrieve values of current user group using OptionTransfer */
        String newRight = optionTransfer.getNewRight();
        // Get existing data
        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(getCustomer().getCustomerId());
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomerQuery(q);
        Customer tmpCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(q);
        log.debug("Checking that the logged in user is allowed to remove the permissions");

        Set<String> newGroups = new HashSet();
        newGroups.addAll(Arrays.asList(newRight.split(",")));
        Set<String> oldGroups = new HashSet();
        oldGroups.addAll(tmpCustomer.getSecurityGroups());

        // Check for removals
        for (String oldGroup : oldGroups) {
            if (!newGroups.contains(oldGroup) && !isUserAllowedToManageGroup(oldGroup)) {
                // Group has been removed
                localiseErrorAndAddToGlobalErrors("cannot.remove.permission");
                return showUpdateCustomerPermission();
            }
        }

        // Check for additions
        for (String newGroup : newGroups) {
            if (!oldGroups.contains(newGroup) && !isUserAllowedToManageGroup(newGroup)) {
                localiseErrorAndAddToGlobalErrors("cannot.assign.permission");
                return showUpdateCustomerPermission();
            }
        }

        tmpCustomer.getSecurityGroups().clear();
        tmpCustomer.getSecurityGroups().addAll(newGroups);
        tmpCustomer.setVersion(getCustomer().getVersion());
        SCAWrapper.getUserSpecificInstance().modifyCustomer(tmpCustomer);
        setPageMessage("customer.updated.successfully");
        return retrieveCustomer();
    }

    public Resolution updateCustomerSellers() {
        checkCSRF();
        checkPermissions(Permissions.EDIT_CUSTOMER_PERMISSIONS);
        /* Retrieve values of current user group using OptionTransfer */
        String newRight = optionTransfer.getNewRight();

        if (newRight.isEmpty()) {
            localiseErrorAndAddToGlobalErrors("no.seller.selected");
            return showUpdateCustomerSellers();
        }
        // Get existing data
        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(getCustomer().getCustomerId());
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomerQuery(q);
        Customer tmpCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(q);

        Set<String> newSellers = new HashSet();
        newSellers.addAll(Arrays.asList(newRight.split(",")));
        Set<Integer> oldSellers = new HashSet();
        oldSellers.addAll(tmpCustomer.getCustomerSellers());

        // Check for removals
        for (int oldSeller : oldSellers) {
            if (!newSellers.contains(String.valueOf(oldSeller)) && !isUserAllowedToManageGroup(String.valueOf(oldSeller))) {
                // Group has been removed
                localiseErrorAndAddToGlobalErrors("cannot.remove.permission");
                return showUpdateCustomerSellers();
            }
        }

        // Check for additions
        for (String newSeller : newSellers) {
            if (!oldSellers.contains(Integer.valueOf(newSeller)) && !isUserAllowedToManageGroup(newSeller)) {
                localiseErrorAndAddToGlobalErrors("cannot.assign.permission");
                return showUpdateCustomerSellers();
            }
        }

        tmpCustomer.getCustomerSellers().clear();
        for (String newSeller : newSellers) {
            tmpCustomer.getCustomerSellers().add(Integer.valueOf(newSeller));
        }

        tmpCustomer.setVersion(getCustomer().getVersion());
        SCAWrapper.getUserSpecificInstance().modifyCustomer(tmpCustomer);
        setPageMessage("customer.updated.successfully");
        return retrieveCustomer();
    }

    String orgsellers;

    public java.lang.String getOrgsellers() {
        return orgsellers;
    }

    public void setOrgsellers(java.lang.String orgsellers) {
        this.orgsellers = orgsellers;
    }

    public Resolution updateOrganisationSellers() {
        checkPermissions(Permissions.EDIT_ORGANISATION_PERMISSIONS);

        OrganisationQuery qry = new OrganisationQuery();
        qry.setVerbosity(StOrganisationLookupVerbosity.MAIN);
        qry.setOrganisationId(getOrganisation().getOrganisationId());
        qry.setResultLimit(1);
        try {
            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(qry));

        } catch (SCABusinessError e) {
            retrieveOrganisation();
            throw e;
        }

        Organisation tmpOrganisation = getOrganisation();

        String newRight = orgsellers;

        Set<String> newSellers = new HashSet();
        newSellers.addAll(Arrays.asList(newRight.split(",")));
        Set<Integer> oldSellers = new HashSet();
        oldSellers.addAll(tmpOrganisation.getOrganisationSellers());

        if (isICP(tmpOrganisation.getOrganisationId())) {  //All sellers must be SD
            for (String newSeller : newSellers) {

                if (newSeller.trim().length() > 0 && !isSuperDealer(Integer.parseInt(newSeller))) {
                    log.debug("I am ICP, cannot take org ID {}", newSeller);
                    localiseErrorAndAddToGlobalErrors("only.superdealer.can.sell.to.this.profile");
                    return showUpdateOrganisationSellers();
                }

            }
        } else if (isSuperDealer(tmpOrganisation.getOrganisationId())) { //All sellers must be MD
            for (String newSeller : newSellers) {
                if (newSeller.trim().length() > 0 && !isMegaDealer(Integer.parseInt(newSeller))) {
                    log.debug("I am SuperDealer, cannot take org ID {}", newSeller);
                    localiseErrorAndAddToGlobalErrors("only.megadealer.can.sell.to.this.profile");
                    return showUpdateOrganisationSellers();
                }
            }
        } else if (isMegaDealer(tmpOrganisation.getOrganisationId())) { //Only Smile can sell to MegaDealer
            for (String newSeller : newSellers) {
                if (newSeller.trim().length() > 0 && Integer.parseInt(newSeller) != 1) {
                    log.debug("I am MegaDealer, cannot take org ID {}", newSeller);
                    localiseErrorAndAddToGlobalErrors("invalid.seller.for.user.type");
                    return showUpdateOrganisationSellers();
                }
            }
        } else {
            localiseErrorAndAddToGlobalErrors("invalid.operation.for.user.type");
            return showUpdateOrganisationSellers();
        }

        // Check for removals
        for (int oldSeller : oldSellers) {
            if (!newSellers.contains(String.valueOf(oldSeller)) && !isUserAllowedToManageGroup(String.valueOf(oldSeller))) {
                // Group has been removed
                localiseErrorAndAddToGlobalErrors("cannot.remove.permission");
                return showUpdateOrganisationSellers();
            }
        }

        // Check for additions
        for (String newSeller : newSellers) {
            if (!newSeller.isEmpty() && !oldSellers.contains(Integer.valueOf(newSeller)) && !isUserAllowedToManageGroup(newSeller)) {
                localiseErrorAndAddToGlobalErrors("cannot.assign.permission");
                return showUpdateOrganisationSellers();
            }
        }

        tmpOrganisation.getOrganisationSellers().clear();
        for (String newSeller : newSellers) {
            if (!newSeller.isEmpty()) {
                tmpOrganisation.getOrganisationSellers().add(Integer.valueOf(newSeller));

            }
        }

        if (tmpOrganisation.getOrganisationSellers().size() == 0) {
            localiseErrorAndAddToGlobalErrors("atleast.one.seller.required.for.user.type");
            return showUpdateOrganisationSellers();
        }
        tmpOrganisation.setVersion(getOrganisation().getVersion());
        SCAWrapper.getUserSpecificInstance().modifyOrganisation(tmpOrganisation);
        setPageMessage("organisation.updated.successfully");
        return retrieveOrganisation();
    }

    private boolean isUserAllowedToManageGroup(String grp) {
        if (grp.equalsIgnoreCase("administrator")) {
            checkPermissions(Permissions.MAKE_CUSTOMER_AN_ADMINISTRATOR);
        }
        boolean allowed = false;
        if (!hasPermissions(Permissions.MAKE_CUSTOMER_AN_ADMINISTRATOR)) {
            log.debug("The logged in user has restrictions on what permissions can be granted. Going to check if the requested security group can be added");
            for (String role : getUsersRoles()) {
                try {
                    Set<String> allowedAssignments = BaseUtils.getPropertyAsSet("env.portal.allowed.assignments." + role);
                    if (allowedAssignments.contains(grp)) {
                        allowed = true;
                        break;
                    }
                } catch (Exception e) {
                    log.debug("Role [{}] has no allowed assignments", role);
                }
            }
        } else {
            allowed = true;
        }
        return allowed;
    }

    public Resolution showAddProductWizard() {
        ForwardResolution forward = (ForwardResolution) getDDForwardResolution(ProductCatalogActionBean.class, "showAddProductWizard");
        forward.addParameter("customer.customerId", getCustomer().getCustomerId());
        return forward;
    }

    public Resolution showCustomersSales() {
        ForwardResolution forward = (ForwardResolution) getDDForwardResolution(SalesActionBean.class, "searchSales");
        forward.addParameter("salesQuery.recipientCustomerId", getCustomer().getCustomerId());
        return forward;
    }

    public Resolution showContractsSales() {
        ForwardResolution forward = (ForwardResolution) getDDForwardResolution(SalesActionBean.class, "searchSales");
        forward.addParameter("salesQuery.contractId", getContract().getContractId());
        return forward;
    }

    /**
     * Returns array of friendly security group names (roles) that the customer
     * is not in currently
     *
     * @return
     */
    public String[] getAvailableUserGroups() {
        Set set = new java.util.TreeSet();
        /* Populate with ALL groups */
        List<String> mappings = BaseUtils.getPropertyAsList("global.customer.securitygroups");
        for (String mapping : mappings) {
            String group = mapping.split("-")[1]; // e.g. Role01-Administrator
            set.add(group);
        }
        List<String> customerGroups;
        if (getCustomer() != null) {
            customerGroups = getCustomer().getSecurityGroups();
        } else {
            customerGroups = getOrganisation().getModificationRoles();
        }


        /* Iterate through customer groups removing groups the customer belongs to */
        for (String customerGroup : customerGroups) {
            set.remove(customerGroup);
        }

        /* Transfer remaining groups to the StringArray */
        int setSize = set.size();
        String[] groups = new String[setSize];
        Iterator<String> it = set.iterator();
        int counter = 0;
        while (it.hasNext()) {
            groups[counter++] = it.next();
        }
        return groups;
    }

    public Resolution showMakeSale() {
        ForwardResolution forward = (ForwardResolution) getDDForwardResolution(SalesActionBean.class, "collectCustomerRoleAndAccountDataForSale");
        forward.addParameter("customer.customerId", getCustomer().getCustomerId());
        return forward;
    }
    private final List<Photograph> photographs = new ArrayList<>();

    public List<Photograph> getPhotographs() {
        return photographs;
    }

    public Resolution retrieveImagesSnippet() {

        try {
            if (getPortInEvent() != null) {

                setPortOrdersQuery(new PortOrdersQuery());
                getPortOrdersQuery().setPortingOrderId(getPortInEvent().getPortingOrderId());
                getPortOrdersQuery().setPortingDirection("IN");

                setPortInEvent(SCAWrapper.getUserSpecificInstance().getPortOrders(getPortOrdersQuery()).getPortInEvents().get(0));

                getPhotographs().addAll(getPortInEvent().getPortRequestForms());
                writeCustomerPhotographsDataToFile(getPhotographs());
            } else if (getContract() != null) {
                setContractQuery(new ContractQuery());
                getContractQuery().setContractId(getContract().getContractId());
                setContract(SCAWrapper.getUserSpecificInstance().getContracts(getContractQuery()).getContracts().get(0));
                getPhotographs().addAll(getContract().getContractDocuments());
                writeCustomerPhotographsDataToFile(getPhotographs());
            } else if (getCustomer() != null) {
                setCustomerQuery(new CustomerQuery());
                getCustomerQuery().setCustomerId(getCustomer().getCustomerId());
                getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);
                setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));

                getPhotographs().addAll(getCustomer().getCustomerPhotographs());
                writeCustomerPhotographsDataToFile(getPhotographs());

            } else if (getOrganisation() != null) {
                setOrganisationQuery(new OrganisationQuery());
                getOrganisationQuery().setOrganisationId(getOrganisation().getOrganisationId());
                getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN_PHOTO);

                setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
                getPhotographs().addAll(getOrganisation().getOrganisationPhotographs());
                writeCustomerPhotographsDataToFile(getPhotographs());
            }

        } catch (SCABusinessError e) {
        }

        return getDDForwardResolution("/photograph/photographs_snippet.jsp");
    }

    public Resolution retrieveStickyNoteListSnippet() {
        try {
            setStickyNoteList(new StickyNoteList());
            setStickyNoteEntityIdentifier(new StickyNoteEntityIdentifier());
            getStickyNoteEntityIdentifier().setEntityId(getCustomer().getCustomerId());
            getStickyNoteEntityIdentifier().setEntityType("Customer");

            StickyNoteList snl = SCAWrapper.getUserSpecificInstance().getEntitiesStickyNotes(getStickyNoteEntityIdentifier());
            StickyNoteTypeList sntl = SCAWrapper.getUserSpecificInstance().getStickyNoteTypeList(makeSCAString(getStickyNoteEntityIdentifier().getEntityType()));

            for (StickyNote sn : snl.getStickyNotes()) {
                for (StickyNoteType snt : sntl.getStickyNoteTypes()) {
                    if (snt.getDisplayPriority() == 1 && sn.getTypeName().equals(snt.getTypeName())) {
                        getStickyNoteList().getStickyNotes().add(sn);
                    }
                }
            }

            return new ForwardResolution("/note/note_xmlhttp.jsp");

        } catch (SCABusinessError se) {
            setStickyNoteList(null);
            return new ForwardResolution("/note/note_xmlhttp.jsp");
        } catch (Exception ex) {
            setStickyNoteList(null);
            return new ForwardResolution("/note/note_xmlhttp.jsp");
        }
    }

    public Resolution retrieveTTViaStream() {
        return new ForwardResolution(TroubleTicketActionBean.class, "retrieveTTViaStream").addParameter("customer.customerId", getCustomer().getCustomerId());
    }

    /**
     *
     * ORGANISATION FUNCTIONALITY
     *
     */
    public Resolution showSearchOrganisation() {
        return getDDForwardResolution("/organisation/search_organisation.jsp");
    }

    @DontValidate()
    public Resolution searchOrganisation() {
        checkPermissions(Permissions.VIEW_ORGANISATION);

        clearValidationErrors(); // In case we got here from the organisation id screen and no id was captured
        try {
            getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN);
            setOrganisationList(SCAWrapper.getUserSpecificInstance().getOrganisations(getOrganisationQuery()));
            if (getOrganisationList().getNumberOfOrganisations() <= 0) {
                localiseErrorAndAddToGlobalErrors("no.records.found");
            }
        } catch (SCABusinessError e) {
            showSearchOrganisation();
            throw e;
        }
        return getDDForwardResolution("/organisation/search_organisation.jsp");
    }

    public Resolution showAddOrganisationWizard() {
        checkPermissions(Permissions.ADD_ORGANISATION);
        return getDDForwardResolution("/organisation/add_organisation_capture_basic_details.jsp");
    }

    @DontValidate()
    public Resolution retrieveOrganisation() {
        checkPermissions(Permissions.VIEW_ORGANISATION);

        if (getOrganisationQuery() == null && getOrganisation() != null) {
            setOrganisationQuery(new OrganisationQuery());
            getOrganisationQuery().setOrganisationId(getOrganisation().getOrganisationId());
            getOrganisationQuery().setResultLimit(1);
        }

        if (getOrganisationQuery() == null) {
            return showSearchOrganisation();
        }
        getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN_ROLES);
        getOrganisationQuery().setRolesResultLimit(BaseUtils.getIntProperty("env.sep.pagesize", 50));
        if (getOrganisationQuery().getRolesOffset() == null) {
            getOrganisationQuery().setRolesOffset(0);
        }
        try {
            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
        } catch (SCABusinessError sbe) {
            localiseErrorAndAddToGlobalErrors("no.records.found");
            return showSearchOrganisation();
        }

        if (getProductInstanceQuery() == null) {
            setProductInstanceQuery(new ProductInstanceQuery());
        }
        getProductInstanceQuery().setResultLimit(BaseUtils.getIntProperty("env.sep.pagesize", 50));

        if (getProductInstanceQuery().getOffset() == null) {
            getProductInstanceQuery().setOffset(0);
        }
        getProductInstanceQuery().setOrganisationId(getOrganisation().getOrganisationId());
        getProductInstanceQuery().setVerbosity(StProductInstanceLookupVerbosity.MAIN);
        setProductInstanceList(SCAWrapper.getUserSpecificInstance().getProductInstances(getProductInstanceQuery()));
        
        return getDDForwardResolution("/organisation/view_organisation.jsp");
    }
    
    public Resolution previousOrgProductInstancePage() {
        return retrieveOrganisation();
    }

    public Resolution nextOrgProductInstancePage() {
        return retrieveOrganisation();
    }

    public Resolution previousOrgRolesPage() {
        return retrieveOrganisation();
    }

    public Resolution nextOrgRolesPage() {
        return retrieveOrganisation();
    }

    public Resolution showChangeOrganisationsAccountManagerCustomer() {
        checkPermissions(Permissions.EDIT_ORGANISATION);
        getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN);
        setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));

        setAccountManagerCustomerList(new HashMap<Integer, String>());

        Organisation org = UserSpecificCachedDataHelper.getOrganisation(1, StOrganisationLookupVerbosity.MAIN_ROLES); // Smile Organisation is 1. Only Smile staff can be account manager of an Organisation
        for (CustomerRole roleInOrg : org.getCustomerRoles()) {
            getAccountManagerCustomerList().put(roleInOrg.getCustomerId(), roleInOrg.getCustomerName());
        }

        return getDDForwardResolution("/organisation/change_account_manager.jsp");
    }

    public Resolution changeOrganisationsAccountManagerCustomer() {
        checkPermissions(Permissions.EDIT_ORGANISATION);
        OrganisationQuery q = new OrganisationQuery();

        q.setOrganisationId(getOrganisation().getOrganisationId());
        q.setResultLimit(1);
        q.setVerbosity(StOrganisationLookupVerbosity.MAIN);

        Organisation tmpOrganisation = SCAWrapper.getUserSpecificInstance().getOrganisation(q);

        tmpOrganisation.setAccountManagerCustomerProfileId(getOrganisation().getAccountManagerCustomerProfileId());
        tmpOrganisation.setVersion(getOrganisation().getVersion());

        SCAWrapper.getUserSpecificInstance().modifyOrganisation(tmpOrganisation);
        if (getOrganisation() != null) {
            setPageMessage("account.manager.change.success");
        }
        return retrieveOrganisation();
    }

    public Resolution showManageCustomerRoles() {
        checkPermissions(Permissions.ADD_CUSTOMER_ROLES_TO_ORGANISATION);
        if (getCustomerQuery() == null && getCustomer() != null) {
            setCustomerQuery(new CustomerQuery());
            getCustomerQuery().setCustomerId(getCustomer().getCustomerId());
            getCustomerQuery().setResultLimit(1);
        }
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        try {
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
        } catch (SCABusinessError e) {
            retrieveCustomer();
            throw e;
        }

        return getDDForwardResolution("/customer/manage_customer_roles.jsp");
    }

    public Resolution deleteCustomerRole() {
        checkPermissions(Permissions.ADD_CUSTOMER_ROLES_TO_ORGANISATION);
        try {
            CustomerQuery q = new CustomerQuery();
            q.setCustomerId(getCustomer().getCustomerId());
            q.setResultLimit(1);
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);

            Customer tmpCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(q);
            tmpCustomer.getCustomerRoles().clear();
            tmpCustomer.getCustomerRoles().addAll(getCustomer().getCustomerRoles());

            SCAWrapper.getUserSpecificInstance().modifyCustomer(tmpCustomer);
            if (getCustomer() != null) {
                setPageMessage("customer.role.deleted.successfully");
            }
        } catch (SCABusinessError e) {
            showManageCustomerRoles();
            throw e;
        }

        return showManageCustomerRoles();
    }

    public Resolution showAddCustomerRole() {
        checkPermissions(Permissions.ADD_CUSTOMER_ROLES_TO_ORGANISATION);
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        try {
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
        } catch (SCABusinessError e) {
            retrieveCustomer();
            throw e;
        }
        return getDDForwardResolution("/customer/add_customer_role.jsp");
    }

    public Resolution searchOrganisationCustomerRole() {  //done, awaiting tests      
        checkPermissions(Permissions.VIEW_ORGANISATION);

        clearValidationErrors();
        try {
            getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
            getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN);
            setOrganisationList(SCAWrapper.getUserSpecificInstance().getOrganisations(getOrganisationQuery()));
            if (getOrganisationList().getNumberOfOrganisations() <= 0) {
                localiseErrorAndAddToGlobalErrors("no.records.found");
            }
        } catch (SCABusinessError e) {
            showSearchOrganisation();
            throw e;
        }
        return getDDForwardResolution("/customer/add_customer_role.jsp");
    }

    public Resolution setOrganisationAndShowRoleField() {
        checkPermissions(Permissions.VIEW_ORGANISATION);

        if (getOrganisationQuery() == null && getOrganisation() != null) {
            setOrganisationQuery(new OrganisationQuery());
            getOrganisationQuery().setOrganisationId(getOrganisation().getOrganisationId());
            getOrganisationQuery().setResultLimit(1);
        }

        if (getOrganisationQuery() == null) {
            return showAddCustomerRole();
        }
        getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN);
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        try {
            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
        } catch (SCABusinessError sbe) {
            localiseErrorAndAddToGlobalErrors("no.records.found");
            return showAddCustomerRole();
        }

        return getDDForwardResolution("/customer/add_customer_role_name.jsp");
    }

    public Resolution updateCustomerRoles() {
        checkPermissions(Permissions.ADD_CUSTOMER_ROLES_TO_ORGANISATION);
        try {
            CustomerQuery q = new CustomerQuery();
            q.setCustomerId(getCustomer().getCustomerId());
            q.setResultLimit(1);
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);

            Customer tmpCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(q);
            tmpCustomer.getCustomerRoles().add(getCustomerRole());
            if (tmpCustomer.getCustomerId() == getUserCustomerIdFromSession()) {
                checkPermissions(Permissions.ADD_ONESELF_TO_ORGANISATION);
            }
            SCAWrapper.getUserSpecificInstance().modifyCustomer(tmpCustomer);
            if (getCustomer() != null) {

                setPageMessage("customer.updated.successfully");
            }
        } catch (SCABusinessError e) {
            showManageCustomerRoles();
            throw e;
        }
        
        return retrieveCustomer();
    }

    public Resolution showEditOrganisation() {
        checkPermissions(Permissions.EDIT_ORGANISATION);
        OrganisationQuery q = new OrganisationQuery();
        q.setVerbosity(StOrganisationLookupVerbosity.MAIN);
        q.setOrganisationId(getOrganisation().getOrganisationId());
        q.setResultLimit(1);

        try {
            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(q));
        } catch (SCABusinessError e) {
            retrieveOrganisation();
            throw e;
        }

        return getDDForwardResolution("/organisation/edit_organisation.jsp");
    }

    public Resolution modifyOrganisation() {
        checkPermissions(Permissions.EDIT_ORGANISATION);
        try {
            OrganisationQuery q = new OrganisationQuery();

            q.setOrganisationId(getOrganisation().getOrganisationId());
            q.setResultLimit(1);
            q.setVerbosity(StOrganisationLookupVerbosity.MAIN);

            Organisation tmpOrganisation = SCAWrapper.getUserSpecificInstance().getOrganisation(q);

            getOrganisation().getModificationRoles().clear();

            getOrganisation().getModificationRoles().addAll(getOrganisation().getModificationRoles());

            getOrganisation().setVersion(tmpOrganisation.getVersion());

            SCAWrapper.getUserSpecificInstance().modifyOrganisation(getOrganisation());
            setPageMessage("organisation.updated.successfully");
        } catch (SCABusinessError e) {
            showEditOrganisation();
            throw e;
        }
        return retrieveOrganisation();
    }

    public Resolution showEditorganisationAddress() {
        checkPermissions(Permissions.EDIT_ORGANISATION_ADDRESS);
        return getDDForwardResolution("/organisation/manage_organisation_addresses.jsp");
    }

    public Resolution showChangeModificationRoles() {
        checkPermissions(Permissions.EDIT_ORGANISATION_PERMISSIONS);

        getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN);
        getOrganisationQuery().setOrganisationId(getOrganisation().getOrganisationId());
        getOrganisationQuery().setResultLimit(1);
        try {
            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));

        } catch (SCABusinessError e) {
            retrieveOrganisation();
            throw e;
        }

        return getDDForwardResolution("/organisation/edit_modification_roles.jsp");
    }

    public Resolution updatePhotographs() {
        if (getCustomer() != null) {
            checkPermissions(Permissions.EDIT_CUSTOMER_PHOTOS);
            CustomerQuery q = new CustomerQuery();
            q.setCustomerId(getCustomer().getCustomerId());
            q.setResultLimit(1);
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);

            Customer tmpCust = SCAWrapper.getUserSpecificInstance().getCustomer(q);

            tmpCust.getCustomerPhotographs().clear();

            tmpCust.getCustomerPhotographs().addAll(setCustomerPhotographsData(getPhotographs()));
            if (isDocumentsForDiplomat(getPhotographs())) {
                tmpCust.setClassification("diplomat");
            }
            if (log.isDebugEnabled()) {
                log.debug("Number of photographs is [{}]", getPhotographs().size());
                for (Photograph photo : getPhotographs()) {
                    log.debug("Photo GUID [{}] Photo Type [{}] Data length [{}]", new Object[]{photo.getPhotoGuid(), photo.getPhotoType(), photo.getData().length()});
                }
            }
            try {
                SCAWrapper.getUserSpecificInstance().modifyCustomer(tmpCust);
                if (getIsDeliveryPerson()) {
                    setPageMessage("delivery.kyc.complete", tmpCust.getAlternativeContact1());
                } else {
                    setPageMessage("customer.updated.successfully");
                }
            } catch (SCABusinessError e) {
                showEditPhotographs();
                throw e;
            }
            return retrieveCustomer();
        } else {
            checkPermissions(Permissions.EDIT_ORGANISATION_PHOTOS);
            try {
                OrganisationQuery q = new OrganisationQuery();
                q.setOrganisationId(getOrganisation().getOrganisationId());
                q.setResultLimit(1);
                q.setVerbosity(StOrganisationLookupVerbosity.MAIN_PHOTO);

                Organisation tmpOrg = SCAWrapper.getUserSpecificInstance().getOrganisation(q);

                tmpOrg.getOrganisationPhotographs().clear();
                tmpOrg.getOrganisationPhotographs().addAll(setCustomerPhotographsData(getPhotographs()));

                SCAWrapper.getUserSpecificInstance().modifyOrganisation(tmpOrg);
                setPageMessage("organisation.updated.successfully");

            } catch (SCABusinessError e) {
            }
            return retrieveOrganisation();
        }
    }

    public Resolution showEditPhotographs() {
        try {
            if (getCustomer() != null) {
                checkPermissions(Permissions.EDIT_CUSTOMER_PHOTOS);
                setCustomerQuery(new CustomerQuery());
                getCustomerQuery().setCustomerId(getCustomer().getCustomerId());
                getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);
                setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
                if (getCustomer().getSCAContext().getObviscated() != null && getCustomer().getSCAContext().getObviscated().equals("ob")) {
                    if (getIsDeliveryPerson() || getIsIndirectChannelPartner()) {
                        localiseErrorAndAddToGlobalErrors("not.pending.kyc");
                        return showSearchCustomer();
                    } else {
                        if (!isAllowed(BaseUtils.getProperty("env.staff.roles.to.be.obviscated"))) {
                            setPageMessage("not.your.customer");
                            return retrieveCustomer();
                        }
                    }
                }
                getPhotographs().addAll(getCustomer().getCustomerPhotographs());
                writeCustomerPhotographsDataToFile(getPhotographs());

            } else if (getOrganisation() != null) {
                checkPermissions(Permissions.EDIT_ORGANISATION_PHOTOS);
                setOrganisationQuery(new OrganisationQuery());
                getOrganisationQuery().setOrganisationId(getOrganisation().getOrganisationId());
                getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN_PHOTO);

                setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
                getPhotographs().addAll(getOrganisation().getOrganisationPhotographs());

                writeCustomerPhotographsDataToFile(getPhotographs());
            }

        } catch (SCABusinessError e) {
        }
        return getDDForwardResolution("/photograph/edit_photographs.jsp");
    }

    public Resolution ChangeModificationRoles() {
        checkPermissions(Permissions.EDIT_ORGANISATION_PERMISSIONS);
        String newRight = optionTransfer.getNewRight();
        String[] roles = newRight.split(",");
        int userGroupsLength = roles.length;
        // Get existing data
        OrganisationQuery q = new OrganisationQuery();
        q.setOrganisationId(getOrganisation().getOrganisationId());
        q.setResultLimit(1);
        q.setVerbosity(StOrganisationLookupVerbosity.MAIN);

        try {
            Organisation tmpOrganisation = SCAWrapper.getUserSpecificInstance().getOrganisation(q);
            tmpOrganisation.getModificationRoles().clear();
            tmpOrganisation.getAddresses().clear(); //TODO Mental Note

            tmpOrganisation.setVersion(getOrganisation().getVersion());
            for (int i = 0; i < userGroupsLength; i++) {
                tmpOrganisation.getModificationRoles().add(roles[i]);
            }
            SCAWrapper.getUserSpecificInstance().modifyOrganisation(tmpOrganisation);
            if (tmpOrganisation.getModificationRoles().size() > 1) {
                setPageMessage("organisation.updated.successfully");
            }
        } catch (SCABusinessError e) {
            retrieveOrganisation();
            throw e;
        }

        return retrieveOrganisation();
    }

    @DontValidate
    public Resolution getCustomerJSON() {
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
        getCustomerQuery().setResultLimit(1);
        Customer c = SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery());
        return getJSONResolution(c);
    }
    private CustomerRole customerRole;

    public CustomerRole getCustomerRole() {
        return customerRole;
    }

    public void setCustomerRole(CustomerRole customerRole) {
        this.customerRole = customerRole;

    }

    public Resolution showSendBulkEmail() {
        checkPermissions(Permissions.BULK_EMAIL);
        return getDDForwardResolution("/customer/bulk_email.jsp");
    }

    public Resolution showSendBulkSMS() {
        checkPermissions(Permissions.BULK_SMS);
        return getDDForwardResolution("/customer/bulk_sms.jsp");
    }

    public Resolution showSendBulkNotification() {
        checkPermissions(Permissions.BULK_SMS);
        return getDDForwardResolution("/customer/bulk_notification.jsp");
    }

    public Resolution showSendBulkSMSSent() {
        checkPermissions(Permissions.BULK_SMS);
        return getDDForwardResolution("/customer/bulk_sms_sent.jsp");
    }

    public Resolution showSendBulkNotificationSent() {
        checkPermissions(Permissions.BULK_SMS);
        return getDDForwardResolution("/customer/bulk_notification_sent.jsp");
    }

    public Resolution sendBulkEmail() {
        checkPermissions(Permissions.BULK_EMAIL);
        final String[] rows;
        String list = getParameter("emailList");
        if (getGeneralQueryRequest().getQueryName().equals("Just Me") && (list == null || list.isEmpty())) {
            CustomerQuery query = new CustomerQuery();
            query.setSSOIdentity(getUser());
            query.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            query.setResultLimit(1);
            Customer loggedInCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(query);
            // Add header row and then logged in customers details
            rows = new String[]{"EMAIL^FIRST_NAME", loggedInCustomer.getEmailAddress() + "^" + loggedInCustomer.getFirstName()};
        } else if (list != null && !list.isEmpty()) {
            list = "EMAIL^FIRST_NAME" + "\r\n" + list;
            log.debug("EMAIL List is [{}]", list);
            rows = list.split("\r\n");
            //rows = new String[]{"EMAIL^FIRST_NAME","jenny.bashala@smilecoms.com^Jenny", "sabelo.dlangamandla@smilecoms.com^Sabelo"};
        } else {
            GeneralQueryResponse resp = SCAWrapper.getUserSpecificInstance().runGeneralQuery(getGeneralQueryRequest());
            String result = Utils.unzip(resp.getBase64CompressedResult());
            rows = result.split("#");
        }

        Event event = new Event();
        event.setEventType("BULK");
        event.setEventSubType("EMAIL");
        event.setEventKey(getUser());
        if (list != null && !list.isEmpty()) {
            event.setEventData(getUser() + " submitted a list which has " + (rows.length) + " rows to receive Email subject " + getParameter("subject"));
        } else {
            event.setEventData(getUser() + " submitted " + getGeneralQueryRequest().getQueryName() + " which has " + rows.length + " rows to receive Email subject " + getParameter("subject"));
        }

        SCAWrapper.getAdminInstance().createEvent(event);

        new Thread(new Runnable() {
            @Override
            public void run() {
                submitEmails(rows, getParameter("subject"), getParameter("body"), getRequest().getSession());
            }
        }).start();

        setPageMessage("bulk.emails.submitted");
        return showSendBulkEmail();
    }

    public Resolution sendBulkSMS() {
        checkPermissions(Permissions.BULK_SMS);
        if (notConfirmed()) {
            return confirm();
        }
        final Set<String> rows = new LinkedHashSet<>();
        String list = getParameter("smsList");
        if (getGeneralQueryRequest().getQueryName().equals("Just Me") && (list == null || list.isEmpty())) {
            Set<String> impus = getCustomersIMPUs(getUserCustomerIdFromSession());
            CustomerQuery query = new CustomerQuery();
            query.setSSOIdentity(getUser());
            query.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            query.setResultLimit(1);
            Customer loggedInCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(query);
            // Add header row and then logged in customers details
            int cnt = 0;
            rows.add("IMPU^FIRST_NAME");
            for (String impu : impus) {
                cnt++;
                rows.add(impu + "^" + loggedInCustomer.getFirstName());
            }
        } else if (list != null && !list.isEmpty()) {
            list = "IMPU^FIRST_NAME" + "\r\n" + list;
            log.debug("SMS List is [{}]", list);
            rows.addAll(Arrays.asList(list.split("\r\n")));
        } else {
            GeneralQueryResponse resp = SCAWrapper.getUserSpecificInstance().runGeneralQuery(getGeneralQueryRequest());
            String result = Utils.unzip(resp.getBase64CompressedResult());
            rows.addAll(Arrays.asList(result.split("#")));
        }

        Event event = new Event();
        event.setEventType("BULK");
        event.setEventSubType("SMS");
        event.setEventKey(getUser());
        if (list != null && !list.isEmpty()) {
            event.setEventData(getUser() + " submitted a list which has " + (rows.size() - 1) + " rows to receive SMS " + getParameter("body") + " from " + getParameter("from"));
        } else {
            event.setEventData(getUser() + " submitted " + getGeneralQueryRequest().getQueryName() + " which has " + (rows.size() - 1) + " rows to receive SMS " + getParameter("body") + " from " + getParameter("from"));
        }
        SCAWrapper.getAdminInstance().createEvent(event);

        new Thread(new Runnable() {
            @Override
            public void run() {
                submitSMSs(getParameter("campaignName"), getParameter("from"), rows, getParameter("body"), getRequest().getSession());
            }
        }).start();

        setPageMessage("bulk.smss.submitted");
        //return showSendBulkSMS();
        return showSendBulkSMSSent();
    }

    public Resolution sendBulkNotification() {
        log.debug("called sendBulkNotification");
        checkPermissions(Permissions.BULK_SMS);
        if (notConfirmed()) {
            return confirm();
        }

        String customerList = getParameter("customerList");
        String[] customerIds = customerList.split("\\r\\n");
        String title = getParameter("notificationTitle");
        String type = getParameter("notificationType");
        String image = getParameter("notificationImage");
        String notificationBody = getParameter("notificationBody");
        for (String cid : customerIds) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    submitNotifications(cid, notificationBody, title, image, type);
                }
            }).start();
        }
        setPageMessage("bulk.notifications.submitted");
        //return showSendBulkSMS();
        return showSendBulkNotificationSent();
    }

    public String getBulkEmailStatus() {
        String result = (String) getRequest().getSession().getAttribute(EMAIL_PROGRESS_SESSION_KEY);
        if (result != null) {
            return result;
        } else {
            return "No Emails in Progress";
        }
    }

    public String getBulkSMSStatus() {
        String result = (String) getRequest().getSession().getAttribute(SMS_PROGRESS_SESSION_KEY);
        if (result != null) {
            return result;
        } else {
            return "No SMS's in Progress";
        }
    }

    public String getBulkNotificationStatus() {
        String result = (String) getRequest().getSession().getAttribute(SMS_PROGRESS_SESSION_KEY);
        if (result != null) {
            return result;
        } else {
            return "No Notifications in Progress";
        }
    }

    public Resolution showChangeNiraPassword() {
        checkPermissions(Permissions.RESET_NIRA_PASSWORD);
        return getDDForwardResolution("/nira/reset_nira_password.jsp");
    }

    String niraNewPassword = null;
    String niraPasswordChangeResponse = null;

    public java.lang.String getNiraPasswordChangeResponse() {
        return niraPasswordChangeResponse;
    }

    public void setNiraPasswordChangeResponse(java.lang.String niraPasswordChangeResponse) {
        this.niraPasswordChangeResponse = niraPasswordChangeResponse;
    }

    public java.lang.String getNiraNewPassword() {
        return niraNewPassword;
    }

    public void setNiraNewPassword(java.lang.String niraPassword) {
        this.niraNewPassword = niraPassword;
    }

    public Resolution doChangeNiraPassword() {
        checkPermissions(Permissions.RESET_NIRA_PASSWORD);

        NiraPasswordChange request = new NiraPasswordChange();

        request.setNewPassword(getNiraNewPassword());
        request.setSepUsername(getUser());

        NiraPasswordChangeReply response = SCAWrapper.getAdminInstance().changeNiraPassword_Direct(request);

        setNiraPasswordChangeResponse(response.getNiraResponse());

        return getDDForwardResolution("/nira/reset_nira_password.jsp");
    }

    public Resolution showSendCustomerEmail() {
        checkPermissions(Permissions.EMAIL);
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
        return getDDForwardResolution("/customer/send_email.jsp");
    }

    public Resolution sendCustomerEmail() {
        checkPermissions(Permissions.EMAIL);
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
        try {
            IMAPUtils.sendEmail(BaseUtils.getProperty("env.customercare.email.address"),
                    getCustomer().getEmailAddress(),
                    getParameter("subject"),
                    escapeCurlyBraces(getParameter("body")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setPageMessage("email.sent.successfully");
        return retrieveCustomer();
    }

    private String escapeCurlyBraces(String msg) {
        log.debug("before replacement [{}]", msg);

        String val = msg.replace("{", "'{'").replace("}", "'}'").replaceAll("'\\{'(\\d+)'\\}'", "{$1}");
        log.debug("After replacement [{}]", val);
        return val;
    }

    private void submitEmails(String[] rows, String subject, String body, HttpSession session) {
        log.debug("Starting email submission thead");
        if (session != null) {
            session.setMaxInactiveInterval(60 * 60 * 24); // Keep users session open for an entire day
        }
        int rowNum = 0;
        int rowcnt = rows.length - 1;
        boolean isHeader = true;
        String err = null;
        String emailAddress = null;
        String srcAddress = BaseUtils.getProperty("env.customercare.email.address");
        for (String row : rows) {
            if (isHeader) {
                // Skip column headers
                isHeader = false;
                continue;
            }
            try {
                log.debug("Email data row is [{}]", row);
                String[] entries = row.split("\\^", -1);
                emailAddress = entries[0];

                String bodyWithParamsReplaced = Utils.format(escapeCurlyBraces(body), (Object[]) entries);

                //replacement of unsubscribe inside message body
                if (BaseUtils.getBooleanProperty("env.customer.email.optout", true)) {
                    bodyWithParamsReplaced = unsubsLink(bodyWithParamsReplaced, emailAddress);
                }
                //end of replacement of unsubscribe
                log.debug("Email Body With Params replaced \n [{}]", bodyWithParamsReplaced);

                log.debug("Calling IMAPUtils to send email [{}] to address [{}]", bodyWithParamsReplaced, emailAddress);

                //New Adding opt in level checking option
                CustomerQuery RoCust = new CustomerQuery();
                RoCust.setEmailAddress(emailAddress);
                RoCust.setResultLimit(1);
                RoCust.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                Customer roCustom = null;
                try {
                    roCustom = SCAWrapper.getAdminInstance().getCustomer(RoCust);
                } catch (Exception ex) {
                    log.warn("Customer not found in SEP. Sending email", ex);
                    err = ex.toString() + " (To: " + emailAddress + ")";
                }
                //Don't send email to a customer if he has unsubscribed.                
                if (roCustom != null && (roCustom.getOptInLevel() & 4) == 0) {
                    log.debug("Customers opt in  level exludes marketing, so not sending campaign messages");
                    continue;
                }

                IMAPUtils.sendEmail(srcAddress,
                        emailAddress,
                        subject,
                        bodyWithParamsReplaced);
                log.debug("Called IMAPUtils to send email to address [{}]", emailAddress);
            } catch (Exception e) {
                log.warn("Error sending bulk email", e);
                err = e.toString() + " (To: " + emailAddress + ")";
            } finally {
                rowNum++;
                if (session == null) {
                    log.warn("Session is null");
                } else {
                    try {
                        if (err != null) {
                            session.setAttribute(EMAIL_PROGRESS_SESSION_KEY, "Submitting " + rowNum + " of " + rowcnt + ". Last Error: " + err);
                        } else {
                            session.setAttribute(EMAIL_PROGRESS_SESSION_KEY, "Submitting " + rowNum + " of " + rowcnt);
                        }
                    } catch (Exception e) {
                        log.warn("Error setting attribute in session [{}]", e.toString());
                    }
                }
            }
        }
        if (session != null) {
            session.removeAttribute(EMAIL_PROGRESS_SESSION_KEY);
        }
        log.debug("Ending email submission thead");
    }

    private void submitNotifications(String customerId, String messageBody, String title, String imageLink, String type) {
        log.debug("called submitNotifications [{}], [{}], [{}], [{}], [{}]", customerId, messageBody, title, imageLink, type);
        if (type == null || type.isEmpty()) {
            return;
        }
        String[] types = type.split("\\.");
        Map<String, String> data = new HashMap<>();
        data.put("type", types[0]);
        if (types.length > 1) {
            data.put("info", types[1]);
        }
        log.debug("data is :" + data);
        SelfcareService scs = new SelfcareService();
        String notificationId = scs.sendPushNotification(Integer.valueOf(customerId.trim()), messageBody, title, imageLink, null, data);
        log.debug("notification message sent with ID [{}]", notificationId);
    }

    private void submitSMSs(String campaignName, String from, Set<String> rows, String body, HttpSession session) {
        log.debug("Starting sms submission thead");
        if (session != null) {
            session.setMaxInactiveInterval(60 * 60 * 24); // Keep users session open for an entire day
        }
        int rowNum = 0;
        int rowcnt = rows.size() - 1;
        boolean isHeader = true;
        String err = null;
        String impu = null;
        for (String row : rows) {
            if (isHeader) {
                // Skip column headers
                isHeader = false;
                continue;
            }
            try {
                log.debug("SMS data row is [{}]", row);
                String[] entries = row.split("\\^", -1);
                impu = entries[0];

                /*    //Optin addition
             ServiceInstanceQuery q_cust = new ServiceInstanceQuery();                     
             q_cust.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
             //
             q_cust.setIdentifier(Utils.getPublicIdentityForPhoneNumber(impu));
             setServiceInstance(SCAWrapper.getAdminInstance().getServiceInstance(q_cust));
             //
                           
              CustomerQuery qcust = new CustomerQuery();
              qcust.setCustomerId(q_cust.getCustomerId());     
              qcust.setResultLimit(1);
              qcust.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
              Customer impuCustomer = SCAWrapper.getAdminInstance().getCustomer(qcust);
                            
              if ((impuCustomer.getOptInLevel() & 4) == 0) {
                log.debug("Customers opt in  level exludes marketing so not sending campaign messages");
                continue;
              }*/
                String bodyWithParamsReplaced = Utils.format(escapeCurlyBraces(body), (Object[]) entries);

                log.debug("SMS Body With Params replaced \n [{}]", bodyWithParamsReplaced);

                log.debug("Calling SCA to send SMS [{}] to IMPU [{}]", bodyWithParamsReplaced, impu);
                ShortMessage sm = new ShortMessage();
                sm.setFrom(from);
                sm.setTo(impu);
                sm.setCampaignId(campaignName);
                sm.setBody(bodyWithParamsReplaced);
                SCAWrapper.getAdminInstance().sendShortMessage(sm);
                log.debug("Called SCA to send SMS to IMPU [{}]", impu);
            } catch (Exception e) {
                log.warn("Error sending bulk SMS", e);
                err = e.toString() + " (To: " + impu + ")";
            } finally {
                rowNum++;
                if (session == null) {
                    log.warn("Session is null");
                } else {
                    try {
                        if (err != null) {
                            session.setAttribute(SMS_PROGRESS_SESSION_KEY, "Submitting " + rowNum + " of " + rowcnt + ". Last Error: " + err);
                        } else {
                            session.setAttribute(SMS_PROGRESS_SESSION_KEY, "Submitting " + rowNum + " of " + rowcnt);
                        }
                    } catch (Exception e) {
                        log.warn("Error setting attribute in session [{}]", e.toString());
                    }
                }
            }
        }
        if (session != null) {
            session.removeAttribute(SMS_PROGRESS_SESSION_KEY);
        }
        log.debug("Ending SMS submission thead");
    }

    private Set<String> getCustomersIMPUs(int customerProfileId) {
        Set<String> impus = new HashSet<>();
        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(customerProfileId);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP);
        Customer cust = SCAWrapper.getAdminInstance().getCustomer(q);

        // Build up an xml doc of the phone numbers and activation codes for each
        for (ProductInstance pi : cust.getProductInstances()) {
            boolean foundOne = false;
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = m.getServiceInstance();
                if (si.getCustomerId() == customerProfileId) {
                    log.debug("Looking at service instance id [{}]", si.getServiceInstanceId());
                    for (AVP avp : si.getAVPs()) {
                        log.debug("Looking at AVP [{}]", avp.getAttribute());
                        if (avp.getAttribute().equals("PublicIdentity")) {
                            log.debug("Found publicIdentity [{}]", avp.getValue());
                            impus.add(avp.getValue());
                            foundOne = true;
                            break;
                        }
                    }
                }
                if (foundOne) {
                    break;
                }
            }
        }
        return impus;
    }

    private void createTicket(String subCategory, String summary, String description, Customer c, boolean close) {
        try {
            NewTTIssue tt = new NewTTIssue();

            tt.setCustomerId(String.valueOf(c.getCustomerId()));
            tt.setMindMapFields(new MindMapFields());
            JiraField f = new JiraField();

            f.setFieldName("Description");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue(description);
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("Issue Type");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("Customer Incident");
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("Summary");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue(summary);
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("Project");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue(BaseUtils.getProperty("env.jira.customer.care.project.key"));
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("SEP Reporter");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("admin admin");
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("Incident Channel");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("System");
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("Category");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("KYC Data Verification");
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("Sub Category");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue(subCategory);
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("Smile Customer Location");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue(c.getAddresses().get(0).getState() + " :: " + c.getAddresses().get(0).getZone());
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("Smile Customer Phone");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue(c.getAlternativeContact1());
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("Smile Customer Name");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue(c.getFirstName() + " " + c.getLastName());
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("Smile Customer Email");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue(c.getEmailAddress());
            tt.getMindMapFields().getJiraField().add(f);

            if (close) {
                f = new JiraField();
                f.setFieldName("Status");
                f.setFieldType("TT_FIXED_FIELD");
                f.setFieldValue("true");
                tt.getMindMapFields().getJiraField().add(f);

            }
            SCAWrapper.getUserSpecificInstance().createTroubleTicketIssue(tt);
        } catch (Exception ex) {
            log.warn("Failed to create ticket for KYC Data capture: ", ex);
        }
    }

    //Replacement of unsubscribe word under message body
    private String unsubsLink(String msg, String emailAddress) {
        String linkToUnsub = BaseUtils.getProperty("env.sra.campaign.direct.optout");
        linkToUnsub = linkToUnsub.replace("email=*", "email=" + emailAddress);
        msg = msg.replace("_unsubscribe_", linkToUnsub);
        return msg;
    }

    //Quick view of verified KYC customers on SEP
    public Resolution quickViewKYC() {
        return getDDForwardResolution("/customer/kyc_quick_view.jsp");
    }

    public Resolution displayQuickViewKYC() {
        checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_VERIFIED);

        //new for input customer list
        Set<String> listCustId = new LinkedHashSet<>();
        String custIdList = getParameter("customerIdList");
        //Set<Long> customerIds = new HashSet<>();
        setCustomerList(new CustomerList());

        if (custIdList != null && !custIdList.isEmpty()) {
            clearValidationErrors();
            log.debug("Customer ID List is [{}]", custIdList);
            listCustId.addAll(Arrays.asList(custIdList.split("\r\n")));
            int count = 0;
            for (String custId : listCustId) {
                //customerIds.add(Long.parseLong(accou));                
                count++;
                CustomerQuery cq = new CustomerQuery();
                cq.setCustomerId(Integer.parseInt(custId.trim()));
                cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO_ADDRESS);
                cq.setResultLimit(1);
                getCustomerList().getCustomers().add(SCAWrapper.getUserSpecificInstance().getCustomers(cq).getCustomers().get(0));
                if (count >= BaseUtils.getIntProperty("env.sep.kyc.validate.rows", 10)) {
                    break;
                }
            }

        }

        //getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);
        //getCustomerQuery().setResultLimit(BaseUtils.getIntProperty("env.sep.kyc.validate.rows", 10));
        //setCustomerList(SCAWrapper.getUserSpecificInstance().getCustomers(getCustomerQuery()));
        /* End of list generation*/
        lastModifiedBy = new HashMap<>();
        for (Customer c : getCustomerList().getCustomers()) {
            List<Photograph> justPhotos = new ArrayList<>();
            for (Photograph p : c.getCustomerPhotographs()) {
                if (!p.getPhotoType().equals("fingerprints")) {
                    String fileName = p.getPhotoGuid();
                    try {
                        Utils.createTempFile(fileName, Utils.decodeBase64(p.getData()));
                        justPhotos.add(p);
                    } catch (Exception e) {
                        log.warn("Error in writeCustomerPhotographsDataToFile", e);
                    }
                }
            }
            c.getCustomerPhotographs().clear();
            c.getCustomerPhotographs().addAll(justPhotos);
            lastModifiedBy.put(c.getCustomerId(), getLastModifiedBySalesPerson(c).getCustomerId());
        }

        return getDDForwardResolution("/customer/kyc_quick_view_display.jsp");

    }

    /**
     * Service instance AVPs just have the name and value in them. This function
     * gets the service spec of the SI and copies the other AVP info into them
     */
    private void populateAVPDetailsIntoServiceInstanceAVPs(ServiceInstance si) {
        ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(si.getServiceSpecificationId());
        for (AVP siAVP : si.getAVPs()) {
            for (AVP ssAVP : ss.getAVPs()) {
                if (siAVP.getAttribute().equals(ssAVP.getAttribute())) {
                    siAVP.setInputType(ssAVP.getInputType());
                    siAVP.setProvisionRoles(ssAVP.getProvisionRoles());
                    siAVP.setTechnicalDescription(ssAVP.getTechnicalDescription());
                    siAVP.setUserDefined(ssAVP.isUserDefined());
                    siAVP.setValidationRule(ssAVP.getValidationRule());
                    if (siAVP.getValue() == null) {
                        siAVP.setValue(ssAVP.getValue());
                    }
                    break;
                }
            }
        }
        // Add any missing AVPs that are in the spec but not on the SI
        for (AVP ssAVP : ss.getAVPs()) {
            boolean found = false;
            for (AVP siAVP : si.getAVPs()) {
                if (siAVP.getAttribute().equals(ssAVP.getAttribute())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                log.debug("Adding attribute [{}]", ssAVP.getAttribute());
                si.getAVPs().add(ssAVP);
            }
        }
    }

    public Resolution showCustomersOtherMNO() {
        checkPermissions(Permissions.VIEW_CUSTOMER_OTHER_MNO);
        return getDDForwardResolution("/customer/get_customer_details_other_mno.jsp");
    }

    public Resolution getCustomersOtherMNO() {
        checkPermissions(Permissions.VIEW_CUSTOMER_OTHER_MNO);
        try {
            setCustomerQueryOtherMNOResponse(RestServices.getCustomerDetailsOtherMNO(getCustomerQueryOtherMNORequest()));
        } catch (Exception ex) {
            log.error("Error in getCustomersOtherMNO():" + ex);
        }

        return getDDForwardResolution("/customer/get_customer_details_other_mno.jsp");
    }

    private boolean isDocumentsForDiplomat(List<Photograph> photographs) {
        for (Photograph ph : photographs) {
            if ("diplomatid".equalsIgnoreCase(ph.getPhotoType()) || "foreignaffairsletter".equalsIgnoreCase(ph.getPhotoType())) {
                return true;
            }
        }
        return false;
    }

    public Resolution verifyExistingCustomerWithNIDAOrImmigration() {
        checkPermissions(Permissions.ADD_CUSTOMER);
        setCustomerQuery(new CustomerQuery());
        getCustomerQuery().setCustomerId(getCustomer().getCustomerId());
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));

        setBiometricKyc(true);
        setVerifyExistingCustomerWithNIDAOrImmigration(true);

        return getDDForwardResolution("/customer/add_customer_select_customer_type.jsp");
    }

    public void getCustomerWithPhoto() {
        setCustomerQuery(new CustomerQuery());
        getCustomerQuery().setCustomerId(getCustomer().getCustomerId());
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));

        // Check if customer has fingerprints.
        Photograph fingerPrint = null;
        for (Photograph photo : getCustomer().getCustomerPhotographs()) {
            if (photo.getPhotoType().equalsIgnoreCase("fingerprints")) {
                fingerPrint = photo;
            }
        }
        //Clear all photos, and only add the fingerprint
        getCustomer().getCustomerPhotographs().clear();

        if (fingerPrint != null) {
            getCustomer().getCustomerPhotographs().add(fingerPrint);
            getPhotographs().addAll(getCustomer().getCustomerPhotographs());
            writeCustomerPhotographsDataToFile(getPhotographs());
        }
    }

    public Resolution getCustomersImmigrationDetailsAndUpdateCustomer() {
        if (getCustomer().getIdentityNumberType() != null) {
            if (!getCustomer().getIdentityNumberType().equalsIgnoreCase("passport")) {
                log.debug("National1" + getCustomer().getIdentityNumberType());
                getCustomersNidaDetailsAndUpdateCustomer();
            } else {
                String idNumberType = getCustomer().getIdentityNumberType();
                log.debug("Passport" + idNumberType);
                try {
                    //Save the fingerprint that was used to verify with Nida
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
                    Photograph fingerPrint = new Photograph();
                    byte[] fingerPrintData = Utils.getDataFromTempFile(getCustomer().getCustomerPhotographs().get(0).getPhotoGuid());
                    //fingerPrint.setData(Utils.decodeBase64(Utils.getDataFromTempFile(fingerPrint.getPhotoGuid())));
                    fingerPrint.setPhotoType("fingerprints");
                    fingerPrint.setPhotoGuid(getCustomer().getCustomerPhotographs().get(0).getPhotoGuid());
                    String countryCode = getCustomer().getCountryCode();
                    setCustomerQuery(new CustomerQuery());
                    getCustomerQuery().setCustomerId(getCustomer().getCustomerId());
                    getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO_ADDRESS);
                    setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
                    getPhotographs().addAll(getCustomer().getCustomerPhotographs());

                    String allowedImmigrationKycIdTypes = BaseUtils.getSubProperty("env.verify.foreigner.config", "AllowedIdTypesRegex");
                    log.debug("IDTYPE " + idNumberType);
                    if (!Utils.matchesWithPatternCache(idNumberType, allowedImmigrationKycIdTypes)) {
                        log.debug("Error in my IDTYPE " + idNumberType);
                        throw new Exception("Id type not enabled for eKyc verification -- customer ["
                                + getCustomer().getCustomerId() + "] is using id type [" + idNumberType + "].");
                    }

                    //Check if this fingerprint has been used before?
                    DocumentUniquenessQuery documentUniquenessQuery = new DocumentUniquenessQuery();
                    documentUniquenessQuery.setDocumentHash(HashUtils.md5(Utils.getDataFromTempFile(fingerPrint.getPhotoGuid())));
                    //An error will be thrown if document has been used before.
                    SCAWrapper.getUserSpecificInstance().checkDocumentUniqueness_Direct(documentUniquenessQuery);

                    VerifyForeignerQuery verifyForeignerQuery = new VerifyForeignerQuery();
                    verifyForeignerQuery.setDocumentNo(getCustomer().getIdentityNumber());
                    verifyForeignerQuery.setCountryCode(countryCode);
                    verifyForeignerQuery.setFingerprintB64(fingerPrintData);
                    verifyForeignerQuery.setVerifiedBy(getUser());
                    verifyForeignerQuery.setEntityId(Integer.toString(getCustomer().getCustomerId()));
                    verifyForeignerQuery.setEntityType("UPDATECUSTOMER");

                    VerifyForeignerReply verifyForeignerReply = SCAWrapper.getUserSpecificInstance().verifyForeignerCustomer_Direct(verifyForeignerQuery);

                    SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat formatter3 = new SimpleDateFormat("yyyy/MM/dd");
                    SimpleDateFormat formatter4 = new SimpleDateFormat("yyyy/MM/dd");
                    Date dateOfBirth = formatter1.parse(verifyForeignerReply.getDateOfBirth());
                    Date passportExpiryDate = formatter2.parse(verifyForeignerReply.getPassportExpiryDate());
                    getCustomer().setFirstName(verifyForeignerReply.getGivenName());
                    getCustomer().setMiddleName("");
                    getCustomer().setLastName(verifyForeignerReply.getSurname());

                    getCustomer().setGender(verifyForeignerReply.getSex());
                    getCustomer().setDateOfBirth(formatter3.format(dateOfBirth));
                    getCustomer().setKYCStatus("V");
                    getCustomer().setIdentityNumberType("passport");
                    getCustomer().setPassportExpiryDate(formatter4.format(passportExpiryDate));
                    getCustomer().setClassification("foreigner");
                    getCustomer().setNationality(iso3CountryCodeToIso2CountryCode(verifyForeignerReply.getNationality()));

                    //Physical address
                    int physicalAddressId = 0;
                    int postalAddressId = 0;
                    for (Address curAddr : getCustomer().getAddresses()) {
                        if (curAddr.getType() != null && curAddr.getType().equalsIgnoreCase("Physical Address")) {
                            physicalAddressId = curAddr.getAddressId();
                        }
                        if (curAddr.getType() != null && curAddr.getType().equalsIgnoreCase("Postal Address")) {
                            postalAddressId = curAddr.getAddressId();
                        }
                    }
                    log.warn("Immigration Verifying, current physical address id is [{}], current postal address is [{}]", physicalAddressId, postalAddressId);
                    Address addrPhy = new Address();
                    addrPhy.setType("Physical Address");
                    addrPhy.setLine1(verifyForeignerReply.getResidentRegion());
                    addrPhy.setLine2(verifyForeignerReply.getResidentRegion() + "," + verifyForeignerReply.getResidentWard()
                            + "," + verifyForeignerReply.getResidentDistrict());
                    addrPhy.setZone(verifyForeignerReply.getResidentWard() != null ? verifyForeignerReply.getResidentWard() : "");
                    addrPhy.setTown(verifyForeignerReply.getResidentDistrict());
                    addrPhy.setCode("");
                    addrPhy.setState(verifyForeignerReply.getResidentRegion());
                    addrPhy.setCountry(BaseUtils.getProperty("env.country.name"));
                    // LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), ""));
                    Address addrPost = new Address();
                    addrPost.setType("Postal Address");
                    addrPost.setLine1("");
                    addrPost.setLine2(verifyForeignerReply.getResidentWard());
                    addrPost.setCode("");
                    addrPost.setCountry(BaseUtils.getProperty("env.country.name"));
                    addrPost.setZone(verifyForeignerReply.getResidentWard() != null ? verifyForeignerReply.getResidentWard() : "");

                    byte[] fileData = Utils.decodeBase64(verifyForeignerReply.getPhoto());
                    String fileExtension = "jpg";
                    String photoGuid = Utils.getUUID() + "." + fileExtension;
                    File tmpFile = Utils.createTempFile(photoGuid, fileData);
                    Photograph photo = new Photograph();
                    photo.setPhotoGuid(photoGuid);
                    photo.setPhotoType("photo");
                    // photo.setData(nidaResponse.getPhoto()); //Base64 photo data
                    log.debug("Wrote tmp file to [{}] of size [{}]", tmpFile.getAbsolutePath(), fileData.length);

                    //Set photograph and fingerprints
                    Photograph curPhoto;
                    Iterator<Photograph> itrPhotos = getCustomer().getCustomerPhotographs().iterator();
                    while (itrPhotos.hasNext()) {
                        curPhoto = itrPhotos.next();
                        if (curPhoto.getPhotoType().equalsIgnoreCase("photo") || curPhoto.getPhotoType().equalsIgnoreCase("fingerprints")) {
                            itrPhotos.remove();
                            // break;
                        }
                    }
                    //Add the photo  from Immigration
                    getCustomer().getCustomerPhotographs().add(photo);
                    //Add fingerprints
                    getCustomer().getCustomerPhotographs().add(fingerPrint);

                    setCustomerPhotographsData(getCustomer().getCustomerPhotographs());

                    log.error("Customer has [{}] photos", getCustomer().getCustomerPhotographs().size());

                    SCAWrapper.getUserSpecificInstance().modifyCustomer(getCustomer());
                    // UPdate address details here;
                    // Physical
                    if (addrPhy.getLine2() == null || addrPhy.getLine2().isEmpty()) {
                        addrPhy.setLine2(" ");
                    }
                    if (addrPhy.getTown() == null || addrPhy.getTown().isEmpty()) {
                        addrPhy.setTown(" ");
                    }
                    if (addrPhy.getState() == null || addrPhy.getState().isEmpty()) {
                        addrPhy.setState(" ");
                    }

                    if (addrPost.getLine2() == null || addrPost.getLine2().isEmpty()) {
                        addrPost.setLine2(" ");
                    }

                    if (addrPost.getTown() == null || addrPost.getTown().isEmpty()) {
                        addrPost.setTown(" ");
                    }

                    if (addrPost.getState() == null || addrPost.getState().isEmpty()) {
                        addrPost.setState(" ");
                    }

                    if (addrPhy.getLine1() != null && !addrPhy.getLine1().isEmpty()) {
                        if (addrPhy.getAddressId() != 0) {
                            log.warn("Going to modify physical address with id [{}]", addrPhy.getAddressId());
                            SCAWrapper.getUserSpecificInstance().modifyAddress(addrPhy);
                        } else {//Add as 
                            log.warn("Going to add new physical address for customer [{}]", addrPhy.getCustomerId());
                            SCAWrapper.getUserSpecificInstance().addAddress(addrPhy);
                        }
                    }
                    //Postal
                    if (addrPost.getLine1() != null && !addrPost.getLine1().isEmpty()) {
                        if (addrPost.getAddressId() != 0) {
                            log.warn("Going to modify postal address with id [{}]", addrPost.getAddressId());
                            SCAWrapper.getUserSpecificInstance().modifyAddress(addrPost);
                        } else {
                            log.warn("Going to add new postal address for customer [{}]", addrPost.getCustomerId());
                            // getCustomer().getAddresses().add(addrPost);
                            SCAWrapper.getUserSpecificInstance().addAddress(addrPost);
                        }
                    }
                    // http://jira.smilecoms.com/browse/HBT-9643
                    //Now cheeck how many products does the customer have - if they have 1 product and immigration was veried successfully, also update their 
                    // SIM service to verified.
                    setCustomerQuery(new CustomerQuery());
                    getCustomerQuery().setCustomerId(getCustomer().getCustomerId());
                    getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP);
                    setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
                    // -----------------------------------------------------------------------------------------------------
                    if (getCustomer().getProductInstances().size() == 1) { //As requested by Pamela, in this scenario, also set the SIM service for this product as verified
                        //See URL: http://jira.smilecoms.com/browse/HBT-9643
                        ProductInstance pi = getCustomer().getProductInstances().get(0);
                        //setProductOrder(new ProductOrder());
                        //Find SIM service
                        ServiceInstance si = null;
                        for (ProductServiceInstanceMapping m : getCustomer().getProductInstances().get(0).getProductServiceInstanceMappings()) {
                            if (m.getServiceInstance().getServiceSpecificationId() == 1) { //SIM Found
                                si = m.getServiceInstance();
                                break;
                            }
                        }

                        if (si != null) { //SIM found 
                            populateAVPDetailsIntoServiceInstanceAVPs(si);
                            setProductOrder(new ProductOrder());
                            getProductOrder().setAction(StAction.NONE);
                            getProductOrder().setOrganisationId(pi.getOrganisationId());
                            getProductOrder().setCustomerId(pi.getCustomerId());
                            getProductOrder().setProductInstanceId(si.getProductInstanceId());
                            getProductOrder().getServiceInstanceOrders().add(new ServiceInstanceOrder());
                            getProductOrder().getServiceInstanceOrders().get(0).setAction(StAction.UPDATE);
                            getProductOrder().getServiceInstanceOrders().get(0).setServiceInstance(new ServiceInstance());
                            getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setServiceInstanceId(si.getServiceInstanceId());
                            getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setServiceSpecificationId(si.getServiceSpecificationId());
                            getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setCustomerId(si.getCustomerId());
                            getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setStatus(si.getStatus());
                            getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setAccountId(si.getAccountId());

                            AVP status = new AVP();
                            status.setAttribute("KYCStatus");
                            status.setValue(StringEscapeUtils.unescapeHtml("Complete"));
                            getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs().add(status);

                            AVP method = new AVP();
                            method.setAttribute("KYCVerifyingMethod");
                            method.setValue(StringEscapeUtils.unescapeHtml("Immigration"));
                            getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs().add(method);

//                    AVP trans = new AVP();
//                    trans.setAttribute("NIDATransactionId");
//                    trans.setValue(nidaResponse.getNIDATransactionId());
//                    getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs().add(trans);
                            AVP date = new AVP();
                            date.setAttribute("ForeignerVerifiedOnDate");
                            date.setValue(sdf.format(new Date()));
                            getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs().add(date);
                            getCustomer().setClassification("foreigner");

                            formatAVPsForSendingToSCA(getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs(), getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getServiceSpecificationId(), true);
                            log.warn("Size of APVs before calling processOder is: [{}]", getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs().size());
                            SCAWrapper.getUserSpecificInstance().processOrder(getProductOrder());
                        } else { //SIM was not found
                            log.error("SIM service was  not found under product instance id [{}]", getCustomer().getProductInstances().get(0).getProductInstanceId());
                        }
                    }

                    if (getCustomer() != null) {
                        setPageMessage("customer.updated.successfully");
                    }
                } catch (SCABusinessError ex) {
                    getCustomer().setClassification("foreigner");
                    throw new RuntimeException(ex);
                } catch (Exception ex) {
                    getCustomer().setClassification("foreigner");
                    setVerifyExistingCustomerWithNIDAOrImmigration(true);
                    throw new RuntimeException(ex);
                } finally {
                    setVerifyExistingCustomerWithNIDAOrImmigration(true);
                    getCustomer().setClassification("foreigner");
                }
            }

        }

        return retrieveCustomer();
    }

    public Resolution verifyExistingCustomerImmigration() {

        getCustomersNidaDetailsAndShowAddCustomerBasicDetails();
        setVerifyExistingCustomerImmigration(true);
        return getDDForwardResolution("/customer/add_customer_capture_nida_input.jsp");
    }

    private Integer getYearOfBirth(java.lang.String dateOfBirth) {
        //dateofbirth in db is YYYYMMDD
        String yob = dateOfBirth.substring(0, 4);
        return Integer.valueOf(yob);
    }

    private java.lang.String getSexForUCC(java.lang.String gender) {
        if (gender.equals("F")) {
            return "Female";
        }
        return "Male";
    }

    private String iso3CountryCodeToIso2CountryCode(String iso3CountryCode) {
        Map<String, Locale> localeMap;
        String[] countries = Locale.getISOCountries();
        localeMap = new HashMap<>(countries.length);
        for (String country : countries) {
            Locale locale = new Locale("", country);
            localeMap.put(locale.getISO3Country().toUpperCase(), locale);
        }
        return localeMap.get(iso3CountryCode).getCountry();
    }

    public Resolution verifyDefacedCustomer() {
        String nin = getVerifyDefacedCustomerRequest().getNin();
        com.smilecoms.commons.sca.direct.im.VerifyDefacedQuery request = new com.smilecoms.commons.sca.direct.im.VerifyDefacedQuery();
        request.setNin(nin);
        request.setVerifiedBy(getUser());
        request.setEntityId(Integer.toString(getCustomer().getCustomerId()));
        request.setEntityType("NEWCUSTOMER");
        log.warn("TEST" + getVerifyDefacedCustomerRequest().getQuestionCode() + " " + getVerifyDefacedCustomerRequest().getAnswer());
        if (getVerifyDefacedCustomerRequest().getAnswer() != null && !getVerifyDefacedCustomerRequest().getAnswer().isEmpty()) {
            request.setQuestionCode(getVerifyDefacedCustomerRequest().getQuestionCode());
            request.setAnswer(getVerifyDefacedCustomerRequest().getAnswer());
        }

        com.smilecoms.commons.sca.direct.im.VerifyDefacedReply defacedResponse = SCAWrapper.getUserSpecificInstance().verifyDefacedCustomer_Direct(request);
        log.warn("TEST" + defacedResponse.getCode() + " " + defacedResponse.getQuestionCode());
        if (defacedResponse.getCode().equalsIgnoreCase("120") && !defacedResponse.getQuestionCode().isEmpty()) {
            log.warn("TEST inside" + defacedResponse.getCode() + " " + defacedResponse.getQuestionCode());
            getVerifyDefacedCustomerRequest().setNin(nin);
            getVerifyDefacedCustomerRequest().setQuestionCode(defacedResponse.getQuestionCode());
            getVerifyDefacedCustomerRequest().setQuestionEnglish(defacedResponse.getQuestionEnglish());
            getVerifyDefacedCustomerRequest().setQuestionSwahili(defacedResponse.getQuestionSwahili());
            getVerifyDefacedCustomerRequest().setAnswer("");
            return getDDForwardResolution("/customer/alternative_customer_verification.jsp");
        } else if (defacedResponse.getCode().equalsIgnoreCase("130")) {
            localiseErrorAndAddToGlobalErrors("npc.validation.error", "You not answered minimum questions");
        } else if ((defacedResponse.getCode().equalsIgnoreCase("120") || defacedResponse.getCode().equalsIgnoreCase("00")) && !defacedResponse.getFirstName().isEmpty()) {
            try {
                SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat formatter3 = new SimpleDateFormat("yyyy/MM/dd");
                SimpleDateFormat formatter4 = new SimpleDateFormat("yyyy/MM/dd");
                Date dateOfBirth = formatter1.parse(defacedResponse.getDateOfBirth());
                getCustomer().setFirstName(defacedResponse.getFirstName());
                getCustomer().setMiddleName(defacedResponse.getMiddleName());
                getCustomer().setLastName(defacedResponse.getLastName());
                if (defacedResponse.getSex().equalsIgnoreCase("male")) {
                    getCustomer().setGender("M");
                } else {
                    getCustomer().setGender("F");
                }
                getCustomer().setDateOfBirth(formatter3.format(dateOfBirth));
                getCustomer().setKYCStatus("V");
                getCustomer().setNationality("TZ");
                getCustomer().setIdentityNumberType("nationalid");
                getCustomer().setIdentityNumber(nin);

                Address addrPhy = new Address();
                addrPhy.setType("Physical Address");
                addrPhy.setLine1(defacedResponse.getResidentHouseNo());
                addrPhy.setLine2(defacedResponse.getResidentStreet() + "," + defacedResponse.getResidentWard()
                        + "," + defacedResponse.getResidentVillage());
                addrPhy.setZone(defacedResponse.getResidentWard());
                addrPhy.setTown(defacedResponse.getResidentDistrict());
                addrPhy.setCode(defacedResponse.getResidentPostCode());
                addrPhy.setState(defacedResponse.getResidentRegion());
                addrPhy.setCountry(BaseUtils.getProperty("env.country.name"));
                // LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), ""));
                Address addrPost = new Address();
                addrPost.setType("Postal Address");
                addrPost.setLine1(defacedResponse.getResidentPostalAddress());
                addrPost.setLine2(defacedResponse.getResidentPostalAddress());
                addrPost.setCode(defacedResponse.getResidentPostCode());
                addrPost.setCountry(BaseUtils.getProperty("env.country.name"));
                addrPost.setZone(defacedResponse.getResidentWard());

                if (addrPost.getLine1() != null && !addrPost.getLine1().isEmpty()) {
                    getCustomer().getAddresses().add(addrPost);
                } else {
                    addrPhy.setLine1("NA");
                }
                getCustomer().getAddresses().add(addrPhy);

                byte[] fileData = Utils.decodeBase64(defacedResponse.getPhoto());
                String fileExtension = "jpg";
                File tmpFile = Utils.createTempFile(Utils.getUUID() + "." + fileExtension, fileData);
                Photograph photo = new Photograph();
                photo.setPhotoGuid(tmpFile.getName());
                photo.setPhotoType("photo");
                if ("nationalid".equalsIgnoreCase(getCustomer().getClassification())) {
                    getCustomer().setClassification("foreigner");
                }

                getCustomer().setAlternativeContact1(defacedResponse.getPhoneNumber());

                log.debug("Wrote tmp file to [{}] of size [{}]", tmpFile.getAbsolutePath(), fileData.length);

                getCustomer().getCustomerPhotographs().add(photo);

                return getDDForwardResolution("/customer/add_customer_capture_basic_details.jsp");
            } catch (Exception ex) {
                log.error("Error:", ex);
                throw new RuntimeException(ex);
            }
        }
        return getDDForwardResolution("/customer/alternative_customer_verification.jsp");
    }

    public Resolution showMandatoryKYC() {
        checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_VERIFIED);
        setCustomerQuery(new CustomerQuery());
        getCustomerQuery().setKYCStatus("P");
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO_ADDRESS_MANDATEKYCFIELD);
        getCustomerQuery().setResultLimit(BaseUtils.getIntProperty("env.sep.kyc.validate.rows", 5));
        List<Customer> customers = SCAWrapper.getUserSpecificInstance().getCustomers(getCustomerQuery()).getCustomers();
        setCustomerQuery(new CustomerQuery());
        getCustomerQuery().setKYCStatus("U");
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO_ADDRESS_MANDATEKYCFIELD);
        getCustomerQuery().setResultLimit(BaseUtils.getIntProperty("env.sep.kyc.validate.rows", 5));
        setCustomerList(SCAWrapper.getUserSpecificInstance().getCustomers(getCustomerQuery()));
        log.info("Customer list size1 before [{}]", getCustomerList().getCustomers().size());
        getCustomerList().getCustomers().addAll(customers);
        log.info("Customer list size1 after [{}]", getCustomerList().getCustomers().size());
        lastModifiedBy = new HashMap<>();
        List<Customer> custlist = getCustomerList().getCustomers();
        Collections.sort(custlist, (Customer o1, Customer o2) -> o1.getCreatedDateTime().compare(o2.getCreatedDateTime()));
        for (Customer c : custlist) {
            List<Photograph> justPhotos = new ArrayList<>();
            for (Photograph p : c.getCustomerPhotographs()) {
                if (!p.getPhotoType().equals("fingerprints")) {
                    String fileName = p.getPhotoGuid();
                    try {
                        Utils.createTempFile(fileName, Utils.decodeBase64(p.getData()));
                        justPhotos.add(p);
                    } catch (Exception e) {
                        log.warn("Error in writeCustomerPhotographsDataToFile", e);
                    }
                }
            }
            c.getCustomerPhotographs().clear();
            c.getCustomerPhotographs().addAll(justPhotos);
            lastModifiedBy.put(c.getCustomerId(), getLastModifiedBySalesPerson(c).getCustomerId());
        }

        return getDDForwardResolution("/customer/kyc_mandatory.jsp");
    }

    public Resolution updateMandateKYCSatus() {
        checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_VERIFIED);

        if (getCustomerList().getCustomers() != null) {
            log.info("List size [{}]", getCustomerList().getCustomers().size());

            for (Customer c : getCustomerList().getCustomers()) {
                MandatoryKYCFields mkf = new MandatoryKYCFields();
                try {
                    formatMandatoryKYCFields(c.getMandatoryKYCFields(), mkf);
                } catch (Exception ex) {
                    log.warn("Error in formating KYC object", ex);
                }

                if (getMandatoryKycfieldsStatus(c.getMandatoryKYCFields())) {
                    log.debug("Setting KYC status to V on cust id [{}]", mkf.getCustomerId());
                    Customer c1 = UserSpecificCachedDataHelper.getCustomer(mkf.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS);;
                    if (c1.getKYCStatus().equals("P") || c1.getKYCStatus().equals("U")) {
                        c1.setKYCStatus("V");
                        c1.setMandatoryKYCFields(mkf);
                        SCAWrapper.getUserSpecificInstance().modifyCustomer(c1);
                        createTicket("KYC Data Accepted", "KYC Verified", "KYC data on customer id " + c1.getCustomerId() + " has been updated to KYCVerified.", c1, true);
                    }
                } else {
                    Customer loggedIn = SCAWrapper.getUserSpecificInstance().getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER);
                    log.debug("Setting KYC status to U on cust id [{}]", mkf.getCustomerId());
                    Customer c2 = UserSpecificCachedDataHelper.getCustomer(mkf.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS);;
                    if (c2.getKYCStatus().equals("P") || c2.getKYCStatus().equals("U")) {
                        Customer salesPerson = getLastModifiedBySalesPerson(c2);
                        try {
                            if (salesPerson != null) {
                                Event eventData = new Event();
                                eventData.setEventType("KYC");
                                eventData.setEventSubType("U");
                                eventData.setEventKey(String.valueOf(c2.getCustomerId()));
                                eventData.setEventData(salesPerson.getCustomerId() + "|" + loggedIn.getCustomerId() + "|" + "Some of the fiels are missing");
                                SCAWrapper.getAdminInstance().createEvent(eventData);
                                IMAPUtils.sendEmail(loggedIn.getEmailAddress(), salesPerson.getEmailAddress(), "KYC Data Rejected!", "KYC data on customer id " + c2.getCustomerId() + " must be recaptured. Reason: " + "Some of the fiels are missing");
                                createTicket("KYC Data Rejected", "KYC Rejected!", "KYC data on customer id " + c2.getCustomerId() + " must be recaptured. Reason: " + "Some of the fiels are missing", c2, false);
                            }
                        } catch (Exception ex) {
                            log.warn("Error sending KYC rejection email", ex);
                        }

                        log.debug("Before sending to identity manager cust id [{}]", mkf.getCustomerId());
                        c2.setKYCStatus("U");
                        c2.setMandatoryKYCFields(mkf);
                        log.debug("all values2 [{}]", c2.getMandatoryKYCFields().getCustomerId() + "-" + c2.getMandatoryKYCFields().getDobVerified() + "-" + c2.getMandatoryKYCFields().getEmailVerified() + "-" + c2.getMandatoryKYCFields().getFacialPitureVerified()
                                + "-" + c2.getMandatoryKYCFields().getFingerPrintVerified() + "-" + c2.getMandatoryKYCFields().getGenderVerified() + "-" + c2.getMandatoryKYCFields().getMobileVerified()
                                + "-" + c2.getMandatoryKYCFields().getNameVerified() + "-" + c2.getMandatoryKYCFields().getNationalityVerified()
                                + "-" + c2.getMandatoryKYCFields().getPhysicalAddressVerified() + "-" + c2.getMandatoryKYCFields().getTitleVerified() + "-" + c2.getMandatoryKYCFields().getValidIdCardVerified());
                        SCAWrapper.getUserSpecificInstance().modifyCustomer(c2);

                    }
                }
            }
        }
        setPageMessage("kyc.status.updated");
        return showMandatoryKYC();
    }

    public static boolean getMandatoryKycfieldsStatus(MandatoryKYCFields kycStatus) {
        boolean status = false;
        if (kycStatus.getDobVerified() != null && kycStatus.getEmailVerified() != null && kycStatus.getFacialPitureVerified() != null && kycStatus.getFingerPrintVerified() != null
                && kycStatus.getGenderVerified() != null && kycStatus.getMobileVerified() != null && kycStatus.getNameVerified() != null && kycStatus.getNationalityVerified() != null
                && kycStatus.getPhysicalAddressVerified() != null && kycStatus.getTitleVerified() != null && kycStatus.getValidIdCardVerified() != null) {
            status = true;
        }
        return status;
    }

    private void formatMandatoryKYCFields(MandatoryKYCFields mandatoryKYCFields, MandatoryKYCFields newmandatoryKYCFields) throws Exception {
        newmandatoryKYCFields.setCustomerId(mandatoryKYCFields.getCustomerId());
        newmandatoryKYCFields.setDobVerified(mandatoryKYCFields.getDobVerified() == null ? "N" : "Y");
        newmandatoryKYCFields.setEmailVerified(mandatoryKYCFields.getEmailVerified() == null ? "N" : "Y");
        newmandatoryKYCFields.setFacialPitureVerified(mandatoryKYCFields.getFacialPitureVerified() == null ? "N" : "Y");
        newmandatoryKYCFields.setFingerPrintVerified(mandatoryKYCFields.getFingerPrintVerified() == null ? "N" : "Y");
        newmandatoryKYCFields.setGenderVerified(mandatoryKYCFields.getGenderVerified() == null ? "N" : "Y");
        newmandatoryKYCFields.setMobileVerified(mandatoryKYCFields.getMobileVerified() == null ? "N" : "Y");
        newmandatoryKYCFields.setNameVerified(mandatoryKYCFields.getNameVerified() == null ? "N" : "Y");
        newmandatoryKYCFields.setNationalityVerified(mandatoryKYCFields.getNationalityVerified() == null ? "N" : "Y");
        newmandatoryKYCFields.setPhysicalAddressVerified(mandatoryKYCFields.getPhysicalAddressVerified() == null ? "N" : "Y");
        newmandatoryKYCFields.setTitleVerified(mandatoryKYCFields.getTitleVerified() == null ? "N" : "Y");
        newmandatoryKYCFields.setValidIdCardVerified(mandatoryKYCFields.getValidIdCardVerified() == null ? "N" : "Y");
    }

    public Resolution showCustomerPendingForNINVerification() {
        checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_VERIFIED);
        setCustomerQuery(new CustomerQuery());
        getCustomerQuery().setIsNinVerified("N");
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);
        getCustomerQuery().setResultLimit(BaseUtils.getIntProperty("env.sep.nin.validate.rows", 10));
        setCustomerList(SCAWrapper.getUserSpecificInstance().getCustomers(getCustomerQuery()));
        return getDDForwardResolution("/customer/verify_customer_nin.jsp");
    }

    private List<Integer> NINVerified;

    public List<Integer> getNINVerified() {
        return NINVerified;
    }

    public void setNINVerified(List<Integer> NINVerified) {
        this.NINVerified = NINVerified;
    }

    public Resolution verifyCustomerNins() {
        checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_VERIFIED);
        if (getNINVerified() != null) {
            for (int custId : getNINVerified()) {
                log.debug("Verifying NIN for cust id [{}]", custId);
                setCustomerQuery(new CustomerQuery());
                getCustomerQuery().setCustomerId(custId);
                getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO_ADDRESS);
                getCustomerQuery().setResultLimit(1);
                setCustomerList(SCAWrapper.getAdminInstance().getCustomers(getCustomerQuery()));
                setCustomer(getCustomerList().getCustomers().get(0));
                try {
                    if (getCustomer().getNationalIdentityNumber() != null && !getCustomer().getNationalIdentityNumber().isEmpty()) {

                        // Check if customer has fingerprints.
                        if (getCustomer().getIsNinVerified().equalsIgnoreCase("N")) {
                            byte[] fingerPrintData = null;
                            for (Photograph photo : getCustomer().getCustomerPhotographs()) {
                                if (photo.getPhotoType().equalsIgnoreCase("fingerprints")) {
                                    String fileName = photo.getPhotoGuid();
                                    try {
                                        Utils.createTempFile(fileName, Utils.decodeBase64(photo.getData()));
                                        fingerPrintData = Utils.getDataFromTempFile(photo.getPhotoGuid());
                                    } catch (Exception e) {
                                        log.debug("Error in writeCustomerPhotographsDataToFile", e);
                                    }
                                }
                            }
                            VerifyNinQuery verifyNinQuery = new VerifyNinQuery();
                            verifyNinQuery.setNin(getCustomer().getNationalIdentityNumber());
                            verifyNinQuery.setFirstName(getCustomer().getFirstName());
                            verifyNinQuery.setLastName(getCustomer().getLastName());
                            verifyNinQuery.setSurname(getCustomer().getMiddleName());
                            verifyNinQuery.setVerifiedBy(getUser());
                            verifyNinQuery.setEntityId(Integer.toString(getCustomer().getCustomerId()));
                            verifyNinQuery.setEntityType("UPDATECUSTOMER");
                            if (fingerPrintData != null) {
                                if (BaseUtils.getBooleanProperty("env.nimc.use.fingerprint", true)) {
                                    verifyNinQuery.setFingerStringInBase64(fingerPrintData);
                                } else {
                                    verifyNinQuery.setFingerStringInBase64(null);
                                }
                            }
                            VerifyNinResponseList verifyNinResponseList = SCAWrapper.getUserSpecificInstance().verifyNinCustomer_Direct(verifyNinQuery);
                            if (!verifyNinResponseList.getVerifyNinReplys().get(0).getNin().isEmpty()) {
                                if (!verifyNinResponseList.getVerifyNinReplys().get(0).getNin().equalsIgnoreCase(getCustomer().getNationalIdentityNumber())) {
                                    log.warn("NIN [{}]", verifyNinResponseList.getVerifyNinReplys().get(0).getNin());
                                    Event eventData = new Event();
                                    eventData.setEventType("NinBulkVerification");
                                    eventData.setEventSubType("NinQueryResponse");
                                    eventData.setEventKey(getCustomer().getNationalIdentityNumber());
                                    eventData.setEventData("NIN:" + verifyNinQuery.getNin() + "|"
                                            + "FirstName:" + verifyNinQuery.getFirstName() + "|"
                                            + "Surname:" + verifyNinQuery.getSurname() + "|"
                                            + "LastName:" + verifyNinQuery.getLastName() + "|"
                                            + "VerifiedBy:" + verifyNinQuery.getVerifiedBy() + "|"
                                            + "EntityID:" + verifyNinQuery.getEntityId() + "|"
                                            + "EntityType:" + verifyNinQuery.getEntityType() + "|"
                                            + "ReturnMessage:" + verifyNinResponseList.getReturnMessage());
                                    SCAWrapper.getAdminInstance().createEvent(eventData);
                                } else {
                                    Event eventData = new Event();
                                    eventData.setEventType("NinBulkVerification");
                                    eventData.setEventSubType("NinQueryResponse");
                                    eventData.setEventKey(getCustomer().getNationalIdentityNumber());
                                    eventData.setEventData("NIN:" + verifyNinQuery.getNin() + "|"
                                            + "FirstName:" + verifyNinQuery.getFirstName() + "|"
                                            + "Surname:" + verifyNinQuery.getSurname() + "|"
                                            + "LastName:" + verifyNinQuery.getLastName() + "|"
                                            + "VerifiedBy:" + verifyNinQuery.getVerifiedBy() + "|"
                                            + "EntityID:" + verifyNinQuery.getEntityId() + "|"
                                            + "EntityType:" + verifyNinQuery.getEntityType() + "|"
                                            + "ReturnMessage:" + verifyNinResponseList.getReturnMessage());
                                    SCAWrapper.getAdminInstance().createEvent(eventData);
                                    getCustomer().setIsNinVerified("Y");
                                    SCAWrapper.getUserSpecificInstance().modifyCustomer(getCustomer());
                                }

                            }
                        }
                    }
                } catch (Exception e) {
                    Event eventData = new Event();
                    eventData.setEventType("NinBulkVerification");
                    eventData.setEventSubType("NinQueryResponse");
                    eventData.setEventKey(getCustomer().getNationalIdentityNumber());
                    eventData.setEventData("NIN:" + getCustomer().getNationalIdentityNumber() + "|"
                            + "FirstName:" + getCustomer().getFirstName() + "|"
                            + "Surname:" + getCustomer().getMiddleName() + "|"
                            + "LastName:" + getCustomer().getLastName() + "|"
                            + "VerifiedBy:" + getUser() + "|"
                            + "EntityID:" + getCustomer().getCustomerId() + "|"
                            + "EntityType:" + "UPDATECUSTOMER" + "|"
                            + "ReturnMessage:" + e);
                    SCAWrapper.getAdminInstance().createEvent(eventData);
                }
            }
        }
        setPageMessage("bulk.nin.verfied.message");
        return showCustomerPendingForNINVerification();
    }

    public Resolution showCustomerDataFromNimc() {
        return getDDForwardResolution("/customer/retrive_customer_details__from_nimc.jsp");
    }

    public Resolution showCustomerDataFromKYCData() {
        HttpSession session = getContext().getRequest().getSession();               
        session.removeAttribute("sessionAddProduct");
        session.removeAttribute("sessionSale");
        
        return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
    }

    boolean isNIMCVerifiedAndApproved = false;

    public boolean isIsNIMCVerifiedAndApproved() {
        return isNIMCVerifiedAndApproved;
    }

    public void setIsNIMCVerifiedAndApproved(boolean isNIMCVerifiedAndApproved) {
        this.isNIMCVerifiedAndApproved = isNIMCVerifiedAndApproved;
    }

    public Resolution retriveComparisonData() {        
        setCustomers(new ArrayList<Customer>());
        if ((getParameter("datasrc") == null || getParameter("datasrc").equalsIgnoreCase("-1")) || getCustomer().getNationalIdentityNumber().trim().isEmpty()) {
            localiseErrorAndAddToGlobalErrors("Please supply all fields.");

            if (getParameter("processType") != null && getParameter("processType").equalsIgnoreCase("simswap")) {
                setSimSwap(true);
                setIsNIMCVerifiedAndApproved(hasVerifiedNIMCStatus());
                return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
            } else if (getParameter("processType") != null && getParameter("processType").equalsIgnoreCase("newsim")) {
                setNewSim(true);
                setIsNIMCVerifiedAndApproved(hasVerifiedNIMCStatus());
                return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
            } else if (getParameter("processType") != null && getParameter("processType").equalsIgnoreCase("addsim")) {
                
                setAddSim(true);
                setIsNIMCVerifiedAndApproved(hasVerifiedNIMCStatus());
                return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
            } else {
                return showCompareCustomerRegulatorData();
            }
        }
        String dataFrom = getParameter("datasrc");
        setCustomerQuery(new CustomerQuery());
        getCustomerQuery().setNationalIdentityNumber(getCustomer().getNationalIdentityNumber());
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO_ADDRESS);
        getCustomerQuery().setResultLimit(1);
        setCustomer(SCAWrapper.getAdminInstance().getCustomer(getCustomerQuery()));

        Customer compareCustomer = new Customer();
        if (dataFrom.equalsIgnoreCase("nimc")) {
            try {

                Photograph facialphoto = null;

                log.warn("TestA NationalIdentityNumber " + getCustomer().getNationalIdentityNumber());
                Photograph fingerPrint = null;
                for (Photograph photo : getCustomer().getCustomerPhotographs()) {
                    if (photo.getPhotoType().equalsIgnoreCase("fingerPrints")) {
                        fingerPrint = photo;
                    }

                    if (photo.getPhotoType().equalsIgnoreCase("photo")) {
                        facialphoto = photo;
                    }
                }
                getCustomer().getCustomerPhotographs().clear();
                if (fingerPrint != null) {
                    getCustomer().getCustomerPhotographs().add(fingerPrint);
                    getPhotographs().addAll(getCustomer().getCustomerPhotographs());
                    writeCustomerPhotographsDataToFile(getPhotographs());
                }

                

                VerifyNinQuery verifyNinQuery = new VerifyNinQuery();
                verifyNinQuery.setNin(getCustomer().getNationalIdentityNumber());

                if (BaseUtils.getBooleanProperty("env.nimc.use.fingerprint", true)) {
                    byte[] fingerPrintData = Utils.getDataFromTempFile(getCustomer().getCustomerPhotographs().get(0).getPhotoGuid());
                    verifyNinQuery.setFingerStringInBase64(fingerPrintData);
                } else {
                    verifyNinQuery.setFingerStringInBase64(null);
                }

                verifyNinQuery.setVerifiedBy(getUser());
                verifyNinQuery.setEntityType("NEWCUSTOMER");
                VerifyNinResponseList verifyNinResponseList = SCAWrapper.getAdminInstance().verifyNinCustomer_Direct(verifyNinQuery);
                VerifyNinReply ninResponse = verifyNinResponseList.getVerifyNinReplys().get(0);

                if (!ninResponse.getNin().isEmpty()) {
                    if (ninResponse.getNin().equalsIgnoreCase(getCustomer().getNationalIdentityNumber())) {
                        // setCustomer(new Customer());
                        compareCustomer.setFirstName(ninResponse.getFirstname());
                        compareCustomer.setMiddleName(ninResponse.getMiddlename());
                        compareCustomer.setLastName(ninResponse.getSurname());
                        compareCustomer.setGender(ninResponse.getGender());
                        compareCustomer.setDateOfBirth(ninResponse.getBirthdate());
                        compareCustomer.setNationalIdentityNumber(ninResponse.getNin());
                        compareCustomer.setLanguage(ninResponse.getNspokenlang());
                        compareCustomer.setNationality(BaseUtils.getProperty("env.country.name"));
                        compareCustomer.setAlternativeContact1(ninResponse.getTelephoneno());
                        CustomerNinData customerNinData = new CustomerNinData();
                        customerNinData.setNinVerificationTrackingId(ninResponse.getTrackingId());
                        compareCustomer.setCustomerNinData(customerNinData);
                        Address addrPhy = new Address();
                        addrPhy.setType("Physical Address");
                        addrPhy.setLine1(ninResponse.getResidenceAdressLine1());
                        addrPhy.setLine2(ninResponse.getResidenceAdressLine2());
                        addrPhy.setZone(ninResponse.getResidenceLga());
                        addrPhy.setTown(ninResponse.getResidenceTown());
                        addrPhy.setCode(ninResponse.getResidencePostalcode());
                        addrPhy.setState(ninResponse.getResidenceState());
                        addrPhy.setCountry(BaseUtils.getProperty("env.country.name"));
                        compareCustomer.getAddresses().add(addrPhy);

                        byte[] fileData = Utils.decodeBase64(ninResponse.getPhoto());
                        //byte[] fileData = ninResponse.getPhoto().getBytes();
                        String fileExtension = "jpg";
                        File tmpFile = Utils.createTempFile(Utils.getUUID() + "." + fileExtension, fileData);
                        Photograph photo = new Photograph();
                        photo.setPhotoGuid(tmpFile.getName());
                        photo.setPhotoType("photo");
                        //photo.setData(ninResponse.getPhoto().getBytes());

                        /*    byte[] fileData1 = ninResponse.getSignature().getBytes();
                        String fileExtension1 = "jpg";
                        File tmpFile1 = Utils.createTempFile(Utils.getUUID() + "." + fileExtension1, fileData1);
                        Photograph photo1 = new Photograph();
                        photo1.setPhotoGuid(tmpFile1.getName());
                        photo1.setPhotoType("photo");                        
                         */
                        getPhotographs().clear();
                        getCustomer().getCustomerPhotographs().clear();
                        if (facialphoto != null) {
                            getCustomer().getCustomerPhotographs().add(facialphoto);
                            getPhotographs().add(getCustomer().getCustomerPhotographs().get(0));
                            writeCustomerPhotographsDataToFile(getPhotographs());
                        }

                        compareCustomer.getCustomerPhotographs().clear();
                        compareCustomer.getCustomerPhotographs().add(photo);
                        //   compareCustomer.getCustomerPhotographs().add(photo1);
                        getPhotographs().add(compareCustomer.getCustomerPhotographs().get(0));

                        //writeCustomerPhotographsDataToFile(getPhotographs());
                        try {
                            SimpleDateFormat sdf1 = new SimpleDateFormat("dd-MM-yyyy");

                            Date compDOB = sdf1.parse(compareCustomer.getDateOfBirth());
                            sdf1.applyPattern("yyyy-MM-dd");
                            compareCustomer.setDateOfBirth(sdf1.format(compDOB));

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        setninVerified(true);
                    } else {
                        setPageMessage("retrieve.customer.details.from.nimc", verifyNinResponseList.getReturnMessage());
                    }
                } else {
                    setPageMessage("retrieve.customer.details.from.nimc", verifyNinResponseList.getReturnMessage());
                    setninVerified(false);
                }
            } catch (Exception e) {
                String err="";
                if(e.getMessage().toString().toLowerCase().contains("with description")) {
                    err = e.getMessage().toString().substring(e.getMessage().toString().toLowerCase().indexOf("description"));
                } else {
                    err = e.getMessage().toString().substring(e.getMessage().toString().toLowerCase().indexOf("desc"));
                }
                localiseErrorAndAddToGlobalErrors("nimc.general.error.message", err);
                setninVerified(false);

                if (getParameter("processType") != null && getParameter("processType").equalsIgnoreCase("simswap")) {
                    setSimSwap(true);
                    setIsNIMCVerifiedAndApproved(hasVerifiedNIMCStatus());
                    return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
                } else if (getParameter("processType") != null && getParameter("processType").equalsIgnoreCase("newsim")) {
                    setNewSim(true);
                    setIsNIMCVerifiedAndApproved(hasVerifiedNIMCStatus());
                    return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
                } else if (getParameter("processType") != null && getParameter("processType").equalsIgnoreCase("addsim")) {
                    setAddSim(true);
                    setIsNIMCVerifiedAndApproved(hasVerifiedNIMCStatus());
                    return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
                } else {
                    return showCompareCustomerRegulatorData();
                }
            }
        } else if (dataFrom.equalsIgnoreCase("smileid")) {
            log.warn("In getParamDataSrc");

            Photograph facialphoto = null;
            for (Photograph photo : getCustomer().getCustomerPhotographs()) {
                if (photo.getPhotoType().equalsIgnoreCase("photo")) {
                    facialphoto = photo;

                }
            }
            //Clear all photos, and only add the fingerprint
            getCustomer().getCustomerPhotographs().clear();

            if (facialphoto != null) {
                log.warn("Facial Photo was found [{}]", facialphoto.getPhotoGuid());
                String fileName = facialphoto.getPhotoGuid();
                try {
                    Utils.createTempFile(fileName, Utils.decodeBase64(facialphoto.getData()));
                    log.warn("Facial Photo tempFile was written was found");
                } catch (Exception e) {
                    log.warn("Error in writeCustomerPhotographsDataToFile", e);
                    setninVerified(false);
                }

                getCustomer().getCustomerPhotographs().add(facialphoto);
                getPhotographs().clear();
                getPhotographs().add(facialphoto);

            }
            compareCustomer = getThirdPartyRegulatorData(getCustomer().getNationalIdentityNumber());

            try {
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

                Date compDOB = sdf1.parse(compareCustomer.getDateOfBirth());
                sdf1.applyPattern("yyyyMMdd");
                compareCustomer.setDateOfBirth(sdf1.format(compDOB));

            } catch (Exception e) {
                e.printStackTrace();

            }

            setninVerified(true);
        }
        customers.clear();
        getPhotographs().clear();

        if (getCustomer().getCustomerPhotographs().size() > 0) {

            getPhotographs().add(getCustomer().getCustomerPhotographs().get(0));
        }

        if (compareCustomer.getCustomerPhotographs().size() > 0) {
            getPhotographs().add(compareCustomer.getCustomerPhotographs().get(0));
        }

        try {
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd");

            Date custDOB = sdf1.parse(getCustomer().getDateOfBirth());
            sdf1.applyPattern("yyyy-MM-dd");
            getCustomer().setDateOfBirth(sdf1.format(custDOB));

        } catch (Exception e) {
            e.printStackTrace();

        }

        customers.add(getCustomer());
        customers.add(compareCustomer);

        if (getParameter("processType") != null && getParameter("processType").equalsIgnoreCase("simswap")) {
            setIsNIMCVerifiedAndApproved(hasVerifiedNIMCStatus());
            setSimSwap(true);
        } 
        
        if (getParameter("processType") != null && getParameter("processType").equalsIgnoreCase("newsim")) {
            setIsNIMCVerifiedAndApproved(hasVerifiedNIMCStatus());
            setNewSim(true);
        }
        
        if (getParameter("processType") != null && getParameter("processType").equalsIgnoreCase("addsim")) {            
            setIsNIMCVerifiedAndApproved(hasVerifiedNIMCStatus());
            setAddSim(true);
        }

        return getDDForwardResolution("/customer/customer_smile_nimc_compare.jsp");
    }

    @DontValidate
    public Resolution retrieveCustomerDetailsForKyc() {
        checkPermissions(Permissions.VIEW_CUSTOMER);

        try {
            log.info("TestKYC NationalIdentityNumber " + getCustomer().getNationalIdentityNumber());
            byte[] fingerPrintData = Utils.getDataFromTempFile(getCustomer().getCustomerPhotographs().get(0).getPhotoGuid());

            setCustomerQuery(new CustomerQuery());
            getCustomerQuery().setNationalIdentityNumber(getCustomer().getNationalIdentityNumber());
            getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);
            getCustomerQuery().setResultLimit(1);
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));

            Photograph fingerPrint = null;
            for (Photograph photo : getCustomer().getCustomerPhotographs()) {
                log.warn("Found photoType111: [{}]", photo.getPhotoType());
                if (photo.getPhotoType().equalsIgnoreCase("photo")) {
                    fingerPrint = photo;
                }
            }
            getCustomer().getCustomerPhotographs().clear();
            if (fingerPrint != null) {
                getCustomer().getCustomerPhotographs().add(fingerPrint);
                getPhotographs().addAll(getCustomer().getCustomerPhotographs());
                writeCustomerPhotographsDataToFile(getPhotographs());
            } else {
                log.warn("NO Client Photo Found!");
            }

            /*if (BaseUtils.getBooleanProperty("env.compare.local.fingerprint", false)) {    
                boolean fingerPrintMatched = false;
                byte[] customerNewFinger = fingerPrintData;
                
                for (Photograph dbFingerPrint : getCustomer().getCustomerPhotographs()) {
                    log.warn("Found photoType: [{}]" , dbFingerPrint.getPhotoType());
                    if (dbFingerPrint.getPhotoType().equalsIgnoreCase("fingerprints")) {
                        log.warn("Fingerprint Found2, going to compare");
                        
                        byte[] candidateImage = dbFingerPrint.getData().getBytes();

                        FingerprintTemplate probe = new FingerprintTemplate(
                            new FingerprintImage()
                                .dpi(500)
                                .decode(customerNewFinger));

                        FingerprintTemplate candidate = new FingerprintTemplate(
                            new FingerprintImage()
                                .dpi(500)
                                .decode(candidateImage));
                        
                        double score = new FingerprintMatcher()
                                        .index(probe)
                                        .match(candidate);
                        if(score >= 40) {  
                            fingerPrintMatched=true;
                            break;
                        }
                    }
                }

                if (!fingerPrintMatched) {
                    setPageMessage("customer.finger.print.not.matched");
                   return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
                } 
            }*/
            setninVerified(true);
            return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
    }

    public Resolution retiveCustomerDetailsFromNimc() {
        checkPermissions(Permissions.VIEW_CUSTOMER);
        //checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_VERIFIED);
        log.info("TestA retiveCustomerDetailsFromNimc [{}]", getCustomer().getNationalIdentityNumber());

        setCustomerQuery(new CustomerQuery());
        getCustomerQuery().setNationalIdentityNumber(getCustomer().getNationalIdentityNumber());
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO_ADDRESS);
        getCustomerQuery().setResultLimit(1);
        setCustomer(SCAWrapper.getAdminInstance().getCustomer(getCustomerQuery()));

        // Retrieve the fingerprint data here
        if (BaseUtils.getBooleanProperty("env.sim.swap.use.nimc", false)) {
            try {
                log.info("TestA NationalIdentityNumber " + getCustomer().getNationalIdentityNumber());
                byte[] fingerPrintData = Utils.getDataFromTempFile(getCustomer().getCustomerPhotographs().get(0).getPhotoGuid());

                Photograph fingerPrint = null;
                for (Photograph photo : getCustomer().getCustomerPhotographs()) {
                    log.warn("Found photoType111: [{}]", photo.getPhotoType());
                    if (photo.getPhotoType().equalsIgnoreCase("photo")) {
                        fingerPrint = photo;
                    }
                }
                getCustomer().getCustomerPhotographs().clear();
                if (fingerPrint != null) {
                    getCustomer().getCustomerPhotographs().add(fingerPrint);
                    getPhotographs().addAll(getCustomer().getCustomerPhotographs());
                    writeCustomerPhotographsDataToFile(getPhotographs());
                } else {
                    log.warn("NO Photo Found!");
                }

                /*if (BaseUtils.getBooleanProperty("env.compare.local.fingerprint", false)) {    
                boolean fingerPrintMatched = false;
                byte[] customerNewFinger = fingerPrintData;
                
                for (Photograph dbFingerPrint : getCustomer().getCustomerPhotographs()) {
                    log.warn("Found photoType: [{}]" , dbFingerPrint.getPhotoType());
                    if (dbFingerPrint.getPhotoType().equalsIgnoreCase("fingerprints")) {
                        log.warn("Fingerprint Found2, going to compare");
                        
                        byte[] candidateImage = dbFingerPrint.getData().getBytes();

                        FingerprintTemplate probe = new FingerprintTemplate(
                            new FingerprintImage()
                                .dpi(500)
                                .decode(customerNewFinger));

                        FingerprintTemplate candidate = new FingerprintTemplate(
                            new FingerprintImage()
                                .dpi(500)
                                .decode(candidateImage));
                        
                        double score = new FingerprintMatcher()
                                        .index(probe)
                                        .match(candidate);
                        if(score >= 40) {  
                            fingerPrintMatched=true;
                            break;
                        }
                    }
                }

                if (!fingerPrintMatched) {
                    setninVerified(false); 
                    localiseErrorAndAddToGlobalErrors("customer.finger.print.not.matched");
                    return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
                   
                } 
            }*/
                VerifyNinQuery verifyNinQuery = new VerifyNinQuery();
                verifyNinQuery.setNin(getCustomer().getNationalIdentityNumber());

                if (BaseUtils.getBooleanProperty("env.nimc.use.fingerprint", true)) {
                    verifyNinQuery.setFingerStringInBase64(fingerPrintData);
                } else {
                    verifyNinQuery.setFingerStringInBase64(null);
                }

                verifyNinQuery.setVerifiedBy(getUser());
                verifyNinQuery.setEntityType("NEWCUSTOMER");
                VerifyNinResponseList verifyNinResponseList = SCAWrapper.getUserSpecificInstance().verifyNinCustomer_Direct(verifyNinQuery);
                VerifyNinReply ninResponse = verifyNinResponseList.getVerifyNinReplys().get(0);

                if (!ninResponse.getNin().isEmpty()) {
                    if (ninResponse.getNin().equalsIgnoreCase(getCustomer().getNationalIdentityNumber())) {
                        // setCustomer(new Customer());
                        getCustomer().setFirstName(ninResponse.getFirstname());
                        getCustomer().setMiddleName(ninResponse.getMiddlename());
                        getCustomer().setLastName(ninResponse.getSurname());
                        getCustomer().setGender(ninResponse.getGender());
                        getCustomer().setDateOfBirth(Utils.addSlashesToDate(ninResponse.getBirthdate()));
                        getCustomer().setNationalIdentityNumber(ninResponse.getNin());
                        getCustomer().setLanguage(ninResponse.getNspokenlang());
                        getCustomer().setNationality(BaseUtils.getProperty("env.country.name"));
                        getCustomer().setAlternativeContact1(ninResponse.getTelephoneno());
                        Address addrPhy = new Address();
                        addrPhy.setType("Physical Address");
                        addrPhy.setLine1(ninResponse.getResidenceAdressLine1());
                        addrPhy.setLine2(ninResponse.getResidenceAdressLine2());
                        addrPhy.setZone(ninResponse.getResidenceLga());
                        addrPhy.setTown(ninResponse.getResidenceTown());
                        addrPhy.setCode(ninResponse.getResidencePostalcode());
                        addrPhy.setState(ninResponse.getResidenceState());
                        addrPhy.setCountry(BaseUtils.getProperty("env.country.name"));
                        getCustomer().getAddresses().add(addrPhy);

                        byte[] fileData = Utils.decodeBase64(ninResponse.getPhoto());
                        String fileExtension = "jpg";
                        String photoGuid = Utils.getUUID() + "." + fileExtension;
                        File tmpFile = Utils.createTempFile(photoGuid, fileData);
                        Photograph photo = new Photograph();
                        photo.setPhotoGuid(photoGuid);
                        photo.setPhotoType("photo");

                        getCustomer().getCustomerPhotographs().clear();

                        getCustomer().getCustomerPhotographs().add(photo);
                        getPhotographs().addAll(getCustomer().getCustomerPhotographs());
                        writeCustomerPhotographsDataToFile(getPhotographs());

                        Customer compareCustomer = new Customer();
                        compareCustomer = getThirdPartyRegulatorData(getCustomer().getNationalIdentityNumber());
                        setninVerified(true);

                        customers.add(getCustomer());
                        customers.add(compareCustomer);
                        return getDDForwardResolution("/customer/customer_smile_nimc_compare.jsp");
                    } else {
                        localiseErrorAndAddToGlobalErrors("nimc.data.not.matched.message");
                        setninVerified(false);
                        return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
                    }
                } else {
                    setPageMessage("retrieve.customer.details.from.nimc", verifyNinResponseList.getReturnMessage());
                    setninVerified(false);
                    return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
                }
            } catch (Exception e) {
                String err="";
                if(e.getMessage().toString().toLowerCase().contains("with description")) {
                    err = e.getMessage().toString().substring(e.getMessage().toString().toLowerCase().indexOf("description"));
                } else {
                    err = e.getMessage().toString().substring(e.getMessage().toString().toLowerCase().indexOf("desc"));
                }
                localiseErrorAndAddToGlobalErrors("nimc.general.error.message", err);
                return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
            }
        } else {

            setCustomerQuery(new CustomerQuery());
            getCustomerQuery().setNationalIdentityNumber(getCustomer().getNationalIdentityNumber());
            getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO_ADDRESS);
            getCustomerQuery().setResultLimit(1);
            setCustomer(SCAWrapper.getAdminInstance().getCustomer(getCustomerQuery()));

            // Check if customer has fingerprints.
            Photograph facialphoto = null;
            for (Photograph photo : getCustomer().getCustomerPhotographs()) {
                if (photo.getPhotoType().equalsIgnoreCase("photo")) {
                    facialphoto = photo;
                }
            }
            //Clear all photos, and only add the fingerprint
            getCustomer().getCustomerPhotographs().clear();

            if (facialphoto != null) {
                getCustomer().getCustomerPhotographs().add(facialphoto);
                getPhotographs().addAll(getCustomer().getCustomerPhotographs());
                writeCustomerPhotographsDataToFile(getPhotographs());
            }
            setninVerified(true);
        }

        return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");

    }

    public boolean ninVerified = false;

    public boolean isNinVerified() {
        return ninVerified;
    }

    public void setninVerified(boolean nin) {
        this.ninVerified = nin;
    }

    public List<Customer> customers;

    public List<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
    }

    public Customer getThirdPartyRegulatorData(String nin) {

        log.warn("getThirdPartyRegulatorData with NIN [{}]", nin);
        PreparedStatement ps = null;
        Connection conn = null;
        Customer cust = new Customer();
        try {
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            conn.setAutoCommit(false);
            String query = "select nin, first_name,middle_name,surname,date_of_birth,Gender, nspoken_lang, nationality, telephone_no, Address, Verify_ID_Number_Status, photo from customer_nimc_data where nin =? limit 1";
            ps = conn.prepareStatement(query);
            ps.setString(1, nin);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                cust.setNationalIdentityNumber(rs.getString("nin"));
                cust.setFirstName(rs.getString(2));
                cust.setMiddleName(rs.getString(3));
                cust.setLastName(rs.getString(4));
                cust.setDateOfBirth(rs.getString(5));
                cust.setGender(rs.getString(6));
                cust.setLanguage(rs.getString(7));
                cust.setNationality(rs.getString(8));
                cust.setAlternativeContact1(rs.getString(9));

                Address addr = new Address();
                addr.setType("Physical Address");
                addr.setLine1(rs.getString("Address"));

                cust.getAddresses().add(addr);

                byte[] fileData = Utils.decodeBase64(new String(rs.getBlob("photo").getBytes((long) 1, (int) rs.getBlob("photo").length())));
                String fileExtension = "jpg";
                File tmpFile = Utils.createTempFile(Utils.getUUID() + "." + fileExtension, fileData);
                Photograph photo = new Photograph();
                photo.setPhotoGuid(tmpFile.getName());
                photo.setPhotoType("photo");

                cust.getCustomerPhotographs().clear();
                cust.getCustomerPhotographs().add(photo);
                getPhotographs().add(photo);

            }
            ps.close();
            conn.commit();
            conn.close();
        } catch (Exception ex) {
            log.error("Error occured getting 3rdparty data: " + ex);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    log.error("Error closing the prepared statement " + ex);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.error("Error closing the connection " + ex);
                }
            }
        }
        return cust;
    }

    public Resolution changeKYCPhotoToRegulatorPhoto() {
        //To avoid too many calls to SCA for NIN verifications we access DB directly here. This to be changed after ALL data is done and only one at a time is required        

        checkPermissions(Permissions.EDIT_CUSTOMER_PHOTOS);
        PreparedStatement ps = null;
        PreparedStatement psGet = null;
        Connection conn = null;

        try {

            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            conn.setAutoCommit(false);

            String q = "select photo, user_id, first_name, surname, nin from customer_nimc_data where nin = ? limit 1";
            psGet = conn.prepareStatement(q);
            psGet.setString(1, getCustomer().getNationalIdentityNumber());

            ResultSet rs = psGet.executeQuery();

            while (rs.next()) {
                String query = "update photograph set data = ? "
                        + " where customer_profile_id = ? and photo_type='photo'";
                ps = conn.prepareStatement(query);
                ps.setBlob(1, new SerialBlob(Utils.getDataFromTempFile(getParameter("thirdpartypic"))));
                ps.setInt(2, Integer.parseInt(rs.getString("user_id")));
                ps.executeUpdate();
                conn.commit();
                ps.close();

                Event eventData = new Event();
                eventData.setEventType("NinThirdPartyDataVerification");
                eventData.setEventSubType("DataVerify");
                eventData.setEventKey(rs.getString("user_id"));
                eventData.setEventData("PhotoReplaceWith3rdParty|"
                        + "NIN:" + rs.getString("nin") + "|"
                        + "FirstName:" + rs.getString("first_name") + "|"
                        + "Surname:" + rs.getString("surname") + "|"
                        + "PhotReplaceBy:" + getUser());
                SCAWrapper.getAdminInstance().createEvent(eventData);
            }
            psGet.close();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                ps.close();
                conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            localiseErrorAndAddToGlobalErrors("photo.edit.failed");
            return showCompareCustomerRegulatorData();
        } finally {
            if (ps != null || psGet != null) {
                try {
                    ps.close();
                    psGet.close();
                } catch (SQLException ex) {
                    log.error("Error closing the prepared statement " + ex);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.error("Error closing the connection " + ex);
                }
            }
        }

        setPageMessage("bulk.nin.verfied.message");
        return showCompareCustomerRegulatorData();
    }
    
    boolean addSim = false;

    public boolean isAddSim() {
        return addSim;
    }

    public void setAddSim(boolean addSim) {
        this.addSim = addSim;
    }
    
    boolean newSim = false;

    public boolean isNewSim() {
        return newSim;
    }

    public void setNewSim(boolean newSim) {
        this.newSim = newSim;
    }
    
    
    boolean simSwap = false;
    boolean noConsent = false;

    public boolean isSimSwap() {
        return simSwap;
    }

    public void setSimSwap(boolean simSwap) {
        this.simSwap = simSwap;
    }

    public boolean isNoConsent() {
        return noConsent;
    }

    public void setNoConsent(boolean noConsent) {
        this.noConsent = noConsent;
    }

    public Resolution uploadConsentForm() {
        setRetailSalesManagers(retrieveRetailStoreManagers());

        if (getParameter("noConsent") != null) {
            if (getParameter("noConsent").equals("true")) {
                setNoConsent(true);
            } else {
                setNoConsent(false);
            }
        }
        return getDDForwardResolution("/customer/upload_customer_consent.jsp");
    }

    public Resolution sendForApproval() {
        setRetailSalesManagers(retrieveRetailStoreManagers());

        if (getParameter("noConsent") != null) {
            if (getParameter("noConsent").equals("true")) {
                setNoConsent(true);
            } else {
                setNoConsent(false);
            }
        }
        return getDDForwardResolution("/customer/upload_customer_consent.jsp");
    }

    public Resolution doConsentFormUpload() {
        if (getParameter("rsm") == null || getParameter("rsm").equals("-1")) {
            localiseErrorAndAddToGlobalErrors("", "Please supply approver/manager");
            return uploadConsentForm();
        }

        if (getCustomer() != null) {
            CustomerQuery q = new CustomerQuery();
            q.setNationalIdentityNumber(getCustomer().getNationalIdentityNumber());
            q.setResultLimit(1);
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);

            Customer tmpCust = SCAWrapper.getAdminInstance().getCustomer(q);

            PreparedStatement ps = null;
            Connection conn = null;

            try {

                conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
                conn.setAutoCommit(false);

                String query = "insert into nin_verify_consent (customer_profile_id, nin, consent_document, file_type, requestor_profile_id, approved_by_profile_id, status) "
                        + "values (?,?,?,?,?,?, 'N') "
                        + "ON DUPLICATE KEY UPDATE CONSENT_DOCUMENT =?, file_type=?, requestor_profile_id=?, approved_by_profile_id=?, STATUS=?";

                ps = conn.prepareStatement(query);
                ps.setInt(1, tmpCust.getCustomerId());
                ps.setString(2, tmpCust.getNationalIdentityNumber());

                if (getPhotographs().size() > 0) {
                    ps.setString(3, Utils.encodeBase64(Utils.getDataFromTempFile(getPhotographs().get(0).getPhotoGuid())));
                    ps.setString(4, getPhotographs().get(0).getPhotoGuid().substring(getPhotographs().get(0).getPhotoGuid().indexOf(".") + 1));
                } else {
                    ps.setString(3, null);
                    ps.setString(4, null);
                }

                ps.setInt(5, getUserCustomerIdFromSession());
                ps.setInt(6, Integer.parseInt(getParameter("rsm")));

                if (getPhotographs().size() > 0) {
                    ps.setString(7, Utils.encodeBase64(Utils.getDataFromTempFile(getPhotographs().get(0).getPhotoGuid())));
                    ps.setString(8, getPhotographs().get(0).getPhotoGuid().substring(getPhotographs().get(0).getPhotoGuid().indexOf(".") + 1));
                } else {
                    ps.setString(7, null);
                    ps.setString(8, null);
                }

                ps.setInt(9, getUserCustomerIdFromSession());
                ps.setInt(10, Integer.parseInt(getParameter("rsm")));
                ps.setString(11, "N");

                try {
                    
                    ps.executeUpdate();
                    
                    conn.commit();

                    ps.close();
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.warn("Unable to dropFIle: {}", e.getCause());
                    ps.close();
                    conn.close();
                    localiseErrorAndAddToGlobalErrors("system.error.please.retry");
                    return getDDForwardResolution("/index.jsp");
                } finally {
                    try {
                        ps.close();
                        conn.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                CustomerQuery rsmQ = new CustomerQuery();
                rsmQ.setCustomerId(Integer.parseInt(getParameter("rsm")));
                rsmQ.setResultLimit(1);
                rsmQ.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);

                Customer rsm = SCAWrapper.getAdminInstance().getCustomer(rsmQ);
                try {
                    IMAPUtils.sendEmail(BaseUtils.getProperty("env.customercare.email.address"),
                            rsm.getEmailAddress(),
                            "SIM Swap Approval Request",
                            "Hi " + rsm.getFirstName() + ", A SIM Swap approval request has been sent to you for your approval. Please logon to SEP to approve.");

                } catch (Exception e) {
                    log.warn("Error sending email: [{}]", e.getMessage());
                    e.printStackTrace();
                }

                setPageMessage("customer.updated.successfully");

            } catch (Exception e) {
                log.warn("Error while creating consent from: [{}]", e.getMessage());
                e.printStackTrace();
                try {
                    ps.close();
                    conn.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        }

        return getDDForwardResolution("/index.jsp");
    }

    public List<Customer> retrieveRetailStoreManagers() {
        List<Customer> rsm = new ArrayList();
        List<String> allowedApprovers = BaseUtils.getPropertyAsList("env.nimc.update.approvers.security.groups");
        String q = "select distinct cp.CUSTOMER_PROFILE_ID, cp.FIRST_NAME, cp.LAST_NAME from customer_profile cp inner join security_group_membership sgm on "
                + "cp.CUSTOMER_PROFILE_ID = sgm.CUSTOMER_PROFILE_ID inner join customer_role cr on cr.CUSTOMER_PROFILE_ID = cp.CUSTOMER_PROFILE_ID "
                + "where sgm.SECURITY_GROUP_NAME in (";

        int i = 0;
        for (String approver : allowedApprovers) {

            if (i == 0) {
                q = q + "'" + approver + "'";
            } else {
                q = q + ",'" + approver + "'";
            }
            i++;
        }

        q += ") and cr.ORGANISATION_ID = 1";

        PreparedStatement ps = null;
        Connection conn = null;

        try {
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            ps = conn.prepareStatement(q);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Customer tmpCust = new Customer();
                tmpCust.setCustomerId(rs.getInt("CUSTOMER_PROFILE_ID"));
                tmpCust.setFirstName(rs.getString("FIRST_NAME"));
                tmpCust.setLastName(rs.getString("LAST_NAME"));

                rsm.add(tmpCust);
                tmpCust = null;
            }
            conn.close();
            ps.close();
        } catch (Exception e) {

            e.printStackTrace();
            try {
                conn.close();
                ps.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
        return rsm;
    }

    List<Customer> retailSalesManagers;

    public List<Customer> getRetailSalesManagers() {
        return retailSalesManagers;
    }

    public void setRetailSalesManagers(List<Customer> retailSalesManagers) {
        this.retailSalesManagers = retailSalesManagers;
    }

    public Resolution showConsentForm() {
        checkPermissions(Permissions.EDIT_CUSTOMER_PHOTOS);
        PreparedStatement ps = null;
        Connection conn = null;
        List<Customer> custlist = new ArrayList<>();

        try {
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            String query = "select CUSTOMER_PROFILE_ID,NIN,CONSENT_DOCUMENT, FILE_TYPE from nin_verify_consent where (STATUS ='' or STATUS is null or STATUS='N') and APPROVED_BY_PROFILE_ID=?";
            ps = conn.prepareStatement(query);
            ps.setString(1, String.valueOf(getUserCustomerIdFromSession()));

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Customer cust = new Customer();
                cust.setCustomerId(rs.getInt(1));
                cust.setNationalIdentityNumber(rs.getString(2));
                if (rs.getBlob(3) != null) {
                    log.warn("Document: [{}]", rs.getBlob(3));
                    byte[] fileData = Utils.decodeBase64(new String(rs.getBlob(3).getBytes((long) 1, (int) rs.getBlob(3).length())));
                    String fileExtension = rs.getString(4);
                    File tmpFile = Utils.createTempFile(Utils.getUUID() + "." + fileExtension, fileData);
                    Photograph photo = new Photograph();
                    photo.setPhotoGuid(tmpFile.getName());
                    photo.setPhotoType("photo");

                    cust.getCustomerPhotographs().add(photo);
                    getPhotographs().add(photo);
                }
                custlist.add(cust);

                log.warn("Customer ID: [{}]", rs.getInt(1));
            }
            
            CustomerList customerList = new CustomerList();
            customerList.getCustomers().addAll(custlist);
            setCustomerList(customerList);
            ps.close();
            conn.close();
        } catch (Exception ex) {
            log.error("Error occured getting 3rdparty data: " + ex);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    log.error("Error closing the prepared statement " + ex);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.error("Error closing the connection " + ex);
                }
            }
        }

        return getDDForwardResolution("/customer/view_consent_form.jsp");
    }

    public Resolution approveConsentForm() {
        log.debug("Inside approveConsentForm()");
        checkCSRF();
//        checkPermissions(Permissions.EDIT_CUSTOMER_DATA);
        PreparedStatement ps = null;
        Connection conn = null;
        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(getCustomer().getCustomerId());
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_CUSTOMERNINDATA);
        Customer tmpCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(q);
        setCustomer(tmpCustomer);

        log.debug("approveConsentForm() With Customer ID [{}]", getCustomer().getCustomerId());

        if (getCustomer().getNationalIdentityNumber() != null && !getCustomer().getNationalIdentityNumber().isEmpty()) {
            if (getCustomer().getNationalIdentityNumber().equalsIgnoreCase(tmpCustomer.getNationalIdentityNumber())) {
                try {
                    // Check if customer has fingerprints.
                    CustomerQuery custQuery = new CustomerQuery();
                    custQuery.setCustomerId(getCustomer().getCustomerId());
                    custQuery.setResultLimit(1);
                    custQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_PHOTO);
                    Customer customer = SCAWrapper.getAdminInstance().getCustomer(custQuery);

                    byte[] fingerPrintData = null;
                    log.debug("approveConsentForm() Entered");
                    if (BaseUtils.getBooleanProperty("env.sim.swap.fingerprint.mandatory", false)) {
                        for (Photograph photo : customer.getCustomerPhotographs()) {
                            if (photo.getPhotoType().equalsIgnoreCase("fingerprints")) {
                                String fileName = photo.getPhotoGuid();
                                try {
                                    Utils.createTempFile(fileName, Utils.decodeBase64(photo.getData()));
                                    fingerPrintData = Utils.getDataFromTempFile(photo.getPhotoGuid());
                                    log.debug("Photo " + fingerPrintData);
                                } catch (Exception e) {
                                    log.debug("Error in writeCustomerPhotographsDataToFile", e);
                                }
                            }
                        }
                    }
                    VerifyNinQuery verifyNinQuery = new VerifyNinQuery();
                    verifyNinQuery.setNin(getCustomer().getNationalIdentityNumber());
                    verifyNinQuery.setFirstName(getCustomer().getFirstName());
                    verifyNinQuery.setLastName(getCustomer().getLastName());
                    verifyNinQuery.setSurname(getCustomer().getLastName());
                    verifyNinQuery.setVerifiedBy(getUser());
                    verifyNinQuery.setEntityId(Integer.toString(getCustomer().getCustomerId()));
                    verifyNinQuery.setEntityType("UPDATECUSTOMER");
                    CustomerNinData customerNinData = new CustomerNinData();
                    if (fingerPrintData != null) {
                        verifyNinQuery.setFingerStringInBase64(fingerPrintData);
                        customerNinData.setNinVerificationType("NIN & Fingerprint");
                    } else {
                        verifyNinQuery.setFingerStringInBase64(null);
                        customerNinData.setNinVerificationType("NIN only");
                    }

                    VerifyNinResponseList verifyNinResponseList = SCAWrapper.getUserSpecificInstance().verifyNinCustomer_Direct(verifyNinQuery);
                    VerifyNinReply verifyNinReply = verifyNinResponseList.getVerifyNinReplys().get(0);
                    if (verifyNinResponseList.getReturnMessage().equalsIgnoreCase(BaseUtils.getProperty("env.nin.status.message"))) {
                        if (!verifyNinReply.getNin().isEmpty()) {
                            SimpleDateFormat sdfo1 = new SimpleDateFormat("dd-MM-yyyy");
                            SimpleDateFormat sdfo2 = new SimpleDateFormat("yyyyMMdd");
                            Date d1 = sdfo1.parse(verifyNinReply.getBirthdate());
                            Date d2 = sdfo2.parse(customer.getDateOfBirth());
                            if (!verifyNinReply.getFirstname().equalsIgnoreCase(customer.getFirstName()) || !verifyNinReply.getSurname().equalsIgnoreCase(customer.getLastName()) || !d1.equals(d2)) {
                                log.debug("approveConsentForm() Updating");
                                log.debug("Date Of Birth Date [{}]", verifyNinReply.getBirthdate());
                                Date date = new SimpleDateFormat("dd-MM-yyyy").parse(verifyNinReply.getBirthdate());
                                SimpleDateFormat DateFor = new SimpleDateFormat("yyyy/MM/dd");
                                String stringDate = DateFor.format(date);
                                getCustomer().setFirstName(verifyNinReply.getFirstname());
                                getCustomer().setLastName(verifyNinReply.getSurname());
                                log.debug("Date Of Birth Date formated[{}]", stringDate);
                                log.debug("Date Without Slashes [{}]", Utils.getDateWithoutSlashes(stringDate));
                                getCustomer().setDateOfBirth(Utils.getDateWithoutSlashes(stringDate));
                            }
                            getCustomer().setIsNinVerified("Y");
                            customerNinData.setCustomerProfileId(getCustomer().getCustomerId());
                            customerNinData.setNinVerificationTrackingId(verifyNinResponseList.getVerifyNinReplys().get(0).getTrackingId());
                            Date date1 = Calendar.getInstance().getTime();
                            DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                            String createdDate1 = dateFormat1.format(date1);
                            customerNinData.setNinVerifiedDate(createdDate1);
                            customerNinData.setNinResponseStatus(verifyNinResponseList.getReturnMessage());
                            if (tmpCustomer.getCustomerNinData() != null && (tmpCustomer.getCustomerNinData().getNinCollectionDate() != null && !tmpCustomer.getCustomerNinData().getNinCollectionDate().isEmpty())) {
                                    customerNinData.setNinCollectionDate(tmpCustomer.getCustomerNinData().getNinCollectionDate());                                
                            } else {
                                customerNinData.setNinCollectionDate("");
                            }
                            getCustomer().setCustomerNinData(customerNinData);
                            SCAWrapper.getUserSpecificInstance().modifyCustomer(getCustomer());
                            try {

                                conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
                                String query = "UPDATE nin_verify_consent SET  STATUS = 'Y' WHERE CUSTOMER_PROFILE_ID = ?";

                                ps = conn.prepareStatement(query);
                                ps.setInt(1, getCustomer().getCustomerId());

                                ps.executeUpdate();
                                conn.commit();
                                ps.close();
                                conn.close();

                            } catch (Exception e) {
                                try {
                                    ps.close();
                                    conn.close();
                                } catch (Exception ex) {
                                }

                                e.printStackTrace();

                            }
                        }

                        setPageMessage("customer.updated.successfully");
                    } else {
                        getCustomer().setIsNinVerified("N");
                        customerNinData.setCustomerProfileId(getCustomer().getCustomerId());
                        customerNinData.setNinVerificationTrackingId(verifyNinResponseList.getReturnMessage());
                        Date date1 = Calendar.getInstance().getTime();
                        DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                        String createdDate1 = dateFormat1.format(date1);
                        customerNinData.setNinVerifiedDate(createdDate1);
                        customerNinData.setNinResponseStatus(verifyNinResponseList.getReturnMessage());
                        if (tmpCustomer.getCustomerNinData() != null) {
                            if ((tmpCustomer.getCustomerNinData().getNinCollectionDate() != null && !tmpCustomer.getCustomerNinData().getNinCollectionDate().isEmpty())) {
                                customerNinData.setNinCollectionDate(tmpCustomer.getCustomerNinData().getNinCollectionDate());
                            }
                        } else {
                            customerNinData.setNinCollectionDate("");
                        }
                        getCustomer().setCustomerNinData(customerNinData);
                        SCAWrapper.getUserSpecificInstance().modifyCustomer(getCustomer());
                        setPageMessage("customer.updated.successfully");
                    }

                } catch (Exception e) {
                    CustomerNinData customerNinData = new CustomerNinData();
                    customerNinData.setCustomerProfileId(getCustomer().getCustomerId());
                    Date date1 = Calendar.getInstance().getTime();
                    DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    String createdDate1 = dateFormat1.format(date1);
                    customerNinData.setNinVerifiedDate(createdDate1);
                    customerNinData.setNinResponseStatus("Error");
                    if (tmpCustomer.getCustomerNinData() != null) {
                     if ((tmpCustomer.getCustomerNinData().getNinCollectionDate() != null && !tmpCustomer.getCustomerNinData().getNinCollectionDate().isEmpty())) {
                            customerNinData.setNinCollectionDate(tmpCustomer.getCustomerNinData().getNinCollectionDate());
                            customerNinData.setNinVerificationTrackingId(tmpCustomer.getCustomerNinData().getNinVerificationTrackingId());
                        }
                    } else {
                        customerNinData.setNinVerificationTrackingId("");
                        customerNinData.setNinCollectionDate("");
                    }
                    getCustomer().setCustomerNinData(customerNinData);
                    SCAWrapper.getUserSpecificInstance().modifyCustomer(getCustomer());
                    String err="";
                    if(e.getMessage().toString().toLowerCase().contains("with description")) {
                        err = e.getMessage().toString().substring(e.getMessage().toString().toLowerCase().indexOf("description"));
                    } else {
                        err = e.getMessage().toString().substring(e.getMessage().toString().toLowerCase().indexOf("desc"));
                    }
                    localiseErrorAndAddToGlobalErrors("nimc.general.error.message", err);
                    log.debug("NIN Verification in updateCustomer() [{}]", e);
                }
            }
        }

        return showConsentForm();

    }

    public Resolution declineConsentForm() {
        checkCSRF();
        checkPermissions(Permissions.EDIT_CUSTOMER_DATA);

        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(getCustomer().getCustomerId());
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
        Customer tmpCustomer = SCAWrapper.getUserSpecificInstance().getCustomer(q);
        setCustomer(tmpCustomer);
        PreparedStatement ps = null;
        Connection conn = null;

        try {

            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            String query = "UPDATE nin_verify_consent SET  STATUS = 'N' WHERE CUSTOMER_PROFILE_ID = ?";

            ps = conn.prepareStatement(query);
            ps.setInt(1, getCustomer().getCustomerId());

            //log.warn(ps.toString());
            ps.executeUpdate();
            conn.commit();
            ps.close();
            conn.close();

        } catch (Exception e) {
            try {
                ps.close();
                conn.close();
            } catch (Exception ex) {
            }

            e.printStackTrace();

        }
        log.debug("declineConsentForm() Deliced completed");

        return showConsentForm();
    }

    public boolean hasVerifiedNIMCStatus() {
        boolean isApproved = false;

        PreparedStatement ps = null;
        Connection conn = null;
        List<Customer> custlist = new ArrayList<>();

        try {
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            String query = "select IS_NIN_VERIFIED,nvc.status as consent_status from nin_verify_consent nvc, customer_profile cp where cp.customer_profile_id=nvc.customer_profile_id and cp.customer_profile_id=? limit 1";
            ps = conn.prepareStatement(query);
            ps.setInt(1, getCustomer().getCustomerId());

            //log.warn("SQL: [{}]", ps.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                isApproved = (rs.getString("IS_NIN_VERIFIED").equalsIgnoreCase("Y") || rs.getString("IS_NIN_VERIFIED").equalsIgnoreCase("V")) && rs.getString("consent_status").equalsIgnoreCase("Y");
                break;
            }
            ps.close();
            conn.close();

            return isApproved;
        } catch (Exception ex) {
            log.error("Error occured verify status data: " + ex);
            isApproved = false;
        } finally {
            isApproved = false;
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    log.error("Error closing the prepared statement " + ex);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.error("Error closing the connection " + ex);
                }
            }
        }

        return isApproved;
    }

    public List<Customer> retrieveAvailableSellers(String secGroup) {
        PreparedStatement ps = null;
        Connection conn = null;
        List<Customer> sellers = new ArrayList<Customer>();
        try {
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            conn.setAutoCommit(false);
            String q = "select distinct cp.customer_profile_id,cp.first_name, cp.last_name from customer_profile cp inner join security_group_membership sgm on "
                    + "cp.CUSTOMER_PROFILE_ID = sgm.CUSTOMER_PROFILE_ID inner join customer_role cr on cr.CUSTOMER_PROFILE_ID = cp.CUSTOMER_PROFILE_ID "
                    + "where sgm.SECURITY_GROUP_NAME like '%" + secGroup + "%'";

            ps = conn.prepareStatement(q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Customer cust = new Customer();
                cust.setCustomerId(rs.getInt(1));
                cust.setFirstName(rs.getString(2));
                cust.setLastName(rs.getString(3));
                sellers.add(cust);
            }
            ps.close();
            conn.commit();
            conn.close();
        } catch (Exception ex) {
            log.warn("Error occured getting sellers data: " + ex);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    log.warn("Error closing the prepared statement " + ex);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.warn("Error closing the connection " + ex);
                }
            }
        }

        return sellers;
    }

    public Resolution proceedToSIMSwap() {
        checkPermissions(Permissions.SIM_SWAP);
        return getDDForwardResolution("/sim/perform_sim_swap.jsp");
    }
    
    public Resolution proceedToQuote() { 
        checkPermissions(Permissions.MAKE_SALE);
        return new ForwardResolution(SalesActionBean.class).addParameter("submit","proceedToQuote");
    }
    
    public Resolution proceedToAddProduct() {
        checkPermissions(Permissions.ADD_PRODUCT_INSTANCE);
        return new ForwardResolution(ProductCatalogActionBean.class).addParameter("submit","proceedToAddProduct");
    }
    
    public Resolution verifyNin() {
        checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_VERIFIED);        
        CustomerQuery q = new CustomerQuery();
        q.setNationalIdentityNumber(getCustomer().getNationalIdentityNumber());
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
        Customer tmpCust = SCAWrapper.getAdminInstance().getCustomer(q);
        setCustomer(tmpCust);
        String oldStatus = getCustomer().getIsNinVerified();
        getCustomer().setIsNinVerified("Y");
        SCAWrapper.getUserSpecificInstance().modifyCustomer(getCustomer());
        
        Event eventData = new Event();
        eventData.setEventType("IM");
        eventData.setEventSubType("VERIFY_NIN");
        eventData.setEventKey(getCustomer().getNationalIdentityNumber());
        eventData.setEventData(getCustomer().getNationalIdentityNumber() + "|" + oldStatus + "|" + getCustomer().getIsNinVerified() + "|" + String.valueOf(getUserCustomerIdFromSession()));
        SCAWrapper.getAdminInstance().createEvent(eventData);
        
        setPageMessage("customer.updated.successfully");
        return showCompareCustomerRegulatorData();
    }
    
    public Resolution verifyNinAndActivateService() {
        checkPermissions(Permissions.EDIT_CUSTOMER_KYC_STATUS_TO_VERIFIED);        
        checkPermissions(Permissions.REACTIVATE_SERVICE_INSTANCE);        
        
        if(getCustomer().getNationalIdentityNumber()==null || getCustomer().getNationalIdentityNumber().isEmpty()) {
            localiseErrorAndAddToGlobalErrors("error", "Cannot activate services as client profile does not have a NIN.");
            return retrieveKycProducts();
        }        
        
        CustomerQuery q = new CustomerQuery();
        q.setNationalIdentityNumber(getCustomer().getNationalIdentityNumber());
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
        Customer tmpCust = SCAWrapper.getAdminInstance().getCustomer(q);
        setCustomer(tmpCust);
        
        if(!getCustomer().getKYCStatus().equalsIgnoreCase("V")) {
            localiseErrorAndAddToGlobalErrors("error", "Cannot activate services as client profile is not KYC Verified.");
            return retrieveKycProducts();
        }
        String oldStatus = getCustomer().getIsNinVerified();        
        getCustomer().setIsNinVerified("Y");
        SCAWrapper.getUserSpecificInstance().modifyCustomer(getCustomer());
        
        if(BaseUtils.getProperty("env.country.name").trim().equalsIgnoreCase("Nigeria") && !getCustomer().getIsNinVerified().equalsIgnoreCase("Y")) {
            localiseErrorAndAddToGlobalErrors("nin.not.verified");
            return retrieveKycProducts();
        }
        
        activateCustomerNewServices(getCustomer().getCustomerId());        
        removeVerifyRequest(getCustomer().getCustomerId());

        Event eventData = new Event();
        eventData.setEventType("IM");        
        eventData.setEventSubType("VERIFY_NIN_AND_ACTIVATE_SERVICES");
        eventData.setEventKey(getCustomer().getNationalIdentityNumber());
        eventData.setEventData(getCustomer().getNationalIdentityNumber() + "|" + oldStatus + "|" + getCustomer().getIsNinVerified() + "|" + String.valueOf(getUserCustomerIdFromSession()));
        SCAWrapper.getAdminInstance().createEvent(eventData);
                
        setPageMessage("customer.updated.successfully");
        return retrieveKycProducts();
    }
    
    public void removeVerifyRequest(int customerId) {       
        PreparedStatement ps = null;
        Connection conn = null;
        try {           
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            String query = "update products_kyc_verification set status='CO', verified_by=?, verification_date=now() where customer_id=?";

            ps = conn.prepareStatement(query);            
            ps.setInt(1, getUserCustomerIdFromSession());
            ps.setInt(2, customerId);

            //log.warn(ps.toString());
            ps.executeUpdate();
            conn.commit();
            ps.close();
            conn.close();

        } catch (Exception e) {
            log.warn("Error updating verified status to CO {}", e);
            try {
                ps.close();
                conn.close();
            } catch (Exception ex) {
            }
            e.printStackTrace();
            
        }
    }
    
    public void activateCustomerNewServices(int customerId) {
        checkPermissions(Permissions.REACTIVATE_SERVICE_INSTANCE);
        
        for(int serviceId : getCustomerNewServices(customerId) ) {
            
            ServiceInstanceQuery q = new ServiceInstanceQuery();
            q.setServiceInstanceId(serviceId);
            q.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            ServiceInstance si = SCAWrapper.getUserSpecificInstance().getServiceInstance(q);
            
            ProductOrder order = new ProductOrder();
            order.setProductInstanceId(si.getProductInstanceId());
            order.setAction(StAction.NONE);
            order.setCustomerId(customerId);
            order.getServiceInstanceOrders().add(new ServiceInstanceOrder());
            order.getServiceInstanceOrders().get(0).setAction(StAction.UPDATE);
            order.getServiceInstanceOrders().get(0).setServiceInstance(si);
            order.getServiceInstanceOrders().get(0).getServiceInstance().setStatus("AC");

            try {
                SCAWrapper.getUserSpecificInstance().processOrder(order);
            } catch (SCABusinessError e) {                
                throw e;
            }
        }
        
    }
    
    public List<Integer> getCustomerNewServices(int customerId) {
        PreparedStatement ps = null;
        Connection conn = null;
        List<Integer> newCustomerServices = new ArrayList<Integer>();
        try {
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            conn.setAutoCommit(false);
            String q = "select distinct service_instance_id from service_instance where customer_profile_id=? and status='TD' and CREATED_DATETIME>?";
            
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(new Date());
            cal.add(Calendar.DATE, -3);
            Date date = cal.getTime();
            
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String createdDate = dateFormat.format(date);
                
            ps = conn.prepareStatement(q);
            ps.setInt(1, customerId);
            ps.setString(2, createdDate);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {                
                newCustomerServices.add(rs.getInt("service_instance_id"));
            }
            ps.close();
            conn.commit();
            conn.close();
        } catch (Exception ex) {
            log.warn("Error occured getting services data: " + ex);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    log.warn("Error closing the prepared statement " + ex);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.warn("Error closing the connection " + ex);
                }
            }
        }
        
        return newCustomerServices;
    }

    public Resolution unVerifyNin() {
        CustomerQuery q = new CustomerQuery();
        q.setNationalIdentityNumber(getCustomer().getNationalIdentityNumber());
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
        Customer tmpCust = SCAWrapper.getAdminInstance().getCustomer(q);
        setCustomer(tmpCust);
        String oldStatus = getCustomer().getIsNinVerified();
        getCustomer().setIsNinVerified("N");
        SCAWrapper.getUserSpecificInstance().modifyCustomer(getCustomer());
        
        Event eventData = new Event();
        eventData.setEventType("IM");
        eventData.setEventSubType("UNVERIFY_NIN");
        eventData.setEventKey(getCustomer().getNationalIdentityNumber());
        eventData.setEventData(getCustomer().getNationalIdentityNumber() + "|" + oldStatus + "|" + getCustomer().getIsNinVerified() + "|" + String.valueOf(getUserCustomerIdFromSession()));
        SCAWrapper.getAdminInstance().createEvent(eventData);
        
        setPageMessage("customer.updated.successfully");
        return showCompareCustomerRegulatorData();
    }

    public void retrieveLegalContacts() {
        checkPermissions(Permissions.EDIT_ORGANISATION);
        PreparedStatement ps = null;
        Connection conn = null;
        List<OrganisationLegalContact> organisationLegalContacts = new ArrayList<OrganisationLegalContact>();
        
        try {            
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            String query = "select * from organisation_legal_contact where organisation_id = ? and status='AC'";
            ps = conn.prepareStatement(query);
            ps.setInt(1, getOrganisation().getOrganisationId());
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                OrganisationLegalContact organisationContact = new OrganisationLegalContact();
                organisationContact.setLegalContactId(rs.getInt("legal_contact_id"));
                organisationContact.setOrganisationId(rs.getInt("organisation_id"));
                organisationContact.setNin(rs.getString("nin"));
                organisationContact.setLegalContactType(rs.getString("legal_contact_type"));
                organisationContact.setFirstName(rs.getString("first_name"));
                organisationContact.setLastName(rs.getString("last_name"));
                organisationContact.setIccid(rs.getString("iccid"));
                organisationContact.setTelNumber(rs.getString("tel_number"));
                organisationContact.setEmail(rs.getString("email"));
                organisationContact.setIsNinVerified(rs.getString("is_nin_verified"));
                organisationContact.setIsKycVerified(rs.getString("is_kyc_verified"));
                organisationContact.setCreatedDate(rs.getString("created_date"));
                
                organisationLegalContacts.add(organisationContact);                
            } 
            
            setOrganisationLegalContacts(organisationLegalContacts);
        } catch (Exception ex) {
            log.error("Error occured getting legal contacts: " + ex);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    log.error("Error closing the prepared statement " + ex);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.error("Error closing the connection " + ex);
                }
            }
        }
    }

    public Resolution showOrganisationLegalContact() {
        retrieveLegalContacts();
        return getDDForwardResolution("/organisation/organisation_legal_contact.jsp");
    }

    public Resolution addLegalContact() {
        
        setOrganisationQuery(new OrganisationQuery());
        getOrganisationQuery().setOrganisationId(getOrganisation().getOrganisationId());
        getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN);

        setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
                
        return getDDForwardResolution("/organisation/add_legal_contact.jsp");
    }
    
    public Resolution retrieveLegalContactNinData() {
        if (getParameter("nin") == null || getParameter("nin").length()<11) {
            localiseErrorAndAddToGlobalErrors("validation.errors", "Incorrect NIN supplied. NIN must be 11 digits.");
            return getDDForwardResolution("/organisation/add_legal_contact.jsp");
        }
        
        VerifyNinQuery verifyNinQuery = new VerifyNinQuery();
        verifyNinQuery.setNin(getParameter("nin").trim());

        /*if (BaseUtils.getBooleanProperty("env.nimc.use.fingerprint", true)) {
            byte[] fingerPrintData = Utils.getDataFromTempFile(getCustomer().getCustomerPhotographs().get(0).getPhotoGuid());
            verifyNinQuery.setFingerStringInBase64(fingerPrintData);
        } else {
            verifyNinQuery.setFingerStringInBase64(null);
        }*/

        verifyNinQuery.setVerifiedBy(getUser());
        verifyNinQuery.setEntityType("NEWCUSTOMER");
        VerifyNinResponseList verifyNinResponseList = SCAWrapper.getAdminInstance().verifyNinCustomer_Direct(verifyNinQuery);
        VerifyNinReply verifyNinReply = verifyNinResponseList.getVerifyNinReplys().get(0);

        log.warn("RETRUNED FROM NIMC: {}", verifyNinResponseList.getReturnMessage());
        if (verifyNinResponseList.getReturnMessage().equalsIgnoreCase(BaseUtils.getProperty("env.nin.status.message"))) {
            OrganisationLegalContact organisationLegalContact = new OrganisationLegalContact();
            
            organisationLegalContact.setOrganisationId(getOrganisation().getOrganisationId());
            organisationLegalContact.setFirstName(verifyNinReply.getFirstname());
            organisationLegalContact.setLastName(verifyNinReply.getSurname());
            organisationLegalContact.setNin(verifyNinReply.getNin());
            organisationLegalContact.setTelNumber(verifyNinReply.getTelephoneno());
            organisationLegalContact.setEmail(verifyNinReply.getEmail());
            
            setOrganisationLegalContact(organisationLegalContact);
        } else {
            localiseErrorAndAddToGlobalErrors("", verifyNinResponseList.getReturnMessage());
        }
        
        return getDDForwardResolution("/organisation/add_legal_contact.jsp");
    }
    
    public Resolution kycLegalContact() {
        
        setOrganisationQuery(new OrganisationQuery());
        getOrganisationQuery().setOrganisationId(getOrganisation().getOrganisationId());
        getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN);

        setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
        
        PreparedStatement ps = null;
        Connection conn = null;
        List<Customer> custlist = new ArrayList<>();

        try {
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            String query = "select * from organisation_legal_contact where nin=? and iccid=?";
            ps = conn.prepareStatement(query);            
            ps.setString(1, getParameter("updatenin"));            
            ps.setString(2, getParameter("updateiccid"));
            
            ResultSet rs = ps.executeQuery();
            
            OrganisationLegalContact organisationContact = new OrganisationLegalContact();
            while (rs.next()) {                
                organisationContact.setLegalContactId(rs.getInt("legal_contact_id"));
                organisationContact.setOrganisationId(rs.getInt("organisation_id"));
                organisationContact.setNin(rs.getString("nin"));
                organisationContact.setLegalContactType(rs.getString("legal_contact_type"));
                organisationContact.setFirstName(rs.getString("first_name"));
                organisationContact.setLastName(rs.getString("last_name"));
                organisationContact.setIccid(rs.getString("iccid"));
                organisationContact.setTelNumber(rs.getString("tel_number"));
                organisationContact.setEmail(rs.getString("email"));
                organisationContact.setIsNinVerified(rs.getString("is_nin_verified"));
                organisationContact.setIsKycVerified(rs.getString("is_kyc_verified"));
                organisationContact.setCreatedDate(rs.getString("created_date"));                
               
                if (rs.getBlob("photo_data") != null) {                    
                    byte[] fileData = Utils.decodeBase64(new String(rs.getBlob("photo_data").getBytes((long) 1, (int) rs.getBlob("photo_data").length())));
                    String fileExtension = rs.getString("file_type");
                    File tmpFile = Utils.createTempFile(Utils.getUUID() + "." + fileExtension, fileData);
                    Photograph photo = new Photograph();
                    photo.setPhotoGuid(tmpFile.getName());
                    photo.setPhotoType("photo");
                    
                    getOrganisation().getOrganisationPhotographs().clear();
                    getOrganisation().getOrganisationPhotographs().add(photo);
                    getPhotographs().add(photo);
                }
            }
            
            setOrganisationLegalContact(organisationContact);
            ps.close();
            conn.close();
        } catch (Exception ex) {
            log.error("Error occured getting 3rdparty data: " + ex);
            localiseErrorAndAddToGlobalErrors("validation.errors", "Error while verifying legal contact: " +ex);
            return showOrganisationLegalContact();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    log.error("Error closing the prepared statement " + ex);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.error("Error closing the connection " + ex);
                    

                }
            }
        }
       
        
        return getDDForwardResolution("/organisation/kyc_verify_contact.jsp");
    }
    
    
    public Resolution verifyLegalContacts() {
        PreparedStatement ps = null;
        Connection conn = null;
        
        log.debug("IN Verify Legal Contacts");
        setOrganisationQuery(new OrganisationQuery());
        getOrganisationQuery().setOrganisationId(getOrganisation().getOrganisationId());
        getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN);

        setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
        
        OrganisationLegalContact organisationLegalContact = getOrganisationLegalContact();
        try {
            
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            conn.setAutoCommit(false);
            String query = "update organisation_legal_contact set is_kyc_verified='Y' where nin = ? and organisation_id=? and iccid=?";
            ps = conn.prepareStatement(query);            
            ps.setString(1, organisationLegalContact.getNin());
            ps.setInt(2, getOrganisation().getOrganisationId());
            ps.setString(3, organisationLegalContact.getIccid());
            
            ps.executeUpdate();            
            conn.commit();
            
            Event event = new Event();
            event.setEventType("IM");
            event.setEventSubType("KycVerifyLegalContact");
            event.setEventKey(getUser());
            event.setEventData(getUser() + " kyc verified user with NIN " + organisationLegalContact.getNin() + " as " +  organisationLegalContact.getLegalContactType() + " to Org " + getOrganisation().getOrganisationId());

            SCAWrapper.getAdminInstance().createEvent(event);
            
        } catch (Exception e) {
            log.warn("Error while verifying legal contact from: [{}]", e);
            e.printStackTrace();
            try {
                ps.close();
                conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if(e.getMessage()!= null)
                localiseErrorAndAddToGlobalErrors("validation.errors", "Error while verifying legal contact: " +e.getMessage());
            else
                localiseErrorAndAddToGlobalErrors("validation.errors", "Error while verifying legal contact: " +e);
            return addLegalContact();

        } finally {
            try {
                ps.close();
                conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        setPageMessage("verifyLegalContact.successfully");
        return showOrganisationLegalContact();
        
    }
    public Resolution removeLegalContact() {
        PreparedStatement ps = null;
        Connection conn = null;
        
        log.warn("IN REMOVE Legal Contacts");
        setOrganisationQuery(new OrganisationQuery());
        getOrganisationQuery().setOrganisationId(getOrganisation().getOrganisationId());
        getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN);

        setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));
        
        try {
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            conn.setAutoCommit(false);
            String query = "delete from organisation_legal_contact where nin = ? and organisation_id=? and iccid=?";
            ps = conn.prepareStatement(query);            
            ps.setString(1, getParameter("removenin"));
            ps.setInt(2, getOrganisation().getOrganisationId());
            ps.setString(3, getParameter("removeiccid"));
            
            ps.executeUpdate();            
            conn.commit();
            
            Event event = new Event();
            event.setEventType("IM");
            event.setEventSubType("RemoveLegalContact");
            event.setEventKey(getUser());
            event.setEventData(getUser() + " removed user with NIN " + getParameter("removenin") + " from Org " + getOrganisation().getOrganisationId());

            SCAWrapper.getAdminInstance().createEvent(event);
            
            
        } catch (Exception e) {
            log.warn("Error while removing legal contact from: [{}]", e);
            e.printStackTrace();
            try {
                ps.close();
                conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if(e.getMessage()!= null)
                localiseErrorAndAddToGlobalErrors("validation.errors", "Error while removing legal contact: " +e.getMessage());
            else
                localiseErrorAndAddToGlobalErrors("validation.errors", "Error while removing legal contact: " +e);
            return addLegalContact();

        } finally {
            try {
                ps.close();
                conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        setPageMessage("removeLegalContact.successfully");
        return showOrganisationLegalContact();
    }
    
    
    public Resolution createLegalContacts() {
        log.debug("createLegalContacts() ");
        PreparedStatement ps = null;
        Connection conn = null;
        PreparedStatement ps1 = null;
        Connection conn1 = null;
        String errorMessage="";
        
        setOrganisationQuery(new OrganisationQuery());
        getOrganisationQuery().setOrganisationId(getOrganisation().getOrganisationId());
        getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN);

        setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getOrganisationQuery()));

        try {
            OrganisationLegalContact organisationLegalContact = getOrganisationLegalContact();
            
            if(organisationLegalContact.getLegalContactType()==null || organisationLegalContact.getLegalContactType().isEmpty()) {
                errorMessage += "Contact type not supplied.<br/>";
            }

            if(organisationLegalContact.getIccid()==null || organisationLegalContact.getIccid().trim().isEmpty()) {
                errorMessage += "SIM Number not supplied.<br/>";
            }
            
            
            if(!errorMessage.trim().isEmpty()) {
                localiseErrorAndAddToGlobalErrors("validation.errors", "<br/>" + errorMessage);
                return addLegalContact();
            }
            
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            conn.setAutoCommit(false);
            
            String query="";
            if (getPhotographs().size() > 0) {
                query = "insert into organisation_legal_contact (organisation_id, nin, legal_contact_type, first_name, last_name, iccid, tel_number, email, is_nin_verified, is_kyc_verified, photo_data, file_type) "
                    + "values (?,?,?,?,?,?,?,?,?,?,?,?)";
            } else {
                query = "insert into organisation_legal_contact (organisation_id, nin, legal_contact_type, first_name, last_name, iccid, tel_number, email, is_nin_verified,is_kyc_verified) "
                    + "values (?,?,?,?,?,?,?,?,?,?)";
            }
            
            ps = conn.prepareStatement(query);
            ps.setInt(1, getOrganisation().getOrganisationId());
            ps.setString(2, organisationLegalContact.getNin());
            ps.setString(3, organisationLegalContact.getLegalContactType());
            ps.setString(4, organisationLegalContact.getFirstName());
            ps.setString(5, organisationLegalContact.getLastName());
            ps.setString(6, organisationLegalContact.getIccid());            
            ps.setString(7, organisationLegalContact.getTelNumber());
            ps.setString(8, organisationLegalContact.getEmail());
            ps.setString(9, "Y");
            ps.setString(10, "N");
            
            if (getPhotographs().size() > 0) {
                ps.setString(11, Utils.encodeBase64(Utils.getDataFromTempFile(getPhotographs().get(0).getPhotoGuid())));  
                ps.setString(12, getPhotographs().get(0).getPhotoGuid().substring(getPhotographs().get(0).getPhotoGuid().indexOf(".") + 1));
            }         
            
            ps.executeUpdate();            
            conn.commit();
            
                Event event = new Event();
                event.setEventType("IM");
                event.setEventSubType("CreateLegalContact");
                event.setEventKey(getUser());
                event.setEventData(getUser() + " added user with NIN " + organisationLegalContact.getNin() + " as " +  organisationLegalContact.getLegalContactType() + " to Org " + getOrganisation().getOrganisationId());

                SCAWrapper.getAdminInstance().createEvent(event);
                
                String from = "2347020010005";
                String to = organisationLegalContact.getTelNumber();
                String body = "SMILE: A simcard has been registered under your name for company " + getOrganisation().getOrganisationName();
                ShortMessage sm = new ShortMessage();
                sm.setBody(body);
                sm.setFrom(from);
                sm.setTo(to);

                if (!sm.getFrom().isEmpty() && !sm.getTo().isEmpty()) {
                    log.debug("Sending notification sms from [{}] to [{}] text [{}]", new Object[]{from, to, body});
                    try {
                        SCAWrapper.getAdminInstance().sendShortMessage(sm);                        
                    } catch (Exception ex) {
                        log.error("error in sendSMS", ex);                        
                        log.warn("Failed to send notification sms from [{}] to [{}] text [{}] Reason: [{}]", new Object[]{from, to, body, ex.getMessage()});
                    }
                }
        } catch (Exception e) {
            log.warn("Error while creating legal contact from: [{}]", e);
            e.printStackTrace();
            try {
                ps.close();
                conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if(e.getMessage()!= null)
                localiseErrorAndAddToGlobalErrors("validation.errors", "Error while creating legal contact: " +e.getMessage());
            else
                localiseErrorAndAddToGlobalErrors("validation.errors", "Error while creating legal contact: " +e);
            return addLegalContact();

        } finally {
            try {
                ps.close();
                conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
       
        setOrganisationLegalContact(new OrganisationLegalContact());
        setPageMessage("createLegalContacts.successfully");
        return showOrganisationLegalContact();
    }

    OrganisationLegalContact organisationLegalContact;

    public OrganisationLegalContact getOrganisationLegalContact() {
        return organisationLegalContact;
    }

    public void setOrganisationLegalContact(OrganisationLegalContact organisationLegalContact) {
        this.organisationLegalContact = organisationLegalContact;
    }

    List<OrganisationLegalContact> organisationLegalContacts;

    public List<OrganisationLegalContact> getOrganisationLegalContacts() {
        return organisationLegalContacts;
    }

    public void setOrganisationLegalContacts(List<OrganisationLegalContact> organisationLegalContacts) {
        this.organisationLegalContacts = organisationLegalContacts;
    }
    
    
    List<ProductsKycVerification> kycProductslist = new ArrayList<>();

    public List<ProductsKycVerification> getKycProductslist() {
        return kycProductslist;
    }

    public void setKycProductslist(List<ProductsKycVerification> kycProductslist) {
        this.kycProductslist = kycProductslist;
    }
    
    public Resolution sendSalesPersonMessage() {
        
        if(getParameter("salesPerson")==null || getParameter("salesPerson").isEmpty()) {
            localiseErrorAndAddToGlobalErrors("error", "Sales person not found.");
            return retrieveKycProducts();
        }
        
        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(Integer.parseInt(getParameter("salesPerson")));
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
        Customer tmpCust = SCAWrapper.getAdminInstance().getCustomer(q);
        
        Customer loggedInCustomer = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER);
        
        String msgBody = "<h3>With regards to your last sale to customerID " 
                            + getCustomer().getCustomerId() + ".</h3><br/>" +  getParameter("msgbody") + "<br/><br/>Regards,<br/>";
        
        msgBody += loggedInCustomer.getFirstName() + " " + loggedInCustomer.getLastName() + "<br/>KYCTeam";
        
        String mailTo = tmpCust.getEmailAddress();
        
        msgBody=msgBody.replace("\n", "<br/>");
        
        try {
            IMAPUtils.sendEmail(loggedInCustomer.getEmailAddress(), mailTo, "Msg From SmileKYCTeam", msgBody);
        } catch (Exception e) {
            log.warn("Error sending message {}", e.getMessage());
            e.printStackTrace();
            return retrieveKycProducts();
        }
        setPageMessage("sendMessageSuccess");
        return retrieveKycProducts();
    }
    
    
    public Resolution retrieveKycProducts() {
        checkPermissions(Permissions.REACTIVATE_SERVICE_INSTANCE);
        PreparedStatement ps = null;
        Connection conn = null;
        List<ProductsKycVerification> productslist = new ArrayList<>();

        try {            
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            String query = "select kyc_id,customer_id,nin,created_date,status,sales_person from products_kyc_verification where STATUS ='NW' or STATUS is null order by created_date asc limit 30";
            ps = conn.prepareStatement(query);            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {                
                ProductsKycVerification product = new ProductsKycVerification();
                product.setKycId(rs.getInt("kyc_id"));
                product.setCustomerId(rs.getInt("customer_id"));
                product.setNin(rs.getString("nin"));
                product.setCreatedDate(rs.getString("created_date"));
                product.setStatus(rs.getString("status"));
                product.setSalesPerson(rs.getString("sales_person"));
                
                productslist.add(product);
            } 
            
            getKycProductslist().clear();
            getKycProductslist().addAll(productslist);
        
            
        /*    
            String upd = "update products_kyc_verification set verified_by=? where kyc_id=?";
            PreparedStatement updps = conn.prepareStatement(upd);
            updps.setInt(1, getUserCustomerIdFromSession());
            updps.setInt(2, kyc_id);
            updps.executeUpdate();
            conn.commit();            
        */    
            ps.close();
            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.warn("Error occured getting kyc products: " + ex);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    log.warn("Error closing the prepared statement " + ex);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.warn("Error closing the connection " + ex);
                }
            }
        }
        
        setVerifyAction(true);
        return getDDForwardResolution("/customer/view_kyc_awaiting_sales.jsp");
    }
    HashMap<String,String> simUser = new HashMap<String,String>();

    public HashMap<java.lang.String, java.lang.String> getSimUser() {
        return simUser;
    }

    public void setSimUser(HashMap<java.lang.String, java.lang.String> simUser) {
        this.simUser = simUser;
    }
    
    
    HashMap<String,String> simStatus = new HashMap<String,String>();

    public HashMap<java.lang.String, java.lang.String> getSimStatus() {
        return simStatus;
    }

    public void setSimStatus(HashMap<java.lang.String, java.lang.String> simStatus) {
        this.simStatus = simStatus;
    }    
    
    Set<NinAccountData> ninAccountData = new HashSet<NinAccountData>();

    public Set<NinAccountData> getNinAccountData() {
        return ninAccountData;
    }

    public void setNinAccountData(Set<NinAccountData> ninAccountData) {
        this.ninAccountData = ninAccountData;
    }
    
}

