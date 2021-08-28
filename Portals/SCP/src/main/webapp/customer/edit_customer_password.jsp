<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="edit.customer.password"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}"> 
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function() {
                makeMenuActive('Profile_ChangePasswordPage');
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <div style="margin-top: 10px;">
            <jsp:include page="/layout/my_details_left_banner.jsp"/>


            <div class="change_password">
                <strong> <fmt:message key="ssoidentity"/>: ${actionBean.customer.SSOIdentity}</strong><br><br>
                <stripes:form action="/Customer.action" focus="">
                    <stripes:hidden name="customer.version" value="${actionBean.customer.version}"/>
                    <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/> 
                    <stripes:hidden name="authenticationQuery.SSOIdentity" value="${actionBean.customer.SSOIdentity}"/>

                    <fmt:message key="customer.SSODigest.new"/>:<br>
                    <stripes:password class="frm_input" name="newPassword" maxlength="20" /><br>
                    <fmt:message key="customer.SSODigest.confirm"/>:<br>
                    <stripes:password class="frm_input" name="confirmPassword" maxlength="20" /><br><br>

                    <input type="submit" class="password_btn" name="updateCustomerPassword" value=""/>
                </stripes:form>
            </div>
        </div>
    </stripes:layout-component>    
</stripes:layout-render>

