<%@ include file="/include/sep_include.jsp" %>

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

            <table class="clear">                                
                <tr>
                    <td colspan="2"><b><fmt:message key="basic.data"/></b></td>
                </tr>
                <tr>
                    <td><fmt:message key="rate.plan.description"/>:</td>
                    <td>${ratePlan.description}</td>
                </tr>
                <tr>
                    <td colspan="2"><b><fmt:message key="rate.plan.configuration"/></b></td>
                </tr>
            </table>
            <table class="green">  
                <tr>
                    <th><fmt:message key="attribute"/></th>
                    <th><fmt:message key="value"/></th> 
                </tr>
                <c:forEach items="${ratePlan.AVPs}" var="avp" varStatus="loop">
                    <s:avp-display-row avp="${avp}"/>
                </c:forEach>
            </table>

        </div>

        <br/>
        <input type="button" value="Back" onclick="previousPage();" />
        
    </stripes:layout-component>
</stripes:layout-render>

