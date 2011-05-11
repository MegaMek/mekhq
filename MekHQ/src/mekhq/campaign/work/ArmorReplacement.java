/*
 * ArmorReplacement.java
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

import megamek.common.Aero;
import megamek.common.EquipmentType;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;

/**
 * 
 * @author Aaron
 */
public class ArmorReplacement extends ReplacementItem {
	private static final long serialVersionUID = -8847912093791051176L;
	private int loc;
	private int amount;
	private int type;
	private boolean rear;

	public ArmorReplacement() {
		this(null, 0, 0, false);
	}

	public ArmorReplacement(Unit unit, int l, int t, boolean r) {
		super(unit);
		this.loc = l;
		this.type = t;
		this.rear = r;
		this.difficulty = -2;
		reCalc();
	}
    
    @Override
    public void reCalc() {
		if (unit == null)
			return;

		int oArmor = unit.getEntity().getOArmor(loc, rear);
		int cArmor = unit.getEntity().getArmor(loc, rear);
		
		//TODO: Figure out better way to handle code hack.
		// Loading from XML sometimes ends up with negative armor on destroyed locations?
		if (cArmor < 0)
			cArmor = 0;
		
		this.amount = oArmor - cArmor;
		this.time = 5 * amount;
		
		if (unit.getEntity() instanceof Tank) {
			this.time = 3 * amount;
		} else if (unit.getEntity() instanceof Aero) {
			if (((Aero) unit.getEntity()).isCapitalScale()) {
				this.time = 120 * amount;
			} else {
				this.time = 15 * amount;
			}
		}
		
		this.name = "Replace " + EquipmentType.getArmorTypeName(type)
				+ " Armor";

		super.reCalc();
    }

	@Override
	public String getDetails() {
		String locName = unit.getEntity().getLocationName(loc);
		if (rear) {
			locName += " Rear";
		}
		return locName + ", " + amount + " points" + " ("
				+ partNeeded().getCostString() + ")";
	}

	@Override
	public void fix() {
		if (null != part && part instanceof Armor) {
			int points = Math.min(amount, ((Armor) part).getAmount());
			int cArmor = unit.getEntity().getArmor(loc, rear);
			if(cArmor < 0) {
				cArmor = 0;
			}
			unit.getEntity().setArmor(cArmor + points, loc, rear);
			boolean taskFound = false;
			// need to check the salvage task for mutation
			/*
			for (WorkItem task : unit.campaign.getAllTasksForUnit(unit.getId())) {
				if (task instanceof ArmorSalvage
						&& ((ArmorSalvage) task).getLoc() == loc
						&& ((ArmorSalvage) task).isRear() == rear) {
					unit.campaign.mutateTask(task, getSalvage());
					taskFound = true;
				}
			}
			if (!taskFound) {
				unit.campaign.addWork(getSalvage());
			}
			*/
			useUpPart();
		} else {
			super.fix();
			unit.getEntity().setArmor(unit.getEntity().getOArmor(loc, rear),
					loc, rear);
		}
	}

	@Override
	public void complete() {
		if (unit.getEntity().getArmor(loc, rear) == unit.getEntity().getOArmor(
				loc, rear)) {
			setCompleted(true);
		} else {
			// we did not fully repair the armor, probably because of supply
			// shortage
			this.amount = unit.getEntity().getOArmor(loc, rear)
					- unit.getEntity().getArmor(loc, rear);
			this.time = 5 * amount;
			if (unit.getEntity() instanceof Tank) {
				this.time = 3 * amount;
			} else if (unit.getEntity() instanceof Aero) {
				if (((Aero) unit.getEntity()).isCapitalScale()) {
					this.time = 120 * amount;
				} else {
					this.time = 15 * amount;
				}
			}
		}
	}

	@Override
	public String checkFixable() {
		if (unit.isLocationDestroyed(loc)) {
			return unit.getEntity().getLocationName(loc) + " is destroyed.";
		}
		return super.checkFixable();
	}

	public int getLoc() {
		return loc;
	}

	public int getType() {
		return type;
	}

	public boolean isRear() {
		return rear;
	}

	@Override
	public boolean sameAs(WorkItem task) {
		return (task instanceof ArmorReplacement
				&& ((ArmorReplacement) task).getUnitId() == this.getUnitId()
				&& ((ArmorReplacement) task).getLoc() == this.getLoc()
				&& ((ArmorReplacement) task).getType() == this.getType() && ((ArmorReplacement) task)
				.isRear() == this.isRear());
	}

	@Override
	public TargetRoll getAllMods() {
		TargetRoll target = super.getAllMods();
		if (unit.getEntity().getArmorTechLevel(loc) == TechConstants.T_IS_EXPERIMENTAL
				|| unit.getEntity().getArmorTechLevel(loc) == TechConstants.T_CLAN_EXPERIMENTAL) {
			target.addModifier(2, "experimental");
		}
		return target;
	}

	@Override
	public void useUpPart() {
		if (hasPart() && part instanceof Armor) {
			Armor armor = (Armor) part;
			armor.setAmount(armor.getAmount() - amount);
			if (armor.getAmount() < 1) {
				super.useUpPart();
			}
		} else {
			super.useUpPart();
		}
	}

	@Override
	public Part stratopsPartNeeded() {
		// armor is checked for in 5-ton increments
		int armorType = unit.getEntity().getArmorType(loc);
		double armorPerTon = 16.0 * EquipmentType.getArmorPointMultiplier(
				armorType, unit.getEntity().getTechLevel());
		if (armorType == EquipmentType.T_ARMOR_HARDENED) {
			armorPerTon = 8.0;
		}
		int points = (int) Math.floor(armorPerTon * 5);
		return new Armor(false, (int) unit.getEntity().getWeight(), armorType,
				points);
	}

	@Override
	public SalvageItem getSalvage() {
		return new ArmorSalvage(unit, loc, type, rear);
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<amount>" + amount
				+ "</amount>");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<loc>" + loc
				+ "</loc>");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<rear>" + rear
				+ "</rear>");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<type>" + type
				+ "</type>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();

		for (int x = 0; x < nl.getLength(); x++) {
			Node wn2 = nl.item(x);

			if (wn2.getNodeName().equalsIgnoreCase("amount")) {
				amount = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("loc")) {
				loc = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("rear")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					rear = true;
				else
					rear = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("type")) {
				type = Integer.parseInt(wn2.getTextContent());
			}
		}

		super.loadFieldsFromXmlNode(wn);
	}
	
	@Override
	public String getPartDescHTML() {
		String bonus = getAllAcquisitionMods().getValueAsString();
		if (getAllAcquisitionMods().getValue() > -1) {
			bonus = "+" + bonus;
		}
		bonus = "(" + bonus + ")";
		String toReturn = "<html><font size='2'";
		
		Part tmpPart = part;
		if(null == part) {
			tmpPart = partNeeded();
		}
		
		toReturn += ">";
		toReturn += "<b>" + tmpPart.getName() + "</b> " + bonus + "<br/>";
		toReturn += getDetails() + "<br/>";
		toReturn += "</font></html>";
		return toReturn;
	}
}
