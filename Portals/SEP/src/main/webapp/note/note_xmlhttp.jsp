<%-- any content can be specified here e.g.: --%>

<%@ page pageEncoding="UTF-8" %>
<%@ include file="/include/sep_include.jsp" %>
<%@page import="com.smilecoms.commons.stripes.SmileActionBean" %>
<c:choose>
    <c:when test="${s:getListSize(actionBean.stickyNoteList.stickyNotes) > 0}">
        <table class="clear">
            <tr>
                <td colspan="2"><b><fmt:message key="view.attached.note"/></b></td>
            </tr>
        </table>
        <table class="green" width="99%">                                
            <tr>
                <th><fmt:message key="note.id"/></th>  
                <th><fmt:message key="created"/></th>
                <th><fmt:message key="created.by"/></th>                    
                <th><fmt:message key="note.type"/></th>
                <th><fmt:message key="last.modified.by"/></th>
                <th><fmt:message key="view"/></th>                
            </tr>

            <c:forEach items="${actionBean.stickyNoteList.stickyNotes}" var="note" varStatus="loop">
                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                    <td>${note.noteId}</td>    
                    <td>${s:formatDateLong(note.createdDateTime)}</td>
                    <td>${s:getPhoneNumberFromSipURI(note.createdBy)}</td>                           
                    <td>${note.typeName}</td>                           
                    <td>${s:getPhoneNumberFromSipURI(note.lastModifiedBy)}</td>                            
                    <td>
                        <stripes:form action="/Note.action">
                            <stripes:hidden name="stickyNote.noteId" value="${note.noteId}"/>
                            <stripes:submit  name="retrieveNote"/>
                        </stripes:form>
                    </td>                         
                </tr>

            </c:forEach>

        </table> 
        <br/>
    </c:when>
    <c:otherwise>
        <%-- <table class="clear">
            <tr>
                <td colspan="2"><b><fmt:message key="view.attached.note"/></b></td>
            </tr>
        </table>
        <table class="green" width="99%"> 
            <tr>
                <td>
                    <fmt:message key="sticky.note.data.not.available"/>
                </td>
            </tr>
        </table>--%>
    </c:otherwise>
</c:choose>