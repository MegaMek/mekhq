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

import mekhq.campaign.storyarc.storyevent.ChoiceStoryEvent;
import mekhq.gui.utilities.MarkdownRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class StoryChoiceDialog extends StoryDialog {

    private ChoiceStoryEvent storyEvent;

    private ButtonGroup choiceGroup;

    //region Constructors
    public StoryChoiceDialog(final JFrame parent, ChoiceStoryEvent sEvent) {
        super(parent, sEvent.getTitle());
        this.storyEvent = sEvent;
        initialize();
    }
    //endregion Constructors

    //region Initialization

    @Override
    protected Container getMainPanel() {

        GridBagConstraints gbc = new GridBagConstraints();
        JPanel mainPanel = new JPanel(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 0.0;
        gbc.weighty = 1.0;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(getImagePanel(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.BOTH;
        JTextPane txtDesc = new JTextPane();
        txtDesc.setEditable(false);
        txtDesc.setContentType("text/html");
        txtDesc.setText(MarkdownRenderer.getRenderedHtml(storyEvent.getQuestion()));
        JScrollPane scrollPane = new JScrollPane(txtDesc);
        scrollPane.setPreferredSize(new Dimension(150, 150 ));
        mainPanel.add(scrollPane, gbc);

        //Create the radio buttons.
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.PAGE_AXIS));
        choiceGroup = new ButtonGroup();
        JRadioButton radioBtn;
        boolean firstEntry = true;
        for (Map.Entry<String, String> entry : storyEvent.getChoices().entrySet()) {
            radioBtn = new JRadioButton(entry.getValue());
            radioBtn.setActionCommand(entry.getKey());
            btnPanel.add(radioBtn);
            choiceGroup.add(radioBtn);
            if(firstEntry) {
                radioBtn.setSelected(true);
            }
            firstEntry = false;
        }
        mainPanel.add(btnPanel, gbc);

        return mainPanel;
    }
    //endregion Initialization

    public String getChoice() {
        return choiceGroup.getSelection().getActionCommand();
    }

}
