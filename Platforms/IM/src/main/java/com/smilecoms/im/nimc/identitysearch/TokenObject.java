
package com.smilecoms.im.nimc.identitysearch;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tokenObject complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tokenObject"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="loginObject" type="{http://IdentitySearch.nimc/}loginObject" minOccurs="0"/&gt;
 *         &lt;element name="loginString" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tokenObject", propOrder = {
    "loginObject",
    "loginString"
})
public class TokenObject {

    protected LoginObject loginObject;
    protected String loginString;

    /**
     * Gets the value of the loginObject property.
     * 
     * @return
     *     possible object is
     *     {@link LoginObject }
     *     
     */
    public LoginObject getLoginObject() {
        return loginObject;
    }

    /**
     * Sets the value of the loginObject property.
     * 
     * @param value
     *     allowed object is
     *     {@link LoginObject }
     *     
     */
    public void setLoginObject(LoginObject value) {
        this.loginObject = value;
    }

    /**
     * Gets the value of the loginString property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLoginString() {
        return loginString;
    }

    /**
     * Sets the value of the loginString property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLoginString(String value) {
        this.loginString = value;
    }

}
