<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="return.sale"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <table class="clear">     
            <c:if test="${actionBean.customer != null}">
                <tr>
                    <td colspan="2"><b>Customer</b></td>
                </tr>
                <tr>
                    <td>Customer Name:</td>
                    <td>
                        <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                            <stripes:param name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                            ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                        </stripes:link>
                    </td>
                </tr>
            </c:if>


            <c:if test="${actionBean.organisation != null}">
                <tr>
                    <td colspan="2"><b>Organisation</b></td>
                </tr>
                <tr>
                    <td>Organisation Name:</td>
                    <td>${actionBean.organisation.organisationName}</td>
                </tr>
            </c:if>

            <c:if test="${actionBean.sale.recipientAccountId > 0}">
                <tr>
                    <td>Account Id:</td>
                    <td>
                        <stripes:link href="/Account.action" event="retrieveAccount"> 
                            <stripes:param name="accountQuery.accountId" value="${actionBean.sale.recipientAccountId}"/>
                            ${actionBean.sale.recipientAccountId}
                        </stripes:link>
                    </td>
                </tr>
            </c:if>


            <tr>
                <td colspan="2"><b>Other</b></td>
            </tr>
            <tr>
                <td>Sale Id:</td>
                <td>
                    <stripes:link href="/Sales.action" event="showSale"> 
                        <stripes:param name="sale.saleId" value="${actionBean.sale.saleId}"/>
                        ${actionBean.sale.saleId}
                    </stripes:link>
                </td>
            </tr>
            <tr>
                <td>Payment Method:</td>
                <td>${actionBean.sale.paymentMethod}</td>
            </tr>
            <tr>
                <td>Sale Status:</td>
                <td><fmt:message key="sale.status.${actionBean.sale.status}"/></td>
            </tr>
            <tr>
                <td>Sales Person:</td>
                <td>
                    <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                        <stripes:param name="customerQuery.customerId" value="${actionBean.salesPerson.customerId}"/>
                        ${actionBean.salesPerson.firstName} ${actionBean.salesPerson.lastName}
                    </stripes:link>
                </td>
            <tr>
                <td>Sales Person Account:</td>
                <td>
                    <stripes:link href="/Account.action" event="retrieveAccount"> 
                        <stripes:param name="accountQuery.accountId" value="${actionBean.sale.salesPersonAccountId}"/>
                        ${actionBean.sale.salesPersonAccountId}
                    </stripes:link>
                </td>
            </tr>
            <c:if test="${!empty(actionBean.sale.cashInDate)}">
                <tr>
                    <td>Cash In Date:</td>
                    <td>${s:formatDateLong(actionBean.sale.cashInDate)}</td>
                </tr>
            </c:if>
            <c:if test="${!empty(actionBean.sale.purchaseOrderData)}">
                <tr>
                    <td>Purchase Order:</td>
                    <td>${actionBean.sale.purchaseOrderData}</td>
                </tr>
            </c:if>
            <c:if test="${!empty(actionBean.sale.paymentTransactionData)}">
                <tr>
                    <td>Payment Transaction Data:</td>
                    <td>${actionBean.sale.paymentTransactionData}</td>
                </tr>
            </c:if>
            <c:if test="${actionBean.sale.invoicePDFBase64 != null}">
                <tr>
                    <td>
                        <c:if test="${actionBean.sale.paymentMethod == 'Quote'}">
                            Quotation:
                        </c:if>
                        <c:if test="${actionBean.sale.paymentMethod != 'Quote'}">
                            Invoice:
                        </c:if>
                    </td>
                    <td>
                        <stripes:link href="/Sales.action" event="getSaleInvoice"> 
                            <stripes:param name="salesQuery.salesIds[0]" value="${actionBean.sale.saleId}"/>
                            Click here to download as a pdf
                        </stripes:link>
                    </td>
                </tr>
            </c:if>

            <tr>
                <td>Promotion Code:</td>
                <td>${actionBean.sale.promotionCode}</td>
            </tr>


        </table>    

        <br/>
        <stripes:form action="/Sales.action" focus="">    
            <table class="green" width="99%">            
                <tr>
                    <th>Line</th>
                    <th>Item Number</th>
                    <th>Serial Number</th>
                    <th>Description</th>
                    <th>Unit Price Incl</th>
                    <th>Quantity</th>
                    <th>Discount Incl</th>
                    <th>Line Price Incl</th>
                    <th>Return</th>
                </tr>
                <c:forEach items="${actionBean.sale.saleLines}" var="line"> 
                    <tr>   
                        <td>${line.lineNumber}</td>
                        <td>${line.inventoryItem.itemNumber}</td>
                        <td>${line.inventoryItem.serialNumber}</td>
                        <td>${line.inventoryItem.description}</td>
                        <td align="right">${s:convertCentsToCurrencyLong(line.inventoryItem.priceInCentsIncl)}</td>
                        <td align="right">
                            <div ng-show="'${line.inventoryItem.itemNumber}'.indexOf('BENTMW') == -1 || ('${line.inventoryItem.itemNumber}'.indexOf('BENTMW') == 0 && '${line.provisioningData}'.indexOf('P2PCalendarInvoicing') == -1)">
                                ${line.quantity}
                            </div>
                            
                            <div ng-show="'${line.inventoryItem.itemNumber}'.indexOf('BENTMW') == 0 && '${line.provisioningData}'.indexOf('P2PCalendarInvoicing') == 0">
                                <stripes:text name="quantity_${line.lineId}" value="${line.quantity}" size="8"/>
                            </div>
                        </td>
                        <td align="right">${s:convertCentsToCurrencyLong(line.lineTotalDiscountOnInclCents)}</td>
                        <td align="right">${s:convertCentsToCurrencyLong(line.lineTotalCentsIncl)}</td>
                        <c:if test="${s:getListSize(line.subSaleLines) != 0}">
                            <td></td>
                        </c:if>
                        <c:if test="${s:getListSize(line.subSaleLines) == 0}">
                            <td><stripes:checkbox name="return_${line.lineId}" checked="false"/></td>
                        </c:if>
                    </tr>
                    <c:forEach items="${line.subSaleLines}" var="subline"> 
                        <tr>   
                            <td>${subline.lineNumber}</td>
                            <td>${subline.inventoryItem.itemNumber}</td>
                            <td>${subline.inventoryItem.serialNumber}</td>
                            <td>${subline.inventoryItem.description}</td>
                            <td align="right">${s:convertCentsToCurrencyLong(subline.inventoryItem.priceInCentsIncl)}</td>
                            <td align="right">${subline.quantity}</td>
                            <td align="right">${s:convertCentsToCurrencyLong(subline.lineTotalDiscountOnInclCents)}</td>
                            <td align="right">${s:convertCentsToCurrencyLong(subline.lineTotalCentsIncl)}</td>
                            <td><stripes:checkbox name="return_${subline.lineId}" checked="false"/></td>
                        </tr>
                    </c:forEach>
                </c:forEach>
            </table>                

            <br/>
            <br/>
            <table class="clear">
                <tr>
                    <td colspan="2"><b>Logistics Data</b></td>
                </tr>
                <tr>
                    <td>Return to Warehouse Id:</td>
                    <td>
                        <stripes:text  name="returnData.returnLocation" size="10" maxlength="10" value="${actionBean.defaultReturnLocation}"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="2"><b>Return Reason</b></td>
                </tr>
                <tr>
                    <td>Reason Code:</td>
                    <td>
                        <stripes:select name="returnData.reasonCode">
                            <c:forEach items="${s:getPropertyAsList('env.pos.return.reason.codes')}" var="code">                                   
                                <stripes:option value="${code}">
                                    ${code}
                                </stripes:option>
                            </c:forEach>
                        </stripes:select>
                    </td>
                </tr>
                <tr>
                    <td>Reason Description:</td>
                    <td><stripes:textarea  rows="5" name="returnData.description" cols="50"/></td>
                </tr>
                <tr>
                    <td>
                        <span class="button">
                            <stripes:submit name="processSaleReturn"/>
                        </span>
                    </td>
                </tr>
            </table>       
            <stripes:hidden name="returnData.saleId" value="${actionBean.sale.saleId}"/>

        </stripes:form>


    </stripes:layout-component>


</stripes:layout-render>

