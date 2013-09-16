/*
 * PersonnelMarket.java
 * 
 * Copyright (c) 2013 Dylan Myers <dylan at dylanspcs.com>. All rights reserved.
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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.UUID;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import mekhq.MekHQ;
import mekhq.Version;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.team.TechTeam;

public class PersonnelMarket {
	private ArrayList<Person> personnel = new ArrayList<Person>();
	private Hashtable<UUID, Person> personnelIds = new Hashtable<UUID, Person>();
	
	public PersonnelMarket() {
	}
	
	public PersonnelMarket(Campaign c) {
		generatePersonnelForDay(c);
	}
	
	/*
	 * Generate new personnel to be added to the
	 * market availability pool
	 */
	public void generatePersonnelForDay(Campaign c) {
		if (!personnel.isEmpty()) {
			removePersonnelForDay();
		}
		
		int roll = Compute.d6(2);
		int q = 0;
		if (roll == 12) {
			q = 6;
		} else if (roll < 12) {
			q = 5;
		} else if (roll < 10) {
			q = 4;
		} else if (roll < 8) {
			q = 3;
		} else if (roll < 5) {
			q = 2;
		} else if (roll < 3) {
			q = 1;
		} else if (roll == 1) {
			q = 0;
		}
		
		for (int i = 0; i < q; i++) {
			roll = Compute.randomInt(Person.T_NUM-1);
			while (roll == Person.T_NONE) {
				roll = Compute.randomInt(Person.T_NUM-1);
			}
			Person p = c.newPerson(roll);
			UUID id = UUID.randomUUID();
			while (null != personnelIds.get(id)) {
				id = UUID.randomUUID();
			}
			p.setId(id);
			personnel.add(p);
			personnelIds.put(id, p);
		}
	}
	
	public void setPersonnel(ArrayList<Person> p) {
		personnel = p;
	}
	
	public ArrayList<Person> getPersonnel() {
		return personnel;
	}
	
	public void addPerson(Person p) {
		personnel.add(p);
	}
	
	public void removePerson(Person p) {
		personnel.remove(p);
	}
	
	/*
	 * Remove personnel from market on a new day
	 * The better they are, the faster they disappear
	 */
	public void removePersonnelForDay() {
		ArrayList<Person> toRemove = new ArrayList<Person>();
		for (Person p : personnel) {
			int roll = Compute.d6(2);
			if (p.getExperienceLevel(false) == SkillType.EXP_ELITE && roll < 10) {
				toRemove.add(p);
			} else if (p.getExperienceLevel(false) == SkillType.EXP_VETERAN && roll < 8) {
				toRemove.add(p);
			} else if (p.getExperienceLevel(false) == SkillType.EXP_REGULAR && roll < 6) {
				toRemove.add(p);
			} else if (p.getExperienceLevel(false) == SkillType.EXP_GREEN && roll < 4) {
				toRemove.add(p);
			} else if (p.getExperienceLevel(false) == SkillType.EXP_ULTRA_GREEN && roll < 2) {
				toRemove.add(p);
			}
		}
		personnel.removeAll(toRemove);
	}
	
	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<personnelMarket>");
		for (Person p : personnel) {
			p.writeToXml(pw1, indent + 1);
		}
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</personnelMarket>");
	}
	
	public static PersonnelMarket generateInstanceFromXML(Node wn, Campaign c, Version version) {
		PersonnelMarket retVal = null;
		
		try {
			// Instantiate the correct child class, and call its parsing function.
			retVal = new PersonnelMarket();
			
			// Okay, now load Part-specific fields!
			NodeList nl = wn.getChildNodes();
			
			// Loop through the nodes and load our personnel
			for (int x = 0; x < nl.getLength(); x++) {
				Node wn2 = nl.item(x);

				// If it's not an element node, we ignore it.
				if (wn2.getNodeType() != Node.ELEMENT_NODE)
					continue;

				if (!wn2.getNodeName().equalsIgnoreCase("person")) {
					// Error condition of sorts!
					// Errr, what should we do here?
					MekHQ.logMessage("Unknown node type not loaded in Personnel nodes: "
							+ wn2.getNodeName());

					continue;
				}

				Person p = Person.generateInstanceFromXML(wn2, version);

				if (p != null) {
					retVal.personnel.add(p);
				}
			}
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQ.logError(ex);
		}
		
		// All personnel need the rank reference fixed
		for (int x = 0; x < retVal.personnel.size(); x++) {
			Person psn = retVal.personnel.get(x);

			psn.setRankSystem(c.getRanks());

			// skill types might need resetting
			psn.resetSkillTypes();
		}
		
		return retVal;
	}
}
