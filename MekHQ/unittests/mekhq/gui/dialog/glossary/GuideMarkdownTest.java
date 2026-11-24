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
package mekhq.gui.dialog.glossary;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mekhq.campaign.utilities.glossary.DocumentationEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GuideMarkdownTest {
    private static final Pattern IN_PAGE_LINK = Pattern.compile("href=\"#([^\"]+)\"");

    @Test
    void tablesBecomeTablesWithoutSections() {
        String html = GuideMarkdown.toHtml("| Code | Name |\n|------|------|\n| `MAF` | Magistracy |\n");

        assertTrue(html.contains("<table>"), html);
        assertTrue(html.contains("<th>Code</th>"), html);
        assertTrue(html.contains("<td><code>MAF</code></td>"), html);
        assertFalse(html.contains("thead") || html.contains("tbody"), html);
    }

    @Test
    void openingTitleIsDroppedAndLaterHeadingsAreKept() {
        String html = GuideMarkdown.toHtml("# Ranks in MekHQ\n\nIntro.\n\n## Professions\n\n# Not the opening\n");

        assertFalse(html.contains("Ranks in MekHQ"), html);
        assertTrue(html.contains("Professions</h2>"), html);
        assertTrue(html.contains("Not the opening</h1>"), html);
    }

    @Test
    void headingsGetTheirGitHubAnchors() {
        String html = GuideMarkdown.toHtml("## How a Character's Rank Name Is Built\n\n### The `SSLDF` System\n");

        assertTrue(html.contains("<a name='how-a-characters-rank-name-is-built'></a>"), html);
        assertTrue(html.contains("<a name='the-ssldf-system'></a>"), html);
    }

    @ParameterizedTest
    @EnumSource(DocumentationEntry.class)
    void everyGuideReadsAndItsInPageLinksLand(DocumentationEntry entry) throws IOException {
        String html = GuideMarkdown.read(entry);

        assertFalse(html.isBlank(), entry + " has no text");
        Matcher link = IN_PAGE_LINK.matcher(html);
        while (link.find()) {
            assertTrue(html.contains("<a name='" + link.group(1) + "'></a>"),
                  entry + " links to #" + link.group(1) + ", which is not a heading");
        }
    }
}
