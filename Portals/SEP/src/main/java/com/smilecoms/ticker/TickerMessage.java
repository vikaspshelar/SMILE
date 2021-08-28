/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.ticker;

/**
 *
 * @author jaybeepee
 */
public class TickerMessage {
    private String message;
    private String url;

    public TickerMessage() {
    }
    
    public TickerMessage(String message, String url) {
        this.message = message;
        this.url = url;
    }

    public TickerMessage(String message) {
        this.message = message;
        this.url = "#";
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }    
}
