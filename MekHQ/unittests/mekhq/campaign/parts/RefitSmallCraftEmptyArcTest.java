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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Entity;
import megamek.common.units.SmallCraft;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.missing.MissingFireControlSystem;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import testUtilities.MHQTestUtilities;
import testUtilities.parts.PartsScenario;

/**
 * Planning a refit of a small craft into a design with no weapons in one arc used to crash in the fire control system
 * when aerospace system hits were on, so the refit list could not be opened (issue #9406). The crash came from timing a
 * fire control system that belonged to no unit yet; 0.51.0 guarded it, and these tests keep it guarded.
 *
 * <p>The Aquarius Escort carries two Large Lasers in its nose; the test design is the same craft with the nose
 * emptied, the case the report describes.</p>
 */
class RefitSmallCraftEmptyArcTest {
    private static final String AQUARIUS_FILE = MHQTestUtilities.TEST_UNIT_DATA_DIR + "Aquarius Escort.blk";

    @TempDir
    Path temporaryFolder;

    private static Entity parse(File file) throws Exception {
        EquipmentType.initializeTypes();
        return new MekFileParser(file).getEntity();
    }

    private File writeDesignWithAnEmptyNose() throws Exception {
        String design = Files.readString(Path.of(AQUARIUS_FILE), StandardCharsets.UTF_8);
        String emptyNose = design.replaceAll("(?s)<Nose Equipment>.*?</Nose Equipment>",
              "<Nose Equipment>\n</Nose Equipment>");
        assertNotEquals(design, emptyNose, "The nose weapons were taken out");
        Path emptyNoseFile = temporaryFolder.resolve("Aquarius Escort Empty Nose.blk");
        Files.writeString(emptyNoseFile, emptyNose, StandardCharsets.UTF_8);
        return emptyNoseFile.toFile();
    }

    @Test
    void aRefitIntoADesignWithAnEmptyArcCanBePlannedWithAerospaceSystemHits() throws Exception {
        PartsScenario scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.USE_AERO_SYSTEM_HITS, true);
        Entity aquarius = parse(new File(AQUARIUS_FILE));
        assertTrue(aquarius instanceof SmallCraft);
        Unit unit = campaign.addNewUnit(aquarius, false, 0, PartQuality.QUALITY_D);
        Entity emptyNoseDesign = parse(writeDesignWithAnEmptyNose());

        Refit refit = assertDoesNotThrow(() -> new Refit(unit, emptyNoseDesign, false, false, false));

        assertTrue(refit.getTime() > 0, "The refit was planned");
        int noseWeapons = 0;
        for (WeaponMounted weapon : emptyNoseDesign.getWeaponList()) {
            if (weapon.getLocation() == SmallCraft.LOC_NOSE) {
                noseWeapons++;
            }
        }
        assertEquals(0, noseWeapons, "The design really has no nose weapons");
    }
    @Test
    void aFireControlSystemNotYetOnAUnitHasARepairTime() {
        PartsScenario scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.USE_AERO_SYSTEM_HITS, true);
        // Refit planning builds the new design's parts before they belong to any unit; this one has no unit
        MissingFireControlSystem plannedFireControl = new MissingFireControlSystem(200, Money.of(150000), campaign);

        int minutes = assertDoesNotThrow(plannedFireControl::getBaseTime);

        assertEquals(600, minutes, "Without a unit it is timed as a small craft or fighter system");
    }
}
