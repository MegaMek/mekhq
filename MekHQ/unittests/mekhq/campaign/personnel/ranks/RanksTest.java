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
package mekhq.campaign.personnel.ranks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import mekhq.campaign.personnel.enums.RankSystemType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the rank system export in {@link Ranks}.
 *
 * <p>The export writes the user's custom rank systems to {@code userdata/data/universe/ranks.xml} on launch, so a
 * failure part-way through it destroys the user's data rather than just logging.</p>
 *
 * @since 0.51.01
 */
class RanksTest {
    private static final String CUSTOM_CODE = "MYMERC";
    private static final String COMBINE_CODE = "DCMS";

    @BeforeAll
    static void loadRankSystems() {
        Ranks.initializeRankSystems();
    }

    @Test
    void testGetRankSystemFromCode_FindsTheCampaignsOwnCustomSystem() {
        // Issue 10867: a system imported in Campaign Options lives only on the campaign. The right-click menu sends
        // its code, and looking that up in the global list fell back to the default system.
        RankSystem custom = campaignOnlyRankSystem();
        assertNotEquals(CUSTOM_CODE, Ranks.getRankSystemFromCode(CUSTOM_CODE).getCode(),
              "this test is only meaningful while the global list does not hold the custom system");

        assertSame(custom, Ranks.getRankSystemFromCode(CUSTOM_CODE, custom),
              "the campaign's own system is returned, not the default");
    }

    @Test
    void testGetRankSystemFromCode_OtherCodesStillComeFromTheGlobalList() {
        RankSystem custom = campaignOnlyRankSystem();

        assertSame(Ranks.getRankSystemFromCode(COMBINE_CODE), Ranks.getRankSystemFromCode(COMBINE_CODE, custom));
        assertEquals(COMBINE_CODE, Ranks.getRankSystemFromCode(COMBINE_CODE, null).getCode(),
              "without a campaign system the lookup is the global one");
    }

    /** A copy of a built-in system under a new code, stored only on the campaign, as a Campaign Options import is. */
    private static RankSystem campaignOnlyRankSystem() {
        RankSystem custom = new RankSystem(Ranks.getRankSystemFromCode(COMBINE_CODE));
        custom.setCode(CUSTOM_CODE);
        custom.setName("My Mercenaries");
        custom.setType(RankSystemType.CAMPAIGN);
        return custom;
    }

    @Test
    void testExportRankSystemsToFile_WritesTheLicenceHeaderAndTheRootElement(@TempDir Path tempDir)
          throws IOException {
        File file = tempDir.resolve("ranks.xml").toFile();

        Ranks.exportRankSystemsToFile(file, List.of());

        String contents = Files.readString(file.toPath());

        assertTrue(contents.startsWith("<?xml"), "The export should begin with the XML declaration.");
        // The header comes from a resource bundle. When that bundle was renamed and the exporter kept asking for the
        // old name, the lookup threw after the declaration was written, and the user's file was left holding
        // nothing else: every launch wiped their rank systems.
        assertTrue(contents.contains("MegaMek Data (C)"),
              "The MegaMek Data licence header should follow the declaration.");
        assertFalse(contents.contains("!Legal."),
              "A missing resource key should not be written into the file as its marker text.");
        assertTrue(contents.contains("<rankSystems"),
              "The root element should be written even when there are no rank systems to export.");
        assertTrue(contents.contains("</rankSystems>"), "The root element should be closed.");
    }
}
