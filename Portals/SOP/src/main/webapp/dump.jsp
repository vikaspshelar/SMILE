<%@page import="java.net.URLEncoder"%>
<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %>

<%


    String locationFilter = request.getParameter("loc");
    String nameFilter = request.getParameter("name");
    String typeFilter = request.getParameter("type");
    String refreshSecs = request.getParameter("refreshsecs");
    int refreshSecsOverride = BaseUtils.getIntProperty("env.sop.refreshsecs.override", -1);
    if (refreshSecsOverride > 0) {
        refreshSecs = String.valueOf(refreshSecsOverride);
    }
    String width = request.getParameter("width");
    if (width == null) {
        width = "1000";
    }
    String height = request.getParameter("height");
    if (height == null) {
        height = "300";
    }
    String gt = request.getParameter("gt");
    if (locationFilter == null || nameFilter == null || typeFilter == null || refreshSecs == null || gt == null) {
%>
Error: URL must be of form: dump.jsp?loc=.*&name=.*&type=.*&refreshsecs=10&amp;gt=0
<%
        return;
    }

    double moreThan = Double.parseDouble(gt);
    List<SyslogStatistic> stats = SyslogStatsSnapshot.getStats(locationFilter, nameFilter, typeFilter);
    Comparator comparator = new DynamicComparator("getName", "getType", "getLocation", "asc");
    Collections.sort(stats, comparator);
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
        <title>Smile Stats Dump</title>

    </head>
    <body>
        <table border="1">
            <tr>
                <th>Location</th>
                <th>Statistic Name</th>
                <th>Statistic Type</th>
                <th>Value</th>
                <th>Graph Last Hour</th>
                <th>Graph Last Day</th>
                <th>Graph Last Week</th>
                <th>Graph Last Month</th>
                <th>Graph Last Year</th>
            </tr>
            <%
                for (SyslogStatistic stat : stats) {
                    double val = stat.getValue();
                    if (val < moreThan) {
                        continue;
                    }
                    String name = stat.getName();
                    String location = stat.getLocation();
                    String value = String.valueOf(val);
                    String type = stat.getType();
                    
                    String lnk = "graphs.jsp?loc=" + URLEncoder.encode(location,"utf-8") + "&name=" + URLEncoder.encode(name,"utf-8") + "&type=^" + URLEncoder.encode(type,"utf-8") + "$&width=" + width + "&height=" + height;
                    lnk = lnk.replace("^", "%5E");
            %>
            <tr>
                <td><%=location%></td>
                <td><%=name%></td>
                <td><%=type%></td>
                <td><%=value%></td>
                <td align="center"><a href="<%=lnk%>&starthoursback=1">Last Hour</a></td>
                <td align="center"><a href="<%=lnk%>&starthoursback=24">Last Day</a></td>
                <td align="center"><a href="<%=lnk%>&starthoursback=168">Last Week</a></td>
                <td align="center"><a href="<%=lnk%>&starthoursback=744">Last Month</a></td>
                <td align="center"><a href="<%=lnk%>&starthoursback=8760">Last Year</a></td>
            </tr>
            <%
                }
            %>
        </table>
    </body>
</html>





