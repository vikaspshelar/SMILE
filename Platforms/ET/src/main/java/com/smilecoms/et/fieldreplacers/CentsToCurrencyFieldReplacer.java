/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.et.fieldreplacers;

import com.smilecoms.commons.util.Utils;

/**
 *
 * @author paul
 */
public class CentsToCurrencyFieldReplacer implements IEventFieldReplacer {

    @Override
    public String getValue(String eventData) {
        return Utils.convertCentsToCurrencyLongRoundHalfEven(Double.parseDouble(eventData));
    }
    
}
