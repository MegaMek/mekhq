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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;
import javax.swing.JPanel;

import megamek.common.annotations.Nullable;

/**
 * A custom implementation of {@link GridBagConstraints} designed for use with panels in the campaign options dialog.
 * <p>
 * This class allows for simplified initialization of {@link GridBagConstraints} with configurable anchor and fill
 * properties, as well as preset insets.
 * <p>
 * It is intended to be paired with {@code CampaignOptionsStandardPanel} or similar components using the
 * {@link GridBagLayout}.
 */
public class CampaignOptionsGridBagConstraints extends GridBagConstraints {

    /**
     * Constructs an instance of {@link GridBagConstraints} with default settings for the specified {@link JPanel}.
     * <p>
     * The {@code JPanel} will automatically be set to use a {@link GridBagLayout}. Default constraints include:
     * </p>
     * <ul>
     *   <li> {@code anchor} set to {@link GridBagConstraints#NORTHWEST} </li>
     *   <li> {@code fill} set to {@link GridBagConstraints#BOTH} </li>
     *   <li> {@code insets} set to {@code new Insets(5, 5, 5, 5)} </li>
     * </ul>
     *
     * @param panel the {@link JPanel} for which the {@link GridBagConstraints} is created
     */
    public CampaignOptionsGridBagConstraints(JPanel panel) {
        this(panel, null, null);
    }

    /**
     * Constructs an instance of {@link GridBagConstraints} with configurable anchor and fill properties for the
     * specified {@link JPanel}.
     * <p>
     * The {@code JPanel} will automatically be set to use a {@link GridBagLayout}. If {@code anchor} or {@code fill}
     * values are not provided, the following default
     * </p>
     * values are used:
     * <ul>
     *   <li>Default {@code anchor}: {@link GridBagConstraints#NORTHWEST}</li>
     *   <li>Default {@code fill}: {@link GridBagConstraints#BOTH}</li>
     * </ul>
     * Default {@code insets} are set to {@code new Insets(5, 5, 5, 5)}.
     *
     * @param panel  the {@link JPanel} for which the {@link GridBagConstraints} is created
     * @param anchor the anchor setting for the {@link GridBagConstraints}, or {@code null} to use the default value
     *               {@link GridBagConstraints#NORTHWEST}
     * @param fill   the fill setting for the {@link GridBagConstraints}, or {@code null} to use the default value
     *               {@link GridBagConstraints#BOTH}
     */
    public CampaignOptionsGridBagConstraints(JPanel panel, @Nullable Integer anchor, @Nullable Integer fill) {
        super();

        // Set up GridBagLayout on the panel
        panel.setLayout(new GridBagLayout());

        // Assign anchor and fill, using defaults if not provided
        this.anchor = Objects.requireNonNullElse(anchor, GridBagConstraints.NORTHWEST);
        this.fill = Objects.requireNonNullElse(fill, GridBagConstraints.BOTH);

        // Set default insets
        this.insets = new Insets(5, 5, 5, 5);
    }
}
