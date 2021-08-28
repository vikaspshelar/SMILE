<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="search.organisation"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents"> 
        <stripes:form action="/Customer.action" focus="" autocomplete="off" onsubmit="return alertValidationErrors();">
            <table class="clear">
                <tr>
                    <td><fmt:message key="id"/>:</td>
                    <td><input type="text"  name="organisationQuery.organisationId" size="30" onkeyup="validate(this,'^[0-9]{1,8}$','emptyok')" value="{{org.originalObject.organisationId}}"/></td>
                    <td><fmt:message key="andor"/></td>
                </tr>
                <tr>
                    <td><fmt:message key="organisation.name"/>:</td>
                    <td style="width:300px;">
                        <div angucomplete-alt placeholder="Start typing organisation name..."
                             pause="1000"
                             selected-object="org"
                             maxlength="200"
                             remote-url="AutoComplete.action?getOrganisationsJSON=&organisationQuery.organisationName="
                             remote-url-data-field="organisations"
                             title-field="organisationName"
                             input-class="form-control"
                             text-searching="Searching..."
                             text-no-results="No results found!"
                             minlength="3"
                             match-class="highlight" />
                    </td>
                </tr>
                <tr>
                    <td>
                        <span class="button">
                            <stripes:submit name="retrieveOrganisation"/>
                        </span>
                    </td>
                </tr>
            </table>            
        </stripes:form>

    </stripes:layout-component>    
</stripes:layout-render>

