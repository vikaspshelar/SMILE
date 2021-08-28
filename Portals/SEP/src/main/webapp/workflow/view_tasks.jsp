<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    Tasks
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <c:forEach items="${actionBean.taskProcessNameSections}" var="sectionName" varStatus="loopSections">
            <c:set var="counter" value="${0}" />
            <table class="green" width="99%">
                <tr><th colspan="6" align="center">Tasks You Own<br/> <font style="color: #000"> ${sectionName}</font></th></tr>
                <tr>
                    <th>Task Id</th>
                    <th>Task Name</th>
                    <th>Description</th>
                    <th>Created</th>
                    <th>Owner</th>
                    <th>Open Task</th>
                </tr>

                <c:forEach items="${s:filterList(s:orderList(actionBean.taskList.tasks, 'getCreatedDate', 'desc'), 'getProcessName', sectionName)}" var="task" varStatus="loop">
                    <c:if test="${fn:toLowerCase(task.assignee) == fn:toLowerCase(actionBean.user)}">
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            <td>${task.taskId}</td>
                            <td>${task.taskName}</td>
                            <td>${task.description}</td>
                            <td>${s:formatDateLong(task.createdDate)}</td>
                            <td>${task.assignee}</td>
                            <td>
                                <stripes:form action="/Workflow.action">
                                    <input type="hidden" name="taskQuery.taskId" value="${task.taskId}"/>
                                    <stripes:submit name="showTask"/>
                                </stripes:form>
                            </td>
                        </tr>
                        <c:set var="counter" value="${counter+1}" />
                    </c:if>
                </c:forEach>
            </table> 
        </c:forEach>

        <br/>

        <c:forEach items="${actionBean.taskProcessNameSections}" var="sectionName" varStatus="loopSections">
            <table class="green" width="99%">
                <c:set var="counter" value="${0}" />
                <tr><th colspan="6" align="center">Tasks You Can Claim<br/> <font style="color: #000">${sectionName}</font></th></tr>
                <tr>
                    <th>Task Id</th>
                    <th>Task Name</th>
                    <th>Description</th>
                    <th>Created</th>
                    <th>Owner</th>
                    <th>Open Task</th>
                </tr>
                <c:forEach items="${s:filterList(s:orderList(actionBean.taskList.tasks, 'getCreatedDate', 'desc'), 'getProcessName', sectionName)}" var="task" varStatus="loop">
                    <c:if test="${task.canCallerClaim && empty task.assignee}">
                        <tr class="${counter mod 2 == 0 ? "even" : "odd"}">
                            <td>${task.taskId}</td>
                            <td>${task.taskName}</td>
                            <td>${task.description}</td>
                            <td>${s:formatDateLong(task.createdDate)}</td>
                            <td>${task.assignee}</td>
                            <td>
                                <stripes:form action="/Workflow.action">
                                    <input type="hidden" name="taskQuery.taskId" value="${task.taskId}"/>
                                    <stripes:submit name="showTask"/>
                                </stripes:form>
                            </td>
                        </tr>
                        <c:set var="counter" value="${counter+1}" />
                    </c:if>
                </c:forEach>
            </table>
        </c:forEach>

        <br/>

        <table class="green" width="99%">
            <tr><th colspan="6" align="center">Tasks Related To You</th></tr>
            <tr>
                <th>Task Id</th>
                <th>Task Name</th>
                <th>Description</th>
                <th>Created</th>
                <th>Owner</th>
                <th>Open Task</th>
            </tr>

            <c:forEach items="${s:orderList(actionBean.taskList.tasks, 'getTaskName', 'desc')}" var="task" varStatus="loop">
                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                    <td>${task.taskId}</td>
                    <td>${task.taskName}</td>
                    <td>${task.description}</td>
                    <td>${s:formatDateLong(task.createdDate)}</td>
                    <td>${task.assignee}</td>
                    <td>
                        <stripes:form action="/Workflow.action">
                            <input type="hidden" name="taskQuery.taskId" value="${task.taskId}"/>
                            <stripes:submit name="showTask"/>
                        </stripes:form>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </stripes:layout-component>   
</stripes:layout-render>

