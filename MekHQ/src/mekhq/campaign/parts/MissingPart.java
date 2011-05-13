/*
 * MissingPart.java
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

package mekhq.campaign.parts;

import java.io.PrintWriter;
import java.io.Serializable;

import megamek.common.EquipmentType;
import megamek.common.TargetRoll;
import mekhq.campaign.MekHqXmlSerializable;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Utilities;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.Modes;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A missing part is a placeholder on a unit to indicate that a replacement
 * task needs to be performed
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class MissingPart extends Part implements Serializable, MekHqXmlSerializable, IPartWork, IAcquisitionWork {

	/**
	 * 
	 */
	private static final long serialVersionUID = 300672661487966982L;
	
	protected boolean checkedToday;
	
	public MissingPart(int tonnage) {
		super(tonnage);
		this.checkedToday = false;
	}
	
	@Override
	public long getCurrentValue() {
		//missing parts aren't worth a thing
		return 0;
	}
	
	@Override
	public boolean isSalvaging() {
		return false;
	}
	
	@Override
	public String getStatus() {
		return "Destroyed";
	}
	
	@Override 
	public boolean isSamePartTypeAndStatus(Part part) {
		//missing parts should always return false
		return false;
	}
	
	public String getDesc() {
		String bonus = getAllMods().getValueAsString();
		if (getAllMods().getValue() > -1) {
			bonus = "+" + bonus;
		}
		bonus = "(" + bonus + ")";
		String toReturn = "<html><font size='2'";
		String scheduled = "";
		if (getTeamId() != -1) {
			scheduled = " (scheduled) ";
		}
	
		//if (this instanceof ReplacementItem
		//		&& !((ReplacementItem) this).hasPart()) {
		//	toReturn += " color='white'";
		//}
		toReturn += ">";
		toReturn += "<b>Replace " + getName() + "</b><br/>";
		toReturn += getDetails() + "<br/>";
		toReturn += "" + getTimeLeft() + " minutes" + scheduled;
		toReturn += ", " + SupportTeam.getRatingName(getSkillMin());
		toReturn += " " + bonus;
		if (getMode() != Modes.MODE_NORMAL) {
			toReturn += "<br/><i>" + getCurrentModeName() + "</i>";
		}
		toReturn += "</font></html>";
		return toReturn;
	}
	
	@Override
	public String succeed() {	
		fix();
		return " <font color='green'><b> replaced.</b></font>";
	}
	
	@Override 
	public void fix() {
		Part replacement = findReplacement();
		if(null != replacement) {
			unit.addPart(replacement);
			remove(false);
			//assign the replacement part to the unit			
			replacement.updateConditionFromPart();
		}
	}
	
	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
			unit.campaign.removePart(this);
			unit.removePart(this);
		}	
		setUnit(null);
	}
	
	public abstract boolean isAcceptableReplacement(Part part);
	
	public Part findReplacement() {
		if(null != unit) {
			for(Part part : unit.campaign.getSpareParts()) {
				if(isAcceptableReplacement(part)) {
					return part;
				}
			}
		}
		return null;
	}
	
	public boolean isReplacementAvailable() {
		return null != findReplacement();
	}
	
	@Override
    public String getDetails() {
		if(isReplacementAvailable()) {
			return "Replacement part avaiable";
		} else {
			return "Replacement part not available";
		}
    }
	
	@Override
	public boolean needsFixing() {
		//missing parts always need fixing
		if(null != unit) {
			return !unit.isSalvage() && unit.isRepairable();
		}
		return false;
	}
	
	@Override
	public Part getMissingPart() {
		//do nothing - this should never be accessed
		return null;
	}
	
	@Override
	public void updateConditionFromPart() {
		//do nothing
	}
	
	@Override
	public void updateConditionFromEntity() {
		//do nothing
	}
	
	@Override
	public String fail(int rating) {
		skillMin = ++rating;
		timeSpent = 0;
		if(skillMin >= SupportTeam.EXP_NUM) {
			Part part = findReplacement();
			if(null != part && null != unit) {
				unit.campaign.removePart(part);
				skillMin = SupportTeam.EXP_GREEN;
			}
			return " <font color='red'><b> failed and part destroyed.</b></font>";
		} else {
			return " <font color='red'><b> failed.</b></font>";
		}
	}
	
	@Override
	public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        // Faction and Tech mod
        int factionMod = 0;
        if (null != unit && unit.campaign.getCampaignOptions().useFactionModifiers()) {
        	factionMod = Availability.getFactionAndTechMod(this, unit.campaign);
        }   
        //availability mod
        int avail = getAvailability(unit.campaign.getEra());
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + EquipmentType.getRatingName(avail) + ")");
        if(factionMod != 0) {
     	   target.addModifier(factionMod, "faction");
        }
        return target;
    }
	
	@Override 
	public boolean hasCheckedToday() {
		return checkedToday;
	}
	
	@Override
	public void setCheckedToday(boolean b) {
		this.checkedToday = b;
	}
	
	@Override
	public String getAcquisitionDesc() {
		String bonus = getAllAcquisitionMods().getValueAsString();
		if(getAllAcquisitionMods().getValue() > -1) {
			bonus = "+" + bonus;
		}
		bonus = "(" + bonus + ")";
		String toReturn = "<html><font size='2'";
		
		toReturn += ">";
		toReturn += "<b>" + getName() + "</b> " + bonus + "<br/>";
		toReturn += Utilities.getCurrencyString(getPurchasePrice()) + "<br/>";
		toReturn += "</font></html>";
		return toReturn;
	}
	
	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<checkedToday>"
				+checkedToday
				+"</checkedToday>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("checkedToday")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					checkedToday = true;
				} else {
					checkedToday = false;
				}
			} 
		}
	}
	
}

