/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.JiraField;
import com.smilecoms.commons.sca.MindMapFields;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.TTComment;
import com.smilecoms.commons.sca.TTCommentList;
import com.smilecoms.commons.sca.TTCommentQuery;
import com.smilecoms.commons.sca.TTIssue;
import com.smilecoms.commons.sca.TTIssueList;
import com.smilecoms.commons.sca.TTIssueQuery;
import com.smilecoms.commons.sca.TTIssueType;
import com.smilecoms.commons.sca.TTMetaData;
import com.smilecoms.commons.sca.TTMetaDataQuery;
import com.smilecoms.commons.sca.TTPriority;
import com.smilecoms.commons.sca.TTProject;
import com.smilecoms.commons.sca.TTResolution;
import com.smilecoms.commons.sca.TTStatus;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.util.IMAPUtils;
import com.smilecoms.commons.util.Utils;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;

/**
 *
 * @author sabelo
 */
public class SCPTroubleTicketActionBean extends SmileActionBean {

    private HashMap projectMap = new HashMap();
    private HashMap issueTypeMap = new HashMap();
    private HashMap priorityMap = new HashMap();
    private HashMap resolutionMap = new HashMap();
    private HashMap statusMap = new HashMap();
    private boolean watchIssue;
    private String projectName;
    private boolean ttStatus = false;
    private String scpTTDescription;
    private Set<Long> accountIdSet;
    private boolean termsAndConditionsRead;

    public boolean getTermsAndConditionsRead() {
        return termsAndConditionsRead;
    }

    public void setTermsAndConditionsRead(boolean termsAndConditionsRead) {
        this.termsAndConditionsRead = termsAndConditionsRead;
    }

    private String accountManagerName;

    public String getAccountManagerName() {
        return accountManagerName;
    }

    @DefaultHandler
    public Resolution retrieveTroubleTickets() {
        return gotoTroubleTicketHome();
    }

    public Resolution addTroubleTicket() {

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
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));

        return getDDForwardResolution("/trouble_ticket/add_trouble_ticket.jsp");
    }

    public Resolution insertTroubleTicket() {

        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
        NewTTIssue issue = new NewTTIssue();
        log.debug("Currently logged in user is [{}] and user ID is [{}]", getUser(), getUserCustomerIdFromSession());
        issue.setMindMapFields(new MindMapFields());
        issue.setCustomerId(Integer.toString(getUserCustomerIdFromSession()));

        //Adds additional fields
        inflateNewIssueWithPreDefinedTTFields(issue);

        TTIssue returnIssue;
        try {
            returnIssue = (TTIssue) SCAWrapper.getUserSpecificInstance().createTroubleTicketIssue(issue);
        } catch (SCABusinessError e) {
            setPageMessage("trouble.ticket.add.failed");
            return addTroubleTicket();
            //return displayError(e, addTroubleTicket());
        }

        setPageMessage("scp.trouble.ticket.added.successfully", returnIssue.getID());

        ttStatus = true;
        return addTroubleTicket();
    }

    public Resolution retrieveTroubleTicketComments() {
        TTIssue issue = getTTIssue();
        String issueID = issue.getID();

        TTCommentQuery commentQuery = new TTCommentQuery();
        commentQuery.setIssueKey(issueID);

        TTCommentList comments = (TTCommentList) SCAWrapper.getUserSpecificInstance().getTroubleTicketComments(commentQuery);

        setTTCommentList(comments);

        return getDDForwardResolution("/trouble_ticket/view_trouble_ticket_comments.jsp");
    }

    public Resolution addTroubleTicketComment() {
        return getDDForwardResolution("/trouble_ticket/add_trouble_ticket_comment.jsp");
    }

    public Resolution insertTroubleTicketComment() {

        TTIssue issue = getTTIssue();
        String issueID = issue.getID();
        TTComment comment = getTTComment();
        comment.setAuthor(getCustomer().getSSOIdentity());
        comment.setIssueKey(issueID);
        SCAWrapper.getUserSpecificInstance().createTroubleTicketComment(comment);
        setPageMessage("trouble.ticket.comment.added.successfully");

        return editTroubleTicket();
    }

    public Resolution queryTroubleTickets() {

        TTIssueQuery issueQuery = getTTIssueQuery();

        issueQuery.setIssueID("");
        issueQuery.setIncidentChannel(BaseUtils.getProperty("env.jira.customer.portal.incident.channel"));
        issueQuery.setSalesLeadAssignee("");
        issueQuery.setSalesLeadPK("");
        issueQuery.setResultLimit(10);
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
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
        if (newIssueList.size() <= 0) {
            setPageMessage("scp.no.tickets.found");
        }
        return getDDForwardResolution("/trouble_ticket/view_trouble_tickets.jsp");
    }

    public Resolution editTroubleTicket() {

        TTIssue issue = getTTIssue();
        if (issue == null) {
            return gotoTroubleTicketHome();
        }
        String issueID = issue.getID();

        /* First retrieve metadata */
        TTMetaDataQuery metaDataQuery = new TTMetaDataQuery();
        metaDataQuery.setDummyField("");
        TTMetaData metaData = (TTMetaData) SCAWrapper.getUserSpecificInstance().getTroubleTicketMetaData(metaDataQuery);
        setTTMetaData(metaData);

        TTIssueQuery issueQuery = new TTIssueQuery();
        //issueQuery.setReporter(Integer.toString(getUserCustomerIdFromSession()));
        issueQuery.setCreatedDateFrom(Utils.getDateAsXMLGregorianCalendar(new java.util.Date()));
        issueQuery.setCreatedDateTo(Utils.getDateAsXMLGregorianCalendar(new java.util.Date()));
        issueQuery.setIssueID(issueID);
        issueQuery.setResultLimit(1);
        issueQuery.setSalesLeadAssignee("");
        issueQuery.setSalesLeadPK("");
        issueQuery.setCustomerId(Integer.toString(getUserCustomerIdFromSession()));

        TTIssueList ttIssueList = (TTIssueList) SCAWrapper.getUserSpecificInstance().getTroubleTicketIssues(issueQuery);

        List issueList = ttIssueList.getTTIssueList();
        int issueListSize = issueList.size();

        for (int i = 0; i < issueListSize; i++) {
            issue = (TTIssue) issueList.get(i);
            if (issueID.equals(issue.getID())) {
                break;
            }
        }
        /* Populate Maps for later translation of information from TT */
        populateMaps(metaData);

        issue = convertIssue(issue, false, false, false, false, true);
        setTTIssue(issue);

        /* Project Name */
        List projectList = metaData.getTTProjectList();
        int projectListSize = projectList.size();
        for (int i = 0; i < projectListSize; i++) {
            TTProject project = (TTProject) projectList.get(i);
            String projectID = project.getID();
            if (projectID.equals(issue.getProject())) {
                projectName = project.getName();
                break;
            }
        }
        watchIssue = false;
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
        return getDDForwardResolution("/trouble_ticket/view_ticket_information.jsp");
    }

    public Resolution updateTroubleTicket() {
        TTIssue modifiedIssue = getTTIssue();

        String username = getCustomer().getSSOIdentity();
        modifiedIssue.setReporter(username);
        modifiedIssue.setUpdated(Utils.getDateAsXMLGregorianCalendar(new java.util.Date()));

        /*if (watchIssue == true) {
         modifiedIssue.getWatchers().add(getUserAsSmileID());
         }*/ //TODO: fix
        TTIssue issue = (TTIssue) SCAWrapper.getUserSpecificInstance().modifyTroubleTicketIssue(modifiedIssue);
        setPageMessage("trouble.ticket.updated.successfully", issue.getID());

        return gotoTroubleTicketHome();
    }

    public Resolution retrieveComments() {
        String result = "No message specified to retrieve...";
        return new StreamingResolution("text", new StringReader(result));
    }

    public Resolution showSalesLeadReferalPage() {
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
        return getDDForwardResolution("/trouble_ticket/add_saleslead_referal_ticket.jsp");
    }

    public Resolution addReferredSalesLead() {

        NewTTIssue newIssue = new NewTTIssue();
        newIssue.setMindMapFields(new MindMapFields());

        Map<String, String[]> allTTMapParams = getRequest().getParameterMap();
        Set<String> allParamKeys = allTTMapParams.keySet();

        for (String paramKey : allParamKeys) {
            if (!(paramKey.startsWith("TT_USER_FIELD")) && !(paramKey.startsWith("TT_FIXED_FIELD"))) {
                //ignore parameters we don't know about
                continue;
            }
            JiraField jfn = new JiraField();
            String[] valuesOfParamKey = allTTMapParams.get(paramKey);
            jfn.setFieldName(paramKey);

            StringBuilder jfnSb = new StringBuilder();

            for (String val : valuesOfParamKey) {
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
        newIssue.setCustomerId("0");
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
        inflateNewSDIssueWithPreDefinedTTFields(newIssue);

        TTIssue returnIssue;
        try {
            returnIssue = (TTIssue) SCAWrapper.getUserSpecificInstance().createTroubleTicketIssue(newIssue);
        } catch (SCABusinessError e) {
            setPageMessage("trouble.ticket.add.failed");
            termsAndConditionsRead = true;
            return showSalesLeadReferalPage();
        }

        try {

            String issueId = returnIssue.getID();
            String referrerName = getCustomer().getFirstName() + " " + getCustomer().getLastName();
            String referrerPhone = getCustomer().getAlternativeContact1();
            String referrerEmail = getCustomer().getEmailAddress();
            String srcAddress = BaseUtils.getProperty("env.refer.afriend.sender.email.address", BaseUtils.getProperty("env.customercare.email.address"));
            
            String referredName = getParameter("TT_FIXED_FIELD_Customer Name");
            String referredPhone = getParameter("TT_FIXED_FIELD_Smile Customer Phone");
            String referredEmail = getParameter("TT_FIXED_FIELD_Customer Email");

            String referrerEmailSubject = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "refer.afriend.referrer.email.subject");
            referrerEmailSubject = Utils.format(referrerEmailSubject, new Object[]{issueId});
            String referrerEmailBody = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "refer.afriend.referrer.email.body");
            String[] referrerEntries = {referrerName, referredName, referredPhone, referredEmail, referredName};//5 entries
            sendEmail(referrerEmail, referrerEmailSubject, referrerEmailBody, referrerEntries, srcAddress);

            String referredEmailSubject = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "refer.afriend.referred.email.subject");
            referredEmailSubject = Utils.format(referredEmailSubject, new Object[]{issueId});
            String referredEmailBody = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "refer.afriend.referred.email.body");
            String[] referredEntries = {referredName, referrerName, referrerPhone};//3 entries
            sendEmail(referredEmail, referredEmailSubject, referredEmailBody, referredEntries, srcAddress);

            String salesOpEmailSubject = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "refer.afriend.salesoperation.email.subject");
            salesOpEmailSubject = Utils.format(salesOpEmailSubject, new Object[]{issueId});
            String salesOpEmailBody = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "refer.afriend.salesoperation.email.body");
            String[] salesOpEntries = {"Sales Operations", referredName, referredPhone, referredEmail, referrerName, referrerPhone, referrerEmail, issueId};//8entries
            String salesOperations = BaseUtils.getProperty("env.salesoperations.email.address", BaseUtils.getProperty("env.customercare.email.address"));
            
            sendEmail(salesOperations, salesOpEmailSubject, salesOpEmailBody, salesOpEntries, srcAddress);

        } catch (Exception ex) {
            log.warn("Failed to send email");
        }
        setPageMessage("scp.trouble.ticket.added.successfully", returnIssue.getID());
        termsAndConditionsRead = true;
        return showSalesLeadReferalPage();
    }

    private Resolution gotoTroubleTicketHome() {

        TTIssueQuery query = new TTIssueQuery();

        /* Compute dateFrom (7 days ago) and dateTo (current time + 1 day) */
        Calendar rightNow = Calendar.getInstance();
        rightNow.add(Calendar.DATE, +1);
        Date dateTo = rightNow.getTime();
        rightNow.add(Calendar.DATE, -8);
        Date dateFrom = rightNow.getTime();
        query.setIncidentChannel(BaseUtils.getProperty("env.jira.customer.portal.incident.channel"));
        query.setCreatedDateFrom(Utils.getDateAsXMLGregorianCalendar(dateFrom));
        query.setCreatedDateTo(Utils.getDateAsXMLGregorianCalendar(dateTo));
        query.setSalesLeadAssignee("");
        query.setSalesLeadPK("");

        Customer customer = getCustomer(); //TODO: WTF?
        if (customer == null) {
            customer = new Customer();
            //customer.setSSOIdentity(getUserAsSmileID());
            setCustomer(customer);
        }
        query.setReporter(customer.getSSOIdentity());
        setTTIssueQuery(query);

        TTMetaDataQuery metaDataQuery = new TTMetaDataQuery();
        metaDataQuery.setDummyField("");
        TTMetaData metaData = (TTMetaData) SCAWrapper.getUserSpecificInstance().getTroubleTicketMetaData(metaDataQuery);
        setTTMetaData(metaData);
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
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

    /**
     * @return the ttStatus
     */
    public boolean isTtStatus() {
        return ttStatus;
    }

    /**
     * @param ttStatus the ttStatus to set
     */
    public void setTtStatus(boolean ttStatus) {
        this.ttStatus = ttStatus;
    }

    /**
     * @return the scpTTDescription
     */
    public String getScpTTDescription() {
        return scpTTDescription;
    }

    /**
     * @param scpTTDescription the scpTTDescription to set
     */
    public void setScpTTDescription(String scpTTDescription) {
        this.scpTTDescription = scpTTDescription;
    }

    private void inflateNewIssueWithPreDefinedTTFields(NewTTIssue issue) {

        JiraField jfn = new JiraField();

        jfn.setFieldName("TT_FIXED_FIELD_Description");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(getScpTTDescription());
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        jfn.setFieldName("TT_FIXED_FIELD_Project");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(BaseUtils.getProperty("env.jira.customer.care.project.key"));
        issue.getMindMapFields().getJiraField().add(jfn);

        String issType = "Customer Request";
        jfn = new JiraField();
        jfn.setFieldName("TT_FIXED_FIELD_Issue Type");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(issType);
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        jfn.setFieldName("TT_FIXED_FIELD_Incident Channel");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(BaseUtils.getProperty("env.jira.customer.portal.incident.channel"));
        issue.getMindMapFields().getJiraField().add(jfn);

        String ttSubject = "Customer Query: " + getCustomer().getFirstName() + " " + getCustomer().getLastName() + " (" + getCustomer().getCustomerId() + ")";
        jfn = new JiraField();
        jfn.setFieldName("TT_FIXED_FIELD_Summary");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(ttSubject);
        issue.getMindMapFields().getJiraField().add(jfn);

        String category = getParameter("TT_FIXED_FIELD_Category");
        jfn = new JiraField();
        jfn.setFieldName("TT_FIXED_FIELD_Category");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(category);
        issue.getMindMapFields().getJiraField().add(jfn);

        String iccidAndimsi = getIMSIandICCID(getCustomer().getCustomerId());

        if (!iccidAndimsi.isEmpty()) {
            String[] iccidAndimsiArray = iccidAndimsi.split(":");

            if (!iccidAndimsiArray[0].equals("iccid")) {
                jfn = new JiraField();
                jfn.setFieldName("ICCID");
                jfn.setFieldType("TT_FIXED_FIELD");
                //trim iccid, max jira customfieldvalue STRINGVALUE column is 255
                if (iccidAndimsiArray[0].length() > 254) {
                    String veryLongIccId = iccidAndimsiArray[0].substring(0, 250);
                    jfn.setFieldValue(veryLongIccId);
                } else {
                    jfn.setFieldValue(iccidAndimsiArray[0]);
                }

                issue.getMindMapFields().getJiraField().add(jfn);
            }
            if (!iccidAndimsiArray[1].equals("imsi")) {
                jfn = new JiraField();
                jfn.setFieldName("SIM CARD IMSI");
                jfn.setFieldType("TT_FIXED_FIELD");
                //trim imsi, max jira customfieldvalue STRINGVALUE column is 255
                if (iccidAndimsiArray[1].length() > 254) {
                    String veryLongImsi = iccidAndimsiArray[1].substring(0, 250);
                    jfn.setFieldValue(veryLongImsi);
                } else {
                    jfn.setFieldValue(iccidAndimsiArray[1]);
                }
                issue.getMindMapFields().getJiraField().add(jfn);
            }
        }

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
                    si = UserSpecificCachedDataHelper.getServiceInstance(si.getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN);
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

    private Customer getCustomerWithProductsAndServices(int custId) {
        Customer cust = SCAWrapper.getUserSpecificInstance().getCustomer(custId, StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
        return cust;
    }

    private void inflateNewSDIssueWithPreDefinedTTFields(NewTTIssue issue) {

        JiraField jfn = new JiraField();
        String salesLeadPK = BaseUtils.getProperty("env.jira.saleslead.project.key");
        jfn = new JiraField();
        jfn.setFieldName("TT_FIXED_FIELD_Project");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(salesLeadPK);
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        String assignee = BaseUtils.getProperty("env.scp.refer.a.friend.assignee", "");
        jfn.setFieldName("TT_FIXED_FIELD_Assignee");//
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(assignee);
        issue.getMindMapFields().getJiraField().add(jfn);

        String issType = "Sales Lead";
        jfn = new JiraField();
        jfn.setFieldName("TT_FIXED_FIELD_Issue Type");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(issType);
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        String leadChannel = "Referral";
        jfn.setFieldName("TT_FIXED_FIELD_Lead Channel");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(leadChannel);
        issue.getMindMapFields().getJiraField().add(jfn);

        String ttSubject = "Refer a friend: " + getCustomer().getFirstName() + " " + getCustomer().getLastName() + " (" + getCustomer().getCustomerId() + ")";
        jfn = new JiraField();
        jfn.setFieldName("TT_FIXED_FIELD_Summary");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(ttSubject);
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        String followUp = "Demo";//None
        jfn.setFieldName("TT_FIXED_FIELD_Required Follow Up");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(followUp);
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        Date futureDate = Utils.getFutureDate(Calendar.DATE, 7);
        String followUpDate = Utils.getDateAsString(futureDate, "dd/MM/yyyy", new Date().toString());
        jfn.setFieldName("TT_FIXED_FIELD_Follow up date");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(followUpDate);
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        String businessSector = "Mining";//None
        jfn.setFieldName("TT_FIXED_FIELD_Business Sector");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(businessSector);
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        String leadProbability = "Undecided";
        jfn.setFieldName("TT_FIXED_FIELD_Lead Probability");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(leadProbability);
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        String segment = "@me";
        jfn.setFieldName("TT_FIXED_FIELD_Customer Segment");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(segment);
        issue.getMindMapFields().getJiraField().add(jfn);

        log.debug("Exiting inflateNewSDIssueWithPreDefinedTTFields");
    }

    private void sendEmail(String emailAddress, String subject, String body, String[] entries, String srcAddress) {
        log.debug("Entering sendEmail");
        try {
            String bodyWithParamsReplaced = Utils.format(body, (Object[]) entries);
            log.debug("Email Body With Params replaced \n [{}]", bodyWithParamsReplaced);

            log.debug("Calling IMAPUtils to send email to address [{}]", emailAddress);
            IMAPUtils.sendEmail(srcAddress, emailAddress, subject, bodyWithParamsReplaced);
            log.debug("Called IMAPUtils to send email to address [{}]", emailAddress);
        } catch (Exception e) {
            log.warn("Error sending email", e);
        }
        log.debug("Exiting sendEmail");
    }

}
