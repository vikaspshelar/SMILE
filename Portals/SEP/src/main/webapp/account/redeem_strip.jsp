<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="redeem.strip"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        
        
        <stripes:form action="/Account.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">       
            <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/> 
            <stripes:hidden name="prepaidStripRedemptionData.redeemedByAccountId" value="0"/> 
            <stripes:hidden name="prepaidStripRedemptionData.accountId" value="${actionBean.account.accountId}"/> 
            <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/> 
            <table class="clear">
                <tr>
                    <td><stripes:label for="prepaidStripRedemptionData.encryptedPIN"/>:</td>
                    <td><stripes:text name="prepaidStripRedemptionData.encryptedPIN" class="required" size="15" maxlength="15" onkeyup="validate(this,'^[0-9]{15,15}$','')"/></td>
                </tr>
                <tr>  
                    <td>
                        <span class="button">
                            <stripes:submit name="redeemPrepaidStrip"/> 
                        </span>    
                    </td>
                </tr>                                
            </table>            
        </stripes:form>
       
        <c:if test="${!empty actionBean.accountHistory.resultsReturned}">
            <table class="entity_header" style="width: 99%">
                <tr>
                    <td><fmt:message key="transfer.result"/></td>
                </tr>
            </table>
            <table class="green">
                <tr>                   
                    <th><fmt:message key="transaction.id"/></th>
                    <th><fmt:message key="transaction.accountid"/></th>
                    <th><fmt:message key="transaction.amount"/></th>
                    <th><fmt:message key="transaction.balance"/></th>
                    <th><fmt:message key="transaction.datetime"/></th>
                    <th><fmt:message key="transaction.source"/></th>
                    <th><fmt:message key="transaction.description"/></th>
                    <th><fmt:message key="transaction.destination"/></th>                    
                </tr>

                <c:forEach items="${actionBean.accountHistory.transactionRecords}" var="Record" varStatus="loop">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>${Record.transactionRecordId}</td>
                        <td>${Record.accountId}</td>                        
                        <td>${s:convertCentsToCurrencyLong(Record.amountInCents)}</td>
                        <td>${s:convertCentsToCurrencyLong(Record.accountBalanceRemainingInCents)}</td>                        
                        <td>${s:formatDateLong(Record.startDate)}</td>
                        <td>${s:getPhoneNumberFromSipURI(Record.source)}</td>
                        <td>${Record.description}</td>                  
                        <td>${s:getPhoneNumberFromSipURI(Record.destination)}</td>                        
                    </tr>       
                </c:forEach>
            </table>
        </c:if>
           
        <br/><br/>
        <stripes:link href="/Account.action" event="retrieveAccount">                            
            <stripes:param name="accountQuery.accountId" value="${actionBean.account.accountId}"/>
            Go back to Account ${actionBean.account.accountId}
        </stripes:link>
        
    </stripes:layout-component>
    
    
</stripes:layout-render>


