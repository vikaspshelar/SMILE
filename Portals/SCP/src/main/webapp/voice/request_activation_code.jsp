<%@page import="com.smilecoms.commons.base.BaseUtils"%>
<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="request.activation.code"/>
</c:set>

<stripes:layout-render name="/layout/loginLayout.jsp">
    <stripes:layout-component name="contents">
        <c:if test="${actionBean.activationDataSent}">
                An email has been sent to you with instructions on how to activate your Smile Voice App
                <br/>
        </c:if>
                
        <div class="login_form eight columns">
            <stripes:form  action="/Login.action">        
                <table width="99%">
                    <tr>
                        <td>
                            <fmt:message key="scp.activation.identifier"/>:
                        </td>
                        <td>
                            <input value="" type="text" name="identifier" size="33" maxlength="100" />
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" align="right">
                            <input type="submit" class="send_email_reset_btn" name="sendActivationCode" value="">
                        </td>
                    </tr>
                </table>
            </stripes:form>
        </div>
    </stripes:layout-component>

</stripes:layout-render>

