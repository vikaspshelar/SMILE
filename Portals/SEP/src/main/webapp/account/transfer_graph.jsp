<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    Transfer Graph
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="html_head">        
        <STYLE type="text/css">
            .ui-datepicker {font-size:12px;}
        </STYLE>
    </stripes:layout-component>
    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="account"/>:
                        ${actionBean.accountQuery.accountId}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/Account.action">                                
                            <stripes:hidden name="accountQuery.accountId" value="${actionBean.accountQuery.accountId}"/>       
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveAccount"><fmt:message key="manage.account"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>

            <stripes:form action="/Account.action" focus="" autocomplete="off" name="frm">
                <input type="hidden" name="transferGraphQuery.rootAccountId" value="${actionBean.accountQuery.accountId}"/>       
                <table class="clear">
                    <tr>
                        <td><stripes:label for="datefrom"/>:</td>
                        <td>
                            <input readonly="true" type="text" id="dateFrom" value="${s:formatDateShort(actionBean.transferGraphQuery.startDate)}" name="transferGraphQuery.startDate" class="required" size="10"/>
                        </td>
                    </tr>
                    <tr>
                        <td><stripes:label for="dateto"/>:</td>
                        <td>
                            <input id="dateTo" readonly="true" type="text" value="${s:formatDateShort(actionBean.transferGraphQuery.endDate)}" name="transferGraphQuery.endDate" class="required" size="10"/>
                        </td>
                    </tr>

                    <tr>
                        <td>Max Recursions:</td>
                        <td><stripes:text name="transferGraphQuery.recursions" size="2" /></td>
                    </tr>
                    <tr>
                        <td>Debit Type Match:</td>
                        <td><stripes:text name="transferGraphQuery.debitType" size="10"/></td>
                    </tr>	
                    <tr>
                        <td>Transfer Regex Match:</td>
                        <td><stripes:text name="transferGraphQuery.regexMatch" size="20"/></td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="doTransferGraph"/>
                            </span>                        
                        </td>
                    </tr>  
                </table>            
                <stripes:hidden name="accountQuery.accountId" value="${actionBean.accountQuery.accountId}"/>
            </stripes:form>

            <script type="text/javascript">
                var $j = jQuery.noConflict();
                var dateNow = new Date();
                var totalUsagePerPeriod = 0.0;

                $j(document).ready(function() {
                    $j("#dateFrom").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", maxDate: dateNow, changeYear: true, changeMonth: true});
                });
                $j(document).ready(function() {
                    $j("#dateTo").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", maxDate: dateNow, changeYear: true, changeMonth: true});
                });
            </script>

        </div>
    </stripes:layout-component>   
</stripes:layout-render>