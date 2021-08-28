/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.util;

import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class SSH {
    
    private static final Logger myLogger = LoggerFactory.getLogger(SSH.class);
    
    public static String executeRemoteOSCommand(String username, String password, String hostname, String cmd) {
        String ret = null;
        ByteArrayOutputStream baosStderr = null;
        ByteArrayOutputStream baosStdout = null;
        SessionChannelClient session = null;
        SshClient ssh = null;
        if (myLogger.isDebugEnabled()) {
            myLogger.debug("Going to run command [" + cmd + "] on host [" + hostname + "]");
        }
        try {
            if (myLogger.isDebugEnabled()) {
                myLogger.debug("Initialising");
            }
            ConfigurationLoader.initialize(false);
            if (myLogger.isDebugEnabled()) {
                myLogger.debug("Initialised");
            }
            // Make a client connection
            ssh = new SshClient();
            ssh.setSocketTimeout(30000);

            SshConnectionProperties properties = new SshConnectionProperties();
            properties.setHost(hostname);
            properties.setPrefPublicKey("ssh-dss");

            // Connect to the host
            if (myLogger.isDebugEnabled()) {
                myLogger.debug("SSH Client connecting to " + hostname);
            }
            ssh.connect(properties, new IgnoreHostKeyVerification());
            if (myLogger.isDebugEnabled()) {
                myLogger.debug("SSH Client Connected to " + hostname);
            }
            // Create a password authentication instance
            PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();
            // Get the users name
            pwd.setUsername(username);
            pwd.setPassword(password);

            // Try the authentication
            if (myLogger.isDebugEnabled()) {
                myLogger.debug("SSH Client Authenticating");
            }
            int result = ssh.authenticate(pwd);
            // Evaluate the result
            if (result == AuthenticationProtocolState.COMPLETE) {
                if (myLogger.isDebugEnabled()) {
                    myLogger.debug("SSH Client Authenticated");
                }
                //Connected and authenticated
                byte buffer[] = new byte[255];
                int read;
                baosStdout = new ByteArrayOutputStream();
                baosStderr = new ByteArrayOutputStream();
                if (myLogger.isDebugEnabled()) {
                    myLogger.debug("Opening session channel");
                }
                session = ssh.openSessionChannel();
                InputStream outIn = session.getInputStream();
                InputStream errIn = session.getStderrInputStream();
                if (myLogger.isDebugEnabled()) {
                    myLogger.debug("Execute command...");
                }
                session.executeCommand(cmd);
                if (myLogger.isDebugEnabled()) {
                    myLogger.debug("Executed command");
                }
                int breakOut = 0;
                while ((read = outIn.read(buffer)) > 0 && (breakOut < 100000)) {
                    baosStdout.write(buffer, 0, read);
                    breakOut++;
                }
                breakOut = 0;
                while ((read = errIn.read(buffer)) > 0 && (breakOut < 100000)) {
                    baosStderr.write(buffer, 0, read);
                    breakOut++;
                }
                ret = baosStderr.toString() + baosStdout.toString();
            } else if (myLogger.isDebugEnabled()) {
                myLogger.debug("SSH Client Failed Authentication. Result code was: " + result);
            }
        } catch (Exception e) {
            myLogger.warn("Error running ssh command on host " + hostname + " : " + e.toString());
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    myLogger.warn("Error closing SSH session: " + e.toString());
                }
            }
            if (ssh != null) {
                ssh.disconnect();
            }
            if (baosStdout != null) {
                try {
                    baosStdout.close();
                } catch (Exception e) {
                    myLogger.warn("Error closing ByteArrayOutputStream: " + e.toString());
                }
            }
            if (baosStderr != null) {
                try {
                    baosStderr.close();
                } catch (Exception e) {
                    myLogger.warn("Error closing ByteArrayOutputStream: " + e.toString());
                }
            }
        }

        if (myLogger.isDebugEnabled()) {
            myLogger.debug("Finished running command. Result was: ");
            myLogger.debug(ret);
        }
        return ret;
    }
    
}
