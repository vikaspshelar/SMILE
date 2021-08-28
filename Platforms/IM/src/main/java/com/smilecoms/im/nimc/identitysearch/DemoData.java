
package com.smilecoms.im.nimc.identitysearch;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for demoData complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="demoData"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="batchid" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="birthcountry" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="birthdate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="birthlga" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="birthstate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="cardstatus" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="centralID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="documentno" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="educationallevel" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="email" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="emplymentstatus" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="firstname" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="gender" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="heigth" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="maidenname" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="maritalstatus" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="middlename" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="nin" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="nok_address1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="nok_address2" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="nok_firstname" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="nok_lga" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="nok_middlename" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="nok_postalcode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="nok_state" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="nok_surname" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="nok_town" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="nspokenlang" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="ospokenlang" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="othername" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="pfirstname" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="photo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="pmiddlename" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="profession" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="psurname" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="religion" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="residence_AdressLine1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="residence_AdressLine2" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="residence_Town" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="residence_lga" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="residence_postalcode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="residence_state" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="residencestatus" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="self_origin_lga" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="self_origin_place" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="self_origin_state" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="signature" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="surname" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="telephoneno" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="title" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="trackingId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "demoData", propOrder = {
    "batchid",
    "birthcountry",
    "birthdate",
    "birthlga",
    "birthstate",
    "cardstatus",
    "centralID",
    "documentno",
    "educationallevel",
    "email",
    "emplymentstatus",
    "firstname",
    "gender",
    "heigth",
    "maidenname",
    "maritalstatus",
    "middlename",
    "nin",
    "nokAddress1",
    "nokAddress2",
    "nokFirstname",
    "nokLga",
    "nokMiddlename",
    "nokPostalcode",
    "nokState",
    "nokSurname",
    "nokTown",
    "nspokenlang",
    "ospokenlang",
    "othername",
    "pfirstname",
    "photo",
    "pmiddlename",
    "profession",
    "psurname",
    "religion",
    "residenceAdressLine1",
    "residenceAdressLine2",
    "residenceTown",
    "residenceLga",
    "residencePostalcode",
    "residenceState",
    "residencestatus",
    "selfOriginLga",
    "selfOriginPlace",
    "selfOriginState",
    "signature",
    "surname",
    "telephoneno",
    "title",
    "trackingId"
})
public class DemoData {

    protected String batchid;
    protected String birthcountry;
    protected String birthdate;
    protected String birthlga;
    protected String birthstate;
    protected String cardstatus;
    protected String centralID;
    protected String documentno;
    protected String educationallevel;
    protected String email;
    protected String emplymentstatus;
    protected String firstname;
    protected String gender;
    protected String heigth;
    protected String maidenname;
    protected String maritalstatus;
    protected String middlename;
    protected String nin;
    @XmlElement(name = "nok_address1")
    protected String nokAddress1;
    @XmlElement(name = "nok_address2")
    protected String nokAddress2;
    @XmlElement(name = "nok_firstname")
    protected String nokFirstname;
    @XmlElement(name = "nok_lga")
    protected String nokLga;
    @XmlElement(name = "nok_middlename")
    protected String nokMiddlename;
    @XmlElement(name = "nok_postalcode")
    protected String nokPostalcode;
    @XmlElement(name = "nok_state")
    protected String nokState;
    @XmlElement(name = "nok_surname")
    protected String nokSurname;
    @XmlElement(name = "nok_town")
    protected String nokTown;
    protected String nspokenlang;
    protected String ospokenlang;
    protected String othername;
    protected String pfirstname;
    protected String photo;
    protected String pmiddlename;
    protected String profession;
    protected String psurname;
    protected String religion;
    @XmlElement(name = "residence_AdressLine1")
    protected String residenceAdressLine1;
    @XmlElement(name = "residence_AdressLine2")
    protected String residenceAdressLine2;
    @XmlElement(name = "residence_Town")
    protected String residenceTown;
    @XmlElement(name = "residence_lga")
    protected String residenceLga;
    @XmlElement(name = "residence_postalcode")
    protected String residencePostalcode;
    @XmlElement(name = "residence_state")
    protected String residenceState;
    protected String residencestatus;
    @XmlElement(name = "self_origin_lga")
    protected String selfOriginLga;
    @XmlElement(name = "self_origin_place")
    protected String selfOriginPlace;
    @XmlElement(name = "self_origin_state")
    protected String selfOriginState;
    protected String signature;
    protected String surname;
    protected String telephoneno;
    protected String title;
    protected String trackingId;

    /**
     * Gets the value of the batchid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBatchid() {
        return batchid;
    }

    /**
     * Sets the value of the batchid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBatchid(String value) {
        this.batchid = value;
    }

    /**
     * Gets the value of the birthcountry property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBirthcountry() {
        return birthcountry;
    }

    /**
     * Sets the value of the birthcountry property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBirthcountry(String value) {
        this.birthcountry = value;
    }

    /**
     * Gets the value of the birthdate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBirthdate() {
        return birthdate;
    }

    /**
     * Sets the value of the birthdate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBirthdate(String value) {
        this.birthdate = value;
    }

    /**
     * Gets the value of the birthlga property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBirthlga() {
        return birthlga;
    }

    /**
     * Sets the value of the birthlga property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBirthlga(String value) {
        this.birthlga = value;
    }

    /**
     * Gets the value of the birthstate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBirthstate() {
        return birthstate;
    }

    /**
     * Sets the value of the birthstate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBirthstate(String value) {
        this.birthstate = value;
    }

    /**
     * Gets the value of the cardstatus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCardstatus() {
        return cardstatus;
    }

    /**
     * Sets the value of the cardstatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCardstatus(String value) {
        this.cardstatus = value;
    }

    /**
     * Gets the value of the centralID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCentralID() {
        return centralID;
    }

    /**
     * Sets the value of the centralID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCentralID(String value) {
        this.centralID = value;
    }

    /**
     * Gets the value of the documentno property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDocumentno() {
        return documentno;
    }

    /**
     * Sets the value of the documentno property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDocumentno(String value) {
        this.documentno = value;
    }

    /**
     * Gets the value of the educationallevel property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEducationallevel() {
        return educationallevel;
    }

    /**
     * Sets the value of the educationallevel property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEducationallevel(String value) {
        this.educationallevel = value;
    }

    /**
     * Gets the value of the email property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the value of the email property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEmail(String value) {
        this.email = value;
    }

    /**
     * Gets the value of the emplymentstatus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEmplymentstatus() {
        return emplymentstatus;
    }

    /**
     * Sets the value of the emplymentstatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEmplymentstatus(String value) {
        this.emplymentstatus = value;
    }

    /**
     * Gets the value of the firstname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * Sets the value of the firstname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFirstname(String value) {
        this.firstname = value;
    }

    /**
     * Gets the value of the gender property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGender() {
        return gender;
    }

    /**
     * Sets the value of the gender property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGender(String value) {
        this.gender = value;
    }

    /**
     * Gets the value of the heigth property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHeigth() {
        return heigth;
    }

    /**
     * Sets the value of the heigth property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHeigth(String value) {
        this.heigth = value;
    }

    /**
     * Gets the value of the maidenname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMaidenname() {
        return maidenname;
    }

    /**
     * Sets the value of the maidenname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMaidenname(String value) {
        this.maidenname = value;
    }

    /**
     * Gets the value of the maritalstatus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMaritalstatus() {
        return maritalstatus;
    }

    /**
     * Sets the value of the maritalstatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMaritalstatus(String value) {
        this.maritalstatus = value;
    }

    /**
     * Gets the value of the middlename property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMiddlename() {
        return middlename;
    }

    /**
     * Sets the value of the middlename property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMiddlename(String value) {
        this.middlename = value;
    }

    /**
     * Gets the value of the nin property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNin() {
        return nin;
    }

    /**
     * Sets the value of the nin property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNin(String value) {
        this.nin = value;
    }

    /**
     * Gets the value of the nokAddress1 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNokAddress1() {
        return nokAddress1;
    }

    /**
     * Sets the value of the nokAddress1 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNokAddress1(String value) {
        this.nokAddress1 = value;
    }

    /**
     * Gets the value of the nokAddress2 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNokAddress2() {
        return nokAddress2;
    }

    /**
     * Sets the value of the nokAddress2 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNokAddress2(String value) {
        this.nokAddress2 = value;
    }

    /**
     * Gets the value of the nokFirstname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNokFirstname() {
        return nokFirstname;
    }

    /**
     * Sets the value of the nokFirstname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNokFirstname(String value) {
        this.nokFirstname = value;
    }

    /**
     * Gets the value of the nokLga property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNokLga() {
        return nokLga;
    }

    /**
     * Sets the value of the nokLga property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNokLga(String value) {
        this.nokLga = value;
    }

    /**
     * Gets the value of the nokMiddlename property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNokMiddlename() {
        return nokMiddlename;
    }

    /**
     * Sets the value of the nokMiddlename property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNokMiddlename(String value) {
        this.nokMiddlename = value;
    }

    /**
     * Gets the value of the nokPostalcode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNokPostalcode() {
        return nokPostalcode;
    }

    /**
     * Sets the value of the nokPostalcode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNokPostalcode(String value) {
        this.nokPostalcode = value;
    }

    /**
     * Gets the value of the nokState property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNokState() {
        return nokState;
    }

    /**
     * Sets the value of the nokState property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNokState(String value) {
        this.nokState = value;
    }

    /**
     * Gets the value of the nokSurname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNokSurname() {
        return nokSurname;
    }

    /**
     * Sets the value of the nokSurname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNokSurname(String value) {
        this.nokSurname = value;
    }

    /**
     * Gets the value of the nokTown property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNokTown() {
        return nokTown;
    }

    /**
     * Sets the value of the nokTown property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNokTown(String value) {
        this.nokTown = value;
    }

    /**
     * Gets the value of the nspokenlang property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNspokenlang() {
        return nspokenlang;
    }

    /**
     * Sets the value of the nspokenlang property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNspokenlang(String value) {
        this.nspokenlang = value;
    }

    /**
     * Gets the value of the ospokenlang property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOspokenlang() {
        return ospokenlang;
    }

    /**
     * Sets the value of the ospokenlang property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOspokenlang(String value) {
        this.ospokenlang = value;
    }

    /**
     * Gets the value of the othername property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOthername() {
        return othername;
    }

    /**
     * Sets the value of the othername property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOthername(String value) {
        this.othername = value;
    }

    /**
     * Gets the value of the pfirstname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPfirstname() {
        return pfirstname;
    }

    /**
     * Sets the value of the pfirstname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPfirstname(String value) {
        this.pfirstname = value;
    }

    /**
     * Gets the value of the photo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPhoto() {
        return photo;
    }

    /**
     * Sets the value of the photo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPhoto(String value) {
        this.photo = value;
    }

    /**
     * Gets the value of the pmiddlename property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPmiddlename() {
        return pmiddlename;
    }

    /**
     * Sets the value of the pmiddlename property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPmiddlename(String value) {
        this.pmiddlename = value;
    }

    /**
     * Gets the value of the profession property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProfession() {
        return profession;
    }

    /**
     * Sets the value of the profession property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProfession(String value) {
        this.profession = value;
    }

    /**
     * Gets the value of the psurname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPsurname() {
        return psurname;
    }

    /**
     * Sets the value of the psurname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPsurname(String value) {
        this.psurname = value;
    }

    /**
     * Gets the value of the religion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReligion() {
        return religion;
    }

    /**
     * Sets the value of the religion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReligion(String value) {
        this.religion = value;
    }

    /**
     * Gets the value of the residenceAdressLine1 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResidenceAdressLine1() {
        return residenceAdressLine1;
    }

    /**
     * Sets the value of the residenceAdressLine1 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResidenceAdressLine1(String value) {
        this.residenceAdressLine1 = value;
    }

    /**
     * Gets the value of the residenceAdressLine2 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResidenceAdressLine2() {
        return residenceAdressLine2;
    }

    /**
     * Sets the value of the residenceAdressLine2 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResidenceAdressLine2(String value) {
        this.residenceAdressLine2 = value;
    }

    /**
     * Gets the value of the residenceTown property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResidenceTown() {
        return residenceTown;
    }

    /**
     * Sets the value of the residenceTown property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResidenceTown(String value) {
        this.residenceTown = value;
    }

    /**
     * Gets the value of the residenceLga property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResidenceLga() {
        return residenceLga;
    }

    /**
     * Sets the value of the residenceLga property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResidenceLga(String value) {
        this.residenceLga = value;
    }

    /**
     * Gets the value of the residencePostalcode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResidencePostalcode() {
        return residencePostalcode;
    }

    /**
     * Sets the value of the residencePostalcode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResidencePostalcode(String value) {
        this.residencePostalcode = value;
    }

    /**
     * Gets the value of the residenceState property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResidenceState() {
        return residenceState;
    }

    /**
     * Sets the value of the residenceState property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResidenceState(String value) {
        this.residenceState = value;
    }

    /**
     * Gets the value of the residencestatus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResidencestatus() {
        return residencestatus;
    }

    /**
     * Sets the value of the residencestatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResidencestatus(String value) {
        this.residencestatus = value;
    }

    /**
     * Gets the value of the selfOriginLga property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSelfOriginLga() {
        return selfOriginLga;
    }

    /**
     * Sets the value of the selfOriginLga property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSelfOriginLga(String value) {
        this.selfOriginLga = value;
    }

    /**
     * Gets the value of the selfOriginPlace property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSelfOriginPlace() {
        return selfOriginPlace;
    }

    /**
     * Sets the value of the selfOriginPlace property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSelfOriginPlace(String value) {
        this.selfOriginPlace = value;
    }

    /**
     * Gets the value of the selfOriginState property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSelfOriginState() {
        return selfOriginState;
    }

    /**
     * Sets the value of the selfOriginState property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSelfOriginState(String value) {
        this.selfOriginState = value;
    }

    /**
     * Gets the value of the signature property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Sets the value of the signature property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSignature(String value) {
        this.signature = value;
    }

    /**
     * Gets the value of the surname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Sets the value of the surname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSurname(String value) {
        this.surname = value;
    }

    /**
     * Gets the value of the telephoneno property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTelephoneno() {
        return telephoneno;
    }

    /**
     * Sets the value of the telephoneno property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTelephoneno(String value) {
        this.telephoneno = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitle(String value) {
        this.title = value;
    }

    /**
     * Gets the value of the trackingId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTrackingId() {
        return trackingId;
    }

    /**
     * Sets the value of the trackingId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTrackingId(String value) {
        this.trackingId = value;
    }

}
