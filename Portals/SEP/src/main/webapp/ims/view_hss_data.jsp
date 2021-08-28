<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="view.hss.data"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">


        <stripes:form action="/IMS.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">       
            <table class="clear">
                <tr>
                    <td><stripes:label for="IMSSubscriptionQuery.integratedCircuitCardIdentifier"/>:</td>
                    <td><stripes:text name="IMSSubscriptionQuery.integratedCircuitCardIdentifier" class="required" size="20" maxlength="20" onkeyup="validate(this,'^[0-9]{20,20}$','luhn emptyok')"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="IMSSubscriptionQuery.IMSPrivateIdentity"/>:</td>
                    <td><stripes:text name="IMSSubscriptionQuery.IMSPrivateIdentity" class="required" size="40" maxlength="60" onkeyup="validate(this,'^[0-9]{15,15}@private.*$','emptyok')"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="IMSSubscriptionQuery.IMSPublicIdentity"/>:</td>
                    <td><stripes:text name="IMSSubscriptionQuery.IMSPublicIdentity" class="required" size="40" maxlength="60" onkeyup="validate(this,'.*','emptyok')"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="IMSI"/>:</td>
                    <td><stripes:text name="IMSI" class="required" size="15" maxlength="15" onkeyup="validate(this,'^[0-9]{15,15}$','emptyok')"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="IMSSubscriptionQuery.IPAddress"/>:</td>
                    <td><stripes:text name="IMSSubscriptionQuery.IPAddress" class="required" size="15" maxlength="15"/></td>
                </tr>
                <tr>  
                    <td>
                        <span class="button">
                            <stripes:submit name="retrieveHSSData"/> 
                        </span>    
                    </td>
                </tr>                                
            </table>            
        </stripes:form>

        <br/>

        <c:if test="${actionBean.IMSSubscription != null}">

            <div id="entity">
                <table class="entity_header">
                    <tr>
                        <td>
                            <fmt:message key="subscription">
                                <fmt:param value="${actionBean.IMSSubscription.IMSSubscriptionId}"></fmt:param>
                            </fmt:message>
                        </td>
                    </tr>
                </table>

                <table class="clear">                
                    <c:if test="${actionBean.IMSSubscription.IMSSubscriptionId != -1}">
                        <tr>
                            <td colspan="2">
                                <b><fmt:message key="subscription.data"/></b>
                            </td>
                        </tr>
                        <tr>
                            <td>SCSCF Name:</td>
                            <td>${actionBean.IMSSubscription.SCSCFName}</td>
                        </tr>
                        <tr>
                            <td>Diameter Name:</td>
                            <td>${actionBean.IMSSubscription.diameterName}</td>
                        </tr>
                    </c:if>
                    <c:if test="${actionBean.IMSSubscription.IMSSubscriptionId == -1}">
                        <tr>
                            <td colspan="2">
                                <b><fmt:message key="no.subscription.data"/></b>
                            </td>
                        </tr>
                    </c:if>
                    <tr>
                        <td colspan="2">
                            <c:forEach items="${actionBean.IMSSubscription.IMSPrivateIdentities}" var="impi" varStatus="loop">
                                <table class="clear">                
                                    <tr>
                                        <td colspan="2">
                                            <b>
                                                <fmt:message key="private.identity"/>
                                            </b>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td>Private Identity:</td>
                                        <td>
                                            ${impi.identity}
                                            <stripes:link href="/ProductCatalog.action" event="retrieveServiceInstances">
                                                <stripes:param name="serviceInstanceQuery.identifierType" value="END_USER_PRIVATE"/>
                                                <stripes:param name="serviceInstanceQuery.identifier" value="${impi.identity}"/>
                                                service instances
                                            </stripes:link> |
                                            <stripes:link href="/IMS.action" event="retrievePCRFData">
                                                <stripes:param name="PCRFDataQuery.IMSPrivateIdentity" value="${impi.identity}"/>
                                                pcrf data
                                            </stripes:link>

                                        </td>
                                    </tr>

                                    <tr>
                                        <td>ICCID:</td>
                                        <td>${impi.integratedCircuitCardIdentifier}</td>
                                    </tr>

                                    <tr>
                                        <td>Status:</td>
                                        <td>${impi.status}</td>
                                    </tr>

                                    <tr>
                                        <td>Locked IMEI List:</td>
                                        <td>${impi.SIMLockedIMEIList}</td>
                                    </tr>

                                    <tr>
                                        <td>Info:</td>
                                        <td>${impi.info}</td>
                                    </tr>

                                    <td colspan="2">
                                        <c:forEach items="${impi.APNList}" var="apn" varStatus="loop1a">
                                            <table class="clear">
                                                <tr>
                                                    <td colspan="2">
                                                        <b>
                                                            <fmt:message key="apn.entry"/>
                                                        </b>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td>APN Name:</td>
                                                    <td>${apn.APNName}</td>
                                                </tr>
                                                <tr>
                                                    <td>Type:</td>
                                                    <td>${apn.type}</td>
                                                </tr>
                                                <tr>
                                                    <td>IPv4 Address:</td>
                                                    <td>${apn.IPv4Address}</td>
                                                </tr>
                                                <tr>
                                                    <td>IPv6 Address:</td>
                                                    <td>${apn.IPv6Address}</td>
                                                </tr>
                                            </table>
                                        </c:forEach>
                                    </td>

                                    <tr>
                                        <td colspan="2">
                                            <c:forEach items="${impi.implicitIMSPublicIdentitySets}" var="implicitset" varStatus="loop2">
                                                <table class="clear">                
                                                    <tr>
                                                        <td colspan="2">
                                                            <b>
                                                                <fmt:message key="implicit.set">
                                                                    <fmt:param>${implicitset.implicitSetId}</fmt:param>
                                                                </fmt:message>
                                                            </b>
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td colspan="2">
                                                            <c:forEach items="${implicitset.associatedIMSPublicIdentities}" var="identityassociation" varStatus="loop3">
                                                                <table class="clear">                
                                                                    <tr>
                                                                        <td colspan="2">
                                                                            <b>
                                                                                <fmt:message key="public.identity"/>
                                                                            </b>
                                                                        </td>
                                                                    </tr>
                                                                    <tr>
                                                                        <td>Public Identity:</td>
                                                                        <td>
                                                                            ${identityassociation.IMSPublicIdentity.identity}
                                                                            <stripes:link href="/ProductCatalog.action" event="retrieveServiceInstances">
                                                                                <c:if   test="${fn:substring(identityassociation.IMSPublicIdentity.identity,0,4) == 'tel:'}">
                                                                                    <stripes:param name="serviceInstanceQuery.identifierType" value="END_USER_E164"/>
                                                                                </c:if>
                                                                                <c:if   test="${fn:substring(identityassociation.IMSPublicIdentity.identity,0,4) != 'tel:'}">
                                                                                    <stripes:param name="serviceInstanceQuery.identifierType" value="END_USER_SIP_URI"/>
                                                                                </c:if>
                                                                                <stripes:param name="serviceInstanceQuery.identifier" value="${identityassociation.IMSPublicIdentity.identity}"/>
                                                                                service instances
                                                                            </stripes:link>
                                                                            |
                                                                            <c:choose>
                                                                                <c:when test='${not empty actionBean.IMSSubscription.SCSCFName}'>
                                                                                    <stripes:link href="/IMS.action" event="retrieveSCSCFIMPUData">
                                                                                        <stripes:param name="SCSCFIMPUQuery.IMPU" value="${identityassociation.IMSPublicIdentity.identity}"/>
                                                                                        <stripes:param name="SCSCFIMPUQuery.SCSCF" value='${fn:substringAfter(actionBean.IMSSubscription.SCSCFName,"sip:")}'/>
                                                                                        scscf data
                                                                                    </stripes:link>
                                                                                    <c:choose>
                                                                                        <c:when test="${identityassociation.IMSPublicIdentity.barring != 1}">
                                                                                            <c:if   test="${fn:substring(identityassociation.IMSPublicIdentity.identity,0,4) != 'tel:'}">
                                                                                                |
                                                                                                <stripes:link href="/IMS.action" event="deregisterIMPU">
                                                                                                    <stripes:param name="deregisterIMPUQuery.IMPU" value="${identityassociation.IMSPublicIdentity.identity}"/>
                                                                                                    <stripes:param name="deregisterIMPUQuery.SCSCF" value='${fn:substringAfter(actionBean.IMSSubscription.SCSCFName,"sip:")}'/>
                                                                                                    <stripes:param name="IMSSubscriptionQuery.IMSPrivateIdentity" value="${impi.identity}"/>
                                                                                                    deregister
                                                                                                </stripes:link>
                                                                                            </c:if>
                                                                                        </c:when>
                                                                                    </c:choose>
                                                                                </c:when>
                                                                                <c:when test='${empty actionBean.IMSSubscription.SCSCFName}'>
                                                                                    scscf data
                                                                                </c:when>
                                                                            </c:choose>
                                                                        </td>
                                                                    </tr>

                                                                    <tr>
                                                                        <td>Association User State:</td>
                                                                        <td>${identityassociation.userState}</td>
                                                                    </tr>
                                                                    <tr>
                                                                        <td>Public Identity User State:</td>
                                                                        <td>${identityassociation.IMSPublicIdentity.userState}</td>
                                                                    </tr>
                                                                    <tr>
                                                                        <td>Barring:</td>
                                                                        <td>${identityassociation.IMSPublicIdentity.barring}</td>
                                                                    </tr>
                                                                    <tr>
                                                                        <td>Can Register:</td>
                                                                        <td>${identityassociation.IMSPublicIdentity.canRegister}</td>
                                                                    </tr>
                                                                    <tr>
                                                                        <td>Service Profile:</td>
                                                                        <td>${identityassociation.IMSPublicIdentity.IMSServiceProfile.name}</td>
                                                                    </tr>
                                                                </table>
                                                                <br/>
                                                            </c:forEach>
                                                        </td>
                                                    </tr>
                                                </table>
                                                <br/>
                                            </c:forEach>
                                        </td>
                                    </tr>
                                </table>
                                <br/>
                            </c:forEach>
                        </td>
                    </tr>
                </table>
            </div>
        </c:if>

    </stripes:layout-component>


</stripes:layout-render>


