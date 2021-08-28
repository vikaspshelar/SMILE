<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="sale.org.account.data"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 

        <table class="green" width="99%">
            <tr>
                <th><fmt:message key="organisation.name"/></th>
                <th><fmt:message key="role.name"/></th>
                <th><fmt:message key="sale.account"/></th>
            </tr>
            <tr>
                <td>Private Sale</td>
                <td>Private Sale</td>
                <td>

                    <table class="clear" width="99%">
                        <tr><th colspan="2">Default Account Id for Provisioning</th></tr>
                                <stripes:form action="/Sales.action" class="buttonOnly">
                                    <stripes:hidden name="sale.recipientOrganisationId" value="0"/>
                                    <stripes:hidden name="sale.recipientCustomerId" value="${actionBean.customer.customerId}"/>

                            <c:forEach items="${actionBean.customersPrivateAccounts}" var="accountId" varStatus="loop"> 
                                <tr>
                                    <td>${accountId}</td>
                                    <td>
                                        <stripes:radio  name="sale.recipientAccountId" value="${accountId}"/>
                                    </td>
                                </tr>
                            </c:forEach>
                            <tr>
                                <td colspan="2" style="text-align:right;">
                                    <span class="button">
                                        <stripes:submit name="collectItemsInSale"/>
                                    </span>
                                </td>
                            </tr>
                        </stripes:form>
                    </table>
                </td>
            </tr>
            <c:forEach items="${actionBean.customer.customerRoles}" var="role" varStatus="loop">
                <tr>
                    <td>${role.organisationName}</td>
                    <td>${role.roleName}</td>
                    <td>
                        <table class="clear" width="99%">
                            <tr><th colspan="2">Default Account Id for Provisioning</th></tr>
                                    <stripes:form action="/Sales.action" class="buttonOnly">
                                        <stripes:hidden name="sale.recipientOrganisationId" value="${role.organisationId}"/>
                                        <stripes:hidden name="sale.recipientCustomerId" value="${actionBean.customer.customerId}"/>
                                        <c:forEach items="${actionBean.customersAccountsPerOrganisation[role.organisationId]}" var="accountId" varStatus="loop"> 
                                    <tr>
                                        <td>${accountId}</td>
                                        <td>
                                            <stripes:radio  name="sale.recipientAccountId" value="${accountId}"/>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <tr>
                                    <td colspan="2" style="text-align:right;">
                                        <span class="button">
                                            <stripes:submit name="collectItemsInSale"/>
                                        </span>
                                        <span class="button">
                                            <stripes:submit name="collectItemsInUSDSale"/>
                                        </span>
                                        <span class="button">
                                            <stripes:submit name="collectItemsInEURSale"/>
                                        </span>
                                    </td>
                                </tr>
                            </stripes:form>
                        </table>
                    </td>
                </tr>
            </c:forEach>

        </table>
    </stripes:layout-component>    
</stripes:layout-render>

