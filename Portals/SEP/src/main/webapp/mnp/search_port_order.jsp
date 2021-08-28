<%@ include file="/include/sep_include.jsp" %>
<c:if test="${actionBean.portOrdersList.portInEvents == null}">
    <c:set var="title">
        <fmt:message key="search.port.order"/>
    </c:set>
</c:if>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <c:if test="${actionBean.portOrdersList.portInEvents == null}">
            <stripes:form action="/Customer.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">    
                <table class="clear">
                    <tr>
                        <td><fmt:message key="port.order.id"/>:</td>
                        <c:set var="defaultRule" value="validate(this, \\\'^.{14,14}$\\\',\\\'emptynotok\\\')"/>
                        <td><stripes:text maxlength="21" name="portOrdersQuery.portingOrderId" onkeyup="${s:getValidationRule('porting.order.id', defaultRule)}"/></td>
                    </tr>
                    <tr>
                        <td>
                            <span class="button">
                                <stripes:submit name="searchPortOrder"/>
                            </span>
                        </td>
                    </tr>
                </table>            
            </stripes:form>
        <br/>
        </c:if>
        
        <c:if test="${actionBean.portOrdersList.portInEvents != null}">
            <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <c:choose>
                           <c:when test="${actionBean.portOrdersQuery.customerProfileId != null}">
                                    <fmt:message key="customer.port.orders.list"/> ${actionBean.portOrdersQuery.customerProfileId}
                           </c:when>
                            <c:otherwise>
                                    <fmt:message key="port.orders.list"/>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </table>
            <br/>     
            <table class="green">
                <c:forEach items="${actionBean.portOrdersList.portInEvents}" var="portOrder" varStatus="loop">
                    <c:if test="${loop.index == 0}">
                        <tr>
                            <th><fmt:message key="port.order.id"/></th>
                            <th><fmt:message key="port.order.direction"/></th>
                            <th><fmt:message key="port.order.request.date"/></th>
                            <th><fmt:message key="port.order.state"/></th>
                            <th><fmt:message key="port.order.processing.status"/></th>
                            <th>
                            <c:choose>
                                <c:when test="${portOrder.messageType == 'SMILE_REQUEST_EMERGENCY_RESTORE_ID'}">
                                    <fmt:message key="customer.port.emergency.restore"/>
                                </c:when>
                                <c:otherwise>
                                    <fmt:message key="view"/>
                                </c:otherwise>
                            </c:choose>
                            </th>        
                        </tr>
                    </c:if>
                    
                    <tr class="odd">
                            <td>${portOrder.portingOrderId}</td>
                            <td>${portOrder.portingDirection}</td>
                            <td>${s:formatDateLong(portOrder.requestDatetime)}</td>
                            <td>${portOrder.npState}</td>
                            <td>${portOrder.processingStatus}</td>
                            <stripes:form action="/Customer.action">
                                <input type="hidden" name="portOrdersQuery.portingOrderId" value="${portOrder.portingOrderId}"/>
                                <input type="hidden" name="portOrdersQuery.portingDirection" value="${portOrder.portingDirection}"/>
                                <td>
                                    <c:choose>
                                        <c:when test="${portOrder.messageType == 'SMILE_REQUEST_EMERGENCY_RESTORE_ID'}">
                                            <stripes:form action="/Customer.action">
                                                <stripes:hidden name="portInEvent.portingOrderId" value="${portOrder.portingOrderId}"/>
                                                <stripes:hidden name="customer.customerId" value="${portOrder.customerProfileId}"/>
                                                <stripes:hidden name="portInEvent.messageType" value="SMILE_REQUEST_EMERGENCY_RESTORE_ID"/>
                                                <stripes:submit name="showEmergencyRestore"/>
                                            </stripes:form>
                                        </c:when>
                                        <c:otherwise>
                                            <input type="submit"  name="searchPortOrder" value="View"/>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </stripes:form>
                        </tr>                    
                </c:forEach>
            </table>    
            </div>
        </c:if>        
    </stripes:layout-component>    
</stripes:layout-render>

