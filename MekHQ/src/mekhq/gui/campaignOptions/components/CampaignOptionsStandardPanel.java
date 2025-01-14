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
 * This class provides a custom {@link JPanel} for campaign options.
 * Offers an optional untitled border, and the panel name is set to "pnl" + name.
 */
public class CampaignOptionsStandardPanel extends JPanel {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    /**
     * Creates a standardized {@link JPanel} without a border.
     * <p>
     * {@code createGroupLayout} should also be called and the resulting {@link GroupLayout}
     * assigned to the panel via {@code setLayout}.
     *
     * @param name         the name of the panel.
     */
    public CampaignOptionsStandardPanel(String name) {
        this(name, false, "");
    }

    /**
     * Creates a standardized {@link JPanel} with an untitled border.
     * <p>
     * {@code createGroupLayout} should also be called and the resulting {@link GroupLayout}
     * assigned to the panel via {@code setLayout}.
     *
     * @param name         the name of the panel.
     * @param includeBorder whether the panel should have a border.
     */
    public CampaignOptionsStandardPanel(String name, boolean includeBorder) {
        this(name, includeBorder, "");
    }

    /**
     * Creates a standardized {@link JPanel} with a titled border.
     * <p>
     * {@code createGroupLayout} should also be called and the resulting {@link GroupLayout}
     * assigned to the panel via {@code setLayout}.
     * <p>
     * If {@code borderTitle} isn't empty the resource bundle reference, used to fetch the border's
     * title, will be {@code "lbl" + borderTitle + ".text"}
     *
     * @param name         the name of the panel.
     * @param includeBorder whether the panel should have a border.
     */
    public CampaignOptionsStandardPanel(String name, boolean includeBorder, String borderTitle) {
        borderTitle = borderTitle.isBlank() ? "" : resources.getString("lbl" + borderTitle + ".text");

        new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension standardSize = super.getPreferredSize();
                return UIUtil.scaleForGUI((Math.max(standardSize.width, 500)), standardSize.height);
            }
        };

        if (includeBorder) {
            if (borderTitle.isBlank()) {
                setBorder(BorderFactory.createEtchedBorder());
            } else {
                setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(),
                    String.format("<html>%s</html>", borderTitle)));
            }
        }

        setName("pnl" + name);
    }
}
