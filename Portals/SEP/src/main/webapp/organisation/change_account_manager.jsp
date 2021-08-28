<%@include  file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="manage.organisation"/>
</c:set>


<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>
                        <fmt:message key="organisation"/> : ${actionBean.organisation.organisationName}
                    </td>
                    <td align="right">
                        <stripes:form name="frm" action="/Customer.action">
                            <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveOrganisation"><fmt:message key="manage.organisation"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction" />
                        </stripes:form>
                    </td>
                </tr>
            </table>
            <stripes:form action="/Customer.action">
                <stripes:hidden name="organisation.version" value="${actionBean.organisation.version}"/>
                <stripes:hidden name="organisation.organisationId" value="${actionBean.organisation.organisationId}"/> 
                <table class="clear">
                    <tr>
                        <td>
                            <fmt:message key="account.manager.customer"/>: 
                        </td>
                        <td>
                            <stripes:select name="organisation.accountManagerCustomerProfileId">
                                <c:forEach items="${actionBean.accountManagerCustomerList}" var="accountManager" varStatus="loop">
                                    <stripes:option value="${accountManager.key}">${accountManager.value}</stripes:option>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <span class="button">
                                <stripes:submit name="changeOrganisationsAccountManagerCustomer" value="Change"/>
                            </span>
                        </td>
                    </tr>
                </table>
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>