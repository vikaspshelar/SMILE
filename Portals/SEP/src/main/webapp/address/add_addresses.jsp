<%-- 
    Document   : add_addresses
    Created on : 22 Jan 2013, 3:16:35 PM
    Author     : lesiba
--%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.address"/>    
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">
            var onSubmit = false;
            var z = new Array();

            var addressLine1ValidationRule = "${s:getValidationRule('address.line1','validate(this,\'^.{2,50}$\',\'emptynotok\')')}";
            var addressLine2ValidationRule = "${s:getValidationRule('address.line1','validate(this,\'^.{2,50}$\',\'emptynotok\')')}";
            var addressStateValidationRule = "${s:getValidationRule('address.state','validate(this,\'^.{1,50}$\',\'emptynotok\')')}";
            var addressTownValidationRule = "${s:getValidationRule('address.town','validate(this,\'^.{1,50}$\',\'emptynotok\')')}";
            var addressZoneValidationRule = "${s:getValidationRule('address.zone','validate(this,\'^.{1,50}$\',\'emptynotok\')')}";
            var addressPostalCodeValidationRule = "${s:getValidationRule('address.postalcode','validate(this,\'^[0-9]{4,10}$\',\'emptynotok\')')}";
            var addressTypeValidationRule =${s:getValidationRule('address.type','validate(this,\'^.{1,50}$\',\'emptynotok\')')};
            var isAddressMandatory = ${s:getPropertyWithDefault("env.address.mandatory", "false")};

            <c:forEach items="${s:getPropertyFromSQL('env.country.zones')}" var="zone" varStatus="loop">z[${loop.index}] = new Array();z[${loop.index}][0] = "${zone[0]}";z[${loop.index}][1] = "${zone[1]}";z[${loop.index}][2] = "${zone[2]}";</c:forEach>

                function populateZones(statesDropdownBox) {
                    if (!onSubmit) {
                        var statesDropdownBox = document.getElementById("addressStateSel");
                        var zonesDropdownBox = document.getElementById("addressZoneSel");
                        //  Get the selected state ...
                        selectedState = statesDropdownBox.options[statesDropdownBox.selectedIndex].value;
                        // Polulate the zones dropdown with the zones related to the selected state.
                        zonesDropdownBox.options.length = 0;
                        zonesDropdownBox.options[0] = new Option("", "");
                        for (var i = 0; i < z.length; i++) {
                            if (selectedState == z[i][2]) {
                                zonesDropdownBox.options[zonesDropdownBox.options.length] = new Option(z[i][1], z[i][1]);
                            }
                        }
                        zonesDropdownBox.style.display = 'block';
                    }
                }

                window.onload = function() {
                    populateZones();
                };

                
        function toggleStatesAndZones() {
                var addressZoneTxt = document.getElementById("addressZoneTxt");
                var addressZoneSel = document.getElementById("addressZoneSel");
                var addressStateTxt = document.getElementById("addressStateTxt");
                var addressStateSel = document.getElementById("addressStateSel");
                var addressCountrySel = document.getElementById("addressCountrySel");
                var addressRowState = document.getElementById("addressRowState");
                var addressRowZone = document.getElementById("addressRowZone");
                
                // var identityTypeSel = document.getElementById("selIdentityType");
                var configuredForCountry="${s:getProperty('env.country.name')}";
                
                if (addressCountrySel.value.toUpperCase() == configuredForCountry.toUpperCase()) {
                    addressRowState.style.display = "table-row";
                    addressRowZone.style.display = "table-row";
                    
                    // passportExpiryDate.setAttribute("onkeyup", "${s:getValidationRule('passport.expirydate',"validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');")}");
                    addressStateSel.style.display = "inline";
                    addressStateSel.setAttribute("onchange", "populateZones(); return ${s:getValidationRule('address.state','validate(this,\'^.{1,50}$\',\'emptynotok\')')}");
                    addressStateSel.name = "address.state";
                    addressZoneSel.style.display = "inline";
                    addressZoneSel.setAttribute("onchange", "return ${s:getValidationRule('address.zone','validate(this,\'^.{1,50}$\',\'emptynotok\')')}");
                    addressZoneSel.name = "address.zone";
                    
                    populateZones();
                    
                    addressStateTxt.style.display = "none";
                    addressStateTxt.setAttribute("onchange", null);
                    addressStateTxt.name = "address.state.notused";
                    
                    addressZoneTxt.style.display = "none";
                    addressZoneTxt.setAttribute("onchange", null);
                    addressZoneTxt.name = "address.zone.notused";
                    
                } else {
                    addressRowState.style.display = "none";
                    addressRowZone.style.display = "none";
                    
                    addressStateSel.style.display = "none";
                    addressStateSel.setAttribute("onchange", null);
                                
                    addressStateSel.name = "address.state.notused";
                    
                    addressZoneSel.style.display = "none";
                    addressZoneSel.setAttribute("onchange", null);
                    addressZoneSel.name = "address.state.notused";
                    
                    addressStateTxt.style.display = "inline";
                    // addressStateTxt.setAttribute("onkeyup", "${s:getValidationRule('address.state','validate(this,\'^.{1,50}$\',\'emptynotok\')')}");
                    addressStateTxt.setAttribute("onkeyup", null);
                    addressStateTxt.name = "address.state";
                    addressStateTxt.value = "";
                    
                    addressZoneTxt.style.display = "inline";
                    // addressZoneTxt.setAttribute("onkeyup", "${s:getValidationRule('address.zone','validate(this,\'^.{1,50}$\',\'emptynotok\')')}");
                    addressZoneTxt.setAttribute("onkeyup", null);
                    addressZoneTxt.name = "address.zone";
                    addressZoneTxt.value = "";
                }
            }
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
                                        <stripes:option value="manageAddresses"><fmt:message key="manage.addresses"/></stripes:option>
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
                                        <stripes:option value="manageAddresses"><fmt:message key="manage.addresses"/></stripes:option>
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
                <c:choose>
                    <c:when test="${actionBean.customer != null}">
                        <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                        <stripes:hidden name="address.customerId" value="${actionBean.customer.customerId}"/>
                    </c:when>
                    <c:otherwise>
                        <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                        <stripes:hidden name="address.organisationId" value="${actionBean.organisation.organisationId}"/>                
                    </c:otherwise>
                </c:choose>                

                <table class="clear" width="100%">
                    <tr>
                        <td><fmt:message key="address.type"/>:</td>
                        <td>
                            <stripes:select name="address.type" onkeyup="validate(this,'^.{2,50}$','emptynotok')">
                                <c:forEach items="${s:getPropertyAsList('env.customer.address.types')}" var="addresstype" varStatus="loop">
                                    <stripes:option value="${addresstype}">${addresstype}</stripes:option>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="address.line1"/>:</td>
                        <td><stripes:text name="address.line1" maxlength="50" size="20" onkeyup="validate(this,'^.{2,50}$','emptynotok')"/></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="address.line2"/>:</td>
                         <c:set var="defaultRule" value="validate(this,\\\'^.{2,100}$\\\',\\\'emptynotok\\\')"/>
                        <td><stripes:text name="address.line2"  maxlength="50" size="20" onkeyup="${s:getValidationRule('address.line2', defaultRule)}"/></td>
                    </tr>
                    <tr id="addressRowState">
                        <td><fmt:message key="state"/>:</td>
                        <td><stripes:select id="addressStateSel" name="address.state" onchange="populateZones(); return validate(this,'^.{1,50}$','emptynotok');">
                                <c:forEach items="${s:getPropertyFromSQL('env.country.states')}"  var="state">
                                    <stripes:option value="${state[1]}">
                                        ${state[1]}
                                    </stripes:option>
                                </c:forEach>
                            </stripes:select>
                            <stripes:text id="addressStateTxt"  name="address.state.notused" style="display:none;" maxlength="50" size="20"/>
                        </td>
                    </tr>
                    <tr id="addressRowZone">
                        <td><fmt:message key="zone"/>:</td>
                        <td>                            
                            <c:choose>
                                <c:when test="${s:getProperty('env.address.zone.mandatory')}">
                                    <stripes:select id="addressZoneSel" name="address.zone"  onchange="validate(this,'^.{1,50}$','emptynotok')">
                                        <c:forEach items="${s:getPropertyFromSQL('env.country.zones')}"  var="zone"><c:if test='${actionBean.address.town == zone[2]}'><stripes:option value="${zone[1]}">${zone[1]}</stripes:option></c:if></c:forEach>
                                    </stripes:select>
                                </c:when>
                                <c:otherwise>
                                    <stripes:select id="addressZoneSel" name="address.zone"  onchange="validate(this,'^.{0,50}$','emptyok')">
                                        <c:forEach items="${s:getPropertyFromSQL('env.country.zones')}"  var="zone"><c:if test='${actionBean.address.town == zone[2]}'><stripes:option value="${zone[1]}">${zone[1]}</stripes:option></c:if></c:forEach>
                                    </stripes:select>
                                </c:otherwise>
                            </c:choose>
                            <stripes:text id="addressZoneTxt"  name="address.zone.notused" style="display:none;" maxlength="50" size="20"/>
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="town"/>:</td>
                        <td>
                            <c:choose>
                                <c:when test="${s:getProperty('env.address.town.mandatory')}">
                                    <stripes:text name="address.town" maxlength="50" size="20" onkeyup="validate(this,'^.{2,50}$','emptynotok')"/>
                                </c:when>
                                <c:otherwise>
                                    <stripes:text name="address.town" onkeyup="validate(this,'^.{2,50}$','emptyok')"/>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    
                    <c:set var="curCountry" value="${s:getProperty('env.country.name')}"/>
                    <c:set var="allowInternationalAddressForOrdinaryCustomers" value="${s:getPropertyWithDefault('env.allow.international.address.for.individual.customers', 'false')}"/>
                    
                    <tr>
                        <td><fmt:message key="country"/>:</td>
                        <td>
                            <c:choose>
                                <c:when test="${(actionBean.customer != null) && (allowInternationalAddressForOrdinaryCustomers == 'false')}">
                                    <stripes:text name="address.country" maxlength="50" size="20" value="${s:getProperty('env.country.name')}" readonly="true"/>
                                </c:when>
                                <c:otherwise>
                                    <stripes:select id="addressCountrySel" name="address.country"  onchange="toggleStatesAndZones();validate(this,'^.{1,50}$','emptynotok')">
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
                                </c:otherwise>
                            </c:choose>
                        </td>    
                   </tr>

                    <c:choose>
                        <c:when test="${(actionBean.organisation != null) || s:getProperty('env.postalcode.display')}">
                            <tr>
                                <td><fmt:message key="code"/>:</td>
                                <td><stripes:text name="address.code" maxlength="20" size="20"/></td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <stripes:hidden name="address.code" value=""/>
                        </c:otherwise>
                    </c:choose>

                    <tr>
                        <td colspan="2">
                            <span class="button">                                
                                <stripes:submit name="addAddress"/>
                            </span>
                        </td>
                    </tr>
                </table>
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
