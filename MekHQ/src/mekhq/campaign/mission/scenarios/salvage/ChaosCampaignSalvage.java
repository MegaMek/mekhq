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
package mekhq.campaign.mission.scenarios.salvage;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.unit.TestUnit;
import mekhq.gui.dialog.camOpsSalvage.SalvageRecoveryConsole;

/**
 * The {@link SalvageSystem#CHAOS_CAMPAIGN Chaos Campaign} salvage system: a simplified version of Campaign Operations
 * salvage.
 *
 * <p>Salvage teams aren't used; the player recovers every wreck automatically. The salvage rights set the player's
 * share of each wreck's value, which they receive in cash unless they buy the unit from the employer (see
 * {@link SalvageSettlement#forSalvagePurchases}).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ChaosCampaignSalvage extends AbstractSalvage {
    @Override
    public boolean isUseSalvageOperations() {
        return false;
    }

    @Override
    public SalvageSettlement createSettlement(AbstractContract contract) {
        return SalvageSettlement.forSalvagePurchases(contract);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the player controls the battlefield, every wreck is recovered, and the salvage recovery console only
     * asks which units the player wants to buy.</p>
     */
    @Override
    public void resolveScenarioSalvage(Campaign campaign, AbstractContract contract, Scenario scenario,
          boolean hasBattlefieldControl, List<TestUnit> claimedSalvage, List<TestUnit> soldSalvage,
          List<TestUnit> unclaimedSalvage) {
        if (hasBattlefieldControl) {
            new SalvageRecoveryConsole(campaign, this, contract, scenario, claimedSalvage, soldSalvage);
        }
    }
}
