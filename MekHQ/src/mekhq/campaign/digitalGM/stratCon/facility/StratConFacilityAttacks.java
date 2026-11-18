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

import static mekhq.campaign.digitalGM.stratCon.facility.StratConEnemyFacilityActivity.COUNTERATTACK_MAXIMUM_WARNING_DAYS;
import static mekhq.campaign.digitalGM.stratCon.facility.StratConEnemyFacilityActivity.COUNTERATTACK_MINIMUM_WARNING_DAYS;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.common.compute.Compute;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConScenarioTempo;
import mekhq.campaign.digitalGM.stratCon.StratConSpawnRates;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConEnemyFacilityActivity.CounterattackTarget;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinition;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinitions;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestRules;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConSupplyInterdictionBehavior;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The enemy's attacks on facilities held by the player or their employer.
 *
 * <p>{@value StratConSpawnRates#FACILITY_ATTACKS_PER_SCALE} come each month per point of the contract's Scale, each on
 * a random day of its month, scheduled when the contract is accepted. On its day, an attack is one of these, each as
 * likely as the others:</p>
 *
 * <ul>
 *     <li><b>A Supply Interdiction:</b> raiders sit across one of the player's supply routes (see
 *     {@link StratConSupplyInterdictionBehavior}). Only with supply lines in use, and only when there is a route to
 *     sit on.</li>
 *     <li><b>A counterattack:</b> a fight on a random facility of the player's side, with
 *     {@value StratConEnemyFacilityActivity#COUNTERATTACK_MINIMUM_WARNING_DAYS} to
 *     {@value StratConEnemyFacilityActivity#COUNTERATTACK_MAXIMUM_WARNING_DAYS} days' warning. One whose warning would
 *     run past the contract's end is an assault instead.</li>
 *     <li><b>An assault:</b> as a counterattack, in the same templates, but fought today.</li>
 * </ul>
 *
 * <p>Counterattacks and assaults are never Crises or Turning Points, and never pay the combat bonus. Losing one hands
 * the facility to the enemy; ignoring one does too, except that one held by the employer is left to an off-screen roll
 * (see {@link StratConEnemyFacilityActivity#resolveIgnoredCounterattack}).</p>
 *
 * <p>An attack only comes while the player's side holds a facility with no fight already on it; one that comes due
 * when there is none is dropped. None come while a Garrison Duty or Retainer contract is peaceful, between raids. The
 * enemy never attacks its own facilities: dealing with those is left to the player.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConFacilityAttacks {
    private static final MMLogger LOGGER = MMLogger.create(StratConFacilityAttacks.class);

    /** The kinds of attack the enemy makes on a facility of the player's side. */
    enum FacilityAttackType {
        SUPPLY_INTERDICTION,
        COUNTERATTACK,
        ASSAULT
    }

    private StratConFacilityAttacks() {}

    /**
     * Schedules a contract's facility attacks from the given day to the contract's end, replacing any scheduled from
     * that day on: {@value StratConSpawnRates#FACILITY_ATTACKS_PER_SCALE} a month per point of Scale, each on a random
     * day within its month. Does nothing without a settled start date.
     *
     * @param contract      the contract
     * @param campaignState the contract's StratCon campaign state
     * @param fromDate      the first day to schedule on; the contract's start date when it is accepted
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void scheduleFacilityAttacks(AbstractContract contract, StratConCampaignState campaignState,
          @Nullable LocalDate fromDate) {
        LocalDate scheduledUntil = StratConEnemyFacilityActivity.scheduleMonthlyDates(contract,
              campaignState,
              campaignState.getFacilityAttackDates(),
              StratConSpawnRates.FACILITY_ATTACKS_PER_SCALE,
              fromDate);
        if (scheduledUntil != null) {
            campaignState.setFacilityAttacksScheduledUntil(scheduledUntil);
        }
    }

    /**
     * The contract-level daily step: makes the facility attacks due today. A contract whose attacks were never
     * scheduled - one from before they were - is scheduled from today first.
     *
     * @param campaign      the current campaign
     * @param contract      the contract
     * @param campaignState the contract's StratCon campaign state
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void processFacilityAttacks(Campaign campaign, AbstractContract contract,
          StratConCampaignState campaignState) {
        LocalDate today = campaign.getLocalDate();
        LocalDate scheduledUntil = campaignState.getFacilityAttacksScheduledUntil();
        if ((scheduledUntil == null) || !today.isBefore(scheduledUntil)) {
            scheduleFacilityAttacks(contract, campaignState, today);
        }

        int dueCount = StratConScenarioTempo.takeDueScenarioDates(campaignState.getFacilityAttackDates(), today);
        // With no enemy activity between raids, there is no one to attack; and without a map, nothing to attack.
        if ((dueCount == 0) || contract.isPeaceful() || campaign.getCampaignOptions().isUseStratConMaplessMode()) {
            return;
        }

        for (int attack = 0; attack < dueCount; attack++) {
            launchFacilityAttack(campaign, contract, campaignState);
        }
    }

    /**
     * Makes one attack on a facility of the player's side (see the class description), if there is one to attack.
     *
     * @param campaign      the current campaign
     * @param contract      the contract
     * @param campaignState the contract's StratCon campaign state
     *
     * @return the kind of attack made, or {@code null} if none could be
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable FacilityAttackType launchFacilityAttack(Campaign campaign, AbstractContract contract,
          StratConCampaignState campaignState) {
        // Worked out once: placing a Supply Interdiction below changes no facility, so the targets still stand after it
        List<CounterattackTarget> targets = StratConEnemyFacilityActivity.getCounterattackTargets(campaignState);
        if (targets.isEmpty()) {
            LOGGER.info("Facility attack on contract {} dropped: the player's side holds no facility to attack.",
                  contract.getName());
            return null;
        }

        boolean isInterdictionPossible = StratConFacilitySupply.isSupplyLinesActive(campaign)
                                               && hasInterdictionCandidates(campaignState);
        FacilityAttackType attackType = pickAttackType(isInterdictionPossible);

        if (attackType == FacilityAttackType.SUPPLY_INTERDICTION) {
            StratConPointOfInterest pointOfInterest = StratConFacilitySupply.placeSupplyInterdiction(campaignState,
                  campaign.getLocalDate());
            if (pointOfInterest != null) {
                StratConPointOfInterestRules.announceNewPointsOfInterest(campaign, contract, List.of(pointOfInterest));
                return attackType;
            }
            // No route took it after all, so the enemy strikes the facilities themselves.
            attackType = pickAttackType(false);
        }

        CounterattackTarget target = targets.get(Compute.randomInt(targets.size()));

        int warningDays = 0;
        if (attackType == FacilityAttackType.COUNTERATTACK) {
            warningDays = COUNTERATTACK_MINIMUM_WARNING_DAYS
                                + Compute.randomInt(COUNTERATTACK_MAXIMUM_WARNING_DAYS
                                                          - COUNTERATTACK_MINIMUM_WARNING_DAYS + 1);
            // Like any scenario, a counterattack must fall before the contract ends; one that would not comes today.
            LocalDate contractEnd = contract.getEndingDate();
            if ((contractEnd != null) && !campaign.getLocalDate().plusDays(warningDays).isBefore(contractEnd)) {
                attackType = FacilityAttackType.ASSAULT;
                warningDays = 0;
            }
        }

        boolean isAssault = attackType == FacilityAttackType.ASSAULT;
        StratConScenario scenario = StratConRulesManager.startCounterattackScenario(campaign,
              contract,
              target.track(),
              target.coords(),
              warningDays,
              isAssault);
        if (scenario == null) {
            LOGGER.warn("Could not generate a {} on {} in sector {}.",
                  isAssault ? "facility assault" : "counterattack",
                  target.coords(),
                  target.track().getDisplayableName());
            return null;
        }

        StratConEnemyFacilityActivity.announceCounterattack(campaign, target, scenario, isAssault);
        return attackType;
    }

    /**
     * Picks the kind of attack the enemy makes, each of those possible as likely as the others.
     *
     * @param isInterdictionPossible whether a Supply Interdiction can be made
     *
     * @return the kind of attack
     *
     * @author Illiani
     * @since 0.51.01
     */
    static FacilityAttackType pickAttackType(boolean isInterdictionPossible) {
        List<FacilityAttackType> attackTypes = getPossibleAttackTypes(isInterdictionPossible);
        return attackTypes.get(Compute.randomInt(attackTypes.size()));
    }

    /**
     * @param isInterdictionPossible whether a Supply Interdiction can be made
     *
     * @return the kinds of attack the enemy can make: a Supply Interdiction first, if one can be made, then a
     *       counterattack and an assault
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<FacilityAttackType> getPossibleAttackTypes(boolean isInterdictionPossible) {
        List<FacilityAttackType> attackTypes = new ArrayList<>();
        if (isInterdictionPossible) {
            attackTypes.add(FacilityAttackType.SUPPLY_INTERDICTION);
        }
        attackTypes.add(FacilityAttackType.COUNTERATTACK);
        attackTypes.add(FacilityAttackType.ASSAULT);
        return attackTypes;
    }

    /**
     * @param campaignState the contract's StratCon campaign state
     *
     * @return {@code true} if any sector has a player supply route a Supply Interdiction could sit across
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean hasInterdictionCandidates(StratConCampaignState campaignState) {
        StratConPointOfInterestDefinition definition = StratConPointOfInterestDefinitions.getDefinition(
              StratConSupplyInterdictionBehavior.TYPE_ID);
        if (definition == null) {
            return false;
        }

        for (StratConTrackState track : campaignState.getTracks()) {
            if (!StratConFacilitySupply.getInterdictionCandidates(track, definition).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
