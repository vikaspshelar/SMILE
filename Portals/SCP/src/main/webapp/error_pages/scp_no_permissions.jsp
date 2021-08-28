<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="insufficient.permissions"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
         <div style="margin-top: 10px; border:1px solid black;" class="confirm_form sixteen columns">
            <br/><br/>

            <table width="100%">
                <tr>
                    <td>
                        <fmt:message key="scp.no.permissions.message">
                            <fmt:param>${param.resource}</fmt:param>
                        </fmt:message>
                    </td>
                </tr>
                <tr>
                    <td>
                        <fmt:message key="logout.url.description">
                            <fmt:param>
                                <stripes:link href="/Login.action" event="logout"><fmt:message key="here"/></stripes:link>
                            </fmt:param>
                        </fmt:message>
                    </td>
                </tr>
            </table>
            <br/><br/>
        </div>
    </stripes:layout-component>
</stripes:layout-render>


