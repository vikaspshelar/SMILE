/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.hwf.activiti;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.CallersRequestContext;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.PurchaseUnitCreditRequest;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class ActivitiSCADelegate implements JavaDelegate {

    private Expression method;
    private Expression sql;
    private EntityManagerFactory emf = null;
    private static final Logger log = LoggerFactory.getLogger(ActivitiSCADelegate.class);
    private final String PAYMENT_METHOD_GIFT = "Cash";
    private final String PAYMENT_METHOD_STAFF = "Credit Account";

    public ActivitiSCADelegate() {
        emf = JPAUtils.getEMF("HWFPU_RL");
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        JPAUtils.closeEMF(emf);
    }

    @Override
    public void execute(DelegateExecution de) throws Exception {
        String sMethod = (String) method.getValue(de);
        log.debug("ActivitiSCADelegate has been called and method is [{}]", sMethod);

        try {
            Map<String, Object> processVariables = de.getVariables();
            if (log.isDebugEnabled()) {
                for (String varName : processVariables.keySet()) {
                    String varValue = processVariables.get(varName).toString();
                    String varType = processVariables.get(varName).getClass().getName();
                    log.debug("ActivitiSCADelegate sees variable [{}] is type [{}] and value [{}]", new Object[]{varName, varType, varValue});
                }
            }
            if (sMethod.equals("provisionUnitCredit")) {
                provisionUnitCredit(processVariables);
            } else if (sMethod.equals("getAccountOwner")) {
                de.setVariable("AccountHolderName", getAccountOwner(processVariables));
            } else if (sMethod.equals("RunSQL")) {
                String sSQL = (String) sql.getValue(de);
                long sqlResult = runSQL(sSQL);
                de.setVariable("SQLResult", sqlResult);
            } else if (sMethod.equals("findApproversEmail")) {
                de.setVariable("ApproverEmail", getApproverEmailByName(processVariables));
            } else if (sMethod.equals("modifyCustomerPermission")) {
                modifyCustomerPermission(processVariables);
            } else if (sMethod.equals("getAssignee")) {
                String val = de.getEngineServices().getTaskService().createTaskQuery().processInstanceId(de.getProcessInstanceId()).active().singleResult().getAssignee();
                de.setVariable("Assignee", val);
            }

        } catch (Exception e) {
            log.warn("Error in ActivitiSCADelegate", e);
            throw e;
        }
        log.debug("Finished ActivitiSCADelegate for method [{}]", sMethod);
    }

    public void setMethod(Expression m) {
        method = m;
    }

    public void setSql(Expression m) {
        sql = m;
    }

    private void provisionUnitCredit(Map<String, Object> processVariables) throws Exception {
        List<String> roles = new ArrayList();
        roles.add("Administrator");

        CallersRequestContext ctx = new CallersRequestContext((String) processVariables.get("Assignee"), BaseUtils.getIPAddress(), 0, roles);
        String accountId = (String) processVariables.get("AccountId");
        PurchaseUnitCreditRequest pucr = new PurchaseUnitCreditRequest();
        pucr.setAccountId(Long.parseLong(accountId.trim()));
        pucr.setNumberToPurchase(1);
        pucr.setProductInstanceId(0);
        pucr.setUnitCreditName((String) processVariables.get("UnitCreditName"));

        SCAWrapper.getUserSpecificInstance().setThreadsRequestContext(ctx);
        try {
            SCAWrapper.getUserSpecificInstance().purchaseUnitCredit(pucr);
        } finally {
            SCAWrapper.removeThreadsRequestContext();
        }
    }

    private String getAccountOwner(Map<String, Object> processVariables) {
        String accountId = (String) processVariables.get("AccountId");
        Account acc = SCAWrapper.getAdminInstance().getAccount(Long.parseLong(accountId.trim()), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        String customerName = "";
        if (acc.getServiceInstances().size() > 0) {
            customerName = UserSpecificCachedDataHelper.getCustomerName(acc.getServiceInstances().get(0).getCustomerId());
        }
        return customerName;
    }

    private long runSQL(String sSQL) throws Exception {
        EntityManager em = null;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            Query q = em.createNativeQuery(sSQL);
            Object o = q.getSingleResult();
            long ret = Utils.getObjectAsLong(o, Long.MAX_VALUE);
            if (ret == Long.MAX_VALUE) {
                throw new Exception("Invalid result from SQL");
            }
            return ret;
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }
    }

    private String getApproverEmailByName(Map<String, Object> processVariables) {
        CustomerQuery cq = new CustomerQuery();
        String fullName[] = ((String) processVariables.get("ApproverEmail")).split(" ");

        cq.setFirstName(fullName[0]);
        cq.setLastName(fullName[1]);
        cq.setResultLimit(1);
        cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);

        Customer cust = SCAWrapper.getAdminInstance().getCustomer(cq);

        return cust.getEmailAddress();
    }

    private void modifyCustomerPermission(Map<String, Object> processVariables) {
        String ssoIdentity = ((String) processVariables.get("Username")).trim();
        String Permission = ((String) processVariables.get("Permission")).trim();
        String action = (String) processVariables.get("Action");

        CustomerQuery cq = new CustomerQuery();
        cq.setSSOIdentity(ssoIdentity);
        cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        cq.setResultLimit(1);

        List<String> newPerms = new ArrayList<>();
        try {
            log.debug("Getting Customer's current permissions");
            Customer tmp = SCAWrapper.getAdminInstance().getCustomer(cq);
            for (String perm : tmp.getSecurityGroups()) {
                if (perm.equalsIgnoreCase("Customer") || perm.contains("Promo")) {
                    log.debug("Current Permission [{}[", perm);
                    newPerms.add(perm);
                }
            }
            if (action.equals("Revoke")) {
                tmp.getSecurityGroups().remove(Permission);
            } else if (action.equals("Grant")) {
                newPerms.add(Permission);
                tmp.getSecurityGroups().clear();
                tmp.getSecurityGroups().addAll(newPerms);
            }
            SCAWrapper.getAdminInstance().modifyCustomer(tmp);
        } catch (Exception e) {
            throw new BpmnError("An error occured - [" + e.getMessage() + "]");
        }

    }
}
