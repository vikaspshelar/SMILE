<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="recharge.select.sim"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}"> 
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function () {
                makeMenuActive('Buy Smile Bundle');
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">  

        <div style="margin-top: 4px;" class="sixteen columns alpha">
            <div style="margin-top: 10px;" class="sixteen columns alpha">

                <table class="greentbl" width="99%">
                    <stripes:form action="/Account.action" autocomplete="off">

                        <tr>
                            <th colspan="3">Choose SIM to load bundle on</th>
                        </tr>

                        <c:forEach items="${actionBean.serviceInstanceList.serviceInstances}" var="serviceInstance" varStatus="loopSIMSIs">
                            <tr class="${loopSIMSIs.count mod 2 == 0 ? "odd" : "even"}">
                                <td>
                                    <input type="radio" name="productInstanceIdForSIM" value="${serviceInstance.productInstanceId}" checked="checked"/>
                                </td>
                                <td>
                                    <c:forEach items="${actionBean.productInstanceList.productInstances}" var="prodInstance" varStatus="loopSIMPIs">
                                        <c:if test="${prodInstance.productInstanceId == serviceInstance.productInstanceId}">
                                            <c:set var="productSpec" value="${s:getProductSpecification(prodInstance.productSpecificationId)}"/>
                                            <c:if test="${prodInstance.friendlyName != ''}">
                                                ${prodInstance.friendlyName}
                                            </c:if>
                                            <c:if test="${prodInstance.friendlyName == ''}">
                                                ${productSpec.name}
                                            </c:if>
                                        </c:if>
                                    </c:forEach>
                                </td>
                                <td>
                                    ${s:getServiceInstanceICCID(serviceInstance.serviceInstanceId)}
                                </td>
                                <%--<td>
                                    ${s:getServiceInstancePhoneNumber(serviceInstance.serviceInstanceId)}
                                </td>--%>

                            </tr>
                        </c:forEach>
                        <tr>
                            <td colspan="4">
                                <input type="hidden" name="account.accountId" value="${actionBean.account.accountId}"/>
                                <input class="button_confirm_to_proceed" type="submit" name="showAddUnitCredits" value="CONFIRM TO PROCEED"/>
                            </td>
                        </tr>
                    </stripes:form>
                </table>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
