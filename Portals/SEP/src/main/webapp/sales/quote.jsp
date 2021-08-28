<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="com.smilecoms.commons.base.BaseUtils, com.smilecoms.commons.stripes.SmileActionBean"%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="show.quote"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">


        <c:if test="${s:getPropertyWithDefault('env.customer.kycstatus.display', false) && actionBean.customer.KYCStatus == 'U'}">
            <script type="text/javascript">
                window.onload = function () {
                    alert("This customer is not fully KYC registered! Please update their fingerprints and photos and change the KYC status to Pending Verification.");
                }
            </script>
        </c:if>



        <table class="clear">     
            <c:if test="${actionBean.customer != null}">
                <tr>
                    <td colspan="2"><b>Customer</b></td>
                </tr>
                <tr>
                    <td>Customer Name:</td>
                    <td>${actionBean.customer.firstName} ${actionBean.customer.lastName}</td>
                </tr>
                <tr>
                    <td>Customer Email Address:</td>
                    <td>${actionBean.customer.emailAddress}</td>
                </tr>
                <c:if test="${s:getPropertyWithDefault('env.customer.kycstatus.display', false) && actionBean.customer.KYCStatus == 'U'}">
                    <tr class="red">
                        <td>KYC Status:</td>
                        <td><fmt:message   key="kycstatus.${actionBean.customer.KYCStatus}" /></td>
                    </tr>
                </c:if>
            </c:if>


            <c:if test="${actionBean.organisation != null}">
                <tr>
                    <td colspan="2"><b>Organisation</b></td>
                </tr>
                <tr>
                    <td>Organisation Name:</td>
                    <td>${actionBean.organisation.organisationName}</td>
                </tr>
                <c:if test="${!empty(actionBean.organisation.creditAccountNumber)}">
                    <tr>
                        <td>Credit Account Number:</td>
                        <td>${actionBean.organisation.creditAccountNumber}</td>
                    </tr>
                </c:if>
            </c:if>
            
            <% if (BaseUtils.getBooleanProperty("env.pos.sales.freelance.model.enabled", false)) {%>
               <c:if test="${actionBean.sale.freelancerCustomerId > 0}">
                   <tr>
                        <td colspan="2"><b>Freelance Sales Person:</b></td>
                   </tr>
                   <tr>
                        <td>Freelance Customer Id:</td>
                        <td>${actionBean.freelanceSalePersonFullName}</td>
                   </tr>
               </c:if>
            <% } %>        
            
            <c:if test="${actionBean.sale.recipientAccountId > 0}">
                <tr>
                    <td colspan="2"><b>Account Data</b></td>
                </tr>
                <tr>
                    <td>Account Id:</td>
                    <td>${actionBean.sale.recipientAccountId}</td>
                </tr>
            </c:if>
            <tr>
                <td>Promotion Code:</td>
                <td>${actionBean.sale.promotionCode}</td>
            </tr>
            <tr>
                <td>Invoice Expiry Date:</td>
                <td>${s:formatDateLong(actionBean.sale.expiryDate)}</td>
            </tr>


        </table>    

        <br/>

        <c:set var="curr" value="${actionBean.sale.tenderedCurrency}"/>

        <stripes:form action="/Sales.action" autocomplete="off">
            <table class="green" width="99%">            
                <tr>
                    <th>L</th>
                    <th>Item Number</th>
                    <th>Serial Number</th>
                    <th>Description</th>
                    <th>Comment</th>
                    <th>Provisioning Data</th>
                    <th>Unit Price Incl</th>
                    <th>Qty</th>
                    <th>Disc. Incl</th>
                    <th>Line Price Incl</th>
                </tr>
                <c:forEach items="${actionBean.sale.saleLines}" var="line" varStatus="loop"> 
                    <input type="hidden" name="sale.saleLines[${loop.index}].lineId" value="${line.lineId}"/>
                    <input type="hidden" name="sale.saleLines[${loop.index}].lineNumber" value="${line.lineNumber}"/>
                    <input type="hidden" name="sale.saleLines[${loop.index}].inventoryItem.itemNumber" value="${line.inventoryItem.itemNumber}"/>
                    <input type="hidden" name="sale.saleLines[${loop.index}].inventoryItem.description" value="${line.inventoryItem.description}"/>
                    <input type="hidden" name="sale.saleLines[${loop.index}].inventoryItem.currency" value="${line.inventoryItem.currency}"/>
                    <input type="hidden" name="sale.saleLines[${loop.index}].inventoryItem.boxSize" value="${line.inventoryItem.boxSize}"/>
                    <input type="hidden" name="sale.saleLines[${loop.index}].quantity" value="${line.quantity}"/>
                    <tr>
                        <c:choose>
                            <c:when test="${line.inventoryItem.serialNumber == '-COMBOUC-'}">
                                <td/>
                            </c:when>
                            <c:otherwise>
                               <td>${line.lineNumber}</td> 
                            </c:otherwise>
                        </c:choose>
                        <td>${line.inventoryItem.itemNumber}</td>
                        <td>
                            <c:choose>
                                <c:when  test="${line.inventoryItem.serialNumber == '-COMBOUC-'}">
                                    ${line.inventoryItem.serialNumber}
                                    <input  type="hidden" name="sale.saleLines[${loop.index}].inventoryItem.serialNumber" value="${line.inventoryItem.serialNumber}"/>
                                </c:when>
                                <c:otherwise>
                                    <c:choose >
                                        <c:when test="${fn:startsWith(line.inventoryItem.itemNumber,'KIT') || fn:startsWith(line.inventoryItem.itemNumber,'BUN') || fn:startsWith(line.inventoryItem.itemNumber,'AIR')}">
                                            <input type="text" readonly="true" name="sale.saleLines[${loop.index}].inventoryItem.serialNumber" value="${line.inventoryItem.serialNumber}"/>
                                        </c:when >
                                        <c:otherwise>
                                            <input type="text" name="sale.saleLines[${loop.index}].inventoryItem.serialNumber" value="${line.inventoryItem.serialNumber}"/>
                                        </c:otherwise>
                                    </c:choose>
                                </c:otherwise>
                            </c:choose>                                            
                        </td>
                        <td>${s:breakUp(line.inventoryItem.description,10)}</td>
                        <c:choose>
                            <c:when test="${fn:startsWith(line.inventoryItem.itemNumber,'BUNC')}">
                                <td/>
                            </c:when>
                            <c:otherwise>
                                <c:choose>
                                    <c:when test="${line.inventoryItem.itemNumber == 'KIT1425'}">
                                        <td><stripes:text  size="5" name="sale.saleLines[${loop.index}].comment" placeholder="Enter Receipt number:promoter name" value="${line.comment}" onkeyup="validate(this, '^[0-9]{1,12}$', 'emptynotok')"/></td>
                                    </c:when>
                                    <c:otherwise>
                                        <td><stripes:text  size="5" name="sale.saleLines[${loop.index}].comment" value="${line.comment}" /></td>
                                    </c:otherwise>
                                </c:choose>
                            </c:otherwise>
                        </c:choose>
                        <td>
                            <c:choose>
                                <c:when test="${fn:startsWith(line.inventoryItem.itemNumber,'BUNC')}"></c:when>
                                <c:otherwise>    
                                    <c:if test="${!fn:startsWith(line.inventoryItem.itemNumber,'KIT')}">
                                        <stripes:select name="sale.saleLines[${loop.index}].provisioningData" style="width: 160px;">
                                            <stripes:option value="Default">Default</stripes:option>
                                            <c:forEach items="${actionBean.provisioningDataOptions}" var="option">
                                                <stripes:option value="${option}">${option}</stripes:option>
                                            </c:forEach>
                                        </stripes:select>
                                    </c:if>

                                    <c:set var="unitCreditInfoProperty" value="env.bm.unit.credit.info.${line.inventoryItem.itemNumber}"/>
                                    <c:if test="${fn:startsWith(line.inventoryItem.itemNumber,'BUN') && s:getPropertyWithDefault(unitCreditInfoProperty,'null') != 'null'}">
                                        <br/>
                                        <br/>
                                        <hr style="border-color: #A4FD9B; font-weight: bold;"/>
                                        <b>Extra-Info</b>
                                        <stripes:select name="extraInfoProvisioningDataOptions[${line.inventoryItem.itemNumber}]" style="width: 160px;">
                                            <c:forEach items="${s:getPropertyAsList(unitCreditInfoProperty)}" var="option">
                                                <stripes:option value="${option}"><fmt:message key="unit.credit.info.${option}" /></stripes:option>
                                            </c:forEach>
                                        </stripes:select>
                                    </c:if>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <c:choose>
                            <c:when test="${fn:startsWith(line.inventoryItem.itemNumber,'BUNC')}">
                                <td align="right"/>
                                <td align="right"/>
                                <td align="right"/>
                                <td align="right"/>
                            </c:when>
                            <c:otherwise>
                                <td align="right">${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, line.inventoryItem.priceInCentsIncl)}</td>
                                <td align="right">${line.quantity}</td>
                                <td align="right">${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, line.lineTotalDiscountOnInclCents)}</td>
                                <td align="right">${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, line.lineTotalCentsIncl)}</td>
                            </c:otherwise>
                        </c:choose>
                    </tr>
                    <c:forEach items="${line.subSaleLines}" var="subline" varStatus="subloop"> 
                        <input type="hidden" name="sale.saleLines[${loop.index}].subSaleLines[${subloop.index}].lineId" value="${subline.lineId}"/>
                        <input type="hidden" name="sale.saleLines[${loop.index}].subSaleLines[${subloop.index}].lineNumber" value="${subline.lineNumber}"/>
                        <input type="hidden" name="sale.saleLines[${loop.index}].subSaleLines[${subloop.index}].inventoryItem.itemNumber" value="${subline.inventoryItem.itemNumber}"/>
                        <input type="hidden" name="sale.saleLines[${loop.index}].subSaleLines[${subloop.index}].inventoryItem.description" value="${subline.inventoryItem.description}"/>
                        <input type="hidden" name="sale.saleLines[${loop.index}].subSaleLines[${subloop.index}].inventoryItem.currency" value="${subline.inventoryItem.currency}"/>
                        <input type="hidden" name="sale.saleLines[${loop.index}].subSaleLines[${subloop.index}].quantity" value="${line.quantity}"/>
                        <tr>
                            <td></td>
                            <td>${subline.inventoryItem.itemNumber}</td>
                            <td>
                                <c:if test="${empty(subline.inventoryItem.serialNumber)}">
                                    <stripes:text name="sale.saleLines[${loop.index}].subSaleLines[${subloop.index}].inventoryItem.serialNumber" value="${subline.inventoryItem.serialNumber}"/>
                                </c:if>
                                <c:if test="${!empty(subline.inventoryItem.serialNumber)}">
                                    ${subline.inventoryItem.serialNumber}
                                    <input type="hidden" name="sale.saleLines[${loop.index}].subSaleLines[${subloop.index}].inventoryItem.serialNumber" value="${subline.inventoryItem.serialNumber}"/>
                                </c:if>
                            </td>
                            <td>${subline.inventoryItem.description}</td>
                            <td></td>
                            <td></td>
                            <td align="right"></td>
                            <td align="right">${subline.quantity}</td>
                            <td align="right"></td>
                            <td align="right"></td>
                        </tr>
                    </c:forEach>
                </c:forEach>
                <tr class="even">
                    <td colspan="10"></td>
                </tr>
                <tr>
                    <td colspan="4">Date: ${s:formatDateLong(actionBean.sale.saleDate)}</td>
                    <td colspan="2">Total Discount</td>
                    <td align="right" colspan="4">${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, actionBean.sale.saleTotalDiscountOnInclCents)}</td>
                </tr>
                <tr>
                    <td colspan="4">
                        <c:if test="${actionBean.sale.taxExempt}">
                            <b>This Sale Is Tax Exempt</b>
                        </c:if>
                    </td>
                    <td colspan="2">Tax</td>
                     <% if (BaseUtils.getDoubleProperty("env.sales.excise.tax.percent",0)<=0) {%>
                        <td align="right" colspan="4">${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, actionBean.sale.saleTotalTaxCents)}</td>
                    <% } %>
                    <% if (BaseUtils.getDoubleProperty("env.sales.excise.tax.percent",0)>0) {%>
                        <td align="right" colspan="4">${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, (actionBean.sale.saleTotalTaxCents-(actionBean.sale.saleTotalCentsExcl *(BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0)/100))))}</td>
                    <% } %> 
                </tr>
                <tr>
                    <td colspan="4"><b>Withholding Tax Rate is ${actionBean.sale.withholdingTaxRate}%</b></td>
                    <td colspan="2">Withholding Tax</td>
                    <td align="right" colspan="4">
                        <input type="hidden" name="sale.withholdingTaxCents" value="${s:convertCentsToLongRoundHalfEven(actionBean.sale.withholdingTaxCents)*100}"/>
                        ${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, actionBean.sale.withholdingTaxCents)}
                    </td>
                </tr>
                <% if (BaseUtils.getDoubleProperty("env.sales.excise.tax.percent",0)>0) {%>
                
                    <tr>
                        <td colspan="4"><b>Excise Tax Rate is ${BaseUtils.getDoubleProperty("env.sales.excise.tax.percent",0)}%</b></td>
                        <td colspan="2">Excise Tax</td>
                        <td align="right" colspan="4">
                            
                            ${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, (actionBean.sale.saleTotalCentsExcl *(BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0)/100)))}
                        </td>
                    </tr>
                
                <% } %>
                <tr>
                    <td colspan="4"></td>
                    <td colspan="2"><b>Total Due</b></td>
                    <td align="right" style="font-weight: bold; font-size: 13pt; text-align: right" colspan="4">
                        <b>${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, actionBean.sale.totalLessWithholdingTaxCents)}</b>
                    </td>
                </tr>
                <tr>
                    <td colspan="4"></td>
                    <td colspan="2">Total Sale</td>
                    <td align="right" colspan="4">
                        <input type="hidden" name="sale.saleTotalCentsIncl" value="${s:convertCentsToLongRoundHalfEven(actionBean.sale.saleTotalCentsIncl)*100}"/>
                        ${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, actionBean.sale.saleTotalCentsIncl)}
                    </td>
                </tr>
                <tr ng-show="pt == 'Cash'">
                    <td colspan="4"></td>
                    <td colspan="2"><b>Tendered Amount</b></td>
                    <td align="right" colspan="4">
                        <div style="font-weight: bold; font-size: 13pt; text-align: right">${s:getProperty('env.locale.currency.majorunit')}                            
                            <input size="8" type="text" name="sale.amountTenderedCents" ng-init="sale.amountTenderedCents = 0.00" ng-model="sale.amountTenderedCents" ng-change="tenderedAmountChanged()"  style="font-weight: bold; font-size: 12pt; text-align: right"/>
                        </div>
                    </td>
                </tr>
                <tr ng-show="pt == 'Cash'">
                    <td colspan="4"></td>
                    <td colspan="2"><b>Change</b></td>
                    <td colspan="4" align="right" style="background-color: lightgreen; font-weight: bold; font-size: 13pt" ng-show="sale.amountTenderedCents - ${s:convertCentsToLongRoundHalfEven(actionBean.sale.totalLessWithholdingTaxCents)} >= 0">
                        {{sale.amountTenderedCents - ${actionBean.sale.totalLessWithholdingTaxCents/100}| currency:"${curr}"}}
                    </td>
                    <td colspan="4" align="right" style="background-color: red; font-weight: bold; font-size: 13pt" ng-show="sale.amountTenderedCents - ${s:convertCentsToLongRoundHalfEven(actionBean.sale.totalLessWithholdingTaxCents)} < 0">

                    </td>
                </tr>
                <tr>
                    <td colspan="4"></td>
                    <td colspan="2"><b>Purchase Order Number</b></td>
                    <td div style="text-align: right" colspan="4">
                       <stripes:text name="sale.purchaseOrderData" size="15" maxlength="20"/>
                    </td>
                </tr>
                <tr ng-show="pt == 'Credit Note'">
                    <td colspan="4"></td>
                    <td colspan="2"><b>Credit Note Number</b></td>
                    <td div style="text-align: right" colspan="4">
                        <input type="text" name="creditNoteNumber" size="15" maxlength="20" onkeyup="validate(this, '^[0-9]{1,12}$', 'emptyok')" />
                    </td>
                </tr>
                <tr ng-show="pt == 'Card Payment'">
                    <td colspan="4"></td>
                    <td colspan="2"><b>Card Payment Reference Number</b></td>
                    <td div style="text-align: right" colspan="4">
                        <input type="text" name="cardReferenceNumber" size="15" maxlength="20" onkeyup="${s:getValidationRule('card.payment.reference.number','validate(this,\'^[0-9]{12,12}$\',\'emptyok\')')}" />
                    </td>
                </tr>
                <tr ng-show="pt == 'Credit Facility'">
                    <td colspan="4"></td>
                    <td colspan="2"><b>Credit Facility Payment Reference</b></td>
                    <td div style="text-align: right" colspan="4">
                        <input type="text" name="creditFacilityReference" size="15" maxlength="8" onkeyup="${s:getValidationRule('credit.facility.reference.validator','validate(this,\'^[0-9]{8,8}$\',\'emptyok\')')}" />
                    </td>
                </tr>
                <tr>
                    <td colspan="4"></td>
                    <td colspan="2"><b>Attribute Sale To</b></td>
                    <td div style="text-align: right" colspan="4">
                        <input type="text" name="sale.salesPersonCustomerId" size="15" maxlength="20" onkeyup="validate(this, '^[0-9]{0,8}$', 'emptyok')"/>
                    </td>
                </tr>


            </table>                
            <stripes:hidden name="sale.recipientCustomerId" value="${actionBean.sale.recipientCustomerId}"/>
            <stripes:hidden name="sale.recipientAccountId" value="${actionBean.sale.recipientAccountId}"/>
            <stripes:hidden name="sale.recipientOrganisationId" value="${actionBean.sale.recipientOrganisationId}"/>
            <stripes:hidden name="sale.promotionCode" value="${actionBean.sale.promotionCode}"/>
            <stripes:hidden name="sale.status" value="${actionBean.sale.status}"/>
            <stripes:hidden name="sale.saleId" value="${actionBean.sale.saleId}"/>
            <stripes:hidden name="sale.taxExempt" value="${actionBean.sale.taxExempt}"/>
            <stripes:hidden name="sale.withholdingTaxRate" value="${actionBean.sale.withholdingTaxRate}"/>
            <stripes:hidden name="sale.expiryDate" value="${actionBean.sale.expiryDate}"/>
            <stripes:hidden name="sale.creditAccountNumber" value="${actionBean.sale.creditAccountNumber}"/>
            <stripes:hidden name="sale.paymentGatewayCode" value="${actionBean.sale.paymentGatewayCode}"/>
            <stripes:hidden name="sale.uniqueId" value="${actionBean.sale.uniqueId}"/>
            <stripes:hidden name="invoiceExpiryDays" value="${invoiceExpiryDays}"/>
            <stripes:hidden name="p2pInvoicingDataOptions" value="${p2pInvoicingDataOptions}"/>
            <stripes:hidden name="sale.tenderedCurrency" value="${actionBean.sale.tenderedCurrency}"/>
            <stripes:hidden name="sale.SCAContext.comment" value="${actionBean.sale.SCAContext.comment}"/>
            <stripes:hidden name="sale.freelancerCustomerId" value="${actionBean.sale.freelancerCustomerId}"/>
            
            <c:choose>
                <c:when test="${actionBean.sale.status == 'QT'}">
                    <input type="hidden" name="sale.extraInfo" value="QuoteId=${actionBean.sale.saleId}"/>
                </c:when>
                <c:when test="${actionBean.sale.status == 'SP'}">
                    <input type="hidden" name="sale.extraInfo" value="ShopPickupId=${actionBean.sale.saleId}"/>
                </c:when>
                <c:otherwise>
                    <input type="hidden" name="sale.extraInfo" value=""/>
                </c:otherwise>
            </c:choose>

            <br/>

            <c:if test="${actionBean.sale.status == 'SP'}">
                <input type="hidden" name="sale.salesPersonAccountId" value="${actionBean.sale.salesPersonAccountId}"/>
                <input type="hidden" name="channel_${actionBean.sale.salesPersonAccountId}" value="${actionBean.sale.channel}"/>
                <table class="green" width="99%" ng-init="canBeCash('true')">
                    <tr>
                        <th>Sales Persons Airtime Account</th>
                        <th>Channel</th>
                    </tr>
                    <tr>
                        <td>${actionBean.sale.salesPersonAccountId}</td>
                        <td>${actionBean.sale.channel}</td>
                    </tr>
                </table>
            </c:if>

            <c:if test="${actionBean.sale.status != 'SP'}">
                <table ng-show="pt != 'Airtime'" class="green" width="99%">
                    <tr>
                        <th>Sales Persons Airtime Account</th>
                        <th>Account Type</th>
                        <th>Available Balance</th>
                        <th>Channel</th>
                        <th>Select</th>
                    </tr>
                    <c:forEach items="${actionBean.possibleAccountIdsForTransfer}" var="accountId" > 
                        <tr>
                            <td>${accountId}</td>
                            <td>${actionBean.accountTypes[accountId]}</td>
                            <td>${s:convertCentsToCurrencyLong(s:getAccount(accountId).availableBalanceInCents)}</td>
                            <td>
                                <stripes:select name="channel_${accountId}">
                                    <c:forEach items="${actionBean.accountChannels[accountId]}" var="channel">
                                        <stripes:option value="${channel}">${channel}</stripes:option>
                                    </c:forEach>
                                </stripes:select>
                            </td>
                            <c:if test="${actionBean.accountTypes[accountId] != 'Direct Sales' && actionBean.accountTypes[accountId] != 'Partner Sales'}">
                                <td><stripes:radio name="sale.salesPersonAccountId" value="${accountId}" ng-model="tmp" ng-change="canBeCash('false')"/></td>
                            </c:if>
                            <c:if test="${actionBean.accountTypes[accountId] == 'Direct Sales' || actionBean.accountTypes[accountId] == 'Partner Sales'}">
                                <td><stripes:radio name="sale.salesPersonAccountId" value="${accountId}" ng-model="tmp" ng-change="canBeCash('true')"/></td>
                            </c:if>
                        </tr>
                    </c:forEach>
                </table>
                            
                <div ng-show="pt == 'Airtime'">
                    <input type="hidden" name="sale.salesPersonAccountId" value="${actionBean.sale.recipientAccountId}"/>
                    <input type="hidden" name="channel_${actionBean.sale.recipientAccountId}" value="${s:getProperty('env.unitcredit.provisioning.x3.channel')}"/>
                    <table class="green" width="99%" ng-init="canBeCash('true')">
                        <tr>
                            <th>Sales Persons Airtime Account</th>
                            <th>Channel</th>
                            <th>Available Balance</th>
                        </tr>
                        <tr>
                            <td>${actionBean.sale.recipientAccountId}</td>
                            <td>${s:getProperty('env.unitcredit.provisioning.x3.channel')}</td>
                            <td>${s:convertCentsToCurrencyLong(s:getAccount(actionBean.sale.recipientAccountId).availableBalanceInCents)}</td>
                        </tr>
                    </table>
                </div>
            </c:if>
            <br/>

            <table class="green" width="99%">
                <tr><th colspan="9" align="center">Payment Method</th></tr>
                        <c:forEach items="${s:getPropertyAsList('env.pos.payment.methods')}" var="paymentType" varStatus="loop">
                            <c:if test="${loop.count == 1 || loop.count == 4 || loop.count == 7 || loop.count == 10 || loop.count == 13}">
                        <tr>
                        </c:if>

                        <td width="33%">
                            <table width="100%" style="border: none;">
                                <tr style="border: none;">
                                    <c:if test="${paymentType != 'Contract'}">
                                        <td width="45%" style="border: none;">${paymentType}</td>
                                        <td width="45%" style="border: none;">
                                            <c:if test="${paymentType == 'Card Payment'}">
                                                <stripes:select name="cardPaymentBank">
                                                    <c:forEach items="${s:getPropertyAsList('env.pos.payment.methods.card.payment.types')}" var="type">
                                                        <stripes:option value="${type}">${type}</stripes:option>
                                                    </c:forEach>
                                                </stripes:select>
                                            </c:if>
                                            <c:if test="${paymentType == 'Card Integration'}">
                                                <stripes:select name="cardIntegrationBank">
                                                    <c:forEach items="${s:getPropertyAsList('env.pos.payment.methods.card.integration.types')}" var="type">
                                                        <stripes:option value="${type}">${type}</stripes:option>
                                                    </c:forEach>
                                                </stripes:select>
                                            </c:if>
                                            <c:if test="${paymentType == 'Credit Facility'}">
                                                <stripes:select name="creditFacilitator" ng-model="crdF">
                                                    <c:forEach items="${s:getPropertyAsList('env.pos.payment.methods.credit.facility.types')}" var="type">
                                                        <c:set var="ptProp" value="${fn:split(type,'=')}"/>
                                                        <c:set var="creditFac" value="${ptProp[0]}"/>
                                                        <c:set var="creditAcc" value="${ptProp[1]}"/>
                                                        <stripes:option value="${creditFac}">${creditFac}</stripes:option>
                                                    </c:forEach>
                                                </stripes:select>
                                            </c:if>                
                                        </td>
                                        <td width="10%" style="border: none;"><stripes:radio   name="sale.paymentMethod" value="${paymentType}" ng-model="pt" ng-change="paymentMethodChanged()"/></td>
                                    </c:if>
                                    <c:if test="${paymentType == 'Contract' && s:getListSize(actionBean.contractList.contracts) > 0}">
                                        <td width="45%" style="border: none;">${paymentType}</td>
                                        <td width="45%" style="border: none;">
                                            Contract:
                                            <stripes:select name="sale.contractId">
                                                <c:forEach items="${actionBean.contractList.contracts}" var="contract">
                                                    <stripes:option value="${contract.contractId}">${contract.contractName}</stripes:option>
                                                </c:forEach>
                                            </stripes:select>
                                            <br/>Hour Of Day:
                                            <stripes:select name="hod">
                                                <c:forEach items="${s:getPropertyAsList('env.pos.contract.fulfilment.frequencies.hod')}" var="frequency">
                                                    <stripes:option value="${frequency}">${frequency}</stripes:option>
                                                </c:forEach>
                                            </stripes:select>
                                            <br/>Day of Week:
                                            <stripes:select name="dow">
                                                <c:forEach items="${s:getPropertyAsList('env.pos.contract.fulfilment.frequencies.dow')}" var="frequency">
                                                    <stripes:option value="${frequency}">${frequency}</stripes:option>
                                                </c:forEach>
                                            </stripes:select>
                                            <br/>Day Of Month:
                                            <stripes:select name="dom">
                                                <c:forEach items="${s:getPropertyAsList('env.pos.contract.fulfilment.frequencies.dom')}" var="frequency">
                                                    <stripes:option value="${frequency}">${frequency}</stripes:option>
                                                </c:forEach>
                                            </stripes:select>
                                            <br/>Days Gap:
                                            <stripes:select name="daysgap">
                                                <stripes:option value=""></stripes:option>
                                                <stripes:option value="1">1</stripes:option>
                                                <stripes:option value="7">7</stripes:option>
                                                <stripes:option value="14">14</stripes:option>
                                                <stripes:option value="30">30</stripes:option>
                                                <stripes:option value="180">180</stripes:option>
                                                <stripes:option value="365">365</stripes:option>
                                            </stripes:select>
                                            <br/>Acc Balance:
                                            <stripes:text  name="ab"  size="6" value=""/>
                                        </td>
                                        <td width="10%" style="border: none;"><stripes:radio   name="sale.paymentMethod" value="${paymentType}" ng-model="pt" ng-change="paymentMethodChanged()"/></td>
                                    </c:if>
                                </tr>
                            </table>
                        </td>
                        <c:if test="${loop.count == 3 || loop.count == 6 || loop.count == 9 || loop.count == 12 || loop.count == 15 || loop.last}">
                        </tr>
                    </c:if>
                </c:forEach>
            </table>


            <br/>
            <div>
                <stripes:submit name="backToGetItems" />
            </div>
            <div ng-show="pt == 'Cash' && cash == 'true' && (sale.amountTenderedCents - ${s:convertCentsToLongRoundHalfEven(actionBean.sale.totalLessWithholdingTaxCents)} >= 0)">
                <stripes:submit name="processSaleCash" />
            </div>
            <div ng-show="(pt == 'Bank Transfer' || pt == 'Cheque') && accountSelected">
                <stripes:submit name="processSaleBankTransfer" />
            </div>
            <div ng-show="pt == 'Quote' && accountSelected">
                <stripes:submit name="processSaleQuote" />
            </div>
            <div ng-show="pt == 'Shop Pickup' && accountSelected">
                <stripes:submit name="processSaleQuote" />
            </div>
            <div ng-show="pt == 'Credit Account' && accountSelected">
                <stripes:submit name="processSaleCreditAccount" />
            </div>
            <div ng-show="pt == 'Loan' && accountSelected">
                <stripes:submit name="processSaleLoan" />
            </div>
            <div ng-show="pt == 'Staff' && accountSelected">
                <stripes:submit name="processSaleStaff" />
            </div>
            <div ng-show="pt == 'Credit Note' && accountSelected">
                <stripes:submit name="processSaleCreditNote" />
            </div>
            <div ng-show="pt == 'Card Payment' && accountSelected">
                <stripes:submit name="processSaleCardPayment" />
            </div>
            <div ng-show="pt == 'Card Integration' && accountSelected">
                <stripes:submit name="processSaleCardIntegration" />
            </div>
            <div ng-show="pt == 'Delivery Service' && accountSelected">
                <stripes:submit name="processSaleDeliveryService" />
            </div>
            <div ng-show="pt == 'Contract' && accountSelected">
                <stripes:submit name="processSaleContract" />
            </div>
            <div ng-show="pt == 'Clearing Bureau' && accountSelected">
                <stripes:submit name="processSaleClearingBureau" />
            </div>
            <div ng-show="pt == 'Airtime' && accountSelected">
                <stripes:submit name="processSaleAirtime" />
            </div>
            <div ng-show="pt == 'Payment Gateway' && accountSelected">
                <stripes:submit name="processSalePaymentGateway" />
            </div>
            <div ng-show="pt == 'Credit Facility' && accountSelected">
                <stripes:submit name="processSaleCreditFacility" />
            </div> 
            <div ng-show="pt == 'Direct Airtime' && accountSelected">
                <stripes:submit name="processDirectAirtimePayment" />
            </div>
        </stripes:form>

        <br/><br/>
        Note: You are selling from Warehouse Id ${actionBean.sale.warehouseId}

    </stripes:layout-component>


</stripes:layout-render>

