<%-- 
    Document   : add_customer_address
    Created on : Nov 22, 2011, 11:09:48 AM
    Author     : lesibaf
--%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.new.customer.addresses"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">
            var requiredAddresses = new Array();
            var onSubmit = false;

            var addressLine1ValidationRule = "${s:getValidationRule('address.line1','validate(this,\'^.{2,50}$\',\'emptynotok\')')}";
            var addressLine2ValidationRule = "${s:getValidationRule('address.line2','validate(this,\'^.{2,50}$\',\'emptynotok\')')}";
            var addressStateValidationRule = "${s:getValidationRule('address.state','validate(this,\'^.{1,50}$\',\'emptynotok\')')}";
            var addressTownValidationRule = "${s:getValidationRule('address.town','validate(this,\'^.{1,50}$\',\'emptynotok\')')}";
            var addressZoneValidationRule = "${s:getValidationRule('address.zone','validate(this,\'^.{1,50}$\',\'emptynotok\')')}";
            var addressPostalCodeValidationRule = "${s:getValidationRule('address.postalcode','validate(this,\'^[0-9]{4,10}$\',\'emptynotok\')')}";
            var addressTypeValidationRule =${s:getValidationRule('address.type','validate(this,\'^.{1,50}$\',\'emptynotok\')')};
            var isAddressMandatory = ${s:getPropertyWithDefault("env.address.mandatory", "false")};

            <c:forEach items="${s:getPropertyAsList('env.country.required.address')}" var="property" varStatus="loop">
            requiredAddresses[${loop.index}] = "${property}";
            </c:forEach>

            var strAddressLine1 = '<fmt:message key="address.line1"/>';
            var strAddressLine2 = '<fmt:message key="address.line2"/>';
            var strTown = '<fmt:message key="town"/>';
            var strCountry = '<fmt:message key="country"/>';
            var strCode = '<fmt:message key="code"/>';
            var strZone = '<fmt:message key="zone"/>';
            var strState = '<fmt:message key="state"/>';
            var displayPostalCode = '${s:getProperty("env.postalcode.display")}';
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

            </script>

        <stripes:form action="/Customer.action" autocomplete="off" onsubmit="clearErrorMessages(); return ((onBack == true) ? true : (alertValidationErrors() && checkRequiredAddresses()));">
            <stripes:hidden name="customer.warehouseId" value="${actionBean.customer.warehouseId}" />
            <stripes:hidden name="customer.SSOIdentity" value="${actionBean.customer.SSOIdentity}"/>
            <stripes:hidden name="customer.SSODigest" value="${actionBean.customer.SSODigest}"/>
            <stripes:hidden name="customer.identityNumberType" value="${actionBean.customer.identityNumberType}"/>
            <stripes:hidden name="customer.identityNumber" value="${actionBean.customer.identityNumber}"/>
            <stripes:hidden name="customer.cardNumber" value="${actionBean.customer.cardNumber}"/>
            <stripes:hidden name="customer.title" value="${actionBean.customer.title}"/>
            <stripes:hidden name="customer.firstName" value="${actionBean.customer.firstName}"/>
            <stripes:hidden name="customer.middleName" value="${actionBean.customer.middleName}"/>
            <stripes:hidden name="customer.lastName" value="${actionBean.customer.lastName}"/>
            <stripes:hidden name="customer.mothersMaidenName" value="${customer.mothersMaidenName}"/>
            <stripes:hidden name="customer.SSOIdentity" value="${actionBean.customer.SSOIdentity}"/>
            <stripes:hidden name="customer.dateOfBirth" value="${actionBean.customer.dateOfBirth}"/>
            <stripes:hidden name="customer.gender" value="${actionBean.customer.gender}"/>
            <stripes:hidden name="customer.language" value="${actionBean.customer.language}"/>
            <stripes:hidden name="customer.emailAddress" value="${actionBean.customer.emailAddress}"/>
            <stripes:hidden name="customer.alternativeContact1" value="${actionBean.customer.alternativeContact1}"/>
            <stripes:hidden name="customer.alternativeContact2" value="${actionBean.customer.alternativeContact2}"/>
            <stripes:hidden name="customer.referralCode" value="${actionBean.customer.referralCode}"/>
            <stripes:hidden name="customer.optInLevel" value="${actionBean.customer.optInLevel}"/>
            <stripes:hidden name="customer.customerStatus"      value="${actionBean.customer.customerStatus}"/>
            <stripes:hidden name="customer.classification"      value="${actionBean.customer.classification}"/>
            <stripes:hidden name="customer.nationality"  value="${actionBean.customer.nationality}"/>
            <stripes:hidden name="customer.securityGroups[0]" value="${actionBean.customer.securityGroups[0]}"/>
            <stripes:hidden name="customer.passportExpiryDate" value="${actionBean.customer.passportExpiryDate}"/>
            <stripes:hidden name="customer.visaExpiryDate" value="${actionBean.customer.visaExpiryDate}"/>
            <stripes:hidden name="TTIssue.ID" value="${actionBean.TTIssue.ID}"/>
            <stripes:hidden name="customer.KYCStatus" value="${actionBean.customer.KYCStatus}"/>

            <stripes:select id="address.types" name="address.types" style="display:none">
                <stripes:option value=""></stripes:option>
                <c:forEach items="${s:getPropertyAsList('env.customer.address.types')}" var="addresstype" varStatus="loop">
                    <stripes:option value="${addresstype}">${addresstype}</stripes:option>
                </c:forEach>
            </stripes:select>

            <stripes:select id="address.zones" name="address.zones" style="display:none">
                <c:forEach items="${s:getPropertyFromSQL('env.country.zones')}" var="zone">
                    <stripes:option value="${zone[1]}">
                        ${zone[1]}
                    </stripes:option>
                </c:forEach>
            </stripes:select>

            <stripes:select id="address.states" name="address.states" style="display:none">
                <c:forEach items="${s:getPropertyFromSQL('env.country.states')}"  var="state">
                    <c:if test="${actionBean.userStateFromSession == state[1]}">
                        <stripes:option value="${state[1]}" selected="true">
                            ${state[1]}
                        </stripes:option>
                    </c:if>
                    <c:if test="${actionBean.userStateFromSession != state[1]}">
                        <stripes:option value="${state[1]}">
                            ${state[1]}
                        </stripes:option>
                    </c:if>
                </c:forEach>
            </stripes:select>

            <c:forEach items="${actionBean.customer.customerPhotographs}" varStatus="loop"> 
                <stripes:hidden name="file${loop.index}"/>
                <stripes:hidden id="photoGuid${loop.index}" name="customer.customerPhotographs[${loop.index}].photoGuid" value="${actionBean.customer.customerPhotographs[loop.index].photoGuid}"/>
                <stripes:hidden id="photoType${loop.index}" name="customer.customerPhotographs[${loop.index}].photoType" value="${actionBean.customer.customerPhotographs[loop.index].photoType}"/>
            </c:forEach>

            <div id="entity">
                <table class="entity_header">    
                    <tr>
                        <td>
                            <fmt:message key="customer"/>: <fmt:message   key="classification.${actionBean.customer.classification}" /> - ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                        </td> 
                    </tr>
                </table> 

                <table class="clear" width="100%">
                    <tr>
                        <c:choose>
                            <c:when test="${s:getPropertyWithDefault('env.address.mandatory','true')}">
                                <td>The following addresses are <b>mandatory</b> according to the country's communications regulation:<br />
                                    <ul>
                                        <c:forEach items="${s:getPropertyAsList('env.country.required.address')}" var="property" varStatus="loop">
                                            <li>${property}</li>
                                            </c:forEach>
                                    </ul>
                                </td>
                            </c:when>
                            <c:otherwise>
                                <td>Capturing of customer addresses is <b>optional</b>. Click on 'Add Address' to add an address.</td>
                            </c:otherwise>
                        </c:choose>
                    </tr>
                </table>
                <table  class="clear" id="tblAddresses">  
                    <tbody>
                        <c:forEach items="${actionBean.customer.addresses}" varStatus="loop">                    
                            <tr>
                                <td style='vertical-align: top; margin-top:auto'> 
                                    <c:set var="defaultRule" value="validate(this,\\\'^.{1,50}$\\\',\\\'emptynotok\\\')"/>
                                    <stripes:select id="addressType${loop.index}" name="customer.addresses[${loop.index}].type" onchange="${s:getValidationRule('address.type',defaultRule)}">
                                        <stripes:option value=""></stripes:option>
                                        <c:forEach items="${s:getPropertyAsList('env.customer.address.types')}" var="addressType" varStatus="loop2">
                                            <c:choose>
                                                <c:when test='${actionBean.customer.addresses[loop2.index].type == addressType}'>
                                                    <stripes:option value="${addressType}" selected='true'>${addressType}</stripes:option>
                                                </c:when>
                                                <c:otherwise>
                                                    <stripes:option value="${addressType}">${addressType}</stripes:option>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:forEach>
                                    </stripes:select>
                                </td>
                                <td style='vertical-align: top; margin-top:auto'>
                                    <table class="clear" style='vertical-align: top; margin-top:auto'>
                                        <tr>
                                            <td><fmt:message key="address.line1"/>:</td>                                  
                                            <td><input type='text' id="addressLine1${loop.index}" name="customer.addresses[${loop.index}].line1" maxlength='100' size='40' value="${actionBean.customer.addresses[loop.index].line1}" onkeyup="${s:getValidationRule('address.line1','validate(this,\'^.{2,50}$\',\'emptynotok\')')}"/></td>
                                        </tr>                        
                                        <tr>
                                            <td><fmt:message key="address.line2"/>:</td>
                                            <td><input type='text' id="addressLine2${loop.index}" name="customer.addresses[${loop.index}].line2" maxlength='100' size='40' value="${actionBean.customer.addresses[loop.index].line2}" onkeyup="${s:getValidationRule('address.line2', 'validate(this,\'^.{2,100}$\',\'emptynotok\')')}"/></td>
                                        </tr>
                                        <tr>
                                            <td><fmt:message key="state"/>:</td>
                                            <c:set var="defaultRule" value="validate(this,\\\'^.{1,50}$\\\',\\\'emptynotok\\\')"/>

                                            <td>
                                                <c:choose>
                                                    <c:when test="${s:getPropertyWithDefault('env.customer.verify.with.nida','true') == 'true'}"> 
                                                        <c:choose>
                                                             <c:when test="${actionBean.customer.addresses[loop.index].type == 'Physical Address'}">
                                                                <stripes:select id="addressState${loop.index}" name="customer.addresses[${loop.index}].state" onchange="validate(this,'^.{1,50}$','emptynotok')">
                                                                   <stripes:option value="${actionBean.customer.addresses[loop.index].state}">
                                                                    ${actionBean.customer.addresses[loop.index].state}
                                                                   </stripes:option>
                                                                </stripes:select>
                                                             </c:when>
                                                             <c:otherwise>
                                                                 <stripes:select id="addressState${loop.index}" name="customer.addresses[${loop.index}].state" onchange="populateZones(${loop.index}); return ${s:getValidationRule('address.state',defaultRule)};">
                                                                    <c:forEach items="${s:getPropertyFromSQL('env.country.states')}"  var="state">
                                                                    <stripes:option value="${state[1]}">
                                                                    ${state[1]}
                                                                    </stripes:option>
                                                                    </c:forEach>
                                                                 </stripes:select>                                             
                                                             </c:otherwise>
                                                        </c:choose>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <stripes:select id="addressState${loop.index}" name="customer.addresses[${loop.index}].state" onchange="populateZones(${loop.index}); return ${s:getValidationRule('address.state',defaultRule)};">
                                                            <c:forEach items="${s:getPropertyFromSQL('env.country.states')}"  var="state">
                                                                <stripes:option value="${state[1]}">
                                                                    ${state[1]}
                                                                </stripes:option>
                                                            </c:forEach>
                                                        </stripes:select>
                                                    </c:otherwise>
                                                </c:choose>        

                                            </td>
                                        </tr>
                                        <tr>
                                            <td><fmt:message key="zone"/>:</td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${s:getProperty('env.address.zone.mandatory')}">
                                                        <stripes:select id="addressZone${loop.index}" name="customer.addresses[${loop.index}].zone" onchange="validate(this,'^.{1,50}$','emptynotok')">
                                                            <c:choose>
                                                                <c:when test="${s:getPropertyWithDefault('env.customer.verify.with.nida','true') == 'true'}"> 
                                                                    <stripes:option value="${actionBean.customer.addresses[loop.index].zone}">
                                                                        ${actionBean.customer.addresses[loop.index].zone}
                                                                    </stripes:option>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <c:forEach items="${s:getPropertyFromSQL('env.country.zones')}"  var="zone">
                                                                        <c:choose>
                                                                            <c:when test='${actionBean.customer.addresses[loop.index].zone == zone[2]}'> 
                                                                                <stripes:option value="${zone[1]}">
                                                                                    ${zone[1]}
                                                                                </stripes:option>

                                                                            </c:when>
                                                                        </c:choose>
                                                                    </c:forEach>
                                                                </c:otherwise>
                                                            </c:choose> 
                                                        </stripes:select>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <stripes:select id="addressZone${loop.index}" name="customer.addresses[${loop.index}].zone" onchange="validate(this,'^.{0,50}$','emptyok')">
                                                            <c:choose>
                                                                <c:when test="${s:getPropertyWithDefault('env.customer.verify.with.nida','true') == 'true'}"> 
                                                                    <stripes:option value="${actionBean.customer.addresses[loop.index].zone}">
                                                                        ${actionBean.customer.addresses[loop.index].zone}
                                                                    </stripes:option>
                                                                </c:when>
                                                                <c:otherwise>  
                                                                    <c:forEach items="${s:getPropertyFromSQL('env.country.zones')}"  var="zone">
                                                                        <c:choose>
                                                                            <c:when test='${actionBean.customer.addresses[loop.index].zone == zone[2]}'> 
                                                                                <stripes:option value="${zone[1]}">
                                                                                    ${zone[1]}
                                                                                </stripes:option>
                                                                            </c:when>
                                                                        </c:choose>
                                                                    </c:forEach>
                                                                </c:otherwise>
                                                            </c:choose> 
                                                        </stripes:select>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td><fmt:message key="town"/>:</td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${s:getProperty('env.address.town.mandatory')}">
                                                        <c:set var="defaultRule" value="validate(this,\\\'^.{1,50}$\\\',\\\'emptynotok\\\')"/>
                                                        <stripes:text id="addressTown${loop.index}" name="customer.addresses[${loop.index}].town" maxlength="50" size="20" onkeyup="${s:getValidationRule('address.town',defaultRule)}"/>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <stripes:text id="addressTown${loop.index}" name="customer.addresses[${loop.index}].town" maxlength="50" size="20" onkeyup="validate(this,'^.{0,50}$','emptyok')"/>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td><fmt:message key="country"/>:</td>
                                            <td><input type='text' id="addressCountry${loop.index}" name="customer.addresses[${loop.index}].country" maxlength='50' size='20' value="${actionBean.customer.addresses[loop.index].country}" onkeyup="validate(this, '^.{2,50}$', 'emptynotok');" disabled='disabled'/></td>
                                                <stripes:hidden name="customer.addresses[${loop.index}].country" value="${actionBean.customer.addresses[loop.index].country}" />
                                        </tr>
                                        <tr>
                                            <c:choose>
                                                <c:when test="${s:getProperty('env.postalcode.display')}">
                                                    <td><fmt:message key="code"/>:</td>
                                                    <td><input type='text' id="addressCode${loop.index}" name="customer.addresses[${loop.index}].code" maxlength='10' size='10' value="${actionBean.customer.addresses[loop.index].code}" onkeyup="${s:getValidationRule('address.postalcode','validate(this,\'^[0-9]{4,10}$\',\'emptynotok\')')}"/></td>
                                                    </c:when>
                                                    <c:otherwise>
                                                <input type='hidden' id="addressCode${loop.index}" name="customer.addresses[${loop.index}].code" value=""/>                                              
                                            </c:otherwise>
                                        </c:choose>
                            </tr>      
                    </table>
                </td>
                <td style='vertical-align: top; margin-top:auto'>
                    <c:choose>
                        <c:when test="${s:getPropertyWithDefault('env.customer.verify.with.nida','true') == 'true'}">
                            <c:choose>
                                <c:when test="${actionBean.customer.addresses[loop.index].type == 'Physical Address'}">
                                    <input type='button' value='Remove' onclick="removeAddress(this, 'customer');" disabled/>
                                </c:when>
                                <c:otherwise>
                                    <input type='button' value='Remove' onclick="removeAddress(this, 'customer');" />                                             
                                </c:otherwise>
                            </c:choose>
                        </c:when>
                        <c:otherwise>
                            <input type='button' value='Remove' onclick="removeAddress(this, 'customer');" />                                             
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
    </tbody>
</table>
<br />
<span   class="button">
    <stripes:button name="btnAddAddress" onclick="addAddress('customer')"/>
</span>
<br />
<table class="clear" width="100%">
    <tr>
        <td id="lblErrorMessages"></td>
    </tr>
</table>
<br />
<span class="button">
    <stripes:submit name="captureCustomerScannedDocumentsBack" onclick="onBack=true;"/>
</span>     
<span class="button">
    <stripes:submit name="showSetCustomerUsernamePasswordNext" onclick="onBack=false;onSubmit=true"/>
</span>
</div>
</stripes:form>    
</stripes:layout-component>
</stripes:layout-render>