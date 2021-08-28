<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.new.customer.summary"/>
</c:set>

<c:set var="customerClassType" value="${s:getDelimitedPropertyValueMapping('env.customer.classifications', actionBean.customer.classification)}"/>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <div id="entity">
            <stripes:form action="/Customer.action" focus="" autocomplete="off">
                <stripes:hidden name="customer.warehouseId" value="${actionBean.customer.warehouseId}" />
                <stripes:hidden name="customer.SSOIdentity" value="${actionBean.customer.SSOIdentity}"/>
                <stripes:hidden name="customer.SSODigest" value="${actionBean.customer.SSODigest}"/>
                <stripes:hidden name="customer.identityNumberType" value="${actionBean.customer.identityNumberType}"/>
                <stripes:hidden name="customer.identityNumber" value="${actionBean.customer.identityNumber}"/>
                <stripes:hidden name="customer.cardNumber" value="${actionBean.customer.cardNumber}"/>
                <stripes:hidden name="customer.title" value="${actionBean.customer.title}"/>
                <stripes:hidden name="customer.firstName" value="${actionBean.customer.firstName}"/>
                <stripes:hidden name="customer.middleName" value="${actionBean.customer.middleName}"/>
                <stripes:hidden name="customer.lastName" value="${actionBean.customer.lastName}"/>
                <stripes:hidden name="customer.mothersMaidenName" value="${customer.mothersMaidenName}"/>
                <stripes:hidden name="customer.dateOfBirth" value="${actionBean.customer.dateOfBirth}"/>
                <stripes:hidden name="customer.gender" value="${actionBean.customer.gender}"/>
                <stripes:hidden name="customer.language" value="${actionBean.customer.language}"/>
                <stripes:hidden name="customer.emailAddress" value="${actionBean.customer.emailAddress}"/>
                <stripes:hidden name="customer.alternativeContact1" value="${actionBean.customer.alternativeContact1}"/>
                <stripes:hidden name="customer.alternativeContact2" value="${actionBean.customer.alternativeContact2}"/>
                <stripes:hidden name="customer.referralCode" value="${actionBean.customer.referralCode}"/>
                <stripes:hidden name="customer.optInLevel" value="${actionBean.customer.optInLevel}"/>
                <stripes:hidden name="customer.customerStatus" value="${actionBean.customer.customerStatus}"/>
                <stripes:hidden name="customer.classification" value="${actionBean.customer.classification}"/>
                <stripes:hidden name="customer.nationality"  value="${actionBean.customer.nationality}"/>
                <stripes:hidden name="customer.securityGroups[0]" value="${actionBean.customer.securityGroups[0]}"/>
                <stripes:hidden name="customer.passportExpiryDate" value="${actionBean.customer.passportExpiryDate}"/>
                <stripes:hidden name="customer.visaExpiryDate" value="${actionBean.customer.visaExpiryDate}"/>
                <stripes:hidden name="TTIssue.ID" value="${actionBean.TTIssue.ID}"/>
                <stripes:hidden name="customer.KYCStatus" value="${actionBean.customer.KYCStatus}"/>
                
                <c:choose>
                    <c:when test='${customerClassType == "personal" || customerClassType == "minor"}'>    
                        <table class="clear">
                            <tr>
                                <td><b><stripes:label for="classification"/>:</b></td>
                                <td><fmt:message  key="classification.${actionBean.customer.classification}" /></td>
                            </tr>
                            <tr>
                                <td><b><fmt:message key="basic.information"/>:</b><br /></td>
                            </tr>
                            <c:choose>
                                <c:when test="${customerClassType == 'minor'}">
                                    <!--Do nothing -->
                                </c:when>
                                <c:otherwise>
                                    <c:if test="${s:getPropertyWithDefault('env.customer.verify.with.nida', false)}">
                                            <tr class="red" style="white-space:nowrap;">
                                                <td>KYC Status:</td>
                                                <td>
                                                    <fmt:message   key="kycstatus.${actionBean.customer.KYCStatus}" />
                                                </td>
                                            </tr>
                                    </c:if>
                                    </tr>
                                    <tr>
                                        <td>
                                            <stripes:label for="id.number.type"/>:
                                        </td>
                                        <td>
                                            <fmt:message  key="document.type.${actionBean.customer.identityNumberType}"/>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td><stripes:label for="id.number"/>:</td>
                                        <td>${actionBean.customer.identityNumber}</td>
                                    </tr>
                                    <c:choose>
                                        <c:when test="${s:getProperty('env.customer.verify.with.nira')}">
                                            <tr>
                                                <td><stripes:label for="id.card.number"/>:</td>
                                                <td>${actionBean.customer.cardNumber}</td>
                                            </tr>
                                        </c:when>
                                        <c:otherwise/>
                                    </c:choose>
                                    <tr>
                                        <td><stripes:label for="nationality"/>:</td>
                                        <td>${actionBean.customer.nationality}</td>
                                    </tr>
                                       
                                    <c:choose>
                                        <c:when test="${actionBean.customer.identityNumberType == 'passport'}">
                                            <tr>
                                                <td><stripes:label for="passport.expiry.date"/>:</td>
                                                <td>${actionBean.customer.passportExpiryDate}</td>
                                            </tr>
                                            <tr>
                                                <td><stripes:label for="visa.expiry.date"/>:</td>
                                                <td>${actionBean.customer.visaExpiryDate}</td>
                                            </tr>
                                        </c:when>
                                        <c:otherwise/>
                                    </c:choose>
                                </c:otherwise>
                            </c:choose>
                            <tr>
                                <td><stripes:label for="first.name"/>:</td>
                                <td>${actionBean.customer.firstName}</td>
                            </tr>
                            <tr>
                                <td><stripes:label for="middle.name"/>:</td>
                                <td>${actionBean.customer.middleName}</td>
                            </tr>
                            <tr>
                                <td><stripes:label for="last.name"/>:</td>
                                <td>${actionBean.customer.lastName}</td>
                            </tr>
                            <tr>
                                <td><stripes:label for="mothers.maidenname"/>:</td>
                                <td>${actionBean.customer.mothersMaidenName}</td>
                            </tr>
                            <tr>
                                <td><fmt:message key="ssoidentity"/>:</td>
                                <td>${actionBean.customer.SSOIdentity}</td>
                            </tr>
                            <tr>
                                <td><stripes:label for="date.of.birth"/>:</td>
                                <td>
                                    ${actionBean.customer.dateOfBirth}
                                </td>
                            </tr>
                            <tr>
                                <td><stripes:label for="gender"/>:</td>
                                <td>
                                    ${actionBean.customer.gender}
                                </td>
                            </tr>
                            <tr>
                                <td><stripes:label for="language"/>:</td>
                                <td>
                                    ${actionBean.customer.language}               
                                </td>
                            </tr>                
                            <tr>
                                <td><stripes:label for="email"/>:</td>
                                <td>${actionBean.customer.emailAddress}</td>
                            </tr>                
                            <tr>
                                <td><stripes:label for="alternative.contact.number.1"/>:</td>
                                <td>${actionBean.customer.alternativeContact1}</td>
                            </tr>
                            <tr>
                                <td><stripes:label for="alternative.contact.number.2"/>:</td>
                                <td>${actionBean.customer.alternativeContact2}</td>
                            </tr>
                            <tr>
                                <td>Referral Code:</td>
                                <td>${actionBean.customer.referralCode}</td>
                            </tr>
                            <tr>
                                <td><stripes:label for="status"/>:</td>
                                <td>${actionBean.customer.customerStatus}</td>
                            </tr>
                        </table>
                    </c:when>

                </c:choose>
                <table class="clear">
                    <tr>
                        <td>
                            <br />
                            <b><fmt:message key="customer.addresses"/>:</b>
                            <br />
                        </td>
                    </tr>
                    <c:forEach items="${actionBean.customer.addresses}" varStatus="loop">                    
                        <tr>
                            <td style='vertical-align: top; margin-top:auto'>
                                ${actionBean.customer.addresses[loop.index].type}
                                <stripes:hidden name="customer.addresses[${loop.index}].type"    value="${actionBean.customer.addresses[loop.index].type}" />
                            </td>
                            <td style='vertical-align: top; margin-top:auto'>
                                <table class="clear" style='vertical-align: top; margin-top:auto'>
                                    <tr>
                                        <td><fmt:message key="address.line1"/>:</td>
                                        <td>
                                            ${actionBean.customer.addresses[loop.index].line1}
                                            <stripes:hidden name="customer.addresses[${loop.index}].line1"   value="${actionBean.customer.addresses[loop.index].line1}" />
                                        </td>
                                    </tr>                        
                                    <tr>
                                        <td><fmt:message key="address.line2"/>:</td>
                                        <td>
                                            ${actionBean.customer.addresses[loop.index].line2}
                                            <stripes:hidden name="customer.addresses[${loop.index}].line2"   value="${actionBean.customer.addresses[loop.index].line2}" />
                                        </td>
                                    </tr>

                                    <tr>
                                        <td><fmt:message key="town"/>:</td>
                                        <td>
                                            ${actionBean.customer.addresses[loop.index].town}
                                            <stripes:hidden name="customer.addresses[${loop.index}].town"    value="${actionBean.customer.addresses[loop.index].town}" />
                                        </td>
                                    </tr>

                                    <tr>
                                        <td><fmt:message key="zone"/>:</td>
                                        <td>
                                            ${actionBean.customer.addresses[loop.index].zone}
                                            <stripes:hidden name="customer.addresses[${loop.index}].zone" value="${actionBean.customer.addresses[loop.index].zone}" />
                                        </td>
                                    </tr>


                                    <tr>
                                        <td><fmt:message key="state"/>:</td>
                                        <td>
                                            ${actionBean.customer.addresses[loop.index].state}
                                            <stripes:hidden name="customer.addresses[${loop.index}].state"    value="${actionBean.customer.addresses[loop.index].state}" />
                                        </td>
                                    </tr>


                                    <tr>
                                        <td><fmt:message key="country"/>:</td>
                                        <td>
                                            ${actionBean.customer.addresses[loop.index].country}
                                            <stripes:hidden name="customer.addresses[${loop.index}].country" value="${actionBean.customer.addresses[loop.index].country}" />
                                        </td>
                                    </tr>

                                    <c:choose>
                                        <c:when test="${s:getProperty('env.postalcode.display')}">
                                            <tr>
                                                <td><fmt:message key="code"/>:</td>
                                                <td>
                                                    ${actionBean.customer.addresses[loop.index].code}
                                                    <stripes:hidden name="customer.addresses[${loop.index}].code"    value="${actionBean.customer.addresses[loop.index].code}" />
                                                </td>
                                            </tr>
                                        </c:when>
                                        <c:otherwise>
                                            <stripes:hidden name="customer.addresses[${loop.index}].code"    value="" />
                                        </c:otherwise>
                                    </c:choose>
                                </table>
                            </td>
                        </tr>
                    </c:forEach>
                    <tr>
                        <td><fmt:message key="user.group"/>:</td>
                        <td>
                            <stripes:select name="userGroup" size="3" style="width: 200px" disabled="disabled">
                                <c:forEach items="${actionBean.customer.securityGroups}" var="usergroup" varStatus="loop">
                                    <stripes:option value="${usergroup}">${usergroup}</stripes:option>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <br />
                            <b><fmt:message key="scanned.documents"/>:</b>
                            <br />
                        </td>
                    </tr>
                    <c:forEach items="${actionBean.customer.customerPhotographs}" varStatus="loop"> 
                        <tr id="row${loop.index}">
                            <td align="left" valign="top">
                                <fmt:message  key="document.type.${actionBean.customer.customerPhotographs[loop.index].photoType}"/>
                                <stripes:hidden   name="customer.customerPhotographs[${loop.index}].photoType" value="${actionBean.customer.customerPhotographs[loop.index].photoType}" />
                                <stripes:hidden name="customer.customerPhotographs[${loop.index}].photoGuid" value="${actionBean.customer.customerPhotographs[loop.index].photoGuid}"/>
                                <stripes:hidden name="customer.customerPhotographs[${loop.index}].data" value="${actionBean.customer.customerPhotographs[loop.index].data}"/>
                            </td>
                            <td align="left" valign="top">
                                <c:choose>

                                    <c:when test='${actionBean.customer.customerPhotographs[loop.index].photoType.matches("^.*fingerprint.*$")}'>
                                        <a href="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg" target="_blank">
                                            <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg"/>
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        <a href="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.customer.customerPhotographs[loop.index].photoGuid}" target="_blank">
                                            <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.customer.customerPhotographs[loop.index].photoGuid}"/>
                                        </a>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                </table>     
                <span class="button">
                    <stripes:submit name="showSetCustomerUsernamePasswordBack"/>
                </span>
                <c:choose> 
                    <c:when test='${actionBean.customer.identityNumberType == "refugeeid"}'>
                        <span class="button">
                            <stripes:submit name="verifyCustomerWithUCC"/>
                        </span>
                    </c:when>
                    <c:otherwise>
                        <span class="button">
                            <stripes:submit name="addCustomerWizard"/>
                        </span>
                    </c:otherwise>
                </c:choose>                 
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>