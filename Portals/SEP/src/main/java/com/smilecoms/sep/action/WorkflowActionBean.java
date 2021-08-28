/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.action;

import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.direct.hwf.PlatformString;
import com.smilecoms.commons.sca.direct.hwf.ProcessDefinitionList;
import com.smilecoms.commons.sca.direct.hwf.ProcessHistory;
import com.smilecoms.commons.sca.direct.hwf.ProcessHistoryQuery;
import com.smilecoms.commons.sca.direct.hwf.ProcessInstance;
import com.smilecoms.commons.sca.direct.hwf.Variable;
import com.smilecoms.commons.sca.direct.hwf.Task;
import com.smilecoms.commons.sca.direct.hwf.TaskUpdateData;
import com.smilecoms.commons.sca.direct.hwf.TaskField;
import com.smilecoms.commons.sca.direct.hwf.TaskList;
import com.smilecoms.commons.sca.direct.hwf.TaskQuery;
import com.smilecoms.commons.sca.direct.hwf.VariableAssignmentData;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.sca.helpers.Permissions;
import java.util.Set;
import java.util.TreeSet;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.Resolution;

/**
 *
 * @author Paul
 */
public class WorkflowActionBean extends SmileActionBean {

    @DefaultHandler
    public Resolution showProcesses() {
        processDefinitionList = SCAWrapper.getUserSpecificInstance().getProcessDefinitions_Direct(new PlatformString());
        return getDDForwardResolution("/workflow/view_processes.jsp");
    }

    public Resolution startProcess() {
        checkPermissions(Permissions.KICK_OFF_PROCESS);
        PlatformString ps = new PlatformString();
        ps.setString(getParameter("processKey"));
        SCAWrapper.getUserSpecificInstance().startProcess_Direct(ps);
        setPageMessage("process.started.successfully");
        return showTasks();
    }

    public Resolution deleteProcessDefinition() {
        checkPermissions(Permissions.DELETE_PROCESS_DEFINITION);
        PlatformString ps = new PlatformString();
        ps.setString(getParameter("processKey"));
        SCAWrapper.getUserSpecificInstance().deleteProcessDefinition_Direct(ps);
        setPageMessage("process.deleted.successfully");
        return showProcesses();
    }

    public Resolution showTasks() {
        TaskQuery tq = new TaskQuery();
        tq.getCandidateGroups().addAll(getUsersRoles());
        tq.setAssignee(getUser());
        tq.setCandidateUser(getUser());
        tq.setInvolvedUser(getUser());
        taskList = SCAWrapper.getUserSpecificInstance().getTaskList_Direct(tq);
        taskProcessNameSections = new TreeSet();
        for (Task t : taskList.getTasks()) {
            taskProcessNameSections.add(t.getProcessName());
        }
        return getDDForwardResolution("/workflow/view_tasks.jsp");
    }

    public Resolution showProcessHistoryList() {
        ProcessHistoryQuery q = new ProcessHistoryQuery();
        q.setProcessDefinitionKey(getParameter("processKey"));
        setProcessHistory(SCAWrapper.getUserSpecificInstance().getProcessHistory_Direct(q));
        return getDDForwardResolution("/workflow/view_process_history_list.jsp");
    }

    public Resolution showProcessHistoryDetail() {
        ProcessHistoryQuery q = new ProcessHistoryQuery();
        q.setProcessInstanceId(getParameter("processInstanceId"));
        setProcessHistory(SCAWrapper.getUserSpecificInstance().getProcessHistory_Direct(q));
        setProcessInstance(getProcessHistory().getProcessInstances().get(0));
        return getDDForwardResolution("/workflow/view_process_history_detail.jsp");
    }

    public Resolution showTask() {
        if (getTask() != null) {
            TaskQuery q = new TaskQuery();
            q.setTaskId(getTask().getTaskId());
            setTaskQuery(q);
        }
        getTaskQuery().getCandidateGroups().addAll(getUsersRoles());
        getTaskQuery().setCandidateUser(getUser());
        taskList = SCAWrapper.getUserSpecificInstance().getTaskList_Direct(getTaskQuery());
        if (taskList.getTasks().isEmpty()) {
            return showTasks();
        }
        setTask(taskList.getTasks().get(0));
        return getDDForwardResolution("/workflow/view_task.jsp");
    }

    public Resolution updateTask() {
        showTask();

        VariableAssignmentData vad = new VariableAssignmentData();
        vad.setTaskId(getTask().getTaskId());

        for (TaskField field : getTask().getTaskFields()) {
            Variable tv = new Variable();
            tv.setName(field.getName());
            tv.setValue(getParameter(field.getName()));
            vad.getVariables().add(tv);
            log.debug("Set field [{}] with [{}]", tv.getName(), tv.getValue());
        }
        SCAWrapper.getUserSpecificInstance().setTaskVariables_Direct(vad);
        setPageMessage("task.updated.successfully");
        return showTask();
    }

    public Resolution claimTask() {
        try {
            getTaskUpdateData().setAction("claim");
            getTaskUpdateData().setUserName(getUser());
            SCAWrapper.getUserSpecificInstance().updateTask_Direct(getTaskUpdateData());
        } catch (RuntimeException e) {
            log.warn("Error in task claim", e);
            showTask();
            throw e;
        }
        setPageMessage("task.claimed.successfully");
        return showTask();
    }

    public Resolution unclaimTask() {
        try {
            getTaskUpdateData().setAction("unclaim");
            SCAWrapper.getUserSpecificInstance().updateTask_Direct(getTaskUpdateData());
        } catch (RuntimeException e) {
            log.warn("Error in task unclaim", e);
            showTask();
            throw e;
        }
        setPageMessage("task.unclaimed.successfully");
        return showTask();
    }

    public Resolution completeTask() throws Exception {
        showTask(); //If task was updated then fields will come up. 
        try {
            for (TaskField field : getTask().getTaskFields()) {
                log.debug("**** Looking at [{}] Value [{}]", field.getName(), field.getValue());
                //for some reason, UnitCreditName is returned as null, looking into it.
                if (!field.getName().equals("UnitCreditName")) {
                    if (field.getValue() == null || field.getValue().isEmpty()) {
                        log.debug("It appears that [{}] is missing", field.getName());
                        localiseErrorAndAddToGlobalErrors("hwf.missing.fields");
                        return showTask();
                    }
                }

            }
            getTaskUpdateData().setAction("complete");
            SCAWrapper.getUserSpecificInstance().updateTask_Direct(getTaskUpdateData());
            setPageMessage("task.completed.successfully");
        } catch (RuntimeException e) {
            log.warn("Error in task completion", e);
            showTask();
            throw e;
        }

        return showTasks();
    }

    public Resolution deleteProcess() {
        checkPermissions(Permissions.DELETE_PROCESS_INSTANCE);
        try {
            getTaskUpdateData().setAction("deleteprocess");
            SCAWrapper.getUserSpecificInstance().updateTask_Direct(getTaskUpdateData());
        } catch (RuntimeException e) {
            log.warn("Error in task deletion", e);
            showTask();
            throw e;
        }
        setPageMessage("task.deleted.successfully");
        return showTasks();
    }
    ProcessDefinitionList processDefinitionList;

    public ProcessDefinitionList getProcessDefinitionList() {
        return processDefinitionList;
    }
    TaskQuery taskQuery;

    public TaskQuery getTaskQuery() {
        return taskQuery;
    }

    public void setTaskQuery(TaskQuery taskQuery) {
        this.taskQuery = taskQuery;
    }
    Task task;

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }
    TaskList taskList;

    public TaskList getTaskList() {
        return taskList;
    }
    TaskUpdateData taskUpdateData;

    public TaskUpdateData getTaskUpdateData() {
        return taskUpdateData;
    }

    public void setTaskUpdateData(TaskUpdateData taskUpdateData) {
        this.taskUpdateData = taskUpdateData;
    }
    ProcessHistory processHistory;

    public ProcessHistory getProcessHistory() {
        return processHistory;
    }

    public void setProcessHistory(ProcessHistory processHistory) {
        this.processHistory = processHistory;
    }
    ProcessInstance processInstance;

    public ProcessInstance getProcessInstance() {
        return processInstance;
    }

    public void setProcessInstance(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    private Set<String> taskProcessNameSections;

    public Set<String> getTaskProcessNameSections() {
        return taskProcessNameSections;
    }

}
