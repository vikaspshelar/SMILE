/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.customerwizard;

/**
 *
 * @author paul
 */
public class Question {
    
    private String questionText;
    private String id;
    
    public Question(String questionText, String id) {
        this.questionText = questionText;
        this.id = id;
    }
    
    public String getQuestionText() {
        return questionText;
    }
    
    public String getQuestionId() {
        return id;
    }
}
