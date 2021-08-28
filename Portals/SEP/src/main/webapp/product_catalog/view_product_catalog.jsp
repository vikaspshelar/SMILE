<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="products"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <table class="green">                                
            <tr>
                <th>Name</th>
                <th>Description</th>
                <th>Available From</th>  
                <th>Available To</th>
                <th>Provision Roles</th>
                <th>Segments</th>
            </tr>

            <c:forEach items="${actionBean.productSpecificationList.productSpecifications}" var="prodSpec" varStatus="loop">
                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                    <td>
                        <stripes:link href="/ProductCatalog.action" event="retrieveProductSpecification">                            
                            <stripes:param name="productSpecification.productSpecificationId" value="${prodSpec.productSpecificationId}"/>
                            ${prodSpec.name}
                        </stripes:link>
                    </td>
                    <td>${prodSpec.description}</td>
                    <td>${s:formatDateLong(prodSpec.availableFrom)}</td>        
                    <td>${s:formatDateLong(prodSpec.availableTo)}</td>    
                    <td>${prodSpec.provisionRoles}</td>
                    <td>${prodSpec.segments}</td>
                </tr>
            </c:forEach>

        </table>     

        <br/>

        <table class="green">                                
            <tr>
                <th>Product Provisioning Promotion Codes</th>
            </tr>

            <c:forEach items="${s:getPropertyAsList('env.promotion.codes.products')}" var="promoCode" varStatus="loop">
                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                    <td>${promoCode}</td>
                </tr>
            </c:forEach>

        </table>
        
        <br/>
        
        <table class="green">                                
            <tr>
                <th>Sales Promotion Codes</th>
            </tr>

            <c:forEach items="${s:getPropertyAsList('env.promotion.codes.sales')}" var="promoCode" varStatus="loop">
                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                    <td>${promoCode}</td>
                </tr>
            </c:forEach>

        </table>

    </stripes:layout-component>
</stripes:layout-render>

