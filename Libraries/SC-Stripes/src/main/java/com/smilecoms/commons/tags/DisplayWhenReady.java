/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.tags;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTag;
import net.sourceforge.stripes.util.Log;
import net.sourceforge.stripes.util.UrlBuilder;

/**
 *
 * @author lesiba
 */
public final class DisplayWhenReady extends net.sourceforge.stripes.tag.HtmlTagSupport implements BodyTag, net.sourceforge.stripes.tag.ParameterizableTag {

    private static final Log log = Log.getInstance(DisplayWhenReady.class);
    private Map<String, Object> parameters = new HashMap<String, Object>();
    private String event;
    private String href;
    private String containerId;
    private String autoLoad;
    private String displayMessage;

    /*
     * Getters and Setters
     */
    public void setEvent(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public void setContainerId(String id) {
        this.containerId = id;
    }

    public String getContainerId() {
        return containerId;
    }

    public String getAutoLoad() {
        return autoLoad;
    }

    public void setAutoLoad(String autoLoad) {
        this.autoLoad = autoLoad;
    }
    public String getDisplayMessage(){
        return displayMessage;
    }
    public void setDisplayMessage(String displayMessage){
        this.displayMessage = displayMessage;
    }
    @Override
    public void addParameter(String name, Object valueOrValues) {
        this.parameters.put(name, valueOrValues);
    }

    @Override
    public int doStartTag() throws JspException {
        return EVAL_BODY_BUFFERED;
    }

    @Override
    public int doEndTag() throws JspException {
        JspWriter out;
        try {
            out = getPageContext().getOut();
            out.print(getScript());
        } catch (Exception e) {
            log.warn("error : " + e.getMessage() + "\n");
            log.warn("Error: ", e);
        }
        return EVAL_PAGE; //TODD : To be implemented
    }

    @Override
    public void doInitBody() throws JspException {
        /*Do nothing*/
    }

    @Override
    public int doAfterBody() throws JspException {
        return SKIP_BODY;
    }

    public String getScript() {
        StringBuilder sb = new StringBuilder();
        log.debug("URL to be called [{}]", getUrl());
        sb.append("<div id=\"");
        sb.append(containerId);
        sb.append("\" style=\"display: inline;\">");
        if (getAutoLoad().contains("false")) {
            sb.append("<a href=\"javascript:loadDivAsynchronously('");
            sb.append(getUrl());
            sb.append("', '").append(containerId).append("')\">");
            sb.append(displayMessage);
            sb.append("</a>");
        } else if (getAutoLoad().contains("true")) {
            sb.append("<script type=\"text/javascript\"> loadDivAsynchronously('");
            sb.append(getUrl());
            sb.append("', '").append(containerId).append("');"); //TODO : Uhm yeah....        
            sb.append("</script>");
        }
        sb.append("</div>");

        return sb.toString();
    }

    /** 
     *  getUrl
     * 
     * @return String
     * 
     */
    public String getUrl() {
        HttpServletRequest request = (HttpServletRequest) getPageContext().getRequest();
        HttpServletResponse response = (HttpServletResponse) getPageContext().getResponse();

        UrlBuilder builder = new UrlBuilder(pageContext.getRequest().getLocale(), href, false);
        builder.setEvent(event);
        builder.addParameters(parameters);

        String url = builder.toString();

        String context = request.getContextPath();
        if (context.length() > 1) {
            if (url.startsWith("/")) {
                url = context + url;
            }
        }
        return response.encodeURL(url);
    }
}
