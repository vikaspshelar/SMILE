<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="sale.basic.data"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 

        <table class="clear">
            
            
                
            <tr>
                <td colspan="2">
                    <br/>
                    <b><fmt:message key="new.or.anonymous.customer"/></b>
                </td>
            </tr>
           
            <stripes:form action="/Sales.action" focus="">   
                <tr>
                    <td><fmt:message key="new.customer"/>:</td>
                    <td colspan="2">
                        <span class="button">
                            <stripes:submit name="addNewCustomer"/>
                        </span>
                    </td>
                </tr>
            </stripes:form>

            

            <stripes:form action="/Sales.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">    
                <tr>
                    <td colspan="2">
                        <br/>
                        <b><fmt:message key="existing.customer"/></b>
                    </td>
                </tr>
                <tr>
                    <td><fmt:message key="id"/>:</td>
                    <td><stripes:text  name="customerQuery.customerId" size="30" onkeyup="validate(this,'^[0-9]{1,8}$','emptyok')"/></td>
                    <td><fmt:message key="andor"/></td>
                </tr>
                <tr>
                    <td><fmt:message key="identity.search"/>:</td>
                    <td><stripes:text name="customerQuery.SSOIdentity" size="30" onkeyup="validate(this,'^.{1,50}$','emptyok')"/></td>
                    <td><fmt:message key="andor"/></td>
                </tr>
                <tr>
                    <td><fmt:message key="last.name"/>:</td>
                    <td><stripes:text name="customerQuery.lastName" size="30" onkeyup="validate(this,'^.{2,50}$','emptyok')"/></td>
                    <td><fmt:message key="andor"/></td>
                </tr>
                <tr>
                    <td><fmt:message key="first.name"/>:</td>
                    <td><stripes:text name="customerQuery.firstName" size="30" onkeyup="validate(this,'^.{2,50}$','emptyok')"/></td>
                    <td><fmt:message key="andor"/></td>
                </tr>
                <tr>
                    <td><fmt:message key="organisation.name"/>:</td>
                    <td><stripes:text name="customerQuery.organisationName" size="30" onkeyup="validate(this,'^.{2,50}$','emptyok')"/></td>
                    <td><fmt:message key="andor"/></td>
                </tr>
                <tr>
                    <td><fmt:message key="id.number"/>:</td>
                    <td><stripes:text name="customerQuery.identityNumber" size="30" onkeyup="validate(this,'^.{5,50}$','emptyok')"/></td>
                    <td><fmt:message key="andor"/></td>
                </tr>
                <tr>
                    <td><fmt:message key="phone.number"/>:</td>
                    <td><stripes:text name="serviceInstanceQuery.identifier" size="30" onkeyup="validate(this,'^.{10,15}$','emptyok')"/></td>
                    <td><fmt:message key="andor"/></td>
                </tr>
                <tr>
                    <td><fmt:message key="iccid"/>:</td>
                    <td><stripes:text name="IMSSubscriptionQuery.integratedCircuitCardIdentifier" size="30" onkeyup="validate(this,'^[0-9]{20,20}$','luhn_emptyok')"/></td>
                    <td><fmt:message key="andor"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="IMSI"/>:</td>
                    <td><stripes:text name="IMSI" size="30" maxlength="15" onkeyup="validate(this,'^[0-9]{15,15}$','emptyok')"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="result.limit"/>:</td>
                    <td>
                        <stripes:select name="customerQuery.resultLimit">
                            <stripes:option value="10">10</stripes:option>
                            <stripes:option value="20">20</stripes:option>
                            <stripes:option value="30">30</stripes:option>
                            <stripes:option value="40">40</stripes:option>
                            <stripes:option value="50">50</stripes:option>
                        </stripes:select>
                    </td>
                </tr>                
                <tr>
                    <td>
                        <span class="button">
                            <stripes:submit name="searchCustomerForSale"/>
                        </span>
                    </td>
                </tr>
            </stripes:form>
        </table>            


        <br/>        
        <c:if test="${actionBean.customerList.numberOfCustomers > 0}">
            <table class="green">
                <tr>
                    <th><fmt:message key="id"/></th>
                    <th><fmt:message key="first.name"/></th>
                    <th><fmt:message key="last.name"/></th>
                    <th><fmt:message key="make.sale"/></th>
                </tr>
                <c:forEach items="${actionBean.customerList.customers}" var="customer" varStatus="loop">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>${customer.customerId}</td>
                        <td>${customer.firstName}</td>
                        <td>${customer.lastName}</td> 
                        <td>
                            <stripes:form action="/Sales.action">
                                <input type="hidden" name="customer.customerId" value="${customer.customerId}"/>
                                <stripes:submit name="collectCustomerRoleAndAccountDataForSale"/>
                            </stripes:form>
                        </td>
                    </tr>                    
                </c:forEach>                
            </table>
        </c:if>     


    </stripes:layout-component>    
</stripes:layout-render>

