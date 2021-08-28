<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include/sep_include.jsp" %>


<c:set var="title">
    <fmt:message key="compare.customer.regulator.data"/>
</c:set>

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
        <style>
            .table_data {
                border-collapse: collapse;
                width: 90%;
                margin: 15px;
            }

            th, td {
                text-align: left;
                padding: 8px;
            }

            tr:nth-child(even) {background-color: #F4F8EB;}
        </style>

    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <c:set var="matched" value="${true}" />
        
        
            <stripes:form action="/Customer.action">   
                <c:choose>
                    
                    <c:when test="${actionBean.ninVerified}">
                        <c:if test="${s:getListSize(actionBean.photographs) > 0}">
                            <table class="table_data">
                                <tr>
                                    <td></td>
                                    <td>
                                        <b><fmt:message key="scanned.documents"/></b>
                                    </td>
                                    <td>
                                        <b><fmt:message key="nimc.documents"/></b>
                                    </td>
                                </tr>                               
                                <tr>
                                    <td align="left" valign="top">
                                <fmt:message  key="document.type.${actionBean.photographs[0].photoType}"/>
                                </td>
                                <td align="left" valign="top">                                            
                                    <a href="/sep/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[0].photoGuid}" target="_blank">
                                        <img id="imgfile0" class="thumb" src="/sep/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[0].photoGuid}"/>
                                    </a>

                                </td>
                                <td align="left" valign="top">

                                    <a href="/sep/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[1].photoGuid}" target="_blank">
                                        <img id="imgfile1" class="thumb" src="/sep/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[1].photoGuid}"/>
                                    </a>

                                </td>
                                </tr>
                                
                            </table>
                        </c:if>
                        <div>
                            <table class="table_data"> 
                                <th>
                                <td align="center"><b>Smile KYC Data</b></td>    
                                <td align="center"><b>Regulator Data</b></td>    
                                </th>
                                <tr>
                                    <td>Tracking ID</td>
                                <td></td>
                                <td>${actionBean.customers[1].customerNinData.ninVerificationTrackingId}</td>
                                </tr>  
                                <tr>
                                    <td><fmt:message key="national.id.number"/></td>
                                <td>${actionBean.customers[0].nationalIdentityNumber}</td>
                                <td>${actionBean.customers[1].nationalIdentityNumber}</td>
                                </tr>  
                                <tr>
                                    <td><fmt:message key="first.name"/></td>
                                <td>${actionBean.customers[0].firstName}</td>
                                <td>${actionBean.customers[1].firstName}</td>
                                <td>                                        
                                    <c:choose>
                                        <c:when test="${!actionBean.customers[0].firstName.trim().equalsIgnoreCase(actionBean.customers[1].firstName.trim()) }">
                                            <span  style='color:red'>Mismatch</span>
                                            <c:set var="matched" value="${false}" />
                                        </c:when>    
                                        <c:otherwise>
                                            <span  style='color:green'>Matched</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                </tr>            
                                <tr>
                                    <td><fmt:message key="last.name"/></td>
                                <td>${actionBean.customers[0].lastName}</td>
                                <td>${actionBean.customers[1].lastName}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${!actionBean.customers[0].lastName.trim().equalsIgnoreCase(actionBean.customers[1].lastName.trim()) }">
                                            <span  style='color:red'>Mismatch</span>
                                            <c:set var="matched" value="${false}" />
                                        </c:when>    
                                        <c:otherwise>
                                            <span  style='color:green'>Matched</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key="date.of.birth"/></td>
                                <td>${actionBean.customers[0].dateOfBirth}</td>                    
                                <td>${actionBean.customers[1].dateOfBirth}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${!actionBean.customers[0].dateOfBirth.trim().equalsIgnoreCase(actionBean.customers[1].dateOfBirth.trim()) }">
                                            <span  style='color:red'>Mismatch</span>
                                            <c:set var="matched" value="${false}" />
                                        </c:when>    
                                        <c:otherwise>
                                            <span  style='color:green'>Matched</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                </tr>    
                            </table>


                            <center>
                                <table>
                                    <tr>

                                        <c:if test="${(actionBean.isSimSwap() || actionBean.isNewSim() || actionBean.isAddSim()) && !matched}" >
                                        <br/><br/>
                                        <span style="color:red; background-color: yellow">
                                            <b><fmt:message key="some.values.did.not.match.nimc"/></b>
                                        </span>
                                        <td>

                                        <stripes:form action="/Customer.action">                                                             
                                            <input type="hidden" id="processType" name="processType" value="simswap"/>
                                            <input type="hidden" name="customer.nationalIdentityNumber"  id="customer.nationalIdentityNumber" value="${actionBean.customer.nationalIdentityNumber}" size="20" maxlength="20" onkeyup="validate(this, '^[0-9]{10,20}$', '')"/>                                                                                                                      
                                            <input type="hidden" name="noConsent" id="noConsent" value="false"/>
                                            <stripes:submit name="uploadConsentForm"/>                                                           
                                            <script type="text/javascript">
                                                document.getElementById('file0').addEventListener('change', uploadDocument, false);
                                            </script>

                                        </stripes:form>
                                        </td>
                                    </c:if>

                                    <c:if test="${actionBean.isSimSwap() && matched && !actionBean.isIsNIMCVerifiedAndApproved()}" >                                                    
                                        <td>                                                    
                                        <stripes:form action="/Customer.action">
                                            <input type="hidden" id="processType" name="processType" value="simswap"/>
                                            <input type="hidden" name="customer.nationalIdentityNumber"  id="customer.nationalIdentityNumber" value="${actionBean.customer.nationalIdentityNumber}" size="20" maxlength="20" onkeyup="validate(this, '^[0-9]{10,20}$', '')"/>                                                                                                                      
                                            <input type="hidden" name="noConsent" id="noConsent" value="true"/>
                                            <stripes:submit name="sendForApproval"/>
                                        </stripes:form>

                                        </td>
                                    </c:if>
                                    <c:if test="${actionBean.isSimSwap() && matched && actionBean.isIsNIMCVerifiedAndApproved()}" >
                                        <td>
                                        <stripes:form action="/Customer.action">
                                            <stripes:submit name="proceedToSIMSwap"/>
                                        </stripes:form>

                                        </td>
                                    </c:if>
                                     <c:if test="${actionBean.isNewSim() && matched}" >
                                     
                                        <td>
                                            <stripes:submit name="proceedToQuote"/>
                                        </td>
                                    </c:if>   
                                      <c:if test="${actionBean.isAddSim() && matched}" >
                                         
                                        <td>
                                            <stripes:submit name="proceedToAddProduct"/>
                                        </td>
                                    </c:if>    
                                    </tr>
                                </table>  
                                
                                <table>
                                    <tr>
                                        <c:if test="${actionBean.isVerifyAction()}" >
                                            <stripes:form action="/Customer.action">  
                                                <input type="hidden" name="customer.nationalIdentityNumber"  id="customer.nationalIdentityNumber" value="${actionBean.customer.nationalIdentityNumber}" size="20" maxlength="20" onkeyup="validate(this, '^[0-9]{10,20}$', '')"/>                                                
                                                <stripes:submit name="verifyNinAndActivateService"/>  
                                            </stripes:form>
                                        </c:if>
                                        
                                        
                                        <c:if test="${!actionBean.isAddSim() && !actionBean.isNewSim() && !actionBean.isSimSwap() && !actionBean.isVerifyAction()}">
                                            <c:if test="${matched}" >
                                                <td>
                                            <stripes:form action="/Customer.action">                                             
                                                <input type="hidden" name="customer.nationalIdentityNumber"  id="customer.nationalIdentityNumber" value="${actionBean.customer.nationalIdentityNumber}" size="20" maxlength="20" onkeyup="validate(this, '^[0-9]{10,20}$', '')"/>                                                
                                                <stripes:submit name="verifyNin"/>                                                 
                                            </stripes:form>
                                            </td>
                                            </c:if>
                                            <td>
                                            <stripes:form action="/Customer.action">                                                                                            
                                                <input type="hidden" name="customer.nationalIdentityNumber"  id="customer.nationalIdentityNumber" value="${actionBean.customer.nationalIdentityNumber}" size="20" maxlength="20" onkeyup="validate(this, '^[0-9]{10,20}$', '')"/>                                                
                                                <stripes:submit name="unVerifyNin"/>  
                                            </stripes:form>
                                            </td>
                                            <td>
                                            <stripes:form action="/Customer.action"> 
                                                <input type="hidden" value="${actionBean.photographs[1].photoGuid}" name="thirdpartypic"  id="thirdpartypic"/>                                        
                                                <input type="hidden" name="customer.nationalIdentityNumber"  id="customer.nationalIdentityNumber" value="${actionBean.customer.nationalIdentityNumber}" size="20" maxlength="20" onkeyup="validate(this, '^[0-9]{10,20}$', '')"/>                                                
                                                <stripes:submit name="changeKYCPhotoToRegulatorPhoto"/>  
                                            </stripes:form>
                                            </td>
                                        </c:if>
                                    </tr>
                                </table>
                            </center> 
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div> 
                            <table class="clear">                       
                                <tr>
                                    <td><stripes:label for="national.id.number"/>:</td>
                                <td>
                                    <input type="hidden" value="true" name="forcompare" id="forcompare" />                                    
                                    <c:choose>
                                        <c:when test="${actionBean.customer==null}" >                                           
                                            <stripes:text name="customer.nationalIdentityNumber"  id="customer.nationalIdentityNumber" value="" size="20" maxlength="11" onkeypress="return isNumberKey(event)"  onkeyup="validate(this, '^.{11,11}$', '')"/>
                                        </c:when>
                                        <c:otherwise>
                                            <stripes:text name="customer.nationalIdentityNumber" readonly="true"  id="customer.nationalIdentityNumber" value="${actionBean.customer.nationalIdentityNumber}" size="20"  maxlength="11" onkeypress="return isNumberKey(event)" onkeyup="validate(this, '^.{11,11}$', '')"/>
                                        </c:otherwise>
                                    </c:choose>
                                </td>

                                </tr>                      
                                <tr>
                                    <td><stripes:label for="regulator.comparison.type"/>:</td>
                                <td>

                                <stripes:select name="datasrc"  id="datasrc">
                                    <stripes:option value="-1">Select Comparison Source</stripes:option>
                                    <stripes:option value="smileid">
                                        Smile Identity (3rd party data)
                                    </stripes:option>
                                    <stripes:option value="nimc">
                                        Directly from Regulator (NIMC)
                                    </stripes:option>

                                </stripes:select>

                                </td>

                                </tr>                      

                            </table>
                        </div>
                        <input type="hidden" name="verifyAction" id="verifyAction" value="${actionBean.verifyAction}"/>
                        <span class="button">
                            <stripes:submit name="retriveComparisonData"/>
                        </span>
                    </c:otherwise>
                </c:choose>
                
                
            </stripes:form>
        
    </stripes:layout-component>
</stripes:layout-render>
