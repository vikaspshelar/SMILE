<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="service.instances"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>
                        <fmt:message key="service.instances.identified.by">
                            <fmt:param>${actionBean.serviceInstanceQuery.identifier}</fmt:param>
                        </fmt:message>
                    </td>                    
                </tr>
            </table>
            <br/>

            <c:if test="${actionBean.serviceInstanceList.numberOfServiceInstances > 0}">
                <table class="green" width="99%"> 
                    <tr>
                        <th><fmt:message key="service.name"/></th>
                        <th><fmt:message key="provisioning.status"/></th>
                        <th>Customer</th>
                        <th><fmt:message key="service.charging.info"/></th>                    
                        <th><fmt:message key="service.configuration"/></th>
                    </tr>

                    <c:forEach items="${actionBean.serviceInstanceList.serviceInstances}" var="serviceInstance" varStatus="loop">                        
                        <c:set var="serviceSpec" value="${s:getServiceSpecification(serviceInstance.serviceSpecificationId)}"/>
                        <c:set var="ratePlan" value="${s:getRatePlan(serviceInstance.ratePlanId)}"/>
                        <c:if test="${serviceInstance.status == 'TD'}">
                            <tr class="greyout">
                            </c:if>
                            <c:if test="${serviceInstance.status != 'TD'}">
                            <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            </c:if>
                            <td>
                                <stripes:link href="/ProductCatalog.action" event="retrieveServiceInstance">
                                    <stripes:param name="serviceInstance.serviceInstanceId" value="${serviceInstance.serviceInstanceId}"/>
                                    ${serviceSpec.name}
                                </stripes:link>                                
                            </td>              
                            <td>
                                ${serviceInstance.status}
                            </td>   
                            <td>
                                <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                    <stripes:param name="customerQuery.customerId" value="${serviceInstance.customerId}"/>
                                    ${s:getServiceInstanceUserName(serviceInstance.serviceInstanceId)}
                                </stripes:link>
                            </td>
                            <td>
                                <stripes:link href="/ProductCatalog.action" event="retrieveRatePlan"> 
                                    <stripes:param name="ratePlan.ratePlanId" value="${ratePlan.ratePlanId}"/>
                                    ${ratePlan.name}
                                </stripes:link>
                                <br/>
                                (Account: 
                                <stripes:link href="/Account.action" event="retrieveAccount"> 
                                    <stripes:param name="accountQuery.accountId" value="${serviceInstance.accountId}"/>
                                    ${serviceInstance.accountId}
                                </stripes:link>)
                            </td>  
                            <td>
                                <stripes:form action="/ProductCatalog.action" class="buttonOnly">
                                    <stripes:param name="serviceInstance.serviceInstanceId" value="${serviceInstance.serviceInstanceId}"/>
                                    <stripes:submit name="retrieveServiceInstance"/>                            
                                </stripes:form>
                            </td>
                        </tr>
                    </c:forEach>


                </table>
            </c:if>
            <c:if test="${actionBean.serviceInstanceList.numberOfServiceInstances == 0}">
                <table class="green" width="99%"> 
                    <tr>
                        <td>
                            <fmt:message key="no.service.instances"/>
                        </td>
                    </tr>
                </table>
            </c:if>
        </div>

        <br/>
        <input type="button" value="Back" onclick="previousPage();"/>

    </stripes:layout-component>
</stripes:layout-render>
