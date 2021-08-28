<%
    response.addHeader("Pragma", "No-cache");
    response.addHeader("Cache-Control", "no-cache");
    response.addDateHeader("Expires", 1);
%>
<img src="/sep/uploaded_images/<%=request.getParameter("filename")%>.jpg"/>
