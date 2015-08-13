/*
 * EnginePart.java
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
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.verifier.TestEntity;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class EnginePart extends Part {
	private static final long serialVersionUID = -6961398614705924172L;
	protected Engine engine;
	protected boolean forHover;

	public EnginePart() {
		this(0, new Engine(0, 0, -1), null, false);
	}

	public EnginePart(int tonnage, Engine e, Campaign c, boolean hover) {
		super(tonnage, c);
		this.engine = e;
		this.forHover = hover;
		this.name = engine.getEngineName() + " Engine";
	}

	public EnginePart clone() {
		EnginePart clone = new EnginePart(getUnitTonnage(), new Engine(engine.getRating(), engine.getEngineType(), engine.getFlags()), campaign, forHover);
        clone.copyBaseData(this);
		return clone;
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
        // hover have a minimum weight of 20%
        if (forHover) {
            return Math.max(TestEntity.ceilMaxHalf(getUnitTonnage()/5, TestEntity.CEIL_HALFTON), toReturn);
        }
        return toReturn;
	}

	@Override
	public long getStickerPrice() {
		return (long)Math.round((getEngine().getBaseCost()/75.0) * getEngine().getRating() * getUnitTonnage());
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
	public boolean isSamePartType(Part part) {
		return part instanceof EnginePart
				&& getName().equals(part.getName())
				&& getEngine().getEngineType() == ((EnginePart) part)
						.getEngine().getEngineType()
				&& getEngine().getRating() == ((EnginePart) part).getEngine()
						.getRating()
				&& getEngine().getTechType() == ((EnginePart) part).getEngine()
						.getTechType()
				&& getEngine().hasFlag(Engine.TANK_ENGINE) == ((EnginePart) part).getEngine().hasFlag(Engine.TANK_ENGINE)
				&& getUnitTonnage() == ((EnginePart) part).getUnitTonnage()
				&& getTonnage() == ((EnginePart)part).getTonnage();
	}

	@Override
	public int getTechLevel() {
		if (getEngine().getTechType() < 0
				|| getEngine().getTechType() >= TechConstants.SIZE)
			return TechConstants.T_TECH_UNKNOWN;
		else
			return getEngine().getTechType();
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
			} else if (wn2.getNodeName().equalsIgnoreCase("forHover")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					forHover = true;
				} else {
					forHover = false;
				}
			}
		}

		engine = new Engine(engineRating, engineType, engineFlags);
	}

	@Override
	public int getAvailability(int era) {
		//TODO: this needs to be updated once we get DA era in
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
	public void fix() {
		super.fix();
		if(null != unit) {
		    if(unit.getEntity() instanceof Mech) {
		        unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE);
		    }
			if(unit.getEntity() instanceof Aero) {
				((Aero)unit.getEntity()).setEngineHits(0);
			}
			if(unit.getEntity() instanceof Tank) {
				((Tank)unit.getEntity()).engineFix();
			}
			if(unit.getEntity() instanceof Protomech) {
                ((Protomech)unit.getEntity()).setEngineHit(false);
            }
		}
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingEnginePart(getUnitTonnage(), new Engine(engine.getRating(), engine.getEngineType(), engine.getFlags()), campaign, forHover);
	}

	@Override
	public void remove(boolean salvage) {
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
	}

	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		if(null != unit) {
			int engineHits = 0;
			int engineCrits = 0;
			Entity entity = unit.getEntity();
			if(unit.getEntity() instanceof Mech) {
    			for (int i = 0; i < entity.locations(); i++) {
    				engineHits += entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,
    						Mech.SYSTEM_ENGINE, i);
    				engineCrits += entity.getNumberOfCriticals(
    						CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i);
    			}
			}
			if(unit.getEntity() instanceof Aero) {
				engineHits = ((Aero)unit.getEntity()).getEngineHits();
				engineCrits = 3;
			}
			if(unit.getEntity() instanceof Tank) {
				engineCrits = 2;
				if(((Tank)unit.getEntity()).isEngineHit()) {
					engineHits = 1;
				}
			}
			if(unit.getEntity() instanceof Protomech) {
			    engineCrits = 1;
			    if(unit.getEntity().getInternal(Protomech.LOC_TORSO) == IArmorState.ARMOR_DESTROYED) {
			        engineHits = 1;
			    } else {
			    	engineHits = ((Protomech)unit.getEntity()).getEngineHits();
			    }
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
	}
	
	@Override 
	public int getBaseTime() {
		//TODO: keep an aero flag here, so we dont need the unit
		if(null != unit && unit.getEntity() instanceof Aero && hits > 0) {
			return 300;
		}
		if(isSalvaging()) {
			return 360;
		}
		if (hits == 1) {
			return 100;
		} else if (hits == 2) {
			return 200;
		} else if (hits > 2) {
			return 300;
		}
		return 0;
	}
	
	@Override
	public int getDifficulty() {
		//TODO: keep an aero flag here, so we dont need the unit
		if(null != unit && unit.getEntity() instanceof Aero && hits > 0) {
			return 1;
		}
		if(isSalvaging()) {
			return -1;
		}
		if (hits == 1) {
			return -1;
		} else if (hits == 2) {
			return 0;
		} else if (hits > 2) {
			return 2;
		}
		return 0;
	}


	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			if(hits == 0) {
				if(unit.getEntity() instanceof Mech) {
				    unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE);
				}
				if(unit.getEntity() instanceof Aero) {
					((Aero)unit.getEntity()).setEngineHits(0);
				}
				if(unit.getEntity() instanceof Tank) {
					((Tank)unit.getEntity()).engineFix();
				}
				if(unit.getEntity() instanceof Protomech) {
	                ((Protomech)unit.getEntity()).setEngineHit(false);
	            }
			} else {
			    if(unit.getEntity() instanceof Mech) {
			        unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, hits);
			    }
				if(unit.getEntity() instanceof Aero) {
					((Aero)unit.getEntity()).setEngineHits(hits);
				}
				if(unit.getEntity() instanceof Tank) {
					((Tank)unit.getEntity()).engineHit();
				}
				if(unit.getEntity() instanceof Protomech) {
	                ((Protomech)unit.getEntity()).setEngineHit(true);
	            }
			}
		}
	}

	@Override
	 public String checkFixable() {
		if(isSalvaging()) {
			return null;
		}
		 for(int i = 0; i < unit.getEntity().locations(); i++) {
			 if(unit.isLocationBreached(i)) {
				 return unit.getEntity().getLocationName(i) + " is breached.";
			 }
			 if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i) > 0
					 && unit.isLocationDestroyed(i)) {
				 return unit.getEntity().getLocationName(i) + " is destroyed.";
			 }
		 }
		 return null;
	 }

	@Override
	public boolean isMountedOnDestroyedLocation() {
		if(null == unit) {
			return false;
		}
		for(int i = 0; i < unit.getEntity().locations(); i++) {
			 if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i) > 0
					 && unit.isLocationDestroyed(i)) {
				 return true;
			 }
		 }
		return false;
	}

	 @Override
	 public String getDetails() {
		 if(null != unit) {
			 return super.getDetails();
		 }
		 String hvrString = "";
		 if(forHover) {
			 hvrString = " (hover)";
		 }
		 return super.getDetails() + ", " + getUnitTonnage() + " tons" + hvrString;
	 }

	 @Override
	 public boolean isPartForEquipmentNum(int index, int loc) {
		 return Mech.SYSTEM_ENGINE == index;
	 }

	 @Override
		public boolean isRightTechType(String skillType) {
		 	if(getEngine().hasFlag(Engine.TANK_ENGINE)) {
				return skillType.equals(SkillType.S_TECH_MECHANIC);
		 	}
		 	else {
				return skillType.equals(SkillType.S_TECH_MECH) || skillType.equals(SkillType.S_TECH_AERO);
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
