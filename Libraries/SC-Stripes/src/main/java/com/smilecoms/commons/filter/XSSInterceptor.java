package com.smilecoms.commons.filter;

import javax.servlet.http.HttpServletRequest;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.ExecutionContext;
import net.sourceforge.stripes.controller.Interceptor;
import net.sourceforge.stripes.controller.Intercepts;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.controller.StripesRequestWrapper;

/**
 * <p>
 * An {@code Interceptor} that sanitizes all the request parameters before allowing the ActionBean resolution to
 * proceed.
 * </p>
 * 
 * <p>
 * To configure {@code XSSInterceptor}, add the following initialization parameters to your Stripes filter configuration
 * in <code>web.xml</code>:
 * </p>
 * 
 * <p>
 * 
 * <pre>
 * &lt;init-param&gt;
 *     &lt;param-name&gt;Interceptor.Classes&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *         com.samaxes.stripes.xss.XSSInterceptor
 *     &lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * If one or more interceptors are already configured in your <code>web.xml</code> simply separate the fully qualified
 * names of the interceptors with commas and place this interceptor as the last param value in the existing list.
 * </p>
 * 
 * @author Samuel Santos
 * @version $Revision: 26 $
 */
@Intercepts(LifecycleStage.BindingAndValidation)
public class XSSInterceptor implements Interceptor {


    /**
     * Sanitize all the request parameters before allowing the ActionBean resolution to proceed.
     * 
     * @param context the current execution context
     * @return the Resolution produced by calling context.proceed()
     * @throws Exception if the lifecycle code or one of the interceptors throws one
     */
    @Override
    public Resolution intercept(ExecutionContext context) throws Exception {
        StripesRequestWrapper stripesWrapper = null;
        HttpServletRequest originalRequest = null;

        try {
            stripesWrapper = StripesRequestWrapper.findStripesWrapper(context.getActionBeanContext().getRequest());
            originalRequest = (HttpServletRequest) stripesWrapper.getRequest();
            stripesWrapper.setRequest(new XSSRequestWrapper(originalRequest));
            return context.proceed();
        } finally {
            if (stripesWrapper != null && originalRequest != null) {
                stripesWrapper.setRequest(originalRequest);
            }
        }
    }
}
