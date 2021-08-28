<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
   <fmt:message key="allowed.note.types"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
   
        <h3><fmt:message key="attach.a.note.from.the.list.of.allowed.types"/></h3>
            <table class="green">                                
                
                <tr>
                    <th><fmt:message key="name"/></th>
                    <th><fmt:message key="use"/></th>
                </tr>
                <c:forEach items="${actionBean.stickyNoteTypeList.stickyNoteTypes}" var="type" varStatus="loop">
                    <stripes:form action="/Note.action">               
                    <stripes:hidden name="stickyNoteType.typeName" value="${type.typeName}"/>
                    <stripes:hidden name="newStickyNote.entityId" value="${actionBean.stickyNoteEntityIdentifier.entityId}"/>
                    <stripes:hidden name="newStickyNote.entityType" value="${actionBean.stickyNoteEntityIdentifier.entityType}"/>
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>${type.typeName}</td>
                        <td>                                                             
                            <stripes:submit  name="addNote"/>
                        </td>                                               
                    </tr>
                    </stripes:form>
                </c:forEach>
            </table>            
           
        <br/>
        <input type="button" value="Back" onclick="previousPage();"/>

            
        
    </stripes:layout-component>
          
</stripes:layout-render>

