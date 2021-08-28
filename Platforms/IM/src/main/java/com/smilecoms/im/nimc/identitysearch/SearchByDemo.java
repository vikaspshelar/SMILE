
package com.smilecoms.im.nimc.identitysearch;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for searchByDemo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="searchByDemo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="token" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="DemoDataMandatory" type="{http://IdentitySearch.nimc/}demoDataMandatory" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "searchByDemo", propOrder = {
    "token",
    "demoDataMandatory"
})
public class SearchByDemo {

    protected String token;
    @XmlElement(name = "DemoDataMandatory")
    protected DemoDataMandatory demoDataMandatory;

    /**
     * Gets the value of the token property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the value of the token property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToken(String value) {
        this.token = value;
    }

    /**
     * Gets the value of the demoDataMandatory property.
     * 
     * @return
     *     possible object is
     *     {@link DemoDataMandatory }
     *     
     */
    public DemoDataMandatory getDemoDataMandatory() {
        return demoDataMandatory;
    }

    /**
     * Sets the value of the demoDataMandatory property.
     * 
     * @param value
     *     allowed object is
     *     {@link DemoDataMandatory }
     *     
     */
    public void setDemoDataMandatory(DemoDataMandatory value) {
        this.demoDataMandatory = value;
    }

}
