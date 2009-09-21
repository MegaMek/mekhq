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

import mekhq.campaign.Campaign;
import mekhq.campaign.team.SupportTeam;

/**
 * A person wrapper for support teams
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class SupportPerson extends Person {

    private SupportTeam team;
    
    public SupportPerson(SupportTeam t) {
        super();
        this.team = t;
    }
    
    public SupportTeam getTeam() {
        return team;
    }
    
    @Override
    public String getDesc() {
        String casualties = "";
        if(team.getCasualties() > 0) {
            casualties = " (" + team.getCasualties() + " casualties) ";
        }
        return team.getName() + " [" + team.getRatingName() + " " + team.getTypeDesc() + "]"  + casualties ;
    }
    
    @Override
    public String getDescHTML() {
        String toReturn = "<html><font size='2'><b>" + team.getName() + "</b><br>";
        toReturn += team.getRatingName() + " " + team.getTypeDesc() + "<br>";
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
        //TODO: implement
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
        String toReturn = "<b>" + team.getName() + "</b><br>";
        
        return toReturn;
    }
    
}
