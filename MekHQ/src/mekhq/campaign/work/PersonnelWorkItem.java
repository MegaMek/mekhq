/*
 * MedicalWorkItem.java
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

import mekhq.campaign.personnel.Person;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class PersonnelWorkItem extends WorkItem {

    //the person for whom the work is being done
    protected Person person;
    //how many patients this person counts for
    protected int patients;
    //is this work item assigned?
    protected boolean assigned;
    
    public PersonnelWorkItem(Person p) {
        super();
        this.person = p;
        this.patients = 1;
        this.assigned = false;
    }
    
    @Override
    public String getDisplayName() {
        return getName() + " " + person.getDesc();
    }
    
    public int getPatients() {
        return patients;
    }
    
    public void setPatients(int i) {
        this.patients = i;
    }
    
    public boolean isAssigned() {
        return assigned;
    }
    
    public void assign() {
        this.assigned = true;
    }
    
    public void unassign() {
        this.assigned = false;
    }
    
}
