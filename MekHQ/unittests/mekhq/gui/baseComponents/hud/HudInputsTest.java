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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

class HudInputsTest {
    /** Clicks the button: a release inside it is what fires a HUD button. */
    private static void press(final HudButton button) {
        button.setSize(120, 30);
        button.dispatchEvent(new MouseEvent(button, MouseEvent.MOUSE_RELEASED, 0, 0, 10, 10, 1, false,
              MouseEvent.BUTTON1));
    }

    private static String textOf(final HudButton button) {
        return ((JLabel) button.getComponent(0)).getText();
    }

    @Test
    void aConfirmButtonNeedsTwoPresses() {
        AtomicInteger confirmed = new AtomicInteger();
        HudConfirmButton button = new HudConfirmButton("DELETE", "CONFIRM DELETE?", true);
        button.addActionListener(event -> confirmed.incrementAndGet());

        press(button);
        assertEquals(0, confirmed.get());
        assertEquals("CONFIRM DELETE?", textOf(button));

        press(button);
        assertEquals(1, confirmed.get());
        assertEquals("DELETE", textOf(button));
    }

    @Test
    void resettingAConfirmButtonStartsOver() {
        AtomicInteger confirmed = new AtomicInteger();
        HudConfirmButton button = new HudConfirmButton("DELETE", "CONFIRM DELETE?", true);
        button.addActionListener(event -> confirmed.incrementAndGet());

        press(button);
        button.reset();
        press(button);
        assertEquals(0, confirmed.get(), "the press after a reset only asks again");
    }

    @Test
    void aDisarmedButtonIgnoresPresses() {
        AtomicInteger pressed = new AtomicInteger();
        HudButton button = new HudButton("ROLL", true);
        button.addActionListener(event -> pressed.incrementAndGet());
        button.setArmed(false);
        press(button);
        assertEquals(0, pressed.get());
        button.setArmed(true);
        press(button);
        assertEquals(1, pressed.get());
    }

    @Test
    void aDateFieldReadsIsoDatesAndIgnoresAnythingElse() {
        AtomicInteger changes = new AtomicInteger();
        HudDateField field = new HudDateField("YYYY-MM-DD", changes::incrementAndGet);

        assertNull(field.getDate());
        field.setText(" 3025-06-01 ");
        assertEquals(LocalDate.of(3025, 6, 1), field.getDate());
        field.setText("June 1st");
        assertNull(field.getDate());
        assertEquals(HudStyle.DANGER, field.getForeground());
        field.setText("");
        assertEquals(HudStyle.TEXT, field.getForeground());
        assertEquals(true, changes.get() > 0);
    }
}
