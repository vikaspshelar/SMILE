<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    Processes
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">


        <table class="green">
            <tr>
                <th>Name</th>
                <th>Delete Process</th>
                <th>View History</th>
                <th>Start Process</th>
            </tr>

            <c:forEach items="${actionBean.processDefinitionList.processDefinitions}" var="process" varStatus="loop">
                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                    <td>${process.name}</td>
                    <td>
                        <stripes:form action="/Workflow.action">
                            <stripes:hidden name="processKey" value="${process.key}"/>
                            <stripes:submit name="deleteProcessDefinition"/>
                        </stripes:form>
                    </td>
                    <td>
                        <stripes:form action="/Workflow.action">
                            <stripes:hidden name="processKey" value="${process.key}"/>
                            <stripes:submit name="showProcessHistoryList"/>
                        </stripes:form>
                    </td>
                    <td>
                        <stripes:form action="/Workflow.action">
                            <stripes:hidden name="processKey" value="${process.key}"/>
                            <stripes:submit name="startProcess"/>
                        </stripes:form>
                    </td>
                </tr>
            </c:forEach>
        </table>  



    </stripes:layout-component>   
</stripes:layout-render>

