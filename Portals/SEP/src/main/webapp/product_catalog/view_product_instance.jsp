<%@page import="com.smilecoms.commons.base.BaseUtils, com.smilecoms.commons.stripes.SmileActionBean"%>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="product.instance.detail"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <c:set var="customer" value="${actionBean.customer}"/>
            <c:set var="productSpecification" value="${actionBean.productSpecification}"/>
            <c:set var="prodInstance" value="${actionBean.productInstance}"/>


            <table class="entity_header">
                <tr>
                    <td>${productSpecification.name}</td>                        
                    <td align="right">                       
                <stripes:form action="/ProductCatalog.action">                                
                    <stripes:select name="entityAction" id="entityaction">
                        <stripes:option value="showAddServiceInstanceList"><fmt:message key="add.service.instance"/></stripes:option>                            
                        <% if (BaseUtils.getBooleanProperty("env.allow.change.product.customer", true)) {%>
                            <stripes:option value="showChangeProductInstanceCustomer"><fmt:message key="change.customer.or.organisation"/></stripes:option>
                        <% } %>
                        <stripes:option value="removeProductInstance"><fmt:message key="remove.product.instance"/></stripes:option>
                        <stripes:option value="retrieveNotes"><fmt:message key="view.notes"/></stripes:option>
                        <stripes:option value="retrieveNoteTypes"><fmt:message key="attach.note"/></stripes:option>
                        <% if (BaseUtils.getBooleanProperty("env.sales.sim.unbundling.logic.enabled", false)) {%>
                            <stripes:option value="removeSIMUnbundlingRestriction"><fmt:message key="remove.sim.unbundling.restriction"/></stripes:option>
                        <% } %>
                    </stripes:select>
                    <stripes:hidden name="customerQuery.customerId" value="${customer.customerId}"/>
                    <stripes:hidden name="productSpecification.productSpecificationId" value="${actionBean.productSpecification.productSpecificationId}"/>
                    <stripes:hidden name="productInstance.productSpecificationId" value="${actionBean.productSpecification.productSpecificationId}"/>
                    <input type="hidden" name="productInstance.productInstanceId" value="${actionBean.productInstance.productInstanceId}"/>
                    <stripes:hidden name="productOrder.segment" value="${actionBean.productInstance.segment}"/>
                    <stripes:hidden name="productOrder.customerId" value="${actionBean.productInstance.customerId}"/>
                    <stripes:hidden name="productOrder.organisationId" value="${actionBean.productInstance.organisationId}"/>
                    <stripes:submit name="performEntityAction"/>
                </stripes:form>
                </td>
                </tr>
            </table>

            <table class="clear">                
                <tr>
                    <td colspan="2"><b><fmt:message key="product.specific.info"/></b></td>
                </tr>
                <tr>
                    <td><fmt:message key="id"/>:</td>
                <td>${prodInstance.productInstanceId}</td>
                </tr>
                <c:if test="${prodInstance.status == 'TD'}">
                    <tr class="greyout">
                </c:if>
                <c:if test="${prodInstance.status != 'TD'}">
                    <tr>
                </c:if>
                    <td><fmt:message key="provisioning.status"/>:</td>
                <td>${prodInstance.status}</td>
                </tr>
                <tr>
                    <td><fmt:message key="Logical SIM Id"/>:</td>
                <td>${prodInstance.logicalId}</td>
                </tr>
                <tr>
                    <td><fmt:message key="friendly.name"/>:</td>
                <td>${prodInstance.friendlyName}</td>
                </tr>
                <tr>
                    <td><fmt:message key="product.name"/>:</td>
                <td>${productSpecification.name}</td>
                </tr>
                <tr>
                    <td><fmt:message key="product.description"/>:</td>
                <td>${productSpecification.description}</td>
                </tr>
                <tr>
                    <td>Phone Number:</td>
                    <td>${s:getProductInstancePhoneNumber(prodInstance.productInstanceId)}</td>
                </tr>
                <tr>
                    <td><fmt:message key="created.date"/>:</td>
                <td>${s:formatDateLong(prodInstance.createdDateTime)}</td>
                </tr>
                <tr>
                    <td>First Used Date:</td>
                    <td>${s:formatDateLong(prodInstance.firstActivityDateTime)}</td>
                </tr>
                <tr>
                    <td>Last Used Date:</td>
                    <td>${s:formatDateLong(prodInstance.lastActivityDateTime)}</td>
                </tr>
                <tr>
                    <td><fmt:message key="segment"/>:</td>
                <td>${prodInstance.segment}</td>
                </tr>
                <tr>
                    <td><fmt:message key="promotion.code"/>:</td>
                <td>${prodInstance.promotionCode}</td>
                </tr>
                <tr>
                    <td>Referral Code:</td>
                    <td>${prodInstance.referralCode}</td>
                </tr>
                <tr>
                    <td><fmt:message key="associated.customer.name"/>:</td>
                <td>
                <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                    <stripes:param name="customer.customerId" value="${actionBean.customer.customerId}"/>
                    ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                </stripes:link>
                </td>
                </tr>
                <c:if test="${prodInstance.organisationId > 0}">
                    <tr>
                        <td><fmt:message key="organisation"/>:</td>
                    <td>
                    <stripes:link href="/Customer.action" event="retrieveOrganisation"> 
                        <stripes:param name="organisation.organisationId" value="${prodInstance.organisationId}"/>
                        ${s:getEntryInList(actionBean.customer.customerRoles,'getOrganisationId',prodInstance.organisationId).roleName} at ${s:getEntryInList(actionBean.customer.customerRoles,'getOrganisationId',prodInstance.organisationId).organisationName}
                    </stripes:link>
                    </td>
                    </tr>
                </c:if>
                <tr>
                    <td>Last Device:</td>
                    <td>
                        ${prodInstance.lastDevice}
                    </td>
                </tr>
                <tr>
                    <td>Provisioned by:</td>
                    <td>
                        <stripes:link href="/Customer.action" event="retrieveCustomer">
                            <stripes:param name="customer.customerId" value="${prodInstance.createdByCustomerProfileId}"/>
                            ${s:getCustomerName(prodInstance.createdByCustomerProfileId)}
                        </stripes:link> for 
                        <stripes:link href="/Customer.action" event="retrieveOrganisation">
                            <stripes:param name="organisation.organisationId" value="${prodInstance.createdByOrganisationId}"/>
                            ${s:getOrganisationName(prodInstance.createdByOrganisationId)}
                        </stripes:link>
                    </td>
                </tr>
                <tr>
                    <td colspan="2"><b><fmt:message key="service.instances"/></b></td>
                </tr>

            </table>

            <table class="green" width="99%">                                
                <tr>
                    <th><fmt:message key="id"/></th>
                <th><fmt:message key="service.name"/></th>
                <th><fmt:message key="provisioning.status"/></th>
                <th><fmt:message key="description"/></th>
                <th><fmt:message key="customer"/></th>
                <th><fmt:message key="service.charging.info"/></th>
                <th><fmt:message key="service.configuration"/></th>
                </tr>

                <c:forEach items="${prodInstance.productServiceInstanceMappings}" var="mapping" varStatus="loop">
                    <c:set var="serviceSpec" value="${s:getServiceSpecification(mapping.serviceInstance.serviceSpecificationId)}"/>
                    <c:if test="${mapping.serviceInstance.status == 'TD'}">
                        <tr class="greyout">
                    </c:if>
                    <c:if test="${mapping.serviceInstance.status != 'TD'}">
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                    </c:if>
                    <td>${mapping.serviceInstance.serviceInstanceId}</td>
                    <td>${serviceSpec.name}</td>
                    <td>${mapping.serviceInstance.status}</td>
                    <td>${serviceSpec.description}</td>
                    <td>
                    <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                        <stripes:param name="customerQuery.customerId" value="${mapping.serviceInstance.customerId}"/>
                        ${s:getCustomerName(mapping.serviceInstance.customerId)}
                    </stripes:link>
                    </td>
                    <c:set var="ratePlan" value="${s:getRatePlan(mapping.serviceInstance.ratePlanId)}"/>
                    <td>
                    <stripes:link href="/ProductCatalog.action" event="retrieveRatePlan"> 
                        <stripes:param name="ratePlan.ratePlanId" value="${ratePlan.ratePlanId}"/>
                        ${ratePlan.name}
                    </stripes:link>
                    <br/>
                    (Account: 
                    <stripes:link href="/Account.action" event="retrieveAccount"> 
                        <stripes:param name="accountQuery.accountId" value="${mapping.serviceInstance.accountId}"/>
                        ${mapping.serviceInstance.accountId}</stripes:link>)
                    </td>
                    <td>
                    <stripes:form action="/ProductCatalog.action" class="buttonOnly">
                        <stripes:param name="serviceInstance.serviceInstanceId" value="${mapping.serviceInstance.serviceInstanceId}"/>
                        <stripes:submit name="retrieveServiceInstance"/>                            
                    </stripes:form>                                    
                    </td>
                    </tr>
                </c:forEach> 
            </table>             
            <s:displayWhenReady event="retrieveStickyNoteListSnippet" href="/ProductCatalog.action" containerId="stickyNote" autoLoad="true">
                <stripes:param name="productInstance.productInstanceId" value="${prodInstance.productInstanceId}"/>
            </s:displayWhenReady>
            <c:if test="${s:getListSize(prodInstance.campaigns) > 0}">
                <table class="clear">                
                    <tr>
                        <td colspan="2"><b>Campaign Data</b></td>
                    </tr>
                </table>
                <table class="green" width="99%">
                    <tr>
                        <th>Campaign Id</th>
                        <th>Name</th>
                        <th>Start Date</th>
                        <th>End Date</th>
                        <th>Last Check</th>
                        <th>UC IDs</th>                 
                        <th>Status</th>                 
                    </tr>
                    <c:forEach items="${prodInstance.campaigns}" var="campaign" varStatus="loop">
                        <tr>
                            <td>${campaign.campaignId}</td>
                            <td>${campaign.name}</td>
                            <td>${s:formatDateShort(campaign.startDateTime)}</td>
                            <td>${s:formatDateShort(campaign.endDateTime)}</td>
                            <td>${s:formatDateLong(campaign.lastCheckDateTime)}</td>
                            <td>
                        <c:forEach items="${campaign.campaignUnitCredits}" var="uc">
                            ${uc} &nbsp;
                        </c:forEach>
                        </td>
                        <td><fmt:message key="campaign.${campaign.status}"/></td>
                        </tr>                    
                    </c:forEach>
                </table>
            </c:if>
        </div>
    </stripes:layout-component>
</stripes:layout-render>

