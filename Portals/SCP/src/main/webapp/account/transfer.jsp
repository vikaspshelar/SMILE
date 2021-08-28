<%@include file="/include/scp_include.jsp" %>
<c:set var="title">
    <fmt:message key="scp.transfer.m2u.peer"/>
</c:set>
<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function() {
            makeMenuActive('Profile_Me2UPage');
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <div style="margin-top: 10px;" class="sixteen columns">
            <script type="text/javascript" xml:space="preserve">
                var submitClicked = false;</script>

            <jsp:include page="/layout/my_details_left_banner.jsp"/>
            <%-- <div class="accounts_table nine columns">--%>
            <div style="margin-top: 6px; float: left; margin-left: 100px;" class="accounts_transfer_table six columns">
                <c:choose>
                    <c:when test="${empty actionBean.accountHistory.resultsReturned}">
                        <stripes:form action="/Account.action" onsubmit=" return alertValidationErrors();" autocomplete="off">
                            <font style="color:#75B343; font-weight:bold;"><fmt:message key="scp.dotransfer.source.account"/>:</font><br/>

                            <input type="text" name="balanceTransferData.sourceAccountId" class="required" size="20" list="items">
                            <datalist id="items">
                                <c:forEach items="${actionBean.accountList.accounts}" var="account">
                                    <option value="${account.accountId}"/>
                                </c:forEach>     
                            </datalist>
                            &nbsp; Double-Click or Hit Down Arrow to see options
                            <br/>
                            <br/>
                            <div ng-show="me2uAllInActive">
                                <font  ng-show="me2uAllInActive" style="color:#75B343; font-weight:bold;"><fmt:message key="scp.dotransfer.recipient"/>:</font><br/>
                                <input type="text" class="frm_input" name="balanceTransferData.targetAccountId" maxlength="10"  ng-show="me2uAllInActive" ng-model="targetAccountId" ng-change="getTargetServiceInstanceUserName()" ng-init="me2uAllInActive = true" onkeyup="validate(this, '^[0-9]{10,10}$', '');"/><br/>
                                <label ng-model="targetCustomerWithProduct">
                                    <div ng-show="targetCustomerWithProduct.customerWithProduct.productInstanceUserName != '' && targetCustomerWithProduct.customerWithProduct.productNames == null" ng-style="{ color:'red' }">
                                        {{targetCustomerWithProduct.customerWithProduct.productInstanceUserName}}
                                    </div>
                                    <div ng-show="targetCustomerWithProduct.customerWithProduct.productNames != null && targetCustomerWithProduct.customerWithProduct.productInstanceUserName != null" ng-style="{ color:'green' }">
                                        {{targetCustomerWithProduct.customerWithProduct.productInstanceUserName}} {{targetCustomerWithProduct.customerWithProduct.productNames}} 
                                    </div>
                                </label>
                                <br/>

                                <font ng-show="me2uAllInActive" style="color:#75B343; font-weight:bold;">
                                <fmt:message key="scp.label.topup.own.amount">
                                    <fmt:param value="${s:getProperty('env.locale.currency.majorunit')}"/>
                                </fmt:message>
                                </font>
                                <br/>
                                <input type="text" name="balanceTransferData.amountInCents" maxlength="20" ng-show="me2uAllInActive" onkeyup="validate(this, '^[0-9]{1,20}\.[0-9]{2,2}$', '')"/>
                            </div>
                            <c:if test="${actionBean.accountList.numberOfAccounts > 1}">
                                <br/>
                                <font ng-show="me2uAllInActive" style="font-weight:bold;">OR</font>
                                <br/>
                                <font style="color:#75B343;font-weight:bold;"><fmt:message key="scp.transfer.toall.accounts"/>:<input type="checkbox" name="me2uAllAccounts"  value="Me2UAll" ng-model="me2uAllAccounts" ng-true-value="'YES'" ng-false-value="'NO'" ng-change="enableMe2UToAll(me2uAllAccounts)"/></font>
                                <br/>
                                <br/>


                                <div ng-init="getMe2UApplicableAccounts(0)" ng-show="showAllMe2UAccounts">
                                    <table class="greentbl" width="99%">            
                                        <tr>
                                            <th><fmt:message key="scp.transfer.toaccount"/></th>
                                            <th><fmt:message key="scp.airtime.balance"/></th>
                                            <th><fmt:message key="scp.bundle.balance"/></th>
                                            <th><fmt:message key="scp.transfer.toaccount.amount"/></th>
                                        </tr>
                                        <tr ng-repeat="account in Me2UAccountList.accountList.accounts">      
                                            <td>{{account.accountId}}</td>
                                            <td>{{account.availableBalance}}</td>
                                            <td>{{account.dataBundleBalance}}</td>
                                            <td>
                                                <font style="font-weight:bold;">${s:getProperty('env.locale.currency.majorunit')}</font><input type="text" name="balanceTransferData.additionalTransferLines[{{$index}}].amountInCents" onkeyup="validate(this, '^[0-9]{1,20}\.[0-9]{2,2}$', '')" placeholder="Enter amount"/>
                                                <input type="hidden" name="balanceTransferData.additionalTransferLines[{{$index}}].targetAccountId"  value="{{account.accountId}}"/>
                                            </td>
                                        </tr>

                                        <tr class="odd">
                                            <td colspan="4"></td>
                                        </tr>
                                    </table>                
                                </div>
                            </c:if>
                            <br/>
                            <br/>
                            <c:if test="${s:getPropertyWithDefault('env.scp.me2u.enabled','true') == 'true' || s:setContains(s:getPropertyAsSet('env.uc.diaspora.icps'), actionBean.getOrganistionIdOfUserInSession())}">
                                <input  class="transfer_btn" type="submit" name="performTransfer" value=""/>
                            </c:if>
                            

                        </stripes:form>
                    </c:when> 

                    <c:when test="${!empty actionBean.accountHistory.resultsReturned}">

                        <table style="width: 99%">
                            <tr>
                                <td><fmt:message key="transfer.result"/></td>
                            </tr>
                        </table>

                        <table class="greentbl">
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
                        <stripes:form action="/Account.action">
                            <table>
                                <tr>                    
                                    <td>
                                        <input  class="transfer_btn" type="submit" name="displayTransferPage" value="">
                                    </td>
                                    <td>
                                        <span>
                                            <stripes:submit name="retrieveAllUserServicesAccounts"/> 
                                        </span>
                                    </td>
                                </tr>
                            </table>
                        </stripes:form>

                    </c:when>

                </c:choose>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>


