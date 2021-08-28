/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tt;

import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.GetCreateIssueMetadataOptionsBuilder;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.BasicUser;
import com.atlassian.jira.rest.client.api.domain.BasicWatchers;
import com.atlassian.jira.rest.client.api.domain.CimFieldInfo;
import com.atlassian.jira.rest.client.api.domain.CimIssueType;
import com.atlassian.jira.rest.client.api.domain.CimProject;
import com.atlassian.jira.rest.client.api.domain.CustomFieldOption;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.jira.rest.client.internal.async.DisposableHttpClient;
import com.atlassian.jira.rest.client.internal.json.JsonParseUtil;
import com.google.common.collect.ImmutableSet;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.tt.Check;
import com.smilecoms.xml.schema.tt.CloseSalesLeadIssueQuery;
import com.smilecoms.xml.schema.tt.Comment;
import com.smilecoms.xml.schema.tt.CommentList;
import com.smilecoms.xml.schema.tt.CommentQuery;
import com.smilecoms.xml.schema.tt.Done;
import com.smilecoms.xml.schema.tt.Issue;
import com.smilecoms.xml.schema.tt.IssueList;
import com.smilecoms.xml.schema.tt.IssueQuery;
import com.smilecoms.xml.schema.tt.IssueType;
import com.smilecoms.xml.schema.tt.IssueTypeList;
import com.smilecoms.xml.schema.tt.IssueTypeQuery;
import com.smilecoms.xml.schema.tt.IssueWatcher;
import com.smilecoms.xml.schema.tt.IssueWatcherQuery;
import com.smilecoms.xml.schema.tt.JiraField;
import com.smilecoms.xml.schema.tt.JiraFields;
import com.smilecoms.xml.schema.tt.JiraUserList;
import com.smilecoms.xml.schema.tt.JiraUserQuery;
import com.smilecoms.xml.schema.tt.MetaData;
import com.smilecoms.xml.schema.tt.MetaDataQuery;
import com.smilecoms.xml.schema.tt.NewIssue;
import com.smilecoms.xml.schema.tt.Priority;
import com.smilecoms.xml.schema.tt.PriorityList;
import com.smilecoms.xml.schema.tt.PriorityQuery;
import com.smilecoms.xml.schema.tt.Project;
import com.smilecoms.xml.schema.tt.ProjectList;
import com.smilecoms.xml.schema.tt.ProjectQuery;
import com.smilecoms.xml.schema.tt.Resolution;
import com.smilecoms.xml.schema.tt.ResolutionList;
import com.smilecoms.xml.schema.tt.ResolutionQuery;
import com.smilecoms.xml.schema.tt.StCheck;
import com.smilecoms.xml.schema.tt.Status;
import com.smilecoms.xml.schema.tt.StatusList;
import com.smilecoms.xml.schema.tt.StatusQuery;
import com.smilecoms.xml.schema.tt.User;
import com.smilecoms.xml.schema.tt.UserID;
import com.smilecoms.xml.tt.TTError;
import com.smilecoms.xml.tt.TTSoap;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;

/**
 *
 * @author sabza
 */
@WebService(serviceName = "TT", portName = "TTSoap", endpointInterface = "com.smilecoms.xml.tt.TTSoap", targetNamespace = "http://xml.smilecoms.com/TT", wsdlLocation = "TTServiceDefinition.wsdl")
@Stateless
@Startup
@HandlerChain(file = "/handler.xml")
@Local({BaseListener.class})
public class TroubleTicketManager extends SmileWebService implements TTSoap, BaseListener {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;
    private static final String CONTEXT_AND_API_VERSION = "/rest/api/2";
    private static Client jerseyClient = null;
    private static final Map<String, String> jiraFieldsCache = new ConcurrentHashMap<>();
    private static final Map<String, IssueList> wildCardIssuesCache = new ConcurrentHashMap<>();
    private static final Map<String, MetaData> xmlMetadataCache = new ConcurrentHashMap<>();
    private static Iterable<CimProject> iterableCreateIssueMetadataOptions = new ArrayList<>();
    private static final Lock metadataLock = new java.util.concurrent.locks.ReentrantLock();
    private static final String BASIC = "Basic";
    /*These names are configured in JIRA, any modification that is not inline with those in JIRA might produce undesired effects*/
    //CustomerCare Fields as seen in JIRA
    private static final String CC_SMILE_CUSTOMER_EMAIL = "Smile Customer Email";
    private static final String CC_SMILE_CUSTOMER_ID = "Smile Customer ID";
    private static final String CC_SMILE_CUSTOMER_PHONE = "Smile Customer Phone";
    private static final String CC_SMILE_CUSTOMER_NAME = "Smile Customer Name";
    private static final String CC_SMILE_CUSTOMER_LOCATION = "Smile Customer Location";
    private static final String CC_CATEGORY = "Category";
    private static final String CC_INCIDENT_CHANNEL = "Incident Channel";
    private static final String CC_SUB_CATEGORY = "Sub Category";
    private static final String CC_RESOLVED_NOTIFY_CUSTOMER_TRANSITION = "Resolved - Notify Customer";
    private static final String CC_CUSTOMER_NOTIFIED_TRANSITION = "Customer Notified";
    private static final String CC_CLOSE_ISSUE_TRANSITION = "Close Issue";
    private static final String CC_ICPSALES_PERSON_ID = "ICPSales Person ID";
    private static final String CC_ICPORGANISATION_ID = "ICPOrganisation ID";
    //SalesLead Fields as seen in JIRA
    private static final String SL_CUSTOMER_FIRST_NAME = "Customer Name";
    private static final String SL_ALTERNATIVE_CONTACT1 = "Smile Customer Phone";
    private static final String SL_CUSTOMER_EMAIL = "Customer Email";
    private static final String SL_ADDRESS_LINE1 = "Add:Location";
    private static final String SL_ADDRESS_LINE2 = "Add:Street";
    private static final String SL_ADDRESS_TOWN = "Add:Town/City";
    private static final String SL_LEAD_STATE = "Lead state";
    private static final String SL_SUCCESSFUL_TRANSITION = "Successful ";
    private static int JIRA_VERSION;
    private static EntityManagerFactory emf;
    private static DisposableHttpClient httpClient = null;
    private static boolean doneStartUp = false;

    @Override
    public User createUser(User newUser) throws TTError {
        setContext(newUser, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        User user = null;
        JiraRestClient connection = null;
        try {
            connection = openRestConnection();
            if (log.isDebugEnabled()) {
                log.debug("Invoking remote JIRA service to create user...");
            }
            log.debug("Creating new JIRA user with uname [{}] and email [{}]", newUser.getUserID(), newUser.getEmail());
            newUser.setFullName(BaseUtils.getProperty("env.jira.security.username"));
            com.atlassian.jira.rest.client.api.domain.User jUser = connection.getUserClient().getUser(newUser.getFullName()).claim();
            if (log.isDebugEnabled()) {
                log.debug("Populating return object with response from JIRA...");
            }

            user = new User();
            user.setUserID(jUser.getName());
            user.setFullName(jUser.getDisplayName());
            user.setEmail(jUser.getEmailAddress());
            user.setPassword("");

        } catch (Exception e) {
            throw processError(TTError.class, e);
        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return user;
    }

    @Override
    public Issue createIssue(NewIssue newIssue) throws TTError {
        setContext(newIssue, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        JiraRestClient restClient;
        Issue returnIssue = null;

        JiraRestClient connection = null;
        try {
            connection = openRestConnection();

            if (!newIssue.getCustomerId().isEmpty() && !newIssue.getCustomerId().equals("0")) {
                JiraField jf = new JiraField();
                jf.setFieldName(CC_SMILE_CUSTOMER_ID);
                jf.setFieldType("TT_FIXED_FIELD");
                jf.setFieldValue(newIssue.getCustomerId());
                newIssue.getJiraFields().getJiraField().add(jf);
            }

            List<JiraField> fields = newIssue.getJiraFields().getJiraField();
            List<JiraField> qFields = fields;
            /*Repetition flag helps not to re-process TT_USER_FIELD field type with the same name during execution e.g. Description, more than once*/
            String fieldRepetitionFlag = "PROCESSED_TT_USER_FIELD_FLAG";
            String isTyp = "";
            String pKey = "";

            if (log.isDebugEnabled()) {
                log.debug("Populate JIRA issue...");
            }

            //Get values for only Project and Issue Type for now so we can retreive metadata of these two
            for (JiraField field : fields) {

                if (field.getFieldType().equals("TT_USER_FIELD")) {
                    continue;
                }

                String fieldName = field.getFieldName().replace("TT_FIXED_FIELD_", "");

                if (!fieldName.equalsIgnoreCase("Project") && !fieldName.equalsIgnoreCase("Issue Type")) {
                    continue;
                }

                if (fieldName.equalsIgnoreCase("Issue Type")) {
                    isTyp = field.getFieldValue();
                }

                if (fieldName.equalsIgnoreCase("Project")) {
                    pKey = field.getFieldValue();
                }
            }

            //Get projects metadata
            final Iterable<CimProject> metadataProjects = getIterableCreateIssueMetadataOptions();

            //select project
            CimProject project = null;
            StringBuilder possibleProjects = new StringBuilder();

            for (CimProject p : metadataProjects) {
                if (p.getKey().equals(pKey)) {
                    project = p;
                    break;
                }
                possibleProjects.append(p.getKey()).append(", ");
            }
            if (project == null) {
                String pp = possibleProjects.toString();
                String projects = pp.substring(0, pp.lastIndexOf(","));
                String errorString = "Could not find the project \"" + pKey + "\" passed from SCA to TT. TT projects must match JIRA configured projects\n"
                        + "Configured JIRA projects: [" + projects + "]";
                throw createError(TTError.class, errorString);
            }
            log.debug("Selected project: [{}] {}", project.getKey(), project.getName());

            // select issue type
            CimIssueType issueType = null;
            StringBuilder possibleIssueType = new StringBuilder();

            for (CimIssueType t : project.getIssueTypes()) {
                if (t.getName().equals(isTyp)) {
                    issueType = t;
                    break;
                }
                possibleIssueType.append(t.getName()).append(", ");
            }
            if (issueType == null) {
                String it = possibleIssueType.toString();
                String itypes = it.substring(0, it.lastIndexOf(","));
                String errorString = "Could not find the issue type \"" + isTyp + "\" passed from SCA to TT for project \"" + project.getKey() + "\". TT issue types must match JIRA configured issue types\n"
                        + "Configured JIRA issue types: [" + itypes + "] for project \"" + project.getKey() + "\"";
                throw createError(TTError.class, errorString);
            }
            log.debug("Selected issue type: [{}] {}", issueType.getId(), issueType.getName());

            //Initialise issue and start capturing required data
            final IssueInputBuilder issueBuilder = new IssueInputBuilder(project.getKey(), issueType.getId());
            boolean statusIsSet = false;
            boolean watchIssue = false;
            String watcherUsername = "";

            for (JiraField field : fields) {

                String ftype = field.getFieldType() == null ? "notset" : field.getFieldType();
                String jiraFieldName = "";
                String fieldValue = "";

                if (ftype.equals("TT_FIXED_FIELD")) {

                    jiraFieldName = field.getFieldName().replace("TT_FIXED_FIELD_", "");

                    //Skip project and issue type, has already been validated and processed (IssueInputBuilder constructor) during project selection and issue type selection
                    if (jiraFieldName.equalsIgnoreCase("Project") || jiraFieldName.equalsIgnoreCase("Issue Type")) {
                        continue;
                    }

                    //Used to determine if the newly created issue can be transitioned
                    if (jiraFieldName.equalsIgnoreCase("Status") && !field.getFieldValue().isEmpty()) {
                        statusIsSet = true;
                    }

                    //Add watcher to issue, a watcher is added after the issue is created successfully so no need to process this field further
                    if (jiraFieldName.equalsIgnoreCase("Watchers") && !field.getFieldValue().isEmpty()) {
                        log.warn("Watchers field is being processed");
                        watchIssue = true;
                        watcherUsername = field.getFieldValue();
                        continue;
                    }

                    fieldValue = field.getFieldValue();

                }

                if (ftype.equals("TT_USER_FIELD")) {
                    String jiraFieldName1 = field.getFieldName().replace("TT_USER_FIELD_", "");//TT_USER_FIELD_Description0_q

                    /*Skip already processed field*/
                    if (jiraFieldName1.contains(fieldRepetitionFlag)) {
                        continue;
                    }

                    String correctFieldName = jiraFieldName1.substring(0, (jiraFieldName1.indexOf("_") - 1));//Description
                    jiraFieldName = correctFieldName;

                    if (correctFieldName != null) {
                        String myValue = buildQuestionAnswerString(correctFieldName, qFields);
                        fieldRepetitionFlag = correctFieldName;
                        fieldValue = myValue;
                    }

                }

                boolean ibelongHere = false;
                boolean amsystemType = false;

                StringBuilder poissibleIssueTypeFields = new StringBuilder();

                //Doing validation of fields for this issue type
                for (Map.Entry<String, CimFieldInfo> entry : issueType.getFields().entrySet()) {
                    CimFieldInfo fieldInfo = entry.getValue();
                    String name = fieldInfo.getName();
                    String fieldId = fieldInfo.getId();

                    //Status is not part of Issue Type fields (Form fields by default), it belongs to a work-flow so we dont validate it
                    if (jiraFieldName.equalsIgnoreCase("Status")) {
                        ibelongHere = true;
                        amsystemType = true;
                        break;
                    }
                    //Only enforce validation for customfields
                    if (name.equals(jiraFieldName)) {
                        ibelongHere = true;
                        if (!fieldId.startsWith("customfield_")) {
                            amsystemType = true;
                        }
                        break;
                    }
                    poissibleIssueTypeFields.append(name).append(", ");

                }

                if (!ibelongHere && !amsystemType) {
                    String errorString = "";
                    String allIssueTypeFields = "";
                    String iFields = poissibleIssueTypeFields.toString();
                    if (!iFields.isEmpty()) {
                        allIssueTypeFields = iFields.substring(0, iFields.lastIndexOf(","));
                    }
                    errorString = "Unknown custom or default field \"" + jiraFieldName + "\" passed from SCA to TT as \"" + field.getFieldName() + "\" for issue type. TT fields must match JIRA configured fields for issue type \"" + issueType.getName() + "\"\n"
                            + "Possible fields for this issue type: [" + allIssueTypeFields + "]";
                    throw createError(TTError.class, errorString);
                }

                //Validation is done, field seems ok
                log.debug("Field validation was successful for field[{}]", jiraFieldName);

                for (Map.Entry<String, CimFieldInfo> entry : issueType.getFields().entrySet()) {

                    CimFieldInfo fieldInfo = entry.getValue();
                    String fieldCustomType = fieldInfo.getSchema().getCustom();
                    String fieldType = fieldInfo.getSchema().getType();
                    String fieldId = fieldInfo.getId();

                    if (!fieldInfo.getName().equals(jiraFieldName)) {
                        continue;
                    }

                    log.debug("Found a match to process for field[{}], see line below for details", jiraFieldName);
                    log.debug("FieldId [{}], FieldName [{}] schema: [{}] required: [{}]\n", new Object[]{fieldId, fieldInfo.getName(), fieldInfo.getSchema(), fieldInfo.isRequired()});

                    //choose value for this field
                    Object value = null;
                    final Iterable<Object> allowedValues = fieldInfo.getAllowedValues();

                    if (allowedValues != null) {

                        log.debug("Allowed values is not null");

                        //Perform validation of value passed in from SCA against JIRA configured value
                        boolean matchingCascadingSelect = false;
                        boolean matchingSelect = false;
                        StringBuilder cascadingSelectPossibleValues = new StringBuilder();
                        StringBuilder selectPossibleValues = new StringBuilder();

                        for (Object val : allowedValues) {
                            CustomFieldOption opt = (CustomFieldOption) val;
                            if ("com.atlassian.jira.plugin.system.customfieldtypes:cascadingselect".equals(fieldCustomType)) {
                                if (opt.getValue().equals(fieldValue)) {
                                    matchingCascadingSelect = true;
                                }
                                cascadingSelectPossibleValues.append(opt.getValue()).append(", ");

                            } else if ("com.atlassian.jira.plugin.system.customfieldtypes:select".equals(fieldCustomType)) {
                                if (opt.getValue().equals(fieldValue)) {
                                    matchingSelect = true;
                                }
                                selectPossibleValues.append(opt.getValue()).append(", ");
                            }
                        }

                        if (!matchingCascadingSelect && !matchingSelect) {
                            String cascading = cascadingSelectPossibleValues.toString();
                            String select = selectPossibleValues.toString();
                            String allowedVauesErrors = "";
                            if (!select.isEmpty()) {
                                String subSelect = select.substring(0, select.lastIndexOf(","));
                                allowedVauesErrors = "The value provided [" + fieldValue + "] for field [" + jiraFieldName + "] is not part of currently configured JIRA field values. Fields entered via Mindmap should also be in JIRA.\nPossible values are [" + subSelect + "]";
                            }
                            if (!cascading.isEmpty()) {
                                String subCascading = cascading.substring(0, cascading.lastIndexOf(","));
                                allowedVauesErrors = "The value provided [" + fieldValue + "] for field [" + jiraFieldName + "] is not part of currently configured JIRA field values. Fields entered via Mindmap should also be in JIRA.\nPossible values are [" + subCascading + "]";
                            }
                            throw createError(TTError.class, allowedVauesErrors);
                        }

                        for (Object val : allowedValues) {

                            if ("com.atlassian.jira.plugin.system.customfieldtypes:cascadingselect".equals(fieldCustomType)) {

                                // there is option with children - set it
                                CustomFieldOption opt = (CustomFieldOption) val;

                                if (opt.getValue().equals(fieldValue)) {

                                    log.debug("I am a parent and have kids, my name is [{}]", opt.getValue());
                                    Iterable<CustomFieldOption> myKids = opt.getChildren();
                                    Iterator<CustomFieldOption> kids = myKids.iterator();

                                    CustomFieldOption cfo = null;
                                    while (kids.hasNext()) {
                                        cfo = kids.next();
                                        log.debug("Am a kid my mom is [{}], this is my name: [{}]", opt.getValue(), cfo.getValue());
                                        //take one kid
                                        break;
                                    }

                                    value = new CustomFieldOption(opt.getId(), opt.getSelf(), opt.getValue(), Collections.<CustomFieldOption>emptyList(), cfo);
                                    break;
                                }
                            } else if ("com.atlassian.jira.plugin.system.customfieldtypes:select".equals(fieldCustomType)) {
                                // no sub-values available, set only top level value
                                CustomFieldOption opt = (CustomFieldOption) val;

                                if (opt.getValue().equals(fieldValue)) {
                                    value = opt;
                                    log.debug("I am single no children my name is [{}]", opt.getValue());
                                    break;
                                }
                            }
                        }
                    } else {
                        log.debug("I have no allowed values in me by default, going to test my type and populate value");

                        if ("com.atlassian.jirafisheyeplugin:jobcheckbox".equals(fieldCustomType)) {

                            value = fieldValue; //value = "false";

                        } else if ("com.atlassian.jira.plugin.system.customfieldtypes:url".equals(fieldCustomType)) {

                            value = fieldValue; //value = "http://www.atlassian.com/";

                        } else if ("string".equals(fieldType)) {

                            value = fieldValue;

                        } else if ("number".equals(fieldType)) {

                            if ("com.atlassian.jira.plugin.system.customfieldtypes:float".equals(fieldCustomType)) {
                                try {
                                    value = Float.parseFloat(fieldValue);
                                } catch (Exception ex) {
                                    value = Double.parseDouble(fieldValue);
                                }
                            } else {
                                try {
                                    value = Long.parseLong(fieldValue);
                                } catch (Exception ex) {
                                    value = Integer.parseInt(fieldValue);
                                }
                            }
                        } else if ("user".equals(fieldType)) {
                            if ("assignee".equals(fieldId)) {
                                value = getBasicUser(fieldValue, connection);
                            }
                        } else if ("array".equals(fieldType) && "user".equals(fieldInfo.getSchema().getItems())) {

                            if (fieldInfo.isRequired()) {
                                log.debug("Someone called an array of users");//value = ImmutableList.of(getBasicUser(fieldValue), getBasicUser(fieldValue), getBasicUser(fieldValue));
                            }
                        } else if ("group".equals(fieldType)) {

                            if (fieldInfo.isRequired()) {
                                log.debug("Someone called a group");//value = ComplexIssueInputFieldValue.with("name", GROUP_JIRA_ADMINISTRATORS);
                            }
                        } else if ("array".equals(fieldType) && "group".equals(fieldInfo.getSchema().getItems())) {

                            if (fieldInfo.isRequired()) {
                                log.debug("Someone called a list of groups");//value = ImmutableList.of(ComplexIssueInputFieldValue.with("name", GROUP_JIRA_ADMINISTRATORS), ComplexIssueInputFieldValue.with("name", GROUP_JIRA_ADMINISTRATORS));
                            }
                        } else if ("date".equals(fieldType)) {

                            Date dd = Utils.getStringAsDate(fieldValue, "dd/MM/yyyy", new Date());
                            DateTime dt = new DateTime(dd);
                            value = JsonParseUtil.formatDate(dt);

                        } else if ("datetime".equals(fieldType)) {

                            Date dd = Utils.getStringAsDate(fieldValue, "dd/MM/yyyy T HH:MM:SS", new Date());
                            DateTime dt = new DateTime(dd);
                            value = JsonParseUtil.formatDate(dt);

                        } else if ("array".equals(fieldType) && "string".equals(fieldInfo.getSchema().getItems())) {
                            //value = ImmutableList.of("one", "two", "three");
                        } else if ("timetracking".equals(fieldType)) {

                            if (fieldInfo.isRequired()) {
                                log.debug("Someone is asking for time spent");//value = new TimeTracking(60, 40, null); // time spent is not allowed
                            }
                        } else if (fieldInfo.isRequired()) {
                            log.warn("I don't know how to fill that required field, sorry.");
                        } else {
                            log.debug("Field value is not required, leaving blank");
                        }
                    }

                    if (value == null) {
                        log.debug("Value is null, skipping this field");
                    } else {
                        log.debug("Setting value => [{}]", value);
                        issueBuilder.setFieldValue(fieldId, value);
                    }

                    log.debug("Am done processing myself[{}], breaking out now", jiraFieldName);
                    break;
                }
            }

            // all required data has been sanitised and captured, let's create issue
            final IssueInput issueInput = issueBuilder.build();

            BasicIssue basicCreatedIssue = null;
            try {
                //creating the issue by sending request to JIRA
                log.debug("All required data has been sanitised and captured, sending 'create issue' request to JIRA");
                basicCreatedIssue = connection.getIssueClient().createIssue(issueInput).claim();

            } catch (Exception ex1) {
                Throwable cause = ex1.getCause();
                if (cause instanceof UniformInterfaceException) {
                    UniformInterfaceException uie = (UniformInterfaceException) cause;
                    String errMsg = "JIRA's response is[" + uie.getLocalizedMessage() + "], the actual cause is[" + ex1.getLocalizedMessage() + "]";
                    throw createError(TTError.class, errMsg);
                }
                throw processError(TTError.class, ex1);
            }

            com.atlassian.jira.rest.client.api.domain.Issue jIssue = connection.getIssueClient().getIssue(basicCreatedIssue.getKey()).claim();
            log.debug("Created new issue successfully, key: " + basicCreatedIssue.getKey());

            connection.getIssueClient().watch(jIssue.getWatchers().getSelf());
            if (watchIssue) {

                if (watcherUsername.contains(":")) {
                    String[] watchers = watcherUsername.split(":");
                    for (String watcher : watchers) {
                        log.warn("Going to add a watcher [{}] for this issue", watcher);
                        try {
                            connection.getIssueClient().addWatcher(jIssue.getWatchers().getSelf(), watcher).claim();
                        } catch (Exception ex) {
                            log.warn("Tried to add watcher but failed, reason: {}", ex.toString());
                        }
                    }
                } else if (!watcherUsername.isEmpty()) {
                    try {
                        log.warn("Going to add a watcher [{}] for this issue", watcherUsername);
                        connection.getIssueClient().addWatcher(jIssue.getWatchers().getSelf(), watcherUsername).claim();
                    } catch (Exception ex) {
                        log.warn("Tried to add watcher but failed, reason: {}", ex.toString());
                    }
                }
            }

            String transUri = "empty";
            /*Transition the issue*/
            try {
                if (statusIsSet) {
                    final Iterable<Transition> transitions = connection.getIssueClient().getTransitions(jIssue.getTransitionsUri()).claim();
                    final Transition notifyCustomerIssueTransition = getTransitionByName(transitions, CC_RESOLVED_NOTIFY_CUSTOMER_TRANSITION, jIssue.getIssueType().getName());
                    transUri = "Transition uri: " + jIssue.getTransitionsUri() + ", Transition name: " + CC_CUSTOMER_NOTIFIED_TRANSITION;
                    connection.getIssueClient().transition(jIssue.getTransitionsUri(), new TransitionInput(notifyCustomerIssueTransition.getId())).claim();
                }
                if (statusIsSet) {
                    final Iterable<Transition> transitions = connection.getIssueClient().getTransitions(jIssue.getTransitionsUri()).claim();
                    final Transition customerNotifiedIssueTransition = getTransitionByName(transitions, CC_CUSTOMER_NOTIFIED_TRANSITION, jIssue.getIssueType().getName());
                    transUri = "Transition uri: " + jIssue.getTransitionsUri() + ", Transition name: " + CC_CUSTOMER_NOTIFIED_TRANSITION;
                    connection.getIssueClient().transition(jIssue.getTransitionsUri(), new TransitionInput(customerNotifiedIssueTransition.getId())).claim();
                }
                if (statusIsSet) {
                    final Iterable<Transition> transitions = connection.getIssueClient().getTransitions(jIssue.getTransitionsUri()).claim();
                    final Transition closeIssueTransition = getTransitionByName(transitions, CC_CLOSE_ISSUE_TRANSITION, jIssue.getIssueType().getName());
                    transUri = "Transition uri: " + jIssue.getTransitionsUri() + ", Transition name: " + CC_CLOSE_ISSUE_TRANSITION;
                    connection.getIssueClient().transition(jIssue.getTransitionsUri(), new TransitionInput(closeIssueTransition.getId())).claim();
                }
            } catch (Exception exx) {
                String errorMessage = exx.getMessage() == null ? "" : exx.getMessage();
                transUri += ", Error cause: " + errorMessage;
                Throwable cause = exx.getCause();
                if (cause instanceof UniformInterfaceException) {
                    String msg = "Failed to transion the new issue[" + jIssue.getKey() + "], check the workflow scheme and transitions applicable for issue type \"" + jIssue.getIssueType().getName() + "\" for project [" + jIssue.getProject().getName() + "],  Extra-Info [" + transUri + "]";
                    throw createError(TTError.class, msg);
                }
                String msg = "Failed to transion the new issue[" + jIssue.getKey() + "], with issue type \"" + jIssue.getIssueType().getName() + "\" for project [" + jIssue.getProject().getName() + "],  Extra-Info [" + transUri + "]";
                throw createError(TTError.class, msg);
            }

            if (log.isDebugEnabled()) {
                log.debug("Populate return issue object...");
            }
            returnIssue = new Issue();

            returnIssue.setID(jIssue.getKey());
            returnIssue.setProject(jIssue.getProject().getKey());
            returnIssue.setIssueType(jIssue.getIssueType().getName());
            returnIssue.setPriority(jIssue.getPriority().getName());

            returnIssue.setDueDate(jIssue.getDueDate() == null ? Utils.getDateAsXMLGregorianCalendar(new Date()) : Utils.getDateAsXMLGregorianCalendar(jIssue.getDueDate().toDate()));
            returnIssue.setSummary(jIssue.getSummary());
            returnIssue.setReporter(jIssue.getReporter().getDisplayName());
            returnIssue.setDescription(jIssue.getDescription() == null ? "Non" : jIssue.getDescription());
            returnIssue.setCreated(jIssue.getCreationDate() == null ? Utils.getDateAsXMLGregorianCalendar(new Date()) : Utils.getDateAsXMLGregorianCalendar(jIssue.getCreationDate().toDate()));

            returnIssue.setAssignee(jIssue.getAssignee() == null ? "Unassigned" : jIssue.getAssignee().getDisplayName());
            returnIssue.setResolution(jIssue.getResolution() == null ? "" : jIssue.getResolution().getName());
            returnIssue.setStatus(jIssue.getStatus().getName());
            returnIssue.setUpdated(jIssue.getUpdateDate() == null ? Utils.getDateAsXMLGregorianCalendar(new Date()) : Utils.getDateAsXMLGregorianCalendar(jIssue.getUpdateDate().toDate()));

            List<JiraField> jfList = new ArrayList<>();
            returnIssue.setJiraFields(new JiraFields());

            //Populate custom fields
            String sdProject = BaseUtils.getProperty("env.jira.saleslead.project.key");
            String ccProject = BaseUtils.getProperty("env.jira.customer.care.project.key");
            
            if (returnIssue.getProject().equals(sdProject)) {
                inflateSalesLeadIssueWithPreDefinedTTFields(jIssue, jfList, issueType.getFields().entrySet());
            } else if (returnIssue.getProject().equals(ccProject)) {
                inflateCustomerCareIssueWithPreDefinedTTFields(jIssue, jfList, issueType.getFields().entrySet());
            }

            int resultSize = newIssue.getJiraFields().getJiraField().size();
            returnIssue.getJiraFields().setTotalJiraFields(resultSize);
            returnIssue.getJiraFields().getJiraField().addAll(jfList);

        } catch (Exception e) {
            throw processError(TTError.class, e);
        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return returnIssue;
    }

    @Override
    public IssueList getIssues(IssueQuery issueQuery) throws TTError {
        setContext(issueQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        SearchResult results;
        IssueList issues = null;
        Set<String> issueIds = null;

        JiraRestClient connection = null;
        try {
            connection = openRestConnection();

            Date dateFrom = Utils.getXMLGregorianCalendarAsDate(issueQuery.getCreatedDateFrom(), new java.util.Date(0));
            Date dateTo = Utils.getXMLGregorianCalendarAsDate(issueQuery.getCreatedDateTo(), new java.util.Date());

            String wildCardChar = (issueQuery.getCustomerName() == null ? "" : issueQuery.getCustomerName());
            String issueID = (issueQuery.getIssueID() == null ? "" : issueQuery.getIssueID());
            String customerId = issueQuery.getCustomerId();
            String salesLeadAssignee = (issueQuery.getSalesLeadAssignee() == null ? "" : issueQuery.getSalesLeadAssignee());
            //PK=Project Key
            String salesLeadPK = (issueQuery.getSalesLeadPK() == null ? "" : issueQuery.getSalesLeadPK());
            int resultLimit = issueQuery.getResultLimit();
            StringBuilder jqlBuilder = new StringBuilder();

            if (!issueID.isEmpty() && issueID.length() > 0) {
                //Do nothing here, work will be done somewhere below after this condition
            } else if (!salesLeadAssignee.isEmpty() && !salesLeadPK.isEmpty()) {
                log.debug("All sales leads for a specific assignee");
                String statusString = "AND (status = 'open' OR status = 'reopened')";
                jqlBuilder.append("project = '").append(salesLeadPK).append("' ");
                jqlBuilder.append(statusString);
                jqlBuilder.append(" AND (assignee = \"").append(salesLeadAssignee).append("\")");

                //jqlBuilder.append(createdDateString);
            } else if (!salesLeadPK.isEmpty() && !wildCardChar.isEmpty()) {
                log.debug("Sales leads based on customer wildcard");
                if (JIRA_VERSION == 0) {
                    JIRA_VERSION = connection.getMetadataClient().getServerInfo().claim().getBuildNumber();
                }
                issueIds = getIssueKeysFromDB(salesLeadPK, SL_CUSTOMER_FIRST_NAME, wildCardChar, resultLimit, JIRA_VERSION);
            } else {
                log.debug("Retrieving tickets for a specifc customer");
                if (log.isDebugEnabled()) {
                    log.debug("Retrieve issues for customerID: [" + customerId + "], dateFrom: [" + dateFrom + "], dateTo: [" + dateTo + "], issueID: [" + issueID + "], resultLimit: [" + resultLimit + "]");
                }

                String channel = issueQuery.getIncidentChannel() == null ? "" : issueQuery.getIncidentChannel();
                String icpChannelProp = BaseUtils.getProperty("env.jira.icp.cc.channel", "ICP Sales Team");

                //building: AND (status = 'open' OR status = 'closed' OR status = 'reopened' OR status = 'resolved' )
                String statusString = getSqlStatusString(BaseUtils.getPropertyAsList("env.jira.filter.status.name"));
                jqlBuilder.append(statusString);

                //building: AND (cf[10003] ~ "23")
                if (!channel.isEmpty() && channel.equalsIgnoreCase(icpChannelProp)) {
                    log.debug("Query is based on ICP Sales agent, adding organisation ID and agent ID");
                    String icpSalesPersonId = customerId;
                    String icpSalesPersonCFID = getJiraFieldIdFromCache(CC_ICPSALES_PERSON_ID);
                    String icpOrganisationCFID = getJiraFieldIdFromCache(CC_ICPORGANISATION_ID);

                    long icpSalesPersonFieldID = Long.parseLong(icpSalesPersonCFID.split("\\_")[1]);// e.g. customfield_10060
                    long icpOrganisationFieldID = Long.parseLong(icpOrganisationCFID.split("\\_")[1]);

                    String sqlCF3 = " AND (cf[" + icpSalesPersonFieldID + "] ~ \"" + icpSalesPersonId + "\")" + " AND (cf[" + icpOrganisationFieldID + "] ~ \"" + wildCardChar + "\")";
                    jqlBuilder.append(sqlCF3);

                } else {

                    log.debug("Query is based on SCP customer ID");
                    String custIdCFID = getJiraFieldIdFromCache(CC_SMILE_CUSTOMER_ID);
                    long custIdFieldID = Long.parseLong(custIdCFID.split("\\_")[1]);
                    String sqlCF = " AND (cf[" + custIdFieldID + "] ~ \"" + customerId + "\")";
                    jqlBuilder.append(sqlCF);
                }

                //search only CC projects
                String jqlProjectName = " AND (project = \"" + BaseUtils.getProperty("env.jira.customer.care.project.key") + "\")";
                jqlBuilder.append(jqlProjectName);

                //request from SCP/ICP, building: AND (cf[10003] = "SCP Help Request")
                if (!channel.isEmpty()) {

                    String incidentChannelCFID = getJiraFieldIdFromCache(CC_INCIDENT_CHANNEL);
                    long iChannelFieldID = Long.parseLong(incidentChannelCFID.split("\\_")[1]);

                    if (channel.equalsIgnoreCase(icpChannelProp)) {
                        log.debug("Query is based on ICP channel");
                        String sqlCF2 = " AND (cf[" + iChannelFieldID + "] = \"" + icpChannelProp + "\")";
                        jqlBuilder.append(sqlCF2);
                    } else {
                        log.debug("Query is based on SCP channel");
                        String sqlSCPCF = " AND (cf[" + iChannelFieldID + "] = \"" + issueQuery.getIncidentChannel() + "\")";
                        jqlBuilder.append(sqlSCPCF);
                    }
                }
            }

            jqlBuilder.append(" order by created");

            if (issueIds != null) {
                log.debug("IssueIds is not null");
                issues = getAllWildCardIssuesFromCache(issueQuery, issueIds, connection);
            } else if (!issueID.isEmpty()) {
                log.debug("IssueId is not empty. Getting issue by Id");
                issues = new IssueList();
                populateIssueList(issues.getIssueList(), issueQuery, issueID, connection);
            } else {

                log.debug("Issue list is based on a Query. About to use rest client to run JQL [{}]", jqlBuilder);
                issues = new IssueList();
                final ImmutableSet<String> fields = ImmutableSet.of("*all");
                long start = System.currentTimeMillis();
                results = connection.getSearchClient().searchJql(jqlBuilder.toString(), issueQuery.getResultLimit(), 0, fields).claim();
                log.debug("Finished calling Jira server with JQL. Jira returned [{}] results and took [{}]ms", results.getTotal(), System.currentTimeMillis() - start);

                Iterator<com.atlassian.jira.rest.client.api.domain.Issue> itIssues = results.getIssues().iterator();
                String strDateTo = getLDNDate(dateTo);
                Date dateToJHB = Utils.getStringAsDate(strDateTo, "yyyy/MM/dd HH:mm");
                String strDateFrom = getLDNDate(dateFrom);
                Date dateFromJHB = Utils.getStringAsDate(strDateFrom, "yyyy/MM/dd HH:mm");
                int limit = 0;

                while (itIssues.hasNext()) {
                    com.atlassian.jira.rest.client.api.domain.Issue apiIssue = itIssues.next();
                    Date createdDate = Utils.getXMLGregorianCalendarAsDate(Utils.getDateAsXMLGregorianCalendar(apiIssue.getCreationDate().toDate()), new java.util.Date());
                    if (createdDate.after(dateFromJHB) && createdDate.before(dateToJHB) && limit < issueQuery.getResultLimit()) {
                        log.debug("Issue matches date filter criteria: {}, created {}", new Object[]{apiIssue.getKey(), createdDate});
                        populateIssueList(issues.getIssueList(), issueQuery, apiIssue);
                        limit++;
                    }
                    if (limit >= issueQuery.getResultLimit()) {
                        break;
                    }
                }
            }
        } catch (Exception e) {

            Throwable cause = Utils.getDeepestCause(e);
            if (cause instanceof UniformInterfaceException) {
                UniformInterfaceException uie = (UniformInterfaceException) cause;
                String errMsg = "JIRA's response is[" + uie.getLocalizedMessage() + "], the actual cause is[" + e.getLocalizedMessage() + "]";
                throw createError(TTError.class, errMsg);
            }

            throw processError(TTError.class, e);

        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return issues;
    }

    @Override
    public ProjectList getProjects(ProjectQuery projectQuery) throws TTError {
        setContext(projectQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        ProjectList projects = new ProjectList();
        JiraRestClient connection = null;
        try {
            connection = openRestConnection();
            MetaData metadata = xmlMetadataCache.get("metaData");
            if (metadata == null) {
                internalGetStatuses(projects.getProjectList(), connection);
            } else {
                projects.getProjectList().addAll(metadata.getProjectList());
            }
        } catch (Exception e) {
            throw processError(TTError.class, e);
        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return projects;
    }

    @Override
    public IssueTypeList getIssueTypes(IssueTypeQuery issueTypeQuery) throws TTError {
        setContext(issueTypeQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        IssueTypeList issueTypes = new IssueTypeList();
        JiraRestClient connection = null;
        try {
            connection = openRestConnection();
            MetaData metadata = xmlMetadataCache.get("metaData");
            if (metadata == null) {
                internalGetStatuses(issueTypes.getIssueTypeList(), connection);
            } else {
                issueTypes.getIssueTypeList().addAll(metadata.getIssueTypeList());
            }
        } catch (Exception e) {
            throw processError(TTError.class, e);
        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return issueTypes;
    }

    @Override
    public PriorityList getPriorities(PriorityQuery priorityQuery) throws TTError {
        setContext(priorityQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        PriorityList priorities = new PriorityList();
        JiraRestClient connection = null;
        try {
            connection = openRestConnection();
            MetaData metadata = xmlMetadataCache.get("metaData");
            if (metadata == null) {
                internalGetStatuses(priorities.getPriorityList(), connection);
            } else {
                priorities.getPriorityList().addAll(metadata.getPriorityList());
            }
        } catch (Exception e) {
            throw processError(TTError.class, e);
        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return priorities;
    }

    @Override
    public ResolutionList getResolutions(ResolutionQuery resolutionQuery) throws TTError {
        setContext(resolutionQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        ResolutionList resolutions = new ResolutionList();
        JiraRestClient connection = null;
        try {
            connection = openRestConnection();
            MetaData metadata = xmlMetadataCache.get("metaData");
            if (metadata == null) {
                internalGetStatuses(resolutions.getResolutionList(), connection);
            } else {
                resolutions.getResolutionList().addAll(metadata.getResolutionList());
            }
        } catch (Exception e) {
            throw processError(TTError.class, e);
        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return resolutions;
    }

    @Override
    public StatusList getStatuses(StatusQuery statusQuery) throws TTError {
        setContext(statusQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        StatusList statuses = new StatusList();
        JiraRestClient connection = null;
        try {
            connection = openRestConnection();
            MetaData metadata = xmlMetadataCache.get("metaData");

            if (metadata == null) {
                internalGetStatuses(statuses.getStatusList(), connection);
            } else {
                statuses.getStatusList().addAll(metadata.getStatusList());
            }
        } catch (Exception e) {
            throw processError(TTError.class, e);
        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return statuses;
    }

    @Override
    public MetaData getMetaData(MetaDataQuery metaDataQuery) throws TTError {
        setContext(metaDataQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        MetaData metadata = xmlMetadataCache.get("metaData");
        JiraRestClient connection = null;
        try {
            connection = openRestConnection();
            if (metadata == null) {
                log.warn("XML Metadata is null, going to populate it");
                metadata = new MetaData();
                internalGetProjects(metadata.getProjectList(), connection);
                internalGetIssueTypes(metadata.getIssueTypeList(), connection);
                internalGetPriorities(metadata.getPriorityList(), connection);
                internalGetResolutions(metadata.getResolutionList(), connection);
                internalGetStatuses(metadata.getStatusList(), connection);
                xmlMetadataCache.put("metaData", metadata);
            } else {
                log.debug("XML Metadata is not null going to return it. ProjectList size: {}", metadata.getProjectList().size());
            }

        } catch (Exception ex) {
            throw processError(TTError.class, ex);
        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return metadata;
    }

    @Override
    public Issue modifyIssue(Issue modifiedIssue) throws TTError {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Check isUser(UserID userIDToCheck) throws TTError {
        setContext(userIDToCheck, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        Check check = new com.smilecoms.xml.schema.tt.Check();
        JiraRestClient connection = null;
        try {
            connection = openRestConnection();
            String userID = userIDToCheck.getUserID();
            /* Open Connection */
            if (log.isDebugEnabled()) {
                log.debug("Checking if user [" + userID + "] is a JIRA user...");
            }
            com.atlassian.jira.rest.client.api.domain.User user = null;
            try {
                /*The JIRA administrator's id*/
                if (userID.equals("admin")) {
                    user = connection.getUserClient().getUser(BaseUtils.getProperty("env.jira.security.username")).claim();
                } else {
                    user = connection.getUserClient().getUser(userID).claim();
                }
            } catch (Exception ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof UniformInterfaceException) {
                    UniformInterfaceException uie = (UniformInterfaceException) cause;
                    String errMsg = "JIRA's response is[" + uie.getLocalizedMessage() + "], the actual cause is[" + ex.getLocalizedMessage() + "]";
                    throw createError(TTError.class, errMsg);
                }
                throw processError(TTError.class, ex);
            }

            if (user.getName() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("User was found in JIRA database...");
                }
                check.setCheck(StCheck.TRUE);
            }
        } catch (Exception ex) {
            throw processError(TTError.class, ex);
        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return check;
    }

    @Override
    public Comment createComment(Comment newComment) throws TTError {
        setContext(newComment, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        Comment returnComment = null;
        JiraRestClient connection = null;
        try {
            connection = openRestConnection();
            /* Open Connection */
            String issuekey = newComment.getIssueKey();

            if (log.isDebugEnabled()) {
                log.debug("Invoking remote JIRA REST API to retrieve comment for issueKey [" + issuekey + "]...");
            }

            String urlResource = CONTEXT_AND_API_VERSION + "/issue/" + issuekey + "/comment";
            URI uri = new URI(BaseUtils.getProperty("env.jira.server.url") + urlResource);
            BasicUser author = new BasicUser(uri, newComment.getAuthor(), "");

            Date createdDate = Utils.getJavaDate(newComment.getCreated());
            Date updatedDate = Utils.getJavaDate(newComment.getUpdated());
            org.joda.time.DateTime jodaCreatedDate = new org.joda.time.DateTime(createdDate);
            org.joda.time.DateTime jodaUpdatedDate = new org.joda.time.DateTime(updatedDate);

            //com.atlassian.jira.rest.client.api.domain.Comment.
            //final IssueRestClient issueClient = client.getIssueClient();
            //final Issue issue = issueClient.getIssue(issueKey).claim();
            //final List<Comment> initialComments = Lists.newArrayList(issue.getComments());
            //issueClient.addComment(issue.getCommentsUri(), comment).claim();
            com.atlassian.jira.rest.client.api.domain.Comment comObj = new com.atlassian.jira.rest.client.api.domain.Comment(uri, newComment.getBody(), author, author, jodaCreatedDate, jodaUpdatedDate, null, null);
            connection.getIssueClient().addComment(uri, comObj);

            com.atlassian.jira.rest.client.api.domain.Issue issue = connection.getIssueClient().getIssue(newComment.getIssueKey()).claim();
            Iterable<com.atlassian.jira.rest.client.api.domain.Comment> commentss = issue.getComments();
            Iterator<com.atlassian.jira.rest.client.api.domain.Comment> commIterator = commentss.iterator();

            while (commIterator.hasNext()) {
                com.atlassian.jira.rest.client.api.domain.Comment itComm = commIterator.next();
                long id = itComm.getId();
                String commentAuthor = itComm.getAuthor().getDisplayName();
                String commentBody = itComm.getBody();
                Calendar commentCreated = itComm.getCreationDate().toCalendar(Locale.getDefault());
                Calendar commentUpdated = itComm.getUpdateDate().toCalendar(Locale.getDefault());

                Comment xmlComment = new Comment();
                if (log.isDebugEnabled()) {
                    log.debug("Comment with id [" + id + "] is returned");
                }
                xmlComment.setID(String.valueOf(id));
                xmlComment.setAuthor(commentAuthor);
                xmlComment.setBody(commentBody);

                Date dateCreated = commentCreated.getTime();
                xmlComment.setCreated(Utils.getDateAsXMLGregorianCalendar(dateCreated));
                Date dateUpdated = commentUpdated.getTime();
                xmlComment.setUpdated(Utils.getDateAsXMLGregorianCalendar(dateUpdated));

                xmlComment.setIssueKey(issuekey);
                returnComment = xmlComment;
            }

        } catch (Exception e) {
            throw processError(TTError.class, e);
        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return returnComment;

    }

    @Override
    public Comment modifyComment(Comment modifiedComment) throws TTError {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CommentList getComments(CommentQuery commentQuery) throws TTError {
        setContext(commentQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        CommentList comments = new CommentList();
        List commentList = comments.getCommentList();
        JiraRestClient connection = null;
        try {
            connection = openRestConnection();
            /* Open Connection */
            String issuekey = commentQuery.getIssueKey();
            if (log.isDebugEnabled()) {
                log.debug("Invoking remote JIRA service to retrieve comment for issueKey [" + issuekey + "]...");
            }

            com.atlassian.jira.rest.client.api.domain.Issue issue = connection.getIssueClient().getIssue(commentQuery.getIssueKey()).claim();
            Iterator<com.atlassian.jira.rest.client.api.domain.Comment> commentIterator = issue.getComments().iterator();

            while (commentIterator.hasNext()) {

                com.atlassian.jira.rest.client.api.domain.Comment itComm = commentIterator.next();
                long commentID = itComm.getId();
                String commentAuthor = itComm.getAuthor().getName();
                String commentBody = itComm.getBody();

                Comment xmlComment = new Comment();
                xmlComment.setID(String.valueOf(commentID));
                xmlComment.setAuthor(commentAuthor);
                xmlComment.setBody(commentBody);

                xmlComment.setCreated(itComm.getCreationDate() == null ? Utils.getDateAsXMLGregorianCalendar(new Date()) : Utils.getDateAsXMLGregorianCalendar(itComm.getCreationDate().toDate()));
                xmlComment.setUpdated(itComm.getUpdateDate() == null ? Utils.getDateAsXMLGregorianCalendar(new Date()) : Utils.getDateAsXMLGregorianCalendar(itComm.getUpdateDate().toDate()));
                xmlComment.setIssueKey(issuekey);

                commentList.add(xmlComment);
            }
        } catch (Exception e) {
            throw processError(TTError.class, e);
        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return comments;
    }

    @Override
    public Done isUp(String isUpRequest) throws TTError {
        if (!BaseUtils.isPropsAvailable()) {
            throw createError(TTError.class, "Properties are not available so this platform will be reported as down");
        }
        return makeDone();
    }

    @Override
    public Done modifyIssueWatcher(IssueWatcher issueWatcher) throws TTError {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public IssueWatcher getIssueWatchers(IssueWatcherQuery issueWatcherQuery) throws TTError {
        setContext(issueWatcherQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        IssueWatcher iw = new IssueWatcher();
        JiraRestClient connection = null;
        try {
            connection = openRestConnection();
            com.atlassian.jira.rest.client.api.domain.Issue issue = connection.getIssueClient().getIssue(issueWatcherQuery.getIssueID()).claim();
            BasicWatchers watchers = issue.getWatchers();
            if (watchers != null) {
                connection.getIssueClient().watch(watchers.getSelf()).claim();
            }
        } catch (Exception e) {
            throw processError(TTError.class, e);
        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return iw;
    }

    @Override
    public Done closeSalesLead(CloseSalesLeadIssueQuery closeSalesLeadIssueQuery) throws TTError {
        setContext(closeSalesLeadIssueQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        com.smilecoms.xml.schema.tt.Done done = new com.smilecoms.xml.schema.tt.Done();

        JiraRestClient connection = null;
        try {
            connection = openRestConnection();

            /* Open Connection */
            String issueID = closeSalesLeadIssueQuery.getIssueID();
            if (issueID != null && !issueID.isEmpty() && issueID.length() > 0) {

                com.atlassian.jira.rest.client.api.domain.Issue issue = connection.getIssueClient().getIssue(issueID).claim();
                /*Transition the issue: transitions are configured in JIRA, check the workflow scheme for this issue type*/
                String transUri = "empty";

                try {
                    if (issue.getStatus().getName().equalsIgnoreCase("Closed")) {
                        done.setDone(com.smilecoms.xml.schema.tt.StDone.FALSE);
                    } else {
                        final Iterable<Transition> transitions1 = connection.getIssueClient().getTransitions(issue.getTransitionsUri()).claim();
                        final Transition successfulIssueTransition = getTransitionByName(transitions1, SL_SUCCESSFUL_TRANSITION, issue.getIssueType().getName());
                        transUri = "Transition uri: " + issue.getTransitionsUri() + ", Transition name: " + SL_SUCCESSFUL_TRANSITION;
                        connection.getIssueClient().transition(issue.getTransitionsUri(), new TransitionInput(successfulIssueTransition.getId()));
                        done.setDone(com.smilecoms.xml.schema.tt.StDone.TRUE);
                    }
                } catch (Exception exx) {
                    String errorMessage = exx.getMessage() == null ? "" : exx.getMessage();
                    transUri += ", Error cause: " + errorMessage;
                    String msg = "Failed to transion the new issue[" + issue.getKey() + "], check the workflow scheme and transitions applicable for issue type \"" + issue.getIssueType().getName() + "\" for project [" + issue.getProject().getName() + "],  Extra-Info [" + transUri + "]";
                    throw createError(TTError.class, msg);
                }
            } else {
                log.warn("The issueId was empty or null, sales lead issue not closed.");
            }

        } catch (Exception e) {
            throw processError(TTError.class, e);
        } finally {
            closeRestConnection(connection);
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return done;
    }

    @Override
    public JiraUserList getJiraUsers(JiraUserQuery jiraUserQuery) throws TTError {

        /*[{"self":"http://jira.smilecoms.com/rest/api/2/user?username=firstname.lastname",
         * "name":"firstname.lastname","emailAddress":"firstname.lastname@smilecoms.com",
         * "avatarUrls":{"16x16":"http://jira.smilecoms.com/secure/useravatar?size=small&avatarId=10122",
         * "48x48":"http://jira.smilecoms.com/secure/useravatar?avatarId=10122"},
         * "displayName":"Firstname Lastname","active":true,"timeZone":"Africa/Johannesburg"}]*/
        JiraUserList jUserList = new JiraUserList();
        Map<String, User> jUser = new HashMap<>();

        try {

            if (jiraUserQuery.getUsername() != null && !jiraUserQuery.getUsername().isEmpty()) {
                log.debug("Looking up a single user with username [{}]", jiraUserQuery.getUsername());
                JSONArray jiraUsers = getJiraUsersAsJson(jiraUserQuery.getUsername());
                User user;
                for (int i = 0; i < jiraUsers.length(); i++) {
                    JSONObject jsonField = jiraUsers.getJSONObject(i);

                    /*no user was found*/
                    if (jsonField.isNull("self")) {
                        break;
                    }

                    String usernameURL = jsonField.getString("self");
                    String name = jsonField.getString("name");
                    String emailAddress = jsonField.getString("emailAddress");
                    String displayName = jsonField.getString("displayName");

                    String username = usernameURL.split("=")[1];

                    user = new User();
                    user.setUserID(username);
                    user.setFullName(displayName);
                    user.setEmail(emailAddress);

                    jUser.put(jiraUserQuery.getUsername(), user);
                }

            } else {

                int limit = jiraUserQuery.getResultLimit();
                int resultsToGet = limit;

                String lastName = jiraUserQuery.getLastName();
                if (lastName != null && !lastName.isEmpty() && resultsToGet > 0) {
                    log.debug("Searching using last name [{}]", lastName);
                    JSONArray jiraUsers = getJiraUsersAsJson(lastName);
                    User user;
                    for (int i = 0; i < jiraUsers.length(); i++) {
                        JSONObject jsonField = jiraUsers.getJSONObject(i);

                        /*no user was found*/
                        if (jsonField.isNull("self")) {
                            break;
                        }

                        String usernameURL = jsonField.getString("self");
                        String name = jsonField.getString("name");
                        String emailAddress = jsonField.getString("emailAddress");
                        String displayName = jsonField.getString("displayName");

                        String username = usernameURL.split("=")[1];

                        user = new User();
                        user.setUserID(username);
                        user.setFullName(displayName);
                        user.setEmail(emailAddress);

                        jUser.put(username, user);
                    }
                    resultsToGet = limit - jUser.size();
                }

                String firstName = jiraUserQuery.getFirstName();
                if (firstName != null && !firstName.isEmpty() && resultsToGet > 0) {
                    log.debug("Searching using first name [{}]", firstName);
                    JSONArray jiraUsers = getJiraUsersAsJson(firstName);
                    User user;
                    for (int i = 0; i < jiraUsers.length(); i++) {
                        JSONObject jsonField = jiraUsers.getJSONObject(i);

                        /*no user was found*/
                        if (jsonField.isNull("self")) {
                            break;
                        }

                        String usernameURL = jsonField.getString("self");
                        String name = jsonField.getString("name");
                        String emailAddress = jsonField.getString("emailAddress");
                        String displayName = jsonField.getString("displayName");

                        String username = usernameURL.split("=")[1];

                        user = new User();
                        user.setUserID(username);
                        user.setFullName(displayName);
                        user.setEmail(emailAddress);

                        jUser.put(username, user);
                    }
                    resultsToGet = limit - jUser.size();
                }

                String emailAdd = jiraUserQuery.getEmailAddress();
                if (emailAdd != null && !emailAdd.isEmpty() && resultsToGet > 0) {
                    log.debug("Searching using email [{}]", emailAdd);
                    JSONArray jiraUsers = getJiraUsersAsJson(emailAdd);
                    User user;
                    for (int i = 0; i < jiraUsers.length(); i++) {
                        JSONObject jsonField = jiraUsers.getJSONObject(i);

                        /*no user was found*/
                        if (jsonField.isNull("self")) {
                            break;
                        }

                        String usernameURL = jsonField.getString("self");
                        String name = jsonField.getString("name");
                        String emailAddress = jsonField.getString("emailAddress");
                        String displayName = jsonField.getString("displayName");

                        String username = usernameURL.split("=")[1];

                        user = new User();
                        user.setUserID(username);
                        user.setFullName(displayName);
                        user.setEmail(emailAddress);

                        jUser.put(username, user);
                    }
                    resultsToGet = limit - jUser.size();
                }
            }
            log.debug("Adding [{}] return results", jUser.size());
            for (User jUs : jUser.values()) {
                jUserList.getJiraUsers().add(jUs);
            }
            jUserList.setNumberOfUsers(jUserList.getJiraUsers().size());

        } catch (Exception ex) {
        }
        return jUserList;
    }

    @PostConstruct
    public void startUp() {
        if (doneStartUp) {
            return;
        }
        doneStartUp = true;
        BaseUtils.registerForPropsAvailability(this);
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsChanges(this);
        JPAUtils.closeEMF(emf);
    }

    @Override
    public void propsAreReadyTrigger() {
        BaseUtils.deregisterForPropsAvailability(this);
        populateMetaDataCache();
        BaseUtils.registerForPropsChanges(this);
    }

    @Override
    public void propsHaveChangedTrigger() {
        populateMetaDataCache();
        jiraFieldsCache.clear();
        wildCardIssuesCache.clear();
        xmlMetadataCache.clear();
    }

    /**
     * Utility method to make a complex boolean object with value TRUE.
     *
     * @return The resulting complex object
     */
    private com.smilecoms.xml.schema.tt.Done makeDone() {
        com.smilecoms.xml.schema.tt.Done done = new com.smilecoms.xml.schema.tt.Done();
        done.setDone(com.smilecoms.xml.schema.tt.StDone.TRUE);
        return done;
    }

    private void internalGetProjects(List projectList, JiraRestClient connection) throws Exception {

        try {
            /* Open Connection */
            if (log.isDebugEnabled()) {
                log.debug("Invoking remote JIRA service to retrieve projects...");
            }
            Iterable<BasicProject> allProjects = connection.getProjectClient().getAllProjects().claim();
            if (allProjects != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Populating return project list with  projects...");
                }

                for (BasicProject bProject : allProjects) {
                    String projectID = bProject.getSelf().toString();
                    String projectKey = bProject.getKey();
                    String projectName = bProject.getName();
                    String projectDescription = bProject.toString();

                    if (log.isDebugEnabled()) {
                        log.debug("Adding project [" + projectID + "]...");
                    }
                    Project project = new Project();
                    project.setID(projectID);
                    project.setKey(projectKey);
                    project.setName(projectName);
                    project.setDescription(projectDescription);
                    projectList.add(project);
                }
            }
        } catch (Exception ex) {
            throw processError(TTError.class, ex);
        }
    }

    private void internalGetIssueTypes(List issueTypeList, JiraRestClient connection) throws Exception {
        try {
            /* Open Connection */

            if (log.isDebugEnabled()) {
                log.debug("Invoking remote JIRA service to retrieve issue types...");
            }
            Iterable<com.atlassian.jira.rest.client.api.domain.IssueType> allIssueTypes = connection.getMetadataClient().getIssueTypes().claim();
            if (allIssueTypes != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Populating return issue type list with  issue types...");
                }

                for (com.atlassian.jira.rest.client.api.domain.IssueType jIssueType : allIssueTypes) {
                    String issueTypeID = jIssueType.getId().toString();
                    String issueTypeName = jIssueType.getName();
                    String issueTypeDescription = jIssueType.getDescription();

                    if (log.isDebugEnabled()) {
                        log.debug("Adding issue type [" + issueTypeID + "]...");
                    }
                    IssueType issueType = new IssueType();
                    issueType.setID(issueTypeID);
                    issueType.setName(issueTypeName);
                    issueType.setDescription(issueTypeDescription);
                    issueTypeList.add(issueType);
                }
            }
        } catch (Exception ex) {
            throw processError(TTError.class, ex);
        }
    }

    private void internalGetPriorities(List priorityList, JiraRestClient connection) throws Exception {

        try {
            /* Open Connection */

            if (log.isDebugEnabled()) {
                log.debug("Invoking remote JIRA service to retrieve priorities...");
            }
            Iterable<com.atlassian.jira.rest.client.api.domain.Priority> allPriorities = connection.getMetadataClient().getPriorities().claim();
            if (allPriorities != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Populating return priority list with  priorities...");
                }

                for (com.atlassian.jira.rest.client.api.domain.Priority pr : allPriorities) {
                    String priorityID = String.valueOf(pr.getId());
                    String priorityName = pr.getName();
                    String priorityDescription = pr.getDescription();

                    if (log.isDebugEnabled()) {
                        log.debug("Adding priority [" + priorityID + "]...");
                    }
                    Priority priority = new Priority();
                    priority.setID(priorityID);
                    priority.setName(priorityName);
                    priority.setDescription(priorityDescription);
                    priorityList.add(priority);
                }
            }
        } catch (Exception ex) {
            throw processError(TTError.class, ex);
        }
    }

    private void internalGetResolutions(List resolutionList, JiraRestClient connection) throws Exception {

        try {
            /* Open Connection */
            if (log.isDebugEnabled()) {
                log.debug("Invoking remote JIRA service to retrieve resolutions...");
            }
            Iterable<com.atlassian.jira.rest.client.api.domain.Resolution> resltns = connection.getMetadataClient().getResolutions().claim();
            Iterator<com.atlassian.jira.rest.client.api.domain.Resolution> itResolutions = resltns.iterator();

            if (itResolutions != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Populating return resolution list with resolutions...");
                }

                while (itResolutions.hasNext()) {
                    com.atlassian.jira.rest.client.api.domain.Resolution remoteresolution = itResolutions.next();
                    String resolutionID = remoteresolution.getSelf().toString();
                    String resolutionName = remoteresolution.getName();
                    String resolutionDescription = remoteresolution.getDescription();

                    if (log.isDebugEnabled()) {
                        log.debug("Adding resolution [" + resolutionID + "]...");
                    }
                    Resolution resolution = new Resolution();
                    resolution.setID(resolutionID);
                    resolution.setName(resolutionName);
                    resolution.setDescription(resolutionDescription);
                    resolutionList.add(resolution);
                }
            }
        } catch (Exception ex) {
            throw processError(TTError.class, ex);
        }
    }

    private void internalGetStatuses(List statusList, JiraRestClient connection) throws Exception {
        try {
            Iterable<com.atlassian.jira.rest.client.api.domain.Status> statuses = connection.getMetadataClient().getStatuses().claim();
            Iterator<com.atlassian.jira.rest.client.api.domain.Status> itStatuses = statuses.iterator();

            if (itStatuses != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Populating return status list with statuses...");
                }

                while (itStatuses.hasNext()) {
                    com.atlassian.jira.rest.client.api.domain.Status stat = itStatuses.next();
                    String statusID = stat.getId().toString();
                    String statusName = stat.getName();
                    String statusDescription = stat.getDescription();

                    if (log.isDebugEnabled()) {
                        log.debug("Adding status [" + statusID + "]...");
                    }
                    Status status = new Status();
                    status.setID(statusID);
                    status.setName(statusName);
                    status.setDescription(statusDescription);
                    statusList.add(status);
                }
            }
        } catch (Exception ex) {
            throw processError(TTError.class, ex);
        }
    }

    private JSONArray getJiraUsersAsJson(String paramValue) throws Exception {
        if (paramValue == null) {
            return null;
        }
        ClientResponse response = null;
        try {
            String urlResource = CONTEXT_AND_API_VERSION + "/user/search?username=" + paramValue;
            response = doRestGet(urlResource);
            String jsonResponse = response.getEntity(String.class);
            JSONArray jiraUsersArray = new JSONArray(jsonResponse);
            return jiraUsersArray;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private ClientResponse doRestGet(String urlResource) throws Exception {
        ClientResponse response = dispatchRestRequest(urlResource, "get", "");
        return response;
    }

    private ClientResponse doRestPost(String urlResource, String jsonString) throws Exception {
        ClientResponse response = dispatchRestRequest(urlResource, "post", jsonString);
        return response;
    }

    private ClientResponse dispatchRestRequest(String urlResource, String requestMethod, String xinfo) throws Exception {
        String serverUrl = BaseUtils.getProperty("env.jira.server.url");
        log.debug("dispatch request method is:[{}], resource requested is:[{}]", new Object[]{requestMethod, serverUrl + urlResource});
        URI uri = new URI(serverUrl + urlResource);
        WebResource webResource = getJerseyClient(uri);
        ClientResponse response = null;
        if (requestMethod.equalsIgnoreCase("get")) {
            response = webResource.header("Authorization", getBasicAuthorizationHeader(BaseUtils.getProperty("env.jira.security.username"), BaseUtils.getProperty("env.jira.security.password"))).type("application/json").accept("application/json").get(ClientResponse.class);
        }
        if (requestMethod.equalsIgnoreCase("post") && !xinfo.isEmpty()) {
            response = webResource.header("Authorization", getBasicAuthorizationHeader(BaseUtils.getProperty("env.jira.security.username"), BaseUtils.getProperty("env.jira.security.password"))).type("application/json").accept("application/json").put(ClientResponse.class, xinfo);
        }
        if (response != null && response.getStatus() == 401) {
            log.warn("Authentication error was encountered");
            response.close();
            throw new Exception("Invalid Username or Password: [" + response.getStatus() + "] ");
        }
        return response;
    }

    private String getBasicAuthorizationHeader(String username, String password) {
        return BASIC + " " + encodeCredentials(username, password);
    }

    private String encodeCredentials(String username, String password) {
        String cred = username + ":" + password;
        String encodedValue;
        byte[] encodedBytes = Base64.encodeBase64(cred.getBytes());
        encodedValue = new String(encodedBytes);
        log.debug("encodedBytes " + new String(encodedBytes));

        byte[] decodedBytes = Base64.decodeBase64(encodedBytes);
        log.debug("decodedBytes " + new String(decodedBytes));

        return encodedValue;
    }

    private WebResource getJerseyClient(URI uri) {
        com.sun.jersey.api.client.WebResource webResource;
        if (jerseyClient == null) {
            jerseyClient = Client.create();
            jerseyClient.setReadTimeout(BaseUtils.getIntProperty("env.jira.read.timeout.millis", 60000));
            jerseyClient.setConnectTimeout(BaseUtils.getIntProperty("env.jira.connect.timeout.millis", 5000));
            webResource = jerseyClient.resource(uri);
        } else {
            webResource = jerseyClient.resource(uri);
        }
        return webResource;
    }

    private Transition getTransitionByName(Iterable<Transition> transitions, String transitionName, String issueType) {
        for (Transition transition : transitions) {
            log.debug("Transition:[{}] found for this issue type[{}]", transition.getName(), issueType);
        }
        for (Transition transition : transitions) {
            if (transition.getName().equals(transitionName)) {
                return transition;
            }
        }
        return null;
    }

    private String getSqlStatusString(List<String> listOfStatuses) {
        log.debug("In getSqlStatusString");
        String retString = "";
        try {
            StringBuilder tmp = new StringBuilder("(");
            int cnt = 0;
            int max = listOfStatuses.size();
            Iterator<String> it = listOfStatuses.iterator();
            //building a string like: (status = 'open' OR status = 'closed' OR status = 'reopened' OR status = 'resolved' )
            while (it.hasNext()) {
                String status = it.next();
                cnt++;
                if (cnt < max) {
                    tmp.append("status = '");
                    tmp.append(status).append("'");
                    tmp.append(" OR ");
                } else {
                    tmp.append("status = '");
                    tmp.append(status).append("'");
                    tmp.append(" )");
                }
            }
            if (max > 0) {
                retString += tmp.toString();
            }

        } catch (Exception ex) {
        }
        log.debug("Finished getSqlStatusString");
        return retString;
    }

    private IssueList getAllWildCardIssuesFromCache(IssueQuery issueQuery, Set<String> issueIds, JiraRestClient connection) throws Exception {
        log.debug("In getAllWildCardIssuesFromCache");
        if (issueIds.isEmpty()) {
            log.debug("Finished getAllWildCardIssuesFromCache. No sales leads found");
            return new IssueList();
        }

        String wildCardChar = (issueQuery.getCustomerName() == null ? "notakey" : issueQuery.getCustomerName());
        IssueList returnList = null;
        IssueList issuesFromCache;

        if (wildCardChar.length() == 3) {

            returnList = new IssueList();
            issuesFromCache = wildCardIssuesCache.get(wildCardChar);
            if (issuesFromCache == null) {
                log.debug("Nothing was found on cache so will call jira, then save to cache");
                for (String issueId : issueIds) {
                    populateIssueList(returnList.getIssueList(), issueQuery, issueId, connection);
                }
                //Only save in cache if something was found
                if (returnList.getIssueList().size() > 1) {
                    wildCardIssuesCache.put(wildCardChar, returnList);
                    log.debug("Saved Issues to cache using key [{}], number of issues saved[{}]", wildCardChar, returnList.getIssueList().size());
                }

            } else {
                //Found something on the cache
                log.debug("Cache exist so will search on it and do jira calls if neccessary");
                IssueList tmpIssues = new IssueList();
                boolean mustUpdateCachedItem = false;
                Set<String> issueIdsInCachedItem = new HashSet<>();

                for (String issueId : issueIds) {
                    for (Issue issue : issuesFromCache.getIssueList()) {
                        if (issue.getID().equalsIgnoreCase(issueId)) {
                            tmpIssues.getIssueList().add(issue);
                            issueIdsInCachedItem.add(issueId);
                        }
                    }
                }
                //Searched in cache couldnt find this issue[e.g UGSD-4102], will search in JIRA
                for (String issueId : issueIds) {
                    if (!issueIdsInCachedItem.contains(issueId)) {
                        populateIssueList(tmpIssues.getIssueList(), issueQuery, issueId, connection);
                    }
                }

                //A new issue[e.g UGSD-5835] was added to tmpCache
                IssueList tmpCache = issuesFromCache;
                int cachedItemSizeBeforeUpdate = issuesFromCache.getIssueList().size();

                for (String issueId : issueIds) {
                    if (!issueIdsInCachedItem.contains(issueId)) {
                        for (Issue issue : tmpIssues.getIssueList()) {
                            if (issue.getID().equalsIgnoreCase(issueId)) {
                                tmpCache.getIssueList().add(issue);
                                mustUpdateCachedItem = true;
                            }
                        }
                    }
                }
                int cachedItemSizeAfterUpdate = tmpCache.getIssueList().size();
                if (mustUpdateCachedItem) {
                    wildCardIssuesCache.put(wildCardChar, tmpCache);
                    log.debug("Cached item was updated: previous size [{}], current size [{}], key[{}]", new Object[]{cachedItemSizeBeforeUpdate, cachedItemSizeAfterUpdate, wildCardChar});
                }

                returnList = tmpIssues;
            }

        } else if (wildCardChar.length() > 3) {

            String keyFromWildCard = wildCardChar.substring(0, 3);
            issuesFromCache = wildCardIssuesCache.get(keyFromWildCard);
            returnList = new IssueList();

            //Cache has probably expired, will search in JIRA
            if (issuesFromCache == null) {
                for (String issueId : issueIds) {
                    populateIssueList(returnList.getIssueList(), issueQuery, issueId, connection);
                }
                if (returnList.getIssueList().size() > 1) {
                    wildCardIssuesCache.put(keyFromWildCard, returnList);
                    log.debug("Saved Issues to cache using key [{}], number of issues saved[{}]", keyFromWildCard, returnList.getIssueList().size());
                }

            } else {
                //Found something on the cache, will first search on it before calling JIRA
                log.debug("Cache exist so will search on it and do jira calls if neccessary");
                IssueList tmpIssues = new IssueList();
                Set<String> issuesInCache = new HashSet<>();
                boolean mustUpdateCachedItem = false;

                for (String issueId : issueIds) {
                    for (Issue issue : issuesFromCache.getIssueList()) {
                        if (issueId.equalsIgnoreCase(issue.getID())) {
                            tmpIssues.getIssueList().add(issue);
                            issuesInCache.add(issueId);
                        }
                    }
                }
                for (String issueId : issueIds) {
                    if (!issuesInCache.contains(issueId)) {
                        populateIssueList(tmpIssues.getIssueList(), issueQuery, issueId, connection);
                    }
                }
                //add the new results to cache if exist
                IssueList tmpCache = issuesFromCache;
                int cachedItemSizeBeforeUpdate = issuesFromCache.getIssueList().size();

                for (String issueId : issueIds) {
                    if (!issuesInCache.contains(issueId)) {
                        for (Issue is : tmpIssues.getIssueList()) {
                            if (is.getID().equalsIgnoreCase(issueId)) {
                                tmpCache.getIssueList().add(is);
                                mustUpdateCachedItem = true;
                            }
                        }
                    }
                }
                int cachedItemSizeAfterUpdate = tmpCache.getIssueList().size();
                if (mustUpdateCachedItem) {
                    wildCardIssuesCache.put(keyFromWildCard, tmpCache);
                    log.debug("Cached item was updated: previous size [{}], current size [{}], key[{}]", new Object[]{cachedItemSizeBeforeUpdate, cachedItemSizeAfterUpdate, keyFromWildCard});
                }
                returnList = tmpIssues;
            }
        }
        log.debug("Finished getAllWildCardIssuesFromCache");
        return returnList;
    }

    private static final Lock emfLock = new ReentrantLock();

    public Set<String> getIssueKeysFromDB(String projectKey, String cfname, String customfieldValue, int max, int buildNumber) throws Exception {
        log.debug("In getIssueKeysFromDB");
        Set<String> issueIds = new HashSet();
        EntityManager em = null;
        try {
            if (emf == null) {
                try {
                    if (!emfLock.tryLock()) {
                        throw new Exception("Connection to Jira DB not established yet");
                    }
                    // Do lazy EMF creation to help speed up deploys so we dont wait for Jira connection
                    log.debug("EMF is null. Getting EntityManagerFactory for JiraDB");
                    emf = JPAUtils.getEMF("TTPU_RL");
                    log.debug("Got EMF to JiraDB");
                } finally {
                    emfLock.unlock();
                }
            }
            String sqlString = "";
            if (projectKey == null || projectKey.isEmpty()) {
                projectKey = BaseUtils.getProperty("env.jira.saleslead.project.key");
            }
            String prKey = "";

            if (buildNumber >= 6) {
                prKey = projectKey;
                sqlString = "select jiraissue.issuenum from customfieldvalue JOIN jiraissue ON customfieldvalue.ISSUE = jiraissue.ID where jiraissue.PROJECT = (select ID from project where project.pkey = ?) AND CUSTOMFIELD = (select ID from customfield where cfname = ?) and jiraissue.issuestatus = ? and jiraissue.CREATED >= ? and customfieldvalue.STRINGVALUE like ? LIMIT ?";
            }
            if (buildNumber >= 5 && buildNumber < 6) {
                prKey = projectKey + "%";
                sqlString = "select jiraissue.pkey from customfieldvalue JOIN jiraissue ON customfieldvalue.ISSUE = jiraissue.ID where jiraissue.pkey like ? AND CUSTOMFIELD = (select ID from customfield where cfname = ?) and jiraissue.issuestatus = ? and jiraissue.CREATED >= ? and customfieldvalue.STRINGVALUE like ? LIMIT ?";
            }

            if (sqlString.isEmpty()) {
                log.warn("SQL string is not set for this jira version: {}", buildNumber);
                sqlString = "select jiraissue.issuenum from customfieldvalue JOIN jiraissue ON customfieldvalue.ISSUE = jiraissue.ID where jiraissue.PROJECT = (select ID from project where project.pkey = ?) AND CUSTOMFIELD = (select ID from customfield where cfname = ?) and jiraissue.issuestatus = ? and jiraissue.CREATED >= ? and customfieldvalue.STRINGVALUE like ? LIMIT ?";
            }

            /* Compute dateFrom (3 months ago) */
            Calendar rightNow = Calendar.getInstance();
            rightNow.add(Calendar.MONTH, -3);
            Date dateFrom = rightNow.getTime();

            em = JPAUtils.getEM(emf);
            Query q = em.createNativeQuery(sqlString);
            q.setParameter(1, prKey);
            q.setParameter(2, cfname);
            q.setParameter(3, 1);//1=Open
            q.setParameter(4, dateFrom);
            q.setParameter(5, "%" + customfieldValue + "%");
            q.setParameter(6, max);
            log.debug("DB search returned [{}] issue ids for wild card [{}] and the limit was [{}]", new Object[]{q.getResultList().size(), customfieldValue, max});

            if (buildNumber <= 5 && buildNumber < 6) {
                for (String issId : (List<String>) q.getResultList()) {
                    issueIds.add(issId);
                }
            } else {
                for (java.math.BigDecimal issId : (List<java.math.BigDecimal>) q.getResultList()) {
                    issueIds.add(projectKey + "-" + issId.toBigInteger());
                }
            }
            log.debug("Finished getIssueKeysFromDB");
        } finally {
            JPAUtils.closeEM(em);
        }
        return issueIds;
    }

    private String getJiraFieldIdFromCache(String fieldNameToSearch) throws Exception {
        log.debug("In getJiraFieldIdFromCache");

        String jiraField = jiraFieldsCache.get(fieldNameToSearch);

        if (jiraField == null) {
            Iterable<CimProject> metadataProjects = getIterableCreateIssueMetadataOptions();
            for (CimProject p : metadataProjects) {
                for (CimIssueType t : p.getIssueTypes()) {
                    for (Map.Entry<String, CimFieldInfo> entry : t.getFields().entrySet()) {
                        CimFieldInfo fieldInfo = entry.getValue();
                        String fieldId = fieldInfo.getId();
                        String fieldName = fieldInfo.getName();
                        String tmpValue = jiraFieldsCache.get(fieldName);
                        if (tmpValue != null) {
                            continue;
                        }
                        jiraFieldsCache.put(fieldName, fieldId);
                        log.debug("Added JIRA field [{}] with id [{}] to the fields cache", fieldName, fieldId);
                    }
                }
            }
            jiraField = jiraFieldsCache.get(fieldNameToSearch);
            if (jiraField == null) {
                jiraField = "_n_";
            }
        } else if (log.isDebugEnabled()) {
            log.debug("Found value[{}] on the cache, cache size is {}", jiraField, jiraFieldsCache.size());
        }
        log.debug("Finished getJiraFieldIdFromCache");
        return jiraField;
    }

    private void populateMetaDataCache() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                while (!success) {

                    try {
                        if (!metadataLock.tryLock()) {
                            log.debug("Another thread is already populating the meta data cache. Wont try to do the same");
                            return;
                        }
                        long start = System.currentTimeMillis();

                        JiraRestClient connection = null;
                        try {
                            connection = openRestConnection();
                            log.warn("Calling rest API for meta data");
                            Iterable<CimProject> iterableCreateIssueMetadataOptionstmp = connection.getIssueClient().getCreateIssueMetadata(new GetCreateIssueMetadataOptionsBuilder().withExpandedIssueTypesFields().build()).claim();
                            if (iterableCreateIssueMetadataOptionstmp != null && !((List) iterableCreateIssueMetadataOptionstmp).isEmpty()) {
                                success = true;
                                iterableCreateIssueMetadataOptions = iterableCreateIssueMetadataOptionstmp;
                            } else {
                                log.warn("For some reson the Jira Meta Data could not be retrieved. Will try again");
                            }
                        } finally {
                            metadataLock.unlock();
                            closeRestConnection(connection);
                            long end = System.currentTimeMillis();
                            log.warn("Jira Meta Data call took [{}]ms", end - start);
                        }
                    } catch (Throwable e) {
                        log.warn("Failed to populate Jira Meta Data Cache [{}]", e.toString());
                        log.warn("Error: ", e);
                        Throwable cause = e.getCause();
                        while (cause != null && cause.getCause() != null) {
                            cause = cause.getCause();
                        }
                        if (cause == null) {
                            cause = e;
                        }
                        if (cause instanceof java.util.concurrent.TimeoutException || cause instanceof java.net.SocketTimeoutException) {
                            if (iterableCreateIssueMetadataOptions != null && !((List) iterableCreateIssueMetadataOptions).isEmpty()) {
                                log.warn("Re-using currently available (and probably stale) create issue metadata instead of retrying");
                                success = true;
                            } else {
                                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "TT", "Thread TT-MetaData-Fetcher failed to populate Jira Meta Data Cache, reason: " + e.toString());
                            }
                        } else {
                            /*Report this error: will help to quickly spot the root cause, while clients to TT get 'Jira Meta Data is not available' */
                            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "TT", "Thread TT-MetaData-Fetcher failed to populate Jira Meta Data Cache, reason: " + e.toString());
                        }
                    }
                    if (!success) {
                        log.warn("Sleeping for 2 minutes before trying again");
                        Utils.sleep(BaseUtils.getIntProperty("env.tt.metadata.failed.wait.sec", 120) * 1000);
                    }
                }
                log.warn("Successfully populated Jira Meta Data cache");
            }
        });
        t.setName("Smile-TT-MetaData-Fetcher");
        log.debug("Starting thread to fetch Jira Meta Data");
        t.start();
        log.debug("Started thread to fetch Jira Meta Data");
    }

    private Iterable<CimProject> getIterableCreateIssueMetadataOptions() throws Exception {
        if (((List) iterableCreateIssueMetadataOptions).isEmpty()) {
            throw new Exception("Jira Meta Data is not available");
        }
        return iterableCreateIssueMetadataOptions;
    }

    /**
     * Fields of TT_USER_FIELD type are typically question->answer format which
     * are dynamically generated. This method is mapping the question to thier
     * respective answers based on question keys (e.g.
     * TT_USER_FIELD_Description0_q) and answer keys (e.g.
     * TT_USER_FIELD_Description0)
     */
    private String buildQuestionAnswerString(String fieldName, List<JiraField> qFields) {
        StringBuilder sb = new StringBuilder();
        List<JiraField> qust = qFields;
        int cnt = 1;
        for (JiraField jf : qFields) {
            if (!jf.getFieldName().contains(fieldName) || jf.getFieldType().equals("TT_FIXED_FIELD")) {
                continue;
            }
            //When modifying the token '_q' please also look at the NewIssue object that TT receives from SCA
            if (jf.getFieldName().endsWith("_q")) {
                String questionKey = jf.getFieldName();
                String questionValue = jf.getFieldValue();
                String responseKey = questionKey.substring(0, (questionKey.lastIndexOf("_")));

                for (JiraField jField : qust) {
                    if (!jField.getFieldName().equals(responseKey) || jField.getFieldType().equals("TT_FIXED_FIELD")) {
                        continue;
                    }
                    sb.append(cnt).append(".").append(questionValue).append("? ").append(jField.getFieldValue()).append(" ");
                    cnt++;
                }
            }
        }
        return sb.toString();
    }

    private void populateIssueList(List issueList, IssueQuery issueQuery, String issueId, JiraRestClient connection) throws Exception {
        log.debug("In populateIssueList1");
        log.debug("Calling Jira to get issue [{}]", issueId);
        com.atlassian.jira.rest.client.api.domain.Issue issue = connection.getIssueClient().getIssue(issueId).claim();
        log.debug("Finished calling Jira to get issue [{}]", issueId);
        populateIssueList(issueList, issueQuery, issue);
        log.debug("Finished populateIssueList1");
    }

    private void populateIssueList(List issueList, IssueQuery issueQuery, com.atlassian.jira.rest.client.api.domain.Issue issue) throws Exception {
        log.debug("In populateIssueList2");
        String salesLeadPK = (issueQuery.getSalesLeadPK() == null ? "" : issueQuery.getSalesLeadPK());
        Issue newIssue = new Issue();

        newIssue.setJiraFields(new JiraFields());
        List<JiraField> jfList = new ArrayList<>();

        newIssue.setProject(issue.getProject().getName());
        newIssue.setDescription(issue.getDescription() == null ? "Non" : issue.getDescription());
        newIssue.setSummary(issue.getSummary());
        newIssue.setPriority(issue.getPriority().getName());
        newIssue.setCreated(Utils.getDateAsXMLGregorianCalendar(issue.getCreationDate().toDate()));

        newIssue.setAssignee(issue.getAssignee() == null ? "Unassigned" : issue.getAssignee().getDisplayName());
        newIssue.setResolution(issue.getResolution() == null ? "" : issue.getResolution().getName());
        newIssue.setDueDate(issue.getDueDate() == null ? Utils.getDateAsXMLGregorianCalendar(new Date()) : Utils.getDateAsXMLGregorianCalendar(issue.getDueDate().toDate()));
        newIssue.setID(issue.getKey());

        newIssue.setIssueType(issue.getIssueType().getName());
        newIssue.setReporter(issue.getReporter().getDisplayName());
        newIssue.setUpdated(Utils.getDateAsXMLGregorianCalendar(issue.getUpdateDate().toDate()));
        newIssue.setStatus(issue.getStatus().getName());

        final Iterable<CimProject> metadataProjects = getIterableCreateIssueMetadataOptions();

        CimProject project = null;
        for (CimProject p : metadataProjects) {
            if (p.getKey().equals(issue.getProject().getKey())) {
                project = p;
                break;
            }
        }
        CimIssueType issueType = null;
        for (CimIssueType t : project.getIssueTypes()) {
            if (t.getName().equals(issue.getIssueType().getName())) {
                issueType = t;
                break;
            }
        }
        
        String ccProject = BaseUtils.getProperty("env.jira.customer.care.project.key");
        if (!salesLeadPK.isEmpty()) {
            //Logic for salesLead issues
            inflateSalesLeadIssueWithPreDefinedTTFields(issue, jfList, issueType.getFields().entrySet());

        } else if (issue.getProject().getKey().equals(ccProject)) {
            //Logic for customer care issues
            inflateCustomerCareIssueWithPreDefinedTTFields(issue, jfList, issueType.getFields().entrySet());
        }
        newIssue.getJiraFields().getJiraField().addAll(jfList);
        int resultSize = newIssue.getJiraFields().getJiraField().size();
        newIssue.getJiraFields().setTotalJiraFields(resultSize);
        issueList.add(newIssue);
        log.debug("Finished populateIssueList2");
    }

    private void inflateCustomerCareIssueWithPreDefinedTTFields(com.atlassian.jira.rest.client.api.domain.Issue issue, List<JiraField> jfList, Set<Map.Entry<String, CimFieldInfo>> entrySet) {

        log.debug("ENTERING inflateCustomerCareIssueWithPreDefinedTTFields");

        //sc=Smile Customer, CFID=CustomField ID
        String scLocationCFID = null;
        String scEmailCFID = null;
        String scPhoneCFID = null;
        String scNameCFID = null;
        String categoryCFID = null;
        String subCategoryCFID = null;
        String custIdCFID = null;
        String incidentChannelCFID = null;

        String issueType = issue.getIssueType().getName();
        String projectKey = issue.getProject().getKey();

        for (Map.Entry<String, CimFieldInfo> entry : entrySet) {

            CimFieldInfo fieldInfo = entry.getValue();
            String fieldId = fieldInfo.getId();
            String fieldName = fieldInfo.getName();

            if (fieldName.equals(CC_SMILE_CUSTOMER_LOCATION)) {
                scLocationCFID = fieldId;
            }
            if (fieldName.equals(CC_SMILE_CUSTOMER_EMAIL)) {
                scEmailCFID = fieldId;
            }
            if (fieldName.equals(CC_SMILE_CUSTOMER_PHONE)) {
                scPhoneCFID = fieldId;
            }
            if (fieldName.equals(CC_SMILE_CUSTOMER_NAME)) {
                scNameCFID = fieldId;
            }
            if (fieldName.equalsIgnoreCase(CC_CATEGORY)) {
                categoryCFID = fieldId;
            }
            if (fieldName.equals(CC_SUB_CATEGORY)) {
                subCategoryCFID = fieldId;
            }
            if (fieldName.equals(CC_SMILE_CUSTOMER_ID)) {
                custIdCFID = fieldId;
            }
            if (fieldName.equals(CC_INCIDENT_CHANNEL)) {
                incidentChannelCFID = fieldId;
            }

        }

        JiraField jf;

        try {
            jf = new JiraField();
            jf.setFieldName(CC_SMILE_CUSTOMER_ID);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue(issue.getField(custIdCFID).getValue() == null ? "0" : issue.getField(custIdCFID).getValue().toString());
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, CC_SMILE_CUSTOMER_ID});
        }

        try {
            JSONObject category = (JSONObject) issue.getField(categoryCFID).getValue();
            jf = new JiraField();
            jf.setFieldName(CC_CATEGORY);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue(category == null ? "" : (String) category.getString("value"));
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, CC_CATEGORY});
        }

        try {
            JSONObject subCategory = (JSONObject) issue.getField(subCategoryCFID).getValue();
            jf = new JiraField();
            jf.setFieldName(CC_SUB_CATEGORY);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue(subCategory == null ? "" : (String) subCategory.getString("value"));
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, CC_SUB_CATEGORY});
        }

        try {
            //Field{id=customfield_10601, name=Smile Customer Location, type=null, value=null}
            jf = new JiraField();
            jf.setFieldName(CC_SMILE_CUSTOMER_EMAIL);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue(issue.getField(scEmailCFID).getValue() == null ? "" : issue.getField(scEmailCFID).getValue().toString());
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, CC_SMILE_CUSTOMER_EMAIL});
        }

        try {
            jf = new JiraField();
            jf.setFieldName(CC_SMILE_CUSTOMER_LOCATION);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue(issue.getField(scLocationCFID).getValue() == null ? "" : issue.getField(scLocationCFID).getValue().toString());
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, CC_SMILE_CUSTOMER_LOCATION});
        }

        try {
            jf = new JiraField();
            jf.setFieldName(CC_SMILE_CUSTOMER_NAME);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue(issue.getField(scNameCFID).getValue() == null ? "" : issue.getField(scNameCFID).getValue().toString());
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, CC_SMILE_CUSTOMER_NAME});
        }

        try {
            jf = new JiraField();
            jf.setFieldName(CC_SMILE_CUSTOMER_PHONE);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue(issue.getField(scPhoneCFID).getValue() == null ? "" : issue.getField(scPhoneCFID).getValue().toString());
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, CC_SMILE_CUSTOMER_PHONE});
        }

        try {
            JSONObject incidentChannel = (JSONObject) issue.getField(incidentChannelCFID).getValue();
            jf = new JiraField();
            jf.setFieldName(CC_INCIDENT_CHANNEL);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue((String) incidentChannel.getString("value") == null ? "" : (String) incidentChannel.getString("value"));
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, CC_INCIDENT_CHANNEL});
        }

        log.debug("EXITING inflateCustomerCareIssueWithPreDefinedTTFields3");
    }

    private void inflateSalesLeadIssueWithPreDefinedTTFields(com.atlassian.jira.rest.client.api.domain.Issue issue, List<JiraField> jfList, Set<Map.Entry<String, CimFieldInfo>> entrySet) throws Exception {
        log.debug("In inflateSalesLeadIssueWithPreDefinedTTFields");
        //sl=Sales Lead
        String slCustomerNameCFID = "";
        String slCustPhoneNoCFID = "";
        String slCustomerEmailCFID = "";
        String slAddressLine1CFID = "";
        String slAddressLine2CFID = "";
        String slAddTownCityCFID = "";
        String slLeadStateCFID = "";

        String issueType = issue.getIssueType().getName();
        String projectKey = issue.getProject().getKey();

        for (Map.Entry<String, CimFieldInfo> entry : entrySet) {

            CimFieldInfo fieldInfo = entry.getValue();
            String fieldId = fieldInfo.getId();
            String fieldName = fieldInfo.getName();

            if (fieldName.equals(SL_CUSTOMER_FIRST_NAME)) {
                slCustomerNameCFID = fieldId;
            }
            if (fieldName.equals(SL_ALTERNATIVE_CONTACT1)) {
                slCustPhoneNoCFID = fieldId;
            }
            if (fieldName.equals(SL_CUSTOMER_EMAIL)) {
                slCustomerEmailCFID = fieldId;
            }
            if (fieldName.equals(SL_ADDRESS_LINE1)) {
                slAddressLine1CFID = fieldId;
            }
            if (fieldName.equals(SL_ADDRESS_LINE2)) {
                slAddressLine2CFID = fieldId;
            }
            if (fieldName.equals(SL_ADDRESS_TOWN)) {
                slAddTownCityCFID = fieldId;
            }
            if (fieldName.equals(SL_LEAD_STATE)) {
                slLeadStateCFID = fieldId;
            }

        }

        //issue.getField => Field{id=customfield_10601, name=Smile Customer Location, type=null, value=null}
        JiraField jf;
        try {
            jf = new JiraField();
            jf.setFieldName(SL_CUSTOMER_FIRST_NAME);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue(issue.getField(slCustomerNameCFID).getValue() == null ? "" : issue.getField(slCustomerNameCFID).getValue().toString());
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, SL_CUSTOMER_FIRST_NAME});
        }
        try {
            jf = new JiraField();
            jf.setFieldName(SL_ALTERNATIVE_CONTACT1);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue(issue.getField(slCustPhoneNoCFID).getValue() == null ? "" : issue.getField(slCustPhoneNoCFID).getValue().toString());
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, SL_ALTERNATIVE_CONTACT1});
        }
        try {
            jf = new JiraField();
            jf.setFieldName(SL_CUSTOMER_EMAIL);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue(issue.getField(slCustomerEmailCFID).getValue() == null ? "" : issue.getField(slCustomerEmailCFID).getValue().toString());
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, SL_CUSTOMER_EMAIL});
        }
        try {
            jf = new JiraField();
            jf.setFieldName(SL_ADDRESS_LINE1);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue(issue.getField(slAddressLine1CFID).getValue() == null ? "" : issue.getField(slAddressLine1CFID).getValue().toString());
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, SL_ADDRESS_LINE1});
        }
        try {
            jf = new JiraField();
            jf.setFieldName(SL_ADDRESS_LINE2);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue(issue.getField(slAddressLine2CFID).getValue() == null ? "" : issue.getField(slAddressLine2CFID).getValue().toString());
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, SL_ADDRESS_LINE2});
        }
        try {
            JSONObject slAddTownCity = (JSONObject) issue.getField(slAddTownCityCFID).getValue();
            jf = new JiraField();
            jf.setFieldName(SL_ADDRESS_TOWN);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue((String) slAddTownCity.getString("value") == null ? "" : (String) slAddTownCity.getString("value"));
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, SL_ADDRESS_TOWN});
        }
        try {
            JSONObject slLeadState = (JSONObject) issue.getField(slLeadStateCFID).getValue();
            jf = new JiraField();
            jf.setFieldName(SL_LEAD_STATE);
            jf.setFieldType("TT_FIXED_FIELD");
            jf.setFieldValue((String) slLeadState.getString("value") == null ? "" : (String) slLeadState.getString("value"));
            jfList.add(jf);
        } catch (Exception ex) {
            log.warn("Issue type [{}] for project [{}] does not support this field [{}], will ignore and continue", new Object[]{issueType, projectKey, SL_LEAD_STATE});
        }

        log.debug("Finished inflateSalesLeadIssueWithPreDefinedTTFields");
    }

    /*
     * When issuing queries with strings as dates, we must convert the date to JHB time as thats where JIRA is located
     */
    private String getLDNDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        formatter.setTimeZone(TimeZone.getTimeZone(BaseUtils.getProperty("env.jira.server.timezone", "GMT+2:00")));
        return formatter.format(date);
    }

    private void closeRestConnection(JiraRestClient connection) {
        log.debug("Reaping rest client");
        try {
            if (connection != null) {
                connection.close();
                log.debug("Rest client connection closed successfully");
            }
        } catch (IOException ex) {
            log.warn("Error occured trying to close rest client: {}", ex.toString());
        }
    }

    private JiraRestClient openRestConnection() throws Exception {
        log.debug("Opening rest connection to Jira server");
        JiraRestClient restClient = null;
        try {
            log.debug("About to call create on factory");
            AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
            URI jiraServerUri = new URI(BaseUtils.getProperty("env.jira.server.url"));
            if (httpClient == null) {
                log.debug("Creating httpclient");
                AuthenticationHandler myAuthenticationHandler = new TTBasicHttpAuthenticationHandlerImpl(BaseUtils.getProperty("env.jira.security.username"), BaseUtils.getProperty("env.jira.security.password"));
                httpClient = new TTAsynchronousHttpClientFactory().createClient(jiraServerUri, myAuthenticationHandler);
                log.debug("Created httpclient [{}][{}]", httpClient, httpClient.hashCode());
            }
            restClient = factory.create(jiraServerUri, httpClient);
            log.debug("Finished creating rest client");
        } catch (Exception ex) {
            throw processError(TTError.class, ex);
        }
        log.debug("Finished opening rest connection to Jira server");
        return restClient;
    }

    private com.atlassian.jira.rest.client.api.domain.BasicUser getBasicUser(String userName, JiraRestClient connection) {
        BasicUser bUser = null;
        try {
            com.atlassian.jira.rest.client.api.domain.User jUser = connection.getUserClient().getUser(userName).claim();
            bUser = new BasicUser(jUser.getSelf(), jUser.getName(), jUser.getDisplayName());
        } catch (Exception e) {
            com.atlassian.jira.rest.client.api.domain.User jUser;
            try {
                String usr = BaseUtils.getProperty("env.jira.security.username");
                jUser = connection.getUserClient().getUser(usr).claim();
                bUser = new BasicUser(jUser.getSelf(), jUser.getName(), jUser.getDisplayName());
            } catch (Exception ex) {
            }
        }
        return bUser;
    }

}
