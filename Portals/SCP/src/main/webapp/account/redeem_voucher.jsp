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
                    <stripes:hidden name="prepaidStripRedemptionData.accountId" value="${actionBean.account.accountId}"/> 
                    <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/> 

                    <tr>
                        <td><fmt:message key="prepaidStripRedemptionData.encryptedPIN"/>:</td>
                        <td><stripes:text name="prepaidStripRedemptionData.encryptedPIN" class="required" size="19" maxlength="19"/></td>                                                    
                    </tr>
                    <tr>
                        <td style="padding: 2px; float:right" width="5px">
                            <stripes:submit class="general_btn" name="redeemPrepaidStrip" value="Submit" style="padding-left:40px;"/>
                        </td>
                    </tr>

                </stripes:form>
                <c:if test="${s:getProperty('env.etranzact.config') != ''}">
                    <tr>
                        <td colspan="3">or<br/></td>
                    </tr>                   

                    <stripes:form action="/ETranzact.action" focus="" autocomplete="off">     
                        <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/>
                        <tr>
                            <td>Recharge with eTranzact ${s:getProperty('env.locale.currency.majorunit')}:&nbsp;</td>
                            <td><stripes:text name="eTranzactMajorCurrencyUnits" class="required" size="19" maxlength="19"/></td>
                            <td  style="padding: 2px;" width="5px"><stripes:submit class="general_btn" name="confirmEtranzact"/></td>                           
                        </tr>
                    </stripes:form>
                </c:if>
            </table>
        </div>
    </stripes:layout-component>
</stripes:layout-render>


