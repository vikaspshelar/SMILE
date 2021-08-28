<%-- 
    Document   : nav
    Created on : Feb 17, 2012, 11:29:55 AM
    Author     : lesiba
--%>

<%@include file="/include/scp_include.jsp" %>
<c:if test="${!(pageContext.request.remoteUser == null)}">
    <c:catch var="exception">
        <c:set var="temp">${actionBean.customer}</c:set>
    </c:catch>

    <c:if test="${empty exception && empty actionBean.customer.outstandingTermsAndConditions}">


        <div class="sixteen columns alpha">
            <div class="logo four columns alpha">
                <img src="images/smile-logo.svg" class="scale-with-grid">
            </div>
            <div class="nav">
                <ul class="main_menu">
                    <li>
                        <stripes:link href="/Customer.action" event="retrieveCustomer">
                            <div id="profile_menu_active" class="profile_menu">&nbsp;
                                <div class="profile_menu_title"><fmt:message key="scp.my.profile"/></div>
                            </div>
                        </stripes:link>
                    </li>

                    <li>
                        <stripes:link href="/Account.action" event="showAddUnitCredits">
                            <div id="bundle_menu_active" class="bundle_menu">&nbsp;
                                <div class="menu_title_contact"><fmt:message key="scp.recharge"/></div>
                            </div>
                        </stripes:link>
                    </li>
                    <li>
                        <stripes:link href="/Account.action" event="showTransactionHistory">
                            <div id="trans_menu_active" class="trans_menu">&nbsp;
                                <div class="menu_title"><fmt:message key="transaction.history"/></div>
                            </div>
                        </stripes:link>
                    </li>
                    <li>
                        <stripes:link href="/SCPTroubleTicket.action" event="addTroubleTicket">
                            <div id="help_menu_active" class="help_menu">&nbsp;
                                <div class="menu_title_contact"><fmt:message key="scp.help"/></div>
                            </div>
                        </stripes:link>
                    </li>
                </ul><br>
            </div>
        </div>
        <div class="sixteen columns">
            <ul id="sub_menu" class="sub_menu">
                <li>
                    <stripes:link href="/Customer.action" event="retrieveCustomer">
                        <div id="details_menu_active" class="details_menu_active"><div class="submenu_title"><fmt:message key="view.details"/></div></div>
                        </stripes:link>
                </li>
                <li>
                    <stripes:link href="/Account.action" event="retrieveAllUserServicesAccounts">
                        <div id="accounts_menu_active" class="accounts_menu"><div class="submenu_title"><fmt:message key="my.accounts"/></div></div>
                        </stripes:link>
                </li>
                <li>
                    <stripes:link href="/Customer.action" event="showUpdateCustomerPassword">
                        <div id="password_menu_active" class="password_menu"><div class="submenu_title"><fmt:message key="reset.password"/></div></div>
                        </stripes:link>
                </li>
                <c:if test="${s:getPropertyWithDefault('env.scp.me2u.enabled','true') == 'true' || s:setContains(s:getPropertyAsSet('env.uc.diaspora.icps'), actionBean.getOrganistionIdOfUserInSession())}">
                    <li>
                        <stripes:link href="/Account.action" event="displayTransferPage">
                            <div id="me2u_menu_active" class="me2u_menu"><div class="submenu_title"><fmt:message key="scp.account.transfer"/></div></div>
                        </stripes:link>
                    </li>
                </c:if>
                <c:if test="${s:getPropertyWithDefault('env.scp.show.sim.verification.enabled','false') == 'true'}">
                    <li>
                        <stripes:link href="/Account.action" event="showVerifySimsPage">
                            <div id="sim_verify_menu_active" class="sim_verify_menu"><div class="submenu_title"><fmt:message key="scp.verify.simcards"/></div></div>
                        </stripes:link>
                    </li>
                </c:if>
                <c:if test="${s:getPropertyWithDefault('env.scp.refer.a.friend.enabled','false') == 'true'}">
                    <li>
                        <stripes:link href="/SCPTroubleTicket.action" event="showSalesLeadReferalPage">
                            <div id="saleslead_referal_menu_active" class="saleslead_referal_menu"><div class="submenu_title"><fmt:message key="scp.refer.a.friend"/></div></div>
                            </stripes:link>
                    </li>
                </c:if>
            </ul>
            <ul id="help_sub_menu" class="help_sub_menu">
                <li>
                    <a href="<fmt:message key="scp.country.website.faq"/>" target="_blank">
                        <div id="faq_menu_active" class="faq_menu"><div class="submenu_title"><fmt:message key="faq"/></div></div>
                    </a>
                </li>
                <li>
                    <stripes:link href="/Customer.action" event="showContactDetails"> 
                        <div id="customercare_menu_active" class="customercare_menu"><div class="submenu_title"><fmt:message key="scp.contact.us"/></div></div>
                        </stripes:link>
                </li>
                <li>
                    <stripes:link href="/SCPTroubleTicket.action" event="addTroubleTicket">
                        <div id="supportdesk_menu_active" class="supportdesk_menu"><div class="submenu_title"><fmt:message key="scp.trouble.ticket.edit"/></div></div>
                        </stripes:link>
                </li>
                <li>
                    <stripes:link href="/SCPTroubleTicket.action" event="retrieveTroubleTickets">
                        <div id="troubletickets_menu_active" class="troubletickets_menu"><div class="submenu_title"><fmt:message key="scp.trouble.ticket.view"/></div></div>
                        </stripes:link>
                </li>
            </ul>

        </div>


    </c:if>
</c:if>
<c:if test="${(pageContext.request.remoteUser == null)}">
    <div class="sixteen columns">
        <div class="logo four columns alpha">
            <img src="images/smile-logo.svg" class="scale-with-grid">
            <ul style="margin-top: 5px;">
                <li>
                    <div><font style="font-size:22px; font-weight: bold;"><fmt:message key="scp.mysmile.welcome.txt.banner"/></font></div>
                </li>
            </ul>
        </div>
        <div class="nav eight columns">
            <ul class="main_menu">
                <c:if test="${s:getPropertyWithDefault('env.scp.xpress.recharge.enabled','false') == 'true'}">
                    <li>
                        <a href="<fmt:message key="scp.xpress.recharge.link"/>">
                            <div class="xpress_recharge_menu">&nbsp;<div class="menu_title"><fmt:message key="scp.xpress.recharge.withspace"/></div></div>
                        </a>
                    </li>
                </c:if>
                <li>
                    <a href="<fmt:message key="scp.header.backto.website"/>">
                        <div class="home_menu">&nbsp;<div class="menu_title_home"><fmt:message key="scp.loginpage.backto.msg"/></div></div>
                    </a>
                </li>
                <li>
                    <a href="<fmt:message key="scp.contact.us.link"/>">
                        <div class="contact_menu">&nbsp;<div class="menu_title_contact"><fmt:message key="scp.contact.us"/></div></div>
                    </a>
                </li>
            </ul>
            <ul class="main_menu">
                <ul class="main_menu">
                    <c:if test="${(pageContext.request.remoteUser == null)}">
                        <c:if test="${s:getPropertyWithDefault('env.scp.xpress.recharge.enabled','false') == 'true'}">
                            <div class="xpress_recharge">
                                <fmt:message key="scp.xpress.recharge.topmenu.link.txt"/> <a href="<fmt:message key="scp.xpress.recharge.link"/>"><fmt:message key="scp.xpress.recharge"/></a>
                            </div>
                        </c:if>
                    </c:if>
                </ul>
            </ul>

        </div>
    </div>
</c:if>
