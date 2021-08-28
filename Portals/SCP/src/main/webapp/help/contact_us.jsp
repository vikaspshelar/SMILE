<%-- 
    Document   : contact_us
    Created on : 24-Mar-2012, 16:37:28
    Author     : lesiba
--%>

<%@include  file="/include/scp_include.jsp" %>
<c:set var="title">
    <fmt:message key="scp.contact.us"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp"  title="${title}">
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function() {
                makeMenuActive('Help_ContactUsPage');
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">
        
        <c:set var="contactSalesInfo" value="${actionBean.contactSalesInfo}"/>
        <c:set var="contactWalkInfo" value="${actionBean.contactWalkInfo}"/>
        <c:set var="contactCustCare" value="${actionBean.contactCustCare}"/>

        <div class="help_section">
            <c:if test="${!empty contactWalkInfo}">
                <div class="walk_in">
                    <fmt:message key="scp.contactus.walkin"/>
                </div>
            </c:if>

            <c:if test="${!empty contactCustCare}">
                <div class="customer_care">
                    <fmt:message key="scp.contactus.custcare"/>
                </div>
            </c:if>

            <c:if test="${!empty contactSalesInfo}">
                <div class="sales" > 
                    <fmt:message key="scp.contactus.sales"/>
                </div>
            </c:if>
        </div>

    </stripes:layout-component>

</stripes:layout-render>