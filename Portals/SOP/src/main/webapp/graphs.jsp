<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.DateFormat"%>
<%@page import="com.smilecoms.commons.util.Utils"%>
<%@page import="com.smilecoms.commons.util.*,com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %>

<%
    String width = request.getParameter("width");
    if (width == null) {
        width = "1000";
    }
    String height = request.getParameter("height");
    if (height == null) {
        height = "200";
    }
    String locationFilter = request.getParameter("loc");
    String nameFilter = request.getParameter("name");
    if (nameFilter == null || nameFilter.isEmpty()) {
        nameFilter = ".*";
    }
    String typeFilter = request.getParameter("type");
    String refreshSecs = request.getParameter("refreshsecs");
    if (refreshSecs == null) {
        refreshSecs = "300";
    }
    String starthoursback = request.getParameter("starthoursback");
    if (starthoursback == null) {
        starthoursback = "25";
    }
    String endhoursback = request.getParameter("endhoursback");
    if (endhoursback == null) {
        endhoursback = "0";
    }
    int refreshSecsOverride = BaseUtils.getIntProperty("env.sop.refreshsecs.override", -1);
    if (refreshSecsOverride > 0) {
        refreshSecs = String.valueOf(refreshSecsOverride);
    }
    List<SyslogStatistic> stats;
    if (locationFilter != null && nameFilter != null && typeFilter != null && !locationFilter.isEmpty() && !nameFilter.isEmpty() && !typeFilter.isEmpty()) {
        stats = SyslogStatsSnapshot.getStats(locationFilter, nameFilter, typeFilter);
        if (stats.isEmpty()) {
            SyslogStatistic specificstat = new SyslogStatistic();
            stats.add(specificstat);
            specificstat.setLocation(locationFilter);
            specificstat.setName(nameFilter);
            specificstat.setType(typeFilter);
            specificstat.setValue(1000000);
        }
    } else {
        locationFilter = "";
        nameFilter = ".*";
        typeFilter = ".*";
        stats = new ArrayList();
    }

    Comparator comparator = new DynamicComparator("getName", "getType", "getLocation", "asc");
    Collections.sort(stats, comparator);
    String gt = request.getParameter("gt");
    if (gt == null) {
        gt = "0";
    }
    double moreThan = Double.parseDouble(gt);
    Set<String> types = SyslogStatsSnapshot.getStatTypes();
    Set<String> locations = SyslogStatsSnapshot.getStatLocations();
    Set<String> names = SyslogStatsSnapshot.getStatNames();

    String startdt = request.getParameter("startdt");

    if (startdt != null && !startdt.isEmpty() && !startdt.equals("yyyy/MM/dd HH")) {
        Date start = Utils.getStringAsDate(startdt, "yyyy/MM/dd HH", null);
        long msBack = System.currentTimeMillis() - start.getTime();
        starthoursback = String.valueOf(msBack / 3600000);
    } else {
        startdt = "yyyy/MM/dd HH";
    }

    String enddt = request.getParameter("enddt");

    if (enddt != null && !enddt.isEmpty() && !enddt.equals("yyyy/MM/dd HH")) {
        Date end = Utils.getStringAsDate(enddt, "yyyy/MM/dd HH", null);
        long msBack = System.currentTimeMillis() - end.getTime();
        endhoursback = String.valueOf(msBack / 3600000);
    } else {
        enddt = "yyyy/MM/dd HH";
    }
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
        <title>Smile SOP Graphs</title>
        <style type="text/css"> 
            body {
                font-family: Arial, Helvetica, Verdana, Sans-serif;
                font-size: 12px;
                color: #EBF0EB;
            }
            a {
                color: #EBF0EB;
                text-decoration: none;
            }
        </style>
    </head>
    <body bgcolor="#000000">
        <form action="graphs.jsp">
            <table align="center">
                <tr>
                    <td align="center"> 
                        Location Regex: 
                        <input type="text" name="loc" size="20" value="<%=locationFilter%>" list="locationlist"/>
                <datalist id="locationlist">
                    <option value=".*"/>
                    <%
                        for (String loc : locations) {
                    %>
                    <option value="<%=loc%>"/>
                    <%
                        }
                    %>
                </datalist>
                </td>
                <td align="center"> 
                    Name Regex: <input type="text" name="name" size="20" value="<%=nameFilter%>" list="namelist"/>
                <datalist id="namelist">
                    <option value=".*"/>
                    <%
                        for (String name : names) {
                    %>
                    <option value="<%=name%>"/>
                    <%
                        }
                    %>
                </datalist>
                </td>
                <td align="center"> 
                    Type:  <input type="text" name="type" size="12" value="<%=typeFilter%>" list="typelist"/>
                <datalist id="typelist">
                    <option value=".*"/>
                    <%
                        for (String type : types) {
                    %>
                    <option value="<%=type%>"/>
                    <%
                        }
                    %>
                </datalist>
                </td>
                <td>Period:</td>
                <td align="center"> 
                    (Time Frame:  
                    <select name="starthoursback">
                        <option value="1" <% if (starthoursback.equals("1")) {%>selected<%}%>>Last Hour</option>
                        <option value="2" <% if (starthoursback.equals("2")) {%>selected<%}%>>Last 2 Hours</option>
                        <option value="6" <% if (starthoursback.equals("6")) {%>selected<%}%>>Last 6 Hours</option>
                        <option value="12" <% if (starthoursback.equals("12")) {%>selected<%}%>>Last 12 Hours</option>
                        <option value="24" <% if (starthoursback.equals("24")) {%>selected<%}%>>Last Day</option>
                        <option value="48" <% if (starthoursback.equals("48")) {%>selected<%}%>>Last 2 Days</option>
                        <option value="168" <% if (starthoursback.equals("168")) {%>selected<%}%>>Last Week</option>
                        <option value="744" <% if (starthoursback.equals("744")) {%>selected<%}%>>Last Month</option>
                        <option value="2232" <% if (starthoursback.equals("2232")) {%>selected<%}%>>Last 3 Months</option>
                        <option value="4464" <% if (starthoursback.equals("4464")) {%>selected<%}%>>Last 6 Months</option>
                        <option value="8760" <% if (starthoursback.equals("8760")) {%>selected<%}%>>Last Year</option>
                    </select> 
                    ) or (
                </td>                
                <td align="center"> 
                    Start: <input type="text" name="startdt" size="13" maxlength="13" value="<%=startdt%>"/>
                </td>
                <td align="center"> 
                    End: <input type="text" name="enddt" size="13" maxlength="13" value="<%=enddt%>"/> )
                </td>
                <td align="center"> 
                    W: <input type="text" name="width" size="4" value="<%=width%>"/>
                </td>
                <td align="center"> 
                    H: <input type="text" name="height" size="4" value="<%=height%>"/>
                </td>
                <td align="center"> 
                    Refresh Secs: <input type="text" name="refreshsecs" size="4" value="<%=refreshSecs%>"/>
                </td>
                <td align="center"> 
                    <input type="submit" name="Go" value="Go"/>
                </td>
                </tr>
            </table>
        </form>
        <br/>
        <table width="100%">
            <%
                for (SyslogStatistic stat : stats) {
                    double val = stat.getValue();
                    if (val < moreThan) {
                        continue;
                    }
                    String name = stat.getName();
                    String location = stat.getLocation();
                    String type = stat.getType();
            %>
            <tr>
                <td align="center"> 
                    <img src="GraphServlet?loc=<%=location%>&name=<%=name%>&type=<%=type%>&width=<%=width%>&height=<%=height%>&starthoursback=<%=starthoursback%>&endhoursback=<%=endhoursback%>"/>
                </td>
            </tr>
            <tr><td><br/></td></tr>
                    <%}%>
        </table>
    </body>
</html>





