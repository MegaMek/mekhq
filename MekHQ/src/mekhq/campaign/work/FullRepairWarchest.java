/*
 * ReloadItem.java
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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import mekhq.CampaignOptionsDialog;
import mekhq.MekHQApp;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class FullRepairWarchest extends UnitWorkItem {
	private static final long serialVersionUID = -5105362085931672916L;
	protected long cost;

    public long getCost() {
        return cost;
    }

	public FullRepairWarchest() {
		this(null);
	}
   
    public FullRepairWarchest(Unit unit) {
        super(unit);
        this.time = 0;
        this.difficulty = TargetRoll.AUTOMATIC_SUCCESS;
        reCalc();
    }
    
    @Override
    public void reCalc() {
        if (unit == null)
        	return;
        
        computeCostAndName();

        super.reCalc();
    }

    private void computeCostAndName () {
        int damageState = unit.getDamageState();

        ResourceMap resourceMap = Application.getInstance(MekHQApp.class).getContext().getResourceMap(CampaignOptionsDialog.class);

        String n = "Repair";

        double repairCostProportionOfEntityPrice = 0;
        switch (damageState) {
            case (Unit.STATE_UNDAMAGED) : {
                repairCostProportionOfEntityPrice = 0;
                n += " (Undamaged)";
                break;
            }
            case (Unit.STATE_LIGHT_DAMAGE) : {
                repairCostProportionOfEntityPrice = resourceMap.getDouble("repairSystem.WarchestCustom.lightDamageModifier");
                n += " (Light Damage)";
                break;
            }
            case (Unit.STATE_HEAVY_DAMAGE) : {
                repairCostProportionOfEntityPrice = resourceMap.getDouble("repairSystem.WarchestCustom.heavyDamageModifier");
                n += " (Heavy Damage)";
                break;
            }
            case (Unit.STATE_CRIPPLED) : {
                repairCostProportionOfEntityPrice = resourceMap.getDouble("repairSystem.WarchestCustom.cripplingDamageModifier");
                n += " (Crippled)";
                break;
            }
        }

        this.name = n;
        int buyCost = unit.getBuyCost();
        this.cost = (long) Math.round(buyCost*repairCostProportionOfEntityPrice);
    }
    
    @Override
    public String getDetails() {
        String desc = "";
        if (getCost() > 0) {
            NumberFormat numberFormat = DecimalFormat.getIntegerInstance();
            String text = numberFormat.format(getCost()) + " " + (getCost()!=0?"CBills":"CBill");
            desc += " (" + text + ")";
        }
        return desc;
    }
    
    @Override
    public String checkFixable() {
        return null;
    }
    
    @Override
    public void fix() {
        for (int loc=0; loc<unit.getEntity().locations(); loc++) {
            // fix armor
            unit.getEntity().setArmor(unit.getEntity().getOArmor(loc), loc);
            if (unit.getEntity().hasRearArmor(loc)) {
                unit.getEntity().setArmor(unit.getEntity().getOArmor(loc, true), loc, true);
            }

            // fix internal
            unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);

            // repair any hips or shoulders
            // unit.getEntity().removeCriticals(loc, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP));
            // unit.getEntity().removeCriticals(loc, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER));

            // repair crits
            int nbCrits = unit.getEntity().getNumberOfCriticals(loc);
            for (int crit=0;crit<nbCrits;crit++) {
                CriticalSlot criticalSlot = unit.getEntity().getCritical(loc, crit);
                if (criticalSlot != null) {
                    criticalSlot.setHit(false);
                    criticalSlot.setDestroyed(false);
                }
            }
        }

        for (Mounted mounted : unit.getEntity().getEquipment()) {
            // repair equipment
            mounted.setHit(false);
            mounted.setDestroyed(false);
            mounted.setRepairable(true);
            unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));

            // reload ammo
            if (mounted.getType() instanceof AmmoType)
                mounted.setShotsLeft(((AmmoType)mounted.getType()).getShots());
        }
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof FullRepairWarchest
                && ((FullRepairWarchest)task).getUnitId() == this.getUnitId()
                && getCost() == ((FullRepairWarchest) task).getCost());
    }

    @Override
    protected String maxSkillReached() {
        //this should never happen because it is automatic success so failure should never be called
        return "";
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+ "<cost>"
				+ cost
				+ "</cost>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("cost")) {
				cost = Long.parseLong(wn2.getTextContent());
			}
		}
		
		super.loadFieldsFromXmlNode(wn);
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_C;
	}
}
