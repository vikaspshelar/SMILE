<%-- 
    Document   : change_account_manager
    Created on : 12 Oct 2012, 3:46:21 PM
    Author     : lesiba
--%>

<%@include  file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="manage.customer"/>
</c:set>


<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>
                        <fmt:message key="customer"/> : ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                    </td>
                    <td align="right">
                        <stripes:form name="frm" action="/Customer.action">
                            <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>

                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveCustomer"><fmt:message key="manage.customer"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction" />
                        </stripes:form>
                    </td>
                </tr>
            </table>
            <stripes:form action="/Customer.action">
                <stripes:hidden name="customer.version" value="${actionBean.customer.version}"/>
                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/> 
                <table class="clear">                                
                    <tr>
                        <td>
                            <fmt:message key="account.manager.customer"/>: 
                        </td>
                        <td>
                            <stripes:select name="customer.accountManagerCustomerProfileId">
                                <c:forEach items="${actionBean.accountManagerCustomerList}" var="AccountManager" varStatus="loop">
                                    <stripes:option value="${AccountManager.key}">${AccountManager.value}</stripes:option>
                                </c:forEach>                                
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <span class="button">
                                <stripes:submit name="changeAccountManagerCustomer" value="Change"/>
                            </span>
                        </td>
                    </tr>
                </table>
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>