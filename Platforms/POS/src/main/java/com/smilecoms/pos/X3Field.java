/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Utils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public final class X3Field {
    private static final Logger log = LoggerFactory.getLogger(X3Field.class);
    private String name;
    private String value;

    public X3Field(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public X3Field(String name, int value) {
        this.name = name;
        setValue(value);
    }

    public X3Field(String name, double value) {
        this.name = name;
        setValue(value);
    }
    
    public X3Field(String name, BigDecimal value) {
        this.name = name;
        setValue(value);
    }

    public X3Field(String name, Date value) {
        this.name = name;
        setValue(value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValue(double value) {
        this.value = getStringRoundedForX3(value);
    }
    
    public void setValue(BigDecimal value) {
        value = value.setScale(getX3DecimalPlaces(), RoundingMode.HALF_EVEN);
        this.value = value.toPlainString();
    }
    
    public static String getStringRoundedForX3(double value) {
        String format = BaseUtils.getProperty("env.pos.x3.decimal.format", "#.00000");
        DecimalFormat dfX3 = new DecimalFormat(format);
        return dfX3.format(getDoubleRoundedForX3(value));
    }
    
    public static double getDoubleRoundedForX3(double value) {
        double res = Utils.round(value, getX3DecimalPlaces());
        return res;
    }
    
    public static int getX3DecimalPlaces() {
        String format = BaseUtils.getProperty("env.pos.x3.decimal.format", "#.00000");
        int ret = format.substring(format.indexOf(".") + 1).length();
        return ret;
    }
    
    public void setValue(int value) {
        this.value = String.valueOf(value);
    }
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

    public void setValue(Date value) {
        this.value = formatter.format(value);
    }

    public StringBuilder getXML() {
        StringBuilder sb = new StringBuilder("<FLD NAME=\"");
        sb.append(name);
        sb.append("\">");
        sb.append(value);
        sb.append("</FLD>");
        return sb;
    }
}
