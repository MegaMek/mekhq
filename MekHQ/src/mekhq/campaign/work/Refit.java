/*
 * Refit.java
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
import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Entity;
import mekhq.MekHQApp;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.RefitKit;
import mekhq.campaign.personnel.PilotPerson;

/**
 * 
 * @author natit
 */
public abstract class Refit extends ReplacementItem {
	private static final long serialVersionUID = 3378050549702162857L;

	Entity targetEntity;

	public static final int REFIT_CLASS_A = 1;
	public static final int REFIT_CLASS_B = 2;
	public static final int REFIT_CLASS_C = 3;
	public static final int REFIT_CLASS_D = 4;
	public static final int REFIT_CLASS_E = 5;
	public static final int REFIT_CLASS_F = 6;

	protected int minimumSite;
	protected int refitClass;
	protected char refitKitAvailability;
	protected int refitKitAvailabilityMod;
	protected int cost;
	private int tmpBaseTime = -1;
	private Entity tmpTarget = null;

	public Refit(Unit unit, Entity target, int baseTime, int refitClass,
			char refitKitAvailability, int refitKitAvailabilityMod, int cost) {
		super(unit);
		this.targetEntity = target;

		this.refitClass = refitClass;

		this.refitKitAvailability = refitKitAvailability;
		this.refitKitAvailabilityMod = refitKitAvailabilityMod;
		this.cost = cost;
		
		tmpBaseTime = baseTime;
		tmpTarget = target;

		reCalc();
	}

	@Override
	public void reCalc() {
		double timeMultiplier = 999;
		int difficultyModifier = 999;
		int site = Unit.SITE_FACTORY;

		switch (refitClass) {
		case (REFIT_CLASS_A): {
			timeMultiplier = 1;
			difficultyModifier = 1;
			site = Unit.SITE_FIELD;
			break;
		}
		case (REFIT_CLASS_B): {
			timeMultiplier = 1;
			difficultyModifier = 1;
			site = Unit.SITE_FIELD;
			break;
		}
		case (REFIT_CLASS_C): {
			timeMultiplier = 2;
			difficultyModifier = 2;
			site = Unit.SITE_BAY;
			break;
		}
		case (REFIT_CLASS_D): {
			timeMultiplier = 3;
			difficultyModifier = 2;
			site = Unit.SITE_BAY;
			break;
		}
		case (REFIT_CLASS_E): {
			timeMultiplier = 4;
			difficultyModifier = 3;
			site = Unit.SITE_FACTORY;
			break;
		}
		case (REFIT_CLASS_F): {
			timeMultiplier = 5;
			difficultyModifier = 4;
			site = Unit.SITE_FACTORY;
			break;
		}
		}

		if (unit.campaign.getCampaignOptions().useEasierRefit()) {
			timeMultiplier = (timeMultiplier - 1) / 4 + 1;
			difficultyModifier = Math.min(difficultyModifier - 1, 2);
		}

		this.difficulty = difficultyModifier;
		this.minimumSite = site;

		if (tmpBaseTime >= -1)
			this.time = (int) Math.round(tmpBaseTime * timeMultiplier);

		if (tmpTarget != null)
			this.name = "Refit to " + tmpTarget.getModel() + " (Class "
				+ getRefitClassName() + ")";

		super.reCalc();
	}

	public Entity getTargetEntity() {
		return targetEntity;
	}

	public int getRefitClass() {
		return refitClass;
	}

	public int getMinimumSite() {
		return minimumSite;
	}

	public char getRefitKitAvailability() {
		return refitKitAvailability;
	}

	public int getRefitKitAvailabilityMod() {
		return refitKitAvailabilityMod;
	}

	public char getRefitClassName() {
		char name = 'Z';
		switch (getRefitClass()) {
		case (REFIT_CLASS_A): {
			name = 'A';
			break;
		}
		case (REFIT_CLASS_B): {
			name = 'B';
			break;
		}
		case (REFIT_CLASS_C): {
			name = 'C';
			break;
		}
		case (REFIT_CLASS_D): {
			name = 'D';
			break;
		}
		case (REFIT_CLASS_E): {
			name = 'E';
			break;
		}
		case (REFIT_CLASS_F): {
			name = 'F';
			break;
		}
		}

		return name;
	}

	@Override
	public Part stratopsPartNeeded() {
		return new RefitKit(false, (int) getUnit().getEntity().getWeight(),
				getUnit().getEntity().getShortName(), getTargetEntity()
						.getShortName(), cost);
	}

	@Override
	public SalvageItem getSalvage() {
		return null;
	}

	@Override
	public void fix() {
		Campaign campaign = getUnit().campaign;

		campaign.addUnit(targetEntity, false);
		Unit targetUnit = campaign.getUnits().get(
				campaign.getUnits().size() - 1);
		this.targetEntity = targetUnit.getEntity();

		PilotPerson pilotPerson = null;

		if (getUnit().hasPilot()) {
			pilotPerson = getUnit().getPilot();
			getUnit().removePilot();
		}

		// Fix target unit
		for (WorkItem task : campaign.getTasksForUnit(targetUnit.getId())) {
			if (task.checkFixable() == null) {
				if (task instanceof ReplacementItem
						&& !((ReplacementItem) task).hasPart()) {
					ReplacementItem replace = (ReplacementItem) task;
					Part partNeeded = replace.partNeeded();
					replace.setPart(partNeeded);
					campaign.addPart(partNeeded);
				}
				task.succeed();
				if (task.isCompleted()) {
					campaign.removeTask(task);
				}
			}
		}

		targetUnit.setPilot(pilotPerson);

		campaign.removeUnit(getUnitId());

		useUpPart();
	}

	@Override
	public String checkFixable() {
		ArrayList<WorkItem> tasks = getUnit().campaign
				.getAllTasksForUnit(getUnitId());

		for (WorkItem task : tasks) {
			if (task instanceof SalvageItem) {
				return "Some items must be salvaged/scrapped";
			}
		}

		return super.checkFixable();
	}

	protected void writeToXmlBegin(PrintWriter pw1, int indentLvl, int id) {
		super.writeToXmlBegin(pw1, indentLvl, id);
		pw1.println(MekHqXmlUtil.writeEntityToXmlString(targetEntity, indentLvl+1));
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<cost>" + cost
				+ "</cost>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<minimumSite>"
				+ minimumSite + "</minimumSite>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<refitClass>"
				+ refitClass + "</refitClass>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1)
				+ "<refitKitAvailability>" + refitKitAvailability
				+ "</refitKitAvailability>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1)
				+ "<refitKitAvailabilityMod>" + refitKitAvailabilityMod
				+ "</refitKitAvailabilityMod>");
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();

		for (int x = 0; x < nl.getLength(); x++) {
			Node wn2 = nl.item(x);

			if (wn2.getNodeName().equalsIgnoreCase("cost")) {
				cost = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("minimumSite")) {
				minimumSite = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("refitClass")) {
				refitClass = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase(
					"refitKitAvailability")) {
				refitKitAvailability = wn2.getTextContent().trim().charAt(0);
			} else if (wn2.getNodeName().equalsIgnoreCase(
					"refitKitAvailabilityMod")) {
				refitKitAvailabilityMod = Integer
						.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("entity")) {
				try {
					targetEntity = MekHqXmlUtil.getEntityFromXmlString(wn2);
				} catch (Exception ex) {
					MekHQApp.logError(ex);
				}
			}
		}

		super.loadFieldsFromXmlNode(wn);
	}
}
