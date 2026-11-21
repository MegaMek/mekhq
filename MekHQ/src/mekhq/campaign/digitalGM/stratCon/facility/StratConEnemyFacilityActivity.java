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

import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConScenarioTempo;
import mekhq.campaign.digitalGM.stratCon.StratConSpawnRates;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConEnemyEngineersBehavior;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestPlacer;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestRules;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * What the enemy does with facilities between the player's fights: monthly upkeep of its own facilities, the
 * counterattacks it makes on facilities the player or their employer holds, and enemy engineers building new
 * outposts.
 *
 * <ul>
 *     <li><b>Monthly upkeep.</b> With Facility Operations, at the start of each month, every enemy facility repairs
 *     and reinforces {@value #ENEMY_UPKEEP_STEPS} step, unless a fight is under way there, it is cut off from its
 *     supply lines, or it is under siege.</li>
 *     <li><b>Counterattacks.</b> Started by the contract's facility attacks, and fought the day they come (see
 *     {@link StratConFacilityAttacks}). Losing one outright hands the facility to the enemy; ignoring one does too,
 *     except that one held by the employer is left to an off-screen roll.</li>
 *     <li><b>Enemy engineers.</b> {@value StratConSpawnRates#ENGINEERS_PER_SCALE} a month per point of the contract's
 *     Scale, each on a random day, as a point of interest. If not dealt with before they leave, they become an enemy
 *     outpost (see {@link StratConEnemyEngineersBehavior}). They need Facility Operations.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConEnemyFacilityActivity {
    private static final MMLogger LOGGER = MMLogger.create(StratConEnemyFacilityActivity.class);

    static final String RESOURCE_BUNDLE = "mekhq.resources.StratConEnemyFacilityActivity";

    /** The condition and garrison steps each enemy facility restores every month. */
    static final int ENEMY_UPKEEP_STEPS = 1;

    /** The percentage chance that the enemy sends a relief force against a new siege. */
    static final int RELIEF_CHANCE_PERCENT = 25;

    /** The 2d6 roll, after modifiers, an employer's garrison must meet to hold off an ignored counterattack. */
    static final int OFF_SCREEN_DEFENSE_TARGET = 9;
    /** The condition and garrison steps your employer restores to each facility it holds every month. */
    static final int EMPLOYER_UPKEEP_STEPS = 1;

    private StratConEnemyFacilityActivity() {}

    /**
     * Gives every enemy facility in a sector its monthly upkeep: with Facility Operations,
     * {@value #ENEMY_UPKEEP_STEPS} condition step repaired and garrison step reinforced. Your employer's facilities get
     * {@value #EMPLOYER_UPKEEP_STEPS} step each month either way, since only facilities you hold take your Fortify and
     * Reinforce orders. A facility being fought over, cut off or besieged gets none. Each facility the player can see
     * that changed is reported.
     *
     * @param track    the sector
     * @param campaign the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void applyMonthlyUpkeep(StratConTrackState track, Campaign campaign) {
        boolean isEnemyUpkeepActive = StratConFacilityOperations.isEnabled(campaign);

        for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
            StratConFacility facility = entry.getValue();
            boolean isEmployerHeld = facility.getOwner() == ForceAlignment.Allied;
            boolean isEnemyHeld = facility.getOwner() == ForceAlignment.Opposing;
            // A facility being fought over, cut off from its supply lines, or under siege cannot be repaired.
            if ((!isEmployerHeld && !isEnemyHeld)
                      || (isEnemyHeld && !isEnemyUpkeepActive)
                      || (track.getScenario(entry.getKey()) != null)
                      || StratConFacilitySupply.isCutOff(track, entry.getKey())
                      || StratConFacilitySiege.isBesieged(track, entry.getKey())) {
                continue;
            }

            int steps = isEmployerHeld ? EMPLOYER_UPKEEP_STEPS : ENEMY_UPKEEP_STEPS;
            boolean isChanged = applyUpkeep(facility, steps);
            if (isChanged && facility.getVisible()) {
                campaign.addReport(BATTLE, getFormattedTextAt(RESOURCE_BUNDLE,
                      isEmployerHeld ? "report.upkeep.employer" : "report.upkeep",
                      facility.getDisplayableName(),
                      track.getDisplayableName(),
                      entry.getKey().toBTString()));
            }
        }
    }

    /**
     * Repairs and reinforces a facility by the given number of steps.
     *
     * @param facility the facility
     * @param steps    how many condition steps to repair and garrison steps to reinforce
     *
     * @return {@code true} if its condition or garrison changed
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean applyUpkeep(StratConFacility facility, int steps) {
        StratConFacility.FacilityCondition originalCondition = facility.getCondition();
        int originalGarrison = facility.getGarrison();
        for (int step = 0; step < steps; step++) {
            facility.setCondition(facility.getCondition().improved());
            facility.setGarrison(facility.getGarrison() + 1);
        }
        return (facility.getCondition() != originalCondition) || (facility.getGarrison() != originalGarrison);
    }

    /**
     * Rolls whether the enemy sends a relief force against a new siege: {@value #RELIEF_CHANCE_PERCENT}% of the time.
     *
     * @param percentile a roll from 0 to 99
     *
     * @return {@code true} if a relief force comes
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isReliefSent(int percentile) {
        return percentile < RELIEF_CHANCE_PERCENT;
    }

    /**
     * A facility held by the player or their employer that the enemy can attack: one with no fight already under way
     * on its hex.
     *
     * @param track  the facility's sector
     * @param coords the facility's hex
     *
     * @author Illiani
     * @since 0.51.01
     */
    record CounterattackTarget(StratConTrackState track, StratConCoords coords) {}

    /**
     * @param campaignState the contract's StratCon campaign state
     *
     * @return every facility held by the player or their employer with no scenario on it
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<CounterattackTarget> getCounterattackTargets(StratConCampaignState campaignState) {
        List<CounterattackTarget> targets = new ArrayList<>();
        for (StratConTrackState track : campaignState.getTracks()) {
            for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
                if (entry.getValue().isOwnerAlliedToPlayer() && (track.getScenario(entry.getKey()) == null)) {
                    targets.add(new CounterattackTarget(track, entry.getKey()));
                }
            }
        }
        return targets;
    }

    /**
     * Tells the player the enemy is attacking one of their side's facilities: in the daily report, and with a warning
     * when there is a GUI.
     *
     * @param campaign the current campaign
     * @param target   the facility under attack
     * @param scenario the attack's scenario
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void announceCounterattack(Campaign campaign, CounterattackTarget target, StratConScenario scenario) {
        StratConFacility facility = target.track().getFacility(target.coords());
        String facilityName = (facility == null) ? "" : facility.getDisplayableName();
        String deadline = StratConFacilityAdvisor.formatDate(scenario.getDeploymentDate());
        boolean isEmployerHeld = (facility != null) && (facility.getOwner() == ForceAlignment.Allied);

        campaign.addReport(BATTLE, getFormattedTextAt(RESOURCE_BUNDLE,
              "report.counterattack",
              facilityName,
              target.track().getDisplayableName(),
              target.coords().toBTString(),
              deadline));

        if (campaign.getGUI() == null) {
            return;
        }

        new ImmersiveDialogSimple(campaign,
              null,
              getFormattedTextAt(RESOURCE_BUNDLE,
                    isEmployerHeld ? "warning.message.employer" : "warning.message.player",
                    facilityName,
                    target.track().getDisplayableName(),
                    deadline),
              List.of(getTextAt(RESOURCE_BUNDLE, "warning.button")),
              getTextAt(RESOURCE_BUNDLE, "warning.ooc"));
    }

    /**
     * Settles a counterattack the player lost outright: the facility goes to the enemy.
     *
     * @param campaign the current campaign
     * @param track    the facility's sector
     * @param coords   the facility's hex
     * @param facility the facility, if it still stands
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void resolveLostCounterattack(Campaign campaign, StratConTrackState track, StratConCoords coords,
          StratConFacility facility) {
        if (!facility.isOwnerAlliedToPlayer()) {
            return;
        }

        StratConRulesManager.switchFacilityOwner(facility);
        campaign.addReport(BATTLE, getFormattedTextAt(RESOURCE_BUNDLE,
              "report.lost",
              facility.getDisplayableName(),
              track.getDisplayableName(),
              coords.toBTString()));
    }

    /**
     * Settles a counterattack nobody deployed against. A facility the player holds is lost. One the employer holds is
     * left to an off-screen roll (see {@link #isOffScreenDefenseHeld}); it loses a garrison step whatever the result.
     *
     * @param track         the facility's sector
     * @param facility      the facility
     * @param campaignState the contract's StratCon campaign state
     *
     * @return {@code true} if the facility was lost
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean resolveIgnoredCounterattack(StratConTrackState track, StratConFacility facility,
          StratConCampaignState campaignState) {
        if (!facility.isOwnerAlliedToPlayer()) {
            return false;
        }

        boolean isHeld = false;
        AbstractContract contract = campaignState.getContract();
        if ((facility.getOwner() == ForceAlignment.Allied) && (contract != null)) {
            isHeld = isOffScreenDefenseHeld(facility,
                  contract.getEmployerForceSkill(),
                  contract.getEnemyForceSkill(),
                  contract.getMoraleLevel(),
                  d6(2));
            facility.setGarrison(facility.getGarrison() - 1);
        }

        if (!isHeld) {
            StratConRulesManager.switchFacilityOwner(facility);
        }
        LOGGER.info("Ignored counterattack on {} in sector {}: {}.",
              facility.getDisplayableName(),
              track.getDisplayableName(),
              isHeld ? "the employer held" : "lost");
        return !isHeld;
    }

    /**
     * Reports how an ignored counterattack went, once it has been settled (see {@link #resolveIgnoredCounterattack}).
     *
     * @param campaign the current campaign
     * @param track    the facility's sector
     * @param coords   the facility's hex
     * @param facility the facility
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void reportIgnoredCounterattack(Campaign campaign, StratConTrackState track, StratConCoords coords,
          StratConFacility facility) {
        campaign.addReport(BATTLE, getFormattedTextAt(RESOURCE_BUNDLE,
              facility.isOwnerAlliedToPlayer() ? "report.ignoredHeld" : "report.ignoredLost",
              facility.getDisplayableName(),
              track.getDisplayableName(),
              coords.toBTString()));
    }

    /**
     * @return the off-screen defence modifier from the facility's garrison: better for veterans, worse for poor morale
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int getTraitModifier(StratConFacility facility) {
        if (facility.hasTrait(FacilityTrait.VETERAN_GARRISON)) {
            return FacilityTrait.OFF_SCREEN_DEFENSE_MODIFIER;
        }
        return facility.hasTrait(FacilityTrait.POOR_MORALE) ? -FacilityTrait.OFF_SCREEN_DEFENSE_MODIFIER : 0;
    }

    /**
     * Rolls an employer's defense of their facility against a counterattack the player left to them. The roll is 2d6,
     * plus the facility's garrison and tier (1 for an Outpost up to 3 for a Stronghold), plus the employer's skill,
     * minus the enemy's skill and morale, against {@link #OFF_SCREEN_DEFENSE_TARGET}. Skills count from Regular, and
     * morale from Stalemate.
     *
     * @param facility      the defended facility
     * @param employerSkill the employer's force skill
     * @param enemySkill    the enemy's force skill
     * @param enemyMorale   the enemy's morale
     * @param roll          the 2d6 roll
     *
     * @return {@code true} if the employer held
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isOffScreenDefenseHeld(StratConFacility facility, @Nullable SkillLevel employerSkill,
          @Nullable SkillLevel enemySkill, @Nullable ContractMoraleLevel enemyMorale, int roll) {
        int modifiedRoll = roll
                                 + facility.getGarrison()
                                 + facility.getTier().ordinal() + 1
                                 + getSkillModifier(employerSkill)
                                 - getSkillModifier(enemySkill)
                                 - ((enemyMorale == null) ? 0 : enemyMorale.getLevel())
                                 + getTraitModifier(facility);
        return modifiedRoll >= OFF_SCREEN_DEFENSE_TARGET;
    }

    private static int getSkillModifier(@Nullable SkillLevel skillLevel) {
        return (skillLevel == null) ? 0 : (skillLevel.ordinal() - SkillLevel.REGULAR.ordinal());
    }

    /**
     * The contract-level daily step for enemy engineers: places those due today, each in a random sector with room.
     * One that finds no room is dropped. None are placed while Facility Operations is off, or while a Garrison Duty or
     * Retainer contract is peaceful, between raids; those due then are simply dropped.
     *
     * <p>A contract whose engineers were never scheduled - one from before they were scheduled up front - is scheduled
     * from today (see {@link #scheduleEnemyEngineers}).</p>
     *
     * @param campaign      the current campaign
     * @param contract      the contract
     * @param campaignState the contract's StratCon campaign state
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void processEnemyEngineers(Campaign campaign, AbstractContract contract,
          StratConCampaignState campaignState) {
        LocalDate today = campaign.getLocalDate();
        LocalDate scheduledUntil = campaignState.getEnemyEngineersScheduledUntil();
        if ((scheduledUntil == null) || !today.isBefore(scheduledUntil)) {
            scheduleEnemyEngineers(contract, campaignState, today);
        }

        int dueCount = StratConScenarioTempo.takeDueScenarioDates(campaignState.getEnemyEngineerDates(), today);
        if ((dueCount == 0) || !StratConFacilityOperations.isEnabled(campaign) || contract.isPeaceful()) {
            return;
        }

        List<StratConPointOfInterest> placed = new ArrayList<>();
        for (int engineer = 0; engineer < dueCount; engineer++) {
            StratConPointOfInterest pointOfInterest = placeEnemyEngineers(campaignState, today);
            if (pointOfInterest == null) {
                LOGGER.info("No sector of contract {} had room for enemy engineers.", contract.getName());
            } else {
                placed.add(pointOfInterest);
            }
        }
        StratConPointOfInterestRules.announceNewPointsOfInterest(campaign, contract, placed);
    }

    /**
     * Schedules a contract's enemy engineers from the given day to the contract's end, replacing any scheduled from
     * that
     * day on: {@value StratConSpawnRates#ENGINEERS_PER_SCALE} a month per point of Scale, each on a random day within
     * its month. Does nothing without a settled start date.
     *
     * @param contract      the contract
     * @param campaignState the contract's StratCon campaign state
     * @param fromDate      the first day to schedule on; the contract's start date when it is accepted
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void scheduleEnemyEngineers(AbstractContract contract, StratConCampaignState campaignState,
          @Nullable LocalDate fromDate) {
        LocalDate scheduledUntil = scheduleMonthlyDates(contract,
              campaignState,
              campaignState.getEnemyEngineerDates(),
              StratConSpawnRates.ENGINEERS_PER_SCALE,
              fromDate);
        if (scheduledUntil != null) {
            campaignState.setEnemyEngineersScheduledUntil(scheduledUntil);
        }
    }

    /**
     * Lays out a contract's dates for something that comes a set number of times a month per point of Scale (see
     * {@link StratConSpawnRates}), each on a random day within its month, from the given day to the contract's end. Any
     * dates already in the list from that day on are replaced; earlier ones are left alone. A date on or after the
     * contract's last day is moved to the day before.
     *
     * @param contract      the contract
     * @param campaignState the contract's StratCon campaign state, for how its Scale is counted (see
     *                      {@link StratConSpawnRates#isFactorSupportPointsIntoScale(StratConCampaignState)})
     * @param dates         the list to schedule into
     * @param ratePerScale  how many come each month per point of Scale
     * @param fromDate     the first day to schedule on, or {@code null} for the contract's start
     *
     * @return the day the schedule runs to - the contract's end - or {@code null} if the contract has no settled start
     *       date and nothing was scheduled
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable LocalDate scheduleMonthlyDates(AbstractContract contract, StratConCampaignState campaignState,
          List<LocalDate> dates, double ratePerScale, @Nullable LocalDate fromDate) {
        LocalDate startDate = contract.getStartDate();
        if (startDate == null) {
            return null;
        }

        LocalDate firstDate = ((fromDate == null) || fromDate.isBefore(startDate)) ? startDate : fromDate;
        LocalDate endDate = contract.getEndingDate();
        if (endDate == null) {
            endDate = startDate.plusMonths(Math.max(1, contract.getLengthInMonths()));
        }

        dates.removeIf(date -> !date.isBefore(firstDate));
        List<Integer> schedule = StratConSpawnRates.getMonthlySchedule(contract,
              StratConSpawnRates.isFactorSupportPointsIntoScale(campaignState),
              ratePerScale);
        dates.addAll(StratConContractInitializer.layOutSpawnDates(startDate, endDate, schedule,
              contract.getLengthInMonths(), firstDate));
        return endDate;
    }

    private static @Nullable StratConPointOfInterest placeEnemyEngineers(StratConCampaignState campaignState,
          LocalDate today) {
        List<StratConTrackState> tracks = new ArrayList<>(campaignState.getTracks());
        while (!tracks.isEmpty()) {
            StratConTrackState track = tracks.remove(Compute.randomInt(tracks.size()));
            // Engineers only go where their outpost would fit under the sector's facility cap.
            if (!StratConContractInitializer.hasRoomForFacility(track)) {
                continue;
            }
            StratConPointOfInterest pointOfInterest = StratConPointOfInterestPlacer.place(track,
                  StratConEnemyEngineersBehavior.TYPE_ID,
                  null,
                  today);
            if (pointOfInterest != null) {
                return pointOfInterest;
            }
        }
        return null;
    }
}
