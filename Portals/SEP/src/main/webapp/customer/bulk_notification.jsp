<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="bulk.sms"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <stripes:form action="/Customer.action" focus="" onsubmit="return alertValidationErrors();">   
            <table class="clear" width="99%">

                <tr>
                    <td>Notification Title:</td>
                    <td><stripes:text  name="notificationTitle" size="30" maxlength="100" onkeyup="validate(this,'^.{2,100}$','emptynotok')"/></td>
                </tr>
                <tr>
                    <td>Notification Type:</td>
                    <td>
                       <stripes:select name="notificationType">
                       <stripes:option value="bundle.monthly">Monthly Bundle</stripes:option>
                       <stripes:option value="promo.unlimited">Booster Promotion</stripes:option>
                       </stripes:select> 
                    </td>
                </tr>
                
                <tr>
                    <td>Image link:</td>
                    <td><stripes:text  name="notificationImage" size="30" maxlength="300"/></td>
                </tr>
                  
                <tr>
                    <td>Bulk Notification Group:</td>
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
                    <td>Customer's List:<br/><br/>
                        <b>Paste list of customer IDs. One ID per line.</b></td>
                <td>
                <stripes:textarea name="customerList" cols="55" rows="10"></stripes:textarea>
                </td>
                </tr>  
                <tr>
                    <td>Notification Body:<br/><br/>
                        <b>For Notification groups, {1} will be replaced with the customers first name</b></td>
                    <td>
                <stripes:textarea name="notificationBody" rows="10" cols="55" maxlength="${s:getPropertyWithDefault('env.sep.bulksms.maxlength', 480)}" onkeyup="textCounter(this, 'charCount',${s:getPropertyWithDefault('env.sep.bulksms.maxlength', 480)});"></stripes:textarea>
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
                <stripes:submit name="sendBulkNotification"/>
                </td>
                </tr>   
                <tr>
                    <td><br/><br/></td>
                    <td></td>
                </tr> 
                <tr>
                    <td>Current Bulk Notification Delivery Status: </td>
                    <td>
                        ${actionBean.bulkNotificationStatus} &nbsp;
                <stripes:link href="/Customer.action" event="showSendBulkNotification">
                    <img src="images/refresh.png" width="25" height="25"/>
                </stripes:link>
                </td>
                </tr>   
            </table>  
        </stripes:form>
    </stripes:layout-component>   
</stripes:layout-render>