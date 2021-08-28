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
public class X3DataStream {
    
    private static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private List<X3Field> fields = new ArrayList<X3Field>();
    private List<X3Table> tables = new ArrayList<X3Table>();
    private List<X3List> lists = new ArrayList<X3List>();
    private List<X3Group> groups = new ArrayList<X3Group>();
    
    private X3Group lastGroup;
    private X3Table lastTable;
    private X3List lastList;
    
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
    
    public void addTable(X3Table table) {
        tables.add(table);
        lastTable = table;
    }
    
    public void addTable(String table) {
        lastTable = new X3Table(table);
        tables.add(lastTable);
    }
    
    public void addList(X3List list) {
        lists.add(list);
        lastList = list;
    }
    
    public void addGroup(X3Group group) {
        groups.add(group);
        lastGroup = group;
    }
    
    public void addGroup(String group) {
        lastGroup = new X3Group(group);
        groups.add(lastGroup);
        
    }
    
    public X3Group getLastGroup() {
        return lastGroup;
    }
    
    public X3Table getLastTable() {
        return lastTable;
    }
    
    public X3List getLastList() {
        return lastList;
    }
    
    public String getXML() {
        StringBuilder sb = new StringBuilder(HEADER);
        sb.append("<PARAM>");
        for (X3Field field : fields) {
            sb.append(field.getXML());
        }
        for (X3Group group : groups) {
            sb.append(group.getXML());
        }
        for (X3List list : lists) {
            sb.append(list.getXML());
        }
        for (X3Table table : tables) {
            sb.append(table.getXML());
        }
        sb.append("</PARAM>");
        return sb.toString();
    }
    
}
