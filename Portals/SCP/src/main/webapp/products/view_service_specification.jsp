<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="service.specification.detail"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <c:set var="serviceSpecification" value="${actionBean.serviceSpecification}"/>
            <table class="entity_header">
                <tr>
                    <td>${serviceSpecification.name}</td>                        
                </tr>
            </table>
            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="service.specific.info"/></b></td>
                </tr>
                <tr>
                    <td><fmt:message key="service.name"/>:</td>
                    <td>${serviceSpecification.name}</td>
                </tr>
                <tr>
                    <td><fmt:message key="service.description"/>:</td>
                    <td>${serviceSpecification.description}</td>
                </tr>                
            </table>
            <br/>
        <input type="button" value="Back" onclick="previousPage();" />
        </div>


    </stripes:layout-component>
</stripes:layout-render>

