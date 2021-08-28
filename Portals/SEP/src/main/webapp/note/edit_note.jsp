<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="title.edit">
        <fmt:param>${actionBean.stickyNote.typeName}</fmt:param>
    </fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <script type="text/javascript">
            window.addEventListener("DOMContentLoaded", function () {
                initWebCam();
            }, false);
        </script>


        <stripes:form action="/Note.action" name="frm" onsubmit="clearErrorMessages(); return (alertValidationErrors() && checkRequiredDocuments());">
            <table class="clear">                                                
                <stripes:hidden name="stickyNote.noteId" value="${actionBean.stickyNote.noteId}"/>   
                <stripes:hidden name="stickyNote.version" value="${actionBean.stickyNote.version}"/>   
                <stripes:hidden name="stickyNote.lastModified" value="${actionBean.stickyNote.lastModified}"/>  
                <c:set var="hasPropertyOfTypeP" value="false"/>
                <tr>
                    <td colspan="2"><b><fmt:message key="general"/></b></td>                    
                </tr>  
                <tr>
                    <td><stripes:label for="type"/>:</td>
                    <td>${actionBean.stickyNote.typeName}</td>                    
                </tr> 
                <tr>
                    <td><stripes:label for="attached.to"/>:</td>
                    <td>${actionBean.stickyNote.entityType} with ID ${actionBean.stickyNote.entityId}</td>                    
                </tr>
                <tr>
                    <td><stripes:label for="created"/>:</td>
                    <td>${s:formatDateLong(actionBean.stickyNote.createdDateTime)}</td>                    
                </tr>
                <tr>
                    <td><stripes:label for="created.by"/>:</td>
                    <td>${actionBean.stickyNote.createdBy}</td>                    
                </tr>
                <tr>
                    <td><stripes:label for="last.modified"/>:</td>
                    <td>${s:formatDateLong(actionBean.stickyNote.lastModified)}</td>                    
                </tr>
                <tr>
                    <td><stripes:label for="last.modified.by"/>:</td>
                    <td>${actionBean.stickyNote.lastModifiedBy}</td>                    
                </tr>
                <tr>
                    <td colspan="2"><b><fmt:message key="field.data"/></b></td>                    
                </tr>

                <c:forEach items="${actionBean.stickyNote.fields}" var="field" varStatus="loop">
                    <stripes:hidden name="fieldTypes['${field.fieldId}']" value="${field.fieldType}"/>
                    <stripes:hidden name="fieldTypes['${field.fieldId}_P']" value="${field.fieldName}"/>
                    <c:if test="${field.fieldType =='L'}">
                        <tr>
                            <td>${field.fieldName}:</td>
                            <td><stripes:text name="fieldValues['${field.fieldId}']" value="${field.fieldData}"/></td>                    
                        </tr>  
                    </c:if>

                    <c:if test="${field.fieldType =='B'}">
                        <tr>
                            <td>${field.fieldName}:</td>
                        </tr>    
                        <tr>
                            <td colspan="2"><stripes:textarea name="fieldValues['${field.fieldId}']" rows="10" cols="60" value="${field.fieldData}"/></td>                    
                        </tr>
                    </c:if>

                    <c:if test="${field.fieldType =='D'}">
                        <tr>
                            <td>${field.fieldName}:</td>
                            <td>
                                <input readonly="true" type="text" name="fieldValues['${field.fieldId}']" class="required" size="10" value="${field.fieldData}"/><input name="datePicker1" type="button" value=".." onclick="displayCalendar(this.previousSibling, 'yyyy/mm/dd', this)">
                            </td>                                        
                        </tr>  
                    </c:if>
                    <c:if test="${field.fieldType =='P'}">
                        <c:set var="hasPropertyOfTypeP" value="true"/>
                        <tr>
                            <td>${field.fieldName}:</td>
                            <td>
                                ${field.fieldData}
                                <input type="hidden" name="fieldValues['${field.fieldId}']" value="${field.fieldData}"/>
                            </td>
                        </tr>
                    </c:if>   
                    <c:if test="${fn:contains(field.fieldType,'|')}">
                        <tr>
                            <td>${field.fieldName}:</td>
                            <td>
                                <stripes:select name="fieldValues['${field.fieldId}']" class="required" value="${field.fieldData}">
                                    <c:forEach items="${fn:split(field.fieldType,'|')}" var="dropdownentry" varStatus="loop">                             
                                        <stripes:option value="${dropdownentry}">${dropdownentry}</stripes:option>
                                    </c:forEach>
                                </stripes:select>
                            </td>
                        </tr>  
                    </c:if>

                </c:forEach>
                <c:if test="${hasPropertyOfTypeP eq 'true'}">
                    <tr>
                        <td colspan="2">
                            <stripes:select name="env.sticky.note.document.types"  id="customer.document.types" style="display:none">
                                <stripes:option value=""></stripes:option>
                                <c:forEach items="${s:getPropertyAsList('env.sticky.note.document.types')}" var="documentType" varStatus="loop1">
                                    <c:choose>
                                        <c:when test='${documentType.matches("^.*fingerprint.*$")}'>
                                        </c:when>
                                        <c:otherwise>
                                            <stripes:option value="${documentType}">
                                                <fmt:message  key="document.type.${documentType}"/>
                                            </stripes:option>
                                        </c:otherwise>
                                    </c:choose>
                                </c:forEach>
                            </stripes:select>
                            <table width="100%">
                                <tr>
                                    <td  colspan="2">
                                        <table class ="clear" width="100%">
                                            <tr>
                                                <td style="text-align: center">
                                                    <video class="video" id="video" width="320" height="240" autoplay></video>
                                                    <canvas id="canvas" width="1024" height="768" style="display:none"></canvas>
                                                </td>
                                            </tr>
                                        </table>
                                        <table class="clear"  width="100%" id="tblDocuments">
                                            <tbody>
                                                <c:forEach items="${actionBean.photographs}" varStatus="loop1">
                                                    <tr id="row${loop1.index}">
                                                        <td align="left" valign="top">
                                                            <stripes:select id="photoType${loop1.index}" name="photographs[${loop1.index}].photoType" onchange="validate(this,'^[a-zA-Z]{1,50}$','emptynotok')">
                                                                <c:choose>
                                                                    <c:when test='${actionBean.photographs[loop1.index].photoType.matches("^.*fingerprint.*$")}'>
                                                                        <stripes:option value="${actionBean.photographs[loop1.index].photoType}" selected="selected">
                                                                            <fmt:message  key="document.type.${actionBean.photographs[loop1.index].photoType}"/>
                                                                        </stripes:option>
                                                                    </c:when>
                                                                    <c:otherwise>
                                                                        <stripes:option value=""></stripes:option>
                                                                        <c:forEach items="${s:getPropertyAsList('env.sticky.note.document.types')}" var="documentType" varStatus="loop2">
                                                                            <c:choose>
                                                                                <c:when test='${documentType.matches("^.*fingerprint.*$")}'>
                                                                                </c:when>
                                                                                <c:otherwise>
                                                                                    <c:choose>
                                                                                        <c:when test='${actionBean.photographs[loop1.index].photoType == documentType}'>
                                                                                            <stripes:option value="${documentType}" selected="selected">
                                                                                                <fmt:message  key="document.type.${documentType}"/>
                                                                                            </stripes:option>
                                                                                        </c:when>
                                                                                        <c:otherwise>
                                                                                            <stripes:option value="${documentType}">
                                                                                                <fmt:message  key="document.type.${documentType}"/>
                                                                                            </stripes:option>
                                                                                        </c:otherwise>
                                                                                    </c:choose>
                                                                                </c:otherwise>
                                                                            </c:choose>
                                                                        </c:forEach>
                                                                    </c:otherwise>
                                                                </c:choose>
                                                            </stripes:select>
                                                        </td>
                                                        <c:choose>
                                                            <c:when test='${actionBean.photographs[loop1.index].photoType.matches("^.*fingerprint.*$")}'>
                                                                <stripes:hidden id="photoGuid${loop1.index}" name="photographs[${loop1.index}].photoGuid" value="${actionBean.photographs[loop1.index].photoGuid}"/>
                                                                <td align="left" valign="top">
                                                                    <div id="imgDiv${loop1.index}">
                                                                        <a href="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg" target="_blank">
                                                                            <img id="imgfile${loop1.index}" class="thumb" src="${pageContext.request.contextPath}/images/dummy-fingerprint.jpg"/>
                                                                        </a>
                                                                        <input class="file" id="file${loop1.index}" name="file${loop1.index}" type="file" style="visibility: hidden;" />
                                                                    </div>
                                                                </td>
                                                            </c:when>
                                                            <c:otherwise>
                                                        <input type="hidden" id="photoGuid${loop1.index}" name="photographs[${loop1.index}].photoGuid" value="${actionBean.photographs[loop1.index].photoGuid}"/>
                                                        <td align="left" valign="top">
                                                            <div id="imgDiv${loop1.index}">
                                                                <a href="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[loop1.index].photoGuid}" target="_blank">
                                                                    <img id="imgfile${loop1.index}" class="thumb" src="${pageContext.request.contextPath}/GetImageDataServlet;jsessionid=<%=request.getRequestedSessionId()%>?photoGuid=${actionBean.photographs[loop1.index].photoGuid}"/>
                                                                </a>
                                                            </div>
                                                        </td>
                                                        <td align="left" valign="top">
                                                            <input type = 'button' class='file' value = 'Browse ...' onclick ="javascript:document.getElementById('file${loop1.index}').click();" />
                                                            <input value="WebCam" class="file" id="snap" type="button" onclick="snapAndUpload('file${loop.index}')"/>
                                                            <input value="Remove" class="file" onclick="removeDocument(this, 'photographs');" type="button"/>
                                                            <input class="file" id="file${loop1.index}" name="file${loop1.index}" type="file" style="visibility: hidden;" />
                                                            <script type="text/javascript">
                                                                document.getElementById('file${loop1.index}').addEventListener('change', uploadDocument, false);
                                                            </script>
                                                        </td>
                                                    </c:otherwise>
                                                </c:choose>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <br/>
                            <input onclick="javascript:addRow('tblDocuments', 'photographs')" type="button" value="Add Document"/>
                            <br/>
                            <label  id="lblErrorMessages"></label> 
                            <br/>
                        </td>
                    </tr>
                </table>
            </td>
        </tr> 
    </c:if>
    <tr>
        <td colspan="2">
            <span class="button">
                <stripes:submit name="modifyNote"/>
            </span>                        
        </td>
    </tr>  
</table>            
</stripes:form>
<br/>



</stripes:layout-component>

</stripes:layout-render>

