/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.sms;


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
public class PortInformationElement extends InformationElement
{
	public static final int PORT_16BIT = 0x05;

	PortInformationElement(byte id, byte[] data)
	{
		super(id, data);
		if (getIdentifier() != PORT_16BIT) { throw new RuntimeException("Invalid identifier " + getIdentifier() + " in data in: " + getClass().getSimpleName()); }
		// iei
		// iel
		// dest(2 bytes)
		// src (2 bytes)
		if (data.length != 4) { throw new RuntimeException("Invalid data length in: " + getClass().getSimpleName()); }
	}

	PortInformationElement(int identifier, int destPort, int srcPort)
	{
		super();
		byte[] data = null;
		switch (identifier)
		{
			case PORT_16BIT:
				data = new byte[4];
				data[0] = (byte) ((destPort & 0xFF00) >>> 8);
				data[1] = (byte) (destPort & 0xFF);
				data[2] = (byte) ((srcPort & 0xFF00) >>> 8);
				data[3] = (byte) (srcPort & 0xFF);
				break;
			default:
				throw new RuntimeException("Invalid identifier for " + getClass().getSimpleName());
		}
		initialize((byte) (identifier & 0xFF), data);
	}

	public int getDestPort()
	{
		// first 2 bytes of data
		byte[] data = getData();
		return (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
	}

	public int getSrcPort()
	{
		// next 2 bytes of data
		byte[] data = getData();
		return (((data[2] & 0xFF) << 8) | (data[3] & 0xFF));
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		sb.append("[Dst Port: ");
		sb.append(getDestPort());
		sb.append(", Src Port: ");
		sb.append(getSrcPort());
		sb.append("]");
		return sb.toString();
	}
}
