<?xml version="1.0" encoding="UTF-8"?>
<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*, org.apache.commons.lang.*" %><%
        response.setContentType("text/xml");

        String locationFilter = request.getParameter("loc");
        if (locationFilter == null) {
            locationFilter = ".*";
        }
        String nameFilter = request.getParameter("name");
        if (nameFilter == null) {
            nameFilter = ".*";
        }
        String typeFilter = request.getParameter("type");
        if (typeFilter == null) {
            typeFilter = ".*";
        }
        String gt = request.getParameter("gt");
        if (gt == null) {
            gt = "0";
        }
        double moreThan = Double.parseDouble(gt);
        
        List<SyslogStatistic> stats = SyslogStatsSnapshot.getStats(locationFilter, nameFilter, typeFilter);
        
        Comparator comparator = new DynamicComparator("getName", "getType", "getLocation", "asc");
        Collections.sort(stats, comparator);
%> 
<SopStats>
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
%><Stat location="<%=StringEscapeUtils.escapeXml(location)%>" name="<%=StringEscapeUtils.escapeXml(name)%>" type="<%=StringEscapeUtils.escapeXml(type)%>" value="<%=StringEscapeUtils.escapeXml(value)%>"/><%}%>
</SopStats>