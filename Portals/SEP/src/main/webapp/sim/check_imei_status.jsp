<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="imei.check"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
                
        <stripes:form action="/SIM.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">                
            <table class="clear">
                <tr>
                    <td><stripes:label for="IMEICheckRequest.imei"/>:</td>
                    <td><stripes:text  name="IMEICheckRequest.imei" class="required" size="15" maxlength="15" onkeyup="validate(this,'^[0-9]{14,15}$','emptynotok')"/>                        
                    </td>
                </tr>
                <tr>
                    <td><stripes:label for="IMEICheckRequest.imsi"/>:</td>
                    <td><stripes:text  name="IMEICheckRequest.imsi" class="required" size="16" maxlength="16" onkeyup="validate(this,'^[0-9]{15,16}$','emptynotok')"/>                        
                    </td>
                </tr>
                <tr>  
                    <td>
                        <span class="button">
                            <stripes:submit name="PerformIMEICheck"/> 
                        </span>    
                    </td>
                </tr>                                
            </table>            
        </stripes:form>
        <br/>
        <br/>    
        <c:if test="${not empty actionBean.IMEICheckResponse.status}">
            <tr>
                <td><fmt:message key="simactionbean.imeistatuscheckresponse.status"/>: </td>
                <td>${actionBean.IMEICheckResponse.status}</td>
            </tr>
        </c:if>
        <br/>
        <br/> 
        <h1><b><u>Note:</u></b></h1>
        <fmt:message key="check.imei.note"/>
        <br/>
        <br/> 
    </stripes:layout-component>
</stripes:layout-render>