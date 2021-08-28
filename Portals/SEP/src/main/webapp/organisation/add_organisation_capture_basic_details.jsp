<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.new.organisation.basic.information"/>
</c:set>

<c:set var="customerClassType" value="organisation"/>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">
            var onNext = false;
            function toggleOrganisationType() {

                if (onNext) {
                    return true;
                }

                var organisationType = document.getElementById("organisation.organisationType");

                var organisationnameRow = document.getElementById("organisationname");
                var companynumberRow = document.getElementById("companynumber");
                var taxnumberRow = document.getElementById("taxnumber");
                var emailRow = document.getElementById("email");
                var contactnumber1Row = document.getElementById("contactnumber1");
                var contactnumber2Row = document.getElementById("contactnumber2");
                var industryRow = document.getElementById("industry");
                var organisationsizeRow = document.getElementById("organisationsize");

                var organisationSubType = document.getElementById("organisation.organisationSubType");

                if (organisationType.value === 'government') {

                    var dataOfSubType = document.getElementById('organisation.organisationSubType.government').options;
                    var newOption = null;
                    removeOptions(organisationSubType);

                    console.log("local");
                    for (var i = 0; i < dataOfSubType.length; i++) {
                        console.log("Adding " + dataOfSubType[i].text);
                        newOption = document.createElement('option');
                        newOption.value = dataOfSubType[i].value;
                        newOption.text = dataOfSubType[i].text;
                        organisationSubType.appendChild(newOption);
                    }
                    organisationnameRow.style.display = "table-row";
                    companynumberRow.style.display = "none";
                    taxnumberRow.style.display = "none";
                    emailRow.style.display = "table-row";
                    contactnumber1Row.style.display = "table-row";
                    contactnumber2Row.style.display = "table-row";
                    industryRow.style.display = "none";
                    organisationsizeRow.style.display = "none";
                } else if (organisationType.value === 'ngo') {

                    var dataOfSubType = document.getElementById('organisation.organisationSubType.ngo').options;
                    var newOption = null;
                    removeOptions(organisationSubType);

                    console.log("local");
                    for (var i = 0; i < dataOfSubType.length; i++) {
                        console.log("Adding " + dataOfSubType[i].text);
                        newOption = document.createElement('option');
                        newOption.value = dataOfSubType[i].value;
                        newOption.text = dataOfSubType[i].text;
                        organisationSubType.appendChild(newOption);
                    }
                    organisationnameRow.style.display = "table-row";
                    companynumberRow.style.display = "table-row";
                    taxnumberRow.style.display = "table-row";
                    emailRow.style.display = "table-row";
                    contactnumber1Row.style.display = "table-row";
                    contactnumber2Row.style.display = "table-row";
                    industryRow.style.display = "none";
                    organisationsizeRow.style.display = "table-row";
                } else if (organisationType.value === 'trust') {

                    var dataOfSubType = document.getElementById('organisation.organisationSubType.trust').options;
                    var newOption = null;
                    removeOptions(organisationSubType);

                    console.log("local");
                    for (var i = 0; i < dataOfSubType.length; i++) {
                        console.log("Adding " + dataOfSubType[i].text);
                        newOption = document.createElement('option');
                        newOption.value = dataOfSubType[i].value;
                        newOption.text = dataOfSubType[i].text;
                        organisationSubType.appendChild(newOption);
                    }
                    organisationnameRow.style.display = "table-row";
                    companynumberRow.style.display = "table-row";
                    taxnumberRow.style.display = "table-row";
                    emailRow.style.display = "table-row";
                    contactnumber1Row.style.display = "table-row";
                    contactnumber2Row.style.display = "table-row";
                    industryRow.style.display = "none";
                    organisationsizeRow.style.display = "none";
                } else if (organisationType.value === 'foreignmission') {

                    var dataOfSubType = document.getElementById('organisation.organisationSubType.foreignmission').options;
                    var newOption = null;
                    removeOptions(organisationSubType);

                    console.log("local");
                    for (var i = 0; i < dataOfSubType.length; i++) {
                        console.log("Adding " + dataOfSubType[i].text);
                        newOption = document.createElement('option');
                        newOption.value = dataOfSubType[i].value;
                        newOption.text = dataOfSubType[i].text;
                        organisationSubType.appendChild(newOption);
                    }
                    organisationnameRow.style.display = "table-row";
                    companynumberRow.style.display = "none";
                    taxnumberRow.style.display = "none";
                    emailRow.style.display = "table-row";
                    contactnumber1Row.style.display = "table-row";
                    contactnumber2Row.style.display = "table-row";
                    industryRow.style.display = "none";
                    organisationsizeRow.style.display = "none";
                } else {
                    var dataOfSubType = document.getElementById('organisation.organisationSubType.company').options;
                    var newOption = null;
                    removeOptions(organisationSubType);

                    console.log("local");
                    for (var i = 0; i < dataOfSubType.length; i++) {
                        console.log("Adding " + dataOfSubType[i].text);
                        newOption = document.createElement('option');
                        newOption.value = dataOfSubType[i].value;
                        newOption.text = dataOfSubType[i].text;
                        organisationSubType.appendChild(newOption);
                    }
                    organisationnameRow.style.display = "table-row";
                    companynumberRow.style.display = "table-row";
                    taxnumberRow.style.display = "table-row";
                    emailRow.style.display = "table-row";
                    contactnumber1Row.style.display = "table-row";
                    contactnumber2Row.style.display = "table-row";
                    industryRow.style.display = "table-row";
                    organisationsizeRow.style.display = "table-row";
                }
            }

        </script>
        <div id="entity">
            <stripes:form action="/Customer.action" focus="" autocomplete="off" onsubmit="return ((onBack == true) ? true : alertValidationErrors());">

                <c:choose>
                    <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">
                        <stripes:select name="organisation.organisationSubType.government" id="organisation.organisationSubType.government" style="display:none">
                            <stripes:option value=""></stripes:option>
                            <c:forEach items="${s:getPropertyAsList('env.organisation.new.subtypes.government')}" var="orgType" varStatus="loop">
                                <stripes:option value="${orgType}">
                                    <fmt:message   key="organisation.type.${orgType}" />
                                </stripes:option>
                            </c:forEach>
                        </stripes:select>

                        <stripes:select name="organisation.organisationSubType.ngo" id="organisation.organisationSubType.ngo" style="display:none">
                            <stripes:option value=""></stripes:option>
                            <c:forEach items="${s:getPropertyAsList('env.organisation.new.subtypes.ngo')}" var="orgType" varStatus="loop">
                                <stripes:option value="${orgType}">
                                    <fmt:message   key="organisation.type.${orgType}" />
                                </stripes:option>
                            </c:forEach>
                        </stripes:select>

                        <stripes:select name="organisation.organisationSubType.trust" id="organisation.organisationSubType.trust" style="display:none">
                            <stripes:option value=""></stripes:option>
                            <c:forEach items="${s:getPropertyAsList('env.organisation.new.subtypes.trust')}" var="orgType" varStatus="loop">
                                <stripes:option value="${orgType}">
                                    <fmt:message   key="organisation.type.${orgType}" />
                                </stripes:option>
                            </c:forEach>
                        </stripes:select>

                        <stripes:select name="organisation.organisationSubType.foreignmission" id="organisation.organisationSubType.foreignmission" style="display:none">
                            <stripes:option value=""></stripes:option>
                            <c:forEach items="${s:getPropertyAsList('env.organisation.new.subtypes.foreignmission')}" var="orgType" varStatus="loop">
                                <stripes:option value="${orgType}">
                                    <fmt:message   key="organisation.type.${orgType}" />
                                </stripes:option>
                            </c:forEach>
                        </stripes:select>

                        <stripes:select name="organisation.organisationSubType.company" id="organisation.organisationSubType.company" style="display:none">
                            <stripes:option value=""></stripes:option>
                            <c:forEach items="${s:getPropertyAsList('env.organisation.new.subtypes')}" var="orgType" varStatus="loop">
                                <stripes:option value="${orgType}">
                                    <fmt:message   key="organisation.type.${orgType}" />
                                </stripes:option>
                            </c:forEach>
                        </stripes:select>
                    </c:when>
                    <c:otherwise>
                    </c:otherwise>
                </c:choose>
                <c:set var="documentTypesResourceName" value="env.customer.organisation.document.types"/>
                <div>      
                    <table class="clear">
                        <tr>
                            <td><stripes:label for="organisation.type"/>:</td>
                        <td>
                        <c:choose>
                            <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">
                                <stripes:select name="organisation.organisationType" id="organisation.organisationType" onchange="toggleOrganisationType();
                                        validate(this, '^[a-zA-Z]{1,50}$', 'emptyok')">
                                    <stripes:option value=""></stripes:option>
                                    <c:forEach items="${s:getPropertyAsList('env.organisation.new.types')}" var="orgType" varStatus="loop">
                                        <stripes:option value="${orgType}">
                                            <fmt:message   key="organisation.type.${orgType}" />
                                        </stripes:option>
                                    </c:forEach>
                                </stripes:select>
                            </c:when>
                            <c:otherwise>
                                <stripes:select name="organisation.organisationType" id="organisation.organisationType" onchange="validate(this, '^[a-zA-Z]{1,50}$', 'emptyok')">
                                    <stripes:option value=""></stripes:option>
                                    <c:forEach items="${s:getPropertyAsList('env.organisation.types')}" var="orgType" varStatus="loop">
                                        <stripes:option value="${orgType}">
                                            <fmt:message   key="organisation.type.${orgType}" />
                                        </stripes:option>
                                    </c:forEach>
                                </stripes:select>
                            </c:otherwise>
                        </c:choose>
                        </td>
                        </tr>

                        <c:choose>
                            <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
                                <tr id="organisationSubTypeCompany">
                                    <td><stripes:label for="organisation.subtype"/>:</td>
                                <td>
                                <stripes:select name="organisation.organisationSubType" id="organisation.organisationSubType" onchange="validate(this, '^[a-zA-Z]{1,50}$', 'emptyok')">
                                    <c:set var="organisationSubTypes" value="env.organisation.new.subtypes"/>
                                    <stripes:option value=""></stripes:option>              
                                    <c:forEach items="${s:getPropertyAsList(organisationSubTypes)}" var="orgType" varStatus="loop">
                                        <stripes:option value="${orgType}">
                                            <fmt:message   key="organisation.type.${orgType}" />
                                        </stripes:option>
                                    </c:forEach>
                                </stripes:select>
                                </td>
                                </tr>
                            </c:when>
                        </c:choose>

                        <!--        <tr>
                                    <td><stripes:label for="organisation.name"/>:</td>
                                <td><stripes:text  id="organisation.organisationName" name="organisation.organisationName"   maxlength="200" size="50" onkeyup="validate(this, '^.{2,200}$', 'emptynotok')" /></td>
                                </tr>-->
                        <c:choose>
                            <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
                                <tr id="organisationname">
                                    <td><stripes:label for="organisation.name"/>:</td>
                                <td><stripes:text  id="organisation.organisationName" name="organisation.organisationName"   maxlength="200" size="50" onkeyup="validate(this, '^.{2,200}$', 'emptynotok')" /></td>
                                </tr>							
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td><stripes:label for="organisation.name"/>:</td>
                                <td><stripes:text  id="organisation.organisationName" name="organisation.organisationName"   maxlength="200" size="50" onkeyup="validate(this, '^.{2,200}$', 'emptynotok')" /></td>
                                </tr>
                            </c:otherwise>
                        </c:choose>

                        <!--        <tr>
                                    <td><stripes:label for="companynumber"/>:</td>
                                <td>
                                <c:choose>
                                    <c:when test="${s:getProperty('env.organization.companynumber.mandatory')}">
                                        <stripes:text name="organisation.companyNumber"  maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptynotok')" />
                                    </c:when>
                                    <c:otherwise>
                                        <stripes:text name="organisation.companyNumber"  maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptyok')" />
                                    </c:otherwise>
                                </c:choose>
                                </td>
                                </tr>-->

                        <c:choose>
                            <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
                                <tr id="companynumber">
                                    <td><stripes:label for="companynumber"/>:</td>
                                <td>
                                <c:choose>
                                    <c:when test="${s:getProperty('env.organization.companynumber.mandatory')}">
                                        <stripes:text name="organisation.companyNumber"  maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptyok')" />
                                    </c:when>
                                    <c:otherwise>
                                        <stripes:text name="organisation.companyNumber"  maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptyok')" />
                                    </c:otherwise>
                                </c:choose>
                                </td>
                                </tr>						
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td><stripes:label for="companynumber"/>:</td>
                                <td>
                                <c:choose>
                                    <c:when test="${s:getProperty('env.organization.companynumber.mandatory')}">
                                        <stripes:text name="organisation.companyNumber"  maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptyok')" />
                                    </c:when>
                                    <c:otherwise>
                                        <stripes:text name="organisation.companyNumber"  maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptyok')" />
                                    </c:otherwise>
                                </c:choose>
                                </td>
                                </tr>
                            </c:otherwise>
                        </c:choose>

                        <!--        <tr>
                                    <td><stripes:label for="taxnumber"/>:</td>
                                <td>
                                <c:choose>
                                    <c:when test="${s:getProperty('env.organization.taxnumber.mandatory')}">
                                        <stripes:text name="organisation.taxNumber"  maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptynotok')" />
                                    </c:when>
                                    <c:otherwise>
                                        <stripes:text name="organisation.taxNumber"  maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptyok')" />
                                    </c:otherwise>
                                </c:choose>
                                </td>
                                </tr>-->

                        <c:choose>
                            <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
                                <tr id="taxnumber">
                                    <td><stripes:label for="taxnumber"/>:</td>
                                <td>
                                <c:choose>
                                    <c:when test="${s:getProperty('env.organization.taxnumber.mandatory')}">
                                        <stripes:text name="organisation.taxNumber"  maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptyok')" />
                                    </c:when>
                                    <c:otherwise>
                                        <stripes:text name="organisation.taxNumber"  maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptyok')" />
                                    </c:otherwise>
                                </c:choose>
                                </td>
                                </tr>		
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td><stripes:label for="taxnumber"/>:</td>
                                <td>
                                <c:choose>
                                    <c:when test="${s:getProperty('env.organization.taxnumber.mandatory')}">
                                        <stripes:text name="organisation.taxNumber"  maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptyok')" />
                                    </c:when>
                                    <c:otherwise>
                                        <stripes:text name="organisation.taxNumber"  maxlength="50" size="20" onkeyup="validate(this, '^.{5,50}$', 'emptyok')" />
                                    </c:otherwise>
                                </c:choose>
                                </td>
                                </tr>
                            </c:otherwise>
                        </c:choose>

                        <!--        <tr>
                                    <td><stripes:label for="email"/>:</td>
                                <td><stripes:text name="organisation.emailAddress" maxlength="200" size="30" onkeyup="validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,4}$/, 'emptynotok')" /></td>
                                </tr> -->
                        <c:choose>
                            <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
                                <tr id="email">
                                    <td><stripes:label for="email"/>:</td>
                                <td><stripes:text name="organisation.emailAddress" maxlength="200" size="30" onkeyup="validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,4}$/, 'emptyok')" /></td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td><stripes:label for="email"/>:</td>
                                <td><stripes:text name="organisation.emailAddress" maxlength="200" size="30" onkeyup="validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,4}$/, 'emptynotok')" /></td>
                                </tr>
                            </c:otherwise>
                        </c:choose>

                        <!--        <tr>
                                    <td><stripes:label for="alternative.contact.number.1" />:</td>
                                <td><stripes:text name="organisation.alternativeContact1" maxlength="15" size='15' onkeyup="validate(this, '^[+()0-9 -]{10,15}$', 'emptynotok')" /></td>
                                </tr>-->
                        <c:choose>
                            <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
                                <tr id="contactnumber1">
                                    <td><stripes:label for="alternative.contact.number.1" />:</td>
                                <td><stripes:text name="organisation.alternativeContact1" maxlength="15" size='15' onkeyup="validate(this, '^[+()0-9 -]{10,15}$', 'emptyok')" /></td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td><stripes:label for="alternative.contact.number.1" />:</td>
                                <td><stripes:text name="organisation.alternativeContact1" maxlength="15" size='15' onkeyup="validate(this, '^[+()0-9 -]{10,15}$', 'emptynotok')" /></td>
                                </tr>
                            </c:otherwise>
                        </c:choose>


                        <!--        <tr>
                                    <td><stripes:label for="alternative.contact.number.2"/>:</td>
                                <td><stripes:text name="organisation.alternativeContact2" maxlength="15" size="15" onkeyup="validate(this, '^[+()0-9 -]{5,15}$', 'emptyok')" /></td>
                                </tr>-->
                        <c:choose>
                            <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
                                <tr id="contactnumber2">
                                    <td><stripes:label for="alternative.contact.number.2"/>:</td>
                                <td><stripes:text name="organisation.alternativeContact2" maxlength="15" size="15" onkeyup="validate(this, '^[+()0-9 -]{5,15}$', 'emptyok')" /></td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td><stripes:label for="alternative.contact.number.2"/>:</td>
                                <td><stripes:text name="organisation.alternativeContact2" maxlength="15" size="15" onkeyup="validate(this, '^[+()0-9 -]{5,15}$', 'emptyok')" /></td>
                                </tr>
                            </c:otherwise>
                        </c:choose>

                        <!--        <tr>
                                    <td><stripes:label for="industry"/>:</td>
                                <td>
                                <stripes:select name="organisation.industry" onchange="validate(this, '^[a-zA-Z]{1,50}$', 'emptynotok')">
                                    <stripes:option value=""></stripes:option>
                                    <c:forEach items="${s:getPropertyAsList('env.organisation.industries')}" var="industry" varStatus="loop">
                                        <stripes:option value="${industry}">
                                            <fmt:message   key="industry.${industry}" />
                                        </stripes:option>
                                    </c:forEach>
                                </stripes:select>
                                </td>
                                </tr>-->
                        <c:choose>
                            <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
                                <tr id="industry">
                                    <td><stripes:label for="industry"/>:</td>
                                <td>
                                <stripes:select name="organisation.industry" onchange="validate(this, '^[a-zA-Z]{1,50}$', 'emptyok')">
                                    <stripes:option value=""></stripes:option>
                                    <c:forEach items="${s:getPropertyAsList('env.organisation.industries')}" var="industry" varStatus="loop">
                                        <stripes:option value="${industry}">
                                            <fmt:message   key="industry.${industry}" />
                                        </stripes:option>
                                    </c:forEach>
                                </stripes:select>
                                </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td><stripes:label for="industry"/>:</td>
                                <td>
                                <stripes:select name="organisation.industry" onchange="validate(this, '^[a-zA-Z]{1,50}$', 'emptynotok')">
                                    <stripes:option value=""></stripes:option>
                                    <c:forEach items="${s:getPropertyAsList('env.organisation.industries')}" var="industry" varStatus="loop">
                                        <stripes:option value="${industry}">
                                            <fmt:message   key="industry.${industry}" />
                                        </stripes:option>
                                    </c:forEach>
                                </stripes:select>
                                </td>
                                </tr>
                            </c:otherwise>
                        </c:choose>

                        <!--        <tr>
                                    <td><stripes:label for="organisation.size"/>:</td>
                                <td>
                                <stripes:select name="organisation.size">
                                    <c:forEach items="${s:getPropertyAsList('env.organisation.size')}" var="size">
                                        <stripes:option value="${size}">
                        ${size}
                    </stripes:option>   
                </c:forEach>
            </stripes:select>
            </td>
            </tr>-->
                        <c:choose>
                            <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
                                <tr id="organisationsize">
                                    <td><stripes:label for="organisation.size"/>:</td>
                                <td>
                                <stripes:select name="organisation.size">
                                    <c:forEach items="${s:getPropertyAsList('env.organisation.size')}" var="size">
                                        <stripes:option value="${size}">
                                            ${size}
                                        </stripes:option>   
                                    </c:forEach>
                                </stripes:select>
                                </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td><stripes:label for="organisation.size"/>:</td>
                                <td>
                                <stripes:select name="organisation.size">
                                    <c:forEach items="${s:getPropertyAsList('env.organisation.size')}" var="size">
                                        <stripes:option value="${size}">
                                            ${size}
                                        </stripes:option>   
                                    </c:forEach>
                                </stripes:select>
                                </td>
                                </tr>
                            </c:otherwise>
                        </c:choose>

                    </table>
                </div>

                <span class="button">
                    <stripes:submit name="captureOrganisationScannedDocuments" onclick="onBack = false;onNext = true;" />
                </span>


                <c:forEach items="${actionBean.organisation.organisationPhotographs}" varStatus="loop"> 
                    <stripes:hidden class="file" id="file${loop.index}" name="file${loop.index}"/>
                    <stripes:hidden id="photoGuid${loop.index}" name="organisation.organisationPhotographs[${loop.index}].photoGuid" value="${actionBean.organisation.organisationPhotographs[loop.index].photoGuid}"/>
                    <stripes:hidden id="photoType${loop.index}" name="organisation.organisationPhotographs[${loop.index}].photoType" value="${actionBean.organisation.organisationPhotographs[loop.index].photoType}"/>
                </c:forEach>

                <c:forEach items="${actionBean.organisation.addresses}" varStatus="loop">     

                    <stripes:hidden name="organisation.addresses[${loop.index}].type" value="${actionBean.organisation.addresses[loop.index].type}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].line1" value="${actionBean.organisation.addresses[loop.index].line1}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].line2" value="${actionBean.organisation.addresses[loop.index].line2}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].state"    value="${actionBean.organisation.addresses[loop.index].state}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].town" value="${actionBean.organisation.addresses[loop.index].town}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].country" value="${actionBean.organisation.addresses[loop.index].country}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].code" value="${actionBean.organisation.addresses[loop.index].code}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].zone" value="${actionBean.organisation.addresses[loop.index].zone}" />
                </c:forEach>
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>