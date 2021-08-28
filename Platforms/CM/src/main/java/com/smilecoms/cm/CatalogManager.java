/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm;

import com.smilecoms.cm.db.model.CampaignRun;
import com.smilecoms.cm.db.model.ProductServiceMapping;
import com.smilecoms.cm.db.op.DAO;
import com.smilecoms.cm.pcrf.api.op.PCRFAPI;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.sca.Address;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.Javassist;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.commons.sca.direct.im.DocumentUniquenessQuery;
import com.smilecoms.commons.sca.direct.im.NidaVerifyQuery;
import com.smilecoms.commons.sca.direct.im.NidaVerifyReply;
import com.smilecoms.commons.sca.direct.im.VerifyForeignerQuery;
import com.smilecoms.commons.sca.direct.im.VerifyForeignerReply;
import com.smilecoms.commons.util.HashUtils;
import static com.smilecoms.commons.util.Utils.getListFromCRDelimitedString;
import com.smilecoms.xml.cm.CMError;
import com.smilecoms.xml.cm.CMSoap;
import com.smilecoms.xml.schema.cm.*;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;

/**
 *
 * @author mukosi
 */
@WebService(serviceName = "CM", portName = "CMSoap", endpointInterface = "com.smilecoms.xml.cm.CMSoap", targetNamespace = "http://xml.smilecoms.com/CM", wsdlLocation = "CMServiceDefinition.wsdl")
@HandlerChain(file = "/handler.xml")
@Stateless
public class CatalogManager extends SmileWebService implements CMSoap {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;
    @PersistenceContext(unitName = "CMPU")
    private EntityManager em;

    public static enum SERVICE_INSTANCE_STATUS {
        // If you change these, also look in the entity class finders for impact there

        AC, TD, DE;
    };

    public static enum PRODUCT_INSTANCE_STATUS {
        // If you change these, also look in the entity class finders for impact there

        AC, DE;
    };

    @Override
    public com.smilecoms.xml.schema.cm.Done isUp(java.lang.String isUpRequest) throws CMError {
        return makeDone();
    }

    private Done makeDone() {
        Done done = new Done();
        done.setDone(StDone.TRUE);
        return done;
    }

    @Override
    public ProductInstance createProductInstance(ProductInstance xmlProductInstance) throws CMError {
        setContext(xmlProductInstance, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        com.smilecoms.cm.db.model.ProductInstance dbProductInstance = new com.smilecoms.cm.db.model.ProductInstance();

        try {
            // Create the header information ...
            dbProductInstance.setCustomerProfileId(xmlProductInstance.getCustomerId());
            dbProductInstance.setOrganisationId(xmlProductInstance.getOrganisationId());
            dbProductInstance.setSegment(xmlProductInstance.getSegment());
            dbProductInstance.setFriendlyName(xmlProductInstance.getFriendlyName() == null ? "" : xmlProductInstance.getFriendlyName());
            int logicalId = getProductInstanceLogicalId(xmlProductInstance.getPhysicalId());
            com.smilecoms.cm.db.model.ProductSpecification prodSpec = DAO.getProductSpecificationById(em, xmlProductInstance.getProductSpecificationId());
            if (logicalId > 0) {
                log.debug("Product instance has the same logical Id as a previous product instance. Ensuring the reporting type has not changed or else it will mess with logical SIM base reporting");
                String existingReportingType = DAO.getReportingTypeForLogicalId(em, logicalId);
                String newReportingType = prodSpec.getReportingType();
                if (!existingReportingType.equals(newReportingType)) {
                    throw new Exception("Cannot move a SIM between completely different product types -- " + existingReportingType + " to " + newReportingType);
                }
            }
            dbProductInstance.setLogicalId(logicalId);
            dbProductInstance.setPhysicalId(xmlProductInstance.getPhysicalId() == null ? "" : xmlProductInstance.getPhysicalId());
            dbProductInstance.setLastIMEI("");
            dbProductInstance.setProductSpecificationId(prodSpec);
            dbProductInstance.setCreatedDatetime(new Date());
            dbProductInstance.setPromotionCode(xmlProductInstance.getPromotionCode() == null ? "" : xmlProductInstance.getPromotionCode());
            dbProductInstance.setReferralCode(xmlProductInstance.getReferralCode() == null ? "" : xmlProductInstance.getReferralCode());
            dbProductInstance.setCreatedByCustomerProfileId(DAO.getCustomerProfileIdBySSOIdentity(em, xmlProductInstance.getPlatformContext().getOriginatingIdentity()));
            try {
                dbProductInstance.setCreatedByOrganisationId(DAO.getFirstOrgIdBySSOIdentity(em, xmlProductInstance.getPlatformContext().getOriginatingIdentity()));
            } catch (Exception e) {
                log.warn("Error setting org id on product instance", e);
            }
            dbProductInstance = DAO.createProductInstance(em, dbProductInstance);
            if (dbProductInstance.getLogicalId() == 0) {
                log.debug("We have a new logical Id. Setting logical Id to the product instance Id [{}]", dbProductInstance.getProductInstanceId());
                dbProductInstance.setLogicalId(dbProductInstance.getProductInstanceId());
                em.persist(dbProductInstance);
                em.flush();
            }
            xmlProductInstance = getXMLProductInstance(dbProductInstance, "MAIN");
            createEvent(dbProductInstance.getProductInstanceId());

            Calendar eventDelay = Calendar.getInstance();
            eventDelay.add(Calendar.SECOND, BaseUtils.getIntProperty("env.cm.new.product.delay.notification", 60));

            PlatformEventManager.createEvent("CL_PRODUCT", "NEW",
                    String.valueOf(dbProductInstance.getProductInstanceId()),
                    "PIId=" + dbProductInstance.getProductInstanceId(),
                    eventDelay.getTime());

        } catch (Exception e) {
            throw processError(CMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }

        return xmlProductInstance;
    }

    @Override
    public ProductInstanceList getProductInstances(ProductInstanceQuery productInstanceQuery) throws CMError {
        setContext(productInstanceQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        ProductInstanceList xmlProdInstList = null;
        try {
            xmlProdInstList = new ProductInstanceList();
            if (productInstanceQuery.getProductInstanceId() > 0) {
                com.smilecoms.cm.db.model.ProductInstance dbProdInst = DAO.getProductInstanceById(em, productInstanceQuery.getProductInstanceId());
                xmlProdInstList.getProductInstances().add(getXMLProductInstance(dbProdInst, productInstanceQuery.getVerbosity()));
            } else if (productInstanceQuery.getCustomerId() > 0) {
                xmlProdInstList.setProductInstancesTotalCount(DAO.getCustomerRelatedProductInstancesTotalCount(em, productInstanceQuery.getCustomerId()));
                log.debug("total count by cust id {}", xmlProdInstList.getProductInstancesTotalCount());
                Collection<com.smilecoms.cm.db.model.ProductInstance> dbProdInstList = DAO.getCustomerRelatedProductInstances(em, productInstanceQuery.getCustomerId(), productInstanceQuery.getOffset(), productInstanceQuery.getResultLimit());
                for (com.smilecoms.cm.db.model.ProductInstance dbProdInst : dbProdInstList) {
                    xmlProdInstList.getProductInstances().add(getXMLProductInstance(dbProdInst, productInstanceQuery.getVerbosity()));
                }
            } else if (productInstanceQuery.getOrganisationId() > 0 && productInstanceQuery.getProductSpecificationId() > 0) {
                Collection<com.smilecoms.cm.db.model.ProductInstance> dbProdInstList = DAO.getOrganisationProductInstancesWithSpecId(em, productInstanceQuery.getOrganisationId(), productInstanceQuery.getProductSpecificationId());
                for (com.smilecoms.cm.db.model.ProductInstance dbProdInst : dbProdInstList) {
                    xmlProdInstList.getProductInstances().add(getXMLProductInstance(dbProdInst, productInstanceQuery.getVerbosity()));
                }
            } else if (productInstanceQuery.getOrganisationId() > 0) {
                xmlProdInstList.setProductInstancesTotalCount(DAO.getOrganisationProductInstancesTotalCount(em, productInstanceQuery.getOrganisationId()));
                log.debug("total count by org id {}", xmlProdInstList.getProductInstancesTotalCount());
                Collection<com.smilecoms.cm.db.model.ProductInstance> dbProdInstList = DAO.getOrganisationProductInstances(em, productInstanceQuery.getOrganisationId(), productInstanceQuery.getOffset(), productInstanceQuery.getResultLimit());
                for (com.smilecoms.cm.db.model.ProductInstance dbProdInst : dbProdInstList) {
                    xmlProdInstList.getProductInstances().add(getXMLProductInstance(dbProdInst, productInstanceQuery.getVerbosity()));
                }
            } else if (productInstanceQuery.getServiceInstanceId() > 0) {
                com.smilecoms.cm.db.model.ProductInstance dbProdInst = DAO.getProductInstanceByServiceInstanceId(em, productInstanceQuery.getServiceInstanceId());
                xmlProdInstList.getProductInstances().add(getXMLProductInstance(dbProdInst, productInstanceQuery.getVerbosity()));
            } else if (productInstanceQuery.getPhysicalId() != null && !productInstanceQuery.getPhysicalId().isEmpty()) {
                Collection<com.smilecoms.cm.db.model.ProductInstance> dbProdInstList = DAO.getProductInstancesByPhysicalId(em, productInstanceQuery.getPhysicalId());
                for (com.smilecoms.cm.db.model.ProductInstance dbProdInst : dbProdInstList) {
                    xmlProdInstList.getProductInstances().add(getXMLProductInstance(dbProdInst, productInstanceQuery.getVerbosity()));
                }
            }
            xmlProdInstList.setNumberOfProductInstances(xmlProdInstList.getProductInstances().size());

            if (xmlProdInstList.getProductInstancesTotalCount() == null || xmlProdInstList.getProductInstancesTotalCount() <= 0) {
                xmlProdInstList.setProductInstancesTotalCount(xmlProdInstList.getNumberOfProductInstances());
            }

        } catch (Exception e) {
            throw processError(CMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return xmlProdInstList;

    }

    @Override
    public ServiceInstanceList getServiceInstances(ServiceInstanceQuery serviceInstanceQuery) throws CMError {
        setContext(serviceInstanceQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        ServiceInstanceList xmlSvcInstList = null;

        try {

            xmlSvcInstList = new ServiceInstanceList();
            Collection<com.smilecoms.cm.db.model.ServiceInstance> dbSvcInstList = null;
            boolean addMACData = false;
            if (serviceInstanceQuery.getCustomerId() > 0) {
                dbSvcInstList = DAO.getCustomerServiceInstances(em, serviceInstanceQuery.getCustomerId());
            } else if (serviceInstanceQuery.getAccountId() > 0) {
                dbSvcInstList = DAO.getAccountServiceInstances(em, serviceInstanceQuery.getAccountId());
            } else if (serviceInstanceQuery.getServiceInstanceId() > 0) {
                dbSvcInstList = new ArrayList();
                dbSvcInstList.add(DAO.getServiceInstanceById(em, serviceInstanceQuery.getServiceInstanceId()));
            } else if (serviceInstanceQuery.getIdentifier() != null && serviceInstanceQuery.getIdentifierType() != null && !serviceInstanceQuery.getIdentifier().isEmpty() && !serviceInstanceQuery.getIdentifierType().isEmpty()) {
                dbSvcInstList = DAO.getServiceInstancesByIdentifierAndType(em, serviceInstanceQuery.getIdentifier(), serviceInstanceQuery.getIdentifierType());
            } else if (serviceInstanceQuery.getProductInstanceId() > 0 && serviceInstanceQuery.getServiceSpecificationId() > 0) {
                dbSvcInstList = DAO.getServiceInstancesByProductInstanceAndServiceSpec(em, serviceInstanceQuery.getProductInstanceId(), serviceInstanceQuery.getServiceSpecificationId());
            } else if (serviceInstanceQuery.getProductInstanceId() > 0) {
                dbSvcInstList = DAO.getServiceInstancesByProductInstanceId(em, serviceInstanceQuery.getProductInstanceId());
            } else if (serviceInstanceQuery.getIPAddress() != null && !serviceInstanceQuery.getIPAddress().isEmpty()) {
                dbSvcInstList = new ArrayList();

                if (BaseUtils.getProperty("env.pcrf.data.model", "DB").equals("DB")) {
                    dbSvcInstList.add(DAO.getServiceInstanceByIPAddress(em, serviceInstanceQuery.getIPAddress()));
                } else {
                    //get highest priority service id from PCRF
                    String bindingIdentifier = "/" + serviceInstanceQuery.getIPAddress();
                    int svcInstanceId = PCRFAPI.doPCRFGetServiceInstanceIdByIPAddress(BaseUtils.getProperty("env.pcrf.api.url", "http://10.0.1.129:8080/mobicents/"), bindingIdentifier);
                    if (svcInstanceId != -1) {
                        //then get service instance from id
                        dbSvcInstList.add(DAO.getServiceInstanceById(em, svcInstanceId));
                        addMACData = true;
                    } else {
                        throw new Exception("IP Address currently not attached to network");
                    }
                }
            } else if (serviceInstanceQuery.getServiceSpecificationId() > 0) {
                dbSvcInstList = DAO.getServiceInstancesByServiceSpec(em, serviceInstanceQuery.getServiceSpecificationId());
            } else {
                throw new Exception("Service Instance query did not contain any query data");
            }

            for (com.smilecoms.cm.db.model.ServiceInstance dbSvcInst : dbSvcInstList) {
                xmlSvcInstList.getServiceInstances().add(getXMLServiceInstance(dbSvcInst, serviceInstanceQuery.getVerbosity()));
            }
            xmlSvcInstList.setNumberOfServiceInstances(xmlSvcInstList.getServiceInstances().size());

            if (addMACData && !xmlSvcInstList.getServiceInstances().isEmpty() && serviceInstanceQuery.getVerbosity().contains("AVP")) {
                log.debug("Adding an AVP for the IMEISV Address of the SI");
                AVP avp = new AVP();
                avp.setAttribute("IMEISV");

                if (BaseUtils.getProperty("env.pcrf.data.model", "DB").equals("DB")) {
                    avp.setValue(DAO.getIMEIForIPAddress(em, serviceInstanceQuery.getIPAddress()));
                } else {
                    String bindingIdentifier = "/" + serviceInstanceQuery.getIPAddress();
                    String imei = PCRFAPI.doPCRFGetImeiByIPAddress(BaseUtils.getProperty("env.pcrf.api.url", "http://10.0.1.129:8080/mobicents/"), bindingIdentifier);
                    avp.setValue(imei);
                }

                xmlSvcInstList.getServiceInstances().get(0).getAVPs().add(avp);
            }

        } catch (Exception e) {
            throw processError(CMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return xmlSvcInstList;
    }

    @Override
    public UnitCreditSpecificationList getUnitCreditSpecifications(UnitCreditSpecificationQuery unitCreditSpecificationQuery) throws CMError {
        setContext(unitCreditSpecificationQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        UnitCreditSpecificationList xmlUnitCreditSpecList = new UnitCreditSpecificationList();

        try {
            if (unitCreditSpecificationQuery.getUnitCreditSpecificationId() > 0) {
                xmlUnitCreditSpecList.getUnitCreditSpecifications().add(getXMLUnitCreditSpecification(unitCreditSpecificationQuery.getUnitCreditSpecificationId(), unitCreditSpecificationQuery.getVerbosity()));
            } else if (unitCreditSpecificationQuery.getUnitCreditSpecificationId() == 0 && unitCreditSpecificationQuery.getUnitCreditName() != null && !unitCreditSpecificationQuery.getUnitCreditName().isEmpty()) {
                xmlUnitCreditSpecList.getUnitCreditSpecifications().add(getXMLUnitCreditSpecificationByName(unitCreditSpecificationQuery.getUnitCreditName(), unitCreditSpecificationQuery.getVerbosity()));
            } else if (unitCreditSpecificationQuery.getUnitCreditSpecificationId() == 0 && unitCreditSpecificationQuery.getItemNumber() != null && !unitCreditSpecificationQuery.getItemNumber().isEmpty()) {
                xmlUnitCreditSpecList.getUnitCreditSpecifications().add(getXMLUnitCreditSpecificationByItemNumber(unitCreditSpecificationQuery.getItemNumber(), unitCreditSpecificationQuery.getVerbosity()));
            } else {
                for (com.smilecoms.cm.db.model.UnitCreditSpecification dbUnitCreditSpec : DAO.getAllAvailableUnitCreditSpecifications(em)) {
                    xmlUnitCreditSpecList.getUnitCreditSpecifications().add(getXMLUnitCreditSpecification(dbUnitCreditSpec.getUnitCreditSpecificationId(), unitCreditSpecificationQuery.getVerbosity()));
                }
            }
            xmlUnitCreditSpecList.setNumberOfUnitCreditSpecifications(xmlUnitCreditSpecList.getUnitCreditSpecifications().size());
        } catch (Exception e) {
            throw processError(CMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return xmlUnitCreditSpecList;
    }

    @Override
    public Done updateProductInstance(ProductInstance productInstance) throws CMError {
        setContext(productInstance, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            com.smilecoms.cm.db.model.ProductInstance dbPrdInstance = em.find(com.smilecoms.cm.db.model.ProductInstance.class, productInstance.getProductInstanceId());

            DAO.createProductInstanceHistory(em, productInstance.getProductInstanceId());

            if (productInstance.getCustomerId() > 0) {
                // pass <= 0 to leave as-is
                dbPrdInstance.setCustomerProfileId(productInstance.getCustomerId());
            }
            if (productInstance.getOrganisationId() >= 0) {
                // pass -1 to leave as-is
                dbPrdInstance.setOrganisationId(productInstance.getOrganisationId());
            }
            if (productInstance.getSegment() != null && !productInstance.getSegment().isEmpty()) {
                dbPrdInstance.setSegment(productInstance.getSegment());
            }
            if (productInstance.getFriendlyName() != null && !productInstance.getFriendlyName().isEmpty()) {
                dbPrdInstance.setFriendlyName(productInstance.getFriendlyName());
            }
            if (productInstance.getReferralCode() != null && !productInstance.getReferralCode().isEmpty()) {
                dbPrdInstance.setReferralCode(productInstance.getReferralCode());
            }
            if (productInstance.getPhysicalId() != null && !productInstance.getPhysicalId().isEmpty()) {
                int logicalId = DAO.getLogicalIdForPhysicalId(em, productInstance.getPhysicalId());
                if (logicalId > 0 && dbPrdInstance.getLogicalId() > 0 && logicalId != dbPrdInstance.getLogicalId()) {
                    // You cannot set a physical Id on a product instance if its already been used previously on a PI with a different logical Id
                    throw new Exception("A physical Id cannot map to more than one logical Id");
                }
                dbPrdInstance.setPhysicalId(productInstance.getPhysicalId());
            }
            dbPrdInstance.setLastModified(new Date());
            em.persist(dbPrdInstance);
            em.flush();
            createEvent(productInstance.getProductInstanceId());
        } catch (Exception e) {
            throw processError(CMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done updateServiceInstance(ServiceInstance serviceInstance) throws CMError {
        setContext(serviceInstance, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }

        try {
            com.smilecoms.cm.db.model.ServiceInstance dbSvcInstance = em.find(com.smilecoms.cm.db.model.ServiceInstance.class, serviceInstance.getServiceInstanceId());

            boolean customerChange = false;

            if (serviceInstance.getCustomerId() > 0) {
                if (dbSvcInstance.getCustomerProfileId() != serviceInstance.getCustomerId()) {
                    customerChange = true;
                }
                dbSvcInstance.setCustomerProfileId(serviceInstance.getCustomerId());
            }
            if (serviceInstance.getRemoteResourceId() != null && !serviceInstance.getRemoteResourceId().isEmpty()) {
                dbSvcInstance.setRemoteResourceId(serviceInstance.getRemoteResourceId());
            }

            boolean accountChange = false;

            if (serviceInstance.getAccountId() > 0) {
                if (serviceInstance.getAccountId() != dbSvcInstance.getAccountId()) { // This is an account change
                    accountChange = true;
                }
                dbSvcInstance.setAccountId(serviceInstance.getAccountId());

            }

            int currentSvcSpecId = dbSvcInstance.getServiceSpecificationId();
            int newSvcSpecId = serviceInstance.getServiceSpecificationId();
            String previousInfo = dbSvcInstance.getInfo();
            String currentKYCStatus = Utils.getValueFromCRDelimitedAVPString(previousInfo, "KYCStatus");
            String currentKYCVerifyingMethod = Utils.getValueFromCRDelimitedAVPString(previousInfo, "KYCVerifyingMethod");
            String eKYCResults = null;
            String kycVerifyingMethod = null;
            String countryCode = null;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
            // Utils.getValueFromCRDelimitedAVPString(serviceInstance, "KYCVerifyingMethod");
            String newKYCStatus = null;
            String newNIDATransactionID = null;
            String newNIDAVerifiedOnDate = null;

            List<String> infoAVPs = new ArrayList<>();
            List<AVP> specAVPs = getServiceSpecAVPs(serviceInstance.getServiceSpecificationId());
            List<AVP> photoAVPs = new ArrayList();

            for (AVP avp : serviceInstance.getAVPs()) {
                for (AVP specAVP : specAVPs) {

                    if (specAVP.getAttribute().equals(avp.getAttribute())) {

                        if (avp.getAttribute().equalsIgnoreCase("eKYCResults")) {
                            eKYCResults = "Unknown";
                            continue;
                        }

                        if (specAVP.getInputType().equals("info")) { // Exclude eKYCResults here since we much first call into NIDA                           
                            infoAVPs.add(avp.getAttribute() + "=" + avp.getValue());
                            if (avp.getAttribute().equals("KYCStatus")) {
                                newKYCStatus = avp.getValue();
                            }

                            if (avp.getAttribute().equals("NIDATransactionId")) {
                                newNIDATransactionID = avp.getValue();
                            }

                            if (avp.getAttribute().equals("NIDAVerifiedOnDate")) {
                                newNIDAVerifiedOnDate = avp.getValue();
                            }

                            if (avp.getAttribute().equals("KYCVerifyingMethod")) {
                                kycVerifyingMethod = avp.getValue();
                            }

                            if (avp.getAttribute().equals("CountryCode")) {
                                countryCode = avp.getValue();
                            }

                        } else if (specAVP.getInputType().equals("photo")) {
                            // Value is of form <filename>|<datainbase64>                            
                            int pipe = avp.getValue().indexOf("|");
                            if (pipe != -1) {
                                String guid = avp.getValue().substring(0, pipe);
                                infoAVPs.add(avp.getAttribute() + "=" + guid);
                                photoAVPs.add(avp);
                            } else if (!avp.getValue().isEmpty()) {
                                infoAVPs.add(avp.getAttribute() + "=" + avp.getValue());
                            }
                        }
                        break;
                    }
                }
            }

            if (serviceInstance.getStatus() != null && !serviceInstance.getStatus().isEmpty()) {
                dbSvcInstance.setStatus(serviceInstance.getStatus());
            }

            log.debug("InfoAVPS - Step 0: [{}]", Utils.makeCRDelimitedStringFromList(infoAVPs));
            //Check if we must veryify customer with NIDA? (for TZ)
            if (eKYCResults != null && kycVerifyingMethod != null
                    && kycVerifyingMethod.equals("NIDA Pilot")
                    && !accountChange
                    && !customerChange
                    && BaseUtils.getBooleanProperty("env.customer.verify.with.nida", false)) {

                if (newKYCStatus != null
                        && newKYCStatus.equalsIgnoreCase("Complete")
                        && newNIDATransactionID != null
                        && !newNIDATransactionID.isEmpty()
                        && newNIDAVerifiedOnDate != null
                        && !newNIDAVerifiedOnDate.isEmpty()) {
                    newKYCStatus = "Complete";
                    infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "NIDATransactionId", newNIDATransactionID));
                    infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "NIDAVerifiedOnDate", newNIDAVerifiedOnDate));

                } else if (currentKYCStatus != null
                        && newKYCStatus != null
                        && currentKYCVerifyingMethod != null
                        && currentKYCStatus.equalsIgnoreCase("Complete")
                        && newKYCStatus.equalsIgnoreCase("Complete")
                        && currentKYCVerifyingMethod.equals("NIDA Pilot")) {
                    newKYCStatus = "Complete"; //If the service's KYCStatus is already NIDA complete
                    infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "NIDATransactionId", newNIDATransactionID));
                    infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "NIDAVerifiedOnDate", newNIDAVerifiedOnDate));

                } else {

                    newKYCStatus = "Unvalidated";

                    com.smilecoms.commons.sca.Customer customer = SCAWrapper.getAdminInstance().getCustomer(serviceInstance.getCustomerId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS);
                    //Ensure customer is verifying with NIDA allowed id types.
                    // AllowedIdTypesRegex=voteridentitycard|nationalid
                    // If this is the first product and customer is nida verified, then do not verify again

                    log.warn("On update service instance [{}] - customer.productInstancesTotalCount [{}], customer.getKYCStatus [{}]",
                            new Object[]{serviceInstance.getServiceInstanceId(), customer.getProductInstancesTotalCount(),
                                customer.getKYCStatus()});

                    byte[] fingerPrintData = null;
                    // Check if fingerprint is attached.
                    boolean fingerprintSupplied = false;
                    for (AVP avp : photoAVPs) {
                        if (avp.getAttribute().equalsIgnoreCase("fingerprints")) {
                            fingerprintSupplied = true;
                            int pipe = avp.getValue().indexOf("|");
                            fingerPrintData = Utils.decodeBase64(avp.getValue().substring(pipe + 1));
                            break;
                        }
                    }

                    //Check if the photos are supplied.
                    if (!fingerprintSupplied && !accountChange && !customerChange) {
                        throw new Exception("NIDA Pilot verifying method was selected but no fingerprints supplied.");
                    }

                    String allowedNidaKycIdTypes = BaseUtils.getSubProperty("env.nida.config", "AllowedIdTypesRegex");

                    if (!Utils.matchesWithPatternCache(customer.getIdentityNumberType(), allowedNidaKycIdTypes)) {
                        throw new Exception("Id type not enabled for eKyc verification -- customer ["
                                + customer.getCustomerId() + "] is using id type [" + customer.getIdentityNumberType() + "].");
                    }

                    //Check if this fingerprint has been used before?
                    DocumentUniquenessQuery documentUniquenessQuery = new DocumentUniquenessQuery();
                    documentUniquenessQuery.setDocumentHash(HashUtils.md5(fingerPrintData));
                    //An error will be thrown if document has been used before.
                    SCAWrapper.getUserSpecificInstance().checkDocumentUniqueness_Direct(documentUniquenessQuery);

                    NidaVerifyQuery nvq = new NidaVerifyQuery();
                    nvq.setIdentityNumber(customer.getIdentityNumber());
                    nvq.setFingerprintB64(fingerPrintData);
                    com.smilecoms.commons.sca.direct.im.PlatformContext pCx = new com.smilecoms.commons.sca.direct.im.PlatformContext();
                    nvq.setVerifiedBy(serviceInstance.getPlatformContext().getOriginatingIdentity());
                    nvq.setPlatformContext(pCx);
                    nvq.setEntityId(Integer.toString(dbSvcInstance.getServiceInstanceId()));
                    nvq.setEntityType("UPDATESIM");
                    // Will throw exception if it fails
                    try {
                        NidaVerifyReply nidaResponse = SCAWrapper.getAdminInstance().verifyCustomerWithNIDA_Direct(nvq);
                        if (BaseUtils.getBooleanProperty("env.customer.address.update.enable", false)) {
                            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
                            // Get the two dates to be compared
                            String dateString = sdf1.format(customer.getCreatedDateTime().toGregorianCalendar().getTime());
                            Date d1 = sdf1.parse(dateString);
                            Date d2 = sdf1.parse(BaseUtils.getProperty("env.date.before.update.address"));
//                               Date d2 = customer.getCreatedDateTime().toGregorianCalendar();
                            if (d1.compareTo(d2) < 0) {
                                log.info("Updating of customer address");
                                Address addrPhy = new Address();
                                addrPhy.setCustomerId(customer.getCustomerId());
                                addrPhy.setLine1(nidaResponse.getResidentHouseNo());
                                addrPhy.setLine2(nidaResponse.getResidentStreet() + "," + nidaResponse.getResidentWard()
                                        + "," + nidaResponse.getResidentVillage());
                                addrPhy.setZone(nidaResponse.getResidentWard());
                                addrPhy.setTown(nidaResponse.getResidentDistrict());
                                addrPhy.setCode(nidaResponse.getResidentPostCode());
                                addrPhy.setState(nidaResponse.getResidentRegion());
                                addrPhy.setCountry(BaseUtils.getProperty("env.country.name"));
                                DAO.updateCustomerAddress(em, addrPhy);
                            }
                        }
                        newKYCStatus = "Complete";
                        infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "NIDATransactionId", nidaResponse.getNIDATransactionId()));
                        infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "NIDAVerifiedOnDate", sdf.format(new Date())));
                        infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "eKYCResults", "SUCCESSFUL"));
                        //infoAVPs.add("eKYCResults" + "=SUCCESSFUL");
                    } catch (Exception ex) {
                        // newKYCStatus = "Unvalidated";
                        // infoAVPs.add("eKYCResults" + "=FAILED");
                        throw ex;
                    }
                }
                // Set newstatus in the new infoAVPs as well;
                infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "KYCStatus", newKYCStatus));
            }

            if (eKYCResults != null && kycVerifyingMethod != null
                    && kycVerifyingMethod.equals("Immigration")
                    && !accountChange
                    && !customerChange
                    && BaseUtils.getBooleanProperty("env.customer.verify.with.immigration", false)) {

                if (newKYCStatus != null
                        && newKYCStatus.equalsIgnoreCase("Complete")) {
                    newKYCStatus = "Complete";
                    infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "ImmigrationVerifiedOnDate", newNIDAVerifiedOnDate));
                } else if (currentKYCStatus != null
                        && newKYCStatus != null
                        && currentKYCVerifyingMethod != null
                        && currentKYCStatus.equalsIgnoreCase("Complete")
                        && newKYCStatus.equalsIgnoreCase("Complete")
                        && currentKYCVerifyingMethod.equals("Immigration")) {
                    newKYCStatus = "Complete"; //If the service's KYCStatus is already NIDA complete
                    infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "ImmigrationVerifiedOnDate", newNIDAVerifiedOnDate));
                } else {

                    newKYCStatus = "Unvalidated";

                    com.smilecoms.commons.sca.Customer customer = SCAWrapper.getAdminInstance().getCustomer(serviceInstance.getCustomerId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS);
                    //Ensure customer is verifying with Immigaration allowed id types.
                    // AllowedIdTypesRegex=voteridentitycard|nationalid
                    // If this is the first product and customer is nida verified, then do not verify again

                    log.warn("On update service instance [{}] - customer.productInstancesTotalCount [{}], customer.getKYCStatus [{}]",
                            new Object[]{serviceInstance.getServiceInstanceId(), customer.getProductInstancesTotalCount(),
                                customer.getKYCStatus()});

                    byte[] fingerPrintData = null;
                    // Check if fingerprint is attached.
                    boolean fingerprintSupplied = false;
                    for (AVP avp : photoAVPs) {
                        if (avp.getAttribute().equalsIgnoreCase("fingerprints")) {
                            fingerprintSupplied = true;
                            int pipe = avp.getValue().indexOf("|");
                            fingerPrintData = Utils.decodeBase64(avp.getValue().substring(pipe + 1));
                            break;
                        }
                    }

                    //Check if the photos are supplied.
                    if (!fingerprintSupplied && !accountChange && !customerChange) {
                        throw new Exception("Immigaration verifying method was selected but no fingerprints supplied.");
                    }

                    String allowedNidaKycIdTypes = BaseUtils.getSubProperty("env.verify.foreigner.config", "AllowedIdTypesRegex");

                    if (!Utils.matchesWithPatternCache(customer.getIdentityNumberType(), allowedNidaKycIdTypes)) {
                        throw new Exception("Id type not enabled for eKyc verification -- customer ["
                                + customer.getCustomerId() + "] is using id type [" + customer.getIdentityNumberType() + "].");
                    }

                    //Check if this fingerprint has been used before?
                    DocumentUniquenessQuery documentUniquenessQuery = new DocumentUniquenessQuery();
                    documentUniquenessQuery.setDocumentHash(HashUtils.md5(fingerPrintData));
                    //An error will be thrown if document has been used before.
                    SCAWrapper.getUserSpecificInstance().checkDocumentUniqueness_Direct(documentUniquenessQuery);

                    VerifyForeignerQuery verifyForeignerQuery = new VerifyForeignerQuery();
                    verifyForeignerQuery.setDocumentNo(customer.getIdentityNumber());
                    verifyForeignerQuery.setCountryCode(countryCode);
                    verifyForeignerQuery.setFingerprintB64(fingerPrintData);
                    verifyForeignerQuery.setVerifiedBy(serviceInstance.getPlatformContext().getOriginatingIdentity());
                    verifyForeignerQuery.setEntityId(Integer.toString(dbSvcInstance.getServiceInstanceId()));
                    verifyForeignerQuery.setEntityType("UPDATESIM");
                    // Will throw exception if it fails
                    try {
                        VerifyForeignerReply verifyForeignerReply = SCAWrapper.getUserSpecificInstance().verifyForeignerCustomer_Direct(verifyForeignerQuery);
                        if (verifyForeignerReply.getStatus().equalsIgnoreCase("0")) {
                            if (BaseUtils.getBooleanProperty("env.customer.address.update.enable", false)) {
                                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
                                // Get the two dates to be compared
                                String dateString = sdf1.format(customer.getCreatedDateTime().toGregorianCalendar().getTime());
                                Date d1 = sdf1.parse(dateString);
                                Date d2 = sdf1.parse(BaseUtils.getProperty("env.date.before.update.address"));
                                if (d1.compareTo(d2) < 0) {
                                    log.info("Updating of customer address");
                                    Address addrPhy = new Address();
                                    addrPhy.setCustomerId(customer.getCustomerId());
                                    addrPhy.setLine1(verifyForeignerReply.getResidentRegion());
                                    addrPhy.setLine2(verifyForeignerReply.getResidentRegion() + "," + verifyForeignerReply.getResidentWard()
                                            + "," + verifyForeignerReply.getResidentDistrict());
                                    addrPhy.setZone(verifyForeignerReply.getResidentWard());
                                    addrPhy.setTown(verifyForeignerReply.getResidentDistrict());
                                    addrPhy.setCode("");
                                    addrPhy.setState(verifyForeignerReply.getResidentRegion());
                                    addrPhy.setCountry(BaseUtils.getProperty("env.country.name"));
                                    DAO.updateCustomerAddress(em, addrPhy);
                                }
                            }
                            newKYCStatus = "Complete";
                            infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "ImmigrationVerifiedOnDate", sdf.format(new Date())));
                            infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "eKYCResults", "SUCCESSFUL"));
                            //infoAVPs.add("eKYCResults" + "=SUCCESSFUL");
                        } else {
                            newKYCStatus = "Unvalidated";
                            infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "ImmigrationVerifiedOnDate", sdf.format(new Date())));
                            infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "eKYCResults", "UNSUCCESSFUL"));
                        }
                    } catch (Exception ex) {
                        throw ex;
                    }
                }
                // Set newstatus in the new infoAVPs as well;
                infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "KYCStatus", newKYCStatus));
            }

            if (kycVerifyingMethod != null && kycVerifyingMethod.equals("Normal")
                    && BaseUtils.getBooleanProperty("env.customer.verify.with.nida", false)) { // This is for TZ - they asked a dropdown for KYCVerifyingMethod- should not affect NG and UG.
                boolean photoSupplied = false;
                for (AVP avp : photoAVPs) {
                    if (avp.getAttribute().equalsIgnoreCase("subscriberform")) {
                        photoSupplied = true;
                        break;
                    }
                }

                //Check if the photos are supplied.
                if (!photoSupplied && !accountChange && !customerChange) {
                    throw new Exception("Normal KYC verifying method was selected but no document attached.");
                }
            }

            String completeRegex = BaseUtils.getProperty("env.kyc.complete.status.regex", "^Complete.*$");

            if (BaseUtils.getProperty("env.kyc.level", "service").equalsIgnoreCase("service")
                    && currentKYCStatus != null && newKYCStatus != null
                    && !Utils.matchesWithPatternCache(currentKYCStatus, completeRegex)
                    && Utils.matchesWithPatternCache(newKYCStatus, completeRegex)) {
                log.debug("KYC is now complete. Setting status to AC on all SIs in the product instance");
                dbSvcInstance.setStatus("AC");
                DAO.reactivateAllSIsInProductInstance(em, dbSvcInstance.getProductInstanceId());
                if (BaseUtils.getBooleanProperty("env.customer.verify.success.notification.enable", false)) {
                    com.smilecoms.commons.sca.Customer customer = SCAWrapper.getAdminInstance().getCustomer(serviceInstance.getCustomerId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS);
                    int cpId = customer.getCustomerId();
                    int piId = dbSvcInstance.getProductInstanceId();
                    Set<String> eventSet = BaseUtils.getPropertyAsSet("env.customer.lifecycle.notification.set");
                    for (String eventTypes : eventSet) {
                        String verifyingMethod = eventTypes.split("\\|")[0];
                        String subType = eventTypes.split("\\|")[1];
                        if (verifyingMethod.equalsIgnoreCase(kycVerifyingMethod)) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("PIId=").append(piId).append("\r\n");
                            sb.append("CustId=").append(cpId).append("\r\n");
                            PlatformEventManager.createEvent("CL_UC", subType,
                                    String.valueOf(cpId),
                                    sb.toString(),
                                    "CL_UC_" + subType + "_" + cpId + "_" + piId + "_" + new Date());
                        }
                    }
                }
            }

            Calendar eventDelay = Calendar.getInstance();
            eventDelay.add(Calendar.SECOND, 5);
            // Dont send more than one per minute per SI
            String key = "SI_CHANGE_" + dbSvcInstance.getServiceInstanceId() + "_" + Utils.getDateAsString(new Date(), "yyyyMMddHHmm", null);

            String newInfo = Utils.makeCRDelimitedStringFromList(infoAVPs);
            log.debug("InfoAVPS - Step 3: [{}]", Utils.makeCRDelimitedStringFromList(infoAVPs));
            dbSvcInstance.setInfo(newInfo);
            DAO.setServiceInstancePhotographs(em, dbSvcInstance.getServiceInstanceId(), photoAVPs);
            boolean triggeredServiceSpecChange = false;
            if (newSvcSpecId > 0 && currentSvcSpecId != newSvcSpecId) {

                log.debug("This is a service specification change. Current spec is [{}] and new spec is [{}]", currentSvcSpecId, newSvcSpecId);
                dbSvcInstance.setInfo(addOrModifyInfoAVP("PreviousSvcSpec", String.valueOf(currentSvcSpecId), dbSvcInstance.getInfo()));

                dbSvcInstance.setServiceSpecificationId(newSvcSpecId);
                // We must store the previous svc info in a field called PreviousInfo so that it can be set back to what it was if new svc spec id is passed as -1
                if (previousInfo != null && !previousInfo.isEmpty()) {
                    previousInfo = removeInfoAVP("PreviousInfo", previousInfo); // Remove the previousinfo value if there is one
                    dbSvcInstance.setInfo(addOrModifyInfoAVP("PreviousInfo", previousInfo.replaceAll("\r\n", "_WASCRLF_"), dbSvcInstance.getInfo()));

                }
                PlatformEventManager.createEvent("CM", "ServiceSpecChange", String.valueOf(dbSvcInstance.getServiceInstanceId()),
                        String.valueOf(dbSvcInstance.getServiceInstanceId()) + "|" + currentSvcSpecId + "|" + newSvcSpecId, key, eventDelay.getTime());
                triggeredServiceSpecChange = true;
            }

            if (newSvcSpecId == -1) {
                String lastSpec = getValueFromInfoAVPString(dbSvcInstance.getInfo(), "PreviousSvcSpec");
                String lastInfo = getValueFromInfoAVPString(dbSvcInstance.getInfo(), "PreviousInfo");

                log.debug("This is a request to set the service specification back to what it last was. It was [{}]. Previous info was [{}]", lastSpec, lastInfo);
                if (lastSpec != null) {
                    dbSvcInstance.setInfo(addOrModifyInfoAVP("PreviousSvcSpec", String.valueOf(currentSvcSpecId), ""));
                    dbSvcInstance.setServiceSpecificationId(Integer.parseInt(lastSpec));
                    if (lastInfo != null && !lastInfo.isEmpty()) {
                        dbSvcInstance.setInfo(dbSvcInstance.getInfo() + "\r\n" + lastInfo.replaceAll("_WASCRLF_", "\r\n"));
                    }
                    PlatformEventManager.createEvent("CM", "ServiceSpecChange", String.valueOf(dbSvcInstance.getServiceInstanceId()),
                            String.valueOf(dbSvcInstance.getServiceInstanceId()) + "|" + currentSvcSpecId + "|" + lastSpec, key, eventDelay.getTime());
                    triggeredServiceSpecChange = true;
                } else {
                    log.debug("This service has never changed service specification so it cannot change back to anything");
                }
            }

            //compare newinfo with previousinfo - if different then trigger a PCRF change request only if one hasn't already been triggered
            if (previousInfo != null && !previousInfo.equals(newInfo) && !triggeredServiceSpecChange) {
                log.debug("The info AVPs have changed so trigger ServiceInfoChange event");
                PlatformEventManager.createEvent(
                        "CM",
                        "ServiceInfoChange",
                        String.valueOf(dbSvcInstance.getServiceInstanceId()),
                        String.valueOf(dbSvcInstance.getServiceInstanceId()),
                        key,
                        eventDelay.getTime()
                );
            }

            DAO.persistServiceInstance(em, dbSvcInstance);

            createEvent(serviceInstance.getServiceInstanceId());
            if (BaseUtils.getBooleanProperty("env.customer.visa.update.notification.enable", false)) {
                log.debug("env.customer.visa.update.notification.enable");
                com.smilecoms.commons.sca.Customer customer = SCAWrapper.getAdminInstance().getCustomer(dbSvcInstance.getCustomerProfileId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER);
                int cpId = customer.getCustomerId();
                int piId = dbSvcInstance.getProductInstanceId();
                if (customer.getVisaExpiryDate() != null && !customer.getVisaExpiryDate().isEmpty()) {
                    if (!customer.getVisaExpiryDate().equalsIgnoreCase("For-Life")) {
                        BigDecimal statusCount = DAO.doesCustomerVisaHasUpdated(em, dbSvcInstance.getProductInstanceId());
                        if (statusCount != null) {
                            if (statusCount.equals(BigDecimal.ZERO)) {
                                log.debug("statusCount " + statusCount);
                                Set<String> eventSet = BaseUtils.getPropertyAsSet("env.customer.visa.update.notification.set");
                                for (String eventType : eventSet) {
                                    String subType = eventType;
                                    log.debug("Creating Event On Update of Visa");
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("PIId=").append(piId).append("\r\n");
                                    sb.append("CustId=").append(cpId).append("\r\n");
                                    PlatformEventManager.createEvent("CL_UC", subType,
                                            String.valueOf(cpId),
                                            sb.toString(),
                                            "CL_UC_" + subType + "_" + cpId + "_" + piId + "_" + new Date());
                                    log.debug("Event Created");
                                }
                            }
                        }
                    }
                }
            }

            if (currentKYCStatus != null && newKYCStatus != null && !currentKYCStatus.equals(newKYCStatus)) {
                log.debug("KYC status change from [{}] to [{}]", currentKYCStatus, newKYCStatus);
                PlatformEventManager.createEvent("CM", "KYCChange", String.valueOf(dbSvcInstance.getServiceInstanceId()),
                        currentKYCStatus + "|" + newKYCStatus + "|" + serviceInstance.getPlatformContext().getOriginatingIdentity() + "|" + dbSvcInstance.getServiceInstanceId());
            }

        } catch (Exception e) {
            throw processError(CMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }

        return makeDone();
    }

    @Override
    public Done deleteServiceInstance(PlatformInteger serviceInstanceIdToDelete) throws CMError {
        setContext(serviceInstanceIdToDelete, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }

        try {
            com.smilecoms.cm.db.model.ServiceInstance dbSvcInstance = em.find(com.smilecoms.cm.db.model.ServiceInstance.class, serviceInstanceIdToDelete.getInteger());
            dbSvcInstance.setStatus(SERVICE_INSTANCE_STATUS.DE.toString());
            dbSvcInstance.setLastModified(new Date());
            em.persist(dbSvcInstance);
            em.flush();
            createEvent(serviceInstanceIdToDelete.getInteger());
        } catch (Exception e) {
            throw processError(CMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done deleteProductInstance(PlatformInteger productInstanceIdToDelete) throws CMError {
        setContext(productInstanceIdToDelete, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }

        try {
            if (!DAO.getServiceInstancesByProductInstanceId(em, productInstanceIdToDelete.getInteger()).isEmpty()) {
                throw new Exception("Cannot delete a product instance that still has service instances");
            }

            com.smilecoms.cm.db.model.ProductInstance dbPrdInstance = em.find(com.smilecoms.cm.db.model.ProductInstance.class, productInstanceIdToDelete.getInteger());

            DAO.createProductInstanceHistory(em, dbPrdInstance.getProductInstanceId());

            dbPrdInstance.setStatus(PRODUCT_INSTANCE_STATUS.DE.toString());
            dbPrdInstance.setLastModified(new Date());
            em.persist(dbPrdInstance);

            DAO.deleteWaitingCampaignsForProductInstance(em, productInstanceIdToDelete.getInteger());

            em.flush();

            if (Utils.isDateInTimeframe(dbPrdInstance.getCreatedDatetime(), 5, Calendar.SECOND)) {
                log.warn("Product instance just added so deleting fully");
                em.remove(dbPrdInstance);
                em.flush();
            }
            createEvent(productInstanceIdToDelete.getInteger());
        } catch (Exception e) {
            throw processError(CMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done storeCampaignData(CampaignData campaignData) throws CMError {
        setContext(campaignData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        // As a start we support loading a list of product instance Id's into the campaign_list table
        try {
            int campId = campaignData.getCampaignId();
            String base64zippedCommaDelimitedPIs = campaignData.getProductInstanceIds();
            if (campId == 0 || base64zippedCommaDelimitedPIs == null) {
                throw new Exception("Missing campaign id or product instance id list");
            }
            String commaDelimitedPIs = Utils.unzip(base64zippedCommaDelimitedPIs);
            DAO.createCampaignListEntries(em, campId, Utils.getListFromCommaDelimitedString(commaDelimitedPIs));
            createEvent(campaignData.getCampaignId());
        } catch (Exception e) {
            throw processError(CMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public ServiceSpecification getServiceSpecification(ServiceSpecificationQuery serviceSpecificationQuery) throws CMError {
        setContext(serviceSpecificationQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        ServiceSpecification svcSpec = null;
        com.smilecoms.cm.db.model.ServiceSpecification dbSvcSpec;

        try {

            // 1. Get Service Specification header details 
            dbSvcSpec = DAO.getServiceSpecificationById(em, serviceSpecificationQuery.getServiceSpecificationId());
            // 2. Construct the WebService return object
            svcSpec = getXMLServiceSpecification(dbSvcSpec, serviceSpecificationQuery.getVerbosity());

        } catch (Exception e) {
            throw processError(CMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return svcSpec;
    }

    @Override
    public ProductSpecificationList getProductSpecifications(ProductSpecificationQuery productSpecificationQuery) throws CMError {
        setContext(productSpecificationQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        ProductSpecificationList xmlProdSpecList = new ProductSpecificationList();
        try {
            if (productSpecificationQuery.getProductSpecificationId() != -1) {
                xmlProdSpecList.getProductSpecifications().add(getXMLProductSpecification(productSpecificationQuery.getProductSpecificationId(), productSpecificationQuery.getVerbosity()));
            } else {
                Collection<com.smilecoms.cm.db.model.ProductSpecification> dbProdSpecList = DAO.getAllProductSpecifications(em);
                for (com.smilecoms.cm.db.model.ProductSpecification dbProdSpec : dbProdSpecList) {
                    xmlProdSpecList.getProductSpecifications().add(getXMLProductSpecification(dbProdSpec.getProductSpecificationId(), productSpecificationQuery.getVerbosity()));
                }
            }
            xmlProdSpecList.setNumberOfProductSpecifications(xmlProdSpecList.getProductSpecifications().size());
        } catch (Exception e) {
            throw processError(CMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return xmlProdSpecList;
    }

    @Override
    public ServiceInstance createServiceInstance(ServiceInstance xmlServiceInstance) throws CMError {
        setContext(xmlServiceInstance, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        try {

            com.smilecoms.cm.db.model.ServiceInstance dbServiceInstance = new com.smilecoms.cm.db.model.ServiceInstance();
            String kycStatus = null;
            dbServiceInstance.setCustomerProfileId(xmlServiceInstance.getCustomerId());
            dbServiceInstance.setProductInstanceId(xmlServiceInstance.getProductInstanceId());
            if (xmlServiceInstance.getRemoteResourceId() == null || xmlServiceInstance.getRemoteResourceId().isEmpty()) {
                // generate our own resource id
                xmlServiceInstance.setRemoteResourceId(Utils.getUUID());
            }

            dbServiceInstance.setRemoteResourceId(xmlServiceInstance.getRemoteResourceId());
            dbServiceInstance.setServiceSpecificationId(xmlServiceInstance.getServiceSpecificationId());
            dbServiceInstance.setAccountId(xmlServiceInstance.getAccountId());
            dbServiceInstance.setStatus(xmlServiceInstance.getStatus() == null ? SERVICE_INSTANCE_STATUS.AC.toString() : xmlServiceInstance.getStatus());
            dbServiceInstance.setCreatedDatetime(new Date());
            dbServiceInstance.setCreatedByCustomerProfileId(DAO.getCustomerProfileIdBySSOIdentity(em, xmlServiceInstance.getPlatformContext().getOriginatingIdentity()));
            List<String> infoAVPs = new ArrayList<>();
            List<AVP> photoAVPs = new ArrayList();
            List<AVP> specAVPs = getServiceSpecAVPs(xmlServiceInstance.getServiceSpecificationId());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");

            String eKYCResults = null;
            String kycVerifyingMethod = null;
            String countryCode = null;

            for (AVP avp : xmlServiceInstance.getAVPs()) {
                log.debug("Create service instance got an AVP [{}]:[{}]", avp.getAttribute(), avp.getValue());
                for (AVP specAVP : specAVPs) {
                    if (specAVP.getAttribute().equals(avp.getAttribute())) {

                        if (avp.getAttribute().equalsIgnoreCase("eKYCResults")) {
                            eKYCResults = "Unknown";
                            continue;
                        }

                        if (specAVP.getInputType().equals("info") && !avp.getAttribute().equalsIgnoreCase("eKYCResults")) {
                            log.debug("This is an info AVP so its being written into the service instance table");
                            infoAVPs.add(avp.getAttribute() + "=" + avp.getValue());

                            if (avp.getAttribute().equals("KYCVerifyingMethod")) {
                                kycVerifyingMethod = avp.getValue();
                            }

                            if (avp.getAttribute().equals("CountryCode")) {
                                countryCode = avp.getValue();
                            }

                        } else if (specAVP.getInputType().equals("photo")) {
                            // Value is of form <filename>|<datainbase64>
                            int pipe = avp.getValue().indexOf("|");
                            if (pipe > 0) {
                                String guid = avp.getValue().substring(0, pipe);
                                infoAVPs.add(avp.getAttribute() + "=" + guid);
                                photoAVPs.add(avp);
                            }
                        }
                        break;
                    }
                }
            }

            //Check if we must veryify customer with NIDA? (for TZ - eKYC) -- Doing this on the  SIM service only
            if (eKYCResults != null && kycVerifyingMethod != null
                    && kycVerifyingMethod.equals("NIDA Pilot")) {

                if (BaseUtils.getBooleanProperty("env.customer.verify.with.nida", false)) {
                    com.smilecoms.commons.sca.Customer customer = SCAWrapper.getAdminInstance().getCustomer(xmlServiceInstance.getCustomerId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS);

                    String allowedNidaKycIdTypes = BaseUtils.getSubProperty("env.nida.config", "AllowedIdTypesRegex");

                    if (!Utils.matchesWithPatternCache(customer.getIdentityNumberType(), allowedNidaKycIdTypes)) {
                        throw new Exception("Id type not enabled for eKyc verification -- customer ["
                                + customer.getCustomerId() + "] is using id type [" + customer.getIdentityNumberType() + "].");
                    }

                    log.warn("On create service instance [{}] - customer.productInstancesTotalCount [{}], customer.getKYCStatus [{}]",
                            new Object[]{dbServiceInstance.getServiceInstanceId(), customer.getProductInstancesTotalCount(),
                                customer.getKYCStatus()});

                    /* if((customer.getProductInstancesTotalCount() == null || customer.getProductInstancesTotalCount() == 0) 
                            && customer.getKYCStatus() != null  
                            && customer.getKYCStatus().equalsIgnoreCase("V")) { */
                    if (kycStatus != null && kycStatus.equalsIgnoreCase("Complete")) {
                        log.warn("On first product and customer is already NIDA verified.");
                        infoAVPs.add("eKYCResults" + "=Auto KYC Complete");
                        infoAVPs.add("KYCStatus=Complete");
                    } else {

                        byte[] fingerPrintData = null;

                        // Check if fingerprint is attached.
                        boolean fingerprintSupplied = false;
                        for (AVP avp : photoAVPs) {
                            if (avp.getAttribute().equalsIgnoreCase("fingerprints")) {
                                fingerprintSupplied = true;
                                int pipe = avp.getValue().indexOf("|");
                                fingerPrintData = Utils.decodeBase64(avp.getValue().substring(pipe + 1));
                                break;
                            }
                        }

                        //Check if the fingerprints are supplied.
                        if (!fingerprintSupplied || fingerPrintData == null) {
                            throw new Exception("NIDA Pilot verifying method was selected but no fingerprints supplied.");
                        }

                        //Check if this fingerprint has been used before?
                        DocumentUniquenessQuery documentUniquenessQuery = new DocumentUniquenessQuery();
                        documentUniquenessQuery.setDocumentHash(HashUtils.md5(fingerPrintData));
                        //An error will be thrown if document has been used before.
                        SCAWrapper.getUserSpecificInstance().checkDocumentUniqueness_Direct(documentUniquenessQuery);

                        NidaVerifyQuery nvq = new NidaVerifyQuery();
                        nvq.setIdentityNumber(customer.getIdentityNumber());
                        nvq.setFingerprintB64(fingerPrintData);
                        nvq.setVerifiedBy(xmlServiceInstance.getPlatformContext().getOriginatingIdentity());
                        // nvq.setEntityId(Integer.toString(dbServiceInstance.getServiceInstanceId()));
                        nvq.setEntityId(customer.getIdentityNumber());
                        nvq.setEntityType("NEWSIM");

                        // Will throw exception if it fails
                        NidaVerifyReply nidaResponse = SCAWrapper.getAdminInstance().verifyCustomerWithNIDA_Direct(nvq);
                        if (BaseUtils.getBooleanProperty("env.customer.address.update.enable", false)) {
                            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
                            // Get the two dates to be compared
                            String dateString = sdf1.format(customer.getCreatedDateTime().toGregorianCalendar().getTime());
                            Date d1 = sdf1.parse(dateString);
                            Date d2 = sdf1.parse(BaseUtils.getProperty("env.date.before.update.address"));
//                               Date d2 = customer.getCreatedDateTime().toGregorianCalendar();
                            if (d1.compareTo(d2) < 0) {
                                log.info("Updating of customer address");
                                Address addrPhy = new Address();
                                addrPhy.setCustomerId(customer.getCustomerId());
                                addrPhy.setLine1(nidaResponse.getResidentHouseNo());
                                addrPhy.setLine2(nidaResponse.getResidentStreet() + "," + nidaResponse.getResidentWard()
                                        + "," + nidaResponse.getResidentVillage());
                                addrPhy.setZone(nidaResponse.getResidentWard());
                                addrPhy.setTown(nidaResponse.getResidentDistrict());
                                addrPhy.setCode(nidaResponse.getResidentPostCode());
                                addrPhy.setState(nidaResponse.getResidentRegion());
                                addrPhy.setCountry(BaseUtils.getProperty("env.country.name"));
                                DAO.updateCustomerAddress(em, addrPhy);
                            }
                        }
                        infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "NIDATransactionId", nidaResponse.getNIDATransactionId()));
                        infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "NIDAVerifiedOnDate", sdf.format(new Date())));
                        infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "eKYCResults", "SUCCESSFUL"));
                        infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "KYCStatus", kycStatus));

                        kycStatus = "Complete";
                        infoAVPs.add("eKYCResults" + "=SUCCESSFUL");

                        infoAVPs.add("KYCStatus=" + kycStatus);
                    }

                    // Set newstatus in the new infoAVPs as well;
                    infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "KYCStatus", kycStatus));

                    String completeRegex = BaseUtils.getProperty("env.kyc.complete.status.regex", "^Complete.*$");

                    if (BaseUtils.getProperty("env.kyc.level", "service").equalsIgnoreCase("service")
                            && kycStatus != null
                            && Utils.matchesWithPatternCache(kycStatus, completeRegex)) {
                        log.debug("KYC is complete. Setting status to AC on all SIs in the product instance");
                        dbServiceInstance.setStatus(SERVICE_INSTANCE_STATUS.AC.toString());
                        if (BaseUtils.getBooleanProperty("env.customer.verify.with.nida.success.notification.enable", false)) {
                            int cpId = customer.getCustomerId();
                            int piId = dbServiceInstance.getProductInstanceId();
                            Set<String> subtypeList = BaseUtils.getPropertyAsSet("env.customer.lifecycle.nida.success.notification.set");
                            for (String subType : subtypeList) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("PIId=").append(piId).append("\r\n");
                                sb.append("CustId=").append(cpId).append("\r\n");
                                PlatformEventManager.createEvent("CL_UC", subType,
                                        String.valueOf(cpId),
                                        sb.toString(),
                                        "CL_UC_" + subType + "_" + cpId + "_" + piId + "_" + new Date());
                            }
                        }
                    } else {
                        dbServiceInstance.setStatus(SERVICE_INSTANCE_STATUS.TD.toString());
                        if (BaseUtils.getBooleanProperty("env.customer.verify.with.nida.failure.notification.enable", false)) {
                            int cpId = customer.getCustomerId();
                            int piId = dbServiceInstance.getProductInstanceId();
                            Set<String> subtypeList = BaseUtils.getPropertyAsSet("env.customer.lifecycle.nida.failure.notification.set");
                            for (String subType : subtypeList) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("PIId=").append(piId).append("\r\n");
                                sb.append("CustId=").append(cpId).append("\r\n");
                                PlatformEventManager.createEvent("CL_UC", subType,
                                        String.valueOf(cpId),
                                        sb.toString(),
                                        "CL_UC_" + subType + "_" + cpId + "_" + piId + "_" + new Date());
                            }
                        }
                    }

                }
            }

            //Check if we must veryify customer with Immigration? (for TZ - eKYC) -- Doing this on the  SIM service only
            if (eKYCResults != null && kycVerifyingMethod != null
                    && kycVerifyingMethod.equals("Immigration")) {

                if (BaseUtils.getBooleanProperty("env.customer.verify.with.immigration", false)) {
                    com.smilecoms.commons.sca.Customer customer = SCAWrapper.getAdminInstance().getCustomer(xmlServiceInstance.getCustomerId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS);

                    String allowedImmigrationKycIdTypes = BaseUtils.getSubProperty("env.verify.foreigner.config", "AllowedIdTypesRegex");

                    if (!Utils.matchesWithPatternCache(customer.getIdentityNumberType(), allowedImmigrationKycIdTypes)) {
                        throw new Exception("Id type not enabled for eKyc verification -- customer ["
                                + customer.getCustomerId() + "] is using id type [" + customer.getIdentityNumberType() + "].");
                    }

                    log.warn("On create service instance [{}] - customer.productInstancesTotalCount [{}], customer.getKYCStatus [{}]",
                            new Object[]{dbServiceInstance.getServiceInstanceId(), customer.getProductInstancesTotalCount(),
                                customer.getKYCStatus()});

                    /* if((customer.getProductInstancesTotalCount() == null || customer.getProductInstancesTotalCount() == 0) 
                            && customer.getKYCStatus() != null  
                            && customer.getKYCStatus().equalsIgnoreCase("V")) { */
                    if (kycStatus != null && kycStatus.equalsIgnoreCase("Complete")) {
                        log.warn("On first product and customer is already Immigration verified.");
                        infoAVPs.add("eKYCResults" + "=Auto KYC Complete");
                        infoAVPs.add("KYCStatus=Complete");
                    } else {

                        byte[] fingerPrintData = null;

                        // Check if fingerprint is attached.
                        boolean fingerprintSupplied = false;
                        for (AVP avp : photoAVPs) {
                            if (avp.getAttribute().equalsIgnoreCase("fingerprints")) {
                                fingerprintSupplied = true;
                                int pipe = avp.getValue().indexOf("|");
                                fingerPrintData = Utils.decodeBase64(avp.getValue().substring(pipe + 1));
                                break;
                            }
                        }

                        //Check if the fingerprints are supplied.
                        if (!fingerprintSupplied || fingerPrintData == null) {
                            throw new Exception("Immigration verifying method was selected but no fingerprints supplied.");
                        }

                        //Check if this fingerprint has been used before?
                        DocumentUniquenessQuery documentUniquenessQuery = new DocumentUniquenessQuery();
                        documentUniquenessQuery.setDocumentHash(HashUtils.md5(fingerPrintData));
                        //An error will be thrown if document has been used before.
                        SCAWrapper.getUserSpecificInstance().checkDocumentUniqueness_Direct(documentUniquenessQuery);

                        VerifyForeignerQuery verifyForeignerQuery = new VerifyForeignerQuery();
                        verifyForeignerQuery.setDocumentNo(customer.getIdentityNumber());
                        verifyForeignerQuery.setCountryCode(countryCode);
                        verifyForeignerQuery.setFingerprintB64(fingerPrintData);
                        verifyForeignerQuery.setVerifiedBy(xmlServiceInstance.getPlatformContext().getOriginatingIdentity());
                        verifyForeignerQuery.setEntityId("");
                        verifyForeignerQuery.setEntityType("NEWSIM");

                        VerifyForeignerReply verifyForeignerReply = SCAWrapper.getUserSpecificInstance().verifyForeignerCustomer_Direct(verifyForeignerQuery);
                        if (verifyForeignerReply.getStatus().equalsIgnoreCase("0")) {
                            if (BaseUtils.getBooleanProperty("env.customer.address.update.enable", false)) {
                                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
                                // Get the two dates to be compared
                                String dateString = sdf1.format(customer.getCreatedDateTime().toGregorianCalendar().getTime());
                                Date d1 = sdf1.parse(dateString);
                                Date d2 = sdf1.parse(BaseUtils.getProperty("env.date.before.update.address"));
                                if (d1.compareTo(d2) < 0) {
                                    log.info("Updating of customer address");
                                    Address addrPhy = new Address();
                                    addrPhy.setCustomerId(customer.getCustomerId());
                                    addrPhy.setLine1(verifyForeignerReply.getResidentRegion());
                                    addrPhy.setLine2(verifyForeignerReply.getResidentRegion() + "," + verifyForeignerReply.getResidentWard()
                                            + "," + verifyForeignerReply.getResidentDistrict());
                                    addrPhy.setZone(verifyForeignerReply.getResidentWard());
                                    addrPhy.setTown(verifyForeignerReply.getResidentDistrict());
                                    addrPhy.setCode("");
                                    addrPhy.setState(verifyForeignerReply.getResidentRegion());
                                    addrPhy.setCountry(BaseUtils.getProperty("env.country.name"));
                                    DAO.updateCustomerAddress(em, addrPhy);
                                }
                            }
                            infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "ImmigrationVerifiedOnDate", sdf.format(new Date())));
                            infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "eKYCResults", "SUCCESSFUL"));
                            infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "KYCStatus", kycStatus));
                            //infoAVPs.add("eKYCResults" + "=SUCCESSFUL");
                            kycStatus = "Complete";
                            infoAVPs.add("eKYCResults" + "=SUCCESSFUL");

                            infoAVPs.add("KYCStatus=" + kycStatus);
                        } else {
                            infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "ImmigrationVerifiedOnDate", sdf.format(new Date())));
                            infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "eKYCResults", "UNSUCCESSFUL"));
                            infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "KYCStatus", kycStatus));

                            kycStatus = "Unvalidated";
                            infoAVPs.add("eKYCResults" + "=UNSUCCESSFUL");

                            infoAVPs.add("KYCStatus=" + kycStatus);
                        }
                    }

                    // Set newstatus in the new infoAVPs as well;
                    infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "KYCStatus", kycStatus));

                    String completeRegex = BaseUtils.getProperty("env.kyc.complete.status.regex", "^Complete.*$");

                    if (BaseUtils.getProperty("env.kyc.level", "service").equalsIgnoreCase("service")
                            && kycStatus != null
                            && Utils.matchesWithPatternCache(kycStatus, completeRegex)) {
                        log.debug("KYC is complete. Setting status to AC on all SIs in the product instance");
                        dbServiceInstance.setStatus(SERVICE_INSTANCE_STATUS.AC.toString());
                        if (BaseUtils.getBooleanProperty("env.customer.verify.with.immigration.success.notification.enable", false)) {
                            int cpId = customer.getCustomerId();
                            int piId = dbServiceInstance.getProductInstanceId();
                            Set<String> subtypeList = BaseUtils.getPropertyAsSet("env.customer.lifecycle.immigration.success.notification.set");
                            for (String subType : subtypeList) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("PIId=").append(piId).append("\r\n");
                                sb.append("CustId=").append(cpId).append("\r\n");
                                PlatformEventManager.createEvent("CL_UC", subType,
                                        String.valueOf(cpId),
                                        sb.toString(),
                                        "CL_UC_" + subType + "_" + cpId + "_" + piId + "_" + new Date());
                            }
                        }
                    } else {
                        dbServiceInstance.setStatus(SERVICE_INSTANCE_STATUS.TD.toString());
                        if (BaseUtils.getBooleanProperty("env.customer.verify.with.immigration.failure.notification.enable", false)) {
                            int cpId = customer.getCustomerId();
                            int piId = dbServiceInstance.getProductInstanceId();
                            Set<String> subtypeList = BaseUtils.getPropertyAsSet("env.customer.lifecycle.immigration.failure.notification.set");
                            for (String subType : subtypeList) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("PIId=").append(piId).append("\r\n");
                                sb.append("CustId=").append(cpId).append("\r\n");
                                PlatformEventManager.createEvent("CL_UC", subType,
                                        String.valueOf(cpId),
                                        sb.toString(),
                                        "CL_UC_" + subType + "_" + cpId + "_" + piId + "_" + new Date());
                            }
                        }
                    }

                }
            }

            if (kycVerifyingMethod != null && kycVerifyingMethod.equals("Normal")) {
                boolean photoSupplied = false;
                for (AVP avp : photoAVPs) {
                    if (avp.getAttribute().equalsIgnoreCase("subscriberform")) {
                        photoSupplied = true;
                        break;
                    }
                }

                //Check if the photos are supplied.
                if (!photoSupplied) {
                    throw new Exception("Normal KYC verifying method was selected but no document attached.");
                }

                // Set newstatus in the new infoAVPs as well;
                kycStatus = "Unvalidated";
                infoAVPs = Utils.getListFromCRDelimitedString(Utils.setValueInCRDelimitedAVPString(Utils.makeCRDelimitedStringFromList(infoAVPs), "KYCStatus", kycStatus));
            }

            dbServiceInstance.setInfo(Utils.makeCRDelimitedStringFromList(infoAVPs));
            dbServiceInstance = DAO.persistServiceInstance(em, dbServiceInstance);
            DAO.setServiceInstancePhotographs(em, dbServiceInstance.getServiceInstanceId(), photoAVPs);

            xmlServiceInstance = getXMLServiceInstance(dbServiceInstance, "MAIN");

            createEvent(dbServiceInstance.getServiceInstanceId());
            if (BaseUtils.getBooleanProperty("env.customer.sim.successfully.registered.enable", false)) {
                if (dbServiceInstance.getStatus().equalsIgnoreCase("AC") && dbServiceInstance.getServiceSpecificationId() == 1) {
                    int cpId = dbServiceInstance.getCustomerProfileId();
                    int piId = dbServiceInstance.getProductInstanceId();
                    Set<String> subtypeList = BaseUtils.getPropertyAsSet("env.customer.sim.successfully.registered.notification.set");
                    for (String subType : subtypeList) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("PIId=").append(piId).append("\r\n");
                        sb.append("CustId=").append(cpId).append("\r\n");
                        PlatformEventManager.createEvent("CL_UC", subType,
                                String.valueOf(cpId),
                                sb.toString(),
                                "CL_UC_" + subType + "_" + cpId + "_" + piId + "_" + new Date());
                    }
                }
            }
        } catch (Exception e) {
            throw processError(CMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return xmlServiceInstance;
    }

    private class MappingValidationData {

        public int specId;
        public int minAllowed;
        public int maxAllowed;
        public int groupId;
        public int instanceCount;
    }

    private Map<Integer, MappingValidationData> getValidationMap(com.smilecoms.cm.db.model.ProductSpecification ps) {
        log.debug("Populating mapping validation data with the config of the product spec with id [{}]", ps.getProductSpecificationId());
        Map<Integer, MappingValidationData> validationMap = new HashMap<>();
        for (ProductServiceMapping psm : ps.getProductServiceMappingCollection()) {
            MappingValidationData mvd = new MappingValidationData();
            mvd.groupId = psm.getGroupId();
            mvd.maxAllowed = psm.getMaxServiceOccurences();
            mvd.minAllowed = psm.getMinServiceOccurences();
            mvd.instanceCount = 0;
            mvd.specId = psm.getProductServiceMappingPK().getServiceSpecificationId();
            validationMap.put(psm.getProductServiceMappingPK().getServiceSpecificationId(), mvd);
        }
        return validationMap;
    }

    private void checkForValidationIssues(Map<Integer, MappingValidationData> validationMap, OrderData orderData) throws Exception {
        if (validationMap == null) {
            return;
        }

        for (MappingValidationData vd : validationMap.values()) {
            if (vd.instanceCount > vd.maxAllowed && vd.groupId == 0) {
                throw new Exception("Too many service instances -- Spec Id:" + vd.specId + " Min:" + vd.minAllowed + " Max:" + vd.maxAllowed + " Actual:" + vd.instanceCount + " PIID:" + orderData.getProductInstanceId());
            }
            if (vd.instanceCount < vd.minAllowed && vd.groupId == 0) {
                throw new Exception("Too few service instances -- Spec Id:" + vd.specId + " Min:" + vd.minAllowed + " Max:" + vd.maxAllowed + " Actual:" + vd.instanceCount + " PIID:" + orderData.getProductInstanceId());
            }
            if (vd.groupId > 0) { // group id of > 0 means its a group
                int cnt = vd.instanceCount;
                for (MappingValidationData vdGrpCheck : validationMap.values()) {
                    if (vdGrpCheck.groupId == vd.groupId && vdGrpCheck.specId != vd.specId) {
                        cnt += vdGrpCheck.instanceCount;
                    }
                }
                if (cnt < vd.minAllowed) {
                    throw new Exception("Too few service instances -- Spec Id:" + vd.specId + " Min:" + vd.minAllowed + " Max:" + vd.maxAllowed + " Actual:" + vd.instanceCount + " PIID:" + orderData.getProductInstanceId());
                }
                if (cnt > vd.maxAllowed) {
                    throw new Exception("Too many service instances -- Spec Id:" + vd.specId + " Min:" + vd.minAllowed + " Max:" + vd.maxAllowed + " Actual:" + vd.instanceCount + " PIID:" + orderData.getProductInstanceId());
                }
            }
        }
    }

    @Override
    public Done validateOrder(OrderData orderData) throws CMError {
        setContext(orderData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        try {
            if (orderData.getCustomerId() > 0) {
                for (ServiceInstanceData si : orderData.getServiceInstanceData()) {
                    if (si.getServiceSpecificationId() == 100 && !allowedVoice(orderData.getCustomerId())) {
                        throw new Exception("This customer is not allowed a voice service -- " + orderData.getCustomerId());
                    }
                }
            }

            Map<Integer, MappingValidationData> validationMap = null;

            if (orderData.getAction().equals("create")) {

                log.debug("This order is for a new product instance");
                com.smilecoms.cm.db.model.ProductSpecification ps = DAO.getProductSpecificationById(em, orderData.getProductSpecificationId());
                validationMap = getValidationMap(ps);
                log.debug("Looping through services in the order and updating the occurences in the validation data");
                for (ServiceInstanceData si : orderData.getServiceInstanceData()) {
                    if (!si.getAction().equals("create")) {
                        continue;
                    }
                    MappingValidationData vd = validationMap.get(si.getServiceSpecificationId());
                    if (vd == null) {
                        throw new Exception("Order contains invalid service specification");
                    }
                    vd.instanceCount++;
                }
                int max = BaseUtils.getIntProperty("env.cm.max.product.instances.per.customer.excl.org", 0);
                if (max > 0 && orderData.getOrganisationId() == 0) {
                    int cnt = DAO.getCustomerNonOrgProductInstances(em, orderData.getCustomerId()).size();
                    if (cnt >= max) {
                        throw new Exception("Customers product instance limit has been reached -- " + max);
                    }
                }

            } else if (orderData.getAction().equals("none") || orderData.getAction().equals("update")) {

                log.debug("This order is for an existing product instance. Going to get existing product instance [{}] and specification thereof", orderData.getProductInstanceId());
                com.smilecoms.cm.db.model.ProductInstance pi = DAO.getProductInstanceById(em, orderData.getProductInstanceId());
                com.smilecoms.cm.db.model.ProductSpecification ps = pi.getProductSpecification();
                validationMap = getValidationMap(ps);
                for (com.smilecoms.cm.db.model.ServiceInstance si : DAO.getServiceInstancesByProductInstanceId(em, pi.getProductInstanceId())) {
                    MappingValidationData vd = validationMap.get(si.getServiceSpecificationId());
                    if (vd == null) {
                        throw new Exception("Product Instance contains invalid service specification -- " + si.getServiceSpecificationId());
                    }
                    vd.instanceCount++;
                }
                // By now, the validation map has  the counts for the existing SI's. Lets augment it with the order data
                for (ServiceInstanceData si : orderData.getServiceInstanceData()) {
                    if (si.getAction().equals("create")) {
                        MappingValidationData vd = validationMap.get(si.getServiceSpecificationId());
                        if (vd == null) {
                            throw new Exception("Order contains invalid service specification -- " + si.getServiceSpecificationId());
                        }
                        vd.instanceCount++;
                    } else if (si.getAction().equals("delete")) {
                        MappingValidationData vd = validationMap.get(si.getServiceSpecificationId());
                        if (vd == null) {
                            throw new Exception("Order contains invalid service specification -- " + si.getServiceSpecificationId());
                        }
                        vd.instanceCount--;
                    } else if (si.getAction().equals("update")) {
                        // Complex case. The update could be a change in service spec id
                        com.smilecoms.cm.db.model.ServiceInstance existingSI = DAO.getServiceInstanceById(em, si.getServiceInstanceId());
                        if (si.getServiceSpecificationId() != existingSI.getServiceSpecificationId() && si.getServiceSpecificationId() != -1) {
                            log.debug("SI order is for a change in spec from [{}] to [{}]", existingSI.getServiceSpecificationId(), si.getServiceSpecificationId());
                            // Decrement occurence count for old spec and increment for new spec
                            MappingValidationData vdNewSpec = validationMap.get(si.getServiceSpecificationId());
                            MappingValidationData vdOrigSpec = validationMap.get(existingSI.getServiceSpecificationId());
                            if (vdNewSpec == null) {
                                throw new Exception("Order contains invalid service -- " + si.getServiceSpecificationId());
                            }
                            if (vdOrigSpec == null) {
                                throw new Exception("Order contains invalid service -- " + existingSI.getServiceSpecificationId());
                            }
                            vdNewSpec.instanceCount++;
                            vdOrigSpec.instanceCount--;
                        }
                    }
                }
            }

            checkForValidationIssues(validationMap, orderData);

            if (!orderData.getAction().equals("delete")) {

                if (orderData.getAction().equals("create") && orderData.getSegment() == null) {
                    throw new Exception("Invalid segment");
                }

                if (orderData.getSegment() != null) {
                    log.debug("Checking the segment and organisation id are ok");
                    if (orderData.getOrganisationId() == 0 && (orderData.getSegment().toLowerCase().contains("work")
                            || orderData.getSegment().toLowerCase().contains("us")
                            || orderData.getSegment().toLowerCase().contains("staff")
                            || orderData.getSegment().toLowerCase().contains("business"))) {
                        log.debug("OrganisationId is zero so this must be Home or Me segment");
                        throw new Exception("Invalid segment for organisation");
                    }
                    if (orderData.getOrganisationId() == 1 && !orderData.getSegment().toLowerCase().contains("staff")) {
                        log.debug("OrganisationId is 1 so this must be Smile Staff segment");
                        throw new Exception("Invalid segment for organisation");
                    }
                    if (orderData.getOrganisationId() > 1 && (orderData.getSegment().toLowerCase().contains("staff")
                            || orderData.getSegment().toLowerCase().contains("me")
                            || orderData.getSegment().toLowerCase().contains("home"))) {
                        log.debug("OrganisationId is > 1 so this cannot be home or me or staff segment");
                        throw new Exception("Invalid segment for organisation");
                    }
                }

                if (orderData.getProductSpecificationId() > 0) {
                    log.debug("Checking segment is allowed for this product");
                    ProductSpecification xmlSpec = getXMLProductSpecification(orderData.getProductSpecificationId(), "MAIN_SVC");
                    if (orderData.getAction().equals("create") && !Utils.listContains(Utils.getListFromCRDelimitedString(xmlSpec.getSegments()), orderData.getSegment())) {
                        throw new Exception("Invalid segment for product");
                    }

                    log.debug("Checking caller has rights to provision the products/services");

                    List<String> productsAllowedRoles = Utils.getListFromCRDelimitedString(xmlSpec.getProvisionRoles());
                    if (orderData.getAction().equals("create") && !Utils.listsIntersect(orderData.getCallersRoles(), productsAllowedRoles)) {
                        throw new Exception("Product cannot be provisioned by the calling user");
                    }

                    for (ServiceInstanceData si : orderData.getServiceInstanceData()) {
                        if (si.getAction().equals("create") || si.getAction().equals("update")) {
                            for (ProductServiceSpecificationMapping mapping : xmlSpec.getProductServiceSpecificationMappings()) {
                                if (mapping.getServiceSpecification().getServiceSpecificationId() == si.getServiceSpecificationId()) {
                                    List<String> productServiceMappingAllowedRoles = Utils.getListFromCRDelimitedString(mapping.getProvisionRoles());
                                    if (!Utils.listsIntersect(orderData.getCallersRoles(), productServiceMappingAllowedRoles)) {
                                        throw new Exception("Service cannot be provisioned by the calling user");
                                    }
                                    if (!Utils.isBetween(new Date(), mapping.getAvailableFrom(), mapping.getAvailableTo())) {
                                        throw new Exception("Service cannot be provisioned as its not currently available");
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } // end if (orderData.getProductSpecificationId() > 0) {
            } // end if (!orderData.getAction().equals("delete")) {

        } catch (Exception e) {
            throw processError(CMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    private boolean allowedVoice(int customerProfileId) {

        // PCB 2017/02/08 - not needed with new KYC process
//        List<String[]> customerids = null;
//        try {
//            customerids = BaseUtils.getPropertyFromSQL("env.customers.voice.banned");
//        } catch (Exception e) {
//            log.debug("Error seeing if voice is banned on this customer - env.customers.voice.banned does not exist");
//        }
//        if (customerids != null) {
//            for (String[] id : customerids) {
//                if (id[0].equals(String.valueOf(customerProfileId))) {
//                    return false;
//                }
//            }
//        }
        return true;
    }

    private ProductSpecification getXMLProductSpecification(int prodSpecId, String verbosity) {
        log.debug("In getXMLProductSpecificication");
        ProductSpecification xmlProdSpec = CMDataCache.productSpecificationCache.get(prodSpecId + verbosity);

        if (xmlProdSpec == null) {

            xmlProdSpec = new ProductSpecification();
            com.smilecoms.cm.db.model.ProductSpecification dbProdSpec = DAO.getProductSpecificationById(em, prodSpecId);
            xmlProdSpec.setProductSpecificationId(dbProdSpec.getProductSpecificationId());
            xmlProdSpec.setName(dbProdSpec.getProductName());
            xmlProdSpec.setDescription(dbProdSpec.getProductDescription());
            xmlProdSpec.setAvailableFrom(Utils.getDateAsXMLGregorianCalendar(dbProdSpec.getAvailableFrom()));
            xmlProdSpec.setAvailableTo(Utils.getDateAsXMLGregorianCalendar(dbProdSpec.getAvailableTo()));
            xmlProdSpec.setProvisionRoles(dbProdSpec.getProvisionRoles());
            xmlProdSpec.setSegments(dbProdSpec.getSegments());

            // Load Product-Service Mappings
            ProductServiceSpecificationMapping xmlProdSvcSpecMapping;
            ServiceSpecification xmlServiceSpecification;

            if (verbosity.contains("SVC")) {
                for (com.smilecoms.cm.db.model.ProductServiceMapping dbProdSvcSpecMapping : DAO.getProductToServiceMappings(em, dbProdSpec.getProductSpecificationId())) {
                    xmlProdSvcSpecMapping = new ProductServiceSpecificationMapping();
                    xmlProdSvcSpecMapping.setMaxServiceOccurences(dbProdSvcSpecMapping.getMaxServiceOccurences());
                    xmlProdSvcSpecMapping.setMinServiceOccurences(dbProdSvcSpecMapping.getMinServiceOccurences());
                    xmlProdSvcSpecMapping.setRatePlanId(dbProdSvcSpecMapping.getRatePlanId());
                    xmlProdSvcSpecMapping.setGroupId(dbProdSvcSpecMapping.getGroupId());
                    xmlProdSvcSpecMapping.setProvisionRoles(dbProdSvcSpecMapping.getProvisionRoles());
                    xmlProdSvcSpecMapping.setAvailableFrom(Utils.getDateAsXMLGregorianCalendar(dbProdSvcSpecMapping.getAvailableFrom()));
                    xmlProdSvcSpecMapping.setAvailableTo(Utils.getDateAsXMLGregorianCalendar(dbProdSvcSpecMapping.getAvailableTo()));
                    com.smilecoms.cm.db.model.ServiceSpecification dbServiceSpecification
                            = DAO.getServiceSpecificationById(em,
                                    dbProdSvcSpecMapping.getProductServiceMappingPK().getServiceSpecificationId());

                    xmlServiceSpecification = getXMLServiceSpecification(dbServiceSpecification, verbosity);
                    xmlProdSvcSpecMapping.setServiceSpecification(xmlServiceSpecification);
                    xmlProdSpec.getProductServiceSpecificationMappings().add(xmlProdSvcSpecMapping);
                }
            }
            // Get ProductSpecificationAVPs

            if (verbosity.contains("PRODAVP")) {
                AVP xmlProdSpecAvp;
                for (com.smilecoms.cm.db.model.ProductSpecificationAvp dbProdSpecAvp : dbProdSpec.getProductSpecificationAvpCollection()) {
                    xmlProdSpecAvp = new AVP();
                    xmlProdSpecAvp.setAttribute(dbProdSpecAvp.getProductSpecificationAvpPK().getAttribute());
                    xmlProdSpecAvp.setInputType(dbProdSpecAvp.getInputType());
                    xmlProdSpecAvp.setUserDefined(Utils.booleanValue(dbProdSpecAvp.getUserDefined()));
                    xmlProdSpecAvp.setTechnicalDescription(dbProdSpecAvp.getTechnicalDescription());
                    xmlProdSpecAvp.setProvisionRoles(dbProdSpecAvp.getProvisionRoles());
                    xmlProdSpecAvp.setValue(dbProdSpecAvp.getValue());
                    xmlProdSpecAvp.setValidationRule(dbProdSpecAvp.getValidationRule());
                    xmlProdSpec.getAVPs().add(xmlProdSpecAvp);
                }
            }
            CMDataCache.productSpecificationCache.put(prodSpecId + verbosity, xmlProdSpec);
        }
        return xmlProdSpec;
    }

    private ServiceSpecification getXMLServiceSpecification(com.smilecoms.cm.db.model.ServiceSpecification dbSvcSpec, String verbosity) {
        log.debug("In getXMLServiceSpecification");
        ServiceSpecification xmlSvcSpec = new ServiceSpecification();

        xmlSvcSpec.setServiceSpecificationId(dbSvcSpec.getServiceSpecificationId());
        xmlSvcSpec.setName(dbSvcSpec.getServiceName());
        xmlSvcSpec.setDescription(dbSvcSpec.getServiceDescription());
        xmlSvcSpec.setServiceCode(dbSvcSpec.getServiceCode());

        if (verbosity.contains("SVCAVP")) {
            for (com.smilecoms.cm.db.model.ServiceSpecificationAvp dbServiceSpecificationAvp : dbSvcSpec.getServiceSpecificationAvpCollection()) {

                AVP avp = new AVP();
                avp.setAttribute(dbServiceSpecificationAvp.getServiceSpecificationAvpPK().getAttribute());
                avp.setInputType(dbServiceSpecificationAvp.getInputType());
                avp.setTechnicalDescription(dbServiceSpecificationAvp.getTechnicalDescription());
                avp.setProvisionRoles(dbServiceSpecificationAvp.getProvisionRoles());
                avp.setUserDefined(Utils.booleanValue(dbServiceSpecificationAvp.getUserDefined()));
                avp.setValidationRule(dbServiceSpecificationAvp.getValidationRule());
                avp.setValue(removeComments(dbServiceSpecificationAvp.getValue()));

                xmlSvcSpec.getAVPs().add(avp);
            }
        }
        return xmlSvcSpec;
    }

    private UnitCreditSpecification getXMLUnitCreditSpecification(com.smilecoms.cm.db.model.UnitCreditSpecification dbUnitCreditSpec, String verbosity) {
        UnitCreditSpecification xmlUnitCrdSpec = new UnitCreditSpecification();
        xmlUnitCrdSpec.setAvailableFrom(Utils.getDateAsXMLGregorianCalendar(dbUnitCreditSpec.getAvailableFrom()));
        xmlUnitCrdSpec.setAvailableTo(Utils.getDateAsXMLGregorianCalendar(dbUnitCreditSpec.getAvailableTo()));
        xmlUnitCrdSpec.setUnitCreditSpecificationId(dbUnitCreditSpec.getUnitCreditSpecificationId());
        xmlUnitCrdSpec.setName(dbUnitCreditSpec.getUnitCreditName());
        xmlUnitCrdSpec.setItemNumber(dbUnitCreditSpec.getItemNumber());
        xmlUnitCrdSpec.setPriceInCents(dbUnitCreditSpec.getPriceCents());
        xmlUnitCrdSpec.setValidityDays(dbUnitCreditSpec.getValidityDays());
        xmlUnitCrdSpec.setUsableDays(dbUnitCreditSpec.getUsableDays());
        xmlUnitCrdSpec.setUnits(dbUnitCreditSpec.getUnits());
        xmlUnitCrdSpec.setDescription(dbUnitCreditSpec.getUnitCreditDescription());
        xmlUnitCrdSpec.setPurchaseRoles(dbUnitCreditSpec.getPurchaseRoles());
        xmlUnitCrdSpec.setPriority(dbUnitCreditSpec.getPriority());
        xmlUnitCrdSpec.setUnitType(dbUnitCreditSpec.getUnitType());
        xmlUnitCrdSpec.setFilterClass(dbUnitCreditSpec.getFilterClass().replace("com.smilecoms.bm.unitcredits.", ""));
        xmlUnitCrdSpec.setConfiguration(dbUnitCreditSpec.getConfiguration());
        xmlUnitCrdSpec.setWrapperClass(dbUnitCreditSpec.getWrapperClass().replace("com.smilecoms.bm.unitcredits.", ""));

        // add avps if requested
        if (verbosity.contains("SVCSPECIDS")) {
            for (com.smilecoms.cm.db.model.UnitCreditServiceMapping mapping : DAO.getUnitCreditProductServiceMappings(em, dbUnitCreditSpec.getUnitCreditSpecificationId())) {
                com.smilecoms.xml.schema.cm.ProductServiceMapping psm = new com.smilecoms.xml.schema.cm.ProductServiceMapping();
                psm.setProductSpecificationId(mapping.getUnitCreditServiceMappingPK().getProductSpecificationId());
                psm.setServiceSpecificationId(mapping.getUnitCreditServiceMappingPK().getServiceSpecificationId());
                xmlUnitCrdSpec.getProductServiceMappings().add(psm);
            }
        }
        return xmlUnitCrdSpec;
    }

    private UnitCreditSpecification getXMLUnitCreditSpecificationByName(String unitCreditName, String verbosity) {
        log.debug("In getXMLUnitCreditSpecification by name");
        UnitCreditSpecification xmlUnitCrdSpec = CMDataCache.unitCreditSpecificationCache.get("UCN_" + unitCreditName + verbosity);
        if (xmlUnitCrdSpec == null) {
            com.smilecoms.cm.db.model.UnitCreditSpecification dbUnitCreditSpec = DAO.getUnitCreditSpecificationByName(em, unitCreditName);
            xmlUnitCrdSpec = getXMLUnitCreditSpecification(dbUnitCreditSpec, verbosity);
            CMDataCache.unitCreditSpecificationCache.put("UCN_" + unitCreditName + verbosity, xmlUnitCrdSpec);
        }
        return xmlUnitCrdSpec;
    }

    private UnitCreditSpecification getXMLUnitCreditSpecificationByItemNumber(String itemNumber, String verbosity) {
        log.debug("In getXMLUnitCreditSpecification by item number");
        UnitCreditSpecification xmlUnitCrdSpec = CMDataCache.unitCreditSpecificationCache.get("UCIN_" + itemNumber + verbosity);
        if (xmlUnitCrdSpec == null) {
            com.smilecoms.cm.db.model.UnitCreditSpecification dbUnitCreditSpec = DAO.getUnitCreditSpecificationByItemNumber(em, itemNumber);
            xmlUnitCrdSpec = getXMLUnitCreditSpecification(dbUnitCreditSpec, verbosity);
            CMDataCache.unitCreditSpecificationCache.put("UCIN_" + itemNumber + verbosity, xmlUnitCrdSpec);
        }
        return xmlUnitCrdSpec;
    }

    private UnitCreditSpecification getXMLUnitCreditSpecification(int ucsId, String verbosity) {
        log.debug("In getXMLUnitCreditSpecification by Id");
        UnitCreditSpecification xmlUnitCrdSpec = CMDataCache.unitCreditSpecificationCache.get("UCID_" + ucsId + verbosity);
        if (xmlUnitCrdSpec == null) {
            com.smilecoms.cm.db.model.UnitCreditSpecification dbUnitCreditSpec = DAO.getUnitCreditSpecificationById(em, ucsId);
            xmlUnitCrdSpec = getXMLUnitCreditSpecification(dbUnitCreditSpec, verbosity);
            CMDataCache.unitCreditSpecificationCache.put("UCID_" + ucsId + verbosity, xmlUnitCrdSpec);
        }
        return xmlUnitCrdSpec;

    }

    private AVP cloneAVP(AVP toClone) {
        AVP cloned = new AVP();
        cloned.setAttribute(toClone.getAttribute());
        cloned.setInputType(toClone.getInputType());
        cloned.setTechnicalDescription(toClone.getTechnicalDescription());
        cloned.setProvisionRoles(toClone.getProvisionRoles());
        cloned.setUserDefined(toClone.isUserDefined());
        cloned.setValidationRule(toClone.getValidationRule());
        cloned.setValue(toClone.getValue());
        return cloned;
    }

    private List<AVP> getClonedAVPList(List<AVP> toClone) {
        List<AVP> cloned = new ArrayList();
        for (AVP avp : toClone) {
            cloned.add(cloneAVP(avp));
        }
        return cloned;
    }

    private List<AVP> getServiceSpecAVPs(int specId) {
        List<AVP> specAVPs = CMDataCache.serviceSpecAVPCache.get(specId);
        if (specAVPs == null) {
            specAVPs = new ArrayList<>();
            for (com.smilecoms.cm.db.model.ServiceSpecificationAvp dbServiceSpecificationAvp : DAO.getServiceSpecificationAVPs(em, specId)) {
                AVP avp = new AVP();
                avp.setAttribute(dbServiceSpecificationAvp.getServiceSpecificationAvpPK().getAttribute());
                avp.setInputType(dbServiceSpecificationAvp.getInputType());
                avp.setTechnicalDescription(dbServiceSpecificationAvp.getTechnicalDescription());
                avp.setProvisionRoles(dbServiceSpecificationAvp.getProvisionRoles());
                avp.setUserDefined(Utils.booleanValue(dbServiceSpecificationAvp.getUserDefined()));
                avp.setValidationRule(dbServiceSpecificationAvp.getValidationRule());
                avp.setValue(removeComments(dbServiceSpecificationAvp.getValue()));
                specAVPs.add(avp);
            }
            CMDataCache.serviceSpecAVPCache.put(specId, specAVPs);
        }
        /*
         * Clone the AVP List before returning. The caller may change the AVPs and we dont want that to pollute our cache
         */
        return getClonedAVPList(specAVPs);
    }

    private CampaignData getXMLCampaignData(com.smilecoms.cm.db.model.ProductInstance pi, com.smilecoms.cm.db.model.CampaignRun dbCampaignData, String verbosity) {

        com.smilecoms.cm.db.model.Campaign dbCampaign = DAO.getCampaign(em, dbCampaignData.getCampaignId());

        CampaignData xmlCampaignData = new CampaignData();

        xmlCampaignData.setCampaignId(dbCampaignData.getCampaignId());
        xmlCampaignData.setEndDateTime(Utils.getDateAsXMLGregorianCalendar(dbCampaign.getEndDateTime()));
        xmlCampaignData.setStartDateTime(Utils.getDateAsXMLGregorianCalendar(dbCampaign.getStartDateTime()));
        xmlCampaignData.setName(dbCampaign.getCampaignPK().getName());
        xmlCampaignData.setStatus(dbCampaignData.getStatus());
        xmlCampaignData.setLastCheckDateTime(Utils.getDateAsXMLGregorianCalendar(dbCampaignData.getLastCheckDateTime()));

        if (verbosity.contains("CAMPAIGNUC")) {
            try {
                log.debug("Verbosity says we must get campaign unit credits for [{}]", pi.getProductInstanceId());
                xmlCampaignData.getCampaignUnitCredits().addAll(getCampaignUnitCredits(pi, dbCampaign));
            } catch (Exception e) {
                new ExceptionManager(log).reportError(e);
            }
        }
        return xmlCampaignData;
    }

    private Collection<Integer> getCampaignUnitCredits(com.smilecoms.cm.db.model.ProductInstance pi, com.smilecoms.cm.db.model.Campaign dbCampaign) throws Exception {
        Set<Integer> ucSet = new HashSet<>();
        String javaAssistCodeProp = Utils.getValueFromCRDelimitedAVPString(dbCampaign.getActionConfig(), "CampaignUC");
        log.debug("Code property name is [{}]", javaAssistCodeProp);
        if (javaAssistCodeProp != null) {
            String code = BaseUtils.getProperty(javaAssistCodeProp, "");
            log.debug("Code is [{}]", code);
            if (!code.isEmpty()) {
                Javassist.runCode(new Class[]{CatalogManager.class}, code, em, log, ucSet, pi, dbCampaign);
                log.debug("Campaign UCs are [{}]", ucSet);
            }
        }
        log.debug("UC Spec Whitelist SQL query is [{}]", dbCampaign.getUnitCreditSpecWhitelistQuery());
        if (!dbCampaign.getUnitCreditSpecWhitelistQuery().isEmpty()) {
            try {
                ucSet.addAll(DAO.getCampaignUnitCreditSpecIds(em, dbCampaign.getUnitCreditSpecWhitelistQuery(), pi.getProductInstanceId()));
                log.debug("UC Spec Ids is now [{}]", ucSet);
            } catch (Exception e) {
                new ExceptionManager(log).reportError(e);
            }
        }

        return ucSet;
    }

    public static String go(javax.persistence.EntityManager em, Logger log, Set ucSpecIds, com.smilecoms.cm.db.model.ProductInstance pi, com.smilecoms.cm.db.model.Campaign dbCampaign) {

        if (System.currentTimeMillis() % 2 == 0) {
            ucSpecIds.add(1);
        }
        return "Done";
    }

    private ServiceInstance getXMLServiceInstance(com.smilecoms.cm.db.model.ServiceInstance dbSvcInst, String verbosity) throws UnsupportedEncodingException {

        log.debug("In getXMLServiceInstance");

        ServiceInstance xmlSvcInst = new ServiceInstance();

        xmlSvcInst.setAccountId(dbSvcInst.getAccountId());
        xmlSvcInst.setCustomerId(dbSvcInst.getCustomerProfileId());
        xmlSvcInst.setServiceInstanceId(dbSvcInst.getServiceInstanceId());
        xmlSvcInst.setProductInstanceId(dbSvcInst.getProductInstanceId());
        xmlSvcInst.setRemoteResourceId(dbSvcInst.getRemoteResourceId());
        xmlSvcInst.setStatus(dbSvcInst.getStatus());
        xmlSvcInst.setServiceSpecificationId(dbSvcInst.getServiceSpecificationId());
        xmlSvcInst.setRatePlanId(DAO.getServiceInstancesRatePlanId(em, dbSvcInst.getProductInstanceId(), dbSvcInst.getServiceSpecificationId()));
        xmlSvcInst.setCreatedDateTime(Utils.getDateAsXMLGregorianCalendar(dbSvcInst.getCreatedDatetime()));
        // add avps if requested
        if (verbosity.contains("AVP")) {
            xmlSvcInst.getAVPs().addAll(getServiceSpecAVPs(dbSvcInst.getServiceSpecificationId()));
            log.debug("Populating informational AVPs values");
            List<String> infoList = Utils.getListFromCRDelimitedString(dbSvcInst.getInfo());
            //Do not return fingerprint information if the country regulation does not allow fingerprints to be stored.

            //In some countries, like Tanzania, fingerprints are not allowed to be stored on the system.
            if (BaseUtils.getBooleanProperty("env.kyc.do.not.store.fingerprints", false)) {
                Iterator<AVP> i = xmlSvcInst.getAVPs().iterator();
                while (i.hasNext()) {
                    AVP avp = i.next();
                    if (avp.getAttribute() != null && avp.getAttribute().equalsIgnoreCase("Fingerprints")) {
                        i.remove();
                    }
                }
            }

            for (AVP avp : xmlSvcInst.getAVPs()) {
                if (avp.getInputType().equals("info") || avp.getInputType().equals("photo")) {
                    log.debug("Looking at service spec AVP [{}] with Input Type [{}]", avp.getAttribute(), avp.getInputType());
                    for (String info : infoList) {
                        log.debug("Checking if service instance info line is for this AVP [{}]", info);
                        String start = avp.getAttribute() + "=";
                        if (info.startsWith(start)) {
                            String val = info.substring(start.length());
                            log.debug("Found info value [{}]", val);
                            if (avp.getInputType().equals("photo")) {
                                log.debug("This is a photo with guid [{}]", val);
                                if (verbosity.contains("PHOTO")) {
                                    avp.setValue(val + "|" + Utils.encodeBase64(DAO.getPhoto(em, val)));
                                } else {
                                    avp.setValue(val);
                                }
                            } else {
                                avp.setValue(val);
                            }
                            log.debug("Set AVP value to [{}]", avp.getValue());
                            break;
                        }
                    }
                }
                // Requirement for Uganda to not allow using a SIM that is not fully registered with a certain number of hours of creation
                if (avp.getAttribute().equals("KYCStatus") && BaseUtils.getProperty("env.kyc.level", "service").equalsIgnoreCase("service")) {
                    if (Utils.matchesWithPatternCache(avp.getValue(), BaseUtils.getProperty("env.kyc.incomplete.status.regex", "Unvalidated|Incomplete"))) {
                        Calendar startDate = Calendar.getInstance();
                        String dt = BaseUtils.getProperty("env.cm.kyc.enforced.date", "20500101");
                        // Note - on the month below field, we minus 1 for Java compliance
                        startDate.set(Integer.parseInt(dt.substring(0, 4)), Integer.parseInt(dt.substring(4, 6)) - 1, Integer.parseInt(dt.substring(6, 8)), 0, 0, 0);
                        Calendar cutOffDate = Calendar.getInstance();
                        cutOffDate.setTime(dbSvcInst.getCreatedDatetime());
                        cutOffDate.add(Calendar.HOUR, BaseUtils.getIntProperty("env.cm.kyc.grace.hours", 48));
                        if (!Utils.isInTheFuture(cutOffDate.getTime()) && dbSvcInst.getCreatedDatetime().after(startDate.getTime())) {
                            log.debug("KYCStatus is [{}] and created date is [{}] so the service status is being set to TD", avp.getValue(), dbSvcInst.getCreatedDatetime());
                            if (!dbSvcInst.getStatus().equals(SERVICE_INSTANCE_STATUS.TD.toString())) {
                                DAO.tempDeactivateAllSIsInProductInstance(em, dbSvcInst.getProductInstanceId());
                                xmlSvcInst.setStatus("TD");
                                PlatformEventManager.createEvent("CM", "KYCStatusIssue", String.valueOf(dbSvcInst.getServiceInstanceId()),
                                        String.valueOf(dbSvcInst.getProductInstanceId()) + "|" + dbSvcInst.getAccountId() + "|" + dbSvcInst.getCustomerProfileId(), "KYCStatusIssue_" + dbSvcInst.getProductInstanceId());
                            }
                        }
                    }
                }

                //For Uganda - Customers who registered with VISA must have their SIMS deactivated when the VISA expires.
                if (BaseUtils.getBooleanProperty("env.customer.visa.enable", false)) {
                    if (BaseUtils.getBooleanProperty("env.customer.visa.enabled", false)) {
                        com.smilecoms.commons.sca.Customer cust = SCAWrapper.getAdminInstance().getCustomer(dbSvcInst.getCustomerProfileId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER);
                        if (cust != null) {
                            if (!cust.getNationality().equalsIgnoreCase(BaseUtils.getProperty("env.locale.country.for.language.en"))) {
                                if (!cust.getVisaExpiryDate().equalsIgnoreCase("For-Life")) {
                                    Date visaExpiryDate = Utils.stringToDate(cust.getVisaExpiryDate());
                                    if (visaExpiryDate != null) {
                                        if (!Utils.isInTheFuture(visaExpiryDate)) {
                                            DAO.tempDeactivateAllSIsInProductInstance(em, dbSvcInst.getProductInstanceId());
                                            xmlSvcInst.setStatus("TD");
                                            PlatformEventManager.createEvent("CM", "VISAExpiryIssue", String.valueOf(dbSvcInst.getServiceInstanceId()),
                                                    String.valueOf(dbSvcInst.getProductInstanceId()) + "|" + dbSvcInst.getAccountId() + "|" + dbSvcInst.getCustomerProfileId(), "VISAEXpiryIssue_" + dbSvcInst.getProductInstanceId());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Requirement for Nigeria to  not allow SIMs that are not fully registered. Kyc in Nigeria is done at customer level.
        if (BaseUtils.getProperty("env.kyc.level", "service").equalsIgnoreCase("customer")) {
            com.smilecoms.commons.sca.Customer cust = SCAWrapper.getAdminInstance().getCustomer(dbSvcInst.getCustomerProfileId(), com.smilecoms.commons.sca.StCustomerLookupVerbosity.CUSTOMER);
            if (Utils.matchesWithPatternCache(cust.getKYCStatus(), BaseUtils.getProperty("env.kyc.incomplete.status.regex", "P|U"))) {
                Calendar startDate = Calendar.getInstance();
                String dt = BaseUtils.getProperty("env.cm.kyc.enforced.date", "20500101");
                // Note - on the month below field, we minus 1 for Java compliance
                startDate.set(Integer.parseInt(dt.substring(0, 4)), Integer.parseInt(dt.substring(4, 6)) - 1, Integer.parseInt(dt.substring(6, 8)), 0, 0, 0);
                Calendar cutOffDate = Calendar.getInstance();
                cutOffDate.setTime(dbSvcInst.getCreatedDatetime());
                cutOffDate.add(Calendar.HOUR, BaseUtils.getIntProperty("env.cm.kyc.grace.hours", 48)); // By default allow 7 days and then block afterwards.
                if (!Utils.isInTheFuture(cutOffDate.getTime()) && dbSvcInst.getCreatedDatetime().after(startDate.getTime())) {
                    log.debug("Customer KYCStatus is [{}] and created date is [{}] so the service status is being set to TD.", cust.getKYCStatus(), dbSvcInst.getCreatedDatetime());
                    if (!dbSvcInst.getStatus().equals(SERVICE_INSTANCE_STATUS.TD.toString())) {
                        DAO.tempDeactivateAllSIsInProductInstance(em, dbSvcInst.getProductInstanceId());
                        xmlSvcInst.setStatus("TD");
                        PlatformEventManager.createEvent("CM", "KYCStatusIssue", String.valueOf(dbSvcInst.getServiceInstanceId()),
                                String.valueOf(dbSvcInst.getProductInstanceId()) + "|" + dbSvcInst.getAccountId() + "|" + dbSvcInst.getCustomerProfileId(), "KYCStatusIssue_" + dbSvcInst.getProductInstanceId());
                    }
                }
            }
        }

        return xmlSvcInst;
    }

    private ProductInstance getXMLProductInstance(com.smilecoms.cm.db.model.ProductInstance dbProdInst, String verbosity) throws UnsupportedEncodingException {
        log.debug("In getXMLProductInstance");

        ProductInstance xmlProdInst = new ProductInstance();

        xmlProdInst.setCustomerId(dbProdInst.getCustomerProfileId());
        xmlProdInst.setOrganisationId(dbProdInst.getOrganisationId());
        xmlProdInst.setProductInstanceId(dbProdInst.getProductInstanceId());
        xmlProdInst.setSegment(dbProdInst.getSegment());
        xmlProdInst.setFriendlyName(dbProdInst.getFriendlyName());
        xmlProdInst.setReferralCode(dbProdInst.getReferralCode() == null ? "" : dbProdInst.getReferralCode());
        xmlProdInst.setLogicalId(dbProdInst.getLogicalId());
        xmlProdInst.setPhysicalId(dbProdInst.getPhysicalId() == null ? "" : dbProdInst.getPhysicalId());
        xmlProdInst.setLastDevice(getDeviceMakeAndModelByIMEI(dbProdInst.getLastIMEI()));
        xmlProdInst.setPromotionCode(dbProdInst.getPromotionCode());
        xmlProdInst.setProductSpecificationId(dbProdInst.getProductSpecification().getProductSpecificationId());
        xmlProdInst.setCreatedDateTime(Utils.getDateAsXMLGregorianCalendar(dbProdInst.getCreatedDatetime()));
        xmlProdInst.setFirstActivityDateTime(Utils.getDateAsXMLGregorianCalendar(dbProdInst.getFirstActivityDateTime()));
        xmlProdInst.setLastActivityDateTime(Utils.getDateAsXMLGregorianCalendar(dbProdInst.getLastActivityDateTime()));
        xmlProdInst.setCreatedByOrganisationId(dbProdInst.getCreatedByOrganisationId());
        xmlProdInst.setCreatedByCustomerProfileId(dbProdInst.getCreatedByCustomerProfileId());
        xmlProdInst.setStatus(dbProdInst.getStatus());

        // - Get the list of service instances mapped to this product instance.
        Collection<com.smilecoms.cm.db.model.ServiceInstance> lstDbSvcInst = DAO.getServiceInstancesByProductInstanceId(em, dbProdInst.getProductInstanceId());
        // xmlProdInst.getProductServiceInstanceMappings().  
        ProductServiceInstanceMapping prodSvcMapping;
        ServiceInstance xmlSvcInst;
        if (verbosity.contains("SVC")) {
            for (com.smilecoms.cm.db.model.ServiceInstance dbSvcInst : lstDbSvcInst) {
                xmlSvcInst = getXMLServiceInstance(dbSvcInst, verbosity);
                prodSvcMapping = new ProductServiceInstanceMapping();
                prodSvcMapping.setServiceInstance(xmlSvcInst);
                prodSvcMapping.setRatePlanId(xmlSvcInst.getRatePlanId());
                xmlProdInst.getProductServiceInstanceMappings().add(prodSvcMapping);
            }
        }
        if (verbosity.contains("CAMPAIGNS")) {
            //CampaignData
            Collection<CampaignRun> dbCampaignDataList = DAO.getCampaignRunList(em, dbProdInst.getProductInstanceId());
            for (CampaignRun dbCampaignData : dbCampaignDataList) {
                xmlProdInst.getCampaigns().add(getXMLCampaignData(dbProdInst, dbCampaignData, verbosity));
            }
        }

        return xmlProdInst;
    }

    private String removeComments(String value) {
        StringTokenizer stValues = new StringTokenizer(value, "\r\n");
        StringBuilder ret = new StringBuilder();
        while (stValues.hasMoreTokens()) {
            String val = stValues.nextToken();
            if (val.length() != 0 && !val.startsWith("#")) {
                ret.append(val);
                ret.append("\r\n");
            }
        }
        if (ret.length() > 0) {
            return ret.substring(0, ret.length() - 2);
        } else {
            return ret.toString();
        }

    }

    private String addOrModifyInfoAVP(String attribute, String value, String currentInfo) {
        List<String> avpList = Utils.getListFromCRDelimitedString(currentInfo);
        StringBuilder newInfo = new StringBuilder();
        String lookup = attribute + "=";
        boolean added = false;
        for (String avp : avpList) {
            if (avp.startsWith(lookup)) {
                newInfo.append(lookup);
                newInfo.append(value);
                newInfo.append("\r\n");
                added = true;
            } else {
                newInfo.append(avp);
                newInfo.append("\r\n");
            }
        }
        if (!added) {
            newInfo.append(lookup);
            newInfo.append(value);
        }
        return newInfo.toString();
    }

    private String removeInfoAVP(String attribute, String currentInfo) {
        List<String> avpList = Utils.getListFromCRDelimitedString(currentInfo);
        StringBuilder newInfo = new StringBuilder();
        String lookup = attribute + "=";
        for (String avp : avpList) {
            if (!avp.startsWith(lookup)) {
                newInfo.append(avp);
                newInfo.append("\r\n");
            }
        }
        return newInfo.toString();
    }

    private String getValueFromInfoAVPString(String info, String attribute) {
        List<String> avpList = Utils.getListFromCRDelimitedString(info);
        String lookup = attribute + "=";
        for (String avp : avpList) {
            if (avp.startsWith(lookup)) {
                return avp.split("\\=")[1];
            }
        }
        return null;
    }

    private int getProductInstanceLogicalId(String physicalId) throws Exception {
        // If the physicalId exists on a product instance, then find the logical Id of that product instance
        // If the physical Id does not exist then this is a new logical Id so return the current Max + 1
        if (physicalId == null || physicalId.isEmpty()) {
            log.debug("This product has no associated physical Id so cant have a logical Id yet");
            return 0;
        }
        int ret = DAO.getLogicalIdForPhysicalId(em, physicalId);
        if (ret == 0) {
            log.debug("Physical Id does not exist or does not have an associated logical Id. Going to get a brand new logical Id");
            return ret;
        } else {
            log.debug("Physical Id has an existing logical Id of [{}]", ret);
            return ret;
        }
    }

    private String getDeviceMakeAndModelByIMEI(String lastIMEI) {
        try {
            if (lastIMEI == null || lastIMEI.isEmpty() || !lastIMEI.contains("=")) {
                return "Unknown";
            }
            String imeisv = lastIMEI.split("=")[1];
            String device = Utils.getDeviceMakeAndModel(imeisv);
            if (device == null) {
                return lastIMEI;
            } else {
                return device;
            }
        } catch (Exception e) {
            log.warn("Error getting DeviceMakeAndModelByIMEI", e);
        }
        return "Error";
    }

}
