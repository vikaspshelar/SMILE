<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="service.instance.detail"></fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="html_head">
        <script type="text/javascript">
            var skipValidate = false;
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
                    <td>${actionBean.serviceSpecification.name}</td>                                            
                </tr>
            </table>
            <table class="clear">
                <tr>
                    <td colspan="2"><b><fmt:message key="service.specific.info"/></b></td>
                </tr>
                <tr>
                    <td><fmt:message key="service.name"/>:</td>
                    <td>${actionBean.serviceSpecification.name}</td>
                </tr>
                <tr>
                    <td><fmt:message key="service.description"/>:</td>
                    <td>${actionBean.serviceSpecification.description}</td>
                </tr>                     
                <tr>
                    <td colspan="2"><b><fmt:message key="service.instance.configuration"/></b></td>
                </tr>
            </table>

            <stripes:form action="/ProductCatalog.action" onsubmit="return alertValidationErrors();">  
                <input type="hidden" name="productOrder.serviceInstanceOrders[0].serviceInstance.serviceSpecificationId" value="${actionBean.serviceInstance.serviceSpecificationId}"/>                
                <input type="hidden" name="productOrder.serviceInstanceOrders[0].serviceInstance.serviceInstanceId" value="${actionBean.serviceInstance.serviceInstanceId}"/>                
                <input type="hidden" name="productOrder.productInstanceId" value="${actionBean.serviceInstance.productInstanceId}"/>                
                <input type="hidden" name="serviceInstance.serviceInstanceId" value="${actionBean.serviceInstance.serviceInstanceId}"/>                
                <table class="green" width="99%"> 
                    <tr>
                        <th><fmt:message key="service.attribute"/></th>
                        <th><fmt:message key="service.value"/></th>
                    </tr>
                    <c:forEach items="${actionBean.serviceInstance.AVPs}" var="avp" varStatus="loop">
                            <s:avp-input-row avp="${avp}" value="" avpIndex="${loop.index}" namePrefix="productOrder.serviceInstanceOrders[0].serviceInstance.AVPs" request="${actionBean.context.request}"/>
                    </c:forEach>
                </table>   
                <span class="button">
                    <stripes:submit name="changeServiceInstanceConfiguration"/>
                </span>               
            </stripes:form>
            <br/>
        </div>

    </stripes:layout-component>
</stripes:layout-render>

