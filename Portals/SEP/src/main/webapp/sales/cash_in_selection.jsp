<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="cash.in"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">



        <table class="clear">                              
            <tr>
                <th>Cash In Description</th>
                <th>Proceed</th>
            </tr>
            <tr>
                <td>
                    <fmt:message key="cash.in.description.office"/>
                </td>
                <td>
                    <stripes:form action="/Sales.action" focus="" autocomplete="off"> 
                        <stripes:hidden name="cashInData.cashInType" value="office"/>
                        <span class="button">
                            <stripes:submit name="showCashInOffice"/>
                        </span> 
                    </stripes:form>
                </td>
            </tr>
            <c:if test="${s:getPropertyWithDefault('env.cashin.bank.deposit.allowed', 'false') == 'true'}">
                <tr>
                    <td>
                        <fmt:message key="cash.in.description.bank.deposit"/>
                    </td>
                    <td>
                        <stripes:form action="/Sales.action" focus="" autocomplete="off">
                            <stripes:hidden  name="cashInData.cashInType" value="bankdeposit"/>
                            <span class="button">
                                <stripes:submit name="showCashInBankDeposit"/>
                            </span> 
                        </stripes:form>
                    </td>
                </tr>
                <tr>
                    <td>
                        <fmt:message key="cash.in.description.bank.deposit"/>
                    </td>
                    <td>
                        <stripes:form action="/Sales.action" focus="" autocomplete="off">
                            <stripes:hidden  name="cashInQuery.cashInType" value="bankdeposit"/>
                            <stripes:hidden  name="cashInQuery.status" value="BDP"/>
                            <span class="button">
                                <stripes:submit name="showPendingBankDepositCashIns"/>
                            </span> 
                        </stripes:form>
                    </td>
                </tr>
            </c:if>
        </table>

    </stripes:layout-component>


</stripes:layout-render>

