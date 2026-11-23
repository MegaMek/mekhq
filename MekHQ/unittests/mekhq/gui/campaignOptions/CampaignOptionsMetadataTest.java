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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import megamek.SuiteConstants;
import megamek.Version;
import megamek.client.ui.settings.SettingsBadge;
import org.junit.jupiter.api.Test;

class CampaignOptionsMetadataTest {
    private static final Color SINCE_LAST_CORE_COLOR = Color.decode("#C344C3");
    private static final Color LAST_CORE_OR_EARLIER_COLOR = Color.decode("#4A9EE8");

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
    void testGetAddedSinceSettingsBadge_NullVersion() {
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(null, Collections.emptySet());

        assertNull(metadata.getAddedSinceSettingsBadge(new Version(0, 51, 0)));
    }

    @Test
    void testGetAddedSinceSettingsBadge_AfterLastCoreIsPurpleVersion() {
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(new Version(0, 51, 1),
              Collections.emptySet());

        SettingsBadge badge = metadata.getAddedSinceSettingsBadge(new Version(0, 51, 0));

        assertNotNull(badge);
        assertEquals("0.51.01", badge.text());
        assertEquals(SINCE_LAST_CORE_COLOR, badge.color());
    }

    @Test
    void testGetAddedSinceSettingsBadge_LastCoreIsBlueVersion() {
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(new Version(0, 51, 0),
              Collections.emptySet());

        SettingsBadge badge = metadata.getAddedSinceSettingsBadge(new Version(0, 51, 0));

        assertNotNull(badge);
        assertEquals("0.51.00", badge.text());
        assertEquals(LAST_CORE_OR_EARLIER_COLOR, badge.color());
    }

    @Test
    void testGetAddedSinceSettingsBadge_OlderVersionIsBlueVersion() {
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(new Version(0, 49, 0),
              Collections.emptySet());

        SettingsBadge badge = metadata.getAddedSinceSettingsBadge(new Version(0, 51, 0));

        assertNotNull(badge);
        assertEquals("0.49.00", badge.text());
        assertEquals(LAST_CORE_OR_EARLIER_COLOR, badge.color());
    }

    @Test
    void testGetAddedSinceBadgeHtml_ShowsVersionText() {
        Version version = new Version(0, 49, 0);
        CampaignOptionsMetadata metadata = new CampaignOptionsMetadata(version, Collections.emptySet());

        String result = metadata.getAddedSinceBadgeHtml();

        assertTrue(result.contains(">0.49.00</font>"), result);
        assertFalse(result.contains("Material Symbols Rounded"), result);
    }

    @Test
    void testAddedSinceLegendEntries_NameTheLastCoreRelease() {
        List<SettingsBadge> entries = CampaignOptionsMetadata.addedSinceLegendEntries();

        assertEquals(List.of(SINCE_LAST_CORE_COLOR, LAST_CORE_OR_EARLIER_COLOR),
              entries.stream().map(SettingsBadge::color).toList());
        for (SettingsBadge entry : entries) {
            assertTrue(entry.isText());
            // The legend names the last Core release by its number alone, without any PR or nightly build suffix
            Version lastCore = SuiteConstants.LAST_MILESTONE;
            String lastCoreNumber = String.format("%d.%02d.%02d", lastCore.getMajor(), lastCore.getMinor(),
                  lastCore.getPatch());
            assertTrue(entry.text().contains(lastCoreNumber), entry.text());
            assertFalse(entry.text().contains(lastCoreNumber + "-"), entry.text());
        }
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
