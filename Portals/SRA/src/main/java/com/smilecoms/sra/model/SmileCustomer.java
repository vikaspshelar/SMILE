/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author rajeshkumar
 */
public class SmileCustomer {
    @XmlElement(name = "CustomerId")
    protected int customerId;
    @XmlElement(name = "Title", required = true)
    protected String title;
    @XmlElement(name = "FirstName", required = true)
    protected String firstName;
    @XmlElement(name = "MiddleName", required = false)
    protected String middleName;
    @XmlElement(name = "LastName", required = true)
    protected String lastName;
    @XmlElement(name = "AlternativeContact1", required = true)
    protected String alternativeContact1;
    @XmlElement(name = "AlternativeContact2", required = true)
    protected String alternativeContact2;
    @XmlElement(name = "EmailAddress", required = false)
    protected String emailAddress;

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAlternativeContact1() {
        return alternativeContact1;
    }

    public void setAlternativeContact1(String alternativeContact1) {
        this.alternativeContact1 = alternativeContact1;
    }

    public String getAlternativeContact2() {
        return alternativeContact2;
    }

    public void setAlternativeContact2(String alternativeContact2) {
        this.alternativeContact2 = alternativeContact2;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    
}
