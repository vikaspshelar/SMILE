package com.smilecoms.rra.model;

import java.util.HashSet;
import java.util.Set;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

/**
 * 
 * @author rajeshkumar
 *
 */
public class UCCUserData {

    private String firstName;
    private String surName;
    private String idNumber;
    private String idType;
    private Set<String> msisdnList = new HashSet<>();

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getSurName() {
		return surName;
	}

	public void setSurName(String surName) {
		this.surName = surName;
	}

	public String getIdType() {
		return idType;
	}

	public void setIdType(String idType) {
		this.idType = idType;
	}

	public String getIdNumber() {
		return idNumber;
	}

	public void setIdNumber(String idNumber) {
		this.idNumber = idNumber;
	}

	public Set<String> getMsisdnList() {
		return msisdnList;
	}

	public void setMsisdnList(Set<String> msisdnList) {
		this.msisdnList = msisdnList;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((idNumber == null) ? 0 : idNumber.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UCCUserData other = (UCCUserData) obj;
		if (idNumber == null) {
			if (other.idNumber != null)
				return false;
		} else if (!idNumber.equals(other.idNumber))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UCCUserData [firstName=" + firstName + ", surName=" + surName + ", idType=" + idType + ", idNumber="
				+ idNumber + ", msisdnList=" + msisdnList + "]";
	}

}
