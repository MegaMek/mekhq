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

import static mekhq.campaign.enums.DailyReportType.GENERAL;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The rules of a point of interest that any formation follows up, and that a scenario of one set template may break
 * out over. Types that need nothing more are instances of this class, set up by
 * {@link StratConConfiguredPointOfInterestType}; types with rules of their own extend it.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the type's
 * {@link NoScenarioOutcome} decides what happens (see {@link #onNoScenario}). If one does, a scenario of the type's
 * template (see {@link #getScenarioTemplateName}) is placed on the hex and linked to the point of interest: an overall
 * victory secures it (see {@link #securePointOfInterest}), and anything else - a defeat, a draw, or leaving the
 * scenario unplayed - loses it, failing its objective. Securing one pays the combat bonus when the contract's Essential
 * scenarios were replaced (see {@link #isCombatBonusPaid}).</p>
 *
 * <p>Each type's player-facing text lives in the {@code StratConPointOfInterest} resource bundle, under its behavior
 * ID (see {@link #getBehaviorId}): {@code .objective}, {@code .contested.report}, {@code .secured.report}, and
 * {@code .lost.report} - and {@code .withdrawn.report} for a type that withdraws when no scenario breaks out - each
 * report taking the point of interest's name and its sector's name. (See
 * {@link AbstractStratConRolledPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConContestedPointOfInterestBehavior extends AbstractStratConRolledPointOfInterestBehavior {
    /**
     * What becomes of a contested point of interest when a formation follows it up and no scenario breaks out.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public enum NoScenarioOutcome {
        /** It is secured on the spot, as if the scenario had been won. */
        SECURE,
        /** It comes to nothing - a dud: it leaves the map and its objective is removed, neither met nor failed. */
        WITHDRAW
    }

    private final NoScenarioOutcome noScenarioOutcome;

    /**
     * @param behaviorId           the ID this behavior is registered under, which also prefixes its resource keys
     * @param scenarioTemplateName the file name of the scenario template fought here, or {@code null} for the
     *                             deploying formation to be ambushed in a template suited to its unit type
     * @param noScenarioOutcome    what happens when no scenario breaks out
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConContestedPointOfInterestBehavior(String behaviorId, @Nullable String scenarioTemplateName,
          NoScenarioOutcome noScenarioOutcome) {
        super(behaviorId, scenarioTemplateName);
        this.noScenarioOutcome = noScenarioOutcome;
    }

    @Override
    protected String getScenarioReportKeySuffix() {
        return "contested.report";
    }

    /**
     * With no scenario breaking out, the point of interest is secured or withdrawn, as its
     * {@link NoScenarioOutcome} says.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onNoScenario(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        if (noScenarioOutcome == NoScenarioOutcome.SECURE) {
            securePointOfInterest(pointOfInterest, track, contract, campaign);
            return;
        }

        StratConPointOfInterestRules.withdrawPointOfInterest(track, pointOfInterest);
        addReport(GENERAL, "withdrawn.report", pointOfInterest, track, campaign);
    }

    /**
     * The scenario over the point of interest has ended: an overall victory secures it, and anything else loses it,
     * failing its objective.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void onLinkedScenarioEnded(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          boolean isVictory, Campaign campaign) {
        if (isVictory) {
            securePointOfInterest(pointOfInterest,
                  track,
                  StratConPointOfInterestRules.getContract(track, campaign),
                  campaign);
            return;
        }

        StratConPointOfInterestRules.losePointOfInterest(track, pointOfInterest);
        addReport(GENERAL, "lost.report", pointOfInterest, track, campaign);
    }
}
