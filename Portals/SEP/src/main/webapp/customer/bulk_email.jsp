<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="bulk.email"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <stripes:form action="/Customer.action" focus="" onsubmit="return alertValidationErrors();">   
            <table class="clear" width="99%">
                <tr>
                    <td>Bulk Email Group:</td>
                    <td>
                        <stripes:select name="generalQueryRequest.queryName">
                            <stripes:option value="Just Me">Just Me</stripes:option>
                            <c:forEach items="${s:getPropertyAsList('env.pm.generalquery.bulk.email.names')}" var="query" varStatus="loop"> 
                                <stripes:option value="${query}">${query}</stripes:option>
                            </c:forEach>
                        </stripes:select>                        
                    </td>
                </tr> 
                 <tr>
                    <td></td>
                    <td>OR</td>
                </tr>  
                <tr>
                    <td>Bulk EMAIL List:<br/><br/>
                        <b>Paste list of email addresses to send. One email address per line.</b></td>
                    <td>
                <stripes:textarea name="emailList" cols="55" rows="10"></stripes:textarea>
                </td>
                </tr>  
                <tr>
                    <td>Bulk Email Subject:</td>
                    <td>
                        <stripes:text name="subject"  maxlength="50" size="50" onkeyup="validate(this,'^.{1,200}$','emptynotok')" />                   
                    </td>
                </tr>     
                <tr>
                    <td>Bulk Email Body:<br/><br/>
                    <b>The body must be HTML and any images must exist and be available on the Internet at the URL specified in the HTML. Always send a test to a small group prior to sending to many customers.
                    {1} will be replaced with the customers first name.</b></td>
                    <td>
                        <stripes:textarea name="body" rows="30" cols="55"></stripes:textarea>
                    </td>
                </tr>   
                <tr>
                    <td></td>
                    <td>
                        <stripes:submit name="sendBulkEmail"/>
                    </td>
                </tr>   
                <tr>
                    <td><br/><br/></td>
                    <td></td>
                </tr> 
                <tr>
                    <td>Current Bulk Email Delivery Status: </td>
                    <td>
                        ${actionBean.bulkEmailStatus} &nbsp;
                         <stripes:link href="/Customer.action" event="showSendBulkEmail">
                            <img src="images/refresh.png" width="25" height="25"/>
                        </stripes:link>
                    </td>
                </tr>   
            </table>  
        </stripes:form>
    </stripes:layout-component>   
</stripes:layout-render>