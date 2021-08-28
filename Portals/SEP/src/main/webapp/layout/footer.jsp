<%@page import="com.smilecoms.commons.base.BaseUtils"%>
<%@ include file="/include/sep_include.jsp" %>
<c:if test="${!(pageContext.request.remoteUser == null)}">
    <fmt:message key="logged.in.as"/> ${pageContext.request.remoteUser}   
    <br/>
    <stripes:link href="/Login.action" event="showPermissions">  <fmt:message key="view.permissions"/></stripes:link>
        &nbsp; &nbsp;
</c:if>
<c:if test="${pageContext.request.remoteUser == null}">
    <fmt:message key="not.logged.in"/>
    &nbsp; &nbsp;
</c:if>
<a href="#" onclick="alert('${actionBean.SCACallInfo}'); return false;">SCA Calls from <%=BaseUtils.getHostNameFromKernel()%></a>
