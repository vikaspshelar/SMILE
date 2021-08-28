package com.smilecoms.commons.scaauth;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SCASPILoginModule implements LoginModule {
    
    private CallbackHandler handler;
    private Subject subject;
    private UserPrincipal userPrincipal;
    private RolePrincipal rolePrincipal;
    private String login;
    private String[] userGroups;
    private static final Logger log = LoggerFactory.getLogger(SCASPILoginModule.class);
    public static final String SCA_CLIENT_ID_KEY = "SCA_CLIENT_ID";
    public static final String SCA_CLIENT_ID = System.getProperty(SCA_CLIENT_ID_KEY);
    private static SCAAuthenticator authenticator = null;
    
    static {
        log.warn("SCASPILoginModule is being created");
    }
    
    @Override
    public void initialize(Subject subject,
            CallbackHandler callbackHandler,
            Map<String, ?> sharedState,
            Map<String, ?> options) {
        
        handler = callbackHandler;
        this.subject = subject;
    }
    
    @Override
    public boolean login() throws LoginException {
        
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("login");
        callbacks[1] = new PasswordCallback("password", true);
        
        try {
            handler.handle(callbacks);
            String name = ((NameCallback) callbacks[0]).getName();
            String password = String.valueOf(((PasswordCallback) callbacks[1]).getPassword());
            
            if (log.isDebugEnabled()) {
                log.debug("Logging in customer " + name);
            }
            
            userGroups = authUser(name, password, SCA_CLIENT_ID);
            login = name;
            return true;
            
        } catch (Exception e) {
            log.debug("Error in login", e);
            throw new LoginException(e.getMessage());
        }
        
    }
    
    @Override
    public boolean commit() throws LoginException {
        
        userPrincipal = new UserPrincipal(login);
        subject.getPrincipals().add(userPrincipal);
        
        if (userGroups != null) {
            for (String groupName : userGroups) {
                rolePrincipal = new RolePrincipal(groupName);
                subject.getPrincipals().add(rolePrincipal);
            }
        }
        
        return true;
    }
    
    @Override
    public boolean abort() throws LoginException {
        return false;
    }
    
    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().remove(userPrincipal);
        subject.getPrincipals().remove(rolePrincipal);
        return true;
    }

    /**
     * Call SCA on callback class to authenticate the user and password
     *
     * @param user
     * @param pass
     * @param application
     * @return Array of groups the user is in
     * @throws javax.security.auth.login.LoginException
     */
    private String[] authUser(String user, String pass, String application) throws LoginException {
        
        SCAAuthenticator auth = getAuthenticator();
        String[] ret = null;
        if (auth == null) {
            log.warn("Authenticator is null. Cannot authenticate customers at this time");
            throw new LoginException("Authenticator is null");
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("About to use authenticator to authenticate customer " + user + " on application " + application);
            }
            ret = auth.authenticate(user, pass, application);
            if (ret == null) {
                throw new LoginException("Return from SCA is null");
            }
        } catch (Exception e) {
            log.info("Error authenticating customer " + user + ": " + e.toString());
            throw new LoginException(e.toString());
        }
        return ret;
    }
    
    public static SCAAuthenticator getAuthenticator() {
        if (log.isDebugEnabled()) {
            log.debug("SCASPILoginModule: getAuthenticator()");
        }
        return authenticator;
    }
    
    public static void setAuthenticator(SCAAuthenticator authenticator) {
        log.warn("My authenticator has been set");
        SCASPILoginModule.authenticator = authenticator;
    }
    
}
