<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="view.trouble.ticket.comments">
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
                            <stripes:hidden name="TTIssue.ID" value="${actionBean.TTIssue.ID}"/>      
                            <stripes:select name="entityAction">
                                <stripes:option value="addTroubleTicketComment"><fmt:message key="add.trouble.ticket.comment"/></stripes:option>
                                <stripes:option value="editTroubleTicket"><fmt:message key="trouble.ticket.edit"/></stripes:option>
                                </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>
        </div>
        <stripes:form action="/TroubleTicket.action" id="msglistForm">
            <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/> 
                <c:forEach items="${actionBean.TTCommentList.TTCommentList}" var="ttComment" varStatus="loop">         
                    <div>               
                        <table class="green">
                                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                    <td align='left'>${s:formatDateLong(ttComment.updated)}</td> 
                                    <td align='left'>
                                        <c:if test="${!empty ttComment.author}">
                                          ${ttComment.author}:   
                                        </c:if>
                                        ${ttComment.body}
                                    </td>
                                </tr>
                                <%--<tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                    <td align='center'>${ttComment.body}</td>
                                </tr>--%>
                        </table>       
                    </div>            
                </c:forEach>                         
        </stripes:form>
    </stripes:layout-component>   
    </stripes:layout-render>
    
    
