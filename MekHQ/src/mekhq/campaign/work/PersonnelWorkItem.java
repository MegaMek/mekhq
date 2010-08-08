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

import java.io.PrintWriter;

import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.personnel.Person;

/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class PersonnelWorkItem extends WorkItem {
	private static final long serialVersionUID = 1830844719681238733L;
	// the person for whom the work is being done
	protected Person person;
	// how many patients this person counts for
	protected int patients;
	protected int personId = -1; // Used for XML Serialization.

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
		return (task instanceof PersonnelWorkItem && ((PersonnelWorkItem) task)
				.getPerson().getId() == this.getPerson().getId());
	}

	protected void writeToXmlBegin(PrintWriter pw1, int indent, int id) {
		super.writeToXmlBegin(pw1, indent, id);
		// Person reference will be corrected in post-load in the campaign
		// object.
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<personId>"
				+ person.getId() + "</personId>");
		pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<patients>"
				+ patients + "</patients>");
	}

	public int getPersonId() {
		return personId;
	}
	
	public void setPerson(Person prsn) {
		person = prsn;
	}
}
