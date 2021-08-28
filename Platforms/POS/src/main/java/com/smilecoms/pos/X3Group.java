/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author paul
 */
public class X3Group {

    private List<X3Field> fields = new ArrayList<X3Field>();
    private String id;

    public X3Group(String id) {
        this.id = id;
    }

    public void addField(X3Field field) {
        fields.add(field);
    }

    public void addField(String name, String value) {
        fields.add(new X3Field(name, value));
    }
    
    public void addField(String name, int value) {
        fields.add(new X3Field(name, value));
    }
    
    public void addField(String name, double value) {
        fields.add(new X3Field(name, value));
    }
    
    public void addField(String name, Date value) {
        fields.add(new X3Field(name, value));
    }

    public StringBuilder getXML() {
        StringBuilder sb = new StringBuilder("<GRP ID=\"");
        sb.append(id);
        sb.append("\">");
        for (X3Field field : fields) {
            sb.append(field.getXML());
        }
        sb.append("</GRP>");
        return sb;
    }
}
