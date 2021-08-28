/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.imssc.db.op;

import com.smilecoms.imssc.db.model.PreferredScscfSet;
import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.slf4j.*;

/**
 *
 * @author jaybeepee
 */
public class IMSSCDAO {
    private static final Logger log = LoggerFactory.getLogger(IMSSCDAO.class);
    
    public List<String> getDistinctSCSCFs(EntityManager em) {
        List<PreferredScscfSet> distinctSCSCFSet = null;
        List<String> scscfs = null;
        
        try {
            Query q = em.createNativeQuery("SELECT distinct scscf_name FROM preferred_scscf_set", PreferredScscfSet.class);
            distinctSCSCFSet = (List<PreferredScscfSet>) q.getResultList();
        } catch (Exception e) {
            log.warn("Exception caught when retrieving distinct SCSCFs [{}]", e);
        }
        
        Iterator it = distinctSCSCFSet.iterator();
        while (it.hasNext()) {
            PreferredScscfSet scscf = (PreferredScscfSet)it.next();
            scscfs.add(scscf.getScscfName());
        }
        
        return scscfs;
    }
}
