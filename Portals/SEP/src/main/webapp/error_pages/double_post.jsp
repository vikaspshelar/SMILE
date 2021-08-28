<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="double.post"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">           
        <br/><br/>
        
        <table class="clear">
            <tr><td>
                    <fmt:message key="double.post.description"/>
            </td></tr>
        </table>
        
        
        <br/><br/>
        
    </stripes:layout-component>
    </stripes:layout-render>
    
    
    