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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Guards {@link ScenarioModifierEditorDialog#insertEntryPreservingComments(String, String)} — the comment-preserving
 * append used by the modifier editor's "Add to Manifest". The shipped {@code modifiermanifest.json} carries curated
 * {@code #} notes documenting disabled modifiers, so appending must not disturb them.
 */
class ScenarioModifierManifestAppendTest {

    /** A manifest fragment shaped like the shipped file: header comment, entries, and interspersed section comments. */
    private static final String MANIFEST = """
          # This is *all* possible scenario modifiers.
          {
            "fileNameList": [
              "EnemyAirSupport.json",
              # Removed by popular demand: EnemyDropShip.json
              "GoodIntel.json",
              # Facility objective modifiers.
              "FacilityHostileDestroy.json"
            ]
          }
          """;

    private static long commentLines(String text) {
        return text.lines().filter(line -> line.strip().startsWith("#")).count();
    }

    private static long entryLines(String text) {
        return Arrays.stream(text.split("\n")).filter(line -> line.strip().matches("\"[^\"]*\",?")).count();
    }

    @Test
    void appendPreservesCommentsAndAddsTrailingComma() {
        long originalComments = commentLines(MANIFEST);
        long originalEntries = entryLines(MANIFEST);

        String updated = ScenarioModifierEditorDialog.insertEntryPreservingComments(MANIFEST, "MyNewModifier.json");

        assertNotNull(updated, "appending a new entry returns the updated text");
        assertEquals(originalComments, commentLines(updated), "every comment line must be preserved");
        assertEquals(originalEntries + 1, entryLines(updated), "exactly one entry is added");
        assertTrue(updated.contains("\"MyNewModifier.json\""), "the new entry is present");
        // the previous last element must gain a trailing comma so the array stays valid JSON
        assertTrue(updated.contains("\"FacilityHostileDestroy.json\","),
              "the prior last element should gain a trailing comma");
        // section comments must not be dragged out of position
        assertTrue(updated.contains("# Facility objective modifiers."));
        assertTrue(updated.contains("# Removed by popular demand: EnemyDropShip.json"));
    }

    @Test
    void appendedEntryComesAfterTheLastElementNotAfterATrailingComment() {
        // insertion anchors on the last quoted element, so a trailing comment before ']' stays put
        String manifest = """
              {
                "fileNameList": [
                  "First.json"
                  # a trailing note that must stay before the bracket
                ]
              }
              """;
        String updated = ScenarioModifierEditorDialog.insertEntryPreservingComments(manifest, "Second.json");
        assertNotNull(updated);
        int firstIdx = updated.indexOf("\"First.json\"");
        int secondIdx = updated.indexOf("\"Second.json\"");
        int noteIdx = updated.indexOf("# a trailing note");
        int closeIdx = updated.indexOf(']');
        assertTrue(firstIdx < secondIdx, "new entry follows the previous entry");
        assertTrue(secondIdx < noteIdx, "new entry is inserted before the trailing comment");
        assertTrue(noteIdx < closeIdx, "the trailing comment stays before the closing bracket");
    }

    @Test
    void appendIsIdempotentForAnAlreadyPresentEntry() {
        assertNull(ScenarioModifierEditorDialog.insertEntryPreservingComments(MANIFEST, "GoodIntel.json"),
              "an entry already present as a quoted token returns null");
    }

    @Test
    void commentMentionOfANameIsNotTreatedAsPresent() {
        // the name appears only inside a '#' comment (unquoted), so it must still be appendable
        String updated = ScenarioModifierEditorDialog.insertEntryPreservingComments(MANIFEST, "EnemyDropShip.json");
        assertNotNull(updated, "a name mentioned only in a comment is not an existing entry");
        assertTrue(updated.contains("\"EnemyDropShip.json\""));
    }

    @Test
    void appendIntoEmptyArray() {
        String manifest = """
              {
                "fileNameList": [
                ]
              }
              """;
        String updated = ScenarioModifierEditorDialog.insertEntryPreservingComments(manifest, "First.json");
        assertNotNull(updated);
        assertEquals(1, entryLines(updated));
        assertTrue(updated.contains("\"First.json\""));
    }
}
