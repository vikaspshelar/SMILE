<%@page import="com.smilecoms.commons.util.Utils"%>
<%@page import="com.smilecoms.commons.util.*,com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %>

<%
    String w = request.getParameter("width");
    String h = request.getParameter("height");
    String dimensions = "";
    if (h != null && w != null) {
        dimensions = " width=" + w + " height=" + h + " ";
    }

    String client = request.getParameter("client");
    String refreshSecs = request.getParameter("refreshsecs");
    if (client == null) {
        client = "all";
    }
    if (refreshSecs == null) {
        refreshSecs = "120";
    }
    int refreshSecsOverride = BaseUtils.getIntProperty("env.sop.refreshsecs.override", -1);
    if (refreshSecsOverride > 0) {
        refreshSecs = String.valueOf(refreshSecsOverride);
    }
    String cmd = "/var/smile/install/scripts/utils/show_routing.sh " + System.getProperty("com.sun.aas.instanceRoot") + "/applications/SOP/SCA_Routing.svg" ;
    String res = SSH.executeRemoteOSCommand("root", BaseUtils.getProperty("env.sop.os.root.passwd"), "localhost", cmd);


%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta http-equiv="refresh" content="<%=refreshSecs%>" />
        <meta content="max-age=0" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Pragma"/>
        <meta content="must-revalidate" httpequiv="Cache-Control"/>
        <title>Smile SOAP Routing</title>
    </head>
    <body>
        <center>
            <img <%=dimensions%> src="SCA_Routing.svg"/>
        </center>
    </body>
</html>





