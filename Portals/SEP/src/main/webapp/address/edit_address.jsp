<%-- 
    Document   : edit_address
    Created on : 22 Jan 2013, 4:39:11 PM
    Author     : lesiba
--%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="edit.address"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">
            var onSubmit = false;
            var z = new Array();            
            
            <c:forEach items="${s:getPropertyFromSQL('env.country.zones')}" var="zone" varStatus="loop">z[${loop.index}] = new Array();z[${loop.index}][0] = "${zone[0]}";z[${loop.index}][1] = "${zone[1]}";z[${loop.index}][2] = "${zone[2]}";</c:forEach>

                function populateZones(statesDropdownBox) {
                    if(!onSubmit) {
                        var statesDropdownBox = document.getElementById("addressStateSel");
                        var zonesDropdownBox  = document.getElementById("addressZoneSel");
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
            
                window.onload = function() {
                    // populateZones();
                    
                    toggleStatesAndZones();
                };
        
            
        </script>
        <c:set var="curCountry" value="${s:getProperty('env.country.name')}"/>
        <c:set var="allowInternationalAddressForOrdinaryCustomers" value="${s:getPropertyWithDefault('env.allow.international.address.for.individual.customers', 'false')}"/>
                                
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
                                    </stripes:select>
                                    <stripes:hidden name="customerQuery.customerId" value="${actionBean.address.customerId}"/>
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
                                    </stripes:select>
                                    <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.address.organisationId}"/>
                                    <stripes:submit name="performEntityAction"/>
                                </stripes:form>
                            </td>
                        </c:otherwise>
                    </c:choose>
                </tr>
            </table>
            <stripes:form action="/Customer.action" autocomplete="off" method="POST" onsubmit="onSubmit=true; return alertValidationErrors();">
                <c:choose>
                    <c:when test="${actionBean.customer != null}">
                        <stripes:hidden name="address.customerId" value="${actionBean.address.customerId}"/>
                        <stripes:hidden name="customerQuery.customerId" value="${actionBean.address.customerId}"/>
                    </c:when>
                    <c:when test="${actionBean.organisation != null}">
                        <stripes:hidden name="address.organisationId" value="${actionBean.address.organisationId}"/>
                        <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.address.organisationId}"/>
                    </c:when>
                </c:choose>

                <stripes:hidden name="address.type" value="${actionBean.address.type}"/>
                <stripes:hidden name="address.addressId" value="${actionBean.address.addressId}"/>
                <table class="clear" width="100%">
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
                            <stripes:text id="addressStateTxt"  name="address.state.notused" value="${actionBean.address.state}" style="display:none;" maxlength="50" size="20" onkeyup="validate(this,'^.{1,50}$','emptynotok')"/>
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
                            <stripes:text id="addressZoneTxt"  name="address.zone.notused" value="${actionBean.address.zone}" style="display:none;" maxlength="50" size="20" onkeyup="validate(this,'^.{1,50}$','emptynotok')"/>
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="town"/>:</td>
                        <c:choose>
                            <c:when test="${s:getProperty('env.address.town.mandatory')}">

                                <td>
                                    <stripes:text name="address.town" maxlength="50" size="20"/>
                                </td>
                            </c:when>
                            <c:otherwise>
                                <td>
                                    <stripes:text name="address.town" onkeyup=""/>
                                </td>
                            </c:otherwise>
                        </c:choose>
                    </tr>
                    <tr>
                        <td><fmt:message key="country"/>:</td>
                        <td>
                            <c:choose>
                                <c:when test="${(actionBean.customer != null) && (allowInternationalAddressForOrdinaryCustomers == 'false')}">
                                    <stripes:text   name="address.country.fixed" value="${actionBean.address.country}"  size="20" disabled="disabled" />
                                    <stripes:hidden name="address.country" />
                                </c:when>
                                <c:otherwise>
                                    <stripes:select id="addressCountrySel" name="address.country"  onchange="toggleStatesAndZones();validate(this,'^.{1,50}$','emptynotok')">
                                        <c:forEach items="${s:getPropertyFromSQL('env.countries')}"  var="vCountry">
                                            <stripes:option value="${vCountry[0]}">${vCountry[0]}</stripes:option>
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
                            <stripes:hidden name="address.code" value="0000"/>
                        </c:otherwise>
                    </c:choose>
                    <tr>
                        <td colspan="2">
                            <span class="button">                                
                                <stripes:submit name="updateAddress"/>                                
                            </span>                        
                        </td>
                    </tr>
                </table>
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>