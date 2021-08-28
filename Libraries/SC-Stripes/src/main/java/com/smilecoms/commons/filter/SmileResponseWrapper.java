/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.filter;

import com.smilecoms.commons.base.BaseUtils;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

final class SmileResponseWrapper extends HttpServletResponseWrapper {

    private String prefix = null;

    public SmileResponseWrapper(HttpServletRequest inRequest, HttpServletResponse response) {
        super(response);
        prefix = getPrefix(inRequest);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        String finalurl;
        if (isUrlAbsolute(location)) {
            finalurl = location;
        } else {
            finalurl = fixForScheme(prefix + location);
        }
        super.sendRedirect(finalurl);
    }

    public boolean isUrlAbsolute(String url) {
        String lowercaseurl = url.toLowerCase();
        return lowercaseurl.startsWith("http");
    }

    public String fixForScheme(String url) {
        //alter the url here if you were to change the scheme
        List<String> matchStrings = BaseUtils.getPropertyAsList("global.portal.redirect.url.ssl.contains");
        boolean mustBeHTTPS = false;
        if (!url.startsWith("https")) {
            for (String match : matchStrings) {
                if (url.contains(match)) {
                    mustBeHTTPS = true;
                }
            }
        }
        if (mustBeHTTPS) {
            url = url.replaceFirst("http", "https");
        }
        return url;
    }

    public String getPrefix(HttpServletRequest request) {
        StringBuffer str = request.getRequestURL();
        String url = str.toString();
        String uri = request.getRequestURI();
        int offset = url.indexOf(uri);
        return url.substring(0, offset);
    }
}
