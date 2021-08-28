<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="scp.trouble.ticket.cc.desk"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function() {
                makeMenuActive('Help');
            }

        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <c:if test="${!actionBean.ttStatus}">
            
            <div class="contact_form offset-by-six">
                <font style="font-size:12px; color:#75B343; font-weight:bold;"><fmt:message key="scp.support.desk.msg"/></font><br/>
                <stripes:form action="/SCPTroubleTicket.action" focus="">
                    Name<br>
                    <input type='text' value="${actionBean.customer.firstName}"disabled='disabled'/><br><br>
                    Email<br>
                    <input type='text' value="${actionBean.customer.emailAddress}"disabled='disabled'/><br><br>
                    Contact number<br>
                    <input type='text' value="${actionBean.customer.alternativeContact1}"disabled='disabled'/><br><br>
                    Please select category<br>
                    <select name="TT_FIXED_FIELD_Category">
                        <c:forEach items="${s:getPropertyAsList('env.scp.jira.category.values')}" var="category">
                            <option value="${category}">${category}</option>
                        </c:forEach>
                    </select><br><br>
                    Comments<br>
                    <stripes:textarea  id="scpTTDescription" name="scpTTDescription" cols="65" rows="5" style="height: 157px; width: 306px;"/><br>
                    <input type="submit" class="submit_btn" name="insertTroubleTicket" value=""/>
                </stripes:form>
            </div>


        </c:if>
        <c:if test="${actionBean.ttStatus}">  
            <div style="align: right;"> </div>                    
        </c:if>

    </stripes:layout-component>   
</stripes:layout-render>

