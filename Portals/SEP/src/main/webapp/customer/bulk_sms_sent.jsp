<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="bulk.sms.sent"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <stripes:form action="/Customer.action">
            <stripes:submit name="showSendBulkSMS" />
        </stripes:form>

    </stripes:layout-component>   
</stripes:layout-render>