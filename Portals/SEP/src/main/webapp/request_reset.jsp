<%@page import="com.smilecoms.commons.base.BaseUtils"%>
<%@ include file="/include/sep_include.jsp" %>


<c:set var="title">
    <fmt:message key="request.password.reset"/>
</c:set>


<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">


        <stripes:form  action="/Login.action">        
            <table class="clear">
                <tr>
                    <td>
                        <fmt:message key="user.name.or.email.address"/>:
                    </td>
                    <td>
                        <input value="" type="text" name="SSOPasswordResetLinkData.identifier" class="required" size="33" maxlength="100" />
                    </td>
                </tr>
                <tr>
                    <td colspan="2" align="right">
                        <span class="button">
                            <stripes:submit name="sendResetLink"  />
                        </span>  
                    </td>
                </tr>
            </table>
        </stripes:form>
    </stripes:layout-component>

</stripes:layout-render>

