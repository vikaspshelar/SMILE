<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*" %>

<%
    /*
     * This page generates a frameset based on the configuration of a dashboard ID.
     * Properties required in the property table are as follows:
     * - env.sop.dash.page.count : The number of dashboard pages to loop through. E.g. 4.
     * - env.sop.dash.page.1.layout - The layout of dashboard 1 expressed as rowsXcolumns. E.g. 1X3 . For 4 pages, there should be 4 properties.
     * - env.sop.dash.page.1.url.1 - The url to load in frame at row 1 column 1 on page 1 . For page 1, we would need 1, 2 and 3 for a 1X3 layout
     * - env.sop.dash.changesecs - The number of seconds between pages when cycling through them.
     *
     * Pages will cycle from page 1 to 2 ... to N and then back to 1 again.
     *
     * URL Formats:
     * Dials:
     * http://192.168.5.46/sop/dial.jsp?loc=DAS&name=BS.*&type=latency&title=BS%20Latency&height=250&width=250
     * &refreshsecs=30&lower=0&upper=1000&major=100&minor=5&unit=ms&agg=avg&warn=200
     * Bars:
     * http://192.168.5.46/sop/bargraph.jsp?loc=DAS&name=Plat.*&type=latency&title=All%20Platform%20Latencies&width=1600&height=400&refreshsecs=30
     */

// Get the target screens width and height if passed in and put in the session
    String w = request.getParameter("width");
    if (w != null) {
        session.setAttribute("width", w);
    }
    String h = request.getParameter("height");
    if (h != null) {
        session.setAttribute("height", h);
    }

    String force = request.getParameter("force");
    int pageToDisplay;
    if (force == null) {

// First get the number of pages
        List<String> pages = BaseUtils.getPropertyAsList("env.sop.dash.pages");
// Now, what page are we on currently
        String lastPage = request.getParameter("page");
        int pageLastDisplayed = 0;
        if (lastPage != null) {
            pageLastDisplayed = Integer.parseInt(lastPage);
        }

// So calculate what page to display now
// First get the greatest page number so we know when to start at 0 again
        int max = 0;
        for (String pageId : pages) {
            int val = Integer.parseInt(pageId);
            if (val > max) {
                max = val;
            }
        }

        pageToDisplay = pageLastDisplayed;
        if (pageToDisplay >= max) {
            pageToDisplay = 0;
        }
// Check that the page is in the list to display
        boolean found = false;
        while (!found && pageToDisplay < 100) { // The 100 prevents an infinite loop
            pageToDisplay += 1;
            for (String pageId : pages) {
                if (pageId.equals(String.valueOf(pageToDisplay))) {
                    // Is in the list
                    found = true;
                    break;
                }
            }
        }

    } else if (request.getParameter("page") != null) {
        pageToDisplay = Integer.parseInt(request.getParameter("page"));
    } else {
        pageToDisplay = 12;
    }

// Now get the layout for the page
    String layout = BaseUtils.getProperty("env.sop.dash.page." + pageToDisplay + ".layout");
    int rows = Integer.parseInt(layout.substring(0, 1));
    int cols = Integer.parseInt(layout.substring(2));

// Calculate total frame count
    int frameCount = rows * cols;

// Now create the frameset html
    String rowStars = "";
    String colStars = "";
    for (int i = 0; i < rows; i++) {
        rowStars += "*,";
    }
    rowStars = rowStars.substring(0, rowStars.length() - 1);
    for (int i = 0; i < cols; i++) {
        colStars += "*,";
    }
    colStars = colStars.substring(0, colStars.length() - 1);
    String framesetStart = "<frameset frameborder=\"no\" framespacing=\"0\" border=\"0\" rows=\"";
    framesetStart += rowStars;
    framesetStart += "\" cols=\"";
    framesetStart += colStars;
    framesetStart += "\">";
    String framesetEnd = "</frameset>";
    String titleurl = request.getContextPath() + "/dash_title.jsp";
// Now get the screen size so we can tell each URL what size it should fit into
    int widthPerFrame = 1;
    int heightPerFrame = 1;
    try {
        int width = Integer.parseInt((String) session.getAttribute("width")) - 5;
        int height = Integer.parseInt((String) session.getAttribute("height")) - 10;
        widthPerFrame = width / cols;
        heightPerFrame = height * 95 / (100 * rows);
    } catch (Exception e) {
        // No screen values are in the session so go to index.jsp so thay are set
%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    </head>

    <body onload="window.location = '<%=request.getContextPath()%>/dash.jsp?width=' + screen.width + '&height=' + screen.height"/>
</html>
<%
        return;
    }
// Now write out what we have thus far

%>


<html>
    <head>
        <title>Smile Ops Dashboard</title>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
        <meta content="max-age=0" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Pragma"/>
        <meta content="must-revalidate" httpequiv="Cache-Control"/>
        <%            if (request.getParameter("pause") == null) {
        %>
        <meta http-equiv="refresh" content="<%=BaseUtils.getProperty("env.sop.dash.changesecs")%>;url=<%=request.getContextPath()%>/dash.jsp?page=<%=pageToDisplay%>" />
        <%
        } else {
        %>
        <meta http-equiv="refresh" content="3600">
        <%}
        %>
    </head>

    <frameset frameborder="no" framespacing="0" border="0" rows="5%,*">
        <frame scrolling="no" src="<%=titleurl%>" />
        <%=framesetStart%>

        <%
            for (int i = 1; i <= frameCount; i++) {
                // Write out each frame's html
                // Pass in the width and height parameters as calculated earlier
                String url = "";
                try {
                    url = BaseUtils.getProperty("env.sop.dash.page." + pageToDisplay + ".url." + i);
                    url += "&height=";
                    url += heightPerFrame;
                    url += "&width=";
                    url += widthPerFrame;
                } catch (Exception e) {
                }
        %>
        <frame  scrolling="no" src="<%=url%>" noresize="noresize"/>
        <%
            }
        %>

        <%=framesetEnd%>
    </frameset>
</html>

