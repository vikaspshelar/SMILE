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
public class BytesToDataVolumeFieldReplacer implements IEventFieldReplacer {

    @Override
    public String getValue(String eventData) {
        return Utils.displayVolumeAsStringWithCommaGroupingSeparator(Double.parseDouble(eventData), "byte");
    }
    
}
