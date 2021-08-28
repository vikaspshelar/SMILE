/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca;

import com.smilecoms.commons.util.Utils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class CallersRequestContext implements Serializable {

    private String remoteUser;
    private String remoteAddr;
    private final Set<String> usersRoles = new HashSet();
    private int customerProfileId;
    private String tenant;
    private static final Logger log = LoggerFactory.getLogger(CallersRequestContext.class);

    public CallersRequestContext(String remoteUser, String remoteAddr, int customerProfileId, Collection<String> usersRoles) {
        this.remoteUser = remoteUser;
        this.remoteAddr = remoteAddr;
        this.customerProfileId = customerProfileId;
        this.usersRoles.addAll(usersRoles);
        if (this.remoteUser != null && !this.remoteUser.isEmpty()) {
            try {
                this.tenant = SCAWrapper.getTenantBySSOIdentity(this.remoteUser);
            } catch (Exception e) {
                log.warn("Error getting tenant", e);
            }
        }
    }

    public int getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(int customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }

    public String getRemoteUser() {
        return remoteUser;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public Set<String> getUsersRoles() {
        return usersRoles;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public CallersRequestContext(HttpServletRequest request) {
        this.remoteUser = request.getRemoteUser();
        this.remoteAddr = Utils.getRemoteIPAddress(request);
        this.customerProfileId = 0;
        for (String role : getRolesInWebXML(request)) {
            if (request.isUserInRole(role)) {
                usersRoles.add(Utils.getFriendlyRoleName(role));
            }
        }
        if (request.getRemoteUser() != null && !request.getRemoteUser().isEmpty()) {
            try {
                this.tenant = SCAWrapper.getTenantBySSOIdentity(this.remoteUser);
            } catch (Exception e) {
                log.warn("Error getting tenant", e);
            }
        }
    }

    private static List<String> rolesinWebXML = null;

    private List<java.lang.String> getRolesInWebXML(HttpServletRequest sr) {
        if (rolesinWebXML != null) {
            return rolesinWebXML;
        }
        log.debug("Getting roles from web.xml");

        ByteArrayInputStream fIS = null;
        try {
            fIS = (ByteArrayInputStream) sr.getServletContext().getResourceAsStream("/WEB-INF/web.xml");
            if (fIS == null) {
                log.warn("Cannot locate web.xml to read security roles");
            }
            DataInputStream in = new DataInputStream(fIS);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            rolesinWebXML = new java.util.concurrent.CopyOnWriteArrayList<>();
            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                // Print the content on the console
                int roleNameLoc = strLine.indexOf("<role-name>");
                if (roleNameLoc != -1) {
                    int roleStart = roleNameLoc + 11;
                    int endTag = strLine.indexOf("</role-name>");
                    String role = strLine.substring(roleStart, endTag);
                    log.debug("Found a role in web.xml called [{}]", role);
                    rolesinWebXML.add(role);
                }
                if (strLine.contains("</auth-constraint>")) {
                    // finished parsing roles
                    break;
                }
            }
        } catch (Exception e) {
            rolesinWebXML = null;
            log.warn("error", e);
        } finally {
            try {
                if (fIS != null) {
                    fIS.close();
                }
            } catch (IOException ex) {
            }
        }
        return rolesinWebXML;
    }

}
