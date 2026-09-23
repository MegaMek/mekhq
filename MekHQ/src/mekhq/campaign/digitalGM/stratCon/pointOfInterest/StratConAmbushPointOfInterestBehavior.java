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
import mekhq.gui.dialog.StratConAmbushedDialog;

/**
 * The rules of a point of interest that any formation can act on, but that may hide an ambush. Types that need nothing
 * more are instances of this class, set up by {@link StratConConfiguredPointOfInterestType}; types with rules of their
 * own extend it.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the point of
 * interest is secured on the spot: its objective is met and the contract's combat bonus is paid when its Essential
 * scenarios were replaced (see {@link #isCombatBonusPaid}). If one does, the deploying formation is ambushed (see
 * {@link #getScenarioTemplateName}) - a Crisis, and never a Turning Point. An ambushed point of interest is spent
 * whatever the ambush's result: it leaves the map and its objective is removed, neither met nor failed.</p>
 *
 * <p>Each type's player-facing text lives in the {@code StratConPointOfInterest} resource bundle, under its behavior
 * ID (see {@link #getBehaviorId}): {@code .objective}, {@code .secured.report}, {@code .ambush.report}, and
 * {@code .ambushSpent.report}, each report taking the point of interest's name and its sector's name. (See
 * {@link AbstractStratConRolledPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConAmbushPointOfInterestBehavior extends AbstractStratConRolledPointOfInterestBehavior {
    /**
     * @param behaviorId           the ID this behavior is registered under, which also prefixes its resource keys
     * @param scenarioTemplateName the file name of the scenario template the ambush is fought in, or {@code null} for
     *                             one suited to ambushing the deploying formation's unit type
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConAmbushPointOfInterestBehavior(String behaviorId, @Nullable String scenarioTemplateName) {
        super(behaviorId, scenarioTemplateName);
    }

    /**
     * Whatever template it is drawn from, the scenario here is always an ambush.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isScenarioAnAmbush() {
        return true;
    }

    @Override
    protected String getScenarioReportKeySuffix() {
        return "ambush.report";
    }

    /**
     * With no ambush, the point of interest is secured at once.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onNoScenario(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        securePointOfInterest(pointOfInterest, track, contract, campaign);
    }

    /**
     * The ambush here has ended - won, lost, or left unplayed. The point of interest is spent whatever the result: it
     * leaves the map and its objective is removed.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void onLinkedScenarioEnded(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          boolean isVictory, Campaign campaign) {
        StratConPointOfInterestRules.withdrawPointOfInterest(track, pointOfInterest);
        addReport(GENERAL, "ambushSpent.report", pointOfInterest, track, campaign);
    }

    /**
     * Tells the player an ambush has been sprung here. By default, the ambushed dialog, as for any other ambush.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void announceScenario(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          int formationId, AbstractContract contract, Campaign campaign) {
        new StratConAmbushedDialog(campaign, formationId, false);
    }
}
