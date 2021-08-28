<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="edit.customer"/>
</c:set>
<c:set var="customerClassType" value="${s:getDelimitedPropertyValueMapping('env.customer.classifications', actionBean.customer.classification)}"/>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="html_head">        
        <script type="text/javascript">
            var nationality = "${actionBean.customer.nationality}";
            var idTypesValidationRules = [<c:forEach items="${s:getPropertyAsList('env.customer.personal.id.document.types')}" var="identityType" varStatus="loop">
                ["${identityType}", "${s:getValidationRule(identityType,'')}"],
            </c:forEach>
            <c:forEach items="${s:getPropertyAsList('env.foreign.customer.personal.id.document.types')}" var="fIdentityType" varStatus="loop">
                ["${fIdentityType}", "${s:getValidationRule(fIdentityType,'')}"],
            </c:forEach>];

            var countryCode = "${s:getProperty('env.locale.country.for.language.en')}";
            var onUpdate = false;
            var foreignUseMultipleIdTypes = "${s:getProperty('env.customer.foreigner.documents.use.multiple.types')}";

            var checkRequiredDocumentsRegEx = "";
            var missingDocumentTypesErrorMessageResource = "";
            var methodOfIdentification = "";

            var today = new Date().getFullYear();
            $j(document).ready(function () { //passportexpirydate 
                $j('#datePicker2').datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", changeYear: true, changeMonth: true, onSelect: function () {
                        validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');
                    }});
            });

            $j(document).ready(function () { /*visaexpirydate*/
                $j('#datePicker3').datepicker({

                    dateFormat: 'yy/mm/dd',
                    showOn: 'button',
                    buttonText: "..",
                    changeYear: true,
                    changeMonth: true,
                    onSelect: function () {
            ${s:getValidationRule('visa.expirydate',"validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');")}
                    },
                    showButtonPanel: true,
                    beforeShow: function (input) {
                        showForLifeButton(input);
                    },
                    onChangeMonthYear: function (yy, mm, inst) {
                        showForLifeButton(inst.input);
                    }
                });
            });

            function showForLifeButton(input) {
                setTimeout(function () {
                    var buttonPane = $j(input)
                            .datepicker("widget")
                            .find(".ui-datepicker-buttonpane");

                    var btn = $j('<button type="button" class="ui-datepicker-current ui-state-default ui-priority-secondary ui-corner-all">For-Life</button>');
                    btn
                            .unbind("click")
                            .bind("click", function () {
                                input.value = "For-Life";
                                $j(input).datepicker('hide');
                            });
                    btn.appendTo(buttonPane);
                }, 1);
            }

            window.addEventListener("DOMContentLoaded", function () {
                disableNiraSuppliedFields();
            });

            function disableNiraSuppliedFields() {
            <c:if test="${s:getPropertyWithDefault('env.customer.verify.with.nida','false') == 'true' && actionBean.customer.KYCStatus == 'V'}">
                var firstName = document.getElementById("customer.firstName");
                firstName.readOnly = true;
                var middleName = document.getElementById("customer.middleName");
                middleName.readOnly = true;
                var lastName = document.getElementById("customer.lastName");
                lastName.readOnly = true;
                var identityNumber = document.getElementById("customer.identityNumber");
                identityNumber.readOnly = true;

                var alternativeContact1 = document.getElementById("customer.alternativeContact1");
                alternativeContact1.readOnly = true;

                var nationalitySel = document.getElementById("customer.nationality");
                nationalitySel.disabled = true;
                var selIdentityType = document.getElementById("selIdentityType");
                selIdentityType.disabled = true;
                var gender = document.getElementById("customer.gender");
                gender.disabled = true;
                <c:if test="${s:getPropertyWithDefault('env.customer.national.id.mandatory','false') == 'true' && actionBean.customer.isNinVerified == 'Y'}">
                    var nationalIdentityNumber = document.getElementById("customer.nationalIdentityNumber");
                    nationalIdentityNumber.readOnly = true;
                </c:if>
            </c:if>
            }


            function enableNiraSuppliedFields() {
            <c:if test="${s:getPropertyWithDefault('env.customer.verify.with.nida','false') == 'true' && actionBean.customer.KYCStatus == 'V'}">
                var firstName = document.getElementById("customer.firstName");
                firstName.readOnly = false;
                var middleName = document.getElementById("customer.middleName");
                middleName.readOnly = false;
                var lastName = document.getElementById("customer.lastName");
                lastName.readOnly = false;
                var identityNumber = document.getElementById("customer.identityNumber");
                identityNumber.readOnly = false;

                var alternativeContact1 = document.getElementById("customer.alternativeContact1");
                alternativeContact1.readOnly = true;

                var nationalitySel = document.getElementById("customer.nationality");
                nationalitySel.disabled = false;
                var selIdentityType = document.getElementById("selIdentityType");
                selIdentityType.disabled = false;
                var gender = document.getElementById("customer.gender");
                gender.disabled = false;
                <c:if test="${s:getPropertyWithDefault('env.customer.national.id.mandatory','false') == 'true' && actionBean.customer.isNinVerified == 'N'}">
                    var nationalIdentityNumber = document.getElementById("customer.nationalIdentityNumber");
                    nationalIdentityNumber.readOnly = false;
                </c:if>
            </c:if>
            }

            <c:choose>
                <c:when test="${s:getPropertyWithDefault('env.customer.verify.with.nida','false') == 'true' && actionBean.customer.KYCStatus == 'V'}">

                </c:when>
                <c:otherwise>
                    <c:choose>
                        <c:when test="${customerClassType == 'minor'}">
            var startDate = new Date().getFullYear() - 18; //gets the current year minus 18.                   

            $j(document).ready(function () {
                $j("#datePicker1").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", yearRange: startDate + ":" + today, changeYear: true, changeMonth: true, onSelect: function () {
                        validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');
                    }});
            });
                        </c:when>
                        <c:otherwise>
            var date = new Date().getFullYear() - 18; //gets the current year minus 18.
            $j(document).ready(function () {
                $j("#datePicker1").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", yearRange: '1900:' + date, changeYear: true, changeMonth: true, onSelect: function () {
                        validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');
                    }});

            });
                        </c:otherwise>
                    </c:choose>
                </c:otherwise>
            </c:choose>

            <c:if test="${not empty actionBean.customer.visaExpiryDate}">
            var visaExpiryDate = "${actionBean.customer.visaExpiryDate}";
            </c:if>

            function checkSelection(obj) {

                if (onUpdate) {
                    return true;
                }

                var identityTypeSel = document.getElementById("selIdentityType");
                var passportExpiryDateRow = document.getElementById("passportExpiryDateRow");
                var passportExpiryDate = document.getElementById("datePicker2");
                var visaExpiryDateRow = document.getElementById("visaExpiryDateRow");
                var visaExpiryDate = document.getElementById("datePicker3");
                var idCardNumberRow = document.getElementById("idCardNumberRow");
                var idCardNumberBox = document.getElementById("customer.cardNumber");

                var options = identityTypeSel.options;

                if (obj.value !== countryCode) {// Customer is a foreign international
                    if (idCardNumberRow != null) {
                        idCardNumberRow.style.display = "none";
                        idCardNumberBox.setAttribute("onkeyup", "");
                    }

                    if (visaExpiryDateRow != null) {
                        visaExpiryDate.setAttribute("onkeyup", "${s:getValidationRule('visa.expirydate',"validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');")}");
                        visaExpiryDateRow.style.display = "table-row";
                    }
                    /* Set method of identification to Passport */
                    if (foreignUseMultipleIdTypes === "true") {
                        var foreignOptions = document.getElementById('customer.foreign.document.id.types').options;
                        var newOption = null;
                        removeOptions(identityTypeSel);

                        for (var i = 0; i < foreignOptions.length; i++) {
                            newOption = document.createElement('option');
                            newOption.value = foreignOptions[i].value;
                            newOption.text = foreignOptions[i].text;
                            identityTypeSel.appendChild(newOption);
                        }
                    } else {
                        for (var i = 0; i < options.length; i++) {
                            if (options[i].value === 'passport') {
                                // Set method of identification passport and disable selection of method of identification
                                identityTypeSel.value = options[i].value;
                                identityTypeSel.selectedIndex = i;
                                identityTypeSel.disabled = true;
                                // Show the passport expiry date
                                passportExpiryDate.setAttribute("onkeyup", "${s:getValidationRule('passport.expirydate',"validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');")}");
                                passportExpiryDateRow.style.display = "table-row";
                                break;
                            }
                        }
                    }
                } else { // Customeris is a local - use local identification types
                    var foreignOptions = document.getElementById('customer.document.id.types').options;
                    var newOption = null;
                    removeOptions(identityTypeSel);

                    for (var i = 0; i < foreignOptions.length; i++) {
                        newOption = document.createElement('option');
                        newOption.value = foreignOptions[i].value;
                        newOption.text = foreignOptions[i].text;
                        identityTypeSel.appendChild(newOption);
                    }

                    if (onUpdate != true) {
                        // Enable the selection of method of identification
                        // identityTypeSel.selectedIndex = 0;
                        identityTypeSel.disabled = false;
                        identityTypeSel.style.display = "block";
                        if (idCardNumberRow != null) {
                            idCardNumberRow.style.display = "table-row";
                            idCardNumberBox.setAttribute("onkeyup", "${s:getValidationRule('cardnumber',"validate(this,'^.{6,30}$','emptyok');")}");
                        }
                        passportExpiryDate.setAttribute("onkeyup", "");
                        // passportExpiryDate.value = "";
                        passportExpiryDateRow.style.display = "none";
                        if (visaExpiryDateRow != null) {
                            visaExpiryDate.setAttribute("onkeyup", "");
                            // visaExpiryDate.value = "";
                            visaExpiryDateRow.style.display = "none";
                        }
                    }
                }
                return true;
            }

            window.onload = function () {

                var nationalitySel = document.getElementById("customer.nationality");
                if (nationalitySel.selectedIndex > 0) {
                    checkSelection(nationalitySel);
                }

                initIdentityTypeSelectionBox();
                togglePassportExpiryDate();

                var identityTypeSel = document.getElementById("selIdentityType");
                validateSelection(identityTypeSel.value);

                disableNiraSuppliedFields();
            };

            function initIdentityTypeSelectionBox() {
                var idType = document.getElementById("customer.identityNumberType");
                var identityTypeSel = document.getElementById("selIdentityType");

                var options = identityTypeSel.options;

                for (var i = 0; i < options.length; i++) {
                    if (options[i].value == idType.value) {
                        identityTypeSel.selectedIndex = i;
                        identityTypeSel.value = options[i].value;
                    }
                }
            }

            function togglePassportExpiryDate() {
                var passportExpiryDateRow = document.getElementById("passportExpiryDateRow");
                var passportExpiryDate = document.getElementById("datePicker2");
                var visaExpiryDateRow = document.getElementById("visaExpiryDateRow");
                var visaExpiryDate = document.getElementById("datePicker3");
                var identityTypeSel = document.getElementById("selIdentityType");

                if (identityTypeSel.value == 'passport') {
                    passportExpiryDate.setAttribute("onkeyup", "validate(this,'^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$','emptynotok')");
                    passportExpiryDateRow.style.display = "table-row";

                } else {
                    passportExpiryDate.setAttribute("onkeyup", "");
                    passportExpiryDate.value = "";
                    passportExpiryDateRow.style.display = "none";

                }
            }

            function validateSelection(selectedValue) {

                var validationRule = "";

                // Look for the validation rule corresponding to this selected value
                for (var i = 0; i < idTypesValidationRules.length; i++) {
                    if (selectedValue == idTypesValidationRules[i][0]) {
                        validationRule = idTypesValidationRules[i][1];
                        break;
                    }
                }

                var textBoxToValidate = document.getElementById("customer.identityNumber");

                if (validationRule.trim().length > 0) {
                    textBoxToValidate.setAttribute("onkeyup", validationRule);
                } else {
                    textBoxToValidate.setAttribute("onkeyup", "<c:choose><c:when test="${s:getProperty('env.customer.id.optional')}"></c:when><c:otherwise>validate(this,'^.{5,50}$','emptynotok')</c:otherwise></c:choose>;");
                }
            }

                </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">        
        
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="customer"/> ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/Customer.action">                                
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveCustomer"><fmt:message key="manage.customer"/></stripes:option>                                
                            </stripes:select>
                            <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>         
            <stripes:form action="/Customer.action" focus="" id="form_edit" autocomplete="off" method="POST" onsubmit="enableNiraSuppliedFields(); return (alertValidationErrors() && checkRequiredDocuments());">    
                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                <stripes:hidden name="customer.version" value="${actionBean.customer.version}"/>
                <stripes:hidden name="customer.classification" value="${actionBean.customer.classification}"/>
                <stripes:hidden name="comingFromMakeSale" value="${actionBean.comingFromMakeSale}"/>

                <stripes:select name="customer.foreign.id.document.types"  id="customer.foreign.document.id.types" style="display:none">
                    <stripes:option value=""></stripes:option>
                    <stripes:option value="${actionBean.customer.identityNumberType}">
                        <fmt:message   key="document.type.${actionBean.customer.identityNumberType}" />
                    </stripes:option>
                    <c:forEach items="${s:getPropertyAsList('env.foreign.customer.personal.id.document.types')}" var="documentType" varStatus="loop">
                        <stripes:option value="${documentType}">
                            <fmt:message  key="document.type.${documentType}"/>
                        </stripes:option>
                    </c:forEach>
                </stripes:select>

                <stripes:select name="customer.id.document.types"  id="customer.document.id.types" style="display:none">
                    <stripes:option value=""></stripes:option>
                    <stripes:option value="${actionBean.customer.identityNumberType}">
                        <fmt:message   key="document.type.${actionBean.customer.identityNumberType}" />
                    </stripes:option>
                    <c:forEach items="${s:getPropertyAsList('env.customer.personal.id.document.types')}" var="documentType" varStatus="loop">
                        <stripes:option value="${documentType}">
                            <fmt:message  key="document.type.${documentType}"/>
                        </stripes:option>
                    </c:forEach>
                </stripes:select>

                <table class="clear">
                    <tr>
                        <td><b><stripes:label for="classification"/>:</b></td>
                        <td><fmt:message   key="classification.${actionBean.customer.classification}" /></td>
                    </tr>
                    <c:if test="${s:getPropertyWithDefault('env.customer.kycstatus.display', false)}">
                        <tr>
                            <td><stripes:label for="kycstatus"/>:</td>
                            <td>
                                <c:set var="defaultRule" value="validate(this,'^[PVUOI]{1}$','emptynotok')"/>
                                <stripes:select name="customer.KYCStatus"  onchange="${s:getValidationRule('kycstatus',defaultRule)}">
                                    <stripes:option value=""><fmt:message key="kycstatus."/></stripes:option>
                                    <stripes:option value="P"><fmt:message key="kycstatus.P"/></stripes:option>
                                    <stripes:option value="V"><fmt:message key="kycstatus.V"/></stripes:option>
                                    <stripes:option value="U"><fmt:message key="kycstatus.U"/></stripes:option>
                                    <c:choose>
                                        <c:when test="${s:getProperty('env.customer.verify.with.nira')}">
                                            <stripes:option value="O"><fmt:message key="kycstatus.O"/></stripes:option>
                                            <stripes:option value="I"><fmt:message key="kycstatus.I"/></stripes:option>
                                        </c:when>
                                        <c:when test="${s:getPropertyWithDefault('env.kycstatus.organisational.enabled', false)}">
                                            <stripes:option value="O"><fmt:message key="kycstatus.O"/></stripes:option>
                                        </c:when>
                                        <c:otherwise>
                                        </c:otherwise>    
                                    </c:choose>            
                                </stripes:select>
                            </td>
                        </tr>
                    </c:if>
                    <tr>
                        <td>Title:</td>
                        <td>
                            <stripes:select name="customer.title">
                                <stripes:option value=""></stripes:option>                                                                   
                                <c:forEach items="${s:getPropertyAsList('env.customer.titles')}" var="title">
                                    <stripes:option value="${title}">${title}</stripes:option>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td><stripes:label for="first.name"/>:</td>
                        <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                        <td><stripes:text name="customer.firstName"    id="customer.firstName"  maxlength="50" size="20" onkeyup="${s:getValidationRule('middlename',defaultRule)}"/></td>
                    </tr>
                    <tr>
                        <td><stripes:label for="middle.name"/>:</td>
                        <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptyok')"/>
                        <td><stripes:text name="customer.middleName" id="customer.middleName" maxlength="50" size="20" onkeyup="${s:getValidationRule('middlename',defaultRule)}"/></td>
                    </tr>
                    <tr>
                        <td><stripes:label for="last.name" />:</td>
                        <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                        <td><stripes:text name="customer.lastName" id="customer.lastName" maxlength="50" size="20" onkeyup="${s:getValidationRule('lastname',defaultRule)}"/></td>
                    </tr>
                    <tr>
                        <td><stripes:label for="gender"/>:</td>
                        <td>
                            <stripes:select name="customer.gender"  id="customer.gender" onchange="validate(this,'^[MF]{1}$','emptynotok')">
                                <stripes:option value=""></stripes:option>
                                <stripes:option value="F"><fmt:message key="acronym.F"/></stripes:option>
                                <stripes:option value="M"><fmt:message key="acronym.M"/></stripes:option>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td><stripes:label for="mothers.maidenname" />:</td>
                        <td>
                            <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptyok')"/>
                            <stripes:text name="customer.mothersMaidenName" value="${actionBean.customer.mothersMaidenName}" maxlength="50" size="20" onkeyup="${s:getValidationRule('mothersmaidenname',defaultRule)}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><stripes:label for="ssoidentity"/>:</td>
                        <td><stripes:text name="customer.SSOIdentity" maxlength="50" size="20" onkeyup="validate(this,'^.{3,50}$','emptynotok')"/></td>
                    </tr>
                    <c:choose>
                        <c:when test="${customerClassType == 'minor'}">
                            <!--Do nothing -->
                            <stripes:hidden name="customer.identityNumberType" value=""/>
                            <stripes:hidden name="customer.identityNumber" value=""/>
                            <tr>
                                <td>
                                    <stripes:label for="date.of.birth"/>:
                                </td>
                                <td>
                                    <stripes:text name="customer.dateOfBirth" readonly="true" id="datePicker1" size="10" onkeyup="validate(this,'^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$','emptydatenotok')" maxlength="10" />
                                </td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td>
                                    <stripes:label for="date.of.birth"/>:
                                </td>
                                <td>
                                    <stripes:text name="customer.dateOfBirth" readonly="false" id="datePicker1" size="10" onkeyup="validate(this,'^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$','emptydatenotok')" maxlength="10" />
                                </td>
                            </tr>

                            <tr>
                                <td>
                                    <stripes:label for="nationality"/>:
                                </td>
                                <td>
                                    <stripes:select name="customer.nationality" id="customer.nationality" onchange="checkSelection(this); return validate(this,'^[a-zA-Z]{2,2}$','emptynotok');">
                                        <stripes:option value=""></stripes:option>
                                        <c:forEach items="${s:getPropertyFromSQL('global.country.codes')}" var="country" varStatus="loop">
                                            <c:choose>
                                                <c:when test='${actionBean.customer.nationality == country[1]}'>
                                                    <stripes:option value="${country[1]}" selected="selected">
                                                        ${country[0]} - ${country[1]}
                                                    </stripes:option>
                                                </c:when>
                                                <c:otherwise>
                                                    <stripes:option value="${country[1]}">
                                                        ${country[0]} - ${country[1]}
                                                    </stripes:option>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:forEach>
                                    </stripes:select>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <stripes:label for="id.number.type"/>:
                                </td>
                                <td>
                                    <stripes:hidden name="customer.identityNumberType" id="customer.identityNumberType" value=""/>
                                    <c:choose>
                                        <c:when test="${s:getProperty('env.customer.id.optional')}">
                                            <c:set var="defaultRule" value="validate(this,'^[a-zA-Z]{1,50}$','emptyok')"/>
                                            <stripes:select name="selIdentityType" id="selIdentityType" onchange="validateSelection(this.value); togglePassportExpiryDate(); document.getElementById('customer.identityNumberType').value = this.value;return ${s:getValidationRule('id.type',defaultRule)}">   
                                                <stripes:option value=""></stripes:option>

                                                <c:forEach items="${s:getPropertyAsList('env.customer.personal.id.document.types')}" var="identityType" varStatus="loop">
                                                    <c:choose>
                                                        <c:when test='${actionBean.customer.identityNumberType == identityType}'>
                                                            <stripes:option value="${identityType}" selected="selected">
                                                                <fmt:message   key="document.type.${identityType}" />
                                                            </stripes:option>
                                                        </c:when>   
                                                        <c:otherwise>
                                                            <stripes:option value="${identityType}">
                                                                <fmt:message   key="document.type.${identityType}" />
                                                            </stripes:option>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:forEach>
                                            </stripes:select>
                                        </c:when>
                                        <c:otherwise>
                                            <stripes:select name="selIdentityType" id="selIdentityType" onchange="validateSelection(this.value); togglePassportExpiryDate(); document.getElementById('customer.identityNumberType').value = this.value;return validate(this,'^[a-zA-Z]{1,50}$','emptynotok')">
                                                <stripes:option value=""></stripes:option>

                                                <c:forEach items="${s:getPropertyAsList('env.customer.personal.id.document.types')}" var="identityType" varStatus="loop">
                                                    <c:choose>
                                                        <c:when test='${actionBean.customer.identityNumberType == identityType}'>
                                                            <stripes:option value="${identityType}" selected="selected">
                                                                <fmt:message   key="document.type.${identityType}" />
                                                            </stripes:option>
                                                        </c:when>   
                                                        <c:otherwise>
                                                            <stripes:option value="${identityType}">
                                                                <fmt:message   key="document.type.${identityType}" />
                                                            </stripes:option>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:forEach>
                                            </stripes:select>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr> 
                            <tr>
                                <td><stripes:label for="id.number"/>:</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${s:getProperty('env.customer.id.optional')}">
                                            <stripes:text name="customer.identityNumber" id="customer.identityNumber" maxlength="50" size="20" onkeyup="validate(this,'^.{5,50}$','emptyok')" />
                                        </c:when>
                                        <c:otherwise>
                                            <stripes:text name="customer.identityNumber" id="customer.identityNumber" maxlength="50" size="20" onkeyup="validate(this,'^.{5,50}$','emptynotok')" />
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                    
                        <c:if test="${s:getProperty('env.customer.national.id.mandatory')}">
                            <tr>
                                <td><stripes:label for="national.id.number"/>:</td>
                                <td>
                                    <stripes:text name="customer.nationalIdentityNumber" id="customer.nationalIdentityNumber" maxlength="11" size="20" onkeypress="return isNumberKey(event)" onkeyup="validate(this,'^.{11,11}$','emptyok')" />
                                </td>
                            </tr>
                        </c:if>
                            <c:choose>
                                <c:when test="${s:getProperty('env.customer.verify.with.nira') and actionBean.displayCardNumberField}">
                                    <c:set var="defaultRule" value="validate(this,'^{6,30}$','emptyok')"/>
                                    <tr id="idCardNumberRow">
                                        <td><stripes:label for="id.card.number"/>:</td>
                                        <td><stripes:text name="customer.cardNumber"  id="customer.cardNumber" maxlength="50" size="20" onkeyup="${s:getValidationRule('cardnumber',defaultRule)}" /> </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                </c:otherwise>    
                            </c:choose>
                            <tr id="passportExpiryDateRow">
                                <td><stripes:label for="passport.expiry.date"/>:</td>
                                <td><stripes:text id="datePicker2" name="customer.passportExpiryDate" maxlength="10" size="10"/></td>
                            </tr>
                            <c:if test="${actionBean.customer.nationality != countryCode}"> 
                                <c:set var="defaultRule" value="validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptyok');"/>
                                <c:if test="${s:getProperty('env.customer.visa.enabled')}">
                                    <tr id="visaExpiryDateRow">
                                        <td><stripes:label for="visa.expiry.date"/>:</td>
                                        <td><stripes:text id="datePicker3" name="customer.visaExpiryDate" maxlength="10" size="10" onkeyup="${s:getValidationRule('visa.expirydate',defaultRule)}"/></td>
                                    </tr>

                                </c:if>
                            </c:if>     
                        </c:otherwise>
                    </c:choose>
                    <tr>
                        <td><stripes:label for="language"/>:</td>
                        <td>
                            <stripes:select name="customer.language">
                                <c:forEach items="${actionBean.allowedLanguages}" var="language" varStatus="loop"> 
                                    <stripes:option value="${language}"><fmt:message key="language.${language}"/></stripes:option>
                                </c:forEach>                                
                            </stripes:select>                        
                        </td>
                    </tr>                
                    <tr>
                        <c:set var="defaultRule" value="validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/,'emptynotok')"/>
                        <td><stripes:label for="email"/>:</td>
                        <td><stripes:text name="customer.emailAddress" maxlength="200" size="50" onkeyup="${s:getValidationRule('email.address',defaultRule)}" /></td>
                    </tr>                
                    <tr>
                        <!-- <c:set var="defaultRule" value="validate(this, '^[+()0-9 -]{10,15}$','emptynotok')"/>   onkeyup="${s:getValidationRule('alternative.contact',defaultRule)}" -->
                        <td><stripes:label for="alternative.contact.number.1" />:</td>
                        <td><stripes:text name="customer.alternativeContact1" id="customer.alternativeContact1" maxlength="15" size='15' /></td>
                    </tr>
                    <tr>
                         <!--<c:set var="defaultRule" value="validate(this, '^[+()0-9 -]{5,15}$','emptyok')"/> -->
                        <td><stripes:label for="alternative.contact.number.2"/>:</td>
                        <td><stripes:text name="customer.alternativeContact2" maxlength="15" size="15" /></td>
                    </tr>

                    <tr>
                        <td><stripes:label for="opt.in.level"/>:</td>
                        <td>
                            <stripes:select name="customer.optInLevel">
                                <c:forEach items="${actionBean.allowedOptInLevels}" var="level"> 
                                    <c:if test="${actionBean.customer.optInLevel == level}">
                                        <stripes:option value="${level}" selected="selected"><fmt:message key="opt.in.level.${level}"/></stripes:option>
                                    </c:if>
                                    <c:if test="${actionBean.customer.optInLevel != level}">
                                        <stripes:option value="${level}"><fmt:message key="opt.in.level.${level}"/></stripes:option>
                                    </c:if>
                                </c:forEach>               
                            </stripes:select>
                        </td>
                    </tr>

                    <tr>
                        <td><stripes:label for="warehouse.id"/>:</td>
                        <td><stripes:text name="customer.warehouseId" maxlength="15" size="15"/></td>
                    </tr>

                    <tr>
                        <td colspan="2">
                            <span class="button">                                
                                <stripes:submit name="updateCustomer" onclick="onUpdate=true;"/>
                            </span>                        
                        </td>
                    </tr> 
                </table>                
            </stripes:form>
        <	
    </stripes:layout-component>
</stripes:layout-render>

