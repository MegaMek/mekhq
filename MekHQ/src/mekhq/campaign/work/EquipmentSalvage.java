/*
 * EquipmentSalvage.java
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
import megamek.common.Mounted;
import megamek.common.WeaponType;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.EquipmentPart;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class EquipmentSalvage extends SalvageItem {
	private static final long serialVersionUID = 7982460672148462859L;
	protected Mounted mounted;
	private int equipmentNum = -1;

	public EquipmentSalvage() {
		this(null, null);
	}
    
    public EquipmentSalvage(Unit unit, Mounted m) {
        super(unit);
        this.mounted = m;    
        this.time = 120;
        this.difficulty = 0;
        reCalc();
    }
    
    @Override
    public void reCalc() {
        if (mounted == null)
        	return;
        
        this.name = "Salvage " + mounted.getType().getName();

        if (mounted.getType() instanceof WeaponType && unit.getEntity().getQuirks().booleanOption("mod_weapons")) {
            this.time = 60;
        }
        
        super.reCalc();
    }
    
    @Override
    public String getDetails() {
        String loc = "Unknown";
        if(mounted.getLocation() > -1 && mounted.getLocation() < unit.getEntity().locations()) {
            loc = unit.getEntity().getLocationName(mounted.getLocation());
        }
        return loc + ", " + super.getDetails();
    }
    
    public Mounted getMounted() {
        return mounted;
    }
    
    @Override
    public Part getPart() {
        return null;//new EquipmentPart(true, (int) getUnit().getEntity().getWeight(), 
                //getUnit().campaign.getFaction(), mounted.getType(), getUnit());
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof EquipmentSalvage
                && ((EquipmentSalvage)task).getUnitId() == this.getUnitId()
                && ((EquipmentSalvage)task).getUnit().getEntity().getEquipmentNum(((EquipmentSalvage)task).getMounted()) == unit.getEntity().getEquipmentNum(mounted));
    }

    @Override
    public ReplacementItem getReplacement() {
        return new EquipmentReplacement(unit, mounted);
    }

    @Override
    public void removePart() {
        mounted.setHit(true);
        mounted.setDestroyed(true);
        mounted.setRepairable(false);
        unit.destroySystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		
		// I hate to do this, on some level, but...
		// We know in all EquipmentRepair types that a Unit should be defined...
		// So we should be able to make this assumption.
		// If we ever hit an EquipmentRepair with a Mounted but no Unit, this'll break.
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<mountedEqNum>"
				+ unit.getEntity().getEquipmentNum(mounted) + "</mountedEqNum>");

		writeToXmlEnd(pw1, indent, id);
	}

	public int getEquipmentNum() {
		return equipmentNum;
	}

	public void setMounted(Mounted m) {
		mounted = m;
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("mountedEqNum")) {
				equipmentNum = Integer.parseInt(wn2.getTextContent());
			}
		}
		
		super.loadFieldsFromXmlNode(wn);
	}
}
