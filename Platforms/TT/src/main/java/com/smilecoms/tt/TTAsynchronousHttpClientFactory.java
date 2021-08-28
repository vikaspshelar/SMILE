/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tt;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.httpclient.apache.httpcomponents.DefaultHttpClient;
import com.atlassian.httpclient.api.Request;
import com.atlassian.httpclient.api.factory.HttpClientOptions;
import com.atlassian.httpclient.spi.ThreadLocalContextManagers;
import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AtlassianHttpClientDecorator;
import com.atlassian.jira.rest.client.internal.async.DisposableHttpClient;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.util.concurrent.Effect;
import com.smilecoms.commons.base.BaseUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class TTAsynchronousHttpClientFactory {

    private static final Logger log = LoggerFactory.getLogger(TTAsynchronousHttpClientFactory.class);

    public DisposableHttpClient createClient(final URI serverUri, final AuthenticationHandler authenticationHandler) {
        final HttpClientOptions options = new HttpClientOptions();
        log.debug("Setting http client timeouts");
        options.setSocketTimeout(BaseUtils.getIntProperty("env.jira.sockettimeout.millis", 300000), TimeUnit.MILLISECONDS);
        options.setRequestTimeout(BaseUtils.getIntProperty("env.jira.requesttimeout.millis", 300000), TimeUnit.MILLISECONDS);
        options.setConnectionTimeout(BaseUtils.getIntProperty("env.jira.connecttimeout.millis", 5000), TimeUnit.MILLISECONDS);
        options.setRequestPreparer(new Effect<Request>() {
            @Override
            public void apply(final Request request) {
                authenticationHandler.configure(request);
            }
        });

        log.debug("Creating DefaultHttpClient");
        final DefaultHttpClient defaultHttpClient = new DefaultHttpClient(new NoOpEventPublisher(), new RestClientApplicationProperties(serverUri), ThreadLocalContextManagers.noop(), options);
        return new AtlassianHttpClientDecorator(defaultHttpClient) {
            @Override
            public void destroy() throws Exception {
                defaultHttpClient.destroy();
            }
        };
    }

    private static class NoOpEventPublisher implements EventPublisher {

        @Override
        public void publish(Object o) {
        }

        @Override
        public void register(Object o) {
        }

        @Override
        public void unregister(Object o) {
        }

        @Override
        public void unregisterAll() {
        }
    }

    /**
     * 98 * These properties are used to present JRJC as a User-Agent during
     * http requests.
     */
    @SuppressWarnings("deprecation")
    private static class RestClientApplicationProperties implements ApplicationProperties {

        private final String baseUrl;

        private RestClientApplicationProperties(URI jiraURI) {
            this.baseUrl = jiraURI.getPath();
        }

        @Override
        public String getBaseUrl() {
            return baseUrl;
        }

        @Override
        public String getDisplayName() {
            return "Atlassian JIRA Rest Java Client";
        }

        @Override
        public String getVersion() {
            return MavenUtils.getVersion("com.atlassian.jira", "jira-rest-java-com.atlassian.jira.rest.client");
        }

        @Override
        public Date getBuildDate() {
            // TODO implement using MavenUtils, JRJC-123
            throw new UnsupportedOperationException();
        }

        @Override
        public String getBuildNumber() {
            // TODO implement using MavenUtils, JRJC-123
            return String.valueOf(0);
        }

        @Override
        public File getHomeDirectory() {
            return new File(".");
        }

        @Override
        public String getPropertyValue(final String s) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    private static final class MavenUtils {

        private static final Logger logger = LoggerFactory.getLogger(MavenUtils.class);

        private static final String DEFAULT_VERSION = "3.0.1";

        static String getVersion(String groupId, String artifactId) {
            final Properties props = new Properties();
            InputStream resourceAsStream = null;
            try {
                resourceAsStream = MavenUtils.class.getResourceAsStream(String.format("/META-INF/maven/%s/%s/pom.properties", groupId, artifactId));
                props.load(resourceAsStream);
                return props.getProperty("version", DEFAULT_VERSION);
            } catch (Exception e) {
                logger.debug("Could not find version for maven artifact {}:{}", groupId, artifactId);
                //logger.debug("Got the following exception", e);
                return DEFAULT_VERSION;
            } finally {
                if (resourceAsStream != null) {
                    try {
                        resourceAsStream.close();
                    } catch (IOException ioe) {
                        // ignore
                    }
                }
            }
        }
    }

}
