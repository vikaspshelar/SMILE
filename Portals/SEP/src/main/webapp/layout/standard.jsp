<%@ include file="/include/sep_include.jsp" %>
<%@page import="com.smilecoms.commons.stripes.SmileActionBean" %>
<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*, com.smilecoms.commons.sca.helpers.Permissions" %>

<stripes:layout-definition>
    <!doctype html>
    <html ng-app="SEPApp">
        <head>
            <link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.ico" type="image/x-icon" />
            <meta http-equiv="Content-type" content="text/html; charset=UTF-8" />
            <meta charset="utf-8"/>
            <title>${title}</title>
            <meta http-equiv="imagetoolbar" content="no" />
            <meta name="MSSmartTagsPreventParsing" content="true" />
            <link rel="stylesheet" media="all" type="text/css" href="${pageContext.request.contextPath}/css/style.css?v=1"/>
            <link rel="stylesheet" media="all" type="text/css" href="${pageContext.request.contextPath}/css/dhtmlgoodies_calendar.css" />
            <link rel="stylesheet" media="all" type="text/css" href="${pageContext.request.contextPath}/css/modalbox.css"/>
            <link rel="stylesheet" media="all" type="text/css" href="${pageContext.request.contextPath}/css/overcast/jquery-ui-1.8.21.custom.css" />
            <link rel="stylesheet" media="all" type="text/css" href="${pageContext.request.contextPath}/css/ticker-style.css"/>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/angular/angular.min.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/angular/controllers.js?v=1"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/browserDetect.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/OptionTransfer.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/dhtmlgoodies_calendar.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/prototype.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/builder.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/effects.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/dragdrop.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/controls.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/slider.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/sound.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/modalbox.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/sep.js?v=1"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/dygraph-combined.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-1.7.2.min.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-ui-1.8.21.custom.min.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery.ticker.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery.blockUI.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/angucomplete-alt.js"></script>
            
            <script type="text/javascript">
                var $j = jQuery.noConflict();
            </script>

            <%
                String extension = SmileActionBean.getLoggedInCCAgentExtension(request);
                if (extension != null) {
            %>
		<script type="text/javascript">
			var CTIUrl = 'wss://<%=BaseUtils.getProperty("env.portal.url")%>/sep/cti'; 
			var extensionNumber='<c:out value="<%=extension%>"/>'
		</script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/js/cti-functions.js"></script>
            <%
                }
            %>
            <stripes:layout-component name="html_head"/>
        </head>
        <body>
            <div id="page-container">

                <div id="top">
                    <jsp:include page="/layout/header.jsp"/>  
                    <div id="ticker">
                        <jsp:include page="/layout/ticker.jsp"/>
                    </div>  
                </div>


                <% if (request.getRemoteUser() != null) {%>
                <div id="leftmenu">
                    <jsp:include page="/layout/left_menu.jsp"/>
                </div>
                <% }%>
                <div id="content" ng-controller="BasicController">
                    <h1>${title}</h1>
                    <jsp:include page="/layout/errors.jsp"/>
                    <jsp:include page="/layout/messages.jsp"/>
                    <stripes:layout-component name="contents"/>
                    <div id="validationpicture">
                    </div>
                </div>
                <div id="help">
                    <jsp:include page="/layout/help.jsp"/>
                </div>
                <div id="footer">
                    <jsp:include page="/layout/footer.jsp"/>
                </div>
            </div>
        </body>
    </html>
</stripes:layout-definition>