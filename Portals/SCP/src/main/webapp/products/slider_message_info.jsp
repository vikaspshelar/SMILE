<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="scp.about.slider.info"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">
        <script type="text/javascript">
            window.onload = function() {
                makeMenuActive('Profile_AccountsPage');
            }
        </script>
    </stripes:layout-component>   

    <stripes:layout-component name="contents">
        <div style="margin-top: 10px;">

        <div style="margin-top: 2px; margin-left: 140px;" class="about_manage_manage_speed twelve columns">
            <table>
                <tr>
                    <td>
                        <fmt:message key="scp.slider.msg.info"/>
                        <br/>
                        <div>
                            <input class="general_btn_inverted" type="button" value="<fmt:message key="back"/>" onclick="previousPage();" style="margin-left:9px"/>
                        </div>
                    </td>
                </tr>

            </table>
            <br/><br/>
        </div>
</div>
    </stripes:layout-component>    
</stripes:layout-render>