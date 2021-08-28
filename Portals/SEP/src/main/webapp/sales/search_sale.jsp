<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="search.sales"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">


    <stripes:layout-component name="html_head">        
        <style type="text/css">
            .ui-datepicker {font-size:12px;}

            /*----- Tabs -----*/
            .tabs {
                width:100%;
                margin-left: 0;
                display:inline-block;
            }

            /*----- Tab Links -----*/
            /* Clearfix */
            .tab-links:after {
                display:block;
                clear:both;
                content:'';
            }

            .tab-links li {
                margin:0px 2px;
                color: #99cc33;
                float:left;
                list-style:none;
            }

            .tab-links a {
                padding:2px 8px;
                display:inline-block;
                border-radius:3px 3px 0px 0px;
                background:#e4ecc3;
                font-size:12px;
                font-weight:600;
                color:#99cc33;
                transition:all linear 0.15s;
            }

            .tab-links a:hover {
                background:#000000;
                text-decoration:none;
            }

            li.active a, li.active a:hover {
                background:#fff;
                color:#4c4c4c;
            }

            /*----- Content of Tabs -----*/
            .tab-content {
                padding:2px;
                border-radius:3px;
                box-shadow:-1px 1px 1px rgba(0,0,0,0.15);
                background:#fff;
            }

            .tab {
                display:none;
            }

            .tab.active {
                display:block;
            }
        </style>
        <script type="text/javascript">
            var $j = jQuery.noConflict();
            var dateNow = new Date();

            $j(document).ready(function () {
                $j("#dateFrom").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", maxDate: dateNow, changeYear: true, changeMonth: true});
            });
            $j(document).ready(function () {
                $j("#dateTo").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", maxDate: dateNow, changeYear: true, changeMonth: true});
            });
        </script>
    </stripes:layout-component>


    <stripes:layout-component name="contents">


        <stripes:form action="/Sales.action" focus="" autocomplete="off" onsubmit="return alertValidationErrorsForElement(this);">    
            <table class="clear">
                <tr>
                    <td><fmt:message key="sale.id"/>:</td>
                    <td><stripes:text size="10" maxlength="10" name="salesQuery.salesIds[0]" onkeyup="validate(this,'^[0-9]{1,10}$','emptyok')"/></td>
                    <td>OR</td>
                </tr>
                <tr>
                    <td>Sale Line Id:</td>
                    <td><stripes:text size="10" maxlength="10" name="salesQuery.saleLineId" onkeyup="validate(this,'^[0-9]{1,10}$','emptyok')"/></td>
                    <td>OR</td>
                </tr>
                <tr>
                    <td>Customer Id:</td>
                    <td><stripes:text  name="salesQuery.recipientCustomerId" size="30" onkeyup="validate(this,'^[0-9]{1,8}$','emptyok')"/></td>
                    <td>OR</td>
                </tr>
                <tr>
                    <td>Contract Id:</td>
                    <td><stripes:text  name="salesQuery.contractId" size="30" onkeyup="validate(this,'^[0-9]{1,8}$','emptyok')"/></td>
                    <td>OR</td>
                </tr>
                <tr>
                    <td>Serial Number (ICCID for SIM):</td>
                    <td><stripes:text name="salesQuery.serialNumber" size="30" onkeyup="validate(this,'^[0-9a-zA-Z]{1,30}$','emptyok')"/></td>
                    <td>OR</td>
                </tr>
                <tr>
                    <td>Status :</td>
                    <td>
                        <stripes:select name="salesQuery.status">
                            <stripes:option value=""></stripes:option>
                            <stripes:option value="PD">
                                <fmt:message key="sale.status.pd"/>
                            </stripes:option>
                            <stripes:option value="PP">
                                <fmt:message key="sale.status.pp"/>
                            </stripes:option>
                            <stripes:option value="QT">
                                <fmt:message key="sale.status.qt"/>
                            </stripes:option>
                            <stripes:option value="ST">
                                <fmt:message key="sale.status.st"/>
                            </stripes:option>
                            <stripes:option value="PV">
                                <fmt:message key="sale.status.pv"/>
                            </stripes:option>
                            <stripes:option value="LC">
                                <fmt:message key="sale.status.lc"/>
                            </stripes:option>
                            <stripes:option value="RV">
                                <fmt:message key="sale.status.rv"/>
                            </stripes:option>
                            <stripes:option value="PL">
                                <fmt:message key="sale.status.pl"/>
                            </stripes:option>
                            <stripes:option value="WT">
                                <fmt:message key="sale.status.wt"/>
                            </stripes:option>
                            <stripes:option value="SP">
                                <fmt:message key="sale.status.sp"/>
                            </stripes:option>
                        </stripes:select>
                    </td>
                </tr>
                <tr>
                    <td><stripes:label for="datefrom"/>:</td>
                    <td>
                        <input readonly="true" type="text" id="dateFrom" value="${s:formatDateShort(actionBean.salesQuery.dateFrom)}" name="salesQuery.dateFrom" class="required" size="10"/>
                        (From beginning of)
                    </td>
                </tr>
                <tr>
                    <td><stripes:label for="dateto"/>:</td>
                    <td>
                        <input id="dateTo" readonly="true" type="text" value="${s:formatDateShort(actionBean.salesQuery.dateTo)}" name="salesQuery.dateTo" class="required" size="10"/>
                        (Till end of)
                    </td>
                </tr>
                <tr>
                    <td>
                        <span class="button">
                            <stripes:submit name="searchSales"/>
                        </span>
                    </td>
                </tr>
            </table>            
        </stripes:form>

        <br/>

        <c:if test="${actionBean.salesList.numberOfSales > 0}">
            <table class="green">
                <tr>
                    <th>Sale Number</th>
                    <th>Sale Date</th>
                    <th>Status</th>
                    <th>Payment Method</th>
                    <th>Sale Total</th>
                    <th>Cash In Date</th>
                    <th>View</th>
                </tr>
                <c:forEach items="${actionBean.salesList.sales}" var="sale" varStatus="loop">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>${sale.saleId}</td>
                        <td>${s:formatDateLong(sale.saleDate)}</td>
                        <td><fmt:message key="sale.status.${sale.status}"/></td>
                        <td>${sale.paymentMethod}</td>
                        <td>${s:convertCentsToSpecifiedCurrencyLongRoundHalfEven(sale.tenderedCurrency, sale.saleTotalCentsIncl)}</td> 
                        <td>${s:formatDateLong(sale.cashInDate)}</td>
                        <td>
                            <stripes:form action="/Sales.action">
                                <input type="hidden" name="salesQuery.salesIds[0]" value="${sale.saleId}"/>
                                <stripes:submit name="showSale"/>
                            </stripes:form>
                        </td>
                    </tr>                    
                </c:forEach>                
            </table>                  
        </c:if>     

    </stripes:layout-component>


</stripes:layout-render>

