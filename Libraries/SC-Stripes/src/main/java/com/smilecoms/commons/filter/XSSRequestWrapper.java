package com.smilecoms.commons.filter;

import com.smilecoms.commons.util.XSSHelper;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.slf4j.*;

public class XSSRequestWrapper extends HttpServletRequestWrapper {

    private static final Logger log = LoggerFactory.getLogger(XSSRequestWrapper.class);
    private Map<String, String[]> sanitized;
    private final Map<String, String[]> orig;

    /**
     * Constructor that will parse and sanitize all input parameters.
     *
     * @param request the HttpServletRequest to wrap
     */
    @SuppressWarnings("unchecked")
    public XSSRequestWrapper(HttpServletRequest request) {
        super(request);
        orig = request.getParameterMap();
    }

    /**
     * Return getParameter(String name) on the wrapped request object.
     */
    @Override
    public String getParameter(String name) {
        String[] vals = getParameterMap().get(name);
        return (vals != null && vals.length > 0) ? vals[0] : null;
    }

    /**
     * Sanitize and return getParameterMap() on the wrapped request object.
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        if (sanitized == null) {
            sanitized = sanitizeParamMap(orig);
        }
        return sanitized;
    }

    /**
     * Return getParameterValues(String name) on the wrapped request object.
     */
    @Override
    public String[] getParameterValues(String name) {
        return getParameterMap().get(name);
    }

    private Map<String, String[]> sanitizeParamMap(Map<String, String[]> raw) {
        Map<String, String[]> res = new HashMap<>();

        if (raw != null) {
            for (String key : raw.keySet()) {
                String[] rawVals = raw.get(key);
                String[] snzVals = new String[rawVals.length];
                for (int i = 0; i < rawVals.length; i++) {
                    snzVals[i] = XSSHelper.stripXSS(rawVals[i]);
                }
                res.put(key, snzVals);
            }
        }

        return res;
    }

}
