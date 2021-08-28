<%-- 
    Document   : select_sim_to_load_bundle
    Created on : 10 Oct 2012, 10:56:33 AM
    Author     : lesiba
--%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.unit.credit"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <table class="clear">
            <tr>
                <td colspan="2">
                    <b>
                        <fmt:message key="scp.select.service.to.load.bundle"/>
                    </b>
                </td>
            </tr>            
        </table>       
        <table class="green" width="99%">
            <tr>
                <th>
                    <fmt:message key="scp.icci.friendlyname"/>
                </th>
                <th>

                </th>
            </tr>
            <c:forEach items="${actionBean.productInstanceSIMList}" var="entry" varStatus="loop">
                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}" width="99%">
                    <td>
                        ${entry.value}
                    </td>
                    <td style="max-width: 100px; width: 100px;" >
                        <stripes:form action="/Account.action">
                            <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/>
                            <stripes:hidden name="purchaseUnitCreditRequest.accountId" value="${actionBean.account.accountId}"/>
                            <stripes:hidden  name="purchaseUnitCreditRequest.unitCreditSpecificationId" value="${actionBean.unitCreditSpecification.unitCreditSpecificationId}"/>
                            <stripes:hidden  name="purchaseUnitCreditRequest.numberToPurchase" value="${actionBean.purchaseUnitCreditRequest.numberToPurchase}"/>
                            <stripes:hidden  name="purchaseUnitCreditRequest.daysGapBetweenStart" value="${actionBean.purchaseUnitCreditRequest.daysGapBetweenStart}"/>
                            <stripes:hidden  name="purchaseUnitCreditRequest.paidByAccountId" value="${actionBean.purchaseUnitCreditRequest.paidByAccountId}"/>
                            <input type="hidden" name="purchaseUnitCreditRequest.productInstanceId" value="${entry.key}"/>                           
                            <div style="margin-top:-1px;">
                                <input type="submit" class="submitBlack" name="provisionUnitCreditForSim" value="Select" style="margin-left:9px"/>
                            </div>
                        </stripes:form>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </stripes:layout-component>
</stripes:layout-render>
