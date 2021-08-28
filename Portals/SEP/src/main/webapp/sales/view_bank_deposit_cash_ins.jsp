<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="process.pending.bank.cash.in"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="">
    <stripes:layout-component name="contents">
        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>
                        <fmt:message key="process.pending.bank.cash.in"/> : Sales Administrator (${actionBean.customer.firstName} ${actionBean.customer.lastName}) 
                    </td>
                </tr>
            </table>
                    <br/>              
            <table  class="green" width="99%">
            <c:if test="${s:getListSize(actionBean.cashInList.cashInDataList) == 0 && actionBean.cashInList != null}">
                <tr>
                    <td>
                        <b>
                            <fmt:message key="no.peding.bank.deposit.cashins"/>
                        </b>
                    </td>
                </tr>
            </c:if>

            <c:if test="${s:getListSize(actionBean.cashInList.cashInDataList) > 0}">
                        <tr>
                            <th>CashIn Id</th>
                            <th>CashIn Date</th>
                            <th>Administrator Id</th>
                            <th>Sale Person Id</th>
                            <th>Cash Receipted Amount</th>
                            <th>Cash Required Amount</th>
                            <th>Sales #</th>
                            <th>Process CashIn</th>
                        </tr>
                        <c:forEach items="${actionBean.cashInList.cashInDataList}" var="cashInData" varStatus="loop"> 
                            <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                <td>  
                                    ${cashInData.cashInId}
                                </td>
                                <td>
                                   ${s:formatDateLong(cashInData.cashInDate)}
                                </td>
                                <td>
                                    ${cashInData.salesAdministratorCustomerId}
                                </td>
                                <td>
                                    ${cashInData.salesPersonCustomerId}
                                </td>
                                <td>
                                    ${s:convertCentsToCurrencyLongRoundHalfEven(cashInData.cashReceiptedInCents)}
                                </td>
                                <td>
                                    ${s:convertCentsToCurrencyLongRoundHalfEven(cashInData.cashRequiredInCents)}
                                </td>
                                <td>
                                    ${s:getListSize(cashInData.salesIds)}
                                </td>
                                <td>
                                <stripes:form action="/Sales.action">
                                    <stripes:hidden name="cashInData.cashInType" value="bankdeposit"/>
                                    <stripes:hidden name="cashInData.status" value="${cashInData.status}"/>
                                    <input type="hidden" name="cashInData.cashInId" value="${cashInData.cashInId}"/>
                                    <input type="hidden" name="cashInData.salesAdministratorCustomerId" value="${cashInData.salesAdministratorCustomerId}"/>
                                    <input type="hidden" name="cashInData.salesPersonCustomerId" value="${cashInData.salesPersonCustomerId}"/>
                                    <input type="hidden" name="cashInData.cashReceiptedInCents" value="${cashInData.cashReceiptedInCents}"/>
                                    <input type="hidden" name="cashInData.cashRequiredInCents" value="${cashInData.cashRequiredInCents}"/>
                                    <c:forEach items="${cashInData.salesIds}" var="saleId" varStatus="loop2"> 
                                        <input type="hidden" name="cashInData.salesIds[${loop2.index}]" value="${saleId}"/>
                                    </c:forEach>                                    
                                    <stripes:submit name="showProcessBankDepositCashIn"/>
                                </stripes:form>
                                </td>
                            </tr>    
                        </c:forEach>
             </c:if>
            </table>
        </div>
    </stripes:layout-component>


</stripes:layout-render>

