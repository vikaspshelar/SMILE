<%-- 
    Document   : add_customer_role_name
    Created on : 18 Jan 2013, 3:27:59 PM
    Author     : lesiba
--%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="manage.customer.roles"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
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

            <stripes:form action="/Customer.action" name="frm"  method="POST" onsubmit="return alertValidationErrors();">
                <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                                
                <stripes:hidden name="customerRole.organisationId" value="${actionBean.organisation.organisationId}"/>
                <stripes:hidden name="customerRole.customerId" value="${actionBean.customer.customerId}"/>
                
                
                <table class="clear">
                    <tr>
                        <td><fmt:message key="organisation.name"/> : </td>
                        <td>${actionBean.organisation.organisationName}</td>
                    </tr>

                    <tr>
                        <td>
                            <fmt:message key="role.name"/>:
                        </td>
                        <td>
                            <stripes:select name="customerRole.roleName" onchange="validate(this,'^.{3,50}$','emptynotok')">
                                <c:forEach items="${s:getPropertyAsList('env.organisation.roles')}" var="role" varStatus="loop">                                   
                                    <stripes:option value="${role}">
                                        ${role}
                                    </stripes:option>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <stripes:submit name="updateCustomerRoles"/>
                        </td>
                    </tr>
                </table>
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>