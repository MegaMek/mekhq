/*
 * Turret.java
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

import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.Tank;

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingTurret extends MissingPart {
	private static final long serialVersionUID = 719267861685599789L;

	public MissingTurret() {
		this(0);
	}
	
	public MissingTurret(int tonnage) {
        super(tonnage);
        this.time = 160;
        this.difficulty = -1;
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof MissingTurret
                && getName().equals(part.getName())
                && getUnitTonnage() == ((MissingTurret)part).getUnitTonnage();
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		// Do nothing - no fields to load.
	}

	@Override
	public int getAvailability(int era) {
		return EquipmentType.RATING_C;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_B;
	}


	@Override
	public boolean isAcceptableReplacement(Part part) {
		return part instanceof TankLocation 
			&& (((TankLocation)part).getLoc() == Tank.LOC_TURRET || ((TankLocation)part).getLoc() == Tank.LOC_TURRET_2);
	}
	
	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		//TODO: how to get second turret location?
		return new TankLocation(Tank.LOC_TURRET, getUnitTonnage(), false);
	}

	@Override
	public double getTonnage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getPurchasePrice() {
		// TODO Auto-generated method stub
		return 0;
	}
}
