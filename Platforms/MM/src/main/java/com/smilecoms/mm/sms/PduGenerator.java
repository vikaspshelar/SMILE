/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.sms;


import java.io.*;
import java.util.*;

//PduUtils Library - A Java library for generating GSM 3040 Protocol Data Units (PDUs)
//
//Copyright (C) 2008, Ateneo Java Wireless Competency Center/Blueblade Technologies, Philippines.
//PduUtils is distributed under the terms of the Apache License version 2.0
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
public class PduGenerator
{
	private ByteArrayOutputStream baos;

	private int firstOctetPosition = -1;

	private boolean updateFirstOctet = false;

	protected void writeSmscInfo(Pdu pdu) throws Exception
	{
		if (pdu.getSmscAddress() != null)
		{
			writeBCDAddress(pdu.getSmscAddress(), pdu.getSmscAddressType(), pdu.getSmscInfoLength());
		}
		else
		{
			writeByte(0);
		}
	}

	protected void writeFirstOctet(Pdu pdu)
	{
		// store the position in case it will need to be updated later
		firstOctetPosition = pdu.getSmscInfoLength() + 1;
		writeByte(pdu.getFirstOctet());
	}

	// validity period conversion from hours to the proper integer
	protected void writeValidityPeriodInteger(int validityPeriod)
	{
		if (validityPeriod == -1)
		{
			baos.write(0xFF);
		}
		else
		{
			int validityInt;
			if (validityPeriod <= 12) validityInt = (validityPeriod * 12) - 1;
			else if (validityPeriod <= 24) validityInt = (((validityPeriod - 12) * 2) + 143);
			else if (validityPeriod <= 720) validityInt = (validityPeriod / 24) + 166;
			else validityInt = (validityPeriod / 168) + 192;
			baos.write(validityInt);
		}
	}

	protected void writeTimeStampStringForDate(Date timestamp)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(timestamp);
		int year = cal.get(Calendar.YEAR) - 2000;
		int month = cal.get(Calendar.MONTH) + 1;
		int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
		int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		TimeZone tz = cal.getTimeZone();
		int offset = tz.getOffset(timestamp.getTime());
		int minOffset = offset / 60000;
		int tzValue = minOffset / 15;
		// for negative offsets, add 128 to the absolute value
		if (tzValue < 0)
		{
			tzValue = 128 - tzValue;
		}
		// note: the nibbles are written as BCD style
		baos.write(PduUtils.createSwappedBCD(year));
		baos.write(PduUtils.createSwappedBCD(month));
		baos.write(PduUtils.createSwappedBCD(dayOfMonth));
		baos.write(PduUtils.createSwappedBCD(hourOfDay));
		baos.write(PduUtils.createSwappedBCD(minute));
		baos.write(PduUtils.createSwappedBCD(sec));
		baos.write(PduUtils.createSwappedBCD(tzValue));
	}

	protected void writeAddress(String address, int addressType, int addressLength) throws Exception
	{
		switch (PduUtils.extractAddressType(addressType))
		{
			case PduUtils.ADDRESS_TYPE_ALPHANUMERIC:
				byte[] textSeptets = PduUtils.stringToUnencodedSeptets(address);
				byte[] alphaNumBytes = PduUtils.encode7bitUserData(null, textSeptets);
				// ADDRESS LENGTH - should be the semi-octet count
				//                - this type is not used for SMSCInfo
				baos.write(alphaNumBytes.length * 2);
				// ADDRESS TYPE
				baos.write(addressType);
				// ADDRESS TEXT
				baos.write(alphaNumBytes);
				break;
			default:
				// BCD-style
				writeBCDAddress(address, addressType, addressLength);
		}
	}

	protected void writeBCDAddress(String address, int addressType, int addressLength) throws Exception
	{
		// BCD-style
		// ADDRESS LENGTH - either an octet count or semi-octet count
		baos.write(addressLength);
		// ADDRESS TYPE
		baos.write(addressType);
		// ADDRESS NUMBERS
		// if address.length is not even, pad the string an with F at the end
		if (address.length() % 2 == 1)
		{
			address = address + "F";
		}
		int digit = 0;
		for (int i = 0; i < address.length(); i++)
		{
			char c = address.charAt(i);
			if (i % 2 == 1)
			{
				digit |= ((Integer.parseInt(Character.toString(c), 16)) << 4);
				baos.write(digit);
				// clear it
				digit = 0;
			}
			else
			{
				digit |= (Integer.parseInt(Character.toString(c), 16) & 0x0F);
			}
		}
	}

	protected void writeUDData(Pdu pdu, int mpRefNo, int partNo)
	{
		int dcs = pdu.getDataCodingScheme();
		try
		{
			switch (PduUtils.extractDcsEncoding(dcs))
			{
				case PduUtils.DCS_ENCODING_7BIT:
					writeUDData7bit(pdu, mpRefNo, partNo);
					break;
				case PduUtils.DCS_ENCODING_8BIT:
					writeUDData8bit(pdu, mpRefNo, partNo);
					break;
				case PduUtils.DCS_ENCODING_UCS2:
					writeUDDataUCS2(pdu, mpRefNo, partNo);
					break;
				default:
					throw new RuntimeException("Invalid DCS encoding: " + PduUtils.extractDcsEncoding(dcs));
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	protected void writeUDH(Pdu pdu) throws IOException
	{
		// stream directly into the internal baos
		writeUDH(pdu, baos);
	}

	protected void writeUDH(Pdu pdu, ByteArrayOutputStream udhBaos) throws IOException
	{
		// need to insure that proper concat info is inserted
		// before writing if needed
		// i.e. the reference number, maxseq and seq have to be set from
		// outside (OutboundMessage)
		udhBaos.write(pdu.getUDHLength());
		for (Iterator<InformationElement> ieIterator = pdu.getInformationElements(); ieIterator.hasNext();)
		{
			InformationElement ie = ieIterator.next();
			udhBaos.write(ie.getIdentifier());
			udhBaos.write(ie.getLength());
			udhBaos.write(ie.getData());
		}
	}

	protected int computeOffset(Pdu pdu, int maxMessageLength, int partNo)
	{
		// computes offset to which part of the string is to be encoded into the PDU
		// also sets the MpMaxNo field of the concatInfo if message is multi-part
		int offset;
		int maxParts = 1;
		if (!pdu.isBinary())
		{
			maxParts = pdu.getDecodedText().length() / maxMessageLength + 1;
		}
		else
		{
			maxParts = pdu.getDataBytes().length / maxMessageLength + 1;
		}
		if (pdu.hasTpUdhi())
		{
			if (pdu.getConcatInfo() != null)
			{
				if (partNo > 0)
				{
					pdu.getConcatInfo().setMpMaxNo(maxParts);
				}
			}
		}
		if ((maxParts > 1) && (partNo > 0))
		{
			//      - if partNo > maxParts
			//          - error
			if (partNo > maxParts) { throw new RuntimeException("Invalid partNo: " + partNo + ", maxParts=" + maxParts); }
			offset = ((partNo - 1) * maxMessageLength);
		}
		else
		{
			// just get from the start
			offset = 0;
		}
		return offset;
	}

	protected void checkForConcat(Pdu pdu, int lengthOfText, int maxLength, int maxLengthWithUdh, int mpRefNo, int partNo)
	{
		if ((lengthOfText <= maxLengthWithUdh) || ((lengthOfText > maxLengthWithUdh) && (lengthOfText <= maxLength)))
		{
			// nothing needed
		}
		else
		{
			// need concat
			if (pdu.getConcatInfo() != null)
			{
				// if concatInfo is already present then just replace the values with the supplied
				pdu.getConcatInfo().setMpRefNo(mpRefNo);
				pdu.getConcatInfo().setMpSeqNo(partNo);
			}
			else
			{
				// add concat info with the specified mpRefNo, bogus maxSeqNo, and partNo
				// bogus maxSeqNo will be replaced once it is known in the later steps
				// this just needs to be added since its presence is needed to compute
				// the UDH length
				ConcatInformationElement concatInfo = InformationElementFactory.generateConcatInfo(mpRefNo, partNo);
				pdu.addInformationElement(concatInfo);
				updateFirstOctet = true;
			}
		}
	}

	protected int computePotentialUdhLength(Pdu pdu)
	{
		int currentUdhLength = pdu.getTotalUDHLength();
		if (currentUdhLength == 0)
		{
			// add 1 for the UDH Length field
			return ConcatInformationElement.getDefaultConcatLength() + 1;
		}
		else
		{
			// this already has the UDH Length field, no need to add 1
			return currentUdhLength + ConcatInformationElement.getDefaultConcatLength();
		}
	}

	protected void writeUDData7bit(Pdu pdu, int mpRefNo, int partNo) throws Exception
	{
		String decodedText = pdu.getDecodedText();
		// partNo states what part of the unencoded text will be used
		//      - max length is based on the size of the UDH
		//        for 7bit => maxLength = 160 - total UDH septets
		// check if this message needs a concat
		byte[] textSeptetsForDecodedText = PduUtils.stringToUnencodedSeptets(decodedText);
		int potentialUdhLength = PduUtils.getNumSeptetsForOctets(computePotentialUdhLength(pdu));
		checkForConcat(pdu, textSeptetsForDecodedText.length, 160 - PduUtils.getNumSeptetsForOctets(pdu.getTotalUDHLength()), // CHANGED
				160 - potentialUdhLength, mpRefNo, partNo);
		// given the IEs in the pdu derive the max message body length
		// this length will include the potential concat added in the previous step
		int totalUDHLength = pdu.getTotalUDHLength();
		int maxMessageLength = 160 - PduUtils.getNumSeptetsForOctets(totalUDHLength);
		// get septets for part
		byte[] textSeptets = getUnencodedSeptetsForPart(pdu, maxMessageLength, partNo);
		// udlength is the sum of udh septet length and the text septet length
		int udLength = PduUtils.getNumSeptetsForOctets(totalUDHLength) + textSeptets.length;
		baos.write(udLength);
		// generate UDH byte[]
		// UDHL (sum of all IE lengths)
		// IE list        
		byte[] udhBytes = null;
		if (pdu.hasTpUdhi())
		{
			ByteArrayOutputStream udhBaos = new ByteArrayOutputStream();
			writeUDH(pdu, udhBaos);
			// buffer the udh since this needs to be 7-bit encoded with the text
			udhBytes = udhBaos.toByteArray();
		}
		// encode both as one unit        
		byte[] udBytes = PduUtils.encode7bitUserData(udhBytes, textSeptets);
		// write combined encoded array
		baos.write(udBytes);
	}

	private byte[] getUnencodedSeptetsForPart(Pdu pdu, int maxMessageLength, int partNo)
	{
		// computes offset to which part of the string is to be encoded into the PDU
		// also sets the MpMaxNo field of the concatInfo if message is multi-part
		int offset;
		int maxParts = 1;
		// must use the unencoded septets not the actual string since
		// it is possible that some special characters in string are multi-septet
		byte[] unencodedSeptets = PduUtils.stringToUnencodedSeptets(pdu.getDecodedText());
		maxParts = (unencodedSeptets.length / maxMessageLength) + 1;
		if (pdu.hasTpUdhi())
		{
			if (pdu.getConcatInfo() != null)
			{
				if (partNo > 0)
				{
					pdu.getConcatInfo().setMpMaxNo(maxParts);
				}
			}
		}
		if ((maxParts > 1) && (partNo > 0))
		{
			//      - if partNo > maxParts
			//          - error
			if (partNo > maxParts) { throw new RuntimeException("Invalid partNo: " + partNo + ", maxParts=" + maxParts); }
			offset = ((partNo - 1) * maxMessageLength);
		}
		else
		{
			// just get from the start
			offset = 0;
		}
		// copy the portion of the full unencoded septet array for this part
		byte[] septetsForPart = new byte[Math.min(maxMessageLength, unencodedSeptets.length - offset)];
		System.arraycopy(unencodedSeptets, offset, septetsForPart, 0, septetsForPart.length);
		return septetsForPart;
	}

	protected void writeUDData8bit(Pdu pdu, int mpRefNo, int partNo) throws Exception
	{
		// NOTE: binary messages are also handled here
		byte[] data;
		if (pdu.isBinary())
		{
			// use the supplied bytes
			data = pdu.getDataBytes();
		}
		else
		{
			// encode the text
			data = PduUtils.encode8bitUserData(pdu.getDecodedText());
		}
		// partNo states what part of the unencoded text will be used
		//      - max length is based on the size of the UDH
		//        for 8bit => maxLength = 140 - the total UDH bytes
		// check if this message needs a concat
		int potentialUdhLength = computePotentialUdhLength(pdu);
		checkForConcat(pdu, data.length, 140 - pdu.getTotalUDHLength(), // CHANGED
				140 - potentialUdhLength, mpRefNo, partNo);
		// given the IEs in the pdu derive the max message body length
		// this length will include the potential concat added in the previous step        
		int totalUDHLength = pdu.getTotalUDHLength();
		int maxMessageLength = 140 - totalUDHLength;
		// compute which portion of the message will be part of the message
		int offset = computeOffset(pdu, maxMessageLength, partNo);
		byte[] dataToWrite = new byte[Math.min(maxMessageLength, data.length - offset)];
		System.arraycopy(data, offset, dataToWrite, 0, dataToWrite.length);
		// generate udlength
		// based on partNo
		// udLength is an octet count for 8bit/ucs2
		int udLength = totalUDHLength + dataToWrite.length;
		// write udlength
		baos.write(udLength);
		// write UDH to the stream directly
		if (pdu.hasTpUdhi())
		{
			writeUDH(pdu, baos);
		}
		// write data
		baos.write(dataToWrite);
	}

	protected void writeUDDataUCS2(Pdu pdu, int mpRefNo, int partNo) throws Exception
	{
		String decodedText = pdu.getDecodedText();
		// partNo states what part of the unencoded text will be used
		//      - max length is based on the size of the UDH
		//        for ucs2 => maxLength = (140 - the total UDH bytes)/2
		// check if this message needs a concat
		int potentialUdhLength = computePotentialUdhLength(pdu);
		checkForConcat(pdu, decodedText.length(), (140 - pdu.getTotalUDHLength()) / 2, // CHANGED
				(140 - potentialUdhLength) / 2, mpRefNo, partNo);
		// given the IEs in the pdu derive the max message body length
		// this length will include the potential concat added in the previous step        
		int totalUDHLength = pdu.getTotalUDHLength();
		int maxMessageLength = (140 - totalUDHLength) / 2;
		// compute which portion of the message will be part of the message
		int offset = computeOffset(pdu, maxMessageLength, partNo);
		String textToEncode = decodedText.substring(offset, Math.min(offset + maxMessageLength, decodedText.length()));
		// generate udlength
		// based on partNo
		// udLength is an octet count for 8bit/ucs2
		int udLength = totalUDHLength + (textToEncode.length() * 2);
		// write udlength
		baos.write(udLength);
		// write UDH to the stream directly
		if (pdu.hasTpUdhi())
		{
			writeUDH(pdu, baos);
		}
		// write encoded text
		baos.write(PduUtils.encodeUcs2UserData(textToEncode));
	}

	protected void writeByte(int i)
	{
		baos.write(i);
	}

	protected void writeBytes(byte[] b) throws Exception
	{
		baos.write(b);
	}

	public List<String> generatePduList(Pdu pdu, int mpRefNo)
	{
		// generate all required PDUs for a given message
		// mpRefNo comes from the ModemGateway
		ArrayList<String> pduList = new ArrayList<String>();
		for (int i = 1; i <= pdu.getMpMaxNo(); i++)
		{
			String pduString = generatePduString(pdu, mpRefNo, i);
			pduList.add(pduString);
		}
		return pduList;
	}

	public String generatePduString(Pdu pdu)
	{
		return generatePduString(pdu, -1, -1);
	}

	// NOTE: partNo indicates which part of a multipart message to generate
	//       assuming that the message is multipart, this will be ignored if the
	//       message is not a concat message
	public String generatePduString(Pdu pdu, int mpRefNo, int partNo)
	{
		try
		{
			baos = new ByteArrayOutputStream();
			firstOctetPosition = -1;
			updateFirstOctet = false;
			// process the PDU
			switch (pdu.getTpMti())
			{
				case PduUtils.TP_MTI_SMS_DELIVER:
					generateSmsDeliverPduString((SmsDeliveryPdu) pdu, mpRefNo, partNo);
					break;
//				case PduUtils.TP_MTI_SMS_SUBMIT:
//					generateSmsSubmitPduString((SmsSubmitPdu) pdu, mpRefNo, partNo);
//					break;
//				case PduUtils.TP_MTI_SMS_STATUS_REPORT:
//					generateSmsStatusReportPduString((SmsStatusReportPdu) pdu);
//					break;
			}
			// in case concat is detected in the writeUD() method
			// and there was no UDHI at the time of detection
			// the old firstOctet must be overwritten with the new value
			byte[] pduBytes = baos.toByteArray();
			if (updateFirstOctet)
			{
				pduBytes[firstOctetPosition] = (byte) (pdu.getFirstOctet() & 0xFF);
			}
			return PduUtils.bytesToPdu(pduBytes);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

//	protected void generateSmsSubmitPduString(SmsSubmitPdu pdu, int mpRefNo, int partNo) throws Exception
//	{
//		// SMSC address info
//		writeSmscInfo(pdu);
//		// first octet
//		writeFirstOctet(pdu);
//		// message reference
//		writeByte(pdu.getMessageReference());
//		// destination address info
//		writeAddress(pdu.getAddress(), pdu.getAddressType(), pdu.getAddress().length());
//		// protocol id
//		writeByte(pdu.getProtocolIdentifier());
//		// data coding scheme
//		writeByte(pdu.getDataCodingScheme());
//		// validity period
//		switch (pdu.getTpVpf())
//		{
//			case PduUtils.TP_VPF_INTEGER:
//				writeValidityPeriodInteger(pdu.getValidityPeriod());
//				break;
//			case PduUtils.TP_VPF_TIMESTAMP:
//				writeTimeStampStringForDate(pdu.getValidityDate());
//				break;
//		}
//		// user data
//		// headers        
//		writeUDData(pdu, mpRefNo, partNo);
//	}

	// NOTE: the following are just for validation of the PduParser
	//       - there is no normal scenario where these are used
	protected void generateSmsDeliverPduString(SmsDeliveryPdu pdu, int mpRefNo, int partNo) throws Exception
	{
		// SMSC address info
		writeSmscInfo(pdu);
		// first octet
		writeFirstOctet(pdu);
		// originator address info
		writeAddress(pdu.getAddress(), pdu.getAddressType(), pdu.getAddress().length());
		// protocol id
		writeByte(pdu.getProtocolIdentifier());
		// data coding scheme
		writeByte(pdu.getDataCodingScheme());
		// timestamp
		writeTimeStampStringForDate(pdu.getTimestamp());
		// user data
		// headers
		writeUDData(pdu, mpRefNo, partNo);
	}

//	protected void generateSmsStatusReportPduString(SmsStatusReportPdu pdu) throws Exception
//	{
//		// SMSC address info
//		writeSmscInfo(pdu);
//		// first octet
//		writeFirstOctet(pdu);
//		// message reference
//		writeByte(pdu.getMessageReference());
//		// destination address info
//		writeAddress(pdu.getAddress(), pdu.getAddressType(), pdu.getAddress().length());
//		// timestamp
//		writeTimeStampStringForDate(pdu.getTimestamp());
//		// discharge time(timestamp)
//		writeTimeStampStringForDate(pdu.getDischargeTime());
//		// status
//		writeByte(pdu.getStatus());
//	}
}
