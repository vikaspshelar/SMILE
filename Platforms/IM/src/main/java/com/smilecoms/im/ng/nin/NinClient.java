/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.ng.nin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.platform.PlatformEventManager;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import com.smilecoms.im.nimc.identitysearch.SearchResponseDemo;
import com.smilecoms.im.nimc.identitysearch.TokenObject;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smilecoms.im.nimc.identitysearch.CreateTokenStringResponse;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.net.*;

/**
 *
 * @author bhaskarhg
 */
public class NinClient {

    private static final Logger log = LoggerFactory.getLogger(NinClient.class);
    public static final String FINGER_LEFT_THUMB = "LEFT_THUMB";
    public static final String FINGER_RIGHT_THUMB = "RIGHT_THUMB";
    public static final String FINGER_RIGHT_INDEX_FINGER = "RIGHT_INDEX_FINGER";
    public static final String FINGER_LEFT_INDEX_FINGER = "LEFT_INDEX_FINGER";

    public static NinResponse ninVerificationLogic(String nin, String surname, String firstName, String lastName, byte[] fingerPrintWSQ, String verifiedBy, String entityId, String entityType) throws Exception {

        String username = BaseUtils.getSubProperty("env.nin.config", "NinUsername");
        String password = BaseUtils.getSubProperty("env.nin.config", "NinPassword");
        String ogId = BaseUtils.getSubProperty("env.nin.config", "NinOrganisationId");
        String encryptedPassword = getEncryptedPassword();

        log.debug("TEST " + BaseUtils.getSubProperty("env.nin.config", "NinUsername"));
        log.debug("TEST before sending request to get token");
        String token = getToakenString(username, ogId, encryptedPassword);
        log.debug("TEST after sending request to get token" + token);
        SearchResponseDemo searchResponseDemo = null;
        if (!token.contains(" ")) {
            if (token != null && !token.isEmpty()) {
                if (fingerPrintWSQ != null) {
                    searchResponseDemo = getCustomerDetailsByFinger(nin, token, getFingerPrintData(fingerPrintWSQ, "RIGHT_THUMB"), 1);
                } else {
                    searchResponseDemo = getCustomerDetailsByNin(nin, token);
                }
            }
        } else {
            throw new Exception("Respose Error: " + token);
        }

        if (searchResponseDemo == null) {
            throw new Exception("Please try after some time");
        }
        ObjectMapper mapper = new ObjectMapper();
        //NinResponse ninResponse = null;
        NinResponse ninResponse = mapper.convertValue(searchResponseDemo, NinResponse.class);

        if (ninResponse.getReturnMessage().equalsIgnoreCase(BaseUtils.getProperty("env.nin.status.message"))) {
            if (ninResponse.getData() != null) {
                if (ninResponse.getData().get(0).getNin() != null && !ninResponse.getData().get(0).getNin().isEmpty()) {
                    PlatformEventManager.createEvent(
                            "IM", "NinQueryResponse", nin,
                            "NIN:" + nin + "|"
                            + "FirstName:" + firstName + "|"
                            + "Surname:" + surname + "|"
                            + "LastName:" + lastName + "|"
                            + "VerifiedBy:" + verifiedBy + "|"
                            + "EntityID:" + entityId + "|"
                            + "EntityType:" + entityType + "|"
                            + "ReturnMessage:" + ninResponse.getReturnMessage());
                }
            }
        } else {
            PlatformEventManager.createEvent(
                    "IM", "NinQueryResponse", nin,
                    "NIN:" + nin + "|"
                    + "FirstName:" + firstName + "|"
                    + "Surname:" + surname + "|"
                    + "LastName:" + lastName + "|"
                    + "VerifiedBy:" + verifiedBy + "|"
                    + "EntityID:" + entityId + "|"
                    + "EntityType:" + entityType + "|"
                    + "ReturnMessage:" + ninResponse.getReturnMessage());
        }

        return ninResponse;
    }

    public static String getFingerPrintData(byte[] fingerprints, String fingerType) throws Exception {

        InputStream fis = null;
        DataInputStream in = null;
        String base64FingerPrintData = null;

        try {

            log.debug("VerificationFinger to be used is: [{}]", fingerType);

            if (fingerprints == null) {
                throw new Exception("Fingerprint data not supplied.");
            }

            fis = new ByteArrayInputStream(fingerprints);
            in = new DataInputStream(fis);

            byte[] leftIndexWSQImage;
            byte[] leftThumbWSQImage;
            byte[] rightThumbWSQImage;
            byte[] rightIndexWSQImage;

            int leftIndexWSQImageSize, leftThumbWSQImageSize, rightThumbWSQImageSize, rightIndexWSQImageSize;
            double version = in.readDouble();

            log.debug("Fingerprint file version [{}].", version);

            leftIndexWSQImageSize = in.readInt();
            leftThumbWSQImageSize = in.readInt();
            rightThumbWSQImageSize = in.readInt();
            rightIndexWSQImageSize = in.readInt();

            log.debug("Fingerprint sizes : leftIndexWSQImageSize:" + leftIndexWSQImageSize
                    + " :: leftThumbWSQImageSize:" + leftThumbWSQImageSize
                    + " :: rightThumbWSQImageSize:" + rightThumbWSQImageSize + " :: rightIndexWSQImageSize:" + rightIndexWSQImageSize);

            leftIndexWSQImage = new byte[leftIndexWSQImageSize];
            leftThumbWSQImage = new byte[leftThumbWSQImageSize];
            rightThumbWSQImage = new byte[rightThumbWSQImageSize];
            rightIndexWSQImage = new byte[rightIndexWSQImageSize];

            in.readFully(leftIndexWSQImage, 0, leftIndexWSQImageSize);
            in.readFully(leftThumbWSQImage, 0, leftThumbWSQImageSize);
            in.readFully(rightThumbWSQImage, 0, rightThumbWSQImageSize);
            in.readFully(rightIndexWSQImage, 0, rightIndexWSQImageSize);

            // Write images to files;
            writeFile(leftIndexWSQImage, "/root/leftIndexWSQImage.wsq");
            writeFile(leftThumbWSQImage, "/root/leftThumbWSQImage.wsq");
            writeFile(rightThumbWSQImage, "/root/rightThumbWSQImage.wsq");
            writeFile(rightIndexWSQImage, "/root/rightIndexWSQImage.wsq");

            if (fingerType.equalsIgnoreCase(FINGER_LEFT_INDEX_FINGER)) {
                //return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(leftIndexWSQImage);
                return new String(Base64.encodeBase64(leftIndexWSQImage), "UTF-8");
            } else if (fingerType.equalsIgnoreCase(FINGER_LEFT_THUMB)) {
                // return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(leftThumbWSQImage);
                return new String(Base64.encodeBase64(leftThumbWSQImage), "UTF-8");
            } else if (fingerType.equalsIgnoreCase(FINGER_RIGHT_THUMB)) {
                // return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(rightThumbWSQImage);
                return new String(Base64.encodeBase64(rightThumbWSQImage), "UTF-8");
            } else if (fingerType.equalsIgnoreCase(FINGER_RIGHT_INDEX_FINGER)) {
                //return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(rightIndexWSQImage);
                return new String(Base64.encodeBase64(rightIndexWSQImage), "UTF-8");
            } else {
                throw new Exception("No finger specified for extraction.");
            }

        } catch (Exception e) {
            log.error("Error while trying to extract fingerprint data for customer:", e);
            throw e;
        } finally {

            if (fis != null) {
                fis.close();
            }

            if (in != null) {
                in.close();
            }
        }

        // throw new Exception("No finger specified for extraction.");
    }

    public static void writeFile(byte[] data, String path) throws Exception {

        FileOutputStream fos = new FileOutputStream(path);
        fos.write(data);
        fos.flush();
        fos.close();

    }

    public static String getEncryptedPassword() {
        String pwd = null;
        try {
            BigInteger e = new BigInteger(BaseUtils.getSubProperty("env.nin.config", "NinExponent"));
            BigInteger m = new BigInteger(BaseUtils.getSubProperty("env.nin.config", "NinModulus"));
            log.debug("Exponent [{}].", BaseUtils.getSubProperty("env.nin.config", "NinExponent"));
            log.debug("Modulus [{}].", BaseUtils.getSubProperty("env.nin.config", "NinModulus"));
//            BigInteger e = new BigInteger("113621440243785421499955306133900099987164309503876199371900611085975699194905621710442876441889195302451922443555354266645737454327409509639333989384262385729949578624044207610948821627355876693570108394899808569346703874513552461157771585312437842555207875241788331401870311503661882350734256428011446552231");
//            BigInteger m = new BigInteger("99656440840574176563305385521896948249485597887868788305755844436736813735716889384156081404108856785411701458057572807701609821377138238971482595936817351313377639458003034637351529602924774615106031875065736828376549082962569871367654360928995574432638495308492887000005021125506027838956077501182295786099");
            String p = BaseUtils.getSubProperty("env.nin.config", "NinPassword");
            p = getPasswordHash(p);
            BigInteger pm = new BigInteger(p.getBytes());
            BigInteger b = pm.modPow(e, m);
            pwd = new String(java.util.Base64.getMimeEncoder().encode(b.toString().getBytes()), StandardCharsets.UTF_8);
            log.debug("Sending request to NIMC password " + pwd);
        } catch (Exception r) {
            r.printStackTrace();
        }
        return pwd;
    }

    private static String formatXML(String unformattedXml) {
        try {
            Document document = parseXmlFile(unformattedXml);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            //transformerFactory.setAttribute("indent-number", 3);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(document);
            StreamResult xmlOutput = new StreamResult(new StringWriter());
            transformer.transform(source, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    // parse XML
    private static Document parseXmlFile(String in) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(in));
            return db.parse(is);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getToken(String xmlResponse) {
        String token = null;
        try {
            XMLInputFactory xif = XMLInputFactory.newFactory();
            XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(xmlResponse));
            xsr.nextTag(); // Advance to Envelope tag

            xsr.nextTag(); // Advance to Body tag
            xsr.nextTag();
            //xsr.nextTag();
            //xsr.nextTag();

            JAXBContext jc = JAXBContext.newInstance(CreateTokenStringResponse.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            JAXBElement<CreateTokenStringResponse> je = unmarshaller.unmarshal(xsr, CreateTokenStringResponse.class);

            token = je.getValue().getReturn();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();

        }
        return token;
    }

    private static SearchResponseDemo getCustomerDetails(String xmlResponse) {
        SearchResponseDemo searchResponseDemo = null;
        try {
            XMLInputFactory xif = XMLInputFactory.newFactory();
            XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(xmlResponse));
            xsr.nextTag(); // Advance to Envelope tag

            xsr.nextTag(); // Advance to Body tag
            xsr.nextTag();
            xsr.nextTag();

            JAXBContext jc = JAXBContext.newInstance(SearchResponseDemo.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            JAXBElement<SearchResponseDemo> je = unmarshaller.unmarshal(xsr, SearchResponseDemo.class);

            searchResponseDemo = je.getValue();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();

        }
        return searchResponseDemo;
    }

    public static String getToakenString(String username, String ogId, String encryptedPassword) throws IOException {
        String responseString = "";
        String outputString = "";
        String wsEndPoint = BaseUtils.getSubProperty("env.nin.config", "NinWSURL");
        URLConnection connection = null;

        if (Boolean.valueOf(BaseUtils.getSubProperty("env.nin.config", "ProxyRequired"))) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.33.9.82", 8080));
            URL url = new URL(wsEndPoint);
            connection = url.openConnection(proxy);
        } else {
            URL url = new URL(wsEndPoint);
            connection = url.openConnection();
        }
        HttpURLConnection httpConn = (HttpURLConnection) connection;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        String xmlInput = "<x:Envelope xmlns:x=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ide=\"http://IdentitySearch.nimc/\">"
                + "<x:Header/>"
                + "<x:Body>"
                + "<ide:createTokenString>"
                + "<username>" + username + "</username>"
                + "<password>" + encryptedPassword + "</password>"
                + "<orgid>" + ogId + "</orgid>"
                + "</ide:createTokenString>"
                + "</x:Body>"
                + "</x:Envelope>";

        log.debug("NIMC request for token  " + formatXML(xmlInput));
        byte[] buffer = new byte[formatXML(xmlInput).length()];
        buffer = xmlInput.getBytes();
        bout.write(buffer);
        byte[] b = bout.toByteArray();
        String SOAPAction = "getUserDetails";
        httpConn.setRequestProperty("Content-Length", String.valueOf(b.length));
        httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        httpConn.setRequestProperty("SOAPAction", SOAPAction);
        httpConn.setRequestMethod("POST");
        if (Boolean.parseBoolean(BaseUtils.getSubProperty("env.nin.config", "NinConnectionTimeoutRequired"))) {
            httpConn.setConnectTimeout(Integer.parseInt(BaseUtils.getSubProperty("env.nin.config", "NinConnectionTimeout")));
        }
        if (Boolean.parseBoolean(BaseUtils.getSubProperty("env.nin.config", "NinReceiveTimeoutRequired"))) {
            httpConn.setReadTimeout(Integer.parseInt(BaseUtils.getSubProperty("env.nin.config", "NinReceiveTimeout")));
        }
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);
        OutputStream out = httpConn.getOutputStream();
        // Write the content of the request to the outputstream of the HTTP
        // Connection.
        out.write(b);
        out.close();
        // Ready with sending the request.
        // Read the response.
        InputStreamReader isr = new InputStreamReader(httpConn.getInputStream(), Charset.forName("UTF-8"));
        BufferedReader in = new BufferedReader(isr);
        // Write the SOAP message response to a String.
        while ((responseString = in.readLine()) != null) {
            outputString = outputString + responseString;
        }
        // Write the SOAP message formatted to the console.
        String formattedSOAPResponse = formatXML(outputString);
        log.debug("NIMC response for fingerwithnin  " + formattedSOAPResponse);
        httpConn.disconnect();
        return getToken(formattedSOAPResponse);
    }

    public static SearchResponseDemo getCustomerDetailsByNin(String nin, String token) throws IOException {
        String responseString = "";
        String outputString = "";
        String wsEndPoint = BaseUtils.getSubProperty("env.nin.config", "NinWSURL");
        URLConnection connection = null;

        if (Boolean.valueOf(BaseUtils.getSubProperty("env.nin.config", "ProxyRequired"))) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.33.9.82", 8080));
            URL url = new URL(wsEndPoint);
            connection = url.openConnection(proxy);
        } else {
            URL url = new URL(wsEndPoint);
            connection = url.openConnection();
        }
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.33.9.82", 8080));
//        URL url = new URL(wsEndPoint);
//        URLConnection connection = url.openConnection(proxy);
//        URL url = new URL(wsEndPoint);
//        URLConnection connection = url.openConnection();
        HttpURLConnection httpConn = (HttpURLConnection) connection;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        String xmlInput = "<x:Envelope xmlns:x=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ide=\"http://IdentitySearch.nimc/\">\n"
                + "   <x:Header/>"
                + "    <x:Body>"
                + "        <ide:searchByNIN>"
                + "            <token>" + token + "</token>"
                + "            <nin>" + nin + "</nin>"
                + "        </ide:searchByNIN>"
                + "    </x:Body>"
                + "</x:Envelope>";
        log.debug("NIMC request for nin  " + formatXML(xmlInput));
        byte[] buffer = new byte[formatXML(xmlInput).length()];
        buffer = xmlInput.getBytes();
        bout.write(buffer);
        byte[] b = bout.toByteArray();
        String SOAPAction = "getUserDetails";
        httpConn.setRequestProperty("Content-Length", String.valueOf(b.length));
        httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        httpConn.setRequestProperty("SOAPAction", SOAPAction);
        httpConn.setRequestMethod("POST");
        if (Boolean.parseBoolean(BaseUtils.getSubProperty("env.nin.config", "NinConnectionTimeoutRequired"))) {
            httpConn.setConnectTimeout(Integer.parseInt(BaseUtils.getSubProperty("env.nin.config", "NinConnectionTimeout")));
        }
        if (Boolean.parseBoolean(BaseUtils.getSubProperty("env.nin.config", "NinReceiveTimeoutRequired"))) {
            httpConn.setReadTimeout(Integer.parseInt(BaseUtils.getSubProperty("env.nin.config", "NinReceiveTimeout")));
        }
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);
        OutputStream out = httpConn.getOutputStream();
        // Write the content of the request to the outputstream of the HTTP
        // Connection.
        out.write(b);
        out.close();
        // Ready with sending the request.
        // Read the response.
        InputStreamReader isr = new InputStreamReader(httpConn.getInputStream(), Charset.forName("UTF-8"));
        BufferedReader in = new BufferedReader(isr);
        // Write the SOAP message response to a String.
        while ((responseString = in.readLine()) != null) {
            outputString = outputString + responseString;
        }
        // Write the SOAP message formatted to the console.
        String formattedSOAPResponse = formatXML(outputString);
        log.debug("NIMC response for nin  " + formattedSOAPResponse);
        httpConn.disconnect();
        return getCustomerDetails(formattedSOAPResponse);
    }

    public static SearchResponseDemo getCustomerDetailsByFinger(String nin, String token, String fingerprint, int pos) throws IOException {
        String responseString = "";
        String outputString = "";
        String wsEndPoint = BaseUtils.getSubProperty("env.nin.config", "NinWSURL");
        URLConnection connection = null;

        if (Boolean.valueOf(BaseUtils.getSubProperty("env.nin.config", "ProxyRequired"))) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.33.9.82", 8080));
            URL url = new URL(wsEndPoint);
            connection = url.openConnection(proxy);
        } else {
            URL url = new URL(wsEndPoint);
            connection = url.openConnection();
        }
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.33.9.82", 8080));
//        URL url = new URL(wsEndPoint);
//        URLConnection connection = url.openConnection(proxy);
//        URL url = new URL(wsEndPoint);
//        URLConnection connection = url.openConnection();
        HttpURLConnection httpConn = (HttpURLConnection) connection;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        String xmlInput = "<x:Envelope xmlns:x=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ide=\"http://IdentitySearch.nimc/\">"
                + "    <x:Header/>"
                + "    <x:Body>"
                + "        <ide:verifyFingerWithData>"
                + "            <token>" + token + "</token>"
                + "            <nin>" + nin + "</nin>"
                + "            <fingerStringInBase64>" + fingerprint + "</fingerStringInBase64>"
                + "            <pos>" + pos + "</pos>"
                + "        </ide:verifyFingerWithData>"
                + "    </x:Body>"
                + "</x:Envelope>";
        //System.out.println(formatXML(xmlInput));
        log.debug("NIMC request for fingerwithnin " + formatXML(xmlInput));
        byte[] buffer = new byte[formatXML(xmlInput).length()];
        buffer = xmlInput.getBytes();
        bout.write(buffer);
        byte[] b = bout.toByteArray();
        String SOAPAction = "getUserDetails";
        httpConn.setRequestProperty("Content-Length", String.valueOf(b.length));
        httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        httpConn.setRequestProperty("SOAPAction", SOAPAction);
        httpConn.setRequestMethod("POST");
        if (Boolean.parseBoolean(BaseUtils.getSubProperty("env.nin.config", "NinConnectionTimeoutRequired"))) {
            httpConn.setConnectTimeout(Integer.parseInt(BaseUtils.getSubProperty("env.nin.config", "NinConnectionTimeout")));
        }
        if (Boolean.parseBoolean(BaseUtils.getSubProperty("env.nin.config", "NinReceiveTimeoutRequired"))) {
            httpConn.setReadTimeout(Integer.parseInt(BaseUtils.getSubProperty("env.nin.config", "NinReceiveTimeout")));
        }
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);
        OutputStream out = httpConn.getOutputStream();
        // Write the content of the request to the outputstream of the HTTP
        // Connection.
        out.write(b);
        out.close();
        // Ready with sending the request.
        // Read the response.
        InputStreamReader isr = new InputStreamReader(httpConn.getInputStream(), Charset.forName("UTF-8"));
        BufferedReader in = new BufferedReader(isr);
        // Write the SOAP message response to a String.
        while ((responseString = in.readLine()) != null) {
            outputString = outputString + responseString;
        }
        // Write the SOAP message formatted to the console.
        String formattedSOAPResponse = formatXML(outputString);
        log.debug("NIMC response token  " + formattedSOAPResponse);
        httpConn.disconnect();
        return getCustomerDetails(formattedSOAPResponse);
    }

    public static String getPasswordHash(String pwd) {

        try {
            MessageDigest m = MessageDigest.getInstance("sha-256");
            m.update(pwd.getBytes(), 0, pwd.length());

            return new BigInteger(1, m.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
