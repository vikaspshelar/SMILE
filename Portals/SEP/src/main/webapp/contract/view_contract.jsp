<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="contract.view.info"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <c:set var="contract" value="${actionBean.contract}"/>
            <table class="entity_header">
                <td><fmt:message key="contract.view.info"/>: ${contract.contractName}</td>                        
                <td align="right">                       
                    <stripes:form action="/Customer.action">                                
                        <stripes:select name="entityAction">
                            <stripes:option value="manageContracts"><fmt:message key="manage.contracts"/></stripes:option>
                            <stripes:option value="showEditContract">Edit</stripes:option>
                            <stripes:option value="deleteContract">Delete</stripes:option>
                            <stripes:option value="showContractsSales"><fmt:message key="view.contracts.sales"/></stripes:option>
                        </stripes:select>
                        <stripes:hidden name="organisationQuery.organisationId" value="${contract.organisationId}"/>
                        <stripes:hidden name="organisation.organisationId" value="${contract.organisationId}"/>
                        <stripes:hidden name="contract.contractId" value="${contract.contractId}"/>
                        <stripes:submit name="performEntityAction"/>
                    </stripes:form>
                </td>
                </tr>
            </table>
            <stripes:form action="/Customer.action"> 


                <stripes:hidden name="contract.contractId" value="${contract.contractId}"/>
                <c:choose>
                    <c:when test="${actionBean.customer != null}">
                        <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                        <stripes:hidden name="contract.customerId" value="${actionBean.customer.customerId}"/>
                    </c:when>
                    <c:otherwise>
                        <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                        <stripes:hidden name="contract.organisationId" value="${actionBean.organisation.organisationId}"/>                
                        <stripes:hidden name="organisation.organisationId" value="${actionBean.organisation.organisationId}"/>
                    </c:otherwise>
                </c:choose>

                <table class="clear">
                    <tr>
                        <td><fmt:message key="contract.name"/>:</td> 
                        <td>${contract.contractName}</td>                    
                    </tr>
                    <tr>
                        <c:choose>
                            <c:when test="${actionBean.contract.customerProfileId > 0}">
                                <td><fmt:message key="contract.customerprofileid"/>:</td> 
                                <td>${contract.customerProfileId}</td> 
                            </c:when>
                            <c:when test="${actionBean.contract.organisationId > 0}">
                                <td><fmt:message key="contract.organisationid"/>:</td> 
                                <td>${contract.organisationId}</td>                    
                            </c:when>
                        </c:choose>
                    </tr>
                    <tr>
                        <td><fmt:message key="contract.contractid"/>:</td> 
                        <td>${contract.contractId}</td>                    
                    </tr>
                    <tr>
                        <td><fmt:message key="contract.startdatetime"/>:</td> 
                        <td>${s:formatDateLong(contract.contractStartDateTime)}</td>                    
                    </tr>
                    <tr>
                        <td><fmt:message key="contract.enddatetime"/>:</td> 
                        <td>${s:formatDateLong(contract.contractEndDateTime)}</td>                    
                    </tr>
                    <tr>
                        <td><fmt:message key="contract.status"/>:</td> 
                        <td>${contract.status}</td>                    
                    </tr>
                    <tr>
                        <td>Payment Method:</td> 
                        <td>${contract.paymentMethod}</td>                    
                    </tr>
                    <c:if test="${contract.accountId != 0}">
                        <tr>
                            <td>Account Id:</td> 
                            <td>${contract.accountId}</td>                    
                        </tr>
                    </c:if>
                    <tr>
                        <td><fmt:message key="contract.creditaccountnumber"/>:</td> 
                        <td>${contract.creditAccountNumber}</td>                    
                    </tr>
                    <tr>
                        <td><fmt:message key="contract.createddatetime"/>:</td> 
                        <td>${s:formatDateLong(contract.createdDateTime)}</td>                    
                    </tr>
                    <tr>
                        <td><fmt:message key="contract.createdbycustomerid"/>:</td> 
                        <td>${contract.createdByCustomerId}</td>                    
                    </tr>
                    <tr>
                        <td><fmt:message key="contract.lastmodifieddatetime"/>:</td> 
                        <td>${s:formatDateLong(contract.lastModifiedDateTime)}</td>                    
                    </tr>
                    <tr>
                        <td colspan="2">
                            <s:displayWhenReady event="retrieveImagesSnippet" href="/Customer.action" containerId="images" autoLoad="false" displayMessage="Click here to view attached documents">
                                <stripes:param name="contract.contractId" value="${actionBean.contract.contractId}"/>
                            </s:displayWhenReady>  
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <table>
                                <tr>
                                    <td colspan="2">
                                        <br/>
                                        <b><fmt:message key="contract.fulfilment.allowed.items"/>:</b>
                                        <br/>
                                    </td>
                                </tr>
                                <c:forEach items="${s:getPropertyAsList('env.contract.allowed.items')}" var="itemNumber" varStatus="loop">
                                    <c:forEach items="${actionBean.fulfilmentItemsAllowed}" var="allowedItem" varStatus="allowedItemsLoop">
                                        <c:if test="${allowedItem == itemNumber}">
                                            <tr>
                                                <td>${itemNumber} ${s:getUnitCreditNameByItemNumber(itemNumber)}:</td>
                                                <td><stripes:checkbox name="fulfilmentItemsAllowed" disabled="disabled" value='${itemNumber}'/></td>                          
                                            </tr>  
                                        </c:if>
                                    </c:forEach>
                                </c:forEach>
                            </table> 
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <table> 
                                <tr>
                                    <td>
                                        <br/>
                                        <b><fmt:message key="contract.allowed.staff"/>:</b>
                                        <br/>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <table class="green">
                                            <c:choose>
                                                <c:when test="${s:getListSize(actionBean.organisation.customerRoles) > 0 && s:getListSize(actionBean.staffMembersAllowed) > 0 }">
                                                    <tr>
                                                        <th><fmt:message key="customer.name"/></th>
                                                        <th><fmt:message key="role.name"/></th>
                                                    </tr>
                                                    <c:forEach items="${actionBean.organisation.customerRoles}" var="role" varStatus="loop">
                                                        <c:forEach items="${actionBean.staffMembersAllowed}" var="allowed" varStatus="allowedloop">
                                                            <c:set var="idAsString" value="${role.customerId}"/>
                                                            <c:if test="${allowed == idAsString}">
                                                                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                                                    <td><stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                                                            <stripes:param name="customer.customerId" value="${role.customerId}"/>
                                                                            ${role.customerName}
                                                                        </stripes:link></td>
                                                                    <td>${role.roleName}</td>
                                                                </tr>
                                                            </c:if>
                                                        </c:forEach>
                                                    </c:forEach>
                                                </c:when>
                                                <c:otherwise>
                                                    <tr>
                                                        <td>
                                                            <fmt:message key="no.organisation.roles"/>
                                                        </td>
                                                    </tr>
                                                </c:otherwise>
                                            </c:choose>
                                        </table>  
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </stripes:form>            
        </div>
    </stripes:layout-component>
</stripes:layout-render>

