<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %>
     
<%
        
        String name = request.getParameter("name");
        String type = request.getParameter("type");
        String loc = request.getParameter("loc");
        double value = Double.parseDouble(request.getParameter("val"));
        BaseUtils.sendStatistic(loc, name, type, value, "");
        
%>    
OK