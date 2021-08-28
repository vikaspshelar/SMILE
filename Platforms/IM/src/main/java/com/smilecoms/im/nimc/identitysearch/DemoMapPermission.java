
package com.smilecoms.im.nimc.identitysearch;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for demoMapPermission complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="demoMapPermission"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="atype" type="{http://IdentitySearch.nimc/}accessType" minOccurs="0"/&gt;
 *         &lt;element name="batchid" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="birthcountry" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="birthdate" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="birthlga" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="birthstate" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="cardstatus" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="documentno" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="educationallevel" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="email" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="emplymentstatus" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="gender" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="heigth" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="maritalstatus" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="message" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="names" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="nin" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="nok_address" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="nok_names" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="nspokenlang" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="ospokenlang" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="photo" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="pnames" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="profession" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="religion" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="residence_address" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="residencestatus" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="self_origin_lga" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="self_origin_place" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="self_origin_state" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="signature" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="telephoneno" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="title" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="userID" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "demoMapPermission", propOrder = {
    "atype",
    "batchid",
    "birthcountry",
    "birthdate",
    "birthlga",
    "birthstate",
    "cardstatus",
    "documentno",
    "educationallevel",
    "email",
    "emplymentstatus",
    "gender",
    "heigth",
    "maritalstatus",
    "message",
    "names",
    "nin",
    "nokAddress",
    "nokNames",
    "nspokenlang",
    "ospokenlang",
    "photo",
    "pnames",
    "profession",
    "religion",
    "residenceAddress",
    "residencestatus",
    "selfOriginLga",
    "selfOriginPlace",
    "selfOriginState",
    "signature",
    "telephoneno",
    "title",
    "userID"
})
public class DemoMapPermission {

    protected AccessType atype;
    protected Boolean batchid;
    protected Boolean birthcountry;
    protected Boolean birthdate;
    protected Boolean birthlga;
    protected Boolean birthstate;
    protected Boolean cardstatus;
    protected Boolean documentno;
    protected Boolean educationallevel;
    protected Boolean email;
    protected Boolean emplymentstatus;
    protected Boolean gender;
    protected Boolean heigth;
    protected Boolean maritalstatus;
    protected String message;
    protected Boolean names;
    protected Boolean nin;
    @XmlElement(name = "nok_address")
    protected Boolean nokAddress;
    @XmlElement(name = "nok_names")
    protected Boolean nokNames;
    protected Boolean nspokenlang;
    protected Boolean ospokenlang;
    protected Boolean photo;
    protected Boolean pnames;
    protected Boolean profession;
    protected Boolean religion;
    @XmlElement(name = "residence_address")
    protected Boolean residenceAddress;
    protected Boolean residencestatus;
    @XmlElement(name = "self_origin_lga")
    protected Boolean selfOriginLga;
    @XmlElement(name = "self_origin_place")
    protected Boolean selfOriginPlace;
    @XmlElement(name = "self_origin_state")
    protected Boolean selfOriginState;
    protected Boolean signature;
    protected Boolean telephoneno;
    protected Boolean title;
    protected Boolean userID;

    /**
     * Gets the value of the atype property.
     * 
     * @return
     *     possible object is
     *     {@link AccessType }
     *     
     */
    public AccessType getAtype() {
        return atype;
    }

    /**
     * Sets the value of the atype property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccessType }
     *     
     */
    public void setAtype(AccessType value) {
        this.atype = value;
    }

    /**
     * Gets the value of the batchid property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBatchid() {
        return batchid;
    }

    /**
     * Sets the value of the batchid property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBatchid(Boolean value) {
        this.batchid = value;
    }

    /**
     * Gets the value of the birthcountry property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBirthcountry() {
        return birthcountry;
    }

    /**
     * Sets the value of the birthcountry property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBirthcountry(Boolean value) {
        this.birthcountry = value;
    }

    /**
     * Gets the value of the birthdate property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBirthdate() {
        return birthdate;
    }

    /**
     * Sets the value of the birthdate property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBirthdate(Boolean value) {
        this.birthdate = value;
    }

    /**
     * Gets the value of the birthlga property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBirthlga() {
        return birthlga;
    }

    /**
     * Sets the value of the birthlga property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBirthlga(Boolean value) {
        this.birthlga = value;
    }

    /**
     * Gets the value of the birthstate property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBirthstate() {
        return birthstate;
    }

    /**
     * Sets the value of the birthstate property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBirthstate(Boolean value) {
        this.birthstate = value;
    }

    /**
     * Gets the value of the cardstatus property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isCardstatus() {
        return cardstatus;
    }

    /**
     * Sets the value of the cardstatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setCardstatus(Boolean value) {
        this.cardstatus = value;
    }

    /**
     * Gets the value of the documentno property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDocumentno() {
        return documentno;
    }

    /**
     * Sets the value of the documentno property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDocumentno(Boolean value) {
        this.documentno = value;
    }

    /**
     * Gets the value of the educationallevel property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isEducationallevel() {
        return educationallevel;
    }

    /**
     * Sets the value of the educationallevel property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEducationallevel(Boolean value) {
        this.educationallevel = value;
    }

    /**
     * Gets the value of the email property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isEmail() {
        return email;
    }

    /**
     * Sets the value of the email property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEmail(Boolean value) {
        this.email = value;
    }

    /**
     * Gets the value of the emplymentstatus property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isEmplymentstatus() {
        return emplymentstatus;
    }

    /**
     * Sets the value of the emplymentstatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEmplymentstatus(Boolean value) {
        this.emplymentstatus = value;
    }

    /**
     * Gets the value of the gender property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isGender() {
        return gender;
    }

    /**
     * Sets the value of the gender property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setGender(Boolean value) {
        this.gender = value;
    }

    /**
     * Gets the value of the heigth property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isHeigth() {
        return heigth;
    }

    /**
     * Sets the value of the heigth property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setHeigth(Boolean value) {
        this.heigth = value;
    }

    /**
     * Gets the value of the maritalstatus property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isMaritalstatus() {
        return maritalstatus;
    }

    /**
     * Sets the value of the maritalstatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setMaritalstatus(Boolean value) {
        this.maritalstatus = value;
    }

    /**
     * Gets the value of the message property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the value of the message property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMessage(String value) {
        this.message = value;
    }

    /**
     * Gets the value of the names property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isNames() {
        return names;
    }

    /**
     * Sets the value of the names property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNames(Boolean value) {
        this.names = value;
    }

    /**
     * Gets the value of the nin property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isNin() {
        return nin;
    }

    /**
     * Sets the value of the nin property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNin(Boolean value) {
        this.nin = value;
    }

    /**
     * Gets the value of the nokAddress property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isNokAddress() {
        return nokAddress;
    }

    /**
     * Sets the value of the nokAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNokAddress(Boolean value) {
        this.nokAddress = value;
    }

    /**
     * Gets the value of the nokNames property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isNokNames() {
        return nokNames;
    }

    /**
     * Sets the value of the nokNames property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNokNames(Boolean value) {
        this.nokNames = value;
    }

    /**
     * Gets the value of the nspokenlang property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isNspokenlang() {
        return nspokenlang;
    }

    /**
     * Sets the value of the nspokenlang property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNspokenlang(Boolean value) {
        this.nspokenlang = value;
    }

    /**
     * Gets the value of the ospokenlang property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isOspokenlang() {
        return ospokenlang;
    }

    /**
     * Sets the value of the ospokenlang property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setOspokenlang(Boolean value) {
        this.ospokenlang = value;
    }

    /**
     * Gets the value of the photo property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isPhoto() {
        return photo;
    }

    /**
     * Sets the value of the photo property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setPhoto(Boolean value) {
        this.photo = value;
    }

    /**
     * Gets the value of the pnames property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isPnames() {
        return pnames;
    }

    /**
     * Sets the value of the pnames property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setPnames(Boolean value) {
        this.pnames = value;
    }

    /**
     * Gets the value of the profession property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isProfession() {
        return profession;
    }

    /**
     * Sets the value of the profession property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setProfession(Boolean value) {
        this.profession = value;
    }

    /**
     * Gets the value of the religion property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isReligion() {
        return religion;
    }

    /**
     * Sets the value of the religion property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setReligion(Boolean value) {
        this.religion = value;
    }

    /**
     * Gets the value of the residenceAddress property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isResidenceAddress() {
        return residenceAddress;
    }

    /**
     * Sets the value of the residenceAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setResidenceAddress(Boolean value) {
        this.residenceAddress = value;
    }

    /**
     * Gets the value of the residencestatus property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isResidencestatus() {
        return residencestatus;
    }

    /**
     * Sets the value of the residencestatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setResidencestatus(Boolean value) {
        this.residencestatus = value;
    }

    /**
     * Gets the value of the selfOriginLga property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isSelfOriginLga() {
        return selfOriginLga;
    }

    /**
     * Sets the value of the selfOriginLga property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSelfOriginLga(Boolean value) {
        this.selfOriginLga = value;
    }

    /**
     * Gets the value of the selfOriginPlace property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isSelfOriginPlace() {
        return selfOriginPlace;
    }

    /**
     * Sets the value of the selfOriginPlace property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSelfOriginPlace(Boolean value) {
        this.selfOriginPlace = value;
    }

    /**
     * Gets the value of the selfOriginState property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isSelfOriginState() {
        return selfOriginState;
    }

    /**
     * Sets the value of the selfOriginState property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSelfOriginState(Boolean value) {
        this.selfOriginState = value;
    }

    /**
     * Gets the value of the signature property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isSignature() {
        return signature;
    }

    /**
     * Sets the value of the signature property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSignature(Boolean value) {
        this.signature = value;
    }

    /**
     * Gets the value of the telephoneno property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isTelephoneno() {
        return telephoneno;
    }

    /**
     * Sets the value of the telephoneno property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setTelephoneno(Boolean value) {
        this.telephoneno = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setTitle(Boolean value) {
        this.title = value;
    }

    /**
     * Gets the value of the userID property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isUserID() {
        return userID;
    }

    /**
     * Sets the value of the userID property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setUserID(Boolean value) {
        this.userID = value;
    }

}
