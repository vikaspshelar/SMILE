<%-- 
    Document   : add_unit_credit_select_account_sim
    Created on : May 26, 2015, 4:59:06 PM
    Author     : sabza
--%>
<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="recharge.select.account"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}"> 
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function () {
            makeMenuActive('Buy Smile Bundle');
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">  

        <div style="margin-top: 10px;" class="sixteen columns alpha">

            <c:choose>
                <c:when test="${s:getListSize(actionBean.customer.productInstances) > 1}">

                    <c:if test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit 
                                  || actionBean.customerQuery.productInstanceOffset != 0}">

                          <stripes:form action="/Account.action" onsubmit=" return alertValidationErrors();" autocomplete="off">
                              <table width="99%">
                                  <tr>
                                      <td align="left">
                                          Enter account to recharge <input type="text" class="frm_input" name="account.accountId" maxlength="10" ng-model="targetAccountId" ng-change="getTargetServiceInstanceUserName()" onkeyup="validate(this, '^[0-9]{10,10}$', '');"/><br/>
                                          <label ng-model="targetCustomerWithProduct">
                                              <div ng-show="targetCustomerWithProduct.customerWithProduct.productInstanceUserName != '' && targetCustomerWithProduct.customerWithProduct.productNames == null" ng-style="{ color:'red' }">
                                                  {{targetCustomerWithProduct.customerWithProduct.productInstanceUserName}}
                                              </div>
                                              <div ng-show="targetCustomerWithProduct.customerWithProduct.productNames != null && targetCustomerWithProduct.customerWithProduct.productInstanceUserName != null" ng-style="{ color:'green' }">
                                                  {{targetCustomerWithProduct.customerWithProduct.productInstanceUserName}} {{targetCustomerWithProduct.customerWithProduct.productNames}} <span><input class="general_go_btn" type="submit" name="showAddUnitCredits" value="GO"/></span>
                                              </div>
                                          </label>
                                      </td>
                                  </tr>
                              </table>
                          </stripes:form>

                          <table width="99%">
                              <tr>
                                  <td align="left">
                                      <c:if test="${actionBean.customerQuery.productInstanceOffset != 0}">
                                          <stripes:form action="/Account.action">
                                              <input type="submit" style="background-color: #75b343; border: medium none; border-radius: 17px; color: #ffffff; cursor: pointer; font-family: 'UniversLT67BoldCn'; font-size: 15px; height: 34px; line-height: 34px;text-transform: uppercase; margin-bottom: 15px; padding-left: 15px; padding-right: 15px;" name="previousAddUnitCreditMultipleAccountsPage" value="Back"/>
                                              <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                              <input type="hidden" name="customerQuery.productInstanceOffset" value="${actionBean.customerQuery.productInstanceOffset - actionBean.customerQuery.productInstanceResultLimit}" />
                                          </stripes:form>
                                      </c:if>
                                  </td>
                                  <td align="right">
                                      <c:if test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit}">
                                          <stripes:form action="/Account.action">
                                              <input type="submit" style="background-color: #75b343; border: medium none; border-radius: 17px; color: #ffffff; cursor: pointer; font-family: 'UniversLT67BoldCn'; font-size: 15px; height: 34px; line-height: 34px;text-transform: uppercase; margin-bottom: 15px; padding-left: 15px; padding-right: 15px;" name="nextAddUnitCreditMultipleAccountsPage" value="Next"/>
                                              <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                              <input type="hidden" name="customerQuery.productInstanceOffset" value="${actionBean.customerQuery.productInstanceOffset + actionBean.customerQuery.productInstanceResultLimit}" />
                                          </stripes:form>
                                      </c:if>
                                  </td>
                              </tr>
                          </table>
                    </c:if>

                    <table class="greentbl" width="100%">
                        <stripes:form action="/Account.action" autocomplete="off">
                            <tr>
                                <th><%--<fmt:message key="scp.recharge.radio.choose"/>--%></th>
                                <th><fmt:message key="scp.account.id"/></th>
                                <th><fmt:message key="scp.friendly.name"/></th>
                                <th><fmt:message key="scp.available.airtime.and.data"/></th>
                            </tr>

                            <c:set var="SIs" value="${actionBean.serviceInstanceList.serviceInstances}"/>
                            <c:set var="PIList" value="${actionBean.productInstanceList.productInstances}"/>
                            <c:set var="counter" value="${1}" />

                            <c:forEach items="${actionBean.accountList.accounts}" var="account" varStatus="loop">
                                <tr class="${loop.count mod 2 == 0 ? "odd" : "even"}">
                                    <td>
                                        <input type="radio" value="${account.accountId}" name="account.accountId" ng-click="payingAccountChanged(${loop.index}, ${account.accountId})"/>
                                    </td>
                                    <td>
                                        ${account.accountId}
                                    </td>

                                    <td>
                                        <table width="100%">
                                            <c:forEach items="${SIs}" var="servInstance" varStatus="loop2">
                                                <c:if test="${servInstance.accountId == account.accountId}">
                                                    <c:forEach items="${PIList}" var="prodInstance" varStatus="loop3">
                                                        <c:if test="${prodInstance.productInstanceId == servInstance.productInstanceId}">                                                            
                                                            <tr class="${counter mod 2 == 0 ? "even1" : "odd1"}"> 
                                                                <td>
                                                                    <c:set var="productSpec" value="${s:getProductSpecification(prodInstance.productSpecificationId)}"/>
                                                                    <c:set var="serviceSpec" value="${s:getServiceSpecification(servInstance.serviceSpecificationId)}"/>

                                                                    <c:if test="${prodInstance.friendlyName == ''}">
                                                                        <strong>${productSpec.name}</strong> -- ${serviceSpec.name} <br/> <fmt:message key="scp.service.phone.number"/>: ${s:getServiceInstancePhoneNumber(servInstance.serviceInstanceId)}
                                                                    </c:if>
                                                                    <c:if test="${prodInstance.friendlyName != ''}">
                                                                        <strong>${prodInstance.friendlyName}</strong> -- ${serviceSpec.name} <br/> <fmt:message key="scp.service.phone.number"/>: ${s:getServiceInstancePhoneNumber(servInstance.serviceInstanceId)}
                                                                    </c:if>
                                                                </td>
                                                            </tr>                                                           
                                                            <c:set var="counter" value="${counter+1}" />
                                                        </c:if>
                                                    </c:forEach>
                                                </c:if>
                                            </c:forEach>
                                            <c:set var="counter" value="${1}" />
                                        </table>
                                    </td>

                                    <td>
                                        ${s:convertCentsToCurrencyLongWithCommaGroupingSeparator(account.availableBalanceInCents)} <strong>/</strong> ${s:displayVolumeAsStringWithCommaGroupingSeparator(s:calculateBundleBalance(account.accountId), 'Byte')} <c:if test="${s:containsUnlimitedBundle(account.accountId)}"><fmt:message key="scp.unlimited.servicespectrigger.msg"/></c:if>
                                        </td>
                                    </tr>
                                <c:if test="${s:getListSize(SIs) > 1}">
                                    <c:set var="counterSIMs" value="${0}" />
                                    <c:forEach items="${actionBean.uniqueSIMsForAccount}" var="entry" varStatus="loop2">
                                        <c:if test="${entry.key eq account.accountId}">
                                            <c:forEach items="${entry.value}" var="iccId" varStatus="loop3">
                                                <c:set var="counterSIMs" value="${counterSIMs+1}" />
                                            </c:forEach>
                                        </c:if>
                                    </c:forEach>
                                    <c:choose>
                                        <c:when test="${counterSIMs > 1}">
                                            <tr ng-show="accountSIMsToDisplay[${account.accountId}] == ${account.accountId}">
                                                <td colspan="4">
                                                    <div>
                                                        <table class="greentbl" width="100%">
                                                            <tr>
                                                                <th colspan="3">Choose SIM to load bundle on</th>
                                                            </tr>
                                                            <c:forEach items="${actionBean.uniqueSIMsForAccount}" var="entry" varStatus="loop2">
                                                                <c:if test="${entry.key eq account.accountId}">
                                                                    <c:forEach items="${entry.value}" var="iccId" varStatus="loop3">
                                                                        <c:forEach items="${actionBean.serviceInstanceList.serviceInstances}" var="serviceInstance" varStatus="loop4">
                                                                            <c:if test="${s:getServiceInstanceICCID(serviceInstance.serviceInstanceId) eq iccId}">
                                                                                <c:forEach items="${actionBean.productInstanceList.productInstances}" var="prodInstance" varStatus="loop5">
                                                                                    <c:if test="${prodInstance.productInstanceId == serviceInstance.productInstanceId}">
                                                                                        <c:set var="productInstanceId" value="${prodInstance.productInstanceId}"/>
                                                                                        <c:set var="productSpec" value="${s:getProductSpecification(prodInstance.productSpecificationId)}"/>
                                                                                        <c:if test="${prodInstance.friendlyName != ''}">
                                                                                            <c:set var="friendlyName" value="${prodInstance.friendlyName}" />
                                                                                        </c:if>
                                                                                        <c:if test="${prodInstance.friendlyName == ''}">
                                                                                            <c:set var="friendlyName" value="${productSpec.name}" />
                                                                                        </c:if>
                                                                                    </c:if>
                                                                                </c:forEach>
                                                                            </c:if>
                                                                        </c:forEach>
                                                                        <tr  class="${loop2.count mod 2 == 0 ? "odd" : "even"}">
                                                                            <td>
                                                                                <input type="radio" name="productInstanceId_${account.accountId}" value="${productInstanceId}" checked ng-model="simProductInstanceId[${account.accountId}]" ng-click="simCardSelectedChanged(${account.accountId})"/>
                                                                            </td>
                                                                            <td>
                                                                                ${friendlyName}
                                                                            </td>
                                                                            <td>
                                                                                ${iccId}
                                                                            </td>
                                                                        </tr>
                                                                    </c:forEach>
                                                                </c:if>
                                                            </c:forEach>
                                                        </table>
                                                    </div>
                                                </td>
                                            </tr>
                                            <c:set var="counterSIMs" value="${1}" />
                                        </c:when>
                                        <c:otherwise>
                                            <tr ng-show="accountSIMsToDisplay[${account.accountId}] == ${account.accountId}" >
                                                <td colspan="4">
                                                    <c:forEach items="${SIs}" var="serviceInstance" varStatus="loopSIMSIs">
                                                        <c:if test="${serviceInstance.accountId == account.accountId}">
                                                            <div ng-init="singleServiceProductInstanceId[${account.accountId}] = ${serviceInstance.productInstanceId}">
                                                            </div>
                                                        </c:if>
                                                    </c:forEach>
                                                </td>
                                            </tr>
                                        </c:otherwise>
                                    </c:choose>

                                </c:if>

                            </c:forEach>

                            <tr>
                                <td colspan="4">
                                    <input type="hidden" ng-init="dynamicProductInstanceId = 0" name="productInstanceIdForSIM" value="{{dynamicProductInstanceId}}"/>
                                    <input class="button_confirm_to_proceed" type="submit" name="showAddUnitCredits" value="CONFIRM TO PROCEED"/>
                                </td>
                            </tr>

                        </stripes:form>
                    </table>
                    <c:if   test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit 
                                    || actionBean.customerQuery.productInstanceOffset != 0}">
                            <table width="99%">
                                <tr>
                                    <td align="left">
                                        <c:if   test="${actionBean.customerQuery.productInstanceOffset != 0}">
                                            <stripes:form action="/Account.action">
                                                <input type="submit" style="background-color: #75b343; border: medium none; border-radius: 17px; color: #ffffff; cursor: pointer; font-family: 'UniversLT67BoldCn'; font-size: 15px; height: 34px; line-height: 34px;text-transform: uppercase; margin-bottom: 15px; padding-left: 15px; padding-right: 15px;" name="previousAddUnitCreditMultipleAccountsPage" value="Back"/>
                                                <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                                <input type="hidden" name="customerQuery.productInstanceOffset" value="${actionBean.customerQuery.productInstanceOffset - actionBean.customerQuery.productInstanceResultLimit}" />
                                            </stripes:form>
                                        </c:if>
                                    </td>
                                    <td align="right">
                                        <c:if   test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit}">
                                            <stripes:form action="/Account.action">
                                                <input type="submit" style="background-color: #75b343; border: medium none; border-radius: 17px; color: #ffffff; cursor: pointer; font-family: 'UniversLT67BoldCn'; font-size: 15px; height: 34px; line-height: 34px;text-transform: uppercase; margin-bottom: 15px; padding-left: 15px; padding-right: 15px;" name="nextAddUnitCreditMultipleAccountsPage" value="Next"/>
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
                    <p><fmt:message key="no.service.instances.account"/></p>
                </c:otherwise>
            </c:choose>                    
        </div>
    </stripes:layout-component>
</stripes:layout-render>
