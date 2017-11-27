/*
 * DropshipDockingCollar.java
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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class DropshipDockingCollar extends Part {

	/**
	 * 
	 */
	private static final long serialVersionUID = -717866644605314883L;
	
    static final TechAdvancement TA_BOOM = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2458, 2470, 2500).setPrototypeFactions(F_TH)
            .setProductionFactions(F_TH).setTechRating(RATING_C)
            .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    static final TechAdvancement TA_NO_BOOM = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2304, 2350, 2364, 2520).setPrototypeFactions(F_TA)
            .setProductionFactions(F_TH).setTechRating(RATING_B)
            .setAvailability(RATING_C, RATING_X, RATING_X, RATING_X)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    
    private int collarType = Dropship.COLLAR_STANDARD;
    private boolean boomDamaged = false;
	
	public DropshipDockingCollar() {
    	this(0, null, Dropship.COLLAR_STANDARD);
    }
    
    public DropshipDockingCollar(int tonnage, Campaign c, int collarType) {
        super(tonnage, c);
        this.name = "Dropship Docking Collar";
        if (collarType == Dropship.COLLAR_NO_BOOM) {
            name += " (No Boom)";
        } else if (collarType == Dropship.COLLAR_PROTOTYPE) {
            name += " (Prototype)";
        }
    }
    
    public DropshipDockingCollar clone() {
    	DropshipDockingCollar clone = new DropshipDockingCollar(getUnitTonnage(), campaign, collarType);
        clone.copyBaseData(this);
        clone.boomDamaged = boomDamaged;
    	return clone;
    }
    
    public int getCollarType() {
        return collarType;
    }
        
	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		int priorHits = hits;
		boolean priorBoomDamage = boomDamaged;
		if(null != unit && unit.getEntity() instanceof Dropship) {
			 if(((Dropship)unit.getEntity()).isDockCollarDamaged()) {
				 hits = 1;
			 } else { 
				 hits = 0;
			 }
			 boomDamaged = ((Dropship) unit.getEntity()).isKFBoomDamaged();
			 if(checkForDestruction 
					 && ((hits > priorHits)
					         || (boomDamaged && !priorBoomDamage))
					 && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
				 remove(false);
				 return;
			 }
		}
	}
	
	@Override 
	public int getBaseTime() {
		if(isSalvaging()) {
			return 2880;
		}
		return 120;
	}
	
	@Override
	public int getDifficulty() {
		if(isSalvaging()) {
			return -2;
		}
		return 3;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship) unit.getEntity()).setDamageDockCollar(hits > 0);
            ((Dropship) unit.getEntity()).setDamageKFBoom(boomDamaged);
		}
	}

	@Override
	public void fix() {
		super.fix();
		boomDamaged = false;
		if(null != unit && unit.getEntity() instanceof Dropship) {
			((Dropship)unit.getEntity()).setDamageDockCollar(false);
            ((Dropship)unit.getEntity()).setDamageKFBoom(false);
		}
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship)unit.getEntity()).setDamageDockCollar(true);
            ((Dropship)unit.getEntity()).setDamageKFBoom(true);
			Part spare = campaign.checkForExistingSparePart(this);
			if(!salvage) {
				campaign.removePart(this);
			} else if(null != spare) {
				spare.incrementQuantity();
				campaign.removePart(this);
			}
			unit.removePart(this);
			Part missing = getMissingPart();
			unit.addPart(missing);
			campaign.addPart(missing, 0);
		}
		setUnit(null);
		updateConditionFromEntity(false);
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingDropshipDockingCollar(getUnitTonnage(), campaign, collarType);
	}

	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public boolean needsFixing() {
		return (hits > 0) || boomDamaged;
	}

	@Override
	public long getStickerPrice() {
	    if (collarType == Dropship.COLLAR_STANDARD) {
	        return 10000;
	    } else if (collarType == Dropship.COLLAR_PROTOTYPE) {
	        return 1010000;
	    } else {
	        return 0;
	    }
	}
	
	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public boolean isSamePartType(Part part) {
		return (part instanceof DropshipDockingCollar)
		        && (collarType == ((DropshipDockingCollar)part).collarType);
	}
	
	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "collarType", collarType);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "boomDamaged", boomDamaged);
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("collarType")) {
                collarType = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("boomDamaged")) {
                boomDamaged = Boolean.parseBoolean(wn2.getTextContent());
            }
        }
	}
	
	@Override
	public boolean isRightTechType(String skillType) {
		return skillType.equals(SkillType.S_TECH_AERO);
	}

	@Override
	public String getLocationName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLocation() {
		return Entity.LOC_NONE;
	}
	
	@Override
	public TechAdvancement getTechAdvancement() {
	    if (collarType != Dropship.COLLAR_NO_BOOM) {
	        return TA_BOOM;
	    } else {
	        return TA_NO_BOOM;
	    }
	}
	
}