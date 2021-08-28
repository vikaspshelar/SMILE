<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="view.trouble.tickets">
    </fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="customer">
                            <fmt:param>${actionBean.customer.customerId}</fmt:param>
                        </fmt:message>
                        ${actionBean.customer.customerId}
                    </td>                        
                </tr>
            </table>
            <stripes:form action="/TroubleTicket.action" focus="">
                <input type="hidden" name="pageSize" value="10"/>

                <table class="clear">
                    <tr>
                        <td><stripes:label for="datefrom"/>:</td>
                        <td>
                            <input readonly="true" type="text" id="dateFrom" value="${s:formatDateShort(actionBean.TTIssueQuery.createdDateFrom)}" name="TTIssueQuery.createdDateFrom" class="required" size="10"/>
                        </td>        
                    </tr>
                    <tr>
                        <td><stripes:label for="dateto"/>:</td>
                        <td>
                            <input id="dateTo" readonly="true" type="text" value="${s:formatDateShort(actionBean.TTIssueQuery.createdDateTo)}" name="TTIssueQuery.createdDateTo" class="required" size="10"/>
                        </td>        
                    </tr>
                    <c:if test="${actionBean.searchAnyICPSalesAgentIssues}">
                        <tr>
                            <td><fmt:message key="andor"/></td>
                        </tr>
                        <tr>
                            <td><fmt:message key="icp.sales.person"/>:</td>
                            <td>
                                <stripes:select name="icpSalesAgentId">
                                    <stripes:option value="0" selected="selected">-- Select Name (Optional) --</stripes:option>
                                    <c:forEach items="${actionBean.salesAgentsForICPOrganisationList}" var="icpAgent" varStatus="loop">
                                        <stripes:option value="${icpAgent.key}">${icpAgent.value}</stripes:option>
                                    </c:forEach>                                
                                </stripes:select>
                            </td>
                        </tr>
                    </c:if>

                    <tr>                              
                        <td>
                            <span class="button">
                                <stripes:submit name="searchIssueForICP"/>
                            </span>                        
                        </td>
                    </tr>  
                </table>
            </stripes:form>  
            <script type="text/javascript">
                var $j = jQuery.noConflict();
                var dateNow = new Date();
                var totalUsagePerPeriod = 0.0;

                $j(document).ready(function() {
                    $j("#dateFrom").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", maxDate: dateNow, changeYear: true, changeMonth: true});
                });
                $j(document).ready(function() {
                    $j("#dateTo").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", maxDate: dateNow, changeYear: true, changeMonth: true});
                });
            </script>

            <br/>
            <c:if test="${!empty actionBean.TTIssueList.TTIssueList}">
                <stripes:form action="/TroubleTicket.action" id="msglistForm">
                    <input type="hidden" name="pageStart" value="${actionBean.pageStart}"/>
                    <input type="hidden" name="pageMax" value="${s:getListSize(actionBean.TTIssueList.TTIssueList)}"/>
                    <input type="hidden" name="action" value="searchIssueForICP"/>

                    <stripes:wizard-fields/>
                    <table>
                        <tr>
                            <td><stripes:submit name="pageFirst"/></td>
                            <td><stripes:submit name="pageBack"/></td>
                            <td><stripes:submit name="pageNext"/></td>
                            <td><stripes:submit name="pageLast"/></td>
                        </tr>
                    </table>
                    <br/>
                </stripes:form>
                <table class="green">
                    <tr>
                        <th></th>
                        <th><fmt:message key="trouble.ticket.project"/></th>
                        <th><fmt:message key="trouble.ticket.status"/></th>
                        <th><fmt:message key="trouble.ticket.assignee"/></th>
                        <th><fmt:message key="trouble.ticket.created"/></th>
                        <th><fmt:message key="open"/></th>
                    </tr>
                    <c:forEach items="${actionBean.TTIssueList.TTIssueList}" var="ttIssue" begin="${actionBean.pageStart}" end="${actionBean.pageStart + actionBean.pageSize -1}" varStatus="loop">                        
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            <td align='center'>${ttIssue.ID}</td>
                            <td align='center'>${ttIssue.project}</td>
                            <td align='center'>${ttIssue.status}</td>
                            <td align='center'>${ttIssue.assignee}</td>
                            <td align='center'>${s:formatDateLong(ttIssue.created)}</td>  
                            <td>   
                                <stripes:form action="/TroubleTicket.action" class="buttonOnly">
                                    <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/> 
                                    <stripes:hidden name="TTIssue.ID" value="${ttIssue.ID}"/>
                                    <stripes:hidden name="icpSalesAgentId" value="${actionBean.icpSalesAgentId}"/>
                                    
                                    <stripes:submit name="editTroubleTicket"/>
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
    </stripes:layout-component>   
</stripes:layout-render>


