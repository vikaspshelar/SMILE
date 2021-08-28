<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="service.instance.detail"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="html_head">
        <script type="text/javascript">
            var skipValidate = false;
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>${actionBean.productServiceSpecificationMappings[0].serviceSpecification.name}</td>                                            
                </tr>
            </table>
            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="service.specific.info"/></b></td>
                </tr>
                <tr>
                    <td><fmt:message key="service.name"/>:</td>
                    <td>${actionBean.productServiceSpecificationMappings[0].serviceSpecification.name}</td>
                </tr>
                <tr>
                    <td><fmt:message key="service.description"/>:</td>
                    <td>${actionBean.productServiceSpecificationMappings[0].serviceSpecification.description}</td>
                </tr>                     
                <tr>
                    <td colspan="2"><b><fmt:message key="service.instance.configuration"/></b></td>
                </tr>
            </table>

            <stripes:form action="/ProductCatalog.action" onsubmit="return alertValidationErrors();">  
                <input type="hidden" name="productOrder.serviceInstanceOrders[0].serviceInstance.serviceSpecificationId" value="${actionBean.productServiceSpecificationMappings[0].serviceSpecification.serviceSpecificationId}"/>                
                <input type="hidden" name="productOrder.productInstanceId" value="${actionBean.productInstance.productInstanceId}"/>                
                <input type="hidden" name="productSpecification.productSpecificationId" value="${actionBean.productSpecification.productSpecificationId}"/>                
                <input type="hidden" name="newServiceSpecificationId" value="${actionBean.newServiceSpecificationId}"/>                
                <table class="green" width="99%"> 
                    <tr>
                        <th><fmt:message key="service.attribute"/></th>
                        <th><fmt:message key="service.value"/></th>
                    </tr>
                    <c:forEach items="${actionBean.productServiceSpecificationMappings[0].serviceSpecification.AVPs}" var="avp" varStatus="loop">
                            <s:avp-input-row avp="${avp}" value="" avpIndex="${loop.index}" namePrefix="productOrder.serviceInstanceOrders[0].serviceInstance.AVPs" request="${actionBean.context.request}"/>
                    </c:forEach>
                </table>   
                <span class="button">
                    <stripes:submit name="provisionNewServiceInstance"/>
                </span>               
            </stripes:form>
            <br/>
        </div>

    </stripes:layout-component>
</stripes:layout-render>

