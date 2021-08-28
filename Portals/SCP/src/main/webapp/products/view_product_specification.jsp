<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="product.specification.detail"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">            
            <c:set var="productSpecification" value="${actionBean.productSpecification}"/>            
            <table class="entity_header">
                <tr>
                    <td>${productSpecification.name}</td>                        
                </tr>
            </table>

            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="product.specific.info"/></b></td>
                </tr>
                <tr>
                    <td><fmt:message key="product.name"/>:</td>
                    <td>${productSpecification.name}</td>
                </tr>
                <tr>
                    <td><fmt:message key="product.description"/>:</td>
                    <td>${productSpecification.description}</td>
                </tr>
                <tr>
                    <td><fmt:message key="available.from"/>:</td>
                    <td>${s:formatDateLong(productSpecification.availableFrom)}</td>
                </tr>
                <tr>
                    <td><fmt:message key="available.to"/>:</td>
                    <td>${s:formatDateLong(productSpecification.availableTo)}</td>
                </tr>  

                <tr>
                    <td colspan="2"><b><fmt:message key="services"/></b></td>
                </tr>
            </table>

            <table class="green">                                
                <tr>
                    <th>Service Name</th>
                    <th>Service Description</th>
                    <th>Occurrences</th>
                    <th>Rate Plan</th>
                </tr>
                <c:forEach items="${productSpecification.productServiceSpecificationMappings}" var="service" varStatus="loop">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">    
                        <td>
                            <stripes:link href="/Product.action" event="retrieveServiceSpecification">                            
                                <stripes:param name="serviceSpecification.serviceSpecificationId" value="${service.serviceSpecification.serviceSpecificationId}"/>
                                ${service.serviceSpecification.name}
                            </stripes:link>
                        </td>
                        <td>${service.serviceSpecification.description}</td>
                        <td>${service.minServiceOccurences} - ${service.maxServiceOccurences}</td>

                        <c:forEach items="${actionBean.ratePlanList}" var="ratePlan">
                            <c:if test="${ratePlan.ratePlanId == service.ratePlanId}">
                                <td>
                                    <stripes:link href="/Product.action" event="retrieveRatePlan">
                                        <stripes:param name="ratePlan.ratePlanId" value="${service.ratePlanId}"/>
                                        ${ratePlan.name}
                                    </stripes:link>
                                </td>
                            </c:if>
                        </c:forEach>
                    </tr>
                </c:forEach>                
            </table>       
            <br/>
        <input type="button" value="Back" onclick="previousPage();" />
        </div>
    </stripes:layout-component>
</stripes:layout-render>

