<%@page import="com.smilecoms.commons.base.BaseUtils, com.smilecoms.commons.stripes.SmileActionBean"%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="view.sale"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <c:set var="curr" value="${actionBean.sale.tenderedCurrency}"/>
        <table class="clear">  


            <tr>
                <td colspan="2"><b>General</b></td>
            </tr>
            <tr>
                <td width="25%">Sale Id:</td>
                <td>
                    <stripes:link href="/Sales.action" event="showSale"> 
                        <stripes:param name="sale.saleId" value="${actionBean.sale.saleId}"/>
                        ${actionBean.sale.saleId}
                    </stripes:link>
                </td>
            </tr>
            <tr>
                <td>Payment Method:</td>
                <td>${actionBean.sale.paymentMethod}</td>
            </tr>
            <tr>
                <td>Sale Status:</td>
                <td><fmt:message key="sale.status.${actionBean.sale.status}"/></td>
            </tr>
            <tr>
                <td>Sale Creation Date:</td>
                <td>${s:formatDateLong(actionBean.sale.saleDate)}</td>
            </tr>
            <tr>
                <td>Last Modified Date:</td>
                <td>${s:formatDateLong(actionBean.sale.lastModifiedDate)}</td>
            </tr>
            
            <% if (!BaseUtils.getProperty("env.country.name").equals("Uganda")) {%>
                <tr>
                    <td>External Transaction Id:</td>
                    <td>${actionBean.sale.extTxId}</td>
                </tr>
            <% } %>    
            <c:if test="${actionBean.sale.uniqueId != null}">
                <tr>
                    <td>Unique Id:</td>
                    <td>${actionBean.sale.uniqueId}</td>
                </tr>
            </c:if>
            <tr>
                <td>Exchange Rate:</td>
                <td>${actionBean.sale.tenderedCurrencyExchangeRate}</td>
            </tr>
            <c:if test="${actionBean.customer != null}">
                <tr>
                    <td colspan="2"><b>Customer</b></td>
                </tr>
                <tr>
                    <td>Customer Name:</td>
                    <td>
                        <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                            <stripes:param name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                            ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                        </stripes:link>
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.recipientAccountId > 0}">
                <tr>
                    <td>Account Id:</td>
                    <td>
                        <stripes:link href="/Account.action" event="retrieveAccount"> 
                            <stripes:param name="accountQuery.accountId" value="${actionBean.sale.recipientAccountId}"/>
                            ${actionBean.sale.recipientAccountId}
                        </stripes:link>
                    </td>
                </tr>
            </c:if>


            <c:if test="${actionBean.organisation != null}">
                <tr>
                    <td colspan="2"><b>Organisation</b></td>
                </tr>
                <tr>
                    <td>Organisation Name:</td>
                    <td><stripes:link href="/Customer.action" event="retrieveOrganisation"> 
                            <stripes:param name="organisation.organisationId" value="${actionBean.sale.recipientOrganisationId}"/>
                            ${actionBean.organisation.organisationName}
                        </stripes:link></td>
                </tr>
            </c:if>
            <c:if test="${!empty(actionBean.sale.organisationChannel)}">
                <tr>
                    <td>Organisations Channel:</td>
                    <td>${actionBean.sale.organisationChannel}</td>
                </tr>
            </c:if>

            <c:if test="${actionBean.sale.contractId > 0}">
                <tr>
                    <td colspan="2"><b>Contract Data</b></td>
                </tr>
                <tr>
                    <td>Contract Id:</td>
                    <td>
                        <stripes:link href="/Customer.action" event="viewContract"> 
                            <stripes:param name="contract.contractId" value="${actionBean.sale.contractId}"/>
                            ${actionBean.sale.contractId}
                        </stripes:link>
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.contractSaleId > 0}">
                <tr>
                    <td>Contract Sale Id:</td>
                    <td>
                        <stripes:link href="/Sales.action" event="showSale"> 
                            <stripes:param name="sale.saleId" value="${actionBean.sale.contractSaleId}"/>
                            ${actionBean.sale.contractSaleId}
                        </stripes:link>
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.fulfilmentScheduleInfo != null}">
                <tr>
                    <td>Schedule:</td>
                    <td>${actionBean.sale.fulfilmentScheduleInfo}</td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.fulfilmentLastCheckDateTime != null}">
                <tr>
                    <td>Last Schedule Check:</td>
                    <td>${s:formatDateLong(actionBean.sale.fulfilmentLastCheckDateTime)}</td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.fulfilmentPausedTillDateTime != null}">
                <tr>
                    <td>Paused Till:</td>
                    <td>${s:formatDateLong(actionBean.sale.fulfilmentPausedTillDateTime)}</td>
                </tr>
            </c:if>


            <tr>
                <td colspan="2"><b>Salesperson</b></td>
            </tr>
            <tr>
                <td>Sales Person:</td>
                <td>
                    <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                        <stripes:param name="customerQuery.customerId" value="${actionBean.salesPerson.customerId}"/>
                        ${actionBean.salesPerson.firstName} ${actionBean.salesPerson.lastName}
                    </stripes:link>
                </td>
            </tr>
            
            <% if (BaseUtils.getBooleanProperty("env.pos.sales.freelance.model.enabled", false)) {%>
                <tr>
                    <td colspan="2"><b>Freelance Sales Person</b></td>
                </tr>
                <c:forEach items="${actionBean.salesPersonsFreelancers}" var="freelancerEntry">
                <tr>
                    <td>Freelance Sales Person:</td>
                    <td>${freelancerEntry.value}</td>
                </tr>
                </c:forEach> 
            <% } %>
                
            <tr>
                <td>Till Type:</td>
                <td>
                    <c:if test="${actionBean.sale.paymentMethod == 'Card Payment'}">
                        <stripes:form action="/Sales.action" >    
                            <stripes:select name="saleModificationData.tillId">
                                <c:forEach items="${s:getPropertyAsList('env.pos.payment.methods.card.payment.types')}" var="type">
                                    <c:choose>
                                        <c:when test='${actionBean.sale.tillId == type}'>
                                            <stripes:option value="${type}" selected="selected">${type}</stripes:option>
                                        </c:when>
                                        <c:otherwise>
                                            <stripes:option value="${type}">${type}</stripes:option>
                                        </c:otherwise>
                                    </c:choose>
                                </c:forEach>
                            </stripes:select>
                            <input type="hidden" name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                            <input type="hidden" name="sale.saleId" value="${actionBean.sale.saleId}"/>
                            <stripes:submit name="modifyTillId"/>
                        </stripes:form>
                    </c:if>
                    <c:if test="${actionBean.sale.paymentMethod != 'Card Payment'}">
                        ${actionBean.sale.tillId}
                    </c:if>
                </td>
            </tr>
            <tr>
                <td>Sales Person Account:</td>
                <td>
                    <stripes:link href="/Account.action" event="retrieveAccount"> 
                        <stripes:param name="accountQuery.accountId" value="${actionBean.sale.salesPersonAccountId}"/>
                        ${actionBean.sale.salesPersonAccountId}
                    </stripes:link>
                </td>
            </tr>
            <c:if test="${!empty(actionBean.sale.cashInDate)}">
                <tr>
                    <td>Cash In Date:</td>
                    <td>${s:formatDateLong(actionBean.sale.cashInDate)}</td>
                </tr>
            </c:if>
            <c:if test="${!empty(actionBean.sale.promotionCode)}">
                <tr>
                    <td>Promotion Code:</td>
                    <td>${actionBean.sale.promotionCode}</td>
                </tr>
            </c:if>
            <tr>
                <td>Location:</td>
                <td>${actionBean.sale.saleLocation}</td>
            </tr>
            <tr>
                <td>Warehouse Id:</td>
                <td>${actionBean.sale.warehouseId}</td>
            </tr>
            <tr>
                <td>Sales Persons Channel:</td>
                <td>${actionBean.sale.channel}</td>
            </tr>

            <tr>
                <td colspan="2"><b>Payment Data</b></td>
            </tr>

            <c:if test="${!empty(actionBean.sale.creditAccountNumber)}">
                <tr>
                    <td>Credit Account Number:</td>
                    <td>${actionBean.sale.creditAccountNumber}</td>
                </tr>
            </c:if>

            <c:if test="${!empty(actionBean.sale.purchaseOrderData)}">
                <tr>
                    <td>Purchase Order:</td>
                    <td>${actionBean.sale.purchaseOrderData}</td>
                </tr>
            </c:if>
            <c:if test="${!empty(actionBean.sale.extraInfo)}">
                <tr>
                    <td>Extra Info:</td>
                    <td>${actionBean.sale.extraInfo}</td>
                </tr>
            </c:if>
            <c:if test="${!empty(actionBean.sale.paymentTransactionData) && actionBean.sale.paymentMethod == 'Card Payment'}">
                <tr>
                    <td>Payment Transaction Data:</td>
                    <td>
                        <stripes:form action="/Sales.action" >    
                            <stripes:text name="saleModificationData.paymentTransactionData" value="${actionBean.sale.paymentTransactionData}" size="30" maxlength="20" onkeyup="validate(this,'^[0-9a-zA-Z]{1,20}$','')"/>
                            <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                            <input type="hidden" name="sale.saleId" value="${actionBean.sale.saleId}"/>
                            <stripes:submit name="modifyPaymentTransactionData"/>
                        </stripes:form>
                    </td>
                </tr>
            </c:if>
            <c:if test="${!empty(actionBean.sale.paymentTransactionData) && actionBean.sale.paymentMethod != 'Card Payment'}">
                <tr>
                    <td>Payment Transaction Data:</td>
                    <td>
                        ${actionBean.sale.paymentTransactionData}
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.invoicePDFBase64 != null}">
                <tr>
                    <td>
                        <c:if test="${actionBean.sale.paymentMethod == 'Quote'}">
                            Quotation:
                        </c:if>
                        <c:if test="${actionBean.sale.paymentMethod != 'Quote'}">
                            Invoice:
                        </c:if>
                    </td>
                    <td>
                        <stripes:link href="/Sales.action" event="getSaleInvoice"> 
                            <stripes:param name="salesQuery.salesIds[0]" value="${actionBean.sale.saleId}"/>
                            Click here to download as a pdf
                        </stripes:link>
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.smallInvoicePDFBase64 != null}">
                <tr>
                    <td>
                        POS Printer Invoice:
                    </td>
                    <td>
                        <stripes:link href="/Sales.action" event="getSmallSaleInvoice"> 
                            <stripes:param name="salesQuery.salesIds[0]" value="${actionBean.sale.saleId}"/>
                            Click here to download as a pdf
                        </stripes:link>
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.reversalPDFBase64 != null}">
                <tr>
                    <td>
                        Reversal:
                    </td>
                    <td>
                        <stripes:link href="/Sales.action" event="getSaleReversal"> 
                            <stripes:param name="salesQuery.salesIds[0]" value="${actionBean.sale.saleId}"/>
                            Click here to download as a pdf
                        </stripes:link>
                    </td>
                </tr>
            </c:if>
            <tr>
                <td>Finance Data:</td>
                <td>${actionBean.sale.financeData}</td>
            </tr>
            <tr>
                <td>Resend failures to X3:</td>
                <td>
                    <stripes:form action="/Sales.action" >    
                        <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                        <input type="hidden" name="sale.saleId" value="${actionBean.sale.saleId}"/>
                        <stripes:submit name="putSaleBackInX3Queue"/>
                        <stripes:submit name="putCashInBackInX3Queue"/>
                    </stripes:form>
                </td>
            </tr>
            <tr>
                <td>Invoice Expiry Date:</td>
                <td>${s:formatDateLong(actionBean.sale.expiryDate)}</td>
            </tr>
            <c:if test="${!empty(actionBean.sale.transactionFeeModel)}">
                <tr>
                    <td>Transaction Fee Model:</td>
                    <td>${actionBean.sale.transactionFeeModel}</td>
                </tr>
                <tr>
                    <td>Transaction Fee:</td>
                    <td>${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, actionBean.sale.transactionFeeCents)}</td>
                </tr>
            </c:if>
            <c:if test="${!empty(actionBean.sale.deliveryFeeModel)}">
                <tr>
                    <td>Delivery Fee Model:</td>
                    <td>${actionBean.sale.deliveryFeeModel}</td>
                </tr>
                <tr>
                    <td>Delivery Fee:</td>
                    <td>${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, actionBean.sale.deliveryFeeCents)}</td>
                </tr>
            </c:if>


            <c:if test="${actionBean.sale.paymentGatewayCode != null}">
                <tr>
                    <td colspan="2"><b>Payment Gateway Data</b></td>
                </tr>
                <tr>
                    <td>Payment Gateway:</td>
                    <td>${actionBean.sale.paymentGatewayCode}</td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.paymentGatewayResponse != null}">
                <tr>
                    <td>Payment Gateway Response:</td>
                    <td>${s:breakUp(actionBean.sale.paymentGatewayResponse,70)}</td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.paymentGatewayPollCount > 0 }">
                <tr>
                    <td>Payment Gateway Poll Count:</td>
                    <td>${actionBean.sale.paymentGatewayPollCount}</td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.paymentGatewayLastPollDate != null }">
                <tr>
                    <td>Last Polled:</td>
                    <td>${s:formatDateLong(actionBean.sale.paymentGatewayLastPollDate)}</td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.paymentGatewayNextPollDate != null }">
                <tr>
                    <td>Next Poll:</td>
                    <td>${s:formatDateLong(actionBean.sale.paymentGatewayNextPollDate)}</td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.paymentGatewayURL != null }">
                <tr>
                    <td>Payment Gateway URL:</td>
                    <td>${actionBean.sale.paymentGatewayURL}</td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.paymentGatewayURLData != null }">
                <tr>
                    <td>Payment Gateway URL Data:</td>
                    <td>${s:breakUp(actionBean.sale.paymentGatewayURLData,70)}</td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.landingURL != null }">
                <tr>
                    <td>Landing URL:</td>
                    <td>${actionBean.sale.landingURL}</td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.callbackURL != null }">
                <tr>
                    <td>Callback URL:</td>
                    <td>${actionBean.sale.callbackURL}</td>
                </tr>
            </c:if>
            <c:if test="${!empty(actionBean.sale.paymentGatewayExtraData)}">
                <tr>
                    <td>Extra Info:</td>
                    <td>${actionBean.sale.paymentGatewayExtraData}</td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.paymentMethod == 'Payment Gateway' && (actionBean.sale.status == 'RV' || actionBean.sale.status == 'FG' || actionBean.sale.status == 'FS') }">
                <tr>
                    <td>Resubmit to payment gateway queue:</td>
                    <td>
                        <stripes:form action="/Sales.action" >    
                            <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                            <input type="hidden" name="sale.saleId" value="${actionBean.sale.saleId}"/>
                            <stripes:submit name="putSaleBackInGatewayQueue"/>
                        </stripes:form>
                    </td>
                </tr>
            </c:if>

        </table>    

        <br/>

        <table class="green" width="99%">            
            <tr>
                <th></th>
                <th>Item Number</th>
                <th>Serial Number</th>
                <th>Description</th>
                <th>Provis Data</th>
                <th>Unit Price Incl</th>
                <th>Qty</th>
                <th>Discount Incl</th>
                <th>Line Price Incl</th>
                <th>Return or Replace</th>
            </tr>
            <c:forEach items="${actionBean.sale.saleLines}" var="line" varStatus="loop"> 
                <tr>   
                    <td title="${line.lineId}">${line.lineNumber}</td>
                    <td>${line.inventoryItem.itemNumber}</td>
                    <c:if test="${fn:length(line.inventoryItem.itemNumber) >= 3 && line.inventoryItem.itemNumber.substring(0,3) == 'SIM'}">
                        <td>
                            <stripes:link href="/ProductCatalog.action" event="showAddProductWizard"> 
                                <stripes:param name="customer.customerId" value="${actionBean.customer.customerId}"/>
                                <stripes:param name="iccid" value="${line.inventoryItem.serialNumber}"/>
                                ${line.inventoryItem.serialNumber}
                            </stripes:link>
                        </td>
                    </c:if>
                    <c:if test="${fn:length(line.inventoryItem.itemNumber) < 3 || line.inventoryItem.itemNumber.substring(0,3) != 'SIM'}">
                        <td>${line.inventoryItem.serialNumber}</td>
                    </c:if>
                    <c:if test="${line.comment == null}">
                        <td title="${line.inventoryItem.description}">${s:abbreviate(line.inventoryItem.description,30)}</td>
                    </c:if>
                    <c:if test="${line.comment != null}">
                        <td title="${s:concat3(line.inventoryItem.description,' - ',line.comment)}">${s:abbreviate(s:concat3(line.inventoryItem.description,' - ',line.comment), 30)}</td>
                    </c:if>
                    <c:choose>
                        <c:when test="${fn:startsWith(line.inventoryItem.itemNumber,'BUNC')}">
                            <td/>
                            <td/>
                            <td/>
                            <td/>
                            <td/>
                            <td>&nbsp;</td>
                        </c:when>
                        <c:otherwise>
                            <td title="${line.provisioningData}">${s:abbreviate(line.provisioningData,10)}</td>
                            <td align="right">${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, line.inventoryItem.priceInCentsIncl)}</td>
                            <td align="right">${line.quantity}</td>
                            <td align="right">${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, line.lineTotalDiscountOnInclCents)}</td>
                            <td align="right">${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, line.lineTotalCentsIncl)}</td>
                            <c:choose>
                                <c:when test="${s:getListSize(line.subSaleLines) == 0}">
                                    <td>
                                        <stripes:form action="/Sales.action" >
                                            <stripes:hidden name="salesQuery.saleLineId" value="${line.lineId}"/>
                                            <stripes:submit name="showDoReturnReplacement"/>
                                        </stripes:form>
                                    </td>
                                </c:when>
                                <c:otherwise>
                                    <td>&nbsp;</td>
                                </c:otherwise>
                            </c:choose>
                        </c:otherwise>
                    </c:choose>
                    
                </tr>
                <c:forEach items="${line.subSaleLines}" var="subline" varStatus="subloop"> 
                    <tr>   
                        <td title="${subline.lineId}"></td>
                        <td>${subline.inventoryItem.itemNumber}</td>
                        <c:if test="${fn:length(subline.inventoryItem.itemNumber) >= 3 && subline.inventoryItem.itemNumber.substring(0,3) == 'SIM'}">
                            <td>
                                <stripes:link href="/ProductCatalog.action" event="showAddProductWizard"> 
                                    <stripes:param name="customer.customerId" value="${actionBean.customer.customerId}"/>
                                    <stripes:param name="iccid" value="${subline.inventoryItem.serialNumber}"/>
                                    ${subline.inventoryItem.serialNumber}
                                </stripes:link>
                            </td>
                        </c:if>
                        <c:if test="${fn:length(subline.inventoryItem.itemNumber) < 3 || subline.inventoryItem.itemNumber.substring(0,3) != 'SIM'}">
                            <td>${subline.inventoryItem.serialNumber}</td>
                        </c:if>
                        <td>${subline.inventoryItem.description}</td>
                        <td title="${subline.provisioningData}">${s:abbreviate(subline.provisioningData,10)}</td>
                        <td align="right"></td>
                        <td align="right">${subline.quantity}</td>
                        <td align="right"></td>
                        <td align="right"></td>
                        <td>
                            <stripes:form action="/Sales.action" >
                                <stripes:hidden name="salesQuery.saleLineId" value="${subline.lineId}"/>
                                <stripes:submit name="showDoReturnReplacement"/>
                            </stripes:form>
                        </td>
                    </tr>
                </c:forEach>
            </c:forEach>
            <tr class="even">
                <td colspan="10"></td>
            </tr>
            <tr>
                <td colspan="4">Date: ${s:formatDateLong(actionBean.sale.saleDate)}</td>
                <td colspan="2">Total Discount</td>
                <td align="right" colspan="5">${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, actionBean.sale.saleTotalDiscountOnInclCents)}</td>
            </tr>
            <tr>
                <td colspan="4">
                    <c:if test="${actionBean.sale.taxExempt}">
                        <b>This Sale Is Tax Exempt</b>
                    </c:if>
                </td>
                <td colspan="2">Tax</td>
                <% if (BaseUtils.getDoubleProperty("env.sales.excise.tax.percent",0)<=0) {%>
                    <td align="right" colspan="5">${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, actionBean.sale.saleTotalTaxCents)}</td>
                <% } %>
                <% if (BaseUtils.getDoubleProperty("env.sales.excise.tax.percent",0)>0) {%>
                    <td align="right" colspan="5">${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, (actionBean.sale.saleTotalTaxCents-(actionBean.sale.saleTotalCentsExcl *(BaseUtils.getDoubleProperty("env.sales.excise.tax.percent", 0)/100))))}</td>
                <% } %>    
            </tr>
            <tr>
                <td colspan="4"></td>
                <td colspan="2">Withholding Tax</td>
                <td align="right" colspan="4">${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, actionBean.sale.withholdingTaxCents)}</td>
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
                    ${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, actionBean.sale.saleTotalCentsIncl)}
                </td>
            </tr>
            <tr>
                <td colspan="4"></td>
                <td colspan="2"><b>Tendered Amount</b></td>
                <td align="right" colspan="4" style="font-weight: bold; font-size: 13pt; text-align: right">
                    <b>${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, actionBean.sale.amountTenderedCents)}</b>
                </td>
            </tr>
            <tr>
                <td colspan="4"></td>
                <td colspan="2"><b>Change</b></td>
                <td align="right" style="font-weight: bold; font-size: 13pt; text-align: right" colspan="4">
                    <b>${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(curr, actionBean.sale.changeCents)}</b>
                </td>
            </tr>
        </table>                

        <br/>
        <br/>


        <table class="green" width="99%">
            <tr><th>Sale Actions</th></tr>
                    <c:if test="${actionBean.sale.status == 'QT' || actionBean.sale.status == 'SP'}">
                <tr>
                    <td>
                        <stripes:form action="/Sales.action" onsubmit="return alertValidationErrors();">    
                            <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                            Reason:&nbsp;<stripes:text name="saleModificationData.SCAContext.comment" value="" size="50" maxlength="200" onkeyup="validate(this,'^.{20,200}$','emptynotok')"/>
                            <stripes:submit name="reverseSale"/>
                        </stripes:form>
                    </td>
                </tr>
                <tr>
                    <td>
                        <stripes:form action="/Sales.action" >    
                            <stripes:hidden name="salesQuery.salesIds[0]" value="${actionBean.sale.saleId}"/>
                            <stripes:submit name="makeSaleFromQuote"/>
                        </stripes:form>
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.status == 'CB'}">
                <tr>
                    <td>
                        <stripes:form action="/Sales.action" onsubmit="return alertValidationErrors();">    
                            <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                            Reason:&nbsp;<stripes:text name="saleModificationData.SCAContext.comment" value="" size="50" maxlength="200" onkeyup="validate(this,'^.{20,200}$','emptynotok')"/>
                            <stripes:submit name="reverseSale"/>
                        </stripes:form>
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.status == 'PP'}">
                <tr>
                    <td>
                        <stripes:form action="/Sales.action" onsubmit="return alertValidationErrors();">    
                            <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                            Reason:&nbsp;<stripes:text name="saleModificationData.SCAContext.comment" value="" size="50" maxlength="200" onkeyup="validate(this,'^.{20,200}$','emptynotok')"/>
                            <stripes:submit name="reverseSale"/>
                        </stripes:form>
                    </td>
                </tr>
                <tr>
                    <td>
                        <stripes:form action="/Sales.action"  autocomplete="off" onsubmit="return alertValidationErrorsForElement(this);">    
                            <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                            <stripes:hidden name="saleModificationData.paymentInCents" value="${actionBean.sale.saleTotalCentsIncl}"/>
                            <table class="clear">
                                <tr>
                                    <td colspan="2"><b>Payment Capture</b></td>
                                </tr>
                                <tr>
                                    <td>Smile Payment Reference Number:</td>
                                    <td><stripes:text size="10" maxlength="20" name="saleModificationData.paymentTransactionData" onkeyup="validate(this,'^[0-9a-zA-Z]{1,20}$','')"/></td>
                                </tr>
                                <tr>
                                    <td colspan="2">
                                        <span class="button">
                                            <stripes:submit name="processPaymentOfNonCashSale"/>
                                        </span>
                                    </td>
                                </tr>
                            </table>            
                        </stripes:form>
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.status == 'PV'}">
                <tr>
                    <td>
                        <stripes:form action="/Sales.action"  autocomplete="off" onsubmit="return alertValidationErrorsForElement(this);">    
                            <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                            <stripes:hidden name="saleModificationData.paymentInCents" value="${actionBean.sale.saleTotalCentsIncl}"/>
                            <stripes:hidden name="saleModificationData.paymentTransactionData" value="${actionBean.sale.paymentTransactionData}"/>
                            <table class="clear">
                                <tr>
                                    <td colspan="2"><b>Payment Verification</b></td>
                                </tr>
                                <tr>
                                    <td>Smile Payment Reference Number:</td>
                                    <td>${actionBean.sale.paymentTransactionData}</td>
                                </tr>
                                <tr>
                                    <td colspan="2">
                                        <span class="button">
                                            <stripes:submit name="processPaymentVerificationOfNonCashSale"/>
                                        </span>
                                    </td>
                                </tr>
                            </table>            
                        </stripes:form>
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.status == 'PD' || actionBean.sale.status == 'WT'}">
                <tr>
                    <td>
                        <stripes:form action="/Sales.action" onsubmit="return alertValidationErrors();">    
                            <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                            Reason:&nbsp;<stripes:text name="saleModificationData.SCAContext.comment" value="" size="50" maxlength="200" onkeyup="validate(this,'^.{20,200}$','emptynotok')"/>
                            <stripes:submit name="reverseSale"/>
                        </stripes:form>
                    </td>
                </tr>
                <tr>
                    <td>
                        <stripes:form action="/Sales.action" >    
                            <input type="hidden" name="sale.saleId" value="${actionBean.sale.saleId}"/>
                            <stripes:submit name="showReturnItemsInSale"/>
                        </stripes:form>
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.status == 'PL'}">
                <tr>
                    <td>
                        <stripes:form action="/Sales.action" onsubmit="return alertValidationErrors();">    
                            <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                            Reason:&nbsp;<stripes:text name="saleModificationData.SCAContext.comment" value="" size="50" maxlength="200" onkeyup="validate(this,'^.{20,200}$','emptynotok')"/>
                            <stripes:submit name="reverseSale"/>
                        </stripes:form>
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.status == 'DP'}">
                <tr>
                    <td>
                        <stripes:form action="/Sales.action" onsubmit="return alertValidationErrors();">    
                            <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                            Reason:&nbsp;<stripes:text name="saleModificationData.SCAContext.comment" value="" size="50" maxlength="200" onkeyup="validate(this,'^.{20,200}$','emptynotok')"/>
                            <stripes:submit name="reverseSale"/>
                        </stripes:form>
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.paymentMethod == 'Delivery Service'}">
                <tr>
                    <td>
                        <stripes:form action="/Sales.action" autocomplete="off" onsubmit="return alertValidationErrors(this);">    
                            <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                            Customer Id:
                            <stripes:text name="saleModificationData.deliveryCustomerId"  size="8" maxlength="10" onkeyup="validate(this,'^[0-9]{1,10}$','emptynotok')"/>
                            <stripes:submit name="sendDeliveryNote"/>
                        </stripes:form>
                    </td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.status == 'ST' && actionBean.sale.paymentMethod == 'Loan'}">
                <tr>
                    <td>
                        <stripes:form action="/Sales.action" >    
                            <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                            <stripes:submit name="processLoanCompletion"/>
                        </stripes:form>
                    </td>
                </tr>
            </c:if>
            <c:if test="${s:getListSize(actionBean.possibleAccountIdsForTransfer) > 0 && (actionBean.sale.status == 'PD' || actionBean.sale.status == 'PL')}">
                <tr>
                    <td>
                        <stripes:form action="/Sales.action" autocomplete="off" onsubmit="return alertValidationErrors(this);">
                            Account: 
                            <stripes:select name="salePostProcessingData.accountId" class="required">
                                <c:forEach items="${actionBean.possibleAccountIdsForTransfer}" var="accountId">
                                    <stripes:option value="${accountId}">${accountId}</stripes:option>
                                </c:forEach>                           
                            </stripes:select>&nbsp;
                            Product Instance Id:
                            <stripes:text name="salePostProcessingData.productInstanceId" value="0" size="7" maxlength="10" onkeyup="validate(this,'^[0-9]{1,10}$','emptyok')"/>
                            <stripes:hidden name="salePostProcessingData.saleId" value="${actionBean.sale.saleId}"/>
                            <input type="hidden" name="sale.saleId" value="${actionBean.sale.saleId}"/>
                            <stripes:submit name="doPostProcessing"/>
                        </stripes:form>
                    </td>
                </tr>
                <tr>
                    <td>
                        <stripes:form action="/Sales.action" autocomplete="off" onsubmit="return alertValidationErrors(this);">
                            Account: 
                            <stripes:text name="salePostProcessingData.accountId"  maxlength="10" size="10" onkeyup="validate(this,'^[0-9]{10,10}$','emptyok')"/>
                            &nbsp;
                            Product Instance Id:
                            <stripes:text name="salePostProcessingData.productInstanceId" value="0" size="7" maxlength="10" onkeyup="validate(this,'^[0-9]{1,10}$','emptyok')"/>
                            &nbsp;
                            ICCID:
                            <stripes:text name="salePostProcessingData.serialNumber"  maxlength="20" size="20" onkeyup="validate(this,'^[0-9]{20,20}$','luhn_emptyok')"/>
                            <stripes:hidden name="salePostProcessingData.saleId" value="${actionBean.sale.saleId}"/>
                            <input type="hidden" name="sale.saleId" value="${actionBean.sale.saleId}"/>
                            <stripes:submit name="doPostProcessing"/>
                        </stripes:form>
                    </td>
                </tr>
                <tr>
                    <td>
                        <stripes:form action="/Sales.action" autocomplete="off" onsubmit="return alertValidationErrors(this);">
                            Account: 
                            <stripes:text name="salePostProcessingData.accountId"  maxlength="10" size="10" onkeyup="validate(this,'^[0-9]{10,10}$','emptyok')"/>
                            &nbsp;
                            Product Instance Id:
                            <stripes:text name="salePostProcessingData.productInstanceId" value="0" size="7" maxlength="10" onkeyup="validate(this,'^[0-9]{1,10}$','emptyok')"/>
                            &nbsp;
                            ICCID:
                            <stripes:text name="salePostProcessingData.serialNumber"  maxlength="20" size="20" onkeyup="validate(this,'^[0-9]{20,20}$','luhn_emptyok')"/>
                            Kit:
                            <stripes:text name="salePostProcessingData.kitItemNumber"  maxlength="20" size="20" />
                            Device:
                            <stripes:text name="salePostProcessingData.deviceSerialNumber"  maxlength="20" size="20" />
                            <stripes:hidden name="salePostProcessingData.saleId" value="${actionBean.sale.saleId}"/>
                            <input type="hidden" name="sale.saleId" value="${actionBean.sale.saleId}"/>
                            <stripes:submit name="doPostProcessing"/>
                        </stripes:form>
                    </td>
                </tr>
            </c:if>
            <tr>
                <td>
                    <stripes:form action="/Sales.action" >    
                        <stripes:hidden name="saleModificationData.saleId" value="${actionBean.sale.saleId}"/>
                        <stripes:submit name="regenerateInvoice"/>
                    </stripes:form>
                </td>
                
            </tr>
        </table>


        <br/>
        <br/>

        <table class="green" width="99%">                         
            <tr>
                <td colspan="2">
                    <b>Typical next steps after making a Sale</b>
                </td>
            </tr>
            <c:if test="${actionBean.sale.paymentMethod == 'Cash'}">
                <tr>
                    <td>This was a cash sale. You can now proceed to provision the customers requested product onto any SIM(s) that were sold
                    </td>
                    <td style="text-align: center">
                        <stripes:form action="/Customer.action" >    
                            <span class="button">
                                <stripes:submit name="showAddProductWizard"/>
                                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>   
                            </span>      
                        </stripes:form>
                    </td>
                </tr>     
            </c:if>
            <c:if test="${actionBean.sale.paymentMethod == 'Card Payment'}">
                <tr>
                    <td>This was a Card Payment sale. You can now proceed to provision the customers requested product onto any SIM(s) that were sold
                    </td>
                    <td style="text-align: center">
                        <stripes:form action="/Customer.action" >    
                            <span class="button">
                                <stripes:submit name="showAddProductWizard"/>
                                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>   
                            </span>      
                        </stripes:form>
                    </td>
                </tr>     
            </c:if>
            <c:if test="${actionBean.sale.paymentMethod == 'Staff'}">
                <tr>
                    <td>This was for a staff member. You can now proceed to provision the staff product onto the SIM
                    </td>
                    <td style="text-align: center">
                        <stripes:form action="/Customer.action" >    
                            <span class="button">
                                <stripes:submit name="showAddProductWizard"/>
                                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>   
                            </span>      
                        </stripes:form>
                    </td>
                </tr>     
            </c:if>
            <c:if test="${actionBean.sale.paymentMethod == 'Loan'}">
                <tr>
                    <td>This was for a trial/loan. You can now proceed to provision the trial product onto the SIM
                    </td>
                    <td style="text-align: center">
                        <stripes:form action="/Customer.action" >    
                            <span class="button">
                                <stripes:submit name="showAddProductWizard"/>
                                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>   
                            </span>      
                        </stripes:form>
                    </td>
                </tr>     
            </c:if>
            <c:if test="${actionBean.sale.paymentMethod == 'Bank Transfer'}">
                <tr>
                    <td colspan="2">This was a Bank Transfer sale. The customer should not be given any goods at this point. 
                        Once Smile finance receives the payment in our bank, you can deliver the goods and provision the SIM(s).                        
                    </td>
                </tr>     
            </c:if>
            <c:if test="${actionBean.sale.paymentMethod == 'Cheque'}">
                <tr>
                    <td colspan="2">This was a Cheque sale. The customer should not be given any goods at this point. 
                        Once the Cheque clears and Smile finance receives the payment in our bank, you can deliver the goods and provision the SIM(s).                        
                    </td>
                </tr>     
            </c:if>
            <c:if test="${actionBean.sale.paymentMethod == 'Quote'}">
                <tr>
                    <td colspan="2">This was a Quote. The customer should not be given any goods                       
                    </td>
                </tr>     
            </c:if>
            <c:if test="${actionBean.sale.paymentMethod == 'Credit Account'}">
                <tr>
                    <td>This was a credit sale. You can now proceed to provision the customers requested product onto any SIM(s) that were sold
                    </td>
                    <td style="text-align: center">
                        <stripes:form action="/Customer.action" >    
                            <span class="button">
                                <stripes:submit name="showAddProductWizard"/>
                                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>   
                            </span>      
                        </stripes:form>
                    </td>
                </tr>        
            </c:if>
        </table>




    </stripes:layout-component>


</stripes:layout-render>

