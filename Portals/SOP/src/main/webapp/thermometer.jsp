<%@page import="com.smilecoms.commons.util.*,com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %>
<%@taglib uri='http://cewolf.sourceforge.net/taglib/cewolf.tld' prefix='cewolf' %>

<%
    String locationFilter = request.getParameter("locationfilter");
    String nameFilter = request.getParameter("namefilter");
    String typeFilter = request.getParameter("typefilter");
    String title = request.getParameter("title");
    int width = Integer.parseInt(request.getParameter("width"));
    int height = Integer.parseInt(request.getParameter("height"));
    String refreshSecs = request.getParameter("refreshsecs");
    int refreshSecsOverride = BaseUtils.getIntProperty("env.sop.refreshsecs.override", -1);
    if (refreshSecsOverride > 0) {
        refreshSecs = String.valueOf(refreshSecsOverride);
    }
    String unit = request.getParameter("unit");
    String lowerBound = request.getParameter("lower");
    String upperBound = request.getParameter("upper");
%>


<html>
    <head>
        <title><%=title%></title>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
        <meta content="max-age=0" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Pragma"/>
        <meta content="must-revalidate" httpequiv="Cache-Control"/>       
        <meta http-equiv="refresh" content="<%=refreshSecs%>" />
    </head>
    <body style="margin: 0px 0px 0px 0px">

        <jsp:useBean id="stats" class="com.smilecoms.sop.dp.SyslogStatsValueDataProducer"/>
        <jsp:useBean id="thermometerPP" class="de.laures.cewolf.cpp.ThermometerEnhancer"/>

        <cewolf:chart id="meterChart" type="thermometer" title="<%=title%>" > 
            <cewolf:data>
                <cewolf:producer id="stats">
                    <cewolf:param name="location_filter" value="<%=locationFilter%>"/>
                    <cewolf:param name="name_filter" value="<%=nameFilter%>"/>
                    <cewolf:param name="type_filter" value="<%=typeFilter%>"/>
                </cewolf:producer>
            </cewolf:data>
            <cewolf:chartpostprocessor id="thermometerPP">
                <cewolf:param name="units" value="<%=unit%>"/>
                <cewolf:param name="mercuryColor" value='<%= "#00CCCC"%>'/>
                <cewolf:param name="valueColor" value='<%= "#FFFFFF"%>'/>
                <cewolf:param name="thermometerColor" value='<%= "#000000"%>'/>
                <cewolf:param name="lowerBound" value="<%=lowerBound%>"/>
                <cewolf:param name="upperBound" value="<%=upperBound%>"/>
            </cewolf:chartpostprocessor>
        </cewolf:chart>

        <div align="middle">
            <cewolf:img chartid="meterChart" timeout="10" renderer="cewolf" width="<%=width%>" height="<%=height%>" />    
        </div>
    </body>
</html>