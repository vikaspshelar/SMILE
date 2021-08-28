/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.im.db.op.HSSDAO;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
@Singleton
@Startup
@Local({BaseListener.class})
public class SCSCFIsUpDaemon implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(SCSCFIsUpDaemon.class);
    private static Set<String> availableSCSCFs = new HashSet();
    private EntityManagerFactory emf = null;
    private static ScheduledFuture runner1 = null;

    private void trigger() {
        log.debug("In CSCFIsUpDaemon trigger");
        EntityManager em = null;
        List<String> scscfs;
        try {
            em = JPAUtils.getEM(emf);
            JPAUtils.beginTransaction(em);
            scscfs = HSSDAO.getAllEnabledSCSCFs(em);
        } finally {
            JPAUtils.commitTransactionAndClose(em);
        }

        Set<String> availableSCSCFsTmp = new HashSet();
        for (String scscf : scscfs) {
            try {
                // Name is in format e.g. sip:tz-cscf.it.tz.smilecoms.com:6060
                String friendlyName = scscf.split("\\:")[1].split("\\.")[0];
                log.debug("Checking SCSCF [{}] with friendly name [{}]", scscf, friendlyName);
                if (isUp(scscf)) {
                    log.debug("[{}] is up", scscf);
                    availableSCSCFsTmp.add(scscf);
                    BaseUtils.sendStatistic(friendlyName, "CSCF", "isup", 1, "IM");
                } else {
                    log.debug("[{}] is down", scscf);
                    BaseUtils.sendStatistic(friendlyName, "CSCF", "isup", 0, "IM");
                    BaseUtils.sendTrapToOpsManagement(
                            BaseUtils.MAJOR,
                            scscf,
                            "A CSCF is failing its availability test. It must be down. Location: " + scscf,
                            friendlyName);
                }
            } catch (Exception e) {
                log.warn("Error checking CSCF is up", e);
            }
        }
        availableSCSCFs = availableSCSCFsTmp;
    }

    @Override
    public void propsAreReadyTrigger() {
        runner1 = Async.scheduleAtFixedRate(new SmileBaseRunnable("IM.scscfIsUpDaemon") {
            @Override
            public void run() {
                trigger();
            }
        }, 40000, 10000);
        BaseUtils.registerForPropsChanges(this);
    }

    @Override
    public void propsHaveChangedTrigger() {
    }

    public static boolean isSCSCFMarkedAsUp(String scscf) {
        if (availableSCSCFs.isEmpty()) {
            log.debug("availableSCSCFs list is empty.....");
        }
        return (availableSCSCFs.isEmpty() || availableSCSCFs.contains(scscf));
    }

    @PostConstruct
    public void startUp() {
        BaseUtils.registerForPropsAvailability(this);
        emf = JPAUtils.getEMF("IMPU_RL");
    }

    @PreDestroy
    public void shutDown() {
        BaseUtils.deregisterForPropsAvailability(this);
        Async.cancel(runner1);
        BaseUtils.deregisterForPropsChanges(this);
        JPAUtils.closeEMF(emf);
    }

    private boolean isUp(String scscf) {
        // Name is in format as in DB e.g. sip:tzre-scscf1.it.tz.smilecoms.com:6060
        DatagramSocket sipSocket = null;
        int retries = BaseUtils.getIntProperty("env.im.scscfisup.connectretries", 3);
        String cid = null;

        while (retries > 0) {
            try {
                sipSocket = new DatagramSocket(0);
                sipSocket.setSoTimeout(BaseUtils.getIntProperty("env.im.scscfisup.connecttimeout.millis", 2000));
                InetAddress inetIpAddress = InetAddress.getByName(scscf.split(":")[1]);
                int port = Integer.parseInt(scscf.split(":")[2]);
                byte[] sendData;
                byte[] receiveData = new byte[1024];
                cid = Utils.getUUID();
                String method = "OPTIONS " + scscf + " SIP/2.0\r\nCall-ID: "
                        + cid + "@" + InetAddress.getLocalHost().getHostAddress() + "\r\nCSeq: 1 OPTIONS\r\nFrom: <sip:"
                        + InetAddress.getLocalHost().getHostAddress() + ":" + sipSocket.getLocalPort()
                        + ">;tag=" + Utils.getUUID() + "\r\nTo: <" + scscf + ">\r\nVia: SIP/2.0/UDP " + InetAddress.getLocalHost().getHostAddress()
                        + ":" + sipSocket.getLocalPort() + ";branch=z9hG4bK-323032-" + Utils.getUUID() + "\r\nMax-Forwards: 70\r\nContact: <sip:"
                        + InetAddress.getLocalHost().getHostAddress() + ":" + sipSocket.getLocalPort() + ">\r\nContent-Length: 0\r\n\r\n";

                sendData = method.getBytes();
                log.debug("Sending options message on socket");
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inetIpAddress, port);
                sipSocket.send(sendPacket);

                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                sipSocket.receive(receivePacket);

                String res = new String(receivePacket.getData());
                String mustMatch = BaseUtils.getProperty("env.im.scscfisup.response.mustmatch.regex", "");
                log.debug("S-CSCF: [{}]. Response was [{}]", scscf, res);
                if (!mustMatch.isEmpty()) {
                    return Utils.matchesWithPatternCache(res, mustMatch);
                }
                return true;
            } catch (SocketTimeoutException e) {
                log.error("S-CSCF: [{}] sockettimeout, decrementing retries... [{} - on call-id [{}]]", new Object[]{scscf, e.toString(), cid});
                retries--;
            } catch (IOException e) {
                log.error("S-CSCF: [{}] IO exception, decrementing retries... [{}]", scscf, e.toString());
                retries--;
            } catch (Exception e) {
                log.error("S-CSCF: [{}] is failing its availability test: [{}]", scscf, e.toString());
                return false;
            } finally {
                if (sipSocket != null) {
                    sipSocket.close();
                }
            }
        }
        log.error("CSCF: [{}] is failing its availability test", scscf);
        return false;
    }
}
