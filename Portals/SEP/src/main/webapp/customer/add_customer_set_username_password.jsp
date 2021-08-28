<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="capture.customer.password"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">

            window.onload = function () {
                checkIfUsernameDoesNotExist();
            };

            function checkIfUsernameDoesNotExist() {
                var username = document.getElementById("customerSSOIdentity").value;
                var labelMsg = document.getElementById("lblSSOIdStatus");
                var vFormData = new FormData();
                var outcome = false;

                vFormData.append("customerQuery.SSOIdentity", username);
                vFormData.append("customerQuery.resultLimit", 10);

                labelMsg.innerHTML = "";

                var oXHR = new XMLHttpRequest();
                oXHR.open('POST', '/sep/Customer.action;' + getJSessionId() + '?checkIfUsernameExists=', false);
                oXHR.send(vFormData);

                if (oXHR.responseText == "true") {
                    labelMsg.innerHTML = "";

                    labelMsg.innerHTML = "<fmt:message key="error.ssoid.not.available"/>";
                    labelMsg.setAttribute("style", "color:red");

                    outcome = false;
                } else
                if (oXHR.responseText == "false") {
                    labelMsg.innerHTML = "<fmt:message key="info.ssoid.available"/>";
                    labelMsg.setAttribute("style", "color:green");
                    outcome = true;
                } else {
                    outcome = false;
                }
                return outcome;
            }

        </script>

        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="customer"/>: <fmt:message   key="classification.${actionBean.customer.classification}" /> - ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                    </td>                        
                </tr>
            </table>         
            <stripes:form action="/Customer.action" focus="" onsubmit="return ((onBack == true) ? true : (alertValidationErrors() && checkIfUsernameDoesNotExist()));">   
                <stripes:hidden name="customer.warehouseId" value="${actionBean.customer.warehouseId}" />
                <stripes:hidden name="customer.identityNumberType" value="${actionBean.customer.identityNumberType}"/>
                <stripes:hidden name="customer.identityNumber" value="${actionBean.customer.identityNumber}"/>
                <stripes:hidden name="customer.cardNumber" value="${actionBean.customer.cardNumber}"/>
                <stripes:hidden name="customer.title" value="${actionBean.customer.title}"/>
                <stripes:hidden name="customer.firstName" value="${actionBean.customer.firstName}"/>
                <stripes:hidden name="customer.middleName" value="${actionBean.customer.middleName}"/>
                <stripes:hidden name="customer.lastName" value="${actionBean.customer.lastName}"/>
                <stripes:hidden name="customer.mothersMaidenName" value="${customer.mothersMaidenName}"/>
                <stripes:hidden name="customer.dateOfBirth" value="${actionBean.customer.dateOfBirth}"/>
                <stripes:hidden name="customer.gender" value="${actionBean.customer.gender}"/>
                <stripes:hidden name="customer.language" value="${actionBean.customer.language}"/>
                <stripes:hidden name="customer.emailAddress" value="${actionBean.customer.emailAddress}"/>
                <stripes:hidden name="customer.alternativeContact1" value="${actionBean.customer.alternativeContact1}"/>
                <stripes:hidden name="customer.alternativeContact2" value="${actionBean.customer.alternativeContact2}"/>
                <stripes:hidden name="customer.referralCode" value="${actionBean.customer.referralCode}"/>
                <stripes:hidden name="customer.optInLevel" value="${actionBean.customer.optInLevel}"/>
                <stripes:hidden name="customer.customerStatus" value="${actionBean.customer.customerStatus}"/>
                <stripes:hidden name="customer.classification" value="${actionBean.customer.classification}"/>
                <stripes:hidden name="customer.nationality"  value="${actionBean.customer.nationality}"/>
                <stripes:hidden name="customer.securityGroups[0]" value="${actionBean.customer.securityGroups[0]}"/>
                <stripes:hidden name="customer.passportExpiryDate" value="${actionBean.customer.passportExpiryDate}"/>
                <stripes:hidden name="customer.visaExpiryDate" value="${actionBean.customer.visaExpiryDate}"/>
                <stripes:hidden name="TTIssue.ID" value="${actionBean.TTIssue.ID}"/>
                <stripes:hidden name="customer.KYCStatus" value="${actionBean.customer.KYCStatus}"/>
                 
                <c:forEach items="${actionBean.customer.customerPhotographs}" varStatus="loop"> 
                    <stripes:hidden name="file${loop.index}"/>
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

                <table class="clear">                                
                    <tr>
                        <td><fmt:message   key="ssoidentity"/>:</td>
                        <td>
                            <c:choose>
                                <c:when test='${actionBean.customer.SSOIdentity == ""}'>
                                    <c:choose>
                                        <c:when test='${actionBean.customer.emailAddress != ""}'>
                                            <input type="text"  id="customerSSOIdentity" name="customer.SSOIdentity" class="required" maxlength="50" size="30" value="${actionBean.customer.emailAddress}"   onkeyup="validate(this, '^.{3,50}$', 'emptynotok')"/>
                                        </c:when>
                                        <c:otherwise>
                                            <input type="text"  id="customerSSOIdentity" name="customer.SSOIdentity" class="required" maxlength="50" size="30" value="${actionBean.customer.firstName}.${actionBean.customer.lastName}"   onkeyup="validate(this, '^.{3,50}$', 'emptynotok')"/>
                                        </c:otherwise>
                                    </c:choose>
                                </c:when>
                                <c:otherwise>
                                    <input type="text"  id="customerSSOIdentity" name="customer.SSOIdentity" class="required" maxlength="50" size="30" value="${actionBean.customer.SSOIdentity}"   onkeyup="validate(this, '^.{3,50}$', 'emptynotok')"/>
                                </c:otherwise>
                            </c:choose>
                            <stripes:button name="checkUsernameAvailability" value="Check availability" onclick="javascript: checkIfUsernameDoesNotExist();" />
                        </td>
                    </tr>   
                    <tr>
                        <td></td>
                        <td>
                            <label  id="lblSSOIdStatus"/>
                        </td>
                    </tr>

                </table>

                <span  class="button">
                    <stripes:submit name="showAddCustomerManageCustomerAddressesSummaryBack" onclick="onBack = true;"/>
                </span> 
                <span class="button">
                    <stripes:submit name="showAddCustomerWizardSummary" onclick="onBack = false;"/>
                </span>

            </stripes:form>
        </div>
    </stripes:layout-component>    
</stripes:layout-render>

