<%-- 
    Document   : add_customer_role
    Created on : 17 Jan 2013, 5:09:29 PM
    Author     : lesiba
--%>
<%@page import="com.smilecoms.commons.base.BaseUtils, com.smilecoms.commons.stripes.SmileActionBean"%>

<%@ include file="/include/sep_include.jsp" %>
<c:set var="title">
    <fmt:message key="customer.port.into.smile"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">

            // var useWebCam = false; // Global variable to control the use of web cam .... (no webcam needed for Porting documents).

            var today = new Date().getFullYear();
            $j(document).ready(function () {
                $j('#datePicker1').datepicker({dateFormat: 'yy/mm/dd 00:00:00', showOn: 'button', buttonText: "..", changeYear: true, changeMonth: true, onSelect: function () {
                        validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01]) ([012][0-9])[:]([0-5][0-9]):([0-5][0-9])$', 'emptynotok');
                    }});
            });

            $j(window).load(function () {
                // executes when complete page is fully loaded, including all frames, objects and images
                $j('#datePicker1').datepicker();
                $j('#datePicker1').datepicker("setDate", new Date());


            });

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
                        <fmt:message key="customer"/> ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/Customer.action">                                
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveCustomer"><fmt:message key="manage.customer"/></stripes:option>                                
                            </stripes:select>
                            <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>

            <stripes:form action="/Customer.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();"> 
                <stripes:select name="customer.document.types"  id="customer.document.types" style="display:none">
                    <stripes:option value=""></stripes:option>
                    <c:forEach items="${s:getPropertyAsList('env.porting.document.types')}" var="documentType" varStatus="loop">
                        <stripes:option value="${documentType}">
                            <fmt:message  key="document.type.${documentType}"/>
                        </stripes:option>
                    </c:forEach>
                </stripes:select>


                <stripes:hidden id="portInEvent.messageType" name="portInEvent.messageType" value="SMILE_NEW_PORTIN_REQUEST"/>
                <stripes:hidden name="portInEvent.portingOrderId" value=""/>
                <stripes:hidden name="portInEvent.messageId" value="-1"/>
                <stripes:hidden name="portInEvent.customerType"    value="individual" />

                <c:forEach items="${actionBean.portInEvent.ringFenceNumberList}" var="phoneNumberRange" varStatus="loop"> 
                    <div ng-init="addPhoneNumberRangeEntryToList('ringfenceNumberRangeList', '${phoneNumberRange.phoneNumberStart}', '${phoneNumberRange.phoneNumberEnd}')"></div>
                </c:forEach>

                <c:forEach items="${actionBean.numberRangesToPort}" var="phoneNumberRange" varStatus="loop"> 
                    <div ng-init="addPhoneNumberRangeEntryToList('phoneNumberRangeList', '${phoneNumberRange.phoneNumberStart}', '${phoneNumberRange.phoneNumberEnd}')"></div>
                </c:forEach>


                <table class="clear">
                    <tr>
                        <td colspan="3"><b><fmt:message key="enter.portin.details"/>:</b></td>                        
                    </tr>
                    <% if (BaseUtils.getBooleanProperty("env.mnp.generate.port.request.form.id", false)) {%>
                    <tr>
                        <td><fmt:message key="port.request.form.id"/>:</td>
                        <td>
                            <stripes:hidden id="portInEvent.portRequestFormId" name="portInEvent.portRequestFormId" value="${actionBean.portInEvent.portRequestFormId}"/>
                            ${actionBean.portInEvent.portRequestFormId}
                        </td>
                    </tr>
                    <% } %>
                    <tr>
                        <td><fmt:message key="id"/>:</td>
                        <td>
                            <stripes:hidden id="portInEvent.customerProfileId" name="portInEvent.customerProfileId" value="${actionBean.customer.customerId}"/>
                            ${actionBean.customer.customerId}
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="customer.name"/>:</td>
                        <td>
                            <stripes:hidden name="portInEvent.customerName"    value="${actionBean.customer.firstName} ${actionBean.customer.lastName}" />
                            ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="classification"/>:</td>
                        <td>
                            <stripes:hidden name="portInEvent.customerType"    value="individual" />
                            Individual
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="date.of.birth"/>:</td>
                        <td>
                            <stripes:hidden name="portInEvent.dateOfBirth"    value="${actionBean.customer.dateOfBirth}" />
                            ${actionBean.customer.dateOfBirth}
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="id.number"/>:</td>
                        <td>
                            <stripes:hidden name="portInEvent.identityNumber"    value="${actionBean.customer.identityNumber}" />
                            ${actionBean.customer.identityNumber}
                        </td>
                    <tr>
                        <td>
                            <fmt:message key="id.number.type"/>:
                        </td>
                        <td>
                            <stripes:hidden name="portInEvent.customerIdType" id="customer.identityNumberType" value="${actionBean.customer.identityNumberType}"/>
                            ${actionBean.customer.identityNumberType}
                        </td>
                    </tr>                         
                    <tr>
                        <td><fmt:message key="subscription.type.at.donor"/>:</td>
                        <td>
                            <stripes:select name="portInEvent.subscriptionType">
                                <stripes:option value="prepaid">Prepaid</stripes:option>  
                                <stripes:option value="postpaid">Postpaid</stripes:option>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="porting.service.type"/>:</td>
                        <td>
                            <stripes:select name="portInEvent.serviceType">
                                <stripes:option value="fixed">Fixed</stripes:option>  
                                <stripes:option value="mobile" selected="true">Mobile</stripes:option>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td><fmt:message key="portin.donor.network"/>:</td>
                        <td>
                            <c:set var="defaultRule" value="validate(this, \\\'^.{1,3}$\\\',\\\'emptynotok\\\')"/>
                            <stripes:select name="portInEvent.donorId" onchange="${s:getValidationRule('porting.donor', defaultRule)}">
                                <stripes:option value=""></stripes:option>
                                <c:forEach items="${s:getDelimitedPropertyAsList('env.mnp.donor.networks.ids.mapping')}" var="network" varStatus="loop">
                                    <stripes:option value="${network[1]}">${network[0]}</stripes:option>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>

                    <tr>
                        <td><fmt:message key="portin.original.rangeholder"/>:</td>
                        <td>
                            <c:set var="defaultRule" value="validate(this, \\\'^.{1,3}$\\\',\\\'emptynotok\\\')"/>
                            <stripes:select name="portInEvent.rangeHolderId" onchange="${s:getValidationRule('porting.donor', defaultRule)}">
                                <stripes:option value=""></stripes:option>
                                <c:forEach items="${s:getDelimitedPropertyAsList('env.mnp.donor.networks.ids.mapping')}" var="network" varStatus="loop">
                                    <stripes:option value="${network[1]}">${network[0]}</stripes:option>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>

                    <tr>
                        <td><fmt:message key="porting.validation.msisdn"/>:</td>
                        <c:set var="defaultRule" value="validate(this, \\\'^[0-9]{10}$\\\',\\\'emptynotok\\\')"/>
                        <td><stripes:text name="portInEvent.validationMSISDN" value="${actionBean.customer.alternativeContact1}" maxlength="11" onkeyup="${s:getValidationRule('porting.msisdn', defaultRule)}"/></td>
                    </tr>

                    <tr>
                        <td><fmt:message key="portin.handlemanually"/>:</td>
                        <td>
                            <stripes:select name="portInEvent.handleManually">
                                <stripes:option value="false">FALSE</stripes:option>  
                                <stripes:option value="true">TRUE</stripes:option>                                
                            </stripes:select>
                        </td>                        
                    </tr>
                    <tr>
                        <td><fmt:message key="portin.datetime"/>:</td>
                        <td><input type="text" id="datePicker1" name="portInEvent.portingDate"  maxlength="19" size="19" onkeyup="validate(this, '^(19|20)[0-9][0-9][/](0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01]) ([012][0-9])[:]([0-5][0-9]):([0-5][0-9])$', 'emptydatenotok')" /></td> 
                    </tr>
                </table>

                <table>
                    <tr>
                        <td colspan="2">
                            <br/>
                            <b><fmt:message key="numbers.to.portin"/>:</b>
                            <br/>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <div>            
                                <table class="green" width="99%"> 
                                    <c:choose>
                                        <c:when test="${s:getListSize(actionBean.customersServiceInstances) > 0}">
                                            <tr>
                                                <th><fmt:message key="checkbox.port"/></th>
                                                <th><fmt:message key="id"/></th>
                                                <th><fmt:message key="service.name"/></th>
                                                <th><fmt:message key="provisioning.status"/></th>
                                                <th><fmt:message key="description"/></th>
                                                <th><fmt:message key="number.to.port"/></th>
                                            </tr>
                                            <c:set var="voiceIndex" value="0"/>
                                            <c:set var="allowedPortingServices" value="${s:getPropertyAsSet('env.mnp.porting.allowed.services.list')}"/>
                                            <c:forEach items="${actionBean.customersServiceInstances}" var="serviceInstance" varStatus="loop">                        
                                                <c:if test="${s:setContains(allowedPortingServices, serviceInstance.serviceSpecificationId)}">
                                                    <c:set var="serviceSpec" value="${s:getServiceSpecification(serviceInstance.serviceSpecificationId)}"/>
                                                    <c:if test="${serviceInstance.status == 'TD'}">
                                                        <tr class="greyout">
                                                        </c:if>
                                                        <c:if test="${serviceInstance.status != 'TD'}">
                                                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                                                        </c:if>
                                                        <td><stripes:checkbox name="portInEvent.routingInfoList.routingInfo[${voiceIndex}].selectedForPortIn"/></td>  
                                                        <td>
                                                            <stripes:link href="/ProductCatalog.action" event="retrieveServiceInstance">
                                                                <stripes:param name="serviceInstance.serviceInstanceId" value="${serviceInstance.serviceInstanceId}"/>
                                                                ${serviceInstance.serviceInstanceId}
                                                            </stripes:link></td>
                                                        <td>${serviceSpec.name}</td>
                                                        <td>${serviceInstance.status}</td>
                                                        <td>${serviceSpec.description} (ICCID:${s:getServiceInstanceICCID(serviceInstance.serviceInstanceId)})</td>
                                                        <c:set var="defaultRule" value="validate(this, \\\'^[0-9]{10}$\\\',\\\'emptyok\\\')"/>
                                                        <td><stripes:text name="portInEvent.routingInfoList.routingInfo[${voiceIndex}].phoneNumberRange.phoneNumberStart"  maxlength="11"  onkeyup="${s:getValidationRule('porting.msisdn', defaultRule)}"/></td> 
                                                        <stripes:hidden name="portInEvent.routingInfoList.routingInfo[${voiceIndex}].serviceInstanceId" value="${serviceInstance.serviceInstanceId}" />
                                                        <stripes:hidden name="portInEvent.routingInfoList.routingInfo[${voiceIndex}].routingNumber" value="${s:getPropertyValueFromList('env.mnp.config', 'SmileNetworkRoutingNumber')}" />
                                                    </tr>
                                                    <c:set var="voiceIndex" value="${voiceIndex + 1}"/>
                                                </c:if>
                                            </c:forEach>
                                        </c:when>
                                        <c:otherwise>
                                            <tr>
                                                <td>
                                                    <fmt:message key="no.service.instances"/>
                                                </td>
                                            </tr>
                                        </c:otherwise>
                                    </c:choose>       
                                </table>
                            </div>
                        </td>
                    </tr>
                </table>

                <% if (BaseUtils.getBooleanProperty("env.mnp.allow.numberrange.porting", false)) {%>
                <table>
                    <tr>
                        <td colspan="2">
                            <br/>
                            <b><fmt:message key="number.ranges.portin"/>:</b>
                            <br/>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <div>    
                                <table class="green" width="99%" > 
                                    <tr>
                                        <th>Item #</th>
                                        <th><fmt:message key="phone.number.start"/></th>
                                        <th><fmt:message key="phone.number.end"/></th>
                                        <th><fmt:message key="delete"/></th>
                                    </tr>
                                    <tr ng-repeat="item in phoneNumberRangeList.items">
                                        <td>{{$index + 1}}</td>
                                        <c:set var="defaultRule" value="validate(this, \\\'^[0-9]{10}$\\\',\\\'emptynotok\\\')"/>
                                        <td><stripes:text name="numberRangesToPort[{{ $index }}].phoneNumberStart" ng-model="item.phoneNumberStart"  onkeyup="${s:getValidationRule('porting.msisdn', defaultRule)}" maxlength="15" size='15'/></td>           
                                        <td><stripes:text name="numberRangesToPort[{{ $index }}].phoneNumberEnd" ng-model="item.phoneNumberEnd" onkeyup="${s:getValidationRule('porting.msisdn', defaultRule)}" maxlength="15" size='15'/></td>           
                                        <td>
                                            [<a href ng:click="removePhoneNumberRangeEntryFromList('phoneNumberRangeList', $index)">Delete</a>]
                                        </td>
                                    </tr>
                                    <tr>
                                        <td></td>
                                        <td></td>
                                        <td></td>
                                        <td>Count: {{ phoneNumberRangeEntryListSize('phoneNumberRangeList')}}</td>
                                    </tr>
                                </table>
                                <span>
                                    <input type="button" ng:click="addPhoneNumberRangeEntryToList('phoneNumberRangeList', null, null)" value="Add phone number"/>
                                </span>
                            </div>
                        </td>
                    </tr>
                </table>
                <% } %>

                <% if (BaseUtils.getBooleanProperty("env.mnp.include.ringfencing.on.new.portorder", false)) {%>
                <table>
                    <tr>
                        <td colspan="2">
                            <br/>
                            <b><fmt:message key="ring.fencing"/>:</b>
                            <br/>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <div>    
                                <table class="green" width="99%" > 
                                    <tr>
                                        <th>Item #</th>
                                        <th><fmt:message key="phone.number.start"/></th>
                                        <th><fmt:message key="phone.number.end"/></th>
                                        <th><fmt:message key="delete"/></th>
                                    </tr>
                                    <tr ng-repeat="item in ringfenceNumberRangeList.items">
                                        <td>{{$index + 1}}</td>
                                        <c:set var="defaultRule" value="validate(this, \\\'^[0-9]{10}$\\\',\\\'emptynotok\\\')"/>
                                        <td><stripes:text name="portInEvent.ringFenceNumberList[{{ $index }}].phoneNumberStart" type="text"  ng-model="item.phoneNumberStart"  onkeyup="${s:getValidationRule('porting.msisdn', defaultRule)}" maxlength="15" size='15'/></td>           
                                        <td><stripes:text name="portInEvent.ringFenceNumberList[{{ $index }}].phoneNumberEnd" type="text" ng-model="item.phoneNumberEnd" onkeyup="${s:getValidationRule('porting.msisdn',defaultRule)}" maxlength="15" size='15'/></td>           
                                        <td>
                                            [<a href ng:click="removePhoneNumberRangeEntryFromList('ringfenceNumberRangeList', $index)">Delete</a>]
                                        </td>
                                    </tr>
                                    <tr>
                                        <td></td>
                                        <td></td>
                                        <td></td>
                                        <td>Count: {{ phoneNumberRangeEntryListSize('ringfenceNumberRangeList')}}</td>
                                    </tr>
                                </table>
                                <span>
                                    <input type="button" ng:click="addPhoneNumberRangeEntryToList('ringfenceNumberRangeList', null, null)" value="Add phone number"/>
                                </span>
                            </div>
                        </td>
                    </tr>
                </table>
                <% } %>    

                <% if (BaseUtils.getBooleanProperty("env.mnp.port.request.form.mandatory", false)) {%>

                <% if (BaseUtils.getBooleanProperty("env.mnp.include.ringfencing.on.new.portorder", false)) {%>
                <div id="divDocuments" style="display:none">
                    <% } else { %>
                    <div id="divDocuments">    
                        <% }%>
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
                                                        <fmt:message  key="document.type.${actionBean.photographs[loop.index].photoType}"/>
                                                        <stripes:hidden   name="photographs[${loop.index}].photoType" value="${actionBean.photographs[loop.index].photoType}" />
                                                    </td>
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
                                                <input value="WebCam" class="file" id="snap" type="button" onclick="snapAndUpload('file${loop.index}');"/>
                                                <input value="Remove" class="file" onclick="removeDocument(this, 'photographs');" type="button"/>
                                                <input class="file" id="file${loop.index}" name="file${loop.index}" type="file" style="visibility: hidden;" />
                                                <script type="text/javascript">
                                                            document.getElementById('file${loop.index}').addEventListener('change', uploadDocument, false);
                                                </script>
                                            </td>
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

                        </table> 
                    </div>
                    <% }%>


                    <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}" />
                    <stripes:hidden name="portInEvent.organisationId" value="0" />

                    <span class="button">
                        <stripes:submit name="createPortInRequest"/>
                    </span>

                </stripes:form>

            </div>
        </stripes:layout-component>
    </stripes:layout-render>