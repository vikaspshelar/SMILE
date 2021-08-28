<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="customer.details.other.mno"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
                
        <stripes:form action="/Customer.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">
            <table class="clear">
                <tr>
                    <td><stripes:label for="CustomerQueryOtherMNORequest.registrationNamesRequest.Msisdn"/>:</td>
                    <td><stripes:text  name="customerQueryOtherMNORequest.registrationNamesRequest.msisdn" class="required" maxlength="20" onkeyup="validate(this,'^[0-9]{1,20}$','')"/>                        
                    </td>
                </tr>
                <tr>  
                    <td>
                        <span class="button">
                            <stripes:submit name="getCustomersOtherMNO"/> 
                        </span>    
                    </td>
                </tr>                                
            </table>            
        </stripes:form>
        <br/>
        <br/>
        <c:choose>
            <c:when test="${not empty actionBean.customerQueryOtherMNOResponse.registrationNamesResponse.firstName 
                      || not empty actionBean.customerQueryOtherMNOResponse.registrationNamesResponse.middleName
                        || not empty actionBean.customerQueryOtherMNOResponse.registrationNamesResponse.lastName}">
            <h1><fmt:message key="customer.details.view"/></h1>
            <table class="clear">
                <tr>
                    <td><fmt:message key="customeractionbean.customerqueryothermnoresponse.registrationnamesresponse.firstname"/>:</td>
                    <td>${actionBean.customerQueryOtherMNOResponse.registrationNamesResponse.firstName}</td>
                </tr>
                <tr>
                    <td><fmt:message key="customeractionbean.customerqueryothermnoresponse.registrationnamesresponse.middlename"/>:</td>
                    <td>${actionBean.customerQueryOtherMNOResponse.registrationNamesResponse.middleName}</td>
                </tr>
                <tr>
                    <td><fmt:message key="customeractionbean.customerqueryothermnoresponse.registrationnamesresponse.lastname"/>:</td>
                    <td>${actionBean.customerQueryOtherMNOResponse.registrationNamesResponse.lastName}</td>
                </tr>
            </table>
            </c:when> 
            <c:when test="${not empty actionBean.customerQueryOtherMNOResponse.registrationNamesResponse.responseCode}">
            <h1><fmt:message key="customer.details.view"/></h1>
            <table class="clear">
                <tr>
                    <td><fmt:message key="customeractionbean.customerqueryothermnoresponse.registrationnamesresponse.respcode"/>:</td>
                    <td>${actionBean.customerQueryOtherMNOResponse.registrationNamesResponse.responseCode}</td>
                </tr>
                <tr>
                    <td><fmt:message key="customeractionbean.customerqueryothermnoresponse.registrationnamesresponse.respdesc"/>:</td>
                    <td>${actionBean.customerQueryOtherMNOResponse.registrationNamesResponse.responseDesc}</td>
                </tr>
            </table>
            </c:when>   
        </c:choose>
    </stripes:layout-component>
</stripes:layout-render>