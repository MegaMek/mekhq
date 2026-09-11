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
package mekhq.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import megamek.client.ui.util.UIUtil;
import mekhq.gui.baseComponents.FramedCommandButton;
import org.junit.jupiter.api.Test;

class MapTabNavigationButtonTest {
    @Test
    void navigationFactoryAppliesSharedFrameStyleAndMetadata() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FramedCommandButton button = MapTab.createNavigationButton("Center on Fleet", "Center the map");

            assertEquals("Center on Fleet", button.getText());
            assertEquals("Center the map", button.getToolTipText());
            assertEquals("Center on Fleet", button.getAccessibleContext().getAccessibleName());
            assertEquals("Center the map", button.getAccessibleContext().getAccessibleDescription());
            assertTrue(button.isFocusable());
            assertTrue(button.isRolloverEnabled());
            assertFalse(button.isContentAreaFilled());
            assertFalse(button.isBorderPainted());
            assertFalse(button.isFocusPainted());
            assertTrue(button.getMargin().left > button.getMargin().top);
        });
    }

    @Test
    void inspectorToggleFactoryCreatesStableAccessibleIconButtons() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton collapse = MapTab.createInspectorToggleButton(true, "Collapse tooltip", "Collapse context");
            JButton expand = MapTab.createInspectorToggleButton(false, "Expand tooltip", "Expand context");

            assertNotNull(collapse.getIcon());
            assertNotNull(expand.getIcon());
            assertEquals(collapse.getPreferredSize(), collapse.getMinimumSize());
            assertEquals(collapse.getPreferredSize(), collapse.getMaximumSize());
            assertEquals(collapse.getPreferredSize(), expand.getPreferredSize());
            assertEquals(collapse.getPreferredSize().width, collapse.getPreferredSize().height);
            assertEquals("Collapse tooltip", collapse.getToolTipText());
            assertEquals("Collapse context", collapse.getAccessibleContext().getAccessibleName());
            assertEquals("Collapse tooltip", collapse.getAccessibleContext().getAccessibleDescription());
            assertTrue(collapse.isFocusable());
            assertFalse(collapse instanceof FramedCommandButton);
            assertFalse(collapse.isContentAreaFilled());
            assertEquals(Boolean.TRUE, collapse.getClientProperty("navigationUtilityButton"));
        });
    }

    @Test
    void mapPopupStyleAppliesToNestedCommandsAndSeparators() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem command = new JMenuItem("Zoom In");
            JSeparator separator = new JSeparator();
            JMenu submenu = new JMenu("Center Map");
            JMenuItem nestedCommand = new JMenuItem("On Terra");
            submenu.add(nestedCommand);
            popup.add(command);
            popup.add(separator);
            popup.add(submenu);

            InterstellarMapPanel.styleNavigationPopup(popup);

            assertTrue(popup.isOpaque());
            assertEquals(popup.getBackground(), command.getBackground());
            assertEquals(popup.getForeground(), command.getForeground());
            assertEquals(popup.getBackground(), separator.getBackground());
            assertEquals(submenu.getPopupMenu().getBackground(), nestedCommand.getBackground());
            assertTrue(command.isOpaque());
            assertTrue(submenu.isOpaque());
        });
    }

    @Test
    void utilityButtonResizeKeepsAllDimensionsEqual() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton information = InterstellarMapPanel.createMapLegendButton();
            Dimension hudSize = new Dimension(38, 38);

            InterstellarMapPanel.setNavigationUtilityButtonSize(information, hudSize);

            assertEquals(hudSize, information.getPreferredSize());
            assertEquals(hudSize, information.getMinimumSize());
            assertEquals(hudSize, information.getMaximumSize());
        });
    }

    @Test
    void contextHeaderCentersUtilityButtonInIndependentCell() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JToggleButton systemTab = MapTab.createContextTabButton("System");
            JToggleButton routeTab = MapTab.createContextTabButton("Route");
            JButton collapse = MapTab.createInspectorToggleButton(true, "Collapse", "Collapse context");
            JPanel header = MapTab.createContextHeader(systemTab, routeTab, collapse);
            header.setSize(500, header.getPreferredSize().height);
            header.doLayout();
            collapse.getParent().doLayout();

            int topMargin = collapse.getY();
            int bottomMargin = collapse.getParent().getHeight() - collapse.getY() - collapse.getHeight();
            assertEquals(topMargin, bottomMargin);
        });
    }

    @Test
    void contextScrollbarUsesCompactNavigationStyle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JScrollBar scrollBar = new JScrollBar(JScrollBar.VERTICAL);

            MapTab.styleContextScrollBar(scrollBar);

            assertEquals(UIUtil.scaleForGUI(10), scrollBar.getPreferredSize().width);
            assertEquals(UIUtil.scaleForGUI(16), scrollBar.getUnitIncrement());
            assertNotNull(scrollBar.getBorder());
            assertEquals(0, scrollBar.getComponent(0).getPreferredSize().height);
            assertEquals(0, scrollBar.getComponent(1).getPreferredSize().height);
        });
    }
}
