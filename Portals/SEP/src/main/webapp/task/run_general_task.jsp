<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="run.general.task"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <c:if test="${!empty actionBean.taskOutput}">
            <table class="green" width="99%">
                <tr>
                    <th>Result</th>
                </tr>
                <tr>
                    <td><pre>${actionBean.taskOutput}</pre></td>
                </tr>
            </table>
            <br/><br/>
        </c:if>

        <table class="green" width="99%">
            <tr>
                <th>Task Name</th>
                <th>Description</th>
                <th>Host</th>
                <th>Script</th>
                <th>Input</th>
                <th>Run</th>
            </tr>
            <c:forEach items="${actionBean.taskConfig}" var="taskConfig" varStatus="loop">

                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                    <stripes:form action="/Property.action" autocomplete="off">
                    <input type="hidden" name="taskName" value="${taskConfig[0]}"/>
                    <td>${taskConfig[0]}</td>
                    <td>${taskConfig[1]}</td>
                    <td>${taskConfig[2]}</td>
                    <td>${s:breakUp(taskConfig[3],20)}</td>
                    <td>
                        <input type="text" name="taskInput" value=""  size="20" maxlength="2000"/><br/>
                    </td>
                    <td>
                        <span class="button">
                            <stripes:submit name="runGeneralTask"/>
                        </span> 
                    </td>
                </stripes:form>
            </tr>

        </c:forEach>
    </table>  
</stripes:layout-component>   
</stripes:layout-render>