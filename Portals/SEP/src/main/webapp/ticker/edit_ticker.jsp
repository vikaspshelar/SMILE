<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="ticker.edit.title"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="html_head">  
        <script type="text/javascript">

            function addRowToTable(tableId, items){
                var rowCount = document.getElementById(tableId).rows.length -1;

                $j(document.getElementById(tableId)).find('tbody')
                    .append($j("<tr>")        
                        .attr('id', 'row'+rowCount)
                        .append($j('<td>')
                            .append($j('<input>')
                                .attr('style', 'width:95%')
                                .attr('type', 'text')
                                .attr('name', items + '['+ rowCount+ '].message')
                            )
                        )            
                        .append($j('<td>')
                            .append($j('<input>')
                                .attr('name',items + '['+ rowCount + '].url')
                                .attr('type', 'text')
                            )
                        )
                        .append($j('<td>')
                            .attr('align', 'center')
                            .append($j('<input>')
                                .attr('id', "removeButton")
                                .attr('type', 'button')
                                .attr('value', 'Delete')
                            )
                        )
                    );   
            }

            function removeRowFromTable(trId) {
                console.log('removing row');
        //        trId.remove();
                  $(trId).closest('tr').remove();
        //          trId.closest('tr').remove();
        //        $j(document.getElementById(trId)).remove(); 
            }

        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">         
        
        <stripes:form action="/Ticker.action">
        <table id="messagetbl" class="green" style="width: 95%">
            <thead>
            <tr>
                <th><fmt:message key="ticker.message"/></th>
                <th><fmt:message key="ticker.url"/></th>
                <th><fmt:message key="ticker.action"/></th>
            </tr>
            </thead>
            <tbody>
            
                <c:set var="count" value="0" scope="page"/>
                <c:forEach items="${actionBean.tickerMessageList}" var="message" varStatus="loop">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td style="width: 80%"><stripes:text style="width:95%" name="tickerMessageList[${loop.index}].message">${message.message}</stripes:text></td>
                        <td><stripes:text name="tickerMessageList[${loop.index}].url">${message.url}</stripes:text></td>
                        <td align="center"><input id="removeButton" onclick="$j(this).closest('tr').remove();" type="button" value="Delete"/></td>
                    </tr>
                    <c:set var="count" value="${count + 1}" scope="page"/>
                </c:forEach>
                    
            
            </tbody>
        </table>
        <table style="width: 95%">
            <tr>
            <td><input onclick="javascript:addRowToTable('messagetbl', 'tickerMessageList');" type="button" value="Add Row"/></td>    
            </tr>
            <tr>
                <td align="right"><stripes:submit name="updateGlobalTicker"/></td>
            </tr>
        </table>
        </stripes:form>
        
        
   </stripes:layout-component>    
</stripes:layout-render>        