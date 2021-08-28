<%@ include file="/include/sep_include.jsp" %>
<c:set var="title">
    <fmt:message key="onetimepin"/>
</c:set>
<script type="text/javascript" xml:space="preserve">
    
    function invoke(form, event, container) {
        
        if (!form.onsubmit) { form.onsubmit = function() { return false } };
        var params = Form.serialize(form, {submit:event});
        
        console.log(JSON.stringify(params));
        new Ajax.Request(form.action,
                            {method:'post',
                                parameters:params,
                                beforeSend: function () 
                                      {
                                          document.getElementById('result').innerHTML= "Please wait...";
                                          
                                      },
                                onSuccess:function(xmlHttpRequest) { 
                                    if(xmlHttpRequest.responseText==="confirmed") {
                                        document.getElementById("result").innerHTML='';
                                        document.getElementById("result").style.display="none";                                        
                                        document.getElementById("getOtp").style.display="none";
                                        document.getElementById("confirmedOtp").style.display="block";
                                        document.getElementById("result").style.display="none";
                                        
                                    } else {
                                        
                                        document.getElementById("getOtp").style.display="block";
                                        document.getElementById("confirmedOtp").style.display="none";
                                        document.getElementById("result").innerHTML=xmlHttpRequest.responseText;
                                    }
                                }
                          }
                        );                
    }
   
</script>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <br/>

        <c:if test="${!actionBean.areValidationErrors}">
            <script type="text/javascript">
                document.body.style.background = '#FF0000';
            </script> 
            
            <div id='result' style="padding:15px"></div>                    
            
            <div id='confirmedOtp' name='confirmedOtp' hidden="true" style="border: 1 solid #eee; border-radius: 10px; padding: 10px; width: 450px">                
                <table class="clear">
                    <tr>                    
                        <td colspan="2">                            
                            <fmt:message key="accountactionbean.otpverified">
                                <c:forEach items="${actionBean.confirmationMessageParams}" var="msgparam">
                                    <fmt:param>${msgparam}</fmt:param>
                                </c:forEach>
                            </fmt:message>
                        </td>
                    </tr>
                    <tr>
                        <td align="center">                            
                            <stripes:form action="${actionBean.postConfirmationAction}">                                
                                <stripes:wizard-fields/>
                                <input type="button" value="<fmt:message key="cancel"/>" onclick="previousPage();" />
                                <stripes:submit name="${actionBean.postConfirmationSubmit}"/>
                            </stripes:form>
                        </td>
                    </tr>
                </table>
            </div>
            
            <div id='getOtp' name='getOtp' style="border: 1 solid #eee; border-radius: 10px; padding:10px; width: 450px">
                <stripes:form name="frmOtp" id="frmOtp"  action="/OTP.action">     
                    <table class="clear">
                        <tr>                    
                            <td style="font-size:12px"><fmt:message key="sep.${actionBean.confirmationMessageKey}" >
                                    <c:forEach items="${actionBean.confirmationMessageParams}" var="msgparam">
                                        <fmt:param>${msgparam}</fmt:param>
                                    </c:forEach>
                                </fmt:message>
                            <br/><br/>
                            </td>

                        </tr>

                        <tr>
                            <td>
                                <input type='text' id="otp" name="otp" value="" maxlength="15" size='25' onkeyup="validate(this,'^.{4,15}$','emptynotok')" />
                            </td>
                        </tr>

                        <tr>
                           <td align="left">
                                <input type="button" value="cancel" onclick="previousPage();" />                  
                                    <stripes:wizard-fields/>
                                    <stripes:submit  name='requestAnotherOtp' onclick="invoke(this.form, 'requestAnotherOtp', 'result');"/>                             
                                    <stripes:submit  name='submitOTP' onclick="invoke(this.form, 'submitOTP', 'result');"/>

                            </td>                        
                        </tr>
                    </table>    
                </stripes:form>  
            </div> 
                                 
        </c:if>
        <c:if test="${actionBean.areValidationErrors}">
            <input type="button" value="cancel" onclick="previousPreviousPage();" />
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
            
            

