<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="customer.email"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <stripes:form action="/Customer.action" focus="" onsubmit="return alertValidationErrors();">  
            <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
            <table class="clear" width="99%">
                <tr>
                    <td>From:</td>
                    <td>
                        ${s:getProperty('env.customercare.email.address')}                    
                    </td>
                </tr> 
                 <tr>
                    <td>To:</td>
                    <td>
                        ${actionBean.customer.emailAddress}                    
                    </td>
                </tr> 
                <tr>
                    <td>Subject:</td>
                    <td>
                        <stripes:text name="subject"  maxlength="100" size="70" onkeyup="validate(this,'^.{1,100}$','emptynotok')" />                    
                    </td>
                </tr>        
                <tr>
                    <td>Body:</td>
                    <td>
                        <stripes:textarea name="body" cols="80" rows="20"/>                   
                    </td>
                </tr>     
                <tr>
                    <td></td>
                    <td>
                        <stripes:submit name="sendCustomerEmail"/>
                    </td>
                </tr>                   
            </table>  
        </stripes:form>
    </stripes:layout-component>   
</stripes:layout-render>