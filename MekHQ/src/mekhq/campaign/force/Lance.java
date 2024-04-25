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
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.AtBLanceRole;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Used by Against the Bot to track additional information about each force
 * on the TO&amp;E that has at least one unit assigned. Extra info includes whether
 * the force counts as a lance (or star or level II) eligible for assignment
 * to a mission role and what the assignment is on which contract.
 *
 * @author Neoancient
 */
public class Lance {
    public static final int STR_IS = 4;
    public static final int STR_CLAN = 5;
    public static final int STR_CS = 6;

    public static final long ETYPE_GROUND = Entity.ETYPE_MECH |
            Entity.ETYPE_TANK | Entity.ETYPE_INFANTRY | Entity.ETYPE_PROTOMECH;

    /** Indicates a lance has no assigned mission */
    public static final int NO_MISSION = -1;

    private int forceId;
    private int missionId;
    private AtBLanceRole role;
    private UUID commanderId;

    public static int getStdLanceSize(Faction f) {
        if (f.isClan()) {
            return STR_CLAN;
        } else if (f.getShortName().equals("CS") || f.getShortName().equals("WOB")) {
            return STR_CS;
        } else {
            return STR_IS;
        }
    }

    public Lance() {}

    public Lance(int fid, Campaign c) {
        forceId = fid;
        role = AtBLanceRole.UNASSIGNED;
        missionId = -1;
        for (AtBContract contract : c.getActiveAtBContracts()) {
            missionId = ((contract.getParentContract() == null)
                    ? contract : contract.getParentContract()).getId();
        }
        commanderId = findCommander(forceId, c);
    }

    public int getForceId() {
        return forceId;
    }

    public int getMissionId() {
        return missionId;
    }

    public AtBContract getContract(Campaign c) {
        return (AtBContract) c.getMission(missionId);
    }

    public void setContract(AtBContract c) {
        if (null == c) {
            missionId = NO_MISSION;
        } else {
            missionId = c.getId();
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

    public Person getCommander(Campaign c) {
        return c.getPerson(commanderId);
    }

    public void setCommander(UUID id) {
        commanderId = id;
    }

    public void setCommander(Person p) {
        commanderId = p.getId();
    }

    public void refreshCommander(Campaign c) {
        commanderId = findCommander(forceId, c);
    }

    public int getSize(Campaign c) {
        if (c.getFaction().isClan()) {
            return (int) Math.ceil(getEffectivePoints(c));
        }
        if (c.getForce(forceId) != null) {
            return c.getForce(forceId).getUnits().size();
        } else {
            return 0;
        }
    }

    public double getEffectivePoints(Campaign c) {
        /* Used to check against force size limits; for this purpose we
         * consider a 'Mech and a Point of BA to be a single Point so that
         * a Nova that has 10 actual Points is calculated as 5 effective
         * Points. We also count Points of vehicles with 'Mechs and
         * conventional infantry with BA to account for CHH vehicle Novas.
         */
        double armor = 0.0;
        double infantry = 0.0;
        double other = 0.0;
        for (UUID id : c.getForce(forceId).getUnits()) {
            Unit unit = c.getUnit(id);
            if (null != unit) {
                Entity entity = unit.getEntity();
                if (null != entity) {
                    if ((entity.getEntityType() & Entity.ETYPE_MECH) != 0) {
                        armor += 1;
                    } else if ((entity.getEntityType() & Entity.ETYPE_AEROSPACEFIGHTER) != 0) {
                        other += 0.5;
                    } else if ((entity.getEntityType() & Entity.ETYPE_TANK) != 0) {
                        armor += 0.5;
                    } else if ((entity.getEntityType() & Entity.ETYPE_PROTOMECH) != 0) {
                        other += 0.2;
                    } else if ((entity.getEntityType() & Entity.ETYPE_INFANTRY) != 0) {
                        infantry += ((Infantry) entity).isSquad()?0.2:1;
                    }
                }
            }
        }
        return Math.max(armor, infantry) + other;
    }

    public int getWeightClass(Campaign c) {
        /* Clan units only count half the weight of ASF and vehicles
         * (2/Point). IS units only count half the weight of vehicles
         * if the option is enabled, possibly dropping the lance to a lower
         * weight class and decreasing the enemy force against vehicle/combined
         * lances.
         */
        double weight = calculateTotalWeight(c, forceId);

        weight = weight * 4.0 / getStdLanceSize(c.getFaction());
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

    public boolean isEligible(Campaign c) {
        // ensure the lance is marked as a combat force
        final Force force = c.getForce(forceId);
        if ((force == null) || !force.isCombatForce()) {
            return false;
        }

        /* Check that the number of units and weight are within the limits
         * and that the force contains at least one ground unit. */
        if (c.getCampaignOptions().isLimitLanceNumUnits()) {
            int size = getSize(c);
            if (size < getStdLanceSize(c.getFaction()) - 1 ||
                    size > getStdLanceSize(c.getFaction()) + 2) {
                return false;
            }
        }

        if (c.getCampaignOptions().isLimitLanceWeight() &&
                getWeightClass(c) > EntityWeightClass.WEIGHT_ASSAULT) {
            return false;
        }

        boolean hasGround = false;
        for (UUID id : force.getUnits()) {
            Unit unit = c.getUnit(id);
            if (null != unit) {
                Entity entity = unit.getEntity();
                if (null != entity) {
                    if (entity.getUnitType() >= UnitType.JUMPSHIP) {
                        return false;
                    }
                    if ((entity.getEntityType() & ETYPE_GROUND) != 0) {
                        hasGround = true;
                    }
                }
            }
        }

        return hasGround;
    }

    /* Code to find unit commander from ForceViewPanel */
    public static UUID findCommander(int forceId, Campaign c) {
        return c.getForce(forceId).getForceCommanderID();
    }

    public static LocalDate getBattleDate(LocalDate today) {
        return today.plusDays(Compute.randomInt(7));
    }

    public AtBScenario checkForBattle(Campaign c) {
        // Make sure there is a battle first
        if ((c.getCampaignOptions().getAtBBattleChance(role) == 0)
                || (Compute.randomInt(100) > c.getCampaignOptions().getAtBBattleChance(role))) {
            // No battle
            return null;
        }

        // if we are using StratCon, don't *also* generate legacy scenarios
        if (c.getCampaignOptions().isUseStratCon() &&
                (getContract(c).getStratconCampaignState() != null)) {
            return null;
        }

        int roll;
        //thresholds are coded from charts with 1-100 range, so we add 1 to mod to adjust 0-based random int
        int battleTypeMod = 1 + (AtBMoraleLevel.NORMAL.ordinal() - getContract(c).getMoraleLevel().ordinal()) * 5;
        battleTypeMod += getContract(c).getBattleTypeMod();

        // debugging code that will allow you to force the generation of a particular scenario.
        // when generating a lance-based scenario (Standup, Probe, etc), the second parameter in
        // createScenario is "this" (the lance). Otherwise, it should be null.

        /*if (true) {
            AtBScenario scenario = AtBScenarioFactory.createScenario(c, this, AtBScenario.BASEATTACK, true, getBattleDate(c.getLocalDate()));
            scenario.setMissionId(this.getMissionId());
            return scenario;
        }*/

        switch (role) {
            case FIGHTING: {
                roll = Compute.randomInt(40) + battleTypeMod;
                if (roll < 1) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.BASEATTACK, false,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 9) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.BREAKTHROUGH, true,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 17) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.STANDUP, true,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 25) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.STANDUP, false,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 33) {
                    if (c.getCampaignOptions().isGenerateChases()) {
                        return AtBScenarioFactory.createScenario(c, this,
                                AtBScenario.CHASE, false,
                                getBattleDate(c.getLocalDate()));
                    } else {
                        return AtBScenarioFactory.createScenario(c, this,
                                AtBScenario.HOLDTHELINE, false,
                                getBattleDate(c.getLocalDate()));
                    }
                } else if (roll < 41) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.HOLDTHELINE, true,
                            getBattleDate(c.getLocalDate()));
                } else {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.BASEATTACK, true,
                            getBattleDate(c.getLocalDate()));
                }
            }
            case SCOUTING: {
                roll = Compute.randomInt(60) + battleTypeMod;
                if (roll < 1) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.BASEATTACK, false,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 11) {
                    if (c.getCampaignOptions().isGenerateChases()) {
                        return AtBScenarioFactory.createScenario(c, this,
                                AtBScenario.CHASE, true,
                                getBattleDate(c.getLocalDate()));
                    } else {
                        return AtBScenarioFactory.createScenario(c, this,
                                AtBScenario.HIDEANDSEEK, false,
                                getBattleDate(c.getLocalDate()));
                    }
                } else if (roll < 21) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.HIDEANDSEEK, true,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 31) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.PROBE, true,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 41) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.PROBE, false,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 51) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.EXTRACTION, true,
                            getBattleDate(c.getLocalDate()));
                } else {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.RECONRAID, true,
                            getBattleDate(c.getLocalDate()));
                }
            }
            case DEFENCE: {
                roll = Compute.randomInt(20) + battleTypeMod;
                if (roll < 1) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.BASEATTACK, false,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 5) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.HOLDTHELINE, false,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 9) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.RECONRAID, false,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 13) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.EXTRACTION, false,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 17) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.HIDEANDSEEK, true,
                            getBattleDate(c.getLocalDate()));
                } else {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.BREAKTHROUGH, false,
                            getBattleDate(c.getLocalDate()));
                }
            }
            case TRAINING: {
                roll = Compute.randomInt(10) + battleTypeMod;
                if (roll < 1) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.BASEATTACK, false,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 3) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.HOLDTHELINE, false,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 5) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.BREAKTHROUGH, true,
                            getBattleDate(c.getLocalDate()));
                } else if (roll < 7) {
                    if (c.getCampaignOptions().isGenerateChases()) {
                        return AtBScenarioFactory.createScenario(c, this,
                                AtBScenario.CHASE, true,
                                getBattleDate(c.getLocalDate()));
                    } else {
                        return AtBScenarioFactory.createScenario(c, this,
                                AtBScenario.BREAKTHROUGH, false,
                                getBattleDate(c.getLocalDate()));
                    }
                } else if (roll < 9) {
                    return AtBScenarioFactory.createScenario(c, this,
                            AtBScenario.HIDEANDSEEK, false,
                            getBattleDate(c.getLocalDate()));
                } else {
                    if (c.getCampaignOptions().isGenerateChases()) {
                        return AtBScenarioFactory.createScenario(c, this,
                                AtBScenario.CHASE, false,
                                getBattleDate(c.getLocalDate()));
                    } else {
                        return AtBScenarioFactory.createScenario(c, this,
                                AtBScenario.HOLDTHELINE, false,
                                getBattleDate(c.getLocalDate()));
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

    public static Lance generateInstanceFromXML(Node wn) {
        Lance retVal = null;
        NamedNodeMap attrs = wn.getAttributes();
        Node classNameNode = attrs.getNamedItem("type");
        String className = classNameNode.getTextContent();
        try {
            retVal = (Lance) Class.forName(className).newInstance();
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
            LogManager.getLogger().error("", ex);
        }
        return retVal;
    }

    /**
     * Worker function that calculates the total weight of a force with the given ID
     * @param c Campaign in which the force resides
     * @param forceId Force for which to calculate weight
     * @return Total force weight
     */
    public static double calculateTotalWeight(Campaign c, int forceId) {
        double weight = 0.0;

        for (UUID id : c.getForce(forceId).getUnits()) {
            Unit unit = c.getUnit(id);
            if (null != unit) {
                Entity entity = unit.getEntity();
                if (null != entity) {
                    if ((entity.getEntityType() & Entity.ETYPE_MECH) != 0 ||
                            (entity.getEntityType() & Entity.ETYPE_PROTOMECH) != 0 ||
                            (entity.getEntityType() & Entity.ETYPE_INFANTRY) != 0) {
                        weight += entity.getWeight();
                    } else if ((entity.getEntityType() & Entity.ETYPE_TANK) != 0) {
                        if (c.getFaction().isClan() || c.getCampaignOptions().isAdjustPlayerVehicles()) {
                            weight += entity.getWeight() * 0.5;
                        } else {
                            weight += entity.getWeight();
                        }
                    } else if ((entity.getEntityType() & Entity.ETYPE_AEROSPACEFIGHTER) != 0) {
                        if (c.getFaction().isClan()) {
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
