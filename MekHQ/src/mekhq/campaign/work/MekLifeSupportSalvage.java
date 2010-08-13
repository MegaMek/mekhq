/*
 * MekLifeSupportSalvage.java
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

import megamek.common.CriticalSlot;
import megamek.common.Mech;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekLifeSupportSalvage extends SalvageItem {
	private static final long serialVersionUID = 5434594112352400189L;

	public MekLifeSupportSalvage() {
		this(null);
	}
	
	public MekLifeSupportSalvage(Unit unit) {
        super(unit);
        this.name = "Salvage life support";
        this.time = 180;
        this.difficulty = -1;
    }

    @Override
    public void reCalc() {
    	// Do nothing.
    	super.reCalc();
    }
    
    @Override
    public ReplacementItem getReplacement() {
        return new MekLifeSupportReplacement(unit);
    }

    @Override
    public Part getPart() {
        return new MekLifeSupport(true, (int) unit.getEntity().getWeight());
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof MekLifeSupportSalvage
                && ((MekLifeSupportSalvage)task).getUnitId() == this.getUnitId());
    }

    @Override
    public void removePart() {
         unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT);
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		writeToXmlEnd(pw1, indent, id);
	}
}
