/*
 * LargeCraftCoolingSystem.java
 * 
 * Copyright (C) 2019, MegaMek team
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

import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.Mounted;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 * Container for DS/JS/WS/SS heat sinks. Eliminates need for tracking hundreds/thousands
 * of individual heat sink parts for large spacecraft.
 * 
 * The remove action adds a single heatsink of the appropriate type to the warehouse.
 * Fix action replaces one. 
 * Large craft don't actually track damage to heatsinks, so you only fix this part if you're salvaging/replacing.
 * There might be 12,500 heatsinks in here. Have fun with that.
 * @author MKerensky
 */
public class LargeCraftCoolingSystem extends Part {
    
    /**
     * 
     */
    private static final long serialVersionUID = -5530683467894875423L;
    
    private int sinkType;
    private int sinksNeeded;
    private int currentSinks;
    private int engineSinks;
    private int removableSinks;
    private int totalSinks;
	
	public LargeCraftCoolingSystem() {
    	this(0, 0, 0, null);
    }
    
    public LargeCraftCoolingSystem(int tonnage, int engineSinks, int totalSinks, Campaign c) {
        super(tonnage, c);
        this.name = "Spacecraft Cooling System";
        this.engineSinks = engineSinks;
        this.totalSinks = totalSinks;
    }
        
    public LargeCraftCoolingSystem clone() {
    	LargeCraftCoolingSystem clone = new LargeCraftCoolingSystem(0, engineSinks, totalSinks, campaign);
        clone.copyBaseData(this);
    	return clone;
    }
    
	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
	    if(null != unit && unit instanceof Aero) {
            totalSinks = ((Aero) unit.getEntity()).getHeatSinks();
            if(null != mounted) {
                capacity = mounted.getAmmoCapacity();
                type = mounted.getType();
                if(mounted.isMissing() || mounted.isDestroyed()) {
                    mounted.setShotsLeft(0);
                    return;
                }
                shotsNeeded = getFullShots() - mounted.getBaseShotsLeft();
            }
        }
	}
	
	@Override 
	public int getBaseTime() {
	    if (isSalvaging()) {
            return 120;
        }
	    return 90;
	}
	
	@Override
	public int getDifficulty() {
	    if(isSalvaging()) {
            return -2;
        }
        return -1;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setCICHits(hits);
		}
		
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setCICHits(0);
		}
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setCICHits(3);
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
		return new MissingCIC(getUnitTonnage(), cost, campaign);
	}

	@Override
	public String checkFixable() {
	    if (isSalvaging()) {
            // FCS/CIC computers are designed for and built into the ship. Can't salvage and use somewhere else
            return "You cannot salvage a spacecraft FCS. You must scrap it instead.";
        }
		return null;
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public Money getStickerPrice() {
		calculateCost();
		return cost;
	}

	public void calculateCost() {
		if(null != unit) {
		    // There's more to CIC than just Fire Control
		    // Use Bridge + Computer + FC Computer + Gunnery Control System costs, p158 SO.
		    cost = Money.of(200000 + (10 * unit.getEntity().getWeight()) + 200000 + 100000 + (10000 * ((Jumpship)unit.getEntity()).getArcswGuns()));
		}
	}
	
	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public boolean isSamePartType(Part part) {
		return part instanceof LargeCraftCoolingSystem && cost == part.getStickerPrice();
	}
	
	@Override
	public boolean isRightTechType(String skillType) {
	    return skillType.equals(SkillType.S_TECH_VESSEL);
	}
	
	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<cost>"
				+cost.toXmlString()
				+"</cost>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);		
			if (wn2.getNodeName().equalsIgnoreCase("cost")) {
				cost = Money.fromXmlString(wn2.getTextContent().trim());
			} 
		}
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
	    return TA_GENERIC;
	}
	
	
}
