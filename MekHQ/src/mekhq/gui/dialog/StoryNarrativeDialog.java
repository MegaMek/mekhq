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

import mekhq.campaign.storyarc.storyevent.NarrativeStoryEvent;
import mekhq.gui.utilities.MarkdownRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StoryNarrativeDialog extends JDialog implements ActionListener {

    private NarrativeStoryEvent storyEvent;

    private JButton doneButton;

    //region Constructors
    public StoryNarrativeDialog(final JFrame parent, NarrativeStoryEvent sEvent) {
        super(parent, sEvent.getTitle(), true);
        this.storyEvent = sEvent;
        initGUI();
    }
    //endregion Constructors

    //region Initialization
    private void initGUI() {
        setLayout(new BorderLayout());
        add(getButtonPanel(), BorderLayout.SOUTH);
        add(getMainPanel(), BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(getParent());
        setResizable(false);
    }

    private JPanel getButtonPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout());

        doneButton = new JButton("Done");
        //doneButton.setMnemonic('o');
        doneButton.addActionListener(this);
        buttonPanel.add(doneButton, BorderLayout.LINE_END);

        return buttonPanel;
    }

    private JPanel getMainPanel() {

        JPanel mainPanel = new JPanel(new GridLayout(1, 2));

        //TODO: put images here

        JTextPane txtDesc = new JTextPane();
        txtDesc.setEditable(false);
        txtDesc.setContentType("text/html");
        txtDesc.setText(MarkdownRenderer.getRenderedHtml(storyEvent.getNarrative()));
        JScrollPane scrollPane = new JScrollPane(txtDesc);
        mainPanel.add(scrollPane);

        mainPanel.setMinimumSize(new Dimension(400, 400));
        mainPanel.setPreferredSize(new Dimension(400, 400));

        return mainPanel;
    }
    //endregion Initialization

    @Override
    public void actionPerformed(ActionEvent e) {
        if (doneButton.equals(e.getSource())) {
            this.setVisible(false);
        }
    }

}
