<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="callcentre.login"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <SCRIPT LANGUAGE="JavaScript">
            var opt = new OptionTransfer("list1","list2");
            opt.setAutoSort(true);
            opt.setDelimiter(",");
            //opt.setStaticOptionRegex("^(Bill|Bob|Matt)$");
            opt.saveRemovedLeftOptions("removedLeft");
            opt.saveRemovedRightOptions("removedRight");
            opt.saveAddedLeftOptions("addedLeft");
            opt.saveAddedRightOptions("addedRight");
            opt.saveNewLeftOptions("newLeft");
            opt.saveNewRightOptions("optionTransfer.newRight");
        </SCRIPT>   
        
        
        <div id="entity">
            <stripes:form action="/Callcentre.action" focus="" id="form_queues" onsubmit="return alertValidationErrors();">    
                <table class="clear" width="100%">    
                    <tr>
                        <td><stripes:label for="callcentre.select.extension"/>:</td>
                        <td><stripes:text name="CCQueueLoginData.CCAgentExtension" size="10"  maxlength="10" onkeyup="validate(this,'^[0-9]{5,10}$','emptynotok')"/></td>
                    </tr>
                    <tr>
                        <td><stripes:label for="callcentre.select.queues"/>:</td>
                        <td>
                            <table BORDER=0>
                                <tr>
                                    <td>
                                        <stripes:select name="list1" size="7" multiple="multiple" style="width: 250px" ondblclick="opt.transferRight()">
                                            <c:forEach items="${actionBean.availableQueues}" var="queue" varStatus="loop"> 
                                                <c:if test="${!empty queue}">
                                                    <stripes:option value="${s:retrieveQueueID(queue)}">${s:retrieveQueueName(queue)}</stripes:option>
                                                </c:if>
                                            </c:forEach>
                                        </stripes:select>                                
                                    </td>
                                    <td VALIGN=MIDDLE ALIGN=CENTER>
                                        <INPUT TYPE="button" NAME="right" VALUE="&gt;&gt;" ONCLICK="opt.transferRight()"><BR><BR>
                                        <INPUT TYPE="button" NAME="right" VALUE="All &gt;&gt;" ONCLICK="opt.transferAllRight()"><BR><BR>
                                        <INPUT TYPE="button" NAME="left" VALUE="&lt;&lt;" ONCLICK="opt.transferLeft()"><BR><BR>
                                        <INPUT TYPE="button" NAME="left" VALUE="All &lt;&lt;" ONCLICK="opt.transferAllLeft()">
                                    </td>
                                    <td>
                                        <stripes:select name="list2" size="7" multiple="multiple" style="width: 250px" ondblclick="opt.transferLeft()">
                                        </stripes:select>                                 
                                    </td>
                                </tr>
                            </table>  
                        </td>
                    </tr>                
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="callCentreLogin"/>
                            </span>                        
                        </td>
                    </tr>                                 
                </table>
                <stripes:hidden name="removedLeft" value=""/>
                <stripes:hidden name="removedRight" value=""/>
                <stripes:hidden name="addedLeft" value=""/>
                <stripes:hidden name="addedRight" value=""/>
                <stripes:hidden name="newLeft" value=""/>
                <stripes:hidden name="optionTransfer.newRight" value=""/>
            </stripes:form>
            <SCRIPT LANGUAGE="JavaScript">opt.init(document.getElementById("form_queues"));</SCRIPT>
        </div>		
        
    </stripes:layout-component>
</stripes:layout-render>

