/*
 * Lance.java
 *
 * Copyright (c) 2011 - Carl Spain. All rights reserved.
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.force;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.Infantry;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.OrganizationChangedEvent;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import static megamek.common.Entity.ETYPE_AEROSPACEFIGHTER;
import static megamek.common.Entity.ETYPE_MEK;
import static megamek.common.Entity.ETYPE_PROTOMEK;
import static megamek.common.Entity.ETYPE_TANK;
import static megamek.common.EntityWeightClass.WEIGHT_ULTRA_LIGHT;
import static mekhq.campaign.force.Force.COMBAT_TEAM_OVERRIDE_NONE;
import static mekhq.campaign.force.Force.COMBAT_TEAM_OVERRIDE_TRUE;
import static mekhq.campaign.force.FormationLevel.LANCE;

/**
 * Used by Against the Bot &amp; StratCon to track additional information about each force
 * on the TO&amp;E that has at least one unit assigned. Extra info includes whether
 * the force counts as a Combat Team eligible for assignment to a scenario role
 * and what the assignment is on which contract.
 *
 * @author Neoancient
 */
public class CombatTeam {
    private static final MMLogger logger = MMLogger.create(CombatTeam.class);

    public static final int LANCE_SIZE = 4;
    public static final int STAR_SIZE = 5;
    public static final int LEVEL_II_SIZE = 6;

    public static final long ETYPE_GROUND = ETYPE_MEK |
            ETYPE_TANK | Entity.ETYPE_INFANTRY | ETYPE_PROTOMEK;

    /** Indicates a lance has no assigned mission */
    public static final int NO_MISSION = -1;

    private int forceId;
    private int missionId;
    private CombatRole role;
    private UUID commanderId;

    /**
     * Determines the standard size for a given faction. The size varies depending on whether the
     * faction is a Clan, ComStar/WoB, or others (Inner Sphere). This overloaded method defaults to
     * Lance/Star/Level II
     *
     * @param faction The {@link Faction} object for which the standard force size is to be calculated.
     * @return The standard force size, at the provided formation level, for the provided faction
     */
    public static int getStandardForceSize(Faction faction) {
        return getStandardForceSize(faction, LANCE.getDepth());
    }

    /**
     * Determines the standard size for a given faction. The size varies depending on whether the
     * faction is a Clan, ComStar/WoB, or others (Inner Sphere).
     *
     * @param faction The {@link Faction} object for which the standard force size is to be calculated.
     * @param formationLevelDepth The {@link FormationLevel} {@code Depth} from which the standard
     *                           force size is to be calculated.
     * @return The standard force size, at the provided formation level, for the provided faction
     */
    public static int getStandardForceSize(Faction faction, int formationLevelDepth) {
        int formationSize;
        if (faction.isClan() || faction.isMarianHegemony()) {
            formationSize = STAR_SIZE;
        } else if (faction.isComStarOrWoB()) {
            formationSize = LEVEL_II_SIZE;
        } else {
            formationSize = LANCE_SIZE;
        }

        if (formationLevelDepth == LANCE.getDepth()) {
            return formationSize;
        }

        formationLevelDepth++; // Lance is depth 0, so we need to add +1 to get the number of iterations

        for (int i = 0; i < formationLevelDepth; i++) {

            if (faction.isComStarOrWoB()) {
                formationSize *= 6;
            } else {
                formationSize *= 3;
            }
        }

        return formationSize;
    }

    /**
     * Default constructor
     */
    public CombatTeam() {}

    public CombatTeam(int forceId, Campaign campaign) {
        this.forceId = forceId;
        role = CombatRole.RESERVE;
        missionId = -1;
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            missionId = ((contract.getParentContract() == null)
                    ? contract
                    : contract.getParentContract()).getId();
        }
        commanderId = findCommander(this.forceId, campaign);
    }

    public int getForceId() {
        return forceId;
    }

    public int getMissionId() {
        return missionId;
    }

    public AtBContract getContract(Campaign campaign) {
        return (AtBContract) campaign.getMission(missionId);
    }

    public void setContract(AtBContract atBContract) {
        if (null == atBContract) {
            missionId = NO_MISSION;
        } else {
            missionId = atBContract.getId();
        }
    }

    public CombatRole getRole() {
        return role;
    }

    public void setRole(CombatRole role) {
        this.role = role;
    }

    public UUID getCommanderId() {
        return commanderId;
    }

    public Person getCommander(Campaign campaign) {
        return campaign.getPerson(commanderId);
    }

    public void setCommander(UUID id) {
        commanderId = id;
    }

    public void setCommander(Person p) {
        commanderId = p.getId();
    }

    public void refreshCommander(Campaign campaign) {
        commanderId = findCommander(forceId, campaign);
    }

    public int getSize(Campaign campaign) {
        if (campaign.getFaction().isClan()) {
            return (int) Math.ceil(getEffectivePoints(campaign));
        }
        if (campaign.getForce(forceId) != null) {
            return campaign.getForce(forceId).getUnits().size();
        } else {
            return 0;
        }
    }

    public double getEffectivePoints(Campaign campaign) {
        /*
         * Used to check against force size limits; for this purpose we
         * consider a 'Mek and a Point of BA to be a single Point so that
         * a Nova that has 10 actual Points is calculated as 5 effective
         * Points. We also count Points of vehicles with 'Meks and
         * conventional infantry with BA to account for CHH vehicle Novas.
         */
        double armor = 0.0;
        double infantry = 0.0;
        double other = 0.0;
        for (UUID id : campaign.getForce(forceId).getUnits()) {
            Unit unit = campaign.getUnit(id);
            if (null != unit) {
                Entity entity = unit.getEntity();
                if (null != entity) {
                    if ((entity.getEntityType() & ETYPE_MEK) != 0) {
                        armor += 1;
                    } else if ((entity.getEntityType() & ETYPE_AEROSPACEFIGHTER) != 0) {
                        other += 0.5;
                    } else if ((entity.getEntityType() & ETYPE_TANK) != 0) {
                        armor += 0.5;
                    } else if ((entity.getEntityType() & ETYPE_PROTOMEK) != 0) {
                        other += 0.2;
                    } else if ((entity.getEntityType() & Entity.ETYPE_INFANTRY) != 0) {
                        infantry += ((Infantry) entity).isSquad() ? 0.2 : 1;
                    }
                }
            }
        }
        return Math.max(armor, infantry) + other;
    }

    public int getWeightClass(Campaign campaign) {
        /*
         * Clan units only count half the weight of ASF and vehicles
         * (2/Point). IS units only count half the weight of vehicles
         * if the option is enabled, possibly dropping the lance to a lower
         * weight class and decreasing the enemy force against vehicle/combined
         * lances.
         */
        double weight = calculateTotalWeight(campaign, forceId);

        Force originForce = campaign.getForce(forceId);

        if (originForce == null) {
            return WEIGHT_ULTRA_LIGHT;
        }

        List<Force> subForces = originForce.getSubForces();
        int subForcesCount = subForces.size();

        for (Force childForce : subForces) {
            double childForceWeight = calculateTotalWeight(campaign, childForce.getId());

            if (childForceWeight > 0) {
                weight += childForceWeight;
            } else {
                subForcesCount--;
            }
        }

        if (subForcesCount > 0) {
            weight = weight / subForcesCount;
        }

        int standardForceSize = getStandardForceSize(campaign.getFaction());

        weight = weight / standardForceSize;

        final int CATEGORY_ULTRA_LIGHT = 20;
        final int CATEGORY_LIGHT = 35;
        final int CATEGORY_MEDIUM = 55;
        final int CATEGORY_HEAVY = 75;
        final int CATEGORY_ASSAULT = 100;

        if (weight < CATEGORY_ULTRA_LIGHT) {
            return WEIGHT_ULTRA_LIGHT;
        }
        if (weight <= CATEGORY_LIGHT) {
            return EntityWeightClass.WEIGHT_LIGHT;
        }
        if (weight <= CATEGORY_MEDIUM) {
            return EntityWeightClass.WEIGHT_MEDIUM;
        }
        if (weight <= CATEGORY_HEAVY) {
            return EntityWeightClass.WEIGHT_HEAVY;
        }
        if (weight <= CATEGORY_ASSAULT) {
            return EntityWeightClass.WEIGHT_ASSAULT;
        }
        return EntityWeightClass.WEIGHT_SUPER_HEAVY;
    }

    public boolean isEligible(Campaign campaign) {
        // ensure the lance is marked as a combat force
        final Force force = campaign.getForce(forceId);

        if (force == null) {
            return false;
        }

        if (!force.isCombatForce()) {
            force.setCombatTeamStatus(false);
            return false;
        }

        /*
         * Check that the number of units and weight are within the limits.
         */
        if (campaign.getCampaignOptions().isLimitLanceNumUnits()) {
            int size = getSize(campaign);
            if (size < getStandardForceSize(campaign.getFaction()) - 1 ||
                    size > getStandardForceSize(campaign.getFaction()) + 2) {
                force.setCombatTeamStatus(false);
                return false;
            }
        }

        if (campaign.getCampaignOptions().isLimitLanceWeight() &&
                getWeightClass(campaign) > EntityWeightClass.WEIGHT_ASSAULT) {
            force.setCombatTeamStatus(false);
            return false;
        }

        int isOverridden = force.getOverrideCombatTeam();
        if (isOverridden != COMBAT_TEAM_OVERRIDE_NONE) {
            boolean overrideState = isOverridden == COMBAT_TEAM_OVERRIDE_TRUE;
            force.setCombatTeamStatus(overrideState);

            List<Force> associatedForces = force.getAllParents();
            associatedForces.addAll(force.getAllSubForces());

            for (Force associatedForce : associatedForces) {
                associatedForce.setCombatTeamStatus(false);
            }

            return overrideState;
        }

        // This should never be getAllUnits() as otherwise parent nodes will be assessed as being
        // automatically eligible to be Combat Teams preventing child nodes from being Combat Teams
        if (force.getUnits().isEmpty()) {
            force.setCombatTeamStatus(false);
            return false;
        }

        List<Force> childForces = force.getAllSubForces();

        for (Force childForce : childForces) {
            if (childForce.isCombatTeam()) {
                force.setCombatTeamStatus(false);
                return false;
            }
        }

        List<Force> parentForces = force.getAllParents();

        for (Force parentForce : parentForces) {
            if (parentForce.isCombatTeam()) {
                force.setCombatTeamStatus(false);
                return false;
            }
        }

        force.setCombatTeamStatus(true);
        return true;
    }

    /* Code to find unit commander from ForceViewPanel */
    public static UUID findCommander(int forceId, Campaign campaign) {
        return campaign.getForce(forceId).getForceCommanderID();
    }

    public static LocalDate getBattleDate(LocalDate today) {
        return today.plusDays(Compute.randomInt(7));
    }

    public AtBScenario checkForBattle(Campaign campaign) {
        // Make sure there is a battle first
        if ((campaign.getCampaignOptions().getAtBBattleChance(role) == 0)
                || (Compute.randomInt(100) > campaign.getCampaignOptions().getAtBBattleChance(role))) {
            // No battle
            return null;
        }

        // if we are using StratCon, don't *also* generate legacy scenarios
        if (campaign.getCampaignOptions().isUseStratCon() &&
                (getContract(campaign).getStratconCampaignState() != null)) {
            return null;
        }

        int roll;
        // thresholds are coded from charts with 1-100 range, so we add 1 to mod to
        // adjust 0-based random int
        int battleTypeMod = 1 + (AtBMoraleLevel.STALEMATE.ordinal() - getContract(campaign).getMoraleLevel().ordinal()) * 5;
        battleTypeMod += getContract(campaign).getBattleTypeMod();

        // debugging code that will allow you to force the generation of a particular
        // scenario.
        // when generating a lance-based scenario (Standup, Probe, etc), the second
        // parameter in
        // createScenario is "this" (the lance). Otherwise, it should be null.

        /*
         * if (true) {
         * AtBScenario scenario = AtBScenarioFactory.createScenario(campaign, this,
         * AtBScenario.BASEATTACK, true, getBattleDate(campaign.getLocalDate()));
         * scenario.setMissionId(this.getMissionId());
         * return scenario;
         * }
         */

        switch (role) {
            case MANEUVER: {
                roll = Compute.randomInt(40) + battleTypeMod;
                if (roll < 1) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.BASEATTACK, false,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 9) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.BREAKTHROUGH, true,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 17) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.STANDUP, true,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 25) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.STANDUP, false,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 33) {
                    if (campaign.getCampaignOptions().isGenerateChases()) {
                        return AtBScenarioFactory.createScenario(campaign, this,
                                AtBScenario.CHASE, false,
                                getBattleDate(campaign.getLocalDate()));
                    } else {
                        return AtBScenarioFactory.createScenario(campaign, this,
                                AtBScenario.HOLDTHELINE, false,
                                getBattleDate(campaign.getLocalDate()));
                    }
                } else if (roll < 41) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.HOLDTHELINE, true,
                            getBattleDate(campaign.getLocalDate()));
                } else {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.BASEATTACK, true,
                            getBattleDate(campaign.getLocalDate()));
                }
            }
            case PATROL: {
                roll = Compute.randomInt(60) + battleTypeMod;
                if (roll < 1) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.BASEATTACK, false,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 11) {
                    if (campaign.getCampaignOptions().isGenerateChases()) {
                        return AtBScenarioFactory.createScenario(campaign, this,
                                AtBScenario.CHASE, true,
                                getBattleDate(campaign.getLocalDate()));
                    } else {
                        return AtBScenarioFactory.createScenario(campaign, this,
                                AtBScenario.HIDEANDSEEK, false,
                                getBattleDate(campaign.getLocalDate()));
                    }
                } else if (roll < 21) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.HIDEANDSEEK, true,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 31) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.PROBE, true,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 41) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.PROBE, false,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 51) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.EXTRACTION, true,
                            getBattleDate(campaign.getLocalDate()));
                } else {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.RECONRAID, true,
                            getBattleDate(campaign.getLocalDate()));
                }
            }
            case FRONTLINE: {
                roll = Compute.randomInt(20) + battleTypeMod;
                if (roll < 1) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.BASEATTACK, false,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 5) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.HOLDTHELINE, false,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 9) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.RECONRAID, false,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 13) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.EXTRACTION, false,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 17) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.HIDEANDSEEK, true,
                            getBattleDate(campaign.getLocalDate()));
                } else {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.BREAKTHROUGH, false,
                            getBattleDate(campaign.getLocalDate()));
                }
            }
            case TRAINING: {
                roll = Compute.randomInt(10) + battleTypeMod;
                if (roll < 1) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.BASEATTACK, false,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 3) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.HOLDTHELINE, false,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 5) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.BREAKTHROUGH, true,
                            getBattleDate(campaign.getLocalDate()));
                } else if (roll < 7) {
                    if (campaign.getCampaignOptions().isGenerateChases()) {
                        return AtBScenarioFactory.createScenario(campaign, this,
                                AtBScenario.CHASE, true,
                                getBattleDate(campaign.getLocalDate()));
                    } else {
                        return AtBScenarioFactory.createScenario(campaign, this,
                                AtBScenario.BREAKTHROUGH, false,
                                getBattleDate(campaign.getLocalDate()));
                    }
                } else if (roll < 9) {
                    return AtBScenarioFactory.createScenario(campaign, this,
                            AtBScenario.HIDEANDSEEK, false,
                            getBattleDate(campaign.getLocalDate()));
                } else {
                    if (campaign.getCampaignOptions().isGenerateChases()) {
                        return AtBScenarioFactory.createScenario(campaign, this,
                                AtBScenario.CHASE, false,
                                getBattleDate(campaign.getLocalDate()));
                    } else {
                        return AtBScenarioFactory.createScenario(campaign, this,
                                AtBScenario.HOLDTHELINE, false,
                                getBattleDate(campaign.getLocalDate()));
                    }
                }
            }
            default: {
                return null;
            }
        }
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "lance", "type", getClass());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forceId", forceId);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "missionId", missionId);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "role", role.name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "commanderId", commanderId);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "lance");
    }

    public static CombatTeam generateInstanceFromXML(Node wn) {
        CombatTeam retVal = null;
        NamedNodeMap attrs = wn.getAttributes();
        Node classNameNode = attrs.getNamedItem("type");
        String className = classNameNode.getTextContent();
        try {
            retVal = (CombatTeam) Class.forName(className).newInstance();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("forceId")) {
                    retVal.forceId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("missionId")) {
                    retVal.missionId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("role")) {
                    retVal.setRole(CombatRole.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("commanderId")) {
                    retVal.commanderId = UUID.fromString(wn2.getTextContent());
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }
        return retVal;
    }

    /**
     * Worker function that calculates the total weight of a force with the given ID
     *
     * @param campaign       Campaign in which the force resides
     * @param forceId        Force for which to calculate weight
     * @return Total force weight
     */
    public static double calculateTotalWeight(Campaign campaign, int forceId) {
        double weight = 0.0;

        for (UUID id : campaign.getForce(forceId).getUnits()) {
            try {
                Unit unit = campaign.getUnit(id);
                Entity entity = unit.getEntity();

                boolean isClan = campaign.getFaction().isClan();
                long entityType = entity.getEntityType();

                if (entityType == ETYPE_TANK) {
                    if (isClan || campaign.getCampaignOptions().isAdjustPlayerVehicles()) {
                        weight += entity.getWeight() * 0.5;
                    } else {
                        weight += entity.getWeight();
                    }
                } else if (entityType == ETYPE_AEROSPACEFIGHTER) {
                    if (isClan) {
                        weight += entity.getWeight() * 0.5;
                    } else {
                        weight += entity.getWeight();
                    }
                } else {
                    weight += entity.getWeight();
                }
            } catch (Exception exception) {
                logger.error(String.format("Failed to parse unit ID %s: %s", forceId, exception));
            }
        }

        return weight;
    }

    /**
     * This static method updates the combat teams across the campaign.
     * It starts at the top level force, and calculates the combat teams for each sub-force.
     * It keeps only the eligible combat teams and imports them into the campaign.
     * After every formation is processed, an 'OrganizationChangedEvent' is triggered by that force.
     *
     * @param campaign the current campaign.
     */
    public static void recalculateCombatTeams(Campaign campaign) {
        Hashtable<Integer, CombatTeam> combatTeamsTable = campaign.getCombatTeamsTable();
        CombatTeam combatTeam = combatTeamsTable.get(0); // This is the origin node
        Force force = campaign.getForce(0);

        // Does the force already exist in our hashtable? If so, update it accordingly
        if (combatTeam != null) {
            boolean isEligible = combatTeam.isEligible(campaign);

            if (!isEligible) {
                campaign.removeCombatTeam(0);
            }

            force.setCombatTeamStatus(isEligible);
        // Otherwise, create a new formation and then add it to the table, if appropriate
        } else {
            combatTeam = new CombatTeam(0, campaign);
            boolean isEligible = combatTeam.isEligible(campaign);

            if (isEligible) {
                campaign.addCombatTeam(combatTeam);
            }

            force.setCombatTeamStatus(isEligible);
        }

        // Update the TO&E and then begin recursively walking it
        MekHQ.triggerEvent(new OrganizationChangedEvent(force));
        recalculateSubForceStrategicStatus(campaign, campaign.getCombatTeamsTable(), force);
    }

    /**
     * This method is used to update the combat teams for the campaign working downwards
     * from a specified node, through all of its sub-forces.
     * It creates a new {@link CombatTeam} for each sub-force and checks its eligibility.
     * Eligible formations are imported into the campaign, and the combat team status of
     * the respective force is set to {@code true}.
     * After every force is processed, an 'OrganizationChangedEvent' is triggered.
     * This function runs recursively on each sub-force, effectively traversing the complete TO&E.
     *
     * @param campaign the current {@link Campaign}.
     * @param workingNode the {@link Force} node from which the method starts working down through
     *                   all its sub-forces.
     */
    private static void recalculateSubForceStrategicStatus(Campaign campaign,
                                                           Hashtable<Integer, CombatTeam> combatTeamsTable,
                                                           Force workingNode) {

        for (Force force : workingNode.getSubForces()) {
            int forceId = force.getId();
            CombatTeam combatTeam = combatTeamsTable.get(forceId);

            // Does the force already exist in our hashtable? If so, update it accordingly
            if (combatTeam != null) {
                boolean isEligible = combatTeam.isEligible(campaign);

                if (!isEligible) {
                    campaign.removeCombatTeam(forceId);
                }

                force.setCombatTeamStatus(isEligible);
            // Otherwise, create a new formation and then add it to the table, if appropriate
            } else {
                combatTeam = new CombatTeam(forceId, campaign);
                boolean isEligible = combatTeam.isEligible(campaign);

                if (isEligible) {
                    campaign.addCombatTeam(combatTeam);
                }

                force.setCombatTeamStatus(isEligible);
            }

            // Update the TO&E and then continue recursively walking it
            MekHQ.triggerEvent(new OrganizationChangedEvent(force));
            recalculateSubForceStrategicStatus(campaign, campaign.getCombatTeamsTable(), force);
        }
    }
}
