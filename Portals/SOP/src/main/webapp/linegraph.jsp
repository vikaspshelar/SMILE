<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %>
<%@taglib uri='http://cewolf.sourceforge.net/taglib/cewolf.tld' prefix='cewolf' %>

<%
    String file = request.getParameter("file");
    String title = request.getParameter("title");
    String xaxis = request.getParameter("xaxis");
    String yaxis = request.getParameter("yaxis");
    int width = Integer.parseInt(request.getParameter("width"));
    int height = Integer.parseInt(request.getParameter("height"));
    String refreshSecs = request.getParameter("refreshsecs");
    int refreshSecsOverride = BaseUtils.getIntProperty("env.sop.refreshsecs.override", -1);
    if (refreshSecsOverride > 0) {
        refreshSecs = String.valueOf(refreshSecsOverride);
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
        <style type="text/css">
            #container img {
                width: 100%;
                height: 100%;
            }
        </style>

    </head>
    <body style="margin: 0px 0px 0px 0px" onclick="checkPause()">

        <jsp:useBean id="filedp" class="com.smilecoms.sop.dp.FileDataProducer"/>
        <jsp:useBean id="lineenhancer" class="com.smilecoms.sop.chartpostprocessors.LineEnhancer" />

        <cewolf:chart id="XYChart" type="timeseries"
        title="<%=title%>"
        xaxislabel="<%=xaxis%>"
        yaxislabel="<%=yaxis%>" >
            <cewolf:data>
                <cewolf:producer id="filedp">
                    <cewolf:param name="filelocation" value="<%=file%>"/>
                </cewolf:producer>
            </cewolf:data>
            <cewolf:chartpostprocessor id="lineenhancer"/>
        </cewolf:chart>

        <div align="middle"  id="container">
            <cewolf:img chartid="XYChart" timeout="60" renderer="cewolf" width="<%=width%>" height="<%=height%>" />
        </div>
    </body>
</html>