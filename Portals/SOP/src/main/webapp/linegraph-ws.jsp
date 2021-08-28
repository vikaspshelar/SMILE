<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="com.smilecoms.commons.base.BaseUtils"%>
<%
    //Global chart params
    String title = request.getParameter("title");
    String unit = request.getParameter("unit");
    int numPoints = 0;
    boolean slideEnabled = (request.getParameter("slide")!=null)?true:false;
    
    if (slideEnabled) {
        numPoints = Integer.parseInt(request.getParameter("slide"));
    }    
    //series-specific chart params and filters
    String locationFilter[] = request.getParameterValues("loc");
    String nameFilter[] = request.getParameterValues("name");
    String typeFilter[] = request.getParameterValues("type");
    String descFilter[] = request.getParameterValues("desc");
    String aggFilter[] = request.getParameterValues("agg");

    //make sure all input data is supplied for multi-series
    boolean pass = ((locationFilter.length & nameFilter.length & typeFilter.length & descFilter.length & aggFilter.length) == locationFilter.length);
    if (!pass) {
%>
Error: Input data not complete - URL must be of form: linegraph-ws.jsp?&title=My Title&unit=X&desc=Series Name&loc=.*&name=.*&type=.*&agg=sum|avg
<%
        return;
    }

    //flatten the string array with a glue character of ","
    String loc = StringUtils.join(locationFilter, ",");
    String name = StringUtils.join(nameFilter, ",");
    String type = StringUtils.join(typeFilter, ",");
    String desc = StringUtils.join(descFilter, ",");
    String agg = StringUtils.join(aggFilter, ",");

    //build filter to pass over websocket to server
    String filter = "loc=" + loc + "&name=" + name + "&type=" + type + "&agg=" + agg + "&desc=" + desc;

    float heightF = Float.parseFloat(request.getParameter("height") != null ? request.getParameter("height") : "900");
    float widthF = Float.parseFloat(request.getParameter("width") != null ? request.getParameter("width") : "800");
    // Get the lesser of the width and height
    int height = 0;
    int width = 0;
    if (widthF <= heightF) {
        //Tall and narrow - width is the limiting factor and hight can be bigger
        height = (int) (widthF * 1.04F);
        width = (int) (widthF);
    }
    if (widthF > heightF) {
        //Long and Thin - height is the limiting factor and width can be bigger
        height = (int) (heightF);
        width = (int) (heightF * 0.96F);
    }
    // Compensate for things being just off square
    if (height > heightF) {
        height = (int) heightF;
    }
    if (width > widthF) {
        width = (int) widthF;
    }

    //if (locationFilter == null || nameFilter == null || typeFilter == null || title == null || unit == null || lowerBound == null
    //        || upperBound == null || major == null || minor == null || sumOrAvg == null) {
    if (!true) {
%>
Error: URL must be of form: dial-ws.jsp?loc=.*&name=.*&type=.*&width=800&height=600&title=My Title&refreshsecs=10&unit=X&lower=X&upper=X&major=X&minor=X&agg=sum_avg
<%
        return;
    }
%>


<html>
    <head>
        <title><%=title%></title>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
        <meta content="max-age=0" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Cache-Control"/>
        <meta content="no-cache" httpequiv="Pragma"/>
        <meta content="must-revalidate" httpequiv="Cache-Control"/> 

        <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js"></script>

        <script>
            <% if (slideEnabled) {%>
                var jsMustSlide=true; 
            <%} else {%>
                var jsMustSlide=false;
            <% }%>    
                
            var linegraph;
            var linegraphOptions = {
                chart: {
                    renderTo: 'chartline',
                    type: 'spline',
                    marginRight: 50,
                    height: 350
                },
                title: {
                    text: '<%=title%>'
                },
                xAxis: {
                    type: 'datetime',
                    tickPixelInterval: 150
                },
                yAxis: {
                    title: {
                        text: '<%=unit%>'
                    },
                    plotLines: [{
                            value: 0,
                            width: 1,
                            color: '#808080'
                        }]
                },
                tooltip: {
                    formatter: function() {
                        return '<b>'+ this.series.name +'</b><br/>'+
                            Highcharts.dateFormat('%Y-%m-%d %H:%M:%S', this.x) +'<br/>'+
                            Highcharts.numberFormat(this.y, 2);
                    }
                },
                legend: {
                    enabled: true
                },
                exporting: {
                    enabled: true
                }
            }
            
            $(document).ready(function() {
                linegraph = new Highcharts.Chart(linegraphOptions);

                var websocket = new WebSocket('ws://<%=BaseUtils.getProperty("env.portal.url")%>/sop/sop-websocket');
                websocket.onopen = function() {
                    // Web Socket is connected. You can send data by send() method
                    console.log("WS linegraph connect - about to send <%=filter%>");
                    websocket.send('<%=filter%>');
                };
                websocket.onmessage = function (evt) {
                    var retSeries = evt.data.split("&");
                    var mustSlide = "false";
                    
                    console.log("received: " + evt.data);
                    var x = (new Date()).getTime();
                    for (var i=0; i<retSeries.length; i++) {
                        var seriesData = retSeries[i].split(":");
                        var dataVal = parseFloat(seriesData[2]);
                        //dataVal = dataVal/1000000;

                        if (linegraph.series.length<retSeries.length) { //we havent built the initial series yet
                            var series = {
                                id: seriesData[0],
                                name: seriesData[0],
                                marker: {enabled: false},
                                data: []
                            }
                            series.data.push([
                                x,
                                dataVal
                            ]);
                            
                            linegraph.addSeries(series);
                            linegraph.redraw();
                        }
                        if (jsMustSlide && linegraph.series[i].data.length><%=numPoints%>) {
                            linegraph.series[i].addPoint([x, dataVal], true, true);
                        } else {
                            linegraph.series[i].addPoint([x, dataVal], true, false); 
                        }
                        linegraph.redraw();
                    };
                        
                }

                websocket.onclose = function() {
                };
            });
            
        </script>
    </head>
    <body style="margin: 10px 10px 10px 10px">
        <script src="javascript/highchart/highcharts.js"></script>
        <script src="javascript/highchart/highcharts-more.js"></script>
        <script src="javascript/highchart/modules/exporting.js"></script>
        <script type="text/javascript" src="javascript/highchart/themes/gray.js"></script>

        <div id="chartline" style="margin:0 auto;" align="middle"></div>
    </body>
</html>