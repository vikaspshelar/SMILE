<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="create.strips"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">


        <stripes:form action="/Account.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">       
            <table class="clear">
                <tr>
                    <td>
                        <fmt:message key="strip.amount">
                            <fmt:param value="${s:getProperty('env.locale.currency.majorunit')}"/>
                        </fmt:message>:
                    </td>
                    <td><stripes:text name="newPrepaidStripsData.valueInCents" class="required" size="10" maxlength="10" onkeyup="validate(this,'^[0-9]{1,15}$','')"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="expiry.date"/>:</td>
                    <td>
                        <input readonly="true" type="text" value="${s:formatDateShort(actionBean.newPrepaidStripsData.expiryDate)}" name="newPrepaidStripsData.expiryDate"  size="10"/>
                        <input name="datePicker1" type="button" value=".." onclick="displayCalendar(document.forms[0].elements['newPrepaidStripsData.expiryDate'] ,'yyyy/mm/dd',this)">
                    </td>
                </tr>
                <tr>
                    <td><stripes:label for="newPrepaidStripsData.numberOfStrips"/>:</td>
                    <td><stripes:text name="newPrepaidStripsData.numberOfStrips" class="required" size="10" maxlength="6" onkeyup="validate(this,'^[0-9]{1,6}$','')"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="newPrepaidStripsData.unitCreditSpecificationName"/>:</td>
                    <td><stripes:text name="newPrepaidStripsData.unitCreditSpecificationId" class="required" size="10" maxlength="6"/></td>
                </tr>
                <tr>  
                    <td>
                        <span class="button">
                            <stripes:submit name="createStrips"/> 
                        </span>    
                    </td>
                </tr>                                
            </table>            
        </stripes:form>

    </stripes:layout-component>


</stripes:layout-render>


