<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="change.customer"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>
                        <fmt:message key="product.name"/>:
                        ${actionBean.productSpecification.name}
                    </td>
                </tr>
            </table>

            <stripes:form action="/ProductCatalog.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">    
                <table class="clear">
                    <tr>
                        <td><fmt:message key="customer.customerId"/>:</td>
                        <td>
                            <stripes:text name="productOrder.customerId" class="required" onkeyup="validate(this,'^[0-9]{1,10}$','')"/>
                            <stripes:hidden name="productOrder.productInstanceId" value="${actionBean.productInstance.productInstanceId}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="organisation.id"/>:</td>
                        <td>
                            <table class="clear">
                                <tr>
                                    <td>No Organisation</td>
                                     <td><stripes:radio  name="productOrder.organisationId" value="0"/></td>
                                </tr>
                                <c:forEach items="${actionBean.customer.customerRoles}" var="role" varStatus="loop">
                                <tr>
                                    <td>${role.roleName} at ${role.organisationName}</td>
                                     <td><stripes:radio  name="productOrder.organisationId" value="${role.organisationId}"/></td>
                                </tr>
                            </c:forEach>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="segment"/>:</td>
                        <td>
                            <table class="clear">
                                <c:forEach items="${actionBean.segments}" var="segment"> 
                                    <tr>
                                        <td>${segment}</td>
                                        <td><stripes:radio  name="productOrder.segment" value="${segment}"/></td>
                                    </tr>
                                </c:forEach>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="apply.to.all.service.instances"/>:</td>
                        <td><stripes:checkbox name="applyToAllServiceInstances" checked="false"/></td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="changeProductInstanceCustomer"/>
                            </span>                        
                        </td>
                    </tr>  
                </table>            
            </stripes:form>
        </div>
    </stripes:layout-component>

</stripes:layout-render>

