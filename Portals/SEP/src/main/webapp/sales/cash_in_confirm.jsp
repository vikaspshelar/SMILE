<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="cash.in"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">


        <stripes:form action="/Sales.action" focus="" autocomplete="off">    
            <input type="hidden" name="confirmed" value="true"/>
            <table class="clear">      
                <c:if test="${s:convertCentsToLongRoundHalfEven(actionBean.cashInData.cashRequiredInCents) != s:convertCentsToLongRoundHalfEven(actionBean.cashInData.cashReceiptedInCents)}">
                    <tr>
                        <td colspan="2"  style="text-align: center; background-color: red; font-weight: bold; font-size: 13pt">WARNING: CASH RECEIPTED DOES NOT EQUAL CASH REQUIRED!</td>
                    </tr>
                </c:if>
                <tr>                    
                    <td colspan="2">
                        <b>
                            <fmt:message key="sep.confirm.processcashin">
                                <fmt:param>${s:convertCentsToCurrencyLong(actionBean.cashInData.cashReceiptedInCents)}</fmt:param>
                                <fmt:param>${s:convertCentsToCurrencyLong(actionBean.cashInData.cashRequiredInCents - actionBean.cashInData.cashReceiptedInCents)}</fmt:param>
                            </fmt:message>
                        </b>
                    </td>
                </tr>
                <tr>
                    <td>
                        <fmt:message key="password"/>:
                    </td>
                    <td>
                        <input value="" type="password" name="password" class="required" size="10" maxlength="20"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                        <span class="button">
                            <stripes:submit name="processCashIn"/>
                        </span>                        
                    </td>
                </tr>  
            </table>
            <stripes:wizard-fields/>
        </stripes:form>

    </stripes:layout-component>

</stripes:layout-render>

