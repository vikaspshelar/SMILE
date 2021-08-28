<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="input.customerids.kyc.check"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/Customer.action">
            <table class="green" width="99%">
                    <tr>
                        <td style="vertical-align:top">
                            <strong>Paste list of target customer ID. One customer ID per line:</strong><br/>
                        </td>
                    </tr>
                    <tr>
                        <td style="vertical-align:top">
                            <stripes:textarea name="customerIdList" cols="55" rows="10"></stripes:textarea>
                        </td>
                    </tr> 
                    <tr>
                        <td>
                            <stripes:submit name="displayQuickViewKYC"/>
                        </td>
                    </tr> 
            </table>
        </stripes:form>
    </stripes:layout-component>    
</stripes:layout-render>

