/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base.sd;

import com.hazelcast.core.IMap;
import java.io.Serializable;

/**
 *
 * @author paul
 */
public interface IService extends Serializable {

    public static enum STATUS {
        UP,
        GOING_DOWN,
        PAUSED,
        DOWN
    };
    
    public String getServiceName();
    
    public String getHostName();
    
    public String getIPAddress();
    
    public int getPort();
    
    boolean isStale();

    String getKey();
    
    public String getClientHostnameRegexMatch();

    public boolean isAvailable(boolean mustBeSameVersion, boolean mustMatchHost);
    
    public int getWeight();
    
    public void setGoingDown();
    
    public void setPaused();
    
    public void resume();
    
    public boolean isUp();
    
    public boolean isGoingDown();
    
    public void doHealthCheck(IMap<String, IService> persistence);
    
    public String  getURL();
    
    public String  getAddressPart();
    
    public String  getVersion();
    
    public long getMillisSinceLastModified();
    
    public String getStatus();
    
    public void setStatus(STATUS status);
    
    public boolean isInTheJVM(String jvmsId);
    
    public String getJVMId();
    
    public void persist(IMap<String, IService> persistence);
    
    public void persistIfDifferent(IMap<String, IService> persistence, IService otherService);
    
    public void inheritHistory(IService parent);
    
    public long getLastTested();
    
    public void setTestTimeoutMillis(int ms);
    
    public int getGapBetweenTestsMillis();
    
    public long getMillisSinceLastTestFail();
    
    public boolean doesNeedHealthCheck(String requestorsHostName, String requestorsJvmsId);
}
