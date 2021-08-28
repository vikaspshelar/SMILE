<%-- 
    Document   : view_consent_form
    Created on : 26 Feb, 2021, 7:14:06 PM
    Author     : user
--%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="nin.consent.form"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/Customer.action">
            <table class="green" width="99%">
                <tr>
                    <th align="center"><fmt:message key="id"/></th>
                <th align="center">NIN</th>
                <th align="center"><fmt:message key="scanned.documents"/></th>
                <th align="center">Approve</th>
                <th align="center">Decline</th>
                </tr>
                <c:forEach items="${actionBean.customerList.customers}" var="customer" varStatus="loop">
                    <tr>
                        <td align="center">
                    <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                        <stripes:param name="customerQuery.customerId" value="${customer.customerId}"/>
                        ${customer.customerId}
                    </stripes:link>
                    </td>
                    <td align="center">
                        ${customer.nationalIdentityNumber}
                    </td>
                    <td align="center">
                    <c:if test="${s:getListSize(customer.customerPhotographs) > 0}">
                        <table class="clear">
                            <c:forEach items="${customer.customerPhotographs}" varStatus="loop"> 
                                <tr align="center" id="row${loop.index}">
                                    <td align="left" valign="top">
                                <c:choose>
                                    <c:when test='${customer.customerPhotographs[loop.index].photoType.matches("^.*publickey.*$")}'>
                                        <a href="${pageContext.request.contextPath}/images/public_key.png" target="_blank">
                                            <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/images/public_key.png"/>
                                        </a>
                                    </c:when>
                                    <c:when test='${customer.customerPhotographs[loop.index].photoType.matches("^.*fingerprint.*$")}'>
                                        <a href="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg" target="_blank">
                                            <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg"/>
                                        </a>
                                    </c:when>
                                    <c:when test='${customer.customerPhotographs[loop.index].photoGuid.matches("^.*.pdf$")}'>
                                        <a href="/sep/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${customer.customerPhotographs[loop.index].photoGuid}" target="_blank">
                                            <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/images/pdf-icon.jpg"/>
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        <a href="/sep/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${customer.customerPhotographs[loop.index].photoGuid}" target="_blank">
                                            <img id="imgfile${loop.index}" class="thumb" src="/sep/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${customer.customerPhotographs[loop.index].photoGuid}"/>
                                        </a>
                                    </c:otherwise>
                                </c:choose>
                                </td>
                                </tr>
                            </c:forEach>
                        </table>
                    </c:if>
                    </td>
                    <td align="center">
                    <stripes:form action="/Customer.action" class="buttonOnly">
                        <input type="hidden" name="customer.customerId" value="${customer.customerId}"/>
                        <input type="hidden" name="customer.nationalIdentityNumber" value="${customer.nationalIdentityNumber}"/>
                        <stripes:submit name="approveConsentForm"/>
                    </stripes:form>                                        
                    </td>
                    <td align="center">
                    <stripes:form action="/Customer.action" class="buttonOnly">
                        <input type="hidden" name="customer.customerId" value="${customer.customerId}"/>
                        <input type="hidden" name="customer.nationalIdentityNumber" value="${customer.nationalIdentityNumber}"/>
                        <stripes:submit name="declineConsentForm"/>
                    </stripes:form>                                        
                    </td>
                    </tr>                    
                </c:forEach>

            </table>
        </stripes:form>

    </stripes:layout-component>    
</stripes:layout-render>