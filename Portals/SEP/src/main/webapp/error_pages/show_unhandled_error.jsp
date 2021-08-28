<%@page import="
        com.smilecoms.commons.sca.SCAErr,
        com.smilecoms.commons.stripes.*,
        com.smilecoms.commons.localisation.*,
        java.util.*,javax.servlet.jsp.jstl.core.*,
        javax.servlet.jsp.jstl.fmt.LocalizationContext"%>
<%@ include file="/include/sep_include.jsp" %>

<%
        Locale locale = LocalisationHelper.getDefaultLocale();
        SmileLocalizationBundleFactory fac = new SmileLocalizationBundleFactory();
        ResourceBundle bundle = fac.getFormFieldBundle(locale);
        javax.servlet.jsp.jstl.core.Config.set(request, Config.FMT_LOCALIZATION_CONTEXT, new LocalizationContext(bundle, locale));
%>


<c:set var="title">
    <fmt:message key="unhandled.error"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">

       <br/><img src="images/dilbert.gif" border="0" width="90%"><br/>

        <%

        Throwable toplevel = pageContext.getException();
        String sTopName = "";
        String sTopDesc = "";
        String sTopTrace = "";
        if (toplevel != null) {
            //Top Level Exception info
            sTopName = toplevel.getClass().getName();
            sTopDesc = toplevel.getMessage();
            StackTraceElement[] stTop = toplevel.getStackTrace();
            if (stTop != null) {
                int l = stTop.length;
                for (int i = 1; i < l; i++) {
                    sTopTrace += stTop[i] + "\n";
                }
            }
        }
        %>
        <br/>
        <div style="background-color: red; font-weight: bold; text-align: center">
            The information below may seem overwhelming but 99% of the time, 
            hidden somewhere in the error message will be a clue as to why you got the error. Please take the time to read the message and check if any of it makes sense to you.<br/>
            Only once you have read it and are sure none of it makes any sense, then escalate the issue to the IT team. Thanks<br/>            
        </div>
        
        
        <h2><fmt:message key="top.level.error"/></h2>
        <b><fmt:message key="error.type"/>:</b> <%=sTopName%><br/>
        <b><fmt:message key="error.description"/>:</b> <%=sTopDesc%><br/>
        <br/>

        <span class="button">
            <input type="button" onclick="return toggleMe('topstacktrace')" value="<fmt:message key="stack.trace"/>"/>
        </span>
        <div id="topstacktrace" style="display:none"><%=sTopTrace%></div>


    </stripes:layout-component>

</stripes:layout-render>


