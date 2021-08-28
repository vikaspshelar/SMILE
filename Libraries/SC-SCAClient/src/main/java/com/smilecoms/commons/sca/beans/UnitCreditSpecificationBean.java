/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.sca.ProductServiceMapping;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author paul
 */
public class UnitCreditSpecificationBean extends BaseBean {
    private UnitCreditSpecification scaUnitCreditSpecification;

    public UnitCreditSpecificationBean() {
    }
    
    public static List<UnitCreditSpecificationBean> wrap(List<UnitCreditSpecification> scaUnitCreditSpecifications) {
        List<UnitCreditSpecificationBean> beans = new ArrayList();
        for (UnitCreditSpecification spec : scaUnitCreditSpecifications) {
            beans.add(new UnitCreditSpecificationBean(spec));
        }
        return beans;
    }
    
    public UnitCreditSpecificationBean(UnitCreditSpecification scaUnitCreditSpecification) {
        this.scaUnitCreditSpecification = scaUnitCreditSpecification;
    }

    @XmlElement
    public int getUnitCreditSpecificationId() {
        return scaUnitCreditSpecification.getUnitCreditSpecificationId();
    }

    @XmlElement
    public String getName() {
        return scaUnitCreditSpecification.getName();
    }

    @XmlElement
    public List<ProductServiceMapping> getProductServiceMappings() {
        return scaUnitCreditSpecification.getProductServiceMappings();
    }

    @XmlElement
    public double getPriceInCents() {
        return scaUnitCreditSpecification.getPriceInCents();
    }

    @XmlElement
    public int getValidityDays() {
        return scaUnitCreditSpecification.getValidityDays();
    }
    
    @XmlElement
    public int getUsableDays() {
        return scaUnitCreditSpecification.getUsableDays();
    }

    @XmlElement
    public long getAvailableFrom() {
        return Utils.getJavaDate(scaUnitCreditSpecification.getAvailableFrom()).getTime();
    }

    @XmlElement
    public long getAvailableTo() {
        return Utils.getJavaDate(scaUnitCreditSpecification.getAvailableTo()).getTime();
    }

    @XmlElement
    public double getUnits() {
        return scaUnitCreditSpecification.getUnits();
    }

    @XmlElement
    public String getUnitType() {
        return scaUnitCreditSpecification.getUnitType();
    }

    @XmlElement
    public String getDescription() {
        return scaUnitCreditSpecification.getDescription();
    }

    @XmlElement
    public String getPurchaseRoles() {
        return scaUnitCreditSpecification.getPurchaseRoles();
    }

    @XmlElement
    public int getPriority() {
        return scaUnitCreditSpecification.getPriority();
    }

    @XmlElement
    public String getFilterClass() {
        return scaUnitCreditSpecification.getFilterClass();
    }

    @XmlElement
    public String getWrapperClass() {
        return scaUnitCreditSpecification.getWrapperClass();
    }

    @XmlElement
    public String getConfiguration() {
        return scaUnitCreditSpecification.getConfiguration();
    }
    
    @XmlElement
    public String getItemNumber() {
        return scaUnitCreditSpecification.getItemNumber();
    }
    
}
