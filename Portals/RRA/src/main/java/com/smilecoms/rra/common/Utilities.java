/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.rra.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author abhilash
 */
public class Utilities {
    
    public String extractValue(String regex, String requestData) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(requestData);
        if(matcher.find())
        {
            return matcher.group(1);
            
        }
        return "";
    }
    
}
