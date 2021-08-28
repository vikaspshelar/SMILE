<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="provision.sim"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        
        
        <stripes:form action="/SIM.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">                
            <table class="clear">
                <tr>
                    <td><stripes:label for="newSIMCardData.integratedCircuitCardIdentifier"/>:</td>
                    <td><stripes:text  name="newSIMCardData.integratedCircuitCardIdentifier" class="required" size="20" maxlength="20" onkeyup="validate(this,'^[0-9]{20,20}$','luhn')"/></td>
                </tr> 
                <tr>
                    <td><stripes:label for="newSIMCardData.IMSI"/>:</td>
                    <td><stripes:text name="newSIMCardData.IMSI" class="required" size="15" maxlength="15" onkeyup="validate(this,'^[0-9]{15,15}$','')"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="newSIMCardData.unencryptedSecretKey"/>:</td>
                    <td><stripes:text name="newSIMCardData.unencryptedSecretKey" class="required" size="32" maxlength="32" onkeyup="validate(this,'^[0-9A-Fa-f]{32,32}$','')"/></td>
                </tr>
                 <tr>
                    <td><stripes:label for="newSIMCardData.unencryptedOperatorVariant"/>:</td>
                    <td><stripes:text name="newSIMCardData.unencryptedOperatorVariant" class="required" size="32" maxlength="32" onkeyup="validate(this,'^[0-9A-Fa-f]{32,32}$','emptyok')"/></td>
                </tr>
                <tr>  
                    <td>
                        <span class="button">
                            <stripes:submit name="provisionSIM"/> 
                        </span>    
                    </td>
                </tr>                                
            </table>            
        </stripes:form>
       
    </stripes:layout-component>
    
    
</stripes:layout-render>


