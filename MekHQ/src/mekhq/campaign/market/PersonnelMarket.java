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
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.TargetRoll;
import megamek.common.event.Subscribe;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.MarketNewPersonnelEvent;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.rating.IUnitRating;
import mekhq.module.PersonnelMarketServiceManager;
import mekhq.module.api.PersonnelMarketMethod;

public class PersonnelMarket {
	private ArrayList<Person> personnel = new ArrayList<Person>();
	private Hashtable<UUID, Person> personnelIds = new Hashtable<UUID, Person>();
	private PersonnelMarketMethod method;

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

	public PersonnelMarket() {
	    method = new PersonnelMarketRandom();
        MekHQ.registerHandler(this);
	}

	public PersonnelMarket(Campaign c) {
		generatePersonnelForDay(c);
		setType(c.getCampaignOptions().getPersonnelMarketType());
		MekHQ.registerHandler(this);
	}
	
	/**
	 * Sets the method for generating potential recruits for the personnel market.
	 * @param type  The lookup name of the market type to use.
	 */
	public void setType(String key) {
	    method = PersonnelMarketServiceManager.getInstance().getService(key);
	    if (null == method) {
	        method = new PersonnelMarketRandom();
	    }
	}
	
	@Subscribe
	public void handleCampaignOptionsEvent(OptionsChangedEvent ev) {
	    setType(ev.getOptions().getPersonnelMarketType());
	}

	/*
	 * Generate new personnel to be added to the
	 * market availability pool
	 */
	public void generatePersonnelForDay(Campaign c) {
		boolean updated = false;

		if (!personnel.isEmpty()) {
			removePersonnelForDay(c);
		}

		if (null != method) {
		    List<Person> newPersonnel = method.generatePersonnelForDay(c);
		    if ((null != newPersonnel) && !newPersonnel.isEmpty()) {
    		    for (Person recruit : newPersonnel) {
                    personnel.add(recruit);
                    personnelIds.put(recruit.getId(), recruit);
    		    }
    		    updated = true;
                MekHQ.triggerEvent(new MarketNewPersonnelEvent(newPersonnel));
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
        if (null != method) {
            List<Person> toRemove = method.removePersonnelForDay(c, personnel);
            if (null != toRemove) {
                for (Person p : toRemove) {
                	if (attachedEntities.contains(p.getId())) {
                		attachedEntities.remove(p.getId());
                	}
                }
                personnel.removeAll(toRemove);
            }
        }
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
    
    /**
     * Assign an <code>Entity</code> to a recruit
     * @param pid  The recruit's id
     * @param en   The Entity to assign
     */
    public void addAttachedEntity(UUID pid, Entity en) {
        attachedEntities.put(pid, en);
    }

    /**
     * Get the Entity associated with a recruit, if any
     * @param p  The recruit
     * @return   The Entity associated with the recruit, or null if there is none
     */
    public Entity getAttachedEntity(Person p) {
    	return attachedEntities.get(p.getId());
    }

    /**
     * Get the Entity associated with a recruit, if any
     * @param pid The id of the recruit
     * @return    The Entity associated with the recruit, or null if there is none
     */
    public Entity getAttachedEntity(UUID pid) {
    	return attachedEntities.get(pid);
    }

    /**
     * Clears the <code>Entity</code> associated with a recruit
     * @param pid  The recruit's id
     */
    public void removeAttachedEntity(UUID pid) {
    	attachedEntities.remove(pid);
    }

    public boolean getPaidRecruitment() {
    	return paidRecruitment;
    }

    public void setPaidRecruitment(boolean pr) {
    	paidRecruitment = pr;
    }

    public int getPaidRecruitType() {
    	return paidRecruitType;
    }

    public void setPaidRecruitType(int pr) {
    	paidRecruitType = pr;
    }

    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<personnelMarket>");
        for (Person p : personnel) {
            p.writeToXml(pw1, indent + 1);
        }
        if (null != method) {
            method.writeToXml(pw1, indent);
        }
        if (paidRecruitment) {
        	pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<paidRecruitment/>");
        }
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "paidRecruitType", paidRecruitType);
        
        for (UUID id : attachedEntities.keySet()) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<entity id=\"" + id.toString() + "\">"
                    + attachedEntities.get(id).getShortNameRaw()
                    + "</entity>");
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</personnelMarket>");
    }

    public static PersonnelMarket generateInstanceFromXML(Node wn, Campaign c, Version version) {
        final String METHOD_NAME = "generateInstanceFromXML(Node,Campaign,Version)"; //$NON-NLS-1$
        
        PersonnelMarket retVal = null;

        try {
            // Instantiate the correct child class, and call its parsing function.
            retVal = new PersonnelMarket();
            retVal.setType(c.getCampaignOptions().getPersonnelMarketType());

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
                        MekHQ.getLogger().log(PersonnelMarket.class, METHOD_NAME, LogLevel.ERROR,
                                "Unable to load entity: " + ms.getSourceFile() + ": " //$NON-NLS-1$
                                        + ms.getEntryName() + ": " + ex.getMessage()); //$NON-NLS-1$
                        MekHQ.getLogger().error(PersonnelMarket.class, METHOD_NAME, ex);
        			}
                    if (null != en) {
                    	retVal.attachedEntities.put(id, en);
                    }
            	} else if (wn2.getNodeName().equalsIgnoreCase("paidRecruitment")) {
            		retVal.paidRecruitment = true;
            	} else if (wn2.getNodeName().equalsIgnoreCase("paidRecruitType")) {
            		retVal.paidRecruitType = Integer.parseInt(wn2.getTextContent());
            	} else if (null != retVal.method) {
            	    retVal.method.loadFieldsFromXml(wn2);
          		} else  {
            		// Error condition of sorts!
            		// Errr, what should we do here?
                    MekHQ.getLogger().log(PersonnelMarket.class, METHOD_NAME, LogLevel.ERROR,
                            "Unknown node type not loaded in Personnel nodes: " //$NON-NLS-1$
            				+ wn2.getNodeName());

            	}

            }
        } catch (Exception ex) {
        	// Errrr, apparently either the class name was invalid...
        	// Or the listed name doesn't exist.
        	// Doh!
            MekHQ.getLogger().error(PersonnelMarket.class, METHOD_NAME, ex);
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
    

    public static long getUnitMainForceType(Campaign c) {
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

    public static long getUnitMainForceTypes(Campaign c) {
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
    
    public TargetRoll getShipSearchTarget(Campaign campaign, boolean jumpship) {
    	TargetRoll target = new TargetRoll(jumpship?12:10, "Base");
		Person adminLog = campaign.findBestInRole(Person.T_ADMIN_LOG, SkillType.S_ADMIN);
		int adminLogExp = (adminLog == null)?SkillType.EXP_ULTRA_GREEN:adminLog.getSkill(SkillType.S_ADMIN).getExperienceLevel();
    	for (Person p : campaign.getAdmins()) {
			if ((p.getPrimaryRole() == Person.T_ADMIN_LOG ||
					p.getSecondaryRole() == Person.T_ADMIN_LOG) &&
					p.getSkill(SkillType.S_ADMIN).getExperienceLevel() > adminLogExp) {
				adminLogExp = p.getSkill(SkillType.S_ADMIN).getExperienceLevel();
			}
    	}
    	target.addModifier(SkillType.EXP_REGULAR - adminLogExp, "Admin/Logistics");
    	target.addModifier(IUnitRating.DRAGOON_C - campaign.getUnitRatingMod(),
    			"Unit Rating");
    	return target;
    }

}
