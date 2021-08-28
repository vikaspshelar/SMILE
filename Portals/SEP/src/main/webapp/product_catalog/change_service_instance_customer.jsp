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
                        <fmt:message key="service.name"/>:
                        ${actionBean.serviceSpecification.name}
                    </td>
                </tr>
        </table>
                    
        <stripes:form action="/ProductCatalog.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">    
            <table class="clear">
                <tr>
                    <td><fmt:message key="customer.customerId"/>:</td>
                    <td>
                        <stripes:text name="productOrder.serviceInstanceOrders[0].serviceInstance.customerId" class="required" onkeyup="validate(this,'^[0-9]{1,10}$','')"/>
                        <stripes:hidden name="productOrder.serviceInstanceOrders[0].serviceInstance.serviceInstanceId" value="${actionBean.serviceInstance.serviceInstanceId}"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                        <span class="button">
                            <stripes:submit name="changeServiceInstanceCustomer"/>
                        </span>                        
                    </td>
                </tr>  
            </table>            
        </stripes:form>
        </div>
     </stripes:layout-component>
  
</stripes:layout-render>

