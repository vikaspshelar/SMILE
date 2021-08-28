<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="cash.in"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
     <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>
                        <fmt:message key="process.bank.cash.in"/> : CashIn Id (${actionBean.cashInData.cashInId}) 
                    </td>
                </tr>
            </table>

        <br/>

        <c:if test="${s:getListSize(actionBean.salesList.sales) == 0 && actionBean.salesList != null}">
            <table class="green">
            <tr>
                <td>
                    <b>
                        <fmt:message key="no.sales.to.cashin"/>
                    </b>
                </td>
            </tr>
            </table>
        </c:if>

        <c:if test="${s:getListSize(actionBean.salesList.sales) > 0}" >
            <stripes:form action="/Sales.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">
                <stripes:hidden name="cashInData.cashInId" value="${actionBean.cashInData.cashInId}"/>
                <stripes:hidden name="cashInData.cashInType" value="${actionBean.cashInData.cashInType}"/>
                <stripes:hidden name="cashInData.salesPersonCustomerId" value="${actionBean.cashInData.salesPersonCustomerId}"/>
                <stripes:hidden name="cashInData.status" value="${actionBean.cashInData.status}"/>
                <stripes:hidden name="salesQuery.salesPersonCustomerId" value="0"/>
                <stripes:hidden name="salesQuery.recipientCustomerId" value="0"/>
                <stripes:hidden name="salesQuery.purchaseOrderData" value=""/>
                <stripes:hidden  name="cashInQuery.cashInType" value="bankdeposit"/>
                <stripes:hidden  name="cashInQuery.status" value="BDP"/>
                            
                <c:forEach items="${actionBean.salesQuery.salesIds}" var="saleId" varStatus="loop2"> 
                    <stripes:hidden name="salesQuery.salesIds[${loop2.index}]" value="${saleId}"/>
                    <stripes:hidden name="cashin_${saleId}" value="true"/>
                </c:forEach> 

                <table class="green">
                    <tr>
                        <th>Sale Id</th>
                        <th>Customer</th>
                        <th>Account</th>
                        <th>Sale Date</th>
                        <th>Line Items</th>
                        <th>Cash In Amnt</th>
                        <th>Payment Method</th>
                   </tr>
                    <c:forEach items="${actionBean.salesList.sales}" var="sale" varStatus="loop"> 
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            <td>  
                                <stripes:link href="/Sales.action" event="showSale"> 
                                    <stripes:param name="sale.saleId" value="${sale.saleId}"/>
                                    ${sale.saleId}
                                </stripes:link>
                            </td>
                            <td>
                                <c:if test="${sale.recipientCustomerId > 0}">
                                    <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                        <stripes:param name="customer.customerId" value="${sale.recipientCustomerId}"/>
                                        ${sale.recipientCustomerId}
                                    </stripes:link>
                                </c:if>
                            </td>
                            <td>
                                <c:if test="${sale.recipientAccountId > 0}">
                                    <stripes:link href="/Account.action" event="retrieveAccount"> 
                                        <stripes:param name="account.accountId" value="${sale.recipientAccountId}"/>
                                        ${sale.recipientAccountId}
                                    </stripes:link>
                                </c:if>
                            </td>
                            <td>${s:formatDateLong(sale.saleDate)}</td>
                            <td>
                                <table class="clear" width="98%">
                                    <tr>
                                        <th>Description</th>
                                        <th>Serial Number</th>
                                        <th>Price</th>
                                    </tr>
                                    <c:forEach items="${sale.saleLines}" var="line"> 
                                        <tr>
                                            <td>${line.inventoryItem.description}</td>
                                            <td>${line.inventoryItem.serialNumber}</td>
                                            <td>${s:convertCentsToCurrencyLongRoundHalfEven(line.lineTotalCentsIncl)}</td>
                                        </tr>
                                    </c:forEach>
                                </table>
                            </td>
                            <td>${s:convertCentsToCurrencyLongRoundHalfEven(sale.totalLessWithholdingTaxCents)}</td>   
                            <td>
                                ${sale.paymentMethod}
                                <c:if test="${sale.paymentMethod == 'Card Payment'}">
                                    (${sale.paymentTransactionData}@${sale.tillId})
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    <tr/>
                    <tr>
                        <td colspan="6" align="right"><b>Deposit Reference Number</b></td>
                        <td>
                            <stripes:text name="cashInData.extTxId" size="15" maxlength="20"/>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="6" align="right"><b>TOTAL CASH EXPECTED:</b></td>
                        <td ng-init="cashInRequiredAmnt=${actionBean.cashInRequiredCents/100}"><b>{{cashInRequiredAmnt | currency:"${s:getProperty('env.locale.currency.majorunit')}"}}</b></td>
                    </tr>
                    <tr>
                        <td colspan="6" align="right"><b>ACTUAL CASH DEPOSITED IN THE BANK:</b></td>
                        <td><input type="hidden" name="cashInData.cashReceiptedInCents" value="${actionBean.cashInRequiredCents/100}"/>{{cashInRequiredAmnt | currency:"${s:getProperty('env.locale.currency.majorunit')}"}}</td>
                    </tr>
                    <tr>
                    <td colspan="6" align="right"><b><stripes:label for="banking.details"/>:</b></td>
                        <td>
                            <stripes:select name="cashInData.bankName" onchange="return validate(this,'^.{2,100}$','emptynotok');">
                                <stripes:option value="" selected="true"></stripes:option>
                                <c:forEach items="${s:getPropertyAsList('env.pos.cashin.smile.bank.names')}" var="bank" varStatus="loop">
                                    <stripes:option value="${bank}">${bank}</stripes:option>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>
                    
                </table>

                <br/>

                <table class="clear">
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="processCashIn"/>
                            </span>                        
                        </td>
                    </tr>  
                </table>            
            </stripes:form>

        </c:if>

     </div>
    </stripes:layout-component>


</stripes:layout-render>

