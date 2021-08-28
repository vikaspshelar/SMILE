<%-- 
    Document   : manage_addresses
    Created on : 22 Jan 2013, 10:43:46 AM
    Author     : lesiba
--%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="manage.contracts"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <div id="entity">
            <table class="entity_header">
                <tr>
                    <c:choose>
                        <c:when test="${actionBean.customer != null}">
                            <td>
                                <fmt:message key="manage.contracts.customer"/> : ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                            </td>
                            <td align="right">
                                <stripes:form action="/Customer.action">                                
                                    <stripes:select name="entityAction">
                                        <stripes:option value="retrieveCustomer"><fmt:message key="manage.customer"/></stripes:option>
                                        <stripes:option value="showAddContract"><fmt:message key="add.customer.contract"/></stripes:option>
                                    </stripes:select>
                                    <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                    <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                                    <stripes:submit name="performEntityAction"/>
                                </stripes:form>
                            </td>
                        </c:when>
                        <c:otherwise>
                            <td>
                                <fmt:message key="manage.contracts.organisation"/> : ${actionBean.organisation.organisationName}
                            </td>
                            <td align="right">
                                <stripes:form action="/Customer.action">
                                    <stripes:select name="entityAction">
                                        <stripes:option value="retrieveOrganisation"><fmt:message key="manage.organisation"/></stripes:option>
                                        <stripes:option value="showAddContract"><fmt:message key="add.organisation.contract"/></stripes:option>
                                    </stripes:select>
                                    <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                                    <stripes:hidden name="organisation.organisationId" value="${actionBean.organisation.organisationId}"/>
                                    <stripes:submit name="performEntityAction"/>
                                </stripes:form>
                            </td>
                        </c:otherwise>
                    </c:choose>
                </tr>
            </table>
            <table  class="green" width="99%">
                <c:set var="contracts" value="${actionBean.contractList.contracts}"/>

                <tr>
                    <th><fmt:message key="contract.contractid"/></th>
                    <th><fmt:message key="contract.name"/></th>
                    <th><fmt:message key="view"/></th>
                </tr>

                <c:forEach items="${contracts}" varStatus="loop" var="contract">                    
                    <tr>
                        <td>${contract.contractId}</td>                    
                        <td>${contract.contractName}</td>                    
                        <td>
                            <stripes:form action="/Customer.action">
                                <input type="hidden" name="contract.contractId" value="${contract.contractId}"/>
                                <stripes:submit name="viewContract"/>
                            </stripes:form>
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </div>
    </stripes:layout-component>
</stripes:layout-render>