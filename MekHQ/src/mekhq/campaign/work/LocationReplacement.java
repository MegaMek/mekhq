/*
 * LocationReplacement.java
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
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;

/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class LocationReplacement extends ReplacementItem {
	private static final long serialVersionUID = -7220042668917924970L;
	protected int loc;

	public LocationReplacement() {
		this(null, 0);
	}

	public LocationReplacement(Unit unit, int i) {
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
		
		this.name = "Replace " + unit.getEntity().getLocationName(loc);

		super.reCalc();
    }

	@Override
	public void fix() {
		super.fix();
		unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);
		// repair any hips or shoulders
		if (unit.getEntity() instanceof Mech) {
			unit.getEntity().removeCriticals(
					loc,
					new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
							Mech.ACTUATOR_HIP));
			unit.getEntity().removeCriticals(
					loc,
					new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
							Mech.ACTUATOR_SHOULDER));
		}
	}

	public int getLoc() {
		return loc;
	}

	@Override
	public String checkFixable() {
		if (unit.getEntity() instanceof Mech) {
			// cant replace appendages when corresponding torso is gone
			if (loc == Mech.LOC_LARM
					&& unit.getEntity().isLocationBad(Mech.LOC_LT)) {
				return "must replace left torso first";
			} else if (loc == Mech.LOC_RARM
					&& unit.getEntity().isLocationBad(Mech.LOC_RT)) {
				return "must replace right torso first";
			}
		}
		return super.checkFixable();
	}

	@Override
	public boolean sameAs(WorkItem task) {
		return (task instanceof LocationReplacement
				&& ((LocationReplacement) task).getUnitId() == this.getUnitId() && ((LocationReplacement) task)
				.getLoc() == this.getLoc());
	}

	@Override
	public Part stratopsPartNeeded() {
		return new MekLocation(false, loc, (int) unit.getEntity().getWeight(),
				unit.getEntity().getStructureType(), unit.hasTSM());
	}

	@Override
	public SalvageItem getSalvage() {
		return new LocationSalvage(unit, loc);
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<loc>" + loc
				+ "</loc>");
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
