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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.campaignOptionsLegendEntries;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import megamek.client.ui.settings.SettingsBadge;
import megamek.common.preference.PreferenceManager;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import org.junit.jupiter.api.Test;

class CampaignOptionsVisualRegressionTest {
    @Test
    void legendDescriptionsArePlainText() {
        for (SettingsBadge badge : campaignOptionsLegendEntries()) {
            assertFalse(badge.description().matches("(?s).*<[^>]+>.*"), badge.description());
        }
    }

    @Test
    void advancedMedicalMultiplierBadgesBelongToLabel() throws ReflectiveOperationException {
        var clientPreferences = PreferenceManager.getClientPreferences();
        String userDirectory = clientPreferences.getUserDir();
        MedicalPage page = new MedicalPage();
        try {
            clientPreferences.setUserDir("");
            page.createPanel(null);
        } finally {
            clientPreferences.setUserDir(userDirectory);
        }

        CampaignOptionsLabel label = getField(page,
              "lblAlternativeAdvancedMedicalHealingTimeMultiplier",
              CampaignOptionsLabel.class);
        CampaignOptionsSpinner spinner = getField(page,
              "spnAlternativeAdvancedMedicalHealingTimeMultiplier",
              CampaignOptionsSpinner.class);

        assertTrue(label.getText().contains(CampaignOptionFlag.IMPORTANT.getSymbol()));
        assertFalse(spinner.getSettingsHelpText().contains("Material Symbols Rounded"));
    }

    @Test
    void portraitRoleChangeTooltipUsesOneVerb() {
        assertEquals(
              "With this enabled, a person without a portrait will automatically gain a random portrait when their "
                    + "primary role changes.",
              getTextAt(getCampaignOptionsResourceBundle(), "lblAssignPortraitOnRoleChange.tooltip"));
    }

    private static <T> T getField(Object target, String fieldName, Class<T> fieldType)
          throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return fieldType.cast(field.get(target));
    }
}
