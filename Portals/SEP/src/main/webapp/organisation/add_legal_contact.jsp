<%-- 
    Document   : organisation_legal_contact
    Created on : 29 Mar, 2021, 3:44:37 PM
    Author     : user
--%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    Add Legal Contact
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
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
                                   <stripes:form action="/Customer.action" autocomplete="off" onsubmit="alertValidationErrors()">
                                       <stripes:hidden name="organisation.organisationId" value="${actionBean.organisation.organisationId}"/>

                                       <div id="entity">              
                                           <table  class="clear" id="tblLegalContacts">  
                                               <tbody>                                              
                                               
                                                   <tr style="display:none;">
                                                       <td><fmt:message key="organisation.organisationId"/>:</td>
                                                   <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                                                   <td><input type='text' readonly="true" id="organisationId" name="actionBean.organisation.organisationId" maxlength='100' size='40' value="${actionBean.organisation.organisationId}" onkeyup="${s:getValidationRule('organisationId', defaultRule)}"/></td>
                                                   </tr>
                                                   <tr>
                                                       <td><fmt:message key="organisation.nin"/>:</td>
                                                   <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                                                   <td><input type='text' onkeypress="return isNumberKey(event)" id="nin" name="nin" maxlength='11' size='20' value="" /></td>
                                                   <td style='vertical-align: top; margin-top:auto;'>                                                       
                                                            <stripes:submit name="retrieveLegalContactNinData"/>
                                                   </td>
                                                   </tr>
                                                   
                                                </tbody>    
                                           </table> 
                                                   
                                        <c:if test="${not empty actionBean.organisationLegalContact.firstName.trim()}">
                                            
                                            <table  class="clear" id="tblLegalContacts">  
                                               <tbody>            
                                                   <tr style="border-top:1px solid green">

                                                       <td><fmt:message key="organisation.legalContactType"/>:</td>                                  
                                                   <td>
                                                   <c:set var="defaultRule" value="validate(this,\\\'^.{1,50}$\\\',\\\'emptynotok\\\')"/>
                                                   <stripes:select id="organisationLegalContact.legalContactType" name="organisationLegalContact.legalContactType" onchange="${s:getValidationRule('customers.type',defaultRule)}">
                                                       <stripes:option value=""></stripes:option>
                                                       <c:forEach items="${s:getPropertyAsList('env.leagal.contact.types')}" var="userType" varStatus="loop2">
                                                           <stripes:option value="${userType}">${userType}</stripes:option>
                                                       </c:forEach>
                                                   </stripes:select>
                                                   </td>
                                                   <td style='vertical-align: top; margin-top:auto;'>                                                       
                                                            
                                                   </td>
                                                   </tr> 
                                                   <tr>
                                                       <td>NIN</td>
                                                   <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                                                   <td><input type='text' readonly="true" id="organisationLegalContact.nin" name="organisationLegalContact.nin" maxlength='100' size='40' value="${actionBean.organisationLegalContact.nin}" onkeyup="${s:getValidationRule('firstName', defaultRule)}"/></td>
                                                   </tr>
                                                   <tr>
                                                       <td>Firstname</td>
                                                   <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                                                   <td><input type='text' readonly="true" id="organisationLegalContact.firstName" name="organisationLegalContact.firstName" maxlength='100' size='40' value="${actionBean.organisationLegalContact.firstName}" onkeyup="${s:getValidationRule('firstName', defaultRule)}"/></td>
                                                   </tr>
                                                   <tr>
                                                       <td>Lastname</td>
                                                   <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                                                   <td><input type='text' readonly="true" id="organisationLegalContact.lastName" name="organisationLegalContact.lastName" maxlength='100' size='40' value="${actionBean.organisationLegalContact.lastName}" onkeyup="${s:getValidationRule('lastName', defaultRule)}"/></td>
                                                   </tr>
                                                   <tr>
                                                       <td>SIM Number</td>
                                                   <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                                                   <td><input type='text' onkeypress="return isNumberKey(event)" id="organisationLegalContact.iccid" name="organisationLegalContact.iccid" maxlength='100' size='40' value="${actionBean.organisationLegalContact.iccid}" onkeyup="${s:getValidationRule('lastName', defaultRule)}"/></td>
                                                   </tr>
                                                   <tr>
                                                   <c:set var="defaultRule" value="validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/ ,'emptynotok')"/>
                                                   <td>Email</td>
                                                   <td><input type='text' id="organisationLegalContact.email" name="organisationLegalContact.email" maxlength='100' size='40' value="${actionBean.organisationLegalContact.email}" onkeyup="${s:getValidationRule('email',defaultRule)}"/></td>
                                                   </tr> 
                                                   <tr>
                                                   <c:set var="defaultRule"  value="validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$/ ,'emptynotok')"/>
                                                   <td>Alternative Telephone</td>
                                                    <td><input type='text' onkeypress="return isNumberKey(event)" id="organisationLegalContact.telNumber" name="organisationLegalContact.telNumber" maxlength='100' size='40' value="${actionBean.organisationLegalContact.telNumber}" /><br/><br/></td>
                                                   </tr>
                                               </tbody>
                                           </table>
                                    <center>
                                        <table class="clear"  width="100%" id="tblDocuments">
                                            <tbody>
                                          
                                                <tr>
                                                    <td align="left" valign="top">
                                                            <input type = 'button' class='sep-button' value = 'Attach Passport Image' onclick ="javascript:document.getElementById('file0').click();" />
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
                                                                                                                                          
                                            </tbody>           
                                        </table>
                                        <br />
                                         <hr />
                                           <br />                                        
                                           
                                           <p class="button">
                                               <stripes:submit name="createLegalContacts"/>
                                           </p>  
                                        </c:if> 
                                                              
                                       </div>
                                </center>              
                                   </stripes:form>    
                                   </stripes:layout-component>
                                   </stripes:layout-render>
                    