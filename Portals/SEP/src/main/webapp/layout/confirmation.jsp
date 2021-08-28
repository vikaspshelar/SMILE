<%@ include file="/include/sep_include.jsp" %>
<c:set var="title">
    <fmt:message key="confirmation"/>
</c:set>
<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <br/>

        <c:if test="${!actionBean.areValidationErrors}">
            <script type="text/javascript">
                document.body.style.background = '#FF0000';
            </script>


            <table class="clear">
                <tr>                    
                    <td colspan="2">
                        <fmt:message key="sep.${actionBean.confirmationMessageKey}">
                            <c:forEach items="${actionBean.confirmationMessageParams}" var="msgparam">
                                <fmt:param>${msgparam}</fmt:param>
                            </c:forEach>
                        </fmt:message>
                    </td>
                </tr>
                <tr>
                    <td>
                        <input type="button" value="<fmt:message key="cancel"/>" onclick="previousPage();" />
                    </td>
                    <td align="left">
                        <stripes:form action="${actionBean.postConfirmationAction}">
                            <input type="hidden" name="confirmed" value="true"/>
                            <stripes:wizard-fields/>
                            <stripes:submit name="${actionBean.postConfirmationSubmit}"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>
        </c:if>
        <c:if test="${actionBean.areValidationErrors}">
            <input type="button" value="<fmt:message key="cancel"/>" onclick="previousPreviousPage();" />
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>

