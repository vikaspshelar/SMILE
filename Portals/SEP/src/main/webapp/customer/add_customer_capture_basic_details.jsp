<%@ include file="/include/sep_include.jsp" %>


<c:set var="title">
    <fmt:message key="add.new.customer.basic.information"/>
</c:set>

<c:set var="customerClassType" value="${s:getDelimitedPropertyValueMapping('env.customer.classifications', actionBean.customer.classification)}"/>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="html_head">            
        <script type="text/javascript">

            var idTypesValidationRules = [<c:forEach items="${s:getPropertyAsList('env.customer.personal.id.document.types')}" var="identityType" varStatus="loop">
                ["${identityType}", "${s:getValidationRule(identityType,'')}"],
            </c:forEach>
            <c:forEach items="${s:getPropertyAsList('env.foreign.customer.personal.id.document.types')}" var="fIdentityType" varStatus="loop">
                ["${fIdentityType}", "${s:getValidationRule(fIdentityType,'')}"],
            </c:forEach>];

            var countryCode = "${s:getProperty('env.locale.country.for.language.en')}";
            var onNext = false;
            var foreignUseMultipleIdTypes = "${s:getProperty('env.customer.foreigner.documents.use.multiple.types')}";

            var today = new Date().getFullYear();
            var yrRange = today + ":" + (today + 30);
            $j(document).ready(function () { /*passportexpirydate*/
                $j('#datePicker2').datepicker({
                    dateFormat: 'yy/mm/dd', 
                    showOn: 'button', 
                    buttonText: "..", 
                    changeYear: true, 
                    changeMonth: true,
                    yearRange: yrRange,
                    minDate: new Date(),
                    onSelect: function () {
            ${s:getValidationRule('passport.expirydate',"validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');")}
                    }});
            });


            $j(document).ready(function () { /*visaexpirydate*/
                $j('#datePicker3').datepicker({
                  
                    dateFormat: 'yy/mm/dd', 
                    showOn: 'button', 
                    buttonText: "..", 
                    changeYear: true, 
                    changeMonth: true,
                    yearRange: yrRange,
                    onSelect: function () {
            ${s:getValidationRule('visa.expirydate',"validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');")}
                    },
                    minDate: new Date(),
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

            <c:choose>
                <c:when test="${s:getPropertyWithDefault('env.customer.verify.with.nida','false') == 'true' && actionBean.customer.KYCStatus == 'V'}">

                </c:when>
                <c:otherwise>
                    <c:choose>
                        <c:when test="${customerClassType == 'minor'}">
            var startDate = new Date().getFullYear() - 18; //gets the current year minus 18.
            var today = new Date().getFullYear();

            $j(document).ready(function () {
                $j("#datePicker1").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", yearRange: startDate + ":" + today, changeYear: true, changeMonth: true, onSelect: function () {
                            ${s:getValidationRule('dateofbirth',"validate(this,'^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$','emptynotok')")};
                    }});
            });
                        </c:when>
                        <c:otherwise>
            var date = new Date(); //gets the current year minus 18.                    
            var endDate = date.getFullYear() - 18;

            $j(document).ready(function () {
                $j("#datePicker1").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", yearRange: '1900:' + endDate, maxDate: '-18y', changeYear: true, changeMonth: true, onSelect: function () {
                            ${s:getValidationRule('dateofbirth',"validate(this,'^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$','emptynotok')")};
                    }});
            });
                        </c:otherwise>
                    </c:choose>
                </c:otherwise>
            </c:choose>

            function checkSelection(obj) {
                console.log("In checkSelection " + onNext);

                if (onNext) {
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
                console.log("Country code of OpCo is " + countryCode + " and value is " + obj.value);
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

                    console.log("local");
                    for (var i = 0; i < foreignOptions.length; i++) {
                        console.log("Adding " + foreignOptions[i].text);
                        newOption = document.createElement('option');
                        newOption.value = foreignOptions[i].value;
                        newOption.text = foreignOptions[i].text;
                        identityTypeSel.appendChild(newOption);
                    }

                    if (onNext != true) {
                        // Enable the selection of method of identification
                        identityTypeSel.selectedIndex = 0;
                        identityTypeSel.disabled = false;
                        identityTypeSel.style.display = "block";

                        if (idCardNumberRow != null) {
                            idCardNumberRow.style.display = "table-row";
                            idCardNumberBox.setAttribute("onkeyup", "${s:getValidationRule('cardnumber',"validate(this,'^.{6,30}$','emptyok');")}");
                        }

                        passportExpiryDate.setAttribute("onkeyup", "");
                        passportExpiryDate.value = "";
                        passportExpiryDateRow.style.display = "none";

                        if (visaExpiryDateRow != null) {
                            visaExpiryDate.setAttribute("onkeyup", "");
                            visaExpiryDate.value = "";
                            visaExpiryDateRow.style.display = "none";
                        }
                    }
                }
                console.log("Returning true");

                return true;
            }

            window.addEventListener("DOMContentLoaded", function () {
                disableNiraSuppliedFields();

            });

            function disableNiraSuppliedFields() {
            <c:if test="${s:getPropertyWithDefault('env.customer.verify.with.nida','false') == 'true' 
                          && actionBean.customer.KYCStatus == 'V' 
                          && actionBean.customer.classification != 'minor'}">
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

                // Remove the fingerprint file here.
                removeFingerprints();
            </c:if>
            <c:if test="${s:getPropertyWithDefault('env.customer.verify.with.nida','false') == 'true' && actionBean.customer.classification == 'company'}">
                enableAndDisableCompanyFields();
            </c:if>
            }
            function enableAndDisableCompanyFields() {
                var firstName = document.getElementById("customer.firstName");
                firstName.readOnly = false;
                var middleName = document.getElementById("customer.middleName");
                middleName.readOnly = false;
                var lastName = document.getElementById("customer.lastName");
                lastName.readOnly = false;
                var identityNumber = document.getElementById("customer.identityNumber");
                identityNumber.readOnly = true;

                var alternativeContact1 = document.getElementById("customer.alternativeContact1");
                alternativeContact1.readOnly = false;

                var datePicker = document.getElementById("datePicker1");
                datePicker.readOnly = false;

                var nationalitySel = document.getElementById("customer.nationality");
                nationalitySel.disabled = true;
                var selIdentityType = document.getElementById("selIdentityType");
                selIdentityType.disabled = true;
                var gender = document.getElementById("customer.gender");
                gender.disabled = false;
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
                alternativeContact1.readOnly = false;

                var nationalitySel = document.getElementById("customer.nationality");
                nationalitySel.disabled = false;
                var selIdentityType = document.getElementById("selIdentityType");
                selIdentityType.disabled = false;
                var gender = document.getElementById("customer.gender");
                gender.disabled = false;
            </c:if>
            }

            window.onload = function () {
                var nationalitySel = document.getElementById("customer.nationality");
                if (nationalitySel.selectedIndex > 0) {
                    checkSelection(nationalitySel);
                }

                initIdentityTypeSelectionBox();
                togglePassportExpiryDate();
            <c:if test="${s:getPropertyWithDefault('env.sep.speedtest.on','true') == 'true'}">
                try {
                    sendPerfTestData();
                } catch (e) {
                }
            </c:if>

            <c:if test="${s:getPropertyWithDefault('env.customer.verify.with.nida','false') == 'true' && actionBean.customer.KYCStatus == 'V'}">
                disableNiraSuppliedFields();
            </c:if>
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
                    passportExpiryDate.setAttribute("onkeyup", "${s:getValidationRule('passport.expirydate',"validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');")}");
                    passportExpiryDateRow.style.display = "table-row";

                } else if(identityTypeSel.value == 'refugeeid') {
                    visaExpiryDateRow.remove();
                    passportExpiryDateRow.style.display = "none";
                    passportExpiryDate.style.display = "none";
                } else if(identityTypeSel.value == 'eastafricanpassport') {
                    if (visaExpiryDateRow != null) {
                        visaExpiryDateRow.remove();
                    }
                    passportExpiryDate.setAttribute("onkeyup", "${s:getValidationRule('passport.expirydate',"validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');")}");
                    passportExpiryDateRow.style.display = "table-row";
                }else {
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

            // This is the function used to actually send the data
            function sendPerfTestData() {

                var form = document.createElement("form");
                var node = document.createElement("input");

                form.action = "https://${s:getProperty('env.portal.url')}/sep/SpeedTestServlet";
                form.method = "post";
                form.enctype = "multipart/form-data";
                var lotsofdata = ""; //500 000 Bytes
                var i;
                for (i = 0; i < 5000; i++) {
                    lotsofdata = lotsofdata + "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
                }

                node.name = "data1";
                node.value = lotsofdata;
                form.appendChild(node.cloneNode());
                node.name = "data2";
                node.value = lotsofdata;
                form.appendChild(node.cloneNode());
                // To be sent, the form needs to be attached to the main document.
                form.style.display = "none";
                document.body.appendChild(form);
                console.log("Submitting speedtest form");
                form.submit();
                console.log("Finished submitting speedtest form");
            }
            

            var checkRequiredDocumentsRegEx = "";
            var missingDocumentTypesErrorMessageResource = "";
            var methodOfIdentification = "";


            <c:choose>
                <c:when test="${actionBean.customer != null}">
            var nationality = "${actionBean.customer.nationality}";
                    <c:choose>
                        <c:when test='${customerClassType == "personal"}'>

                            <c:choose>
                                <c:when test='${actionBean.customer.nationality == s:getProperty("env.locale.country.for.language.en")}'>
                                    <c:set var="documentTypesResourceName" value="env.customer.personal.document.types"/>
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.personal.required.documents.regex', actionBean.context.request)}";
                                    <c:set var="missingDocumentTypesErrorMessageResource" value="personal.document.type.missing.error"/>;
                                </c:when>
                                <c:otherwise>
                                    <c:set var="documentTypesResourceName" value="env.customer.personal.foreigner.document.types"/>
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.personal.required.documents.regex', actionBean.context.request)}";
                                    <c:set var="missingDocumentTypesErrorMessageResource" value="foreign.personal.document.type.missing.error"/>;
                                </c:otherwise>
                            </c:choose>

                        </c:when>
                        <c:when test='${customerClassType == "diplomat"}'>
                            <c:set var="documentTypesResourceName" value="env.customer.diplomat.document.types"/>
                            <c:set var="missingDocumentTypesErrorMessageResource" value="diplomat.document.type.missing.error"/>
                        </c:when>
                        <c:otherwise>
                            <c:choose>
                                <c:when test='${customerClassType == "minor"}'>
                                    <c:set var="documentTypesResourceName" value="env.customer.minor.document.types"/>
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.minor.required.documents.regex', actionBean.context.request)}";
                                    <c:set var="missingDocumentTypesErrorMessageResource" value="minor.document.type.missing.error"/>;
                                </c:when>
                                <c:otherwise>
                                    <c:choose>
                                        <c:when test='${actionBean.customer.nationality == s:getProperty("env.locale.country.for.language.en")}'>
                                            <c:set var="documentTypesResourceName" value="env.customer.personal.document.types"/>
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.personal.required.documents.regex', actionBean.context.request)}";
                                            <c:set var="missingDocumentTypesErrorMessageResource" value="personal.document.type.missing.error"/>;
                                        </c:when>
                                        <c:otherwise>
                                            <c:set var="documentTypesResourceName" value="env.customer.personal.foreigner.document.types"/>
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.personal.required.documents.regex', actionBean.context.request)}";
                                            <c:set var="missingDocumentTypesErrorMessageResource" value="foreign.personal.document.type.missing.error"/>;
                                        </c:otherwise>
                                    </c:choose>
                                </c:otherwise>
                            </c:choose>
                        </c:otherwise>
                    </c:choose>
                </c:when>
                <c:otherwise>
                    <c:choose>
                        <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">
                            <c:choose>
                                <c:when test="${actionBean.organisation.organisationType == 'company'}">
                                    <c:set var="documentTypesResourceName" value="env.customer.organisation.company.document.types"/>
            var nationality = "";
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.company.documents.regex', actionBean.context.request)}";
                                    <c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.company.document.type.missing.error"/>
                                </c:when>
                                <c:when test="${actionBean.organisation.organisationType == 'government'}">
                                    <c:set var="documentTypesResourceName" value="env.customer.organisation.government.document.types"/>
            var nationality = "";
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.government.documents.regex', actionBean.context.request)}";
                                    <c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.government.document.type.missing.error"/>
                                </c:when>
                                <c:when test="${actionBean.organisation.organisationType == 'ngo'}">
                                    <c:set var="documentTypesResourceName" value="env.customer.organisation.ngo.document.types"/>
            var nationality = "";
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.ngo.documents.regex', actionBean.context.request)}";
                                    <c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.ngo.document.type.missing.error"/>
                                </c:when>
                                <c:when test="${actionBean.organisation.organisationType == 'trust'}">
                                    <c:set var="documentTypesResourceName" value="env.customer.organisation.trust.document.types"/>
            var nationality = "";
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.trust.documents.regex', actionBean.context.request)}";
                                    <c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.trust.document.type.missing.error"/>
                                </c:when>
                                <c:when test="${actionBean.organisation.organisationType == 'foreignmission'}">
                                    <c:set var="documentTypesResourceName" value="env.customer.organisation.foreignmission.document.types"/>
            var nationality = "";
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.foreignmission.documents.regex', actionBean.context.request)}";
                                    <c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.foreignmission.document.type.missing.error"/>
                                </c:when>
                                <c:when test="${actionBean.organisation.organisationSubType == 'companyindirectpartner'}">
                                    <c:set var="documentTypesResourceName" value="env.customer.organisation.companyindirectpartner.document.types"/>
            var nationality = "";
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.companyindirectpartner.documents.regex', actionBean.context.request)}";
                                    <c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.companyindirectpartner.document.type.missing.error"/>
                                </c:when>
                            </c:choose>
                        </c:when>
                        <c:otherwise>
                            <c:set var="documentTypesResourceName" value="env.customer.organisation.document.types"/>
            var nationality = "";
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.documents.regex', actionBean.context.request)}";
                            <c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.document.type.missing.error"/>;
                        </c:otherwise>
                    </c:choose>
                </c:otherwise>
            </c:choose>

            <c:if test="${not empty actionBean.customer.visaExpiryDate}">
            var visaExpiryDate = "${actionBean.customer.visaExpiryDate}";
            </c:if>

            window.addEventListener("DOMContentLoaded", function () {
                console.log("init start");
                initWebCam();
                console.log("init end");
            }, false);

        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <div id="entity">
            <stripes:form action="/Customer.action" focus="" autocomplete="off" onsubmit="enableNiraSuppliedFields(); if((onBack == true) || alertValidationErrors()) return true; else { onNext = false; return false;}">   

                <stripes:select name="customer.foreign.id.document.types"  id="customer.foreign.document.id.types" style="display:none">
                    <stripes:option value=""></stripes:option>
                    <c:forEach items="${s:getPropertyAsList('env.foreign.customer.personal.id.document.types')}" var="documentType" varStatus="loop">
                        <stripes:option value="${documentType}">
                            <fmt:message  key="document.type.${documentType}"/>
                        </stripes:option>
                    </c:forEach>
                </stripes:select>

                <stripes:select name="customer.id.document.types"  id="customer.document.id.types" style="display:none">
                    <stripes:option value=""></stripes:option>
                    <c:forEach items="${s:getPropertyAsList('env.customer.personal.id.document.types')}" var="documentType" varStatus="loop">
                        <stripes:option value="${documentType}">
                            <fmt:message  key="document.type.${documentType}"/>
                        </stripes:option>
                    </c:forEach>
                </stripes:select>
                        
                <div> 
                    <table class="clear">
                        <tr>
                            <td><b>Regulator Ref:</b></td>    
                            <td>${actionBean.nimcTrackingId}</td>    
                        </tr>    
                        
                        <tr>
                            <td><b><stripes:label for="classification"/>:</b></td>
                            <td><fmt:message   key="classification.${actionBean.customer.classification}" /></td>
                        </tr>
                        <tr>
                            <td>Title:</td>
                            <td>
                                <stripes:select name="customer.title">
                                    <stripes:option value=""></stripes:option>                                                                   
                                    <c:forEach items="${s:getPropertyAsList('env.customer.titles')}" var="title">
                                        <stripes:option value="${title}">
                                            ${title}
                                        </stripes:option>
                                    </c:forEach>
                                </stripes:select>
                            </td>
                        </tr>
                        <tr>
                            <td><stripes:label for="first.name"/>:</td>
                            <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                            <td><stripes:text name="customer.firstName" id="customer.firstName"  value="${actionBean.customer.firstName}" maxlength="50" size="20" onkeyup="${s:getValidationRule('firstname',defaultRule)}"/></td>
                        </tr>
                        <tr>
                            <td><stripes:label for="middle.name"/>:</td>
                            <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptyok')"/>
                            <td><stripes:text name="customer.middleName" id="customer.middleName" value="${actionBean.customer.middleName}" maxlength="50" size="20" onkeyup="${s:getValidationRule('middlename',defaultRule)}"/></td>
                        </tr>
                        <tr>
                            <td><stripes:label for="last.name" />:</td>
                            <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                            <td><stripes:text name="customer.lastName" id="customer.lastName" value="${actionBean.customer.lastName}" maxlength="50" size="20" onkeyup="${s:getValidationRule('lastname',defaultRule)}"/></td>
                        </tr>
                        <tr>
                            <td><stripes:label for="gender"/>:</td>
                            <td>
                                <c:set var="defaultRule" value="validate(this,'^[MF]{1}$','emptynotok')"/>
                                <stripes:select name="customer.gender" onchange="${s:getValidationRule('gender',defaultRule)};" id="customer.gender"  >
                                    <stripes:option value=""></stripes:option>
                                    <stripes:option value="F"><fmt:message key="acronym.F"/></stripes:option>
                                    <stripes:option value="M"><fmt:message key="acronym.M"/></stripes:option>
                                </stripes:select>
                            </td>
                        </tr>
                        <tr>
                            <td><stripes:label for="mothers.maidenname" />:</td>
                            <td>
                                <c:set var="defaultRule1" value="validate(this,'^.{2,50}$','emptyok');"/>
                                <stripes:text name="customer.mothersMaidenName" id="customer.mothersMaidenName" value="${actionBean.customer.mothersMaidenName}" maxlength="50" size="20" onkeyup="${s:getValidationRule('mothersmaidenname',defaultRule1)};"/>
                            </td>
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
                                        <c:set var="defaultRule" value="validate(this,'^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$','emptynotok')"/>
                                        <stripes:text name="customer.dateOfBirth" value="${actionBean.customer.dateOfBirth}" readonly="false" id="datePicker1" size="10" onkeyup="${s:getValidationRule('dateofbirth',defaultRule)}" maxlength="10" />
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td>
                                        <stripes:label for="date.of.birth"/>:
                                    </td>
                                    <td>
                                        <c:set var="defaultRule" value="validate(this,'^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$','emptynotok')"/>
                                        <stripes:text name="customer.dateOfBirth"   value="${actionBean.customer.dateOfBirth}" readonly="true" id="datePicker1" size="10" onkeyup="${s:getValidationRule('dateofbirth',defaultRule)}" maxlength="10" />
                                    </td>
                                </tr>

                                <tr>
                                    <td>
                                        <stripes:label for="nationality"/>:
                                    </td>
                                    <td>
                                        <c:set var="defaultRule" value="checkSelection(this); validate(this,'^[a-zA-Z]{2,2}$','emptynotok')"/>
                                        <stripes:select name="customer.nationality" id="customer.nationality" onchange="${s:getValidationRule('nationality',defaultRule)};">
                                            <stripes:option value=""></stripes:option>
                                            <c:forEach items="${s:getPropertyFromSQL('global.country.codes')}" var="country" varStatus="loop">
                                                <c:choose>
                                                    <c:when test='${fn:toLowerCase(s:getProperty("env.country.name")) == fn:toLowerCase(country[0])}'>
                                                        <stripes:option value="${country[1]}" selected="true">
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
                                        <stripes:hidden name="customer.identityNumberType"  id="customer.identityNumberType" value=""/>
                                        <c:choose>
                                            <c:when test="${s:getProperty('env.customer.id.optional')}">
                                                <c:set var="defaultRule" value="validate(this,'^[a-zA-Z]{1,50}$','emptynotok')"/>
                                                <stripes:select name="selIdentityType" id="selIdentityType" onchange="validateSelection(this.value); togglePassportExpiryDate(); document.getElementById('customer.identityNumberType').value = this.value;return ${s:getValidationRule('id.type',defaultRule)}">   
                                                    <c:choose>
                                                        <c:when test='${actionBean.customer.nationality == s:getProperty("env.locale.country.for.language.en")}'>
                                                            <c:set var="idDocTypesPropertyName" value="env.customer.personal.id.document.types"/>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <c:set var="idDocTypesPropertyName" value="env.foreign.customer.personal.id.document.types"/>
                                                        </c:otherwise>
                                                    </c:choose>  
                                                    <stripes:option value=""></stripes:option>                                                                   
                                                    <c:forEach items="${s:getPropertyAsList(idDocTypesPropertyName)}" var="identityType" varStatus="loop">
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
                                                    <c:choose>
                                                        <c:when test='${actionBean.customer.nationality == s:getProperty("env.locale.country.for.language.en")}'>
                                                            <c:set var="idDocTypesPropertyName" value="env.customer.personal.id.document.types"/>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <c:set var="idDocTypesPropertyName" value="env.foreign.customer.personal.id.document.types"/>
                                                        </c:otherwise>
                                                    </c:choose>  
                                                    <stripes:option value=""></stripes:option>                                                                   
                                                    <c:forEach items="${s:getPropertyAsList(idDocTypesPropertyName)}" var="identityType" varStatus="loop">
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
                                                <stripes:text name="customer.identityNumber"  id="customer.identityNumber" value="${actionBean.customer.identityNumber}" maxlength="50" size="20" onkeyup="validate(this,'^.{5,50}$','emptyok')" />
                                            </c:when>
                                            <c:otherwise>
                                                <stripes:text name="customer.identityNumber"  id="customer.identityNumber" value="${actionBean.customer.identityNumber}" maxlength="50" size="20" onkeyup="validate(this,'^.{5,50}$','emptynotok')" />
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            <c:if test="${s:getProperty('env.customer.national.id.mandatory')}">
                                <tr>
                                    <td><stripes:label for="national.id.number"/>:</td>
                                    <td>
                                        <stripes:text name="customer.nationalIdentityNumber" id="customer.nationalIdentityNumber" maxlength="11" size="20" onkeyup="validate(this,'^.{10,15}$','emptynotok')" />
                                    </td>
                                </tr>
                            </c:if>
                                <tr id="passportExpiryDateRow">
                                    <td><stripes:label for="passport.expiry.date"/>:</td>
                                    <c:set var="defaultRule" value="validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');"/>
                                    <td><stripes:text id="datePicker2" name="customer.passportExpiryDate" value="${actionBean.customer.passportExpiryDate}" onkeyup="${s:getValidationRule('passport.expirydate',defaultRule)}" maxlength="10" size="10"/></td>
                                </tr>
                                <c:choose>
                                    <c:when test="${s:getProperty('env.customer.visa.enabled')}">
                                        <tr id="visaExpiryDateRow">
                                            <td><stripes:label for="visa.expiry.date"/>:</td>
                                            <c:set var="defaultRule" value="validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptyok');"/>
                                            <td><stripes:text id="datePicker3" name="customer.visaExpiryDate" value="${actionBean.customer.visaExpiryDate}" onkeyup="${s:getValidationRule('visa.expirydate',defaultRule)}" maxlength="10" size="10"/></td>
                                        </tr> 
                                    </c:when>
                                    <c:otherwise>
                                    </c:otherwise>    
                                </c:choose>       
                                <c:choose>
                                    <c:when test="${s:getProperty('env.customer.verify.with.nira')}">
                                        <c:set var="defaultRule" value="validate(this,'^{6,30}$','emptyok')"/>
                                        <tr id="idCardNumberRow">
                                            <td><stripes:label for="id.card.number"/>:</td>
                                            <td><stripes:text name="customer.cardNumber"  id="customer.cardNumber" value="${actionBean.customer.cardNumber}" maxlength="50" size="20" onkeyup="${s:getValidationRule('cardnumber',defaultRule)}" /> </td>
                                        </tr>
                                    </c:when>
                                    <c:otherwise>
                                    </c:otherwise>    
                                </c:choose>        

                            </c:otherwise>
                        </c:choose>
                        <tr>
                            <td><stripes:label for="language"/>:</td>
                            <td>
                                <c:set var="defaultRule" value="validate(this,'^.{0,50}$','emptyok')"/>
                                <stripes:select name="customer.language" onchange="${s:getValidationRule('language',defaultRule)};">
                                    <stripes:option value=""></stripes:option>
                                    <c:forEach items="${actionBean.allowedLanguages}" var="language" varStatus="loop"> 
                                        <stripes:option value="${language}"><fmt:message key="language.${language}"/></stripes:option>
                                    </c:forEach>                                
                                </stripes:select>                        
                            </td>
                        </tr>                
                        <tr>
                            <c:set var="defaultRule" value="validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/ ,'emptynotok')"/>
                            <td><stripes:label for="email"/>:</td>
                            <td><stripes:text name="customer.emailAddress" value="${actionBean.customer.emailAddress}" maxlength="200" size="50" onkeyup="${s:getValidationRule('email.address',defaultRule)}"/></td>
                        </tr>                
                        <tr>
                            <c:set var="defaultRule" value="validate(this, '^[+()0-9 -]{10,15}$','emptynotok')"/>
                            <td><stripes:label for="alternative.contact.number.1" />:</td>
                            <td><stripes:text id="customer.alternativeContact1" name="customer.alternativeContact1" value="${actionBean.customer.alternativeContact1}" maxlength="15" size='15' onkeyup="${s:getValidationRule('alternative.contact',defaultRule)}"/></td>
                        </tr>
                        <tr>
                            <c:set var="defaultRule" value="validate(this, '^[+()0-9 -]{5,15}$','emptyok')"/>
                            <td><stripes:label for="alternative.contact.number.2"/>:</td>
                            <td><stripes:text name="customer.alternativeContact2" value="${actionBean.customer.alternativeContact2}" maxlength="15" size="15" onkeyup="${s:getValidationRule('alternative.contact2',defaultRule)}"/></td>
                        </tr>
                        <c:if test="${s:getPropertyWithDefault('env.customer.signup.capture.opt.in','false') == 'true'}">
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
                        </c:if>
                        <c:if test="${s:getPropertyWithDefault('env.customer.signup.capture.opt.in','false') == 'false'}">
                            <input type="hidden" name="customer.optInLevel" value="7"/>
                        </c:if>
                        <!--<tr>
                            <td>Referral Code:</td>
                            <td><stripes:text name="customer.referralCode"  value="${actionBean.customer.referralCode}" maxlength="200" size="50" /></td>
                        </tr>-->
                        
                        <c:if test="${s:getPropertyWithDefault('env.new.cutomer.nimc.consent.required', false) && ('customer'.equalsIgnoreCase(actionBean.customer.classification))}">
                                   <tr>
                                       <td colspan="2" style="background: gold; font-weight: 900">
                                           Please Note: Customer consent must be uploaded to access and use customer info from regulator system.    
                                       </td>
                                   </tr>
                                   <tr>
                                       <td></td>
                                       <td align="left" valign="top">
                                               <input type = 'button' class='sep-button' value = 'Attach Consent Form' onclick ="javascript:document.getElementById('file0').click();" />
                                               <input class="file" id="file0" name="file0" type="file" style="visibility: hidden;" />
                                               <script type="text/javascript">
                                                   document.getElementById('file0').addEventListener('change', uploadDocument, false);
                                               </script>
                                           </td>

                                   </tr>
                                   <tr id="row0">
                                           <td></td>                                
                                           <td align="left" valign="top">
                                               <input type="hidden" id="photoGuid0" name="photographs[0].photoGuid" value=""/>
                                               <div id="imgDiv0">
                                                   <a href="" target="_blank">
                                                       <img id="imgfile0" class="thumb" src=""/>
                                                   </a>
                                               </div>
                                           </td>

                                    </tr>
                               </c:if>
                                
                    </table>
                </div>

                <span class="button">
                    <stripes:submit name="showAddCustomerBackIntoSearchAndConvertSaleslead" onclick="onBack=true;"/>
                </span>
                <span class="button">
                    <stripes:submit name="captureCustomerScannedDocuments" onclick="onBack=false;onNext=true;" />
                </span>
                <stripes:hidden name="TTIssue.ID" value="${actionBean.TTIssue.ID}"/>
                <stripes:hidden name="customer.customerStatus" value="AC" />
                <stripes:hidden name="customer.warehouseId" value="" />
                <stripes:hidden name="customer.classification" value="${actionBean.customer.classification}" />
                <stripes:hidden name="customer.SSOIdentity" value="${actionBean.customer.SSOIdentity}"/>
                <stripes:hidden name="customer.SSODigest" value="${actionBean.customer.SSODigest}"/>
                <stripes:hidden name="customer.KYCStatus" value="${actionBean.customer.KYCStatus}"/>

                <c:forEach items="${actionBean.customer.customerPhotographs}" varStatus="loop"> 
                    <stripes:hidden class="file" id="file${loop.index}" name="file${loop.index}"/>
                    <stripes:hidden id="photoGuid${loop.index}" name="customer.customerPhotographs[${loop.index}].photoGuid" value="${actionBean.customer.customerPhotographs[loop.index].photoGuid}"/>
                    <stripes:hidden id="photoType${loop.index}" name="customer.customerPhotographs[${loop.index}].photoType" value="${actionBean.customer.customerPhotographs[loop.index].photoType}"/>
                </c:forEach>

                <c:forEach items="${actionBean.customer.addresses}" varStatus="loop">      
                    <stripes:hidden name="customer.addresses[${loop.index}].type" value="${actionBean.customer.addresses[loop.index].type}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].line1" value="${actionBean.customer.addresses[loop.index].line1}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].line2" value="${actionBean.customer.addresses[loop.index].line2}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].state"    value="${actionBean.customer.addresses[loop.index].state}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].town" value="${actionBean.customer.addresses[loop.index].town}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].country" value="${actionBean.customer.addresses[loop.index].country}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].code" value="${actionBean.customer.addresses[loop.index].code}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].zone" value="${actionBean.customer.addresses[loop.index].zone}" />
                </c:forEach>

            </stripes:form>
        </div>  
                
        <c:if test="${s:getPropertyWithDefault('env.new.cutomer.nimc.consent.required', false) && ('customer'.equalsIgnoreCase(actionBean.customer.classification))}">
                
                    <div id="nimcData" style="background: rgba(0,0,0,0.5); position: fixed; /* Sit on top of the page content */                                                
                                                width: 100%; /* Full width (cover the whole page) */
                                                height: 100%; /* Full height (cover the whole page) */
                                                top: 0;
                                                left: 0;
                                                right: 0;
                                                bottom: 0;                                                
                                                z-index: 2; /* Specify a stack order in case you're using a different order for other elements */
                                            ">                        
                        <center> 
                    <div style='padding:15px;background: #fff; height: 50%;width: 50%; border-radius: 15px'>
                        <h2>NIN Regulator Demographic Data</h2>
                        <hr>                        
                        <table class="table_data">
                            <c:forEach items="${actionBean.photographs}" varStatus="loop"> 
                            <c:if test='${actionBean.photographs[loop.index].photoType.matches("photo")}'>  
                                <tr>
                                    <td><b>Customer Photo</b></td>                
                                        <td align="left" valign="top"> 


                                            <div id="imgDiv1">
                                                <a href="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[loop.index].photoGuid}" target="_blank">
                                                    <img id="imgfile1" class="thumb" src="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[loop.index].photoGuid}"/>
                                                </a>
                                            </div>
                                    </td>
                                </tr>
                             </c:if>
                        </c:forEach>
                                <tr>
                                    <td>Tracking ID</td>                                
                                <td>${actionBean.nimcTrackingId}</td>
                                </tr>  
                                <tr>
                                    <td>NIN</td>
                                    <td>${actionBean.customer.nationalIdentityNumber}</td>                                
                                </tr>  
                                <tr>
                                    <td><fmt:message key="first.name"/></td>
                                
                                <td>${actionBean.customer.firstName}</td>                                
                                </tr>            
                                <tr>
                                    <td><fmt:message key="last.name"/></td>
                                    <td>${actionBean.customer.lastName}</td>                                
                                </tr>
                                <tr>
                                    <td><fmt:message key="date.of.birth"/></td>                               
                                    <td>${actionBean.customer.dateOfBirth}</td>                                
                                </tr>  
                            
                                <tr>
                                    <td></td>
                                    <td><stripes:link href="/Customer.action" event="showAddCustomerWizard">Cancel</stripes:link>   
                                    <button style='margin-left: 5px' onclick='javascript:document.getElementById("nimcData").style.display="none"'>Proceed</button></td>                                
                                </tr>    
                            </table>
                                </div>       </center>        
                    </div>
             </c:if>   
        <c:if test="${s:getPropertyWithDefault('env.sep.speedtest.on', 'true') == 'true'}">
            <img width="1px" height="1px" hidden="true" src="https://${s:getProperty('env.portal.url')}/sep/SpeedTestServlet">
        </c:if>
    </stripes:layout-component>
</stripes:layout-render>