<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="transaction.history"/>
</c:set>

<html>
    <head>
        <meta http-equiv="Content-type" content="text/html; charset=UTF-8" />
        <title>${title} - ${actionBean.accountHistoryQuery.accountId}</title>
        <meta http-equiv="imagetoolbar" content="no" />
        <meta name="MSSmartTagsPreventParsing" content="true" />
        <link rel="stylesheet" media="all" type="text/css" href="${pageContext.request.contextPath}/css/style.css"/>    
        <%
            response.addHeader("Pragma", "No-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.addDateHeader("Expires", 1);
        %>
    </head>
    <body>
        <h3>${actionBean.accountHistoryQuery.accountId}</h3>
        <br/>    
        <table class="green">
            <tr>
                
                <th><fmt:message key="transaction.source"/></th>
                <th><fmt:message key="transaction.destination"/></th>
                <th><fmt:message key="transaction.value"/></th>
                <th><fmt:message key="transaction.totalunits"/></th>
                <th><fmt:message key="transaction.ucunits"/></th>
                <th><fmt:message key="transaction.balance"/></th>                        
                <th><fmt:message key="transaction.type"/></th>
                <th><fmt:message key="transaction.description"/></th>
                <th><fmt:message key="transaction.datetime"/></th>
                <th><fmt:message key="transaction.device"/></th>
                <th><fmt:message key="transaction.status"/></th>
                <th><fmt:message key="transaction.termcode"/></th>
            </tr>

            <c:forEach items="${s:orderList(actionBean.accountHistory.transactionRecords,'getStartDate','desc')}" var="transactionRecord" varStatus="loop">
                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                    <td>${s:getPhoneNumberFromSipURI(transactionRecord.source)}</td>
                    <td>${s:getPhoneNumberFromSipURI(transactionRecord.destination)}</td>
                    <td align='center'>${s:convertCentsToCurrencyLong(transactionRecord.amountInCents)}</td>
                    <c:if test="${fn:startsWith(transactionRecord.description, 'Data')}">
                        <td align='center'>
                            <stripes:link  href="/Account.action" event="retrieveSessionDetail">
                                <stripes:param name="transactionRecordId" value="${transactionRecord.transactionRecordId}"/>
                                ${s:displayVolumeAsString(transactionRecord.totalUnits, 'Byte')}
                            </stripes:link>
                        </td>
                        <td align='center'>${s:displayVolumeAsString(transactionRecord.unitCreditUnits, 'Byte')}</td>
                    </c:if>
                    <c:if test="${fn:startsWith(transactionRecord.description, 'SMS')}">
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
                        <td align='center'>
                            ${s:displayVolumeAsString(transactionRecord.totalUnits, 'Byte')}
                        </td>
                        <td align='center'>${s:displayVolumeAsString(transactionRecord.unitCreditUnits, 'Byte')}</td>
                    </c:if>
                    <c:if test="${fn:startsWith(transactionRecord.description, 'Voice')}">
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
                        <td><c:if test="${transactionRecord.totalUnits != 0}">${transactionRecord.totalUnits}</c:if></td>
                            <td align='center'>
                            <c:if test="${transactionRecord.unitCreditUnits != 0}">${transactionRecord.unitCreditUnits*-1}</c:if>
                            <c:if test="${transactionRecord.unitCreditBaselineUnits != 0}">
                                (${transactionRecord.unitCreditBaselineUnits*-1})
                            </c:if>
                        </td>
                    </c:if>
                    <td align='center'>${s:convertCentsToCurrencyLong(transactionRecord.accountBalanceRemainingInCents)}</td>                            
                    <td><fmt:message key="event.${transactionRecord.transactionType}"/></td>
                    <td>${transactionRecord.description}</td>
                    <td>${s:formatDateLong(transactionRecord.startDate)} - ${s:formatDateLong(transactionRecord.endDate)}</td>
                    <td>${transactionRecord.sourceDevice}</td>
                    <td>${transactionRecord.status}</td>
                    <td>${transactionRecord.termCode}</td>
                </tr>
            </c:forEach>                    
        </table>  
        <br/><br/>
        <stripes:link href="/Account.action" event="retrieveAccount">                            
            <stripes:param name="accountQuery.accountId" value="${actionBean.accountHistoryQuery.accountId}"/>
            <fmt:message key="go.back.to"> <fmt:param>${actionBean.accountHistoryQuery.accountId}</fmt:param></fmt:message>
        </stripes:link>
    </body>
