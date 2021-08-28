<%@ include file="/include/sep_include.jsp" %>


<c:if test="${!empty param.pageMessage}">    
    <div class="messages">
        <fmt:message key="${param.pageMessage}"/>
    </div>
    <br/>
</c:if>


<c:if test="${!empty actionBean.pageMessage}">    
    <div class="messages">
        <c:if test="${!empty actionBean.pageMessageParameters}">
            <fmt:message key="${actionBean.pageMessage}">
                <c:forEach items="${actionBean.pageMessageParameters}" var="parameter">
                    <fmt:param>${parameter}</fmt:param>
                </c:forEach>
            </fmt:message>
        </c:if>
        <c:if test="${empty actionBean.pageMessageParameters}">
            <fmt:message key="${actionBean.pageMessage}"/>
        </c:if>
    </div>
    <br/>
</c:if>
