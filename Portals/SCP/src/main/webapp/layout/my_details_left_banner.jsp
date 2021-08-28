<%-- 
    Document   : my_details_left_banner
    Created on : Nov 26, 2015, 10:43:36 AM
    Author     : sabza
--%>
<%@include file="../include/scp_include.jsp" %>

<div class="customer columns">
    <img src="images/customer.png"><br>
    <font class="light" style="font-size:30px;"><fmt:message key="scp.basic.data"/></font><br>
    <font style="font-size:12px; color:#75B343;">${actionBean.customer.firstName} ${actionBean.customer.lastName}<br>
    <strong> <fmt:message key="customer.since"/> : </strong>
    <strong>${s:formatDateShort(actionBean.customer.createdDateTime)}</strong></font>
</div>