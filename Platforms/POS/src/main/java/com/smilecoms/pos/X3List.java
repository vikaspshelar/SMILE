/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author paul
 */
public class X3List {
    
    private String name;
    private List<String> items = new ArrayList<String>();
    public X3List(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void addItem(String item) {
        items.add(item);
    }
    
    public StringBuilder getXML() {
        StringBuilder sb = new StringBuilder("<LST NAME=\"");
        sb.append(name);
        sb.append("\">");
        for (String item : items) {
            sb.append("<ITM>");
            sb.append(item);
            sb.append("</ITM>");
        }
        sb.append("</LST>");
        return sb;
    }
    
}
