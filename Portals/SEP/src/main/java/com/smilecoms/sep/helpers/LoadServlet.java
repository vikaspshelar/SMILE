/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.helpers;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.Utils;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class LoadServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(LoadServlet.class);
    private static final byte[] data;

    static {
        Random r = new Random();
        data = new byte[10000];
        r.nextBytes(data);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.debug("In doPost");
        resp.setStatus(204); // No content
        try {

            String ip = Utils.getRemoteIPAddress(req);
            byte[] inputArray = new byte[1000];
            int read;
            long start = System.currentTimeMillis();
            long bytes = 0;
            do {
                read = req.getInputStream().read(inputArray, 0, 10000);
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
            event.setEventData((long) bitspersec + "|" + 0 + "|" + ip);
            SCAWrapper.getAdminInstance().createEvent(event);
        } catch (Exception e) {
            log.warn("Error: [{}]", e.toString());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.debug("In doGet on SpeedTestServlet");

        try {
            String ip = Utils.getRemoteIPAddress(request);

            response.setContentType("image/gif");
            OutputStream output = response.getOutputStream();
            long start = System.currentTimeMillis();
            long bytes = 0;
            int duration = BaseUtils.getIntProperty("env.sep.loadtest.run.secs", 3600);
            log.debug("Starting speed test data send [{}]", ip);
            while (System.currentTimeMillis() - start < duration * 1000) {
                output.write(data);
                bytes += data.length;
            }
            output.close();
            log.debug("Finished speed test data send [{}]", ip);
            double bitspersec = (BaseUtils.getDoubleProperty("env.sep.speedtest.overhead.percent", 0) / 100.0d + 1) * bytes * 8.0d / duration;
            log.debug("Download Speed was [{}]Mbps Bytes was [{}] over [{}]s. For IP [{}]", new Object[]{bitspersec / 1000000, bytes, duration, ip});
            BaseUtils.sendStatistic("Network", "AccessPerformanceDL", "bps", bitspersec, null);

            Event event = new Event();
            event.setDate(Utils.getDateAsXMLGregorianCalendar(new Date()));
            event.setEventType("STAT");
            event.setEventSubType("SpeedTestDL");
            event.setEventKey(ip);
            event.setUniqueKey(null);
            event.setEventData((long) bitspersec + "|" + 0 + "|" + ip);
            SCAWrapper.getAdminInstance().createEvent(event);

        } catch (Exception e) {
            log.warn("Error: [{}]", e.toString());
        }
        log.debug("Finished doGet on SpeedTestServlet");
    }


}
