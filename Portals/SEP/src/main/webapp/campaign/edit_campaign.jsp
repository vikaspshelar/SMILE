<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="edit.campaign"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <stripes:form action="/Campaign.action" focus="" onsubmit="return alertValidationErrors();">   
            <table class="clear" width="99%">
                <tr>
                    <td style="width: 30%">Campaign Id:</td>
                    <td>
                        <stripes:text name="campaignData.campaignId"  maxlength="10" size="10" onkeyup="validate(this,'^[0-9]{1,8}$','emptynotok')" />            
                    </td>
                </tr>    
                <tr>
                    <td>Product Instance Ids:<br/><br/>
                        <b>Paste list of Product Instance Ids to add to the campaign. One id per line.</b></td>
                    <td>
                        <stripes:textarea name="campaignData.productInstanceIds" cols="10" rows="20"></stripes:textarea>
                        </td>
                    </tr>  
                </tr> 

                <tr>
                    <td></td>
                    <td>
                        <stripes:submit name="storeCampaignData"/>
                    </td>
                </tr>   
            </table>  
        </stripes:form>
    </stripes:layout-component>   
</stripes:layout-render>