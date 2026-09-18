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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.commandGeneration.SupportCapability;
import mekhq.campaign.universe.commandGeneration.SupportUnitGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import testUtilities.MHQTestUtilities;

/**
 * Verifies {@link CommandGenerator#grantStandaloneSupportVehicles}: recovery vehicles and MASH trucks are generated
 * standalone when support teams are off, and left to the support sections when they are on, so a command always gets
 * them exactly once (issue #10059).
 */
class CommandGeneratorSupportVehiclesTest {

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    private static Campaign campaignWith(boolean useSupportTeams, boolean useSalvage, boolean useMedical) {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.USE_SUPPORT_TEAMS, useSupportTeams);
        campaign.getCampaignOptions().set(CampaignOption.IS_USE_CAM_OPS_SALVAGE, useSalvage);
        campaign.getCampaignOptions().set(CampaignOption.USE_MASH_THEATRES, useMedical);
        return campaign;
    }

    @Test
    void supportTeamsOff_salvageOn_generatesRecoveryVehicles() {
        Campaign campaign = campaignWith(false, true, false);

        try (MockedStatic<SupportUnitGenerator> generator = mockStatic(SupportUnitGenerator.class)) {
            CommandGenerator.grantStandaloneSupportVehicles(campaign);

            generator.verify(() -> SupportUnitGenerator.generate(eq(SupportCapability.SALVAGE), eq(campaign),
                  any(), eq(true)));
            generator.verify(() -> SupportUnitGenerator.generate(eq(SupportCapability.MEDICAL), any(), any(),
                  any(Boolean.class)), never());
        }
    }

    @Test
    void supportTeamsOff_medicalOn_generatesMashTrucks() {
        Campaign campaign = campaignWith(false, false, true);

        try (MockedStatic<SupportUnitGenerator> generator = mockStatic(SupportUnitGenerator.class)) {
            CommandGenerator.grantStandaloneSupportVehicles(campaign);

            generator.verify(() -> SupportUnitGenerator.generate(eq(SupportCapability.MEDICAL), eq(campaign),
                  any(), eq(true)));
            generator.verify(() -> SupportUnitGenerator.generate(eq(SupportCapability.SALVAGE), any(), any(),
                  any(Boolean.class)), never());
        }
    }

    @Test
    void supportTeamsOn_leavesRecoveryVehiclesAndMashTrucksToTheSections() {
        // The sections build and crew these from the generated staff; granting them here too would double them.
        Campaign campaign = campaignWith(true, true, true);

        try (MockedStatic<SupportUnitGenerator> generator = mockStatic(SupportUnitGenerator.class)) {
            CommandGenerator.grantStandaloneSupportVehicles(campaign);

            generator.verify(() -> SupportUnitGenerator.generate(eq(SupportCapability.SALVAGE), any(), any(),
                  any(Boolean.class)), never());
            generator.verify(() -> SupportUnitGenerator.generate(eq(SupportCapability.MEDICAL), any(), any(),
                  any(Boolean.class)), never());
        }
    }

    @Test
    void supportTeamsOff_capabilitiesOff_generatesNeither() {
        Campaign campaign = campaignWith(false, false, false);

        try (MockedStatic<SupportUnitGenerator> generator = mockStatic(SupportUnitGenerator.class)) {
            CommandGenerator.grantStandaloneSupportVehicles(campaign);

            generator.verify(() -> SupportUnitGenerator.generate(eq(SupportCapability.SALVAGE), any(), any(),
                  any(Boolean.class)), never());
            generator.verify(() -> SupportUnitGenerator.generate(eq(SupportCapability.MEDICAL), any(), any(),
                  any(Boolean.class)), never());
        }
    }
}
