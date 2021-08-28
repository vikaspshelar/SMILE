<%-- 
    Document   : page_help
    Created on : Feb 17, 2012, 11:40:45 AM
    Author     : lesiba
--%>

<%@include file="/include/scp_include.jsp" %>
<div id="page-help">..............................................................................................................................................................
    <br />
    <br />
    <span style="color:#339900"><strong><fmt:message key="page.help.title"/></strong></span> 
    <br />
    <fmt:message key="${pageContext.request.servletPath}"/>  
    <br />
    <br />..............................................................................................................................................................
    <br/>
    <br/>
    
    <%-- to be moved to black strip on navigation bar--%>
    
    <c:if test="${!(pageContext.request.remoteUser == null)}">
        <fmt:message key="logged.in.as"/> ${pageContext.request.remoteUser}   
        <br/> <stripes:link href="/Login.action" event="logOut"><fmt:message key="log.out"/></stripes:link>
            &nbsp; &nbsp; 
    </c:if>
    <c:if test="${pageContext.request.remoteUser == null}">
        <fmt:message key="not.logged.in"/>
    </c:if>
</div>