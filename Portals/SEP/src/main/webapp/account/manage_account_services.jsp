<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    Manage Account Services
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="account"/>:
                        ${actionBean.account.accountId}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/Account.action">                                
                            <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/>       
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveAccount"><fmt:message key="manage.account"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>
                            
            <stripes:form action="/Account.action" focus="" id="form_edit">
                <stripes:hidden name="account.accountId" value="${actionBean.account.accountId}"/>
                <br><br>
                <p style='padding:5px; background: gold;'>
                    Please note: This will update all services in the account, except deleted services.
                </p>
                <br>
                <table class="clear" style='padding:15px'>
                    <tr style='padding:15px'>
                        <td style='padding:15px'>Service Status</td>
                        <td style='padding:15px'>
                            <stripes:select name="serviceStatus">
                                <stripes:option value="-1">Please Select</stripes:option>
                                <c:forEach items="${actionBean.allowedAccountLevelServiceStatuses}" var="status">                                                                         
                                        <stripes:option value="${status}"><fmt:message key="account.service.status.${status}"/></stripes:option>                                    
                                </c:forEach>               
                            </stripes:select>
                        </td>
                    </tr>
                    <tr style='padding:15px'>
                        <td style='padding:15px'>Status Reason</td>
                        <td style='padding:15px'>
                            <stripes:select name="reason">
                                <stripes:option value="-1">Please Select</stripes:option>
                                <c:forEach items="${actionBean.accountLevelServiceStatusReason}" var="status">                                                                         
                                        <stripes:option value='<fmt:message key="account.service.status.reason.${status}'><fmt:message key="account.service.status.reason.${status}"/></stripes:option>                                    
                                </c:forEach>               
                            </stripes:select>
                        </td>
                    </tr>
                    <tr style='padding:15px'>
                        <td colspan="2" style='padding:15px'>
                            <span class="button">
                                <stripes:submit name="modifyAccountServiceStatus"/>
                            </span>                        
                        </td>
                    </tr>  
                </table>            
            </stripes:form>
        </div>		

    </stripes:layout-component>
</stripes:layout-render>

