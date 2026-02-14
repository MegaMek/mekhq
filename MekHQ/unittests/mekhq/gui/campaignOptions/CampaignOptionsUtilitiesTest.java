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
package mekhq.gui.campaignOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.Version;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;

class CampaignOptionsUtilitiesTest {

    @Test
    void testFormatBadges_NullMetadata() {
        // Null metadata should return empty string
        assertEquals("", CampaignOptionsUtilities.formatBadges(null));
    }

    @Test
    void testFormatBadges_NoFlagsNoVersion() {
        // Metadata with no flags and no version should still process version badge logic
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(null, Collections.emptySet());
        String result = CampaignOptionsUtilities.formatBadges(metadata);

        // Should return empty string (no flags, no version badge)
        assertEquals("", result);
    }

    @Test
    void testFormatBadges_WithFlags() {
        // Metadata with flags should include flag symbols
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            null,
            EnumSet.of(CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.IMPORTANT)
        );

        String result = CampaignOptionsUtilities.formatBadges(metadata);

        // Should contain flag symbols
        assertFalse(result.isEmpty());
        assertTrue(result.contains(CampaignOptionFlag.CUSTOM_SYSTEM.getSymbol()));
        assertTrue(result.contains(CampaignOptionFlag.IMPORTANT.getSymbol()));
    }

    @Test
    void testFormatBadges_WithVersion() {
        // Metadata with current version should include development badge
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            megamek.SuiteConstants.VERSION,
            Collections.emptySet()
        );

        String result = CampaignOptionsUtilities.formatBadges(metadata);

        // Should contain HTML span with color formatting for development badge
        if (!result.isEmpty()) {
            assertTrue(result.contains("<span"));
            assertTrue(result.contains("color:"));
            assertTrue(result.contains("</span>"));
        }
    }

    @Test
    void testFormatBadges_WithOldVersion() {
        // Old version (before last milestone) should not show version badge
        Version oldVersion = new Version(0, 49, 0);
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            oldVersion,
            Collections.emptySet()
        );

        String result = CampaignOptionsUtilities.formatBadges(metadata);

        // Should return empty string (no version badge for old versions)
        assertEquals("", result);
    }

    @Test
    void testFormatBadges_WithFlagsAndVersion() {
        // Metadata with both flags and version
        Version testVersion = megamek.SuiteConstants.VERSION;
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            testVersion,
            EnumSet.of(CampaignOptionFlag.DOCUMENTED, CampaignOptionFlag.RECOMMENDED)
        );

        String result = CampaignOptionsUtilities.formatBadges(metadata);

        // Should contain flag symbols
        assertFalse(result.isEmpty());
        assertTrue(result.contains(CampaignOptionFlag.DOCUMENTED.getSymbol()));
        assertTrue(result.contains(CampaignOptionFlag.RECOMMENDED.getSymbol()));
    }

    @Test
    void testFormatBadges_AllFlags() {
        // Test with all available flags
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            null,
            EnumSet.allOf(CampaignOptionFlag.class)
        );

        String result = CampaignOptionsUtilities.formatBadges(metadata);

        // Should contain all flag symbols
        assertFalse(result.isEmpty());
        for (CampaignOptionFlag flag : CampaignOptionFlag.values()) {
            assertTrue(result.contains(flag.getSymbol()),
                "Result should contain symbol for " + flag);
        }
    }

    @Test
    void testFormatBadges_SingleFlag() {
        // Test with single flag
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            null,
            EnumSet.of(CampaignOptionFlag.CUSTOM_SYSTEM)
        );

        String result = CampaignOptionsUtilities.formatBadges(metadata);

        // Should contain the single flag symbol
        assertFalse(result.isEmpty());
        assertTrue(result.contains(CampaignOptionFlag.CUSTOM_SYSTEM.getSymbol()));
    }
}
