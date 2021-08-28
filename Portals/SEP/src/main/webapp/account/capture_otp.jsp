<%@include file="/include/sep_include.jsp" %>
<c:set var="title">
    <fmt:message key="otp.page.header"/>
</c:set>
<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        
        <div style="font-size: 18px; margin: 10px auto; line-height:25px; text-align: center; font-family:'Arial';">              
                    <fmt:message key="otp.page.sub.header"/>
        </div>

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
                        <span class="button">
                            <stripes:submit name="skipOtpStep"/> 
                        </span>
                    </td>
                    <td>
                        <span class="button">
                            <stripes:submit name="requestAnotherOtp"/> 
                        </span>
                    </td>
                    <td>
                        <span class="button">
                            <stripes:submit name="submitOTP"/> 
                        </span>
                    </td>
                </tr>
            </table>
        </stripes:form>

    </stripes:layout-component>
</stripes:layout-render>