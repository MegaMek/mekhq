/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.panels;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;

import mekhq.campaign.Campaign;
import mekhq.campaign.storyArc.StoryArc;
import mekhq.gui.baseComponents.AbstractMHQPanel;
import mekhq.gui.utilities.MarkdownRenderer;

public class StoryChoicePanel extends AbstractMHQPanel {

    JLabel lblChoice;

    public StoryChoicePanel(final JFrame frame) {
        super(frame, "StoryChoicePanel");
        initialize();
    }

    @Override
    protected void initialize() {
        setLayout(new GridLayout(0, 1));
        lblChoice = new JLabel();
        lblChoice.setText("");
        add(lblChoice);
    }

    protected void updateChoice(String choice, boolean isSelected, Campaign c, Color fg, Color bg) {
        // this gets a little complicated because we have to dynamically set the height
        // of the panel based on
        // how long the text is, but that text may or not be bolded. So we calculate the
        // height as if it was
        // bolded and then switch for unselected cases.
        // the div business sets a fixed width on the label and forces it to wrap.
        lblChoice.setText("<html><div style=\"width:280px;\">"
                                +
                                MarkdownRenderer.getRenderedHtml(StoryArc.replaceTokens("**" + choice + "**", c)) +
                                "</div></html>");
        setBackground(bg);
        lblChoice.setForeground(fg);
        int height = lblChoice.getPreferredSize().height;
        if (!isSelected) {
            lblChoice.setText("<html><div style=\"width:280px;\">"
                                    +
                                    MarkdownRenderer.getRenderedHtml(StoryArc.replaceTokens(choice, c)) +
                                    "</div></html>");
        }
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));
        setMinimumSize(new Dimension(400, height + 10));
        setPreferredSize(new Dimension(400, height + 10));
    }
}
