/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

import java.util.Date;

/**
 *
 * @author paul
 */
public class ServiceInstance {

    private long accountId;
    private int serviceInstanceId;
    private String info;
    private int productInstanceId;
    private int serviceSpecificationId;
    private int productSpecificationId;
    private int productInstanceLogicalId;
    private Date productInstanceLastActivityDate;
    private Date lastActivityDate;
    private Date productInstanceLastRGActivityDate;
    private Date lastRGActivityDate;

    public long getAccountId() {
        return accountId;
    }

    public int getProductInstanceId() {
        return productInstanceId;
    }

    public void setProductInstanceId(int productInstanceId) {
        this.productInstanceId = productInstanceId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(int serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public int getServiceSpecificationId() {
        return serviceSpecificationId;
    }

    public void setServiceSpecificationId(int serviceSpecificationId) {
        this.serviceSpecificationId = serviceSpecificationId;
    }

    public int getProductSpecificationId() {
        return productSpecificationId;
    }

    public void setProductSpecificationId(int productSpecificationId) {
        this.productSpecificationId = productSpecificationId;
    }

    public int getProductInstanceLogicalId() {
        return productInstanceLogicalId;
    }

    public void setProductInstanceLogicalId(int productInstanceLogicalId) {
        this.productInstanceLogicalId = productInstanceLogicalId;
    }

    public Date getProductInstanceLastActivityDate() {
        return productInstanceLastActivityDate;
    }

    public void setProductInstanceLastActivityDate(Date productInstanceLastActivityDate) {
        this.productInstanceLastActivityDate = productInstanceLastActivityDate;
    }

    public Date getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(Date lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public Date getProductInstanceLastRGActivityDate() {
        return productInstanceLastRGActivityDate;
    }

    public void setProductInstanceLastRGActivityDate(Date productInstanceLastRGActivityDate) {
        this.productInstanceLastRGActivityDate = productInstanceLastRGActivityDate;
    }

    public Date getLastRGActivityDate() {
        return lastRGActivityDate;
    }

    public void setLastRGActivityDate(Date lastRGActivityDate) {
        this.lastRGActivityDate = lastRGActivityDate;
    }

    @Override
    public String toString() {
        return "Account Id:" + accountId + " Service Instance ID:" + serviceInstanceId + " Info:" + info;
    }

}
