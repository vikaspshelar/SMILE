<%@include  file="../include/scp_include.jsp" %>
<%@page import="com.smilecoms.commons.stripes.SmileActionBean" %>
<stripes:layout-definition>
    <%@page contentType="text/html" pageEncoding="UTF-8"%>
    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
    <html ng-app="scpApp">
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <title>MySmile - ${title}</title>
            <%--<link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/reset.css" type="text/css"/>
            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/styles.css" type="text/css"/>
            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/nav.css" type="text/css" />
            
            --%> 

            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/base.css" type="text/css" /> 
            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/layout.css?v=1" type="text/css" /> 
            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/skeleton.css" type="text/css" />
            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/fonts.css" type="text/css" /> 
            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/data_bundles.scss?v=1"/>
            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/recharge.scss"/>
            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/sections.scss"/>
            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/constants.scss"/>
           
            <link rel="stylesheet" href="${pageContext.request.contextPath}/css/jquery-ui.css" type="text/css" /> 
            <link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.ico" type="image/x-icon" />
            
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/scp.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/dygraph-combined.js"></script>

            <script type="text/javascript" src="${pageContext.request.contextPath}/js/angular/angular.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/angular/app.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/angular/directives.js?v=1"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/angular/services.js?v=1"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/angular/controllers.js?v=1"></script>

            <script type="text/javascript" src="${pageContext.request.contextPath}/js/scripts.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/menu.js"></script>

            <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery.latest.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-ui.js"></script>
            <script type="text/javascript" src="${pageContext.request.contextPath}/js/dir-pagination.js"></script>
            


            <script type="text/javascript">

                var _gaq = _gaq || [];
                _gaq.push(['_setAccount', '${s:getProperty('env.google.analytics.user.code')}']);
                _gaq.push(['_trackPageview']);

                (function () {
                    var ga = document.createElement('script');
                    ga.type = 'text/javascript';
                    ga.async = true;
                    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
                    var s = document.getElementsByTagName('script')[0];
                    s.parentNode.insertBefore(ga, s);
                })();

            </script>            
            <stripes:layout-component name="head"/>
        </head>
        <body>    

            <div class="container" id="content">
                <c:if test="${!(pageContext.request.remoteUser == null)}">
                    <!-- Start of the header -->
                    <jsp:include page="/layout/header.jsp"/>

                    <div class="sixteen columns alpha" ng-controller="BasicController">
                        <!-- Start of content -->
                        <div class="recharge_heading light">
                            ${title}
                        </div>
                        <div style="top: 50px;" class="sixteen columns alpha">
                            <stripes:layout-component name="contents"/>
                        </div>
                    </div>
                </c:if>
                <c:if test="${(pageContext.request.remoteUser == null)}">
                    <!-- Start of the header -->
                    <jsp:include page="/layout/header.jsp"/>


                    <div class="sixteen columns alpha">
                        <!-- Start of content -->                        
                        <stripes:layout-component name="contents"/>

                    </div>
                </c:if>


                <!-- Start of the footer -->
                <c:if test="${pageContext.request.session.new == false && pageContext.request.remoteUser != null}">
                    <jsp:include page="/layout/footer.jsp"/>
                </c:if>

                <div class="sixteen columns alpha">
                    <c:if test="${not empty s:getSubProperty('env.scp.banner.pages', title)}">
                        <c:set var="bannerConfig" value="${s:getSubProperty('env.scp.banner.pages', title)}"/>
                        <c:set var="bannerConfigBits" value="${fn:split(bannerConfig,'|')}"/>
                        <c:set var="bannerURL" value="${bannerConfigBits[0]}"/>
                        <c:set var="actionBeanConfig" value="${bannerConfigBits[1]}"/>
                        <%--Choose Account=https://smile.com.ng/email_images/scp_unlimited_image.png|Account#showRechargePaymentOptions--%>
                        <%--Choose Account=https://smile.com.ng/email_images/scp_unlimited_image.png|URL#https://smile.co.ug/voice/--%>
                        <c:if test="${not empty actionBeanConfig}">
                            <c:set var="actionBeanConfigBits" value="${fn:split(actionBeanConfig,'#')}"/>
                            <c:set var="actionName" value="${actionBeanConfigBits[0]}"/>
                            <c:set var="eventName" value="${actionBeanConfigBits[1]}"/>

                            <c:choose>
                                <c:when test="${actionName eq 'URL'}">
                                    <a href="${eventName}" target="_blank">
                                        <img src="${bannerURL}"/>
                                    </a>
                                </c:when>
                                <c:otherwise>
                                    <stripes:link href="/${actionName}.action" event="${eventName}">
                                        <img src="${bannerURL}"/>
                                    </stripes:link>
                                </c:otherwise>
                            </c:choose>

                        </c:if>
                        <c:if test="${empty actionBeanConfig}">
                            <img src="${bannerURL}"/>
                        </c:if>
                    </c:if>
                </div>

            </div>
        </body>
    </html>
</stripes:layout-definition>