<%-- 
    Document   : index
    Created on : Feb 16, 2012, 6:12:04 PM
    Author     : lesiba
--%>

<%@include file="/include/scp_include.jsp" %>
<c:set var="title">
    <fmt:message key="customer.login"/>
</c:set>

<stripes:layout-render name="/layout/loginLayout.jsp">
    <stripes:layout-component name="contents">        
        <div class="nine columns">
            <div style="float: left;" class="login_form columns">
                <p><fmt:message key="scp.login.page.middle.div.msg"/></p>

                <stripes:form  action="/Login.action" id="loginform">
                    <label><fmt:message key="user.name"/></label>
                    <input type="text" name="username" placeholder="<fmt:message key="user.name"/>" size="30" maxlength="50" /><br><br>

                    <label><fmt:message key="password"/></label>
                    <input type="password" name="password" placeholder="<fmt:message key="password"/>" size="10" maxlength="20" /><br><br>
                    <br>
                    <input type="submit" class="login_btn" name="login" value=""/>
                </stripes:form>               
            </div>
            <div style="float: left;" class="four columns">
                <stripes:form name="frm" action="/Login.action">
                    <div class="register_block">
                        <fmt:message key="scp.customer.login.signupmsg"/><br>
                        <input type="submit" class="register_btn" name="showSetNewPassword" value=""> 
                    </div><br>
                    <div class="register_block">
                        <fmt:message key="scp.customer.login.resetpwd"/><br>
                        <input type="submit" class="reset_btn" name="showSendResetLink" value="">
                    </div>
                </stripes:form>
            </div>
        </div>
    </stripes:layout-component> 

    <script type="text/javascript">
        document.getElementById("loginform").username.focus();
    </script>
</stripes:layout-render>
