package com.smilecoms.scp.helpers;

import javax.xml.datatype.XMLGregorianCalendar;
import net.sourceforge.stripes.config.Configuration;
import net.sourceforge.stripes.validation.DefaultTypeConverterFactory;


/**
 * This class is Configured in web.xml as the Stripes ConverterFactory
 * It adds our custom converter for binding a String to a XMLGregorianCalendar
 * so that form post data can be bound directly into the xml beans setter methods 
 * for Date data types
 */
public class CustomTypeConverterFactory extends DefaultTypeConverterFactory {
    @Override
    public void init(Configuration configuration) {
        super.init(configuration);
        add(XMLGregorianCalendar.class, XMLCalTypeConverter.class);
    }
}