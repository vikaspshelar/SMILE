<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.new.organisation.summary"/>
</c:set>

<c:set var="customerClassType" value="${s:getDelimitedPropertyValueMapping('env.customer.classifications', actionBean.customer.classification)}"/>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <div id="entity">
            <stripes:form action="/Customer.action" focus="" autocomplete="off">
                <stripes:hidden name="organisation.organisationType" value="${actionBean.organisation.organisationType}"/>
                <stripes:hidden name="organisation.organisationSubType" value="${actionBean.organisation.organisationSubType}"/>
                <stripes:hidden name="organisation.organisationName"    value="${actionBean.organisation.version}"/>
                <stripes:hidden name="organisation.organisationStatus" value="AC"/>
                <stripes:hidden name="organisation.companyNumber" value="${actionBean.organisation.companyNumber}"/>
                <stripes:hidden name="organisation.taxNumber" value="${actionBean.organisation.taxNumber}"/>
                <stripes:hidden name="organisation.emailAddress" value="${actionBean.organisation.emailAddress}"/>
                <stripes:hidden name="organisation.alternativeContact1" value="${actionBean.organisation.alternativeContact1}"/>
                <stripes:hidden name="organisation.alternativeContact2" value="${actionBean.organisation.alternativeContact2}"/>
                <stripes:hidden name="organisation.industry" value="${actionBean.organisation.industry}"/>
                <stripes:hidden name="organisation.size" value="${actionBean.organisation.size}" />

                <table class="clear">
                    <tr>
                        <td><stripes:label for="organisation.type"/>:</td>
                        <td><fmt:message   key="organisation.type.${actionBean.organisation.organisationType}" /></td>
                    </tr>
                    <c:choose>
                        <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
                            <tr>
                                <td><stripes:label for="organisation.subtype"/>:</td>
                                <td><fmt:message   key="organisation.type.${actionBean.organisation.organisationSubType}" /></td>
                            </tr>			
                        </c:when>
                    </c:choose>
                    <tr>
                        <td><stripes:label for="organisation.name"/>:</td>
                        <td>${actionBean.organisation.organisationName}</td>
                    </tr>
                    
<!--                    <tr>
                        <td><stripes:label for="companynumber"/>:</td>
                        <td>${actionBean.organisation.companyNumber}</td>
                    </tr>-->
                    <c:choose>
                    <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
			<c:choose>
                        <c:when test="${actionBean.organisation.organisationType == 'company' || actionBean.organisation.organisationType == 'ngo' || actionBean.organisation.organisationType == 'trust'}">							
                            <tr>
                                <td><stripes:label for="companynumber"/>:</td>
                                <td>${actionBean.organisation.companyNumber}</td>
                            </tr>			
                        </c:when>
                        </c:choose>		
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td><stripes:label for="companynumber"/>:</td>
                            <td>${actionBean.organisation.companyNumber}</td>
                        </tr>
                    </c:otherwise>
                    </c:choose>
<!--                    <tr>
                        <td><stripes:label for="taxnumber"/>:</td>
                        <td>${actionBean.organisation.taxNumber}</td>
                    </tr>-->
                    <c:choose>
                    <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
			<c:choose>
                        <c:when test="${actionBean.organisation.organisationType == 'company' || actionBean.organisation.organisationType == 'ngo' || actionBean.organisation.organisationType == 'trust'}">							
                            <tr>
                                <td><stripes:label for="taxnumber"/>:</td>
                                <td>${actionBean.organisation.taxNumber}</td>
                            </tr>			
                        </c:when>
                        </c:choose>		
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td><stripes:label for="taxnumber"/>:</td>
                            <td>${actionBean.organisation.taxNumber}</td>
                        </tr>
                    </c:otherwise>
                    </c:choose>
                    <tr>
                        <td><stripes:label for="email"/>:</td>
                        <td>${actionBean.organisation.emailAddress}</td>
                    </tr>     
                    <tr>
                        <td><stripes:label for="alternative.contact.number.1"/>:</td>
                        <td>${actionBean.organisation.alternativeContact1}</td>
                    </tr>
                    <tr>
                        <td><stripes:label for="alternative.contact.number.2"/>:</td>
                        <td>${actionBean.organisation.alternativeContact2}</td>
                    </tr>
<!--                    <tr>
                        <td><stripes:label for="industry"/>:</td>
                        <td><fmt:message   key="industry.${actionBean.organisation.industry}"/></td>
                    </tr>-->
                    <c:choose>
                    <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
			<c:choose>
                            <c:when test="${actionBean.organisation.organisationType == 'company'}">							
                                <tr>
                                    <td><stripes:label for="industry"/>:</td>
                                    <td><fmt:message   key="industry.${actionBean.organisation.industry}"/></td>
                                </tr>			
                            </c:when>
                        </c:choose>		
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td><stripes:label for="industry"/>:</td>
                            <td><fmt:message   key="industry.${actionBean.organisation.industry}"/></td>
                        </tr>
                    </c:otherwise>
                    </c:choose>
<!--                    <tr>
                        <td><stripes:label for="size"/>:</td>
                        <td>${actionBean.organisation.size}</td>
                    </tr>-->
                    <c:choose>
                    <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
			<c:choose>
                            <c:when test="${actionBean.organisation.organisationType == 'company' || actionBean.organisation.organisationType == 'ngo'}">							
                                <tr>
                                    <td><stripes:label for="size"/>:</td>
                                    <td>${actionBean.organisation.size}</td>
                                </tr>			
                            </c:when>
                        </c:choose>		
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td><stripes:label for="size"/>:</td>
                            <td>${actionBean.organisation.size}</td>
                        </tr>
                    </c:otherwise>
                    </c:choose>
                </table>


                <table class="clear">
                    <tr>
                        <td>
                            <br />
                            <b><fmt:message key="organisation.addresses"/>:</b>
                            <br />
                        </td>
                    </tr>
                    <c:forEach items="${actionBean.organisation.addresses}" varStatus="loop">                    
                        <tr>
                            <td style='vertical-align: top; margin-top:auto'>
                                ${actionBean.organisation.addresses[loop.index].type}
                                <stripes:hidden name="organisation.addresses[${loop.index}].type"    value="${actionBean.organisation.addresses[loop.index].type}" />
                            </td>
                            <td style='vertical-align: top; margin-top:auto'>
                                <table class="clear" style='vertical-align: top; margin-top:auto'>
                                    <tr>
                                        <td><fmt:message key="address.line1"/>:</td>
                                        <td>
                                            ${actionBean.organisation.addresses[loop.index].line1}
                                            <stripes:hidden name="organisation.addresses[${loop.index}].line1"   value="${actionBean.organisation.addresses[loop.index].line1}" />
                                        </td>
                                    </tr>                        
                                    <tr>
                                        <td><fmt:message key="address.line2"/>:</td>
                                        <td>
                                            ${actionBean.organisation.addresses[loop.index].line2}
                                            <stripes:hidden name="organisation.addresses[${loop.index}].line2"   value="${actionBean.organisation.addresses[loop.index].line2}" />
                                        </td>
                                    </tr>

                                    <tr>
                                        <td><fmt:message key="town"/>:</td>
                                        <td>
                                            ${actionBean.organisation.addresses[loop.index].town}
                                            <stripes:hidden name="organisation.addresses[${loop.index}].town"    value="${actionBean.organisation.addresses[loop.index].town}" />
                                        </td>
                                    </tr>

                                    <tr>
                                        <td><fmt:message key="zone"/>:</td>
                                        <td>
                                            ${actionBean.organisation.addresses[loop.index].zone}
                                            <stripes:hidden name="organisation.addresses[${loop.index}].zone" value="${actionBean.organisation.addresses[loop.index].zone}" />
                                        </td>
                                    </tr>


                                    <tr>
                                        <td><fmt:message key="state"/>:</td>
                                        <td>
                                            ${actionBean.organisation.addresses[loop.index].state}
                                            <stripes:hidden name="organisation.addresses[${loop.index}].state"    value="${actionBean.organisation.addresses[loop.index].state}" />
                                        </td>
                                    </tr>

                                    <tr>
                                        <td><fmt:message key="country"/>:</td>
                                        <td>
                                            ${actionBean.organisation.addresses[loop.index].country}
                                            <stripes:hidden name="organisation.addresses[${loop.index}].country" value="${actionBean.organisation.addresses[loop.index].country}" />
                                        </td>
                                    </tr>

                                    <c:choose>
                                        <c:when test="${s:getProperty('env.postalcode.display')}">
                                            <tr>
                                                <td><fmt:message key="code"/>:</td>
                                                <td>
                                                    ${actionBean.organisation.addresses[loop.index].code}
                                                    <stripes:hidden name="organisation.addresses[${loop.index}].code"    value="${actionBean.organisation.addresses[loop.index].code}" />
                                                </td>
                                            </tr>
                                        </c:when>
                                        <c:otherwise>
                                            <stripes:hidden name="organisation.addresses[${loop.index}].code"  value="${actionBean.organisation.addresses[loop.index].code}" />
                                        </c:otherwise>
                                    </c:choose>
                                </table>
                            </td>
                        </tr>
                    </c:forEach>

                    <tr>
                        <td>
                            <br />
                            <b><fmt:message key="scanned.documents"/>:</b>
                            <br />
                        </td>
                    </tr>
                    <c:forEach items="${actionBean.organisation.organisationPhotographs}" varStatus="loop"> 
                        <tr id="row${loop.index}">
                            <td align="left" valign="top">
                                <fmt:message  key="document.type.${actionBean.organisation.organisationPhotographs[loop.index].photoType}"/>
                                <stripes:hidden   name="organisation.organisationPhotographs[${loop.index}].photoType" value="${actionBean.organisation.organisationPhotographs[loop.index].photoType}" />
                                <stripes:hidden name="organisation.organisationPhotographs[${loop.index}].photoGuid" value="${actionBean.organisation.organisationPhotographs[loop.index].photoGuid}"/>
                                <stripes:hidden name="organisation.organisationPhotographs[${loop.index}].data" value="${actionBean.organisation.organisationPhotographs[loop.index].data}"/>
                            </td>
                            <td align="left" valign="top">
                                <a href="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.organisation.organisationPhotographs[loop.index].photoGuid}" target="_blank">
                                    <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.organisation.organisationPhotographs[loop.index].photoGuid}"/>
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                </table>     
                <span class="button">
                    <stripes:submit name="showAddOrganisationManageOrganisationAddressesSummaryBack"/>
                </span>
                <span class="button">
                    <stripes:submit name="addOrganisationWizard"/>
                </span>                     
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>