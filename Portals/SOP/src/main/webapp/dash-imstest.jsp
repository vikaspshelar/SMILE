<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.smilecoms.commons.base.BaseUtils, java.util.*" %>

<html>
    <head>
        <title>Smile Ops Test Dashboard</title>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
        <meta content="no-cache" httpequiv="Pragma"/>
    </head>
    
    <%
	    String reg200URL=BaseUtils.getProperty("env.sop.imsdash.reg200");
	    String rapsURL=BaseUtils.getProperty("env.sop.imsdash.raps");
	    String successRapsURL=BaseUtils.getProperty("env.sop.imsdash.successraps");
	    String usersURL=BaseUtils.getProperty("env.sop.imsdash.users");
	    String regTimeURL=BaseUtils.getProperty("env.sop.imsdash.regtime");
	    String cdpTimeoutsURL=BaseUtils.getProperty("env.sop.imsdash.cdptimeouts");
	    String memoryURL=BaseUtils.getProperty("env.sop.imsdash.memory");
	    String cdpURL=BaseUtils.getProperty("env.sop.imsdash.cdp");	    
            String uarTimeoutsURL=BaseUtils.getProperty("env.sop.imsdash.uartimeouts");	    
            String lirTimeoutsURL=BaseUtils.getProperty("env.sop.imsdash.lirtimeouts");	    
            String marTimeoutsURL=BaseUtils.getProperty("env.sop.imsdash.martimeouts");	    
            String sarTimeoutsURL=BaseUtils.getProperty("env.sop.imsdash.sartimeouts");	    
            String aarTimeoutsURL=BaseUtils.getProperty("env.sop.imsdash.aartimeouts");	    
            String ccrTimeoutsURL=BaseUtils.getProperty("env.sop.imsdash.ccrtimeouts");	  
            String billedSecsURL=BaseUtils.getProperty("env.sop.imsdash.billedsecs");	  
            String initialCcrsURL=BaseUtils.getProperty("env.sop.imsdash.initialccrs");	  
            String interimCcrsURL=BaseUtils.getProperty("env.sop.imsdash.interimccrs");	  
            String finalCcrsURL=BaseUtils.getProperty("env.sop.imsdash.finalccrs");	  
            
            String registersURL=BaseUtils.getProperty("env.sop.imsdash.registers");	  
            String subscribesURL=BaseUtils.getProperty("env.sop.imsdash.subscribes");	  
            String invitesURL=BaseUtils.getProperty("env.sop.imsdash.invites");	  
            String messagesURL=BaseUtils.getProperty("env.sop.imsdash.messages");
            String failureResponsesURL=BaseUtils.getProperty("env.sop.imsdash.failureresponses");	
            String messageQueueURL=BaseUtils.getProperty("env.sop.imsdash.messagequeue");	
            
            
    %>
    
    <table style="width:100%">
        <tr align="center">
            <td><iframe height="300" seamless="seamless" id="reg200ok" src="<%=reg200URL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="raps" src="<%=rapsURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="successraps" src="<%=successRapsURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="users" src="<%=usersURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="regtime" src="<%=regTimeURL%>"></iframe></td>
           
        </tr>
        <tr align="center">
            <td><iframe height="300" seamless="seamless" id="cdptimeouts" src="<%=cdpTimeoutsURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="billedseconds" src="<%=billedSecsURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="initialccrs" src="<%=initialCcrsURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="interimccrs" src="<%=interimCcrsURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="finalccrs" src="<%=finalCcrsURL%>"></iframe></td>
            <!--<td><iframe height="300" seamless="seamless" id="uartimeouts" src="<%=uarTimeoutsURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="lirtimeouts" src="<%=lirTimeoutsURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="martimeouts" src="<%=marTimeoutsURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="sartimeouts" src="<%=sarTimeoutsURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="aartimeouts" src="<%=aarTimeoutsURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="ccrtimeouts" src="<%=ccrTimeoutsURL%>"></iframe></td>-->
        </tr>
        <tr align="center">
            <td><iframe height="300" seamless="seamless" id="registers" src="<%=registersURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="subscribes" src="<%=subscribesURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="messages" src="<%=messagesURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="invites" src="<%=invitesURL%>"></iframe></td>
            <td><iframe height="300" seamless="seamless" id="failureresponses" src="<%=failureResponsesURL%>"></iframe></td>
        </tr>
        <tr align="center">
            <td><iframe height="300" seamless="seamless" id="messagequeue" src="<%=messageQueueURL%>"></iframe></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
        </tr>
    </table>
    
    <iframe height="380" width="100%" seamless="seamless" id="memory" src="<%=memoryURL%>"></iframe>
    <iframe height="380" width="100%" seamless="seamless" id="cdp" src="<%=cdpURL%>"></iframe>
    
</html>