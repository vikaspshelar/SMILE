<%@page import="com.smilecoms.commons.util.Utils"%>
<%@page import="java.io.FileReader"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%

    String matchFilter = request.getParameter("match");

    if (matchFilter == null) {
        matchFilter = ".*";
    }
    String line = "";

%>    

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta content="max-age=0" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Pragma"/>
        <meta content="must-revalidate" httpequiv="Cache-Control"/>
        <title>Smile Errors</title>

    </head>
    <body>
        <%            BufferedReader br = new BufferedReader(new FileReader("/opt/smile/var/log/recent_err.log"));

            for (; (line = br.readLine()) != null;) {
                if (matchFilter.equals(".*") || Utils.matchesWithPatternCache(line, matchFilter)) {
        %>
        <%=line%><br/>
        <%
                }
            }
        %>
    </body>
</html>





