/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.base.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class HazelcastHelper {

    private static final Logger log = LoggerFactory.getLogger(HazelcastHelper.class);
    private static final Lock lock = new ReentrantLock();
    private static final boolean IS_SERVER = (BaseUtils.SERVER_NAME.contains("PLAT") || (System.getProperty("HC_FORCE_SERVER") != null && System.getProperty("HC_FORCE_SERVER").equals("true")));

    static {
        if (IS_SERVER) {
            log.warn("IS_SERVER [{}] HC_FORCE_SERVER [{}] SERVER_NAME [{}]", new Object[]{IS_SERVER, System.getProperty("HC_FORCE_SERVER"), BaseUtils.SERVER_NAME});
        }
    }
    private static HazelcastInstance baseHCInstance = null;
    private static boolean warmedUp = false;
    private static final String BASE_HC_GROUP_NAME = "BASE";

    public static void warmUp() {
        Async.executeImmediate(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    log.warn("Warming up our base Hazelcast instance");
                    long start = System.currentTimeMillis();
                    baseHCInstance = newHazelcastInstance("SMILEBASE", BaseUtils.class.getClassLoader());
                    warmedUp = true;
                    log.warn("Finished warming up our base Hazelcast instance. Took [{}]ms", System.currentTimeMillis() - start);
                } finally {
                    lock.unlock();
                }
            }
        });
    }

    public static boolean isHelperBusy() {
        if (!warmedUp) {
            return true;
        }
        boolean gotLock = lock.tryLock();
        if (gotLock) {
            lock.unlock();
        }
        return !gotLock;
    }

    public static HazelcastInstance getBaseHazelcastInstance() {
        if (baseHCInstance == null) {
            // Fail fast. Dont let threads wait around for Hazelcast
            if (!warmedUp) {
                throw new RuntimeException("Hazelcast is not available yet");
            }
            boolean gotLock = lock.tryLock();
            if (!gotLock) {
                throw new RuntimeException("Hazelcast is not available yet");
            }
            try {
                if (baseHCInstance != null) {
                    return baseHCInstance;
                }
                baseHCInstance = newHazelcastInstance("SMILEBASE", BaseUtils.class.getClassLoader());
            } finally {
                lock.unlock();
            }
        }
        return baseHCInstance;
    }

    public static boolean isBaseHazelcastInstanceUp() {
        if (!warmedUp) {
            return false;
        }
        return getBaseHazelcastInstance().getLifecycleService().isRunning();
    }

    public static void shutdownBaseHazelcastInstance() {
        shutdownHazelcastInstance(baseHCInstance);
    }

    private static void shutdownHazelcastInstance(HazelcastInstance hc) {
        if (hc == null) {
            return;
        }
        try {
            PrintWriter writer = null;
            try {
                log.warn("Requested to shut down hazelcast client instance [{}]. First persisting current instance IPs into [{}]", hc.getName(), HC_CONF_FILE);
                File f = new File(HC_CONF_FILE);
                // Only write list if it has not been written recently
                Set<Member> members = hc.getCluster().getMembers();
                if (!f.exists() || (f.lastModified() < System.currentTimeMillis() - 600000 && members.size() >= 5)) {
                    writer = new PrintWriter(HC_CONF_FILE, "UTF-8");
                    int cnt = 0;
                    for (Member member : members) {
                        String ip = member.getAddress().getInetAddress().getHostAddress();
                        if (!ip.equals(BaseUtils.getIPAddress())) {
                            writer.println(ip);
                            cnt++;
                            log.warn("Persisted remote HC instance IP [{}] to [{}] for later bootstrapping", ip, HC_CONF_FILE);
                        } else {
                            log.warn("Wont persist IP [{}] as its my own", ip);
                        }
                    }
                    log.warn("Persisted [{}] IPs to [{}]", cnt, HC_CONF_FILE);
                } else {
                    log.warn("Skipping persisting to [{}]", HC_CONF_FILE);
                }
            } catch (Exception e) {
                log.warn("Error persisting HC instance IPs: ", e);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
            shutdown(hc);
        } catch (Exception e) {
            log.warn("Error shutting down base hazelcast: ", e);
        }
    }

    public static HazelcastInstance newHazelcastServerInstance(String groupName, String instanceName, ClassLoader classLoader) throws RuntimeException {
        if (groupName.equals(BASE_HC_GROUP_NAME)) {
            throw new java.lang.IllegalArgumentException("Cannot create the base instance from outside base");
        }
        return internalNewHazelcastServerInstance(groupName, instanceName, classLoader);
    }

    private static HazelcastInstance internalNewHazelcastServerInstance(String groupName, String instanceName, ClassLoader classLoader) throws RuntimeException {
        HazelcastInstance hazelcastInstance;
        try {
            // First use one if we have it
            hazelcastInstance = getExistingInstance(groupName, instanceName);
            if (hazelcastInstance != null) {
                log.warn("We have a server instance to give back");
                return hazelcastInstance;
            }

            log.debug("Initialising Hazelcast server");
            Config config = new XmlConfigBuilder(new ByteArrayInputStream(HAZELCAST_SERVER_CONFIG.getBytes("UTF-8"))).build();
            config.setClassLoader(classLoader);
            config.setInstanceName(instanceName);
            config.getGroupConfig().setName(groupName);
            log.debug("Hazelcast group name is [{}]", groupName);
            if (config.getNetworkConfig().getJoin().getTcpIpConfig().isEnabled()) {
                for (String ip : getPossibleBoostrapMembers(groupName)) {
                    log.warn("Hazelcast TCP/IP discovery will use this as a possible bootstrap starting member: [{}] for group [{}]", ip, groupName);
                    config.getNetworkConfig().getJoin().getTcpIpConfig().addMember(ip);
                }
            }

            if (groupName.equals("PCRF")) {//Hack to increase timeout for PCRF
                //When you make an API call using Hazelcast IMDG, an operation has been started on one of the Hazelcast cluster members. 
                //These operations send heartbeats to the invocation owner (caller) periodically.
                //If the invocation owner does not receive any heartbeats from the pending invocation for the configured timeout duration ("hazelcast.operation.call.timeout.millis"),
                //then it throws an OperationTimeoutException.

                //We have issues where a PCRF instance is unresponsive and does not return heartbeats. We try to mitigate these exceptions by increasing the operation heartbeat timeout:
                //So this means client will check each 5 seconds if 60 second is reached. And each 5 seconds sends a packet to node to say it is alive.
                log.warn("PCRF group is adjusting Hazelcast server properties: [hazelcast.operation.call.timeout.millis={}], [hazelcast.heartbeat.interval.seconds={}]", config.getProperty("hazelcast.operation.call.timeout.millis"), config.getProperty("hazelcast.heartbeat.interval.seconds"));
                config.setProperty("hazelcast.operation.call.timeout.millis", "60000");
                config.setProperty("hazelcast.heartbeat.interval.seconds", "5");
                config.setProperty("hazelcast.partition.migration.timeout", "420");//default 300
                log.warn("PCRF group Hazelcast server instance properties: [call.timeout.millis={}], [heartbeat.interval.seconds={}]", config.getProperty("hazelcast.operation.call.timeout.millis"), config.getProperty("hazelcast.heartbeat.interval.seconds"));
            }

            if (groupName.equals("BASE")) {//Hack to increase timeout for SMILEBASE
                log.warn("BASE group is adjusting Hazelcast server properties: [hazelcast.operation.call.timeout.millis={}], [hazelcast.heartbeat.interval.seconds={}]", config.getProperty("hazelcast.operation.call.timeout.millis"), config.getProperty("hazelcast.heartbeat.interval.seconds"));
                config.setProperty("hazelcast.operation.call.timeout.millis", "60000");
                config.setProperty("hazelcast.heartbeat.interval.seconds", "5");
                config.setProperty("hazelcast.partition.migration.timeout", "420");//default 300
                log.warn("BASE group Hazelcast server instance properties: [call.timeout.millis={}], [heartbeat.interval.seconds={}]", config.getProperty("hazelcast.operation.call.timeout.millis"), config.getProperty("hazelcast.heartbeat.interval.seconds"));
            }

            String bindIP = System.getProperty("HC_BIND_IP");
            if (bindIP != null && !bindIP.isEmpty()) {
                log.warn("We have a specific IP that hazelcast must bind on [{}]", bindIP);
                config.getNetworkConfig().getInterfaces().setEnabled(true);
                config.getNetworkConfig().getInterfaces().clear();
                config.getNetworkConfig().getInterfaces().addInterface(bindIP);
            } else {
                String ip = BaseUtils.getIPAddress();
                if (!ip.startsWith("127.") && !ip.startsWith("172.")) {
                    log.warn("Determined that hazelcast must bind on [{}]", ip);
                    config.getNetworkConfig().getInterfaces().setEnabled(true);
                    config.getNetworkConfig().getInterfaces().clear();
                    config.getNetworkConfig().getInterfaces().addInterface(ip);
                }
            }
            log.warn("Calling Hazelcast to create new Hazelcast server instance for name [{}] in group [{}]", instanceName, groupName);
            hazelcastInstance = Hazelcast.newHazelcastInstance(config);
            log.warn("Successfully created hazelcast server instance [{}] for name [{}] in group [{}]", new Object[]{hazelcastInstance.getLocalEndpoint().getUuid(), instanceName, groupName});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.debug("Successfully initialised Hazelcast");
        return hazelcastInstance;
    }

    private static HazelcastInstance newHazelcastClientInstance(String groupName, String instanceName, ClassLoader classLoader) throws RuntimeException {
        HazelcastInstance hazelcastInstance;
        try {
            // First use one if we have it
            hazelcastInstance = getExistingInstance(groupName, instanceName);
            if (hazelcastInstance != null) {
                log.warn("We have a client instance to give back");
                return hazelcastInstance;
            }

            log.debug("Initialising Hazelcast client");
            ClientConfig config = new XmlClientConfigBuilder(new ByteArrayInputStream(HAZELCAST_CLIENT_CONFIG.getBytes("UTF-8"))).build();
            config.setClassLoader(classLoader);
            config.setInstanceName(instanceName);
            config.getGroupConfig().setName(groupName);
            log.debug("Hazelcast group name is [{}]", groupName);
            for (String ip : getPossibleBoostrapMembers(groupName)) {
                log.warn("Hazelcast TCP/IP discovery will use this as a possible starting member: [{}]", ip);
                config.getNetworkConfig().addAddress(ip + ":8900");
            }

            /*
                Setting Connection Timeout
                Connection timeout is the timeout value in milliseconds for members to accept client connection requests.
             */
            if (groupName.equals("BASE")) {
                log.debug("Current Hazelcast group name [{}]s connection timeout setting is [{}]", groupName, config.getNetworkConfig().getConnectionTimeout());
                config.getNetworkConfig().setConnectionTimeout(5000);
                log.warn("Resetting Hazelcast connection timeout to [{}]", config.getNetworkConfig().getConnectionTimeout());
            }

            /*
                Client Deadline Failure Detector
                Deadline Failure Detector uses an absolute timeout for missing/lost heartbeats. After timeout, a member is considered as crashed/unavailable and marked as suspected.
                
                Deadline Failure Detector has two configuration properties:
                    hazelcast.client.heartbeat.interval: This is the interval at which client sends heartbeat messages to members.
                    hazelcast.client.heartbeat.timeout: This is the timeout which defines when a cluster member is suspected, because it has not sent any response back to client requests.
                                                        Timeout for the heartbeat messages sent by the client to members.
                                                        If no messages pass between client and member within the given time via this property in milliseconds, the connection will be closed.

                The value of hazelcast.client.heartbeat.interval should be smaller than that of hazelcast.client.heartbeat.timeout.
                In addition, the value of system property hazelcast.client.max.no.heartbeat.seconds, which is set at the member side,
                should be larger than that of hazelcast.client.heartbeat.interval.
             */
            if (groupName.equals("BASE")) {
                log.debug("Current Hazelcast group name [{}]s  client heatbeat timeout is [{}]", groupName, config.getProperty("hazelcast.client.heartbeat.timeout"));
                config.setProperty("hazelcast.client.heartbeat.timeout", "30000");
                //config.setProperty("hazelcast.client.heartbeat.interval", "5000"); Leave as is
                log.warn("Resetting Hazelcast client heatbeat timeout to [{}]", config.getProperty("hazelcast.client.heartbeat.timeout"));
                
                //hazelcast.client.invocation.timeout.seconds: Period, in seconds, to give up the invocation when a member in the member list is not reachable.
                log.debug("Resetting Hazelcast client invocation timeout from [{}]", config.getProperty("hazelcast.client.invocation.timeout.seconds"));
                config.setProperty("hazelcast.client.invocation.timeout.seconds", "15");
                log.warn("New Hazelcast client invocation timeout is [{}]", config.getProperty("hazelcast.client.invocation.timeout.seconds"));
            }

            log.warn("Calling Hazelcast to create new Hazelcast client instance for name [{}] in group [{}]", instanceName, groupName);
            hazelcastInstance = HazelcastClient.newHazelcastClient(config);
            log.warn("Successfully created hazelcast client instance [{}] for name [{}] in group [{}]", new Object[]{hazelcastInstance.getLocalEndpoint().getUuid(), instanceName, groupName});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.debug("Successfully initialised Hazelcast client");
        return hazelcastInstance;
    }

    private static HazelcastInstance newHazelcastInstance(String instanceName, ClassLoader classLoader) throws RuntimeException {
        if (IS_SERVER) {
            return internalNewHazelcastServerInstance(BASE_HC_GROUP_NAME, instanceName, classLoader);
        } else {
            return newHazelcastClientInstance(BASE_HC_GROUP_NAME, instanceName, classLoader);
        }
    }

    public static HazelcastInstance newHazelcastClientInstance(String instanceName, ClassLoader classLoader) throws RuntimeException {
        return newHazelcastClientInstance(BASE_HC_GROUP_NAME, instanceName, classLoader);
    }

    private static final String HC_CONF_FILE = "/etc/smile/hazelcast.conf";

    public static void shutdown(HazelcastInstance hazelcastInstance) {
        if (hazelcastInstance == null) {
            log.debug("Hazelcast instance is null. Wont try to shut it down");
            return;
        }
        try {
            log.warn("Shutting down Hazelcast instance [{}]", hazelcastInstance.getName());
            hazelcastInstance.shutdown();
            log.warn("Shut down Hazelcast instance [{}]", hazelcastInstance.getName());
        } catch (com.hazelcast.core.HazelcastInstanceNotActiveException notActive) {
            log.debug("Hazelcast instance is not active. Ignoring shitdown error");
        } catch (Exception e) {
            log.warn("Error shutting down hazelcast instance: ", e);
        }
    }

    private static HazelcastInstance getExistingInstance(String groupName, String instanceName) {
        if (IS_SERVER) {
            HazelcastInstance hazelcastInstance = Hazelcast.getHazelcastInstanceByName(instanceName);
            if (hazelcastInstance != null && hazelcastInstance.getConfig().getGroupConfig().getName().equals(groupName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Returning existing hazelcast server instance [{}] for name [{}] in group [{}]", new Object[]{hazelcastInstance.getLocalEndpoint().getUuid(), instanceName, groupName});
                }
                return hazelcastInstance;
            }
        } else {
            HazelcastInstance hazelcastInstance = HazelcastClient.getHazelcastClientByName(instanceName);
            if (hazelcastInstance != null && hazelcastInstance.getConfig().getGroupConfig().getName().equals(groupName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Returning existing hazelcast client instance [{}] for name [{}] in group [{}]", new Object[]{hazelcastInstance.getLocalEndpoint().getUuid(), instanceName, groupName});
                }
                return hazelcastInstance;
            }
        }
        return null;
    }

    private static Set<String> getPossibleBoostrapMembers(String groupName) {
        Set<String> ret = new HashSet<>();
        // First check HC_BOOTSTRAP_IPS

        String envList = System.getProperty("HC_BOOTSTRAP_IPS_" + groupName);
        if (envList != null && !envList.isEmpty()) {
            log.warn("HC_BOOTSTRAP_IPS_" + groupName + "=[{}]", envList);
            ret.addAll(getListFromCommaDelimitedString(envList));
        }

        if (!ret.isEmpty()) {
            return ret;
        }
        ret.addAll(getLastInstanceMembers());
        if (!ret.isEmpty()) {
            return ret;
        }
        log.warn("No env variable specifying boostrap Hazelcast IP's so we will use host IP");

        ret.add(BaseUtils.getIPAddress());
        return ret;
    }

    private static Set<String> getLastInstanceMembers() {
        Set<String> res = new HashSet<>();
        DataInputStream in = null;
        try {
            FileInputStream fstream = new FileInputStream(HC_CONF_FILE);
            in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                log.warn("Got hazelcast member [{}] from file [{}]", strLine, HC_CONF_FILE);
                res.add(strLine);
            }
        } catch (java.io.FileNotFoundException e1) {
            log.debug("No file containing last instance info");
        } catch (Exception e) {
            log.warn("Error in getLastInstanceMembers: ", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e2) {
                    log.warn("Error closing stream", e2);
                }
            }
        }
        return res;
    }

    private static List<String> getListFromCommaDelimitedString(String s) {
        List<String> ret = new ArrayList<>();
        if (s != null) {
            StringTokenizer stValues = new StringTokenizer(s, ",");
            while (stValues.hasMoreTokens()) {
                String val = stValues.nextToken();
                if (!val.isEmpty()) {
                    ret.add(val.trim());
                }
            }
        }
        return ret;
    }

    public static Lock getGlobalLock(String lockName) {
        return getBaseHazelcastInstance().getLock(lockName);
    }

    private static final String HAZELCAST_SERVER_CONFIG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<hazelcast xsi:schemaLocation=\"http://www.hazelcast.com/schema/config hazelcast-config-3.8.xsd\"\n"
            + "           xmlns=\"http://www.hazelcast.com/schema/config\"\n"
            + "           xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
            + "    <properties>\n"
            + "        <property name=\"hazelcast.shutdownhook.enabled\">false</property>\n"
            + "        <property name=\"hazelcast.clientengine.thread.count\">5</property>\n"
            + "        <property name=\"hazelcast.operation.call.timeout.millis\">1500</property>\n"
            + "        <property name=\"hazelcast.heartbeat.interval.seconds\">2</property>\n"
            + "        <property name=\"hazelcast.max.no.heartbeat.seconds\">10</property>\n"
            + "        <property name=\"hazelcast.partition.migration.timeout\">300</property>\n"
            + "        <property name=\"hazelcast.phone.home.enabled\">false</property>\n"
            + "        <property name=\"hazelcast.max.no.master.confirmation.seconds\">100</property>\n"
            + "        <property name=\"hazelcast.event.thread.count\">3</property>\n"
            + "    </properties>"
            + "    <group>\n"
            + "        <name>SmileGroup</name>\n"
            + "        <password>Huhi78gAmldo9w27</password>\n"
            + "    </group>\n"
            + "    <management-center enabled=\"false\">http://localhost:8080/mancenter</management-center>\n"
            + "    <network>\n"
            + "        <port auto-increment=\"true\" port-count=\"20\">8900</port>\n"
            + "        <outbound-ports>\n"
            + "            <ports>0</ports>\n"
            + "        </outbound-ports>\n"
            + "        <join>\n"
            + "            <multicast enabled=\"false\">\n"
            + "                <multicast-group>224.2.2.3</multicast-group>\n"
            + "                <multicast-port>54331</multicast-port>\n"
            + "            </multicast>\n"
            + "            <tcp-ip enabled=\"true\">\n"
            + "            </tcp-ip>\n"
            + "            <aws enabled=\"false\">\n"
            + "                <access-key>my-access-key</access-key>\n"
            + "                <secret-key>my-secret-key</secret-key>\n"
            + "                <region>us-west-1</region>\n"
            + "                <host-header>ec2.amazonaws.com</host-header>\n"
            + "                <security-group-name>hazelcast-sg</security-group-name>\n"
            + "                <tag-key>type</tag-key>\n"
            + "                <tag-value>hz-nodes</tag-value>\n"
            + "            </aws>\n"
            + "            <discovery-strategies>\n"
            + "            </discovery-strategies>\n"
            + "        </join>\n"
            + "        <interfaces enabled=\"false\">\n"
            + "        </interfaces>\n"
            + "        <ssl enabled=\"false\"/>\n"
            + "        <socket-interceptor enabled=\"false\"/>\n"
            + "        <symmetric-encryption enabled=\"false\">\n"
            + "            <algorithm>PBEWithMD5AndDES</algorithm>\n"
            + "            <salt>dhduiqwdoiwqjopqcoihqiuYGW76298ajpO</salt>\n"
            + "            <password>johoiPOEh9w8w90</password>\n"
            + "            <iteration-count>19</iteration-count>\n"
            + "        </symmetric-encryption>\n"
            + "    </network>\n"
            + "    <partition-group enabled=\"true\" group-type=\"HOST_AWARE\" />"
            + "    <executor-service name=\"default\">\n"
            + "        <pool-size>16</pool-size>\n"
            + "        <queue-capacity>0</queue-capacity>\n"
            + "    </executor-service>\n"
            + "    <queue name=\"default\">\n"
            + "        <max-size>0</max-size>\n"
            + "        <backup-count>1</backup-count>\n"
            + "        <async-backup-count>0</async-backup-count>\n"
            + "        <empty-queue-ttl>-1</empty-queue-ttl>\n"
            + "    </queue>\n"
            + "    <map name=\"default\">\n"
            + "        <in-memory-format>BINARY</in-memory-format>\n"
            + "        <backup-count>1</backup-count>\n"
            + "        <async-backup-count>0</async-backup-count>\n"
            + "        <time-to-live-seconds>0</time-to-live-seconds>\n"
            + "        <max-idle-seconds>0</max-idle-seconds>\n"
            + "        <eviction-policy>NONE</eviction-policy>\n"
            + "        <max-size policy=\"PER_NODE\">0</max-size>\n"
            + "        <eviction-percentage>25</eviction-percentage>\n"
            + "        <min-eviction-check-millis>100</min-eviction-check-millis>\n"
            + "        <merge-policy>com.hazelcast.map.merge.PutIfAbsentMapMergePolicy</merge-policy>\n"
            + "        <cache-deserialized-values>INDEX-ONLY</cache-deserialized-values>\n"
            + "    </map>\n"
            + "    <map name=\"SmileBaseCache\">\n"
            + "        <in-memory-format>BINARY</in-memory-format>\n"
            + "        <backup-count>1</backup-count>\n"
            + "        <async-backup-count>0</async-backup-count>\n"
            + "        <time-to-live-seconds>0</time-to-live-seconds>\n"
            + "        <max-idle-seconds>1209600</max-idle-seconds>\n"
            + "        <eviction-policy>NONE</eviction-policy>\n"
            + "        <max-size policy=\"PER_NODE\">10000</max-size>\n"
            + "        <eviction-percentage>25</eviction-percentage>\n"
            + "        <min-eviction-check-millis>100</min-eviction-check-millis>\n"
            + "        <merge-policy>com.hazelcast.map.merge.PutIfAbsentMapMergePolicy</merge-policy>\n"
            + "        <cache-deserialized-values>INDEX-ONLY</cache-deserialized-values>\n"
            + "        <near-cache>\n"
            + "           <time-to-live-seconds>0</time-to-live-seconds>\n"
            + "           <max-idle-seconds>1209600</max-idle-seconds>\n"
            + "           <invalidate-on-change>true</invalidate-on-change>\n"
            + "           <eviction eviction-policy=\"LFU\"\n"
            + "            max-size-policy=\"ENTRY_COUNT\"\n"
            + "            size=\"10000\"/>"
            + "           <in-memory-format>OBJECT</in-memory-format>\n"
            + "        </near-cache>"
            + "    </map>\n"
            + "    <multimap name=\"default\">\n"
            + "        <backup-count>1</backup-count>\n"
            + "        <value-collection-type>SET</value-collection-type>\n"
            + "    </multimap>\n"
            + "    <list name=\"default\">\n"
            + "        <backup-count>1</backup-count>\n"
            + "    </list>\n"
            + "    <set name=\"default\">\n"
            + "        <backup-count>1</backup-count>\n"
            + "    </set>\n"
            + "    <jobtracker name=\"default\">\n"
            + "        <max-thread-size>0</max-thread-size>\n"
            + "        <queue-size>0</queue-size>\n"
            + "        <retry-count>0</retry-count>\n"
            + "        <chunk-size>1000</chunk-size>\n"
            + "        <communicate-stats>true</communicate-stats>\n"
            + "        <topology-changed-strategy>CANCEL_RUNNING_OPERATION</topology-changed-strategy>\n"
            + "    </jobtracker>\n"
            + "    <semaphore name=\"default\">\n"
            + "        <initial-permits>0</initial-permits>\n"
            + "        <backup-count>1</backup-count>\n"
            + "        <async-backup-count>0</async-backup-count>\n"
            + "    </semaphore>\n"
            + "    <reliable-topic name=\"default\">\n"
            + "        <read-batch-size>10</read-batch-size>\n"
            + "        <topic-overload-policy>BLOCK</topic-overload-policy>\n"
            + "        <statistics-enabled>true</statistics-enabled>\n"
            + "    </reliable-topic>\n"
            + "    <ringbuffer name=\"default\">\n"
            + "        <capacity>10000</capacity>\n"
            + "        <backup-count>1</backup-count>\n"
            + "        <async-backup-count>0</async-backup-count>\n"
            + "        <time-to-live-seconds>0</time-to-live-seconds>\n"
            + "        <in-memory-format>BINARY</in-memory-format>\n"
            + "    </ringbuffer>\n"
            + "    <serialization>\n"
            + "        <portable-version>0</portable-version>\n"
            + "    </serialization>\n"
            + "    <services enable-defaults=\"true\"/>\n"
            + "    <lite-member enabled=\"false\"/>\n"
            + "</hazelcast>";

    private static final String HAZELCAST_CLIENT_CONFIG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<hazelcast-client xsi:schemaLocation=\n"
            + "    \"http://www.hazelcast.com/schema/client-config hazelcast-client-config-3.8.xsd\"\n"
            + "                  xmlns=\"http://www.hazelcast.com/schema/client-config\"\n"
            + "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
            + "    <properties>\n"
            + "        <property name=\"hazelcast.shutdownhook.enabled\">false</property>\n"
            + "        <property name=\"hazelcast.client.request.retry.count\">5</property>\n"
            + "        <property name=\"hazelcast.client.request.retry.wait.time\">10</property>\n"
            + "        <property name=\"hazelcast.client.invocation.timeout.seconds\">2</property>\n"
            + "        <property name=\"hazelcast.client.heartbeat.interval\">5000</property>\n"
            + "        <property name=\"hazelcast.client.heartbeat.timeout\">17000</property>\n"
            + "    </properties>"
            + "    <group>\n"
            + "        <name>SmileGroup</name>\n"
            + "        <password>Huhi78gAmldo9w27</password>\n"
            + "    </group>\n"
            + "    <network>\n"
            + "        <connection-timeout>2000</connection-timeout>\n"
            + "        <connection-attempt-period>1000</connection-attempt-period>\n"
            + "        <connection-attempt-limit>3600</connection-attempt-limit>"
            + "        <smart-routing>true</smart-routing>\n"
            + "        <redo-operation>false</redo-operation>"
            + "    </network>\n"
            + "    <near-cache name=\"SmileBaseCache\">\n"
            + "        <time-to-live-seconds>0</time-to-live-seconds>\n"
            + "        <max-idle-seconds>1209600</max-idle-seconds>\n"
            + "        <eviction eviction-policy=\"LFU\"\n"
            + "            max-size-policy=\"ENTRY_COUNT\"\n"
            + "            size=\"10000\"/>"
            + "        <invalidate-on-change>true</invalidate-on-change>\n"
            + "        <in-memory-format>OBJECT</in-memory-format>\n"
            + "     </near-cache>"
            + "</hazelcast-client>";
}
