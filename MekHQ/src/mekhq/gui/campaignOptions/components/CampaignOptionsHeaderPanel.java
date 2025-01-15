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
 * A specialized {@link JPanel} designed for headers in the campaign options dialog.
 * <p>
 * This panel includes a header label, an image, and optionally a body label for additional text.
 * The text for the labels is dynamically loaded from a resource bundle based on the provided name.
 * The image is scaled to fit the layout, and font scaling is applied to the labels to ensure
 * consistent appearance.
 */
public class CampaignOptionsHeaderPanel extends JPanel {
    /**
     * The path to the resource bundle containing label text for the header panel.
     */
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";

    /**
     * The {@link ResourceBundle} used to load localized strings for the header and body labels.
     */
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    /**
     * Constructs a new instance of {@link CampaignOptionsHeaderPanel} with a header label
     * and an image.
     * <p>
     * The panel is named {@code "pnl" + name + "HeaderPanel"}. The header label's text is
     * fetched from a resource bundle using {@code "lbl" + name + ".text"}.
     * The image is loaded from the specified file path and scaled appropriately.
     *
     * @param name         the name of the header panel, used to construct the resource bundle keys
     * @param imageAddress the file path of the image to be displayed in the panel
     */
    public CampaignOptionsHeaderPanel(String name, String imageAddress) {
        this(name, imageAddress, false);
    }

    /**
     * Constructs a new instance of {@link CampaignOptionsHeaderPanel} with a header label,
     * an image, and optionally a body label for descriptive text.
     * <p>
     * The panel is named {@code "pnl" + name + "HeaderPanel"}. The header label's text is
     * fetched from a resource bundle using {@code "lbl" + name + ".text"}. If {@code includeBodyText}
     * is {@code true}, an additional body label is created using the resource key
     * {@code "lbl" + name + "Body.text"}. The image is loaded from the specified file path
     * and scaled appropriately.
     *
     * @param name            the name of the header panel, used to construct the resource bundle keys
     * @param imageAddress    the file path of the image to be displayed in the panel
     * @param includeBodyText if {@code true}, includes a body label below the image
     */
    public CampaignOptionsHeaderPanel(String name, String imageAddress, boolean includeBodyText) {
        // Load and scale the image using the provided file path
        ImageIcon imageIcon = new ImageIcon(imageAddress);
        int width = (int) UIUtil.scaleForGUI(round(imageIcon.getIconWidth() * 0.75));
        int height = (int) UIUtil.scaleForGUI(round(imageIcon.getIconHeight() * 0.75));
        Image image = imageIcon.getImage();
        Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        imageIcon = new ImageIcon(scaledImage);

        // Create a JLabel to display the image in the panel
        JLabel lblImage = new JLabel(imageIcon);

        // Create the header label with text from the resource bundle
        final JLabel lblHeader = new JLabel(resources.getString("lbl" + name + ".text"), SwingConstants.CENTER);
        lblHeader.setName("lbl" + name);
        setFontScaling(lblHeader, true, 2);

        // Optionally create a body label with additional text, if includeBodyText is true
        JLabel lblBody = new JLabel();
        if (includeBodyText) {
            lblBody = new JLabel(String.format(
                    "<html><div style='width: %s; text-align:justify;'>%s</div></html>",
                    UIUtil.scaleForGUI(750),
                    resources.getString("lbl" + name + "Body.text")), SwingConstants.CENTER);
            lblBody.setName("lbl" + name + "Body");
            setFontScaling(lblBody, false, 1);
        }

        // Initialize the panel's layout using a GridBagLayout
        new CampaignOptionsStandardPanel("pnl" + name + "HeaderPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(this);

        // Configure and add components to the panel
        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        this.add(lblHeader, layout);

        layout.gridy++;
        this.add(lblImage, layout);

        if (includeBodyText) {
            layout.gridy++;
            layout.gridwidth = 1;
            this.add(lblBody, layout);
        }
    }
}
