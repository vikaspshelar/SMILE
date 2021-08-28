<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="products"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <c:set value="${actionBean.productSpecificationList.productSpecifications}" var="productSpecifications"/>
        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td><img width="50px" height="50px" src="images/Unknown-person.gif"> ${actionBean.customer.firstName} ${actionBean.customer.lastName} </td>                    
                </tr>
            </table>
            <table class="green">                                
                <tr>
                    <th>Name</th>
                    <th>Description</th>
                    <th>Available From</th>  
                    <th>Available To</th>
                </tr>

                <c:forEach items="${productSpecifications}" var="catalog" varStatus="loop">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>
                            <stripes:link href="/Product.action" event="retrieveProductSpecification">                            
                                <stripes:param name="productSpecification.productSpecificationId" value="${catalog.productSpecificationId}"/>
                                ${catalog.name}
                            </stripes:link>
                        </td>
                        <td>${catalog.description}</td>
                        <td>${s:formatDateLong(catalog.availableFrom)}</td>        
                        <td>${s:formatDateLong(catalog.availableTo)}</td>      
                    </tr>
                </c:forEach>
            </table>
            <br/>
        <input type="button" value="Back" onclick="previousPage();" />
        </div>
    </stripes:layout-component>
</stripes:layout-render>

