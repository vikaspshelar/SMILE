<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="view.process.detail"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">    
    <stripes:layout-component name="contents">

        <div id="entity"> 
            <table class="entity_header">    
                <tr>
                    <td>Process Instance ${actionBean.processInstance.processInstanceId} : ${actionBean.processInstance.name}</td>                        
                </tr>    
            </table>
            <table class="clear">                    
                <tr>
                    <td colspan="2"><b><fmt:message key="general"/></b></td>                    
                </tr> 
                <tr>
                    <td>Process Instance Id:</td>
                    <td>${actionBean.processInstance.processInstanceId}</td>                    
                </tr>
                <tr>
                    <td>Process Name:</td>
                    <td>${actionBean.processInstance.name}</td>                    
                </tr>
                <tr>
                    <td>Start Date:</td>
                    <td>${s:formatDateLong(actionBean.processInstance.startDate)}</td>                    
                </tr>
                <tr>
                    <td>End Date:</td>
                    <td>${s:formatDateLong(actionBean.processInstance.endDate)}</td>                    
                </tr>
                <tr>
                    <td colspan="2"><br/></td>                    
                </tr>
                <tr>
                    <td colspan="2"><b>Tasks</b></td>                    
                </tr>
                <c:forEach items="${actionBean.processInstance.tasks}" var="task" varStatus="loop">
                    <tr>
                        <td colspan="2"><b>${task.taskName}</b></td>                    
                    </tr>
                    <tr>
                        <td>Task Description:</td>
                        <td>${task.description}</td>                    
                    </tr>
                    <tr>
                        <td>Task Owner:</td>
                        <td>${task.assignee}</td>                    
                    </tr>
                    <tr>
                        <td>Created Date:</td>
                        <td>${s:formatDateLong(task.createdDate)}</td>
                    </tr>
                    <tr>
                        <td>Completed Date:</td>
                        <td>${s:formatDateLong(task.endDate)}</td>
                    </tr>
                    <c:if test="${loop.last && s:getListSize(task.processFields) > 0}">
                        <tr>
                            <td colspan="2"><br/></td>                    
                        </tr>
                        <tr>
                            <td colspan="2"><b>Process Variables</b></td>                    
                        </tr>
                        <c:forEach items="${task.processFields}" var="var">
                            <tr>
                                <td>${var.name}:</td>
                                <td>${var.value}</td>                    
                            </tr>
                        </c:forEach>
                    </c:if>
                </c:forEach>
            </table>
        </div>

    </stripes:layout-component>

</stripes:layout-render>

