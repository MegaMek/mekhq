/*
 * SupportPerson.java
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

package mekhq.campaign.personnel;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.team.TechTeam;

/**
 * A person wrapper for support teams
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class SupportPerson extends Person {
	private static final long serialVersionUID = 8476806535893310057L;
	private SupportTeam team;
	private int teamId;
    
	public SupportPerson() {
		this(null);
	}
	
    public SupportPerson(SupportTeam t) {
        super();
        this.team = t;
        reCalc();
    }
    
    @Override
    public void reCalc() {
    	// Do nothing.
    }
    
    public SupportTeam getTeam() {
        return team;
    }
    
    public void setTeam(SupportTeam t) {
    	team = t;
    }
    
    public int getTeamId() {
    	return teamId;
    }
   
    
    public String getTypeDesc() {
    	if(null != team) {
    		return team.getTypeDesc();
    	} else {
    		return "Unknown";
    	}
    }
    
    @Override
    public String getSkillSummary() {
    	return getTeam().getRatingName();
    }
    
    @Override
    public String getDesc() {
        String casualties = "";
        if(team.getCasualties() > 0) {
            casualties = " (" + team.getCasualties() + " casualties) ";
        }
        return team.getName() + " [" + team.getRatingName() + " " + team.getTypeDesc() + "]"  + casualties;
    }
    
    @Override
    public String getDescHTML() {
        String toReturn = "<html><font size='2'><b>" + team.getName() + "</b><br/>";
        toReturn += team.getRatingName() + " " + team.getTypeDesc() + "<br/>";
        if(team.getCasualties() > 0) {
            toReturn = team.getCasualties() + " casualties" + getAssignedDoctorString();
        }
        if(isDeployed()) {
            toReturn += "DEPLOYED!";
        }
        toReturn += "</font></html>";
        return toReturn;
    }

    @Override
    public void runDiagnostic(Campaign campaign) {
        //TODO: Implement diagnostics on the SupportPerson class.
    }

    @Override
    public String getName() {
    	if(null != team) {
    		return team.getName();
    	} else {
    		return "Unknown";
    	}
    }
    
    @Override
    public String getCallsign() {
    	return "-";
    }
    
    @Override
    public void heal() {
        if(needsHealing()) {
            team.setCurrentStrength(team.getCurrentStrength() + 1);
        }
        if(!needsHealing() && null != task) {
            task.complete();
        }
    }

    @Override
    public boolean needsHealing() {
       return (team.getCasualties() > 0);
    }
  
    @Override
    public String getDossier() {
        String toReturn = "<b>" + team.getName() + "</b><br/>";
        toReturn += "<b>XP:" + getXp() + "</b><br/>";
        
        return toReturn;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<teamId>"
				+team.getId()
				+"</teamId>");
		writeToXmlEnd(pw1, indent, id);
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("teamId")) {
				teamId = Integer.parseInt(wn2.getTextContent());
			}
		}
	}
    
    @Override
    public int getMonthlySalary() {
    	int retVal = 0;
       	int type = team.getTeamType();
       	int type2 = -1;
       	
       	if (type == SupportTeam.TYPE_TECH)
       		type2 = ((TechTeam) team).getType();

    	switch (type) {
		case SupportTeam.TYPE_MEDICAL:
			retVal = 1500;
			break;
		case SupportTeam.TYPE_TECH:
			switch (type2) {
			case TechTeam.T_MECH:
				retVal = 800;
				break;
			case TechTeam.T_MECHANIC:
				retVal = 640;
				break;
			case TechTeam.T_AERO:
				retVal = 800;
				break;
			case TechTeam.T_BA:
				retVal = 800;
				break;
			}
    	}
    	
    	//TODO: Add Administrator.
    	//TODO: Add Astech/Paramedic rules for salary.

    	//TODO: Add era mod to salary calc.
    	//TODO: Add quality mods to salary calc.
    	
    	return retVal;
    }
}
