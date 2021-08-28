<%-- 
    Document   : add_customer_role
    Created on : 17 Jan 2013, 5:09:29 PM
    Author     : lesiba
--%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="mnp.create.ring-fence.request"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">

            var today = new Date().getFullYear();
            $j(document).ready(function () {
                $j('#datePicker1').datepicker({dateFormat: 'yy/mm/dd 00:00:00', showOn: 'button', buttonText: "..", changeYear: true, changeMonth: true, onSelect: function () {
                        validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01]) ([012][0-9])[:]([0-5][0-9]):([0-5][0-9])$', 'emptynotok');
                    }});
            });
  
        </script>

        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="customer"/> ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/Customer.action">                                
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveCustomer"><fmt:message key="manage.customer"/></stripes:option>                                
                            </stripes:select>
                            <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>
            <stripes:form action="/Customer.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();"> 
                <stripes:hidden name="portInEvent.recipientId" value="08"/>
                <stripes:hidden name="portInEvent.portingOrderId" value=""/>
                <stripes:hidden name="portInEvent.messageId" value="-1"/>
                
                <c:forEach items="${actionBean.portInEvent.routingInfoList.routingInfo}" var="routingInfo" varStatus="loop">
                    <div ng-init="addPhoneNumberRangeEntryToList(${routingInfo.phoneNumberRange.phoneNumberStart}, ${routingInfo.phoneNumberRange.phoneNumberEnd})"></div>
                </c:forEach>
                    
                <table class="clear" >
                    <tr>
                        <td colspan="2"><b><fmt:message key="customer"/>:</b></td>                        
                    </tr>
                    <tr>
                        <td><fmt:message key="id"/>:</td>
                        <td>
                            <stripes:hidden id="portInEvent.customerProfileId" name="portInEvent.customerProfileId" value="${actionBean.customer.customerId}"/>
                            ${actionBean.customer.customerId}
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="customer.name"/>:</td>
                        <td>
                            <stripes:hidden name="portInEvent.customerName"    value="${actionBean.customer.firstName} ${actionBean.customer.lastName}" />
                            ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="classification"/>:</td>
                        <td>
                            <stripes:hidden name="portInEvent.customerType"    value="individual" />
                            Individual
                        </td>
                    </tr>
                        <tr>
                            <td><fmt:message key="porting.validation.msisdn"/>:</td>
                            <td>${actionBean.customer.alternativeContact1}</td>
                        </tr>
                    <tr>
                    <td colspan="2">
                        <br/><b><fmt:message key="phone.numbers.to.ring.fence"/>:</b><br/>
                    </td>                        
                    </tr>
                    <tr>
                        <td colspan="2">
                <div>
                    <table class="green" width="99%" > 
                        <tr>
                            <th>Row #</th>
                            <th><fmt:message key="phone.number.start"/></th>
                            <th><fmt:message key="phone.number.end"/></th>
                            <th><fmt:message key="delete"/></th>
                        </tr>
                        <tr ng-repeat="item in phoneNumberRangeList.items">
                            <td>{{$index + 1}}</td>
                            <c:set var="defaultRule" value="validate(this, \\\'^[0-9]{10}$\\\',\\\'emptynotok\\\')"/>
                            <td><stripes:text name="portInEvent.routingInfoList.routingInfo[{{ $index }}].phoneNumberRange.phoneNumberStart" type="text"  ng-model="item.phoneNumberStart"  onkeyup="${s:getValidationRule('porting.msisdn', defaultRule)}" maxlength="15" size='15'/></td>           
                            <td><stripes:text name="portInEvent.routingInfoList.routingInfo[{{ $index }}].phoneNumberRange.phoneNumberEnd" type="text" ng-model="item.phoneNumberEnd" onkeyup="${s:getValidationRule('porting.msisdn', defaultRule)}" maxlength="15" size='15'/></td>           
                            <td>
                                [<a href ng:click="removePhoneNumberRangeEntryFromList($index)">Delete</a>]
                            </td>
                        </tr>
                        <tr>
                            <td></td>
                            <td></td>
                            <td></td>
                            <td>Count: {{ phoneNumberRangeEntryListSize() }}</td>
                        </tr>
                    </table>
                        <span>
                            <input type="button" ng:click="addPhoneNumberRangeEntryToList('','')" value="Add phone number"/>
                        </span>
                </div>
                </div>
                        </td>
                </tr>
                </table>
                            
                <span class="button">
                    <stripes:submit name="createRingFenceRequest"/>
                </span>
        </stripes:form>

  
    </div>
                        
                        
</stripes:layout-component>
</stripes:layout-render>