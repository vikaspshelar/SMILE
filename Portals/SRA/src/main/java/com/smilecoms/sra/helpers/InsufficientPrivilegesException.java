package com.smilecoms.sra.helpers;

/**
 * Thrown when a caller does not have enough privileges to access the resource they want to access
 * @author PCB
 */
public class InsufficientPrivilegesException extends RuntimeException{
    String resource;
    String userName;
    public InsufficientPrivilegesException(String userName, String resource) {
        this.resource = resource;
        this.userName = userName;
    }
    
    public String getResource() {
        return resource;
    }

    @Override
    public String toString() {
        return "Insufficient Privileges for user " + userName + " to access resource " + resource;
    }
    
    
}
