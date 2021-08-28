<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="scp.entity.access.notauthorised"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">    
        <div style="margin-top: 10px; border:1px solid black;" class="confirm_form sixteen columns">
            <br/><br/>
            <table width="100%">
                <tr><td>
                        <fmt:message key="scp.entity.access.notauthorised.message"/>
                    </td>
                </tr>
            </table>
            <br/><br/>
        </div>
    </stripes:layout-component>
</stripes:layout-render>


