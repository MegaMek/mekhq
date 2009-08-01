/*
 * MedicalTeam.java
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

package mekhq.campaign.team;

import megamek.common.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.work.PersonnelWorkItem;
import mekhq.campaign.work.WorkItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MedicalTeam extends SupportTeam {

    public MedicalTeam(Campaign c, String name, int rating) {
        super(c, name, rating);
        this.fullSize = 5;
        this.currentSize = 5;
    }
    
    @Override
    public int getSkillBase(int effectiveRating) {
        int base = 11;
         switch(effectiveRating) {
           case SupportTeam.EXP_GREEN:
               base = 10;
               break;
           case SupportTeam.EXP_REGULAR:
               base = 8;
               break;
           case SupportTeam.EXP_VETERAN:
               base = 7;
               break;
           case SupportTeam.EXP_ELITE:
               base = 6;
               break;
       }
       return base;
    }

    protected int getPatients() {
       int patients = 0;
        for(WorkItem task : campaign.getTasks()) {
           if(task instanceof PersonnelWorkItem && task.isAssigned() && task.getTeam().getId() == getId()) {
               patients += ((PersonnelWorkItem)task).getPatients();
           }
       }
       return patients;
    }

    @Override
    public String getTypeDesc() {
        return "Doctor";
    }

    @Override
    public boolean canDo(WorkItem task) {
        if(!(task instanceof PersonnelWorkItem)) {
            return false;
        }
        return true; 
    }

    @Override
    public int makeRoll(WorkItem task) {
        return Compute.d6(2);
    }
   
    @Override
   public String getDescHTML() {
        String toReturn = "<html><b>" + getName() + "</b><br>";
        toReturn += getRatingName() + " " + getTypeDesc() + "<br>";
        toReturn += getPatients() + " patient(s)";
        toReturn += "</html>";
        return toReturn;
   }
}
