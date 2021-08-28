/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.util;

import com.smilecoms.commons.base.BaseUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.*;
import org.slf4j.*;

/**
 *
 * @author PCB
 */
public class IMAPUtils {

    private static final Logger log = LoggerFactory.getLogger(IMAPUtils.class);

    public static void sendEmail(String from, String to, String subject, String body) throws Exception {
        sendEmail(from, to, null, null, subject, body, null, null);
    }

    public static void sendEmail(String from, String to, String cc, String bcc, String subject, String body) throws Exception {
        sendEmail(from, to, cc, bcc, subject, body, null, null);
    }

    /**
     * Simple method to send an email with an IMAP session
     *
     * @param from
     * @param to
     * @param subject
     * @param body
     * @throws javax.mail.MessagingException
     */
    public static void sendEmail(String from, String to, String cc, String bcc, String subject, String body, String attachmentName, byte[] attachment) throws Exception {
        long start = System.currentTimeMillis();
        log.debug("Sending email from [{}] to [{}] cc [{}] bcc [{}] with subject [{}] body [{}]", new Object[]{from, to, cc, bcc, subject, body});
        Message msg = new MimeMessage(getMailSession());
        msg.setFrom(new InternetAddress(from));
        if (bcc != null && !bcc.isEmpty()) {
            msg.setRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
        }
        if (cc != null && !cc.isEmpty()) {
            msg.setRecipient(Message.RecipientType.CC, new InternetAddress(cc));
        }
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        msg.setSubject(subject);

        Multipart multipart = new MimeMultipart();

        if (body != null) {
            // Part one is the body
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(body, "text/html; charset=ISO-8859-1");
            multipart.addBodyPart(messageBodyPart);
        }

        File tmp = null;

        if (attachment != null) {
            // Part two is attachment
            log.debug("Email has an attachment");
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            OutputStream fos = null;
            try {
                log.debug("Writing byte array to a temp file");
                tmp = File.createTempFile(Utils.getUUID(), null);
                fos = new FileOutputStream(tmp);
                fos.write(attachment);
                fos.close();
                log.debug("Attaching file to the message");
                DataSource source = new javax.activation.FileDataSource(tmp);
                attachmentBodyPart.setDataHandler(new DataHandler(source));
                attachmentBodyPart.setFileName(attachmentName);
                multipart.addBodyPart(attachmentBodyPart);
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
            log.debug("Finished adding attachment");
        }

        // Put parts in message
        msg.setContent(multipart);
        Transport t = null;
        try {
            t = getMailTransport();
            log.debug("Sending message to [{}] on transport", to);
            t.sendMessage(msg, msg.getAllRecipients());
            log.debug("Sent email to [{}] with subject [{}]. Operation took [{}]ms", new Object[]{to, subject, (System.currentTimeMillis() - start)});
        } catch (Exception e) {
            log.warn("Error sending on transport", e);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMTP", "Error sending email with subject: " + subject + " to: " + to + " via: "
                    + BaseUtils.getProperty("env.smtp.external.host", "")
                    + " on port: " + BaseUtils.getProperty("env.smtp.external.port", "")
                    + " Error : " + Utils.getDeepestCause(e).toString());
        } finally {
            if (t != null) {
                Lock lock = transportList.get(t);
                if (lock != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Releasing lock for transport [{}]", t.hashCode());
                    }
                    lock.unlock();
                }
            }
            if (tmp != null) {
                tmp.delete();
            }
        }
    }
    private static Session sharedSession;
    private static Map<Transport, Lock> transportList;
    private static String currentConfig = "";

    private static Session getMailSession() {
        log.debug("In getMailSession");
        String smtpUser = BaseUtils.getProperty("env.smtp.external.user", "");
        String smtpPassword = BaseUtils.getProperty("env.smtp.external.password", "");
        String config = BaseUtils.getProperty("env.smtp.external.host", "")
                + BaseUtils.getProperty("env.smtp.external.port", "")
                + smtpPassword
                + smtpUser;

        if (!currentConfig.equals(config)) {
            log.debug("SMTP configuration has changed from [{}] to [{}]. Will reinit", currentConfig, config);
            sharedSession = null;
            if (transportList != null) {
                for (Transport t : transportList.keySet()) {
                    try {
                        t.close();
                    } catch (Exception e) {
                        log.warn("Error closing mail transport: ", e);
                    }
                }
                transportList.clear();
            }
            currentConfig = config;
        }

        if (sharedSession != null) {
            if (log.isDebugEnabled()) {
                log.debug("Returning an alive mail session to host [{}] as the cached session is not null", sharedSession.getProperties().getProperty("mail.smtp.host"));
            }
            return sharedSession;
        }
        log.debug("Creating a mail session as the current session is null");
        String host = BaseUtils.getProperty("env.smtp.external.host");
        String port = BaseUtils.getProperty("env.smtp.external.port");
        log.debug("SMTP Host : [{}] on port [{}]", host, port);
        // Get system properties
        Properties props = new Properties();

        if (!smtpUser.isEmpty()) {
            log.debug("SMTP User Provided. Assuming this is a ssl mail server with authentication");
            props.put("mail.smtp.user", smtpUser);
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.socketFactory.port", port);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.connectiontimeout", 5000);
            props.put("mail.smtp.timeout", BaseUtils.getIntProperty("env.smtp.timeout.ms", 15000));

            Authenticator auth = new SMTPAuthenticator(smtpUser, smtpPassword);
            sharedSession = Session.getInstance(props, auth);
        } else {
            log.debug("SMTP User is blank. Assuming no authentication nor encryption");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.connectiontimeout", 5000);
            props.put("mail.smtp.timeout", BaseUtils.getIntProperty("env.smtp.timeout.ms", 15000));
            sharedSession = Session.getInstance(props);
        }
        // Get session
        log.debug("Finished creating a mail session");
        return sharedSession;
    }

    private static Transport getMailTransport() throws Exception {
        log.debug("In getMailTransport");

        if (transportList == null) {
            transportList = new ConcurrentHashMap<>();
        }
        Transport returnTransport = null;
        int cnt = 0;
        log.debug("Map has [{}] transports", transportList.size());
        for (Entry<Transport, Lock> entry : transportList.entrySet()) {
            cnt++;
            boolean mustRelease = true;
            if (entry.getValue().tryLock()) {
                try {
                    Transport t = entry.getKey();
                    int hashCode = t.hashCode();
                    log.debug("Got a a lock on [{}]", hashCode);

                    if (returnTransport != null && cnt > 10) {
                        log.debug("Too many transports. Cleaning one out [{}]", hashCode);
                        try {
                            t.close();
                        } catch (Exception e) {
                            log.warn("Error closing mail transport: ", e);
                        }
                        transportList.remove(t);
                    } else if (returnTransport == null) {
                        if (t.isConnected()) {
                            log.debug("Using transport [{}]", hashCode);
                            returnTransport = t;
                            mustRelease = false;
                        } else {
                            try {
                                t.close();
                            } catch (Exception e) {
                                log.warn("Error closing mail transport: ", e);
                            }
                            log.debug("Removing transport [{}] as its not connected", hashCode);
                            transportList.remove(t);
                        }
                    } else {
                        log.debug("Ignoring transport [{}] sitting in list at count [{}]", hashCode, cnt);
                    }
                } finally {
                    if (mustRelease) {
                        if (log.isDebugEnabled()) {
                            log.debug("Releasing lock on [{}]", entry.getKey().hashCode());
                        }
                        entry.getValue().unlock();
                    }
                }
            } else if (log.isDebugEnabled()) {
                log.debug("Someone has a lock on [{}]", entry.getKey().hashCode());
            }
        }

        if (returnTransport == null) {
            returnTransport = getNewMailTransport();
            Lock lock = new ReentrantLock();
            lock.lock();
            transportList.put(returnTransport, lock);
            if (log.isDebugEnabled()) {
                log.debug("Put new transport [{}] in map", returnTransport.hashCode());
            }
        }
        return returnTransport;
    }

    private static Transport getNewMailTransport() throws Exception {
        log.debug("In getNewMailTransport");
        Transport newTransport;
        log.debug("Creating a mail Transport");
        // Get session
        Session session = getMailSession();
        log.debug("Entering synchronised block");
        synchronized (session) {
            log.debug("Entered synchronised block");
            newTransport = session.getTransport();
            String smtpUser = BaseUtils.getProperty("env.smtp.external.user", "");
            log.debug("Calling connect");
            if (smtpUser.isEmpty()) {
                newTransport.connect();
            } else {
                
                String smtpPassword = "";
                
                if(BaseUtils.getBooleanProperty("env.smtp.use.sendgrid.api.key", false)) {
                    smtpPassword= BaseUtils.getPropertyAsMap("env.sendgrid.api.config").get("apikey");                    
                } else {
                    smtpPassword= BaseUtils.getProperty("env.smtp.external.password", "");
                }
                newTransport.connect(smtpUser, smtpPassword);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Connected the transport  [{}]. Is connected [{}]", newTransport.getURLName(), newTransport.isConnected());
        }
        return newTransport;
    }

    private static class SMTPAuthenticator extends javax.mail.Authenticator {

        String email;
        String password;

        public SMTPAuthenticator(String email, String password) {
            this.email = email;
            this.password = password;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(email, password);
        }
    }
}
