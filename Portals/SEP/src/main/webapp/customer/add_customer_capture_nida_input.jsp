<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include/sep_include.jsp" %>


<c:set var="title">
    <fmt:message key="add.new.customer.basic.information"/>
</c:set>

<c:set var="customerClassType" value="${s:getDelimitedPropertyValueMapping('env.customer.classifications', actionBean.customer.classification)}"/>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="html_head">            
        <script type="text/javascript">
            var nationality = "";
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
            $j(document).ready(function () { /*passportexpirydate*/
                $j('#datePicker2').datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", changeYear: true, changeMonth: true, onSelect: function () {
            ${s:getValidationRule('passport.expirydate',"validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])$', 'emptynotok');")}
                    }});
            });
            /* function for diplomat check box */
            $j(document).ready(function () {
                $j('#captureDiplomatDocuments').hide();
                $j('#diplomat-check').change(function () {
                    if (this.checked === true) {
                        $j('#row0').hide();
                        $j('#captureDiplomatDocuments').show();
                        $j('#getCustomersNidaDetailsAndUpdateCustomer').hide();
                    } else {
                        $j('#row0').show();
                        $j('#getCustomersNidaDetailsAndUpdateCustomer').show();
                        $j('#captureDiplomatDocuments').hide();
                    }
                });
            });
            $j(document).ready(function () { /*passportexpirydate*/
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
                var passport = document.getElementById("passport");
                var countryCodeRow = document.getElementById("countryCodeRow");

                var options = identityTypeSel.options;
                console.log("Country code of OpCo is " + countryCode + " and value is " + obj.value);

                if (passport.value === 'passport') {
                    countryCodeRow.style.display = "table-row";
                } else {
                    countryCodeRow.value = "";
                    countryCodeRow.style.display = "none";

                }


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

            window.onload = function () {
//                var countryCodeRow = document.getElementById("countryCodeRow");
//                if (countryCodeRow !== null) {
//                    countryCodeRow.value = "";
//                    countryCodeRow.style.display = "none";
//                    checkSelection(countryCodeRow);
//                }
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
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">
            <stripes:form action="/Customer.action" focus="" autocomplete="off" onsubmit="if ((onBack == true) || alertValidationErrors())
                        return true;
                    else {
                        onNext = false;
                        return false;
                    }">   

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

                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>

                <div> 
                    <table class="clear">
                        <tr>
                            <td><b><stripes:label for="classification"/>:</b></td>
                            <td><fmt:message   key="classification.${actionBean.customer.classification}" /></td>
                        </tr>
                        <c:choose>
                            <c:when test="${actionBean.verifyExistingCustomerWithNIDAOrImmigration}">
                                <tr>
                                    <td><stripes:label for="foreigneridtype"/>:</td>
                                <td>
                                <stripes:radio name="customer.identityNumberType" value="nationalid" />National ID   
                                <stripes:radio name="customer.identityNumberType" onchange="checkSelection(this);" value="passport" id="passport" />Passport
                                </td>
                                </tr>
                            </c:when>


                            <c:when test="${actionBean.verifyExistingCustomerImmigration}">
                                <tr>
                                    <td><stripes:label for="foreigneridtype"/>:</td>
                                <td>
                                <stripes:radio name="customer.identityNumberType" value="nationalid" />National ID
                                <stripes:radio name="customer.identityNumberType" onchange="checkSelection(this);" value="passport" id="passport" />Passport
                                </td>
                                </tr>
                            </c:when>
                        </c:choose>

                        <c:choose>
                            <c:when test="${'foreigner'.equalsIgnoreCase(actionBean.customer.classification)}">
                                <tr id="countryCodeRow">
                                    <td><stripes:label for="countyCode"/>:</td>
                                <td><stripes:text name="customer.countryCode"  id="customer.countryCode" value="${actionBean.customer.countryCode}" /></td>
                                </tr>
                            </c:when>
                            <c:when test="${actionBean.verifyExistingCustomerWithNIDAOrImmigration}">
                                <tr id="countryCodeRow">
                                    <td><stripes:label for="countyCode"/>:</td>
                                <td><stripes:text name="customer.countryCode"  id="customer.countryCode" value="${actionBean.customer.countryCode}" /></td>
                                </tr>
                            </c:when>
                        </c:choose>

                        <tr>
                            <c:choose>
                                <c:when test="${'minor'.equalsIgnoreCase(actionBean.customer.classification)}">
                                    <td><stripes:label for="id.number.parent"/>:</td>
                                </c:when>
                                <c:when test="${'company'.equalsIgnoreCase(actionBean.customer.classification)}">
                                <td><stripes:label for="id.number.representative"/>:</td>
                            </c:when>
                            <c:otherwise>
                                <td><stripes:label for="id.number"/>:</td>
                            </c:otherwise>
                        </c:choose>
                        <td>
                            <c:choose>
                                <c:when test="${actionBean.verifyExistingCustomerWithNIDA}">
                                    <b>${actionBean.customer.identityNumber}</b>
                                    <input type="hidden"  name="customer.identityNumber"  id="customer.identityNumber" value="${actionBean.customer.identityNumber}"/>
                                </c:when>
                                <c:otherwise>
                                    <c:choose>
                                        <c:when test="${s:getProperty('env.customer.id.optional')}">
                                        <stripes:text name="customer.identityNumber"  id="customer.identityNumber" value="${actionBean.customer.identityNumber}" maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptyok')" />
                                    </c:when>
                                    <c:otherwise>
                                        <stripes:text name="customer.identityNumber"  id="customer.identityNumber" value="${actionBean.customer.identityNumber}" maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptynotok')" />
                                    </c:otherwise>
                                </c:choose>
                            </c:otherwise>
                        </c:choose>
                        </td>
                        </tr>
                       
                        <c:choose>
                            <c:when test="${s:getPropertyWithDefault('env.new.cutomer.fingerprint.required', false)}">
                                <c:choose>
                                    <c:when test="${actionBean.verifyExistingCustomerWithNIDA}">
                                        <tr>
                                            <td>Are you a diplomat</td>  
                                            <td><stripes:checkbox id = "diplomat-check" name="idType" value="diplomat"/></td>
                                        </tr>    
                                    </c:when>
                                </c:choose>
                                <c:choose>
                                    <c:when test="${!('minor'.equalsIgnoreCase(actionBean.customer.classification)) and !('company'.equalsIgnoreCase(actionBean.customer.classification))}">                           
                                        <tr id="row0">
                                            <td align="left" valign="top">
                                        <stripes:select id="photoType0" name="customer.customerPhotographs[0].photoType" onchange="validate(this, '^[a-zA-Z]{1,50}$', 'emptynotok')">
                                            <stripes:option value="fingerprints">Fingerprints</stripes:option>
                                        </stripes:select>
                                        </td>
                                        <td align="left" valign="top">

                                            <c:choose>
                                                <c:when test='${s:getListSize(actionBean.customer.customerPhotographs) > 0 && actionBean.customer.customerPhotographs[0].photoType.matches("^.*fingerprint.*$")}'>
                                                    <a href="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg" target="_blank">
                                                        <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg"/>
                                                    </a>
                                                </c:when>
                                                <c:otherwise>
                                                    <div id="imgDiv0">
                                                        <img class='thumb' id='imgfile0' src='images/upload.png' />
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>


                                        </td>
                                        <td align="left" valign="top">
                                            <input type = 'button' class='file' value = 'Browse ...' onclick ="javascript:document.getElementById('file0').click();" />
                                            <input type="hidden"  id="photoGuid0" name="customer.customerPhotographs[0].photoGuid" value="${actionBean.customer.customerPhotographs[0].photoGuid}"/>
                                            <input type="file" class="file" id="file0" name="file0" style="visibility: hidden;" />
                                            <script type="text/javascript">
                                                document.getElementById('file0').addEventListener('change', uploadDocument, false);
                                            </script>
                                        </td>
                                        </tr>
                                    </c:when>
                                </c:choose> 
                            </c:when>
                        </c:choose> 
                    </table>
                </div>
                <span class="button">
                    <stripes:submit name="showAddCustomerBackIntoSearchAndConvertSaleslead" onclick="onBack = true;"/>
                </span>

                <span class="button">
                    <c:choose>
                        <c:when test="${actionBean.verifyExistingCustomerWithNIDA}">
                            <stripes:submit name="getCustomersNidaDetailsAndUpdateCustomer" id="getCustomersNidaDetailsAndUpdateCustomer" onclick="onNext = true;"/>
                            <stripes:submit name="captureDiplomatDocuments" id="captureDiplomatDocuments" />
                        </c:when>
                        <c:when test="${actionBean.verifyExistingCustomerWithNIDAOrImmigration}">
                            <stripes:submit name="getCustomersImmigrationDetailsAndUpdateCustomer" onclick="onNext = true;"/>
                        </c:when>

                        <c:otherwise>
                            <stripes:submit name="getCustomersNidaDetailsAndShowAddCustomerBasicDetails" onclick="onNext = true;"/>
                        </c:otherwise>
                    </c:choose>
                </span>

                <stripes:hidden name="TTIssue.ID" value="${actionBean.TTIssue.ID}"/>
                <stripes:hidden name="customer.customerStatus" value="AC" />
                <stripes:hidden name="customer.warehouseId" value="" />
                <stripes:hidden name="customer.classification" value="${actionBean.customer.classification}" />
                <stripes:hidden name="customer.SSOIdentity" value="${actionBean.customer.SSOIdentity}"/>
                <stripes:hidden name="customer.SSODigest" value="${actionBean.customer.SSODigest}"/>

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

    </stripes:layout-component>
</stripes:layout-render>