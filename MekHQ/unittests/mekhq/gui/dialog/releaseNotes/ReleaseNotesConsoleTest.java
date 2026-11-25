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

package mekhq.gui.dialog.releaseNotes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;

import megamek.client.ui.dialogs.releaseNotes.ReleaseNotes;
import org.junit.jupiter.api.Test;

class ReleaseNotesConsoleTest {
    private static final URI PAGE = URI.create("https://github.com/MegaMek/mekhq/releases");

    @Test
    void oneReleaseListsItsSectionsWithoutAVersionHeadingAndEscapesTitles() {
        ReleaseNotes notes = new ReleaseNotes("mekhq", PAGE, null, List.of(release("0.51.01.5", 5,
              "Fix <script> & 'quotes'")));

        String html = ReleaseNotesConsole.body(notes);

        assertFalse(html.contains("<h2>"), "one release needs no version heading");
        assertTrue(html.contains("<h3>BUG FIXES</h3>"));
        assertTrue(html.contains("Fix &lt;script&gt; &amp; &#39;quotes&#39;"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("href='https://github.com/MegaMek/mekhq/pull/5'>#5</a>"));
        assertEquals("MekHQ: 1 change in 1 release", ReleaseNotesConsole.summary(notes));
    }

    @Test
    void aRangeGetsAVersionHeadingPerReleaseNewestFirst() {
        ReleaseNotes notes = new ReleaseNotes("megamek", PAGE, "0.51.01.3", List.of(release("0.51.01.5", 5, "Five"),
              release("0.51.01.4", 4, "Four")));

        String html = ReleaseNotesConsole.body(notes);

        int five = html.indexOf(">0.51.01.5</a></h2>");
        int four = html.indexOf(">0.51.01.4</a></h2>");
        assertTrue((five >= 0) && (five < html.indexOf("Five")) && (html.indexOf("Five") < four)
              && (four < html.indexOf("Four")));
        assertEquals("MegaMek: 2 changes in 2 releases", ReleaseNotesConsole.summary(notes));
    }

    @Test
    void emptyNotesSaySoAndUngroupedChangesGetTheChangesHeading() {
        ReleaseNotes empty = new ReleaseNotes("megameklab", PAGE, null, List.of());
        assertTrue(ReleaseNotesConsole.body(empty).contains("No changes are listed for this version."));
        assertEquals("MegaMekLab: no changes in no releases", ReleaseNotesConsole.summary(empty));

        ReleaseNotes ungrouped = new ReleaseNotes("mekhq", PAGE, null, List.of(new ReleaseNotes.Release("0.51.01.5",
              PAGE, List.of(new ReleaseNotes.Section(ReleaseNotes.UNGROUPED, List.of(new ReleaseNotes.Change("Old",
              1, URI.create("https://github.com/MegaMek/mekhq/pull/1"))))))));
        assertTrue(ReleaseNotesConsole.body(ungrouped).contains("<h3>CHANGES</h3>"));
    }

    @Test
    void theSubtitleNamesTheStartOnlyWhenTheTabCoversARange() {
        ReleaseNotes range = new ReleaseNotes("megamek", PAGE, "0.51.01.3", List.of(release("0.51.01.5", 5, "Five")));
        ReleaseNotes single = new ReleaseNotes("mekhq", PAGE, null, List.of(release("0.51.01.5", 5, "Five")));

        assertEquals("Everything that changed since 0.51.01.3.", ReleaseNotesConsole.subtitle(range));
        assertEquals(ReleaseNotesConsole.subtitle(null), ReleaseNotesConsole.subtitle(single));
        assertFalse(ReleaseNotesConsole.subtitle(single).contains("since"));
    }

    @Test
    void tabsAreMekHqThenTheApplicationsItIncludes() {
        assertEquals(List.of("MekHQ", "MegaMek", "MegaMekLab"),
              ReleaseNotesConsole.REPOSITORIES.stream().map(ReleaseNotesConsole::productName).toList());
    }

    private static ReleaseNotes.Release release(String version, int pullRequest, String title) {
        return new ReleaseNotes.Release(version, URI.create("https://github.com/MegaMek/mekhq/releases/tag/v" + version),
              List.of(new ReleaseNotes.Section("Bug fixes", List.of(new ReleaseNotes.Change(title, pullRequest,
                    URI.create("https://github.com/MegaMek/mekhq/pull/" + pullRequest))))));
    }
}
