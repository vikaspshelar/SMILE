/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.tz.verify.defaced.restclient;

import com.smilecoms.im.tz.foreigner.verify.restclient.Info;

/**
 *
 * @author bhaskarhg
 */
public class VerifyDefacedCustomerResponse {
    private String id;
    private String code;
    private Result result;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }
}
