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

/**
 * A specialized {@link JPanel} tailored for use in campaign options dialogs.
 * <p>
 * This panel offers configurable borders (titled or untitled) and applies a standardized
 * layout and styling for consistency across the UI. The panel's name can also be dynamically
 * assigned based on the provided {@code name} parameter.
 */
public class CampaignOptionsStandardPanel extends JPanel {

    /**
     * The path to the resource bundle containing localized strings for border titles.
     */
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";

    /**
     * The {@link ResourceBundle} used to fetch localized strings for titles and labels.
     */
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    /**
     * Constructs a {@link CampaignOptionsStandardPanel} without a border.
     * <p>
     * The name of the panel is set to {@code "pnl" + name}. The layout should
     * be configured using {@code createGroupLayout} and assigned to the panel using {@code setLayout}.
     *
     * @param name the name of the panel, used to set its internal name
     */
    public CampaignOptionsStandardPanel(String name) {
        this(name, false, "");
    }

    /**
     * Constructs a {@link CampaignOptionsStandardPanel} with an optional untitled border.
     * <p>
     * If {@code includeBorder} is {@code true}, an etched border is applied to the panel. The name of
     * the panel is set to {@code "pnl" + name}. The layout should be configured using
     * {@code createGroupLayout} and assigned to the panel using {@code setLayout}.
     *
     * @param name           the name of the panel, used to set its internal name
     * @param includeBorder  {@code true} if the panel should include an untitled border
     */
    public CampaignOptionsStandardPanel(String name, boolean includeBorder) {
        this(name, includeBorder, "");
    }

    /**
     * Constructs a {@link CampaignOptionsStandardPanel} with an optional titled border.
     * <p>
     * If {@code includeBorder} is {@code true}, a border is applied to the panel. If {@code borderTitle}
     * is provided, it is used to fetch the localized string for the border's title from the resource bundle.
     * The name of the panel is set to {@code "pnl" + name}. The layout should be configured using
     * {@code createGroupLayout} and assigned to the panel using {@code setLayout}.
     *
     * @param name          the name of the panel, used to set its internal name
     * @param includeBorder {@code true} if the panel should include a border
     * @param borderTitle   the resource bundle key for the border's title; an empty string indicates no title
     */
    public CampaignOptionsStandardPanel(String name, boolean includeBorder, String borderTitle) {
        borderTitle = borderTitle.isBlank() ? "" : resources.getString("lbl" + borderTitle + ".text");

        // Set a standardized panel behavior and preferred size scaling
        new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension standardSize = super.getPreferredSize();
                return UIUtil.scaleForGUI(Math.max(standardSize.width, 500), standardSize.height);
            }
        };

        if (includeBorder) {
            if (borderTitle.isBlank()) {
                // Add an untitled etched border
                setBorder(BorderFactory.createEtchedBorder());
            } else {
                // Add a titled border with localized title
                setBorder(BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(),
                        String.format("<html>%s</html>", borderTitle)));
            }
        }

        // Set the name of the panel dynamically
        setName("pnl" + name);
    }
}
