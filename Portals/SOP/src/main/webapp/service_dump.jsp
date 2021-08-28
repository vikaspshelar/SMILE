<%@page import="com.smilecoms.commons.util.Utils"%>
<%@page import="com.smilecoms.commons.base.sd.IService"%>
<%@page import="com.smilecoms.sop.vmware.HostLookup"%>
<%@page import="com.smilecoms.commons.base.sd.ServiceDiscoveryAgent"%>
<%@page import="com.smilecoms.commons.tags.SmileTags"%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><%@ taglib prefix="s" uri="http://portals.smilecoms.com/smile.tld" %><%@page import="java.text.SimpleDateFormat"%><%@page import="com.smilecoms.commons.base.sd.IService.STATUS"%><%@page import="com.hazelcast.core.IMap"%><%@page contentType="text/html" pageEncoding="UTF-8"%><%@page import="com.smilecoms.commons.base.*, java.util.*" %>
<%

    Map<String, IService> services = null;
    try {
        services = ServiceDiscoveryAgent.getInstance().getServiceMapCopy();
    } catch (Exception e) {
        e.printStackTrace();
    }
    if (services != null) {

        List<IService> list = new ArrayList<IService>();
        for (String key : services.keySet()) {
            try {
            list.add(services.get(key));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        List<IService> orderedlist = SmileTags.orderList(list, "getURL", "ASC");

        for (IService ep : orderedlist) {
            try {%><%=ep.getServiceName()%>#<%=ep.getStatus()%>#<%=ep.getHostName()%>#<%=HostLookup.getPhysicalHost(ep.getHostName())%>#<%=ep.getIPAddress()%>#<%=ep.getPort()%>#<%=ep.getAddressPart()%>#<%=ep.getClientHostnameRegexMatch()%>#<%=ep.getVersion()%>#<%=ep.getWeight()%>#<%=Utils.getMillisHumanReadable(ep.getMillisSinceLastModified())%>#<%=Utils.getMillisHumanReadable(ep.getMillisSinceLastTestFail())%>#<%=ep.getJVMId()%>
<%
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    %>