/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sop.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author PCB
 */
public class SyslogStatistic {

    private double value;
    private String name = "";
    private String type = "";
    private String location = "";
    private String other = "";
    private long added = System.currentTimeMillis();
    private static final Logger log = LoggerFactory.getLogger(SyslogStatistic.class);

    public long getAgeSecs() {
        return (System.currentTimeMillis() - added) / 1000;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location.toLowerCase();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Location: " + location + " Type: " + type  + " Name: " + name  + " Value: " + value;
    }

    public boolean isSameIgnoringLocation(SyslogStatistic other) {
        return other.getName().equals(name) && other.getType().equals(type);
    }
    
}
