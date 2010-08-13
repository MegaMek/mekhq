/*
 * JumpJetRepair.java
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

import megamek.common.Mounted;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class JumpJetRepair extends EquipmentRepair {
	private static final long serialVersionUID = 125943032552444821L;

	public JumpJetRepair() {
		this(null, 0, null);
	}

	public JumpJetRepair(Unit unit, int h, Mounted m) { 
        super(unit, h, m);
        this.time = 90;
        this.difficulty = 0;
        reCalc();
    }
    
    @Override
    public void reCalc() {
    	// Do nothing.
    	super.reCalc();
    }

    @Override
    public WorkItem getReplacementTask() {
        return new JumpJetReplacement(unit, mounted);
    }
}
