package com.smilecoms.rra.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

/**
 * 
 * @author rajeshkumar
 *
 */
public class CDRData {

    //@CsvBindByPosition(position = 0)
    @CsvBindByName(column = "Originator")
    private String originator;
    
    //@CsvBindByPosition(position = 1)
    @CsvBindByName(column = "Receipient")
    private String recipient;
    
    //@CsvBindByPosition(position = 2)
    @CsvBindByName(column = "Start_Time")
    private String startingTime;
        
    //@CsvBindByPosition(position = 3)
    @CsvBindByName(column = "Duration")
    private String duration;
            
    //@CsvBindByPosition(position = 4)
    @CsvBindByName(column = "Type")
    private String type;
        
    //@CsvBindByPosition(position = 5)
    @CsvBindByName(column = "Disposition")
    private String disposition;

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

	public String getStartingTime() {
		return startingTime;
	}

	public void setStartingTime(String startingTime) {
		this.startingTime = startingTime;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getDisposition() {
		return disposition;
	}

	public void setDisposition(String disposition) {
		this.disposition = disposition;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "CDRData [originator=" + originator + ", recipient=" + recipient + ", startingTime=" + startingTime
				+ ", duration=" + duration + ", disposition=" + disposition + ", type=" + type + "]";
	}

}
