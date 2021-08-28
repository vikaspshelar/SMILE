<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="service.instance.detail"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <c:set var="serviceSpecification" value="${actionBean.serviceSpecification}"/>
            <table class="entity_header">
                <tr>
                    <td colspan="2"><b><fmt:message key="service.specific.info"/></b></td>
                </tr>
                <tr>
                    <td><fmt:message key="id"/>:${actionBean.serviceInstance.serviceInstanceId}</td>                    
                </tr>
            </table>

            <table class="clear">
                
                <tr>
                    <td><fmt:message key="product.instance.id"/>:</td>
                    <c:choose>
                        <c:when test="${actionBean.serviceInstance.customerId eq actionBean.customer.customerId}">
                            <td>
                                <stripes:link href="/Product.action" event="retrieveProductInstance"> 
                                    <stripes:param name="productInstance.productInstanceId" value="${actionBean.serviceInstance.productInstanceId}"/>
                                    ${actionBean.serviceInstance.productInstanceId}
                                </stripes:link>
                            </td>
                        </c:when>
                        <c:otherwise>
                            <td>
                                ${actionBean.serviceInstance.productInstanceId}
                            </td>
                        </c:otherwise>
                    </c:choose>

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
                    <td><fmt:message key="associated.customer.name"/>:</td>
                    <c:choose>
                        <c:when test="${actionBean.serviceInstance.customerId eq actionBean.customer.customerId}">
                            <td>
                                <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                    <stripes:param name="customer.customerId" value="${actionBean.customer.customerId}"/>
                                    ${actionBean.customer.firstName} ${actionBean.customer.lastName} 
                                </stripes:link>
                            </td>
                        </c:when>
                        <c:otherwise>
                            <td>
                                ${actionBean.serviceInstanceAssociatedCustomer}
                            </td>
                        </c:otherwise>
                    </c:choose>

                </tr>
                <tr>
                    <td><fmt:message key="account.id"/>:</td>
                    <c:choose>
                        <c:when test="${actionBean.serviceInstance.customerId eq actionBean.customer.customerId}">
                            <td>
                                <stripes:link href="/Account.action" event="retrieveAccount"> 
                                    <stripes:param name="accountQuery.accountId" value="${actionBean.serviceInstance.accountId}"/>
                                    ${actionBean.serviceInstance.accountId}
                                </stripes:link>
                            </td>
                        </c:when>
                        <c:otherwise>
                            <td>
                                ${actionBean.serviceInstance.accountId}
                            </td>
                        </c:otherwise>
                    </c:choose>
                </tr>                
            </table>
            <br/>
        <input type="button" value="Back" onclick="previousPage();" />
        </div>
        <br/>
        

    </stripes:layout-component>
</stripes:layout-render>

