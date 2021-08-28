/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.util;

import com.smilecoms.commons.base.cache.CacheHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.smilecoms.commons.base.BaseUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
public class Oauth20Utils {

    private static final String CLASS = Oauth20Utils.class.getName();
    private static final Logger myLogger = LoggerFactory.getLogger(CLASS);
    private static final String ACCESS_TOKEN = "access_token";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String TOKEN_CREATED = "token_created";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String AUTHENTICATION_SERVER_URL = "authentication_server_url";
    private static final String RESOURCE_SERVER_URL = "resource_server_url";
    private static final String GRANT_TYPE = "grant_type";
    private static final String SCOPE = "scope";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer";
    private static final String BASIC = "Basic";
    private static final String URL_ENCODED_CONTENT = "application/x-www-form-urlencoded";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String POST = "POST";
    private static final String ENCODING = "UTF-8";

    public static String getProtectedResource(String clientId, String clientSecret, String scope, String authenticationServerUrl, String password, String username, String grantType, String resourceServerUrl, Map<String, String> resourceParams) throws Exception {
        Map<String, String> tokenParams = new HashMap<>();
        tokenParams.put(RESOURCE_SERVER_URL, resourceServerUrl);
        tokenParams.put(GRANT_TYPE, grantType);
        tokenParams.put(USERNAME, username);
        tokenParams.put(PASSWORD, password);
        tokenParams.put(CLIENT_ID, clientId);
        tokenParams.put(CLIENT_SECRET, clientSecret);
        tokenParams.put(SCOPE, scope);
        tokenParams.put(AUTHENTICATION_SERVER_URL, authenticationServerUrl);

        HttpMsgResponse response;
        String responseBody;
        try {
            String token = getLatestAccessToken(tokenParams);
            if (token == null) {
                throw new Exception("Failed to get token from: " + authenticationServerUrl);
            }
            response = invoke("QUERY_STRING", resourceServerUrl, "GET", resourceParams, null, null, token);
            responseBody = response.getResponseBody();
        } catch (Exception e) {
            myLogger.warn("Failed to get resource: {}", e.toString());
            throw new Exception(e.getMessage());
        }
        return responseBody;
    }

    public static String getAccessToken(String clientId, String clientSecret, String scope, String authenticationServerUrl, String password, String username, String grantType) throws Exception {
        Map<String, String> resourceParams = new HashMap<>();
        resourceParams.put(GRANT_TYPE, grantType);
        resourceParams.put(AUTHENTICATION_SERVER_URL, authenticationServerUrl);
        if (clientId == null || clientId.isEmpty()) {
            resourceParams.put(USERNAME, username);
            resourceParams.put(PASSWORD, password);
        } else {
            resourceParams.put(CLIENT_ID, clientId);
            resourceParams.put(CLIENT_SECRET, clientSecret);
        }

        HttpMsgResponse response;
        String accessToken = null;

        try {
            myLogger.debug("Try with client credentials: clientId and clientSecret");
            response = invoke("NEW_TOKEN", authenticationServerUrl, "POST", resourceParams, clientId, clientSecret, null);
            int code = Integer.parseInt(response.getHTTPStatusCode());
            if (code >= 400) {
                myLogger.debug("Failed to get token using client credentials, retry with login credentials: username and password");
                response = invoke("NEW_TOKEN", authenticationServerUrl, "POST", resourceParams, username, password, null);
                code = Integer.parseInt(response.getHTTPStatusCode());
                if (code >= 400) {
                    String error = response.getResponseBody();//{"status":"invalid_client","message":"Client credentials are invalid","code":401,"data":{"name":"OAuth2Error","error":"invalid_client","error_description":"Client credentials are invalid"}}
                    JsonParser jsonParser = new JsonParser();
                    String msg = "";
                    JsonObject errorMsgAsJsonObject;
                    errorMsgAsJsonObject = jsonParser.parse(error).getAsJsonObject();
                    for (Map.Entry element : errorMsgAsJsonObject.entrySet()) {
                        if (element.getKey().equals("message")) {
                            JsonPrimitive asJsonPrimitive = errorMsgAsJsonObject.getAsJsonPrimitive("message");
                            msg = asJsonPrimitive.getAsString();
                            break;
                        }
                    }
                    throw new Exception("Could not retrieve access token for user: " + username + ", reason: " + msg);
                }
            }
            accessToken = response.getResponseBody();//{"token_type":"bearer","access_token":"f694df735e72538d8e08f7d95c32dbcc24385954","expires_in":3600,"refresh_token":"378a36c6b5d6785a78c43c8e2eeedd62bdfef71b"}
        } catch (Exception e) {
            myLogger.warn("Failed to get token reason: {}", e.toString());
            throw new Exception(e.getMessage());
        }
        return accessToken;
    }

    private static String getAccessToken(Map<String, String> resourceParams) throws Exception {

        String clientId = resourceParams.get(CLIENT_ID);
        String clientSecret = resourceParams.get(CLIENT_SECRET);
        String username = resourceParams.get(USERNAME);
        String password = resourceParams.get(PASSWORD);
        String grantType = resourceParams.get(GRANT_TYPE);
        String scope = resourceParams.get(SCOPE);
        String authenticationServerUrl = resourceParams.get(AUTHENTICATION_SERVER_URL);

        String retToken = getAccessToken(clientId, clientSecret, scope, authenticationServerUrl, password, username, grantType);

        return retToken;
    }

    private static String refreshAccessToken(Map<String, String> resourceParams) throws Exception {
        myLogger.debug("In refreshAccessToken");
        String clientId = resourceParams.get(CLIENT_ID);
        String clientSecret = resourceParams.get(CLIENT_SECRET);
        String authenticationServerUrl = resourceParams.get(AUTHENTICATION_SERVER_URL);

        HttpMsgResponse response;
        String accessToken = null;

        try {
            response = invoke("REFRESH_TOKEN", authenticationServerUrl, "POST", resourceParams, clientId, clientSecret, null);
            int code = Integer.parseInt(response.getHTTPStatusCode());
            if (code >= 400) {
                myLogger.warn("Could not retrieve access token for user: {}", clientId);
            }
            accessToken = response.getResponseBody();//{"token_type":"bearer","access_token":"f694df735e72538d8e08f7d95c32dbcc24385954","expires_in":3600,"refresh_token":"378a36c6b5d6785a78c43c8e2eeedd62bdfef71b"}
        } catch (Exception e) {
            myLogger.warn("Error occured trying to refresh token: {}", e.toString());
            throw new Exception(e.toString());
        }
        return accessToken;
    }

    private static boolean checkIfTokenIsStale(JsonObject json) {
        myLogger.debug("In checkIfTokenIsStale");
        boolean isStale = false;
        boolean refreshTokenFound = false;
        boolean tokenCreatedFound = false;
        long tokenCreated = 0;
        int expireIn = 0;

        for (Map.Entry element : json.entrySet()) {
            if (element.getKey().equals(REFRESH_TOKEN)) {
                refreshTokenFound = true;
            }
            if (element.getKey().equals(TOKEN_CREATED)) {
                JsonPrimitive asJsonPrimitive = json.getAsJsonPrimitive(TOKEN_CREATED);
                tokenCreated = asJsonPrimitive.getAsLong();
                tokenCreatedFound = true;
            }
            if (element.getKey().equals("expires_in")) {
                JsonPrimitive asJsonPrimitive = json.getAsJsonPrimitive("expires_in");
                expireIn = asJsonPrimitive.getAsInt();
            }
        }
        if (refreshTokenFound && tokenCreatedFound) {
            if (System.currentTimeMillis() > (tokenCreated + expireIn * 1000L)) {
                myLogger.debug("Token needs to be refreshed");
                isStale = true;
            }
        }
        myLogger.debug("Exiting checkIfTokenIsStale, is token stale:{}", isStale);
        return isStale;
    }

    private static String getLatestAccessToken(Map<String, String> resourceParams) throws Exception {
        myLogger.debug("In getLatestAccessToken");
        String tokenKey = resourceParams.get(CLIENT_ID);
        if (tokenKey == null || tokenKey.isEmpty()) {
            tokenKey = "oauth20_" + resourceParams.get(USERNAME);
        }
        JsonParser jsonParser = new JsonParser();
        JsonObject tokenAsJsonObject;

        String token = (String) CacheHelper.getFromRemoteCache(tokenKey);
        boolean tokenExistInCache = true;

        if (token == null) {
            myLogger.debug("Access token not found in cache, going to request a new one from authentication server");
            token = getAccessToken(resourceParams);
            tokenExistInCache = false;
            tokenAsJsonObject = jsonParser.parse(token).getAsJsonObject();
            tokenAsJsonObject.addProperty(TOKEN_CREATED, System.currentTimeMillis());
            token = tokenAsJsonObject.toString();
            myLogger.debug("New token is: " + token);
            CacheHelper.putInRemoteCache(tokenKey, token, BaseUtils.getIntProperty("env.oauth2.refresh.token.expiry.seconds", 4500));
        }

        tokenAsJsonObject = jsonParser.parse(token).getAsJsonObject();
        String accessToken = null;
        String refreshToken = "";

        if (tokenExistInCache) {
            myLogger.debug("Found token from cache: {}", token);
            if (checkIfTokenIsStale(tokenAsJsonObject)) {

                for (Map.Entry element : tokenAsJsonObject.entrySet()) {
                    if (element.getKey().equals("refresh_token")) {
                        JsonPrimitive asJsonPrimitive = tokenAsJsonObject.getAsJsonPrimitive("refresh_token");
                        refreshToken = asJsonPrimitive.getAsString();
                        break;
                    }
                }

                resourceParams.put(GRANT_TYPE, "refresh_token");
                resourceParams.put(REFRESH_TOKEN, refreshToken);
                token = refreshAccessToken(resourceParams);

                tokenAsJsonObject = jsonParser.parse(token).getAsJsonObject();
                tokenAsJsonObject.addProperty(TOKEN_CREATED, System.currentTimeMillis());
                token = tokenAsJsonObject.toString();
                CacheHelper.putInRemoteCache(tokenKey, token, BaseUtils.getIntProperty("env.oauth2.refresh.token.expiry.seconds", 4500));
            }
        }

        tokenAsJsonObject = jsonParser.parse(token).getAsJsonObject();

        for (Map.Entry element : tokenAsJsonObject.entrySet()) {
            if (element.getKey().equals(ACCESS_TOKEN)) {
                JsonPrimitive asJsonPrimitive = tokenAsJsonObject.getAsJsonPrimitive(ACCESS_TOKEN);
                accessToken = asJsonPrimitive.getAsString();
                break;
            }
        }

        return accessToken;
    }

    private static String getAuthorizationHeaderForAccessToken(String accessToken) {
        return BEARER + " " + accessToken;
    }

    private static String getBasicAuthorizationHeader(String username, String password) {
        return BASIC + " " + encodeCredentials(username, password);
    }

    private static String encodeCredentials(String username, String password) {
        String cred = username + ":" + password;
        String encodedValue;
        byte[] encodedBytes = Base64.encodeBase64(cred.getBytes());
        encodedValue = new String(encodedBytes);
        //myLogger.debug("encodedBytes " + new String(encodedBytes));

        byte[] decodedBytes = Base64.decodeBase64(encodedBytes);
        //myLogger.debug("decodedBytes " + new String(decodedBytes));

        return encodedValue;

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
    private static HttpMsgResponse invoke(String style, String requestURL, String httpMethod, Map<String, String> pMap, String username, String password, String token) throws Exception {
        myLogger.debug("Oauth20: In invoke");

        final boolean isPost = POST.equalsIgnoreCase(httpMethod);
        InputStream body = null;

        final List<Map.Entry<String, String>> headers = new ArrayList<>();

        switch (style) {

            case "NEW_TOKEN":
            case "REFRESH_TOKEN": {
                List<Map.Entry<String, String>> allParams = addParameters(pMap);
                headers.add(new Parameter(AUTHORIZATION, getBasicAuthorizationHeader(username, password)));
                if (isPost && body == null) {
                    byte[] form = formEncode(allParams).getBytes("UTF-8");
                    headers.add(new Parameter(CONTENT_TYPE, URL_ENCODED_CONTENT));
                    body = new ByteArrayInputStream(form);
                }
                break;
            }
            case "BODY_RESOURCE": {
                List<Map.Entry<String, String>> allParams = addParameters(pMap);
                headers.add(new Parameter(AUTHORIZATION, getAuthorizationHeaderForAccessToken(token)));
                if (isPost && body == null) {
                    byte[] form = formEncode(allParams).getBytes("UTF-8");
                    headers.add(new Parameter(CONTENT_TYPE, URL_ENCODED_CONTENT));
                    headers.add(new Parameter(CONTENT_LENGTH, form.length + ""));
                    body = new ByteArrayInputStream(form);
                }
                break;
            }
            case "QUERY_STRING":
                headers.add(new Parameter(AUTHORIZATION, getAuthorizationHeaderForAccessToken(token)));
                requestURL = constructGetURL(requestURL, addParameters(pMap));
                break;
        }
        myLogger.debug("Oauth20: Passing request details to Apache HTTP Client");
        HttpMsgResponse httpResponse = execute(requestURL, httpMethod, body, headers);
        myLogger.debug("Oauth20: Apache HTTP Client is done executing request");

        return httpResponse;
    }

    private static HttpMsgResponse execute(String resourceURL, String httpMethodString, InputStream in, List<Map.Entry<String, String>> headers) throws Exception {
        myLogger.debug("Oauth20: In execute");
        final String method = httpMethodString;
        URL stringURL = new URL(resourceURL);
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

        if (myLogger.isDebugEnabled()) {
            myLogger.debug("********** Oauth20: Request Headers **********");
            for (Map.Entry<String, String> header : headers) {
                myLogger.debug(header.getKey() + ":" + header.getValue());
            }
            myLogger.debug("********** Oauth20: End of Request Headers **********");
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
            myLogger.debug("Oauth20: Finished calling client.executeMethod(). Request took " + callDuration + "ms");

            for (Header header : httpMethod.getResponseHeaders()) {
                String name = header.getName();
                String value = header.getValue();
                responseHeaders.add(new Parameter(name, value));
            }

            if (myLogger.isDebugEnabled()) {
                myLogger.debug("********** Oauth20: Response Headers Received **********");
                for (Header header : httpMethod.getResponseHeaders()) {
                    String name = header.getName();
                    String value = header.getValue();
                    myLogger.debug(name + ":" + value);
                }
                myLogger.debug("********** Oauth20: End of Response Headers **********");
            }

            statusLine = httpMethod.getStatusLine().toString();
            responseBody = parseStreamToString(httpMethod.getResponseBodyAsStream());
            myLogger.debug("Oauth20: Apache Http client raw response: status: {}, responseBody: {}", responseBody, statusLine);

        } catch (Exception ex) {
            myLogger.warn("Oauth20: Error occured trying to execute http method. Reason:{} ", ex.toString());
            throw new Exception(ex);
        } finally {
            try {
                //Connections must be manually released when no longer used.
                myLogger.debug("Oauth20: Done using the connection, releasing it now.");
                httpMethod.releaseConnection();
            } catch (Exception ex) {
                myLogger.debug("Oauth20: Error releasing http connection:{}", ex.toString());
            }
            try {
                if (client != null) {
                    myLogger.debug("Oauth20: Shutting down connection manager");
                    ((SimpleHttpConnectionManager) client.getHttpConnectionManager()).shutdown();
                }
            } catch (Exception ex) {
                myLogger.debug("Oauth20: Error releasing http client connection:{}", ex.toString());
            }
        }

        return new HttpMsgResponse(statusLine, responseHeaders, responseBody);
    }

    private static HttpClient getHHTPClient() {
        myLogger.debug("Oauth20: Creating new Apache Http client instance");
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
                myLogger.debug("Oauth20: SQUID is on host[{}] and listening on port[{}]......", host, port);
                client.getHostConfiguration().setProxy(host, port);
            } else {
                myLogger.debug("Oauth20: env variable: HTTP_PROXY is not set on this host. Not going to configure SQUID on the Apache http client.");
            }
        } catch (Exception ex) {
            myLogger.debug("Oauth20: Error occured processing env variable: HTTP_PROXY, {}", ex.toString());
        }
        myLogger.debug("Oauth20: Created new Apache Http client instance, returning it");
        return client;
    }

    public static class HttpMsgResponse {

        private final List<Map.Entry<String, String>> responseHeaders;
        private final String responseBody;
        private final String statusLine;
        private final String statusCode;

        public HttpMsgResponse(String statuLine, List<Map.Entry<String, String>> responseHeaders, String responseBody) {
            this.responseHeaders = responseHeaders;
            this.responseBody = responseBody;
            this.statusLine = statuLine;
            this.statusCode = statuLine.split(" ")[1];
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

        public String getHTTPStatusLine() throws IOException {
            return statusLine;
        }

        public String getHTTPStatusCode() throws IOException {
            return statusCode;
        }
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
        myLogger.debug("Oauth20: Ruturning form-urlencoded string: {}", ret);
        return ret;
    }

    private static String constructGetURL(String url, Iterable<? extends Map.Entry<String, String>> parameters) throws IOException {
        String form = formEncode(parameters);
        if (form == null || form.length() <= 0) {
            return url;
        } else {
            return url + ((!url.contains("?")) ? "?" : "&") + form;
        }
    }

    private static String percentEncode(String s) {
        if (s == null) {
            return "";
        }
        try {
            return URLEncoder.encode(s, ENCODING).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
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

    private static String decodePercent(String s) {
        try {
            return URLDecoder.decode(s, ENCODING);
        } catch (java.io.UnsupportedEncodingException wow) {
            throw new RuntimeException(wow.getMessage(), wow);
        }
    }

    private static List<Map.Entry<String, String>> addParameters(Map<String, String> pMap) throws Exception {
        myLogger.debug("Oauth20: Inside addParameters");
        List<Map.Entry<String, String>> defaultParams = new ArrayList<>();
        for (Map.Entry entry : pMap.entrySet()) {
            defaultParams.add(new Parameter((String) entry.getKey(), (String) entry.getValue()));
        }
        myLogger.debug("Oauth20: exiting addParameters");
        return defaultParams;
    }

    private static class Parameter implements Map.Entry<String, String> {

        private final String key;
        private String value;

        public Parameter(String key, String value) {
            this.key = key;
            this.value = value;
        }

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
            return getKey() + '=' + getValue();
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

}
