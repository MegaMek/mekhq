/*
 * Customization.java
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
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.personnel.PilotPerson;

/**
 *
 * @author natit
 */
public abstract class Customization extends UnitWorkItem {
	private static final long serialVersionUID = -2016091299829485469L;

	Entity targetEntity;

    public static final int REFIT_CLASS_A = 1;
    public static final int REFIT_CLASS_B = 2;
    public static final int REFIT_CLASS_C = 3;
    public static final int REFIT_CLASS_D = 4;
    public static final int REFIT_CLASS_E = 5;
    public static final int REFIT_CLASS_F = 6;

    protected int minimumSite;
    protected int refitClass;
    
    private int tmpBaseTime = -1;
    private Entity tmpTarget = null;

    public Customization(Unit unit, Entity target, int baseTime, int refitClass) {
        super(unit);
        this.targetEntity = target;
        this.refitClass = refitClass;
        tmpBaseTime = -1;
        reCalc();
    }
    
    @Override
    public void reCalc() {
        double timeMultiplier = 999;
        int difficultyModifier = 999;
        int site = Unit.SITE_FACTORY;

        switch (refitClass) {
            case (REFIT_CLASS_A) : {
                timeMultiplier = 1;
                difficultyModifier = 1;
                site = Unit.SITE_FIELD;
                break;
            }
            case (REFIT_CLASS_B) : {
                timeMultiplier = 1;
                difficultyModifier = 1;
                site = Unit.SITE_FIELD;
                break;
            }
            case (REFIT_CLASS_C) : {
                timeMultiplier = 2;
                difficultyModifier = 2;
                site = Unit.SITE_BAY;
                break;
            }
            case (REFIT_CLASS_D) : {
                timeMultiplier = 3;
                difficultyModifier = 2;
                site = Unit.SITE_BAY;
                break;
            }
            case (REFIT_CLASS_E) : {
                timeMultiplier = 4;
                difficultyModifier = 3;
                site = Unit.SITE_FACTORY;
                break;
            }
            case (REFIT_CLASS_F) : {
                timeMultiplier = 5;
                difficultyModifier = 4;
                site = Unit.SITE_FACTORY;
                break;
            }
        }

        if (unit.campaign.getCampaignOptions().useEasierRefit()) {
            timeMultiplier = (timeMultiplier - 1) / 4 + 1;
            difficultyModifier = Math.min(difficultyModifier-1, 2);
        }

        // No refit kit customization
        timeMultiplier *= 2;
        difficultyModifier += 2;
        
        if (tmpBaseTime >= 0)
        	this.time = (int) Math.round(tmpBaseTime * timeMultiplier);
        
        this.difficulty = difficultyModifier;
        this.minimumSite = site;

        // Repairs must still be performed after the customization is done
        this.time -= getTotalRepairTime();
        
        if (tmpTarget != null)
        	this.name = "Customize to " + tmpTarget.getModel() + " (Class " + getRefitClassName() + ")";

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

    public char getRefitClassName () {
        char name = 'Z';

        switch (getRefitClass()) {
            case (REFIT_CLASS_A) : {
                name = 'A';
                break;
            }
            case (REFIT_CLASS_B) : {
                name = 'B';
                break;
            }
            case (REFIT_CLASS_C) : {
                name = 'C';
                break;
            }
            case (REFIT_CLASS_D) : {
                name = 'D';
                break;
            }
            case (REFIT_CLASS_E) : {
                name = 'E';
                break;
            }
            case (REFIT_CLASS_F) : {
                name = 'F';
                break;
            }
        }

        return name;
    }

    @Override
    public void fix() {
        Campaign campaign = getUnit().campaign;
        campaign.addUnit(targetEntity, false);
        Unit targetUnit = campaign.getUnits().get(campaign.getUnits().size()-1);
        this.targetEntity = targetUnit.getEntity();
        PilotPerson pilotPerson = null;

        if (getUnit().hasPilot()) {
            pilotPerson = getUnit().getPilot();
            getUnit().removePilot();
        }

        targetUnit.setPilot(pilotPerson);
        campaign.removeUnit(getUnitId());
    }

    @Override
    public String checkFixable() {
        ArrayList<WorkItem> tasks = getUnit().campaign.getAllTasksForUnit(getUnitId());
 
        for (WorkItem task : tasks) {
            if (task instanceof SalvageItem) {
                return "Some items must be salvaged/scrapped";
            }
        }

        return super.checkFixable();
    }

    private int getTotalRepairTime () {
        Campaign campaign = getUnit().campaign;
        campaign.addUnit(targetEntity, false);
        Unit targetUnit = campaign.getUnits().get(campaign.getUnits().size()-1);
        this.targetEntity = targetUnit.getEntity();
        ArrayList<WorkItem> unitTasks = campaign.getTasksForUnit(targetUnit.getId());
        int totalRepairTime = 0;
        
        for (WorkItem unitTask : unitTasks) {
            if (unitTask instanceof RepairItem
                    || unitTask instanceof ReplacementItem
                    || unitTask instanceof ReloadItem) {
                totalRepairTime += unitTask.getTimeLeft();
            }
        }

        campaign.removeUnit(targetUnit.getId());

        return totalRepairTime;
    }

    @Override
    public String fail(int rating) {
        return " <font color='red'><b>task failed.</b></font>";
    }

    @Override
    protected String maxSkillReached() {
        // Impossible
        return "Max skill reached";
    }

	protected void writeToXmlBegin(PrintWriter pw1, int indentLvl, int id) {
		super.writeToXmlBegin(pw1, indentLvl, id);
		pw1.println(MekHqXmlUtil.writeEntityToXmlString(targetEntity, indentLvl+1));
		pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
				+"<minimumSite>"
				+minimumSite
				+"</minimumSite>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
				+"<refitClass>"
				+refitClass
				+"</refitClass>");
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("minimumSite")) {
				minimumSite = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("refitClass")) {
				refitClass = Integer.parseInt(wn2.getTextContent());
			}
		}
		
		super.loadFieldsFromXmlNode(wn);
	}
}
