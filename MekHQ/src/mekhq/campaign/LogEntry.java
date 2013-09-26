/*
 * LogEntry.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 * 
 * This file is part of MekHQ.
 * 
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class LogEntry implements MekHqXmlSerializable {
	
	private Date date;
	private String desc;

	public LogEntry() {
		this(null, "");
	}
	
	public LogEntry(Date d, String de) {
		this.date = d;
		this.desc = de;
	}
	
	public void setDate(Date d) {
		this.date = d;
	}
	
	public Date getDate() {
		return date;
	}
	
	public void setDesc(String d) {
		this.desc = d;
	}
	
	public String getDesc() {
		return desc;
	}
	
	public void writeToXml(PrintWriter pw1, int indent) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<logEntry>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<date>"
				+df.format(date)
				+"</date>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<desc>"
				+MekHqXmlUtil.escape(desc)
				+"</desc>");
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</logEntry>");
	}
	
	public static LogEntry generateInstanceFromXML(Node wn) {
		LogEntry retVal = new LogEntry();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		
		try {	
			// Okay, now load fields!
			NodeList nl = wn.getChildNodes();
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				
				if (wn2.getNodeName().equalsIgnoreCase("desc")) {
					retVal.desc = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("date")) {
					retVal.date = df.parse(wn2.getTextContent().trim());
				}
			}
		} catch (Exception ex) {
			// Doh!
			MekHQ.logError(ex);
		}
		
		return retVal;
	}
	
	@Override
	public LogEntry clone() {
		return new LogEntry(getDate(), getDesc());
	}
}