<%@ include file="/include/scp_include.jsp" %>
<c:set var="title">
    <fmt:message key="onetimepin"/>
</c:set>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-1.8.3.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/prototype.js"></script>
<script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>

<script type="text/javascript" xml:space="preserve">
    
    
    
    function invoke(form, event, container) {
        
        if (!form.onsubmit) { form.onsubmit = function() { return false } };
        
        var formValues= $(form).serializeArray(form,{submit: event});
        formValues.push({name: event, value: event});
        
        $.post(form.action, $.param(formValues), function(data){           
            if(data==="confirmed") {
                document.getElementById("result").innerHTML='';
                document.getElementById("result").style.display="none";                                        
                document.getElementById("getOtp").style.display="none";
                document.getElementById("confirmedOtp").style.display="block";
                document.getElementById("result").style.display="none";

            } else {
                document.getElementById("getOtp").style.display="block";
                document.getElementById("confirmedOtp").style.display="none";
                document.getElementById("result").innerHTML=data;
            }
        });
        
    }
   
</script>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <div style="margin-top: 10px;" class="sixteen columns alpha">
        <br/>

        <c:if test="${!actionBean.areValidationErrors}">
             <%--<script type="text/javascript">
                document.body.style.background = '#FF0000';
            </script> --%>
             <cennter>
            <div id='result' style="padding:15px"></div>    
                        
            <div id='confirmedOtp' name='confirmedOtp' hidden="true" style="border: 1px solid #eee; border-radius: 10px; padding: 10px; width: 450px">                
                 <table style="margin: auto;">
                    <tr>                    
                        <td>
                            <fmt:message key="accountactionbean.otpverified">
                                <c:if test="${s:getListSize(actionBean.confirmationMessageParams) > 0}">
                                    <c:forEach items="${actionBean.confirmationMessageParams}" var="params">
                                        <fmt:param>${params}</fmt:param>
                                    </c:forEach>
                                </c:if>                               
                            </fmt:message>                            
                        </td>
                    </tr>
                     <tr>
                        <td>
                            <br class="clear" />
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <br class="clear" />
                        </td>
                    </tr>
                    <tr>   
                       
                        <td>
                            <stripes:form action="${actionBean.postConfirmationAction}">
                                <input style="border:1px green solid; background-color: transparent;padding:5px;border-radius: 10px" type="button" value='<fmt:message key="scp.noresponse.${actionBean.postConfirmationSubmit}"/>' onclick="previousPreviousPage();" />
                                <input style="border:1px green solid; background-color: transparent;padding:5px;border-radius: 10px" type="submit" name="${actionBean.postConfirmationSubmit}" value='<fmt:message key="scp.yesresponse.${actionBean.postConfirmationSubmit}"/>' />
                                <input type="hidden" name="confirmed" value="true"/>
                                <stripes:wizard-fields/>
                            </stripes:form>
                        </td>
                    </tr>                
                </table>
            </div>
            
            <div id='getOtp' name='getOtp' style="align-content: center; align-self: center;align-items: center; border: 1px solid #eee; border-radius: 10px; padding:10px; width: 450px">
                <stripes:form action="/OTP.action">     
                    <table style="margin: auto;" border="1px" width="fit-content    ">
                        <tr>                    
                            <td style="font-size:12px"><fmt:message key="scp.${actionBean.confirmationMessageKey}" >
                                    <c:forEach items="${actionBean.confirmationMessageParams}" var="msgparam">
                                        <fmt:param>${msgparam}</fmt:param>
                                    </c:forEach>
                                </fmt:message>
                            <br/><br/>
                            </td>

                        </tr>

                        <tr style="padding-bottom: 15px">
                            <td style="padding-bottom: 15px">
                                <input  type='text' id="otp" name="otp" value="" maxlength="15" size='25' onkeyup="validate(this,'^.{4,15}$','emptynotok')" /><br/>
                            </td>
                        </tr>

                        <tr>
                           <td align="left">
                               <input style="border:1px green solid; background-color: transparent;padding:5px;border-radius: 10px" type="button" value='<fmt:message key="scp.noresponse.${actionBean.postConfirmationSubmit}"/>' onclick="previousPreviousPage();" />
                               <input style="border:1px green solid; background-color: transparent;padding:5px;border-radius: 10px" type="submit" name="requestAnotherOtp" value='<fmt:message key="requestAnotherOtp"/>' onclick="invoke(this.form, 'requestAnotherOtp', 'result');" />
                               <input style="border:1px green solid; background-color: transparent;padding:5px; border-radius: 10px" type="submit" name="submitOTP" value='<fmt:message key="submitOTP"/>' onclick="invoke(this.form, 'submitOTP', 'result');" />                               

                            </td>                        
                        </tr>
                    </table>    
                </stripes:form>  
            </div> 
           </cennter>                        
        </c:if>
        <c:if test="${actionBean.areValidationErrors}">
                <div>
                    <input class="button_gateway_go_back" type="button" value='<fmt:message key="back"/>' onclick="previousPreviousPage();" />
                </div>
            </c:if>
        </div>      
    </stripes:layout-component>
</stripes:layout-render>
            
            

