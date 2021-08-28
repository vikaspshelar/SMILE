<%-- 
    Document   : baseLayout
    Created on : 26 May 2012, 9:31:57 AM
    Author     : sabza
--%>
<%@include  file="../include/scp_include.jsp" %>

<stripes:layout-definition>

    <stripes:layout-render name="/layout/baseLayout.jsp"> 
        <stripes:layout-component name="head">
            <style>
                body{
                    background-image:url(${pageContext.request.contextPath}/images/bg.jpg);
                    background-repeat:no-repeat;
                    background-position: center;
                }
            </style>
        </stripes:layout-component>
        <stripes:layout-component name="contents">
            <div style="margin-top: 150px;" class="sixteen columns alpha">
                <jsp:include page="/layout/errors.jsp"/>
                <jsp:include page="/layout/messages.jsp"/>
                <br/>
                ${contents}
            </div>

        </stripes:layout-component> 
    </stripes:layout-render>
</stripes:layout-definition>