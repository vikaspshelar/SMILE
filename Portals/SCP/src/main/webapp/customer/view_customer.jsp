<%-- 
    Document   : view_customer
    Created on : Feb 20, 2012, 3:45:57 PM
    Author     : lesiba
--%>
<%@include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="customer"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">  
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function () {
                makeMenuActive('Profile');
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">  
        <div style="margin-top: 10px;">
            <jsp:include page="/layout/my_details_left_banner.jsp"/>


            <div class="my_details"  style="margin-top: 2px;">
                <table border="0" cellpadding="0" cellspacing="0" width="100%">
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.first.name"/>:</strong>
                        </td>
                        <td>
                            ${actionBean.customer.firstName}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.last.name"/>:</strong>
                        </td>
                        <td>
                            ${actionBean.customer.lastName}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.username"/>:</strong>
                        </td>
                        <td>
                            ${actionBean.customer.SSOIdentity}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.account.manager.customer"/>:</strong>
                        </td>
                        <td>
                            ${actionBean.accountManagerName}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.mothers.maiden.name"/>:</strong>
                        </td>
                        <td>
                            ${actionBean.customer.mothersMaidenName}
                        </td>
                    </tr>
                    <tr>
                        <td>&nbsp;

                        </td>
                        <td>&nbsp;

                        </td>
                    </tr>                    
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.email"/>:</strong>
                        </td>
                        <td>
                            ${actionBean.customer.emailAddress}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.alternative.contact.number.1"/>:&nbsp;&nbsp;</strong> 
                        </td>
                        <td>
                            ${actionBean.customer.alternativeContact1}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.alternative.contact.number.2"/>:&nbsp;&nbsp;</strong>
                        </td>
                        <td>
                            ${actionBean.customer.alternativeContact2}
                        </td>
                    </tr>
                    <stripes:form action="/Customer.action" focus="">
                        <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                        <tr>
                            <td><strong>Receive Smile Marketing Emails/SMS:</strong></td>
                            <td>
                                <stripes:checkbox  name="optInMarketing"/>
                            </td>
                        </tr>
                        <tr>
                            <td><strong>Receive Post Call SMS:</strong></td>
                            <td>
                                <stripes:checkbox name="optInPostCall"/>
                            </td>
                        </tr>
                        <tr>
                            <td>
                            </td>
                            <td>
                                <stripes:submit name="updateOptIn"/>
                            </td>
                        </tr>
                    </stripes:form>
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.gender"/>:</strong>
                        </td>
                        <td>
                            ${actionBean.customer.gender}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.language"/>:</strong>
                        </td>
                        <td>
                            ${actionBean.customer.language}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.date.of.birth"/>:</strong>
                        </td>
                        <td>
                            ${actionBean.customer.dateOfBirth}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.nationality"/>:</strong>
                        </td>
                        <td>
                            ${actionBean.customer.nationality}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong><fmt:message key="scp.passport.expiry.date"/>:</strong>
                        </td>
                        <td>
                            ${actionBean.customer.passportExpiryDate}
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <strong>&nbsp;</strong>
                        </td>
                        <td>&nbsp;

                        </td>
                    </tr>
                    <c:if test="${s:getListSize(actionBean.customer.addresses) > 0}">
                        <tr>
                            <td>
                                <font style="color:#75B343; font-weight:bold;"><fmt:message key="scp.customer.addresses"/></font>
                            </td>
                            <td>&nbsp;

                            </td>
                        </tr>
                        <tr>
                            <td>
                                <strong>&nbsp;</strong>
                            </td>
                            <td>&nbsp;

                            </td>
                        </tr>
                        <c:forEach items="${s:getPropertyAsList('env.customer.address.types')}" var="addressesTypes" varStatus="loopTypes">
                            <c:forEach items="${actionBean.customer.addresses}" var="addresses" varStatus="loop">
                                <c:choose>
                                    <c:when test="${actionBean.customer.addresses[loop.index].type == addressesTypes}">
                                        <tr>
                                            <td>
                                                <strong>${actionBean.customer.addresses[loop.index].type}</strong>
                                            </td>
                                            <td>&nbsp;

                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <strong><fmt:message key="scp.address.line1"/>:</strong>
                                            </td>
                                            <td>
                                                ${actionBean.customer.addresses[loop.index].line1}
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <strong><fmt:message key="scp.address.line2"/>:</strong> 
                                            </td>
                                            <td>
                                                ${actionBean.customer.addresses[loop.index].line2}
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <strong><fmt:message key="scp.town"/>:</strong>
                                            </td>
                                            <td>
                                                ${actionBean.customer.addresses[loop.index].town}
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <strong><fmt:message key="scp.zone"/>:</strong>
                                            </td>
                                            <td>
                                                ${actionBean.customer.addresses[loop.index].zone}
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <strong><fmt:message key="scp.state"/>:</strong>
                                            </td>
                                            <td>
                                                ${actionBean.customer.addresses[loop.index].state}
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <strong><fmt:message key="scp.country"/>:</strong> 
                                            </td>
                                            <td>
                                                ${actionBean.customer.addresses[loop.index].country}
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <strong><fmt:message key="scp.code"/>:</strong>
                                            </td>
                                            <td>
                                                ${actionBean.customer.addresses[loop.index].code}
                                            </td>
                                        </tr>
                                    </c:when>
                                    <c:otherwise>
                                        <tr>
                                            <td>
                                                <strong><fmt:message key="scp.${addressesTypes}"/>:</strong>
                                            </td>
                                            <td>
                                                <fmt:message key="scp.address.none"/>
                                            </td>
                                        </tr>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>
                            <tr>
                                <td>
                                    <strong>&nbsp;</strong>
                                </td>
                                <td>&nbsp;

                                </td>
                            </tr>
                        </c:forEach>

                    </c:if>
                </table>
            </div>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
