<%-- 
    Document   : add_addresses
    Created on : 22 Jan 2013, 3:16:35 PM
    Author     : lesiba
--%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.contract"/>    
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">

            var useWebCam = false; // Global variable to control the use of web cam .... (no webcam needed for Contract documents).

            var today = new Date().getFullYear();

            $j(document).ready(function () {
                $j('#datePicker1').datepicker({dateFormat: 'yy/mm/dd 00:00:00', showOn: 'button', buttonText: "..", changeYear: true, changeMonth: true, onSelect: function () {
                        validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01]) ([012][0-9])[:]([0-5][0-9]):([0-5][0-9])$', 'emptynotok');
                    }});
            });

            $j(document).ready(function () {
                $j('#datePicker2').datepicker({dateFormat: 'yy/mm/dd 00:00:00'
                    , showOn: 'button', buttonText: "..", changeYear: true, changeMonth: true, onSelect: function () {
                        validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01]) ([012][0-9])[:]([0-5][0-9]):([0-5][0-9])$', 'emptynotok');
                    }});
            });

        </script>

        <div id="entity">
            <table class="entity_header">                
                <tr>
                    <c:choose>
                        <c:when test="${actionBean.customer != null}">
                            <td>
                                <fmt:message key="customer"/>:  ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                            </td>
                            <td align="right">                       
                                <stripes:form action="/Customer.action">                                
                                    <stripes:select name="entityAction">
                                        <stripes:option value="retrieveCustomer"><fmt:message key="manage.customer"/></stripes:option>
                                        <stripes:option value="manageContracts"><fmt:message key="manage.contracts"/></stripes:option>
                                    </stripes:select>
                                    <stripes:hidden name="customerQuery.customerId" value="${actionBean.address.customerId}"/>
                                    <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                                    <stripes:hidden name="customer.firstName" value="${actionBean.customer.firstName}"/>
                                    <stripes:hidden name="customer.lastName" value="${actionBean.customer.lastName}"/>
                                    <stripes:submit name="performEntityAction"/>
                                </stripes:form>
                            </td>
                        </c:when>
                        <c:otherwise>
                            <td>
                                <fmt:message key="organisation"/>: ${actionBean.organisation.organisationName}
                            </td>
                            <td align="right">
                                <stripes:form action="/Customer.action">
                                    <stripes:select name="entityAction">
                                        <stripes:option value="retrieveOrganisation"><fmt:message key="manage.organisation"/></stripes:option>
                                        <stripes:option value="manageContracts"><fmt:message key="manage.contracts"/></stripes:option>
                                    </stripes:select>
                                    <stripes:hidden name="organisation.organisationId" value="${actionBean.organisation.organisationId}"/>
                                    <stripes:hidden name="organisation.organisationName" value="${actionBean.organisation.organisationName}"/>
                                    <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                                    <stripes:submit name="performEntityAction"/>
                                </stripes:form>
                            </td>
                        </c:otherwise>
                    </c:choose>
                </tr>
            </table>

            <stripes:form action="/Customer.action" id="frm_edit" method="POST" onsubmit="onSubmit=true; return alertValidationErrors()" autocomplete="off">
                <stripes:select name="customer.document.types"  id="customer.document.types" style="display:none">
                    <stripes:option value=""></stripes:option>
                    <c:forEach items="${s:getPropertyAsList('env.contract.document.types')}" var="documentType" varStatus="loop">
                        <stripes:option value="${documentType}">
                            <fmt:message  key="document.type.${documentType}"/>
                        </stripes:option>
                    </c:forEach>
                </stripes:select>
                <c:choose>
                    <c:when test="${actionBean.customer != null}">
                        <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                        <stripes:hidden name="contract.customerProfileId" value="${actionBean.customer.customerId}"/>
                        <stripes:hidden name="customerQuery.customerId" value="${actionBean.address.customerId}"/>
                        <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                        <stripes:hidden name="customer.firstName" value="${actionBean.customer.firstName}"/>
                        <stripes:hidden name="customer.lastName" value="${actionBean.customer.lastName}"/>
                        <stripes:hidden name="contract.organisationId" value="0"/>
                    </c:when>
                    <c:otherwise>
                        <stripes:hidden name="organisation.organisationId" value="${actionBean.organisation.organisationId}"/>
                        <stripes:hidden name="organisation.organisationName" value="${actionBean.organisation.organisationName}"/>
                        <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                        <stripes:hidden name="contract.organisationId" value="${actionBean.organisation.organisationId}"/>
                        <stripes:hidden name="contract.customerProfileId" value="0"/>
                    </c:otherwise>
                </c:choose>                

                <table class="clear" width="100%">
                    <tr>
                        <td><fmt:message key="contract.name"/>:</td>
                        <td><stripes:text name="contract.contractName" maxlength="50" size="20" onkeyup="validate(this,'^.{2,50}$','emptynotok')"/></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="contract.startdatetime"/>:</td>
                        <td><stripes:text name="contract.contractStartDateTime" readonly="false" id="datePicker1" size="20" onkeyup="validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01]) ([012][0-9])[:]([0-5][0-9]):([0-5][0-9])$', 'emptydatenotok')"  /></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="contract.enddatetime"/>:</td>
                        <td><stripes:text name="contract.contractEndDateTime" readonly="false" id="datePicker2" size="20" onkeyup="validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01]) ([012][0-9])[:]([0-5][0-9]):([0-5][0-9])$', 'emptydatenotok')"  /></td>
                    </tr>
                    <tr>
                        <td>Payment Method:</td>
                        <td>
                            <stripes:select name="contract.paymentMethod">
                                <c:forEach items="${s:getPropertyAsList('env.contract.payment.methods')}" var="method">
                                    <stripes:option value="${method}">${method}</stripes:option>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="contract.creditaccountnumber"/>:</td>
                        <td><stripes:text name="contract.creditAccountNumber"  maxlength="50" size="20" onkeyup="validate(this,'^.{3,20}$','emptyok')"/></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="account.accountId"/>:</td>
                        <td><stripes:text size="10" maxlength="10" name="contract.accountId" onkeyup="validate(this,'^[0-9]{10,10}$','emptyok')"/></td>
                    </tr>
                </table>

                <table>
                    <tr>
                        <td colspan="2">
                            <br/>
                            <b><fmt:message key="contract.fulfilment.allowed.items"/>:</b>
                            <br/>
                        </td>
                    </tr>
                    <c:forEach items="${s:getPropertyAsList('env.contract.allowed.items')}" var="itemNumber" varStatus="loop">
                        <tr>
                            <td>${itemNumber} ${s:getUnitCreditNameByItemNumber(itemNumber)}:</td>
                            <td><stripes:checkbox name="fulfilmentItemsAllowed" value='${itemNumber}'/></td>                          
                        </tr>  
                    </c:forEach>
                </table> 

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
                                    <c:when test="${s:getListSize(actionBean.organisation.customerRoles) > 0}">
                                        <tr>
                                            <th><fmt:message key="customer.name"/></th>
                                            <th><fmt:message key="role.name"/></th>
                                            <th><fmt:message key="select.customer"/></th>
                                        </tr>
                                        <c:forEach items="${actionBean.organisation.customerRoles}" var="role" varStatus="loop">
                                            <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                                <td><stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                                        <stripes:param name="customer.customerId" value="${role.customerId}"/>
                                                        ${role.customerName}
                                                    </stripes:link></td>
                                                <td>${role.roleName}</td>
                                                <td><stripes:checkbox name="staffMembersAllowed" value='${role.customerId}'/></td>
                                            </tr>
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
                <table>
                    <tr>
                        <td colspan="2">
                            <br/>
                            <b><fmt:message key="scanned.documents"/>:</b>
                            <br/>
                        </td>
                    </tr>
                    <tr>
                        <td  colspan="2">
                            <table class="clear"  width="100%" id="tblDocuments">
                                <tbody>

                                </tbody>           
                            </table>                                
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <br/>
                            <input onclick="javascript:addRow('tblDocuments', 'photographs')" type="button" value="Add Document"/> 
                            <br/>
                            <label  id="lblErrorMessages"></label> 
                            <br/>
                        </td>
                    </tr>

                </table>

                <span class="button">                                
                    <stripes:submit name="addContract"/>
                </span>
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
