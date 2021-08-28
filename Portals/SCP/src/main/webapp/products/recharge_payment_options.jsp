<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="recharge.select.payment.options"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function () {
            makeMenuActive('Buy Smile Bundle');
            }

            var dialogStartUps = function () {

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
            <c:forEach items="${s:getPropertyAsList('env.scp.payment.methods')}" var="paymentTypeProps" varStatus="loopP">
                <c:set var="ptProps" value="${fn:split(paymentTypeProps,'|')}"/>
                <c:set var="paymentTypeIntegration" value="${ptProps[1]}"/>
                <c:set var="paymentType" value="${ptProps[0]}"/>
                <c:set var="paymentTypeConfigs" value="${ptProps[2]}"/>

                <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="paymentTypeProps1" varStatus="loopConf">
                    <c:set var="ptConfigProps" value="${fn:split(paymentTypeProps1,'|')}"/>
                    <c:set var="paymentTypeImage" value="${ptConfigProps[0]}"/>
                    <c:set var="imageFileType" value="${ptConfigProps[1]}"/>

                    <c:if test="${paymentTypeIntegration == 'SCA'}">
                        <c:set var="gateAvailabilityProp" value="${ptConfigProps[2]}"/>
                        <c:set var="isPaymentGateNotAvailable" value="${fn:split(gateAvailabilityProp,'=')}"/>
                        <c:if test="${fn:toLowerCase(isPaymentGateNotAvailable[0]) == 'yes'}">



            $("#opener_${fn:toLowerCase(paymentTypeImage)}").click(function () {
            $("#dialog").dialog("option", "position", 'center');
            $("#dialog").dialog("open");
            });
                        </c:if>
                    </c:if>

                </c:forEach>
            </c:forEach>

            }

            $(dialogStartUps);</script>


    </stripes:layout-component>
    <stripes:layout-component name="contents">
        
        <%--
            *****************************************************************************************************************************
            * This page is a bit complex, but all it does is to display payment options when you click 'Recharge' menu option on MySmile*
            *****************************************************************************************************************************
        
            The page is rendered as per the following logic:
                1. Get the value of the property 'env.scp.paymentgateway.makepayment.allowed.customers'
                2. The value of property in 1 determines who is allowed to recharge using the payment options stored in property 'env.scp.payment.methods' 
                    2.1 If the property's (env.scp.paymentgateway.makepayment.allowed.customers) value is NOT EMPTY, then only the customer's whos SEP ID is set on this property can use the configured payment options* as per property 'env.scp.payment.methods'
                    2.2 If the property value is EMPTY then all the payment options are available to all SCP users.
                3. The value for property 'env.scp.payment.methods' is a set of available payment options to be displayed on the page, each entry is a list of tokens delimited by the PIPE (|) character
                    3.1 The number of tokens for each entry is not limited, currently we defined six (6) tokens. Can be less/more depending on the customisation needed
                    3.2 Some tokens are standalone and some are AVPs: E.g, in the entry "YoPayments|SCA|env.scp.yopayments.partner.integration.config|AllowedCustomerIdOnGateway=9995|PaymentOptionsDisplayPreference=Horizontal_ImageFirst_WithGatewayLogo|ImageLogo=yo.png" ImageLogo=yo.png is an AVP
                    3.3 The order of token positions is VERY IMPORTANT, the order can be described as follows:
                                                                                                                1=Payment option
                                                                                                                2=One of this options: SCA|LINK|WALLET|TPGW
                                                                                                                3=Property name in the format 'env.scp.XXXXX.partner.integration.config'
                                                                                                                4=AllowedCustomerIdOnGateway
                                                                                                                5=PaymentOptionsDisplayPreference
                                                                                                                6=ImageLogo
                    3.4 The definition of tokens is as follows:
                                                                    1=Payment option name e.g 'Gemex', identifier for the payment option/gateway
                                                                    2=Options: SCA means the payment gateway option is integrated with POS
                                                                               LINK means its an external link which will take you out of SCP to the partners website
                                                                               WALLET means the Smile airtime
                                                                               TPGW means TPGW currently not implemented
                                                                    3=Property that stores more configuration information for the payment option e.g. 'env.scp.gemex.partner.integration.config'
                                                                    4=This is an AVP token that determines who can use this specific payment option;
                                                                                        AllowedCustomerIdOnGateway=All means everyone has access to it
                                                                                        AllowedCustomerIdOnGateway=9995,7778 means only customers with SEP ID 9995,7778 can use this payment option
                                                                    5=This an AVP token which determines the display arrangement of the services provided via this payment option; Such preference is determined opcos, available options are as below:
                                                                                        PaymentOptionsDisplayPreference=Horizontal_ImageFirst
                                                                                        PaymentOptionsDisplayPreference=Horizontal_ImageFirst_WithGatewayLogo
                                                                                        PaymentOptionsDisplayPreference=Horizontal_ImageLast
                                                                                        PaymentOptionsDisplayPreference=Horizontal_ImageLastWithGatewayLogo
                                                                                        PaymentOptionsDisplayPreference=Vertical
                                                                    6=Logo for payment gateway
        --%>
        <div style="margin-top: 10px;" class="sixteen columns">
            <stripes:form action="/Account.action" autocomplete="off">

                <table border="0" width="100%">
                    <c:choose>
                        <c:when test="${s:getListSize(s:getPropertyAsList('env.scp.paymentgateway.makepayment.allowed.customers')) > 0 }"><%--Enter this block if this property is set with customer's IDs--%>
                            <c:set var="customerIdIsSet" value="false"/>
                            <c:forEach items="${s:getPropertyAsList('env.scp.paymentgateway.makepayment.allowed.customers')}" var="customerId" varStatus="loop">
                                <c:if test="${customerId eq actionBean.customer.customerId}">
                                    <c:set var="customerIdIsSet" value="true"/>
                                </c:if>
                            </c:forEach>
                            <c:if test="${customerIdIsSet}"><%--The customer in this session has access to all payment options--%>

                                <c:forEach items="${s:getPropertyAsList('env.scp.payment.methods')}" var="paymentTypeProps" varStatus="loopP">
                                    <c:set var="ptProps" value="${fn:split(paymentTypeProps,'|')}"/>
                                    <c:set var="paymentType" value="${ptProps[0]}"/>
                                    <c:set var="paymentTypeIntegration" value="${ptProps[1]}"/>
                                    <c:set var="paymentTypeConfigs" value="${ptProps[2]}"/>

                                    <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="paymentTypeProps1" varStatus="loopConf">
                                        <c:set var="ptConfigProps" value="${fn:split(paymentTypeProps1,'|')}"/>
                                        <c:set var="paymentTypeImage" value="${ptConfigProps[0]}"/>
                                        <c:set var="imageFileType" value="${ptConfigProps[1]}"/>
                                        <tr>
                                            <td class="offset-by-five"width="33%">

                                                <table width="100%" style="border: none;">
                                                    <c:set var="urlLink" value="unknown"/>
                                                    <c:if test="${paymentTypeIntegration == 'LINK'}">
                                                        <c:set var="urlLink" value="${ptConfigProps[2]}"/>
                                                    </c:if>
                                                    <tr>
                                                        <td colspan="2">
                                                            <br class="clear" />
                                                        </td>
                                                    </tr>
                                                    <tr style="border: none;">
                                                        <td width="10%" style="border: none;">
                                                            <input type="radio"   name="gatewayCode" value="${paymentType}" style="vertical-align:middle" checked ng-init="proceedGatewayCode_${paymentTypeImage} = ''" ng-model="proceedGatewayCode_${paymentTypeImage}" ng-click="enableProceedButtonByType('${paymentTypeIntegration}', '${urlLink}')"/>
                                                        </td>
                                                        <td width="90%" style="border: none;">
                                                            <c:if test="${paymentTypeIntegration == 'WALLET'}">
                                                                <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20" style="display:inline;"/>
                                                            </c:if>
                                                            <c:if test="${paymentTypeIntegration != 'WALLET'}">
                                                                <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/>
                                                            </c:if>
                                                        </td>
                                                    </tr>

                                                </table>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:forEach>

                            </c:if>
                            <c:if test="${!customerIdIsSet}"><%--The customer in this session does not have access to all payment options, display the DEFAULT ones--%>

                                <c:forEach items="${s:getPropertyAsList('env.scp.payment.methods')}" var="paymentTypeProps" varStatus="loopP">
                                    <c:set var="ptProps" value="${fn:split(paymentTypeProps,'|')}"/>
                                    <c:set var="paymentType" value="${ptProps[0]}"/>
                                    <c:set var="paymentTypeIntegration" value="${ptProps[1]}"/>
                                    <c:set var="paymentTypeConfigs" value="${ptProps[2]}"/>

                                    <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="paymentTypeProps1" varStatus="loopConf">
                                        <c:set var="ptConfigProps" value="${fn:split(paymentTypeProps1,'|')}"/>
                                        <c:set var="paymentTypeImage" value="${ptConfigProps[0]}"/>
                                        <c:set var="imageFileType" value="${ptConfigProps[1]}"/>
                                        <c:if test="${paymentTypeIntegration == 'LINK' || paymentTypeIntegration == 'WALLET'}">
                                            <tr>
                                                <td class="offset-by-five"width="33%">
                                                    <table width="100%" style="border: none;">
                                                        <c:set var="urlLink" value="unknown"/>
                                                        <c:if test="${paymentTypeIntegration == 'LINK'}">
                                                            <c:set var="urlLink" value="${ptConfigProps[2]}"/>
                                                        </c:if>
                                                        <tr>
                                                            <td colspan="2">
                                                                <br class="clear" />
                                                            </td>
                                                        </tr>
                                                        <tr style="border: none;">
                                                            <td width="10%" style="border: none;">
                                                                <input type="radio"   name="gatewayCode" value="${paymentType}" style="vertical-align:middle" checked ng-init="proceedGatewayCode_${paymentTypeImage} = ''" ng-model="proceedGatewayCode_${paymentTypeImage}" ng-click="enableProceedButtonByType('${paymentTypeIntegration}', '${urlLink}')"/>
                                                            </td>
                                                            <td width="90%" style="border: none;">
                                                                <c:if test="${paymentTypeIntegration == 'WALLET'}">
                                                                    <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/> <span><fmt:message key="scp.smile.airtime"/></span>
                                                                </c:if>
                                                                <c:if test="${paymentTypeIntegration == 'LINK'}">
                                                                    <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/>
                                                                </c:if>
                                                            </td>
                                                        </tr>

                                                    </table>
                                                </td>
                                            </tr>
                                        </c:if>
                                    </c:forEach>
                                </c:forEach>


                            </c:if>
                        </c:when>


                        <c:otherwise><%--Enter this block to give all customers access to all available payment options on MySmile--%>

                            <c:forEach items="${s:getPropertyAsList('env.scp.payment.methods')}" var="paymentTypeProps" varStatus="loopP"><%--List of all currently configured payment options for MySmile recharge--%>

                                <tr>
                                    <td class="offset-by-five"width="33%">
                                        <br/>
                                    </td>
                                </tr>
                                <c:set var="ptProps" value="${fn:split(paymentTypeProps,'|')}"/><%--E.g Gemex|SCA|env.scp.gemex.partner.integration.config|AllowedCustomerIdOnGateway=9995,679755,692207|PaymentOptionsDisplayPreference=Horizontal_ImageFirst--%>
                                <c:set var="paymentType" value="${ptProps[0]}"/><%--Gateway name as per implementation on SCA, e.g 'Gemex'--%>
                                <c:set var="paymentTypeIntegration" value="${ptProps[1]}"/><%--One of these options: 'WALLET', 'LINK', 'SCA', 'TPGW'--%>
                                <c:set var="paymentTypeConfigs" value="${ptProps[2]}"/><%--Property holding more configuration information for the payment gateway e.g 'env.scp.gemex.partner.integration.config' --%>

                                <c:set var="AllowedCustomerIdOnGatewayConfig" value="AllowedCustomerIdOnGateway=All"/> 
                                <c:set var="PaymentOptionsDisplayPreference" value="PaymentOptionsDisplayPreference=Vertical"/>
                                <c:if test="${paymentTypeIntegration == 'SCA'}">
                                    <c:set var="AllowedCustomerIdOnGatewayConfig" value="${ptProps[3]}"/><%--E.g: AllowedCustomerIdOnGateway=9995,679755,692207. This config checks if payments via this PAYMENT_GATEWAY is limited to specific customers, a value of 'All' means everyone has permissions to use it--%>  
                                    <c:set var="PaymentOptionsDisplayPreference" value="${ptProps[4]}"/><%--E.g: PaymentOptionsDisplayPreference=Horizontal_ImageFirst. Two options available for display preference: Horizontal_ImageFirst--%>
                                </c:if>
                                <c:set var="DisplayPreference" value="${fn:split(PaymentOptionsDisplayPreference,'=')}"/>

                                <c:choose>
                                    <c:when test="${AllowedCustomerIdOnGatewayConfig != 'AllowedCustomerIdOnGateway=All'}"><%--Enter this block as payments via this PAYMENT_GATEWAY is limited to specific customers--%>
                                        <c:set var="AllowedCustomerIdOnGatewayConfigData" value="${fn:split(AllowedCustomerIdOnGatewayConfig,'=')}"/><%--E.g: AllowedCustomerIdOnGateway=9995,679755,692207.--%>
                                        <c:set var="AllowedCustomerIdArray" value="${fn:split(AllowedCustomerIdOnGatewayConfigData[1],',')}"/>
                                        <c:set var="customerIdIsSetOnGateway" value="false"/>

                                        <c:forEach items="${fn:split(AllowedCustomerIdOnGatewayConfigData[1],',')}" var="allowedCustomerId" varStatus="loopGW">
                                            <c:if test="${allowedCustomerId eq actionBean.customer.customerId}"><%--Check if the current customer in session matches customers configured for this payment gateway--%>
                                                <c:set var="customerIdIsSetOnGateway" value="true"/>
                                            </c:if>
                                        </c:forEach>

                                        <c:choose>
                                            <c:when test="${customerIdIsSetOnGateway}"><%--Customer in session matches customers configured for this payment gateway so ENABLE payment gateway for this customer--%>
                                                <c:if test="${DisplayPreference[1] == 'Vertical'}"><%--Display preference is set to VERTICAL--%>    
                                                    <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="paymentTypeProps1" varStatus="loopConf">
                                                        <c:set var="ptConfigProps" value="${fn:split(paymentTypeProps1,'|')}"/>
                                                        <c:set var="paymentTypeImage" value="${ptConfigProps[0]}"/>
                                                        <c:set var="imageFileType" value="${ptConfigProps[1]}"/>
                                                        <tr>
                                                            <td class="offset-by-five"width="33%">
                                                                <table width="100%" style="border: none;">
                                                                    <c:set var="urlLink" value="unknown"/>
                                                                    <c:if test="${paymentTypeIntegration == 'LINK'}">
                                                                        <c:set var="urlLink" value="${ptConfigProps[2]}"/>
                                                                    </c:if>
                                                                    <tr>
                                                                        <td colspan="3">
                                                                            <br class="clear" />
                                                                        </td>
                                                                    </tr>
                                                                    <tr style="border: none;">
                                                                        <td width="5%" style="border: none;">
                                                                            <input type="radio"   name="gatewayCode" value="${paymentType}" style="vertical-align:middle" checked ng-init="proceedGatewayCode_${paymentTypeImage} = ''" ng-model="proceedGatewayCode_${paymentTypeImage}" ng-click="enableProceedButtonByType('${paymentTypeIntegration}', '${urlLink}')"/>
                                                                        </td>

                                                                        <td width="30%" style="border: none;">
                                                                            <c:if test="${paymentTypeIntegration == 'WALLET'}">
                                                                                <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/> <span><fmt:message key="scp.smile.airtime"/></span>
                                                                            </c:if>
                                                                            <c:if test="${paymentTypeIntegration != 'WALLET'}">
                                                                                <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/>
                                                                            </c:if>
                                                                        </td>

                                                                        <td width="65%" style="border: none;">
                                                                            <c:if test="${paymentTypeIntegration == 'SCA'}">
                                                                                <c:set var="gateAvailabilityProp" value="${ptConfigProps[2]}"/>
                                                                                <c:set var="isPaymentGateNotAvailable" value="${fn:split(gateAvailabilityProp,'=')}"/>
                                                                                <c:if test="${fn:toLowerCase(isPaymentGateNotAvailable[0]) == 'yes'}">
                                                                                    <p ng-style="{ color:'red' }">    
                                                                                        <a id="opener_${fn:toLowerCase(paymentTypeImage)}" href="#">
                                                                                            <img style="vertical-align:middle" src="${pageContext.request.contextPath}/images/tmpFiles/information.png"/>
                                                                                        </a>
                                                                                        <fmt:message key="scp.${fn:toLowerCase(isPaymentGateNotAvailable[1])}"/>
                                                                                    </p>
                                                                                </c:if>
                                                                            </c:if>
                                                                        </td>

                                                                    </tr>

                                                                </table>
                                                            </td>
                                                        </tr>
                                                    </c:forEach>
                                                </c:if>
                                                <c:if test="${fn:startsWith(DisplayPreference[1], 'Horizontal')}"><%--Display preference is set to HORIZONTAL--%>    
                                                    <tr>
                                                        <td class="offset-by-five" width="33%">
                                                            <table width="100%" style="border: none;">
                                                                <tr style="border: none;">
                                                                    <c:set var="urlLink" value="unknown"/>
                                                                    <td style="border: none;">
                                                                        <input type="radio"   name="gatewayCode" value="${paymentType}" style="vertical-align:middle" checked ng-init="proceedGatewayCode_${paymentTypeImage} = ''" ng-model="proceedGatewayCode_${paymentTypeImage}" ng-click="enableProceedButtonByType('${paymentTypeIntegration}', '${urlLink}')"/>
                                                                    </td>




                                                                    <td width="70%"style="border: none;">
                                                                        <c:if test="${DisplayPreference[1] == 'Horizontal_ImageFirst_WithGatewayLogo'}">
                                                                            <c:set var="imageLogo" value="${ptProps[5]}"/><%--E.g: ImageLogo=gemex.png--%>
                                                                            <c:set var="logo" value="${fn:split(imageLogo,'=')}"/>
                                                                            <span><fmt:message key="scp.payment.gateway.options.poweredby.${paymentType}"/>&nbsp;<img style="vertical-align:middle" src="images/${logo[1]}" alt="${paymentType}" title="${paymentType} logo" height="20"/>&nbsp;&nbsp;</span>
                                                                        </c:if>
                                                                        <c:if test="${DisplayPreference[1] == 'Horizontal_ImageFirst'}">
                                                                            <span><fmt:message key="scp.payment.gateway.options.poweredby.${paymentType}"/></span>
                                                                        </c:if>
                                                                            <span> (
                                                                        <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="paymentTypeProps1" varStatus="loopConf">
                                                                            <c:set var="ptConfigProps" value="${fn:split(paymentTypeProps1,'|')}"/>
                                                                            <c:set var="paymentTypeImage" value="${ptConfigProps[0]}"/>
                                                                            <c:set var="imageFileType" value="${ptConfigProps[1]}"/>

                                                                            <c:if test="${paymentTypeIntegration == 'LINK'}">
                                                                                <c:set var="urlLink" value="${ptConfigProps[2]}"/>
                                                                            </c:if>
                                                                            <c:if test="${paymentTypeIntegration == 'WALLET'}">
                                                                                <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/> <span><fmt:message key="scp.smile.airtime"/></span>
                                                                            </c:if>
                                                                            <c:if test="${paymentTypeIntegration != 'WALLET'}">
                                                                                <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/>
                                                                            </c:if>
                                                                        </c:forEach>
                                                                                )</span>
                                                                        <c:if test="${DisplayPreference[1] == 'Horizontal_ImageLast_WithGatewayLogo'}">
                                                                            <c:set var="imageLogo" value="${ptProps[5]}"/><%--E.g: ImageLogo=gemex.png--%>
                                                                            <c:set var="logo" value="${fn:split(imageLogo,'=')}"/>
                                                                            <span>&nbsp;&nbsp;<fmt:message key="scp.payment.gateway.options.poweredby.${paymentType}"/>&nbsp;<img style="vertical-align:middle" src="images/${logo[1]}" alt="${paymentType}" title="${paymentType} logo" height="20"/></span>
                                                                        </c:if>
                                                                        <c:if test="${DisplayPreference[1] == 'Horizontal_ImageLast' || DisplayPreference[1] == 'Horizontal'}">
                                                                            <span><fmt:message key="scp.payment.gateway.options.poweredby.${paymentType}"/></span>
                                                                        </c:if>
                                                                    </td>

                                                                    <td width="25%"style="border: none;">
                                                                        <c:if test="${paymentTypeIntegration == 'SCA'}">
                                                                            <c:set var="counter" value="${1}" />
                                                                            <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="paymentTypeProps1" varStatus="loopConf">
                                                                                <c:set var="ptConfigProps" value="${fn:split(paymentTypeProps1,'|')}"/>
                                                                                <c:set var="gateAvailabilityProp" value="${ptConfigProps[2]}"/>
                                                                                <c:set var="paymentTypeImage" value="${ptConfigProps[0]}"/>
                                                                                <c:set var="isPaymentGateNotAvailable" value="${fn:split(gateAvailabilityProp,'=')}"/>
                                                                                <c:if test="${fn:toLowerCase(isPaymentGateNotAvailable[0]) == 'yes'}">
                                                                                    <p ng-style="{ color:'red' }">    
                                                                                        <a id="opener_${fn:toLowerCase(paymentTypeImage)}" href="#">
                                                                                            <img style="vertical-align:middle" src="${pageContext.request.contextPath}/images/tmpFiles/information.png"/>
                                                                                        </a>
                                                                                        <fmt:message key="scp.${fn:toLowerCase(isPaymentGateNotAvailable[1])}"/>
                                                                                    </p>
                                                                                </c:if>
                                                                            </c:forEach>
                                                                        </c:if>
                                                                    </td>
                                                                </tr>
                                                            </table>
                                                        </td>
                                                    </tr>
                                                </c:if>
                                            </c:when>
                                            <c:otherwise><%--Customer in session does not match customers configured for this payment gateway so DISABLE payment gateway for this customer and allow DEFAULT options--%>

                                                <c:if test="${paymentTypeIntegration == 'LINK' || paymentTypeIntegration == 'WALLET'}">
                                                    <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="paymentTypeProps1" varStatus="loopConf">
                                                        <c:set var="ptConfigProps" value="${fn:split(paymentTypeProps1,'|')}"/>
                                                        <c:set var="paymentTypeImage" value="${ptConfigProps[0]}"/>
                                                        <c:set var="imageFileType" value="${ptConfigProps[1]}"/>
                                                        <tr>
                                                            <td class="offset-by-five"width="33%">
                                                                <table width="100%" style="border: none;">
                                                                    <c:set var="urlLink" value="unknown"/>
                                                                    <c:if test="${paymentTypeIntegration == 'LINK'}">
                                                                        <c:set var="urlLink" value="${ptConfigProps[2]}"/>
                                                                    </c:if>
                                                                    <tr>
                                                                        <td colspan="3">
                                                                            <br class="clear" />
                                                                        </td>
                                                                    </tr>
                                                                    <tr style="border: none;">
                                                                        <td width="5%" style="border: none;">
                                                                            <input type="radio"   name="gatewayCode" value="${paymentType}" style="vertical-align:middle" checked ng-init="proceedGatewayCode_${paymentTypeImage} = ''" ng-model="proceedGatewayCode_${paymentTypeImage}" ng-click="enableProceedButtonByType('${paymentTypeIntegration}', '${urlLink}')"/>
                                                                        </td>

                                                                        <td width="30%" style="border: none;">
                                                                            <c:if test="${paymentTypeIntegration == 'WALLET'}">
                                                                                <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20" style="vertical-align:middle"/> <span><fmt:message key="scp.smile.airtime"/></span>
                                                                            </c:if>
                                                                            <c:if test="${paymentTypeIntegration != 'WALLET'}">
                                                                                <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/>
                                                                            </c:if>
                                                                        </td>

                                                                        <td width="65%" style="border: none;">
                                                                            <c:if test="${paymentTypeIntegration == 'SCA'}">
                                                                                <c:set var="gateAvailabilityProp" value="${ptConfigProps[2]}"/>
                                                                                <c:set var="isPaymentGateNotAvailable" value="${fn:split(gateAvailabilityProp,'=')}"/>
                                                                                <c:if test="${fn:toLowerCase(isPaymentGateNotAvailable[0]) == 'yes'}">
                                                                                    <p ng-style="{ color:'red' }">    
                                                                                        <a id="opener_${fn:toLowerCase(paymentTypeImage)}" href="#">
                                                                                            <img style="vertical-align:middle" src="${pageContext.request.contextPath}/images/tmpFiles/information.png"/>
                                                                                        </a>
                                                                                        <fmt:message key="scp.${fn:toLowerCase(isPaymentGateNotAvailable[1])}"/>
                                                                                    </p>
                                                                                </c:if>
                                                                            </c:if>
                                                                        </td>

                                                                    </tr>

                                                                </table>
                                                            </td>
                                                        </tr>
                                                    </c:forEach>
                                                </c:if>

                                            </c:otherwise>
                                        </c:choose>


                                    </c:when>
                                    <c:otherwise><%--Enter this block as payments via this PAYMENT_GATEWAY is enabled for all MySmile customers or this part of the DEFAULT payment options available on MySmile--%>

                                        <c:if test="${DisplayPreference[1] == 'Vertical'}"><%--Display preference is set to VERTICAL--%>    
                                            <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="paymentTypeProps1" varStatus="loopConf">
                                                <c:set var="ptConfigProps" value="${fn:split(paymentTypeProps1,'|')}"/>
                                                <c:set var="paymentTypeImage" value="${ptConfigProps[0]}"/>
                                                <c:set var="imageFileType" value="${ptConfigProps[1]}"/>
                                                <tr>
                                                    <td class="offset-by-five"width="33%">
                                                        <table width="100%" style="border: none;">
                                                            <c:set var="urlLink" value="unknown"/>
                                                            <c:if test="${paymentTypeIntegration == 'LINK'}">
                                                                <c:set var="urlLink" value="${ptConfigProps[2]}"/>
                                                            </c:if>
                                                            <tr>
                                                                <td colspan="3">
                                                                    <br class="clear" />
                                                                </td>
                                                            </tr>
                                                            <tr style="border: none;">
                                                                <td width="5%" style="border: none;">
                                                                    <input type="radio"   name="gatewayCode" value="${paymentType}" style="vertical-align:middle" checked ng-init="proceedGatewayCode_${paymentTypeImage} = ''" ng-model="proceedGatewayCode_${paymentTypeImage}" ng-click="enableProceedButtonByType('${paymentTypeIntegration}', '${urlLink}')"/>
                                                                </td>

                                                                <td width="30%" style="border: none;">
                                                                    <c:if test="${paymentTypeIntegration == 'WALLET'}">
                                                                        <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/> <span><fmt:message key="scp.smile.airtime"/></span>
                                                                    </c:if>
                                                                    <c:if test="${paymentTypeIntegration != 'WALLET'}">
                                                                        <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/>
                                                                    </c:if>
                                                                </td>

                                                                <td width="65%" style="border: none;">
                                                                    <c:if test="${paymentTypeIntegration == 'SCA'}">
                                                                        <c:set var="gateAvailabilityProp" value="${ptConfigProps[2]}"/>
                                                                        <c:set var="isPaymentGateNotAvailable" value="${fn:split(gateAvailabilityProp,'=')}"/>
                                                                        <c:if test="${fn:toLowerCase(isPaymentGateNotAvailable[0]) == 'yes'}">
                                                                            <p ng-style="{ color:'red' }">    
                                                                                <a id="opener_${fn:toLowerCase(paymentTypeImage)}" href="#">
                                                                                    <img style="vertical-align:middle" src="${pageContext.request.contextPath}/images/tmpFiles/information.png"/>
                                                                                </a>
                                                                                <fmt:message key="scp.${fn:toLowerCase(isPaymentGateNotAvailable[1])}"/>
                                                                            </p>
                                                                        </c:if>
                                                                    </c:if>
                                                                </td>

                                                            </tr>

                                                        </table>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </c:if>
                                        <c:if test="${fn:startsWith(DisplayPreference[1], 'Horizontal')}"><%--Display preference is set to HORIZONTAL--%>    
                                            <tr>
                                                <td class="offset-by-five" width="33%">
                                                    <table width="100%" style="border: none;">
                                                        <tr style="border: none;">
                                                            <c:set var="urlLink" value="unknown"/>
                                                            <td style="border: none;">
                                                                <input type="radio"   name="gatewayCode" value="${paymentType}" style="vertical-align:middle" checked ng-init="proceedGatewayCode_${paymentTypeImage} = ''" ng-model="proceedGatewayCode_${paymentTypeImage}" ng-click="enableProceedButtonByType('${paymentTypeIntegration}', '${urlLink}')"/>
                                                            </td>


                                                            <td width="70%"style="border: none;">
                                                                <c:if test="${DisplayPreference[1] == 'Horizontal_ImageFirst'}">
                                                                    <span><fmt:message key="scp.payment.gateway.options.poweredby.${paymentType}"/></span>
                                                                </c:if>
                                                                <c:if test="${DisplayPreference[1] == 'Horizontal_ImageFirst_WithGatewayLogo'}">
                                                                    <c:set var="imageLogo" value="${ptProps[5]}"/><%--E.g: ImageLogo=gemex.png--%>
                                                                    <c:set var="logo" value="${fn:split(imageLogo,'=')}"/>
                                                                    <span><fmt:message key="scp.payment.gateway.options.poweredby.${paymentType}"/>&nbsp;<img style="vertical-align:middle" src="images/${logo[1]}" alt="${paymentType}" title="${paymentType} logo" height="20"/>&nbsp;&nbsp;</span>
                                                                </c:if>
                                                                    <span> (   
                                                                <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="paymentTypeProps1" varStatus="loopConf">
                                                                    <c:set var="ptConfigProps" value="${fn:split(paymentTypeProps1,'|')}"/>
                                                                    <c:set var="paymentTypeImage" value="${ptConfigProps[0]}"/>
                                                                    <c:set var="imageFileType" value="${ptConfigProps[1]}"/>

                                                                    <c:if test="${paymentTypeIntegration == 'LINK'}">
                                                                        <c:set var="urlLink" value="${ptConfigProps[2]}"/>
                                                                    </c:if>
                                                                    <c:if test="${paymentTypeIntegration == 'WALLET'}">
                                                                        <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/> <span><fmt:message key="scp.smile.airtime"/></span>
                                                                    </c:if>
                                                                    <c:if test="${paymentTypeIntegration != 'WALLET'}">
                                                                        <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/>
                                                                    </c:if>
                                                                </c:forEach>
                                                                      )  </span>
                                                                <c:if test="${DisplayPreference[1] == 'Horizontal_ImageLast_WithGatewayLogo'}">
                                                                    <c:set var="imageLogo" value="${ptProps[5]}"/><%--E.g: ImageLogo=gemex.png--%>
                                                                    <c:set var="logo" value="${fn:split(imageLogo,'=')}"/>
                                                                    <span>&nbsp;&nbsp;<fmt:message key="scp.payment.gateway.options.poweredby.${paymentType}"/>&nbsp;<img style="vertical-align:middle" src="images/${logo[1]}" alt="${paymentType}" title="${paymentType} logo" height="20"/></span>
                                                                </c:if>
                                                                    
                                                                <c:if test="${DisplayPreference[1] == 'Horizontal_ImageLast' || DisplayPreference[1] == 'Horizontal'}">
                                                                    <span><fmt:message key="scp.payment.gateway.options.poweredby.${paymentType}"/></span>
                                                                </c:if>

                                                            </td>

                                                            <td width="25%"style="border: none;">
                                                                <c:if test="${paymentTypeIntegration == 'SCA'}">
                                                                    <c:set var="counter" value="${1}" />
                                                                    <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="paymentTypeProps1" varStatus="loopConf">
                                                                        <c:set var="ptConfigProps" value="${fn:split(paymentTypeProps1,'|')}"/>
                                                                        <c:set var="gateAvailabilityProp" value="${ptConfigProps[2]}"/>
                                                                        <c:set var="paymentTypeImage" value="${ptConfigProps[0]}"/>
                                                                        <c:set var="isPaymentGateNotAvailable" value="${fn:split(gateAvailabilityProp,'=')}"/>
                                                                        <c:if test="${fn:toLowerCase(isPaymentGateNotAvailable[0]) == 'yes'}">
                                                                            <p ng-style="{ color:'red' }">    
                                                                                <a id="opener_${fn:toLowerCase(paymentTypeImage)}" href="#">
                                                                                    <img src="${pageContext.request.contextPath}/images/tmpFiles/information.png"/>
                                                                                </a>
                                                                                <fmt:message key="scp.${fn:toLowerCase(isPaymentGateNotAvailable[1])}"/>
                                                                            </p>
                                                                        </c:if>
                                                                    </c:forEach>
                                                                </c:if>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                        </c:if>
                                    </c:otherwise>
                                </c:choose>

                            </c:forEach><%--End of listing all currently configured payment options for MySmile recharge--%>

                        </c:otherwise>
                    </c:choose>

                </table>

                <table border="0" cellpadding="0" cellspacing="0" width="100%">
                    <tr>
                        <td colspan="3">
                            <br class="clear" />
                        </td>
                    </tr>

                    <tr>
                        <td colspan="3">
                            <br class="clear" />
                        </td>
                    </tr>

                    <tr>

                        <td ng-show="displayProceedButton" class="offset-by-five">
                            <div ng-show="otherTypesSelected">
                                <input type="submit" name="showAddUnitCredits" class="button_proceed" value="Proceed"/>
                            </div>
                            <div ng-show="urlLinkSelected">
                                <a href="{{paymentUrlLink}}" class="link_proceed" target="_blank">Proceed</a>
                            </div>
                        </td>
                    </tr>
                </table>


            </stripes:form>



            <div id="dialog" title="Warning">
                <p><fmt:message key="scp.paymentgateway.notavailable"/></p>
            </div>

        </div>
    </stripes:layout-component>

</stripes:layout-render>


