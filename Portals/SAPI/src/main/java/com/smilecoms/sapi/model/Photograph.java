/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.model;


/**
 *
 * @author bhaskarhg
 */
public class Photograph {
    
    
    protected String photoGuid;
    
    protected String photoType;
    
    protected String data;

    public String getPhotoGuid() {
        return photoGuid;
    }

    public void setPhotoGuid(String photoGuid) {
        this.photoGuid = photoGuid;
    }

    public String getPhotoType() {
        return photoType;
    }

    public void setPhotoType(String photoType) {
        this.photoType = photoType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
    
    
}
