<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="edit.customer.permission"/>
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
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="customer"/> ${actionBean.customer.firstName} ${actionBean.customer.lastName}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/Customer.action">                                
                            <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>                              
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveCustomer"><fmt:message key="manage.customer"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table>         
            <stripes:form action="/Customer.action" focus="" id="form_edit">
                <stripes:hidden name="customer.version" value="${actionBean.customer.version}"/>
                <stripes:hidden name="customer.customerId" value="${actionBean.customer.customerId}"/>
                <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                <table class="clear" width="100%">                                
                    <tr>
                        <td><stripes:label for="user.group"/>:</td>
                        <td>
                            <table BORDER=0>
                                <tr>
                                    <td>
                                        <stripes:select name="list1" size="15" multiple="multiple" style="width: 250px" ondblclick="opt.transferRight()">
                                            <c:forEach items="${actionBean.availableUserGroups}" var="usergroup" varStatus="loop"> 
                                                <c:if test="${!empty usergroup}">
                                                    <stripes:option value="${usergroup}">${usergroup}</stripes:option>
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
                                        <stripes:select name="list2" size="15" multiple="multiple" style="width: 250px" ondblclick="opt.transferLeft()">
                                            <c:if test="${!empty actionBean.customer.securityGroups}">
                                                <c:forEach items="${actionBean.customer.securityGroups}" var="usergroup" varStatus="loop"> 
                                                    <c:if test="${!empty usergroup}">
                                                        <stripes:option value="${usergroup}">${usergroup}</stripes:option>
                                                    </c:if>
                                                </c:forEach>
                                            </c:if>
                                        </stripes:select>                                 
                                    </td>
                                </tr>
                            </table>  
                        </td>
                    </tr>                
                    <tr>
                        <td colspan="2">
                            <span class="button">
                                <stripes:submit name="updateCustomerPermission"/>
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
            <SCRIPT LANGUAGE="JavaScript">opt.init(document.getElementById("form_edit"))</SCRIPT>
        </div>		

    </stripes:layout-component>
</stripes:layout-render>

