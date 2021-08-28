<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="run.general.query"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <table class="green" width="99%">
            <tr>
                <th>Query Name</th>
                <th>Database</th>
                <th>Parameters</th>
                <th>Run</th>
            </tr>
            <c:forEach items="${actionBean.queryConfig}" var="queryConfig" varStatus="loop">

                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                    <stripes:form action="/Property.action" autocomplete="off">
                        <stripes:hidden name="generalQueryRequest.queryName" value="${queryConfig[0]}"/>
                        <td>${queryConfig[0]}</td>
                        <td>${queryConfig[1]}</td>
                        <td>
                            <c:forEach begin="1" end="${queryConfig[3]}" varStatus="paramloop">
                                <stripes:text name="generalQueryRequest.parameters[${paramloop.index}]" size="20" maxlength="500"/><br/>
                            </c:forEach>
                        </td>
                        <td>
                            <span class="button">
                                <stripes:submit name="runGeneralQuery"/>
                            </span> 
                        </td>
                    </stripes:form>
                </tr>

            </c:forEach>
        </table>  
    </stripes:layout-component>   
</stripes:layout-render>