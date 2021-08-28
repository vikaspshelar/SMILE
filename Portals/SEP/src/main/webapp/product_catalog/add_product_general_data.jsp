<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="product.general.data"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/ProductCatalog.action" focus="" autocomplete="off">  

            <table class="clear">


                <c:if test="${s:hasPermissions('ADD_PRODUCT_SPECIFYING_KIT', actionBean.context)}">
                    <tr><td colspan="2"><b>Kit Information</b></td></tr>
                    <tr>
                        <td>
                            <fmt:message key="provision.as.kit"/>:
                        </td>
                        <td>
                            <stripes:select name="productOrder.kitItemNumber" class="required">
                                <stripes:option value=""></stripes:option>
                                <c:forEach items="${actionBean.kits}" var="kit">
                                    <stripes:option value="${kit[0]}">${s:convertCentsToCurrencyLongRoundHalfEven(kit[2])} - ${kit[0]} - ${kit[1]}</stripes:option>
                                </c:forEach>                           
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <fmt:message key="kits.device.serial.number"/>:
                        </td>
                        <td>
                            <stripes:text name="productOrder.deviceSerialNumber"  size="30"  maxlength="30"/>
                        </td>
                    </tr>
                </c:if>


                <c:if test="${s:hasPermissions('CHANGE_SERVICE_INSTANCE_EXISTING_ACCOUNT', actionBean.context) || s:hasPermissions('CHANGE_SERVICE_INSTANCE_ANY_ACCOUNT', actionBean.context)}">
                    <tr><td></td></tr>
                    <tr><td colspan="2"><b>Account To Charge</b></td></tr>
                    <tr>
                        <td>Account:</td>
                        <td>
                            <input type="text" name="productAccount" class="required" size="20" list="items">
                            <datalist id="items">
                                <option value="Create New Account"/>
                                <c:forEach items="${actionBean.accountIdList}" var="accountId">
                                    <option value="${accountId}"/>
                                </c:forEach>     
                            </datalist>
                            &nbsp; Double-Click or Hit Down Arrow to see options
                        </td>
                    </tr>
                </c:if>

                <tr><td></td></tr>
                <tr><td colspan="2"><b>Customer Segment Information</b></td></tr>
                <c:forEach items="${actionBean.segments}" var="segment"> 
                    <tr>
                        <td>${segment}</td>
                        <td><stripes:radio  name="productOrder.segment" value="${segment}"/></td>
                    </tr>
                </c:forEach>

                <c:if test="${s:getListSize(actionBean.customer.customerRoles) > 0}">
                    <tr><td></td></tr>
                    <tr><td colspan="2"><b>Customer Role</b></td></tr>
                    <c:forEach items="${actionBean.customer.customerRoles}" var="role"> 
                        <tr>
                            <td>${role.roleName} at ${role.organisationName}</td>
                            <td><stripes:radio  name="productOrder.organisationId" value="${role.organisationId}"/></td>
                        </tr>
                    </c:forEach>
                </c:if>


                <c:if test="${s:hasPermissions('USE_PRODUCT_PROMOTION_CODE', actionBean.context)}">
                    <tr><td></td></tr>
                    <tr><td colspan="2"><b>Promotion Information</b></td></tr>
                    <tr>
                        <td><fmt:message key="promotion.code"/>:</td>
                        <td>
                            <stripes:select name="productOrder.promotionCode">
                                <stripes:option value=""></stripes:option>
                                <c:forEach items="${s:getRoleBasedSubsetOfPropertyAsSet('env.promotion.codes.products', actionBean.context.request)}" var="code">
                                    <stripes:option value="${code}">${code}</stripes:option>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>   
                </c:if>

                <c:if test="${s:hasPermissions('ADD_PRODUCT_SPECIFYING_REFERRAL_CODE', actionBean.context)}">
                    <tr><td></td></tr>
                    <tr><td colspan="2"><b>Referral Information</b></td></tr>
                    <tr>
                        <td>Referral Code:</td>
                        <td>
                            <stripes:text name="productOrder.referralCode" size="20"  maxlength="100" />
                        </td>
                    </tr> 
                </c:if>
                <tr><td></td></tr>
                <tr>
                    <td colspan="2">
                        <span class="button">
                            <stripes:submit name="showAddProductWizard"/>
                        </span>
                        <span class="button">
                            <stripes:submit name="validateGeneralData"/>
                        </span>
                    </td>
                </tr>
            </table>
            <stripes:wizard-fields />
        </stripes:form>        
    </stripes:layout-component>    
</stripes:layout-render>

