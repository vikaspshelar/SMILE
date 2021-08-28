<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="edit.account.status"/>
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
                <table class="clear">
                    <tr>
                        <td><stripes:label for="account.status"/>:</td>
                        <td>
                            <stripes:select name="account.status">
                                <c:forEach items="${actionBean.allowedAccountStatuses}" var="status"> 
                                    <c:if test="${actionBean.account.status == status}">
                                        <stripes:option value="${status}" selected="selected"><fmt:message key="account.status.${status}"/></stripes:option>
                                    </c:if>
                                    <c:if test="${actionBean.account.status != status}">
                                        <stripes:option value="${status}"><fmt:message key="account.status.${status}"/></stripes:option>
                                    </c:if>
                                </c:forEach>               
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="modifyAccountStatus"/>
                            </span>                        
                        </td>
                    </tr>  
                </table>            
            </stripes:form>
        </div>		

    </stripes:layout-component>
</stripes:layout-render>

