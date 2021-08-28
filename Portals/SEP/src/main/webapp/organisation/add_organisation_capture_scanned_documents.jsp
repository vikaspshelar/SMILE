<%@ include file="/include/sep_include.jsp" %>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.new.organisation.scanned.documents"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">

            var nationality = "";//Not sure what value to use here ...
            var checkRequiredDocumentsRegEx = "";
            var missingDocumentTypesErrorMessageResource = "";
            var methodOfIdentification = "";
            
            window.addEventListener("DOMContentLoaded", function () {
                initWebCam();
            }, false);

<c:set var="customerClassType" value="${s:getDelimitedPropertyValueMapping('env.customer.classifications', actionBean.customer.classification)}"/>
<c:choose>
    <c:when test="${s:getPropertyWithDefault('env.organisation.type.enable','false') == 'true'}">							
	<c:choose>
			<c:when test="${actionBean.organisation.organisationType == 'company'}">							
				<c:set var="documentTypesResourceName" value="env.customer.organisation.company.document.types"/>
                                checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.company.documents.regex', actionBean.context.request)}";
				<c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.company.document.type.missing.error"/>						
			</c:when>
			<c:when test="${actionBean.organisation.organisationType == 'government'}">							
				<c:set var="documentTypesResourceName" value="env.customer.organisation.government.document.types"/>
				checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.government.documents.regex', actionBean.context.request)}";
				<c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.government.document.type.missing.error"/>						
			</c:when>
			<c:when test="${actionBean.organisation.organisationType == 'ngo'}">							
				<c:set var="documentTypesResourceName" value="env.customer.organisation.ngo.document.types"/>
				checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.ngo.documents.regex', actionBean.context.request)}";
				<c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.ngo.document.type.missing.error"/>						
			</c:when>
			<c:when test="${actionBean.organisation.organisationType == 'trust'}">							
				<c:set var="documentTypesResourceName" value="env.customer.organisation.trust.document.types"/>
				checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.trust.documents.regex', actionBean.context.request)}";
				<c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.trust.document.type.missing.error"/>						
			</c:when>
			<c:when test="${actionBean.organisation.organisationType == 'foreignmission'}">							
				<c:set var="documentTypesResourceName" value="env.customer.organisation.foreignmission.document.types"/>
				checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.foreignmission.documents.regex', actionBean.context.request)}";
				<c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.foreignmission.document.type.missing.error"/>						
			</c:when>
                         <c:when test="${actionBean.organisation.organisationSubType == 'companyindirectpartner'}">							
				<c:set var="documentTypesResourceName" value="env.customer.organisation.companyindirectpartner.document.types"/>
				checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.companyindirectpartner.documents.regex', actionBean.context.request)}";
				<c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.companyindirectpartner.document.type.missing.error"/>						
			</c:when>
		</c:choose>										
    </c:when>
    <c:otherwise>
            <c:set var="documentTypesResourceName" value="env.customer.organisation.document.types"/>
            checkRequiredDocumentsRegEx = "${s:getRoleBasedProperty('env.customer.organisation.required.documents.regex', actionBean.context.request)}";
            <c:set var="missingDocumentTypesErrorMessageResource"  value= "organisation.document.type.missing.error"/>
    </c:otherwise>
</c:choose>

            // - The purpose of the following code is to ensure that the document that was specified as the method of identification in the previous page is scanned into the system
            // - if is is not specified as a required document already.
</script>

        <div id="entity">

            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="organisation"/>: ${actionBean.organisation.organisationName}
                    </td>                        
                </tr>
            </table>   

            <stripes:form  action="/Customer.action" focus="" id="frmUploadDocuments" autocomplete="off" enctype="mulipart/form-data" method="POST" onsubmit="clearErrorMessages(); return ((onBack == true) ? true : (alertValidationErrors() && checkRequiredDocuments()));">    

                <stripes:hidden name="organisation.organisationType" value="${actionBean.organisation.organisationType}"/>
                <stripes:hidden name="organisation.organisationSubType" value="${actionBean.organisation.organisationSubType}"/>
                <stripes:hidden name="organisation.organisationName"    value="${actionBean.organisation.version}"/>
                <stripes:hidden name="organisation.companyNumber" value="${actionBean.organisation.companyNumber}"/>
                <stripes:hidden name="organisation.taxNumber" value="${actionBean.organisation.taxNumber}"/>
                <stripes:hidden name="organisation.emailAddress" value="${actionBean.organisation.emailAddress}"/>
                <stripes:hidden name="organisation.alternativeContact1" value="${actionBean.organisation.alternativeContact1}"/>
                <stripes:hidden name="organisation.alternativeContact2" value="${actionBean.organisation.alternativeContact2}"/>
                <stripes:hidden name="organisation.industry" value="${actionBean.organisation.industry}"/>
                <stripes:hidden name="organisation.size" value="${actionBean.organisation.size}" />

                <c:forEach items="${actionBean.organisation.addresses}" varStatus="loop">      
                    <stripes:hidden name="organisation.addresses[${loop.index}].type" value="${actionBean.organisation.addresses[loop.index].type}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].line1" value="${actionBean.organisation.addresses[loop.index].line1}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].line2" value="${actionBean.organisation.addresses[loop.index].line2}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].town" value="${actionBean.organisation.addresses[loop.index].town}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].country" value="${actionBean.organisation.addresses[loop.index].country}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].code" value="${actionBean.organisation.addresses[loop.index].code}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].zone" value="${actionBean.organisation.addresses[loop.index].zone}" />
                    <stripes:hidden name="organisation.addresses[${loop.index}].state" value="${actionBean.organisation.addresses[loop.index].state}" />
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
                            <video class="video" id="video" width="320" height="240" autoplay></video>
                            <canvas id="canvas" width="1024" height="768" style="display:none"></canvas>
                        </td>
                    </tr>
                </table>
                <table class="clear"  width="100%" id="tblDocuments">
                    <tbody>
                        <c:forEach items="${actionBean.organisation.organisationPhotographs}" varStatus="loop"> 
                            <tr id="row${loop.index}">
                                <td align="left" valign="top">
                                    <div id="imgDiv${loop.index}">
                                        <stripes:select id="photoType${loop.index}" name="organisation.organisationPhotographs[${loop.index}].photoType" onchange="validate(this,'^[a-zA-Z]{1,50}$','emptynotok')">
                                            <stripes:option value=""></stripes:option>
                                            <c:forEach items="${s:getPropertyAsList(documentTypesResourceName)}" var="documentType" varStatus="loop1">
                                                <c:choose>
                                                    <c:when test='${actionBean.organisation.organisationPhotographs[loop.index].photoType == documentType}'>
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
                                    </div>
                                </td>

                                <td align="left" valign="top">
                                    <a href="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.organisation.organisationPhotographs[loop.index].photoGuid}" target="_blank">
                                        <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.organisation.organisationPhotographs[loop.index].photoGuid}"/>
                                    </a>
                                </td>

                                <td align="left" valign="top">
                                    <input type = 'button' class='file' value = 'Browse ...' onclick ="javascript:document.getElementById('file${loop.index}').click();" />
                                    <input value="WebCam" class="file" id="snap" type="button" onclick="snapAndUpload('file${loop.index}')"/>
                                    <input value="Remove" class="file" onclick="removeDocument(this, 'organisation.organisationPhotographs');" type="button"/>
                                    <input type="hidden"  id="photoGuid${loop.index}" name="organisation.organisationPhotographs[${loop.index}].photoGuid" value="${actionBean.organisation.organisationPhotographs[loop.index].photoGuid}"/>
                                    <input class="file" id="file${loop.index}" name="file${loop.index}" type="file" style="visibility: hidden;" />
                                    <script type="text/javascript">
                                        document.getElementById('file${loop.index}').addEventListener('change', uploadDocument, false);
                                    </script>
                                </td>
                            </tr>
                        </c:forEach>

                    </tbody>
                </table>
                <br />
                <input onclick="addRow('tblDocuments', 'organisation.organisationPhotographs');" type="button" value="Add Document"/> 
                <br />
                <table class="clear" width="100%">
                    <tr>
                        <td id="lblErrorMessages"> </td>
                    </tr>
                </table>
                <br />
                <span class="button">
                    <stripes:submit name="showAddOrganisationBackIntoBasicDetails" onclick="onBack=true;"/>
                </span>
                <span class="button">
                    <stripes:submit name="showAddOrganisationManageOrganisationAddresses" onclick="onBack=false;"/>
                </span>

            </stripes:form>
        </div>		

    </stripes:layout-component>
</stripes:layout-render>