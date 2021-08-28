<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="credit.note"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <table class="clear">     
            <tr>
                <td>Credit Note Id:</td>
                <td>${actionBean.creditNote.creditNoteId}</td>
            </tr>
            <tr>
                <td>Sales Person:</td>
                <td>
                    <stripes:link href="/Customer.action" event="retrieveCustomer"> 
                        <stripes:param name="customerQuery.customerId" value="${actionBean.creditNote.salesPersonCustomerId}"/>
                        ${actionBean.salesPerson.firstName} ${actionBean.salesPerson.lastName}
                    </stripes:link>
                </td>
            </tr>
            <tr>
                <td>Sale Id:</td>
                <td>
                    <stripes:link href="/Sales.action" event="showSale"> 
                        <stripes:param name="sale.saleId" value="${actionBean.creditNote.saleId}"/>
                        ${actionBean.creditNote.saleId}
                    </stripes:link>
                </td>
            </tr>
            <tr>
                <td>Reason Code:</td>
                <td>${actionBean.creditNote.reasonCode}</td>
            </tr>
            <tr>
                <td>Description:</td>
                <td>${actionBean.creditNote.description}</td>
            </tr>            
        </table>    

    </stripes:layout-component>


</stripes:layout-render>

