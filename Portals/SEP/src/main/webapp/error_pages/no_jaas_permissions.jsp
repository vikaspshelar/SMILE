<%@page import="com.smilecoms.commons.stripes.SmileActionBean, com.smilecoms.commons.base.*,java.util.*" %>


<br/>
Hi ${pageContext.request.remoteUser} Sorry, but you cannot access SEP as you dont have sufficient permissions
<br/><br/><br/>
<a href="login.jsp">Click here to try again as a different user</a>

<%
 SmileActionBean.invalidateSession(request);
%>



    
    