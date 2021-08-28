<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="resend.strips"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">


        <stripes:form action="/Account.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">       
            <table class="clear">
                <tr>
                    <td><stripes:label for="prepaidStripBatchData.invoiceData"/>:</td>
                    <td><stripes:text name="prepaidStripBatchData.invoiceData" class="required" size="15" maxlength="15" onkeyup="validate(this,'^[0-9]{1,15}$','emptynotok')"/></td>
                </tr>
                <tr>  
                    <td>
                        <span class="button">
                            <stripes:submit name="resendStrips"/> 
                        </span>    
                    </td>
                </tr>                                
            </table>            
        </stripes:form>

        <br/>


        <br/>


    </stripes:layout-component>


</stripes:layout-render>


