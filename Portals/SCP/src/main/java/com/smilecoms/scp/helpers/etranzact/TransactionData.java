/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.helpers.etranzact;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.HashUtils;
import com.smilecoms.commons.util.Utils;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class TransactionData implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(TransactionData.class);
    // Fields required by eTransact
    private String terminalId;
    private String transactionId;
    private double amountInCents;
    private String responseURL;
    private int success;
    private String checkSum;
    private String correctFinalCheckSum;
    private String secretKey;
    // Fields required by Smile
    private long recipientAccountId;
    private long eTranzactAccountId;
    private String eTranzactPostURL;

    public TransactionData(double amountInCents, long recipientAccountId, HttpServletRequest request) throws Exception {
        this.amountInCents = amountInCents;
        this.recipientAccountId = recipientAccountId;

        Properties props = new Properties();
        props.load(BaseUtils.getPropertyAsStream("env.etranzact.config"));
        terminalId = props.getProperty("TerminalId");
        secretKey = Codec.encryptedHexStringToDecryptedString(props.getProperty("SecretKeyEncrypted"));
        log.debug("Secret key is [{}]", secretKey);
        eTranzactPostURL = props.getProperty("URLCustomerPostsTo");
        responseURL = props.getProperty("URLeTranzactPostsTo") + ";jsessionid=" + request.getRequestedSessionId();
        transactionId = Utils.getUUID();
        List<String> mappings = BaseUtils.getPropertyAsList("env.tpgw.account.mappings");
        for (String mapping : mappings) {
            String[] bits = mapping.split("\\|");
            if (bits[0].equalsIgnoreCase("etranzact")) {
                eTranzactAccountId = Long.parseLong(bits[1]);
                log.debug("ETransacts account id is [{}]", eTranzactAccountId);
            }
        }
        if (eTranzactAccountId == 0) {
            throw new Exception("ETransact account mapping is not set up in env.tpgw.account.mappings");
        }
    }

    public String getTerminalId() {
        return terminalId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAmountInMajorCurrencyUnit() {
        return String.valueOf(amountInCents / 100);
    }

    public double getAmountInCents() {
        return amountInCents;
    }
    

    public String getDescription() {
        return recipientAccountId + ":" + amountInCents;
    }

    public String getResponseURL() {
        return responseURL;
    }

    public String getLogoURL() {
        return "";
    }

    public String getEchoData() {
        return "";
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public String getETranzactPostURL() {
        return eTranzactPostURL;
    }

    /*
     * MD5 encrypt with :AMOUNT+TERMINAL_ID+TRANSACTION_ID+RESPONSE_URL+SECRETKEY

     */
    public String getCheckSum() {
        if (checkSum != null) {
            return checkSum;
        }
        StringBuilder checksumsb = new StringBuilder();
        checksumsb.append(getAmountInMajorCurrencyUnit());
        checksumsb.append(getTerminalId());
        checksumsb.append(getTransactionId());
        checksumsb.append(getResponseURL());
        checksumsb.append(secretKey);
        checkSum = HashUtils.md5AsHex(checksumsb.toString());
        return checkSum;
    }

    /*
     * MD5 encrypted with :SUCCESS+AMOUNT+TERMINAL_ID+TRANSACTION_ID+RESPONSE_URL+SECRETKEY

     */
    public String getCorrectFinalCheckSum() {
        if (correctFinalCheckSum != null) {
            return correctFinalCheckSum;
        }
        StringBuilder checksumsb = new StringBuilder();
        checksumsb.append(success);
        checksumsb.append(getAmountInMajorCurrencyUnit());
        checksumsb.append(getTerminalId());
        checksumsb.append(getTransactionId());
        checksumsb.append(getResponseURL());
        checksumsb.append(secretKey);
        correctFinalCheckSum = HashUtils.md5AsHex(checksumsb.toString());
        return correctFinalCheckSum;
    }

    public boolean isFinalCheckSumCorrect(String finalCheckSum) {
        return getCorrectFinalCheckSum().equalsIgnoreCase(finalCheckSum);
    }

    public long getRecipientAccountId() {
        return recipientAccountId;
    }

    public long getETranzactAccountId() {
        return eTranzactAccountId;
    }

    @Override
    public String toString() {
        return "CheckSum=" + getCheckSum() + " Description=" + getDescription() + " Callback URL=" + getResponseURL()
                + " TransactionId=" + getTransactionId() + " RecipientAccountId=" + getRecipientAccountId() + " eTranzactAccountId=" + getETranzactAccountId();
    }
}
