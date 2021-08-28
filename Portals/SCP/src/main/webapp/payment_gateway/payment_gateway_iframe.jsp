<%-- 
    Document   : payment_gateway_iframe
    Created on : Sep 1, 2015, 6:01:38 PM
    Author     : sabza
--%>
<%@ include file="../include/scp_include.jsp" %>

<c:set var="title">
    PesaPal Payment Page
</c:set>

<stripes:layout-render name="/layout/noCreditLayout.jsp" title="${title}">
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function () {
                makeMenuActive('Buy Smile Bundle');
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">

        <div style="margin-top: 10px;" class="sixteen columns alpha">
            <div class="sixteen columns alpha">
                <iframe src="<%out.print(session.getAttribute("gatewayPostURL"));%>" width="100%" height="850px">
                Browser does not support IFrame
                </iframe>
            </div>
        </div>
    </stripes:layout-component>

</stripes:layout-render>
        
        
