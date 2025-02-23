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

import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * A custom implementation of {@link GridBagConstraints} designed for use with
 * panels in the campaign options dialog.
 * <p>
 * This class allows for simplified initialization of {@link GridBagConstraints}
 * with configurable anchor and fill properties, as well as preset insets.
 * <p>
 * It is intended to be paired with {@code CampaignOptionsStandardPanel} or similar components
 * using the {@link GridBagLayout}.
 */
public class CampaignOptionsGridBagConstraints extends GridBagConstraints {

    /**
     * Constructs an instance of {@link GridBagConstraints} with default settings
     * for the specified {@link JPanel}.
     * <p>
     * The {@code JPanel} will automatically be set to use a {@link GridBagLayout}.
     * Default constraints include:
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
     * Constructs an instance of {@link GridBagConstraints} with configurable
     * anchor and fill properties for the specified {@link JPanel}.
     * <p>
     * The {@code JPanel} will automatically be set to use a {@link GridBagLayout}.
     * If {@code anchor} or {@code fill} values are not provided, the following default
     * </p>
     * values are used:
     * <ul>
     *   <li>Default {@code anchor}: {@link GridBagConstraints#NORTHWEST}</li>
     *   <li>Default {@code fill}: {@link GridBagConstraints#BOTH}</li>
     * </ul>
     * Default {@code insets} are set to {@code new Insets(5, 5, 5, 5)}.
     *
     * @param panel  the {@link JPanel} for which the {@link GridBagConstraints} is created
     * @param anchor the anchor setting for the {@link GridBagConstraints}, or {@code null} to use
     *               the default value {@link GridBagConstraints#NORTHWEST}
     * @param fill   the fill setting for the {@link GridBagConstraints}, or {@code null} to use the
     *               default value {@link GridBagConstraints#BOTH}
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
