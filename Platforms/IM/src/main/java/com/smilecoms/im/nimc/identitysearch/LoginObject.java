
package com.smilecoms.im.nimc.identitysearch;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for loginObject complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="loginObject"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="tokenStringx" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="timestamp" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *         &lt;element name="token" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="lMessage" type="{http://IdentitySearch.nimc/}loginMessage" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "loginObject", propOrder = {
    "tokenStringx",
    "timestamp",
    "token",
    "lMessage"
})
public class LoginObject {

    protected String tokenStringx;
    protected long timestamp;
    protected String token;
    protected LoginMessage lMessage;

    /**
     * Gets the value of the tokenStringx property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTokenStringx() {
        return tokenStringx;
    }

    /**
     * Sets the value of the tokenStringx property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTokenStringx(String value) {
        this.tokenStringx = value;
    }

    /**
     * Gets the value of the timestamp property.
     * 
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the value of the timestamp property.
     * 
     */
    public void setTimestamp(long value) {
        this.timestamp = value;
    }

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
     * Gets the value of the lMessage property.
     * 
     * @return
     *     possible object is
     *     {@link LoginMessage }
     *     
     */
    public LoginMessage getLMessage() {
        return lMessage;
    }

    /**
     * Sets the value of the lMessage property.
     * 
     * @param value
     *     allowed object is
     *     {@link LoginMessage }
     *     
     */
    public void setLMessage(LoginMessage value) {
        this.lMessage = value;
    }

}
