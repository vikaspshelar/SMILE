<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="approve.promo.code"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/Sales.action" focus="" autocomplete="off">    
            <table class="clear">
                <tr>
                    <td>Approval Code:</td>
                    <td><textarea type="textarea" cols="60" rows="4" name="promoCodeApprovalCode" ></textarea></td>
                </tr>
                <tr>
                    <td colspan="2">
                        <span class="button">
                            <stripes:submit name="approvePromotionCode"/>
                        </span>
                    </td>
                </tr>
            </table>            
        </stripes:form>
    </stripes:layout-component>    
</stripes:layout-render>
