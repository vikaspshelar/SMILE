package com.smilecoms.commons.base.hazelcast.tomcat;

import com.hazelcast.core.IMap;
import com.smilecoms.commons.base.hazelcast.HazelcastHelper;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Session;
import org.apache.catalina.session.ManagerBase;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import org.apache.catalina.session.StandardSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmileHazelcastSessionManager extends ManagerBase implements Lifecycle, PropertyChangeListener, SessionManager {

    private static final String NAME = "SmileHazelcastSessionManager";

    private final Logger log = LoggerFactory.getLogger(SmileHazelcastSessionManager.class);

    private IMap<String, HazelcastSession> sessionMap;

    private String mapName;

    public void setSessionTimeout(int t) {
        log.debug("Setting session timeout to [{}]", t);
        getContext().setSessionTimeout(t);
    }

    private IMap<String, HazelcastSession> getSessionMap() {
        if (sessionMap == null) {
            sessionMap = HazelcastHelper.getBaseHazelcastInstance().getMap(mapName);
        }
        return sessionMap;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {
    }

    @Override
    public void unload() throws IOException {
    }

    @Override
    public void startInternal() throws LifecycleException {
        super.startInternal();
        super.generateSessionId();
        log.debug("In startInternal");

        configureValves();

        Context ctx = getContext();
        String contextPath = ctx.getServletContext().getContextPath();
        log.debug("contextPath:" + contextPath);
        if (contextPath == null || contextPath.equals("/") || contextPath.equals("")) {
            mapName = "smile_empty_session_replication";
        } else {
            mapName = contextPath.substring(1, contextPath.length()) + "_smile_session_replication";
        }
        log.debug("SmileHazelcastSessionManager started with map name [{}]", mapName);
        setState(LifecycleState.STARTING);
    }

    private void configureValves() {
        log.debug("Configuring valve");
        HazelcastSessionCommitValve hazelcastSessionCommitValve = new HazelcastSessionCommitValve(this);
        getContext().getPipeline().addValve(hazelcastSessionCommitValve);
    }

    @Override
    public void stopInternal() throws LifecycleException {
    }

    @Override
    public int getRejectedSessions() {
        // Essentially do nothing.
        return 0;
    }

    public void setRejectedSessions(int i) {
        // Do nothing.
    }

    @Override
    public Session createSession(String sessionId) {
        HazelcastSession session = (HazelcastSession) createEmptySession();

        session.setNew(true);
        session.setValid(true);
        session.setCreationTime(System.currentTimeMillis());
        session.setMaxInactiveInterval(getContext().getSessionTimeout() * 60);

        String newSessionId = sessionId;
        if (newSessionId == null) {
            newSessionId = generateSessionId();
        }

        session.setId(newSessionId);
        session.tellNew();

        sessions.put(newSessionId, session);
        log.debug("Created new session [{}] with timeout [{}]s", newSessionId, session.getMaxInactiveInterval());
        return session;
    }

    @Override
    public Session createEmptySession() {
        return new HazelcastSession(this);
    }

    @Override
    public void add(Session session) {
        sessions.put(session.getId(), (HazelcastSession) session);
        if (requiresFailover((HazelcastSession) session)) {
            getSessionMap().set(session.getId(), (HazelcastSession) session);
            if (log.isDebugEnabled()) {
                log.debug("Added session to Hazelcast. Total in Hazelcast is now [{}]", getSessionMap().size());
            }
        }
    }

    @Override
    public Session findSession(String id) throws IOException {
        log.debug("Finding sessionId: [{}]", id);
        if (id == null) {
            return null;
        }

        if (!sessions.containsKey(id)) {
            try {

                HazelcastSession hazelcastSession;
                log.debug("Some failover occured so reading session from Hazelcast map: [{}]", mapName);

                hazelcastSession = getSessionMap().get(id);
                if (hazelcastSession == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("No Session found even in Hazelcast for: [{}]. Here is what we have in map [{}]:", id, getSessionMap().getName());
                        for (String sesid : getSessionMap().keySet()) {
                            log.debug("Map has session id [{}]", sesid);
                        }
                        log.debug("Was in map but null [{}]", getSessionMap().containsKey(id));
                    }
                    return null;
                }

                hazelcastSession.access();
                hazelcastSession.endAccess();
                hazelcastSession.setSessionManager(this);
                sessions.put(id, hazelcastSession);

                // call remove method to trigger eviction Listener on each node to invalidate local sessions
                getSessionMap().delete(id);
                getSessionMap().set(id, hazelcastSession);
                log.debug("Great! Found a session in Hazelcast. User [{}] can transparently fail over", hazelcastSession.getPrincipal());
                return hazelcastSession;
            } catch (Exception e) {
                log.warn("Error getting session from Hazelcast. Continuing as though it does not exist", e);
                try {
                    getSessionMap().delete(id);
                } catch (Exception e2) {
                    log.warn("Error deleting: ", e2);
                }
                return null;
            }
        } else {
            Session ret = sessions.get(id);
            if (log.isDebugEnabled()) {
                log.debug("Successfully found local session [{}] Last Accessed [{}]ms ago, last accessed at [{}], max inactive [{}]", new Object[]{id, ret.getIdleTime(), ret.getLastAccessedTime(), ret.getMaxInactiveInterval()});
            }
            return ret;
        }
    }

    private boolean requiresFailover(HazelcastSession hazelcastSession) {
        boolean ret = hazelcastSession.getAttribute("FAILOVER") != null;
        log.debug("Session requires failover? [{}]", ret);
        return ret;
    }

    @Override
    public void commit(Session session) {
        HazelcastSession hazelcastSession = (HazelcastSession) session;
        if (requiresFailover(hazelcastSession) && hazelcastSession.isDirty()) {
            hazelcastSession.setDirty(false);
            getSessionMap().set(session.getId(), hazelcastSession);
            if (log.isDebugEnabled()) {
                log.debug("Committed session to Hazelcast with id: [{}] to map [{}]", session.getId(), getSessionMap().getName());
                for (String id : getSessionMap().keySet()) {
                    log.debug("Map has session id [{}]", id);
                }
            }
        }
    }

    @Override
    public String updateJvmRouteForSession(String sessionId, String newJvmRoute) throws IOException {
        log.debug("In updateJvmRouteForSession");
        return sessionId;
    }

    @Override
    public void remove(Session session) {
        remove(session.getId());
    }

    @Override
    public void remove(Session session, boolean update) {
        remove(session.getId());
    }

    @Override
    public IMap<String, HazelcastSession> getDistributedMap() {
        return getSessionMap();
    }

    private void remove(String id) {
        log.debug("Removing session with id [{}]", id);
        sessions.remove(id);
        getSessionMap().remove(id);
    }

    @Override
    public void expireSession(String sessionId) {
        log.debug("ExpireSession [{}]", sessionId);
        super.expireSession(sessionId);
        remove(sessionId);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("sessionTimeout")) {
            getContext().setSessionTimeout((Integer) evt.getNewValue() * 60);
        }
    }

    @Override
    public int getMaxActiveSessions() {
        return this.maxActiveSessions;
    }

    @Override
    public void setMaxActiveSessions(int maxActiveSessions) {
        int oldMaxActiveSessions = this.maxActiveSessions;
        this.maxActiveSessions = maxActiveSessions;
        this.support.firePropertyChange("maxActiveSessions", Integer.valueOf(oldMaxActiveSessions), Integer.valueOf(this.maxActiveSessions));
    }

}
