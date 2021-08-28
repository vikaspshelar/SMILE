/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pvs;

import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.sca.AccountQuery;
import com.smilecoms.commons.sca.CustomerCommunicationData;
import com.smilecoms.commons.sca.Organisation;
import com.smilecoms.commons.sca.OrganisationList;
import com.smilecoms.commons.sca.OrganisationQuery;
import com.smilecoms.commons.sca.Photograph;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.SaleLine;
import com.smilecoms.commons.sca.SalesList;
import com.smilecoms.commons.sca.SalesQuery;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StOrganisationLookupVerbosity;
import com.smilecoms.commons.sca.StSaleLookupVerbosity;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.pgpexample.PGPFileUtils;
import com.smilecoms.pvs.db.model.PrepaidStrip;
import com.smilecoms.pvs.db.model.PrepaidVoucherSystemLock;
import com.smilecoms.pvs.db.op.DAO;
import com.smilecoms.xml.pvs.PVSError;
import com.smilecoms.xml.pvs.PVSSoap;
import com.smilecoms.xml.schema.pvs.Done;
import com.smilecoms.xml.schema.pvs.NewPrepaidStripsData;
import com.smilecoms.xml.schema.pvs.PlatformInteger;
import com.smilecoms.xml.schema.pvs.PrepaidStripBatchData;
import com.smilecoms.xml.schema.pvs.PrepaidStripCountQuery;
import com.smilecoms.xml.schema.pvs.PrepaidStripQuery;
import com.smilecoms.xml.schema.pvs.PrepaidStripRedemptionData;
import com.smilecoms.xml.schema.pvs.PrepaidStrips;
import com.smilecoms.xml.schema.pvs.PrepaidStripsQuery;
import com.smilecoms.xml.schema.pvs.ResetAccountVoucherLock;
import com.smilecoms.xml.schema.pvs.SendPrepaidStripsData;
import com.smilecoms.xml.schema.pvs.StDone;
import com.smilecoms.xml.schema.pvs.VoucherLockForAccount;
import com.smilecoms.xml.schema.pvs.VoucherLockForAccountQuery;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 *
 * @author paul,abhilash
 */
@WebService(serviceName = "PVS", portName = "PVSSoap", endpointInterface = "com.smilecoms.xml.pvs.PVSSoap", targetNamespace = "http://xml.smilecoms.com/PVS", wsdlLocation = "PVSServiceDefinition.wsdl")
@HandlerChain(file = "/handler.xml")
@Stateless
public class PrepaidVoucherSystem extends SmileWebService implements PVSSoap {

    private static final int PIN_LENGTH = 15;
    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;
    @PersistenceContext(unitName = "PVSPU")
    private EntityManager em;
    public static Properties props = null;

    @Override
    public PrepaidStrips getPrepaidStrips(PrepaidStripsQuery prepaidStripsQuery) throws PVSError {
        
        Collection <PrepaidStrip> strips = DAO.getPrepaidStripsForInvoiceAndStatuss(em, prepaidStripsQuery.getSaleId(), prepaidStripsQuery.getStatus());
        PrepaidStrips response = new PrepaidStrips();
        
        if(strips != null) {
            for(PrepaidStrip strip : strips) {
                response.getPrepaidStrips().add(getXMLPrepaidStrip(strip));
            }
        }
        
        return response;
    }

    @Override
    public Done sendPrepaidStripsForInvoice(SendPrepaidStripsData request) throws PVSError {
        setContext(request, wsctx);
        try {
            this.generateAndSendVoucherPinsCSVFileForInvoice(request.getSaleId());
            return makeDone();
        } catch (Exception ex) {
            throw processError(PVSError.class, ex);
        } finally {
            createEvent(request.getSaleId());
        }
    }    

    public static enum STRIP_STATUS {

        GE, EX, WH, DC, RI, RE;
    };

    @Override
    public com.smilecoms.xml.schema.pvs.Done isUp(java.lang.String isUpRequest) throws com.smilecoms.xml.pvs.PVSError {
        return makeDone();
    }

    @Override
    public PlatformInteger getPrepaidStripCount(PrepaidStripCountQuery prepaidStripCountQuery) throws PVSError {
        setContext(prepaidStripCountQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        PlatformInteger cnt = new PlatformInteger();
        try {
            if(!StringUtils.isEmpty(prepaidStripCountQuery.getInvoiceData()) && !StringUtils.isEmpty(prepaidStripCountQuery.getStatus())) {
                cnt.setInteger(DAO.getStripCountForSaleIdAndStatusFilter(em, prepaidStripCountQuery.getInvoiceData(), prepaidStripCountQuery.getStatus()));
            } else
            if (prepaidStripCountQuery.getInvoiceData() != null && !prepaidStripCountQuery.getInvoiceData().isEmpty()) {
                cnt.setInteger(DAO.getStripCountForInvoiceDataAndValue(em, prepaidStripCountQuery.getInvoiceData(), prepaidStripCountQuery.getValueInCents()));
            } else {
                cnt.setInteger(DAO.getStripCountForIdRange(em, prepaidStripCountQuery.getStartingPrepaidStripId(), prepaidStripCountQuery.getEndingPrepaidStripId()));
            }
        } catch (Exception e) {
            throw processError(PVSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return cnt;
    }

    @Override
    public PrepaidStrips createPrepaidStrips(NewPrepaidStripsData newPrepaidStripsData) throws PVSError {
        setContext(newPrepaidStripsData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        PrepaidStrips psList = new PrepaidStrips();        
        try {
                int lastStripIDGenerated=DAO.getLastStripIdGenerated(em);
                
                boolean isValidTx=true;
                if(newPrepaidStripsData.getUnitCreditSpecificationId()!=0) {  
                    isValidTx=DAO.checkUnitCreditSpecExists(em,newPrepaidStripsData.getUnitCreditSpecificationId());
                }
                else
                {
                    String denominationList=BaseUtils.getProperty("env.pvs.allowed.airtime","");
                    isValidTx=denominationList.contains("#"+String.valueOf((int)newPrepaidStripsData.getValueInCents()/100)+"#");
                }
                                
                if(isValidTx && newPrepaidStripsData.getNumberOfStrips() !=0 && newPrepaidStripsData.getNumberOfStrips()%10==0)
                {
                    
                    for (int i = 0; i < newPrepaidStripsData.getNumberOfStrips(); i++) 
                    {
                        if (log.isDebugEnabled()) {
                            log.debug("Creating strip [{}] of [{}]", i + 1, newPrepaidStripsData.getNumberOfStrips());
                        }
                        newPrepaidStripsData.getExpiryDate().setTime(0, 0, 0); // ignore the time part of the date
                        PrepaidStrip ps = createPrepaidStrip(newPrepaidStripsData.getValueInCents(), Utils.getJavaDate(newPrepaidStripsData.getExpiryDate()), newPrepaidStripsData.getUnitCreditSpecificationId(), newPrepaidStripsData.getAccountId());
                        psList.getPrepaidStrips().add(getXMLPrepaidStrip(ps));
                    }
                    createEvent(newPrepaidStripsData.getNumberOfStrips());                    
                }
                else
                {
                    throw new Exception("Please check if Unit Specification ID/ Airtime value is valid or number of strips is a multiple of 10 ");
                }
                
                String fileLocation = System.getProperty("java.io.tmpdir") + File.separator + "/VOUCHER_PINS_FOR_X3.csv";
                
                int newLastStripIdGenerated = generateCSVforX3(psList,lastStripIDGenerated,fileLocation);
                
                boolean success=sendCSVToX3(fileLocation,lastStripIDGenerated+2,newLastStripIdGenerated);   
                if(success)
                {
                    DAO.bulkUpdate(em,lastStripIDGenerated+2,newLastStripIdGenerated,STRIP_STATUS.valueOf("EX"),"");
                }                
                
        } catch (Exception e) {
            throw processError(PVSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return psList;
    }

    @Override
    public Done batchUpdatePrepaidStrips(PrepaidStripBatchData prepaidStripBatchData) throws PVSError {
        setContext(prepaidStripBatchData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        try {
            
            if(!StringUtils.isEmpty(prepaidStripBatchData.getInvoiceData())) { // Batch update using invoice number(sale id)
                int saleId = Integer.valueOf(prepaidStripBatchData.getInvoiceData());
                
                SalesQuery salesQuery =  new SalesQuery();
                
                salesQuery.setVerbosity(StSaleLookupVerbosity.SALE_LINES);
                salesQuery.getSalesIds().add(saleId);
                
                SalesList saleList = SCAWrapper.getAdminInstance().getSales(salesQuery);
                // PVS-0012
                if(saleList.getNumberOfSales() != 1) {
                    throw new Exception("Invalid sale id supplied for prepaid strips batch update.");
                }
                // PVS-0011
                if(!saleList.getSales().get(0).getStatus().equalsIgnoreCase("PD")) {
                    throw new Exception("The sale is not paid yet -- current sale status " + saleList.getSales().get(0).getStatus());
                }
                
                // Do some checking here
                for(SaleLine line : saleList.getSales().get(0).getSaleLines()) {
                    
                    //PVS-0010
                    if(!line.getInventoryItem().getItemNumber().startsWith("VOU"))  { // Non voucher item encountered
                        throw new Exception("Sale contains an item which is not a voucher box -- check, sale row id " + line.getLineId() + ", item number" + line.getInventoryItem().getItemNumber() + " of sale " + saleList.getSales().get(0).getSaleId() + ".");
                    }
                    
                    String strBoxSize = Utils.getValueFromCRDelimitedAVPString(line.getProvisioningData(), "BoxSize");
                    //PVS-0009
                    if(StringUtils.isEmpty(strBoxSize)) {
                        throw new Exception("Boxsize setting not set for line item -- sale row id " + line.getLineId() + " of sale " + saleList.getSales().get(0).getSaleId() + ".");
                    }
                    //Check if is is a valid number
                    // PVS-0013
                    if(!StringUtils.isNumeric(strBoxSize)) {
                        throw new Exception("Invalid number format for BoxSixe -- BoxSize:" + strBoxSize + " specified for Boxsize of line item " + line.getLineId() + " of sale " + saleList.getSales().get(0).getSaleId());
                    }
                    
                    // 0001-0001
                    String [] serialComponents = line.getInventoryItem().getSerialNumber().split("-");
                    if(serialComponents.length < 2) {
                        throw new Exception("Invalid serial number format -- serial number " + line.getInventoryItem().getSerialNumber() + " specified for line item " + line.getLineId() + " of sale " + saleList.getSales().get(0).getSaleId());
                    }
                    
                    int startingStripId = 0;
                    
                    try {
                        startingStripId = Integer.valueOf(serialComponents[0]);
                    } catch (Exception ex) {
                        throw new Exception("Invalid number format for starting serial number -- serial number: " + line.getInventoryItem().getSerialNumber() + " specified for line item " + line.getLineId() + " of sale " + saleList.getSales().get(0).getSaleId() + " -- " + ex.getMessage());
                    }
                    
                    int actualCount =  DAO.getStripCountForStartingIdAndBoxSize(em, startingStripId, Integer.valueOf(strBoxSize));
                    
                    if(Integer.valueOf(strBoxSize) != actualCount) {
                        throw new Exception("The number of strips on the system does not match the content of the boxes -- actual strips count " + actualCount + ", box size " + strBoxSize + " sale  id " + saleList.getSales().get(0).getSaleId());
                    }
                
                }
                
                // Now do the actual status update here for each box...
                for(SaleLine line : saleList.getSales().get(0).getSaleLines()) {
                    // Need to calculate ending strip id here.
                    String strBoxSize = Utils.getValueFromCRDelimitedAVPString(line.getProvisioningData(), "BoxSize");
                    String [] serialComponents = line.getInventoryItem().getSerialNumber().trim().split("-");
                    
                    int startingStripId = Integer.valueOf(serialComponents[0]);
                    int endingStripId = DAO.getLastStripIdInTheRangeForBox(em, startingStripId, Integer.valueOf(strBoxSize));
                    DAO.bulkUpdate(em, startingStripId, endingStripId, STRIP_STATUS.valueOf(prepaidStripBatchData.getStatus()), prepaidStripBatchData.getInvoiceData());
                } 
                
                //If we get here - it means status updates are all fine
                if(STRIP_STATUS.valueOf(prepaidStripBatchData.getStatus()).equals(STRIP_STATUS.DC)) {
                    // Vooucher PINS have just been released to market -  generate and send Encrypted Excel file here
                    if(BaseUtils.getBooleanProperty("env.pvs.dc.send.pgp.encrypted.file", false)) {
                        generateAndSendVoucherPinsCSVFileForInvoice(Integer.valueOf(prepaidStripBatchData.getInvoiceData()));
                    }
                }
            } else
            if(prepaidStripBatchData.getStartingPrepaidStripId() > 0 && prepaidStripBatchData.getEndingPrepaidStripId() > 0) {
                DAO.bulkUpdate(em, prepaidStripBatchData.getStartingPrepaidStripId(), prepaidStripBatchData.getEndingPrepaidStripId(), STRIP_STATUS.valueOf(prepaidStripBatchData.getStatus()), prepaidStripBatchData.getInvoiceData());
                createEvent(prepaidStripBatchData.getStartingPrepaidStripId());
            } else {
                throw new Exception("Prepaid Strip Batch Data did not contain any data.");
            }
        } catch (Exception e) {
            throw processError(PVSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return makeDone();
    }

    @Override
    public com.smilecoms.xml.schema.pvs.PrepaidStrip getPrepaidStrip(PrepaidStripQuery prepaidStripQuery) throws PVSError {
        setContext(prepaidStripQuery, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        com.smilecoms.xml.schema.pvs.PrepaidStrip strip = null;
        try {

            if (prepaidStripQuery.getPrepaidStripId() == 0 && prepaidStripQuery.getPlatformContext().getOriginatingIdentity().equalsIgnoreCase("admin")) {
                // Hack - an easy way of kicking off checksum regeneration
                regenerateCheckSums();
            }
            
            PrepaidStrip pvDb = null;
            if(prepaidStripQuery.getPrepaidStripId() > 0) {
                pvDb = DAO.getStrip(em, prepaidStripQuery.getPrepaidStripId());
            } else 
                if(!StringUtils.isEmpty(prepaidStripQuery.getEncryptedPINHex())) {
                pvDb = DAO.getStripUsingEncryptedPINHex(em, prepaidStripQuery.getEncryptedPINHex());
            }
            strip = getXMLPrepaidStrip(pvDb);
        } catch (Exception e) {
            throw processError(PVSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return strip;
    }

    @Override
    public com.smilecoms.xml.schema.pvs.PrepaidStrip redeemPrepaidStrip(PrepaidStripRedemptionData PrepaidStripRedemptionData) throws PVSError {
        setContext(PrepaidStripRedemptionData, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }
        
        boolean thirdPartyRedemption = false;
        boolean wrongAccount =  false;
        long accountIdForVoucherLock;
        if(PrepaidStripRedemptionData.getRedeemedByAccountId() > 0) {
            accountIdForVoucherLock =  PrepaidStripRedemptionData.getRedeemedByAccountId();
            thirdPartyRedemption = true;
        } else   {
            accountIdForVoucherLock = PrepaidStripRedemptionData.getAccountId();
        }

        com.smilecoms.xml.schema.pvs.PrepaidStrip psXml = null;
        try {
            PrepaidVoucherSystemLock pvl = null;
            try {
                    pvl = DAO.getVoucherLockForAccount(em, accountIdForVoucherLock);
                    Date now = new Date();
                    if (pvl.getLockedUntilTimestamp() != null && pvl.getLockedUntilTimestamp().after(now)) {
                        throw new Exception("Account is temporarily blocked from recharge attempts -- " + accountIdForVoucherLock);
                    }
                
            } catch (javax.persistence.NoResultException nre) {
            }

            try {
                if(thirdPartyRedemption) {
                    // Chech if account exists.
                    AccountQuery aQuery = new AccountQuery();
                    aQuery.setAccountId(PrepaidStripRedemptionData.getAccountId());
                    aQuery.setVerbosity(StAccountLookupVerbosity.ACCOUNT);
                    try {
                         SCAWrapper.getAdminInstance().getAccount(aQuery);
                    } catch (Exception ex) {
                        wrongAccount = true;
                        throw new javax.persistence.NoResultException("Account does not exist.");
                    }
                }
                PrepaidStrip pvDb = DAO.getUnredeemedStrip(em, PrepaidStripRedemptionData.getEncryptedPIN());
                if (pvDb.getAccountId() > 0 && pvDb.getAccountId() != PrepaidStripRedemptionData.getAccountId()) {
                    throw new Exception("Buddy, strip doesn't belong to you");
                }
                if ((new Date().after(pvDb.getExpiryDate()))) {
                    throw new Exception("Strip has expired");
                }
                if (!isChecksumCorrect(pvDb)) {
                    throw new Exception("Strip data is corrupt or tampered with");
                }
                if (pvl != null) {
                    // delete any locks
                    em.remove(pvl);
                    em.flush();
                }
                psXml = getXMLPrepaidStrip(pvDb);
                createEvent(pvDb.getPrepaidStripId());
            } catch (javax.persistence.NoResultException nre) {
                // Invalid voucher code
                if (pvl == null) {
                    // no locks yet
                    pvl = new PrepaidVoucherSystemLock();
                    pvl.setAccountId(accountIdForVoucherLock);
                    
                    if(thirdPartyRedemption && wrongAccount) {
                        pvl.setAccountAttempts(1);
                        pvl.setAttempts(0);
                    } else {
                        pvl.setAttempts(1);
                        pvl.setAccountAttempts(0);
                    }
                    pvl.setLockedUntilTimestamp(null);
                } else {
                    if(thirdPartyRedemption && wrongAccount) {
                        pvl.setAccountAttempts(pvl.getAccountAttempts() + 1);
                    } else {
                        pvl.setAttempts(pvl.getAttempts() + 1);
                    }
                }
                
                if (pvl.getAccountAttempts() >= BaseUtils.getIntProperty("env.pvs.voucherlock.accountattempts.count",2)) {
                    Calendar future = Calendar.getInstance();
                    future.add(GregorianCalendar.MINUTE, BaseUtils.getIntProperty("env.pvs.voucherlock.duration.minutes",10));
                    pvl.setLockedUntilTimestamp(future.getTime());
                }
                
                // After 5 incorrect attempts, the account cannot recharge for 10 minutes
                if (pvl.getAttempts() >= BaseUtils.getIntProperty("env.pvs.voucherlock.count",5)) {
                    Calendar future = Calendar.getInstance();
                    future.add(GregorianCalendar.MINUTE, BaseUtils.getIntProperty("env.pvs.voucherlock.duration.minutes",10));
                    pvl.setLockedUntilTimestamp(future.getTime());
                }      
                
                em.persist(pvl);
                em.flush();
                throw new Exception("Invalid strip PIN");
            }
        } catch (Exception e) {
            throw processError(PVSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return psXml;
    }

    @Override
    public com.smilecoms.xml.schema.pvs.PrepaidStrip updatePrepaidStrip(com.smilecoms.xml.schema.pvs.PrepaidStrip updatedPrepaidStrip) throws PVSError {
        setContext(updatedPrepaidStrip, wsctx);
        if (log.isDebugEnabled()) {
            logStart();
        }

        com.smilecoms.xml.schema.pvs.PrepaidStrip psXml = null;
        try {
            PrepaidStrip pvDb = DAO.getStrip(em, updatedPrepaidStrip.getPrepaidStripId());
            pvDb.setRedemptionAccountHistoryId(BigInteger.valueOf(updatedPrepaidStrip.getRedemptionAccountHistoryId()));
            pvDb.setRedemptionAccountId(updatedPrepaidStrip.getRedemptionAccountId());
            pvDb.setStatus(updatedPrepaidStrip.getStatus());
            em.persist(pvDb);
            psXml = getXMLPrepaidStrip(pvDb);
            createEvent(pvDb.getPrepaidStripId());
        } catch (Exception e) {
            throw processError(PVSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return psXml;
    }

    private String generateUnusedEncryptedPIN() throws Exception {
        boolean gotOne = false;
        String thisTry = null;
        while (!gotOne) {
            thisTry = generateRandomEncryptedPIN();
            if (!DAO.unredeemedStripExists(em, thisTry)) {
                gotOne = true;
            }
        }
        return thisTry;
    }

    private String generateRandomEncryptedPIN() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PIN_LENGTH; i++) {
            sb.append(getRandomNumericDigit());
        }
        return Codec.stringToEncryptedHexString(sb.toString());
    }

    private String getRandomNumericDigit() {
        Random r = new Random();
        return String.valueOf(r.nextInt(10));
    }

    private com.smilecoms.xml.schema.pvs.PrepaidStrip getXMLPrepaidStrip(PrepaidStrip pvDb) {
        com.smilecoms.xml.schema.pvs.PrepaidStrip psXml = new com.smilecoms.xml.schema.pvs.PrepaidStrip();
        psXml.setPrepaidStripId(pvDb.getPrepaidStripId());
        psXml.setGeneratedDate(Utils.getDateAsXMLGregorianCalendar(pvDb.getGeneratedDate()));
        psXml.setExpiryDate(Utils.getDateAsXMLGregorianCalendar(pvDb.getExpiryDate()));
        if (pvDb.getRedemptionAccountHistoryId() != null) {
            psXml.setRedemptionAccountHistoryId(pvDb.getRedemptionAccountHistoryId().longValue());
        }
        if (pvDb.getRedemptionAccountId() != null) {
            psXml.setRedemptionAccountId(pvDb.getRedemptionAccountId());
        }
        if (pvDb.getInvoiceData() != null) {
            psXml.setInvoiceData(pvDb.getInvoiceData());
        }
        psXml.setStatus(pvDb.getStatus());
        psXml.setValueInCents(pvDb.getValueCents().doubleValue());
        psXml.setUnitCreditSpecificationId(pvDb.getUnitCreditSpecificationId());
        psXml.setAccountId(pvDb.getAccountId());
        psXml.setPIN(pvDb.getPIN());
        return psXml;
    }

    private boolean isChecksumCorrect(PrepaidStrip pvDb) {
        String dbChecksum = pvDb.getChecksum();
        String correctChecksum = generateChecksum(pvDb.getEncryptedPINHex(), pvDb.getValueCents());
        if (dbChecksum.equals(correctChecksum)) {
            return true;
        } else {
            log.warn("Checksums for a voucher do not match DB:[{}] and Correct:[{}]", dbChecksum, correctChecksum);
            return false;
        }
    }

    private String generateChecksum(String encryptedPINHex, BigDecimal valueCents) {
        StringBuilder sb = new StringBuilder();
        sb.append("89&^hHui");
        sb.append(encryptedPINHex);
        sb.append(String.valueOf(valueCents.longValue()));
        sb.append("(&*^TRJdaVBI(*Y&GU");
        return Utils.oneWayHash(sb.toString());
    }

    private Done makeDone() {
        Done done = new Done();
        done.setDone(StDone.TRUE);
        return done;
    }

    private PrepaidStrip createPrepaidStrip(double valueInCents, Date expiry, int ucsId, Long accId) throws PVSError {
        PrepaidStrip pv = new PrepaidStrip();
        try {            
            pv.setEncryptedPINHex(generateUnusedEncryptedPIN());
            pv.setGeneratedDate(Utils.getCurrentJavaDate());
            pv.setExpiryDate(expiry);
            pv.setRedemptionAccountHistoryId(null);
            pv.setRedemptionAccountId(null);
            pv.setInvoiceData(null);
            pv.setStatus(STRIP_STATUS.GE.toString());
            pv.setValueCents(BigDecimal.valueOf(valueInCents));
            pv.setPIN(Codec.encryptedHexStringToDecryptedString(pv.getEncryptedPINHex()));
            pv.setChecksum(generateChecksum(pv.getEncryptedPINHex(), pv.getValueCents()));
            pv.setUnitCreditSpecificationId(ucsId);
            pv.setAccountId(accId);
            em.persist(pv);
            em.flush();
            em.refresh(pv);
            
            return pv;
        } catch (Exception e) {
            throw processError(PVSError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
    }

    private void regenerateCheckSums() {
        log.warn("Administrator is regenerating checksums");
        Query q = em.createNativeQuery("select * from prepaid_strip where STATUS!=?", PrepaidStrip.class);
        q.setParameter(1, STRIP_STATUS.RE.toString());
        List<PrepaidStrip> strips = q.getResultList();
        int regen = 0;
        for (PrepaidStrip strip : strips) {
            String newChecksum = generateChecksum(strip.getEncryptedPINHex(), strip.getValueCents());
            String currentChecksum = strip.getChecksum();
            if (!currentChecksum.equals(newChecksum)) {
                log.debug("Updating checksum for strip id [{}]", strip.getPrepaidStripId());
                strip.setChecksum(newChecksum);
                em.persist(strip);
                regen++;
            }
        }
        log.warn("Administrator is finished regenerating checksums. Number regenerated = [{}]", regen);
    }
    
    @Override
    public VoucherLockForAccount getVoucherLockForAccount(VoucherLockForAccountQuery voucherLockForAccountQuery) throws PVSError {
        // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
        VoucherLockForAccount vlA = new VoucherLockForAccount();
        
        PrepaidVoucherSystemLock pvl = null;
        
        try {
            pvl = DAO.getVoucherLockForAccount(em, voucherLockForAccountQuery.getAccountId());
            vlA = getXMLVoucherLockForAccount(pvl);             
        } catch (javax.persistence.NoResultException nre) {
            vlA.setFound(false);
        }
        
        return vlA; // Will be null if it does not exist.
    }
    
    
    private com.smilecoms.xml.schema.pvs.VoucherLockForAccount getXMLVoucherLockForAccount(PrepaidVoucherSystemLock pvDb) {
        com.smilecoms.xml.schema.pvs.VoucherLockForAccount vlXml = new com.smilecoms.xml.schema.pvs.VoucherLockForAccount();
        vlXml.setAccountId(pvDb.getAccountId());
        vlXml.setAttempts(pvDb.getAttempts());
        vlXml.setAccountAttempts(pvDb.getAccountAttempts());
        vlXml.setLockUntilTimestamp(Utils.getDateAsXMLGregorianCalendar(pvDb.getLockedUntilTimestamp()));
        vlXml.setFound(true);
        return vlXml;
    }
    
    
    
    @Override
    public Done resetVoucherLockForAccount(ResetAccountVoucherLock resetAccountVoucherLock) throws PVSError {
        Done done = new Done();
        
        DAO.deletePrepaidVoucherLock(em, resetAccountVoucherLock.getAccountIdToReset());
        done.setDone(StDone.TRUE);
        return done;
        
    }
    
    private void generateAndSendVoucherPinsCSVFileForInvoice(int saleId) throws Exception {
        log.debug("In GenerateVoucherPinsSpreadsheetForInvoice id [{}]", saleId);
        try {
            
            //Rettieve the sale here first.
            SalesQuery salesQuery =  new SalesQuery();
                
            salesQuery.setVerbosity(StSaleLookupVerbosity.SALE_LINES);
            salesQuery.getSalesIds().add(saleId);

            SalesList saleList = SCAWrapper.getAdminInstance().getSales(salesQuery);
            // PVS-0012
            if(saleList.getNumberOfSales() != 1) {
                throw new Exception("Invalid sale id supplied");
            }
            // PVS-0011
            if(!saleList.getSales().get(0).getStatus().equalsIgnoreCase("PD")) {
                throw new Exception("The sale is not paid yet -- current sale status " + saleList.getSales().get(0).getStatus());
            }
                
            int partnerOrganisationId = saleList.getSales().get(0).getRecipientOrganisationId();
            int recipientCustId = saleList.getSales().get(0).getRecipientCustomerId();
            
            if(!(partnerOrganisationId > 0)) {
                throw new Exception("Sale was not made to an organization -- sale id [" + saleId + "].");
            }
            
            // Retrieve organisation here
            OrganisationQuery orgQ = new OrganisationQuery();
            
            orgQ.setOrganisationId(partnerOrganisationId);
            orgQ.setVerbosity(StOrganisationLookupVerbosity.MAIN_PHOTO);
            
            OrganisationList orgList = SCAWrapper.getAdminInstance().getOrganisations(orgQ);
            
            if(orgList.getNumberOfOrganisations() != 1) {
                throw new Exception("Recipient organisation does not exist -- Organisation Id [" + partnerOrganisationId + "]");
            }
            
            Organisation org = orgList.getOrganisations().get(0);
            
            // Find the public key
            Photograph pubKey = null;
            
            for(Photograph p: org.getOrganisationPhotographs()) {
                if(p.getPhotoType() != null  && p.getPhotoType().equalsIgnoreCase("publickey")) {
                    pubKey = p;
                    break;
                }
            }
            
            if(pubKey == null) { // Organisation does not have public key loaded
                throw new Exception("Recipeint organisation does not have public key document attached --- Organisation Id [" + partnerOrganisationId + "]");
            }
            
            // Now pull the customer profile to get recipient's email address.
            /*CustomerQuery custQ = new CustomerQuery();
            custQ.setCustomerId(recipientCustId);
            custQ.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            custQ.setResultLimit(1);
            
            customer customer = SCAWrapper.getAdminInstance().getCustomer(custQ); */
                    
             
            // Do some checking here
            // for(SaleLine line : saleList.getSales().get(0).getSaleLines()) {
                
            //}
                
            String status = "DC";
            Collection <PrepaidStrip> strips = DAO.getPrepaidStripsForInvoiceAndStatuss(em, saleId, status);
            PrepaidStrips response = new PrepaidStrips();
            
            if(strips == null || strips.size() <= 0) {
                throw new Exception("No strips found for invoice -- " + "sale id " + saleId + " and status '" + status + "'");
            }
            
            log.debug("Found [{}] voucher strips for sale id [{}] to write to export into file.", strips.size(), saleId);
            
            // Create workbook and header row here
            // Write strip to Excel;
            String fileLocation = System.getProperty("java.io.tmpdir") + File.separator + "/VOUCHER_PINS_" + saleId + ".txt";
            
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileLocation));
            
            // Workbook workbook = new XSSFWorkbook();

            //String headerRow = "PIN,SERIAL_NUMBER,DENOMINATION,EXPIRY_DATE,BUNDLE_NAME";
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            //writer.write(headerRow);
            //writer.newLine();
            
            Map <Integer, String> bundles = DAO.getUnitCreditNames(em);
                        
            // For each voucher strip
            // String serialFormat = String.format("%020d", number);
            // .divide(new BigDecimal(100)
            // 20238352263193330429
            
            for(PrepaidStrip strip : strips) {
                
                String stripId=StringUtils.leftPad(String.valueOf(strip.getPrepaidStripId().longValue()),4,"0");
                String strRowData = org.getOrganisationName()
                        .concat(",")
                        .concat(StringUtils.leftPad(strip.getPIN(), 16, "0"))
                        .concat(",")
                        .concat(String.valueOf(stripId).substring(stripId.length() - 4))
                        .concat(",")
                        .concat(String.format("%020d", strip.getPrepaidStripId().longValue()))
                        .concat(",")
                        .concat(String.format("%09d",strip.getValueCents().divide(new BigDecimal(100)).longValue())) 
                        .concat(",")
                        .concat(sdf.format(strip.getExpiryDate()));
                        
                        if(strip.getUnitCreditSpecificationId() > 0) {
                            strRowData = strRowData.concat(",")
                                    .concat(bundles.get(strip.getUnitCreditSpecificationId()));
                        }
                writer.write(strRowData);
                writer.newLine();
            }

            writer.flush();
            writer.close();
            
            String pgpEncryptedFileLocation = System.getProperty("java.io.tmpdir") + File.separator + "/VOUCHER_PINS_" + saleId + ".pgp";
            log.debug("Wrote [{}] voucher strips for sale id [{}] to spreadsheet file [{}].", strips.size(), saleId, fileLocation);

        //Load Public Key File
        ByteArrayInputStream pubKeyIS = new ByteArrayInputStream(Utils.decodeBase64(pubKey.getData()));
             

        //Output file
        FileOutputStream encryptedFOS = new FileOutputStream(pgpEncryptedFileLocation);
       
        try {

            //Other settings
            boolean armor = false;
            boolean integrityCheck = false;
            PGPFileUtils.encryptFile(encryptedFOS, fileLocation, pubKeyIS, armor, integrityCheck);   

            CustomerCommunicationData email = new CustomerCommunicationData();
            email.setAttachmentBase64(Utils.readFileAsBase64String(pgpEncryptedFileLocation));
            email.setSubjectResourceName("voucher.pins.email.subject");
            email.setBodyResourceName("voucher.pins.email.body");
            email.getBodyParameters().add(String.valueOf(saleId));
            email.getSubjectParameters().add(String.valueOf(saleId));
            String attachmentPrefix = LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "voucher.pins.filename.prefix");
            email.setAttachmentFileName(attachmentPrefix + saleId + ".txt.pgp");
            email.setCustomerId(recipientCustId);
            email.setBCCAddress(BaseUtils.getProperty("env.sales.invoice.bcc.email.address"));
            email.setBlocking(false);
            SCAWrapper.getAdminInstance().sendCustomerCommunication(email);
            log.debug("Sent a voucher PINs file for invoice" + saleId);
        } catch (Exception e) {
            log.warn("Error sending voucher PINs file", e);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "PVS", "Error sending voucher PINs file: " + e.toString());
        }           
            
            
        } catch(Exception ex) {
            log.error("Error while exporting vouchers for sale id [{}] into spreadsheet.", saleId);
            throw ex;
        }
         
    }
    
    private int generateCSVforX3(PrepaidStrips psList, int lastStripIDGenerated, String location) throws IOException {
        log.debug("Forming the CSV file with file location " + location + " lastStripIDGenerated:"+lastStripIDGenerated+" & count:"+psList.getPrepaidStrips().size());
        BufferedWriter writer = new BufferedWriter(new FileWriter(location));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Map<Integer, String> bundles = DAO.getUnitCreditNames(em);

        CSVPrinter cp = new CSVPrinter(writer, CSVFormat.DEFAULT);

        cp.printRecord("|E|", "19", "||", "|SCN|", sdf.format(new Date()), "|Voucher creation|");

        int lcount = 1000;
        int newLastStripId = 0;
        String channel=BaseUtils.getProperty("env.pvs.default.channel", "12PTBA01");

        for (com.smilecoms.xml.schema.pvs.PrepaidStrip strip : psList.getPrepaidStrips()) {
            try {
                String prefix = strip.getUnitCreditSpecificationId() > 0 ? "|VOUB" : "|VOUA";
                String code = "";
                String description = "";
                String value = String.valueOf((int) strip.getValueInCents() / 100);

                if (prefix.equalsIgnoreCase("|VOUB")) {
                    code = prefix + strip.getUnitCreditSpecificationId() + "|";
                    description = bundles.get(strip.getUnitCreditSpecificationId()).trim();
                } else {
                    code = prefix + value + "|";
                    description = "Airtime of Value N" + value;
                }
                
                if (((strip.getPrepaidStripId() - (lastStripIDGenerated + 2)) % 20 == 0)) {
                    newLastStripId = strip.getPrepaidStripId() + 18;
                    cp.printRecord("|L|", lcount, code, description, "|UN|", "1", "|UN|", "1", "0");
                    cp.printRecord("|S|", "|UN|", "1", "||", "||", "||", "|"+channel+"|", "|A|", strip.getPrepaidStripId() + "-" + newLastStripId);
                    lcount += 1000;
                }
            } catch (IOException ex) {
                log.error("Exception while forming the CSV file: " + ex);
            }
        };

        cp.flush();
        cp.close();
        writer.close();
        
      return newLastStripId;
    }
        
    public boolean sendCSVToX3(String location, int startId, int endId) throws Exception {
        FTPClient client = null;
        InputStream is = null;
        boolean isStored = false;
        
        try {
            client = getClient();
            is = new FileInputStream(new File(location));
            client.changeWorkingDirectory("IMPORTS/NEW");
            client.setFileType(FTP.BINARY_FILE_TYPE); // File Type needs to be set when uploading files.       
            isStored = client.storeFile("voucher_"+startId+"_"+endId+".csv", is);
            if (isStored) {
                try {
                    log.debug("CSV succesfully sent to X3 with Starting ID:"+startId+" and Ending ID:"+endId);
                } catch (Exception ex) {
                    log.warn("Error occured while sending CSV to X3 with Starting ID:"+startId+" and Ending ID:"+endId+" Error is:"+ ex);
                }
            }
        } finally {
            if (is != null) {
                is.close();
            }
            closeClient(client);
        }
        
        return isStored;
    } 
    
    public FTPClient getClient() throws Exception {
        log.debug("Connecting to FTP Server to send strips to X3");
        props = new Properties();
        props.load(BaseUtils.getPropertyAsStream("env.pvs.ftp.server.config"));
        FTPClient fc = new FTPClient();
        fc.setDefaultTimeout(30000); // ms
        fc.setDataTimeout(30000); // ms
        fc.connect(props.getProperty("hostIP"),Integer.parseInt(props.getProperty("hostPort")));
        boolean login = fc.login(props.getProperty("username"), props.getProperty("password"));
        fc.enterLocalPassiveMode();
        return fc;
    }

    public void closeClient(FTPClient client) {
        if (client != null) {
            try {
                client.logout();
                client.disconnect();
            } catch (Exception ex) {
                log.warn("Error disconnecting FTP client: ", ex);
            }
        }
    }
        
}
