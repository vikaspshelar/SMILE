<%@include  file="../include/scp_include.jsp" %>
<%@page import="com.smilecoms.commons.stripes.SmileActionBean" %>
<stripes:layout-definition>
    <%@page contentType="text/html" pageEncoding="UTF-8"%>
    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
    <html>
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <stripes:layout-component name="head"/>
        </head>
        <body>    

            <stripes:layout-component name="contents"/>
        </body>
    </html>
</stripes:layout-definition>