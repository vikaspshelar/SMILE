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
public class X3Table {
    
    private final List<X3Line> lines = new ArrayList<>();
    private final String id;
    private X3Line lastLine;
    public X3Table(String id) {
        this.id = id;
    }

    public void addLine(X3Line line) {
        lines.add(line);
        lastLine = line;
    }
    
    public void addLine(int lineNumber) {
        lastLine = new X3Line(lineNumber);
        lines.add(lastLine);
    }
    
    public void addLine() {
        lastLine = new X3Line(lines.size() + 1);
        lines.add(lastLine);
    }
    
    public X3Line getLastLine() {
        return lastLine;
    }
    
    public StringBuilder getXML() {
        StringBuilder sb = new StringBuilder("<TAB ID=\"");
        sb.append(id);
        sb.append("\">");
        for (X3Line line : lines) {
            sb.append(line.getXML());
        }
        sb.append("</TAB>");
        return sb;
    }
    
}
