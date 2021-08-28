<%-- 
    Document   : verify_customer_nin
    Created on : 21 Jan 2021, 5:04:00 PM
    Author     : bhaskarhg
--%>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="nin.verification"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/Customer.action">
            <table class="green" width="99%">
                <tr>
                    <th><fmt:message key="id"/></th>
                <th>Full Name</th>
                <th>National Identity Number</th>
                <th align="center">Verify</th>
                </tr>
                <c:forEach items="${actionBean.customerList.customers}" var="customer" varStatus="loop">
                    <tr>
                    <td>
                    <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                        <stripes:param name="customerQuery.customerId" value="${customer.customerId}"/>
                        ${customer.customerId}
                    </stripes:link>
                    </td>
                    <td>
                    <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                        <stripes:param name="customerQuery.customerId" value="${customer.customerId}"/>
                        ${customer.firstName} ${customer.lastName}
                    </stripes:link>
                    </td>
                    <td>
                        ${customer.nationalIdentityNumber}
                    </td>
                    <td align="center">
                    <stripes:checkbox name="NINVerified" value="${customer.customerId}"/>
                    </td>
                    </tr>                    
                </c:forEach>                
            </table>
            <stripes:submit name="verifyCustomerNins"/>
        </stripes:form>

    </stripes:layout-component>    
</stripes:layout-render>
