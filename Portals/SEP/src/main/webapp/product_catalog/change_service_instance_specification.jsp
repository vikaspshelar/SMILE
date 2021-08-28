<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="change.service.specification"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>
                        <fmt:message key="service.name"/>:
                        ${actionBean.serviceSpecification.name}
                    </td>
                </tr>
            </table>

            <c:if test="${s:getListSize(actionBean.productServiceSpecificationMappings) == 0}">      
                <table class="green">
                    <tr>
                        <td>
                            <fmt:message key="no.alternative.service.specifications.exist"/>
                        </td>
                    </tr>
                </table>
            </c:if>    
            <c:if test="${s:getListSize(actionBean.productServiceSpecificationMappings) > 0}">      
                <table class="green">

                    <tr>
                        <th>Service Name</th>
                        <th>Service Description</th>
                        <th>Occurrences</th>
                        <th>Rate Plan</th>
                        <th>Select</th>
                    </tr>

                    <c:forEach items="${actionBean.productServiceSpecificationMappings}" var="service" varStatus="loop">
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">    
                            <td>${service.serviceSpecification.name}</td>
                            <td>${service.serviceSpecification.description}</td>
                            <td>${service.minServiceOccurences} - ${service.maxServiceOccurences}</td>
                            <td>${s:getRatePlan(service.ratePlanId).name}</td>
                            <td>
                                <stripes:form action="/ProductCatalog.action">
                                    <input type="hidden" name="newServiceSpecificationId" value="${service.serviceSpecification.serviceSpecificationId}"/>
                                    <stripes:hidden name="serviceInstance.serviceSpecificationId" value="${actionBean.serviceInstance.serviceSpecificationId}"/>
                                    <stripes:hidden name="serviceInstance.serviceInstanceId" value="${actionBean.serviceInstance.serviceInstanceId}"/>
                                    <stripes:hidden name="productInstance.productSpecificationId" value="${actionBean.productInstance.productSpecificationId}"/>
                                    <stripes:submit name="configureChangeServiceInstanceSpecification"/>
                                </stripes:form>                                
                            </td>
                        </tr>
                    </c:forEach>             
                </table>     
            </c:if>  

        </div>

        <br/>
        <input type="button" value="Back" onclick="previousPage();"/>

    </stripes:layout-component>

</stripes:layout-render>

