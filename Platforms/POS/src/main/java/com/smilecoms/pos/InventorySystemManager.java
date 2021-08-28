/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import com.smilecoms.commons.base.BaseUtils;

/**
 *
 * @author paul
 */
public class InventorySystemManager {
    public static InventorySystem getInventorySystem() throws Exception {
        Class isClass = InventorySystemManager.class.getClassLoader().loadClass(BaseUtils.getProperty("env.pos.inventorysystem.class"));
        InventorySystem system = (InventorySystem) isClass.newInstance();
        system.initialise();
        return system;
    }
}
