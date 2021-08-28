<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="imei.change"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
                
        <stripes:form action="/SIM.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">   
            <stripes:hidden name="IMEIStatusChangeRequest.equipmentType" value="${actionBean.IMEIStatusChangeGetinfoRequest.equipmentType}"/> 
            <c:if test="${not empty actionBean.IMEIStatusChangeGetinfoRequest.equipmentType}">
                <h1><fmt:message key="imei.change"/></h1>
                <table class="clear">
                    <tr>
                        <td><stripes:label for="IMEIStatusChangeRequest.action"/>:</td>
                        <td>
                            <stripes:select name="IMEIStatusChangeRequest.action">
                                <stripes:option value="CRE">Create New IMEI Entry</stripes:option>
                                <stripes:option value="DEL">Delete Existing IMEI Entry</stripes:option>
                            </stripes:select>
                        </td>
                    </tr> 
                    <tr>
                        <td><stripes:label for="IMEIStatusChangeRequest.imei"/>:</td>
                        <td><stripes:text  name="IMEIStatusChangeRequest.imei" class="required" size="15" maxlength="15" onkeyup="validate(this,'^[0-9]{14,15}$','emptynotok')"/>                        
                        </td>
                    </tr>                    
                    <c:if test="${actionBean.IMEIStatusChangeGetinfoRequest.equipmentType == 'EQU'}">
                        <tr>
                        <td><stripes:label for="IMEIStatusChangeRequest.status"/>:</td>
                            <td>
                                <stripes:select name="IMEIStatusChangeRequest.status">
                                    <stripes:option value="WL">Whitelist</stripes:option>
                                    <stripes:option value="BL">Blacklist</stripes:option>
                                    <stripes:option value="GL">Greylist</stripes:option>
                                </stripes:select>
                            </td>
                        </tr> 
                        <tr>
                            <td><stripes:label for="IMEIStatusChangeRequest.imeiend"/>:</td>
                            <td><stripes:text  name="IMEIStatusChangeRequest.imeiend" class="required" size="15" maxlength="15" onkeyup="validate(this,'^[0-9]{14,15}$','emptynotok')"/>                        
                            </td>
                        </tr>
                        <tr>
                            <td><stripes:label for="IMEIStatusChangeRequest.comment"/>:</td>
                            <td><stripes:text  name="IMEIStatusChangeRequest.comment" class="required"/>                        
                            </td>
                        </tr>
                    </c:if>
                    <c:if test="${actionBean.IMEIStatusChangeGetinfoRequest.equipmentType != 'EQU'}">
                        <tr>
                            <td><stripes:label for="IMEIStatusChangeRequest.imsi"/>:</td>
                            <td><stripes:text  name="IMEIStatusChangeRequest.imsi" class="required" size="16" maxlength="16" onkeyup="validate(this,'^[0-9]{15,16}$','emptynotok')"/>                        
                            </td>
                        </tr>
                    </c:if>
                    <tr>  
                        <td>
                         <span class="button">
                             <stripes:submit name="PerformIMEIStatusChange"/> 
                         </span>    
                        </td>
                    </tr>    
                </table>
            </c:if>           
        </stripes:form>
        <br/>
        <br/> 
        <h1><b><u>Note:</u></b></h1>
        <fmt:message key="change.imei.note"/>
        <br/>
        <br/>  
    </stripes:layout-component>
</stripes:layout-render>