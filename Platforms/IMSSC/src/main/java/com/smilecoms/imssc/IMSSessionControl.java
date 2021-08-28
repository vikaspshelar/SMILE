/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.imssc;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.xml.imssc.IMSSCError;
import com.smilecoms.xml.imssc.IMSSCSoap;
import com.smilecoms.xml.schema.imssc.*;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.ejb.Stateless;

/**
 *
 * @author jaybeepee
 */
@WebService(serviceName = "IMSSC", portName = "IMSSCSoap", endpointInterface = "com.smilecoms.xml.imssc.IMSSCSoap", targetNamespace = "http://xml.smilecoms.com/IMSSC", wsdlLocation = "IMSSCServiceDefinition.wsdl")
@Stateless
@HandlerChain(file = "/handler.xml")
@Local({BaseListener.class})
public class IMSSessionControl extends SmileWebService implements IMSSCSoap, BaseListener {
    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;
    private static boolean doneStartUp = false;
    
    private CSCFController getCSCFController(String url) throws Exception {
        CSCFController cscfController = CSCFController.getInstance(url);
        return cscfController;
    }
    
    @Override
    public Done deregisterIMPU(DeregisterIMPUQuery deregisterIMPUQuery) throws IMSSCError {

        setContext(deregisterIMPUQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        log.debug("IMSSC Platform: deregisterIMPU");
        
        try {
            getCSCFController(deregisterIMPUQuery.getSCSCF()).deregisterIMPU(deregisterIMPUQuery.getIMPU());
        } catch (Exception ex) {
            throw processError(IMSSCError.class, ex);
        }
        return makeDone();
    }
    
    
    @Override
    public SCSCFIMPUData getSCSCFIMPUInfo(SCSCFIMPUQuery scscfimpuQuery) throws IMSSCError {
        SCSCFIMPUData impuData;
        
        try {
            impuData = getCSCFController(scscfimpuQuery.getSCSCF()).getSCSCFIMPUData(scscfimpuQuery.getIMPU());
        } catch (Exception ex) {
            throw processError(IMSSCError.class, ex);
        }
        return impuData;
    }

    @Override
    public SCSCFStatusData getSCSCFStatus(String scscfStatusQuery) throws IMSSCError {
        SCSCFStatusData statusData = null;
        try {
            statusData = getCSCFController(scscfStatusQuery).getSCSCFStatus();
        } catch (Exception ex) {
            throw processError(IMSSCError.class, ex);
        }
        return statusData;
    }

    private Done makeDone() {
        Done done = new Done();
        done.setDone(StDone.TRUE);
        return done;
    }
    
    @Override
    public Done isUp(String isUpRequest) throws IMSSCError {
        if (!BaseUtils.isPropsAvailable()) {
            throw createError(IMSSCError.class, "Properties are not available so this platform will be reported as down");
        }
        return makeDone();
    }
    
    @PostConstruct
    public void startUp() {
         if (doneStartUp) {
            return;
        }
        doneStartUp = true;
        BaseUtils.registerForPropsAvailability(this);
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsChanges(this);
    }

    @Override
    public void propsAreReadyTrigger() {
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.registerForPropsChanges(this);
    }

    @Override
    public void propsHaveChangedTrigger() {
        Async.runOnceAcrossAllJVMs(new SmileBaseRunnable("IM.refreshCSCFConfig") {
            @Override
            public void run() {
                refreshCSCFConfig();
            }
        }, "IMSSC_CSCF_CONFIG", 30000);
    }
    
    private void refreshCSCFConfig() {
        String [] parts;
        String [] cscfs;
        String [] actions;
        Object [] params = new Object[]{};
        String tmp;
        String aa = "", cc = "";
        //text will look like: tzre-scscf1.it.tz.smilecoms.com:6060,tzre-scscf2.it.tz.smilecoms.com:6060|dispatcher.reload,interconnectroute.reload (separated by newline)
        List<String> cscfListWithActions = BaseUtils.getPropertyAsList("global.imssc.cscfactions");
        
        for (String s : cscfListWithActions) {
            try {
                parts = s.split("\\|");
                cscfs = parts[0].split(",");
                actions = parts[1].split(",");

                for (String c : cscfs) {
                    for (String a : actions) {
                        if (a.split(" ").length > 1) {
                            tmp = a.substring(a.indexOf(" ")+ 1);
                            params = tmp.split(" ");
                            a = a.split(" ")[0];
                        }
                        log.debug("Sending action [{}] to [{}]\n", new Object[]{a, c});
                        aa = a;
                        cc = c;
                        getCSCFController(c).xmlRpcExecute(a, params, false);
                    }
                }
            }catch (Exception ex) {
                log.error("Failed to issue command [{}] on cscf [{}]", new Object[]{aa,cc});
                log.warn("Error: ", ex);
            }
        }
    }

}
