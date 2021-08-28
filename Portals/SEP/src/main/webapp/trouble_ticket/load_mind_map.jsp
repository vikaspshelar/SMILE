<%@ include file="/include/sep_include.jsp" %>


<c:set var="title">
    Load Customer Support Flow
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <br/>

        <stripes:form action="/TroubleTicket.action">

            <table class="clear" width="99%">
                <tr>
                    <td><b>The file must be a Freemind Mind Map file called</b></td>
                </tr>
                <tr>
                    <td><stripes:file name="mindMapFile"/></td>
                </tr>
                <tr>
                    <td><stripes:submit name="loadMindMap"/></td>
                </tr>
            </table>

        </stripes:form>      
    </stripes:layout-component>
</stripes:layout-render>