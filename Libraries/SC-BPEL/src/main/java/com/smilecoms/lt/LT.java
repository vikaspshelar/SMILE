/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.lt;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.sd.IService;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import java.util.Set;
import org.slf4j.LoggerFactory;

public class LT {

    private static final String EPREF1 = "<service-ref xmlns=\"http://docs.oasis-open.org/wsbpel/2.0/serviceref\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"><wsa:EndpointReference><wsa:Address>";
    private static final String EPREF2 = "</wsa:Address><wsa:ServiceName xmlns:";
    private static final String EPREF3 = "=\"http://xml.smilecoms.com/";
    private static final String EPREF4 = "\" PortName=\"";
    private static final String EPREF5 = "Soap" + "\">";
    private static final String EPREF6 = ":";
    private static final String EPREF7 = "</wsa:ServiceName></wsa:EndpointReference></service-ref>";
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LT.class);

    public static String getEndPoint(String platform) throws Exception {
        String url = ServiceDiscoveryAgent.getInstance().getAvailableService(platform).getURL();
        return makeXMLEPR(url, platform);
    }

    public static String getEndPointAddresses() throws Exception {
        try {

            if (BaseUtils.IN_SHUTDOWN) {
                // This is needed for SCA clients that dont use Hazelcast to know what resources are available
                throw new Exception("SCA is in shutdown and reporting itself as down");
            }

            boolean hasAnAvailableService = false;
            for (String availableService : ServiceDiscoveryAgent.getInstance().getCurrentUpServices().keySet()) {
                if (!availableService.equals("SCA")) {
                    hasAnAvailableService = true;
                    break;
                }
            }

            if (!hasAnAvailableService && BaseUtils.getBooleanPropertyFailFast("env.sca.noplatforms.showscaasdown", false)) {
                throw new Exception("Reporting this SCA as being down due to there being no platforms endpoints available and env.sca.noplatforms.showscaasdown=true");
            }

            StringBuilder addresses = new StringBuilder();
            for (Set<IService> availableForService : ServiceDiscoveryAgent.getInstance().getCurrentUpServices().values()) {
                for (IService service : availableForService) {
                    addresses.append(service.getServiceName());
                    addresses.append("|");
                    addresses.append(service.getURL());
                    addresses.append("|");
                    addresses.append("1");
                    addresses.append("#");
                }
            }
            return addresses.toString();
        } catch (Exception e) {
            log.warn("Error preparing list of alive services on this SCA. Returning empty string. [{}]", e.toString());
            log.warn("Error: ", e);
            return "";
        }
    }

    private static String makeXMLEPR(String address, String platform) {
        StringBuilder buf = new StringBuilder(EPREF1);
        buf.append(address);
        buf.append(EPREF2);
        buf.append(platform);
        buf.append(EPREF3);
        buf.append(platform);
        buf.append(EPREF4);
        buf.append(platform);
        buf.append(EPREF5);
        buf.append(platform);
        buf.append(EPREF6);
        buf.append(platform);
        buf.append(EPREF7);
        return buf.toString();
    }
}
