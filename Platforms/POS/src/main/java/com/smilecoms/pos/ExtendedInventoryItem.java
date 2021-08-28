/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import com.smilecoms.xml.schema.pos.InventoryItem;

/**
 *
 * @author paul
 */
public class ExtendedInventoryItem extends InventoryItem {
    private boolean isKittedItem = false;

    public boolean isIsKittedItem() {
        return isKittedItem;
    }

    public void setIsKittedItem(boolean isKittedItem) {
        this.isKittedItem = isKittedItem;
    }
    
}
