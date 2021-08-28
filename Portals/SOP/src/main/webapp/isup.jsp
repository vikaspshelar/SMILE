<%@page import="com.smilecoms.commons.util.Utils"%>
<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %>

<%
            // Make sure prop is in cache if its needed and other systems are down 
            BaseUtils.getPropertyFromSQL("global.sop.isup.procedures.sql");
            
            String showISUPDownRegex = BaseUtils.getProperty("env.sop.show.isup.system.down.match", "^$");
            
            String locationFilter = request.getParameter("loc");
            String nameFilter = request.getParameter("name");
            String refreshSecs = request.getParameter("refreshsecs");
            int refreshSecsOverride = BaseUtils.getIntProperty("env.sop.refreshsecs.override", -1);
            if (refreshSecsOverride > 0) {
                refreshSecs = String.valueOf(refreshSecsOverride);
            }
            String fontsize = request.getParameter("fontsize");
            String colsString = request.getParameter("cols");
            if (locationFilter == null || nameFilter == null || colsString == null || refreshSecs == null || fontsize == null) {
%>
Error: URL must be of form: isup.jsp?loc=.*&name=.*&refreshsecs=10&cols=3&fontsize=3
<%    
return;
         }
            
            int cols = Integer.parseInt(colsString);
            List<SyslogStatistic> stats = SyslogStatsSnapshot.getStats(locationFilter, nameFilter, "isup");
            Comparator comparator = new DynamicComparator("getName", "getType", "getLocation", "asc");
            String colour = "#0FF82B";
            String sound = "";
            Collections.sort(stats, comparator);
            for (SyslogStatistic stat : stats) {
                if (stat.getValue() < 1) {
                    // must be down
                    colour = "#FE120C";
                    sound = "DHTMLSound('sounds/warning.wav')";
                    break;
                }
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
        <title>Smile IsUp</title>
        <script language="javascript">
        function DHTMLSound(surl) {
            document.getElementById("dummyspan").innerHTML="<embed src='"+surl+"' hidden=true autostart=true loop=false/>";
        }
        </script>
        
    </head>
    <body onload="<%=sound%>">
        <span id=dummyspan></span>
        
        <center>
            <font size="<%=fontsize%>">
                <table border="1">
                    
                    <tr bgcolor="<%=colour%>"><th colspan="1000">High Level Smile Systems Status</th></tr>
                    
                    
                    <tr><th bgcolor="#A7ACAC"></th>
                        <% for (int i = 0; i < cols; i++) {%>
                        <th>System</th><th>Location</th><th>Data Source</th><th bgcolor="#A7ACAC"></th>
                        <% }%>
                    </tr>
                    
                    <%
            int colcnt = 0;
            for (SyslogStatistic stat : stats) {
                String name = stat.getName();
                String location = stat.getLocation();
                String source = stat.getOther();
                String isup = stat.getValue() > 0 ? "true" : "false";
                String bgcolour;
                if (isup.equalsIgnoreCase("true") && !Utils.matchesWithPatternCache(name, showISUPDownRegex)) {
                    bgcolour = "#0FF82B";
                } else {
                    bgcolour = "#FE120C";
                }
                colcnt++;
                if (colcnt == 1) {
                    %>
                    <tr><td bgcolor="#A7ACAC"></td>
                        <%                        }
                        %>                
                        <td bgcolor="<%=bgcolour%>">
                            <a href="procedure.jsp?id=<%=name%>-<%=location%>-<%=source%>">
                                <%=name%>
                            </a>
                        </td>
                        <td bgcolor="<%=bgcolour%>"><%=location%></td>
                        <td bgcolor="<%=bgcolour%>"><%=source%></td>
                        <td bgcolor="#A7ACAC"></td>
                        
                        <%
                        if (colcnt == cols) {
                            colcnt = 0;
                    %></tr><%
                }
            }

                    %>
                    
                </table>
            </font>
            <form method="post" action="reset.jsp">                
                <input type="submit" value="Reset" name="Reset"></input>
            </form>
        </center>
    </body>
</html>





