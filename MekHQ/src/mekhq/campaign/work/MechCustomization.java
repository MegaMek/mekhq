/*
 * MechCustomization.java
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

import megamek.common.Entity;
import megamek.common.EquipmentType;
import mekhq.campaign.Unit;

/**
 *
 * @author natit
 */
public class MechCustomization extends Customization {
	private static final long serialVersionUID = 6874258058034113524L;

	public MechCustomization() {
		this(null, null, 0, 0);
	}

	public MechCustomization(Unit unit, Entity target, int baseTime, int refitClass) {
        super(unit, target, baseTime, refitClass);
    }    

    @Override
    public void reCalc() {
    	// Do nothing.
    	super.reCalc();
    }
    
    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof Customization
                && ((Customization) task).getUnitId() == this.getUnitId()
                && ((Customization) task).getTargetEntity().getModel().equals(this.getTargetEntity().getModel()));
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		super.loadFieldsFromXmlNode(wn);
	}
	
	@Override
	public int getTechRating() {
		return EquipmentType.RATING_C;
	}
}
