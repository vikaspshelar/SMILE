<%-- 
    Document   : kyc_requirement
    Created on : 13 Oct 2020, 11:38:03 AM
    Author     : bhaskarhg
--%>

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
                <th colspan="2">Info</th>
                <th align="center">Verified</th>
                </tr>
                <c:forEach items="${actionBean.customerList.customers}" var="customer" varStatus="loop">
                    <tr>
                    <stripes:hidden name="customerList.customers[${loop.index}].mandatoryKYCFields.customerId"  value="${customer.customerId}"/>
                    <td rowspan="11">
                    <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                        <stripes:param name="customerQuery.customerId" value="${customer.customerId}"/>
                        ${customer.customerId}
                    </stripes:link>
                    </td>
                    <td>
                        Title:
                    </td>
                    <td>
                        ${customer.title}
                    </td>
                    <td align="center"> 
                    <input type="checkbox" name="customerList.customers[${loop.index}].mandatoryKYCFields.titleVerified" value="Y" ${"Y" == customer.mandatoryKYCFields.titleVerified ? "checked='checked'" : ""}/>
                    </td>
                    </tr> 

                    <tr>
                        <td>
                            Full Name:
                        </td>
                        <td>
                    <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                        <stripes:param name="customerQuery.customerId" value="${customer.customerId}"/>
                        ${customer.firstName} ${customer.lastName}
                    </stripes:link>
                    </td>
                    <td align="center">
                        <input type="checkbox" name="customerList.customers[${loop.index}].mandatoryKYCFields.nameVerified" value="Y" ${"Y" == customer.mandatoryKYCFields.nameVerified ? "checked='checked'" : ""}/>
                    </td>
                    </tr> 

                    <tr>
                        <td>
                            Mobile Number:
                        </td>
                        <td>
                            ${customer.alternativeContact1}
                        </td>  
                        <td align="center">
                            <input type="checkbox" name="customerList.customers[${loop.index}].mandatoryKYCFields.mobileVerified" value="Y" ${"Y" == customer.mandatoryKYCFields.mobileVerified ? "checked='checked'" : ""}/>
                        </td>
                    </tr>

                    <tr>
                        <td>
                            Email Address:
                        </td>
                        <td>
                            ${customer.emailAddress}
                        </td>  
                        <td align="center">                            
                            <input type="checkbox" name="customerList.customers[${loop.index}].mandatoryKYCFields.emailVerified" value="Y" ${"Y" == customer.mandatoryKYCFields.emailVerified ? "checked='checked'" : ""}/>
                        </td>
                    </tr>

                    <tr>
                        <td>
                            Gender:
                        </td>
                        <td>
                            ${customer.gender}
                        </td>  
                        <td align="center">                           
                            <input type="checkbox" name="customerList.customers[${loop.index}].mandatoryKYCFields.genderVerified" value="Y" ${"Y" == customer.mandatoryKYCFields.genderVerified ? "checked='checked'" : ""}/>
                        </td>
                    </tr>

                    <tr>
                        <td>
                            Date of birth:
                        </td>
                        <td>
                            ${customer.dateOfBirth}
                        </td>  
                        <td align="center">                            
                            <input type="checkbox" name="customerList.customers[${loop.index}].mandatoryKYCFields.dobVerified" value="Y" ${"Y" == customer.mandatoryKYCFields.dobVerified ? "checked='checked'" : ""}/>
                        </td>
                    </tr>

                    <tr>
                        <td>
                            Nationality:
                        </td>
                        <td>
                            ${customer.nationality}
                        </td>  
                        <td align="center">                          
                            <input type="checkbox" name="customerList.customers[${loop.index}].mandatoryKYCFields.nationalityVerified" value="Y" ${"Y" == customer.mandatoryKYCFields.nationalityVerified ? "checked='checked'" : ""}/>
                        </td>
                    </tr>

                    <tr>
                        <td>
                            Physical Address:
                        </td>
                        <td>
                    <c:forEach items="${customer.addresses}" var="addresse">
                        <c:choose>
                            <c:when test='${addresse.type == "Physical Address"}'>
                                ${addresse.line1}, ${addresse.zone}, ${addresse.town}, ${addresse.state}, ${addresse.country}, ${addresse.code}.
                            </c:when>
                        </c:choose>
                    </c:forEach>
                    </td>  
                    <td align="center">               
                        <input type="checkbox" name="customerList.customers[${loop.index}].mandatoryKYCFields.physicalAddressVerified" value="Y" ${"Y" == customer.mandatoryKYCFields.physicalAddressVerified ? "checked='checked'" : ""}/>
                    </td>
                    </tr>

                    <tr>
                        <td>
                            Facial Picture:
                        </td>
                        <td>
                    <c:forEach items="${customer.customerPhotographs}" var="photo">
                        <c:choose>
                            <c:when test='${photo.photoType == "photo"}'>
                                <a href="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${photo.photoGuid}" target="_blank">
                                    <img class="bigthumb" src="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${photo.photoGuid}"/>
                                </a>
                            </c:when>
                        </c:choose>
                    </c:forEach>
                    </td>  
                    <td align="center">                    
                        <input type="checkbox" name="customerList.customers[${loop.index}].mandatoryKYCFields.facialPitureVerified" value="Y" ${"Y" == customer.mandatoryKYCFields.facialPitureVerified ? "checked='checked'" : ""}/>
                    </td>
                    </tr>

                    <tr>
                        <td>
                            Valid ID card:
                        </td>
                        <td>
                            ${customer.identityNumber}
                        </td>  
                        <td align="center">                          
                            <input type="checkbox" name="customerList.customers[${loop.index}].mandatoryKYCFields.validIdCardVerified" value="Y" ${"Y" == customer.mandatoryKYCFields.validIdCardVerified ? "checked='checked'" : ""}/>
                        </td>
                    </tr>

                    <tr>
                        <td>
                            Fingerprint:
                        </td>
                        <td>
                            ${customer.firstName}
                        </td>  
                        <td align="center">                          
                            <input type="checkbox" name="customerList.customers[${loop.index}].mandatoryKYCFields.fingerPrintVerified" value="Y" ${"Y" == customer.mandatoryKYCFields.fingerPrintVerified ? "checked='checked'" : ""}/>
                        </td>
                    </tr>
                </c:forEach>                
            </table>
            <stripes:submit name="updateMandateKYCSatus"/>
        </stripes:form>

    </stripes:layout-component>    
</stripes:layout-render>