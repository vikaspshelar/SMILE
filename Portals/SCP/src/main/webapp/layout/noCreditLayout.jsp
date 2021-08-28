
<%@include  file="../include/scp_include.jsp" %>
<%@page import="com.smilecoms.commons.stripes.SmileActionBean" %>
<stripes:layout-definition>
    <%@page contentType="text/html" pageEncoding="UTF-8"%>
    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
    <html ng-app>
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <title>MySmile - ${title}</title>

            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/base.css" type="text/css" /> 
            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/layout.css" type="text/css" /> 
            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/skeleton.css" type="text/css" /> 
            <link rel="stylesheet" media="all" href="${pageContext.request.contextPath}/css/fonts.css" type="text/css" />
            <link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.ico" type="image/x-icon" />

            <stripes:layout-component name="head"/>
        </head>
        <body>    

            <div class="container" id="content">

                <div class="sixteen columns">
                    <div class="logo six columns">
                        <img src="images/logo.jpg">
                    </div>
                    <div class="nav eight columns">
                        <ul class="main_menu">
                            <li>
                                <a href="<fmt:message key="scp.header.backto.website"/>">
                                    <div class="home_menu">&nbsp;<div class="menu_title"><fmt:message key="scp.loginpage.backto.msg"/></div></div>
                                </a>
                            </li>
                            <li>
                                <a href="<fmt:message key="scp.contact.us.link"/>">
                                    <div class="contact_menu">&nbsp;<div class="menu_title_contact"><fmt:message key="scp.contact.us"/></div></div>
                                </a>
                            </li>
                        </ul>
                    </div>
                </div>

                <div class="sixteen columns alpha">
                    <div class="recharge_heading light">
                        ${title}
                    </div>
                    <div style="top: 50px;" class="sixteen columns alpha">
                        <stripes:layout-component name="contents"/>
                    </div>
                </div>

                <div class="sixteen columns alpha footer_terms">
                    <div class="copyright columns">
                        <fmt:message key="scp.footer.copyright.msg"/>
                    </div>
                    <div class="terms">
                        <a href="<fmt:message key="scp.footer.termsofuse.link"/>" title="<fmt:message key="scp.footer.termsofuse"/>" target="_blank"><fmt:message key="scp.footer.termsofuse"/></a> |
                        <a href="<fmt:message key="scp.footer.termsandconditions.link"/>" title="<fmt:message key="scp.footer.termsandconditions"/>" target="_blank"><fmt:message key="scp.footer.termsandconditions"/></a> |
                        <a href="<fmt:message key="scp.footer.privacypolicy.link"/>" title="<fmt:message key="scp.footer.privacypolicy"/>" target="_blank"><fmt:message key="scp.footer.privacypolicy"/></a>
                    </div>
                </div>

            </div>

        </body>
    </html>
</stripes:layout-definition>