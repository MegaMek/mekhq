/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.camOpsSalvage;

import static megamek.common.compute.Compute.d6;
import static megamek.common.equipment.MiscType.F_NAVAL_TUG_ADAPTOR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.equipment.Mounted;
import megamek.common.units.Aero;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Warship;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.InjurySPAUtility;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.stratCon.StratConCoords;
import mekhq.campaign.stratCon.StratConScenario;
import mekhq.campaign.stratCon.StratConTrackState;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

public class CamOpsSalvageUtilities {
    private static final MMLogger LOGGER = MMLogger.create(CamOpsSalvageUtilities.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CamOpsSalvage";

    /**
     * Generates a tooltip string describing the salvage capabilities of units in a force.
     *
     * <p>For each unit capable of salvage, the tooltip includes:</p>
     * <ul>
     *   <li>Unit name</li>
     *   <li>Drag/tow capacity in tons (for non-large vessels)</li>
     *   <li>Cargo capacity in tons (for non-Mek units)</li>
     *   <li>Naval tug status (for large vessels like DropShips and WarShips)</li>
     * </ul>
     *
     * @param unitsInForce the list of units to analyze for salvage capabilities
     * @param isInSpace    {@code true} if checking space salvage capabilities, {@code false} for ground operations
     *
     * @return an HTML-formatted string describing each salvage-capable unit's capabilities
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static String getSalvageTooltip(List<Unit> unitsInForce, boolean isInSpace) {
        StringBuilder tooltip = new StringBuilder();

        for (Unit unit : unitsInForce) {
            if (unit.canSalvage(isInSpace)) {
                Entity entity = unit.getEntity();
                if (entity != null) {
                    if (!tooltip.isEmpty()) {
                        tooltip.append("<br>");
                    }

                    boolean isLargeVessel = entity instanceof Dropship || entity instanceof Warship;
                    tooltip.append(unit.getName());

                    double tonnage = entity.getTonnage();
                    if (!isLargeVessel) {
                        tooltip.append(" (").append(getFormattedTextAt(RESOURCE_BUNDLE,
                              "CamOpsSalvageUtilities.tooltip.drag", tonnage)).append(")");
                    }

                    double cargoCapacity = unit.getCargoCapacity();
                    if (!(entity instanceof Mek)) {
                        tooltip.append(" (").append(getFormattedTextAt(RESOURCE_BUNDLE,
                              "CamOpsSalvageUtilities.tooltip.cargo", cargoCapacity)).append(")");

                        if (isLargeVessel) {
                            if (CamOpsSalvageUtilities.hasNavalTug(entity)) {
                                tooltip.append(" (").append(getFormattedTextAt(RESOURCE_BUNDLE,
                                      "CamOpsSalvageUtilities.tooltip.tug")).append(")");
                            }
                        }
                    }
                }
            }
        }

        return tooltip.toString();
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
     * Processes and finalizes salvage after player selection.
     *
     * <p>This method handles the actual processing of salvage units, including:</p>
     * <ul>
     *   <li>Adding claimed salvage units to the campaign</li>
     *   <li>Processing sold salvage units and crediting the account</li>
     *   <li>Handling salvage exchange for contracts</li>
     *   <li>Updating contract salvage tracking</li>
     *   <li>Setting repair locations for salvaged units</li>
     * </ul>
     *
     * @param campaign        The current {@link Campaign} to add salvage to.
     * @param mission         The {@link Mission} associated with the salvage.
     * @param scenario        The {@link Scenario} that generated the salvage.
     * @param keptSalvage     The list of units claimed by the player.
     * @param soldSalvage     The list of units that were sold instead of claimed.
     * @param employerSalvage The list of units going to the employer or unclaimed.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void resolveSalvage(Campaign campaign, Mission mission, Scenario scenario,
          List<TestUnit> keptSalvage, List<TestUnit> soldSalvage, List<TestUnit> employerSalvage) {
        boolean isContract = mission instanceof Contract;

        // now let's take care of salvage
        for (TestUnit salvageUnit : keptSalvage) {
            ResolveScenarioTracker.UnitStatus salvageStatus = new ResolveScenarioTracker.UnitStatus(salvageUnit);
            if (salvageUnit.getEntity() instanceof Aero) {
                ((Aero) salvageUnit.getEntity()).setFuelTonnage(((Aero) salvageStatus.getBaseEntity()).getFuelTonnage());
            }
            campaign.clearGameData(salvageUnit.getEntity());
            campaign.addTestUnit(salvageUnit);
            // if this is a contract, add to the salvaged value
            if (isContract) {
                ((Contract) mission).addSalvageByUnit(salvageUnit.getSellValue());
            }
        }

        // And any ransomed salvaged units
        Money unitRansoms = Money.zero();
        if (!soldSalvage.isEmpty()) {
            for (TestUnit ransomedUnit : soldSalvage) {
                unitRansoms = unitRansoms.plus(ransomedUnit.getSellValue());
            }

            if (unitRansoms.isGreaterThan(Money.zero())) {
                campaign.getFinances()
                      .credit(TransactionType.SALVAGE,
                            campaign.getLocalDate(),
                            unitRansoms,
                            getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.unitSale", scenario.getName()));
                campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.unitSale.report",
                      unitRansoms.toAmountString(), scenario.getHyperlinkedName()));
                if (isContract) {
                    ((Contract) mission).addSalvageByUnit(unitRansoms);
                }
            }
        }

        if (isContract) {
            Money value = Money.zero();
            for (TestUnit salvageUnit : employerSalvage) {
                value = value.plus(salvageUnit.getSellValue());
            }
            if (((Contract) mission).isSalvageExchange()) {
                value = value.multipliedBy(((Contract) mission).getSalvagePct()).dividedBy(100);
                campaign.getFinances()
                      .credit(TransactionType.SALVAGE_EXCHANGE,
                            campaign.getLocalDate(),
                            value,
                            getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.exchange", scenario.getName()));
                campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.exchange.report",
                      unitRansoms.toAmountString(), scenario.getHyperlinkedName()));
            } else {
                ((Contract) mission).addSalvageByEmployer(value);
            }
        }

        for (TestUnit unit : keptSalvage) {
            unit.setSite(mission.getRepairLocation());
        }
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
        final int fatigueRate = campaignOptions.getFatigueRate();
        final boolean useInjuryFatigue = campaignOptions.isUseInjuryFatigue();

        int injuryEvents = 0;
        for (int i = 0; i < numberOfSalvagedUnits; i++) {
            int roll = d6(2);
            if (roll == 2) {
                injuryEvents++;
            }
        }

        List<Person> techs = new ArrayList<>();
        for (UUID uuid : techUUIDs) {
            Person tech = campaign.getPerson(uuid);
            if (tech == null) {
                LOGGER.error("null tech was passed into risky salvage");
                continue;
            }

            techs.add(tech);
        }

        boolean didAccidentOccur = false;
        for (int i = 0; i < injuryEvents; i++) {
            if (techUUIDs.isEmpty()) {
                break;
            }
            Person victim = ObjectUtility.getRandomItem(techs);

            int newHits = d6(1);
            newHits = InjurySPAUtility.adjustInjuriesAndFatigueForSPAs(victim, useInjuryFatigue, fatigueRate, newHits);

            if (isUseAdvancedMedical) {
                InjuryUtil.resolveCombatDamage(campaign, victim, newHits);
            } else {
                int priorHits = victim.getHits();
                victim.setHits(priorHits + newHits);
            }

            if (victim.getInjuries().size() > 5 || victim.getHits() > 5) {
                victim.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACCIDENTAL);
                techs.remove(victim); // We're nice enough that we only kill each tech once
            }

            MekHQ.triggerEvent(new PersonChangedEvent(victim));
            didAccidentOccur = true;
        }

        if (didAccidentOccur) {
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.accident",
                  spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG));
        }
    }

    /**
     * Depletes the remaining work time for all specified technicians to zero.
     *
     * <p>This method sets the remaining minutes to zero for each technician in the provided list, effectively
     * marking them as having used all their available work time for the current period.</p>
     *
     * <p>If a technician UUID cannot be found in the campaign, an error is logged and that entry is skipped.</p>
     *
     * @param campaign the campaign containing the technicians
     * @param techs    list of technician UUIDs whose time should be depleted
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void depleteTechMinutes(Campaign campaign, List<UUID> techs) {
        for (UUID uuid : techs) {
            Person tech = campaign.getPerson(uuid);
            if (tech == null) {
                LOGGER.error("null tech was passed into depleteTechMinutes");
                continue;
            }

            tech.setMinutesLeft(0);
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
     * given {@link Scenario}, and assigns each salvage-capable {@link Force} participating in the scenario to that
     * location. If the scenario is not part of an {@link AtBContract}, or if the contract has no active strategic
     * campaign state, the method exits without making changes.</p>
     *
     * @param campaign the active {@link Campaign} containing forces and missions
     * @param scenario the {@link Scenario} whose salvage teams are being deployed
     *
     * @since 0.50.10
     */
    public static void deploySalvageTeams(Campaign campaign, Scenario scenario) {
        final Mission mission = campaign.getMission(scenario.getMissionId());

        if (!(mission instanceof AtBContract contract)) {
            return;
        }

        final StratConCampaignState state = contract.getStratconCampaignState();
        if (state == null) {
            return;
        }

        findTrackAndCoords(scenario, state).ifPresent(loc -> {
            for (int forceId : scenario.getSalvageForces()) {
                Force force = campaign.getForce(forceId);
                if (force != null) {
                    loc.track().assignForce(forceId, loc.coords(), campaign.getLocalDate(), false);
                }
            }
        });
    }
}
