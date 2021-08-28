<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="insufficient.permissions"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">           
        <br/><br/>
        
        <table class="clear">
            <tr><td>
                    <fmt:message key="no.permissions.message">
                        <fmt:param>${param.resource}</fmt:param>
                    </fmt:message>
            </td></tr>
            <tr><td>
                    <fmt:message key="logout.url.description">
                        <fmt:param>
                            <stripes:link href="/login.jsp"><fmt:message key="here"/></stripes:link>
                        </fmt:param>
                    </fmt:message>
            </td></tr>
        </table>
        
       <br/>
       
       
        <table class="green">
            <tr>
                <th colspan="2"><b>Your current roles and permissions per role are as follows</b></th>
            </tr>
            <tr>
                <th><fmt:message key="role"/></th>
                <th><fmt:message key="permissions"/></th>
            </tr>
            <c:forEach items="${actionBean.usersRoles}" var="role" varStatus="loop1">
                <tr class="${loop1.count mod 2 == 0 ? "even" : "odd"}">
                    <td valign="top">${role}</td>     
                    <td>
                        
                        <table class="clear">
                            <c:forEach items="${actionBean.permissions[loop1.count -1]}" var="perm">
                                <tr>
                                    <td>${perm}</td>     
                                </tr>
                            </c:forEach>
                        </table>
                        
                    </td>
                </tr>
            </c:forEach>
        </table>
        
        <br/><br/>
        
    </stripes:layout-component>
    </stripes:layout-render>
    
    
    