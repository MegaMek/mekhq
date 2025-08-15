/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.components;

import static java.awt.Color.BLACK;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.utilities.ImageUtilities.addTintToImageIcon;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import megamek.client.ui.util.UIUtil;

/**
 * A specialized {@link JPanel} designed for headers in the campaign options dialog.
 * <p>
 * This panel includes a header label, an image, and optionally a body label for additional text. The text for the
 * labels is dynamically loaded from a resource bundle based on the provided name. The image is scaled to fit the
 * layout, and font scaling is applied to the labels to ensure consistent appearance.
 */
public class CampaignOptionsHeaderPanel extends JPanel {
    private static final String TIP_PANEL_NAME = "TipPanel";

    private int tipPanelHeight = 0;

    public int getTipPanelHeight() {
        return tipPanelHeight;
    }

    public static String getTipPanelName() {
        return TIP_PANEL_NAME;
    }

    @Deprecated(since = "0.50.06", forRemoval = true)
    public CampaignOptionsHeaderPanel(String name, String imageAddress, boolean includeBodyText) {
        this(name, imageAddress, includeBodyText, false, 0);
    }

    /**
     * Constructs a {@code CampaignOptionsHeaderPanel} that displays a header label and an image.
     *
     * <p>The panel is named {@code "pnl" + name + "HeaderPanel"}. The header label's text is fetched from a resource
     * bundle using {@code "lbl" + name + ".text"}. The image is loaded from the specified file path and scaled
     * appropriately.</p>
     *
     * @param name         a unique identifier used to fetch resource bundle entries and to form the panel's name
     * @param imageAddress the path to the image file displayed in the panel
     */
    public CampaignOptionsHeaderPanel(String name, String imageAddress) {
        this(name, imageAddress, false, false, 0);
    }

    /**
     * Constructs a {@code CampaignOptionsHeaderPanel} that displays a header label and an image.
     *
     * <p>The panel is named {@code "pnl" + name + "HeaderPanel"}. The header label's text is fetched from a resource
     * bundle using {@code "lbl" + name + ".text"}. The image is loaded from the specified file path and scaled
     * appropriately.</p>
     *
     * @param name           a unique identifier used to fetch resource bundle entries and to form the panel's name
     * @param imageAddress   the path to the image file displayed in the panel
     * @param tipPanelHeight the number of empty line breaks to reserve vertical space for the tip panel area
     */
    public CampaignOptionsHeaderPanel(String name, String imageAddress, int tipPanelHeight) {
        this(name, imageAddress, false, true, tipPanelHeight);
    }

    /**
     * Constructs a {@code CampaignOptionsHeaderPanel} that displays a header label, an image, and optionally includes
     * additional descriptive body text and/or a tip panel.
     *
     * <p>The panel is named {@code "pnl" + name + "HeaderPanel"}. The header label's text is fetched from a resource
     * bundle using {@code "lbl" + name + ".text"}. The image is loaded from the specified file path and scaled
     * appropriately.</p>
     *
     * @param name            a unique identifier used for resource bundle lookups and to form the panel's name
     * @param imageAddress    the path to the image file to display at the top of the panel
     * @param includeBodyText if true, includes a body label beneath the image with descriptive text
     * @param includeTipPanel if true, displays a tip panel beneath the body text (or image if body text is excluded)
     * @param tipPanelHeight  the number of empty line breaks to reserve vertical space for the tip panel area
     */
    public CampaignOptionsHeaderPanel(String name, String imageAddress, boolean includeBodyText,
          boolean includeTipPanel, int tipPanelHeight) {
        this.tipPanelHeight = tipPanelHeight;

        // Load and scale the image using the provided file path
        ImageIcon imageIcon = new ImageIcon(imageAddress);
        imageIcon = scaleImageIcon(imageIcon, 100, true);
        imageIcon = addTintToImageIcon(imageIcon.getImage(), BLACK);

        // Create a JLabel to display the image in the panel
        JLabel lblImage = new JLabel(imageIcon);

        // Create the header label with text from the resource bundle
        final JLabel lblHeader = new JLabel("<html>" +
                                                  getFormattedTextAt(getCampaignOptionsResourceBundle(),
                                                        "lbl" + name + ".text") +
                                                  "</html>",
              SwingConstants.CENTER);
        lblHeader.setName("lbl" + name);
        setFontScaling(lblHeader, true, 2);

        // Optionally create a body label with additional text if includeBodyText is true
        JLabel lblBody = new JLabel();
        if (includeBodyText) {
            lblBody = new JLabel(String.format("<html><div style='width: %s'>%s</div></html>",
                  UIUtil.scaleForGUI(750), getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + "Body.text")),
                  SwingConstants.CENTER);
            lblBody.setName("lbl" + name + "Body");
            setFontScaling(lblBody, false, 1);
        }

        JLabel lblTip = new JLabel();
        if (includeTipPanel) {
            // This stops the tip panel from bouncing around too much as new options are selected
            StringBuilder lineBreaks = new StringBuilder();
            for (int lineBreak = 0; lineBreak < tipPanelHeight; lineBreak++) {
                lineBreaks.append("<br>");
            }

            lblTip.setName("lbl" + name + TIP_PANEL_NAME);
            lblTip.setText("<html>" + lineBreaks + "</html>");
            lblTip.setHorizontalAlignment(SwingConstants.CENTER);
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

        if (includeTipPanel) {
            layout.gridy++;
            layout.gridwidth = 5;
            this.add(new JSeparator(SwingConstants.HORIZONTAL), layout);
            layout.gridy++;
            this.add(lblTip, layout);
            layout.gridy++;
            this.add(new JSeparator(SwingConstants.HORIZONTAL), layout);
        }
    }
}
