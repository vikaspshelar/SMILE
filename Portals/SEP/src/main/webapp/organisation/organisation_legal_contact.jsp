<%-- 
    Document   : organisation_legal_contact
    Created on : 29 Mar, 2021, 3:44:37 PM
    Author     : user
--%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="organisation.legal.contact"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">

            var nationality = "";
            var checkRequiredDocumentsRegEx = "";
            var missingDocumentTypesErrorMessageResource = "";

            var regexp = "validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/ ,'emptynotok')";
            var legalContactTypeValidationRule = "${s:getValidationRule('customers.legalContactType','validate(this,\\\'^.{1,50}$\\\',\\\'emptynotok\\\')')}";
            var legalContactIdValidationRule = "${s:getValidationRule('legalContactId','validate(this,\'^.{2,50}$\',\'emptynotok\')')}";
            var organisationIdValidationRule = "${s:getValidationRule('organisationId','validate(this,\'^[0-9]{1,8}$\',\'emptynotok\')')}";
            var ninValidationRule = "${s:getValidationRule('nin','validate(this,\'^.{1,11}$\',\'emptynotok\')')}";
            var firstNameValidationRule = "${s:getValidationRule('firstName','validate(this,\'^.{1,50}$\',\'emptynotok\')')}";
            var lastNameValidationRule = "${s:getValidationRule('lastName','validate(this,\'^.{1,50}$\',\'emptynotok\')')}";
            var emailValidationRule = "validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/ ,'emptynotok')";
            var telNumberValidationRule = "${s:getValidationRule('telNumber', 'validate(this, \'^[+()0-9 -]{10,15}$\',\'emptynotok\')')}";
            var isNinVerifiedValidationRule = "${s:getValidationRule('nin.verified','validate(this,\'^.{1,50}$\',\'emptynotok\')')}";

            var strLegalContactType = '<fmt:message key="organisation.legalContactType"/>';
            var legalContactId = '<fmt:message key="organisation.legalContactId"/>';
            var organisationId = '<fmt:message key="organisation.organisationId"/>';
            var nin = '<fmt:message key="organisation.nin"/>';
            var firstName = '<fmt:message key="organisation.firstName"/>';
            var lastName = '<fmt:message key="organisation.lastName"/>';
            var email = '<fmt:message key="organisation.email"/>';
            var telNumber = '<fmt:message key="organisation.telNumber"/>';
            var isNinVerified = '<fmt:message key="organisation.isNinVerified"/>';

            var legalContactTypes = '${s:getPropertyAsList('env.customer.address.types')}';
            var countryName = '${s:getProperty('env.country.name')}';
            var isTownMandatory = ${s:getProperty("env.address.town.mandatory")};

            var z = new Array();

                        <c:forEach items="${s:getPropertyFromSQL('env.country.zones')}" var="zone" varStatus="loop">z[${loop.index}] = new Array();
            z[${loop.index}][0] = "${zone[0]}";
                    z[${loop.index}][1] = "${zone[1]}";
                    z[${loop.index}][2] = "${zone[2]}";</c:forEach>

            function populateZones(rowIndex) {
                console.log(onSubmit);
                if (!onSubmit) {
                    var statesDropdownBox = document.getElementById("addressState" + rowIndex);
                    var zonesDropdownBox = document.getElementById("addressZone" + rowIndex);
                    //  Get the selected state ...
                    selectedState = statesDropdownBox.options[statesDropdownBox.selectedIndex].value;
                    // Polulate the zones dropdown with the zones related to the selected state.
                    zonesDropdownBox.options.length = 0;
                    zonesDropdownBox.options[0] = new Option("", "");
                    for (var i = 0; i < z.length; i++) {
                        if (selectedState === z[i][2]) {
                            zonesDropdownBox.options[zonesDropdownBox.options.length] = new Option(z[i][1], z[i][1]);
                        }
                    }
                    zonesDropdownBox.style.display = 'block';
                }
            }
            
            var methodOfIdentification = "";
            var methodOfIdentificationDescr = "";
            <c:if test="${actionBean.customer.KYCStatus !=  'V'}">
            methodOfIdentification = "${actionBean.customer.identityNumberType}";
            methodOfIdentificationDescr = "<fmt:message  key='document.type.${actionBean.customer.identityNumberType}'/>";
            </c:if>

            <c:if test="${not empty actionBean.customer.visaExpiryDate}">
            var visaExpiryDate = "${actionBean.customer.visaExpiryDate}";
            </c:if>


            window.addEventListener("DOMContentLoaded", function () {
                console.log("init start");
                initWebCam();
                console.log("init end");
            }, false);
                        <c:set var="customerClassType" value="${s:getDelimitedPropertyValueMapping('env.customer.classifications', actionBean.customer.classification)}"/>
            <c:set var="documentTypesResourceName" value="env.customer.minor.document.types"/>
                    checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.minor.required.documents.regex', actionBean.context.request)}";
                        <c:set var="missingDocumentTypesErrorMessageResource" value="minor.document.type.missing.error"/>
                               </script>

                                   <stripes:form action="/Customer.action" autocomplete="off" onsubmit="alertValidationErrors()">
                                       <stripes:hidden name="organisation.organisationId" value="${actionBean.organisation.organisationId}"/>
                                       <stripes:select id="userType" name="userType" style="display:none">
                                           <stripes:option value=""></stripes:option>
                                               <c:forEach items="${s:getPropertyAsList('env.leagal.contact.types')}" var="userType" varStatus="loop">                       
                                                   <stripes:option value="${userType}">${userType}</stripes:option>
                                               </c:forEach>
                                       </stripes:select>
                                       <stripes:select name="customer.document.types"  id="customer.document.types" style="display:none">
                                           <stripes:option value=""></stripes:option>
                                           <c:forEach items="${s:getPropertyAsList(documentTypesResourceName)}" var="documentType" varStatus="loop">
                                               <stripes:option value="${documentType}">
                                                   <fmt:message  key="document.type.${documentType}"/>
                                               </stripes:option>
                                           </c:forEach>
                                       </stripes:select>

                                       <div id="entity">              
                                           <table  class="clear" id="tblLegalContacts">  
                                               <tbody>
                                               <c:forEach items="${actionBean.organisationLegalContacts}" varStatus="loop">                    
                                                   <tr style="border-top:1px solid green">
                                                       
                                                       <td><fmt:message key="organisation.legalContactType"/>:</td>                                  
                                                   <td>
                                                   <c:set var="defaultRule" value="validate(this,\\\'^.{1,50}$\\\',\\\'emptynotok\\\')"/>
                                                   <stripes:select id="userType${loop.index}" name="actionBean.organisationLegalContacts[${loop.index}].legalContactType" onchange="${s:getValidationRule('customers.type',defaultRule)}">
                                                       <stripes:option value="${actionBean.organisationLegalContacts[loop.index].legalContactType}">${actionBean.organisationLegalContacts[loop.index].legalContactType}</stripes:option>                                                       
                                                   </stripes:select>
                                                   </td>
                                                   <stripes:form action="/Customer.action" autocomplete="off" onsubmit="alertValidationErrors()">
                                                        <td align="center">
                                                            <stripes:hidden name="organisation.organisationId" value="${actionBean.organisationLegalContacts[loop.index].organisationId}"/>
                                                                <input type='hidden' name="removeiccid" id="removeiccid" value="${actionBean.organisationLegalContacts[loop.index].iccid}"/>
                                                                <input type='hidden' name="removenin" id="removenin" value="${actionBean.organisationLegalContacts[loop.index].nin}"/>
                                                            <stripes:submit name="removeLegalContact"/><br/>
                                                            
                                                            <c:if test="${actionBean.organisationLegalContacts[loop.index].legalContactType.trim().equalsIgnoreCase('Primary Contact')}">
                                                                <c:choose>
                                                                    <c:when test="${actionBean.organisationLegalContacts[loop.index].isKycVerified.trim().equalsIgnoreCase('Y')}">
                                                                        <span style="font-weight:900;color:green">KYC Verified</span>
                                                                    </c:when>
                                                                    <c:otherwise>
                                                                        <stripes:hidden name="organisation.organisationId" value="${actionBean.organisationLegalContacts[loop.index].organisationId}"/>                                                                
                                                                            <input type='hidden' name="updateiccid" id="updateiccid" value="${actionBean.organisationLegalContacts[loop.index].iccid}"/>
                                                                            <input type='hidden' name="updatenin" id="updatenin" value="${actionBean.organisationLegalContacts[loop.index].nin}"/>
                                                                        <stripes:submit name="kycLegalContact"/>
                                                                    </c:otherwise>
                                                                    
                                                                
                                                                </c:choose>
                                                            
                                                            </c:if>
                                                            
                                                        </td>
                                                   </stripes:form>   
                                                   </tr> 
                                                   
                                                   <tr style='display: none'>
                                                       <td><fmt:message key="organisation.organisationId"/>:</td>
                                                   <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                                                   <td><input type='hidden' readonly="true" id="organisationId${loop.index}" name="actionBean.organisationLegalContacts[${loop.index}].organisationId" maxlength='100' size='40' value="${actionBean.organisationLegalContacts[loop.index].organisationId}" onkeyup="${s:getValidationRule('organisationId', defaultRule)}"/></td>
                                                    
                                                    <stripes:form action="/Customer.action" autocomplete="off" onsubmit="alertValidationErrors()">
                                                        <td style='vertical-align: top; margin-top:auto'>
                                                            
                                                        </td>
                                                   </stripes:form>  

                                                    </tr>
                                                   <tr>
                                                       <td><fmt:message key="organisation.nin"/>:</td>
                                                   <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                                                   <td><input type='text' readonly="true" id="nin${loop.index}" name="actionBean.organisationLegalContacts[${loop.index}].nin" maxlength='100' size='40' value="${actionBean.organisationLegalContacts[loop.index].nin}" onkeyup="${s:getValidationRule('nin', defaultRule)}"/></td>
                                                   </tr>
                                                   <tr>
                                                       <td><fmt:message key="organisation.firstName"/>:</td>
                                                   <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                                                   <td><input type='text' readonly="true" id="firstName${loop.index}" name="actionBean.organisationLegalContacts[${loop.index}].firstName" maxlength='100' size='40' value="${actionBean.organisationLegalContacts[loop.index].firstName}" onkeyup="${s:getValidationRule('firstName', defaultRule)}"/></td>
                                                   </tr>
                                                   <tr>
                                                       <td><fmt:message key="organisation.lastName"/>:</td>
                                                   <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                                                   <td><input type='text' readonly="true" id="lastName${loop.index}" name="actionBean.organisationLegalContacts[${loop.index}].lastName" maxlength='100' size='40' value="${actionBean.organisationLegalContacts[loop.index].lastName}" onkeyup="${s:getValidationRule('lastName', defaultRule)}"/></td>
                                                   </tr>
                                                   <tr>
                                                       <td>SIM Ref Number</td>
                                                   <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                                                   <td><input type='text' readonly="true" id="iccid${loop.index}" name="actionBean.organisationLegalContacts[${loop.index}].iccid" maxlength='100' size='40' value="${actionBean.organisationLegalContacts[loop.index].iccid}" onkeyup="${s:getValidationRule('lastName', defaultRule)}"/></td>
                                                   </tr>
                                                   <tr>
                                                   <c:set var="defaultRule" value="validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/ ,'emptynotok')"/>
                                                   <td><fmt:message key="organisation.email"/>:</td>
                                                   <td><input type='text' readonly="true" id="lastName${loop.index}" name="actionBean.organisationLegalContacts[${loop.index}].email" maxlength='100' size='40' value="${actionBean.organisationLegalContacts[loop.index].email}" onkeyup="${s:getValidationRule('email',defaultRule)}"/></td>
                                                   </tr> 
                                                   <tr>
                                                   <c:set var="defaultRule" value="validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/ ,'emptynotok')"/>
                                                   <td><fmt:message key="organisation.telNumber"/>:</td>
                                                        <td><input readonly="true" type='text' id="telNumber${loop.index}" name="actionBean.organisationLegalContacts[${loop.index}].telNumber" maxlength='100' size='40' value="${actionBean.organisationLegalContacts[loop.index].telNumber}" onkeyup="${s:getValidationRule('telNumber',defaultRule)}"/><br/><br/></td>
                                                   </tr>
                                                   
                                               </c:forEach>
                                               </tbody>
                                           </table> 
                                         <hr />
                                           <br /> <br />
<!--                                           <input onclick="addRow('tblDocuments', 'customer.customerPhotographs');" type="button" value="Add Document"/> -->
                                           <br />
                                           <table class="clear" width="100%">
                                               <tr>
                                                   <td id="lblErrorMessages"> </td>
                                               </tr>
                                           </table>
                                           <p  class="button">
                                               <stripes:submit name="addLegalContact"/>
                                           </p>                
                                       </div>
                                   </stripes:form>    
                                   </stripes:layout-component>
                                   </stripes:layout-render>
