<%@ include file="/include/sep_include.jsp" %>
<%@page import="java.util.regex.Pattern"%>
<%@page import="com.smilecoms.commons.base.cache.CacheHelper"%>
<%@page import="com.smilecoms.commons.stripes.SmileActionBean, com.smilecoms.commons.base.*,java.util.*" %>


<br/>
Hi ${pageContext.request.remoteUser}, sorry but you cannot access SEP from this machine or browser while you are still logged-in from another machine or browser
<br/><br/><br/>
<% 
    String ssoId = request.getRemoteUser();
    String sessionIP = "";
    String ua = "";
    String sessionIPandID = "";

    if (ssoId != null) {
        try {
             ssoId = ssoId + "_SINGLE_MACHINE_LOGIN";
            sessionIPandID = (String) CacheHelper.getFromRemoteCache(ssoId);
            String[] data = sessionIPandID.split(Pattern.quote("|"));
            sessionIP = data[0];
            ua = data[2];
        } catch (Exception ex) {

        }
    }
%>   
Current session details blocking access:<br/>
IP: <pre><%=sessionIP%></pre>
UA: <pre><%=ua%></pre>

