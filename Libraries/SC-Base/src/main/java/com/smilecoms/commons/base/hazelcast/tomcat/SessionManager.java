/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 */

package com.smilecoms.commons.base.hazelcast.tomcat;

import com.hazelcast.core.IMap;
import org.apache.catalina.Session;

import java.io.IOException;

public interface SessionManager {

    void remove(Session session);

    void commit(Session session);

    String updateJvmRouteForSession(String sessionId, String newJvmRoute) throws IOException;

    String getJvmRoute();

    IMap<String, HazelcastSession> getDistributedMap();

}