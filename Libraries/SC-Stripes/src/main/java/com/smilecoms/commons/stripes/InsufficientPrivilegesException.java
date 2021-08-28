package com.smilecoms.commons.stripes;

/**
 * Thrown when a caller does not have enough priveledges to access the resource they want to access
 * @author PCB
 */
public class InsufficientPrivilegesException extends RuntimeException{
    String resource;
    public InsufficientPrivilegesException(String resource) {
        this.resource = resource;
    }
    
    public String getResource() {
        return resource;
    }
}
