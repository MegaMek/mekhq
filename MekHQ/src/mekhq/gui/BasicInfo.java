/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

/**
 * An extension of JPanel that is intended to be used for visual table renderers allowing for a visual image and html
 * coded text
 *
 * @author Jay Lawson
 */
public class BasicInfo extends JPanel {
    private final JLabel lblImage;
    private final JLabel lblLoad;

    public BasicInfo() {
        lblImage = new JLabel();
        lblLoad = new JLabel();

        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        setLayout(gridBagLayout);

        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagLayout.setConstraints(lblLoad, gridBagConstraints);
        add(lblLoad);

        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagLayout.setConstraints(lblImage, gridBagConstraints);
        add(lblImage);

        lblImage.setBorder(BorderFactory.createEmptyBorder());
    }

    public void setText(String s, String color) {
        lblImage.setText("<html><font color='" + color + "'>" + s + "</font></html>");
    }

    public void setText(String s) {
        lblImage.setText("<html>" + s + "</html>");
    }

    public void setHtmlText(String s) {
        lblImage.setText(s);
    }

    public void highlightBorder() {
        lblImage.setBorder(new LineBorder(UIManager.getColor("Tree.selectionBorderColor"), 4, true));
    }

    public void unhighlightBorder() {
        lblImage.setBorder(BorderFactory.createEtchedBorder());
    }

    public void clearImage() {
        lblImage.setIcon(null);
    }

    public void setImage(Image img) {
        lblImage.setIcon(new ImageIcon(img));
    }

    public JLabel getLabel() {
        return lblImage;
    }

    public void setLoad(boolean load) {
        // if this is a loaded unit then do something with lblLoad to make
        // it show up
        // otherwise clear lblLoad
        if (load) {
            lblLoad.setText(" +");
        } else {
            lblLoad.setText("");
        }
    }
}
