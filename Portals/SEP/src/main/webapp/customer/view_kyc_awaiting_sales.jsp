<%-- 
    Document   : view_consent_form
    Created on : 26 Feb, 2021, 7:14:06 PM
    Author     : user
--%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    Sales/Products KYC Verification 
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/Customer.action">
            <table class="green" width="99%">
                <tr>
                   <th align="center">Customer Id</th>
                    <th align="center">NIN</th>                                   
                    <th align="center">Sales Person</th>                    
                    
                </tr>
                <c:forEach items="${actionBean.kycProductslist}" var="product" varStatus="loop">
                    <tr>
                    <td align="center">
                        <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                            <stripes:param name="customerQuery.customerId" value="${product.customerId}"/>
                            ${product.customerId}
                        </stripes:link>
                    </td>
                    <td align="center">
                        ${product.nin}
                    </td>   
                    <td align="center">
                        <c:choose>
                            <c:when test="${product.salesPerson==null || product.salesPerson.isEmpty()}">
                                     ${product.salesPerson}
                            </c:when>
                            <c:otherwise>
                                <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                                    <stripes:param name="customerQuery.customerId" value="${product.salesPerson}"/>
                                    ${product.salesPerson}
                                </stripes:link>                       
                            </c:otherwise>
                        </c:choose>
                    </td>    
                                
                    <td align="left">
                        <stripes:form action="/Customer.action">
                            <input type="hidden" name="verifyAction" id="verifyAction" value="true"/>
                            <input type="hidden" name="customer.customerId" value="${product.customerId}"/>
                            <input type="hidden" name="customer.nationalIdentityNumber" value="${product.nin}"/>
                            <input type="hidden" name="salesPerson" id="salesPerson" value="${product.salesPerson}"/>
                            <stripes:submit name="processSaleKycProductActivation"/>                           
                            
                            <input type="button" onclick="javascript:document.getElementById('sendMsg${product.salesPerson}').style.display='block'" value='Contact SalesPerson'/>
                            <div id="sendMsg${product.salesPerson}" name="sendMsg${product.salesPerson}" style="display: none">
                                <textarea id="msgbody" name="msgbody" placeholder="Enter messge to SalesPerson here..." style="font-size:10.5px" rows="10" cols="30">                                        

                                </textarea>          <br/>                      
                                <stripes:submit name="sendSalesPersonMessage"/>
                            </div>
                            
                        </stripes:form>                                        
                    </td>
                </tr>                    
                </c:forEach>

            </table>
        </stripes:form>

    </stripes:layout-component>    
</stripes:layout-render>