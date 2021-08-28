<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="reset.password"/>
</c:set>

<stripes:layout-render name="/layout/loginLayout.jsp">
    <stripes:layout-component name="contents">
        <div>
            <div class="nine columns">
                <div class="login_form columns">
                    <font class="light" style="font-size:25px;">Set New Password</font><br><br>
                    <fmt:message key="set.new.password"/>
                    <br><br>
                    <stripes:form  action="/Login.action" id="loginform">
                        <stripes:hidden name="SSOPasswordResetData.GUID" value="${actionBean.SSOPasswordResetData.GUID}"/>
                        <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>            
                        <fmt:message key="new.password"/>:
                        <input value="" type="password" name="password1" class="required" size="30" maxlength="50" />
                        <fmt:message key="new.password.repeat"/>:
                        <input value="" type="password" name="password2" class="required" size="30" maxlength="50" />
                        <input type="submit" class="general_btn" name="resetPassword" value="Update"  style="margin-left: 95px;"/>
                    </stripes:form>
                </div>
                <div class="four columns">
                    <div class="register_block">
                        <fmt:message key="set.new.password.info"/><br>
                    </div>
                </div>
            </div>

        </div>
    </stripes:layout-component>

</stripes:layout-render>

