<%@include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="my.accounts"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}"> 
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function () {
            makeMenuActive('Profile_AccountsPage');
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">
        <div style="margin-top: 10px;" class="sixteen columns alpha">

            <c:if test="${1==1}">
                <c:choose>
                    <c:when test="${s:getListSize(actionBean.customer.productInstances) > 0}">

                        <c:if test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit 
                                      || actionBean.customerQuery.productInstanceOffset != 0}">

                              <stripes:form action="/Account.action" onsubmit=" return alertValidationErrors();" autocomplete="off">
                                  <table width="99%">
                                      <tr>
                                          <td align="left">
                                              Enter account to manage <input type="text" class="frm_input" name="accountQuery.accountId" maxlength="10" ng-model="targetAccountId" ng-change="getTargetServiceInstanceUserName()" onkeyup="validate(this, '^[0-9]{10,10}$', '');"/><br/>
                                              <label ng-model="targetCustomerWithProduct">
                                                  <div ng-show="targetCustomerWithProduct.customerWithProduct.productInstanceUserName != '' && targetCustomerWithProduct.customerWithProduct.productNames == null" ng-style="{ color:'red' }">
                                                      {{targetCustomerWithProduct.customerWithProduct.productInstanceUserName}}
                                                  </div>
                                                  <div ng-show="targetCustomerWithProduct.customerWithProduct.productNames != null && targetCustomerWithProduct.customerWithProduct.productInstanceUserName != null" ng-style="{ color:'green' }">
                                                      {{targetCustomerWithProduct.customerWithProduct.productInstanceUserName}} {{targetCustomerWithProduct.customerWithProduct.productNames}} <span><input class="general_go_btn" type="submit" name="retrieveAccount" value="GO"/></span>
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
                                                  <input type="submit" style="background-color: #75b343; border: medium none; border-radius: 17px; color: #ffffff; cursor: pointer; font-family: 'UniversLT67BoldCn'; font-size: 15px; height: 34px; line-height: 34px;text-transform: uppercase; margin-bottom: 15px; padding-left: 15px; padding-right: 15px;" name="previousAccountsPage" value="Back"/>
                                                  <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                                  <input type="hidden" name="customerQuery.productInstanceOffset" value="${actionBean.customerQuery.productInstanceOffset - actionBean.customerQuery.productInstanceResultLimit}" />
                                              </stripes:form>
                                          </c:if>
                                      </td>
                                      <td align="right">
                                          <c:if test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit}">
                                              <stripes:form action="/Account.action">
                                                  <input type="submit" style="background-color: #75b343; border: medium none; border-radius: 17px; color: #ffffff; cursor: pointer; font-family: 'UniversLT67BoldCn'; font-size: 15px; height: 34px; line-height: 34px;text-transform: uppercase; margin-bottom: 15px; padding-left: 15px; padding-right: 15px;" name="nextAccountsPage" value="Next"/>
                                                  <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                                  <input type="hidden" name="customerQuery.productInstanceOffset" value="${actionBean.customerQuery.productInstanceOffset + actionBean.customerQuery.productInstanceResultLimit}" />
                                              </stripes:form>
                                          </c:if>
                                      </td>
                                  </tr>
                              </table>
                        </c:if>

                        <table class="greentbl" width="100%">
                            <tr>
                                <th><fmt:message key="scp.account.id"/></th>
                                <th><fmt:message key="scp.friendly.name"/> <%--/<fmt:message key="scp.service.phone.number"/>--%></th>
                                <th><fmt:message key="available.balance"/></th>
                                <th><fmt:message key="available.data.balance"/></th>
                                <th>Manage</th>
                                <th>Recharge</th>
                            </tr>

                            <c:set var="SIs" value="${actionBean.serviceInstanceList.serviceInstances}"/>
                            <c:set var="PIList" value="${actionBean.productInstanceList.productInstances}"/>
                            <c:set var="counter" value="${1}" />

                            <c:forEach items="${actionBean.accountList.accounts}" var="account" varStatus="loop">
                                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                    <td>
                                        ${account.accountId}
                                    </td>

                                    <td>
                                        <table class="greentbl" width="100%">
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
                                        ${s:convertCentsToCurrencyShortWithCommaGroupingSeparator(account.availableBalanceInCents)}
                                    </td>

                                    <td>
                                        ${s:displayVolumeAsStringWithCommaGroupingSeparator(s:calculateBundleBalance(account.accountId), 'Byte')} <c:if test="${s:containsUnlimitedBundle(account.accountId)}"><fmt:message key="scp.unlimited.servicespectrigger.msg"/></c:if>
                                    </td>

                                    <td>
                                        <stripes:link href="/Account.action" event="retrieveAccount">
                                            <stripes:param name="accountQuery.accountId" value="${account.accountId}"/>
                                            <img src="images/manage.png">
                                        </stripes:link>
                                    </td>
                                    
                                    <td>
                                        <stripes:form action="/Account.action">
                                            <stripes:hidden name="account.accountId" value="${account.accountId}"/>
                                            <input type="hidden" name="productInstanceIdForSIM" value="0"/>
                                            <input type="submit" class="button_recharge" name="showAddUnitCredits" value="Recharge"/>
                                        </stripes:form>
                                    </td>
                                </tr>
                            </c:forEach>
                        </table>

                        <c:if   test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit 
                                        || actionBean.customerQuery.productInstanceOffset != 0}">
                                <table width="99%">
                                    <tr>
                                        <td align="left">
                                            <c:if   test="${actionBean.customerQuery.productInstanceOffset != 0}">
                                                <stripes:form action="/Account.action">
                                                    <input type="submit" style="background-color: #75b343; border: medium none; border-radius: 17px; color: #ffffff; cursor: pointer; font-family: 'UniversLT67BoldCn'; font-size: 15px; height: 34px; line-height: 34px;text-transform: uppercase; margin-bottom: 15px; padding-left: 15px; padding-right: 15px;" name="previousAccountsPage" value="Back"/>
                                                    <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                                    <input type="hidden" name="customerQuery.productInstanceOffset" value="${actionBean.customerQuery.productInstanceOffset - actionBean.customerQuery.productInstanceResultLimit}" />
                                                </stripes:form>
                                            </c:if>
                                        </td>
                                        <td align="right">
                                            <c:if   test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit}">
                                                <stripes:form action="/Account.action">
                                                    <input type="submit" style="background-color: #75b343; border: medium none; border-radius: 17px; color: #ffffff; cursor: pointer; font-family: 'UniversLT67BoldCn'; font-size: 15px; height: 34px; line-height: 34px;text-transform: uppercase; margin-bottom: 15px; padding-left: 15px; padding-right: 15px;" name="nextAccountsPage" value="Next"/>
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
            </c:if>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
