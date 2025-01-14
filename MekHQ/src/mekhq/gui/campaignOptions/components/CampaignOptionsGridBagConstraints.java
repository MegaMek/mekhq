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

public class CampaignOptionsGridBagConstraints extends GridBagConstraints {
    /**
     * Creates a {@link GridBagConstraints} object for the specified {@link JPanel}.
     * <p>
     * Written to be paired with {@code CampaignOptionsStandardPanel}.
     *
     * @param panel the {@link JPanel} for which the {@link GridBagConstraints} is created
     */
    public CampaignOptionsGridBagConstraints(JPanel panel) {
        this(panel, null, null);
    }

    /**
     * Creates a {@link GridBagConstraints} object for the specified {@link JPanel} according to the
     * provided settings.
     * <p>
     * Written to be paired with {@code CampaignOptionsStandardPanel}.
     *
     * @param panel the {@link JPanel} for which the {@link GridBagConstraints} is created
     * @param anchor the anchor setting for the {@link GridBagConstraints}, or {@code null} to use
     *              the default value {@link GridBagConstraints#NORTHWEST}
     * @param fill the fill setting for the {@link GridBagConstraints}, or {@code null} to use the
     *            default value {@link GridBagConstraints#NORTHWEST}
     */
    public CampaignOptionsGridBagConstraints(JPanel panel, @Nullable Integer anchor, @Nullable Integer fill) {
        super();
        panel.setLayout(new GridBagLayout());

        this.anchor = Objects.requireNonNullElse(anchor, GridBagConstraints.NORTHWEST);
        this.fill = Objects.requireNonNullElse(fill, GridBagConstraints.BOTH);

        this.insets = new Insets(5, 5, 5, 5);
    }
}
