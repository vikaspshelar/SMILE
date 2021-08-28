<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="reset.password"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <stripes:form  action="/Customer.action">        
            <table class="clear">
                <tr>
                    <td>
                       Current Password:
                    </td>
                    <td>
                        <input value="" type="password" name="currpassword" class="required" size="30" maxlength="50" />
                    </td>
                </tr>
                <tr>
                    <td>
                        <fmt:message key="new.password"/>:
                    </td>
                    <td>
                        <input value="" type="password" name="password1" class="required" size="30" maxlength="50" />
                    </td>
                </tr>
                <tr>
                    <td>
                        <fmt:message key="new.password.repeat"/>:
                    </td>
                    <td>
                        <input value="" type="password" name="password2" class="required" size="30" maxlength="50" />
                    </td>
                </tr>
                <tr>
                    <td colspan="2" align="right">
                        <span class="button">
                            <stripes:submit name="changePassword"  />
                        </span>  
                    </td>
                </tr>
            </table>
        </stripes:form>

    </stripes:layout-component>

</stripes:layout-render>

