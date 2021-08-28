package com.smilecoms.commons.stripes;

import com.smilecoms.commons.localisation.*;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.slf4j.*;
import net.sourceforge.stripes.config.Configuration;

/**
 * Overides stripes for getting a resource bundle for a given locale. Our version (as configured in web.xml) returns a SmileResourceBundle for the locale.
 * The SmileResourceBundle uses PM platform to get resources out of the DB instead of from .property files like Stripes does. Uses LocalisationHelper for all 
 * its logic
 * @author PCB
 */
public class SmileLocalizationBundleFactory implements net.sourceforge.stripes.localization.LocalizationBundleFactory {
    
    private static final Logger log = LoggerFactory.getLogger(SmileLocalizationBundleFactory.class.getName());
    
    @Override
    public ResourceBundle getErrorMessageBundle(Locale arg0) throws MissingResourceException {
       return LocalisationHelper.getBundle(arg0);
    }
    
    @Override
    public ResourceBundle getFormFieldBundle(Locale arg0) throws MissingResourceException {
        return LocalisationHelper.getBundle(arg0);
    }
    
    @Override
    public void init(Configuration arg0) throws Exception {
         log.debug("SmileLocalizationBundleFactory: in init");
    }

    
}
