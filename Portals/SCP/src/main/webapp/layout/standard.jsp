<%@include  file="../include/scp_include.jsp" %>

<stripes:layout-definition>

    <stripes:layout-render name="/layout/baseLayout.jsp" title="${title}">    
        <stripes:layout-component name="contents">

            <div class="sixteen columns alpha">
                <jsp:include page="/layout/errors.jsp"/>
                <jsp:include page="/layout/messages.jsp"/>
                <br/>
                ${contents}
            </div>

        </stripes:layout-component> 
    </stripes:layout-render>        
</stripes:layout-definition>