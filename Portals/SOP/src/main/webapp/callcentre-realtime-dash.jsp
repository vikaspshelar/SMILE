<%-- 
    Document   : callcentre-realtime-dash
    Created on : 15 Jan 2013, 5:25:32 PM
    Author     : jaybeepee
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*" %>

<% String queueFilter = request.getParameter("queue"); %>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Callcentre Dashboard</title>
        <link href="css/callcentre.css" rel="stylesheet" type="text/css" />
        <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js"></script>
        <script>
            //convert text to time
            String.prototype.toHHMMSS = function () {
                sec_numb    = parseInt(this);
                var hours   = Math.floor(sec_numb / 3600);
                var minutes = Math.floor((sec_numb - (hours * 3600)) / 60);
                var seconds = sec_numb - (hours * 3600) - (minutes * 60);
                
                if (hours   < 10) {hours   = "0"+hours;}
                if (minutes < 10) {minutes = "0"+minutes;}
                if (seconds < 10) {seconds = "0"+seconds;}
                var time    = hours+':'+minutes+':'+seconds;
                return time;
            }
            
            var connected = false;
            var initTable = true;
            
            $(document).ready(function() {
                var websocket = new WebSocket('ws://<%=BaseUtils.getProperty("env.portal.url")%>/sop/sop-callcentre');
                websocket.onopen = function() {
                    // Web Socket is connected. You can send data by send() method
                    websocket.send('dummydata');
                };
                websocket.onmessage = function (evt) {
                    if (connected == false && evt.data != "success") {
                        //console.log("ignoring message whilst not connected");
                        return;
                    }                
                    if (evt.data == "success"){
                        connected = true;
                        console.log("connected");
                        return;
                    }
                    console.log(evt.data);
                    var queues = JSON.parse(evt.data);
                    var td;
                    //check to see if this is the first data to build our table.
                    var summaryTable = document.getElementById("tbody_summary");
                    if (summaryTable.rows.length <= 0) {
                        //console.log("first data parcel received, building table rows");
                        
                        for (var k in queues){
                            var queueData = queues[k];
                            
                            if (<%=queueFilter==null%> || ((<%=queueFilter!=null%>) && (queueData["queueName"] == <%=queueFilter%>))) {
                                //insert new row for this queue data
                                var tr = summaryTable.insertRow(summaryTable.rows.length);
                                tr.id = queueData["queueName"];
                                if (summaryTable.rows.length % 2 == 0)
                                    tr.className="odd";

                                td = tr.insertCell(-1);
                                td.innerHTML = queueData["queueName"];
                                td.id = "queueName";
                                td = tr.insertCell(-1);
                                td.innerHTML = queueData["waiting"];
                                td.id = "waiting";
                                td = tr.insertCell(-1);
                                td.innerHTML = queueData["agents"];
                                td.id = "agents";
                                td = tr.insertCell(-1);
                                td.innerHTML = queueData["busy"];
                                td.id = "busy";
                                td = tr.insertCell(-1);
                                td.innerHTML = queueData["paused"];
                                td.id = "paused";
                                td = tr.insertCell(-1);
                                td.innerHTML = queueData["answered"];
                                td.id = "answered";
                                td = tr.insertCell(-1);
                                td.innerHTML = queueData["abandoned"] + " (" + queueData["abandonRate"] + "%)";
                                td.id = "abandoned";
                                
                                td = tr.insertCell(-1);
                                td.innerHTML = (JSON.stringify(queueData["avgWait"])).toHHMMSS();
                                td.id = "avgWait";
                                
                                td = tr.insertCell(-1);
                                td.innerHTML = (JSON.stringify(queueData["avgDuration"])).toHHMMSS();
                                td.id = "avgDuration";
    
                                td = tr.insertCell(-1);
                                td.innerHTML = queueData["serviceLevel"] + "  (" + queueData["serviceLevelTime"] + ")";
                                td.id = "serviceLevel";

                                var agentTablePrefix = "<table id=\"agent_summary_" + queueData["queueName"] + "\" width=\"100%\" border=\"0\" cellspacing=\"1\" cellpadding=\"1\" summary=\"Agent Summary\">" + 
                                            "<caption>Agent Real-Time Statistics - " + queueData["queueName"] + "</caption>" +
                                            "<thead>" +
                                            "<tr>" +
                                            "<th class=\"topleftround\" scope=\"col\">Agent</th>" + 
                                            "<th scope=\"col\">State</th>" + 
                                            "<th scope=\"col\">Duration</th>" + 
                                            "<th scope=\"col\">CLI</th>" + 
                                            "<th class=\"toprightround\" scope=\"col\">Last Call</th>" + 
                                            "</tr>" +
                                            "</thead>" +
                                            "<tbody id=\"tbody_agentsummary_" + queueData["queueName"] + "\">" +
                                            "</tbody>" +
                                            "</table>" + 
                                            "<p>&nbsp;</p>";

                                $('#agentsdiv').append(agentTablePrefix);
                                var agentTable = document.getElementById("tbody_agentsummary_" + queueData["queueName"]);
                                //traverse the agents for this queue
                                var members = queueData["members"];
    //                            console.log(members);
                                for (var j in members) {
                                    var member = members[j];
                                    var tr = agentTable.insertRow(agentTable.rows.length);
                                    tr.id = member["id"].replace("/", "-");
                                    if ((agentTable.rows.length % 2) == 0) {
                                        tr.className="odd";
                                    }
                                    td = tr.insertCell(-1);
                                    td.innerHTML = member["name"];
                                    td.id = "name";

                                    td = tr.insertCell(-1);
                                    td.innerHTML = member["state"];
                                    td.id = "state";

                                    td = tr.insertCell(-1);
                                    td.innerHTML = "";
                                    td.id = "duration";

                                    td = tr.insertCell(-1);
                                    td.innerHTML = "";
                                    td.id = "cli";

                                    td = tr.insertCell(-1);
                                    String 
                                    td.innerHTML = (JSON.stringify(member["lastcall"])).toHHMMSS() + " ago";
                                    td.id = "lastcall";
                                }
                            }
                            }

                        } else {
                            //update existing rows
                            for (var k in queues){
                                var queueData = queues[k];
                                var selector = "";

                                for (var key in queueData) {
                                    if (key ==  "id") {
                                        selector = "#table_summary #" + queueData['queueName'] + " #" + key.replace("/","-");// #61000 #queueName"
                                    } else {
                                        selector = "#table_summary #" + queueData['queueName'] + " #" + key;// #61000 #queueName"
                                    }
                                    var theTD = $(selector);
                                    try {
                                        if (key=="serviceLevel") {
                                            theTD.html(queueData[key] + "  (" + queueData["serviceLevelTime"] + ")");
                                        } else {
                                            theTD.html(queueData[key]);
                                        }
                                   
                                        if (key=="avgWait" || key=="avgDuration") {
                                            theTD.html((JSON.stringify(queueData[key])).toHHMMSS());
                                        }
                                        if (key=="abandoned") {
                                            theTD.html(queueData[key] + " (" + queueData["abandonRate"] + "%)");
                                        }
                                    }catch(err) {
                                        console.log("error populating table for key: " + key);
                                    }
                                }
                                
                                //now the agent tables
                                if (<%=queueFilter==null%> || ((<%=queueFilter!=null%>) && (queueData["queueName"] == <%=queueFilter%>))) {
                                    members = queueData["members"];
                                    for (var key in members) {
                                        var member = members[key];
//                                        console.log(member);
                                        
                                        for (x in member) {
                                            var selector = "#agent_summary_" + queueData["queueName"] + " #" + member["id"].replace("/","-") + " #" + x;
//                                            console.log(selector);
                                            var theTD = $(selector);
                                            try {
                                                if (x=="lastcall") {
                                                    theTD.html((JSON.stringify(member[x])).toHHMMSS() + " ago");
                                                } else {
                                                    theTD.html(member[x]);
                                                }
                                            }catch(err) {
                                                console.log("error populating table for key: " + x);
                                            }
                                        }
                                    }
                                 }
     
                            }
                        }
                    };
                    websocket.onclose = function() {

                    };
                });
        </script>
    </head>
    <body>
        <table id="table_summary" width="100%" border="0" cellspacing="1" cellpadding="1" summary="Queue Summary">
            <caption>Queue Real-Time Statistics</caption>
  <thead>
  <tr>
    <th class="topleftround" scope="col">Queue</th>
    <th scope="col">Calls Waiting</th>
    <th scope="col">Staffed</th> <!--agents-->
    <!--<th scope="col">Logged Off</th> <!--remove-->
    <th scope="col">Busy</th>
    <th scope="col">Paused</th>
    <th scope="col">Answered</th>
    <!--<th scope="col">Unanswered</th> <!--remove-->
    <th scope="col">Abandoned</th>
    <!--<th scope="col">Abandon Rate</th>-->
    <th scope="col">Avg Wait</th>
    <th scope="col">Avg duration</th>
    <th class="toprightround" scope="col">Service Level</th>
  </tr>
  </thead>
  <tbody id="tbody_summary">
  </tbody>
</table>
<p>&nbsp;</p>
<div id="agentsdiv">
</div>
<p>&nbsp;</p>
        
    </body>
</html>
