/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceOrder;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.bind.annotation.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lesiba
 */
public final class ServiceBean extends BaseBean {

    private static final Logger log = LoggerFactory.getLogger(ServiceBean.class);
    private ServiceInstance scaServiceInstance;
    private ProductBean product;

    public ServiceBean() {
    }

    static List<ServiceBean> wrap(List<ServiceInstance> serviceInstances) {
        List<ServiceBean> services = new ArrayList<>();
        for (ServiceInstance serviceInstance : serviceInstances) {
            services.add(new ServiceBean(serviceInstance));
        }
        return services;
    }

    public static ServiceBean getServiceInstanceById(int serviceInstanceId) {
        return new ServiceBean(SCAWrapper.getUserSpecificInstance().getServiceInstance(serviceInstanceId, StServiceInstanceLookupVerbosity.MAIN_SVCAVP));
    }

    public static ServiceBean getServiceInstanceByIMPU(String impu) {
        log.debug("Getting SI by IMPU [{}]", impu);
        ServiceInstanceQuery siq = new ServiceInstanceQuery();
        siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
        siq.setIdentifierType("END_USER_SIP_URI");
        siq.setIdentifier(Utils.getPublicIdentityForPhoneNumber(impu));
        ServiceInstanceList sil = SCAWrapper.getUserSpecificInstance().getServiceInstances(siq);
        checkForNoResult(sil.getServiceInstances());
        return new ServiceBean(sil.getServiceInstances().get(0));
    }

    public static ServiceBean changeAttributes(int serviceInstanceId, Map<String, String> attributes, boolean onlyUserDefinedAllowed) {
        log.debug("changeUserDefinedAttributes on [{}] with  [{}]", serviceInstanceId, attributes);
        ServiceInstance si = UserSpecificCachedDataHelper.getServiceInstance(serviceInstanceId, StServiceInstanceLookupVerbosity.MAIN);
        ProductOrder po = new ProductOrder();
        po.setProductInstanceId(si.getProductInstanceId());

        ServiceInstanceOrder sio = new ServiceInstanceOrder();
        sio.setServiceInstance(si);
        sio.setAction(StAction.UPDATE);
        po.setAction(StAction.NONE);
        po.getServiceInstanceOrders().add(sio);
        si.getAVPs().clear();
        
        for (Entry<String, String> entry : attributes.entrySet()) {
            if (!entry.getKey().startsWith("UserDefined") && onlyUserDefinedAllowed) {
                log.debug("Not changing attribute [{}] to [{}]", entry.getKey(), entry.getValue());
                continue;
            }
            AVP avp = new AVP();
            avp.setAttribute(entry.getKey());
            avp.setValue(entry.getValue());
            si.getAVPs().add(avp);
        }

        SCAWrapper.getUserSpecificInstance().processOrder(po);

        return new ServiceBean(UserSpecificCachedDataHelper.getServiceInstance(serviceInstanceId, StServiceInstanceLookupVerbosity.MAIN_SVCAVP));

    }

    public ServiceBean(ServiceInstance serviceInstance) {
        this.scaServiceInstance = serviceInstance;
        this.product = ProductBean.getProductInstanceById(getProductInstanceId());
    }

    @XmlElement
    public String getServiceName() {
        return NonUserSpecificCachedDataHelper.getServiceSpecification(getServiceSpecificationId()).getName();
    }

    @XmlElement
    public String getServiceDescription() {
        return NonUserSpecificCachedDataHelper.getServiceSpecification(getServiceSpecificationId()).getDescription();
    }

    @XmlElement
    public String getProductName() {
        return product.getProductName();
    }

    @XmlElement
    public String getProductDescription() {
        return product.getProductDescription();
    }

    @XmlElement
    public String getProductFriendlyName() {
        return product.getFriendlyName();
    }

    @XmlElement
    public int getProductOrganisationId() {
        return product.getOrganisationId();
    }

    @XmlElement
    public int getProductCustomerId() {
        return product.getCustomerId();
    }

    @XmlElement
    public int getServiceInstanceId() {
        return scaServiceInstance.getServiceInstanceId();
    }

    @XmlElement
    public String getProductSegment() {
        return product.getSegment();
    }

    @XmlElement
    public String getProductPromotionCode() {
        return product.getPromotionCode();
    }

    @XmlElement
    public String getPhoneNumber() {
        return UserSpecificCachedDataHelper.getServiceInstancePhoneNumber(getServiceInstanceId());
    }

    @XmlElement
    public String getServiceUser() {
        return UserSpecificCachedDataHelper.getServiceInstanceUserName(getServiceInstanceId());
    }

    @XmlElement
    public String getServiceICCID() {
        return UserSpecificCachedDataHelper.getServiceInstanceICCID(getServiceInstanceId());
    }

    @XmlElement
    public int getServiceSpecificationId() {
        return scaServiceInstance.getServiceSpecificationId();
    }

    @XmlElement
    public int getProductInstanceId() {
        return scaServiceInstance.getProductInstanceId();
    }

    @XmlElement
    public int getCustomerId() {
        return scaServiceInstance.getCustomerId();
    }

    @XmlElement
    public long getAccountId() {
        return scaServiceInstance.getAccountId();
    }

    @XmlElement
    public String getStatus() {
        return scaServiceInstance.getStatus();
    }

    @XmlElement
    public long getCreatedDateTime() {
        return Utils.getJavaDate(scaServiceInstance.getCreatedDateTime()).getTime();
    }

    @XmlElement
    public Integer getRatePlanId() {
        return scaServiceInstance.getRatePlanId();
    }

    @XmlElement
    public List<AVP> getAVPs() {
        return scaServiceInstance.getAVPs();
    }

}
