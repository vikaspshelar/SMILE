<%@ include file="/include/sep_include.jsp" %>


<c:set var="title">
    Trouble Ticket Wizard
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

        <c:if test="${actionBean.currentStep != null}">
            <table class="green" width="99%"> 
                <c:if test="${actionBean.customer.customerId eq 0}">
                    <tr>
                        <td colspan="2">
                            <b style="color: red; font-style: italic;"><fmt:message key="customer.care.support.wizard.no.customerId.message"/></b>
                        </td>
                    </tr>
                </c:if>

                <tr>
                    <td colspan="2">
                        <br/>
                        <b>${actionBean.currentStep.question.questionText}</b>
                        <br/>
                    </td>
                </tr>

                <c:forEach items="${actionBean.currentStep.options}" var="option" varStatus="loop">
                    <tr class="${loop.count mod 2 == 0 ? "even" : "odd"}">
                        <td>${option.optionText}</td>
                        <td>
                            <stripes:form action="/TroubleTicket.action" class="buttonOnly">
                                <input type="hidden" name="selectedOptionId" value="${option.optionId}"/>
                                <input type="hidden" name="customer.customerId" value="${actionBean.customer.customerId}"/>
                                <c:forEach items="${actionBean.fastForwardTTFixedFieldList}" var="item" varStatus="loop">
                                    <input type="hidden" name="fastForwardTTFixedFieldList['${item.key}']" value="${item.value}"/>
                                </c:forEach>
                                <stripes:submit name="wizardNext"/>
                            </stripes:form>                                
                        </td>
                    </tr>
                </c:forEach>

                <c:if test="${actionBean.currentStep.parentStep != null}">
                    <tr style="text-align: center">
                        <td colspan="3">
                            <stripes:form action="/TroubleTicket.action" class="buttonOnly">
                                <input type="button" value="<fmt:message key="back"/>" onclick="previousPage();" />
                            </stripes:form>  
                        </td>
                    </tr>
                </c:if>
            </table>
        </c:if>



        <c:if test="${actionBean.currentLeaf != null && actionBean.currentLeaf.type == 'TT'}">
            <stripes:form action="/TroubleTicket.action">
                <input type="hidden" name="customer.customerId" value="${actionBean.customer.customerId}"/>
                <table class="green" width="99%">  
                    <c:if test="${actionBean.customer.customerId eq 0}">
                        <tr>
                            <td colspan="2">
                                <b style="color: red; font-style: italic;"><fmt:message key="customer.care.support.wizard.no.customerId.message"/></b>
                            </td>
                        </tr>
                    </c:if>
                    <tr>
                        <td colspan="2">
                            <b>${actionBean.currentLeaf.leafName}</b>
                            <input type="hidden" name="TT_WIZARD_SUMMARY_FIELD" value="${actionBean.currentLeaf.leafName}"/>
                        </td>
                    </tr>
                    <c:forEach items="${actionBean.currentLeaf.leafDataItems}" var="ldi" varStatus="loop">
                        <c:if test="${ldi.type == 'TT_EXPLANATION'}">
                            <tr>
                                <td colspan="2">${ldi.value}:</td>
                            </tr>
                        </c:if>
                    </c:forEach>
                    <c:forEach items="${actionBean.currentLeaf.leafDataItems}" var="ldi" varStatus="loop">
                        <c:if test="${ldi.type == 'TT_USER_FIELD'}">
                            <tr>
                                <td>${ldi.value}:<input type="hidden" name="${ldi.type}_${ldi.attribute}${loop.index}_q" value="${ldi.value}"/></td>
                                <td><stripes:textarea name="${ldi.type}_${ldi.attribute}${loop.index}" cols="65" rows="2" style="font-family:Arial, sans-serif; font-size:12px;"/></td>
                            </tr>
                        </c:if>
                        <c:if test="${ldi.type == 'TT_FIXED_FIELD'}">
                            <stripes:hidden name="${ldi.type}_${ldi.attribute}" value="${ldi.value}"/>
                            <%--<tr>
                                <td>${ldi.attribute}:</td>
                                <td><stripes:text name="${ldi.type}_${ldi.attribute}" size="50" value="${ldi.value}" readonly="true"/></td>
                                </tr>--%>
                        </c:if>
                        <c:if test="${ldi.type == 'TT_DROPDOWN_FIELD'}">
                            <tr>
                                <td>${ldi.attribute}:</td>
                                <td>
                                    <stripes:select name="${ldi.type}_${ldi.attribute}" >
                                        <c:forEach items="${ldi.valueAsList}" var="option" varStatus="loop">
                                            <stripes:option value="${option}">
                                                ${option}
                                            </stripes:option>
                                        </c:forEach>
                                    </stripes:select>
                                </td>
                            </tr>
                        </c:if>
                    </c:forEach>
                    
                    <c:forEach items="${actionBean.fastForwardTTFixedFieldList}" var="item" varStatus="loop">
                        <input type="hidden" name="fastForwardTTFixedFieldList['${item.key}']" value="${item.value}"/>
                    </c:forEach>        
                        
                    <tr style="text-align: center">
                        <td>
                            <input type="button" value="<fmt:message key="back"/>" onclick="previousPage();" />
                        </td>  
                        <td>
                            <stripes:submit name="createTTForWizard"/>
                        </td> 
                    </tr>
                </table>
            </stripes:form>
        </c:if>

        <c:if test="${actionBean.currentLeaf != null && actionBean.currentLeaf.type == 'EXPLANATION'}">
            <stripes:form action="/TroubleTicket.action">
                <table class="green" width="99%">  
                    <tr>
                        <td colspan="3">
                            ${actionBean.currentLeaf.leafName}
                            <input type="hidden" name="TT_WIZARD_SUMMARY_FIELD" value="${actionBean.currentLeaf.leafName}"/>
                        </td>
                    </tr>
                    <tr style="text-align: center">
                        <td>
                            <input type="button" value="<fmt:message key="back"/>" onclick="previousPage();" />
                        </td>  
                        <td>
                            <input type="hidden" name="customer.customerId" value="${actionBean.customer.customerId}"/>
                            <stripes:submit name="startWizardAgain"/>
                        </td> 
                        <c:if test="${actionBean.currentLeaf.TTLeaf != null}">
                        <input type="hidden" name="currentLeafId" value="${actionBean.currentLeaf.TTLeaf.leafId}"/>
                        <input type="hidden" name="customer.customerId" value="${actionBean.customer.customerId}"/>
                        <td>
                            <stripes:submit name="wizardNext"/>
                        </td> 
                    </c:if>
                </tr>
            </table>
        </stripes:form>
    </c:if>

    <c:if test="${actionBean.currentLeaf != null && actionBean.currentLeaf.type == 'EXTERNAL_RESOURCE'}">
        <stripes:form action="/TroubleTicket.action">
            <table class="green" width="99%">  
                <tr>
                    <td colspan="3">
                        <b>${actionBean.currentLeaf.leafName}</b>
                        <input type="hidden" name="TT_WIZARD_SUMMARY_FIELD" value="${actionBean.currentLeaf.leafName}"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="3">
                        <iframe name="externalResource" src="${actionBean.currentLeaf.leafDataItems[0].value}" width="100%" height="600px"></iframe>
                    </td>
                </tr>
                <tr style="text-align: center">
                    <td>
                        <input type="button" value="<fmt:message key="back"/>" onclick="previousPage();" />
                    </td>  
                    <td>
                        <stripes:submit name="startWizardAgain"/>
                    </td> 
                    <c:if test="${actionBean.currentLeaf.TTLeaf != null}">
                    <input type="hidden" name="currentLeafId" value="${actionBean.currentLeaf.TTLeaf.leafId}"/>
                    <input type="hidden" name="customer.customerId" value="${actionBean.customer.customerId}"/>
                    <td>
                        <stripes:submit name="wizardNext"/>
                    </td> 
                </c:if>
            </tr>
        </table>
    </stripes:form>
</c:if>


</stripes:layout-component>
</stripes:layout-render>