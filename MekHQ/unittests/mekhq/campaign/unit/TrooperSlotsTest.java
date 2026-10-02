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
package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

/**
 * The rules for putting people in battle armor suits (issue #10257).
 */
class TrooperSlotsTest {
    private final UUID alice = UUID.randomUUID();
    private final UUID bruno = UUID.randomUUID();
    private final UUID chen = UUID.randomUUID();

    @Test
    void newcomersTakeTheBestFreeSuitsInOrder() {
        TrooperSlots trooperSlots = new TrooperSlots();

        List<Integer> wornSlots = trooperSlots.seat(List.of(alice, bruno), List.of(3, 1, 2));

        assertEquals(List.of(3, 1), wornSlots);
        assertEquals(alice, trooperSlots.getOccupant(3));
        assertEquals(bruno, trooperSlots.getOccupant(1));
    }

    @Test
    void aPersonKeepsTheirSuitEvenWhenABetterOneIsFree() {
        TrooperSlots trooperSlots = new TrooperSlots();
        trooperSlots.seat(List.of(alice), List.of(2, 1));

        trooperSlots.seat(List.of(alice, bruno), List.of(1, 2));

        assertEquals(2, trooperSlots.findSlot(alice));
        assertEquals(1, trooperSlots.findSlot(bruno));
    }

    @Test
    void aPersonWhoseSuitCannotBeWornMovesToAFreeOne() {
        TrooperSlots trooperSlots = new TrooperSlots();
        trooperSlots.seat(List.of(alice, bruno), List.of(1, 2, 3));

        trooperSlots.seat(List.of(alice, bruno), List.of(2, 3));

        assertEquals(2, trooperSlots.findSlot(bruno), "Bruno keeps suit 2");
        assertEquals(3, trooperSlots.findSlot(alice), "Alice leaves the lost suit 1 for suit 3");
    }

    @Test
    void aPersonNoLongerInTheCrewLosesTheirSuit() {
        TrooperSlots trooperSlots = new TrooperSlots();
        trooperSlots.seat(List.of(alice, bruno, chen), List.of(1, 2, 3));

        List<Integer> wornSlots = trooperSlots.seat(List.of(alice, chen), List.of(1, 2, 3));

        assertEquals(List.of(1, 3), wornSlots);
        assertNull(trooperSlots.findSlot(bruno));
    }

    @Test
    void releaseEmptiesOnlyThatPersonsSuit() {
        TrooperSlots trooperSlots = new TrooperSlots();
        trooperSlots.seat(List.of(alice, bruno), List.of(1, 2));

        trooperSlots.release(alice);

        assertNull(trooperSlots.getOccupant(1));
        assertEquals(bruno, trooperSlots.getOccupant(2));
    }

    @Test
    void assignmentsSurviveASaveAndLoadAndBadEntriesAreSkipped() throws Exception {
        TrooperSlots trooperSlots = new TrooperSlots();
        trooperSlots.seat(List.of(alice, bruno), List.of(4, 2));
        StringWriter savedXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(savedXml);
        trooperSlots.writeToXML(printWriter, 0);
        printWriter.flush();
        String withABadEntry = savedXml.toString()
                                     .replace("</trooperSlots>",
                                           "<trooperSlot><slot>x</slot><personId>" + chen
                                                 + "</personId></trooperSlot></trooperSlots>");
        Element trooperSlotsElement = MHQXMLUtility.newSafeDocumentBuilder()
                                            .parse(new ByteArrayInputStream(
                                                  withABadEntry.getBytes(StandardCharsets.UTF_8)))
                                            .getDocumentElement();
        assertTrue(TrooperSlots.isTrooperSlotsNode(trooperSlotsElement.getNodeName()));

        TrooperSlots loadedSlots = new TrooperSlots();
        loadedSlots.readFromXML(trooperSlotsElement);

        assertEquals(alice, loadedSlots.getOccupant(4));
        assertEquals(bruno, loadedSlots.getOccupant(2));
        assertNull(loadedSlots.findSlot(chen), "The unreadable entry is skipped");
    }

    @Test
    void nothingIsWrittenWhenNobodyWearsASuit() {
        StringWriter savedXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(savedXml);

        new TrooperSlots().writeToXML(printWriter, 0);
        printWriter.flush();

        assertEquals("", savedXml.toString());
    }
}
