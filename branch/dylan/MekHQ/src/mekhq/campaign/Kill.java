/*
 * Kill.java
 * 
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.UUID;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Version;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * A kill record
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Kill implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4680018605784351078L;
	private UUID pilotId;
	private Date date;
	private String killed;
	private String killer;
	
	//reverse compatability
	private int oldPilotId = -1;
	
	public Kill() {
	
	}
	
	public Kill(UUID id, String kill, String killer, Date d) {
		pilotId = id;
		this.killed = kill;
		this.killer = killer;
		date = d;
	}
	
	public UUID getPilotId() {
		return pilotId;
	}
	
	public void setPilotId(UUID id) {
		pilotId = id;
	}
	
	public Date getDate() {
		return date;
	}
	
	public String getWhatKilled() {
		return killed;
	}
	
	public String getKilledByWhat() {
		return killer;
	}
	
	public void setDate(Date d) {
		date = d;
	}
	
	public void setWhatKilled(String s) {
		killed = s;
	}
	
	public void setKilledByWhat(String s) {
		killer = s;
	}
	
	public static Kill generateInstanceFromXML(Node wn, Version version) {
	
		Kill retVal = null;
		try {
			retVal = new Kill();
			NodeList nl = wn.getChildNodes();
		
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				if (wn2.getNodeName().equalsIgnoreCase("killed")) {
					retVal.killed = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("pilotId")) {
					if(version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
						retVal.oldPilotId = Integer.parseInt(wn2.getTextContent());
					} else {
						retVal.pilotId = UUID.fromString(wn2.getTextContent());
					}
				} else if (wn2.getNodeName().equalsIgnoreCase("killer")) {
					retVal.killer = (wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("date")) {
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
					retVal.date = df.parse(wn2.getTextContent().trim());
				}
			}
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQ.logError(ex);
		}
		return retVal;
	}
	
	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<kill>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<pilotId>"
				+pilotId.toString()
				+"</pilotId>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<killed>"
				+MekHqXmlUtil.escape(killed)
				+"</killed>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<killer>"
				+MekHqXmlUtil.escape(killer)
				+"</killer>");
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<date>"
				+df.format(date)
				+"</date>");
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</kill>");
		
	}
	
	public void fixIdReferences(Hashtable<Integer, UUID> pHash) {
    	pilotId = pHash.get(oldPilotId);
    }
	
	@Override
	public Kill clone() {
		return new Kill(getPilotId(), getWhatKilled(), getKilledByWhat(), getDate());
	}
	
}