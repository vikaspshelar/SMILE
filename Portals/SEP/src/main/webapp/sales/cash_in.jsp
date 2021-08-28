<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="cash.in"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">


        <stripes:form action="/Sales.action" focus="" autocomplete="off">    
            <table class="clear">                              
                <tr>
                    <td><fmt:message key="sales.person.username"/>:</td>
                    <td><input  type="text" name="username" value="${actionBean.username}" class="required" size="30" maxlength="50" /></td>
                </tr>
                <tr>
                    <td>
                        <fmt:message key="password"/>:
                    </td>
                    <td>
                        <input value="" type="password" name="password" class="required" size="10" maxlength="20"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                        <span class="button">
                            <stripes:submit name="showCashIn"/>
                        </span>                        
                    </td>
                </tr>  
            </table>
            <stripes:hidden name="cashInData.cashInType" value="${actionBean.cashInData.cashInType}"/>
        </stripes:form>

        <br/>

        <c:if test="${s:getListSize(actionBean.salesList.sales) == 0 && actionBean.salesList != null}">
            <tr>
                <td>
                    <b>
                        <fmt:message key="no.sales.to.cashin"/>
                    </b>
                </td>
            </tr>
        </c:if>

        <c:if test="${s:getListSize(actionBean.salesList.sales) > 0}">
            <stripes:form action="/Sales.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">    
                <stripes:hidden name="salesQuery.salesPersonCustomerId" value="${actionBean.salesQuery.salesPersonCustomerId}"/>
                <stripes:hidden name="customer.customerId" value="${actionBean.salesQuery.salesPersonCustomerId}"/>
                <stripes:hidden name="cashInData.cashInType" value="${actionBean.cashInData.cashInType}"/>
                <table class="green">
                    <tr>
                        <th>Sale Id</th>
                        <th>Customer</th>
                        <th>Account</th>
                        <th>Sale Date</th>
                        <th>Line Items</th>
                        <th>Cash In Amnt</th>
                        <th>Payment Method</th>
                        <th>Cash In</th>
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
                                    (${sale.paymentTransactionData} @ ${sale.tillId})
                                </c:if>
                            </td>
                            <td>
                                <c:if test="${actionBean.cashInData.cashInType == 'bankdeposit'}">
                                    <input type="checkbox"name="cashin_${sale.saleId}" checked="true" value="true" readonly onclick="return false;"/>
                                </c:if>
                                <c:if test="${actionBean.cashInData.cashInType != 'bankdeposit'}">
                                    <input type="checkbox" ng-init="cb_${loop.index} = true" ng-model="cb_${loop.index}" ng-change="cashInTotalChange(${s:convertCentsToLongRoundHalfEven(sale.totalLessWithholdingTaxCents)}, cb_${loop.index}, '${sale.paymentMethod}')" name="cashin_${sale.saleId}" checked="true" value="true" />
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    <tr/>
                    <tr>
                        <td colspan="6" align="right"><b>TOTAL CASH:</b></td>
                        <td ng-init="cashInRequiredAmnt=${actionBean.cashInRequiredCents/100}"><b>{{cashInRequiredAmnt | currency:"${s:getProperty('env.locale.currency.majorunit')}"}}</b></td>
                    </tr>
                    <tr>
                        <td colspan="6" align="right"><b>ACTUAL CASH:</b></td>
                        <c:if test="${actionBean.cashInData.cashInType == 'bankdeposit'}">
                            <td><input type="text" name="cashInData.cashReceiptedInCents" value="${s:convertCentsToLongRoundHalfEven(actionBean.cashInRequiredCents)}" readonly class="required" size="12" maxlength="15"/></td>
                            </c:if>
                            <c:if test="${actionBean.cashInData.cashInType != 'bankdeposit'}">
                            <td><stripes:text name="cashInData.cashReceiptedInCents" class="required" size="12" maxlength="15" onkeyup="validate(this,'^[0-9]{1,20}\.[0-9]{2,2}$','')"/></td>
                        </c:if>
                    </tr>
                </table>

                <br/>

                <table class="clear">
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:hidden name="cashInData.status" value=""/>
                                <stripes:submit name="processCashIn"/>
                            </span>                        
                        </td>
                    </tr>  
                </table>            
            </stripes:form>

        </c:if>


    </stripes:layout-component>


</stripes:layout-render>

