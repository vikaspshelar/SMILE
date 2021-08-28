<%-- 
    Document   : add_customer_address
    Created on : Nov 22, 2011, 11:09:48 AM
    Author     : lesiba
--%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.new.organisation.addresses"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">
            var requiredAddresses = new Array();
            var onSubmit = false;
            
            <c:forEach items="${s:getPropertyAsList('env.country.required.address')}" var="property" varStatus="loop">
                requiredAddresses[${loop.index}]    = "${property}";
            </c:forEach>
                
                var strAddressLine1 = '<fmt:message key="address.line1"/>';
                var strAddressLine2 = '<fmt:message key="address.line2"/>';
                var strTown = '<fmt:message key="town"/>';
                var strCountry = '<fmt:message key="country"/>';
                var strCode    = '<fmt:message key="code"/>';
                var strZone    = '<fmt:message key="zone"/>';
                var strState    = '<fmt:message key="state"/>';
                var displayPostalCode = '${s:getProperty("env.postalcode.display")}';
                var countryName = '${s:getProperty('env.country.name')}';
                var isTownMandatory = ${s:getProperty("env.address.town.mandatory")};
                
                var addressLine1ValidationRule="${s:getValidationRule('address.line1','validate(this,\'^.{2,50}$\',\'emptynotok\')')}";
                var addressLine2ValidationRule="${s:getValidationRule('address.line1','validate(this,\'^.{2,50}$\',\'emptynotok\')')}";
                var addressStateValidationRule="${s:getValidationRule('address.state','validate(this,\'^.{1,50}$\',\'emptynotok\')')}";
                var addressTownValidationRule="${s:getValidationRule('address.town','validate(this,\'^.{1,50}$\',\'emptynotok\')')}";
                var addressZoneValidationRule="${s:getValidationRule('address.zone','validate(this,\'^.{1,50}$\',\'emptynotok\')')}";
                var addressCountryValidationRule="${s:getValidationRule('address.country','validate(this,\'^.{1,100}$\',\'emptynotok\')')}";
                
                var addressPostalCodeValidationRule="${s:getValidationRule('address.postalcode','validate(this,\'^[0-9]{4,10}$\',\'emptynotok\')')}";
                var addressTypeValidationRule=${s:getValidationRule('address.type','validate(this,\'^.{1,50}$\',\'emptynotok\')')};
                var isAddressMandatory = ${s:getPropertyWithDefault("env.address.mandatory", "false")};
            
                
                var z = new Array();
            
<c:forEach items="${s:getPropertyFromSQL('env.country.zones')}" var="zone" varStatus="loop">z[${loop.index}] = new Array();z[${loop.index}][0] = "${zone[0]}";z[${loop.index}][1] = "${zone[1]}";z[${loop.index}][2] = "${zone[2]}";</c:forEach>
            
                function populateZones(rowIndex) {
                    if(!onSubmit) {
                        var statesDropdownBox = document.getElementById("addressStateSel" + rowIndex);
                        var zonesDropdownBox  = document.getElementById("addressZoneSel" + rowIndex);
                        //  Get the selected state ...
                        selectedState = statesDropdownBox.options[statesDropdownBox.selectedIndex].value;
                        // Polulate the zones dropdown with the zones related to the selected state.
                        zonesDropdownBox.options.length = 0;
                        zonesDropdownBox.options[0] = new Option("","");
                        for(var i = 0; i < z.length; i++) {
                            if(selectedState == z[i][2]) {
                                zonesDropdownBox.options[zonesDropdownBox.options.length] = new Option(z[i][1], z[i][1]);
                            }
                        }
                        zonesDropdownBox.style.display = 'block';
                    }
                } 
            
            
            function toggleStatesAndZones(rowIndex) {
                var addressZoneTxt = document.getElementById("addressZoneTxt" + rowIndex);
                var addressZoneSel = document.getElementById("addressZoneSel" + rowIndex);
                var addressStateTxt = document.getElementById("addressStateTxt" + rowIndex);
                var addressStateSel = document.getElementById("addressStateSel" + rowIndex);
                var addressCountrySel = document.getElementById("addressCountrySel" + rowIndex);
                 var addressRowState = document.getElementById("addressRowState" + rowIndex);
                var addressRowZone = document.getElementById("addressRowZone" + rowIndex);
                
                // var identityTypeSel = document.getElementById("selIdentityType");
                var configuredForCountry="${s:getProperty('env.country.name')}";
                
                if (addressCountrySel.value.toUpperCase() == configuredForCountry.toUpperCase()) {
                    addressRowState.style.display = "table-row";
                    addressRowZone.style.display = "table-row";
                    
                    // passportExpiryDate.setAttribute("onkeyup", "${s:getValidationRule('passport.expirydate',"validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');")}");
                    addressStateSel.style.display = "inline";
                    addressStateSel.setAttribute("onchange", "populateZones(); return ${s:getValidationRule('address.state','validate(this,\'^.{1,50}$\',\'emptynotok\')')}");
                    addressStateSel.name = "organisation.addresses[" + rowIndex + "].state";
                    addressZoneSel.style.display = "inline";
                    addressZoneSel.setAttribute("onchange", "return ${s:getValidationRule('address.zone','validate(this,\'^.{1,50}$\',\'emptynotok\')')}");
                    addressZoneSel.name = "organisation.addresses[" + rowIndex + "].zone";
                    
                    populateZones(rowIndex);
                    
                    addressStateTxt.style.display = "none";
                    addressStateTxt.setAttribute("onchange", null);
                    addressStateTxt.name = "organisation.addresses[" + rowIndex + "].state.notused";
                    
                    addressZoneTxt.style.display = "none";
                    addressZoneTxt.setAttribute("onchange", null);
                    addressZoneTxt.name = "organisation.addresses[" + rowIndex + "].zone.notused";
                    
                } else {
                    addressRowState.style.display = "none";
                    addressRowZone.style.display = "none";
                    
                    addressStateSel.style.display = "none";
                    addressStateSel.setAttribute("onchange", null);
                                
                    addressStateSel.name = "organisation.addresses[" + rowIndex + "].state.notused";
                    
                    addressZoneSel.style.display = "none";
                    addressZoneSel.setAttribute("onchange", null);
                    addressZoneSel.name = "organisation.addresses[" + rowIndex + "].zone.notused";
                    
                    addressStateTxt.style.display = "inline";
                    // addressStateTxt.setAttribute("onkeyup", "${s:getValidationRule('address.state','validate(this,\'^.{1,50}$\',\'emptynotok\')')}");
                    addressStateTxt.setAttribute("onkeyup", null);
                    addressStateTxt.name = "organisation.addresses[" + rowIndex + "].state";
                    addressStateTxt.value = "";
                    
                    addressZoneTxt.style.display = "inline";
                    // addressZoneTxt.setAttribute("onkeyup", "${s:getValidationRule('address.zone','validate(this,\'^.{1,50}$\',\'emptynotok\')')}");
                    addressZoneTxt.setAttribute("onkeyup", null);
                    addressZoneTxt.name = "organisation.addresses[" + rowIndex + "].zone";
                    addressZoneTxt.value = "";
                }
            }
            
            window.onload = function() {
                    var table = document.getElementById("tblAddresses");
                    for (var i = 0, row; row = table.rows[i]; i++) {
                        toggleStatesAndZones(i);
                    }
            };
        </script>

        <stripes:form action="/Customer.action" autocomplete="off" onsubmit="onSubmit=true; return ((onBack == true) ? true : (alertValidationErrors() && checkRequiredAddresses()));">

            <stripes:hidden name="organisation.organisationType" value="${actionBean.organisation.organisationType}"/>
            <stripes:hidden name="organisation.organisationSubType" value="${actionBean.organisation.organisationSubType}"/>
            <stripes:hidden name="organisation.organisationName"    value="${actionBean.organisation.version}"/>
            <stripes:hidden name="organisation.companyNumber" value="${actionBean.organisation.companyNumber}"/>
            <stripes:hidden name="organisation.taxNumber" value="${actionBean.organisation.taxNumber}"/>
            <stripes:hidden name="organisation.emailAddress" value="${actionBean.organisation.emailAddress}"/>
            <stripes:hidden name="organisation.alternativeContact1" value="${actionBean.organisation.alternativeContact1}"/>
            <stripes:hidden name="organisation.alternativeContact2" value="${actionBean.organisation.alternativeContact2}"/>
            <stripes:hidden name="organisation.industry" value="${actionBean.organisation.industry}"/>
            <stripes:hidden name="organisation.size" value="${actionBean.organisation.size}" />

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

            <stripes:select id="address.states" name="address.towns" style="display:none">
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
            
            <c:set var="curCountry" value="${s:getProperty('env.country.name')}"/>
                    
            
            <stripes:select id="address.countries" name="address.countries"  style="display:none">
                <c:forEach items="${s:getPropertyFromSQL('env.countries')}"  var="vCountry">
                    <c:choose>
                        <c:when test="${vCountry[0].equalsIgnoreCase(curCountry)}">
                            <stripes:option value="${vCountry[0]}" selected="true">${vCountry[0]}</stripes:option>
                        </c:when>
                        <c:otherwise>
                            <stripes:option value="${vCountry[0]}">${vCountry[0]}</stripes:option>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </stripes:select>

            <c:forEach items="${actionBean.organisation.organisationPhotographs}" varStatus="loop"> 
                <stripes:hidden name="file${loop.index}"/>
                <stripes:hidden id="photoGuid${loop.index}" name="organisation.organisationPhotographs[${loop.index}].photoGuid" value="${actionBean.organisation.organisationPhotographs[loop.index].photoGuid}"/>
                <stripes:hidden id="photoType${loop.index}" name="organisation.organisationPhotographs[${loop.index}].photoType" value="${actionBean.organisation.organisationPhotographs[loop.index].photoType}"/>
            </c:forEach>

            <div id="entity">

                <table class="entity_header">    
                    <tr>
                        <td>
                            <fmt:message key="Organisation"/>:${actionBean.organisation.organisationName}
                        </td> 
                    </tr>
                </table> 

                <table class="clear" width="100%">
                    <tr>
                        <td>The following addresses are <b>mandatory</b> according to the country's communications regulation:<br />
                            <ul>
                                <c:forEach items="${s:getPropertyAsList('env.country.required.address')}" var="property" varStatus="loop">
                                    <li>${property}</li>
                                </c:forEach>
                            </ul>
                        </td>
                    </tr>
                </table>
                <table  class="clear" id="tblAddresses">  
                    <tbody>
                        <c:forEach items="${actionBean.organisation.addresses}" varStatus="loop">                    
                            <tr>
                                <td style='vertical-align: top; margin-top:auto'>      
                                    <stripes:select id="addressType${loop.index}" name="organisation.addresses[${loop.index}].type" onchange="validate(this,'^.{1,50}$','emptynotok')">
                                        <stripes:option value=""></stripes:option>
                                        <c:forEach items="${s:getPropertyAsList('env.customer.address.types')}" var="addressType" varStatus="loop2">
                                            <c:choose>
                                                <c:when test='${actionBean.organisation.addresses[loop2.index].type == addressType}'>
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
                                            <td><input type='text' id="addressLine1${loop.index}" name="organisation.addresses[${loop.index}].line1" maxlength='100' size='40' value="${actionBean.organisation.addresses[loop.index].line1}" onkeyup="validate(this,'^.{2,100}$','emptynotok')"/></td>
                                        </tr>                        
                                        <tr>
                                            <td><fmt:message key="address.line2"/>:</td>
                                            <td><input type='text' id="addressLine2${loop.index}" name="organisation.addresses[${loop.index}].line2" maxlength='100' size='40' value="${actionBean.organisation.addresses[loop.index].line2}" onkeyup="validate(this,'^.{2,100}$','emptyok')"/></td>
                                        </tr>
                                        <tr id="addressRowState${loop.index}">
                                            <td><fmt:message key="state"/>:</td>
                                            <td><stripes:select id="addressStateSel${loop.index}" name="organisation.addresses[${loop.index}].state" onchange="populateZones(${loop.index}); return validate(this,'^.{1,50}$','emptynotok');">
                                                    <c:forEach items="${s:getPropertyFromSQL('env.country.states')}"  var="state">
                                                        <stripes:option value="${state[1]}">
                                                            ${state[1]}
                                                        </stripes:option>
                                                    </c:forEach>
                                                </stripes:select>
                                                <stripes:text id="addressStateTxt${loop.index}"  name="organisation.addresses[${loop.index}].state" value="${actionBean.organisation.addresses[loop.index].state}" style="display:none;" maxlength="50" size="20"/>
                                            </td>
                                        </tr>
                                        <tr id="addressRowZone${loop.index}">
                                            <td><fmt:message key="zone"/>:</td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${s:getProperty('env.address.zone.mandatory')}">
                                                        <stripes:select id="addressZoneSel${loop.index}" name="organisation.addresses[${loop.index}].zone" onchange="validate(this,'^.{1,50}$','emptynotok')">
                                                            <stripes:option value=""></stripes:option>
                                                            <c:forEach items="${s:getPropertyFromSQL('env.country.zones')}"  var="zone">
                                                                <c:choose>
                                                                    <c:when test='${actionBean.organisation.addresses[loop.index].state == zone[2]}'>                                                                        
                                                                        <stripes:option value="${zone[1]}">
                                                                            ${zone[1]}
                                                                        </stripes:option>
                                                                    </c:when>
                                                                </c:choose>
                                                            </c:forEach>
                                                        </stripes:select>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <stripes:select id="addressZoneSel${loop.index}" name="organisation.addresses[${loop.index}].zone" onchange="validate(this,'^.{0,50}$','emptyok')">
                                                            <c:forEach items="${s:getPropertyFromSQL('env.country.zones')}"  var="zone">
                                                                <c:choose>
                                                                    <c:when test='${actionBean.organisation.addresses[loop.index].state == zone[2]}'>
                                                                        <stripes:option value=""></stripes:option>
                                                                        <stripes:option value="${zone[1]}">
                                                                            ${zone[1]}
                                                                        </stripes:option>
                                                                    </c:when>
                                                                </c:choose>
                                                            </c:forEach>
                                                        </stripes:select>
                                                    </c:otherwise>
                                                </c:choose>
                                                <stripes:text id="addressZoneTxt${loop.index}"  name="address.zone.notused" style="display:none;" value="${actionBean.organisation.addresses[loop.index].zone}" maxlength="50" size="20"/>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td><fmt:message key="town"/>:</td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${s:getProperty('env.address.town.mandatory')}">
                                                        <stripes:text id="addressTown${loop.index}" name="organisation.addresses[${loop.index}].town" maxlength="50" size="20" onkeyup="validate(this,'^.{1,50}$','emptynotok')"/></td>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <stripes:text id="addressTown${loop.index}" name="organisation.addresses[${loop.index}].town" maxlength="50" size="20" onkeyup="validate(this,'^.{0,50}$','emptyok')"/>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                        
                                        <c:choose>
                                            <c:when test="${s:getProperty('env.postalcode.display')}">
                                                <tr>
                                                    <td><fmt:message key="code"/>:</td>
                                                    <td><input type='text' id="addressCode${loop.index}" name="organisation.addresses[${loop.index}].code" maxlength='10' size='10' value="${actionBean.organisation.addresses[loop.index].code}" onkeyup="validate(this,'^[0-9]{4,10}$','emptyok')"/></td>
                                                </tr>
                                            </c:when>
                                            <c:otherwise>
                                                <tr>
                                                    <td><input type='hidden' id="addressCode${loop.index}" name="organisation.addresses[${loop.index}].code" value=""/></td> 
                                                </tr>
                                            </c:otherwise>
                                            </c:choose>
                                         <c:set var="curCountry" value="${s:getProperty('env.country.name')}"/>
                                         <c:set var="allowInternationalAddressForOrdinaryCustomers" value="${s:getPropertyWithDefault('env.allow.international.address.for.individual.customers', 'false')}"/>
                    
                                        <tr>
                                            <td><fmt:message key="country"/>:</td>
                                            <td>
                                                <stripes:select id="addressCountrySel${loop.index}" name="organisation.addresses[${loop.index}].country"  onchange="toggleStatesAndZones(${loop.index});validate(this,'^.{1,50}$','emptynotok')">
                                                    <c:forEach items="${s:getPropertyFromSQL('env.countries')}"  var="vCountry">
                                                        <c:choose>
                                                            <c:when test="${vCountry[0].equalsIgnoreCase(curCountry)}">
                                                                <stripes:option value="${vCountry[0]}" selected="true">${vCountry[0]}</stripes:option>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <stripes:option value="${vCountry[0]}">${vCountry[0]}</stripes:option>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </c:forEach>
                                                </stripes:select>
                                                
                                        </tr> 
                                    </table>
                                </td>
                                <td style='vertical-align: top; margin-top:auto'>
                                    <input type='button' value='Remove' onclick="removeAddress(this, 'organisation');" />
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                <br />
                <span   class="button">
                    <stripes:button name="btnAddAddress" onclick="addAddress('organisation')"/>
                </span>
                <br />
                <table class="clear" width="100%">
                    <tr>
                        <td id="lblErrorMessages"></td>
                    </tr>
                </table>
                <br />
                <span class="button">
                    <stripes:submit name="captureOrganisationScannedDocumentsBack" onclick="onBack=true;"/>
                </span>     
                <span class="button">
                    <stripes:submit name="showAddOrganisationWizardSummary" onclick="onBack=false;"/>
                </span>
            </div>
        </stripes:form>    
    </stripes:layout-component>
</stripes:layout-render>