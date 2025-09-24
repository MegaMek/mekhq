/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package testUtilities;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Base64;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekSummary;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignConfiguration;
import mekhq.campaign.CampaignFactory;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.TestSystems;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;

public final class MHQTestUtilities {
    private static final String TEST_RESOURCES_DIR = "testresources/";
    private static final String TEST_DATA_DIR = TEST_RESOURCES_DIR + "data/";

    public static final String TEST_UNIT_DATA_DIR = TEST_DATA_DIR + "mekfiles/";
    public static final String TEST_CANON_SYSTEMS_DIR = TEST_DATA_DIR + "planetary_systems/canon_systems/";
    public static final String TEST_BLK = ".blk";
    public static final String TEST_MTF = ".mtf";


    public static CampaignConfiguration buildTestConfigWithSystems(Systems systems) {
        CampaignOptions options = new CampaignOptions();
        CampaignConfiguration campaignConfiguration = CampaignFactory.createPartialCampaignConfiguration(options);

        campaignConfiguration.setSystemsInstance(systems);
        // Finalize config and set up game instance
        Game game = new Game();
        campaignConfiguration.setGame(game);

        Player player = new Player(0, "TestPlayer");
        campaignConfiguration.setPlayer(player);

        LocalDate date = LocalDate.ofYearDay(3067, 1);
        campaignConfiguration.setCurrentDay(date);

        // We need one planetary system at least; load Galatea and get its location
        systems.load(TEST_CANON_SYSTEMS_DIR + "Galatea.yml");
        PlanetarySystem starterSystem = systems.getSystems().get("Galatea");
        campaignConfiguration.setLocation(new CurrentLocation(starterSystem, 0));

        return campaignConfiguration;
    }

    public static Campaign getTestCampaign() {
        return new Campaign(buildTestConfigWithSystems(TestSystems.getInstance()));
    }

    public static InputStream ParseBase64XmlFile(String base64) {
        return new ByteArrayInputStream(Decode(base64));
    }

    public static byte[] Decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Loads an {@link Entity} from a test unit data file (BLK or MTF format) for use in automated testing.
     *
     * <p>This method ensures that equipment types are initialized, constructs the appropriate file path from the
     * provided unit name and file type, and loads the entity using {@link MekSummary#loadEntity(File)}. If loading
     * fails (for example, if the file does not exist or cannot be parsed), a message is printed to standard output and
     * {@code null} is returned.</p>
     *
     * @param unitName the name of the unit file (without extension) to load
     * @param isBLK    {@code true} to load a BLK file, {@code false} to load an MTF file
     *
     * @return the loaded {@link Entity}, or {@code null} if loading fails
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static @Nullable Entity getEntityForUnitTesting(String unitName, boolean isBLK) {
        EquipmentType.initializeTypes();

        File file = new File(TEST_UNIT_DATA_DIR + unitName + (isBLK ? TEST_BLK : TEST_MTF));
        Entity entity = MekSummary.loadEntity(file);
        if (entity == null) {
            System.out.println("Failed to load entity " + file.getAbsolutePath());
            return null;
        }

        return entity;
    }
}
