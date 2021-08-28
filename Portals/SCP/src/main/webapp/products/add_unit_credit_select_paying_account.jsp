<%@include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="customer"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp">

    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function () {
                makeMenuActive('Buy Smile Bundle');
            };

        </script>

    </stripes:layout-component>


    <stripes:layout-component name="contents">
        <div style="margin-top: 4px;" class="sixteen columns alpha">
            <div style="margin-top: 10px;" class="sixteen columns alpha">

                <table class="greentbl" width="99%">
                    <tr>
                        <th colspan="3">Please reconfirm and select paying account</th>
                    </tr>  

                    <c:forEach items="${actionBean.accountList.accounts}" var="account" varStatus="loop">                        
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            <td> 
                                ${account.accountId}
                            </td> 

                            <td>
                                ${s:convertCentsToCurrencyLong(account.availableBalanceInCents)}
                            </td>

                            <td style="width: 100px;">
                                <div id="link_button">
                                    <p style="color: white;">
                                        <stripes:form action="/Account.action" autocomplete="off">
                                            <c:set var="isAirtimeTransaction" value="false"/>
                                            <c:choose>
                                                <c:when test="${!empty actionBean.purchaseUnitCreditRequest.unitCreditSpecificationId && actionBean.purchaseUnitCreditRequest.unitCreditSpecificationId > 0}">
                                                    <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/>
                                                    <stripes:hidden name="purchaseUnitCreditRequest.accountId" value="${actionBean.account.accountId}"/>
                                                    <stripes:hidden name="purchaseUnitCreditRequest.unitCreditSpecificationId" value="${actionBean.unitCreditSpecification.unitCreditSpecificationId}"/>
                                                    <stripes:hidden name="purchaseUnitCreditRequest.productInstanceId" value="${actionBean.unitCreditSpecification.unitCreditSpecificationId}"/>
                                                    <stripes:hidden name="sale.recipientCustomerId" value="${actionBean.customer.customerId}"/>
                                                    <stripes:hidden name="sale.recipientAccountId" value="${actionBean.account.accountId}"/>
                                                </c:when>
                                                <c:otherwise>
                                                    <c:set var="isAirtimeTransaction" value="true"/>
                                                    <stripes:hidden name="sale.recipientCustomerId" value="${actionBean.customer.customerId}"/>
                                                    <stripes:hidden name="sale.saleTotalCentsIncl" value="${s:convertCentsToLong(actionBean.sale.saleTotalCentsIncl)}"/>
                                                    <stripes:hidden name="sale.recipientAccountId" value="${actionBean.account.accountId}"/>
                                                    <c:forEach items="${actionBean.sale.saleLines}" var="line" varStatus="loop"> 
                                                        <stripes:hidden name="sale.saleLines[${loop.index}].lineTotalCentsIncl" value="${s:convertCentsToLong(line.lineTotalCentsIncl)}"/>
                                                        <stripes:hidden name="sale.saleLines[${loop.index}].lineNumber" value="${line.lineNumber}"/>
                                                        <stripes:hidden name="sale.saleLines[${loop.index}].inventoryItem.itemNumber" value="${line.inventoryItem.itemNumber}"/>
                                                        <stripes:hidden name="sale.saleLines[${loop.index}].inventoryItem.serialNumber" value="${line.inventoryItem.serialNumber}"/>
                                                        <stripes:hidden name="sale.saleLines[${loop.index}].inventoryItem.description" value="${line.inventoryItem.description}"/>
                                                        <stripes:hidden name="sale.saleLines[${loop.index}].quantity" value="${line.quantity}"/>
                                                    </c:forEach>
                                                </c:otherwise>
                                            </c:choose>

                                            <c:if test="${isAirtimeTransaction != 'true'}">
                                                <input type="hidden" name="purchaseUnitCreditRequest.paidByAccountId" value="${account.accountId}"/>
                                                <input type="submit" name="provisionUnitCreditByPaymentType" class="button_proceed" value="Proceed"/>
                                                <input type="hidden" name="gatewayCode" value="${actionBean.TPGWPartnerCode}" />
                                            </c:if>

                                            <c:if test="${isAirtimeTransaction == 'true'}">
                                                <input type="submit" name="buyAirtimeViaPaymentGateway" class="button_proceed" value="Proceed"/>
                                                <input type="hidden" name="gatewayCode" value="${actionBean.TPGWPartnerCode}" />
                                            </c:if>
                                        </stripes:form>
                                    </p>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                </table>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
