/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.digitalGM.stratCon.facility;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConScenario.ScenarioState;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityIntel;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConEnemyEngineersBehavior;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * Works out what the player can do about the facilities on a StratCon map, and what needs their attention, so the
 * interface can tell them in plain words instead of leaving them to discover the rules from greyed-out menu items.
 *
 * <p>Nothing here changes the campaign; it only reads the rules in {@link StratConFacilityOperations},
 * {@link StratConFacilitySiege} and {@link StratConFacilitySupply}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConFacilityAdvisor {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConFacilityOperations";

    private StratConFacilityAdvisor() {
    }

    /**
     * One order the player could give on a hex, with everything the interface needs to offer it or explain why not.
     *
     * @param operation            the order
     * @param supportPointCost     the Employer Support it costs to give
     * @param reasonKey            the resource key of why it cannot be given now, or {@code null} if it can
     * @param guidanceKey          the resource key of what the player could do to make it possible, or {@code null} if
     *                             there is nothing useful to suggest
     * @param eligibleFormationIds the formations that could carry it out, empty if it cannot be given
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record OrderOption(FacilityOperation operation, int supportPointCost, @Nullable String reasonKey,
          @Nullable String guidanceKey, List<Integer> eligibleFormationIds) {
        public boolean isAvailable() {
            return reasonKey == null;
        }
    }

    /**
     * Something about to happen to a facility, or on the map, that the player may want to act on before it does.
     *
     * @param date        the day it happens
     * @param track       the sector it happens in
     * @param coords      the hex it happens on
     * @param description what happens, ready to show
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record UpcomingEvent(LocalDate date, StratConTrackState track, StratConCoords coords, String description) {
    }

    /**
     * @param campaign the current campaign
     * @param contract the contract whose map holds the sector
     * @param track    the sector
     * @param coords   the hex
     *
     * @return every order that makes sense on the hex, in the order they are declared, each with whether it can be
     *       given now and, if not, why and what to do about it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<OrderOption> getOrderOptions(Campaign campaign, AbstractContract contract,
          StratConTrackState track, StratConCoords coords) {
        List<OrderOption> options = new ArrayList<>();
        for (FacilityOperation operation : StratConFacilityOperations.getOperationsFor(track, coords)) {
            String reasonKey = StratConFacilityOperations.getUnavailableReasonKey(campaign,
                  contract,
                  track,
                  coords,
                  operation);
            List<Integer> formationIds = new ArrayList<>();
            if (reasonKey == null) {
                formationIds = StratConFacilityOperations.getEligibleFormationIds(campaign, track, coords, operation);
                if (formationIds.isEmpty()) {
                    reasonKey = "contextMenu.noFormation";
                }
            }
            options.add(new OrderOption(operation,
                  StratConFacilityOperations.getSupportPointCost(operation),
                  reasonKey,
                  getGuidanceKey(reasonKey, operation),
                  formationIds));
        }
        return options;
    }

    /**
     * @param reasonKey the reason an order cannot be given, or {@code null}
     * @param operation the order
     *
     * @return the resource key of advice on what the player could do to make the order possible, or {@code null} if
     *       the order can be given or nothing the player does would help
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable String getGuidanceKey(@Nullable String reasonKey, FacilityOperation operation) {
        if (reasonKey == null) {
            return null;
        }
        return switch (reasonKey) {
            case "contextMenu.noFormation" -> {
                if (operation.isOnlyFromAdjacentHex()) {
                    yield "guidance.noFormation.adjacentOnly";
                }
                yield operation.isAllowedFromAdjacentHex() ? "guidance.noFormation.adjacent" :
                            "guidance.noFormation.onHex";
            }
            case "reason.supportPoints" -> "guidance.supportPoints";
            case "reason.needsDetailedIntel" -> "guidance.needsDetailedIntel";
            case "reason.scenarioUnderway" -> "guidance.scenarioUnderway";
            default -> null;
        };
    }

    /**
     * Works out the next steps worth pointing out for a hex: what is under way there, what is about to happen, and
     * what the player needs to do to act on it. A hex with nothing worth saying gets no hints.
     *
     * @param campaign the current campaign
     * @param contract the contract whose map holds the sector
     * @param track    the sector
     * @param coords   the hex
     *
     * @return the hints, ready to show, most pressing first
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<String> getHints(Campaign campaign, AbstractContract contract, StratConTrackState track,
          StratConCoords coords) {
        List<String> hints = new ArrayList<>();
        if (!StratConFacilityOperations.isEnabled(campaign)) {
            return hints;
        }

        StratConFacility facility = track.getFacility(coords);
        LocalDate today = campaign.getLocalDate();

        StratConScenario counterattack = getCounterattack(track, coords);
        if (counterattack != null) {
            hints.add((counterattack.getCurrentState() == ScenarioState.UNRESOLVED) ?
                            getFormattedTextAt(RESOURCE_BUNDLE, "hint.counterattack", getDeadline(counterattack)) :
                            getTextAt(RESOURCE_BUNDLE, "hint.counterattack.committed"));
        }

        for (StratConFacilityOrder order : track.getFacilityOrders()) {
            if (!order.getTargetCoords().equals(coords) || (order.getOperation() == FacilityOperation.SIEGE)) {
                continue;
            }
            hints.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "hint.orderUnderway",
                  getFormationName(campaign, order.getFormationId()),
                  getTextAt(RESOURCE_BUNDLE, "operation." + order.getOperation().name()),
                  Math.max(0, ChronoUnit.DAYS.between(today, order.getCompletionDate()))));
        }

        if (facility == null) {
            addEmptyHexHints(campaign, contract, track, coords, hints);
            return hints;
        }

        int besiegerCount = StratConFacilitySiege.getSieges(track, coords).size();
        if (besiegerCount > 0) {
            if (facility.getIntel().isAtLeast(FacilityIntel.DETAILED)) {
                hints.add(getFormattedTextAt(RESOURCE_BUNDLE,
                      "hint.besieged.detailed",
                      besiegerCount,
                      getWeeksToSurrender(facility.getGarrison()
                                                - (facility.hasTrait(FacilityTrait.POOR_MORALE) ? 1 : 0),
                            besiegerCount)));
            } else {
                hints.add(getFormattedTextAt(RESOURCE_BUNDLE, "hint.besieged", besiegerCount));
            }
        }

        if (StratConFacilitySupply.isCutOff(track, coords)) {
            hints.add(getTextAt(RESOURCE_BUNDLE,
                  facility.isOwnerAlliedToPlayer() ? "hint.cutOff.own" : "hint.cutOff.enemy"));
        }

        if (facility.isOwnerAlliedToPlayer()) {
            if (facility.getGarrison() < facility.getGarrisonMaximum()) {
                hints.add(getTextAt(RESOURCE_BUNDLE, "hint.understrength"));
            }
            return hints;
        }

        if ((besiegerCount == 0) && (counterattack == null)) {
            hints.add(getTextAt(RESOURCE_BUNDLE,
                  hasAnyOrderAvailable(campaign, contract, track, coords) ? "hint.ordersAvailable" :
                        "hint.deployToAct"));
        }
        return hints;
    }

    private static void addEmptyHexHints(Campaign campaign, AbstractContract contract, StratConTrackState track,
          StratConCoords coords, List<String> hints) {
        if (!track.areAnyForceDeployedTo(coords)) {
            return;
        }
        if (hasAnyOrderAvailable(campaign, contract, track, coords)) {
            hints.add(getTextAt(RESOURCE_BUNDLE, "hint.emptyHexOrders"));
        }
    }

    private static boolean hasAnyOrderAvailable(Campaign campaign, AbstractContract contract,
          StratConTrackState track, StratConCoords coords) {
        for (OrderOption option : getOrderOptions(campaign, contract, track, coords)) {
            if (option.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param garrison      the besieged facility's garrison
     * @param besiegerCount how many formations besiege it
     *
     * @return how many weekly siege steps it takes to empty the garrison, at least one
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int getWeeksToSurrender(int garrison, int besiegerCount) {
        int stepsPerWeek = Math.max(1, Math.min(besiegerCount, 2));
        return Math.max(1, (garrison + stepsPerWeek - 1) / stepsPerWeek);
    }

    /**
     * @return the counterattack against the facility on a hex that has not been fought yet, or {@code null} if there is
     *       none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConScenario getCounterattack(StratConTrackState track, StratConCoords coords) {
        StratConScenario scenario = track.getScenario(coords);
        if ((scenario != null) && scenario.isCounterattack() && isPending(scenario)) {
            return scenario;
        }
        return null;
    }

    /**
     * @return the date the player must deploy against a scenario by; its battle date if it has no deployment date
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable LocalDate getDeadline(StratConScenario scenario) {
        return (scenario.getDeploymentDate() != null) ? scenario.getDeploymentDate() : scenario.getActionDate();
    }

    /**
     * @return {@code true} if the scenario has not been fought yet, whether or not the player has committed forces
     */
    private static boolean isPending(StratConScenario scenario) {
        ScenarioState state = scenario.getCurrentState();
        return (state == ScenarioState.UNRESOLVED)
                     || (state == ScenarioState.PRIMARY_FORCES_COMMITTED)
                     || (state == ScenarioState.AWAITING_REINFORCEMENTS)
                     || (state == ScenarioState.REINFORCEMENTS_COMMITTED);
    }

    /**
     * Lists, soonest first, what is about to happen across a contract's map that concerns facilities: counterattacks,
     * siege fights, orders coming due, enemy engineers finishing their work, and supply-line cuts running out. Only
     * what the player can see is listed.
     *
     * @param campaign      the current campaign
     * @param campaignState the contract's StratCon state
     *
     * @return the upcoming events, soonest first
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<UpcomingEvent> getUpcomingEvents(Campaign campaign, StratConCampaignState campaignState) {
        List<UpcomingEvent> events = new ArrayList<>();
        for (StratConTrackState track : campaignState.getTracks()) {
            addScenarioEvents(track, events);
            addOrderEvents(campaign, track, events);
            addEngineerEvents(track, events);

            for (StratConRoadCut roadCut : track.getRoadCuts()) {
                events.add(new UpcomingEvent(roadCut.getEndDate(),
                      track,
                      roadCut.getCoords(),
                      getTextAt(RESOURCE_BUNDLE, "event.roadCutEnds")));
            }
        }
        events.sort(Comparator.comparing(UpcomingEvent::date));
        return events;
    }

    private static void addScenarioEvents(StratConTrackState track, List<UpcomingEvent> events) {
        for (Map.Entry<StratConCoords, StratConScenario> entry : track.getScenarios().entrySet()) {
            StratConScenario scenario = entry.getValue();
            LocalDate deadline = getDeadline(scenario);
            if (!isPending(scenario) || (deadline == null)) {
                continue;
            }

            if (scenario.isCounterattack()) {
                StratConFacility facility = track.getFacility(entry.getKey());
                String facilityName = (facility == null) ? "" : facility.getDisplayableName();
                events.add(new UpcomingEvent(deadline,
                      track,
                      entry.getKey(),
                      getFormattedTextAt(RESOURCE_BUNDLE, "event.counterattack", facilityName)));
            } else if (scenario.getSiegeCoords() != null) {
                StratConFacility facility = track.getFacility(scenario.getSiegeCoords());
                String facilityName = (facility == null) ? "" : facility.getDisplayableName();
                events.add(new UpcomingEvent(deadline,
                      track,
                      entry.getKey(),
                      getFormattedTextAt(RESOURCE_BUNDLE,
                            scenario.isSiegeSortie() ? "event.sortie" : "event.relief",
                            facilityName)));
            }
        }
    }

    private static void addOrderEvents(Campaign campaign, StratConTrackState track, List<UpcomingEvent> events) {
        for (StratConFacilityOrder order : track.getFacilityOrders()) {
            // A siege has no end date; its weekly steps are shown with the facility instead.
            if (order.getOperation() == FacilityOperation.SIEGE) {
                continue;
            }
            events.add(new UpcomingEvent(order.getCompletionDate(),
                  track,
                  order.getTargetCoords(),
                  getFormattedTextAt(RESOURCE_BUNDLE,
                        "event.orderDue",
                        getFormationName(campaign, order.getFormationId()),
                        getTextAt(RESOURCE_BUNDLE, "operation." + order.getOperation().name()))));
        }
    }

    private static void addEngineerEvents(StratConTrackState track, List<UpcomingEvent> events) {
        for (StratConPointOfInterest pointOfInterest : track.getPointsOfInterest()) {
            boolean isEngineers = StratConEnemyEngineersBehavior.TYPE_ID.equals(pointOfInterest.getTypeId());
            if (!isEngineers || !pointOfInterest.isActive() || (pointOfInterest.getExpiryDate() == null)
                      || !pointOfInterest.isVisibleToPlayer(track)) {
                continue;
            }
            events.add(new UpcomingEvent(pointOfInterest.getExpiryDate(),
                  track,
                  pointOfInterest.getCoords(),
                  getTextAt(RESOURCE_BUNDLE, "event.engineers")));
        }
    }

    /**
     * @return a short summary of what is happening at a facility right now (siege, order under way, counterattack),
     *       or an empty string if nothing is
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String getActivitySummary(Campaign campaign, StratConTrackState track, StratConCoords coords) {
        List<String> parts = new ArrayList<>();
        StratConScenario counterattack = getCounterattack(track, coords);
        if (counterattack != null) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "activity.counterattack", getDeadline(counterattack)));
        }

        int besiegerCount = StratConFacilitySiege.getSieges(track, coords).size();
        if (besiegerCount > 0) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "activity.besieged", besiegerCount));
        }

        for (StratConFacilityOrder order : track.getFacilityOrders()) {
            if (order.getTargetCoords().equals(coords) && (order.getOperation() != FacilityOperation.SIEGE)) {
                parts.add(getFormattedTextAt(RESOURCE_BUNDLE,
                      "activity.order",
                      getTextAt(RESOURCE_BUNDLE, "operation." + order.getOperation().name()),
                      order.getCompletionDate()));
            }
        }
        return String.join(", ", parts);
    }

    /**
     * @return {@code true} if the player knows the facility's traits: they have detailed intel on it, which they always
     *       have on their own side's facilities
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean areTraitsKnown(StratConFacility facility) {
        return facility.getIntel().isAtLeast(FacilityIntel.DETAILED);
    }

    /**
     * @return the names of the facility's traits, comma-separated, or a note that it has none or that they are not yet
     *       known
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String getTraitSummary(StratConFacility facility) {
        if (!areTraitsKnown(facility)) {
            return getTextAt(RESOURCE_BUNDLE, "traits.unknown");
        }
        if (facility.getTraits().isEmpty()) {
            return getTextAt(RESOURCE_BUNDLE, "traits.none");
        }
        List<String> names = new ArrayList<>();
        for (FacilityTrait trait : facility.getTraits()) {
            names.add(getTextAt(RESOURCE_BUNDLE, "trait." + trait.name()));
        }
        return String.join(", ", names);
    }

    /**
     * @return the formation's name, or its ID if it no longer exists
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String getFormationName(Campaign campaign, int formationId) {
        Formation formation = campaign.getPlayerForce().getFormation(formationId);
        return (formation == null) ? String.valueOf(formationId) : formation.getName();
    }
}
