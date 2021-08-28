<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="product.instance.detail"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div style="margin-top: 10px;" class="sixteen columns">
            <table class="greentbl" width="99%">
                <tr>
                    <th>Product name</th>
                    <th>Friendly name</th>
                    <%--<th>Phone</th>--%>
                    <th>ICCID</th>
                    <th>Choose</th>
                </tr>
                <c:forEach items="${actionBean.productInstanceList.productInstances}" var="productInstance" varStatus="loop">
                    <c:set var="productSpec" value="${s:getProductSpecification(productInstance.productSpecificationId)}"/>
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>${productSpec.name}</td>
                        <td>${productInstance.friendlyName}</td>
                        <%--<td>${s:getProductInstancePhoneNumber(productInstance.productInstanceId)}</td>--%>
                        <td>${s:getProductInstanceICCID(productInstance.productInstanceId)}</td>
                        <td>
                            <stripes:form action="/Account.action">
                                <stripes:hidden name="productInstance.productInstanceId" value="${productInstance.productInstanceId}"/>
                                <stripes:hidden name="accountQuery.accountId" value="${actionBean.accountHistoryQuery.accountId}"/>
                                <stripes:hidden name="accountHistoryQuery.accountId" value="${actionBean.accountHistoryQuery.accountId}"/>
                                <stripes:hidden name="searchMonth" value="${actionBean.searchMonth}"/>
                                <input type="submit" name="retrieveTNFData" class="button_proceed" value="Proceed"/>
                            </stripes:form>                                
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </div>
    </stripes:layout-component>
</stripes:layout-render>

