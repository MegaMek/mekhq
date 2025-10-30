/*
 * Copyright (c) 2011 - Carl Spain. All rights reserved.
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.force;

import static megamek.common.units.Entity.ETYPE_AEROSPACE_FIGHTER;
import static megamek.common.units.Entity.ETYPE_MEK;
import static megamek.common.units.Entity.ETYPE_PROTOMEK;
import static megamek.common.units.Entity.ETYPE_TANK;
import static megamek.common.units.EntityWeightClass.WEIGHT_ULTRA_LIGHT;
import static mekhq.campaign.force.Force.COMBAT_TEAM_OVERRIDE_NONE;
import static mekhq.campaign.force.Force.COMBAT_TEAM_OVERRIDE_TRUE;
import static mekhq.campaign.force.ForceType.STANDARD;
import static mekhq.campaign.force.FormationLevel.LANCE;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Infantry;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.OrganizationChangedEvent;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.utilities.EntityUtilities;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Used by Against the Bot &amp; StratCon to track additional information about each force on the TO&amp;E that has at
 * least one unit assigned. Extra info includes whether the force counts as a Combat Team eligible for assignment to a
 * scenario role and what the assignment is on which contract.
 *
 * @author Neoancient
 */
public class CombatTeam {
    private static final MMLogger LOGGER = MMLogger.create(CombatTeam.class);

    public static final int LANCE_SIZE = 4;
    public static final int STAR_SIZE = 5;
    public static final int LEVEL_II_SIZE = 6;

    /** Indicates a lance has no assigned mission */
    public static final int NO_MISSION = -1;

    private int forceId;
    private int missionId;
    private CombatRole role;
    private UUID commanderId;

    /**
     * Determines the standard size for a given faction. The size varies depending on whether the faction is a Clan,
     * ComStar/WoB, or others (Inner Sphere). This overloaded method defaults to Lance/Star/Level II
     *
     * @param faction The {@link Faction} object for which the standard force size is to be calculated.
     *
     * @return The standard force size, at the provided formation level, for the provided faction
     */
    public static int getStandardForceSize(Faction faction) {
        return getStandardForceSize(faction, LANCE.getDepth());
    }

    /**
     * Determines the standard size for a given faction. The size varies depending on whether the faction is
     * ComStar/WoB, or others (Inner Sphere, Clan, etc.).
     *
     * @param faction             The {@link Faction} object for which the standard force size is to be calculated.
     * @param formationLevelDepth The {@link FormationLevel} {@code Depth} from which the standard force size is to be
     *                            calculated.
     *
     * @return The standard force size, at the provided formation level, for the provided faction
     */
    public static int getStandardForceSize(Faction faction, int formationLevelDepth) {
        int multiplier = faction.isComStar() ? 6 : 3;
        int base = faction.getFormationBaseSize();

        return (int) (base * Math.pow(multiplier, formationLevelDepth - 1));
    }

    /**
     * Default constructor
     */
    public CombatTeam() {
    }

    public CombatTeam(int forceId, Campaign campaign) {
        this.forceId = forceId;

        Force force = campaign.getForce(forceId);
        role = force != null ? force.getCombatRoleInMemory() : CombatRole.FRONTLINE;

        missionId = -1;
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            missionId = ((contract.getParentContract() == null) ? contract : contract.getParentContract()).getId();
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

    public @Nullable UUID getCommanderId() {
        return commanderId;
    }

    public @Nullable Person getCommander(Campaign campaign) {
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

    /**
     * Effective size used when determining for many units this combat team is. Sometimes a unit may count as less than
     * a unit, like a vehicle point in a Clan star (two vehicles would return a size of 1).
     * <p>
     * This method iterates through all combat teams in the specified campaign, ignoring combat teams with the auxiliary
     * role. For each valid combat team, it retrieves the associated force and evaluates all units within that force.
     * The unit contribution to the total is determined based on its type: </p>
     * <ul>
     *     <li><b>TANK, VTOL, NAVAL, CONV_FIGHTER, AEROSPACE_FIGHTER:</b> Adds 1 for non-clan factions, and 0.5
     *     for clan factions.</li>
     *     <li><b>PROTOMEK:</b> Adds 0.2 to the total.</li>
     *     <li><b>BATTLE_ARMOR, INFANTRY:</b> Adds 0 (excluded from the total, unless no other units).</li>
     *     <li><b>Other types:</b> Adds 1 to the total.</li>
     * </ul>
     *
     * @return effective size of the combat team
     */
    public int getSize(Campaign campaign) {
        if (campaign.getFaction().isClan()) {
            return (int) Math.ceil(getEffectivePoints(campaign));
        }
        if (campaign.getForce(forceId) != null) {
            return (int) Math.ceil(getEffectiveLanceSize(campaign));
        } else {
            return 0;
        }
    }

    /**
     * Effective size used when determining for many combat elements this combat team is.
     * <p>
     * Retrieves the associated force and evaluates all units within that force. The unit contribution to the total is
     * determined based on its type: </p>
     * <ul>
     *     <li><b>TANK, VTOL, NAVAL, CONV_FIGHTER, AEROSPACE_FIGHTER:</b> Adds 1 for non-clan factions, and 0.5
     *     for clan factions.</li>
     *     <li><b>PROTOMEK:</b> Adds 0.2 to the total.</li>
     *     <li><b>BATTLE_ARMOR, INFANTRY:</b> Adds 1. Infantry squads add 1/3. (excluded from the total if count
     *     of infantry is less than the count of everything else)
     *     .</li>
     *     <li><b>Other types:</b> Adds 1 to the total.</li>
     *     </ul>
     *
     * @return effective size of the lance for calculating contract requirements
     */
    private double getEffectiveLanceSize(Campaign campaign) {
        double numUnits = 0;
        double numInfantry = 0;
        Force force = getForce(campaign);

        if (force == null) {
            return numUnits;
        }

        if (!force.isForceType(STANDARD)) {
            return numUnits;
        }

        for (UUID unitId : force.getAllUnits(true)) {
            Entity entity = EntityUtilities.getEntityFromUnitId(campaign.getHangar(), unitId);

            if (entity == null) {
                continue;
            }

            switch (entity.getUnitType()) {
                case UnitType.TANK,
                     UnitType.VTOL,
                     UnitType.NAVAL,
                     UnitType.CONV_FIGHTER,
                     UnitType.AEROSPACE_FIGHTER,
                     UnitType.MEK -> numUnits += 1;
                case UnitType.PROTOMEK -> numUnits += 0.2;
                case UnitType.BATTLE_ARMOR -> numInfantry += 1;
                case UnitType.INFANTRY ->
                      numInfantry += entity instanceof Infantry infantry && infantry.isSquad() ? 1.0 / 4 : 1;
                default -> numUnits += 0; // All other unit types
            }
        }

        if (numInfantry > numUnits) {
            return (int) Math.floor(numInfantry);
        }
        return (int) Math.floor(numUnits);
    }

    private double getEffectivePoints(Campaign campaign) {
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
        for (UUID id : campaign.getForce(forceId).getAllUnits(true)) {
            Unit unit = campaign.getUnit(id);
            if (null != unit) {
                Entity entity = unit.getEntity();
                if (null != entity) {
                    if ((entity.getEntityType() & ETYPE_MEK) != 0) {
                        armor += 1;
                    } else if ((entity.getEntityType() & ETYPE_AEROSPACE_FIGHTER) != 0) {
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

        if (!force.isForceType(STANDARD)) {
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

            if (!parentForce.isForceType(STANDARD)) {
                force.setCombatTeamStatus(false);
                return false;
            }
        }

        force.setCombatTeamStatus(true);
        return true;
    }

    /* Code to find unit commander from ForceViewPanel */
    public static @Nullable UUID findCommander(int forceId, Campaign campaign) {
        return campaign.getForce(forceId).getForceCommanderID();
    }

    public static LocalDate getBattleDate(LocalDate today) {
        return today.plusDays(Compute.randomInt(7));
    }

    public AtBScenario checkForBattle(Campaign campaign) {
        // Make sure there is a battle first
        if ((campaign.getCampaignOptions().getAtBBattleChance(role, true) == 0) ||
                  (Compute.randomInt(100) > campaign.getCampaignOptions().getAtBBattleChance(role, true))) {
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
        int battleTypeMod = 1 +
                                  (AtBMoraleLevel.STALEMATE.ordinal() -
                                         getContract(campaign).getMoraleLevel().ordinal()) * 5;
        battleTypeMod += getContract(campaign).getBattleTypeMod();

        // debugging code that will allow you to force the generation of a particular
        // scenario.
        // when generating a lance-based scenario (Standup, Probe, etc.), the second
        // parameter in
        // createScenario is "this" (the lance). Otherwise, it should be null.

        /*
         * if (true) {
         * AtBScenario scenario = AtBScenarioFactory.createScenario(campaign, this,
         * AtBScenario.BASE_ATTACK, true, getBattleDate(campaign.getLocalDate()));
         * scenario.setMissionId(this.getMissionId());
         * return scenario;
         * }
         */

        switch (role) {
            case MANEUVER: {
                roll = Compute.randomInt(40) + battleTypeMod;
                if (roll < 1) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.BASE_ATTACK,
                          false,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 9) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.BREAKTHROUGH,
                          true,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 17) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.STANDUP,
                          true,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 25) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.STANDUP,
                          false,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 33) {
                    if (campaign.getCampaignOptions().isGenerateChases()) {
                        return AtBScenarioFactory.createScenario(campaign,
                              this,
                              AtBScenario.CHASE,
                              false,
                              getBattleDate(campaign.getLocalDate()));
                    } else {
                        return AtBScenarioFactory.createScenario(campaign,
                              this,
                              AtBScenario.HOLD_THE_LINE,
                              false,
                              getBattleDate(campaign.getLocalDate()));
                    }
                } else if (roll < 41) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.HOLD_THE_LINE,
                          true,
                          getBattleDate(campaign.getLocalDate()));
                } else {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.BASE_ATTACK,
                          true,
                          getBattleDate(campaign.getLocalDate()));
                }
            }
            case PATROL: {
                roll = Compute.randomInt(60) + battleTypeMod;
                if (roll < 1) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.BASE_ATTACK,
                          false,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 11) {
                    if (campaign.getCampaignOptions().isGenerateChases()) {
                        return AtBScenarioFactory.createScenario(campaign,
                              this,
                              AtBScenario.CHASE,
                              true,
                              getBattleDate(campaign.getLocalDate()));
                    } else {
                        return AtBScenarioFactory.createScenario(campaign,
                              this,
                              AtBScenario.HIDE_AND_SEEK,
                              false,
                              getBattleDate(campaign.getLocalDate()));
                    }
                } else if (roll < 21) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.HIDE_AND_SEEK,
                          true,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 31) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.PROBE,
                          true,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 41) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.PROBE,
                          false,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 51) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.EXTRACTION,
                          true,
                          getBattleDate(campaign.getLocalDate()));
                } else {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.RECON_RAID,
                          true,
                          getBattleDate(campaign.getLocalDate()));
                }
            }
            case FRONTLINE: {
                roll = Compute.randomInt(20) + battleTypeMod;
                if (roll < 1) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.BASE_ATTACK,
                          false,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 5) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.HOLD_THE_LINE,
                          false,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 9) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.RECON_RAID,
                          false,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 13) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.EXTRACTION,
                          false,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 17) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.HIDE_AND_SEEK,
                          true,
                          getBattleDate(campaign.getLocalDate()));
                } else {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.BREAKTHROUGH,
                          false,
                          getBattleDate(campaign.getLocalDate()));
                }
            }
            case TRAINING: {
                roll = Compute.randomInt(10) + battleTypeMod;
                if (roll < 1) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.BASE_ATTACK,
                          false,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 3) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.HOLD_THE_LINE,
                          false,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 5) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.BREAKTHROUGH,
                          true,
                          getBattleDate(campaign.getLocalDate()));
                } else if (roll < 7) {
                    if (campaign.getCampaignOptions().isGenerateChases()) {
                        return AtBScenarioFactory.createScenario(campaign,
                              this,
                              AtBScenario.CHASE,
                              true,
                              getBattleDate(campaign.getLocalDate()));
                    } else {
                        return AtBScenarioFactory.createScenario(campaign,
                              this,
                              AtBScenario.BREAKTHROUGH,
                              false,
                              getBattleDate(campaign.getLocalDate()));
                    }
                } else if (roll < 9) {
                    return AtBScenarioFactory.createScenario(campaign,
                          this,
                          AtBScenario.HIDE_AND_SEEK,
                          false,
                          getBattleDate(campaign.getLocalDate()));
                } else {
                    if (campaign.getCampaignOptions().isGenerateChases()) {
                        return AtBScenarioFactory.createScenario(campaign,
                              this,
                              AtBScenario.CHASE,
                              false,
                              getBattleDate(campaign.getLocalDate()));
                    } else {
                        return AtBScenarioFactory.createScenario(campaign,
                              this,
                              AtBScenario.HOLD_THE_LINE,
                              false,
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
        try {
            retVal = new CombatTeam();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("forceId")) {
                    // We're not using MathUtility here because there is no good fallback value.
                    // If this breaks, we need it to break loudly so we immediately notice
                    retVal.forceId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("missionId")) {
                    retVal.missionId = MathUtility.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("role")) {
                    retVal.setRole(CombatRole.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("commanderId")) {
                    retVal.commanderId = UUID.fromString(wn2.getTextContent());
                }
            }
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }
        return retVal;
    }

    /**
     * Worker function that calculates the total weight of a force with the given ID
     *
     * @param campaign Campaign in which the force resides
     * @param forceId  Force for which to calculate weight
     *
     * @return Total force weight
     */
    public static double calculateTotalWeight(Campaign campaign, int forceId) {
        double weight = 0.0;

        for (UUID id : campaign.getForce(forceId).getUnits()) {
            try {
                Unit unit = campaign.getUnit(id);
                Entity entity = unit.getEntity();
                long entityType = entity.getEntityType();

                boolean isClan = campaign.isClanCampaign();

                CampaignOptions campaignOptions = campaign.getCampaignOptions();
                if (campaignOptions.isUseAtB() && !campaignOptions.isUseStratCon()) {
                    if (entityType == ETYPE_TANK) {
                        if (isClan || campaignOptions.isAdjustPlayerVehicles()) {
                            weight += entity.getWeight() * 0.5;
                        } else {
                            weight += entity.getWeight();
                        }
                    } else if (entityType == ETYPE_AEROSPACE_FIGHTER) {
                        if (isClan) {
                            weight += entity.getWeight() * 0.5;
                        } else {
                            weight += entity.getWeight();
                        }
                    }
                } else {
                    weight += entity.getWeight();
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to parse unit ID {}: {}", forceId, exception);
            }
        }

        return weight;
    }

    /**
     * This static method updates the combat teams across the campaign. It starts at the top level force, and calculates
     * the combat teams for each sub-force. It keeps only the eligible combat teams and imports them into the campaign.
     * After every formation is processed, an 'OrganizationChangedEvent' is triggered by that force.
     *
     * @param campaign the current campaign.
     */
    public static void recalculateCombatTeams(Campaign campaign) {
        Hashtable<Integer, CombatTeam> combatTeamsTable = campaign.getCombatTeamsAsMap();
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
        recalculateSubForceStrategicStatus(campaign, campaign.getCombatTeamsAsMap(), force);
    }

    /**
     * This method is used to update the combat teams for the campaign working downwards from a specified node, through
     * all of its sub-forces. It creates a new {@link CombatTeam} for each sub-force and checks its eligibility.
     * Eligible formations are imported into the campaign, and the combat team status of the respective force is set to
     * {@code true}. After every force is processed, an 'OrganizationChangedEvent' is triggered. This function runs
     * recursively on each sub-force, effectively traversing the complete TO&E.
     *
     * @param campaign    the current {@link Campaign}.
     * @param workingNode the {@link Force} node from which the method starts working down through all its sub-forces.
     */
    private static void recalculateSubForceStrategicStatus(Campaign campaign,
          Hashtable<Integer, CombatTeam> combatTeamsTable, Force workingNode) {

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
            recalculateSubForceStrategicStatus(campaign, campaign.getCombatTeamsAsMap(), force);
        }
    }

    /**
     * Retrieves the force associated with the given campaign using the stored force ID.
     *
     * <p>
     * This method returns a {@link Force} object corresponding to the stored {@code forceId}, if it exists within the
     * specified campaign. If no matching force is found, {@code null} is returned.
     * </p>
     *
     * @param campaign the campaign containing the forces to search for the specified {@code forceId}
     *
     * @return the {@link Force} object associated with the {@code forceId}, or {@code null} if not found
     */
    public @Nullable Force getForce(Campaign campaign) {
        return campaign.getForce(forceId);
    }
}
