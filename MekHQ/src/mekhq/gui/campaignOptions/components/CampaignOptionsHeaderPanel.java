/*
 * Copyright (c) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.components;

import megamek.client.ui.swing.util.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static java.lang.Math.round;
import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;

/**
 * This class provides a custom {@link JPanel} for campaign options, consisting of a label and an image.
 * The panel, label and optional body label names are set based on the provided name parameter.
 * The text for the label(s) is fetched from a resource bundle.
 */
public class CampaignOptionsHeaderPanel extends JPanel {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    /**
     * Creates a {@link JPanel} consisting of a {@link JLabel} above an image.
     * <p>
     * The {@link JPanel} will be named {@code "pnl" + name + "HeaderPanel"}.
     * The resource bundle references for the {@link JLabel} will be {@code "lbl" + name + ".text"}.
     *
     * @param name           the name of the header panel.
     * @param imageAddress   the file path of the image to be displayed in the panel
     */
    public CampaignOptionsHeaderPanel(String name, String imageAddress) {
        this(name, imageAddress, false);
    }

    /**
     * Creates a {@link JPanel} consisting of a {@link JLabel} above an image.
     * If {@code includeBodyText} is {@code true} a second {@link JLabel} is placed after the first.
     * <p>
     * The {@link JPanel} will be named {@code "pnl" + name + "HeaderPanel"}.
     * The resource bundle references for the first {@link JLabel} will be {@code "lbl" + name + ".text"}.
     * The optional second {@link JLabel} is assigned the name {@code ""lbl" + name + "Body"}
     * and uses the following resource bundle reference: {@code "lbl" + name + "Body.text"}.
     *
     * @param name           the name of the header panel.
     * @param imageAddress   the file path of the image to be displayed in the panel
     * @param includeBodyText    if {@code true}, include a second {@link JLabel}.
     */
    public CampaignOptionsHeaderPanel(String name, String imageAddress, boolean includeBodyText) {
        // Fetch and scale image
        ImageIcon imageIcon = new ImageIcon(imageAddress);

        int width = (int) UIUtil.scaleForGUI(round(imageIcon.getIconWidth() * .75));
        int height = (int) UIUtil.scaleForGUI(round(imageIcon.getIconHeight() * .75));

        Image image = imageIcon.getImage();
        Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        imageIcon = new ImageIcon(scaledImage);

        JLabel lblImage = new JLabel(imageIcon);

        // Create header text
        final JLabel lblHeader = new JLabel(resources.getString("lbl" + name + ".text"), SwingConstants.CENTER);
        lblHeader.setName("lbl" + name);
        setFontScaling(lblHeader, true, 2);

        JLabel lblBody = new JLabel();
        if (includeBodyText) {
            lblBody = new JLabel(String.format("<html><div style='width: %s; text-align:justify;'>%s</div></html>",
                UIUtil.scaleForGUI(750),
                resources.getString("lbl" + name + "Body.text")), SwingConstants.CENTER);
            lblBody.setName("lbl" + name + "Body");
            setFontScaling(lblBody, false, 1);
        }

        // Layout panel
        new CampaignOptionsStandardPanel("pnl" + name + "HeaderPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(this);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        this.add(lblHeader, layout);

        layout.gridy++;
        this.add(lblImage, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        this.add(lblBody, layout);
    }
}
