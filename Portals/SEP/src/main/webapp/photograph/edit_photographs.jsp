<%-- 
    Document   : edit_photographs
    Created on : 05 Aug 2013, 10:31:04 AM
    Author     : lesley
--%>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="edit.photographs"/>
</c:set>
<c:set var="customerClassType" value="${s:getDelimitedPropertyValueMapping('env.customer.classifications', actionBean.customer.classification)}"/>

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

        <div id="entity">
            <table class="entity_header">
                <tr>
                    <c:choose>
                        <c:when test="${actionBean.customer != null}">
                            <td>
                                <fmt:message key="customer"/>:  ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                            </td>
                            <td align="right">                       
                                <stripes:form action="/Customer.action" method="get">                                
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
                                <stripes:form action="/Customer.action" method="get">
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



            <stripes:form action="/Customer.action" name="frm" onsubmit="clearErrorMessages(); return (alertValidationErrors() && checkRequiredDocuments());">
                <c:choose>
                    <c:when test="${actionBean.customer != null}">
                        <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                    </c:when>
                    <c:when test="${actionBean.organisation != null}">
                        <stripes:hidden name="organisation.organisationId" value="${actionBean.organisation.organisationId}"/>                        
                    </c:when>
                </c:choose>
                <stripes:select name="customer.document.types"  id="customer.document.types" style="display:none">
                    <stripes:option value=""></stripes:option>
                    <c:forEach items="${s:getPropertyAsList(documentTypesResourceName)}" var="documentType" varStatus="loop">
                        <stripes:option value="${documentType}">
                            <fmt:message  key="document.type.${documentType}"/>
                        </stripes:option>
                    </c:forEach>
                </stripes:select>
                <table>
                    <tr>
                        <td colspan="2">
                            <br/>
                            <b><fmt:message key="scanned.documents"/>:</b>
                            <br/>
                        </td>
                    </tr>
                    <tr>
                        <td  colspan="2">
                            <table class ="clear" width="100%">
                                <tr>
                                    <td>
                                        <fmt:message key="${missingDocumentTypesErrorMessageResource}"/>
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
                                    <c:forEach items="${actionBean.photographs}" varStatus="loop"> 
                                        <tr id="row${loop.index}">
                                            <td align="left" valign="top">
                                                <stripes:select id="photoType${loop.index}" name="photographs[${loop.index}].photoType" onchange="validate(this,'^[a-zA-Z]{1,50}$','emptynotok')">
                                                    <c:choose>
                                                        <c:when test='${actionBean.photographs[loop.index].photoType.matches("^.*fingerprint.*$")}'>
                                                            <stripes:option value="${actionBean.photographs[loop.index].photoType}" selected="selected">
                                                                <fmt:message  key="document.type.${actionBean.photographs[loop.index].photoType}"/>
                                                            </stripes:option>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <stripes:option value=""></stripes:option>
                                                            <c:forEach items="${s:getPropertyAsList(documentTypesResourceName)}" var="documentType" varStatus="loop1">
                                                                <c:choose>
                                                                    <c:when test='${actionBean.photographs[loop.index].photoType == documentType}'>
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
                                                        </c:otherwise>
                                                    </c:choose>    
                                                </stripes:select>
                                            </td>
                                            <c:choose>
                                                <c:when test='${actionBean.photographs[loop.index].photoType.matches("^.*fingerprint.*$")}'>
                                            <input type="hidden" id="photoGuid${loop.index}" name="photographs[${loop.index}].photoGuid" value="${actionBean.photographs[loop.index].photoGuid}"/>
                                            <td align="left" valign="top">
                                                <div id="imgDiv${loop.index}">
                                                    <a href="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg" target="_blank">
                                                        <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg"/>
                                                    </a>
                                                </div>
                                            </td>
                                            <td align="left" valign="top">
                                                <input type = 'button' class='file' value = 'Browse ...' onclick ="javascript:document.getElementById('file${loop.index}').click();" />
                                                <input value="Remove" class="file" onclick="removeDocument(this, 'photographs');" type="button"/>
                                                <input class="file" id="file${loop.index}" name="file${loop.index}" type="file" style="visibility: hidden;" />
                                                <script type="text/javascript">
                                                    document.getElementById('file${loop.index}').addEventListener('change', uploadDocument, false);
                                                </script>
                                            </td>

                                        </c:when>
                                        <c:when test='${actionBean.photographs[loop.index].photoGuid.matches("^.*.xlsx$") || actionBean.photographs[loop.index].photoGuid.matches("^.*.xls$")}'>
                                            <input type="hidden" id="photoGuid${loop.index}" name="photographs[${loop.index}].photoGuid" value="${actionBean.photographs[loop.index].photoGuid}"/>
                                            <td align="left" valign="top">
                                                <div id="imgDiv${loop.index}">
                                                    <a href="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[loop.index].photoGuid}" target="_blank">
                                                        <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/images/excel_file_image.png"/>
                                                    </a>
                                                </div>
                                            </td>
                                            <td align="left" valign="top">
                                                <input type = 'button' class='file' value = 'Browse ...' onclick ="javascript:document.getElementById('file${loop.index}').click();" />
                                                <input value="Remove" class="file" onclick="removeDocument(this, 'photographs');" type="button"/>
                                                <input class="file" id="file${loop.index}" name="file${loop.index}" type="file" style="visibility: hidden;" />
                                                <script type="text/javascript">
                                                    document.getElementById('file${loop.index}').addEventListener('change', uploadDocument, false);
                                                </script>
                                            </td>

                                        </c:when>
                                        <c:otherwise>
                                            <input type="hidden" id="photoGuid${loop.index}" name="photographs[${loop.index}].photoGuid" value="${actionBean.photographs[loop.index].photoGuid}"/>
                                            <td align="left" valign="top">
                                                <div id="imgDiv${loop.index}">
                                                    <a href="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[loop.index].photoGuid}" target="_blank">
                                                        <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[loop.index].photoGuid}"/>
                                                    </a>
                                                </div>
                                            </td>
                                            <td align="left" valign="top">
                                                <input type = 'button' class='file' value = 'Browse ...' onclick ="javascript:document.getElementById('file${loop.index}').click();" />
                                                <input value="WebCam" class="file" id="snap" type="button" onclick="console.log('here 0');
                                                        snapAndUpload('file${loop.index}');
                                                        console.log('here 0');"/>
                                                <input value="Remove" class="file" onclick="removeDocument(this, 'photographs');" type="button"/>
                                                <input class="file" id="file${loop.index}" name="file${loop.index}" type="file" style="visibility: hidden;" />
                                                <script type="text/javascript">
                                                    document.getElementById('file${loop.index}').addEventListener('change', uploadDocument, false);
                                                </script>
                                            </td>
                                        </c:otherwise>
                                    </c:choose>
                        </tr>
                    </c:forEach>
                    </tbody>           

                </table>                                
            </td>
        </tr>
        <tr>
            <td colspan="2">
                <br/>
                <input onclick="javascript:addRow('tblDocuments', 'photographs')" type="button" value="Add Document"/> 
                <br/>
                <label  id="lblErrorMessages"></label> 
                <br/>
            </td>
        </tr>
        <tr>                        
            <td colspan="2"><stripes:submit name="updatePhotographs"/></td>
        </tr>
    </table>
</stripes:form>

</div>
</stripes:layout-component>
</stripes:layout-render>
