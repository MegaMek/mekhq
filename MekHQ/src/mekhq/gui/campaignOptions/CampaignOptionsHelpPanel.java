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
import java.awt.Dimension;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.util.UIUtil;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

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
        setBorder(RoundedLineBorder.createRoundedLineBorder(
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

    void setHelpText(String helpText) {
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
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        return new Dimension(preferredSize.width, UIUtil.scaleForGUI(HELP_PANEL_HEIGHT));
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension minimumSize = super.getMinimumSize();
        return new Dimension(minimumSize.width, UIUtil.scaleForGUI(HELP_PANEL_HEIGHT));
    }
}