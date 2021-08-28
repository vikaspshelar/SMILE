/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.sca.Address;
import com.smilecoms.commons.sca.CustomerRole;
import com.smilecoms.commons.sca.OrganisationQuery;
import com.smilecoms.commons.sca.Photograph;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StOrganisationLookupVerbosity;
import com.smilecoms.commons.util.Utils;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public final class OrganisationBean extends BaseBean {

    private static final Logger log = LoggerFactory.getLogger(OrganisationBean.class);

    private com.smilecoms.commons.sca.Organisation scaOrganisation;

    public OrganisationBean() {
    }

    public static OrganisationBean getOrganisationById(int organisationId) {
        log.debug("Getting Organisation by Id [{}]", organisationId);
        OrganisationQuery query = new OrganisationQuery();
        query.setOrganisationId(organisationId);
        query.setVerbosity(StOrganisationLookupVerbosity.MAIN);
        return new OrganisationBean(SCAWrapper.getUserSpecificInstance().getOrganisation(query));
    }

    private OrganisationBean(com.smilecoms.commons.sca.Organisation scaOrganisation) {
        this.scaOrganisation = scaOrganisation;
    }

    @XmlElement
    public String getOrganisationName() {
        return scaOrganisation.getOrganisationName();
    }

    @XmlElement
    public int getOrganisationId() {
        return scaOrganisation.getOrganisationId();
    }

    @XmlElement
    public String getOrganisationType() {
        return scaOrganisation.getOrganisationType();
    }

    @XmlElement
    public String getOrganisationStatus() {
        return scaOrganisation.getOrganisationStatus();
    }

    @XmlElement
    public long getCreatedDateTime() {
        return Utils.getJavaDate(scaOrganisation.getCreatedDateTime()).getTime();
    }

    @XmlElement
    public int getVersion() {
        return scaOrganisation.getVersion();
    }

    @XmlElement
    public List<Address> getAddresses() {
        return scaOrganisation.getAddresses();
    }

    @XmlElement
    public String getEmailAddress() {
        return scaOrganisation.getEmailAddress();
    }

    @XmlElement
    public String getAlternativeContact1() {
        return scaOrganisation.getAlternativeContact1();
    }

    @XmlElement
    public String getAlternativeContact2() {
        return scaOrganisation.getAlternativeContact2();
    }

    @XmlElement
    public String getIndustry() {
        return scaOrganisation.getIndustry();
    }

    @XmlElement
    public String getSize() {
        return scaOrganisation.getSize();
    }

    @XmlElement
    public String getTaxNumber() {
        return scaOrganisation.getTaxNumber();
    }

    @XmlElement
    public String getCompanyNumber() {
        return scaOrganisation.getCompanyNumber();
    }

    @XmlElement
    public String getCreditAccountNumber() {
        return scaOrganisation.getCreditAccountNumber();
    }

    @XmlElement
    public String getChannelCode() {
        return scaOrganisation.getChannelCode();
    }

    @XmlElement
    public List<String> getModificationRoles() {
        return scaOrganisation.getModificationRoles();
    }

    @XmlElement
    public List<Photograph> getOrganisationPhotographs() {
        return scaOrganisation.getOrganisationPhotographs();
    }

    @XmlElement
    public int getAccountManagerCustomerProfileId() {
        return scaOrganisation.getAccountManagerCustomerProfileId();
    }

    @XmlElement
    public List<CustomerRole> getCustomerRoles() {
        return scaOrganisation.getCustomerRoles();
    }

    @XmlElement
    public int getCreatedByCustomerProfileId() {
        return scaOrganisation.getCreatedByCustomerProfileId();
    }

}
