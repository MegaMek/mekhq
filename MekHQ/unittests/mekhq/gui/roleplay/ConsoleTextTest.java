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
package mekhq.gui.roleplay;

import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT;
import static mekhq.gui.baseComponents.hud.HudStyle.AMBER;
import static mekhq.gui.baseComponents.hud.HudStyle.DANGER;
import static mekhq.gui.baseComponents.hud.HudStyle.READY;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.UUID;

import mekhq.campaign.roleplay.CheckRecord;
import mekhq.campaign.roleplay.PlotThread;
import mekhq.campaign.roleplay.PlotThreadLength;
import mekhq.campaign.roleplay.RandomOracleGenerator;
import org.junit.jupiter.api.Test;

/** The console pages' text helpers, which decide what players read in lists and result banners. */
class ConsoleTextTest {
    private static final String BUNDLE = "mekhq.resources.Roleplay";

    private static String text(final String key) {
        return getTextAt(BUNDLE, key);
    }

    private static CheckRecord.Side side(final String name, final int roll, final int margin, final boolean won) {
        return new CheckRecord.Side(name, UUID.randomUUID(), "Stealth (Normal)", "7+", roll, List.of(3, 4), margin,
              false, won);
    }

    @Test
    void verdictsFollowTheMargin() {
        assertEquals(text("ChecksPage.verdict.outstanding"), ChecksPage.verdictWord(4));
        assertEquals(text("ChecksPage.verdict.passed"), ChecksPage.verdictWord(3));
        assertEquals(text("ChecksPage.verdict.passed"), ChecksPage.verdictWord(0));
        assertEquals(text("ChecksPage.verdict.failed"), ChecksPage.verdictWord(-1));
        assertEquals(text("ChecksPage.verdict.failed"), ChecksPage.verdictWord(-2));
        assertEquals(text("ChecksPage.verdict.botched"), ChecksPage.verdictWord(-3));
        assertEquals(READY, ChecksPage.colorFor(10));
        assertEquals(ACCENT, ChecksPage.colorFor(0));
        assertEquals(AMBER, ChecksPage.colorFor(-2));
        assertEquals(DANGER, ChecksPage.colorFor(-10));
    }

    @Test
    void summariesSuitEachKindOfCheck() {
        // Built from the same resource keys, so rewording the text doesn't break the test.
        assertEquals(OracleConsole.joined(text("ChecksPage.verdict.passed"), "Rook"),
              ChecksPage.summarize(new CheckRecord(false, "", List.of(side("Rook", 9, 2, true)))));
        assertEquals(getFormattedTextAt(BUNDLE, "ChecksPage.result.group", 1, 2),
              ChecksPage.summarize(new CheckRecord(false, "", List.of(side("Rook", 9, 2, true),
                    side("Dace", 4, -3, false)))));
        assertEquals(getFormattedTextAt(BUNDLE, "ChecksPage.result.wins", "Dace"),
              ChecksPage.summarize(new CheckRecord(true, "", List.of(side("Rook", 8, 1, false),
                    side("Dace", 8, 1, true)))));
        assertEquals(OracleConsole.joined(
                    getFormattedTextAt(BUNDLE, "ChecksPage.result.short", "Rook", "Stealth (Normal)", 9, "7+"),
                    getFormattedTextAt(BUNDLE, "ChecksPage.result.short", "Dace", "Stealth (Normal)", 4, "7+")),
              ChecksPage.describeSides(new CheckRecord(false, "", List.of(side("Rook", 9, 2, true),
                    side("Dace", 4, -3, false)))));
    }

    @Test
    void chancesRoundToWholePercents() {
        assertEquals("58%", ChecksPage.percent(21.0 / 36));
        assertEquals("0%", ChecksPage.percent(0));
        assertEquals("100%", ChecksPage.percent(1));
    }

    @Test
    void threadProgressCountsToTheNextMark() {
        RandomOracleGenerator generator = mock(RandomOracleGenerator.class);
        PlotThread thread = PlotThread.create("Ghost", PlotThreadLength.SHORT, generator);
        assertEquals(getFormattedTextAt(BUNDLE, "OracleConsole.threads.progress.toFlashpoint", 10, 5),
              ThreadsPage.describeProgress(thread));
        for (int step = 0; step < 4; step++) {
            thread.revealNextStep(null);
        }
        assertEquals(text("OracleConsole.threads.progress.flashpointNext"), ThreadsPage.describeProgress(thread));
        thread.revealNextStep(null);
        thread.revealNextStep(null);
        // Step 10 of a Short thread is the conclusion, not a flashpoint.
        assertEquals(getFormattedTextAt(BUNDLE, "OracleConsole.threads.progress.toConclusion", 4, 5),
              ThreadsPage.describeProgress(thread));
        for (int step = 0; step < 3; step++) {
            thread.revealNextStep(null);
        }
        assertEquals(text("OracleConsole.threads.progress.conclusionNext"), ThreadsPage.describeProgress(thread));
        thread.revealNextStep(null);
        assertEquals(text("OracleConsole.threads.progress.concluded"), ThreadsPage.describeProgress(thread));
    }
}
