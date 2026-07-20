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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConHydrology;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConOrogeny;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorShape;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConUrban;

/**
 * Points the StratCon data singletons at the test copies of their definition files.
 *
 * <p>At runtime these load from the {@code data} directory, which MekHQ builds when it launches. Nothing builds it for
 * the tests, so without this every one of them silently falls back to its hard-coded default - a single square sector
 * shape, one hydrology profile, an empty biome manifest - and any test that asserts something about the authored data
 * fails for want of a build step rather than for a real defect.</p>
 *
 * <p>The fixtures under {@code testresources/data/stratconbiomedefinitions/} are verbatim copies of the mm-data
 * originals, so tests keep asserting against the data the game actually ships. Re-copy them when the originals
 * change.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConTestData {
    private StratConTestData() {}

    private static final String FIXTURE_DIRECTORY = "testresources/data/stratconbiomedefinitions";

    private static boolean loaded;

    /**
     * Loads every StratCon definition file from {@code testresources}, once per JVM. Safe (and cheap) to call from the
     * {@code @BeforeAll} of any test that touches terrain, biomes, or sector generation.
     */
    public static void install() {
        if (loaded) {
            return;
        }

        StratConBiomeManifest.loadForTest(fixture("StratConBiomeManifest.xml"));
        StratConHydrology.loadForTest(fixture("HydrologyProfiles.yaml"));
        StratConOrogeny.loadForTest(fixture("OrogenyProfiles.yaml"));
        StratConUrban.loadForTest(fixture("UrbanProfiles.yaml"));
        StratConSectorShape.loadForTest(fixture("SectorShapeProfiles.yaml"));

        loaded = true;
    }

    /**
     * @return the path to a fixture file, failing loudly if it is missing. A silent miss would leave the singleton on
     *       its default and produce puzzling assertion failures much later.
     */
    private static String fixture(String fileName) {
        File file = new File(FIXTURE_DIRECTORY, fileName);
        assertTrue(file.isFile(),
              "Missing StratCon test fixture: " + file.getAbsolutePath() +
                    " - copy it from mm-data/data/stratconbiomedefinitions/");
        return file.getPath();
    }
}
