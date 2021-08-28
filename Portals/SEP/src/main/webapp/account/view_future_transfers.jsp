<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="future.transfers"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="html_head">        
        <script type="text/javascript" xml:space="preserve">
            $j(document).ready(function(){
                $j("#futureDate").datepicker({dateFormat: 'yy/mm/dd', showOn: 'button', buttonText: "..", maxDate: null, changeYear: true, changeMonth: true});                
            });
            
            var submitClicked = false;
            
            //populateResultsText() is refered to as the call back function
            function populateResultsText(xmlhttp){
                var lblMsg = document.getElementById("lblToName");
                var rsl = xmlhttp.responseText;
                var notFound = rsl.search("ACCOUNT ID NOT FOUND");
                if(notFound == -1){
                    lblMsg.innerHTML=rsl;
                    lblMsg.setAttribute("style", "color:green"); 
                }else{
                    lblMsg.innerHTML=rsl;
                    lblMsg.setAttribute("style", "color:red");
                }
                
            }
            
            
            <%-- This method returns the XMLHttpRequest object --%>
                function getXMLHttprequest(){
                
                    try { 
                        return new XMLHttpRequest(); 
                    }
                    catch (ex) { 
                        try {  
                            return new ActiveXObject('Msxml2.XMLHTTP'); 
                        }
                        catch (ex1) { 
                            try { 
                                return new ActiveXObject('Microsoft.XMLHTTP'); 
                            }
                            catch(ex1) {       
                                return new ActiveXObject('Msxml2.XMLHTTP.4.0'); 
                            }
                        }
                    }
                
                }
            <%-- This method performs the HTTP GET method  --%>
                function getTheNameAndSurname(url, callbackFunction){
                    //call back function is the same as this: populateResultsText()
                    var xmlhttp = getXMLHttprequest();
                
                    xmlhttp.onreadystatechange=function(){
                        if (xmlhttp.readyState==4 && callbackFunction != null)
                        {
                            callbackFunction(xmlhttp);
                        }
                            
                    }
                    xmlhttp.open('GET',url,true,"","");
                    xmlhttp.send("");
                    return xmlhttp;
                
                }
            
                function invokeTheActionBeanMethod(form, dtValue, callbackFunction){
                    var regExp = new RegExp('^[0-9]{10,10}$','');
                    if(dtValue.valueOf().trim().length >= 10) {
                        if(dtValue.valueOf().search(regExp) == -1){
                            return
                        }
                        var url = form.action + '?' + 'getCustomerNameAsStream=&account.accountId='+dtValue;                
                        return getTheNameAndSurname(url, callbackFunction);
                    }
                }
                
                function lessThanTenTargetIdValidation(varTarget){
                    if(varTarget.valueOf() < 10 || varTarget.valueOf() > 10){
                        document.getElementById('lblToName').innerHTML='';
                    }                
                }
            
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="account"/>:
                        ${actionBean.account.accountId}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/Account.action">                                
                            <stripes:hidden name="accountQuery.accountId" value="${actionBean.account.accountId}"/>       
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveAccount"><fmt:message key="manage.account"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>

            <br/>


            <stripes:form action="/Account.action" focus="" name="frm" autocomplete="off">
                <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/>
                <table class="clear">
                    <tr>
                        <td colspan="2"><b>Schedule Future Transfer/s</b></td>
                    </tr>
                    <tr>
                        <td>Scheduled Date:</td>
                        <td>
                            <input readonly="true" type="text" id="futureDate" value="${s:formatDateShort(event.date)}" name="event.date" class="required" size="10"/>
                        </td>
                    </tr>
                    <tr>
                        <td>Destination Account Id:</td>
                        <td><input type="text" size="10" value="" class="required" maxlength="10" name="futureTransferDestinationAccountId" onkeyup="lessThanTenTargetIdValidation(this.value); validate(this,'^[0-9]{10,10}$',''); invokeTheActionBeanMethod(this.form, this.value, populateResultsText);" onchange="invokeTheActionBeanMethod(this.form, this.value, populateResultsText);"/>
                            &nbsp;<div id="lblToName"></div></td>
                        
                    </tr>	
                    <tr>
                        <td>
                            <fmt:message key="label.topup.own.amount">
                                <fmt:param value="${s:getProperty('env.locale.currency.majorunit')}"/>
                            </fmt:message>
                        </td>
                        <td><input type="text" name="majorCurrencyUnitsToTransferInFuture" class="required" maxlength="20" onkeyup="validate(this,'^[0-9]{1,20}\.[0-9]{2,2}$','')"/></td>
                    </tr>
                    <tr>
                        <td>Number Of Repeats:</td>
                        <td><stripes:text size="3" maxlength="3" name="repeats" value="0" class="required" onkeyup="validate(this,'^[0-9]{1,3}$','')"/>
                        &nbsp;<b>(Repeats are in addition to the first transfer. I.e. Total Transfers=Repeats + 1)</b></td>
                    </tr>	
                    <tr>
                        <td>Repeat Cycle:</td>
                        <td>
                            <stripes:select name="repeatCycle">
                                <stripes:option value="d">Daily</stripes:option>
                                <stripes:option value="w">Weekly</stripes:option>
                                <stripes:option value="t">30 Days</stripes:option>
                                <stripes:option value="m">Monthly</stripes:option>
                                <stripes:option value="y">Yearly</stripes:option>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="scheduleFutureAccountTransfer"/>
                            </span>                        
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2"><b>Note that scheduled transfers happen at 5AM on the day in question</b></td>
                    </tr>
                </table>            
            </stripes:form>

            <br/>

            <table class="green" width="99%">
                <tr>
                    <th>From</th>
                    <th>To</th>
                    <th>Transfer Date</th>
                    <th>Transfer Amount</th>
                    <th>Delete</th>
                </tr>

                <c:forEach items="${actionBean.eventList.events}" var="fe" varStatus="loop">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>${fe.eventKey}</td>
                        <td>
                            <stripes:link href="/Account.action" event="retrieveAccount"> 
                                <stripes:param name="accountQuery.accountId" value="${fn:split(fe.eventData,'|')[1]}"/>
                                ${fn:split(fe.eventData,'|')[1]}
                            </stripes:link>
                        </td>
                        <td>${s:formatDateLong(fe.date)}</td>
                        <td>${s:convertCentsToCurrencyLong(fn:split(fe.eventData,'|')[2])}</td>
                        <td>
                            <stripes:form action="/Account.action">
                                <input type="hidden" name="eventToDelete" value="${fe.eventId}"/>
                                <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/>
                                <stripes:submit name="deleteFutureAccountTransfer"/>                            
                            </stripes:form>
                        </td>

                    </tr>
                </c:forEach>
            </table>  

        </div>
    </stripes:layout-component>   
</stripes:layout-render>