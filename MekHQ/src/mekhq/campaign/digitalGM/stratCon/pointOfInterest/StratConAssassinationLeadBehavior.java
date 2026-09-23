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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static java.lang.Math.max;
import static mekhq.campaign.enums.DailyReportType.GENERAL;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of a lead on an assassination target, placed in place of every point of interest on an Assassination
 * contract. It works much as a Mole Hunting contract's potential lead does (see
 * {@link StratConConfiguredPointOfInterestType#POTENTIAL_LEAD}), sharing the contested rules (see
 * {@link StratConContestedPointOfInterestBehavior}), but no lead is ever a dud - every one is fought as a
 * {@value #SCENARIO_TEMPLATE} scenario - and most of the targets they turn up are body doubles.
 *
 * <p>Only one lead per point of the contract's scale points to the real target, settled when the contract is accepted
 * (see {@link #REAL_TARGET_STATE_KEY}); the player cannot tell which, as every lead looks alike and is a strategic
 * objective. No lead is a dud, so following one up always brings a fight; that the fight happened gives nothing
 * away.</p>
 *
 * <p>Winning the fight against the real target meets its objective and pays the combat bonus; losing lets the target
 * escape, failing it. Winning against a body double also pays the combat bonus, but only then is the double revealed,
 * and its objective is withdrawn - neither met nor failed, as it was never the target. Losing to a double pays nothing,
 * and its objective is withdrawn all the same. A lead of either kind not followed up in time fails its objective, as
 * the player cannot know which it was.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConAssassinationLeadBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the assassination lead definition names. */
    public static final String BEHAVIOR_ID = "assassinationLead";

    /** The type ID of the assassination lead definition. */
    public static final String TYPE_ID = "AssassinationLead";

    /** The state key marking a lead that points to the real target, rather than a body double. */
    public static final String REAL_TARGET_STATE_KEY = "realTarget";

    /** The scenario template fought over a lead that pans out. */
    static final String SCENARIO_TEMPLATE = "Assassination.json";

    /**
     * Every lead is fought as a {@value #SCENARIO_TEMPLATE} scenario.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConAssassinationLeadBehavior() {
        super(BEHAVIOR_ID, SCENARIO_TEMPLATE, NoScenarioOutcome.WITHDRAW);
    }

    /**
     * Settles, when the contract is accepted, which leads point to the real target: one per point of the contract's
     * scale, or all of them if there are fewer. The rest turn up body doubles.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void onScheduled(List<StratConScheduledPointOfInterest> scheduledPointsOfInterest,
          AbstractContract contract) {
        StratConScheduledPointOfInterest.markAtRandom(scheduledPointsOfInterest,
              max(1, contract.getScale()),
              REAL_TARGET_STATE_KEY);
    }

    /**
     * @param pointOfInterest an assassination lead
     *
     * @return {@code true} if the lead points to the real target rather than a body double
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isRealTarget(StratConPointOfInterest pointOfInterest) {
        return Boolean.parseBoolean(pointOfInterest.getStateValue(REAL_TARGET_STATE_KEY));
    }

    /**
     * No lead is a dud: the real target and its body doubles alike always stand and fight.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isScenarioCertain(StratConPointOfInterest pointOfInterest) {
        return true;
    }

    /**
     * Never reached, since every lead fights (see {@link #isScenarioCertain}). Should it be, the lead has gone cold: it
     * leaves the map and its objective is withdrawn, giving away nothing about which kind it was.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onNoScenario(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        StratConPointOfInterestRules.withdrawPointOfInterest(track, pointOfInterest);
    }

    /**
     * The real target is secured or lost as any contested point of interest is. A body double is revealed only now: it
     * is withdrawn whatever the result, and beating it still pays the combat bonus.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void onLinkedScenarioEnded(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          boolean isVictory, Campaign campaign) {
        if (isRealTarget(pointOfInterest)) {
            super.onLinkedScenarioEnded(pointOfInterest, track, isVictory, campaign);
            return;
        }

        StratConPointOfInterestRules.withdrawPointOfInterest(track, pointOfInterest);
        if (!isVictory) {
            addReport(GENERAL, "doubleEscaped.report", pointOfInterest, track, campaign);
            return;
        }

        addReport(GENERAL, "bodyDouble.report", pointOfInterest, track, campaign);
        AbstractContract contract = StratConPointOfInterestRules.getContract(track, campaign);
        if ((contract != null) && isCombatBonusPaid(contract)) {
            StratConRulesManager.awardCombatBonus(campaign, contract);
        }
    }
}
