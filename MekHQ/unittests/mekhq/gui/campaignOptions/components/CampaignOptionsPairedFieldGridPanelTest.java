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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

/**
 * Layout-contract tests for {@link CampaignOptionsPairedFieldGridPanel}. The
 * panel's value is that its control columns
 * line up regardless of label length, so these tests assert the grid
 * coordinates each pair (and the trailing filler)
 * receives, plus the argument validation the panel performs.
 */
class CampaignOptionsPairedFieldGridPanelTest {

    private static JComponent[] labels(int count) {
        JComponent[] components = new JComponent[count];
        for (int i = 0; i < count; i++) {
            components[i] = new JLabel("label " + i);
        }
        return components;
    }

    private static JComponent[] controls(int count) {
        JComponent[] components = new JComponent[count];
        for (int i = 0; i < count; i++) {
            components[i] = new JLabel("control " + i);
        }
        return components;
    }

    private static GridBagConstraints constraintsFor(CampaignOptionsPairedFieldGridPanel panel, int index) {
        GridBagLayout layout = (GridBagLayout) panel.getLayout();
        return layout.getConstraints(panel.getComponent(index));
    }

    @Test
    void constructorRejectsFewerThanOneColumn() {
        assertThrows(IllegalArgumentException.class,
                () -> new CampaignOptionsPairedFieldGridPanel("Test", 300, 300, 100, 0));
    }

    @Test
    void addPairsRejectsMismatchedArrayLengths() {
        CampaignOptionsPairedFieldGridPanel panel = new CampaignOptionsPairedFieldGridPanel("Test", 300, 300, 100, 2);

        assertThrows(IllegalArgumentException.class, () -> panel.addPairs(labels(2), controls(1)));
    }

    @Test
    void addPairsLaysOutPairsRowMajorWithTrailingFiller() {
        CampaignOptionsPairedFieldGridPanel panel = new CampaignOptionsPairedFieldGridPanel("Test", 300, 300, 100, 2);

        panel.addPairs(labels(3), controls(3));

        // Three pair sub-panels plus one trailing filler.
        assertEquals(4, panel.getComponentCount());

        // Pairs fill row-major across two columns: (0,0) (1,0) (0,1).
        assertEquals(0, constraintsFor(panel, 0).gridx);
        assertEquals(0, constraintsFor(panel, 0).gridy);
        assertEquals(1, constraintsFor(panel, 1).gridx);
        assertEquals(0, constraintsFor(panel, 1).gridy);
        assertEquals(0, constraintsFor(panel, 2).gridx);
        assertEquals(1, constraintsFor(panel, 2).gridy);

        // The trailing filler sits in the column past the last data column and absorbs
        // slack.
        GridBagConstraints filler = constraintsFor(panel, 3);
        assertEquals(2, filler.gridx);
        assertEquals(1.0, filler.weightx);
        assertEquals(GridBagConstraints.HORIZONTAL, filler.fill);
    }

    @Test
    void singleColumnPlacesEveryPairInOneColumn() {
        CampaignOptionsPairedFieldGridPanel panel = new CampaignOptionsPairedFieldGridPanel("Test", 300, 300, 100, 1);

        panel.addPairs(labels(3), controls(3));

        // Three pairs stacked vertically in column 0, plus the trailing filler in
        // column 1.
        assertEquals(0, constraintsFor(panel, 0).gridx);
        assertEquals(0, constraintsFor(panel, 0).gridy);
        assertEquals(0, constraintsFor(panel, 1).gridx);
        assertEquals(1, constraintsFor(panel, 1).gridy);
        assertEquals(0, constraintsFor(panel, 2).gridx);
        assertEquals(2, constraintsFor(panel, 2).gridy);
        assertEquals(1, constraintsFor(panel, 3).gridx);
    }
}
