<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="edit.unit.credit.instance"/>
</c:set>
<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="html_head">        
        <script type="text/javascript">

            var today = new Date().getFullYear();
            $j(document).ready(function () {
                $j('#datePicker1').datepicker({dateFormat: 'yy/mm/dd 00:00:00', 
                    showOn: 'button', buttonText: "..", changeYear: true, changeMonth: true, onSelect: function () {
                        validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01]) ([012][0-9])[:]([0-5][0-9]):([0-5][0-9])$', 'emptynotok');
                    }});
            });

            $j(document).ready(function () {
                $j('#datePicker2').datepicker({dateFormat: 'yy/mm/dd 00:00:00'
                    , showOn: 'button', buttonText: "..", changeYear: true, changeMonth: true, onSelect: function () {
                        validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01]) ([012][0-9])[:]([0-5][0-9]):([0-5][0-9])$', 'emptynotok');
                    }});
            });
            
            $j(document).ready(function () {
                $j('#datePicker3').datepicker({dateFormat: 'yy/mm/dd 00:00:00'
                    , showOn: 'button', buttonText: "..", changeYear: true, changeMonth: true, onSelect: function () {
                        validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01]) ([012][0-9])[:]([0-5][0-9]):([0-5][0-9])$', 'emptynotok');
                    }});
            });

        </script>
    </stripes:layout-component>
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
            <stripes:form action="/Account.action" autocomplete="off" onsubmit="return (alertValidationErrors());">    
                <input type="hidden" name="unitCreditInstance.unitCreditInstanceId" value="${actionBean.unitCreditInstance.unitCreditInstanceId}"/>

                <table class="clear">
                    <tr>
                        <td colspan="4"><b>Main Data</b></td>
                    </tr>
                    <tr>
                        <td colspan="2"><stripes:label for="account.accountid" />:</td>
                        <td colspan="2"><stripes:text name="unitCreditInstance.accountId" class="required" size="10"  maxlength="10" id="accVal"  onkeyup="validate(this,'^[0-9]{10,10}$','emptynotok');"/></td>
                    </tr>
                    <tr>
                        <td colspan="2"><stripes:label for="unit.credit.product.instance.id"/>:</td>
                        <td colspan="2"><stripes:text name="unitCreditInstance.productInstanceId"    maxlength="20" size="20" onkeyup="validate(this,'^.{1,20}$','emptynotok')"/></td>
                    </tr>
                    <tr>
                        <td colspan="2"><stripes:label for="unit.credit.start.date"/>:</td>
                        <td colspan="2"><input type="text" id="datePicker1" name="unitCreditInstance.startDate" maxlength="19" size="19" onkeyup="validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01]) ([012][0-9])[:]([0-5][0-9]):([0-5][0-9])$', 'emptydatenotok')" value='${s:formatDateLong(actionBean.unitCreditInstance.startDate)}'/></td>
                    </tr>
                    <tr>
                        <td colspan="2"><stripes:label for="unit.credit.expiry.date" />:</td>
                        <td colspan="2"><input type="text" id="datePicker2" name="unitCreditInstance.expiryDate" maxlength="19" size="19" onkeyup="validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01]) ([012][0-9])[:]([0-5][0-9]):([0-5][0-9])$', 'emptydatenotok')" value='${s:formatDateLong(actionBean.unitCreditInstance.expiryDate)}'/></td>
                    </tr>
                    <tr>
                        <td colspan="2"><stripes:label for="unit.credit.end.date" />:</td>
                        <td colspan="2"><input type="text" id="datePicker3" name="unitCreditInstance.endDate" maxlength="19" size="19" onkeyup="validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01]) ([012][0-9])[:]([0-5][0-9]):([0-5][0-9])$', 'emptydatenotok')" value='${s:formatDateLong(actionBean.unitCreditInstance.endDate)}'/></td>
                    </tr>
                    <tr>
                        <td colspan="2">Behaviour Hint:</td>
                        <td colspan="2"><stripes:text name="behaviourHint"  size="50"  maxlength="200"/></td>
                    </tr>
                    <tr>
                        <td colspan="2">Reason For Editing:</td>
                        <td colspan="2"><textarea name="unitCreditInstance.comment" class="required" cols="50" rows="5" onkeyup="validate(this,'^.{10,2000}$','emptynotok')"></textarea></td>
                    </tr>
                    <tr>
                        <td colspan="4"><b>Availability - Days Available for use</b></td>
                    </tr>
                    <tr>
                        <td>Monday:<stripes:checkbox name="availabilityBitIndexOn" value="25"/></td>
                        <td>Tuesday:<stripes:checkbox name="availabilityBitIndexOn" value="26"/></td>
                        <td>Wednesday:<stripes:checkbox name="availabilityBitIndexOn" value="27"/></td>
                        <td>Thursday:<stripes:checkbox name="availabilityBitIndexOn" value="28"/></td>
                    </tr>
                    <tr>
                        <td>Friday:<stripes:checkbox name="availabilityBitIndexOn" value="29"/></td>
                        <td>Saturday:<stripes:checkbox name="availabilityBitIndexOn" value="30"/></td>
                        <td>Sunday:<stripes:checkbox name="availabilityBitIndexOn" value="24"/></td>
                        <td>Public Holidays:<stripes:checkbox name="availabilityBitIndexOn" value="31"/></td>
                    </tr>
                    <tr>
                        <td colspan="4"><b>Availability - Hour of Day Available for use</b></td>
                    </tr>
                    <tr>
                        <td>00:00 to 01:00:<stripes:checkbox name="availabilityBitIndexOn" value="0" /></td>
                        <td>01:00 to 02:00:<stripes:checkbox name="availabilityBitIndexOn" value="1" /></td>
                        <td>02:00 to 03:00:<stripes:checkbox name="availabilityBitIndexOn" value="2" /></td>
                        <td>03:00 to 04:00:<stripes:checkbox name="availabilityBitIndexOn" value="3" /></td>
                    </tr>
                    <tr>
                        <td>04:00 to 05:00:<stripes:checkbox name="availabilityBitIndexOn" value="4" /></td>
                        <td>05:00 to 06:00:<stripes:checkbox name="availabilityBitIndexOn" value="5" /></td>
                        <td>06:00 to 07:00:<stripes:checkbox name="availabilityBitIndexOn" value="6" /></td>
                        <td>07:00 to 08:00:<stripes:checkbox name="availabilityBitIndexOn" value="7" /></td>
                    </tr>
                    <tr>
                        <td>08:00 to 09:00:<stripes:checkbox name="availabilityBitIndexOn" value="8" /></td>
                        <td>09:00 to 10:00:<stripes:checkbox name="availabilityBitIndexOn" value="9" /></td>
                        <td>10:00 to 11:00:<stripes:checkbox name="availabilityBitIndexOn" value="10" /></td>
                        <td>11:00 to 12:00:<stripes:checkbox name="availabilityBitIndexOn" value="11" /></td>
                    </tr>
                    <tr>
                        <td>12:00 to 13:00:<stripes:checkbox name="availabilityBitIndexOn" value="12" /></td>
                        <td>13:00 to 14:00:<stripes:checkbox name="availabilityBitIndexOn" value="13" /></td>
                        <td>14:00 to 15:00:<stripes:checkbox name="availabilityBitIndexOn" value="14" /></td>
                        <td>15:00 to 16:00:<stripes:checkbox name="availabilityBitIndexOn" value="15" /></td>
                    </tr>
                    <tr>
                        <td>16:00 to 17:00:<stripes:checkbox name="availabilityBitIndexOn" value="16" /></td>
                        <td>17:00 to 18:00:<stripes:checkbox name="availabilityBitIndexOn" value="17" /></td>
                        <td>18:00 to 19:00:<stripes:checkbox name="availabilityBitIndexOn" value="18" /></td>
                        <td>19:00 to 20:00:<stripes:checkbox name="availabilityBitIndexOn" value="19" /></td>
                    </tr>
                    <tr>
                        <td>20:00 to 21:00:<stripes:checkbox name="availabilityBitIndexOn" value="20" /></td>
                        <td>21:00 to 22:00:<stripes:checkbox name="availabilityBitIndexOn" value="21" /></td>
                        <td>22:00 to 23:00:<stripes:checkbox name="availabilityBitIndexOn" value="22" /></td>
                        <td>23:00 to 24:00:<stripes:checkbox name="availabilityBitIndexOn" value="23" /></td>
                    </tr>

                    <tr>
                        <td colspan="2">
                            <span class="button"> 
                                <stripes:submit name="updateUnitCreditInstance"/>
                            </span>                        
                        </td>
                    </tr> 
                </table>                
            </stripes:form>

            <br/><br/>
            <table class="clear">
                <tr>
                    <td><b>Unit Credit Info:</b></td>
                </tr>
                <tr>
                    <td>${actionBean.unitCreditInstance.info}</td>
                </tr>
            </table>

        </div>		
    </stripes:layout-component>
</stripes:layout-render>

