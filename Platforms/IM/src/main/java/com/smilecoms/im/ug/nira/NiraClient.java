/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.ug.nira;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.UpdatePropertyRequest;
import com.smilecoms.commons.util.IMAPUtils;
import com.smilecoms.commons.util.Utils;
import de.muehlbauer.tidis.thirdparty.pilatus.server.facade.ChangePasswordRequest;
import de.muehlbauer.tidis.thirdparty.pilatus.server.facade.ChangePasswordResponse;
import de.muehlbauer.tidis.thirdparty.pilatus.server.facade.TransactionStatusResponse;
import de.muehlbauer.tidis.thirdparty.pilatus.server.facade.VerifyPersonInformationRequest;
import de.muehlbauer.tidis.thirdparty.pilatus.server.facade.VerifyPersonInformationResponse;
import de.muehlbauer.tidis.thirdparty.pilatus.server.facade.VerifyPersonInformationResponse2;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 */
@Singleton
@Startup
public class NiraClient implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(NiraClient.class);

    public static NiraResponse checkIfCustomerIdExistsAtNIRA(String nationalId, String documentId,
            String surname, String name, String otherNames, Date dateOfBirth) throws Exception {

        /* Sample data used is:
            NIN: CM930121003EGE
            Document ID: 000092564
            SURNAME: Tipiyai
            Given Names: Johnson
            OtherNames:
            Date Of Birth: 01/01/1993
         */
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        log.warn("Sending request to NIRA");

        VerifyPersonInformationRequest request = new VerifyPersonInformationRequest();
        request.setDateOfBirth(sdf.format(dateOfBirth));
        request.setDocumentId(documentId);
        request.setNationalId(nationalId);
        request.setGivenNames(name);
        request.setSurname(surname);
        request.setOtherNames(otherNames);
        // request.setSurname(); ...

        VerifyPersonInformationResponse2 iResponse = NiraHelper.getNiraConnection(null).verifyPersonInformation(request);

        /* <ns2:verifyPersonInformationResponse xmlns:ns2="http://facade.server.pilatus.thirdparty.tidis.muehlbauer.de/">
            <return>
              <transactionStatus>
                <transactionStatus>Error</transactionStatus>
                <error>
                  <code>202</code>
                  <message>ERROR-C-2000 : User Management Error: "de.muehlbauer.usermanagement.core.exception.FacadeException: ERROR-UM-1103::::Authentication for user smiletel@ROOT failed.::::smiletel@ROOT".</message>
                </error>
              </transactionStatus>
            </return>
          </ns2:verifyPersonInformationResponse>
         */
 /* <ns2:verifyPersonInformationResponse xmlns:ns2="http://facade.server.pilatus.thirdparty.tidis.muehlbauer.de/">
            <return>
              <transactionStatus>
                <transactionStatus>Ok</transactionStatus>
                <passwordDaysLeft>57</passwordDaysLeft>
                <executionCost>0.0</executionCost>
              </transactionStatus>
              <matchingStatus>true</matchingStatus>
              <cardStatus>Valid</cardStatus>
            </return>
          </ns2:verifyPersonInformationResponse>
         */
        log.warn("Got response from NIRA with Transaction Status [{}]", iResponse.getTransactionStatus().getTransactionStatus()); // iResponse.getBody());

        PlatformEventManager.createEvent("IM", "NiraQueryResponse", nationalId,
                "DocumentId:" + documentId + "|"
                + "NIN:" + nationalId + "|"
                + "Name:" + name + "|"
                + "Surname:" + surname + "|"
                + "OtherNames:" + otherNames + "|"
                + "TransactionStatus:" + iResponse.getTransactionStatus().getTransactionStatus() + "|"
                + "CardStatus:" + iResponse.getCardStatus() + "|"
                + "Matchingtatus:" + iResponse.isMatchingStatus() + "|"
                + ((iResponse.getTransactionStatus().getError() == null) ? ""
                : "ErrorCode:" + iResponse.getTransactionStatus().getError().getCode() + "|"
                + "ErrorMessage:" + iResponse.getTransactionStatus().getError().getMessage()) + "|"
                + ((iResponse.getTransactionStatus().getExecutionCost() == null) ? ""
                : "ExecutionCost:" + iResponse.getTransactionStatus().getExecutionCost().doubleValue()) + "|"
                + "PasswordDaysLeft:" + iResponse.getTransactionStatus().getPasswordDaysLeft());

        NiraResponse nResponse = new NiraResponse();
        nResponse.setSuccessful(false);
        nResponse.setCardStatus(iResponse.getCardStatus());
        if (iResponse.getTransactionStatus().getError() != null) { // Request failed.
            nResponse.setError("(" + iResponse.getTransactionStatus().getError().getCode() + ") - " + iResponse.getTransactionStatus().getError().getMessage());

        } else {

            boolean cardStatus = ((iResponse.getCardStatus() != null && iResponse.getCardStatus().equalsIgnoreCase(NiraHelper.NIRA_CARD_STATUS_VALID)) ? true : false);
            boolean matchingStatus = ((iResponse.isMatchingStatus() != null && iResponse.isMatchingStatus()) ? true : false);

            nResponse.setExecutionCost(iResponse.getTransactionStatus().getExecutionCost().toPlainString());
            nResponse.setPasswordDaysLeft(iResponse.getTransactionStatus().getPasswordDaysLeft());
            nResponse.setTransactionStatus(iResponse.getTransactionStatus().getTransactionStatus());
            nResponse.setMatchingStatus(matchingStatus);
            /*nResponse.setSuccessful(iResponse.getTransactionStatus().getTransactionStatus().equalsIgnoreCase(NiraHelper.TRANSACTION_STATUS_OK) &&
            cardStatus && iResponse.isMatchingStatus()); */
            nResponse.setSuccessful(matchingStatus);

        }

        //Send email based on password days left here:
        if (iResponse.getTransactionStatus().getPasswordDaysLeft() != null) {
            log.warn("NIRA is reporting number of password days left as [{}]", iResponse.getTransactionStatus().getPasswordDaysLeft());
            int iPasswordDaysLeft = 0;
            String error;
            String emailMessage = null;
            int warnBeforeNumdays = Integer.valueOf(NiraHelper.props.getProperty("NiraMinPasswordWarningDays", "10"));

            try {
                iPasswordDaysLeft = Integer.parseInt(iResponse.getTransactionStatus().getPasswordDaysLeft());
            } catch (Exception ex) {
                iPasswordDaysLeft = -1;
                error = ex.getMessage();
            }

            if (iPasswordDaysLeft == -1) {
                emailMessage = "Hello,\r\n\r\n Failed to determine the number of NIRA password days left, please contact NIRA staff directly to confirm and ask them "
                        + "to reset it manually if necessary. \r\n\r\nRegards,\r\nSEP";
            } else if (iPasswordDaysLeft <= warnBeforeNumdays) {
                emailMessage = "Hello, WARNING: NIRA password is about to expire, use SEP to reset NIRA password.\r\n"
                        + "The number of NIRA password days left is [" + iPasswordDaysLeft + "], which is less than or equal to minimum confugured days [" + warnBeforeNumdays + "]."
                        + "\r\n\r\nRegards,\r\nSEP";
            }
            // itops-ug@smilecoms.com
            if (emailMessage != null) { // Send it.
                IMAPUtils.sendEmail("admin@smilecoms.com", NiraHelper.props.getProperty("SmileUGPasswordExpiryNotifyEmail", "itops-ug@smilecoms.com"), null, null, "NIRA Password Expiry Notification", emailMessage);
            }
        } else {
            log.error("Unable to obtain number of password days left from NIRA");
        }

        return nResponse;

    }

    public static String changeNIRAPassword(String newPassword, String sepUserName) throws Exception {
        //Base64 of the password.
        // String b64Password =  Utils.encodeBase64(newPassword.getBytes());

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword(new String(Base64.encodeBase64(encryptWithNIRAPublicKey(newPassword.getBytes())), "UTF-8"));

        TransactionStatusResponse iResponse = NiraHelper.getNiraConnection("NiraChangePasswordURL").changePassword(request);
        // TransactionStatusResponse iResponse =  NiraHelper.getNiraConnection(null).changePassword(request);

        String responseHtml = "<ul><li>SEP User Id: " + sepUserName + "</li>"
                + "<li>NewPassword: " + newPassword + "</li>"
                + "<li>TransactionStatus: " + iResponse.getTransactionStatus().getTransactionStatus() + "</li>"
                + ((iResponse.getTransactionStatus().getError() == null) ? ""
                : "<li>ErrorCode:" + iResponse.getTransactionStatus().getError().getCode() + "</li>"
                + "<li>ErrorMessage:" + iResponse.getTransactionStatus().getError().getMessage()) + "</li>"
                + ((iResponse.getTransactionStatus().getExecutionCost() == null) ? ""
                : "<li>ExecutionCost:" + iResponse.getTransactionStatus().getExecutionCost().doubleValue()) + "</li>"
                + "<li>PasswordDaysLeft:" + iResponse.getTransactionStatus().getPasswordDaysLeft() + "</li></ul>";

        PlatformEventManager.createEvent("IM", "NiraPasswordChange", String.valueOf((new Date()).getTime()), responseHtml);

        // Update the property here:
        // Only update the property if password change was successfull
        if (iResponse.getTransactionStatus().getTransactionStatus().equalsIgnoreCase("ok")
                && (iResponse.getTransactionStatus().getError() == null)) {
            String propName = "env.nira.config";
            String props = BaseUtils.getProperty(propName);
            log.debug("Going to update NIRA password on the properties [{}] ->  [{}]", propName, props);
            UpdatePropertyRequest updateRequest = new UpdatePropertyRequest();
            updateRequest.setClient("default");
            updateRequest.setPropertyName(propName);
            props = Utils.setValueInCRDelimitedAVPString(props, "NiraPassword", newPassword);
            updateRequest.setPropertyValue(props);
            SCAWrapper.getAdminInstance().updateProperty(updateRequest);
            log.debug("Successfully updated NIRA password.");
        }
        return responseHtml;
    }

    public static byte[] encryptWithNIRAPublicKey(byte[] plaintext) throws Exception {
        // Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");   
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, readPublicKey(NiraHelper.props.getProperty("NiraPublicCertificate")));
        return cipher.doFinal(plaintext);
    }

    public static PublicKey readPublicKey(String filename) throws Exception {
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        FileInputStream is = new FileInputStream(filename);
        X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
        return cer.getPublicKey();
    }

    @Override
    public void propsAreReadyTrigger() {
        NiraHelper.initialise();
        BaseUtils.registerForPropsChanges(this);
        BaseUtils.deregisterForPropsAvailability(this);
    }

    @Override
    public void propsHaveChangedTrigger() {
        NiraHelper.initialise();
    }

    @PostConstruct
    public void startUp() {
        log.warn("NiraHelper is starting up.");
        BaseUtils.registerForPropsAvailability(this);
    }

    @PreDestroy
    public void shutDown() {
        log.warn("NiraHelper is shutting down.");
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.deregisterForPropsChanges(this);
    }

}
