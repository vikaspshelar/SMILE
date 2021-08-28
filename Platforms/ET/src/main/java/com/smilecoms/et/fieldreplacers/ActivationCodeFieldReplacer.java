/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.et.fieldreplacers;

import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.IMSNestedIdentityAssociation;
import com.smilecoms.commons.sca.IMSPrivateIdentity;
import com.smilecoms.commons.sca.IMSPublicIdentity;
import com.smilecoms.commons.sca.IMSSubscriptionQuery;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceActivationData;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.util.Utils;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class ActivationCodeFieldReplacer implements IEventFieldReplacer {

    private static final Logger log = LoggerFactory.getLogger(ActivationCodeFieldReplacer.class);

    @Override
    public String getValue(String eventData) {

        log.debug("Getting activation code data for event data [{}]", eventData);
        StringBuilder sb = new StringBuilder();

        int productInstanceId = Integer.parseInt(eventData.split("\\|")[1]);
        ProductInstance pi = SCAWrapper.getAdminInstance().getProductInstance(productInstanceId, StProductInstanceLookupVerbosity.MAIN_SVC_SVCAVP);
        int cnt = 0;
        for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
            ServiceInstance si = m.getServiceInstance();
            log.debug("Looking at service instance id [{}]", si.getServiceInstanceId());
            for (AVP avp : si.getAVPs()) {
                log.debug("Looking at AVP [{}]", avp.getAttribute());
                if (avp.getAttribute().equals("PublicIdentity")) {
                    log.debug("Found publicIdentity [{}]", avp.getValue());
                    String impu = avp.getValue();
                    ServiceActivationData sad = getServiceActivationData(impu);
                    IMSPrivateIdentity firstIMPI = sad.getIMSSubscription().getIMSPrivateIdentities().get(0);
                    IMSPublicIdentity sipUnbarredIMPU = getSIPUnbarredIMPU(firstIMPI);
                    cnt++;
                    sb.append("<PERSONALIZATION><TAG_NAME>PHONE_NUMBER_").append(cnt).append("</TAG_NAME><VALUE>").append(Utils.getFriendlyPhoneNumber(sipUnbarredIMPU.getIdentity())).append("</VALUE></PERSONALIZATION>");
                    sb.append("<PERSONALIZATION><TAG_NAME>ACT_CODE_").append(cnt).append("</TAG_NAME><VALUE>").append(sad.getActivationCode()).append("</VALUE></PERSONALIZATION>");
                    sb.append("<PERSONALIZATION><TAG_NAME>ACCOUNT_ID_").append(cnt).append("</TAG_NAME><VALUE>").append(si.getAccountId()).append("</VALUE></PERSONALIZATION>");
                    sb.append("<PERSONALIZATION><TAG_NAME>FRIENDLY_NAME_").append(cnt).append("</TAG_NAME><VALUE>").append(StringEscapeUtils.escapeXml(pi.getFriendlyName())).append("</VALUE></PERSONALIZATION>");
                    sb.append("<PERSONALIZATION><TAG_NAME>ICCID_").append(cnt).append("</TAG_NAME><VALUE>").append(pi.getPhysicalId()).append("</VALUE></PERSONALIZATION>");
                    break;
                }
            }
        }
        String ret = sb.toString();
        log.debug("Activation code data is [{}]", ret);
        return ret;
    }

    private ServiceActivationData getServiceActivationData(String impu) {
        IMSSubscriptionQuery q = new IMSSubscriptionQuery();
        q.setIMSPublicIdentity(impu);
        ServiceActivationData sad = SCAWrapper.getAdminInstance().getServiceActivationData(q);
        return sad;
    }

    private IMSPublicIdentity getSIPUnbarredIMPU(IMSPrivateIdentity impi) {
        IMSPublicIdentity sipUnbarredIMPU = null;
        // Returned first unbarred sip impu
        for (IMSNestedIdentityAssociation assoc : impi.getImplicitIMSPublicIdentitySets().get(0).getAssociatedIMSPublicIdentities()) {
            sipUnbarredIMPU = assoc.getIMSPublicIdentity();
            if (sipUnbarredIMPU.getBarring() != 0) {
                continue;
            }
            if (sipUnbarredIMPU.getIdentity().startsWith("sip:")) {
                break;
            }
        }
        return sipUnbarredIMPU;
    }
}
