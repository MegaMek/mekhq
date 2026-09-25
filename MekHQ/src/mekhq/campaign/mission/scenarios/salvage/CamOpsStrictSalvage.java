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
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.unit.TestUnit;
import mekhq.gui.dialog.camOpsSalvage.SalvageRecoveryConsole;

/**
 * The {@link SalvageSystem#CAM_OPS_STRICT CamOps (Strict)} salvage system: salvage as written in Campaign Operations.
 *
 * <p>This is the baseline for every system that uses salvage operations: salvage formations and techs are assigned
 * before a scenario starts, and recover the wrecks afterward.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class CamOpsStrictSalvage extends AbstractSalvage {
    @Override
    public boolean isUseSalvageOperations() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The assigned salvage teams recover the wrecks in the salvage recovery console. Their techs then risk
     * accidents (if risky salvage is enabled), and spend the time the recovery took.</p>
     *
     * <p>There is nothing to recover if no teams or techs were assigned, or if the player doesn't control the
     * battlefield.</p>
     */
    @Override
    public void resolveScenarioSalvage(Campaign campaign, AbstractContract contract, Scenario scenario,
          boolean hasBattlefieldControl, List<TestUnit> claimedSalvage, List<TestUnit> soldSalvage,
          List<TestUnit> unclaimedSalvage) {
        boolean hasAssignedSalvageFormations = !scenario.getSalvageFormations().isEmpty();
        boolean hasAssignedSalvageTechs = !scenario.getSalvageTechs().isEmpty();
        if (!hasBattlefieldControl || !hasAssignedSalvageFormations || !hasAssignedSalvageTechs) {
            return;
        }

        SalvageRecoveryConsole console = new SalvageRecoveryConsole(campaign, this, contract, scenario,
              claimedSalvage, soldSalvage);

        List<UUID> techUUIDs = scenario.getSalvageTechs();
        if (campaign.getCampaignOptions().get(CampaignOption.IS_USE_RISKY_SALVAGE)) {
            CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, techUUIDs, console.getCountOfSalvageUnits());
        }

        CamOpsSalvageUtilities.depleteTechMinutes(campaign, techUUIDs, console.getUsedSalvageTime());
    }
}
