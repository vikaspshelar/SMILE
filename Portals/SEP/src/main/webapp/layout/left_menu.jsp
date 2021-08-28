<%@page import="com.smilecoms.commons.base.BaseUtils, com.smilecoms.commons.stripes.SmileActionBean"%>
<%@ include file="/include/sep_include.jsp" %>
<div style="float: left" id="my_menu" class="sdmenu">

    <% if (SmileActionBean.getIsIndirectChannelPartner(request)) {%>
    <c:set var="isInICPRole" value="true"/>
    <%} else {%>
    <c:set var="isInICPRole" value="false"/>
    <% }%>

    <% if (SmileActionBean.getIsDeliveryPerson(request)) {%>
    <c:set var="isDeliveryPerson" value="true"/>
    <%} else {%>
    <c:set var="isDeliveryPerson" value="false"/>
    <% }%>


    <c:if test="${!isDeliveryPerson}">

        <div>
            <span><fmt:message key="customers"/></span>       
            <stripes:link href="/Customer.action" event="showSearchCustomer"><fmt:message key="search.customer"/></stripes:link>
            <stripes:link href="/Customer.action" event="showAddCustomerWizard"><fmt:message key="add.customer.wizard"/></stripes:link>                    
            <c:if test="${isInICPRole == 'false'}">
                <stripes:link href="/TroubleTicket.action" event="startWizard">Support Customer</stripes:link>  
                <stripes:link href="/Customer.action" event="showSendBulkEmail">Send Bulk Email</stripes:link>
                <stripes:link href="/Customer.action" event="showSendBulkSMS">Send Bulk SMS</stripes:link>
                <stripes:link href="/Customer.action" event="showSendBulkNotification">Bulk App Notification</stripes:link>
                <c:if test="${s:getPropertyWithDefault('env.customer.kycstatus.display', false)}">
                    <stripes:link href="/Customer.action" event="showPendingKYC">KYC Verification</stripes:link>
                    <% if (BaseUtils.getBooleanProperty("env.customer.mandatory.kyc.enable", false)) {%>
                    <stripes:link href="/Customer.action" event="showMandatoryKYC">Mandatory KYC</stripes:link>
                    <% }%>
                     <% if (BaseUtils.getBooleanProperty("env.verify.customers.nin", false)) {%>
                        <stripes:link href="/Customer.action" event="showCustomerPendingForNINVerification">Verify Customers NIN</stripes:link>                        
                    <% }%>
                        <% if (BaseUtils.getBooleanProperty("env.sim.swap", false)) {%>
                        <stripes:link href="/Customer.action" event="showConsentForm">View Consent Form's</stripes:link>
                    <% }%>
                        
                    <stripes:link href="/Customer.action" event="quickViewKYC">KYC Quick View</stripes:link>
                </c:if>                
            </c:if>
            <% if (BaseUtils.getBooleanProperty("env.show.compare.customer.nimc.data", false)) {%>
                <stripes:link href="/Customer.action" event="showCompareCustomerRegulatorData">KYC vs Reg Data</stripes:link>
                <stripes:link href="/Customer.action" event="retrieveKycProducts">Sales KYC Verify</stripes:link>
                <stripes:link href="/Customer.action" event="showNinAccounts">View Accounts by NIN</stripes:link>
            <% }%>
            <% if (BaseUtils.getBooleanProperty("env.customer.details.other.mno.display", false)) {%>
                    <stripes:link href="/Customer.action" event="showCustomersOtherMNO">Search Customer - Other MNO</stripes:link>
            <% }%>
        </div>
        <div>
            <span><fmt:message key="organisations"/></span>       
            <stripes:link href="/Customer.action" event="showSearchOrganisation"><fmt:message key="search.organisation"/></stripes:link>
            <stripes:link href="/Customer.action" event="showAddOrganisationWizard"><fmt:message key="add.organisation.wizard"/></stripes:link>
            </div>
            <div>
                <span><fmt:message key="accounts"/></span>
            <stripes:link href="/Account.action" event="showSearchAccount"><fmt:message key="search.account"/></stripes:link>
            <c:if test="${(isInICPRole == 'false')}">
                <stripes:link href="/Account.action" event="showCreateAccount"><fmt:message key="create.account"/></stripes:link>
            </c:if>
            <stripes:link href="/Account.action" event="showTransfer"><fmt:message key="balance.transfer"/></stripes:link>            
            </div>
        <c:if test="${(isInICPRole == 'false')}">
            <div>
                <span><fmt:message key="sales"/></span>
                <stripes:link href="/Sales.action" event="searchSales"><fmt:message key="search.sales"/></stripes:link>
                <stripes:link href="/Sales.action" event="showMakeSaleWizard"><fmt:message key="make.sale"/></stripes:link>
                <stripes:link href="/Sales.action" event="showCashInSelection"><fmt:message key="cash.in"/></stripes:link>
                <stripes:link href="/Sales.action" event="showApprovePromotionCode">Approve Promo Code</stripes:link>
                </div>
                <div>
                    <span><fmt:message key="products"/></span>
                <stripes:link href="/ProductCatalog.action" event="showSearchProductInstance"><fmt:message key="search.product.instance"/></stripes:link>
                <stripes:link href="/ProductCatalog.action" event="showProductCatalog"><fmt:message key="show.product.catalog"/></stripes:link>
                <stripes:link href="/ProductCatalog.action" event="showUnitCreditCatalog"><fmt:message key="show.unit.credit.catalog"/></stripes:link>
                </div>
            <div>
                    <span>Campaigns</span>
                <stripes:link href="/Campaign.action" event="showStoreCampaignData">Store Campaign Data</stripes:link>        
                </div> 
                <div>
                    <span>Workflow</span>
                <stripes:link href="/Workflow.action" event="showProcesses">Processes</stripes:link>
                <stripes:link href="/Workflow.action" event="showTasks">Tasks</stripes:link>
                </div> 
                <div>
                    <span><fmt:message key="sim.management"/></span>
                <stripes:link href="/SIM.action" event="showProvisionSIM"><fmt:message key="provision.sim"/></stripes:link>
                <c:choose>
                    <c:when test="${s:getPropertyWithDefault('env.sim.swap', false)}">
                        <stripes:link href="/Customer.action" event="showCustomerDataFromKYCData"><fmt:message key="sim.swap"/></stripes:link>     
                    </c:when>
                    <c:otherwise>
                        <stripes:link href="/SIM.action" event="showPerformSIMSwap"><fmt:message key="sim.swap"/></stripes:link>     
                    </c:otherwise>
                </c:choose>
                <% if (BaseUtils.getBooleanProperty("env.sim.compliance.check.display", false)) {%>
                    <stripes:link href="/SIM.action" event="showCheckSimCompliance"><fmt:message key="sim.compliance"/></stripes:link>
                <% }%>
                <% if (BaseUtils.getProperty("env.country.name").equals("Tanzania")) {%>
                    <stripes:link href="/SIM.action" event="showVerifyRegulatorSim"><fmt:message key="sim.verify"/></stripes:link>
                <% }%>                    
                <% if (BaseUtils.getBooleanProperty("env.imei.check.display", false)) {%>
                    <stripes:link href="/SIM.action" event="showImeiStatusCheck"><fmt:message key="imei.status"/></stripes:link>
                    <stripes:link href="/SIM.action" event="showImeiStatusChange"><fmt:message key="imei.change"/></stripes:link>
                <% }%>  
                </div> 
                <div>
                    <span><fmt:message key="ims.management"/></span>
                <stripes:link href="/IMS.action" event="showRetrieveHSSData"><fmt:message key="view.hss.data"/></stripes:link>
                <stripes:link href="/IMS.action" event="showInterconnectUploadRateCard"><fmt:message key="upload.interconnect.rate.card"/></stripes:link>
                </div> 
            <% if (BaseUtils.getBooleanProperty("env.show.voucher.management.menu")) {%>
            <div>
                <span><fmt:message key="prepaid.voucher.management"/></span>
                <stripes:link href="/Account.action" event="showCreateStrips"><fmt:message key="create.strips"/></stripes:link>
                <stripes:link href="/Account.action" event="showBatchUpdateStrips"><fmt:message key="batch.update.strips"/></stripes:link>
                <stripes:link href="/Account.action" event="showViewStrip"><fmt:message key="view.strip"/></stripes:link>
                <stripes:link href="/Account.action" event="showResendStrips"><fmt:message key="resend.strips"/></stripes:link>
                </div>
            <% }%>
            <div>
                <span><fmt:message key="administration"/></span>
                <stripes:link href="/Property.action" event="refreshResourceBundleCache">Refresh Strings</stripes:link> 
                <stripes:link href="/Property.action" event="refreshPropertyCache">Refresh Properties</stripes:link>   
                 <% if (BaseUtils.getProperty("env.country.name").equals("Nigeria")) {%>
                    <stripes:link href="/Property.action" event="refreshKitsFromX3">Refresh X3Inventory Kits</stripes:link>
                <%}%>
                <stripes:link href="/Property.action" event="sendAllTestMessages">Test Trigger Messages</stripes:link>
                <stripes:link href="/TroubleTicket.action" event="showLoadMindMap">Upload Support Tree</stripes:link>     
                <stripes:link href="/Ticker.action" event="showEditTicker">Update Ticker</stripes:link>
                <stripes:link href="/Property.action" event="showManagePermissions">Manage Permissions</stripes:link>
                <stripes:link href="/Property.action" event="showRunGeneralQuery">General Queries</stripes:link>
                <stripes:link href="/Property.action" event="showRunGeneralTask">General Tasks</stripes:link>
                <stripes:link href="/Customer.action" event="allowTracking">Allow Tracking</stripes:link>
                <stripes:link href="/Sales.action" event="showUploadBankStatement">Load Statement</stripes:link>
                <% if (BaseUtils.getBooleanProperty("env.customer.verify.with.nira", false)) {%>
                    <stripes:link href="/Customer.action" event="showChangeNiraPassword">Change NIRA Password</stripes:link>
                <% }%>
            </div>
        </c:if>
        <c:if test="${(isInICPRole == 'true')}">
            <div>
                <span><fmt:message key="sales"/></span>
                <stripes:link href="/Sales.action" event="searchSales"><fmt:message key="search.sales"/></stripes:link>
                </div>
        </c:if>
        <div>
            <span><fmt:message key="stock"/></span>
            <stripes:link href="/Sales.action" event="showStockLocation"><fmt:message key="view.stock.location"/></stripes:link>
            </div>
            <div>
                <span><fmt:message key="Other"/></span>
            <c:if test="${isInICPRole == 'false'}">
                <c:choose>
                    <c:when test="${empty actionBean.loggedInCCAgentExtension}">
                        <stripes:link href="/Callcentre.action" event="showCallCentreLogin"><fmt:message key="callcentre.login"/></stripes:link>     
                    </c:when>
                    <c:otherwise>
                        <stripes:link href="/Callcentre.action" event="callCentreLogout"><fmt:message key="callcentre.logout"/></stripes:link>     
                    </c:otherwise>
                </c:choose>
                <stripes:link href="/TroubleTicket.action" event="showSalesLeadUsernamePage"><fmt:message key="tt.saleslead.tickets"/></stripes:link>
            </c:if>
            <c:if test="${isInICPRole == 'true'}"><!-- Start of ICP check roles -->
                <stripes:link href="/TroubleTicket.action" event="showCreateIssueForICPPage">Trouble Tickets</stripes:link>
            </c:if>
            <stripes:link href="/Customer.action" event="showChangePassword">Change Password</stripes:link> 
            <stripes:link href="/Login.action" event="logOut"><fmt:message key="log.out"/></stripes:link>    
            </div> 

    </c:if>

    <c:if test="${isDeliveryPerson}">
        <div>
            <span>KYC</span>       
            <stripes:link href="/Customer.action" event="showSearchCustomer"><fmt:message key="search.customer"/></stripes:link>
            </div>
            <div>
                <span><fmt:message key="Other"/></span>
            <stripes:link href="/Customer.action" event="showChangePassword">Change Password</stripes:link> 
            <stripes:link href="/Login.action" event="logOut"><fmt:message key="log.out"/></stripes:link>    
            </div> 
    </c:if>


</div>