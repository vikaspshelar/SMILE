package com.smilecoms.pc;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.props.PropertyFetchException;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAContext;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstanceOrder;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.pc.pcrf.AFSession;
import com.smilecoms.pc.pcrf.AFSessionFactory;
import com.smilecoms.pc.pcrf.IPCANSession;
import com.smilecoms.pc.pcrf.IPCANSessionFactory;
import com.smilecoms.pc.pcrf.PCCRule;
import com.smilecoms.pc.pcrf.PCCRuleFactory;
import com.smilecoms.pc.pcrf.dp.op.PCDAO;
import com.smilecoms.pc.pcrf.api.op.PCRFAPI;
import com.smilecoms.xml.pc.PCError;
import com.smilecoms.xml.pc.PCSoap;
import com.smilecoms.xml.schema.pc.*;
import java.util.List;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.smilecoms.commons.util.ExceptionManager;

/**
 *
 * @author paul
 */
@WebService(serviceName = "PC", portName = "PCSoap", endpointInterface = "com.smilecoms.xml.pc.PCSoap", targetNamespace = "http://xml.smilecoms.com/PC", wsdlLocation = "PCServiceDefinition.wsdl")
@Stateless
@HandlerChain(file = "/handler.xml")
public class PolicyControl extends SmileWebService implements PCSoap {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;
    @PersistenceContext(unitName = "PCRFDB")
    private EntityManager em;

    /*
     *
     * POLICY CONTROL FUNCTIONALITY
     *
     */
    @Override
    public Done purgeUserData(PurgeUserDataQuery purgeUserDataQuery) throws PCError {

        setContext(purgeUserDataQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        log.debug("PC Platform: purgeUserData");

        String pcrfDataModel = BaseUtils.getProperty("env.pcrf.data.model", "DB");

        if (purgeUserDataQuery.getVerbosity().equals(StPurgeUserDataVerbosity.PGW_PURGE)) {

            List<? extends IPCANSession> IPCANSessions = IPCANSessionFactory.getIPCANSessions(pcrfDataModel);

            if (pcrfDataModel.equals("DB")) {
                IPCANSessions = PCDAO.getIPCANSessionsByEndUserPrivate(em, purgeUserDataQuery.getIMSPrivateIdentity());
            } else {
                IPCANSessions = PCRFAPI.doPCRFGetIPCANSessionsByPrivateIdentity(BaseUtils.getProperty("env.pcrf.api.url", "http://10.0.1.129:8080/mobicents/"), purgeUserDataQuery.getIMSPrivateIdentity());
            }

            if (IPCANSessions != null && IPCANSessions.size() > 0) {
                for (int a = 0; a < IPCANSessions.size(); a++) {
                    EventHelper.sendRequestIPCANSessionTermination(IPCANSessions.get(a).getEndUserPrivate(), IPCANSessions.get(a).getCalledStationId());
                }
            }
        } else if (purgeUserDataQuery.getVerbosity().equals(StPurgeUserDataVerbosity.MME_PURGE)) {
            EventHelper.sendRequestMMEPurge(purgeUserDataQuery.getIMSPrivateIdentity());
        }
        return makeDone();
    }

    @Override
    public PCRFData getPCRFData(PCRFDataQuery PCRFDataQuery) throws PCError {

        setContext(PCRFDataQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        log.debug("PC Platform: getPCRFData");

        PCRFData PCRFData = new PCRFData();

        String pcrfDataModel = BaseUtils.getProperty("env.pcrf.data.model", "DB");

        List<? extends IPCANSession> IPCANSessions = IPCANSessionFactory.getIPCANSessions(pcrfDataModel);

        if (pcrfDataModel.equals("DB")) {
            IPCANSessions = PCDAO.getIPCANSessionsByEndUserPrivate(em, PCRFDataQuery.getIMSPrivateIdentity());
        } else {
            IPCANSessions = PCRFAPI.doPCRFGetIPCANSessionsByPrivateIdentity(BaseUtils.getProperty("env.pcrf.api.url", "http://10.0.1.129:8080/mobicents/"), PCRFDataQuery.getIMSPrivateIdentity());
        }

        if (IPCANSessions != null && IPCANSessions.size() > 0) {
            log.debug("There are IPCANSessions");
            PCRFData.setHasIPCANSessions(1);
            for (int a = 0; a < IPCANSessions.size(); a++) {
                IPCANSessionData IPCANSessionData = new IPCANSessionData();
                IPCANSessionData.setGxServerSessionId(IPCANSessions.get(a).getGxServerSessionId());
                IPCANSessionData.setBindingIdentifier(IPCANSessions.get(a).getBindingIdentifier());
                IPCANSessionData.setHighestPriorityServiceId(IPCANSessions.get(a).getHighestPriorityServiceId());
                IPCANSessionData.setState(IPCANSessions.get(a).getState());
                IPCANSessionData.setCalledStationId(IPCANSessions.get(a).getCalledStationId());
                PCRFData.getIPCANSessions().add(IPCANSessionData);

                List<? extends AFSession> AFSessions = AFSessionFactory.getAFSessions(pcrfDataModel);

                if (pcrfDataModel.equals("DB")) {
                    AFSessions = PCDAO.getAFSessionsFromBindingIdentifier(em, IPCANSessions.get(a).getBindingIdentifier());
                } else {
                    AFSessions = PCRFAPI.doPCRFGetAFSessionsByBindingIdentifier(BaseUtils.getProperty("env.pcrf.api.url", "http://10.0.1.129:8080/mobicents/"), IPCANSessions.get(a).getBindingIdentifier());
                }

                if (AFSessions != null && AFSessions.size() > 0) {
                    PCRFData.setHasAFSessions(1);
                    for (int b = 0; b < AFSessions.size(); b++) {
                        AFSessionData afSessionData = new AFSessionData();
                        afSessionData.setBindingIdentifier(AFSessions.get(b).getBindingIdentifier());
                        afSessionData.setRxServerSessionId(AFSessions.get(b).getRxServerSessionId());
                        afSessionData.setState(AFSessions.get(b).getState());
                        afSessionData.setType(AFSessions.get(b).getType());
                        PCRFData.getAFSessions().add(afSessionData);
                    }
                } else {
                    PCRFData.setHasAFSessions(0);
                }

                List<? extends PCCRule> PCCRules = PCCRuleFactory.getPCCRules(pcrfDataModel);

                if (pcrfDataModel.equals("DB")) {
                    PCCRules = PCDAO.getPCCRulesFromBindingIdentifier(em, IPCANSessions.get(a).getBindingIdentifier());
                } else {
                    PCCRules = PCRFAPI.doPCRFGetPCCRulesByBindingIdentifier(BaseUtils.getProperty("env.pcrf.api.url", "http://10.0.1.129:8080/mobicents/"), IPCANSessions.get(a).getBindingIdentifier());
                }

                if (PCCRules != null && PCCRules.size() > 0) {
                    PCRFData.setHasPCCRules(1);
                    for (int b = 0; b < PCCRules.size(); b++) {
                        PCCRuleData pccRuleData = new PCCRuleData();
                        pccRuleData.setBindingIdentifier(PCCRules.get(b).getBindingIdentifier());
                        pccRuleData.setPCCRuleName(PCCRules.get(b).getPccRuleName());
                        pccRuleData.setType(PCCRules.get(b).getType());
                        PCRFData.getPCCRules().add(pccRuleData);
                    }
                } else {
                    PCRFData.setHasPCCRules(0);
                }
            }
        } else {
            log.debug("There are no IPCANSessions");
            PCRFData.setHasIPCANSessions(0);
        }

        return PCRFData;
    }

    @Override
    public Done enforceVoiceAppQoS(VoiceAppQoSDataQuery VoiceAppQoSDataQuery) throws PCError {

        setContext(VoiceAppQoSDataQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        try {
            log.debug("Calling SCA to get service instance for sId [{}]", VoiceAppQoSDataQuery.getServiceInstanceId());
            ServiceInstanceQuery q = new ServiceInstanceQuery();
            q.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            q.setServiceInstanceId(VoiceAppQoSDataQuery.getServiceInstanceId());
            com.smilecoms.commons.sca.ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(q);

            int newServiceSpecId = -1;
            String currentServiceSpecIdString = "" + si.getServiceSpecificationId();
            try {
                log.debug("Checking if sID [{}] has a QoS enabled equivalent sID", currentServiceSpecIdString);

                newServiceSpecId = Integer.parseInt(BaseUtils.getSubProperty("env.pcrf.sid.qos.equivalent", currentServiceSpecIdString));
            } catch (PropertyFetchException e) {
                log.debug("No property for env.pcrf.sid.qos.equivalent [{}]", currentServiceSpecIdString);
            }

            if (newServiceSpecId != -1) {
                ProductOrder po = new ProductOrder();
                // Call as admin so that this is permitted
                po.setSCAContext(new SCAContext());
                po.getSCAContext().setAsync(Boolean.TRUE);
                po.setAction(StAction.NONE);
                po.setProductInstanceId(si.getProductInstanceId());
                ServiceInstanceOrder sio = new ServiceInstanceOrder();
                sio.setAction(StAction.UPDATE);
                sio.setServiceInstance(new com.smilecoms.commons.sca.ServiceInstance());
                sio.getServiceInstance().setServiceInstanceId(VoiceAppQoSDataQuery.getServiceInstanceId());
                sio.getServiceInstance().setProductInstanceId(si.getProductInstanceId());
                sio.getServiceInstance().setCustomerId(si.getCustomerId());
                sio.getServiceInstance().setStatus(si.getStatus());
                sio.getServiceInstance().setAccountId(si.getAccountId());
                sio.getServiceInstance().setServiceSpecificationId(newServiceSpecId);
                po.getServiceInstanceOrders().add(sio);
                SCAWrapper.getAdminInstance().processOrder(po);
                log.debug("Called SCA to change to new service spec id [{}]", newServiceSpecId);
            }
        } catch (SCABusinessError sbe) {
            log.debug("Service instance no longer exists. Ignoring", sbe);
        } catch (Exception e) {
            log.warn("Error trying to change system DPI rules [{}]", e.toString());
            new ExceptionManager(log).reportError(e);
        }

        return makeDone();
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
    public Done isUp(String isUpRequest) throws PCError {
        log.debug("On isup on PolicyControl");
        if (!BaseUtils.isPropsAvailable()) {
            throw createError(PCError.class, "Properties are not available so this platform will be reported as down");
        }
        return makeDone();
    }
}
