<%-- 
    Document   : confirm_tpgw_partner_transaction
    Created on : May 27, 2015, 6:36:16 PM
    Author     : sabza
--%>
<%@ include file="../include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="scp.payment.gateway.redirect"/>
</c:set>


<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">

    </stripes:layout-component>
    <stripes:layout-component name="contents">

        <c:set var="isAirtimeTransaction" value="false"/>
        <c:if test="${!empty actionBean.paymentGatewayTransactionData.balanceTransferLines && s:getListSize(actionBean.paymentGatewayTransactionData.balanceTransferLines) > 0}">
            <c:set var="isAirtimeTransaction" value="true"/>
        </c:if>


        <div style="margin-top: 10px;" class="sixteen columns alpha">

            <div class="sixteen columns alpha">

                <table style="margin: auto;">
                    <tr>                    
                        <td colspan="2">
                            <fmt:message key="scp.payment.gateway.redirect.confirm.${actionBean.TPGWPartnerCode}"/>                         
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <br class="clear" />
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <br class="clear" />
                        </td>
                    </tr>
                    <tr>            
                        <td align="right" valign="top">
                            <div>
                                <input class="button_gateway_go_back" type="button" onclick="previousPreviousPage();" value="<fmt:message key="back"/>"/>
                            </div>
                        </td>
                        <td>
                            &nbsp;&nbsp;
                        </td>
                        <td align="left">

                            <c:forEach items="${s:getPropertyAsList('env.scp.payment.methods')}" var="paymentTypeProps" varStatus="loopP">
                                <c:set var="ptProps" value="${fn:split(paymentTypeProps,'|')}"/>
                                <c:set var="paymentTypeConfigs" value="${ptProps[2]}"/>
                                <c:set var="paymentType" value="${ptProps[0]}"/>
                                <c:if test="${actionBean.TPGWPartnerCode eq paymentType}">
                                    <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="type">
                                        <c:set var="typeProps" value="${fn:split(type,'|')}"/>
                                        <c:set var="partnerCode" value="${ptProps[0]}"/>
                                        <c:if test="${partnerCode eq paymentType}">
                                            <c:set var="actionURL" value="${typeProps[2]}"/>
                                            <c:set var="refererURL" value="${typeProps[3]}"/>
                                        </c:if>
                                    </c:forEach>
                                </c:if>
                            </c:forEach>

                            <form method="GET" name="payall_form" action="${actionURL}" target="_top">
                                <input type="hidden" name="acc" value="${actionBean.account.accountId}">
                                <c:if test="${isAirtimeTransaction != 'true'}">
                                    <input type="hidden" name="ucspec" value="${actionBean.unitCreditSpecification.unitCreditSpecificationId}"/>
                                    <input type="hidden" name="amnt" value="${s:convertCentsToLong(actionBean.unitCreditSpecification.priceInCents)}"/>
                                </c:if>
                                <c:if test="${isAirtimeTransaction == 'true'}">
                                    <input type="hidden" name="ucspec" value="0">
                                    <input type="hidden" name="amnt" value="${s:convertCentsToLong(actionBean.paymentGatewayTransactionData.transactionAmountCents)}"/>
                                </c:if>
                                <input type="hidden" name="callback" value="${refererURL}">
                                <input class="general_gateway_btn" type="submit" name="submit" value="Ok"/>
                            </form>
                        </td>
                    </tr>                
                </table>

            </div>
        </div>
    </stripes:layout-component>

</stripes:layout-render>