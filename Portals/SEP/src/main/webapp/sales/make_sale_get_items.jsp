<%@page import="com.smilecoms.commons.base.BaseUtils, com.smilecoms.commons.stripes.SmileActionBean"%>
<%@ include file="/include/sep_include.jsp" %>



<c:set var="title">
    <fmt:message key="add.sale.items"/>
</c:set>

<c:set var="curr" value="${actionBean.sale.tenderedCurrency.concat(' ')}"/>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    


    <stripes:layout-component name="contents">

        <table class="clear" width="99%">  
            <tr>
                <td>
                    <div style="width: 95%;" angucomplete-alt placeholder="Type part of KIT, serial number, item number or description..." id="itemsAutoComplete"
                         pause="800"
                         selected-object="itemSelected"
                         maxlength="500"
                         remote-url="Sales.action?getItemsForSaleJSON=&inventoryQuery.recipientAccountId=${actionBean.sale.recipientAccountId}&inventoryQuery.recipientCustomerId=${actionBean.sale.recipientCustomerId}&inventoryQuery.recipientOrganisationId=${actionBean.sale.recipientOrganisationId}&inventoryQuery.currency=${actionBean.sale.tenderedCurrency}&inventoryQuery.stringMatch="
                         remote-url-data-field="inventoryItems"
                         title-field="description"
                         input-class="form-control"
                         clear-selected="true"
                         text-searching="Searching X3 stock..."
                         text-no-results="No results found!"
                         minlength="3"
                         match-class="highlight">
                    </div>
                </td>
            </tr>
        </table>


        <c:forEach items="${actionBean.sale.saleLines}" var="line" varStatus="loop"> 
            <div ng-init="addPastLineToSale(${actionBean.sale.recipientAccountId}, ${actionBean.sale.recipientCustomerId}, ${actionBean.sale.recipientOrganisationId}, '${line.inventoryItem.itemNumber}', '${line.inventoryItem.serialNumber}', '${line.inventoryItem.description}', ${line.quantity}, '${line.inventoryItem.currency}')"></div>
        </c:forEach>

        <br/>

        <stripes:form action="/Sales.action" autocomplete="off" focus="">
            <table class="green" width="99%">            
                <tr>
                    <th>Line</th>
                    <th>Item Number</th>
                    <th>Serial Number</th>
                    <th>Description</th>
                    <th>Unit Price Incl</th>
                    <th>Quantity</th>
                    <th>Line Price Incl</th>
                </tr>
                <tr ng-repeat="item in saleLineItems" ng-class='{red : item.isForUpSize}'>     
                    <td><input type="text" name="sale.saleLines[{{$index}}].lineNumber" value="{{$index + 1}}" readonly="true" size="2"/></td>
                    <td><input type="text" name="sale.saleLines[{{$index}}].inventoryItem.itemNumber" value="{{item.itemNumber}}" readonly="true" size="10"/></td>
                    <td><input type="text" name="sale.saleLines[{{$index}}].inventoryItem.serialNumber" value="{{item.serialNumber}}" readonly="true" size="21"/></td>
                    <td>{{item.description}}</td>
                    <td ng-show="(item.itemNumber.indexOf('BENTMW') == -1 && item.itemNumber.indexOf('OTT') == -1) || ((item.itemNumber.indexOf('BENTMW') == 0) && !isP2PInvoiceOption)" align="right">{{item.priceInCentsIncl / 100| currency:"${curr}"}}</td>
                    <td ng-show="(item.itemNumber.indexOf('OTT') == 0 && !isP2PInvoiceOption)" align="right">{{item.unitPriceInCentsIncl / 100| currency:"${curr}"}}</td>
                    <td ng-show="(item.itemNumber.indexOf('BENTMW') == 0 || item.itemNumber.indexOf('OTT') == 0) && isP2PInvoiceOption" align="right">{{item.unitPriceInCentsIncl / 100| currency:"${curr}"}}</td>
                    <td ng-show="(item.itemNumber.indexOf('BENTMW') == -1 && item.itemNumber.indexOf('OTT') == -1) || ((item.itemNumber.indexOf('BENTMW') == 0 || item.itemNumber.indexOf('OTT') == 0) && !isP2PInvoiceOption)" align="right"><input type="text" ng-model="item.quantity" ng-change="checkForUpSize({{$index}})" size="7" name="sale.saleLines[{{$index}}].quantity"/></td>
                    <td ng-show="(item.itemNumber.indexOf('BENTMW') == 0 || item.itemNumber.indexOf('OTT') == 0) && isP2PInvoiceOption" align="right"><input type="text" ng-model="item.quantity"  readonly="true" size="7" name="sale.saleLines[{{$index}}].quantity"/></td>
                    <td ng-show="(item.itemNumber.indexOf('BENTMW') == -1 && item.itemNumber.indexOf('OTT') == -1) || ((item.itemNumber.indexOf('BENTMW') == 0) && !isP2PInvoiceOption)" align="right">{{item.quantity * item.priceInCentsIncl / 100| currency:"${curr}"}}</td>
                    <td ng-show="(item.itemNumber.indexOf('OTT') == 0 && !isP2PInvoiceOption)" align="right">{{item.quantity * item.unitPriceInCentsIncl / 100| currency:"${curr}"}}</td>
                    <td ng-show="(item.itemNumber.indexOf('BENTMW') == 0 || item.itemNumber.indexOf('OTT') == 0) && isP2PInvoiceOption" align="right">{{item.quantity * item.unitPriceInCentsIncl / 100| currency:"${curr}"}}</td>
                </tr>
                <tr class="even">
                    <td colspan="7"></td>
                </tr>
                <tr>
                    <td colspan="4"></td>
                    <td>Tax</td>
                    <td align="right" colspan="2">{{getSaleTotalTax() | currency:"${curr}"}}</td>
                </tr>
                <tr>
                    <td colspan="4"></td>
                    <td><b>Total Incl Tax</b></td>
                    <td align="right" colspan="2"><b>{{getSaleTotalIncl() | currency:"${curr}"}}</b></td>
                </tr>
                <tr>
                    <c:if test="${actionBean.sale.recipientAccountId == 0}">
                        <td colspan="7" style="background-color: red; font-weight: bold; text-align: center">WARNING: No Recipient Account has been selected. Airtime/Unit Credits sold will only be transferred upon SIM provisioning</td>
                    </c:if>
                    <c:if test="${actionBean.sale.recipientAccountId > 0}">
                        <td colspan="7">Default Recipient Account: ${actionBean.sale.recipientAccountId}</td>
                    </c:if>
                </tr>
                <% if (BaseUtils.getBooleanProperty("env.pos.sales.freelance.model.enabled", false)) {%>
                    <c:if test="${actionBean.salesPersonsFreelancers.size() > 0}">
                        <tr>
                            <td colspan="7">
                                Freelance Sales Person:&nbsp;
                                <select name="sale.freelancerCustomerId">
                                    <option value="-1"></option>
                                    <c:forEach items="${actionBean.salesPersonsFreelancers}" var="freelancerEntry">
                                        <option value="${freelancerEntry.key}">${freelancerEntry.value}</option>
                                    </c:forEach>
                                </select> 
                            </td>
                        </tr>
                    </c:if>
                <% } %>
                <tr >
                    <td colspan="7">
                        Promotion Code:&nbsp;
                        <stripes:select name="sale.promotionCode">
                            <stripes:option value=""></stripes:option>
                            <c:forEach items="${s:getRoleBasedSubsetOfPropertyAsSet('env.promotion.codes.sales', actionBean.context.request)}" var="code">
                                <stripes:option value="${code}">${code}</stripes:option>
                            </c:forEach>
                        </stripes:select>
                        <br/> 
                        Comment relating to the use of the promotion code:<br/>
                        <stripes:textarea name="sale.SCAContext.comment" rows="3" cols="80"></stripes:textarea>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="7">
                            Days to Keep Invoice Open:&nbsp;
                        <stripes:select name="invoiceExpiryDays">
                            <stripes:option value="7">7</stripes:option>
                            <c:forEach items="${s:getRoleBasedSubsetOfPropertyAsSet('env.pos.invoice.expiry.days.options', actionBean.context.request)}" var="days">
                                <stripes:option value="${days}">${days}</stripes:option>
                            </c:forEach>
                        </stripes:select>
                    </td>
                </tr>
                <tr>
                    <td colspan="7">Customer is Tax Exempt: <stripes:checkbox name="sale.taxExempt" checked="false"/></td>
                </tr>
                <tr>
                    <td colspan="7">
                        Withholding Tax %: 
                        <stripes:select name="sale.withholdingTaxRate">
                            <c:forEach items="${s:getPropertyAsList('env.pos.withholding.tax.rates')}" var="rate">
                                <stripes:option value="${rate}">${rate}</stripes:option>
                            </c:forEach>
                        </stripes:select>
                    </td>
                </tr>
                <tr>
                    <td colspan="7">
                        Credit Account Number (for overriding): 
                        <stripes:text name="sale.creditAccountNumber" value="" size="6" maxlength="6"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="7">
                        Payment Gateway (for testing): 
                        <stripes:text name="sale.paymentGatewayCode" value="" size="20" maxlength="20"/>
                    </td>
                </tr>
                <tr ng-show="isP2PInvoicing">
                    <td colspan="7">
                        <b>P2P Calendar Invoicing(s):&nbsp;</b>
                        <input type="checkbox" name="P2PCalendarInvoicing" ng-model="isP2PInvoiceOption" ng-change="daysInMonth(p2pInvoicingDate)">
                        <input ng-show="isP2PInvoiceOption" type="text" name="P2PInvoicingPeriod" sep-datepicker="" ng-model="p2pInvoicingDate" ng-change="daysInMonth(p2pInvoicingDate)" />
                    </td>
                </tr>
                <tr>
                    <td colspan="7">
                        Unique Id (for testing): 
                        <stripes:text name="sale.uniqueId" value="" size="20" maxlength="20"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="7">
                        Returned Device Serial#: 
                        <stripes:text name="sale.purchaseOrderData" value="" size="50" maxlength="50"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="7">Note: You are selling from Warehouse Id ${actionBean.sale.warehouseId}</td>
                </tr>
            </table>                

            <stripes:hidden name="sale.recipientCustomerId" value="${actionBean.sale.recipientCustomerId}"/>
            <stripes:hidden name="sale.recipientAccountId" value="${actionBean.sale.recipientAccountId}"/>
            <stripes:hidden name="sale.recipientOrganisationId" value="${actionBean.sale.recipientOrganisationId}"/>
            <stripes:hidden name="sale.tenderedCurrency" value="${actionBean.sale.tenderedCurrency}"/>
                        
            <div ng-show="!isP2PInvoiceOption">
                <stripes:submit name="generateQuote"/>
            </div>
            <div ng-show="isP2PInvoiceOption">
                <stripes:submit name="generateP2PQuote"/>
            </div>
            
            

        </stripes:form>
    </stripes:layout-component>


</stripes:layout-render>

