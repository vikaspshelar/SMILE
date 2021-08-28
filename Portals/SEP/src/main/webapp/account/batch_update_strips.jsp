<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="batch.update.strips"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">
       
        function checkSelection(obj) {

            var startingPrepaidStripId = document.getElementById("txtStartingPrepaidStripId");
            var endingPrepaidStripId = document.getElementById("txtEndingPrepaidStripId");
     
            if(obj.value == "DC") {
                startingPrepaidStripId.value = "0";
                endingPrepaidStripId.value = "0";
            } 
     }
     </script>           

        <stripes:form action="/Account.action" focus="" autocomplete="off"  onsubmit="return alertValidationErrors();">       
            <table class="clear">
                <tr>
                    <td><stripes:label for="prepaidStripBatchData.startingPrepaidStripId"/>:</td>
                    <td><stripes:text name="prepaidStripBatchData.startingPrepaidStripId" id="txtStartingPrepaidStripId" class="required" size="10" maxlength="10" onkeyup="validate(this,'^[0-9]{1,10}$','')"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="prepaidStripBatchData.endingPrepaidStripId"/>:</td>
                    <td><stripes:text name="prepaidStripBatchData.endingPrepaidStripId" id="txtEndingPrepaidStripId" class="required" size="10" maxlength="10" onkeyup="validate(this,'^[0-9]{1,10}$','')"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="prepaidStripBatchData.invoiceData"/>:</td>
                    <td><stripes:text name="prepaidStripBatchData.invoiceData"  class="required" size="15" maxlength="15" onkeyup="validate(this,'^[0-9]{1,15}$','emptyok')"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="new.status"/>:</td>
                    <td>
                        <stripes:select name="prepaidStripBatchData.status" onchange="checkSelection(this);">
                            <stripes:option value="EX">Extracted For Printing</stripes:option>
                            <stripes:option value="WH">Received in Warehouse</stripes:option>
                            <stripes:option value="DC">Distribution Chain (Redeemable)</stripes:option>
                        </stripes:select>
                    </td>
                </tr>
                <tr>  
                    <td>
                        <span class="button">
                            <stripes:submit name="batchUpdateStrips"/> 
                        </span>    
                    </td>
                </tr>                                
            </table>            
        </stripes:form>

    </stripes:layout-component>


</stripes:layout-render>


