/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.scenarios.salvage;

import static java.lang.Math.max;
import static megamek.common.compute.Compute.d6;
import static megamek.common.equipment.MiscType.F_NAVAL_TUG_ADAPTOR;
import static megamek.common.units.Crew.DEATH;
import static mekhq.campaign.enums.DailyReportType.FINANCES;
import static mekhq.campaign.enums.DailyReportType.MEDICAL;
import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.bays.ASFBay;
import megamek.common.bays.Bay;
import megamek.common.bays.SmallCraftBay;
import megamek.common.equipment.Mounted;
import megamek.common.icons.Camouflage;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.utilities.ContractRepairLocation;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.InjurySPAUtility;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.enums.TransporterType;

public class CamOpsSalvageUtilities {
    private static final MMLogger LOGGER = MMLogger.create(CamOpsSalvageUtilities.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CamOpsSalvage";

    /**
     * Checks whether an entity is able to drag or tow salvage during ground salvage operations.
     *
     * <p>'Meks with two working hands and non-trailer vehicles can tow. Trailers cannot, as they must themselves be
     * towed.</p>
     *
     * @param entity the entity to check
     *
     * @return {@code true} if the entity can tow salvage
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isTowCapable(@Nullable Entity entity) {
        // A 'Mek needs two working hands to drag salvage
        if (entity instanceof Mek mek) {
            return mek.canPerformGroundSalvageOperations();
        }

        return entity instanceof Tank tank && !tank.isTrailer();
    }

    /**
     * Calculates how much salvage a unit can tow during ground salvage operations.
     *
     * <p>The tow capacity is the unit's own weight, less the weight of any trailers it is already towing. Units that
     * are not tow capable (see {@link #isTowCapable(Entity)}) have no tow capacity.</p>
     *
     * @param unit the unit to check
     *
     * @return the available tow capacity, in tons
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getTowCapacity(Unit unit) {
        Entity entity = unit.getEntity();
        if (!isTowCapable(entity)) {
            return 0.0;
        }

        double currentTowWeight = unit.getTotalWeightOfUnitsAssignedToBeTransported(
              CampaignTransportType.TOW_TRANSPORT,
              TransporterType.TANK_TRAILER_HITCH);
        return max(0.0, entity.getWeight() - currentTowWeight);
    }

    /**
     * Determines whether a tech's skill level should be taken from their secondary role.
     *
     * <p>The secondary role is only used when the primary role is not a tech role, but the secondary role is.</p>
     *
     * @param tech the tech to check
     *
     * @return {@code true} if the secondary role's skill level should be used
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isUseSecondaryTechSkill(Person tech) {
        return !tech.getPrimaryRole().isTech() && tech.getSecondaryRole().isTechSecondary();
    }

    public static boolean hasNavalTug(Entity entity) {
        for (Mounted<?> mounted : entity.getMisc()) {
            if (mounted.getType().hasFlag(F_NAVAL_TUG_ADAPTOR)) {
                // isOperable doesn't check if the mounted location still exists, so we check for that first.
                if (!mounted.getEntity().isLocationBad(mounted.getLocation()) && (mounted.isOperable())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Counts an entity's bays of a given type that still have at least one working door.
     *
     * @param entity  the entity to check
     * @param bayType the type of bay to count, such as {@link ASFBay} or {@link SmallCraftBay}
     *
     * @return the number of matching bays with working doors
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int countBaysWithWorkingDoors(Entity entity, Class<? extends Bay> bayType) {
        int bayCount = 0;
        for (Bay bay : entity.getTransportBays()) {
            if (bayType.isInstance(bay) && (bay.getCurrentDoors() > 0)) {
                bayCount++;
            }
        }
        return bayCount;
    }

    /**
     * Processes and finalizes salvage after player selection.
     *
     * <p>This method handles the actual processing of salvage units, including:</p>
     * <ul>
     *   <li>Adding claimed salvage units to the campaign</li>
     *   <li>Processing sold salvage units and crediting the account</li>
     *   <li>Charging the player for any salvage they must buy, and paying them any cash share of the employer's
     *   salvage, as the settlement dictates (see {@link SalvageSettlement})</li>
     *   <li>Updating contract salvage tracking</li>
     *   <li>Setting repair locations for salvaged units</li>
     * </ul>
     *
     * @param campaign        The current {@link Campaign} to add salvage to.
     * @param mission         The {@link AbstractContract} associated with the salvage.
     * @param scenario        The {@link Scenario} that generated the salvage.
     * @param settlement      How the salvage is divided between the player and their employer.
     * @param keptSalvage     The list of units claimed (or, under salvage purchases, bought) by the player.
     * @param soldSalvage     The list of units that were sold instead of claimed.
     * @param employerSalvage The list of units going to the employer or unclaimed.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void resolveSalvage(Campaign campaign, AbstractContract mission, Scenario scenario,
          SalvageSettlement settlement, List<TestUnit> keptSalvage, List<TestUnit> soldSalvage,
          List<TestUnit> employerSalvage) {
        int deliveryTime = getSalvageDeliveryTime(campaign, scenario, mission);
        Money keptSalvageValue = Money.zero();
        boolean isKeepEnemyCamouflage = campaign.getCampaignOptions()
                                              .get(CampaignOption.IS_KEEP_ENEMY_CAMOUFLAGE_ON_SALVAGE);

        // now let's take care of salvage
        for (TestUnit salvageUnit : keptSalvage) {
            // Adding the unit to the campaign resets its camouflage to our own, so grab it first
            Camouflage fieldedCamouflage = salvageUnit.getEntity().getCamouflage().clone();

            if (salvageUnit.getEntity() instanceof Aero aero) {
                // Refuel from a fresh copy of the unit. This is only needed for aerospace units, and building the
                // status parses the unit's file, so it isn't done for anything else.
                ResolveScenarioTracker.UnitStatus salvageStatus = new ResolveScenarioTracker.UnitStatus(salvageUnit);
                if (salvageStatus.getBaseEntity() instanceof Aero baseAero) {
                    aero.setFuelTonnage(baseAero.getFuelTonnage());
                }
            }

            campaign.clearGameData(salvageUnit.getEntity());
            // The campaign keeps a new unit wrapped around the salvage's entity, so that's the unit to update
            Unit salvagedUnit = campaign.addTestUnit(salvageUnit, deliveryTime);
            if (isKeepEnemyCamouflage && !fieldedCamouflage.hasDefaultCategory()) {
                salvagedUnit.getEntity().setCamouflage(fieldedCamouflage);
            }
            // Units still in transit are given the best repair site when they arrive (see LocationNewDayUtil)
            if (salvagedUnit.isPresent()) {
                salvagedUnit.setSite(ContractRepairLocation.getRepairLocation(mission.getObjectiveType()));
            }

            // if this is a contract, add to the salvaged value
            mission.changeSalvagedByUnitValue(salvageUnit.getSellValue());
            keptSalvageValue = keptSalvageValue.plus(salvageUnit.getSellValue());
        }

        settlement.chargePurchases(campaign, scenario, keptSalvageValue);

        // And any ransomed salvaged units
        if (!soldSalvage.isEmpty()) {
            Money unitRansoms = Money.zero();
            for (TestUnit ransomedUnit : soldSalvage) {
                unitRansoms = unitRansoms.plus(ransomedUnit.getSellValue());
            }

            if (unitRansoms.isPositive()) {
                campaign.getPlayerForce().getFinances()
                      .credit(TransactionType.SALVAGE,
                            campaign.getLocalDate(),
                            unitRansoms,
                            getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.unitSale", scenario.getName()));
                campaign.addReport(FINANCES, getFormattedTextAt(RESOURCE_BUNDLE,
                      "CamOpsSalvageUtilities.unitSale.report",
                      unitRansoms.toAmountString(), scenario.getHyperlinkedName()));

                // if this is a contract, add to the salvaged value
                mission.changeSalvagedByUnitValue(unitRansoms);
            }
        }

        Money employerTakeHome = Money.zero();
        for (TestUnit salvageUnit : employerSalvage) {
            employerTakeHome = employerTakeHome.plus(salvageUnit.getSellValue());
        }

        // Depending on the settlement, the player may be paid a share of the employer's salvage
        Money playerTakeHome = settlement.payCashShare(campaign, scenario, employerTakeHome);
        employerTakeHome = employerTakeHome.minus(playerTakeHome);
        mission.changeSalvagedByUnitValue(playerTakeHome);

        mission.changeSalvagedByEmployerValue(employerTakeHome);
    }

    /**
     * Gets how many days until salvage from a StratCon scenario reaches the hangar.
     *
     * <p>Salvage travels back with the salvage teams, so it arrives when the last salvage formation assigned to the
     * scenario returns. If no salvage formation has a return date (for example, when not using CamOps salvage), the
     * scenario's own return date is used instead, falling back to the track's deployment time.</p>
     *
     * @param campaign the current campaign
     * @param scenario the scenario the salvage came from
     * @param contract the contract to search within
     *
     * @return the number of days until the salvage arrives, or 0 if the scenario is not found or the contract has no
     *       StratCon state
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int getSalvageDeliveryTime(Campaign campaign, Scenario scenario, AbstractContract contract) {
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if (campaignState == null) {
            return 0;
        }

        for (StratConTrackState track : campaignState.getTracks()) {
            StratConScenario stratConScenario = track.getBackingScenariosMap().get(scenario.getId());
            if (stratConScenario == null) {
                continue;
            }

            LocalDate returnDate = null;
            Map<Integer, LocalDate> formationReturnDates = track.getAssignedForceReturnDates();
            for (int formationId : scenario.getSalvageFormations()) {
                LocalDate formationReturnDate = formationReturnDates.get(formationId);
                if ((formationReturnDate != null) &&
                          ((returnDate == null) || formationReturnDate.isAfter(returnDate))) {
                    returnDate = formationReturnDate;
                }
            }

            if (returnDate == null) {
                returnDate = stratConScenario.getReturnDate();
            }

            if (returnDate == null) {
                return track.getDeploymentTime();
            }

            return (int) max(0, ChronoUnit.DAYS.between(campaign.getLocalDate(), returnDate));
        }

        return 0;
    }

    /**
     * Performs risky salvage safety checks for assigned technicians.
     *
     * <p>This method simulates the dangers of salvage operations by rolling for potential accidents and injuries
     * among the assigned technicians. For each salvaged unit, there is a small chance (snake eyes on 2d6) that an
     * injury event occurs. When an injury event occurs, a random technician from the assigned pool is injured.</p>
     *
     * <p>The severity of injuries is determined by rolling 1d6 for hits, which may be modified by the victim's SPAs
     * (Special Pilot Abilities). The method respects the campaign's medical system settings, using either advanced
     * medical injury resolution or simple hit tracking.</p>
     *
     * <p>If a technician accumulates more than 5 injuries or hits as a result of the accident, their status is
     * changed to {@link PersonnelStatus#ACCIDENTAL} (deceased due to accident).</p>
     *
     * <p>Key features:</p>
     * <ul>
     *   <li>Rolls 2d6 for each salvaged unit; snake eyes (2) triggers an injury event</li>
     *   <li>Random technician selection for each injury event</li>
     *   <li>Injury severity adjusted by victim's SPAs and campaign fatigue settings</li>
     *   <li>Compatible with both simple and advanced medical systems</li>
     *   <li>Generates campaign report if any accidents occur</li>
     * </ul>
     *
     * @param campaign              the current campaign
     * @param techUUIDs             list of technicians assigned to salvage operations (may be modified if techs die)
     * @param numberOfSalvagedUnits the number of units being salvaged
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void performRiskySalvageChecks(Campaign campaign, List<UUID> techUUIDs, int numberOfSalvagedUnits) {
        if (techUUIDs.isEmpty()) {
            return;
        }

        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final boolean isUseAdvancedMedical = campaignOptions.isUseAdvancedMedical();
        final int fatigueRate = campaignOptions.get(CampaignOption.FATIGUE_RATE);
        final boolean useInjuryFatigue = campaignOptions.get(CampaignOption.USE_INJURY_FATIGUE);

        int injuryEvents = getInjuryEventsCount(numberOfSalvagedUnits);

        List<Person> techs = getValidTechs(campaign, techUUIDs);

        StringBuilder accidentDetails = new StringBuilder();
        for (int i = 0; i < injuryEvents; i++) {
            if (techs.isEmpty()) {
                break;
            }

            Person victim = ObjectUtility.getRandomItem(techs);

            if (campaignOptions.get(CampaignOption.USE_EDGE) && victim.getCurrentEdge() > 0) {
                if (performEdgeReroll(campaign, victim)) {
                    continue;
                }
            }

            int newHits = injuryVictim(campaign, victim, useInjuryFatigue, fatigueRate, isUseAdvancedMedical);

            boolean isKilled = testForDeath(campaign, victim, techs);
            accidentDetails.append(getAccidentDetail(victim, newHits, isKilled));

            MekHQ.triggerEvent(new PersonChangedEvent(victim));
        }

        if (!accidentDetails.isEmpty()) {
            campaign.addReport(MEDICAL,
                  getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.accident",
                        spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG) + accidentDetails);
        }
    }

    /**
     * Builds the report line describing what happened to a single accident victim.
     *
     * @param victim   the tech injured in the accident
     * @param newHits  the number of hits the tech took in this accident
     * @param isKilled {@code true} if the accident killed the tech
     *
     * @return a report line naming the tech (hyperlinked), the hits they took, and whether they died
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static String getAccidentDetail(Person victim, int newHits, boolean isKilled) {
        if (isKilled) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.accident.killed",
                  victim.getHyperlinkedFullTitle(), newHits, spanOpeningWithCustomColor(getNegativeColor()),
                  CLOSING_SPAN_TAG);
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.accident.injured",
              victim.getHyperlinkedName(), newHits);
    }

    /**
     * Checks whether an accident victim's injuries have killed them, and if so, marks them as dead.
     *
     * @param campaign the current campaign
     * @param victim   the tech injured in the accident
     * @param techs    the techs still eligible to be injured; the victim is removed if they died
     *
     * @return {@code true} if the victim died
     */
    private static boolean testForDeath(Campaign campaign, Person victim, List<Person> techs) {
        if (victim.getTotalInjurySeverity() >= DEATH) {
            victim.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACCIDENTAL);
            techs.remove(victim); // We're nice enough that we only kill each tech once
            return true;
        }

        return false;
    }

    /**
     * Injures an accident victim, applying 1d6 hits adjusted for their SPAs.
     *
     * @return the number of hits the victim took
     */
    private static int injuryVictim(Campaign campaign, Person victim, boolean useInjuryFatigue, int fatigueRate,
          boolean isUseAdvancedMedical) {
        int newHits = d6(1);
        newHits = InjurySPAUtility.adjustInjuriesAndFatigueForSPAs(victim, useInjuryFatigue, fatigueRate, newHits);

        if (isUseAdvancedMedical) {
            InjuryUtil.resolveCombatDamage(campaign, victim, newHits);
        } else {
            int priorHits = victim.getHits();
            victim.setHits(priorHits + newHits);
        }

        return newHits;
    }

    private static boolean performEdgeReroll(Campaign campaign, Person victim) {
        if (victim.getOptions().booleanOption(PersonnelOptions.EDGE_SALVAGE_ACCIDENTS)) {
            campaign.addReport(PERSONNEL,
                  getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.reroll",
                        victim.getHyperlinkedName()));
            victim.spendEdge();

            int roll = d6(2);
            return roll != 2;
        }

        return false;
    }

    private static @Nonnull List<Person> getValidTechs(Campaign campaign, List<UUID> techUUIDs) {
        List<Person> techs = new ArrayList<>();
        for (UUID uuid : techUUIDs) {
            Person tech = campaign.getPlayerForce().getHumanResources().getPerson(uuid);
            if (tech == null) {
                LOGGER.error("Salvage tech {} not found in campaign", uuid);
                continue;
            }

            techs.add(tech);
        }
        return techs;
    }

    private static int getInjuryEventsCount(int numberOfSalvagedUnits) {
        int injuryEvents = 0;
        for (int i = 0; i < numberOfSalvagedUnits; i++) {
            int roll = d6(2);
            if (roll == 2) {
                injuryEvents++;
            }
        }
        return injuryEvents;
    }

    /**
     * Deducts the minutes spent on salvage operations from the assigned technicians.
     *
     * <p>The work is spread as evenly as possible across the technicians. Techs with the least remaining time are
     * processed first; any share they cannot cover is carried over to the techs with more time remaining.</p>
     *
     * <p>If a technician UUID cannot be found in the campaign, an error is logged and that entry is skipped.</p>
     *
     * @param campaign    the campaign containing the technicians
     * @param techs       list of technician UUIDs whose time should be depleted
     * @param minutesUsed the total number of minutes spent on salvage operations
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void depleteTechMinutes(Campaign campaign, List<UUID> techs, int minutesUsed) {
        List<Person> validTechs = getValidTechs(campaign, techs);
        validTechs.sort(Comparator.comparingInt(Person::getMinutesLeft));

        int remainingMinutes = max(0, minutesUsed);
        int techsRemaining = validTechs.size();
        for (Person tech : validTechs) {
            if (remainingMinutes <= 0) {
                break;
            }

            int minutesLeft = max(0, tech.getMinutesLeft());
            int share = (remainingMinutes + techsRemaining - 1) / techsRemaining; // Round up
            int minutesSpent = Math.min(share, minutesLeft);

            tech.setMinutesLeft(minutesLeft - minutesSpent);
            remainingMinutes -= minutesSpent;
            techsRemaining--;
        }
    }

    /**
     * A lightweight record for associating a {@link StratConTrackState} with its corresponding {@link StratConCoords}
     * on the campaign map.
     *
     * <p>This is used internally when resolving which strategic track and coordinates a scenario belongs to during
     * salvage team deployment. It allows both values to be returned together as a single, immutable result.</p>
     *
     * @param track  the {@link StratConTrackState} that contains the scenario
     * @param coords the {@link StratConCoords} location of the scenario within that track
     *
     * @author Illiani
     * @since 0.50.10
     */
    private record TrackLocation(StratConTrackState track, StratConCoords coords) {}

    /**
     * Searches a given {@link StratConCampaignState} for the track and coordinates corresponding to a specific
     * {@link Scenario}.
     *
     * <p>This method iterates over all tracks and scenarios within the provided campaign state, comparing each
     * scenario’s backing scenario ID to the ID of the provided {@link Scenario}. When a match is found, the associated
     * track and coordinates are returned as a {@link TrackLocation}.</p>
     *
     * @param scenario the scenario to locate within the campaign state
     * @param state    the {@link StratConCampaignState} to search
     *
     * @return an {@link Optional} containing the {@link TrackLocation} if found, or an empty Optional if the scenario
     *       is not part of any track
     *
     * @since 0.50.10
     */
    private static Optional<TrackLocation> findTrackAndCoords(Scenario scenario, StratConCampaignState state) {
        final int scenarioId = scenario.getId();
        for (StratConTrackState track : state.getTracks()) {
            for (Map.Entry<StratConCoords, StratConScenario> entry : track.getScenarios().entrySet()) {
                if (entry.getValue().getBackingScenarioID() == scenarioId) {
                    return Optional.of(new TrackLocation(track, entry.getKey()));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Deploys all salvage forces from a scenario onto its corresponding strategic track location within the contract’s
     * {@link StratConCampaignState}.
     *
     * <p>This method identifies the {@link StratConTrackState} and {@link StratConCoords} that correspond to the
     * given {@link Scenario}, and assigns each salvage-capable {@link Formation} participating in the scenario to that
     * location. If the scenario is not part of an {@link AbstractContract}, or if the contract has no active strategic
     * campaign state, the method exits without making changes.</p>
     *
     * @param campaign the active {@link Campaign} containing forces and missions
     * @param scenario the {@link Scenario} whose salvage teams are being deployed
     *
     * @since 0.50.10
     */
    public static void deploySalvageTeams(Campaign campaign, Scenario scenario) {
        final AbstractContract mission = campaign.getContract(scenario.getMissionId());

        final StratConCampaignState state = mission.getStratConCampaignState();
        if (state == null) {
            return;
        }

        findTrackAndCoords(scenario, state).ifPresent(loc -> {
            for (int forceId : scenario.getSalvageFormations()) {
                Formation formation = campaign.getPlayerForce().getFormation(forceId);
                if (formation != null) {
                    loc.track().assignForce(forceId, loc.coords(), campaign.getLocalDate(), false);
                }
            }
        });
    }
}
