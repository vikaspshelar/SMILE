<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="notes.attached.to">
        <fmt:param>${actionBean.stickyNoteEntityIdentifier.entityType}</fmt:param>
        <fmt:param>${s:getPhoneNumberFromSipURI(actionBean.stickyNoteEntityIdentifier.entityId)}</fmt:param>
    </fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <table class="green">                                
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
                            <input type="hidden" name="stickyNote.noteId" value="${note.noteId}"/>
                            <stripes:submit  name="retrieveNote"/>
                        </stripes:form>
                    </td>                         
                </tr>

            </c:forEach>
        </table>            

        <br/>
        <input type="button" value="Back" onclick="previousPage();"/>


    </stripes:layout-component>

</stripes:layout-render>

