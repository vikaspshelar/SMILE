<%@page import="com.smilecoms.commons.util.Utils"%>
<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %>

<%
    String server = BaseUtils.getProperty("env.webrtc.server", "");
    String id = request.getParameter("id");
    List<String[]> rows = BaseUtils.getPropertyFromSQL("global.sop.isup.procedures.sql");
    StringBuilder body = new StringBuilder("");
    for (String[] row : rows) {
        String regex = row[0];
        if (Utils.matches(id, regex)) {
            body.append(row[1]);
            body.append("<br/><br/>");
        }
    }

%>    

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta content="max-age=0" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Pragma"/>
        <meta content="must-revalidate" httpequiv="Cache-Control"/>
        <title>Contact Procedures</title>
        <script type="text/javascript" src="javascript/janus/jquery.min.js" ></script>
<!--        <script type="text/javascript" src="javascript/janus/janus.nojquery.js" ></script>-->
        <script type="text/javascript" src="javascript/janus/jquery.blockUI.js" ></script>
        <script type="text/javascript" src="javascript/janus/js/bootstrap.js"></script>
        <script type="text/javascript" src="javascript/janus/js/bootbox.min.js"></script>
        <script type="text/javascript" src="javascript/janus/js/spin.min.js"></script>
        <script type="text/javascript" src="javascript/janus/js/md5.min.js"></script>
        <script type="text/javascript" src="javascript/janus/janus.js" ></script>
        <script type="text/javascript" src="javascript/janus/webrtc.js"></script>
    </head>
    <body>
        <script type="text/javascript">
            var server = "<%=server%>";
            if (server && server != "") {
                start(server);
            }
        </script>
        <%=body.toString()%>

        <div class="container hide" id="sipcall">
            <div class="row">
                <div/>
                <div id="videos" class="hide">
                    <div class="col-md-6">
                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <h3 class="panel-title">Local Video</h3>
                            </div>
                            <div class="panel-body" id="videoleft"></div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <h3 class="panel-title">Remote Video</h3>
                            </div>
                            <div class="panel-body" id="videoright"></div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </body>
</html>





