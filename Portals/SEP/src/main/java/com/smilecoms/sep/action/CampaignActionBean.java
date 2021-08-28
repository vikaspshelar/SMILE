/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.action;

import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.util.Utils;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.Resolution;

/**
 *
 * @author paul
 */
public class CampaignActionBean extends SmileActionBean {
    
    @DefaultHandler
    public Resolution showStoreCampaignData() {
        checkPermissions(Permissions.CAMPAIGN_MANAGEMENT);
        return getDDForwardResolution("/campaign/edit_campaign.jsp");
    }
    
    public Resolution storeCampaignData() {
        log.debug("In storeCampaignData");
        checkPermissions(Permissions.CAMPAIGN_MANAGEMENT);
        getCampaignData().setProductInstanceIds(Utils.zip(getCampaignData().getProductInstanceIds().replace("\r\n", ",")));
        SCAWrapper.getUserSpecificInstance().storeCampaignData(getCampaignData());
        setPageMessage("campaign.data.stored.successfully");
        return getDDForwardResolution("/campaign/edit_campaign.jsp");
    }
    
}
