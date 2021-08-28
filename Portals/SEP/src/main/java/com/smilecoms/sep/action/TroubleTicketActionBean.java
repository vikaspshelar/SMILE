/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.*;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.customerwizard.CustomerWizard;
import com.smilecoms.customerwizard.Leaf;
import com.smilecoms.customerwizard.Option;
import com.smilecoms.customerwizard.Step;
import com.smilecoms.commons.sca.helpers.Permissions;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.sourceforge.stripes.action.*;

/**
 *
 * @author Jason
 */
public class TroubleTicketActionBean extends SmileActionBean {

    private final HashMap projectMap = new HashMap();
    private final HashMap issueTypeMap = new HashMap();
    private final HashMap priorityMap = new HashMap();
    private final HashMap resolutionMap = new HashMap();
    private final HashMap statusMap = new HashMap();
    private boolean watchIssue;
    private String projectName;
    private Step currentStep;
    private Leaf currentLeaf;
    private Set<Long> accountIdSet;
    private Map<String, String> fastForwardTTFixedFieldList;
    private int icpSalesAgentId;
    private boolean searchAnyICPSalesAgentIssues = false;
    private boolean icpSalesAgentIssueViewed = false;
    /**
     * used for mindmap file upload
     */
    private FileBean mindMapFile;

    public FileBean getMindMapFile() {
        return mindMapFile;
    }

    public void setMindMapFile(FileBean mindMapFile) {
        this.mindMapFile = mindMapFile;
    }

    public Map<String, String> getFastForwardTTFixedFieldList() {
        return fastForwardTTFixedFieldList;
    }

    public void setFastForwardTTFixedFieldList(Map<String, String> fastForwardTTFixedFieldList) {
        this.fastForwardTTFixedFieldList = fastForwardTTFixedFieldList;
    }

    public Step getCurrentStep() {
        return currentStep;
    }

    public Leaf getCurrentLeaf() {
        return currentLeaf;
    }
    private String currentLeafId;
    private String selectedOptionId;

    public void setCurrentLeafId(java.lang.String currentLeafId) {
        this.currentLeafId = currentLeafId;
    }

    public void setSelectedOptionId(java.lang.String selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }
    private Map<Integer, String> salesAgentsForICPOrganisationList;

    public Map<Integer, String> getSalesAgentsForICPOrganisationList() {
        return salesAgentsForICPOrganisationList;
    }

    public void setSalesAgentsForICPOrganisationList(Map<Integer, String> salesAgentsForICPOrganisationList) {
        this.salesAgentsForICPOrganisationList = salesAgentsForICPOrganisationList;
    }

    public int getIcpSalesAgentId() {
        return icpSalesAgentId;
    }

    public void setIcpSalesAgentId(int icpSalesAgentId) {
        this.icpSalesAgentId = icpSalesAgentId;
    }

    public boolean isSearchAnyICPSalesAgentIssues() {
        return searchAnyICPSalesAgentIssues;
    }

    public void setSearchAnyICPSalesAgentIssues(boolean searchAnyICPSalesAgentIssues) {
        this.searchAnyICPSalesAgentIssues = searchAnyICPSalesAgentIssues;
    }

    public boolean isIcpSalesAgentIssueViewed() {
        return icpSalesAgentIssueViewed;
    }

    public void setIcpSalesAgentIssueViewed(boolean icpSalesAgentIssueViewed) {
        this.icpSalesAgentIssueViewed = icpSalesAgentIssueViewed;
    }

    public Resolution wizardNext() {
        CustomerWizard.getInstance();
        if (fastForwardTTFixedFieldList == null) {
            fastForwardTTFixedFieldList = new HashMap();
        }
        if (currentLeafId != null && !currentLeafId.isEmpty()) {
            // For a leaf under a leaf
            currentLeaf = Leaf.getLeafById(currentLeafId);
        } else if (selectedOptionId != null && !selectedOptionId.isEmpty()) {
            log.debug("User selected an option within a step. Option Id is [{}]", selectedOptionId);
            Option selectedOption = Option.getOptionById(selectedOptionId);

            if (selectedOption.getFastForwardTTFixedFields() != null && selectedOption.getFastForwardTTFixedFields().size() > 0) {
                //we have some more fast forward fields to add from this currently selected option
                for (Entry<String, String> entry : selectedOption.getFastForwardTTFixedFields().entrySet()) {
                    fastForwardTTFixedFieldList.put(entry.getKey(), entry.getValue());
                }
            }
            // One of these will be null and one wont
            currentStep = selectedOption.getNextStep();
            currentLeaf = selectedOption.getLeaf();
            if (log.isDebugEnabled()) {
                if (currentStep != null) {
                    log.debug("Option with Id [{}] indicates next step has Id [{}]", selectedOptionId, currentStep.getStepId());
                }
                if (currentLeaf != null) {
                    log.debug("Option with Id [{}] indicates leaf has Id [{}]", selectedOptionId, currentLeaf.getLeafId());
                }
            }
        }
        return getDDForwardResolution("/trouble_ticket/issue_wizard.jsp");
    }

    public Resolution createTTForWizard() {
        prepareCreationOfTTWizardIssue();
        return insertTroubleTicket();
    }

    public Resolution startWizardAgain() {
        return startWizard();
    }

    public Resolution startWizard() {

        currentStep = CustomerWizard.getInstance().getFirstStep();
        currentLeaf = null;
        if (getCustomer() == null) {
            setCustomer(new Customer());
            getCustomer().setCustomerId(0);
        }
        return getDDForwardResolution("/trouble_ticket/issue_wizard.jsp");
    }

    public Resolution showLoadMindMap() {
        checkPermissions(Permissions.LOAD_MIND_MAP);
        return getDDForwardResolution("/trouble_ticket/load_mind_map.jsp");
    }

    public Resolution loadMindMap() throws IOException {
        log.debug("loading mindmap");
        checkPermissions(Permissions.LOAD_MIND_MAP);
        FileBean fileBean = this.getMindMapFile();
        if (fileBean != null) {
            setPageMessage("mindmap.update.successful");

            InputStream content = fileBean.getInputStream();
            String mindmapXML;

            try {
                mindmapXML = Utils.parseStreamToString(content, getRequest().getCharacterEncoding());
                mindmapXML = mindmapXML.replaceAll("PROJECTKEY", BaseUtils.getProperty("env.jira.customer.care.project.key"));
                mindmapXML = mindmapXML.trim();
                log.debug("going to upload support tree [{}]", mindmapXML);
                UpdatePropertyRequest updateRequest = new UpdatePropertyRequest();
                updateRequest.setClient("default");
                //updateRequest.setClient(System.getProperty("SCA_CLIENT_ID"));
                updateRequest.setPropertyName("env.troubleticket.wizardxml");
                updateRequest.setPropertyValue(mindmapXML);
                SCAWrapper.getUserSpecificInstance().updateProperty(updateRequest);
            } catch (Exception e) {
                log.warn("Error reading uploaded mindmap file");
                setPageMessage("mindmap.update.failed");
            } finally {
                fileBean.delete();
                if (content != null) {
                    content.close();
                }
            }
        } else {
            log.debug("uploaded mindmap failed");
            setPageMessage("mindmap.update.failed");
        }

        return getDDForwardResolution("/index.jsp");
    }

    public Resolution retrieveTTCustomer() throws Exception {
        return getDDForwardResolution(CustomerActionBean.class, "retrieveCustomer");
    }

    @DefaultHandler
    public Resolution retrieveTroubleTickets() {
        return gotoTroubleTicketHome();
    }

    public Resolution addTroubleTicket() {
        checkPermissions(Permissions.TROUBLE_TICKETING);

        TTMetaDataQuery metaDataQuery = new TTMetaDataQuery();
        metaDataQuery.setDummyField("");
        TTMetaData metaData = (TTMetaData) SCAWrapper.getUserSpecificInstance().getTroubleTicketMetaData(metaDataQuery);
        setTTMetaData(metaData);

        TTIssue issue = new TTIssue();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 7);
        Date dueDate = calendar.getTime();
        issue.setDueDate(Utils.getDateAsXMLGregorianCalendar(dueDate));
        setTTIssue(issue);

        return getDDForwardResolution("/trouble_ticket/add_trouble_ticket.jsp");
    }

    public Resolution insertTroubleTicket() {
        checkPermissions(Permissions.TROUBLE_TICKETING);

        NewTTIssue issue;
        if (getNewTTIssue() != null) {
            issue = getNewTTIssue();
        } else {
            issue = new NewTTIssue();
        }

        log.debug("Currently logged in user is [{}] and user ID is [{}]", getUser(), getUserCustomerIdFromSession());
        issue.setCustomerId(Integer.toString(getCustomer().getCustomerId()));
        TTIssue returnIssue;

        try {
            returnIssue = (TTIssue) SCAWrapper.getUserSpecificInstance().createTroubleTicketIssue(issue);
        } catch (SCABusinessError e) {
            throw e;
        }
        setPageMessage("trouble.ticket.added.successfully", returnIssue.getID());

        return gotoTroubleTicketHome();
    }

    public Resolution retrieveTroubleTicketComments() {
        checkPermissions(Permissions.TROUBLE_TICKETING);
        TTIssue issue = getTTIssue();
        String issueID = issue.getID();

        TTCommentQuery commentQuery = new TTCommentQuery();
        commentQuery.setIssueKey(issueID);

        TTCommentList comments = (TTCommentList) SCAWrapper.getUserSpecificInstance().getTroubleTicketComments(commentQuery);

        setTTCommentList(comments);

        return getDDForwardResolution("/trouble_ticket/view_trouble_ticket_comments.jsp");
    }

    public Resolution addTroubleTicketComment() {
        checkPermissions(Permissions.TROUBLE_TICKETING);
        return getDDForwardResolution("/trouble_ticket/add_trouble_ticket_comment.jsp");
    }

    public Resolution insertTroubleTicketComment() {
        checkPermissions(Permissions.TROUBLE_TICKETING);

        TTIssue issue = getTTIssue();
        String issueID = issue.getID();
        TTComment comment = getTTComment();
        comment.setAuthor(Integer.toString(getUserCustomerIdFromSession()));
        comment.setIssueKey(issueID);
        comment.setCreated(Utils.getDateAsXMLGregorianCalendar(new Date()));
        comment.setUpdated(Utils.getDateAsXMLGregorianCalendar(new Date()));
        comment.setID("");
        comment.setIssueKey(issueID);
        SCAWrapper.getUserSpecificInstance().createTroubleTicketComment(comment);
        setPageMessage("trouble.ticket.comment.added.successfully");

        return editTroubleTicket();
    }

    public Resolution queryTroubleTickets() {
        try {
            checkPermissions(Permissions.TROUBLE_TICKETING);

            TTIssueQuery issueQuery = getTTIssueQuery();
            issueQuery.setIssueID("");
            issueQuery.setIncidentChannel("");

            TTIssueList ttIssueList = (TTIssueList) SCAWrapper.getUserSpecificInstance().getTroubleTicketIssues(issueQuery);
            List issueList = ttIssueList.getTTIssueList();
            int issueListSize = issueList.size();

            TTIssueList newTTIssueList = new TTIssueList();
            List newIssueList = newTTIssueList.getTTIssueList();

            for (int i = 0; i < issueListSize; i++) {
                TTIssue issue = (TTIssue) issueList.get(i);
                issue = convertIssue(issue, true, true, true, true, true);
                newIssueList.add(issue);
            }

            setTTIssueList(newTTIssueList);

        } catch (Exception ex) {
            if (ex instanceof SCAErr) {
                SCAErr err = (SCAErr) ex;
                err.getErrorDesc();
                embedErrorIntoStipesActionBeanErrorMechanism(err);
            } else {
                //embedErrorIntoStipesActionBeanErrorMechanism(ex);
            }
        }

        return getDDForwardResolution("/trouble_ticket/view_trouble_tickets.jsp");
    }

    public Resolution editTroubleTicket() {
        checkPermissions(Permissions.TROUBLE_TICKETING);
        TTIssue issue = getTTIssue();
        if (issue == null) {
            return gotoTroubleTicketHome();
        }
        String issueID = issue.getID();

        TTIssueQuery issueQuery = new TTIssueQuery();
        issueQuery.setReporter(Integer.toString(getUserCustomerIdFromSession()));
        issueQuery.setCustomerId(Integer.toString(getCustomer().getCustomerId()));
        issueQuery.setCreatedDateFrom(Utils.getDateAsXMLGregorianCalendar(new java.util.Date()));
        issueQuery.setCreatedDateTo(Utils.getDateAsXMLGregorianCalendar(new java.util.Date()));
        issueQuery.setIssueID(issueID);
        issueQuery.setResultLimit(1);
        issueQuery.setIncidentChannel("");


        TTIssueList ttIssueList = (TTIssueList) SCAWrapper.getUserSpecificInstance().getTroubleTicketIssues(issueQuery);

        List issueList = ttIssueList.getTTIssueList();
        int issueListSize = issueList.size();

        for (int i = 0; i < issueListSize; i++) {
            issue = (TTIssue) issueList.get(i);
            if (issueID.equals(issue.getID())) {
                break;
            }
        }

        issue = convertIssue(issue, true, true, true, true, true);
        setTTIssue(issue);

        watchIssue = false;


        TTCommentQuery commentQuery = new TTCommentQuery();
        commentQuery.setIssueKey(issueID);

        TTCommentList comments = (TTCommentList) SCAWrapper.getUserSpecificInstance().getTroubleTicketComments(commentQuery);
        setTTCommentList(comments);

        setCustomer(UserSpecificCachedDataHelper.getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));

        if (getIcpSalesAgentId() > 0) {
            setIcpSalesAgentIssueViewed(true);
            setIcpSalesAgentId(getIcpSalesAgentId());
        } else {
            setIcpSalesAgentIssueViewed(false);
        }
        return getDDForwardResolution("/trouble_ticket/edit_trouble_ticket.jsp");
    }

    public Resolution updateTroubleTicket() {
        checkPermissions(Permissions.TROUBLE_TICKETING);
        TTIssue modifiedIssue = getTTIssue();

        modifiedIssue.setReporter(Integer.toString(getUserCustomerIdFromSession()));
        modifiedIssue.setUpdated(Utils.getDateAsXMLGregorianCalendar(new java.util.Date()));

        TTIssue issue = (TTIssue) SCAWrapper.getUserSpecificInstance().modifyTroubleTicketIssue(modifiedIssue);
        setPageMessage("trouble.ticket.updated.successfully", issue.getID());

        return gotoTroubleTicketHome();
    }

    public Resolution retrieveComments() {
        checkPermissions(Permissions.TROUBLE_TICKETING);
        String result = "No message specified to retrieve...";
        return new StreamingResolution("text", new StringReader(result));
    }

    private Resolution gotoTroubleTicketHome() {
        checkPermissions(Permissions.TROUBLE_TICKETING);
        TTIssueQuery query = new TTIssueQuery();

        /* Compute dateFrom (7 days ago) and dateTo (current time + 1 day) */
        Calendar rightNow = Calendar.getInstance();
        rightNow.add(Calendar.DATE, +1);
        Date dateTo = rightNow.getTime();
        rightNow.add(Calendar.DATE, -8);
        Date dateFrom = rightNow.getTime();

        query.setCreatedDateFrom(Utils.getDateAsXMLGregorianCalendar(dateFrom));
        query.setCreatedDateTo(Utils.getDateAsXMLGregorianCalendar(dateTo));
        if (getCustomer() != null) {
            query.setCustomerId(Integer.toString(getCustomer().getCustomerId()));
        }
        setTTIssueQuery(query);

        return getDDForwardResolution("/trouble_ticket/view_trouble_tickets.jsp");
    }

    private TTIssue convertIssue(TTIssue issue,
            boolean projectFlag,
            boolean issueTypeFlag,
            boolean priorityFlag,
            boolean resolutionFlag,
            boolean statusFlag) {
        if (projectFlag) {
            //issue.setProject((String) projectMap.get(issue.getProject()));
            issue.setProject((issue.getProject()));
        }
        if (issueTypeFlag) {
            issue.setIssueType((issue.getIssueType()));
        }
        if (priorityFlag) {
            issue.setPriority((issue.getPriority()));
        }
        if (resolutionFlag) {
            issue.setResolution((issue.getResolution()));
        }
        if (statusFlag) {
            issue.setStatus((issue.getStatus()));
        }
        return issue;
    }

    private void populateMaps(TTMetaData metaData) {
        List projectList = metaData.getTTProjectList();
        int projectListSize = projectList.size();
        for (int i = 0; i < projectListSize; i++) {
            TTProject project = (TTProject) projectList.get(i);
            String key = project.getID();
            String name = project.getName();
            projectMap.put(key, name);
        }

        List issueTypeList = metaData.getTTIssueTypeList();
        int issueTypeListSize = issueTypeList.size();
        for (int i = 0; i < issueTypeListSize; i++) {
            TTIssueType issueType = (TTIssueType) issueTypeList.get(i);
            String key = issueType.getID();
            String name = issueType.getName();
            issueTypeMap.put(key, name);
        }

        List priorityList = metaData.getTTPriorityList();
        int priorityListSize = priorityList.size();
        for (int i = 0; i < priorityListSize; i++) {
            TTPriority priority = (TTPriority) priorityList.get(i);
            String key = priority.getID();
            String name = priority.getName();
            priorityMap.put(key, name);
        }

        List resolutionList = metaData.getTTResolutionList();
        int resolutionListSize = resolutionList.size();
        for (int i = 0; i < resolutionListSize; i++) {
            TTResolution resolution = (TTResolution) resolutionList.get(i);
            String key = resolution.getID();
            String name = resolution.getName();
            resolutionMap.put(key, name);
        }

        List statusList = metaData.getTTStatusList();
        int statusListSize = statusList.size();
        for (int i = 0; i < statusListSize; i++) {
            TTStatus status = (TTStatus) statusList.get(i);
            String key = status.getID();
            String name = status.getName();
            statusMap.put(key, name);
        }
    }

    public boolean isWatchIssue() {
        return watchIssue;
    }

    public void setWatchIssue(boolean watchIssue) {
        this.watchIssue = watchIssue;
    }

    public String getProjectName() {
        return projectName;
    }

    public Set getIssueFunctionalAreas() {
        Set<String> mappings = BaseUtils.getPropertyAsSet("env.jira.area.to.project.mappings");
        HashSet<String> ret = new HashSet();
        for (String mapping : mappings) {
            ret.add(mapping.split("\\|")[0]);
        }
        return ret;

    }

    public Resolution getFunctionalAreaDescriptionTemplate() {
        // Find the template from the functional area
        String res = "";
        return new StreamingResolution("text", new StringReader(res));

    }

    public Resolution retrieveTTViaStream() {
        try {

            setTTIssueQuery(new TTIssueQuery());
            Date fromDate;
            Calendar cal = Calendar.getInstance(getLocale());
            int month = BaseUtils.getIntProperty("env.jira.months.number.filter.by");
            cal.add(Calendar.MONTH, -month);
            fromDate = cal.getTime();

            getTTIssueQuery().setCreatedDateFrom(Utils.getDateAsXMLGregorianCalendar(fromDate));
            getTTIssueQuery().setCreatedDateTo(Utils.getDateAsXMLGregorianCalendar(new Date()));
            getTTIssueQuery().setIssueID("");
            getTTIssueQuery().setCustomerId(Integer.toString(getCustomer().getCustomerId()));
            getTTIssueQuery().setIncidentChannel("");
            getTTIssueQuery().setResultLimit(50);

            TTIssueList ttIssueList = (TTIssueList) SCAWrapper.getUserSpecificInstance().getTroubleTicketIssues(getTTIssueQuery());

            List issueList = ttIssueList.getTTIssueList();
            int issueListSize = issueList.size();

            TTIssueList newTTIssueList = new TTIssueList();

            List newIssueList = newTTIssueList.getTTIssueList();

            for (int i = 0; i < issueListSize; i++) {
                TTIssue issue = (TTIssue) issueList.get(i);
                issue = convertIssue(issue, true, true, true, true, true);
                newIssueList.add(issue);
            }

            setTTIssueList(newTTIssueList);
            return getDDForwardResolution("/trouble_ticket/tt_xmlhttp.jsp");

        } catch (Exception ex) {
            setTTIssueList(null);
            log.warn("Error getting TT for xmlhttp: ", ex);
            return new ForwardResolution("/trouble_ticket/tt_xmlhttp.jsp");
        }
    }

    public Resolution searchSalesLeadAssignedTickets() {

        try {
            TTIssueQuery iq = new TTIssueQuery();
            Date fromDate;
            Calendar cal = Calendar.getInstance(getLocale());
            int month = BaseUtils.getIntProperty("env.jira.months.number.filter.by");
            cal.add(Calendar.MONTH, -month);
            fromDate = cal.getTime();

            iq.setCreatedDateFrom(Utils.getDateAsXMLGregorianCalendar(fromDate));
            iq.setCreatedDateTo(Utils.getDateAsXMLGregorianCalendar(new Date()));
            iq.setIssueID("");
            iq.setCustomerId("0");
            iq.setResultLimit(50);
            String salesLeadPK = BaseUtils.getProperty("env.jira.saleslead.project.key");
            String slUsername = getTTUser().getUserID();
            iq.setSalesLeadAssignee(slUsername);
            iq.setSalesLeadPK(salesLeadPK);

            TTIssueList ttIssueList = (TTIssueList) SCAWrapper.getUserSpecificInstance().getTroubleTicketIssues(iq);

            setTTUser(new TTUser());
            getTTUser().setUserID(slUsername);
            setTTIssueList(ttIssueList);
        } catch (Exception ex) {
            setPageMessage("tt.saleslead.username.notfound.error", getTTUser().getUserID());
            return getDDForwardResolution("/trouble_ticket/search_sales_lead_username.jsp");
        }
        if (getTTIssueList().getTTIssueCount() <= 0) {
            setPageMessage("tt.saleslead.username.notfound", getTTUser().getUserID());
            return getDDForwardResolution("/trouble_ticket/search_sales_lead_username.jsp");
        }
        return getDDForwardResolution("/trouble_ticket/view_sales_lead_tickets.jsp");
    }

    public Resolution showSalesLeadUsernamePage() {

        TTJiraUserList jiraUsers;
        if (getTTJiraUserQuery() != null) {
            checkPermissions(Permissions.VIEW_SALESLEADS_FOR_ASSIGNEE);
            jiraUsers = SCAWrapper.getUserSpecificInstance().getJiraUsers(getTTJiraUserQuery());
        } else {

            Customer sepUser = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER);
            TTJiraUserQuery jq = new TTJiraUserQuery();
            jq.setFirstName(sepUser.getFirstName());
            jq.setLastName(sepUser.getLastName());
            jq.setEmailAddress(sepUser.getEmailAddress());
            jq.setResultLimit(4);
            jiraUsers = SCAWrapper.getUserSpecificInstance().getJiraUsers(jq);
        }

        if (jiraUsers.getNumberOfUsers() > 1) {
            setTTJiraUserList(jiraUsers);
            return getDDForwardResolution("/trouble_ticket/search_sales_lead_username.jsp");
        } else if (jiraUsers.getNumberOfUsers() == 1) {
            setTTUser(new TTUser());
            getTTUser().setUserID(jiraUsers.getJiraUsers().get(0).getUserID());
            return searchSalesLeadAssignedTickets();
        } else {
            String salesLead = (getTTJiraUserQuery().getUsername() == null ? "" : getTTJiraUserQuery().getUsername());
            if (salesLead.isEmpty()) {
                salesLead = (getTTJiraUserQuery().getEmailAddress() == null ? "" : getTTJiraUserQuery().getEmailAddress());
            }
            if (salesLead.isEmpty()) {
                salesLead = (getTTJiraUserQuery().getLastName() == null ? "" : getTTJiraUserQuery().getLastName());
            }
            if (salesLead.isEmpty()) {
                salesLead = (getTTJiraUserQuery().getFirstName() == null ? "" : getTTJiraUserQuery().getFirstName());
            }
            if (salesLead.isEmpty()) {
                salesLead = (getTTJiraUserQuery().getIdentityNumber() == null ? "" : getTTJiraUserQuery().getIdentityNumber());
            }
            if (salesLead.isEmpty()) {
                salesLead = " ";
            }
            setPageMessage("tt.saleslead.username.notfound", salesLead);
            return getDDForwardResolution("/trouble_ticket/search_sales_lead_username.jsp");
        }
    }

    public Resolution showAddCustomerBasicDetailsPage() {
        return getDDForwardResolution(CustomerActionBean.class, "showAddCustomerBasicDetails");
    }

    public Resolution getSalesLeadByWildCardAsJSON() {

        String salesLeadPK = BaseUtils.getProperty("env.jira.saleslead.project.key");
        TTIssueQuery issueQuery = getTTIssueQuery();

        issueQuery.setIncidentChannel("");
        issueQuery.setCustomerId("0");
        issueQuery.setResultLimit(10);

        issueQuery.setSalesLeadAssignee("");
        issueQuery.setSalesLeadPK(salesLeadPK);
        issueQuery.setCreatedDateFrom(Utils.getDateAsXMLGregorianCalendar(new Date()));
        issueQuery.setCreatedDateTo(Utils.getDateAsXMLGregorianCalendar(new Date()));

        TTIssueList ttIssueList = (TTIssueList) SCAWrapper.getUserSpecificInstance().getTroubleTicketIssues(issueQuery);
        List issueList = ttIssueList.getTTIssueList();
        int issueListSize = issueList.size();

        TTIssueList newTTIssueList = new TTIssueList();
        List newIssueList = newTTIssueList.getTTIssueList();

        for (int i = 0; i < issueListSize; i++) {
            TTIssue issue = (TTIssue) issueList.get(i);
            issue = convertIssue(issue, true, true, true, true, true);
            newIssueList.add(issue);
        }

        setTTIssueList(newTTIssueList);
        getTTIssueList().setTTIssueCount(issueListSize);
        return getJSONResolution(getTTIssueList());
    }

    public Resolution showCreateIssueForICPPage() {
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
        return getDDForwardResolution("/trouble_ticket/add_icp_ticket.jsp");
    }

    public Resolution showSearchIssueForICPPage() {

        checkPermissions(Permissions.TROUBLE_TICKETING);
        TTIssueQuery query = new TTIssueQuery();

        Calendar rightNow = Calendar.getInstance();
        rightNow.add(Calendar.DATE, +1);
        Date dateTo = rightNow.getTime();
        rightNow.add(Calendar.DATE, -8);
        Date dateFrom = rightNow.getTime();

        query.setCreatedDateFrom(Utils.getDateAsXMLGregorianCalendar(dateFrom));
        query.setCreatedDateTo(Utils.getDateAsXMLGregorianCalendar(dateTo));
        int custId = getUserCustomerIdFromSession();
        query.setCustomerId(String.valueOf(custId));

        Customer loggedInCustomer = UserSpecificCachedDataHelper.getCustomer(custId, StCustomerLookupVerbosity.CUSTOMER);
        setCustomer(loggedInCustomer);
        setTTIssueQuery(query);

        salesAgentsForICPOrganisationList = new HashMap<>();
        Set<String> icpManager = getUsersRoles();
        
        if (icpManager.contains("ICPManager")) {//TODO: Should it be based on roles or permissions? I think to avoid a lot of CC calls just base it on Roles
            setSearchAnyICPSalesAgentIssues(true);
            for (CustomerRole role : loggedInCustomer.getCustomerRoles()) {
                Organisation org = UserSpecificCachedDataHelper.getOrganisation(role.getOrganisationId(), StOrganisationLookupVerbosity.MAIN_ROLES);
                for (CustomerRole roleInOrg : org.getCustomerRoles()) {
                    salesAgentsForICPOrganisationList.put(roleInOrg.getCustomerId(), roleInOrg.getCustomerName());
                }
            }
        } else {
            setSearchAnyICPSalesAgentIssues(false);
        }

        return getDDForwardResolution("/trouble_ticket/search_icp_trouble_tickets.jsp");
    }

    public Resolution createIssueForICP() {
        checkPermissions(Permissions.TROUBLE_TICKETING);

        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
        prepareCreationOfIssueForICP();

        NewTTIssue issue;
        if (getNewTTIssue() != null) {
            issue = getNewTTIssue();
        } else {
            issue = new NewTTIssue();
        }

        TTIssue returnIssue;

        try {
            returnIssue = (TTIssue) SCAWrapper.getUserSpecificInstance().createTroubleTicketIssue(issue);
        } catch (SCABusinessError e) {
            throw e;
        }
        setPageMessage("trouble.ticket.added.successfully", returnIssue.getID());
        return getDDForwardResolution("/trouble_ticket/add_icp_ticket.jsp");
    }

    public Resolution searchIssueForICP() {

        try {

            TTIssueQuery iq = getTTIssueQuery();

            iq.setIssueID("");
            int custId;

            Set<String> icpManager = getUsersRoles();
            Customer loggedInCustomer = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER);
            iq.setResultLimit(50);

            if (icpManager.contains("ICPManager")) {//TODO: Should it be based on roles or permissions? I think to avoid a lot of CC calls just base it on Roles

                if (getIcpSalesAgentId() > 0) {
                    custId = getIcpSalesAgentId();
                    setSearchAnyICPSalesAgentIssues(true);

                    if (custId == loggedInCustomer.getCustomerId()) {
                        setIcpSalesAgentIssueViewed(false);
                    } else {
                        setIcpSalesAgentIssueViewed(true);
                    }

                } else {
                    setSearchAnyICPSalesAgentIssues(true);
                    setIcpSalesAgentIssueViewed(false);
                    custId = loggedInCustomer.getCustomerId();
                }

                salesAgentsForICPOrganisationList = new HashMap<>();
                for (CustomerRole role : loggedInCustomer.getCustomerRoles()) {
                    Organisation org = UserSpecificCachedDataHelper.getOrganisation(role.getOrganisationId(), StOrganisationLookupVerbosity.MAIN_ROLES);
                    for (CustomerRole roleInOrg : org.getCustomerRoles()) {
                        salesAgentsForICPOrganisationList.put(roleInOrg.getCustomerId(), roleInOrg.getCustomerName());
                    }
                }

            } else {
                custId = loggedInCustomer.getCustomerId();
                setSearchAnyICPSalesAgentIssues(false);
            }

            //used to identify a ticket created by this user
            iq.setCustomerId(String.valueOf(custId));
            iq.setIncidentChannel(BaseUtils.getProperty("env.jira.icp.cc.channel", "ICP Sales Team"));
            setCustomer(loggedInCustomer);

            String orgId = "";
            for (CustomerRole role : loggedInCustomer.getCustomerRoles()) {//TODO if the customer belongs to more than one org, then what? hmmm take the first one
                Organisation org = UserSpecificCachedDataHelper.getOrganisation(role.getOrganisationId(), StOrganisationLookupVerbosity.MAIN_ROLES);
                orgId = String.valueOf(org.getOrganisationId());
                break;
            }
            //In this instance (CustomerName) is just used to carry the organisationID for this ICP.
            iq.setCustomerName(orgId);

            String slUsername = getUser();
            setTTUser(new TTUser());
            getTTUser().setUserID(slUsername);

            if (isSearchAnyICPSalesAgentIssues() && isIcpSalesAgentIssueViewed()) {
                TTIssueList ttIssueList = (TTIssueList) SCAWrapper.getAdminInstance().getTroubleTicketIssues(iq);
                setTTIssueList(ttIssueList);
            } else {
                TTIssueList ttIssueList = (TTIssueList) SCAWrapper.getUserSpecificInstance().getTroubleTicketIssues(iq);
                setTTIssueList(ttIssueList);
            }



        } catch (Exception ex) {
            
            if (getIcpSalesAgentId() > 0) {
                Customer customer = UserSpecificCachedDataHelper.getCustomer(getIcpSalesAgentId(), StCustomerLookupVerbosity.CUSTOMER);
                setPageMessage("tt.icp.tickets.notfound.error", customer.getSSOIdentity());
            } else {
                setPageMessage("tt.icp.tickets.notfound.error", getTTUser().getUserID());
            }

            return getDDForwardResolution("/trouble_ticket/add_icp_ticket.jsp");
        }

        if (getTTIssueList().getTTIssueCount() <= 0) {
            if (getIcpSalesAgentId() > 0) {
                Customer customer = UserSpecificCachedDataHelper.getCustomer(getIcpSalesAgentId(), StCustomerLookupVerbosity.CUSTOMER);
                setPageMessage("tt.icp.tickets.notfound", customer.getSSOIdentity());
            } else {
                setPageMessage("tt.icp.tickets.notfound", getTTUser().getUserID());
            }
            return getDDForwardResolution("/trouble_ticket/add_icp_ticket.jsp");
        }

        return getDDForwardResolution("/trouble_ticket/search_icp_trouble_tickets.jsp");
    }

    private void prepareCreationOfTTWizardIssue() {

        TTMetaDataQuery metaDataQuery = new TTMetaDataQuery();
        metaDataQuery.setDummyField("");
        TTMetaData metaData = (TTMetaData) SCAWrapper.getUserSpecificInstance().getTroubleTicketMetaData(metaDataQuery);
        setTTMetaData(metaData);
        populateMaps(getTTMetaData());
        NewTTIssue newIssue = new NewTTIssue();
        newIssue.setMindMapFields(new MindMapFields());

        log.debug("Currently logged in user is [{}] and user ID is [{}]", getUser(), getUserCustomerIdFromSession());
        newIssue.setCustomerId(Integer.toString(getCustomer().getCustomerId()));

        Map<String, String[]> allTTMapParams = getRequest().getParameterMap();
        Set<String> allParamKeys = allTTMapParams.keySet();

        for (String paramKey : allParamKeys) {
            if (!(paramKey.startsWith("TT_USER_FIELD")) && !(paramKey.startsWith("TT_FIXED_FIELD"))) {
                //ignore parameters we don't know about
                continue;
            }
            JiraField jfn = new JiraField();
            String[] theValuesOfParamKey = allTTMapParams.get(paramKey);
            jfn.setFieldName(paramKey);

            StringBuilder jfnSb = new StringBuilder();

            for (String val : theValuesOfParamKey) {
                val = val.replaceAll("[^\\p{ASCII}]", "");
                jfnSb.append(val);
            }

            jfn.setFieldValue(jfnSb.toString());

            if (paramKey.startsWith("TT_USER_FIELD")) {
                jfn.setFieldType("TT_USER_FIELD");
            } else if (paramKey.startsWith("TT_FIXED_FIELD")) {
                jfn.setFieldType("TT_FIXED_FIELD");
            } else {
                jfn.setFieldType("TT_FIXED_FIELD");
            }
            newIssue.getMindMapFields().getJiraField().add(jfn);
        }

        //now get our butterfly params
        for (Entry<String, String> entry : fastForwardTTFixedFieldList.entrySet()) {
            JiraField jfn = new JiraField();
            jfn.setFieldName(entry.getKey());
            jfn.setFieldValue(entry.getValue());
            jfn.setFieldType("TT_FIXED_FIELD"); //for now we will assume all butterfly fields are fixed fields
            newIssue.getMindMapFields().getJiraField().add(jfn);
        }

        inflateNewIssueWithPreDefinedTTFields(newIssue);

        setNewTTIssue(newIssue);
    }

    private void inflateNewIssueWithPreDefinedTTFields(NewTTIssue newIssue) {
        //to get the user who reported the issue
        JiraField jiraF = new JiraField();
        Customer reporter = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER);
        jiraF.setFieldName("SEP Reporter");
        jiraF.setFieldType("TT_FIXED_FIELD");
        jiraF.setFieldValue(reporter.getFirstName() + " " + reporter.getLastName());
        newIssue.getMindMapFields().getJiraField().add(jiraF);

        //customer and services related info
        if (getCustomer().getCustomerId() != 0) {

            Customer cust = UserSpecificCachedDataHelper.getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER);

            jiraF = new JiraField();
            jiraF.setFieldName("Smile Customer Email");
            jiraF.setFieldType("TT_FIXED_FIELD");
            jiraF.setFieldValue(cust.getEmailAddress());
            newIssue.getMindMapFields().getJiraField().add(jiraF);

            jiraF = new JiraField();
            jiraF.setFieldName("Smile Customer Name");
            jiraF.setFieldType("TT_FIXED_FIELD");
            jiraF.setFieldValue(cust.getFirstName() + " " + cust.getLastName());
            newIssue.getMindMapFields().getJiraField().add(jiraF);

            jiraF = new JiraField();
            jiraF.setFieldName("Smile Customer Phone");
            jiraF.setFieldType("TT_FIXED_FIELD");
            jiraF.setFieldValue(cust.getAlternativeContact1());
            newIssue.getMindMapFields().getJiraField().add(jiraF);


            String iccidAndimsi = getIMSIandICCID(cust.getCustomerId());
            if (!iccidAndimsi.isEmpty()) {
                String[] iccidAndimsiArray = iccidAndimsi.split(":");

                if (!iccidAndimsiArray[0].equals("iccid")) {
                    JiraField jfnICCID = new JiraField();
                    jfnICCID.setFieldName("ICCID");
                    jfnICCID.setFieldType("TT_FIXED_FIELD");
                    //trim iccid, max jira customfieldvalue STRINGVALUE column is 255
                    if (iccidAndimsiArray[0].length() > 254) {
                        String veryLongIccId = iccidAndimsiArray[0].substring(0, 250);
                        jfnICCID.setFieldValue(veryLongIccId);
                    } else {
                        jfnICCID.setFieldValue(iccidAndimsiArray[0]);
                    }
                    newIssue.getMindMapFields().getJiraField().add(jfnICCID);
                }
                if (!iccidAndimsiArray[1].equals("imsi")) {
                    JiraField jfnIMSI = new JiraField();
                    jfnIMSI.setFieldName("SIM CARD IMSI");
                    jfnIMSI.setFieldType("TT_FIXED_FIELD");
                    //trim imsi, max jira customfieldvalue STRINGVALUE column is 255
                    if (iccidAndimsiArray[1].length() > 254) {
                        String veryLongImsi = iccidAndimsiArray[1].substring(0, 250);
                        jfnIMSI.setFieldValue(veryLongImsi);
                    } else {
                        jfnIMSI.setFieldValue(iccidAndimsiArray[1]);
                    }
                    newIssue.getMindMapFields().getJiraField().add(jfnIMSI);
                }
            }
        }
    }

    private Customer getCustomerWithProductsAndServices(int custId) {
        Customer cust = SCAWrapper.getUserSpecificInstance().getCustomer(custId, StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
        return cust;
    }

    private String getIMSIandICCID(int custId) {
        String imsiICCID;
        //initialise to avoid ArrayIndexOutOfBoundsExceptions
        String imsi = "imsi,";
        String iccid = "iccid,";
        Set<String> imsiSet = new HashSet<>();
        Set<String> iccidSet = new HashSet<>();
        StringBuilder imsiSB = new StringBuilder();
        StringBuilder iccidSB = new StringBuilder();

        Customer cust = getCustomerWithProductsAndServices(custId);
        accountIdSet = new HashSet<>();

        for (ProductInstance pi : cust.getProductInstances()) {
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = m.getServiceInstance();
                if (si.getCustomerId() == cust.getCustomerId()) {
                    accountIdSet.add(si.getAccountId());
                }
            }
        }
        if (accountIdSet.size() > 0) {

            for (long accId : accountIdSet) {
                setAccount(SCAWrapper.getUserSpecificInstance().getAccount(accId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));
                for (ServiceInstance si : getAccount().getServiceInstances()) {
                    si = SCAWrapper.getUserSpecificInstance().getServiceInstance(si.getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
                    for (AVP avp : si.getAVPs()) {
                        if (avp.getAttribute().equals("IntegratedCircuitCardIdentifier")) {
                            iccidSet.add(avp.getValue());
                        }
                        if (avp.getAttribute().equals("PrivateIdentity")) {
                            imsiSet.add(Utils.getIMSIfromPrivateIdentity(avp.getValue()));
                        }
                    }
                }
            }
            for (String icc : iccidSet) {
                iccidSB.append(icc).append(", ");
            }
            for (String ims : imsiSet) {
                imsiSB.append(ims).append(", ");
            }
            if (!iccidSB.toString().isEmpty()) {
                iccid = iccidSB.toString();
            }
            if (!imsiSB.toString().isEmpty()) {
                imsi = imsiSB.toString();
            }
            imsiICCID = iccid.substring(0, iccid.lastIndexOf(",")) + ":" + imsi.substring(0, imsi.lastIndexOf(","));
            return imsiICCID;

        } else {
            imsiICCID = iccid.substring(0, iccid.lastIndexOf(",")) + ":" + imsi.substring(0, imsi.lastIndexOf(","));
            return imsiICCID;
        }
    }

    private void prepareCreationOfIssueForICP() {

        NewTTIssue newIssue = new NewTTIssue();
        newIssue.setMindMapFields(new MindMapFields());

        newIssue.setCustomerId(String.valueOf(getUserCustomerIdFromSession()));

        Map<String, String[]> allTTMapParams = getRequest().getParameterMap();
        Set<String> allParamKeys = allTTMapParams.keySet();

        for (String paramKey : allParamKeys) {
            if (!(paramKey.startsWith("TT_USER_FIELD")) && !(paramKey.startsWith("TT_FIXED_FIELD"))) {
                //ignore parameters we don't know about
                continue;
            }
            JiraField jfn = new JiraField();
            String[] theValuesOfParamKey = allTTMapParams.get(paramKey);
            jfn.setFieldName(paramKey);

            StringBuilder jfnSb = new StringBuilder();

            for (String val : theValuesOfParamKey) {
                val = val.replaceAll("[^\\p{ASCII}]", "");
                jfnSb.append(val);
            }

            jfn.setFieldValue(jfnSb.toString());

            if (paramKey.startsWith("TT_USER_FIELD")) {
                jfn.setFieldType("TT_USER_FIELD");
            } else if (paramKey.startsWith("TT_FIXED_FIELD")) {
                jfn.setFieldType("TT_FIXED_FIELD");
            } else {
                jfn.setFieldType("TT_FIXED_FIELD");
            }
            newIssue.getMindMapFields().getJiraField().add(jfn);
        }

        inflateNewICPIssueWithPreDefinedTTFields(newIssue);

        setNewTTIssue(newIssue);
    }

    private void inflateNewICPIssueWithPreDefinedTTFields(NewTTIssue issue) {

        JiraField jfn;

        jfn = new JiraField();
        jfn.setFieldName("TT_FIXED_FIELD_Project");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(BaseUtils.getProperty("env.jira.customer.care.project.key"));
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        jfn.setFieldName("TT_FIXED_FIELD_Incident Channel");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(BaseUtils.getProperty("env.jira.icp.cc.channel", "ICP Sales Team"));
        issue.getMindMapFields().getJiraField().add(jfn);

        //used to identify a lead created by this user
        jfn = new JiraField();
        jfn.setFieldName("TT_FIXED_FIELD_ICPSales Person ID");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(String.valueOf(getUserCustomerIdFromSession()));
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        jfn.setFieldName("Smile Customer Phone");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(getCustomer().getAlternativeContact1());
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        jfn.setFieldName("Smile Customer Name");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(getCustomer().getFirstName() + " " + getCustomer().getLastName());
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        jfn.setFieldName("Smile Customer Email");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(getCustomer().getEmailAddress());
        issue.getMindMapFields().getJiraField().add(jfn);

        String orgId = "";
        for (CustomerRole role : getCustomer().getCustomerRoles()) {//TODO if the customer belongs to more than one org, then what?
            Organisation org = UserSpecificCachedDataHelper.getOrganisation(role.getOrganisationId(), StOrganisationLookupVerbosity.MAIN_ROLES);
            orgId = String.valueOf(org.getOrganisationId());
            break;
        }
        if (!orgId.isEmpty()) {
            jfn = new JiraField();
            jfn.setFieldName("TT_FIXED_FIELD_ICPOrganisation ID");
            jfn.setFieldType("TT_FIXED_FIELD");
            jfn.setFieldValue(orgId);
            issue.getMindMapFields().getJiraField().add(jfn);
        }

        int accountMananagerId = getCustomer().getAccountManagerCustomerProfileId();
        if (accountMananagerId > 0) {
            Customer accountManager = UserSpecificCachedDataHelper.getCustomer(accountMananagerId, StCustomerLookupVerbosity.CUSTOMER);
            TTJiraUserQuery tj = new TTJiraUserQuery();
            tj.setEmailAddress(accountManager.getEmailAddress());
            tj.setResultLimit(1);
            TTJiraUserList jiraUsers = SCAWrapper.getUserSpecificInstance().getJiraUsers(tj);
            String username = "";
            for (TTUser usr : jiraUsers.getJiraUsers()) {
                username = usr.getUserID();
            }
            if (!username.isEmpty()) {
                log.debug("Username to use for creating a watcher is: {}", username);
                jfn = new JiraField();
                jfn.setFieldName("TT_FIXED_FIELD_Watchers");
                jfn.setFieldType("TT_FIXED_FIELD");
                jfn.setFieldValue(username);
                issue.getMindMapFields().getJiraField().add(jfn);
            } else {
                log.debug("Could not find account manager for ICP in JIRA, searched was based on email address {}. No watcher is gonna be added", accountManager.getEmailAddress());
            }

        }

        log.debug("Exiting inflateNewICPIssueWithPreDefinedTTFields");
    }
}
