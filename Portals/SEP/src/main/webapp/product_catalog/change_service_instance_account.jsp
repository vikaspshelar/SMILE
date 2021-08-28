<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="change.account"/>
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
                        <td><fmt:message key="account.accountId"/>:</td>
                        <td>
                            <input type="text" name="productAccount" class="required" size="20" list="items">
                            <datalist id="items">
                                <c:forEach items="${actionBean.accountIdList}" var="accountId">
                                    <option value="${accountId}"/>
                                </c:forEach>     
                            </datalist>
                            <stripes:hidden name="productOrder.serviceInstanceOrders[0].serviceInstance.serviceInstanceId" value="${actionBean.serviceInstance.serviceInstanceId}"/>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="changeServiceInstanceAccount"/>
                            </span>                        
                        </td>
                    </tr>  
                </table>            
            </stripes:form>
        </div>
    </stripes:layout-component>

</stripes:layout-render>

