<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    Processes
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">


        <table class="green">
            <tr>
                <th>Process Instance Id</th>
                <th>Process Name</th>
                <th>Start Date</th>
                <th>End Date</th>
                <th>View Tasks</th>
            </tr>

            <c:forEach items="${actionBean.processHistory.processInstances}" var="process" varStatus="loop">
                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                    <td>${process.processInstanceId}</td>
                    <td>${process.name}</td>
                    <td>${s:formatDateLong(process.startDate)}</td>
                    <td>${s:formatDateLong(process.endDate)}</td>
                    <td>
                        <stripes:form action="/Workflow.action">
                            <stripes:hidden name="processInstanceId" value="${process.processInstanceId}"/>
                            <stripes:submit name="showProcessHistoryDetail"/>
                        </stripes:form>
                    </td>
                </tr>
            </c:forEach>
        </table>  



    </stripes:layout-component>   
</stripes:layout-render>

