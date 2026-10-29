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
package mekhq.gui.dialog.markets.contractMarket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.client.ui.util.DiscordFormat;
import mekhq.gui.dialog.markets.contractMarket.ContractReadout.Row;
import mekhq.gui.dialog.markets.contractMarket.ContractReadout.Section;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ContractDiscordFormatter}. Discord refuses a message over its length limit outright, so the
 * formatter must always stay within it, however long the briefing.
 */
class ContractDiscordFormatterTest {
    private static ContractReadout readout(String title, String briefing) {
        return new ContractReadout("Lyran Alliance (Planetary Government)",
              title,
              "Objective Raid · Galatea · 6-month term · 12 days transit",
              "Even Fight 5/10",
              List.of("Proving Ground"),
              "Leutnant Katrina Steiner",
              briefing,
              List.of(new Section("Terms",
                          List.of(new Row("Command Rights", "House", false), new Row("Salvage", "40%", false)),
                          List.of("Characteristics: Career Maker")),
                    new Section("Money",
                          List.of(new Row("Total Payout", "12,500,000 C-bills", true),
                                new Row("Estimated Net", "+4,200,000", true)),
                          List.of())),
              "Estimate only.");
    }

    @Test
    void theMessageNamesTheOfferAndHoldsItsStatsInAnAnsiBlock() {
        String message = ContractDiscordFormatter.format(readout("Operation Sundowner", "We need you on Galatea."));

        assertTrue(message.startsWith("**OPERATION SUNDOWNER**\n"));
        assertTrue(message.contains("```ansi\n"));
        assertTrue(message.endsWith("```"));
        assertTrue(message.contains("> We need you on Galatea."));
        assertTrue(message.contains("Command Rights"));
        assertTrue(message.contains("12,500,000 C-bills"));
        assertTrue(message.contains("Characteristics: Career Maker"));
    }

    @Test
    void aLongBriefingIsCutShortToFitDiscordsLimit() {
        String briefing = "The Combine has landed in force and we need every lance we can hire. ".repeat(200);

        String message = ContractDiscordFormatter.format(readout("Operation Sundowner", briefing));

        assertTrue(message.length() <= ContractDiscordFormatter.MAX_LENGTH, "length was " + message.length());
        assertTrue(message.contains("…\n"), "a shortened briefing ends in an ellipsis");
        assertTrue(message.endsWith("```"), "the stat block must survive a shortened briefing");
    }

    /** An offer with every row the dossier shows, including the money section's long operating-costs label. */
    private static ContractReadout fullDossier(String briefing) {
        return new ContractReadout("Draconis Combine (Planetary Government)",
              "Operation Iron Lantern",
              "Asset Retrieval · Luthien · 6-month term · 214 days transit",
              "Even Fight 5/10",
              List.of("Proving Ground", "Double Support Payouts"),
              "Tai-i Yatun Kartosuwiryo",
              briefing,
              List.of(new Section("Terms",
                          List.of(new Row("Command Rights", "Liaison", false),
                                new Row("Salvage", "40%", false),
                                new Row("Support", "60% straight support", false),
                                new Row("Transport", "75% reimbursed", false),
                                new Row("Term", "6 months", false),
                                new Row("Transit", "214 days", false)),
                          List.of("Characteristics: Career Maker, Cut Rate, Elite Negotiator")),
                    new Section("Intel",
                          List.of(new Row("Allied Command", "Veteran (B)", false),
                                new Row("Opposition", "Federated Suns (Davion Heavy Guards)", false),
                                new Row("Threat", "Elite (A)", false),
                                new Row("Morale", "Stalemate", false),
                                new Row("Recommended Formations", "4", false)),
                          List.of("Advisory: Irregular pay credentials.")),
                    new Section("Money",
                          List.of(new Row("Total Payout", "123,456,789 C-bills", true),
                                new Row("Net / Month", "+1,234,567", true),
                                new Row("Monthly × 6", "18,456,789", false),
                                new Row("Transport Pay", "12,345,678", false),
                                new Row("Operating Costs (6 mo + 214d transit)", "-98,765,432", false),
                                new Row("Estimated Net", "+24,691,357", true),
                                new Row("Combat Pay (up to 12 wins)", "+3,600,000", false),
                                new Row("Net if Every Win Is Secured", "+28,291,357", true)),
                          List.of())),
              "Estimate only. Combat pay assumes every win is secured and leaves out one-off bonuses. Salvage, "
                    + "repairs, and battlefield losses are excluded.");
    }

    /** A generated pitch of the usual length: the contact's greeting, the objective, and the ask. */
    private static final String TYPICAL_PITCH = "The Draconis Combine requires this task to be completed, and your "
                                                      + "unit has been selected to carry it out. Attend to your duty "
                                                      + "with the discipline and resolve it demands. Your mission "
                                                      + "objective is asset retrieval. Proceed to the area of "
                                                      + "operations, reconnoiter the target, and secure the asset "
                                                      + "before the enemy can move it off-world. The Coordinator "
                                                      + "does not forget those who serve well, nor those who fail "
                                                      + "him. We expect your answer before the next jump.";

    @Test
    void aTypicalPitchIsNotCutShortOnAFullDossier() {
        String message = ContractDiscordFormatter.format(fullDossier(TYPICAL_PITCH));

        assertTrue(message.length() <= ContractDiscordFormatter.MAX_LENGTH, "length was " + message.length());
        assertFalse(message.contains("…"), "the briefing must not be shortened");
        assertTrue(message.contains("We expect your answer before the next jump."));
    }

    @Test
    void theMoneyCaveatIsLeftOut() {
        String message = ContractDiscordFormatter.format(fullDossier(TYPICAL_PITCH));

        assertFalse(message.contains("Estimate only."), "the caveat belongs in the dossier and PDF, not the post");
    }

    @Test
    void figuresAreLeftUncoloured() {
        String message = ContractDiscordFormatter.format(fullDossier(TYPICAL_PITCH));

        assertTrue(message.contains("  123,456,789 C-bills\n"), "the value follows its padded label, uncoded");
        assertFalse(message.contains(DiscordFormat.YELLOW.toString()));
    }

    @Test
    void notesUseTheReadableDefaultColour() {
        String message = ContractDiscordFormatter.format(fullDossier(TYPICAL_PITCH));

        assertTrue(message.contains("\nAdvisory: Irregular pay credentials.\n"));
        assertFalse(message.contains(DiscordFormat.GRAY.toString()), "Discord's grey is too dark to read");
    }

    @Test
    void aShortBriefingIsLeftWhole() {
        String message = ContractDiscordFormatter.format(readout("Operation Sundowner", "Short and sweet."));

        assertFalse(message.contains("…"));
    }

    @Test
    void markdownInNamesAndBriefingsReadsAsWritten() {
        String message = ContractDiscordFormatter.format(readout("Operation *Star*", "Bring __everything__."));

        assertTrue(message.contains("OPERATION \\*STAR\\*"));
        assertTrue(message.contains("Bring \\_\\_everything\\_\\_."));
    }

    @Test
    void textInsideTheStatBlockCannotCloseItEarly() {
        ContractReadout sneaky = new ContractReadout("Employer", "Title", "status ``` sneaky", "Even Fight 5/10",
              List.of(), null, "", List.of(), "caveat");

        String message = ContractDiscordFormatter.format(sneaky);

        // The opening fence, and the closing one: nothing in between.
        int fences = message.split("```", -1).length - 1;
        assertEquals(2, fences);
    }

    @Test
    void anOfferWithNoBriefingHasNoQuote() {
        String message = ContractDiscordFormatter.format(readout("Operation Sundowner", "   "));

        assertFalse(message.contains("> "));
        assertFalse(message.contains("Leutnant Katrina Steiner"), "no speaker without words to attribute");
    }

    @Test
    void theQuoteFitsTheRoomItIsGiven() {
        String quoted = ContractDiscordFormatter.quote("one two three four five six seven eight nine ten", 25);

        assertTrue(quoted.length() <= 25, quoted);
        assertTrue(quoted.startsWith("> one"));
    }
}
