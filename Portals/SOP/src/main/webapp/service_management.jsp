<%@page import="com.smilecoms.sop.vmware.HostLookup"%>
<%@page import="com.smilecoms.commons.util.Utils"%>
<%@page import="com.smilecoms.commons.base.sd.IService"%>
<%@page import="com.smilecoms.commons.base.sd.ServiceDiscoveryAgent"%>
<%@page import="com.smilecoms.commons.tags.SmileTags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><%@ taglib prefix="s" uri="http://portals.smilecoms.com/smile.tld" %>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.smilecoms.sop.vmware.HostLookup"%>
<%@page import="com.smilecoms.commons.base.sd.IService.STATUS"%>
<%@page import="com.hazelcast.core.IMap"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.smilecoms.commons.base.*, java.util.*" %>

<%

    if (request.getParameter("Clear") != null && request.getParameter("Clear").equals("Clear")) {
        ServiceDiscoveryAgent.getInstance().resetServiceList();
    }

    if (request.getParameter("Pause") != null) {
        String key = request.getParameter("endPointKey");
        if (key != null) {
            IService res = ServiceDiscoveryAgent.getInstance().getService(key);
            if (res != null) {
                res.setPaused();
                ServiceDiscoveryAgent.getInstance().publishServiceForced(res);
            }
        }
    }
    if (request.getParameter("Resume") != null) {
        String key = request.getParameter("endPointKey");
        if (key != null) {
            IService res = ServiceDiscoveryAgent.getInstance().getService(key);
            if (res != null) {
                res.resume();
                ServiceDiscoveryAgent.getInstance().publishServiceForced(res);
            }
        }
    }
    if (request.getParameter("Delete") != null) {
        String key = request.getParameter("endPointKey");
        if (key != null) {
            ServiceDiscoveryAgent.getInstance().deleteService(key);
        }
    }
    if (request.getParameter("PauseAll") != null) {
        String jvmid = request.getParameter("jvmid");
        if (jvmid != null) {
            IMap<String, IService> services = ServiceDiscoveryAgent.getInstance().getServiceMap();
            for (IService service : services.values()) {
                if (service.getJVMId().equals(jvmid)) {
                    service.setPaused();
                    ServiceDiscoveryAgent.getInstance().publishServiceForced(service);
                }
            }
        }
    }
    if (request.getParameter("ResumeAll") != null) {
        String jvmid = request.getParameter("jvmid");
        if (jvmid != null) {
            IMap<String, IService> services = ServiceDiscoveryAgent.getInstance().getServiceMap();
            for (IService service : services.values()) {
                if (service.getJVMId().equals(jvmid)) {
                    service.resume();
                    ServiceDiscoveryAgent.getInstance().publishServiceForced(service);
                }
            }
        }
    }
    Map<String, IService> services = null;
    String headerClass = "UP";
    try {
        services = ServiceDiscoveryAgent.getInstance().getServiceMapCopy();
        for (IService svc : services.values()) {
            try {
                if (!svc.isUp()) {
                    headerClass = "DOWN";
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
%>

<html>
    <head>
        <title>Services</title>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
        <meta content="max-age=0" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Pragma"/>
        <meta content="must-revalidate" httpequiv="Cache-Control"/>
        <style>
            /* Tables Green*/
            table.green {
                border-width: 1px;
                border-style: solid;
                border-color: #639B2E;
                border-collapse: collapse;
                background-color: white;
                margin: 3px 3px 3px 3px;
                table-layout: auto;

            }
            table.green th {
                border-width: 1px;
                padding: 2px;
                border-style: solid;
                border-color: #639B2E;
                color: #fff;
            }
            table.green tr {
                border-width: 1px;
                padding: 1px;
                border-style: solid;
                border-color: #639B2E;
                background-color: #fff;
            }
            table.green td {
                border-width: 1px;
                padding: 1px;
                border-style: solid;
                border-color: #639B2E;
                text-align: center;
            }
            table.green tr.UP {
                background-color: #88D13F;
            }
            table.green tr.DOWN {
                background-color: red;
            }
            table.green tr.GOING_DOWN {
                background-color: orange;
            }
            table.green tr.PAUSED {
                background-color:  yellow;
            }
        </style>
    </head>

    <body>
        <form method="get" action="service_management.jsp">                
            <input type="submit" value="Refresh" name="Refresh"></input>
        </form>
        <table class="green">
            <tr class="<%=headerClass%>">
                <th>Service</th>
                <th>Status</th>
                <th>Hostname</th>
                <th>Physical Host</th>
                <th>IP Address</th>
                <th>Port</th>
                <th>Address Part</th>
                <th>Client Match</th>
                <th>Version</th>
                <th>Weight</th>
                <th>Modtime</th>
                <th>Uptime</th>
                <th>JVM Id</th>
                <th>Pause Service</th>
                <th>Resume Service</th>
                <th>Delete Service</th>
                <th>Pause All In JVM</th>
                <th>Resume All in JVM</th>
            </tr>
            <%
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
                        try {%>
            <tr class="<%=ep.getStatus()%>">
                <td><%=ep.getServiceName()%></td>
                <td><%=ep.getStatus()%></td>
                <td><%=ep.getHostName()%></td>
                <td><%=HostLookup.getPhysicalHost(ep.getHostName())%></td>
                <td><%=ep.getIPAddress()%></td>
                <td><%=ep.getPort()%></td>
                <td><%=ep.getAddressPart()%></td>
                <td><%=ep.getClientHostnameRegexMatch()%></td>
                <td><%=ep.getVersion()%></td>
                <td><%=ep.getWeight()%></td>
                <td><%=Utils.getMillisHumanReadable(ep.getMillisSinceLastModified())%></td>
                <td><%=Utils.getMillisHumanReadable(ep.getMillisSinceLastTestFail())%></td>
                <td><%=ep.getJVMId()%></td>
                <td>
                    <form method="post" action="service_management.jsp" style="margin: 0px">                
                        <input type="hidden" name="endPointKey" value="<%=ep.getKey()%>"/>
                        <input type="submit" value="Pause" name="Pause"></input>
                    </form>
                </td>
                <td>
                    <form method="post" action="service_management.jsp" style="margin: 0px">                
                        <input type="hidden" name="endPointKey" value="<%=ep.getKey()%>"/>
                        <input type="submit" value="Resume" name="Resume"></input>
                    </form>
                </td>
                <td>
                    <form method="post" action="service_management.jsp" style="margin: 0px">                
                        <input type="hidden" name="endPointKey" value="<%=ep.getKey()%>"/>
                        <input type="submit" value="Delete" name="Delete"></input>
                    </form>
                </td>
                <td>
                    <form method="post" action="service_management.jsp" style="margin: 0px">                
                        <input type="hidden" name="jvmid" value="<%=ep.getJVMId()%>"/>
                        <input type="submit" value="Pause JVM" name="PauseAll"></input>
                    </form>
                </td>
                <td>
                    <form method="post" action="service_management.jsp" style="margin: 0px">                
                        <input type="hidden" name="jvmid" value="<%=ep.getJVMId()%>"/>
                        <input type="submit" value="Resume JVM" name="ResumeAll"></input>
                    </form>
                </td>
            </tr>

            <%
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            %>
        </table>

        <br/>
        <form method="post" action="service_management.jsp">                
            <input type="submit" value="Clear" name="Clear"></input>
        </form>

    </body>




</html>

