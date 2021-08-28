/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.util;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class XSSHelper {
    private static final Logger log = LoggerFactory.getLogger(XSSHelper.class);
    private static final Set<Pattern> testPatterns = new HashSet<>();

    static {
        testPatterns.add(Pattern.compile("<script>", Pattern.CASE_INSENSITIVE));
        testPatterns.add(Pattern.compile("</script>", Pattern.CASE_INSENSITIVE));
        testPatterns.add(Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL));
        testPatterns.add(Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL));
        testPatterns.add(Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL));
        testPatterns.add(Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL));
        testPatterns.add(Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL));
        testPatterns.add(Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE));
        testPatterns.add(Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE));
        testPatterns.add(Pattern.compile("onload(\\s*)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL));
    }

    public static String stripXSS(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        value = Normalizer.normalize(value, Normalizer.Form.NFD);
        // Avoid null characters
        value = value.replaceAll("", "");
        for (Pattern pat : testPatterns) {
            value = pat.matcher(value).replaceAll("");
        }
        log.debug("XSS Cleaned as [{}]", value);
        return value;
    }
    
    public static boolean containsXSS(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        value = Normalizer.normalize(value, Normalizer.Form.NFD);
        // Avoid null characters
        value = value.replaceAll("", "");
        for (Pattern pat : testPatterns) {
            if (pat.matcher(value).find()) {
                log.warn("Matches [{}]", pat.pattern());
                return true;
            }
        }
        return false;
    }

}
