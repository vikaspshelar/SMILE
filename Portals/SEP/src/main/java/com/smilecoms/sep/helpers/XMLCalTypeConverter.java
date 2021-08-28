/*
 * XMLCalTypeConverter.java
 * 
 * Created on 2008/01/13, 11:58:14
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.helpers;

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.slf4j.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import net.sourceforge.stripes.validation.TypeConverter;
import java.text.SimpleDateFormat;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Enter description of class
 * 
 */
public class XMLCalTypeConverter implements TypeConverter<XMLGregorianCalendar> {

    private static DatatypeFactory df = null;
    private static final String T = "T";
    private static final String SPACE = " ";
    private static final String FS = "/";
    private static final Logger log = LoggerFactory.getLogger(XMLCalTypeConverter.class.getName());
    private static final SimpleDateFormat sdfShort = new SimpleDateFormat("yyyy/MM/dd");
    private static final SimpleDateFormat sdfLong = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final SimpleDateFormat sdfXMLGregFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public XMLCalTypeConverter() {
        if (df == null) {
            try {
                df = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException ex) {
                log.warn("Error creating DatatypeFactory : " + ex.toString());
            }
        }
    }

    @Override
    public void setLocale(Locale arg0) {

    }

    @Override
    public XMLGregorianCalendar convert(String s, Class arg1, Collection arg2) {
        try {
            //Check for null/blank/invalid date and return empty string
            if (s == null || s.length() < 10) {
                return null;
            }
            // We need to get the format of the date and then we will know how to parse and convert it
            if (s.indexOf(T) > 0) {
                // Is in format like "2008-01-13T00:00:00.000+02:00"
                s = s.replace(T, SPACE);
                return getXMLDate(sdfXMLGregFormat.parse(s));
            }
            if (s.indexOf(FS) == 4 && s.lastIndexOf(FS) == 7 && s.length() == 10 && Integer.parseInt(s.substring(5, 6)) <= 12) {
                // Is in format 2008/02/22
                return getXMLDate(sdfShort.parse(s));
            }
            // Default catch all
            return getXMLDate(sdfLong.parse(s));
        } catch (ParseException ex) {
            log.error("Error", ex);
        }
        return null;
    }

    private XMLGregorianCalendar getXMLDate(Date d) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(d);
        return df.newXMLGregorianCalendar(calendar);
    }
}
