<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="kyc.verification"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/Customer.action">
            <table class="green" width="99%">
                <tr>
                    <th><fmt:message key="id"/></th>
                    <th>Info</th>
                    <th align="center">Photos</th>
                    <th align="center">Verified</th>
                    <th align="center">Unverified</th>
                </tr>
                <c:forEach items="${actionBean.customerList.customers}" var="customer" varStatus="loop">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>
                            <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                <stripes:param name="customerQuery.customerId" value="${customer.customerId}"/>
                                ${customer.customerId}
                            </stripes:link>
                        </td>
                        <td>
                            Customer:
                            <br/>
                            <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                <stripes:param name="customerQuery.customerId" value="${customer.customerId}"/>
                                ${customer.firstName} ${customer.lastName}
                            </stripes:link>
                            <br/><br/>
                            ID Type:
                            <br/>
                            ${customer.identityNumberType}
                            <br/><br/>
                            Sales Person:
                            <br/>
                            <stripes:link href="/Customer.action" event="retrieveCustomer">
                                <stripes:param name="customer.customerId" value="${customer.createdByCustomerProfileId}"/>
                                ${s:getCustomerName(customer.createdByCustomerProfileId)}
                            </stripes:link>
                             <br/><br/>
                            Last Modified By:
                            <br/>
                            <stripes:link href="/Customer.action" event="retrieveCustomer">
                                <stripes:param name="customer.customerId" value="${actionBean.lastModifiedBy[customer.customerId]}"/>
                                ${s:getCustomerName(actionBean.lastModifiedBy[customer.customerId])}
                            </stripes:link>
                        </td>
                        <td align="center">
                            <table class="clear">
                                <c:forEach items="${customer.customerPhotographs}" var="photo">
                                    <tr style="border-width: 0px;">
                                        <td style="border-width: 0px;">
                                            ${photo.photoType}:<br/>
                                            <a href="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${photo.photoGuid}" target="_blank">
                                                <img class="bigthumb" src="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${photo.photoGuid}"/>
                                            </a>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </table>
                        </td>
                        <td align="center">
                            <stripes:checkbox name="KYCVerified" value="${customer.customerId}"/>
                        </td>
                        <td align="center">
                            <stripes:checkbox name="KYCUnverified" value="${customer.customerId}"/>
                            <br/>
                            <stripes:textarea cols="20" rows="8" name="unverifiedReason[${customer.customerId}]"/>
                        </td>
                    </tr>     
                </c:forEach>                
            </table>
            <stripes:submit name="updateKYCStatus"/>
        </stripes:form>

    </stripes:layout-component>    
</stripes:layout-render>

