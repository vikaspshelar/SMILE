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

                    <stripes:form  action="/Login.action">
                        <label><fmt:message key="user.name"/></label>
                        <input type="text" name="username" size="30" maxlength="50" /><br><br>
                        <label><fmt:message key="password"/></label>
                        <input type="password" name="password" size="10" maxlength="20" /><br><br>
                        <input type="submit" name="wifiLogin" value="Login"/>
                    </stripes:form>
                        <br/>
                        IP: ${actionBean.originatingIP}
    </stripes:layout-component> 
</stripes:layout-render>
