/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.hwf;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.hwf.activiti.ActivitiFactory;
import com.smilecoms.xml.hwf.HWFError;
import com.smilecoms.xml.hwf.HWFSoap;
import com.smilecoms.xml.schema.hwf.Done;
import com.smilecoms.xml.schema.hwf.DropDownRow;
import com.smilecoms.xml.schema.hwf.ProcessDefinitionList;
import com.smilecoms.xml.schema.hwf.StDone;
import com.smilecoms.xml.schema.hwf.TaskUpdateData;
import com.smilecoms.xml.schema.hwf.TaskField;
import com.smilecoms.xml.schema.hwf.TaskList;
import com.smilecoms.xml.schema.hwf.Variable;
import com.smilecoms.xml.schema.hwf.TaskQuery;
import com.smilecoms.xml.schema.hwf.PlatformString;
import com.smilecoms.xml.schema.hwf.ProcessHistory;
import com.smilecoms.xml.schema.hwf.ProcessHistoryQuery;
import com.smilecoms.xml.schema.hwf.ProcessInstance;
import com.smilecoms.xml.schema.hwf.VariableAssignmentData;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;

/**
 *
 * @author paul
 */
@WebService(serviceName = "HWF", portName = "HWFSoap", endpointInterface = "com.smilecoms.xml.hwf.HWFSoap", targetNamespace = "http://xml.smilecoms.com/HWF", wsdlLocation = "HWFServiceDefinition.wsdl")
@Stateless
@HandlerChain(file = "/handler.xml")
public class HWFManager extends SmileWebService implements HWFSoap {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;

    @Override
    public com.smilecoms.xml.schema.hwf.Done isUp(java.lang.String isUpRequest) throws HWFError {

        if (BaseUtils.getBooleanProperty("env.is.training.environment", false)) {
            return makeDone();
        }
        if (!BaseUtils.isPropsAvailable()) {
            throw createError(HWFError.class, "Properties are not available so this platform will be reported as down");
        }
        TaskService taskService = ActivitiFactory.getProcessEngine().getTaskService();
        if (taskService != null) {
            return makeDone();
        } else {
            throw this.createError(HWFError.class, "HWF is down");
        }
    }

    @Override
    public TaskList getTaskList(TaskQuery taskQuery) throws HWFError {
        setContext(taskQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart("getTaskList");
        }
        String taskQueryCandidiateUser = taskQuery.getCandidateUser().toLowerCase();
        TaskList taskList = new TaskList();
        Set<String> ids = new TreeSet();

        try {

            int lookBackDays = BaseUtils.getIntProperty("env.hwf.task.query.lookback.days", 7);
            TaskService taskService = ActivitiFactory.getProcessEngine().getTaskService();

            if (taskQuery.getTaskId() != null && !taskQuery.getTaskId().isEmpty()) {
                log.debug("Task query is for task id [{}]", taskQuery.getTaskId());

                Task task = taskService.createTaskQuery().taskId(taskQuery.getTaskId())
                        .includeProcessVariables()
                        .singleResult();
                if (task == null) {
                    throw new Exception("Task has been deleted");
                }
                if (!ids.contains(task.getId())) {
                    ids.add(task.getId());
                    taskList.getTasks().add(getXMLTaskFromActivitiTask(task, taskQuery.getCandidateGroups(), taskQueryCandidiateUser));
                }
                log.debug("Found 1 task for id  [{}]", taskQuery.getTaskId());

            } else {
                String assignee = taskQuery.getAssignee().toLowerCase();

                if (taskQuery.getAssignee() != null && !taskQuery.getAssignee().isEmpty()) {
                    log.debug("Task query is for assignee [{}]", assignee);
                    List<Task> tasks = taskService.createTaskQuery()
                            .taskAssignee(assignee)
                            .taskCreatedAfter(Utils.getPastDate(Calendar.DATE, lookBackDays))
                            .orderByTaskCreateTime().asc().list();
                    for (Task task : tasks) {
                        if (ids.contains(task.getId())) {
                            continue;
                        }
                        ids.add(task.getId());
                        taskList.getTasks().add(getXMLTaskFromActivitiTask(task, taskQuery.getCandidateGroups(), taskQueryCandidiateUser));
                    }
                    log.debug("Found [{}] tasks for assignee [{}]", taskList.getTasks().size(), assignee);

                }
                if (taskQuery.getCandidateGroups() != null && !taskQuery.getCandidateGroups().isEmpty()) {

                    for (String group : taskQuery.getCandidateGroups()) {
                        log.debug("Task query is for candidate group [{}]", group);
                        List<Task> tasks = taskService.createTaskQuery()
                                .taskCandidateGroup(group)
                                .taskCreatedAfter(Utils.getPastDate(Calendar.DATE, lookBackDays))
                                .orderByTaskCreateTime().asc().list();
                        for (Task task : tasks) {
                            if (ids.contains(task.getId())) {
                                continue;
                            }
                            ids.add(task.getId());
                            taskList.getTasks().add(getXMLTaskFromActivitiTask(task, taskQuery.getCandidateGroups(), taskQueryCandidiateUser));
                        }
                        log.debug("Found [{}] tasks for candidate group [{}]", taskList.getTasks().size(), group);
                    }

                }
                if (taskQueryCandidiateUser != null && !taskQueryCandidiateUser.isEmpty()) {

                    log.debug("Task query is for candidate user [{}]", taskQueryCandidiateUser);
                    List<Task> tasks = taskService.createTaskQuery()
                            .taskCandidateUser(taskQueryCandidiateUser)
                            .taskCreatedAfter(Utils.getPastDate(Calendar.DATE, lookBackDays))
                            .orderByTaskCreateTime().asc().list();
                    for (Task task : tasks) {
                        if (ids.contains(task.getId())) {
                            continue;
                        }
                        ids.add(task.getId());
                        taskList.getTasks().add(getXMLTaskFromActivitiTask(task, taskQuery.getCandidateGroups(), taskQueryCandidiateUser));
                    }
                    log.debug("Found [{}] tasks for candidate user [{}]", taskList.getTasks().size(), taskQueryCandidiateUser);

                }
                if (taskQuery.getInvolvedUser() != null && !taskQuery.getInvolvedUser().isEmpty()) {
                    log.debug("Task query is for InvolvedUser  [{}]", taskQuery.getInvolvedUser());
                    List<Task> tasks = taskService.createTaskQuery()
                            .taskInvolvedUser(taskQuery.getInvolvedUser())
                            .taskCreatedAfter(Utils.getPastDate(Calendar.DATE, lookBackDays))
                            .includeProcessVariables()
                            .list();
                    for (Task task : tasks) {
                        if (ids.contains(task.getId())) {
                            continue;
                        }
                        ids.add(task.getId());
                        taskList.getTasks().add(getXMLTaskFromActivitiTask(task, taskQuery.getCandidateGroups(), taskQueryCandidiateUser));
                    }
                    log.debug("Found [{}] tasks for InvolvedUser [{}]", taskList.getTasks().size(), taskQuery.getInvolvedUser());
                }
            }
        } catch (Exception e) {
            throw processError(HWFError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd("getTaskList");
            }
        }
        return taskList;
    }

    private com.smilecoms.xml.schema.hwf.Task getXMLTaskFromActivitiTask(Task task, List<String> usersGroups, String userName) {
        com.smilecoms.xml.schema.hwf.Task xmlTask = new com.smilecoms.xml.schema.hwf.Task();
        xmlTask.setAssignee(task.getAssignee());
        xmlTask.setCreatedDate(Utils.getDateAsXMLGregorianCalendar(task.getCreateTime()));
        xmlTask.setDescription(task.getDescription());
        if (task.getDueDate() != null) {
            xmlTask.setDueDate(Utils.getDateAsXMLGregorianCalendar(task.getDueDate()));
        }
        xmlTask.setOwner(task.getOwner());
        xmlTask.setPriority(task.getPriority());
        xmlTask.setProcessDefinitionId(task.getProcessDefinitionId());
        xmlTask.setProcessName(getProcessName(task.getProcessDefinitionId()));
        xmlTask.setProcessInstanceId(task.getProcessInstanceId());
        xmlTask.setTaskId(task.getId());
        xmlTask.setTaskName(task.getName());
        Map<String, Object> processVars = task.getProcessVariables();
        for (String name : processVars.keySet()) {
            String val = (String) processVars.get(name);
            TaskField taskField = new TaskField();
            taskField.setName(name);
            taskField.setValue(val);
            xmlTask.getProcessFields().add(taskField);
        }
        FormService formService = ActivitiFactory.getProcessEngine().getFormService();
        TaskFormData tfd = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = tfd.getFormProperties();
        for (FormProperty prop : formProperties) {
            TaskField taskField = new TaskField();
            taskField.setId(prop.getId());
            taskField.setName(prop.getName());
            taskField.setValue(prop.getValue());
            taskField.setReadable(prop.isReadable());
            taskField.setRequired(prop.isRequired());
            taskField.setWritable(prop.isWritable());
            taskField.setType(prop.getType() == null ? "string" : prop.getType().getName());
            if (taskField.getType().equals("enum")) {
                Map<String, String> values = (Map<String, String>) prop.getType().getInformation("values");
                if (values != null) {
                    for (Entry<String, String> enumEntry : values.entrySet()) {
                        DropDownRow dd = new DropDownRow();
                        // Add value and label (if any)
                        dd.setOption(enumEntry.getKey());
                        dd.setValue(enumEntry.getValue());
                        taskField.getDropDownRows().add(dd);
                    }
                }
            }
            xmlTask.getTaskFields().add(taskField);
        }

        xmlTask.setCanCallerClaim(false);
        List<String> candidateGroups = getCandidateGroups(task);
        if (usersGroups != null) {
            for (String canGrp : candidateGroups) {
                for (String usrGrp : usersGroups) {
                    log.debug("Looking at Candidate group [{}] and User Group [{}]", canGrp, usrGrp);
                    if (canGrp.equals(usrGrp)) {
                        xmlTask.setCanCallerClaim(true);
                        break;
                    }

                }
            }
        }
        xmlTask.getCandidateGroups().addAll(candidateGroups);

        List<String> candidateUsers = getCandidateUsers(task);

        if (userName != null) {
            for (String canUser : candidateUsers) {
                log.debug("Looking at Candidate User [{}] and Actual User [{}]", canUser, userName);
                if (canUser.equalsIgnoreCase(userName)) {
                    xmlTask.setCanCallerClaim(true);
                    break;
                }
            }
        }

        xmlTask.getCandidateUsers().addAll(candidateUsers);

        return xmlTask;
    }

    @Override
    public com.smilecoms.xml.schema.hwf.Done updateTask(TaskUpdateData taskupdateData) throws HWFError {
        setContext(taskupdateData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            TaskService taskService = ActivitiFactory.getProcessEngine().getTaskService();
            switch (taskupdateData.getAction()) {
                case "claim":
                    taskService.claim(taskupdateData.getTaskId(), taskupdateData.getUserName().toLowerCase());
                    break;
                case "complete":
                    log.debug("[{}] completing task id [{}]", taskupdateData.getUserName(), taskupdateData.getTaskId());
                    taskService.complete(taskupdateData.getTaskId());
                    break;
                case "unclaim":
                    taskService.unclaim(taskupdateData.getTaskId());
                    break;
                case "deleteprocess":
                    Task task = taskService.createTaskQuery().taskId(taskupdateData.getTaskId()).singleResult();
                    ActivitiFactory.getProcessEngine().getRuntimeService().deleteProcessInstance(task.getProcessInstanceId(), "");
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.warn("updateTask Error", e);
            if (e instanceof org.activiti.engine.impl.pvm.PvmException) {
                log.warn("This is an instance of org.activiti.engine.impl.pvm.PvmException");
                Throwable t = Utils.getDeepestCause(e);
                log.warn("E toString [{}] get Message [{}] class[{}]", new Object[]{e.toString(), e.getMessage(), e.getClass().getName()});
                log.warn("Deepest cause toString [{}] get Message [{}] class[{}]", new Object[]{t.toString(), t.getMessage(), t.getClass().getName()});
                e = new Exception(Utils.getDeepestCause(e).getMessage().replace("couldn't execute activity : ", ""));
            }
            throw processError(HWFError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public Done setProcessVariables(VariableAssignmentData variableAssignmentData) throws HWFError {
        setContext(variableAssignmentData, wsctx);
        if (log.isDebugEnabled()) {
            logStart("setProcessVariables");
        }
        try {
            TaskService taskService = ActivitiFactory.getProcessEngine().getTaskService();
            String id = variableAssignmentData.getTaskId();
            for (Variable pv : variableAssignmentData.getVariables()) {
                log.debug("Setting  [{}] to [{}]", pv.getName(), pv.getValue());
                taskService.setVariable(id, pv.getName(), pv.getValue());
            }
        } catch (Exception e) {
            throw processError(HWFError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd("setProcessVariables");
            }
        }
        return makeDone();
    }

    @Override
    public Done setTaskVariables(VariableAssignmentData variableAssignmentData) throws HWFError {
        setContext(variableAssignmentData, wsctx);
        if (log.isDebugEnabled()) {
            logStart("setTaskVariables");
        }
        try {
            FormService formService = ActivitiFactory.getProcessEngine().getFormService();
            String id = variableAssignmentData.getTaskId();
            Map<String, String> vars = new HashMap();
            for (Variable pv : variableAssignmentData.getVariables()) {
                log.debug("Setting  [{}] to [{}]", pv.getName(), pv.getValue());
                vars.put(pv.getName(), pv.getValue());
            }
            formService.saveFormData(id, vars);
        } catch (Exception e) {
            throw processError(HWFError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd("setTaskVariables");
            }
        }
        return makeDone();
    }

    private Done makeDone() {
        com.smilecoms.xml.schema.hwf.Done done = new com.smilecoms.xml.schema.hwf.Done();
        done.setDone(StDone.TRUE);
        return done;
    }

    @Override
    public Done startProcess(PlatformString processDefinitionKey) throws HWFError {
        setContext(processDefinitionKey, wsctx);
        if (log.isDebugEnabled()) {
            logStart("startProcess");
        }
        try {
            org.activiti.engine.runtime.ProcessInstance pi = ActivitiFactory.getProcessEngine().getRuntimeService().startProcessInstanceByKey(processDefinitionKey.getString());
            log.debug("Process Instance name is [{}]", pi.getName());
        } catch (Exception e) {
            throw processError(HWFError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd("startProcess");
            }
        }
        return makeDone();
    }

    @Override
    public Done deleteProcessDefinition(PlatformString processDefinitionKeyToDelete) throws HWFError {
        setContext(processDefinitionKeyToDelete, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            ActivitiFactory.getProcessEngine().getRepositoryService().suspendProcessDefinitionByKey(processDefinitionKeyToDelete.getString());
        } catch (Exception e) {
            throw processError(HWFError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public ProcessDefinitionList getProcessDefinitions(PlatformString processDefinitionQuery) throws HWFError {
        setContext(processDefinitionQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        ProcessDefinitionList processList = new ProcessDefinitionList();
        try {
            RepositoryService repoService = ActivitiFactory.getProcessEngine().getRepositoryService();
            List<ProcessDefinition> pl = repoService.createProcessDefinitionQuery().active().latestVersion().list();
            for (ProcessDefinition p : pl) {
                com.smilecoms.xml.schema.hwf.ProcessDefinition xmlDefinition = new com.smilecoms.xml.schema.hwf.ProcessDefinition();
                xmlDefinition.setKey(p.getKey());
                xmlDefinition.setName(p.getName());
                xmlDefinition.setVersion(p.getVersion());
                processList.getProcessDefinitions().add(xmlDefinition);
                log.debug("Found process definition [{}]", p.getName());
            }
        } catch (Exception e) {
            throw processError(HWFError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return processList;
    }

    @Override
    public ProcessHistory getProcessHistory(ProcessHistoryQuery processHistoryQuery) throws HWFError {
        setContext(processHistoryQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        ProcessHistory processHistory = new ProcessHistory();
        try {
            String name = null;
            if (processHistoryQuery.getProcessDefinitionKey() != null
                    && !processHistoryQuery.getProcessDefinitionKey().isEmpty()) {

                HistoryService historyService = ActivitiFactory.getProcessEngine().getHistoryService();
                List<HistoricProcessInstance> processInstHistory = historyService
                        .createHistoricProcessInstanceQuery()
                        .startedAfter(Utils.getPastDate(Calendar.DATE, BaseUtils.getIntProperty("env.hwf.task.query.lookback.days", 7)))
                        .processDefinitionKey(processHistoryQuery.getProcessDefinitionKey())
                        .orderByProcessInstanceStartTime().asc()
                        .list();

                for (HistoricProcessInstance processInst : processInstHistory) {
                    if (name == null) {
                        name = getProcessName(processInst.getProcessDefinitionId());
                    }
                    ProcessInstance xmlPI = getXMLProcessInstance(processInst);
                    xmlPI.setName(name);
                    processHistory.getProcessInstances().add(xmlPI);
                }
            } else if (processHistoryQuery.getProcessInstanceId() != null
                    && !processHistoryQuery.getProcessInstanceId().isEmpty()) {

                HistoryService historyService = ActivitiFactory.getProcessEngine().getHistoryService();
                List<HistoricProcessInstance> processInstHistory = historyService
                        .createHistoricProcessInstanceQuery()
                        .processInstanceId(processHistoryQuery.getProcessInstanceId())
                        .list();
                for (HistoricProcessInstance processInst : processInstHistory) {
                    if (name == null) {
                        name = getProcessName(processInst.getProcessDefinitionId());
                    }
                    ProcessInstance xmlPI = getXMLProcessInstance(processInst);
                    xmlPI.setName(name);
                    processHistory.getProcessInstances().add(xmlPI);

                    List<HistoricTaskInstance> taskInstHistory = historyService.createHistoricTaskInstanceQuery()
                            .processInstanceId(processInst.getId())
                            .includeProcessVariables()
                            .orderByTaskCreateTime().asc()
                            .list();
                    for (HistoricTaskInstance taskInst : taskInstHistory) {
                        xmlPI.getTasks().add(getXMLHistoricTaskInstance(taskInst));
                    }
                }
            }
        } catch (Exception e) {
            throw processError(HWFError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return processHistory;
    }

    private ProcessInstance getXMLProcessInstance(HistoricProcessInstance processInst) {
        ProcessInstance pi = new ProcessInstance();
        pi.setEndDate(Utils.getDateAsXMLGregorianCalendar(processInst.getEndTime()));
        pi.setStartDate(Utils.getDateAsXMLGregorianCalendar(processInst.getStartTime()));
        pi.setProcessInstanceId(processInst.getId());
        return pi;
    }

    private com.smilecoms.xml.schema.hwf.Task getXMLHistoricTaskInstance(HistoricTaskInstance taskInst) {
        com.smilecoms.xml.schema.hwf.Task task = new com.smilecoms.xml.schema.hwf.Task();
        task.setAssignee(taskInst.getAssignee());
        task.setCreatedDate(Utils.getDateAsXMLGregorianCalendar(taskInst.getCreateTime()));
        task.setDescription(taskInst.getDescription());
        task.setEndDate(Utils.getDateAsXMLGregorianCalendar(taskInst.getEndTime()));
        task.setDueDate(Utils.getDateAsXMLGregorianCalendar(taskInst.getDueDate()));
        task.setOwner(taskInst.getOwner());
        task.setProcessName(getProcessName(taskInst.getProcessDefinitionId()));
        task.setProcessDefinitionId(taskInst.getProcessDefinitionId());
        task.setProcessInstanceId(taskInst.getProcessInstanceId());
        task.setTaskId(taskInst.getId());
        task.setTaskName(taskInst.getName());
        for (String name : taskInst.getProcessVariables().keySet()) {
            String val = String.valueOf(taskInst.getProcessVariables().get(name));//TODO Throwing Cast Exception. 
            TaskField tf = new TaskField();
            tf.setName(name);
            tf.setValue(val);
            task.getProcessFields().add(tf);
        }
        return task;
    }

    private ProcessDefinitionEntity getProcessDefinition(String processDefinitionId) {
        ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) ActivitiFactory.getProcessEngine().getRepositoryService().getProcessDefinition(processDefinitionId);
        return processDefinition;
    }

    public List<String> getCandidateGroups(Task task) {
        List<String> ret = new ArrayList();
        ProcessDefinitionEntity processDefinition = getProcessDefinition(task.getProcessDefinitionId());
        Set<Expression> candidateGroupIdExpressions = processDefinition.getTaskDefinitions().get(task.getTaskDefinitionKey()).getCandidateGroupIdExpressions();
        for (Expression expression : candidateGroupIdExpressions) {
            ret.add(expression.getExpressionText());
        }
        return ret;
    }

    private List<String> getCandidateUsers(Task task) {
        List<String> ret = new ArrayList();
        ProcessDefinitionEntity processDefinition = getProcessDefinition(task.getProcessDefinitionId());
        Set<Expression> candidateUserIdExpressions = processDefinition.getTaskDefinitions().get(task.getTaskDefinitionKey()).getCandidateUserIdExpressions();
        for (Expression expression : candidateUserIdExpressions) {
            ret.add(expression.getExpressionText());
        }
        return ret;
    }

    private String getProcessName(String processDefinitionId) {
        return ActivitiFactory.getProcessEngine().getRepositoryService().getProcessDefinition(processDefinitionId).getName();
    }
}
