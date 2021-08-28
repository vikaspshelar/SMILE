<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="search.product.instance"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/ProductCatalog.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">    
            <table class="clear">
                <tr>
                    <td><fmt:message key="id"/>:</td>
                    <td><stripes:text  name="productInstance.productInstanceId" size="40" onkeyup="validate(this,'^[0-9]{1,8}$','emptyok')"/></td>
                    <td><fmt:message key="or"/></td>
                </tr>
                <tr>
                    <td><fmt:message key="iccid"/>:</td>
                    <td><stripes:text name="IMSSubscriptionQuery.integratedCircuitCardIdentifier" maxlength="20" size="40" onkeyup="validate(this,'^[0-9]{20,20}$','luhn_emptyok')"/></td>
                    <td><fmt:message key="or"/></td>
                </tr>
                 <tr>
                    <td><stripes:label for="IMSSubscriptionQuery.IMSPrivateIdentity"/>:</td>
                    <td><stripes:text name="IMSSubscriptionQuery.IMSPrivateIdentity" size="40" maxlength="60" onkeyup="validate(this,'^[0-9]{15,15}@private.*$','emptyok')"/></td>
                    <td><fmt:message key="or"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="IMSSubscriptionQuery.IMSPublicIdentity"/>:</td>
                    <td><stripes:text name="IMSSubscriptionQuery.IMSPublicIdentity"  size="40" maxlength="60" onkeyup="validate(this,'.*','emptyok')"/></td>
                    <td><fmt:message key="or"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="IMSI"/>:</td>
                    <td><stripes:text name="IMSI" size="40" maxlength="15" onkeyup="validate(this,'^[0-9]{15,15}$','emptyok')"/></td>
                </tr>
                <tr>
                    <td>
                        <span class="button">
                            <stripes:submit name="retrieveProductInstance"/>
                        </span>
                    </td>
                </tr>
            </table>            
        </stripes:form>

</stripes:layout-component>    
</stripes:layout-render>

