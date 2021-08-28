<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="../include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="scp.payment.gateway.confirmation.page.title"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function () {
                makeMenuActive('Buy Smile Bundle');
            }   
            
            function getRechargePhoneNumber(mobileMoneyOperator) {
                document.getElementById('mobileMoneyProcessor').value=mobileMoneyOperator;
                document.getElementById('phoneNumberDiv').style.display='block';                
                document.getElementById('yoPaymentsProceedBtn').style.display='block';                
            }
            
            function enableYoProceedButton() {
                document.getElementById('yoPaymentsProceedBtn').style.display='block';                
            }
            
        </script>

    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <div style="margin-top: 10px;" class="sixteen columns">
            <stripes:form action="/Account.action" autocomplete="off">
                <c:set var="isAirtimeTransaction" value="false"/>
                <c:if test="${!empty actionBean.purchaseUnitCreditLine.unitCreditSpecificationId && actionBean.purchaseUnitCreditLine.unitCreditSpecificationId > 0}">
                    <c:set var="ucs" value="${s:getUnitCreditSpecification(actionBean.purchaseUnitCreditLine.unitCreditSpecificationId)}"/>
                    <stripes:hidden name="purchaseUnitCreditLine.unitCreditSpecificationId" value="${actionBean.purchaseUnitCreditLine.unitCreditSpecificationId}"/>
                </c:if>
                <c:choose>
                    <c:when test="${!empty actionBean.purchaseUnitCreditRequest.unitCreditSpecificationId && actionBean.purchaseUnitCreditRequest.unitCreditSpecificationId > 0}">
                        <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/>
                        <stripes:hidden name="purchaseUnitCreditRequest.accountId" value="${actionBean.account.accountId}"/>
                        <stripes:hidden name="purchaseUnitCreditRequest.unitCreditSpecificationId" value="${actionBean.unitCreditSpecification.unitCreditSpecificationId}"/>
                        <input type="hidden" name="purchaseUnitCreditRequest.productInstanceId" value="${actionBean.purchaseUnitCreditRequest.productInstanceId}"/>
                        <input type="hidden" name="sale.recipientCustomerId" value="${actionBean.customer.customerId}"/>
                        <input type="hidden" name="sale.recipientAccountId" value="${actionBean.account.accountId}"/>
                    </c:when>
                    <c:otherwise>
                        <c:set var="isAirtimeTransaction" value="true"/>
                        <input type="hidden" name="sale.recipientCustomerId" value="${actionBean.customer.customerId}"/>
                        <input type="hidden" name="sale.saleTotalCentsIncl" value="${s:convertCentsToLong(actionBean.sale.saleTotalCentsIncl)}"/>
                        <input type="hidden" name="sale.recipientAccountId" value="${actionBean.account.accountId}"/>
                        <c:forEach items="${actionBean.sale.saleLines}" var="line" varStatus="loop"> 
                            <input type="hidden" name="sale.saleLines[${loop.index}].lineTotalCentsIncl" value="${s:convertCentsToLong(line.lineTotalCentsIncl)}"/>
                            <input type="hidden" name="sale.saleLines[${loop.index}].lineNumber" value="${line.lineNumber}"/>
                            <input type="hidden" name="sale.saleLines[${loop.index}].inventoryItem.itemNumber" value="${line.inventoryItem.itemNumber}"/>
                            <input type="hidden" name="sale.saleLines[${loop.index}].inventoryItem.serialNumber" value="${line.inventoryItem.serialNumber}"/>
                            <input type="hidden" name="sale.saleLines[${loop.index}].inventoryItem.description" value="${line.inventoryItem.description}"/>
                            <input type="hidden" name="sale.saleLines[${loop.index}].quantity" value="${line.quantity}"/>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>


                <table border="0" cellpadding="0" cellspacing="0">
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.rechargeportal.smile.acc.number"/>:</strong>
                        </td>
                        <td>
                            ${actionBean.account.accountId}
                        </td>
                    </tr>

                    <tr>
                        <td>
                            <strong><fmt:message key="scp.rechargeportal.smile.customer.email"/>:&nbsp;</strong>
                        </td>
                        <td>
                            ${actionBean.customer.emailAddress}
                            <input type="hidden" name="scp.email" value="${actionBean.customer.emailAddress}"/>
                        </td>
                    </tr>
                </table>

                <div class="row"></div>

                <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color:lightgrey">
                    <tr style="background-color: #A7B2BB;">
                        <td colspan="2">
                            <strong>&nbsp;Order Details</strong> 
                        </td>
                        <td class="offset-by-three">
                            <strong><fmt:message key="scp.rechargeportal.smile.reg.number"/></strong>
                        </td>
                    </tr>

                    <tr>
                        <td colspan="2">
                            <c:if test="${!isAirtimeTransaction}">
                                &nbsp;${actionBean.unitCreditSpecification.name}
                            </c:if>
                            <c:if test="${isAirtimeTransaction}">
                                &nbsp;SmileAirtime
                            </c:if>
                        </td>
                        <td class="offset-by-three">
                            <c:if test="${!isAirtimeTransaction}">
                                ${s:convertCentsToCurrencyShortWithCommaGroupingSeparator(actionBean.unitCreditSpecification.priceInCents)}
                            </c:if>
                            <c:if test="${isAirtimeTransaction}">
                                ${s:convertCentsToCurrencyShortWithCommaGroupingSeparator(actionBean.sale.saleTotalCentsIncl)}
                            </c:if>
                        </td>
                    </tr>
                    <c:if test="${!empty actionBean.purchaseUnitCreditLine.unitCreditSpecificationId && actionBean.purchaseUnitCreditLine.unitCreditSpecificationId > 0}">
                        <tr>
                            <td colspan="2">
                                    &nbsp;${ucs.name}
                            </td>
                            <td class="offset-by-three">
                                    ${s:convertCentsToCurrencyShortWithCommaGroupingSeparator(ucs.priceInCents)}
                            </td>
                        </tr>
                    </c:if>

                    <tr>
                        <td colspan="2">
                            &nbsp;Processing Fees
                        </td>
                        <td class="offset-by-three">
                            ${s:convertCentsToCurrencyShortWithCommaGroupingSeparator(0.00)}
                        </td>
                    </tr>
                    <tr style="background-color: #A7B2BB;">
                        <td colspan="3">
                            <br class="clear" />
                        </td>
                    </tr>

                    <tr style="background-color: #A7B2BB;">
                        <td colspan="2">
                            <strong>&nbsp;Total Amount Payable</strong>
                        </td>
                        <td class="offset-by-three">
                            <strong>
                                <c:if test="${!isAirtimeTransaction}">
                                    <c:choose>
                                        <c:when test="${!empty actionBean.purchaseUnitCreditLine.unitCreditSpecificationId && actionBean.purchaseUnitCreditLine.unitCreditSpecificationId > 0}">
                                            ${s:convertCentsToCurrencyShortWithCommaGroupingSeparator(actionBean.unitCreditSpecification.priceInCents + ucs.priceInCents)} 
                                        </c:when>
                                        <c:otherwise>
                                           ${s:convertCentsToCurrencyShortWithCommaGroupingSeparator(actionBean.unitCreditSpecification.priceInCents)} 
                                        </c:otherwise>
                                    </c:choose>
                                </c:if>
                                <c:if test="${isAirtimeTransaction}">
                                    ${s:convertCentsToCurrencyShortWithCommaGroupingSeparator(actionBean.sale.saleTotalCentsIncl)}
                                </c:if>
                            </strong>
                        </td>
                    </tr>
                    <tr style="background-color: #A7B2BB;">
                        <td colspan="3">
                            <br class="clear" />
                        </td>
                    </tr>


                </table>


                <br/>

                <table class="greentbl" border="0" width="100%">
                    <tr>
                        <th colspan="9" align="center" style="padding: 2px; background-color: #86919A; color:#fff; font-family: UniversLTCn; font-size: 18px; font-weight: bold;"><fmt:message key="scp.recharge.bundle.payby.title"/></th>
                    </tr>


                    <c:choose>
                        <c:when test="${s:getListSize(s:getPropertyAsList('env.scp.paymentgateway.makepayment.allowed.customers')) > 0 }">
                            <c:set var="customerIdIsSet" value="false"/>
                            <c:forEach items="${s:getPropertyAsList('env.scp.paymentgateway.makepayment.allowed.customers')}" var="customerId" varStatus="loop">
                                <c:if test="${customerId eq actionBean.customer.customerId}">
                                    <c:set var="customerIdIsSet" value="true"/>
                                </c:if>
                            </c:forEach>
                            <c:if test="${customerIdIsSet}">
                                <c:forEach items="${s:getPropertyAsList('env.scp.payment.methods')}" var="paymentTypeProps" varStatus="loopP">
                                    <c:set var="ptProps" value="${fn:split(paymentTypeProps,'|')}"/>
                                    <c:set var="paymentTypeIntegration" value="${ptProps[1]}"/>
                                    <c:set var="paymentType" value="${ptProps[0]}"/>
                                    <c:if test="${paymentType != 'Wallet' && paymentTypeIntegration != 'LINK'}">
                                        <tr>
                                            <td width="33%">
                                                <table width="100%" style="border: none;">
                                                    <tr style="border: none;">
                                                        <td width="3%" style="border: none;">
                                                            <c:if test="${isAirtimeTransaction}">
                                                                <stripes:radio   name="sale.paymentGatewayCode" value="${paymentType}" ng-model="proceed_${paymentType}" ng-change="enableProceedButton()"/>
                                                            </c:if>
                                                            <c:if test="${!isAirtimeTransaction}">
                                                                <stripes:radio   name="gatewayCode" value="${paymentType}" ng-model="proceed_${paymentType}" ng-change="enableProceedButton()"/>
                                                            </c:if>
                                                        </td>
                                                        <td width="45%" style="border: none;">
                                                            <fmt:message key="scp.recharge.bundle.payby.${paymentType}"/>
                                                        </td>
                                                        <td width="72%" style="border: none;">

                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                    </c:if>
                                </c:forEach>

                            </c:if>
                            <c:if test="${!customerIdIsSet}">

                            </c:if>
                        </c:when>


                        <c:otherwise>

                            <c:forEach items="${s:getPropertyAsList('env.scp.payment.methods')}" var="paymentTypeProps" varStatus="loopP">
                                
                                <c:set var="ptProps" value="${fn:split(paymentTypeProps, '|')}"/> <%--E.g Gemex|SCA|env.scp.gemex.partner.integration.config|AllowedCustomerIdOnGateway=9995,679755,692207|PaymentOptionsDisplayPreference=Horizontal_ImageFirst--%>
                                
                                <c:set var="paymentType" value="${ptProps[0]}"/> <%--Gateway name as per implementation on SCA, e.g 'Gemex'--%>
                                
                                <c:set var="paymentTypeIntegration" value="${ptProps[1]}"/> <%--One of these options: 'WALLET', 'LINK', 'SCA', 'TPGW'--%>
                                
                                <c:set var="paymentTypeConfigs" value="${ptProps[2]}"/><%--Property holding EXTRA configuration information for the payment gateway e.g 'env.scp.gemex.partner.integration.config' --%>

                                <c:set var="AllowedCustomerIdOnGatewayConfig" value="AllowedCustomerIdOnGateway=All"/>
                                
                                <c:if test="${paymentTypeIntegration == 'SCA'}">
                                    <c:set var="AllowedCustomerIdOnGatewayConfig" value="${ptProps[3]}"/><%--E.g: AllowedCustomerIdOnGateway=9995,679755,692207. This config checks if payments via this PAYMENT_GATEWAY is limited to specific customers, a value of 'All' means everyone has permissions to use it--%>  
                                    <c:set var="PaymentOptionsDisplayPreference" value="${ptProps[4]}"/><%--E.g: PaymentOptionsDisplayPreference=Horizontal_ImageFirst. Two options available for display preference: Horizontal_ImageFirst--%>
                                    <c:set var="paymentTypePropsItems" value="${s:getPropertyAsList(paymentTypeConfigs)}"/>
                                </c:if>
                                  
                                <c:if test="${paymentTypeIntegration == 'LINKSS'}">
                                    <c:set var="PaymentOptionsDisplayPreference" value=""/>
                                </c:if>
                                
                                <c:set var="DisplayPreference" value="${fn:split(PaymentOptionsDisplayPreference, '=')}"/>
                                
                                <c:set var="isAllowedToPurchaseViaGateway" value="false"/>
                                
                                
                                <%-- START :: CHECK IF CUSTOMER IS ALLOWED TO USE THIS SPECIFIC PAYMENT GATEWAY--%>
                                    
                                <c:if test="${AllowedCustomerIdOnGatewayConfig == 'AllowedCustomerIdOnGateway=All'}">
                                    <c:set var="isAllowedToPurchaseViaGateway" value="true"/>
                                </c:if>
                                    
                                <c:if test="${AllowedCustomerIdOnGatewayConfig != 'AllowedCustomerIdOnGateway=All'}">
                                    <c:set var="AllowedCustomerIdOnGatewayConfigData" value="${fn:split(AllowedCustomerIdOnGatewayConfig, '=')}"/>
                                    <c:set var="AllowedCustomerIdArray" value="${fn:split(AllowedCustomerIdOnGatewayConfigData[1], ',')}"/>
                                    <c:forEach items="${fn:split(AllowedCustomerIdOnGatewayConfigData[1], ',')}" var="allowedCustomerId" varStatus="loopGW">
                                        <c:if test="${allowedCustomerId eq actionBean.customer.customerId}">
                                            <c:set var="isAllowedToPurchaseViaGateway" value="true"/>
                                        </c:if>
                                    </c:forEach>
                                </c:if>

                                <%-- END :: CHECK IF CUSTOMER IS ALLOWED TO USE THIS SPECIFIC PAYMENT GATEWAY--%>
                                    
                                    
                                    <c:if test="${loopP.count == 1 || loopP.count == 4 || loopP.count == 7 || loopP.count == 10 || loopP.count == 13}">
                                        <tr width="99%">
                                    </c:if>
                                        <%--<tr>--%>
                                        
                                        <%-- START :: DISPLAY PAYMENT GATEWAY OPTION IF ALLOWED--%>
                                    
                                        <c:if test="${isAllowedToPurchaseViaGateway}">
                                            <td width="33%">
                                                
                                                <table  cellpadding="0" cellspacing="0" width="100%" style="border: none;">
                                                    <tr style="border: none;">
                                                        <td align="center" style="border: none;">
                                                            <c:if test="${(paymentTypeIntegration == 'WALLET' && !isAirtimeTransaction) || paymentTypeIntegration == 'VOURCHER_PIN'}">
                                                                <span style="color:green">Status: Online</span>
                                                            </c:if>
                                                            <c:if test="${(paymentTypeIntegration == 'WALLET' && isAirtimeTransaction)}">
                                                                <span style="color:red">Not available for this transaction</span>
                                                            </c:if>
                                                            <c:if test="${paymentTypeIntegration == 'LINK'}">
                                                                <span style="color:red"><fmt:message key="scp.payment.gateway.options.externallink"/></span>
                                                            </c:if>
                                                            <c:if test="${paymentTypeIntegration == 'SCA' || paymentTypeIntegration == 'SCAWALLET'}">
                                                                <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="paymentTypeProps1" varStatus="loopConf">
                                                                    <c:set var="ptConfigProps" value="${fn:split(paymentTypeProps1, '|')}"/>
                                                                    <c:set var="gatewayStatus" value="${ptConfigProps[2]}"/>
                                                                </c:forEach>
                                                                <c:set var="ptGatewayStatus" value="${fn:split(gatewayStatus, '=')}"/>
                                                                <c:if test="${ptGatewayStatus[1] == 'Offline'}">
                                                                    <span style="color:red">Status: ${ptGatewayStatus[1]}</span>
                                                                </c:if>
                                                                <c:if test="${ptGatewayStatus[1] == 'Online'}">
                                                                    <span style="color:green">Status: ${ptGatewayStatus[1]}</span>
                                                                </c:if>    
                                                            </c:if>
                                                            
                                                        </td>
                                                    </tr>
                                                    <tr style="border: none;">
                                                        
                                                        <td align="center" style="border: none;">
                                                            
                                                            <c:if test="${DisplayPreference[1] == 'Horizontal_ImageFirst_WithGatewayLogo'}">
                                                                <c:set var="imageLogo" value="${ptProps[5]}"/><%--E.g: ImageLogo=gemex.png--%>
                                                                <c:set var="logo" value="${fn:split(imageLogo, '=')}"/>
                                                                <span><fmt:message key="scp.payment.gateway.options.poweredby.${paymentType}"/>&nbsp;<img style="vertical-align:middle" src="images/${logo[1]}" alt="${paymentType}" title="${paymentType} logo" height="20"/>&nbsp;&nbsp;</span>
                                                            </c:if>
                                                            <c:if test="${DisplayPreference[1] == 'Horizontal_ImageFirst'}">
                                                                <span><fmt:message key="scp.payment.gateway.options.poweredby.${paymentType}"/></span>
                                                            </c:if>
                                                            <c:if test="${DisplayPreference[1] == 'Horizontal_ImageLast_WithGatewayLogo'}">
                                                                <c:set var="imageLogo" value="${ptProps[5]}"/><%--E.g: ImageLogo=gemex.png--%>
                                                                <c:set var="logo" value="${fn:split(imageLogo, '=')}"/>
                                                                <span>&nbsp;&nbsp;<fmt:message key="scp.payment.gateway.options.poweredby.${paymentType}"/>&nbsp;<img style="vertical-align:middle" src="images/${logo[1]}" alt="${paymentType}" title="${paymentType} logo" height="20"/></span>
                                                            </c:if>
                                                            <c:if test="${DisplayPreference[1] == 'Horizontal_ImageLast' || DisplayPreference[1] == 'Horizontal'}">
                                                                <span><fmt:message key="scp.payment.gateway.options.poweredby.${paymentType}"/></span>
                                                            </c:if>
                                                            <c:if test="${paymentTypeIntegration == 'LINKSS'}">
                                                                <span><fmt:message key="scp.payment.gateway.options.poweredby.${paymentType}"/>&nbsp;<img style="vertical-align:middle" src="images/logo_${paymentType}" alt="${paymentType}" title="${paymentType} logo" height="20"/></span>
                                                            </c:if>
                                                            
                                                        </td>
                                                        
                                                    </tr>
                                                    <tr style="border: none;">
                                                        
                                                        <td align="center" style="border: none;">
                                                                <span> 
                                                                        <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="paymentTypeProps1" varStatus="loopConf">
                                                                            <c:set var="ptConfigProps" value="${fn:split(paymentTypeProps1, '|')}"/>
                                                                            <c:set var="paymentTypeImage" value="${ptConfigProps[0]}"/>
                                                                            <c:set var="imageFileType" value="${ptConfigProps[1]}"/>

                                                                            <c:if test="${paymentTypeIntegration == 'WALLET'}">
                                                                                <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/> <span><fmt:message key="scp.smile.airtime"/></span>
                                                                            </c:if>
                                                                            <c:if test="${paymentTypeIntegration != 'WALLET' && paymentTypeIntegration != 'SCAWALLET'}">
                                                                                <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}recharge_${fn:toLowerCase(paymentTypeImage)}.${imageFileType}" alt="${paymentTypeImage}" title="${paymentTypeImage}" height="20"/>
                                                                            </c:if>
                                                                        </c:forEach>
                                                                </span>
                                                            
                                                            
                                                        </td>

                                                    </tr>
                                                  
                                                    <tr style="border: none;">
                                                        <td width="3%" align="center" style="border: none; vertical-align:middle">
                                                            
                                                            <c:forEach items="${s:getPropertyAsList(paymentTypeConfigs)}" var="paymentTypeProps1" varStatus="loopConf">
                                                                <c:set var="ptConfigProps" value="${fn:split(paymentTypeProps1, '|')}"/>
                                                                    <c:set var="gatewayStatus" value="${ptConfigProps[2]}"/>
                                                            </c:forEach>
                                                            <c:set var="ptGatewayStatus" value="${fn:split(gatewayStatus, '=')}"/>
                                                            
                                                            <c:if test="${ptGatewayStatus[1] == 'Offline'}">
                                                                <c:if test="${isAirtimeTransaction}">
                                                                    <stripes:radio name="sale.paymentGatewayCode" value="${paymentType}" disabled="true"/>
                                                                </c:if>
                                                                <c:if test="${!isAirtimeTransaction}">
                                                                    <stripes:radio name="gatewayCode" value="${paymentType}" disabled="true"/>
                                                                </c:if>
                                                            </c:if>
                                                            <c:if test="${ptGatewayStatus[1] == 'Online'}">
                                                                <c:if test="${isAirtimeTransaction}">                                                                                                                               
                                                                        <c:choose>
                                                                            <c:when test="${paymentType == 'YoPayments' && ((paymentTypeIntegration == 'SCAWALLET' && isAirtimeTransaction) || paymentTypeIntegration == 'VOURCHER_PIN')}">
                                                                                <table width="80%">
                                                                                    <tr>
                                                                                        <td style="text-align: center; vertical-align: middle;">
                                                                                            <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}MTN-Mobile-Money.jpg" alt="MTN" title="MTN" height="50"/><br/>
                                                                                            <stripes:radio name="sale.paymentGatewayCode" id="mtnProcessor" value="${paymentType}" onchange="javascript:getRechargePhoneNumber('MTN')"/>
                                                                                        </td>
                                                                                        <td style="text-align: center; vertical-align: middle;">
                                                                                        <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}Airtel-Money.png" alt="Airtel" title="Airtel" height="50"/><br/>
                                                                                        <stripes:radio name="sale.paymentGatewayCode" id="airtelProcessor" value="${paymentType}" onchange="javascript:getRechargePhoneNumber('Airtel')" />
                                                                                        </td>
                                                                                    </tr>                                                                        
                                                                                </table>
                                                                                    <input type="hidden" value="" name="mobileMoneyProcessor" id="mobileMoneyProcessor">    
                                                                            </c:when>
                                                                            <c:otherwise>
                                                                                    <stripes:radio name="sale.paymentGatewayCode" value="${paymentType}" ng-model="pt" ng-change="enableProceedButton()"/>
                                                                            </c:otherwise>
                                                                        </c:choose>
                                                                        <c:if test="${(paymentTypeIntegration == 'SCAWALLET')}">

                                                                            <div id="phoneNumberDiv" name="phoneNumberDiv" hidden="true">
                                                                                <table>
                                                                                    <tr style="border: none;">
                                                                                        <td style="border: none; align-content: center">
                                                                                            <label for="sale.recipientPhoneNumber"><fmt:message key="scp.payment.gateway.wallet.txt"/></label><b/>
                                                                                            <input type="text" name="sale.recipientPhoneNumber" value="" placeholder="eg. 256123456789" style="padding:5px; border-radius: 5px" title="Wallet linked mobile number"/>
                                                                                        </td>                                                                    
                                                                                    </tr>
                                                                                </table>                                                                  
                                                                            </div>    
                                                                        </c:if>      
                                                                </c:if>
                                                                
                                                                <c:if test="${!isAirtimeTransaction}">
                                                                    <c:choose>
                                                                        <c:when test="${paymentType == 'YoPayments' && ((paymentTypeIntegration == 'SCAWALLET' && !isAirtimeTransaction) || paymentTypeIntegration == 'VOURCHER_PIN')}">
                                                                            <table width="80%">
                                                                                <tr>
                                                                                    <td style="text-align: center; vertical-align: middle;">
                                                                                        <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}MTN-Mobile-Money.jpg" alt="MTN" title="MTN" height="50"/><br/>
                                                                                        <stripes:radio name="gatewayCode" id="mtnProcessor" value="${paymentType}" onchange="javascript:getRechargePhoneNumber('MTN')"/>
                                                                                    </td>
                                                                                    <td style="text-align: center; vertical-align: middle;">
                                                                                    <img style="vertical-align:middle" src="${s:getPropertyWithDefault('env.scp.recharge.hosted.image.location.dir', 'images/')}Airtel-Money.png" alt="Airtel" title="Airtel" height="50"/><br/>
                                                                                    <stripes:radio name="gatewayCode" id="airtelProcessor" value="${paymentType}" onchange="javascript:getRechargePhoneNumber('Airtel')" />
                                                                                    </td>
                                                                                </tr>                                                                        
                                                                            </table>
                                                                                <input type="hidden" value="" name="mobileMoneyProcessor" id="mobileMoneyProcessor">    
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                                <stripes:radio name="gatewayCode" value="${paymentType}" ng-model="pt" ng-change="enableProceedButton()"/>
                                                                        </c:otherwise>
                                                                    </c:choose>
                                                                    <c:if test="${(paymentTypeIntegration == 'SCAWALLET')}">
                                                                        
                                                                        <div id="phoneNumberDiv" name="phoneNumberDiv" hidden="true">
                                                                            <table>
                                                                                <tr style="border: none;">
                                                                                    <td style="border: none; align-content: center">
                                                                                        <label for="sale.recipientPhoneNumber"><fmt:message key="scp.payment.gateway.wallet.txt"/></label><b/>
                                                                                        <input type="text" name="sale.recipientPhoneNumber" value="" placeholder="eg. 256123456789" style="padding:5px; border-radius: 5px" title="Wallet linked mobile number"/>
                                                                                    </td>                                                                    
                                                                                </tr>
                                                                            </table>                                                                  
                                                                        </div>    
                                                                    </c:if>            
                                                                    
                                                                </c:if>
                                                            </c:if> 
                                                                    
                                                            <c:if test="${(paymentTypeIntegration == 'WALLET' && !isAirtimeTransaction) || paymentTypeIntegration == 'VOURCHER_PIN'}">
                                                                <stripes:radio name="gatewayCode" value="${paymentType}" ng-model="pt" ng-change="enableProceedButton()"/>
                                                            </c:if>
                                                            
                                                            <c:if test="${paymentTypeIntegration == 'LINK'}">
                                                                <a href="${ptConfigProps[2]}" class="link_visit" target="_blank"><fmt:message key="scp.payment.gateway.options.externallink.clickhere"/></a>
                                                            </c:if>        
                                                                    
                                                            
                                                        </td>
                                                    </tr>
 
                                                </table>
                                            </td>
                                        </c:if>
                                        <%-- END :: DISPLAY PAYMENT GATEWAY OPTION IF ALLOWED--%>    
                                        <%--</tr>--%>
                                    <c:if test="${loopP.count == 3 || loopP.count == 6 || loopP.count == 9 || loopP.count == 12 || loopP.count == 15 || loopP.last}">
                                        </tr>
                                    </c:if>
                                   
                            </c:forEach>

                        </c:otherwise>
                    </c:choose>


                   
                </table>

                
                 <table ng-show="pt == 'Wallet'" border="0" cellpadding="0" cellspacing="0" width="100%">
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
                        <td>
                             <c:if test="${!isAirtimeTransaction}">
                                <div ng-init="getResultsPage(1)">
                                </div>

                                <div class="sixteen columns">
                                    Choose Paying Account
                                    <div class="fifteen columns">
                                        <label dir-paginate="account in usersAccounts | itemsPerPage: accountsPerPage" total-items="pendingPagesCount" current-page="pagination.current">
                                            &nbsp;&nbsp; <input type="radio" name="payingAccountID" ng-model="$parent.payingAccountID" ng-value="{{account.accountId}}">
                                            {{ account.accountId }} -- {{ account.availableBalanceInCents/100| currency:"${s:getPropertyWithDefault('env.portal.locale.currency.notation','')}"}}
                                        </label><br/>

                                    </div>
                                </div>
                                <div ng-show="pendingPagesCount > accountsPerPage" class="sixteen columns">
                                    <small>Navigate pages</small>
                                    <div class="fifteen columns">
                                        <dir-pagination-controls on-page-change="pageChangeHandler(newPageNumber)"></dir-pagination-controls>
                                    </div>
                                </div>
                                <br/>
                            </c:if>
                        </td>
                    </tr>
                </table>
                <table ng-show="pt == 'Vourcher_Pin'" border="0" cellpadding="0" cellspacing="0" width="100%">
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
                        <td>
                             PIN:1234567890
                        </td>
                    </tr>
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
                        
                        <td ng-show="displayProceedButton" class="offset-by-fourteen">
                            <c:if test="${!isAirtimeTransaction}">
                                <div ng-show="payingAccountID > 0">
                                    <input type="hidden" name="purchaseUnitCreditRequest.paidByAccountId" value="{{payingAccountID}}"/>
                                </div>
                                <input type="submit" name="provisionUnitCreditByPaymentType" class="button_proceed" value="Proceed"/>
                            </c:if>

                            <c:if test="${isAirtimeTransaction}">
                                <input type="submit" name="buyAirtimeViaPaymentGateway" class="button_proceed" value="Proceed"/>
                            </c:if>
                        </td>
                        
                            <td  class="offset-by-fourteen">
                                <div id="yoPaymentsProceedBtn" name="yoPaymentsProceedBtn" hidden="true">                                    
                                    <c:if test="${!isAirtimeTransaction}">                                        
                                        <div ng-show="payingAccountID > 0">
                                            <input type="hidden" name="purchaseUnitCreditRequest.paidByAccountId" value="{{payingAccountID}}"/>
                                        </div>
                                        <input type="submit" name="provisionUnitCreditByPaymentType" class="button_proceed" value="Proceed"/>
                                    </c:if>

                                    <c:if test="${isAirtimeTransaction}">
                                        <input type="submit" name="buyAirtimeViaPaymentGateway" class="button_proceed" value="Proceed"/>
                                    </c:if>
                                </div>
                            </td>
                        
                    </tr>
                </table>
                
               


            </stripes:form>
           
        </div>
    </stripes:layout-component>

</stripes:layout-render>


