/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.sca.TTIssueQuery;
import com.smilecoms.commons.sca.beans.TTIssueBean;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class TTIssueResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(TTIssueResource.class);
    private final List<TTIssueBean> issues;

    public TTIssueResource(String issueId) {
        issues = new ArrayList<>();
        issues.add(TTIssueBean.getIssueById(issueId));
    }

    public TTIssueResource(TTIssueQuery query) {
        issues = TTIssueBean.getIssues(query);
    }
    
    public TTIssueResource(TTIssueBean issue) {
         issues = new ArrayList<>();
         issues.add(issue);
    }

    public List<TTIssueBean> getIssues() {
        log.debug("In getIssues");
        return issues;
    }
}
