<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="com.smilecoms.commons.base.BaseUtils, com.smilecoms.commons.stripes.SmileActionBean"%>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="manage.customer"/>
</c:set>
<c:choose> 
    <c:when test="${actionBean.customer.classification != 'foreigner'}">
        <c:set var="customerClassType" value="${s:getDelimitedPropertyValueMapping('env.customer.classifications', actionBean.customer.classification)}"/>
    </c:when>
    <c:otherwise>
        <c:set var="customerClassType" value="Foreigner"/>
    </c:otherwise>
</c:choose>


<stripes:layout-render name="/layout/standard.jsp" title="${title}">

    <stripes:layout-component name="contents">
        
            <table class="entity_header">
                <tr>
                    <td>
                <fmt:message key="customer"/> ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                </td>
                <td align="right">
                <stripes:form action="/Customer.action">
                    <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                    <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                    <stripes:hidden name="customer.SSOIdentity" value="${actionBean.customer.SSOIdentity}"/>
                    <stripes:select name="entityAction">
                        <c:if test="${actionBean.isDeliveryPerson}">
                            <c:if test="${customerClassType != 'minor'}"> 
                                <!-- Do not allow adding of product to a minor customer -->
                                <stripes:option value="showAddProductWizard"><fmt:message key="add.product"/></stripes:option>
                            </c:if>
                        </c:if>
                        <c:if test="${!actionBean.isDeliveryPerson}">
                            <c:if test="${actionBean.customer.SCAContext.obviscated != 'ob'}">
                                <stripes:option value="editCustomer"><fmt:message key="edit"/></stripes:option>
                            </c:if>

                            <% if (BaseUtils.getBooleanProperty("env.pilot.nin.only.update", false)) {%>      
                            <stripes:option value="editCustomerNIN"><fmt:message key="edit.customer.nin"/></stripes:option>
                                <% } %>      
                            <c:if test="${customerClassType != 'minor'}"> 
                                <!-- Do not allow adding of product to a minor customer -->
                                <stripes:option value="showAddProductWizard"><fmt:message key="add.product"/></stripes:option>
                            </c:if>
                            <c:if test="${!actionBean.isIndirectChannelPartner}">
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
                            <c:if test="${!actionBean.isIndirectChannelPartner}">
                                <stripes:option value="showNextStepAfterAddingCustomer">Next Steps Wizard</stripes:option>
                                <stripes:option value="showTrackSession">Track Customer</stripes:option>
                                <stripes:option value="showSendCustomerEmail">Send Email</stripes:option>
                            </c:if>
                            <stripes:option value="showManageCustomerRoles"><fmt:message key="manage.customer.roles"/></stripes:option>
                            <stripes:option value="showUpdateCustomerPermission"><fmt:message key="edit.customer.permission"/></stripes:option>
                            <% if (BaseUtils.getBooleanProperty("env.allow.sellers.update", false)) {%>      
                                <stripes:option value="showUpdateCustomerSellers"><fmt:message key="edit.customer.sellers"/></stripes:option>
                            <% } %> 

                        </c:if>
                    </stripes:select>
                    <stripes:submit name="performEntityAction" />
                </stripes:form>
                </td>
                </tr>
            </table>

            <table class="clear" >
                <tr>
                    <td><b><fmt:message key="classification"/>:</b></td>
                    <td><fmt:message   key="classification.${actionBean.customer.classification}" /></td>
                </tr>
                <tr>
                    <td colspan="2"><b><fmt:message key="basic.data"/></b></td>
                </tr>
                <tr>
                    <td><fmt:message key="id"/>:</td>
                <td>${actionBean.customer.customerId}</td>
                </tr>
                <c:if test="${s:getPropertyWithDefault('env.customer.kycstatus.display', false)}">
                    <c:if test="${actionBean.customer.KYCStatus != 'V'}">
                        <tr class="red" style="white-space:nowrap;">
                            <td>KYC Status:</td>
                            <td><c:choose> 
                            <c:when test="${s:getPropertyWithDefault('env.customer.verify.with.nida', false)}">
                                <stripes:form action="/Customer.action">
                                    <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                                    <stripes:hidden name="customer.identityNumber" value="${actionBean.customer.identityNumber}"/>
                                    <table class="clear">
                                        <tr>
                                            <td>
                                        <fmt:message   key="kycstatus.${actionBean.customer.KYCStatus}" />
                                        </td>
                                        <td>
                                        <stripes:submit name="verifyExistingCustomerWithNIDAOrImmigration" onclick="onBack = true;"/>
                                        </td>
                                        </tr>    
                                    </table>
                                </stripes:form>
                            </c:when>
                            <c:otherwise>
                                <fmt:message   key="kycstatus.${actionBean.customer.KYCStatus}" />
                            </c:otherwise>
                        </c:choose>
                        </td>
                        </tr>
                    </c:if>
                    <c:if test="${actionBean.customer.KYCStatus == 'V'}">
                        <tr>
                            <td>KYC Status:</td>
                            <td><fmt:message   key="kycstatus.${actionBean.customer.KYCStatus}" /></td>
                        </tr>
                    </c:if>
                </c:if>              
                <tr>
                    <td><fmt:message key="created.date"/>:</td>
                <td>${s:formatDateLong(actionBean.customer.createdDateTime)}</td>
                </tr>           
                <c:choose> 
                    <c:when test="${s:getPropertyWithDefault('env.customer.national.id.mandatory', false)}">
                        <tr>
                            <td><fmt:message key="nin.verfication.status"/>:</td>
                        <c:choose> 
                            <c:when test="${actionBean.customer.isNinVerified == 'Y'}">
                                <td><fmt:message   key="nin.verified" /></td>
                            </c:when>
                            <c:when test="${actionBean.customer.isNinVerified == 'M'}">
                                <td><fmt:message   key="nin.mismached" /></td>
                            </c:when>
                            <c:when test="${actionBean.customer.isNinVerified == 'P'}">
                                <td><fmt:message   key="nin.partialy.verified" /></td>
                            </c:when>
                            <c:otherwise>
                                <td><fmt:message   key="nin.not.verified" /></td>
                            </c:otherwise>
                        </c:choose>
                        </tr>
                        <tr>
                            <td><fmt:message key="national.id.number"/>:</td>
                        <td>${actionBean.customer.nationalIdentityNumber}</td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                    </c:otherwise>
                </c:choose>   
                <c:if test="${customerClassType != 'minor'}"> 
                    <tr>
                        <td><fmt:message key="id.number"/>:</td>
                    <td>${actionBean.customer.identityNumber}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="id.number.type"/>:</td>
                    <td>
                    <fmt:message  key="document.type.${actionBean.customer.identityNumberType}"/>
                    </td>
                    </tr>
                </c:if>
                <c:if test="${actionBean.customer.identityNumberType == 'passport'}">
                    <tr id="passportExpiryDateRow">
                        <td><fmt:message key="passport.expiry.date"/>:</td>
                    <td>${actionBean.customer.passportExpiryDate}</td>
                    </tr> 
                </c:if>
                <c:set var="countryCode" value="${s:getProperty('env.locale.country.for.language.en')}"/>
                <c:if test="${actionBean.customer.nationality != countryCode}"> 
                    <c:if test="${s:getProperty('env.customer.visa.enabled')}">
                        <tr id="visaExpiryDateRow">
                            <td><fmt:message key="visa.expiry.date"/>:</td>
                        <td>${actionBean.customer.visaExpiryDate}</td>
                        </tr>
                    </c:if>
                </c:if>
                <tr>
                    <td>Title:</td>
                    <td>${actionBean.customer.title}</td>
                </tr>
                <tr>
                    <td><fmt:message key="first.name"/>:</td>
                <td>${actionBean.customer.firstName}</td>
                </tr>
                <tr>
                    <td><fmt:message key="middle.name"/>:</td>
                <td>${actionBean.customer.middleName}</td>
                </tr>
                <tr>
                    <td><fmt:message key="last.name"/>:</td>
                <td>${actionBean.customer.lastName}</td>
                </tr>
                <tr>
                    <td><stripes:label for="mothers.maiden.name"/>:</td>
                <td>${actionBean.customer.mothersMaidenName}</td>
                </tr>
                <tr>
                    <td><fmt:message key="ssoidentity"/>:</td>
                <td>${actionBean.customer.SSOIdentity}</td>
                </tr>
                <tr>
                    <td><fmt:message key="date.of.birth"/>:</td>
                <td>${actionBean.customer.dateOfBirth}</td>                    
                </tr>
                <tr>
                    <td><fmt:message key="gender"/>:</td>
                <td>${actionBean.customer.gender}</td>
                </tr>
                <tr>
                    <td><fmt:message key="language"/>:</td>
                <td>${actionBean.customer.language}</td>
                </tr>
                <tr>
                    <td><stripes:label for="nationality"/>:</td>
                <td>${actionBean.customer.nationality}</td>
                </tr>
                <tr>
                    <td><fmt:message key="opt.in.level"/>:</td>
                <td><fmt:message  key="opt.in.level.${actionBean.customer.optInLevel}"/></td>
                </tr>
                <tr>
                    <td><fmt:message key="email"/>:</td>
                <td>${actionBean.customer.emailAddress}</td>
                </tr>
                <tr>
                    <td><fmt:message key="alternative.contact.number.1"/>:</td>
                <td>${actionBean.customer.alternativeContact1}</td>
                </tr>
                <tr>
                    <td><fmt:message key="alternative.contact.number.2"/>:</td>
                <td>${actionBean.customer.alternativeContact2}</td>
                </tr>
                <tr>
                    <td><fmt:message key="status"/>:</td>
                <td>${actionBean.customer.customerStatus}</td>
                </tr>
                <tr>
                    <td>Signed up by:</td>
                    <td>
                <stripes:link href="/Customer.action" event="retrieveCustomer">
                    <stripes:param name="customer.customerId" value="${actionBean.customer.createdByCustomerProfileId}"/>
                    ${s:getCustomerName(actionBean.customer.createdByCustomerProfileId)}
                </stripes:link>
                </td>
                </tr>
                <tr>
                    <td>Referral Code:</td>
                    <td>${actionBean.customer.referralCode}</td>
                </tr>
                <tr>
                    <td><fmt:message key="account.manager.customer"/>:</td>
                <td>
                <c:choose>
                    <c:when test="${actionBean.customer.accountManagerCustomerProfileId > 0}">
                        <stripes:link href="/Customer.action" event="retrieveCustomer">
                            <stripes:param name="customer.customerId" value="${actionBean.customer.accountManagerCustomerProfileId}"/>
                            ${s:getCustomerName(actionBean.customer.accountManagerCustomerProfileId)}
                        </stripes:link>
                    </c:when>
                    <c:otherwise>
                        Not Assigned
                    </c:otherwise>
                </c:choose>
                </td>
                </tr>

                <c:if test="${!actionBean.verifier.isEmpty()}">
                    <tr>
                        <td style='vertical-align: top'>KYC Verify Log</td>
                        <td>${actionBean.verifier}</td>
                    </tr>
                </c:if>
            </table>
            <s:displayWhenReady event="retrieveStickyNoteListSnippet" href="/Customer.action" containerId="stickyNote" autoLoad="true">
                <stripes:param name="customer.customerId" value="${actionBean.customer.customerId}"/>
            </s:displayWhenReady>

            <s:displayWhenReady event="retrieveTTViaStream" href="/Customer.action" containerId="troubleTicket"  autoLoad="true">
                <stripes:param name="customer.customerId" value="${actionBean.customer.customerId}"/>
            </s:displayWhenReady>                

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
                        <td>
                            <b><fmt:message key="customer.addresses"/>:</b>
                        </td>
                    </tr>
                    <c:forEach items="${actionBean.customer.addresses}" varStatus="loop">                    
                        <tr>
                            <td style='vertical-align: top; margin-top:auto'>
                                ${actionBean.customer.addresses[loop.index].type}
                            </td>
                            <td style='vertical-align: top; margin-top:auto'>
                                <table class="clear" style='vertical-align: top; margin-top:auto'>
                                    <tr>
                                        <td><fmt:message key="address.line1"/>:</td>
                            <td>
                                ${actionBean.customer.addresses[loop.index].line1}
                            </td>
                        </tr>                        
                        <tr>
                            <td><fmt:message key="address.line2"/>:</td>
                        <td>
                            ${actionBean.customer.addresses[loop.index].line2}
                        </td>
                        </tr>

                        <tr>
                            <td><fmt:message key="town"/>:</td>
                        <td>
                            ${actionBean.customer.addresses[loop.index].town}
                        </td>
                        </tr>

                        <tr>
                            <td><fmt:message key="zone"/>:</td>
                        <td>
                            ${actionBean.customer.addresses[loop.index].zone}
                        </td>
                        </tr>
                        <tr>
                            <td><fmt:message key="state"/>:</td>
                        <td>
                            ${actionBean.customer.addresses[loop.index].state}
                        </td>
                        </tr>

                        <tr>
                            <td><fmt:message key="country"/>:</td>
                        <td>
                            ${actionBean.customer.addresses[loop.index].country}
                        </td>
                        </tr>
                        <c:if test="${s:getProperty('env.postalcode.display')}">
                            <tr>
                                <td><fmt:message key="code"/>:</td>
                            <td>
                                ${actionBean.customer.addresses[loop.index].code}
                            </td>
                            </tr>
                        </c:if>
                </table>
                </td>
                </tr>
                </c:forEach>
                <tr>
                    <td><fmt:message key="user.group"/>:</td>
                <td>
                <c:forEach items="${actionBean.customer.securityGroups}" var="usergroup" varStatus="loop">
                    ${usergroup}<br/>
                </c:forEach>
                </td>
                </tr>
                <tr>
                    <td>Outstanding Terms and Conditions:</td>
                    <td>
                <c:forEach items="${actionBean.customer.outstandingTermsAndConditions}" var="terms" varStatus="loop">
                    ${terms}<br/> 
                </c:forEach>
                </td> 
                </tr> 
                <tr>
                    <td><fmt:message key="warehouse.id"/>:</td>
                <td>${actionBean.customer.warehouseId}</td>
                </tr>
                <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                    <tr>
                        <td colspan="2">
                    <s:displayWhenReady event="retrieveImagesSnippet" href="/Customer.action" containerId="images"  autoLoad="false" displayMessage="Click here to view attached documents">
                        <stripes:param name="customer.customerId" value="${actionBean.customer.customerId}"/>
                    </s:displayWhenReady>                        
                    </td>
                    </tr>
                </c:if>
                </table>
            </div>


            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="customers.roles"/></b></td>
                </tr>
            </table>
            <table class="green" width="99%">                                
                <c:if test="${1==1}">
                    <c:choose>
                        <c:when test="${s:getListSize(actionBean.customer.customerRoles) > 0}">
                            <tr>
                                <th>Org Id</th>
                                <th><fmt:message key="organisation.name"/></th>
                            <th><fmt:message key="role.name"/></th>
                            <th><fmt:message key="view.organisation"/></th>
                            </tr>
                            <c:forEach items="${actionBean.customer.customerRoles}" var="role" varStatus="loop">
                                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                    <td>${role.organisationId}</td>
                                    <td>${role.organisationName}</td>
                                    <td>${role.roleName}</td>
                                    <td>
                                <stripes:form action="/Customer.action" class="buttonOnly">
                                    <input type="hidden" name="organisation.organisationId" value="${role.organisationId}"/>
                                    <stripes:submit name="retrieveOrganisation"/>
                                </stripes:form>                                
                                </td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td colspan="3">
                            <fmt:message key="no.customer.roles"/>
                            </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                </c:if>
            </table>
            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="primary.contact.product.instances"/></b></td>
                </tr>
            </table>

            <c:choose>
                <c:when test="${s:getListSize(actionBean.customer.productInstances) > 0}">

                    <c:if   test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit 
                                    || actionBean.customerQuery.productInstanceOffset != 0}">
                        <table class="clear" width="99%">
                            <tr>
                                <td align="left">
                            <c:if   test="${actionBean.customerQuery.productInstanceOffset != 0}">
                                <stripes:form action="/Customer.action">
                                    <stripes:submit name="previousProductInstancePage"/>
                                    <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                    <input type="hidden" name="customerQuery.productInstanceOffset" value="${actionBean.customerQuery.productInstanceOffset - actionBean.customerQuery.productInstanceResultLimit}" />
                                </stripes:form>
                            </c:if>
                            </td>
                            <td align="right">
                            <c:if   test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit}">
                                <stripes:form action="/Customer.action">
                                    <stripes:submit name="nextProductInstancePage"/>
                                    <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                    <input type="hidden" name="customerQuery.productInstanceOffset" value="${actionBean.customerQuery.productInstanceOffset + actionBean.customerQuery.productInstanceResultLimit}" />
                                </stripes:form>
                            </c:if>
                            </td>
                            </tr>
                        </table>
                    </c:if>

                    <c:forEach items="${actionBean.customer.productInstances}" var="productInstance" varStatus="loop">
                        <table class="green" width="99%">                                
                            <c:set var="productSpec" value="${s:getProductSpecification(productInstance.productSpecificationId)}"/>
                            <tr>
                                <th colspan="6"  style="text-align:center; color: #000">
                                    ${productSpec.name} : ${productInstance.friendlyName} <br/> Phone:${s:getProductInstancePhoneNumber(productInstance.productInstanceId)} ICCID:${s:getProductInstanceICCID(productInstance.productInstanceId)} Last Device:${productInstance.lastDevice} ${s:getKYCStatus(productInstance)}
                                </th>
                            </tr>
                            <tr>
                                <th><fmt:message key="id"/></th>
                            <th><fmt:message key="provisioning.status"/></th>
                            <th><fmt:message key="organisation"/></th>
                            <th colspan="2">Product Owner</th>
                            <th colspan="2">
                            <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                                <fmt:message key="product.settings"/>
                            </c:if>
                            </th>
                            </tr>

                            <c:choose>
                                <c:when test="${s:isProductInstanceMissingKYC(productInstance)}">
                                    <tr class="red" ondblclick="document.location = '/sep/ProductCatalog.action;jsessionid=<%=request.getRequestedSessionId()%>?showEditProductInstanceSIMService=&productInstance.productInstanceId=${productInstance.productInstanceId}'">
                                </c:when>
                                <c:otherwise>
                                    <tr class="even">
                                </c:otherwise>
                            </c:choose>
                            <td>${productInstance.productInstanceId}</td>
                            <td>${productInstance.status}</td>
                            <td>${s:getEntryInList(actionBean.customer.customerRoles,'getOrganisationId',productInstance.organisationId).organisationName} </td>
                            <td colspan="2">
                            <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                <stripes:param name="customerQuery.customerId" value="${productInstance.customerId}"/>
                                ${s:getCustomerName(productInstance.customerId)}
                            </stripes:link>
                            </td>  
                            <td colspan="2">
                            <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                                <stripes:form action="/ProductCatalog.action" class="buttonOnly">
                                    <input type="hidden" name="productInstance.productInstanceId" value="${productInstance.productInstanceId}"/>
                                    <stripes:submit name="retrieveProductInstance"/>
                                </stripes:form>                                
                            </c:if><!-- End of ICP check roles -->
                            </td>
                            </tr>
                            <tr>
                                <th colspan="6" style="text-align:center">Products Service Instances</th>
                            </tr>
                            <tr>
                                <th><fmt:message key="id"/></th>
                            <th><fmt:message key="service.name"/></th>
                            <th><fmt:message key="provisioning.status"/></th>
                            <th>Service User</th>
                            <th>Account</th>
                            <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                                <th>
                                    Settings
                                </th>
                            </c:if>
                            </tr>
                            <c:forEach items="${productInstance.productServiceInstanceMappings}" var="mapping">
                                <c:set var="serviceInstance" value="${mapping.serviceInstance}" />                        
                                <c:set var="serviceSpec" value="${s:getServiceSpecification(serviceInstance.serviceSpecificationId)}"/>
                                <c:if test="${serviceInstance.status == 'TD'}">
                                    <tr class="greyout">
                                </c:if>
                                <c:if test="${serviceInstance.status != 'TD'}">
                                    <tr class="odd">
                                </c:if>
                                <td>${serviceInstance.serviceInstanceId} ${actionBean.simStatus.size()}</td>
                            <!--  
                            
                            THIS IS FOR TZ to show SIM verification Status.  Commented out till they are ready for the project to go live
                                <c:choose>
                                    <c:when test="${serviceSpec.name.trim().equalsIgnoreCase('Sim Card') && BaseUtils.getProperty('env.country.name').trim().equalsIgnoreCase('Tanzania')}" >
                                        <c:set var="matched" value="${false}" />
                                        <c:set var="receivedStatus" value="NotVerified" />   
                                        <c:forEach items="${actionBean.simStatus}" var="simStatuses">
                                           
                                            <c:if test="${s:getProductInstanceICCID(productInstance.productInstanceId).equalsIgnoreCase(simStatuses.key)}">                                                 
                                                <c:set var="matched" value="${true}" />  
                                                <c:set var="receivedStatus" value="${simStatuses.value}" /> 
                                                
                                            </c:if>
                                            
                                        </c:forEach>
                                        <c:choose>
                                            <c:when test="${!matched}">                                                
                                                <td>${serviceSpec.name } 
                                                    [ <stripes:link href="/SIM.action" event="showVerifyRegulatorSim" class="buttonOnly">                                                        
                                                       ${receivedStatus}
                                                    </stripes:link> ]
                                                </td>
                                            </c:when>
                                            <c:otherwise>
                                                <td>${serviceSpec.name } [ ${receivedStatus} ]</td>
                                            </c:otherwise>
                                        </c:choose>
                                        <c:set var="matched" value="${false}" />
                                        <c:set var="receivedStatus" value="NotVerified" />  
                                    </c:when>
                                    <c:otherwise>
                                        <td>${serviceSpec.name}</td>
                                    </c:otherwise>
                                </c:choose>      
                            
                            
                            -->  
                                <td>${serviceSpec.name}</td>
                                <td>${serviceInstance.status}</td>
                                <td>
                                <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                    <stripes:param name="customerQuery.customerId" value="${serviceInstance.customerId}"/>
                                    ${s:getCustomerName(serviceInstance.customerId)}
                                </stripes:link>
                                </td>
                                <td>
                                <stripes:link href="/Account.action" event="retrieveAccount"> 
                                    <stripes:param name="accountQuery.accountId" value="${serviceInstance.accountId}"/>
                                    ${serviceInstance.accountId}
                                </stripes:link>
                                </td>  
                                <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                                    <td>
                                    <stripes:form action="/ProductCatalog.action" class="buttonOnly">
                                        <input type="hidden" name="serviceInstance.serviceInstanceId" value="${serviceInstance.serviceInstanceId}"/>
                                        <stripes:submit name="retrieveServiceInstance"/>
                                    </stripes:form>                                        
                                    </td>
                                </c:if>
                                </tr>
                            </c:forEach>
                        </table>            
                        <br/>
                    </c:forEach>

                    <c:if   test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit 
                                    || actionBean.customerQuery.productInstanceOffset != 0}">
                        <table class="clear" width="99%">
                            <tr>
                                <td align="left">
                            <c:if   test="${actionBean.customerQuery.productInstanceOffset != 0}">
                                <stripes:form action="/Customer.action">
                                    <stripes:submit name="previousProductInstancePage"/>
                                    <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                    <input type="hidden" name="customerQuery.productInstanceOffset" value="${actionBean.customerQuery.productInstanceOffset - actionBean.customerQuery.productInstanceResultLimit}" />
                                </stripes:form>
                            </c:if>
                            </td>
                            <td align="right">
                            <c:if   test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit}">
                                <stripes:form action="/Customer.action">
                                    <stripes:submit name="nextProductInstancePage"/>
                                    <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                    <input type="hidden" name="customerQuery.productInstanceOffset" value="${actionBean.customerQuery.productInstanceOffset + actionBean.customerQuery.productInstanceResultLimit}" />
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