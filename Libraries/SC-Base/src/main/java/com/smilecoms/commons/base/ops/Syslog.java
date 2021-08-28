package com.smilecoms.commons.base.ops;

import java.io.*;
import java.net.*;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.*;

/**
 * The Syslog class implements the UNIX syslog protocol allowing Java to log
 * messages to a specified UNIX host. Care has been taken to preserve as much of
 * the UNIX implementation as possible.
 * <br>
 * To use Syslog, simply create an instance, and use the Syslog() method to log
 * your message. The class provides all the expected syslog constants. For
 * example, LOG_ERR is Syslog.LOG_ERR.
 * <br>
 *
 * Written: <a href="http://www.ice.com/time/">Tim Endres
 *
 * Version: 1.2 - July 27, 1998<br>
 * Version: 1.0 - August 14, 1996<br>
 * Source: <a href="http://www.ice.com/java/syslog/index.shtml">Syslog.java
 *
 * @see DatagramSocket
 * @see	InetAddress
 */
public class Syslog {
    
    private int portNum;
    private int flags;
    private InetAddress boundAddress;
    private DatagramSocket socket;
    private static final SimpleDateFormat date1Format = new SimpleDateFormat("MMM  d HH:mm:ss ", Locale.US);
    private static final SimpleDateFormat date2Format = new SimpleDateFormat("MMM dd HH:mm:ss ", Locale.US);
    
    static {
        date1Format.setTimeZone(TimeZone.getDefault());
        date2Format.setTimeZone(TimeZone.getDefault());
    }
    private static final Logger log = LoggerFactory.getLogger(Syslog.class.getName());
    private final String sysloghost;
    private static final int maxMsgSize = 950;
    private static final Map<String, Syslog> syslogs = new ConcurrentHashMap<>();
    private static final Lock lock = new ReentrantLock();
    
    public static void sendSyslog(String host, String name, int fac, int pri, String msg) {
        log.debug("In sendSyslog");
        Syslog syslog = syslogs.get(host);
        log.debug("Got syslog [{}]", syslog);
        if (syslog == null && lock.tryLock()) {
            try {
                log.debug("Creating new syslog for host [{}]", host);
                syslog = new Syslog(host);
                log.debug("Created new syslog for host [{}]", host);
                syslogs.put(host, syslog);
            } catch (Exception e) {
                log.warn("Error creating syslog", e);
            } finally {
                lock.unlock();
            }
        }
        if (syslog != null) {
            try {
                syslog.syslog(name, fac, pri, msg);
            } catch (Exception e) {
                log.warn("Error sending syslog", e);
            }
        } else {
            log.warn("Error sending msg[{}] via syslog as initialisation is in progress", msg);
        }
    }
    
    public static void sendSyslogWithSplits(String host, String name, int fac, int pri, String msg) {
        try {
            msg = "{{{" + msg + "}}}";
            int start = 0;
            int length = msg.length();
            int end = (length > maxMsgSize ? maxMsgSize : length);
            
            do {
                String bitToSend;
                if (start == 0) {
                    bitToSend = msg.substring(start, end);
                } else {
                    bitToSend = "...cont..." + msg.substring(start, end);
                }
                sendSyslog(host, name, SyslogDefs.LOG_LOCAL6, SyslogDefs.LOG_INFO, bitToSend);
                start = end;
                end = start + maxMsgSize;
                if (end > length) {
                    end = length;
                }
            } while (end > start);
        } catch (Exception e) {
            log.error("Error in sendErrorToSyslog: ", e.toString());
        }
    }

    /**
     * Unbinds the current syslog host.
     */
    public void close() {
        try {
            socket.close();
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }

    /**
     * Creates a Syslog object instance, targeted for the UNIX host with the
     * hostname 'hostname' on the syslog port 'port'. The only flags recognized
     * are 'LOG_PERROR', which will log the message to Java's 'System.err'.
     */
    public Syslog(String hostname) throws Exception {
        this.portNum = SyslogDefs.DEFAULT_PORT;
        this.flags = 0;
        this.sysloghost = hostname;
        this.boundAddress = InetAddress.getByName(hostname);
        this.socket = new DatagramSocket();
    }

    
    
    /**
     * Use this method to log your syslog messages. The facility and level are
     * the same as their UNIX counterparts, and the Syslog class provides
     * constants for these fields. The msg is what is actually logged.
     */
    /**
     * Use this method to log your syslog messages. The facility and level are
     * the same as their UNIX counterparts, and the Syslog class provides
     * constants for these fields. The msg is what is actually logged.
     */
    private void syslog(String name, int fac, int pri, String msg) throws Exception {
        int pricode;
        int length;
        int idx;
        byte[] data;
        byte[] sBytes;
        String strObj;
        
        log.debug("Sending syslog message [{}] to [{}]", msg, sysloghost);
        
        pricode = SyslogDefs.computeCode(fac, pri);
        Integer priObj = pricode;
        byte msgBytes[] = msg.getBytes();
        
        length = 4 + name.length() + msgBytes.length + 1;
        length += (pricode > 99) ? 3 : ((pricode > 9) ? 2 : 1);
        
        String dStr;
        // See note above on why we have two formats...
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.DAY_OF_MONTH) < 10) {
            dStr = date1Format.format(now.getTime());
        } else {
            dStr = date2Format.format(now.getTime());
        }
        length += dStr.length();
        
        data = new byte[length];
        
        idx = 0;
        data[idx++] = '<';
        
        strObj = Integer.toString(priObj);
        sBytes = strObj.getBytes();
        System.arraycopy(sBytes, 0, data, idx, sBytes.length);
        idx += sBytes.length;
        
        data[idx++] = '>';
        
        sBytes = dStr.getBytes();
        System.arraycopy(sBytes, 0, data, idx, sBytes.length);
        idx += sBytes.length;
        
        sBytes = name.getBytes();
        System.arraycopy(sBytes, 0, data, idx, sBytes.length);
        idx += sBytes.length;
        
        data[idx++] = ':';
        data[idx++] = ' ';
        
        try {
            System.arraycopy(msgBytes, 0, data, idx, msgBytes.length);
        } catch (Exception e) {
            log.warn("Error in syslog arraycopy [{}] Destination length [{}] Copying [{}] bytes from index [{}]. Message bytes [{}]  string [{}] ", new Object[]{e.toString(), data.length, msgBytes.length, idx, msgBytes, msg});
        }
        idx += msgBytes.length;
        
        data[idx] = 0;
        
        DatagramPacket packet = new DatagramPacket(data, length, this.boundAddress, this.portNum);
        socket.send(packet);
        log.debug("Sent syslog message");
    }
}
