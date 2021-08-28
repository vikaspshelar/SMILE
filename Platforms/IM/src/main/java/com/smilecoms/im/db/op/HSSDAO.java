/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.op;

import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.im.IMDataCache;
import com.smilecoms.im.db.model.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class HSSDAO {

    private static final Logger log = LoggerFactory.getLogger(HSSDAO.class);

    /**
     * This will remove a subscription and all impi, impu, mappings and visited
     * network entries that are related to it
     *
     * @param em
     * @param id
     */
    public static void deleteSubscriptionCascaded(EntityManager em, int id) {
        Imsu imsu = JPAUtils.findAndThrowENFE(em, Imsu.class, id);
        Collection<Impi> impiList = imsu.getImpiCollection();
        for (Impi impi : impiList) {
            Collection<ImpiImpu> impiimpuList = impi.getImpiImpuCollection();
            for (ImpiImpu impiImpu : impiimpuList) {
                Impu impu = impiImpu.getImpu();
                Collection<ImpuVisitedNetwork> impuVisitedNetworkList = impu.getImpuVisitedNetworkCollection();
                for (ImpuVisitedNetwork impuVisitedNetwork : impuVisitedNetworkList) {
                    log.warn("Deleting impuVisitedNetwork [{}]", impuVisitedNetwork.getIdVisitedNetwork());
                    em.remove(impuVisitedNetwork);
                }
                log.warn("Deleting impiImpu [{}]", impiImpu.getId());
                em.remove(impiImpu);
                log.warn("Deleting impu [{}]", impu.getId());
                em.remove(impu);
            }
            log.warn("Deleting impi [{}]", impi.getId());
            em.remove(impi);
        }
        log.warn("Deleting imsu [{}]", imsu.getId());
        em.remove(imsu);
        em.flush();
    }

    public static Impi getImpiByImpuIdentity(EntityManager em, String impu) {
        Query q = em.createNativeQuery("select impi.* from impi, impi_impu, impu where impi_impu.id_impi=impi.id and impi_impu.id_impu=impu.id and impu.identity=? limit 1", Impi.class);
        q.setParameter(1, impu);
        Impi ret = (Impi) q.getSingleResult();
        return ret;
    }

    public static List<Impi> getImpiListByImpuIdentity(EntityManager em, String impu) {
        Query q = em.createNativeQuery("select impi.* from impi, impi_impu, impu where impi_impu.id_impi=impi.id and impi_impu.id_impu=impu.id and impu.identity=?", Impi.class);
        q.setParameter(1, impu);
        return q.getResultList();
    }

    public static Impi getImpiByIdentity(EntityManager em, String id) {
        Query q = em.createNativeQuery("select * from impi where impi.identity=?", Impi.class);
        q.setParameter(1, id);
        Impi ret = (Impi) q.getSingleResult();
        return ret;
    }

    public static Impi getImpiByICCID(EntityManager em, String iccid) {
        Query q = em.createNativeQuery("select * from impi where iccid=?", Impi.class);
        q.setParameter(1, iccid);
        Impi ret = (Impi) q.getSingleResult();
        return ret;
    }

    public static Impi getImpiByIdentityId(EntityManager em, int id) {
        Query q = em.createNativeQuery("select * from impi where impi.id=?", Impi.class);
        q.setParameter(1, id);
        Impi ret = (Impi) q.getSingleResult();
        return ret;
    }

    public static Impi getImpiByOSSBSSReferenceId(EntityManager em, String id) {
        Query q = em.createNativeQuery("select * from impi where impi.ossbss_reference_id=?", Impi.class);
        q.setParameter(1, id);
        Impi ret = (Impi) q.getSingleResult();
        return ret;
    }

    public static Impi getImpiBySimLockedImeiList(EntityManager em, String ip) {
        Query q = em.createNativeQuery("select * from impi where impi.sim_locked_imei_list=?", Impi.class);
        q.setParameter(1, ip);
        Impi ret = (Impi) q.getSingleResult();
        return ret;
    }

    public static Set<String> getImpiAppKeys(EntityManager em, int idIMPI) {
        Query q = em.createNativeQuery("select * from app_k where id_impi=?", AppK.class);
        q.setParameter(1, idIMPI);
        List<AppK> rows = q.getResultList();
        Set<String> ret = new HashSet<>();
        for (AppK row : rows) {
            ret.add(row.getAppKPK().getAppK());
        }
        return ret;
    }

    public static void addAppK(EntityManager em, int idIMPI, String appK) {
        Query q = em.createNativeQuery("insert into app_k values(?,?)");
        q.setParameter(1, idIMPI);
        q.setParameter(2, appK);
        try {
            q.executeUpdate();
        } catch (Exception e) {
            log.debug("Error adding app_k. It probably exists [{}]", e.toString());
        }
    }

    public static ImpiImpu createImpiImpu(EntityManager em, int imsPrivateIdentityId, int imsPublicIdentityId, int userState) {
        Query q = em.createNativeQuery("insert into impi_impu (id_impi,id_impu,user_state) values (?,?,?)");
        q.setParameter(1, imsPrivateIdentityId);
        q.setParameter(2, imsPublicIdentityId);
        q.setParameter(3, userState);
        q.executeUpdate();
        q = em.createNativeQuery("select * from impi_impu where id_impi=? and id_impu=?", ImpiImpu.class);
        q.setParameter(1, imsPrivateIdentityId);
        q.setParameter(2, imsPublicIdentityId);
        return (ImpiImpu) q.getSingleResult();
    }

    public static void clearIMPIAppKs(EntityManager em, int idIMPI) {
        Query q = em.createNativeQuery("delete from app_k where id_impi=?");
        q.setParameter(1, idIMPI);
        q.executeUpdate();
    }

    public static Impu getImpuByIdentity(EntityManager em, String identity) {
        Query q = em.createNativeQuery("select * from impu where impu.identity=?", Impu.class);
        q.setParameter(1, identity);
        Impu ret = (Impu) q.getSingleResult();
        return ret;
    }

    public static Impu getImpuByIdentityId(EntityManager em, int id) {
        Query q = em.createNativeQuery("select * from impu where impu.id=?", Impu.class);
        q.setParameter(1, id);
        Impu ret = (Impu) q.getSingleResult();
        return ret;
    }

    public static List getImpuVisitedNetworksByIdentity(EntityManager em, String identity) {
        Query q = em.createNativeQuery("select * from impu_visited_network where id_impu=?", ImpuVisitedNetwork.class);
        q.setParameter(1, identity);
        List ret = q.getResultList();
        return ret;
    }

    public static Impu getWildcardImpuByIdentity(EntityManager em, String identity) {
        Query q = em.createNativeQuery("select * from impu where psi_activation != 0 and ? like wildcard_psi limit 1", Impu.class);
        q.setParameter(1, identity);
        Impu ret = (Impu) q.getSingleResult();
        return ret;
    }

    public static Sp getServiceProfileById(EntityManager em, int id) {
        Query q = em.createNativeQuery("select * from sp where id=?", Sp.class);
        q.setParameter(1, id);
        Sp ret = (Sp) q.getSingleResult();
        return ret;
    }

    public static List<String> getSCSCFList(EntityManager em, int idPreferredScscfSet) {
        Query q = em.createNativeQuery("select * from preferred_scscf_set where id_set=? and priority > 0 order by priority desc", PreferredScscfSet.class);
        q.setParameter(1, idPreferredScscfSet);
        List<String> ret = new ArrayList<>();

        List<PreferredScscfSet> setList = q.getResultList();
        for (PreferredScscfSet aSet : setList) {
            ret.add(aSet.getScscfName());
        }
        return ret;
    }

    public static List<PreferredScscfSet> getAllSCSCFs(EntityManager em) {
        Query q = em.createNativeQuery("select * from preferred_scscf_set where scscf_name != '' order by id_set");
        return q.getResultList();
    }

    /*Get all scscfs with their current assigned subscriptions (effective load)*/
    public static List<Object[]> getAllSCSCFsWithLoadData(EntityManager em) {
        Query q = em.createNativeQuery("SELECT PSS.id_set, "
                + "       PSS.scscf_name, "
                + "       PSS.enabled, "
                + "       PSS.priority, "
                + "       count(*) "
                + "  FROM preferred_scscf_set PSS "
                + "       LEFT JOIN imsu ON imsu.scscf_name = PSS.scscf_name "
                + "GROUP BY PSS.id_set, PSS.scscf_name, PSS.enabled, PSS.priority "
                + "ORDER BY PSS.id_set ASC, PSS.enabled ASC");
        return q.getResultList();
    }

    public static List<String> getAllEnabledSCSCFs(EntityManager em) {
        Query q = em.createNativeQuery("select distinct scscf_name from preferred_scscf_set where enabled>=0 and scscf_name != ''");
        return q.getResultList();
    }

    public static Imsu getImsuByImpiIdentity(EntityManager em, String privateIdentity) {
        Query q = em.createNativeQuery("select imsu.* from imsu, impi where impi.id_imsu=imsu.id and impi.identity=?", Imsu.class);
        q.setParameter(1, privateIdentity);
        Imsu ret = (Imsu) q.getSingleResult();
        return ret;
    }

    public static Imsu getImsuByImpuIdentity(EntityManager em, String publicIdentity) {
        Query q = em.createNativeQuery("select imsu.* from imsu, impi, impi_impu, impu where impi.id_imsu=imsu.id and impi_impu.id_impi=impi.id and impi_impu.id_impu=impu.id and impu.identity=?", Imsu.class);
        q.setParameter(1, publicIdentity);
        List<Imsu> rows = q.getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    public static Imsu getImsuByPrivateIdentityId(EntityManager em, int privateIdentityId) {
        Query q = em.createNativeQuery("select imsu.* from imsu, impi where impi.id_imsu=imsu.id and impi.id=?", Imsu.class);
        q.setParameter(1, privateIdentityId);
        Imsu ret = (Imsu) q.getSingleResult();
        return ret;
    }

    public static Imsu getImsuByICCID(EntityManager em, String iccid) {
        Query q = em.createNativeQuery("select imsu.* from imsu, impi where impi.id_imsu=imsu.id and impi.iccid=?", Imsu.class);
        q.setParameter(1, iccid);
        Imsu ret = (Imsu) q.getSingleResult();
        return ret;
    }

    public static Imsu getImsuByImsuId(EntityManager em, int id) {
        Query q = em.createNativeQuery("select imsu.* from imsu where imsu.id=?", Imsu.class);
        q.setParameter(1, id);
        Imsu ret = (Imsu) q.getSingleResult();
        return ret;
    }

    public static Imsu getImsuByPublicIdentityId(EntityManager em, int publicIdentityId) {
        Query q = em.createNativeQuery("select imsu.* from imsu, impi, impi_impu, impu where impi.id_imsu=imsu.id and impi_impu.id_impi=impi.id and impi_impu.id_impu=impu.id and impu.id=?", Imsu.class);
        q.setParameter(1, publicIdentityId);
        List<Imsu> rows = q.getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    public static Imsu getImsuBygetOSSBSSReferenceId(EntityManager em, String ossbssReferenceId) {
        Query q = em.createNativeQuery("select imsu.* from imsu, impi where impi.id_imsu=imsu.id and impi.ossbss_reference_id=?", Imsu.class);
        q.setParameter(1, ossbssReferenceId);
        Imsu ret = (Imsu) q.getSingleResult();
        return ret;
    }

    public static int getRegisteredImpusCountForImsuId(EntityManager em, int id_imsu) {
        Query query = em.createNativeQuery("select count(*) from impi_impu inner join impi on impi.id=impi_impu.id_impi"
                + " where impi.id_imsu=? and impi_impu.user_state != 0");
        query.setParameter(1, id_imsu);
        Long result = (Long) query.getSingleResult();
        if (result == null) {
            return 0;
        }
        return result.intValue();
    }

    public static Imsu getImsuByImplicitSetId(EntityManager em, int implicitSetId) {
        Query q = em.createNativeQuery("select imsu.* from imsu, impi, impi_impu, impu where impi.id_imsu=imsu.id and impi_impu.id_impi=impi.id and impi_impu.id_impu=impu.id and impu.id_implicit_set=?", Imsu.class);
        q.setParameter(1, implicitSetId);
        List<Imsu> rows = q.getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    public static Impu getDefaultImpuForImpi(EntityManager em, Impi impi) {
        Query q = em.createNativeQuery("SELECT b.* FROM impi_impu a, impu b WHERE a.id_impi = ? AND a.id_impu = b.id AND id_sp = 1000", Impu.class);
        q.setParameter(1, impi.getId());
        return (Impu) q.getSingleResult();
    }

    public static void updateImpiSQN(EntityManager em, int id, String sqn) {
        Query q = em.createNativeQuery("UPDATE impi SET sqn = ? WHERE id = ?");
        q.setParameter(1, sqn);
        q.setParameter(2, id);
        try {
            q.executeUpdate();
        } catch (Exception e1) {
            log.debug("Got exception updating sequence. Going to try again: [{}]", e1.toString());
            try {
                q.executeUpdate();
            } catch (Exception e2) {
                log.debug("Got exception trying the second time: [{}]", e2.toString());
                throw e2;
            }
        }
    }

    public static void updateImpiToImpuMapping(EntityManager em, Impi oldImpi, Impi newImpi, int defaultImpu) {
        Query q = em.createNativeQuery("UPDATE impi_impu SET id_impi = ? WHERE id_impi = ? AND id_impu != ?");
        q.setParameter(1, newImpi.getId());
        q.setParameter(2, oldImpi.getId());
        q.setParameter(3, defaultImpu);
        q.executeUpdate();
    }

    public static void updateImpiToApnMapping(EntityManager em, Impi oldImpi, Impi newImpi) {
        Query q = em.createNativeQuery("UPDATE impi_apn SET id_impi = ? WHERE id_impi = ?");
        q.setParameter(1, newImpi.getId());
        q.setParameter(2, oldImpi.getId());
        q.executeUpdate();
    }

    public static List<Object[]> getIMSUDataStructure(EntityManager em, int imsuId) {
        long startMillis = 0;
        if (log.isDebugEnabled()) {
            log.debug("Getting IMSUDataStructure for imsuId [{}]", imsuId);
            startMillis = System.currentTimeMillis();
        }
        Query q = em.createNativeQuery("select  "
                + "imsu.id, imsu.name, imsu.scscf_name, imsu.diameter_name, imsu.id_capabilities_set, imsu.id_preferred_scscf_set, imsu.`version` as ver, "
                + "impi.id , impi.id_imsu, impi.identity, impi.k, impi.auth_scheme, impi.default_auth_scheme, impi.amf, impi.op, impi.sqn, impi.line_identifier, impi.iccid, impi.ossbss_reference_id, impi.sim_locked_imei_list, impi.public_k, impi.status, impi.info, "
                + "impi_impu.id, impi_impu.id_impi, impi_impu.id_impu, impi_impu.user_state, "
                + "impu.id, impu.identity, impu.type, impu.barring, impu.user_state, impu.id_sp, impu.id_implicit_set, impu.id_charging_info, impu.display_name, impu.can_register, impu.psi_activation, impu.wildcard_psi, "
                + "impi_apn.id, impi_apn.id_impi, impi_apn.apn_name, impi_apn.type, impi_apn.ipv4, impi_apn.ipv6 , "
                + "impu_visited_network.id, impu_visited_network.id_impu, impu_visited_network.id_visited_network "
                + "from imsu "
                + "left join impi on impi.id_imsu = imsu.id "
                + "left join impi_impu on impi_impu.id_impi = impi.id "
                + "left join impu ON impu.id = impi_impu.id_impu "
                + "left join impi_apn on impi_apn.id_impi = impi.id "
                + "left join impu_visited_network on impu_visited_network.id_impu = impu.id "
                + "where imsu.id=?");
        q.setParameter(1, imsuId);
        List<Object[]> ret = q.getResultList();

        if (log.isDebugEnabled()) {
            log.debug("Row count is [{}]", ret.size());
            if (!ret.isEmpty()) {
                Object[] row1 = ret.get(0);
                log.debug("Column count is [{}]", row1.length);
//                for (int i = 0; i < row1.length; i++) {
//                    log.debug("Type of column [{}] is [{}] and value is [{}]", new Object[]{i + 1, row1[i].getClass().getName(), row1[i]});
//                }
            }
            log.debug("got IMSUDataStructure for imsuId [{}] and took [{}]ms", imsuId, System.currentTimeMillis() - startMillis);
        }
        return ret;
    }

    /**
     * Do these in 1 SQL statement for best performance. May need to add more ?
     * in the in list if we need to update more later
     *
     * @param em
     * @param impiimpuStateUpdates
     * @param impuStateUpdates
     */
    public static void persistStateChanges(EntityManager em, Map<Integer, List<Integer>> impiimpuStateUpdates, Map<Integer, List<Integer>> impuStateUpdates) {
        Query q = em.createNativeQuery("update impi_impu, impu set impi_impu.user_state=?, impu.user_state=? where impu.id in (?,?,?,?,?) and impi_impu.id in (?,?,?,?,?)");

        Set<Integer> allStateChanges = new HashSet();
        allStateChanges.addAll(impiimpuStateUpdates.keySet());
        allStateChanges.addAll(impuStateUpdates.keySet());

        for (int newState : allStateChanges) {
            List<Integer> impiimpuIds = impiimpuStateUpdates.get(newState);
            if (impiimpuIds == null) {
                impiimpuIds = new ArrayList();
            }
            List<Integer> impuIds = impuStateUpdates.get(newState);
            if (impuIds == null) {
                impuIds = new ArrayList();
            }

            q.setParameter(1, newState);
            q.setParameter(2, newState);

            for (int i = 0; i < impuIds.size(); i++) {
                Integer impuId = impuIds.get(i);
                q.setParameter(i + 3, impuId);
            }
            for (int i = impuIds.size(); i < 5; i++) {
                q.setParameter(i + 3, -1);
            }

            for (int i = 0; i < impiimpuIds.size(); i++) {
                Integer impiimpuId = impiimpuIds.get(i);
                q.setParameter(i + 8, impiimpuId);
            }
            for (int i = impiimpuIds.size(); i < 5; i++) {
                q.setParameter(i + 8, -1);
            }
            q.executeUpdate();
        }

    }

    public static void setImpiImpuAndImpuUserState(EntityManager em, Impi impi, int userState) {
        Query q = em.createNativeQuery("UPDATE impi_impu join impu ON impu.id = impi_impu.id_impu SET impi_impu.user_state = ?, impu.user_state = ?  WHERE impi_impu.id_impi = ?");
        q.setParameter(1, userState);
        q.setParameter(2, userState);
        q.setParameter(3, impi.getId());
        q.executeUpdate();
    }
    
    public static void updateImpusImplicitSet(EntityManager em, Impi impi, int implicitSetId) {
        Query q = em.createNativeQuery("UPDATE impi_impu pipu, impu pu  "
                + " SET pu.id_implicit_set = ?  "
                + "  WHERE pu.id = pipu.id_impu AND "
                + "        pipu.id_impi = ? AND "
                + "        pu.id_sp != 1000");
        q.setParameter(1, implicitSetId);
        q.setParameter(2, impi.getId());
        q.executeUpdate();
    }

    /**
     * Get all the trigger info and stuff in an xml document
     *
     * @param em
     * @param privateIdentity
     * @param id_implicit_set
     * @param subscriptionData
     * @return
     */
    public static String getUserData(EntityManager em, String privateIdentity, int id_implicit_set, List<Object[]> subscriptionData) {

        if (log.isDebugEnabled()) {
            log.debug("Retrieving user data for private identity [{}] and implicit set [{}]", privateIdentity, id_implicit_set);
        }

        List<List> ifcs_list = new ArrayList<>();    // List of list of iFCs associated to each IMPU
        List<Integer> Sps = new ArrayList<>(); // List of SP id
        List<List<Impu>> impus_list = new ArrayList<>();   // "initial_impus_list" re-formatted into a matrix-like list

        List<Impu> initial_impus_list;
        if (subscriptionData == null) {
            initial_impus_list = getAllImpuInImplicitSet(em, id_implicit_set); //List of IMPUs that belong to the same implicit set
        } else {
            // Single query optimisation
            initial_impus_list = getAllImpuInImplicitSet(id_implicit_set, subscriptionData);
            Collections.sort(initial_impus_list, new IMPUIdentityComparator());
        }
        for (Impu impu : initial_impus_list) {
            List<Impu> aux = new ArrayList<>(); // Insert every IMPU in a List format
            aux.add(impu);				//
            impus_list.add(aux);	    // Insert the list "aux" into the "impus_list" matrix
            List<Ifc> aux2;
            if (subscriptionData == null) {
                aux2 = getAllIFCsByImpuIdAndDsaiValueActive(em, impu.getId());
            } else {
                // Optimisation with cache
                aux2 = getAllIFCsByServiceProfileId(em, impu.getIdSp());
            }
            ifcs_list.add(aux2);
            Sps.add(impu.getIdSp());  // We only store the SP id, instead of the SP object
        }

        /*
         We will handle 3 Lists:
         impus_list,  Where we have the impus in a matrix (each position of the list is another list of IMPUs; Initially there is only one element in each position)
         ifcs_list, in each position of this list we have the active ifcs associated to the IMPU/s which are exactly in the same position of the impus_list
         Sps, in each position of this list we have the SP_id of the IMPU/s which are exactly in the same position of the impus_list
        
         The main idea is looking inside the different positions of the ifcs_list and check if there are two positions of the list which have exactly the same iFCs;
         If this is the case (and if they have also the same SP_id) we can group the IMPUs which are in these two positions.
        
         If we have grouped two IMPUs in the position of the first IMPU which is being compared, we will delete from the three lists the position of the second IMPU
         (since we needn't comparing that position anylonger).
         Otherwise, we will jump to the next position on the lists to go on comparing.
         */
        List<Impu> export;
        int j;
        for (int i = 0; i < (ifcs_list.size()); i++) {
            List Ifc_copy = (List) ifcs_list.get(i);
            j = i + 1;
            while (j < ifcs_list.size()) {
                boolean exit = false;
                List Ifc_copy2 = (List) ifcs_list.get(j);
                if (Ifc_copy2.size() != Ifc_copy.size() || !Sps.get(i).equals(Sps.get(j))) {
                    //Check if the ifc Lists have not the same number of elements or if the SPs they belong to are different
                    //In that case they cannot be associated
                    exit = true;
                }
                if (exit == false) {
                    for (Object Ifc_copy21 : Ifc_copy2) {
                        if (!(Ifc_copy.contains(Ifc_copy21))) {
                            // Check if all ifcs are the same in 2 different positions of the main list. If not, they cannot be associated
                            exit = true;
                            break;
                        }
                    }
                }
                if (exit == false) {
                    export = impus_list.get(i);
                    export.add((impus_list.get(j)).get(0));
                    impus_list.remove(j);
                    ifcs_list.remove(j);
                    Sps.remove(j);
                    impus_list.remove(i);	// Remove the position of the list where we are going to insert the new list of IMPUs
                    impus_list.add(i, export);	// Add the new list of IMPUs
                } else {
                    j++; //if we don't change anything we jump to the next position.
                }
            }
        }

        //
        // begin to write the data in the buffer
        StringBuilder sb = new StringBuilder();
        sb.append(xml_version);
        sb.append(ims_subscription_s);

        // PrivateID
        sb.append(private_id_s);
        sb.append(privateIdentity);
        sb.append(private_id_e);

        //SP
        int i = -1;
        for (List<Impu> impu_array : impus_list) {
            i++;
            sb.append(service_profile_s);
            // PublicIdentity 					=> 1 to n
            for (Impu impu : impu_array) {
                sb.append(public_id_s);
                if (impu.getBarring() == 1) {
                    // add Barring Indication
                    sb.append(barring_indication_s);
                    sb.append(impu.getBarring());
                    sb.append(barring_indication_e);
                }

                // add Identity
                sb.append(identity_s);
                sb.append(impu.getIdentity());
                sb.append(identity_e);

                // add Extension
                sb.append(extension_s);
                // add Identity Type
                sb.append(identity_type_s);
                sb.append(impu.getType());
                sb.append(identity_type_e);

                if (impu.getType() == CxConstants.Identity_Type_Wildcarded_PSI) {
                    String wildcard_psi = impu.getWildcardPSI();
                    if (wildcard_psi == null || wildcard_psi.isEmpty()) {
                        log.warn("Wildcarded PSI is NULL or is empty! Please provide a valid Wildcarded PSI! \n Aborting...");
                        return null;
                    }

                    sb.append(wildcarded_psi_s);
                    wildcard_psi = wildcard_psi.replace('%', '*');
                    wildcard_psi = wildcard_psi.replace('_', '?');
                    sb.append(wildcard_psi);
                    sb.append(wildcarded_psi_e);

                }

                if (impu.getDisplayName() != null && !impu.getDisplayName().isEmpty()) {
                    // add Extension 2 (Display Name)
                    sb.append(extension_s);
                    sb.append(display_name_s);
                    sb.append(impu.getDisplayName());
                    sb.append(display_name_e);
                    sb.append(extension_e);
                }
                sb.append(extension_e);

                sb.append(public_id_e);
            }

            // InitialFilterCriteria 			=> 0 to n
            //	List list_ifc = SP_IFC_DAO.get_all_SP_IFC_by_SP_ID(session, sp_array[i].getId());
            List list_ifc = (List) ifcs_list.get(i);
            if (list_ifc != null && list_ifc.size() > 0) {
                Iterator it_ifc = list_ifc.iterator();
                //Object[] crt_row;

                while (it_ifc.hasNext()) {
                    sb.append(ifc_s);

                    Ifc crt_ifc = (Ifc) it_ifc.next();
                    SpIfc crt_sp_ifc = getBySpAndIfcId(em, Sps.get(i), crt_ifc.getId());

                    // adding priority
                    sb.append(priority_s);
                    sb.append(crt_sp_ifc.getPriority());
                    sb.append(priority_e);

                    // add trigger
                    if (crt_ifc.getTp() != null) {
                        // we have a trigger to add
                        sb.append(tp_s);

                        Tp tp = crt_ifc.getTp();
                        sb.append(cnf_s);
                        sb.append(tp.getConditionTypeCnf());
                        sb.append(cnf_e);

                        Collection<Spt> list_spt = tp.getSptCollection();
                        if (list_spt.isEmpty()) {
                            log.warn("SPT list is empty! Should be at least one SPT asssociated to the TP!\nAborting...");
                            return null;
                        }

                        // add SPT
                        Iterator it_spt = list_spt.iterator();
                        Spt crt_spt;
                        while (it_spt.hasNext()) {
                            crt_spt = (Spt) it_spt.next();
                            sb.append(spt_s);

                            // condition negated
                            sb.append(condition_negated_s);
                            sb.append(crt_spt.getConditionNegated());
                            sb.append(condition_negated_e);

                            // group
                            sb.append(group_s);
                            sb.append(crt_spt.getGrp());
                            sb.append(group_e);

                            switch (crt_spt.getType()) {

                                case CxConstants.SPT_Type_RequestURI:
                                    sb.append(req_uri_s);
                                    sb.append(crt_spt.getRequesturi());
                                    sb.append(req_uri_e);
                                    break;

                                case CxConstants.SPT_Type_Method:
                                    sb.append(method_s);
                                    sb.append(crt_spt.getMethod());
                                    sb.append(method_e);
                                    break;

                                case CxConstants.SPT_Type_SIPHeader:
                                    sb.append(sip_hdr_s);

                                    sb.append(header_s);
                                    sb.append(crt_spt.getHeader());
                                    sb.append(header_e);
                                    sb.append(content_s);
                                    sb.append(crt_spt.getHeaderContent());
                                    sb.append(content_e);

                                    sb.append(sip_hdr_e);
                                    break;

                                case CxConstants.SPT_Type_SessionCase:
                                    sb.append(session_case_s);
                                    sb.append(crt_spt.getSessionCase());
                                    sb.append(session_case_e);
                                    break;

                                case CxConstants.SPT_Type_SessionDescription:
                                    sb.append(session_desc_s);

                                    sb.append(line_s);
                                    sb.append(crt_spt.getSdpLine());
                                    sb.append(line_e);
                                    sb.append(content_s);
                                    sb.append(crt_spt.getSdpLineContent());
                                    sb.append(content_e);

                                    sb.append(session_desc_e);
                                    break;
                            }

                            // add Extension if available
                            if (crt_spt.getRegistrationType() != -1) {
                                sb.append(extension_s);

                                int reg_type = crt_spt.getRegistrationType();
                                if ((reg_type & CxConstants.RType_Reg_Mask) != 0) {
                                    sb.append(registration_type_s);
                                    sb.append(zero);
                                    sb.append(registration_type_e);

                                }
                                if ((reg_type & CxConstants.RType_Re_Reg_Mask) != 0) {
                                    sb.append(registration_type_s);
                                    sb.append(one);
                                    sb.append(registration_type_e);

                                }
                                if ((reg_type & CxConstants.RType_De_Reg_Mask) != 0) {
                                    sb.append(registration_type_s);
                                    sb.append(two);
                                    sb.append(registration_type_e);

                                }
                                sb.append(extension_e);
                            }
                            sb.append(spt_e);
                        }
                        sb.append(tp_e);
                    }

                    // add the Application Server
                    ApplicationServer crt_as = crt_ifc.getApplicationServer();
                    if (crt_as == null) {
                        log.warn("Application Server is NULL, Initial Filter Criteria should contain a valid Application Server! \nAborting...");
                        return null;
                    }

                    sb.append(app_server_s);
                    sb.append(server_name_s);
                    sb.append(crt_as.getServerName());
                    sb.append(server_name_e);
                    if (crt_as.getDefaultHandling() != -1) {
                        sb.append(default_handling_s);
                        sb.append(crt_as.getDefaultHandling());
                        sb.append(default_handling_e);
                    }

                    if (crt_as.getServiceInfo() != null && !crt_as.getServiceInfo().isEmpty()) {
                        sb.append(service_info_s);
                        sb.append(crt_as.getServiceInfo());
                        sb.append(service_info_e);
                    }
                    sb.append(app_server_e);

                    if (crt_ifc.getProfilePartInd() != -1) {
                        // add the profile part indicator
                        sb.append(profile_part_ind_s);
                        sb.append(crt_ifc.getProfilePartInd());
                        sb.append(profile_part_ind_e);

                    }

                    sb.append(ifc_e);
                }

            }
            /*
             // CoreNetworkServiceAuthorization	=> 0 to n
             if (sp_array[i].getCn_service_auth() != -1){
             sb.append(cn_services_auth_s);
             sb.append(subs_media_profile_id_s);
             sb.append(sp_array[i].getCn_service_auth());
             sb.append(subs_media_profile_id_e);
             sb.append(cn_services_auth_e);
             }
             */

            // Extension						=> 0 to 1
            //List all_IDs = SP_Shared_IFC_Set_DAO.get_all_shared_IFC_set_IDs_by_SP_ID(session, sp_array[i].getId());
            int sp_id = Sps.get(i);
            List all_IDs = getAllSharedIfcSetIDsBySpId(em, sp_id);
            if (all_IDs != null && all_IDs.size() > 0) {
                sb.append(extension_s);
                Iterator all_IDs_it = all_IDs.iterator();
                while (all_IDs_it.hasNext()) {
                    int crt_ID = (Integer) all_IDs_it.next();
                    sb.append(shared_ifc_set_id_s);
                    sb.append(crt_ID);
                    sb.append(shared_ifc_set_id_e);
                }
                sb.append(extension_e);
            }
            sb.append(service_profile_e);
        }
        sb.append(ims_subscription_e);

        if (log.isDebugEnabled()) {
            log.debug("\n\nThe UserData XML document which is sent to the S-CSCF:\n" + sb.toString());
        }
        return sb.toString();
    }
    /*
     * 
     * xml constants
     * 
     */
    public static final String xml_version = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    public static final String ims_subscription_s = "<IMSSubscription>";
    public static final String ims_subscription_e = "</IMSSubscription>";
    public static final String private_id_s = "<PrivateID>";
    public static final String private_id_e = "</PrivateID>";
    public static final String service_profile_s = "<ServiceProfile>";
    public static final String service_profile_e = "</ServiceProfile>";
    public static final String public_id_s = "<PublicIdentity>";
    public static final String public_id_e = "</PublicIdentity>";
    public static final String barring_indication_s = "<BarringIndication>";
    public static final String barring_indication_e = "</BarringIndication>";
    public static final String identity_s = "<Identity>";
    public static final String identity_e = "</Identity>";
    public static final String identity_type_s = "<IdentityType>";
    public static final String identity_type_e = "</IdentityType>";
    public static final String display_name_s = "<DisplayName>";
    public static final String display_name_e = "</DisplayName>";
    public static final String ifc_s = "<InitialFilterCriteria>";
    public static final String ifc_e = "</InitialFilterCriteria>";
    public static final String priority_s = "<Priority>";
    public static final String priority_e = "</Priority>";
    public static final String tp_s = "<TriggerPoint>";
    public static final String tp_e = "</TriggerPoint>";
    public static final String cnf_s = "<ConditionTypeCNF>";
    public static final String cnf_e = "</ConditionTypeCNF>";
    public static final String spt_s = "<SPT>";
    public static final String spt_e = "</SPT>";
    public static final String condition_negated_s = "<ConditionNegated>";
    public static final String condition_negated_e = "</ConditionNegated>";
    public static final String group_s = "<Group>";
    public static final String group_e = "</Group>";
    public static final String req_uri_s = "<RequestURI>";
    public static final String req_uri_e = "</RequestURI>";
    public static final String method_s = "<Method>";
    public static final String method_e = "</Method>";
    public static final String sip_hdr_s = "<SIPHeader>";
    public static final String sip_hdr_e = "</SIPHeader>";
    public static final String session_case_s = "<SessionCase>";
    public static final String session_case_e = "</SessionCase>";
    public static final String session_desc_s = "<SessionDescription>";
    public static final String session_desc_e = "</SessionDescription>";
    public static final String registration_type_s = "<RegistrationType>";
    public static final String registration_type_e = "</RegistrationType>";
    public static final String header_s = "<Header>";
    public static final String header_e = "</Header>";
    public static final String content_s = "<Content>";
    public static final String content_e = "</Content>";
    public static final String line_s = "<Line>";
    public static final String line_e = "</Line>";
    public static final String app_server_s = "<ApplicationServer>";
    public static final String app_server_e = "</ApplicationServer>";
    public static final String server_name_s = "<ServerName>";
    public static final String server_name_e = "</ServerName>";
    public static final String default_handling_s = "<DefaultHandling>";
    public static final String default_handling_e = "</DefaultHandling>";
    public static final String service_info_s = "<ServiceInfo>";
    public static final String service_info_e = "</ServiceInfo>";
    public static final String profile_part_ind_s = "<ProfilePartIndicator>";
    public static final String profile_part_ind_e = "</ProfilePartIndicator>";
    public static final String cn_services_auth_s = "<CoreNetworkServicesAuthorization>";
    public static final String cn_services_auth_e = "</CoreNetworkServicesAuthorization>";
    public static final String subs_media_profile_id_s = "<SubscribedMediaProfileId>";
    public static final String subs_media_profile_id_e = "</SubscribedMediaProfileId>";
    public static final String shared_ifc_set_id_s = "<Extension><SharedIFCSetID>";
    public static final String shared_ifc_set_id_e = "</SharedIFCSetID></Extension>";
    public static final String extension_s = "<Extension>";
    public static final String extension_e = "</Extension>";
    public static final String zero = "0";
    public static final String one = "1";
    public static final String two = "2";
    public static final String wildcarded_psi_s = "<WildcardedPSI>";
    public static final String wildcarded_psi_e = "</WildcardedPSI>";

    private static List<Impu> getAllImpuInImplicitSet(EntityManager em, int id_implicit_set) {
        Query q = em.createNativeQuery("select * from impu where impu.id_implicit_set=? order by identity", Impu.class);
        q.setParameter(1, id_implicit_set);
        return q.getResultList();
    }

    public static List<Ifc> getAllIFCsByImpuIdAndDsaiValueActive(EntityManager em, int id_impu) {

        //we get all associated IFCs to the IMPU.
        Query query = em.createNativeQuery("select IFC.* from"
                + " ifc IFC, sp_ifc SP_IFC, impu IMPU where"
                + " IMPU.id=? and IMPU.id_sp=SP_IFC.id_sp and SP_IFC.id_ifc=IFC.id", Ifc.class);
        query.setParameter(1, id_impu);
        List<Ifc> all_ifc = (List<Ifc>) query.getResultList();
        return all_ifc;
    }

    public static List<Ifc> getAllIFCsByServiceProfileId(EntityManager em, int spId) {

        List<Ifc> all_ifc = IMDataCache.IfcCache.get(spId);

        if (all_ifc == null) {
            Query query = em.createNativeQuery("select IFC.* from ifc IFC, sp_ifc SP_IFC where SP_IFC.id_sp=? and SP_IFC.id_ifc=IFC.id", Ifc.class);
            query.setParameter(1, spId);
            all_ifc = (List<Ifc>) query.getResultList();
            for (Ifc ifc : all_ifc) {
                // ensure eager fetching
                ifc.getTp().getSptCollection().size();
                ifc.getTp().getIfcCollection().size();
                em.detach(ifc);
            }
            IMDataCache.IfcCache.put(spId, all_ifc);
        }
        return all_ifc;
    }

    public static SpIfc getBySpAndIfcId(EntityManager em, int id_sp, int id_ifc) {

        String key = id_sp + "_" + id_ifc;
        SpIfc result = IMDataCache.SpIfcCache.get(key);
        if (result == null) {
            Query query = em.createNativeQuery("select * from sp_ifc where id_sp=? and id_ifc=?", SpIfc.class);
            query.setParameter(1, id_sp);
            query.setParameter(2, id_ifc);
            try {
                result = (SpIfc) query.getSingleResult();
                em.detach(result);
                IMDataCache.SpIfcCache.put(key, result);
            } catch (Exception e) {
                log.warn("Query did not returned an unique result! You have a duplicate in the database!");
                log.warn("Error: ", e);
            }
        }
        return result;
    }

    public static List<Integer> getAllSharedIfcSetIDsBySpId(EntityManager em, int id_sp) {
        List<Integer> res = IMDataCache.sharedIfcSetIdsCache.get(id_sp);
        if (res == null) {
            Query query = em.createNativeQuery(
                    "select id_set from shared_ifc_set inner join sp_shared_ifc_set on (sp_shared_ifc_set.id_shared_ifc_set=shared_ifc_set.id) where sp_shared_ifc_set.id_sp=?");
            query.setParameter(1, id_sp);
            res = query.getResultList();
            IMDataCache.sharedIfcSetIdsCache.put(id_sp, res);
        }
        return res;
    }

    public static ImpiApn getApnfromImpiIdAndApnName(EntityManager em, int impiId, String apnName) {
        log.debug("Entering Apn method");
        Query query = em.createNativeQuery("select * from impi_apn where id_impi=? and apn_name=?", ImpiApn.class);
        query.setParameter(1, impiId);
        query.setParameter(2, apnName);
        List resp = query.getResultList();
        if (resp.isEmpty()) {
            return null;
        } else {
            return (ImpiApn) resp.get(0);
        }
    }

    // imsu.id, imsu.name, imsu.scscf_name, imsu.diameter_name, imsu.id_capabilities_set, imsu.id_preferred_scscf_set, imsu.version,
    private static final int COL_ID_IMSU_ID = 0;
    private static final int COL_ID_IMSU_NAME = COL_ID_IMSU_ID + 1;
    private static final int COL_ID_IMSU_SCSCF_NAME = COL_ID_IMSU_ID + 2;
    private static final int COL_ID_IMSU_DIAMETER_NAME = COL_ID_IMSU_ID + 3;
    private static final int COL_ID_IMSU_CAPABILITIES_SET_ID = COL_ID_IMSU_ID + 4;
    private static final int COL_ID_IMSU_PREFERRED_SCSCF_SET_ID = COL_ID_IMSU_ID + 5;
    private static final int COL_ID_IMSU_VERSION = COL_ID_IMSU_ID + 6;

    //impi.id , impi.id_imsu, impi.identity, impi.k, impi.auth_scheme, impi.default_auth_scheme, impi.amf, impi.op, 
    //impi.sqn, impi.line_identifier, impi.iccid, impi.ossbss_reference_id, impi.sim_locked_imei_list, impi.public_k,
    private static final int COL_ID_IMPI_ID = COL_ID_IMSU_VERSION + 1;
    private static final int COL_ID_IMPI_IMSU_ID = COL_ID_IMPI_ID + 1;
    private static final int COL_ID_IMPI_IDENTITY = COL_ID_IMPI_ID + 2;
    private static final int COL_ID_IMPI_K = COL_ID_IMPI_ID + 3;
    private static final int COL_ID_IMPI_AUTH_SCHEME = COL_ID_IMPI_ID + 4;
    private static final int COL_ID_IMPI_DEFAULT_AUTH_SCHEME = COL_ID_IMPI_ID + 5;
    private static final int COL_ID_IMPI_AMF = COL_ID_IMPI_ID + 6;
    private static final int COL_ID_IMPI_OP = COL_ID_IMPI_ID + 7;
    private static final int COL_ID_IMPI_SQN = COL_ID_IMPI_ID + 8;
    private static final int COL_ID_LINE_IDENTIFIER = COL_ID_IMPI_ID + 9;
    private static final int COL_ID_IMPI_ICCID = COL_ID_IMPI_ID + 10;
    private static final int COL_ID_IMPI_OSS_BSS = COL_ID_IMPI_ID + 11;
    private static final int COL_ID_IMPI_SIM_LOCKED = COL_ID_IMPI_ID + 12;
    private static final int COL_ID_IMPI_PUBLIC_K = COL_ID_IMPI_ID + 13;
    private static final int COL_ID_IMPI_STATUS = COL_ID_IMPI_ID + 14;
    private static final int COL_ID_IMPI_INFO = COL_ID_IMPI_ID + 15;

    // impi_impu.id, impi_impu.id_impi, impi_impu.id_impu, impi_impu.user_state,
    private static final int COL_ID_IMPI_IMPU_ID = COL_ID_IMPI_INFO + 1;
    private static final int COL_ID_IMPI_IMPU_IMPI_ID = COL_ID_IMPI_IMPU_ID + 1;
    private static final int COL_ID_IMPI_IMPU_IMPU_ID = COL_ID_IMPI_IMPU_ID + 2;
    private static final int COL_ID_IMPI_IMPU_USER_STATE = COL_ID_IMPI_IMPU_ID + 3;

    // impu.id, impu.identity, impu.type, impu.barring, impu.user_state, impu.id_sp, impu.id_implicit_set, 
    // impu.id_charging_info, impu.display_name, impu.can_register, impu.psi_activation, impu.wildcard_psi,
    private static final int COL_ID_IMPU_ID = COL_ID_IMPI_IMPU_USER_STATE + 1;
    private static final int COL_ID_IMPU_IDENTITY = COL_ID_IMPU_ID + 1;
    private static final int COL_ID_IMPU_TYPE = COL_ID_IMPU_ID + 2;
    private static final int COL_ID_IMPU_BARRING = COL_ID_IMPU_ID + 3;
    private static final int COL_ID_IMPU_USER_STATE = COL_ID_IMPU_ID + 4;
    private static final int COL_ID_IMPU_SP_ID = COL_ID_IMPU_ID + 5;
    private static final int COL_ID_IMPU_IMPLICIT_SET_ID = COL_ID_IMPU_ID + 6;
    private static final int COL_ID_IMPU_CHARGING_INFO_ID = COL_ID_IMPU_ID + 7;
    private static final int COL_ID_IMPU_DISPLAY_NAME = COL_ID_IMPU_ID + 8;
    private static final int COL_ID_IMPU_CAN_REGISTER = COL_ID_IMPU_ID + 9;
    private static final int COL_ID_IMPU_PSI_ACTIVATION = COL_ID_IMPU_ID + 10;
    private static final int COL_ID_IMPU_WILDCARD_PSI = COL_ID_IMPU_ID + 11;

    // impi_apn.id, impi_apn.id_impi, impi_apn.apn_name, impi_apn.type, impi_apn.ipv4, impi_apn.ipv6 ,
    private static final int COL_ID_APN_ID = COL_ID_IMPU_WILDCARD_PSI + 1;
    private static final int COL_ID_APN_IMPI = COL_ID_APN_ID + 1;
    private static final int COL_ID_APN_NAME = COL_ID_APN_ID + 2;
    private static final int COL_ID_APN_TYPE = COL_ID_APN_ID + 3;
    private static final int COL_ID_APN_IPV4 = COL_ID_APN_ID + 4;
    private static final int COL_ID_APN_IPV6 = COL_ID_APN_ID + 5;

    // impu_visited_network.id, impu_visited_network.id_impu, impu_visited_network.id_visited_network
    private static final int COL_ID_IMPU_VISITED_NETWORK_ID = COL_ID_APN_IPV6 + 1;
    private static final int COL_ID_IMPU_VISITED_NETWORK_IMPU_ID = COL_ID_IMPU_VISITED_NETWORK_ID + 1;
    private static final int COL_ID_IMPU_VISITED_NETWORK_VISITED_NETWORK_ID = COL_ID_IMPU_VISITED_NETWORK_ID + 2;

    public static Set<Integer> getIMPIIdsFromSubscriptionData(List<Object[]> subscriptionData) {
        Set<Integer> ids = new HashSet();
        for (Object[] row : subscriptionData) {
            Integer i = getIntValue(row[COL_ID_IMPI_ID]);
            if (i != null) {
                ids.add(i);
            }
        }
        return ids;
    }

    public static Impi getImpiFromSubscriptionData(int impiId, List<Object[]> subscriptionData, boolean iterate) {
        log.debug("In getImpiFromSubscriptionData for impi id [{}]", impiId);

        Impi dbImpi = new Impi();
        for (Object[] row : subscriptionData) {
            if (getIntValue(row[COL_ID_IMPI_ID]) == impiId) {
                // Found the impi
                dbImpi.setAmf(getStringValue(row[COL_ID_IMPI_AMF]));
                dbImpi.setAuthScheme(getIntValue(row[COL_ID_IMPI_AUTH_SCHEME]));
                dbImpi.setDefaultAuthScheme(getIntValue(row[COL_ID_IMPI_DEFAULT_AUTH_SCHEME]));
                dbImpi.setIccid(getStringValue(row[COL_ID_IMPI_ICCID]));
                dbImpi.setId(getIntValue(row[COL_ID_IMPI_ID]));
                dbImpi.setIdentity(getStringValue(row[COL_ID_IMPI_IDENTITY]));
                dbImpi.setStatus(getStringValue(row[COL_ID_IMPI_STATUS]));

                List<ImpiApn> impiApnList = getImpiApnListFromSubscriptionData(impiId, subscriptionData);
                dbImpi.setImpiApnCollection(impiApnList);

                if (iterate) {
                    List<ImpiImpu> impiimpuList = getImpiImpuListForImpiFromSubscriptionData(impiId, subscriptionData);
                    dbImpi.setImpiImpuCollection(impiimpuList);
                }
                dbImpi.setImsu(getImsuFromSubscriptionData(impiId, subscriptionData));
                dbImpi.setIp("");
                dbImpi.setK(getStringValue(row[COL_ID_IMPI_K]));
                dbImpi.setLineIdentifier(getStringValue(row[COL_ID_LINE_IDENTIFIER]));
                dbImpi.setOSSBSSReferenceId(getStringValue(row[COL_ID_IMPI_OSS_BSS]));
                dbImpi.setOp(getStringValue(row[COL_ID_IMPI_OP]));
                dbImpi.setPublicK(getStringValue(row[COL_ID_IMPI_PUBLIC_K]));
                dbImpi.setSimLockedImeiList(getStringValue(row[COL_ID_IMPI_SIM_LOCKED]));

                dbImpi.setInfo(getStringValue(row[COL_ID_IMPI_INFO]));

                log.debug("Setting regionalSubscriptionZoneCodes");
                String regionalSubscriptionZoneCodes = "";
                String info = getStringValue(row[COL_ID_IMPI_INFO]);
                if (info != null) {
                    String lines[] = getStringValue(row[COL_ID_IMPI_INFO]).split("\\r?\\n");
                    for (String line : lines) {
                        if (line.startsWith("RegionalSubscriptionZoneCodes")) {
                            String avp[] = line.split("=");
                            if (avp.length == 2) {
                                regionalSubscriptionZoneCodes = avp[1];
                            }
                        }
                    }

                }
                dbImpi.setRegionalSubscriptionZoneCodes(regionalSubscriptionZoneCodes);

                dbImpi.setSqn(getStringValue(row[COL_ID_IMPI_SQN]));
                break;
            }
        }
        log.debug("Finished getImpiFromSubscriptionData for impi id [{}]", impiId);
        return dbImpi;
    }

    private static List<ImpiApn> getImpiApnListFromSubscriptionData(int impiId, List<Object[]> subscriptionData) {
        List<ImpiApn> impiApnList = new ArrayList();
        Set<Integer> apnsDone = new HashSet();
        for (Object[] row : subscriptionData) {
            if (getIntValue(row[COL_ID_IMPI_ID]).equals(impiId)) {
                Integer apnId = getIntValue(row[COL_ID_APN_ID]);
                if (apnId == null || apnsDone.contains(apnId)) {
                    continue;
                }
                apnsDone.add(apnId);
                ImpiApn impiapn = new ImpiApn();
                impiapn.setApnName(getStringValue(row[COL_ID_APN_NAME]));
                impiapn.setId(getIntValue(row[COL_ID_APN_ID]));
                impiapn.setImpi(null);
                impiapn.setIpv4(getStringValue(row[COL_ID_APN_IPV4]));
                impiapn.setIpv6(getStringValue(row[COL_ID_APN_IPV6]));
                impiapn.setType(getIntValue(row[COL_ID_APN_TYPE]));

                impiApnList.add(impiapn);
            }
        }
        return impiApnList;
    }

    private static List<ImpiImpu> getImpiImpuListForImpiFromSubscriptionData(int impiId, List<Object[]> subscriptionData) {
        List<ImpiImpu> impiImpuList = new ArrayList();
        Set<Integer> impiimpusDone = new HashSet();
        for (Object[] row : subscriptionData) {
            if (getIntValue(row[COL_ID_IMPI_ID]).equals(impiId)) {
                Integer impiimpuId = getIntValue(row[COL_ID_IMPI_IMPU_ID]);
                if (impiimpuId == null || impiimpusDone.contains(impiimpuId)) {
                    continue;
                }
                impiimpusDone.add(impiimpuId);
                ImpiImpu impiimpu = new ImpiImpu();
                impiimpu.setId(getIntValue(row[COL_ID_IMPI_IMPU_ID]));
                impiimpu.setImpi(getImpiFromSubscriptionData(getIntValue(row[COL_ID_IMPI_IMPU_IMPI_ID]), subscriptionData, false));
                impiimpu.setImpu(getImpuFromSubscriptionData(getIntValue(row[COL_ID_IMPI_IMPU_IMPU_ID]), subscriptionData));
                impiimpu.setUserState(getShortValue(row[COL_ID_IMPI_IMPU_USER_STATE]));
                impiImpuList.add(impiimpu);
            }
        }
        return impiImpuList;
    }

    private static Imsu getImsuFromSubscriptionData(int impiId, List<Object[]> subscriptionData) {
        Imsu imsu = new Imsu();
        for (Object[] row : subscriptionData) {
            if (getIntValue(row[COL_ID_IMPI_ID]).equals(impiId)) {
                imsu.setDiameterName(getStringValue(row[COL_ID_IMSU_DIAMETER_NAME]));
                imsu.setId(getIntValue(row[COL_ID_IMSU_ID]));
                imsu.setIdCapabilitiesSet(getIntValue(row[COL_ID_IMSU_CAPABILITIES_SET_ID]));
                imsu.setIdPreferredScscfSet(getIntValue(row[COL_ID_IMSU_PREFERRED_SCSCF_SET_ID]));
                imsu.setImpiCollection(null);
                imsu.setName(getStringValue(row[COL_ID_IMSU_NAME]));
                imsu.setScscfName(getStringValue(row[COL_ID_IMSU_SCSCF_NAME]));
                break;
            }
        }
        return imsu;
    }

    private static List<Impu> getAllImpuInImplicitSet(int id_implicit_set, List<Object[]> subscriptionData) {
        List<Impu> impus = new ArrayList();
        Set<Integer> impusDone = new HashSet();
        for (Object[] row : subscriptionData) {
            if (getIntValue(row[COL_ID_IMPU_IMPLICIT_SET_ID]).equals(id_implicit_set)) {
                Integer impuId = getIntValue(row[COL_ID_IMPU_ID]);
                if (impuId == null || impusDone.contains(impuId)) {
                    continue;
                }
                impusDone.add(impuId);
                impus.add(getImpuFromSubscriptionData(impuId, subscriptionData));
            }
        }
        return impus;
    }

    private static Impu getImpuFromSubscriptionData(int impuId, List<Object[]> subscriptionData) {
        Impu dbImpu = new Impu();
        for (Object[] row : subscriptionData) {
            if (getIntValue(row[COL_ID_IMPU_ID]).equals(impuId)) {
                // Found the impu
                dbImpu.setBarring(getShortValue(row[COL_ID_IMPU_BARRING]));
                dbImpu.setCanRegister(getShortValue(row[COL_ID_IMPU_CAN_REGISTER]));
                dbImpu.setDisplayName(getStringValue(row[COL_ID_IMPU_DISPLAY_NAME]));
                dbImpu.setId(getIntValue(row[COL_ID_IMPU_ID]));
                dbImpu.setIdChargingInfo(getIntValue(row[COL_ID_IMPU_CHARGING_INFO_ID]));
                dbImpu.setIdImplicitSet(getIntValue(row[COL_ID_IMPU_IMPLICIT_SET_ID]));
                dbImpu.setIdSp(getIntValue(row[COL_ID_IMPU_SP_ID]));
                dbImpu.setIdentity(getStringValue(row[COL_ID_IMPU_IDENTITY]));
                dbImpu.setImpiImpuCollection(null);
                List<ImpuVisitedNetwork> networkList = getImpuVisitedNetworkListFromSubscriptionData(impuId, subscriptionData);
                dbImpu.setImpuVisitedNetworkCollection(networkList);

                dbImpu.setPSIActivation(getShortValue(row[COL_ID_IMPU_PSI_ACTIVATION]));
                dbImpu.setType(getShortValue(row[COL_ID_IMPU_TYPE]));
                dbImpu.setUserState(getShortValue(row[COL_ID_IMPU_USER_STATE]));
                dbImpu.setWildcardPSI(getStringValue(row[COL_ID_IMPU_WILDCARD_PSI]));
                break;
            }
        }

        return dbImpu;
    }

    private static List<ImpuVisitedNetwork> getImpuVisitedNetworkListFromSubscriptionData(int impuId, List<Object[]> subscriptionData) {
        List<ImpuVisitedNetwork> networkList = new ArrayList();
        Set<Integer> networksDone = new HashSet();
        for (Object[] row : subscriptionData) {
            if (getIntValue(row[COL_ID_IMPU_VISITED_NETWORK_IMPU_ID]).equals(impuId)) {
                Integer networkId = getIntValue(row[COL_ID_IMPU_VISITED_NETWORK_ID]);
                if (networkId == null || networksDone.contains(networkId)) {
                    continue;
                }
                networksDone.add(networkId);
                ImpuVisitedNetwork network = new ImpuVisitedNetwork();
                network.setId(getIntValue(row[COL_ID_IMPU_VISITED_NETWORK_ID]));
                network.setIdVisitedNetwork(getIntValue(row[COL_ID_IMPU_VISITED_NETWORK_VISITED_NETWORK_ID]));
                networkList.add(network);
            }
        }
        return networkList;
    }

    private static Integer getIntValue(Object o) {
        try {
            return (Integer) o;
        } catch (Exception e) {
            return Integer.parseInt((String) o);
        }
    }

    private static Short getShortValue(Object o) {
        return Short.parseShort(o.toString());
    }

    private static String getStringValue(Object o) {
        try {
            return (String) o;
        } catch (Exception e) {
            return o.toString();
        }
    }

    private static class IMPUIdentityComparator implements Comparator {

        @Override
        public int compare(Object impu1, Object impu2) {
            String id1 = ((Impu) impu1).getIdentity();
            String id2 = ((Impu) impu2).getIdentity();
            return id1.compareTo(id2);
        }
    }

}
