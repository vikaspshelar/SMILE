<%-- 
    Document   : image_snippets
    Created on : 24 Jul 2013, 21:41:55 PM
    Author     : lesiba
--%>

<%@ page pageEncoding="UTF-8" %>
<%@ include file="/include/sep_include.jsp" %>
<%@page import="com.smilecoms.commons.stripes.SmileActionBean" %>

<c:if test="${s:getListSize(actionBean.photographs) > 0}">
    <table class="clear">
        <tr>
            <td>
                <b><fmt:message key="scanned.documents"/>:</b>
            </td>
        </tr>

        <c:forEach items="${actionBean.photographs}" varStatus="loop"> 
            <tr id="row${loop.index}">
                <td align="left" valign="top">
                    <fmt:message  key="document.type.${actionBean.photographs[loop.index].photoType}"/>
                </td>
                <td align="left" valign="top">
                    <c:choose>
                        <c:when test='${actionBean.photographs[loop.index].photoType.matches("^.*publickey.*$")}'>
                            <a href="${pageContext.request.contextPath}/images/public_key.png" target="_blank">
                                <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/images/public_key.png"/>
                            </a>
                        </c:when>
                        <c:when test='${actionBean.photographs[loop.index].photoType.matches("^.*fingerprint.*$")}'>
                            <a href="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg" target="_blank">
                                <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg"/>
                            </a>
                        </c:when>
                        <c:when test='${actionBean.photographs[loop.index].photoGuid.matches("^.*.pdf$")}'>
                            <a href="/sep/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[loop.index].photoGuid}" target="_blank">
                                <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/images/pdf-icon.jpg"/>
                            </a>
                        </c:when>
                        <c:when test='${actionBean.photographs[loop.index].photoGuid.matches("^.*.xlsx$") || actionBean.photographs[loop.index].photoGuid.matches("^.*.xls$")}'>
                            <a href="/sep/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[loop.index].photoGuid}" target="_blank">
                                <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/images/excel_file_image.png"/>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <a href="/sep/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[loop.index].photoGuid}" target="_blank">
                                <img id="imgfile${loop.index}" class="thumb" src="/sep/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[loop.index].photoGuid}"/>
                            </a>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
    </table>
</c:if>