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
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.TechConstants;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class EnginePart extends Part {
	private static final long serialVersionUID = -6961398614705924172L;
	protected Engine engine;

	public EnginePart() {
		this(0, new Engine(0, 0, -1));
	}

	public EnginePart(int tonnage, Engine e) {
		super(tonnage);
		this.engine = e;
		this.name = engine.getEngineName() + " Engine";
	}
	
	public EnginePart clone() {
		return new EnginePart(0, engine);
	}

	public Engine getEngine() {
		return engine;
	}
	
	@Override
	public double getTonnage() {
		if(null != unit) {
			return engine.getWeightEngine(unit.getEntity());
		}
		return 0;
	}
	
	@Override 
	public long getCurrentValue() {
		return (long)Math.round(getEngine().getBaseCost() * getEngine().getRating() * getUnitTonnage() / 75.0);
	}

	@Override
	public boolean isSamePartTypeAndStatus(Part part) {
		if(needsFixing() || part.needsFixing()) {
    		return false;
    	}
		return part instanceof EnginePart
				&& getName().equals(part.getName())
				&& getEngine().getEngineType() == ((EnginePart) part)
						.getEngine().getEngineType()
				&& getEngine().getRating() == ((EnginePart) part).getEngine()
						.getRating()
				&& getEngine().getTechType() == ((EnginePart) part).getEngine()
						.getTechType();
	}

	@Override
	public int getPartType() {
		return PART_TYPE_MEK_ENGINE;
	}

	@Override
	public int getTech() {
		if (getEngine().getTechType() < 0
				|| getEngine().getTechType() >= TechConstants.SIZE)
			return TechConstants.T_IS_TW_NON_BOX;
		else
			return getEngine().getTechType();
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		// The engine is a MM object...
		// And doesn't support XML serialization...
		// But it's defined by 3 ints. So we'll save those here.
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<engineType>"
				+ engine.getEngineType() + "</engineType>");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<engineRating>"
				+ engine.getRating() + "</engineRating>");
		// TODO: Modify MM to get access to engine flags.
		// Without those flags, the engine has a good chance of being loaded wrong!
		/*
		 * pw1.println(MekHqXmlUtil.indentStr(indent+1)
		 * +"<engineFlags>"
		 * +engine.getFlags()
		 * +"</engineFlags>");
		 */
		writeToXmlEnd(pw1, indent, id);
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
		case Engine.COMBUSTION_ENGINE:
			return EquipmentType.RATING_C;
		case Engine.FUEL_CELL:
		case Engine.FISSION:
			return EquipmentType.RATING_D;
		case Engine.XL_ENGINE:	
		case Engine.LIGHT_ENGINE:
		case Engine.COMPACT_ENGINE:
			return EquipmentType.RATING_E;
		case Engine.XXL_ENGINE:
			return EquipmentType.RATING_F;
		default:
			return EquipmentType.RATING_D;
		}
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit) {
			unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE);
			if(unit.getEntity() instanceof Aero) {
				((Aero)unit.getEntity()).setEngineHits(0);
			}
			if(unit.getEntity() instanceof Tank) {
				((Tank)unit.getEntity()).engineFix();
			}
		}
	}

	@Override
	public Part getMissingPart() {
		return new MissingEnginePart(getUnitTonnage(), getEngine());
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
			unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE);
			if(unit.getEntity() instanceof Aero) {
				((Aero)unit.getEntity()).setEngineHits(((Aero)unit.getEntity()).getMaxEngineHits());
			}
			if(unit.getEntity() instanceof Tank) {
				((Tank)unit.getEntity()).engineHit();
			}
			if(!salvage) {
				unit.campaign.removePart(this);
			}
			unit.removePart(this);
			Part missing = getMissingPart();
			unit.campaign.addPart(missing);
			unit.addPart(missing);
		}
		setUnit(null);	
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit) {
			int engineHits = 0;
			int engineCrits = 0;
			Entity entity = unit.getEntity();
			for (int i = 0; i < entity.locations(); i++) {
				engineHits += entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.SYSTEM_ENGINE, i);
				engineCrits += entity.getNumberOfCriticals(
						CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i);
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
			if(engineHits >= engineCrits) {
				remove(false);
				return;
			} 
			else if(engineHits > 0) {
				hits = engineHits;
			} else {
				hits = 0;
			}
			this.time = 0;
			this.difficulty = 0;
			if (hits == 1) {
	            this.time = 100;
	            this.difficulty = -1;
	        } else if (hits == 2) {
	            this.time = 200;
	            this.difficulty = 0;
	        } else if (hits > 2) {
	            this.time = 300;
	            this.difficulty = 2;
	        }
	        if(unit.getEntity() instanceof Aero && hits > 0) {
	        	this.time = 300;
	        	this.difficulty = 1;
	        }
			if(isSalvaging()) {
				this.time = 360;
				this.difficulty = -1;
			}
		}		
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			if(hits == 0) {
				unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE);
				if(unit.getEntity() instanceof Aero) {
					((Aero)unit.getEntity()).setEngineHits(0);
				}
				if(unit.getEntity() instanceof Tank) {
					((Tank)unit.getEntity()).engineFix();
				}
			} else {
				for(int i = 0; i < hits; i++) {
					unit.hitSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE);
				}
				if(unit.getEntity() instanceof Aero) {
					((Aero)unit.getEntity()).setEngineHits(hits);
				}
				if(unit.getEntity() instanceof Tank) {
					((Tank)unit.getEntity()).engineHit();
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
			 if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i) > 0
					 && unit.isLocationDestroyed(i)) {
				 return unit.getEntity().getLocationName(i) + " is destroyed.";
			 }
		 }
		 return null;
	 }
}
