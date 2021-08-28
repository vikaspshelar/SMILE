<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="trouble.ticket.info"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function() {
                makeMenuActive('Help');
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <div style="margin-top: 10px;">
            <jsp:include page="/layout/my_details_left_banner.jsp"/>
            <div  style="margin-top: 10px;" class="accounts_table account_form nine columns">
                <stripes:form action="/SCPTroubleTicket.action" focus="" id="ttEditForm">
                    <table>
                        <tr>
                            <td><fmt:message key="scp.trouble.ticket.id"/>:</td>
                            <td>
                                <stripes:text name="TTIssue.ID" value="${actionBean.TTIssue.ID}" readonly="true"/>
                            </td>
                        </tr>
                        <tr>
                            <td><fmt:message key="scp.ttrouble.ticket.created"/>:</td>
                            <td>
                                <input readonly="true" type="text" value="${s:formatDateLong(actionBean.TTIssue.created)}" class="required" size="20"  readonly="true" name="TTIssue.created"/>
                            </td>        
                        </tr>
                        <tr>
                            <td><fmt:message key="scp.trouble.ticket.updated"/>:</td>
                            <td>
                                <input readonly="true" type="text" value="${s:formatDateLong(actionBean.TTIssue.updated)}" class="required" size="20"  readonly="true"/>
                            </td>        
                        </tr>
                        <tr>
                            <td><fmt:message key="scp.trouble.ticket.status"/>:</td>
                            <td><stripes:text name="TTIssue.status" maxlength="15" size="15" readonly="true"/></td>
                        </tr>
                        <tr>
                            <td><fmt:message key="scp.trouble.ticket.description"/>:</td>
                            <td><stripes:textarea name="TTIssue.description" cols="50" rows="5"  readonly="true"/></td>
                        </tr>

                    </table>            
                </stripes:form>
            </div>

        </div>
    </stripes:layout-component>    
</stripes:layout-render>

