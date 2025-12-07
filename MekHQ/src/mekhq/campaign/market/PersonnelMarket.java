/*
 * Copyright (c) 2013 Dylan Myers <dylan at dylanspcs.com>. All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.market;

import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ULTRA_GREEN;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.Version;
import megamek.common.event.Subscribe;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.MarketNewPersonnelEvent;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.module.PersonnelMarketServiceManager;
import mekhq.module.api.PersonnelMarketMethod;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Deprecated(since = "0.50.06")
public class PersonnelMarket {
    private static final MMLogger logger = MMLogger.create(PersonnelMarket.class);

    private List<Person> personnel = new ArrayList<>();
    private PersonnelMarketMethod method;

    public static final int TYPE_RANDOM = 0;
    public static final int TYPE_DYLANS = 1;
    public static final int TYPE_FMMR = 2;
    public static final int TYPE_CAMPAIGN_OPS = 3;
    public static final int TYPE_ATB = 4;
    public static final int TYPE_NONE = 5;
    public static final int TYPE_NUM = 6;

    /*
     * Used by AtB to track Units assigned to recruits; the key
     * is the person UUID.
     */
    private final Map<UUID, Entity> attachedEntities = new LinkedHashMap<>();
    /* Alternate types of rolls, set by PersonnelMarketDialog */
    private boolean paidRecruitment = false;
    private PersonnelRole paidRecruitRole = PersonnelRole.MEKWARRIOR;

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
     *
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

    /**
     * Generates new personnel for the current day, adding them to the campaign's personnel market if applicable. The
     * method handles removing outdated personnel, checking hiring hall and capital conditions, and updating the
     * personnel pool.
     * <p>
     * The process includes:
     * <ul>
     *     <li>Removing personnel already listed for the day.</li>
     *     <li>Clearing the personnel list entirely if it does not meet hiring hall or capital
     *     requirements (based on campaign settings).</li>
     *     <li>Generating new personnel through the associated generation method, if applicable.</li>
     *     <li>Triggering updates in the personnel market with an event.</li>
     *     <li>Optionally generating a personnel market report, if this is enabled in campaign options.</li>
     * </ul>
     *
     * @param campaign The {@link Campaign} related to the current gameplay context. Used to determine the campaign's
     *                 current planetary system, date, settings, factions, and more.
     */
    public void generatePersonnelForDay(Campaign campaign) {
        PlanetarySystem location = campaign.getLocation().getCurrentSystem();
        LocalDate today = campaign.getLocalDate();

        // Determine conditions
        boolean isOnPlanet = campaign.getLocation().isOnPlanet();
        boolean useCapitalsHiringHallsOnly = campaign.getCampaignOptions().isUsePersonnelHireHiringHallOnly();
        boolean isHiringHall = location.isHiringHall(today);
        boolean isCapital = location.getFactionSet(today)
                                  .stream()
                                  .anyMatch(faction -> location.equals(faction.getStartingPlanet(campaign, today)));

        // Remove existing personnel for the day
        if (!personnel.isEmpty()) {
            removePersonnelForDay(campaign);

            // If only capitals/hiring halls are allowed and the location fails both conditions, clear personnel
            if (!isOnPlanet || (useCapitalsHiringHallsOnly && !isHiringHall && !isCapital)) {
                removeAll();
                return;
            }
        }

        // Generate new personnel if `method` is defined and conditions allow
        boolean updated = false;
        if (method != null && (!useCapitalsHiringHallsOnly || isHiringHall || isCapital)) {
            List<Person> newPersonnel = method.generatePersonnelForDay(campaign);
            if (newPersonnel != null && !newPersonnel.isEmpty()) {
                personnel.addAll(newPersonnel);
                updated = true;

                // Notify about new personnel in the market
                MekHQ.triggerEvent(new MarketNewPersonnelEvent(newPersonnel));
            }
        }

        // Skip further processing if no personnel are present
        if (personnel.isEmpty()) {
            return;
        }

        // Generate campaign reports if the personnel market was updated
        if (updated && campaign.getCampaignOptions().isPersonnelMarketReportRefresh()) {
            generatePersonnelReport(campaign);
        }
    }

    /**
     * Generates and adds a report to the campaign about new personnel added to the personnel market.
     * <p>
     * If the corresponding option is enabled in the campaign settings, this function produces a detailed, user-facing
     * report about the most notable individual in the new personnel pool. The report includes their experience level,
     * primary role, and name.
     * <p>
     * The generated report is in HTML format and provides easy access to the personnel market interface.
     *
     * @param campaign The {@link Campaign} to which the report will be added.
     */
    private void generatePersonnelReport(Campaign campaign) {
        StringBuilder report = new StringBuilder();
        report.append("<a href='PERSONNEL_MARKET'>Personnel market updated</a>");

        if (campaign.getCampaignOptions().getPersonnelMarketName().equals("Campaign Ops")) {
            report.append(':');

            // Add details about the first personnel's experience, primary role, and name
            Person person = personnel.get(0);
            int experienceLevel = person.getExperienceLevel(campaign, false);
            String expLevel = SkillType.getExperienceLevelName(experienceLevel);

            if (expLevel.equals("Elite") || expLevel.equals("Ultra-Green")) {
                report.append("<br>An ");
            } else {
                report.append("<br>A ");
            }

            report.append("<b>")
                  .append(SkillType.getColoredExperienceLevelName(experienceLevel))
                  .append(' ')
                  .append(person.getPrimaryRole())
                  .append("</b>")
                  .append(" named ")
                  .append(person.getFullName())
                  .append(" is available.");
        }

        campaign.addReport(GENERAL, report.toString());
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

    /**
     * Removes all personnel from the market and their attached units.
     */
    public void removeAll() {
        personnel.clear();
        attachedEntities.clear();
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
     *
     * @param pid The recruit's id
     * @param en  The Entity to assign
     */
    public void addAttachedEntity(UUID pid, Entity en) {
        attachedEntities.put(pid, en);
    }

    /**
     * Get the Entity associated with a recruit, if any
     *
     * @param p The recruit
     *
     * @return The Entity associated with the recruit, or null if there is none
     */
    public Entity getAttachedEntity(Person p) {
        return attachedEntities.get(p.getId());
    }

    /**
     * Get the Entity associated with a recruit, if any
     *
     * @param pid The id of the recruit
     *
     * @return The Entity associated with the recruit, or null if there is none
     */
    public Entity getAttachedEntity(UUID pid) {
        return attachedEntities.get(pid);
    }

    /**
     * Clears the <code>Entity</code> associated with a recruit
     *
     * @param pid The recruit's id
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
            MHQXMLUtility.writeSimpleXMLAttributedTag(pw,
                  indent,
                  "entity",
                  "id",
                  id,
                  attachedEntities.get(id).getShortNameRaw());
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
                    MekSummary ms = MekSummaryCache.getInstance().getMek(wn2.getTextContent());
                    Entity en = null;
                    try {
                        en = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                    } catch (EntityLoadingException ex) {
                        logger.error(ex, "Unable to load entity: {}: {}: {}",
                              ms.getSourceFile(),
                              ms.getEntryName(),
                              ex.getMessage());
                    }
                    if (null != en) {
                        retVal.attachedEntities.put(id, en);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("paidRecruitment")) {
                    retVal.paidRecruitment = true;
                } else if (wn2.getNodeName().equalsIgnoreCase("paidRecruitType")) {
                    retVal.setPaidRecruitRole(PersonnelRole.fromString(wn2.getTextContent().trim()));
                } else if (null != retVal.method) {
                    retVal.method.loadFieldsFromXml(wn2);
                } else {
                    // Error condition of sorts!
                    // Errr, what should we do here?
                    logger.error("Unknown node type not loaded in Personnel nodes: {}", wn2.getNodeName());
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
            logger.error("", ex);
        }

        return retVal;
    }

    public static String getTypeName(int type) {
        return switch (type) {
            case TYPE_RANDOM -> "Random";
            case TYPE_DYLANS -> "Dylan's Method";
            case TYPE_FMMR -> "FM: Mercenaries Revised";
            case TYPE_CAMPAIGN_OPS -> "Campaign Ops";
            case TYPE_ATB -> "Against the Bot";
            case TYPE_NONE -> "Disabled";
            default -> "ERROR: Default case reached in PersonnelMarket.getTypeName()";
        };
    }

    public boolean isNone() {
        return null == method || method instanceof PersonnelMarketDisabled;
    }

    public static long getUnitMainForceType(Campaign c) {
        long mostTypes = getUnitMainForceTypes(c);
        if ((mostTypes & Entity.ETYPE_MEK) != 0) {
            return Entity.ETYPE_MEK;
        } else if ((mostTypes & Entity.ETYPE_TANK) != 0) {
            return Entity.ETYPE_TANK;
        } else if ((mostTypes & Entity.ETYPE_AEROSPACE_FIGHTER) != 0) {
            return Entity.ETYPE_AEROSPACE_FIGHTER;
        } else if ((mostTypes & Entity.ETYPE_BATTLEARMOR) != 0) {
            return Entity.ETYPE_BATTLEARMOR;
        } else if ((mostTypes & Entity.ETYPE_INFANTRY) != 0) {
            return Entity.ETYPE_INFANTRY;
        } else if ((mostTypes & Entity.ETYPE_PROTOMEK) != 0) {
            return Entity.ETYPE_PROTOMEK;
        } else if ((mostTypes & Entity.ETYPE_CONV_FIGHTER) != 0) {
            return Entity.ETYPE_CONV_FIGHTER;
        } else if ((mostTypes & Entity.ETYPE_SMALL_CRAFT) != 0) {
            return Entity.ETYPE_SMALL_CRAFT;
        } else if ((mostTypes & Entity.ETYPE_DROPSHIP) != 0) {
            return Entity.ETYPE_DROPSHIP;
        } else {
            return Entity.ETYPE_MEK;
        }
    }

    public static long getUnitMainForceTypes(Campaign c) {
        HangarStatistics hangarStats = c.getHangarStatistics();
        int meks = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_MEK);
        int ds = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_DROPSHIP);
        int sc = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_SMALL_CRAFT);
        int cf = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_CONV_FIGHTER);
        int asf = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_AEROSPACE_FIGHTER);
        int vee = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_TANK, true) +
                        hangarStats.getNumberOfUnitsByType(Entity.ETYPE_TANK);
        int inf = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_INFANTRY);
        int ba = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_BATTLEARMOR);
        int proto = hangarStats.getNumberOfUnitsByType(Entity.ETYPE_PROTOMEK);
        int most = meks;
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
        long retVal = 0;
        if (most == meks) {
            retVal = retVal | Entity.ETYPE_MEK;
        }
        if (most == ds) {
            retVal = retVal | Entity.ETYPE_DROPSHIP;
        }
        if (most == sc) {
            retVal = retVal | Entity.ETYPE_SMALL_CRAFT;
        }
        if (most == cf) {
            retVal = retVal | Entity.ETYPE_CONV_FIGHTER;
        }
        if (most == asf) {
            retVal = retVal | Entity.ETYPE_AEROSPACE_FIGHTER;
        }
        if (most == vee) {
            retVal = retVal | Entity.ETYPE_TANK;
        }
        if (most == inf) {
            retVal = retVal | Entity.ETYPE_INFANTRY;
        }
        if (most == ba) {
            retVal = retVal | Entity.ETYPE_BATTLEARMOR;
        }
        if (most == proto) {
            retVal = retVal | Entity.ETYPE_PROTOMEK;
        }
        return retVal;
    }

    /**
     * @deprecated Unused. Seems to be an unused alternative to
     *       {@link mekhq.campaign.againstTheBot.AtBConfiguration#shipSearchTargetRoll(int, Campaign)}
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public TargetRoll getShipSearchTarget(Campaign campaign, boolean jumpship) {
        TargetRoll target = new TargetRoll(jumpship ? 12 : 10, "Base");
        Person logisticsAdmin = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_LOGISTICS, SkillType.S_ADMIN);

        int experienceLevel = EXP_ULTRA_GREEN;
        if (logisticsAdmin != null && logisticsAdmin.hasSkill(S_ADMIN)) {
            Skill skill = logisticsAdmin.getSkill(S_ADMIN);
            SkillModifierData skillModifierData = logisticsAdmin.getSkillModifierData();
            experienceLevel = skill.getExperienceLevel(skillModifierData);
        }

        target.addModifier(SkillType.EXP_REGULAR - experienceLevel, "Admin/Logistics");
        target.addModifier(IUnitRating.DRAGOON_C - campaign.getAtBUnitRatingMod(), "Unit Rating");
        return target;
    }
}
