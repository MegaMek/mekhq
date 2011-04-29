/*
 * Force.java
 * 
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package mekhq.campaign;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;

import mekhq.campaign.personnel.PilotPerson;

/**
 * This is a hierarchical object to define forces for TO&E. Each Force
 * object can have a parent force object and a vector of child force objects.
 * Each force can also have a vector of PilotPerson objects. The idea
 * is that any time TOE is refreshed in MekHQView, the force object can be traversed
 * to generate a set of TreeNodes that can be applied to the JTree showing the force
 * TO&E.
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Force implements Serializable, MekHqXmlSerializable {

	private static final long serialVersionUID = -3018542172119419401L;

	private String name;
	private String desc;
	private Force parentForce;
	private Vector<Force> subForces;
	private Vector<PilotPerson> personnel;

	//an ID so that forces can be tracked in Campaign hash
	private int id;
	
	public Force(String n) {
		this.name = n;
		this.desc = "";
		this.parentForce = null;
		this.subForces = new Vector<Force>();
		this.personnel = new Vector<PilotPerson>();
	}
	
	public Force(String n, int id, Force parent) {
		this(n);
		this.parentForce = parent;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String n) {
		this.name = n;
	}
	
	public String getDescription() {
		return desc;
	}
	
	public void setDescription(String d) {
		this.desc = d;
	}
	
	public Force getParentForce() {
		return parentForce;
	}
	
	public void setParentForce(Force parent) {
		this.parentForce = parent;
	}
	
	public Vector<Force> getSubForces() {
		return subForces;
	}
	
	/**
	 * Add a subforce to the subforce vector. In general, this
	 * should not be called directly to add forces to the campaign
	 * because they will not be assigned an id. Use {@link Campaign#addForce(Force, Force)}
	 * instead
	 * @param sub
	 */
	public void addSubForce(Force sub) {
		subForces.add(sub);
	}
	
	public Vector<PilotPerson> getPersonnel() {
		return personnel;
	}
	
	public void addPerson(PilotPerson person) {
		person.setForceId(id);
		personnel.add(person);
	}
	
	public String toString() {
		return name;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int i) {
		this.id = i;
	}
	
	@Override
	public void writeToXml(PrintWriter pw1, int indent, int inId) {
		// TODO Auto-generated method stub
		
	}
	
}