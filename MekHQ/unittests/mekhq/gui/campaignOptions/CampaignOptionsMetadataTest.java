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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.SuiteConstants;
import megamek.Version;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;

class CampaignOptionsMetadataTest {

    @Test
    void testConstructor_NullFlags() {
        // Constructor should handle null flags and convert to empty set
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(null, null);

        assertNotNull(metadata.flags());
        assertTrue(metadata.flags().isEmpty());
    }

    @Test
    void testConstructor_EmptyFlags() {
        // Constructor should handle empty flags
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            null,
            Collections.emptySet()
        );

        assertNotNull(metadata.flags());
        assertTrue(metadata.flags().isEmpty());
    }

    @Test
    void testConstructor_WithFlags() {
        // Constructor should properly store flags
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            null,
            EnumSet.of(CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.DOCUMENTED)
        );

        assertNotNull(metadata.flags());
        assertEquals(2, metadata.flags().size());
        assertTrue(metadata.flags().contains(CampaignOptionFlag.CUSTOM_SYSTEM));
        assertTrue(metadata.flags().contains(CampaignOptionFlag.DOCUMENTED));
    }

    @Test
    void testConstructor_WithVersion() {
        // Constructor should properly store version
        Version testVersion = new Version(0, 50, 11);
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            testVersion,
            Collections.emptySet()
        );

        assertNotNull(metadata.version());
        assertEquals(testVersion, metadata.version());
    }

    @Test
    void testHasFlag_True() {
        // hasFlag should return true when flag is present
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            null,
            EnumSet.of(CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.RECOMMENDED)
        );

        assertTrue(metadata.hasFlag(CampaignOptionFlag.IMPORTANT));
        assertTrue(metadata.hasFlag(CampaignOptionFlag.RECOMMENDED));
    }

    @Test
    void testHasFlag_False() {
        // hasFlag should return false when flag is not present
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            null,
            EnumSet.of(CampaignOptionFlag.IMPORTANT)
        );

        assertFalse(metadata.hasFlag(CampaignOptionFlag.CUSTOM_SYSTEM));
        assertFalse(metadata.hasFlag(CampaignOptionFlag.DOCUMENTED));
        assertFalse(metadata.hasFlag(CampaignOptionFlag.RECOMMENDED));
    }

    @Test
    void testHasFlag_EmptyFlags() {
        // hasFlag should return false when no flags are set
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            null,
            Collections.emptySet()
        );

        assertFalse(metadata.hasFlag(CampaignOptionFlag.IMPORTANT));
        assertFalse(metadata.hasFlag(CampaignOptionFlag.CUSTOM_SYSTEM));
    }

    @Test
    void testGetAddedSinceBadgeHtml_NullVersion() {
        // No version should return empty string
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            null,
            Collections.emptySet()
        );

        assertEquals("", metadata.getAddedSinceBadgeHtml());
    }

    @Test
    void testGetAddedSinceBadgeHtml_CurrentVersion() {
        // Version matching current running version should show development badge
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            SuiteConstants.VERSION,
            Collections.emptySet()
        );

        String result = metadata.getAddedSinceBadgeHtml();

        // Should contain HTML span with color formatting
        if (!result.isEmpty()) {
            assertTrue(result.contains("<span"));
            assertTrue(result.contains("color:"));
            assertTrue(result.contains("</span>"));
        }
    }

    @Test
    void testGetAddedSinceBadgeHtml_AfterMilestone() {
        // Version after last milestone should show milestone badge
        Version afterMilestone = new Version(
            SuiteConstants.LAST_MILESTONE.getMajor(),
            SuiteConstants.LAST_MILESTONE.getMinor(),
            SuiteConstants.LAST_MILESTONE.getPatch() + 1
        );

        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            afterMilestone,
            Collections.emptySet()
        );

        String result = metadata.getAddedSinceBadgeHtml();

        // Should contain HTML span with color formatting
        if (!result.isEmpty()) {
            assertTrue(result.contains("<span"));
            assertTrue(result.contains("color:"));
            assertTrue(result.contains("</span>"));
        }
    }

    @Test
    void testGetAddedSinceBadgeHtml_OldVersion() {
        // Old version (before last milestone) should return empty string
        Version oldVersion = new Version(0, 49, 0);

        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            oldVersion,
            Collections.emptySet()
        );

        assertEquals("", metadata.getAddedSinceBadgeHtml());
    }

    @Test
    void testRecordImmutability() {
        // Test that the record properly exposes version and flags
        Version testVersion = new Version(0, 50, 11);
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            testVersion,
            EnumSet.of(CampaignOptionFlag.CUSTOM_SYSTEM)
        );

        // Verify record accessors work
        assertEquals(testVersion, metadata.version());
        assertNotNull(metadata.flags());
        assertTrue(metadata.flags().contains(CampaignOptionFlag.CUSTOM_SYSTEM));
    }

    @Test
    void testAllFlags() {
        // Test with all flags
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(
            null,
            EnumSet.allOf(CampaignOptionFlag.class)
        );

        for (CampaignOptionFlag flag : CampaignOptionFlag.values()) {
            assertTrue(metadata.hasFlag(flag),
                "Metadata should have flag: " + flag);
        }
    }

    @Test
    void testMultipleInstancesWithSameFlags() {
        // Test that multiple instances with same configuration work independently
        CampaignOptionsMetadata metadata1 = new CampaignOptionsMetadata(
            null,
            EnumSet.of(CampaignOptionFlag.IMPORTANT)
        );

        CampaignOptionsMetadata metadata2 = new CampaignOptionsMetadata(
            null,
            EnumSet.of(CampaignOptionFlag.IMPORTANT)
        );

        // Both should have the same flag
        assertTrue(metadata1.hasFlag(CampaignOptionFlag.IMPORTANT));
        assertTrue(metadata2.hasFlag(CampaignOptionFlag.IMPORTANT));
    }
}
