<%@page import="com.smilecoms.commons.base.BaseUtils"%>
<%@ include file="/include/scp_include.jsp" %>


<c:set var="title">
    <c:if test="${empty actionBean.reason}">
        <fmt:message key="scp.no.credit.heading"/>
    </c:if>
    <c:if test="${!empty actionBean.reason}">
        <fmt:message key="scp.no.credit.heading.${actionBean.reason}"/>
    </c:if>
</c:set>

<stripes:layout-render name="/layout/noCreditLayout.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <div style="margin-top:10px;">
        <div>
                <table>
                    <tr>
                        <td>
                            <c:if   test="${empty actionBean.reason}">
                                <fmt:message key="scp.no.credit.body"/>
                            </c:if>
                            <c:if   test="${!empty actionBean.reason}">
                                <fmt:message key="scp.no.credit.body.${actionBean.reason}"/>
                            </c:if>
                        </td>
                    </tr>
                </table>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
