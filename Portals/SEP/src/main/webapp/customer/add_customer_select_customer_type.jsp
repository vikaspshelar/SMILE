<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="com.smilecoms.commons.base.BaseUtils"%>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.new.customer.select.type"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="html_head">            
        <script type="text/javascript">

//            function checkSelection(obj) {
//
//                var custId = document.getElementById("custId");
//                var addCust = document.getElementById("addCust");
//                var nida = document.getElementById("nida");
//                var immigration = document.getElementById("immigration");
//
//                if (custId.value > 0) {// Customer is a foreign international
//                    if (obj.value === 'foreigner') {
//                        immigration.style.display = "table-row";
//                        addCust.style.display = "none";
//                        nida.style.display = "none";
////                        submitbutton = addCust.name = "verifyExistingCustomerWithNIDAOrImmigration";
////                        immigration = immigration + 'foreigner';
//                    }
//
//                    if (obj.value === 'Individual Customer') {
////                        submitbutton = addCust.name = "verifyExistingCustomerWithNIDA";
//                        nida.style.display = "none";
//                        immigration.style.display = "table-row";
//                        addCust.style.display = "none";
//
//                    }
//                }
//                console.log("Returning true");
//
//                return true;
//            }
//            window.onload = function () {
//                var bio = document.getElementById("biokyc");
//                var biometric = document.getElementsByName("biometricKyc");
//                if (biometric[0].value === "true") {
//                    bio.style.display = "none";
//                }
//            };

//            window.onload = function () {
//                var nida = document.getElementById("nida");
//                var immigration = document.getElementById("immigration");
//                if (nida != null && immigration != null) {
//                    nida.style.display = "none";
//                    immigration.style.display = "none";
//                }
//
//            };
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <div id="entity">
            <stripes:form action="/Customer.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">    

                <table class="clear">
                    <% if (BaseUtils.getBooleanProperty("env.customer.verify.with.nida", false)) {%>
                    
                    <% if (BaseUtils.getBooleanProperty("env.customer.verify.with.nida.choose")==true) {%>
                            <tr id="biokyc">
                     <% } %>
                     <% if (BaseUtils.getBooleanProperty("env.customer.verify.with.nida.choose")==false) {%>
                            <tr id="biokyc" style="display: none">
                     <% } %>
                            <td><stripes:label for="biometrickycyn"/>:</td>
                        <td><stripes:checkbox name="biometricKyc" checked="true"/></td>
                        </tr>
                    <% } %>
                    <tr>
                        <td><stripes:label for="classification"/>:</td>
                    <td>
                        <!--                    <c:set var="defaultRule" value="checkSelection(this); validate(this,'^[a-zA-Z]{2,2}$','emptynotok')"/>-->
                    <stripes:select name="customer.classification" onchange="checkSelection(this);return validate(this, '^[a-zA-Z]{4,20}$', 'emptynotok');">
                        <stripes:option value=""></stripes:option>
                        <c:forEach items="${s:getDelimitedPropertyAsList('env.customer.classifications')}" var="classification" varStatus="loop">
                            <c:choose>
                                <c:when test='${actionBean.customer.classification == classification[0]}'>
                                    <stripes:option value="${classification[0]}" selected="true"><fmt:message key="classification.${classification[0]}" /></stripes:option>
                                </c:when>
                                <c:otherwise>
                                    <stripes:option value="${classification[0]}"><fmt:message key="classification.${classification[0]}" /></stripes:option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </stripes:select>
                    </td>
                    </tr>
                </table>
                <span class="button">
                    <stripes:submit name="showAddCustomerSearchAndConvertSaleslead"/>
                </span>

                <c:choose>
                    <c:when test="${actionBean.verifyExistingCustomerWithNIDAOrImmigration}">
                        <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}" id="custId"/>
                    </c:when>
                </c:choose>
                <stripes:hidden name="customer.identityNumberType" value="${actionBean.customer.identityNumberType}" />
                <stripes:hidden name="customer.customerStatus" value="AC" />
                <stripes:hidden name="customer.optInLevel" value="7" />
                <stripes:hidden name="customer.SSOIdentity" value="${actionBean.customer.SSOIdentity}"/>
                <stripes:hidden name="customer.SSODigest" value="${actionBean.customer.SSODigest}"/>
                <stripes:hidden name="customer.securityGroups[0]" value="Customer"/>
                <stripes:hidden name="customer.warehouseId" value="${actionBean.customer.warehouseId}" />
                <stripes:hidden name="TTIssue.ID" value="${actionBean.TTIssue.ID}"/>

                <c:forEach items="${actionBean.customer.customerPhotographs}" varStatus="loop"> 
                    <stripes:hidden class="file" id="file${loop.index}" name="file${loop.index}"/>
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
                    <stripes:hidden name="customer.addresses[${loop.index}].zone"    value="${actionBean.customer.addresses[loop.index].zone}" />
                </c:forEach>

            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>