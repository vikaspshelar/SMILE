/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.action;

import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.ticker.TickerMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.Resolution;

/**
 *
 * @author jaybeepee
 */
public class TickerActionBean extends SmileActionBean {
    public static final String TICKER_KEY="env.sep.ticker.messages";
    
    /**
     * helper method to update the ticket text in remote cache
     * we use an expiry of a year... as permanent as we need for now 
     * @param key  
     * @param tickerText 
     */
    public void putTickerTextIntoRemoteCache(String key, String tickerText) {
        CacheHelper.putInRemoteCache(key, tickerText, 24*3600*30); // 30 days
    }
    
    /**
     * main method to build the list of ticker messages to display for update
     * We get the current list of global ticker messages from remote cache
     * @return Stripes resolution
     */
    @DefaultHandler
    public Resolution showEditTicker() {
        checkPermissions(Permissions.UPDATE_TICKER);
        tickerMessageList = new ArrayList<TickerMessage>();
        
        String tickerString = (String)CacheHelper.getFromRemoteCache(TICKER_KEY);

        if (tickerString != null) {
            //patterns is "message1<url1>|message2<url2>, etc"
            String[] tickerMessageStrings = tickerString.split("\\|");
            if (tickerMessageStrings != null && tickerMessageStrings.length > 0) {
                Pattern p = Pattern.compile("^(\\S+[\\S|\\s]+)\\<(\\S+)\\>");
                for (String msg : tickerMessageStrings) {
                    Matcher m = p.matcher(msg);
                    if (m.find()) {
                        tickerMessageList.add(new TickerMessage(m.group(1), m.group(2)));
                    }                
                }
            }        
        }
        return getDDForwardResolution("/ticker/edit_ticker.jsp");
    }
    
    /**
     * called to update global ticker messages. This method write the messages to the remote cache
     * @return Page resolution
     */
    public Resolution updateGlobalTicker() {  
        checkPermissions(Permissions.UPDATE_TICKER);
        String propertyTicker = "";
        
        if (tickerMessageList != null) {
            for (TickerMessage msg : tickerMessageList) {
                if (msg == null) {
                    continue;
                }
                if (propertyTicker.length() > 0) {
                    propertyTicker = propertyTicker.concat("|");
                }
                propertyTicker = propertyTicker + msg.getMessage() + "<" + msg.getUrl() + ">";
            }
        }
        
        if (propertyTicker.equals("")) {
            propertyTicker = "empty";
        }
        
        putTickerTextIntoRemoteCache(TICKER_KEY, propertyTicker);        
        
        setPageMessage("ticker.updated");
        return getDDForwardResolution("/index.jsp");
    }

    private List<TickerMessage> tickerMessageList;
    
//    @ValidateNestedProperties ({
//        @Validate(field="message", required=true, maxlength=60),
//        @Validate(field="url", required=true, maxlength=25),
//    })
    public List<TickerMessage> getTickerMessageList() {
        return tickerMessageList;
    }

    public void setTickerMessageList(List<TickerMessage> tickerMessageList) {
        this.tickerMessageList = tickerMessageList;
    }
    
    
}
