<%-- 
    Document   : FAQ
    Created on : Jan 23, 2012, 4:27:42 PM
    Author     : lesiba
--%>

<%@include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="faq"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp"  title="${title}">   
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function() {
                makeMenuActive('Help_FAQPage');
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <div style="text-align: justify; padding-top: 5px;">            
            <c:forEach items="${actionBean.faqList}" var="Faq" varStatus="loop">
                ${Faq}
            </c:forEach>
        </div>
    </stripes:layout-component>
</stripes:layout-render>