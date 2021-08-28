<%@page import="com.smilecoms.commons.base.cache.CacheHelper"%>

<%
    String ssoid = request.getParameter("ssoid").toLowerCase();
    String resp = (String) CacheHelper.getFromRemoteCache("Track_" + ssoid);
    if (resp == null) {
        resp = "<head><meta http-equiv='refresh' content='2;/sep/customer/track_customer.jsp;jsessionid=" + request.getRequestedSessionId() + "?trackSession=true&ssoid=" + ssoid + "'/></head>This customer has not viewed a page in the last 10 minutes or has not permitted tracking yet";
    } else if (resp.indexOf("<head>") != -1)  {
        if (resp.indexOf("href=\"/scp/") != -1) {
            resp = resp.replaceAll("src=\"images/", "src=\"/scp/images/");
        } else {
            resp = resp.replaceAll("src=\"images/", "src=\"/sep/images/");
        }
        resp = resp.replaceFirst("<head>", "<head><meta http-equiv='refresh' content='2;/sep/customer/track_customer.jsp;jsessionid=" + request.getRequestedSessionId() + "?trackSession=true&ssoid=" + ssoid + "'/>");
    } else {
        resp = "<head><meta http-equiv='refresh' content='2;/sep/customer/track_customer.jsp;jsessionid=" + request.getRequestedSessionId() + "?trackSession=true&ssoid=" + ssoid + "'/>" + resp;
    }
    
     resp = resp.replaceAll("<a href=\"", "<a href=\"/sep/index.jsp?NOGO");
     resp = resp.replaceAll("action=\"", "action=\"/sep/index.jsp?NOGO");
     
%>

<%=resp%>
