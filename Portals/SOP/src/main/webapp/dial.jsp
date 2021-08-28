<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %>
<%@taglib uri='http://cewolf.sourceforge.net/taglib/cewolf.tld' prefix='cewolf' %>

<%
    String locationFilter = request.getParameter("loc");
    String nameFilter = request.getParameter("name");
    String typeFilter = request.getParameter("type");
    String title = request.getParameter("title");

    float heightF = Float.parseFloat(request.getParameter("height") != null ? request.getParameter("height") : "900");
    float widthF = Float.parseFloat(request.getParameter("width") != null ? request.getParameter("width") : "800");
    // Get the lesser of the width and height
    int height = 0;
    int width = 0;
    if (widthF <= heightF) {
        //Tall and narrow - width is the limiting factor and hight can be bigger
        height = (int) (widthF * 1.04F);
        width = (int) (widthF);
    }
    if (widthF > heightF) {
        //Long and Thin - height is the limiting factor and width can be bigger
        height = (int) (heightF);
        width = (int) (heightF * 0.96F);
    }
    // Compensate for things being just off square
    if (height > heightF) {
        height = (int) heightF;
    }
    if (width > widthF) {
        width = (int) widthF;
    }

    String refreshSecs = request.getParameter("refreshsecs");
    int refreshSecsOverride = BaseUtils.getIntProperty("env.sop.refreshsecs.override", -1);
    if (refreshSecsOverride > 0) {
        refreshSecs = String.valueOf(refreshSecsOverride);
    }
    String unit = request.getParameter("unit");
    String lowerBound = request.getParameter("lower");
    String upperBound = request.getParameter("upper");
    String major = request.getParameter("major");
    String minor = request.getParameter("minor");
    String sumOrAvg = request.getParameter("agg");
    String highwarn = request.getParameter("highwarn");
    if (highwarn == null) {
        // backwards compatability
        highwarn = request.getParameter("warn");
    }
    String lowwarn = request.getParameter("lowwarn");

    if (highwarn == null) {
        highwarn = "999999999";
    }
    if (lowwarn == null) {
        lowwarn = "-999999999";
    }


    String sound = "";
    //sound = "DHTMLSound('sounds/warning.wav')";

    if (locationFilter == null || nameFilter == null || typeFilter == null || title == null || refreshSecs == null || unit == null || lowerBound == null
            || upperBound == null || major == null || minor == null || sumOrAvg == null) {
%>
Error: URL must be of form: dial.jsp?loc=.*&name=.*&type=.*&width=800&height=600&title=My Title&refreshsecs=10&unit=X&lower=X&upper=X&major=X&minor=X&agg=sum_avg
<%
        return;
    }
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
        <script language="javascript">
            function DHTMLSound(surl) {
                document.getElementById("dummyspan").innerHTML = "<embed src='" + surl + "' hidden=true autostart=true loop=false/>";
            }
        </script>

        <style type="text/css">
            #container img {
                height: 100%;
                width: 100%;
            }
        </style>

    </head>
    <body style="margin: 0px 0px 0px 0px" onclick="checkPause()" onload="<%=sound%>">
        <jsp:useBean id="stats" class="com.smilecoms.sop.dp.SyslogStatsValueDataProducer"/>
        <jsp:useBean id="dialPP" class="com.smilecoms.sop.chartpostprocessors.DialEnhancer"/>

        <cewolf:chart id="dialChart" type="dial" title="<%=title%>" > 
            <cewolf:data>
                <cewolf:producer id="stats">
                    <cewolf:param name="location_filter" value="<%=locationFilter%>"/>
                    <cewolf:param name="name_filter" value="<%=nameFilter%>"/>
                    <cewolf:param name="type_filter" value="<%=typeFilter%>"/>
                    <cewolf:param name="sum_avg" value="<%=sumOrAvg%>"/>
                </cewolf:producer>
            </cewolf:data>
            <cewolf:chartpostprocessor id="dialPP">
                <cewolf:param name="dialText" value="<%=unit%>"/>
                <cewolf:param name="lowerBound" value="<%=lowerBound%>"/>
                <cewolf:param name="upperBound" value="<%=upperBound%>"/>
                <cewolf:param name="majorTickIncrement" value="<%=major%>"/>
                <cewolf:param name="minorTickCount" value="<%=minor%>"/>
                <cewolf:param name="highwarn" value="<%=highwarn%>"/>
                <cewolf:param name="lowwarn" value="<%=lowwarn%>"/>
                <cewolf:param name="title" value="<%=title%>"/>
            </cewolf:chartpostprocessor>
        </cewolf:chart>

        <div align="middle"  id="container">
            <cewolf:img  chartid="dialChart" timeout="60" renderer="cewolf" width="<%=width%>" height="<%=height%>" />    
        </div>
    </body>
</html>



