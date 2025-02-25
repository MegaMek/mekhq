/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import mekhq.IconPackage;

/**
 * A specialized JPanel wrapper for repair tasks. This is different from
 * BasicInfo due to the need for an extra image on the right side.
 *
 * @author Cord Awtry (kipstafoo)
 */
public class RepairTaskInfo extends JPanel {
    private JLabel lblImage;
    private JLabel lblSecondaryImage;

    public RepairTaskInfo(IconPackage i) {
        lblImage = new JLabel();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.BOTH;
        gridbag.setConstraints(lblImage, c);
        add(lblImage);

        lblSecondaryImage = new JLabel();
        lblSecondaryImage.setText("");

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 5);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(lblSecondaryImage, c);
        add(lblSecondaryImage);

        this.setBorder(BorderFactory.createEmptyBorder());
    }

    public void setText(String s) {
        lblImage.setText("<html><font>" + s + "</font></html>");
    }

    public void highlightBorder() {
        this.setBorder(new LineBorder(
                UIManager.getColor("Tree.selectionBorderColor"), 4, true));
    }

    public void unhighlightBorder() {
        this.setBorder(BorderFactory.createEtchedBorder());
    }

    public void setImage(Image img) {
        if (null == img) {
            lblImage.setIcon(null);
        } else {
            lblImage.setIcon(new ImageIcon(img));
        }
    }

    public void setSecondaryImage(Image img) {
        if (null == img) {
            lblSecondaryImage.setIcon(null);
        } else {
            lblSecondaryImage.setIcon(new ImageIcon(img));
        }
    }
}
