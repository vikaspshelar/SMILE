package com.smilecoms.rra.model;

/**
 * 
 * @author rajeshkumar
 *
 */
public class CDRDataRequest {

	private String fromDate;
	private String toDate;
	private String originator;
	private String recipient;

	public String getFromDate() {
		return fromDate;
	}

	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}

	public String getToDate() {
		return toDate;
	}

	public void setToDate(String toDate) {
		this.toDate = toDate;
	}

	public String getOriginator() {
		return originator;
	}

	public void setOriginator(String originator) {
		this.originator = originator;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	@Override
	public String toString() {
		return "CDRDataRequest [fromDate=" + fromDate + ", toDate=" + toDate + ", originator=" + originator
				+ ", recipient=" + recipient + "]";
	}

}
