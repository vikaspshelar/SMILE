<%@include file="../include/scp_include.jsp" %>
<div class="sixteen columns alpha footer_nav">
    <ul>
        <li>
            <a class="green" href='<fmt:message key="scp.contact.us.link"/>' target="_blank"><fmt:message key="scp.contact.us"/></a>
        </li>
        <li>
            <stripes:link class="green" href="/Login.action" event="logOut"><fmt:message key="logout"/></stripes:link>
        </li>

        </ul>
    </div>
    <div class="sixteen columns alpha footer_terms">
        <div class="copyright columns">
        <fmt:message key="scp.footer.copyright.msg"/>
    </div>
    <div class="terms">
        <a href="<fmt:message key="scp.footer.termsofuse.link"/>" title="<fmt:message key="scp.footer.termsofuse"/>" target="_blank"><fmt:message key="scp.footer.termsofuse"/></a> | <a href="<fmt:message key="scp.footer.termsandconditions.link"/>" title="<fmt:message key="scp.footer.termsandconditions"/>" target="_blank"><fmt:message key="scp.footer.termsandconditions"/></a> | <a href="<fmt:message key="scp.footer.privacypolicy.link"/>" title="<fmt:message key="scp.footer.privacypolicy"/>" target="_blank"><fmt:message key="scp.footer.privacypolicy"/></a>
    </div>
</div>