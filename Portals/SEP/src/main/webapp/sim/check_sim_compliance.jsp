<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="sim.compliance"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
                
        <stripes:form action="/SIM.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">                
            <table class="clear">
                <tr>
                    <td><stripes:label for="SIMVerifyRequest.StackholderVerifyReq.ControlNumber"/>:</td>
                    <td><stripes:text  name="SIMVerifyRequest.stackholderVerifyReq.controlNumber" class="required" maxlength="20" onkeyup="validate(this,'^[0-9]{1,20}$','')"/>                        
                    </td>
                </tr>    
                <tr>
                    <td><stripes:label for="SIMVerifyRequest.StackholderVerifyReq.IdentityNumber"/>:</td>
                    <td><stripes:text  name="SIMVerifyRequest.stackholderVerifyReq.identityNumber" class="required" maxlength="30" onkeyup="validate(this,'^.{1,30}$','')"/>                        
                    </td>
                </tr>
                <tr>  
                    <td>
                        <span class="button">
                            <stripes:submit name="performSIMComplianceCheck"/> 
                        </span>    
                    </td>
                </tr>                                
            </table>            
        </stripes:form>
        <br/>
        <br/>
        <c:if test="${actionBean.SIMVerifyResponse.stackholderResp.status == 100}">
            <h1><fmt:message key="sim.compliance.view"/></h1>
            <table class="clear">
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.fullname"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.fullname}</td>
                </tr>
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.identitytype"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.identityType}</td>
                </tr>
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.identitynumber"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.identityNumber}</td>
                </tr>
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.pdf"/>:</td>
                    <td>
                        <stripes:link href="/SIM.action" event="getPdf"> 
                            <stripes:param name="SIMVerifyRequest.stackholderVerifyReq.controlNumber" value="${actionBean.SIMVerifyRequest.stackholderVerifyReq.controlNumber}"/>
                            Click here to download as a pdf
                        </stripes:link>
                    </td>
                </tr>
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.rbnumber"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.rbNumber}</td>
                </tr>
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.dateofloss"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.dateofloss}</td>
                </tr>
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.losslocation"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.lossLocation}</td>
                </tr>
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.lossregion"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.lossRegion}</td>
                </tr>
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.controlnumber"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.controlNumber}</td>
                </tr>
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.itemtype"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.itemtype}</td>
                </tr>
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.itemname"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.itemname}</td>
                </tr>
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.itemnumber"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.itemnumber}</td>
                </tr>
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.otherdetails"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.otherDetails}</td>
                </tr>
            </table>
        </c:if>
        <c:if test="${actionBean.SIMVerifyResponse.stackholderResp.status != 100 
                      && actionBean.SIMVerifyResponse.stackholderResp.controlNumber > 0}">
            <h1><fmt:message key="sim.compliance.view"/></h1>
            <table class="clear">
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.status"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.status}</td>
                </tr>
                <tr>
                    <td><fmt:message key="simactionbean.simverifyresponse.stackholderresp.statusdescription"/>:</td>
                    <td>${actionBean.SIMVerifyResponse.stackholderResp.statusDescription}</td>
                </tr>
            </table>
        </c:if>        
    </stripes:layout-component>
</stripes:layout-render>


