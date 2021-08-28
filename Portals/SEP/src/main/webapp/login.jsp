<%@page import="com.smilecoms.commons.stripes.SmileActionBean"%>
<%@ include file="/include/sep_include.jsp" %>

<c:if test="${pageContext.request.remoteUser != null}">
    <%
        // Log user out if logged in
        SmileActionBean.invalidateSession(request);
    %>
</c:if>

<c:set var="title">
    <fmt:message key="login"/>
</c:set>


<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <stripes:form  action="/Login.action" id="loginform" autocomplete="off" method="POST">        
            <table class="clear">
                <tr>
                    <td>
                        <fmt:message key="user.name"/>:
                    </td>
                    <td>
                        <input  type="text" name="username" value="${actionBean.username}" class="required" size="30" maxlength="50" />
                    </td>
                </tr>
                <tr>
                    <td>
                        <fmt:message key="password"/>:
                    </td>
                    <td>
                        <input value="" type="password" name="password" class="required" size="30" maxlength="20"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                        <stripes:link event="showSendResetLink" href="/Login.action">
                            <fmt:message key="forgotten.password"/>
                        </stripes:link>
                    </td>
                </tr>
                <tr>
                    <td colspan="2" align="right">
                        <span class="button">
                            <stripes:submit name="login"  />
                        </span>  
                    </td>
                </tr>
            </table>
        </stripes:form>

        <script type="text/javascript">
            document.getElementById("loginform").username.focus();
        </script>

    </stripes:layout-component>

</stripes:layout-render>

