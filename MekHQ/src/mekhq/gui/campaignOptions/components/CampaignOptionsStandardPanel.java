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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.CAMPAIGN_OPTIONS_PANEL_WIDTH;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Dimension;
import javax.swing.JPanel;

import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * A specialized {@link JPanel} tailored for use in campaign options dialogs.
 * <p>
 * This panel offers configurable borders (titled or untitled) and applies a standardized layout and styling for
 * consistency across the UI. The panel's name can also be dynamically assigned based on the provided {@code name}
 * parameter.
 */
public class CampaignOptionsStandardPanel extends JPanel {
    /**
     * Constructs a {@link CampaignOptionsStandardPanel} without a border.
     * <p>
     * The name of the panel is set to {@code "pnl" + name}. The layout should be configured using
     * {@code createGroupLayout} and assigned to the panel using {@code setLayout}.
     *
     * @param name the name of the panel, used to set its internal name
     */
    public CampaignOptionsStandardPanel(String name) {
        this(name, false, "");
    }

    /**
     * Constructs a {@link CampaignOptionsStandardPanel} with an optional untitled border.
     * <p>
     * If {@code includeBorder} is {@code true}, an etched border is applied to the panel. The name of the panel is set
     * to {@code "pnl" + name}. The layout should be configured using {@code createGroupLayout} and assigned to the
     * panel using {@code setLayout}.
     *
     * @param name          the name of the panel, used to set its internal name
     * @param includeBorder {@code true} if the panel should include an untitled border
     */
    public CampaignOptionsStandardPanel(String name, boolean includeBorder) {
        this(name, includeBorder, "");
    }

    /**
     * Constructs a {@link CampaignOptionsStandardPanel} with an optional titled border.
     * <p>
     * If {@code includeBorder} is {@code true}, a border is applied to the panel. If {@code borderTitle} is provided,
     * it is used to fetch the localized string for the border's title from the resource bundle. The name of the panel
     * is set to {@code "pnl" + name}. The layout should be configured using {@code createGroupLayout} and assigned to
     * the panel using {@code setLayout}.
     *
     * @param name          the name of the panel, used to set its internal name
     * @param includeBorder {@code true} if the panel should include a border
     * @param borderTitle   the resource bundle key for the border's title; an empty string indicates no title
     */
    public CampaignOptionsStandardPanel(String name, boolean includeBorder, String borderTitle) {
        borderTitle = borderTitle.isBlank() ?
                            "" :
                            getTextAt(getCampaignOptionsResourceBundle(), "lbl" + borderTitle + ".text");

        // Set a standardized panel behavior and preferred size scaling
        new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension standardSize = super.getPreferredSize();
                return scaleForGUI(Math.max(standardSize.width, CAMPAIGN_OPTIONS_PANEL_WIDTH), standardSize.height);
            }

            @Override
            public Dimension getMinimumSize() {
                Dimension standardSize = super.getPreferredSize();
                return scaleForGUI(Math.max(standardSize.width, CAMPAIGN_OPTIONS_PANEL_WIDTH), standardSize.height);
            }
        };

        if (includeBorder) {
            if (borderTitle.isBlank()) {
                // Add an untitled etched border
                setBorder(RoundedLineBorder.createRoundedLineBorder());
            } else {
                // Add a titled border with localized title
                setBorder(RoundedLineBorder.createRoundedLineBorder(borderTitle));
            }
        }

        // Set the name of the panel dynamically
        setName("pnl" + name);
    }
}
