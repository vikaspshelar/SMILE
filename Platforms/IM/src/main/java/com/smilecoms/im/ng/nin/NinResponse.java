/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.ng.nin;

import java.util.List;

/**
 *
 * @author bhaskarhg
 */
public class NinResponse {

    protected List<DemoData> data;
    protected String returnMessage;

    public List<DemoData> getData() {
        return data;
    }

    public void setData(List<DemoData> data) {
        this.data = data;
    }

    public String getReturnMessage() {
        return returnMessage;
    }

    public void setReturnMessage(String returnMessage) {
        this.returnMessage = returnMessage;
    }

}
