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

import megamek.common.enums.SkillLevel;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.Test;

/**
 * Exhaustive round-trip test for {@link SalariesOptionsModel}. The scalar fields are mutated automatically; the two
 * salary maps (per skill-level XP multiplier and per-role base salary) are mutated and then asserted explicitly, since
 * a full-map comparison would also include default entries the test does not touch.
 */
class SalariesOptionsModelTest {
    @Test
    void applyToRoundTripsEveryField() {
        SalariesOptionsModel model = new SalariesOptionsModel(new CampaignOptions());
        OptionsModelTestSupport.mutateScalarFields(model);
        model.salaryXpMultipliers.put(SkillLevel.GREEN, 9.0);
        model.roleBaseSalaries.put(PersonnelRole.MEKWARRIOR, 12345.0);

        CampaignOptions destination = new CampaignOptions();
        model.applyTo(destination);
        SalariesOptionsModel roundTripped = new SalariesOptionsModel(destination);

        OptionsModelTestSupport.assertAllFieldsMatch(model, roundTripped, "salaryXpMultipliers", "roleBaseSalaries");
        assertEquals(9.0, roundTripped.salaryXpMultipliers.get(SkillLevel.GREEN));
        assertEquals(12345.0, roundTripped.roleBaseSalaries.get(PersonnelRole.MEKWARRIOR));
    }
}
