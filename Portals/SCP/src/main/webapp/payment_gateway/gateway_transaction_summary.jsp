<%-- 
    Document   : gateway_transaction_summary
    Created on : 15 Dec 2017, 6:17:25 AM
    Author     : sabza
--%>
<%@page import="com.smilecoms.commons.base.BaseUtils"%>
<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="scp.recharge.transaction.status.${actionBean.sale.status}"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">

    <stripes:layout-component name="contents">
        <div style="margin-top:10px;">
            <div>
                <table width="100%" style="border-style: solid; border-width: 1px; border-color: #639B2E; border-collapse: separate; border-spacing: 10px; background-color: white; margin: 3px 3px 3px 3px;">
                    <tr>
                        <td width="100%">
                            <c:choose>
                                <c:when test="${actionBean.sale.status == 'PD'}">
                                    <fmt:message key="scp.sale.status.pd.msg" >
                                        <fmt:param>${actionBean.customer.firstName}</fmt:param>
                                        <fmt:param>${actionBean.sale.saleId}</fmt:param>
                                    </fmt:message>    
                                </c:when>
                                <c:when test="${actionBean.sale.status == 'FG'}">
                                    <fmt:message key="scp.sale.status.fg.msg">
                                        <fmt:param>${actionBean.customer.firstName}</fmt:param>
                                        <fmt:param>${actionBean.sale.saleId}</fmt:param>
                                    </fmt:message>    
                                </c:when>
                                <c:when test="${actionBean.sale.status == 'FS'}">
                                    <fmt:message key="scp.sale.status.fs.msg">
                                        <fmt:param>${actionBean.customer.firstName}</fmt:param>
                                        <fmt:param>${actionBean.sale.saleId}</fmt:param>
                                    </fmt:message>    
                                </c:when>
                                <c:when test="${actionBean.sale.status == 'PP'}">
                                    <fmt:message key="scp.sale.status.pp.msg">
                                        <fmt:param>${actionBean.customer.firstName}</fmt:param>
                                        <fmt:param>${actionBean.sale.saleId}</fmt:param>
                                    </fmt:message>    
                                </c:when>      
                                <c:otherwise>
                                    <span style="font-weight: bold; color:#75b343;">Unknown status</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </table>
            </div>
            
        </div>
    </stripes:layout-component>
</stripes:layout-render>
