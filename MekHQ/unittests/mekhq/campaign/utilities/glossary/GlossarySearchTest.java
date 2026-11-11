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
package mekhq.campaign.utilities.glossary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import mekhq.campaign.utilities.glossary.GlossarySearch.Document;
import mekhq.campaign.utilities.glossary.GlossarySearch.Hit;
import mekhq.campaign.utilities.glossary.GlossarySearch.Where;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GlossarySearch}: players must be able to find an entry from words in its text as well as its title,
 * with title matches first.
 */
class GlossarySearchTest {
    private static final Document HR_STRAIN = new Document("HR_STRAIN", "HR Strain",
          "HR Strain measures how much paperwork your administrators can handle before morale suffers.");
    private static final Document SECURITY_DUTY = new Document("SECURITY_DUTY", "Mission Type: Security Duty",
          "Your unit acts as a bodyguard for an important client.");
    private static final Document CONTRACTS = new Document("CONTRACTS", "Contracts",
          "Contracts are offered by employers. A contract has terms you can negotiate.");
    private static final List<Document> DOCUMENTS = List.of(HR_STRAIN, SECURITY_DUTY, CONTRACTS);

    @Test
    void aQuotedPhraseIsOneTerm() {
        assertEquals(List.of("hr strain", "morale"), GlossarySearch.parseQuery("\"HR Strain\" Morale"));
    }

    @Test
    void singleLetterTermsAreDropped() {
        assertEquals(List.of("strain"), GlossarySearch.parseQuery("a strain"));
        assertTrue(GlossarySearch.search(DOCUMENTS, " x ", true).isEmpty());
    }

    @Test
    void aWordOnlyInTheTextFindsTheEntry() {
        List<Hit> hits = GlossarySearch.search(DOCUMENTS, "paperwork", true);

        assertEquals(1, hits.size());
        assertEquals(HR_STRAIN, hits.get(0).document());
        assertEquals(Where.TEXT, hits.get(0).where());
    }

    @Test
    void textIsNotSearchedWhenTurnedOff() {
        assertTrue(GlossarySearch.search(DOCUMENTS, "paperwork", false).isEmpty());
    }

    @Test
    void everyTermMustAppear() {
        assertTrue(GlossarySearch.search(DOCUMENTS, "bodyguard paperwork", true).isEmpty());
    }

    @Test
    void aTitleMatchRanksAboveATextMatch() {
        // HR Strain mentions morale in its text; the Morale entry has it as its title.
        Document morale = new Document("MORALE", "Morale", "How willing your people are to fight.");
        List<Hit> hits = GlossarySearch.search(List.of(HR_STRAIN, morale), "morale", true);

        assertEquals(2, hits.size());
        assertEquals(morale, hits.get(0).document());
        assertEquals(Where.TITLE, hits.get(0).where());
        assertEquals(Where.TEXT, hits.get(1).where());
    }

    @Test
    void aPluralFindsItsSingular() {
        assertEquals(List.of("contracts", "contract"), GlossarySearch.variants("contracts"));
        // The text says "negotiate"; the player typed "negotiates".
        List<Hit> hits = GlossarySearch.search(List.of(SECURITY_DUTY, CONTRACTS), "negotiates", true);
        assertEquals(1, hits.size());
        assertEquals(CONTRACTS, hits.get(0).document());
    }

    @Test
    void theSnippetShowsWhereTheWordsAppearTogether() {
        Document document = new Document("KEY", "Title",
              "Some units are mentioned early. Much later in the text there is a unit auction for players.");

        String snippet = GlossarySearch.snippet(document, List.of("unit", "auction"), 20, 60);

        assertTrue(snippet.contains("unit auction"), snippet);
        assertTrue(snippet.startsWith("\u2026"), "a snippet cut from the middle starts with an ellipsis");
    }

    @Test
    void theSnippetIsEmptyWhenNoTermIsInTheText() {
        assertEquals("", GlossarySearch.snippet(HR_STRAIN, List.of("bodyguard"), 20, 60));
    }

    @Test
    void theHighlightPatternMatchesLongerFormsFirstAndIgnoresCase() {
        Pattern pattern = GlossarySearch.highlightPattern(List.of("contracts")).orElseThrow();

        assertTrue(pattern.matcher("CONTRACTS").find());
        assertTrue(pattern.matcher("a contract").find());
        assertFalse(GlossarySearch.highlightPattern(List.of()).isPresent());
    }
}
