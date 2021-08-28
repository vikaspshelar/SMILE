/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.tz.verify.defaced.restclient;

import com.smilecoms.im.tz.foreigner.verify.restclient.FingerPrints;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author bhaskarhg
 */
@XmlRootElement
public class VerifyDefacedCustomerRequest {
    private String nin;
    private String questionCode;
    private String answer;

    public String getNin() {
        return nin;
    }

    public void setNin(String nin) {
        this.nin = nin;
    }

    public String getQuestionCode() {
        return questionCode;
    }

    public void setQuestionCode(String questionCode) {
        this.questionCode = questionCode;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
