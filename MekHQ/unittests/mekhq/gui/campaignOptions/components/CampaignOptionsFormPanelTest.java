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
package mekhq.gui.campaignOptions.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

/**
 * Layout-contract tests for {@link CampaignOptionsFormPanel}. They lock in the
 * behaviours other code relies on: a
 * single shared row counter (so different row styles stack in call order), the
 * stretched control column on labelled
 * rows, the configured minimum control width, and the single-column fallback of
 * the grid helpers.
 */
class CampaignOptionsFormPanelTest {

    private static GridBagConstraints constraintsFor(CampaignOptionsFormPanel panel, int index) {
        GridBagLayout layout = (GridBagLayout) panel.getLayout();
        return layout.getConstraints(panel.getComponent(index));
    }

    @Test
    void addRowPlacesLabelLeftAndStretchedControlRight() {
        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("Test", 300, 220);

        panel.addRow(new JLabel("Label"), new JLabel("Control"));

        assertEquals(2, panel.getComponentCount());

        GridBagConstraints labelConstraints = constraintsFor(panel, 0);
        assertEquals(0, labelConstraints.gridx);
        assertEquals(0, labelConstraints.gridy);

        GridBagConstraints controlConstraints = constraintsFor(panel, 1);
        assertEquals(1, controlConstraints.gridx);
        assertEquals(0, controlConstraints.gridy);
        assertEquals(GridBagConstraints.REMAINDER, controlConstraints.gridwidth);
        assertEquals(1.0, controlConstraints.weightx);
        assertEquals(GridBagConstraints.HORIZONTAL, controlConstraints.fill);
    }

    @Test
    void addRowAppliesConfiguredMinimumControlWidth() {
        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("Test", 0, 200);
        JComponent control = new JLabel();

        panel.addRow(new JLabel("Label"), control);

        // A naturally tiny control is grown to the configured control-column width.
        assertEquals(200, control.getMinimumSize().width);
    }

    @Test
    void addCheckBoxSpansBothColumnsAndAddsTrailingFiller() {
        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("Test", 300, 220);

        panel.addCheckBox(new JCheckBox("Toggle"));

        // The checkbox plus its trailing filler.
        assertEquals(2, panel.getComponentCount());
        assertEquals(2, constraintsFor(panel, 0).gridwidth);
        assertEquals(2, constraintsFor(panel, 1).gridx);
    }

    @Test
    void mixedRowsShareRowCounterSoLaterRowsStackBelow() {
        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("Test", 300, 220);

        // A two-column grid of four checkboxes occupies two rows (indices 0-3 are the
        // boxes, 4-5 the fillers).
        panel.addCheckBoxGrid(2, new JCheckBox("a"), new JCheckBox("b"), new JCheckBox("c"), new JCheckBox("d"));
        JLabel below = new JLabel("Below");
        panel.addRow(below, new JLabel("Control"));

        // The labelled row added afterwards must land on the row below the two grid
        // rows, proving the shared counter.
        GridBagConstraints belowConstraints = constraintsFor(panel, 6);
        assertEquals(0, belowConstraints.gridx);
        assertEquals(2, belowConstraints.gridy);
    }

    @Test
    void singleColumnGridFallsBackToFullWidthRows() {
        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("Test");

        panel.addComponentGrid(1, new JLabel("first"), new JLabel("second"));

        assertEquals(2, panel.getComponentCount());

        GridBagConstraints first = constraintsFor(panel, 0);
        assertEquals(0, first.gridy);
        assertEquals(2, first.gridwidth);

        GridBagConstraints second = constraintsFor(panel, 1);
        assertEquals(1, second.gridy);
        assertEquals(2, second.gridwidth);
    }
}
