<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    Schedule Transaction History
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="account"/>:
                        ${actionBean.account.accountId}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/Account.action">                                
                            <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/>       
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveAccount"><fmt:message key="manage.account"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>

            <stripes:form action="/Account.action" focus="" id="form_edit">
                <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/>
                <table class="clear">
                    
                    <tr>
                        <td>ReportType</td>
                        <td>
                            <stripes:select name="frequency">
                                <stripes:option value="-1" selected="selected">- Please Select -</stripes:option>
                                <stripes:option value="Daily" >Daily</stripes:option>
                                <stripes:option value="Weekly" >Weekly</stripes:option>
                                <stripes:option value="Monthly" >Monthly</stripes:option>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <tr>
                            <td>RecipientEmail</td>
                            <td>
                                <c:set var="defaultRule" value="validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/,'emptynotok')"/>                        
                                <stripes:text name="emailAddress" maxlength="200" size="50" onkeyup="${s:getValidationRule('email.address',defaultRule)}" />
                            </td>
                        </tr>
                    </tr>
                    
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="scheduleTransactionHistorySend"/>
                            </span>                        
                        </td>
                    </tr>  
                </table>            
            </stripes:form>
        </div>		

    </stripes:layout-component>
</stripes:layout-render>

