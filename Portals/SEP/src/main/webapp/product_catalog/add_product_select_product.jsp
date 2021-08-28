<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.product"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="add.product.to"><fmt:param value="${actionBean.customer.firstName} ${actionBean.customer.lastName}"/></fmt:message>
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/Customer.action">               
                            <input type="hidden" name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveCustomer"><fmt:message key="manage.customer"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>         

            <br/>        
            <table class="green" width="99%">                                
                <tr>
                    <th>Name</th>
                    <th>Description</th>
                    <th>Select</th>
                </tr>
                <c:forEach items="${actionBean.productSpecificationList.productSpecifications}" var="catalog" varStatus="loop">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>${catalog.name}</td>
                        <td>${catalog.description}</td>
                        <td>
                            <stripes:form action="/ProductCatalog.action">
                                <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                                <stripes:hidden name="productOrder.customerId" value="${actionBean.customer.customerId}"/>
                                <stripes:hidden name="iccid" value="${actionBean.iccid}"/>
                                <input type="hidden" name="productOrder.productSpecificationId" value="${catalog.productSpecificationId}"/>
                                <stripes:submit name="collectGeneralDataForProductInstall"/>
                            </stripes:form>
                        </td>
                    </tr>
                </c:forEach>
            </table>            
        </div>
    </stripes:layout-component>
</stripes:layout-render>

