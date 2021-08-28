<%@page import="com.smilecoms.commons.base.BaseUtils, com.smilecoms.commons.stripes.SmileActionBean"%>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="search.customer"/>
</c:set>
<% if (SmileActionBean.getIsDeliveryPerson(request)) {%>
<c:set var="isDeliveryPerson" value="true"/>
<%} else {%>
<c:set var="isDeliveryPerson" value="false"/>
<% }%>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/Customer.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">   

            <table class="clear">
                <c:if test="${!isDeliveryPerson}">
                    <tr>
                        <td><fmt:message key="id"/>:</td>
                        <td><stripes:text  name="customerQuery.customerId" size="30" onkeyup="validate(this,'^[0-9]{1,8}$','emptyok')"/></td>
                        <td><fmt:message key="andor"/></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="identity.search"/>:</td>
                        <td><stripes:text name="customerQuery.SSOIdentity" size="30" onkeyup="validate(this,'^.{1,50}$','emptyok')"/></td>
                        <td><fmt:message key="andor"/></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="last.name"/>:</td>
                        <td><stripes:text name="customerQuery.lastName" size="30" onkeyup="validate(this,'^.{2,50}$','emptyok')"/></td>
                        <td><fmt:message key="andor"/></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="first.name"/>:</td>
                        <td><stripes:text name="customerQuery.firstName" size="30" onkeyup="validate(this,'^.{2,50}$','emptyok')"/></td>
                        <td><fmt:message key="andor"/></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="email"/>:</td>
                        <td><stripes:text name="customerQuery.emailAddress" size="30" onkeyup="validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,4}$/ ,'emptyok')"/></td>
                        <td><fmt:message key="andor"/></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="organisation.name"/>:</td>
                        <td><stripes:text name="customerQuery.organisationName" size="30" onkeyup="validate(this,'^.{2,50}$','emptyok')"/></td>
                        <td><fmt:message key="andor"/></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="id.number"/>:</td>
                        <td><stripes:text name="customerQuery.identityNumber" size="30" onkeyup="validate(this,'^.{5,50}$','emptyok')"/></td>
                        <td><fmt:message key="andor"/></td>
                    </tr>  
                    <% if (BaseUtils.getBooleanProperty("env.customer.national.id.mandatory", false)) {%>     
                    <tr>
                         
                           <td><fmt:message key="national.id.number"/>:</td>
                            <td><stripes:text name="customerQuery.nationalIdentityNumber" size="30" onkeyup="validate(this,'^.{5,50}$','emptyok')"/></td>
                            <td><fmt:message key="andor"/></td>
                                               
                    </tr> 
                    <% } %>
                    <tr>
                        <td><fmt:message key="phone.number"/>:</td>
                        <td><stripes:text name="serviceInstanceQuery.identifier" size="30" onkeyup="validate(this,'^.{10,15}$','emptyok')"/></td>
                        <td><fmt:message key="andor"/></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="iccid"/>:</td>
                        <td><stripes:text name="IMSSubscriptionQuery.integratedCircuitCardIdentifier" maxlength="20" size="30" onkeyup="validate(this,'^[0-9]{20,20}$','luhn_emptyok')"/></td>
                        <td><fmt:message key="andor"/></td>
                    </tr>
                    <tr>
                        <td><stripes:label for="IMSI"/>:</td>
                        <td><stripes:text name="IMSI" size="30" maxlength="15" onkeyup="validate(this,'^[0-9]{15,15}$','emptyok')"/></td>
                    </tr>
                    <tr>
                        <td><stripes:label for="result.limit"/>:</td>
                        <td>
                            <stripes:select name="customerQuery.resultLimit">
                                <stripes:option value="10">10</stripes:option>
                                <stripes:option value="20">20</stripes:option>
                                <stripes:option value="30">30</stripes:option>
                                <stripes:option value="40">40</stripes:option>
                                <stripes:option value="50">50</stripes:option>
                            </stripes:select>
                        </td>
                    </tr>    
                </c:if>
                <c:if test="${isDeliveryPerson}">
                    <stripes:hidden name="customerQuery.resultLimit" value="1"/>
                    <tr>
                        <td><fmt:message key="id"/>:</td>
                        <td><stripes:text  name="customerQuery.customerId" size="30" onkeyup="validate(this,'^[0-9]{1,8}$','emptyok')"/></td>
                    </tr>
                </c:if>
                <tr>
                    <td>
                        <span class="button">
                            <stripes:submit name="searchCustomer"/>
                        </span>
                    </td>
                </tr>
            </table>            
        </stripes:form>
        <br/>     
         
        <c:if test="${actionBean.customerList.numberOfCustomers > 0}">
            <table class="green" width="99%">
                <c:if test="${!isDeliveryPerson}">
                    <tr>
                        <th><fmt:message key="id"/></th>
                        <th><fmt:message key="first.name"/></th>
                        <th><fmt:message key="last.name"/></th>
                        <th><fmt:message key="ssoidentity"/></th>
                        <th><fmt:message key="email"/></th>
                        <th><fmt:message key="view"/></th>
                        <th>Direct Action</th>
                    </tr>
                    <c:forEach items="${actionBean.customerList.customers}" var="customer" varStatus="loop">
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            <td>${customer.customerId}</td>
                            <td>${customer.firstName}</td>
                            <td>${customer.lastName}</td> 
                            <td>${customer.SSOIdentity}</td>
                            <td>${s:breakUp(customer.emailAddress,40)}</td>
                            <stripes:form action="/Customer.action">
                            <input type="hidden" name="customerQuery.customerId" value="${customer.customerId}"/>
                            <td>
                                <stripes:submit name="retrieveCustomer"/>
                            </td>
                        </stripes:form>
                        <td align="right">
                            <c:set var="customerClassType" value="${s:getDelimitedPropertyValueMapping('env.customer.classifications', customer.classification)}"/>
                            <stripes:form action="/Customer.action">
                                <stripes:hidden name="portOrdersQuery.customerProfileId" value="${customer.customerId}"/>
                                <input type="hidden" name="customerQuery.customerId" value="${customer.customerId}"/>
                                <input type="hidden" name="customer.customerId" value="${customer.customerId}"/>
                                <input type="hidden" name="customer.SSOIdentity" value="${customer.SSOIdentity}"/>
                                <stripes:select name="entityAction" style="width:50px">
                                    <c:if test="${customer.SCAContext.obviscated != 'ob'}">
                                        <stripes:option value="editCustomer"><fmt:message key="edit"/></stripes:option>
                                    </c:if>
                                        
                                    <% if (BaseUtils.getBooleanProperty("env.pilot.nin.only.update", false)) {%>      
                                        <stripes:option value="editCustomerNIN"><fmt:message key="edit.customer.nin"/></stripes:option>
                                    <% } %>     
                                    <c:if test="${customerClassType != 'minor'}"> 
                                        <!-- Do not allow adding of product to a minor customer -->
                                        <stripes:option value="showAddProductWizard"><fmt:message key="add.product"/></stripes:option>
                                    </c:if>
                                    <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                                        <stripes:option value="showMakeSale"><fmt:message key="make.sale"/></stripes:option>
                                        <stripes:option value="showCustomersSales"><fmt:message key="view.customers.sales"/></stripes:option>
                                        <stripes:option value="showUpdateCustomerPassword"><fmt:message key="edit.customer.password"/></stripes:option>
                                        <stripes:option value="retrieveTroubleTickets"><fmt:message key="trouble.tickets"/></stripes:option>
                                        <stripes:option value="retrieveNotes"><fmt:message key="view.notes"/></stripes:option>
                                        <stripes:option value="retrieveNoteTypes"><fmt:message key="attach.note"/></stripes:option>
                                        <stripes:option value="manageAddresses"><fmt:message key="manage.addresses"/></stripes:option>   
                                    </c:if>
                                    <c:if test="${s:getPropertyWithDefault('env.sep.mnp.enabled','false') == 'true'}"> 
                                        <stripes:option value="showCreateCustomerPortInRequest"><fmt:message key="customer.port.into.smile"/></stripes:option>
                                        <stripes:option value="showEmergencyRestore"><fmt:message key="customer.port.emergency.restore"/></stripes:option>
                                        <stripes:option value="showCreateRingFenceRequest"><fmt:message key="create.ring.fence"/></stripes:option>
                                        <stripes:option value="searchPortOrder"><fmt:message key="view.customer.port.orders"/></stripes:option>
                                        <stripes:option value="showSearchPortOrder"><fmt:message key="search.porting.order"/></stripes:option>
                                    </c:if>
                                    <stripes:option value="showChangeAccountManagerCustomer"><fmt:message key="change.account.manager.customer"/></stripes:option>
                                    <stripes:option value="showEditPhotographs"><fmt:message key="manage.photographs"/></stripes:option>
                                    <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                                        <stripes:option value="showNextStepAfterAddingCustomer">Next Steps Wizard</stripes:option>
                                        <stripes:option value="showTrackSession">Track Customer</stripes:option>
                                        <stripes:option value="showSendCustomerEmail">Send Email</stripes:option>
                                    </c:if><!-- End of ICP check roles -->
                                    <stripes:option value="showManageCustomerRoles"><fmt:message key="manage.customer.roles"/></stripes:option>
                                    <stripes:option value="showUpdateCustomerPermission"><fmt:message key="edit.customer.permission"/></stripes:option>
                                    <% if (BaseUtils.getBooleanProperty("env.allow.sellers.update", false)) {%>                                        
                                             <stripes:option value="showUpdateCustomerSellers"><fmt:message key="edit.customer.sellers"/></stripes:option>                                        
                                    <% } %> 
                                </stripes:select>
                                <stripes:submit name="performEntityAction" />
                            </stripes:form>
                        </td>
                    </tr>                    
                </c:forEach> 
            </c:if>

            <c:if test="${isDeliveryPerson}">
                <tr>
                    <th><fmt:message key="id"/></th>
                    <th><fmt:message key="first.name"/></th>
                    <th><fmt:message key="last.name"/></th>
                    <th><fmt:message key="email"/></th>
                    <th>Complete KYC</th>
                    <th>Add Product</th>
                </tr>
                <c:forEach items="${actionBean.customerList.customers}" var="customer" varStatus="loop">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>${customer.customerId}</td>
                        <td>${customer.firstName}</td>
                        <td>${customer.lastName}</td> 
                        <td>${s:breakUp(customer.emailAddress,40)}</td>
                        <stripes:form action="/Customer.action">
                        <input type="hidden" name="customerQuery.customerId" value="${customer.customerId}"/>
                        <input type="hidden" name="customer.customerId" value="${customer.customerId}"/>
                        <td>
                            <stripes:submit name="showEditPhotographs"/>
                        </td>
                        <td>
                            <stripes:submit name="showAddProductWizard"/>
                        </td>
                    </stripes:form>
                </tr>                    
            </c:forEach> 
        </c:if>
    </table>
</c:if>     

</stripes:layout-component>    
</stripes:layout-render>

