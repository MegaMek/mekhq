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

package mekhq.campaign.market;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.UUID;

import megamek.client.RandomUnitGenerator;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.UnitTableData;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PersonnelMarket {
	private ArrayList<Person> personnel = new ArrayList<Person>();
	private Hashtable<UUID, Person> personnelIds = new Hashtable<UUID, Person>();
	private int daysSinceRolled;

	public static final int TYPE_RANDOM = 0;
	public static final int TYPE_DYLANS = 1;
	public static final int TYPE_FMMR = 2;
	public static final int TYPE_STRAT_OPS = 3;
	public static final int TYPE_ATB = 4;
	public static final int TYPE_NUM = 5;

	/* Used by AtB to track Units assigned to recruits; the key
	 * is the person UUID. */
	private Hashtable<UUID, Entity> attachedEntities = new Hashtable<UUID, Entity>();
	/* Alternate types of rolls, set by PersonnelMarketDialog */
	private boolean paidRecruitment = false;
	private int paidRecruitType;
	private boolean shipSearch = false;

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
		Person p;
		boolean updated = false;

		if (!personnel.isEmpty()) {
			removePersonnelForDay(c);
		}

		if (paidRecruitment && c.getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
			if (c.getFinances().debit(100000, Transaction.C_MISC,
					"Paid recruitment roll", c.getDate())) {
				doPaidRecruitment(c);
				updated = true;
			} else {
				c.addReport("<html><font color=\"red\">Insufficient funds for paid recruitment.</font></html>");
			}
		} else  if (shipSearch && c.getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
			if (c.getFinances().debit(100000, Transaction.C_UNIT,
					"Ship search", c.getDate())) {
				doShipSearch(c);
			} else {
				c.addReport("<html><font color=\"red\">Insufficient funds for ship search.</font></html>");
			}
		} else {

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


				int weight = (int) (c.getCampaignOptions().getPersonnelMarketDylansWeight() * 100);
				for (int i = 0; i < q; i++) {
					long choice = mtf.get(Compute.randomInt(Math.max(mtf.size() - 1, 1)));
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
						roll = Compute.randomInt(Person.T_NUM - 1);
						while (roll == Person.T_NONE) {
							roll = Compute.randomInt(Person.T_NUM - 1);
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
					if (c.getCampaignOptions().getUseAtB()) {
						addRecruitUnit(p, c);
					}
				}
				updated = true;
				break;
			case TYPE_FMMR:
				long mft = getUnitMainForceType(c);
				int mftMod = 0;
				if (mft == Entity.ETYPE_MECH || mft == Entity.ETYPE_TANK || mft == Entity.ETYPE_INFANTRY || mft == Entity.ETYPE_BATTLEARMOR) {
					mftMod = 1;
				}
				if (c.getCalendar().get(Calendar.DAY_OF_MONTH) == 1) {
					for (int i = Person.T_NONE + 1; i < Person.T_NUM; i++) {
						roll = Compute.d6(2);
						// TODO: Modifiers for hiring hall, but first needs to track the hiring hall
						if (c.getUnitRating().equalsIgnoreCase("A") || c.getUnitRating().equalsIgnoreCase("A*")) {
							roll += 3;
						} else if (c.getUnitRating().equalsIgnoreCase("B")) {
							roll += 2;
						} else if (c.getUnitRating().equalsIgnoreCase("C")) {
							roll += 1;
						} else if (c.getUnitRating().equalsIgnoreCase("E")) {
							roll -= 1;
						} else if (c.getUnitRating().equalsIgnoreCase("F")) {
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
							if (c.getCampaignOptions().getUseAtB()) {
								addRecruitUnit(p, c);
							}
						}
					}
					updated = true;
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
					if (c.getCampaignOptions().getUseAtB()) {
						addRecruitUnit(p, c);
					}
					updated = true;
				} else {
					incrementDaysSinceRolled();
				}
				break;
			case TYPE_ATB:
				p = null;

				if (c.getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
					roll = Compute.d6(2);
					if (roll == 2) {
						switch (Compute.randomInt(4)) {
						case 0:
							p = c.newPerson(Person.T_ADMIN_COM);
							break;
						case 1:
							p = c.newPerson(Person.T_ADMIN_HR);
							break;
						case 2:
							p = c.newPerson(Person.T_ADMIN_LOG);
							break;
						case 3:
							p = c.newPerson(Person.T_ADMIN_TRA);
							break;
						}
					} else if (roll == 3 || roll == 11) {
						int r = Compute.d6();
						if (r == 1 && c.getCalendar().get(Calendar.YEAR) >
						(c.getFaction().isClan()?2870:3050)) {
							p = c.newPerson(Person.T_BA_TECH);
						} else if (r < 4) {
							p = c.newPerson(Person.T_MECHANIC);
						} else if (r == 4 && c.getCampaignOptions().getUseAero()) {
							p = c.newPerson(Person.T_AERO_TECH);
						} else {
							p = c.newPerson(Person.T_MECH_TECH);
						}
					} else if (roll == 4 || roll == 10) {
						p = c.newPerson(Person.T_MECHWARRIOR);
					} else if (roll == 5 && c.getCampaignOptions().getUseAero()) {
						p = c.newPerson(Person.T_AERO_PILOT);
					} else if (roll == 5 && c.getFaction().isClan()) {
						p = c.newPerson(Person.T_MECHWARRIOR);
					} else if (roll == 5 || roll == 10) {
						int r = Compute.d6(2);
						if (r == 2) {
							p = c.newPerson(Person.T_VTOL_PILOT);
							//Frequency based on frequency of VTOLs in Xotl 3028 Merc/General
						} else if (r <= 5) {
							p = c.newPerson(Person.T_GVEE_DRIVER);
						} else {
							p = c.newPerson(Person.T_VEE_GUNNER);
						}
					} else if (roll == 6 || roll == 8) {
						if (c.getFaction().isClan() &&
								c.getCalendar().get(Calendar.YEAR) > 2870 &&
								Compute.d6(2) > 3) {
							p = c.newPerson(Person.T_BA);
						} else if (!c.getFaction().isClan() &&
								c.getCalendar().get(Calendar.YEAR) > 3050 &&
								Compute.d6(2) > 11) {
							p = c.newPerson(Person.T_BA);
						} else {
							p = c.newPerson(Person.T_INFANTRY);
						}
					} else if (roll == 12) {
						p = c.newPerson(Person.T_DOCTOR);
					}

					if (null != p) {
						UUID id = UUID.randomUUID();
						while (null != personnelIds.get(id)) {
							id = UUID.randomUUID();
						}
						p.setId(id);
						personnel.add(p);
						personnelIds.put(id, p);
						addRecruitUnit(p, c);

						if (p.getPrimaryRole() == Person.T_GVEE_DRIVER) {
							/* Replace driver with 1-6 crew with equal
							 * chances of being drivers or gunners */
							personnel.remove(p);
							for (int i = 0; i < Compute.d6(); i++) {
								if (Compute.d6() < 4) {
									p = c.newPerson(Person.T_GVEE_DRIVER);
								} else {
									p = c.newPerson(Person.T_VEE_GUNNER);
								}
								p = c.newPerson((Compute.d6() < 4)?Person.T_GVEE_DRIVER:Person.T_VEE_GUNNER);
								int nabil = Math.max(0, p.getExperienceLevel(false) - SkillType.EXP_REGULAR);
								while (nabil > 0 && null != c.rollSPA(p.getPrimaryRole(), p)) {
									nabil--;
								}
								id = UUID.randomUUID();
								while (null != personnelIds.get(id)) {
									id = UUID.randomUUID();
								}
								p.setId(id);
								personnel.add(p);
								personnelIds.put(id, p);
							}
						}

						int adminHR = SkillType.EXP_ULTRA_GREEN;
						for (Person a :c.getAdmins()) {
							if ((a.getPrimaryRole() == Person.T_ADMIN_HR ||
									a.getSecondaryRole() == Person.T_ADMIN_HR) &&
									a.getSkill(SkillType.S_ADMIN).getExperienceLevel() > adminHR) {
								adminHR = a.getSkill(SkillType.S_ADMIN).getExperienceLevel();
							}
						}
						int gunneryMod = 0;
						int pilotingMod = 0;
						switch (adminHR) {
						case SkillType.EXP_ULTRA_GREEN:
							gunneryMod = -1;
							pilotingMod = -1;
							break;
						case SkillType.EXP_GREEN:
							if (Compute.d6() < 4) {
								gunneryMod = -1;
							} else {
								pilotingMod = -1;
							}
							break;
						case SkillType.EXP_VETERAN:
							if (Compute.d6() < 4) {
								gunneryMod = 1;
							} else {
								pilotingMod = 1;
							}
							break;
						case SkillType.EXP_ELITE:
							gunneryMod = 1;
							pilotingMod = 1;
						}

						switch (p.getPrimaryRole()) {
						case Person.T_MECHWARRIOR:
							adjustSkill(p, SkillType.S_GUN_MECH, gunneryMod);
							adjustSkill(p, SkillType.S_PILOT_MECH, pilotingMod);
							break;
						case Person.T_GVEE_DRIVER:
							adjustSkill(p, SkillType.S_PILOT_GVEE, pilotingMod);
							break;
						case Person.T_NVEE_DRIVER:
							adjustSkill(p, SkillType.S_PILOT_NVEE, pilotingMod);
							break;
						case Person.T_VTOL_PILOT:
							adjustSkill(p, SkillType.S_PILOT_VTOL, pilotingMod);
							break;
						case Person.T_VEE_GUNNER:
							adjustSkill(p, SkillType.S_GUN_VEE, gunneryMod);
							break;
						case Person.T_AERO_PILOT:
							adjustSkill(p, SkillType.S_GUN_AERO, gunneryMod);
							adjustSkill(p, SkillType.S_PILOT_AERO, pilotingMod);
							break;
						case Person.T_INFANTRY:
							adjustSkill(p, SkillType.S_SMALL_ARMS, gunneryMod);
							adjustSkill(p, SkillType.S_ANTI_MECH, pilotingMod);
							break;
						case Person.T_BA:
							adjustSkill(p, SkillType.S_GUN_BA, gunneryMod);
							adjustSkill(p, SkillType.S_ANTI_MECH, pilotingMod);
							break;
						case Person.T_PROTO_PILOT:
							adjustSkill(p, SkillType.S_GUN_PROTO, gunneryMod);
							break;
						}
						int nabil = Math.max(0, p.getExperienceLevel(false) - SkillType.EXP_REGULAR);
						while (nabil > 0 && null != c.rollSPA(p.getPrimaryRole(), p)) {
							nabil--;
						}
					}
					updated = true;
				}

				break;
			case TYPE_RANDOM:
			default: // default is random
				q = generateRandomQuantity();

				for (int i = 0; i < q; i++) {
					roll = Compute.randomInt(Person.T_NUM - 1);
					while (roll == Person.T_NONE) {
						roll = Compute.randomInt(Person.T_NUM - 1);
					}
					p = c.newPerson(roll);
					UUID id = UUID.randomUUID();
					while (null != personnelIds.get(id)) {
						id = UUID.randomUUID();
					}
					p.setId(id);
					personnel.add(p);
					personnelIds.put(id, p);
					updated = true;
					if (c.getCampaignOptions().getUseAtB()) {
						addRecruitUnit(p, c);
					}
				}
			}
		}

		if (updated && c.getCampaignOptions().getPersonnelMarketReportRefresh()) {
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
                    attachedEntities.clear();
                }
                break;
            case TYPE_STRAT_OPS:
                if (daysSinceRolled == c.getCampaignOptions().getMaintenanceCycleDays()) {
                    personnel.clear();
                    attachedEntities.clear();
                }
                break;
            case TYPE_ATB:
            	if (c.getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            		personnel.clear();
                    attachedEntities.clear();
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
        for (Person p : toRemove) {
        	if (attachedEntities.contains(p.getId())) {
        		attachedEntities.remove(p.getId());
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
		UUID id = UUID.randomUUID();
		while (null != personnelIds.get(id)) {
			id = UUID.randomUUID();
		}
		p.setId(id);
        personnel.add(p);
    }

    public void addPerson(Person p, Entity e) {
    	addPerson(p);
    	attachedEntities.put(p.getId(), e);
    }

    public void removePerson(Person p) {
        personnel.remove(p);
        if (attachedEntities.containsKey(p.getId())) {
        	attachedEntities.remove(p.getId());
        }
    }

    public Entity getAttachedEntity(Person p) {
    	return attachedEntities.get(p.getId());
    }

    public Entity getAttachedEntity(UUID pid) {
    	return attachedEntities.get(pid);
    }

    public void removeAttachedEntity(UUID id) {
    	attachedEntities.remove(id);
    }

    public boolean getPaidRecruitment() {
    	return paidRecruitment;
    }

    public void setPaidRecruitment(boolean pr) {
    	paidRecruitment = pr;
    }

    public boolean getShipSearch() {
    	return shipSearch;
    }

    public void setShipSearch(boolean ss) {
    	shipSearch = ss;
    }

    public int getPaidRecruitType() {
    	return paidRecruitType;
    }

    public void setPaidRecruitType(int pr) {
    	paidRecruitType = pr;
    }

    public void incrementDaysSinceRolled() {
        daysSinceRolled++;
    }

    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<personnelMarket>");
        for (Person p : personnel) {
            p.writeToXml(pw1, indent + 1);
        }
        for (UUID id : attachedEntities.keySet()) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<entity id=\"" + id.toString() + "\">"
                    + attachedEntities.get(id).getShortNameRaw()
                    + "</entity>");
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
            	if (wn2.getNodeType() != Node.ELEMENT_NODE) {
            		continue;
            	}

            	if (wn2.getNodeName().equalsIgnoreCase("person")) {
            		Person p = Person.generateInstanceFromXML(wn2, c, version);

            		if (p != null) {
            			retVal.personnel.add(p);
               		}
            	} else if (wn2.getNodeName().equalsIgnoreCase("entity")) {
                    UUID id = UUID.fromString(wn2.getAttributes().getNamedItem("id").getTextContent());
                    MechSummary ms = MechSummaryCache.getInstance().getMech(wn2.getTextContent());
                    Entity en = null;
        			try {
        				en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        			} catch (EntityLoadingException ex) {
        	            en = null;
        	            MekHQ.logError("Unable to load entity: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage());
        	            MekHQ.logError(ex);
        			}
                    if (null != en) {
                    	retVal.attachedEntities.put(id, en);
                    }
          		} else  {
            		// Error condition of sorts!
            		// Errr, what should we do here?
            		MekHQ.logMessage("Unknown node type not loaded in Personnel nodes: "
            				+ wn2.getNodeName());

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
            case TYPE_ATB:
            	return "Against the Bot";
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

    private void doPaidRecruitment(Campaign c) {
    	int mod;
    	switch (paidRecruitType) {
    	case Person.T_MECHWARRIOR:
    		mod = -2;
    		break;
    	case Person.T_INFANTRY:
    		mod = 2;
    		break;
    	case Person.T_MECH_TECH:
    	case Person.T_AERO_TECH:
    	case Person.T_MECHANIC:
    	case Person.T_BA_TECH:
    	case Person.T_DOCTOR:
    		mod = 1;
    		break;
    	default:
    		mod = 0;
    	}

    	mod += c.getUnitRatingMod() - IUnitRating.DRAGOON_C;
		if (c.getFinances().isInDebt()) {
			mod -= 3;
		}

		int q = 0;
		int r = Compute.d6(2) + mod;
		if (r > 15) {
			q = 6;
		} else if (r > 12) {
			q = 5;
		} else if (r > 10) {
			q = 4;
		} else if (r > 8) {
			q = 3;
		} else if (r > 5) {
			q = 2;
		} else if (r > 3) {
			q = 1;
		}
		for (int i = 0; i < q; i++) {
			int type = paidRecruitType;
			if ((type == Person.T_GVEE_DRIVER || type == Person.T_NVEE_DRIVER ||
					type == Person.T_VTOL_PILOT) && Compute.d6() > 2) {
				type = Person.T_VEE_GUNNER;
			}
            Person p = c.newPerson(type);
            UUID id = UUID.randomUUID();
            while (null != personnelIds.get(id)) {
                id = UUID.randomUUID();
            }
            p.setId(id);
            personnel.add(p);
            personnelIds.put(id, p);
            if (c.getCampaignOptions().getUseAtB()) {
            	addRecruitUnit(p, c);
            }
		}
    }

    private void doShipSearch(Campaign c) {
    	int adminLog = SkillType.EXP_ULTRA_GREEN;
    	for (Person p : c.getAdmins()) {
			if ((p.getPrimaryRole() == Person.T_ADMIN_LOG ||
					p.getSecondaryRole() == Person.T_ADMIN_LOG) &&
					p.getSkill(SkillType.S_ADMIN).getExperienceLevel() > adminLog) {
				adminLog = p.getSkill(SkillType.S_ADMIN).getExperienceLevel();
			}
    	}
    	int mod = adminLog - SkillType.EXP_REGULAR;
		mod += c.getUnitRatingMod() -
				IUnitRating.DRAGOON_C;

		int roll = Compute.d6(2) + mod;
		Person p = null;
		if (paidRecruitType == Person.T_NAVIGATOR && roll >= 12) {
			p = c.newPerson(Person.T_NAVIGATOR);
		} else if (paidRecruitType == Person.T_SPACE_PILOT && roll >= 10) {
			p = c.newPerson(Person.T_SPACE_PILOT);
		}
		//TODO: ships available for long-term higher with mos == 0
		if (null != p) {
            UUID id = UUID.randomUUID();
            while (null != personnelIds.get(id)) {
                id = UUID.randomUUID();
            }
            p.setId(id);
            personnel.add(p);
            personnelIds.put(id, p);
            if (c.getCampaignOptions().getUseAtB()) {
            	addRecruitUnit(p, c, true);
            }
		}
    }

    private void addRecruitUnit(Person p, Campaign c) {
    	addRecruitUnit(p, c, false);
    }

    private void addRecruitUnit(Person p, Campaign c, boolean spaceShips) {
    	int unitType;
    	switch (p.getPrimaryRole()) {
    	case Person.T_MECHWARRIOR:
    		unitType = UnitTableData.UNIT_MECH;
    		break;
    	case Person.T_GVEE_DRIVER:
    	case Person.T_VEE_GUNNER:
    	case Person.T_VTOL_PILOT:
    		return;
     	case Person.T_AERO_PILOT:
    		if (!c.getCampaignOptions().getAeroRecruitsHaveUnits()) {
    			return;
    		}
    		unitType = UnitTableData.UNIT_AERO;
    		break;
    	case Person.T_SPACE_CREW:
    	case Person.T_SPACE_GUNNER:
    	case Person.T_SPACE_PILOT:
    		if (spaceShips) {
    			unitType = UnitTableData.UNIT_DROPSHIP;
    		} else {
    			return;
    		}
    		break;
    	case Person.T_NAVIGATOR:
    		if (spaceShips) {
    			unitType = -1;
    		} else {
    			return;
    		}
    		break;
    	case Person.T_INFANTRY:
    		unitType = UnitTableData.UNIT_INFANTRY;
    		break;
    	case Person.T_BA:
    		unitType = UnitTableData.UNIT_BATTLEARMOR;
    		break;
    	case Person.T_PROTO_PILOT:
    		unitType = UnitTableData.UNIT_PROTOMECH;
    		break;
    	default:
    		return;
    	}

    	int weight = 0;
    	if (unitType <= UnitTableData.UNIT_AERO) {
			int roll = Compute.d6(2);
	    	if (roll < 8) {
	    		return;
	    	}
	    	if (roll < 10) {
	    		weight = UnitTableData.WT_LIGHT;
	    	} else if (roll < 12) {
	    		weight = UnitTableData.WT_MEDIUM;
	    	} else {
	    		weight = UnitTableData.WT_HEAVY;
	    	}
    	}
    	Entity en = null;

    	String faction = getRecruitFaction(c);
		MechSummary ms = null;

    	if (unitType < 0) {
    		//TODO: add other JumpShips
    		String name;
    		int roll = Compute.d6();
    		if (roll == 1) {
    			name = "Scout Jumpship";
    		} else if (roll < 4) {
    			name = "Merchant Jumpship";
    		} else {
    			name = "Invader Jumpship";
    		}
    		ms = MechSummaryCache.getInstance().getMech(name);
    	} else {
    		UnitTableData.FactionTables ft = UnitTableData.getInstance().getBestRAT(c.getCampaignOptions().getRATs(),
    				c.getCalendar().get(Calendar.YEAR),
    				faction, unitType);
    		if (null == ft) {
    			//Most likely proto pilot for IS faction
    			return;
    		}
    		String rat = ft.getTable(unitType, weight, UnitTableData.QUALITY_F);
    		if (null != rat) {
    			RandomUnitGenerator.getInstance().setChosenRAT(rat);
    			ArrayList<MechSummary> msl = RandomUnitGenerator.getInstance().generate(1);
    			if (msl.size() > 0) {
    				ms = msl.get(0);
    			}
    		}
    	}
    	if (null != ms) {
    		if (Faction.getFaction(faction).isClan() && ms.getName().matches(".*Platoon.*")) {
				String name = "Clan " + ms.getName().replaceAll("Platoon", "Point");
				ms = MechSummaryCache.getInstance().getMech(name);
				System.out.println("looking for Clan infantry " + name);
			}
			try {
				en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
			} catch (EntityLoadingException ex) {
	            en = null;
	            MekHQ.logError("Unable to load entity: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage());
	            MekHQ.logError(ex);
			}
		}

		if (null != en) {
			attachedEntities.put(p.getId(), en);
			/* adjust vehicle pilot roles according to the type of vehicle rolled */
			if ((en.getEntityType() & Entity.ETYPE_TANK) != 0) {
				if (en.getMovementMode() == EntityMovementMode.TRACKED ||
						en.getMovementMode() == EntityMovementMode.WHEELED ||
						en.getMovementMode() == EntityMovementMode.HOVER ||
						en.getMovementMode() == EntityMovementMode.WIGE) {
					if (p.getPrimaryRole() == Person.T_VTOL_PILOT) {
						swapSkills(p, SkillType.S_PILOT_VTOL, SkillType.S_PILOT_GVEE);
						p.setPrimaryRole(Person.T_GVEE_DRIVER);
					}
					if (p.getPrimaryRole() == Person.T_NVEE_DRIVER) {
						swapSkills(p, SkillType.S_PILOT_NVEE, SkillType.S_PILOT_GVEE);
						p.setPrimaryRole(Person.T_GVEE_DRIVER);
					}
				} else if (en.getMovementMode() == EntityMovementMode.VTOL) {
					if (p.getPrimaryRole() == Person.T_GVEE_DRIVER) {
						swapSkills(p, SkillType.S_PILOT_GVEE, SkillType.S_PILOT_VTOL);
						p.setPrimaryRole(Person.T_VTOL_PILOT);
					}
					if (p.getPrimaryRole() == Person.T_NVEE_DRIVER) {
						swapSkills(p, SkillType.S_PILOT_NVEE, SkillType.S_PILOT_VTOL);
						p.setPrimaryRole(Person.T_VTOL_PILOT);
					}
				} else if (en.getMovementMode() == EntityMovementMode.NAVAL ||
						en.getMovementMode() == EntityMovementMode.HYDROFOIL ||
						en.getMovementMode() == EntityMovementMode.SUBMARINE) {
					if (p.getPrimaryRole() == Person.T_GVEE_DRIVER) {
						swapSkills(p, SkillType.S_PILOT_GVEE, SkillType.S_PILOT_NVEE);
						p.setPrimaryRole(Person.T_NVEE_DRIVER);
					}
					if (p.getPrimaryRole() == Person.T_VTOL_PILOT) {
						swapSkills(p, SkillType.S_PILOT_VTOL, SkillType.S_PILOT_NVEE);
						p.setPrimaryRole(Person.T_NVEE_DRIVER);
					}
				}
			}
		}
    }

    private void swapSkills(Person p, String skill1, String skill2) {
    	int s1 = p.hasSkill(skill1)?p.getSkill(skill1).getLevel():0;
    	int b1 = p.hasSkill(skill1)?p.getSkill(skill1).getBonus():0;
    	int s2 = p.hasSkill(skill2)?p.getSkill(skill2).getLevel():0;
    	int b2 = p.hasSkill(skill2)?p.getSkill(skill2).getBonus():0;
    	p.addSkill(skill1, s2, b2);
    	p.addSkill(skill2, s1, b1);
    	if (p.getSkill(skill1).getLevel() == 0) {
    		p.removeSkill(skill1);
    	}
    	if (p.getSkill(skill2).getLevel() == 0) {
    		p.removeSkill(skill2);
    	}
    }

    public void adjustSkill (Person p, String skillName, int mod) {
    	if (p.getSkill(skillName) == null) {
    		return;
    	}
    	if (mod > 0) {
    		p.improveSkill(skillName);
    	}
    	if (mod < 0) {
    		int lvl = p.getSkill(skillName).getLevel() + mod;
    		p.getSkill(skillName).setLevel(Math.max(lvl, 0));
    	}
    }

    public static String getRecruitFaction(Campaign c) {
    	if (!c.getFactionCode().equals("MERC")) {
    		return c.getFactionCode();
    	}
    	if (c.getCalendar().get(Calendar.YEAR) > 3055 && Compute.randomInt(20) == 0) {
    		ArrayList<String> clans = new ArrayList<String>();
    		for (String f : RandomFactionGenerator.getInstance().getCurrentFactions()) {
    			if (Faction.getFaction(f).isClan()) {
    				clans.add(f);
    			}
    		}
    		return clans.get(Compute.randomInt(clans.size()));
    	}
    	return RandomFactionGenerator.getInstance().getEmployer();
    }
}
