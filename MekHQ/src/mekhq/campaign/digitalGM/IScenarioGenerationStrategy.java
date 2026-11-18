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
package mekhq.campaign.digitalGM;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.gm.AbstractStratConGM;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * Strategy for deciding <i>when</i> new scenarios appear and <i>generating</i> them. This is the seam that most sharply
 * distinguishes the StratCon play types: Normal schedules a weekly batch, Singles caps the campaign to one drop per
 * week, and other GMs may pace generation entirely differently.
 *
 * <p>The method signatures mirror the corresponding static entry points on
 * {@link mekhq.campaign.digitalGM.stratCon.StratConRulesManager StratConRulesManager}; the default StratCon
 * implementation delegates to them, so the rules themselves are unchanged &mdash; only the daily loop that invokes them
 * now lives in {@link AbstractStratConGM AbstractStratConGM}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface IScenarioGenerationStrategy {

    /**
     * Makes the week's scenario scheduling for a contract, on a Monday: in Single Drop play, its one scenario of the
     * week; otherwise, the week's normal scenario rolls against each sector's threat.
     *
     * @param campaign       the active campaign
     * @param campaignState  the StratCon state for the contract
     * @param contract       the contract
     * @param singleDropMode {@code true} to schedule Single Drop play's one scenario for the week
     */
    void generateWeeklyScenarioDates(Campaign campaign, StratConCampaignState campaignState, AbstractContract contract,
          boolean singleDropMode);

    /**
     * Generates the normal scenarios due today, one for each entry in {@code scenarioSectors}.
     *
     * @param campaign        the active campaign
     * @param campaignState   the StratCon state for the contract
     * @param contract        the contract
     * @param scenarioSectors the sector each scenario appears in, one entry per scenario; a {@code null} entry for a
     *                        random sector
     */
    void generateDailyScenarios(Campaign campaign, StratConCampaignState campaignState, AbstractContract contract,
          List<StratConTrackState> scenarioSectors);
}
