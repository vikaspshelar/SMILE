/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.localisation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 *
 * @author paul
 */
public class PDFUtils {

    private static final Logger log = LoggerFactory.getLogger(PDFUtils.class);

    public static byte[] generateLocalisedPDF(String xslResourceName, String xml, Locale loc, ClassLoader cl) throws Exception {
        byte[] pdf = null;
        log.debug("Generating a PDF from XHTML and XSL");
        String xslt = LocalisationHelper.getLocalisedString(loc, xslResourceName);
        log.debug("Using xslt to generate the PDF. XML: [{}], XSLT: [{}]", xml, xslt);
        String attachmentXHTML = doXSLTransform(xml, xslt, cl);
        File tmp = null;
        OutputStream fos = null;
        ByteArrayOutputStream baos = null;
        try {
            log.debug("Creating temporary file to hold the xhtml");
            tmp = File.createTempFile(java.util.UUID.randomUUID().toString(), null);
            fos = new FileOutputStream(tmp);
            log.debug("Writing xhtml [{}] to a temporary file", attachmentXHTML);
            fos.write(attachmentXHTML.getBytes());
            log.debug("Closing file output stream");
            fos.close();
            log.debug("Rendering pdf - generating ITextRenderer");
            ITextRenderer renderer = new ITextRenderer();
            log.debug("Rendering pdf - setting document");
            renderer.setDocument(tmp);
            log.debug("Rendering pdf - calling layout");
            renderer.layout();
            log.debug("Rendering pdf - creating ByteArrayOutputStream");
            baos = new ByteArrayOutputStream();
            log.debug("Rendering pdf - calling createPDF");
            renderer.createPDF(baos);
            log.debug("Rendering pdf - closing ByteArrayOutputStream");
            baos.close();
            log.debug("Rendering pdf - getting byte array");
            pdf = baos.toByteArray();
            log.debug("Finished rendering pdf and writing to byte array");
        } finally {
            if (tmp != null) {
                tmp.delete();
            }
            if (fos != null) {
                fos.close();
            }
            if (baos != null) {
                baos.close();
            }
        }
        return pdf;
    }
    
    private static String doXSLTransform(String xml, String xslt, ClassLoader cl) throws Exception {
        // Create transformer factory
        TransformerFactory factory;
        if (xslt.contains(" version=\"2.0\"")) {
            log.debug("Using SAXON as this is xslt 2.0");
            try {
                factory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", cl);
            } catch (Throwable e) {
                log.warn("Error: [{}]", e.toString());
                factory = TransformerFactory.newInstance();
            }
        } else {
            factory = TransformerFactory.newInstance();
        }
        // Use the factory to create a template containing the xsl stream
        Templates template = factory.newTemplates(new StreamSource(stringToStream(xslt)));
        // Use the template to create a transformer
        Transformer xformer = template.newTransformer();
        // Prepare the input and output streams
        Source source = new StreamSource(stringToStream(xml));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Result result = new StreamResult(baos);
        // Apply the xsl file to the source file and write the result to the output stream
        xformer.transform(source, result);
        return streamtoString(baos);
    }

    private static ByteArrayInputStream stringToStream(String s) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(s.getBytes("ISO-8859-1"));
    }

    private static String streamtoString(ByteArrayOutputStream s) throws UnsupportedEncodingException {
        return s.toString("ISO-8859-1");
    }
}
