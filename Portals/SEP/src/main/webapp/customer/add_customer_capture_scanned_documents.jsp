<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.new.customer.scanned.documents"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">
            var nationality = "${actionBean.customer.nationality}";
            var checkRequiredDocumentsRegEx = "";
            var missingDocumentTypesErrorMessageResource = "";

            <c:set var="customerClassType" value="${s:getDelimitedPropertyValueMapping('env.customer.classifications', actionBean.customer.classification)}"/>
            <c:choose>
                <c:when test='${customerClassType == "organisation"}'>
                    <c:set var="documentTypesResourceName" value="env.customer.organisation.document.types"/>
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.documents.regex', actionBean.context.request)}";
                    <c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.document.type.missing.error"/>
                </c:when>
                <c:when test='${customerClassType == "company"}'>
                    <c:set var="documentTypesResourceName" value="env.customer.company.document.types"/>
                    <c:set var="missingDocumentTypesErrorMessageResource" value="company.document.type.missing.error"/>
                </c:when>
                <c:when test='${customerClassType == "diplomat"}'>
                    <c:set var="documentTypesResourceName" value="env.customer.diplomat.document.types"/>
                    <c:set var="missingDocumentTypesErrorMessageResource" value="diplomat.document.type.missing.error"/>
                </c:when>
                <c:when test='${actionBean.customer.identityNumberType == "refugeeid"}'>
                    checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.refugee.required.documents.regex', actionBean.context.request)}";
                    <c:set var="documentTypesResourceName" value="env.customer.refugee.document.types"/>
                    <c:set var="missingDocumentTypesErrorMessageResource" value="refugee.document.type.missing.error"/>
                </c:when>
                <c:otherwise>
                    <c:choose>
                        <c:when test='${customerClassType != "minor"}'>
                            <c:set var="documentTypesResourceName" value="env.customer.personal.document.types"/>
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.personal.required.documents.regex', actionBean.context.request)}";
                            <c:choose>
                                <c:when test="${actionBean.customer.nationality != s:getProperty('env.locale.country.for.language.en')}">
                                    <c:choose>
                                        <c:when test="${s:getPropertyWithDefault('env.customer.foreigner.documents.use.multiple.types','false')}">
                                            <c:set var="missingDocumentTypesErrorMessageResource" value="foreign.personal.document.type.missing.error"/>
                                            <c:set var="documentTypesResourceName" value="env.customer.personal.foreigner.document.types"/>
                                        </c:when>
                                        <c:otherwise>
                                            <c:set var="missingDocumentTypesErrorMessageResource" value="personal.document.type.missing.error"/>
                                        </c:otherwise>
                                    </c:choose>
                                </c:when>
                                <c:otherwise>
                                    <c:choose>
                                        <c:when test='${customerClassType == "company"}'>
                                            <c:set var="missingDocumentTypesErrorMessageResource" value="company.document.type.missing.error"/>
                                        </c:when>
                                        <c:otherwise>
                                            <c:set var="missingDocumentTypesErrorMessageResource" value="personal.document.type.missing.error"/>                                
                                        </c:otherwise>
                                    </c:choose>

                                </c:otherwise>
                            </c:choose>
                        </c:when>
                        <c:otherwise>
                            <c:set var="documentTypesResourceName" value="env.customer.minor.document.types"/>
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.minor.required.documents.regex', actionBean.context.request)}";
                            <c:set var="missingDocumentTypesErrorMessageResource" value="minor.document.type.missing.error"/>
                        </c:otherwise>
                    </c:choose>
                </c:otherwise>
            </c:choose>

            // - The purpose of the following code is to ensure that the document that was specified as the method of identification in the previous page is scanned into the system
            // - if is is not specified as a required document already.
            var methodOfIdentification = "";
            var methodOfIdentificationDescr = "";
            <c:if test="${actionBean.customer.KYCStatus !=  'V'}">
            methodOfIdentification = "${actionBean.customer.identityNumberType}";
            methodOfIdentificationDescr = "<fmt:message  key='document.type.${actionBean.customer.identityNumberType}'/>";
            </c:if>

            <c:if test="${not empty actionBean.customer.visaExpiryDate}">
            var visaExpiryDate = "${actionBean.customer.visaExpiryDate}";
            </c:if>


            window.addEventListener("DOMContentLoaded", function () {
                console.log("init start");
                initWebCam();
                console.log("init end");
            }, false);

        </script>

        <div id="entity">

            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="customer"/>: <fmt:message   key="classification.${actionBean.customer.classification}" /> - ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                    </td>                        
                </tr>
            </table>   

            <table class="clear">
                <tr class="red" align="center">
                    <td align="center">
                        <b>NB: Facial Photographs must be clear with no head tilt. See the example of a good photograph below. Failure to capture proper photographs will require you to revisit the customer and take new photographs.<b/>
                    </td>
                </tr>
                <tr class="red" align="center">
                    <td align="center">
                        <img src="images/photo_example.jpg" width="200" height="200"/>
                    </td>
                </tr>
            </table>


            <stripes:form  action="/Customer.action" focus="" id="frmUploadDocuments" autocomplete="off" enctype="mulipart/form-data" method="POST" onsubmit="clearErrorMessages(); return ((onBack == true) ? true : (alertValidationErrors() && checkRequiredDocuments()));">    
                <stripes:hidden name="customer.warehouseId" value="${actionBean.customer.warehouseId}" />
                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                <stripes:hidden name="customer.version"    value="${actionBean.customer.version}"/>
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
                <stripes:hidden name="customer.customerStatus" value="${actionBean.customer.customerStatus}"/>
                <stripes:hidden name="customer.classification"  value="${actionBean.customer.classification}"/>
                <stripes:hidden name="customer.nationality"  value="${actionBean.customer.nationality}"/>
                <stripes:hidden name="customer.securityGroups[0]" value="${actionBean.customer.securityGroups[0]}"/>
                <stripes:hidden name="customer.passportExpiryDate" value="${actionBean.customer.passportExpiryDate}"/>
                <stripes:hidden name="customer.visaExpiryDate" value="${actionBean.customer.visaExpiryDate}"/>
                <stripes:hidden name="TTIssue.ID" value="${actionBean.TTIssue.ID}"/>
                <stripes:hidden name="customer.KYCStatus" value="${actionBean.customer.KYCStatus}"/>

                <c:forEach items="${actionBean.customer.addresses}" varStatus="loop">      
                    <stripes:hidden name="customer.addresses[${loop.index}].type" value="${actionBean.customer.addresses[loop.index].type}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].line1" value="${actionBean.customer.addresses[loop.index].line1}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].line2" value="${actionBean.customer.addresses[loop.index].line2}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].town" value="${actionBean.customer.addresses[loop.index].town}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].country" value="${actionBean.customer.addresses[loop.index].country}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].code" value="${actionBean.customer.addresses[loop.index].code}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].zone" value="${actionBean.customer.addresses[loop.index].zone}" />
                    <stripes:hidden name="customer.addresses[${loop.index}].state" value="${actionBean.customer.addresses[loop.index].state}" />
                </c:forEach>

                <stripes:select name="customer.document.types"  id="customer.document.types" style="display:none">
                    <stripes:option value=""></stripes:option>
                    <c:forEach items="${s:getPropertyAsList(documentTypesResourceName)}" var="documentType" varStatus="loop">
                        <stripes:option value="${documentType}">
                            <fmt:message  key="document.type.${documentType}"/>
                        </stripes:option>
                    </c:forEach>
                </stripes:select>

                <table class ="clear" width="100%">
                    <tr>
                        <td>
                            <fmt:message   key='${missingDocumentTypesErrorMessageResource}'/>
                        </td>
                    </tr>
                </table>
                <table class ="clear" width="100%">
                    <tr>
                        <td style="text-align: center">
                            <video class="video" id="video" width="320" height="240" style="-moz-transform: scale(-1, 1); -webkit-transform: scale(-1, 1); -o-transform: scale(-1, 1); transform: scale(-1, 1); filter: FlipH;" autoplay></video>
                            <canvas id="canvas" width="1024" height="768" style="display:none"></canvas>
                        </td>
                    </tr>
                </table>
                <table class="clear"  width="100%" id="tblDocuments">
                    <tbody>
                        <c:forEach items="${actionBean.customer.customerPhotographs}" varStatus="loop"> 
                            <tr id="row${loop.index}">
                                <td align="left" valign="top">
                                    <stripes:select id="photoType${loop.index}" name="customer.customerPhotographs[${loop.index}].photoType" onchange="validate(this,'^[a-zA-Z]{1,50}$','emptynotok')">
                                        <stripes:option value=""></stripes:option>
                                        <c:forEach items="${s:getPropertyAsList(documentTypesResourceName)}" var="documentType" varStatus="loop1">
                                            <c:choose>
                                                <c:when test='${actionBean.customer.customerPhotographs[loop.index].photoType == documentType}'>
                                                    <stripes:option value="${documentType}" selected="selected">
                                                        <fmt:message  key="document.type.${documentType}"/>
                                                    </stripes:option>
                                                </c:when>
                                                <c:otherwise>
                                                    <stripes:option value="${documentType}">
                                                        <fmt:message  key="document.type.${documentType}"/>
                                                    </stripes:option>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:forEach>
                                    </stripes:select>
                                </td>
                                <td align="left" valign="top">
                                    <div id="imgDiv${loop.index}">
                                        <c:choose>
                                            <c:when test='${actionBean.customer.customerPhotographs[loop.index].photoType.matches("^.*fingerprint.*$")}'>
                                                <a href="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg" target="_blank">
                                                    <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg"/>
                                                </a>
                                            </c:when>
                                            <c:otherwise>
                                                <a href="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.customer.customerPhotographs[loop.index].photoGuid}" target="_blank">
                                                    <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.customer.customerPhotographs[loop.index].photoGuid}"/>
                                                </a>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </td>
                                <td align="left" valign="top">
                                    <input type = 'button' class='file' value = 'Browse ...' onclick ="javascript:document.getElementById('file${loop.index}').click();" />
                                    <input value="WebCam" class="file" id="snap" type="button" onclick="snapAndUpload('file${loop.index}');"/>
                                    <input value="Remove" class="file" onclick="removeDocument(this, 'customer.customerPhotographs');" type="button"/>
                                    <input type="hidden"  id="photoGuid${loop.index}" name="customer.customerPhotographs[${loop.index}].photoGuid" value="${actionBean.customer.customerPhotographs[loop.index].photoGuid}"/>
                                    <input type="file" class="file" id="file${loop.index}" name="file${loop.index}" style="visibility: hidden;" />
                                    <script type="text/javascript">
                                        document.getElementById('file${loop.index}').addEventListener('change', uploadDocument, false);
                                    </script>
                                </td>
                            </tr>
                        </c:forEach>

                    </tbody>
                </table>
                <br />
                <input onclick="addRow('tblDocuments', 'customer.customerPhotographs');" type="button" value="Add Document"/> 
                <br />
                <table class="clear" width="100%">
                    <tr>
                        <td id="lblErrorMessages"> </td>
                    </tr>
                </table>
                <br />
                <span class="button">
                    <stripes:submit name="showAddCustomerBackIntoBasicDetails" onclick="onBack=true;"/>
                </span>
                <span class="button">
                    <stripes:submit name="showAddCustomerManageCustomerAddresses" onclick="onBack=false;"/>
                </span>

            </stripes:form>

        </div>		

    </stripes:layout-component>
</stripes:layout-render>