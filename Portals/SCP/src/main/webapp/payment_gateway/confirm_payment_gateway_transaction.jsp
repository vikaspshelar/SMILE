<%-- 
    Document   : confirm_payment_gateway_transaction
    Created on : May 8, 2015, 11:09:11 AM
    Author     : sabza
--%>
<%@ include file="../include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="scp.payment.gateway.redirect"/>
</c:set>


<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function () {
                makeMenuActive('Buy Smile Bundle');
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">

        <div style="margin-top: 10px;" class="sixteen columns alpha">

            <div class="sixteen columns alpha">

                <table style="margin: auto;">
                    <tr>                    
                        <td colspan="3">
                            <fmt:message key="scp.payment.gateway.redirect.confirm.${fn:toLowerCase(actionBean.PGWTransactionData.paymentGatewaySale.paymentGatewayCode)}"/>                         
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
                        <td align="right" valign="top">
                            <div>
                                <input class="button_gateway_go_back" type="button" onclick="previousPreviousPage();" value="Cancel"/>
                            </div>
                        </td>
                        <td>
                            &nbsp;&nbsp;
                        </td>
                        <td align="left">
                            <c:choose>
                                <c:when test="${actionBean.PGWTransactionData.isPaymentPageIFrame()}">
                                    <stripes:form id="upay_form" name="upay_form" action="/Account.action">
                                        <stripes:hidden name="gatewayPostURL" value="${actionBean.PGWTransactionData.paymentGatewayPostURL}"/>
                                        <input class="general_gateway_btn" type="submit" name="invokeIframe" value="Ok"/>
                                    </stripes:form >
                                </c:when>
                                <c:when test="${actionBean.PGWTransactionData.isAutoRedirect()}">
                                    <stripes:form name="upay_form" action="/Account.action">
                                        <stripes:hidden name="gatewayPostURL" value="${actionBean.PGWTransactionData.paymentGatewayPostURL}"/>
                                        <input class="general_gateway_btn" type="submit" name="invokeAutoRedirect" value="Ok"/>
                                    </stripes:form >
                                </c:when>
                                <c:otherwise>
                                    <form method="POST" id="upay_form" name="upay_form" action="${actionBean.PGWTransactionData.paymentGatewayPostURL}" target="_top">
                                        <c:set var="urlParams" value="${fn:split(actionBean.PGWTransactionData.gatewayURLData, ',')}"/>
                                        <c:forEach items="${urlParams}" var="avps" varStatus="loopP">
                                            <c:set var="paramAvp" value="${fn:split(avps,'=')}"/>
                                            <c:set var="paramName" value="${paramAvp[0]}"/>
                                            <c:set var="paramValue" value="${fn:replace(paramAvp[1], '#', '=')}"/>

                                            <c:choose>
                                                <c:when test="${paramValue == 'EMPTY'}">
                                                    <input type="hidden" name="${paramName}" value="">
                                                </c:when>
                                                <c:otherwise>
                                                    <input type="hidden" name="${paramName}" value="${paramValue}">
                                                </c:otherwise>
                                            </c:choose>
                                            
                                        </c:forEach>
                                        <input class="general_gateway_btn" type="submit" name="submit" value="Ok"/>
                                    </form>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>                
                </table>
            </div>
        </div>
    </stripes:layout-component>

</stripes:layout-render>