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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class ImmersiveDialogStyleTest {
    private static final String FLATLAF_STYLE_PROPERTY = "FlatLaf.style";
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

    @Test
    void sectionRuleIsVerticallyCentered() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel header = ImmersiveDialogStyle.createSectionHeader("TRANSMISSION DATA", Color.CYAN);
            JComponent rule = (JComponent) ((java.awt.BorderLayout) header.getLayout())
                                                    .getLayoutComponent(java.awt.BorderLayout.CENTER);
            rule.setSize(80, 20);

            BufferedImage image = render(rule, 80, 20);
            assertEquals(0, new Color(image.getRGB(40, 0), true).getAlpha());
            assertTrue(new Color(image.getRGB(40, 9), true).getAlpha() > 0);
        });
    }

    @Test
    void framedPanelLeavesCutCornersTransparent() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel panel = ImmersiveDialogStyle.createFramedPanel();
            panel.setBorder(BorderFactory.createEmptyBorder());
            panel.setSize(100, 60);

            BufferedImage image = render(panel, 100, 60);
            assertEquals(0, new Color(image.getRGB(99, 0), true).getAlpha());
            assertTrue(new Color(image.getRGB(50, 30), true).getAlpha() > 0);
        });
    }

    @Test
    void responseButtonsUseSignalInteractionStyleAndRemainFocusable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton button = new JButton("Respond");
            button.setFocusable(false);

            ImmersiveDialogStyle.applyResponseButtonStyle(button);

            assertTrue(button.isFocusable());
            assertTrue(button.isFocusPainted());
            Map<?, ?> styleMap = getStyleMap(button);
            assertEquals(ImmersiveDialogStyle.getSignalColor(), styleMap.get("focusedBorderColor"));
            assertEquals(ImmersiveDialogStyle.getSignalColor(), styleMap.get("pressedBorderColor"));
            assertNotNull(styleMap.get("hoverBorderColor"));
            assertNotNull(styleMap.get("hoverBackground"));
            assertNotNull(styleMap.get("pressedBackground"));
        });
    }

    @Test
    void supplementalControlsUseSignalInteractionStyle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel supplementalPanel = new JPanel();
            JSpinner spinner = new JSpinner();
            JComboBox<String> comboBox = new JComboBox<>();
            supplementalPanel.add(spinner);
            supplementalPanel.add(comboBox);

            ImmersiveDialogStyle.applySupplementalControlStyle(supplementalPanel);

            assertSupplementalControlStyle(getStyleMap(spinner));
            assertSupplementalControlStyle(getStyleMap(comboBox));
        });
    }

    private static void assertSupplementalControlStyle(Map<?, ?> styleMap) {
        Color signalColor = ImmersiveDialogStyle.getSignalColor();
        assertEquals(signalColor, styleMap.get("focusColor"));
        assertEquals(signalColor, styleMap.get("focusedBorderColor"));
        assertEquals(signalColor, styleMap.get("buttonHoverArrowColor"));
        assertEquals(signalColor, styleMap.get("buttonPressedArrowColor"));
        assertNotNull(styleMap.get("buttonSeparatorColor"));
        assertNotNull(styleMap.get("buttonArrowColor"));
    }

    private static Map<?, ?> getStyleMap(JComponent component) {
        Object style = component.getClientProperty(FLATLAF_STYLE_PROPERTY);
        assertTrue(style instanceof Map<?, ?>);
        return (Map<?, ?>) style;
    }

    private static BufferedImage render(JComponent component, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = image.createGraphics();
        component.paint(graphics2D);
        graphics2D.dispose();
        return image;
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
