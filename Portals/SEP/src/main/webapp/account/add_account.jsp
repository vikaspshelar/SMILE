<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="add.new.account"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        
        <stripes:form action="/Account.action" focus="">    
            <table class="clear">                              
                <tr>
                    <td colspan="2">
                        <span class="button">
                            <stripes:submit name="createAccount"  />
                        </span>                        
                    </td>
                </tr>  
            </table>            
        </stripes:form>
        
        
    </stripes:layout-component>
    
    
</stripes:layout-render>

