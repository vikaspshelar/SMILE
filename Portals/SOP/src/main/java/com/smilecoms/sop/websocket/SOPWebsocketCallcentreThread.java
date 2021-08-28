/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sop.websocket;

import com.google.gson.Gson;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.sop.helpers.SyslogStatistic;
import com.smilecoms.sop.helpers.SyslogStatsSnapshot;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.action.CommandAction;
import org.asteriskjava.manager.response.ManagerResponse;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.websocket.Session;
import org.asteriskjava.manager.*;
import org.slf4j.*;

/**
 *
 * @author jaybeepee
 */
public class SOPWebsocketCallcentreThread extends Thread implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(SOPWebsocketCallcentreThread.class.getName());
    private final CopyOnWriteArraySet<Session> sessionList;
    private final Conductor app;
    public boolean mustRun = true;
    public boolean propsReady = false;
    private ManagerConnection managerConnection;
    private String managerHost = "";
    private String managerUserName = "";
    private String managerPassword = "";
    
    public SOPWebsocketCallcentreThread(Conductor app, CopyOnWriteArraySet<Session> sessions) {
        this.app = app;
        sessionList = sessions;
    }
    
    public void shutDown()
    {
        this.mustRun = false;
    }

    @Override
    public void run() {
        boolean connected = false;        
        BaseUtils.registerForPropsAvailability(this);
        while (mustRun && !connected) {
            try { 
                while (!propsReady) {
                    log.debug("properties not yet ready, will try again in 10 seconds");
                    sleep(10000);                    
                }
                managerHost = BaseUtils.getProperty("env.callcentre.asterisk.manager.host");
                managerUserName = BaseUtils.getProperty("env.callcentre.asterisk.manager.username");
                managerPassword = BaseUtils.getProperty("env.callcentre.asterisk.manager.password");

                ManagerConnectionFactory factory = new ManagerConnectionFactory(
                        managerHost, managerUserName, managerPassword);
                this.managerConnection = factory.createManagerConnection();
                // connect to Asterisk and log in
                managerConnection.login();
                log.debug("connected to Asterisk Manager");
                connected = true;
            } catch (Exception e) {
                log.debug("Failed to connect to AMI...trying again in 30 seconds");
                try {
                    sleep(30000);
                } catch (InterruptedException ex) {
                    log.warn("interrupted sleep..... ignoring");
                }
            }
        }
        
        CommandAction commandAction;
        ManagerResponse commandResponse = null;
        commandAction = new CommandAction("queue show");
        
        while (mustRun) {
            try {
                log.debug("Running callcentre thread");
                while (managerConnection.getState() != ManagerConnectionState.CONNECTED) {
                    log.warn("AMI not connected..... attempting to reconnect AMI");
                    try {
                        managerConnection.login();
                    } catch (Exception e) {
                        log.warn("failed to reconnect..... trying again in 30 seconds");
                        sleep(30000);
                    }
                }

                try {
                    commandResponse = managerConnection.sendAction(commandAction, 10000);
                } catch (Exception ex) {
                    log.warn("Exception trying to get queue stats");
                    log.warn("Error: ", ex);
                    sleep(30000);
                    continue;
                }
                //log.debug("response is " + commandResponse.getAttribute("__result__"));
               
                //process the text
                String json = processQueueResponse(commandResponse.getAttribute("__result__"));
                //now send data to all connected sockets
                for (Session socket : sessionList) {
                    socket.getAsyncRemote().sendText(json.trim());
                }
                sleep(BaseUtils.getIntProperty("env.callcentre.sop.dashboard.refresh.secs", 5)*1000);
            } catch (Exception ex) {
                log.warn("Error in callcentre thread (SOP websocket) [{}]", ex.toString());
            }
        }
        log.warn("CALLCENTRE THREAD SHUTTING DOWN");
    }
    
    private static final Pattern p = Pattern.compile("(\\S+)\\s+has\\s(\\d+)[\\s+,\\S+]*W:([\\d]*),\\sC:([\\d]*),\\sA:([\\d]*),\\sSL:(\\S*)[^\\d]*(.*)");    //(61000) has (10( callers
    private static final Pattern patternMembers = Pattern.compile("^([^\\(]*)\\(([^\\)]*)\\)[^\\(]\\(([^\\)]*)\\)[^\\)]*\\(([^\\)]*)[^\\d]*(\\d*)[^\\d]*(\\d*).*");    //Emmanuel Mdaka (Local/61009@from-queue/n) (Unavailable) has taken 2 calls (last was 75268 secs ago)
        
    public String processQueueResponse(String text) {
        String [] lines = text.split("\n");
        String queueName = "";      //current q we are parsing
        String reading = "";        //current state we are in within q parsing
        
        //Pattern p = Pattern.compile("(\\S+)\\s+has\\s(\\d+)");                      //(61000) has (10( callers
        QueueSummary queue = null;
        List<QueueSummary> queues = new ArrayList();
        
        for (String line:lines) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                queueName = m.group(1);
                queue = new QueueSummary();
                queue.queueName = queueName;
                queue.calls = Integer.parseInt(m.group(2).trim());  //calls being processed currently   
                queue.waiting = queue.calls;
//                queue.answered = Integer.parseInt(m.group(4).trim());
                queue.abandoned = Integer.parseInt(m.group(5).trim());
                queue.serviceLevel = m.group(6).trim();
                queue.serviceLevelTime = m.group(7);
                //get the statistics for this queue
                List<SyslogStatistic> ss = SyslogStatsSnapshot.getStats("callcentre", queue.queueName + ".*", "ccstat");
                for (SyslogStatistic s : ss) {
                    if (s.getName().contains("AVGCALLTIME")) {
                        queue.avgDuration = (int)s.getValue();
                    } else if (s.getName().contains("MAXCALLTIME")) {
                        queue.maxCallTime = (int)s.getValue();
                    } else if (s.getName().contains("AVGWAITTIME")) {
                        queue.avgWait = (int)s.getValue();
                    } else if (s.getName().contains("MAXWAITTIME")) {
                        queue.maxWait = (int)s.getValue();
                    } else if (s.getName().contains("ABANDONED")) {
                        queue.abandoned = (int)s.getValue();
                    } else if (s.getName().contains("CONNECTED")) {
                        queue.calls = (int)s.getValue();
                        queue.answered = queue.calls;
                    }
                }
                if ((queue.calls + queue.abandoned) > 0) {
                    queue.abandonRate = queue.abandoned*100 / (queue.calls + queue.abandoned);
                }
                continue;
            }            
            if (!queueName.isEmpty() && line.isEmpty()) {
                //tally up the agent stats for the queue data
                queues.add(queue);
                reading = "";
                queueName = "";
                continue;
            }            
            if (reading.isEmpty() && line.contains("Members:")) {
                reading = "members";
                continue;
            }
            if (reading.equalsIgnoreCase("members")) {
                m = patternMembers.matcher(line);
                if (m.find()) {
                    QueueMember qMember = new QueueMember();
                    qMember.name = m.group(1).trim();
                    qMember.id = (m.group(2).trim()).replace("Local/", "SIP/");
                    qMember.id = qMember.id.replaceAll("@from.*", "");
                    qMember.state = m.group(4).trim();
                    if (m.group(5).trim().length()<=0) {
                        qMember.calls = 0;
                    } else {
                        qMember.calls = Integer.parseInt(m.group(5).trim());
                    }
                    
                    if (m.group(6).trim().length() <= 0) {
                        qMember.lastcall = 0;
                    } else {
                        qMember.lastcall = Integer.parseInt(m.group(6).trim());
                    }
                   
                    if (line.contains("paused"))
                        qMember.status = "paused";
                    queue.members.put(qMember.id, qMember);
                    
                    //update queue values
                    if (qMember.status.equalsIgnoreCase("paused"))
                        queue.paused++;
                    
                    switch (qMember.state.toLowerCase()) {
                        case "unknown":
                            queue.loggedOff++;
                            break;
                        case "unavailable":
                            queue.loggedOff++;
                            break;
                        case "busy":
                            queue.busy++;
                            queue.agents++;
                            break;
                        case "not in use":
                            queue.agents++;
                            break;
                        case "in use":
                            queue.agents++;
                            queue.busy++;
                            break;
                        case "dynamic":
                            queue.agents++;
                    }
                }
            }
            
        }
        
//        for (QueueSummary q:queues) {
//            System.out.println(q.queueName + " has had " + q.calls + "calls, and there are "
//                    + q.members.entrySet().size() + " members");
//        }
        Gson gson = new Gson();
        String json = gson.toJson(queues);
        return json;
        
    }

    @Override
    public void propsAreReadyTrigger() {
        BaseUtils.deregisterForPropsAvailability(this);
        propsReady = true;
    }

    @Override
    public void propsHaveChangedTrigger() {
    }

    

}
class QueueMember {
    public String id;
    public String name;
    public String status;   //paused or not.
    public String state;    //whate state we are in now (Unavailable, not in use, busy, ringing)
    public int calls;
    public int lastcall; 
    
    public QueueMember() {
        id="";
        name="";
        status="";
        state="";
    }
}

class QueueSummary {
    public String queueName;
    public int calls;           //currently being processed
    public int waiting;         //waiting to be answered
    public int agents;
    public int loggedOff;
    public int busy;
    public int paused;
    public int answered;
    public int unAnswered;
    public int maxCallTime;
    public int abandoned;
    public float abandonRate;
    public float avgWait;
    public int avgDuration;
    public int maxWait;  
    public String serviceLevel;
    public String serviceLevelTime;
    public HashMap<String, QueueMember> members;

    public QueueSummary() {
        members = new HashMap<>();
    }
}
