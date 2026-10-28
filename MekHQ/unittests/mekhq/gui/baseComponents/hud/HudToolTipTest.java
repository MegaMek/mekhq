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
package mekhq.gui.baseComponents.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The tooltip text the HUD tooltip builds, tested without showing a tooltip. */
class HudToolTipTest {
    @Test
    void escapesMarkupSoTextShowsAsWritten() {
        assertEquals("Fire &amp; Forget &lt;b&gt;", HudToolTip.escape("Fire & Forget <b>"));
        assertEquals("&quot;Ace&quot; &#39;Pilot&#39;", HudToolTip.escape("\"Ace\" 'Pilot'"));
    }

    @Test
    void lineBreaksBecomeHtmlBreaks() {
        assertEquals("one<br>two", HudToolTip.escape("one\ntwo"));
    }

    @Test
    void titleIsUpperCasedAboveTheBody() {
        String html = HudToolTip.html("Sniper", "Range modifiers are halved.");

        assertTrue(html.startsWith("<html>"));
        assertTrue(html.contains(">SNIPER</div>"));
        assertTrue(html.indexOf("SNIPER") < html.indexOf("Range modifiers are halved."));
    }

    @Test
    void blankTitleLeavesOnlyTheBody() {
        String html = HudToolTip.html("  ", "Just the text");

        assertFalse(html.contains("font-weight:bold"));
        assertTrue(html.contains("Just the text"));
    }
}
