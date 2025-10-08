/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
 * A specialized JPanel wrapper for repair tasks. This is different from BasicInfo due to the need for an extra image on
 * the right side.
 *
 * @author Cord Awtry (kipstafoo)
 */
public class RepairTaskInfo extends JPanel {
    private final JLabel lblImage;
    private final JLabel lblSecondaryImage;

    public RepairTaskInfo(IconPackage iconPackage) {
        lblImage = new JLabel();

        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridBagLayout);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.WEST;
        gridBagLayout.setConstraints(lblImage, c);
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
        gridBagLayout.setConstraints(lblSecondaryImage, c);
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
