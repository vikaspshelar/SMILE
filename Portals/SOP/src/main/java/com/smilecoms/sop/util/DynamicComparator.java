/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sop.util;

import java.lang.reflect.Method;
import java.util.Comparator;

/**
 *
 * @author paul
 */
public class DynamicComparator implements Comparator {

    private String getterToOrderBy1;
    private String getterToOrderBy2;
    private String getterToOrderBy3;
    private String order;
    private static final String DESC = "desc";
    private Method m1 = null;
    private Method m2 = null;
    private Method m3 = null;

    public DynamicComparator(String getterToOrderBy1, String getterToOrderBy2, String getterToOrderBy3, String order) {
        this.getterToOrderBy1 = getterToOrderBy1;
        this.getterToOrderBy2 = getterToOrderBy2;
        this.getterToOrderBy3 = getterToOrderBy3;
        this.order = order;
    }

    public int compare(Object o1, Object o2) {
        // Ok so, o1 and o2 are both objects in a list. We need to get the result of calling the method "getterToOrderBy" on each object and compare them
        int ret = 0;
        if (m1 == null) {
            try {
                m1 = o1.getClass().getMethod(getterToOrderBy1);
            } catch (Exception ex) {
            }
        }
        if (m2 == null) {
            try {
                m2 = o1.getClass().getMethod(getterToOrderBy2);
            } catch (Exception ex) {
            }
        }
        if (m3 == null) {
            try {
                m3 = o1.getClass().getMethod(getterToOrderBy3);
            } catch (Exception ex) {
            }
        }
        try {
            Object o1Data1 = m1.invoke(o1);
            Object o1Data2 = m2.invoke(o1);
            Object o1Data3 = m3.invoke(o1);
            Object o2Data1 = m1.invoke(o2);
            Object o2Data2 = m2.invoke(o2);
            Object o2Data3 = m3.invoke(o2);
            String o1String = (String) o1Data1 + (String) o1Data2 + (String) o1Data3;
            String o2String = (String) o2Data1 + (String) o2Data2 + (String) o2Data3;
            ret = o1String.compareTo(o2String);
        } catch (Exception ex) {
        }

        if (order.equalsIgnoreCase(DESC)) {
            ret *= -1;
        }
        return ret;
    }
}
