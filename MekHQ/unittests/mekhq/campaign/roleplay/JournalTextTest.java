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
package mekhq.campaign.roleplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JournalTextTest {
    @Test
    void escapesTheCharactersHtmlTreatsSpecially() {
        assertEquals("a &lt;b&gt; &amp; &quot;c&quot;", JournalText.escapeHtml("a <b> & \"c\""));
    }

    @Test
    void recognisesHtml() {
        assertTrue(JournalText.isHtml("  <HTML><body>x</body></HTML>"));
        assertFalse(JournalText.isHtml("plain <b>text</b>"));
    }

    @Test
    void bodyOfDropsTheHeadAndWrapper() {
        assertEquals("<p>Hi</p>", JournalText.bodyOf("<html><head><style>p {}</style></head><body><p>Hi</p>"
                                                           + "</body></html>"));
        assertEquals("loose", JournalText.bodyOf("<html>loose</html>"));
    }

    @Test
    void numericEntitiesBecomeTheirCharacters() {
        assertEquals("é and 😀", JournalText.htmlToPlain("<html><body>&#233; and &#128512;</body></html>"));
    }

    @Test
    void invalidNumericEntitiesAreLeftAlone() {
        assertEquals("&#99999999999; &#1114112;",
              JournalText.htmlToPlain("<html><body>&#99999999999; &#1114112;</body></html>"));
    }

    @Test
    void anEscapedAmpersandIsNotDecodedTwice() {
        assertEquals("&lt; means <", JournalText.htmlToPlain("<html><body>&amp;lt; means &lt;</body></html>"));
    }

    @Test
    void headingsAndEmphasisBecomeMarkdown() {
        String markdown = JournalText.htmlToMarkdown("<html><body><h3>Landfall</h3><p><b>Dawn</b> and <i>dust</i>"
                                                           + "</p></body></html>");
        assertEquals("### Landfall\n**Dawn** and *dust*", markdown);
    }
}
