/*
 * Copyright (c) 2013, 2020 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import megamek.client.ui.swing.tileset.EntityImage;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Portrait;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * An extension of JPanel that is intended to be used for visual table renderers
 * allowing for a visual image and html coded text
 *
 * @author Jay Lawson
 */
public class BasicInfo extends JPanel {
    private static final long serialVersionUID = -7337823041775639463L;

    private JLabel lblImage;
    private JLabel lblLoad;

    public BasicInfo() {
        lblImage = new JLabel();
        lblLoad = new JLabel();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(lblLoad, c);
        add(lblLoad);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(lblImage, c);
        add(lblImage);

        lblImage.setBorder(BorderFactory.createEmptyBorder());
    }

    public void setText(String s, String color) {
        lblImage.setText("<html><font size='2' color='" + color + "'>" + s + "</font></html>");
    }

    public void setText(String s) {
        lblImage.setText("<html><font size='2'>" + s + "</font></html>");
    }

    public void setHtmlText(String s) {
        lblImage.setText(s);
    }

    public void highlightBorder() {
        lblImage.setBorder(new LineBorder(UIManager.getColor("Tree.selectionBorderColor"), 4, true));
    }

    public void unhighlightBorder() {
        lblImage.setBorder(javax.swing.BorderFactory.createEtchedBorder());
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

    protected Image getImageFor(Unit u) {
        if (null == MHQStaticDirectoryManager.getMechTileset()) {
            return null;
        }
        Image base = MHQStaticDirectoryManager.getMechTileset().imageFor(u.getEntity());
        if (null == base) {
            return null;
        }
        int tint = PlayerColors.getColorRGB(u.getCampaign().getColorIndex());
        EntityImage entityImage = new EntityImage(base, tint, getCamo(u), this, u.getEntity());
        return entityImage.loadPreviewImage();
    }

    protected Image getCamo(Unit unit) {
        // Try to get the player's camo file.
        Image camo = null;
        try {
            camo = (Image) MHQStaticDirectoryManager.getCamouflage()
                    .getItem(unit.getCamoCategory(), unit.getCamoFileName());
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }
        return camo;
    }

    protected void setPortrait(Person p) {
        String category = p.getPortraitCategory();
        String filename = p.getPortraitFileName();

        // Return a null if the player has selected no portrait file.
        if ((null == category) || (null == filename)) {
            return;
        }

        if (AbstractIcon.ROOT_CATEGORY.equals(category)) {
            category = "";
        }

        if (AbstractIcon.DEFAULT_ICON_FILENAME.equals(filename)) {
            filename = Portrait.DEFAULT_PORTRAIT_FILENAME;
        }

        // Try to get the player's portrait file.
        Image portrait;
        try {
            portrait = (Image) MHQStaticDirectoryManager.getPortraits().getItem(category, filename);
            if (portrait == null) {
                // the image could not be found so switch to default one
                portrait = (Image) MHQStaticDirectoryManager.getPortraits().getItem("", Portrait.DEFAULT_PORTRAIT_FILENAME);
            }
            // make sure no images are longer than 72 pixels
            if (null != portrait) {
                portrait = portrait.getScaledInstance(-1, 58, Image.SCALE_SMOOTH);
                setImage(portrait);
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }
    }

    protected Image getImageFor(Force force) {
        String category = force.getIconCategory();
        String filename = force.getIconFileName();
        LinkedHashMap<String, Vector<String>> iconMap = force.getIconMap();

        if (AbstractIcon.ROOT_CATEGORY.equals(category)) {
            category = "";
        }

        // Return a null if the player has selected no portrait file.
        if ((null == category) || (null == filename)
                || (AbstractIcon.DEFAULT_ICON_FILENAME.equals(filename) && !Force.ROOT_LAYERED.equals(category))) {
            filename = "empty.png";
        }

        // Try to get the player's portrait file.
        Image portrait;
        try {
            portrait = MHQStaticDirectoryManager.buildForceIcon(category, filename, iconMap);
            if (null != portrait) {
                portrait = portrait.getScaledInstance(58, -1, Image.SCALE_SMOOTH);
            } else {
                portrait = (Image) MHQStaticDirectoryManager.getForceIcons().getItem("", "empty.png");
                if (null != portrait) {
                    portrait = portrait.getScaledInstance(58, -1, Image.SCALE_SMOOTH);
                }
            }
            return portrait;
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            return null;
        }
    }
}
