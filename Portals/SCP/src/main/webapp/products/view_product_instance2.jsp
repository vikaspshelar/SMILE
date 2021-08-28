<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="product.instance.detail"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <c:set var="customer" value="${actionBean.customer}"/>
            <c:set var="productSpecification" value="${actionBean.productSpecification}"/>
            <c:set var="prodInstance" value="${actionBean.productInstance}"/>
            <table class="entity_header">
                <tr>
                    <td colspan="2"><b><fmt:message key="product.specific.info"/></b></td>
                </tr>
                <tr>
                    <td><fmt:message key="id"/>:${prodInstance.productInstanceId}</td>                    
                </tr>
            </table>
            <table class="clear">               

                <tr>
                    <td><fmt:message key="product.name"/>:</td>
                    <td>${productSpecification.name}</td>
                </tr>
                <tr>
                    <td><fmt:message key="product.description"/>:</td>
                    <td>${productSpecification.description}</td>
                </tr>
                <tr>
                    <td><fmt:message key="associated.customer.name"/>:</td>
                    <c:choose>
                        <c:when test="${prodInstance.customerId eq actionBean.customer.customerId}">
                            <td>
                                <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                    <stripes:param name="customer.customerId" value="${actionBean.customer.customerId}"/>
                                    ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                                </stripes:link>
                            </td>
                        </c:when>
                        <c:otherwise>
                            ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                        </c:otherwise>
                    </c:choose>

                </tr>
                <tr>
                    <td colspan="2"><b><fmt:message key="service.instances"/></b></td>
                </tr>

            </table>

            <table class="green" width="99%">                                
                <tr>
                    <th><fmt:message key="id"/></th>
                    <th><fmt:message key="service.name"/></th>
                    <th><fmt:message key="description"/></th>
                    <th><fmt:message key="customer"/></th>
                    <th><fmt:message key="service.charging.info"/></th>
                    <th><fmt:message key="service.configuration"/></th>
                </tr>
                <c:choose>
                    <c:when test="${prodInstance.customerId eq actionBean.customer.customerId}">
                        <c:forEach items="${prodInstance.productServiceInstanceMappings}" var="mapping">
                            <c:set var="serviceSpec" value="${s:getEntryInList(actionBean.serviceSpecList, 'getServiceSpecificationId', mapping.serviceInstance.serviceSpecificationId)}"/>
                            <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                <td>${mapping.serviceInstance.serviceInstanceId}</td>
                                <td>${serviceSpec.name}</td>
                                <td>${serviceSpec.description}</td>
                                <td>
                                    <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                        <stripes:param name="customerQuery.customerId" value="${mapping.serviceInstance.customerId}"/>
                                        ${mapping.serviceInstance.customerId}
                                    </stripes:link>
                                </td>
                                <c:set var="ratePlan" value="${s:getEntryInList(actionBean.ratePlanList, 'getRatePlanId', mapping.serviceInstance.ratePlanId)}"/>
                                <td>
                                    <stripes:link href="/Product.action" event="retrieveRatePlan"> 
                                        <stripes:param name="ratePlan.ratePlanId" value="${ratePlan.ratePlanId}"/>
                                        ${ratePlan.name}
                                    </stripes:link>
                                    <br/>
                                    (Acc: 
                                    <stripes:link href="/Account.action" event="retrieveAccount"> 
                                        <stripes:param name="accountQuery.accountId" value="${mapping.serviceInstance.accountId}"/>
                                        ${mapping.serviceInstance.accountId}</stripes:link>)
                                    </td>
                                    <td>
                                    <stripes:form action="/Product.action">
                                        <stripes:param name="serviceInstance.serviceInstanceId" value="${mapping.serviceInstance.serviceInstanceId}"/>
                                        <stripes:submit name="retrieveServiceInstance"/>                            
                                    </stripes:form>                                    
                                </td>
                            </tr>
                        </c:forEach> 
                    </c:when>                                   
                </c:choose>
            </table> 
            <br/>
        <input type="button" value="Back" onclick="previousPage();" />
        </div>

    </stripes:layout-component>
</stripes:layout-render>

