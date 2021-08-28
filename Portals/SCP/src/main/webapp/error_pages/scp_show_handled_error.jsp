<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="error"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">         
        
        <div style="margin-top: 10px; border:1px solid black;" class="confirm_form sixteen columns">
            <table>
                <tr>
                    <td>
                        <fmt:message key="scp.handlederror.message">
                            <fmt:param>CODE:<%=request.getAttribute("error_code")%></fmt:param>
                            <fmt:param>${pageContext.request.contextPath}</fmt:param>
                        </fmt:message>
                    </td>
                </tr>
            </table>
            <br/><br/>
        </div>
    </stripes:layout-component>
</stripes:layout-render>


