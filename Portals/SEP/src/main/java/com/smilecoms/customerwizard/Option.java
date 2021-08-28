/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.customerwizard;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class Option {

    private final String optionText;
    private final String id;
    private final String nextId;
    private final String stepId;
    private static final Logger log = LoggerFactory.getLogger(Option.class);
    private static final Map<String,Option> allOptions = new HashMap<>(); 
    private final HashMap<String, String> fastForwardTTFixedFields;

    public Option(String optionText, String nextId, String id, String stepId) {
        this.optionText = optionText;
        this.id = id;
        this.nextId = nextId;
        this.stepId = stepId;
        allOptions.put(id, this);
        fastForwardTTFixedFields = new HashMap<>();
    }

    public void addFastFowardTTFixedField(String data) {
        String[] parts = data.split(":");
        if (parts.length <= 0) {
            log.error("Fast forward TT Fixed field is not in correct format of {fieldname: filedtext}.... {}", data);
            return;
        }
        
        log.debug("Adding fastforwardTTFixed Field [{}]", data);
        fastForwardTTFixedFields.put(parts[0].trim(), parts[1].trim());       
    }
    
    public Step getNextStep() {
        return Step.getStepById(nextId);
    }
    
    public Step getOwningStep() {
        return Step.getStepById(stepId);
    }
    
    public Leaf getLeaf() {
        return Leaf.getLeafById(nextId);
    }
    
    public String getOptionText() {
        return optionText;
    }

    public String getOptionId() {
        return id;
    }
    
    public static Option getOptionById(String optionId) {
        return allOptions.get(optionId);
    }
    
    public HashMap<String, String> getFastForwardTTFixedFields() {
        return fastForwardTTFixedFields;
    }
}
