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

/**
 * The shared rules of a point of interest that a ground formation follows up, and that a scenario of one set template
 * may break out over.
 *
 * <p>When a ground formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the
 * type decides what happens (see {@link #onNoScenario}). If one does, a scenario of the type's template (see
 * {@link #getScenarioTemplateName}) is placed on the hex and linked to the point of interest: an overall victory
 * secures it (see {@link #securePointOfInterest}), and anything else - a defeat, a draw, or leaving the scenario
 * unplayed - loses it, failing its objective. By default, securing one pays the combat bonus, standing in for the
 * Essential scenarios its contract does not get (see {@link #isCombatBonusPaid}).</p>
 *
 * <p>By default, a formation that is not a ground formation cannot follow the point of interest up (see
 * {@link #canFollowUp}).</p>
 *
 * <p>Each type's player-facing text lives in the {@code StratConRulesManager} resource bundle, under its key prefix
 * (see {@link #getResourceKeyPrefix}): {@code .objective}, {@code .contested.report}, {@code .secured.report}, and
 * {@code .lost.report}, each report taking the point of interest's name and its sector's name. (See
 * {@link AbstractStratConRolledPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public abstract class StratConContestedPointOfInterestBehavior extends AbstractStratConRolledPointOfInterestBehavior {
    /**
     * @return the file name of the scenario template fought over this type of point of interest, such as
     *       {@code Recon Evasion.json}; or {@code null} for the deploying formation to be ambushed instead, in a
     *       template suited to ambushing its unit type
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected abstract @Nullable String getScenarioTemplateName();

    /**
     * By default, only a ground formation can follow the point of interest up (see {@link #isGroundFormation}).
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean canFollowUp(int formationId, Campaign campaign) {
        return isGroundFormation(formationId, campaign);
    }

    @Override
    protected String getScenarioReportKeySuffix() {
        return "contested.report";
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
