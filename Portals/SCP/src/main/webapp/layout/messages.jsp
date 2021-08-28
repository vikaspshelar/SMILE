<%@ include file="/include/scp_include.jsp" %>

<c:catch var="exception">
    <c:set var="temp">${actionBean.pageMessage}</c:set>
</c:catch>    
<div class="sixteen columns alpha">
    <c:if test="${empty exception && !empty param.pageMessage}">   
        <div class="messages">
            <table>
                <tr>
                    <td>
                        <img src="images/info.gif"/>
                    </td>
                    <td>
                        <fmt:message key="${param.pageMessage}"/>
                    </td>
                </tr> 
            </table>
        </div>
        <br/>
    </c:if>
    <c:if test="${empty exception && !empty actionBean.pageMessage}">    
        <div style="margin-top: 20px;" class="messages">
            <c:if test="${!empty actionBean.pageMessageParameters}">
                <table>
                    <tr>
                        <td>
                            <img src="images/info.gif"/>
                        </td>
                        <td>
                            <fmt:message key="${actionBean.pageMessage}">
                                <c:forEach items="${actionBean.pageMessageParameters}" var="parameter">
                                    <fmt:param>${parameter}</fmt:param>
                                </c:forEach>                    
                            </fmt:message>
                        </td>
                    </tr> 
                </table>
            </c:if>
            <c:if test="${empty actionBean.pageMessageParameters}">
                <table>
                    <tr>
                        <td><img src="images/info.gif"/></td>
                        <td>
                            <fmt:message key="${actionBean.pageMessage}"/>  
                        </td>
                    </tr>
                </table>

            </c:if>
        </div>
        <br/>
    </c:if>
</div>