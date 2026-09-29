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
package mekhq.gui.developerTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.factionStanding.FactionStandingUltimatumData;
import mekhq.campaign.universe.factionStanding.FactionStandingUltimatumSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the validation and parsing helpers in {@link FactionStandingUltimatumEditorDialog}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class FactionStandingUltimatumEditorDialogTest {
    private static final FactionStandingUltimatumSide LYRAN_SIDE = new FactionStandingUltimatumSide("LA",
          "Challenger", PersonnelRole.NOBLE, "LA");
    private static final FactionStandingUltimatumSide DAVION_SIDE = new FactionStandingUltimatumSide("FS",
          "Incumbent", PersonnelRole.NOBLE, "FS");

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static FactionStandingUltimatumData buildUltimatum(String name, String date,
          List<String> affectedFactionCodes, List<FactionStandingUltimatumSide> sides, String dissenterPreference,
          int divisiveness) {
        return new FactionStandingUltimatumData(name, date, null, affectedFactionCodes, sides, dissenterPreference,
              false, divisiveness);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static FactionStandingUltimatumData buildValidUltimatum(List<FactionStandingUltimatumSide> sides,
          String dissenterPreference) {
        return buildUltimatum("FED_COM_CIVIL_WAR", "3057-09-18", List.of("FC"), sides, dissenterPreference, 0);
    }

    @Test
    @DisplayName("Complete ultimatums with one, two, or three sides have no problems")
    void testValidUltimatumsHaveNoProblems() {
        FactionStandingUltimatumSide thirdSide = new FactionStandingUltimatumSide("CS", "Third", PersonnelRole.NOBLE,
              "CS");

        assertTrue(FactionStandingUltimatumEditorDialog.findProblems(
              buildValidUltimatum(List.of(LYRAN_SIDE), FactionStandingUltimatumData.ROGUE_PREFERENCE)).isEmpty());
        assertTrue(FactionStandingUltimatumEditorDialog.findProblems(
              buildValidUltimatum(List.of(LYRAN_SIDE, DAVION_SIDE), "FS")).isEmpty());
        assertTrue(FactionStandingUltimatumEditorDialog.findProblems(
              buildValidUltimatum(List.of(LYRAN_SIDE, DAVION_SIDE, thirdSide), "CS")).isEmpty());
    }

    @Test
    @DisplayName("Names must be upper-case identifiers")
    void testInvalidNameIsReported() {
        for (String name : List.of("", "fedCom", "FED COM", "FED-COM")) {
            FactionStandingUltimatumData ultimatum = buildUltimatum(name, "3057-09-18", List.of("FC"),
                  List.of(LYRAN_SIDE), "LA", 0);

            assertEquals(List.of("ultimatumEditor.problem.name"),
                  FactionStandingUltimatumEditorDialog.findProblems(ultimatum), name);
        }
    }

    @Test
    @DisplayName("Dates must be real yyyy-MM-dd dates")
    void testInvalidDateIsReported() {
        for (String date : List.of("", "18/09/3057", "3057-02-30")) {
            FactionStandingUltimatumData ultimatum = buildUltimatum("TEST", date, List.of("FC"), List.of(LYRAN_SIDE),
                  "LA", 0);

            assertEquals(List.of("ultimatumEditor.problem.date"),
                  FactionStandingUltimatumEditorDialog.findProblems(ultimatum), date);
        }
    }

    @Test
    @DisplayName("An ultimatum needs at least one side, and the dissenter must prefer one of them or ROGUE")
    void testNoSidesIsReported() {
        FactionStandingUltimatumData ultimatum = buildValidUltimatum(List.of(), "LA");

        assertEquals(List.of("ultimatumEditor.problem.noSides", "ultimatumEditor.problem.dissenterPreference"),
              FactionStandingUltimatumEditorDialog.findProblems(ultimatum));
    }

    @Test
    @DisplayName("Incomplete sides are reported")
    void testIncompleteSideIsReported() {
        for (FactionStandingUltimatumSide side : List.of(
              new FactionStandingUltimatumSide("la", "Leader", PersonnelRole.NOBLE, "LA"),
              new FactionStandingUltimatumSide("LA", " ", PersonnelRole.NOBLE, "LA"),
              new FactionStandingUltimatumSide("LA", "Leader", null, "LA"),
              new FactionStandingUltimatumSide("LA", "Leader", PersonnelRole.NOBLE, ""))) {
            FactionStandingUltimatumData ultimatum = buildValidUltimatum(List.of(side, DAVION_SIDE), "FS");

            assertEquals(List.of("ultimatumEditor.problem.side"),
                  FactionStandingUltimatumEditorDialog.findProblems(ultimatum), side.toString());
        }
    }

    @Test
    @DisplayName("Repeated side IDs and repeated factions are reported")
    void testDuplicatesAreReported() {
        FactionStandingUltimatumSide sameIdSide = new FactionStandingUltimatumSide("LA", "Other", PersonnelRole.NOBLE,
              "CS");
        FactionStandingUltimatumSide sameFactionSide = new FactionStandingUltimatumSide("LA2", "Other",
              PersonnelRole.NOBLE, "LA");

        assertEquals(List.of("ultimatumEditor.problem.duplicateSideId"),
              FactionStandingUltimatumEditorDialog.findProblems(
                    buildValidUltimatum(List.of(LYRAN_SIDE, sameIdSide), "LA")));
        assertEquals(List.of("ultimatumEditor.problem.sameFaction"),
              FactionStandingUltimatumEditorDialog.findProblems(
                    buildValidUltimatum(List.of(LYRAN_SIDE, sameFactionSide), "LA")));
    }

    @Test
    @DisplayName("A dissenter preference that names no side is reported")
    void testUnknownDissenterPreferenceIsReported() {
        for (String preference : new String[] { "DC", "rogue" }) {
            assertEquals(List.of("ultimatumEditor.problem.dissenterPreference"),
                  FactionStandingUltimatumEditorDialog.findProblems(
                        buildValidUltimatum(List.of(LYRAN_SIDE, DAVION_SIDE), preference)), preference);
        }
    }

    @Test
    @DisplayName("Divisiveness outside -6 to +6 is reported; the limits themselves are allowed")
    void testDivisivenessRange() {
        for (int divisiveness : new int[] { -7, 7 }) {
            FactionStandingUltimatumData ultimatum = buildUltimatum("TEST", "3057-09-18", List.of("FC"),
                  List.of(LYRAN_SIDE), "LA", divisiveness);

            assertEquals(List.of("ultimatumEditor.problem.divisiveness"),
                  FactionStandingUltimatumEditorDialog.findProblems(ultimatum), String.valueOf(divisiveness));
        }

        for (int divisiveness : new int[] { FactionStandingUltimatumData.MINIMUM_DIVISIVENESS,
                                            FactionStandingUltimatumData.MAXIMUM_DIVISIVENESS }) {
            FactionStandingUltimatumData ultimatum = buildUltimatum("TEST", "3057-09-18", List.of("FC"),
                  List.of(LYRAN_SIDE), "LA", divisiveness);

            assertTrue(FactionStandingUltimatumEditorDialog.findProblems(ultimatum).isEmpty(),
                  String.valueOf(divisiveness));
        }
    }

    @Test
    @DisplayName("getSideIds lists side IDs in order")
    void testGetSideIds() {
        assertEquals(List.of("LA", "FS"),
              FactionStandingUltimatumEditorDialog.getSideIds(buildValidUltimatum(List.of(LYRAN_SIDE, DAVION_SIDE),
                    "FS")));
    }

    @Test
    @DisplayName("parseLines keeps non-blank lines, trimmed")
    void testParseLines() {
        assertEquals(List.of("TH", "SLIE", "CS"),
              FactionStandingUltimatumEditorDialog.parseLines(" TH \n\nSLIE\r\n  \nCS"));
        assertTrue(FactionStandingUltimatumEditorDialog.parseLines("").isEmpty());
    }

    @Test
    @DisplayName("The Text Keys table lists scenes then each side, and finds existing text")
    void testBuildTextKeyRowsForShippedUltimatum() {
        List<FactionStandingUltimatumEditorDialog.TextKeyRow> rows = FactionStandingUltimatumEditorDialog
                                                                           .buildTextKeyRows(buildValidUltimatum(
                                                                                 List.of(LYRAN_SIDE, DAVION_SIDE),
                                                                                 "FS"));

        assertEquals(9, rows.size());
        assertEquals("FactionStandingUltimatumDialog.FED_COM_CIVIL_WAR.initialOffer", rows.get(0).key());
        assertEquals("FactionStandingUltimatumDialog.FED_COM_CIVIL_WAR.side.LA.pitch", rows.get(3).key());
        assertEquals("FactionStandingUltimatumDialog.FED_COM_CIVIL_WAR.side.FS.news", rows.get(6).key());
        assertEquals("FactionStandingUltimatumDialog.FED_COM_CIVIL_WAR.rogue.pirate.pitch", rows.get(8).key());
        for (FactionStandingUltimatumEditorDialog.TextKeyRow row : rows) {
            assertTrue(row.isWritten(), row.key());
            assertFalse(row.purpose().isBlank(), row.key());
        }
        assertEquals("", FactionStandingUltimatumEditorDialog.buildMissingKeysSnippet("FED_COM_CIVIL_WAR", rows));
    }

    @Test
    @DisplayName("Missing keys are copied as ready-to-fill lines under the ultimatum's name")
    void testMissingKeysSnippet() {
        FactionStandingUltimatumData ultimatum = buildUltimatum("UNWRITTEN_TEST", "3057-09-18", List.of("FC"),
              List.of(LYRAN_SIDE), "LA", 0);

        List<FactionStandingUltimatumEditorDialog.TextKeyRow> rows =
              FactionStandingUltimatumEditorDialog.buildTextKeyRows(ultimatum);

        assertEquals("""
                    # UNWRITTEN_TEST
                    FactionStandingUltimatumDialog.UNWRITTEN_TEST.initialOffer=
                    FactionStandingUltimatumDialog.UNWRITTEN_TEST.for=
                    FactionStandingUltimatumDialog.UNWRITTEN_TEST.against=
                    FactionStandingUltimatumDialog.UNWRITTEN_TEST.side.LA.pitch=
                    FactionStandingUltimatumDialog.UNWRITTEN_TEST.side.LA.news=
                    FactionStandingUltimatumDialog.UNWRITTEN_TEST.rogue.mercenary.pitch=
                    FactionStandingUltimatumDialog.UNWRITTEN_TEST.rogue.pirate.pitch=
                    """, FactionStandingUltimatumEditorDialog.buildMissingKeysSnippet("UNWRITTEN_TEST", rows));
    }

    @Test
    @DisplayName("A bundle name maps to its properties file in the MekHQ repository")
    void testGetBundleFilePath() {
        assertEquals("MekHQ/resources/mekhq/resources/FactionStandingUltimatumDialog.properties",
              FactionStandingUltimatumEditorDialog.getBundleFilePath(
                    "mekhq.resources.FactionStandingUltimatumDialog"));
    }
}
