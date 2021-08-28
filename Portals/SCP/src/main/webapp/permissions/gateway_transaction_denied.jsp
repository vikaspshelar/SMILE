<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="scp.payment.gateway.service.denied"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">    
        <div style="margin-top: 10px; border:1px solid black;" class="confirm_form sixteen columns alpha">
            <br/><br/>

            <table width="99%">
                <tr><td>
                        <fmt:message key="scp.payment.gateway.service.notavailable"/>
                    </td></tr>
            </table>
            <br/><br/>

            <div class="help_section">
                <div class="customer_care">
                    <fmt:message key="scp.contactus.custcare"/>
                </div>
                <div class="sales" > 
                    <fmt:message key="scp.contactus.sales"/>
                </div>
            </div>
            <br/><br/>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
