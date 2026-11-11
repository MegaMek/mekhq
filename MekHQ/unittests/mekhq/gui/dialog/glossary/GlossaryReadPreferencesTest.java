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
package mekhq.gui.dialog.glossary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.Version;
import megamek.client.ui.preferences.PreferencesNode;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GlossaryReadPreferences}: an entry read in one version counts as unread once it is updated.
 */
class GlossaryReadPreferencesTest {
    private static GlossaryReadPreferences preferences() {
        return new GlossaryReadPreferences(new PreferencesNode(GlossaryReadPreferencesTest.class));
    }

    @Test
    void anEntryIsReadOnlyInTheVersionItWasReadIn() {
        GlossaryReadPreferences preferences = preferences();
        preferences.markRead("HR_STRAIN", new Version("0.51.01"));

        assertTrue(preferences.isRead("HR_STRAIN", new Version("0.51.01")));
        assertFalse(preferences.isRead("HR_STRAIN", new Version("0.51.02")));
        assertFalse(preferences.isRead("TEMP_CREWS", new Version("0.51.01")));
    }

    @Test
    void readingANewerVersionReplacesTheOlderRecord() {
        GlossaryReadPreferences preferences = preferences();
        preferences.markRead("HR_STRAIN", new Version("0.51.01"));
        preferences.markRead("HR_STRAIN", new Version("0.51.02"));

        assertEquals("HR_STRAIN@0.51.02", preferences.serialize());
    }

    @Test
    void aLookUpNameThatStartsAnotherIsKeptApart() {
        GlossaryReadPreferences preferences = preferences();
        preferences.markRead("HR", new Version("0.51.01"));
        preferences.markRead("HR_STRAIN", new Version("0.51.01"));

        assertTrue(preferences.isRead("HR", new Version("0.51.01")));
        assertTrue(preferences.isRead("HR_STRAIN", new Version("0.51.01")));
    }

    @Test
    void savedRecordsAreReadBackAndJunkIsIgnored() {
        GlossaryReadPreferences preferences = preferences();
        preferences.deserialize("HR_STRAIN@0.51.01, junk ,TEMP_CREWS@0.50.12,");

        assertTrue(preferences.isRead("HR_STRAIN", new Version("0.51.01")));
        assertTrue(preferences.isRead("TEMP_CREWS", new Version("0.50.12")));
        assertEquals("HR_STRAIN@0.51.01,TEMP_CREWS@0.50.12", preferences.serialize());
    }
}
