<%-- 
    Document   : index
    Created on : Feb 16, 2012, 6:12:04 PM
    Author     : lesiba
--%>

<%@page import="com.smilecoms.commons.stripes.SmileActionBean"%>
<%@include file="/include/scp_include.jsp" %>

<c:if test="${pageContext.request.remoteUser != null}">
    <%
        // Log user out if logged in
        SmileActionBean.invalidateSession(request);
    %>
</c:if>

<c:set var="title">
    <fmt:message key="customer.login"/>
</c:set>

<stripes:layout-render name="/layout/loginLayout.jsp">
    <stripes:layout-component name="contents">
        <div class="sixteen columns alpha">

            <div style="float: left; line-height:35px; color:#ffffff;"class="five columns">
                <font class="light" style="font-size:30px;"><fmt:message key="scp.login.page.left.div.msg"/></font>
            </div>

            <div style="float: left; line-height:35px;" class="nine columns">
                <div style="float: left;"class="login_form columns">
                    <%--<font class="light" style="font-size:25px;"><fmt:message key="my.smile.heading"/></font><br><br>
                    <fmt:message key="scp.login.page.middle.div.msg"/><br>--%>
                    <font class="light" style="font-size:25px;"><fmt:message key="scp.login.page.middle.div.msg"/></font><br><br>

                    <stripes:form  action="/Login.action" id="loginform">
                       <input type="hidden" name="nextAction" value="${actionBean.nextAction}"/>
                        <label><fmt:message key="user.name.or.email.address"/></label>
                        <input type="text" name="username" placeholder="<fmt:message key="user.name.or.email.address"/>" size="30" maxlength="50" /><br><br>

                        <label><fmt:message key="password"/></label>
                        <input type="password" name="password" placeholder="<fmt:message key="password"/>" size="10" maxlength="20" /><br><br>
                        <br>
                        <input type="submit" class="login_btn" name="login" value=""/>
                    </stripes:form>
                </div>
                <div style="float: left;" class="four columns">
                    <stripes:form name="frm" action="/Login.action">
                        <input type="hidden" name="nextAction" value="${actionBean.nextAction}"/>
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
        </div>
    </stripes:layout-component> 
    <script type="text/javascript">
        document.getElementById("loginform").username.focus();
    </script>
    <script type="text/javascript">
        $(window).load(function () {
            $('#slider').nivoSlider({
                effect: 'fold',
                boxCols: 10, // For box animations
                boxRows: 10, // For box animations
                animSpeed: 900,
                pauseTime: 8000,
                controlNav: true, // 1,2,3... navigation
                controlNavThumbs: true, // Use thumbnails for Control Nav
                controlNavThumbsFromRel: true, // Use image rel for thumbs
                controlNavThumbsSearch: '.png', // Replace this with...
                controlNavThumbsReplace: '_thumb.png', // ...this in thumb Image src
                directionNavHide: false,
                prevText: '', // Prev directionNav text
                nextText: '' // Next directionNav text

            });
        });
    </script>
</stripes:layout-render>
