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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

/**
 * One damaged or unfamiliar value in a save, such as one written by a newer version, must cost as little as possible:
 * never the rest of the cast, a whole plot thread or the rest of the journal.
 */
class RoleplaySaveResilienceTest {
    private static Roleplay load(final String xml) throws Exception {
        Node node = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                          .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                          .getDocumentElement();
        return Roleplay.generateInstanceFromXML(node);
    }

    @Test
    void anUnknownRatingKeepsTheCharacter() throws Exception {
        Roleplay roleplay = load("<roleplay><characters><character><id>00000000-0000-0000-0000-000000000001</id>"
                                       + "<name>Guard</name><rating>MYTHIC</rating></character></characters>"
                                       + "</roleplay>");

        assertEquals(1, roleplay.getCharacters().size());
        assertEquals("Guard", roleplay.getCharacters().get(0).getName());
        assertNull(roleplay.getCharacters().get(0).getRating());
    }

    @Test
    void oneUnreadableCharacterDoesNotLoseTheRestOfTheCast() throws Exception {
        Roleplay roleplay = load("<roleplay><characters>"
                                       + "<character><id>not-a-uuid</id><name>Broken</name></character>"
                                       + "<character><name>Natasha</name><rating>ELITE</rating></character>"
                                       + "</characters></roleplay>");

        assertEquals(1, roleplay.getCharacters().size());
        assertEquals("Natasha", roleplay.getCharacters().get(0).getName());
        assertEquals(NpcRating.ELITE, roleplay.getCharacters().get(0).getRating());
    }

    @Test
    void anUnknownOracleTableIsSkippedAndTheThreadKept() throws Exception {
        Roleplay roleplay = load("<roleplay><plotThreads><plotThread><name>Ghost</name><length>SHORT</length>"
                                       + "<revealedSteps>1</revealedSteps><steps><step>"
                                       + "<concept><table>THEMES_RETIRED</table><meaning>gone</meaning></concept>"
                                       + "<concept><table>THEMES_FEAR</table><meaning>dread</meaning></concept>"
                                       + "</step></steps></plotThread></plotThreads></roleplay>");

        PlotThread thread = roleplay.getPlotThreads().get(0);
        List<Concepts.Concept> concepts = thread.getSteps().get(0).concepts();
        assertEquals(1, concepts.size());
        assertEquals(OracleTable.THEMES_FEAR, concepts.get(0).table());
    }

    @Test
    void anUnreadableRevealDateBecomesUnknown() throws Exception {
        Roleplay roleplay = load("<roleplay><plotThreads><plotThread><name>Ghost</name><length>SHORT</length>"
                                       + "<revealedSteps>2</revealedSteps><revealedOn>soon</revealedOn>"
                                       + "<revealedOn>3025-06-01</revealedOn><steps><step/><step/><step/></steps>"
                                       + "</plotThread></plotThreads></roleplay>");

        PlotThread thread = roleplay.getPlotThreads().get(0);
        assertNull(thread.getRevealDate(1));
        assertEquals(LocalDate.of(3025, 6, 1), thread.getRevealDate(2));
    }

    @Test
    void anUnknownEntryTypeDropsOnlyThatEntry() throws Exception {
        Roleplay roleplay = load("<roleplay><oracleLog>"
                                       + "<entry><date>3025-06-01</date><type>DREAM</type><text>?</text></entry>"
                                       + "<entry><date>3025-06-02</date><type>CONCEPTS</type><text>ok</text></entry>"
                                       + "</oracleLog></roleplay>");

        assertEquals(1, roleplay.getOracleLog().size());
        assertEquals("ok", roleplay.getOracleLog().get(0).getText());
    }

    @Test
    void aCheckWithNoSidesIsDroppedButTheEntryKept() throws Exception {
        Roleplay roleplay = load("<roleplay><oracleLog><entry><date>3025-06-01</date><type>CHECK</type>"
                                       + "<text>Rolled</text><check><reason>x</reason></check></entry>"
                                       + "</oracleLog></roleplay>");

        JournalEntry entry = roleplay.getOracleLog().get(0);
        assertEquals(JournalEntryType.CHECK, entry.getType());
        assertNull(entry.getCheck());
    }

    @Test
    void checkDiceSurviveMissingOrOddValues() throws Exception {
        Roleplay roleplay = load("<roleplay><oracleLog><entry><date>3025-06-01</date><type>CHECK</type>"
                                       + "<text>Rolled</text><check><side><name>Rook</name><roll>9</roll>"
                                       + "<dice>4, 5,</dice><margin>2</margin><won>true</won></side></check>"
                                       + "</entry></oracleLog></roleplay>");

        CheckRecord check = roleplay.getOracleLog().get(0).getCheck();
        assertNotNull(check);
        CheckRecord.Side side = check.sides().get(0);
        assertEquals(List.of(4, 5), side.dice());
        assertNull(side.personId());
        assertTrue(side.won());
        assertEquals("", check.reason());
    }
}
