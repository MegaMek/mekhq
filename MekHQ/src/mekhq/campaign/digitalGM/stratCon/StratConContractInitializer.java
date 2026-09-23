/*
 * Copyright (C) 2019-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.digitalGM.stratCon;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static mekhq.campaign.digitalGM.stratCon.SupportPointNegotiation.negotiateInitialSupportPoints;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.util.weightedMaps.WeightedIntMap;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.ObjectiveParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.PointOfInterestParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityFactory;
import mekhq.campaign.digitalGM.stratCon.gm.StratConGMs;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.*;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.LatitudeBand;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.PlanetProfile;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.SectorShapeProfile;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.SectorSpec;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorCountMethod;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorGenerator;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorPlanner;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorShape;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.contractGeneration.AbstractContractGeneration;
import mekhq.campaign.mission.contract.contractGeneration.TrackIntensityTable;
import mekhq.campaign.mission.contract.utilities.ContractCharacteristics;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import mekhq.campaign.mission.scenarios.atb.AtBScenarioModifier;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.enums.Alphabet;

/**
 * This class handles StratCon state initialization when a contract is signed.
 */
public class StratConContractInitializer {
    private static final MMLogger LOGGER = MMLogger.create(StratConContractInitializer.class);

    public static final int NUM_FORMATIONS_PER_TRACK = 3;
    public static final int ZERO_CELSIUS_IN_KELVIN = 273;

    /** Legacy sizing: hexes per required formation, laid out as a wider-than-tall rectangle. */
    private static final int LEGACY_HEXES_PER_FORMATION = 28;

    /**
     * Improved sizing: combat teams per assumed recon force. One in three of the teams fronting a sector is taken to be
     * out scouting it.
     */
    static final int COMBAT_TEAMS_PER_RECON_FORCE = 3;

    /**
     * Improved sizing: the land hexes one recon combat team is expected to cover in a quarter - three hexes a week over
     * the twelve weeks of three months. A sector is sized so the recon teams fronting it can scout all of its dry
     * ground inside three months.
     *
     * <p>Note what the underlying mechanic actually reveals, before revising this: a <em>regular</em> deployment
     * uncovers a single hex (scan range is zero unless a facility grants more), while a Patrol-role deployment covers
     * its hex plus the six around it.</p>
     */
    static final int RECON_HEXES_PER_QUARTER = 36;

    /** Improved sizing: the minimum dry fraction of a sector, so oceans never leave too little land. */
    private static final double MINIMUM_LAND_FRACTION = 0.25;

    /**
     * Improved sizing: the most hexes a generated sector may cover, whatever the planet and options ask for. Roughly a
     * 32x32 sector. Without it, a large ocean world with a condensed multi-unit sector and a doubled size multiplier
     * asks for several thousand hexes.
     */
    private static final int MAX_SECTOR_HEXES = 1024;

    /**
     * Upper bound on the {@link #maximumTeamsPerSector} search. A dry, small world with a reduced size multiplier fits
     * a great many teams into one sector, and nothing needs the exact figure past this - it only has to be larger than
     * any contract will ever ask for.
     */
    private static final int MAXIMUM_TEAMS_PER_SECTOR_SEARCH_LIMIT = 1000;

    /**
     * The most of a sector's dry land that may be given over to facilities, leaving the rest for scenarios to spawn on
     * over the life of the contract. Ordinary contracts sit well under this; it bites when a large contract's
     * facilities - which scale with its combat teams - are concentrated into few sectors whose area is capped.
     */
    private static final double MAXIMUM_FACILITY_COVERAGE = 0.5;

    /** Improved sizing: bounds on either dimension, so no shape profile can produce a sliver or a runaway map. */
    public static final int MIN_SECTOR_DIMENSION = 4;
    private static final int MAX_SECTOR_DIMENSION = 48;

    /** Terrain given to a newly-exposed hex that has no mapped neighbor to take after (an otherwise blank sector). */
    private static final String DEFAULT_FILL_TERRAIN = "Plains";

    /**
     * Initializes the campaign state given a contract, campaign and contract definition
     */
    public static void initializeCampaignState(AbstractContract contract, Campaign campaign,
          StratConContractDefinition contractDefinition) {
        StratConCampaignState campaignState = new StratConCampaignState(contract);
        campaignState.setBriefingText(contractDefinition.getBriefing() +
                                            "<br/>" +
                                            contract.getCommandRights().getStratConText());
        campaignState.setAllowEarlyVictory(contractDefinition.isAllowEarlyVictory());

        // dependency: this is required here in order for scenario initialization to
        // work properly
        contract.setStratConCampaignState(campaignState);

        // First, initialize the proper number of tracks. Then: for each objective:
        // step 1: calculate objective count if scaled, multiply # required lances by factor, round up, otherwise just
        // fixed number
        // step 2: evenly distribute objectives through tracks if uneven number is remaining, distribute randomly
        // when objective is specific scenario victory, place specially flagged scenarios when objective is
        // allied/hostile facility, place those facilities

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isUseMaplessMode = campaignOptions.isUseStratConMaplessMode();

        // Resolve the contract's destination planet once (not the campaign's current location); its data drives every
        // sector's size and temperature.
        PlanetProfile planetProfile = PlanetProfile.from(contract, campaign);

        // Decide how many sectors to generate and how large each one is. The planner always returns at least one
        // sector, so no separate zero-sector fallback is needed.
        List<SectorSpec> sectorSpecs = StratConSectorPlanner.generateSectorSpecs(contract.getScale(),
              campaignOptions.get(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD),
              maximumTeamsPerSector(planetProfile,
                    campaignOptions.get(CampaignOption.STRAT_CON_SECTOR_SIZE_MULTIPLIER)));

        // Ares Conventions: when both the employer and the enemy are signatories, urban targeting is off-limits.
        int year = campaign.getLocalDate().getYear();
        boolean allowCities = !(contract.getEmployerFaction().isAresConventionsSignatory(year) &&
                                      contract.getEnemyFaction().isAresConventionsSignatory(year));

        // Sectors are named after the Greek alphabet in order (Sector Alpha, Sector Beta, ...). When there are more
        // sectors than letters the letters wrap around, so first tally how many sectors will share each letter; any
        // letter used more than once is disambiguated with a running suffix (Sector Alpha-1, Sector Alpha-2, ...).
        Alphabet[] greekLetters = Alphabet.values();
        int[] letterTotals = new int[greekLetters.length];
        for (int index = 0; index < sectorSpecs.size(); index++) {
            letterTotals[index % greekLetters.length]++;
        }
        int[] letterSeen = new int[greekLetters.length];

        for (int index = 0; index < sectorSpecs.size(); index++) {
            int scenarioOdds = getScenarioOdds(contractDefinition);
            int deploymentTime = isUseMaplessMode ? 0 : getDeploymentTime(contractDefinition);

            StratConTrackState track = initializeTrackState(sectorSpecs.get(index),
                  planetProfile,
                  campaignOptions,
                  allowCities,
                  scenarioOdds,
                  deploymentTime);

            int letterIndex = index % greekLetters.length;
            String greek = greekLetters[letterIndex].getGreek();
            if (letterTotals[letterIndex] > 1) {
                track.setDisplayableName(String.format("Sector %s-%d", greek, ++letterSeen[letterIndex]));
            } else {
                track.setDisplayableName(String.format("Sector %s", greek));
            }
            campaignState.addTrack(track);
        }

        // now seed the tracks with objectives and facilities
        if (!isUseMaplessMode) {
            for (ObjectiveParameters objectiveParams : contractDefinition.getObjectiveParameters()) {
                int objectiveCount = objectiveParams.objectiveCount > 0 ?
                                           (int) objectiveParams.objectiveCount :
                                           (int) max(1,
                                                 -objectiveParams.objectiveCount * contract.getScale());

                List<Integer> trackObjects = trackObjectDistribution(objectiveCount, campaignState.getTrackCount());

                for (int x = 0; x < trackObjects.size(); x++) {
                    int numObjects = trackObjects.get(x);

                    switch (objectiveParams.objectiveType) {
                        case SpecificScenarioVictory:
                            // Specific-scenario objectives are no longer all placed up front. They spawn over the
                            // contract's months, driven by the contract's scenario schedule - see
                            // spawnScheduledStrategicScenarios, called from the daily StratCon lifecycle.
                            break;
                        case AlliedFacilityControl:
                            initializeTrackFacilities(campaignState.getTrack(x),
                                  numObjects,
                                  ForceAlignment.Allied,
                                  true,
                                  objectiveParams.objectiveScenarioModifiers);
                            break;
                        case HostileFacilityControl:
                        case FacilityDestruction:
                            initializeTrackFacilities(campaignState.getTrack(x),
                                  numObjects,
                                  ForceAlignment.Opposing,
                                  true,
                                  objectiveParams.objectiveScenarioModifiers);
                            break;
                        case PointOfInterest:
                            // Point of interest objectives are not placed up front either. They appear over the
                            // contract's months on a schedule rolled below - see schedulePointsOfInterest.
                            break;
                        case AnyScenarioVictory:
                            // set up a "win X scenarios" objective
                            StratConStrategicObjective sso = new StratConStrategicObjective();
                            sso.setDesiredObjectiveCount(numObjects);
                            sso.setObjectiveType(StrategicObjectiveType.AnyScenarioVictory);
                            campaignState.getTrack(x).addStrategicObjective(sso);

                            // modifiers defined for "any scenario" by definition apply to any scenario
                            // so they get added to the global campaign modifiers. Use sparingly since
                            // this can snowball pretty quickly.
                            if (objectiveParams.objectiveScenarioModifiers != null) {
                                for (String modifier : objectiveParams.objectiveScenarioModifiers) {
                                    if (!campaignState.getGlobalScenarioModifiers().contains(modifier)) {
                                        campaignState.getGlobalScenarioModifiers().add(modifier);
                                    }
                                }
                            }

                            break;
                    }
                }
            }
        }

        // if any modifiers are to be applied across all scenarios in the campaign
        // do so here; do not add duplicates
        if (contractDefinition.getGlobalScenarioModifiers() != null) {
            for (String modifier : contractDefinition.getGlobalScenarioModifiers()) {
                if (!campaignState.getGlobalScenarioModifiers().contains(modifier)) {
                    campaignState.getGlobalScenarioModifiers().add(modifier);
                }
            }
        }

        // non-objective allied facilities
        if (!isUseMaplessMode) {
            int facilityCount = contractDefinition.getAlliedFacilityCount() > 0 ?
                                      (int) contractDefinition.getAlliedFacilityCount() :
                                      (int) (-contractDefinition.getAlliedFacilityCount() *
                                                   contract.getScale());

            List<Integer> trackObjects = trackObjectDistribution(facilityCount, campaignState.getTrackCount());

            for (int x = 0; x < trackObjects.size(); x++) {
                int numObjects = trackObjects.get(x);

                initializeTrackFacilities(campaignState.getTrack(x),
                      numObjects,
                      ForceAlignment.Allied,
                      false,
                      Collections.emptyList());
            }

            // non-objective hostile facilities
            facilityCount = contractDefinition.getHostileFacilityCount() > 0 ?
                                  (int) contractDefinition.getHostileFacilityCount() :
                                  (int) (-contractDefinition.getHostileFacilityCount() *
                                               contract.getScale());

            trackObjects = trackObjectDistribution(facilityCount, campaignState.getTrackCount());

            for (int x = 0; x < trackObjects.size(); x++) {
                int numObjects = trackObjects.get(x);

                initializeTrackFacilities(campaignState.getTrack(x),
                      numObjects,
                      ForceAlignment.Opposing,
                      false,
                      Collections.emptyList());
            }
        }

        // Now that facilities exist, fold the planet-owner's facilities into each sector's road network via the GM's
        // sector-generation strategy (a road-less generator ignores this). Facilities are only placed outside mapless.
        if (!isUseMaplessMode) {
            for (StratConTrackState track : campaignState.getTracks()) {
                connectFacilitiesToRoads(track, contract, campaign);
            }
        }

        // Pre-roll the days on which each strategic-objective scenario, and each point of interest, appears over the
        // contract's run.
        if (!isUseMaplessMode) {
            boolean isContractsUseSpecialMechanics =
                  campaignOptions.get(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS);

            // Most contracts with special points of interest get no Essential scenarios: those points of interest, and
            // the combat bonus paid for each one dealt with, take their place.
            if (!isReplacingEssentialScenarios(contract, isContractsUseSpecialMechanics)) {
                scheduleStrategicScenarioSpawnDates(contract, campaignState);
            }

            schedulePointsOfInterest(contract,
                  contractDefinition,
                  campaignState,
                  campaignOptions.get(CampaignOption.MULTIPLY_TRACK_INTENSITY_BY_SCALE),
                  isContractsUseSpecialMechanics);

            // A Diversionary Raid's strategic objective is to escalate the fighting, rather than to hit any one target.
            if (isContractsUseSpecialMechanics && contract.getObjectiveType().isDiversionaryRaid()) {
                StratConEscalation.addDiversionaryRaidObjective(contract, campaignState);
            }

            // A Garrison Duty contract starts with its Escalation - the unrest the garrison must calm - at its maximum.
            if (isContractsUseSpecialMechanics) {
                StratConEscalation.startEscalation(contract, campaignState);
            }

            // A Recon Raid's sectors must each be scouted, alongside its Essential scenarios.
            if (StratConReconnaissance.usesReconnaissance(contract, isContractsUseSpecialMechanics)) {
                StratConReconnaissance.addReconnaissanceObjectives(campaignState);
            }
        }

        // Required victory points depend on the StratCon state
        contract.setRequiredVictoryPoints(ContractCharacteristics.bakeRequiredVictoryPoints(
              AbstractContractGeneration.determineRequiredVictoryPoints(contract), contract));

        // Determine starting Support Points
        negotiateInitialSupportPoints(campaign, contract);
    }

    /**
     * Pre-rolls, at contract start, the calendar day each strategic-objective scenario will appear on.
     *
     * <p>The contract's scenario schedule ({@link mekhq.campaign.mission.contract.contractData.ContractIntensityData})
     * gives, per contract month, how many strategic scenarios appear that month. Each such scenario is assigned an
     * independent random day within that month's window, so they trickle in over the month rather than all landing on
     * the first day. The dates are stored on the campaign state and drained by the daily StratCon lifecycle, which
     * spawns whatever is due via {@link #spawnScheduledStrategicScenarios}.</p>
     *
     * <p>Schedule entries at or beyond the contract's final month are folded into that final month's window, so a
     * schedule longer than the contract still delivers every scenario (its tail lands in the last month); a schedule
     * shorter than the contract simply leaves the later months empty. Does nothing without a settled start date or a
     * schedule.</p>
     *
     * @param contract      the contract whose schedule is being laid out
     * @param campaignState the campaign state to store the rolled spawn dates on
     */
    private static void scheduleStrategicScenarioSpawnDates(AbstractContract contract,
          StratConCampaignState campaignState) {
        LocalDate startDate = contract.getStartDate();
        List<Integer> schedule = contract.getScenarioSchedule();
        if ((startDate == null) || schedule.isEmpty()) {
            return;
        }

        for (LocalDate spawnDate : rollSpawnDates(startDate, schedule, contract.getLengthInMonths())) {
            campaignState.addStrategicScenarioSpawnDate(spawnDate);
        }
    }

    /**
     * Turns a per-month schedule into calendar days: each item in a month gets an independent random day within that
     * month's window, so items trickle in over the month rather than all landing on its first day.
     *
     * <p>Schedule entries at or beyond the contract's final month are folded into that final month's window, so a
     * schedule longer than the contract still delivers every item (its tail lands in the last month); a schedule
     * shorter than the contract simply leaves the later months empty.</p>
     *
     * @param startDate      the contract's start date, which opens its first month
     * @param schedule       the per-month item counts
     * @param lengthInMonths the contract's length in months
     *
     * @return one day per scheduled item, in schedule order
     *
     * @author Illiani
     * @since 0.51.01
     */
    // Package-private rather than private so the calendar rules can be tested directly.
    static List<LocalDate> rollSpawnDates(LocalDate startDate, List<Integer> schedule, int lengthInMonths) {
        List<LocalDate> spawnDates = new ArrayList<>();
        int contractMonths = max(1, lengthInMonths);

        for (int entry = 0; entry < schedule.size(); entry++) {
            int itemsThisMonth = schedule.get(entry);
            if (itemsThisMonth <= 0) {
                continue;
            }

            // Entries past the final month share that month's window, so none are stranded by a short contract.
            int month = min(entry, contractMonths - 1);
            LocalDate windowStart = startDate.plusMonths(month);
            LocalDate windowEnd = startDate.plusMonths(month + 1L);
            int windowDays = (int) ChronoUnit.DAYS.between(windowStart, windowEnd);

            for (int item = 0; item < itemsThisMonth; item++) {
                LocalDate spawnDate = (windowDays > 0) ?
                                            windowStart.plusDays(Compute.randomInt(windowDays)) :
                                            windowStart;
                spawnDates.add(spawnDate);
            }
        }

        return spawnDates;
    }

    /**
     * Schedules every point of interest the contract definition asks for, both strategic objectives and ordinary ones,
     * to appear over the contract's run instead of all at once - the way strategic-objective scenarios do.
     *
     * <p>The points of interest are spread across the contract's months by the Track Intensity Tables (see
     * {@link TrackIntensityTable#rollScheduleForCount}); that per-month schedule is kept on the contract (see
     * {@link AbstractContract#getPointOfInterestSchedule()}). Each month's points of interest then get random days
     * within it (see {@link #rollSpawnDates}), and are stored on the campaign state for the daily StratCon lifecycle to
     * place as their days come (see {@link #spawnScheduledPointOfInterest}). Which point of interest lands on which day
     * is shuffled, so types are not bunched together.</p>
     *
     * <p>When the "Contracts Use Special Mechanics" option is on, a contract type with a special point of interest (see
     * {@link #getSpecialPointOfInterestTypeId}) ignores its definition's points of interest and schedules its special
     * ones instead (see {@link #scheduleSpecialPointsOfInterest}), and a Recon Raid schedules none at all (see
     * {@link StratConReconnaissance}). With it off, every contract schedules its definition's points of interest.</p>
     *
     * <p>Does nothing if the contract asks for no points of interest, or has no settled start date.</p>
     *
     * @param contract                        the contract being accepted
     * @param contractDefinition              its StratCon contract definition
     * @param campaignState                   the campaign state to store the scheduled points of interest on
     * @param isMultiplyTrackIntensityByScale whether the "Multiply Track Intensity by Scale" option is on
     * @param isContractsUseSpecialMechanics  whether the "Contracts Use Special Mechanics" option is on
     *
     * @author Illiani
     * @since 0.51.01
     */
    // Package-private rather than private so scheduling can be tested without standing up a whole contract.
    static void schedulePointsOfInterest(AbstractContract contract, StratConContractDefinition contractDefinition,
          StratConCampaignState campaignState, boolean isMultiplyTrackIntensityByScale,
          boolean isContractsUseSpecialMechanics) {
        // A Recon Raid using special mechanics has no points of interest at all: its sectors are scouted instead.
        if (StratConReconnaissance.usesReconnaissance(contract, isContractsUseSpecialMechanics)) {
            return;
        }

        String specialTypeId = getSpecialPointOfInterestTypeId(contract, isContractsUseSpecialMechanics);
        if (specialTypeId != null) {
            scheduleSpecialPointsOfInterest(contract, campaignState, isMultiplyTrackIntensityByScale, specialTypeId);
            return;
        }

        List<StratConScheduledPointOfInterest> requestedPointsOfInterest =
              getRequestedPointsOfInterest(contractDefinition, contract.getScale());
        if (requestedPointsOfInterest.isEmpty()) {
            return;
        }

        LocalDate startDate = contract.getStartDate();
        if (startDate == null) {
            LOGGER.warn("Contract {} has no start date, so its {} points of interest cannot be scheduled.",
                  contract.getName(),
                  requestedPointsOfInterest.size());
            return;
        }

        List<Integer> schedule = TrackIntensityTable.rollScheduleForCount(contract.getLengthInMonths(),
              requestedPointsOfInterest.size());
        contract.setPointOfInterestSchedule(schedule);

        List<LocalDate> spawnDates = rollSpawnDates(startDate, schedule, contract.getLengthInMonths());
        Collections.shuffle(requestedPointsOfInterest);

        int scheduledCount = min(requestedPointsOfInterest.size(), spawnDates.size());
        for (int index = 0; index < scheduledCount; index++) {
            StratConScheduledPointOfInterest scheduledPointOfInterest = requestedPointsOfInterest.get(index);
            scheduledPointOfInterest.setSpawnDate(spawnDates.get(index));
            campaignState.addScheduledPointOfInterest(scheduledPointOfInterest);
        }
    }

    /**
     * Decides which special point of interest, if any, a contract uses in place of its definition's own. Special points
     * of interest belong to the "Contracts Use Special Mechanics" option, and to these contract types:
     *
     * <ul>
     *     <li>Espionage: data caches (see {@link StratConDataCacheBehavior})</li>
     *     <li>Guerrilla Warfare: vulnerable infrastructure (see {@link StratConVulnerableInfrastructureBehavior})</li>
     *     <li>Mole Hunting: potential leads (see {@link StratConPotentialLeadBehavior})</li>
     *     <li>Assassination: leads on the target (see {@link StratConAssassinationLeadBehavior})</li>
     *     <li>Observation Raid: lookout points (see {@link StratConLookoutPointBehavior})</li>
     *     <li>Relief Duty: beleaguered forces (see {@link StratConBeleagueredForcesBehavior})</li>
     *     <li>Cadre Duty: training maneuvers (see {@link StratConTrainingManeuversBehavior})</li>
     *     <li>Diversionary Raid: high profile targets (see {@link StratConHighProfileTargetBehavior})</li>
     *     <li>Extraction Raid: VIPs (see {@link StratConVIPBehavior})</li>
     *     <li>Planetary Assault: strategic positions (see {@link StratConStrategicPositionBehavior})</li>
     *     <li>Retainer: scheduled parades (see {@link StratConScheduledParadeBehavior})</li>
     *     <li>Riot Duty: civil disobedience (see {@link StratConCivilDisobedienceBehavior})</li>
     *     <li>Sabotage: sabotage targets (see {@link StratConSabotageTargetBehavior})</li>
     *     <li>Terrorism: civilian infrastructure (see {@link StratConCivilianInfrastructureBehavior})</li>
     *     <li>Security Duty: security reviews (see {@link StratConSecurityReviewBehavior})</li>
     *     <li>Pirate Raid: plunder targets (see {@link StratConPlunderTargetBehavior})</li>
     *     <li>Objective Raid: target intelligence (see {@link StratConTargetIntelligenceBehavior})</li>
     *     <li>Garrison Duty: shows of force (see {@link StratConShowOfForceBehavior})</li>
     * </ul>
     *
     * <p>A contract with special points of interest schedules them in place of its definition's points of interest
     * (see {@link #scheduleSpecialPointsOfInterest}). Most also replace the contract's Essential scenarios (see
     * {@link #isReplacingEssentialScenarios}).</p>
     *
     * @param contract                       the contract
     * @param isContractsUseSpecialMechanics whether the "Contracts Use Special Mechanics" option is on
     *
     * @return the type ID of the contract's special point of interest, or {@code null} if it has none
     *
     * @author Illiani
     * @since 0.51.01
     */
    // Package-private rather than private so the decision can be tested directly.
    static @Nullable String getSpecialPointOfInterestTypeId(AbstractContract contract,
          boolean isContractsUseSpecialMechanics) {
        ContractObjectiveType objectiveType = contract.getObjectiveType();
        if (!isContractsUseSpecialMechanics || (objectiveType == null)) {
            return null;
        }

        if (objectiveType.isEspionage()) {
            return StratConDataCacheBehavior.TYPE_ID;
        }

        if (objectiveType.isGuerrillaWarfare()) {
            return StratConVulnerableInfrastructureBehavior.TYPE_ID;
        }

        if (objectiveType.isMoleHunting()) {
            return StratConPotentialLeadBehavior.TYPE_ID;
        }

        if (objectiveType.isAssassination()) {
            return StratConAssassinationLeadBehavior.TYPE_ID;
        }

        if (objectiveType.isObservationRaid()) {
            return StratConLookoutPointBehavior.TYPE_ID;
        }

        if (objectiveType.isReliefDuty()) {
            return StratConBeleagueredForcesBehavior.TYPE_ID;
        }

        if (objectiveType.isCadreDuty()) {
            return StratConTrainingManeuversBehavior.TYPE_ID;
        }

        if (objectiveType.isDiversionaryRaid()) {
            return StratConHighProfileTargetBehavior.TYPE_ID;
        }

        if (objectiveType.isExtractionRaid()) {
            return StratConVIPBehavior.TYPE_ID;
        }

        if (objectiveType.isPlanetaryAssault()) {
            return StratConStrategicPositionBehavior.TYPE_ID;
        }

        if (objectiveType.isRetainer()) {
            return StratConScheduledParadeBehavior.TYPE_ID;
        }

        if (objectiveType.isRiotDuty()) {
            return StratConCivilDisobedienceBehavior.TYPE_ID;
        }

        if (objectiveType.isSabotage()) {
            return StratConSabotageTargetBehavior.TYPE_ID;
        }

        if (objectiveType.isTerrorism()) {
            return StratConCivilianInfrastructureBehavior.TYPE_ID;
        }

        if (objectiveType.isSecurityDuty()) {
            return StratConSecurityReviewBehavior.TYPE_ID;
        }

        if (objectiveType.isPirateRaid()) {
            return StratConPlunderTargetBehavior.TYPE_ID;
        }

        if (objectiveType.isObjectiveRaid()) {
            return StratConTargetIntelligenceBehavior.TYPE_ID;
        }

        if (objectiveType.isGarrisonDuty()) {
            return StratConShowOfForceBehavior.TYPE_ID;
        }

        return null;
    }

    /**
     * Decides whether a contract's special points of interest (see {@link #getSpecialPointOfInterestTypeId}) replace
     * its Essential scenarios. Most do: such a contract gets no Essential scenarios, and the combat bonus is paid for
     * each special point of interest dealt with instead. Beleaguered forces, training maneuvers, high profile targets,
     * strategic positions, security reviews, target intelligence, and shows of force do not - Relief Duty, Cadre Duty,
     * Diversionary Raid, Planetary Assault, Security Duty, Objective Raid, and Garrison Duty contracts keep their
     * Essential scenarios alongside them.
     *
     * @param contract                       the contract
     * @param isContractsUseSpecialMechanics whether the "Contracts Use Special Mechanics" option is on
     *
     * @return {@code true} if the contract gets no Essential scenarios
     *
     * @author Illiani
     * @since 0.51.01
     */
    // Package-private rather than private so the decision can be tested directly.
    static boolean isReplacingEssentialScenarios(AbstractContract contract, boolean isContractsUseSpecialMechanics) {
        String specialTypeId = getSpecialPointOfInterestTypeId(contract, isContractsUseSpecialMechanics);
        return (specialTypeId != null)
                     && !StratConBeleagueredForcesBehavior.TYPE_ID.equals(specialTypeId)
                     && !StratConTrainingManeuversBehavior.TYPE_ID.equals(specialTypeId)
                     && !StratConHighProfileTargetBehavior.TYPE_ID.equals(specialTypeId)
                     && !StratConStrategicPositionBehavior.TYPE_ID.equals(specialTypeId)
                     && !StratConSecurityReviewBehavior.TYPE_ID.equals(specialTypeId)
                     && !StratConTargetIntelligenceBehavior.TYPE_ID.equals(specialTypeId)
                     && !StratConShowOfForceBehavior.TYPE_ID.equals(specialTypeId);
    }

    /**
     * Decides whether a special point of interest type is a strategic objective. Every one is, except high profile
     * targets - a Diversionary Raid's objective is its Escalation instead (see {@link StratConEscalation}) - target
     * intelligence, whose objectives are the facilities it leads to, and shows of force, which only calm a Garrison
     * Duty contract's Escalation.
     *
     * @param typeId the type ID of a special point of interest
     *
     * @return {@code true} if each point of interest of the type is a strategic objective
     *
     * @author Illiani
     * @since 0.51.01
     */
    // Package-private rather than private so the decision can be tested directly.
    static boolean isSpecialPointOfInterestObjective(String typeId) {
        return !StratConHighProfileTargetBehavior.TYPE_ID.equals(typeId)
                     && !StratConTargetIntelligenceBehavior.TYPE_ID.equals(typeId)
                     && !StratConShowOfForceBehavior.TYPE_ID.equals(typeId);
    }

    /**
     * Schedules a contract's special points of interest (see {@link #getSpecialPointOfInterestTypeId}). Each is a
     * strategic objective if its type is one (see {@link #isSpecialPointOfInterestObjective}).
     *
     * <p>They are scheduled the way the contract's Essential scenarios are: a roll on the Track Intensity Tables for the
     * contract's length and track count (see {@link TrackIntensityTable#rollSchedule}), made once per point of scale
     * when "Multiply Track Intensity by Scale" is on and once otherwise. That roll is independent of the scenarios' own,
     * and is kept on the contract (see {@link AbstractContract#getPointOfInterestSchedule()}). Each month's points of
     * interest then get random days within it (see {@link #rollSpawnDates}).</p>
     *
     * <p>Does nothing if the contract has no settled start date.</p>
     *
     * @param contract                        the contract being accepted
     * @param campaignState                   the campaign state to store the scheduled points of interest on
     * @param isMultiplyTrackIntensityByScale whether the "Multiply Track Intensity by Scale" option is on
     * @param typeId                          the type ID of the special point of interest to schedule
     *
     * @author Illiani
     * @since 0.51.01
     */
    // Package-private rather than private so scheduling can be tested without standing up a whole contract.
    static void scheduleSpecialPointsOfInterest(AbstractContract contract, StratConCampaignState campaignState,
          boolean isMultiplyTrackIntensityByScale, String typeId) {
        LocalDate startDate = contract.getStartDate();
        if (startDate == null) {
            LOGGER.warn("Contract {} has no start date, so its {} points of interest cannot be scheduled.",
                  contract.getName(),
                  typeId);
            return;
        }

        int rollCount = isMultiplyTrackIntensityByScale ? contract.getScale() : 1;
        List<Integer> schedule = TrackIntensityTable.rollSchedule(contract.getLengthInMonths(),
              contract.getTrackCount(),
              rollCount);
        contract.setPointOfInterestSchedule(schedule);

        boolean isStrategicObjective = isSpecialPointOfInterestObjective(typeId);
        List<StratConScheduledPointOfInterest> scheduledPointsOfInterest = new ArrayList<>();
        for (LocalDate spawnDate : rollSpawnDates(startDate, schedule, contract.getLengthInMonths())) {
            scheduledPointsOfInterest.add(new StratConScheduledPointOfInterest(spawnDate, typeId,
                  isStrategicObjective));
        }

        // Only so many target intelligence leads pan out into facilities: settled now, so it cannot be gamed later.
        if (StratConTargetIntelligenceBehavior.TYPE_ID.equals(typeId)) {
            markFacilityLeads(scheduledPointsOfInterest, max(1, contract.getScale()));
        }

        for (StratConScheduledPointOfInterest scheduledPointOfInterest : scheduledPointsOfInterest) {
            campaignState.addScheduledPointOfInterest(scheduledPointOfInterest);
        }
    }

    /**
     * Marks, at random, which of a contract's scheduled target intelligence will lead to a facility (see
     * {@link StratConTargetIntelligenceBehavior}): as many as the given count, or all of them if there are fewer.
     *
     * @param scheduledPointsOfInterest the contract's scheduled target intelligence
     * @param facilityLeadCount         how many may lead to a facility: one per point of the contract's scale
     *
     * @author Illiani
     * @since 0.51.01
     */
    // Package-private rather than private so the marking can be tested directly.
    static void markFacilityLeads(List<StratConScheduledPointOfInterest> scheduledPointsOfInterest,
          int facilityLeadCount) {
        List<StratConScheduledPointOfInterest> candidates = new ArrayList<>(scheduledPointsOfInterest);
        Collections.shuffle(candidates);

        int leads = min(facilityLeadCount, candidates.size());
        for (int index = 0; index < leads; index++) {
            candidates.get(index)
                  .getInitialState()
                  .put(StratConTargetIntelligenceBehavior.FACILITY_LEAD_STATE_KEY, Boolean.TRUE.toString());
        }
    }

    /**
     * Places a random hostile facility at an eligible hex of a sector, reveals it, and makes destroying it a strategic
     * objective of the sector - as the contract's own objective facilities are, but revealed. Respects the sector's
     * facility capacity.
     *
     * @param track    the sector to place the facility in
     * @param contract the contract whose map holds the sector
     * @param campaign the current campaign
     *
     * @return the hex the facility was placed on, or {@code null} if the sector had no room for it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConCoords spawnObjectiveFacility(StratConTrackState track, AbstractContract contract,
          Campaign campaign) {
        if (track.getOccupiedHexCount() >= facilityCapacity(track)) {
            return null;
        }

        StratConCoords coords = getUnoccupiedCoords(track);
        if (coords == null) {
            return null;
        }

        StratConFacility facility = StratConFacilityFactory.getRandomHostileFacility();
        facility.setOwner(ForceAlignment.Opposing);
        facility.setStrategicObjective(true);
        facility.setVisible(true);
        track.addFacility(coords, facility);
        track.getRevealedCoords().add(coords);

        StratConStrategicObjective objective = new StratConStrategicObjective();
        objective.setObjectiveCoords(coords);
        objective.setObjectiveType(StrategicObjectiveType.FacilityDestruction);
        track.addStrategicObjective(objective);

        // A new base belongs on the road grid if the planet's owner holds it, as any other placed base does.
        connectFacilitiesToRoads(track, contract, campaign);
        return coords;
    }

    /**
     * Lists every point of interest a contract definition asks for, without spawn dates: one per point of interest
     * objective (its type drawn at random from that objective's {@code objectivePointsOfInterest}), and one per
     * ordinary point of interest in its {@code pointsOfInterest}. Counts are worked out as the facility and objective
     * counts are - a negative count is scaled by the contract's size.
     *
     * <p>An objective with no types to choose from, or an ordinary entry with no type, is skipped and logged.</p>
     *
     * @param contractDefinition the contract definition
     * @param scale              the contract's scale
     *
     * @return the requested points of interest, in definition order
     *
     * @author Illiani
     * @since 0.51.01
     */
    // Package-private rather than private so the counting rules can be tested directly.
    static List<StratConScheduledPointOfInterest> getRequestedPointsOfInterest(
          StratConContractDefinition contractDefinition, int scale) {
        List<StratConScheduledPointOfInterest> requestedPointsOfInterest = new ArrayList<>();

        List<ObjectiveParameters> objectiveParameters = contractDefinition.getObjectiveParameters();
        if (objectiveParameters != null) {
            for (ObjectiveParameters objectiveParams : objectiveParameters) {
                if (objectiveParams.getObjectiveType() != StrategicObjectiveType.PointOfInterest) {
                    continue;
                }

                List<String> typeIds = objectiveParams.getObjectivePointsOfInterest();
                if (typeIds.isEmpty()) {
                    LOGGER.warn("A point of interest objective in contract definition {} lists no types; skipping it.",
                          contractDefinition.getContractTypeName());
                    continue;
                }

                // As for every other objective type, a scaled count never drops below one.
                int objectiveCount = (objectiveParams.getObjectiveCount() > 0) ?
                                           (int) objectiveParams.getObjectiveCount() :
                                           (int) max(1, -objectiveParams.getObjectiveCount() * scale);

                for (int objectiveIndex = 0; objectiveIndex < objectiveCount; objectiveIndex++) {
                    String typeId = typeIds.get(Compute.randomInt(typeIds.size()));
                    requestedPointsOfInterest.add(new StratConScheduledPointOfInterest(null, typeId, true));
                }
            }
        }

        for (PointOfInterestParameters pointOfInterestParameters : contractDefinition.getPointsOfInterest()) {
            String typeId = pointOfInterestParameters.getTypeId();
            if ((typeId == null) || typeId.isBlank()) {
                LOGGER.warn("A point of interest entry in contract definition {} has no type; skipping it.",
                      contractDefinition.getContractTypeName());
                continue;
            }

            // As for non-objective facilities, a scaled count may round down to none.
            int pointOfInterestCount = (pointOfInterestParameters.getCount() > 0) ?
                                             (int) pointOfInterestParameters.getCount() :
                                             (int) (-pointOfInterestParameters.getCount() * scale);

            for (int pointOfInterestIndex = 0; pointOfInterestIndex < pointOfInterestCount; pointOfInterestIndex++) {
                requestedPointsOfInterest.add(new StratConScheduledPointOfInterest(null, typeId, false));
            }
        }

        return requestedPointsOfInterest;
    }

    /**
     * Places a scheduled point of interest whose day has come, in one of the contract's sectors: tried in a random
     * order until one has an eligible hex for it (see {@link StratConPointOfInterestPlacer}). Its lifespan counts from
     * today. Does nothing in mapless mode, where there is no map to place it on.
     *
     * @param campaign                 the current campaign
     * @param contract                 the contract the point of interest belongs to
     * @param scheduledPointOfInterest the point of interest to place
     *
     * @return the placed point of interest, or {@code null} if no sector had room for it (logged) or there is no map
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConPointOfInterest spawnScheduledPointOfInterest(Campaign campaign,
          AbstractContract contract, StratConScheduledPointOfInterest scheduledPointOfInterest) {
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if ((campaignState == null) || campaign.getCampaignOptions().isUseStratConMaplessMode()) {
            return null;
        }

        List<StratConTrackState> tracks = new ArrayList<>(campaignState.getTracks());
        Collections.shuffle(tracks);

        for (StratConTrackState track : tracks) {
            StratConPointOfInterest pointOfInterest = scheduledPointOfInterest.isStrategicObjective() ?
                                                            StratConPointOfInterestPlacer.placeAsStrategicObjective(
                                                                  track,
                                                                  scheduledPointOfInterest.getTypeId(),
                                                                  null,
                                                                  campaign.getLocalDate()) :
                                                            StratConPointOfInterestPlacer.place(track,
                                                                  scheduledPointOfInterest.getTypeId(),
                                                                  null,
                                                                  campaign.getLocalDate());
            if (pointOfInterest != null) {
                // Anything the point of interest was told when it was scheduled goes with it onto the map.
                scheduledPointOfInterest.getInitialState().forEach(pointOfInterest::setStateValue);
                return pointOfInterest;
            }
        }

        LOGGER.info("No sector of contract {} had room for scheduled point of interest {}.",
              contract.getName(),
              scheduledPointOfInterest);
        return null;
    }

    /**
     * Retrieves a random deployment time from the provided {@link StratConContractDefinition}.
     *
     * <p>The deployment time is selected randomly from the list of deployment times in the
     * given {@code StratConContractDefinition}.</p>
     *
     * @param contractDefinition the contract definition containing deployment time options
     *
     * @return a randomly selected deployment time
     *
     * @throws IllegalArgumentException if the list of deployment times is empty
     * @throws NullPointerException     if {@code contractDefinition} or its deployment times list is null
     * @author Illiani
     * @since 0.50.05
     */
    private static int getDeploymentTime(StratConContractDefinition contractDefinition) {
        return contractDefinition.getDeploymentTimes()
                     .get(Compute.randomInt(contractDefinition.getDeploymentTimes().size()));
    }

    /**
     * Retrieves a random scenario odds value from the provided {@link StratConContractDefinition}.
     *
     * <p>The scenario odds are selected randomly from the list of scenario odds in the
     * given {@code StratConContractDefinition}.</p>
     *
     * @param contractDefinition the contract definition containing scenario odds options
     *
     * @return a randomly selected scenario odds value
     *
     * @throws IllegalArgumentException if the list of scenario odds is empty
     * @throws NullPointerException     if {@code contractDefinition} or its scenario odds list is null
     * @author Illiani
     * @since 0.50.05
     */
    public static int getScenarioOdds(StratConContractDefinition contractDefinition) {
        return contractDefinition.getScenarioOdds().get(Compute.randomInt(contractDefinition.getScenarioOdds().size()));
    }


    /**
     * Sets up the initial state of a single track from its {@link SectorSpec} and the destination planet's profile.
     *
     * <p>Sizing and temperature follow one of two regimes, chosen by
     * {@link StratConSectorCountMethod#usesImprovedSizing()}. Every method but {@code LEGACY} uses the improved sizing
     * (a per-quarter scouting budget scaled by the sector's combat teams, planetary size, the size multiplier, and
     * hydrology) and a latitude-driven temperature. {@code LEGACY} keeps the historical behaviour: a rectangle sized
     * from the formation count, and a broad equatorial temperature swing with no latitude input.</p>
     *
     * @param sector          the sector blueprint (required combat teams, latitude band)
     * @param planetProfile   the destination planet's resolved data
     * @param campaignOptions the campaign options governing which sizing/temperature regime applies
     * @param allowCities     {@code true} to allow the improved terrain generator to place cities; {@code false}
     *                        suppresses them (has no effect on the legacy terrain path, which places no cities)
     * @param scenarioOdds    the per-track scenario odds
     * @param deploymentTime  the per-track deployment time
     *
     * @return the initialized track
     */
    public static StratConTrackState initializeTrackState(SectorSpec sector, PlanetProfile planetProfile,
          CampaignOptions campaignOptions, boolean allowCities, int scenarioOdds, int deploymentTime) {
        StratConTrackState retVal = new StratConTrackState();
        retVal.setRequiredLanceCount(sector.requiredLances());

        boolean useImprovedSizing = campaignOptions.get(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD)
                                          .usesImprovedSizing();

        if (useImprovedSizing) {
            applyImprovedDimensions(retVal,
                  sector,
                  planetProfile,
                  campaignOptions.get(CampaignOption.STRAT_CON_SECTOR_SIZE_MULTIPLIER));
            retVal.setTemperature(improvedTemperature(planetProfile, sector.latitudeBand()));
        } else {
            applyLegacyDimensions(retVal, sector.requiredLances());
            retVal.setTemperature(legacyTemperature(planetProfile.temperatureCelsius()));
        }

        retVal.setScenarioOdds(scenarioOdds);
        retVal.setDeploymentTime(deploymentTime);

        // Place terrain via the GM's sector-generation strategy (improved geography-aware pipeline or legacy placer).
        StratConGMs.sectorGeneration(campaignOptions)
              .initializeTrack(retVal, planetProfile, sector.latitudeBand(), allowCities);

        return retVal;
    }

    /**
     * Regenerates a single track's terrain in place, as used by the GM "Regenerate Sector" tool. Clears the existing
     * terrain, cities, and fog, then re-runs terrain generation - the improved geography-aware generator when the
     * alternate-terrain option is set, otherwise the legacy placer. The track's dimensions are kept, but a fresh
     * latitude band is rolled and the temperature is recomputed from it, and assigned forces are left untouched.
     * Scenarios and facilities are preserved, but any that the new coastline leaves on an ocean hex are relocated back
     * onto land.
     *
     * @param track    the track to regenerate
     * @param contract the contract the track belongs to (source of the planet profile and Ares-Conventions status)
     * @param campaign the campaign (source of options and the current date)
     */
    public static void regenerateTrack(StratConTrackState track, AbstractContract contract, Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        PlanetProfile planetProfile = PlanetProfile.from(contract, campaign);

        int year = campaign.getLocalDate().getYear();
        boolean allowCities = !(contract.getEmployerFaction().isAresConventionsSignatory(year) &&
                                      contract.getEnemyFaction().isAresConventionsSignatory(year));

        // Re-roll the latitude band and recompute the temperature from it (matching initializeTrackState), so a
        // regenerated sector's climate actually changes and drives the new biome selection - not just its terrain.
        boolean useImprovedSizing = campaignOptions.get(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD)
                                          .usesImprovedSizing();
        LatitudeBand latitudeBand = LatitudeBand.random();
        if (useImprovedSizing) {
            track.setTemperature(improvedTemperature(planetProfile, latitudeBand));
        } else {
            track.setTemperature(legacyTemperature(planetProfile.temperatureCelsius()));
        }

        // Re-roll the sector's proportions too, so regenerating does not keep handing back the same shape of map.
        if (useImprovedSizing) {
            reshapeForRegeneration(track);
        }

        var sectorStrategy = StratConGMs.sectorGeneration(campaignOptions);
        sectorStrategy.regenerateTrack(track, planetProfile, latitudeBand, allowCities);

        // A regenerated coastline can leave existing facilities and scenarios sitting on new ocean hexes; move them
        // back onto land.
        relocateOccupantsOffOcean(track);

        // Fold the planet-owner's facilities into the (possibly rebuilt) road network; a road-less generator ignores it.
        connectFacilitiesToRoads(track, contract, campaign);
    }

    /**
     * What a proposed resize would disturb, so a GM can be told before anything is moved.
     *
     * @param facilities                how many facilities would be displaced back inside the sector
     * @param scenarios                 how many scenarios would be displaced back inside the sector
     * @param pointsOfInterest          how many points of interest would be displaced back inside the sector
     * @param occupyingPointsOfInterest how many of those points of interest occupy their hex, and so need a free hex
     *                                  of their own
     * @param objectives                how many strategic objectives sit on the ground being cut away
     * @param forces                    how many deployed forces would be recalled
     * @param freeHexes                 how many hexes inside the new bounds are free to receive a displaced occupant
     */
    public record ResizeImpact(int facilities, int scenarios, int pointsOfInterest, int occupyingPointsOfInterest,
          int objectives, int forces, int freeHexes) {
        public boolean isEmpty() {
            return (facilities == 0) &&
                         (scenarios == 0) &&
                         (pointsOfInterest == 0) &&
                         (objectives == 0) &&
                         (forces == 0);
        }

        /**
         * @return how many facilities, scenarios, and occupying points of interest would have to be found a free hex
         *       inside the sector. Points of interest that do not occupy their hex can share one, so they are not
         *       counted.
         */
        public int displacedOccupants() {
            return facilities + scenarios + occupyingPointsOfInterest;
        }

        /**
         * @return {@code true} if the sector would still have somewhere to put everything that must move. A resize that
         *       does not fit is refused rather than performed, because the alternative is destroying bases and
         *       scenarios - and any strategic objective riding on them - to make the numbers work.
         */
        public boolean fits() {
            return displacedOccupants() <= freeHexes;
        }
    }

    /**
     * Reports what {@link #resizeTrack} would disturb at the given size, without changing anything.
     *
     * @param track     the track to be resized
     * @param newWidth  the proposed width
     * @param newHeight the proposed height
     *
     * @return a tally of the occupants that would have to be moved or recalled
     */
    public static ResizeImpact previewResize(StratConTrackState track, int newWidth, int newHeight) {
        int facilities = 0;
        int scenarios = 0;
        int pointsOfInterest = 0;
        int occupyingPointsOfInterest = 0;
        int objectives = 0;
        int forces = 0;

        for (StratConCoords coords : track.getFacilities().keySet()) {
            if (isOutside(coords, newWidth, newHeight)) {
                facilities++;
            }
        }
        for (StratConCoords coords : track.getScenarios().keySet()) {
            if (isOutside(coords, newWidth, newHeight)) {
                scenarios++;
            }
        }
        for (StratConPointOfInterest pointOfInterest : track.getPointsOfInterest()) {
            if ((pointOfInterest.getCoords() != null) && isOutside(pointOfInterest.getCoords(), newWidth, newHeight)) {
                pointsOfInterest++;
                if (pointOfInterest.occupiesHex()) {
                    occupyingPointsOfInterest++;
                }
            }
        }
        for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
            if ((objective.getObjectiveCoords() != null) &&
                      isOutside(objective.getObjectiveCoords(), newWidth, newHeight)) {
                objectives++;
            }
        }
        for (StratConCoords coords : track.getAssignedForceCoords().values()) {
            if (isOutside(coords, newWidth, newHeight)) {
                forces++;
            }
        }

        return new ResizeImpact(facilities,
              scenarios,
              pointsOfInterest,
              occupyingPointsOfInterest,
              objectives,
              forces,
              freeHexes(track, newWidth, newHeight));
    }

    /**
     * Counts the hexes that would still be able to take a relocated occupant at the proposed size, mirroring what
     * {@link #getUnoccupiedCoords(StratConTrackState)} considers eligible: dry land that is not occupied (see
     * {@link StratConTrackState#isHexOccupied}) and holds no deployed force. Anything already inside the new bounds
     * keeps its hex, so it is counted as taken.
     */
    private static int freeHexes(StratConTrackState track, int newWidth, int newHeight) {
        Collection<StratConCoords> forceCoords = track.getAssignedForceCoords().values();
        int free = 0;

        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                StratConCoords coords = new StratConCoords(x, y);
                boolean available = !StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords)) &&
                                          !track.isHexOccupied(coords) &&
                                          !forceCoords.contains(coords);
                if (available) {
                    free++;
                }
            }
        }

        return free;
    }

    /**
     * Resizes a sector, growing or shrinking it at its right and bottom edges.
     *
     * <p>Only those two edges are offered on purpose. StratCon hexes sit on a parity-offset grid - a hex's neighbors
     * depend on whether its x is odd or even (see {@link StratConCoords#translate}) - so shifting every hex to make
     * room at the left or top would silently rewire the whole map's adjacency and tear apart coastlines, ranges, and
     * roads. Growing at the far edges leaves every existing hex on its original coordinates.</p>
     *
     * <p>Ground outside the new bounds is discarded, but its occupants are not: facilities, scenarios, and points of
     * interest are moved back inside (with their strategic objectives), and any force left standing outside is
     * recalled. Call
     * {@link #previewResize} first so the GM knows what is about to move.</p>
     *
     * @param track     the track to resize
     * @param newWidth  the new width, at least 1
     * @param newHeight the new height, at least 1
     * @param contract  the contract, for the planet-owner road rules
     * @param campaign  the campaign, for the current date and options
     */
    public static boolean resizeTrack(StratConTrackState track, int newWidth, int newHeight, AbstractContract contract,
          Campaign campaign) {
        int width = max(1, newWidth);
        int height = max(1, newHeight);

        // Refuse a size with nowhere to put everything that would be displaced, BEFORE touching the track. Squeezing
        // them out would mean destroying bases and scenarios - and any strategic objective riding on them, which would
        // quietly make the contract unwinnable.
        if (!previewResize(track, width, height).fits()) {
            return false;
        }

        applyNewBounds(track, width, height);

        fillNewHexes(track);

        applyTerrainChange(track, contract, campaign);
        return true;
    }

    /**
     * Moves a sector's bounds and puts its occupants back inside: ground outside the new bounds is discarded, but
     * facilities, scenarios, and occupying points of interest are relocated to free hexes (carrying their strategic
     * objectives and any forces deployed to them), points of interest that do not occupy their hex are pulled in to the
     * nearest edge, and a force left standing off the map is recalled.
     *
     * <p>Callers must check {@link #previewResize} first - this assumes the sector can hold what it is about to
     * displace.</p>
     */
    private static void applyNewBounds(StratConTrackState track, int width, int height) {
        // Note who is about to be left outside before the bounds move, so they can be re-homed afterward.
        List<StratConCoords> displacedFacilities = outsideCoords(track.getFacilities().keySet(), width, height);
        List<StratConCoords> displacedScenarios = outsideCoords(track.getScenarios().keySet(), width, height);
        List<StratConCoords> displacedOccupyingPointsOfInterest = outsideCoords(occupyingPointOfInterestCoords(track),
              width,
              height);
        List<StratConPointOfInterest> displacedNonOccupyingPointsOfInterest = new ArrayList<>();
        for (StratConPointOfInterest pointOfInterest : track.getPointsOfInterest()) {
            StratConCoords coords = pointOfInterest.getCoords();
            if (!pointOfInterest.occupiesHex() && (coords != null) && isOutside(coords, width, height)) {
                displacedNonOccupyingPointsOfInterest.add(pointOfInterest);
            }
        }

        track.setWidth(width);
        track.setHeight(height);
        track.trimToBounds();

        for (StratConCoords source : occupiedCoords(displacedFacilities,
              displacedScenarios,
              displacedOccupyingPointsOfInterest)) {
            StratConCoords destination = findRelocationCoords(track, source);
            if (destination == null) {
                // The capacity check above should have prevented this; drop the occupant rather than strand it outside
                // the sector, where it would be invisible and unreachable but still counted.
                LOGGER.warn("No room to relocate occupant at {} on track {}; removing it.",
                      source,
                      track.getDisplayableName());
                dropOccupant(track, source);
                continue;
            }

            relocateOccupant(track, source, destination);
        }

        pullInNonOccupyingPointsOfInterest(track, displacedNonOccupyingPointsOfInterest);

        // Forces left standing on ground that no longer exists are recalled.
        recallForcesOutsideBounds(track);
    }

    /**
     * Brings points of interest that do not occupy their hex back inside a shrunken sector, onto the nearest hex at its
     * new edge. They need no free hex of their own, so they are not scattered the way occupants are - but they are not
     * discarded with the ground either, since any of them may carry a strategic objective.
     *
     * <p>If that edge hex breaks the type's placement rules (see {@link StratConPointOfInterestPlacer#canPlace}), the
     * point of interest goes to a random hex that keeps them instead. Failing that, it stays on the edge hex - unless
     * it is land-only and that hex is ocean, in which case it goes to any free dry hex, and is removed only if the
     * sector has none.</p>
     *
     * @param track            the sector, already at its new bounds
     * @param pointsOfInterest the points of interest left outside those bounds
     */
    private static void pullInNonOccupyingPointsOfInterest(StratConTrackState track,
          List<StratConPointOfInterest> pointsOfInterest) {
        for (StratConPointOfInterest pointOfInterest : pointsOfInterest) {
            StratConCoords coords = pointOfInterest.getCoords();
            StratConCoords edgeCoords = new StratConCoords(min(coords.getX(), track.getWidth() - 1),
                  min(coords.getY(), track.getHeight() - 1));
            StratConCoords destination = edgeCoords;

            StratConPointOfInterestDefinition definition = pointOfInterest.getDefinition();
            if ((definition != null) && !StratConPointOfInterestPlacer.canPlace(track, definition, edgeCoords)) {
                StratConCoords eligibleCoords = StratConPointOfInterestPlacer.findPlacementCoords(track, definition);
                if (eligibleCoords != null) {
                    destination = eligibleCoords;
                } else if (isLandOnly(pointOfInterest) &&
                                 StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(edgeCoords))) {
                    destination = getUnoccupiedCoords(track);
                }
            }

            if ((destination == null) || !track.movePointOfInterest(pointOfInterest.getId(), destination)) {
                LOGGER.warn("No room to relocate point of interest {} on track {}; removing it.",
                      pointOfInterest,
                      track.getDisplayableName());
                dropPointOfInterest(track, pointOfInterest);
            }
        }
    }

    /**
     * Picks where to relocate the occupant of {@code source}. A point of interest that occupies the hex is sent to a
     * hex its type's placement rules allow (see {@link StratConPointOfInterestPlacer#findPlacementCoords}), kept on
     * dry land if a facility or scenario shares the hex and moves with it; anything else, or a point of interest with
     * no such hex, goes to any free dry hex.
     *
     * @param track  the sector
     * @param source the hex whose occupant is moving
     *
     * @return the hex to move it to, or {@code null} if there is none
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static @Nullable StratConCoords findRelocationCoords(StratConTrackState track, StratConCoords source) {
        StratConPointOfInterest occupyingPointOfInterest = track.getOccupyingPointOfInterest(source);
        StratConPointOfInterestDefinition definition = (occupyingPointOfInterest == null) ?
                                                             null :
                                                             occupyingPointOfInterest.getDefinition();

        if (definition != null) {
            StratConCoords eligibleCoords = StratConPointOfInterestPlacer.findPlacementCoords(track, definition);
            boolean sharedWithFacilityOrScenario = (track.getFacility(source) != null) ||
                                                         (track.getScenario(source) != null);
            boolean eligibleCoordsAreOcean = (eligibleCoords != null) &&
                                                   StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(
                                                         eligibleCoords));

            if ((eligibleCoords != null) && !(sharedWithFacilityOrScenario && eligibleCoordsAreOcean)) {
                return eligibleCoords;
            }
        }

        return getUnoccupiedCoords(track);
    }

    /**
     * Removes a point of interest that no hex can receive, along with any strategic objective tied to it - as a
     * facility that cannot be relocated takes its objective with it - so nothing is left pointing at a point of
     * interest that is gone.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void dropPointOfInterest(StratConTrackState track, StratConPointOfInterest pointOfInterest) {
        track.getStrategicObjectives()
              .removeAll(StratConPointOfInterestRules.getStrategicObjectives(track, pointOfInterest));
        track.removePointOfInterest(pointOfInterest.getId());
    }

    /** @return the hexes of every point of interest in the sector that occupies its hex. */
    private static List<StratConCoords> occupyingPointOfInterestCoords(StratConTrackState track) {
        List<StratConCoords> coords = new ArrayList<>();
        for (StratConPointOfInterest pointOfInterest : track.getPointsOfInterest()) {
            if ((pointOfInterest.getCoords() != null) && pointOfInterest.occupiesHex()) {
                coords.add(pointOfInterest.getCoords());
            }
        }
        return coords;
    }

    /**
     * @return {@code true} if the point of interest's definition keeps it off water; one whose type is no longer
     *       defined is not held to it
     */
    private static boolean isLandOnly(StratConPointOfInterest pointOfInterest) {
        StratConPointOfInterestDefinition definition = pointOfInterest.getDefinition();
        return (definition != null) && definition.isLandOnly();
    }

    /**
     * Collects the distinct hexes holding a displaced facility, scenario, or occupying point of interest, facility
     * hexes first, so that a hex shared by more than one is visited once.
     *
     * <p>A facility scenario is created on its facility's coordinates, and later completion, capture, or destruction
     * resolves the facility by looking it up from the scenario's coordinates. Relocating the facility and the scenario
     * separately - each to its own free hex - splits that pair, and the battle can no longer act on its facility. So
     * the two must move together, as one occupant of a single hex.</p>
     */
    private static Collection<StratConCoords> occupiedCoords(Collection<StratConCoords> facilityCoords,
          Collection<StratConCoords> scenarioCoords, Collection<StratConCoords> pointOfInterestCoords) {
        Set<StratConCoords> coords = new LinkedHashSet<>(facilityCoords);
        coords.addAll(scenarioCoords);
        coords.addAll(pointOfInterestCoords);
        return coords;
    }

    /**
     * Moves everything sitting on {@code source} - a facility, a scenario, the facility scenario that is both, or a
     * point of interest that occupies its hex - to {@code destination} as one occupant, carrying its strategic
     * objective and any forces deployed to it. This keeps a facility and its co-located scenario on the same hex, which
     * their capture and destruction rules depend on.
     *
     * <p>Points of interest that do not occupy their hex stay where they are: they belong to the ground, not to the
     * occupant.</p>
     */
    private static void relocateOccupant(StratConTrackState track, StratConCoords source, StratConCoords destination) {
        // Move an occupying point of interest first: once a facility or scenario lands on the destination, that hex
        // counts as occupied and the point of interest would be refused.
        StratConPointOfInterest occupyingPointOfInterest = track.getOccupyingPointOfInterest(source);
        if ((occupyingPointOfInterest != null) &&
                  !track.movePointOfInterest(occupyingPointOfInterest.getId(), destination)) {
            LOGGER.warn("Could not relocate point of interest {} to {} on track {}.",
                  occupyingPointOfInterest,
                  destination,
                  track.getDisplayableName());
        }

        StratConFacility facility = track.getFacility(source);
        if (facility != null) {
            track.removeFacility(source);
            track.addFacility(destination, facility);
        }

        StratConScenario scenario = track.getScenario(source);
        if (scenario != null) {
            track.getScenarios().remove(source);
            scenario.setCoords(destination);
            track.getScenarios().put(destination, scenario);
        }

        track.moveObjective(source, destination);
        moveAssignedForces(track, source, destination);
    }

    /**
     * Removes whatever occupies {@code source} - facility, scenario, occupying point of interest, and its strategic
     * objective - when no free hex can receive it.
     */
    private static void dropOccupant(StratConTrackState track, StratConCoords source) {
        track.removeFacility(source);
        track.getScenarios().remove(source);

        StratConPointOfInterest occupyingPointOfInterest = track.getOccupyingPointOfInterest(source);
        if (occupyingPointOfInterest != null) {
            dropPointOfInterest(track, occupyingPointOfInterest);
        }

        removeObjectiveAt(track, source);
    }

    /** Drops any strategic objective tied to a hex whose occupant could not be saved, so nothing points at dead ground. */
    private static void removeObjectiveAt(StratConTrackState track, StratConCoords coords) {
        track.getStrategicObjectives().removeIf(objective -> coords.equals(objective.getObjectiveCoords()));
    }

    /** Moves any forces deployed to {@code source} along with the occupant that just moved to {@code destination}. */
    private static void moveAssignedForces(StratConTrackState track, StratConCoords source,
          StratConCoords destination) {
        Set<Integer> forces = track.getAssignedCoordForces().remove(source);
        if ((forces == null) || forces.isEmpty()) {
            return;
        }

        track.getAssignedCoordForces().computeIfAbsent(destination, key -> new HashSet<>()).addAll(forces);
        for (int forceID : forces) {
            track.getAssignedForceCoords().put(forceID, destination);
        }
    }

    /** Recalls every force still standing outside the sector, so no formation is stranded off the map. */
    private static void recallForcesOutsideBounds(StratConTrackState track) {
        List<Integer> stranded = new ArrayList<>();
        for (Map.Entry<Integer, StratConCoords> entry : track.getAssignedForceCoords().entrySet()) {
            if (track.isOutOfBounds(entry.getValue())) {
                stranded.add(entry.getKey());
            }
        }

        for (int forceID : stranded) {
            track.unassignFormation(forceID);
        }
        track.getAssignedCoordForces().keySet().removeIf(track::isOutOfBounds);
    }

    /**
     * Gives newly-exposed hexes terrain by extending their nearest already-mapped neighbor, so a grown sector reads as
     * more of the same country rather than a blank margin.
     */
    private static void fillNewHexes(StratConTrackState track) {
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (!track.getTerrainTile(coords).isEmpty()) {
                    continue;
                }

                // Walking left-to-right and top-to-bottom means the neighbor we copy has already been filled itself,
                // so terrain propagates outward from the old sector instead of leaving holes.
                String source = (x > 0) ?
                                      track.getTerrainTile(new StratConCoords(x - 1, y)) :
                                      track.getTerrainTile(new StratConCoords(x, max(0, y - 1)));
                track.setTerrainTile(coords, source.isEmpty() ? DEFAULT_FILL_TERRAIN : source);
            }
        }
    }

    private static boolean isOutside(StratConCoords coords, int width, int height) {
        return (coords.getX() >= width) || (coords.getY() >= height);
    }

    private static List<StratConCoords> outsideCoords(Collection<StratConCoords> coords, int width, int height) {
        List<StratConCoords> outside = new ArrayList<>();
        for (StratConCoords candidate : coords) {
            if (isOutside(candidate, width, height)) {
                outside.add(candidate);
            }
        }
        return outside;
    }

    /**
     * Re-settles a track after its terrain has been edited by a GM, so the sector stays internally consistent: cities
     * and occupants cannot be left sitting on new water, open water carries no fog, and the road network has to be
     * re-laid because ocean and relief are what drive its path costs.
     *
     * <p>Call this once when an edit is finished rather than per hex - it rebuilds the whole road network.</p>
     *
     * @param track    the track whose terrain just changed
     * @param contract the contract, for the planet-owner road rules
     * @param campaign the campaign, for the current date and options
     */
    public static void applyTerrainChange(StratConTrackState track, AbstractContract contract, Campaign campaign) {
        // A city that has just been flooded is no longer a city.
        track.getCities().removeIf(coords -> StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords)));

        StratConSectorGenerator.revealOceanHexes(track);
        relocateOccupantsOffOcean(track);
        connectFacilitiesToRoads(track, contract, campaign);
    }

    /**
     * Folds the planet-owner's facilities on the given track into its road network, via the GM's sector-generation
     * strategy (a road-less generator ignores this). Call whenever a GM edits what sits on the map - adding or removing
     * a city, adding or removing a facility - so the network matches the sites that are actually there. Sector
     * generation and regeneration lay the roads the same way.
     *
     * <p>Deliberately <em>not</em> called for anything that happens in play: a facility changing hands, or being
     * destroyed, during scenario resolution. The line is between editing the map and playing on it. A road is built
     * ground, so who holds the base at the end of it does not decide whether the road was ever laid - and because this
     * rebuilds the entire network from scratch, calling it mid-campaign could redraw roads far from the facility that
     * changed.</p>
     *
     * @param track    the track whose road network to refresh
     * @param contract the contract (source of the planet and the employer/enemy factions)
     * @param campaign the campaign, for the current date and options
     */
    public static void connectFacilitiesToRoads(StratConTrackState track, AbstractContract contract,
          Campaign campaign) {
        StratConGMs.sectorGeneration(campaign.getCampaignOptions())
              .connectFacilitiesToRoads(track,
                    planetOwnedFacilityCoords(track, contract, campaign.getLocalDate()));
    }

    /**
     * Returns the hexes of facilities whose owning side controls the contract's planet. Allied (and player) facilities
     * count when the employer holds the planet; opposing facilities count when the enemy holds it. On a contested world
     * both sides can qualify; on a world held by neither contract party, none do.
     *
     * @param track    the track whose facilities to examine
     * @param contract the contract (source of the planet and the employer/enemy factions)
     * @param date     the date at which to evaluate planetary ownership
     *
     * @return the coordinates of the qualifying facilities (possibly empty)
     */
    private static Set<StratConCoords> planetOwnedFacilityCoords(StratConTrackState track, AbstractContract contract,
          LocalDate date) {
        Set<StratConCoords> result = new HashSet<>();

        Planet planet = contract.getTargetPlanet();
        if (planet == null) {
            return result;
        }

        Set<Faction> owners = planet.getFactionSet(date);
        boolean employerOwns = owners.contains(contract.getEmployerFaction());
        boolean enemyOwns = owners.contains(contract.getEnemyFaction());

        if (!employerOwns && !enemyOwns) {
            return result;
        }

        for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
            ForceAlignment owner = entry.getValue().getOwner();
            boolean planetOwned = (enemyOwns && (owner == ForceAlignment.Opposing)) ||
                                        (employerOwns &&
                                               ((owner == ForceAlignment.Allied) || (owner == ForceAlignment.Player)));
            if (planetOwned) {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    /**
     * Moves any facility, scenario, or occupying point of interest that a regeneration left sitting on an ocean hex to
     * a fresh, non-ocean unoccupied hex, carrying any strategic-objective marker along with it. Land-only points of
     * interest that do not occupy their hex are moved ashore the same way; any others stay put, since they can sit on
     * water. Everything is left in place if the sector has no free land to receive it.
     *
     * @param track the freshly regenerated track to clean up
     */
    private static void relocateOccupantsOffOcean(StratConTrackState track) {
        List<StratConCoords> floodedFacilities = floodedCoords(track, track.getFacilities().keySet());
        List<StratConCoords> floodedScenarios = floodedCoords(track, track.getScenarios().keySet());
        List<StratConCoords> floodedPointsOfInterest = floodedCoords(track, occupyingPointOfInterestCoords(track));

        for (StratConCoords source : occupiedCoords(floodedFacilities, floodedScenarios, floodedPointsOfInterest)) {
            StratConCoords destination = findRelocationCoords(track, source);
            if (destination == null) {
                // No dry land left to receive it; leave the occupant where it is rather than destroy it.
                break;
            }

            relocateOccupant(track, source, destination);
        }

        relocateLandOnlyPointsOfInterestOffOcean(track);

        // A force standing on ground that just flooded, with no facility or scenario to carry it ashore, is recalled.
        recallForcesOnOcean(track);
    }

    /**
     * Moves every land-only point of interest that does not occupy its hex, and that a terrain change has left on
     * ocean, to dry land: a hex its type's placement rules allow if there is one, else any free dry hex. Left in place
     * if the sector has no dry land to receive it.
     */
    private static void relocateLandOnlyPointsOfInterestOffOcean(StratConTrackState track) {
        List<StratConPointOfInterest> floodedPointsOfInterest = new ArrayList<>();
        for (StratConPointOfInterest pointOfInterest : track.getPointsOfInterest()) {
            StratConCoords coords = pointOfInterest.getCoords();
            if (!pointOfInterest.occupiesHex() &&
                      (coords != null) &&
                      isLandOnly(pointOfInterest) &&
                      StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords))) {
                floodedPointsOfInterest.add(pointOfInterest);
            }
        }

        for (StratConPointOfInterest pointOfInterest : floodedPointsOfInterest) {
            // Prefer a hex its type's placement rules allow; it needs no free hex of its own, only dry land.
            StratConCoords destination = StratConPointOfInterestPlacer.findPlacementCoords(track,
                  pointOfInterest.getDefinition());
            if (destination == null) {
                destination = getUnoccupiedCoords(track);
            }

            if (destination == null) {
                // No dry land left to receive it; leave it where it is rather than destroy it.
                continue;
            }

            track.movePointOfInterest(pointOfInterest.getId(), destination);
        }
    }

    /** @return those of the given occupant hexes that a terrain change has left sitting on ocean. */
    private static List<StratConCoords> floodedCoords(StratConTrackState track, Collection<StratConCoords> coords) {
        List<StratConCoords> flooded = new ArrayList<>();
        for (StratConCoords candidate : coords) {
            if (StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(candidate))) {
                flooded.add(candidate);
            }
        }
        return flooded;
    }

    /**
     * Recalls every force left standing on water, so no formation is stranded at sea after a hex floods.
     *
     * <p>The ocean counterpart to {@link #recallForcesOutsideBounds}: a regeneration or a GM painting {@code Sea} over
     * an occupied hex can put a deployed force in water just as a shrink can put one outside the map.</p>
     */
    private static void recallForcesOnOcean(StratConTrackState track) {
        List<Integer> stranded = new ArrayList<>();
        for (Map.Entry<Integer, StratConCoords> entry : track.getAssignedForceCoords().entrySet()) {
            if (StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(entry.getValue()))) {
                stranded.add(entry.getKey());
            }
        }

        for (int forceID : stranded) {
            track.unassignFormation(forceID);
        }
        track.getAssignedCoordForces()
              .keySet()
              .removeIf(coords -> StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords)));
    }

    /**
     * Applies the legacy track dimensions: a total of {@code formations * 28} hexes laid out as a rectangle that is
     * wider than it is tall, so a scout formation deployed to a fresh spot each week can more or less cover it.
     *
     * <p>The area is capped at {@link #MAX_SECTOR_HEXES}, matching the improved path. Nothing reaches that today,
     * because the legacy count hands every sector at most three formations and so at most 84 hexes - the cap is here so
     * that a future count method built on legacy sizing cannot produce an unbounded map. No trim loop is needed after
     * it: {@code width} is the floored quotient of the capped total, so the laid-out area cannot exceed it.</p>
     */
    private static void applyLegacyDimensions(StratConTrackState track, int numFormations) {
        int numHexes = min(MAX_SECTOR_HEXES, numFormations * LEGACY_HEXES_PER_FORMATION);
        int height = max(1, (int) Math.floor(Math.sqrt(numHexes)));
        int width = numHexes / height;
        track.setWidth(width);
        track.setHeight(height);
    }

    /**
     * Applies the improved track dimensions. Starts from a quarter's scouting budget for the sector's combat teams,
     * scaled by planetary size and the configured size multiplier to get the dry playable target, then grows the sector
     * to offset ocean so the dry target survives. The area is laid out by {@link #rollSectorShape}, which rolls a
     * weighted aspect ratio - sectors are deliberately not square.
     */
    private static void applyImprovedDimensions(StratConTrackState track, SectorSpec sector, PlanetProfile profile,
          double sizeMultiplier) {
        // The ceiling is a backstop rather than a working limit: the planner has already split the contract into enough
        // sectors that none of them should ask for more than this. It stays because rounding and the shape roll can
        // land a hex or two over.
        int totalHexes = min(MAX_SECTOR_HEXES, requestedHexes(sector.requiredLances(), profile, sizeMultiplier));

        SectorDimensions shape = rollSectorShape(totalHexes);
        track.setWidth(shape.width());
        track.setHeight(shape.height());
    }

    /**
     * @return the total hexes a sector fronting the given combat teams asks for on this planet, before the area ceiling
     *       is applied.
     *
     *       <p>Size the sector to what its own recon can cover in a quarter: a third of the combat teams fronting it
     *       are assumed to be recon, and each covers {@link #RECON_HEXES_PER_QUARTER} hexes of dry ground in three
     *       months. The recon count is deliberately NOT rounded - a sector fronting four teams gets 1.33 recon teams'
     *       worth of ground, not one team's. Rounding down undersized every sector whose team count was not a multiple
     *       of three. It is floored at one, because even the smallest sector is scouted by someone. The dry total is
     *       then grown by the planet's land fraction, since ocean is not playable.</p>
     */
    static int requestedHexes(int combatTeams, PlanetProfile profile, double sizeMultiplier) {
        double reconTeams = max(1.0, combatTeams / (double) COMBAT_TEAMS_PER_RECON_FORCE);
        double landHexes = reconTeams * RECON_HEXES_PER_QUARTER * profile.sizeFactor() * sizeMultiplier;
        int playableHexes = max(1, (int) Math.round(landHexes));

        double landFraction = max(MINIMUM_LAND_FRACTION, 1.0 - (profile.waterPercent() / 100.0));

        return (int) Math.round(playableHexes / landFraction);
    }

    /**
     * @return the most combat teams a single sector on this planet can front without asking for more ground than
     *       {@link #MAX_SECTOR_HEXES} will grant.
     *
     *       <p>Used by the planner to split a contract into enough sectors that none of them is clipped by the
     *       ceiling. The threshold is planetary, not fixed: a dry, small world fits far more teams into one sector than
     *       a large ocean world does, so this cannot be a constant. Searched rather than solved because the sizing rule
     *       rounds twice and floors the recon share, which an inverted formula would have to reproduce exactly to stay
     *       in step with it.</p>
     */
    public static int maximumTeamsPerSector(PlanetProfile profile, double sizeMultiplier) {
        int teams = 1;
        while ((teams < MAXIMUM_TEAMS_PER_SECTOR_SEARCH_LIMIT) &&
                     (requestedHexes(teams + 1, profile, sizeMultiplier) <= MAX_SECTOR_HEXES)) {
            teams++;
        }

        return teams;
    }

    /** A sector's laid-out proportions. */
    private record SectorDimensions(int width, int height) {}

    /**
     * Lays a sector's area out into width and height, rolling a fresh {@link SectorShapeProfile#SectorShapeProfile} for
     * the proportions.
     *
     * @param totalHexes the area to lay out
     *
     * @return the resulting dimensions, both inside the playable bounds and together inside the area ceiling
     */
    private static SectorDimensions rollSectorShape(int totalHexes) {
        // Both dimensions are derived from the ratio rather than one from the other, so a 1.0 ratio really is square
        // instead of drifting a hex wide.
        double aspectRatio = StratConSectorShape.getInstance().selectProfile().aspectRatioOrDefault();
        int width = clampDimension((int) Math.round(Math.sqrt(totalHexes * aspectRatio)));
        int height = clampDimension((int) Math.round(Math.sqrt(totalHexes / aspectRatio)));

        // Rounding each dimension independently can nudge the laid-out area just past the ceiling, so trim the longer
        // side until it fits. The ceiling is meant to be a hard bound, not an approximate one.
        while (((width * height) > MAX_SECTOR_HEXES) && (max(width, height) > MIN_SECTOR_DIMENSION)) {
            if (width >= height) {
                width--;
            } else {
                height--;
            }
        }

        return new SectorDimensions(width, height);
    }

    /**
     * Re-rolls a sector's proportions when it is regenerated, so a regenerated sector is not stuck with the shape it
     * happened to be given when the contract was signed. Its <em>area</em> is preserved - regeneration re-rolls the
     * terrain and climate, not how much sector there is to fight over.
     *
     * <p>If the new proportions would leave more facilities and scenarios outside the sector than it can re-home, the
     * current shape is kept. A cosmetic re-roll is not worth destroying anything over.</p>
     */
    private static void reshapeForRegeneration(StratConTrackState track) {
        SectorDimensions shape = rollSectorShape(track.getWidth() * track.getHeight());
        if ((shape.width() == track.getWidth()) && (shape.height() == track.getHeight())) {
            return;
        }

        if (!previewResize(track, shape.width(), shape.height()).fits()) {
            return;
        }

        applyNewBounds(track, shape.width(), shape.height());
    }

    /** Keeps a sector dimension inside the playable range, so no shape can produce a sliver or a runaway map. */
    private static int clampDimension(int dimension) {
        return Math.clamp(dimension, MIN_SECTOR_DIMENSION, MAX_SECTOR_DIMENSION);
    }

    /**
     * Improved temperature: the planet's equatorial temperature, shifted colder by the sector's latitude band, plus a
     * small local variation of -5 to +5 degrees.
     */
    private static int improvedTemperature(PlanetProfile profile, LatitudeBand latitudeBand) {
        int localVariation = Compute.randomInt(11) - 5;
        return profile.temperatureCelsius() + latitudeBand.getTemperatureOffset() + localVariation;
    }

    /**
     * Legacy temperature: the equatorial temperature with a random -40 to +10 degree swing, on the notion that the
     * equator is about as hot as it gets, with some exceptions.
     */
    private static int legacyTemperature(int equatorialTemperature) {
        int tempVariation = Compute.randomInt(51) - 40;
        return equatorialTemperature + tempVariation;
    }

    /**
     * Generates an array list representing the number of objects to place in a given number of tracks.
     */
    private static List<Integer> trackObjectDistribution(int numObjects, int numTracks) {
        // This ensures we're not at risk of dividing by 0
        numTracks = max(1, numTracks);

        List<Integer> retVal = new ArrayList<>();
        int leftOver = numObjects % numTracks;

        for (int track = 0; track < numTracks; track++) {
            int trackObjects = numObjects / numTracks;

            // if we are unevenly distributed, add an extra one
            if (leftOver > 0) {
                trackObjects++;
                leftOver--;
            }

            retVal.add(trackObjects);
        }

        // don't always front-load extra objects
        Collections.shuffle(retVal);
        return retVal;
    }

    /**
     * Worker function that takes a track state and plops down the given number of facilities owned by the given faction
     * Avoids places with existing facilities and scenarios, capable of taking facility sub set and setting strategic
     * objective flag.
     */
    // Package-private rather than private so the capacity rules can be tested directly; reaching them through contract
    // initialization would mean standing up a whole contract to assert on a placement loop.
    static void initializeTrackFacilities(StratConTrackState trackState, int numFacilities, ForceAlignment owner,
          boolean strategicObjective, List<String> modifiers) {

        int capacity = facilityCapacity(trackState);
        int placed = 0;

        for (int fCount = 0; fCount < numFacilities; fCount++) {
            // Stop deliberately at capacity rather than running on until placement happens to fail.
            if (trackState.getOccupiedHexCount() >= capacity) {
                break;
            }

            StratConFacility sf = owner == ForceAlignment.Allied ?
                                        StratConFacilityFactory.getRandomAlliedFacility() :
                                        StratConFacilityFactory.getRandomHostileFacility();

            sf.setOwner(owner);
            sf.setStrategicObjective(strategicObjective);
            sf.getLocalModifiers().addAll(modifiers);

            StratConCoords coords = getUnoccupiedCoords(trackState);

            if (coords == null) {
                break;
            }

            placed++;
            trackState.addFacility(coords, sf);

            if (strategicObjective) {
                StratConStrategicObjective sso = new StratConStrategicObjective();
                sso.setObjectiveCoords(coords);

                if (sf.getOwner() == ForceAlignment.Allied) {
                    trackState.getRevealedCoords().add(coords);
                    sf.setVisible(true);
                    sso.setObjectiveType(StrategicObjectiveType.AlliedFacilityControl);
                } else {
                    sf.setVisible(false);
                    sso.setObjectiveType(StrategicObjectiveType.HostileFacilityControl);
                }

                trackState.addStrategicObjective(sso);
            }
        }

        if (placed < numFacilities) {
            LOGGER.info("Sector {} had room for {} of {} {} facilities. The contract asks for facilities in " +
                              "proportion to its combat teams, but a sector's area is capped, so a large contract in " +
                              "few sectors can want more than its ground will hold.",
                  trackState.getDisplayableName(),
                  placed,
                  numFacilities,
                  owner);
        }
    }

    /**
     * @return how many facilities a sector will accept, being a fraction of the hexes that can actually hold one.
     *
     *       <p>Two things make this narrower than it looks. Only dry land counts - {@link #getUnoccupiedCoords} never
     *       returns an ocean hex - so a wet sector holds far fewer facilities than its width times its height would
     *       suggest. And only part of that land is offered, because scenarios need somewhere to spawn for the life of
     *       the contract; a sector paved with facilities has nowhere left to fight.</p>
     */
    private static int facilityCapacity(StratConTrackState trackState) {
        int placeable = 0;
        for (int x = 0; x < trackState.getWidth(); x++) {
            for (int y = 0; y < trackState.getHeight(); y++) {
                if (!StratConBiomeManifest.isOceanTerrain(trackState.getTerrainTile(new StratConCoords(x, y)))) {
                    placeable++;
                }
            }
        }

        return max(1, (int) Math.round(placeable * MAXIMUM_FACILITY_COVERAGE));
    }

    /**
     * Spawns a batch of specific-scenario strategic objectives partway through a contract, distributing them across its
     * StratCon tracks.
     *
     * <p>Specific-scenario objectives are not all placed at contract start; they appear over the contract's months,
     * paced by its scenario schedule ({@link mekhq.campaign.mission.contract.contractData.ContractIntensityData}). The
     * daily StratCon lifecycle calls this once per contract month with that month's scheduled count. The scenarios are
     * drawn from the contract definition's {@link StrategicObjectiveType#SpecificScenarioVictory} template pool - the
     * same pool the up-front placement used - and are placed cloaked and dateless, exactly as before, so the existing
     * reveal-on-scouting flow is unchanged.</p>
     *
     * <p>Does nothing in mapless mode (there is no map to place on), when the contract has no campaign state, or when
     * the contract definition has no specific-scenario objective.</p>
     *
     * @param campaign      the campaign managing overall gameplay
     * @param contract      the contract whose strategic scenarios are being spawned
     * @param scenarioCount how many strategic scenarios to spawn this batch (skipped when not positive)
     */
    public static void spawnScheduledStrategicScenarios(Campaign campaign, AbstractContract contract,
          int scenarioCount) {
        if (scenarioCount <= 0) {
            return;
        }

        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if (campaignState == null) {
            return;
        }

        // Objective scenarios are placed on a map; mapless play never placed them, so it spawns none here either.
        if (campaign.getCampaignOptions().isUseStratConMaplessMode()) {
            return;
        }

        StratConContractDefinition definition = StratConContractDefinition.getContractDefinition(
              contract.getObjectiveType());
        if (definition == null) {
            return;
        }

        // Every shipped contract definition carries at most one specific-scenario objective; use its template pool.
        ObjectiveParameters specificObjective = null;
        for (ObjectiveParameters objectiveParams : definition.getObjectiveParameters()) {
            if (objectiveParams.getObjectiveType() == StrategicObjectiveType.SpecificScenarioVictory) {
                specificObjective = objectiveParams;
                break;
            }
        }
        if (specificObjective == null) {
            return;
        }

        List<Integer> trackObjects = trackObjectDistribution(scenarioCount, campaignState.getTrackCount());
        for (int x = 0; x < trackObjects.size(); x++) {
            initializeObjectiveScenarios(campaign,
                  contract,
                  campaignState.getTrack(x),
                  trackObjects.get(x),
                  specificObjective.getObjectiveScenarios(),
                  specificObjective.getObjectiveScenarioModifiers());
        }
    }

    /**
     * Initializes and populates a StratCon track with a specified number of objective scenarios. This method selects
     * scenario templates, places them on the track in unoccupied coordinates, and optionally assigns facilities and
     * objectives based on predefined rules.
     *
     * <p>The key steps of this method include:
     * <ul>
     *   <li>Selecting scenario templates from the provided list of objective scenarios.</li>
     *   <li>Identifying unoccupied coordinates on the track to place each scenario.</li>
     *   <li>Adding facilities if the scenario template requires them (hostile or allied).</li>
     *   <li>Generating and configuring scenarios with relevant attributes and modifiers:</li>
     *   <ul>
     *     <li>Marking scenarios as strategic objectives.</li>
     *     <li>Adding optional modifiers to provide additional effects or conditions.</li>
     *   </ul>
     *   <li>Tracking newly added scenarios as strategic objectives for gameplay purposes.</li>
     * </ul>
     *
     * <p>The scenarios keep the deployment dates {@code generateScenario} assigns and are left uncloaked, so each is
     * ready to fight like a normal scenario as soon as it is placed - the player does not have to scout it out first.
     * They are placed on their scheduled day by {@link #spawnScheduledStrategicScenarios}.</p>
     *
     * @param campaign           the {@link Campaign} managing the state of the overall gameplay
     * @param contract           the {@link AbstractContract} related to the current StratCon campaign
     * @param trackState         the {@link StratConTrackState} representing the track where objectives are placed
     * @param numScenarios       the number of objective scenarios to generate
     * @param objectiveScenarios a list of {@link String} identifiers for potential scenarios that can be generated
     * @param objectiveModifiers a list of optional {@link String} modifiers to apply to the generated scenarios; can be
     *                           {@code null} if no modifiers are required
     */
    private static void initializeObjectiveScenarios(Campaign campaign, AbstractContract contract,
          StratConTrackState trackState, int numScenarios, List<String> objectiveScenarios,
          List<String> objectiveModifiers) {
        // pick scenario from subset
        // place it on the map somewhere nothing else has been placed yet
        // if it's a facility scenario, place the facility
        // run generateScenario() to apply all the necessary mods
        // apply objective mods (?)

        int trackSize = trackState.getWidth() * trackState.getHeight();

        for (int sCount = 0; sCount < numScenarios; sCount++) {
            // if there's no possible empty places to put down a new scenario, then move on
            if (trackState.getOccupiedHexCount() >= trackSize) {
                break;
            }

            // pick
            ScenarioTemplate template = StratConScenarioFactory.getSpecificScenario(objectiveScenarios.get(Compute.randomInt(
                  objectiveScenarios.size())));

            if (template == null) {
                LOGGER.error("Unable to place objective scenario on track {}, as no scenario template was available.",
                      trackState.getDisplayableName());
                continue;
            }

            StratConCoords coords = getUnoccupiedCoords(trackState);

            if (coords == null) {
                LOGGER.error("Unable to place objective scenario on track {}, as all coords were occupied. Aborting.",
                      trackState.getDisplayableName());
                return;
            }

            // facility
            boolean addedFacility = false;
            if (template.isFacilityScenario()) {
                StratConFacility facility = template.isHostileFacility() ?
                                                  StratConFacilityFactory.getRandomHostileFacility() :
                                                  StratConFacilityFactory.getRandomAlliedFacility();
                trackState.addFacility(coords, facility);
                addedFacility = true;
            }

            // create scenario - don't assign a force yet
            StratConScenario scenario = StratConRulesManager.generateScenario(campaign,
                  contract,
                  trackState,
                  Formation.FORMATION_NONE,
                  coords,
                  template,
                  null);

            if (scenario != null) {
                // These appear over the contract's run on their scheduled day and are meant to be fought like any
                // normal scenario: keep the deployment dates generateScenario set and leave them uncloaked, so the
                // player can engage immediately rather than having to scout them out first.
                scenario.setStrategicObjective(true);
                scenario.setTurningPoint(false);
                // apply objective mods
                if (objectiveModifiers != null) {
                    for (String modifier : objectiveModifiers) {
                        scenario.getBackingScenario()
                              .addScenarioModifier(AtBScenarioModifier.getScenarioModifier(modifier));
                    }
                }

                trackState.addScenario(scenario);

                // Reveal the hex (and any facility placed with the scenario) so its location is known from the start,
                // as befits a scenario that is already visible and fightable: the objectives list then shows its
                // coordinates instead of the "Locate and..." wording used for hexes still to be scouted.
                trackState.getRevealedCoords().add(coords);
                if (addedFacility) {
                    StratConFacility placedFacility = trackState.getFacility(coords);
                    if (placedFacility != null) {
                        placedFacility.setVisible(true);
                    }
                }

                StratConStrategicObjective sso = new StratConStrategicObjective();
                sso.setObjectiveCoords(coords);
                sso.setObjectiveType(StrategicObjectiveType.SpecificScenarioVictory);
                sso.setDesiredObjectiveCount(1);
                trackState.addStrategicObjective(sso);
            } else if (addedFacility) {
                trackState.removeFacility(coords);
            }
        }
    }

    /**
     * Searches for a random, unoccupied coordinate on the specified {@link StratConTrackState}.
     *
     * <p>This method provides a basic, simplified call to search for an unoccupied coordinate
     * with default settings: hexes containing player facilities and forces are not considered eligible targets, and
     * strategic targets are not emphasized.</p>
     *
     * <p>Delegates to {@link #getUnoccupiedCoords(StratConTrackState, boolean, boolean, boolean)}
     * with default values.</p>
     *
     * @param trackState the {@link StratConTrackState} on which to search for unoccupied coordinates
     *
     * @return a {@link StratConCoords} object representing a suitable, unoccupied location, or {@code null} if no such
     *       location is available
     */
    public static @Nullable StratConCoords getUnoccupiedCoords(StratConTrackState trackState) {
        return getUnoccupiedCoords(trackState, false, false, false);
    }

    /**
     * Searches for a suitable, random unoccupied coordinate on the specified {@link StratConTrackState}, applying
     * optional rules regarding player facilities, player forces, and strategic weighting.
     *
     * <p>A coordinate is considered suitable when all the following are true:</p>
     * <ul>
     *     <li>There is <b>no active scenario</b> at that coordinate.</li>
     *     <li>There is <b>no point of interest that occupies</b> that coordinate.</li>
     *     <li>The coordinate does <b>not</b> contain player-assigned forces, unless {@code
     *     allowPlayerForces} is {@code true}.</li>
     *     <li>The coordinate either contains no facility, or contains an <b>allied facility</b> and
     *     {@code allowPlayerFacilities} is {@code true}.</li>
     * </ul>
     *
     * <p>Suitable coordinates are added into a weighted pool:</p>
     * <ul>
     *     <li>Empty coordinates (no facility, no forces) receive a default weight of {@code 1}.</li>
     *     <li>Allowed allied facilities receive a higher weight when {@code emphasizeStrategicTargets} is
     *     {@code true}.</li>
     *     <li>Allowed player-force coordinates also receive the strategic weight when
     *     {@code emphasizeStrategicTargets} is {@code true}.</li>
     * </ul>
     *
     * <p>The strategic emphasis weight is equal to {@code max(trackWidth, trackHeight)} when
     * {@code emphasizeStrategicTargets} is enabled, and {@code 1} otherwise.</p>
     *
     * <p>The method returns one randomly weighted coordinate, or {@code null} if no valid coordinates are
     * available.</p>
     *
     * @param trackState                the {@link StratConTrackState} to search for unoccupied coordinates
     * @param allowPlayerFacilities     whether allied (player-owned or allied-owned) facilities are considered valid
     * @param allowPlayerForces         whether coordinates containing player-assigned forces are valid; if
     *                                  {@code false}, such coordinates are excluded entirely
     * @param emphasizeStrategicTargets whether to apply increased weighting to strategic locations (allowed allied
     *                                  facilities and allowed player-force coordinates)
     *
     * @return a randomly weighted, valid {@link StratConCoords}, or {@code null} if none exist
     */
    public static @Nullable StratConCoords getUnoccupiedCoords(StratConTrackState trackState,
          boolean allowPlayerFacilities, boolean allowPlayerForces, boolean emphasizeStrategicTargets) {
        final int trackHeight = trackState.getHeight();
        final int trackWidth = trackState.getWidth();

        int defaultWeight = 1;
        int strategicEmphasis = emphasizeStrategicTargets ? max(trackHeight, trackWidth) : defaultWeight;

        Collection<StratConCoords> forceCoords = trackState.getAssignedForceCoords().values();
        WeightedIntMap<StratConCoords> weightedMap = new WeightedIntMap<>();
        for (int y = 0; y < trackHeight; y++) {
            for (int x = 0; x < trackWidth; x++) {
                StratConCoords coords = new StratConCoords(x, y);
                // Ocean hexes never host scenarios or facilities.
                if (StratConBiomeManifest.isOceanTerrain(trackState.getTerrainTile(coords))) {
                    continue;
                }

                if (trackState.getScenario(coords) != null) {
                    continue;
                }

                // A point of interest that occupies its hex keeps scenarios and facilities off it.
                if (trackState.getOccupyingPointOfInterest(coords) != null) {
                    continue;
                }

                if (forceCoords.contains(coords)) {
                    if (allowPlayerForces) {
                        weightedMap.add(strategicEmphasis, coords);
                    }
                    continue;
                }

                StratConFacility facility = trackState.getFacility(coords);
                if (facility == null) {
                    weightedMap.add(defaultWeight, coords);
                } else if (allowPlayerFacilities && facility.isOwnerAlliedToPlayer()) {
                    weightedMap.add(strategicEmphasis, coords);
                }
            }
        }

        return weightedMap.randomItem();
    }

    /**
     * Given a mission (that's an AtB contract), restore track state information, such as pointers from StratCon
     * scenario objects to AtB scenario objects.
     */
    public static void restoreTransientStratconInformation(AbstractContract mission, Campaign campaign) {
        // Having loaded scenarios and such, we now need to go through any StratCon
        // scenarios for this contract
        // and set their backing scenario pointers to the existing scenarios stored in
        // the campaign for this contract
        StratConCampaignState campaignState = mission.getStratConCampaignState();
        if (campaignState != null) {
            // The campaign state's contract is an @XmlTransient back-pointer, so it comes back null from the save.
            // Restore it before anything reads it: deploying a force to a scenario goes through
            // StratConCampaignState.getContract(), which would otherwise hand a null contract to scenario generation.
            campaignState.setContract(mission);

            for (StratConTrackState track : campaignState.getTracks()) {
                for (StratConScenario scenario : track.getScenarios().values()) {
                    Scenario campaignScenario = campaign.getScenario(scenario.getBackingScenarioID());

                    if ((campaignScenario instanceof AtBDynamicScenario)) {
                        scenario.setBackingScenario((AtBDynamicScenario) campaignScenario);
                    } else {
                        LOGGER.warn("Unable to set backing scenario for StratCon scenario in track {} ID {}",
                              track.getDisplayableName(),
                              scenario.getBackingScenarioID());
                    }
                }
            }
        }
    }
}
