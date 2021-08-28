<%-- 
    Document   : manage_addresses
    Created on : 22 Jan 2013, 10:43:46 AM
    Author     : lesiba
--%>

<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="manage.addresses"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">
            window.addEventListener("DOMContentLoaded", function() {
                    <c:if test="${s:getPropertyWithDefault('env.customer.verify.with.nida','false') == 'true' && actionBean.customer.KYCStatus == 'V'}">
                        disableNiraSuppliedFields();
                    </c:if>
                });
                
            function disableNiraSuppliedFields()  {
                var showEditAddressButtons =  document.querySelectorAll('[id=showEditAddress]');
                for(i = 0; i < showEditAddressButtons.length; i++){
                    showEditAddressButtons[i].disabled=true;
                }
                
                var deleteAddressButtons =  document.querySelectorAll('[id=deleteAddress]');
                for(j = 0; j < deleteAddressButtons.length; j++){
                    deleteAddressButtons[j].disabled=true;
                }
            }
            
        </script>
            
            
        <div id="entity">
            <table class="entity_header">
                <tr>
                    <c:choose>
                        <c:when test="${actionBean.customer != null}">
                            <td>
                                <fmt:message key="customer"/> : ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                            </td>
                            <td align="right">
                                <stripes:form action="/Customer.action">                                
                                    <stripes:select name="entityAction">
                                        <stripes:option value="retrieveCustomer"><fmt:message key="manage.customer"/></stripes:option>
                                        <stripes:option value="showAddAddresses"><fmt:message key="add.customer.address"/></stripes:option>
                                    </stripes:select>
                                    <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                    <stripes:hidden name="address.customerId" value="${actionBean.customer.customerId}"/>
                                    <stripes:hidden name="customer.firstName" value="${actionBean.customer.firstName}"/> 
                                    <stripes:hidden name="customer.lastName" value="${actionBean.customer.lastName}"/>
                                    <stripes:submit name="performEntityAction"/>
                                </stripes:form>
                            </td>
                        </c:when>
                        <c:otherwise>
                            <td>
                                <fmt:message key="organisation"/> : ${actionBean.organisation.organisationName}
                            </td>
                            <td align="right">
                                <stripes:form action="/Customer.action">
                                    <stripes:select name="entityAction">
                                        <stripes:option value="retrieveOrganisation"><fmt:message key="manage.organisation"/></stripes:option>
                                        <stripes:option value="showAddAddresses"><fmt:message key="add.address"/></stripes:option>
                                    </stripes:select>
                                    <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>
                                    <stripes:hidden name="organisation.organisationId" value="${actionBean.organisation.organisationId}"/>
                                    <stripes:hidden name="organisation.organisationName" value="${actionBean.organisation.organisationName}"/>
                                    <stripes:submit name="performEntityAction"/>
                                </stripes:form>
                            </td>
                        </c:otherwise>
                    </c:choose>
                </tr>
            </table>
            <table  class="clear">
                <c:choose>
                    <c:when test="${actionBean.customer != null}">
                        <c:set var="addresses" value="${actionBean.customer.addresses}"/>
                    </c:when>
                    <c:when test="${actionBean.organisation != null}">
                        <c:set var="addresses" value="${actionBean.organisation.addresses}"/>
                    </c:when>
                </c:choose>
                <c:forEach items="${addresses}" varStatus="loop" var="address">                    
                    <tr>
                        <td colspan="2"><b>${address.type}</b></td>                    
                    </tr>
                    <tr>
                        <td><fmt:message key="address.line1"/>:</td>
                        <td>${address.line1}</td>
                    </tr>                        
                    <tr>
                        <td><fmt:message key="address.line2"/>:</td>
                        <td>${address.line2}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="town"/>:</td>
                        <td>${address.town}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="zone"/>:</td>
                        <td>${address.zone}</td>
                    </tr>
                    <tr>
                        <td><fmt:message key="state"/>:</td>
                        <td>${address.state}</td>
                    </tr>                    
                    <tr>
                        <td><fmt:message key="country"/>:</td>
                        <td>${address.country}</td>
                    </tr>
                    <c:choose>
                        <c:when test="${s:getProperty('env.postalcode.display')}">
                            <tr>
                                <td><fmt:message key="code"/>:</td>
                                <td>${address.code}</td>
                            </tr>
                        </c:when>                        
                    </c:choose>
                    <stripes:form action="/Customer.action">
                        <c:choose>
                            <c:when test="${actionBean.customer != null}">
                                <input type="hidden" name="customerQuery.customerId" value="${address.customerId}"/>
                                <input type="hidden" name="customer.firstName" value="${actionBean.customer.firstName}"/> 
                                <input type="hidden" name="customer.lastName" value="${actionBean.customer.lastName}"/>
                            </c:when>
                            <c:when test="${actionBean.organisation != null}">
                                <input type="hidden" name="organisationQuery.organisationId" value="${address.organisationId}"/>
                                <input type="hidden" name="organisation.organisationName" value="${actionBean.organisation.organisationName}"/>
                            </c:when>
                        </c:choose>

                        <input type="hidden" name="address.addressId" value="${address.addressId}"/>
                        <input type="hidden" name="address.organisationId" value="${address.organisationId}"/>
                        <input type="hidden" name="address.customerId" value="${address.customerId}"/>
                        <input type="hidden" name="address.type" value="${address.type}"/>
                        <input type="hidden" name="address.line1" value="${address.line1}"/>
                        <input type="hidden" name="address.line2" value="${address.line2}"/>
                        <input type="hidden" name="address.town" value="${address.town}"/>
                        <input type="hidden" name="address.zone" value="${address.zone}"/>
                        <input type="hidden" name="address.state" value="${address.state}"/>
                        <input type="hidden" name="address.country" value="${address.country}"/>

                        <c:choose>
                            <c:when test="${s:getProperty('env.postalcode.display')}">
                                <input type="hidden" name="address.code" value="${address.code}"/>
                            </c:when>
                            <c:otherwise>
                                <stripes:hidden name="address.code" value="0000"/>
                            </c:otherwise>
                        </c:choose>
                        <tr>
                            <td>
                                <span class="button">
                                    <stripes:submit name="deleteAddress" id="deleteAddress"/>
                                </span>
                                &nbsp;
                                <span class="button">
                                    <stripes:submit name="showEditAddress" id="showEditAddress"/>
                                </span>
                            </td>
                        </tr>
                    </stripes:form>
                </c:forEach>
            </table>
        </div>
    </stripes:layout-component>
</stripes:layout-render>