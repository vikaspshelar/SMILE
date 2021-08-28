<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="view.attached.note"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">    
    <stripes:layout-component name="contents">

        <div id="entity"> 
            <table class="entity_header">    
                <tr>
                    <td>${actionBean.stickyNote.typeName}</td>                        
                    <td align="right">                       
                        <stripes:form action="/Note.action" focus="">                                
                            <stripes:hidden name="stickyNote.noteId" value="${actionBean.stickyNote.noteId}"/>    
                            <stripes:hidden name="stickyNoteEntityIdentifier.entityType" value="${actionBean.stickyNote.entityType}"/>
                            <stripes:hidden name="stickyNoteEntityIdentifier.entityId" value="${actionBean.stickyNote.entityId}"/>
                            <stripes:hidden name="stickyNote.typeName" value="${actionBean.stickyNote.typeName}"/>
                            <stripes:select name="entityAction">
                                <stripes:option value="editNote"><fmt:message key="edit"/></stripes:option>
                                <stripes:option value="deleteNote"><fmt:message key="delete"/></stripes:option>
                                <stripes:option value="backToEntity">
                                    <fmt:message key="go.back.to">
                                        <fmt:param>${actionBean.stickyNote.entityType}</fmt:param>
                                    </fmt:message>
                                </stripes:option> 
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>    
            </table>
            <table class="clear">                    
                <tr>
                    <td colspan="2"><b><fmt:message key="general"/></b></td>                    
                </tr> 
                <tr>
                    <td><fmt:message key="note.id"/>:</td>
                    <td>${actionBean.stickyNote.noteId}</td>                    
                </tr>
                <tr>
                    <td><fmt:message key="type"/>:</td>
                    <td>${actionBean.stickyNote.typeName}</td>                    
                </tr> 
                <tr>
                    <td><fmt:message key="attached.to"/>:</td>
                    <td>${actionBean.stickyNote.entityType} <fmt:message key="with.id"/> ${s:getPhoneNumberFromSipURI(actionBean.stickyNote.entityId)}</td>
                </tr>
                <tr>
                    <td><fmt:message key="created"/>:</td>
                    <td>${s:formatDateLong(actionBean.stickyNote.createdDateTime)}</td>                    
                </tr>
                <tr>
                    <td><fmt:message key="created.by"/>:</td>
                    <td>${actionBean.stickyNote.createdBy}</td>                    
                </tr>
                <tr>
                    <td><fmt:message key="last.modified"/>:</td>
                    <td>${s:formatDateLong(actionBean.stickyNote.lastModified)}</td>                    
                </tr>
                <tr>
                    <td><fmt:message key="last.modified.by"/>:</td>
                    <td>${actionBean.stickyNote.lastModifiedBy}</td>                    
                </tr>
                <tr>
                    <td colspan="2"><b><fmt:message key="field.data"/>:</b></td>                    
                </tr>
                <c:forEach items="${actionBean.stickyNote.fields}" var="field" varStatus="loop">

                    <tr>
                        <td>${field.fieldName}:</td>
                        <td>${field.fieldData}</td>                    
                    </tr>  

                </c:forEach>
                <c:if test="${actionBean.stickyNote.typeName == 'Photographs'}">
                    <c:if test="${s:getListSize(actionBean.photographs) > 0}">
                        <tr>
                        <table class="clear">
                            <tr>
                                <td>
                                    <b><fmt:message key="scanned.documents"/>:</b>
                                </td>
                            </tr>

                            <c:forEach items="${actionBean.photographs}" varStatus="loop"> 
                                <tr id="row${loop.index}">
                                    <td align="left" valign="top">
                                        <fmt:message  key="document.type.${actionBean.photographs[loop.index].photoType}"/>
                                    </td>
                                    <td align="left" valign="top">
                                        <c:choose>
                                            <c:when test='${actionBean.photographs[loop.index].photoType.matches("^.*fingerprint.*$")}'>
                                                <a href="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg" target="_blank">
                                                    <img id="imgfile${loop.index}" class="thumb" src="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg"/>
                                                </a>
                                            </c:when>
                                            <c:otherwise>
                                                <a href="/sep/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[loop.index].photoGuid}" target="_blank">
                                                    <img id="imgfile${loop.index}" class="thumb" src="/sep/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[loop.index].photoGuid}"/>
                                                </a>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                        </table>
                        </tr>
                    </c:if>
                </c:if>

            </table>            
        </div>
        <br/>
        <input type="button" value="Back" onclick="previousPage();"/>

    </stripes:layout-component>

</stripes:layout-render>

