<%@ include file="/include/sep_include.jsp" %>
<stripes:layout-render name="/layout/standard.jsp" title="${actionBean.stickyNoteType.typeName}">
    <stripes:layout-component name="contents">
        <script type="text/javascript">

            window.addEventListener("DOMContentLoaded", function () {
                initWebCam();
            }, false);
        </script>
        <stripes:form action="/Note.action" name="frm" onsubmit="clearErrorMessages(); return (alertValidationErrors() && checkRequiredDocuments());">
            <table class="clear">
                <stripes:hidden name="newStickyNote.typeName" value="${actionBean.stickyNoteType.typeName}"/>
                <stripes:hidden name="stickyNoteType.typeName" value="${actionBean.stickyNoteType.typeName}"/>
                <stripes:hidden name="newStickyNote.entityId" value="${actionBean.newStickyNote.entityId}"/>
                <stripes:hidden name="newStickyNote.entityType" value="${actionBean.newStickyNote.entityType}"/>
                <c:forEach items="${actionBean.stickyNoteType.fieldTypes}" var="field" varStatus="loop">
                    <stripes:hidden name="fieldTypes['${field.fieldName}']" value="${field.fieldType}"/>
                    <c:if test="${field.fieldType =='L'}">
                        <tr>
                            <td>${field.fieldName}:</td>
                            <td>
                                <stripes:text name="fieldValues['${field.fieldName}']"/>
                            </td>
                        </tr>
                    </c:if>
                    <c:if test="${field.fieldType =='B'}">
                        <tr>
                            <td>${field.fieldName}:</td>
                        </tr>
                        <tr>
                            <td colspan="2">
                                <stripes:textarea name="fieldValues['${field.fieldName}']" rows="10" cols="60"/>
                            </td>
                        </tr>
                    </c:if>
                    <c:if test="${field.fieldType =='D'}">
                        <tr>
                            <td>${field.fieldName}:</td>
                            <td>
                                <input readonly="true" type="text" name="fieldValues['${field.fieldName}']" class="required" size="10"/><input name="datePicker1" type="button" value=".." onclick="displayCalendar(this.previousSibling, 'yyyy/mm/dd', this)">
                            </td>
                        </tr>
                    </c:if>
                        
                        
                        
                        
                        
                    <c:if test="${field.fieldType =='P'}">
                        <tr>
                            <td>${field.fieldName}:</td>
                            <td>
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
            
            
            
            
            
            
            
            
        <c:if test="${fn:contains(field.fieldType,'|')}">
            <tr>
                <td>${field.fieldName}:</td>
                <td>
                    <stripes:select name="fieldValues['${field.fieldName}']" class="required">
                        <c:forEach items="${fn:split(field.fieldType,'|')}" var="dropdownentry" varStatus="loop1">
                            <stripes:option value="${dropdownentry}">${dropdownentry}</stripes:option>
                        </c:forEach>
                    </stripes:select>
                </td>
            </tr>
        </c:if>
    </c:forEach>
    <tr>
        <td colspan="2">
            <span class="button">
                <stripes:submit name="attachNote"/>
            </span>
        </td>
    </tr>
</table>
</stripes:form>
<br/>
</stripes:layout-component>
</stripes:layout-render>