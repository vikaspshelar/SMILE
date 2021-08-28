<%@page import="com.smilecoms.commons.base.BaseUtils"%>
<%@ include file="/include/scp_include.jsp" %>


<c:set var="title">
    <fmt:message key="request.password.reset"/>
</c:set>

<stripes:layout-render name="/layout/loginLayout.jsp">
    <stripes:layout-component name="contents">

        <div class="login_form eleven columns">
            <stripes:form  action="/Login.action">        
                <table width="99%">
                    <tr>
                        <td>
                            <fmt:message key="user.name.or.email.address"/>:
                        </td>
                        <td>
                            <input value="" type="text" name="SSOPasswordResetLinkData.identifier" size="33" maxlength="100" />
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" align="right">
                            <input type="submit" class="send_email_reset_btn" name="sendResetLink" value="">
                        </td>
                    </tr>
                </table>
            </stripes:form>
        </div>
    </stripes:layout-component>

</stripes:layout-render>

