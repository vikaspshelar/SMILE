/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base.sd;

import com.hazelcast.core.IMap;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.helpers.BaseHelper;
import com.smilecoms.commons.base.helpers.DNSUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public abstract class BaseService implements IService {

    private static final transient Logger log = LoggerFactory.getLogger(BaseService.class);
    private final String jvmId;
    private static final long serialVersionUID = 1;
    private final String serviceName;
    private final String version;
    STATUS status;
    private String clientHostnameRegexMatch;
    String testResponseCriteria;
    String testData;
    private int weight = 5;
    private long lastModified;
    final URL url;
    private Pattern clientMatchPattern;
    private int msBetweenTests = 1000;
    private long lastIsUpTest;
    int testTimeoutMillis = 1000;
    long lastTestFail = System.currentTimeMillis() - 60000;
    long dateOfBirth = System.currentTimeMillis();
    String hostName = BaseUtils.getHostNameFromKernel();

    @Override
    public boolean isStale() {
        return System.currentTimeMillis() - lastModified > BaseUtils.getLongPropertyFailFast("env.sd.stale.millis", 7200000);
    }

    public void setGapBetweenTestsMillis(int ms) {
        msBetweenTests = ms;
    }

    @Override
    public int getGapBetweenTestsMillis() {
        return msBetweenTests;
    }

    private void touch() {
        lastModified = System.currentTimeMillis();
    }

    @Override
    public long getMillisSinceLastTestFail() {
        return System.currentTimeMillis() - lastTestFail;
    }

    public BaseService(String serviceName, String protocol, int port, String addressPart) throws MalformedURLException {
        this.url = new URL(protocol, BaseUtils.getIPAddress(), port, addressPart);
        jvmId = BaseUtils.JVM_ID;
        this.serviceName = serviceName;
        this.version = BaseUtils.SW_VERSION;
        this.status = STATUS.UP;
        touch();
    }

    @Override
    public void persist(IMap<String, IService> persistence) {
        persistence.set(this.getKey(), this);
        log.debug("Persisted service into distributed map [{}]", this);
    }

    @Override
    public void persistIfDifferent(IMap<String, IService> persistence, IService otherService) {
        BaseService other = (BaseService) otherService;
        if (other != null
                && this.lastModified == other.lastModified
                && this.clientHostnameRegexMatch.equals(other.clientHostnameRegexMatch)
                && this.dateOfBirth == other.dateOfBirth
                && this.lastIsUpTest == other.lastIsUpTest
                && this.weight == other.weight
                && this.lastTestFail == other.lastTestFail
                && this.msBetweenTests == other.msBetweenTests
                && this.testTimeoutMillis == other.testTimeoutMillis
                && this.jvmId.equals(other.jvmId)
                && this.testResponseCriteria.equals(other.testResponseCriteria)
                && this.serviceName.equals(other.serviceName)
                && this.status.equals(other.status)
                && this.testData.equals(other.testData)
                && this.url.equals(other.url)
                && this.version.equals(other.version)
                && this.hostName.equals(other.hostName)) {
            log.debug("Not persisting as there are no config changes");
            return;
        }
        persist(persistence);
        ServiceDiscoveryAgent.getInstance().publishServiceChange();
    }

    @Override
    public boolean doesNeedHealthCheck(String requestorsHostname, String requestorsJvmsId) {
        if (isInTheJVM(requestorsJvmsId) && !BaseUtils.getBooleanPropertyFailFast("env.sd.test.local.jvm", false)) {
            // Dont test own services
            return false;
        }
        long msSinceLastTest = System.currentTimeMillis() - lastIsUpTest;
        // By default dont test if we are not a preferred service caller, but do if nobody else seems to be testing as the ones supposed to be testing may be down
        if (!serviceAllowedFromHost(requestorsHostname) && (msSinceLastTest < (msBetweenTests + 3000))) {
            return false;
        }
        return (status.equals(STATUS.UP) || status.equals(STATUS.DOWN)) && (msSinceLastTest > msBetweenTests);
    }

    @Override
    public void doHealthCheck(final IMap<String, IService> persistence) {
        try {
            if (System.currentTimeMillis() - lastIsUpTest <= msBetweenTests || (!status.equals(STATUS.UP) && !status.equals(STATUS.DOWN))) {
                return;
            }

            try {
                log.debug("Testing [{}]", this);
                setLastTested(System.currentTimeMillis());
                persist(persistence);
                final STATUS before = status;

                Async.executeImmediate(new SmileBaseRunnable("SD.HealthCheck") {
                    @Override
                    public void run() {

                        if (!doTest()) {
                            setStatus(STATUS.DOWN);
                            lastTestFail = lastIsUpTest;
                        } else {
                            setStatus(STATUS.UP);
                        }
                        if (!before.equals(status)) {
                            // Status change
                            persist(persistence);
                            // Let everyone know a service has changed status
                            ServiceDiscoveryAgent.getInstance().publishServiceChange();
                        }

                    }
                }
                );
            } catch (java.util.concurrent.RejectedExecutionException re) {
                log.debug("Too many threads are trying to test at the same time. Wont test [{}]", this);
            }
        } catch (Exception e) {
            log.warn("Error doing health check. Leaving service as-is: ", e);
        }
    }

    abstract boolean doTest();

    @Override
    public String toString() {
        return "Service: " + serviceName + " @ " + url.toString() + " JVMId:" + jvmId + " Version:" + version + " Status:" + status + " ClientRegexMatch:" + clientHostnameRegexMatch;
    }

    @Override
    public boolean isInTheJVM(String otherJvmsId) {
        if (otherJvmsId == null) {
            return false;
        }
        return jvmId.equals(otherJvmsId);
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public void setGoingDown() {
        setStatus(STATUS.GOING_DOWN);
    }

    @Override
    public void setPaused() {
        setStatus(STATUS.PAUSED);
    }

    @Override
    public String getHostName() {
        if (hostName == null) {
            return "Unknown";
        }
        return hostName;
    }

    @Override
    public String getIPAddress() {
        String host = url.getHost();
        if (BaseHelper.isValidInet4Address(host)) {
            return host;
        }
        return new DNSUtils().doForwardLookup(host);
    }

    @Override
    public int getPort() {
        return url.getPort();
    }

    @Override
    public String getAddressPart() {
        return url.getFile();
    }

    @Override
    public String getJVMId() {
        return jvmId;
    }

    @Override
    public String getURL() {
        return url.toExternalForm();
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getStatus() {
        return status.toString();
    }

    @Override
    public void resume() {
        if (status.equals(STATUS.PAUSED)) {
            setStatus(STATUS.UP);
        }
    }

    @Override
    public void setStatus(STATUS status) {
        this.status = status;
        touch();
    }

    @Override
    public void inheritHistory(IService parent) {
        lastIsUpTest = ((BaseService) parent).lastIsUpTest;
        lastTestFail = ((BaseService) parent).lastTestFail;
        dateOfBirth = ((BaseService) parent).dateOfBirth;
        lastModified = ((BaseService) parent).lastModified;
    }

    @Override
    public void setTestTimeoutMillis(int ms) {
        testTimeoutMillis = ms;
        touch();
    }

    @Override
    public boolean isUp() {
        return status.equals(STATUS.UP);
    }

    @Override
    public String getClientHostnameRegexMatch() {
        return clientHostnameRegexMatch;
    }

    public void setClientHostnameRegexMatch(String clientHostnameRegexMatch) {
        this.clientHostnameRegexMatch = clientHostnameRegexMatch;
        clientMatchPattern = Pattern.compile(clientHostnameRegexMatch);
        touch();
    }

    @Override
    public boolean isAvailable(boolean mustBeSameVersion, boolean mustMatchHost) {
        boolean hostIsOk;
        if (!mustMatchHost) {
            hostIsOk = true;
        } else {
            hostIsOk = serviceAllowedFromThisHost();
        }
        if (!hostIsOk) {
            return false;
        }
        boolean statusIsOk = (status.equals(STATUS.UP));
        if (!statusIsOk) {
            log.warn("A resource is marked as not up and wont be used [{}]", this);
            return false;
        }
        if (!mustBeSameVersion) {
            return true;
        }
        return (version.equals(BaseUtils.SW_VERSION));
    }

    @Override
    public boolean isGoingDown() {
        return status.equals(STATUS.GOING_DOWN);
    }

    public void setTestResponseCriteria(String testResponseCriteria) {
        this.testResponseCriteria = testResponseCriteria;
        touch();
    }

    public void setTestData(String testData) {
        this.testData = testData;
        touch();
    }

    @Override
    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
        touch();
    }

    private boolean serviceAllowedFromThisHost() {
        return serviceAllowedFromHost(BaseUtils.getHostNameFromKernel());
    }

    private boolean serviceAllowedFromHost(String hostName) {
        return clientMatchPattern.matcher(hostName).find();
    }

    public static String makeKey(String serviceName, String protocol, String ip, int port, String addressPart) {
        return serviceName + protocol + ip + port + addressPart;
    }

    @Override
    public String getKey() {
        return makeKey(serviceName, url.getProtocol(), url.getHost(), url.getPort(), url.getFile());
    }

    @Override
    public long getMillisSinceLastModified() {
        return System.currentTimeMillis() - lastModified;
    }

    @Override
    public long getLastTested() {
        return lastIsUpTest;
    }

    private void setLastTested(long currentTimeMillis) {
        lastIsUpTest = currentTimeMillis;
        touch();
    }

}
