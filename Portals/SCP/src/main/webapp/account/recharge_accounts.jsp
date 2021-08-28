<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="recharge.account"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}"> 
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function () {
                makeMenuActive('Recharge_AccountsPGWPage');
            <c:if test="${ getPropertyWithDefault('env.scp.kyc.warning', false) && actionBean.KYCUnverified}">
                alert('Our records show that your registration on our network is incomplete. Kindly go to any of our shops or kiosks to complete your registration. You can reach us on our toll free number 08004444444, email: customercare@smile.com.ng or smileteam-ng@smilecoms.com and our social media platforms for more enquiries.')
            </c:if>
            }
        </script>

    </stripes:layout-component>
    <stripes:layout-component name="contents">     
        <div style="margin-top: 10px;" class="sixteen columns">

            <c:if test="${1==1}">
                <c:choose>
                    <c:when test="${s:getListSize(actionBean.customer.serviceInstances) > 0}">

                        <table class="greentbl" width="100%">
                            <c:set var="accountListSize" value="${s:getListSize(actionBean.accountList.accounts)}"/>
                            <stripes:form action="/PaymentGateway.action" autocomplete="off">
                                <tr>
                                    <th><fmt:message key="scp.account.id"/></th>
                                    <th><fmt:message key="scp.friendly.name"/></th>
                                    <th><fmt:message key="available.balance"/></th>
                                    <th><div>Amount</div></th>
                                        <c:if test="${accountListSize > 1}">
                                        <th>Choose</th>
                                        </c:if>
                                </tr>

                                <c:set var="SIs" value="${actionBean.serviceInstanceList.serviceInstances}"/>
                                <c:set var="PIList" value="${actionBean.productInstanceList.productInstances}"/>
                                <c:set var="counter" value="${1}" />

                                <c:forEach items="${actionBean.accountList.accounts}" var="account" varStatus="loop">
                                    <tr class="${loop.count mod 2 == 0 ? "odd" : "even"}">
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
                                            <font style="font-size:12pt; font-weight: bold; text-align: left">Enter amount</font> 
                                            <input size="8" type="text" name="paymentGatewayTransactionData.balanceTransferLines[${loop.index}].amountInCents" onkeyup="validate(this, '^[0-9]{1,20}\.[0-9]{2,2}$', '')" ng-init="airtimeAmountInMajorUnitsArray[${loop.index}] = 0" ng-model="airtimeAmountInMajorUnitsArray[${loop.index}]" ng-change="airtimeAmountChanged()" style="font-weight: bold; font-size: 12pt; text-align: right"/>
                                            <input type="hidden" name="paymentGatewayTransactionData.balanceTransferLines[${loop.index}].targetAccountId" value="${account.accountId}"/>
                                        </td>
                                        <c:choose>
                                            <c:when test="${accountListSize > 1}">
                                                <td>
                                                    <input type="radio" value="${account.accountId}" name="account.accountId"/>
                                                </td>
                                            </c:when>
                                            <c:otherwise>
                                            <div><input type="hidden" value="${account.accountId}" name="account.accountId"/></div>
                                            </c:otherwise>
                                        </c:choose>

                                    </tr>
                                </c:forEach>
                                <tr>
                                    <td colspan="3"></td>
                                    <td colspan="2" style="font-weight: bold; font-size: 13pt" ng-show="airtimeAmountInMajorUnits >= 0">
                                        Total: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font style="background-color: #75B343;">{{airtimeAmountInMajorUnits| currency:"${s:getProperty('env.locale.currency.majorunit')}"}} </font>
                                    </td>
                                    <td  colspan="2" style="background-color: red; font-weight: bold; font-size: 13pt" ng-show="airtimeAmountInMajorUnits < 0">

                                    </td>
                                </tr>
                                <tr>
                                    <td colspan="3"></td>
                                    <td colspan="2">
                                        <input type="hidden" name="paymentGatewayTransactionData.paymentGatewayCode" value="Diamond"/>
                                        <input type="hidden" name="paymentGatewayTransactionData.transactionAmountCents" value="{{airtimeAmountInMajorUnits}}"/>
                                        <input class="button_confirm" type="submit" name="confirmBankTransaction" value="Confirm"/>
                                    </td>
                                </tr>
                            </stripes:form>
                        </table>

                    </c:when>
                    <c:otherwise>
                        <p><fmt:message key="no.service.instances.account"/></p>
                    </c:otherwise>
                </c:choose>                    
            </c:if>

        </div>
    </stripes:layout-component>
</stripes:layout-render>
