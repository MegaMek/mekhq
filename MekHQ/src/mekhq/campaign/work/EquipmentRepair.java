/*
 * NewClass.java
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
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class EquipmentRepair extends RepairItem {
    private static final long serialVersionUID = -2354604328101928006L;
	protected Mounted mounted;
	private int equipmentNum = -1;

	public EquipmentRepair() {
		this(null, 0, null);
	}
    
    public EquipmentRepair(Unit unit, int h, Mounted m) {
        super(unit, h);
        this.mounted = m;
        this.time = 100;
        this.difficulty = -3;
        reCalc();
    }
    
    @Override
    public void reCalc() {
        if (hits == 2) {
            this.time = 150;
            this.difficulty = -2;
        } else if (hits == 3) {
            this.time = 200;
            this.difficulty = 0;
        } else if (hits >= 4) {
            this.time = 250;
            this.difficulty = 2;
        }

        if (mounted == null)
        	return;
        
        this.name = "Repair " + mounted.getType().getName();

        super.reCalc();
    }

    @Override
    public String getDetails() {
        return unit.getEntity().getLocationName(mounted.getLocation()) + ", " + super.getDetails();
    }

    @Override
    public void fix() {
        mounted.setHit(false);
        mounted.setDestroyed(false);
        unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));
    }

    @Override
    public void doReplaceChanges() {
        removeSalvage();
        mounted.setHit(true);
        mounted.setDestroyed(true);
        unit.destroySystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));
    }

    @Override
    public WorkItem getReplacementTask () {
        return new EquipmentReplacement(unit, mounted);
    }
    
    @Override
    public String checkFixable() {
        // The part is only fixable if the location is not destroyed.
        // We have to cycle through all locations because some equipment is spreadable.
        for(int loc = 0; loc < unit.getEntity().locations(); loc++) {
            for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                
                // ignore empty & system slots
                if ((slot == null) || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                    continue;
                }
                
                if (unit.getEntity().getEquipmentNum(mounted) == slot.getIndex()) {
                    if (unit.isLocationDestroyed(loc)) {
                        return unit.getEntity().getLocationName(loc) + " is destroyed.";
                    }
                }
            }
        }
        
        return super.checkFixable();
    }
    
    public Mounted getMounted() {
        return mounted;
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof EquipmentRepair 
                && ((EquipmentRepair)task).getUnitId() == this.getUnitId()
                && ((EquipmentRepair)task).getUnit().getEntity().getEquipmentNum(((EquipmentRepair)task).getMounted()) == unit.getEntity().getEquipmentNum(mounted));
    }
    
    @Override
    public TargetRoll getAllMods() {
        TargetRoll target = super.getAllMods();
        
        if (mounted.getType().getTechLevel() == TechConstants.T_IS_EXPERIMENTAL
                || mounted.getType().getTechLevel() == TechConstants.T_CLAN_EXPERIMENTAL) {
            target.addModifier(2,"experimental");
        }
        
        return target;
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

	public int getEquipmentNum() {
		return equipmentNum;
	}

	public void setMounted(Mounted m) {
		mounted = m;
	}
}
