/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.hwf.activiti;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class ExceptionGenerator implements JavaDelegate {

    private Expression exception;
    private static final Logger log = LoggerFactory.getLogger(ExceptionGenerator.class);
    
    @Override
    public void execute(DelegateExecution de) throws Exception {
        String msg = (String) exception.getValue(de);
        log.debug("ExceptionGenerator making an exception with text [{}]", msg);
        throw new ActivitiWorkflowException(msg);
    }

    public void setException(Expression exception) {
        this.exception = exception;
    }
    
    
    
}
