/*
 * SpacecraftEngine.java
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

import megamek.common.Aero;
import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.SmallCraft;
import megamek.common.TechConstants;
import megamek.common.Warship;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class SpacecraftEngine extends Part {
	private static final long serialVersionUID = -6961398614705924172L;
	double engineTonnage;  
	boolean clan;
	
	public SpacecraftEngine() {
		this(0, 0, null, false);
	}

	public SpacecraftEngine(int tonnage, double etonnage, Campaign c, boolean clan) {
		super(tonnage, c);
		this.engineTonnage = etonnage;
		this.clan = clan;
		this.name = "Spacecraft Engine";
	}
	
	public SpacecraftEngine clone() {
		SpacecraftEngine clone = new SpacecraftEngine(getUnitTonnage(), engineTonnage, campaign, clan);
        clone.copyBaseData(this);
		return clone;
	}
	
	@Override
	public double getTonnage() {
		return engineTonnage;
	}
	
	public void calculateTonnage() {
		
		if(null != unit) {
			clan = unit.getEntity().isClan();
			if(unit.getEntity() instanceof SmallCraft) {
				float moveFactor = unit.getEntity().getWeight() * unit.getEntity().getOriginalWalkMP();
				if(clan) {
					engineTonnage = Math.round(moveFactor * 0.061 * 2)/2f;
				} else {
					engineTonnage = Math.round(moveFactor * 0.065 * 2)/2f;
				}
			} else if(unit.getEntity() instanceof Jumpship) {
				if(unit.getEntity() instanceof Warship) {
					engineTonnage = Math.round(unit.getEntity().getWeight() * 0.06 *  unit.getEntity().getOriginalWalkMP() * 2)/2f;
				} else {
					engineTonnage = Math.round(unit.getEntity().getWeight() * 0.012 * 2)/2f;
				}
			}
		}
	}
	
	@Override 
	public long getStickerPrice() {
		return (long)Math.round(engineTonnage * 1000);
	}
	
	@Override
	public boolean isSamePartType(Part part) {
		return part instanceof SpacecraftEngine
				&& getName().equals(part.getName())
				&& getTonnage() == ((SpacecraftEngine)part).getTonnage();
	}

	@Override
	public int getTechLevel() {
		if(clan) {
			return TechConstants.T_CLAN_TW;
		} else {
			return TechConstants.T_IS_TW_NON_BOX;
		}
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		// The engine is a MM object...
		// And doesn't support XML serialization...
		// But it's defined by 3 ints. So we'll save those here.
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<engineTonnage>"
				+engineTonnage
				+"</engineTonnage>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<clan>"
				+clan
				+"</clan>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("engineTonnage")) {
				engineTonnage = Double.parseDouble(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					clan = true;
				} else {
					clan = false;
				}
			} 
		}
	}

	@Override
	public int getAvailability(int era) {
		if(era == EquipmentType.ERA_SL) {
			return EquipmentType.RATING_C;
		} else if(era == EquipmentType.ERA_SW) {
			return EquipmentType.RATING_E;
		} else {
			return EquipmentType.RATING_C;
		}
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_D;
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit) {
			if(unit.getEntity() instanceof Aero) {
				((Aero)unit.getEntity()).setEngineHits(0);
			}
		}
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingSpacecraftEngine(getUnitTonnage(), engineTonnage, campaign, clan);
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
			unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE);
			if(unit.getEntity() instanceof Aero) {
				((Aero)unit.getEntity()).setEngineHits(((Aero)unit.getEntity()).getMaxEngineHits());
			}
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
		setSalvaging(false);
		setUnit(null);
		updateConditionFromEntity();
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit) {
			int engineHits = 0;
			int engineCrits = 0;
			if(unit.getEntity() instanceof Aero) {
				engineHits = ((Aero)unit.getEntity()).getEngineHits();
				engineCrits = 3;
			}
			if(engineHits >= engineCrits) {
				remove(false);
				return;
			} 
			else if(engineHits > 0) {
				hits = engineHits;
			} else {
				hits = 0;
			}
		}
		this.time = 0;
		this.difficulty = 0;
		if (hits > 0) {
			this.time = 300;
			this.difficulty = 1;
		}
		if(isSalvaging()) {
			this.time = 43200;
			this.difficulty = 1;
		}	
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			if(unit.getEntity() instanceof Aero) {
				((Aero)unit.getEntity()).setEngineHits(hits);
			}
		}
	}
	
	@Override
	 public String checkFixable() {
		 return null;
	 }
	
	@Override
	public boolean isMountedOnDestroyedLocation() {
		return false;
	}
	 
	 @Override
	 public boolean isRightTechType(String skillType) {
		 return skillType.equals(SkillType.S_TECH_AERO);	
	 }
}
