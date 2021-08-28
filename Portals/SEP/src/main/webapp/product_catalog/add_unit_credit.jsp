<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.unit.credit"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>Add Unit Credit To Account ${actionBean.account.accountId}</td>                        
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


            <table class="green" width="95%">                                
                <tr>
                    <th>Name</th>
                    <th>Price</th>
                    <th>Usable (Days)</th>
                    <th>Units</th>
                    <th>Occurrences</th>
                    <th>Start Delay (Days)</th>
                    <th>Paying Account</th>
                    <th>Info</th>
                    <th>Purchase</th>
                </tr>
                <c:forEach items="${actionBean.unitCreditSpecificationList.unitCreditSpecifications}" var="unitCreditSpecification" varStatus="loop">
                    <stripes:form action="/Account.action">
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            <td>${unitCreditSpecification.name}</td>                
                            <td>${s:convertCentsToCurrencyLong(unitCreditSpecification.priceInCents)}</td>
                            <td>${unitCreditSpecification.usableDays}</td>
                            <td>${s:formatBigNumber(unitCreditSpecification.units)}</td>
                            <td><input type="text" name="purchaseUnitCreditRequest.numberToPurchase" value="1" class="required" size="2"  maxlength="2"/></td>
                            <td><input type="text" name="purchaseUnitCreditRequest.daysGapBetweenStart" value="-1" class="required" size="2"  maxlength="2"/></td>
                            <td><input type="text" name="purchaseUnitCreditRequest.paidByAccountId" value="${actionBean.account.accountId}"  size="10"  maxlength="10"/></td>
                            <td>
                                <c:set var="unitCreditInfoProperty" value="env.bm.unit.credit.info.${unitCreditSpecification.itemNumber}"/>
                                <c:choose>
                                    <c:when test='${s:getPropertyWithDefault(unitCreditInfoProperty,"null") == "null"}'>
                                            <textarea name="purchaseUnitCreditRequest.info" rows="0" cols="5"/></textarea>
                                    </c:when>
                                    <c:otherwise>
                                            <stripes:select name="purchaseUnitCreditRequest.info">
                                                <c:forEach items='${s:getPropertyAsList(unitCreditInfoProperty)}' var="info" varStatus="loop">
                                                    <stripes:option value="${info}"><fmt:message key="unit.credit.info.${info}" /></stripes:option>
                                                </c:forEach>
                                            </stripes:select>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/>
                                <stripes:hidden name="purchaseUnitCreditRequest.accountId" value="${actionBean.account.accountId}"/>
                                <input type="hidden" name="purchaseUnitCreditRequest.unitCreditSpecificationId" value="${unitCreditSpecification.unitCreditSpecificationId}"/>
                                <stripes:submit name="provisionUnitCredit"/>
                            </td>
                        </tr>        
                    </stripes:form>
                </c:forEach>
            </table>   
                    
        </div>
    </stripes:layout-component>
</stripes:layout-render>

