<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="permissions"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <br/>
        
        <table class="green">
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
    </stripes:layout-component>
</stripes:layout-render>

