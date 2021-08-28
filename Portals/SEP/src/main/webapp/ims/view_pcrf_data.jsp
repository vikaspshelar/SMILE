<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="view.pcrf.data"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <div id="entity">
            <table class="entity_header">
                <tr>
                    <td>
                        <fmt:message key="pcrf.data.identified.by">
                            <fmt:param>${actionBean.PCRFDataQuery.IMSPrivateIdentity}</fmt:param>
                        </fmt:message>
                    </td>                    
                </tr>
            </table>
            <br/>

            <table class="entity_header">
                <tr>
                    <td><fmt:message key="ipcan.session.data"></fmt:message></td>                    
                </tr>
            </table>

            <c:if test="${actionBean.PCRFData.hasIPCANSessions == 1}">
                <table class="green" width="99%"> 
                    <tr>
                        <th>Gx Session ID</th>
                        <th>Attached IP Address</th>
                        <th>Attached APN</th>
                        <th>Type</th>
                        <th>State</th>
                    </tr>

                    <c:forEach items="${actionBean.PCRFData.IPCANSessions}" var="ipcanSession" varStatus="loop">
                        <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">

                            <td>${ipcanSession.gxServerSessionId}</td>              
                            <td><a href="http:/${ipcanSession.bindingIdentifier}" title="Link to customer's router admin">${ipcanSession.bindingIdentifier} </a></td>   
                            <td>${ipcanSession.calledStationId}</td>   
                            <td>${ipcanSession.type}</td>
                            <td>${ipcanSession.state}</td>

                        </tr>
                    </c:forEach>             
                </table>     

                <table class="entity_header">
                    <tr>
                        <td>
                            <fmt:message key="afsession.session.data"></fmt:message>
                        </td>                    
                    </tr>
                </table>

                <c:if test="${actionBean.PCRFData.hasAFSessions == 1}">
                    <table class="green" width="99%"> 
                        <tr>
                            <th>Rx Session ID</th>
                            <th>Attached IP Address</th>
                            <th>Type</th>
                            <th>State</th>                    
                        </tr>


                        <c:forEach items="${actionBean.PCRFData.AFSessions}" var="afSession" varStatus="loop">
                            <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">

                                <td>${afSession.rxServerSessionId}</td>              
                                <td>${afSession.bindingIdentifier}</td>   
                                <td>${afSession.type}</td>
                                <td>${afSession.state}</td>

                            </tr>
                        </c:forEach>
                    </table>
                </c:if>

                <c:if test="${actionBean.PCRFData.hasAFSessions == 0}">
                    <table class="green" width="99%"> 
                        <tr>
                            <td><fmt:message key="no.afsessions"/></td>
                        </tr>
                    </table>
                </c:if>

                <table class="entity_header">
                    <tr>
                        <td><fmt:message key="pccule.data"></fmt:message></td>                    
                    </tr>
                </table>

                <c:if test="${actionBean.PCRFData.hasPCCRules == 1}">
                    <table class="green" width="99%"> 
                        <tr>
                            <th>PCC Rule Name</th>
                            <th>Attached IP Address</th>
                            <th>Type</th>                    
                        </tr>


                        <c:forEach items="${actionBean.PCRFData.PCCRules}" var="pccRule" varStatus="loop">
                            <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">

                                <td>${pccRule.PCCRuleName}</td>              
                                <td>${pccRule.bindingIdentifier}</td>   
                                <td>${pccRule.type}</td>

                            </tr>
                        </c:forEach>
                    </table>
                </c:if>
                <c:if test="${actionBean.PCRFData.hasPCCRules == 0}">
                    <table class="green" width="99%"> 
                        <tr>
                            <td><fmt:message key="no.pccrules"/></td>
                        </tr>
                    </table>
                </c:if>

            </c:if>
            <c:if test="${actionBean.PCRFData.hasIPCANSessions == 0}">
                <table class="green" width="99%"> 
                    <tr>
                        <td><fmt:message key="no.ipcansessions"/></td>
                    </tr>
                </table>
            </c:if>



        </div>

        <br/>

        <stripes:form action="/IMS.action">
            <stripes:hidden name="purgeUserDataQuery.IMSPrivateIdentity" value="${actionBean.PCRFDataQuery.IMSPrivateIdentity}"/>
            <stripes:hidden name="IMSSubscriptionQuery.IMSPrivateIdentity" value="${actionBean.PCRFDataQuery.IMSPrivateIdentity}"/>
            <stripes:submit name="purgeUserDataFromPGW" />
        </stripes:form>
        
        <stripes:form action="/IMS.action">
            <stripes:hidden name="purgeUserDataQuery.IMSPrivateIdentity" value="${actionBean.PCRFDataQuery.IMSPrivateIdentity}"/>
            <stripes:hidden name="IMSSubscriptionQuery.IMSPrivateIdentity" value="${actionBean.PCRFDataQuery.IMSPrivateIdentity}"/>
            <stripes:submit name="purgeUserDataFromMME" />
        </stripes:form>

        <input type="button" value="Back" onclick="previousPage();"/>



    </stripes:layout-component>
</stripes:layout-render>
