<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="view.task"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">    
    <stripes:layout-component name="contents">

        <div id="entity"> 
            <table class="entity_header">    
                <tr>
                    <td>Task ${actionBean.task.taskId} : ${actionBean.task.taskName}</td>                        
                    <td align="right">                       
                        <stripes:form action="/Workflow.action" focus="">      
                            <stripes:hidden name="task.taskId" value="${actionBean.task.taskId}"/>
                            <stripes:hidden name="taskUpdateData.taskId" value="${actionBean.task.taskId}"/>
                            <stripes:select name="entityAction">
                                <c:if test="${empty actionBean.task.assignee && actionBean.task.canCallerClaim}">
                                    <stripes:option value="claimTask">Claim Task</stripes:option>
                                </c:if>
                                <c:if test="${fn:toLowerCase(actionBean.task.assignee) == fn:toLowerCase(actionBean.user)}">
                                    <stripes:option value="completeTask">Complete Task</stripes:option>
                                    <stripes:option value="unclaimTask">Unclaim Task</stripes:option>
                                    <stripes:option value="deleteProcess">Delete Process</stripes:option>                                    
                                </c:if>
                                <stripes:option value="showTasks">Back to Tasks</stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>    
            </table>
            <table class="clear">                    
                <tr>
                    <td colspan="2"><b><fmt:message key="general"/></b></td>                    
                </tr> 
                <tr>
                    <td>Process Name:</td>
                    <td>${actionBean.task.processName}</td>                    
                </tr>
                <tr>
                    <td>Task Id:</td>
                    <td>${actionBean.task.taskId}</td>                    
                </tr>
                <tr>
                    <td>Task Name:</td>
                    <td>${actionBean.task.taskName}</td>                    
                </tr>
                <tr>
                    <td>Task Owner:</td>
                    <td>${actionBean.task.assignee}</td>                    
                </tr>
                <tr>
                    <td>Task Description:</td>
                    <td>${actionBean.task.description}</td>                    
                </tr>
                <tr>
                    <td>Task Creation Date:</td>
                    <td>${s:formatDateLong(actionBean.task.createdDate)}</td>                    
                </tr>
                <tr>
                    <td>Candidate User Groups:</td>
                    <td><c:forEach items="${actionBean.task.candidateGroups}" var="grp" varStatus="loop">${grp} &nbsp;</c:forEach></td>                    
                </tr>
                <tr>
                    <td>Candidate Users:</td>
                    <td><c:forEach items="${actionBean.task.candidateUsers}" var="usr" varStatus="loop">${usr} &nbsp;</c:forEach></td>                    
                </tr>
                <c:if test="${s:getListSize(actionBean.task.processFields) > 0}">
                    <tr>
                        <td colspan="2"><b>Process Information</b></td>                    
                    </tr> 
                    <c:forEach items="${actionBean.task.processFields}" var="field" varStatus="loop">
                        <tr>
                            <td>${field.name}:</td>
                            <td>${field.value}</td>                    
                        </tr>  
                    </c:forEach>
                </c:if>
            </table>
            <c:if test="${fn:toLowerCase(actionBean.task.assignee) == fn:toLowerCase(actionBean.user) && s:getListSize(actionBean.task.taskFields) > 0}">
                <stripes:form action="/Workflow.action"  autocomplete="off">
                    <stripes:hidden name="task.taskId" value="${actionBean.task.taskId}"/>                    
                    <table class="clear"> 
                        <tr>
                            <td colspan="2"><b>Task Form Input</b></td>                 
                        </tr>
                        <c:forEach items="${actionBean.task.taskFields}" var="field" varStatus="loop">
                            <c:if test="${field.type == 'enum'}">
                                <tr>
                                    <td>${field.name}:</td>
                                    <td>
                                        <stripes:select name="${field.name}" value="${field.value}">
                                            <c:forEach items="${field.dropDownRows}" var="dropDownRow" varStatus="loop">
                                                <stripes:option value="${dropDownRow.value}">${dropDownRow.option}</stripes:option>
                                            </c:forEach>
                                        </stripes:select>
                                    </td>                    
                                </tr>
                            </c:if>
                            <c:if test="${field.type != 'enum'}">
                                <tr>
                                    <td>${field.name}:</td>
                                    <td><input type="text" name="${field.name}" value="${field.value}" size="60" maxlength="1000"/></td>                    
                                </tr>  
                            </c:if>
                        </c:forEach>
                    </table>
                    <stripes:submit name="updateTask"/>
                </stripes:form>
            </c:if>

        </div>

    </stripes:layout-component>

</stripes:layout-render>

