<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.trouble.ticket.comment"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="customer">
                            <fmt:param>${actionBean.customer.customerId}</fmt:param>
                        </fmt:message>
                        ${actionBean.customer.customerId}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/TroubleTicket.action">         
                            <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                            <stripes:hidden name="TTIssue.ID" value="${actionBean.TTIssue.ID}"/>      
                            <stripes:select name="entityAction">
                               <stripes:option value="retrieveTroubleTicketComments"><fmt:message key="view.trouble.ticket.comments"/></stripes:option>
                                <stripes:option value="editTroubleTicket"><fmt:message key="trouble.ticket.edit"/></stripes:option>
                                </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>
            
            <stripes:form action="/TroubleTicket.action" focus="">
                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/> 
                <stripes:hidden name="TTIssue.ID" value="${actionBean.TTIssue.ID}"/>      
                <table class="clear">
                    <tr>
                        <td><stripes:label for="trouble.ticket.description"/>:</td>
                        <td><stripes:textarea name="TTComment.body" cols="60" rows="5"/></td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="insertTroubleTicketComment"/>
                            </span>                        
                        </td>
                    </tr>  				
                </table>            
            </stripes:form>
        </div>
    </stripes:layout-component>
    </stripes:layout-render>
    
    
