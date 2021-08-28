<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="search.account"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/Account.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">    
            <table class="clear">
                <tr>
                    <td><fmt:message key="account.accountId"/>:</td>
                    <td><stripes:text size="10" maxlength="10" name="accountQuery.accountId" onkeyup="validate(this,'^[0-9]{10,10}$','')"/></td>
                </tr>
                <tr>
                    <td>
                        <span class="button">
                            <stripes:submit name="searchAccount"/>
                        </span>
                    </td>
                </tr>
            </table>            
        </stripes:form>
        <br/>
        <c:if test="${actionBean.accountList.accounts != null}">
            <c:forEach items="${actionBean.accountList.accounts}" var="account" varStatus="loop">
                <table class="green">
                    <tr>
                        <th><fmt:message key="account.accountId"/></th>
                        <th><fmt:message key="view"/></th>
                    </tr>
                    <tr class="odd">
                        <td>${account.accountId}</td>
                        <stripes:form action="/Account.action">
                            <td>
                                <stripes:hidden name="accountQuery.accountId" value="${account.accountId}"/>
                                <stripes:submit name="retrieveAccount"/>
                            </td>
                        </stripes:form>
                    </tr>                    
                </table>             
            </c:forEach>
        </c:if>        
    </stripes:layout-component>    
</stripes:layout-render>

