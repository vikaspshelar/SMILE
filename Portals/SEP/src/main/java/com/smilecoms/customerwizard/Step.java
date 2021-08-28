/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.customerwizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class Step {

    private Question question;
    private List<Option> optionList = new ArrayList<Option>();
    private String id;
    private String parentStepId;
    private static final Map<String,Step> allSteps = new HashMap<String,Step>();
    private static final Logger log = LoggerFactory.getLogger(Step.class);
    
    public Step(Question question, String id, String parentStepId) {
        this.question = question;
        this.id = id;
        this.parentStepId = parentStepId;
        log.debug("Adding Step with id [{}]", id);
        allSteps.put(id, this);
    }
    
    public void addOption(Option option) {
        optionList.add(option);
    }

    public List<Option> getOptions() {        
            return optionList;
    }

    public Question getQuestion() {
            return question;
    }

    public Step getParentStep() {    
        if (parentStepId == null) {
            return null;
        }
        return getStepById(parentStepId);
    }

    public String getStepId() {
        return id;
    }
    
    public static Step getStepById(String stepId) {
       Step step = allSteps.get(stepId);
       return step;
    }
}
