<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="cash.in"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">


        <table class="clear">      
            <tr>                    
                <td>
                    <b>
                        <fmt:message key="sep.summary.processcashin.${actionBean.cashInData.cashInType}">
                            <fmt:param>${s:convertCentsToCurrencyLong(actionBean.cashInData.cashReceiptedInCents)}</fmt:param>
                            <fmt:param>${s:convertCentsToCurrencyLong(actionBean.cashInData.cashRequiredInCents - actionBean.cashInData.cashReceiptedInCents)}</fmt:param>
                            <fmt:param>${actionBean.cashInData.cashInId}</fmt:param>
                        </fmt:message>
                    </b>
                </td>
            </tr>
            <tr>
                <td>
                    <br/>
                </td>
            </tr>
            <tr>
                <td>
                    <div class="emphasise">Reference Number: ci${actionBean.cashInData.cashInId}</div>
                </td>
            </tr>
        </table>

    </stripes:layout-component>

</stripes:layout-render>

