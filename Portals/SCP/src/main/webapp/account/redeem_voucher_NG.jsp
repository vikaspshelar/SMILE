<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="recharge.account"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}"> 
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function() {
                makeMenuActive('Recharge_VoucherPage');
            }

        </script>

    </stripes:layout-component>
    <stripes:layout-component name="contents">     
        <div style="margin-top: 10px;" class="sixteen columns">
            <table>
                <stripes:form action="/Account.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">       
                    <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/>                     
                    <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/> 
                    <tr>
                        <td align="center" colspan="2"><a style="text-decoration: none" href="http://www.quickteller.com/smile" target="_blank"><strong>Recharge with Quickteller</strong></a></td>
                    </tr>                    
                    <tr style="margin-top: 10px">
                        <td>
                            <a href="http://www.quickteller.com/smile" target="_blank"><img src="${pageContext.request.contextPath}/images/quickteller_logo.png" width="300" height="96"></a>
                        </td>
                    </tr>
                </stripes:form>
            </table>
        </div>
    </stripes:layout-component>
</stripes:layout-render>