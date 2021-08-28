<%@ include file="/include/sep_include.jsp" %>

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
                <tr>
                    <td colspan="2"><b><fmt:message key="service.configuration"/></b></td>
                </tr>
            </table>

            <table class="green">
                <tr>
                    <th>Attribute</th>
                    <th>Default Value</th> 
                </tr>
                <c:forEach items="${serviceSpecification.AVPs}" var="avp" varStatus="loop">
                    <s:avp-display-row avp="${avp}"/>
                </c:forEach>
            </table>

        </div>

        <br/>
        <input type="button" value="<fmt:message key="back"/>" onclick="previousPage();" />

    </stripes:layout-component>
</stripes:layout-render>

