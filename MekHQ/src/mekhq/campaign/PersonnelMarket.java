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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.UUID;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.Version;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;

public class PersonnelMarket {
	private ArrayList<Person> personnel = new ArrayList<Person>();
	private Hashtable<UUID, Person> personnelIds = new Hashtable<UUID, Person>();
	private int daysSinceRolled;
	
	public static final int TYPE_RANDOM		= 0;
	public static final int TYPE_DYLANS		= 1;
	public static final int TYPE_FMMR		= 2;
	public static final int TYPE_STRAT_OPS	= 3;
	public static final int TYPE_NUM		= 4;
	
	public PersonnelMarket() {
	}
	
	public PersonnelMarket(Campaign c) {
		daysSinceRolled = c.getCampaignOptions().getMaintenanceCycleDays();
		generatePersonnelForDay(c);
	}
	
	/*
	 * Generate new personnel to be added to the
	 * market availability pool
	 */
	public void generatePersonnelForDay(Campaign c) {
		int roll;
		int q = 0;
		boolean report = false;
		Person p;
		
		if (!personnel.isEmpty()) {
			removePersonnelForDay(c);
		}
		
		switch (c.getCampaignOptions().getPersonnelMarketType()) {
		case TYPE_DYLANS: // TODO: Add in extra infantry and vehicle crews
			q = generateRandomQuantity();
			
			ArrayList<Long> mtf = new ArrayList<Long>();
			long mostTypes = getUnitMainForceTypes(c);
			if ((mostTypes & Entity.ETYPE_MECH) != 0) {
				mtf.add(Entity.ETYPE_MECH);
			} else if ((mostTypes & Entity.ETYPE_TANK) != 0) {
				mtf.add(Entity.ETYPE_TANK);
			} else if ((mostTypes & Entity.ETYPE_AERO) != 0) {
				mtf.add(Entity.ETYPE_AERO);
			} else if ((mostTypes & Entity.ETYPE_BATTLEARMOR) != 0) {
				mtf.add(Entity.ETYPE_BATTLEARMOR);
			} else if ((mostTypes & Entity.ETYPE_INFANTRY) != 0) {
				mtf.add(Entity.ETYPE_INFANTRY);
			} else if ((mostTypes & Entity.ETYPE_PROTOMECH) != 0) {
				mtf.add(Entity.ETYPE_PROTOMECH);
			} else if ((mostTypes & Entity.ETYPE_CONV_FIGHTER) != 0) {
				mtf.add(Entity.ETYPE_CONV_FIGHTER);
			} else if ((mostTypes & Entity.ETYPE_SMALL_CRAFT) != 0) {
				mtf.add(Entity.ETYPE_SMALL_CRAFT);
			} else if ((mostTypes & Entity.ETYPE_DROPSHIP) != 0) {
				mtf.add(Entity.ETYPE_DROPSHIP);
			} else {
				mtf.add(Entity.ETYPE_MECH);
			}
			
			
			int weight = (int)(c.getCampaignOptions().getPersonnelMarketDylansWeight() * 100);
			for (int i = 0; i < q; i++) {
				long choice = mtf.get(Compute.randomInt(Math.max(mtf.size()-1, 1)));
				if (Compute.randomInt(99) < weight) {
					if (choice == Entity.ETYPE_MECH) {
						p = c.newPerson(Person.T_MECHWARRIOR);
					} else if (choice == Entity.ETYPE_TANK) {
						if (Compute.d6() < 3) {
							p = c.newPerson(Person.T_GVEE_DRIVER);
						} else {
							p = c.newPerson(Person.T_VEE_GUNNER);
						}
					} else if (choice == Entity.ETYPE_AERO) {
						p = c.newPerson(Person.T_AERO_PILOT);
					} else if (choice == Entity.ETYPE_BATTLEARMOR) {
						p = c.newPerson(Person.T_BA);
					} else if (choice == Entity.ETYPE_INFANTRY) {
						p = c.newPerson(Person.T_INFANTRY);
					} else if (choice == Entity.ETYPE_PROTOMECH) {
						p = c.newPerson(Person.T_PROTO_PILOT);
					} else if (choice == Entity.ETYPE_CONV_FIGHTER) {
						p = c.newPerson(Person.T_CONV_PILOT);
					} else if (choice == Entity.ETYPE_SMALL_CRAFT) {
						p = c.newPerson(Person.T_SPACE_PILOT);
					} else if (choice == Entity.ETYPE_DROPSHIP) {
						int space = Compute.randomInt(Person.T_SPACE_GUNNER);
						while (space < Person.T_SPACE_PILOT) {
							space = Compute.randomInt(Person.T_SPACE_GUNNER);
						}
						p = c.newPerson(space);
					} else {
						p = c.newPerson(Person.T_NONE);
					}
				} else {
					roll = Compute.randomInt(Person.T_NUM-1);
					while (roll == Person.T_NONE) {
						roll = Compute.randomInt(Person.T_NUM-1);
					}
					p = c.newPerson(roll);
				}
				UUID id = UUID.randomUUID();
				while (null != personnelIds.get(id)) {
					id = UUID.randomUUID();
				}
				p.setId(id);
				personnel.add(p);
				personnelIds.put(id, p);
			}
			break;
		case TYPE_FMMR:
			long mft = getUnitMainForceType(c);
			int mftMod = 0;
			if (mft == Entity.ETYPE_MECH || mft == Entity.ETYPE_TANK || mft == Entity.ETYPE_INFANTRY || mft == Entity.ETYPE_BATTLEARMOR) {
				mftMod = 1;
			}
			if (c.getCalendar().get(Calendar.DAY_OF_MONTH) == 1) {
				for (int i = Person.T_NONE+1; i < Person.T_NUM; i++) {
					roll = Compute.d6(2);
					// TODO: Modifiers for hiring hall, but first needs to track the hiring hall
					if (c.getDragoonRating().equalsIgnoreCase("A") || c.getDragoonRating().equalsIgnoreCase("A*")) {
						roll += 3;
					} else if (c.getDragoonRating().equalsIgnoreCase("B")) {
						roll += 2;
					} else if (c.getDragoonRating().equalsIgnoreCase("C")) {
						roll += 1;
					} else if (c.getDragoonRating().equalsIgnoreCase("E")) {
						roll -= 1;
					} else if (c.getDragoonRating().equalsIgnoreCase("F")) {
						roll -= 2;
					}
					roll += mftMod;
					roll = Math.max(roll, 0);
					if (roll < 4) {
						q = 0;
					} else if (roll < 6) {
						q = 1;
					} else if (roll < 9) {
						q = 2;
					} else if (roll < 11) {
						q = 3;
					} else if (roll < 14) {
						q = 4;
					} else if (roll < 16) {
						q = 5;
					} else {
						q = 6;
					}
					for (int j = 0; j < q; j++) {
						p = c.newPerson(i);
						UUID id = UUID.randomUUID();
						while (null != personnelIds.get(id)) {
							id = UUID.randomUUID();
						}
						p.setId(id);
						personnel.add(p);
						personnelIds.put(id, p);
					}
				}
				report = c.getCampaignOptions().getPersonnelMarketReportRefresh();
			}
			break;
		case TYPE_STRAT_OPS:
			if (daysSinceRolled == c.getCampaignOptions().getMaintenanceCycleDays()) {
				roll = Compute.d6(2);
				if (roll == 2) { // Medical
					p = c.newPerson(Person.T_DOCTOR);
				} else if (roll == 3) { // ASF or Proto Pilot
					if (c.getFaction().isClan() && c.getCalendar().after(new GregorianCalendar(3059, 1, 1)) && Compute.d6(2) < 6) {
						p = c.newPerson(Person.T_PROTO_PILOT);
					} else {
						p = c.newPerson(Person.T_AERO_PILOT);
					}
				} else if (roll == 4 || roll == 10) { // MW
					p = c.newPerson(Person.T_MECHWARRIOR);
				} else if (roll == 5 || roll == 9) { // Vehicle Crews
					if (Compute.d6() < 3) {
						p = c.newPerson(Person.T_GVEE_DRIVER);
					} else {
						p = c.newPerson(Person.T_VEE_GUNNER);
					}
				} else if (roll == 6 || roll == 8) { // Infantry
					if (c.getFaction().isClan() && Compute.d6(2) > 3) {
						p = c.newPerson(Person.T_BA);
					} else {
						p = c.newPerson(Person.T_INFANTRY);
					}
				} else if (roll == 11) { // Tech
					int tr = Compute.randomInt(Person.T_ASTECH);
					while (tr < Person.T_MECH_TECH) {
						tr = Compute.randomInt(Person.T_ASTECH);
					}
					p = c.newPerson(tr);
				} else if (roll == 12) { // Vessel Crew
					int tr = Compute.randomInt(Person.T_SPACE_GUNNER);
					while (tr < Person.T_SPACE_PILOT) {
						tr = Compute.randomInt(Person.T_SPACE_GUNNER);
					}
					p = c.newPerson(tr);
				} else {
					p = c.newPerson(Person.T_NONE);
				}
				UUID id = UUID.randomUUID();
				while (null != personnelIds.get(id)) {
					id = UUID.randomUUID();
				}
				p.setId(id);
				personnel.add(p);
				personnelIds.put(id, p);
				report = c.getCampaignOptions().getPersonnelMarketReportRefresh();
			} else {
				incrementDaysSinceRolled();
			}
			break;
		case TYPE_RANDOM:
		default: // default is random
			q = generateRandomQuantity();
			
			for (int i = 0; i < q; i++) {
				roll = Compute.randomInt(Person.T_NUM-1);
				while (roll == Person.T_NONE) {
					roll = Compute.randomInt(Person.T_NUM-1);
				}
				p = c.newPerson(roll);
				UUID id = UUID.randomUUID();
				while (null != personnelIds.get(id)) {
					id = UUID.randomUUID();
				}
				p.setId(id);
				personnel.add(p);
				personnelIds.put(id, p);
			}
			report = c.getCampaignOptions().getPersonnelMarketReportRefresh();
		}
		
		if (report) {
			c.addReport("<a href='PERSONNEL_MARKET'>Personnel market updated</a>");
		}
	}
	
	/*
	 * Remove personnel from market on a new day
	 * The better they are, the faster they disappear
	 */
	public void removePersonnelForDay(Campaign c) {
		ArrayList<Person> toRemove = new ArrayList<Person>();
		int roll;
		
		switch (c.getCampaignOptions().getPersonnelMarketType()) {
		case TYPE_FMMR:
			if (c.getCalendar().get(Calendar.DAY_OF_MONTH) == 1) {
				personnel.clear();
			}
			break;
		case TYPE_STRAT_OPS:
			if (daysSinceRolled == c.getCampaignOptions().getMaintenanceCycleDays()) {
				personnel.clear();
			}
			break;
		case TYPE_DYLANS:
		case TYPE_RANDOM:
		default: // default is random
			for (Person p : personnel) {
				roll = Compute.d6(2);
				if (p.getExperienceLevel(false) == SkillType.EXP_ELITE
						&& roll < c.getCampaignOptions().getPersonnelMarketRandomEliteRemoval()) {
					toRemove.add(p);
				} else if (p.getExperienceLevel(false) == SkillType.EXP_VETERAN
						&& roll < c.getCampaignOptions().getPersonnelMarketRandomVeteranRemoval()) {
					toRemove.add(p);
				} else if (p.getExperienceLevel(false) == SkillType.EXP_REGULAR
						&& roll < c.getCampaignOptions().getPersonnelMarketRandomRegularRemoval()) {
					toRemove.add(p);
				} else if (p.getExperienceLevel(false) == SkillType.EXP_GREEN
						&& roll < c.getCampaignOptions().getPersonnelMarketRandomGreenRemoval()) {
					toRemove.add(p);
				} else if (p.getExperienceLevel(false) == SkillType.EXP_ULTRA_GREEN
						&& roll < c.getCampaignOptions().getPersonnelMarketRandomUltraGreenRemoval()) {
					toRemove.add(p);
				}
			}
		}
		personnel.removeAll(toRemove);
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
    
    public void incrementDaysSinceRolled() {
        daysSinceRolled++;
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

				Person p = Person.generateInstanceFromXML(wn2, c, version);

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

			// skill types might need resetting
			psn.resetSkillTypes();
		}
		
		return retVal;
	}
	
	public static String getTypeName(int type) {
		switch (type) {
		case TYPE_RANDOM:
			return "Random";
		case TYPE_DYLANS:
			return "Dylan's Method";
		case TYPE_FMMR:
			return "FM: Mercenaries Revised";
		case TYPE_STRAT_OPS:
			return "Strat Ops";
		default:
			return "ERROR: Default case reached in PersonnelMarket.getTypeName()";
		}
	}
	
	public long getUnitMainForceType(Campaign c) {
		long mostTypes = getUnitMainForceTypes(c);
		if ((mostTypes & Entity.ETYPE_MECH) != 0) {
			return Entity.ETYPE_MECH;
		} else if ((mostTypes & Entity.ETYPE_TANK) != 0) {
			return Entity.ETYPE_TANK;
		} else if ((mostTypes & Entity.ETYPE_AERO) != 0) {
			return Entity.ETYPE_AERO;
		} else if ((mostTypes & Entity.ETYPE_BATTLEARMOR) != 0) {
			return Entity.ETYPE_BATTLEARMOR;
		} else if ((mostTypes & Entity.ETYPE_INFANTRY) != 0) {
			return Entity.ETYPE_INFANTRY;
		} else if ((mostTypes & Entity.ETYPE_PROTOMECH) != 0) {
			return Entity.ETYPE_PROTOMECH;
		} else if ((mostTypes & Entity.ETYPE_CONV_FIGHTER) != 0) {
			return Entity.ETYPE_CONV_FIGHTER;
		} else if ((mostTypes & Entity.ETYPE_SMALL_CRAFT) != 0) {
			return Entity.ETYPE_SMALL_CRAFT;
		} else if ((mostTypes & Entity.ETYPE_DROPSHIP) != 0) {
			return Entity.ETYPE_DROPSHIP;
		} else {
			return Entity.ETYPE_MECH;
		}
	}
	
	public long getUnitMainForceTypes(Campaign c) {
		int mechs = c.getNumberOfUnitsByType(Entity.ETYPE_MECH);
		int ds = c.getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP);
		int sc = c.getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT);
		int cf = c.getNumberOfUnitsByType(Entity.ETYPE_CONV_FIGHTER);
		int asf = c.getNumberOfUnitsByType(Entity.ETYPE_AERO);
		int vee = c.getNumberOfUnitsByType(Entity.ETYPE_TANK, true) + c.getNumberOfUnitsByType(Entity.ETYPE_TANK);
		int inf = c.getNumberOfUnitsByType(Entity.ETYPE_INFANTRY);
		int ba = c.getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR);
		int proto = c.getNumberOfUnitsByType(Entity.ETYPE_PROTOMECH);
		int most = mechs;
		if (ds > most) {
			most = ds;
		}
		if (sc > most) {
			most = sc;
		}
		if (cf > most) {
			most = cf;
		}
		if (asf > most) {
			most = asf;
		}
		if (vee > most) {
			most = vee;
		}
		if (inf > most) {
			most = inf;
		}
		if (ba > most) {
			most = ba;
		}
		if (proto > most) {
			most = proto;
		}
		long retval = 0;
		if (most == mechs) {
			retval = retval | Entity.ETYPE_MECH;
		}
		if (most == ds) {
			retval = retval | Entity.ETYPE_DROPSHIP;
		}
		if (most == sc) {
			retval = retval | Entity.ETYPE_SMALL_CRAFT;
		}
		if (most == cf) {
			retval = retval | Entity.ETYPE_CONV_FIGHTER;
		}
		if (most == asf) {
			retval = retval | Entity.ETYPE_AERO;
		}
		if (most == vee) {
			retval = retval | Entity.ETYPE_TANK;
		}
		if (most == inf) {
			retval = retval | Entity.ETYPE_INFANTRY;
		}
		if (most == ba) {
			retval = retval | Entity.ETYPE_BATTLEARMOR;
		}
		if (most == proto) {
			retval = retval | Entity.ETYPE_PROTOMECH;
		}
		return retval;
	}
	
	public int generateRandomQuantity() {
		int roll = Compute.d6(2);
		int retval = 0;
		if (roll == 12) {
			retval = 6;
		} else if (roll < 12) {
			retval = 5;
		} else if (roll < 10) {
			retval = 4;
		} else if (roll < 8) {
			retval = 3;
		} else if (roll < 5) {
			retval = 2;
		} else if (roll < 3) {
			retval = 1;
		} else if (roll == 1) {
			retval = 0;
		}
		return retval;
	}
}
