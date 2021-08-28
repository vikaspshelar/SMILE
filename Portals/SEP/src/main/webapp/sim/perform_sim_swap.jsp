<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="perform.sim.swap"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
                
        <stripes:form action="/SIM.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">                
            <table class="clear">
                <tr>
                    <td><stripes:label for="SIMSwapRequest.oldIntegratedCircuitCardIdentifier"/>:</td>
                    <td><stripes:text  name="SIMSwapRequest.oldIntegratedCircuitCardIdentifier" class="required" size="20" maxlength="20" onkeyup="validate(this,'^[0-9]{20,20}$','luhn')"/>
                        
                    </td>
                </tr>
                <tr>
                    <td><stripes:label for="SIMSwapRequest.newIntegratedCircuitCardIdentifier"/>:</td>
                    <td><stripes:text name="SIMSwapRequest.newIntegratedCircuitCardIdentifier" class="required" size="20" maxlength="20" onkeyup="validate(this,'^[0-9]{20,20}$','luhn')"/>
                    
                    </td>
                </tr> 
                               
                <tr>  
                    <td>
                        <span class="button">
                            <stripes:submit name="performSIMSwap"/> 
                        </span>    
                    </td>
                </tr>                                
            </table>            
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>


