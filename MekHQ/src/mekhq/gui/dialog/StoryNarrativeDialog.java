/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.storyarc.storypoint.NarrativeStoryPoint;
import mekhq.gui.utilities.MarkdownRenderer;

import javax.swing.*;
import java.awt.*;

/**
 * Creates a simple narrative {@link StoryDialog StoryDialog} with an optional image and text.
 */
public class StoryNarrativeDialog extends StoryDialog {

    //region Constructors
    public StoryNarrativeDialog(final JFrame parent, NarrativeStoryPoint sEvent) {
        super(parent, sEvent);
        initialize();
    }
    //endregion Constructors

    @Override
    protected Container getMainPanel() {

        GridBagConstraints gbc = new GridBagConstraints();
        JPanel mainPanel = new JPanel(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 0.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(getImagePanel(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JTextPane txtDesc = new JTextPane();
        txtDesc.setEditable(false);
        txtDesc.setContentType("text/html");
        String text = StoryArc.replaceTokens(((NarrativeStoryPoint) getStoryPoint()).getNarrative(), getStoryPoint().getCampaign());
        txtDesc.setText(MarkdownRenderer.getRenderedHtml(text));
        txtDesc.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(txtDesc);
        mainPanel.add(scrollPane, gbc);

        return mainPanel;
    }
    //endregion Initialization

}
