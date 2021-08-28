package com.smilecoms.im;

import com.smilecoms.im.ug.nira.NiraClient;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductInstanceList;
import com.smilecoms.commons.sca.ProductInstanceQuery;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceOrder;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.HashUtils;
import com.smilecoms.commons.util.IMAPUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.im.db.model.ChargingInfo;
import com.smilecoms.im.db.model.CustomerProfile;
import com.smilecoms.im.db.model.Impi;
import com.smilecoms.im.db.model.ImpiApn;
import com.smilecoms.im.db.model.ImpiImpu;
import com.smilecoms.im.db.model.Impu;
import com.smilecoms.im.db.model.ImpuVisitedNetwork;
import com.smilecoms.im.db.model.Imsu;
import com.smilecoms.im.db.model.NetworkAccessIdentifier;
import com.smilecoms.im.db.model.Sp;
import com.smilecoms.im.db.model.SsoPasswordReset;
import com.smilecoms.im.db.model.UiccDetails;
import com.smilecoms.im.db.model.VisitedNetwork;
import com.smilecoms.im.db.op.HSSDAO;
import com.smilecoms.im.db.op.IMDAO;
import com.smilecoms.im.tz.foreigner.verify.restclient.VerifyForeignerResponse;
import com.smilecoms.im.tz.foreigner.verify.restclient.VerifyForeignerRestClient;
import com.smilecoms.im.ug.nira.NiraResponse;
import com.smilecoms.im.tz.nida.restclient.NidaResponse;
import com.smilecoms.im.tz.nida.restclient.NidaRestClient;
import com.smilecoms.im.tz.verify.defaced.restclient.VerifyDefacedCustomerResponse;
import com.smilecoms.im.tz.verify.defaced.restclient.VerifyDefacedCustomerRestClient;
import com.smilecoms.im.ng.nin.DemoData;
import com.smilecoms.im.ng.nin.NinClient;
import com.smilecoms.im.ng.nin.NinResponse;
import com.smilecoms.xml.im.IMError;
import com.smilecoms.xml.im.IMSoap;
import com.smilecoms.xml.schema.im.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

/**
 *
 * @author paul
 */
@WebService(serviceName = "IM", portName = "IMSoap", endpointInterface = "com.smilecoms.xml.im.IMSoap", targetNamespace = "http://xml.smilecoms.com/IM", wsdlLocation = "IMServiceDefinition.wsdl")
@Stateless
@HandlerChain(file = "/handler.xml")
public class IdentityManager extends SmileWebService implements IMSoap {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;
    @PersistenceContext(unitName = "IMPU")
    private EntityManager em;

    /*
     *
     * IM FUNCTIONALITY
     *
     */
    @Override
    public CustomerList getCustomers(CustomerQuery customerQuery) throws IMError {
        setContext(customerQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        CustomerList customerList = new CustomerList();
        try {
            Map<Integer, CustomerProfile> dbProfiles = new HashMap<>();

            if (customerQuery.getCustomerId() > 0) {
                log.debug("Looking up a single customer with ID [{}]", customerQuery.getCustomerId());
                CustomerProfile dbCustomer = IMDAO.getCustomerProfileById(em, customerQuery.getCustomerId());
                dbProfiles.put(dbCustomer.getCustomerProfileId(), dbCustomer);
            } else {
                int limit = customerQuery.getResultLimit();
                int resultsToGet = limit;

                String lastName = customerQuery.getLastName();
                String firstName = customerQuery.getFirstName();

                if (firstName != null
                        && !firstName.isEmpty()
                        && lastName != null
                        && !lastName.isEmpty()
                        && resultsToGet > 0) {
                    log.debug("Searching using first and last name [{}][{}]", firstName, lastName);
                    for (com.smilecoms.im.db.model.CustomerProfile c : IMDAO.getCustomerProfilesByWildcardedFirstAndLastName(em, firstName, lastName, resultsToGet)) {
                        dbProfiles.put(c.getCustomerProfileId(), c);
                    }
                    resultsToGet = limit - dbProfiles.size();
                } else {

                    if (lastName != null && !lastName.isEmpty() && resultsToGet > 0) {
                        log.debug("Searching using last name [{}]", lastName);
                        for (com.smilecoms.im.db.model.CustomerProfile c : IMDAO.getCustomerProfilesByWildcardedLastName(em, lastName, resultsToGet)) {
                            dbProfiles.put(c.getCustomerProfileId(), c);
                        }
                        resultsToGet = limit - dbProfiles.size();
                    }

                    if (firstName != null && !firstName.isEmpty() && resultsToGet > 0) {
                        log.debug("Searching using first name [{}]", firstName);
                        for (com.smilecoms.im.db.model.CustomerProfile c : IMDAO.getCustomerProfilesByWildcardedFirstName(em, firstName, resultsToGet)) {
                            dbProfiles.put(c.getCustomerProfileId(), c);
                        }
                        resultsToGet = limit - dbProfiles.size();
                    }
                }
                String organisationName = customerQuery.getOrganisationName();
                if (organisationName != null && !organisationName.isEmpty() && resultsToGet > 0) {
                    log.debug("Searching using organisation name [{}]", firstName);
                    for (com.smilecoms.im.db.model.CustomerProfile c : IMDAO.getCustomerProfileByWildcardedOrganisationName(em, organisationName, resultsToGet)) {
                        dbProfiles.put(c.getCustomerProfileId(), c);
                    }
                    resultsToGet = limit - dbProfiles.size();
                }

                String idNumber = customerQuery.getIdentityNumber();
                if (idNumber != null && !idNumber.isEmpty() && resultsToGet > 0) {
                    log.debug("Searching using ID number [{}]", idNumber);
                    for (com.smilecoms.im.db.model.CustomerProfile c : IMDAO.getCustomerProfileByIDNumber(em, idNumber, resultsToGet)) {
                        dbProfiles.put(c.getCustomerProfileId(), c);
                    }
                    resultsToGet = limit - dbProfiles.size();
                }

                String nationalIdNumber = customerQuery.getNationalIdentityNumber();
                if (nationalIdNumber != null && !nationalIdNumber.isEmpty() && resultsToGet > 0) {
                    log.warn("Searching using National ID number [{}]", nationalIdNumber);
                    for (com.smilecoms.im.db.model.CustomerProfile c : IMDAO.getCustomerProfileByNationalIDNumber(em, nationalIdNumber, resultsToGet)) {
                        dbProfiles.put(c.getCustomerProfileId(), c);
                    }
                    resultsToGet = limit - dbProfiles.size();
                }

                String sso = customerQuery.getSSOIdentity();
                if (sso != null && !sso.isEmpty() && resultsToGet > 0) {
                    log.debug("Searching using SSO Identity [{}]", sso);
                    try {
                        com.smilecoms.im.db.model.CustomerProfile c = IMDAO.getCustomerProfileBySSOIdentity(em, sso);
                        dbProfiles.put(c.getCustomerProfileId(), c);
                    } catch (Exception e) {
                    }
                    resultsToGet = limit - dbProfiles.size();
                }

                String email = customerQuery.getEmailAddress();
                if (email != null && !email.isEmpty() && resultsToGet > 0) {
                    log.debug("Searching using Email Address [{}]", email);
                    try {
                        com.smilecoms.im.db.model.CustomerProfile c = IMDAO.getCustomerProfileByEmailAddress(em, email);
                        dbProfiles.put(c.getCustomerProfileId(), c);
                    } catch (Exception e) {
                    }
                    resultsToGet = limit - dbProfiles.size();
                }

                String alternativeContactNumber = Utils.getFriendlyPhoneNumberKeepingCountryCode(customerQuery.getAlternativeContact());
                if (alternativeContactNumber != null && !alternativeContactNumber.isEmpty() && resultsToGet > 0) {
                    log.debug("Searching using Alternative Contact Number [{}]", alternativeContactNumber);
                    try {
                        for (com.smilecoms.im.db.model.CustomerProfile c : IMDAO.getCustomerProfileByAlternativeContactNumber(em, alternativeContactNumber, resultsToGet)) {
                            dbProfiles.put(c.getCustomerProfileId(), c);
                        }
                    } catch (Exception e) {
                    }
                    resultsToGet = limit - dbProfiles.size();
                }

                String kycStatus = customerQuery.getKYCStatus();
                if (kycStatus != null && !kycStatus.isEmpty() && resultsToGet > 0) {
                    log.debug("Searching using KYCStatus [{}]", kycStatus);
                    try {
                        for (com.smilecoms.im.db.model.CustomerProfile c : IMDAO.getCustomerProfilesByKYCStatus(em, kycStatus, resultsToGet)) {
                            dbProfiles.put(c.getCustomerProfileId(), c);
                        }
                    } catch (Exception e) {
                    }
                }

                if (BaseUtils.getBooleanProperty("env.verify.customers.nin", false)) {
                    String isNinVerified = customerQuery.getIsNinVerified();
                    if (isNinVerified != null && !isNinVerified.isEmpty() && resultsToGet > 0) {
                        log.debug("Searching using isNinVerified [{}]", isNinVerified);
                        try {
                            for (com.smilecoms.im.db.model.CustomerProfile c : IMDAO.getCustomerProfilesByNinVerificationStatus(em, isNinVerified, resultsToGet)) {
                                dbProfiles.put(c.getCustomerProfileId(), c);
                            }
                        } catch (Exception e) {
                        }
                    }
                }

                int resLimit = customerQuery.getResultLimit();
                if (customerQuery.getSecurityGroupType() != null && !customerQuery.getSecurityGroupType().isEmpty() && resultsToGet > 0) {
                    log.warn("Searching using secGroup [{}]", customerQuery.getSecurityGroupType());
                    try {
                        List<com.smilecoms.im.db.model.CustomerProfile> c = IMDAO.getCustomerProfilesBySecurityGroup(em, customerQuery.getSecurityGroupType(), resultsToGet);
                        for (CustomerProfile customer : c) {
                            dbProfiles.put(customer.getCustomerProfileId(), customer);
                        }

                    } catch (Exception e) {
                    }
                    resultsToGet = limit - dbProfiles.size();
                }

            }

            log.debug("Adding [{}] return results", dbProfiles.size());
            for (CustomerProfile dbProfile : dbProfiles.values()) {
                customerList.getCustomers().add(getXMLCustomer(dbProfile, customerQuery.getVerbosity()));
            }
            customerList.setNumberOfCustomers(customerList.getCustomers().size());

        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return customerList;
    }

    @Override
    public PlatformInteger addCustomer(Customer newCustomer) throws IMError {
        setContext(newCustomer, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        PlatformInteger customerId = new PlatformInteger();
        try {

            if (newCustomer.getEmailAddress() != null && !newCustomer.getEmailAddress().isEmpty()) {
                try {
                    CustomerProfile tmp = IMDAO.getCustomerProfileByEmailAddress(em, newCustomer.getEmailAddress());
                    // If we get here, then there is a customer with that email address which is not ok
                    throw new Exception("Customer email address must be unique");
                } catch (javax.persistence.NoResultException e) {
                    log.debug("Customer email address is unique");
                }
            }
            
            //HBT-13193
            if (BaseUtils.getBooleanProperty("env.phone.number.force.unique", false) && newCustomer.getAlternativeContact1()!= null && !newCustomer.getAlternativeContact1().isEmpty()) {
                try {
                    List<CustomerProfile> tmp = IMDAO.getCustomerProfileByAlternativeContactNumber(em, newCustomer.getAlternativeContact1(), 1);
                    // If we get here, then there is already a customer with that contact number which is not ok
                    if(tmp.size()>0) 
                        throw new Exception("Customer contact number must be unique");
                } catch (javax.persistence.NoResultException e) {
                    log.debug("Customer contact number is unique");
                }
            }

            if (BaseUtils.getBooleanProperty("env.identity.number.force.unique", false) && newCustomer.getIdentityNumber() != null && !newCustomer.getIdentityNumber().isEmpty()) {
                newCustomer.setIdentityNumber(newCustomer.getIdentityNumber().replace(" ", ""));
                List<CustomerProfile> customers = IMDAO.getCustomerProfileByIDNumber(em, newCustomer.getIdentityNumber(), 1);
                if (!customers.isEmpty()) {
                    throw new Exception("Customer Id number must be unique");
                }
            }

            if (BaseUtils.getBooleanProperty("env.national.identity.number.force.unique", false) && newCustomer.getNationalIdentityNumber() != null && !newCustomer.getNationalIdentityNumber().isEmpty()) {
                newCustomer.setNationalIdentityNumber(newCustomer.getNationalIdentityNumber().replace(" ", ""));
                List<CustomerProfile> customers = IMDAO.getCustomerProfileByNationalIDNumber(em, newCustomer.getNationalIdentityNumber(), 1);
                if (!customers.isEmpty()) {
                    throw new Exception("Customer National Id number must be unique");
                }
            }

            if (newCustomer.getEmailAddress().contains(";")) {
                // Sometimes a non ascii code is sent though like &#x73;
                throw new Exception("Invalid email address. Address contains invalid characters -- " + newCustomer.getEmailAddress());
            }

            String kYCStatus = ""; // Pending varification - default

            if (BaseUtils.getBooleanProperty("env.customer.verify.with.nida", false)
                    && newCustomer.getKYCStatus() != null
                    && newCustomer.getKYCStatus().equalsIgnoreCase("V")) {
                kYCStatus = "V";
            } else {
                kYCStatus = "P";
            }

            // For Uganda - check Nira status - Only for Ugandan locals.
            if (BaseUtils.getBooleanProperty("env.customer.verify.with.nira", false)) {

                if (newCustomer.getNationality().equalsIgnoreCase(BaseUtils.getProperty("env.locale.country.for.language.en"))
                        && isAllowed(BaseUtils.getProperty("env.sep.roles.allowed.to.verify.customers.with.nira", ""), newCustomer.getPlatformContext())) {
                    /*  Sample data used is:
                        NIN: CM930121003EGE
                        Document ID: 000092564
                        SURNAME: Tipiyai
                        Given Names: Johnson
                        OtherNames:
                        Date Of Birth: 01/01/1993
                     */
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

                    Date dateOfBirth = sdf.parse(newCustomer.getDateOfBirth().replaceAll("/", ""));
                    String otherNames = (newCustomer.getMiddleName() == null ? "" : newCustomer.getMiddleName());

                    NiraResponse rsp = NiraClient.checkIfCustomerIdExistsAtNIRA(newCustomer.getIdentityNumber(), newCustomer.getCardNumber(),
                            newCustomer.getLastName(), newCustomer.getFirstName(), otherNames,
                            dateOfBirth);

                    if (rsp.isSuccessful()) { // Nira passed.
                        newCustomer.setKYCStatus("V");
                        kYCStatus = "V";
                    } else { //Set verification status to Unvalidated and save the response
                        newCustomer.setKYCStatus("U");
                        throw new Exception("Verification with NIRA failed"); // -- " + rsp.toString());
                    }
                } else if (!newCustomer.getNationality().equalsIgnoreCase(BaseUtils.getProperty("env.locale.country.for.language.en"))) { // As per URL: http://jira.smilecoms.com/browse/HBT-10466 foreigners must automatically be set to Verified
                    kYCStatus = "V";
                }

            }

            CustomerProfile dbCustomer = new CustomerProfile();
            if (newCustomer.getClassification().equals("customer")) {
                log.debug("Dont allow direct setting of customer classification. Only allow it to become customer when 2 photographs are attached");
                newCustomer.setClassification("pending");
            }
            Set iddocTypes = BaseUtils.getPropertyAsSet("env.customer.personal.id.document.types");
            iddocTypes.addAll(BaseUtils.getPropertyAsSet("env.foreign.customer.personal.id.document.types"));
            if (newCustomer.getIdentityNumberType() != null
                    && !newCustomer.getIdentityNumberType().isEmpty()
                    && !iddocTypes.contains(newCustomer.getIdentityNumberType())) {
                throw new Exception("Invalid Id document type -- " + newCustomer.getIdentityNumberType());
            }
            syncXMLCustomerIntoDBCustomer(newCustomer, dbCustomer);
            dbCustomer.setClassification(newCustomer.getClassification());
            dbCustomer.setVersion(0);
            dbCustomer.setkYCStatus(kYCStatus); // Pending varification
            dbCustomer.setCreatedDatetime(new Date());
            dbCustomer.setUpdatedDatetime(new Date());
            dbCustomer.setSsoLockExpiry(new Date());
            // PCB - 202 New functionality to track who created who
            int createdById = IMDAO.getCustomerProfileIdBySSOIdentity(em, newCustomer.getPlatformContext().getOriginatingIdentity());
            dbCustomer.setCreatedByCustomerProfileId(createdById);
            dbCustomer.setAccountManagerCustomerProfileId(createdById);
            dbCustomer.setSsoAuthAttempts((short) 0);
            try {
                em.persist(dbCustomer);
                em.flush();
            } catch (javax.persistence.PersistenceException e) {
                log.warn("Error persisting new customer profile: {}", e.toString());
                if (e.toString().contains("MySQLIntegrityConstraintViolationException")) {
                    throw new Exception("Duplicate customer sso identity");
                } else {
                    throw e;
                }
            }
            em.refresh(dbCustomer);
            IMDAO.setCustomerRoles(em, newCustomer.getCustomerRoles(), dbCustomer.getCustomerProfileId());
            IMDAO.setCustomersSecurityGroups(em, newCustomer.getSecurityGroups(), dbCustomer.getCustomerProfileId());
            IMDAO.setCustomersSellers(em, newCustomer.getCustomerSellers(), dbCustomer.getCustomerProfileId());

            for (Address newAddress : newCustomer.getAddresses()) {
                newAddress.setCustomerId(dbCustomer.getCustomerProfileId());
                com.smilecoms.im.db.model.Address dbAddress = new com.smilecoms.im.db.model.Address();
                syncXMLAddressIntoDBAddress(newAddress, dbAddress);
                em.persist(dbAddress);
            }

            if (BaseUtils.getBooleanProperty("env.customer.mandatory.kyc.enable", false)) {
                com.smilecoms.im.db.model.MandatoryKYCFields dBKycStatus = new com.smilecoms.im.db.model.MandatoryKYCFields();
                dBKycStatus.setCustomerId(dbCustomer.getCustomerProfileId());
                em.persist(dBKycStatus);
            }

            String requiredPhotoTypesRegex = BaseUtils.getProperty("env.customer.personal.required.documents.im.regex", ".*");

            if (!newCustomer.getCustomerPhotographs().isEmpty()) {
                IMDAO.setCustomersPhotographs(em, newCustomer.getCustomerPhotographs(), dbCustomer.getCustomerProfileId());
                if (!dbCustomer.getClassification().equals("customer") && !dbCustomer.getClassification().equals("minor") && !dbCustomer.getClassification().equals("foreigner")) {
                    log.debug("This customer may now have photographs so we can change the classification to customer");
                    String docTypes = "";
                    for (Photograph photo : newCustomer.getCustomerPhotographs()) {
                        docTypes += photo.getPhotoType() + " ";
                    }
                    if (Utils.matchesWithPatternCache(docTypes, requiredPhotoTypesRegex)) {
                        log.debug("All required photos were found so changing classification to customer [{}]", docTypes);
                        dbCustomer.setClassification("customer");
                        em.persist(dbCustomer);
                    } else {
                        log.debug("Not all required photos were found so wont change classification to customer [{}]", docTypes);
                    }
                }
            } else if (requiredPhotoTypesRegex.equals(".*")) {
                log.debug("No documents supplied but env.customer.personal.required.documents.im.regex is .* so its ok");
                dbCustomer.setClassification("customer");
            }

            em.flush();
            em.refresh(dbCustomer);

            customerId.setInteger(dbCustomer.getCustomerProfileId());
            createEvent(dbCustomer.getCustomerProfileId());

            PlatformEventManager.createEvent("CL_CUST", "NEW",
                    String.valueOf(dbCustomer.getCustomerProfileId()),
                    "CustId=" + dbCustomer.getCustomerProfileId());

        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return customerId;
    }

    @Override
    public Done modifyCustomer(Customer modifiedCustomer) throws IMError {

        setContext(modifiedCustomer, wsctx);
        if (log.isDebugEnabled()) {
            logStart(modifiedCustomer.getCustomerId());
        }
        try {

            if (modifiedCustomer.getEmailAddress() != null && !modifiedCustomer.getEmailAddress().isEmpty()) {
                try {
                    CustomerProfile tmp = IMDAO.getCustomerProfileByEmailAddress(em, modifiedCustomer.getEmailAddress());
                    if (tmp.getCustomerProfileId() != modifiedCustomer.getCustomerId()) {
                        // If we get here, then there is a customer with that email address which is not ok
                        throw new Exception("Customer email address must be unique");
                    }
                } catch (javax.persistence.NoResultException e) {
                    log.debug("Customer email address is unique");
                }
            }

            if (BaseUtils.getBooleanProperty("env.identity.number.force.unique", false) && modifiedCustomer.getIdentityNumber() != null && !modifiedCustomer.getIdentityNumber().isEmpty()) {
                modifiedCustomer.setIdentityNumber(modifiedCustomer.getIdentityNumber().replace(" ", ""));
                List<CustomerProfile> customers = IMDAO.getCustomerProfileByIDNumber(em, modifiedCustomer.getIdentityNumber(), 1);
                if (!customers.isEmpty() && customers.get(0).getCustomerProfileId() != modifiedCustomer.getCustomerId()) {
                    throw new Exception("Customer Id number must be unique");
                }
            }
            /*
            if (BaseUtils.getBooleanProperty("env.national.identity.number.force.unique", false) && modifiedCustomer.getNationalIdentityNumber() != null && !modifiedCustomer.getNationalIdentityNumber().isEmpty()) {
                modifiedCustomer.setNationalIdentityNumber(modifiedCustomer.getNationalIdentityNumber().replace(" ", ""));
                List<CustomerProfile> customers = IMDAO.getCustomerProfileByNationalIDNumber(em, modifiedCustomer.getNationalIdentityNumber(), 1);
                if (!customers.isEmpty() && customers.get(0).getCustomerProfileId() != modifiedCustomer.getCustomerId()) {
                    throw new Exception("Customer Id number must be unique");
                }
            }
             */

            if (modifiedCustomer.getEmailAddress().contains(";")) {
                // Sometimes a non ascii code is sent though like &#x73;
                throw new Exception("Invalid email address. Address contains invalid characters -- " + modifiedCustomer.getEmailAddress());
            }

            CustomerProfile dbCustomer = IMDAO.getCustomerProfileById(em, modifiedCustomer.getCustomerId());

            JPAUtils.checkLastModified(modifiedCustomer.getVersion(), dbCustomer.getVersion());

            List<String> currentSecurityGroups = IMDAO.getCustomersSecurityGroups(em, modifiedCustomer.getCustomerId());

            if (!isCallerAnAdministrator(modifiedCustomer.getPlatformContext())) {
                if (isAdministrator(modifiedCustomer.getCustomerId())) {
                    throw new Exception("Only Administrators can edit Administrators");
                }
                for (String group : modifiedCustomer.getSecurityGroups()) {
                    if (group.equalsIgnoreCase("administrator")) {
                        throw new Exception("Only Administrators can edit Administrators");
                    }
                }
            }

            // First get the list of roles in Organisations that are new and old and work out the ones that have been added
            List<CustomerRole> customersExistingRolesInOrganisations = IMDAO.getCustomerRolesByCustomerProfileId(em, dbCustomer.getCustomerProfileId());
            List<CustomerRole> customersModifiedRolesInOrganisations = modifiedCustomer.getCustomerRoles();
            List<CustomerRole> customersNewRolesInOrganisations = new ArrayList();

            boolean isInSmile = false;
            // Build up a list of all new Organisations this customer has a role in
            for (CustomerRole possiblyNew : customersModifiedRolesInOrganisations) {
                if (possiblyNew.getOrganisationId() == 1) {
                    isInSmile = true;
                }
                boolean isNew = true;
                for (CustomerRole existing : customersExistingRolesInOrganisations) {
                    if (existing.getOrganisationId() == possiblyNew.getOrganisationId()) {
                        isNew = false;
                        break;
                    }
                }
                if (isNew) {
                    customersNewRolesInOrganisations.add(possiblyNew);
                }
            }

            // Verify with NIRA if is at UG;
            // For Uganda - check Nira status - Only for Ugandan locals.
            if (BaseUtils.getBooleanProperty("env.customer.verify.with.nira", false)) {
                if (customersModifiedRolesInOrganisations.size() <= 0) {

                    // In UG International customers must be given the International KYC status
                    if (!modifiedCustomer.getNationality().equalsIgnoreCase(BaseUtils.getProperty("env.locale.country.for.language.en"))) {
                        modifiedCustomer.setKYCStatus("I");
                    }

                    if (modifiedCustomer.getNationality().equalsIgnoreCase(BaseUtils.getProperty("env.locale.country.for.language.en"))
                            && isAllowed(BaseUtils.getProperty("env.sep.roles.allowed.to.verify.customers.with.nira", ""), modifiedCustomer.getPlatformContext())) {
                        // !(dbCustomer.getkYCStatus() != null && dbCustomer.getkYCStatus().equals("V"))) { //Unverified/Pending customers
                        /* Sample data used is:
                            NIN: CM930121003EGE
                            Document ID: 000092564
                            SURNAME: Tipiyai
                            Given Names: Johnson
                            OtherNames:
                            Date Of Birth: 01/01/1993
                         */
                        // List<CustomerRole> lstCustRoles = IMDAO.getCustomerRolesByCustomerProfileId(em, modifiedCustomer.getCustomerId());

                        if (customersNewRolesInOrganisations.isEmpty()) { // Verify - customer who belong to organisation are not supposed to the verified.
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

                            Date dateOfBirth = sdf.parse(modifiedCustomer.getDateOfBirth().replaceAll("/", ""));
                            String otherNames = (modifiedCustomer.getMiddleName() == null ? "" : modifiedCustomer.getMiddleName());

                            NiraResponse rsp = NiraClient.checkIfCustomerIdExistsAtNIRA(modifiedCustomer.getIdentityNumber(), modifiedCustomer.getCardNumber(),
                                    modifiedCustomer.getLastName(), modifiedCustomer.getFirstName(), otherNames,
                                    dateOfBirth);

                            if (rsp.isSuccessful()) { // Nira passed.
                                modifiedCustomer.setKYCStatus("V");
                            } else { //Set verification status to Unvalidated and save the response
                                modifiedCustomer.setKYCStatus("U");
                                throw new Exception("Verification with NIRA failed"); //  -- " + rsp.toString());
                            }
                        }
                    }
                }
            }

            boolean detailsChanged = false;
            String dbNationalIdNumber = "";
            if (dbCustomer.getNationalIdNumber() != null) {
                dbNationalIdNumber = dbCustomer.getNationalIdNumber();
            }

            if (!dbCustomer.getEmailAddress().equals(modifiedCustomer.getEmailAddress())
                    || !dbCustomer.getAlternativeContact1().equals(modifiedCustomer.getAlternativeContact1())
                    || !dbCustomer.getMothersMaidenName().equals(modifiedCustomer.getMothersMaidenName())
                    || !dbCustomer.getIdNumber().equals(modifiedCustomer.getIdentityNumber())
                    || !dbNationalIdNumber.equals(modifiedCustomer.getNationalIdentityNumber())
                    || !dbCustomer.getFirstName().equals(modifiedCustomer.getFirstName())
                    || !dbCustomer.getLastName().equals(modifiedCustomer.getLastName())
                    || dbCustomer.getOptInLevel() != modifiedCustomer.getOptInLevel()
                    || !dbCustomer.getClassification().equals(modifiedCustomer.getClassification())
                    || !dbCustomer.getSsoIdentity().equals(modifiedCustomer.getSSOIdentity())) {
                detailsChanged = true;
            }

            if (dbCustomer.getOptInLevel() != modifiedCustomer.getOptInLevel()) {
                PlatformEventManager.createEvent("IM", "OPT_IN_CHANGE", String.valueOf(modifiedCustomer.getCustomerId()),
                        dbCustomer.getOptInLevel() + "|" + modifiedCustomer.getOptInLevel() + "|" + modifiedCustomer.getPlatformContext().getOriginatingIdentity()
                        + "|" + modifiedCustomer.getPlatformContext().getOriginatingIdentity().equalsIgnoreCase(dbCustomer.getSsoIdentity()));
            }

            if (modifiedCustomer.getSSODigest() != null
                    && !modifiedCustomer.getSSODigest().isEmpty()
                    && !modifiedCustomer.getSSODigest().equals(dbCustomer.getSsoDigest())) {
                // Dont lock out the customer if they changed their password
                dbCustomer.setSsoAuthAttempts((short) 0);
                dbCustomer.setSsoLockExpiry(null);
                em.persist(dbCustomer);
                EventHelper.sendCustomerPasswordChange(dbCustomer.getCustomerProfileId());
            }

            String origKYCStatus = dbCustomer.getkYCStatus();
            String newKYCStatus = modifiedCustomer.getKYCStatus();

            syncXMLCustomerIntoDBCustomer(modifiedCustomer, dbCustomer);

            try {
                dbCustomer.setUpdatedDatetime(new Date());

                log.debug("Going to persist Customer NIN [{}]", dbCustomer.getNationalIdNumber());
                em.persist(dbCustomer);
                em.flush();
            } catch (javax.persistence.PersistenceException e) {
                throw new Exception("Duplicate customer sso identity -- " + e.toString());
            }

            boolean diff = false;
            if (currentSecurityGroups.size() != modifiedCustomer.getSecurityGroups().size()) {
                diff = true;
            }
            if (diff == false) {
                for (String groupCur : currentSecurityGroups) {
                    boolean found = false;
                    for (String groupNew : modifiedCustomer.getSecurityGroups()) {
                        if (groupCur.equals(groupNew)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        diff = true;
                        break;
                    }
                }
            }
            if (diff == false) {
                for (String groupNew : modifiedCustomer.getSecurityGroups()) {
                    boolean found = false;
                    for (String groupCur : currentSecurityGroups) {
                        if (groupCur.equals(groupNew)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        diff = true;
                        break;
                    }
                }
            }
            if (diff) {
                String before = "";
                for (String groupCur : currentSecurityGroups) {
                    before += groupCur + " ";
                }
                String after = "";
                for (String groupNew : modifiedCustomer.getSecurityGroups()) {
                    after += groupNew + " ";
                }
                PlatformEventManager.createEvent("IM", "SECURITY_CHANGE", String.valueOf(modifiedCustomer.getCustomerId()), "Before: " + before + " After: " + after + " By:" + modifiedCustomer.getPlatformContext().getOriginatingIdentity());
            }
            IMDAO.setCustomersSecurityGroups(em, modifiedCustomer.getSecurityGroups(), modifiedCustomer.getCustomerId());

            IMDAO.setCustomersSellers(em, modifiedCustomer.getCustomerSellers(), modifiedCustomer.getCustomerId());

            if (!modifiedCustomer.getCustomerPhotographs().isEmpty()) {                               
                if (origKYCStatus != null && origKYCStatus.equals("V") && !isOnlyProfilePhotoChange(modifiedCustomer.getCustomerId(), modifiedCustomer.getCustomerPhotographs())
                        && !BaseUtils.getBooleanProperty("env.customer.verify.with.nira", false)) {
                    throw new Exception("Cannot modify photographs on a KYC Verified customer");
                }
                
                IMDAO.setCustomersPhotographs(em, modifiedCustomer.getCustomerPhotographs(), modifiedCustomer.getCustomerId());
                String docTypes = "";
                for (Photograph photo : modifiedCustomer.getCustomerPhotographs()) {
                    docTypes += photo.getPhotoType() + " ";
                }
                String requiredPhotoTypesRegex = BaseUtils.getProperty("env.customer.personal.required.documents.im.regex", ".*");
                if (!BaseUtils.getBooleanProperty("env.customer.verify.with.nira", false)
                        && !BaseUtils.getBooleanProperty("env.customer.verify.with.nida", false)) {
                    if (Utils.matchesWithPatternCache(docTypes, requiredPhotoTypesRegex)) {
                        dbCustomer.setkYCStatus("P");
                    } else {
                        dbCustomer.setkYCStatus("U");
                    }
                }

                em.persist(dbCustomer);
            }
//            if ("diplomat".equalsIgnoreCase(modifiedCustomer.getClassification())) {
//                IMDAO.setCustomersClassification(em, modifiedCustomer.getCustomerId());
//                log.debug("customers classification is updated ");
//            }
//            if ("foreigner".equalsIgnoreCase(modifiedCustomer.getClassification())) {
//                IMDAO.setCustomersClassification(em, modifiedCustomer.getCustomerId());
//                log.debug("customers classification is updated ");
//            }
            if (origKYCStatus != null && dbCustomer.getkYCStatus() != null && !origKYCStatus.equals("V") && dbCustomer.getkYCStatus().equals("V")) {
                log.debug("Customer KYC status is being changed to Verified");
                String docTypes = "";
                for (Photograph photo : IMDAO.getCustomersPhotographs(em, modifiedCustomer.getCustomerId())) {
                    docTypes += photo.getPhotoType() + " ";
                }
                String requiredPhotoTypesRegex = BaseUtils.getProperty("env.customer.personal.required.documents.im.regex", ".*");
                if (!Utils.matchesWithPatternCache(docTypes, requiredPhotoTypesRegex)) {
                    throw new Exception("Customer does not meet KYC requirements for verification");
                }

                // At this point, all is OK here ... ReActivate all customer's services if they where disabled.
                if (BaseUtils.getProperty("env.kyc.level", "service").equalsIgnoreCase("customer")) {
                    //ServiceInstanceQuery serviceInstanceQuery = new ServiceInstanceQuery();
                    //serviceInstanceQuery.setCustomerId(dbCustomer.getCustomerProfileId());
                    //serviceInstanceQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);

                    ProductInstanceQuery productInstanceQuery = new ProductInstanceQuery();
                    productInstanceQuery.setCustomerId(dbCustomer.getCustomerProfileId());
                    productInstanceQuery.setVerbosity(StProductInstanceLookupVerbosity.MAIN_SVC);

                    ProductInstanceList productInstances = SCAWrapper.getAdminInstance().getProductInstances(productInstanceQuery);

                    for (ProductInstance pi : productInstances.getProductInstances()) {
                        ProductOrder order = new ProductOrder();
                        order.setProductInstanceId(pi.getProductInstanceId());
                        order.setAction(StAction.NONE);
                        order.setCustomerId(pi.getCustomerId());
                        // Only activate the services under the products that were created after the enforecement date;
                        Calendar startDate = Calendar.getInstance();
                        String dt = BaseUtils.getProperty("env.cm.kyc.enforced.date", "20500101");
                        // Note - on the month below field, we minus 1 for Java compliance
                        startDate.set(Integer.parseInt(dt.substring(0, 4)), Integer.parseInt(dt.substring(4, 6)) - 1, Integer.parseInt(dt.substring(6, 8)), 0, 0, 0);
                        Date piCreatedDateTime = Utils.getJavaDate(pi.getCreatedDateTime());

                        if (piCreatedDateTime.after(startDate.getTime())) { // We do not want to touch product instances that were created before the enforcement date.

                            // Reactivate all the service instances under this product.
                            for (ProductServiceInstanceMapping psim : pi.getProductServiceInstanceMappings()) {
                                ServiceInstance si = psim.getServiceInstance();
                                log.debug("Customer KYCStatus is [{}] and si created date is [{}] so the service status for si [{}] is being set to AC.",
                                        dbCustomer.getkYCStatus(), Utils.getJavaDate(si.getCreatedDateTime()), si.getServiceInstanceId());

                                ServiceInstanceOrder siOrder = new ServiceInstanceOrder();
                                siOrder.setAction(StAction.UPDATE);
                                si.setStatus("AC");
                                siOrder.setServiceInstance(si);
                                order.getServiceInstanceOrders().add(siOrder);
                            }
                        }

                        SCAWrapper.getAdminInstance().processOrder(order);
                    }

                    log.debug("KYC status change from [{}] to [{}]", origKYCStatus, newKYCStatus);
                    PlatformEventManager.createEvent("IM", "KYCChange", String.valueOf(dbCustomer.getCustomerProfileId()),
                            origKYCStatus + "|" + newKYCStatus + "|" + modifiedCustomer.getPlatformContext().getOriginatingIdentity() + "|" + dbCustomer.getCustomerProfileId());
                }
            }

            if (BaseUtils.getProperty("env.kyc.level", "service").equalsIgnoreCase("customer")) {
                if (origKYCStatus != null && dbCustomer.getkYCStatus() != null && !origKYCStatus.equals("V")
                        && Utils.matchesWithPatternCache(dbCustomer.getkYCStatus(), BaseUtils.getProperty("env.kyc.incomplete.status.regex", "P|U"))) {
                    Calendar startDate = Calendar.getInstance();
                    String dt = BaseUtils.getProperty("env.cm.kyc.enforced.date", "20500101");
                    // Note - on the month below field, we minus 1 for Java compliance
                    startDate.set(Integer.parseInt(dt.substring(0, 4)), Integer.parseInt(dt.substring(4, 6)) - 1, Integer.parseInt(dt.substring(6, 8)), 0, 0, 0);
                    //Calendar cutOffDate = Calendar.getInstance();
                    //cutOffDate.setTime(dbCustomer.getCreatedDatetime());
                    //cutOffDate.add(Calendar.HOUR, BaseUtils.getIntProperty("env.cm.kyc.grace.hours", 168)); // By default allow 7 days and then block afterwards.
                    log.debug("KYC status change from [{}] to [{}]", origKYCStatus, newKYCStatus);
                    if (dbCustomer.getCreatedDatetime().after(startDate.getTime())) {

                        PlatformEventManager.createEvent("IM", "KYCStatusIssue", String.valueOf(dbCustomer.getCustomerProfileId()),
                                origKYCStatus + "|" + newKYCStatus + "|" + modifiedCustomer.getPlatformContext().getOriginatingIdentity() + "|" + dbCustomer.getCustomerProfileId(), "KYCStatusIssue_" + dbCustomer.getCustomerProfileId());
                    }

                    //if (!Utils.isInTheFuture(cutOffDate.getTime()) && dbCustomer.getCreatedDatetime().after(startDate.getTime())) {
                    //  log.debug("Customer KYCStatus is [{}] and created date is [{}] so the service status is being set to TD.", cust.getKYCStatus(), dbSvcInst.getCreatedDatetime());
                    //}
                }
            }

            if (!dbCustomer.getClassification().equals("customer") && !dbCustomer.getClassification().equals("minor") && !dbCustomer.getClassification().equals("diplomat") && !dbCustomer.getClassification().equals("foreigner")) {
                log.debug("This customer may now have photographs so we can change the classification to customer");
                String docTypes = "";
                for (Photograph photo : IMDAO.getCustomersPhotographs(em, modifiedCustomer.getCustomerId())) {
                    docTypes += photo.getPhotoType() + " ";
                }
                String requiredPhotoTypesRegex = BaseUtils.getProperty("env.customer.personal.required.documents.im.regex", ".*");
                if (Utils.matchesWithPatternCache(docTypes, requiredPhotoTypesRegex)) {
                    log.debug("All required photos were found so changing classification to customer [{}] matched [{}]", docTypes, requiredPhotoTypesRegex);
                    dbCustomer.setClassification("customer");
                    em.persist(dbCustomer);
                    PlatformEventManager.createEvent("IM", "CUST_CLASS",
                            String.valueOf(dbCustomer.getCustomerProfileId()),
                            modifiedCustomer.getPlatformContext().getOriginatingIdentity());
                } else {
                    log.debug("Not all required photos were found so wont change classification to customer [{}] did not match [{}]", docTypes, requiredPhotoTypesRegex);
                }
            }

            if (isInSmile) {
                log.debug("Customer is a member of Smile communications");
                if (!dbCustomer.getEmailAddress().contains("@smilecoms.com")) {
                    throw new Exception("Members of Smile Communications must have a @smilecoms.com email address");
                }
                String userNameShouldBe = dbCustomer.getEmailAddress().replace("@smilecoms.com", "");
                if (!userNameShouldBe.equals(dbCustomer.getSsoIdentity())) {
                    throw new Exception("Members of Smile Communications must have a user name the same as the first part of their email address -- " + dbCustomer.getSsoIdentity() + " should be " + userNameShouldBe);
                }
            } else {
                log.debug("Customer is not a member of Smile communications");
            }

            if (!customersNewRolesInOrganisations.isEmpty()) {
                List<String> callingCustomersSecurityGroups = IMDAO.getCustomersSecurityGroups(em, IMDAO.getCustomerProfileIdBySSOIdentity(em, modifiedCustomer.getPlatformContext().getOriginatingIdentity()));
                for (CustomerRole newRole : customersNewRolesInOrganisations) {
                    String roles = IMDAO.getOrganisationModificationRoles(em, newRole.getOrganisationId());
                    List<String> modificationRoles = Utils.getListFromCRDelimitedString(roles);
                    if (!modificationRoles.isEmpty() && !Utils.listsIntersect(modificationRoles, callingCustomersSecurityGroups)) {
                        throw new Exception("You have insufficient permissions to modify customer roles");
                    }
                }

                //For UG - KYC status must be set to Organisational when a role is created for the customer.
//                if (BaseUtils.getBooleanProperty("env.customer.verify.with.nira", false)) {
//                    dbCustomer.setkYCStatus("O");
//                    em.persist(dbCustomer);
//                }
                detailsChanged = true;
            }

            if (BaseUtils.getBooleanProperty("env.customer.verify.with.nira", false)) {
                //Set it to  P if the roles have been removed in UG only
                List<CustomerRole> customersRolesInOrganisations = IMDAO.getCustomerRolesByCustomerProfileId(em, dbCustomer.getCustomerProfileId());

                if (customersRolesInOrganisations.size() <= 0
                        && dbCustomer.getkYCStatus() != null
                        && dbCustomer.getkYCStatus().equalsIgnoreCase("O")
                        && customersNewRolesInOrganisations.isEmpty()) {
                    dbCustomer.setkYCStatus("P");
                    em.persist(dbCustomer);
                }
            }

            IMDAO.setCustomerRoles(em, modifiedCustomer.getCustomerRoles(), modifiedCustomer.getCustomerId());

            for (String tcWasOustanding : IMDAO.getCustomersOutstandingTermsAndConditions(em, modifiedCustomer.getCustomerId())) {
                boolean stillOustanding = false;
                for (String tcStillOutstanding : modifiedCustomer.getOutstandingTermsAndConditions()) {
                    if (tcWasOustanding.equals(tcStillOutstanding)) {
                        stillOustanding = true;
                        break;
                    }
                }
                if (!stillOustanding) {
                    log.debug("Customer [{}] has accepted terms and conditions [{}]", modifiedCustomer.getCustomerId(), tcWasOustanding);
                    IMDAO.setCustomersTermsAndConditionsAccepted(em, modifiedCustomer.getCustomerId(), tcWasOustanding);
                }
            }
            createEvent(dbCustomer.getCustomerProfileId());

            if (detailsChanged) {
                List<CustomerRole> customersRolesInOrganisations = IMDAO.getCustomerRolesByCustomerProfileId(em, dbCustomer.getCustomerProfileId());
                String organisationRoles = "";
                for (CustomerRole role : customersRolesInOrganisations) {
                    organisationRoles += role.getOrganisationName() + ", ";
                }
                if (!organisationRoles.isEmpty()) {
                    organisationRoles = organisationRoles.substring(0, organisationRoles.length() - 2);
                }
                EventHelper.sendCustomerDetailChangeOld(dbCustomer.getCustomerProfileId(), modifiedCustomer, organisationRoles);
                PlatformEventManager.createEvent("CL_CUST", "CHANGE",
                        String.valueOf(dbCustomer.getCustomerProfileId()),
                        "CustId=" + dbCustomer.getCustomerProfileId());
            }

            //For UG - KYC status must be set to Organisational when a role is created for the customer.
            if (BaseUtils.getBooleanProperty("env.kycstatus.organisational.enabled", false)) {
                if (dbCustomer.getNationality().equalsIgnoreCase(BaseUtils.getProperty("env.locale.country.for.language.en"))) {
                    if (dbCustomer.getkYCStatus().equalsIgnoreCase("V")) {
                        if (IMDAO.getCustomerRolesByCustomerProfileId(em, dbCustomer.getCustomerProfileId()).size() > 0) {
                            dbCustomer.setkYCStatus("O");
                            em.persist(dbCustomer);
                        }
                    } else if (dbCustomer.getkYCStatus().equalsIgnoreCase("O") && IMDAO.getCustomerRolesByCustomerProfileId(em, dbCustomer.getCustomerProfileId()).size() <= 0) {
                        dbCustomer.setkYCStatus("V");
                        em.persist(dbCustomer);
                    }
                }
            }

            if (BaseUtils.getBooleanProperty("env.customer.mandatory.kyc.enable", false)) {
                if (modifiedCustomer.getMandatoryKYCFields() != null) {
                    if (modifiedCustomer.getMandatoryKYCFields().getCustomerId() != 0) {

                        log.debug("Mandatory kyc fields customer Id [{}]", modifiedCustomer.getMandatoryKYCFields().getCustomerId());
                        com.smilecoms.im.db.model.MandatoryKYCFields dBMandatoryKYCFields = IMDAO.getCustomersMandatoryKYCFields(em, modifiedCustomer.getMandatoryKYCFields().getCustomerId());

                        if (dBMandatoryKYCFields != null) {
                            if (dBMandatoryKYCFields.getCustomerId() == modifiedCustomer.getMandatoryKYCFields().getCustomerId()) {
                                syncXMLMandatoryKYCFieldsIntoDB(modifiedCustomer.getMandatoryKYCFields(), dBMandatoryKYCFields);
                                em.persist(dBMandatoryKYCFields);
                            }
                        } else {
                            log.debug("Mandatory kyc fields customer Id [{}]", modifiedCustomer.getMandatoryKYCFields().getCustomerId());
                            com.smilecoms.im.db.model.MandatoryKYCFields dBKycStatus = new com.smilecoms.im.db.model.MandatoryKYCFields();
                            syncXMLMandatoryKYCFieldsIntoDB(modifiedCustomer.getMandatoryKYCFields(), dBKycStatus);
                            em.persist(dBKycStatus);
                        }
                    }
                }
            }

            if (BaseUtils.getBooleanProperty("env.customer.nin.data.enable", false)) {
                if (modifiedCustomer.getCustomerNinData() != null) {
                    if (modifiedCustomer.getCustomerNinData().getCustomerProfileId() != 0) {
                        log.debug("CustomerNinData field customer Id [{}]", modifiedCustomer.getCustomerNinData().getCustomerProfileId());
                        com.smilecoms.im.db.model.CustomerNinData dBCustomerNinData = IMDAO.getCustomerNinDatas(em, modifiedCustomer.getCustomerNinData().getCustomerProfileId());
                        
                        log.debug("CustomerNinData field customer Id [{}]", dBCustomerNinData);
                        if (dBCustomerNinData != null) {
                            log.debug("dBCustomerNinData was not NULL");
                            if (dBCustomerNinData.getCustomerProfileId() == modifiedCustomer.getCustomerNinData().getCustomerProfileId()) {
                                log.debug("ProfileIDs matched, going to persist dBCustomerNinData");
                                syncXMLCustomerNinDataIntoDB(modifiedCustomer.getCustomerNinData(), dBCustomerNinData);
                                
                                log.debug("dBCustomerNinData has: {} ", dBCustomerNinData.toString());
                                
                                try {
                                    em.persist(dBCustomerNinData);
                                    em.flush();
                                } catch (Exception e) {
                                    log.warn("THE ERROR IS: {}", e);
                                }
                            }
                        } else {
                            com.smilecoms.im.db.model.CustomerNinData dBCustNinData = new com.smilecoms.im.db.model.CustomerNinData();
                            syncXMLCustomerNinDataIntoDB(modifiedCustomer.getCustomerNinData(), dBCustNinData);
                            em.persist(dBCustNinData);
                            em.flush();
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warn("WHAT WENT WRONG: {}", e);
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    private boolean isAllowed(String roles, PlatformContext ctx) {

        List<String> groups = IMDAO.getCustomersSecurityGroups(em, IMDAO.getCustomerProfileBySSOIdentity(em, ctx.getOriginatingIdentity()).getCustomerProfileId());

        // Set usersRoles = SCAWrapper.getAdminInstance().getThreadsRequestContext().getUsersRoles();
        StringTokenizer stValues = new StringTokenizer(roles, "\r\n");
        while (stValues.hasMoreTokens()) {
            String role = stValues.nextToken().trim();
            if (!role.isEmpty() && groups.contains(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PlatformInteger addOrganisation(Organisation newOrganisation) throws IMError {
        setContext(newOrganisation, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        PlatformInteger organisationId = new PlatformInteger();
        try {

            com.smilecoms.im.db.model.Organisation dbOrganisation = new com.smilecoms.im.db.model.Organisation();
            syncXMLOrganisationIntoDBOrganisation(newOrganisation, dbOrganisation);
            dbOrganisation.setVersion(0);
            dbOrganisation.setCreatedDatetime(new Date());
            int createdById = IMDAO.getCustomerProfileIdBySSOIdentity(em, newOrganisation.getPlatformContext().getOriginatingIdentity());
            dbOrganisation.setCreatedByCustomerProfileId(createdById);
            dbOrganisation.setAccountManagerCustomerProfileId(createdById);
            dbOrganisation.setStatus("AC");

            dbOrganisation.setKycComment(newOrganisation.getKycComment());
            dbOrganisation.setKycStatus(newOrganisation.getKycStatus());

            dbOrganisation.setOrganisationSubType(newOrganisation.getOrganisationSubType() == null ? "" : newOrganisation.getOrganisationSubType());

            em.persist(dbOrganisation);
            em.flush();
            em.refresh(dbOrganisation);

            for (Address newAddress : newOrganisation.getAddresses()) {
                newAddress.setOrganisationId(dbOrganisation.getOrganisationId());
                com.smilecoms.im.db.model.Address dbAddress = new com.smilecoms.im.db.model.Address();
                syncXMLAddressIntoDBAddress(newAddress, dbAddress);
                em.persist(dbAddress);
            }
            if (!newOrganisation.getOrganisationPhotographs().isEmpty()) {
                IMDAO.setOrganisationsPhotographs(em, newOrganisation.getOrganisationPhotographs(), dbOrganisation.getOrganisationId());
            }

            organisationId.setInteger(dbOrganisation.getOrganisationId());
            createEvent(dbOrganisation.getOrganisationId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return organisationId;
    }

    @Override
    public Done modifyOrganisation(Organisation modifiedOrganisation) throws IMError {
        setContext(modifiedOrganisation, wsctx);
        if (log.isDebugEnabled()) {
            logStart(modifiedOrganisation.getOrganisationId());
        }
        try {

            com.smilecoms.im.db.model.Organisation dbOrganisation = IMDAO.getOrganisationByOrganisationId(em, modifiedOrganisation.getOrganisationId());
            final List<String> modificationRoles = Utils.getListFromCRDelimitedString(dbOrganisation.getModificationRoles());
            List<String> groups = IMDAO.getCustomersSecurityGroups(em, IMDAO.getCustomerProfileIdBySSOIdentity(em, modifiedOrganisation.getPlatformContext().getOriginatingIdentity())); //TODO To be completed
            JPAUtils.checkLastModified(modifiedOrganisation.getVersion(), dbOrganisation.getVersion());
            if (!dbOrganisation.getModificationRoles().equalsIgnoreCase("")) {
                if (!Utils.listsIntersect(modificationRoles, groups)) {
                    throw new Exception("You have insufficient permissions to modify the organisation");
                }
            }
            syncXMLOrganisationIntoDBOrganisation(modifiedOrganisation, dbOrganisation);

            em.persist(dbOrganisation);
            em.flush();
            if (!modifiedOrganisation.getOrganisationPhotographs().isEmpty()) {
                IMDAO.setOrganisationsPhotographs(em, modifiedOrganisation.getOrganisationPhotographs(), modifiedOrganisation.getOrganisationId());
            }

            if (modifiedOrganisation.getOrganisationSellers().size() > 0) {
                log.warn("THERE ARE SELLERS. GOING TO MODIFY");
                IMDAO.setOrganisationSellers(em, modifiedOrganisation.getOrganisationSellers(), modifiedOrganisation.getOrganisationId());
            }

            createEvent(modifiedOrganisation.getOrganisationId());

        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public OrganisationList getOrganisations(OrganisationQuery organisationQuery) throws IMError {
        setContext(organisationQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        OrganisationList organisationList = new OrganisationList();

        try {
            Map<Integer, com.smilecoms.im.db.model.Organisation> dbOrgs = new HashMap<>();

            if (organisationQuery.getOrganisationId() > 0) {
                log.debug("Looking up a single organisation with ID [{}]", organisationQuery.getOrganisationId());
                com.smilecoms.im.db.model.Organisation dbOrganisation = IMDAO.getOrganisationByOrganisationId(em, organisationQuery.getOrganisationId());
                dbOrgs.put(dbOrganisation.getOrganisationId(), dbOrganisation);
            } else {

                int limit = organisationQuery.getResultLimit();
                int resultsToGet = limit;

                String organisationName = organisationQuery.getOrganisationName();

                log.warn("Looking for OrganisationName [{}]", organisationName);
                if (organisationName != null && organisationName.equalsIgnoreCase("All")) {
                    log.warn("Going to get ALL organisations");
                    for (com.smilecoms.im.db.model.Organisation o : IMDAO.getAllOrganisations(em)) {
                        dbOrgs.put(o.getOrganisationId(), o);
                    }
                }

                if (organisationName != null && !organisationName.isEmpty() && !organisationName.equalsIgnoreCase("All") && resultsToGet > 0) {
                    for (com.smilecoms.im.db.model.Organisation o : IMDAO.getOrganisationsByWildcardedName(em, organisationName, resultsToGet)) {
                        dbOrgs.put(o.getOrganisationId(), o);
                        dbOrgs.put(o.getOrganisationId(), o);
                    }
                    resultsToGet = limit - dbOrgs.size();
                }

                int customerId = organisationQuery.getCustomerId();
                if (customerId > 0 && resultsToGet > 0) {
                    log.debug("Searching using customer profile id [{}]", customerId);
                    for (com.smilecoms.im.db.model.Organisation o : IMDAO.getOrganisationsByCustomerProfileId(em, customerId, resultsToGet)) {
                        dbOrgs.put(o.getOrganisationId(), o);
                    }
                }
            }

            log.debug("Adding [{}] return results", dbOrgs.size());
            for (com.smilecoms.im.db.model.Organisation dbOrg : dbOrgs.values()) {
                organisationList.getOrganisations().add(getXMLOrganisation(dbOrg, organisationQuery.getVerbosity(), organisationQuery.getRolesOffset(), organisationQuery.getRolesResultLimit()));

            }
            organisationList.setNumberOfOrganisations(organisationList.getOrganisations().size());

        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }

        for (Organisation org : organisationList.getOrganisations()) {
            log.debug("RETURNING ORG {} with [{}] Sellers to returnedList", org.getOrganisationId(), org.getOrganisationSellers().size());
        }

        return organisationList;
    }

    private boolean isCallerAnAdministrator(PlatformContext ctx) {

        if (ctx.getOriginatingIdentity().equals("NOT_LOGGED_IN")) {
            // Needed so that an admin can change their password when not logged in
            return true;
        }
        List<String> groups = IMDAO.getCustomersSecurityGroups(em, IMDAO.getCustomerProfileBySSOIdentity(em, ctx.getOriginatingIdentity()).getCustomerProfileId());
        for (String group : groups) {
            if (group.equalsIgnoreCase("administrator")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SSOPasswordResetLink sendSSOPasswordResetLink(SSOPasswordResetLinkData ssoPasswordResetLinkData) throws IMError {
        setContext(ssoPasswordResetLinkData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        SSOPasswordResetLink res = new SSOPasswordResetLink();
        try {
            // First check if the identifier is a sso identity
            CustomerProfile customer = null;
            if (ssoPasswordResetLinkData.getIdentifier().isEmpty()) {
                throw new Exception("Identity is empty");
            }
            try {
                customer = IMDAO.getCustomerProfileBySSOIdentity(em, ssoPasswordResetLinkData.getIdentifier());
            } catch (Exception e) {
            }
            if (customer == null) {
                try {
                    customer = IMDAO.getCustomerProfileByEmailAddress(em, ssoPasswordResetLinkData.getIdentifier());
                } catch (Exception e) {
                }
            }
            if (customer == null) {
                throw new Exception("No customer data found");
            }

            for (String group : IMDAO.getCustomersSecurityGroups(em, customer.getCustomerProfileId())) {
                if (group.equalsIgnoreCase("Administrator")) {
                    throw new Exception("Administrators cannot get a password reset link");
                }
            }

            // We have a customer
            String emailAddress = customer.getEmailAddress();
            if (emailAddress.isEmpty()) {
                throw new Exception("Customer without an email address cannot get password reset link");
            }

            SsoPasswordReset pr = new SsoPasswordReset();
            pr.setCustomerProfileId(customer.getCustomerProfileId());
            pr.setExpiryDatetime(Utils.getFutureDate(Calendar.HOUR, 2));
            pr.setGuid(Utils.getUUID());
            em.persist(pr);
            em.flush();
            String link = BaseUtils.getProperty("env.password.reset.link.prefix") + pr.getGuid();
            if (BaseUtils.getBooleanProperty("env.im.send.password.reset.email.direct", true)) {
                Object[] params = new Object[]{customer.getFirstName(), customer.getLastName(), customer.getSsoIdentity(), link};
                String msg = LocalisationHelper.getLocalisedString(LocalisationHelper.getLocaleForLanguage(customer.getLanguage()), "reset.password.email.body", params);
                String subject = LocalisationHelper.getLocalisedString(LocalisationHelper.getLocaleForLanguage(customer.getLanguage()), "reset.password.email.subject");
                IMAPUtils.sendEmail(BaseUtils.getProperty("env.smtp.customercomms.from"), emailAddress, subject, msg);
            }
            res.setExpiry(Utils.getDateAsXMLGregorianCalendar(pr.getExpiryDatetime()));
            res.setGUID(pr.getGuid());
            res.setURL(link);
            PlatformEventManager.createEvent("IM", "PasswordResetLink", String.valueOf(customer.getCustomerProfileId()), emailAddress + "|" + customer.getFirstName() + "|" + pr.getGuid() + "|" + link + "|" + pr.getExpiryDatetime());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return res;

    }

    @Override
    public Done resetSSOPassword(SSOPasswordResetData ssoPasswordResetData) throws IMError {
        setContext(ssoPasswordResetData, wsctx);
        if (log.isDebugEnabled()) {
            logStart(ssoPasswordResetData.getGUID());
        }
        try {

            SsoPasswordReset pr = IMDAO.getSSOPasswordReset(em, ssoPasswordResetData.getGUID());
            em.remove(pr);
            if (pr.getExpiryDatetime().before(new Date())) {
                throw new Exception("SSO reset has expired");
            }
            CustomerProfile dbCustomer = IMDAO.getCustomerProfileById(em, pr.getCustomerProfileId());
            dbCustomer.setSsoDigest(getEncryptedPassword(ssoPasswordResetData.getNewSSODigest()));
            em.persist(dbCustomer);
            em.flush();
            createEvent(pr.getCustomerProfileId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();

    }

    @Override
    public TenantData getCustomerTenant(TenantQuery tenantQuery) throws IMError {
        setContext(tenantQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        TenantData ret = new TenantData();
        try {
            try {
                ret.setTenant(IMDAO.getTenantBySSOIdentity(em, tenantQuery.getSSOIdentity()));
            } catch (Exception e) {
                log.warn("Error getting tenant [{}]. Will default to [{}] while development is in progress", e.toString(), ret.getTenant());
            }
            if (ret.getTenant() == null || ret.getTenant().isEmpty()) {
                ret.setTenant(BaseUtils.getProperty("env.im.default.tenant", "sm"));
            }
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return ret;
    }

    @Override
    public Done addAddress(Address newAddress) throws IMError {
        setContext(newAddress, wsctx);
        if (log.isDebugEnabled()) {
            logStart(newAddress.getCustomerId());
        }
        try {
            com.smilecoms.im.db.model.Address dbAddress = new com.smilecoms.im.db.model.Address();
            syncXMLAddressIntoDBAddress(newAddress, dbAddress);
            em.persist(dbAddress);
            em.flush();
            IMDAO.setCustomerProfileUpdatedDatetime(em, newAddress.getCustomerId());
            createEvent(dbAddress.getAddressId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done deleteAddress(Address addressToDelete) throws IMError {
        setContext(addressToDelete, wsctx);
        if (log.isDebugEnabled()) {
            logStart(addressToDelete.getCustomerId());
        }
        try {
            IMDAO.deleteAddress(em, addressToDelete.getAddressId());
            em.flush();
            IMDAO.setCustomerProfileUpdatedDatetime(em, addressToDelete.getCustomerId());
            createEvent(addressToDelete.getAddressId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done modifyAddress(Address modifiedAddress) throws IMError {
        setContext(modifiedAddress, wsctx);
        if (log.isDebugEnabled()) {
            logStart(modifiedAddress.getCustomerId());
        }
        Customer customer = null;
        try {
            com.smilecoms.im.db.model.Address dbAddress = IMDAO.getAddress(em, modifiedAddress.getAddressId());
            syncXMLAddressIntoDBAddress(modifiedAddress, dbAddress);
            em.persist(dbAddress);
            em.flush();
            IMDAO.setCustomerProfileUpdatedDatetime(em, modifiedAddress.getCustomerId());
            createEvent(dbAddress.getAddressId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    private void syncXMLAddressIntoDBAddress(Address xmlAddress, com.smilecoms.im.db.model.Address dbAddress) throws Exception {
        dbAddress.setAddressId(xmlAddress.getAddressId());
        dbAddress.setCustomerProfileId(xmlAddress.getCustomerId() <= 0 ? null : xmlAddress.getCustomerId());
        dbAddress.setOrganisationId(xmlAddress.getOrganisationId() <= 0 ? null : xmlAddress.getOrganisationId());
        dbAddress.setType(xmlAddress.getType());
        dbAddress.setCode(xmlAddress.getCode());
        dbAddress.setCountry(xmlAddress.getCountry());
        dbAddress.setLine1(xmlAddress.getLine1());
        dbAddress.setLine2(xmlAddress.getLine2());
        dbAddress.setZone(xmlAddress.getZone());
        dbAddress.setState(xmlAddress.getState());
        dbAddress.setPostalMatchesPhysical(xmlAddress.isPostalMatchesPhysical() ? "Y" : "N");
        dbAddress.setTown(xmlAddress.getTown());

        // Could optimise with cache at some point but not necessary due to volume
        // Only do the below validations if the captured address is within the operating country (local states and zones).
        // Overseas states and zones cannot be validated as we do not keep track of these in the DB (could be looked at in the future).
        if (dbAddress.getCountry().equalsIgnoreCase(BaseUtils.getProperty("env.country.name"))
                && !BaseUtils.getBooleanProperty("env.customer.verify.with.nida", false)) {
            if (!IMDAO.stateExists(em, dbAddress.getState())) {
                throw new Exception("Invalid address -- State - " + dbAddress.getState());
            }
            if (!IMDAO.zoneExists(em, dbAddress.getZone(), dbAddress.getState())) {
                throw new Exception("Invalid address -- Zone - " + dbAddress.getZone());
            }
        }
        // Validate the country...
        if (!IMDAO.countryExists(em, dbAddress.getCountry())) {
            throw new Exception("Invalid address -- Country - " + dbAddress.getCountry());
        }
    }

    private void syncXMLCustomerIntoDBCustomer(Customer xmlCustomer, CustomerProfile dbCustomer) throws Exception {
        dbCustomer.setAlternativeContact1(Utils.getFriendlyPhoneNumberKeepingCountryCode(xmlCustomer.getAlternativeContact1()));
        dbCustomer.setAlternativeContact2(Utils.getFriendlyPhoneNumberKeepingCountryCode(xmlCustomer.getAlternativeContact2()));
        dbCustomer.setDateOfBirth(xmlCustomer.getDateOfBirth().replaceAll("/", ""));
        dbCustomer.setEmailAddress(xmlCustomer.getEmailAddress());
        dbCustomer.setTitle(xmlCustomer.getTitle() == null ? "" : xmlCustomer.getTitle());
        dbCustomer.setFirstName(xmlCustomer.getFirstName());
        if (xmlCustomer.getKYCStatus() != null && !xmlCustomer.getKYCStatus().isEmpty()) {
            dbCustomer.setkYCStatus(xmlCustomer.getKYCStatus());
        }
        dbCustomer.setGender(xmlCustomer.getGender());
        if (dbCustomer.getGender() == null || (!dbCustomer.getGender().equals("M") && !dbCustomer.getGender().equals("F"))) {
            throw new Exception("Invalid customer gender. Must be one of M or F");
        }

        if (xmlCustomer.getCardNumber() != null && !xmlCustomer.getCardNumber().isEmpty()) {
            dbCustomer.setCardNumber(xmlCustomer.getCardNumber());
        }

        dbCustomer.setIdNumber(xmlCustomer.getIdentityNumber());
        dbCustomer.setIdNumberType(xmlCustomer.getIdentityNumberType());

        dbCustomer.setNationalIdNumber(xmlCustomer.getNationalIdentityNumber());
        dbCustomer.setIsNinVerified(xmlCustomer.getIsNinVerified() == null ? "N" : xmlCustomer.getIsNinVerified());
        dbCustomer.setLanguage(xmlCustomer.getLanguage());
        dbCustomer.setSsoAuthFlags(xmlCustomer.getSSOAuthFlags());
        dbCustomer.setLastName(xmlCustomer.getLastName());
        dbCustomer.setMiddleName(xmlCustomer.getMiddleName());
        dbCustomer.setStatus(xmlCustomer.getCustomerStatus() == null ? "AC" : xmlCustomer.getCustomerStatus());
        dbCustomer.setSsoIdentity(xmlCustomer.getSSOIdentity());
        dbCustomer.setSsoDigest(getEncryptedPassword(xmlCustomer.getSSODigest()));
        dbCustomer.setMothersMaidenName(xmlCustomer.getMothersMaidenName());
        dbCustomer.setNationality(xmlCustomer.getNationality());
        dbCustomer.setPassportExpiryDate(xmlCustomer.getPassportExpiryDate().replaceAll("/", ""));

        dbCustomer.setVisaExpiryDate(xmlCustomer.getVisaExpiryDate() == null ? "" : xmlCustomer.getVisaExpiryDate().replaceAll("/", ""));

        // dbCustomer.setVisaExpiryDate(xmlCustomer.getVisaExpiryDate().replaceAll("/", ""));
        dbCustomer.setWarehouseId(xmlCustomer.getWarehouseId() == null ? "" : xmlCustomer.getWarehouseId());
        dbCustomer.setReferralCode(xmlCustomer.getReferralCode());

        if (xmlCustomer.getAccountManagerCustomerProfileId() > 0) {
            dbCustomer.setAccountManagerCustomerProfileId(xmlCustomer.getAccountManagerCustomerProfileId());
        }

        /*
         BitMask for Messaging Opt in Level
         Bit 0 = 1 = General Notification Emails and SMS's         
         Bit 1 = 2 = Post Call SMS
         Bit 2 = 4 = Marketing Emails
         So:
        
         7 = Marketing Emails + Notification Emails + Post Call SMS
         6 = Marketing Emails + Post Call SMS
         5 = Marketing Emails + Notification Emails
         4 = Marketing Emails
         3 = Notification Emails + Post Call SMS
         2 = Post Call SMS
         1 = Notification Emails
         0 = Nothing
        
         
        
         */
        dbCustomer.setOptInLevel(xmlCustomer.getOptInLevel() == -1 ? BaseUtils.getIntProperty("env.customer.default.opt.in.level", Integer.MAX_VALUE) : xmlCustomer.getOptInLevel());
    }

    private void syncXMLOrganisationIntoDBOrganisation(Organisation xmlOrganisation, com.smilecoms.im.db.model.Organisation dbOrganisation) {
        dbOrganisation.setAccountManagerCustomerProfileId(xmlOrganisation.getAccountManagerCustomerProfileId());
        dbOrganisation.setAlternativeContact1(xmlOrganisation.getAlternativeContact1());
        dbOrganisation.setAlternativeContact2(xmlOrganisation.getAlternativeContact2());
        dbOrganisation.setCompanyNumber(xmlOrganisation.getCompanyNumber());
        dbOrganisation.setEmailAddress(xmlOrganisation.getEmailAddress());
        dbOrganisation.setIndustry(xmlOrganisation.getIndustry());
        if (xmlOrganisation.getKycStatus() != null && !xmlOrganisation.getKycStatus().isEmpty()) {
            dbOrganisation.setKycStatus(xmlOrganisation.getKycStatus());
        }
        if (xmlOrganisation.getKycComment() != null && !xmlOrganisation.getKycComment().isEmpty()) {
            dbOrganisation.setKycComment(xmlOrganisation.getKycComment());
        }

        dbOrganisation.setOrganisationSubType((xmlOrganisation.getOrganisationSubType() == null || xmlOrganisation.getOrganisationSubType().isEmpty()) ? "-" : xmlOrganisation.getOrganisationSubType());
        dbOrganisation.setOrganisationType(xmlOrganisation.getOrganisationType());
        dbOrganisation.setOrganisationName(xmlOrganisation.getOrganisationName());
        dbOrganisation.setSize(xmlOrganisation.getSize());
        dbOrganisation.setStatus(xmlOrganisation.getOrganisationStatus());
        dbOrganisation.setTaxNumber(xmlOrganisation.getTaxNumber());
        dbOrganisation.setCreditAccountNumber(xmlOrganisation.getCreditAccountNumber() == null ? "" : xmlOrganisation.getCreditAccountNumber());
        dbOrganisation.setChannelCode(xmlOrganisation.getChannelCode() == null ? "" : xmlOrganisation.getChannelCode());
        dbOrganisation.setModificationRoles(xmlOrganisation.getModificationRoles() == null ? "" : Utils.makeCRDelimitedStringFromList(xmlOrganisation.getModificationRoles()));

    }

    private String getEncryptedPassword(String maybeEncrypted) {
        if (maybeEncrypted == null || maybeEncrypted.isEmpty()) {
            return "";
        }
        if (maybeEncrypted.length() < 60) {
            return Utils.oneWayHash(maybeEncrypted);
        } else {
            return maybeEncrypted;
        }
    }

    private Customer getXMLCustomer(CustomerProfile dbCustomer, String verbosity) throws Exception {
        Customer customer = new Customer();
        customer.setAlternativeContact1(dbCustomer.getAlternativeContact1());
        customer.setAlternativeContact2(dbCustomer.getAlternativeContact2());
        customer.setClassification(dbCustomer.getClassification());
        customer.setCreatedDateTime(Utils.getDateAsXMLGregorianCalendar(dbCustomer.getCreatedDatetime()));
        customer.setVersion(dbCustomer.getVersion());
        customer.setCustomerId(dbCustomer.getCustomerProfileId());
        customer.setCustomerStatus(dbCustomer.getStatus());
        customer.setDateOfBirth(dbCustomer.getDateOfBirth());
        customer.setEmailAddress(dbCustomer.getEmailAddress());
        customer.setTitle(dbCustomer.getTitle() == null ? "" : dbCustomer.getTitle());
        customer.setFirstName(dbCustomer.getFirstName());
        customer.setGender(dbCustomer.getGender());
        customer.setIdentityNumber(dbCustomer.getIdNumber());
        customer.setIdentityNumberType(dbCustomer.getIdNumberType());
        customer.setNationalIdentityNumber(dbCustomer.getNationalIdNumber());
        customer.setIsNinVerified(dbCustomer.getIsNinVerified() == null ? "" : dbCustomer.getIsNinVerified());
        customer.setLanguage(dbCustomer.getLanguage());
        customer.setLastName(dbCustomer.getLastName());
        customer.setCardNumber(dbCustomer.getCardNumber());
        customer.setMiddleName(dbCustomer.getMiddleName());
        customer.setSSOIdentity(dbCustomer.getSsoIdentity());
        customer.setSSODigest(dbCustomer.getSsoDigest());
        customer.setSSOAuthFlags(dbCustomer.getSsoAuthFlags() == null ? 0 : dbCustomer.getSsoAuthFlags());
        customer.setKYCStatus(dbCustomer.getkYCStatus() == null ? "" : dbCustomer.getkYCStatus());
        customer.getSecurityGroups().addAll(IMDAO.getCustomersSecurityGroups(em, dbCustomer.getCustomerProfileId()));
        customer.getCustomerSellers().addAll(IMDAO.getCustomersSellers(em, dbCustomer.getCustomerProfileId()));
        if (verbosity.contains("PHOTO")) {
            customer.getCustomerPhotographs().addAll(IMDAO.getCustomersPhotographs(em, dbCustomer.getCustomerProfileId()));
        }
        if (verbosity.contains("ADDRESS")) {
            customer.getAddresses().addAll(getXMLAddresses(IMDAO.getCustomersAddresses(em, dbCustomer.getCustomerProfileId())));
        }
        if (verbosity.contains("MANDATEKYCFIELD")) {
            com.smilecoms.im.db.model.MandatoryKYCFields dbMandatoryKYCFields = IMDAO.getCustomersMandatoryKYCFields(em, dbCustomer.getCustomerProfileId());
            if (dbMandatoryKYCFields != null) {
                customer.setMandatoryKYCFields(xMLMandatoryKYCFields(dbMandatoryKYCFields));
            }
        }
        if (verbosity.contains("CUSTOMERNINDATA")) {

            com.smilecoms.im.db.model.CustomerNinData dBCustomerNinData = IMDAO.getCustomerNinDatas(em, dbCustomer.getCustomerProfileId());

            if (dBCustomerNinData != null) {
                customer.setCustomerNinData(xMLCustomerNinData(dBCustomerNinData));

            }
        }
        customer.setAccountManagerCustomerProfileId(dbCustomer.getAccountManagerCustomerProfileId());
        customer.setOptInLevel(dbCustomer.getOptInLevel());
        customer.setReferralCode(dbCustomer.getReferralCode());
        customer.getOutstandingTermsAndConditions().addAll(IMDAO.getCustomersOutstandingTermsAndConditions(em, dbCustomer.getCustomerProfileId()));
        customer.getCustomerRoles().addAll(IMDAO.getCustomerRolesByCustomerProfileId(em, dbCustomer.getCustomerProfileId()));
        customer.setMothersMaidenName(dbCustomer.getMothersMaidenName());
        customer.setPassportExpiryDate(dbCustomer.getPassportExpiryDate());

        customer.setVisaExpiryDate(dbCustomer.getVisaExpiryDate() == null ? "" : dbCustomer.getVisaExpiryDate());

        customer.setNationality(dbCustomer.getNationality());
        customer.setWarehouseId(dbCustomer.getWarehouseId());
        customer.setCreatedByCustomerProfileId(dbCustomer.getCreatedByCustomerProfileId());
        return customer;
    }

    private Organisation getXMLOrganisation(com.smilecoms.im.db.model.Organisation dbOrg, String verbosity, int offset, int limit) throws Exception {
        Organisation org = new Organisation();
        org.setAccountManagerCustomerProfileId(dbOrg.getAccountManagerCustomerProfileId());
        org.setAlternativeContact1(dbOrg.getAlternativeContact1());
        org.setAlternativeContact2(dbOrg.getAlternativeContact2());
        org.setCompanyNumber(dbOrg.getCompanyNumber());
        org.setCreatedDateTime(Utils.getDateAsXMLGregorianCalendar(dbOrg.getCreatedDatetime()));
        org.setEmailAddress(dbOrg.getEmailAddress());
        org.setIndustry(dbOrg.getIndustry());
        org.setOrganisationId(dbOrg.getOrganisationId());
        org.setOrganisationName(dbOrg.getOrganisationName());
        org.setOrganisationStatus(dbOrg.getStatus());
        org.setOrganisationType(dbOrg.getOrganisationType());
        org.setSize(dbOrg.getSize());
        org.setKycStatus((dbOrg.getKycStatus() == null || dbOrg.getKycStatus().isEmpty()) ? "-" : dbOrg.getKycStatus());
        org.setKycComment((dbOrg.getKycComment() == null || dbOrg.getKycComment().isEmpty()) ? "-" : dbOrg.getKycComment());
        org.setTaxNumber(dbOrg.getTaxNumber());
        org.setVersion(dbOrg.getVersion());
        org.setCreditAccountNumber(dbOrg.getCreditAccountNumber());
        org.setChannelCode(dbOrg.getChannelCode());
        org.setCreatedByCustomerProfileId(dbOrg.getCreatedByCustomerProfileId());
        if (verbosity.contains("PHOTO")) {
            org.getOrganisationPhotographs().addAll(IMDAO.getOrganisationsPhotographs(em, dbOrg.getOrganisationId()));
        }
        org.getAddresses().addAll(getXMLAddresses(IMDAO.getOrganisationsAddresses(em, dbOrg.getOrganisationId())));
        if (verbosity.contains("ROLES")) {
            org.getCustomerRoles().addAll(IMDAO.getCustomerRolesByOrganisationId(em, dbOrg.getOrganisationId(), offset, limit));
        }
        org.getModificationRoles().addAll(Utils.getListFromCRDelimitedString(dbOrg.getModificationRoles()));

        org.getOrganisationSellers().addAll(IMDAO.getOrganisationSellers(em, dbOrg.getOrganisationId()));

        log.warn("OrganisationSellers now has: {} SELLERS attached!", org.getOrganisationSellers().size());
        org.setOrganisationSubType((dbOrg.getOrganisationSubType() == null || dbOrg.getOrganisationSubType().isEmpty()) ? "-" : dbOrg.getOrganisationSubType());

        return org;

    }

    private Address getXMLAddress(com.smilecoms.im.db.model.Address dbAddress) {
        Address address = new Address();
        address.setAddressId(dbAddress.getAddressId());
        address.setLine1(dbAddress.getLine1());
        address.setLine2(dbAddress.getLine2());
        address.setZone(dbAddress.getZone());
        address.setTown(dbAddress.getTown());
        address.setState(dbAddress.getState());
        address.setCode(dbAddress.getCode());
        address.setCountry(dbAddress.getCountry());
        address.setType(dbAddress.getType());
        address.setPostalMatchesPhysical("Y".equals(dbAddress.getPostalMatchesPhysical()));
        address.setCustomerId(dbAddress.getCustomerProfileId() == null ? 0 : dbAddress.getCustomerProfileId());
        address.setOrganisationId(dbAddress.getOrganisationId() == null ? 0 : dbAddress.getOrganisationId());
        return address;
    }

    private List<Address> getXMLAddresses(List<com.smilecoms.im.db.model.Address> dbAddresses) {
        List<Address> xmlAddresses = new ArrayList<>();
        for (com.smilecoms.im.db.model.Address dbAddress : dbAddresses) {
            xmlAddresses.add(getXMLAddress(dbAddress));
        }
        return xmlAddresses;
    }

    @Override
    public AuthenticationResult authenticate(AuthenticationQuery authenticationQuery) throws IMError {
        setContext(authenticationQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart(authenticationQuery.getSSOIdentity());
        }
        AuthenticationResult result = null;
        try {

            if (authenticationQuery.getSSOIdentity() == null || authenticationQuery.getSSOIdentity().isEmpty()) {
                if (authenticationQuery.getIMSPublicIdentity() != null && !authenticationQuery.getIMSPublicIdentity().isEmpty()) {
                    return appAuthenticate(authenticationQuery);
                } else {
                    throw new Exception("SSOIdentity is null or empty");
                }
            }

            /*
             * Initialization - Defaults
             */
            result = new AuthenticationResult();
            result.setDone(StDone.FALSE);

            /*
             * First check Authentication lock
             */
            CustomerProfile customerProfile = IMDAO.getLockedCustomerProfileBySSOIdentity(em, authenticationQuery.getSSOIdentity());

            boolean isAStaffMember = isCustomerAStaffMember(customerProfile.getCustomerProfileId());
            if (customerProfile.getSsoDigest().isEmpty()) {
                // The customers password is empty
                if (!isAStaffMember || (isAStaffMember && !BaseUtils.getBooleanProperty("env.active.directory.auth.enabled", false))) {
                    // Its not a staff member or it is a staff member and we dont use AD for authentication
                    throw new Exception("Customer has no password");
                }
            }

            // Get authlock date
            Date authLock = customerProfile.getSsoLockExpiry();
            // Get auth attempt        
            short authAttempt = customerProfile.getSsoAuthAttempts();
            int maxAuthAttempt = BaseUtils.getIntProperty("global.customer.auth.maxattempt");

            if (log.isDebugEnabled()) {
                log.debug("Customer authLock: [" + authLock + "] - authAttempt: [" + authAttempt + "] - maxAuthAttempt: [" + maxAuthAttempt + "]");
            }

            Date currentDateTime = new java.util.Date();
            if (authLock != null && currentDateTime.before(authLock)) {
                throw createError(IMError.class, "Customer account temporarily locked -- unit " + authLock + " due to " + maxAuthAttempt + " invalid attempts...");
            }

            boolean isAuthenticated = authUser(customerProfile, authenticationQuery.getSSOEncryptedPassword());

            if (isAuthenticated) {
                result.setDone(StDone.TRUE);
                result.setCustomerStatus(customerProfile.getStatus());
                result.getSecurityGroups().addAll(IMDAO.getCustomersSecurityGroups(em, customerProfile.getCustomerProfileId()));
                if (authAttempt > 0) {
                    customerProfile.setSsoAuthAttempts((short) 0);
                    customerProfile.setSsoLockExpiry(null);
                    em.persist(customerProfile);
                }
                EventHelper.sendCustomerAuthenticateAttempt(
                        authenticationQuery.getPlatformContext().getOriginatingIP(),
                        "PASS",
                        authenticationQuery.getSSOIdentity(),
                        customerProfile.getCustomerProfileId());
            } else {
                authAttempt++;
                if (authAttempt == maxAuthAttempt) {
                    authAttempt = 0;
                    int authTimeout = BaseUtils.getIntProperty("global.customer.auth.timeout.secs");
                    Calendar rightNow = Calendar.getInstance();
                    rightNow.add(Calendar.SECOND, authTimeout);
                    customerProfile.setSsoLockExpiry(rightNow.getTime());
                }
                customerProfile.setSsoAuthAttempts(authAttempt);
                em.persist(customerProfile);
                em.flush();
                EventHelper.sendCustomerAuthenticateAttempt(
                        authenticationQuery.getPlatformContext().getOriginatingIP(),
                        "FAIL",
                        authenticationQuery.getSSOIdentity(),
                        customerProfile.getCustomerProfileId());
            }
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    /*
     *
     * WLAN FUNCTIONALITY
     *
     */
    @Override
    public NAIIdentity getNAIIdentity(NAIIdentityQuery naiIdentityQuery) throws IMError {
        setContext(naiIdentityQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        NAIIdentity nai = null;
        try {
            NetworkAccessIdentifier dbNai = null;
            if (naiIdentityQuery.getNAIUsername() != null && !naiIdentityQuery.getNAIUsername().isEmpty()) {
                dbNai = IMDAO.getNetworkAccessIdentifierByIdentity(em, naiIdentityQuery.getNAIUsername());
            } else if (naiIdentityQuery.getNAIIdentityId() > 0) {
                dbNai = IMDAO.getNetworkAccessIdentifierByIdentityId(em, naiIdentityQuery.getNAIIdentityId());
            } else if (naiIdentityQuery.getOSSBSSReferenceId() != null && !naiIdentityQuery.getOSSBSSReferenceId().isEmpty()) {
                dbNai = IMDAO.getNetworkAccessIdentifierByOSSBSSReferenceId(em, naiIdentityQuery.getOSSBSSReferenceId());
            } else {
                throw new Exception("Network Access Identifier Query contained no query data");
            }

            nai = getXMLNAIIdentity(dbNai);
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return nai;
    }

    @Override
    public Done deleteNAIIdentity(NAIIdentity deleteNAIIdentityRequest) throws IMError {
        setContext(deleteNAIIdentityRequest, wsctx);

        if (log.isDebugEnabled()) {
            logStart(deleteNAIIdentityRequest.getNAIUsername());
        }
        try {
            NetworkAccessIdentifier nai = JPAUtils.findAndThrowENFE(em, NetworkAccessIdentifier.class, deleteNAIIdentityRequest.getNAIIdentityId());
            em.remove(nai);
            em.flush();
            createEvent(nai.getId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }

        return makeDone();
    }

    @Override
    public NAIIdentity createNAIIdentity(NAIIdentity newNAIIdentity) throws IMError {
        setContext(newNAIIdentity, wsctx);
        NAIIdentity result = null;
        if (log.isDebugEnabled()) {
            logStart(newNAIIdentity.getNAIUsername());
        }
        try {
            NetworkAccessIdentifier nai = new NetworkAccessIdentifier();
            nai.setNAIUsername(newNAIIdentity.getNAIUsername());
            nai.setInfo(newNAIIdentity.getInfo() != null ? newNAIIdentity.getInfo() : "");
            nai.setOSSBSSReferenceId(Utils.getUUID());
            nai.setNAIPassword(newNAIIdentity.getNAIPassword());
            nai.setStatus("AC");
            nai.setWLANAttachStatus("");
            em.persist(nai);
            em.flush();
            em.refresh(nai);
            result = getXMLNAIIdentity(nai);
            createEvent(nai.getId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public NAIIdentity modifyNAIIdentity(NAIIdentity modifiedNAIIdentity) throws IMError {
        setContext(modifiedNAIIdentity, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        NAIIdentity xmlNai = null;
        try {
            NetworkAccessIdentifier nai = IMDAO.getNetworkAccessIdentifierByIdentity(em, modifiedNAIIdentity.getNAIUsername());

            if (modifiedNAIIdentity.getNAIUsername() != null) {
                nai.setNAIUsername(modifiedNAIIdentity.getNAIUsername());
            }

            if (modifiedNAIIdentity.getInfo() != null) {
                nai.setInfo(modifiedNAIIdentity.getInfo());
            }

            if (modifiedNAIIdentity.getInfo() != null) {
                nai.setInfo(modifiedNAIIdentity.getInfo());
            }

            if (modifiedNAIIdentity.getStatus() != null) {
                log.debug("Changing Network Access Identifier status from [{}] to [{}]", nai.getStatus(), modifiedNAIIdentity.getStatus());
                nai.setStatus(modifiedNAIIdentity.getStatus());
            }

            em.persist(nai);
            em.flush();
            em.refresh(nai);
            xmlNai = getXMLNAIIdentity(nai);
            createEvent(nai.getId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return xmlNai;
    }

    private NAIIdentity getXMLNAIIdentity(NetworkAccessIdentifier dbNai) {
        log.debug("In getXMLNAIIdentity");
        NAIIdentity nai = new NAIIdentity();
        nai.setNAIIdentityId(dbNai.getId());
        nai.setNAIUsername(dbNai.getNAIUsername());
        nai.setNAIPassword(dbNai.getNAIPassword());
        nai.setStatus(dbNai.getStatus() == null ? "AC" : dbNai.getStatus());
        nai.setOSSBSSReferenceId(dbNai.getOSSBSSReferenceId());
        nai.setInfo(dbNai.getInfo() != null ? dbNai.getInfo() : "");
        nai.setInfo(dbNai.getInfo() != null ? dbNai.getInfo() : "");
        return nai;
    }

    /*
     *
     * HSS FUNCTIONALITY
     *
     */
    @Override
    public IMSPrivateIdentity createIMSPrivateIdentity(IMSPrivateIdentity newIMSPrivateIdentity) throws IMError {
        setContext(newIMSPrivateIdentity, wsctx);
        IMSPrivateIdentity result = null;
        if (log.isDebugEnabled()) {
            logStart(newIMSPrivateIdentity.getIdentity());
        }
        try {
            Impi impi = new Impi();
            impi.setAmf(newIMSPrivateIdentity.getAuthenticationManagementField());
            impi.setAuthScheme(Impi.generateAuthScheme(true, true, true, true, true, true, true, true, true));
            impi.setDefaultAuthScheme(newIMSPrivateIdentity.getDefaultAuthScheme());
            if (newIMSPrivateIdentity.getIMSSubscriptionId() > 0) {
                Imsu imsu = JPAUtils.findAndThrowENFE(em, Imsu.class, newIMSPrivateIdentity.getIMSSubscriptionId());
                impi.setImsu(imsu);
            } else {
                impi.setImsu(null);
            }
            impi.setIdentity(newIMSPrivateIdentity.getIdentity());
            impi.setIp("");
            impi.setSimLockedImeiList(newIMSPrivateIdentity.getSIMLockedIMEIList() != null ? newIMSPrivateIdentity.getSIMLockedIMEIList() : "");

            impi.setInfo(newIMSPrivateIdentity.getInfo() != null ? newIMSPrivateIdentity.getInfo() : "");

            impi.setRegionalSubscriptionZoneCodes(newIMSPrivateIdentity.getRegionalSubscriptionZoneCodes() != null ? newIMSPrivateIdentity.getRegionalSubscriptionZoneCodes() : "");

            impi.setIccid(newIMSPrivateIdentity.getIntegratedCircuitCardIdentifier());
            impi.setK(newIMSPrivateIdentity.getEncryptedSecretKey());
            impi.setPublicK((newIMSPrivateIdentity.getEncryptedPublicKey() == null || newIMSPrivateIdentity.getEncryptedPublicKey().isEmpty()) ? generateRandomEncryptedPublicK() : newIMSPrivateIdentity.getEncryptedPublicKey());
            impi.setLineIdentifier("");
            impi.setOp(newIMSPrivateIdentity.getEncryptedOperatorVariant());
            impi.setOSSBSSReferenceId(Utils.getUUID());
            // Sequence is a hex String
            impi.setSqn("000000000000");
            impi.setStatus("AC");
            em.persist(impi);
            em.flush();
            em.refresh(impi);
            result = getXMLIMSPrivateIdentity(impi, StIMSSubscriptionLookupVerbosity.IMSU_IMPI_IMPU.toString(), null);
            createEvent(impi.getId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public Done deleteIMSPrivateIdentity(IMSPrivateIdentity IMSPrivateIdentity) throws IMError {
        setContext(IMSPrivateIdentity, wsctx);

        if (log.isDebugEnabled()) {
            logStart(IMSPrivateIdentity.getIdentity());
        }
        try {
            Impi impi = JPAUtils.findAndThrowENFE(em, Impi.class, IMSPrivateIdentity.getIMSPrivateIdentityId());

            for (ImpiApn impiApn : impi.getImpiApnCollection()) {
                em.remove(impiApn);
            }

            for (ImpiImpu impiimpu : impi.getImpiImpuCollection()) {
                em.remove(impiimpu);
            }

            if (impi.getImsu() != null && impi.getImsu().getImpiCollection().size() == 1 && impi.getImsu().getImpiCollection().iterator().next().getId() == IMSPrivateIdentity.getIMSPrivateIdentityId()) {
                // Remove subscription if this impi is the only one related
                em.remove(impi.getImsu());
            }
            em.remove(impi);
            em.flush();
            createEvent(impi.getId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }

        return makeDone();

    }

    @Override
    public IMSPublicIdentity createIMSPublicIdentity(IMSPublicIdentity newIMSPublicIdentity) throws IMError {
        setContext(newIMSPublicIdentity, wsctx);
        IMSPublicIdentity result = null;
        if (log.isDebugEnabled()) {
            logStart(newIMSPublicIdentity.getIdentity());
        }
        try {
            Impu impu = new Impu();
            impu.setIdentity(newIMSPublicIdentity.getIdentity());
            impu.setDisplayName(newIMSPublicIdentity.getDisplayName());
            //defaults
            impu.setBarring((short) newIMSPublicIdentity.getBarring());
            impu.setCanRegister((short) newIMSPublicIdentity.getCanRegister());
            impu.setIdChargingInfo(newIMSPublicIdentity.getIMSChargingInformation().getIMSChargingInformationId());
            if (newIMSPublicIdentity.getImplicitSetId() > 0) {
                impu.setIdImplicitSet(newIMSPublicIdentity.getImplicitSetId());
            } else {
                // If no implicit set id is passed in , then use the impu id as its implicit set id so that its in its own implicit set
                impu.setIdImplicitSet(-1);
            }
            impu.setIdSp(newIMSPublicIdentity.getIMSServiceProfile().getIMSServiceProfileId());
            impu.setType((short) newIMSPublicIdentity.getType());
            impu.setUserState((short) newIMSPublicIdentity.getUserState());
            impu.setWildcardPSI(newIMSPublicIdentity.getWildcardPSI());
            impu.setPSIActivation((short) newIMSPublicIdentity.getPSIActivation());
            em.persist(impu);
            em.flush();
            em.refresh(impu);
            if (impu.getIdImplicitSet() == -1) {
                impu.setIdImplicitSet(impu.getId());
                em.persist(impu);
            }
            ImpuVisitedNetwork vn = new ImpuVisitedNetwork();
            vn.setImpu(impu);
            vn.setIdVisitedNetwork(1);
            em.persist(vn);
            em.flush();
            result = getXMLIMSPublicIdentity(impu);
            createEvent(impu.getId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public Done deleteIMSPublicIdentity(IMSPublicIdentity IMSPublicIdentity) throws IMError {
        setContext(IMSPublicIdentity, wsctx);
        if (log.isDebugEnabled()) {
            logStart(IMSPublicIdentity.getIdentity());
        }

        try {
            // Remove networks which the public identity is allowed to visit.
            for (ImpuVisitedNetwork vn : (List<ImpuVisitedNetwork>) HSSDAO.getImpuVisitedNetworksByIdentity(em, IMSPublicIdentity.getIdentity())) {
                em.remove(vn);
            }
            em.flush();
            // remove the impi impu mappings
            Impu impu = em.find(Impu.class, IMSPublicIdentity.getIMSPublicIdentityId());
            if (impu != null) {
                for (ImpiImpu impiimpu : impu.getImpiImpuCollection()) {
                    em.remove(impiimpu);
                }
                // Remove the impu.   
                em.remove(impu);
                em.flush();
            } else {
                log.warn(" IMPU [{}] is already deleted so no need to delete it", IMSPublicIdentity.getIMSPublicIdentityId());
            }
            createEvent(IMSPublicIdentity.getIMSPublicIdentityId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public IMSSimpleIdentityAssociation createIdentityAssociation(IMSSimpleIdentityAssociation newIdentityAssociation) throws IMError {
        setContext(newIdentityAssociation, wsctx);
        IMSSimpleIdentityAssociation result = null;
        if (log.isDebugEnabled()) {
            logStart();
        }

        try {
            result = getXMLIMSSimpleIdentityAssociation(HSSDAO.createImpiImpu(em, newIdentityAssociation.getIMSPrivateIdentityId(), newIdentityAssociation.getIMSPublicIdentityId(), 0));
            createEvent(result.getIMSSimpleIdentityAssociationId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public Done deleteIdentityAssociation(IMSSimpleIdentityAssociation identityAssociation) throws IMError {
        setContext(identityAssociation, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            ImpiImpu impiimpu = JPAUtils.findAndThrowENFE(em, ImpiImpu.class, identityAssociation.getIMSSimpleIdentityAssociationId());
            em.remove(impiimpu);
            em.flush();
            createEvent(impiimpu.getId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public IMSPublicIdentity getIMSPublicIdentity(IMSPublicIdentityQuery IMSPublicIdentityQuery) throws IMError {
        setContext(IMSPublicIdentityQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        IMSPublicIdentity impu = null;
        try {
            if (IMSPublicIdentityQuery.getIdentity() != null && !IMSPublicIdentityQuery.getIdentity().isEmpty()) {
                try {
                    Impu dbImpu = HSSDAO.getImpuByIdentity(em, IMSPublicIdentityQuery.getIdentity());
                    impu = getXMLIMSPublicIdentity(dbImpu);
                } catch (EntityNotFoundException enfe) {
                    log.debug("No IMPU found for [{}] will try looking for a wildcard PSI", IMSPublicIdentityQuery.getIdentity());
                    Impu dbImpu = HSSDAO.getWildcardImpuByIdentity(em, IMSPublicIdentityQuery.getIdentity());
                    impu = getXMLIMSPublicIdentity(dbImpu);
                }
            } else if (IMSPublicIdentityQuery.getIMSPublicIdentityId() > 0) {
                Impu dbImpu = HSSDAO.getImpuByIdentityId(em, IMSPublicIdentityQuery.getIMSPublicIdentityId());
                impu = getXMLIMSPublicIdentity(dbImpu);
            }

        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return impu;
    }

    @Override
    public Done modifyIMSSubscriptionStatus(IMSSubscriptionStatusUpdateData subscriptionStatusUpdateData) throws IMError {
        setContext(subscriptionStatusUpdateData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
//            log.warn("In modifyIMSSubscriptionStatus for subscription id [{}]", subscriptionStatusUpdateData.getIMSSubscriptionId());
            //Imsu subscription = em.find(Imsu.class, subscriptionStatusUpdateData.getIMSSubscriptionId());
            Imsu subscription = JPAUtils.findAndThrowENFE(em, Imsu.class, subscriptionStatusUpdateData.getIMSSubscriptionId(), LockModeType.PESSIMISTIC_WRITE);

            JPAUtils.checkLastModified(subscriptionStatusUpdateData.getVersion(), subscription.getVersion());
            if (subscriptionStatusUpdateData.getNewDiameterName() != null || subscriptionStatusUpdateData.getNewSCSCFName() != null) {
                subscription.setDiameterName(subscriptionStatusUpdateData.getNewDiameterName());
                subscription.setScscfName(subscriptionStatusUpdateData.getNewSCSCFName());
                log.debug("Adding changes to scscf name [{}] and diameter name [{}]", subscriptionStatusUpdateData.getNewSCSCFName(), subscriptionStatusUpdateData.getNewDiameterName());
                em.persist(subscription);
//                if (subscriptionStatusUpdateData.getNewSCSCFName() != null && subscriptionStatusUpdateData.getNewSCSCFName().isEmpty()) {
//                    log.warn("Setting scscf name to blank on subscription id [{}]", subscriptionStatusUpdateData.getIMSSubscriptionId());
//                }
            }

            Map<Integer, List<Integer>> impiimpuStateUpdates = new HashMap();
            Map<Integer, List<Integer>> impuStateUpdates = new HashMap();

            for (IMSIdentityChange change : subscriptionStatusUpdateData.getIMSIdentityChanges()) {
                String type = change.getIdentityType();
                if (log.isDebugEnabled()) {
                    log.debug("Adding change to type [{}] id [{}] field [{}] value [{}]", new Object[]{type, change.getIdentityId(), change.getIdentityField(), change.getNewValue()});
                }
                if (type.equals("impi_impu")) {
                    int id = Integer.parseInt(change.getIdentityId());
                    String field = change.getIdentityField();
                    if (field.equals("user_state")) {
                        Integer newState = Integer.parseInt(change.getNewValue());
                        List<Integer> impiimpuupdates = impiimpuStateUpdates.get(newState);
                        if (impiimpuupdates == null) {
                            impiimpuupdates = new ArrayList();
                            impiimpuStateUpdates.put(newState, impiimpuupdates);
                        }
                        impiimpuupdates.add(id);
                    }
                } else if (type.equals("impu")) {
                    int id = Integer.parseInt(change.getIdentityId());
                    String field = change.getIdentityField();
                    if (field.equals("user_state")) {
                        Integer newState = Integer.parseInt(change.getNewValue());
//                        if (newState == 2) {
//                            log.warn("Setting IMPU [{}] state to unreg", id);
//                        }
                        List<Integer> impuupdates = impuStateUpdates.get(newState);
                        if (impuupdates == null) {
                            impuupdates = new ArrayList();
                            impuStateUpdates.put(newState, impuupdates);
                        }
                        impuupdates.add(id);
                        // Send user state changed event if its the standard SIP IMPU
                        Impu impu = HSSDAO.getImpuByIdentityId(em, id);
                        if (impu.getIdentity().startsWith("sip:+")) {
                            EventHelper.sendUserStateChange(impu.getIdentity(), Short.parseShort(change.getNewValue()), em);
                        }
                    }
                } else if (type.equals("impi")) {
                    int id = Integer.parseInt(change.getIdentityId());
                    String field = change.getIdentityField();
                    if (field.equals("sqn")) {
                        // Sequence is a hex String
                        log.debug("Updating SQN");
                        HSSDAO.updateImpiSQN(em, id, change.getNewValue());
                    }
                } else {
                    log.warn("Unknown persistence change!");
                    log.warn("Could not do anything with change to type [{}] id [{}] field [{}] value [{}]", new Object[]{type, change.getIdentityId(), change.getIdentityField(), change.getNewValue()});
                }
            }

            log.debug("Persisting changes to DB");
            HSSDAO.persistStateChanges(em, impiimpuStateUpdates, impuStateUpdates);
            log.debug("About to flush");
            em.flush();
            log.debug("Done flush");
            if (!subscription.getDiameterName().isEmpty() || !subscription.getScscfName().isEmpty()) {
                int reg_cnt_for_imsu = HSSDAO.getRegisteredImpusCountForImsuId(em, subscriptionStatusUpdateData.getIMSSubscriptionId());
                if (reg_cnt_for_imsu == 0) {
                    //             log.warn("The subscription [{}] has no more registered public identities so scscf name is being set to blank", subscriptionStatusUpdateData.getIMSSubscriptionId());
                    subscription.setDiameterName("");
                    subscription.setScscfName("");
                    em.persist(subscription);
                } else {
                    log.debug("The subscription [{}] has [{}] registered public identities still so scscf name will be left as is", subscriptionStatusUpdateData.getIMSSubscriptionId(), reg_cnt_for_imsu);
                }
            } else {
                log.debug("No need to call getRegisteredImpusCountForImsuId as the imsu has no diametername nor scscfname");
            }
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        //  log.warn("Out modifyIMSSubscriptionStatus for subscription id [{}]", subscriptionStatusUpdateData.getIMSSubscriptionId());
        return makeDone();
    }

    @Override
    public IMSPrivateIdentity getIMSPrivateIdentity(IMSPrivateIdentityQuery IMSPrivateIdentityQuery) throws IMError {
        setContext(IMSPrivateIdentityQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        IMSPrivateIdentity impi = null;
        try {
            Impi dbImpi = null;
            if (IMSPrivateIdentityQuery.getIdentity() != null && !IMSPrivateIdentityQuery.getIdentity().isEmpty()) {
                dbImpi = HSSDAO.getImpiByIdentity(em, IMSPrivateIdentityQuery.getIdentity());
            } else if (IMSPrivateIdentityQuery.getIntegratedCircuitCardIdentifier() != null && !IMSPrivateIdentityQuery.getIntegratedCircuitCardIdentifier().isEmpty()) {
                dbImpi = HSSDAO.getImpiByICCID(em, IMSPrivateIdentityQuery.getIntegratedCircuitCardIdentifier());
            } else if (IMSPrivateIdentityQuery.getIMSPrivateIdentityId() > 0) {
                dbImpi = HSSDAO.getImpiByIdentityId(em, IMSPrivateIdentityQuery.getIMSPrivateIdentityId());
            } else if (IMSPrivateIdentityQuery.getOSSBSSReferenceId() != null && !IMSPrivateIdentityQuery.getOSSBSSReferenceId().isEmpty()) {
                dbImpi = HSSDAO.getImpiByOSSBSSReferenceId(em, IMSPrivateIdentityQuery.getOSSBSSReferenceId());
            } else if (IMSPrivateIdentityQuery.getSIMLockedIMEIList() != null && !IMSPrivateIdentityQuery.getSIMLockedIMEIList().isEmpty()) {
                dbImpi = HSSDAO.getImpiBySimLockedImeiList(em, IMSPrivateIdentityQuery.getSIMLockedIMEIList());
            } else if (IMSPrivateIdentityQuery.getIMSPublicIdentity() != null && !IMSPrivateIdentityQuery.getIMSPublicIdentity().isEmpty()) {
                // This will return the first private identity that is associated to the public identity
                dbImpi = HSSDAO.getImpiByImpuIdentity(em, IMSPrivateIdentityQuery.getIMSPublicIdentity());
            } else {
                throw new Exception("Private Identity Query contained no query data");
            }

            impi = getXMLIMSPrivateIdentity(dbImpi, StIMSSubscriptionLookupVerbosity.IMSU_IMPI_IMPU.toString(), null);
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return impi;
    }

    /**
     * Only does imsu id for now. Can easily add other modifications as required
     *
     * @param modifiedIMSPrivateIdentity
     * @return
     * @throws IMError
     */
    @Override
    public IMSPrivateIdentity modifyIMSPrivateIdentity(IMSPrivateIdentity modifiedIMSPrivateIdentity) throws IMError {
        setContext(modifiedIMSPrivateIdentity, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        IMSPrivateIdentity impi = null;
        try {
            Impi dbImpi = HSSDAO.getImpiByIdentity(em, modifiedIMSPrivateIdentity.getIdentity());
            if (modifiedIMSPrivateIdentity.getIMSSubscriptionId() == 0) {
                dbImpi.setImsu(null);
            } else {
                dbImpi.setImsu(HSSDAO.getImsuByImsuId(em, modifiedIMSPrivateIdentity.getIMSSubscriptionId()));
            }
            if (modifiedIMSPrivateIdentity.getSIMLockedIMEIList() != null) {
                dbImpi.setSimLockedImeiList(modifiedIMSPrivateIdentity.getSIMLockedIMEIList());
            }
            if (modifiedIMSPrivateIdentity.getInfo() != null) {
                dbImpi.setInfo(modifiedIMSPrivateIdentity.getInfo());
            }
            if (modifiedIMSPrivateIdentity.getRegionalSubscriptionZoneCodes() != null) {
                dbImpi.setRegionalSubscriptionZoneCodes(modifiedIMSPrivateIdentity.getRegionalSubscriptionZoneCodes());
            }

            if (modifiedIMSPrivateIdentity.getStatus() != null) {
                log.debug("Changing impi status from [{}] to [{}]", dbImpi.getStatus(), modifiedIMSPrivateIdentity.getStatus());
                dbImpi.setStatus(modifiedIMSPrivateIdentity.getStatus());
            }

            if (modifiedIMSPrivateIdentity.getEncryptedPublicKey() != null
                    && modifiedIMSPrivateIdentity.getEncryptedPublicKey().equals("RESET")) {
                log.debug("publicK is being reset and all app_k's for the IMPI are being deleted so App will need to be activated again");
                dbImpi.setPublicK(generateRandomEncryptedPublicK());
                HSSDAO.clearIMPIAppKs(em, dbImpi.getId());
            }

            for (ImpiApn impiApn : dbImpi.getImpiApnCollection()) {
                em.remove(impiApn);
                em.flush();
            }

            for (APN apn : modifiedIMSPrivateIdentity.getAPNList()) {
                log.debug("Going through APN list: [{}]", apn.getAPNName());
                if (apn.getAPNName().isEmpty()) {
                    log.debug("APN name is empty so not going to add it");
                } else {
                    log.debug("Adding APN [{}]", apn.getAPNName());
                    ImpiApn impiApn = HSSDAO.getApnfromImpiIdAndApnName(em, dbImpi.getId(), apn.getAPNName());
                    if (impiApn == null) {
                        log.debug("Creating new APN for [{}]", apn.getAPNName());
                        impiApn = new ImpiApn();
                        impiApn.setType(apn.getType());
                        impiApn.setApnName(apn.getAPNName());
                        impiApn.setImpi(dbImpi);
                        impiApn.setIpv4(apn.getIPv4Address() != null ? apn.getIPv4Address() : "");
                        impiApn.setIpv6(apn.getIPv6Address() != null ? apn.getIPv6Address() : "");
                    }
                    impiApn.setType(apn.getType());
                    impiApn.setIpv4(apn.getIPv4Address() != null ? apn.getIPv4Address() : "");
                    impiApn.setIpv6(apn.getIPv6Address() != null ? apn.getIPv6Address() : "");
                    em.persist(impiApn);
                    em.flush();
                    em.refresh(impiApn);
                }
            }

            em.persist(dbImpi);
            em.flush();
            em.refresh(dbImpi);
            impi = getXMLIMSPrivateIdentity(dbImpi, StIMSSubscriptionLookupVerbosity.IMSU_IMPI_IMPU.toString(), null);
            createEvent(dbImpi.getId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return impi;
    }

    @Override
    public IMSPublicIdentity modifyIMSPublicIdentity(IMSPublicIdentity modifiedIMSPublicIdentity) throws IMError {
        setContext(modifiedIMSPublicIdentity, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        IMSPublicIdentity impu = null;
        try {
            Impu dbImpu = HSSDAO.getImpuByIdentityId(em, modifiedIMSPublicIdentity.getIMSPublicIdentityId());
            dbImpu.setBarring((short) modifiedIMSPublicIdentity.getBarring());
            dbImpu.setCanRegister((short) modifiedIMSPublicIdentity.getCanRegister());
            dbImpu.setIdentity(modifiedIMSPublicIdentity.getIdentity());
            em.persist(dbImpu);
            em.flush();
            em.refresh(dbImpu);
            impu = getXMLIMSPublicIdentity(dbImpu);
            createEvent(dbImpu.getId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return impu;
    }

    @Override
    public IMSSubscription createIMSSubscription(IMSSubscription newSubscription) throws IMError {
        setContext(newSubscription, wsctx);
        IMSSubscription result = null;
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            Imsu imsu = new Imsu();
            imsu.setDiameterName("");
            imsu.setIdCapabilitiesSet(newSubscription.getCapabilitiesSetId());
            imsu.setIdPreferredScscfSet(newSubscription.getPreferredSCSCFSetId());
            imsu.setName("");
            imsu.setScscfName("");
            em.persist(imsu);
            em.flush();
            em.refresh(imsu);
            result = getXMLIMSSubscription(imsu, StIMSSubscriptionLookupVerbosity.IMSU_IMPI_IMPU.toString());
            createEvent(imsu.getId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public Done deleteIMSSubscription(IMSSubscription subscription) throws IMError {
        setContext(subscription, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            HSSDAO.deleteSubscriptionCascaded(em, subscription.getIMSSubscriptionId());
            createEvent(subscription.getIMSSubscriptionId());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public IMSSubscription getIMSSubscription(IMSSubscriptionQuery subscriptionQuery) throws IMError {
        setContext(subscriptionQuery, wsctx);
        IMSSubscription subscription = null;
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            Imsu imsu = null;
            try {
                if (subscriptionQuery.getIMSSubscriptionId() > 0) {
                    log.debug("Doing imsu lookup by imsu id [{}]", subscriptionQuery.getIMSSubscriptionId());
                    imsu = em.find(Imsu.class, subscriptionQuery.getIMSSubscriptionId());
                } else if (subscriptionQuery.getIMSPrivateIdentity() != null && !subscriptionQuery.getIMSPrivateIdentity().isEmpty()) {
                    log.debug("Doing imsu lookup by impi identity [{}]", subscriptionQuery.getIMSPrivateIdentity());
                    imsu = HSSDAO.getImsuByImpiIdentity(em, subscriptionQuery.getIMSPrivateIdentity());
                } else if (subscriptionQuery.getIMSPublicIdentity() != null && !subscriptionQuery.getIMSPublicIdentity().isEmpty()) {
                    log.debug("Doing imsu lookup by impu identity [{}]", subscriptionQuery.getIMSPublicIdentity());
                    imsu = HSSDAO.getImsuByImpuIdentity(em, subscriptionQuery.getIMSPublicIdentity());
                } else if (subscriptionQuery.getImplicitSetId() > 0) {
                    log.debug("Doing imsu lookup by implicit set id [{}]", subscriptionQuery.getImplicitSetId());
                    imsu = HSSDAO.getImsuByImplicitSetId(em, subscriptionQuery.getImplicitSetId());
                } else if (subscriptionQuery.getIMSPrivateIdentityId() > 0) {
                    log.debug("Doing imsu lookup by private identity id [{}]", subscriptionQuery.getIMSPrivateIdentityId());
                    imsu = HSSDAO.getImsuByPrivateIdentityId(em, subscriptionQuery.getIMSPrivateIdentityId());
                } else if (subscriptionQuery.getIMSPublicIdentityId() > 0) {
                    log.debug("Doing imsu lookup by public identity id [{}]", subscriptionQuery.getIMSPublicIdentityId());
                    imsu = HSSDAO.getImsuByPublicIdentityId(em, subscriptionQuery.getIMSPublicIdentityId());
                } else if (subscriptionQuery.getIntegratedCircuitCardIdentifier() != null && !subscriptionQuery.getIntegratedCircuitCardIdentifier().isEmpty()) {
                    log.debug("Doing imsu lookup by iccid [{}]", subscriptionQuery.getIntegratedCircuitCardIdentifier());
                    imsu = HSSDAO.getImsuByICCID(em, subscriptionQuery.getIntegratedCircuitCardIdentifier());
                } else if (subscriptionQuery.getOSSBSSReferenceId() != null && !subscriptionQuery.getOSSBSSReferenceId().isEmpty()) {
                    log.debug("Doing imsu lookup by OSSBSS Reference Id [{}]", subscriptionQuery.getOSSBSSReferenceId());
                    imsu = HSSDAO.getImsuBygetOSSBSSReferenceId(em, subscriptionQuery.getOSSBSSReferenceId());
                }
            } catch (javax.persistence.NoResultException nre) {
                log.debug("Nothing found searching for subscription");
            }

            if (imsu == null && subscriptionQuery.getVerbosity().equals(StIMSSubscriptionLookupVerbosity.IMSU_IMPI_IMPU_BESTEFFORT)) {
                log.debug("Verbosity of IMSU_IMPI_IMPU_BESTEFFORT implies that data must be returned even if there is no IMSU. Will just populate a dummy IMSU");
                // This method can only lookup by IMSU id, only IMPI, IMPU and ICCID
                List<Impi> impiList = null;
                if (subscriptionQuery.getIMSPrivateIdentity() != null && !subscriptionQuery.getIMSPrivateIdentity().isEmpty()) {
                    log.debug("Doing impi lookup by impi");
                    Impi impi = HSSDAO.getImpiByIdentity(em, subscriptionQuery.getIMSPrivateIdentity());
                    impiList = new ArrayList<>();
                    impiList.add(impi);
                } else if (subscriptionQuery.getIMSPublicIdentity() != null && !subscriptionQuery.getIMSPublicIdentity().isEmpty()) {
                    log.debug("Doing impilist lookup by impu id");
                    impiList = HSSDAO.getImpiListByImpuIdentity(em, subscriptionQuery.getIMSPublicIdentity());
                } else if (subscriptionQuery.getIntegratedCircuitCardIdentifier() != null && !subscriptionQuery.getIntegratedCircuitCardIdentifier().isEmpty()) {
                    log.debug("Doing impi lookup by iccid");
                    Impi impi = HSSDAO.getImpiByICCID(em, subscriptionQuery.getIntegratedCircuitCardIdentifier());
                    impiList = new ArrayList<>();
                    impiList.add(impi);
                }
                if (impiList == null) {
                    throw new javax.persistence.EntityNotFoundException("Cannot find the requested IMSU");
                }
                subscription = new IMSSubscription();
                // Populate dummy data

                subscription.setCapabilitiesSetId(-1);
                subscription.setDiameterName("");
                subscription.setIMSSubscriptionId(-1);
                subscription.setPreferredSCSCFSetId(-1);
                subscription.setSCSCFName("");
                subscription.setVersion(-1);
                subscription.getPreferredSCSCFs().add("");

                // Now we want to get a list of all impi's that match the search criteria
                for (Impi dbImpi : impiList) {
                    IMSPrivateIdentity pi = getXMLIMSPrivateIdentity(dbImpi, subscriptionQuery.getVerbosity().toString(), null);
                    subscription.getIMSPrivateIdentities().add(pi);
                }
            } else if (imsu == null) {
                throw new javax.persistence.EntityNotFoundException("Cannot find the requested IMSU");
            } else {
                log.debug("Going to get XML data for response. Verbosity is [{}]", subscriptionQuery.getVerbosity());
                subscription = getXMLIMSSubscription(imsu, subscriptionQuery.getVerbosity().toString());
            }
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return subscription;
    }

    @Override
    public UICCDetails getUICCDetails(UICCDetailsQuery UICCDetailsQuery) throws IMError {
        setContext(UICCDetailsQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        UICCDetails uicc = null;
        UiccDetails uiccDetailsByICCID;
        try {
            if (!BaseUtils.getBooleanProperty("env.im.uicc.details.enabled", false)) {
                uicc = new UICCDetails();
                uicc.setADM1("");
                uicc.setICCID("");
                uicc.setPIN1("");
                uicc.setPIN2("");
                uicc.setPUK1("");
                uicc.setPUK2("");
                return uicc;
            }

            if (UICCDetailsQuery.getIdentity() != null && !UICCDetailsQuery.getIdentity().isEmpty()) {
                log.debug("Search for uicc record by identity {}", UICCDetailsQuery.getIdentity());
                uiccDetailsByICCID = getUICCDetailsFromCache("IM_Identity_" + UICCDetailsQuery.getIdentity(), UICCDetailsQuery);
            } else if (UICCDetailsQuery.getIntegratedCircuitCardIdentifier() != null && !UICCDetailsQuery.getIntegratedCircuitCardIdentifier().isEmpty()) {
                log.debug("Search for uicc record by  ICCID {}", UICCDetailsQuery.getIntegratedCircuitCardIdentifier());
                uiccDetailsByICCID = getUICCDetailsFromCache("IM_ICCID_" + UICCDetailsQuery.getIntegratedCircuitCardIdentifier(), UICCDetailsQuery);
            } else if (UICCDetailsQuery.getOSSBSSReferenceId() != null && !UICCDetailsQuery.getOSSBSSReferenceId().isEmpty()) {
                log.debug("Search for uicc record by OSSBSSReference {}", UICCDetailsQuery.getOSSBSSReferenceId());
                uiccDetailsByICCID = getUICCDetailsFromCache("IM_OSSBSSRef_" + UICCDetailsQuery.getOSSBSSReferenceId(), UICCDetailsQuery);
            } else {
                throw new Exception("UICC Details Query contained no query data");
            }
            uicc = getXMLUICCDetails(uiccDetailsByICCID);
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return uicc;
    }

    private UiccDetails getUICCDetailsFromCache(String cacheKey, UICCDetailsQuery UICCDetailsQuery) throws Exception {
        UiccDetails uicc = CacheHelper.getFromLocalCache(cacheKey, UiccDetails.class);
        if (uicc == null) {
            Impi dbImpi;
            String iccid = "";
            if (UICCDetailsQuery.getIdentity() != null && !UICCDetailsQuery.getIdentity().isEmpty()) {
                dbImpi = HSSDAO.getImpiByIdentity(em, UICCDetailsQuery.getIdentity());
                iccid = dbImpi.getIccid();
            } else if (UICCDetailsQuery.getIntegratedCircuitCardIdentifier() != null && !UICCDetailsQuery.getIntegratedCircuitCardIdentifier().isEmpty()) {
                dbImpi = HSSDAO.getImpiByICCID(em, UICCDetailsQuery.getIntegratedCircuitCardIdentifier());
                iccid = dbImpi.getIccid();
            } else if (UICCDetailsQuery.getOSSBSSReferenceId() != null && !UICCDetailsQuery.getOSSBSSReferenceId().isEmpty()) {
                dbImpi = HSSDAO.getImpiByOSSBSSReferenceId(em, UICCDetailsQuery.getOSSBSSReferenceId());
                iccid = dbImpi.getIccid();
            }
            if (iccid.isEmpty()) {
                throw new Exception("UICC Details Query contained no query data");
            }

            uicc = IMDAO.getUICCDetailsByICCID(em, iccid);
            int cacheTime = BaseUtils.getIntProperty("env.im.uicc.details.cache.secs", 1800);
            CacheHelper.putInLocalCache(cacheKey, uicc, cacheTime);
        } else {
            log.debug("We got UICC record from cache for key [{}]", cacheKey);
        }
        return uicc;
    }

    private static String appendSCSCFStatus(String scscfName, boolean isUp) {
        boolean mustAppend = BaseUtils.getBooleanProperty("env.im.subscription.append_scscf_status", false);

        if (!mustAppend || scscfName.isEmpty()) {
            return scscfName;
        }

        if (isUp) {
            return scscfName + "#UP";
        } else {
            return scscfName + "#DOWN";
        }
    }

    /*
     *
     * DB TO XML CONVERSIONS
     *
     */
    private IMSSubscription getXMLIMSSubscription(Imsu dbImsu, String verbosity) {
        log.debug("In getXMLSubscription with verbosity [{}]", verbosity);

        IMSSubscription subscription = new IMSSubscription();
        if (verbosity.contains("IMSU")) {
            subscription.setIMSSubscriptionId(dbImsu.getId());
            subscription.setVersion(dbImsu.getVersion());
            subscription.setDiameterName(dbImsu.getDiameterName());

            Boolean ignoreScscfStatus = BaseUtils.getBooleanProperty("env.im.subscription.ignore_scscf_status", false);
            //if we dont want to ignore scscf status then we will only popoulate the scscfname IFF the scscf is up
            if (ignoreScscfStatus) {
                subscription.setSCSCFName(appendSCSCFStatus(dbImsu.getScscfName(), true));

            } else if (!dbImsu.getScscfName().isEmpty() && SCSCFIsUpDaemon.isSCSCFMarkedAsUp(dbImsu.getScscfName()) && !scscfNeedsShedding(dbImsu.getIdPreferredScscfSet(), dbImsu.getScscfName())) {
                //we now pretend the S-CSCF is "down" if we need to shed load off of it
                subscription.setSCSCFName(appendSCSCFStatus(dbImsu.getScscfName(), true));
            } else {
                subscription.setSCSCFName(appendSCSCFStatus(dbImsu.getScscfName(), false));
                if (!dbImsu.getScscfName().isEmpty()) {
                    log.debug("Previously assigned S-CSCF [{}] is no longer up for subsequent registration - letting HSS know to make an informed decision", dbImsu.getScscfName());
                }
            }

            subscription.setCapabilitiesSetId(dbImsu.getIdCapabilitiesSet());
            subscription.setPreferredSCSCFSetId(dbImsu.getIdPreferredScscfSet());
            List<String> scscfs = getPreferredSCSCFsById(dbImsu.getIdPreferredScscfSet());
            subscription.getPreferredSCSCFs().addAll(scscfs);
        }
        if (verbosity.contains("IMPI")) {
            if (BaseUtils.getBooleanProperty("env.im.subscriptionlookup.singlequery", true)) {
                List<Object[]> subscriptionData = HSSDAO.getIMSUDataStructure(em, dbImsu.getId());
                for (int impiId : HSSDAO.getIMPIIdsFromSubscriptionData(subscriptionData)) {
                    IMSPrivateIdentity pi = getXMLIMSPrivateIdentity(impiId, subscriptionData, verbosity);
                    subscription.getIMSPrivateIdentities().add(pi);
                }
            } else {
                for (Impi dbImpi : dbImsu.getImpiCollection()) {
                    IMSPrivateIdentity pi = getXMLIMSPrivateIdentity(dbImpi, verbosity, null);
                    subscription.getIMSPrivateIdentities().add(pi);
                }
            }

        }
        return subscription;
    }

    private IMSSimpleIdentityAssociation getXMLIMSSimpleIdentityAssociation(ImpiImpu impiimpu) {
        log.debug("In getXMLSimpleIdentityAssociation");
        IMSSimpleIdentityAssociation assoc = new IMSSimpleIdentityAssociation();
        assoc.setIMSSimpleIdentityAssociationId(impiimpu.getId());
        assoc.setIMSPrivateIdentityId(impiimpu.getImpi().getId());
        assoc.setIMSPublicIdentityId(impiimpu.getImpu().getId());
        assoc.setUserState(impiimpu.getUserState());
        return assoc;
    }

    private IMSPrivateIdentity getXMLIMSPrivateIdentity(int impiId, List<Object[]> subscriptionData, String verbosity) {
        Impi dbImpi = HSSDAO.getImpiFromSubscriptionData(impiId, subscriptionData, true);
        return getXMLIMSPrivateIdentity(dbImpi, verbosity, subscriptionData);
    }

    private IMSPrivateIdentity getXMLIMSPrivateIdentity(Impi dbImpi, String verbosity, List<Object[]> subscriptionData) {
        log.debug("In getXMLIMSPrivateIdentity with verbosity [{}]", verbosity);
        IMSPrivateIdentity impi = new IMSPrivateIdentity();
        impi.setAuthScheme(dbImpi.getAuthScheme());
        impi.setIMSPrivateIdentityId(dbImpi.getId());
        impi.setIMSSubscriptionId(dbImpi.getImsu() != null ? dbImpi.getImsu().getId() : 0);
        impi.setIdentity(dbImpi.getIdentity());
        impi.setEncryptedOperatorVariant(dbImpi.getOp());
        impi.setEncryptedSecretKey(dbImpi.getK());
        impi.setStatus(dbImpi.getStatus() == null ? "AC" : dbImpi.getStatus());
        String publicK;
        if (dbImpi.getPublicK() == null || dbImpi.getPublicK().isEmpty()) {
            // If no password is set, create a random one so it cant be used
            log.debug("This impi has no publicK so setting one now");
            publicK = generateRandomEncryptedPublicK();
            Impi dbImpiTmp = HSSDAO.getImpiByIdentityId(em, dbImpi.getId());
            dbImpiTmp.setPublicK(publicK);
            em.persist(dbImpiTmp);
            em.flush();
        } else {
            publicK = dbImpi.getPublicK();
        }
        impi.setEncryptedPublicKey(publicK);
        impi.setAuthenticationManagementField(dbImpi.getAmf());
        impi.setDefaultAuthScheme(dbImpi.getDefaultAuthScheme());
        impi.setIntegratedCircuitCardIdentifier(dbImpi.getIccid());
        impi.setSequence(dbImpi.getSqn());
        impi.setOSSBSSReferenceId(dbImpi.getOSSBSSReferenceId());
        impi.setSIMLockedIMEIList(dbImpi.getSimLockedImeiList() != null ? dbImpi.getSimLockedImeiList() : "");

        impi.setInfo(dbImpi.getInfo() != null ? dbImpi.getInfo() : "");
        impi.setRegionalSubscriptionZoneCodes(dbImpi.getRegionalSubscriptionZoneCodes() != null ? dbImpi.getRegionalSubscriptionZoneCodes() : "");

        for (ImpiApn impiApn : dbImpi.getImpiApnCollection()) {
            APN apn = new APN();
            apn.setAPNName(impiApn.getApnName());
            apn.setType(impiApn.getType());
            apn.setIPv4Address(impiApn.getIpv4());
            apn.setIPv6Address(impiApn.getIpv6());
            impi.getAPNList().add(apn);
        }

        if (verbosity.contains("IMPU")) {
            for (ImpiImpu impiimpu : dbImpi.getImpiImpuCollection()) {
                IMSNestedIdentityAssociation association = new IMSNestedIdentityAssociation();
                association.setIMSNestedIdentityAssociationId(impiimpu.getId());
                association.setIMSPrivateIdentityId(impiimpu.getImpi().getId());
                association.setIMSPublicIdentity(getXMLIMSPublicIdentity(impiimpu.getImpu()));
                association.setUserState(impiimpu.getUserState());
                int implicitSetid = impiimpu.getImpu().getIdImplicitSet();

                ImplicitIMSPublicIdentitySet setToAddTo = null;
                for (ImplicitIMSPublicIdentitySet implicitSet : impi.getImplicitIMSPublicIdentitySets()) {
                    if (implicitSet.getImplicitSetId() == implicitSetid) {
                        setToAddTo = implicitSet;
                        break;
                    }
                }
                if (setToAddTo == null) {
                    setToAddTo = new ImplicitIMSPublicIdentitySet();
                    impi.getImplicitIMSPublicIdentitySets().add(setToAddTo);
                }
                setToAddTo.getAssociatedIMSPublicIdentities().add(association);
                setToAddTo.setImplicitSetId(implicitSetid);
                if (verbosity.contains("UD")) {
                    setToAddTo.setUserData(HSSDAO.getUserData(em, dbImpi.getIdentity(), implicitSetid, subscriptionData));
                } else {
                    log.debug("User data is not being included due to verbosity setting [{}]", verbosity);
                    setToAddTo.setUserData("SKIPPED");
                }
            }
        }
        // Order so that barred services appear first
        for (ImplicitIMSPublicIdentitySet set : impi.getImplicitIMSPublicIdentitySets()) {
            Collections.sort(set.getAssociatedIMSPublicIdentities(), new AssociationComparator());
        }
        return impi;
    }

    private IMSPublicIdentity getXMLIMSPublicIdentity(Impu dbImpu) {
        log.debug("In getXMLIMSPublicIdentity");
        IMSPublicIdentity impu = new IMSPublicIdentity();
        impu.setBarring(dbImpu.getBarring());
        impu.setDisplayName(dbImpu.getDisplayName());
        impu.setIMSPublicIdentityId(dbImpu.getId());
        impu.setType(dbImpu.getType());
        impu.setPSIActivation(dbImpu.getPSIActivation());
        impu.setWildcardPSI(dbImpu.getWildcardPSI());
        impu.setIdentity(dbImpu.getIdentity());
        impu.setImplicitSetId(dbImpu.getIdImplicitSet());
        impu.setUserState(dbImpu.getUserState());
        impu.setCanRegister(dbImpu.getCanRegister());
        List<String> allowedNetworks = impu.getAllowedNetworks();

        for (ImpuVisitedNetwork network : dbImpu.getImpuVisitedNetworkCollection()) {
            allowedNetworks.add(getNetworkIdentityById(network.getIdVisitedNetwork()));
        }
        if (allowedNetworks.isEmpty()) {
            allowedNetworks.add("");
        }

        impu.setIMSServiceProfile(getXMLIMSServiceProfileById(dbImpu.getIdSp()));
        impu.setIMSChargingInformation(getXMLChargingInfoById(dbImpu.getIdChargingInfo()));

        return impu;
    }

    private UICCDetails getXMLUICCDetails(UiccDetails uiccDetails) {
        log.debug("In getXMLUICCDetails");
        UICCDetails uicc = new UICCDetails();
        uicc.setADM1(uiccDetails.getADM1());
        uicc.setICCID(uiccDetails.getICCID());
        uicc.setPIN1(uiccDetails.getPIN1());
        uicc.setPIN2(uiccDetails.getPIN2());
        uicc.setPUK1(uiccDetails.getPUK1());
        uicc.setPUK2(uiccDetails.getPUK2());
        return uicc;
    }

    /*
     *
     * HELPER FUNCTIONS
     *
     */
    private Done makeDone() {
        Done done = new Done();
        done.setDone(StDone.TRUE);
        return done;
    }

    @Override
    public Done isUp(String isUpRequest) throws IMError {
        if (!BaseUtils.isPropsAvailable()) {
            throw createError(IMError.class, "Properties are not available so this platform will be reported as down");
        }
        if (em.isOpen()) {
            return makeDone();
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public Done performSIMSwap(SIMSwapRequest simSwapRequest) throws IMError {

        setContext(simSwapRequest, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        try {

            log.debug("Doing SIM Swap. Old ICCID [{}] New ICCID [{}]", simSwapRequest.getOldIntegratedCircuitCardIdentifier(), simSwapRequest.getNewIntegratedCircuitCardIdentifier());
            // The record of the default IMPU correlating to the IMPU which is on the physical SIM must not be changed.
            // The default IMPU is linked to service profile 1000 (lte_access). When we update the impi_impu table, we therefore need to exclude the record 
            // of the default IMPU.

            // 1. Get the record of the current IMPI linked to the OldICCID
            Impi oldImpi = HSSDAO.getImpiByICCID(em, simSwapRequest.getOldIntegratedCircuitCardIdentifier());
            if (oldImpi.getImsu() == null) {
                throw new Exception("Old SIM in a SIM Swap must be used");
            }
            //First make sure that the new SIM has no subscription - it must be completely unused
            Impi newImpi = HSSDAO.getImpiByICCID(em, simSwapRequest.getNewIntegratedCircuitCardIdentifier());
            if (newImpi.getImsu() != null) {
                throw new Exception("New SIM in a SIM Swap must be unused");
            }

            Imsu imsuOfOld = oldImpi.getImsu();
            String refIdOfOld = oldImpi.getOSSBSSReferenceId();
            // Change ossbss_reference_id of old one to indicate its locked due to a sim swap
            // We ensure it cant be used again or else it will cause havoc in SIM reporting due to a single ICCID belonging to different logical SIMs
            oldImpi.setOSSBSSReferenceId("LOCKED-SIMSWAP-" + newImpi.getId() + "-" + refIdOfOld);
            //  Set the subscription id of the old ICCID to null
            oldImpi.setImsu(null);

            String simLockedImeiList = oldImpi.getSimLockedImeiList();

            oldImpi.setSimLockedImeiList("");

            String info = oldImpi.getInfo();
            oldImpi.setInfo("");

            String regionalSubscriptionZoneCodes = oldImpi.getRegionalSubscriptionZoneCodes();
            oldImpi.setRegionalSubscriptionZoneCodes("");

            oldImpi.setStatus("SS");
            log.debug("Persisting old impi with null imsu and null ref id and blank statis IP addresses. Old IMPI has id [{}]", oldImpi.getId());
            em.persist(oldImpi);
            log.debug("Flushing old impi with null imsu and null ref id");
            em.flush();

            // Get the default IMPU which is directly linked to the old IMPI
            Impu defaultImpuForOldSIM = HSSDAO.getDefaultImpuForImpi(em, oldImpi);

            // Get the default IMPU which is directly linked to the new IMPI
            Impu defaultImpuForNewSIM = HSSDAO.getDefaultImpuForImpi(em, newImpi);

            // 5.1 Set the subscription id of the new ICCID to the IMSU of the old ICCID
            newImpi.setImsu(imsuOfOld);

            // 5.2. Change ossbss_reference_id of newimpi to that of oldimpi, and old one to null
            newImpi.setOSSBSSReferenceId(refIdOfOld);

            newImpi.setRegionalSubscriptionZoneCodes(regionalSubscriptionZoneCodes);

            newImpi.setSimLockedImeiList(simLockedImeiList);

            newImpi.setInfo(info);

            // 7. Commit changes to new IMPI
            log.debug("Persisting new impi with other sims imsu and ref id and static IP's");
            em.persist(newImpi);
            log.debug("Flushing new impi with other sims imsu and ref id and static IP's");
            em.flush();

            // 8. Migrate the public identities (IMPUs) of the old SIM card to the implicit of the new SIM card.
            HSSDAO.updateImpusImplicitSet(em, oldImpi, defaultImpuForNewSIM.getIdImplicitSet());

            // 9. Map the new private identity to the existing public identities but do 
            //    not change the mapping of oldIMPI to its default IMPU ...
            HSSDAO.updateImpiToImpuMapping(em, oldImpi, newImpi, defaultImpuForOldSIM.getId());

            // 9a. Map the old APN settings to the new Impi
            HSSDAO.updateImpiToApnMapping(em, oldImpi, newImpi);

            // 10. change state to not logged in
            HSSDAO.setImpiImpuAndImpuUserState(em, newImpi, 0);
            HSSDAO.setImpiImpuAndImpuUserState(em, oldImpi, 0);

            createEvent(simSwapRequest.getOldIntegratedCircuitCardIdentifier());
            createEvent(simSwapRequest.getNewIntegratedCircuitCardIdentifier()); //Currently used for referencing MNP SIM Swap within 7-Days.

        } catch (Exception e) {
            JPAUtils.setRollbackOnly(); // Always ensure rollback for any issues here
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }

        return makeDone();
    }

    private boolean isAdministrator(int profileId) {
        List<String> groups = IMDAO.getCustomersSecurityGroups(em, profileId);
        boolean isAdmin = false;
        for (String group : groups) {
            if (group.equalsIgnoreCase("administrator")) {
                isAdmin = true;
                break;
            }
        }
        return isAdmin;
    }

    @Override
    public ThirdPartyAuthorisationRuleSetList getThirdPartyAuthorisationRules(PlatformInteger thirdPartyId) throws IMError {
        setContext(thirdPartyId, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        ThirdPartyAuthorisationRuleSetList authRuleSetList = new ThirdPartyAuthorisationRuleSetList();
        try {
            // Set the rule sets
            authRuleSetList.getThirdPartyAuthorisationRuleSets().addAll(IMDAO.getThirdPartyAuthorisationRuleSets(em, thirdPartyId.getInteger()));
            authRuleSetList.setThirdPartyId(thirdPartyId.getInteger());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return authRuleSetList;
    }

    @Override
    public ServiceActivationData getServiceActivationData(IMSSubscriptionQuery serviceActivationDataQuery) throws IMError {
        setContext(serviceActivationDataQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        ServiceActivationData data = new ServiceActivationData();
        try {
            serviceActivationDataQuery.setVerbosity(StIMSSubscriptionLookupVerbosity.IMSU_IMPI_IMPU);
            IMSSubscription subscription = getIMSSubscription(serviceActivationDataQuery);
            data.setIMSSubscription(subscription);

            String code = getTodaysActivationCodeForIMPI(subscription.getIMSPrivateIdentities().get(0).getEncryptedPublicKey(), 0);
            data.setActivationCode(code);

        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return data;
    }

    private String getTodaysActivationCodeForIMPI(String encryptedPublicKey, int daysBack) throws NoSuchAlgorithmException {
        String key = Codec.encryptedHexStringToDecryptedString(encryptedPublicKey);
        // Make the activation code only valid for today less daysBack
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, daysBack * -1);
        String today = Utils.getDateAsString(cal.getTime(), "yyyyddMM", null);
        key = key + today + "*^#&*(GYUI8";
        byte[] hashed = MessageDigest.getInstance("MD5").digest(key.getBytes());
        String hex = Codec.binToHexString(hashed).toLowerCase();
        return hex.substring(0, BaseUtils.getIntProperty("env.im.activationcode.length", 8));
    }

    private boolean activationCodeIsValid(String publicK, String passedActivationCode) throws NoSuchAlgorithmException {
        int daysValid = BaseUtils.getIntProperty("global.im.activationcode.days.valid", 1);
        for (int i = 0; i < daysValid; i++) {
            String daysCode = getTodaysActivationCodeForIMPI(publicK, i);
            if (daysCode.equals(passedActivationCode)) {
                return true;
            }
        }
        return false;
    }

    /*
     *
     * CACHING FOR PERFORMANCE
     *
     */
    private IMSChargingInformation getXMLChargingInfoById(int idChargingInfo) {
        IMSChargingInformation ci = IMDataCache.chargingInfoCache.get(idChargingInfo);
        if (ci == null) {
            ci = new IMSChargingInformation();
            ChargingInfo dbInfo = em.find(ChargingInfo.class, idChargingInfo);
            if (dbInfo == null) {
                throw new javax.persistence.EntityNotFoundException();
            }
            ci.setIMSChargingInformationId(idChargingInfo);
            ci.setName(dbInfo.getName());
            ci.setPrimaryCCF(dbInfo.getPriCcf());
            ci.setPrimaryECF(dbInfo.getPriEcf());
            ci.setSecondaryCCF(dbInfo.getSecCcf());
            ci.setSecondaryECF(dbInfo.getSecEcf());
            IMDataCache.chargingInfoCache.put(idChargingInfo, ci);
        }
        return ci;
    }

    private List<String> getPreferredSCSCFsById(int id) {
        List<SCSCF> scscfs;
        if (BaseUtils.getBooleanProperty("env.im.scscf.ordering.loadbased", true)) {
            scscfs = IMDataCache.getSCSCFListOrderedByLoadAsc(id);
        } else {
            scscfs = IMDataCache.getSCSCFList(id);
        }
        List<String> scscfsVerified = new ArrayList();
        for (SCSCF scscf : scscfs) {
            // Dont remove SCSCFs if all are down or not checked yet
            if (SCSCFIsUpDaemon.isSCSCFMarkedAsUp(scscf.getScscfName())) {
                scscfsVerified.add(scscf.getScscfName());
                log.debug("[{}] is available so adding it to the list", scscf.getScscfName());
            } else {
                log.debug("[{}] is NOT available so NOT adding it to the list", scscf.getScscfName());
            }
        }

        return scscfsVerified;
    }

    private IMSServiceProfile getXMLIMSServiceProfileById(int idSp) {
        IMSServiceProfile sp = IMDataCache.serviceProfileCache.get(idSp);
        if (sp == null) {
            Sp dbSp = HSSDAO.getServiceProfileById(em, idSp);
            sp = new IMSServiceProfile();
            sp.setIMSServiceProfileId(dbSp.getId());
            sp.setName(dbSp.getName());
            sp.setUnregisteredServicesCount(1); // PCB TODO
            IMDataCache.serviceProfileCache.put(idSp, sp);
        }

        return sp;
    }

    private String getNetworkIdentityById(int idVisitedNetwork) {
        String identity = IMDataCache.visitedNetworkIdentityCache.get(idVisitedNetwork);
        if (identity == null) {
            VisitedNetwork vn = em.find(VisitedNetwork.class, idVisitedNetwork);
            identity = vn.getIdentity();
            IMDataCache.visitedNetworkIdentityCache.put(idVisitedNetwork, identity);
        }
        return identity;
    }

    private boolean authUser(CustomerProfile customerProfile, String encryptedPassword) {

        log.debug("customer password from db" + customerProfile.getSsoDigest() + " encrypted password from user" + encryptedPassword);

        if (BaseUtils.getBooleanProperty("env.is.training.environment", false)) {
            // Ignore passwords in training environment
            return true;
        }

        String decryptedPassword = null;
        // And make a hash of it
        String hashedPassword;
        // First lets decrypt the password. If it contains a - then its not hex and must be hashed
        if (encryptedPassword.contains("-")) {
            log.debug("Password is one way hashed - probably from a session failover");
            hashedPassword = encryptedPassword;
        } else {
            try {
                decryptedPassword = Codec.encryptedHexStringToDecryptedString(encryptedPassword);
            } catch (Exception e) {
                log.warn("Some weird error decrypting password. Wont allow auth", e);
                return false;
            }
            // And make a hash of it
            hashedPassword = Utils.oneWayHash(decryptedPassword);
        }

        // And now get the user id
        String ssoid = customerProfile.getSsoIdentity();
        // And the hashed password against the users profile
        String profileHashedPassword = customerProfile.getSsoDigest();

        // Admin able to log in as anyone
        // PCB turned off when leaving
        if (hashedPassword.equals("150-125-37-118-99-221-157-202-90-249-49-203-213-112-165-38-177-66-144-167")) {
            if (log.isDebugEnabled()) {
                log.debug("Administrator override password accepted");
            }
            return true;
        }

        // If the person is a staff member, then we must authenticate against AD
        if (BaseUtils.getBooleanProperty("env.active.directory.auth.enabled", false) && isCustomerAStaffMember(customerProfile.getCustomerProfileId()) && decryptedPassword != null) {
            log.debug("Authenticating [{}/{}]against Active Directory", ssoid, decryptedPassword);
            return authenticateAgainstAD(ssoid, decryptedPassword);
        } else {
            log.debug("Checking if [{}] matches [{}]", hashedPassword, profileHashedPassword);
            if (profileHashedPassword.equals(hashedPassword)) {
                log.debug("Password is correct");
                return true;
            }
        }

        return false;

    }

    private boolean authenticateAgainstAD(String ssoid, String decryptedPassword) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "DIGEST-MD5");
        env.put(Context.PROVIDER_URL, BaseUtils.getProperty("env.active.directory.provider.url", "ldap://WIN-6QJVSND9K4F.domserver.com:389")); // AD requires that this be a FQDN and not an IP
        // The value of Context.SECURITY_PRINCIPAL must be the logon username with the domain name
        env.put(Context.SECURITY_PRINCIPAL, ssoid);
        // The value of the Context.SECURITY_CREDENTIALS should be the user's password
        env.put(Context.SECURITY_CREDENTIALS, decryptedPassword);
        DirContext ctx = null;
        try {
            // Authenticate the user
            ctx = new InitialDirContext(env);
            /*
             * Once the above line was executed successfully, the user is
             * said to be authenticated and the InitialDirContext object
             * will be created.  
             */
            log.debug("Authentication against active directory succeeded");
            return true;
        } catch (Exception ex) {
            // Authentication failed, just check on the exception and do something about it.
            log.warn("Authentication failed [{}]", ex.toString());
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ex) {
                    log.warn("Error closing AD context", ex);
                }
            }
        }
        return false;
    }

    private boolean isCustomerAStaffMember(Integer customerProfileId) {
        for (com.smilecoms.im.db.model.Organisation org : IMDAO.getOrganisationsByCustomerProfileId(em, customerProfileId, 100)) {
            if (org.getOrganisationId() == 1) {
                return true;
            }
        }
        return false;
    }

    private String generateRandomEncryptedPublicK() {

        // Start with 16 ascii characters
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            int randomAscii = getAllowedRandomAscii();
            ascii.append((char) randomAscii);
        }
        return Codec.stringToEncryptedHexString(ascii.toString());
    }

    private static final Random rand = new Random(System.currentTimeMillis());

    private int getAllowedRandomAscii() {
        int caps = 65 + rand.nextInt(26); // A, B , C ... to Z
        if (rand.nextBoolean()) {
            return caps + 32; // make lowercase randomly
        }
        return caps;
    }

    private AuthenticationResult appAuthenticate(AuthenticationQuery authenticationQuery) throws Exception {
        log.debug("This is an authentication request for an app getting its configuration for IMPU [{}]", authenticationQuery.getIMSPublicIdentity());
        if (appAuthIsDoingBruteForce(authenticationQuery.getIMSPublicIdentity())) {
            PlatformEventManager.createEvent("APPAUTH", "BRUTEFORCE", authenticationQuery.getIMSPublicIdentity(), authenticationQuery.getIMSPublicIdentity() + "|" + authenticationQuery.getPlatformContext().getOriginatingIP());
            throw new Exception("App brute force detected");
        }
        List<Impi> impis = HSSDAO.getImpiListByImpuIdentity(em, authenticationQuery.getIMSPublicIdentity());
        AuthenticationResult res = new AuthenticationResult();
        for (Impi impi : impis) {
            log.debug("Looking at IMPI with identity [{}]", impi.getIdentity());

            // this will not be encrypted
            String passedActivationCode = authenticationQuery.getActivationCode();
            // this will have been one way hashed
            String passedAppKeyEncrypted = authenticationQuery.getEncryptedAppKey();
            if (passedActivationCode != null && !passedActivationCode.isEmpty()) {
                log.debug("This app auth request has an activation code. Lets check if its valid for today. Passed [{}]", passedActivationCode);
                if (activationCodeIsValid(impi.getPublicK(), passedActivationCode)) {
                    log.debug("Activation code is valid");
                    if (passedAppKeyEncrypted != null && !passedAppKeyEncrypted.isEmpty()) {
                        log.debug("Adding appK to database as we have a valid activation code");
                        HSSDAO.addAppK(em, impi.getId(), passedAppKeyEncrypted);
                    }
                    res.setDone(StDone.TRUE);
                    log.debug("App authentication based on activation code succeeded");
                    PlatformEventManager.createEvent("APPAUTH", "PASSAC", authenticationQuery.getIMSPublicIdentity(), authenticationQuery.getIMSPublicIdentity() + "|" + authenticationQuery.getPlatformContext().getOriginatingIP());
                    return res;
                } else {
                    log.debug("Invalid activation code");
                    continue;
                }
            }
            if (passedAppKeyEncrypted != null && !passedAppKeyEncrypted.isEmpty()) {
                log.debug("Authentication is with just an App Key");
                Set<String> appKeys = HSSDAO.getImpiAppKeys(em, impi.getId());
                if (appKeys.contains(passedAppKeyEncrypted)) {
                    res.setDone(StDone.TRUE);
                    log.debug("App authentication based on app key succeeded");
                    PlatformEventManager.createEvent("APPAUTH", "PASSAK", authenticationQuery.getIMSPublicIdentity(), authenticationQuery.getIMSPublicIdentity() + "|" + authenticationQuery.getPlatformContext().getOriginatingIP());
                    return res;
                } else {
                    log.debug("Invalid app k");
                }
            }
        }
        incrementBruteForceForAppAuth(authenticationQuery.getIMSPublicIdentity());
        PlatformEventManager.createEvent("APPAUTH", "FAIL", authenticationQuery.getIMSPublicIdentity(), authenticationQuery.getIMSPublicIdentity());
        throw new Exception("App authentication failed");
    }

    private void incrementBruteForceForAppAuth(String impu) {
        String key = "App_BF_" + impu;
        String tries = (String) CacheHelper.getFromRemoteCache(key);
        if (tries == null) {
            tries = "0";
        }
        String val = String.valueOf(Integer.parseInt(tries) + 1);
        log.debug("[{}] has now got [{}] failed app auth attempts", impu, val);
        CacheHelper.putInRemoteCacheSync(key, val, 3600);
    }

    private boolean appAuthIsDoingBruteForce(String impu) {
        String key = "App_BF_" + impu;
        String tries = (String) CacheHelper.getFromRemoteCache(key);
        if (tries == null) {
            tries = "0";
        }
        int iTries = Integer.parseInt(tries);
        if (iTries > BaseUtils.getIntProperty("env.im.app.auth.block.threshold", 10)) {
            log.warn("[{}] has [{}] failed app auth attempts. Assuming this is a brute force attack. Denying for an hour", impu, iTries);
            CacheHelper.putInRemoteCacheSync(key, tries, 3600);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "App", impu + " has had over " + iTries + " invalid auth attempts. Disallowing trying again for an hour");
            return true;
        }
        return false;
    }

    @Override
    public Photograph getPhoto(PlatformString photoGuid) throws IMError {
        setContext(photoGuid, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        Photograph photo = new Photograph();
        try {
            com.smilecoms.im.db.model.Photograph dbPhoto = IMDAO.getPhoto(em, photoGuid.getString());
            photo.setData(Utils.encodeBase64(dbPhoto.getData()));
            photo.setPhotoGuid(dbPhoto.getPhotoGuid());
            photo.setPhotoType(dbPhoto.getPhotoType());
        } catch (Exception e) {
            throw processError(IMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return photo;
    }

    private boolean scscfNeedsShedding(int idPreferredScscfSet, String scscfName) {
        SCSCFSet scscfSet = IMDataCache.preferredSCSCFsCache.get(idPreferredScscfSet);
        if (scscfSet == null || !scscfSet.isAllCSCFsUp() || scscfSet.isBalanced() || scscfSet.getScscfList() == null || scscfSet.getScscfList().isEmpty()) {
            return false;
        }

        log.debug("SCSCF set is unbalanced - let's check if we need to re-assign this SCSCF [{}]", scscfName);
        List<SCSCF> scscfList = scscfSet.getScscfList();
        if (scscfList == null || scscfList.isEmpty()) {
            return false;
        }

        for (SCSCF scscf : scscfList) {
            if (scscf.getScscfName().equalsIgnoreCase(scscfName)) {
                log.debug("The current assigned SCSCF has currentLoadPercentage of [{}] and MaxLoadPercentage [{}] with a delta of [{}]",
                        new Object[]{scscf.getCurrentLoadPercentage(), scscf.getMaxLoadPercentage(), scscf.getDeltarequired()});
                if (scscf.getDeltarequired() >= 0) {
                    return false;
                } else {
                    return true;
                }
            }
        }

        log.debug("Couldn't find scscf [{}] in cache", scscfName);
        return false;
    }

    @Override
    public NiraVerifyReply verifyCustomerWithNIRA(NiraVerifyQuery niraVerifyQuery) throws IMError {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        NiraVerifyReply nReply = new NiraVerifyReply();

        try {
            Date dateOfBirth = sdf.parse(niraVerifyQuery.getDateOfBirth().replaceAll("/", ""));

            NiraResponse rsp = NiraClient.checkIfCustomerIdExistsAtNIRA(niraVerifyQuery.getIdentityNumber(),
                    niraVerifyQuery.getCardNumber(),
                    niraVerifyQuery.getLastName(),
                    niraVerifyQuery.getFirstName(),
                    niraVerifyQuery.getOtherNames(),
                    dateOfBirth);

            if (rsp.isSuccessful()) { // Also update the customer_profile here once
                List<CustomerProfile> dbCustomerList = IMDAO.getCustomerProfileByIDNumber(em, niraVerifyQuery.getIdentityNumber(), 1);
                CustomerProfile dbCustomer = dbCustomerList.get(0);
                dbCustomer.setkYCStatus("V");
                em.persist(dbCustomer);
                em.flush();
            }

            nReply.setStatus(rsp.isSuccessful());
            return nReply;

        } catch (Exception ex) {
            throw processError(IMError.class, ex);
        }
    }

    @Override
    public NiraPasswordChangeReply changeNiraPassword(NiraPasswordChange niraPasswordChange) throws IMError {
        NiraPasswordChangeReply reply = new NiraPasswordChangeReply();
        String niraResponse = null;
        try {
            niraResponse = NiraClient.changeNIRAPassword(niraPasswordChange.getNewPassword(), niraPasswordChange.getSepUsername());
        } catch (Exception ex) {
            niraResponse = ex.getMessage();
            throw processError(IMError.class, ex);
        } finally {
            reply.setNiraResponse(niraResponse);
        }
        return reply;
    }

    @Override
    public NidaVerifyReply verifyCustomerWithNIDA(NidaVerifyQuery nidaVerifyQuery) throws IMError {
        NidaResponse nidaResponse = null;
        try {
            if (nidaVerifyQuery.getVerifiedBy() == null || nidaVerifyQuery.getVerifiedBy().isEmpty()) {
                throw new Exception("Originating identity not supplied.");
            }

            if (nidaVerifyQuery.getEntityType() == null || nidaVerifyQuery.getEntityType().isEmpty()) {
                throw new Exception("Entity type not suppied.");
            }

            if (nidaVerifyQuery.getEntityId() == null || nidaVerifyQuery.getEntityId().isEmpty()
                    || nidaVerifyQuery.equals("0")) {
                nidaVerifyQuery.setEntityId(nidaVerifyQuery.getIdentityNumber());
            }

            nidaResponse = NidaRestClient.verifyCustomerWithNIDA(nidaVerifyQuery.getIdentityNumber(), nidaVerifyQuery.getFingerprintB64(),
                    nidaVerifyQuery.getVerifiedBy(), nidaVerifyQuery.getEntityType(), nidaVerifyQuery.getEntityId());

            NidaVerifyReply reply = new NidaVerifyReply();

            if (nidaResponse.getCode() == null || nidaResponse.getCode().trim().isEmpty()) {
                throw new Exception("Null or Empty response code from NIDA");
            }

            if (nidaResponse.getCode().equals("00")) { // Success
                reply.setFirstName(nidaResponse.getFirstName());
                reply.setMiddleName(nidaResponse.getMiddleName());
                reply.setLastName(nidaResponse.getLastName());
                reply.setOtherNames(nidaResponse.getOtherNames());
                reply.setGender(nidaResponse.getGender());
                reply.setDateOfBirth(nidaResponse.getDateOfBirth());
                reply.setPlaceOfBirth(nidaResponse.getPlaceOfBirth());
                reply.setResidentRegion(nidaResponse.getResidentRegion());
                reply.setResidentDistrict(nidaResponse.getResidentDistrict());
                reply.setResidentWard(nidaResponse.getResidentWard());
                reply.setResidentVillage(nidaResponse.getResidentVillage());
                reply.setResidentStreet(nidaResponse.getResidentStreet());
                reply.setResidentHouseNo(nidaResponse.getResidentHouseNo());
                reply.setResidentPostalAddress(nidaResponse.getResidentPostalAddress());
                reply.setResidentPostCode(nidaResponse.getResidentPostCode());
                reply.setBirthCountry(nidaResponse.getBirthCountry());
                reply.setBirthRegion(nidaResponse.getBirthRegion());
                reply.setBirthDistrict(nidaResponse.getBirthDistrict());
                reply.setBirthWard(nidaResponse.getBirthWard());
                reply.setBirthCertificateNo(nidaResponse.getBirthCertificateNo());
                reply.setNationality(nidaResponse.getNationality());
                reply.setPhoneNumber(nidaResponse.getPhoneNumber());
                reply.setPhoto(nidaResponse.getPhoto());
                reply.setSignature(nidaResponse.getSignature());
                reply.setNIDATransactionId(nidaResponse.getTransactionId());
                return reply;
            } else if (nidaResponse.getCode().equals(BaseUtils.getProperty("env.defaced.status.code"))) {
                reply.setCode(nidaResponse.getCode());
                return reply;
            } else if (nidaResponse.getCode().equals("141")) {
                throw new Exception("Fingerprint match failed  -- NIDA Response Code: [" + nidaResponse.getCode() + "]");
            } else if (nidaResponse.getCode().equals("01")) {
                throw new Exception("General error -- NIDA Response Code: [" + nidaResponse.getCode() + "]");
            } else {
                //throw new Exception("Unknown response code from NIDA -- " + nidaResponse.getCode());
                throw new Exception("NIDA Verification failed -- NIDA Response: [" + nidaResponse.getCode() + "]");
            }
        } catch (Exception ex) {
            throw processError(IMError.class, ex);
        }
    }

    @Override
    public DocumentUniquenessReply checkDocumentUniqueness(DocumentUniquenessQuery documentUniquenessQuery) throws IMError {
        DocumentUniquenessReply reply = new DocumentUniquenessReply();
        try {
            IMDAO.checkIfDocumentHasBeenUsedBefore(em, documentUniquenessQuery.getDocumentHash());
            reply.setIsUnique(true);
        } catch (Exception ex) {
            throw processError(IMError.class, ex);
        }
        return reply;
    }

    @Override
    public VerifyForeignerReply verifyForeignerCustomer(VerifyForeignerQuery verifyForeignerQuery) throws IMError {
        log.debug("In verifyForeignerCustomer IM");
        VerifyForeignerResponse verifyForeignerResponse = null;
        try {
            if (verifyForeignerQuery.getVerifiedBy() == null || verifyForeignerQuery.getVerifiedBy().isEmpty()) {
                throw new Exception("Originating identity not supplied.");
            }

            if (verifyForeignerQuery.getEntityType() == null || verifyForeignerQuery.getEntityType().isEmpty()) {
                throw new Exception("Entity type not suppied.");
            }

            if (verifyForeignerQuery.getEntityId() == null || verifyForeignerQuery.getEntityId().isEmpty()
                    || verifyForeignerQuery.equals("0")) {
                verifyForeignerQuery.setEntityId(verifyForeignerQuery.getDocumentNo());
            }
            log.debug("Before verifyForeignerCustomer api call IM");
            verifyForeignerResponse = VerifyForeignerRestClient.verifyForeignerCustomer(verifyForeignerQuery.getDocumentNo(), verifyForeignerQuery.getCountryCode(), verifyForeignerQuery.getFingerprintB64(),
                    verifyForeignerQuery.getVerifiedBy(), verifyForeignerQuery.getEntityType(), verifyForeignerQuery.getEntityId());
            log.debug("after verifyForeignerCustomer api call IM");
            VerifyForeignerReply reply = new VerifyForeignerReply();

            if (verifyForeignerResponse.getStatus() == null || verifyForeignerResponse.getStatus().trim().isEmpty()) {
                throw new Exception("Null or Empty response code from verifyForeigner");
            }
//            log.debug("after verifyForeignerCustomer api call IM" + "***" + verifyForeignerResponse.getInfo().getGivenName());
            if (verifyForeignerResponse.getStatus().equalsIgnoreCase("0")) { // Success
                reply.setDateOfBirth(verifyForeignerResponse.getInfo().getDateOfBirth());
                reply.setEntryDocExpiryDate(verifyForeignerResponse.getInfo().getEntryDocExpiryDate());
                reply.setEntryDocNumber(verifyForeignerResponse.getInfo().getEntryDocNumber());
                reply.setGivenName(verifyForeignerResponse.getInfo().getGivenName());
                reply.setMessage(verifyForeignerResponse.getMessage());
                reply.setNationality(verifyForeignerResponse.getInfo().getNationality());
                reply.setPassportExpiryDate(verifyForeignerResponse.getInfo().getPassportExpiryDate());
                reply.setPassportNo(verifyForeignerResponse.getInfo().getPassportNo());
                reply.setPhoto(verifyForeignerResponse.getInfo().getPhoto());
                reply.setResidentDistrict(verifyForeignerResponse.getInfo().getResidentDistrict());
                reply.setResidentRegion(verifyForeignerResponse.getInfo().getResidentRegion());
                reply.setResidentWard(verifyForeignerResponse.getInfo().getResidentWard());
                reply.setSex(verifyForeignerResponse.getInfo().getSex());
                reply.setSignature(verifyForeignerResponse.getInfo().getSignature());
                reply.setStatus(verifyForeignerResponse.getStatus());
                reply.setSurname(verifyForeignerResponse.getInfo().getSurname());

                return reply;
            } else if (verifyForeignerResponse.getStatus().equals("141")) {
                throw new Exception("Fingerprint match failed  -- verifyForeigner Response Status: [" + verifyForeignerResponse.getStatus() + "]");
            } else if (verifyForeignerResponse.getStatus().equals("01")) {
                throw new Exception("General error -- verifyForeigner Response Status: [" + verifyForeignerResponse.getStatus() + "]");
            } else {
                throw new Exception("verifyForeigner Verification failed -- verifyForeigner Response: [" + verifyForeignerResponse.getStatus() + "]");
            }
        } catch (Exception ex) {
            throw processError(IMError.class, ex);
        }
    }

    @Override
    public VerifyDefacedReply verifyDefacedCustomer(VerifyDefacedQuery verifyDefacedQuery) throws IMError {
        log.debug("In verifyDefacedCustomer IM");
        VerifyDefacedCustomerResponse verifyDefacedCustomerResponse = null;
        try {
            if (verifyDefacedQuery.getVerifiedBy() == null || verifyDefacedQuery.getVerifiedBy().isEmpty()) {
                throw new Exception("Originating identity not supplied.");
            }

            if (verifyDefacedQuery.getEntityType() == null || verifyDefacedQuery.getEntityType().isEmpty()) {
                throw new Exception("Entity type not suppied.");
            }

            if (verifyDefacedQuery.getEntityId() == null || verifyDefacedQuery.getEntityId().isEmpty()
                    || verifyDefacedQuery.equals("0")) {
                verifyDefacedQuery.setEntityId(String.valueOf(verifyDefacedQuery.getNin()));
            }

            com.smilecoms.im.tz.verify.defaced.restclient.VerifyDefacedCustomerRequest verifyDefacedCustomerRequest = new com.smilecoms.im.tz.verify.defaced.restclient.VerifyDefacedCustomerRequest();
            if (verifyDefacedQuery.getNin() != null && !verifyDefacedQuery.getNin().isEmpty()) {
                verifyDefacedCustomerRequest.setNin(verifyDefacedQuery.getNin());
            }

            if (verifyDefacedQuery.getAnswer() != null && !verifyDefacedQuery.getAnswer().isEmpty()) {
                verifyDefacedCustomerRequest.setAnswer(verifyDefacedQuery.getAnswer());
            }

            if (verifyDefacedQuery.getQuestionCode() != null && !verifyDefacedQuery.getQuestionCode().isEmpty()) {
                verifyDefacedCustomerRequest.setQuestionCode(verifyDefacedQuery.getQuestionCode());
            }

            log.debug("Before VerifyDefacedCustomerRestClient api call IM");
            verifyDefacedCustomerResponse = VerifyDefacedCustomerRestClient.verifyDefacedCustomer(verifyDefacedCustomerRequest, verifyDefacedQuery.getVerifiedBy(), verifyDefacedQuery.getEntityType(), verifyDefacedQuery.getEntityId());
            log.debug("after VerifyDefacedCustomerRestClient api call IM");
            VerifyDefacedReply reply = new VerifyDefacedReply();

            if (verifyDefacedCustomerResponse.getCode() == null || verifyDefacedCustomerResponse.getCode().trim().isEmpty()) {
                throw new Exception("Null or Empty response code from NIDA fro alternative verification method");
            }
//            log.info("TEST" + "***" + verifyDefacedCustomerResponse.getCode() + " " + verifyDefacedCustomerResponse.getResult().getQuestionCode());
            if ((verifyDefacedCustomerResponse.getCode().equalsIgnoreCase("120") || verifyDefacedCustomerResponse.getCode().equalsIgnoreCase("00")) && verifyDefacedCustomerResponse.getResult().getFirstName() != null) { // Success
                reply.setId(verifyDefacedCustomerResponse.getId() != null ? verifyDefacedCustomerResponse.getId() : "");
                reply.setCode(verifyDefacedCustomerResponse.getCode() != null ? verifyDefacedCustomerResponse.getCode() : "");
                reply.setBirthCertificateNo(verifyDefacedCustomerResponse.getResult().getBirthCertificateNo() != null ? verifyDefacedCustomerResponse.getResult().getBirthCertificateNo() : "");
                reply.setBirthCountry(verifyDefacedCustomerResponse.getResult().getBirthCountry() != null ? verifyDefacedCustomerResponse.getResult().getBirthCountry() : "");
                reply.setBirthDistrict(verifyDefacedCustomerResponse.getResult().getBirthDistrict() != null ? verifyDefacedCustomerResponse.getResult().getBirthDistrict() : "");
                reply.setBirthRegion(verifyDefacedCustomerResponse.getResult().getBirthRegion() != null ? verifyDefacedCustomerResponse.getResult().getBirthRegion() : "");
                reply.setBirthWard(verifyDefacedCustomerResponse.getResult().getBirthWard() != null ? verifyDefacedCustomerResponse.getResult().getBirthWard() : "");
                reply.setDateOfBirth(verifyDefacedCustomerResponse.getResult().getDateOfBirth() != null ? verifyDefacedCustomerResponse.getResult().getDateOfBirth() : "");
                reply.setFirstName(verifyDefacedCustomerResponse.getResult().getFirstName() != null ? verifyDefacedCustomerResponse.getResult().getFirstName() : "");
                reply.setLastName(verifyDefacedCustomerResponse.getResult().getLastName() != null ? verifyDefacedCustomerResponse.getResult().getLastName() : "");
                reply.setMiddleName(verifyDefacedCustomerResponse.getResult().getMiddleName() != null ? verifyDefacedCustomerResponse.getResult().getMiddleName() : "");
                reply.setNationality(verifyDefacedCustomerResponse.getResult().getNationality() != null ? verifyDefacedCustomerResponse.getResult().getNationality() : "");
                reply.setOtherNames(verifyDefacedCustomerResponse.getResult().getOtherNames() != null ? verifyDefacedCustomerResponse.getResult().getOtherNames() : "");
                reply.setPhoneNumber(verifyDefacedCustomerResponse.getResult().getPhoneNumber() != null ? verifyDefacedCustomerResponse.getResult().getPhoneNumber() : "");
                reply.setPhoto(verifyDefacedCustomerResponse.getResult().getPhoto() != null ? verifyDefacedCustomerResponse.getResult().getPhoto() : "");
                reply.setPlaceOfBirth(verifyDefacedCustomerResponse.getResult().getPlaceOfBirth() != null ? verifyDefacedCustomerResponse.getResult().getPlaceOfBirth() : "");
                reply.setResidentDistrict(verifyDefacedCustomerResponse.getResult().getResidentDistrict() != null ? verifyDefacedCustomerResponse.getResult().getResidentDistrict() : "");
                reply.setResidentHouseNo(verifyDefacedCustomerResponse.getResult().getResidentHouseNo() != null ? verifyDefacedCustomerResponse.getResult().getResidentHouseNo() : "");
                reply.setResidentPostalAddress(verifyDefacedCustomerResponse.getResult().getResidentPostalAddress() != null ? verifyDefacedCustomerResponse.getResult().getResidentPostalAddress() : "");
                reply.setResidentPostCode(verifyDefacedCustomerResponse.getResult().getResidentPostCode() != null ? verifyDefacedCustomerResponse.getResult().getResidentPostCode() : "");
                reply.setResidentRegion(verifyDefacedCustomerResponse.getResult().getResidentRegion() != null ? verifyDefacedCustomerResponse.getResult().getResidentRegion() : "");
                reply.setResidentStreet(verifyDefacedCustomerResponse.getResult().getResidentStreet() != null ? verifyDefacedCustomerResponse.getResult().getResidentStreet() : "");
                reply.setResidentVillage(verifyDefacedCustomerResponse.getResult().getResidentVillage() != null ? verifyDefacedCustomerResponse.getResult().getResidentVillage() : "");
                reply.setResidentWard(verifyDefacedCustomerResponse.getResult().getResidentWard() != null ? verifyDefacedCustomerResponse.getResult().getResidentWard() : "");
                reply.setSex(verifyDefacedCustomerResponse.getResult().getSex() != null ? verifyDefacedCustomerResponse.getResult().getSex() : "");
                reply.setSignature(verifyDefacedCustomerResponse.getResult().getSignature() != null ? verifyDefacedCustomerResponse.getResult().getSignature() : "");
                return reply;
            } else if (verifyDefacedCustomerResponse.getCode().equalsIgnoreCase("120") && verifyDefacedCustomerResponse.getResult() != null && !verifyDefacedCustomerResponse.getResult().getQuestionCode().isEmpty()) {
                reply.setId(verifyDefacedCustomerResponse.getId() != null ? verifyDefacedCustomerResponse.getId() : "");
                reply.setCode(verifyDefacedCustomerResponse.getCode() != null ? verifyDefacedCustomerResponse.getCode() : "");
                if (verifyDefacedCustomerResponse.getResult().getPrevCode() != null && !verifyDefacedCustomerResponse.getResult().getPrevCode().isEmpty()) {
                    reply.setPrevCode(verifyDefacedCustomerResponse.getResult().getPrevCode());
                }
                reply.setQuestionCode(verifyDefacedCustomerResponse.getResult().getQuestionCode() != null ? verifyDefacedCustomerResponse.getResult().getQuestionCode() : "");
                reply.setQuestionEnglish(verifyDefacedCustomerResponse.getResult().getQuestionEnglish() != null ? verifyDefacedCustomerResponse.getResult().getQuestionEnglish() : "");
                reply.setQuestionSwahili(verifyDefacedCustomerResponse.getResult().getQuestionSwahili() != null ? verifyDefacedCustomerResponse.getResult().getQuestionSwahili() : "");
                return reply;
            } else {
                reply.setId(verifyDefacedCustomerResponse.getId() != null ? verifyDefacedCustomerResponse.getId() : "");
                reply.setCode(verifyDefacedCustomerResponse.getCode() != null ? verifyDefacedCustomerResponse.getCode() : "");
                return reply;
//                throw new Exception("verifyForeigner Verification failed -- alternative varification Response: [" + verifyDefacedCustomerResponse.getCode() + "]");
            }
        } catch (Exception ex) {
            throw processError(IMError.class, ex);
        }
    }

    //profile photo is used for selfcare application
    private boolean isOnlyProfilePhotoChange(int customerId, List<Photograph> customerPhotographs) {
        try{
            List<Photograph> dbPhotographs = IMDAO.getCustomersPhotographs(em, customerId);
            
            for(Photograph dbPhoto : dbPhotographs) {                
                for(Photograph modPhoto: customerPhotographs) {
                    //We need to check if there are changes of images that are not the propic 
                    if(!dbPhoto.getPhotoType().equalsIgnoreCase("profilephoto") && modPhoto.getPhotoType().equalsIgnoreCase(dbPhoto.getPhotoType())) {
                        if(!HashUtils.md5(dbPhoto.getData()).equals(HashUtils.md5(modPhoto.getData()))) {
                            //A photo that is not a propic has been changed
                            return false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();            
            return false;
        }
        
        return true;
    }

    @Override
    public VerifyNinResponseList verifyNinCustomer(VerifyNinQuery verifyNinQuery) throws IMError {
        log.debug("verifyNinCustomer in IM");
        NinResponse ninResponse = null;
        try {
            if (verifyNinQuery.getNin() == null || verifyNinQuery.getNin().isEmpty()) {
                throw new Exception("NIN is not suppied.");
            }
            log.warn("NIN is ReturnMessage() before request");
            ninResponse = NinClient.ninVerificationLogic(verifyNinQuery.getNin().trim(), verifyNinQuery.getSurname(), verifyNinQuery.getFirstName(), verifyNinQuery.getLastName(), verifyNinQuery.getFingerStringInBase64(), verifyNinQuery.getVerifiedBy(), verifyNinQuery.getEntityId(), verifyNinQuery.getEntityType());
            VerifyNinResponseList verifyNinResponseList = new VerifyNinResponseList();
            log.warn("NIN is ReturnMessage() test" + ninResponse.getReturnMessage());
            if (ninResponse != null) {
                if (ninResponse.getReturnMessage() != null || !ninResponse.getReturnMessage().isEmpty()) {
                    verifyNinResponseList.setReturnMessage(ninResponse.getReturnMessage());
                    if (ninResponse.getReturnMessage().equalsIgnoreCase(BaseUtils.getProperty("env.nin.status.message"))) {
                        log.warn("NIN is ReturnMessage() inside" + ninResponse.getReturnMessage());
                        
                        for (DemoData demoData : ninResponse.getData()) {
                            VerifyNinReply verifyNinReply = new VerifyNinReply();
                            verifyNinReply.setBatchid(demoData.getBatchid() != null ? demoData.getBatchid() : "");
                            verifyNinReply.setBirthcountry(demoData.getBirthcountry() != null ? demoData.getBirthcountry() : "");
                            verifyNinReply.setBirthdate(demoData.getBirthdate() != null ? demoData.getBirthdate() : "");
                            verifyNinReply.setBirthlga(demoData.getBirthlga() != null ? demoData.getBirthlga() : "");
                            verifyNinReply.setBirthstate(demoData.getBirthstate() != null ? demoData.getBirthstate() : "");
                            verifyNinReply.setCardstatus(demoData.getCardstatus() != null ? demoData.getCardstatus() : "");
                            verifyNinReply.setCentralID(demoData.getCentralID() != null ? demoData.getCentralID() : "");
                            verifyNinReply.setDocumentno(demoData.getDocumentno() != null ? demoData.getDocumentno() : "");
                            verifyNinReply.setEducationallevel(demoData.getEducationallevel() != null ? demoData.getEducationallevel() : "");
                            verifyNinReply.setEmail(demoData.getEmail() != null ? demoData.getEmail() : "");
                            verifyNinReply.setEmplymentstatus(demoData.getEmplymentstatus() != null ? demoData.getEmplymentstatus() : "");
                            verifyNinReply.setFirstname(demoData.getFirstname() != null ? demoData.getFirstname() : "");
                            verifyNinReply.setGender(demoData.getGender() != null ? demoData.getGender() : "");
                            verifyNinReply.setHeigth(demoData.getHeigth() != null ? demoData.getHeigth() : "");
                            verifyNinReply.setMaidenname(demoData.getMaidenname() != null ? demoData.getMaidenname() : "");
                            verifyNinReply.setMaritalstatus(demoData.getMaritalstatus() != null ? demoData.getMaritalstatus() : "");
                            verifyNinReply.setMiddlename(demoData.getMiddlename() != null ? demoData.getMiddlename() : "");
                            verifyNinReply.setNin(demoData.getNin() != null ? demoData.getNin() : "");
                            verifyNinReply.setNokAddress1(demoData.getNokAddress1() != null ? demoData.getNokAddress1() : "");
                            
                            log.warn("NIN is NOK Address1" + demoData.getNokAddress1());
                            verifyNinReply.setNokAddress2(demoData.getNokAddress2() != null ? demoData.getNokAddress2() : "");
                            verifyNinReply.setNokFirstname(demoData.getNokFirstname() != null ? demoData.getNokFirstname() : "");
                            verifyNinReply.setNokLga(demoData.getNokLga() != null ? demoData.getNokLga() : "");
                            verifyNinReply.setNokMiddlename(demoData.getNokMiddlename() != null ? demoData.getNokMiddlename() : "");
                            verifyNinReply.setNokPostalcode(demoData.getNokPostalcode() != null ? demoData.getNokPostalcode() : "");
                            verifyNinReply.setNokState(demoData.getNokState() != null ? demoData.getNokState() : "");
                            verifyNinReply.setNokSurname(demoData.getNokSurname() != null ? demoData.getNokSurname() : "");
                            verifyNinReply.setNokTown(demoData.getNokTown() != null ? demoData.getNokTown() : "");
                            verifyNinReply.setNspokenlang(demoData.getNspokenlang() != null ? demoData.getNspokenlang() : "");
                            verifyNinReply.setOspokenlang(demoData.getOspokenlang() != null ? demoData.getOspokenlang() : "");
                            verifyNinReply.setOthername(demoData.getOthername() != null ? demoData.getOthername() : "");
                            verifyNinReply.setPfirstname(demoData.getPfirstname() != null ? demoData.getPfirstname() : "");
                            verifyNinReply.setPhoto(demoData.getPhoto() != null ? demoData.getPhoto() : "");
                            verifyNinReply.setPmiddlename(demoData.getPmiddlename() != null ? demoData.getPmiddlename() : "");
                            verifyNinReply.setProfession(demoData.getProfession() != null ? demoData.getProfession() : "");
                            verifyNinReply.setPsurname(demoData.getPsurname() != null ? demoData.getPsurname() : "");
                            verifyNinReply.setReligion(demoData.getReligion() != null ? demoData.getReligion() : "");
                            
                            log.warn("NIN is RES Address1" + demoData.getResidenceAdressLine1());
                            verifyNinReply.setResidenceAdressLine1(demoData.getResidenceAdressLine1() != null ? demoData.getResidenceAdressLine1() : "");
                            verifyNinReply.setResidenceAdressLine2(demoData.getResidenceAdressLine2() != null ? demoData.getResidenceAdressLine2() : "");
                            verifyNinReply.setResidenceLga(demoData.getResidenceLga() != null ? demoData.getResidenceLga() : "");
                            verifyNinReply.setResidencePostalcode(demoData.getResidencePostalcode() != null ? demoData.getResidencePostalcode() : "");
                            
                            log.warn("NIN is RES STATE" + demoData.getResidenceState());
                            verifyNinReply.setResidenceState(demoData.getResidenceState() != null ? demoData.getResidenceState() : "");
                            verifyNinReply.setResidenceTown(demoData.getResidenceTown() != null ? demoData.getResidenceTown() : "");
                            verifyNinReply.setResidencestatus(demoData.getResidencestatus() != null ? demoData.getResidencestatus() : "");
                            verifyNinReply.setSelfOriginLga(demoData.getSelfOriginLga() != null ? demoData.getSelfOriginLga() : "");
                            verifyNinReply.setSelfOriginPlace(demoData.getSelfOriginPlace() != null ? demoData.getSelfOriginPlace() : "");
                            verifyNinReply.setSelfOriginState(demoData.getSelfOriginState() != null ? demoData.getSelfOriginState() : "");
                            verifyNinReply.setSignature(demoData.getSignature() != null ? demoData.getSignature() : "");
                            verifyNinReply.setSurname(demoData.getSurname() != null ? demoData.getSurname() : "");
                            verifyNinReply.setTelephoneno(demoData.getTelephoneno() != null ? demoData.getTelephoneno() : "");
                            verifyNinReply.setTitle(demoData.getTitle() != null ? demoData.getTitle() : "");
                            verifyNinReply.setTrackingId(demoData.getTrackingId() != null ? demoData.getTrackingId() : "");
                            verifyNinResponseList.getVerifyNinReplys().add(verifyNinReply);
                        }
                    } else {
                        throw new Exception(ninResponse.getReturnMessage());
                    }
                }
                return verifyNinResponseList;
            }

            return verifyNinResponseList;
        } catch (Exception ex) {
            throw processError(IMError.class, ex);
        }
    }

    class AssociationComparator implements Comparator {

        @Override
        public int compare(Object assoc1, Object assoc2) {
            // Compare sorts from smallest to largest so negate to sort from highest priority to lowest
            Integer barring1 = ((IMSNestedIdentityAssociation) assoc1).getIMSPublicIdentity().getBarring();
            Integer barring2 = ((IMSNestedIdentityAssociation) assoc2).getIMSPublicIdentity().getBarring();
            return barring2.compareTo(barring1);
        }
    }

    public static boolean getMandatoryKycfieldsStatus(MandatoryKYCFields kycStatus) {
        boolean status = false;
        if (!kycStatus.getDobVerified().equals("N") && !kycStatus.getEmailVerified().equals("N") && !kycStatus.getFacialPitureVerified().equals("N") && !kycStatus.getFingerPrintVerified().equals("N")
                && !kycStatus.getGenderVerified().equals("N") && !kycStatus.getMobileVerified().equals("N") && !kycStatus.getNameVerified().equals("N") && !kycStatus.getNationalityVerified().equals("N")
                && !kycStatus.getPhysicalAddressVerified().equals("N") && !kycStatus.getTitleVerified().equals("N") && !kycStatus.getValidIdCardVerified().equals("N")) {
            status = true;
        }
        return status;
    }

    private void syncXMLMandatoryKYCFieldsIntoDB(MandatoryKYCFields mandatoryKYCFields, com.smilecoms.im.db.model.MandatoryKYCFields dbMandatoryKYCFields) throws Exception {
        dbMandatoryKYCFields.setCustomerId(mandatoryKYCFields.getCustomerId());
        dbMandatoryKYCFields.setDobVerified(mandatoryKYCFields.getDobVerified());
        dbMandatoryKYCFields.setEmailVerified(mandatoryKYCFields.getEmailVerified());
        dbMandatoryKYCFields.setFacialPitureVerified(mandatoryKYCFields.getFacialPitureVerified());
        dbMandatoryKYCFields.setFingerPrintVerified(mandatoryKYCFields.getFingerPrintVerified());
        dbMandatoryKYCFields.setGenderVerified(mandatoryKYCFields.getGenderVerified());
        dbMandatoryKYCFields.setMobileVerified(mandatoryKYCFields.getMobileVerified());
        dbMandatoryKYCFields.setNameVerified(mandatoryKYCFields.getNameVerified());
        dbMandatoryKYCFields.setNationalityVerified(mandatoryKYCFields.getNationalityVerified());
        dbMandatoryKYCFields.setPhysicalAddressVerified(mandatoryKYCFields.getPhysicalAddressVerified());
        dbMandatoryKYCFields.setTitleVerified(mandatoryKYCFields.getTitleVerified());
        dbMandatoryKYCFields.setValidIdCardVerified(mandatoryKYCFields.getValidIdCardVerified());
    }

    private void syncXMLCustomerNinDataIntoDB(CustomerNinData customerNinData, com.smilecoms.im.db.model.CustomerNinData dbCustomerNinData) throws Exception {
        dbCustomerNinData.setCustomerProfileId(customerNinData.getCustomerProfileId());
        dbCustomerNinData.setNinVerificationTrackingId(customerNinData.getNinVerificationTrackingId());
        dbCustomerNinData.setNinVerificationType(customerNinData.getNinVerificationType());
        dbCustomerNinData.setNinVerifiedDate(customerNinData.getNinVerifiedDate());
        dbCustomerNinData.setNinResponseStatus(customerNinData.getNinResponseStatus());
        dbCustomerNinData.setNinCollectionDate(customerNinData.getNinCollectionDate());
    }

    private CustomerNinData xMLCustomerNinData(com.smilecoms.im.db.model.CustomerNinData dbCustomerNinData) throws Exception {
        CustomerNinData customerNinData = new CustomerNinData();
        customerNinData.setNinDataId(dbCustomerNinData.getId());
        customerNinData.setCustomerProfileId(dbCustomerNinData.getCustomerProfileId());
        customerNinData.setNinVerificationTrackingId(dbCustomerNinData.getNinVerificationTrackingId() != null ? dbCustomerNinData.getNinVerificationTrackingId() : "");
        customerNinData.setNinVerificationType(dbCustomerNinData.getNinVerificationType() != null ? dbCustomerNinData.getNinVerificationType() : "");
        customerNinData.setNinVerifiedDate(dbCustomerNinData.getNinVerifiedDate() != null ? dbCustomerNinData.getNinVerifiedDate() : "");
        customerNinData.setNinResponseStatus(dbCustomerNinData.getNinResponseStatus() != null ? dbCustomerNinData.getNinResponseStatus() : "");
        customerNinData.setNinCollectionDate(dbCustomerNinData.getNinCollectionDate() != null ? dbCustomerNinData.getNinCollectionDate() : "");
        return customerNinData;
    }

    private MandatoryKYCFields xMLMandatoryKYCFields(com.smilecoms.im.db.model.MandatoryKYCFields dbMandatoryKYCFields) throws Exception {
        MandatoryKYCFields mandatoryKYCFields = new MandatoryKYCFields();
        mandatoryKYCFields.setCustomerId(dbMandatoryKYCFields.getCustomerId());
        mandatoryKYCFields.setDobVerified(dbMandatoryKYCFields.getDobVerified());
        mandatoryKYCFields.setEmailVerified(dbMandatoryKYCFields.getEmailVerified());
        mandatoryKYCFields.setFacialPitureVerified(dbMandatoryKYCFields.getFacialPitureVerified());
        mandatoryKYCFields.setFingerPrintVerified(dbMandatoryKYCFields.getFingerPrintVerified());
        mandatoryKYCFields.setGenderVerified(dbMandatoryKYCFields.getGenderVerified());
        mandatoryKYCFields.setMobileVerified(dbMandatoryKYCFields.getMobileVerified());
        mandatoryKYCFields.setNameVerified(dbMandatoryKYCFields.getNameVerified());
        mandatoryKYCFields.setNationalityVerified(dbMandatoryKYCFields.getNationalityVerified());
        mandatoryKYCFields.setPhysicalAddressVerified(dbMandatoryKYCFields.getPhysicalAddressVerified());
        mandatoryKYCFields.setTitleVerified(dbMandatoryKYCFields.getTitleVerified());
        mandatoryKYCFields.setValidIdCardVerified(dbMandatoryKYCFields.getValidIdCardVerified());
        return mandatoryKYCFields;
    }
    
/*    @Override
    public ProductsKycVerificationResponseList getSaleForKycVerification(ProductsKycVerificationQuery productsKycVerificationQuery) throws IMError {
        ProductsKycVerificationResponseList reply = new ProductsKycVerificationResponseList();
        
        
        
        reply.setReturnMessage("Returned KycSales");
        return reply;
    }*/
    
    @Override
    public ProductsKycVerificationResponseList kycVerifySale(ProductsKycVerificationQuery productsKycVerificationQuery) throws IMError {
        
        ProductsKycVerificationResponseList reply = new ProductsKycVerificationResponseList();
        
        
        reply.setReturnMessage("Updated and Verified");
        return reply;
    }
}
