<%-- 
Document   : view_account
Created on : Feb 21, 2012, 4:52:32 PM
Author     : lesiba
--%>

<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="scp.manage.account"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp"  title="${title}"> 
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function () {
            makeMenuActive('Profile_AccountsPage');
            }

            function showEditDiv(elementId, elementId2) {
            document.getElementById(elementId).style.display = "block";
            document.getElementById(elementId2).style.display = "none";
            }
            function hideEditDiv(elementId, elementId2, elementId3) {
            document.getElementById(elementId).style.display = "none";
            if (elementId3 && elementId3.length > 0) {
            document.getElementById(elementId3).style.display = "block";
            document.getElementById(elementId2).style.display = "none";
            } else {
            document.getElementById(elementId2).style.display = "block";
            }
            }
            function hideInfoDiv(elementId, elementId2) {
            document.getElementById(elementId).style.display = "none";
            document.getElementById(elementId2).style.display = "block";
            }

            var dialogStartUps = function () {

            $("#left_banner_popup").dialog({
            resizable: false,
                    modal: true,
                    width: 930,
                    buttons: {
                    Ok: function () {
                    $(this).dialog("close");
                    }
                    },
                    autoOpen: false,
                    show: {
                    effect: "blind",
                            duration: 500
                    },
                    hide: {
                    effect: "explode",
                            duration: 500
                    },
            });
            $("#left_banner_img_link").click(function () {
            $("#left_banner_popup").dialog("option", "position", 'center');
            $("#left_banner_popup").dialog("open");
            });
            $("#dialog").dialog({
            resizable: false,
                    modal: true,
                    width: 500,
                    buttons: {
                    Ok: function () {
                    $(this).dialog("close");
                    }
                    },
                    autoOpen: false,
                    show: {
                    effect: "blind",
                            duration: 500
                    },
                    hide: {
                    effect: "explode",
                            duration: 500
                    }
            });
            $("#opener").click(function () {
            $("#dialog").dialog("option", "position", 'center');
            $("#dialog").dialog("open");
            });
            }

            $(dialogStartUps);</script>

    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <div style="margin-top: 10px;" class="sixteen columns alpha">

            
            <div class="accounts_table sixteen columns">
                <div class="account_form">
                    <table width="100%">
                        <tr>
                            <td style="padding-top: 5px;">
                                Acc: ${actionBean.account.accountId}
                            </td>
                            <td>
                                <fmt:message key="scp.airtime.balance"/>: 
                            </td>
                            <td>
                                ${s:convertCentsToCurrencyShortWithCommaGroupingSeparator(actionBean.account.availableBalanceInCents)}
                            </td>
                        </tr>
                        <tr>
                            <td style="padding-top: 5px;">

                            </td>
                            <td>
                                <fmt:message key="scp.bundle.balance"/>: 
                            </td>
                            <td>
                                ${s:displayVolumeAsStringWithCommaGroupingSeparator(actionBean.bundleBalance, 'Byte')}
                                <c:if test="${s:containsUnlimitedBundle(actionBean.account.accountId)}"><fmt:message key="scp.unlimited.servicespectrigger.msg"/></c:if>
                            </td>
                            </tr>
                            <tr>
                            <stripes:form action="/Account.action">
                                <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/>
                                <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/>

                                <td style="padding-top: 5px;"></td>
                                <td>
                                    <fmt:message key="account.status"/>: 
                                </td>

                                <td>
                                    <stripes:select name="account.status">
                                        <c:forEach items="${actionBean.allowedAccountStatuses}" var="status"> 
                                            <c:if test="${actionBean.account.status == status}">
                                                <stripes:option value="${status}" selected="selected"><fmt:message key="account.status.${status}"/></stripes:option>
                                            </c:if>
                                            <c:if test="${actionBean.account.status != status}">
                                                <stripes:option value="${status}"><fmt:message key="account.status.${status}"/></stripes:option>
                                            </c:if>
                                        </c:forEach>               
                                    </stripes:select>  

                                    <div id="dialog" title="Information">
                                        <p><fmt:message key="scp.account.status.explanations"/></p>
                                    </div>                                

                                </td>
                                <td>
                                    <a style="margin-top: 5px;" id="opener"><img src="${pageContext.request.contextPath}/images/tmpFiles/information.png"/></a>
                                        <stripes:submit  name="changeAccountStatus"/>
                                </td>
                            </stripes:form>
                        </tr> 
                    </table>
                </div>

                <div style="margin-top: 20px;" class="accounts_table sixteen columns">

                    <c:choose>
                        <c:when test="${s:getListSize(actionBean.account.serviceInstances) > 0}"> 
                            <table>
                                <tr>
                                    <td colspan="2"><font style="color:#75B343; font-weight:bold;"><fmt:message key="scp.account.services"/></font></td>
                                </tr>                    
                            </table>

                            <table class="greentbl" width="99%">
                                <tr>
                                    <th><fmt:message key="service.name"/></th>
                                    <th><fmt:message key="scp.friendly.name"/></th>
                                    <th><fmt:message key="scp.icci.friendlyname.and.phonenumber"/></th>
                                    <th><fmt:message key="scp.edit.service.config"/></th>
                                    <th>Recharge</th>
                                </tr>

                                <c:set var="SIs" value="${actionBean.serviceInstanceList.serviceInstances}"/>
                                <c:set var="PIList" value="${actionBean.productInstanceList.productInstances}"/>

                                <c:forEach items="${SIs}" var="servInstance" varStatus="loop"> 

                                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}"> 

                                        <c:set var="serviceSpec" value="${s:getServiceSpecification(servInstance.serviceSpecificationId)}"/>
                                        <c:forEach items="${PIList}" var="prodInstance" varStatus="loop2">
                                            <c:if test="${prodInstance.productInstanceId eq servInstance.productInstanceId}">
                                                <c:set var="productInstance" value="${prodInstance}"/>
                                            </c:if>
                                        </c:forEach>
                                        <c:set var="productSpec" value="${s:getProductSpecification(productInstance.productSpecificationId)}"/>

                                        <td align="center">
                                            ${serviceSpec.name}
                                        </td>


                                        <td  align="center">
                                            <!-- Start of Informational Div-->
                                            <div id="msg_update_${servInstance.serviceInstanceId}_${loop.count}" style="display: none;">

                                                <!-- Informational div for errors -->
                                                <div ng-show="serverError != '' && serverError != null" ng-style="{ color:'red' }">
                                                    <em><img src="images/error.gif"/><fmt:message key="scp.change.friendlyname.servererror.msg"/></em>
                                                        <c:if test="${!empty productInstance.friendlyName}">
                                                        <a class="clicktoeditsavecancel" href="#" onclick="hideInfoDiv('msg_update_${servInstance.serviceInstanceId}_${loop.count}', 'divSI_${servInstance.serviceInstanceId}_${loop.count}');">Ok</a>
                                                    </c:if>
                                                    <c:if test="${empty productInstance.friendlyName}">
                                                        <a class="clicktoeditsavecancel" href="#" onclick="hideInfoDiv('msg_update_${servInstance.serviceInstanceId}_${loop.count}', 'divSS_${servInstance.serviceInstanceId}_${loop.count}');">Ok</a>
                                                    </c:if>
                                                </div>

                                                <!-- Informational div for success -->
                                                <div ng-show="serverSuccess != '' && serverSuccess != null" ng-style="{ color:'green' }">
                                                    <em><img style="vertical-align:middle" src="images/info.gif"/>Successfully changed to: "{{productName}}"</em>
                                                </div>
                                            </div>
                                            <!-- End of Informational Div-->           

                                            <!-- Start of Editor Enabler Div-->           
                                            <c:if test="${!empty productInstance.friendlyName}">
                                                <div id="divSI_${servInstance.serviceInstanceId}_${loop.count}">
                                                    <a class="clicktoedit" href="#" onclick="showEditDiv('si_${servInstance.serviceInstanceId}_${loop.count}', 'divSI_${servInstance.serviceInstanceId}_${loop.count}');" ng-style="{ color:'green' }">${productInstance.friendlyName}</a>
                                                </div>
                                            </c:if>
                                            <c:if test="${empty productInstance.friendlyName}">
                                                <div id="divSS_${servInstance.serviceInstanceId}_${loop.count}">
                                                    <a class="clicktoedit" href="#" onclick="showEditDiv('ss_${servInstance.serviceInstanceId}_${loop.count}', 'divSS_${servInstance.serviceInstanceId}_${loop.count}');"  ng-style="{ color:'green' }">${productSpec.name}</a>
                                                </div>
                                            </c:if>
                                            <!-- End of Editor Enabler Div--> 

                                            <!-- Start of Changes Saver Div--> 
                                            <c:if test="${!empty productInstance.friendlyName}">
                                                <div id="si_${servInstance.serviceInstanceId}_${loop.count}" style="display: none;">
                                                    <input style="border-width:1px; border-style:inset;" ng-model="editableName"/><br/>
                                                    <span>
                                                        <a class="clicktoeditsavecancel" href="#" onclick="hideEditDiv('si_${servInstance.serviceInstanceId}_${loop.count}', 'divSI_${servInstance.serviceInstanceId}_${loop.count}', 'msg_update_${servInstance.serviceInstanceId}_${loop.count}');" ng-click="modifyProductInstanceFriendlyName('${actionBean.customer.customerId}', '${productInstance.productInstanceId}')">Save</a>
                                                        or 
                                                        <a class="clicktoeditsavecancel" href="#" onclick="hideEditDiv('si_${servInstance.serviceInstanceId}_${loop.count}', 'divSI_${servInstance.serviceInstanceId}_${loop.count}', '');">Cancel</a>
                                                    </span>    
                                                </div>
                                            </c:if>
                                            <c:if test="${empty productInstance.friendlyName}">
                                                <div id="ss_${servInstance.serviceInstanceId}_${loop.count}" style="display: none;">
                                                    <input style="border-width:1px; border-style:inset;" ng-model="editableName"/><br/>
                                                    <span>
                                                        <a class="clicktoeditsavecancel" href="#" onclick="hideEditDiv('ss_${servInstance.serviceInstanceId}_${loop.count}', 'divSS_${servInstance.serviceInstanceId}_${loop.count}', 'msg_update_${servInstance.serviceInstanceId}_${loop.count}');" ng-click="modifyProductInstanceFriendlyName('${actionBean.customer.customerId}', '${productInstance.productInstanceId}')">Save</a>
                                                        or 
                                                        <a class="clicktoeditsavecancel" href="#" onclick="hideEditDiv('ss_${servInstance.serviceInstanceId}_${loop.count}', 'divSS_${servInstance.serviceInstanceId}_${loop.count}', '');">Cancel</a>
                                                    </span>
                                                </div> 
                                            </c:if>
                                            <!-- End of Changes Saver Div--> 
                                        </td>

                                        <td  align="center"><fmt:message key="scp.smile.number"/>:${s:getServiceInstancePhoneNumber(servInstance.serviceInstanceId)} <br/>ICCID:${s:getServiceInstanceICCID(servInstance.serviceInstanceId)}</td>

                                        <td align="center">
                                            <c:choose>
                                                <c:when test="${serviceSpec.serviceSpecificationId ge 1000 || serviceSpec.serviceSpecificationId eq 200}">
                                                    N/A
                                                </c:when>
                                                <c:otherwise>
                                                    <stripes:form action="/Product.action" focus="" name="frm">
                                                        <stripes:param name="productInstance.productInstanceId" value="${servInstance.productInstanceId}"/>
                                                        <stripes:param name="serviceInstance.serviceInstanceId" value="${servInstance.serviceInstanceId}"/>
                                                        <input class="button_recharge" type="submit" name="showChangeServiceInstanceConfiguration" value="Manage"/>
                                                    </stripes:form>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        
                                        <td align="center">
                                            <c:choose>
                                                <c:when test="${serviceSpec.serviceSpecificationId ge 1000 || serviceSpec.serviceSpecificationId eq 200}">
                                                    N/A
                                                </c:when>
                                                <c:otherwise>
                                                    <stripes:form action="/Account.action">
                                                        <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/>
                                                        <input type="hidden" name="productInstanceIdForSIM" value="0"/>
                                                        <input type="submit" class="button_recharge" name="showAddUnitCredits" value="Recharge"/>
                                                    </stripes:form>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>                                
                                </c:forEach>
                            </table>
                        </c:when>
                        <c:otherwise>
                            <table>
                                <tr style="font-size: 12px;">
                                    <td>
                                        <fmt:message key="no.service.instances"/>
                                    </td>
                                </tr>
                            </table>
                        </c:otherwise>
                    </c:choose>

                </div>

            </div>
            
            <div class="sixteen columns">
                <br/>
                <br/>
                <table>
                    <tr>
                        <td colspan="5"><font style="color:#75B343; font-weight:bold;"><fmt:message key="scp.unit.credits"/></font></td>
                    </tr>
                </table>

                <c:choose>
                    <c:when test="${s:getListSize(actionBean.account.unitCreditInstances) > 0}">
                        <table class="greentbl" width="99%">
                            <tr>
                                <th><fmt:message key="unit.credit.name"/></th>
                                <th><fmt:message key="scp.icci.friendlyname"/></th>
                                <th><fmt:message key="unit.credit.purchase.date"/></th>
                                <th><fmt:message key="start.date"/></th>
                                <th><fmt:message key="unit.credit.end.date"/></th>                                
                                <th><fmt:message key="balance"/></th>                        
                            </tr>
                            <c:forEach items="${s:orderList(actionBean.account.unitCreditInstances,'getPurchaseDate','desc')}" var="UnitCreditInstance" varStatus="loop">
                                <c:set var="ucs" value="${s:getUnitCreditSpecification(UnitCreditInstance.unitCreditSpecificationId)}"/>
                                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                    <td>${UnitCreditInstance.name}</td>
                                    <c:choose>
                                        <c:when test="${UnitCreditInstance.productInstanceId > 0}">
                                            <td>${s:getProductInstanceICCID(UnitCreditInstance.productInstanceId)}</td>
                                        </c:when>
                                        <c:otherwise>
                                            <td>All SIM Cards</td>
                                        </c:otherwise>
                                    </c:choose>
                                    <td align="center">${s:formatDateLong(UnitCreditInstance.purchaseDate)}</td>
                                    <td align="center">${s:formatDateLong(UnitCreditInstance.startDate)}</td>
                                    <td align="center">${s:formatDateLong(UnitCreditInstance.endDate)}</td>        
                                    <c:choose>
                                        <c:when test="${fn:contains(ucs.configuration, 'SCPDisplayOnlyUsage=true')}">
                                            <td align="right">${s:displayVolumeAsStringWithCommaGroupingSeparator(UnitCreditInstance.unitsAtStart - UnitCreditInstance.currentUnitsRemaining, ucs.unitType)} Used</td>
                                        </c:when>
                                        <c:when test="${fn:contains(ucs.configuration, 'DisplayBalance=false')}">
                                            <td align="right">Unlimited</td>
                                        </c:when>
                                        <c:when test="${fn:contains(ucs.configuration, 'PeriodicLimitUnits=') && !fn:contains(ucs.configuration, 'DisplayUnitsType=byte')}">
                                            <td align="right">
                                            <c:if test="${s:getValueFromCRDelimitedAVPString(ucs.configuration, 'DisplayUnitsType') == 'second'}">
                                                ${s:displayVolumeAsStringWithCommaGroupingSeparator((s:getTimeLeftBasedOnUnitsUsedFromBaseline(s:getValueFromCRDelimitedAVPString(ucs.configuration, 'MaxSpeedbps'),
                                                  UnitCreditInstance.auxCounter1, ucs.unitType, s:getValueFromCRDelimitedAVPString(ucs.configuration, 'PeriodicLimitUnits'),
                                                  s:getValueFromCRDelimitedAVPString(ucs.configuration, 'PeriodicLimitUnitType'), s:getValueFromCRDelimitedAVPString(ucs.configuration, 'DisplayUnitsType')))
                                                  , 'sec')}
                                            </c:if>
                                            <c:if test="${s:getValueFromCRDelimitedAVPString(ucs.configuration, 'DisplayUnitsType') == 'minute'}">
                                                ${s:displayVolumeAsStringWithCommaGroupingSeparator((s:getTimeLeftBasedOnUnitsUsedFromBaseline(s:getValueFromCRDelimitedAVPString(ucs.configuration, 'MaxSpeedbps'),
                                                  UnitCreditInstance.auxCounter1, ucs.unitType, s:getValueFromCRDelimitedAVPString(ucs.configuration, 'PeriodicLimitUnits'),
                                                  s:getValueFromCRDelimitedAVPString(ucs.configuration, 'PeriodicLimitUnitType'), s:getValueFromCRDelimitedAVPString(ucs.configuration, 'DisplayUnitsType'))) * 60
                                                  , 'sec')}
                                            </c:if>
                                            <c:if test="${s:getValueFromCRDelimitedAVPString(ucs.configuration, 'DisplayUnitsType') == 'hour'}">
                                                ${s:displayVolumeAsStringWithCommaGroupingSeparator((s:getTimeLeftBasedOnUnitsUsedFromBaseline(s:getValueFromCRDelimitedAVPString(ucs.configuration, 'MaxSpeedbps'),
                                                  UnitCreditInstance.auxCounter1, ucs.unitType, s:getValueFromCRDelimitedAVPString(ucs.configuration, 'PeriodicLimitUnits'),
                                                  s:getValueFromCRDelimitedAVPString(ucs.configuration, 'PeriodicLimitUnitType'), s:getValueFromCRDelimitedAVPString(ucs.configuration, 'DisplayUnitsType'))) * 60 * 60
                                                  , 'sec')}
                                            </c:if>
                                                
                                            </td>
                                        </c:when>    
                                        <c:otherwise>
                                            <td align="right">
                                                ${s:displayVolumeAsStringWithCommaGroupingSeparator(UnitCreditInstance.availableUnitsRemaining, ucs.unitType)}
                                                <c:if test="${fn:contains(ucs.configuration, 'AllowSplitting=true')}">
                                                    <br/>
                                                    <stripes:form action="/Account.action" focus="">
                                                        <stripes:hidden name="unitCreditInstance.unitCreditInstanceId" value="${UnitCreditInstance.unitCreditInstanceId}"/>
                                                        <stripes:hidden name="unitCreditInstance.unitCreditSpecificationId" value="${UnitCreditInstance.unitCreditSpecificationId}"/>
                                                        <stripes:hidden name="unitCreditInstance.accountId" value="${UnitCreditInstance.accountId}"/>
                                                        <stripes:hidden name="unitCreditInstance.availableUnitsRemaining" value="${UnitCreditInstance.availableUnitsRemaining}"/>
                                                        <%--<input class="split_uc_btn" type="submit" name="showSplitUnitCreditInstance" value=""/>--%>
                                                        <input class="button_split_uc" type="submit" name="showSplitUnitCreditInstance" value="<fmt:message key="scp.split.uc.btn"/>"/>
                                                    </stripes:form>
                                                </c:if>
                                            </td>  
                                        </c:otherwise>
                                    </c:choose>
                                </tr>
                            </c:forEach>
                        </table>
                    </c:when>
                    <c:otherwise>
                        <table>
                            <tr style="font-size: 12px;">
                                <td colspan="2"> 
                                    <font style="color:#75B343; font-weight:bold;"><fmt:message key="scp.no.unit.credits"/></font>
                                </td>
                            </tr>
                        </table>
                    </c:otherwise>
                </c:choose>

            </div>

        </div>


    </stripes:layout-component>
</stripes:layout-render>

