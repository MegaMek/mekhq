/*
 * PartsStore.java
 * 
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package mekhq.campaign;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;

import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.WeaponType;
import mekhq.campaign.parts.EquipmentPart;
import mekhq.campaign.parts.Part;


/**
 * This is a parts store which will contain one copy of every possible
 * part that might be needed as well as a variety of helper functions to
 * acquire parts.
 * 
 * We could in the future extend this to different types of stores that have different finite numbers of
 * parts in inventory
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class PartsStore implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1686222527383868364L;
	
	private ArrayList<Part> parts;
	
	public PartsStore() {
		parts = new ArrayList<Part>();
		stock();
	}
	
	public ArrayList<PartInventory> getInventory() {
		ArrayList<PartInventory> partsInventory = new ArrayList<PartInventory>();

		Iterator<Part> itParts = parts.iterator();
		while (itParts.hasNext()) {
			Part part = itParts.next();
			partsInventory.add(new PartInventory(part, 1));
		}

		return partsInventory;
	}
	
	public void stock() {
		stockWeaponsAmmoAndEquipment();	
	}
	
	public void stockWeaponsAmmoAndEquipment() {
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            parts.add(new EquipmentPart(0, et, -1));
            //TODO: ammo, heat sinks, and jump jets needed to use their own constructors
            /*
            if ((et instanceof WeaponType)) {
                weapons.add(et.getName());
                if (et.hasFlag(WeaponType.F_C3M) || et.hasFlag(WeaponType.F_C3MBS)) {
                    equipment.add(et.getName());
                }
            }
            if ((et instanceof MiscType)) {
                equipment.add(et.getName());
            }
            */
        }
	}
	
}