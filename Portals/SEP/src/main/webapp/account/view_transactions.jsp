<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="transaction.history"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="html_head">        

        <script src="${pageContext.request.contextPath}/js/highchart/highcharts.js"></script>
        <script src="${pageContext.request.contextPath}/js/highchart/highcharts-more.js"></script>
        <script src="${pageContext.request.contextPath}/js/highchart/modules/exporting.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js/highchart/themes/gray.js"></script>
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
    </stripes:layout-component>
    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="account"/>:
                        ${actionBean.accountHistoryQuery.accountId}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/Account.action">                                
                            <stripes:hidden name="accountQuery.accountId" value="${actionBean.accountHistoryQuery.accountId}"/>       
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveAccount"><fmt:message key="manage.account"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>

            <stripes:form action="/Account.action" focus="" name="frm">
                <stripes:hidden name="accountHistoryQuery.accountId" value="${actionBean.accountHistoryQuery.accountId}"/>
                <stripes:hidden name="accountQuery.accountId" value="${actionBean.accountHistoryQuery.accountId}"/>       
                <input type="hidden" name="pageSize" value="20"/>
                <table class="clear">
                    <tr>
                        <td><stripes:label for="datefrom"/>:</td>
                        <td>
                            <input readonly="true" type="text" id="dateFrom" value="${s:formatDateShort(actionBean.accountHistoryQuery.dateFrom)}" name="accountHistoryQuery.dateFrom" class="required" size="10"/>
                            (From beginning of)
                        </td>
                    </tr>
                    <tr>
                        <td><stripes:label for="dateto"/>:</td>
                        <td>
                            <input id="dateTo" readonly="true" type="text" value="${s:formatDateShort(actionBean.accountHistoryQuery.dateTo)}" name="accountHistoryQuery.dateTo" class="required" size="10"/>
                            (Till end of)
                        </td>
                    </tr>

                    <tr>
                        <td><stripes:label for="tx.id"/>:</td>
                        <td><stripes:text name="accountHistoryQuery.extTxId" size="40"/></td>
                    </tr>
                    <tr>
                        <td>Product Instance:</td>
                        <td>
                            <stripes:select name="productInstance.productInstanceId">
                                <stripes:option value="0">All</stripes:option>
                                <c:forEach items="${actionBean.productInstanceList.productInstances}" var="pi">
                                    <stripes:option value="${pi.productInstanceId}">
                                        ${pi.productInstanceId} :  ${pi.physicalId}  ${pi.friendlyName}
                                    </stripes:option>
                                </c:forEach>
                            </stripes:select>                            
                        </td>
                    </tr>	
                    <tr>
                        <td><stripes:label for="transaction.type"/>:</td>
                        <td>
                            <stripes:select name="accountHistoryQuery.transactionType">
                                <stripes:option value=""></stripes:option>
                                <c:forEach items="${s:getPropertyAsList('env.account.transactions.types')}" var="type">
                                    <stripes:option value="${type}">
                                        <fmt:message key="event.${type}"/>
                                    </stripes:option>
                                </c:forEach>
                            </stripes:select>                            
                        </td>
                    </tr>				
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="retrieveTransactionHistory"/>
                            </span>                        
                        </td>
                    </tr>
                    <c:if test="${s:getPropertyWithDefault('env.sep.tnf.enabled','false') == 'true'}">
                        <tr>
                            <td colspan="2"><strong>OR</strong><br/>Data Usage By Applications</td>
                        </tr>
                        <tr>
                            <td colspan="2">
                                <span class="button">
                                    <stripes:submit name="retrieveTNFData"/>
                                </span>                        
                            </td>
                        </tr>  
                    </c:if>
                </table>            
            </stripes:form>
            <br/>
            <fmt:message key="transaction.disclaimer"/>

            <br/>

            <c:if test="${s:getPropertyWithDefault('env.sep.tnf.enabled','false') == 'true'}">
                <c:if test="${!empty actionBean.breakdownDataset.breakdownDimension}">
                    <fmt:message key="transaction.disclaimer.breakdowndimension"/>
                </c:if>
                <br/>
                <c:if test="${!empty actionBean.trendOverTimeDataset.timeEntrySeries}">
                    <fmt:message key="transaction.disclaimer.timeentryseries"/>
                </c:if>
            </c:if>
            <br/>

            <c:if test="${!empty actionBean.accountSummary.periodSummaries}">
                <br/>
                <br/>
                <c:if test="${s:getProperty('env.sep.session.barchart.enabled') == 'true'}">
                    <div class="tabs" style="width: 100%;">
                        <ul class="tab-links">
                            <li class="active"><a href="#alltab" style="color: #99cc33;">All Usage</a></li>
                            <li><a href="#voicetab" style="color: #99cc33;">Voice Only View</a></li>
                            <li><a href="#smstab" style="color: #99cc33;">SMS Only View</a></li>
                            <li><a href="#datatab" style="color: #99cc33;">Data Only View</a></li>
                        </ul>
                        <div class="tab-content">
                            <div id="alltab" class="tab active">
                                <div id="container" style="width: 95%; height: 400px; margin: 0 auto;"></div>
                                <table border="0" width="100%">
                                    <tr>
                                        <td>
                                            <table width="100%" style="border: none; text-align:center; background-color: #EBF2D4;">
                                                <tr style="font-size: 15px; font-weight: bold;">
                                                    <td colspan="2">
                                                        Total Used for Period
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>
                                                        <br class="clear" />
                                                    </td>
                                                </tr>
                                                <tr style="border: none;">
                                                    <td width="30%" style="border: none; text-align:left">
                                                        <img src="images/voice_call.png" alt="Voice" title="Voice" height="40"/><br/>
                                                        ${s:displayVolumeAsStringWithCommaGroupingSeparator(s:getListTotalAsDoubleWithFilterForFilterType(actionBean.accountSummary.periodSummaries, 'getTransactionType', 'Voice', 'getTotalUnits'),'sec')}
                                                    </td>
                                                    <td width="70%" style="border: none; text-align:left">
                                                        <img src="images/sms.png" alt="SMS" title="SMS" height="40"/><br/>
                                                        ${s:displayVolumeAsStringWithCommaGroupingSeparator(s:getListTotalAsDoubleWithFilterForFilterType(actionBean.accountSummary.periodSummaries, 'getTransactionType', 'SMS', 'getTotalUnits'),'sms')} 
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                </table>
                            </div>
                            <div id="voicetab" class="tab">
                                <div id="voicetab_container" style="margin: 0 auto;"></div>
                            </div>
                            <div id="smstab" class="tab">
                                <div id="smstab_container" style="margin: 0 auto;"></div>
                            </div>
                            <div id="datatab" class="tab">
                                <div id="datatab_container" style="margin: 0 auto;"></div>
                            </div>
                        </div>
                    </div>


                </c:if>
                <br/>
                <br/>
            </c:if>
            <c:if test="${s:getPropertyWithDefault('env.sep.tnf.enabled','false') == 'true'}">
                <c:if test="${!empty actionBean.breakdownDataset.breakdownDimension}">
                    <div id="breakdownDimensionContainer" style="width: 100%; height: 400px; margin: 0 auto"></div>
                </c:if>
                <br/>
                <c:if test="${!empty actionBean.trendOverTimeDataset.timeEntrySeries}">
                    <div id="timeEntrySeriesContainer" style="width: 100%; height: 400px; margin: 0 auto"></div>
                </c:if>
            </c:if>
            <br/>

            <c:if test="${!empty actionBean.accountHistory.transactionRecords}">

                <stripes:form action="/Account.action">
                    <input type="hidden" name="pageStart" value="${actionBean.pageStart}"/>
                    <input type="hidden" name="pageMax" value="${s:getListSize(actionBean.accountHistory.transactionRecords)}"/>
                    <input type="hidden" name="action" value="retrieveTransactionHistory"/>

                    <stripes:wizard-fields/>
                    <table>
                        <tr>
                            <td><stripes:submit name="pageFirst"/></td>
                            <td><stripes:submit name="pageBack"/></td>
                            <td><stripes:submit name="pageNext"/></td>
                            <td><stripes:submit name="pageLast"/></td>
                        </tr>
                    </table>
                </stripes:form>
                <table class="green" width="99%">
                    <tr>
                        <th><fmt:message key="transaction.sourcedest"/></th>
                        <th><fmt:message key="transaction.value"/></th>
                        <th><fmt:message key="transaction.totalunits"/></th>
                        <th><fmt:message key="transaction.ucunits"/></th>
                        <th><fmt:message key="transaction.balance"/></th>                       
                        <th><fmt:message key="transaction.type"/></th>
                        <th><fmt:message key="transaction.description"/></th>
                        <th><fmt:message key="transaction.datetime"/></th>
                        <th><fmt:message key="transaction.device"/></th>
                        <th>ICCID</th>
                        <th>Reverse</th>
                    </tr>

                    <c:forEach items="${s:orderList(actionBean.accountHistory.transactionRecords,'getStartDate','desc')}" begin="${actionBean.pageStart}" end="${actionBean.pageStart + actionBean.pageSize -1}" var="transactionRecord" varStatus="loop">
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            <c:if test="${fn:startsWith(transactionRecord.description,'Data')}">
                                <td></td>
                                <td align='center'>${s:convertCentsToCurrencyLong(transactionRecord.amountInCents)}</td>
                                <td align='center'>
                                    <stripes:link  href="/Account.action" event="retrieveSessionDetail">
                                        <stripes:param name="transactionRecordId" value="${transactionRecord.transactionRecordId}"/>
                                        ${s:displayVolumeAsString(transactionRecord.totalUnits, 'Byte')}
                                    </stripes:link>
                                </td>
                                <td align='center'>${s:displayVolumeAsString(transactionRecord.unitCreditUnits, 'Byte')}</td>
                            </c:if>
                            <c:if test="${fn:startsWith(transactionRecord.description,'SMS')}">
                                <td>${s:getPhoneNumberFromSipURI(transactionRecord.source)} - ${s:getPhoneNumberFromSipURI(transactionRecord.destination)}</td>
                                <td align='center'>${s:convertCentsToCurrencyLong(transactionRecord.amountInCents)}</td>
                                <td align='center'>
                                    <stripes:link  href="/Account.action" event="retrieveSessionDetail">
                                        <stripes:param name="transactionRecordId" value="${transactionRecord.transactionRecordId}"/>
                                        ${transactionRecord.totalUnits}
                                    </stripes:link>
                                </td>
                                <td align='center'>${s:displayVolumeAsString(transactionRecord.unitCreditUnits*-1, 'Byte')}
                                    <c:if test="${transactionRecord.unitCreditBaselineUnits != 0}">
                                        (${s:displayVolumeAsString(transactionRecord.unitCreditBaselineUnits*-1, 'Byte')})
                                    </c:if>
                                </td>
                            </c:if>
                            <c:if test="${fn:startsWith(transactionRecord.description,'Split')}">
                                <td>${transactionRecord.source} - ${transactionRecord.destination}</td>
                                <td></td>
                                <td align='center'>
                                    ${s:displayVolumeAsString(transactionRecord.totalUnits, 'Byte')}
                                </td>
                                <td align='center'>${s:displayVolumeAsString(transactionRecord.unitCreditUnits, 'Byte')}</td>
                            </c:if>
                            <c:if test="${fn:startsWith(transactionRecord.description,'Voice')}">
                                <td>${s:getPhoneNumberFromSipURI(transactionRecord.source)} - ${s:getPhoneNumberFromSipURI(transactionRecord.destination)}</td>
                                <td align='center'>${s:convertCentsToCurrencyLong(transactionRecord.amountInCents)}</td>
                                <td align='center'>
                                    <stripes:link  href="/Account.action" event="retrieveSessionDetail">
                                        <stripes:param name="transactionRecordId" value="${transactionRecord.transactionRecordId}"/>
                                        ${s:displayVolumeAsString(transactionRecord.totalUnits, 'Sec')}
                                    </stripes:link>
                                </td>
                                <td align='center'>${s:displayVolumeAsString(transactionRecord.unitCreditUnits*-1, 'Sec')}
                                    <c:if test="${transactionRecord.unitCreditBaselineUnits != 0}">
                                        (${s:displayVolumeAsString(transactionRecord.unitCreditBaselineUnits*-1, 'Byte')})
                                    </c:if>
                                </td>
                            </c:if>
                            <c:if test="${!fn:startsWith(transactionRecord.description,'Voice') && !fn:startsWith(transactionRecord.description, 'Data')  && !fn:startsWith(transactionRecord.description, 'SMS') && !fn:startsWith(transactionRecord.description,'Split')}">
                                <td>Other Acc: ${transactionRecord.destination}</td>
                                <td align='center'>${s:convertCentsToCurrencyLong(transactionRecord.amountInCents)}</td>
                                <td><c:if test="${transactionRecord.totalUnits != 0}">${transactionRecord.totalUnits}</c:if></td>
                                    <td align='center'>
                                    <c:if test="${transactionRecord.unitCreditUnits != 0}">${transactionRecord.unitCreditUnits*-1}</c:if>
                                    <c:if test="${transactionRecord.unitCreditBaselineUnits != 0}">
                                        (${transactionRecord.unitCreditBaselineUnits*-1})
                                    </c:if>
                                </td>
                            </c:if>

                            <td align='center'>${s:convertCentsToCurrencyShort(transactionRecord.accountBalanceRemainingInCents)}</td>
                            <td><fmt:message key="event.${transactionRecord.transactionType}"/></td>
                            <td>${transactionRecord.description}</td>
                            <td>${s:formatDateLong(transactionRecord.startDate)} - ${s:formatDateLong(transactionRecord.endDate)}</td>
                            <td>${s:breakUp(fn:replace(transactionRecord.sourceDevice,"IMEISV=",""),8)}</td>
                            <td>${s:breakUp(s:getServiceInstanceICCID(transactionRecord.serviceInstanceId),10)}</td>
                            <td>
                                <c:if test="${fn:startsWith(transactionRecord.transactionType, 'txtype.tfr.')}">
                                    <stripes:form action="/Account.action" class="buttonOnly">
                                        <stripes:hidden name="account.accountId" value="${transactionRecord.accountId}"/>
                                        <stripes:hidden name="transactionReversalData.transferExtTxIds[0]" value="${transactionRecord.extTxId}"/>
                                        <stripes:submit name="reverseBalanceTransfer"/>                            
                                    </stripes:form>
                                </c:if>
                            </td>
                        </tr>
                        <c:set var="loopCount">${loop.count}</c:set>
                    </c:forEach>
                </table>  
                <fmt:message key="results.xtoyofz">
                    <fmt:param>${actionBean.pageStart+1}</fmt:param>
                    <fmt:param>${actionBean.pageStart+loopCount}</fmt:param>
                    <fmt:param>${actionBean.accountHistory.resultsReturned}</fmt:param>
                </fmt:message>


                <br/>
                <stripes:form action="/Account.action">    
                    <stripes:hidden name="accountHistoryQuery.accountId" value="${actionBean.account.accountId}"/>                
                    <stripes:hidden name="accountHistoryQuery.dateFrom" value="${actionBean.accountHistoryQuery.dateFrom}"/>
                    <stripes:hidden name="accountHistoryQuery.dateTo" value="${actionBean.accountHistoryQuery.dateTo}"/>
                    <stripes:hidden name="accountHistoryQuery.extTxId" value="${actionBean.accountHistoryQuery.extTxId}"/>
                    <stripes:hidden name="accountHistoryQuery.resultLimit" value="${actionBean.accountHistoryQuery.resultLimit}"/>
                    <c:forEach items="${actionBean.accountHistoryQuery.serviceInstanceIds}" var="id" varStatus="loop">
                        <input type="hidden" name="accountHistoryQuery.serviceInstanceIds[${loop.index}]" value="${id}"/>
                    </c:forEach>
                    <stripes:submit name="retrieveTransactionHistoryPrintable"/>
                </stripes:form>

            </c:if>
            <br/>   
            <script type="text/javascript">
                var $j = jQuery.noConflict();
                var dateNow = new Date();
                var totalUsagePerPeriod = 0.0;

                Highcharts.setOptions({
                    global: {
                        useUTC: true
                    }
                });
                
                $j(document).ready(function () {
                    $j("#dateFrom").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", maxDate: dateNow, changeYear: true, changeMonth: true});
                });
                $j(document).ready(function () {
                    $j("#dateTo").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", maxDate: dateNow, changeYear: true, changeMonth: true});
                });

                $j(document).ready(function () {
                    $j('.tabs .tab-links a').on('click', function (e) {
                        var currentAttrValue = $j(this).attr('href');

                        // Show/Hide Tabs
                        //$j('.tabs ' + currentAttrValue).show().siblings().hide();
                        $j('.tabs ' + currentAttrValue).fadeIn(400).siblings().hide();
                        // Change/remove current tab to active
                        $j(this).parent('li').addClass('active').siblings().removeClass('active');

                        e.preventDefault();
                    });
                });

                var graphTitle = "Data Usage for Account " + "${actionBean.accountSummaryQuery.accountId}" + " From " + "${s:formatDateShort(actionBean.accountSummaryQuery.dateFrom)}" + " To " + "${s:formatDateShort(actionBean.accountSummaryQuery.dateTo)}";

                var rangeStartDateMs = Number('${s:convertXMLGregorianCalendarToTimestamp(actionBean.accountSummaryQuery.dateFrom)}');
                var rangeEndDateMs = Number('${s:convertXMLGregorianCalendarToTimestamp(actionBean.accountSummaryQuery.dateTo)}');
                var fullRangeStartDate = timeConverter(rangeStartDateMs);
                var fullRangeEndDate = timeConverter(rangeEndDateMs);
                var rangeStartDate = Date.UTC(fullRangeStartDate.getFullYear(), fullRangeStartDate.getMonth(), fullRangeStartDate.getDate());
                var rangeEndDate = Date.UTC(fullRangeEndDate.getFullYear(), fullRangeEndDate.getMonth(), fullRangeEndDate.getDate());

                var diff = fullRangeEndDate - fullRangeStartDate;
                //var diffDays = Math.ceil(diff / (1000 * 3600 * 24));
                var diffDays = 0;
                var diffSeconds = 0;
                
                <c:if test="${!empty actionBean.accountSummary.periodSummaries}">
                diffDays = ${s:getDaysBetweenDates(s:getJavaDate(actionBean.accountSummaryQuery.dateFrom), s:getJavaDate(actionBean.accountSummaryQuery.dateTo))};
                </c:if>
                
                var sessionDetailBarGraph = JSON.parse('${actionBean.sessionDetailBarGraph}');
                var seriesHCArray = [];

                var today = new Date();

                function getTickInterval() {
                    var tickIntervalValue = 24 * 3600 * 1000;
                    if (diffDays <= 1) {
                        tickIntervalValue = 2 * 3600 * 1000;//no of hours * 3600 * 1000 
                        if (diffDays === 1) {
                            tickIntervalValue = 4 * 3600 * 1000;
                        }
                    } else if (diffDays <= 31) {
                        tickIntervalValue = 1 * 24 * 3600 * 1000;//no of days * 24 * 3600 * 1000 
                    } else if (diffDays > 31 && diffDays <= 365) {
                        tickIntervalValue = 1 * 30 * 24 * 3600 * 1000;//no of months * days * 24 * 3600 * 1000 
                    } else if (diffDays > 365) {
                        tickIntervalValue = 1 * 365 * 24 * 3600 * 1000;//no of years * 365 * 24 * 3600 * 1000 
                    }
                    return tickIntervalValue;
                }


                function getDateTimeLabelFormat() {
                    var dateFormarted = {day: '%e'};
                    if (diffDays <= 1) {
                        dateFormarted = {hour: '%H:%M'};
                    } else if (diffDays <= 31) {
                        dateFormarted = {day: '%e'};
                    } else if (diffDays > 31 && diffDays <= 365) {
                        dateFormarted = {month: '%b'};
                    } else if (diffDays > 365) {
                        dateFormarted = {year: '%Y'};
                    }
                    return dateFormarted;
                }


                function getXaxisMininum() {
                    if (diffDays <= 1) {
                        return Date.UTC(fullRangeStartDate.getFullYear(), fullRangeStartDate.getMonth(), fullRangeStartDate.getDate(), fullRangeStartDate.getHours(), fullRangeStartDate.getMinutes());
                    } else {
                        return rangeStartDate;
                    }
                }

                function getXaxisMaximum() {
                    if (diffDays <= 1) {
                        return Date.UTC(fullRangeEndDate.getFullYear(), fullRangeEndDate.getMonth(), fullRangeEndDate.getDate(), fullRangeEndDate.getHours(), fullRangeEndDate.getMinutes());
                    } else {
                        return rangeEndDate;
                    }
                }

                function timeConverter(UNIX_timestamp) {
                    var a = new Date(UNIX_timestamp);
                    return a;
                }

                function graphPlotPointsColumnFactory(typeParam) {
                    var periodDataSummary = [];

                    for (var j = 0; j < sessionDetailBarGraph.resultsReturned; j++) {
                        var periodSummary = sessionDetailBarGraph.periodSummaries[j];
                        var transactionType = periodSummary.transactionType;
                        var eventUnitsValue = 0.0;

                        if (transactionType === typeParam) {

                            eventUnitsValue = periodSummary.totalUnits;
                            if (transactionType !== 'Data') {
                                eventUnitsValue = periodSummary.unitCreditBaselineUnits;
                            }

                            var unitValue = parseFloat(eventUnitsValue);
                            var totalUnitsAsYAxis = 0.0;

                            console.log("Period: ", periodSummary.period);
                            var periodYear = "20" + periodSummary.period.substring(0, 2);
                            var periodMonth = periodSummary.period.substring(2, 4);
                            var dataDate = new Date(Number(periodYear), Number(periodMonth), 0);
                            var periodDay = dataDate.getDate();
                            console.log("Data date: ", dataDate);
                            if(periodSummary.period.length >= 5){
                                periodDay = periodSummary.period.substring(4, 6);
                            }
                            
                            var periodHour = dataDate.getHours();
                            
                            if (periodSummary.period.length >= 7) {
                                periodHour = periodSummary.period.substring(6, 8);
                            }
                            
                            var newDate = Date.UTC(periodYear, periodMonth - 1, periodDay, periodHour);

                            if (unitValue > 0 && unitValue < 1) {
                                totalUnitsAsYAxis = Math.round(parseFloat(unitValue) / ${s:getPropertyWithDefault('env.portals.data.volume.display.denominator', '1000000')} * 10) / 10;
                            } else {
                                totalUnitsAsYAxis = Math.round(parseFloat(unitValue) / ${s:getPropertyWithDefault('env.portals.data.volume.display.denominator', '1000000')});//convert to MB
                            }
                            console.log("Date : " + newDate + " Value : " + totalUnitsAsYAxis);
                            periodDataSummary.push([newDate, totalUnitsAsYAxis]);
                            console.log(periodDataSummary);
                            fullDate = 0.0;
                            newDate = null;

                        }
                    }

                    var seriesObj = {
                        name: typeParam,
                        data: periodDataSummary,
                        type: 'column',
                        showInLegend: true
                    };
                    console.log(seriesObj);
                    seriesHCArray.push(seriesObj);
                    console.log(seriesHCArray);

                }


                function graphPlotPointsTxTypeSpecificFactory(typeParam) {
                    var periodDataSummary = [];

                    for (var j = 0; j < sessionDetailBarGraph.resultsReturned; j++) {
                        var periodSummary = sessionDetailBarGraph.periodSummaries[j];
                        var transactionType = periodSummary.transactionType;
                        var eventUnitsValue = 0.0;
                        if (transactionType !== typeParam) {
                            continue;
                        }

                        if (transactionType === typeParam) {

                            eventUnitsValue = periodSummary.totalUnits;

                            var unitValue = parseFloat(eventUnitsValue);
                            var totalUnitsAsYAxis = 0.0;

                            console.log("Period: ", periodSummary.period);
                            var periodYear = "20" + periodSummary.period.substring(0, 2);
                            var periodMonth = periodSummary.period.substring(2, 4);
                            var periodDay = "01";
                            
                            if(periodSummary.period.length >= 5){
                                periodDay = periodSummary.period.substring(4, 6);
                            }
                            
                            var periodHour = "0";
                            
                            if (periodSummary.period.length >= 7) {
                                periodHour = periodSummary.period.substring(6, 8);
                            }
                            
                            var newDate = Date.UTC(periodYear, periodMonth - 1, periodDay, periodHour);
                            
                            if (typeParam === 'SMS') {
                                totalUnitsAsYAxis = Math.round(parseFloat(unitValue));
                            } else if (typeParam === 'Voice') {
                                totalUnitsAsYAxis = Math.ceil((Math.round(parseFloat(unitValue)) / 60));//convert to Min
                            } else if (typeParam === 'Data') {
                                if (unitValue > 0 && unitValue < 1) {
                                    totalUnitsAsYAxis = Math.round(parseFloat(unitValue) / ${s:getPropertyWithDefault('env.portals.data.volume.display.denominator', '1000000')} * 10) / 10;
                                } else {
                                    totalUnitsAsYAxis = Math.round(parseFloat(unitValue) / ${s:getPropertyWithDefault('env.portals.data.volume.display.denominator', '1000000')});//convert to MB
                                }
                            }
                            console.log("Date1 : " + newDate + " Value1 : " + totalUnitsAsYAxis);
                            periodDataSummary.push([newDate, totalUnitsAsYAxis]);

                            fullDate = 0.0;
                            newDate = null;
                        }
                    }
                    return periodDataSummary;

                }

                function initData() {
                    graphPlotPointsColumnFactory('Data');
                    graphPlotPointsColumnFactory('SMS');
                    graphPlotPointsColumnFactory('Voice');
                }


                <c:if test="${!empty actionBean.accountSummary.periodSummaries}">
                $j(document).ready(function () {

                    initData();

                    var hObj = {
                        chart: {
                            //renderTo: 'container'
                        },
                        title: {
                            text: graphTitle
                        },
                        credits: {
                            enabled: false
                        },
                        xAxis: {    
                            type: 'datetime',
                            min: getXaxisMininum(),
                            max: getXaxisMaximum(),
                            tickInterval: getTickInterval(),
                            dateTimeLabelFormats: getDateTimeLabelFormat(),
                            title: {
                                text: 'Time'
                            }
                        },
                        yAxis: {
                            min: 0,
                            title: {
                                text: 'Data (MB)'
                            }
                        },
                        tooltip: {
                            xDateFormat: '%b %e',
                            headerFormat: '<span style="font-size:10px">{point.key}</span><table>',
                            pointFormat: '<tr><td style="color:{series.color};padding:0">{point.y} ${s:getPropertyWithDefault('env.portals.data.volume.display.suffix', 'MB')} </td>',
                            footerFormat: '</table>',
                            //shared: false,
                            useHTML: true
                        },
                        plotOptions: {
                            column: {
                                //pointPadding: 10,
                                //groupPadding: 20,   // Exactly overlap
                                //pointWidth: 20
                                //stacking: 'normal'
                            },
                            series: {
                                events: {
                                    legendItemClick: function () {
                                        return false;
                                    }
                                },
                                pointPadding: 0,
                                groupPadding: 0.01
                            }
                        },
                        legend: {
                            layout: 'vertical',
                            backgroundColor: '#FFFFFF',
                            floating: true,
                            align: 'center',
                            verticalAlign: 'top',
                            x: 90,
                            y: 25,
                            itemHoverStyle: {
                                color: '#228B22'
                            },
                            labelFormatter: function () {
                                var total = 0.0;
                                for (var i = this.yData.length; i--; ) {
                                    total += this.yData[i];
                                }
                                var stringToReturn = "";
                                stringToReturn = this.name + " " + total + "${s:getPropertyWithDefault('env.portals.data.volume.display.suffix', 'MB')}";
                                return stringToReturn;
                            },
                            title: {
                                text: 'Total Data',
                                style: {
                                    fontStyle: 'italic'
                                }
                            }
                        },
                        series: seriesHCArray

                    };

                    $j('#container').highcharts(hObj);


                    var voiceContiner = {
                        chart: {
                        },
                        exporting: {
                            enabled: false
                        },
                        title: {
                            text: 'Voice Call Duration'
                        },
                        credits: {
                            enabled: false
                        },
                        tooltip: {
                            xDateFormat: '%b %e',
                            headerFormat: '<span style="font-size:10px">{point.key}</span><table>',
                            pointFormat: '<tr><td style="color:{series.color};padding:0">{point.y} Minutes </td>',
                            footerFormat: '</table>',
                            useHTML: true
                        },
                        xAxis: {
                            type: 'datetime',
                            min: getXaxisMininum(),
                            max: getXaxisMaximum(),
                            tickInterval: getTickInterval(),
                            dateTimeLabelFormats: getDateTimeLabelFormat(),
                            title: {
                                text: 'Time'
                            }
                        },
                        yAxis: {
                            min: 0,
                            title: {
                                text: 'Duration (Min)'
                            }
                        },
                        series: [{
                                name: 'Voice',
                                data: graphPlotPointsTxTypeSpecificFactory('Voice'),
                                type: 'column',
                                color: '#99cc33',
                                showInLegend: false
                            }]
                    };



                    $j('#container2').highcharts(voiceContiner);
                    $j('#voicetab_container').highcharts(voiceContiner);


                    var smsContainerObj = {
                        chart: {
                        },
                        exporting: {
                            enabled: false
                        },
                        title: {
                            text: 'SMS Sent'
                        },
                        credits: {
                            enabled: false
                        },
                        tooltip: {
                            xDateFormat: '%b %e',
                            headerFormat: '<span style="font-size:10px">{point.key}</span><table>',
                            pointFormat: '<tr><td style="color:{series.color};padding:0">{point.y} SMS </td>',
                            footerFormat: '</table>',
                            //shared: false,
                            useHTML: true
                        },
                        plotOptions: {
                            scatter: {
                                marker: {
                                    symbol: 'circle',
                                    lineColor: '#99cc33',
                                    lineWidth: 1
                                },
                                dataLabels: {
                                    enabled: true,
                                    verticalAlign: 'middle'
                                }
                            }
                        },
                        xAxis: {
                            type: 'datetime',
                            min: getXaxisMininum(),
                            max: getXaxisMaximum(),
                            tickInterval: getTickInterval(),
                            dateTimeLabelFormats: getDateTimeLabelFormat(),
                            title: {
                                text: 'Time'
                            }
                        },
                        yAxis: {
                            min: 0,
                            title: {
                                text: 'No. of SMS'
                            }
                        },
                        series: [{
                                name: 'SMS',
                                data: graphPlotPointsTxTypeSpecificFactory('SMS'),
                                type: 'scatter',
                                //color: '#99cc33',
                                showInLegend: false,
                                dataLabels: {
                                    style: {
                                        fontSize: '15px'
                                    }
                                },
                                marker: {radius: 18}
                            }]
                    };

                    $j('#container3').highcharts(smsContainerObj);
                    $j('#smstab_container').highcharts(smsContainerObj);


                    var dataContainerObj = {
                        chart: {
                        },
                        title: {
                            text: 'Internet Data Usage'
                        },
                        credits: {
                            enabled: false
                        },
                        xAxis: {
                            type: 'datetime',
                            min: getXaxisMininum(),
                            max: getXaxisMaximum(),
                            tickInterval: getTickInterval(),
                            dateTimeLabelFormats: getDateTimeLabelFormat(),
                            title: {
                                text: 'Time'
                            }
                        },
                        yAxis: {
                            min: 0,
                            title: {
                                text: 'Data (MB)'
                            }
                        },
                        tooltip: {
                            xDateFormat: '%b %e',
                            headerFormat: '<span style="font-size:10px">{point.key}</span><table>',
                            pointFormat: '<tr><td style="color:{series.color};padding:0">{point.y} ${s:getPropertyWithDefault('env.portals.data.volume.display.suffix', 'MB')} </td>',
                            footerFormat: '</table>',
                            //shared: false,
                            useHTML: true
                        },
                        plotOptions: {
                        },
                        series: [{
                                name: 'Data',
                                data: graphPlotPointsTxTypeSpecificFactory('Data'),
                                type: 'column',
                                color: '#99cc33',
                                showInLegend: false
                            }]

                    };

                    $j('#datatab_container').highcharts(dataContainerObj);

                });
                </c:if>
            </script>

            <script>
                <c:if test="${s:getPropertyWithDefault('env.sep.tnf.enabled','false') == 'true'}">
                var applicationName = "";
                var seriesDataObjTNF = [];
                var reportUnits = "unknown";
                var seriesPointIntervalTNF = 24 * 3600 * 1000;

                var rangeEndDateMsTNF = Number('${s:convertXMLGregorianCalendarToTimestamp(actionBean.timeRange.endTime)}');
                var fullRangeEndDateTNF = timeConverter(rangeEndDateMsTNF);
                var rangeEndDateTNF = Date.UTC(fullRangeEndDateTNF.getFullYear(), fullRangeEndDateTNF.getMonth(), fullRangeEndDateTNF.getDate());

                var rangeStartDateMsTNF = Number('${s:convertXMLGregorianCalendarToTimestamp(actionBean.timeRange.startTime)}');
                var fullRangeStartDateTNF = timeConverter(rangeStartDateMsTNF);
                var rangeStartDateTNF = Date.UTC(fullRangeStartDateTNF.getFullYear(), fullRangeStartDateTNF.getMonth(), fullRangeStartDateTNF.getDate());

                var today = new Date();
                var browserOffset = -(today.getTimezoneOffset() / 60);
                var serveroffset = ${s:getTimeZoneOffsetHours()};
                var jhboffset = 2 - browserOffset;
                    <%--SERVER OFFSET IS BASED ON JHB TIME --%>
                var seriesHCArrayTNF = [];

                    <c:set var="granularityParts" value="${fn:split(actionBean.timeRange.granularity, '_')}" />;
                    <c:set var="granularityUnit" value="${granularityParts[1]}" />;


                function getTNFGraphTitle() {
                    var granularity = '${granularityUnit}';

                    if (granularity === 'MINUTE' || granularity === 'HOUR') {
                        return 'Search range from ' + '${s:formatDateLong(actionBean.timeRange.startTime)}' + ' to ' + '${s:formatDateLong(actionBean.timeRange.endTime)}';
                    } else {
                        return 'Search range from ' + '${s:formatDateShort(actionBean.timeRange.startTime)}' + ' to ' + '${s:formatDateShort(actionBean.timeRange.endTime)}';
                    }
                }



                var trendOverTimeData = [];

                    <c:forEach items="${actionBean.trendOverTimeDataset.timeEntrySeries}" var="timeEntrySeries" varStatus="loopTNF">

                var timeEntrySeriesData = [];
                reportUnits = "${timeEntrySeries.unitLabel}";
                var timeEntryObj = "${timeEntrySeries.timeEntry}";
                var granularity = '${granularityUnit}';

                        <c:forEach items="${timeEntrySeries.timeEntry}" var="timeEntry" varStatus="loopTimeEntry">
                var timestampValue = ${s:convertXMLGregorianCalendarToTimestamp(timeEntry.startTimestamp)};
                var fullDate = timeConverter(timestampValue + jhboffset * 3600 * 1000); // Put the date into the timezone of the server
                var year = fullDate.getFullYear();
                var month = fullDate.getMonth();
                var date = fullDate.getDate();
                var hours = fullDate.getHours();
                var minutes = fullDate.getMinutes();
                var newDate = Date.UTC(year, month, date); // The new date is the day in the local timezone of the server

                if (granularity === 'MINUTE') {
                    newDate = Date.UTC(year, month, date, hours, minutes);
                } else if (granularity === 'HOUR') {
                    newDate = Date.UTC(year, month, date, hours);
                } else if (granularity === 'DAY') {
                    newDate = Date.UTC(year, month, date);
                } else if (granularity === 'MONTH') {
                    newDate = Date.UTC(year, month);
                } else if (granularity === 'YEAR') {
                    newDate = Date.UTC(year, month);
                }

                var timeEntryValue = 0.0;

                var entryValue = ${timeEntry.timeEntryValue};
                var unitValue = parseFloat(entryValue);

                if (unitValue > 0 && unitValue < 1) {
                    timeEntryValue = Math.round(parseFloat(unitValue) / ${s:getPropertyWithDefault('env.portals.data.volume.display.denominator', '1000000')} * 10) / 10;
                } else {
                    timeEntryValue = Math.round(parseFloat(unitValue) / ${s:getPropertyWithDefault('env.portals.data.volume.display.denominator', '1000000')});//convert to MB
                }
                timeEntrySeriesData.push([newDate, timeEntryValue]);

                        </c:forEach>

                trendOverTimeData.push(timeEntrySeriesData);
                var totSeries = {
                    name: '${timeEntrySeries.dimensionName}',
                    data: timeEntrySeriesData,
                    type: 'column',
                    pointWidth: 20,
                    //showInLegend: false
                };
                seriesHCArrayTNF.push(totSeries);
                    </c:forEach>



                function getBreakdownDatasetBreakdownDimensionDataAsPie() {
                    var pieData = [];
                    var totalDataUsagePie = 0.0;
                    <c:forEach items="${actionBean.breakdownDataset.breakdownDimension}" var="dimension" varStatus="loop">
                    var value = 0.0;
                        <c:forEach items="${dimension.breakdownKpi}" var="breakdownKpi" varStatus="loopDimension">
                    value = ${breakdownKpi.breakdownValue};
                    totalDataUsagePie += value;
                        </c:forEach>
                    value = parseFloat(value);

                    if (value > 0 && value < 1) {
                        value = Math.round(parseFloat(value) / ${s:getPropertyWithDefault('env.portals.data.volume.display.denominator', '1000000')} * 10) / 10;
                    } else {
                        value = Math.round(parseFloat(value) / ${s:getPropertyWithDefault('env.portals.data.volume.display.denominator', '1000000')});//convert to MB
                    }
                    pieData.push(['${dimension.dimensionName}', value]);
                    </c:forEach>

                    return pieData;
                }


                var breakdownDimensionDataAsPie = {
                    data: getBreakdownDatasetBreakdownDimensionDataAsPie()
                };


                $j(function () {
                    $j('#breakdownDimensionContainer').highcharts({
                        chart: {
                            plotBackgroundColor: null,
                            plotBorderWidth: null,
                            plotShadow: false,
                            type: 'pie'
                        },
                        title: {
                            text: getTNFGraphTitle()
                        },
                        tooltip: {
                            pointFormat: '{point.name}: <b>{point.y} ${s:getPropertyWithDefault('env.portals.data.volume.display.suffix', 'MB')}</b>'//'{series.name}: <b>{point.percentage:.1f}%</b>'
                        },
                        credits: {
                            enabled: false
                        },
                        plotOptions: {
                            pie: {
                                allowPointSelect: true,
                                cursor: 'pointer',
                                dataLabels: {
                                    enabled: true,
                                    format: '{point.name}: <b>{point.y} ${s:getPropertyWithDefault('env.portals.data.volume.display.suffix', 'MB')}</b>', //'<b>{point.name}</b>: {point.percentage:.1f} %',
                                    style: {
                                        color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black'
                                    }
                                }
                            }
                        },
                        series: [breakdownDimensionDataAsPie]
                    });
                });

                $j(function () {
                    $j('#timeEntrySeriesContainer').highcharts({
                        chart: {
                            plotBackgroundColor: null,
                            plotBorderWidth: null,
                            plotShadow: false
                        },
                        title: {
                            text: ''
                        },
                        credits: {
                            enabled: false
                        },
                        xAxis: {
                            type: 'datetime',
                            min: rangeStartDateTNF,
                            max: rangeEndDateTNF,
                            title: {
                                text: 'Time'
                            }
                        },
                        yAxis: {
                            min: 0,
                            title: {
                                text: 'Data (MB)'
                            }
                        },
                        tooltip: {
                            xDateFormat: '%b %e',
                            headerFormat: '<span style="font-size:10px">{point.key}</span><table>',
                            pointFormat: '<tr><td style="color:{series.color};padding:0">{point.y} ${s:getPropertyWithDefault('env.portals.data.volume.display.suffix', 'MB')} </td>',
                            footerFormat: '</table>',
                            useHTML: true
                        },
                        plotOptions: {
                            series: {
                                dataLabels: {
                                    enabled: true,
                                    align: 'right',
                                    color: '#FFFFFF',
                                    y: -5
                                },
                                pointPadding: 0.1,
                                groupPadding: 0,
                                events: {
                                    legendItemClick: function () {
                                        return false;
                                    }
                                }
                            }
                        },
                        legend: {
                            layout: 'vertical',
                            //backgroundColor: '#FFFFFF',
                            //floating: true,
                            align: 'center',
                            verticalAlign: 'top',
                            //x: 90,
                            //y: 25,
                            itemHoverStyle: {
                                color: '#228B22'
                            },
                            labelFormatter: function () {
                                var total = 0.0;
                                for (var i = this.yData.length; i--; ) {
                                    total += this.yData[i];
                                }
                                var stringToReturn = "";
                                stringToReturn = " " + Number(total).toLocaleString() + "${s:getPropertyWithDefault('env.portals.data.volume.display.suffix', 'MB')}";
                                return stringToReturn;
                            },
                            title: {
                                text: 'Total',
                                style: {
                                    fontStyle: 'italic'
                                }
                            }
                        },
                        series: seriesHCArrayTNF
                    });
                });
                </c:if>

            </script>
        </div>
    </stripes:layout-component>
</stripes:layout-render>