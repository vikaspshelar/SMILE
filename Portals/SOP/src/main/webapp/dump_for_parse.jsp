<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %><%


        String locationFilter = request.getParameter("loc");
        String nameFilter = request.getParameter("name");
        String typeFilter = request.getParameter("type");
        double moreThan = Double.parseDouble(request.getParameter("gt"));
        List<SyslogStatistic> stats = SyslogStatsSnapshot.getStats(locationFilter, nameFilter, typeFilter);
        Comparator comparator = new DynamicComparator("getName", "getType", "getLocation", "asc");
        Collections.sort(stats, comparator);
%>    
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
            String other = stat.getOther();
%><%=location%>|<%=name%>|<%=value%>|<%=type%>|<%=other%>#<%}%>