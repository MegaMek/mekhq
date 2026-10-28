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
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.universe.Faction2;
import megamek.common.universe.Factions2;
import mekhq.MHQConstants;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

class FactionMapLogoTest {
    private static final int GAME_YEAR = 3025;
    private static final int ICON_PIXEL = 0x804080c0;
    private static final int LEGACY_PIXEL = 0xffabcdef;
    private static final int FALLBACK_PIXEL = 0xff123456;

    @TempDir
    Path temporaryDirectory;

    private final Map<String, Faction2> factionData = new HashMap<>();
    private Path icon;
    private Path fallback;
    private ClientPreferences preferences;
    private MockedStatic<Factions2> factionsMock;
    private MockedStatic<PreferenceManager> preferencesMock;
    private MockedStatic<RATGenerator> logosMock;

    @BeforeEach
    void setUp() throws IOException {
        icon = temporaryDirectory.resolve("icon.png");
        writeImage(icon, ICON_PIXEL);
        fallback = temporaryDirectory.resolve("fallback.png");
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
        logosMock = mockStatic(RATGenerator.class);
        logosMock.when(() -> RATGenerator.getCanonicalFactionLogoAddress(GAME_YEAR, "ICON"))
              .thenReturn(icon.toString());
        logosMock.when(() -> RATGenerator.getFactionLogoFile(GAME_YEAR, "ICON")).thenReturn(icon.toFile());
        logosMock.when(() -> RATGenerator.getGenericFactionLogoFile(anyString())).thenReturn(fallback.toFile());
    }

    @AfterEach
    void tearDown() {
        logosMock.close();
        preferencesMock.close();
        factionsMock.close();
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
    void invalidFactionIconUsesGenericFallback() throws IOException {
        Path invalidIcon = temporaryDirectory.resolve("invalid.png");
        Files.writeString(invalidIcon, "not an image");
        logosMock.when(() -> RATGenerator.getFactionLogoFile(GAME_YEAR, "ICON")).thenReturn(invalidIcon.toFile());

        assertEquals(FALLBACK_PIXEL, loadedPixel("ICON", GAME_YEAR));
    }

    @Test
    void factionWithoutIconUsesGenericFallback() {
        assertEquals(FALLBACK_PIXEL, loadedPixel("CUSTOM", GAME_YEAR));
        assertFalse(FactionMapLogo.hasIcon(GAME_YEAR, "CUSTOM"));
    }

    @Test
    void unreadableFallbackDoesNotProduceAnImage() {
        logosMock.when(() -> RATGenerator.getGenericFactionLogoFile("CUSTOM"))
              .thenReturn(temporaryDirectory.resolve("missing-fallback.png").toFile());

        assertNull(FactionMapLogo.load(GAME_YEAR, "CUSTOM"));
    }

    @Test
    void loadsLegacyLogoFromUserDirectory() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              logo: Custom/My Faction.png
              """);
        writeImage(userLegacyLogo("Custom/My Faction.png"), LEGACY_PIXEL);

        assertEquals(LEGACY_PIXEL, loadedPixel("CUSTOM", GAME_YEAR));
        assertTrue(FactionMapLogo.hasIcon(GAME_YEAR, "CUSTOM"));
    }

    @Test
    void honorsEraDependentLegacyLogos() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              logo: Custom/Base.png
              logoChanges:
                3080: Custom/Later.png
              """);
        writeImage(userLegacyLogo("Custom/Base.png"), LEGACY_PIXEL);
        writeImage(userLegacyLogo("Custom/Later.png"), ICON_PIXEL);

        assertEquals(LEGACY_PIXEL, loadedPixel("CUSTOM", GAME_YEAR));
        assertEquals(ICON_PIXEL, loadedPixel("CUSTOM", 3080));
    }

    @Test
    void missingOrInvalidLegacyLogoUsesGenericFallback() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              logo: Custom/Missing.png
              """);
        assertEquals(FALLBACK_PIXEL, loadedPixel("CUSTOM", GAME_YEAR));

        Path invalidLogo = userLegacyLogo("Custom/Missing.png");
        Files.createDirectories(invalidLogo.getParent());
        Files.writeString(invalidLogo, "not an image");
        assertEquals(FALLBACK_PIXEL, loadedPixel("CUSTOM", GAME_YEAR));
    }

    @Test
    void installedLegacyLogoWorksWhenUserArtworkIsAbsent() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              logo: Custom/Installed.png
              """);
        File expectedFile = new File(
              new File(MHQConstants.FORCE_ICON_PATH, MHQConstants.LAYERED_FORCE_ICON_LOGO_PATH),
              "Custom/Installed.png");
        BufferedImage installedImage = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        installedImage.setRGB(1, 0, LEGACY_PIXEL);
        try (MockedStatic<ImageIO> images = mockStatic(ImageIO.class)) {
            images.when(() -> ImageIO.read(expectedFile)).thenReturn(installedImage);

            for (String userDirectory : new String[] { "", temporaryDirectory.toString() }) {
                when(preferences.getUserDir()).thenReturn(userDirectory);
                assertEquals(LEGACY_PIXEL, loadedPixel("CUSTOM", GAME_YEAR));
            }
            images.verify(() -> ImageIO.read(expectedFile), times(2));
        }
    }

    @Test
    void iconTakesPrecedenceOverLegacyLogo() throws IOException {
        registerFaction("""
              key: ICON
              name: Faction With Icon
              logo: Custom/Replacement.png
              """);
        writeImage(userLegacyLogo("Custom/Replacement.png"), LEGACY_PIXEL);

        assertEquals(ICON_PIXEL, loadedPixel("ICON", GAME_YEAR));
    }

    @Test
    void blankLegacyLogoDoesNotGrantEmblemEligibility() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              logo: '  '
              """);

        assertFalse(FactionMapLogo.hasIcon(GAME_YEAR, "CUSTOM"));
        assertEquals(FALLBACK_PIXEL, loadedPixel("CUSTOM", GAME_YEAR));
        assertEquals(-1, priority("CUSTOM"));
    }

    @Test
    void eraOnlyLegacyLogoGrantsEligibilityOnlyAfterItsIntroduction() throws IOException {
        registerFaction("""
              key: CUSTOM
              name: Custom Faction
              logoChanges:
                3080: Custom/Later.png
              """);

        assertFalse(FactionMapLogo.hasIcon(GAME_YEAR, "CUSTOM"));
        assertTrue(FactionMapLogo.hasIcon(3080, "CUSTOM"));
        assertEquals(-1, priority("CUSTOM"));
        assertEquals(1, InterstellarMapPanel.getFactionLogoPriority(new Faction(factionData.get("CUSTOM")), 3080));
    }

    @Test
    void iconOrLegacyLogoGrantsCompactPriorityWithoutPowerTags() throws IOException {
        registerFaction("""
              key: ICON
              name: Faction With Icon
              tags: [PLAYABLE]
              """);
        registerFaction("""
              key: LEGACY
              name: Faction With Legacy Logo
              tags: [PLAYABLE]
              logo: Custom/Logo.png
              """);
        registerFaction("""
              key: CUSTOM
              name: Faction Without Icon
              tags: [PLAYABLE]
              """);

        assertEquals(1, priority("ICON"));
        assertEquals(1, priority("LEGACY"));
        assertEquals(-1, priority("CUSTOM"));
    }

    @Test
    void existingPowerPrioritiesRemainUnchanged() throws IOException {
        for (String tag : new String[] { "MAJOR", "SUPER", "CLAN", "MINOR", "PERIPHERY", "DEEP_PERIPHERY",
                                       "PIRATE" }) {
            registerFaction("""
                  key: ICON
                  name: Faction With Icon
                  tags: [%s]
                  """.formatted(tag));
            int expectedPriority = switch (tag) {
                case "MAJOR", "SUPER", "CLAN" -> 0;
                case "PIRATE" -> 2;
                default -> 1;
            };
            assertEquals(expectedPriority, priority("ICON"), tag);
        }
    }

    @Test
    void independentAndAbandonedTerritoriesRemainExcluded() throws IOException {
        logosMock.when(() -> RATGenerator.getCanonicalFactionLogoAddress(GAME_YEAR, "IND"))
              .thenReturn(icon.toString());
        registerFaction("""
              key: IND
              name: Independent
              """);
        registerFaction("""
              key: ICON
              name: Abandoned
              tags: [ABANDONED]
              """);

        assertEquals(-1, priority("IND"));
        assertEquals(-1, priority("ICON"));
    }

    private void registerFaction(String yaml) throws IOException {
        Faction2 faction = new ObjectMapper(new YAMLFactory()).readValue(yaml, Faction2.class);
        factionData.put(faction.getKey(), faction);
    }

    private Path userLegacyLogo(String logo) {
        return temporaryDirectory.resolve(MHQConstants.FORCE_ICON_PATH)
                     .resolve(MHQConstants.LAYERED_FORCE_ICON_LOGO_PATH).resolve(logo);
    }

    private static void writeImage(Path path, int pixel) throws IOException {
        Files.createDirectories(path.getParent());
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(1, 0, pixel);
        assertTrue(ImageIO.write(image, "png", path.toFile()));
    }

    private static int loadedPixel(String factionCode, int year) {
        BufferedImage image = FactionMapLogo.load(year, factionCode);
        assertNotNull(image);
        return image.getRGB(1, 0);
    }

    private int priority(String code) {
        return InterstellarMapPanel.getFactionLogoPriority(new Faction(factionData.get(code)), GAME_YEAR);
    }
}
