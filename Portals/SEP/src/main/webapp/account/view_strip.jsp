<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="view.strip"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">


        <stripes:form action="/Account.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">
            <stripes:hidden name="prepaidStripQuery.encryptedPINHex" value="0"/>
            <table class="clear">
                <tr>
                    <td><stripes:label for="prepaidStripQuery.prepaidStripId"/>:</td>
                    <td><stripes:text name="prepaidStripQuery.prepaidStripId" class="required" size="15" maxlength="15" onkeyup="validate(this,'^[0-9]{1,10}$','')"/></td>
                </tr>
                <tr>  
                    <td>
                        <span class="button">
                            <stripes:submit name="retrieveStrip"/> 
                        </span>    
                    </td>
                </tr>                                
            </table>            
        </stripes:form>

        <br/>

        <c:if test="${actionBean.prepaidStrip != null}">
            <div id="entity">
                <table class="entity_header">
                    <tr>
                        <td>
                            <fmt:message key="strip">
                                <fmt:param value="${actionBean.prepaidStripQuery.prepaidStripId}"></fmt:param>
                            </fmt:message>
                        </td>
                    </tr>
                </table>
                <table class="clear">                
                    <tr>
                        <td colspan="2"><b><fmt:message key="strip.data"/></b></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="status"/>:</td>
                        <td>${actionBean.prepaidStrip.status}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="value"/>:</td>
                        <td>${s:convertCentsToCurrencyLong(actionBean.prepaidStrip.valueInCents)}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="invoice.data"/>:</td>
                        <td>${actionBean.prepaidStrip.invoiceData}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="generated.date"/>:</td>
                        <td>${s:formatDateLong(actionBean.prepaidStrip.generatedDate)}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="expiry.date"/>:</td>
                        <td>${s:formatDateLong(actionBean.prepaidStrip.expiryDate)}</td>
                    </tr>
                    <c:if test="${actionBean.prepaidStrip.redemptionAccountId != 0}">
                        <tr>
                            <td><fmt:message key="redemption.account"/>:</td>
                            <td>
                                <stripes:link href="/Account.action" event="retrieveAccount" addSourcePage="true"> 
                                    <stripes:param name="accountQuery.accountId" value="${actionBean.prepaidStrip.redemptionAccountId}"/>
                                    ${actionBean.prepaidStrip.redemptionAccountId}
                                </stripes:link>
                            </td>
                        </tr>
                    </c:if>
                </table>



                <br/>

                <c:if test="${!empty actionBean.accountHistory.resultsReturned}">
                    <table class="entity_header" style="width: 99%">
                        <tr>
                            <td><fmt:message key="strip.redemption.data"/></td>
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

            </div>
        </c:if>

        <br/>


    </stripes:layout-component>


</stripes:layout-render>


