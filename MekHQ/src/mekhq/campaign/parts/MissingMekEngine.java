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

import megamek.common.CriticalSlot;
import megamek.common.Engine;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.TechConstants;
import mekhq.campaign.Faction;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingMekEngine extends MissingPart {
	private static final long serialVersionUID = -6961398614705924172L;
	protected Engine engine;

	public MissingMekEngine() {
		this(0, null);
	}

	public MissingMekEngine(int tonnage, Engine e) {
		super(tonnage);
		this.engine = e;
		if(null != engine) {
			this.name = engine.getEngineName() + " Engine";
		}
		this.engine = e;
		this.time = 360;
		this.difficulty = -1;
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
	public long getPurchasePrice() {
		double c = getEngine().getBaseCost() * getEngine().getRating() * getUnitTonnage() / 75.0;
		return (long) Math.round(c);
	}

	@Override
	public int getPartType() {
		return PART_TYPE_MEK_ENGINE;
	}

	@Override
	public boolean isClanTechBase() {
		String techBase = TechConstants.getTechName(getEngine().getTechType());

		if (techBase.equals("Clan"))
			return true;
		else if (techBase.equals("Inner Sphere"))
			return false;
		else
			return false;
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
	public boolean isAcceptableReplacement(Part part) {
		if(part instanceof MekEngine) {
			Engine eng = ((MekEngine)part).getEngine();
			if (null != eng) {
				return getEngine().getEngineType() == eng.getEngineType()
						&& getEngine().getRating() == eng.getRating()
						&& getEngine().getTechType() == eng.getTechType();
			}
		}
		return false;
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
		return new MekEngine(getUnitTonnage(), getEngine());
	}


}
