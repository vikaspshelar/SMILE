<%-- 
    Document   : alternative_customer_verification.jsp
    Created on : 18 Jun 2020, 2:31:00 PM
    Author     : bhaskarhg
--%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="defaced.customer.verification"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/Customer.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">
            <div>
                <c:if test="${empty actionBean.verifyDefacedCustomerRequest.questionCode && empty actionBean.verifyDefacedCustomerRequest.questionSwahili}">
                    <p><b><fmt:message key="defaced.customr.verification.message"/></b>
                </c:if>
            </div>
            <div>
                <table class="clear">
                    <tr>
                        <td><fmt:message key="customer.nin"/>:</td>
                    <td><stripes:text name="verifyDefacedCustomerRequest.nin" value="${actionBean.verifyDefacedCustomerRequest.nin}" size="30" onkeyup="validate(this, '^.{2,50}$', 'emptyok')"/></td>
                    </tr> 
                    <c:if test="${not empty actionBean.verifyDefacedCustomerRequest.questionCode && not empty actionBean.verifyDefacedCustomerRequest.questionSwahili}">
                        <tr>
                            <td><fmt:message key="customer.nin.question.english"/>:</td>
                        <td>${actionBean.verifyDefacedCustomerRequest.questionEnglish}</td>
                        </tr>
                        <tr>
                            <td><fmt:message key="customer.nin.question.swahili"/>:</td>
                        <td>${actionBean.verifyDefacedCustomerRequest.questionSwahili}</td>
                        </tr>
                        <tr>
                            <td><fmt:message key="customer.nin.question.answer"/>:</td>
                        <td><input type="text" name="verifyDefacedCustomerRequest.answer" value="${actionBean.verifyDefacedCustomerRequest.answer}" size="30" onkeyup="validate(this, '^.{2,50}$', 'emptyok')"/></td>
                        </tr>   
                        <tr>    
                            <td><input type="hidden" name="verifyDefacedCustomerRequest.questionCode" value="${actionBean.verifyDefacedCustomerRequest.questionCode}" size="30" onkeyup="validate(this, '^.{2,50}$', 'emptyok')"/></td>
                        </tr>
                    </c:if>   
                    <tr>
                        <td>
                            <span class="button">
                                <stripes:submit name="verifyDefacedCustomer"/>
                            </span>
                        </td>
                    </tr>
                </table> 
            </div>

            <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}" id="custId"/>                 
            <stripes:hidden name="customer.identityNumberType" value="${actionBean.customer.identityNumberType}" />
            <stripes:hidden name="customer.customerStatus" value="AC" />
            <stripes:hidden name="customer.optInLevel" value="7" />
            <stripes:hidden name="customer.SSOIdentity" value="${actionBean.customer.SSOIdentity}"/>
            <stripes:hidden name="customer.SSODigest" value="${actionBean.customer.SSODigest}"/>
            <stripes:hidden name="customer.securityGroups[0]" value="Customer"/>
            <stripes:hidden name="customer.warehouseId" value="${actionBean.customer.warehouseId}" />
            <stripes:hidden name="TTIssue.ID" value="${actionBean.TTIssue.ID}"/>

<!--            <stripes:hidden name="verifyDefacedCustomerRequest.nin" value="${verifyDefacedCustomerRequest.nin}"/>-->
            <!--            <stripes:hidden name="verifyDefacedCustomerRequest.questionCode"/>-->
        </stripes:form>
    </stripes:layout-component>    
</stripes:layout-render>