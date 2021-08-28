

<%@page import="com.smilecoms.commons.base.BaseUtils"%>
<%
    String locationFilter = request.getParameter("loc");
    String nameFilter = request.getParameter("name");
    String typeFilter = request.getParameter("type");
    String title = request.getParameter("title");

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

    String unit = request.getParameter("unit");
    String lowerBound = request.getParameter("lower");
    String upperBound = request.getParameter("upper");
    String major = request.getParameter("major");
    String minor = request.getParameter("minor");
    String sumOrAvg = request.getParameter("agg");
    String highwarn = request.getParameter("highwarn");
    if (highwarn == null) {
        // backwards compatability
        highwarn = request.getParameter("warn");
    }
    String lowwarn = request.getParameter("lowwarn");

    if (highwarn == null) {
        highwarn = "999999999";
    }
    if (lowwarn == null) {
        lowwarn = "-999999999";
    }


    if (locationFilter == null || nameFilter == null || typeFilter == null || title == null || unit == null || lowerBound == null
            || upperBound == null || major == null || minor == null || sumOrAvg == null) {
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
            var gauge;
            var gaugeOptions = {
                chart: {
                    renderTo: 'dial',
                    type: 'gauge',
                    plotBackgroundColor: null,
                    plotBackgroundImage: null,
                    plotBorderWidth: 0,
                    plotShadow: false
                },
                title: {
                    text: '<%=title%>'
                },
                pane: {
                    startAngle: -150,
                    endAngle: 150,
                    background: [{
                            backgroundColor: {
                                linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
                                stops: [
                                    [0, '#FFF'],
                                    [1, '#333']
                                ]
                            },
                            borderWidth: 0,
                            outerRadius: '109%'
                        }, {
                            backgroundColor: {
                                linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
                                stops: [
                                    [0, '#333'],
                                    [1, '#FFF']
                                ]
                            },
                            borderWidth: 1,
                            outerRadius: '107%'
                        }, {
                            // default background
                        }, {
                            backgroundColor: '#DDD',
                            borderWidth: 0,
                            outerRadius: '105%',
                            innerRadius: '103%'
                        }]
                },
                // the value axis
                yAxis: {
                    min: 0,
                    max: <%=upperBound%>,
	            tickinterval: <%=major%>,
                    minorTickInterval: 'auto',
                    minorTickWidth: 1,
                    minorTickLength: 10,
                    minorTickPosition: 'inside',
                    minorTickColor: '#666',
	
//                    tickPixelInterval: 30,
                    tickWidth: 2,
                    tickPosition: 'inside',
                    tickLength: 10,
                    tickColor: '#666',
                    labels: {
                        step: 1,
                        rotation: 'auto'
                    },
                    title: {
                        text: '<%=unit%>'
                    },
                    plotBands: [{
                            from: 0,
                            to: <%=lowwarn%>,
                            color: '#55BF3B' // green
                        }, {
                            from: <%=lowwarn%>,
                            to: <%=highwarn%>,
                            color: '#DDDF0D' // yellow
                        }, {
                            from: <%=highwarn%>,
                            to: <%=upperBound%>,
                            color: '#DF5353' // red
                        }]        
                },
                series: [{
                        name: '<%=title%>',
                        data: [0],
                        tooltip: {
                            valueSuffix: ' <%=unit%>'
                        }
                    }]
	
            }
            
            $(document).ready(function() {
                gauge = new Highcharts.Chart(gaugeOptions);

        var websocket = new WebSocket('ws://<%=BaseUtils.getProperty("env.portal.url")%>/sop/sop-websocket');
                websocket.onopen = function() {
                    // Web Socket is connected. You can send data by send() method
//                    console.log("DEBUG: connecting...")
                    websocket.send('<%=request.getQueryString()%>');
                };
                websocket.onmessage = function (evt) {
//                    console.log("DEBUG: received message " + evt.data);
                    var retSeries = evt.data;//.split("&");
                    var seriesData = retSeries.split(":");
                    
                    var val = parseFloat(seriesData[2]);
                    var point = gauge.series[0].points[0];
                    point.update(val);
                };
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

        <div id="dial" style="height:<%=height%>;width:<%=width%>;margin:0 auto;" align="middle"></div>
    </body>
</html>