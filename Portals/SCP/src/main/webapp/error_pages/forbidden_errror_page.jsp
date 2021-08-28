<%-- 
    Document   : forbidden_errror_page
    Created on : Mar 8, 2012, 9:40:06 AM
    Author     : lesiba
--%>

<%@include file="/include/scp_include.jsp" %>

<c:set var="title">
    This is an error page. enjoy
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="content">
        
        <div style="clear: both;"></div>
        <div id="error-image">
            
        </div>
        <table>
            <tr>
                <td>
                    <img height="399" width="320" src="images/errorman.jpg" alt="Ooops!!!">
                </td>
            </tr>
        </table>
    </stripes:layout-component>
</stripes:layout-render>