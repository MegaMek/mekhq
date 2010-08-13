/*
 * InternalRepair.java
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

import megamek.common.IArmorState;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class InternalRepair extends RepairItem {
	private static final long serialVersionUID = 5772050322556439372L;
	int loc;
    
    public InternalRepair(Unit unit, int i) {
        super(unit, 0);
        this.loc = i;
        this.name = "Repair Internal Structure";
        reCalc();
    }
    
    @Override
    public void reCalc() {
    	// Do nothing.
    	super.reCalc();
    }

    @Override
    public String getDetails() {
        return unit.getEntity().getLocationName(loc);
    }
        
    @Override
    public void fix() {
        unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);
    }

    @Override
    public void doReplaceChanges() {
        removeSalvage();
        unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
    }

    @Override
    public WorkItem getReplacementTask() {
        return new LocationReplacement(unit, loc);
    }
    
    public int getLoc() {
        return loc;
    }
    
    @Override
    public boolean canScrap() {
        return false;
    }

	protected void writeToXmlBegin(PrintWriter pw1, int indent, int id) {
		super.writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<loc>"
				+loc
				+"</loc>");
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
