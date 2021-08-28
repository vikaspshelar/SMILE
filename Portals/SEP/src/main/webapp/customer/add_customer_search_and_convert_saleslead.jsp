<%@ include file="/include/sep_include.jsp" %>


<c:set var="title">
    <fmt:message key="add.new.customer.sales.lead"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">

    <stripes:layout-component name="contents">
        <div id="entity">
            <table class="clear" width="99%">
                <tr>
                    <td>
                        <stripes:form action="/TroubleTicket.action" onsubmit="return false"  autocomplete="off" ng-submit="getSalesLeadByWildCard(wildCardChar);" >    
                            <input type="text" name="item" size="35" ng-model="wildCardChar" ng-readonly="looking" placeholder="Please enter name to start search"/>
                            <stripes:submit name="add"/>
                        </stripes:form> 
                    </td>
                    <td><b>OR</b></td>
                    <td>
                        <stripes:form action="/Customer.action"  class="buttonOnly" >    

                            <span class="button">
                                <stripes:submit name="showAddCustomerWizard" onclick="onBack=true;"/>
                            </span>

                            <span class="button">
                                <stripes:submit name="showAddCustomerSkipSalesleadSearch"  onclick="onBack=false;onNext=true;"/>
                            </span>
                            
                            <stripes:hidden name="customer.KYCStatus" value="${actionBean.customer.KYCStatus}"/>
                            <stripes:hidden name="customer.customerStatus" value="AC" />
                            <stripes:hidden name="customer.optInLevel" value="${actionBean.customer.optInLevel}" />
                            <stripes:hidden name="customer.SSOIdentity" value="${actionBean.customer.SSOIdentity}"/>
                            <stripes:hidden name="customer.SSODigest" value="${actionBean.customer.SSODigest}"/>
                            <stripes:hidden name="customer.securityGroups[0]" value="Customer"/>
                            <stripes:hidden name="customer.warehouseId" value="${actionBean.customer.warehouseId}" />
                            <stripes:hidden name="customer.classification" value="${actionBean.customer.classification}" />
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
                    </td>
                </tr>
            </table>



            <div ng-show="ttIssues.simpleIssue != '' && ttIssues.simpleIssue != null ">
                <div id="simplyfiedIssue">
                    <c:set var="countryName" value="${s:getProperty('env.country.name')}"/>
                    <table class="green" width="99%">            
                        <tr>
                            <th></th>
                            <th><fmt:message key="trouble.ticket.status"/></th>
                            <th><fmt:message key="trouble.ticket.assignee"/></th>
                            <th><fmt:message key="trouble.ticket.created"/></th>
                            <th><fmt:message key="tt.saleslead.xinfo"/></th>
                            <th><fmt:message key="tt.saleslead.convert.to.customer"/></th>
                        </tr>
                        <tr ng-repeat="issue in ttIssues.simpleIssue">      
                            <td>{{issue.issueId}}</td>
                            <td>{{issue.status}}</td>
                            <td>{{issue.assignee}}</td>
                            <td>{{issue.created}}</td>
                            <td align="left">FirstName: {{issue.firstName}}<br/>Email: {{issue.emailAddress}}<br/>Phone: {{issue.alternativeContact1}}<br/>
                                Location: {{issue.addressLine1}} <br/>Street: {{issue.addressLine2}}<br/>Town: {{issue.addressTown}} <br/>Code: {{issue.addressCode}}</td>
                            <td>           
                                <stripes:form action="/TroubleTicket.action" class="buttonOnly">
                                    <stripes:hidden name="customer.firstName" value="{{issue.firstName}}" />
                                    <stripes:hidden name="customer.emailAddress" value="{{issue.emailAddress}}" />
                                    <stripes:hidden name="customer.alternativeContact1" value="{{issue.alternativeContact1}}" />
                                    <stripes:hidden name="customer.addresses[0].line1" value="{{issue.addressLine1}}" />
                                    <stripes:hidden name="customer.addresses[0].line2" value="{{issue.addressLine2}}" />
                                    <stripes:hidden name="customer.addresses[0].town" value="{{issue.addressTown}}" />
                                    <stripes:hidden name="customer.addresses[0].code" value="{{issue.addressCode}}" />
                                    <stripes:hidden name="customer.addresses[0].country" value="${countryName}" />
                                    <input type="hidden" name="TTIssue.ID" value="{{issue.issueId}}"/>

                                    <stripes:hidden name="customer.customerStatus" value="AC" />
                                    <stripes:hidden name="customer.optInLevel" value="${actionBean.customer.optInLevel}" />
                                    <stripes:hidden name="customer.SSOIdentity" value="${actionBean.customer.SSOIdentity}"/>
                                    <stripes:hidden name="customer.SSODigest" value="${actionBean.customer.SSODigest}"/>
                                    <stripes:hidden name="customer.securityGroups[0]" value="Customer"/>
                                    <stripes:hidden name="customer.warehouseId" value="${actionBean.customer.warehouseId}" />
                                    <stripes:hidden name="customer.classification" value="${actionBean.customer.classification}" />

                                    <c:forEach items="${actionBean.customer.customerPhotographs}" varStatus="loop"> 
                                        <stripes:hidden class="file" id="file${loop.index}" name="file${loop.index}"/>
                                        <stripes:hidden id="photoGuid${loop.index}" name="customer.customerPhotographs[${loop.index}].photoGuid" value="${actionBean.customer.customerPhotographs[loop.index].photoGuid}"/>
                                        <stripes:hidden id="photoType${loop.index}" name="customer.customerPhotographs[${loop.index}].photoType" value="${actionBean.customer.customerPhotographs[loop.index].photoType}"/>
                                    </c:forEach>

                                    <stripes:submit name="showAddCustomerBasicDetailsPage"/>
                                </stripes:form>
                            </td>

                        </tr>
                        <tr class="even">
                            <td colspan="7"></td>
                        </tr>
                        <tr class="even">
                            <td colspan="6">
                                <b><fmt:message key="register.customer.ignore.saleslead.msg"/></b><br/>
                                <stripes:form action="/Customer.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">    

                                    <span class="button">
                                        <stripes:submit name="showAddCustomerBasicDetails"  onclick="onBack=false;onNext=true;"/>
                                    </span>
                                    <stripes:hidden name="customer.customerStatus" value="AC" />
                                    <stripes:hidden name="customer.optInLevel" value="${actionBean.customer.optInLevel}" />
                                    <stripes:hidden name="customer.SSOIdentity" value="${actionBean.customer.SSOIdentity}"/>
                                    <stripes:hidden name="customer.SSODigest" value="${actionBean.customer.SSODigest}"/>
                                    <stripes:hidden name="customer.securityGroups[0]" value="Customer"/>
                                    <stripes:hidden name="customer.warehouseId" value="${actionBean.customer.warehouseId}" />
                                    <stripes:hidden name="customer.classification" value="${actionBean.customer.classification}" />
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
                            </td>
                        </tr>
                    </table>                
                </div>
            </div>
            <div ng-show="ttIssues.simpleIssue == '' && ttIssues.simpleIssue != null ">
                <p><em><fmt:message key="register.customer.nosaleslead.found.msg"/></em></p>
            </div>
            <div ng-show="serverError != '' && serverError != null " ng-style="{ color:'red' }">
                <p><em><fmt:message key="register.customer.nosaleslead.servererror.msg"/></em></p>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>