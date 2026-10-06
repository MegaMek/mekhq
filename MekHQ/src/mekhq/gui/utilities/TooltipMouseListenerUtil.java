/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.utilities;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Builds the mouse listener that drives a dialog's own help area.
 *
 * <p>Several MekHQ dialogs explain their controls in a line of text near the buttons rather than in a floating tool
 * tip, because the text is too long for one. This turns "show this text when the pointer is over that control" into a
 * single call, instead of a hand-rolled {@link MouseAdapter} per control.</p>
 *
 * @since 0.50.11
 */
public class TooltipMouseListenerUtil {
    private TooltipMouseListenerUtil() {
        // Utility class: not instantiable.
    }

    /**
     * Returns a listener that shows the given text while the pointer is over the component.
     *
     * <p>The text is restored to {@code null} on exit, so the help area does not keep showing whatever the pointer
     * last passed over after it has moved away.</p>
     *
     * @param setter what to do with the text, usually the dialog's help-area setter
     * @param text   the text to show
     *
     * @return the listener to add to the component
     *
     * @since 0.50.11
     */
    public static MouseAdapter forTooltip(Consumer<String> setter, String text) {
        return forTooltip(setter, text, null);
    }

    /**
     * Returns a listener that shows the given text on entry and restores a default on exit.
     *
     * @param setter      what to do with the text, usually the dialog's help-area setter
     * @param text        the text to show while the pointer is over the component
     * @param defaultText the text to restore when the pointer leaves; {@code null} clears the area
     *
     * @return the listener to add to the component
     *
     * @since 0.50.11
     */
    public static MouseAdapter forTooltip(Consumer<String> setter, String text, String defaultText) {
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                setter.accept(text);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                setter.accept(defaultText == null ? "" : defaultText);
            }
        };
    }
}
