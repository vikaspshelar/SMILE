/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.paymentgateway.helpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class PaymentGatewayHelper {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayHelper.class);
    private static final String ENCODING = "UTF-8";
    private static final String POST = "POST";

    public static String sendHTTPRequest(String parameterStyle, String httpMethod, String requestURL, Map<String, String> requestHeaders, Map<String, String> requestParams, String caller) throws Exception {
        log.debug("{}: Passing request details to Apache HTTP Client. requestURL = [{}] requestHeaders=[{}] requestParams=[{}]", caller, requestURL, requestHeaders, requestParams);
        String res = invoke(parameterStyle, requestURL, httpMethod, addParameters(requestHeaders, caller), addParameters(requestParams, caller), caller);
        log.debug("{}: Apache HTTP Client is done executing request", caller);
        return res;
    }

    public static String sendHTTPRequest(String parameterStyle, String httpMethod, String requestURL, Map<String, String> requestHeaders, String entity, String caller) throws Exception {
        log.debug("{}: Passing request details to Apache HTTP Client. parameterStyle = [{}] httpMethod = [{}] requestURL = [{}] requestHeaders= [{}] entity = [{}]", caller, parameterStyle, httpMethod, requestURL, requestHeaders, entity);
        String res = invoke(parameterStyle, requestURL, httpMethod, addParameters(requestHeaders, caller), entity, caller);
        log.debug("{}: Apache HTTP Client is done executing request", caller);
        return res;
    }

    private static String invoke(String style, String requestURL, String httpMethod, List<Map.Entry<String, String>> headers, List<Map.Entry<String, String>> requestParams, String caller) throws Exception {
        return invoke(style, requestURL, httpMethod, headers, requestParams, null, caller);
    }

    private static String invoke(String style, String requestURL, String httpMethod, List<Map.Entry<String, String>> headers, String entity, String caller) throws Exception {
        return invoke(style, requestURL, httpMethod, headers, null, entity, caller);
    }

    private static String invoke(String style, String requestURL, String httpMethod, List<Map.Entry<String, String>> headers, List<Map.Entry<String, String>> requestParams, String entity, String caller) throws Exception {

        InputStream body = null;

        switch (style) {
            case "QUERY_STRING":
                requestURL = constructGetURL(requestURL, requestParams, caller);
                break;
            case "X_WWW_FORM_URLENCODED":
                byte[] form = formEncode(requestParams, caller).getBytes(ENCODING);
                body = new ByteArrayInputStream(form);
                break;
            case "BODY": {
                if (entity == null) {
                    entity = "";
                }
                body = new ByteArrayInputStream(entity.getBytes(ENCODING));
                break;
            }
        }
        log.debug("URL to invoke is: {}", requestURL);
        return execute(requestURL, httpMethod, headers, body, caller);
    }

    private static String execute(String resourceURL, String requestMethod, List<Map.Entry<String, String>> headers, InputStream in, String caller) throws Exception {
        URL stringURL = new URL(resourceURL);
        final String url = stringURL.toExternalForm();
        final InputStream body = in;
        final boolean isPost = POST.equalsIgnoreCase(requestMethod);
        HttpMethod httpMethod;

        log.debug("URL is: {}", url);
        
        if (isPost) {
            EntityEnclosingMethod entityEnclosingMethod = new PostMethod(url);
            if (body != null) {
                //entityEnclosingMethod.setRequestEntity((length == null) ? new InputStreamRequestEntity(body) : new InputStreamRequestEntity(body, Long.parseLong(length)));
                entityEnclosingMethod.setRequestEntity(new InputStreamRequestEntity(body));
            }
            httpMethod = entityEnclosingMethod;
        } else {
            httpMethod = new GetMethod(url);
        }

        httpMethod.setFollowRedirects(false);

        if (log.isDebugEnabled()) {
            log.debug("********** {}: Request Headers **********", caller);
            for (Map.Entry<String, String> header : headers) {
                log.debug(header.getKey() + ":" + header.getValue());
            }
            log.debug("********** {}: End of Request Headers **********", caller);
        }

        for (Map.Entry<String, String> header : headers) {
            httpMethod.addRequestHeader(header.getKey(), header.getValue());
        }
        
        String responseBody = "";
        String statusLine;

        HttpClient client = getHHTPClient(caller);

        try {
            long start = System.currentTimeMillis();
            client.executeMethod(httpMethod);
            long end = System.currentTimeMillis();
            long callDuration = end - start;
            log.debug("{}: Finished calling service provider. Request took " + callDuration + "ms", caller);

            if (log.isDebugEnabled()) {
                log.debug("********** {}: Response Headers Received **********", caller);
                for (Header header : httpMethod.getResponseHeaders()) {
                    String name = header.getName();
                    String value = header.getValue();
                    log.debug(name + ":" + value);
                }
                log.debug("********** {}: End of Response Headers **********", caller);
            }

            statusLine = httpMethod.getStatusLine().toString();
            responseBody = parseStreamToString(httpMethod.getResponseBodyAsStream());
            log.debug("{}: Apache Http client raw response: status: {}, responseBody: {}", new Object[]{caller, statusLine, responseBody});

        } catch (Exception ex) {
            log.warn("{}: Error occured trying to execute http method. Reason:{} ", caller, ex.toString());
            throw new Exception(ex);
        } finally {
            try {
                log.debug("{}: Done using the connection, releasing it now.", caller);
                httpMethod.releaseConnection();
            } catch (Exception ex) {
                log.debug("{}: Error releasing http connection:{}", caller, ex.toString());
            }
            try {
                if (client != null) {
                    log.debug("{}: Shutting down connection manager", caller);
                    ((SimpleHttpConnectionManager) client.getHttpConnectionManager()).shutdown();
                }
            } catch (Exception ex) {
                log.debug("{}: Error releasing http client connection:{}", ex.toString(), caller);
            }
        }

        return responseBody;
    }

    private static HttpClient getHHTPClient(String caller) {
        log.debug("{}: Creating new Apache Http client instance", caller);
        HttpClient client = new HttpClient();
        HttpMethodRetryHandler retryHandler = new HttpMethodRetryHandler() {
            @Override
            public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount) {
                if (executionCount >= 5) {
                    //Do not retry if over max retry count
                    return false;
                }
                if (exception instanceof InterruptedIOException) {
                    //timeout
                    return false;
                }
                if (exception instanceof UnknownHostException) {
                    //unknow host
                    return false;
                }
                if (exception instanceof ConnectException) {
                    //connection refused
                    return false;
                }
                if (exception instanceof SSLException) {
                    //handshake exception
                    return false;
                }
                return method.getName().equalsIgnoreCase("GET");
            }
        };

        HttpParams httpParams = HttpClientParams.getDefaultParams();
        httpParams.setParameter(HttpClientParams.RETRY_HANDLER, retryHandler);
        httpParams.setIntParameter(HttpClientParams.SO_TIMEOUT, 50000);
        httpParams.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 10000);
        client.setParams((HttpClientParams) httpParams);
        client.setHttpConnectionManager(new SimpleHttpConnectionManager());

        //http_proxy=http://UGBUK-SQUID1.it.ug.smilecoms.com:8800
        String proxCon = System.getenv("HTTP_PROXY");
        if (proxCon != null) {
            String[] poxyConArray = proxCon.split(":");
            String host = poxyConArray[1].substring(2);
            int port = Integer.parseInt(poxyConArray[2]);
            log.debug("{}: SQUID is on host[{}] and listening on port[{}]......", new Object[]{caller, host, port});
            client.getHostConfiguration().setProxy(host, port);
        } else {
            log.debug("{}: env variable: HTTP_PROXY is not set on this host. Not going to configure SQUID on the Apache http client.", caller);
        }
        log.debug("{}: Created new Apache Http client instance, returning it", caller);
        return client;
    }

    private static String constructGetURL(String url, Iterable<? extends Map.Entry<String, String>> parameters, String caller) throws IOException {
        String form = formEncode(parameters, caller);
        if (form == null || form.length() <= 0) {
            return url;
        } else {
            return url + ((!url.contains("?")) ? "?" : "&") + form;
        }
    }

    private static List<Map.Entry<String, String>> addParameters(Map<String, String> pMap, String caller) throws Exception {
        log.debug("{}: Inside addParameters", caller);
        List<Map.Entry<String, String>> defaultParams = new ArrayList<>();
        if (pMap == null) {
            return defaultParams;
        }
        for (Map.Entry entry : pMap.entrySet()) {
            defaultParams.add(new Parameter((String) entry.getKey(), (String) entry.getValue()));
        }
        log.debug("{}: exiting addParameters", caller);
        return defaultParams;
    }

    private static String formEncode(Iterable<? extends Map.Entry> parameters, String caller) throws IOException {
        StringBuilder into = new StringBuilder();
        if (parameters != null) {
            boolean first = true;
            for (Map.Entry parameter : parameters) {
                if (first) {
                    first = false;
                } else {
                    into.append("&");
                }
                into.append(percentEncode((String) (parameter.getKey()), caller));
                into.append("=");
                into.append(percentEncode((String) (parameter.getValue()), caller));
            }
        }
        String ret = into.toString();
        log.debug("{}: Ruturning form-urlencoded string: {}", caller, ret);
        return ret;
    }

    private static String percentEncode(String s, String caller) {
        if (s == null) {
            return "";
        }
        log.debug("{}: Validate encoding for: string: {}", caller, s);
        try {
            return URLEncoder.encode(s, ENCODING).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        } catch (UnsupportedEncodingException wow) {
            throw new RuntimeException(wow.getMessage(), wow);
        }
    }

    private static String parseStreamToString(java.io.InputStream is) throws Exception {
        byte[] buffer = new byte[1024];
        OutputStream outputStream = new ByteArrayOutputStream();

        while (true) {
            int read = is.read(buffer);
            if (read == -1) {
                break;
            }
            outputStream.write(buffer, 0, read);
        }
        outputStream.close();
        is.close();
        return outputStream.toString();
    }

}
