<%@ include file="/include/sep_include.jsp" %>
<%@page import="com.smilecoms.commons.stripes.SmileActionBean" %>
<c:set var="title">
    <fmt:message key="trouble.ticket.info"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript" xml:space="preserve">
            function openTroubleTicket(address) {
                var newWin = window.open(address, "subWindow", "height=500,width=700,resizable=yes,scrollbars=yes");
            }
        </script>    

        <div id="entity">
            <% if (SmileActionBean.getIsIndirectChannelPartner(request)) {%>
            <c:set var="isINICPROLE" value="true"/>
            <%} else {%>
            <c:set var="isINICPROLE" value="false"/>
            <% }%>
            <table class="entity_header">    
                <tr>
                    <td>
                        <c:if test="${(isINICPROLE == 'false')}">
                            <fmt:message key="customer">
                                <fmt:param>${actionBean.customer.customerId}</fmt:param>
                            </fmt:message>
                            ${actionBean.customer.customerId}
                        </c:if>
                        <c:if test="${(isINICPROLE == 'true')}">
                            <c:if test="${actionBean.icpSalesAgentIssueViewed}">
                                ${actionBean.icpSalesAgentId}
                            </c:if>
                            <c:if test="${!actionBean.icpSalesAgentIssueViewed}">
                                ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                            </c:if>
                        </c:if>
                    </td>    
                    <c:if test="${(isINICPROLE == 'false')}">
                        <td align="right">                       
                            <stripes:form action="/TroubleTicket.action">                                
                                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>               
                                <stripes:hidden name="TTIssue.ID" value="${actionBean.TTIssue.ID}"/>      
                                <stripes:select name="entityAction">
                                    <stripes:option value="addTroubleTicketComment"><fmt:message key="add.trouble.ticket.comment"/></stripes:option>
                                    <stripes:option value="retrieveTroubleTicketComments"><fmt:message key="view.trouble.ticket.comments"/></stripes:option>
                                    <stripes:option value="retrieveTroubleTickets"><fmt:message key="trouble.tickets"/></stripes:option>
                                </stripes:select>
                                <stripes:submit name="performEntityAction"/>
                            </stripes:form>
                        </td>
                    </c:if>
                </tr>
            </table>         
            <div>
                <stripes:form action="/TroubleTicket.action" focus="" id="ttEditForm">
                    <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/> 
                    <stripes:hidden name="TTIssue.resolution" value="${actionBean.TTIssue.resolution}"/>
                    <%--<stripes:hidden name="TTIssue.functionalArea" value="${actionBean.TTIssue.functionalArea}"/>--%>
                    <stripes:hidden name="TTIssue.status" value="${actionBean.TTIssue.status}"/>      
                    <table class="clear">
                        <tr>
                            <td><stripes:label for="trouble.ticket.id"/>:</td>
                            <td>
                                <stripes:text name="TTIssue.ID" value="${actionBean.TTIssue.ID}" readonly="true"/>
                            </td>
                        </tr>
                        <tr>
                            <td><stripes:label for="trouble.ticket.project"/>:</td>
                            <td>
                                <stripes:hidden name="TTIssue.project"/>
                                <stripes:text name="TTIssue.project" value="${actionBean.TTIssue.project}" readonly="true"/>
                            </td>
                        </tr>
                        <%--<tr>
                            <td><stripes:label for="trouble.ticket.fa"/>:</td>
                            <td>
                                <stripes:text name="TTIssue.functionalArea" value="${actionBean.TTIssue.functionalArea}" readonly="true"/>
                            </td>
                        </tr>--%>
                        <tr>
                            <td><stripes:label for="trouble.ticket.issuetype"/>:</td>
                            <td><stripes:text name="TTIssue.issueType" value="${actionBean.TTIssue.issueType}" readonly="true"/><%--<stripes:select name="TTIssue.issueType">
                                    <c:forEach items="${actionBean.TTMetaData.TTIssueTypeList}" var="ttissuetype" varStatus="loop">
                                        <stripes:option value="${ttissuetype.ID}">${ttissuetype.name}</stripes:option>
                                    </c:forEach>
                                </stripes:select>--%>
                            </td>
                        </tr>
                        <tr>
                            <td><stripes:label for="trouble.ticket.summary"/>:</td>
                            <td><stripes:text name="TTIssue.summary" maxlength="50" size="50"  readonly="true"/></td>
                        </tr>
                        <tr>
                            <td><stripes:label for="trouble.ticket.priority"/>:</td>
                            <td><stripes:text name="TTIssue.priority" value="${actionBean.TTIssue.priority}" readonly="true"/><%--<stripes:select name="TTIssue.priority">
                                    <c:forEach items="${actionBean.TTMetaData.TTPriorityList}" var="ttpriority" varStatus="loop">
                                        <stripes:option value="${ttpriority.ID}">${actionBean.TTIssue.priority}</stripes:option>
                                    </c:forEach>
                                </stripes:select>--%>
                            </td>
                        </tr>
                        <tr>
                            <td><stripes:label for="trouble.ticket.created"/>:</td>
                            <td>
                                <input readonly="true" type="text" value="${s:formatDateShort(actionBean.TTIssue.created)}" class="required" size="20"  readonly="true" name="TTIssue.created"/>
                            </td>        
                        </tr>
                        <tr>
                            <td><stripes:label for="trouble.ticket.updated"/>:</td>
                            <td>
                                <input readonly="true" type="text" value="${s:formatDateShort(actionBean.TTIssue.updated)}" class="required" size="20"  readonly="true"/>
                            </td>        
                        </tr>
                        <tr>
                            <td><stripes:label for="trouble.ticket.duedate"/>:</td>
                            <td>
                                <input readonly="true" type="text" value="${s:formatDateShort(actionBean.TTIssue.dueDate)}" name="TTIssue.dueDate" class="required" size="20"/>
                                <%--<input readonly="true" name="datePicker1" type="button" value=".." onclick="displayCalendar(document.forms[1].elements['TTIssue.dueDate'] ,'yyyy/mm/dd',this)">--%>
                            </td>
                        </tr>
                        <tr>
                            <td><stripes:label for="trouble.ticket.status" />:</td>
                            <td><stripes:text name="TTIssue.status" maxlength="15" size="15" readonly="true"/></td>
                        </tr>
                        <tr>
                            <td><stripes:label for="trouble.ticket.assignee"/>:</td>
                            <td><stripes:text name="TTIssue.assignee" maxlength="15" size="15" readonly="true"/></td>
                        </tr>
                        <tr>
                            <td><stripes:label for="trouble.ticket.description"/>:</td>
                            <td><stripes:textarea name="TTIssue.description" cols="50" rows="5"  readonly="true"/></td>
                        </tr>
                        <tr>
                            <td><stripes:label for="trouble.ticket.watcher"/>:</td>
                            <td><stripes:checkbox name="watchIssue" value="true" readonly="true"></stripes:checkbox></td>
                            <stripes:hidden name="oldWatchIssue" value="${actionBean.watchIssue}"/>
                        </tr>
                        <tr>
                            <%--
                            <td colspan="2">
                                <span class="button">
                                    <stripes:submit name="updateTroubleTicket"/>
                                </span>                        
                            </td>
                            --%>
                        </tr>  				
                    </table>            
                </stripes:form>
            </div>
            <div>
                <table class="entity_header">    
                    <tr>
                        <td>
                            <fmt:message key="trouble.ticket.comments">
                            </fmt:message>
                        </td>
                    </tr>
                </table>

                <table class="green" width="99%"> 
                    <c:choose>
                        <c:when test="${s:getListSize(actionBean.TTCommentList.TTCommentList) > 0}">
                            <c:forEach items="${actionBean.TTCommentList.TTCommentList}" var="ttComment" varStatus="loop">         
                                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                    <td align='left'>${ttComment.author}</td>                                            
                                    <td align='left'>${s:formatDateLong(ttComment.updated)}</td>                                            
                                    <td align='left' colspan="2">${ttComment.body}</td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td colspan="3">
                                    <fmt:message key="comments.not.available"/>
                                </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                </table>                   
            </div>
        </div>
    </stripes:layout-component>    
</stripes:layout-render>

