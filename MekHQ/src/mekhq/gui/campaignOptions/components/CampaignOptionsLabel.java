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
import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.processWrapSize;

/**
 * A specialized {@link JLabel} component designed for use in campaign options dialogs.
 * <p>
 * The label's text and tooltip are dynamically loaded from a resource bundle
 * based on the provided name. Tooltip text can also be configured for word wrapping,
 * and an option exists to create the label without a tooltip.
 */
public class CampaignOptionsLabel extends JLabel {

    /**
     * The path to the resource bundle containing text and tooltip data for the label.
     */
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";

    /**
     * The {@link ResourceBundle} used to retrieve text and tooltips for the label.
     */
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    /**
     * Constructs a {@link CampaignOptionsLabel} with the specified name.
     * <p>
     * The label's text is retrieved using the resource key {@code "lbl" + name + ".text"}.
     * The tooltip is retrieved using the resource key {@code "lbl" + name + ".tooltip"}.
     * If the keys do not exist in the resource bundle, an exception will be thrown.
     * <p>
     * A tooltip is included by default, and a default line wrapping size is used for the tooltip text.
     *
     * @param name the base name of the label, used to construct resource bundle keys
     */
    public CampaignOptionsLabel(String name) {
        this(name, null, false);
    }

    /**
     * Constructs a {@link CampaignOptionsLabel} with the specified name,
     * optional custom tooltip wrap size, and optional tooltip exclusion.
     * <p>
     * The label's text is retrieved using the resource key {@code "lbl" + name + ".text"}.
     * If {@code noTooltip} is {@code false}, the tooltip is retrieved using the resource key
     * {@code "lbl" + name + ".tooltip"} and word-wrapped based on the provided {@code customWrapSize},
     * defaulting to 100 characters if {@code customWrapSize} is {@code null}.
     * If {@code noTooltip} is {@code true}, no tooltip is set for the label.
     * <p>
     * If the resource keys do not exist in the resource bundle, an exception will be thrown.
     *
     * @param name           the base name of the label, used to construct resource bundle keys
     * @param customWrapSize the maximum number of characters per line in the tooltip, or {@code null}
     *                       to use the default value of 100
     * @param noTooltip      if {@code true}, the label is created without a tooltip
     */
    public CampaignOptionsLabel(String name, @Nullable Integer customWrapSize, boolean noTooltip) {
        // Set the label's text using the resource bundle with HTML formatting for better rendering
        super(String.format("<html>%s</html>",
                resources.getString("lbl" + name + ".text")));

        // Configure the tooltip if not excluded
        if (!noTooltip) {
            setToolTipText(wordWrap(resources.getString("lbl" + name + ".tooltip"),
                    processWrapSize(customWrapSize)));
        }

        // Set the internal name of the label
        setName("lbl" + name);

        // Apply font scaling
        setFontScaling(this, false, 1);
    }
}
