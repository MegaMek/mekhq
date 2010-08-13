/*
 * MekActuatorRepair.java
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
import megamek.common.Mech;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekActuatorRepair extends RepairItem {
	private static final long serialVersionUID = -3060594760170107991L;
	protected int loc;
    protected int type;

	public MekActuatorRepair() {
		this(null, 0, 0, 0);
	}
   
    public MekActuatorRepair(Unit unit, int h, int i, int t) {
        super(unit, h);
        this.loc = i;
        this.type = t;
        this.time = 120;
        this.difficulty = 0;
        reCalc();
    }
    
    @Override
    public void reCalc() {
        if (unit == null)
        	return;
        
        this.name = "Repair " + ((Mech)unit.getEntity()).getSystemName(type) + " Actuator";

        super.reCalc();
    }

    @Override
    public String getDetails() {
        return unit.getEntity().getLocationName(loc);
    }
    
    @Override
    public String checkFixable() {
        if(unit.isLocationDestroyed(loc)) {
            return unit.getEntity().getLocationName(loc) + " is destroyed.";
        }
        return super.checkFixable();
    }
    
    public int getLoc() {
        return loc;
    }
    
    public int getType() {
        return type;
    }
    
    @Override
    public void fix() {
        unit.repairSystem(CriticalSlot.TYPE_SYSTEM, type, loc);
    }

    @Override
    public void doReplaceChanges() {
        removeSalvage();
        unit.destroySystem(CriticalSlot.TYPE_SYSTEM, type, loc);
    }

    @Override
    public WorkItem getReplacementTask () {
        return new MekActuatorReplacement(unit, loc, type);
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof MekActuatorRepair
                && ((MekActuatorRepair)task).getUnitId() == this.getUnitId()
                && ((MekActuatorRepair)task).getLoc() == this.getLoc()
                && ((MekActuatorRepair)task).getType() == this.getType());
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+ "<loc>"
				+ loc
				+ "</loc>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+ "<type>"
				+ type
				+ "</type>");
		writeToXmlEnd(pw1, indent, id);
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("loc")) {
				loc = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("type")) {
				type = Integer.parseInt(wn2.getTextContent());
			}

		}
		
		super.loadFieldsFromXmlNode(wn);
	}
}
