<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="return.sale"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <c:forEach items="${actionBean.sale.saleLines}" var="rline">
            <c:if test="${rline.lineId == actionBean.salesQuery.saleLineId}">
                <c:set var="line" value="${rline}"/> 
            </c:if>
            <c:forEach items="${rline.subSaleLines}" var="subline"> 
                <c:if test="${subline.lineId == actionBean.salesQuery.saleLineId}">
                    <c:set var="line" value="${subline}"/> 
                </c:if>  
            </c:forEach>
        </c:forEach>
        
        <div id="entity">
        <table class="entity_header"  width="99%">
                <tr>
                    <td>
                        Returning or Replacing Item ${line.inventoryItem.serialNumber} in Sale Id ${actionBean.sale.saleId}
                    </td>
                </tr>
         </table>    
         <br/>           
         <table class="green" width="99%">            
                <tr>
                    <th>Line</th>
                    <th>Item Number</th>
                    <th>Serial Number</th>
                    <th>Description</th>
                    <th>Unit Price Incl</th>
                    <th>Quantity</th>
                    <th>Discount Incl</th>
                    <th>Line Price Incl</th>
                </tr>
                
                    <c:set var="returnedSerialNumber" value="${line.inventoryItem.serialNumber}"/>
                    <c:set var="returnedItemNumber" value="${line.inventoryItem.itemNumber}"/> 
                    <tr>   
                        <td>${line.lineNumber}</td>
                        <td>${line.inventoryItem.itemNumber}</td>
                        <td>${line.inventoryItem.serialNumber}</td>
                        <td>${line.inventoryItem.description}</td>
                        <td align="right">${s:convertCentsToCurrencyLong(line.inventoryItem.priceInCentsIncl)}</td>
                        <td align="right">${line.quantity}</td>
                        <td align="right">${s:convertCentsToCurrencyLong(line.lineTotalDiscountOnInclCents)}</td>
                        <td align="right">${s:convertCentsToCurrencyLong(line.lineTotalCentsIncl)}</td>
                    </tr>
            </table> <br/>
                    
                    <c:if test="${s:getListSize(line.subSaleLines) == 0}">
                        <c:set var="lastSeenReturnLineType" value="replacement"/>
                        <tr>
                            <td colspan="8">
                                   <c:forEach items="${line.returns}" var="returnLine">
                                       <c:choose>
                                            <c:when test="${empty returnLine.parentReturnId}">
                                                <c:set var="lastSeenReturnLineType" value="return"/>
                                                <c:set var="parentReturnId" value="${returnLine.returnId}"/>
                                                <c:set var="returnedSerialNumber" value="${returnLine.returnedSerialNumber}"/>
                                                <c:set var="returnedItemNumber" value="${returnLine.returnedItemNumber}"/>
                                                <span style="margin-left:3px;"><b>Return:</b></span><br/>
                                                <table class="green" width="99%">    
                                                <tr>
                                                    <th>Location</th>
                                                    <th>Reason</th>
                                                    <th>Description</th>
                                                    <th>Date</th>
                                                    <th><span style="white-space: nowrap">Sales Person</span></th>
                                                    <th><span style="white-space: nowrap">Item #</span></th>
                                                    <th><span style="white-space: nowrap">Serial #</span></th>
                                                    <th>PDF</th>
                                                </tr>
                                                <tr>                                                    
                                                    <td>${returnLine.location}</td>
                                                    <td>${returnLine.reasonCode}</td>
                                                    <td>${returnLine.description}</td>
                                                    <td>${s:formatDateLong(returnLine.createdDate)}</td>
                                                    <td>${returnLine.createdByCustomerId}</td>
                                                    <td>${returnLine.returnedItemNumber}</td>
                                                    <td>${returnLine.returnedSerialNumber}</td>
                                                    <td>
                                                        <stripes:link href="/Sales.action" event="getReturnReplacementPDF"> 
                                                            <stripes:param name="salesQuery.saleLineId" value="${returnLine.saleLineId}"/>
                                                            <stripes:param name="return.returnId" value="${returnLine.returnId}"/>
                                                            View
                                                        </stripes:link>
                                                    </td>
                                                </tr>
                                                </table>
                                            </c:when>
                                            <c:otherwise>
                                                <c:set var="lastSeenReturnLineType" value="replacement"/>
                                                <span style="margin-left:3px;"><b>Replacement:</b></span><br/>
                                                <table class="green" width="99%">
                                                <tr>
                                                    <th>Location</th>
                                                    <th>Date</th>
                                                    <th><span style="white-space: nowrap">Sales Person</span></th>
                                                    <th><span style="white-space: nowrap">Item #</span></th>
                                                    <th><span style="white-space: nowrap">Serial #</span></th>
                                                    <th>PDF</th>
                                                </tr>
                                                <tr>
                                                    <td>${returnLine.location}</td>
                                                    <td>${s:formatDateLong(returnLine.createdDate)}</td>
                                                    <td>${returnLine.createdByCustomerId}</td>
                                                    <td>${returnLine.replacementItem.itemNumber}</td>
                                                    <td>${returnLine.replacementItem.serialNumber}</td>
                                                    <td>
                                                        <stripes:link href="/Sales.action" event="getReturnReplacementPDF"> 
                                                            <stripes:param name="salesQuery.saleLineId" value="${returnLine.saleLineId}"/>
                                                            <stripes:param name="return.returnId" value="${returnLine.returnId}"/>
                                                            <stripes:param name="return.replacementItem.serialNumber" value="${returnLine.replacementItem.serialNumber}"/>
                                                            View
                                                        </stripes:link>
                                                    </td>
                                               </tr>
                                                </table>
                                            </c:otherwise>
                                       </c:choose>
                                   </c:forEach>
                                   
                                        
                                   <c:if test="${lastSeenReturnLineType == 'replacement'}">
                                       <span style="margin-left:3px;"><b>Do Return:</b></span><br/>
                                       <table class="green" width="99%">
                                           <tr>
                                                <th>Location</th>
                                                <th>Reason</th>
                                                <th>Description</th>
                                                <th><span style="white-space: nowrap">Return</span></th>
                                            </tr>     
                                               <tr>
                                                    <stripes:form action="/Sales.action" focus="">
                                                    <td><stripes:text  name="return.location" size="10" maxlength="10" value="${actionBean.defaultReturnLocation}"/></td>
                                                    <td><stripes:select name="return.reasonCode">
                                                        <c:forEach items="${s:getPropertyAsList('env.pos.return.reason.codes')}" var="code">                                   
                                                            <stripes:option value="${code}">
                                                                ${code}
                                                            </stripes:option>
                                                        </c:forEach>
                                                    </stripes:select>
                                                    </td>
                                                    <td>
                                                        <stripes:textarea  rows="5" name="return.description" cols="20"/>
                                                    </td>
                                                    <td>   
                                                           <stripes:hidden name="return.saleLineId" value="${actionBean.salesQuery.saleLineId}"/>
                                                           <stripes:hidden name="return.createdByCustomerId" value="${actionBean.salesPerson.customerId}"/>
                                                           <stripes:hidden name="return.returnedSerialNumber" value="${returnedSerialNumber}"/>
                                                           <stripes:hidden name="return.returnedItemNumber" value="${returnedItemNumber}"/>
                                                           <stripes:submit name="createReturnOrReplacement"/>
                                                    </stripes:form>       
                                                    </td>
                                                </tr>
                                       </table>
                                   </c:if>
                                      
                                   
                                       <c:if test="${lastSeenReturnLineType == 'return'}">
                                        <span style="margin-left:3px;"><b>Do Replacement:</b></span><br/>
                                        <table class="green" width="99%">   
                                        <tr>
                                            <th colspan="2"><span style="white-space: nowrap">Search for Item</span></th>
                                            <th><span style="white-space: nowrap">Location</span></th>
                                            <th ng-show="replacementItem != null"><span style="white-space: nowrap">Replacement Item</span></th>
                                        </tr>   
                                        <tr>
                                         <td colspan="2">
                                             <stripes:form action="/Sales.action" onsubmit="return false"  autocomplete="off" ng-submit="getItemsForSale(sale.recipientAccountId, sale.recipientCustomerId, sale.recipientOrganisationId, itemInfo, sale.tenderedCurrency);" >    
                                                <stripes:hidden name="sale.recipientCustomerId" value="${actionBean.sale.recipientCustomerId}" nginit="true"/>
                                                <stripes:hidden name="sale.recipientAccountId" value="${actionBean.sale.recipientAccountId}" nginit="true"/>
                                                <stripes:hidden name="sale.recipientOrganisationId" value="${actionBean.sale.recipientOrganisationId}" nginit="true"/>
                                                <stripes:hidden name="sale.tenderedCurrency" value="${actionBean.sale.tenderedCurrency}" nginit="true"/>
                                                <input id="itemsbox" type="text" name="item" list="items" size="30" ng-model="itemInfo" ng-readonly="searching" autocomplete="off" />
                                                <datalist id="items">
                                                    <option ng-repeat="inventoryItem in inventoryList.inventoryItems" value="{{inventoryItem.description}} [{{inventoryItem.itemNumber}}][{{inventoryItem.serialNumber}}]"/>
                                                </datalist>
                                                <stripes:submit name="add" onclick="document.getElementById('itemsbox').blur();"/>
                                             </stripes:form>   
                                         </td>
                                         <stripes:form action="/Sales.action" focus="" id="testing">
                                             
                                            <input type="text" name="return.parentReturnId" value="${parentReturnId}" hidden="true"/>
                                             <stripes:hidden name="return.returnedSerialNumber" value="${returnedSerialNumber}"/>
                                             <stripes:hidden name="return.returnedItemNumber" value="${returnedItemNumber}"/>
                                             <stripes:hidden name="return.saleLineId" value="${actionBean.salesQuery.saleLineId}"/>
                                             <stripes:hidden name="return.createdByCustomerId" value="${actionBean.salesPerson.customerId}"/>
                                                 
                                         <td><stripes:text  name="return.location" size="10" maxlength="10" value="${actionBean.defaultReturnLocation}"/></td>
                                         <td ng-show="replacementItem != null">
                                             <div ng-show="replacementItem != null">
                                             <table class="green" width="99%">
                                                 <tr><td><p style="display:inline">Descr:</p></td> 
                                                     <td><input type="text" name="return.replacementItem.description" value="{{replacementItem.description}}" readonly="true" size="21"/></td>
                                                 </tr>
                                                 <tr><td><span style="white-space: nowrap">Item #:</span></td> 
                                                     <td><input type="text" name="return.replacementItem.itemNumber" value="{{replacementItem.itemNumber}}" readonly="true" size="21"/></td>
                                                 </tr>
                                                 <tr><td><span style="white-space: nowrap">Serial #:</span></td>
                                                     <td><input type="text" name="return.replacementItem.serialNumber" value="{{replacementItem.serialNumber}}" readonly="true" size="21"/></td>
                                                 </tr>    
                                                 <tr><td colspan="2"><stripes:submit name="createReplacement"/></td>
                                             </table>
                                             </div>
                                         </td>
                                         </stripes:form> 
                                         </tr>
                                        </table>
                                   </c:if> 
                    </c:if>
        </div>
                                           
    </stripes:layout-component>
</stripes:layout-render>

