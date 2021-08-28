<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="next.steps.after.adding"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="customer"/> ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/Customer.action">                                
                            <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>                    
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveCustomer"><fmt:message key="manage.customer"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>   

            <br/>

            <table class="green" width="99%">                         
                <tr>
                    <td colspan="2">
                        <b>Now that the customers details are registered in the system, the next steps may be different based on the type of sale/installation/provisioning intended.
                            Please select the most applicable option</b>
                    </td>
                </tr>
                <tr>
                    <td>
                        <b>Smile@Business or Smile@Us</b>
                        <br/>If this customer intends on purchasing a Smile product in the Smile@Business or Smile@Us segment and they belong to an Organisation that has not been created yet.
                        Your next step is to add the organisation and then link the customer to the organisation.
                        <br/><br/> After adding the Organisation and linking the customer to it:
                        <br/>- If the customer wants to buy Airtime in the initial sale, then after adding them to the organisation, provision the "Corporate Airtime Account" product so that they have an account number into 
                        which the Airtime will be transferred. 
                        <br/>- If they don't need Airtime just yet, then after adding them to the organisation,  make the sale.
                        <br/><br/> After making the Sale:
                        <br/>- If they pay cash, then you can provision the required product onto the SIM(s) that were sold. 
                        <br/>- If they pay via bank transfer/cheque, then only once Finance has received the money in the bank, will you be notified to deliver the device(s) and SIM(s) and provision them.
                    </td>
                    <td style="text-align: center">
                        <stripes:form action="/Customer.action" focus="">    
                            <span class="button">
                                <stripes:submit name="showAddOrganisationWizard"/>
                            </span>      
                        </stripes:form>
                    </td>
                </tr>     
                <tr>
                    <td>
                        <b>Smile@Home or Smile@Me</b>
                        <br/>If this customer intends on purchasing a Smile product in the Smile@Home or Smile@Me segment. 
                        Your next step is to make a sale and then provision the required product onto the SIM(s) that were sold. After that, you can sell them Airtime as needed.
                    </td>
                    <td style="text-align: center">
                        <stripes:form action="/Customer.action" focus="">    
                            <span class="button">
                                <stripes:submit name="showMakeSale"/>
                            </span>      
                            <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>                    
                        </stripes:form>
                    </td>
                </tr>   
                <tr>
                    <td>
                        <b>Trial</b><br/>
                        If this customer would like a 1 week trial. 
                        Your next step is to provision the Trial product onto a SIM and set this up on a trial router
                    </td>
                    <td style="text-align: center">
                        <stripes:form action="/Customer.action" focus="">    
                            <span class="button">
                                <stripes:submit name="showAddProductWizard"/>
                            </span>      
                            <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>                    
                        </stripes:form>
                    </td>
                </tr> 
                <tr>
                    <td>
                        <b>Staff</b><br/>
                        If this customer is a Smile Staff member. Your next step is to link them to Smile as an Organisation and then provision the Staff Product/ Sales Product or other products accordingly
                    </td>
                    <td style="text-align: center">
                        <stripes:form action="/Customer.action" focus="">    
                            <span class="button">
                                <stripes:submit name="showManageCustomerRoles"/>
                            </span>      
                            <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>                    
                        </stripes:form>
                    </td>
                </tr> 
            </table>  




        </div>

    </stripes:layout-component>    
</stripes:layout-render>

