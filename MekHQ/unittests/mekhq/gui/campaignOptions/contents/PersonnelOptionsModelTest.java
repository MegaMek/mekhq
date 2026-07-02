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

import static org.mockito.Mockito.mock;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import org.junit.jupiter.api.Test;

/**
 * Exhaustive round-trip test for {@link PersonnelOptionsModel}. Every scalar field is mutated automatically and the
 * {@code awardSetFilterList} string explicitly, then each is verified through a save/reload. {@code applyTo} takes a
 * {@link Campaign} but only touches it when the transient {@code resetTemporaryPrisonerCapacity} flag is set (it never
 * is after construction), so a bare mock suffices and that flag is excluded.
 */
class PersonnelOptionsModelTest {
    @Test
    void applyToRoundTripsEveryField() {
        Campaign campaign = mock(Campaign.class);
        PersonnelOptionsModel model = new PersonnelOptionsModel(new CampaignOptions());
        OptionsModelTestSupport.mutateScalarFields(model, "resetTemporaryPrisonerCapacity");
        model.awardSetFilterList = "Alpha,Beta";

        CampaignOptions destination = new CampaignOptions();
        model.applyTo(campaign, destination);
        PersonnelOptionsModel roundTripped = new PersonnelOptionsModel(destination);

        OptionsModelTestSupport.assertAllFieldsMatch(model, roundTripped, "resetTemporaryPrisonerCapacity");
    }
}
