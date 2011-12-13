/*
 * UnitStub.java
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

package mekhq.campaign.force;

import java.io.PrintWriter;
import java.io.Serializable;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Pilot;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.personnel.Person;


	public class UnitStub implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1448449600864209589L;
		
		private String desc;
		private String portraitCategory;
		private String portraitFileName;
		
		public UnitStub() {
			portraitCategory = Pilot.ROOT_PORTRAIT;
	        portraitFileName = Pilot.PORTRAIT_NONE;
			desc = "";
		}
		
		public UnitStub(Unit u) {
			portraitCategory = Pilot.ROOT_PORTRAIT;
	        portraitFileName = Pilot.PORTRAIT_NONE;
			desc = getUnitDescription(u);
			Person commander = u.getCommander();
			if(null != commander) {
				portraitCategory = commander.getPortraitCategory();
				portraitFileName = commander.getPortraitFileName();
			}
		}
		
		public String toString() {
			return desc;
		}
		
		public String getPortraitCategory() {
			return portraitCategory;
		}
		
		public String getPortraitFileName() {
			return portraitFileName;
		}
		
		private String getUnitDescription(Unit u) {
			String name = "<font color='red'>No Crew</font>";
	    	String uname = "";
	    	Person pp = u.getCommander();
	    	if(null != pp) {
	    		name = pp.getFullTitle();
	    		name += " (" + u.getEntity().getCrew().getGunnery() + "/" + u.getEntity().getCrew().getPiloting() + ")";
	    		if(pp.needsFixing()) {
	    			name = "<font color='red'>" + name + "</font>";
	    		}     
	    	}
	    	uname = "<i>" + u.getName() + "</i>";
	    	if(u.isDamaged()) {
	    		uname = "<font color='red'>" + uname + "</font>";
	    	}          	
	    	return "<html>" + name + ", " + uname + "</html>";
		}
		
		public void writeToXml(PrintWriter pw1, int indent) {
			pw1.println(MekHqXmlUtil.indentStr(indent) + "<unitStub>");
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<desc>"
					+MekHqXmlUtil.escape(desc)
					+"</desc>");
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<portraitCategory>"
					+portraitCategory
					+"</portraitCategory>");
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<portraitFileName>"
					+portraitFileName
					+"</portraitFileName>");
			pw1.println(MekHqXmlUtil.indentStr(indent) + "</unitStub>");
		}
		
		public static UnitStub generateInstanceFromXML(Node wn) {
			UnitStub retVal = null;
			
			try {		
				retVal = new UnitStub();
				NodeList nl = wn.getChildNodes();
				
				for (int x=0; x<nl.getLength(); x++) {
					Node wn2 = nl.item(x);
					if (wn2.getNodeName().equalsIgnoreCase("desc")) {
						retVal.desc = wn2.getTextContent();
					} else if (wn2.getNodeName().equalsIgnoreCase("portraitCategory")) {
						retVal.portraitCategory = wn2.getTextContent();
					} else if (wn2.getNodeName().equalsIgnoreCase("portraitFileName")) {
						retVal.portraitFileName = wn2.getTextContent();
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
	}