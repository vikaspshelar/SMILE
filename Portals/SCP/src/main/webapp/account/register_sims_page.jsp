<%@include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="my.simcards"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}"> 
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function () {
            makeMenuActive('Profile_SimVerifyPage');
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <div style="margin-top: 10px;" class="sixteen columns alpha">
            
        <br/>      

           
        <stripes:form action="/Account.action" focus="" autocomplete="off">   
           
            <c:if test="${fn:length(actionBean.regulatorResponse)>0}">
                <span style="font-weight: 900; color: cadetblue; border: 1px solid darkgreen; padding: 10px; border-radius: 5px">${actionBean.regulatorResponse}</span>
                <br><br>
            </c:if>
            
            <table class="clear">
                <tr>
                    <td>
                        <stripes:label for="customer.national.id.number"/>:
                    </td>
                    <td>
                        <stripes:text name="customerNIN" required="true" readonly="false" id="customerMSISDN" size="30" onkeyup="validate(this,'^.{5,50}$','emptyok')"  />
                        <span class="button">
                            <stripes:submit name="fetchNINData"/>
                        </span>
                    </td>
                </tr>
                
                <c:if test="${actionBean.ownNumbers.size() >0 || actionBean.availableNumbers.size() >0}"><br><br>
                    <div id="mainData">
                        <tr>
                            <td>
                                <stripes:label for="customer.msisdn"/>:
                            </td>
                            <td>

                                    <stripes:text name="customerMSISDN" placeholder="e.g 2556621234567" readonly="false" id="customerMSISDN" size="30" onkeyup="validate(this,'^.{5,50}$','emptyok')"  />                                       

                            </td>
                        </tr>
                        <tr>
                            <td>
                                <stripes:label for="customer.iccid"/>:
                            </td>
                            <td>
                                <stripes:text name="iccid" required="true" readonly="false" id="iccid" size="30" onkeyup="validate(this,'^[0-9]{20,20}$','luhn_emptyok')"  />
                            </td>
                        </tr>


                        <tr>
                                <td><stripes:label for="registration.category" />:</td>
                                <td>                            
                                        <stripes:select name="simRegistrationCategory" onchange="loadCategory(this)">
                                            <stripes:option value="2000">Individual</stripes:option>
                                            <stripes:option value="2010">Corporate</stripes:option>
                                            <stripes:option value="2020">Foreigner</stripes:option>
                                            <stripes:option value="2030">Visitor</stripes:option>
                                            <stripes:option value="2040">Minor</stripes:option>
                                            <stripes:option value="2050">Diplomat</stripes:option>
                                            <stripes:option value="2060">Defaced</stripes:option>
                                        </stripes:select>
                                </td>
                        </tr>  

                           <tr>
                                <td><stripes:label for="additional.sim.reason"/>:</td>
                                <td>
                                    <div id="div1">
                                        <stripes:select name="addSimReasonCode" >
                                            <stripes:option value="1000">For additional devices (phones, tablets, CCTV, routers etc.)</stripes:option>
                                            <stripes:option value="1001">To separate office and private usage</stripes:option>
                                            <stripes:option value="1002">To separate business and personal usage</stripes:option>
                                            <stripes:option value="1003">For mobile financial services.</stripes:option>
                                            <stripes:option value="1004">Mobile number porting with reasons</stripes:option>
                                            <stripes:option value="1005">Increase branches/shops or business</stripes:option>                                    
                                        </stripes:select>
                                    </div>
                                    <div id="div2" hidden>
                                        <stripes:select name="addSimReasonCode" >
                                            <stripes:option value="1003">For mobile financial services.</stripes:option>
                                            <stripes:option value="1004">Mobile number porting with reasons</stripes:option>
                                            <stripes:option value="1005">Increase branches/shops or business</stripes:option>
                                            <stripes:option value="1006">Increase staff/employee</stripes:option>
                                            <stripes:option value="1007">Test numbers for compliance purposes</stripes:option>
                                            <stripes:option value="1008">Test numbers for roaming partners</stripes:option>
                                        </stripes:select>
                                    </div>
                                </td>
                            </tr>                                    
                        <tr><td></td>
                            <td><br><br>
                                <span class="button">
                                    <stripes:submit name="verifySimRegistration"/>
                                </span>
                            </td>
                        </tr>
                        </div> 
                      </c:if>
            </table>            
        </stripes:form>
        </div>
    </stripes:layout-component>
    
    
</stripes:layout-render>