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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
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
 * What the enemy does with facilities between the player's fights: monthly upkeep of its own facilities,
 * counterattacks on facilities the player or their employer holds, and enemy engineers building new outposts.
 *
 * <ul>
 *     <li><b>Monthly upkeep.</b> At the start of each month, every enemy facility repairs one condition step and
 *     reinforces one garrison step, unless a fight is under way there, it is cut off from its supply lines, or it is
 *     under siege.</li>
 *     <li><b>Counterattacks.</b> When one of the contract's ordinary scenarios comes due, it may instead become a
 *     counterattack on a facility held by the player or their employer. A counterattack is a Crisis, placed on the
 *     facility's hex with a deployment deadline of 3 to 7 days. Losing or ignoring it hands the facility to the enemy,
 *     except that one held by the employer is left to an off-screen roll when ignored. If a month passes without a
 *     counterattack, the next ordinary scenario is always one, if there is anything to counterattack.</li>
 *     <li><b>Enemy engineers.</b> Once a month per 3 points of the contract's scale (per 9 when support points are
 *     factored into scale), enemy engineers appear on a random day as a point of interest. If not dealt with before
 *     they leave, they become an enemy outpost (see {@link StratConEnemyEngineersBehavior}).</li>
 * </ul>
 *
 * <p>The "Enemy Facility Activity" campaign option scales all three; at 0, the enemy does none of them.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConEnemyFacilityActivity {
    private static final MMLogger LOGGER = MMLogger.create(StratConEnemyFacilityActivity.class);

    static final String RESOURCE_BUNDLE = "mekhq.resources.StratConEnemyFacilityActivity";

    /** The highest value the Enemy Facility Activity option takes. */
    static final double MAXIMUM_ACTIVITY = 3.0;

    /** The percentage chance, at an activity of 1, that a due ordinary scenario becomes a counterattack. */
    static final int COUNTERATTACK_CHANCE_PERCENT = 25;

    /** How many days, at an activity of 1, can pass before the next due ordinary scenario is always a counterattack. */
    static final int COUNTERATTACK_GUARANTEE_DAYS = 30;

    /** The fewest days a counterattack gives the player to deploy. */
    static final int COUNTERATTACK_MINIMUM_WARNING_DAYS = 3;

    /** The most days a counterattack gives the player to deploy. */
    static final int COUNTERATTACK_MAXIMUM_WARNING_DAYS = 7;

    /** The 2d6 roll, after modifiers, an employer's garrison must meet to hold off an ignored counterattack. */
    static final int OFF_SCREEN_DEFENSE_TARGET = 9;

    /** How many points of scale bring one enemy engineers point of interest a month. */
    static final int ENGINEER_SCALE_DIVISOR = 3;

    /** How many points of scale bring one a month when support points are factored into scale. */
    static final int ENGINEER_SUPPORT_POINT_SCALE_DIVISOR = 9;

    private StratConEnemyFacilityActivity() {}

    /**
     * @param campaign the current campaign
     *
     * @return the "Enemy Facility Activity" option, kept between 0 and {@link #MAXIMUM_ACTIVITY}; 0 with Facility
     *       Operations off, so turning that off restores play as it was before, and 0 in mapless play, where there are
     *       no facilities
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getActivity(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (!StratConFacilityOperations.isEnabled(campaign)) {
            return 0;
        }

        Double activity = campaignOptions.get(CampaignOption.ENEMY_FACILITY_ACTIVITY);
        if (activity == null) {
            return 1.0;
        }
        return Math.max(0, Math.min(MAXIMUM_ACTIVITY, activity));
    }

    /**
     * Works out how many steps of upkeep an enemy facility gets this month. The whole part of the activity is always
     * given; its fraction is the percentage chance of one more step.
     *
     * @param activity   the Enemy Facility Activity option
     * @param percentile a roll from 0 to 99
     *
     * @return the number of steps
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int getUpkeepSteps(double activity, int percentile) {
        int wholeSteps = (int) Math.floor(activity);
        int fractionPercent = (int) Math.round((activity - wholeSteps) * 100);
        return wholeSteps + ((percentile < fractionPercent) ? 1 : 0);
    }

    /**
     * Gives every enemy facility in a sector its monthly upkeep: for each step (see {@link #getUpkeepSteps}), one
     * condition step repaired and one garrison step reinforced. A facility being fought over gets none. Each facility
     * the player can see that changed is reported.
     *
     * @param track    the sector
     * @param campaign the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void applyMonthlyUpkeep(StratConTrackState track, Campaign campaign) {
        double activity = getActivity(campaign);
        if (activity <= 0) {
            return;
        }

        for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
            StratConFacility facility = entry.getValue();
            // A facility being fought over, cut off from its supply lines, or under siege cannot be repaired.
            if ((facility.getOwner() != ForceAlignment.Opposing)
                      || (track.getScenario(entry.getKey()) != null)
                      || StratConFacilitySupply.isCutOff(track, entry.getKey())
                      || StratConFacilitySiege.isBesieged(track, entry.getKey())) {
                continue;
            }

            boolean isChanged = applyUpkeep(facility, getUpkeepSteps(activity, Compute.randomInt(100)));
            if (isChanged && facility.getVisible()) {
                campaign.addReport(BATTLE, getFormattedTextAt(RESOURCE_BUNDLE,
                      "report.upkeep",
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
     * Decides whether a due ordinary scenario becomes a counterattack.
     *
     * @param activity             the Enemy Facility Activity option
     * @param daysSinceLastAttack  days since the last counterattack, or since the contract began if there has been none
     * @param percentile           a roll from 0 to 99
     *
     * @return {@code true} if it does; never at an activity of 0
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isCounterattack(double activity, long daysSinceLastAttack, int percentile) {
        if (activity <= 0) {
            return false;
        }

        if (daysSinceLastAttack >= Math.round(COUNTERATTACK_GUARANTEE_DAYS / activity)) {
            return true;
        }
        return percentile < Math.round(COUNTERATTACK_CHANCE_PERCENT * activity);
    }

    /**
     * Rolls whether the enemy sends a relief force against a new siege: a counterattack's daily chance, scaled by
     * activity, without the guarantee a long quiet spell gives a counterattack.
     *
     * @param activity   the "Enemy Facility Activity" option (see {@link #getActivity})
     * @param percentile a roll from 0 to 99
     *
     * @return {@code true} if a relief force comes
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isReliefSent(double activity, int percentile) {
        return (activity > 0) && (percentile < Math.round(COUNTERATTACK_CHANCE_PERCENT * activity));
    }

    /**
     * Turns as many of the day's due ordinary scenarios into counterattacks as the rules call for (see
     * {@link #isCounterattack}), each on a different facility held by the player or their employer. Each counterattack
     * takes the place of one ordinary scenario, so the contract's pace is unchanged.
     *
     * @param campaign      the current campaign
     * @param contract      the contract
     * @param campaignState the contract's StratCon campaign state
     * @param scenarioCount how many ordinary scenarios are due today
     *
     * @return how many of them became counterattacks, and so should not be generated as ordinary scenarios
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int launchCounterattacks(Campaign campaign, AbstractContract contract,
          StratConCampaignState campaignState, int scenarioCount) {
        double activity = getActivity(campaign);
        if (activity <= 0) {
            return 0;
        }

        LocalDate today = campaign.getLocalDate();
        // The quiet spell that guarantees a counterattack counts from the first day counterattacks could come - not
        // from the contract's start, which for a save loaded partway through would make the first one certain.
        if (campaignState.getLastCounterattackDate() == null) {
            campaignState.setLastCounterattackDate(today);
        }

        LocalDate contractEnd = contract.getEndingDate();
        int launchedCount = 0;
        for (int scenarioIndex = 0; scenarioIndex < scenarioCount; scenarioIndex++) {
            if (!isCounterattack(activity, getDaysSinceLastCounterattack(contract, campaignState, today),
                  Compute.randomInt(100))) {
                continue;
            }

            CounterattackTarget target = pickCounterattackTarget(campaignState);
            if (target == null) {
                break;
            }

            int warningDays = COUNTERATTACK_MINIMUM_WARNING_DAYS
                                    + Compute.randomInt(COUNTERATTACK_MAXIMUM_WARNING_DAYS
                                                              - COUNTERATTACK_MINIMUM_WARNING_DAYS + 1);
            // Like any scenario, a counterattack must fall before the contract ends.
            if ((contractEnd != null) && !today.plusDays(warningDays).isBefore(contractEnd)) {
                continue;
            }
            StratConScenario scenario = StratConRulesManager.startCounterattackScenario(campaign,
                  contract,
                  target.track(),
                  target.coords(),
                  warningDays);
            if (scenario == null) {
                LOGGER.warn("Could not generate a counterattack on {} in sector {}.",
                      target.coords(),
                      target.track().getDisplayableName());
                continue;
            }

            campaignState.setLastCounterattackDate(today);
            launchedCount++;
            announceCounterattack(campaign, target, scenario);
        }
        return launchedCount;
    }

    static long getDaysSinceLastCounterattack(AbstractContract contract, StratConCampaignState campaignState,
          LocalDate today) {
        LocalDate lastAttackDate = campaignState.getLastCounterattackDate();
        if (lastAttackDate == null) {
            lastAttackDate = contract.getStartDate();
        }
        if (lastAttackDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(lastAttackDate, today);
    }

    /**
     * A facility an enemy counterattack can fall on.
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

    private static @Nullable CounterattackTarget pickCounterattackTarget(StratConCampaignState campaignState) {
        List<CounterattackTarget> targets = getCounterattackTargets(campaignState);
        if (targets.isEmpty()) {
            return null;
        }
        return targets.get(Compute.randomInt(targets.size()));
    }

    private static void announceCounterattack(Campaign campaign, CounterattackTarget target,
          StratConScenario scenario) {
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
     * @param scale                          the contract's scale
     * @param isFactorSupportPointsIntoScale whether support points are factored into scale
     * @param activity                       the Enemy Facility Activity option
     *
     * @return how many enemy engineers appear each month
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int getMonthlyEngineerCount(int scale, boolean isFactorSupportPointsIntoScale, double activity) {
        int divisor = isFactorSupportPointsIntoScale ? ENGINEER_SUPPORT_POINT_SCALE_DIVISOR : ENGINEER_SCALE_DIVISOR;
        return (int) Math.floor((Math.max(0, scale) * activity) / divisor);
    }

    /**
     * The contract-level daily step: schedules the next month's enemy engineers once the last month's schedule runs
     * out, and places those due today on a random sector with room. One that finds no room is dropped.
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
        // At no activity the enemy builds nothing, including engineers scheduled before the option was turned down.
        if (getActivity(campaign) <= 0) {
            campaignState.getEnemyEngineerDates().clear();
            return;
        }

        LocalDate today = campaign.getLocalDate();
        LocalDate scheduledUntil = campaignState.getEnemyEngineersScheduledUntil();
        if ((scheduledUntil == null) || !today.isBefore(scheduledUntil)) {
            scheduleEnemyEngineers(campaign, contract, campaignState, today);
        }

        List<LocalDate> engineerDates = campaignState.getEnemyEngineerDates();
        int dueCount = 0;
        for (LocalDate engineerDate : engineerDates) {
            if (!engineerDate.isAfter(today)) {
                dueCount++;
            }
        }
        if (dueCount == 0) {
            return;
        }
        engineerDates.removeIf(engineerDate -> !engineerDate.isAfter(today));

        // The enemy is in no state to expand while routed.
        if (contract.getMoraleLevel().isRouted()) {
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

    private static void scheduleEnemyEngineers(Campaign campaign, AbstractContract contract,
          StratConCampaignState campaignState, LocalDate today) {
        LocalDate monthEnd = today.plusMonths(1);
        campaignState.setEnemyEngineersScheduledUntil(monthEnd);

        LocalDate contractEnd = contract.getEndingDate();
        if ((contractEnd != null) && contractEnd.isBefore(monthEnd)) {
            monthEnd = contractEnd;
        }
        int dayCount = (int) ChronoUnit.DAYS.between(today, monthEnd);
        if (dayCount <= 0) {
            return;
        }

        int engineerCount = getMonthlyEngineerCount(contract.getScale(),
              campaign.getCampaignOptions().get(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION),
              getActivity(campaign));
        for (int engineer = 0; engineer < engineerCount; engineer++) {
            campaignState.getEnemyEngineerDates().add(today.plusDays(Compute.randomInt(dayCount)));
        }
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
