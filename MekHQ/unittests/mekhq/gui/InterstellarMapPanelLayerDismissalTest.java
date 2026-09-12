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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.junit.jupiter.api.Test;

class InterstellarMapPanelLayerDismissalTest {
    @Test
    void layerControlsAndTriggerRemainInsideThePopoverInteraction() {
        JPanel control = new JPanel();
        JCheckBox checkbox = new JCheckBox();
        control.add(checkbox);
        JButton trigger = new JButton();

        assertTrue(InterstellarMapPanel.isLayerControlInteraction(control, control, trigger));
        assertTrue(InterstellarMapPanel.isLayerControlInteraction(checkbox, control, trigger));
        assertTrue(InterstellarMapPanel.isLayerControlInteraction(trigger, control, trigger));
    }

    @Test
    void dropdownPopupBelongsToItsLayerControlInvoker() {
        JPanel control = new JPanel();
        JComboBox<String> dropdown = new JComboBox<>();
        control.add(dropdown);
        JPopupMenu popup = new JPopupMenu();
        popup.setInvoker(dropdown);
        JList<String> choices = new JList<>();
        popup.add(new JScrollPane(choices));

        assertTrue(InterstellarMapPanel.isLayerControlInteraction(choices, control, null));
    }

    @Test
    void mapAndInspectorClicksAreOutsideEvenUnderTheSameParent() {
        JPanel workspace = new JPanel();
        JPanel control = new JPanel();
        JPanel map = new JPanel();
        JButton inspectorButton = new JButton();
        workspace.add(control);
        workspace.add(map);
        workspace.add(inspectorButton);

        assertFalse(InterstellarMapPanel.isLayerControlInteraction(map, control, null));
        assertFalse(InterstellarMapPanel.isLayerControlInteraction(inspectorButton, control, null));
    }

    @Test
    void unrelatedPopupDoesNotCountAsALayerInteraction() {
        JPanel control = new JPanel();
        JPopupMenu popup = new JPopupMenu();
        popup.setInvoker(new JButton());

        assertFalse(InterstellarMapPanel.isLayerControlInteraction(popup, control, null));
    }
}
