/*
 * MissingEquipmentPart.java
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

import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megamek.common.weapons.Weapon;
import mekhq.campaign.Era;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingHeatSink extends MissingEquipmentPart {
	private static final long serialVersionUID = 2892728320891712304L;

	public MissingHeatSink() {
    	this(0, null, -1);
    }
    
    public MissingHeatSink(int tonnage, EquipmentType et, int equipNum) {
        // TODO Memorize all entity attributes needed to calculate cost
        // As it is a part bought with one entity can be used on another entity
        // on which it would have a different price (only tonnage is taken into
        // account for compatibility)
        super(tonnage, et, equipNum);
        this.time = 90;
        this.difficulty = -2;
    }
    
    /**
     * Copied from megamek.common.Entity.getWeaponsAndEquipmentCost(StringBuffer detail, boolean ignoreAmmo)
     */
    @Override
    public long getPurchasePrice() {
    	if(null != unit) {
    		Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
    		if(null != mounted) {			
    			if(mounted.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
    				return 6000;
    			}
    		}
    	}
    	return 2000;
    }
}
