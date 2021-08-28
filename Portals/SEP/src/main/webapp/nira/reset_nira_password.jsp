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
                        Change NIRA Password
                    </td>                        
                </tr>
            </table>         

            <c:if test="${actionBean.niraNewPassword == null}">
                <stripes:form action="/Customer.action" focus="">    
                    <table class="clear"> 
                        <tr>
                            <td colspan="2">
                             NIRA Password Policy:<br/>  
                             <ul>
                                    <li>At least 6 characters, maximum 10.</li>
                                    <li>A digit 0-9 must occur at least once.</li>
                                    <li>A lower case letter must occur at least once.</li>
                                    <li>An upper case letter must occur at least once.</li>
                                    <li>A special character must occur at least once.</li>
                                    <li>No whitespace allowed in the entire string.</li>
                             </ul>
                            </td>
                        </tr>
                        <tr>
                            <td><stripes:label for="new.nira.password"/>:</td>
                            <td><stripes:password name="niraNewPassword" maxlength="20" class="required" size="20" /></td>
                        </tr>
                        <tr>
                            <td colspan="2">
                                <span class="button">
                                    <stripes:submit name="doChangeNiraPassword"/>
                                </span>                        
                            </td>
                        </tr>                                 
                    </table> 
                </stripes:form>
            </c:if>
            
            <c:if test="${actionBean.niraPasswordChangeResponse != null}">
                <table class="clear"> 
                    <tr>
                        <td>
                            NIRA Change Password Response:<br/> 
                            ${actionBean.niraPasswordChangeResponse}
                        </td>
                    </tr>
                </table>
            </c:if>
            

        </div>

    </stripes:layout-component>    
</stripes:layout-render>

