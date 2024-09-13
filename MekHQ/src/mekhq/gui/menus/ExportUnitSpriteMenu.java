/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.menus;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenuItem;

import megamek.client.ui.dialogs.CamoChooserDialog;
import megamek.common.icons.Camouflage;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import mekhq.gui.GUI;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.io.FileType;

/**
 * This is a standard menu that takes a unit and lets the user export their icon
 * with the camouflage
 * applied to it.
 */
public class ExportUnitSpriteMenu extends JScrollableMenu {
    private static final MMLogger logger = MMLogger.create(ExportUnitSpriteMenu.class);

    // region Constructors
    public ExportUnitSpriteMenu(final JFrame frame, final Campaign campaign, final Unit unit) {
        super("ExportUnitSpriteMenu");
        initialize(frame, campaign, unit);
    }
    // endregion Constructors

    // region Initialization
    private void initialize(final JFrame frame, final Campaign campaign, final Unit unit) {
        // Initialize Menu
        setText(resources.getString("ExportUnitSpriteMenu.title"));
        setToolTipText(resources.getString("ExportUnitSpriteMenu.toolTipText"));

        final JMenuItem miCurrentCamouflage = new JMenuItem(resources.getString("miCurrentCamouflage.text"));
        miCurrentCamouflage.setToolTipText(resources.getString("miCurrentCamouflage.toolTipText"));
        miCurrentCamouflage.setName("miCurrentCamouflage");
        miCurrentCamouflage
                .addActionListener(evt -> exportSprite(frame, unit, unit.getUtilizedCamouflage(campaign), false));
        add(miCurrentCamouflage);

        final JMenuItem miCurrentDamage = new JMenuItem(resources.getString("miCurrentDamage.text"));
        miCurrentDamage.setToolTipText(resources.getString("miCurrentDamage.toolTipText"));
        miCurrentDamage.setName("miCurrentDamage");
        miCurrentDamage.addActionListener(evt -> exportSprite(frame, unit, new Camouflage(), true));
        add(miCurrentDamage);

        final JMenuItem miCurrentCamouflageAndDamage = new JMenuItem(
                resources.getString("miCurrentCamouflageAndDamage.text"));
        miCurrentCamouflageAndDamage.setToolTipText(resources.getString("miCurrentCamouflageAndDamage.toolTipText"));
        miCurrentCamouflageAndDamage.setName("miCurrentCamouflageAndDamage");
        miCurrentCamouflageAndDamage
                .addActionListener(evt -> exportSprite(frame, unit, unit.getUtilizedCamouflage(campaign), true));
        add(miCurrentCamouflageAndDamage);

        final JMenuItem miSelectedCamouflage = new JMenuItem(resources.getString("miSelectedCamouflage.text"));
        miSelectedCamouflage.setToolTipText(resources.getString("miSelectedCamouflage.toolTipText"));
        miSelectedCamouflage.setName("miSelectedCamouflage");
        miSelectedCamouflage.addActionListener(evt -> {
            final CamoChooserDialog camoChooserDialog = new CamoChooserDialog(frame,
                    unit.getUtilizedCamouflage(campaign));
            if (camoChooserDialog.showDialog().isConfirmed()) {
                exportSprite(frame, unit, camoChooserDialog.getSelectedItem(), false);
            }
        });
        add(miSelectedCamouflage);

        final JMenuItem miSelectedCamouflageAndCurrentDamage = new JMenuItem(
                resources.getString("miSelectedCamouflageAndCurrentDamage.text"));
        miSelectedCamouflageAndCurrentDamage.setToolTipText(
                resources.getString("miSelectedCamouflageAndCurrentDamage.toolTipText"));
        miSelectedCamouflageAndCurrentDamage.setName("miSelectedCamouflageAndCurrentDamage");
        miSelectedCamouflageAndCurrentDamage.addActionListener(evt -> {
            final CamoChooserDialog camoChooserDialog = new CamoChooserDialog(frame,
                    unit.getUtilizedCamouflage(campaign));
            if (camoChooserDialog.showDialog().isConfirmed()) {
                exportSprite(frame, unit, camoChooserDialog.getSelectedItem(), true);
            }
        });
        add(miSelectedCamouflageAndCurrentDamage);
    }
    // endregion Initialization

    private void exportSprite(final JFrame frame, final Unit unit, final Camouflage camouflage,
            final boolean showDamage) {
        // Save Location
        File file = GUI.fileDialogSave(frame, resources.getString("ExportUnitSpriteDialog.title"), FileType.PNG,
                MekHQ.getMHQOptions().getUnitSpriteExportPath(), unit.getName() + ".png").orElse(null);
        if (file == null) {
            return;
        }
        MekHQ.getMHQOptions().setUnitSpriteExportPath(file.getParent());

        // Ensure it's a PNG file
        final String path = file.getPath();
        if (!path.endsWith(".png")) {
            file = new File(path + ".png");
        }

        // Get the Sprite
        final Image sprite = unit.getImage(this, camouflage, showDamage);
        if (sprite == null) {
            logger.error("Null sprite");
            return;
        }

        // Export to File
        try {
            ImageIO.write((BufferedImage) sprite, "png", file);
        } catch (Exception ex) {
            logger.error("Failed to export to file", ex);
        }
    }
}
