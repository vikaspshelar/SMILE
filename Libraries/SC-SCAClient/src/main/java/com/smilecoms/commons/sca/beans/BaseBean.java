/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.sca.Errors;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAWrapper;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public abstract class BaseBean {
    
    private static final Logger log = LoggerFactory.getLogger(BaseBean.class);
    
    static boolean isAllowed(String roles) {
        Set usersRoles = SCAWrapper.getUserSpecificInstance().getThreadsRequestContext().getUsersRoles();
        StringTokenizer stValues = new StringTokenizer(roles, "\r\n");
        while (stValues.hasMoreTokens()) {
            String role = stValues.nextToken().trim();
            if (!role.isEmpty() && usersRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }
    
    protected static void checkForNoResult(List list) {
        if (list == null || list.isEmpty()) {
            SCABusinessError b = new SCABusinessError();
            b.setErrorCode(Errors.ERROR_CODE_SCA_NO_RESULT);
            b.setErrorDesc("No results found");
            throw b;
        }
    }
}
