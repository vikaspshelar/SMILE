<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="tt.salesleads">
    </fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    
    <stripes:layout-component name="contents">
        <c:choose>
            <c:when test="${!empty actionBean.TTIssueList.TTIssueList}">
                <c:set var="JiraURL" value="${s:getProperty('env.jira.server.url')}"/>
                <c:set var="countryName" value="${s:getProperty('env.country.name')}"/>
                <table class="green">
                    <tr>
                        <th></th>
                        <th><fmt:message key="trouble.ticket.status"/></th>
                        <th><fmt:message key="trouble.ticket.assignee"/></th>
                        <th><fmt:message key="trouble.ticket.created"/></th>
                        <th><fmt:message key="tt.saleslead.xinfo"/></th>
                        <th><fmt:message key="tt.saleslead.convert.to.customer"/></th>
                    </tr>
                    <c:forEach items="${actionBean.TTIssueList.TTIssueList}" var="ttIssue" varStatus="loop">                        
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            <td align='center'>
                                <a href="${JiraURL}/browse/${ttIssue.ID}" title="View this ticket" target="_blank">${ttIssue.ID}</a>
                            </td>
                            <td align='center'>${ttIssue.status}</td>
                            <td align='center'>${ttIssue.assignee}</td>
                            <td align='center'>${s:formatDateLong(ttIssue.created)}</td>
                            <td align='left'>
                                <c:forEach items="${ttIssue.mindMapFields.jiraField}" var="ttFields">
                                    ${ttFields.fieldName}=> ${ttFields.fieldValue} <br/>
                                </c:forEach>
                            </td>
                            <td>           
                                <stripes:form action="/TroubleTicket.action" class="buttonOnly">
                                    <c:forEach items="${ttIssue.mindMapFields.jiraField}" var="ttFields">
                                       <!-- basic data -->
                                        <c:if test="${ttFields.fieldName eq 'Customer Name'}">
                                            <stripes:hidden name="customer.firstName" value="${ttFields.fieldValue}" />
                                        </c:if>
                                        <c:if test="${ttFields.fieldName eq 'Customer Last Name'}">
                                            <stripes:hidden name="customer.lastName" value="${ttFields.fieldValue}" />
                                        </c:if>
                                        <c:if test="${ttFields.fieldName eq 'Customer Email'}">
                                            <stripes:hidden name="customer.emailAddress" value="${ttFields.fieldValue}" />
                                        </c:if>
                                        <c:if test="${ttFields.fieldName eq 'Smile Customer Phone'}">
                                            <stripes:hidden name="customer.alternativeContact1" value="${ttFields.fieldValue}" />
                                        </c:if>
                                        
                                        <!-- address -->
                                        <c:if test="${ttFields.fieldName eq 'Add:Location'}">
                                            <stripes:hidden name="customer.addresses[0].line1" value="${ttFields.fieldValue}" />
                                        </c:if>
                                        <c:if test="${ttFields.fieldName eq 'Add:Street'}">
                                            <stripes:hidden name="customer.addresses[0].line2" value="${ttFields.fieldValue}" />
                                        </c:if>
                                        <c:if test="${ttFields.fieldName eq 'Add:Town/City'}">
                                            <stripes:hidden name="customer.addresses[0].town" value="${ttFields.fieldValue}" />
                                        </c:if>
                                        <c:if test="${ttFields.fieldName eq 'Address Code'}">
                                            <stripes:hidden name="customer.addresses[0].code" value="${ttFields.fieldValue}" />
                                        </c:if>
                                        <c:if test="${ttFields.fieldName eq 'Add:Town/City'}">
                                            <stripes:hidden name="customer.addresses[0].country" value="${countryName}" />
                                        </c:if>
                                        
                                    </c:forEach>
                                    <stripes:hidden name="customer.classification" value="customer" />
                                    <stripes:hidden name="TTIssue.ID" value="${ttIssue.ID}"/>
                                    <stripes:submit name="showAddCustomerBasicDetailsPage"/>
                                </stripes:form>
                            </td>  
                        </tr>
                    </c:forEach>                    
                </table>       
            </c:when>
            <c:when test="${!empty actionBean.TTIssueList.TTIssueCount and actionBean.TTIssueList.TTIssueCount eq 0}">
                <table class="green" width="99%"> 
                    <tr>
                        <td>
                            <fmt:message key="tt.saleslead.issues.not.available"/> '${actionBean.TTUser.userID}'
                        </td>
                    </tr>
                </table>
            </c:when>
        </c:choose>
    </stripes:layout-component>   
</stripes:layout-render>


