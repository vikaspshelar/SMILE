<%-- 
    Document   : index
    Created on : Feb 16, 2012, 6:12:04 PM
    Author     : lesiba
--%>

<%@page import="com.smilecoms.commons.stripes.SmileActionBean"%>
<%@include file="/include/scp_include.jsp" %>

<c:if test="${pageContext.request.remoteUser != null}">
    <%
        // Log user out if logged in
        SmileActionBean.invalidateSession(request);
    %>
</c:if>

<c:set var="title">
    <fmt:message key="customer.login"/>
</c:set>

<stripes:layout-render name="/layout/wifiLayout.jsp">
    <stripes:layout-component name="contents">

                    You are logged in with IP: ${actionBean.originatingIP}
                    
    </stripes:layout-component> 
</stripes:layout-render>
