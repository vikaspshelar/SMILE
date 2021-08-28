<%@include file="/include/scp_include.jsp" %>
<c:set var="title">
    eTranzact Confirmation
</c:set>
<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function() {
                 makeMenuActive('Recharge');
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <div style="margin-top: 80px; margin-left: 200px;" class="confirm_form twelve columns">
            <c:set var="accAsString">${actionBean.ETranzactData.recipientAccountId}</c:set>
            <fmt:message key="scp.etranzact.confirm.message">
                <fmt:param value="${accAsString}"/>
                <fmt:param value="${s:convertCentsToCurrencyLong(actionBean.ETranzactData.amountInCents)}"/>
            </fmt:message>

            <form method = "POST" action="${actionBean.ETranzactData.ETranzactPostURL}">
                <input type=hidden name = "TERMINAL_ID" value="${actionBean.ETranzactData.terminalId}">
                <input type=hidden name = "RESPONSE_URL" value="${actionBean.ETranzactData.responseURL}">
                <input type=hidden name = "TRANSACTION_ID" value="${actionBean.ETranzactData.transactionId}">
                <input type=hidden name = "AMOUNT" value="${actionBean.ETranzactData.amountInMajorCurrencyUnit}">
                <input type=hidden name = "DESCRIPTION" value="${actionBean.ETranzactData.description}">
                <input type=hidden name = "CHECKSUM" value= "${actionBean.ETranzactData.checkSum}">
                <input type=hidden name = "LOGO_URL" value="${actionBean.ETranzactData.logoURL}">
                <input type="submit" class="small_submit" name="submitETranzact" value="ok"/>
            </form>
        </div>

    </stripes:layout-component>
</stripes:layout-render>