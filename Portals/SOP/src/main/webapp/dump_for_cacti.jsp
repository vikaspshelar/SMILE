<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %><%


        String locationFilter = request.getParameter("loc");
        String nameFilter = request.getParameter("name");
        if (nameFilter == null) {
            nameFilter = ".*";
        }
        String typeFilter = request.getParameter("type");
        if (typeFilter == null) {
            typeFilter = ".*";
        }
        String gtequals = request.getParameter("gt");
        if (gtequals == null) {
            gtequals = "0";
        }
        double moreThan = Double.parseDouble(gtequals);
        List<SyslogStatistic> stats = SyslogStatsSnapshot.getStats(locationFilter, nameFilter, typeFilter);
%>    
<%
        for (SyslogStatistic stat : stats) {
            double val = stat.getValue();
            if (val < moreThan) {
                continue;
            }
            long vall = (long)val;
%><%=stat.getName()%>(<%=stat.getType()%>):<%=vall%> <%}%>