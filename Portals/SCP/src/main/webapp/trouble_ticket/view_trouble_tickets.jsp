<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="view.trouble.tickets">
    </fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">
        <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-1.7.2.min.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-ui-1.8.21.custom.min.js"></script>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/overcast/jquery-ui-1.8.21.custom.css" type="text/css" />        
        <script type="text/javascript">
            window.onload = function() {
                makeMenuActive('Help_TroubleTicketsPage');
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <div style="margin-top: 10px;">
            <script type="text/javascript">
                var $j = jQuery.noConflict();

                $j(document).ready(function() {
                    $j("#dateFrom").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", maxDate: new Date(), changeYear: true, changeMonth: true});
                });
                $j(document).ready(function() {
                    $j("#dateTo").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", maxDate: new Date(), changeYear: true, changeMonth: true, limitToToday: true});
                });

            </script>


            <jsp:include page="/layout/my_details_left_banner.jsp"/>
            
            <div  style="margin-top: 2px;" class="accounts_table account_form nine columns">
                <stripes:form action="/SCPTroubleTicket.action" focus="">
                    <stripes:hidden name="customer.customerId" value="${actionBean.userCustomerIdFromSession}"/>
                    <stripes:hidden name="TTIssueQuery.customerId" value="${actionBean.userCustomerIdFromSession}"/>
                    <input type="hidden" name="pageSize" value="10"/>

                    <table style="padding: 5px;">
                        <tr>
                            <td><fmt:message key="scp.datefrom"/>:&nbsp;&nbsp;</td>

                            <td>
                                <input id="dateFrom" readonly="true" style="padding-left: 4px;" type="text" value="${s:formatDateShort(actionBean.TTIssueQuery.createdDateFrom)}" name="TTIssueQuery.createdDateFrom" class="required" size="10"/> 
                            </td>        
                        </tr>
                        <tr>
                            <td><fmt:message key="scp.dateto"/>:</td>
                            <td>
                                <input id="dateTo" readonly="true" style="padding-left: 4px;" type="text" value="${s:formatDateShort(actionBean.TTIssueQuery.createdDateTo)}" name="TTIssueQuery.createdDateTo" class="required" size="10"/>                            
                            </td>
                        </tr>
                        
                        <tr> 
                            <td></td>
                            <td>
                                <div style="margin-top:-1px;">
                                    <input type="submit" class="button_search" name="queryTroubleTickets" value="Search"/>
                                </div>
                            </td>
                        </tr>  
                    </table>
                </stripes:form>           
            </div>
            <div class="ten columns">
                <br/>
                <c:if test="${!empty actionBean.TTIssueList.TTIssueList}">
                    <stripes:form action="/SCPTroubleTicket.action" id="msglistForm">
                        <input type="hidden" name="pageStart" value="${actionBean.pageStart}"/>
                        <input type="hidden" name="pageMax" value="${s:getListSize(actionBean.TTIssueList.TTIssueList)}"/>
                        <input type="hidden" name="action" value="queryTroubleTickets"/>

                        <stripes:wizard-fields/>
                        <table>
                            <tr>
                                <td><input type="submit" class="button_list_navigation" name="pageFirst" value="First"/></td>
                                <td><input type="submit" class="button_list_navigation" name="pageBack" value="Back"/></td>
                                <td><input type="submit" class="button_list_navigation" name="pageNext" value="Next"/></td>
                                <td><input type="submit" class="button_list_navigation" name="pageLast" value="Last"/></td>
                            </tr>
                        </table>

                    </stripes:form>
                    <table class="greentbl"  width="99%">
                        <tr>
                            <th></th>
                            <th><fmt:message key="trouble.ticket.status"/></th>
                            <th><fmt:message key="trouble.ticket.created"/></th>
                            <th><fmt:message key="trouble.ticket.updated"/></th>
                            <th><fmt:message key="open"/></th>
                        </tr>
                        <c:forEach items="${actionBean.TTIssueList.TTIssueList}" var="ttIssue" begin="${actionBean.pageStart}" end="${actionBean.pageStart + actionBean.pageSize -1}" varStatus="loop">                        
                            <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                <td align='center'>${ttIssue.ID}</td>
                                <td align='center'>${ttIssue.status}</td>
                                <td align='center'>${s:formatDateLong(ttIssue.created)}</td>   
                                <td align="center">${s:formatDateLong(ttIssue.updated)}</td>        
                                <td>           
                                    <stripes:form action="/SCPTroubleTicket.action" class="buttonOnly">
                                        <stripes:hidden name="customer.customerId" value="${actionBean.userCustomerIdFromSession}"/> 
                                        <stripes:hidden name="TTIssue.ID" value="${ttIssue.ID}"/>
                                        <%--<stripes:submit name="editTroubleTicket"/>--%>
                                        <div style="margin-top:-1px;">
                                            <input type="submit" class="button_view" name="editTroubleTicket" value="View"/>
                                        </div>
                                    </stripes:form>
                                </td>                        
                            </tr>
                            <c:set var="loopCount">${loop.count}</c:set>
                        </c:forEach>                         
                    </table>       

                    <fmt:message key="results.xtoyofz">
                        <fmt:param>${actionBean.pageStart+1}</fmt:param>
                        <fmt:param>${actionBean.pageStart+loopCount}</fmt:param>
                        <fmt:param>${s:getListSize(actionBean.TTIssueList.TTIssueList)}</fmt:param>
                    </fmt:message>
                    <br/><br/>

                </c:if>
            </div>
        </div>
        </stripes:layout-component>   
    </stripes:layout-render>


