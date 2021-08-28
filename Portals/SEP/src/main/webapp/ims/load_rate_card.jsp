<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="load.rate.card"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <br/>

        <stripes:form action="/IMS.action">

            <table class="clear" width="99%">
                <tr>
                    <td><b>Upload xlsx of BICS or IBASIS rate card for processing</b></td>
                </tr>
                <tr>
                    <td>
                        <stripes:select name="ratecardPartner" class="required">
                            <stripes:option value="" selected="selected">-- Select Partner --</stripes:option>
                            <stripes:option value="BICS">BICS</stripes:option>
                            <stripes:option value="IBASIS">IBASIS</stripes:option>
                        </stripes:select>
                    </td>
                </tr>
                <tr>
                    <td><stripes:file name="interconnectRateCardFile"/></td>
                </tr>
                <tr>
                    <td><stripes:submit name="uploadInterconnectRateCard"/></td>
                </tr>
            </table>

        </stripes:form>      
    </stripes:layout-component>
</stripes:layout-render>

