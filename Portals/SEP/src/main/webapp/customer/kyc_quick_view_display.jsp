<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="kyc.quick.view"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/Customer.action">
            <table class="green" width="99%">
                <tr>
                    <th><fmt:message key="id"/></th>
                    <th>Info</th>
                    <th align="center">Photos</th>
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
                            <%--<br/>--%>
                            <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                <stripes:param name="customerQuery.customerId" value="${customer.customerId}"/>
                                ${customer.title} ${customer.firstName} ${customer.lastName}
                            </stripes:link>
                            <br/><br/>
                            ID Type:
                             <%--<br/>--%>
                            ${customer.identityNumberType}
                            <br/><br/>
                            Primary mobile number:
                             <%--<br/>--%>
                            <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                <stripes:param name="customerQuery.customerId" value="${customer.customerId}"/>
                                ${customer.alternativeContact1}
                            </stripes:link>
                            <br/><br/>
                            Email Address:
                             <%--<br/>--%>
                            <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                <stripes:param name="customerQuery.customerId" value="${customer.customerId}"/>
                                ${customer.emailAddress}
                            </stripes:link>
                            <br/><br/>
                            Gender:
                             <%--<br/>--%>
                            <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                <stripes:param name="customerQuery.customerId" value="${customer.customerId}"/>
                                ${customer.gender}  
                            </stripes:link>
                            <br/><br/>
                            KYC Status:
                             <%--<br/>--%>
                            <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                <stripes:param name="customerQuery.customerId" value="${customer.customerId}"/>
                                ${customer.KYCStatus}
                            </stripes:link>
                            <br/><br/>
                            <%--Physical Address: 
                            <br/> --%>
                            <%--<stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                <stripes:param name="customerQuery.customerId" value="${customer.customerId}"/>
                                ${customer.addresses}
                            </stripes:link>
                            <br/>
                      </td>--%>
                                <c:forEach items="${customer.addresses}" varStatus="loop">                    
                                    <%--<tr>
                                        <td style='vertical-align: top; margin-top:auto'> --%>
                                         ${customer.addresses[loop.index].type}
                                        <%--</td> 
                                        <td style='vertical-align: top; margin-top:auto'> --%>
                                            <table width="100%" class="clear">
                                                <tr>
                                                    <td colspan="5"><fmt:message key="address.line1"/>:</td>
                                                    <td colspan="5">
                                                        ${customer.addresses[loop.index].line1}
                                                    </td>
                                                    </tr>                        
                                                <tr>
                                                    <td colspan="5"><fmt:message key="address.line2"/>:</td>
                                                    <td colspan="5">
                                                        ${customer.addresses[loop.index].line2}
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td colspan="5"><fmt:message key="town"/>:</td>
                                                    <td colspan="5">
                                                        ${customer.addresses[loop.index].town}
                                                    </td>
                                                </tr>

                                                <tr>
                                                    <td colspan="5"><fmt:message key="zone"/>:</td>
                                                    <td>
                                                        ${customer.addresses[loop.index].zone}
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td colspan="5"><fmt:message key="state"/>:</td>
                                                    <td colspan="5">
                                                        ${customer.addresses[loop.index].state}
                                                    </td>
                                                </tr>

                                                <tr colspan="5">
                                                    <td colspan="5"><fmt:message key="country"/>:</td>
                                                    <td colspan="5">
                                                        ${customer.addresses[loop.index].country}
                                                        </td>
                                                </tr>

                                            </table>
                                       <%-- </td>
                                    </tr>--%>
                                </c:forEach>
                            <br/><br/>
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
                    </tr>     
                </c:forEach>                
            </table>
        </stripes:form>

    </stripes:layout-component>    
</stripes:layout-render>

