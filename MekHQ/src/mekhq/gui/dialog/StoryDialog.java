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

import mekhq.campaign.storyarc.StoryEvent;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


/**
 * This is the base class for dialogs related to the Story Arc, to help create a similar look and feel
 *
 * Inheriting classes must call initialize() in their constructors and override getMainPanel()
 */
public abstract class StoryDialog extends JDialog implements ActionListener {

    private JButton doneButton;

    private int imgWidth;

    private StoryEvent storyEvent;

    public StoryDialog(final JFrame parent, StoryEvent sEvent) {
        super(parent, sEvent.getTitle(), true);
        this.storyEvent = sEvent;
    }

    //region initialization
    protected void initialize() {
        setLayout(new BorderLayout());
        add(getButtonPanel(), BorderLayout.SOUTH);
        add(getMainPanel(), BorderLayout.CENTER);

        setDialogSize();
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

    protected abstract Container getMainPanel();
    //endregion initialization

    protected StoryEvent getStoryEvent() {
        return storyEvent;
    }

    protected JPanel getImagePanel() {
        JPanel imagePanel = new JPanel();

        imgWidth = 0;
        Image img = getStoryEvent().getImage();

        if(null != img) {
            ImageIcon icon = new ImageIcon(img);
            imgWidth = icon.getIconWidth();
            JLabel imgLbl = new JLabel();
            imgLbl.setIcon(icon);
            imagePanel.add(imgLbl);
        }

        //we can grab and put here in an image panel
        return imagePanel;
    }

    protected void setDialogSize() {

        int width = 400+imgWidth;
        int height = 400;
        setMinimumSize(new Dimension(width, height));
        setPreferredSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
    }

    //region Listeners
    @Override
    public void actionPerformed(ActionEvent e) {
        if (doneButton.equals(e.getSource())) {
            this.setVisible(false);
        }
    }
    //endregion Listeners

}
