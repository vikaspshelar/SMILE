<%@ include file="/include/sep_include.jsp" %>


<c:set var="title">
    <fmt:message key="login"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <stripes:form  action="/Login.action" id="loginform" autocomplete="off">        
            <table class="clear">
                <tr>
                    <td>
                        <fmt:message key="user.name"/>:
                    </td>
                    <td>
                        <input value="" type="text" name="username" class="required" size="30" maxlength="50" />
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
        
    </stripes:layout-component>

</stripes:layout-render>

