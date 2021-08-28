<%@page import="com.smilecoms.commons.base.BaseUtils, com.smilecoms.commons.stripes.SmileActionBean"%>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="service.instance.detail"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">
            <c:if test="${s:getPropertyWithDefault('env.customer.verify.with.nida','false') == 'true'}">
                removeFingerprints();  // For Tanzania - Remove any fingerprints captured
            </c:if>
        </script>
        
        <div id="entity">
            <c:set var="serviceSpecification" value="${actionBean.serviceSpecification}"/>
            <table class="entity_header">
                <tr>
                    <td>${serviceSpecification.name}</td>                        
                    <td align="right">                       
                        <stripes:form action="/ProductCatalog.action">                                
                            <stripes:select name="entityAction">
                                <stripes:option value="showChangeServiceInstanceCustomer"><fmt:message key="change.customer"/></stripes:option>
                                <% if (BaseUtils.getBooleanProperty("env.allow.change.product.customer", true)) {%>
                                    <stripes:option value="showChangeServiceInstanceAccount"><fmt:message key="change.account"/></stripes:option>
                                <% } %>
                                <stripes:option value="showChangeServiceInstanceConfiguration"><fmt:message key="change.service.avps"/></stripes:option>
                                <stripes:option value="showChangeServiceInstanceSpecification"><fmt:message key="change.service.specification"/></stripes:option>

                                <c:if test="${actionBean.serviceInstance.status == 'AC'}">
                                    <stripes:option value="temporarilyDeactivateServiceInstance"><fmt:message key="temporarily.deactivate.service.instance"/></stripes:option>
                                </c:if>

                                <c:if test="${actionBean.serviceInstance.status == 'TD'}">
                                    <stripes:option value="reactivateServiceInstance"><fmt:message key="reactivate.service.instance"/></stripes:option>
                                </c:if>

                                <stripes:option value="removeServiceInstance"><fmt:message key="remove.service.instance"/></stripes:option>
                            </stripes:select>
                            <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                            <stripes:hidden name="productInstance.productInstanceId" value="${actionBean.productInstance.productInstanceId}"/>
                            <stripes:hidden name="productInstance.productSpecificationId" value="${actionBean.productInstance.productSpecificationId}"/>
                            <stripes:hidden name="serviceInstance.serviceInstanceId" value="${actionBean.serviceInstance.serviceInstanceId}"/>
                            <stripes:hidden name="serviceInstance.serviceSpecificationId" value="${actionBean.serviceInstance.serviceSpecificationId}"/>
                            <stripes:hidden name="serviceInstance.status" value="${actionBean.serviceInstance.status}"/>
                            <stripes:hidden name="serviceSpecification.name" value="${serviceSpecification.name}"/>
                            <stripes:hidden name="serviceSpecification.description" value="${serviceSpecification.description}"/>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>

            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="service.specific.info"/></b></td>
                </tr>
                <tr>
                    <td><fmt:message key="id"/>:</td>
                    <td>${actionBean.serviceInstance.serviceInstanceId}</td>
                </tr>
                <tr>
                    <td><fmt:message key="provisioning.status"/>:</td>
                    <td>${actionBean.serviceInstance.status}</td>
                </tr>
                <tr>
                    <td><fmt:message key="product.instance.id"/>:</td>
                    <td>
                        <stripes:link href="/ProductCatalog.action" event="retrieveProductInstance"> 
                            <stripes:param name="productInstance.productInstanceId" value="${actionBean.serviceInstance.productInstanceId}"/>
                            ${actionBean.serviceInstance.productInstanceId}
                        </stripes:link>
                    </td>
                </tr>
                <tr>
                    <td><fmt:message key="service.name"/>:</td>
                    <td>${serviceSpecification.name}</td>
                </tr>
                <tr>
                    <td><fmt:message key="created.date"/>:</td>
                    <td>${s:formatDateLong(actionBean.serviceInstance.createdDateTime)}</td>
                </tr>
                <tr>
                    <td><fmt:message key="service.description"/>:</td>
                    <td>${serviceSpecification.description}</td>
                </tr>   
                <tr>
                    <td><fmt:message key="service.code"/>:</td>
                    <td>${serviceSpecification.serviceCode}</td>
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
                <tr>
                    <td><fmt:message key="account.id"/>:</td>
                    <td>
                        <stripes:link href="/Account.action" event="retrieveAccount"> 
                            <stripes:param name="accountQuery.accountId" value="${actionBean.serviceInstance.accountId}"/>
                            ${actionBean.serviceInstance.accountId}
                        </stripes:link>
                    </td>
                </tr>
                <tr>
                    <td><fmt:message key="rate.plan"/>:</td>
                    <td>
                        <stripes:link href="/ProductCatalog.action" event="retrieveRatePlan"> 
                            <stripes:param name="ratePlan.ratePlanId" value="${actionBean.ratePlan.ratePlanId}"/>
                            ${actionBean.ratePlan.name}
                        </stripes:link>                    
                    </td>
                </tr>
            </table>

            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="service.instance.configuration"/></b></td>
                </tr>
            </table>
            <table class="green"> 
                <tr>
                    <th>Attribute</th>
                    <th>Value</th> 
                </tr>
                <c:forEach items="${actionBean.serviceInstance.AVPs}" var="avp">                        
                    <s:avp-display-row avp="${avp}"/>                        
                </c:forEach>
            </table>



            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="service.instance.mappings"/></b></td>
                </tr>
            </table>
            <table class="green"> 
                <tr>
                    <th>Identifier Type</th> 
                    <th>Identifier</th>
                </tr>
                <c:forEach items="${actionBean.serviceInstance.serviceInstanceMappings}" var="mapping">                        
                    <tr>
                        <td>${mapping.identifierType}</td>                        
                        <td>
                            <stripes:link href="/IMS.action" event="retrieveIMSDataForMapping"> 
                                <stripes:param name="mappingIdentifier" value="${mapping.identifier}"/>
                                <stripes:param name="mappingIdentifierType" value="${mapping.identifierType}"/>
                                ${mapping.identifier}
                            </stripes:link>          
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </div>
    </stripes:layout-component>
</stripes:layout-render>

