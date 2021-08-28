

<%@include file="/include/sep_include.jsp" %>
<c:set var="title">
    <fmt:message key="transfer"/>
</c:set>
<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <stripes:form action="/Account.action" onsubmit="submitClicked = true; return alertValidationErrors();" autocomplete="off">
            <table class="clear">
                <tr>
                    <td><stripes:label for="dotransfer.supplier"/>: </td>
                    <td>

                        <input type="text" name="balanceTransferData.sourceAccountId"  size="10" maxlength="10" class="required" ng-model="sourceAccountId" ng-change="getSourceServiceInstanceUserName()" onkeyup="validate(this, '^[0-9]{10,10}$', '');">

                        <c:choose>
                            <c:when test="${s:getListSize(actionBean.accountIdList) > 0}">
                                <stripes:select name="srcAccId" id="srcAccIdDropDown" ng-model="sourceAccountId" ng-change="getSourceServiceInstanceUserName()" class="required">
                                    <stripes:option value="" selected="selected">-- Select Account --</stripes:option>
                                    <c:forEach items="${actionBean.accountIdList}" var="account">
                                        <stripes:option value="${account}">
                                            ${account}                                 
                                        </stripes:option>
                                    </c:forEach>                           
                                </stripes:select>
                            </c:when>
                            <c:otherwise>
                                <c:if test="${!actionBean.transferFromAnyAccount}">
                                    <stripes:select name="balanceTransferData.sourceAccountId" class="required" ng-model="sourceAccountId" ng-change="getSourceServiceInstanceUserName()">
                                        <stripes:option value="">-- No Account --</stripes:option>
                                    </stripes:select>
                                </c:if>
                            </c:otherwise>
                        </c:choose>
                    </td> 
                    <td id="lblFromName" ng-model="sourceCustomerWithProduct">
                        <%--
                            Example of json:
                            {"customerWithProduct":{"productInstanceUserName":"admin admin","productNames":"Corporate Sales"}}
                            or
                            {"customerWithProduct":{"productInstanceUserName":"unknown"}}
                        --%>
                        <div ng-show="sourceCustomerWithProduct.customerWithProduct.productInstanceUserName != '' && sourceCustomerWithProduct.customerWithProduct.productNames == null" ng-style="{ color:'red' }">
                            {{sourceCustomerWithProduct.customerWithProduct.productInstanceUserName}}
                        </div>
                        <div ng-show="sourceCustomerWithProduct.customerWithProduct.productNames != null && sourceCustomerWithProduct.customerWithProduct.productInstanceUserName != null" ng-style="{ color:'green' }">
                            {{sourceCustomerWithProduct.customerWithProduct.productInstanceUserName}} {{sourceCustomerWithProduct.customerWithProduct.productNames}}
                        </div>
                    </td>
                </tr>

                <tr>
                    <td><stripes:label for="dotransfer.recipient"/>: </td>
                    <td>
                        <input type="text" name="balanceTransferData.targetAccountId"  size="10" maxlength="10" class="required" ng-model="targetAccountId" ng-change="getTargetServiceInstanceUserName()" onkeyup="validate(this, '^[0-9]{10,10}$', '');">
                    </td>                                                                                                               
                    <td id="lblToName" ng-model="targetCustomerWithProduct">
                        <%--
                            Example of json:
                            {"customerWithProduct":{"productInstanceUserName":"admin admin","productNames":"Corporate Sales"}}
                            or
                            {"customerWithProduct":{"productInstanceUserName":"unknown"}}
                        --%>
                        <div ng-show="targetCustomerWithProduct.customerWithProduct.productInstanceUserName != '' && targetCustomerWithProduct.customerWithProduct.productNames == null" ng-style="{ color:'red' }">
                            {{targetCustomerWithProduct.customerWithProduct.productInstanceUserName}}
                        </div>
                        <div ng-show="targetCustomerWithProduct.customerWithProduct.productNames != null && targetCustomerWithProduct.customerWithProduct.productInstanceUserName != null" ng-style="{ color:'green' }">
                            {{targetCustomerWithProduct.customerWithProduct.productInstanceUserName}} {{targetCustomerWithProduct.customerWithProduct.productNames}} 
                        </div>
                    </td>
                </tr>
                <tr>
                    <td>
                        <fmt:message key="label.topup.own.amount">
                            <fmt:param value="${s:getProperty('env.locale.currency.majorunit')}"/>
                        </fmt:message>
                    </td>
                    <td><stripes:text name="balanceTransferData.amountInCents" class="required" maxlength="20" onkeyup="validate(this,'^[0-9]{1,20}\.[0-9]{2,2}$','')"/></td>
                </tr>
                <tr>
                    <td>Description:</td>
                    <td><stripes:text name="balanceTransferData.description" maxlength="200" size="50"/></td>
                </tr>
                <tr>                    
                    <td>
                        <span class="button">
                            <stripes:submit name="doBalanceTransfer"/> 
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
                        <td>${Record.description}</td>                  
                        <td>${s:getPhoneNumberFromSipURI(Record.destination)}</td>                        
                    </tr>       
                </c:forEach>
            </table>
        </c:if>
        <br/>
        <c:if test="${actionBean.transferRecipientKYCStatus == 'U'}">
            <table class="clear">     
                <tr class="red">
                    <td>
                        <b>
                            <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                <stripes:param name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                            </stripes:link> 
                            has not been properly KYC Registered. Please update the customers fingerprints and photograph to ensure compliance.
                        </b>
                    </td>
                </tr>
            </table>
        </c:if>


    </stripes:layout-component>
</stripes:layout-render>