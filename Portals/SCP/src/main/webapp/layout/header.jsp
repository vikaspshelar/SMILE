<%-- 
    Document   : header
    Created on : Feb 17, 2012, 9:54:08 AM
    Author     : lesiba
--%>

<%@include file="../include/scp_include.jsp" %>

<c:if test="${!(pageContext.request.remoteUser == null)}">

    <div class="welcome">
        <fmt:message key="scp.logged.in.as"/>, ${pageContext.request.remoteUser}
    </div>
</c:if>
<jsp:include page="/layout/nav.jsp" />