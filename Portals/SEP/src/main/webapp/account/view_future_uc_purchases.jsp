<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="future.uc.purchases"/>
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
                        <td colspan="2"><b>Schedule Future Unit Credit Purchases</b></td>
                    </tr>
                    <tr>
                        <td>Scheduled Date:</td>
                        <td>
                            <input readonly="true" type="text" id="futureDate"  name="event.date" class="required" size="10"/>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <table class="clear">                                
                                <c:forEach items="${actionBean.unitCreditSpecificationList.unitCreditSpecifications}" var="spec"> 
                                    <tr>
                                        <td>${spec.name}&nbsp;(${s:convertCentsToCurrencyLong(spec.priceInCents)})</td>
                                        <td><stripes:radio  name="unitCreditSpecIdToSchedule" value="${spec.unitCreditSpecificationId}"/></td>
                                    </tr>
                                </c:forEach>
                            </table>   
                        </td>
                    </tr>
                    <tr>
                        <td>Number Of Repeats:</td>
                        <td><stripes:text class="required" size="3" maxlength="3" name="repeats" value="0" onkeyup="validate(this,'^[0-9]{1,3}$','')"/>
                        &nbsp;<b>(Repeats are in addition to the first purchase. I.e. Total Purchases=Repeats + 1)</b></td>
                    </tr>	
                    <tr>
                        <td>Paying Account:</td>
                        <td><stripes:text class="required" size="10" maxlength="10" name="paidByAccountId" value="${actionBean.paidByAccountId}" onkeyup="validate(this,'^[0-9]{10,10}$','')"/></td>
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
                                <stripes:submit name="scheduleFutureUCPurchase"/>
                            </span>                        
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2"><b>Note that scheduled unit credit purchases happen at 5:05AM on the day in question.
                                <br/>Unit Credit price and validity may change between now and the purchase.</b></td>
                    </tr>
                </table>            
            </stripes:form>

            <br/>

            <table class="green" width="99%">
                <tr>
                    <th>Future Purchase Date</th>
                    <th>Unit Credit Name</th>
                    <th>Price</th>
                    <th>Usable Days</th>
                    <th>Units</th>
                    <th>Unit Type</th>
                    <th>Paying Account</th>
                    <th>Scheduled By</th>
                    <th>Delete</th>     
                </tr>

                <c:forEach items="${actionBean.eventList.events}" var="fe" varStatus="loop">
                    <c:set var="ucSpec" value="${s:getUnitCreditSpecification(fn:split(fe.eventData,'|')[1])}"/>
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>${s:formatDateLong(fe.date)}</td>
                        <td>${ucSpec.name}</td>     
                        <td>${s:convertCentsToCurrencyShort(ucSpec.priceInCents)}</td>
                        <td>${ucSpec.usableDays}</td>
                        <td>${s:formatBigNumber(ucSpec.units)}</td>
                        <td>${ucSpec.unitType}</td>
                        <td>${fn:split(fe.eventData,'|')[3]}</td>
                        <td>${fn:split(fe.eventData,'|')[2]}</td>
                        <td>
                            <stripes:form action="/Account.action">
                                <input type="hidden" name="eventToDelete" value="${fe.eventId}"/>
                                <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/>
                                <stripes:submit name="deleteFutureUCPurchase"/>                            
                            </stripes:form>
                        </td>
                    </tr>
                </c:forEach>
            </table>  

        </div>
    </stripes:layout-component>   
</stripes:layout-render>