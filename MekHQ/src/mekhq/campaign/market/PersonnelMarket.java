/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.market;

import megamek.Version;
import megamek.common.*;
import megamek.common.event.Subscribe;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.MarketNewPersonnelEvent;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.module.PersonnelMarketServiceManager;
import mekhq.module.api.PersonnelMarketMethod;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.*;

public class PersonnelMarket {
    private List<Person> personnel = new ArrayList<>();
    private PersonnelMarketMethod method;


    public static final int TYPE_RANDOM = 0;
    public static final int TYPE_DYLANS = 1;
    public static final int TYPE_FMMR = 2;
    public static final int TYPE_STRAT_OPS = 3;
    public static final int TYPE_ATB = 4;
    public static final int TYPE_NONE = 5;
    public static final int TYPE_NUM = 6;

    /* Used by AtB to track Units assigned to recruits; the key
     * is the person UUID. */
    private Map<UUID, Entity> attachedEntities = new LinkedHashMap<>();
    /* Alternate types of rolls, set by PersonnelMarketDialog */
    private boolean paidRecruitment = false;
    private PersonnelRole paidRecruitRole = PersonnelRole.MECHWARRIOR;

    public PersonnelMarket() {
        method = new PersonnelMarketDisabled();
        MekHQ.registerHandler(this);
    }

    public PersonnelMarket(Campaign c) {
        generatePersonnelForDay(c);
        setType(c.getCampaignOptions().getPersonnelMarketName());
        MekHQ.registerHandler(this);
    }

    /**
     * Sets the method for generating potential recruits for the personnel market.
     * @param key The lookup name of the market type to use.
     */
    public void setType(String key) {
        method = PersonnelMarketServiceManager.getInstance().getService(key);
        if (null == method) {
            method = new PersonnelMarketDisabled();
        }
    }

    @Subscribe
    public void handleCampaignOptionsEvent(OptionsChangedEvent ev) {
        setType(ev.getOptions().getPersonnelMarketName());
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
                personnel.addAll(newPersonnel);
                updated = true;
                MekHQ.triggerEvent(new MarketNewPersonnelEvent(newPersonnel));
            }
        }

        if (updated && c.getCampaignOptions().isPersonnelMarketReportRefresh()) {
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
                    attachedEntities.remove(p.getId());
                }
                personnel.removeAll(toRemove);
            }
        }
    }

    public void setPersonnel(List<Person> p) {
        personnel = new ArrayList<>(p);
    }

    public List<Person> getPersonnel() {
        return Collections.unmodifiableList(personnel);
    }

    public void addPerson(Person p) {
        personnel.add(p);
    }

    public void addPerson(Person p, Entity e) {
        addPerson(p);
        attachedEntities.put(p.getId(), e);
    }

    public void removePerson(Person p) {
        personnel.remove(p);
        attachedEntities.remove(p.getId());
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

    public PersonnelRole getPaidRecruitRole() {
        return paidRecruitRole;
    }

    public void setPaidRecruitRole(PersonnelRole paidRecruitRole) {
        this.paidRecruitRole = paidRecruitRole;
    }

    public void writeToXML(final PrintWriter pw, int indent, final Campaign campaign) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "personnelMarket");
        for (Person p : personnel) {
            p.writeToXML(pw, indent, campaign);
        }

        if (null != method) {
            method.writeToXML(pw, indent);
        }

        if (paidRecruitment) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "paidRecruitment", true);
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "paidRecruitType", getPaidRecruitRole().name());

        for (UUID id : attachedEntities.keySet()) {
            MHQXMLUtility.writeSimpleXMLAttributedTag(pw, indent, "entity", "id", id, attachedEntities.get(id).getShortNameRaw());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "personnelMarket");
    }

    public static PersonnelMarket generateInstanceFromXML(Node wn, Campaign c, Version version) {
        PersonnelMarket retVal = null;

        try {
            // Instantiate the correct child class, and call its parsing function.
            retVal = new PersonnelMarket();
            retVal.setType(c.getCampaignOptions().getPersonnelMarketName());

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
                        LogManager.getLogger().error("Unable to load entity: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage(), ex);
                    }
                    if (null != en) {
                        retVal.attachedEntities.put(id, en);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("paidRecruitment")) {
                    retVal.paidRecruitment = true;
                } else if (wn2.getNodeName().equalsIgnoreCase("paidRecruitType")) {
                    retVal.setPaidRecruitRole(PersonnelRole.parseFromString(wn2.getTextContent().trim()));
                } else if (null != retVal.method) {
                    retVal.method.loadFieldsFromXml(wn2);
                } else  {
                    // Error condition of sorts!
                    // Errr, what should we do here?
                    LogManager.getLogger().error("Unknown node type not loaded in Personnel nodes: " + wn2.getNodeName());
                }
            }

            // All personnel need the rank reference fixed
            for (int x = 0; x < retVal.personnel.size(); x++) {
                Person psn = retVal.personnel.get(x);

                // skill types might need resetting
                psn.resetSkillTypes();
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            LogManager.getLogger().error("", ex);
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
            case TYPE_NONE:
                return "Disabled";
            default:
                return "ERROR: Default case reached in PersonnelMarket.getTypeName()";
        }
    }

    public boolean isNone() {
        return null == method || method instanceof PersonnelMarketDisabled;
    }

    public static long getUnitMainForceType(Campaign c) {
        long mostTypes = getUnitMainForceTypes(c);
        if ((mostTypes & Entity.ETYPE_MECH) != 0) {
            return Entity.ETYPE_MECH;
        } else if ((mostTypes & Entity.ETYPE_TANK) != 0) {
            return Entity.ETYPE_TANK;
        } else if ((mostTypes & Entity.ETYPE_AEROSPACEFIGHTER) != 0) {
            return Entity.ETYPE_AEROSPACEFIGHTER;
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
        HangarStatistics hangarStats = c.getHangarStatistics();
        int mechs = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_MECH);
        int ds = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP);
        int sc = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT);
        int cf = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_CONV_FIGHTER);
        int asf = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_AEROSPACEFIGHTER);
        int vee = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_TANK, true) + hangarStats.getNumberOfUnitsByType(Entity.ETYPE_TANK);
        int inf = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_INFANTRY);
        int ba = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR);
        int proto = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_PROTOMECH);
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
            retval = retval | Entity.ETYPE_AEROSPACEFIGHTER;
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
        TargetRoll target = new TargetRoll(jumpship ? 12 : 10, "Base");
        Person adminLog = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_LOGISTICS, SkillType.S_ADMIN);
        int adminLogExp = (adminLog == null) ? SkillType.EXP_ULTRA_GREEN
                : adminLog.getSkill(SkillType.S_ADMIN).getExperienceLevel();
        for (Person p : campaign.getAdmins()) {
            if ((p.getPrimaryRole().isAdministratorLogistics() || p.getSecondaryRole().isAdministratorLogistics())
                    && p.getSkill(SkillType.S_ADMIN).getExperienceLevel() > adminLogExp) {
                adminLogExp = p.getSkill(SkillType.S_ADMIN).getExperienceLevel();
            }
        }
        target.addModifier(SkillType.EXP_REGULAR - adminLogExp, "Admin/Logistics");
        target.addModifier(IUnitRating.DRAGOON_C - campaign.getUnitRatingMod(), "Unit Rating");
        return target;
    }
}
