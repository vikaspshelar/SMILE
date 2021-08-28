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
                    <td align="right">                       
                        <stripes:form action="/TroubleTicket.action">                                
                            <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>                                
                            <stripes:select name="entityAction">
                                <stripes:option value="startWizard"><fmt:message key="start.tt.wizard"/></stripes:option>
                                <%--<stripes:option value="addTroubleTicket"><fmt:message key="add.trouble.ticket"/></stripes:option>--%>
                                <stripes:option value="retrieveTTCustomer"><fmt:message key="manage.customer"/></stripes:option>
                                </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>
            <stripes:form action="/TroubleTicket.action" focus="">
                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                <stripes:hidden name="TTIssueQuery.customerId" value="${actionBean.customer.customerId}"/>
                <input type="hidden" name="pageSize" value="10"/>
                
                <table class="clear">
                    <tr>
                        <td><stripes:label for="datefrom"/>:</td>
                        <td>
                            <input readonly="true" type="text" value="${s:formatDateShort(actionBean.TTIssueQuery.createdDateFrom)}" name="TTIssueQuery.createdDateFrom" class="required" size="20"/>
                            <input name="datePicker1" type="button" value=".." onclick="displayCalendar(document.forms[1].elements['TTIssueQuery.createdDateFrom'] ,'yyyy/mm/dd',this)"/>
                        </td>        
                    </tr>
                    <tr>
                        <td><stripes:label for="dateto"/>:</td>
                        <td>
                            <input readonly="true" type="text" value="${s:formatDateShort(actionBean.TTIssueQuery.createdDateTo)}" name="TTIssueQuery.createdDateTo" class="required" size="20"/>
                            <input name="datePicker1" type="button" value=".." onclick="displayCalendar(document.forms[1].elements['TTIssueQuery.createdDateTo'] ,'yyyy/mm/dd',this)"/>
                        </td>        
                    </tr>
                    <tr>
                        <td><stripes:label for="result.limit"/>:</td>
                        <td>
                            <stripes:select name="TTIssueQuery.resultLimit">
                                <stripes:option value="10">10</stripes:option>
                                <stripes:option value="20">20</stripes:option>
                                <stripes:option value="30">30</stripes:option>
                                <stripes:option value="40">40</stripes:option>
                                <stripes:option value="50">50</stripes:option>
                                <stripes:option value="100">100</stripes:option>
                                <stripes:option value="-1"><fmt:message key="All"/></stripes:option>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>                              
                        <td>
                            <span class="button">
                                <stripes:submit name="queryTroubleTickets"/>
                            </span>                        
                        </td>
                    </tr>  
                </table>
            </stripes:form>           
        </div>
        <br/>
        <c:if test="${!empty actionBean.TTIssueList.TTIssueList}">
            <stripes:form action="/TroubleTicket.action" id="msglistForm">
                    <input type="hidden" name="pageStart" value="${actionBean.pageStart}"/>
                    <input type="hidden" name="pageMax" value="${s:getListSize(actionBean.TTIssueList.TTIssueList)}"/>
                    <input type="hidden" name="action" value="queryTroubleTickets"/>

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
                        <%--<th><fmt:message key="trouble.ticket.fa"/></th>--%>
                        <th><fmt:message key="trouble.ticket.priority"/></th>
                        <th><fmt:message key="trouble.ticket.status"/></th>
                        <th><fmt:message key="trouble.ticket.assignee"/></th>
                        <th><fmt:message key="trouble.ticket.created"/></th>
                        <th><fmt:message key="open"/></th>
                    </tr>
                    <c:forEach items="${actionBean.TTIssueList.TTIssueList}" var="ttIssue" begin="${actionBean.pageStart}" end="${actionBean.pageStart + actionBean.pageSize -1}" varStatus="loop">                        
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            <td align='center'>${ttIssue.ID}</td>
                            <td align='center'>${ttIssue.project}</td>
                            <%--<td align='center'>${ttIssue.functionalArea}</td>--%>
                            <td align='center'>${ttIssue.priority}</td>
                            <td align='center'>${ttIssue.status}</td>
                            <td align='center'>${ttIssue.assignee}</td>
                            <td align='center'>${s:formatDateLong(ttIssue.created)}</td>   
                        <td>           
                            <stripes:form action="/TroubleTicket.action" class="buttonOnly">
                                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/> 
                                <stripes:hidden name="TTIssue.ID" value="${ttIssue.ID}"/>
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
    </stripes:layout-component>   
    </stripes:layout-render>
    
    
