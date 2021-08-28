<%-- 
    Document   : add_unit_credit
    Created on : Jan 26, 2012, 7:07:58 PM
    Author     : lesiba
--%>

<%@include file="../include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="scp.recharge"/>
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

            <table>
                <tr>
                    <td>
                        <strong style="color:#8ecb32;">Your Smile account&nbsp;&nbsp;&nbsp;</strong>
                    </td>
                    <td>
                        <b>${actionBean.account.accountId}</b>
                    </td>
                    <td>
                        <div ng-init="getAccountsSIMs(${actionBean.account.accountId})">
                        
                            <form name="myForm">
                                <label ng-repeat="item in SIMs.accountSIMs.products">
                                  &nbsp;&nbsp; <input type="radio" name="pInstanceID" ng-model="$parent.productInstanceID" ng-value="{{item.productInstanceId}}">
                                  <span style="font-weight: bold; font-size:16px;">SIM: {{item.ICCID}} -- {{item.friendlyName}}</span>
                                </label><br/>
                            </form>
                        </div>
                    </td>
                </tr>
                
                <tr>
                    <td>
                        <strong style="color:#8ecb32;">Take your pick&nbsp;&nbsp;&nbsp;</strong>
                    </td>
                    <td>
                        <b><button class="recharge_toggle_btn" ng-click="toggleAirtime()">SmileAirtime</button></b>
                    </td>
                    <td>
                        <b><button class="recharge_toggle_btn" ng-click="toggleUnitCredit()">SmileData</button></b>
                    </td>
                </tr>
                <tr>
                    <td colspan="3"><br/></td> 
                </tr>
            </table>
            <div ng-show="airtimeVisible">
                <c:choose>
                    <c:when test="${s:getListSize(s:getPropertyAsList('env.scp.paymentgateway.makepayment.allowed.customers')) > 0 }">
                        <c:set var="customerIdIsSet" value="false"/>
                        <c:forEach items="${s:getPropertyAsList('env.scp.paymentgateway.makepayment.allowed.customers')}" var="customerId" varStatus="loop">
                            <c:if test="${customerId eq actionBean.customer.customerId}">
                                <c:set var="customerIdIsSet" value="true"/>
                            </c:if>
                        </c:forEach>
                        <c:if test="${customerIdIsSet}">
                            <table class="greentbl" width="100%">
                                <tr>
                                    <th colspan="4"><fmt:message key="scp.buy.smile.airtime"/></th>
                                </tr>
                                <tr class="even" style="border: none;">
                                    <td colspan="3">
                                        <font style="text-align: left">Enter amount</font> 
                                    </td>
                                    <td align="right">
                                        <input  width="100%" type="text" name="airtimeAmnt" onkeyup="validate(this, '^[0-9]{1,20}\.[0-9]{2,2}$', '')" ng-init="airtimeAmountInMajorUnits = ''" ng-model="airtimeAmountInMajorUnits" ng-change="airtimeAmountChanged()" style="font-weight: bold; font-size: 12pt; text-align: right"/>
                                    </td>
                                </tr>

                                <tr>
                                    <td colspan="4" align="right" style="font-weight: bold; font-size: 13pt;" ng-show="amountInMajorUnits >= 0">
                                        Total: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font style="background-color: #75B343;">{{amountInMajorUnits| currency:"${s:getPropertyWithDefault('env.portal.locale.currency.notation','')}"}} </font>
                                    </td>
                                    <td colspan="4" align="right" style="background-color: red; font-weight: bold; font-size: 13pt" ng-show="amountInMajorUnits < 0">

                                    </td>
                                </tr>
                                <tr ng-show="amountInMajorUnits > 0">

                                    <td colspan="4" align="right">
                                        <stripes:form action="/Account.action" autocomplete="off">
                                            <input type="hidden" name="sale.saleLines[0].lineNumber" value="1"/>
                                            <input type="hidden" name="sale.saleLines[0].inventoryItem.itemNumber" value="AIR1004"/>
                                            <input type="hidden" name="sale.saleLines[0].inventoryItem.serialNumber" value="AIRTIME"/>
                                            <input type="hidden" name="sale.saleLines[0].inventoryItem.description" value="AIRTIME"/>
                                            <input type="hidden" name="sale.saleLines[0].lineTotalCentsIncl" value="{{amountInMajorUnits}}"/>
                                            <input type="hidden" name="sale.saleLines[0].quantity" value="{{amountInMajorUnits}}"/>
                                            <input type="hidden" name="sale.saleTotalCentsIncl" value="{{amountInMajorUnits}}"/>
                                            <input type="hidden" name="sale.recipientCustomerId" value="${actionBean.customer.customerId}"/>
                                            <input type="hidden" name="sale.recipientAccountId" value="${actionBean.account.accountId}"/>
                                            <input class="button_purchase" type="submit" name="confirmAirtimeTransaction" value="Purchase"/>
                                        </stripes:form>
                                    </td>
                                </tr>

                            </table>
                        </c:if>
                        <c:if test="${!customerIdIsSet}">

                        </c:if>
                    </c:when>
                    <c:otherwise>

                        <c:set var="isAllowedAirtimePurchase" value="false"/>
                        <c:forEach items="${s:getPropertyAsList('env.scp.payment.methods')}" var="paymentTypeProps" varStatus="loopP">
                            <c:set var="ptProps" value="${fn:split(paymentTypeProps,'|')}"/>
                            <c:set var="paymentTypeIntegration" value="${ptProps[1]}"/>
                            <c:set var="AllowedCustomerIdOnGatewayConfig" value="AllowedCustomerIdOnGateway=All"/>
                            <c:if test="${paymentTypeIntegration == 'SCA'}">
                                <c:set var="AllowedCustomerIdOnGatewayConfig" value="${ptProps[3]}"/>
                                <c:if test="${AllowedCustomerIdOnGatewayConfig == 'AllowedCustomerIdOnGateway=All'}">
                                    <c:set var="isAllowedAirtimePurchase" value="true"/>
                                </c:if>
                                <c:if test="${AllowedCustomerIdOnGatewayConfig != 'AllowedCustomerIdOnGateway=All'}">
                                    <c:set var="AllowedCustomerIdOnGatewayConfigData" value="${fn:split(AllowedCustomerIdOnGatewayConfig,'=')}"/>
                                    <c:set var="AllowedCustomerIdArray" value="${fn:split(AllowedCustomerIdOnGatewayConfigData[1],',')}"/>
                                    <c:forEach items="${fn:split(AllowedCustomerIdOnGatewayConfigData[1],',')}" var="allowedCustomerId" varStatus="loopGW">
                                        <c:if test="${allowedCustomerId eq actionBean.customer.customerId}">
                                            <c:set var="isAllowedAirtimePurchase" value="true"/>
                                        </c:if>
                                    </c:forEach>
                                </c:if>
                            </c:if>
                        </c:forEach>
                        <c:if test="${isAllowedAirtimePurchase}">
                            <table class="greentbl" width="100%">
                                <tr>
                                    <th colspan="4"><fmt:message key="scp.buy.smile.airtime"/></th>
                                </tr>
                                <tr class="even" style="border: none;">
                                    <td colspan="3">
                                        <font style="text-align: left">Enter amount</font> 
                                    </td>
                                    <td align="right">
                                        <input  width="100%" type="text" name="airtimeAmnt" onkeyup="validate(this, '^[0-9]{1,20}\.[0-9]{2,2}$', '')" ng-init="airtimeAmountInMajorUnits = ''" ng-model="airtimeAmountInMajorUnits" ng-change="airtimeAmountChanged()" style="font-weight: bold; font-size: 12pt; text-align: right"/>
                                    </td>
                                </tr>

                                <tr>
                                    <td colspan="4" align="right" style="font-weight: bold; font-size: 13pt;" ng-show="amountInMajorUnits >= 0">
                                        Total: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font style="background-color: #75B343;">{{amountInMajorUnits| currency:"${s:getPropertyWithDefault('env.portal.locale.currency.notation','')}":0}} </font>
                                    </td>
                                    <td colspan="4" align="right" style="background-color: red; font-weight: bold; font-size: 13pt" ng-show="amountInMajorUnits < 0">

                                    </td>
                                </tr>
                                <tr ng-show="amountInMajorUnits > 0">
                                    <td colspan="4" align="right">
                                        <stripes:form action="/Account.action" autocomplete="off">
                                            <input type="hidden" name="sale.saleLines[0].lineNumber" value="1"/>
                                            <input type="hidden" name="sale.saleLines[0].inventoryItem.itemNumber" value="AIR1004"/>
                                            <input type="hidden" name="sale.saleLines[0].inventoryItem.serialNumber" value="AIRTIME"/>
                                            <input type="hidden" name="sale.saleLines[0].inventoryItem.description" value="AIRTIME"/>
                                            <input type="hidden" name="sale.saleLines[0].lineTotalCentsIncl" value="{{amountInMajorUnits}}"/>
                                            <input type="hidden" name="sale.saleLines[0].quantity" value="{{amountInMajorUnits}}"/>
                                            <input type="hidden" name="sale.saleTotalCentsIncl" value="{{amountInMajorUnits}}"/>
                                            <input type="hidden" name="sale.recipientCustomerId" value="${actionBean.customer.customerId}"/>
                                            <input type="hidden" name="sale.recipientAccountId" value="${actionBean.account.accountId}"/>
                                            <input class="button_purchase" type="submit" name="confirmAirtimeTransaction" value="Purchase"/>
                                        </stripes:form>
                                    </td>
                                </tr>
                            </table>
                            <br/>
                        </c:if>

                    </c:otherwise>
                </c:choose>

            </div>
            
            <div ng-show="unitCreditVisible">
                <c:if test="${!empty actionBean.unitCreditSpecificationList.unitCreditSpecifications}">

                <div id="bundle_list" style="display:block">

                    <c:forEach items="${actionBean.unitCreditSections}" var="sectionName" varStatus="loopSections">   

                        <table class="greentbl" width="100%">
                            <tr>
                                <th colspan="4"><fmt:message key="scp.unitcredit.section.${sectionName}"/></th>
                            </tr>
                            <tr>
                                <td width="100%">
                                        <table width="100%">
                                            <c:forEach items="${s:filterList(s:orderUnitCreditListByNamePopularity(actionBean.unitCreditSpecificationList.unitCreditSpecifications, sectionName),'getConfiguration','MySmileSection.*'.concat(sectionName))}" var="unitCreditSpecification" varStatus="loop">                                        
                                                <c:if test="${loop.count == 1 || loop.count == 5 || loop.count == 9 || loop.count == 13 || loop.count == 17}">
                                                    <tr width="99%">
                                                </c:if>
                                                        <td align="center">
                                                            
                                                            <div style="margin-left: 0px; margin-right: 0px;" class="four columns data-bundle-column">
                                                               <div class="circle ${s:getValueFromCRDelimitedAVPString(unitCreditSpecification.configuration, 'MySmileBubbleColor')}">
                                                                   <span class="circle-back"></span>
                                                                   <span class="data-bundle">
                                                                       <c:choose>
                                                                           <c:when test="${fn:toLowerCase(unitCreditSpecification.unitType) == 'byte' && !fn:contains(fn:toLowerCase(unitCreditSpecification.name), 'unlimited') && empty s:getValueFromCRDelimitedAVPString(unitCreditSpecification.configuration, 'MySmileDisplayNamePreference')}">
                                                                               <c:choose>
                                                                                   <c:when test="${unitCreditSpecification.units < (1024 * 1024 * 1024)}">
                                                                                       <span class="data-bundle">${s:displayVolumeAsStringInGB(unitCreditSpecification.units, 'byte')}<span class="data-bundle-size">MB</span></span>
                                                                                   </c:when>
                                                                                   <c:otherwise>
                                                                                       <span class="data-bundle">${s:displayVolumeAsStringInGB(unitCreditSpecification.units, 'byte')}<span class="data-bundle-size">GB</span></span>
                                                                                   </c:otherwise>
                                                                               </c:choose>
                                                                           </c:when>
                                                                           <c:otherwise>
                                                                               <c:choose>
                                                                                    <c:when test="${not empty s:getValueFromCRDelimitedAVPString(unitCreditSpecification.configuration, 'MySmileDisplayNamePreference')}">
                                                                                        <span class="text">${s:getValueFromCRDelimitedAVPString(unitCreditSpecification.configuration, 'MySmileDisplayNamePreference')}</span>
                                                                                    </c:when>
                                                                                    <c:otherwise>
                                                                                        <span class="text">${unitCreditSpecification.name}</span>
                                                                                    </c:otherwise>
                                                                                </c:choose>
                                                                           </c:otherwise>
                                                                       </c:choose>
                                                                       
                                                                   </span>
                                                               </div>
                                                                <%--<p class="subtitle"><strong>${unitCreditSpecification.name}</strong></p>--%>           
                                                                <p class="subtitle"><strong>${unitCreditSpecification.name}</strong></p>
                                                            </div>
                                                      
                                                            <div style="min-height: 10px;" class="three columns">
                                                                
                                                                <c:choose>
                                                                    <c:when test="${not empty s:getValueFromCRDelimitedAVPString(unitCreditSpecification.configuration, 'MySmileDisplayValidityAs')}">
                                                                        <span class="column-title">Validity: ${s:getValueFromCRDelimitedAVPString(unitCreditSpecification.configuration, 'MySmileDisplayValidityAs')}</span>
                                                                    </c:when>
                                                                    <c:otherwise>
                                                                        <span class="column-title">Validity: ${unitCreditSpecification.usableDays} days</span>
                                                                    </c:otherwise>
                                                                </c:choose>
                                                                <br/>
                                                                <span class="column-title price">Price: ${s:convertCentsToCurrencyShortWithCommaGroupingSeparator(unitCreditSpecification.priceInCents)}</span>
                                                                <br/>
                                                                <br/>
                                                                 <stripes:form action="/Account.action">
                                                                    <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/>
                                                                    <stripes:hidden name="purchaseUnitCreditRequest.accountId" value="${actionBean.account.accountId}"/>
                                                                    <input type="hidden" name="purchaseUnitCreditRequest.unitCreditSpecificationId" value="${unitCreditSpecification.unitCreditSpecificationId}"/>
                                                                    <stripes:hidden name="sale.recipientCustomerId" value="${actionBean.customer.customerId}"/>
                                                                    <input type="hidden" name="sale.recipientAccountId" value="${actionBean.account.accountId}"/>
                                                                    <stripes:hidden name="purchaseUnitCreditRequest.productInstanceId" value="{{productInstanceID}}"/>
                                                                    <input type="submit" class="button_recharge" name="provisionUnitCreditPaymentMethodPage" value="Recharge"/>
                                                                </stripes:form>
                                                            </div>
                                                           
                                                        </td>                
                                                        
                                                <c:if test="${loop.count == 4 || loop.count == 8 || loop.count == 12 || loop.count == 16 || loop.count == 20 || loop.last}">
                                                    </tr>
                                                </c:if>
                                            </c:forEach>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        <br/>
                        </c:forEach>
                    </div>
                </c:if>
                                        
            </div>
            
        </div>
    </stripes:layout-component>
</stripes:layout-render>