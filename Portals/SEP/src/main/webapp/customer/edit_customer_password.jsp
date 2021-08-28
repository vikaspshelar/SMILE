<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="edit.customer.password"/>
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

            <stripes:form action="/Customer.action" focus="">    
                <stripes:hidden name="customer.version" value="${actionBean.customer.version}"/>
                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>                    
                <table class="clear">                                
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="sendPasswordResetLink"/>
                            </span>                        
                        </td>
                    </tr>                                 
                </table>                
            </stripes:form>
                    
            <stripes:form action="/Customer.action" focus="">    
                <stripes:hidden name="customer.version" value="${actionBean.customer.version}"/>
                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>                    
                <table class="clear">       
                    <tr>
                        <td colspan="2">or:</td>
                    </tr>
                    <tr>
                        <td><stripes:label for="customer.SSODigest"/>:</td>
                        <td><stripes:password name="customer.SSODigest" maxlength="20" class="required" size="20" /></td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="updateCustomerPassword"/>
                            </span>                        
                        </td>
                    </tr>                                 
                </table>                
            </stripes:form>

        </div>

    </stripes:layout-component>    
</stripes:layout-render>

