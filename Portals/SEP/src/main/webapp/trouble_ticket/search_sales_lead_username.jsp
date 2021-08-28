<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="search.user.saleslead"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/TroubleTicket.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">    
            <table class="clear">
                <tr>
                    <td><fmt:message key="jira.user.username"/>:</td>
                    <td><stripes:text name="TTJiraUserQuery.username" size="30" onkeyup="validate(this,'^.{2,50}$','emptyok')"/></td>
                    <td><fmt:message key="andor"/></td>
                </tr>
                
                <tr>
                    <td><fmt:message key="last.name"/>:</td>
                    <td><stripes:text name="TTJiraUserQuery.lastName" size="30" onkeyup="validate(this,'^.{2,50}$','emptyok')"/></td>
                    <td><fmt:message key="andor"/></td>
                </tr>
                <tr>
                    <td><fmt:message key="first.name"/>:</td>
                    <td><stripes:text name="TTJiraUserQuery.firstName" size="30" onkeyup="validate(this,'^.{2,50}$','emptyok')"/></td>
                    <td><fmt:message key="andor"/></td>
                </tr>
                <tr>
                    <td>Email:</td>
                    <td><stripes:text name="TTJiraUserQuery.emailAddress" size="30" onkeyup="validate(this,'^.{2,50}$','emptyok')"/></td>
                    <td><fmt:message key="andor"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="result.limit"/>:</td>
                    <td>
                        <stripes:select name="TTJiraUserQuery.resultLimit">
                            <stripes:option value="10">10</stripes:option>
                            <stripes:option value="20">20</stripes:option>
                        </stripes:select>
                    </td>
                </tr>  
                <tr>
                    <td>
                        <span class="button">
                            <stripes:submit name="showSalesLeadUsernamePage"/>
                        </span>
                    </td>
                </tr>
            </table>            
        </stripes:form>
        <br/>
        <br/>        
        <c:if test="${!empty actionBean.TTJiraUserList.jiraUsers}">
            <p><b><fmt:message key="tt.select.assignee.message"/></b></p>
            <table class="green">
                <tr>
                    <th><fmt:message key="id"/></th>
                    <th><fmt:message key="tt.jira.display.name"/></th>
                    <th><fmt:message key="tt.jira.user.email"/></th>
                    <th><fmt:message key="view"/></th>
                </tr>
                <c:forEach items="${actionBean.TTJiraUserList.jiraUsers}" var="jUser" varStatus="loop">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>${jUser.userID}</td>
                        <td>${jUser.fullName}</td>
                        <td>${jUser.email}</td> 
                        <stripes:form action="/TroubleTicket.action">
                        <input type="hidden" name="TTUser.userID" value="${jUser.userID}"/>
                        <td>
                            <stripes:submit name="searchSalesLeadAssignedTickets"/>
                        </td>
                    </stripes:form>
                </tr>                    
            </c:forEach>               
        </table>
    </c:if>     
    </stripes:layout-component>    
</stripes:layout-render>

