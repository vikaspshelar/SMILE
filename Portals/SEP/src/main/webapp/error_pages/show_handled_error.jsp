<%@ include file="/include/sep_include.jsp" %>
<c:set var="title">
    <fmt:message key="error"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">   

        <br/><img src="images/dilbert.gif" border="0" width="90%"><br/>

        <%
            String sType = (String) request.getAttribute("error_type");
            String sDesc = (String) request.getAttribute("error_desc");
            String sCode = (String) request.getAttribute("error_code");
            String sTrace = (String) request.getAttribute("error_trace");
            String sSCA = (String) request.getAttribute("error_scarequest");

        %>        
        <c:set var="errType">
            error.<%=sType%>
        </c:set>
        <c:set var="errCode">
            error.<%=sCode%>
        </c:set>
        <br/>

        <div style="background-color: red; font-weight: bold; text-align: center">
            The information below may seem overwhelming but 99% of the time, 
            hidden somewhere in the error message will be a clue as to why you got the error. Please take the time to read the message and check if any of it makes sense to you.<br/>
            Only once you have read it and are sure none of it makes any sense, then escalate the issue to the IT team. Thanks<br/>            
        </div>
        <b><fmt:message key="error.type"/>:</b> <fmt:message key="${errType}"/><br/>
        <b><fmt:message key="error.description"/>:</b> <fmt:message key="${errCode}"/><br/>      
        <b><fmt:message key="error.technical.description"/>:</b> <%=sDesc%><br/>        
        <b><fmt:message key="error.code"/>:</b> <%=sCode%><br/>        
        <br/>

        <span class="button">
            <input type="button" name="stack.trace" onclick="return toggleMe('underlyingstacktrace')" value="<fmt:message key="stack.trace"/>"/>
        </span>        
        <table id="underlyingstacktrace" style="display:none">
            <tr><td>
                    <%=sTrace%>
                </td></tr>
        </table>
        <br/>
        <% if (!sSCA.equalsIgnoreCase("NA") && !sSCA.trim().equalsIgnoreCase("") && sSCA != null) {%>
        <span class="button">
            <input type="button" name="sca.request" onclick="return toggleMe('scarequest')" value="<fmt:message key="sca.request"/>"/>
        </span>  
        <table id="scarequest" style="display:none">
            <tr><td>
                    <pre><%=sSCA%></pre>
                </td></tr>
        </table>
        <% }%>
    </stripes:layout-component>

</stripes:layout-render>


