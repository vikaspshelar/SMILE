<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="bulk.sms"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <stripes:form action="/Customer.action" focus="" onsubmit="return alertValidationErrors();">   
            <table class="clear" width="99%">

                <tr>
                    <td>Campaign Name:</td>
                    <td><stripes:text  name="campaignName" size="30" maxlength="100" onkeyup="validate(this,'^.{2,100}$','emptynotok')"/></td>
                </tr>
                <tr>
                    <td>From:</td>
                    <td>
                <stripes:select name="from">
                    <c:forEach items="${s:getRoleBasedSubsetOfPropertyAsSet('env.customercare.bulk.sms.from', actionBean.context.request)}" var="fm"  varStatus="loop">
                        <stripes:option value="${fm}">${fm}</stripes:option>
                    </c:forEach>
                </stripes:select>                        
                </td>
                </tr>    
                <tr>
                    <td>Bulk SMS Group:</td>
                    <td>
                <stripes:select name="generalQueryRequest.queryName">
                    <stripes:option value="Just Me">Just Me</stripes:option>
                    <c:forEach items="${s:getRoleBasedSubsetOfPropertyAsSet('env.pm.generalquery.bulk.sms.names', actionBean.context.request)}" var="query"  varStatus="loop">
                        <stripes:option value="${query}">${query}</stripes:option>
                    </c:forEach>
                </stripes:select>                        
                </td>
                </tr>   
                <tr>
                    <td></td>
                    <td>OR</td>
                </tr>  
                <tr>
                    <td>Bulk SMS List:<br/><br/>
                        <b>Paste list of phone numbers to SMS. One number per line.</b></td>
                    <td>
                <stripes:textarea name="smsList" cols="55" rows="10"></stripes:textarea>
                </td>
                </tr>  
                <tr>
                    <td>Bulk SMS Body:<br/><br/>
                        <b>For SMS groups, {1} will be replaced with the customers first name</b></td>
                    <td>
                <stripes:textarea name="body" rows="10" cols="55" maxlength="${s:getPropertyWithDefault('env.sep.bulksms.maxlength', 480)}" onkeyup="textCounter(this, 'charCount',${s:getPropertyWithDefault('env.sep.bulksms.maxlength', 480)});"></stripes:textarea>
                </td>
                </tr>
                <br/>
                <tr>
                    <td></td>
                    <td>
                        <p id="charCount"></p>
                    </td>
                </tr> 

                <script>
                    function textCounter(field, charCount, maxlimit)
                    {
                        var charCount = document.getElementById(charCount);

                        if (field.value.length > maxlimit) {
                            field.value = field.value.substring(0, maxlimit);
                            return false;
                        } else {
                            charCount.innerHTML = "" + maxlimit - field.value.length + " characters remaining of maximum " + maxlimit + " - SMS count " + Math.ceil((field.value.length / 160)) + ".";
                        }
                    }
                </script>

                <tr>
                    <td></td>
                    <td>
                <stripes:submit name="sendBulkSMS"/>
                </td>
                </tr>   
                <tr>
                    <td><br/><br/></td>
                    <td></td>
                </tr> 
                <tr>
                    <td>Current Bulk SMS Delivery Status: </td>
                    <td>
                        ${actionBean.bulkSMSStatus} &nbsp;
                <stripes:link href="/Customer.action" event="showSendBulkSMS">
                    <img src="images/refresh.png" width="25" height="25"/>
                </stripes:link>
                </td>
                </tr>   
            </table>  
        </stripes:form>
    </stripes:layout-component>   
</stripes:layout-render>