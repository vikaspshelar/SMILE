<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="imei.change"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
                
        <stripes:form action="/SIM.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">                
            <table class="clear">
                <tr>
                    <td><stripes:label for="IMEIStatusChangeRequest.equipmentType"/>:</td>
                    <td>
                        <stripes:select name="IMEIStatusChangeGetinfoRequest.equipmentType">
                            <stripes:option value="EQU">Standard Equipment List</stripes:option>
                            <stripes:option value="CLHU">Cloned Handsets List</stripes:option>
                            <stripes:option value="EMST">MS List</stripes:option>
                            <stripes:option value="FSIM">Fixed SIM List</stripes:option>
                        </stripes:select>
                    </td>
                </tr>
                <tr>  
                    <td>
                        <span class="button">
                            <stripes:submit name="GetDetailsForIMEIStatusChange"/> 
                        </span>    
                    </td>
                </tr>                            
            </table> 
        </stripes:form>
        <br/>
        <br/>
        <h1><b><u>Note:</u></b></h1>
        <fmt:message key="change.imei.info.note"/>
        <br/>
        <br/>
    </stripes:layout-component>
</stripes:layout-render>