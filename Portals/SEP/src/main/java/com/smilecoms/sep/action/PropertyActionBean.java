package com.smilecoms.sep.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.FlushPropertyCacheRequest;
import com.smilecoms.commons.sca.GeneralQueryResponse;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.UpdatePropertyRequest;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.stripes.*;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.commons.util.SSH;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.sourceforge.stripes.action.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Stripes action bean for doing a SIP register
 *
 * @author PCB
 */
public class PropertyActionBean extends SmileActionBean {

    /* *************************************************************************************
     Actions to do something with the instance data and then send the browser somewhere
     ************************************************************************************** */
    public Resolution refreshResourceBundleCache() {
        checkPermissions(Permissions.REFRESH_PROPERTIES);
        LocalisationHelper.refreshAllBundleCaches(true);
        return getDDForwardResolution("/index.jsp");
    }

    public Resolution refreshPropertyCache() {
        checkPermissions(Permissions.REFRESH_PROPERTIES);
        SCAWrapper.getUserSpecificInstance().flushPropertyCache(new FlushPropertyCacheRequest());
        setPageMessage("properties.flushed");
        return getDDForwardResolution("/index.jsp");
    }
    
    private Map<String, Set<String>> roles;

    public void setRoles(Map<String, Set<String>> roles) {
        this.roles = roles;
    }

    public Map<String, Set<String>> getRoles() {
        return roles;
    }

    List<List<String>> queryRows = new ArrayList();

    public List<List<String>> getQueryRows() {
        return queryRows;
    }

    public List<List<String>> getQueryConfig() {
        List<String> queryConfig = BaseUtils.getPropertyAsList("env.pm.generalquery.config");
        List<List<String>> ret = new ArrayList();
        for (String configRow : queryConfig) {
            String[] bits = configRow.split("\\|");
            if (bits.length < 3) {
                continue;
            }
            String query = "";
            for (int i = 2; i < bits.length; i++) {
                query = query + bits[i] + "|";
            }
            query = query.substring(0, query.length() - 1);
            int paramCnt = StringUtils.countMatches(query, "?");
            List<String> config = new ArrayList();
            config.add(bits[0]);
            config.add(bits[1]);
            config.add(query);
            config.add(String.valueOf(paramCnt));
            ret.add(config);
        }
        return ret;
    }

    public List<List<String>> getTaskConfig() {
        List<String> queryConfig = BaseUtils.getPropertyAsList("env.general.tasks.config");
        List<List<String>> ret = new ArrayList();
        for (String configRow : queryConfig) {
            List<String> config = new ArrayList();
            String[] bits = configRow.split("\\|");
            if (bits.length != 4) {
                continue;
            }
            config.addAll(Arrays.asList(bits));
            ret.add(config);
        }
        return ret;
    }

    public Resolution runGeneralQuery() {
        checkPermissions(Permissions.RUN_GENERAL_QUERY);
        GeneralQueryResponse resp = SCAWrapper.getUserSpecificInstance().runGeneralQuery(getGeneralQueryRequest());
        String result = Utils.unzip(resp.getBase64CompressedResult());
        String[] rows = result.split("#");

        for (String row : rows) {
            log.debug("Row is [{}]", row);
            String[] entries = row.split("\\^", -1);
            List<String> entryList = new ArrayList();
            for (String entry : entries) {
                log.debug("Entry is [{}]", entry);
                entryList.add(entry == null ? "" : StringEscapeUtils.escapeHtml(entry));
            }
            queryRows.add(entryList);

        }

        return getDDForwardResolution("/query/general_query_results.jsp");
    }

    public Resolution runGeneralTask() {
        checkPermissions(Permissions.RUN_GENERAL_TASK);
        String taskName = this.getParameter("taskName");
        String taskInput = this.getParameter("taskInput");
        for (List<String> config : getTaskConfig()) {
            if (taskName.equals(config.get(0))) {
                doTask(config.get(2), config.get(3), taskInput);
                break;
            }
        }
        return getDDForwardResolution("/task/run_general_task.jsp");
    }

    public Map<String, String> getAllPermissions() {
        Map<String, String> all = new TreeMap();
        for (Permissions p : Permissions.values()) {
            all.put(p.name(), p.getDescription());
        }
        return all;
    }

    public Resolution showRunGeneralQuery() {
        checkPermissions(Permissions.RUN_GENERAL_QUERY);
        return getDDForwardResolution("/query/run_general_query.jsp");
    }

    public Resolution showRunGeneralTask() {
        checkPermissions(Permissions.RUN_GENERAL_TASK);
        return getDDForwardResolution("/task/run_general_task.jsp");
    }

    public Resolution showManagePermissions() {
        roles = new TreeMap<>();
        for (String roleMapping : BaseUtils.getPropertyAsSet("global.customer.securitygroups")) {
            String role = roleMapping.split("\\-")[1];
            if (role.equals("Customer")) {
                continue;
            }
            Set<String> permissions;
            try {
                permissions = BaseUtils.getPropertyAsSet("env.portal.allowed.resources." + role);
            } catch (Exception e) {
                permissions = new HashSet<>();
            }
            roles.put(role, permissions);
        }
        return getDDForwardResolution("/permissions/manage_permissions.jsp");
    }

    public Resolution sendAllTestMessages() {
        checkPermissions(Permissions.TEST_TRIGGER_EMAILS);
        int custId = getUserCustomerIdFromSession();
        Customer cust = UserSpecificCachedDataHelper.getCustomer(custId, StCustomerLookupVerbosity.CUSTOMER);
        Event testEvent = new Event();
        testEvent.setEventType("CL_TEST");
        testEvent.setEventSubType("TEST");
        testEvent.setEventKey("TEST");
        String data="";
        data = Utils.setValueInCRDelimitedAVPString(data, "EMail", cust.getEmailAddress());
        data = Utils.setValueInCRDelimitedAVPString(data, "Phone", cust.getAlternativeContact1());
        data = Utils.setValueInCRDelimitedAVPString(data, "Language", cust.getLanguage());
        testEvent.setEventData(data);
        SCAWrapper.getAdminInstance().createEvent(testEvent);
        setPageMessage("emails.sent");
        return getDDForwardResolution("/index.jsp");
    }

    public Resolution updateRolePermissions() {
        checkPermissions(Permissions.MODIFY_ROLES_PERMISSIONS);
        UpdatePropertyRequest upr;
        try {
            if (!getRoles().isEmpty()) {
                for (Map.Entry<String, Set<String>> entry : getRoles().entrySet()) {
                    upr = new UpdatePropertyRequest();
                    upr.setClient(BaseUtils.SCA_CLIENT_ID);
                    upr.setPropertyName("env.portal.allowed.resources.".concat(entry.getKey()));
                    upr.setPropertyValue(Utils.makeCRDelimitedStringFromList(entry.getValue()));
                    SCAWrapper.getUserSpecificInstance().updateProperty(upr);
                    log.debug("Modifying Properties [{}] [{}]", "env.portal.allowed.resources.".concat(entry.getKey()), Utils.makeCRDelimitedStringFromList(entry.getValue()));
                }
            }
            setPageMessage("permissions.update.success");
        } catch (SCABusinessError e) {
            showManagePermissions();
            throw e;
        }
        return getDDForwardResolution("/index.jsp");
    }

    @DefaultHandler
    public Resolution goHome() {
        return getDDForwardResolution(LoginActionBean.class, "goHome");
    }

    private String taskOutput;

    public java.lang.String getTaskOutput() {
        return taskOutput;
    }

    public void setTaskOutput(java.lang.String taskOutput) {
        this.taskOutput = taskOutput;
    }

    private void doTask(String host, String script, String taskInput) {

        if (!taskInput.isEmpty() && taskInput.contains(";")) {
            this.localiseErrorAndAddToGlobalErrors("invalid.general.task.paramater");
            return;
        }
        
        Event event = new Event();
        event.setEventType("TASK");
        event.setEventSubType("RAN");
        event.setEventKey(getUser());
        String cmd = "/bin/bash " + script + " \"" + taskInput + "\" \"" + getUser() + "\"";
        event.setEventData(cmd);
        SCAWrapper.getUserSpecificInstance().createEvent(event);
        setTaskOutput("Result Of " + cmd + ":\n\n"
                + SSH.executeRemoteOSCommand(BaseUtils.getProperty("env.general.task.ssh.user", "root"),
                        BaseUtils.getProperty("env.general.task.ssh.password", "newroot"),
                        host,
                        cmd));
    }
    
     public Resolution refreshKitsFromX3() {
        checkPermissions(Permissions.REFRESH_PROPERTIES);        
        checkPermissions(Permissions.RUN_GENERAL_TASK);        
        
        String taskName = "Refresh X3 Inventory Kits";
        String taskInput = "none";
        for (List<String> config : getTaskConfig()) {
            if (taskName.equals(config.get(0))) {
                doTask(config.get(2), config.get(3), taskInput);                
                break;
            }
        }
        
        setPageMessage("inventory.flushed");
        return getDDForwardResolution("/index.jsp");
    }
}
