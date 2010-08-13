/*
 * HeatSinkSalvage.java
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
public class HeatSinkSalvage extends EquipmentSalvage {
	private static final long serialVersionUID = 6038368573767322304L;

	public HeatSinkSalvage() {
		this(null, null);
	}

	public HeatSinkSalvage(Unit u, Mounted m) {
        super(u, m);
        this.time = 90;
        this.difficulty = -2;
    } 
    
    @Override
    public void reCalc() {
    	super.reCalc();
    }

    @Override
    public ReplacementItem getReplacement() {
        return new HeatSinkReplacement(unit, mounted);
    }
}
