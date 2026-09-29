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
package mekhq.campaign.universe.factionStanding;

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.gui.dialog.factionStanding.FactionStandingUltimatumDialog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link FactionStandingUltimatumsLibrary}.
 *
 * <p>The fixtures under {@code testresources/data/universe/factionStandingUltimatums/} are verbatim copies of the
 * mm-data files, so these tests also check the shipped data for mistakes such as missing resource keys.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class FactionStandingUltimatumsLibraryTest {
    private static final File FIXTURE_DIRECTORY = new File("testresources/data/universe/factionStandingUltimatums");
    private static final File FIXTURE_MANIFEST = new File(FIXTURE_DIRECTORY, "ultimatummanifest.json");
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandingUltimatumDialog";
    private static final String MISSING_RESOURCE_TAG = "!";

    private static final LocalDate EXODUS_DATE = LocalDate.of(2784, 11, 5);
    private static final LocalDate TEST_DATE = LocalDate.of(3025, 1, 1);

    private static FactionStandingUltimatumsLibrary fixtureLibrary;

    @BeforeAll
    static void loadFixture() {
        fixtureLibrary = new FactionStandingUltimatumsLibrary(FIXTURE_MANIFEST);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static FactionStandingUltimatumData buildUltimatum(String name, LocalDate date,
          List<String> affectedFactionCodes) {
        FactionStandingUltimatumSide lyranSide = new FactionStandingUltimatumSide("LA", "Challenger",
              PersonnelRole.NOBLE, "LA");
        return new FactionStandingUltimatumData(name, date.toString(), null, affectedFactionCodes, List.of(lyranSide),
              "LA", false, 0);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static List<FactionStandingUltimatumData> getDistinctFixtureUltimatums() {
        List<FactionStandingUltimatumData> distinctUltimatums = new ArrayList<>();
        for (Map<String, FactionStandingUltimatumData> ultimatumsOnDate : fixtureLibrary.getUltimatums().values()) {
            for (FactionStandingUltimatumData ultimatum : ultimatumsOnDate.values()) {
                if (!distinctUltimatums.contains(ultimatum)) {
                    distinctUltimatums.add(ultimatum);
                }
            }
        }
        return distinctUltimatums;
    }

    @Test
    @DisplayName("The fixture loads every ultimatum")
    void testFixtureLoadsEveryUltimatum() {
        Set<String> names = new HashSet<>();
        for (FactionStandingUltimatumData ultimatum : getDistinctFixtureUltimatums()) {
            names.add(ultimatum.name());
        }

        assertEquals(Set.of("AMARIS_COUP", "EXODUS", "ESPINOSA_COUP", "ST_IVES_SPLIT", "RASALHAGUE_INDEPENDENCE",
              "COMSTAR_SCHISM", "FED_COM_CIVIL_WAR", "CLAN_WOLF_SPLIT"), names);
    }

    @Test
    @DisplayName("An ultimatum with several affected factions is indexed under each of them")
    void testMultipleAffectedFactionsShareOneUltimatum() {
        FactionStandingUltimatumData terranHegemony = fixtureLibrary.getUltimatum(EXODUS_DATE, "TH");
        FactionStandingUltimatumData starLeagueInExile = fixtureLibrary.getUltimatum(EXODUS_DATE, "SLIE");
        FactionStandingUltimatumData comStar = fixtureLibrary.getUltimatum(EXODUS_DATE, "CS");

        assertNotNull(terranHegemony);
        assertEquals("EXODUS", terranHegemony.name());
        assertSame(terranHegemony, starLeagueInExile);
        assertSame(terranHegemony, comStar);
        assertNull(fixtureLibrary.getUltimatum(EXODUS_DATE, "SL"));
    }

    @Test
    @DisplayName("getUltimatum returns null for a date or faction without an ultimatum")
    void testGetUltimatumReturnsNullWhenNoMatch() {
        assertNull(fixtureLibrary.getUltimatum(EXODUS_DATE.plusDays(1), "TH"));
        assertNull(fixtureLibrary.getUltimatum(EXODUS_DATE, "FS"));
    }

    @Test
    @DisplayName("getUltimatums cannot be modified by callers")
    void testGetUltimatumsIsUnmodifiable() {
        Map<LocalDate, Map<String, FactionStandingUltimatumData>> ultimatums = fixtureLibrary.getUltimatums();

        assertThrows(UnsupportedOperationException.class, () -> ultimatums.put(TEST_DATE, Map.of()));
        assertThrows(UnsupportedOperationException.class,
              () -> ultimatums.get(EXODUS_DATE).put("FS", buildUltimatum("TEST", EXODUS_DATE, List.of("FS"))));
    }

    @Test
    @DisplayName("Every ultimatum has all of its dialog resource keys")
    void testEveryUltimatumHasResourceKeys() {
        for (FactionStandingUltimatumData ultimatum : getDistinctFixtureUltimatums()) {
            List<String> sideIds = new ArrayList<>();
            for (FactionStandingUltimatumSide side : ultimatum.sides()) {
                sideIds.add(side.id());
            }
            for (String key : FactionStandingUltimatumDialog.getRequiredTextKeys(ultimatum.name(), sideIds)) {
                String text = getTextAt(RESOURCE_BUNDLE, key);
                assertFalse(text.startsWith(MISSING_RESOURCE_TAG), "Missing resource key: " + key);
            }
        }
    }

    @Test
    @DisplayName("Every ultimatum has complete sides from different factions and a valid dissenter preference")
    void testEveryUltimatumHasValidSides() {
        for (FactionStandingUltimatumData ultimatum : getDistinctFixtureUltimatums()) {
            assertFalse(ultimatum.sides().isEmpty(), ultimatum.name());
            Set<String> sideIds = new HashSet<>();
            Set<String> factionCodes = new HashSet<>();
            for (FactionStandingUltimatumSide side : ultimatum.sides()) {
                assertFalse(side.id() == null || side.id().isBlank(), ultimatum.name());
                assertFalse(side.name() == null || side.name().isBlank(), ultimatum.name());
                assertNotNull(side.role(), ultimatum.name());
                assertFalse(side.factionCode() == null || side.factionCode().isBlank(), ultimatum.name());
                assertTrue(sideIds.add(side.id()), ultimatum.name() + " repeats side " + side.id());
                assertTrue(factionCodes.add(side.factionCode()), ultimatum.name() + " repeats " + side.factionCode());
            }

            String preference = ultimatum.dissenterPreference();
            assertTrue(FactionStandingUltimatumData.ROGUE_PREFERENCE.equals(preference)
                             || ultimatum.getSide(preference) != null, ultimatum.name());
            assertFalse(ultimatum.affectedFactionCodes().isEmpty(), ultimatum.name());
        }
    }

    @Test
    @DisplayName("An ultimatum without sides is skipped")
    void testUltimatumWithoutSidesIsSkipped() {
        FactionStandingUltimatumData ultimatum = new FactionStandingUltimatumData("NO_SIDES", TEST_DATE.toString(),
              null, List.of("FC"), List.of(), "ROGUE", false, 0);

        FactionStandingUltimatumsLibrary library = new FactionStandingUltimatumsLibrary(List.of(ultimatum));

        assertTrue(library.getUltimatums().isEmpty());
    }

    @Test
    @DisplayName("A second ultimatum for the same date and faction is skipped, keeping the first")
    void testDuplicateDateAndFactionKeepsFirst() {
        FactionStandingUltimatumData first = buildUltimatum("FIRST", TEST_DATE, List.of("FC"));
        FactionStandingUltimatumData second = buildUltimatum("SECOND", TEST_DATE, List.of("FC", "LA"));

        FactionStandingUltimatumsLibrary library = new FactionStandingUltimatumsLibrary(List.of(first, second));

        assertSame(first, library.getUltimatum(TEST_DATE, "FC"));
        assertSame(second, library.getUltimatum(TEST_DATE, "LA"));
    }

    @Test
    @DisplayName("An ultimatum listing the same faction twice is not reported as its own duplicate")
    void testRepeatedFactionCodeInOneUltimatum() {
        FactionStandingUltimatumData ultimatum = buildUltimatum("REPEATED", TEST_DATE, List.of("FC", "FC"));

        FactionStandingUltimatumsLibrary library = new FactionStandingUltimatumsLibrary(List.of(ultimatum));

        assertSame(ultimatum, library.getUltimatum(TEST_DATE, "FC"));
    }

    @Test
    @DisplayName("An ultimatum without affected factions is skipped")
    void testUltimatumWithoutAffectedFactionsIsSkipped() {
        FactionStandingUltimatumData ultimatum = buildUltimatum("EMPTY", TEST_DATE, null);

        FactionStandingUltimatumsLibrary library = new FactionStandingUltimatumsLibrary(List.of(ultimatum));

        assertTrue(library.getUltimatums().isEmpty());
    }

    @Test
    @DisplayName("Every ultimatum file is named after its ultimatum")
    void testFileNamesMatchUltimatumNames() {
        FactionStandingUltimatumManifest manifest = FactionStandingUltimatumManifest.deserialize(FIXTURE_MANIFEST);

        assertNotNull(manifest);
        for (String fileName : manifest.ultimatumFileNames) {
            FactionStandingUltimatumData ultimatum = FactionStandingUltimatumData.deserialize(
                  new File(FIXTURE_DIRECTORY, fileName));
            assertNotNull(ultimatum, fileName);
            assertEquals(ultimatum.name() + ".json", fileName);
        }
    }

    @Test
    @DisplayName("A missing manifest fails loudly")
    void testMissingManifestThrows() {
        File missingFile = new File(FIXTURE_DIRECTORY, "doesNotExist.json");

        assertThrows(RuntimeException.class, () -> new FactionStandingUltimatumsLibrary(missingFile));
    }

    @Test
    @DisplayName("A manifest entry whose file is missing is skipped, keeping the others")
    void testMissingUltimatumFileIsSkipped(@TempDir Path temporaryDirectory) throws IOException {
        Files.copy(new File(FIXTURE_DIRECTORY, "EXODUS.json").toPath(), temporaryDirectory.resolve("EXODUS.json"));
        FactionStandingUltimatumManifest manifest = new FactionStandingUltimatumManifest();
        manifest.ultimatumFileNames.add("EXODUS.json");
        manifest.ultimatumFileNames.add("MISSING.json");
        File manifestFile = temporaryDirectory.resolve("ultimatummanifest.json").toFile();
        assertTrue(manifest.serialize(manifestFile));

        FactionStandingUltimatumsLibrary library = new FactionStandingUltimatumsLibrary(manifestFile);

        assertNotNull(library.getUltimatum(EXODUS_DATE, "TH"));
        assertEquals(1, library.getUltimatums().size());
    }

    @Test
    @DisplayName("An ultimatum with an invalid date is skipped")
    void testInvalidDateIsSkipped() {
        FactionStandingUltimatumSide side = new FactionStandingUltimatumSide("LA", "Challenger", PersonnelRole.NOBLE,
              "LA");
        FactionStandingUltimatumData ultimatum = new FactionStandingUltimatumData("BAD_DATE", "not a date", null,
              List.of("FC"), List.of(side), "LA", false, 0);

        FactionStandingUltimatumsLibrary library = new FactionStandingUltimatumsLibrary(List.of(ultimatum));

        assertTrue(library.getUltimatums().isEmpty());
    }
}
