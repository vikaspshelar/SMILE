<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.trouble.ticket"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        
        <script type="text/javascript">
            setTimeout(function(){try{setDescriptionTemplate();}catch(e){}},1);
        </script>

        <script type="text/javascript" xml:space="preserve">


            function setDescriptionTemplate() {
                templateBox = $('TTIssue.description');
                templateBox.value = "";
                var params = 'getFunctionalAreaDescriptionTemplate=&TTIssue.functionalArea=' + $F('TTIssue.functionalArea');
                var ajax = new Ajax.Request(
                '<%=request.getContextPath()%>' + '/TroubleTicket.action',
                {method: 'get', parameters: params, onSuccess:  populateTemplate});
            }

            function populateTemplate(response) {
                templateBox = $('TTIssue.description');
                templateBox.value = response.responseText;                
            }
        </script>

        <div id="entity">
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="customer">
                            <fmt:param>${actionBean.customer.customerId}</fmt:param>
                        </fmt:message>
                        ${actionBean.customer.customerId}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/TroubleTicket.action">                                
                            <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>                                
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveTroubleTickets"><fmt:message key="view.trouble.tickets"/></stripes:option>
                                <stripes:option value="retrieveTTCustomer"><fmt:message key="manage.customer"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>

            <stripes:form action="/TroubleTicket.action" focus="">
                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>                               
                <table class="clear">
                    <%--<tr>
                        <td><stripes:label for="trouble.ticket.fa"/>:</td>
                        <td><stripes:select name="TTIssue.functionalArea" id="TTIssue.functionalArea" onchange="setDescriptionTemplate();">
                                <c:forEach items="${actionBean.issueFunctionalAreas}" var="funcArea" varStatus="loop">
                                    <stripes:option value="${funcArea}">${funcArea}</stripes:option>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>--%>
                    <tr>
                        <td><stripes:label for="trouble.ticket.issuetype"/>:</td>
                        <td><stripes:select name="TTIssue.issueType">
                                <c:forEach items="${actionBean.TTMetaData.TTIssueTypeList}" var="ttissuetype" varStatus="loop">
                                    <stripes:option value="${ttissuetype.ID}">${ttissuetype.name}</stripes:option>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td><stripes:label for="trouble.ticket.summary"/>:</td>
                        <td><stripes:text name="TTIssue.summary" maxlength="50" size="50"/></td>
                    </tr>
                    <tr>
                        <td><stripes:label for="trouble.ticket.priority"/>:</td>
                        <td><stripes:select name="TTIssue.priority">
                                <c:forEach items="${actionBean.TTMetaData.TTPriorityList}" var="ttpriority" varStatus="loop">
                                    <stripes:option value="${ttpriority.ID}">${ttpriority.name}</stripes:option>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td><stripes:label for="trouble.ticket.duedate"/>:</td>
                        <td>
                            <input readonly="true" type="text" value="${s:formatDateShort(actionBean.TTIssue.dueDate)}" name="TTIssue.dueDate" class="required" size="10"/>
                            <input name="datePicker1" type="button" value=".." onclick="displayCalendar(document.forms[1].elements['TTIssue.dueDate'] ,'yyyy/mm/dd',this)">
                        </td>        
                    </tr>
                    <tr>
                        <td><stripes:label for="trouble.ticket.description"/>:</td>
                        <td><stripes:textarea  id="TTIssue.description" name="TTIssue.description" cols="80" rows="15"/></td>
                    </tr>                    
                    <tr>
                        <td><stripes:label for="trouble.ticket.watcher"/>:</td>
                        <td><stripes:checkbox name="watchIssue" value="TRUE"></stripes:checkbox></td>
                    </tr>

                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="insertTroubleTicket"/>
                            </span>                        
                        </td>
                    </tr>  				
                </table>            
            </stripes:form>
        </div>
    </stripes:layout-component>
</stripes:layout-render>

