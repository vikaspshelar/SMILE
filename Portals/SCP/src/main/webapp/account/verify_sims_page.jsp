<%@include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="my.simcards"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}"> 
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function () {
            makeMenuActive('Profile_SimVerifyPage');
            }
        </script>
    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <div style="margin-top: 10px;" class="sixteen columns alpha">
            
        <br/>      

           
                <c:choose>
                    <c:when test="${s:getListSize(actionBean.customer.productInstances) > 0}">
                        <c:if test="${fn:length(actionBean.regulatorResponse)>0}">
                            <span style="font-weight: 900; color: cadetblue; border: 1px solid darkgreen; padding: 10px; border-radius: 5px">${actionBean.regulatorResponse}</span>
                            <br><br>
                        </c:if>
                            
                        <table class="greentbl" width="100%">
                            <tr>                                
                                <th>Simcard</th>                                                               
                                <th>Regulator</th>                                
                            </tr>

                            <c:set var="SIs" value="${actionBean.serviceInstanceList.serviceInstances}"/>
                            <c:set var="PIList" value="${actionBean.productInstanceList.productInstances}"/>
                            <c:set var="counter" value="${1}" />

                            <c:forEach items="${actionBean.accountList.accounts}" var="account" varStatus="loop">
                                <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">       
                                    <c:forEach items="${SIs}" var="servInstance" varStatus="loop2">
                                        <c:if test="${servInstance.accountId == account.accountId}">

                                            <c:forEach items="${PIList}" var="prodInstance" varStatus="loop3">
                                                <c:if test="${prodInstance.productInstanceId == servInstance.productInstanceId}">                                                            

                                                                <c:set var="simNumber" value="${s:getServiceInstancePhoneNumber(servInstance.serviceInstanceId)}"/>
                                                                <c:if test="${fn:length(fn:trim(simNumber))>0 && simNumber != 'NA'}"> 
                                                                                                    
                                                                            <td>
                                                                            <fmt:message key="scp.simcard.number"/>: ${s:getServiceInstanceICCID(servInstance.serviceInstanceId)} <br/>
                                                                            <fmt:message key="scp.service.phone.number"/>: ${s:getServiceInstancePhoneNumber(servInstance.serviceInstanceId)}<br/>
                                                                            
                                                                                <c:if test="${servInstance.status=='AC'}">  
                                                                                    <fmt:message key="status"/>:<span style='font-weight: 600; color: #75b343'>Active</span>
                                                                                </c:if>    
                                                                                <c:if test="${servInstance.status=='TD'}">  
                                                                                    <fmt:message key="status"/>:<span style='font-weight: 600; color: orange'>Temporarily Deactivated</span>
                                                                                </c:if>        
                                                                                <c:if test="${servInstance.status=='DE'}">  
                                                                                    <fmt:message key="status"/>:<span style='font-weight: 600; color: red'>Deleted</span>
                                                                                </c:if>
                                                                            </td>
                                                                            <td>
                                                                                <stripes:form action="/Account.action">                                                                                    
                                                                                    <input type="hidden" name="iccid" value="${s:getServiceInstanceICCID(servInstance.serviceInstanceId)}"/>
                                                                                    <input type="hidden" name="phoneNumber" value="${s:getServiceInstancePhoneNumber(servInstance.serviceInstanceId)}"/>
                                                                                    <input type="hidden" name="simStatus" value="${servInstance.status}"/>                                                                                    
                                                                                    <input type="submit" style="border-radius:10px; padding:2px; border: 1px solid green; background: transparent;color: green;font-weight: 900" name="checkRegStatus" value="CheckRegStatus"/>
                                                                                    <input type="submit" style="border-radius:10px;padding:2px; border: 1px solid green; background: transparent; color: green" name="removeSimcard" value="RemoveSim"/> 
                                                                                    <input type="submit" style="border-radius:10px;padding:2px; border: 1px solid green; background: transparent;color:green" name="showVerifyRegulatorSim" value="RequestSimReg"/> 
                                                                                </stripes:form>
                                                                            </td>
                                                                        
                                                                </c:if>

                                                    <c:set var="counter" value="${counter+1}" />
                                                </c:if>
                                            </c:forEach>
                                        </c:if>
                                    </c:forEach>
                            <c:set var="counter" value="${1}" />
                            </tr>
                            </c:forEach>
                        </table>

                        <c:if   test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit 
                                        || actionBean.customerQuery.productInstanceOffset != 0}">
                                <table width="99%">
                                    <tr>
                                        <td align="left">
                                            <c:if   test="${actionBean.customerQuery.productInstanceOffset != 0}">
                                                <stripes:form action="/Account.action">
                                                    <input type="submit" style="background-color: #75b343; border: medium none; border-radius: 17px; color: #ffffff; cursor: pointer; font-family: 'UniversLT67BoldCn'; font-size: 15px; height: 34px; line-height: 34px;text-transform: uppercase; margin-bottom: 15px; padding-left: 15px; padding-right: 15px;" name="previousAccountsPage" value="Back"/>
                                                    <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                                    <input type="hidden" name="customerQuery.productInstanceOffset" value="${actionBean.customerQuery.productInstanceOffset - actionBean.customerQuery.productInstanceResultLimit}" />
                                                </stripes:form>
                                            </c:if>
                                        </td>
                                        <td align="right">
                                            <c:if   test="${s:getListSize(actionBean.customer.productInstances) == actionBean.customerQuery.productInstanceResultLimit}">
                                                <stripes:form action="/Account.action">
                                                    <input type="submit" style="background-color: #75b343; border: medium none; border-radius: 17px; color: #ffffff; cursor: pointer; font-family: 'UniversLT67BoldCn'; font-size: 15px; height: 34px; line-height: 34px;text-transform: uppercase; margin-bottom: 15px; padding-left: 15px; padding-right: 15px;" name="nextAccountsPage" value="Next"/>
                                                    <stripes:hidden name="customerQuery.customerId" value="${actionBean.customer.customerId}"/>
                                                    <input type="hidden" name="customerQuery.productInstanceOffset" value="${actionBean.customerQuery.productInstanceOffset + actionBean.customerQuery.productInstanceResultLimit}" />
                                                </stripes:form>
                                            </c:if>
                                        </td>
                                    </tr>
                                </table>
                        </c:if>

                    </c:when>
                    <c:otherwise>
                        <p><fmt:message key="no.service.instances.account"/></p>
                    </c:otherwise>
                </c:choose>                    
           
        </div>
    </stripes:layout-component>
    
    
</stripes:layout-render>