<%@include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="scp.terms.and.conditions.title"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">    
    <stripes:layout-component name="contents">
        <div style="margin-top: 10px;" class="confirm_form sixteen columns">
            
            <div class="sixteen columns" style="text-align: justify; padding-top: 5px;">  
                <c:choose>
                    <c:when test="${!empty actionBean.customer.outstandingTermsAndConditions}">
                        <stripes:form  action="/Account.action" id="tc" style="display:block; float:left;">
                            <c:forEach items="${actionBean.customer.outstandingTermsAndConditions}" var="tcs" varStatus="loop">
                                <p style="margin-top: 10px;">
                                    <textarea name="dummy" cols="180" rows="10" readonly="true"  style="height: 147px; width: 910px;">
                                        <fmt:message key="${tcs}"/>
                                    </textarea>
                                </p>
                                <br/>
                                <p><stripes:checkbox name="acceptedTCs[${loop.index}]" value="${tcs}"/> <fmt:message key="scp.tc.accept.decline.to.proceed"/></p>
                                <br/>
                            </c:forEach>
                            <br/>
                            <input type="hidden" name="customer.customerId" value="${actionBean.customer.customerId}"/>
                            <input class="button_proceed" type="submit" name="acceptTermsAndConditions" value="Proceed"/>
                        </stripes:form>
                    </c:when>
                </c:choose>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>