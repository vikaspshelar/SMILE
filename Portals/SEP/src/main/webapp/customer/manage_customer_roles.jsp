<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="manage.customer.roles"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="html-head">
        <script type="text/javascript">
            $j(document.getElementById('updateButton')).attr("disabled", "disabled");
        </script>

    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="customer"/> ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/Customer.action">                                
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveCustomer"><fmt:message key="manage.customer"/></stripes:option>
                            </stripes:select>
                            <stripes:hidden id="customerId" name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                            <stripes:hidden name="customer.firstName" value="${actionBean.customer.firstName}"/> 
                            <stripes:hidden name="customer.lastName" value="${actionBean.customer.lastName}"/>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>            
            <stripes:form id="frm" action="/Customer.action" onsubmit="return true;">
                <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                <table class="green" id="roleTable">
                    <tbody>
                        <c:choose>                        
                            <c:when test="${s:getListSize(actionBean.customer.customerRoles) > 0}">
                                <tr>
                                    <th><fmt:message key="organisation.name"/></th>
                                    <th><fmt:message key="role.name"/></th>
                                    <th><fmt:message key="delete.role"/></th>
                                </tr>
                                <c:forEach items="${actionBean.customer.customerRoles}" varStatus="loop">                      
                                    <tr id="row${loop.index}">
                                        <td align="left" valign="top">
                                            <stripes:text name="customer.customerRoles[${loop.index}].organisationName" readonly="true"/>
                                            <stripes:hidden name="customer.customerRoles[${loop.index}].organisationId"/>
                                            <stripes:hidden name="customer.customerRoles[${loop.index}].customerId" value="${actionBean.customer.customerId}"/>
                                        </td>
                                        <td align="left" valign="top">
                                            <stripes:text name="customer.customerRoles[${loop.index}].roleName" readonly="true"/>                                    
                                        </td>
                                        <td align="left" valign="top">
                                            <stripes:button name="deleteCustomerRole" value="Delete Role" onclick="removeRow('row${loop.index}');"/>                                        
                                        </td>
                                    </tr>                            
                                </c:forEach>
                                <tr  style="display:none;">
                                    <td><stripes:submit id="deleteCustomerRole" name="deleteCustomerRole" style="display:none;"/></td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td colspan="3">
                                        <fmt:message key="no.customer.roles"/>
                                    </td>
                                </tr>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>               
            </stripes:form>
            <stripes:form action="/Customer.action">
                <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                <table class="clear">
                    <tr>
                        <td>
                            <br/>
                            <br/>
                            <br/>
                            <stripes:submit name="showAddCustomerRole"/>
                        </td>
                    </tr>
                </table>
            </stripes:form>            
        </div>
    </stripes:layout-component>
</stripes:layout-render>