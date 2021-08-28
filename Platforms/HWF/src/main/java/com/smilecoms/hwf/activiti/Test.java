/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.hwf.activiti;

import java.util.List;
import org.activiti.engine.FormService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class Test {

    private static final Logger log = LoggerFactory.getLogger(Test.class);

    public void doit() {


        // Start a process instance
        log.debug("Starting process instance");
        String procId = ActivitiFactory.getProcessEngine().getRuntimeService().startProcessInstanceByKey("test1").getId();
        
        
        List<ProcessDefinition> p = ActivitiFactory.getProcessEngine().getRepositoryService().createProcessDefinitionQuery().latestVersion().list();
        
        
        
        // Get the first task
        log.debug("Getting task service");
        TaskService taskService = ActivitiFactory.getProcessEngine().getTaskService();
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("SDClerk").list();
        log.debug("Getting a task for SDClerk group and assigning it to Paul");
        Task theTask = tasks.get(0);
        String id = theTask.getId();
        taskService.claim(id, "Paul");
        log.debug("Task [{}] is now claimed by Paul", theTask.getName());
        
        
        log.debug("Paul must now do this task... lets find out what the form data is all about...");
        
        FormService formService = ActivitiFactory.getProcessEngine().getFormService();
        
        
        TaskFormData tfd = formService.getTaskFormData(id);
        List<FormProperty> formProperties = tfd.getFormProperties();
        
        
        for (FormProperty prop : formProperties) {
            String propId = prop.getId();
            String value = prop.getValue();
            log.debug("Variable ID [{}] value [{}]", new Object[]{propId,value});
            
            taskService.setVariable(id, propId, "testing 123");
            
        }
        
        
        tfd = formService.getTaskFormData(id);
        formProperties = tfd.getFormProperties();
        for (FormProperty prop : formProperties) {
            String propId = prop.getId();
            String value = prop.getValue();
            log.debug("Variable ID [{}] value [{}]", new Object[]{propId,value});
        }
        
        log.debug("Going to tell engine that Paul has completed this task");
        taskService.complete(id);
        
        // Retrieve and claim the second task
        tasks = taskService.createTaskQuery().taskCandidateGroup("Administrator").list();
        log.debug("Getting a task for Administrator group and assigning it to Jason");
        theTask = tasks.get(0);
        log.debug("Going to tell engine that Jason has completed this task");
        taskService.complete(theTask.getId());
        
        log.debug("The engine should now be calling com.smilecoms.commons.sca.ActivitiSCADelegate and pass SCAMethod as CreateSubscription");
        
        log.debug("Finished");
    }
}
