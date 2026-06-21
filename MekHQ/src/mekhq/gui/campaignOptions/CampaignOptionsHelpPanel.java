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
 * of The Topps Company Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.campaignOptions;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import jakarta.annotation.Nonnull;
import megamek.client.ui.util.UIUtil;

/**
 * Sticky contextual help surface for Campaign Options.
 */
class CampaignOptionsHelpPanel extends JPanel {
    private static final int HELP_PANEL_HEIGHT = 120;
    private static final int SCROLL_SPEED = 12;

    private final JEditorPane helpTextPane;

    CampaignOptionsHelpPanel() {
        super(new BorderLayout());
        setName("campaignOptionsHelpPanel");
        // Draw the caption on the same flush frame border the navigation and content panels use. A standard
        // TitledBorder insets its line a couple of pixels (which misaligned this box with those squared frames), so
        // FlushTitledBorder paints the line flush while keeping the title on the line.
        Border frameBorder = UIManager.getBorder("ScrollPane.border");
        if (frameBorder == null) {
            frameBorder = BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"));
        }
        setBorder(new FlushTitledBorder(frameBorder,
              getTextAt(getCampaignOptionsResourceBundle(), "campaignOptionsHelp.title")));

        helpTextPane = new JEditorPane();
        helpTextPane.setName("campaignOptionsHelpText");
        helpTextPane.setContentType("text/html");
        helpTextPane.setEditable(false);
        helpTextPane.setOpaque(false);
        helpTextPane.setBorder(new EmptyBorder(4, 8, 4, 8));
        helpTextPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        JScrollPane helpScrollPane = new JScrollPane(helpTextPane,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        helpScrollPane.setName("campaignOptionsHelpScrollPane");
        helpScrollPane.setBorder(null);
        helpScrollPane.setOpaque(false);
        helpScrollPane.getViewport().setOpaque(false);
        helpScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_SPEED);

        add(helpScrollPane, BorderLayout.CENTER);
        clearHelpText();
    }

    void setHelpText(@Nonnull String helpText) {
        if (helpText.isBlank()) {
            clearHelpText();
            return;
        }

        helpTextPane.setText(helpText);
        helpTextPane.setCaretPosition(0);
    }

    void clearHelpText() {
        helpTextPane.setText("");
    }

    @Override
    public @Nonnull Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        return new Dimension(preferredSize.width, UIUtil.scaleForGUI(HELP_PANEL_HEIGHT));
    }

    @Override
    public @Nonnull Dimension getMinimumSize() {
        Dimension minimumSize = super.getMinimumSize();
        return new Dimension(minimumSize.width, UIUtil.scaleForGUI(HELP_PANEL_HEIGHT));
    }

    /**
     * A {@link TitledBorder} that paints its line flush with the component bounds instead of inset by the standard
     * two-pixel edge spacing, so this box lines up exactly with the squared frame borders beside it while still
     * drawing the caption on the border line.
     */
    private static class FlushTitledBorder extends TitledBorder {
        private static final int EDGE_SPACING = 2;

        FlushTitledBorder(Border border, String title) {
            super(border, title);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            // Expand the paint rectangle by the edge spacing on the left, right, and bottom so TitledBorder's internal
            // inset lands exactly on the component edges. The top is left untouched so the caption keeps its place on
            // the line.
            super.paintBorder(c, g, x - EDGE_SPACING, y, width + (2 * EDGE_SPACING), height + EDGE_SPACING);
        }
    }
}
