<%-- 
    Document   : add_customer_role
    Created on : 17 Jan 2013, 5:09:29 PM
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

            <stripes:form action="/Customer.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();"> 
                <stripes:hidden id="customerId" name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                <stripes:hidden name="organisationQuery.resultLimit" value="50"/>
                <stripes:hidden name="customer.KYCStatus" value="${actionBean.customer.KYCStatus}"/>
                <table class="clear">
                    <tr>
                        <td colspan="3"><fmt:message key="enter.organisation.name"/>:</td>                        
                    </tr>
                    <tr>
                        <td><fmt:message key="organisation.name"/>:</td>
                        <td><stripes:text name="organisationQuery.organisationName" size="30" onkeyup="validate(this,'^.{2,50}$','emptyok')"/></td>                        
                    </tr>                                 
                    <tr>
                        <td>
                            <span class="button">
                                <stripes:submit name="searchOrganisationCustomerRole"/>
                            </span>
                        </td>
                    </tr>
                </table>            
            </stripes:form>
            <br/>        
            <c:if test="${actionBean.organisationList.numberOfOrganisations > 0}">
                <table class="green">
                    <tr>
                        <th><fmt:message key="id"/></th>
                        <th><fmt:message key="organisation.name"/></th>
                        <th><fmt:message key="view"/></th>
                    </tr>
                    <c:forEach items="${actionBean.organisationList.organisations}" var="organisation" varStatus="loop">
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            <td>${organisation.organisationId}</td>
                            <td>${organisation.organisationName}</td>                           
                            <td>
                                <stripes:form action="/Customer.action">                                    
                                    <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                    <input type="hidden" name="organisationQuery.organisationId" value="${organisation.organisationId}"/>
                                    <stripes:submit name="setOrganisationAndShowRoleField"/>
                                </stripes:form>
                            </td>
                        </tr>                    
                    </c:forEach>                
                </table>
            </c:if> 
        </div>
    </stripes:layout-component>
</stripes:layout-render>