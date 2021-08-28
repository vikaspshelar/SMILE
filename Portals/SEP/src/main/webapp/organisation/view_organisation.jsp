<%@page import="com.smilecoms.commons.base.BaseUtils, com.smilecoms.commons.stripes.SmileActionBean"%>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="manage.organisation"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        
            <table class="entity_header">
                <tr>
                    <td>
                        <fmt:message key="organisation"/> ${actionBean.organisation.organisationName} 
                    </td>

                    <td align="right">

                        <stripes:form action="/Customer.action">
                            <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                            <stripes:hidden name="organisation.organisationId" value="${actionBean.organisation.organisationId}"/>
                            <stripes:hidden name="organisation.organisationName" value="${actionBean.organisation.organisationName}"/>
                            <stripes:select name="entityAction">
                                <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                                    <stripes:option value="showEditOrganisation"><fmt:message key="edit"/></stripes:option>
                                    <stripes:option value="showEditPhotographs"><fmt:message key="manage.photographs"/></stripes:option>
                                    <stripes:option value="manageAddresses"><fmt:message key="manage.addresses"/></stripes:option>
                                    <stripes:option value="manageContracts"><fmt:message key="manage.contracts"/></stripes:option>
                                    <% if (BaseUtils.getBooleanProperty("env.show.legal.contacts", false)) {%>
                                        <stripes:option value="showOrganisationLegalContact">Manage Legal Contact</stripes:option>
                                    <% } %>
                                    <stripes:option value="showChangeOrganisationsAccountManagerCustomer"><fmt:message key="change.account.manager.customer"/></stripes:option>
                                    <c:if test="${(s:getPropertyWithDefault('env.sep.mnp.enabled','false') == 'true') && (s:getPropertyWithDefault('env.sep.mnp.org.enabled','false') == 'true')}"> 
                                        <stripes:option value="showCreateOrganisationPortInRequest"><fmt:message key="customer.port.into.smile"/></stripes:option>
                                        <stripes:option value="showEmergencyRestore"><fmt:message key="customer.port.emergency.restore"/></stripes:option>
                                        <stripes:option value="showCreateRingFenceRequest"><fmt:message key="create.ring.fence"/></stripes:option>
                                        <stripes:option value="searchPortOrder"><fmt:message key="view.customer.port.orders"/></stripes:option>
                                        <stripes:option value="showSearchPortOrder"><fmt:message key="search.porting.order"/></stripes:option>
                                    </c:if>
                                </c:if>
                                <stripes:option value="showChangeModificationRoles"><fmt:message key="change.modification.roles"/></stripes:option>
                                <% if (BaseUtils.getBooleanProperty("env.allow.sellers.update", false)) {%>      
                                <stripes:option value="showUpdateOrganisationSellers">Edit Organisation Sellers</stripes:option>
                                <% } %>
                            </stripes:select>
                            <stripes:submit name="performEntityAction" />
                        </stripes:form>

                    </td>
                </tr>
            </table>

            <table class="clear">
                <tr>
                    <td><fmt:message key="id"/>:</td>
                    <td>${actionBean.organisation.organisationId}</td>
                </tr>
                <tr>
                    <td><stripes:label for="organisation.name"/>:</td>
                    <td>${actionBean.organisation.organisationName}</td>
                </tr>

<!--                <tr>
                    <td><stripes:label for="company.number"/>:</td>
                    <td>${actionBean.organisation.companyNumber}</td>
                </tr>-->
                
                <c:choose>
                    <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
			<c:choose>
                        <c:when test="${actionBean.organisation.organisationType == 'company' || actionBean.organisation.organisationType == 'ngo' || actionBean.organisation.organisationType == 'trust'}">							
                            <tr>
                                <td><stripes:label for="company.number"/>:</td>
                                <td>${actionBean.organisation.companyNumber}</td>
                            </tr>			
                        </c:when>
                        </c:choose>		
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td><stripes:label for="company.number"/>:</td>
                            <td>${actionBean.organisation.companyNumber}</td>
                        </tr>
                    </c:otherwise>
                </c:choose>
                
<!--                <tr>
                    <td><stripes:label for="organisation.taxnumber"/>:</td>
                    <td>${actionBean.organisation.taxNumber}</td>
                </tr>-->
                
                <c:choose>
                    <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
			<c:choose>
                        <c:when test="${actionBean.organisation.organisationType == 'company' || actionBean.organisation.organisationType == 'ngo' || actionBean.organisation.organisationType == 'trust'}">							
                            <tr>
                                <td><stripes:label for="organisation.taxnumber"/>:</td>
                                <td>${actionBean.organisation.taxNumber}</td>
                            </tr>			
                        </c:when>
                        </c:choose>		
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td><stripes:label for="organisation.taxnumber"/>:</td>
                            <td>${actionBean.organisation.taxNumber}</td>
                        </tr>
                    </c:otherwise>
                </c:choose>
                
<!--                <tr>
                    <td><stripes:label for="organisation.industry"/>:</td>
                    <td><fmt:message   key="industry.${actionBean.organisation.industry}" /></td>
                </tr>-->
                
                <c:choose>
                    <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
			<c:choose>
                        <c:when test="${actionBean.organisation.organisationType == 'company' || actionBean.organisation.organisationType == 'ngo' || actionBean.organisation.organisationType == 'trust'}">							
                            <tr>
                                <td><stripes:label for="organisation.industry"/>:</td>
                                <td><fmt:message   key="industry.${actionBean.organisation.industry}" /></td>
                            </tr>			
                        </c:when>
                        </c:choose>		
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td><stripes:label for="organisation.industry"/>:</td>
                            <td><fmt:message   key="industry.${actionBean.organisation.industry}" /></td>
                        </tr>
                    </c:otherwise>
                </c:choose>
                
                <tr>
                    <td><stripes:label for="organisation.type"/>:</td>
                    <td><fmt:message   key="organisation.type.${actionBean.organisation.organisationType}" /></td>
                </tr>

                <tr>
                    <td><stripes:label for="organisation.status"/>:</td>
                    <td>${actionBean.organisation.organisationStatus}</td>
                </tr>
                <tr>
                    <td><stripes:label for="email.address"/>:</td>
                    <td>${actionBean.organisation.emailAddress}</td>
                </tr>

                <tr>
                    <td><stripes:label for="alternative.contact1"/>:</td>
                    <td>${actionBean.organisation.alternativeContact1}</td>
                </tr>
                <tr>
                    <td><stripes:label for="alternative.contact2"/>:</td>
                    <td>${actionBean.organisation.alternativeContact2}</td>
                </tr>
                <tr>
                    <td>Signed up by:</td>
                    <td>
                        <stripes:link href="/Customer.action" event="retrieveCustomer">
                            <stripes:param name="customer.customerId" value="${actionBean.organisation.createdByCustomerProfileId}"/>
                            ${s:getCustomerName(actionBean.organisation.createdByCustomerProfileId)}
                        </stripes:link>
                    </td>
                </tr>
                <tr>
                    <td><fmt:message key="account.manager.customer"/>:</td>
                    <td>
                        <c:choose>
                            <c:when test="${actionBean.organisation.accountManagerCustomerProfileId > 0}">
                                <stripes:link href="/Customer.action" event="retrieveCustomer">
                                    <stripes:param name="customer.customerId" value="${actionBean.organisation.accountManagerCustomerProfileId}"/>
                                    ${s:getCustomerName(actionBean.organisation.accountManagerCustomerProfileId)}
                                </stripes:link>
                            </c:when>
                            <c:otherwise>
                                Not Assigned
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                <tr>
                    <td><stripes:label for="credit.account.number"/>:</td>
                    <td>${actionBean.organisation.creditAccountNumber}</td>
                </tr>
                <tr>
                    <td>Partner Code:</td>
                    <td>${actionBean.organisation.channelCode}</td>
                </tr>
                <c:if test="${fn:length(s:getPropertyWithDefault('env.organisation.kyc.status.mappings', '').trim()) > 0}">
                    <tr>
                        <td><stripes:label for="organisation.kycstatus"/>:</td>
                        <c:set var="defaultRule" value="validate(this,'^.{1,20}$','emptyok')"/>
                        <td>${s:getDelimitedPropertyValueMapping('env.organisation.kyc.status.mappings', actionBean.organisation.kycStatus)}
                        </td>
                    </tr>

                    <tr>
                        <td><stripes:label for="organisation.kyccomment"/>:</td>
                        <td>
                            ${s:getDelimitedPropertyValueMapping('env.organisation.kyc.comment.mappings', actionBean.organisation.kycComment)}                        
                        </td>
                    </tr>
                </c:if>

            </table>
            <table class="clear">
                <tr>
                    <td>
                        <b>Show Details <input type="checkbox" value="" onchange="toggleMe('detail');" /></b>
                    </td>
                </tr>
            </table>
            <div id="detail" style="display:none">
                <table class="clear">
                    <tr>
                        <td colspan="2">
                            <b><fmt:message key="organisation.addresses"/>:</b>
                        </td>
                    </tr>
                    <c:forEach items="${actionBean.organisation.addresses}" varStatus="loop">
                        <tr>
                            <td style='vertical-align: top; margin-top:auto'>
                                ${actionBean.customer.addresses[loop.index].type}
                            </td>
                            <td style='vertical-align: top; margin-top:auto'>
                                <table class="clear" style='vertical-align: top; margin-top:auto'>
                                    <tr>
                                        <td><fmt:message key="address.line1"/>:</td>
                                        <td>
                                            ${actionBean.organisation.addresses[loop.index].line1}
                                        </td>
                                    </tr>                        
                                    <tr>
                                        <td><fmt:message key="address.line2"/>:</td>
                                        <td>
                                            ${actionBean.organisation.addresses[loop.index].line2}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td><fmt:message key="town"/>:</td>
                                        <td>
                                            ${actionBean.organisation.addresses[loop.index].town}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td><fmt:message key="zone"/>:</td>
                                        <td>${actionBean.organisation.addresses[loop.index].zone}</td>
                                    </tr>
                                    <tr>
                                        <td><fmt:message key="state"/>:</td>
                                        <td>${actionBean.organisation.addresses[loop.index].state}</td>
                                    </tr>
                                    <tr>
                                        <td><fmt:message key="country"/>:</td>
                                        <td>
                                            ${actionBean.organisation.addresses[loop.index].country}
                                        </td>
                                    </tr>
                                    <c:if test="${s:getProperty('env.postalcode.display')}">
                                        <tr>
                                            <td><fmt:message key="code"/>:</td>
                                            <td>
                                                ${actionBean.organisation.addresses[loop.index].code}
                                            </td>
                                        </tr>
                                    </c:if>
                                </table>                                
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                        <tr>
                            <td colspan="2">
                                <s:displayWhenReady event="retrieveImagesSnippet" href="/Customer.action" containerId="images" autoLoad="false" displayMessage="Click here to view attached documents">
                                    <stripes:param name="organisation.organisationId" value="${actionBean.organisation.organisationId}"/>
                                </s:displayWhenReady>  
                            </td>
                        </tr>
                    </c:if>
                </table>
            </div>
            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="organisations.roles"/></b></td>
                </tr>
            </table>
            <c:choose>
                <c:when test="${s:getListSize(actionBean.organisation.customerRoles) > 0}">


                    <c:if   test="${s:getListSize(actionBean.organisation.customerRoles) == actionBean.organisationQuery.rolesResultLimit 
                                    || actionBean.organisationQuery.rolesOffset != 0}">
                            <table class="clear" width="99%">
                                <tr>
                                    <td align="left">
                                        <c:if   test="${actionBean.organisationQuery.rolesOffset != 0}">
                                            <stripes:form action="/Customer.action">
                                                <stripes:submit name="previousOrgRolesPage"/>
                                                <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                                                <input type="hidden" name="organisationQuery.rolesOffset" value="${actionBean.organisationQuery.rolesOffset - actionBean.organisationQuery.rolesResultLimit}" />
                                            </stripes:form>
                                        </c:if>
                                    </td>
                                    <td align="right">
                                        <c:if   test="${s:getListSize(actionBean.organisation.customerRoles) == actionBean.organisationQuery.rolesResultLimit}">
                                            <stripes:form action="/Customer.action">
                                                <stripes:submit name="nextOrgRolesPage"/>
                                                <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                                                <input type="hidden" name="organisationQuery.rolesOffset" value="${actionBean.organisationQuery.rolesOffset + actionBean.organisationQuery.rolesResultLimit}" />
                                            </stripes:form>
                                        </c:if>
                                    </td>
                                </tr>
                            </table>
                    </c:if>


                    <table class="green" width="99%">                                
                        <tr>
                            <th><fmt:message key="customer.name"/></th>
                            <th><fmt:message key="role.name"/></th>
                            <th><fmt:message key="view.customer"/></th>
                        </tr>
                        <c:forEach items="${actionBean.organisation.customerRoles}" var="role" varStatus="loop">
                            <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                <td>${role.customerName}</td>
                                <td>${role.roleName}</td>
                                <td>
                                    <stripes:form action="/Customer.action" class="buttonOnly">
                                        <input type="hidden" name="customer.customerId" value="${role.customerId}"/>
                                        <stripes:submit name="retrieveCustomer"/>
                                    </stripes:form>                                
                                </td>
                            </tr>
                        </c:forEach>
                    </table>
                    <c:if   test="${s:getListSize(actionBean.organisation.customerRoles) == actionBean.organisationQuery.rolesResultLimit 
                                    || actionBean.organisationQuery.rolesOffset != 0}">
                            <table class="clear" width="99%">
                                <tr>
                                    <td align="left">
                                        <c:if   test="${actionBean.organisationQuery.rolesOffset != 0}">
                                            <stripes:form action="/Customer.action">
                                                <stripes:submit name="previousOrgRolesPage"/>
                                                <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                                                <input type="hidden" name="organisationQuery.rolesOffset" value="${actionBean.organisationQuery.rolesOffset - actionBean.organisationQuery.rolesResultLimit}" />
                                            </stripes:form>
                                        </c:if>
                                    </td>
                                    <td align="right">
                                        <c:if   test="${s:getListSize(actionBean.organisation.customerRoles) == actionBean.organisationQuery.rolesResultLimit}">
                                            <stripes:form action="/Customer.action">
                                                <stripes:submit name="nextOrgRolesPage"/>
                                                <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                                                <input type="hidden" name="organisationQuery.rolesOffset" value="${actionBean.organisationQuery.rolesOffset + actionBean.organisationQuery.rolesResultLimit}" />
                                            </stripes:form>
                                        </c:if>
                                    </td>
                                </tr>
                            </table>
                    </c:if>
                </c:when>
                <c:otherwise>
                    <table class="green" width="99%">    
                        <tr>
                            <td>
                                <fmt:message key="no.organisation.roles"/>
                            </td>
                        </tr>
                    </table>
                </c:otherwise>
            </c:choose>

            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="organisations.product.instances"/></b></td>
                </tr>
            </table>
            <c:choose>
                <c:when test="${s:getListSize(actionBean.productInstanceList.productInstances) > 0}">


                    <c:if   test="${s:getListSize(actionBean.productInstanceList.productInstances) == actionBean.productInstanceQuery.resultLimit 
                                    || actionBean.productInstanceQuery.offset != 0}">
                            <table class="clear" width="99%">
                                <tr>
                                    <td align="left">
                                        <c:if   test="${actionBean.productInstanceQuery.offset != 0}">
                                            <stripes:form action="/Customer.action">
                                                <stripes:submit name="previousOrgProductInstancePage"/>
                                                <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                                                <input type="hidden" name="productInstanceQuery.offset" value="${actionBean.productInstanceQuery.offset - actionBean.productInstanceQuery.resultLimit}" />
                                            </stripes:form>
                                        </c:if>
                                    </td>
                                    <td align="right">
                                        <c:if   test="${s:getListSize(actionBean.productInstanceList.productInstances) == actionBean.productInstanceQuery.resultLimit}">
                                            <stripes:form action="/Customer.action">
                                                <stripes:submit name="nextOrgProductInstancePage"/>
                                                <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                                                <input type="hidden" name="productInstanceQuery.offset" value="${actionBean.productInstanceQuery.offset + actionBean.productInstanceQuery.resultLimit}" />
                                            </stripes:form>
                                        </c:if>
                                    </td>
                                </tr>
                            </table>
                    </c:if>


                    <table class="green" width="99%">                                
                        <tr>
                            <th><fmt:message key="id"/></th>
                            <th><fmt:message key="product.name"/></th>
                            <th><fmt:message key="product.description"/></th>
                                <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                                <th><fmt:message key="product.settings"/></th>
                                </c:if>
                        </tr>
                        <c:forEach items="${actionBean.productInstanceList.productInstances}" var="productInstance" varStatus="loop">
                            <c:set var="productSpec" value="${s:getProductSpecification(productInstance.productSpecificationId)}"/>
                            <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                <td>${productInstance.productInstanceId}</td>
                                <td>${productSpec.name}</td>
                                <td>${productSpec.description}</td> 
                                <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                                    <td>
                                        <stripes:form action="/ProductCatalog.action" class="buttonOnly">
                                            <input type="hidden" name="productInstance.productInstanceId" value="${productInstance.productInstanceId}"/>
                                            <stripes:submit name="retrieveProductInstance"/>
                                        </stripes:form>                                
                                    </td>
                                </c:if>
                            </tr>
                        </c:forEach>
                    </table>
                    <c:if   test="${s:getListSize(actionBean.productInstanceList.productInstances) == actionBean.productInstanceQuery.resultLimit 
                                    || actionBean.productInstanceQuery.offset != 0}">
                            <table class="clear" width="99%">
                                <tr>
                                    <td align="left">
                                        <c:if   test="${actionBean.productInstanceQuery.offset != 0}">
                                            <stripes:form action="/Customer.action">
                                                <stripes:submit name="previousOrgProductInstancePage"/>
                                                <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                                                <input type="hidden" name="productInstanceQuery.offset" value="${actionBean.productInstanceQuery.offset - actionBean.productInstanceQuery.resultLimit}" />
                                            </stripes:form>
                                        </c:if>
                                    </td>
                                    <td align="right">
                                        <c:if   test="${s:getListSize(actionBean.productInstanceList.productInstances) == actionBean.productInstanceQuery.resultLimit}">
                                            <stripes:form action="/Customer.action">
                                                <stripes:submit name="nextOrgProductInstancePage"/>
                                                <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                                                <input type="hidden" name="productInstanceQuery.offset" value="${actionBean.productInstanceQuery.offset + actionBean.productInstanceQuery.resultLimit}" />
                                            </stripes:form>
                                        </c:if>
                                    </td>
                                </tr>
                            </table>
                    </c:if>
                </c:when>
                <c:otherwise>
                    <table class="green" width="99%">
                        <tr>
                            <td>
                                <fmt:message key="no.product.instances"/>
                            </td>
                        </tr>
                    </table>
                </c:otherwise>
            </c:choose>
        
    </stripes:layout-component>
</stripes:layout-render>
