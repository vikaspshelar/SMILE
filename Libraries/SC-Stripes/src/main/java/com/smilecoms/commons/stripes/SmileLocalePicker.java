/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.stripes;

import com.smilecoms.commons.localisation.LocalisationHelper;
import java.util.Locale;

import org.slf4j.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import net.sourceforge.stripes.config.Configuration;
import net.sourceforge.stripes.localization.DefaultLocalePicker;

/**
 * Our custom LocalePicker that extends Stripes DefaultLocalePicker. Registered with stripes (via web.xml) so that Stripes offloads the logic to
 * pick a locale onto our code. This class uses the smile commons LocalisationHelper for doing what it needs to do.
 * @author PCB
 */
public class SmileLocalePicker extends DefaultLocalePicker {

    private static final Logger log = LoggerFactory.getLogger(SmileLocalePicker.class.getName());

    @Override
    public void init(Configuration arg0) throws Exception {
        super.init(arg0);
    }

    @Override
    public String pickCharacterEncoding(HttpServletRequest arg0, Locale arg1) {
        if (log.isDebugEnabled()) {
            log.debug("SmileLocalePicker: pickCharacterEncoding Called");
        }
        return super.pickCharacterEncoding(arg0, arg1);
    }

    /**
     * Chooses the locale to use for the users request
     * @param request
     * @return The selected locale
     */
    @Override
    public Locale pickLocale(HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("ENTERING com.smilecoms.commons.stripes.SmileLocalePicker : pickLocale");
        }

        String language = request.getLocale().getLanguage();
        if (log.isDebugEnabled()) {
            log.debug("No locale in session so using language from locale provided in HTTP request: " + language);
        }
        // getLocaleForLanguageAndUserAgent returns default locale from properties if the language passed in is not supported
        Locale locale = LocalisationHelper.getLocaleForLanguage(language);

        // Set the resource bundle that jstl must use in the request.
        // Note that stripes will also use SmileLocalizationBundleFactory to get the bundle due to our config in web.xml
        // This way, both stipes and jstl will use the same bundle on the request
        javax.servlet.jsp.jstl.core.Config.set(request, Config.FMT_LOCALIZATION_CONTEXT,
                new LocalizationContext(LocalisationHelper.getBundle(locale), locale));

        if (log.isDebugEnabled()) {
            log.debug("EXITING com.smilecoms.commons.stripes.SmileLocalePicker : pickLocale Returning Locale " + locale.toString());
        }

        return locale;
    }
}
