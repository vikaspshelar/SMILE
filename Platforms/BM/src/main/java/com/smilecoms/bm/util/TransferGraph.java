/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.util;

import com.smilecoms.bm.db.model.AccountHistory;
import com.smilecoms.bm.util.AccountTransfer.CustomerData;
import com.smilecoms.bm.util.AccountTransfer.DeviceData;
import com.smilecoms.bm.util.AccountTransfer.SaleData;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Utils;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class TransferGraph {

    EntityManager em;

    public TransferGraph(EntityManager em) {
        this.em = em;
    }
    private static final Logger log = LoggerFactory.getLogger(TransferGraph.class);

    public String getTransferGraph(long rootAccountId, Date startDate, Date endDate, int recursions, String txTypeMatch, String stringMatch) {
        start = System.currentTimeMillis();
        StringBuilder graph = new StringBuilder();
        graph.append("digraph G_component_0 {graph [root=\"");
        graph.append(rootAccountId);
        graph.append("\"];");
        appendAccountsTransfers(graph, rootAccountId, startDate, endDate, recursions, txTypeMatch, stringMatch);
        return graph.append("}").toString();
    }
    private Set<Long> acountsAnalysed = new HashSet();
    long start;

    private void appendAccountsTransfers(StringBuilder graph, long accountId, Date startDate, Date endDate, int recursions, String txTypeMatch, String stringMatch) {
        log.debug("Recursions is [{}]. Start Date [{}] End Date [{}] Account Id [{}]", new Object[]{recursions, startDate, endDate, accountId});

        if (System.currentTimeMillis() - start > 30*60000) {
            log.warn("Not doing any more processing");
            return;
        }
        if (acountsAnalysed.contains(accountId)) {
            log.debug("Accounts transfers processed already");
            return;
        }
        acountsAnalysed.add(accountId);

        Query q = em.createNativeQuery("select * from account_history where account_id=? and start_date >= ? and start_date <= ? and TRANSACTION_TYPE like ?", AccountHistory.class);
        q.setParameter(1, accountId);
        q.setParameter(2, startDate);
        q.setParameter(3, endDate);
        String match = "txtype.tfr.debit." + txTypeMatch + "%";
        q.setParameter(4, match);
        log.debug("Match is [{}]", match);
        List<AccountHistory> transfersFromAccount = q.getResultList();

        log.debug("There are [{}] transfers from [{}]", new Object[]{transfersFromAccount.size(), accountId});
        for (AccountHistory transfer : transfersFromAccount) {
            if (System.currentTimeMillis() - start > 30*60000) {
                log.warn("Not doing any more processing");
                return;
            }
            log.debug("Looking at transfer id for graph insertion[{}]", transfer.getId());
            putTransferIntoGraph(graph, transfer, stringMatch);
        }
        if (recursions == 0) {
            return;
        }
        recursions--;
        for (AccountHistory transfer : transfersFromAccount) {
            log.debug("Looking at transfer id for recursion [{}]", transfer.getId());
            appendAccountsTransfers(graph, Long.parseLong(transfer.getDestination()), startDate, endDate, recursions, txTypeMatch, stringMatch);
        }

    }
    private final Set<Long> accountsAdded = new HashSet();

    private void putTransferIntoGraph(StringBuilder origGraph, AccountHistory transfer, String stringMatch) {
        AccountTransfer at = new AccountTransfer(em, transfer);
        StringBuilder graph = new StringBuilder();
        // From ACC
        if (!accountsAdded.contains(at.getFromAccountId())) {
            graph.append("\"");
            graph.append(at.getFromAccountId()).append("\" [label=\"");
            graph.append("Acc: ").append(at.getFromAccountId());
            if (at.getFromCustomer().name != null) {
                graph.append("\\n");
                graph.append("Name: ").append(at.getFromCustomer().name).append(" (Id:").append(at.getFromCustomer().customerProfileId).append(")");
                graph.append("\\n");
                graph.append("User Name: ").append(at.getFromCustomer().ssoIdentity);
                graph.append("\\n");
                graph.append("EMail: ").append(at.getFromCustomer().emailAddress);
                if (!at.getFromCustomer().roleInOrganisation.isEmpty()) {
                    graph.append("\\n");
                    graph.append("Role: ").append(at.getFromCustomer().roleInOrganisation);
                }
                if (!at.getFromCustomer().securityGroup.isEmpty()) {
                    graph.append("\\n");
                    graph.append("Permissions: ").append(at.getFromCustomer().securityGroup);
                }
                graph.append("\\n");
                graph.append("Created By: ").append(at.getFromCustomer().createdByName);
            } else {
                graph.append("\\nAccount Has No Owner");
            }
            graph.append("\", shape=oval, style=filled, color=green, fontsize=10];");
        }
        // To ACC
        if (!accountsAdded.contains(at.getToAccountId())) {
            graph.append("\"");
            graph.append(at.getToAccountId()).append("\" [label=\"");
            graph.append("Acc: ").append(at.getToAccountId());
            if (at.getToCustomer().name != null) {
                graph.append("\\n");
                graph.append("Name: ").append(at.getToCustomer().name).append(" (Id:").append(at.getToCustomer().customerProfileId).append(")");
                graph.append("\\n");
                graph.append("User Name: ").append(at.getToCustomer().ssoIdentity);
                graph.append("\\n");
                graph.append("EMail: ").append(at.getToCustomer().emailAddress);
                if (!at.getToCustomer().roleInOrganisation.isEmpty()) {
                    graph.append("\\n");
                    graph.append("Role: ").append(at.getToCustomer().roleInOrganisation);
                }
                if (!at.getToCustomer().securityGroup.isEmpty()) {
                    graph.append("\\n");
                    graph.append("Permissions: ").append(at.getToCustomer().securityGroup);
                }
                graph.append("\\n");
                graph.append("Created By: ").append(at.getToCustomer().createdByName);
            } else {
                graph.append("\\nAccount Has No Owner");
            }
            graph.append("\", shape=oval, style=filled, color=green, fontsize=10];");
        }
        // Transfer Line

        CustomerData byCustomer = at.getByCustomer();
        DeviceData byDevice = at.getByDevice();
        SaleData sale = at.getSale();

        graph.append("\"");
        graph.append(at.getFromAccountId());
        graph.append("\" -> \"");
        graph.append(at.getToAccountId());
        graph.append("\" [label=\"");

        graph.append(convertCentsToCurrencyLong(at.getTransferAmountCents()));
        graph.append("\\n");
        graph.append(formatDateLong(at.getTransferDateTime()));
        graph.append("\\n");
        graph.append("Logged In: ").append(at.getLoggedInUser()).append(" (Id:").append(at.getLoggedInUserCustomerProfileId()).append(")");
        graph.append("\\n");
        graph.append(at.getTransferType());
        graph.append("\\n(IP ");
        graph.append(at.getTransferredByIPAddress());
        graph.append(" :");
        if (byCustomer != null && byCustomer.name != null) {
            graph.append("\\n");
            graph.append("Device Owner: ").append(byCustomer.name).append(" (Id:").append(byCustomer.customerProfileId).append(")");
            graph.append("\\n");
            graph.append("User Name: ").append(byCustomer.ssoIdentity);
            if (byCustomer.roleInOrganisation != null && !byCustomer.roleInOrganisation.isEmpty()) {
                graph.append("\\n");
                graph.append("Role: ").append(byCustomer.roleInOrganisation);
            }
            if (byCustomer.securityGroup != null && !byCustomer.securityGroup.isEmpty()) {
                graph.append("\\n");
                graph.append("Permissions: ").append(byCustomer.securityGroup);
            }
        } else {
            graph.append("\\n");
            graph.append("Not Via LTE Session");
        }

        if (byDevice != null && byDevice.imeisv != null && !byDevice.imeisv.isEmpty()) {
            graph.append("\\n");
            graph.append("IMEI: ").append(byDevice.imeisv);
            graph.append("\\n");
            graph.append("IMSI: ").append(byDevice.imsi);
            graph.append("\\n");
            graph.append("Sector: ").append(byDevice.sectorName);
            graph.append("\\n");
            graph.append("GPS: ").append(byDevice.gps);
        }

        graph.append(")");

        if (sale != null && sale.saleId > 0) {
            graph.append("\\n(Sale ");
            graph.append(sale.saleId);
            graph.append(" :");
            graph.append("\\n");
            graph.append(sale.paymentType);
            graph.append("\\n");
            graph.append("Sales Person: ").append(sale.salesPersonName).append(" (Id:").append(sale.salesPersonCustomerProfileId).append(")");
            graph.append("\\n");
            graph.append("User Name: ").append(at.getToCustomer().ssoIdentity);
            graph.append("\\n");
            graph.append("Cancelled: ").append(sale.wasSaleCancelled);
            graph.append(")");
        }
        if (at.isScheduled()) {
            graph.append("\\n");
            graph.append("Scheduled");
        }
        graph.append("\", color=red, arrowhead=normal, fontsize=10];");
        
        if (stringMatch == null || stringMatch.isEmpty() || Utils.matchesWithPatternCache(graph.toString(), stringMatch)) {
            origGraph.append(graph);
            accountsAdded.add(at.getFromAccountId());
            accountsAdded.add(at.getToAccountId());
        }

    }
    private static final SimpleDateFormat sdfLong = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private static String formatDateLong(Date d) {
        String ret = sdfLong.format(d);
        return ret;
    }
    private static final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();

    static {
        formatSymbols.setDecimalSeparator('.');
        formatSymbols.setGroupingSeparator(' ');
    }
    private static DecimalFormat CurrencyLong = null;

    public static String convertCentsToCurrencyLong(double minorUnit) {
        if (CurrencyLong == null) {
            CurrencyLong = new DecimalFormat(BaseUtils.getProperty("env.locale.currency.longformat"), formatSymbols);
        }
        double majorUnit = (double) (Math.floor(minorUnit) / 100.0);
        return CurrencyLong.format(majorUnit);
    }
}
