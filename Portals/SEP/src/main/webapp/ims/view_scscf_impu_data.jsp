<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="view.scscf.impu.data"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <c:if test="${actionBean.SCSCFIMPUData != null}">
            <div id="entity">
                <table class="entity_header">
                    <tr>
                        <td>
                            <fmt:message key="scscf.data.for">
                                <fmt:param value="${actionBean.SCSCFIMPUData.IMPU}"></fmt:param>
                            </fmt:message>
                        </td>
                    </tr>
                </table>

                <table class="clear">  
                    <tr>
                        <td>Associated IMPI:</td>
                        <td>${actionBean.SCSCFIMPUData.subscription.IMPI}</td>
                    </tr>
                    <tr>
                        <td>State:</td>
                        <td>${actionBean.SCSCFIMPUData.state}</td>
                    </tr>
                    <tr>
                        <td>Barring:</td>
                        <td>${actionBean.SCSCFIMPUData.barring}</td>
                    </tr>
                    <tr>
                        <td>ccf1:</td>
                        <td>${actionBean.SCSCFIMPUData.CCF1}</td>
                    </tr>
                    <tr>
                        <td>ccf2:</td>
                        <td>${actionBean.SCSCFIMPUData.CCF2}</td>
                    </tr>
                    <tr>
                        <td>ecf1:</td>
                        <td>${actionBean.SCSCFIMPUData.ECF1}</td>
                    </tr>
                    <tr>
                        <td>ecf2:</td>
                        <td>${actionBean.SCSCFIMPUData.ECF2}</td>
                    </tr>
                    <tr>
                        <td colspan="2"><b>Contacts</b></td>
                    </tr>
                    
                    <tr>
                    <c:forEach items="${actionBean.SCSCFIMPUData.contact}" var="contact" varStatus="loop">    
                    <table class="clear">
                        <tr>
                            <td colspan="2">
                                ${contact.aoR}
                            </td>
                        </tr>
                        <tr>
                            <td>Client:</td>
                            <td>${contact.userAgent}</td>
                        </tr>
                        <tr>
                            <td>Expires:</td>
                            <td>${contact.expires}</td>
                        </tr>
                    </table>
                    </c:forEach>
                    </tr>
                </table>
            </div>
        </c:if>

            </stripes:layout-component>


        </stripes:layout-render>


