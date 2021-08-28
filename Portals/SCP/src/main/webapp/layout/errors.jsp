<%@ include file="/include/scp_include.jsp" %>
<stripes:errors>
    <div style="margin-top: 20px;" class="sixteen columns alpha">
        <div class="error">        
            <table>
                <tr>
                    <td>
                        <img src="images/error.gif" alt="error"/>
                    </td>
                    <td>
                        <p style="color: red;">
                            <stripes:individual-error/>
                        </p>
                    </td>
                </tr>
            </table>
        </div>
        <br/>
    </div>
</stripes:errors>
