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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link FactionStandingUltimatumData}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class FactionStandingUltimatumDataTest {
    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static FactionStandingUltimatumData buildUltimatum(String date, List<String> affectedFactionCodes) {
        return new FactionStandingUltimatumData("TEST", date, null, affectedFactionCodes, null, null, false, 0);
    }

    @Test
    @DisplayName("getDate parses the ISO date string")
    void testGetDateParsesIsoDate() {
        assertEquals(LocalDate.of(3057, 9, 18), buildUltimatum("3057-09-18", List.of("FC")).getDate());
    }

    @Test
    @DisplayName("getDate rejects a malformed date")
    void testGetDateRejectsMalformedDate() {
        FactionStandingUltimatumData ultimatum = buildUltimatum("18/09/3057", List.of("FC"));

        assertThrows(DateTimeParseException.class, ultimatum::getDate);
    }

    @Test
    @DisplayName("Null affected faction codes become an empty list")
    void testNullAffectedFactionCodesBecomeEmpty() {
        assertTrue(buildUltimatum("3057-09-18", null).affectedFactionCodes().isEmpty());
    }

    @Test
    @DisplayName("Affected faction codes are copied and cannot be modified")
    void testAffectedFactionCodesAreImmutableCopy() {
        List<String> source = new ArrayList<>(List.of("FC"));
        FactionStandingUltimatumData ultimatum = buildUltimatum("3057-09-18", source);
        source.add("LA");

        assertEquals(List.of("FC"), ultimatum.affectedFactionCodes());
        assertThrows(UnsupportedOperationException.class, () -> ultimatum.affectedFactionCodes().add("LA"));
    }

    @Test
    @DisplayName("An ultimatum survives a save and reload unchanged, led by the license header")
    void testSerializeRoundTrip(@TempDir Path temporaryDirectory) throws IOException {
        FactionStandingUltimatumSide lyranSide = new FactionStandingUltimatumSide("LA", "Archon Katrina Steiner",
              PersonnelRole.NOBLE, "LA");
        FactionStandingUltimatumSide davionSide = new FactionStandingUltimatumSide("FS",
              "Archon-Prince Victor Steiner-Davion", PersonnelRole.NOBLE, "FS");
        FactionStandingUltimatumData ultimatum = new FactionStandingUltimatumData("FED_COM_CIVIL_WAR", "3057-09-18",
              "Some notes", List.of("FC"), List.of(lyranSide, davionSide), "FS", true, 3);
        File outputFile = temporaryDirectory.resolve("FED_COM_CIVIL_WAR.json").toFile();

        assertTrue(ultimatum.serialize(outputFile));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(content.startsWith("# MegaMek Data"));
        assertTrue(content.contains("\"isViolentTransition\" : true"));
        assertTrue(content.contains("\"divisiveness\" : 3"));
        assertTrue(content.contains("\"dissenterPreference\" : \"FS\""));
        assertEquals(ultimatum, FactionStandingUltimatumData.deserialize(outputFile));
    }

    @Test
    @DisplayName("A file without divisiveness loads with a divisiveness of 0")
    void testMissingDivisivenessDefaultsToZero(@TempDir Path temporaryDirectory) throws IOException {
        Path ultimatumFile = temporaryDirectory.resolve("OLD.json");
        Files.writeString(ultimatumFile, """
              # A license header comment
              {
                "name": "OLD",
                "date": "3057-09-18",
                "affectedFactionCodes": [ "FC" ],
                "isViolentTransition": false
              }
              """, StandardCharsets.UTF_8);

        FactionStandingUltimatumData ultimatum = FactionStandingUltimatumData.deserialize(ultimatumFile.toFile());

        assertEquals(0, ultimatum.divisiveness());
    }

    @Test
    @DisplayName("Null sides become an empty list, and getSide finds sides by ID")
    void testSides() {
        FactionStandingUltimatumSide side = new FactionStandingUltimatumSide("FRR", "Haakon Magnusson",
              PersonnelRole.NOBLE, "FRR");

        assertTrue(buildUltimatum("3034-03-14", List.of("DC")).sides().isEmpty());
        FactionStandingUltimatumData ultimatum = new FactionStandingUltimatumData("TEST", "3034-03-14", null,
              List.of("DC"), List.of(side), FactionStandingUltimatumData.ROGUE_PREFERENCE, false, 0);
        assertEquals(side, ultimatum.getSide("FRR"));
        assertNull(ultimatum.getSide("DC"));
    }

    @Test
    @DisplayName("deserialize returns null for a missing file")
    void testDeserializeMissingFile(@TempDir Path temporaryDirectory) {
        assertNull(FactionStandingUltimatumData.deserialize(temporaryDirectory.resolve("missing.json").toFile()));
    }

    @Test
    @DisplayName("getSide tolerates a null ID and sides missing their ID")
    void testGetSideWithMissingIds() {
        FactionStandingUltimatumSide idlessSide = new FactionStandingUltimatumSide(null, "Leader", PersonnelRole.NOBLE,
              "LA");
        FactionStandingUltimatumSide side = new FactionStandingUltimatumSide("FS", "Leader", PersonnelRole.NOBLE,
              "FS");
        FactionStandingUltimatumData ultimatum = new FactionStandingUltimatumData("TEST", "3057-09-18", null,
              List.of("FC"), List.of(idlessSide, side), "FS", false, 0);

        assertNull(ultimatum.getSide(null));
        assertEquals(side, ultimatum.getSide("FS"));
        assertNull(ultimatum.getSide("LA"));
    }

    @Test
    @DisplayName("Divisiveness from a hand-edited file is clamped to -6..+6 in play, but kept as written")
    void testEffectiveDivisivenessIsClamped() {
        assertEquals(6, buildWithDivisiveness(20).getEffectiveDivisiveness());
        assertEquals(-6, buildWithDivisiveness(-20).getEffectiveDivisiveness());
        assertEquals(3, buildWithDivisiveness(3).getEffectiveDivisiveness());
        assertEquals(20, buildWithDivisiveness(20).divisiveness());
    }

    @Test
    @DisplayName("Saving writes only the real fields, not computed ones like the parsed date")
    void testSerializeWritesOnlyRealFields(@TempDir Path temporaryDirectory) throws IOException {
        File outputFile = temporaryDirectory.resolve("TEST.json").toFile();

        assertTrue(buildWithDivisiveness(20).serialize(outputFile));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertFalse(content.contains("effectiveDivisiveness"), content);
        assertEquals(1, content.split("\"date\"", -1).length - 1, "date must be written exactly once");
        assertEquals(20, FactionStandingUltimatumData.deserialize(outputFile).divisiveness());
    }

    @Test
    @DisplayName("A file that isn't JSON, or has a malformed date, is handled without throwing")
    void testMalformedFiles(@TempDir Path temporaryDirectory) throws IOException {
        Path notJson = temporaryDirectory.resolve("notJson.json");
        Files.writeString(notJson, "this is not json", StandardCharsets.UTF_8);
        Path badDate = temporaryDirectory.resolve("badDate.json");
        Files.writeString(badDate, "{ \"name\": \"BAD\", \"date\": \"3057-13-40\" }", StandardCharsets.UTF_8);

        assertNull(FactionStandingUltimatumData.deserialize(notJson.toFile()));
        FactionStandingUltimatumData loaded = FactionStandingUltimatumData.deserialize(badDate.toFile());
        assertNotNull(loaded);
        assertThrows(DateTimeParseException.class, loaded::getDate);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static FactionStandingUltimatumData buildWithDivisiveness(int divisiveness) {
        FactionStandingUltimatumSide side = new FactionStandingUltimatumSide("LA", "Leader", PersonnelRole.NOBLE,
              "LA");
        return new FactionStandingUltimatumData("TEST", "3057-09-18", null, List.of("FC"), List.of(side), "LA",
              false, divisiveness);
    }
}
