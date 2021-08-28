package com.smilecoms.commons.base.sd;

import com.hazelcast.query.Predicate;
import com.smilecoms.commons.base.BaseUtils;
import java.io.Serializable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthCheckPredicate implements Predicate<String, IService>, Serializable {
    private static final transient Logger log = LoggerFactory.getLogger(HealthCheckPredicate.class);
    private final String hostname = BaseUtils.getHostNameFromKernel();
    private final String jvmid = BaseUtils.JVM_ID;
    private static final long serialVersionUID = 1;
    
    @Override
    public boolean apply(Map.Entry<String, IService> entry) {
        try {
            boolean ret = entry.getValue().doesNeedHealthCheck(hostname, jvmid);
            log.debug("Ran healthcheck predicate for host [{}] on [{}] and got [{}]", new Object[]{hostname, entry.getValue(), ret});
            return ret;
        } catch (Exception e) {
            log.warn("Error applying mayNeedHealthCheck predicate: ", e);
            return false;
        }
    }
}
