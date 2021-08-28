package com.smilecoms.im;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.im.SCSCF;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author jaybeepee
 */
public class SCSCFSet {

    private static final Logger log = LoggerFactory.getLogger(IMDataCache.class);
    private int ID;
    private List<SCSCF> scscfList;
    private boolean balanced;
    private boolean allCSCFsUp;
    private int totalLoad;

    public boolean isAllCSCFsUp() {
        return allCSCFsUp;
    }

    public void setAllCSCFsUp(boolean allCSCFsUp) {
        this.allCSCFsUp = allCSCFsUp;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public int getTotalLoad() {
        return totalLoad;
    }

    public void setTotalLoad(int totalLoad) {
        this.totalLoad = totalLoad;
    }

    public SCSCFSet(int id) {
        this.balanced = true;
        this.ID = id;
    }

    public List<SCSCF> getScscfAvailableList() {
        return scscfList;
    }

    public List<SCSCF> getScscfList() {
        return scscfList;
    }

    public void setScscfList(List<SCSCF> scscfList) {
        this.scscfList = scscfList;
    }

    public void calculateSetStats() {
        log.debug("calculating set stats for SCSCF set [{}] with total subscriptions of [{}]", new Object[]{this.ID, this.totalLoad});
        int threshold = BaseUtils.getIntProperty("env.im.scscf.load.thresholdpercentage", 10);
        int upCSCFsCount = 0;
        int distributeLoad = 0;
        List<SCSCF> upCSCFs = new ArrayList<>();
        this.setBalanced(true);
        this.setAllCSCFsUp(true);
        for (SCSCF scscf : this.scscfList) {
            log.debug("Callculating stats for [{}]", scscf.getScscfName());
            scscf.setCurrentLoadPercentage((int) (((float) scscf.getCurrentLoad() / (float) this.totalLoad) * 100));
            log.debug("Setting current load percentage to [{}]", scscf.getCurrentLoadPercentage());

            if (SCSCFIsUpDaemon.isSCSCFMarkedAsUp(scscf.getScscfName())) {
                log.debug("SCSCF is up - checking threshold");
                upCSCFs.add(scscf);
                upCSCFsCount++;
                if (scscf.getCurrentLoadPercentage() > (scscf.getMaxLoadPercentage() + threshold)) {
                    log.debug("scscf [{}] of set ID [{}] has breached it's threshold", new Object[]{scscf.getScscfName(), this.getID()});
                    int delta = scscf.getMaxLoadPercentage() - scscf.getCurrentLoadPercentage();
                    log.debug("delta for SCSCF [{}] set to [{}]", new Object[]{scscf.getScscfName(), scscf.getDeltarequired()});
                    scscf.setDeltarequired(delta);
                    this.setBalanced(false);
                } else {
                    //could be underloaded
                    int delta = scscf.getMaxLoadPercentage() - scscf.getCurrentLoadPercentage();
                    log.debug("delta for SCSCF [{}] set to [{}]", new Object[]{scscf.getScscfName(), scscf.getDeltarequired()});
                    scscf.setDeltarequired(delta);
                }
            } else {
                log.debug("SCSCF is down - adjusting for other scscfs in the set to carry the load");
                this.setAllCSCFsUp(false);
                scscf.setAvailable(false);
                distributeLoad += scscf.getMaxLoadPercentage();
            }
        }

        if (!this.allCSCFsUp && (distributeLoad > 0) && (upCSCFsCount > 0)) {
            log.debug("SCSCF set [{}] has 1+ SCSCF downed - shedding the load", this.ID);
            int loadForEachSCSCF = distributeLoad / upCSCFsCount;
            int extra = distributeLoad % upCSCFsCount;
            for (SCSCF ups : upCSCFs) {
                ups.setMaxLoadPercentage(ups.getMaxLoadPercentage() + loadForEachSCSCF + extra);
                log.debug("Added [{}] load to S-CSCF [{}]", new Object[]{loadForEachSCSCF, ups.getScscfName()});
                extra = 0;
            }
        }
    }

    /* get a list of SCSCFs for this set based on their load - ie ascending order of current load (least loaded first). */
    List<SCSCF> getSCSCFListOrderedByLoadAsc() {
        List<SCSCF> orderedList = new ArrayList<>();
        int i, j = 0;
        int insertPos;
        SCSCF firstIfBalanced = null;
        double rand = Math.random();
        boolean skipBalance = false;

        for (SCSCF s : this.getScscfList()) {
            //if the delta needed on any of the s-cscfs in the set exceed a threshold then 
            //we ignore the loadbalancing algorithm in favour of a good balance
            if (s.getDeltarequired() > BaseUtils.getIntProperty("env.im.scscf.load.skipbalance.thresholdpercentage", 10)) {
                skipBalance = true;
            }
            if (firstIfBalanced == null && rand < s.getProbability()) {
                firstIfBalanced = s;
                log.debug("setting firstBalanced to [{}] and rand is [{}] and probability is [{}]", new Object[]{s.getScscfName(), rand, s.getProbability()});
            }
            if (orderedList.isEmpty()) {
                orderedList.add(s);
            } else {
                i = 0;
                insertPos = -1;
                for (SCSCF orderedSCSCF : orderedList) {
                    if (s.getDeltarequired() > orderedSCSCF.getDeltarequired()) {
                        log.debug("Adding SCSCF at position [{}] in front of SCSCF that has delta of [{}]", new Object[]{i, orderedSCSCF.getDeltarequired()});
                        insertPos = i;
                        break;
                    } else {
                        i++;
                    }
                }
                if (insertPos >= 0) {
                    log.debug("Adding SCSCF [{}] at position [{}]", new Object[]{s.getScscfName(), i});
                    orderedList.add(insertPos, s);
                } else {
                    //add to end
                    log.debug("Adding SCSCF [{}] to end of ordered list", s.getScscfName());
                    orderedList.add(s);
                }
            }
        }

        if (!skipBalance && this.isBalanced() && (firstIfBalanced != null)) {
            log.debug("we are balanced so adding [{}] to the front of the list", firstIfBalanced.getScscfName());
            orderedList.add(0, firstIfBalanced);
        }

        return orderedList;
    }
}
