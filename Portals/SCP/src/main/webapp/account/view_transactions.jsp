<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="transaction.history"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">
        <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-1.7.2.min.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-ui-1.8.21.custom.min.js"></script>
        <script src="${pageContext.request.contextPath}/js/highchart/highcharts.js"></script>
        <script src="${pageContext.request.contextPath}/js/highchart/highcharts-more.js"></script>
        <script src="${pageContext.request.contextPath}/js/highchart/modules/exporting.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js/highchart/themes/gray.js"></script>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/overcast/jquery-ui-1.8.21.custom.css" type="text/css" />  
        <STYLE type="text/css">
            .ui-datepicker {font-size:10px;}
        </STYLE>
        <script type="text/javascript">
            window.onload = function () {
                makeMenuActive('Transaction History');
            }

        </script>
    </stripes:layout-component>    
    <stripes:layout-component name="contents">

        <div style="margin-top: 10px;">
            <stripes:form action="/Account.action" focus="" name="frm">
                <fmt:message key="transaction.disclaimer"/><br/>
                <c:if test="${s:getPropertyWithDefault('env.scp.tnf.enabled','false') == 'true'}">
                    <c:if test="${!empty actionBean.breakdownDataset.breakdownDimension}">
                        <fmt:message key="transaction.disclaimer.breakdowndimension"/>
                    </c:if>
                    <br/>
                    <c:if test="${!empty actionBean.trendOverTimeDataset.timeEntrySeries}">
                        <fmt:message key="transaction.disclaimer.timeentryseries"/>
                    </c:if>
                </c:if>
                <br/>
                <stripes:hidden name="accountHistoryQuery.accountId" value="${actionBean.accountHistoryQuery.accountId}"/>
                <stripes:hidden name="accountQuery.accountId" value="${actionBean.accountHistoryQuery.accountId}"/>

                <input type="hidden" name="pageSize" value="10"/>                
                <table>
                    <tr>
                        <td><fmt:message key="Month"/>:</td>
                        <td>
                            <input id="txtDate" readonly="true" type="text" value="${s:formatDateShort(actionBean.searchMonth)}" name="searchMonth" class="required" size="10"/>
                        </td>
                    <br/>
                    </tr>
                    <tr>
                        <td><fmt:message key="scp.transaction.history.productinstance.idlabel"/>:</td>
                        <td>
                            <stripes:select name="productInstance.productInstanceId">
                                <stripes:option value="0">All</stripes:option>
                                <c:forEach items="${actionBean.productInstanceList.productInstances}" var="pi">
                                    <stripes:option value="${pi.productInstanceId}">
                                        ${pi.physicalId}  ${pi.friendlyName}
                                    </stripes:option>
                                </c:forEach>
                            </stripes:select>                          
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" align="right">
                            <input type="submit" class="general_btn" name="retrieveTransactionHistory" value="Search" style="margin-left:90px; margin-top: 5px;"/>
                        </td>
                    </tr>
                    <c:if test="${s:getPropertyWithDefault('env.scp.tnf.enabled','false') == 'true'}">
                        <tr>
                            <td colspan="2"><strong>OR</strong><br/><fmt:message key="scp.tnf.search.msg"/></td>
                        </tr>
                        <tr>
                            <td colspan="2" align="right">
                                <input type="submit" class="general_btn" name="retrieveTNFData" value="Search" style="margin-left:90px; margin-top: 5px;"/>
                            </td>
                        </tr>
                    </c:if>
                </table>

                <br/>

            </stripes:form>
            <br/>
            <br/>
            <c:if test="${s:getProperty('env.scp.session.barchart.enabled') == 'true' && !empty actionBean.accountSummary.periodSummaries}">
                <div id="container" style="min-width: 400px; height: 400px; margin: 0 auto"></div>      
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
            </c:if>
            <br/>
            <br/>
            <c:if test="${s:getPropertyWithDefault('env.scp.tnf.enabled','false') == 'true'}">
                <c:if test="${!empty actionBean.breakdownDataset.breakdownDimension}">
                    <div id="container1" style="width: 100%; height: 400px; margin: 0 auto"></div>
                </c:if>
                <br/>
                <c:if test="${!empty actionBean.trendOverTimeDataset.timeEntrySeries}">
                    <div id="datatab_container" style="width: 100%; height: 400px; margin: 0 auto"></div>
                </c:if>               
            </c:if>
            <br/>

            <c:if test="${!empty actionBean.accountHistory.transactionRecords}">
                <stripes:form action="/Account.action">
                    <input type="hidden" name="pageStart" value="${actionBean.pageStart}"/>
                    <input type="hidden" name="pageMax" value="${s:getListSize(actionBean.accountHistory.transactionRecords)}"/>
                    <input type="hidden" name="action" value="retrieveTransactionHistory"/>                    
                    <c:set var="transactionListSize" value="${s:getListSize(actionBean.accountHistory.transactionRecords)}"/>

                    <c:if test="${transactionListSize > 10}">
                        <stripes:wizard-fields/>
                        <table style="font-size: 11px; float: right;">
                            <tr>
                                <td><input type="submit" class="button_list_navigation" name="pageFirst" value="First"/></td>
                                <td><input type="submit" class="button_list_navigation" name="pageBack" value="Back"/></td>
                                <td><input type="submit" class="button_list_navigation" name="pageNext" value="Next"/></td>
                                <td><input type="submit" class="button_list_navigation" name="pageLast" value="Last"/></td>
                            </tr>
                        </table>
                    </c:if>

                </stripes:form>   
                <table class="clear">
                    <tr>
                        <td colspan="2"><b><fmt:message key="transactions"/></b></td>
                    </tr>                    
                </table>
                <table class="greentbl" width="99%">                    
                    <tr>
                        <th><fmt:message key="transaction.sourcedest"/></th>
                        <th><fmt:message key="transaction.value"/></th>
                        <th><fmt:message key="transaction.totalunits"/></th>
                        <th><fmt:message key="transaction.ucunits"/></th>
                        <th><fmt:message key="transaction.type"/></th>
                        <th><fmt:message key="transaction.description"/></th>
                        <th><fmt:message key="transaction.datetime"/></th>
                        <th><fmt:message key="transaction.device"/></th>
                        <th>SIM</th>
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
                            <td><fmt:message key="event.${transactionRecord.transactionType}"/></td>
                            <td>${transactionRecord.description}</td>
                            <td>${s:formatDateLong(transactionRecord.startDate)} - ${s:formatDateLong(transactionRecord.endDate)}</td>
                            <td>${s:breakUp(fn:replace(transactionRecord.sourceDevice,"IMEISV=",""),8)}</td>
                            <td>${s:breakUp(s:getServiceInstanceICCID(transactionRecord.serviceInstanceId),10)}</td>
                        </tr>
                        <c:set var="loopCount">${loop.count}</c:set>
                    </c:forEach>
                </table>
                <p  style="font-size: 11px; float: right;">
                    <fmt:message key="results.xtoyofz" >
                        <fmt:param>${actionBean.pageStart+1}</fmt:param>
                        <fmt:param>${actionBean.pageStart+loopCount}</fmt:param>
                        <fmt:param>${actionBean.accountHistory.resultsReturned}</fmt:param>
                    </fmt:message>
                </p>
            </c:if>  

            <script type="text/javascript">
                var $j = jQuery.noConflict();

                var dateNow = new Date();

                Highcharts.setOptions({
                    global: {
                        useUTC: true
                    }
                });

                $j(document).ready(function () {
                    $j('#txtDate').datepicker({
                        changeMonth: true,
                        changeYear: true,
                        showOn: 'button',
                        buttonText: "..",
                        autoSize: true,
                        dateFormat: 'yy/mm/dd',
                        maxDate: dateNow,
                        onClose: function () {
                            var iMonth = $j("#ui-datepicker-div .ui-datepicker-month :selected").val();
                            var iYear = $j("#ui-datepicker-div .ui-datepicker-year :selected").val();

                            $j(this).datepicker('setDate', new Date(iYear, iMonth, 1));
                            $j(this).datepicker('refresh');
                        }

                    });

                    $j("#txtDate").focus(function () {
                        $j(".ui-datepicker-calendar").hide();
                        $j("#ui-datepicker-div").position({
                            my: "center top",
                            at: "center bottom",
                            of: $j(this)
                        });
                    });

                    $j("#txtDate").blur(function () {
                        $j(".ui-datepicker-calendar").hide();
                    });



                    $j('#txtFromDate').datepicker({
                        changeMonth: true,
                        changeYear: true,
                        showOn: 'button',
                        buttonText: "..",
                        autoSize: true,
                        dateFormat: 'yy/mm/dd',
                        maxDate: dateNow,
                        onClose: function () {
                            var iMonth = $j("#ui-datepicker-div .ui-datepicker-month :selected").val();
                            var iYear = $j("#ui-datepicker-div .ui-datepicker-year :selected").val();

                            $j(this).datepicker('setDate', new Date(iYear, iMonth, 1));
                            $j(this).datepicker('refresh');
                        }

                    });

                    $j("#txtFromDate").focus(function () {
                        $j(".ui-datepicker-calendar").hide();
                        $j("#ui-datepicker-div").position({
                            my: "center top",
                            at: "center bottom",
                            of: $j(this)
                        });
                    });

                    $j("#txtFromDate").blur(function () {
                        $j(".ui-datepicker-calendar").hide();
                    });

                });


                function timeConverter(UNIX_timestamp) {
                    var a = new Date(UNIX_timestamp);
                    return a;
                }

                var graphTitle = "${s:formatDateShortMonthYear(actionBean.searchMonth)}" + " Data Usage for Account " + "${actionBean.accountSummaryQuery.accountId}";

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
                var sessionDetailBarGraphJson = JSON.parse('${actionBean.sessionDetailBarGraphJson}');
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

                    for (var j = 0; j < sessionDetailBarGraphJson.resultsReturned; j++) {
                        var periodSummary = sessionDetailBarGraphJson.periodSummaries[j];
                        var transactionType = periodSummary.transactionType;
                        var eventUnitsValue = 0.0;

                        if (transactionType === typeParam) {

                            eventUnitsValue = periodSummary.totalUnits;
                            if (transactionType !== 'Data') {
                                eventUnitsValue = periodSummary.unitCreditBaselineUnits;
                            }

                            var unitValue = parseFloat(eventUnitsValue);
                            var totalUnitsAsYAxis = 0.0;

                            <%--console.log("Period: ", periodSummary.period);--%>
                            var periodYear = "20" + periodSummary.period.substring(0, 2);
                            var periodMonth = periodSummary.period.substring(2, 4);
                            var periodDay = periodSummary.period.substring(4, 6);
                            if (periodSummary.period.length < 8) {
                                periodSummary.period = periodSummary.period + "00";
                            }
                            var periodHour = periodSummary.period.substring(7, 9);
                            var newDate = Date.UTC(periodYear, periodMonth - 1, periodDay, periodHour);

                            if (unitValue > 0 && unitValue < 1) {
                                totalUnitsAsYAxis = Math.round(parseFloat(unitValue) / ${s:getPropertyWithDefault('env.portals.data.volume.display.denominator', '1000000')} * 10) / 10;
                            } else {
                                totalUnitsAsYAxis = Math.round(parseFloat(unitValue) / ${s:getPropertyWithDefault('env.portals.data.volume.display.denominator', '1000000')});//convert to MB
                            }
                            <%--console.log("Date : " + newDate + " Value : " + totalUnitsAsYAxis);--%>
                            periodDataSummary.push([newDate, totalUnitsAsYAxis]);

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
                    seriesHCArray.push(seriesObj);

                }

                function initData() {
                    graphPlotPointsColumnFactory('Data');//Only data for now!!!  
                    graphPlotPointsColumnFactory('SMS');
                    graphPlotPointsColumnFactory('Voice');
                }

                var legendValue = "${actionBean.accountSummaryQuery.accountId}";


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
                                pointPadding: 0, // Defaults to 0.1
                                groupPadding: 0.01 // Defaults to 0.2
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

                });
                </c:if>
            </script> 

            <script>
                <c:if test="${s:getPropertyWithDefault('env.scp.tnf.enabled','false') == 'true'}">
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
                var offset = 2 - browserOffset; <%--SERVER OFFSET IS BASED ON JHB TIME --%>
                var seriesHCArrayTNF = [];


                    <c:set var="granularityString" value="${fn:split(actionBean.timeRange.granularity, '_')}" />;
                    <c:set var="granularityUnit" value="${granularityString[1]}" />;


                function getTNFGraphTitle() {
                    var granularity = '${granularityUnit}';

                    if (granularity === 'MINUTES' || granularity === 'HOUR') {
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
                var fullDate = timeConverter(timestampValue + offset * 3600 * 1000); // Put the date into the timezone of the server
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
                    pointWidth: 20
                };
                seriesHCArrayTNF.push(totSeries);
                    </c:forEach>

                function getBreakdownDatasetBreakdownDimensionDataAsPie() {
                    var pieData = [];
                    <c:forEach items="${actionBean.breakdownDataset.breakdownDimension}" var="dimension" varStatus="loop">
                    var value = 0.0;
                        <c:forEach items="${dimension.breakdownKpi}" var="breakdownKpi" varStatus="loopDimension">
                    value = ${breakdownKpi.breakdownValue};
                        </c:forEach>
                    value = parseFloat(value);
                    if (value > 0 && value < 1) {
                        value = Math.round(parseFloat(value) / ${s:getPropertyWithDefault('env.portals.data.volume.display.denominator', '1000000')} * 10) / 10;
                    } else {
                        value = Math.round((parseFloat(value) / ${s:getPropertyWithDefault('env.portals.data.volume.display.denominator', '1000000')}).toFixed(2));//convert to MB
                    }
                    pieData.push(['${dimension.dimensionName}', value]);
                    </c:forEach>

                    return pieData;
                }


                var breakdownDimensionDataAsPie = {
                    data: getBreakdownDatasetBreakdownDimensionDataAsPie()
                };


                $j(function () {
                    $j('#container1').highcharts({
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
                            pointFormat: '{point.name}: <b>{point.y} ${s:getPropertyWithDefault('env.portals.data.volume.display.suffix', 'MB')}</b>'
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
                                    format: '{point.name}: <b>{point.y} ${s:getPropertyWithDefault('env.portals.data.volume.display.suffix', 'MB')}</b>',
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
                    $j('#datatab_container').highcharts({
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