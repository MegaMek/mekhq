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
package mekhq.gui.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Point;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import mekhq.campaign.Campaign;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

class PlanetViewPanelTest {
    @Test
    void animatedDossierResetsPreviouslyScrolledViewportBeforeReveal() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Campaign campaign = MHQTestUtilities.getTestCampaign();
            PlanetViewPanel panel = new PlanetViewPanel(campaign.getCurrentSystem(), campaign, 0, true);
            JScrollPane scrollPane = new JScrollPane(panel);
            JViewport viewport = scrollPane.getViewport();
            viewport.setExtentSize(new Dimension(320, 200));
            viewport.setViewSize(new Dimension(320, 1_000));
            viewport.setViewPosition(new Point(0, 160));

            panel.addNotify();
            try {
                assertTrue(viewport.getViewPosition().y == 0);
                assertTrue(panel.isRevealAnimationRunning());
            } finally {
                panel.removeNotify();
            }
        });
    }

    @Test
    void verticalViewportMovementFinishesRevealAnimation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Campaign campaign = MHQTestUtilities.getTestCampaign();
            PlanetViewPanel panel = new PlanetViewPanel(campaign.getCurrentSystem(), campaign, 0, true);
            JScrollPane scrollPane = new JScrollPane(panel);
            JViewport viewport = scrollPane.getViewport();
            viewport.setExtentSize(new Dimension(320, 200));
            viewport.setViewSize(new Dimension(320, 1_000));
            panel.addNotify();
            try {
                assertTrue(panel.isRevealAnimationRunning());

                viewport.setExtentSize(new Dimension(300, 200));
                assertTrue(panel.isRevealAnimationRunning());

                viewport.setViewPosition(new Point(0, 16));
                assertFalse(panel.isRevealAnimationRunning());
            } finally {
                panel.removeNotify();
            }
        });
    }
}
