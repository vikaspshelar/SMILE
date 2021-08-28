/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.helpers;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.util.Utils;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class SpeedTestServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SpeedTestServlet.class);
    private static final byte[] data;

    static {
        data = new byte[10000];
        for (int i = 0; i < 1000; i++) {
            data[i] = (byte) 0xFF;
        }
    }
    private static final Lock lockDownload = new ReentrantLock();
    private static final Lock lockUpload = new ReentrantLock();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.debug("In doPost");
        resp.setStatus(204); // No content
        if (lockUpload.tryLock()) {
            try {

                String ip = Utils.getRemoteIPAddress(req);
                int siid = getSIId(ip);
                if (siid == 0) {
                    log.debug("Cannot determine location and SIId of user");
                    if (!BaseUtils.getBooleanProperty("env.development.mode", false)) {
                        return;
                    }
                }

                byte[] inputArray = new byte[1000];
                int read = 0;
                long start = System.currentTimeMillis();
                long bytes = 0;
                do {
                    read = req.getInputStream().read(inputArray, 0, 1000);
                    if (read > 0) {
                        bytes += read;
                    }
                } while (read >= 0);
                long ms = System.currentTimeMillis() - start;
                double bitspersec = (BaseUtils.getDoubleProperty("env.sep.speedtest.overhead.percent", 0) / 100.0d + 1) * bytes * 8.0d * 1000d / ms;
                log.debug("Finished doPost [{}]ms for [{}]Bytes. Upload Speed was [{}]Mbps", new Object[]{ms, bytes, bitspersec / 1000000});
                BaseUtils.sendStatistic("Network", "AccessPerformanceUL", "bps", bitspersec, null);

                Event event = new Event();
                event.setDate(Utils.getDateAsXMLGregorianCalendar(new Date()));
                event.setEventType("STAT");
                event.setEventSubType("SpeedTestUL");
                event.setEventKey(ip);
                event.setUniqueKey(null);
                event.setEventData((long) bitspersec + "|" + siid + "|" + ip + "|" + Utils.getIPLocation(ip));
                SCAWrapper.getAdminInstance().createEvent(event);

            } catch (Exception e) {
                log.warn("Error: [{}]", e.toString());
            } finally {
                lockUpload.unlock();
            }
        } else {
            log.debug("Somebody else is running a upload speed test on this node");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.debug("In doGet on SpeedTestServlet");

        if (!BaseUtils.getBooleanProperty("env.sep.speedtest.on", true)) {
            return;
        }

        if (lockDownload.tryLock()) {
            try {
                String ip = Utils.getRemoteIPAddress(request);

                int siid = getSIId(ip);
                if (siid == 0) {
                    log.debug("Cannot determine location and SIId of user");
                    if (!BaseUtils.getBooleanProperty("env.development.mode", false)) {
                        return;
                    }
                }

                response.setContentType("image/gif");
                OutputStream output = response.getOutputStream();
                long start = System.currentTimeMillis();
                long bytes = 0;
                int duration = BaseUtils.getIntProperty("env.sep.speedtest.run.secs", 10);
                log.debug("Starting speed test data send [{}]", ip);
                while (System.currentTimeMillis() - start < duration * 1000) {
                    output.write(data);
                    bytes += data.length;
                }
                output.close();
                log.debug("Finished speed test data send [{}]", ip);
                double bitspersec = (BaseUtils.getDoubleProperty("env.sep.speedtest.overhead.percent", 0) / 100.0d + 1) * bytes * 8.0d / duration;
                log.debug("Download Speed was [{}]Mbps Bytes was [{}] over [{}]s. Service Instance Id was [{}] for IP [{}]", new Object[]{bitspersec / 1000000, bytes, duration, siid, ip});
                BaseUtils.sendStatistic("Network", "AccessPerformanceDL", "bps", bitspersec, null);

                Event event = new Event();
                event.setDate(Utils.getDateAsXMLGregorianCalendar(new Date()));
                event.setEventType("STAT");
                event.setEventSubType("SpeedTestDL");
                event.setEventKey(ip);
                event.setUniqueKey(null);
                event.setEventData((long) bitspersec + "|" + siid + "|" + ip + "|" + Utils.getIPLocation(ip));
                SCAWrapper.getAdminInstance().createEvent(event);

            } catch (Exception e) {
                log.warn("Error: [{}]", e.toString());
            } finally {
                lockDownload.unlock();
            }
        } else {
            log.debug("Somebody else is running a download speed test on this node");
        }
        log.debug("Finished doGet on SpeedTestServlet");
    }

    private int getSIId(String ip) {
        if (ip.startsWith("10.")) {
            log.debug("LAN IP detected so dont bother checking");
            return 0;
        }
        int siid = 0;
        log.debug("In getSIId");
        try {
            ServiceInstanceQuery siq = new ServiceInstanceQuery();
            siq.setIPAddress(ip);
            siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(siq);
            siid = si.getServiceInstanceId();
        } catch (Exception e) {
            log.debug("Error getting Service instance for IP [{}]", e.toString());
        }
        log.debug("Returning SIId [{}]", siid);
        return siid;
    }

}
