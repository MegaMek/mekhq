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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import javax.swing.JEditorPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import megamek.common.ui.FastJScrollPane;
import org.junit.jupiter.api.Test;

class ImmersiveDialogSizingTest {
    @Test
    void naturalContentFitsWithoutScrolling() {
        ImmersiveDialogSizing.SizingResult result = ImmersiveDialogSizing.calculate(600, 320, 120, 900);

        assertEquals(600, result.dialogHeight());
        assertEquals(320, result.viewportHeight());
    }

    @Test
    void oversizedContentShrinksOnlyViewportToScreenCap() {
        ImmersiveDialogSizing.SizingResult result = ImmersiveDialogSizing.calculate(1_100, 700, 120, 1_000);

        assertEquals(900, result.dialogHeight());
        assertEquals(500, result.viewportHeight());
    }

    @Test
    void viewportNeverShrinksBelowReadingMinimum() {
        ImmersiveDialogSizing.SizingResult result = ImmersiveDialogSizing.calculate(1_500, 400, 120, 800);

        assertEquals(720, result.dialogHeight());
        assertEquals(120, result.viewportHeight());
    }

    @Test
    void minimumHeightUsesReadingFloorWhenNaturalViewportExceedsIt() {
        ImmersiveDialogSizing.SizingResult result = ImmersiveDialogSizing.calculate(600, 320, 120, 900);

        assertEquals(400, result.minimumDialogHeight());
    }

    @Test
    void minimumHeightDoesNotExceedNaturalHeightWhenViewportIsBelowFloor() {
        ImmersiveDialogSizing.SizingResult result = ImmersiveDialogSizing.calculate(180, 80, 120, 900);

        assertEquals(180, result.dialogHeight());
        assertEquals(result.dialogHeight(), result.minimumDialogHeight());
    }

    @Test
    void minimumHeightDoesNotExceedCalculatedHeightOnSmallScreen() {
        ImmersiveDialogSizing.SizingResult result = ImmersiveDialogSizing.calculate(1_500, 400, 120, 800);

        assertEquals(720, result.dialogHeight());
        assertEquals(result.dialogHeight(), result.minimumDialogHeight());
    }

    @Test
    void responsiveHtmlUsesInitialWidthThenReflowsToComponentWidth() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int initialWidth = 400;
            JEditorPane editorPane = new ImmersiveDialogCore.ResponsiveHtmlEditorPane(initialWidth);
            editorPane.setContentType("text/html");
            editorPane.setText("<html><p>Alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu xi " +
                                     "omicron pi rho sigma tau upsilon phi chi psi omega.</p>" +
                                     "<p>Alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu.</p></html>");

            assertEquals(initialWidth, editorPane.getPreferredSize().width);

            editorPane.setSize(180, Short.MAX_VALUE);
            Dimension narrowSize = editorPane.getPreferredSize();
            editorPane.setSize(600, Short.MAX_VALUE);
            Dimension wideSize = editorPane.getPreferredSize();

            assertEquals(180, narrowSize.width);
            assertEquals(600, wideSize.width);
            assertTrue(narrowSize.height > wideSize.height);
        });
    }

    @Test
    void boundedInformationViewportReflowsWithoutGrowingAllocatedHeight() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int initialWidth = 400;
            JEditorPane editorPane = new ImmersiveDialogCore.ResponsiveHtmlEditorPane(initialWidth);
            editorPane.setContentType("text/html");
            editorPane.setText("<html><p>Alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu xi " +
                                     "omicron pi rho sigma tau upsilon phi chi psi omega.</p>" +
                                     "<p>Alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu.</p></html>");
            int wideContentHeight = editorPane.getPreferredSize().height;

            FastJScrollPane scrollPane = ImmersiveDialogCore.createBoundedInformationScrollPane(editorPane);
            Dimension allocatedSize = scrollPane.getPreferredSize();
            scrollPane.setSize(180, allocatedSize.height);
            for (int pass = 0; pass < 3; pass++) {
                scrollPane.doLayout();
                scrollPane.getViewport().doLayout();
                scrollPane.getViewport().getView().doLayout();
            }

            assertEquals(scrollPane.getViewport().getExtentSize().width, editorPane.getWidth());
            assertEquals(allocatedSize.height, scrollPane.getPreferredSize().height);
            assertTrue(editorPane.getPreferredSize().height > wideContentHeight);
            assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                  scrollPane.getVerticalScrollBarPolicy());
            assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
                scrollPane.getHorizontalScrollBarPolicy());
            assertTrue(scrollPane.getVerticalScrollBar().isVisible());
        });
    }
}
