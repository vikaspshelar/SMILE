<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="stock.location.data"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 

        <table class="clear">
            <stripes:form action="/Sales.action" autocomplete="off" >

                <tr>
                    <td><fmt:message key="sold.to.organisation"/>:</td>
                    <td style="width:300px;">
                        <div angucomplete-alt 
                             placeholder="Start typing organisation name..."
                             pause="400"
                             selected-object="soldTo"
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
                        <div ng-show="soldTo">
                            Organisation Id {{soldTo.originalObject.organisationId}}
                        </div>
                        <input type="hidden" name="soldStockLocationQuery.soldToOrganisationId" value="{{soldTo.originalObject.organisationId}}" />
                    </td>
                </tr>
                <tr>
                    <td><fmt:message key="held.by.organisation"/>:</td>
                    <td style="width:300px;">
                        <div angucomplete-alt 
                             placeholder="Start typing organisation name..."
                             pause="400"
                             selected-object="heldBy"
                             maxlength="200"
                             remote-url="AutoComplete.action?getOrganisationsJSON=&organisationQuery.organisationName="
                             remote-url-data-field="organisations"
                             title-field="organisationName"
                             input-class="form-control"
                             text-searching="Searching..."
                             text-no-results="No results found!"
                             minlength="3"
                             match-class="highlight"/>
                    </td>
                    <td>
                        <div ng-show="heldBy">
                            Organisation Id {{heldBy.originalObject.organisationId}}
                        </div>
                        <input type="hidden" name="soldStockLocationQuery.heldByOrganisationId" value="{{heldBy.originalObject.organisationId}}" />
                    </td>
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
                        <th>Sold To Org Id</th>
                        <th>Held By Org Id</th>
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
                            <td>${s:getOrganisationName(stock.soldToOrganisationId)}</td>
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
                        </tr>                    
                    </c:forEach>                
                </table>
                <span class="button">
                    <stripes:submit name="setStockLocation"/>
                </span>
            </stripes:form>
        </c:if>     

        <stripes:form action="/Sales.action" autocomplete="off">   
            <table class="clear">
                <tr><td colspan="2"><br/><br/><b>Bulk Location Update</b></td></tr>
                <tr>
                    <td>Item Number:</td>
                    <td colspan="2"><stripes:text name="soldStockLocationData.soldStockLocations[0].itemNumber"  size="10"  maxlength="10"/></td>
                </tr>
                <tr>
                    <td>Sold To Org:</td>
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
                    <td>Held By Org:</td>
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

    </stripes:layout-component>    
</stripes:layout-render>

