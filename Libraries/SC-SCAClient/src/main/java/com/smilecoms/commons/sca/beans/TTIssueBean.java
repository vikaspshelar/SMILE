/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.JiraField;
import com.smilecoms.commons.sca.MindMapFields;
import com.smilecoms.commons.sca.NewTTIssue;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.TTIssue;
import com.smilecoms.commons.sca.TTIssueList;
import com.smilecoms.commons.sca.TTIssueQuery;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.datatype.XMLGregorianCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class TTIssueBean extends BaseBean {

    private static final Logger log = LoggerFactory.getLogger(TTIssueBean.class);

    private TTIssue scaIssue;

    public TTIssueBean() {
    }

    public static TTIssueBean getIssueById(String issueId) {
        log.debug("Getting Issue by Id [{}]", issueId);
        TTIssueBean issueBean = null;
        TTIssueQuery query = new TTIssueQuery();
        query.setIssueID(issueId);
        query.setCustomerId("0");
        query.setCreatedDateFrom(Utils.getDateAsXMLGregorianCalendar(new java.util.Date()));
        query.setCreatedDateTo(Utils.getDateAsXMLGregorianCalendar(new java.util.Date()));
        query.setResultLimit(1);

        TTIssueList list = SCAWrapper.getUserSpecificInstance().getTroubleTicketIssues(query);
        for (TTIssue issue : list.getTTIssueList()) {
            issueBean = new TTIssueBean(issue);
        }
        return issueBean;
    }

    public static List<TTIssueBean> getIssues(TTIssueQuery query) {
        log.debug("Getting issues by calling getIssues()");
        return wrap(SCAWrapper.getUserSpecificInstance().getTroubleTicketIssues(query));
    }

    public static TTIssueBean createTroubleTicket(int customerId, String description, String category) {
        NewTTIssue issue = new NewTTIssue();

        issue.setCustomerId(String.valueOf(customerId));
        issue.setMindMapFields(new MindMapFields());
        Customer cust = SCAWrapper.getUserSpecificInstance().getCustomer(customerId, StCustomerLookupVerbosity.CUSTOMER);
        inflateNewIssueWithPreDefinedTTFields(issue,
                customerId,
                cust.getFirstName(),
                cust.getLastName(),
                description,
                cust.getAlternativeContact1(),
                cust.getEmailAddress(), category);

        return new TTIssueBean(SCAWrapper.getUserSpecificInstance().createTroubleTicketIssue(issue));
    }

    private TTIssueBean(com.smilecoms.commons.sca.TTIssue scaIssue) {
        this.scaIssue = scaIssue;
    }

    public static TTIssueBean wrap(TTIssue issue) {
        return new TTIssueBean(issue);
    }

    public static List<TTIssueBean> wrap(TTIssueList issueList) {
        List<TTIssueBean> issueBeanList = new ArrayList<>();
        for (TTIssue issue : issueList.getTTIssueList()) {
            issueBeanList.add(wrap(issue));
        }
        return issueBeanList;
    }

    @XmlElement
    public String getID() {
        return scaIssue.getID();
    }

    @XmlElement
    public String getProject() {
        return scaIssue.getProject();
    }

    @XmlElement
    public String getSummary() {
        return scaIssue.getSummary();
    }

    @XmlElement
    public String getIssueType() {
        return scaIssue.getIssueType();
    }

    @XmlElement
    public String getDescription() {
        return scaIssue.getDescription();
    }

    @XmlElement
    public String getReporter() {
        return scaIssue.getReporter();
    }

    @XmlElement
    public String getAssignee() {
        return scaIssue.getAssignee();
    }

    @XmlElement
    public String getPriority() {
        return scaIssue.getPriority();
    }

    @XmlElement
    public long getCreated() {
        return Utils.getJavaDate(scaIssue.getCreated()).getTime();
    }

    @XmlElement
    public long getDueDate() {
        return scaIssue.getDueDate() == null ? 0 : Utils.getJavaDate(scaIssue.getDueDate()).getTime();
    }

    @XmlElement
    public long getUpdated() {
        return Utils.getJavaDate(scaIssue.getUpdated()).getTime();
    }

    @XmlElement
    public String getResolution() {
        return scaIssue.getResolution();
    }

    @XmlElement
    public String getStatus() {
        return scaIssue.getStatus();
    }

    @XmlElement
    public List<String> getWatchers() {
        return scaIssue.getWatchers();
    }

    @XmlElement
    public MindMapFields getMindMapFields() {
        return scaIssue.getMindMapFields();
    }

    private static void inflateNewIssueWithPreDefinedTTFields(
            NewTTIssue issue,
            int customerId,
            String firstName,
            String lastName,
            String description,
            String alternativeContact1,
            String emailAddress,
            String category) {

        JiraField jfn = new JiraField();

        jfn.setFieldName("TT_FIXED_FIELD_Description");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(description);
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

        String ttSubject = "Customer Query: " + firstName + " " + lastName + " (" + customerId + ")";
        jfn = new JiraField();
        jfn.setFieldName("TT_FIXED_FIELD_Summary");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(ttSubject);
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        jfn.setFieldName("TT_FIXED_FIELD_Category");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(category);
        issue.getMindMapFields().getJiraField().add(jfn);

        String iccidAndimsi = getIMSIandICCID(customerId);

        if (!iccidAndimsi.isEmpty()) {
            String[] iccidAndimsiArray = iccidAndimsi.split(":");

            if (!iccidAndimsiArray[0].equals("iccid")) {
                jfn = new JiraField();
                jfn.setFieldName("ICCID");
                jfn.setFieldType("TT_FIXED_FIELD");                
                jfn.setFieldValue(iccidAndimsiArray[0]);                
                if(iccidAndimsiArray[0].length() > 250){
                    jfn.setFieldValue(iccidAndimsiArray[0].substring(0, 250));
                }
                
                issue.getMindMapFields().getJiraField().add(jfn);
            }
            if (!iccidAndimsiArray[1].equals("imsi")) {
                jfn = new JiraField();
                jfn.setFieldName("SIM CARD IMSI");
                jfn.setFieldType("TT_FIXED_FIELD");
                jfn.setFieldValue(iccidAndimsiArray[1]);
                issue.getMindMapFields().getJiraField().add(jfn);
            }
        }

        jfn = new JiraField();
        jfn.setFieldName("Smile Customer Phone");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(alternativeContact1);
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        jfn.setFieldName("Smile Customer Name");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(firstName + " " + lastName);
        issue.getMindMapFields().getJiraField().add(jfn);

        jfn = new JiraField();
        jfn.setFieldName("Smile Customer Email");
        jfn.setFieldType("TT_FIXED_FIELD");
        jfn.setFieldValue(emailAddress);
        issue.getMindMapFields().getJiraField().add(jfn);
    }
    private static Set<Long> accountIdSet;

    private static String getIMSIandICCID(int custId) {
        String imsiICCID;
        //initialise to avoid ArrayIndexOutOfBoundsExceptions
        String imsi = "imsi,";
        String iccid = "iccid,";
        Set<String> imsiSet = new HashSet<>();
        Set<String> iccidSet = new HashSet<>();
        StringBuilder imsiSB = new StringBuilder();
        StringBuilder iccidSB = new StringBuilder();

        Customer cust = SCAWrapper.getUserSpecificInstance().getCustomer(custId, StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
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
                Account acc = SCAWrapper.getUserSpecificInstance().getAccount(accId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
                for (ServiceInstance si : acc.getServiceInstances()) {
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
}
