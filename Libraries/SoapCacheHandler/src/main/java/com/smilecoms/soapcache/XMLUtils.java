/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.soapcache;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import org.slf4j.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 *
 * @author paul
 */
public class XMLUtils {

    private static final String CLASS = XMLUtils.class.getName();
    private static Logger log = LoggerFactory.getLogger(CLASS);
    private static final String ENCODING = "utf-8";

    protected static byte[] doc2bytes(Node node) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            StringWriter writer = new StringWriter();
            Result result = new StreamResult(writer);
            Source source = new DOMSource(node);
            transformer.transform(source, result);
            writer.close();
            return writer.toString().getBytes();
        } catch (Exception ex) {
            log.warn(ex.toString());
        }
        return null;
    }

    private static String getMessageEncoding(SOAPMessage msg) throws SOAPException {
        String encoding = ENCODING;
        if (msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING) != null) {
            encoding = msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING).toString();
        }
        return encoding;
    }

    private static Document getDomDocument(SOAPMessage msg) {
        if (msg == null) {
            return null;
        }
        Document doc = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            msg.writeTo(baos);
            String msgString = baos.toString(getMessageEncoding(msg));
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(msgString));
            doc = db.parse(is);
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
        return doc;
    }

    protected static void logSOAPMessage(MessageContext context, Logger scalog, boolean inbound, boolean cached, String callersIP) {
        Document doc = getDomDocument(((SOAPMessageContext) context).getMessage());
        if (doc == null) {
            return;
        }
        
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
            String msgString = result.getWriter().toString();
            if (inbound) {
                scalog.debug(">>>>>>>>>>>>>> BEGIN SCA SOAP MESSAGE [{}] >>>>>>>>>>>>>>>", callersIP);
            } else {
                if (!cached) {
                    scalog.debug("<<<<<<<<<<<<<< BEGIN SCA SOAP MESSAGE <<<<<<<<<<<<<<<");
                } else {
                    scalog.debug("<<<<<<<<<<<<<< BEGIN SCA SOAP MESSAGE(CACHED) <<<<<<<<<<<<<<<");
                }
            }
            scalog.debug(msgString);
            if (inbound) {
                scalog.debug(">>>>>>>>>>>>>> END SCA SOAP MESSAGE [{}] >>>>>>>>>>>>>>>", callersIP);
            } else {
                if (!cached) {
                    scalog.debug("<<<<<<<<<<<<<< END SCA SOAP MESSAGE <<<<<<<<<<<<<<<");
                } else {
                    scalog.debug("<<<<<<<<<<<<<< END SCA SOAP MESSAGE(CACHED) <<<<<<<<<<<<<<<");
                }
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
    }
    
}
