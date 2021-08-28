/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.util;

import com.smilecoms.bm.db.model.AccountHistory;
import com.smilecoms.bm.db.op.DAO;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class AccountTransfer {

    private String loggedInUser;
    private long fromAccountId;
    private long toAccountId;
    private double transferAmountCents;
    private Date transferDateTime;
    private String transferType;
    private long accountHistoryId;
    private double fromAccountBalance;
    private String transferredByIPAddress;
    private CustomerData byCustomer;
    private CustomerData fromCustomer;
    private CustomerData toCustomer;
    private DeviceData byDevice;
    private SaleData sale;
    private EntityManager em;
    private static final Logger log = LoggerFactory.getLogger(AccountTransfer.class);
    private String txId;
    private boolean scheduled;
    private int loggedInUserCustomerProfileId;

    public int getLoggedInUserCustomerProfileId() {
        return loggedInUserCustomerProfileId;
    }
    
    public long getFromAccountId() {
        return fromAccountId;
    }

    public long getToAccountId() {
        return toAccountId;
    }

    public double getTransferAmountCents() {
        return transferAmountCents;
    }

    public Date getTransferDateTime() {
        return transferDateTime;
    }

    public String getTransferType() {
        return transferType;
    }

    public long getAccountHistoryId() {
        return accountHistoryId;
    }

    public double getFromAccountBalance() {
        return fromAccountBalance;
    }

    public String getTransferredByIPAddress() {
        return transferredByIPAddress;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public CustomerData getByCustomer() {
        if (byCustomer == null) {
            if (txId.contains("SCHEDULED")) {
                scheduled = true;
                byCustomer = new CustomerData(transferDateTime, fromAccountId, toAccountId);
            } else {
                byCustomer = new CustomerData(transferredByIPAddress, transferDateTime);
            }
        }
        return byCustomer;
    }

    public CustomerData getFromCustomer() {
        if (fromCustomer == null) {
            fromCustomer = new CustomerData(fromAccountId);
        }
        return fromCustomer;
    }

    public CustomerData getToCustomer() {
        if (toCustomer == null) {
            toCustomer = new CustomerData(toAccountId);
        }
        return toCustomer;
    }

    public DeviceData getByDevice() {
        return byDevice;
    }

    public SaleData getSale() {
        return sale;
    }

    public String getLoggedInUser() {
        return loggedInUser;
    }

    public AccountTransfer(EntityManager em, AccountHistory ahRow) {
        this.em = em;
        txId = ahRow.getExtTxId();
        accountHistoryId = ahRow.getId();
        transferDateTime = ahRow.getStartDate();
        fromAccountId = ahRow.getAccountId();
        toAccountId = Long.parseLong(ahRow.getDestination());
        transferAmountCents = ahRow.getAccountCents().doubleValue() * -1;
        fromAccountBalance = ahRow.getAccountBalanceRemaining().doubleValue();
        transferType = ahRow.getTransactionType();
        transferredByIPAddress = ahRow.getIPAddress();
        byDevice = new DeviceData(transferredByIPAddress, transferDateTime);
        if (ahRow.getDescription().startsWith("Account Transfer Debit (DSTU-")) {
            int saleId = Integer.parseInt(ahRow.getDescription().substring(29).replace(")", ""));
            sale = new SaleData(saleId);
        }
        loggedInUser = getLoggedInUser(transferDateTime, fromAccountId, toAccountId);
        loggedInUserCustomerProfileId = DAO.getCustomerProfileId(em, loggedInUser);

    }

    private String getLoggedInUser(Date transferDateTime, long fromAccountId, long toAccountId) {
        try {
            Query q = em.createNativeQuery("select \n"
                    + "SUBSTRING(DATA, INSTR(DATA,'<OriginatingIdentity>') + length('<OriginatingIdentity>'), INSTR(DATA,'</OriginatingIdentity>') - INSTR(DATA,'<OriginatingIdentity>') - length('<OriginatingIdentity>')) as TRANSFERRED_BY_USER\n"
                    + "from event_data \n"
                    + "where EVENT_TIMESTAMP >= ? - interval 5 second\n"
                    + "and EVENT_TIMESTAMP <= ? + interval 5 second\n"
                    + "and TYPE='BM'\n"
                    + "and SUB_TYPE = 'transferBalance'\n"
                    + "and DATA like ?\n"
                    + "and DATA like ?\n"
                    + "limit 1");
            q.setParameter(1, transferDateTime);
            q.setParameter(2, transferDateTime);
            q.setParameter(3, "%>" + fromAccountId + "%");
            q.setParameter(4, "%>" + toAccountId + "%");
            String data = (String) q.getSingleResult();
            return data;
        } catch (Exception e) {
            log.debug("Cannot get account transfers user name", e.toString());
            return "UNKNOWN";
        }
    }

    
    public final class CustomerData {

        public String name;
        public String ssoIdentity;
        public int customerProfileId;
        public String roleInOrganisation;
        public String securityGroup;
        public String createdByName;
        public String idNumber;
        public Date signUpDate;
        public String emailAddress;

        public CustomerData(long accountId) {
            try {
                Query q = em.createNativeQuery("select concat(C.FIRST_NAME,' ',C.LAST_NAME) as NAME,\n"
                        + "C.CUSTOMER_PROFILE_ID,\n"
                        + "C.ID_NUMBER,\n"
                        + "C.CREATED_DATETIME,\n"
                        + "C.EMAIL_ADDRESS,\n"
                        + "C.SSO_IDENTITY,\n"
                        + "ifnull(concat(R.ROLE,' in ', O.ORGANISATION_NAME),'') as ORG,\n"
                        + "ifnull(concat(S.FIRST_NAME,' ',S.LAST_NAME),'UNKNOWN') as SP_NAME,\n"
                        + "ifnull(G.SECURITY_GROUP_NAME,'')\n"
                        + "from service_instance SI\n"
                        + "join customer_profile C on C.CUSTOMER_PROFILE_ID = SI.CUSTOMER_PROFILE_ID\n"
                        + "left join customer_profile S on C.CREATED_BY_CUSTOMER_PROFILE_ID = S.CUSTOMER_PROFILE_ID\n"
                        + "left join customer_role R on R.CUSTOMER_PROFILE_ID = C.CUSTOMER_PROFILE_ID\n"
                        + "left join organisation O on O.ORGANISATION_ID = R.ORGANISATION_ID\n"
                        + "left join security_group_membership G on (G.CUSTOMER_PROFILE_ID = C.CUSTOMER_PROFILE_ID and G.SECURITY_GROUP_NAME != 'customer')\n"
                        + "where SI.ACCOUNT_ID=?\n"
                        + "order by C.CREATED_DATETIME limit 1");
                q.setParameter(1, accountId);
                Object[] res = (Object[]) q.getSingleResult();
                this.createdByName = (String) res[7];
                this.customerProfileId = (Integer) res[1];
                this.emailAddress = (String) res[4];
                this.idNumber = (String) res[2];
                this.name = (String) res[0];
                this.roleInOrganisation = (String) res[6];
                this.securityGroup = (String) res[8];
                this.signUpDate = (Date) res[3];
                this.ssoIdentity = (String) res[5];
            } catch (Exception e) {
                log.debug("Error getting customer by account id [{}]: {}", accountId, e);
            }
        }

        public CustomerData(String ipAddress, Date date) {
            Query q = em.createNativeQuery("select SERVICE_INSTANCE_ID from account_history where \n"
                    + "START_DATE <= ? + interval 20 minute\n"
                    + "and START_DATE >= ? - interval 2 day\n"
                    + "and END_DATE >= ? and IP_ADDRESS = ? limit 1");
            q.setParameter(1, date);
            q.setParameter(2, date);
            q.setParameter(3, date);
            q.setParameter(4, ipAddress);
            try {
                int siId = (Integer) q.getSingleResult();
                populateCustomerData(siId);
            } catch (Exception e) {
                log.debug("Cannot get customer from ip and date: {}", e.toString());
            }
        }

        // For scheduled transfers
        public CustomerData(Date transferDate, long fromAcc, long toAcc) {
            Query q = em.createNativeQuery("SELECT concat(C.FIRST_NAME, ' ', C.LAST_NAME) AS NAME,\n"
                    + "C.CUSTOMER_PROFILE_ID,\n"
                    + "C.ID_NUMBER,\n"
                    + "C.CREATED_DATETIME,\n"
                    + "C.EMAIL_ADDRESS,\n"
                    + "C.SSO_IDENTITY,\n"
                    + "ifnull(concat(R.ROLE, ' in ', O.ORGANISATION_NAME), '') AS ORG,\n"
                    + "ifnull(concat(S.FIRST_NAME,' ',S.LAST_NAME),'UNKNOWN') as SP_NAME,\n"
                    + "ifnull(G.SECURITY_GROUP_NAME, '')\n"
                    + "FROM customer_profile C       \n"
                    + "left JOIN customer_profile S\n"
                    + "ON C.CREATED_BY_CUSTOMER_PROFILE_ID = S.CUSTOMER_PROFILE_ID\n"
                    + "LEFT JOIN customer_role R\n"
                    + "ON R.CUSTOMER_PROFILE_ID = C.CUSTOMER_PROFILE_ID\n"
                    + "LEFT JOIN organisation O ON O.ORGANISATION_ID = R.ORGANISATION_ID\n"
                    + "LEFT JOIN security_group_membership G\n"
                    + "ON (G.CUSTOMER_PROFILE_ID = C.CUSTOMER_PROFILE_ID\n"
                    + "AND G.SECURITY_GROUP_NAME != 'customer')\n"
                    + "WHERE C.SSO_IDENTITY in \n"
                    + "(\n"
                    + "SELECT SUBSTRING(DATA,length(SUBSTRING_INDEX(DATA, '|', 3)) + 2) as SSO from event_data \n"
                    + "where EVENT_TIMESTAMP >= ? - interval 5 minute\n"
                    + "and EVENT_TIMESTAMP <= ?\n"
                    + "and TYPE='BM'\n"
                    + "and SUB_TYPE='SCHEDULED_TRANSFER'\n"
                    + "and DATA like ? group by TYPE\n"
                    + ")");
            q.setParameter(1, transferDate);
            q.setParameter(2, transferDate);
            q.setParameter(3, fromAcc + "|" + toAcc + "|%");
            try {
                Object[] res = (Object[]) q.getSingleResult();
                this.createdByName = (String) res[7];
                this.customerProfileId = (Integer) res[1];
                this.emailAddress = (String) res[4];
                this.idNumber = (String) res[2];
                this.name = (String) res[0];
                this.roleInOrganisation = (String) res[6];
                this.securityGroup = (String) res[8];
                this.signUpDate = (Date) res[3];
                this.ssoIdentity = (String) res[5];
            } catch (Exception e) {
                log.debug("Cannot get customer from future transfer: {}", e.toString());
            }
        }

        public CustomerData(int serviceInstanceId) {
            populateCustomerData(serviceInstanceId);
        }

        private void populateCustomerData(int serviceInstanceId) {
            Query q = em.createNativeQuery("select concat(C.FIRST_NAME,' ',C.LAST_NAME) as NAME,\n"
                    + "C.CUSTOMER_PROFILE_ID,\n"
                    + "C.ID_NUMBER,\n"
                    + "C.CREATED_DATETIME,\n"
                    + "C.EMAIL_ADDRESS,\n"
                    + "C.SSO_IDENTITY,\n"
                    + "ifnull(concat(R.ROLE,' in ', O.ORGANISATION_NAME),'') as ORG,\n"
                    + "ifnull(concat(S.FIRST_NAME,' ',S.LAST_NAME),'UNKNOWN') as SP_NAME,\n"
                    + "ifnull(G.SECURITY_GROUP_NAME,'')\n"
                    + "from service_instance SI\n"
                    + "join customer_profile C on C.CUSTOMER_PROFILE_ID = SI.CUSTOMER_PROFILE_ID\n"
                    + "left join customer_profile S on C.CREATED_BY_CUSTOMER_PROFILE_ID = S.CUSTOMER_PROFILE_ID\n"
                    + "left join customer_role R on R.CUSTOMER_PROFILE_ID = C.CUSTOMER_PROFILE_ID\n"
                    + "left join organisation O on O.ORGANISATION_ID = R.ORGANISATION_ID\n"
                    + "left join security_group_membership G on (G.CUSTOMER_PROFILE_ID = C.CUSTOMER_PROFILE_ID and G.SECURITY_GROUP_NAME != 'customer')\n"
                    + "where SI.SERVICE_INSTANCE_ID=?\n"
                    + "order by C.CREATED_DATETIME limit 1");
            q.setParameter(1, serviceInstanceId);
            Object[] res = (Object[]) q.getSingleResult();
            this.createdByName = (String) res[7];
            this.customerProfileId = (Integer) res[1];
            this.emailAddress = (String) res[4];
            this.idNumber = (String) res[2];
            this.name = (String) res[0];
            this.roleInOrganisation = (String) res[6];
            this.securityGroup = (String) res[8];
            this.signUpDate = (Date) res[3];
            this.ssoIdentity = (String) res[5];
        }
    }

    public class DeviceData {

        public String imeisv;
        public String gps;
        public String sectorName;
        public String imsi;

        public DeviceData(String ipAddress, Date date) {
            Query q = em.createNativeQuery("select SUBSTRING(tmp1.SOURCE_DEVICE, 8, 16) as IMEI,\n"
                    + "ifnull(S.SECTOR_NAME,''),\n"
                    + "ifnull(S.GPS_COORD,''),\n"
                    + "substring(tmp1.SERVICE_INSTANCE_IDENTIFIER, 18, 15) as IMSI\n"
                    + "from \n"
                    + "(\n"
                    + "select * from account_history H where \n"
                    + "H.START_DATE <= ? + interval 20 minute\n"
                    + "and H.START_DATE >= ? - interval 2 day\n"
                    + "and H.END_DATE >= ? and H.IP_ADDRESS = ? limit 1\n"
                    + ") as tmp1\n"
                    + "left join Network.sector_location S on (tmp1.LOCATION = S.TAI_ECGI)");
            q.setParameter(1, date);
            q.setParameter(2, date);
            q.setParameter(3, date);
            q.setParameter(4, ipAddress);
            try {
                Object[] res = (Object[]) q.getSingleResult();
                imeisv = (String) res[0];
                gps = (String) res[2];
                sectorName = (String) res[1];
                imsi = (String) res[3];
            } catch (Exception e) {
                log.debug("Cannot get record for ip and date: {}", e.toString());
            }
        }
    }

    public class SaleData {

        public int saleId;
        public String salesPersonName;
        public int salesPersonCustomerProfileId;
        public boolean wasSaleCancelled;
        public String paymentType;

        public SaleData(int saleId) {
            try {
                Query q = em.createNativeQuery("select concat(C.FIRST_NAME,' ',C.LAST_NAME) as NAME,\n"
                        + "C.CUSTOMER_PROFILE_ID,\n"
                        + "S.STATUS,\n"
                        + "S.PAYMENT_METHOD\n"
                        + "from sale S\n"
                        + "join customer_profile C on C.CUSTOMER_PROFILE_ID = S.SALES_PERSON_CUSTOMER_ID\n"
                        + "where S.SALE_ID=?");
                q.setParameter(1, saleId);
                Object[] res = (Object[]) q.getSingleResult();
                salesPersonName = (String) res[0];
                salesPersonCustomerProfileId = (Integer) res[1];
                wasSaleCancelled = ((String) res[2]).equals("RV");
                paymentType = (String) res[3];
                this.saleId = saleId;
            } catch (Exception e) {
                log.warn("Cannot find sale [{}]: {}", saleId, e.toString());
            }
        }
    }
}
