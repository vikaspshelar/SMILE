<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="service.instance.detail"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">

    <stripes:layout-component name="html_head">
        <script type="text/javascript">
            
            var skipValidate = false;
            $j(document).ready(function(){
                document.getElementById('siaction').value='CREATE';
                document.getElementById('avps').style.display='block';
                skipValidate = false;
                
                skipChanged();
                
            });
                        
            function skipChanged() {
                if (document.getElementById('mustSkip') != null && document.getElementById('mustSkip').checked) {
                    document.getElementById('siaction').value='NONE';
                    document.getElementById('avps').style.display='none';
                    skipValidate = true;
                } else {
                    document.getElementById('siaction').value='CREATE';
                    document.getElementById('avps').style.display='block';
                    skipValidate = false;
                }   
            }
            window.addEventListener("DOMContentLoaded", function () {
                console.log("init start");
                initWebCam();
                console.log("init end");
            }, false);
            
            var nationality = "${actionBean.customer.nationality}";
            
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>${actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.name}</td>                                            
                </tr>
            </table>
            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="service.specific.info"/></b></td>
                </tr>
                <tr>
                    <td><fmt:message key="service.name"/>:</td>
                    <td>${actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.name}</td>
                </tr>
                <tr>
                    <td><fmt:message key="service.description"/>:</td>
                    <td>${actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.description}</td>
                </tr>         
                <tr>
                    <td><fmt:message key="group.id"/>:</td>
                    <td>${actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].groupId}</td>
                </tr> 
                
                <c:choose>
                        
                    <c:when test = "${(actionBean.customer.nationality=='UG' && (actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.serviceSpecificationId ==5 || actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.serviceSpecificationId == 100))
                                      ||(actionBean.customer.nationality=='TZ' && (actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.serviceSpecificationId ==2 || actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.serviceSpecificationId == 100))}">
                        <tr>
                            <td colspan="2"></td>
                        </tr>
                    </c:when>

                    <c:otherwise>
                        <tr>
                            <td colspan="2"><b><fmt:message key="service.instance.configuration"/></b></td>
                        </tr>
                    </c:otherwise>
                </c:choose>
                          
            </table>

            <stripes:form action="/ProductCatalog.action" autocomplete="off">  
                <input type="hidden" name="currentServiceSpecificationIndex" value="${actionBean.currentServiceSpecificationIndex}"/>
                <input type="hidden" name="productOrder.serviceInstanceOrders[${actionBean.currentServiceSpecificationIndex}].serviceInstance.serviceSpecificationId" value="${actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.serviceSpecificationId}"/>
                <input id="siaction" type="hidden" name="productOrder.serviceInstanceOrders[${actionBean.currentServiceSpecificationIndex}].action" value="CREATE"/>
                
                <c:if test="${(actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].minServiceOccurences == 0 && actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].groupId == 0) or (actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].groupId > 0)}">
                    
                    <c:set var="curCountry" value="${s:getProperty('env.country.name')}"/>                    
                    <c:choose>
                        <c:when test = "${(curCountry.equalsIgnoreCase('Uganda') && (actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.serviceSpecificationId == '5' || actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.serviceSpecificationId == '100'))
                                          ||(curCountry.equalsIgnoreCase('Tanzania') && (actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.serviceSpecificationId =='2' || actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.serviceSpecificationId == '100'))}">
                        
                        </c:when>

                        <c:otherwise>
                           <table class="clear">
                               <tr>
                                    <td><fmt:message key="skip.service"/>:</td>
                                    <td><stripes:checkbox  id="mustSkip" value="no"  name="mustSkip${actionBean.currentServiceSpecificationIndex}" checked="false" onchange="skipChanged()"/></td>
                                </tr>
                            </table>
                        </c:otherwise>
                    </c:choose>
                                
                </c:if>


                <br/>
                <div id="avps">
                    <table class="green" width="99%"> 
                        <tr>
                            <th><fmt:message key="service.attribute"/></th>
                            <th><fmt:message key="service.value"/></th>
                        </tr>
                        <c:forEach items="${actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.AVPs}" var="avp" varStatus="loop">
                            <c:if test="${actionBean.iccid != null && avp.attribute == 'IntegratedCircuitCardIdentifier' && empty actionBean.productOrder.serviceInstanceOrders[actionBean.currentServiceSpecificationIndex].serviceInstance.AVPs[loop.index].value}">
                                <s:avp-input-row avp="${avp}" value="${actionBean.iccid}" avpIndex="${loop.index}" namePrefix="productOrder.serviceInstanceOrders[${actionBean.currentServiceSpecificationIndex}].serviceInstance.AVPs" request="${actionBean.context.request}"/>
                            </c:if>
                            <c:if test="${(actionBean.iccid == null || avp.attribute != 'IntegratedCircuitCardIdentifier' || !empty actionBean.productOrder.serviceInstanceOrders[actionBean.currentServiceSpecificationIndex].serviceInstance.AVPs[loop.index].value)
                                  && s:getPropertyWithDefault('env.customer.verify.with.nida','false') == 'false'}">
                                <s:avp-input-row avp="${avp}" avpIndex="${loop.index}" value="${actionBean.productOrder.serviceInstanceOrders[actionBean.currentServiceSpecificationIndex].serviceInstance.AVPs[loop.index].value}" namePrefix="productOrder.serviceInstanceOrders[${actionBean.currentServiceSpecificationIndex}].serviceInstance.AVPs" request="${actionBean.context.request}"/>
                            </c:if>
                            <c:if test="${(actionBean.iccid == null || avp.attribute != 'IntegratedCircuitCardIdentifier' || !empty actionBean.productOrder.serviceInstanceOrders[actionBean.currentServiceSpecificationIndex].serviceInstance.AVPs[loop.index].value)
                                  && s:getPropertyWithDefault('env.customer.verify.with.nida','false') == 'true'}">
                                <s:avp-input-row avp="${avp}" value="${actionBean.productOrder.serviceInstanceOrders[actionBean.currentServiceSpecificationIndex].serviceInstance.AVPs[loop.index].value}"  avpIndex="${loop.index}" namePrefix="productOrder.serviceInstanceOrders[${actionBean.currentServiceSpecificationIndex}].serviceInstance.AVPs" request="${actionBean.context.request}"/>
                            </c:if>
                        </c:forEach>
                    </table>   
                </div>
                <span class="button">
                    <stripes:submit name="collectServiceInstanceDataForProductInstallBack"/>
                </span>
                <span class="button">
                    <stripes:submit name="collectServiceInstanceDataForProductInstallNext" onclick="return alertValidationErrors();"/>
                </span>               
                <stripes:wizard-fields/>
            </stripes:form>
            <br/>
        </div>

    </stripes:layout-component>
</stripes:layout-render>

