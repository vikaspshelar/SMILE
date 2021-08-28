package com.smilecoms.rra.service;

import com.smilecoms.rra.dao.TCRADao;
import com.smilecoms.rra.model.RegistrationImageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author abhilash
 */

@Service
public class TCRADataService {
    
    private static final Logger log = LoggerFactory.getLogger(TCRADataService.class);
    
    @Autowired
    private TCRADao tcraDao;

    public RegistrationImageResponse getImageData(String iccid, String msisdn)
    {
        RegistrationImageResponse rir = new RegistrationImageResponse();
        
        boolean iccidFlag = (!iccid.equals("") || !iccid.isEmpty());
        boolean msisdnFlag = (!msisdn.equals("") || !msisdn.isEmpty());
        
        if(iccidFlag && msisdnFlag)
        {
            log.debug("Both ICCID and MSISDN are present");
            String custIdIccid = tcraDao.getCustomerId(iccid,"ICCID");
            String custIdMsisdn = tcraDao.getCustomerId(msisdn,"MSISDN");
            log.info("custIdIccid: "+custIdIccid+" custIdMsisdn:"+custIdMsisdn);
            //custIdMsisdn=custIdIccid;
            if(custIdIccid.equalsIgnoreCase(custIdMsisdn))
            {
                rir = tcraDao.getImages(custIdIccid,iccid,msisdn);
            }
            else
            {
                log.error("ICCID and MSISDN belongs to different Customers. Recheck the Request");
                rir.setIccid(iccid);
                rir.setMsisdn(msisdn);
                rir.setRegistrationImages(null);
            }
        }
        else if(iccidFlag && !msisdnFlag)
        {
            log.debug("Only ICCID is present in request: "+iccid);
            String custIdIccid = tcraDao.getCustomerId(iccid,"ICCID");
            if(!custIdIccid.isEmpty() && !custIdIccid.equalsIgnoreCase(""))
            {
                rir = tcraDao.getImages(custIdIccid, iccid, msisdn);
            }
            else
            {
                log.debug("Customer ID is empty for iccid "+iccid);
                rir.setIccid(iccid);
            }
        }
        else if(msisdnFlag && !iccidFlag)
        {
            log.debug("Only MSISDN is present in request: "+msisdn);
            String custIdMsisdn = tcraDao.getCustomerId(msisdn, "MSISDN");
            if(!custIdMsisdn.isEmpty() && !custIdMsisdn.equalsIgnoreCase(""))
            {
                rir = tcraDao.getImages(custIdMsisdn, iccid, msisdn);
            }
            else
            {
                log.debug("Customer ID is empty for msisdn "+msisdn);
                rir.setMsisdn(msisdn);
            }
        }        
        return rir;
    }
    
}
