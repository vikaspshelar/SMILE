<%-- 
    Document   : create_new_password
    Created on : 15 Aug 2013, 5:25:12 PM
    Author     : lesiba
--%>
<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    Customer Care Tracking
</c:set>

<stripes:layout-render name="/layout/loginLayout.jsp">
    <stripes:layout-component name="contents">

        <div class="nine columns">
            <div class="login_form columns" style="margin-top: 150px; border-bottom-color: yellowgreen;border-bottom-width: 2px;">
                <font class="light" style="font-size:25px; line-height:100%;">${actionBean.trackerUserName} would like to be able to see what you see on Smiles Web Site so they can help you &#63;</font><br><br>
                <br><br>
                <stripes:form  action="/Login.action"> 
                           <input type="submit" class="general_btn" name="allowTracking" value="Allow" style="margin-left: -5px;"/>
                           <input type="submit" class="general_btn" name="disallowTracking" value="Deny" style="margin-left: -5px;"/>
                </stripes:form>
            </div>
        </div>            
    </stripes:layout-component>
</stripes:layout-render>