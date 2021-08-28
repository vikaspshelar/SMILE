/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.op;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.HashUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.im.db.model.*;
import com.smilecoms.xml.schema.im.ThirdPartyAuthorisationRule;
import com.smilecoms.xml.schema.im.ThirdPartyAuthorisationRuleSet;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class IMDAO {

    private static final Logger log = LoggerFactory.getLogger(IMDAO.class);

    public static CustomerProfile getCustomerProfileById(EntityManager em, int customerId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_profile WHERE CUSTOMER_PROFILE_ID=? AND STATUS='AC'", CustomerProfile.class);
        q.setParameter(1, customerId);
        return (CustomerProfile) q.getSingleResult();
    }

    public static CustomerProfile getCustomerProfileByEmailAddress(EntityManager em, String identifier) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_profile WHERE EMAIL_ADDRESS=? AND STATUS='AC'", CustomerProfile.class);
        q.setParameter(1, identifier);
        return (CustomerProfile) q.getSingleResult();
    }

    public static Iterable<CustomerProfile> getCustomerProfilesByWildcardedFirstAndLastName(EntityManager em, String firstName, String lastName, int limit) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_profile WHERE FIRST_NAME LIKE ? AND LAST_NAME LIKE ? AND STATUS='AC' LIMIT ?", CustomerProfile.class);
        q.setParameter(1, firstName + "%");
        q.setParameter(2, lastName + "%");
        q.setParameter(3, limit);
        return (List<CustomerProfile>) q.getResultList();
    }

    public static List<CustomerProfile> getCustomerProfilesByWildcardedFirstName(EntityManager em, String firstName, int limit) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_profile WHERE FIRST_NAME LIKE ? AND STATUS='AC' LIMIT ?", CustomerProfile.class);
        q.setParameter(1, firstName + "%");
        q.setParameter(2, limit);
        return (List<CustomerProfile>) q.getResultList();
    }

    public static List<CustomerProfile> getCustomerProfileByWildcardedOrganisationName(EntityManager em, String orgName, int limit) {
        Query q = em.createNativeQuery("SELECT C.* FROM SmileDB.organisation O, SmileDB.customer_profile C, SmileDB.customer_role R  "
                + "WHERE O.ORGANISATION_NAME LIKE ? and O.ORGANISATION_ID = R.ORGANISATION_ID and R.CUSTOMER_PROFILE_ID = C.CUSTOMER_PROFILE_ID AND C.STATUS='AC' LIMIT ?", CustomerProfile.class);
        q.setParameter(1, orgName + "%");
        q.setParameter(2, limit);
        return (List<CustomerProfile>) q.getResultList();
    }

    public static List<CustomerProfile> getCustomerProfilesByWildcardedLastName(EntityManager em, String lastName, int limit) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_profile WHERE LAST_NAME LIKE ? AND STATUS='AC' LIMIT ?", CustomerProfile.class);
        q.setParameter(1, lastName + "%");
        q.setParameter(2, limit);
        return (List<CustomerProfile>) q.getResultList();
    }

    public static List<CustomerProfile> getCustomerProfileByIDNumber(EntityManager em, String idNumber, int limit) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_profile WHERE ID_NUMBER = ? AND STATUS='AC' LIMIT ?", CustomerProfile.class);
        q.setParameter(1, idNumber);
        q.setParameter(2, limit);
        return (List<CustomerProfile>) q.getResultList();
    }
    
    public static List<CustomerProfile> getCustomerProfileByNationalIDNumber(EntityManager em, String nationalIdNumber, int limit) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_profile WHERE NATIONAL_ID_NUMBER = ? AND STATUS='AC' LIMIT ?", CustomerProfile.class);
        q.setParameter(1, nationalIdNumber);
        q.setParameter(2, limit);
        return (List<CustomerProfile>) q.getResultList();
    }

    public static List<CustomerProfile> getCustomerProfileByAlternativeContactNumber(EntityManager em, String alternativeContactNumber, int limit) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_profile WHERE ALTERNATIVE_CONTACT_1 = ? OR ALTERNATIVE_CONTACT_2 = ? AND STATUS='AC' LIMIT ?", CustomerProfile.class);
        q.setParameter(1, alternativeContactNumber);
        q.setParameter(2, alternativeContactNumber);
        q.setParameter(3, limit);
        return (List<CustomerProfile>) q.getResultList();
    }

    public static List<String> getCustomersSecurityGroups(EntityManager em, int customerId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.security_group_membership WHERE CUSTOMER_PROFILE_ID=?", SecurityGroupMembership.class);
        q.setParameter(1, customerId);
        List<SecurityGroupMembership> groups = q.getResultList();
        List<String> ret = new ArrayList<>();
        for (SecurityGroupMembership group : groups) {
            ret.add(group.getSecurityGroupMembershipPK().getSecurityGroupName());
        }
        return ret;
    } 
    
    public static List<Integer> getCustomersSellers(EntityManager em, int customerId) {        
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_sellers WHERE CUSTOMER_PROFILE_ID=?", CustomerSellers.class);
        q.setParameter(1, customerId);
        List<CustomerSellers> sellers = q.getResultList();
        List<Integer> ret = new ArrayList<>();
        for (CustomerSellers seller : sellers) {
            ret.add(seller.getCustomerSellersPK().getSellerProfileId());
        }        
        return ret;
    }
    
    public static int getCustomerProfileIdBySSOIdentity(EntityManager em, String originatingIdentity) {
        Query q = em.createNativeQuery("SELECT customer_profile_id FROM SmileDB.customer_profile WHERE SSO_IDENTITY = ? AND STATUS='AC'");
        q.setParameter(1, originatingIdentity);
        return (Integer) q.getSingleResult();
    }

    public static List<CustomerProfile> getCustomerProfilesByKYCStatus(EntityManager em, String kycStatus, int limit) {
        //requested by TZ to give priority to diplomats for kyc verification. 
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_profile WHERE KYC_STATUS=? ORDER BY FIELD(classification,'diplomat') DESC LIMIT ?", CustomerProfile.class);
        q.setParameter(1, kycStatus);
        q.setParameter(2, limit);
        return (List<CustomerProfile>) q.getResultList();
    }
    
    public static List<CustomerProfile> getCustomerProfilesByNinVerificationStatus(EntityManager em, String isNinVerified, int limit) {
        //requested by NG to give priority to diplomats for kyc verification. 
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_profile WHERE NATIONAL_ID_NUMBER != '' and NATIONAL_ID_NUMBER is not null and IS_NIN_VERIFIED=? LIMIT ?", CustomerProfile.class);
        q.setParameter(1, isNinVerified);
        q.setParameter(2, limit);
        return (List<CustomerProfile>) q.getResultList();
    }

    public static CustomerProfile getCustomerProfileBySSOIdentity(EntityManager em, String ssoId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_profile WHERE SSO_IDENTITY = ? AND STATUS='AC'", CustomerProfile.class);
        q.setParameter(1, ssoId);
        return (CustomerProfile) q.getSingleResult();
    }

    public static CustomerProfile getLockedCustomerProfileBySSOIdentity(EntityManager em, String ssoId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_profile WHERE SSO_IDENTITY = ? AND STATUS='AC' FOR UPDATE", CustomerProfile.class);
        q.setParameter(1, ssoId);
        return (CustomerProfile) q.getSingleResult();
    }

    public static List<Address> getCustomersAddresses(EntityManager em, int customerProfileId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.address WHERE CUSTOMER_PROFILE_ID = ?", Address.class);
        q.setParameter(1, customerProfileId);
        return (List<Address>) q.getResultList();
    }

    public static Address getAddress(EntityManager em, int addressId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.address WHERE ADDRESS_ID = ?", Address.class);
        q.setParameter(1, addressId);
        return (Address) q.getSingleResult();
    }

    public static void deleteAddress(EntityManager em, int addressId) {
        Query q = em.createNativeQuery("DELETE  FROM SmileDB.address WHERE ADDRESS_ID = ?");
        q.setParameter(1, addressId);
        q.executeUpdate();
    }

    public static void setCustomersSecurityGroups(EntityManager em, List<String> securityGroups, int customerId) {
        Query q = em.createNativeQuery("DELETE FROM SmileDB.security_group_membership WHERE CUSTOMER_PROFILE_ID = ?");
        q.setParameter(1, customerId);
        q.executeUpdate();
        for (String group : securityGroups) {
            SecurityGroupMembership m = new SecurityGroupMembership();
            m.setSecurityGroupMembershipPK(new SecurityGroupMembershipPK());
            m.getSecurityGroupMembershipPK().setCustomerProfileId(customerId);
            m.getSecurityGroupMembershipPK().setSecurityGroupName(group);
            em.persist(m);
        }
        em.flush();
    }
    
    public static void setCustomersSellers(EntityManager em, List<Integer> sellers, int customerId) {
        Query q = em.createNativeQuery("DELETE FROM SmileDB.customer_sellers WHERE CUSTOMER_PROFILE_ID = ?");
        q.setParameter(1, customerId);
        q.executeUpdate();
        for (int seller : sellers) {            
            CustomerSellers m = new CustomerSellers();
            m.setCustomerSellersPK(new CustomerSellersPK());
            m.getCustomerSellersPK().setCustomerProfileId(customerId);
            m.getCustomerSellersPK().setSellerProfileId(seller);    
            log.warn("Setting CustSeller");
            em.persist(m);            
        }
        em.flush();
    }
    
    public static void setOrganisationSellers(EntityManager em, List<Integer> sellers, int organisationId) {
        log.warn("InModifySellers");
        Query q = em.createNativeQuery("DELETE FROM SmileDB.organisation_sellers WHERE ORGANISATION_ID = ?");
        q.setParameter(1, organisationId);
        q.executeUpdate();
        for (int seller : sellers) {            
            OrganisationSellers m = new OrganisationSellers();
            m.setOrganisationSellersPK(new OrganisationSellersPK());
            m.getOrganisationSellersPK().setOrganisationId(organisationId);
            m.getOrganisationSellersPK().setSellerOrganisationId(seller);    
            log.warn("Setting OrgSeller");
            em.persist(m);            
        }
        em.flush();
    }
    
    public static void setCustomersPhotographs(EntityManager em, List<com.smilecoms.xml.schema.im.Photograph> customerPhotoGraphs, int customerId) throws Exception {
        Query q = em.createNativeQuery("DELETE FROM SmileDB.photograph WHERE CUSTOMER_PROFILE_ID = ?");
        q.setParameter(1, customerId);
        q.executeUpdate();
        for (com.smilecoms.xml.schema.im.Photograph p : customerPhotoGraphs) {
            if (!p.getPhotoGuid().isEmpty() && !p.getPhotoType().isEmpty()) {

                Photograph photo = new Photograph();
                photo.setCustomerProfileId(customerId);
                photo.setPhotoGuid(p.getPhotoGuid());
                photo.setPhotoType(p.getPhotoType());
                if (p.getData() == null || p.getData().length() < 1000) {
                    throw new Exception("Invalid/empty photograph -- " + photo.getPhotoType());
                }

                photo.setData(Utils.decodeBase64(p.getData()));
                photo.setPhotoHash(HashUtils.md5(photo.getData()));

                //In some countries, like Tanzania, fingerprints are not allowed to be stored on the system.
                if (p.getPhotoType() != null
                        && p.getPhotoType().equalsIgnoreCase("fingerprints")
                        && BaseUtils.getBooleanProperty("env.kyc.do.not.store.fingerprints", false)) {
                    photo.setData(null); // Remove fingerprints
                }
                
                if(!photo.getPhotoType().equalsIgnoreCase("profilephoto")) { //Allow propic to be any pic client chooses
                    Query q2 = em.createNativeQuery("select concat(ifnull(customer_profile_id,''),ifnull(organisation_id,''),ifnull(service_instance_id,'')) FROM SmileDB.photograph WHERE PHOTO_HASH = ? limit 1");
                    q2.setParameter(1, photo.getPhotoHash());                
                    List res = q2.getResultList();
                    if (!res.isEmpty()) {
                        throw new Exception("Duplicate photograph -- " + photo.getPhotoType() + " exists on a customer or organisation or service instance with Id " + res.get(0));
                    }
                }
                em.persist(photo);
            }
        }
        em.flush();
    }

    public static List<com.smilecoms.xml.schema.im.Photograph> getCustomersPhotographs(EntityManager em, int customerId) throws Exception {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.photograph WHERE CUSTOMER_PROFILE_ID=?", Photograph.class);
        q.setParameter(1, customerId);
        List<Photograph> photos = q.getResultList();
        List<com.smilecoms.xml.schema.im.Photograph> ret = new ArrayList<>();
        for (Photograph photo : photos) {
            //In some countries, fingerprints are not supposed to be stored, so we by-pass them here. We are only storing the Hash so that we can check if the fingerprint was used before.
            if (photo.getPhotoType() != null
                    && photo.getPhotoType().equalsIgnoreCase("fingerprints")
                    && BaseUtils.getBooleanProperty("env.kyc.do.not.store.fingerprints", false)) {
                continue; //Do not add fingerprints on the retrieved list.
            }

            com.smilecoms.xml.schema.im.Photograph cp = new com.smilecoms.xml.schema.im.Photograph();
            cp.setPhotoGuid(photo.getPhotoGuid());
            cp.setPhotoType(photo.getPhotoType());
            cp.setData(Utils.encodeBase64(photo.getData()));
            ret.add(cp);
            log.debug("Got a photo from DB with GUID [{}]", cp.getPhotoGuid());
        }
        return ret;
    }

    public static void checkIfDocumentHasBeenUsedBefore(EntityManager em, String photoHash) throws Exception {

        log.debug("Going to check if there is a document with hash [{}]", photoHash);

        Query q2 = em.createNativeQuery("select concat(ifnull(concat('Contract:',contract_id),''), ifnull(concat('Customer:',customer_profile_id),''),ifnull(concat('Organisation:', organisation_id),''), ifnull(concat('Service Instance:', service_instance_id),'')) FROM SmileDB.photograph WHERE PHOTO_HASH = ? limit 1");
        q2.setParameter(1, photoHash);
        List res = q2.getResultList();
        if (!res.isEmpty()) {
            throw new Exception("Duplicate photograph -- " + " this document has already been used on business entity [" + res.get(0) + "].");
        }

        log.warn("Document does not exist, hash [{}]", photoHash);
    }

    public static void setOrganisationsPhotographs(EntityManager em, List<com.smilecoms.xml.schema.im.Photograph> organisationsPhotoGraphs, int organisationId) throws Exception {
        Query q = em.createNativeQuery("DELETE FROM SmileDB.photograph WHERE ORGANISATION_ID = ?");
        q.setParameter(1, organisationId);
        q.executeUpdate();
        for (com.smilecoms.xml.schema.im.Photograph p : organisationsPhotoGraphs) {
            if (!p.getPhotoGuid().isEmpty() && !p.getPhotoType().isEmpty()) {
                Photograph photo = new Photograph();
                photo.setOrganisationId(organisationId);
                photo.setPhotoGuid(p.getPhotoGuid());
                photo.setPhotoType(p.getPhotoType());
                if (p.getData() == null || p.getData().length() < 1000) {
                    throw new Exception("Invalid/empty photograph -- " + photo.getPhotoType());
                }
                photo.setData(Utils.decodeBase64(p.getData()));
                photo.setPhotoHash(HashUtils.md5(photo.getData()));

                //In some countries, like Tanzania, fingerprints are not allowed to be stored on the system.
                if (p.getPhotoType() != null
                        && p.getPhotoType().equalsIgnoreCase("fingerprints")
                        && BaseUtils.getBooleanProperty("env.kyc.do.not.store.fingerprints", false)) {
                    photo.setData(null); // Remove fingerprints
                }

                Query q2 = em.createNativeQuery("select concat(ifnull(contract_id,''), ifnull(customer_profile_id,''),ifnull(organisation_id,'')) FROM SmileDB.photograph WHERE PHOTO_HASH = ? limit 1");
                q2.setParameter(1, photo.getPhotoHash());
                List res = q2.getResultList();
                if (!res.isEmpty()) {
                    throw new Exception("Duplicate photograph -- " + photo.getPhotoType() + " exists on a customer, contract or organisation with Id " + res.get(0));
                }
                em.persist(photo);
            }
        }
        em.flush();
    }

    public static List<com.smilecoms.xml.schema.im.Photograph> getOrganisationsPhotographs(EntityManager em, int organisationId) throws Exception {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.photograph WHERE ORGANISATION_ID=?", Photograph.class);
        q.setParameter(1, organisationId);
        List<Photograph> photos = q.getResultList();
        List<com.smilecoms.xml.schema.im.Photograph> ret = new ArrayList<>();
        for (Photograph photo : photos) {

            //In some countries, fingerprints are not supposed to be stored, so we by-pass them here. We are only storing the Hash so that we can check if the fingerprint was used before.
            if (photo.getPhotoType() != null
                    && photo.getPhotoType().equalsIgnoreCase("fingerprints")
                    && BaseUtils.getBooleanProperty("env.kyc.do.not.store.fingerprints", false)) {
                continue; //Do not add fingerprints on the retrieved list.
            }

            com.smilecoms.xml.schema.im.Photograph cp = new com.smilecoms.xml.schema.im.Photograph();
            cp.setPhotoGuid(photo.getPhotoGuid());
            cp.setPhotoType(photo.getPhotoType());
            cp.setData(Utils.encodeBase64(photo.getData()));
            ret.add(cp);
            log.debug("Got a photo from DB with GUID [{}]", cp.getPhotoGuid());
        }
        return ret;
    }

    public static SsoPasswordReset getSSOPasswordReset(EntityManager em, String guid) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.sso_password_reset WHERE GUID=?", SsoPasswordReset.class);
        q.setParameter(1, guid);
        return (SsoPasswordReset) q.getSingleResult();
    }

    public static List<ThirdPartyAuthorisationRuleSet> getThirdPartyAuthorisationRuleSets(EntityManager em, int customerId) {
        Query q = em.createNativeQuery("SELECT DISTINCT(rule_set_id)  FROM SmileDB.authorisation_rule WHERE customer_profile_id = ?");
        q.setParameter(1, customerId);

        List<ThirdPartyAuthorisationRuleSet> xmlRuleSets = new ArrayList<>();

        List<Integer> ruleSetIds = q.getResultList();

        ThirdPartyAuthorisationRuleSet xmlRuleSet;

        for (Integer ruleSetId : ruleSetIds) {
            xmlRuleSet = new ThirdPartyAuthorisationRuleSet();
            xmlRuleSet.setRuleSetId(ruleSetId);

            // - Get the rules belonging to this rule set
            q = em.createNativeQuery("SELECT *  FROM SmileDB.authorisation_rule WHERE customer_profile_id = ? and rule_set_id = ? ", AuthorisationRule.class);
            q.setParameter(1, customerId);
            q.setParameter(2, ruleSetId);

            List<AuthorisationRule> dbAuthRules = q.getResultList();

            for (AuthorisationRule dbAuthRule : dbAuthRules) {
                ThirdPartyAuthorisationRule tpAuthRule = new ThirdPartyAuthorisationRule();
                tpAuthRule.setDescription(dbAuthRule.getDescription());
                tpAuthRule.setRegexMatch(dbAuthRule.getRegexMatch());
                tpAuthRule.setRuleId(dbAuthRule.getRuleId());
                tpAuthRule.setXQuery(dbAuthRule.getXquery());
                xmlRuleSet.getThirdPartyAuthorisationRules().add(tpAuthRule);
            }
            xmlRuleSets.add(xmlRuleSet);
        }
        return xmlRuleSets;
    }

    public static List<String> getCustomersOutstandingTermsAndConditions(EntityManager em, int customerId) {

        Query q = em.createNativeQuery("SELECT *  FROM SmileDB.terms_conditions WHERE customer_profile_id = ? and status='PE'", TermsConditions.class);
        q.setParameter(1, customerId);
        List<TermsConditions> res = q.getResultList();
        List<String> ret = new ArrayList<>();
        for (TermsConditions tc : res) {
            ret.add(tc.getTermsConditionsPK().getTCResourceKey());
        }
        return ret;
    }

    public static void setCustomersTermsAndConditionsAccepted(EntityManager em, int customerId, String tcResourceKey) {
        Query q = em.createNativeQuery("update SmileDB.terms_conditions set  status='AC' where customer_profile_id = ? and t_c_resource_key = ?");
        q.setParameter(1, customerId);
        q.setParameter(2, tcResourceKey);
        q.executeUpdate();
    }

    public static void setCustomerProfileUpdatedDatetime(EntityManager em, int customerId) {
        Query q = em.createNativeQuery("update SmileDB.customer_profile set  updated_datetime = NOW() where customer_profile_id = ?");
        q.setParameter(1, customerId);
        q.executeUpdate();
    }

    public static Organisation getOrganisationByOrganisationId(EntityManager em, int organisationId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.organisation WHERE ORGANISATION_ID=?", Organisation.class);
        q.setParameter(1, organisationId);
        return (Organisation) q.getSingleResult();
    }
    
    public static Iterable<Organisation> getAllOrganisations(EntityManager em) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.organisation limit 20", Organisation.class);                
        return (List<Organisation>) q.getResultList();
    }
    
    public static List<Integer> getOrganisationSellers(EntityManager em, int organisationId) {        
        Query q = em.createNativeQuery("SELECT DISTINCT * FROM SmileDB.organisation_sellers WHERE ORGANISATION_ID=?", OrganisationSellers.class);
        q.setParameter(1, organisationId);
        List<OrganisationSellers> sellers = q.getResultList();
        
        log.warn("Found {} sellers for this guy", sellers.size());
        List<Integer> ret = new ArrayList<>();
        for (OrganisationSellers seller : sellers) {
            log.warn("Adding seller {}", seller.getOrganisationSellersPK().getSellerOrganisationId());
            ret.add(seller.getOrganisationSellersPK().getSellerOrganisationId());
        }       
        log.warn("Returning All {} sellers", ret.size());
        return ret;
    }

    public static Iterable<Organisation> getOrganisationsByWildcardedName(EntityManager em, String organisationName, int limit) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.organisation WHERE ORGANISATION_NAME LIKE ? LIMIT ?", Organisation.class);
        q.setParameter(1, "%" + organisationName + "%");
        q.setParameter(2, limit);
        return (List<Organisation>) q.getResultList();
    }

    public static Iterable<Organisation> getOrganisationsByCustomerProfileId(EntityManager em, int customerId, int limit) {
        Query q = em.createNativeQuery("SELECT O.* FROM SmileDB.organisation O, SmileDB.customer_role R WHERE R.CUSTOMER_PROFILE_ID = ? AND R.ORGANISATION_ID = O.ORGANISATION_ID LIMIT ?", Organisation.class);
        q.setParameter(1, customerId);
        q.setParameter(2, limit);
        return (List<Organisation>) q.getResultList();
    }

    public static List<com.smilecoms.xml.schema.im.CustomerRole> getCustomerRolesByCustomerProfileId(EntityManager em, int customerId) {
        Query q = em.createNativeQuery("SELECT R.CUSTOMER_PROFILE_ID, R.ORGANISATION_ID, R.ROLE, O.ORGANISATION_NAME, concat(C.FIRST_NAME,' ', C.LAST_NAME) "
                + "FROM SmileDB.organisation O, SmileDB.customer_role R, SmileDB.customer_profile C "
                + "WHERE R.CUSTOMER_PROFILE_ID = ? AND R.ORGANISATION_ID = O.ORGANISATION_ID AND R.CUSTOMER_PROFILE_ID = C.CUSTOMER_PROFILE_ID AND C.STATUS='AC' ORDER BY O.ORGANISATION_ID");
        q.setParameter(1, customerId);
        List<com.smilecoms.xml.schema.im.CustomerRole> roles = new ArrayList<>();
        List<Object[]> objects = q.getResultList();
        for (Object[] arr : objects) {
            com.smilecoms.xml.schema.im.CustomerRole role = new com.smilecoms.xml.schema.im.CustomerRole();
            role.setCustomerId((Integer) arr[0]);
            role.setOrganisationId((Integer) arr[1]);
            role.setRoleName((String) arr[2]);
            role.setOrganisationName((String) arr[3]);
            role.setCustomerName((String) arr[4]);
            roles.add(role);
        }
        return roles;
    }

    public static List<com.smilecoms.xml.schema.im.CustomerRole> getCustomerRolesByOrganisationId(EntityManager em, int organisationId, int offset, int limit) {
        log.debug("Limit [{}] Offset [{}]", limit, offset);
        if (limit <= 0) {
            limit = Integer.MAX_VALUE;
        }
        if (offset < 0) {
            offset = 0;
        }
        Query q = em.createNativeQuery("SELECT R.CUSTOMER_PROFILE_ID, R.ORGANISATION_ID, R.ROLE, O.ORGANISATION_NAME, concat(C.FIRST_NAME,' ', C.LAST_NAME) "
                + "FROM SmileDB.organisation O, SmileDB.customer_role R, SmileDB.customer_profile C "
                + "WHERE R.ORGANISATION_ID = ? AND R.ORGANISATION_ID = O.ORGANISATION_ID AND R.CUSTOMER_PROFILE_ID = C.CUSTOMER_PROFILE_ID AND C.STATUS='AC' ORDER BY concat(C.FIRST_NAME,' ', C.LAST_NAME) LIMIT ? OFFSET ?");
        q.setParameter(1, organisationId);
        q.setParameter(2, limit);
        q.setParameter(3, offset);
        List<com.smilecoms.xml.schema.im.CustomerRole> roles = new ArrayList<>();
        List<Object[]> objects = q.getResultList();
        for (Object[] arr : objects) {
            com.smilecoms.xml.schema.im.CustomerRole role = new com.smilecoms.xml.schema.im.CustomerRole();
            role.setCustomerId((Integer) arr[0]);
            role.setOrganisationId((Integer) arr[1]);
            role.setRoleName((String) arr[2]);
            role.setOrganisationName((String) arr[3]);
            role.setCustomerName((String) arr[4]);
            roles.add(role);
        }
        return roles;
    }

    public static void setCustomerRoles(EntityManager em, List<com.smilecoms.xml.schema.im.CustomerRole> customerRoles, int customerId) {
        Query q = em.createNativeQuery("DELETE FROM SmileDB.customer_role WHERE CUSTOMER_PROFILE_ID = ?");
        q.setParameter(1, customerId);
        q.executeUpdate();
        for (com.smilecoms.xml.schema.im.CustomerRole role : customerRoles) {
            CustomerRolePK key = new CustomerRolePK();
            key.setCustomerProfileId(customerId);
            key.setOrganisationId(role.getOrganisationId());
            CustomerRole r = new CustomerRole();
            r.setCustomerRolePK(key);
            r.setRole(role.getRoleName());
            em.persist(r);
        }
        em.flush();
    }

    public static List<Address> getOrganisationsAddresses(EntityManager em, int organisationId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.address WHERE ORGANISATION_ID = ?", Address.class);
        q.setParameter(1, organisationId);
        return (List<Address>) q.getResultList();
    }

    public static String getOrganisationModificationRoles(EntityManager em, int organisationId) {
        Query q = em.createNativeQuery("SELECT MODIFICATION_ROLES FROM SmileDB.organisation  WHERE ORGANISATION_ID = ?");
        q.setParameter(1, organisationId);

        return (String) q.getSingleResult();
    }

    public static boolean stateExists(EntityManager em, String state) {
        Query q = em.createNativeQuery("SELECT count(*) FROM SmileDB.state WHERE STATE_NAME = ?");
        q.setParameter(1, state);
        Long cnt = (Long) q.getSingleResult();
        return cnt >= 1;
    }

    public static boolean countryExists(EntityManager em, String country) {
        Query q = em.createNativeQuery("SELECT count(*) FROM SmileDB.country WHERE COUNTRY_NAME = ?");
        q.setParameter(1, country);
        Long cnt = (Long) q.getSingleResult();
        return cnt >= 1;
    }

    public static boolean zoneExists(EntityManager em, String zone, String state) {
        Query q = em.createNativeQuery("SELECT count(*) FROM SmileDB.zone join SmileDB.state on state.STATE_ID = zone.STATE_ID WHERE zone.ZONE_NAME = ? and state.STATE_NAME=?");
        q.setParameter(1, zone);
        q.setParameter(2, state);
        Long cnt = (Long) q.getSingleResult();
        return cnt >= 1;
    }

    public static Photograph getPhoto(EntityManager em, String val) {
        Query q = em.createNativeQuery("select * from SmileDB.photograph where photo_guid=?", Photograph.class);
        q.setParameter(1, val);
        return (Photograph) q.getSingleResult();
    }

    public static String getTenantBySSOIdentity(EntityManager em, String ssoIdentity) {
        Query q = em.createNativeQuery("select tenant from SmileDB.customer_profile where sso_identity=?");
        q.setParameter(1, ssoIdentity);
        return (String) q.getSingleResult();
    }

    public static UiccDetails getUICCDetailsByICCID(EntityManager em, String iccid) {
        UiccDetails ret;
        try {
            Query q = em.createNativeQuery("select * from SmileDB.uicc_details where iccid=?", UiccDetails.class);
            q.setParameter(1, iccid);
            ret = (UiccDetails) q.getSingleResult();
        } catch (Exception ex) {
            log.debug("No uicc record found for iccid {}", iccid);
            ret = new UiccDetails();
            ret.setADM1("");
            ret.setICCID(iccid);
            ret.setPIN1("");
            ret.setPIN2("");
            ret.setPUK1("");
            ret.setPUK2("");
        }
        return ret;
    }

    public static NetworkAccessIdentifier getNetworkAccessIdentifierByIdentity(EntityManager em, String identity) {
        Query q = em.createNativeQuery("select * from SmileDB.network_access_identifier where nai_username=?", NetworkAccessIdentifier.class);
        q.setParameter(1, identity);
        NetworkAccessIdentifier ret = (NetworkAccessIdentifier) q.getSingleResult();
        return ret;
    }

    public static NetworkAccessIdentifier getNetworkAccessIdentifierByIdentityId(EntityManager em, int naiIdentityId) {
        Query q = em.createNativeQuery("select * from SmileDB.network_access_identifier where id=?", NetworkAccessIdentifier.class);
        q.setParameter(1, naiIdentityId);
        NetworkAccessIdentifier ret = (NetworkAccessIdentifier) q.getSingleResult();
        return ret;
    }

    public static NetworkAccessIdentifier getNetworkAccessIdentifierByOSSBSSReferenceId(EntityManager em, String ossbssReferenceId) {
        Query q = em.createNativeQuery("select * from SmileDB.network_access_identifier where ossbss_reference_id=?", NetworkAccessIdentifier.class);
        q.setParameter(1, ossbssReferenceId);
        NetworkAccessIdentifier ret = (NetworkAccessIdentifier) q.getSingleResult();
        return ret;
    }

    public static boolean pendingIMSSubscriptionsExists(EntityManager em, String impu) {
        Query q = em.createNativeQuery("select count(*) from SmileDB.event_subscription where TYPE='IMS_USER_STATE' and SUB_TYPE='REGISTERED' and EVENT_KEY=? and EXPIRY_TIMESTAMP >= now()");
        q.setParameter(1, impu);
        Long cnt = (Long) q.getSingleResult();
        return cnt >= 1;
    }

    public static void setCustomersClassification(EntityManager em, int customerId) {
        Query q = em.createNativeQuery("update SmileDB.customer_profile set classification='diplomat' where customer_profile_id = ?");
        q.setParameter(1, customerId);
        q.executeUpdate();
        log.debug("classification for customer [[]] is updated", customerId);
    }

    public static MandatoryKYCFields getCustomersMandatoryKYCFields(EntityManager em, int customerProfileId) {
        MandatoryKYCFields mkf;
        try {
            Query q = em.createNativeQuery("SELECT * FROM SmileDB.mandatorykycfields WHERE customerId = ?", MandatoryKYCFields.class);
            q.setParameter(1, customerProfileId);
            mkf = (MandatoryKYCFields) q.getSingleResult();
        } catch (Exception e) {
            return null;
        }
        return mkf;
    }
    
    public static List<CustomerProfile> getCustomerProfilesBySecurityGroup(EntityManager em, String securityGroup, int resultLimit) {
        
        Query q = em.createNativeQuery("select distinct cp.* from customer_profile cp inner join security_group_membership sgm on " +
                                        "cp.CUSTOMER_PROFILE_ID = sgm.CUSTOMER_PROFILE_ID inner join customer_role cr on cr.CUSTOMER_PROFILE_ID = cp.CUSTOMER_PROFILE_ID " +
                                        "where sgm.SECURITY_GROUP_NAME like ? LIMIT ?", CustomerProfile.class);
        q.setParameter(1, securityGroup);        
        q.setParameter(2, resultLimit);
        
        return (List<CustomerProfile>) q.getResultList();
    }
    
    public static CustomerNinData getCustomerNinDatas(EntityManager em, int customerProfileId) {
        CustomerNinData cnd;
        try {
            Query q = em.createNativeQuery("SELECT * FROM SmileDB.customer_nin_data WHERE customer_profile_id = ?", CustomerNinData.class);
            q.setParameter(1, customerProfileId);
            cnd = (CustomerNinData) q.getSingleResult();
        } catch (Exception e) {
            return null;
        }
        return cnd;
    }
}
