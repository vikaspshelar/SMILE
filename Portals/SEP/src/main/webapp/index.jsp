<%@ include file="/include/sep_include.jsp" %>


<c:set var="title">
    <fmt:message key="home"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <br/>
        <table class="clear">
            <tr>
                <td>
                    <b><fmt:message key="welcome"/></b>
                </td>
            </tr>
            <tr>
                <td>
                    <br/>
                </td>
            </tr>
            <tr>
                <td style="background-color: #CAD986; text-align: center">
                    <b>Did You Know?</b><br/>
                    <fmt:message key="sep.did.you.know.message"/>
                </td>
            </tr>
            <tr>
                <td>
                    <br/>
                </td>
            </tr>
            <tr>
                <td style="background-color: red; font-weight: bold; text-align: center">
                    <fmt:message key="sep.index.message"/>
                </td>
            </tr>
            <tr>
                <td>
                    <img width="100%" src="images/smile_logo.jpg">  
                </td>
            </tr>
        </table>
        
    </stripes:layout-component>
</stripes:layout-render>