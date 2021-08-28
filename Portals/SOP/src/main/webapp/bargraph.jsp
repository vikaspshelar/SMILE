<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %>
<%@taglib uri='http://cewolf.sourceforge.net/taglib/cewolf.tld' prefix='cewolf' %>

<%
    String locationFilter = request.getParameter("loc");
    String nameFilter = request.getParameter("name");
    String typeFilter = request.getParameter("type");
    String title = request.getParameter("title");
    String refreshSecs = request.getParameter("refreshsecs");
    int refreshSecsOverride = BaseUtils.getIntProperty("env.sop.refreshsecs.override", -1);
    if (refreshSecsOverride > 0) {
        refreshSecs = String.valueOf(refreshSecsOverride);
    }
    String widthS = request.getParameter("width");
    String heightS = request.getParameter("height");
    if (locationFilter == null || nameFilter == null || typeFilter == null || title == null || refreshSecs == null || widthS == null || heightS == null) {
%>
Error: URL must be of form: bargraph.jsp?loc=.*&name=.*&type=.*&width=800&height=600&title=My Title&refreshsecs=10
<%
        return;
    }

    int width = Integer.parseInt(widthS);
    int height = Integer.parseInt(heightS);

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
        <script type="text/javascript" language="javascript">
            function checkPause() {
                loc = parent.location;
                if (loc.toString().indexOf("pause", 0) != -1) {
                    parent.location = 'index.jsp';
                } else {
                    parent.location = parent.location + '&pause=yes';
                }
            }

        </script>
        <style type="text/css">
            #container img {
                height: 100%;
                width: 100%;
            }
        </style>
    </head>
    <body style="margin: 0px 0px 0px 0px" onclick="checkPause()">

        <jsp:useBean id="stats" class="com.smilecoms.sop.dp.SyslogStatsCategoryDataProducer"/>
        <jsp:useBean id="barenhancer" class="com.smilecoms.sop.chartpostprocessors.BarEnhancer" />

        <cewolf:chart id="barChart" type="verticalbar3d" title="<%=title%>" > 
            <cewolf:data>
                <cewolf:producer id="stats">
                    <cewolf:param name="location_filter" value="<%=locationFilter%>"/>
                    <cewolf:param name="name_filter" value="<%=nameFilter%>"/>
                    <cewolf:param name="type_filter" value="<%=typeFilter%>"/>
                </cewolf:producer>
            </cewolf:data>
            <cewolf:chartpostprocessor id="barenhancer"/>
        </cewolf:chart>

        <div align="middle"  id="container">
            <cewolf:img  chartid="barChart" timeout="60" renderer="cewolf" width="<%=width%>" height="<%=height%>" />    
        </div>
    </body>
</html>