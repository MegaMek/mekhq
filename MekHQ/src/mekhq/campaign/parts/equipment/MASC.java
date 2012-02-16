/*
 * MASC.java
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

package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Engine;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MASC extends EquipmentPart {
	private static final long serialVersionUID = 2892728320891712304L;

	protected int engineRating;
	
	public MASC() {
    	this(0, null, -1, null, 0);
    }
    
    public MASC(int tonnage, EquipmentType et, int equipNum, Campaign c, int rating) {
        super(tonnage, et, equipNum, c);
        this.engineRating = rating;
        equipTonnage = calculateTonnage();
    }
    
    public MASC clone() {
    	MASC clone = new MASC(getUnitTonnage(), getType(), getEquipmentNum(), campaign, engineRating);
    	clone.hits = this.hits;
    	clone.time = this.time;
    	return clone;
    }
 
    public void setUnit(Unit u) {
    	super.setUnit(u);
    	if(null != unit && null != unit.getEntity().getEngine()) {
    		engineRating = unit.getEntity().getEngine().getRating();
    	}
    }
    
    private double calculateTonnage() {
    	if(null == type) {
    		return 0;
    	}
    	//supercharger tonnage will need to be set by hand in parts store
        if (TechConstants.isClan(type.getTechLevel())) {
            return Math.round(getUnitTonnage() / 25.0f);
        }
        return Math.round(getUnitTonnage() / 20.0f);
    }
    
    @Override
    public long getStickerPrice() {
    	if (isSupercharger()) {
    		return engineRating * 10000;
    	} else {           
            return (long)(engineRating * getTonnage() * 1000);
        }
    }
    
    public int getEngineRating() {
    	return engineRating;
    }
    
    private boolean isSupercharger() {
    	return type.hasSubType(MiscType.S_SUPERCHARGER);
    }
    
    
    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
    	if(needsFixing() || part.needsFixing()) {
    		return false;
    	}
        return part instanceof MASC
        		&& getType().equals(((EquipmentPart)part).getType())
        		&& getTonnage() == part.getTonnage()
        		&& getEngineRating() == ((MASC)part).getEngineRating();
    }

    
    @Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);		
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<equipmentNum>"
				+equipmentNum
				+"</equipmentNum>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<typeName>"
				+type.getInternalName()
				+"</typeName>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<equipTonnage>"
				+equipTonnage
				+"</equipTonnage>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<engineRating>"
				+engineRating
				+"</engineRating>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("equipmentNum")) {
				equipmentNum = Integer.parseInt(wn2.getTextContent());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
				typeName = wn2.getTextContent();
			}
			else if (wn2.getNodeName().equalsIgnoreCase("equipTonnage")) {
				equipTonnage = Double.parseDouble(wn2.getTextContent());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("engineRating")) {
				engineRating = Integer.parseInt(wn2.getTextContent());
			}
		}
		restore();
	}
	
	@Override
	public Part getMissingPart() {
		return new MissingMASC(getUnitTonnage(), type, equipmentNum, campaign, equipTonnage, engineRating);
	}
	
	@Override
	public String getDetails() {
		if(null != unit) {
			return super.getDetails();
		}
		if(isSupercharger()) {
			return super.getDetails() + ", " + getEngineRating() + " rating";
		}
		return super.getDetails() + ", " + getUnitTonnage() + " tons, " + getEngineRating() + " rating";
	 }
	
	@Override
    public boolean isOmniPoddable() {
    	return false;
    }
}