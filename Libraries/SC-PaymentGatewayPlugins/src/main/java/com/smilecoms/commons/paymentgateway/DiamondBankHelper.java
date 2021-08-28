/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.paymentgateway;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.paymentgateway.diamondbank.ITransactionStatusCheck;
import com.smilecoms.commons.paymentgateway.diamondbank.TransactionStatusCheck;
import com.smilecoms.commons.util.Utils;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class DiamondBankHelper {
    
    private static ITransactionStatusCheck cachedDiamondBankProxy = null;
    private static String proxyConfig = null;
    private static final Logger log = LoggerFactory.getLogger(DiamondBankHelper.class);
    
    public static ITransactionStatusCheck getDiamondBankProxy() {
        String proxyConfigNow = BaseUtils.getProperty("env.pgw.diamond.config");
        if (proxyConfig != null && !proxyConfigNow.equals(proxyConfig)) {
            cachedDiamondBankProxy = null;
        }
        if (cachedDiamondBankProxy != null) {
            return cachedDiamondBankProxy;
        }
        proxyConfig = proxyConfigNow;
        String targetNamespace = Utils.getValueFromCRDelimitedAVPString(proxyConfig, "namespace");//the targetNamespace attribute in the WSDL's <wsdl:definitions> element.
        String serviceName = Utils.getValueFromCRDelimitedAVPString(proxyConfig, "name");//the name attribute in the WSDL's <wsdl:definitions> element.
        String endpointAddress = Utils.getValueFromCRDelimitedAVPString(proxyConfig, "location");//the location attribute in the WSDL's <soap:address> element

        log.debug("Going to create soap client to query transaction  webservice config: namespace[{}], name[{}], location[{}]", new Object[]{targetNamespace, serviceName, endpointAddress});

        URL url = DiamondBankHelper.class.getResource("/TransactionStatusCheck.svc.wsdl");
        TransactionStatusCheck diamondBankService = new TransactionStatusCheck(url, new QName(targetNamespace, serviceName));
        cachedDiamondBankProxy = diamondBankService.getBasicHttpBindingITransactionStatusCheck();

        ((BindingProvider) cachedDiamondBankProxy).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
        ((BindingProvider) cachedDiamondBankProxy).getRequestContext().put("javax.xml.ws.client.connectionTimeout", 5000);
        ((BindingProvider) cachedDiamondBankProxy).getRequestContext().put("javax.xml.ws.client.receiveTimeout", 50000);
        return cachedDiamondBankProxy;
    }
    
}
