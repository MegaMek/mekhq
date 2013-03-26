/*
 * AeroHeatSink.java
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
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class AeroHeatSink extends Part {

	/**
	 * 
	 */
	private static final long serialVersionUID = -717866644605314883L;

	private int type;
	
	public AeroHeatSink() {
    	this(0, Aero.HEAT_SINGLE, null);
    }
    
    public AeroHeatSink(int tonnage, int type, Campaign c) {
        super(tonnage, c);
        this.name = "Aero Heat Sink";
        this.type = type;
        if(type == Aero.HEAT_DOUBLE) {
            this.name = "Aero Double Heat Sink";
        }
    }
    
    public AeroHeatSink clone() {
    	AeroHeatSink clone = new AeroHeatSink(getUnitTonnage(), type, campaign);
        clone.copyBaseData(this);
    	return clone;
    }
        
	@Override
	public void updateConditionFromEntity() {
		if(null != unit && unit.getEntity() instanceof Aero && hits == 0) {
			//ok this is really ugly, but we don't track individual heat sinks, so I have no idea of
			//a better way to do it
			int hsDamage = ((Aero)unit.getEntity()).getHeatSinkHits();
			for(Part part : unit.getParts()) {
				if(hsDamage == 0) {
					break;
				}
				else if((part instanceof AeroHeatSink && part.getHits() > 0) || part instanceof MissingAeroHeatSink) {
					hsDamage--;
				}
			}
			if(hsDamage > 0) {
				hits = 1;
			} else {
				hits = 0;
			}
		}
		if(hits > 0) {
			time = 90;
			difficulty = -1;
		} else {
			time = 0;
			difficulty = 0;
		}
		if(isSalvaging()) {
			time = 90;
			difficulty = -2;
		}
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			if(hits == 0) {
				((Aero)unit.getEntity()).setHeatSinks(((Aero)unit.getEntity()).getHeatSinks()+1);
			}
		}
		
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setHeatSinks(((Aero)unit.getEntity()).getHeatSinks()+1);
		}
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof Aero) {
			if(hits == 0) {
				((Aero)unit.getEntity()).setHeatSinks(((Aero)unit.getEntity()).getHeatSinks()-1);
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
	public MissingPart getMissingPart() {
		return new MissingAeroHeatSink(getUnitTonnage(), type, campaign);
	}

	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public long getStickerPrice() {
		if(type == Aero.HEAT_DOUBLE) {
			return 6000;
		} else {
			return 2000;
		}
	}

	@Override
	public double getTonnage() {
		return 1;
	}

	@Override
	public int getTechRating() {
		if(type == Aero.HEAT_DOUBLE) {
			return EquipmentType.RATING_D;
		} else {
			return EquipmentType.RATING_E;
		}
	}

	@Override
	public int getAvailability(int era) {
		if(type == Aero.HEAT_DOUBLE) {
		if(era == EquipmentType.ERA_SL) {
			return EquipmentType.RATING_C;
		} else if(era == EquipmentType.ERA_SW) {
			return EquipmentType.RATING_E;
		} else {
			return EquipmentType.RATING_D;
		}
		} else {
			return EquipmentType.RATING_B;
		}
	}
	
	@Override
	public int getTechLevel() {
		return TechConstants.T_IS_TW_ALL;
	}
	
	@Override 
	public int getTechBase() {
		return T_BOTH;	
	}

	@Override
	public boolean isSamePartType(Part part) {
		return part instanceof AeroHeatSink && type == ((AeroHeatSink)part).getType();
	}

	public int getType() {
		return type;
	}
	
	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("type")) {
				type = Integer.parseInt(wn2.getTextContent());
				if(type == Aero.HEAT_DOUBLE) {
		            this.name = "Aero Double Heat Sink";
		        } 
			} 
		}
	}
	
	@Override
	public boolean isRightTechType(String skillType) {
		return skillType.equals(SkillType.S_TECH_AERO);
	}
}