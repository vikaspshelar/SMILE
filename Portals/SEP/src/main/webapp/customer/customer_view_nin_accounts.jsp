<%-- 
    Document   : organisation_legal_contact
    Created on : 29 Mar, 2021, 3:44:37 PM
    Author     : user
--%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    Accounts per NIN
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">        
        <stripes:form action="/Customer.action" autocomplete="off" onsubmit="alertValidationErrors()">
            <div id="entity">              
                <table  class="clear">  
                    <tbody>                                              
                        <tr>
                            <td>Enter NIN </td>
                        <c:set var="defaultRule" value="validate(this,'^.{2,50}$','emptynotok')"/>
                        <td><input type='text' onkeypress="return isNumberKey(event)" id="customer.nationalIdentityNumber" name="customer.nationalIdentityNumber" maxlength='11' size='20' value="" /></td>
                        <td style='vertical-align: top; margin-top:auto;'>                                                       
                                 <stripes:submit name="retrieveNinAccounts"/>
                        </td>
                        </tr>

                     </tbody>    
                </table> 
                        
             <c:if test="${actionBean.ninAccountData.size()>0}">
                 <h3>Found ${actionBean.ninAccountData.size()} accounts for nin</h3>         
                 <table class="green" width="99%">
                        <tr>
                            <th align="center">NIN</th>                                   
                           <th align="center">Customer Id</th>
                            <th align="center">Account No</th>                                   
                            <th align="center">Account Type</th>                                   
                            <th align="center">Status</th>                    

                        </tr>
                        
                        <c:forEach items="${actionBean.ninAccountData}" var="acc" varStatus="loop">
                            <tr>
                            <td align="center">
                                ${acc.nin}
                            </td>  
                            <td align="center">
                                <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                    <stripes:param name="customerQuery.customerId" value="${acc.customerId}"/>
                                        ${acc.customerId}
                                    </stripes:link>
                            </td>
                            <td align="center">
                                <stripes:link href="/Account.action" event="retrieveAccount">
                                    <stripes:param name="accountQuery.accountId" value="${acc.accountId}"/>
                                    ${acc.accountId}
                                </stripes:link>
                            </td> 
                            <td align="center">                                
                                <c:if test="${acc.accountOrg>0}">
                                    ${acc.accountType}
                                    <stripes:link href="/Customer.action" event="retrieveOrganisation">
                                    <stripes:param name="organisationQuery.organisationId" value="${acc.accountOrg}"/>
                                        View
                                    </stripes:link>
                                </c:if>
                                <c:if test="${acc.accountOrg==0}">
                                    <span style="font-weight: 500">${acc.accountType}</span>
                                </c:if>
                            </td>  
                            <td align="center">
                                
                                ${acc.status}
                            </td>    
                        </tr>                    
                        </c:forEach> 

                    </table>
             
             <br />
              <hr />
             </c:if> 

            </div>
     </center>              
        </stripes:form>    
        </stripes:layout-component>
        </stripes:layout-render>
