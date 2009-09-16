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
    
    public PersonnelWorkItem(Person p) {
        super();
        this.person = p;
        this.patients = 1;
    }
    
    @Override
    public String getDisplayName() {
        return getDesc() + " " + person.getDesc();
    }
    
    public int getPatients() {
        return patients;
    }
    
    public void setPatients(int i) {
        this.patients = i;
    }
    
    public Person getPerson() {
        return person;
    }
    
    @Override
    public String fail(int rating) {
        return " <font color='red'><b>task failed.</b></font>";
    }
    
    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof PersonnelWorkItem 
                    && ((PersonnelWorkItem)task).getPerson().getId() == this.getPerson().getId());
    }
}
