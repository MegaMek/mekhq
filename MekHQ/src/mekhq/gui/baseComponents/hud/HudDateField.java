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

import static mekhq.gui.baseComponents.hud.HudStyle.DANGER;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.common.annotations.Nullable;

/**
 * A HUD-styled date field that takes an ISO date ({@code YYYY-MM-DD}). A blank field means "no date"; a date that
 * cannot be read turns the text red and counts as no date. A faint placeholder shows the expected format while empty.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudDateField extends JTextField {
    private final String placeholder;

    /**
     * @param placeholder the faint text shown while the field is empty, such as "YYYY-MM-DD"
     * @param onChange    called whenever the text changes
     */
    public HudDateField(String placeholder, Runnable onChange) {
        super(9);
        this.placeholder = placeholder;
        Hud.styleField(this);
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                changed(onChange);
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                changed(onChange);
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                changed(onChange);
            }
        });
    }

    private void changed(Runnable onChange) {
        String text = getText().strip();
        setForeground(text.isEmpty() || parse(text) != null ? TEXT : DANGER);
        onChange.run();
    }

    /**
     * @return the date typed, or {@code null} if the field is blank or cannot be read
     */
    public @Nullable LocalDate getDate() {
        String text = getText().strip();
        return text.isEmpty() ? null : parse(text);
    }

    private static @Nullable LocalDate parse(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (getText().isEmpty() && !isFocusOwner()) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                Insets insets = getInsets();
                g2.setColor(TEXT_FAINT);
                g2.setFont(getFont());
                g2.drawString(placeholder, insets.left, insets.top + g2.getFontMetrics().getAscent());
            } finally {
                g2.dispose();
            }
        }
    }
}
