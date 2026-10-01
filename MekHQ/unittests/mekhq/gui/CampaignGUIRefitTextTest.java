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
package mekhq.gui;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The refit screens read their text from the CampaignGUI resource bundle. Every key they use must exist, and patterns
 * with arguments must format cleanly, so no player sees a missing-key marker or a raw placeholder.
 */
class CampaignGUIRefitTextTest {
    private static final String BUNDLE = "mekhq.resources.CampaignGUI";

    @ParameterizedTest
    @ValueSource(strings = { "refitSelectTech.title", "refitSelectTech.prompt", "refitSelectTech.noTechs.title",
                             "refitSelectTech.noTechs.text", "refitSelectTech.wrongType.title",
                             "refitSelectTech.wrongType.text", "refitNoEngineer.title", "refitNoEngineer.text",
                             "refitConfirm.title", "refitLoadFailed.title", "refitLoadFailed.text",
                             "refitIOException.title", "refitEngineerCannotWork.title" })
    void plainRefitTextExists(String key) {
        String text = getFormattedTextAt(BUNDLE, key);

        assertFalse(text.startsWith("!"), key + " is missing from the bundle");
    }

    @ParameterizedTest
    @ValueSource(strings = { "refitConfirm.refit.text", "refitConfirm.refurbish.text" })
    void refitConfirmationNamesTheClassAndTheUnit(String key) {
        String text = getFormattedTextAt(BUNDLE, key, "Class C", "Locust LCT-1V");

        assertFalse(text.contains("{"), text);
        assertEquals(true, text.contains("Class C") && text.contains("Locust LCT-1V"), text);
    }

    @ParameterizedTest
    @ValueSource(strings = { "refitSelectTech.techLabel" })
    void techLabelFillsEveryField(String key) {
        String estimate = "TN 7 (58%), 1480 min, done in 3 more day(s)";
        String text = getFormattedTextAt(BUNDLE, key, "Jane Doe", "Regular", "Mek Tech", estimate);

        assertFalse(text.contains("{"), text);
        assertEquals(true, text.contains(estimate), text);
    }
}
