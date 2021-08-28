<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="stock.location.data"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 

        <table class="clear">
            <stripes:form action="/Sales.action" autocomplete="off" >
                <tr>
                    <td colspan="3">
                        <h4>Search for stock to sell/move by</h4>
                    </td>
                </tr>
                
                <tr style="display:none">
                    <td colspan="3">
                        <input type="hidden"  name="soldStockLocationQuery.soldToOrganisationId" size="30" onkeyup="validate(this,'^[0-9]{1,8}$','emptyok')" value="{{org.originalObject.organisationId}}"/>
                    </td>
                </tr>
                <tr>
                    <td>Seller Org Name</td>
                    <td style="width:300px;">
                        <div angucomplete-alt placeholder="Start typing organisation name..."
                             pause="1000"
                             selected-object="org"
                             maxlength="200"
                             remote-url="AutoComplete.action?getOrganisationsJSON=&organisationQuery.organisationName="
                             remote-url-data-field="organisations"
                             title-field="organisationName"
                             input-class="form-control"
                             text-searching="Searching..."
                             text-no-results="No results found!"
                             minlength="3"
                             match-class="highlight" />
                    </td>
                    <td>AND/OR</td>
                </tr>
                
                <tr>
                    <td><fmt:message key="serial.number"/>:</td>
                    <td colspan="2">
                        <stripes:text name="soldStockLocationQuery.serialNumber"  size="20"  maxlength="20"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                        <span class="button">
                            <stripes:submit name="retrieveStockLocation"/>
                        </span>
                    </td>
                </tr>
            </stripes:form>
        </table>     

        <br/>        
        <c:if test="${actionBean.soldStockLocationList.numberOfSoldStockLocations > 0}">
            <stripes:form action="/Sales.action" autocomplete="off">   
                <table class="green" width="99%">
                    <tr>
                        <th>Item Number</th>
                        <th>Serial Number</th>
                        <th>Description</th>
                        <th>Unit Price</th>
                        <th>KIT Price</th>
                        <th>Sale Date</th>                        
                        
                        <c:choose>
                            <c:when test="${actionBean.isICPSeller()}">
                                <th>Sold/Moved From</th>                                
                            </c:when> 
                            <c:otherwise>
                                <th>Sell/Move From</th>                                
                            </c:otherwise> 
                        </c:choose>
                        <th>To Organisation</th>        
                    </tr>
                    <c:set var="index" value="-1"/>
                    <c:forEach items="${actionBean.soldStockLocationList.soldStockLocations}" var="stock" varStatus="loop">
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                            <td>${stock.itemNumber}</td>
                            <td>${stock.serialNumber}</td>
                            <td>${stock.description}</td>
                            <td>${s:convertCentsToCurrencyLong(stock.priceInCentsIncl)}</td>
                           <td>${s:convertCentsToCurrencyLong(stock.kitPrice)}</td>
                           <td>${s:formatDateLong(stock.saleDate)}</td>
                           
                        
                            <c:if test="${actionBean.isSuperDealerSeller()}">
                                <td>
                                    ${s:getOrganisationName(stock.heldByOrganisationId)}
                                    <stripes:hidden name="superDealerSeller" value="${actionBean.isSuperDealerSeller()}"/>
                                </td>
                                <c:set var="index" value="${index + 1}"/>
                                <td> 
                                    <stripes:hidden name="soldStockLocationData.soldStockLocations[${index}].itemNumber" value="${stock.itemNumber}"/>
                                    <stripes:hidden name="soldStockLocationData.soldStockLocations[${index}].serialNumber" value="${stock.serialNumber}"/>
                                    <stripes:hidden name="soldStockLocationData.soldStockLocations[${index}].soldToOrganisationId" value="${stock.heldByOrganisationId}"/>                                            
                                    <stripes:text name="soldStockLocationData.soldStockLocations[${index}].heldByOrganisationId"  size="10"  maxlength="10"/>
                                </td>
                            </c:if>
                            <c:if test="${!actionBean.isSuperDealerSeller()}">
                                 <td>${s:getOrganisationName(stock.soldToOrganisationId)}
                                 <stripes:hidden name="superDealerSeller" value="${actionBean.isSuperDealerSeller()}"/>
                                </td>
                                    <c:if test="${stock.heldByOrganisationId <= 0}">
                                        <c:set var="index" value="${index + 1}"/>
                                        <td> 
                                            <stripes:hidden name="soldStockLocationData.soldStockLocations[${index}].itemNumber" value="${stock.itemNumber}"/>
                                            <stripes:hidden name="soldStockLocationData.soldStockLocations[${index}].serialNumber" value="${stock.serialNumber}"/>
                                            <stripes:hidden name="soldStockLocationData.soldStockLocations[${index}].soldToOrganisationId" value="${stock.soldToOrganisationId}"/>
                                            <stripes:text name="soldStockLocationData.soldStockLocations[${index}].heldByOrganisationId"  size="10"  maxlength="10"/>
                                        </td>
                                    </c:if>
                                    <c:if test="${stock.heldByOrganisationId > 0}">
                                        <td>${s:getOrganisationName(stock.heldByOrganisationId)}</td>
                                    </c:if>
                            </c:if>
                        
                        </tr>                    
                    </c:forEach>                
                </table>
                    <c:if test="${!actionBean.isICPSeller()}">
                        <span class="button">
                            <stripes:submit name="setStockLocation"/>
                        </span>
                    </c:if>        
            </stripes:form>
        </c:if>     
        
    <c:if test="${!actionBean.isICPSeller()}">
        
        <stripes:form action="/Sales.action" autocomplete="off">   
            <table class="clear">
                <tr><td colspan="2"><br/><br/><b>Bulk Location Update</b></td></tr>
                <tr>
                    <td>Item Number:</td>
                    <td colspan="2"><stripes:text name="soldStockLocationData.soldStockLocations[0].itemNumber"  size="10"  maxlength="10"/></td>
                </tr>
                <tr>
                    <td>Sell from Org:</td>
                    <td style="width:300px;">
                        <div angucomplete-alt placeholder="Start typing organisation name..."
                             pause="400"
                             selected-object="soldToBulk"
                             maxlength="200"
                             remote-url="AutoComplete.action?getOrganisationsJSON=&organisationQuery.organisationName="
                             remote-url-data-field="organisations"
                             title-field="organisationName"
                             input-class="form-control"
                             text-searching="Searching..."
                             text-no-results="No results found!"
                             minlength="3"
                             match-class="highlight" />
                    </td>
                    <td>
                        <div ng-show="soldToBulk">
                            Organisation Id {{soldToBulk.originalObject.organisationId}}
                        </div>
                        <input type="hidden" name="soldStockLocationData.soldStockLocations[0].soldToOrganisationId" value="{{soldToBulk.originalObject.organisationId}}"/>
                    </td>
                </tr>
                <tr>
                    <td>Sell to Org:</td>
                    <td style="width:300px;">
                        <div angucomplete-alt placeholder="Start typing organisation name..."
                             pause="400"
                             selected-object="heldByToBulk"
                             maxlength="200"
                             remote-url="AutoComplete.action?getOrganisationsJSON=&organisationQuery.organisationName="
                             remote-url-data-field="organisations"
                             title-field="organisationName"
                             input-class="form-control"
                             text-searching="Searching..."
                             text-no-results="No results found!"
                             minlength="3"
                             match-class="highlight" />
                    </td>
                    <td>
                        <div ng-show="heldByToBulk">
                            Organisation Id {{heldByToBulk.originalObject.organisationId}}
                        </div>
                        <input type="hidden" name="soldStockLocationData.soldStockLocations[0].heldByOrganisationId" value="{{heldByToBulk.originalObject.organisationId}}"/>
                    </td>
                </tr>
                <tr>
                    <td>Used as Replacement:</td>
                    <td><stripes:checkbox name="soldStockLocationData.soldStockLocations[0].usedAsReplacement" value="true" /></td>
                </tr>
                <tr>
                    <td>Serial Numbers:</td>
                    <td colspan="2"><stripes:textarea name="serialList" cols="55" rows="10"></stripes:textarea></td>
                    </tr>
                </table>
                <span class="button">
                <stripes:submit name="setStockLocation"/>
            </span>
        </stripes:form>
    </c:if>       
    </stripes:layout-component>    
</stripes:layout-render>

