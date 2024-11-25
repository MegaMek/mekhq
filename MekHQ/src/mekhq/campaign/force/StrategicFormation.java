/*
 * Lance.java
 *
 * Copyright (c) 2011 - Carl Spain. All rights reserved.
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.*;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.AtBLanceRole;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Used by Against the Bot & StratCon to track additional information about each force
 * on the TO&amp;E that has at least one unit assigned. Extra info includes whether
 * the force counts as a Strategic Formation eligible for assignment to a scenario role
 * and what the assignment is on which contract.
 *
 * @author Neoancient
 */
public class StrategicFormation {
    private static final MMLogger logger = MMLogger.create(StrategicFormation.class);

    public static final int STR_IS = 4;
    public static final int STR_CLAN = 5;
    public static final int STR_CS = 6;

    public static final long ETYPE_GROUND = Entity.ETYPE_MEK |
            Entity.ETYPE_TANK | Entity.ETYPE_INFANTRY | Entity.ETYPE_PROTOMEK;

    /** Indicates a lance has no assigned mission */
    public static final int NO_MISSION = -1;

    private int forceId;
    private int missionId;
    private AtBLanceRole role;
    private UUID commanderId;

    /**
     * Determines the standard size for a given faction. The size varies depending on whether the
     * faction is a Clan, ComStar/WoB, or others (Inner Sphere).
     *
     * @param faction The {@link Faction} object for which the standard force size is to be calculated.
     * @return The standard force size for the given faction. It returns {@code STR_CLAN} if the
     * faction is a Clan, {@code STR_CS} if the faction is ComStar or WoB, and {@code STR_IS} otherwise.
     */
    public static int getStdLanceSize(Faction faction) {
        if (faction.isClan()) {
            return STR_CLAN;
        } else if (faction.isComStarOrWoB()) {
            return STR_CS;
        } else {
            return STR_IS;
        }
    }

    /**
     * Default constructor
     */
    public StrategicFormation() {}

    public StrategicFormation(int forceId, Campaign campaign) {
        this.forceId = forceId;
        role = AtBLanceRole.UNASSIGNED;
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

    public AtBLanceRole getRole() {
        return role;
    }

    public void setRole(AtBLanceRole role) {
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
                    if ((entity.getEntityType() & Entity.ETYPE_MEK) != 0) {
                        armor += 1;
                    } else if ((entity.getEntityType() & Entity.ETYPE_AEROSPACEFIGHTER) != 0) {
                        other += 0.5;
                    } else if ((entity.getEntityType() & Entity.ETYPE_TANK) != 0) {
                        armor += 0.5;
                    } else if ((entity.getEntityType() & Entity.ETYPE_PROTOMEK) != 0) {
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

        weight = weight * 4.0 / getStdLanceSize(campaign.getFaction());
        if (weight < 40) {
            return EntityWeightClass.WEIGHT_ULTRA_LIGHT;
        }
        if (weight <= 130) {
            return EntityWeightClass.WEIGHT_LIGHT;
        }
        if (weight <= 200) {
            return EntityWeightClass.WEIGHT_MEDIUM;
        }
        if (weight <= 280) {
            return EntityWeightClass.WEIGHT_HEAVY;
        }
        if (weight <= 390) {
            return EntityWeightClass.WEIGHT_ASSAULT;
        }
        return EntityWeightClass.WEIGHT_SUPER_HEAVY;
    }

    public boolean isEligible(Campaign campaign) {
        // ensure the lance is marked as a combat force
        final Force force = campaign.getForce(forceId);

        if ((force == null) || !force.isCombatForce()) {
            return false;
        }

        /*
         * Check that the number of units and weight are within the limits
         * and that the force contains at least one ground unit.
         */
        if (campaign.getCampaignOptions().isLimitLanceNumUnits()) {
            int size = getSize(campaign);
            if (size < getStdLanceSize(campaign.getFaction()) - 1 ||
                    size > getStdLanceSize(campaign.getFaction()) + 2) {
                return false;
            }
        }

        if (campaign.getCampaignOptions().isLimitLanceWeight() &&
                getWeightClass(campaign) > EntityWeightClass.WEIGHT_ASSAULT) {
            return false;
        }

        boolean hasGround = false;
        for (UUID id : force.getUnits()) {
            Unit unit = campaign.getUnit(id);
            if (unit != null) {
                Entity entity = unit.getEntity();

                if (entity != null) {
                    if (entity.getUnitType() >= UnitType.JUMPSHIP) {
                        return false;
                    }
                    if ((entity.getEntityType() & ETYPE_GROUND) != 0) {
                        hasGround = true;
                    }
                }
            }
        }

        if (hasGround) {
            ArrayList<StrategicFormation> strategicFormations = campaign.getStrategicFormationList();
            List<Integer> allLanceForceIds = new ArrayList<>();

            for (StrategicFormation strategicFormation : strategicFormations) {
                allLanceForceIds.add(strategicFormation.getForceId());
            }

            // Parent Forces
            List<Force> parentForces = force.getAllParents();

            for (Force parentForce : parentForces) {
                if (allLanceForceIds.contains(parentForce.getId())) {
                    return false;
                }
            }

            // Child Forces
            List<Force> childForces = force.getAllSubForces();

            for (Force childForce : childForces) {
                if (allLanceForceIds.contains(childForce.getId())) {
                    return false;
                }
            }
        }

        return hasGround;
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
            case FIGHTING: {
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
            case SCOUTING: {
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
            case DEFENCE: {
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

    public static StrategicFormation generateInstanceFromXML(Node wn) {
        StrategicFormation retVal = null;
        NamedNodeMap attrs = wn.getAttributes();
        Node classNameNode = attrs.getNamedItem("type");
        String className = classNameNode.getTextContent();
        try {
            retVal = (StrategicFormation) Class.forName(className).newInstance();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("forceId")) {
                    retVal.forceId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("missionId")) {
                    retVal.missionId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("role")) {
                    retVal.setRole(AtBLanceRole.parseFromString(wn2.getTextContent().trim()));
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
     * @param c       Campaign in which the force resides
     * @param forceId Force for which to calculate weight
     * @return Total force weight
     */
    public static double calculateTotalWeight(Campaign campaign, int forceId) {
        double weight = 0.0;

        for (UUID id : campaign.getForce(forceId).getUnits()) {
            Unit unit = campaign.getUnit(id);
            if (null != unit) {
                Entity entity = unit.getEntity();
                if (null != entity) {
                    if ((entity.getEntityType() & Entity.ETYPE_MEK) != 0 ||
                            (entity.getEntityType() & Entity.ETYPE_PROTOMEK) != 0 ||
                            (entity.getEntityType() & Entity.ETYPE_INFANTRY) != 0) {
                        weight += entity.getWeight();
                    } else if ((entity.getEntityType() & Entity.ETYPE_TANK) != 0) {
                        if (campaign.getFaction().isClan() || campaign.getCampaignOptions().isAdjustPlayerVehicles()) {
                            weight += entity.getWeight() * 0.5;
                        } else {
                            weight += entity.getWeight();
                        }
                    } else if ((entity.getEntityType() & Entity.ETYPE_AEROSPACEFIGHTER) != 0) {
                        if (campaign.getFaction().isClan()) {
                            weight += entity.getWeight() * 0.5;
                        } else {
                            weight += entity.getWeight();
                        }
                    }
                }
            }
        }

        return weight;
    }
}
