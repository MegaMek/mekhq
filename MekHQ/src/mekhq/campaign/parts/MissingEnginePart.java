/*
 * MissingMekEngine.java
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
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.verifier.TestEntity;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingEnginePart extends MissingPart {
	private static final long serialVersionUID = -6961398614705924172L;
	protected Engine engine;
	protected boolean forHover;

	public MissingEnginePart() {
		this(0, null, null, false);
	}

	public MissingEnginePart(int tonnage, Engine e, Campaign c, boolean hover) {
		super(tonnage, c);
		this.engine = e;
		this.forHover = hover;
		if(null != engine) {
			this.name = engine.getEngineName() + " Engine";
		}
		this.engine = e;
	}
	
	@Override 
	public int getBaseTime() {
		return 360;
	}
	
	@Override
	public int getDifficulty() {
		return -1;
	}

	public Engine getEngine() {
		return engine;
	}
	
	@Override
	public double getTonnage() {
		float weight = Engine.ENGINE_RATINGS[(int) Math.ceil(engine.getRating() / 5.0)];
        switch (engine.getEngineType()) {
            case Engine.COMBUSTION_ENGINE:
                weight *= 2.0f;
                break;
            case Engine.NORMAL_ENGINE:
                break;
            case Engine.XL_ENGINE:
                weight *= 0.5f;
                break;
            case Engine.LIGHT_ENGINE:
                weight *= 0.75f;
                break;
            case Engine.XXL_ENGINE:
                weight /= 3f;
                break;
            case Engine.COMPACT_ENGINE:
                weight *= 1.5f;
                break;
            case Engine.FISSION:
                weight *= 1.75;
                weight = Math.max(5, weight);
                break;
            case Engine.FUEL_CELL:
                weight *= 1.2;
                break;
            case Engine.NONE:
                return 0;
        }
        weight = TestEntity.ceilMaxHalf(weight, TestEntity.CEIL_HALFTON);
        if (engine.hasFlag(Engine.TANK_ENGINE) && engine.isFusion()) {
            weight *= 1.5f;
        }
        float toReturn = TestEntity.ceilMaxHalf(weight, TestEntity.CEIL_HALFTON);
        if(forHover) {
            return Math.max(TestEntity.ceilMaxHalf(getUnitTonnage()/5, TestEntity.CEIL_HALFTON), toReturn);
        }
        return toReturn;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		// The engine is a MM object...
		// And doesn't support XML serialization...
		// But it's defined by 3 ints. So we'll save those here.
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<engineType>"
				+ engine.getEngineType() + "</engineType>");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<engineRating>"
				+ engine.getRating() + "</engineRating>");
		 pw1.println(MekHqXmlUtil.indentStr(indent+1)
				 +"<engineFlags>"
				 +engine.getFlags()
				 +"</engineFlags>");
		 pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<forHover>"
					+forHover
					+"</forHover>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		int engineType = -1;
		int engineRating = -1;
		int engineFlags = 0;
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("engineType")) {
				engineType = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("engineRating")) {
				engineRating = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("engineFlags")) {
				engineFlags = Integer.parseInt(wn2.getTextContent());
			} 
		}
		
		engine = new Engine(engineRating, engineType, engineFlags);
		this.name = engine.getEngineName() + " Engine";
	}

	@Override
	public int getAvailability(int era) {
		switch(engine.getTechType()) {
		case Engine.COMBUSTION_ENGINE:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_A;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_A;
			} else {
				return EquipmentType.RATING_A;
			}
		case Engine.FUEL_CELL:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_C;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_D;
			} else {
				return EquipmentType.RATING_D;
			}
		case Engine.FISSION:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_E;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_E;
			} else {
				return EquipmentType.RATING_D;
			}
		case Engine.XL_ENGINE:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_D;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_F;
			} else {
				return EquipmentType.RATING_E;
			}
		case Engine.LIGHT_ENGINE:
		case Engine.COMPACT_ENGINE:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_X;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_X;
			} else {
				return EquipmentType.RATING_E;
			}
		case Engine.XXL_ENGINE:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_X;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_X;
			} else {
				return EquipmentType.RATING_F;
			}
		default:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_C;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_E;
			} else {
				return EquipmentType.RATING_D;
			}
		}
	}

	@Override
	public int getTechRating() {
		switch(engine.getTechType()) {
		case Engine.XL_ENGINE:
			if(engine.hasFlag(Engine.CLAN_ENGINE)) {
				return EquipmentType.RATING_F;
			}
		case Engine.LIGHT_ENGINE:
		case Engine.COMPACT_ENGINE:
			return EquipmentType.RATING_E;
		case Engine.XXL_ENGINE:
			return EquipmentType.RATING_F;
		case Engine.FUEL_CELL:
		case Engine.FISSION:
			if(engine.hasFlag(Engine.SUPPORT_VEE_ENGINE)) {
				return EquipmentType.RATING_C;
			}
		case Engine.NORMAL_ENGINE:
			return EquipmentType.RATING_D;
		case Engine.STEAM:
			return EquipmentType.RATING_A;
		case Engine.COMBUSTION_ENGINE:
			if(engine.hasFlag(Engine.SUPPORT_VEE_ENGINE)) {
				return EquipmentType.RATING_B;
			}
		default:
			return EquipmentType.RATING_C;
		}
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		if(part instanceof EnginePart) {
			Engine eng = ((EnginePart)part).getEngine();
			if (null != eng) {
				return getEngine().getEngineType() == eng.getEngineType()
						&& getEngine().getRating() == eng.getRating()
						&& getEngine().getTechType() == eng.getTechType()
						&& getUnitTonnage() == ((EnginePart)part).getUnitTonnage()
						&& getTonnage() == ((EnginePart)part).getTonnage();				
			}
		}
		return false;
	}
	
	public void fixTankFlag(boolean hover) {
		int flags = engine.getFlags();
		if(!engine.hasFlag(Engine.TANK_ENGINE)) {
			flags |= Engine.TANK_ENGINE;
		}
		engine = new Engine(engine.getRating(), engine.getEngineType(), flags);
		this.name = engine.getEngineName() + " Engine";
		this.forHover = hover;
	}
	
	public void fixClanFlag() {
		int flags = engine.getFlags();
		if(!engine.hasFlag(Engine.CLAN_ENGINE)) {
			flags |= Engine.CLAN_ENGINE;
		}
		engine = new Engine(engine.getRating(), engine.getEngineType(), flags);
		this.name = engine.getEngineName() + " Engine";
	}
	
	 @Override
	 public String checkFixable() {
		 for(int i = 0; i < unit.getEntity().locations(); i++) {
			 if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i) > 0
					 && unit.isLocationDestroyed(i)) {
				 return unit.getEntity().getLocationName(i) + " is destroyed.";
			 }
		 }
		 return null;
	 }

	@Override
	public Part getNewPart() {
		boolean useHover = null != unit && unit.getEntity().getMovementMode() == EntityMovementMode.HOVER && unit.getEntity() instanceof Tank;
		int flags = 0;
		if(engine.hasFlag(Engine.CLAN_ENGINE)) {
			flags = Engine.CLAN_ENGINE;
		}
		if(null != unit && unit.getEntity() instanceof Tank) {
			flags |= Engine.TANK_ENGINE;
		}
		return new EnginePart(getUnitTonnage(), new Engine(engine.getRating(), engine.getEngineType(), flags), campaign, useHover);
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			if(unit.getEntity() instanceof Mech) {
			    unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE);
			}
			if(unit.getEntity() instanceof Aero) {
				((Aero)unit.getEntity()).setEngineHits(((Aero)unit.getEntity()).getMaxEngineHits());
			}
			if(unit.getEntity() instanceof Tank) {
				((Tank)unit.getEntity()).engineHit();
			}
			if(unit.getEntity() instanceof Protomech) {
                ((Protomech)unit.getEntity()).setEngineHit(true);
            }
		}
	}
	
	@Override
	public String getAcquisitionName() {
		return getPartName() + ",  " + getTonnage() + " tons";
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
	public int getIntroDate() {
		switch(engine.getEngineType()) {
		case Engine.XL_ENGINE:
			if(engine.hasFlag(Engine.CLAN_ENGINE)) {
				if(engine.hasFlag(Engine.LARGE_ENGINE)) {
					return 2850;
				} else {
					return 2824;
				}
			} else {
				if(engine.hasFlag(Engine.LARGE_ENGINE)) {
					return 2635;
				} else {
					return 2556;
				}
			}
		case Engine.XXL_ENGINE:
			if(engine.hasFlag(Engine.CLAN_ENGINE)) {
				if(engine.hasFlag(Engine.LARGE_ENGINE)) {
					return 3055;
				} else {
					return 2954;
				}
			} else {
				if(engine.hasFlag(Engine.LARGE_ENGINE)) {
					return 3058;
				} else {
					return 3055;
				}
			}
		case Engine.LIGHT_ENGINE:
			if(engine.hasFlag(Engine.LARGE_ENGINE)) {
				return 3064;
			} else {
				return 3055;
			}
		case Engine.COMPACT_ENGINE:
			return 3065;
		case Engine.FUEL_CELL:
			if(!engine.hasFlag(Engine.SUPPORT_VEE_ENGINE)) {
				return 2300;
			}
		case Engine.FISSION:
			if(!engine.hasFlag(Engine.SUPPORT_VEE_ENGINE)) {
				return 2470;
			}
		case Engine.MAGLEV:
		case Engine.BATTERY:
		case Engine.SOLAR:
		case Engine.NORMAL_ENGINE:
		case Engine.COMBUSTION_ENGINE:
			if(engine.hasFlag(Engine.LARGE_ENGINE)) {
				return 2630;
			}
		case Engine.STEAM:
		default:
			return EquipmentType.DATE_NONE; 
		}		
	}

	@Override
	public int getExtinctDate() {
		switch(engine.getEngineType()) {
		case Engine.XL_ENGINE:
			if(!engine.hasFlag(Engine.CLAN_ENGINE)) {
				if(engine.hasFlag(Engine.LARGE_ENGINE)) {
					return 2822;
				} else {
					return 2865;
				}
			}
		default:
			return EquipmentType.DATE_NONE;
		}
	}

	@Override
	public int getReIntroDate() {
		switch(engine.getEngineType()) {
		case Engine.XL_ENGINE:
			if(!engine.hasFlag(Engine.CLAN_ENGINE)) {
				if(engine.hasFlag(Engine.LARGE_ENGINE)) {
					return 3054;
				} else {
					return 3035;
				}
			}
		default:
			return EquipmentType.DATE_NONE;
		}
	}


}
