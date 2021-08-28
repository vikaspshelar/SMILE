<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="check.rate"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">


        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>
                        Service Instance Id ${actionBean.serviceInstance.serviceInstanceId}
                    </td>
                    <td align="right">                       
                        <stripes:form action="/Account.action">                                
                            <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/>       
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveAccount"><fmt:message key="manage.account"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>

            <stripes:form action="/Account.action" focus="" autocomplete="off">    
                <stripes:hidden name="serviceInstance.serviceInstanceId" value="${actionBean.serviceInstance.serviceInstanceId}"/>
                <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/> 
                <table class="clear">            
                    <tr>
                        <td>Incoming Trunk Id:</td>
                        <td><stripes:text name="reservationRequestData.incomingTrunk" class="required" maxlength="20" onkeyup="validate(this,'^[0-9]{1,5}$','')"/></td>
                    </tr>
                    <tr>
                        <td>Outgoing Trunk Id:</td>
                        <td><stripes:text name="reservationRequestData.outgoingTrunk" class="required" maxlength="20" onkeyup="validate(this,'^[0-9]{1,5}$','')"/></td>
                    </tr>
                    <tr>
                        <td>From:</td>
                        <td><stripes:text name="reservationRequestData.from" class="required" maxlength="20" onkeyup="validate(this,'^[0-9]{0,15}$','')"/></td>
                    </tr>
                    <tr>
                        <td>To:</td>
                        <td><stripes:text name="reservationRequestData.to" class="required" maxlength="20" onkeyup="validate(this,'^[0-9]{0,15}$','')"/></td>
                    </tr>
                    <tr>
                        <td>Service Code:</td>
                        <td><stripes:text name="reservationRequestData.serviceCode" size="50" class="required" maxlength="100" onkeyup="validate(this,'.*','')"/></td>
                    </tr>
                    <tr>
                        <td>Rating Group:</td>
                        <td><stripes:text name="reservationRequestData.ratingGroup" class="required" maxlength="20" onkeyup="validate(this,'^[0-9]{1,15}$','')"/></td>
                    </tr>
                    <tr>
                        <td>Leg:</td>
                        <td><stripes:text name="reservationRequestData.leg" class="required" maxlength="1" onkeyup="validate(this,'^O|T$','')"/></td>
                    </tr>
                    <tr>
                        <td>Number of Units:</td>
                        <td><stripes:text name="reservationRequestData.unitQuantity" class="required" maxlength="15" onkeyup="validate(this,'^[0-9]{1,15}$','')"/></td>
                    </tr>
                    <tr>
                        <td>Unit Type:</td>
                        <td><stripes:text name="reservationRequestData.unitType" class="required" maxlength="15" onkeyup="validate(this,'.*','')"/></td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="checkRate"  />
                            </span>                        
                        </td>
                    </tr>  
                </table>            
            </stripes:form>

            <br/>
            <c:if test="${actionBean.reservationResultData != null}">
                <table class="clear">         
                    <tr>
                        <td>Service Code:</td>
                        <td>${actionBean.reservationRequestData.serviceCode}</td>
                    </tr>
                    <tr>
                        <td>Identifier:</td>
                        <td>${actionBean.reservationRequestData.identifier}</td>
                    </tr>
                    <tr>
                        <td>Identifier Type:</td>
                        <td>${actionBean.reservationRequestData.identifierType}</td>
                    </tr>
                    <tr>
                        <td>Retail cents per Unit:</td>
                        <td>${s:formatBigDecimal(actionBean.reservationResultData.retailCentsPerUnit)}</td>
                    </tr>
                    <tr>
                        <td>From Interconnect cents per Unit:</td>
                        <td>${s:formatBigDecimal(actionBean.reservationResultData.fromInterconnectCentsPerUnit)}</td>
                    </tr>
                    <tr>
                        <td>To Interconnect cents per Unit:</td>
                        <td>${s:formatBigDecimal(actionBean.reservationResultData.toInterconnectCentsPerUnit)}</td>
                    </tr>
                    <tr>
                        <td>Result Code:</td>
                        <td>${actionBean.reservationResultData.errorCode}</td>
                    </tr>
                </table> 
            </c:if>
        </div>

    </stripes:layout-component>


</stripes:layout-render>

