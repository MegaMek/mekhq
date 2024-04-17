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
import mekhq.campaign.storyarc.storypoint.ChoiceStoryPoint;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import mekhq.gui.panels.StoryChoicePanel;
import mekhq.gui.utilities.MarkdownRenderer;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Creates a {@link StoryDialog StoryDialog} with an optional image, text, and choices from which the player can
 * choose a response.
 */
public class StoryChoiceDialog extends StoryDialog implements KeyListener {

    private JFrame frame;
    private JList<String> choiceList;
    private List<String> choices;

    //region Constructors
    public StoryChoiceDialog(final JFrame parent, ChoiceStoryPoint sEvent) {
        super(parent, sEvent);
        choices = new ArrayList<>();
        initialize();
    }
    //endregion Constructors

    //region Initialization
    @Override
    protected void initialize() {
        super.initialize();
        choiceList.requestFocusInWindow();
    }


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

        DefaultMHQScrollablePanel rightPanel = new DefaultMHQScrollablePanel(null, "rightPanel", new GridBagLayout());

        JTextPane txtDesc = new JTextPane();
        txtDesc.setEditable(false);
        txtDesc.setContentType("text/html");
        String text = StoryArc.replaceTokens(((ChoiceStoryPoint) getStoryPoint()).getQuestion(), getStoryPoint().getCampaign());
        txtDesc.setText(MarkdownRenderer.getRenderedHtml(text));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;
        rightPanel.add(txtDesc, gbc);

        JPanel choicePanel = new JPanel();
        choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.PAGE_AXIS));
        DefaultListModel<String> listModel = new DefaultListModel<>();
        int idx = 1;
        for (Entry<String, String> entry : ((ChoiceStoryPoint) getStoryPoint()).getChoices().entrySet()) {
            choices.add(entry.getKey());
            listModel.addElement(idx + "- " + entry.getValue());
            idx++;
        }
        choiceList = new JList<>(listModel);
        choiceList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        choiceList.setCellRenderer(new StoryChoiceRenderer(frame));
        choiceList.setSelectedIndex(0);
        choiceList.addKeyListener(this);
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        choicePanel.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
        choicePanel.add(choiceList);
        rightPanel.add(choicePanel, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane scrollPane = new JScrollPane(rightPanel);
        scrollPane.setMinimumSize(new Dimension(200, 150 ));
        scrollPane.setPreferredSize(new Dimension(200, 150 ));
        mainPanel.add(scrollPane, gbc);

        return mainPanel;
    }
    //endregion Initialization

    public String getChoice() {
        return choices.get(choiceList.getSelectedIndex());
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        // FIXME: This is not working!
        /*
        if (Character.isDigit(e.getKeyChar())) {
            int selected = Integer.parseInt(String.valueOf(e.getKeyChar()));
            choiceList.setSelectedIndex(selected--);
        }
        */
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    private class StoryChoiceRenderer extends StoryChoicePanel implements ListCellRenderer<String> {

        public StoryChoiceRenderer(JFrame frame) {
            super(frame);
        }

        @Override
        public Component getListCellRendererComponent(final JList list,
                                                      final String value, final int index,
                                                      final boolean isSelected,
                                                      final boolean cellHasFocus) {
            final Color foreground = new Color((isSelected
                    ? list.getSelectionForeground() : list.getForeground()).getRGB());
            final Color background = new Color((isSelected
                    ? list.getSelectionBackground() : list.getBackground()).getRGB());
            //setOpaque(true);
            //setForeground(foreground);
            //setBackground(background);

            updateChoice(value, isSelected, getStoryPoint().getCampaign(), foreground, background);

            this.revalidate();

            return this;
        }
    }
}


