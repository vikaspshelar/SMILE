<%-- 
    Document   : add_customer_role
    Created on : 17 Jan 2013, 5:09:29 PM
    Author     : lesiba
--%>
<%@page import="com.smilecoms.commons.base.BaseUtils, com.smilecoms.commons.stripes.SmileActionBean"%>

<%@ include file="/include/sep_include.jsp" %>

<c:choose>
    <c:when test="${actionBean.portInEvent.messageType == 'SMILE_REQUEST_EMERGENCY_RESTORE_ID'}">
        <c:set var="title">
            <fmt:message key="customer.port.emergency.restore"/>
        </c:set>
    </c:when>
    <c:otherwise>
        <c:set var="title">
            <fmt:message key="view.port.order"/>
        </c:set>
    </c:otherwise>
</c:choose>
                                
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
                        <fmt:message key="port.order"/> ${actionBean.portInEvent.portingOrderId}
                    </td>
                </tr>
            </table>

            <table class="clear">
                    <tr>
                        <td colspan="3"><b><fmt:message key="port.order.details"/>:</b></td>                       
                    </tr>
                    <% if (BaseUtils.getBooleanProperty("env.mnp.generate.port.request.form.id", false)) {%>
                    <tr>
                        <td><fmt:message key="port.request.form.id"/>:</td>
                        <td>${actionBean.portInEvent.portRequestFormId}</td>
                    </tr>
                    <% } %>
                    <tr>
                        <td><fmt:message key="porting.customerprofileid"/></td>   
                        <td>
                            <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                <stripes:param name="customerQuery.customerId" value="${actionBean.portInEvent.customerProfileId}"/>
                                ${actionBean.portInEvent.customerProfileId}
                            </stripes:link>
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="port.order.id"/>:</td>
                        <td>${actionBean.portInEvent.portingOrderId}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="port.order.direction"/>:</td>
                        <td>${actionBean.portInEvent.portingDirection}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="porting.validation.msisdn"/>:</td>
                        <td>${actionBean.portInEvent.validationMSISDN}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="port.order.request.date"/>:</td>
                        <td>${s:formatDateShort(actionBean.portInEvent.requestDatetime)}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="port.order.state"/>:</td>
                        <td>${actionBean.portInEvent.npState}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="portin.datetime"/>:</td>
                        <td>${s:formatDateShort(actionBean.portInEvent.portingDate)}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="portin.donor.network"/>:</td>
                        <td>${actionBean.portInEvent.donorId}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="port.order.recipient.network.id"/>:</td>
                        <td>${actionBean.portInEvent.recipientId}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="portin.original.rangeholder"/>:</td>
                        <td>${actionBean.portInEvent.rangeHolderId}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="port.order.processing.status"/>:</td>
                        <td>${actionBean.portInEvent.processingStatus}</td>
                    </tr>
                    <% if (BaseUtils.getBooleanProperty("env.mnp.include.ringfencing.on.new.portorder", false)) {%>
                    <tr>
                        <td><fmt:message key="ring.fence.indicator"/>:</td>
                        <td>${actionBean.portInEvent.ringFenceIndicator}</td>
                    </tr>
                    <%}%>
                    <tr>
                        <td><fmt:message key="port.order.error.code"/>:</td>
                        <td>${actionBean.portInEvent.errorCode}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="port.order.error.description"/>:</td>
                        <td>${actionBean.portInEvent.errorDescription}</td>
                    </tr>
                    <c:if test="${actionBean.portInEvent.emergencyRestoreId != null}">
                    <tr>
                        <td><fmt:message key="port.order.remergency.restore.id"/>:</td>
                        <td>${actionBean.portInEvent.emergencyRestoreId}</td>
                    </tr>   
                    </c:if>
                    <tr>
                        <td colspan="2"><b><fmt:message key="numbers.to.port"/>:</b></td>
                    </tr>
                    
                    <tr>
                        <td colspan="2">
                        <table class="green" width="98%"> 
                            <c:choose>
                                <c:when test="${s:getListSize(actionBean.portInEvent.routingInfoList.routingInfo) > 0}">
                                    <tr>
                                        <th><fmt:message key="service.instance.id"/></th>
                                        <th><fmt:message key="phone.number.start"/></th>
                                        <th><fmt:message key="phone.number.end"/></th>
                                        <th><fmt:message key="ported.number.return"/></th>
                                    </tr>
                                    <c:forEach items="${actionBean.portInEvent.routingInfoList.routingInfo}" var="routingInfo" varStatus="loop">                        
                                            <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                                <td>${routingInfo.serviceInstanceId}</td>
                                                <td>${routingInfo.phoneNumberRange.phoneNumberStart}</td>
                                                <td>${routingInfo.phoneNumberRange.phoneNumberEnd}</td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${routingInfo.serviceInstanceStatus != 'AC' && actionBean.portInEvent.portingDirection == 'IN' && actionBean.portInEvent.npState == 'NP_DONE'}">
                                                            <stripes:form action="/Customer.action">
                                                                    <stripes:hidden name="portInEvent.customerProfileId" value="${actionBean.portInEvent.customerProfileId}"/>
                                                                    <stripes:hidden name="portInEvent.routingInfoList.routingInfo[${loop.index}].phoneNumberRange.phoneNumberStart" value="${routingInfo.phoneNumberRange.phoneNumberStart}"/>
                                                                    <stripes:hidden name="portInEvent.routingInfoList.routingInfo[${loop.index}].phoneNumberRange.phoneNumberEnd" value="${routingInfo.phoneNumberRange.phoneNumberEnd}"/>
                                                                    <input type="hidden" name="portInEvent.portingOrderId" value="${actionBean.portInEvent.portingOrderId}"/>
                                                                    <input type="submit"  name="returnPortedNumberToRangeHolder" value="Return"/>
                                                            </stripes:form>
                                                        </c:when>

                                                        <c:otherwise>
                                                            <c:if test="${routingInfo.serviceInstanceStatus != 'AC' && actionBean.portInEvent.npState == 'NP_AWAITING_RETURN_BROADCAST'}">
                                                                <stripes:form action="/Customer.action">
                                                                    <stripes:hidden name="portInEvent.customerProfileId" value="${actionBean.portInEvent.customerProfileId}"/>
                                                                    <stripes:hidden name="portInEvent.routingInfoList.routingInfo[${loop.index}].phoneNumberRange.phoneNumberStart" value="${routingInfo.phoneNumberRange.phoneNumberStart}"/>
                                                                    <stripes:hidden name="portInEvent.routingInfoList.routingInfo[${loop.index}].phoneNumberRange.phoneNumberEnd" value="${routingInfo.phoneNumberRange.phoneNumberEnd}"/>
                                                                    <input type="hidden" name="portInEvent.portingOrderId" value="${actionBean.portInEvent.portingOrderId}"/>
                                                                    <input type="submit"  name="cancelReturnOfPortedNumberToRangeHolder" value="Cancel Return"/>
                                                                </stripes:form>
                                                            </c:if>
                                                        </c:otherwise>

                                                    </c:choose>
                                                </td>
                                            </tr>
                                    </c:forEach>
                                </c:when>
                            </c:choose>
                    </table>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <s:displayWhenReady event="retrieveImagesSnippet" href="/Customer.action" containerId="images" autoLoad="false" displayMessage="Click here to view attached documents">
                                <stripes:param name="portInEvent.portingOrderId" value="${actionBean.portInEvent.portingOrderId}"/>
                            </s:displayWhenReady>  
                        </td>
                    </tr>
                    <c:if test="${fn:length(actionBean.portInEvent.ringFenceNumberList) > 0}">
                        <tr>
                            <td colspan="2">
                                <br />
                                <b><fmt:message key="ring.fencing"/>:</b>
                                <br />
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2">
                                <table class="green" width="99%" > 
                                    <tr>
                                        <th>#</th>
                                        <th><fmt:message key="phone.number.start"/></th>
                                        <th><fmt:message key="phone.number.end"/></th>
                                    </tr>
                                    <c:forEach items="${actionBean.portInEvent.ringFenceNumberList}" var="ringFenceNumber" varStatus="ringFenceIndex"> 
                                    <tr>
                                        <td>${ringFenceIndex.index}</td>
                                        <td>${ringFenceNumber.phoneNumberStart}</td>           
                                        <td>${ringFenceNumber.phoneNumberEnd}</td>           
                                    </tr>
                                    </c:forEach>
                                    <tr>
                                        <td></td>
                                        <td></td>
                                        <td>Count: ${fn:length(actionBean.portInEvent.ringFenceNumberList)}</td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </c:if>
                        <tr>
                            <td colspan="2">
                                <table class="clear">
                                    <tr> 
                                        <td>
                                            <input type="button" value="Back" onclick="window.history.back();"/>
                                        </td>
                                        <td>
                                            <c:if test="${actionBean.portInEvent.messageType == 'SMILE_REQUEST_EMERGENCY_RESTORE_ID' && actionBean.portInEvent.npState == 'NP_DONE' 
                                                       && actionBean.portInEvent.processingStatus == 'DONE' && actionBean.portInEvent.emergencyRestoreId == null}">
                                                <stripes:form action="/Customer.action">
                                                    <stripes:hidden name="portInEvent.portingOrderId" value="${routingInfo.phoneNumberRange.phoneNumberStart}"/>
                                                    <stripes:hidden name="customer.customerId" value="${actionBean.portInEvent.customerProfileId}"/>
                                                    <stripes:submit name="requestEmergencyRestoreId"/>
                                                    <stripes:hidden name="portInEvent.messageType" value="SMILE_REQUEST_EMERGENCY_RESTORE_ID"/>
                                                </stripes:form>
                                            </c:if>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                    </tr>
            </table>
    </div>
</stripes:layout-component>
</stripes:layout-render>