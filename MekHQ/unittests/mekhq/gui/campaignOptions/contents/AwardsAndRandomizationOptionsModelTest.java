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

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import org.junit.jupiter.api.Test;

/**
 * Exhaustive round-trip test for {@link AwardsAndRandomizationOptionsModel}, whose fields live partly on {@link CampaignOptions}
 * and partly on {@link RandomSkillPreferences}. Every scalar field plus the phenotype and experience-level bonus arrays
 * are mutated and verified through a save/reload. The {@code recruitmentBonuses} map is written through an accumulating
 * setter, so it is asserted explicitly for a single role rather than by full-map equality.
 */
class AwardsAndRandomizationOptionsModelTest {
    @Test
    void applyToRoundTripsEveryField() {
        AwardsAndRandomizationOptionsModel model =
              new AwardsAndRandomizationOptionsModel(new CampaignOptions(), new RandomSkillPreferences());
        OptionsModelTestSupport.mutateScalarFields(model);
        model.phenotypeProbabilities[0] += 1;
        model.specialAbilityBonus[0] += 1;
        model.commandSkillsModifier[0] += 1;
        model.utilitySkillsModifier[0] += 1;
        model.recruitmentBonuses.put(PersonnelRole.MEKWARRIOR, 9);

        CampaignOptions destinationOptions = new CampaignOptions();
        RandomSkillPreferences destinationPreferences = new RandomSkillPreferences();
        model.applyTo(destinationOptions, destinationPreferences);
        AwardsAndRandomizationOptionsModel roundTripped =
              new AwardsAndRandomizationOptionsModel(destinationOptions, destinationPreferences);

        OptionsModelTestSupport.assertAllFieldsMatch(model, roundTripped, "recruitmentBonuses");
        assertEquals(model.recruitmentBonuses.get(PersonnelRole.MEKWARRIOR),
              roundTripped.recruitmentBonuses.get(PersonnelRole.MEKWARRIOR));
    }
}
