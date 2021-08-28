package com.smilecoms.bm;

import com.smilecoms.bm.db.model.AccountHistory;
import com.smilecoms.bm.db.model.ScheduledAccountHistory;        
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.AccountHistoryQuery;
import java.io.FileOutputStream;import java.util.List;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import org.slf4j.*;
import com.smilecoms.commons.util.IMAPUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

@Singleton
@Startup
@Local({BaseListener.class})
public class ScheduledAccountHistoryDaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(ScheduledAccountHistoryDaemon.class.getName());
    private EntityManagerFactory emf = null;
    private EntityManager em;
    private static ScheduledFuture runner = null;

    @PostConstruct
    public void startUp() {
        BaseUtils.registerForPropsAvailability(this);
        emf = JPAUtils.getEMF("BMPU_RL");
    }

    @Override
    public void propsAreReadyTrigger() {
        runner = Async.scheduleAtFixedRate(new SmileBaseRunnable("BM.scheduledAccountHistoryDaemon") {
            @Override
            public void run() {
                trigger();
            }
        }, 10000, 50000, 70000);
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsAvailability(this);
        if (runner != null) {
            runner.cancel(false);
        }
        
        JPAUtils.closeEMF(emf);
    }

    private void trigger() {
        try {
            
            if (!BaseUtils.getBooleanProperty("env.scheduled.account.statement.daemon.mustrun", false)) {
                log.warn("ScheduledAccountHistoryDaemon daemon is off");
                return;
            }
            
            
            if (log.isDebugEnabled()) {
                log.debug("ScheduledAccountHistoryDaemon triggered by thread {} on class {}", new Object[]{Thread.currentThread().getId(), this.toString()});
            }

            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);  
            
            Query q = em.createNativeQuery("select * from scheduled_account_history where last_run_date is null or last_run_date< DATE_FORMAT(now(), '%Y-%m-%d') or status='FAILED'", ScheduledAccountHistory.class);            
            List<ScheduledAccountHistory> historyConfigList = q.getResultList();            
            Collections.shuffle(historyConfigList);
            
            Calendar now = Calendar.getInstance();
            now.add(Calendar.DATE, 0);
            Date dateTo = now.getTime();
            
            log.debug("Found {} records to process.", historyConfigList.size());
            
            for (ScheduledAccountHistory historyConfig : historyConfigList) {
                boolean timeToExportReport=false;
                try {                    
                    em.refresh(historyConfig, LockModeType.PESSIMISTIC_READ);
                    Date dateFrom = now.getTime();
                    
                    if(historyConfig.getFrequency().equalsIgnoreCase("Daily")) {
                        if(historyConfig.getLastRunDate()==null || !Utils.areDatesOnSameDay(historyConfig.getLastRunDate(), dateTo)) { //Make sure we don't repeat run
                            log.debug("Processing daily report.");                        
                            now.add(Calendar.DATE, -1);
                            dateFrom = now.getTime();
                            timeToExportReport=true;
                        }
                    }
                    
                    if(historyConfig.getFrequency().equalsIgnoreCase("Weekly")) {
                        log.debug("Checking weekly report. We are on Day: {}", Utils.getDateDayOfWeek(dateTo));
                        if(Utils.getDateDayOfWeek(dateTo)==1) {  //Make sure we always run on the 1st day of the week
                            
                            now.add(Calendar.DATE, -7);
                            dateFrom = now.getTime();
                            if(historyConfig.getLastRunDate()==null || Utils.getDaysBetween2Dates(historyConfig.getLastRunDate(), dateTo)>=7) { //Make sure we don't repeat run
                                timeToExportReport=true;
                            }
                        }
                    }
                    
                    
                    if(historyConfig.getFrequency().equalsIgnoreCase("Monthly")) {                                                                        
                        if(historyConfig.getLastRunDate()==null || !Utils.areDatesInSameMonth(historyConfig.getLastRunDate(), dateTo)) { //We just started a new month
                            log.debug("Processing Monthly report.");
                            now.add(Calendar.DATE, -Utils.getMaxDaysInMonth(dateTo));
                            dateFrom = now.getTime();
                            timeToExportReport=true;
                        }
                        
                    }
                    
                    if(!timeToExportReport) {
                        return;
                    } else {
                        timeToExportReport=false;
                    }
                                 
                    
                    AccountHistoryQuery accountHistoryQuery = new AccountHistoryQuery();
                    accountHistoryQuery.setAccountId(historyConfig.getAccountId());                    
                    accountHistoryQuery.setDateFrom(Utils.getDateAsXMLGregorianCalendar(dateFrom));
                    accountHistoryQuery.setDateTo(Utils.getDateAsXMLGregorianCalendar(dateTo));
                    accountHistoryQuery.setResultLimit(20000);
                    
                    List<AccountHistory> historyList = DAO.getAccountHistory(em, accountHistoryQuery);
                    
                    log.debug("{} records returned for account: [{}] ", historyList.size(), historyConfig.getAccountId());
                    if(historyList.size()!=0) { 
                        File inputRecordsFile = new File("/tmp/"+ historyConfig.getAccountId() +"_"+ historyConfig.getFrequency()+"_report.csv");
                        FileOutputStream fos = new FileOutputStream(inputRecordsFile);
                        

                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
                        log.debug("insideAccountHistory");
                        try {
                                String fw = "ExternalId Id, Source, Destination,Value,Total Units,UC Units,Balance,Type,Description,Start Date,Device,Status,Term Code";
                                bw.write(fw);
                                bw.newLine();
                                for(AccountHistory record: historyList) {                                    

                                    fw= String.valueOf(record.getExtTxId());
                                    fw+=",";
                                    fw+=record.getSource();                                    
                                    fw+=",";
                                    fw+=record.getDestination();
                                    fw+=",";
                                    fw+=String.valueOf(record.getAccountCents());
                                    fw+=",";                        
                                    fw+=String.valueOf(record.getTotalUnits());
                                    fw+=",";
                                    fw+=String.valueOf(record.getUnitCreditUnits());
                                    fw+=",";
                                    fw+=record.getTransactionType();
                                    fw+=",";
                                    fw+=record.getDescription();
                                    fw+=",";
                                    fw+=record.getStartDate().toString();
                                    fw+=",";
                                    fw+=record.getSourceDevice();
                                    fw+=",";
                                    fw+=record.getStatus();
                                    fw+=",";
                                    fw+=record.getTermCode();                                      
                                    bw.write(fw);
                                    bw.newLine();                                    
                                    fw="";                                    
                                }
                                
                            bw.close();
                            
                            File xslFileToSend = inputRecordsFile;
                            
                            //createExcelFileFromRecords(inputRecordsFile, xslFileToSend);                            
                            if(emailFile(xslFileToSend, historyConfig.getEmailTo(), historyConfig.getFrequency())) {
                                log.debug("Going to Update status and runDate");
                                
                                historyConfig.setStatus("SUCCESS");
                                historyConfig.setLastRunDate(Utils.getCurrentJavaDate());
                                em.persist(historyConfig);
                                em.flush();
                                em.refresh(historyConfig);
                                log.debug("Update Completed!");
                            } else {
                                historyConfig.setStatus("FAILED");
                                historyConfig.setLastRunDate(Utils.getCurrentJavaDate());
                                em.persist(historyConfig);
                                em.flush();
                                em.refresh(historyConfig);
                                log.debug("Update Completed!");
                            }                            
                          inputRecordsFile.delete();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }    
                } catch (Exception e) {
                    e.printStackTrace();
                    log.debug("Something went wrong: [{}]", e);
                }
            }
        } catch (Exception e) {
            log.warn("Error in ScheduledAccountHistoryDaemon. Will ignore : [{}]", e.toString());
        } finally {
            try {
                JPAUtils.commitTransaction(em);
                JPAUtils.closeEM(em);
            } catch (Exception ex) {
                log.warn("Failed to close EntityManager: " + ex.toString());
            }
        }

    }
    
    public boolean emailFile(File xslFileToSend, String emailTo, String reportType) throws Exception {      
        log.debug("inside emailFile....");
        
        String fileName=xslFileToSend.getPath();
        try {
            
            log.debug("Attempting send...");
            IMAPUtils.sendEmail("noreply@smilecoms.com", emailTo, null, null, "SMILE Comms: " + reportType + " Account Report", "Please find the attached " + reportType + " account history for your account.", xslFileToSend.getName(), Utils.getDataFromFile(fileName));
            log.debug("Sent!");                
            return true;
        } catch (Exception me) {
            log.warn("Error sending email. Let's try again", me);  
            try {
                    log.debug("Attempting Again...");
                    IMAPUtils.sendEmail("noreply@smilecoms.com", emailTo, null, null, "SMILE Comms: " + reportType + " Account Report", "Please find the attached " + reportType + " account history for your account.", xslFileToSend.getName(), Utils.getDataFromFile(fileName));
                    log.debug("Sent!");                
                    return true;
                } catch (Exception e) {
                    log.debug("Final Fail", e);                
                    
                }
        }
        return false;
    }    

    @Override
    public void propsHaveChangedTrigger() {
    }
}
