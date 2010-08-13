/*
 * LocationSalvage.java
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

package mekhq.campaign.work;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.CriticalSlot;
import megamek.common.IArmorState;
import megamek.common.Mech;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class LocationSalvage extends SalvageItem {
	private static final long serialVersionUID = 168319348571646202L;
	protected int loc;

	public LocationSalvage() {
		this(null, 0);
	}
   
    public LocationSalvage(Unit unit, int i) {
        super(unit);
        this.time = 240;
        this.difficulty = 3;
        this.loc = i;
  
        reCalc();
    }
  
    @Override
    public void reCalc() {
        if (unit == null)
        	return;
        
        this.name = "Salvage " + unit.getEntity().getLocationName(loc);
        super.reCalc();
    }
    
    public int getLoc() {
        return loc;
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof LocationSalvage
                && ((LocationSalvage)task).getUnitId() == this.getUnitId()
                && ((LocationSalvage)task).getLoc() == this.getLoc());
    }

    @Override
    public ReplacementItem getReplacement() {
        return new LocationReplacement(unit, loc);
    }

    @Override
    public Part getPart() {
        return new MekLocation(true, loc, (int) unit.getEntity().getWeight(), unit.getEntity().getStructureType(), unit.hasTSM());
    }
    
    @Override
    public String checkFixable() {
         //cant salvage torsos until arms and legs are gone
        if(unit.getEntity() instanceof Mech && loc == Mech.LOC_RT && !unit.getEntity().isLocationBad(Mech.LOC_RARM)) {
            return "must salvage/scrap right arm first";
        }
        if(unit.getEntity() instanceof Mech && loc == Mech.LOC_LT && !unit.getEntity().isLocationBad(Mech.LOC_LARM)) {
            return "must salvage/scrap left arm first";
        } 
        //you can only salvage a location that has nothing left on it
        for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
            // ignore empty & non-hittable slots
            if ((slot == null) || !slot.isEverHittable()) {
                continue;
            }
            //certain other specific crits need to be left out (uggh, must be a better way to do this!)
            if(slot.getType() == CriticalSlot.TYPE_SYSTEM 
                    && (slot.getIndex() == Mech.SYSTEM_COCKPIT
                          || slot.getIndex() == Mech.ACTUATOR_HIP
                          || slot.getIndex() == Mech.ACTUATOR_SHOULDER)) {
                continue;
            }
            if (slot.isRepairable()) {
                return "Repairable parts in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first.";
            } 
        }
        return super.checkFixable();
    }

    @Override
    public void removePart() {
       unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<loc>"
				+loc
				+"</loc>");
		writeToXmlEnd(pw1, indent, id);
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("loc")) {
				loc = Integer.parseInt(wn2.getTextContent());
			}
		}
		
		super.loadFieldsFromXmlNode(wn);
	}
}
