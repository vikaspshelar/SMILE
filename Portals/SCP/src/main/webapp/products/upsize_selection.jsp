<%-- 
    Document   : upsize_selection
    Created on : 15 Dec 2017, 6:17:25 AM
    Author     : sabza
--%>
<%@include file="../include/scp_include.jsp" %>

<c:set var="title">
    <c:set var="booster" value="false" />
    <c:forEach items="${actionBean.purchaseUnitCreditLines}" var="purchaseUnitCreditLine" varStatus="loop">         
            <c:if  test="${fn:containsIgnoreCase(purchaseUnitCreditLine.unitCreditSpecification.name, 'booster')}">
                <c:set var="booster" value="true" />
            </c:if>   
    </c:forEach> 
    
    <c:choose>
        <c:when test="${booster}">
            <fmt:message key="scp.recharge.booster.selection"/>
        </c:when>
        <c:otherwise>
            <fmt:message key="scp.recharge.upsize.selection"/>
        </c:otherwise>
    </c:choose>   
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
        <div style="margin-top: 10px;" class="sixteen columns alpha">

            <div id="bundle_list" style="display:block">
                <c:set var="ucs" value="${s:getUnitCreditSpecification(actionBean.unitCreditSpecification.unitCreditSpecificationId)}"/>
                
                <div style="font-size: 18px; margin: 10px auto; line-height:25px; text-align: center; font-family:'Arial';">
                    
                    <c:set var="booster" value="false" />
                    <c:forEach items="${actionBean.purchaseUnitCreditLines}" var="purchaseUnitCreditLine" varStatus="loop">         
                            <c:if  test="${fn:containsIgnoreCase(purchaseUnitCreditLine.unitCreditSpecification.name, 'booster')}">
                                <c:set var="booster" value="true" />
                            </c:if>   
                    </c:forEach> 

                    <c:choose>
                        <c:when test="${booster}">
                            <fmt:message key="scp.booster.subheading.msg"/>
                        </c:when>
                        <c:otherwise>
                            <fmt:message key="scp.upsize.subheading.msg"/>
                        </c:otherwise>
                    </c:choose>   

                    
                </div>

                <table width="100%" style="border-style: solid; border-width: 1px; border-color: #639B2E; border-collapse: separate; border-spacing: 10px; background-color: white; margin: 3px 3px 3px 3px;">
                   
                    <tr>
                        <th style="padding: 2px; background-color: #86919A; color:#fff; font-family: UniversLTCn; font-size: 20px; font-weight: bold;"><fmt:message key="scp.upsize.data.bundle.name"/></th>
                        <th style="padding: 2px; background-color: #86919A; color:#fff; font-family: UniversLTCn; font-size: 20px; font-weight: bold;"><fmt:message key="price"/></th>
                        <th style="padding: 2px; background-color: #86919A; color:#fff; font-family: UniversLTCn; font-size: 20px; font-weight: bold;"><fmt:message key="scp.validity.days"/></th>                    
                        <th></th>
                    </tr>
                    
                    <c:forEach items="${actionBean.purchaseUnitCreditLines}" var="purchaseUnitCreditLine" varStatus="loop"> 
                        <stripes:form action="/Account.action">
                            <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/>
                            <stripes:hidden name="purchaseUnitCreditRequest.accountId" value="${actionBean.account.accountId}"/>
                            <input type="hidden" name="purchaseUnitCreditRequest.unitCreditSpecificationId" value="${actionBean.unitCreditSpecification.unitCreditSpecificationId}"/>
                            <stripes:hidden name="sale.recipientCustomerId" value="${actionBean.customer.customerId}"/>
                            <input type="hidden" name="sale.recipientAccountId" value="${actionBean.account.accountId}"/>
                            <stripes:hidden name="purchaseUnitCreditLine.unitCreditSpecificationId" value="${purchaseUnitCreditLine.unitCreditSpecification.unitCreditSpecificationId}"/>

                            <tr class="${loop.count mod 2 == 0 ? "even1" : "odd1"}" width="99%">
                                <td>${purchaseUnitCreditLine.unitCreditSpecification.name}</td>                
                                <td>${s:convertCentsToCurrencyShortWithCommaGroupingSeparator(purchaseUnitCreditLine.unitCreditSpecification.priceInCents)}</td>
                                <td align="center">${purchaseUnitCreditLine.unitCreditSpecification.usableDays}</td>                            
                                <td style="padding: 2px 2px 2px 2px; width: 122px;">
                                    <c:if test="${!empty actionBean.productInstanceIdForSIM}">
                                        <stripes:hidden name="purchaseUnitCreditRequest.productInstanceId" value="${actionBean.productInstanceIdForSIM}"/>
                                    </c:if>                            
                                    <c:choose>
                                        <c:when test="${fn:containsIgnoreCase(purchaseUnitCreditLine.unitCreditSpecification.name, 'booster')}">
                                            <input class="button_purchase" style="text-transform: capitalize;" type="submit" name="showUnitCreditPaymentMethodPage" value="AddBooster"/>
                                        </c:when>
                                        <c:otherwise>
                                            <input class="button_purchase" style="text-transform: capitalize;" type="submit" name="showUnitCreditPaymentMethodPage" value="UPSize"/>
                                        </c:otherwise>
                                    </c:choose>                                
                                </td>
                            </tr>
                        </stripes:form>
                    </c:forEach>
                            
                    <tr width="99%">
                        <stripes:form action="/Account.action">
                            <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/>
                            <stripes:hidden name="purchaseUnitCreditRequest.accountId" value="${actionBean.account.accountId}"/>
                            <input type="hidden" name="purchaseUnitCreditRequest.unitCreditSpecificationId" value="${actionBean.unitCreditSpecification.unitCreditSpecificationId}"/>
                            <stripes:hidden name="sale.recipientCustomerId" value="${actionBean.customer.customerId}"/>
                            <input type="hidden" name="sale.recipientAccountId" value="${actionBean.account.accountId}"/>
                            <td colspan="4">
                                <input class="button_purchase" type="submit" name="showUnitCreditPaymentMethodPage" value="Skip"/>
                            </td>
                        </stripes:form>
                    </tr>

                </table>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
