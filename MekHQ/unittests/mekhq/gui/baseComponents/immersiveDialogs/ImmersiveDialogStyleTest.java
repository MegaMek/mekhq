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
package mekhq.gui.baseComponents.immersiveDialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class ImmersiveDialogStyleTest {
    private static final String TITLE_BAR_CAPTION_PROPERTY = "JComponent.titleBarCaption";
    private static final String WINDOW_BUTTONS_PLACEHOLDER_PROPERTY =
          "FlatLaf.fullWindowContent.buttonsPlaceholder";

    @Test
    void fullWindowHeaderReservesWindowsButtonsAndActsAsCaption() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel header = ImmersiveDialogStyle.createHeaderPanel("Transmission", "Clear", true);

            assertEquals(Boolean.TRUE, header.getClientProperty(TITLE_BAR_CAPTION_PROPERTY));
            JComponent placeholder = findComponentWithProperty(header, WINDOW_BUTTONS_PLACEHOLDER_PROPERTY);
            assertNotNull(placeholder);
            assertEquals("win horizontal", placeholder.getClientProperty(WINDOW_BUTTONS_PLACEHOLDER_PROPERTY));
        });
    }

    @Test
    void fallbackHeaderDoesNotReserveWindowButtons() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel header = ImmersiveDialogStyle.createHeaderPanel("Transmission", "Clear", false);

            assertNull(header.getClientProperty(TITLE_BAR_CAPTION_PROPERTY));
            assertNull(findComponentWithProperty(header, WINDOW_BUTTONS_PLACEHOLDER_PROPERTY));
        });
    }

    private static JComponent findComponentWithProperty(Container container, String propertyName) {
        for (Component child : container.getComponents()) {
            if (child instanceof JComponent component && component.getClientProperty(propertyName) != null) {
                return component;
            }
            if (child instanceof Container childContainer) {
                JComponent match = findComponentWithProperty(childContainer, propertyName);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }
}
