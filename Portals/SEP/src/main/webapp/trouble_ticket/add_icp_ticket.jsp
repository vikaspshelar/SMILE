<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.icp.trouble.ticket"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">

    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/TroubleTicket.action">                                
                            <stripes:select name="entityAction">
                                <stripes:option value="showSearchIssueForICPPage"><fmt:message key="view.trouble.tickets"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>

            <stripes:form action="/TroubleTicket.action" focus="">
                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>                               
                <table class="clear">


                    <tr>
                        <td><stripes:label for="icp.trouble.ticket.issue.type"/>:</td>
                        <td>
                            <stripes:select name="TT_FIXED_FIELD_Issue Type">
                                <stripes:option value="Customer Request">Request</stripes:option>
                                <stripes:option value="Customer Inquiry">Inquiry</stripes:option>
                                <stripes:option value="Customer Complaint">Complaint</stripes:option>
                                <stripes:option value="Customer Incident">Incident</stripes:option>
                            </stripes:select>
                        </td>
                    </tr>

                    <tr>
                        <td><stripes:label for="trouble.ticket.summary"/>:</td>
                        <td><stripes:text name="TT_FIXED_FIELD_Summary" maxlength="50" size="50"/></td>
                    </tr>

                    <tr>
                        <td><stripes:label for="icp.trouble.ticket.category"/>:</td>
                        <td>
                            <stripes:select name="TT_FIXED_FIELD_Category">
                                <stripes:option value="Access">Access</stripes:option>
                                <stripes:option value="Application">Application</stripes:option>
                                <stripes:option value="Connectivity">Connectivity</stripes:option>
                                <stripes:option value="Others">Others</stripes:option>
                            </stripes:select>
                        </td>
                    </tr>

                    <tr>
                        <td><stripes:label for="icp.trouble.ticket.location"/>:</td>
                        <td>
                            <table class="clear">
                                <tr>
                                    <td><stripes:label for="icp.trouble.ticket.location.gps.latitudes"/>:</td>
                                    <td>
                                        <stripes:text name="TT_FIXED_FIELD_GPS:Lat Coords" maxlength="20" size="20"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td><stripes:label for="icp.trouble.ticket.location.gps.longitues"/>:</td>
                                    <td>
                                        <stripes:text name="TT_FIXED_FIELD_GPS:Long Coords" maxlength="20" size="20"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td><fmt:message key="andor"/></td>
                                </tr>
                                <tr>
                                    <td><stripes:label for="icp.trouble.ticket.location.street"/>:</td>
                                    <td>
                                        <stripes:text name="TT_FIXED_FIELD_Add:Street" maxlength="60" size="40"/>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td><stripes:label for="icp.trouble.ticket.description"/>:</td>
                        <td><stripes:textarea name="TT_FIXED_FIELD_Description" cols="80" rows="8"/></td>
                    </tr>                    

                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="createIssueForICP"/>
                            </span>                        
                        </td>
                    </tr>  				
                </table>            
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>

