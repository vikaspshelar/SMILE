<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="split.unit.credit.instance"/>
</c:set>
<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">        
        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="unit.credit.instance.id"/>:${actionBean.unitCreditInstance.unitCreditInstanceId} 
                    </td>
                    <td align="right">                       
                        <stripes:form action="/Account.action">                                
                            <stripes:hidden name="accountQuery.accountId" value="${actionBean.unitCreditInstance.accountId}"/>
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveAccount"><fmt:message key="manage.account"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>         
            <stripes:form action="/Account.action" autocomplete="off"  onsubmit="return (alertValidationErrors());">    
                <input type="hidden" name="splitUnitCreditData.unitCreditInstanceId" value="${actionBean.unitCreditInstance.unitCreditInstanceId}"/>
                <table class="clear">
                    <tr>
                        <td>Target Account Id:</td>
                        <td>
                            <stripes:text name="splitUnitCreditData.targetAccountId"  maxlength="10" size="10" onkeyup="validate(this,'^[0-9]{10,10}$','emptynotok');"/>
                        </td>
                    </tr>
                    <tr>
                        <td>Target Product Instance Id (0 = auto select):</td>
                        <td><stripes:text name="splitUnitCreditData.targetProductInstanceId" value="0" maxlength="10" size="10" onkeyup="validate(this,'^[0-9]{1,10}$','emptyok');"/></td>
                    </tr>
                    <tr>
                        <td>Units:</td>
                        <td><stripes:text name="splitUnitCreditData.units" class="required" size="15"  maxlength="20" onkeyup="validate(this,'^[0-9]{1,15}$','emptynotok');"/></td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <span class="button"> 
                                <stripes:submit name="splitUnitCreditInstance"/>
                            </span>                        
                        </td>
                    </tr> 
                </table>                
            </stripes:form>
        </div>		
    </stripes:layout-component>
</stripes:layout-render>

