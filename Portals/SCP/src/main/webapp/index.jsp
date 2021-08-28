<%-- 
    Document   : index
    Created on : Feb 17, 2012, 2:09:50 PM
    Author     : lesiba
--%>

<%@include file="/include/scp_include.jsp" %>
<c:set var="title">
    <fmt:message key="scp.home"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="copy_main">

            <br />
            <br />
            <strong>Hi, ${actionBean.customer.firstName} </strong> 
            <br /><fmt:message key="scp.welcome"/>
        </div>
        <div id="smile_welcome"></div>
        <br/>
        <div style="clear:both; margin:10px 0 0 13px;text-align:left; font-family:Arial, Helvetica, sans-serif; font-size:14px;">
            <stripes:form action="/Customer.action">
                <stripes:link href="/Customer.action" event="retrieveCustomer"><fmt:message key="scp.click.here"/></stripes:link>
                <stripes:param name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
            </stripes:form>        
        </div>
        <br/>

    </stripes:layout-component>
</stripes:layout-render>