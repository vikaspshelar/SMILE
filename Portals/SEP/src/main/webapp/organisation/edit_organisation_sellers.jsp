<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="edit.customer.sellers"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <SCRIPT LANGUAGE="JavaScript">    
            
            function addSeller() {
                var x = document.getElementById("list2");
                var option = document.createElement("option");
                option.value = document.getElementById("idtoadd").value;                
                option.text = document.getElementById("sellerName_value").value;        
                
                document.getElementById("orgsellers").value = ""; 
                var sellers = document.getElementById('list2'), seller, i;
                                
                for(i = 0; i < sellers.length; i++) {
                  seller = sellers[i];                  
                  
                  if(document.getElementById("orgsellers").value === "") {
                       document.getElementById("orgsellers").value = document.getElementById("orgsellers").value + seller.value; 
                    } else {
                        document.getElementById("orgsellers").value = document.getElementById("orgsellers").value + "," + seller.value;
                    }
                }
                
                if (document.getElementById("orgsellers").value.indexOf(option.value)===-1)
                    {
                     x.add(option);
                     document.getElementById("orgsellers").value = document.getElementById("orgsellers").value + "," + document.getElementById("idtoadd").value;                                 
                     document.getElementById("idtoadd").value="";
                     document.getElementById("SellerName_value").value="";
                    }
                
            }
            
            function removeSeller() {                
                var x = document.getElementById("list2");                
                
                x.remove(x.selectedIndex);
                var sellers = document.getElementById('list2'), seller, i;
                document.getElementById("orgsellers").value = ""; 
                for(i = 0; i < sellers.length; i++) {
                  seller = sellers[i];                  
                  if(document.getElementById("orgsellers").value === "") {
                       document.getElementById("orgsellers").value = document.getElementById("orgsellers").value + seller.value; 
                    } else {
                        document.getElementById("orgsellers").value = document.getElementById("orgsellers").value + "," + seller.value;
                    }
                }
            }
            
        </SCRIPT>   

        
            <table class="entity_header">    
                <tr>
                    <td>
                        <fmt:message key="organisation"/> ${actionBean.organisation.organisationName}
                    </td>                        
                    <td align="right">                       
                        <stripes:form action="/Customer.action">                                
                            <stripes:hidden name="organisationQuery.organisationId" value="${actionBean.organisation.organisationId}"/>                              
                            <stripes:select name="entityAction">
                                <stripes:option value="retrieveOrganisation"><fmt:message key="manage.organisation"/></stripes:option>
                            </stripes:select>
                            <stripes:submit name="performEntityAction"/>
                        </stripes:form>
                    </td>
                </tr>
            </table> 
                            
            <stripes:form action="/Customer.action" focus="" id="form_edit">
                <stripes:hidden name="organisation.version" value="${actionBean.organisation.version}"/>
                <stripes:hidden name="organisation.organisationId" value="${actionBean.organisation.organisationId}"/>                
                
                <table class="clear">                                
                    <tr>                        
                        <td valign="top" style="width:fit-content;min-width:250px;">
                            <input type="hidden" id="idtoadd"  name="idtoadd" size="30" value="{{org.originalObject.organisationId}}"/>
                            <div style width="500px"  id="sellerName" angucomplete-alt placeholder="Start typing organisation name..."                                
                                pause="1000"
                                selected-object="org"
                                maxlength="400"
                                remote-url="AutoComplete.action?getOrganisationsJSON=&organisationQuery.organisationName="
                                remote-url-data-field="organisations"
                                title-field="organisationName"
                                input-class="form-control"
                                text-searching="Searching..."
                                text-no-results="No results found!"
                                minlength="3"
                                match-class="highlight" />
                        </td>
                                   
                        <td VALIGN=TOP ALIGN=CENTER style="padding-left:20px; padding-right: 20px">
                            <INPUT TYPE="button" NAME="right" VALUE="Add" style="width:60px" ONCLICK="javascript:addSeller();"><br><br>
                            <INPUT TYPE="button" NAME="left" VALUE="Remove" style="width:60px" ONCLICK="javascript:removeSeller();"><br><br>                                       
                        </td>
                                    
                        <td valign="top">
                            <input type="hidden" id="orgsellers" name="orgsellers" value=""/>
                            <stripes:select id="list2" name="list2" size="15" multiple="multiple" style="width: 300px; min-width: fit-content" ondblclick="javascript:removeSeller()">
                                <c:if test="${!empty actionBean.organisationSellers}">
                                    <c:forEach items="${actionBean.organisationSellers}" var="organisationSeller" varStatus="loop"> 
                                        <c:if test="${!empty organisationSeller}">
                                            <stripes:option value="${organisationSeller.organisationId}">${organisationSeller.organisationName}</stripes:option>
                                        </c:if>
                                    </c:forEach>
                                </c:if>
                            </stripes:select>                                 
                        </td>                             
                    </tr>                
                    <tr>
                        <td colspan="2"></td>
                        <td >
                            <span class="button">
                                <stripes:submit name="updateOrganisationSellers"/>
                            </span>                        
                        </td>
                    </tr>                                 
                </table>
                <stripes:hidden name="removedLeft" value=""/>
                <stripes:hidden name="removedRight" value=""/>
                <stripes:hidden name="addedLeft" value=""/>
                <stripes:hidden name="addedRight" value=""/>
                <stripes:hidden name="newLeft" value=""/>
                <stripes:hidden name="optionTransfer.newRight" value=""/>
            </stripes:form>
            <script>
                var sellers = document.getElementById('list2'), seller, i;
                document.getElementById("orgsellers").value = ""; 
                for(i = 0; i < sellers.length; i++) {
                  seller = sellers[i];                  
                  if(document.getElementById("orgsellers").value === "") {
                       document.getElementById("orgsellers").value = document.getElementById("orgsellers").value + seller.value; 
                    } else {
                        document.getElementById("orgsellers").value = document.getElementById("orgsellers").value + "," + seller.value;
                    }
                }
                                            
            </script>
    </stripes:layout-component>
</stripes:layout-render>

