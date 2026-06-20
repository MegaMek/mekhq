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
package mekhq.gui.campaignOptions.contents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import org.junit.jupiter.api.Test;

/**
 * Exhaustive round-trip test for {@link MarketsOptionsModel}. Every scalar field is mutated automatically, with four
 * exclusions handled explicitly: {@code personnelMarketStyle} (changing it drives a market-replacement branch that
 * needs a live campaign, so it is left at its default) and the four contract-percent doubles (their setters clamp, so
 * they are set to in-range values). The personnel-market removal-target map is asserted explicitly. {@code applyTo}
 * takes a {@link Campaign}, which a bare mock satisfies because neither the style nor the "Campaign Ops" name branch is
 * taken.
 */
class MarketsOptionsModelTest {
    @Test
    void applyToRoundTripsEveryField() {
        Campaign campaign = mock(Campaign.class);
        MarketsOptionsModel model = new MarketsOptionsModel(new CampaignOptions());
        OptionsModelTestSupport.mutateScalarFields(model,
              "personnelMarketStyle",
              "equipmentContractPercent",
              "dropShipContractPercent",
              "jumpShipContractPercent",
              "warShipContractPercent");
        model.equipmentContractPercent = 4.5;
        model.dropShipContractPercent = 0.5;
        model.jumpShipContractPercent = 0.5;
        model.warShipContractPercent = 0.5;
        model.personnelMarketName = "Test Market";
        model.personnelMarketRandomRemovalTargets.put(SkillLevel.GREEN, 5);

        CampaignOptions destination = new CampaignOptions();
        model.applyTo(campaign, destination);
        MarketsOptionsModel roundTripped = new MarketsOptionsModel(destination);

        OptionsModelTestSupport.assertAllFieldsMatch(model, roundTripped, "personnelMarketRandomRemovalTargets");
        assertEquals(model.personnelMarketRandomRemovalTargets.get(SkillLevel.GREEN),
              roundTripped.personnelMarketRandomRemovalTargets.get(SkillLevel.GREEN));
    }
}
