/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.CustomerCommunicationData;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.pos.db.model.CustomerProfile;
import com.smilecoms.pos.db.model.Organisation;
import com.smilecoms.pos.db.model.TraState;
import com.smilecoms.pos.db.op.DAO;
import com.smilecoms.xml.schema.pos.Sale;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author lesiba,Abhilash
 */
public class TRAHelper {

    private static final Logger log = LoggerFactory.getLogger(TRAHelper.class);
    public static Properties props = null;
    
    private static final String REST_URI = BaseUtils.getSubProperty("env.tra.esd.rest.properties", "URL"); 
    private static final Client restClient = ClientBuilder.newClient();

    public static boolean ready() {
        try {
            new URL(REST_URI).toURI();
            return true;
        } catch (Exception e) {
            log.debug("ESD URL is not reachable");
            return false;
        }
    }
   
    public static void sendSaleToESDForSigning(EntityManager em, com.smilecoms.pos.db.model.Sale sale) throws Exception {             
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayInputStream bais = null;
        try {
                //JPAUtils.beginTransaction(em);
                TraState newState = DAO.createTRAStateInOwnTranscationScope(em, sale.getSaleId(), TRADaemon.TRA_STATUS_PENDING);
                bais = new ByteArrayInputStream(sale.getInvoicePDF());

                SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy");
                EsdRequest request = new EsdRequest();
                BigDecimal hundred = new BigDecimal(100);
                CustomerProfile cp;
                Organisation org;
                
                try {
                    cp = DAO.getCustomerProfileById(em, sale.getRecipientCustomerId());
                } catch (Exception ex)
                {
                    log.error("error fetching the customer details so assigning the customer object to null");
                    cp =null;
                }
                
                try{
                    org = DAO.getOrganisationByOrganisationId(em, sale.getRecipientOrganisationId());
                } catch (Exception ex)
                {
                    log.error("error fetching the org details so assigning the org object to null");
                    org =null;
                }                

                //request.setGrand_total(sale.getSaleTotalCentsIncl().divide(hundred));
                //request.setGross_amount(sale.getSaleTotalCentsExcl().divide(hundred));
                request.setGrand_total(sale.getSaleTotalCentsIncl().movePointLeft(2).setScale(2,1));
                request.setGross_amount(sale.getSaleTotalCentsExcl().movePointLeft(2).setScale(2,1));
                request.setInvoice_date(sdf.format(sale.getSaleDateTime()));
                request.setInvoice_number(sale.getSaleId());
                request.setLocation(BaseUtils.getSubProperty("env.tra.esd.rest.properties", "LOCATION"));
                request.setCustomer_tin(org==null?" ":org.getTaxNumber());
                request.setCustomer_name(cp==null?" ":cp.getFirstName()+" "+cp.getLastName());
                request.setCustomer_phone(cp==null?" ":cp.getAlternativeContact1());                
                request.setVat(Integer.parseInt(BaseUtils.getSubProperty("env.tra.esd.rest.properties", "VAT")));
                
                
                //request.setUsername(BaseUtils.getProperty("env.tra.esd.rest.username","esduser"));
                //request.setPassword(BaseUtils.getProperty("env.tra.esd.rest.password","esdpwd123"));

                log.error("sendSaleToESDForSigning(): Input json is: "+Entity.entity(request, MediaType.APPLICATION_JSON).getEntity());   
                
                //Logic to get the signature from the API
                
                String response = restClient.target(REST_URI).request(MediaType.APPLICATION_JSON).post(Entity.entity(request,MediaType.APPLICATION_JSON),String.class);
                
                log.error("sendSaleToESDForSigning(): response is "+response);
                
                JSONParser parser = new JSONParser();  
                JSONObject json = (JSONObject) parser.parse(response);  
                
                //{"signature":"65B107F47F02FE3152EBAD3E4BAA82BDFFCC586A/0710191750/03TZ342002054#4999999.97","status":"00","description":"Document signed successfully."}
                String signature="";
                String verification_url="";
                String status="";
                
                if(json.containsKey("signature") && json.containsKey("verification_url"))
                {
                    signature=json.get("signature").toString();
                    verification_url=json.get("verification_url").toString();
                }
                else
                {
                    log.error("output from ESD device is not in expected format. There is some error");
                }
                
//                Pattern pattern = Pattern.compile("\"signature\":(.*),\"verification_url\":(.*),\"invoice_number\":(.*),\"status\":(.*),\"description\":");
//                Matcher matcher = pattern.matcher(response);
//
//                if(matcher.find())
//                {
//                    signature=matcher.group(1);
//                    verification_url=matcher.group(2);
//                    status=matcher.group(4);
//                }
//                else
//                {
//                    log.error("output from ESD URL is not in expected format");
//                }
                
                log.error("Signature is: "+signature+" and Status is: "+status+" and verification url is:"+verification_url);
                
                //if (Integer.parseInt(status.replaceAll("\"", ""))==0) 
                 if (signature != null && !signature.isEmpty() && verification_url != null && !verification_url.isEmpty() && !signature.equals("") && !verification_url.equals("")) 
                {
                    try {
                        DAO.setTRAStateInOwnTransactionScope(em, newState, TRADaemon.TRA_STATUS_IN_PROGRESS);
                        log.debug("Sales Invoice for Sale ID [{}] sent for signing", sale.getSaleId());
                
                        //logic to append the signature to the PDF
                        log.debug("Adding Signature to PDF");
                        PdfReader reader = new PdfReader(bais);
                        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream("edited-invoice.pdf")); // temp output PDF
                        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

                        for (int i=1; i<=reader.getNumberOfPages(); i++){

                            // get object for writing over the existing content;use getUnderContent for writing in the bottom layer
                            PdfContentByte over = stamper.getOverContent(i);

                            over.beginText();
                            over.setFontAndSize(bf, 8);
                            over.setTextMatrix(85, 30);
                            over.showText(signature.replaceAll("\"", ""));
                            over.endText();
                            
                            PdfContentByte over1 = stamper.getOverContent(i);
                            over1.beginText();
                            over1.setFontAndSize(bf, 8);
                            over1.setTextMatrix(100, 20);
                            over1.showText(verification_url.replaceAll("\"", ""));
                            over1.endText();
                            
                            //generate the QR code
                            String path = sale.getSaleId()+".png";
                            String charset = "UTF-8";
                            Map<EncodeHintType, ErrorCorrectionLevel> hashMap = new HashMap<EncodeHintType,ErrorCorrectionLevel>();
                            hashMap.put(EncodeHintType.ERROR_CORRECTION,ErrorCorrectionLevel.L);
                            generateQRCode(verification_url, path, charset, hashMap, 50, 50);
                            
                            //add QR code to the pdf invoice
                            Image image = Image.getInstance(path);
                            image.setAbsolutePosition(10, 10);
                            over.addImage(image);
                            
                        }

                        stamper.close();
                
                        log.debug("completed adding signature to PDF. Now converting to bytestream to store back to DB for sale:"+sale.getSaleId());

                        File file = new File("edited-invoice.pdf");
                        FileInputStream fis = new FileInputStream(file);
                        byte[] data = new byte[(int)file.length()];

                        fis.read(data);
                        baos.write(data);

                        //logic to update database with signed invoice
                
                        JPAUtils.beginTransaction(em);
                        com.smilecoms.pos.db.model.Sale sale1 = DAO.getLockedSale(em, sale.getSaleId());
                        sale1.setInvoicePDF(baos.toByteArray());
                        em.persist(sale1);
                        em.flush();
                        log.debug("Stored sale [{}] invoice", sale1.getSaleId());
                        TraState state = DAO.getTRAStateBySaleId(em, sale1.getSaleId());
                        DAO.setTRAStateInOwnTransactionScope(em, state, TRADaemon.TRA_STATUS_STORED_SIGNED);
                        JPAUtils.commitTransaction(em);
                    } catch (Exception ex) {
                        log.warn("Error occured while sending sale "+ex);
                    }
                } else {
                    log.error("Error getting the signature");
                }
                
            } catch (Exception ex)
            {
                log.error("Error while adding signature to the invoice "+ex);
                JPAUtils.rollbackTransaction(em);
            }
            finally {
            if (bais != null) {
                bais.close();
            }
                baos.close();
            }
    }

    public static boolean sendInvoice(Sale newSale) throws Exception {
        if (!BaseUtils.getBooleanProperty("env.tra.send.email", false)) {
            log.debug("Automated invoice emailing temporarily suspended.");
            return true;
        }
        if (newSale.getRecipientCustomerId() == 0) {
            log.debug("This is an anonymous sale to the invoice cannot be emailed");
            return false;
        }
        
        try {
            CustomerCommunicationData email = new CustomerCommunicationData();
            email.setAttachmentBase64(newSale.getInvoicePDFBase64());
            email.setSubjectResourceName("sales.invoice.email.subject");
            email.setBodyResourceName("sales.invoice.email.body");
            email.setAttachmentFileName("Smile Invoice or Quotation - " + newSale.getSaleId() + ".pdf");
            email.setCustomerId(newSale.getRecipientCustomerId());
            email.setBCCAddress(BaseUtils.getProperty("env.sales.invoice.bcc.email.address"));
            email.setBlocking(false);
            SCAWrapper.getAdminInstance().sendCustomerCommunication(email);

            log.debug("Sent a sale invoice");
            return true;
        } catch (Exception e) {
            log.warn("Error sending sales invoice", e);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "POS", "Error sending sales invoice: " + e.toString());
            return false;
        }
    }
    
    public static void generateQRCode(String data, String path, String charset, Map hashMap,int height, int width) throws WriterException, IOException
    {
        BitMatrix matrix = new MultiFormatWriter().encode(new String(data.getBytes(charset), charset),BarcodeFormat.QR_CODE, width, height);
        MatrixToImageWriter.writeToFile(matrix,path.substring(path.lastIndexOf('.') + 1),new File(path));
    }
}