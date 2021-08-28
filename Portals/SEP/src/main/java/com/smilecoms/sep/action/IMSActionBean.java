/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.IMSSubscriptionQuery;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StIMSSubscriptionLookupVerbosity;
import com.smilecoms.commons.sca.StPCRFDataLookupVerbosity;
import com.smilecoms.commons.sca.StPurgeUserDataVerbosity;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.commons.sca.helpers.Permissions;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.Resolution;
/**
 *
 * @author jaybeepee
 */
public class IMSActionBean extends SmileActionBean {
    public Resolution retrieveSCSCFIMPUData() {
        checkPermissions(Permissions.EPC_ADMIN);
        checkPermissions(Permissions.VIEW_PRODUCT_OR_SERVICE_INSTANCES);
        setSCSCFIMPUData(SCAWrapper.getUserSpecificInstance().getSCSCFIMPUData(getSCSCFIMPUQuery()));
        return getDDForwardResolution("/ims/view_scscf_impu_data.jsp");
    }
    
    @DefaultHandler
    public Resolution showRetrieveHSSData() {
        checkPermissions(Permissions.EPC_ADMIN);
        checkPermissions(Permissions.VIEW_PRODUCT_OR_SERVICE_INSTANCES);
        return getDDForwardResolution("/ims/view_hss_data.jsp");
    }
    private String imsi;

    public void setIMSI(String imsi) {
        this.imsi = imsi;
    }

    public Resolution retrieveHSSData() {
        checkPermissions(Permissions.EPC_ADMIN);
        checkPermissions(Permissions.VIEW_PRODUCT_OR_SERVICE_INSTANCES);

        String impu = getIMSSubscriptionQuery().getIMSPublicIdentity();
        log.debug("Pre cleanup [{}]", impu);
        impu = Utils.getPublicIdentityForPhoneNumber(impu);
        log.debug("Post cleanup [{}]", impu);
        getIMSSubscriptionQuery().setIMSPublicIdentity(impu);
        getIMSSubscriptionQuery().setVerbosity(StIMSSubscriptionLookupVerbosity.IMSU_IMPI_IMPU_BESTEFFORT);
        if (imsi != null && !imsi.isEmpty()) {
            getIMSSubscriptionQuery().setIMSPrivateIdentity(Utils.makePrivateIdentityFromIMSI(imsi));
        }
        setIMSSubscription(SCAWrapper.getUserSpecificInstance().getIMSSubscription(getIMSSubscriptionQuery()));
        
        if (!getIMSSubscription().getSCSCFName().isEmpty() && BaseUtils.getBooleanProperty("env.im.subscription.append_scscf_status", false)) {
            getIMSSubscription().setSCSCFName(getIMSSubscription().getSCSCFName().split("#")[0]);
        }
        return getDDForwardResolution("/ims/view_hss_data.jsp");
    }

    public Resolution retrievePCRFData() {
        log.debug("Retrieving PCRF Data");
        checkPermissions(Permissions.EPC_ADMIN);
        checkPermissions(Permissions.VIEW_PRODUCT_OR_SERVICE_INSTANCES);
        getPCRFDataQuery().setVerbosity(StPCRFDataLookupVerbosity.NONE);
        setPCRFData(SCAWrapper.getUserSpecificInstance().getPCRFData(getPCRFDataQuery()));
        return getDDForwardResolution("/ims/view_pcrf_data.jsp");
    }
    
    public Resolution purgeUserDataFromPGW() {
        log.debug("Purging User Data from PGW");
        checkPermissions(Permissions.EPC_ADMIN);
        getPurgeUserDataQuery().setVerbosity(StPurgeUserDataVerbosity.PGW_PURGE);
        SCAWrapper.getUserSpecificInstance().purgeUserData(getPurgeUserDataQuery());
        return retrieveHSSData();
    }
    
    public Resolution deregisterIMPU() {
        log.debug("deRegisteringIMPU");
        checkPermissions(Permissions.EPC_ADMIN);
        SCAWrapper.getUserSpecificInstance().deregisterIMPU(getDeregisterIMPUQuery());
        return retrieveHSSData();
    }
    
    public Resolution purgeUserDataFromMME() {
        log.debug("Purging User Data from MME");
        checkPermissions(Permissions.EPC_ADMIN);
        getPurgeUserDataQuery().setVerbosity(StPurgeUserDataVerbosity.MME_PURGE);
        SCAWrapper.getUserSpecificInstance().purgeUserData(getPurgeUserDataQuery());
        return retrieveHSSData();
    }

    public Resolution retrieveIMSDataForMapping() {
        checkPermissions(Permissions.EPC_ADMIN);
        String identifier = getParameter("mappingIdentifier");
        String identifierType = getParameter("mappingIdentifierType");
        setIMSSubscriptionQuery(new IMSSubscriptionQuery());
        if (identifierType.equals("END_USER_SIP_URI")) {
            getIMSSubscriptionQuery().setIMSPublicIdentity(identifier);
        } else if (identifierType.equals("END_USER_PRIVATE")) {
            getIMSSubscriptionQuery().setIMSPrivateIdentity(identifier);
        }
        return retrieveHSSData();
    }
    
    public Resolution showInterconnectUploadRateCard() {
        checkPermissions(Permissions.UPLOAD_INTERCONNECT_RATE_CARD);
        return getDDForwardResolution("/ims/load_rate_card.jsp");
    }
    
    public FileBean getInterconnectRateCardFile() {
        return interconnectRateCardFile;
    }
    
    public void setInterconnectRateCardFile(FileBean interconnectRateCardFile) {
        this.interconnectRateCardFile = interconnectRateCardFile;
    }

    private FileBean interconnectRateCardFile;
    
    public Resolution uploadInterconnectRateCard() throws IOException {
        checkPermissions(Permissions.UPLOAD_INTERCONNECT_RATE_CARD);
        log.debug("Loading interconnect rate card statement");
        
        Date d = new Date();
        
        String ratecardPartner = getParameter("ratecardPartner");
        
        if (interconnectRateCardFile != null) {
            try {
                new File("/var/interconnect/in").mkdirs();
            } catch (Exception e) {
                log.debug("Error making rate card directory: [{}]", e.toString());
            }
            setPageMessage("interconnectratecard.queued.successfully");
            Utils.writeStreamToDisk(interconnectRateCardFile.getFileName() + ".tmp", interconnectRateCardFile.getInputStream(), "/var/interconnect/in");
            Utils.moveFile("/var/interconnect/in/" + interconnectRateCardFile.getFileName() + ".tmp", "/var/interconnect/in/" + ratecardPartner + "-" 
                    + interconnectRateCardFile.getFileName().replaceAll(" ", "-") + "." + d.toString().replaceAll(" ", "-"));
            log.debug("Wrote file to [{}] ", ratecardPartner + "-" + interconnectRateCardFile.getFileName());
        }
        return showInterconnectUploadRateCard();
    }
}


