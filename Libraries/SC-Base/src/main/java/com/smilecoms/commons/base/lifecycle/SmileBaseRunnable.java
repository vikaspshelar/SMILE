/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base.lifecycle;

/**
 *
 * @author paul
 * Just like a runnable but with a name that can be used for populating stats
 */
public abstract class SmileBaseRunnable implements Runnable{

    private final String name;
    
    public SmileBaseRunnable(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}
