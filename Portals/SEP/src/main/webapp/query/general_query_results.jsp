<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="general.query.results"/>
</c:set>

<html>
    <head>
        <meta http-equiv="Content-type" content="text/html; charset=UTF-8" />
        <title>${title}</title>
        <meta http-equiv="imagetoolbar" content="no" />
        <meta name="MSSmartTagsPreventParsing" content="true" />
        <link rel="stylesheet" media="all" type="text/css" href="${pageContext.request.contextPath}/css/style.css"/>    
        <%
            response.addHeader("Pragma", "No-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.addDateHeader("Expires", 1);
        %>
    </head>
    <body>
        <br/>    
        <table class="green">
            <c:forEach items="${actionBean.queryRows}" var="row" varStatus="loop">
                <c:if test="${loop.count == 1}">
                    <tr>
                        <c:forEach items="${row}" var="entry">
                            <th>${entry}</th>
                        </c:forEach>
                    </tr>
                </c:if>
                <c:if test="${loop.count != 1}">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <c:forEach items="${row}" var="entry">
                            <td>${entry}</td>
                        </c:forEach>
                    </tr>
                </c:if>
            </c:forEach>                    
        </table>  
    </body>
