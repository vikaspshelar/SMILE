<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="unit.credits"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <table class="green" width="95%">                                
            <tr>
                <th>Name</th>
                <th>Price</th>
                <th>Usable / Validity</th>
                <th>Available From</th>  
                <th>Available To</th>
                <th>Units</th>
                <th>Purchase Roles</th>
                <th>Detailed Configuration</th>
            </tr>
            <c:if test="${actionBean.unitCreditSpecificationList.numberOfUnitCreditSpecifications != 0}">
                <c:forEach items="${s:orderList(actionBean.unitCreditSpecificationList.unitCreditSpecifications, 'getUnitCreditSpecificationId', 'asc')}" var="unitCreditSpecification" varStatus="loop">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>${unitCreditSpecification.name}</td>     
                        <td>${s:convertCentsToCurrencyShort(unitCreditSpecification.priceInCents)}</td>
                        <td>${unitCreditSpecification.usableDays} / ${unitCreditSpecification.validityDays}</td>
                        <td>${s:formatDateLong(unitCreditSpecification.availableFrom)}</td>        
                        <td>${s:formatDateLong(unitCreditSpecification.availableTo)}</td> 
                        <td>${s:displayVolumeAsString(unitCreditSpecification.units, unitCreditSpecification.unitType)}</td>
                        <td>${unitCreditSpecification.purchaseRoles}</td>
                        <td>
                            Spec Id: ${unitCreditSpecification.unitCreditSpecificationId} <br/>
                            Item Number: ${unitCreditSpecification.itemNumber} <br/>
                            Unit Type: ${unitCreditSpecification.unitType} <br/>
                            Prod-Svc Spec Ids:
                            <c:forEach items="${unitCreditSpecification.productServiceMappings}" var="psm" varStatus="loop2">
                                ${psm.productSpecificationId}-${psm.serviceSpecificationId}&nbsp;
                            </c:forEach><br/>
                            Priority: ${unitCreditSpecification.priority}<br/>
                            Filter Class: ${s:breakUp(unitCreditSpecification.filterClass,25)}<br/>
                            Wrapper Class: ${s:breakUp(unitCreditSpecification.wrapperClass,25)}<br/>
                            ${s:breakUp(unitCreditSpecification.configuration,25)}
                        </td>
                    </tr>           
                </c:forEach>
            </c:if>
        </table>            

    </stripes:layout-component>
</stripes:layout-render>

