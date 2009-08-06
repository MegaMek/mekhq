/*
 * HealPilot.java
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

import mekhq.campaign.personnel.PilotPerson;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class HealPilot extends PersonnelWorkItem {
    
    public HealPilot(PilotPerson pp) {
        super(pp);
        this.name = "Heal";
    }

    @Override
    public void fix() {
        person.heal();
    }
    
    @Override
    public void complete() {
        //only complete the task if the pilot is fully healed
        if(person instanceof PilotPerson) {
            //should be here, but just to be safe
            PilotPerson pp = (PilotPerson)person;
            if(null != pp.getPilot() && pp.getPilot().getHits() < 1) {
                super.complete();
                team = null;
                pp.setTask(null);
            }
        }
    }
}
