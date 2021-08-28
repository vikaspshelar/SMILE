/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.sca.Errors;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.UnitCreditInstance;
import com.smilecoms.commons.sca.UnitCreditInstanceQuery;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.bind.annotation.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lesiba
 */
public final class UnitCreditBean extends BaseBean {

    private static final Logger log = LoggerFactory.getLogger(UnitCreditBean.class);
    private UnitCreditInstance scaUnitCreditInstance;
    private UnitCreditSpecification scaUnitCreditSpecification;

    public UnitCreditBean() {
    }

    public static UnitCreditBean getUnitCreditById(int ucid) {
        log.debug("Getting Unit Credit Instance by Id [{}]", ucid);
        return new UnitCreditBean(ucid);
    }

    private UnitCreditBean(int ucid) {
        UnitCreditInstanceQuery q = new UnitCreditInstanceQuery();
        q.setUnitCreditInstanceId(ucid);
        List<UnitCreditInstance> l = SCAWrapper.getUserSpecificInstance().getUnitCreditInstances(q).getUnitCreditInstances();
        if (l.isEmpty()) {
            SCABusinessError b = new SCABusinessError();
            b.setErrorCode(Errors.ERROR_CODE_SCA_NO_RESULT);
            b.setErrorDesc("No results found");
            throw b;
        }

        this.scaUnitCreditInstance = l.get(0);
        this.scaUnitCreditSpecification = NonUserSpecificCachedDataHelper.getUnitCreditSpecification(getUnitCreditSpecificationId());
    }

    static List<UnitCreditBean> wrap(List<UnitCreditInstance> unitCreditInstances) {
        List<UnitCreditBean> unitCredits = new ArrayList<>();
        for (UnitCreditInstance unitCreditInstance : unitCreditInstances) {
            unitCredits.add(wrap(unitCreditInstance));
        }
        return unitCredits;
    }

    private static UnitCreditBean wrap(UnitCreditInstance unitCreditInstance) {
        return new UnitCreditBean(unitCreditInstance);
    }

    private UnitCreditBean(UnitCreditInstance scaUnitCreditInstance) {
        this.scaUnitCreditInstance = scaUnitCreditInstance;
        this.scaUnitCreditSpecification = NonUserSpecificCachedDataHelper.getUnitCreditSpecification(getUnitCreditSpecificationId());
    }

    public static String getPropertyFromConfig(UnitCreditSpecification ucs, String propName) {

        Map<String, String> config = new HashMap<>();
        StringTokenizer stValues = new StringTokenizer(ucs.getConfiguration(), "\r\n");
        while (stValues.hasMoreTokens()) {
            String row = stValues.nextToken();
            if (!row.isEmpty()) {
                String[] bits = row.split("=");
                String name = bits[0].trim();
                String value;
                if (bits.length == 1) {
                    value = " ";
                } else {
                    value = bits[1].trim();
                }
                config.put(name, value);
            }
        }

        return config.get(propName);
    }

    public String getConfigProperty(String propName) {
        String value = getPropertyFromConfig(scaUnitCreditSpecification, propName);
        return value != null ? value : " ";
    }

    @XmlElement
    public String getUnitCreditDescription() {
        return scaUnitCreditSpecification.getDescription();
    }

    @XmlElement
    public String getUnitType() {
        return scaUnitCreditSpecification.getUnitType();
    }

    @XmlElement
    public int getUnitCreditInstanceId() {
        return scaUnitCreditInstance.getUnitCreditInstanceId();
    }

    @XmlElement
    public String getName() {
        return scaUnitCreditInstance.getName();
    }

    @XmlElement
    public int getUnitCreditSpecificationId() {
        return scaUnitCreditInstance.getUnitCreditSpecificationId();
    }

    @XmlElement
    public long getAccountId() {
        return scaUnitCreditInstance.getAccountId();
    }

    @XmlElement
    public long getPurchaseDate() {
        return Utils.getJavaDate(scaUnitCreditInstance.getPurchaseDate()).getTime();
    }

    @XmlElement
    public long getStartDate() {
        return Utils.getJavaDate(scaUnitCreditInstance.getStartDate()).getTime();
    }

    @XmlElement
    public long getExpiryDate() {
        return Utils.getJavaDate(scaUnitCreditInstance.getExpiryDate()).getTime();
    }

    @XmlElement
    public long getEndDate() {
        return Utils.getJavaDate(scaUnitCreditInstance.getEndDate()).getTime();
    }

    @XmlElement
    public double getCurrentUnitsRemaining() {
        return scaUnitCreditInstance.getCurrentUnitsRemaining();
    }

    @XmlElement
    public double getAvailableUnitsRemaining() {
        return scaUnitCreditInstance.getAvailableUnitsRemaining();
    }

    @XmlElement
    public int getProductInstanceId() {
        return scaUnitCreditInstance.getProductInstanceId();
    }

    @XmlElement
    public double getUnitsAtStart() {
        return scaUnitCreditInstance.getUnitsAtStart();
    }

    @XmlElement
    public String getExtTxId() {
        return scaUnitCreditInstance.getExtTxId();
    }

    @XmlElement
    public String getInfo() {
        return scaUnitCreditInstance.getInfo();
    }

    @XmlElement
    public int getSaleLineId() {
        return scaUnitCreditInstance.getSaleLineId();
    }

    @XmlElement
    public double getPriceInCents() {
        return scaUnitCreditSpecification.getPriceInCents();
    }

    @XmlElement
    public String getFirstSubMessage() {
        return getConfigProperty("FirstMessage");
    }

    @XmlElement
    public String getSecondSubMessage() {
        return getConfigProperty("SecondMessage");
    }

}
