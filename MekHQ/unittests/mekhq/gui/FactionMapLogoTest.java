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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.universe.Faction2;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

class FactionMapLogoTest {
    private static final int GAME_YEAR = 3025;
    private static final int ICON_PIXEL = 0x804080c0;
    private static final int FALLBACK_PIXEL = 0xff123456;

    @TempDir
    Path temporaryDirectory;

    private Path icon;
    private Path fallback;
    private MockedStatic<RATGenerator> logosMock;

    @BeforeEach
    void setUp() throws IOException {
        icon = temporaryDirectory.resolve("icon.png");
        writeImage(icon, ICON_PIXEL);
        fallback = temporaryDirectory.resolve("fallback.png");
        writeImage(fallback, FALLBACK_PIXEL);

        logosMock = mockStatic(RATGenerator.class);
        logosMock.when(() -> RATGenerator.getCanonicalFactionLogoAddress(GAME_YEAR, "ICON"))
              .thenReturn(icon.toString());
        logosMock.when(() -> RATGenerator.getFactionLogoAddress(GAME_YEAR, "ICON"))
              .thenReturn(icon.toString());
        logosMock.when(() -> RATGenerator.getFactionLogoAddress(GAME_YEAR, "CUSTOM"))
              .thenReturn(fallback.toString());
    }

    @AfterEach
    void tearDown() {
        logosMock.close();
    }

    @Test
    void loadsFactionIconWithOriginalDimensionsAndTransparency() {
        BufferedImage image = FactionMapLogo.load(GAME_YEAR, "ICON");

        assertNotNull(image);
        assertEquals(3, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(ICON_PIXEL, image.getRGB(1, 0));
        assertEquals(0, image.getRGB(0, 0));
        assertTrue(FactionMapLogo.hasIcon(GAME_YEAR, "ICON"));
    }

    @Test
    void factionWithoutIconUsesGenericFallback() {
        BufferedImage image = FactionMapLogo.load(GAME_YEAR, "CUSTOM");

        assertNotNull(image);
        assertEquals(FALLBACK_PIXEL, image.getRGB(1, 0));
        assertFalse(FactionMapLogo.hasIcon(GAME_YEAR, "CUSTOM"));
    }

    @Test
    void unreadableFallbackDoesNotProduceAnImage() {
        logosMock.when(() -> RATGenerator.getFactionLogoAddress(GAME_YEAR, "CUSTOM"))
              .thenReturn(temporaryDirectory.resolve("missing-fallback.png").toString());

        assertNull(FactionMapLogo.load(GAME_YEAR, "CUSTOM"));
    }

    @Test
    void iconGrantsCompactPriorityWithoutPowerTags() throws IOException {
        assertEquals(1, priority("""
              key: ICON
              name: Faction With Icon
              tags: [PLAYABLE]
              """));
        assertEquals(-1, priority("""
              key: CUSTOM
              name: Faction Without Icon
              tags: [PLAYABLE]
              """));
    }

    @Test
    void existingPowerPrioritiesRemainUnchanged() throws IOException {
        for (String tag : new String[] { "MAJOR", "SUPER", "CLAN", "MINOR", "PERIPHERY", "DEEP_PERIPHERY",
                                       "PIRATE" }) {
            int expectedPriority = switch (tag) {
                case "MAJOR", "SUPER", "CLAN" -> 0;
                case "PIRATE" -> 2;
                default -> 1;
            };
            assertEquals(expectedPriority, priority("""
                  key: ICON
                  name: Faction With Icon
                  tags: [%s]
                  """.formatted(tag)), tag);
        }
    }

    @Test
    void independentAndAbandonedTerritoriesRemainExcluded() throws IOException {
        logosMock.when(() -> RATGenerator.getCanonicalFactionLogoAddress(GAME_YEAR, "IND"))
              .thenReturn(icon.toString());

        assertEquals(-1, priority("""
              key: IND
              name: Independent
              """));
        assertEquals(-1, priority("""
              key: ICON
              name: Abandoned
              tags: [ABANDONED]
              """));
    }

    private static int priority(String yaml) throws IOException {
        Faction2 faction = new ObjectMapper(new YAMLFactory()).readValue(yaml, Faction2.class);
        return InterstellarMapPanel.getFactionLogoPriority(new Faction(faction), GAME_YEAR);
    }

    private static void writeImage(Path path, int pixel) throws IOException {
        Files.createDirectories(path.getParent());
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(1, 0, pixel);
        assertTrue(ImageIO.write(image, "png", path.toFile()));
    }
}
