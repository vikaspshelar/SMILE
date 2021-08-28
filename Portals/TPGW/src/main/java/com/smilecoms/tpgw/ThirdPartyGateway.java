/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tpgw;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.AccountQuery;
import com.smilecoms.commons.sca.BalanceTransferData;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.PurchaseUnitCreditRequest;
import com.smilecoms.commons.sca.SCAContext;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.sca.beans.ServiceBean;
import com.smilecoms.commons.sca.direct.pvs.PrepaidStrip;
import com.smilecoms.commons.sca.direct.pvs.PrepaidStripQuery;
import com.smilecoms.commons.sca.direct.pvs.PrepaidStripRedemptionData;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.FriendlyException;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.tpgw.api.TPGWApi;
import com.smilecoms.tpgw.api.TPGWApiError;
import com.smilecoms.xml.schema.tpgw.AccountList;
import com.smilecoms.xml.schema.tpgw.AddCustomerResult;
import com.smilecoms.xml.schema.tpgw.Address;
import com.smilecoms.xml.schema.tpgw.Authenticate;
import com.smilecoms.xml.schema.tpgw.AuthenticateResult;
import com.smilecoms.xml.schema.tpgw.BalanceQuery;
import com.smilecoms.xml.schema.tpgw.BalanceResult;
import com.smilecoms.xml.schema.tpgw.BalanceTransfer;
import com.smilecoms.xml.schema.tpgw.BalanceTransferResult;
import com.smilecoms.xml.schema.tpgw.Bundle;
import com.smilecoms.xml.schema.tpgw.BundleCatalogueQuery;
import com.smilecoms.xml.schema.tpgw.BundleCatalogueResult;
import com.smilecoms.xml.schema.tpgw.BundleList;
import com.smilecoms.xml.schema.tpgw.BuyBundle;
import com.smilecoms.xml.schema.tpgw.BuyBundleResult;
import com.smilecoms.xml.schema.tpgw.BuyBundleUsingPriceInCentsData;
import com.smilecoms.xml.schema.tpgw.BuyBundleUsingPriceInCentsResult;
import com.smilecoms.xml.schema.tpgw.CustomerIdByAccountIdQuery;
import com.smilecoms.xml.schema.tpgw.CustomerIdByAccountIdResult;
import com.smilecoms.xml.schema.tpgw.NinByAccountIdOrPhoneNumberQuery;
import com.smilecoms.xml.schema.tpgw.NinByAccountIdOrPhoneNumberResult;
import com.smilecoms.xml.schema.tpgw.RedemPrepaidStrip;
import com.smilecoms.xml.schema.tpgw.RedemPrepaidStripResult;
import com.smilecoms.xml.schema.tpgw.StDone;
import com.smilecoms.xml.schema.tpgw.TransactionStatusQuery;
import com.smilecoms.xml.schema.tpgw.TransactionStatusResult;
import com.smilecoms.xml.schema.tpgw.ValidateAccountQuery;
import com.smilecoms.xml.schema.tpgw.ValidateAccountResult;
import com.smilecoms.xml.schema.tpgw.ValidateEmailAddressQuery;
import com.smilecoms.xml.schema.tpgw.ValidateEmailAddressResult;
import com.smilecoms.xml.schema.tpgw.ValidatePhoneNumberQuery;
import com.smilecoms.xml.schema.tpgw.ValidatePhoneNumberResult;
import com.smilecoms.xml.schema.tpgw.ValidateReferenceIdQuery;
import com.smilecoms.xml.schema.tpgw.ValidateReferenceIdResult;
import com.smilecoms.xml.schema.tpgw.VoucherDetailResult;
import com.smilecoms.xml.schema.tpgw.VoucherDetailQuery;
import com.smilecoms.xml.tpgw.TPGWError;
import com.smilecoms.xml.tpgw.TPGWSoap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;

/**
 *
 * @author mukosi
 */
@HandlerChain(file = "/handler.xml")
@WebService(serviceName = "TPGW", portName = "TPGWSoap", endpointInterface = "com.smilecoms.xml.tpgw.TPGWSoap", targetNamespace = "http://xml.smilecoms.com/TPGW", wsdlLocation = "TPGWServiceDefinition.wsdl")
@Stateless
public class ThirdPartyGateway extends SmileWebService implements TPGWSoap {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;

    @Override
    public ValidateAccountResult validateAccount(ValidateAccountQuery validateAccountQuery) throws TPGWError {
        setContext(validateAccountQuery, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }

        ValidateAccountResult result = new ValidateAccountResult();
        boolean computeHashOnResponse = false;
        String token = "";
        try {
            if (validateAccountQuery.getHash() != null && !validateAccountQuery.getHash().isEmpty()) {
                String hash = validateAccountQuery.getHash();
                String sessionId = validateAccountQuery.getTPGWContext().getSessionId();
                //username + source account Id + sessionid + customer account id
                token = getThirdPartyUsername(sessionId) + getThirdPartyAccountBySessionId(sessionId) + sessionId;
                String accToken = token + validateAccountQuery.getAccountId();
                computeHashOnResponse = true;
                validateHash(hash, accToken);
            }
            Customer customer = TPGWApi.getInstance().validateAccount(validateAccountQuery.getAccountId());
            result.setFirstName(customer.getFirstName());
            result.setMiddleName(customer.getMiddleName());
            result.setLastName(customer.getLastName());
            result.setTPGWContext(validateAccountQuery.getTPGWContext());
            if (computeHashOnResponse) {
                token += customer.getFirstName() + customer.getLastName();
                if (log.isDebugEnabled()) {
                    log.debug("String tokens used to compute response hash {}", token);
                }
                result.setHash(getSHA512(token));
            }
        } catch (Exception ex) {
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage()));
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public ValidatePhoneNumberResult validatePhoneNumber(ValidatePhoneNumberQuery validatePhoneNumberQuery) throws TPGWError {
        setContext(validatePhoneNumberQuery, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }

        ValidatePhoneNumberResult result = new ValidatePhoneNumberResult();
        boolean computeHashOnResponse = false;
        String token = "";

        try {

            if (validatePhoneNumberQuery.getHash() != null && !validatePhoneNumberQuery.getHash().isEmpty()) {
                String hash = validatePhoneNumberQuery.getHash();
                String sessionId = validatePhoneNumberQuery.getTPGWContext().getSessionId();
                //username + source account Id + sessionid + phone-number
                token = getThirdPartyUsername(sessionId) + getThirdPartyAccountBySessionId(sessionId) + sessionId;
                String phoneToken = token + validatePhoneNumberQuery.getPhoneNumber();
                computeHashOnResponse = true;
                validateHash(hash, phoneToken);
            }

            long accountId = getPhoneNumbersAccountId(validatePhoneNumberQuery.getPhoneNumber());
            Customer customer = TPGWApi.getInstance().validateAccount(accountId);
            result.setFirstName(customer.getFirstName());
            result.setMiddleName(customer.getMiddleName());
            result.setLastName(customer.getLastName());
            result.setTPGWContext(validatePhoneNumberQuery.getTPGWContext());
            if (computeHashOnResponse) {
                token += customer.getFirstName() + customer.getLastName();
                if (log.isDebugEnabled()) {
                    log.debug("String tokens used to compute response hash {}", token);
                }
                result.setHash(getSHA512(token));
            }
        } catch (Exception ex) {
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage()));
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public AuthenticateResult authenticateUser(Authenticate authenticate) throws TPGWError {
        setContext(authenticate, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        com.smilecoms.xml.schema.tpgw.AuthenticateResult tpgwAuthResult = new com.smilecoms.xml.schema.tpgw.AuthenticateResult();

        try {
            String sessionId = Utils.getUUID();
            TPGWApi.getInstance().authenticateUser(sessionId, authenticate.getUsername(), authenticate.getPassword());
            tpgwAuthResult.setSessionId(sessionId);
            log.debug("Successfully authenticated user [{}] and allocated new session id [{}].", new Object[]{authenticate.getUsername(), sessionId});

        } catch (Exception ex) {
            log.info("Error logging in user in TPGW: " + ex.toString());
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage())); // throw processError(TPGWError.class, ex);
        }
        return tpgwAuthResult;
    }

    @Override
    public BalanceResult getBalance(BalanceQuery balanceQuery) throws TPGWError {

        setContext(balanceQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        com.smilecoms.xml.schema.tpgw.BalanceResult balanceResult = new com.smilecoms.xml.schema.tpgw.BalanceResult();

        try {
            AccountQuery accountQuery = new AccountQuery();
            accountQuery.setAccountId(balanceQuery.getAccountId());
            accountQuery.setVerbosity(StAccountLookupVerbosity.ACCOUNT);

            Account account = TPGWApi.getInstance().getBalance(accountQuery);
            balanceResult.setAvailableBalanceInCents(account.getAvailableBalanceInCents());
            balanceResult.setTPGWContext(balanceQuery.getTPGWContext());

        } catch (Exception ex) {
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage())); // throw processError(TPGWError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return balanceResult;
    }

    @Override
    public BalanceTransferResult doBalanceTransfer(BalanceTransfer balanceTransfer) throws TPGWError {
        setContext(balanceTransfer, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        BalanceTransferResult result = new BalanceTransferResult();
        com.smilecoms.commons.sca.Done isDone;

        try {

            String uniqueTxId = balanceTransfer.getUniqueTransactionId() == null ? "" : balanceTransfer.getUniqueTransactionId();

            //This is a unique identifier for a transaction, it should never be empty
            if (uniqueTxId == null || uniqueTxId.isEmpty()) {

                String errorMessage = "Element UniqueTransactionId is empty, UniqueTransactionId value is mandotory according TPGW API Spec";
                String tpUsername = getThirdPartyUsername(balanceTransfer.getTPGWContext().getSessionId());
                String bodyPart = ",<br/><br/><strong>" + tpUsername + ":</strong> Missing UniqueTransactionId on payment notification, customers will not getting their airtime. Escalate to partner with SEP username \"" + tpUsername + "\""
                        + "<br/><strong>Caused by:</strong><br/> " + errorMessage
                        + "<br/>";
                String subject = tpUsername + ": Missing UniqueTransactionId on payment notification";
                sendEmailNotification(subject, bodyPart);
                uniqueTxId = "TPGW_" + Utils.getUUID();
                if (BaseUtils.getBooleanProperty("env.tpgw.paymentgateway.enforce.required.field", true)) {
                    throw new Exception("Element UniqueTransactionId is empty, UniqueTransactionId value is mandotory according TPGW API Spec");
                }
            }
            boolean computeHashOnResponse = false;
            String tokens = "";
            String emailAddressForToAccountId = balanceTransfer.getCustomerEmailAddress() == null ? "" : balanceTransfer.getCustomerEmailAddress();
            if (!emailAddressForToAccountId.isEmpty()) {
                long accId = getTargetAccountIdFromEmailAddress(balanceTransfer.getTPGWContext().getSessionId(), emailAddressForToAccountId);
                balanceTransfer.setToAccountId(accId);
            }

            BalanceTransferData balanceTransferData = new BalanceTransferData();

            balanceTransferData.setSCAContext(new SCAContext());
            balanceTransferData.getSCAContext().setTxId("UNIQUE-" + uniqueTxId + "-" + balanceTransfer.getFromAccountId());

            if ((balanceTransfer.getToAccountId() == null || balanceTransfer.getToAccountId() == 0)
                    && balanceTransfer.getToPhoneNumber() != null && balanceTransfer.getToPhoneNumber() > 0) {
                balanceTransfer.setToAccountId(getPhoneNumbersAccountId(balanceTransfer.getToPhoneNumber()));
            }

            if (balanceTransfer.getHash() != null && !balanceTransfer.getHash().isEmpty()) {
                computeHashOnResponse = true;
                String hash = balanceTransfer.getHash();
                String sessionId = balanceTransfer.getTPGWContext().getSessionId();
                //NEW: Username + FromAccountId + SessionID + ToAccountId + UniqueTransactionId + TransferAmountInCents + Currency + ChannelUsed
                tokens = getThirdPartyUsername(sessionId) + getThirdPartyAccountBySessionId(sessionId) + sessionId;
                DecimalFormat df = new DecimalFormat("#");//remove the '.0' on the double, otherwise hash will fail
                String amt = df.format(balanceTransfer.getTransferAmountInCents());
                String channelUsed = balanceTransfer.getChannelUsed() == null ? "" : balanceTransfer.getChannelUsed();
                String currency = balanceTransfer.getCurrency() == null ? "" : balanceTransfer.getCurrency();
                String subTokens;
                if ((balanceTransfer.getToAccountId() == null || balanceTransfer.getToAccountId() == 0)
                        && balanceTransfer.getToPhoneNumber() != null && balanceTransfer.getToPhoneNumber() > 0) {
                    subTokens = tokens + balanceTransfer.getToPhoneNumber() + balanceTransfer.getUniqueTransactionId() + amt + channelUsed + currency;
                } else {
                    subTokens = tokens + balanceTransfer.getToAccountId() + balanceTransfer.getUniqueTransactionId() + amt + channelUsed + currency;
                }
                validateHash(hash, subTokens);
            }

            balanceTransferData.setAmountInCents(balanceTransfer.getTransferAmountInCents());
            balanceTransferData.setSourceAccountId(balanceTransfer.getFromAccountId());
            balanceTransferData.setTargetAccountId(balanceTransfer.getToAccountId());

            isDone = TPGWApi.getInstance().doBalanceTransfer(balanceTransferData, balanceTransfer.getChannelUsed(), balanceTransfer.getUniqueTransactionId());

            if (isDone.getDone() == com.smilecoms.commons.sca.StDone.TRUE) {
                result.setTxId(balanceTransferData.getSCAContext().getTxId());
                result.setDone(StDone.TRUE);
                result.setTPGWContext(balanceTransfer.getTPGWContext());
                if (computeHashOnResponse) {
                    tokens += balanceTransfer.getUniqueTransactionId() + result.getTxId() + String.valueOf(result.getDone()).toLowerCase();
                    if (log.isDebugEnabled()) {
                        log.debug("String tokens used to compute response hash {}", tokens);
                    }
                    result.setHash(getSHA512(tokens));
                }
            }
        } catch (Exception ex) {
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage())); //throw processError(TPGWError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }

        return result;
    }

    @Override
    public com.smilecoms.xml.schema.tpgw.Done isUp(String isUpRequest) throws TPGWError {
        return makeDone();
    }

    private com.smilecoms.xml.schema.tpgw.Done makeDone() {
        com.smilecoms.xml.schema.tpgw.Done done = new com.smilecoms.xml.schema.tpgw.Done();
        done.setDone(com.smilecoms.xml.schema.tpgw.StDone.TRUE);
        return done;
    }

    private com.smilecoms.xml.schema.tpgw.TPGWError getTPGWError(Throwable e, Object inputMsg) {

        com.smilecoms.xml.schema.tpgw.TPGWError tpgwError = new com.smilecoms.xml.schema.tpgw.TPGWError();

        if (e instanceof TPGWApiError) { // Do not report this error, already reported by TPGWApi

            tpgwError.setErrorCode(((TPGWApiError) e).getErrorCode());
            tpgwError.setErrorDesc(((TPGWApiError) e).getErrorDesc());
            tpgwError.setErrorType(((TPGWApiError) e).getErrorType());

        } else { // Not reported yet, report it first and return a new TPGWError
            ExceptionManager exm = new ExceptionManager(this.getClass().getName());
            FriendlyException fe = exm.getFriendlyException(e, inputMsg);
            exm.reportError(fe);

            try {
                tpgwError.setErrorCode(fe.getErrorCode());
                tpgwError.setErrorDesc(fe.getErrorDesc());
                tpgwError.setErrorType(fe.getErrorType());
            } catch (Exception ex) {
                log.warn("Error: ", ex);
            }
        }
        return tpgwError;
    }

    @Override
    public BundleCatalogueResult getBundleCatalogue(BundleCatalogueQuery bundleCatalogueQuery) throws TPGWError {
        return getXMLBundleCatalogueResult();
    }

    public BundleCatalogueResult getXMLBundleCatalogueResult() {
        BundleCatalogueResult bcr = new BundleCatalogueResult();
        bcr.setBundleList(new BundleList());
        for (UnitCreditSpecification ucs : TPGWApi.getInstance().getTPGWUnitCreditCatalogue()) {
            Bundle b = new Bundle();
            b.setBundleDescription(ucs.getName());
            b.setBundlePrice(ucs.getPriceInCents());
            b.setBundleTypeCode(ucs.getUnitCreditSpecificationId());
            b.setValidityDays(ucs.getUsableDays());
            bcr.getBundleList().getBundle().add(b);
        }
        bcr.setNumberOfBundles(bcr.getBundleList().getBundle().size());
        return bcr;
    }

    @Override
    public BuyBundleResult purchaseBundle(BuyBundle buyBundle) throws TPGWError {
        setContext(buyBundle, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        BuyBundleResult result = new BuyBundleResult();
        com.smilecoms.commons.sca.Done isDone;
        result.setDone(StDone.FALSE);

        try {

            String uniqueTxId = buyBundle.getUniqueTransactionId() == null ? "" : buyBundle.getUniqueTransactionId();

            //This is a unique identifier for a transaction, it should never be empty
            if (uniqueTxId == null || uniqueTxId.isEmpty()) {

                String errorMessage = "Element UniqueTransactionId is empty, UniqueTransactionId value is mandotory according TPGW API Spec";
                String tpUsername = getThirdPartyUsername(buyBundle.getTPGWContext().getSessionId());
                String bodyPart = ",<br/><br/><strong>" + tpUsername + ":</strong> Missing UniqueTransactionId on payment notification, customers will not getting their data bundle. Escalate to partner with SEP username \"" + tpUsername + "\""
                        + "<br/><strong>Caused by:</strong><br/> " + errorMessage
                        + "<br/>";
                String subject = tpUsername + ": Missing UniqueTransactionId on payment notification";
                sendEmailNotification(subject, bodyPart);
                uniqueTxId = "TPGW_" + Utils.getUUID();
                if (BaseUtils.getBooleanProperty("env.tpgw.paymentgateway.enforce.required.field", true)) {
                    throw new Exception("Element UniqueTransactionId is empty, UniqueTransactionId value is mandotory according TPGW API Spec");
                }
            }

            PurchaseUnitCreditRequest request = new PurchaseUnitCreditRequest();
            boolean computeHashOnResponse = false;
            String tokens = "";

            if (buyBundle.getHash() != null && !buyBundle.getHash().isEmpty()) {
                String hash = buyBundle.getHash();
                String sessionId = buyBundle.getTPGWContext().getSessionId();

                tokens = getThirdPartyUsername(sessionId) + getThirdPartyAccountBySessionId(sessionId) + sessionId;
                computeHashOnResponse = true;
                DecimalFormat df = new DecimalFormat("#");//remove the '.0' on the double, otherwise hash will fail
                String amt = df.format(buyBundle.getCustomerTenderedAmountInCents());
                String channelUsed = buyBundle.getChannelUsed() == null ? "" : buyBundle.getChannelUsed();
                String currency = buyBundle.getCurrency() == null ? "" : buyBundle.getCurrency();
                String subTokens;
                if ((buyBundle.getCustomerAccountId() == null || buyBundle.getCustomerAccountId() == 0)
                        && buyBundle.getPhoneNumber() != null && buyBundle.getPhoneNumber() > 0) {
                    subTokens = tokens + buyBundle.getPhoneNumber() + buyBundle.getUniqueTransactionId() + amt + channelUsed + currency + buyBundle.getBundleTypeCode() + buyBundle.getQuantityBought();
                } else {
                    subTokens = tokens + buyBundle.getCustomerAccountId() + buyBundle.getUniqueTransactionId() + amt + channelUsed + currency + buyBundle.getBundleTypeCode() + buyBundle.getQuantityBought();
                }

                validateHash(hash, subTokens);
            }

            request.setSCAContext(new SCAContext());
            request.getSCAContext().setTxId(uniqueTxId);
            request.setUniqueId(uniqueTxId + "-" + TPGWApi.getInstance().getThirdPartyAccountMappingBySessionId(buyBundle.getTPGWContext().getSessionId()));

            String emailAddressForToAccountId = buyBundle.getCustomerEmailAddress() == null ? "" : buyBundle.getCustomerEmailAddress();
            if (!emailAddressForToAccountId.isEmpty()) {
                long accId = getTargetAccountIdFromEmailAddress(buyBundle.getTPGWContext().getSessionId(), emailAddressForToAccountId);
                buyBundle.setCustomerAccountId(accId);
            }

            if ((buyBundle.getCustomerAccountId() == null || buyBundle.getCustomerAccountId() == 0)
                    && buyBundle.getPhoneNumber() != null && buyBundle.getPhoneNumber() > 0) {
                buyBundle.setCustomerAccountId(getPhoneNumbersAccountId(buyBundle.getPhoneNumber()));
            }
            if (buyBundle.getCustomerAccountId() == null) {
                // Prevent NPE and throw account not found
                buyBundle.setCustomerAccountId(0l);
            }
            request.setAccountId(buyBundle.getCustomerAccountId());
            request.setUnitCreditSpecificationId(buyBundle.getBundleTypeCode());
            request.setNumberToPurchase(buyBundle.getQuantityBought());

            isDone = TPGWApi.getInstance().doPurchaseBundle(request, buyBundle.getTPGWContext().getSessionId(), null, buyBundle.getCustomerTenderedAmountInCents(), buyBundle.getChannelUsed());
            if (isDone.getDone() == com.smilecoms.commons.sca.StDone.TRUE) {
                result.setTxId(request.getSCAContext().getTxId());
                result.setDone(StDone.TRUE);
                result.setTPGWContext(buyBundle.getTPGWContext());
                if (computeHashOnResponse) {
                    tokens += buyBundle.getUniqueTransactionId() + result.getTxId() + String.valueOf(result.getDone()).toLowerCase();
                    if (log.isDebugEnabled()) {
                        log.debug("String tokens used to compute response hash {}", tokens);
                    }
                    result.setHash(getSHA512(tokens));
                }
            }
        } catch (Exception ex) {
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage())); //throw processError(TPGWError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public TransactionStatusResult getTransactionStatus(TransactionStatusQuery transactionStatusQuery) throws TPGWError {
        setContext(transactionStatusQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        TransactionStatusResult tsResult = new TransactionStatusResult();
        try {
            String uniqueTxId = transactionStatusQuery.getUniqueTransactionId();
            Date startDate = transactionStatusQuery.getSearchStartDate() == null ? null : Utils.getJavaDate(transactionStatusQuery.getSearchStartDate());
            Date endDate = transactionStatusQuery.getSearchEndDate() == null ? null : Utils.getJavaDate(transactionStatusQuery.getSearchEndDate());
            String sessionId = transactionStatusQuery.getTPGWContext().getSessionId();
            com.smilecoms.commons.sca.TransactionStatusResult status = TPGWApi.getInstance().getTransactionStatus(uniqueTxId, sessionId, startDate, endDate);
            tsResult.setTransactionStatus(status.getTransactionStatus());
            String info = status.getTransactionInfo() == null ? "" : status.getTransactionInfo();
            tsResult.setTransactionInfo(info);
        } catch (Exception ex) {
            tsResult.setTransactionStatus("ERROR");
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage())); //throw processError(TPGWError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return tsResult;
    }

    @Override
    public com.smilecoms.xml.schema.tpgw.AddCustomerResult addCustomer(com.smilecoms.xml.schema.tpgw.Customer newCustomer) throws TPGWError {
        setContext(newCustomer, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        AddCustomerResult res = new AddCustomerResult();
        try {
            com.smilecoms.commons.sca.Customer scaCustomer = new com.smilecoms.commons.sca.Customer();
            for (Address add : newCustomer.getAddresses()) {
                com.smilecoms.commons.sca.Address scaAddress = new com.smilecoms.commons.sca.Address();
                scaAddress.setCode(add.getCode());
                scaAddress.setCountry(add.getCountry());
                scaAddress.setLine1(add.getLine1());
                scaAddress.setLine2(add.getLine2());
                scaAddress.setPostalMatchesPhysical(add.isPostalMatchesPhysical());
                scaAddress.setState(add.getState());
                scaAddress.setTown(add.getTown());
                scaAddress.setType(add.getType());
                scaAddress.setZone(add.getZone());
                scaCustomer.getAddresses().add(scaAddress);
            }
            scaCustomer.setAlternativeContact1(newCustomer.getAlternativeContact1());
            scaCustomer.setAlternativeContact2(newCustomer.getAlternativeContact2());
            scaCustomer.setDateOfBirth(newCustomer.getDateOfBirth());
            scaCustomer.setEmailAddress(newCustomer.getEmailAddress());
            scaCustomer.setFirstName(newCustomer.getFirstName());
            scaCustomer.setGender(newCustomer.getGender());
            scaCustomer.setIdentityNumber(newCustomer.getIdentityNumber());
            scaCustomer.setIdentityNumberType(newCustomer.getIdentityNumberType());
            scaCustomer.setLanguage(newCustomer.getLanguage());
            scaCustomer.setLastName(newCustomer.getLastName());
            scaCustomer.setMiddleName(newCustomer.getMiddleName());
            scaCustomer.setMothersMaidenName(newCustomer.getMothersMaidenName());
            scaCustomer.setNationality(newCustomer.getNationality());
            scaCustomer.setPassportExpiryDate(newCustomer.getPassportExpiryDate());
            scaCustomer.setSSOIdentity(newCustomer.getEmailAddress());
            scaCustomer.setClassification("tpgw");
            scaCustomer.getSecurityGroups().add("Customer");
            int profileId = TPGWApi.getInstance().addCustomer(scaCustomer);
            res.setCustomerProfileId(profileId);
        } catch (Exception ex) {
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage()));
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return res;
    }

    @Override
    public ValidateReferenceIdResult validateReferenceId(ValidateReferenceIdQuery validateReferenceIdQuery) throws TPGWError {
        setContext(validateReferenceIdQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        ValidateReferenceIdResult vr = new ValidateReferenceIdResult();
        try {
            com.smilecoms.tpgw.api.ValidateReferenceIdResult validateReferenceID = TPGWApi.getInstance().validateReferenceID(validateReferenceIdQuery.getReferenceId());
            vr.setValidationResult(validateReferenceID.getValidationResult());
            vr.setAmountInCents(validateReferenceID.getAmountInCents());
        } catch (Exception ex) {
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage()));
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return vr;
    }

    private Long getPhoneNumbersAccountId(Long toPhoneNumber) {
        ServiceBean service = ServiceBean.getServiceInstanceByIMPU(String.valueOf(toPhoneNumber));
        log.debug("Phone number [{}] has account id [{}]", toPhoneNumber, service.getAccountId());
        return service.getAccountId();
    }

    private Long getTargetAccountIdFromEmailAddress(String sessionId, String emailAddress) throws Exception {

        com.smilecoms.tpgw.api.ValidateEmailAddressResult scaResults = TPGWApi.getInstance().validateEmailAddress(emailAddress, sessionId);
        long accountId = 0;
        AccountList al = new AccountList();
        Set<Map.Entry<Long, String>> entrySet = scaResults.getAccountIdFriendlyNameMapping().entrySet();
        Iterator<Map.Entry<Long, String>> iterator = entrySet.iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, String> next = iterator.next();
            com.smilecoms.xml.schema.tpgw.Account acc = new com.smilecoms.xml.schema.tpgw.Account();
            acc.setAccountId(next.getKey());
            al.getAccount().add(acc);
        }
        //If account size is zero, an exception would have been thrown already
        if (al.getAccount().size() > 1) {
            throw new Exception("More than one accounts detected for email: " + emailAddress + ", use a specific phone number or account number instead.");
        }
        accountId = al.getAccount().get(0).getAccountId();
        log.debug("Email address [{}] has account id [{}]", emailAddress, accountId);
        return accountId;
    }

    @Override
    public ValidateEmailAddressResult validateEmailAddress(ValidateEmailAddressQuery validateEmailAddressQuery) throws TPGWError {

        setContext(validateEmailAddressQuery, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }
        boolean computeHashOnResponse = false;
        String tokens = "";
        ValidateEmailAddressResult result = new ValidateEmailAddressResult();

        try {
            String sessionId = validateEmailAddressQuery.getTPGWContext().getSessionId();
            if (validateEmailAddressQuery.getHash() != null && !validateEmailAddressQuery.getHash().isEmpty()) {
                String hash = validateEmailAddressQuery.getHash();
                //username + source account Id + sessionid + email address
                tokens = getThirdPartyUsername(sessionId) + getThirdPartyAccountBySessionId(sessionId) + sessionId;
                String eTokens = tokens + validateEmailAddressQuery.getEmailAddress();
                computeHashOnResponse = true;
                validateHash(hash, eTokens);
            }
            com.smilecoms.tpgw.api.ValidateEmailAddressResult scaResults = TPGWApi.getInstance().validateEmailAddress(validateEmailAddressQuery.getEmailAddress(), sessionId);
            result.setFirstName(scaResults.getCustomer().getFirstName());
            result.setMiddleName(scaResults.getCustomer().getMiddleName());
            result.setLastName(scaResults.getCustomer().getLastName());
            AccountList al = new AccountList();
            Set<Map.Entry<Long, String>> entrySet = scaResults.getAccountIdFriendlyNameMapping().entrySet();
            Iterator<Map.Entry<Long, String>> iterator = entrySet.iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, String> next = iterator.next();
                com.smilecoms.xml.schema.tpgw.Account acc = new com.smilecoms.xml.schema.tpgw.Account();
                acc.setAccountId(next.getKey());
                acc.setFriendlyName(next.getValue());
                al.getAccount().add(acc);
            }
            result.setAccountList(al);
            result.getAccountList().setNumberOfAccounts(al.getAccount().size());
            result.setTPGWContext(validateEmailAddressQuery.getTPGWContext());
            if (computeHashOnResponse) {
                tokens += scaResults.getCustomer().getFirstName() + scaResults.getCustomer().getLastName();
                if (log.isDebugEnabled()) {
                    log.debug("String tokens used to compute response hash {}", tokens);
                }
                result.setHash(getSHA512(tokens));
            }
        } catch (Exception ex) {
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage()));
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public BuyBundleUsingPriceInCentsResult buyBundleUsingPriceInCents(BuyBundleUsingPriceInCentsData buyBundleUsingPriceInCentsData) throws TPGWError {

        setContext(buyBundleUsingPriceInCentsData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        BuyBundleUsingPriceInCentsResult result = new BuyBundleUsingPriceInCentsResult();

        com.smilecoms.commons.sca.Done isDone;

        try {
            String uniqueTxId = buyBundleUsingPriceInCentsData.getUniqueTransactionId();
            boolean computeHashOnResponse = false;
            String tokens = "";
            String emailAddressForToAccountId = buyBundleUsingPriceInCentsData.getCustomerEmailAddress() == null ? "" : buyBundleUsingPriceInCentsData.getCustomerEmailAddress();
            if (!emailAddressForToAccountId.isEmpty()) {
                long accId = getTargetAccountIdFromEmailAddress(buyBundleUsingPriceInCentsData.getTPGWContext().getSessionId(), emailAddressForToAccountId);
                buyBundleUsingPriceInCentsData.setCustomerAccountId(accId);
            }

            BalanceTransferData balanceTransferData = new BalanceTransferData();

            if (uniqueTxId != null) {
                balanceTransferData.setSCAContext(new SCAContext());
                balanceTransferData.getSCAContext().setTxId("UNIQUE-" + uniqueTxId + "-" + buyBundleUsingPriceInCentsData.getSourceAccountId());
            }

            if ((buyBundleUsingPriceInCentsData.getCustomerAccountId() == null || buyBundleUsingPriceInCentsData.getCustomerAccountId() == 0)
                    && buyBundleUsingPriceInCentsData.getCustomerPhoneNumber() != null && buyBundleUsingPriceInCentsData.getCustomerPhoneNumber() > 0) {
                buyBundleUsingPriceInCentsData.setCustomerAccountId(getPhoneNumbersAccountId(buyBundleUsingPriceInCentsData.getCustomerPhoneNumber()));
            }

            if (buyBundleUsingPriceInCentsData.getHash() != null && !buyBundleUsingPriceInCentsData.getHash().isEmpty()) {
                computeHashOnResponse = true;
                String hash = buyBundleUsingPriceInCentsData.getHash();
                String sessionId = buyBundleUsingPriceInCentsData.getTPGWContext().getSessionId();
                //NEW: Username + FromAccountId + SessionID + CustomerAccountId + UniqueTransactionId + TransferAmountInCents + Currency + ChannelUsed
                tokens = getThirdPartyUsername(sessionId) + getThirdPartyAccountBySessionId(sessionId) + sessionId;
                DecimalFormat df = new DecimalFormat("#");//remove the '.0' on the double, otherwise hash will fail
                String amt = df.format(buyBundleUsingPriceInCentsData.getBundlePriceInCents());
                String channelUsed = buyBundleUsingPriceInCentsData.getChannelUsed() == null ? "" : buyBundleUsingPriceInCentsData.getChannelUsed();
                String currency = buyBundleUsingPriceInCentsData.getCurrency() == null ? "" : buyBundleUsingPriceInCentsData.getCurrency();
                String subTokens;
                if ((buyBundleUsingPriceInCentsData.getCustomerAccountId() == null || buyBundleUsingPriceInCentsData.getCustomerAccountId() == 0)
                        && buyBundleUsingPriceInCentsData.getCustomerPhoneNumber() != null && buyBundleUsingPriceInCentsData.getCustomerPhoneNumber() > 0) {
                    subTokens = tokens + buyBundleUsingPriceInCentsData.getCustomerPhoneNumber() + buyBundleUsingPriceInCentsData.getUniqueTransactionId() + amt + channelUsed + currency;
                } else {
                    subTokens = tokens + buyBundleUsingPriceInCentsData.getCustomerAccountId() + buyBundleUsingPriceInCentsData.getUniqueTransactionId() + amt + channelUsed + currency;
                }
                validateHash(hash, subTokens);
            }

            balanceTransferData.setAmountInCents(buyBundleUsingPriceInCentsData.getBundlePriceInCents());
            balanceTransferData.setSourceAccountId(buyBundleUsingPriceInCentsData.getSourceAccountId());
            balanceTransferData.setTargetAccountId(buyBundleUsingPriceInCentsData.getCustomerAccountId());

            isDone = TPGWApi.getInstance().buyBundleUsingPriceInCents(balanceTransferData, buyBundleUsingPriceInCentsData.getChannelUsed(), buyBundleUsingPriceInCentsData.getUniqueTransactionId());

            if (isDone.getDone() == com.smilecoms.commons.sca.StDone.TRUE) {
                result.setTxId(balanceTransferData.getSCAContext().getTxId());
                result.setDone(StDone.TRUE);
                result.setTPGWContext(buyBundleUsingPriceInCentsData.getTPGWContext());
                if (computeHashOnResponse) {
                    tokens += buyBundleUsingPriceInCentsData.getUniqueTransactionId() + result.getTxId() + String.valueOf(result.getDone()).toLowerCase();
                    if (log.isDebugEnabled()) {
                        log.debug("String tokens used to compute response hash {}", tokens);
                    }
                    result.setHash(getSHA512(tokens));
                }
            }
        } catch (Exception ex) {
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage()));
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }

        return result;
    }

    private void validateHash(String hash, String token) throws Exception {

        String shaHash = getSHA512(token.trim());
        log.debug("Partner generated hash is [{}] and Smile generated hash is: [{}], token used is [{}]", new Object[]{hash, shaHash, token});

        if (!shaHash.equalsIgnoreCase(hash)) {
            throw new Exception("Invalid hash");
        }
    }

    private String getSHA512(String text) {
        String hash = "";
        try {
            MessageDigest mda = MessageDigest.getInstance("SHA-512");
            byte[] digest = mda.digest(text.getBytes());
            hash = Codec.binToHexString(digest);
        } catch (NoSuchAlgorithmException ex) {
            log.warn("Error occured trying to hash string: {}", ex);
        }
        return hash.toUpperCase();
    }

    private long getThirdPartyAccountBySessionId(String sessionId) throws Exception {
        return TPGWApi.getInstance().getThirdPartyAccountMappingBySessionId(sessionId);
    }

    private String getThirdPartyUsername(String sessionId) throws Exception {
        return TPGWApi.getInstance().getUsername(sessionId);
    }

    private void sendEmailNotification(String subject, String message) {
        if (!BaseUtils.getBooleanProperty("env.tpgw.paymentgateway.send.email.notification", false)) {
            return;
        }
        Set<String> configWatchers = BaseUtils.getPropertyAsSet("env.tpgw.paymentgateway.email.watchers");
        String from = BaseUtils.getProperty("env.tpgw.paymentgateway.email.notification.from", "admin@smilecoms.com");
        for (String to : configWatchers) {
            String fullBody = "Hi " + to + message;
            TPGWApi.getInstance().sendEmail(from, to, subject, fullBody);
        }
    }

    @Override
    public VoucherDetailResult getVoucheDetail(VoucherDetailQuery voucherDetailQuery) throws TPGWError {
        setContext(voucherDetailQuery, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }

        VoucherDetailResult result = new VoucherDetailResult();
        try {

            PrepaidStripQuery stripQuery = new PrepaidStripQuery();
            stripQuery.setEncryptedPINHex(Codec.stringToEncryptedHexString(String.valueOf(voucherDetailQuery.getPIN())));

            PrepaidStrip prepaidstrip = TPGWApi.getInstance().getPrepaidStrip(stripQuery);

            result.setAccountId(prepaidstrip.getAccountId());
            result.setExpiryDate(prepaidstrip.getExpiryDate());
            //TODO: set all other fields

        } catch (Exception ex) {
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage())); // throw processError(TPGWError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public RedemPrepaidStripResult redemPrepaidStripVoucher(RedemPrepaidStrip redemPrepaidStrip) throws TPGWError {
        setContext(redemPrepaidStrip, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }

        RedemPrepaidStripResult result = new RedemPrepaidStripResult();
        try {

            PrepaidStripRedemptionData redemptionData = new PrepaidStripRedemptionData();
            redemptionData.setAccountId(redemPrepaidStrip.getAccountId());
            redemptionData.setRedeemedByAccountId(redemPrepaidStrip.getRedeemedByAccountId());
            redemptionData.setEncryptedPIN(Codec.stringToEncryptedHexString(redemPrepaidStrip.getEncryptedPIN()));
            log.debug("redemPrepaidStripVoucher request : PIN [{}] and EncryptedPIN [{}]", redemPrepaidStrip.getEncryptedPIN(), redemptionData.getEncryptedPIN());
            PrepaidStrip prepaidstrip = TPGWApi.getInstance().redeamPrepaidStrip(redemptionData);

            result.setAccountId(prepaidstrip.getAccountId());
            result.setExpiryDate(prepaidstrip.getExpiryDate());
            result.setStatus(prepaidstrip.getStatus());
            result.setUnitCreditSpecificationId(prepaidstrip.getUnitCreditSpecificationId());
            result.setValueInCents(prepaidstrip.getValueInCents());

        } catch (Exception ex) {
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage())); // throw processError(TPGWError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public CustomerIdByAccountIdResult getCustomerIdByAccountId(CustomerIdByAccountIdQuery customerIdByAccountIdQuery) throws TPGWError {
        setContext(customerIdByAccountIdQuery, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }

        CustomerIdByAccountIdResult result = new CustomerIdByAccountIdResult();
        boolean computeHashOnResponse = false;
        String token = "";
        try {
            if (customerIdByAccountIdQuery.getHash() != null && !customerIdByAccountIdQuery.getHash().isEmpty()) {
                String hash = customerIdByAccountIdQuery.getHash();
                String sessionId = customerIdByAccountIdQuery.getTPGWContext().getSessionId();
                //username + source account Id + sessionid + customer account id
                token = getThirdPartyUsername(sessionId) + getThirdPartyAccountBySessionId(sessionId) + sessionId;
                String accToken = token + customerIdByAccountIdQuery.getAccountId();
                computeHashOnResponse = true;
                validateHash(hash, accToken);
            }
            int customerId = TPGWApi.getInstance().getCustomerIdByAccountId(customerIdByAccountIdQuery.getAccountId());
            result.setCustomerId(customerId);
            result.setTPGWContext(customerIdByAccountIdQuery.getTPGWContext());
            if (computeHashOnResponse) {
                token += customerId;
                if (log.isDebugEnabled()) {
                    log.debug("String tokens used to compute response hash {}", token);
                }
                result.setHash(getSHA512(token));
            }
        } catch (Exception ex) {
            throw new TPGWError(ex.getMessage(), getTPGWError(ex, ex.getMessage()));
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }

    @Override
    public NinByAccountIdOrPhoneNumberResult updateNinByAccountIdOrPhoneNumber(NinByAccountIdOrPhoneNumberQuery ninByAccountIdOrPhoneNumberQuery) throws TPGWError {
        setContext(ninByAccountIdOrPhoneNumberQuery, wsctx);

        if (log.isDebugEnabled()) {
            logStart();
        }

        NinByAccountIdOrPhoneNumberResult result = new NinByAccountIdOrPhoneNumberResult();
        boolean computeHashOnResponse = false;
        String token = "";
        try {
            if (ninByAccountIdOrPhoneNumberQuery.getHash() != null && !ninByAccountIdOrPhoneNumberQuery.getHash().isEmpty()) {
                String hash = ninByAccountIdOrPhoneNumberQuery.getHash();
                String sessionId = ninByAccountIdOrPhoneNumberQuery.getTPGWContext().getSessionId();
                //username + source account Id + sessionid + customer account id
                token = getThirdPartyUsername(sessionId) + getThirdPartyAccountBySessionId(sessionId) + sessionId;
                String accToken = token + ninByAccountIdOrPhoneNumberQuery.getAccountIdOrPhoneNumber();
                computeHashOnResponse = true;
                validateHash(hash, accToken);
            }
            Customer customer = TPGWApi.getInstance().getCustomerByAccountIdOrSmileNumber(ninByAccountIdOrPhoneNumberQuery.getAccountIdOrPhoneNumber());
            if (customer != null) {
                if (customer.getNationalIdentityNumber() != null && !customer.getNationalIdentityNumber().isEmpty()) {
                    result.setMessage("NIN is already updated.");
                } else {
                    customer.setNationalIdentityNumber(ninByAccountIdOrPhoneNumberQuery.getNationalIdentityNumber());
                    TPGWApi.getInstance().updateCustomer(customer);
                    result.setMessage("The NIN has been updated successfully on your account");
                }
            }
            result.setTPGWContext(ninByAccountIdOrPhoneNumberQuery.getTPGWContext());
            if (computeHashOnResponse) {
                token += customer.getCustomerId();
                if (log.isDebugEnabled()) {
                    log.debug("String tokens used to compute response hash {}", token);
                }
                result.setHash(getSHA512(token));
            }
        } catch (Exception ex) {
            result.setMessage("Your request was not successful, Please try again or call 0702444444 for assistance.");
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return result;
    }
}
