<%@ include file="/include/scp_include.jsp" %>
<c:set var="title">
    <fmt:message key="confirmation"/>
</c:set>
<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">   
        <div style="margin-top: 10px;" class="sixteen columns alpha">
            <c:if test="${!actionBean.areValidationErrors}">
                <%--<script type="text/javascript">
                        document.body.style.background = '#fff';
                    </script>--%>


                <table style="margin: auto;">
                    <tr>                    
                        <td colspan="3">
                            <fmt:message key="scp.${actionBean.confirmationMessageKey}">
                                <c:if test="${s:getListSize(actionBean.confirmationMessageParams) > 0}">
                                    <c:forEach items="${actionBean.confirmationMessageParams}" var="params">
                                        <fmt:param>${params}</fmt:param>
                                    </c:forEach>
                                </c:if>                               
                            </fmt:message>                            
                        </td>
                    </tr>
                     <tr>
                        <td colspan="3">
                            <br class="clear" />
                        </td>
                    </tr>
                    <tr>
                        <td colspan="3">
                            <br class="clear" />
                        </td>
                    </tr>
                    <tr>            
                        <td align="right">
                            <input class="button_gateway_go_back" type="button" value='<fmt:message key="scp.noresponse.${actionBean.postConfirmationSubmit}"/>' onclick="previousPreviousPage();" />
                        </td>
                        <td>
                            &nbsp;&nbsp;
                        </td>
                        <td align="left">
                            <stripes:form action="${actionBean.postConfirmationAction}">
                                <input class="general_gateway_btn" type="submit" name="${actionBean.postConfirmationSubmit}" value='<fmt:message key="scp.yesresponse.${actionBean.postConfirmationSubmit}"/>' />
                                <input type="hidden" name="confirmed" value="true"/>
                                <stripes:wizard-fields/>
                            </stripes:form>
                        </td>
                    </tr>                
                </table>
            </c:if>
            <c:if test="${actionBean.areValidationErrors}">
                <div>
                    <input class="button_gateway_go_back" type="button" value='<fmt:message key="back"/>' onclick="previousPreviousPage();" />
                </div>
            </c:if>
        </div>
    </stripes:layout-component>
</stripes:layout-render>

