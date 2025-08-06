/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import mekhq.campaign.storyarc.Personality;
import mekhq.campaign.storyarc.storypoint.DialogStoryPoint;

/**
 * This is the base class for dialogs related to the Story Arc, to help create a similar look and feel. Inheriting
 * classes must call initialize() in their constructors and override getMainPanel()
 */
public abstract class StoryDialog extends JDialog implements ActionListener {

    private JButton doneButton;

    private int imgWidth;

    private DialogStoryPoint storyPoint;

    public StoryDialog(final JFrame parent, DialogStoryPoint sEvent) {
        super(parent, sEvent.getTitle(), true);
        this.storyPoint = sEvent;
    }

    // region initialization
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
        doneButton.addActionListener(this);
        buttonPanel.add(doneButton, BorderLayout.LINE_END);

        return buttonPanel;
    }

    protected abstract Container getMainPanel();
    // endregion initialization

    protected DialogStoryPoint getStoryPoint() {
        return storyPoint;
    }

    protected JPanel getImagePanel() {
        JPanel imagePanel = new JPanel(new BorderLayout());

        imgWidth = 0;
        Image img = getStoryPoint().getImage();

        // check for personality as this will override image
        Personality p = getStoryPoint().getPersonality();
        if (null != p) {
            img = p.getImage();
        }

        if (null != img) {
            ImageIcon icon = new ImageIcon(img);
            imgWidth = icon.getIconWidth();
            JLabel imgLbl = new JLabel();
            imgLbl.setIcon(icon);
            imagePanel.add(imgLbl, BorderLayout.CENTER);
            if (null != p) {
                // add a caption
                imagePanel.add(new JLabel(p.getTitle(), SwingConstants.CENTER), BorderLayout.PAGE_END);
            }
        }

        // we can grab and put here in an image panel
        return imagePanel;
    }

    protected void setDialogSize() {

        int width = 400 + imgWidth;
        int height = 400;
        setMinimumSize(new Dimension(width, height));
        setPreferredSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
    }

    // region Listeners
    @Override
    public void actionPerformed(ActionEvent e) {
        if (doneButton.equals(e.getSource())) {
            this.setVisible(false);
        }
    }
    // endregion Listeners

}
