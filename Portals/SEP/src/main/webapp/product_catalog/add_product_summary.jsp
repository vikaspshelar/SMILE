<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="product.provisioning.summary"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">
            <c:if test="${s:getPropertyWithDefault('env.customer.verify.with.nida','false') == 'true'}">
                removeFingerprints();  // For Tanzania - Remove any fingerprints captured
            </c:if>
        </script>
        
        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>
                <fmt:message key="product.summary.for">
                    <fmt:param value="${actionBean.productSpecification.name}"/>
                    <fmt:param value="${actionBean.customer.firstName} ${actionBean.customer.lastName} "/>
                </fmt:message>
                </td>                                            
                </tr>                
            </table>
            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="product.specific.info"/></b></td>
                </tr>
                <tr>
                    <td><fmt:message key="product.name"/>:</td>
                <td>${actionBean.productSpecification.name}</td>
                </tr>
                <tr>
                    <td><fmt:message key="product.description"/>:</td>
                <td>${actionBean.productSpecification.description}</td>
                </tr>
                <tr>
                    <td><fmt:message key="segment"/>:</td>
                <td>${actionBean.productOrder.segment}</td>
                </tr>
                <tr>
                    <td>Referral Code:</td>
                    <td>${actionBean.productOrder.referralCode}</td>
                </tr>
                <tr>
                    <td><fmt:message key="provision.as.kit"/>:</td>
                <td>${actionBean.productOrder.kitItemNumber}</td>
                </tr>
                <tr>
                    <td><fmt:message key="kits.device.serial.number"/>:</td>
                <td>${actionBean.productOrder.deviceSerialNumber}</td>
                </tr>
                <c:if test="${actionBean.productOrder.organisationId > 0}">
                    <tr>
                        <td><fmt:message key="organisation"/>:</td>
                    <td>${s:getEntryInList(actionBean.customer.customerRoles,'getOrganisationId',actionBean.productOrder.organisationId).roleName} at ${s:getEntryInList(actionBean.customer.customerRoles,'getOrganisationId',actionBean.productOrder.organisationId).organisationName}</td>
                    </tr>
                </c:if>
            </table>


            <c:if test="${actionBean.publicIdentityCostCents > 0}">
                <table class="clear">
                    <tr>
                        <td>
                            <div class="error">
                                <fmt:message key="product.summary.public.identity.cost">
                                    <fmt:param value="${s:convertCentsToCurrencyLongRoundHalfEven(actionBean.publicIdentityCostCents)}"/>
                                </fmt:message>
                            </div> 
                        </td>
                    </tr>
                </table>
            </c:if>



            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="service.instances"/></b></td>
                </tr>
            </table>

            <table class="green" width="99%">
                <tr>
                    <th>Service Name</th>
                    <th>Charging</th>
                    <th>Settings</th>
                </tr>
                <c:forEach items="${actionBean.productOrder.serviceInstanceOrders}" var="sio">
                    <c:if test="${sio.action == 'CREATE'}">
                        <c:forEach items="${actionBean.productSpecification.productServiceSpecificationMappings}" var="m">
                            <c:if test="${m.serviceSpecification.serviceSpecificationId == sio.serviceInstance.serviceSpecificationId}">
                                <c:set var="mapping" value="${m}"/>
                            </c:if>
                        </c:forEach>
                        <tr>
                            <td>${mapping.serviceSpecification.name}</td>
                            <td>
                        <fmt:message key="rate.plan.and.account">
                            <fmt:param value="${mapping.ratePlanId}"/>
                            <fmt:param value="${fn:trim(actionBean.account.accountId)}"/>
                        </fmt:message>
                        </td>
                        <td>
                            <table class="clear">
                                <c:forEach items="${sio.serviceInstance.AVPs}" var="avp" varStatus="loop2">
                                    <s:avp-display-row avp="${avp}"/>
                                </c:forEach>
                            </table>
                        </td>
                        </tr>
                    </c:if>
                </c:forEach>

            </table>    


            <stripes:form action="/ProductCatalog.action" autocomplete="off">
                
                
                
                <table class="clear">
                    <tr>
                        <td><fmt:message key="provision.friendly.name"/>:</td>
                    <td><stripes:text name="productOrder.friendlyName" maxlength="200" size="50"/></td>
                    </tr>
                    <tr>
                        <td>Allow Pending Payment SIM:</td>
                        <td><stripes:checkbox  name="productOrder.allowPendingSIMSale" checked="false"/></td>
                    </tr>
                    <c:if test="${s:hasPermissions('BULK_PROVISION_PRODUCT_INSTANCE', actionBean.context)}">
                        <tr>
                            <td>Additional ICCID's to bulk provision with same configuration:</td>
                            <td><stripes:textarea name="bulkICCIDs" cols="25" rows="5"></stripes:textarea></td>
                        </tr>
                    </c:if>
                </table>
                
                          
                <input type="hidden" name="currentServiceSpecificationIndex" value="${actionBean.currentServiceSpecificationIndex}"/>
                <span class="button">
                    <stripes:submit name="collectServiceInstanceDataForProductInstallBack"/>
                </span>
                <span class="button">
                    <stripes:submit name="doProductProvisioning"/>
                </span>  
                <stripes:wizard-fields/>
            </stripes:form>

        </div>

    </stripes:layout-component>
</stripes:layout-render>

