<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="rate.plan.detail"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <c:set var="ratePlan" value="${actionBean.ratePlan}"/>
            <table class="entity_header">
                <tr>
                    <td>${ratePlan.name}</td>                        
                </tr>
            </table>
            <br/>
        <input type="button" value="Back" onclick="previousPage();" />
        </div>        
    </stripes:layout-component>
</stripes:layout-render>

