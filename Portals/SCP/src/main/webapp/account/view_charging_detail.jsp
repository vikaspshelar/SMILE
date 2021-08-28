<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="transaction.history"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">          
        <script type="text/javascript">
            window.onload = function() {
                 makeMenuActive('Transaction History');
            }
        </script>
    </stripes:layout-component>    
    <stripes:layout-component name="contents">

        <div style="margin-top: 10px;">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="transaction.record"/>:
                        ${actionBean.transactionRecordId}
                    </td>                    
                </tr>
            </table>



            <br/>
            <div id="graphdiv" style="width:100%;"></div>
            <br/><br/>
            Click and Drag on Graph to Zoom in. Double Click to Zoom out.
            <br/><br/>
            <c:set var="rec" value="${actionBean.accountHistory.transactionRecords[0]}"/>

            <table class="greentbl" width="99%">
                <tr>
                    <th>Attribute</th>
                    <th>Value</th>
                </tr>
                <tr style="background-color: #EBF2D4;">
                    <td>Transaction Record Id</td>
                    <td>${rec.transactionRecordId}</td>
                </tr>
                <tr>
                    <td>Transaction Type</td>
                    <td>${rec.transactionType}</td>
                </tr>
                <tr style="background-color: #EBF2D4;">
                    <td>Session Start Date</td>
                    <td>${s:formatDateLong(rec.startDate)}</td>
                </tr>
                <tr>
                    <td>Session End Date</td>  
                    <td>${s:formatDateLong(rec.endDate)}</td>
                </tr>
                <tr style="background-color: #EBF2D4;">
                    <td>Source</td>    
                    <td>${rec.source}</td>
                </tr>
                <tr>
                    <td>Destination</td> 
                    <td>${rec.destination}</td>
                </tr>
                <tr style="background-color: #EBF2D4;">
                    <td>Description</td>    
                    <td>${rec.description}</td>
                </tr>
                <tr>
                    <td>Account Monetary Change</td>
                    <td>${s:convertCentsToCurrencyLong(rec.amountInCents)}</td>
                </tr>
                <tr style="background-color: #EBF2D4;">
                    <td>Bundle Change</td>
                    <td>${s:formatBigNumber(rec.unitCreditUnits)}</td>
                </tr>
                <tr>
                    <td>Account Balance Remaining</td>
                    <td>${s:convertCentsToCurrencyLong(rec.accountBalanceRemainingInCents)}</td>
                </tr>
                <tr style="background-color: #EBF2D4;">
                    <td>Total Units Used in Session</td>
                    <td>${s:formatBigNumber(rec.totalUnits)}</td>
                </tr>                
                <tr>
                    <td>Source Device IMEISV</td>                    
                    <td>${rec.sourceDevice}</td>
                </tr>
                <tr style="background-color: #EBF2D4;">
                    <td>Transaction Record Status</td>
                    <td>${rec.status}</td>
                </tr>
                <tr>
                    <td>Account Id</td>
                    <td>${rec.accountId}</td>
                </tr>                
            </table>




            <script type="text/javascript">
                var today = new Date();
                var browserOffset = -(today.getTimezoneOffset() / 60);
                var serveroffset = ${s:getTimeZoneOffsetHours()};
                var offset = serveroffset - browserOffset;
                g = new Dygraph(
                        document.getElementById("graphdiv"),
                        "Date,MB\n" + "${actionBean.sessionDetailLineGraph}",
                        {
                            xValueFormatter: Dygraph.dateString_,
                            axisLabelFontSize: 12,
                            xValueParser: function(x) {
                                return parseInt(x) + offset * 3600 * 1000;
                            },
                            xTicker: Dygraph.dateTicker,
                            title: "Smile Data Session Tracker",
                            titleHeight: 20,
                            strokeWidth: 2,
                            highlightCircleSize: 4,
                            colors: ["#50922C"],
                            ylabel: "Data Usage In Session (MB)",
                            yLabelWidth: 15,
                            axes: {
                                x: {
                                    axisLabelFormatter: function(d, gran) {
                                        return d.getDate() + "/" + (d.getMonth() + 1) + " " + Dygraph.zeropad(d.getHours()) + ":" + Dygraph.zeropad(d.getMinutes());
                                    }
                                }
                            }
                        });
            </script>

            <br/>
            <input type="button" value="<fmt:message key="back"/>" onclick="previousPage();" class="general_btn_inverted"/>



        </div>                
    </stripes:layout-component>    
</stripes:layout-render>