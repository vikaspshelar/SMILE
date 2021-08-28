<%-- 
    Document   : create_new_password
    Created on : 15 Aug 2013, 5:25:12 PM
    Author     : lesiba
--%>
<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="create.new.password"/>
</c:set>

<stripes:layout-render name="/layout/loginLayout.jsp">
    <stripes:layout-component name="contents">
        <div class="sixteen columns">
            <div class="five login_form columns">
                <font class="light" style="font-size:25px;">Login In For the First Time</font><br><br>
                <fmt:message key="login.for.first.time"/>
                <br><br>
                <stripes:form  action="/Login.action" id="loginform"> 
                    <label><fmt:message key="user.name.or.email.address"/>:</label>
                    <input type="text" name="username" placeholder="<fmt:message key="user.name.or.email.address"/>" length="30" maxlength="50" class="required"/>
                    <input type="hidden" name="password" value=""/>
                    <table>
                        <tr>
                            <td><input type="button" class="general_btn_inverted" name="back" value="Back" onclick="previousPage()" style="margin-left: -5px;"></td>
                            <td><input type="submit" class="general_btn" name="login" value="Proceed" style="margin-left: -5px;"/></td>
                        </tr>


                    </table>

                </stripes:form>
            </div>
        </div>            
    </stripes:layout-component>
</stripes:layout-render>