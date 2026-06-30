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

import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.AgeGroup;
import mekhq.campaign.universe.Planet;
import org.junit.jupiter.api.Test;

/**
 * Exhaustive round-trip test for {@link BiographyOptionsModel}, whose fields span {@link CampaignOptions} and
 * {@link RandomOriginOptions}. Every scalar field is mutated automatically, plus the per-role portrait array and the
 * specified origin planet (a mock, so {@code applyTo} does not fall back to a {@code Systems} lookup). The random-death
 * age-group map is asserted explicitly. {@code percentFemale} is backed by a process-wide gender generator, so it is
 * left untouched to avoid leaking state into other tests.
 */
class BiographyOptionsModelTest {
    @Test
    void applyToRoundTripsEveryField() {
        CampaignOptions sourceOptions = new CampaignOptions();
        RandomOriginOptions sourceOrigin = sourceOptions.getRandomOriginOptions();
        BiographyOptionsModel model = new BiographyOptionsModel(sourceOptions, sourceOrigin);

        OptionsModelTestSupport.mutateScalarFields(model, "percentFemale");
        model.specifiedPlanet = mock(Planet.class);
        model.usePortraitForRole[0] = !model.usePortraitForRole[0];
        AgeGroup ageGroup = AgeGroup.values()[0];
        boolean ageGroupValue = !Boolean.TRUE.equals(model.enabledRandomDeathAgeGroups.get(ageGroup));
        model.enabledRandomDeathAgeGroups.put(ageGroup, ageGroupValue);

        CampaignOptions destinationOptions = new CampaignOptions();
        RandomOriginOptions destinationOrigin = destinationOptions.getRandomOriginOptions();
        model.applyTo(destinationOptions, destinationOrigin);
        BiographyOptionsModel roundTripped = new BiographyOptionsModel(destinationOptions, destinationOrigin);

        OptionsModelTestSupport.assertAllFieldsMatch(model, roundTripped, "enabledRandomDeathAgeGroups");
        assertEquals(ageGroupValue, roundTripped.enabledRandomDeathAgeGroups.get(ageGroup));
    }
}
