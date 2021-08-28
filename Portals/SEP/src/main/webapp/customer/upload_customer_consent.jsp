<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="upload.customer.consent"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <input type="hidden" id="refreshed" value="no">
        <script type="text/javascript">
            onload = function () {
                var e = document.getElementById("refreshed");
                if (e.value == "no") {
                    e.value = "yes";
                } else {
                    e.value = "no";
                    document.location = document.location;
                }
            }
       
        </script>

        <script type="text/javascript">

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
                    <c:set var="documentTypesResourceName" value="env.customer.organisation.document.types"/>
                var nationality = "";    
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.documents.regex', actionBean.context.request)}";
                    <c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.document.type.missing.error"/>;
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

            <stripes:form action="/Customer.action" name="frm" onsubmit="clearErrorMessages(); return checkConsentValid();">
                <table class="clear" width="100%">
                    <tr>
                        <td id="lblErrorMessages"> </td>
                    </tr>
                </table><br/>
                <table class="clear"  width="100%" id="tblDocuments">
                    <tbody>
                    <c:if test="${!actionBean.noConsent}" >
                        <tr>
                            <td align="left" valign="top">
                                    <input type = 'button' class='sep-button' value = 'Attach Consent Form' onclick ="javascript:document.getElementById('file0').click();" />
                                    <input class="file" id="file0" name="file0" type="file" style="visibility: hidden;" />
                                    <script type="text/javascript">
                                        document.getElementById('file0').addEventListener('change', uploadDocument, false);
                                    </script>
                                </td>
                        </tr>
                            <tr id="row0">                                
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
                        <tr>
                            <td>             
                                    <select name="rsm" id="rsm">
                                        <option value="-1">Please Select Your Manager</option>

                                        <c:forEach items="${actionBean.retailSalesManagers}" varStatus="loop"> 

                                            <option value="${actionBean.retailSalesManagers[loop.index].customerId}">${actionBean.retailSalesManagers[loop.index].firstName} ${actionBean.retailSalesManagers[loop.index].lastName}</option>
                                        </c:forEach>
                                    </select>
                                
                            </td>
                        </tr>
                        <tr>
                            <input type="hidden" name="customer.nationalIdentityNumber"  id="customer.nationalIdentityNumber" value="${actionBean.customer.nationalIdentityNumber}" size="20" maxlength="20" onkeyup="validate(this,'^[0-9]{10,20}$','')"/>                                                           
                            <td colspan="2"><stripes:submit name="doConsentFormUpload" class="sep-button"/></td>
                        </tr>
                    </tbody>           
                </table>
</stripes:form>

</stripes:layout-component>
</stripes:layout-render>
