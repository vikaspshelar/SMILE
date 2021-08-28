/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.util;

import com.smilecoms.commons.base.BaseUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLException;
import org.apache.commons.codec.binary.Base64;
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
public class Oauth1Utils {

    private static final String CLASS = Oauth1Utils.class.getName();
    private static final Logger myLogger = LoggerFactory.getLogger(CLASS);
    private static final Base64 BASE64 = new Base64();
    private static final String POST = "POST";
    private static final ConcurrentHashMap<String, String> oauth1KeysMap = new ConcurrentHashMap<>();
    private static long cacheLastReaped = 0;

    private static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";
    private static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
    private static final String OAUTH_SIGNATURE = "oauth_signature";
    private static final String OAUTH_TIMESTAMP = "oauth_timestamp";
    private static final String OAUTH_NONCE = "oauth_nonce";
    private static final String OAUTH_VERSION = "oauth_version";
    private static final String OAUTH_CALLBACK = "oauth_callback";
    private static final String AUTH_SCHEME = "OAuth";

    private static final String HMAC_SHA1 = "HmacSHA1";
    private static final String ENCODING = "UTF-8";
    private static final String VERSION_1_0 = "1.0";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String FORM_ENCODED = "application/x-www-form-urlencoded";

    public static String getHttpRequestResponse(String requestURL, String httpMethod, Map<String, String> resourceParams, String callback, String propName) throws Exception {
        String style = "BODY";
        if (httpMethod.equalsIgnoreCase("get")) {
            style = "QUERY_STRING";
        }
        HttpMsgResponse response = invoke(style, requestURL, httpMethod, resourceParams, callback, propName);
        return response.getResponseBody();
    }

    public static String getAbsoluteUrl(String reqUrl, String httpMethod, Map<String, String> resourceParams, String callback, String propName) throws Exception {
        myLogger.debug("inside getAbsoluteUrl");
        StringBuilder into = new StringBuilder();
        List<Map.Entry<String, String>> parameters = addRequiredParameters(resourceParams, reqUrl, httpMethod, callback, propName);
        if (parameters != null) {
            int count = 0;
            for (Map.Entry<String, String> parameter : parameters) {
                String name = parameter.getKey();
                if (!reqUrl.contains("?") && count == 0) {
                    into.append("?").append(name).append("=");
                } else {
                    into.append("&").append(name).append("=");
                }
                into.append(percentEncode(parameter.getValue()));
                count++;
            }
            String fullURL = reqUrl + into.toString();
            myLogger.debug("exiting getAbsoluteUrl: {}", fullURL);
            return fullURL;
        }
        return "";
    }

    public static List<Map.Entry<String, String>> addRequiredParameters(Map<String, String> pMap, String url, String httpMethod, String callback, String propName) throws Exception {
        myLogger.debug("Inside addRequiredParameters");
        checkIfLocalCacheStillFresh();
        List<Map.Entry<String, String>> otherParams = new ArrayList<>();
        String cacheKey = "CONSUMER_KEY_" + propName;
        String keyString = oauth1KeysMap.get(cacheKey);

        if (keyString == null) {
            try {
                int consumerKeyLen = "CONSUMER_KEY_".length();
                Set<String> lines = BaseUtils.getPropertyAsSet(propName);
                for (String line : lines) {
                    if (line.startsWith("CONSUMER_KEY_")) {
                        keyString = line.substring(consumerKeyLen);
                        oauth1KeysMap.put(cacheKey, keyString);
                        break;
                    }
                }
                if (keyString == null) {
                    myLogger.warn("Failed to get consumer key for property {}, signature might not match that of service provider!!!", propName);
                    keyString = "";
                }
            } catch (Exception ex) {
                String errMsg = "Failed to get oauth consumer key for property '" + propName + "', caused by: " + ex.toString();
                throw new Exception(errMsg);
            }
        }

        if (pMap.get(OAUTH_CONSUMER_KEY) == null) {
            otherParams.add(new Parameter(OAUTH_CONSUMER_KEY, keyString));
        }
        if (pMap.get(OAUTH_SIGNATURE_METHOD) == null) {
            otherParams.add(new Parameter(OAUTH_SIGNATURE_METHOD, "HMAC-SHA1"));
        }
        if (pMap.get(OAUTH_TIMESTAMP) == null) {
            otherParams.add(new Parameter(OAUTH_TIMESTAMP, (System.currentTimeMillis() / 1000) + ""));
        }
        if (pMap.get(OAUTH_NONCE) == null) {
            otherParams.add(new Parameter(OAUTH_NONCE, System.nanoTime() + ""));
        }
        if (pMap.get(OAUTH_VERSION) == null) {
            otherParams.add(new Parameter(OAUTH_VERSION, VERSION_1_0));
        }
        if (callback != null) {
            if (pMap.get(OAUTH_CALLBACK) == null) {
                otherParams.add(new Parameter(OAUTH_CALLBACK, callback));
            }
        }

        for (Map.Entry entry : pMap.entrySet()) {
            otherParams.add(new Parameter((String) entry.getKey(), (String) entry.getValue()));
        }

        sign(otherParams, url, httpMethod, propName);

        myLogger.debug("exiting addRequiredParameters");
        return otherParams;
    }

    public static String getAuthorizationHeader(String realm, List<Map.Entry<String, String>> others) throws Exception {
        StringBuilder into = new StringBuilder();
        if (realm != null) {
            into.append(" realm=\"").append(percentEncode(realm)).append('"');
        }
        if (others != null) {
            for (Map.Entry parameter : others) {
                String name = (String) parameter.getKey();
                if (name.startsWith("oauth_")) {
                    if (into.length() > 0) {
                        into.append(",");
                    }
                    into.append(" ");
                    into.append(percentEncode(name)).append("=\"");
                    into.append(percentEncode((String) (parameter.getValue()))).append('"');
                }
            }
        }
        return AUTH_SCHEME + into.toString();
    }

    public static String getSignature(List<Map.Entry<String, String>> pMap, String url, String httpMethod, String propName) throws Exception {
        myLogger.debug("Inside getSignature1");
        String baseString = getBaseString(pMap, url, httpMethod);
        String signature = getSignature(baseString, propName);
        if (myLogger.isDebugEnabled()) {
            myLogger.debug("signature is: {} , corresponding baseString is: ({})", new Object[]{signature, baseString});
        }
        myLogger.debug("exiting getSignature1");
        return signature;
    }

    public static String getSignature(String baseString, String propName) throws Exception {
        myLogger.debug("inside getSignature2");
        try {
            String signature = base64Encode(computeSignature(baseString, propName));
            myLogger.debug("exiting getSignature2");
            return signature;
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public boolean isValid(String signature, String baseString, String propName) throws Exception {
        try {
            byte[] expected = computeSignature(baseString, propName);
            byte[] actual = decodeBase64(signature);
            return Arrays.equals(expected, actual);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    private static byte[] computeSignature(String baseString, String propName) throws Exception {

        checkIfLocalCacheStillFresh();
        String cacheKey = "CONSUMER_SECRET_" + propName;
        String keyString = oauth1KeysMap.get(cacheKey);
        if (keyString == null) {
            try {
                int consumerSecretLen = "CONSUMER_SECRET_".length();
                Set<String> lines = BaseUtils.getPropertyAsSet(propName);
                for (String line : lines) {
                    if (line.startsWith("CONSUMER_SECRET_")) {
                        keyString = line.substring(consumerSecretLen);
                        oauth1KeysMap.put(cacheKey, keyString);
                        break;
                    }
                }
                if (keyString == null) {
                    throw new NullPointerException("Consumer secret key is not initialised for property '" + propName + "'");
                }
            } catch (Exception ex) {
                String errMsg = "Failed to get oauth secrete key for property '" + propName + "', caused by: " + ex.toString();
                throw new Exception(errMsg);
            }
        }

        keyString = percentEncode(keyString) + "&";
        byte[] keyBytes = keyString.getBytes(ENCODING);
        SecretKey key = new SecretKeySpec(keyBytes, HMAC_SHA1);
        Mac mac = Mac.getInstance(HMAC_SHA1);
        mac.init(key);
        byte[] text = baseString.getBytes(ENCODING);

        return mac.doFinal(text);
    }

    private static void checkIfLocalCacheStillFresh() {
        if (System.currentTimeMillis() > (cacheLastReaped + 30000)) {
            myLogger.debug("Marking cache as stale so we can get fresh values.");
            oauth1KeysMap.clear();
            cacheLastReaped = System.currentTimeMillis();
        } else {
            myLogger.debug("Cache values still fresh no need to refresh them.");
        }
    }

    private static byte[] decodeBase64(String s) {
        return BASE64.decode(s.getBytes());
    }

    private static String base64Encode(byte[] b) {
        return new String(BASE64.encode(b));
    }

    private static String decodePercent(String s) {
        try {
            return URLDecoder.decode(s, ENCODING);
        } catch (java.io.UnsupportedEncodingException wow) {
            throw new RuntimeException(wow.getMessage(), wow);
        }
    }

    private static String normalizeParameters(Collection<? extends Map.Entry> parameters) throws IOException {
        myLogger.debug("Inside normalizeParameters");
        if (parameters == null) {
            return "";
        }
        List<ComparableParameter> p = new ArrayList<>(parameters.size());
        for (Map.Entry parameter : parameters) {
            if (!OAUTH_SIGNATURE.equals(parameter.getKey())) {
                p.add(new ComparableParameter(parameter));
            }
        }
        Collections.sort(p);
        String normalizedParams = formEncode(getParameters(p));
        myLogger.debug("exiting normalizeParameters: {}", normalizedParams);
        return normalizedParams;
    }

    /**
     * Construct a form-urlencoded document containing the given sequence of
     * name/value pairs.
     */
    private static String formEncode(Iterable<? extends Map.Entry> parameters) throws IOException {
        StringBuilder into = new StringBuilder();
        if (parameters != null) {

            boolean first = true;
            for (Map.Entry parameter : parameters) {
                if (first) {
                    first = false;
                } else {
                    into.append("&");
                }
                into.append(percentEncode((String) (parameter.getKey())));
                into.append("=");
                into.append(percentEncode((String) (parameter.getValue())));
            }
        }
        String ret = into.toString();
        myLogger.debug("Ruturning form-urlencoded string: {}", ret);
        return ret;
    }

    private static List<Map.Entry> getParameters(Collection<ComparableParameter> parameters) {
        if (parameters == null) {
            return null;
        }
        List<Map.Entry> list = new ArrayList<>(parameters.size());
        for (ComparableParameter parameter : parameters) {
            list.add(parameter.value);
        }
        return list;
    }

    private static String percentEncode(String s) {
        if (s == null) {
            return "";
        }
        try {
            return URLEncoder.encode(s, ENCODING)
                    .replaceAll("\\+", "%20").replaceAll("\\*", "%2A").replaceAll("\\&", "%26")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException wow) {
            throw new RuntimeException(wow.getMessage(), wow);
        }
    }

    private static List<Map.Entry<String, String>> decodeForm(String form) {
        List<Map.Entry<String, String>> list = new ArrayList<>();
        boolean empty = (form == null) || (form.length() == 0);
        if (!empty) {
            for (String nvp : form.split("\\&")) {
                int equals = nvp.indexOf("=");
                String name;
                String value;
                if (equals < 0) {
                    name = decodePercent(nvp);
                    value = null;
                } else {
                    name = decodePercent(nvp.substring(0, equals));
                    value = decodePercent(nvp.substring(equals + 1));
                }
                list.add(new Parameter(name, value));
            }
        }
        return list;
    }

    private static void sign(List<Map.Entry<String, String>> pMap, String url, String httpMethod, String propName) throws Exception {
        pMap.add(new Parameter(OAUTH_SIGNATURE, getSignature(pMap, url, httpMethod, propName)));
    }

    private static String normalizeUrl(String url) throws URISyntaxException {
        myLogger.debug("Entering normalize URL: {}", url);
        URI uri = new URI(url);
        String scheme = uri.getScheme().toLowerCase();
        String authority = uri.getAuthority().toLowerCase();
        boolean dropPort = (scheme.equals("http") && uri.getPort() == 80)
                || (scheme.equals("https") && uri.getPort() == 443);
        if (dropPort) {
            // find the last : in the authority
            int index = authority.lastIndexOf(":");
            if (index >= 0) {
                authority = authority.substring(0, index);
            }
        }
        String path = uri.getRawPath();
        if (path == null || path.length() <= 0) {
            path = "/";
        }
        // we know that there is no query and no fragment here.
        String normalizedUrl = scheme + "://" + authority + path;
        myLogger.debug("Returning normalized URL: {}", normalizedUrl);
        return normalizedUrl;
    }

    private static String getBaseString(List<Map.Entry<String, String>> pMap, String url, String httpMethod) throws IOException, URISyntaxException {
        myLogger.debug("inside getBaseString");
        List<Map.Entry<String, String>> parameters;
        int q = url.indexOf("?");
        if (q < 0) {
            parameters = pMap;
        } else {
            // Combine the URL query string with the other mainMarameters:
            parameters = new ArrayList<>();
            parameters.addAll(decodeForm(url.substring(q + 1)));
            parameters.addAll(pMap);
            url = url.substring(0, q);
        }

        String baseString = percentEncode(httpMethod.toUpperCase()) + "&" + percentEncode(normalizeUrl(url)) + "&" + percentEncode(normalizeParameters(parameters));
        myLogger.debug("exiting getBaseString: {}", baseString);
        return baseString;
    }

    private static String constructFullURL(String url, Iterable<? extends Map.Entry<String, String>> parameters) throws IOException {
        String form = formEncode(parameters);
        if (form == null || form.length() <= 0) {
            return url;
        } else {
            return url + ((!url.contains("?")) ? "?" : "&") + form;
        }
    }

    private static String removeMapEntryFromList(String name, List<Map.Entry<String, String>> params) {
        String value = null;
        for (Iterator<Map.Entry<String, String>> i = params.iterator(); i.hasNext();) {
            Map.Entry<String, String> header = i.next();
            if (name.equalsIgnoreCase(header.getKey())) {
                value = header.getValue();
                i.remove();
            }
        }
        return value;
    }

    /**
     * Send a request message to the service provider and get the response.
     */
    private static HttpMsgResponse invoke(String style, String requestURL, String httpMethod, Map<String, String> pMap, String callback, String propName) throws Exception {
        myLogger.debug("In invoke");

        final boolean isPost = POST.equalsIgnoreCase(httpMethod);
        InputStream body = null;

        if (style.equalsIgnoreCase("BODY") && !(isPost && body == null)) {
            style = "QUERY_STRING";
        }
        final List<Map.Entry<String, String>> headers = new ArrayList<>();

        switch (style) {
            case "QUERY_STRING":
                requestURL = constructFullURL(requestURL, addRequiredParameters(pMap, requestURL, httpMethod, callback, propName));
                break;
            case "BODY": {
                byte[] form = formEncode(addRequiredParameters(pMap, requestURL, httpMethod, callback, propName)).getBytes("UTF-8");
                headers.add(new Parameter(CONTENT_TYPE, FORM_ENCODED));
                headers.add(new Parameter(CONTENT_LENGTH, form.length + ""));
                body = new ByteArrayInputStream(form);
                break;
            }
            case "AUTHORIZATION_HEADER":
                List<Map.Entry<String, String>> allParams = addRequiredParameters(pMap, requestURL, httpMethod, callback, propName);
                headers.add(new Parameter("Authorization", getAuthorizationHeader(null, allParams)));
                // Find the non-OAuth Parameters:
                for (Iterator<Map.Entry<String, String>> p = allParams.iterator(); p.hasNext();) {
                    if (p.next().getKey().startsWith("oauth_")) {
                        p.remove();
                    }
                }
                // Place the non-OAuth parameters elsewhere in the request:
                if (isPost && body == null) {
                    byte[] form = formEncode(allParams).getBytes("UTF-8");
                    headers.add(new Parameter(CONTENT_TYPE, FORM_ENCODED));
                    headers.add(new Parameter(CONTENT_LENGTH, form.length + ""));
                    body = new ByteArrayInputStream(form);
                } else {
                    requestURL = constructFullURL(requestURL, allParams);
                }
                break;
        }
        myLogger.debug("Passing request details to Apache HTTP Client");
        HttpMsgResponse httpResponse = execute(requestURL, httpMethod, body, headers);
        myLogger.debug("Apache HTTP Client is done executing request");

        return httpResponse;
    }

    private static HttpMsgResponse execute(String url2, String httpMethodString, InputStream in, List<Map.Entry<String, String>> headers) throws Exception {
        myLogger.debug("In execute");
        final String method = httpMethodString;
        URL stringURL = new URL(url2);
        final String url = stringURL.toExternalForm();
        final InputStream body = in;
        final boolean isPost = POST.equalsIgnoreCase(method);
        HttpMethod httpMethod;

        if (isPost) {
            EntityEnclosingMethod entityEnclosingMethod = new PostMethod(url);
            if (body != null) {
                String length = removeMapEntryFromList(CONTENT_LENGTH, headers);
                entityEnclosingMethod.setRequestEntity((length == null) ? new InputStreamRequestEntity(body) : new InputStreamRequestEntity(body, Long.parseLong(length)));
                //entityEnclosingMethod.setRequestEntity(new InputStreamRequestEntity(body));
            }
            httpMethod = entityEnclosingMethod;
        } else {
            httpMethod = new GetMethod(url);
        }

        httpMethod.setFollowRedirects(false);
        for (Map.Entry<String, String> header : headers) {
            httpMethod.addRequestHeader(header.getKey(), header.getValue());
        }

        List<Map.Entry<String, String>> responseHeaders = new ArrayList<>();
        String responseBody = "";
        String statusLine = "";

        HttpClient client = getHHTPClient();
        try {
            long start = System.currentTimeMillis();
            client.executeMethod(httpMethod);
            long end = System.currentTimeMillis();
            long callDuration = end - start;
            if (myLogger.isDebugEnabled()) {
                myLogger.debug("Finished calling client.executeMethod(). Request took " + callDuration + "ms");
            }

            for (Header header : httpMethod.getResponseHeaders()) {
                String name = header.getName();
                String value = header.getValue();
                responseHeaders.add(new Parameter(name, value));
            }
            statusLine = httpMethod.getStatusLine().toString();
            responseBody = parseStreamToString(httpMethod.getResponseBodyAsStream());
            if (myLogger.isDebugEnabled()) {
                myLogger.debug("Apache Http client raw response: status: {}, responseBody: {}", statusLine, responseBody);
            }

        } catch (Exception ex) {
            myLogger.warn("Error occured trying to execute http method. Reason: {} ", ex.toString());
            throw new Exception(ex);
        } finally {
            try {
                //Connections must be manually released when no longer used.
                myLogger.debug("Done using the connection, releasing connection now.");
                httpMethod.releaseConnection();
            } catch (Exception ex) {
                myLogger.debug("Error releasing http connection:{}", ex.toString());
            }
            try {
                if (client != null) {
                    myLogger.debug("Shutting down connection manager");
                    ((SimpleHttpConnectionManager) client.getHttpConnectionManager()).shutdown();
                }
            } catch (Exception ex) {
                myLogger.warn("Error releasing http client connection:" + ex.toString());
            }
        }

        return new HttpMsgResponse(statusLine, responseHeaders, responseBody);
    }

    private static HttpClient getHHTPClient() {
        myLogger.debug("Creating new Apache Http client instance");
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
        httpParams.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 3000);
        client.setParams((HttpClientParams) httpParams);
        client.setHttpConnectionManager(new SimpleHttpConnectionManager());

        try {
            //http_proxy=http://UGBUK-SQUID1.it.ug.smilecoms.com:8800
            String proxCon = System.getenv("HTTP_PROXY");
            if (proxCon != null) {
                String[] poxyConArray = proxCon.split(":");
                String host = poxyConArray[1].substring(2);
                int port = Integer.parseInt(poxyConArray[2]);
                myLogger.debug("SQUID is on host[{}] and listening on port[{}]", host, port);
                client.getHostConfiguration().setProxy(host, port);
            } else {
                myLogger.debug("env variable: HTTP_PROXY is not set on this host. Not going to configure SQUID on the Apache http client.");
            }
        } catch (Exception ex) {
            myLogger.debug("Error occured processing env variable: HTTP_PROXY, {}", ex.toString());
        }
        myLogger.debug("Created new Apache Http client instance, returning it");
        return client;
    }

    private static String parseStreamToString(java.io.InputStream is) throws Exception {
        //myLogger.debug("In parseStreamToString");
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
        //myLogger.debug("Exiting parseStreamToString");
        return org.apache.commons.lang.StringEscapeUtils.unescapeJava(outputStream.toString());
    }

    private static class Parameter implements Map.Entry<String, String> {

        public Parameter(String key, String value) {
            this.key = key;
            this.value = value;
        }

        private final String key;

        private String value;

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String setValue(String value) {
            try {
                return this.value;
            } finally {
                this.value = value;
            }
        }

        @Override
        public String toString() {
            return percentEncode(getKey()) + '=' + percentEncode(getValue());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Parameter that = (Parameter) obj;
            if (key == null) {
                if (that.key != null) {
                    return false;
                }
            } else if (!key.equals(that.key)) {
                return false;
            }
            if (value == null) {
                if (that.value != null) {
                    return false;
                }
            } else if (!value.equals(that.value)) {
                return false;
            }
            return true;
        }
    }

    private static class ComparableParameter implements Comparable<ComparableParameter> {

        final Map.Entry value;
        private final String key;

        ComparableParameter(Map.Entry value) {
            this.value = value;
            String n = toString(value.getKey());
            String v = toString(value.getValue());
            this.key = percentEncode(n) + ' ' + percentEncode(v);
            // ' ' is used because it comes before any character
            // that can appear in a percentEncoded string.
        }

        private static String toString(Object from) {
            return (from == null) ? null : from.toString();
        }

        @Override
        public int compareTo(ComparableParameter that) {
            return this.key.compareTo(that.key);
        }

        @Override
        public String toString() {
            return key;
        }

    }

    public static class HttpMsgResponse {

        private final List<Map.Entry<String, String>> responseHeaders;
        private final String responseBody;
        private final String statusLine;

        public HttpMsgResponse(String statuLine, List<Map.Entry<String, String>> responseHeaders, String responseBody) {
            this.responseHeaders = responseHeaders;
            this.responseBody = responseBody;
            this.statusLine = statuLine;
        }

        public String getResponseBody() throws Exception {
            return responseBody;
        }

        public final String getHeader(String name) {
            String value = null;
            for (Map.Entry<String, String> header : responseHeaders) {
                if (name.equalsIgnoreCase(header.getKey())) {
                    value = header.getValue();
                }
            }
            return value;
        }

        public List<Map.Entry<String, String>> getHearders() throws IOException {
            return Collections.unmodifiableList(responseHeaders);
        }

        public String getHTTPStatus() throws IOException {
            return statusLine;
        }
    }

}
