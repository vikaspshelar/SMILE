<%@ include file="/include/sep_include.jsp" %>


<c:set var="title">
    Load Bank Statement
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <br/>

        <stripes:form action="/Sales.action">

            <table class="clear" width="99%">
                <tr>
                    <td><b>Upload a bank statement for processing</b></td>
                </tr>
                <tr>
                    <td><stripes:file name="bankStatementFile"/></td>
                </tr>
                <tr>
                    <td><stripes:submit name="uploadBankStatement"/></td>
                </tr>
            </table>

        </stripes:form>      
    </stripes:layout-component>
</stripes:layout-render>