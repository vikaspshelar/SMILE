/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 */

package com.smilecoms.commons.base.hazelcast.tomcat;

import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import javax.servlet.ServletException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastSessionCommitValve extends ValveBase {

    private final Logger log = LoggerFactory.getLogger(HazelcastSessionCommitValve.class);

    private final SessionManager sessionManager;

    public HazelcastSessionCommitValve(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            getNext().invoke(request, response);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            final Session session = request.getSessionInternal(false);
            storeOrRemoveSession(session);
        }
    }

    private void storeOrRemoveSession(Session session) {
        if (session != null) {
            if (session.isValid()) {
                log.debug("Request with session completed, saving session [{}] if necessary", session.getId());
                if (session.getSession() != null) {
                    sessionManager.commit(session);
                }
            } else {
                log.debug("HTTP Session has been invalidated, removing : [{}]", session.getId());
                sessionManager.remove(session);
            }
        }
    }
}