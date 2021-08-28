/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.engine;

/**
 *
 * @author paul
 */
public class RerouteException extends RuntimeException{
    
    private final String routeToHost;
    
    public RerouteException(String routeToHost) {
        this.routeToHost = routeToHost;
    }

    public String getRouteToHost() {
        return routeToHost;
    }
    
}
