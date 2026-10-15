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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.universe.Faction2;
import megamek.common.universe.Factions2;
import mekhq.MHQConstants;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

class FactionMapLogoTest {
    private static final int GAME_YEAR = 3025;
    private static final int CUSTOM_PIXEL = 0x804080c0;
    private static final int FALLBACK_PIXEL = 0xff123456;

    @TempDir
    Path temporaryDirectory;

    private final Map<String, Faction2> factionData = new HashMap<>();
    private ClientPreferences preferences;
    private MockedStatic<Factions2> factionsMock;
    private MockedStatic<PreferenceManager> preferencesMock;
    private MockedStatic<Factions> logosMock;

    @BeforeEach
    void setUp() throws IOException {
        Path fallback = temporaryDirectory.resolve("fallback.png");
        writeImage(fallback, FALLBACK_PIXEL);
        preferences = mock(ClientPreferences.class);
        when(preferences.getUserDir()).thenReturn(temporaryDirectory.toString());
        Factions2 factions = mock(Factions2.class);
        when(factions.getFaction(anyString())).thenAnswer(invocation ->
              Optional.ofNullable(factionData.get(invocation.getArgument(0, String.class))));

        factionsMock = mockStatic(Factions2.class);
        factionsMock.when(Factions2::getInstance).thenReturn(factions);
        preferencesMock = mockStatic(PreferenceManager.class);
        preferencesMock.when(PreferenceManager::getClientPreferences).thenReturn(preferences);
        logosMock = mockStatic(Factions.class);
        logosMock.when(() -> Factions.getFactionLogoAddress(GAME_YEAR, "CUSTOM"))
              .thenReturn(fallback.toString());
        logosMock.when(() -> Factions.getFactionLogoAddress(3080, "CUSTOM"))
              .thenReturn(fallback.toString());
    }

    @AfterEach
    void tearDown() {
        logosMock.close();
        preferencesMock.close();
        factionsMock.close();
    }

    @Test
    void loadsUserDirectoryLogoWithOriginalDimensionsAndTransparency() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              logo: Custom/My Faction.png
              """);
        writeImage(userLogo("Custom/My Faction.png"), CUSTOM_PIXEL);

        BufferedImage image = FactionMapLogo.load(GAME_YEAR, "CUSTOM");

        assertNotNull(image);
        assertEquals(3, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(CUSTOM_PIXEL, image.getRGB(1, 0));
        assertEquals(0, image.getRGB(0, 0));
    }

    @Test
    void honorsEraDependentCustomLogos() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              logo: Custom/Base.png
              logoChanges:
                3080: Custom/Later.png
              """);
        writeImage(userLogo("Custom/Base.png"), CUSTOM_PIXEL);
        writeImage(userLogo("Custom/Later.png"), 0xffabcdef);

        assertEquals(CUSTOM_PIXEL, loadedPixel(GAME_YEAR));
        assertEquals(0xffabcdef, loadedPixel(3080));
    }

    @Test
    void missingOrInvalidCustomArtworkUsesGenericFallback() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              logo: Custom/Missing.png
              """);
        assertEquals(FALLBACK_PIXEL, loadedPixel(GAME_YEAR));

        Path invalidLogo = userLogo("Custom/Missing.png");
        Files.createDirectories(invalidLogo.getParent());
        Files.writeString(invalidLogo, "not an image");
        assertEquals(FALLBACK_PIXEL, loadedPixel(GAME_YEAR));
    }

    @Test
    void absentOrBlankLogoUsesGenericFallbackWithoutGrantingEmblemEligibility() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              """);
        assertFalse(FactionMapLogo.hasCustomLogo(GAME_YEAR, "CUSTOM"));
        assertEquals(FALLBACK_PIXEL, loadedPixel(GAME_YEAR));
        assertEquals(-1, priority("CUSTOM"));

        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              logo: '  '
              """);
        assertFalse(FactionMapLogo.hasCustomLogo(GAME_YEAR, "CUSTOM"));
        assertEquals(FALLBACK_PIXEL, loadedPixel(GAME_YEAR));
    }

    @Test
    void unknownFactionStillUsesFallback() {
        assertFalse(FactionMapLogo.hasCustomLogo(GAME_YEAR, "CUSTOM"));
        assertEquals(FALLBACK_PIXEL, loadedPixel(GAME_YEAR));
    }

    @Test
    void unreadableFallbackDoesNotProduceAnImage() {
        logosMock.when(() -> Factions.getFactionLogoAddress(GAME_YEAR, "CUSTOM"))
              .thenReturn(temporaryDirectory.resolve("missing-fallback.png").toString());

        assertNull(FactionMapLogo.load(GAME_YEAR, "CUSTOM"));
    }

    @Test
    void userArtworkTakesPrecedenceOverInstalledFormationLogo() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              logo: Inner Sphere/Federated Suns.png
              """);
        writeImage(userLogo("Inner Sphere/Federated Suns.png"), CUSTOM_PIXEL);

        assertEquals(CUSTOM_PIXEL, loadedPixel(GAME_YEAR));
    }

    @Test
    void installedFormationLogoWorksWhenUserArtworkIsAbsent() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              logo: Custom/Installed.png
              """);
        File expectedFile = new File(
              new File(MHQConstants.FORCE_ICON_PATH, MHQConstants.LAYERED_FORCE_ICON_LOGO_PATH),
              "Custom/Installed.png");
        BufferedImage installedImage = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        installedImage.setRGB(1, 0, CUSTOM_PIXEL);
        try (MockedStatic<ImageIO> images = mockStatic(ImageIO.class)) {
            images.when(() -> ImageIO.read(expectedFile)).thenReturn(installedImage);

            for (String userDirectory : new String[] { "", temporaryDirectory.toString() }) {
                when(preferences.getUserDir()).thenReturn(userDirectory);
                assertEquals(CUSTOM_PIXEL, loadedPixel(GAME_YEAR));
            }
            images.verify(() -> ImageIO.read(expectedFile), times(2));
        }
    }

    @Test
    void builtInArtworkRetainsPrecedenceOverFormationLogo() throws IOException {
        registerFaction("""
              key: FS
              name: Federated Suns
              logo: Custom/Replacement.png
              """);
        writeImage(userLogo("Custom/Replacement.png"), CUSTOM_PIXEL);
        Path canonicalImage = temporaryDirectory.resolve("canonical.png");
        writeImage(canonicalImage, FALLBACK_PIXEL);
        logosMock.when(() -> Factions.getCanonicalFactionLogoAddress(GAME_YEAR, "FS"))
              .thenReturn(canonicalImage.toString());

        BufferedImage image = FactionMapLogo.load(GAME_YEAR, "FS");

        assertNotNull(image);
        assertEquals(FALLBACK_PIXEL, image.getRGB(1, 0));
        assertFalse(FactionMapLogo.hasCustomLogo(GAME_YEAR, "FS"));
    }

    @Test
    void eraOnlyLogoGrantsEligibilityOnlyAfterItsIntroduction() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              logoChanges:
                3080: Custom/Later.png
              """);
        writeImage(userLogo("Custom/Later.png"), CUSTOM_PIXEL);

        assertFalse(FactionMapLogo.hasCustomLogo(GAME_YEAR, "CUSTOM"));
        assertTrue(FactionMapLogo.hasCustomLogo(3080, "CUSTOM"));
        assertEquals(-1, priority("CUSTOM"));
        assertEquals(1, InterstellarMapPanel.getFactionLogoPriority(new Faction(factionData.get("CUSTOM")), 3080));
        assertEquals(CUSTOM_PIXEL, loadedPixel(3080));
    }

    @Test
    void customLogoGrantsCompactPriorityWithoutPowerTags() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              tags: [PLAYABLE]
              logo: Custom/Logo.png
              """);

        assertTrue(FactionMapLogo.hasCustomLogo(GAME_YEAR, "CUSTOM"));
        assertEquals(1, priority("CUSTOM"));
    }

    @Test
    void existingPowerPrioritiesRemainUnchanged() throws IOException {
        for (String tag : new String[] { "MAJOR", "SUPER", "CLAN", "MINOR", "PERIPHERY", "DEEP_PERIPHERY",
                                       "PIRATE" }) {
            registerFaction("""
                  key: CUSTOM
                  name: Custom Faction
                  tags: [%s]
                  logo: Custom/Logo.png
                  """.formatted(tag));
            int expectedPriority = switch (tag) {
                case "MAJOR", "SUPER", "CLAN" -> 0;
                case "PIRATE" -> 2;
                default -> 1;
            };
            assertEquals(expectedPriority, priority("CUSTOM"), tag);
        }
    }

    @Test
    void independentAndAbandonedTerritoriesRemainExcluded() throws IOException {
        registerFaction("""
              key: IND
              name: Independent
              logo: Custom/Logo.png
              """);
        registerFaction("""
              key: CUSTOM
              name: Abandoned
              tags: [ABANDONED]
              logo: Custom/Logo.png
              """);

        assertEquals(-1, priority("IND"));
        assertEquals(-1, priority("CUSTOM"));
    }

    private void registerFaction(String yaml) throws IOException {
        Faction2 faction = new ObjectMapper(new YAMLFactory()).readValue(yaml, Faction2.class);
        factionData.put(faction.getKey(), faction);
    }

    private Path userLogo(String logo) {
        return temporaryDirectory.resolve(MHQConstants.FORCE_ICON_PATH)
                     .resolve(MHQConstants.LAYERED_FORCE_ICON_LOGO_PATH).resolve(logo);
    }

    private static void writeImage(Path path, int pixel) throws IOException {
        Files.createDirectories(path.getParent());
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(1, 0, pixel);
        assertTrue(ImageIO.write(image, "png", path.toFile()));
    }

    private static int loadedPixel(int year) {
        BufferedImage image = FactionMapLogo.load(year, "CUSTOM");
        assertNotNull(image);
        return image.getRGB(1, 0);
    }

    private int priority(String code) {
        return InterstellarMapPanel.getFactionLogoPriority(new Faction(factionData.get(code)), GAME_YEAR);
    }
}
