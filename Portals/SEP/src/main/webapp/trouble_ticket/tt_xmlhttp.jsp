<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/include/sep_include.jsp" %>
<%@page import="com.smilecoms.commons.stripes.SmileActionBean" %>
<c:choose>
    <c:when test="${s:getListSize(actionBean.TTIssueList.TTIssueList) > 0}">
        <table class="clear">
            <tr>
                <td colspan="2"><b><fmt:message key="trouble.tickets"/></b></td>
            </tr>
        </table>

        <table class="green" width="99%">
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
            <c:forEach items="${actionBean.TTIssueList.TTIssueList}" var="ttIssue" varStatus="loop">                        
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
    </c:when>
    <c:otherwise>
        <%-- <table class="clear">
            <tr>
                <td colspan="2"><b><fmt:message key="trouble.tickets"/></b></td>
            </tr>
        </table>

        <table class="green" width="99%"> 
            <tr>
                <td>
                    <fmt:message key="trouble.ticket.data.not.available"/>
                </td>
            </tr>
        </table>--%>
    </c:otherwise>
</c:choose>