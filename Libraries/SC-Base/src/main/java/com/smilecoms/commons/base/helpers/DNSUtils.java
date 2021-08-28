package com.smilecoms.commons.base.helpers;

import java.net.UnknownHostException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.ReverseMap;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import com.smilecoms.commons.base.BaseUtils;

/**
 *
 * @author pdutoit
 * @author Francois Hensley
 * @author PCB
 */
public class DNSUtils {

    private static final Logger log = LoggerFactory.getLogger(DNSUtils.class);
    private static final Set<SimpleResolver> resolvers = new java.util.concurrent.CopyOnWriteArraySet<>();

    
    public DNSUtils() {
        try {
            if (resolvers.isEmpty()) {
                @SuppressWarnings("sunapi")
                sun.net.dns.ResolverConfiguration rc = sun.net.dns.ResolverConfiguration.open();
                for (String dns : rc.nameservers()) {
                    try {
                        SimpleResolver res = new SimpleResolver(dns);
                        res.setTimeout(0, 100); // Only wait for 100ms. We dont want slow DNS to slow us down
                        resolvers.add(res);
                        log.info("Using nameserver [{}] for DNS resolving", dns);
                    } catch (UnknownHostException ex) {
                        log.warn("DNSUtils", "Could not add the nameserver [" + dns + "]", ex);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error initialising DNSUtils. All resolves will return null for now [{}]", e.toString());
        }
    }

    /**
     * Get the reverse address notation for the given address
     *
     * @param ipAddress The address to convert
     * @return in-addr.arpa reverse notation
     * @throws Exception
     */
    private String INAddrArpaNotation(String ipAddress) throws Exception {
        Name ipReverseNotation = new Name(ipAddress);

        try {
            // Turn IP into reverse notation
            ipReverseNotation = ReverseMap.fromAddress(ipAddress);
        } catch (UnknownHostException ex) {
            log.warn("INAddrArpaNotation", "Could not get the reverse notation for [" + ipAddress + "]", ex);
            throw ex;
        }

        return ipReverseNotation.toString();
    }

    /**
     * Lookup the PTR for the address
     *
     * @param ipAddress The address to lookup
     * @return PTR record
     */
    public String doReverseLookup(String ipAddress) {
        log.debug("Doing reverse lookup of IP [{}]", ipAddress);
        if (ipAddress.equals("127.0.0.1") || ipAddress.equals("127.0.1.1")) {
            log.debug("Using hostname from kernel");
            return BaseUtils.getHostNameFromKernel();
        }
        String ptrRecordString = null;
        try {
            String ipReverseNotation = this.INAddrArpaNotation(ipAddress);
            ptrRecordString = resolve(new Lookup(ipReverseNotation, Type.PTR));
            if (ptrRecordString != null) {
                ptrRecordString = ptrRecordString.toLowerCase();
                ptrRecordString = ptrRecordString.substring(0, ptrRecordString.length() - 1); // remove trailing .
            }
        } catch (Exception ex) {
            log.warn("doReverseLookup error for IP [{}]", ipAddress, ex);
        }

        if (ptrRecordString == null) {
            log.debug("IP [{}] has no reverse lookup record in DNS or DNS did not respond in time", ipAddress);
        } else {
            log.debug("Result [{}]", ptrRecordString);
        }

        return ptrRecordString;
    }

    /**
     * Lookup the ADDR record for a FQDN
     *
     * @param fqdn The FQDN to get the ADDR record
     * @return ADDR record
     */
    public String doForwardLookup(String hostName) {
        log.debug("Doing forward lookup of host [{}]", hostName);
        String ipString = null;
        try {
            ipString = resolve(new Lookup(hostName, Type.A));
        } catch (Exception ex) {
            log.warn("doForwardLookup error for host [{}]", hostName, ex);
        }
        if (ipString == null) {
            log.info("Host [{}] has no forward lookup record in DNS or DNS did not respond in time", hostName);
        } else {
            log.debug("Result [{}]", ipString);
        }
        return ipString;
    }

    private String resolve(Lookup lookup) {
        String returnString = null;
        // We do not want to cache anything here
        lookup.setCache(null);
        for (SimpleResolver resolver : resolvers) {
            lookup.setResolver(resolver);
            Record[] records = lookup.run();
            if (records != null) {
                returnString = records[0].rdataToString();
                break;
            }
        }

        return returnString;
    }

}
