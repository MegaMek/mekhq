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

import mekhq.campaign.campaignOptions.CampaignOptions;
import org.junit.jupiter.api.Test;

/**
 * Exhaustive round-trip test for {@link RelationshipsOptionsModel}. Every field is a scalar (boolean, enum, or int), so
 * {@link OptionsModelTestSupport#mutateScalarFields} mutates all of them and {@link OptionsModelTestSupport#assertAllFieldsMatch}
 * verifies each one survives a save/reload unchanged &mdash; catching any field that loads from or saves to the wrong option.
 */
class RelationshipsOptionsModelTest {
    @Test
    void applyToRoundTripsEveryField() {
        RelationshipsOptionsModel model = new RelationshipsOptionsModel(new CampaignOptions());
        OptionsModelTestSupport.mutateScalarFields(model);

        CampaignOptions destination = new CampaignOptions();
        model.applyTo(destination);
        RelationshipsOptionsModel roundTripped = new RelationshipsOptionsModel(destination);

        OptionsModelTestSupport.assertAllFieldsMatch(model, roundTripped);
    }
}
