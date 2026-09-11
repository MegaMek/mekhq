/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.advancedCharacterBuilder;

import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTest.populatedBuilder;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTestFixtures.validBuilder;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import mekhq.MHQConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests {@link LifePathIO}: that a Life Path survives a trip to disk and back, that the licence header written above
 * the JSON does not stop it being read, and that the stored XP cost is brought up to date on the way in.
 *
 * @since 0.50.11
 */
class LifePathIOTest {
    @TempDir
    Path temporaryDirectory;

    // region Round trip

    @Test
    void testRoundTrip_PreservesEveryComponent() throws IOException {
        LifePathBuilder builder = populatedBuilder();
        LifePath original = builder.xpCost(LifePathXPCostCalculator.calculateXPCost(builder)).build();
        File file = temporaryDirectory.resolve("round-trip.json").toFile();

        LifePathIO.saveAction(original, file, false);
        LifePath reloaded = LifePathIO.readLifePathFromFile(file);

        assertEquals(original, reloaded, "A Life Path with every map populated should survive a save and load.");
    }

    @Test
    void testRoundTrip_PreservesAnEmptyLifePath() throws IOException {
        LifePath original = validBuilder().build();
        File file = temporaryDirectory.resolve("empty.json").toFile();

        LifePathIO.saveAction(original, file, false);
        LifePath reloaded = LifePathIO.readLifePathFromFile(file);

        assertEquals(original, reloaded, "An empty Life Path should survive a save and load.");
    }

    @Test
    void testSave_StampsTheCurrentVersion() throws IOException {
        LifePath original = validBuilder().version(MHQConstants.LAST_MILESTONE).build();
        File file = temporaryDirectory.resolve("versioned.json").toFile();

        LifePathIO.saveAction(original, file, false);

        assertEquals(MHQConstants.VERSION, LifePathIO.readLifePathFromFile(file).version(),
              "Saving should bring the file up to the current version.");
    }

    @Test
    void testRoundTrip_PreservesSparseGroupIndexes() throws IOException {
        // Empty groups are stripped on save, so a file can legitimately hold groups 4 and 9 and nothing between.
        LifePath original = validBuilder().fixedXPSkills(Map.of(4, Map.of("Gunnery/Mek", 100),
              9, Map.of("Piloting/Mek", 100))).xpCost(200).build();
        File file = temporaryDirectory.resolve("sparse.json").toFile();

        LifePathIO.saveAction(original, file, false);
        LifePath reloaded = LifePathIO.readLifePathFromFile(file);

        assertEquals(original.fixedXPSkills(), reloaded.fixedXPSkills(),
              "Sparse group indexes should come back as they went in.");
    }

    // endregion Round trip

    // region Licence header

    @Test
    void testSave_WithLegalStatement_WritesAHeaderAboveTheJson() throws IOException {
        LifePath original = validBuilder().build();
        File file = temporaryDirectory.resolve("with-header.json").toFile();

        LifePathIO.saveAction(original, file, true);

        String contents = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        assertFalse(contents.stripLeading().startsWith("{"),
              "A file saved with the licence statement should have something above the JSON.");
        assertTrue(LifePathIO.fileHasLegalStatement(file),
              "A file saved with the licence statement should be recognised as having one.");
    }

    @Test
    void testRead_SkipsTheLicenceHeader() throws IOException {
        LifePath original = validBuilder().build();
        File file = temporaryDirectory.resolve("header-round-trip.json").toFile();

        LifePathIO.saveAction(original, file, true);

        assertEquals(original, LifePathIO.readLifePathFromFile(file),
              "The licence header must not stop the file being read.");
    }

    @Test
    void testSave_WithoutLegalStatement_WritesNoHeader() throws IOException {
        LifePath original = validBuilder().build();
        File file = temporaryDirectory.resolve("no-header.json").toFile();

        LifePathIO.saveAction(original, file, false);

        assertTrue(Files.readString(file.toPath(), StandardCharsets.UTF_8).stripLeading().startsWith("{"),
              "A file saved without the licence statement should start with its JSON.");
        assertFalse(LifePathIO.fileHasLegalStatement(file),
              "A file saved without the licence statement should not be recognised as having one.");
    }

    @Test
    void testRead_SkipsAnArbitraryCommentPreamble() throws IOException {
        LifePath original = validBuilder().build();
        File plainFile = temporaryDirectory.resolve("plain.json").toFile();
        LifePathIO.saveAction(original, plainFile, false);

        String json = Files.readString(plainFile.toPath(), StandardCharsets.UTF_8);
        File commentedFile = temporaryDirectory.resolve("commented.json").toFile();
        Files.writeString(commentedFile.toPath(),
              "# a comment\n\n# another comment\n" + json, StandardCharsets.UTF_8);

        assertEquals(original, LifePathIO.readLifePathFromFile(commentedFile),
              "Everything above the first brace should be discarded, whatever it says.");
    }

    // endregion Licence header

    // region Tolerance of older and newer files

    @Test
    void testRead_IgnoresAnUnknownProperty() throws IOException {
        LifePath original = validBuilder().build();
        File plainFile = temporaryDirectory.resolve("plain-for-extra.json").toFile();
        LifePathIO.saveAction(original, plainFile, false);

        // Stands in for a component that has since been removed from the record. Jackson rejects unknown properties
        // by default, which would mean every existing file stopped loading the day one was dropped.
        String json = Files.readString(plainFile.toPath(), StandardCharsets.UTF_8);
        String withExtra = json.replaceFirst("\\{", "{\n    \"componentThatNoLongerExists\" : 42,");
        File extraFile = temporaryDirectory.resolve("extra.json").toFile();
        Files.writeString(extraFile.toPath(), withExtra, StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> LifePathIO.readLifePathFromFile(extraFile),
              "A property the record no longer has should be ignored, not fatal.");
    }

    // endregion Tolerance of older and newer files

    // region Recomputed XP cost

    @Test
    void testRead_RecomputesAStaleXPCost() throws IOException {
        // Written with a cost that does not match its contents, as a file saved before a cost rule changed would be.
        LifePath wrongCost = validBuilder().fixedXPSkills(Map.of(0, Map.of("Gunnery/Mek", 200))).xpCost(999).build();
        File file = temporaryDirectory.resolve("stale-cost.json").toFile();

        LifePathIO.saveAction(wrongCost, file, false);
        LifePath reloaded = LifePathIO.readLifePathFromFile(file);

        assertEquals(200, reloaded.xpCost(), "A stored cost that disagrees with the contents should be recomputed.");
    }

    @Test
    void testRead_LeavesACorrectXPCostAlone() throws IOException {
        LifePathBuilder builder = validBuilder().fixedXPSkills(Map.of(0, Map.of("Gunnery/Mek", 200)));
        LifePath correctCost = builder.xpCost(LifePathXPCostCalculator.calculateXPCost(builder)).build();
        File file = temporaryDirectory.resolve("correct-cost.json").toFile();

        LifePathIO.saveAction(correctCost, file, false);
        LifePath reloaded = LifePathIO.readLifePathFromFile(file);

        assertEquals(correctCost.xpCost(), reloaded.xpCost(), "A cost that already agrees should be left alone.");
        assertEquals(correctCost, reloaded, "Nothing else should change either.");
    }

    // endregion Recomputed XP cost

    // region File naming

    @Test
    void testWriteWithoutDialog_NamesTheFileAfterTheLifePath() {
        LifePath lifePath = validBuilder().name("Test Path").build();

        LifePathIO.writeToJSONWithoutDialog(lifePath, temporaryDirectory.toString(), false);

        assertTrue(temporaryDirectory.resolve("Test Path.json").toFile().exists(),
              "The file should be named after the Life Path.");
    }

    @Test
    void testWriteWithoutDialog_StripsCharactersAFileNameCannotHold() {
        LifePath lifePath = validBuilder().name("Bad/Name:With*Characters?").build();

        LifePathIO.writeToJSONWithoutDialog(lifePath, temporaryDirectory.toString(), false);

        File[] written = temporaryDirectory.toFile().listFiles();

        assertEquals(1, written == null ? 0 : written.length, "Exactly one file should have been written.");
        assertEquals("BadNameWithCharacters.json", written[0].getName(),
              "Characters a file name cannot hold should be removed.");
    }

    @Test
    void testWriteWithoutDialog_CreatesTheDirectory() {
        LifePath lifePath = validBuilder().build();
        Path nested = temporaryDirectory.resolve("does").resolve("not").resolve("exist");

        LifePathIO.writeToJSONWithoutDialog(lifePath, nested.toString(), false);

        assertTrue(nested.toFile().isDirectory(), "A missing directory should be created rather than failing.");
    }

    @Test
    void testWriteWithoutDialog_AndReadBack_RoundTrips() throws IOException {
        UUID id = UUID.randomUUID();
        LifePath lifePath = validBuilder().id(id).name("Directory Round Trip").build();

        LifePathIO.writeToJSONWithoutDialog(lifePath, temporaryDirectory.toString(), false);
        File file = temporaryDirectory.resolve("Directory Round Trip.json").toFile();

        assertEquals(id, LifePathIO.readLifePathFromFile(file).id(),
              "Writing into a directory and reading the file back should round trip.");
    }

    // endregion File naming
}
