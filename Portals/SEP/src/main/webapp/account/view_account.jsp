<%@page import="com.smilecoms.commons.base.BaseUtils"%>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="manage.account"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>
                        <fmt:message key="account"/>: ${actionBean.account.accountId}                       
                        <stripes:link href="/Account.action" event="retrieveAccount">
                            <stripes:param name="accountQuery.accountId" value="${actionBean.account.accountId}"/>
                            <img src="images/refresh.png" width="25" height="25"/>
                        </stripes:link>
                    </td>
                    <td align="right">
                        <stripes:form action="/Account.action">
                            <stripes:select name="entityAction">
                                <stripes:option value="showTransactionHistory"><fmt:message key="transaction.history"/></stripes:option>                                
                                    <stripes:option value="showTPGWDownloadTransactionHistory"><fmt:message key="download.transaction.history"/></stripes:option>                                 
                                <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                                    <stripes:option value="showChangeStatus"><fmt:message key="edit.account.status"/></stripes:option>
                                </c:if>
                                <stripes:option value="showAddUnitCredits"><fmt:message key="add.unit.credits"/></stripes:option>
                                <stripes:option value="showTransfer"><fmt:message key="transfer.to.account"/></stripes:option>
                                <% if (BaseUtils.getProperty("env.country.name").equalsIgnoreCase("Nigeria")) {%>
                                    <stripes:option value="showScheduleTransactionHistory">Scheduled Transaction History</stripes:option>
                                <% } %>    
                                <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                                    <stripes:option value="showRedeemPrepaidStrip"><fmt:message key="redeem.strip"/></stripes:option>
                                    <stripes:option value="retrieveNotes"><fmt:message key="view.notes"/></stripes:option>
                                    <stripes:option value="retrieveNoteTypes"><fmt:message key="attach.note"/></stripes:option>
                                    <% if (BaseUtils.getBooleanProperty("env.show.voucher.management.menu")) {%>
                                    <stripes:option value="removePrepaidVoucherLockForAccount"><fmt:message key="remove.voucher.lock.account"/></stripes:option>
                                    <% } %>
                                    <stripes:option value="showAccountsFutureTransfers"><fmt:message key="future.transfers"/></stripes:option>
                                    <stripes:option value="showAccountsFutureUCPurchases"><fmt:message key="future.uc.purchases"/></stripes:option>
                                    <stripes:option value="showTransferGraph">Transfer Graph</stripes:option>
                                </c:if>
                                 <% if (BaseUtils.getBooleanProperty("env.allow.account.level.services.management",false)) {%>
                                    <stripes:option value="showManageAccountServices">Manage Account Services</stripes:option>
                                <% } %>
                            </stripes:select>
                            <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/>   
                            <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/> 
                            <stripes:hidden name="account.status" value="${actionBean.account.status}"/> 
                            <stripes:hidden name="accountHistoryQuery.accountId" value="${actionBean.account.accountId}"/>
                            <stripes:submit name="performEntityAction" />
                        </stripes:form>
                    </td>
                </tr>
            </table>
            <table class="clear">                
                <tr>
                    <td colspan="2"><b><fmt:message key="basic.data"/></b></td>
                </tr>
                <tr>
                    <td><fmt:message key="account.status"/>:</td>
                    <td><fmt:message key="account.status.${actionBean.account.status}"/></td>
                </tr>
                <tr>
                    <td><fmt:message key="sep.current.balance"/>:</td>
                    <td>${s:convertCentsToCurrencyLong(actionBean.account.currentBalanceInCents)}</td>
                </tr>
                <tr>
                    <td><fmt:message key="sep.available.balance"/>:</td>
                    <td>${s:convertCentsToCurrencyLong(actionBean.account.availableBalanceInCents)}</td>
                </tr>
                <tr>
                    <td colspan="2"><b><fmt:message key="account.service.instances"/></b></td>
                </tr>
            </table>

            <table class="green" width="99%">                                
                <c:if test="${s:getListSize(actionBean.account.serviceInstances) > 0}">                    
                    <tr>
                        <th>SI Id</th>
                        <th>Service Name</th>
                        <th>Customer</th>
                        <th>Info</th>
                        <th>Charging</th> 
                        <th>Rate</th> 
                            <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                            <th>Settings</th>
                            </c:if>
                    </tr>

                    <c:forEach items="${actionBean.account.serviceInstances}" var="serviceInstance" varStatus="loop">                        
                        <c:set var="serviceSpec" value="${s:getServiceSpecification(serviceInstance.serviceSpecificationId)}"/>
                        <c:set var="ratePlan" value="${s:getRatePlan(serviceInstance.ratePlanId)}"/>
                        <c:if test="${serviceInstance.status == 'TD'}">
                            <tr class="greyout">
                            </c:if>
                            <c:if test="${serviceInstance.status != 'TD'}">
                            <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            </c:if>
                            <td>${serviceInstance.serviceInstanceId} (<stripes:link href="/ProductCatalog.action" event="retrieveProductInstance">
                                    <stripes:param name="productInstance.productInstanceId" value="${serviceInstance.productInstanceId}"/>
                                    ${serviceInstance.productInstanceId}
                                </stripes:link>)
                            </td>
                            <td>
                                <stripes:link href="/ProductCatalog.action" event="retrieveServiceSpecification">
                                    <stripes:param name="serviceSpecification.serviceSpecificationId" value="${serviceSpec.serviceSpecificationId}"/>
                                    ${serviceSpec.name}
                                </stripes:link>                                
                            </td>    
                            <td>
                                <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                    <stripes:param name="customerQuery.customerId" value="${serviceInstance.customerId}"/>
                                    ${s:getServiceInstanceUserName(serviceInstance.serviceInstanceId)}
                                </stripes:link>
                            </td>
                            <td>Phone:${s:getServiceInstancePhoneNumber(serviceInstance.serviceInstanceId)}  ICCID:${s:getServiceInstanceICCID(serviceInstance.serviceInstanceId)}</td>
                            <td>
                                <stripes:link href="/ProductCatalog.action" event="retrieveRatePlan"> 
                                    <stripes:param name="ratePlan.ratePlanId" value="${ratePlan.ratePlanId}"/>
                                    ${ratePlan.name}
                                </stripes:link>
                            </td>
                            <td>
                                <div class="buttonOnly">
                                    <stripes:form action="/Account.action" class="buttonOnly">
                                        <stripes:hidden name="serviceInstance.serviceInstanceId" value="${serviceInstance.serviceInstanceId}"/>
                                        <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/> 
                                        <stripes:submit name="checkRate"/>                            
                                    </stripes:form>
                                </div>
                            </td>
                            <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                                <td>
                                    <stripes:form action="/ProductCatalog.action" class="buttonOnly">
                                        <stripes:hidden name="serviceInstance.serviceInstanceId" value="${serviceInstance.serviceInstanceId}"/>
                                        <stripes:submit name="retrieveServiceInstance"/>                            
                                    </stripes:form>
                                </td>
                            </c:if>
                        </tr>
                    </c:forEach>
                </c:if>
                <c:if test="${s:getListSize(actionBean.account.serviceInstances) == 0}">      
                    <tr>
                        <td>
                            <fmt:message key="no.service.instances"/>
                        </td>
                    </tr>
                </c:if>
            </table> 

            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="accounts.unit.credits"/></b></td>
                </tr>
            </table>
            <table class="green" width="99%">
                <c:if test="${s:getListSize(actionBean.account.unitCreditInstances) > 0}">
                    <tr>
                        <th><fmt:message key="unit.credit.id"/></th>
                        <th>PI Id</th>
                        <th><fmt:message key="unit.credit.name"/></th>
                        <th><fmt:message key="unit.credit.purchase.date"/></th>
                        <th>Start Date</th>
                        <th><fmt:message key="unit.credit.expiry.date"/></th>
                        <th><fmt:message key="current.units.remaining"/></th>
                        <th><fmt:message key="available.units.remaining"/></th>
                            <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                            <th>Edit</th>
                            <th>Delete</th>
                            <th>Split</th>
                            <th>View Sale</th>
                            </c:if>
                    </tr>
                    <c:forEach items="${actionBean.account.unitCreditInstances}" var="unitCreditInstance" varStatus="loop">

                        <c:choose>
                            <c:when test="${s:setContains(actionBean.ucCannotBeUsedSet, unitCreditInstance.unitCreditInstanceId)}">
                                <tr class="greyout">
                                </c:when>
                                <c:otherwise>
                                <tr class="${loop.count mod 2 == 0 ? 'even' : 'odd'}">
                                </c:otherwise>
                            </c:choose>

                            <td>${unitCreditInstance.unitCreditInstanceId}</td>
                            <td>${unitCreditInstance.productInstanceId}</td>
                            <td>
                                <c:if test="${fn:startsWith(unitCreditInstance.extTxId,'Split_')}">
                                    Part of : ${unitCreditInstance.name}
                                </c:if>
                                <c:if test="${!fn:startsWith(unitCreditInstance.extTxId,'Split_')}">
                                    ${unitCreditInstance.name}
                                </c:if>
                            </td>
                            <td>${s:formatDateLong(unitCreditInstance.purchaseDate)}</td>
                            <td>${s:formatDateLong(unitCreditInstance.startDate)}</td>
                            <td>
                                <c:if test="${!empty unitCreditInstance.endDate && unitCreditInstance.expiryDate != unitCreditInstance.endDate}">
                                    Ends: ${s:formatDateLong(unitCreditInstance.endDate)} Expires: &nbsp;
                                </c:if>${s:formatDateLong(unitCreditInstance.expiryDate)}
                            </td>
                            <c:set var="ucs" value="${s:getUnitCreditSpecification(unitCreditInstance.unitCreditSpecificationId)}"/>
                            <c:choose>
                                <c:when test="${fn:contains(ucs.configuration, 'SEPDisplayOnlyUsage=true')}">
                                    <td>${s:displayVolumeAsString(unitCreditInstance.unitsAtStart - unitCreditInstance.currentUnitsRemaining, ucs.unitType)} Used</td>
                                    <td></td>
                                </c:when>
                                <c:when test="${fn:contains(ucs.configuration, 'DisplayBalance=false')}">
                                    <td>NA</td>
                                    <td>NA</td>
                                </c:when>
                                <c:otherwise>
                                    <td>${s:displayVolumeAsString(unitCreditInstance.currentUnitsRemaining, unitCreditInstance.unitType)}</td>
                                    <td>${s:displayVolumeAsString(unitCreditInstance.availableUnitsRemaining, unitCreditInstance.unitType)}</td>
                                </c:otherwise>
                            </c:choose>

                            <c:if test="${actionBean.isIndirectChannelPartner == 'false'}">
                                <td>
                                    <stripes:form action="/Account.action" class="buttonOnly">
                                        <input type="hidden" name="unitCreditInstance.unitCreditInstanceId" value="${unitCreditInstance.unitCreditInstanceId}"/>
                                        <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/>
                                        <stripes:submit name="showEditUnitCreditInstance"/>
                                    </stripes:form> 
                                </td>
                                <td>
                                    <stripes:form action="/Account.action" class="buttonOnly">
                                        <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/> 
                                        <input type="hidden" name="unitCreditInstance.unitCreditInstanceId" value="${unitCreditInstance.unitCreditInstanceId}"/>
                                        <stripes:submit name="deleteUnitCreditInstance"/>
                                    </stripes:form> 
                                </td>
                                <td>
                                    <stripes:form action="/Account.action" class="buttonOnly">
                                        <input type="hidden" name="unitCreditInstance.unitCreditInstanceId" value="${unitCreditInstance.unitCreditInstanceId}"/>
                                        <stripes:submit name="showSplitUnitCreditInstance"/>
                                    </stripes:form> 
                                </td>
                                <td>
                                    <c:if test="${unitCreditInstance.saleLineId > 0}">
                                        <stripes:form action="/Account.action" class="buttonOnly">
                                            <input type="hidden" name="unitCreditInstance.saleLineId" value="${unitCreditInstance.saleLineId}"/>
                                            <input type="hidden" name="unitCreditInstance.accountId" value="${unitCreditInstance.accountId}"/>
                                            <stripes:submit name="getUnitCreditSale"/>
                                        </stripes:form> 
                                    </c:if>
                                </td>
                            </c:if>
                        </tr> 
                        </td>
                        </tr>
                    </c:forEach>
                </c:if>
                <c:if test="${s:getListSize(actionBean.account.unitCreditInstances) == 0}">
                    <tr>
                        <td>
                            <fmt:message key="no.unit.credits"/>
                        </td>
                    </tr>
                </c:if>
            </table>

            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="reservations"/></b></td>
                </tr>
            </table>
            <table class="green" width="99%">
                <c:if test="${s:getListSize(actionBean.account.reservations) > 0}">
                    <tr>
                        <th><fmt:message key="unit.credit.id"/></th>
                        <th><fmt:message key="session.id"/></th>
                        <th>Description</th>
                        <th><fmt:message key="unit.credit.reservation.date"/></th>
                        <th><fmt:message key="unit.credit.expiry.date"/></th>
                        <th><fmt:message key="reservation.unit.credit.units"/></th>
                        <th><fmt:message key="reservation.monetary.units"/></th>                        
                    </tr>
                    <c:forEach items="${actionBean.account.reservations}" var="reservation" varStatus="loop">
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            <td>${reservation.unitCreditInstanceId}</td>
                            <td>${s:breakUp(reservation.sessionId,20)}</td>
                            <td>${reservation.description}</td>
                            <td>${s:formatDateLong(reservation.reservationDate)}</td>
                            <td>${s:formatDateLong(reservation.expiryDate)}</td>
                            <td>${s:formatBigNumber(reservation.unitCreditUnits)}</td>
                            <td>${s:convertCentsToCurrencyLong(reservation.amountInCents)}</td>                              
                        </tr>
                    </c:forEach>
                </c:if>
                <c:if test="${s:getListSize(actionBean.account.reservations) == 0}">
                    <tr>
                        <td>
                            <fmt:message key="no.reservations"/>
                        </td>
                    </tr>
                </c:if>
            </table>
        </div>
    </stripes:layout-component>
</stripes:layout-render>

