package com.smilecoms.imssc;

import com.smilecoms.commons.base.BaseUtils;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.xmlrpc.client.AsyncCallback;
import org.apache.xmlrpc.client.XmlRpcLiteHttpTransportFactory;
import org.slf4j.*;
import com.smilecoms.xml.schema.imssc.*;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Set;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.serializer.StringSerializer;
import static org.apache.xmlrpc.serializer.StringSerializer.STRING_TAG;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author Jason Penton
 */
public class CSCFController {

    private static final Logger log = LoggerFactory.getLogger(CSCFController.class.getName());

    public static final Integer FAILURE = -1;
    public static final Integer OK = 0;    //props for XMLRPC calls

    private String xmlRPCUrl = "";
    private static final String HTTP = "http://";
    private static final String RPC2 = "/RPC2";
    private XmlRpcClient rpcClient = null;
    private static final Map<String, CSCFController> ccMap = new HashMap();

    public static CSCFController getInstance(String xmlRPCURL) throws Exception {

        CSCFController ret = ccMap.get(xmlRPCURL);

        if (ret == null) {
            //No call controller for that url in the cache - create one
            synchronized (ccMap) {
                ret = new CSCFController(xmlRPCURL);
                ccMap.remove(xmlRPCURL); // In case 2 threads get here one after each other
                ccMap.put(xmlRPCURL, ret);
            }
        }

        if (ret.rpcClient == null) {
            synchronized (ccMap) {
                ccMap.remove(xmlRPCURL);
            }
            throw new Exception("Error in the XMLRPCClient - the client connection is null because it could not be created");
        }
        return ret;
    }

    /**
     * Constructor for Call Controller - private for singleton pattern
     *
     * @throws java.lang.Exception
     *
     */
    private CSCFController(String xmlRPCURL) throws Exception {
        this.xmlRPCUrl = HTTP + xmlRPCURL + RPC2;
        this.createXMLRPCClient();

    }

    private HashMap execute(String cmd, Object[] params, boolean async) throws Exception {
        long startTime = 0;

        if (log.isDebugEnabled()) {
            log.debug("Calling " + cmd + " via xmlrpc on CSCF at " + this.xmlRPCUrl);
            startTime = System.currentTimeMillis();
        }

        HashMap ret = null;
        try {
            if (async) {
                rpcClient.executeAsync(cmd, params, new AsyncCallback() {

                    @Override
                    public void handleResult(XmlRpcRequest xrr, Object o) {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                    @Override
                    public void handleError(XmlRpcRequest xrr, Throwable thrwbl) {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }
                });
                ret = null;
            } else {
                ret = (HashMap) rpcClient.execute(cmd, params);
            }
        } catch (ClassCastException e) {
            log.debug("ignoring class cast exceptions");
        } catch (Exception e) {
            log.info("Error calling " + cmd + " via xmlrpc on CSCF at " + this.xmlRPCUrl + ". Error: " + e.toString());
            throw new Exception("Error calling " + cmd + " on CSCF via XMLRPC -- Error: [" + e.toString() + "] CSCF location: [" + xmlRPCUrl + "]");
        }

        if (log.isDebugEnabled()) {
            long callTime = System.currentTimeMillis() - startTime;
            log.debug("Successfully finished calling " + cmd + " via xmlrpc on CSCF at " + this.xmlRPCUrl + ". Call took " + callTime + "ms"); //+ " Result is: [" + ret + "]");
        }
        return ret;
    }

    protected HashMap xmlRpcExecuteLeavingNewline(String cmd, Object[] params, boolean async) throws Exception {
        return execute(cmd, params, async);
    }

    protected HashMap xmlRpcExecute(String cmd, Object[] params, boolean async) throws Exception {
        HashMap result = execute(cmd, params, async);
        return result;
    }

    private String removeNewline(String orig) {
        if (orig == null) {
            return null;
        }
        if (orig.equals("")) {
            return null;
        }
        StringBuffer returnString = new StringBuffer("");

        try {
            if (orig.indexOf("\n") > 0) {
                try {
                    String returnSplit[] = orig.split("\n", -1);
                    for (int i = 0; i < returnSplit.length; i++) {
                        returnString.append(returnSplit[i]);
                    }
                } catch (IndexOutOfBoundsException iobe) {
                    log.warn("Error: ", iobe);
                }
            } else {
                returnString.append(orig);
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
        return returnString.toString();
    }

    private void createXMLRPCClient() throws Exception {

        try {
            rpcClient = null;
            rpcClient = new XmlRpcClient();
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            if (log.isDebugEnabled()) {
                log.debug("SERVER URL IS " + this.xmlRPCUrl);
            }
            config.setServerURL(new URL(this.xmlRPCUrl));
            config.setConnectionTimeout(BaseUtils.getIntProperty("global.imssc.xmlrpc.connect.timeout.millis"));
            config.setReplyTimeout(BaseUtils.getIntProperty("global.imssc.xmlrpc.reply.timeout.millis"));
            rpcClient.setConfig(config);
            rpcClient.setTransportFactory(new XmlRpcCommonsTransportFactory(rpcClient));
            rpcClient.setTypeFactory(new MyTypeFactoryImpl(rpcClient));
        } catch (Exception e) {
            rpcClient = null;
            log.error("Error occurred initialising XMLRPC environment to connect to CSCF: " + e.toString());
            throw e;
        }
    }

    /*here we set info for generic SCSCF, whether it be I, P or S */
    public CSCFData getCSCFStatus() throws Exception {
        Object[] params = new Object[]{};
        CSCFData cscfData = new CSCFData();

        HashMap uptimeData = this.xmlRpcExecute("core.uptime", params, false);
        HashMap memoryData = this.xmlRpcExecute("core.shmmem", params, false);
        cscfData.setUptime((Integer) (uptimeData.get("uptime")));
        cscfData.setTotalMemory((Integer) memoryData.get("total"));
        cscfData.setFreeMemory((Integer) memoryData.get("free"));
        cscfData.setUsedMemory((Integer) memoryData.get("used"));
        cscfData.setMaxUsedMemory((Integer) memoryData.get("max_used"));

        return cscfData;
    }

    public SCSCFStatusData getSCSCFStatus() throws Exception {
        HashMap usrlocStats;
        Object[] params = new Object[]{};
        SCSCFStatusData scscfStatusData = new SCSCFStatusData();

        //set generic data
        CSCFData genericData = getCSCFStatus();
        scscfStatusData.setTotalMemory(BigInteger.valueOf(genericData.getTotalMemory()));
        scscfStatusData.setFreeMemory(BigInteger.valueOf(genericData.getFreeMemory()));
        scscfStatusData.setUsedMemory(BigInteger.valueOf(genericData.getUsedMemory()));
        scscfStatusData.setMaxUsedMemory(BigInteger.valueOf(genericData.getMaxUsedMemory()));
        scscfStatusData.setUptime(BigInteger.valueOf(genericData.getUptime()));

        //now scscf-specific data
        HashMap usrlocData = this.xmlRpcExecute("ulscscf.status", params, false);
        usrlocStats = (HashMap) usrlocData.get("Stats");
        scscfStatusData.setUsrlocHashSize(BigInteger.valueOf((Integer) usrlocData.get(("Size"))));
        scscfStatusData.setUsrlocRecords(BigInteger.valueOf((Integer) usrlocStats.get("Records")));
        scscfStatusData.setUsrlocMaxSlots(BigInteger.valueOf((Integer) usrlocStats.get("Max-Slots")));

        return scscfStatusData;
    }

    public void deregisterIMPU(String impu) throws Exception {
        Object[] params = new Object[]{impu};
        this.xmlRpcExecute("regscscf.dereg_impu", params, false);

    }

    public SCSCFIMPUData getSCSCFIMPUData(String impu) throws Exception {
        SCSCFIMPUData impuData = new SCSCFIMPUData();
        IMPUSubscription impuSubscription = new IMPUSubscription();
        Object[] params = new Object[]{impu};

        HashMap impuHash = this.xmlRpcExecute("ulscscf.showimpu", params, false);
        if (impuHash == null) {
            throw new Exception("IMPU not found in SCSCF");
        }
        impuData.setBarring((Integer) impuHash.get("barring"));
        impuData.setCcf1((String) impuHash.get("ccf1"));
        impuData.setCcf2((String) impuHash.get("ccf2"));
        impuData.setEcf1((String) impuHash.get("ecf1"));
        impuData.setEcf2((String) impuHash.get("ecf2"));
        impuData.setState((String) impuHash.get("state"));
        impuData.setIMPU((String) impuHash.get("impu"));

        if (impuHash.containsKey("subscription")) {
            HashMap subscriptionHash = (HashMap) impuHash.get("subscription");
            impuSubscription.setIMPI((String) subscriptionHash.get("impi"));
            HashMap serviceProfilesHash = (HashMap) subscriptionHash.get("service profiles");
            Set serviceProfiles = serviceProfilesHash.entrySet();
            Iterator it = serviceProfiles.iterator();
            while (it.hasNext()) {
                IMPUServiceProfile serviceProfile = new IMPUServiceProfile();
                Map.Entry me = (Map.Entry) it.next();
                HashMap spIMPUsHash = (HashMap) ((HashMap) (me.getValue())).get("impus");
                Set spIMPUs = spIMPUsHash.entrySet();
                Iterator ita = spIMPUs.iterator();
                while (ita.hasNext()) {
                    Map.Entry impume = (Map.Entry) ita.next();
                    log.debug("adding IMPU to Service profile " + (String) impume.getValue());
                    serviceProfile.getIMPU().add((String) impume.getValue());
                }
                log.debug("Adding Service Profile");
                impuSubscription.getServiceProfiles().add(serviceProfile);
            }
            impuData.setSubscription(impuSubscription);
        }

        if (impuHash.containsKey("contacts")) {
            IMPUContact impuContact;
            HashMap contactsHash = (HashMap) impuHash.get("contacts");
            Set contactSet = contactsHash.entrySet();
            Iterator ita = contactSet.iterator();

            while (ita.hasNext()) {
                Map.Entry me = (Map.Entry) ita.next();
                impuContact = new IMPUContact();
                HashMap contactDetail = (HashMap) me.getValue();
                impuContact.setAoR((String) me.getKey());
                impuContact.setExpires(BigInteger.valueOf((Integer) contactDetail.get("expires")));
                impuContact.setUserAgent((String) contactDetail.get("client"));
                impuData.getContacts().add(impuContact);
            }
        }

        return impuData;
    }

    /**
     * I need these 2 inner classes to be able to override the default string
     * type in apache XML RPC client to write the <string> tag Kamailio requires
     * the string tag and by default apache xmlrpcclient ommits it. e.g.
     * <?xml version="1.0" encoding="UTF-8"?><methodCall>
     * <methodName>ulscscf.showimpu</methodName><params><param><value><string>somestring</string></value></param></params></methodCall>
     */
    private class MyStringSerializer extends StringSerializer {

        public void write(ContentHandler pHandler, Object pObject)
                throws SAXException {
            // Write <string> tag explicitly
            write(pHandler, STRING_TAG, pObject.toString());
        }
    }

    private class MyTypeFactoryImpl extends TypeFactoryImpl {

        public MyTypeFactoryImpl(XmlRpcController pController) {
            super(pController);
        }

        public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException {
            if (pObject instanceof String) {
                return new MyStringSerializer();
            } else {
                return super.getSerializer(pConfig, pObject);
            }
        }
    }
}
